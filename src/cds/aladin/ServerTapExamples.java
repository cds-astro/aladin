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

/**
 * 
 */
package cds.aladin;

import static cds.aladin.Constants.ACCESSURL;
import static cds.aladin.Constants.ACCESS_ESTSIZE;
import static cds.aladin.Constants.ACCESS_FORMAT;
import static cds.aladin.Constants.BIBCODE;
import static cds.aladin.Constants.CHECKQUERY;
import static cds.aladin.Constants.COMMA_CHAR;
import static cds.aladin.Constants.DATAPRODUCT_TYPE;
import static cds.aladin.Constants.DOCTITLE;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.EM_MAX;
import static cds.aladin.Constants.EM_MIN;
import static cds.aladin.Constants.JOURNAL;
import static cds.aladin.Constants.MAG;
import static cds.aladin.Constants.MAINID;
import static cds.aladin.Constants.OBSID;
import static cds.aladin.Constants.PARALLAX;
import static cds.aladin.Constants.PMDEC;
import static cds.aladin.Constants.PMRA;
import static cds.aladin.Constants.RADIALVELOCITY;
import static cds.aladin.Constants.REDSHIFT;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.SRCCLASS;
import static cds.aladin.Constants.TAPFORM_STATUS_LOADED;
import static cds.aladin.Constants.TARGETNAN;
import static cds.aladin.Constants.T_MAX;
import static cds.aladin.Constants.T_MIN;
import static cds.tools.CDSConstants.BOLD;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;

import cds.tools.ConfigurationReader;
import cds.tools.Util;

/**
 * Server that works as "template" based tap client. 
 * Here example queries(generated w.r.t server) as well as server provided examples(if any)
 * are provided by this class
 * 
 * @author chaitra
 *
 */
public class ServerTapExamples extends DynamicTapForm {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9113338791629047699L;
	
	public static String TAPSERVICEEXAMPLESTOOLTIP, SETTARGETTOOLTIP, TAPEXDEFAULTMAXROWS, CHANGESETTINGSTOOLTIP, NODUPLICATESELECTION;
	public static final String TAPEXDEFAULTMAXROWS_INT = "2000";
	Map serviceExamples = null;
	
	Map<String, CustomListCell> basicExamples = new LinkedHashMap<String, CustomListCell>();
	Map<String, String> serviceExamples2;

	String secondaryTable; 
	String grabItX1;
	String grabItY1;
	String grabItR1;
	JCheckBox addSecondTable;
	
	String max;
	
	JList examplesGui;
	JList serviceExamplesGui;
	JPanel queryDisplays;

	public boolean initExamples = true;

	JComboBox secondaryTablesGui;

	public ServerTapExamples(Aladin aladin) {
		// TODO Auto-generated constructor stub
		super(aladin);
	}
	
	protected Coord getDefaultTargetCoo() {
		Coord coo = null;
		if (this.tapClient.target != null)
			coo = this.tapClient.target;
		else if (aladin.view.isFree() || !Projection.isOk(aladin.view.getCurrentView().getProj())) {
			coo = null;
		} else {
			coo = aladin.view.getCurrentView().getCooCentre();
			coo = aladin.localisation.ICRSToFrame(coo);
		}
		return coo;
	}
	
	/**
	 * Creation of the tap query panel
	 * @param tablesMetaData
	 * @param priTableChoice
	 * @param secTableChoice 
	 */
	protected void createForm(String priTableChoice, String secTableChoice) {
		CLIENTINSTR = Aladin.chaine.getString("TAPEXCLIENTINSTR");
		Coord defaultCoo = getDefaultTargetCoo();
 		
// 		aladin.localisation.getLastCoord();
 		// defaultCoo = aladin.localisation.frameToICRS(defaultCoo);

		//Setting dummy for init.
		if (defaultCoo == null || defaultCoo.al == 0.0) {
			grabItX1 = "313.25097844474084";
		} else {
			grabItX1 = String.format(Locale.US, "%.5f", defaultCoo.al);
		}
		if (defaultCoo == null || defaultCoo.del == 0.0) {
			grabItY1 = "31.1768737946931";
		} else {
			grabItY1 = String.format(Locale.US, "%.5f", defaultCoo.del);
		}
		
		
		double defaultRadius = this.tapClient.radius;
		if (defaultRadius < 0) {
			grabItR1 = String.format(Locale.US, "%.5f", Server.getRM("10'")/60.);
		} else {
			grabItR1 = String.valueOf(defaultRadius * 2);
		}
	
		Vector<String> tables = getTableNames();
		TapTable chosenTable = null;
		
		if (priTableChoice == null || !tables.contains(priTableChoice)) {
			chosenTable = this.tapClient.tablesMetaData.get(tables.firstElement());
		} else {
			chosenTable = this.tapClient.tablesMetaData.get(priTableChoice);
		}
		selectedTableName = chosenTable.getTable_name();
		getColumnsToLoad(selectedTableName, this.tapClient.tablesMetaData);
		
		Map<String, TapTable> uploadMeta = this.tapClient.tapManager.getUploadedTables();
		
		TapTable chosenTable2 = null;
		if (this.tapClient.isUploadAllowed() && uploadMeta != null && !uploadMeta.isEmpty()) {
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
		
		setBasics();
		max = null;
		
		JPanel containerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		setTopPanel(containerPanel, c, info1, CLIENTINSTR);
	    
		JPanel tablesPanel = null;
		try {
			tablesGui = new JComboBox(tables);
			tablesPanel = getTablesPanel(null, tablesGui, chosenTable, tables, null, true);
			tablesPanel.setBackground(this.tapClient.primaryColor);
			tablesPanel.setFont(BOLD);
			c.weighty = 0.02;
	        c.weightx = 0.10;
	        c.anchor = GridBagConstraints.NORTHWEST;
		    c.fill = GridBagConstraints.NONE;
		    c.insets = new Insets(0, 4, 0, 0);
		    c.gridy++;
		    containerPanel.add(tablesPanel, c);	
		    c.weighty = 0.57;
		    
		    if (this.tapClient.isUploadAllowed()) {
			    JPanel secondaryTablePanel = new JPanel();
				secondaryTablePanel.setLayout(new BoxLayout(secondaryTablePanel, BoxLayout.PAGE_AXIS));
				MutableComboBoxModel uploadModel = (MutableComboBoxModel) this.tapClient.tapManager.getUploadClientModel();
				
				addSecondTable = new JCheckBox();
				addSecondTable.setToolTipText("Join with an uploaded table");
				addSecondTable.setEnabled((uploadModel.getSize() > 0));
				addSecondTable.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						// TODO Auto-generated method stub
						ComboBoxModel uploadModel = tapClient.tapManager.getUploadClientModel();
						if (e.getStateChange() == ItemEvent.SELECTED && uploadModel.getSize() > 0) {
							secondaryTablesGui.setEnabled(true);
							secondaryTable = (String) secondaryTablesGui.getSelectedItem();
						} else {
							secondaryTable = null;
							secondaryTablesGui.setEnabled(false);
						}
//						resetExamplesGui();
						regenerateBasicExamplesRepaint();
					}
				});	
//				Vector uploadTableNames = new Vector();
//				if (uploadMeta != null) {
//					uploadTableNames.addAll(uploadMeta.keySet());
//				}
				secondaryTablesGui = new JComboBox();
				secondaryTablesGui.setModel(uploadModel);
//				model.addListDataListener(new ListDataListener() {
//					
//					@Override
//					public void intervalRemoved(ListDataEvent e) {
//						// TODO Auto-generated method stub
//						if (((MutableComboBoxModel)e.getSource()).getSize() > 0) {
//							secondaryTablesGui.setEnabled(true);
//							secondaryTable = (String) secondaryTablesGui.getSelectedItem();
//						} else {
//							secondaryTable = null;
//							secondaryTablesGui.setEnabled(false);
//						}
//						changeTableSelection(selectedTableName);
//					}
//					
//					@Override
//					public void intervalAdded(ListDataEvent e) {
//						// TODO Auto-generated method stub
//						if (((MutableComboBoxModel)e.getSource()).getSize() > 0) {
//							secondaryTablesGui.setEnabled(true);
//							secondaryTable = (String) secondaryTablesGui.getSelectedItem();
//						} else {
//							secondaryTable = null;
//							secondaryTablesGui.setEnabled(false);
//						}
//						changeTableSelection(selectedTableName);
//					}
//					
//					@Override
//					public void contentsChanged(ListDataEvent e) {
//						// TODO Auto-generated method stub
//						
//					}
//				});
				ArrayList<JComponent> compToPrefix = new ArrayList<JComponent>();
				compToPrefix.add(addSecondTable);
				tablesPanel = getTablesPanel("Join:", secondaryTablesGui, chosenTable2, null, compToPrefix, false);
				tablesPanel.setBackground(this.tapClient.primaryColor);
				tablesPanel.setFont(BOLD);
				if (secondaryTable != null) {
					secondaryTablesGui.setSelectedItem(secondaryTable);
					addSecondTable.setSelected(true);
				} else {
					addSecondTable.setSelected(false);
					secondaryTablesGui.setEnabled(false);
				}
				secondaryTablePanel.add(tablesPanel);
				
				c.weightx = 0.12;
				c.weighty = 0.02;
		        c.anchor = GridBagConstraints.NORTHWEST;
			    c.fill = GridBagConstraints.NONE;
			    c.insets = new Insets(0, 4, 0, 0);
			    c.gridy++;
			    containerPanel.add(secondaryTablePanel, c);	
			    c.weighty = 0.55;
			}
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			Aladin.error(this, e.getMessage());
			return;
		}
		c.weighty = 0.55;
		populateExamples();
		this.examplesGui = new JList(this.basicExamples.keySet().toArray());
		this.examplesGui.setCellRenderer(new CustomListCellRenderer(this.basicExamples));
		this.examplesGui.setVisibleRowCount(4);
		this.examplesGui.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.examplesGui.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				// TODO Auto-generated method stub
				String queryLabel = (String) examplesGui.getSelectedValue();
				if (queryLabel != null && basicExamples.containsKey(queryLabel) ) {
					tap.setText(((CustomListCell)basicExamples.get(queryLabel)).label);
					if (serviceExamplesGui != null && !examplesGui.isSelectionEmpty()) {
						serviceExamplesGui.clearSelection();
					}
				} else {
					tap.setText(EMPTYSTRING);
				}
			}
		});
		
		queryDisplays = new JPanel();
		JScrollPane scrolley = new JScrollPane(this.examplesGui);
		
		queryDisplays.setLayout(new GridLayout(1, 1));
		queryDisplays.add(scrolley);
		c.fill = GridBagConstraints.BOTH;
		c.gridy++;
		c.insets = new Insets(0, 4, 0, 0);
		containerPanel.add(queryDisplays, c);
		
		JPanel linePanel = getBottomPanel(true);
		c.gridx = 0;
		c.weightx = 1;
		c.weighty = 0.02;
		c.insets = new Insets(0, -6, 0, 0);
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(linePanel, c);
	    
		tap = new JTextArea(8, 100);//"", 8, 50
		tap.setFont(Aladin.ITALIC);
		tap.setWrapStyleWord(true);
		tap.setLineWrap(true);
		tap.setEditable(true);
		scrolley = new JScrollPane(tap);
//		c.weightx = 0.35;
		c.weighty = 0.35;
		c.weightx = 1;
		c.insets = new Insets(0, 4, 0, 0);
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(scrolley, c);

	    this.removeAll();
	    add(containerPanel);
	    
	    formLoadStatus = TAPFORM_STATUS_LOADED;
	    this.tapClient.tapManager.initTapExamples(this);
	}
	
	public void loadTapExamples() {
		if (initExamples) {
			// we might never need to get the examples if user won't choose this mode. 
			// so we load here as this one is very specific to loading of this gui
			this.serviceExamples = this.tapClient.tapManager.getTapExamples(this.tapClient.tapBaseUrl);
			initExamples = false;
		} 
		if (serviceExamples != null && !serviceExamples.isEmpty()) {
			this.serviceExamplesGui = new JList(serviceExamples.keySet().toArray());
			this.serviceExamplesGui.setVisibleRowCount(4);
			this.serviceExamplesGui.setToolTipText(TAPSERVICEEXAMPLESTOOLTIP);
			this.serviceExamplesGui.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			this.serviceExamplesGui.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					// TODO Auto-generated method stub
					//tintin todo
					String queryLabel = (String) serviceExamplesGui.getSelectedValue();
					if (queryLabel != null) {
						tap.setText(serviceExamples.get(queryLabel).toString());
						if (!serviceExamplesGui.isSelectionEmpty()) {
							examplesGui.clearSelection();
						}
					}
				}
			});
			waitCursor();
			synchronized (queryDisplays) {
				JScrollPane scrolley = new JScrollPane(this.examplesGui);
				queryDisplays.removeAll();
				queryDisplays.setLayout(new GridLayout(1, 2));
				queryDisplays.add(scrolley);
				scrolley = new JScrollPane(this.serviceExamplesGui);
				queryDisplays.add(scrolley);
				queryDisplays.revalidate();
				queryDisplays.repaint();
			}
			defaultCursor();
		}
		ball.setMode(Ball.OK);
		info1.setText(CLIENTINSTR);
	}
	
	@Override
	public void submit() {
		if (this.sync_async != null &&  this.tap != null) {
			String tapQuery = tap.getText();
			if (tap.getText().isEmpty()) {
				Aladin.error(this, CHECKQUERY_ISBLANK);
				return;
			}
			Map<String, Object> requestParams = null;
			if (secondaryTable != null) {
				TapManager tapManager = TapManager.getInstance(aladin);
				if (!this.tapClient.tablesMetaData.containsKey(secondaryTable)) {
					if (tapManager.uploadFrame.uploadedTableFiles.containsKey(secondaryTable)) {
						if (tap.getText().contains(secondaryTable)) {
							requestParams = new HashMap<String, Object>();
							FrameUploadServer uploadFrame = tapManager.uploadFrame;
							if (uploadFrame.uploadedTableFiles.get(secondaryTable) == null) {
								Aladin.error(this, "Unable to submit " + secondaryTable + " data!");
								return;
							}
							String uploadFileName = FrameUploadServer.UPLOADFILEPREFIX+secondaryTable;
							requestParams.put("upload", uploadFrame.getUploadParam(secondaryTable, uploadFileName));
							requestParams.put(uploadFileName, uploadFrame.uploadedTableFiles.get(secondaryTable));
						}
					}
				}
			}
			
			String fullQuery = tapQuery.toUpperCase();
			if (!fullQuery.startsWith("SELECT ")) {
				tapQuery = fullQuery.substring(fullQuery.indexOf("SELECT", 0), fullQuery.length());
				Aladin.trace(3,tapQuery);
			}
			boolean sync = this.sync_async.getSelectedItem().equals("SYNC");
	  	  	this.submitTapServerRequest(sync, requestParams, this.tapClient.tapLabel, this.tapClient.tapBaseUrl, tapQuery);
		}
		
	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		super.actionPerformed(arg0);
		Object source = arg0.getSource();
		if (source instanceof JButton) {
			String action = ((JButton) source).getActionCommand();
			/*if (action.equals(SETTINGS)) {//tintin remove comments
				try {
					TapManager tapManager = TapManager.getInstance(aladin);
					if (tapManager.settingsFrame == null) {
						tapManager.settingsFrame = new FrameTapSettings(aladin);
					}
					tapManager.settingsFrame.show(this, "Settings for " + this.tapClient.tapLabel);
				} catch (Exception e) {
					if (Aladin.levelTrace >= 3) e.printStackTrace();
					Aladin.warning(this, TapClient.GENERICERROR);
		            ball.setMode(Ball.NOK);
				}
			} else*/ if (action.equals(CHECKQUERY)) {
				checkQueryFlagMessage();
			} 
		}
	}
	
	@Override
	public void createFormDefault() {
		this.createForm(null, null);
	}

	@Override
	public void changeTableSelection(String tableChoice){//tableChoice is not set from here. Ever. for this class only.(we set second table also. thats why i tink)
		if (this.examplesGui != null) {
			this.setTableGetColumnsToLoad(tableChoice, this.tapClient.tablesMetaData);
			regenerateBasicExamplesRepaint();
		}
	}
	
	@Override
	public void checkSelectionChanged(JComboBox<String> comboBox) {
		if (comboBox.getSelectedItem() != null) {
			String chosen = comboBox.getSelectedItem().toString();
			if (comboBox.equals(this.tablesGui) && !selectedTableName.equalsIgnoreCase(chosen)) {
				Aladin.trace(3, "Change table selection from within the document");
				selectedTableName = chosen;
				this.changeTableSelection(selectedTableName);
			} else if (comboBox.equals(this.secondaryTablesGui)
					&& (secondaryTable != null || !chosen.equalsIgnoreCase(secondaryTable))) {
				Aladin.trace(3, "Change table selection from within the document");
				if (secondaryTablesGui.getItemCount() > 0) {
					if (addSecondTable.isSelected()) {
						secondaryTablesGui.setEnabled(true);
						secondaryTable = chosen;
					}
					addSecondTable.setEnabled(true);
				} else {
					secondaryTable = null;
					addSecondTable.setEnabled(false);
					secondaryTablesGui.setEnabled(false);
				}
				this.changeTableSelection(selectedTableName);
			}
		} else if (comboBox.equals(this.secondaryTablesGui)) {
			secondaryTable = null;
			addSecondTable.setEnabled(false);
			secondaryTablesGui.setEnabled(false);
			this.changeTableSelection(selectedTableName);
			TapManager.getInstance(aladin).eraseNotification(info1, "Queries regenerated!", CLIENTINSTR);
		}
	};

	public void regenerateBasicExamples() {
		this.basicExamples.clear();
		if (secondaryTable != null && selectedTableName.equalsIgnoreCase(secondaryTable)) {
			secondaryTable = null;
			TapManager.getInstance(aladin).eraseNotification(info1, NODUPLICATESELECTION, CLIENTINSTR);
		}
		String priTableNameForQuery = TapTable.getQueryPart(selectedTableName, true);
		this.basicExamples.put("Select all", new CustomListCell("Select * from " + priTableNameForQuery, EMPTYSTRING));
		this.basicExamples.put("Select top 1000", new CustomListCell("Select TOP 1000 * from " + priTableNameForQuery, EMPTYSTRING));
		// this.basicExamples.put("Select few columns", "Select oidref, filter,
		// flux, bibcode from "+primaryTable);
		this.basicExamples.put("Get the number of rows", new CustomListCell("Select count(*) from " + priTableNameForQuery, EMPTYSTRING));

		String areaConstraint = "0.09";
		TapTable priTableMetaData = this.tapClient.tablesMetaData.get(selectedTableName);
		String tableSelectAllQuery = "Select %s *  from %s";
		if (max == null) {
			if (TAPEXDEFAULTMAXROWS != null) {
				max = TAPEXDEFAULTMAXROWS;
			} else {
				max = TAPEXDEFAULTMAXROWS_INT;
			} 
		}
		
		if (!max.equalsIgnoreCase(EMPTYSTRING) && !max.startsWith("TOP ")) {
			max = "TOP "+max;
		}
		String conesearchtemplate = " where 1=CONTAINS(POINT('ICRS', %s, %s), CIRCLE('ICRS', %s, %s, %s))";
		String primaryTableSelectAllQuery = String.format(tableSelectAllQuery, max, priTableNameForQuery);

		String priRaColumnName = TapTable.getQueryPart(priTableMetaData.getRaColumnName(), false);
		String priDecColumnName = TapTable.getQueryPart(priTableMetaData.getDecColumnName(), false);

		String targetQuery = primaryTableSelectAllQuery + conesearchtemplate;
		String coneSearchPart = null;
		try {
			if (priRaColumnName != null && priDecColumnName != null) {
				if (grab != null) {
					grab.setVisible(true);
				}
				targetQuery = String.format(targetQuery, priRaColumnName, priDecColumnName, grabItX1, grabItY1,
						grabItR1);
				this.basicExamples.put("Cone search query", new CustomListCell(targetQuery, SETTARGETTOOLTIP));
				coneSearchPart = String.format(conesearchtemplate, priRaColumnName, priDecColumnName, grabItX1, grabItY1,
						grabItR1);
			} else {
				if (grab != null) {
					grab.setVisible(false);
				}
			}

		} catch (Exception e) {
			// won't add those examples..
		}

		StringBuffer spQuery = null;
		StringBuffer hints = null;
		CustomListCell setCell = null;
		String queryName = null;
		List<String> mandateParamsToAdd = new ArrayList<String>();
		List<String> optionalParamsToAdd = new ArrayList<String>();

		// some nice select statements..also set param names
		TapTableColumn parallax = priTableMetaData.getFlaggedColumn(PARALLAX), radialVelocity = priTableMetaData.getFlaggedColumn(RADIALVELOCITY);
		TapTableColumn bibCode = priTableMetaData.getFlaggedColumn(BIBCODE), journal = priTableMetaData.getFlaggedColumn(JOURNAL), title = priTableMetaData.getFlaggedColumn(DOCTITLE);
		TapTableColumn pmra = priTableMetaData.getFlaggedColumn(PMRA), pmdec = priTableMetaData.getFlaggedColumn(PMDEC);
		TapTableColumn redshift = priTableMetaData.getFlaggedColumn(REDSHIFT), mag = priTableMetaData.getFlaggedColumn(MAG);
		TapTableColumn id = priTableMetaData.getFlaggedColumn(MAINID), srcClass = priTableMetaData.getFlaggedColumn(SRCCLASS);
		
		// Bibliographic data
		if (bibCode != null) {
			mandateParamsToAdd.add(bibCode.getColumnNameForQuery());
		}
		if (journal != null) {
			mandateParamsToAdd.add(journal.getColumnNameForQuery());
		}
		if (title != null) {
			mandateParamsToAdd.add(title.getColumnNameForQuery());
		}
		if (id != null) {
			optionalParamsToAdd.add(0, id.getColumnNameForQuery());
		}
		 
		if (!mandateParamsToAdd.isEmpty()) {
			addSelectsToBasicExamples("Get bibliographic data", mandateParamsToAdd, optionalParamsToAdd, null, null, null);
		}

		// radial velocity and parallax
		spQuery = new StringBuffer();
		hints = new StringBuffer();
		mandateParamsToAdd.clear();
		
		if (parallax != null) {
			mandateParamsToAdd.add(parallax.getColumnNameForQuery());
			queryName = "Get parallax ";
			setHint(hints, parallax);
		}
		if (radialVelocity != null) {
			mandateParamsToAdd.add(radialVelocity.getColumnNameForQuery());
			if (queryName != null) {
				queryName = queryName+" and radial velocity";
			} else {
				queryName = "Get radial velocity";
			}
			setHint(hints, radialVelocity);
		}
		if (!mandateParamsToAdd.isEmpty() && queryName != null) {
			setCell = new CustomListCell();
			setCell.tooltip = hints.toString();
			addSelectsToBasicExamples(queryName, mandateParamsToAdd, optionalParamsToAdd, setCell, null,
					coneSearchPart);
		}

		// position and proper motion
		mandateParamsToAdd.clear();
		hints = new StringBuffer();
		setCell = new CustomListCell();
		if (pmra != null) {
			mandateParamsToAdd.add(pmra.getColumnNameForQuery());
			setHint(hints, pmra);
		}
		if (pmdec != null) {
			mandateParamsToAdd.add(pmdec.getColumnNameForQuery());
			setHint(hints, pmdec);
		}
		if (!mandateParamsToAdd.isEmpty()) {// needed
			int index = optionalParamsToAdd.size();
			if (priDecColumnName != null) {
				optionalParamsToAdd.add(index, priDecColumnName);
			}
			if (priRaColumnName != null) {
				optionalParamsToAdd.add(index, priRaColumnName);
			}
			setCell.tooltip = hints.toString();
			addSelectsToBasicExamples("Position and proper motion", mandateParamsToAdd, optionalParamsToAdd, setCell, null,
					coneSearchPart);

			String properMotionQuery = "SQRT(POWER(" + pmra.getColumnNameForQuery() + ",2)+POWER(" + pmdec.getColumnNameForQuery() + ",2))";
			mandateParamsToAdd.add(properMotionQuery + " as pm");
			properMotionQuery = " WHERE " + properMotionQuery + " > 20";
			setCell = new CustomListCell();
			setCell.tooltip = hints.toString();
			addSelectsToBasicExamples("Proper motion", mandateParamsToAdd, optionalParamsToAdd, setCell, 
					"Proper motion limits ", properMotionQuery);
		}

		// red shift
		spQuery = new StringBuffer();
		mandateParamsToAdd.clear();
		if (redshift != null) {
			mandateParamsToAdd.add(redshift.getColumnNameForQuery());
			setCell = new CustomListCell();
			hints = new StringBuffer();
			setHint(hints, redshift);
			setCell.tooltip = hints.toString();
			addSelectsToBasicExamples(null, mandateParamsToAdd, optionalParamsToAdd, setCell, null, coneSearchPart);
		}

		// operations with those params
		spQuery = new StringBuffer(primaryTableSelectAllQuery);
		hints = new StringBuffer();
		if (srcClass != null) {
			spQuery.append(" where ").append(srcClass.getColumnNameForQuery()).append(" LIKE '%STAR%'");
			setHint(hints, srcClass);
			this.basicExamples.put("Based on source type ", new CustomListCell(spQuery.toString(), hints.toString()));
		}
		
		
		String sRegionColumnName = null;
		String secRaColumnName = null, secDecColumnName = null;
		String secTableNameForQuery = null;
		TapTable secTableMetaData = null;
		if (secondaryTable != null) {
			Map<String, TapTable> uploadedTables = this.tapClient.tapManager.getUploadedTables();
			if (uploadedTables != null) {
				secTableMetaData = uploadedTables.get(secondaryTable);
				if (secTableMetaData != null) {
					secRaColumnName = TapTable.getQueryPart(secTableMetaData.getRaColumnName(), false); 
					secDecColumnName = TapTable.getQueryPart(secTableMetaData.getDecColumnName(), false);
					secTableNameForQuery = TapTable.getQueryPart(secondaryTable, true);
				}
			}
		}
		
		boolean obscore = priTableMetaData.isObscore();
		if (obscore) {
			String dataproduct_type = priTableMetaData.getObsColumnNameForQuery(DATAPRODUCT_TYPE);
			String obs_id = priTableMetaData.getObsColumnNameForQuery(OBSID);
			String access_url = priTableMetaData.getObsColumnNameForQuery(ACCESSURL),
					access_format = priTableMetaData.getObsColumnNameForQuery(ACCESS_FORMAT),
					access_estsize = priTableMetaData.getObsColumnNameForQuery(ACCESS_ESTSIZE);
			sRegionColumnName = priTableMetaData.getObsColumnNameForQuery(ServerObsTap.FIELDSIZE);
			String s_resolution = priTableMetaData.getObsColumnNameForQuery(ServerObsTap.SPATIALRESOLUTION);
			String t_min = priTableMetaData.getObsColumnNameForQuery(T_MIN), t_max = priTableMetaData.getObsColumnNameForQuery(T_MAX),
					t_exptime = priTableMetaData.getObsColumnNameForQuery(ServerObsTap.EXPOSURETIME),
					t_resolution = priTableMetaData.getObsColumnNameForQuery(ServerObsTap.TIMERESOLUTION);
			String em_min = priTableMetaData.getObsColumnNameForQuery(EM_MIN),
					em_max = priTableMetaData.getObsColumnNameForQuery(EM_MAX),
					em_res_power = priTableMetaData.getObsColumnNameForQuery(ServerObsTap.SPECTRALRESOLUTIONPOWER);
			/*String o_ucd = null, o_unit = null;
			String pol_states = null;*/
			
			//Obscore things
			spQuery = new StringBuffer(primaryTableSelectAllQuery);
			/**
			 * example from spec
			 * DataType=Image. Spatial resolution better than 0.3 arc seconds Filter
			 * = J or H or K.
			 */
			if (dataproduct_type != null && s_resolution != null) {
				spQuery.append(" WHERE ").append(dataproduct_type).append(" = 'image'")
				.append(" AND ").append(s_resolution).append(" < 0.3 ");
				if (em_min != null && em_max != null) {
					spQuery.append(" AND (").append(em_min).append(" > 2.1e-06").append(" AND ").append(em_max).append(" < 2.4e-06)")
					.append(" OR (").append(em_min).append(" > 1.6e-06").append(" AND ").append(em_max).append(" < 1.8e-06)")
					.append(" OR (").append(em_min).append(" > 1.2e-06").append(" AND ").append(em_max).append(" < 1.4e-06)");
				}
				this.basicExamples.put("Images in band J, H, K with spatial res. > 0.3 arcsec", new CustomListCell(spQuery.toString(), EMPTYSTRING));
			}
			
			mandateParamsToAdd.clear();
			optionalParamsToAdd.clear();
			if (access_url != null) {
				mandateParamsToAdd.add(access_url);
			}
			if (access_format != null) {
				mandateParamsToAdd.add(access_format);
			}
			if (access_estsize != null) {
				mandateParamsToAdd.add(access_estsize);
			}
			if (obs_id != null) {
				optionalParamsToAdd.add(0, obs_id);
			}
			if (!mandateParamsToAdd.isEmpty()) {
				addSelectsToBasicExamples("Get access_url, access_formats...", mandateParamsToAdd, optionalParamsToAdd, null, null, null);
			}
			
			/**
			 * example from spec
			 * DataType = any Energy includes 5 keV RA includes 16.00 DEC includes
			 * +10 Exposure time > 10 ks
			 */
			spQuery = new StringBuffer(primaryTableSelectAllQuery);
			if (t_exptime != null && em_min != null && em_max != null) {
				spQuery.append(" WHERE ").append(t_exptime).append(" > 10000");
				if (sRegionColumnName != null) {
					spQuery.append(" AND CONTAINS(POINT('ICRS',"+grabItX1+" , "+ grabItY1+"),"+sRegionColumnName+") = 1");
				}
				spQuery.append(" AND (").append(em_min).append(" < 2.48e-10").append(" AND ").append(em_max).append(" > 2.48e-10)");
				this.basicExamples.put("Data at specified values for energy, position and exposure time", new CustomListCell(spQuery.toString(), EMPTYSTRING));
			}
			
			/**
			 * example from spec
			 * DataType=Cube including target point
			 */
			spQuery = new StringBuffer(primaryTableSelectAllQuery);
			String label = null;
			if (dataproduct_type != null) {
				spQuery.append(" WHERE ").append(dataproduct_type).append(" = 'cube'");
				if (sRegionColumnName != null) {
					spQuery.append(" AND CONTAINS(POINT('ICRS',"+grabItX1+" , "+ grabItY1+"),"+sRegionColumnName+") = 1");
					label = "Get cube at specified target";
				} else {
					label = "Get cube";
				}
				this.basicExamples.put(label, new CustomListCell(spQuery.toString(), EMPTYSTRING));
				
				if (em_res_power != null && em_min != null && em_max != null) {
					spQuery.append(" AND ").append(em_res_power).append(" < 5250/5")
					.append(" AND ").append(em_min).append(" <= 4000e-10 ").append(" AND 6500e-10 <= ").append(em_max);
					
					/**
					 * Cube including spectral values at 6500 and at 4000 angstroms and resolution better than 5 Angstroms
					 */
					this.basicExamples.put(label+ " with spectral constraints", new CustomListCell(spQuery.toString(), EMPTYSTRING));
				}
			}
			
			
			/**
			 * example from spec
			 * DataType=TimeSeries, RA includes 16.00 hours,DEC includes
			 * +41.00 ,Time resolution better than 1 minute, Observation data
			 * before June 10, 2008, Observation data after June 10, 2007
			 */
			if (targetQuery != null) {
				spQuery = new StringBuffer(targetQuery).append(" AND ");
			} else {
				spQuery = new StringBuffer(primaryTableSelectAllQuery).append(" WHERE ");
			}
			
			if (dataproduct_type != null && t_resolution != null) {
				spQuery.append(dataproduct_type).append(" = 'timeseries'")
				.append(" AND ").append(t_resolution).append(" > 60");
				if (t_min != null) {
					spQuery.append(" AND ").append(t_min).append(" BETWEEN 54261 AND 54627");
				}
				this.basicExamples.put("Time series for position and time constraints", new CustomListCell(spQuery.toString(), EMPTYSTRING));
			}
			
			spQuery = new StringBuffer(primaryTableSelectAllQuery);
			hints = new StringBuffer();
			if (sRegionColumnName != null) {
				spQuery.append(" WHERE AREA(").append(sRegionColumnName).append(") > ").append(areaConstraint);
				this.basicExamples.put("Area > " + areaConstraint, new CustomListCell(spQuery.toString(), EMPTYSTRING));
				
				
				spQuery = new StringBuffer(primaryTableSelectAllQuery);
				String targetSearch = " WHERE 1=CONTAINS(POINT('ICRS', %s, %s), %s)";
				targetSearch = String.format(targetSearch, grabItX1, grabItY1, sRegionColumnName);
				spQuery.append(targetSearch);
				this.basicExamples.put("Specific target search", new CustomListCell(spQuery.toString(), SETTARGETTOOLTIP));
			}
			
			
			if (secondaryTable != null && secTableMetaData != null) {
				String sec_t_min = secTableMetaData.getObsColumnNameForQuery(T_MIN), 
						sec_t_max = secTableMetaData.getObsColumnNameForQuery(T_MAX);
				
				spQuery = new StringBuffer("Select ");
				spQuery.append(max).append(SPACESTRING).append(priTableNameForQuery).append(".*,")
				.append(secTableNameForQuery).append(".* FROM ").append(priTableNameForQuery).append(" JOIN ")
				.append(secTableNameForQuery).append(SPACESTRING);
				
				if (sRegionColumnName != null && secRaColumnName != null && secDecColumnName != null) {
					spQuery.append(" ON 1= CONTAINS(POINT('ICRS',").append(secTableNameForQuery).append(DOT_CHAR)
							.append(secRaColumnName).append(", ").append(secTableNameForQuery).append(DOT_CHAR)
							.append(secDecColumnName).append("),").append(priTableNameForQuery).append(DOT_CHAR)
							.append(sRegionColumnName).append(")");
					this.basicExamples.put("Join w.r.t second table positions",
							new CustomListCell(spQuery.toString(), EMPTYSTRING));
				
				
				spQuery.append(" WHERE ");
				if (t_min != null && t_max != null && sec_t_min != null && sec_t_max != null) {
					spQuery.append(priTableNameForQuery).append(DOT_CHAR).append(dataproduct_type).append(" = 'image'").append(" AND ").append(priTableNameForQuery)
					.append(DOT_CHAR).append(t_min).append(" <= ").append(secTableNameForQuery).append(DOT_CHAR).append(sec_t_min)
					.append(" + 1 AND ").append(secTableNameForQuery).append(DOT_CHAR).append(sec_t_min)
					.append(" - 1 <= ").append(priTableNameForQuery)
					.append(DOT_CHAR).append(t_max);
					this.basicExamples.put("Images within one day of second table time interval",
							new CustomListCell(spQuery.toString(), EMPTYSTRING));
				}
				}
			}
		}
		
		 
		
		/**
		 * Join queries for target in table2 contained in region of table1
		 */
		spQuery = new StringBuffer(primaryTableSelectAllQuery);
		if (secondaryTable != null) {
			spQuery = new StringBuffer(primaryTableSelectAllQuery);
			if (priRaColumnName != null && priDecColumnName != null && secRaColumnName != null
					&& secDecColumnName != null) {
				spQuery.append(COMMA_CHAR).append(secTableNameForQuery).append(" WHERE 1= CONTAINS(POINT('ICRS', ")
						.append(priTableNameForQuery).append(DOT_CHAR).append(priRaColumnName).append(", ")
						.append(priTableNameForQuery).append(DOT_CHAR).append(priDecColumnName)
						.append("), CIRCLE('ICRS', ").append(grabItX1).append(COMMA_CHAR).append(grabItY1)
						.append(COMMA_CHAR).append(grabItR1).append(")) AND ").append("1 =CONTAINS (POINT('ICRS', ")
						.append(priTableNameForQuery).append(DOT_CHAR).append(priRaColumnName).append(", ")
						.append(priTableNameForQuery).append(DOT_CHAR).append(priDecColumnName)
						.append("), CIRCLE('ICRS', ").append(secTableNameForQuery).append(DOT_CHAR)
						.append(secRaColumnName).append(", ").append(secTableNameForQuery).append(DOT_CHAR)
						.append(secDecColumnName).append(", 0.0001))");
				this.basicExamples.put("Cross match", new CustomListCell(spQuery.toString(), EMPTYSTRING));

				spQuery = new StringBuffer(primaryTableSelectAllQuery);
				spQuery.append(" WHERE NOT EXISTS (").append(" SELECT * FROM ").append(secTableNameForQuery)
						.append(" WHERE 1= CONTAINS (POINT('ICRS',").append(secTableNameForQuery).append(DOT_CHAR)
						.append(secRaColumnName).append(", ").append(secTableNameForQuery).append(DOT_CHAR)
						.append(secDecColumnName).append("), CIRCLE('ICRS', ").append(priTableNameForQuery).append(DOT_CHAR)
						.append(priRaColumnName).append(", ").append(priTableNameForQuery).append(DOT_CHAR)
						.append(priDecColumnName).append(", 0.0001)))");
				this.basicExamples.put("No match without target", new CustomListCell(spQuery.toString(), "Filter out objects from the second table. "+CHANGESETTINGSTOOLTIP));
				
				
				spQuery.append(" AND 1=CONTAINS(POINT('ICRS',")
						.append(priTableNameForQuery).append(DOT_CHAR).append(priRaColumnName).append(", ")
						.append(priTableNameForQuery).append(DOT_CHAR).append(priDecColumnName)
						.append("), CIRCLE('ICRS', ").append(grabItX1).append(COMMA_CHAR).append(grabItY1)
						.append(COMMA_CHAR).append(grabItR1).append("))");
						
				this.basicExamples.put("No match at selected target", new CustomListCell(spQuery.toString(), "Filter out objects from the second table. "+CHANGESETTINGSTOOLTIP));
				
			}
			
			
		}
	}

	private void setHint(StringBuffer hints, TapTableColumn tapTableColumn) {
		// TODO Auto-generated method stub
		if (tapTableColumn != null ) {
			if (hints.length() != 0) {
				hints.append(", ");
			}
			hints.append(tapTableColumn.getColumn_name());
			if (tapTableColumn.getUnit() != null && !tapTableColumn.getUnit().isEmpty()) {
				hints.append(" in ").append(tapTableColumn.getUnit());
			}
			if (tapTableColumn.getUtype() != null && !tapTableColumn.getUtype().isEmpty()) {
				hints.append(", UTYPE: ").append(tapTableColumn.getUtype());
			}
		}
	}
	
	/**
	 * sets target changes and calls regenerates examples
	 */
	public void targetSettingsChangedAction() {
		changeTargetSettings();
		regenerateBasicExamplesRepaint();
	}
	
	public void regenerateBasicExamplesRepaint() {
		regenerateBasicExamples();
		this.examplesGui.removeAll();
		this.examplesGui.setListData(this.basicExamples.keySet().toArray());
		this.examplesGui.revalidate();
		this.examplesGui.repaint();
	}
	
	public void changeTargetSettings() {
		if (this.target != null) {
			String obj;
			try {
				obj = resolveQueryField();
				if( obj == null) throw new Exception(UNKNOWNOBJ);
				else if (!obj.trim().equals(TARGETNAN)) {//we do not change the settings when nothing is provided
					if (this.radius != null) {
						String grabItX = Util.myRound(coo[0].getText(), 5);
						String grabItY = Util.myRound(coo[1].getText(), 5);
//						double grabItR = Double.parseDouble(rad[0].getText());
//						resolveRadius(targetPanel.radius.getText().trim(), false);
						grabItX1 = grabItX;
						grabItY1 = grabItY;
						grabItR1 = Util.myRound(rad[0].getText(), 5);
					}	
				}
			} catch (Exception e) {
				Aladin.error(this, "No target set. "+e.getMessage());
			}
		}
	}

	/*public void changeTargetSettings(TargetPanel targetPanel, int whichTarget) {
		if (targetPanel.target != null) {
			String obj;
			try {
				obj = resolveQueryField();
				if( obj == null) throw new Exception(UNKNOWNOBJ);
				else if (!obj.trim().equals(TARGETNAN)) {//we do not change the settings when nothing is provided
					if (targetPanel.radius != null) {
						String grabItX = coo[0].getText();
						String grabItY = coo[1].getText();
						double grabItR = Double.parseDouble(rad[0].getText());
//						resolveRadius(targetPanel.radius.getText().trim(), false);
						if (whichTarget == 0) {
							grabItX1 = grabItX;
							grabItY1 = grabItY;
							grabItR1 = grabItR;
						} else if (whichTarget == 1) {
							grabItX2 = grabItX;
							grabItY2 = grabItY;
							grabItR2 = grabItR;
						}
					}	
				}
			} catch (Exception e) {
				Aladin.warning(this, "No target set. "+e.getMessage());
			}
		}
	}*/
	
	public void appendSelectString(List<String> selectParams, StringBuffer spQuery) {
		boolean notFirst = false;
		for (String selectParam : selectParams) {
			if (notFirst) {
				spQuery.append(", ");
			}
			spQuery.append(selectParam);
			notFirst = true;
		}
	}
	
	public void populateExamples() {
		regenerateBasicExamples();
		fetchServiceProvidedExamples();
	}

	private void fetchServiceProvidedExamples() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Convenience method that prepared select params 
	 * and also adds where constraint to the basic examples
	 * @param queryName
	 * @param selectParamsToAdd - non empty list
	 * @param optionalParamsToAdd
	 * @param whereLabel
	 * @param wherePart
	 */
	public void addSelectsToBasicExamples(String queryName, List<String> mandatoryParams,
			List<String> optionalParamsToAdd, CustomListCell setCell, String whereLabel, String wherePart) {
		String tableSelectQuery = "Select %s %s from %s";
		StringBuffer spQuery = new StringBuffer();
		List<String> selectParamsToAdd = new ArrayList<String>();
		selectParamsToAdd.addAll(optionalParamsToAdd);
		selectParamsToAdd.addAll(mandatoryParams);
		appendSelectString(selectParamsToAdd, spQuery);
		if (spQuery.toString().isEmpty()) {
			spQuery.append(" * ");
		}
		String priTableNameForQuery = TapTable.getQueryPart(selectedTableName, true);
		String queryToDisplay = String.format(tableSelectQuery, max, spQuery.toString(), priTableNameForQuery);
		if (queryName == null) {
			queryName = "Select " + spQuery.toString();
		}
		if (setCell == null) {
			this.basicExamples.put(queryName, new CustomListCell(queryToDisplay, EMPTYSTRING));
		} else {
			setCell.label = queryToDisplay;
			this.basicExamples.put(queryName, setCell);
		}
		
		if (wherePart != null) {
			if (whereLabel == null) {
				whereLabel = queryName + " plus conesearch";
			}
			this.basicExamples.put(whereLabel, new CustomListCell(queryToDisplay + wherePart, SETTARGETTOOLTIP));
		}
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
	}
	
	protected void createChaine() {
		super.createChaine();
		description = Aladin.chaine.getString("TAPFORMINFO");
		title = Aladin.chaine.getString("TAPFORMTITLE");
		verboseDescr = loadedServerDescription = Aladin.chaine.getString("TAPEXAMPLEFORMDESC");
		TAPSERVICEEXAMPLESTOOLTIP = Aladin.chaine.getString("TAPSERVICEEXAMPLESTOOLTIP");
		SETTARGETTOOLTIP = Aladin.chaine.getString("SETTARGETTOOLTIP");
		TAPEXDEFAULTMAXROWS = ConfigurationReader.getInstance().getPropertyValue("TAPEXDEFAULTMAXROWS");
		CHANGESETTINGSTOOLTIP = Aladin.chaine.getString("CHANGESETTINGSTOOLTIP");
		NODUPLICATESELECTION = Aladin.chaine.getString("NODUPLICATESELECTION");
	}

}
