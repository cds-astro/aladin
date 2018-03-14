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

/**
 * Class is a model class for key columns of tap services
 * @author chaitra
 *
 */
public class ForeignKeyColumn {
	
	private String from_table;
	private String from_column;
	private String target_column;
	
	public String getFrom_column() {
		return from_column;
	}
	public void setFrom_column(String from_column) {
		this.from_column = from_column;
	}
	public String getTarget_column() {
		return target_column;
	}
	public void setTarget_column(String target_column) {
		this.target_column = target_column;
	}
	public String getFrom_table() {
		return from_table;
	}
	public void setFrom_table(String from_table) {
		this.from_table = from_table;
	}

}