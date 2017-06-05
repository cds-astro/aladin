// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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
import static cds.aladin.Constants.EMPTYSTRING;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Vector;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;


/**
 * Model class representing the column information in the tap schema for columns
 * Refer: http://www.ivoa.net/documents/TAP/
 * @author chaitra
 *
 */
public class TapTableColumn {
	private String table_name;
	private String column_name;
	private String description;
	private String unit;
	private String ucd;
	private String utype;
	private String datatype; //adql datatype
	private int size;
	private String isPrincipal; 
	private String isIndexed;	
	private String isStandard;
	
	/**
	 * Method to convert represent all properties as row vectors
	 * @return Vector<String>
	 */
	public Vector<String> getRowVector() {
		Vector<String> allValuesVector = new Vector<String>(10);
		allValuesVector.addElement(this.column_name);
		allValuesVector.addElement(this.description);
		allValuesVector.addElement(this.unit);
		allValuesVector.addElement(this.ucd);
		allValuesVector.addElement(this.utype);
		allValuesVector.addElement(this.datatype);
		if (this.size == -1) {
			allValuesVector.addElement(EMPTYSTRING);
		} else {
			allValuesVector.addElement(String.valueOf(this.size));
		}
		allValuesVector.addElement(this.isPrincipal);
		allValuesVector.addElement(this.isIndexed);
		allValuesVector.addElement(this.isStandard);
		return allValuesVector;

	}
	
	/**
	 * Method returns column names of a table representation of this model class
	 * @return
	 */
	public static Vector<String> getColumnLabels() {
		Vector<String> columnNames = new Vector<String>(10);
		columnNames.addElement("ColumnName");
		columnNames.addElement("Description");
		columnNames.addElement("Unit");
		columnNames.addElement("UCD");
		columnNames.addElement("Utype");
		columnNames.addElement("Datatype");
		columnNames.addElement("Size");
		columnNames.addElement("isPrincipalColumn?");
		columnNames.addElement("isIndexedColumn?");
		columnNames.addElement("isStandardColumn?");
		return columnNames;
	}
	
	public String getTable_name() {
		return table_name;
	}
	public void setTable_name(String table_name) {
		this.table_name = table_name;
	}
	
	public String getColumn_name() {
		return column_name;
	}
	public void setColumn_name(String column_name) {
		this.column_name = column_name;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getUnit() {
		return unit;
	}
	public void setUnit(String unit) {
		this.unit = unit;
	}
	
	public String getUcd() {
		return ucd;
	}
	public void setUcd(String ucd) {
		this.ucd = ucd;
	}
	
	public String getUtype() {
		return utype;
	}
	public void setUtype(String utype) {
		this.utype = utype;
	}
	
	public String getDatatype() {
		return datatype;
	}
	public void setDatatype(String datatype) {
		this.datatype = datatype;
	}
	
	public void setSize(String dataType, String value) {
		this.size = verifyExtractIntString(dataType, value);
	}
	
	/**
	 * Method to verify an int datatype
	 * @param dataType
	 * @param value
	 * @return
	 */
	public int verifyExtractIntString(String dataType, String value) {
		int parsedValue = -1;
		if (dataType != null && value != null && !value.isEmpty() && dataType.equals("int")) {
			try {
				parsedValue = Integer.parseInt(value);
			} catch (Exception e) {
				// TODO: handle exception
				Aladin.trace(3, "unable to parse: value:" + value);
			}
			return parsedValue;
		}
		return parsedValue;
	}
	
	public String getIsPrincipal() {
		return isPrincipal;
	}

	public void setIsPrincipal(String isPrincipal) {
		this.isPrincipal = isPrincipal;
	}

	public String getIsIndexed() {
		return isIndexed;
	}

	public void setIsIndexed(String isIndexed) {
		this.isIndexed = isIndexed;
	}

	public String getIsStandard() {
		return isStandard;
	}

	public void setIsStandard(String isStandard) {
		this.isStandard = isStandard;
	}
	
	/**
	 * To string method for the class
	 * @return
	 */
	@Override
	public String toString() {
		StringBuffer toPrint = new StringBuffer();
		toPrint.append("table name:").append(this.table_name)
		.append(", Column name: ").append(this.column_name)
		.append(", description :").append(this.description)
		.append(", unit :").append(this.unit)
		.append(", ucd :").append(this.ucd)
		.append(", datatype :").append(this.datatype)
		.append(", size :").append(this.size)
		.append(",is principal : ").append(this.isPrincipal)
		.append(", is indexed :").append(this.isIndexed)
		.append(", is standard :").append(this.isStandard);
		return toPrint.toString();
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

}
