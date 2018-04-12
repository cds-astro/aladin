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

import static cds.aladin.Constants.DEC;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.RA;
import static cds.aladin.Constants.REMOVEWHERECONSTRAINT;

import java.awt.FlowLayout;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class representing join gui after addition, in the list
 * @author chaitra
 *
 */
public class JoinConstraint extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9057608446810725723L;
	
	public JoinFacade facade;
	public String mainTableName;
	public String joinTableName;
	public String constraint; //could be either col or position, for now this is uncomplicated
	JButton removeButton;
	public Vector<TapTableColumn> columns;
	public String raColumnName;
	public String decColumnName;

	public String alias;
	
	public JoinConstraint() {
		// TODO Auto-generated constructor stub
		Image image = Aladin.aladin.getImagette("delete_button.png");
		if (image == null) {
			this.removeButton = new JButton("X");
		} else {
			this.removeButton = new JButton(new ImageIcon(image));
		}
		this.removeButton.setToolTipText(WhereGridConstraint.DELETEBUTTON_TOOLTIP);
		this.removeButton.setActionCommand(REMOVEWHERECONSTRAINT);
	}
	
	public JoinConstraint(JoinFacade facade, String mainTableName, TapTable secTableMetaData) {
		// TODO Auto-generated constructor stub
		this();
		this.facade = facade;
		this.mainTableName = mainTableName;
		this.joinTableName = secTableMetaData.getTable_name();
		
		this.removeButton.addActionListener(this.facade);
		setAlais(facade.constraints);
		columns = getColumnsWithAlais(secTableMetaData, this.alias);
	}
	
	public Vector<TapTableColumn> getColumnsWithAlais(TapTable refTable, String alias) {
		// TODO Auto-generated method stub
		Vector<TapTableColumn> results = new Vector<TapTableColumn>();
		TapTableColumn copy = null;
		TapTableColumn refRaCol = refTable.getFlaggedColumn(RA);
		TapTableColumn refDecCol = refTable.getFlaggedColumn(DEC);
		for (TapTableColumn refColumn : refTable.getColumns()) {
			copy = new TapTableColumn(refColumn, alias);
			if (refRaCol != null && refRaCol == refColumn) {
				this.raColumnName = copy.getColumnNameForQuery();
//				if (copy.getAlais() != null) {
//					this.raColumnName = copy.getAlais() + DOT_CHAR + this.raColumnName;
//				}
			} else if (refDecCol != null && refDecCol == refColumn) {
				this.decColumnName = copy.getColumnNameForQuery();
//				if (copy.getAlais() != null) {
//					this.decColumnName = copy.getAlais() + DOT_CHAR + this.decColumnName;
//				}
			}
			results.add(copy);
		}
		return results;
	}

	public void setGui() {
		setLayout(new FlowLayout(FlowLayout.LEFT));
		add(this.removeButton);
		String label = null;
		String tooltip = null;
		if (constraint != null) {
			label = this.alias+" ("+constraint+" )";
			tooltip = "Join table: "+this.joinTableName+", alais: "+this.alias +" ("+constraint+" )";
		} else {
			label = JoinFacade.GENERCIERROR_JOIN;
		}
		
		JLabel joinLabel = new JLabel(label);
		joinLabel.setToolTipText(tooltip);
		add(joinLabel);
	}
	
	public void setConstraintAndGui(String constraint) {
		this.constraint = constraint;
		setGui();
	}
	
	/**
	 * Main table remains without alais. For the rest we keep alais. if there
	 * are 2 same tables joined, second name gets a number increment on the name
	 */
	public void setAlais(List<JoinConstraint> otherConstraints) {
		alias = getAlais(mainTableName, joinTableName, otherConstraints);
	}
	
	/*public static String getAlais(String mainTableName, String joinTableName, List<JoinConstraint> otherConstraints) {
		int aliasIndex = 0;
		String alias = null;
		if (mainTableName.equalsIgnoreCase(joinTableName)) {
			aliasIndex++;
		}
		for (JoinConstraint otherConstraint : otherConstraints) {
			if (otherConstraint.joinTableName.equalsIgnoreCase(joinTableName)) {
				aliasIndex++;
			}
		}
		if (aliasIndex > 0) {
			alias = joinTableName + aliasIndex;
		} else {
			alias = joinTableName;
		}
		
		if (alias.startsWith("TAP_UPLOAD.")) {
			alias = alias.replace("TAP_UPLOAD.", "");
		}
		
		alias = TapTable.getQueryPart(alias, false);
		return alias;
	}*/
	
	public static String getAlais(String mainTableName, String joinTableName, List<JoinConstraint> otherConstraints) {
		int aliasIndex = 0;
		String alias = joinTableName;
		
		if (joinTableName.startsWith("TAP_UPLOAD.")) {
			alias = joinTableName.replace("TAP_UPLOAD.", "");
		}
		if (TapTable.isUnQuotedPattern(TapTable.getQueryPart(joinTableName, true))) {
			alias = alias.replaceAll("\\.", "");
		}
		String aliasNameInWorks = alias;
		if (mainTableName.equalsIgnoreCase(joinTableName)) {
			aliasIndex++;
			aliasNameInWorks = alias + aliasIndex;
		}
		List<String> tableNames = new ArrayList<String>();
		tableNames.add(mainTableName);
		for (JoinConstraint otherConstraint : otherConstraints) {
			tableNames.add(otherConstraint.alias);
		}
		
		aliasNameInWorks = TapTable.getQueryPart(aliasNameInWorks, false);
		
		alias = getUniqueTableName(aliasIndex, alias, aliasNameInWorks, tableNames);
		
		
		return alias;
	}
	
	private static String getUniqueTableName(int index, String init, String input, List<String> takenValues) {
		String resultInWorks = input;
		if (!takenValues.contains(input)) {
			return input;
		} else {
			index++;
			resultInWorks = init + index;
			resultInWorks = TapTable.getQueryPart(resultInWorks 	, false);
		}
		return getUniqueTableName(index, init, resultInWorks, takenValues);
	}
	
	public String getADQLString() {
		String result = EMPTYSTRING;
		if (constraint != null && !constraint.isEmpty()) {
			StringBuffer whereClause = new StringBuffer(" JOIN ");
			whereClause.append(TapTable.getQueryPart(joinTableName, true));
			if (alias != null) {
				whereClause.append(" AS ").append(alias);
			}
			whereClause.append(" ON ").append(constraint);
			result = whereClause.toString();
		}
		return result;
	}

	public boolean hasThisColumn(TapTableColumn tapTableColumn) {
		// TODO Auto-generated method stub
		boolean result = false;
		String fullyQualifiedTableName = this.joinTableName + DOT_CHAR + this.alias;
		if (fullyQualifiedTableName.equalsIgnoreCase(tapTableColumn.getTable_name())) {
			result = true;
		}
		return result;
	}

}
