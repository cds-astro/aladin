// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

/**
 * 
 */
package cds.aladin;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.REGEX_DIGITS;
import static cds.aladin.Constants.UCD_MAINIDQUALIFIER;
import static cds.aladin.Constants.DBColumnType.BIGINT;
import static cds.aladin.Constants.DBColumnType.CHAR;
import static cds.aladin.Constants.DBColumnType.DOUBLE;
import static cds.aladin.Constants.DBColumnType.INTEGER;
import static cds.aladin.Constants.DBColumnType.REAL;
import static cds.aladin.Constants.DBColumnType.SMALLINT;
import static cds.aladin.Constants.DBColumnType.VARBINARY;
import static cds.aladin.Constants.DBColumnType.VARCHAR;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.xml.Field;


/**
 * Model class representing the column information in the tap schema for columns
 * Refer: http://www.ivoa.net/documents/TAP/
 * @author chaitra
 *
 */
public class TapTableColumn {
	private String table_name;
	private String column_name;
	private String datatype; //adql datatype
//	private String arraysize; 1.1v Will add
	private String xtype; //1.1v not yet added already as v1.1 upgrade is not yet done. but needed datatype logic will be complete
	private int size; //"size" in 1.1v
	private String description;
	private String utype;
	private String unit;
	private String ucd;
	private String isIndexed; //1.1v change to indexed
	private String isPrincipal; //1.1v change to principal
	private String isStandard; //1.1v change to std
//	private String column_index; 1.1v
	
	public TapTableColumn() {
		// TODO Auto-generated constructor stub
	}
	
	public TapTableColumn(TapTableColumn ref, String alais) {
		// TODO Auto-generated constructor stub
		this.table_name = ref.getTable_name()+DOT_CHAR+alais;
		this.column_name = ref.column_name;
		this.description = ref.description;
		this.unit = ref.unit;
		this.ucd = ref.ucd;
		this.utype = ref.utype;
		this.datatype = ref.datatype;
		this.size = ref.size;
		this.isPrincipal = ref.isPrincipal;
		this.isIndexed = ref.isIndexed;
		this.isStandard = ref.isStandard;
	}
	
	/**
	 * Method to convert represent all properties as row vectors
	 * @return Vector<String>
	 */
	public Vector<String> getRowVector() {
		Vector<String> allValuesVector = new Vector<>(10);
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
		Vector<String> columnNames = new Vector<>(10);
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
	
	static public String unquote(String s) {
	   if( s==null ) return s;
	   int n = s.length();
	   if( n>=2 && s.charAt(0)=='"' && s.charAt( n-1 )=='"' ) return s.substring(1,n-1);
	   return s;
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
	
	public String getColumnNameForQuery() {
		return TapTable.getQueryPart(column_name, false);
	}
	
	public void setColumn_name(String column_name) {
		this.column_name = unquote(column_name);
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
	
	/**
	 * Method tries to set the database equivalent type to that of votable parsed in Aladin
	 * @param f
	 * 
	 datatype		Meaning			FITS Bytes	db translation
	"boolean"		Logical			"L"	1	not supported
	"bit"			Bit				"X"	*	donno is it VARBINARY/ unsignedbyte no size
	"unsignedByte"	Byte(0 to 255)	"B"	1	VARBINARY, VARBINARY(n), BINARY(n), BLOB
	"short"			Short Integer	"I"	2	smallint
	"int"			Integer			"J"	4	INTEGER
	"long"			Long integer	"K"	8	BIGINT
	"char"			ASCII Character	"A"	1	If arraysize is 1 : CHAR(1), 
	Aladin string							if array size is not [1]: VARCHAR. VARCHAR(n), CHAR(n),BLOB, TIMESTAMP, POINT, POINT, REGION 
	"unicodeChar"	Unicode Character	2	donno. No equivalent
	"float"			Floating point	"E"	4	REAL
	"double"		Double			"D"	8	DOUBLE
	"floatComplex"	Float Complex	"C"	8	not supported
	"doubleComplex"	Double Complex	"M"	16	not supported
	
	type is set based on arraysize and xtype information, if available
	 *
	 */
	public void setDataType(Field f) {
		// TODO Auto-generated method stub
		String datatype = Field.typeFits2VOTable(f.datatype);
		if (datatype.equalsIgnoreCase("int")) {
			this.datatype = INTEGER.name();
		} else if (datatype.equalsIgnoreCase("long")) {
			this.datatype = BIGINT.name();
		} else if (datatype.equalsIgnoreCase("char")) {
			this.datatype = CHAR.name();
		} else if (datatype.equalsIgnoreCase("string")) {
			this.datatype = VARCHAR.name();
//			if (f.arraysize == null || f.arraysize.isEmpty()) {
//				this.datatype = VARCHAR.name();
//			} else if (f.arraysize.endsWith("*")) {
//				this.datatype = VARCHARn.name();
//			} else if(isInteger(f.arraysize)) {
//				this.datatype = CHARn.name();
//			} else {
//				this.datatype = VARCHAR.name();
//			}
		} else if (datatype.equalsIgnoreCase("float")) {
			this.datatype = REAL.name();
		} else if (datatype.equalsIgnoreCase("double")) {
			this.datatype = DOUBLE.name();
		} else if (datatype.equalsIgnoreCase("short")) {
			this.datatype = SMALLINT.name();
		} else if (datatype.equalsIgnoreCase("unsignedByte")) {
			this.datatype = VARBINARY.name();
//			if (f.arraysize == null || f.arraysize.isEmpty()) {
//				this.datatype = VARBINARY.name();
//			} else if (f.arraysize.endsWith("*")) {
//				this.datatype = VARBINARYn.name();
//			} else if(isInteger(f.arraysize)) {
//				this.datatype = BINARYn.name();
//			} else {
//				this.datatype = VARBINARY.name();
//			}
		}
		//bit, boolean, floatComplex, doubleComplex, unicodeChar not supported
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
	
	/**
	 * Method to verify an int datatype
	 * @param dataType
	 * @param value
	 * @return
	 */
	public boolean isDefinedMain() {
		boolean result = false;
		if (this.ucd != null) {
			String[] words = this.ucd.split(";");
			if (words.length > 1 && words[1].equalsIgnoreCase(UCD_MAINIDQUALIFIER)) {
				result = true;
			}
		}
		return result;
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
	
	public String getFlaggedColumnToolTip() {
		String tooltip = column_name;
		if (this.unit != null) {
			tooltip = "Unit: "+this.unit;
		}
		if (this.utype != null) {
			if (this.unit != null) {
				tooltip = tooltip+", ";
			}
			tooltip = tooltip+this.utype;
		}
		return column_name;
		
	}
	
	public boolean isNumeric() {
		boolean result = false;
		if (datatype != null) {
			if (datatype.toUpperCase().contains("SMALLINT") || datatype.toUpperCase().contains("INTEGER")
					|| datatype.toUpperCase().contains("BIGINT") || datatype.toUpperCase().contains("REAL")
					|| datatype.toUpperCase().contains("DOUBLE")) {
				result = true;
			}
		}
		return result;
	}
	
	public static boolean isInteger(String input) {
		boolean result = false;
		Pattern pattern = Pattern.compile(REGEX_DIGITS);
		Matcher matcher = pattern.matcher(input);
		if (matcher.find()) {
			result = true;
		} else {
			result = false;
		}
		return result;
	}

	public String getXtype() {
		return xtype;
	}

	public void setXtype(String xtype) {
		this.xtype = xtype;
	}

}
