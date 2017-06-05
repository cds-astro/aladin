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
