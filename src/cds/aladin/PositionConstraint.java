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

package cds.aladin;

import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.POSQuery;
import static cds.aladin.Constants.EMPTYSTRING;

import javax.swing.JLabel;

public class PositionConstraint extends WhereGridConstraint{
	private static final long serialVersionUID = 1L;
	
	private String raConstraint;
	private String decConstraint;
	private String radiusConstraint;
	private String selectedDecColumnName;
	private String selectedRaColumnName;
	private String tableName;
	
	public PositionConstraint(ServerTap serverTap) {
		// TODO Auto-generated constructor stub
		super(serverTap);
	}
	
	public PositionConstraint(ServerTap serverTap, String raConstraint, String decConstraint, String radiusConstraint, String tableName, String selectedRaColumnName, String selectedDecColumnName) {
		// TODO Auto-generated constructor stub
		super(serverTap, new JLabel("Ra= "+raConstraint), new JLabel("Dec= "+decConstraint), new JLabel("Radius= "+radiusConstraint));
		this.raConstraint = raConstraint;
		this.decConstraint = decConstraint;
		this.radiusConstraint = radiusConstraint;
		this.selectedRaColumnName = selectedRaColumnName;
		this.selectedDecColumnName = selectedDecColumnName;
		this.tableName = tableName;
	}
	
	@Override
	public String getAdqlString() throws Exception{
		StringBuffer whereClause = new StringBuffer();
		if (this.andOrOperator != null) {
			whereClause.append(this.andOrOperator.getSelectedItem()).append(SPACESTRING);
		}
		String alias = serverTap.tapClient.tablesMetaData.get(tableName).getAlias();
		if (alias != null) {
			alias = alias + DOT_CHAR;
		} else {
			alias = EMPTYSTRING;
		}
		whereClause.append(String.format(POSQuery, alias, this.selectedRaColumnName,
				this.selectedDecColumnName, this.raConstraint, this.decConstraint,
				this.radiusConstraint)).append(SPACESTRING);

		return whereClause.toString();
	}

	public String getSelectedRaColumnName() {
		return selectedRaColumnName;
	}

	public void setSelectedRaColumnName(String selectedRaColumnName) {
		this.selectedRaColumnName = selectedRaColumnName;
	}

	public String getSelectedDecColumnName() {
		return selectedDecColumnName;
	}

	public void setSelectedDecColumnName(String selectedDecColumnName) {
		this.selectedDecColumnName = selectedDecColumnName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	
}
