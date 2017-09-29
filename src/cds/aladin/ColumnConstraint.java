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

import static cds.aladin.Constants.REGEX_RANGEINPUT;
import static cds.aladin.Constants.SPACESTRING;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import cds.tools.Util;

public class ColumnConstraint extends WhereGridConstraint implements ItemListener{
	private static final long serialVersionUID = 1L;
	public static final String BETWEEN = "BETWEEN";
	public static final String NOTBETWEEN = "NOT BETWEEN";
	public static final String REGEX_BETWEENINPUT = "(?<range1>[A-Za-z0-9]+)\\s+and\\s+(?<range1>[A-Za-z0-9]+)";
	private static final String toolTipText = "Format should be as: value1 AND value2";
	
	public ColumnConstraint(ServerTap serverTap, Vector columnNames) {
		// TODO Auto-generated constructor stub
		super(serverTap, new JComboBox(columnNames), new JComboBox(operators), new JTextField(6));
		JComboBox columns = (JComboBox) this.firstGridComponent;
		columns.setRenderer(new CustomListCellRenderer());
		columns.setSize(columns.getWidth(), Server.HAUT);
		columns.addItemListener(this);
		
		
		((JTextField) this.thirdGridComponent).addFocusListener(new FocusListener () {
			@Override
			public void focusGained(FocusEvent arg0) {
				// TODO Auto-generated method stub
			}

			@Override
			public void focusLost(FocusEvent e) {
				// TODO Auto-generated method stub
				processInput((JTextField) e.getSource());
				itemStateChanged(null);
			}
		});
		
		((JComboBox) this.secondGridComponent).addItemListener(this);
	}
	
	/**
	 * This method currently work for checking the inputs for between fields only.
	 * @param constraint
	 */
	public void processInput(JTextField constraint) {
		ColumnConstraint columnConstraintPanel = (ColumnConstraint) constraint.getParent();
		String selectedWhereOperator = (String) ((JComboBox<String>) columnConstraintPanel.secondGridComponent).getSelectedItem();
		if (selectedWhereOperator.equals(BETWEEN) || selectedWhereOperator.equals(NOTBETWEEN)) {
			DelimitedValFieldListener.andConstraint(constraint, REGEX_RANGEINPUT/*REGEX_BETWEENINPUT*/, toolTipText);
		}
	}
	
	
	@Override
	public String getAdqlString() throws Exception {
		// TODO Auto-generated method stub
		StringBuffer whereClause = new StringBuffer();
		if (this.andOrOperator != null) {
			whereClause.append(this.andOrOperator.getSelectedItem()).append(SPACESTRING);
		}
		JComboBox columns = (JComboBox) this.firstGridComponent;
		JComboBox whereOperator = (JComboBox) this.secondGridComponent;
		JTextField constraintValue = (JTextField) this.thirdGridComponent;
		String selectedWhereOperator = (String) whereOperator.getSelectedItem();
		
		TapTableColumn column = ((TapTableColumn)columns.getSelectedItem());
		String columnName = column.getColumnNameForQuery();
		whereClause.append(columnName).append(SPACESTRING);
		
		if (constraintValue.getText().isEmpty()) {
			whereClause.append(defaultValue).append(SPACESTRING);
		} else {
			if (selectedWhereOperator.equals(BETWEEN) || selectedWhereOperator.equals(NOTBETWEEN)) {
				String selectedText = constraintValue.getText();
				Highlighter highlighter = constraintValue.getHighlighter();
//				Pattern re = Pattern.compile(REGEX_BETWEENINPUT, Pattern.CASE_INSENSITIVE);
				Pattern regex = Pattern.compile(REGEX_RANGEINPUT);
				Matcher matcher = regex.matcher(selectedText);
				if (!matcher.find()) {
					Aladin.trace(3, "No 'AND' used for the between operator!");
					constraintValue.setToolTipText("Format should be as: value1 AND value2");
					HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Aladin.LIGHTORANGE);
					try {
						highlighter.addHighlight(0, selectedText.length(), painter);
					} catch (BadLocationException e1) {
						if (Aladin.levelTrace >= 3) 
							e1.printStackTrace();
						//Don't do anything if this feature fails
					}
					throw new Exception("Error for column contraint: "+columnName+". \nFormat for between operator: value1 AND value2. Ex: 2 and 3. ");
				} else {
					if (matcher.group("range1") != null && matcher.group("range2") != null) {
						whereClause.append(selectedWhereOperator).append(SPACESTRING).append(Util.formatterPourRequete(false, matcher.group("range1")))
						.append(" AND ").append(Util.formatterPourRequete(false, matcher.group("range2"))).append(SPACESTRING);
					}
				}
			} else{
				String dataType = column.getDatatype();
				boolean processAsNumber = true;
				if (dataType != null && !dataType.toUpperCase().contains("VARCHAR")) {
					processAsNumber = false;
				}
				whereClause.append(whereOperator.getSelectedItem()).append(SPACESTRING).append(Util.formatterPourRequete(processAsNumber, constraintValue.getText()))
				.append(SPACESTRING);
			}
			
		}
		return whereClause.toString();
	}
	

	public void keyReleased(KeyEvent e) {
	      if( e.getSource() instanceof JTextField ) {
	         
	      }
	   }

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		serverTap.writeQuery();
	}
}
