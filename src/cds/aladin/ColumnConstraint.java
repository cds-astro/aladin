// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import static cds.aladin.Constants.REGEX_RANGEINPUT;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.REGEX_NUMBERWITHEXP;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import cds.tools.Util;

public class ColumnConstraint extends WhereGridConstraint implements ItemListener {
	private static final long serialVersionUID = 1L;
	public static final String BETWEEN = "BETWEEN";
	public static final String NOTBETWEEN = "NOT BETWEEN";
	public static final String REGEX_BETWEENINPUT = "(?<range1>[A-Za-z0-9]+)\\s+and\\s+(?<range1>[A-Za-z0-9]+)";
	private static final String toolTipText = "Format should be as: value1 AND value2";
	protected static final String[] charOperators = { "=", "!=", "IS NULL", "IS NOT NULL", "LIKE", "NOT LIKE" };
	protected static final String[] numOperators = { "=", "!=", "<", ">", "<=", ">=", "BETWEEN", "NOT BETWEEN", "IS NULL",
			"IS NOT NULL"};
	public static final String FORMATFORBETWEEN, FORMATFORBETWEENTOOLTIP, INCORRECTCONSTRAINTVALUE;
	
	static {
		FORMATFORBETWEEN = Aladin.chaine.getString("FORMATFORBETWEEN");
		FORMATFORBETWEENTOOLTIP = Aladin.chaine.getString("FORMATFORBETWEENTOOLTIP");
		INCORRECTCONSTRAINTVALUE = Aladin.chaine.getString("INCORRECTCONSTRAINTVALUE");
	}
	
	public ColumnConstraint(ServerTap serverTap, Vector columnNames) {
		// TODO Auto-generated constructor stub
		super(serverTap, new JComboBox(columnNames), new JComboBox(operators), new JTextField(14));
		JComboBox columns = (JComboBox) this.firstGridComponent;
		columns.setRenderer(new CustomListCellRenderer(serverTap));
		columns.setSize(columns.getWidth(), Server.HAUT);
		setWhereOperators();
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
		
		((JComboBox) this.firstGridComponent).addItemListener(this);
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
		
		String alias = null;
		String columnName = column.getColumnNameForQuery();
		alias = serverTap.getRelevantAlias(column);
		if (alias != null) {
			whereClause.append(alias).append(DOT_CHAR);
		}
		whereClause.append(columnName).append(SPACESTRING);
		
		if (selectedWhereOperator.equals("IS NULL")) {
			whereClause.append(selectedWhereOperator).append(SPACESTRING);
		} else if (constraintValue.getText().isEmpty()) {
			whereClause.append(defaultValue).append(SPACESTRING);
		} else {
			if (selectedWhereOperator.equals(BETWEEN) || selectedWhereOperator.equals(NOTBETWEEN)) {
				String selectedText = constraintValue.getText();
				Highlighter highlighter = constraintValue.getHighlighter();
//				Pattern re = Pattern.compile(REGEX_BETWEENINPUT, Pattern.CASE_INSENSITIVE);
//				Pattern regex = Pattern.compile(REGEX_RANGEINPUT);
//				Matcher matcher = regex.matcher(selectedText);
				String processedInput = TapClient.getRangeInput(selectedText, null);
				/*if (!matcher.find())*/ if(processedInput.isEmpty()){
					Aladin.trace(3, "No 'AND' used for the between operator!");
					constraintValue.setToolTipText(FORMATFORBETWEENTOOLTIP);
					HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Aladin.LIGHTORANGE);
					try {
						highlighter.addHighlight(0, selectedText.length(), painter);
					} catch (BadLocationException e1) {
						if (Aladin.levelTrace >= 3) 
							e1.printStackTrace();
						//Don't do anything if this feature fails
					}
					throw new Exception("Error for column contraint: "+columnName+". \n"+FORMATFORBETWEEN);
				} else {
					/*if (matcher.group("range1") != null && matcher.group("range2") != null) {
						whereClause.append(selectedWhereOperator).append(SPACESTRING).append(Util.formatterPourRequete(false, matcher.group("range1")))
						.append(" AND ").append(Util.formatterPourRequete(false, matcher.group("range2"))).append(SPACESTRING);
					}*/
					whereClause.append(processedInput);
				}
			} else{
				String dataType = column.getDatatype();
				boolean considerAsString = true;
				if (dataType != null && !dataType.toUpperCase().contains("VARCHAR")
						&& !dataType.toUpperCase().contains("CHAR")) {
					considerAsString = false;
				}
				if (!considerAsString) {
					Pattern pattern = Pattern.compile(REGEX_NUMBERWITHEXP);
					Matcher matcher = pattern.matcher(constraintValue.getText());
					if (!matcher.find()) {
						throw new Exception(INCORRECTCONSTRAINTVALUE);
					}
				}
				whereClause.append(whereOperator.getSelectedItem()).append(SPACESTRING).append(Util.formatterPourRequete(considerAsString, constraintValue.getText()))
				.append(SPACESTRING);
			}
			
		}
		return whereClause.toString();
	}
	
	public TapTableColumn getSelectedItem() {
		// TODO Auto-generated method stub
		return (TapTableColumn) ((JComboBox) this.firstGridComponent).getSelectedItem();
	}
	

	public void keyReleased(KeyEvent e) {
	      if( e.getSource() instanceof JTextField ) {
	         
	      }
	   }
	
	public void setWhereOperators() {
		JComboBox columns = (JComboBox) this.firstGridComponent;
		JComboBox whereOperator = (JComboBox) this.secondGridComponent;
		TapTableColumn column = ((TapTableColumn)columns.getSelectedItem());
		String dataType = column.getDatatype();
		if (column.isNumeric()) {
			whereOperator.removeAllItems();
			DefaultComboBoxModel items = new DefaultComboBoxModel(numOperators);
			whereOperator.setModel(items);
		} else {
			whereOperator.removeAllItems();
			DefaultComboBoxModel items = new DefaultComboBoxModel(charOperators);
			whereOperator.setModel(items);
		}
		setIsNullGui();
		
	}
	
	public void setIsNullGui() {
		JComboBox whereOperator = (JComboBox) this.secondGridComponent;
		JTextField constraintValue = (JTextField) this.thirdGridComponent;
		if (whereOperator.getSelectedItem() != null && (whereOperator.getSelectedItem().equals("IS NULL")
				|| whereOperator.getSelectedItem().equals("IS NOT NULL"))) {
			constraintValue.setText(EMPTYSTRING);
			constraintValue.setEnabled(false);
		} else {
			constraintValue.setEnabled(true);
		}
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		if (e != null && e.getSource().equals(this.firstGridComponent)) {
			//change second grid component
			setWhereOperators();
		} else {
			setIsNullGui(); //already done for first
		}
		
		serverTap.writeQuery();
	}

	public void setWhereModel(Vector<TapTableColumn> displayColumns, TapTableColumn selectedItem) {
		// TODO Auto-generated method stub
		DefaultComboBoxModel model = new DefaultComboBoxModel(displayColumns);
		JComboBox columns = (JComboBox) this.firstGridComponent;
		columns.setModel(model);
		columns.setSelectedItem(selectedItem);
	}
	
}
