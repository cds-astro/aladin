/**
 * 
 */
package cds.aladin;

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
	private boolean isPrincipal; //is principal column
	private boolean isIndexed;	//is indexed column
	private boolean isStandard; //is standard column
	
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
		allValuesVector.addElement(String.valueOf(this.size));
		allValuesVector.addElement(String.valueOf(this.isPrincipal));
		allValuesVector.addElement(String.valueOf(this.isIndexed));
		allValuesVector.addElement(String.valueOf(this.isStandard));
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
	
	public int getSize() {
		return size;
	}
	public void setSize(int size) {
		this.size = size;
	}
	public void setSize(String dataType, String value) {
		int parsedValue = verifyExtractInt(dataType, value);
		this.size = parsedValue;
	}
	
	/**
	 * Method to verify an int datatype
	 * @param dataType
	 * @param value
	 * @return
	 */
	public int verifyExtractInt(String dataType, String value) {
		if (dataType!=null && dataType.equals("int")) {
			int parsedValue = Integer.parseInt(value);
			return parsedValue;
		}
		return -1;
	}
	
	public boolean isPrincipal() {
		return isPrincipal;
	}
	public void setPrincipal(boolean isPrincipal) {
		this.isPrincipal = isPrincipal;
	}
	public void setPrincipal(String dataType, String value) {
		int parsedValue = verifyExtractInt(dataType, value);
		if (parsedValue==1) {
			this.isPrincipal = true;
		} else if (parsedValue==0){
			this.isPrincipal = false;
		}
	}
	
	public boolean isIndexed() {
		return isIndexed;
	}
	public void setIndexed(boolean isIndexed) {
		this.isIndexed = isIndexed;
	}
	public void setIndexed(String dataType, String value) {
		int parsedValue = verifyExtractInt(dataType, value);
		if (parsedValue==1) {
			this.isIndexed = true;
		} else if (parsedValue==0){
			this.isIndexed = false;
		}
	}
	
	public boolean isStandard() {
		return isStandard;
	}
	public void setStandard(boolean isStandard) {
		this.isStandard = isStandard;
	}
	public void setStandard(String dataType, String value) {
		int parsedValue = verifyExtractInt(dataType, value);
		if (parsedValue==1) {
			this.isStandard = true;
		} else if (parsedValue==0){
			this.isStandard = false;
		}
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
	
}
