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

import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.REGEX_ALPHA;
import static cds.aladin.Constants.REGEX_ONLYALPHANUM;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.TAPFORM_STATUS_NOTLOADED;
import static cds.aladin.Constants.UPLOAD;
import static cds.aladin.Constants.UPLOADTABLEPREFIX;
import static cds.aladin.Constants.EDITUPLOADTABLENAMEACTION;
import static cds.aladin.Constants.TABLEGUINAME;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cds.tools.Util;


/**
 * This class is to show the upload frame for Tap servers
 * 
 * @deprecated as of now the interface to load upload table on a tap client is removed. <br>
 * All upload tables are synchronized to eligible tables on Aladin stack. Use {@link TapManager #uploadTablesModel }
 */
@Deprecated 
public class FrameUploadServer extends JFrame implements ActionListener, PlaneLoadListener, GrabItFrame {

   /**
	 * 
	 */
	private static final long serialVersionUID = 399753558953437543L;

	static String TITLE, CLOSE, ERRORMSG, IDENTIFIER, TAPTABLEUPLOADPARSETIP, UPFILEINFO, BROWSE, DISCARDALL, DISCARDALLTIP,
			NOTABLELOADEDMESSAGE, TABLEDISCARDINFO, NEWOPTIONADDEDMESSAGE, CHOOSELOCALFILEFORUPLOADLABEL,
			UPLOADEDFILESLABEL, BUTTONLABELLOADLOCALFILEUPLOADED, UPLOADTABLENAMETIPLABEL, UPLOADNOLOCALFILECHOSENEMESSAGE,
			TABLENAMEINPUTTIP, UPLOADINVALIDTABLENAME, UPLOADDPLICATETABLENAME, LOADUPLOADCLIENTTIP, UPLOADFILEBROWSERLABEL;
//	public static final String FILEPREFIX = "file_";
	private Aladin aladin;
	protected JComboBox<String> uploadOptions;
	protected Map<String, String> uploadTableNameDict = new HashMap<String, String>();
	TapClient uploadClient;
	GridBagConstraints c;
	JLabel infoLabel;
	JTextField systemFile;
	JComboBox<String> uploadAvailableServers;
	JPanel bottomButtonsPanel;

	private JLabel tableNameLabel;

	private JButton editButton;

	private AbstractButton expandCollapse;

	protected void createChaine() {
		TITLE = Aladin.chaine.getString("UPTITLE");
		CLOSE = Aladin.chaine.getString("CLOSE");
		IDENTIFIER = Aladin.chaine.getString("ISIDENTIFIER");
		ERRORMSG = Aladin.chaine.getString("ERROR");
		TAPTABLEUPLOADPARSETIP = Aladin.chaine.getString("TAPTABLEUPLOADPARSETIP");
		UPFILEINFO = Aladin.chaine.getString("UPFILEINFO");
		BROWSE = Aladin.chaine.getString("FILEBROWSE");
		DISCARDALL = Aladin.chaine.getString("DISCARDALL");
		DISCARDALLTIP = Aladin.chaine.getString("DISCARDALLTIP");
		TABLEDISCARDINFO = Aladin.chaine.getString("TABLEDISCARDINFO");
		NEWOPTIONADDEDMESSAGE = Aladin.chaine.getString("NEWOPTIONADDEDMESSAGE");
		CHOOSELOCALFILEFORUPLOADLABEL = Aladin.chaine.getString("CHOOSELOCALFILEFORUPLOADLABEL");
		UPLOADEDFILESLABEL = Aladin.chaine.getString("UPLOADEDFILESLABEL");
		BUTTONLABELLOADLOCALFILEUPLOADED = Aladin.chaine.getString("BUTTONLABELLOADLOCALFILEUPLOADED");
		UPLOADTABLENAMETIPLABEL = Aladin.chaine.getString("UPLOADTABLENAMETIPLABEL");
		UPLOADNOLOCALFILECHOSENEMESSAGE = Aladin.chaine.getString("UPLOADNOLOCALFILECHOSENEMESSAGE");
		TABLENAMEINPUTTIP = Aladin.chaine.getString("TABLENAMEINPUTTIP");
		UPLOADINVALIDTABLENAME = Aladin.chaine.getString("UPLOADINVALIDTABLENAME");
		UPLOADDPLICATETABLENAME = Aladin.chaine.getString("UPLOADDPLICATETABLENAME");
		LOADUPLOADCLIENTTIP = Aladin.chaine.getString("LOADUPLOADCLIENTTIP");
		UPLOADFILEBROWSERLABEL = Aladin.chaine.getString("UPLOADFILEBROWSERLABEL");
	}

	/**
	 * @param url 
	 * @wbp.parser.constructor
	 */
	protected FrameUploadServer(Aladin aladin, String mainServerUrl) {
		super();
		this.aladin = aladin;
		Aladin.setIcon(this);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
		
		this.uploadClient = TapClient.getUploadTapClient(aladin, "LocalResources", mainServerUrl);
		    
		createChaine();
		setTitle(TITLE);
	}
	
	public void setGui() {
		getContentPane().setLayout(new GridBagLayout());
		setLocation(Aladin.computeLocation(this));
		setUploadFileForm();
		aladin.grabUtilInstance.grabItServers.add(uploadClient.serverTap);
	}
	
	/** Affichage des infos associées à un serveur */
	protected void show(TapClient tapClient) {
		setTitle("Upload to "+tapClient.tapLabel);
//		boolean addServer = true;
//		for (int i = 0; i < uploadAvailableServers.getItemCount(); i++) {
//			if (uploadAvailableServers.getItemAt(i).equalsIgnoreCase(tapClient.tapLabel)) {
//				addServer = false;
//			}
//		}
//		if (addServer) {
//			uploadAvailableServers.addItem(tapClient.tapLabel);
//		}
//		uploadAvailableServers.setSelectedItem(tapClient.tapLabel);
		uploadClient.tapBaseUrl = tapClient.tapBaseUrl;
		
		setFont(Aladin.PLAIN);
		pack();
		setVisible(true);
	}
	
	/**
	 * Creates the first form for loading upload data
	 */
	public void setUploadFileForm() {
		this.getContentPane().setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
		c = new GridBagConstraints();
		c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 3; 
	    c.fill = GridBagConstraints.NONE;
	    c.anchor = GridBagConstraints.LINE_START;
	    c.insets = new Insets(10, 10, 5, 5);
	    
	 // Premiere indication
		JLabel l = new JLabel(UPFILEINFO);
		this.getContentPane().add(l, c);

		/*c.gridy++;
		c.gridx = 1;
		c.insets = new Insets(1,1,1,1);
		c.weightx = 0.06;
		this.getContentPane().add(new JLabel("Upload server:"), c);
		
		c.gridx = 2;
		c.gridwidth = 1;
		c.insets = new Insets(1, 1, 1, 1);
		uploadAvailableServers = new JComboBox<String>();
		uploadAvailableServers.setToolTipText("Change upload server");
	    uploadAvailableServers.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				Server sourceServerSelected = TapManager.getTapServerForLabel((String) uploadAvailableServers.getSelectedItem());
				if (sourceServerSelected != null) {
					show(sourceServerSelected.tapClient);
				}
			}
		});
		this.getContentPane().add(uploadAvailableServers, c);*/
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		c.insets = new Insets(1, 10, 1, 1);
		c.weightx = 0.04;
		this.getContentPane().add(new JLabel(CHOOSELOCALFILEFORUPLOADLABEL), c);
		
		c.gridx = 1;
		c.weightx = 0.93;
		c.insets = new Insets(1, 1, 1, 1);
		systemFile = new JTextField();
		systemFile.setPreferredSize(new Dimension(240, Server.HAUT));
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.getContentPane().add(systemFile, c);
		
		// Pour s'aider d'une boite de recherche
		if (Aladin.STANDALONE) {
			JButton browse = new JButton(BROWSE);
			browse.addActionListener(this);
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(1, 3, 1, 5);
			c.weightx = 0.03;
			c.gridx = 2;
			this.getContentPane().add(browse, c);
		}

		c.gridx = 0;
		c.gridwidth = 3;
		c.gridy++;
		c.insets = new Insets(1, 3, 20, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		JButton button = new JButton(BUTTONLABELLOADLOCALFILEUPLOADED);
		button.setActionCommand(UPLOAD);
		button.addActionListener(this);
		button.setToolTipText(TAPTABLEUPLOADPARSETIP); 
		this.getContentPane().add(button, c);
		
		c.gridx = 0;
		c.gridy++;
		infoLabel = new JLabel();
		infoLabel.setFont(Aladin.ITALIC);
		c.insets = new Insets(1, 10, 1, 1);
		this.getContentPane().add(infoLabel, c);
		
		
		JPanel uploadableVoTablesPanel = getAllUploadableVoTablesPanel();
		c.gridy++;
		c.gridwidth = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		this.getContentPane().add(uploadableVoTablesPanel, c);
		pack();
	}
	
	/**
	 * Method adds the gui for selection of files already loaded in Aladin.
	 * @param containerPanel
	 * @param canUploadVOTablesNames
	 * @param radioGroup
	 * @return 
	 */
	public JPanel getAllUploadableVoTablesPanel() {
		JPanel containerPanel = new JPanel(new GridBagLayout());
		containerPanel.setBorder(BorderFactory.createTitledBorder(UPLOADEDFILESLABEL));
		GridBagConstraints c = new GridBagConstraints();
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		c.weightx = 0.48;
		c.insets = new Insets(2, 10, 5, 2);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		boolean hasUploaded = false;
		
		initUploadParameters();
		Vector<String> canUploadVOTablesNames = new Vector<String>();
		canUploadVOTablesNames.addAll(this.uploadTableNameDict.keySet());
		
		if (canUploadVOTablesNames.isEmpty()) {
			uploadOptions  = new JComboBox<String>();
		} else {
			uploadOptions  = new JComboBox<String>(canUploadVOTablesNames);
			hasUploaded = true;
		}
		uploadOptions.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				setStateForUploadedComponents();
			}
		});
		uploadOptions.setEnabled(hasUploaded);
		uploadOptions.setOpaque(false);
		containerPanel.add(uploadOptions, c);
		
		c.gridx = 1;
		c.weightx = 0.50;
		c.insets = new Insets(1,5,1,1);
		c.fill = GridBagConstraints.NONE;
		tableNameLabel = new JLabel(UPLOADTABLENAMETIPLABEL);//as
		containerPanel.add(tableNameLabel, c);
		
		c.gridx = 2;
		c.weightx = 0.01;
		c.insets = new Insets(1,1,1,2);
		editButton = new JButton("EDIT");
		editButton.setActionCommand(EDITUPLOADTABLENAMEACTION);
		editButton.addActionListener(this);
		containerPanel.add(editButton, c);
		
		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		c.insets = new Insets(10, 10, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.WEST;
//		JButton button = new JButton(">> load this table on tap client");
		Image image = Aladin.aladin.getImagette("Expand.png");
		if (image != null) {
			expandCollapse =  new JButton(new ImageIcon(image));
		} else {
			expandCollapse = new JButton(">> load this table on tap client");
		}
		expandCollapse.setToolTipText(LOADUPLOADCLIENTTIP);
		expandCollapse.setMargin(new Insets(0, 0, 0, 0));
		expandCollapse.setBorderPainted(false);
		expandCollapse.setContentAreaFilled(false);
		expandCollapse.setActionCommand(UPLOAD);
		expandCollapse.addActionListener(this);
		containerPanel.add(expandCollapse, c);
		
		setStateForUploadedComponents();
		return containerPanel;
	}
	
	public void setStateForUploadedComponents() {
		// TODO Auto-generated method stub
		boolean hasUploaded = false;
		if (this.uploadOptions.getItemCount() > 0) {
			hasUploaded = true;
			String tableNameTip = UPLOADTABLENAMETIPLABEL+ SPACESTRING;
			String tableName = this.uploadTableNameDict.get(uploadOptions.getSelectedItem());
			tableNameTip = tableNameTip + tableName;
			tableNameLabel.setText(tableNameTip);
		}
		this.uploadOptions.setEnabled(hasUploaded);
		tableNameLabel.setVisible(hasUploaded);
		editButton.setVisible(hasUploaded);
		expandCollapse.setVisible(hasUploaded);
	}

	/**
	 * Gets all the plane names that can be used for upload queries (votables). 
	 * @return
	 */
	public Vector<String> initUploadParameters() {
		PlanCatalog planCatalog = null;
		Vector<String> canUploadVOTablesNames = new Vector<String>();
		Plan[] plan = this.aladin.calque.plan;
		for (int i = 0; i < plan.length; i++) {
			if (plan[i].flagOk/*plan[i].error == null*/ && plan[i].pcat != null && plan[i] instanceof PlanCatalog && plan[i].pcat.flagVOTable) {
				canUploadVOTablesNames.add(plan[i].label);
				String uploadTableName = generateSuffix();//UPLOADTABLEPREFIX.concat(TapTable.getQueryPart(uploadFilesName, false));
				this.uploadTableNameDict.put(plan[i].label, uploadTableName);
				try {
					this.allowPlanIntoUploadFacade(plan[i]);
					planCatalog = (PlanCatalog) plan[i];
					updateUploadGuiWithNewUpload(planCatalog);
				} /*catch (RejectedExecutionException ex) { //no warning at init/drag drop plane load. warning of why a plane cannot be used for upload is done when it is loaded specifically from upload gui. 
					Aladin.error(this.uploadFrame, "Unable to get load "+newPlan.label+"\n Request overload! Please wait and try again.");
				}*/  catch (Exception e) {
					// TODO Auto-generated catch block
					if(Aladin.levelTrace >= 3) e.printStackTrace();
					Aladin.trace(3, "Unable to parse " + plan[i].label + " data for upload");
				}
			}
		}
		return canUploadVOTablesNames;
	}
	
	/**
	 * Appends upload parameters for http request
	 * @param referencedUploadTables
	 * @param requestParams
	 * @throws Exception 
	 */
	public void addUploadToSubmitParams(List<String> referencedUploadTables, Map<String, Object> requestParams) throws Exception {
		StringBuffer uploadParam = null;
		for (String referencedTable : referencedUploadTables) {
			if (this.uploadTableNameDict.containsValue(referencedTable)) {
				String uploadFileName = null;
				for (Entry<String, String> entry : this.uploadTableNameDict.entrySet()) {
					if (entry.getValue().equals(referencedTable)) {
						uploadFileName = entry.getKey();
						break;
					}
				}
				if (uploadFileName != null) {
					if (uploadParam == null) {
						uploadParam = new StringBuffer();
					} else {
						uploadParam.append(";");
					}
					uploadParam.append(getUploadParam(referencedTable, uploadFileName));
				}
				
				if (requestParams!= null && uploadParam != null && uploadFileName != null) {
					requestParams.put("UPLOAD", uploadParam.toString());
					
					final File tmpFile;
					if ((tmpFile = aladin.createTempFile(SAMPUtil.sanitizeFilename(referencedTable), ".xml")) == null) {
						// TODO:: tintin when doing join need to send more files
						Aladin.trace(3, "ERROR in aladin.createTempFile for "+uploadFileName);
						throw new Exception("Unable to parse " + uploadFileName + " data for upload!");
					}
					if (aladin.save == null)
						aladin.save = new Save(aladin);
					
					Plan plan = aladin.calque.getPlan(uploadFileName, 1);
					PlanCatalog planCatalog = (PlanCatalog) plan;
					aladin.save.saveCatVOTable(tmpFile, planCatalog, false, false);
					tmpFile.deleteOnExit();
					requestParams.put(uploadFileName, tmpFile);
				}
			}
		}
	}
	
	protected String getUploadParam(String tableName, String fileName) {
		return tableName.replace(UPLOADTABLEPREFIX, EMPTYSTRING).concat(",param:").concat(fileName);
	}
	
	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		
		if (command.equals("SUBMIT")) {
			if (!uploadTableNameDict.containsValue(uploadClient.serverTap.selectedTableName)) {
				Aladin.error(this.getContentPane(), "Unable to submit " + uploadClient.serverTap.selectedTableName + " data!");
				return;
			}
//			addUploadToSubmitParams(uploadClient.serverTap.selectedTableName, requestParams);
			
			uploadClient.serverTap.submit();
			TapManager.getInstance(aladin).eraseNotification(this.infoLabel, "Submitting your query for table: "+uploadClient.serverTap.selectedTableName, EMPTYSTRING);
		} else if (command.equals(UPLOAD)) {
			//Just parse the selected table's metadata to create gui and store file version of it
			if (checkInputs()) {
				return;
			}
			try {
				Plan loadingPlan = aladin.calque.createPlan(systemFile.getText().trim(), "localTableData", null, uploadClient.serverTap);
				loadingPlan.addPlaneLoadListener(this);
				if (loadingPlan instanceof PlanFree) {
					Aladin.error(this.getContentPane(), "Unable to upload " + systemFile.getText().trim() + " data!");
					return;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				if(Aladin.levelTrace >= 3) e.printStackTrace();
				Aladin.error(this.getContentPane(), "Error unable upload your data!\n"+e.getMessage());
			}
			pack();
		} else if (command.equals(BROWSE)) {
			browseFile();
			return;
		} else if (command.equals(CLOSE)) {
			setVisible(false);
		} else if (command.equals(EDITUPLOADTABLENAMEACTION)) {
			JTextField userInput = new JTextField();
			JLabel tip = new JLabel(TABLENAMEINPUTTIP);

			Object[] uploadTableName = { "Upload table name:", userInput, tip };
			int option = JOptionPane.showConfirmDialog(this.getContentPane(), uploadTableName, "Edit upload table name",
					JOptionPane.OK_CANCEL_OPTION);
			if (option == JOptionPane.OK_OPTION) {

				String tableNameConstr = userInput.getText();
				if (tableNameConstr.isEmpty() || !isTableNameValid(tableNameConstr)) {
					Aladin.error(this.getContentPane(), UPLOADINVALIDTABLENAME);
					return;
				}
				tableNameConstr = UPLOADTABLEPREFIX + TapTable.getQueryPart(tableNameConstr, false);
				if (tableAlreadyExists(tableNameConstr)) {
					Aladin.error(this.getContentPane(), UPLOADDPLICATETABLENAME);
					return;
				}

//				String fileName = (String) uploadOptions.getSelectedItem();
				String fileName = getCorrespondingUploadTableName((JButton) evt.getSource());
				String oldTableName = this.uploadTableNameDict.get(fileName);
				this.uploadTableNameDict.put(fileName, tableNameConstr);
				setStateForUploadedComponents();
				TapManager tapManager = TapManager.getInstance(aladin);
				tapManager.changeUploadTableName(oldTableName, tableNameConstr, this.uploadClient.tablesMetaData);
				tapManager.eraseNotification(this.infoLabel,
						"New table name: " + tableNameConstr + ", set for " + fileName, EMPTYSTRING);
				// this.uploadClient.serverTap.
			}

		}
	}
	
	public String getCorrespondingUploadTableName(JButton button) {
		String result = null;
		JPanel wrapper = (JPanel) SwingUtilities.getAncestorOfClass(JPanel.class, button);
		if (wrapper != null) {
			componentLoop:
			for (Component component : wrapper.getComponents()) {
				if (component instanceof JComboBox && component.getName().equals(TABLEGUINAME)) {
					JComboBox tablesGui = (JComboBox) component;
					String tableName = (String) tablesGui.getSelectedItem();
					if (this.uploadTableNameDict.containsValue(tableName)) {
						for (Entry<String, String> entry : this.uploadTableNameDict.entrySet()) {
							if (entry.getValue().equals(tableName)) {
								result = entry.getKey();
								break componentLoop;
							}
						}
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public void planeLoaded(PlaneLoadEvent ple) {
		if (ple.status != PlaneLoadEvent.SUCCESS || 
				!(ple.plane.flagOk && ple.plane.pcat != null && ple.plane.pcat.flagVOTable)){
			Aladin.trace(3, "Cannot load " + ple.plane.label + " data for upload. Error!");
			Aladin.error(this.getContentPane(), "Cannot load " + ple.plane.label + " data for upload. Error!");
		}
		
	}
	
	/**
	 * Method adds request param file(created again from PlanCatalog) into cache for the upload table
	 * File name = UPLOADFILEPREFIX+uploadTableName.xml
	 * @param uploadTableName
	 * @param planCatalog
	 * @throws Exception
	 */
	public void allowPlanIntoUploadFacade(Plan plan) throws Exception {
		if (!(plan instanceof PlanCatalog)) {
			throw new Exception("Cannot parse " + plan.label + " data for upload. Please select a catalog!");
		}
	         
//		String uploadTableName = UPLOADTABLEPREFIX.concat(TapTable.getQueryPart(plan.label, false));
	    String uploadTableName = generateSuffix();
		this.uploadTableNameDict.put(plan.label, uploadTableName);
	}
	
	public void updateUploadGuiWithNewUpload(PlanCatalog planCatalog) {
		if (this.uploadTableNameDict.containsKey(planCatalog.label)) {
			TapManager tapManager = TapManager.getInstance(aladin);
			String uploadTableName = this.uploadTableNameDict.get(planCatalog.label);
//			tapManager.createTapServerFromAladinPlan(planCatalog, uploadTableName); //plan and ui creation removed. refer to svn v10994 TapManager version to get the method
			tapManager.eraseNotification(this.infoLabel, "New table(Name: "+uploadTableName+") from "+planCatalog.label+" is parsed in Aladin!", EMPTYSTRING);
		}
		
	}
	
	public void deleteAvailableUploadTable(Plan planInDeletion) {
		if (planInDeletion.pcat != null && planInDeletion.pcat.flagVOTable) {
			TapManager tapManager = TapManager.getInstance(aladin);
			String tableToDiscard = this.uploadTableNameDict.get(planInDeletion.label);
			this.uploadClient.tablesMetaData.remove(tableToDiscard);
			boolean enable = false;
			if (this.uploadClient.tablesMetaData != null && !this.uploadClient.tablesMetaData.isEmpty()) {
				enable = true;
//				this.uploadClient.serverTap.tablesGui.removeItem(tableToDiscard);
				tapManager.uploadTablesModel.removeElement(tableToDiscard);
			}
			if (!enable) {
				this.clearBottomPanel();
			}
			this.uploadTableNameDict.remove(planInDeletion.label);
			this.setStateForUploadedComponents();
			if (this.isVisible()) {
				TapManager.getInstance(aladin).eraseNotification(this.infoLabel, FrameUploadServer.TABLEDISCARDINFO,
						EMPTYSTRING);
				this.pack();
			}
		}
	}
	
	protected void createUploadServer() {
		uploadClient.preprocessTapClient();
		uploadClient.serverTap.setOpaque(true);
		uploadClient.serverTap.createFormDefault(); //default choice is the first table
		
		c.insets = new Insets(5, 10, 10, 5);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		c.weighty = 0.94;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTH;
		this.getContentPane().add(uploadClient.serverTap, c);
		
		c.gridy++;
		c.weighty = 0.01;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.SOUTH;
		bottomButtonsPanel = new JPanel();
		JButton submit = new JButton("Submit");
		submit.addActionListener(this);
		submit.setActionCommand("SUBMIT");
		bottomButtonsPanel.add(submit);
		this.getContentPane().add(bottomButtonsPanel, c);
		
		uploadClient.serverTap.revalidate();
		uploadClient.serverTap.repaint();
		
	}
	
	/**
	 * removes all the uploaded table gui
	 */
	public void clearBottomPanel() {
		this.uploadClient.tablesMetaData.clear();
		this.uploadClient.serverTap.formLoadStatus = TAPFORM_STATUS_NOTLOADED;
		if (this.uploadClient.serverTap.tablesGui != null) {
			this.uploadClient.serverTap.tablesGui.removeAllItems();
		}
		this.uploadClient.serverTap.removeAll();
		this.remove(this.uploadClient.serverTap);
		this.remove(this.bottomButtonsPanel);
		pack();
	}
	
	protected boolean checkInputs() {
		boolean hasError = true;
		if (systemFile.getText().trim().isEmpty()) {
			Aladin.error(this.getContentPane(), UPLOADNOLOCALFILECHOSENEMESSAGE);
		} else {
			hasError = false;
		}
//		if (selectedFile == -1) {
//			Aladin.error(this.getContentPane(), "Please select your upload data!");
//		} else if (tableName.getText().isEmpty()  ||  !isTableNameValid(tableName.getText())) {
//			Aladin.error(this.getContentPane(), "Please input a valid table name (with no special characters)!");
//		} else if (tableAlreadyExists(tableName.getText())) {
//			Aladin.error(this.getContentPane(), "This table name is already submitted for upload. Please edit the table name!");
//		} else if (selectedFile == 0 && systemFile.getText().trim().isEmpty() ) {
//			Aladin.error(this.getContentPane(), "Please choose a file!");
//		} else if (selectedFile == 1 && uploadOptions.getSelectedItem()==null ) {
//			Aladin.error(this.getContentPane(), "Please choose a file!");
//		} else {
//			hasError = false;
//		}
		return hasError;
	}
	
	/** Ouverture de la fenêtre de sélection d'un fichier */
	   protected void browseFile() {
	      String path = Util.dirBrowser(UPLOADFILEBROWSERLABEL, aladin.getDefaultDirectory(), systemFile, 2);
	      if( path == null ) return;

	      String dir = path;
	      int offset = path.indexOf(" ");
	      if( offset > 0 ) {
	         Tok tok = new Tok(path," ");
	         dir = tok.nextToken();
	      }
	      
	      File f = new File(dir);
	      if( !f.isDirectory() ) dir = f.getParent();
	      aladin.memoDefaultDirectory(dir);
	   }
	
	   
   /**
    * Method checks if table name is already used
    * @param input
    * @return
    */
	public boolean tableAlreadyExists(String input) {
		return (this.uploadTableNameDict.containsValue(input));
	}
	
	/**
	 * Method checks if the tablename is valid.
	 * @param input
	 * @return
	 */
	public boolean isTableNameValid(String input) {
		boolean result = false;
		Pattern regex = Pattern.compile(REGEX_ONLYALPHANUM);//find no special chars
		Matcher matcher = regex.matcher(input);
		if (!matcher.find()){
			regex = Pattern.compile(REGEX_ALPHA);//find atleast one alphabet
			matcher = regex.matcher(input);
			if (matcher.find()){
				result = true;
			}			
		}
		
		return result;
	}
	
	/**
	 * Method to generate a unique <i>upload</i> table name
	 * @param uploadFrame
	 * @return
	 */
	public String generateUploadTableName(String prefix) {
		String uploadTableName = prefix + new Random().nextInt(Integer.SIZE - 1);
		if (!tableAlreadyExists(uploadTableName)) {
			return uploadTableName;
		}
		return generateUploadTableName(prefix);
	}
	
	/**
	 * Method to generate a unique suffix for an <i>upload</i> table name
	 * @param uploadFrame
	 * @return
	 */
	private String generateSuffix() {
		String uploadTableSuffix = "AladinTable" + new Random().nextInt(Integer.SIZE - 1);
		String uploadTableName = "TAP_UPLOAD.".concat(uploadTableSuffix);
		if (!tableAlreadyExists(uploadTableName)) {
			return uploadTableName;
		}
		return generateSuffix();
	}

	@Override
	public void setGrabItCoord(double x, double y) {
		GrabUtil.setGrabItCoord(aladin, uploadClient.serverTap, x, y);
	}

	@Override
	public void stopGrabIt() {
	    GrabUtil.stopGrabIt(aladin, this, uploadClient.serverTap);
	}
	
	/**
	    * Retourne true si le bouton grabit du formulaire existe et qu'il est
	    * enfoncé
	    */
	@Override
	public boolean isGrabIt() {
	      return (uploadClient.serverTap.modeCoo != Server.NOMODE
	            && uploadClient.serverTap.grab != null && uploadClient.serverTap.grab.getModel().isSelected());
	   }

	@Override
	public void setGrabItRadius(double x1, double y1, double x2, double y2) {
		GrabUtil.setGrabItRadius(aladin, uploadClient.serverTap, x1, y1, x2, y2);
	}


}
