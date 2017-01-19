package cds.aladin;

import java.util.Vector;

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
	private String raColumnName;
	private String decColumnName; 
	private Vector<TapTableColumn> columns;
	
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

	public Vector<TapTableColumn> getColumns() {
		return columns;
	}

	public void setColumns(Vector<TapTableColumn> columns) {
		this.columns = columns;
	}
	
	
}
