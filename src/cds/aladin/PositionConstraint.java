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

import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.POSQuery;

import javax.swing.JLabel;

public class PositionConstraint extends WhereGridConstraint{
	private static final long serialVersionUID = 1L;
	
	private String raConstraint;
	private String decConstraint;
	private String radiusConstraint;
	private String selectedDecColumnName;
	private String selectedRaColumnName;
	
	public PositionConstraint(ServerTap serverTap) {
		// TODO Auto-generated constructor stub
		super(serverTap);
	}
	
	public PositionConstraint(ServerTap serverTap, String raConstraint, String decConstraint, String radiusConstraint, String selectedRaColumnName, String selectedDecColumnName) {
		// TODO Auto-generated constructor stub
		super(serverTap, new JLabel("Ra= "+raConstraint), new JLabel("Dec= "+decConstraint), new JLabel("Radius= "+radiusConstraint));
		this.raConstraint = raConstraint;
		this.decConstraint = decConstraint;
		this.radiusConstraint = radiusConstraint;
		this.selectedRaColumnName = selectedRaColumnName;
		this.selectedDecColumnName = selectedDecColumnName;
	}
	
	@Override
	public String getAdqlString() throws Exception{
		StringBuffer whereClause = new StringBuffer();
		if (this.andOrOperator != null) {
			whereClause.append(this.andOrOperator.getSelectedItem()).append(SPACESTRING);
		}
		whereClause.append(String.format(POSQuery, this.selectedRaColumnName, this.selectedDecColumnName,
				this.raConstraint, this.decConstraint, this.radiusConstraint)).append(SPACESTRING);

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

	
}
