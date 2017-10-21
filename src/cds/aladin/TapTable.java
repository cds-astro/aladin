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

import static cds.aladin.Constants.ACCESSURL;
import static cds.aladin.Constants.ACCESS_ESTSIZE;
import static cds.aladin.Constants.ACCESS_FORMAT;
import static cds.aladin.Constants.BIBCODE;
import static cds.aladin.Constants.DATAPRODUCT_TYPE;
import static cds.aladin.Constants.DEC;
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
import static cds.aladin.Constants.RA;
import static cds.aladin.Constants.RADIALVELOCITY;
import static cds.aladin.Constants.REDSHIFT;
import static cds.aladin.Constants.REGEX_TABLENAME_SPECIALCHAR;
import static cds.aladin.Constants.T_MAX;
import static cds.aladin.Constants.T_MIN;
import static cds.aladin.Constants.UCD_DEC_PATTERN2;
import static cds.aladin.Constants.UCD_DEC_PATTERN3;
import static cds.aladin.Constants.UCD_RA_PATTERN2;
import static cds.aladin.Constants.UCD_RA_PATTERN3;
import static cds.aladin.Constants.REGEX_VALIDTABLENAME;
import static cds.aladin.Constants.REGEX_VALIDTABLEPREFIX;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adql.db.DefaultDBTable;

/**
 * Class is a model class for columns of tap services
 * @author chaitra
 *
 */
public class TapTable {
	
	private String schema_name;
	private String table_name;
	private String table_type;
	private String description;
	private String utype;
	private Vector<TapTableColumn> columns;
	public Map<String, TapTableColumn> flaggedColumns;
	public Map<String, String> obsCoreColumns;
	public static final int MAXOBSCORECOLSCOUNTED = 6;
	
	/**
	 * Identifies certain columns
	 * @param table
	 * @param column
	 */
	public synchronized void parseUcds(TapTableColumn column) {
		// some nice select statements..also set param names
		String ucd = column.getUcd();
		if (ucd != null && !ucd.isEmpty()) {
			ucd = ucd.toLowerCase();
			if (ucd.startsWith(UCD_RA_PATTERN2) || ucd.equalsIgnoreCase(UCD_RA_PATTERN3)) {
				this.setFlaggedColumn(RA, column);
			} else if (ucd.startsWith(UCD_DEC_PATTERN2) || ucd.equalsIgnoreCase(UCD_DEC_PATTERN3)) {
				this.setFlaggedColumn(DEC, column);
			} else if (ucd.startsWith("pos.parallax")) {
				this.setFlaggedColumn(PARALLAX, column);
			} else if (ucd.startsWith("spect.dopplerveloc")) {
				this.setFlaggedColumn(RADIALVELOCITY, column);
			} else if (ucd.equals("meta.bib.bibcode")) {
				this.setFlaggedColumn(BIBCODE, column);
			} else if (ucd.equals("meta.bib.journal")) {
				this.setFlaggedColumn(JOURNAL, column);
			} else if (ucd.equals("meta.title")) {
				this.setFlaggedColumn(DOCTITLE, column);
			} else if (ucd.startsWith("pos.pm;pos.eq.ra")) {
				this.setFlaggedColumn(PMRA, column);
			} else if (ucd.startsWith("pos.pm;pos.eq.dec")) {
				this.setFlaggedColumn(PMDEC, column);
			} /*else if (ucd.startsWith("sregion;") && ucd.contains("instr.fov")) {// phys.angSize;instr.fov
				this.setFlaggedColumn(S_REGION, column);
			}*/ else if (ucd.startsWith("meta.id")) {//"meta.id;meta.main"
				this.setFlaggedColumn(MAINID, column);
			} else if (ucd.contains("src.redshift")) {
				this.setFlaggedColumn(REDSHIFT, column);
			} else if (ucd.startsWith("phot.flux")) {
				this.setFlaggedColumn(MAG, column);
			} /*else if (ucd.startsWith("src.class")) {
				this.setFlaggedColumn(SRCCLASS, column);
			} */ 
		}
	}
	
	public boolean hasObscoreInTheName() {
		boolean result = false;
		Pattern pattern = Pattern.compile("obscore", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(table_name);
		if (matcher.find()) {
			result = true;
		}
		return result;
	}
	
	public boolean isObscore() {
		boolean result = false;
		if (hasObscoreInTheName() && this.obsCoreColumns != null && this.obsCoreColumns.size() > MAXOBSCORECOLSCOUNTED) {
			result = true;
		}
		return result;
	}
	
	public void initObsCoreColumns() {
		if (this.obsCoreColumns == null) {
			obsCoreColumns = new HashMap<String, String>();
		}
	}
	
	/**
	 * Spec is not unambiguously interpreted, after discussing with Laurent
	 * For some servers, table name mapping has a discrepancy between having a fully qualified name versus
	 * just table name. So some mediocre, but necessary steps (like this method) 
	 * will be added to go around this issue
	 * @return
	 */
	public String getFullyQualifiedTableName(String schemaNameAsInSchema, String tableNameAsInSchema) {
		String tableName = null;
		
		if (tableNameAsInSchema != null) {
			if (schemaNameAsInSchema != null) {
				tableName = schemaNameAsInSchema + Constants.DOT_CHAR;
			}
			if (tableName != null) {
				tableName = tableName + tableNameAsInSchema;
			} else {
				tableName = tableNameAsInSchema;
			}
		}
		if (tableName != null) {
			DefaultDBTable table = new DefaultDBTable(tableName);
			if (table.getADQLSchemaName() != null) {
				tableName = table.getADQLSchemaName() + DOT_CHAR;
			}

			if (table.getADQLName() != null) {
				tableName = tableName + table.getADQLName();
			}
		}
		return tableName;
	}
	
	public String getFullyQualifiedTableName() {
		return getFullyQualifiedTableName(this.schema_name, this.table_name);
	}
	
	public String getFullyQualifiedTableName(String tableNameInput) {
		return getFullyQualifiedTableName(this.schema_name, tableNameInput);
	}
	
	public String getAdqlName() {
		String adqlName = null;
		if (this.table_name != null) {
			DefaultDBTable table = new DefaultDBTable(this.table_name);
			if (table.getADQLName() != null) {
				adqlName = table.getADQLName();
			}
		}
		return adqlName;
	}
	
	/**
	 * Method only used for cases of table names with special chars.
	 * Adds double quote to the names
	 * @param tapTable 
	 * @param queryPartInput
	 * @return
	 */
	public static String getQueryPart1(String queryPartInput) {//keeping for reference
		if (queryPartInput != null) {
//			queryPartInput = tapTable.getAdqlName(); //TODO:: tintin : when we add schema name
			Pattern regex = Pattern.compile(REGEX_TABLENAME_SPECIALCHAR);
			/*String[] tableName = queryPartInput.split("\\."); nope. Vizier can have dot inside a table adql name: J/other/BAJ/24.62/table5 
			if (tableName.length > 1) {
				queryPartInput = tableName[tableName.length];
			}*/
			Matcher matcher = regex.matcher(queryPartInput);
			if (matcher.find()){
				queryPartInput = Glu.doubleQuote(queryPartInput);
			}
//			queryPartInput = tapTable.getFullyQualifiedTableName(queryPartInput); //if add schema name
		}
		
		return queryPartInput;
	}
	
	public static String getQueryPart(String queryPartInput, boolean isForTableName) {
//		 String queryPartInput = "J/other./BAJ/24.62/table5";
		if (queryPartInput != null) {
			Pattern regex = Pattern.compile(REGEX_VALIDTABLENAME);
			/*String[] tableName = queryPartInput.split("\\."); nope. Vizier can have dot inside a table adql name: J/other/BAJ/24.62/table5 
			if (tableName.length > 1) {
				queryPartInput = tableName[tableName.length];
			}*/
			Matcher matcher = regex.matcher(queryPartInput);
			if (!matcher.find()){
				if (!isForTableName) {
					queryPartInput = Glu.doubleQuote(queryPartInput);
				} else {
					String prefix = EMPTYSTRING;
					String potentialTableName = queryPartInput;
					Pattern prefixPattern = Pattern.compile(REGEX_VALIDTABLEPREFIX);
					matcher = prefixPattern.matcher(queryPartInput);
					if (matcher.find()){
						prefix = matcher.group("prefix");
						potentialTableName = queryPartInput.replaceFirst(prefix, EMPTYSTRING);
					}
					matcher = regex.matcher(potentialTableName);
					if (!matcher.find()){
						queryPartInput = Glu.doubleQuote(potentialTableName);
						queryPartInput = prefix+queryPartInput;
					}
				}
			}
		} 
			 return queryPartInput;
	}
	
	public synchronized void parseForObscore(boolean isUpload, TapTableColumn columnMeta) {
		if (isUpload || hasObscoreInTheName()) {
			initObsCoreColumns();
			int mandatoryColumnCount = 0;
			
			//17 mandatory columns counted
			String name = columnMeta.getColumn_name();
			String utype = columnMeta.getUtype();
			if (name == null && utype == null) {
				return;
			}
			if (name == null) {
				name = EMPTYSTRING;
			}
			if (utype == null) {
				utype = EMPTYSTRING;
			} else {
				utype = getNoNamesSpaceUtype(utype);
			}
			synchronized (obsCoreColumns) {
			if (utype.equalsIgnoreCase("ObsDataset.dataProductType") || name.equalsIgnoreCase("dataproduct_type")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(DATAPRODUCT_TYPE, name);
			} else if (utype.equalsIgnoreCase("DataID.observationID") || name.equalsIgnoreCase("obs_id")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(OBSID, name);
			} /*else if (utype.equals("obscore:DataID.title") || name.equalsIgnoreCase("obs_title")) {
			} else if (utype.equals("obscore:Curation.reference") || name.equalsIgnoreCase("bib_reference")) {
			}*/ else if (utype.equalsIgnoreCase("Access.reference") || name.equalsIgnoreCase("access_url")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ACCESSURL, name);
			} else if (utype.equalsIgnoreCase("Access.format") || name.equalsIgnoreCase("access_format")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ACCESS_FORMAT, name);
			} else if (utype.equalsIgnoreCase("Access.size") || name.equalsIgnoreCase("access_estsize")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ACCESS_ESTSIZE, name);
			} else if (utype.equalsIgnoreCase("Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C1")
					|| name.equalsIgnoreCase("s_ra")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(RA, name);
			} else if (utype.equalsIgnoreCase("Char.SpatialAxis.Coverage.Location.Coord.Position2D.Value2.C2")
					|| name.equalsIgnoreCase("s_dec")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(DEC, name);
			} else if (utype.equalsIgnoreCase("Char.SpatialAxis.Coverage.Support.Area") || name.equals("s_region")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ServerObsTap.FIELDSIZE, name);
			} else if (utype.equalsIgnoreCase("Char.SpatialAxis.Resolution.refval")
					|| name.equalsIgnoreCase("s_resolution")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ServerObsTap.SPATIALRESOLUTION, name);
			} else if (utype.equalsIgnoreCase("Char.TimeAxis.Coverage.Bounds.Limits.StartTime")
					|| name.equalsIgnoreCase("t_min")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(T_MIN, name);
			} else if (utype.equalsIgnoreCase("Char.TimeAxis.Coverage.Bounds.Limits.StopTime")
					|| name.equalsIgnoreCase("t_max")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(T_MAX, name);
			} else if (utype.equalsIgnoreCase("Char.TimeAxis.Coverage.Support.Extent")
					|| name.equalsIgnoreCase("t_exptime")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ServerObsTap.EXPOSURETIME, name);
			} else if (utype.equalsIgnoreCase("Char.TimeAxis.Resolution.Refval.valueResolution.Refval.value")
					|| name.equalsIgnoreCase("t_resolution")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ServerObsTap.TIMERESOLUTION, name);
			} else if (utype.equalsIgnoreCase("Char.SpectralAxis.Coverage.Bounds.Limits.LoLimit")
					|| name.equalsIgnoreCase("em_min")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(EM_MIN, name);
			} else if (utype.equalsIgnoreCase("Char.SpectralAxis.Coverage.Bounds.Limits.HiLimit")
					|| name.equalsIgnoreCase("em_max")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(EM_MAX, name);
			} else if (utype.equalsIgnoreCase("Char.SpectralAxis.Resolution.Refval.value")
					|| name.equalsIgnoreCase("em_res")) {
				obsCoreColumns.put(ServerObsTap.SPECTRALRESOLUTION, name);
			} else if (utype.equalsIgnoreCase("Char.SpectralAxis.Resolution.ResolPower.refVal")
					|| name.equalsIgnoreCase("em_res_power")) {
				mandatoryColumnCount++;
				obsCoreColumns.put(ServerObsTap.SPECTRALRESOLUTIONPOWER, columnMeta.getColumn_name());
			} /*else if (utype.equals("obscore:Char.SpectralAxis.ucd") || name.equalsIgnoreCase("em_ucd")) {
			} else if (utype.equals("obscore:Char.ObservableAxis.ucd") || name.equalsIgnoreCase("o_ucd")) {
				mandatoryColumnCount++;
			} else if (utype.equals("obscore:Char.ObservableAxis.unit") || name.equalsIgnoreCase("o_unit")) {
			} else if (utype.equals("obscore:Char.PolarizationAxis.stateList")
					|| name.equalsIgnoreCase("pol_states")) {
			}*/
		
		}
		}
	}

	private String getNoNamesSpaceUtype(String utype) {
		// TODO Auto-generated method stub
		String result = utype;
		if (utype.startsWith("obscore:")) {
			result = result.replace("obscore:", EMPTYSTRING);
		}
		return result;
	}

	/**
	 * Selects the main out of the columns to be identified
	 * @param flaggedColumns
	 * @param key
	 * @param col2
	 */
	public void compareAddMainFlaggedColumn(Map<String, TapTableColumn> flaggedColumns, String key,
			TapTableColumn col2) {
		// TODO Auto-generated method stub
		TapTableColumn col1 = flaggedColumns.get(key);
		if (!col1.isDefinedMain() && col2.isDefinedMain()) {
			flaggedColumns.put(key, col2);
		}
	}
	
	public void setFlaggedColumn(String key, TapTableColumn flaggedColumn) {
		initFlaggedColumns();
		if (this.flaggedColumns.containsKey(key)) {
			compareAddMainFlaggedColumn(this.flaggedColumns, key, flaggedColumn);
		} else {
			this.flaggedColumns.put(key, flaggedColumn);
		}
	}
	
	public void removeFlaggedColumn(String key) {
		initFlaggedColumns();
		if (this.flaggedColumns.containsKey(key)) {
			this.flaggedColumns.remove(key);
		}
	}
	
	public String getFlaggedColumnName(String key) {
		String result = null;
		if (this.flaggedColumns != null && this.flaggedColumns.containsKey(key)) {
			result = this.flaggedColumns.get(key).getColumn_name();
		}
		return result;
	}
	
	public TapTableColumn getFlaggedColumn(String key) {
		TapTableColumn result = null;
		if (this.flaggedColumns != null && this.flaggedColumns.containsKey(key)) {
			result = this.flaggedColumns.get(key);
		}
		return result;
	}
	
	public String getObsColumnNameForQuery(String key) {
		String result = getObsColumnName(key);
		if (result != null) {
			result = TapTable.getQueryPart(result, false);
		}
		return result;
	}
	
	public String getObsColumnName(String key) {
		String result = null;
		if (this.obsCoreColumns != null && this.obsCoreColumns.containsKey(key)) {
			result = this.obsCoreColumns.get(key);
		}
		return result;
	}
	
	public String getRaColumnName() {
		return getFlaggedColumnName(RA);
	}

	public String getDecColumnName() {
		return getFlaggedColumnName(DEC);
	}
	
	/**
	 * directly sets and changes- w.n.r.t exxiting
	 * @param flaggedColumn
	 */
	public void setRaColumn(TapTableColumn flaggedColumn) {
		if (flaggedColumn == null) {
			removeFlaggedColumn(RA);
		} else {
//			setFlaggedColumn(RA, flaggedColumn);
			initFlaggedColumns();
			this.flaggedColumns.put(RA, flaggedColumn);
		}
		
	}

	public void setDecColumn(TapTableColumn flaggedColumn) {
		if (flaggedColumn == null) {
			removeFlaggedColumn(DEC);
		} else {
			initFlaggedColumns();
			setFlaggedColumn(DEC, flaggedColumn);
		}
	}

	public String getSchema_name() {
		return schema_name;
	}
	
	public void setSchema_name(String schema_name) {
		this.schema_name = schema_name;
	}
	
	public String getTable_name() {
		return table_name;
	}
	
	public void setTable_name(String table_name) {
		this.table_name = table_name;
	}
	
	public String getTable_type() {
		return table_type;
	}
	
	public void setTable_type(String table_type) {
		this.table_type = table_type;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getUtype() {
		return utype;
	}
	
	public void setUtype(String utype) {
		this.utype = utype;
	}

	public Vector<TapTableColumn> getColumns() {
		return columns;
	}

	public void setColumns(Vector<TapTableColumn> columns) {
		this.columns = columns;
	}

	public Map<String, TapTableColumn> getFlaggedColumns() {
		return flaggedColumns;
	}
	
	public void initFlaggedColumns() {
		if (this.flaggedColumns == null) {
			this.flaggedColumns = new HashMap<String, TapTableColumn>();
		}
	}

	public void setFlaggedColumns(Map<String, TapTableColumn> flaggedColumns) {
		this.flaggedColumns = flaggedColumns;
	}

	public Map<String, String> getObsCoreColumns() {
		return obsCoreColumns;
	}

	public void setObsCoreColumns(Map<String, String> obsCoreColumns) {
		this.obsCoreColumns = obsCoreColumns;
	}
	
}
