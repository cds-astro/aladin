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
import static cds.aladin.Constants.TABLEGUINAME;
import static cds.aladin.Constants.UPLOADTABLEPREFIX;

import java.awt.Component;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


/**
 * This class is maintain tables that can be uploaded to Tap servers.
 * 
 */
public class UploadFacade extends JFrame {

   /**
	 * 
	 */
	private static final long serialVersionUID = 399753558953437543L;

	static String TABLENAMEINPUTTIP, UPLOADINVALIDTABLENAME, UPLOADDPLICATETABLENAME;
	private Aladin aladin;
	protected Map<String, String> uploadTableNameDict = new HashMap<String, String>();
	Map<String, TapTable> uploadTablesMetaData;

	protected void createChaine() {
		TABLENAMEINPUTTIP = Aladin.chaine.getString("TABLENAMEINPUTTIP");
		UPLOADINVALIDTABLENAME = Aladin.chaine.getString("UPLOADINVALIDTABLENAME");
		UPLOADDPLICATETABLENAME = Aladin.chaine.getString("UPLOADDPLICATETABLENAME");
	}

	/**
	 * @param url 
	 * @wbp.parser.constructor
	 */
	protected UploadFacade(Aladin aladin) {
		super();
		this.aladin = aladin;
		createChaine();
		uploadTablesMetaData = new HashMap<String, TapTable>();
		initUploadParameters();
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
	
	public void uploadSettingsClickAction(JButton button) {
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

//			String fileName = (String) uploadOptions.getSelectedItem();
			String fileName = getCorrespondingUploadTableName(button);
			String oldTableName = this.uploadTableNameDict.get(fileName);
			this.uploadTableNameDict.put(fileName, tableNameConstr);
			TapManager tapManager = TapManager.getInstance(aladin);
			tapManager.changeUploadTableName(oldTableName, tableNameConstr, this.uploadTablesMetaData);
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
			String uploadTableName = this.uploadTableNameDict.get(planCatalog.label);
			TapManager.getInstance(aladin).initialiseUploadFacadeFromAladinPlan(planCatalog, uploadTableName);
		}
	}
	
	public void deleteAvailableUploadTable(Plan planInDeletion) {
		if (planInDeletion.pcat != null && planInDeletion.pcat.flagVOTable) {
			TapManager tapManager = TapManager.getInstance(aladin);
			String tableToDiscard = this.uploadTableNameDict.get(planInDeletion.label);
			if (this.uploadTablesMetaData != null && !this.uploadTablesMetaData.isEmpty()) {
				this.uploadTablesMetaData.remove(tableToDiscard);
				tapManager.uploadTablesModel.removeElement(tableToDiscard);
			}
			this.uploadTableNameDict.remove(planInDeletion.label);
		}
	}
	
   /**
    * Method checks if table name is already used
    * @param input
    * @return
    */
	public boolean tableAlreadyExists(String input) {
		return (this.uploadTableNameDict.containsValue(input));
	}
	
	public String getUploadTableName(String planeName) {
		String result = null;
		if (this.uploadTableNameDict.containsKey(planeName)) {
			result = this.uploadTableNameDict.get(planeName);
		}
		return result;
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


}
