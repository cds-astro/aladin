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

import java.awt.Component;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;



/**
 * Modèle de données pour les macros
 * (utilisation des scripts avec liste de paramètres)
 *
 * @author Thomas Boch [CDS]
 *
 * @version 0.9 : sept. 2006 - création
 * @see cds.aladin.FrameMacro
 */
public class MacroModel {
	private String script;
	private ParamTableModel paramTableModel;
	private TableCellEditor tableCellEditor;
	
	private Aladin a;
	
	public MacroModel(Aladin a) {
		this.a = a;
		paramTableModel = new ParamTableModel(0);
		tableCellEditor = new MyCellEditor();
	}
	
	/**
	 * @return Returns the script.
	 */
	protected String getScript() {
		return script;
	}
	/**
	 * @param script The script to set.
	 */
	protected void setScript(String script) {
		this.script = script;
	}
	
	/**
	 * 
	 * @param command commande à exécuter
	 * @param params map des paramètres ("nom param" --> valeur param)
	 */
	protected void executeScript(String command, Map params) {
		if( command.trim().length()==0 || command.trim().startsWith("#") ) return;
		
		// TODO : vérifier qu'on a tous les params
		String curParam;
		Set paramSet = params.keySet();
		Object[] paramNames = paramSet.toArray();
		for( int j=0; j<paramNames.length; j++ ) {
			curParam = (String)paramNames[j];
        	command = MetaDataTree.replace(command, curParam, 
        								   (String)params.get(curParam), -1);
        }
//		System.out.println("exec script : "+command);
        // TODO : gestion des erreurs
        a.execCommand(command);
	}
	
	/** classe représentant une ligne de paramètres */
	class ParamRecord {
		String[] values;
		
		ParamRecord() {
		}
		
		void setValues(String[] val) {
			this.values = val;
		}
		
		String[] getValues() {
			return this.values;
		}
		
		String getValue(int idx) {
			if( values==null || idx>=values.length) return "";
			return values[idx];
		}
		
		void setValue(int col, String val) {
			if( col>=values.length ) {
				String[] tmp = new String[col+1];
				System.arraycopy(values, 0, tmp, 0, values.length);
				values = tmp;
			}
			values[col] = val;
		}
		
		public String toString() {
			if( values==null ) return "";
			StringBuffer ret = new StringBuffer();
			for( int i=0; i<values.length; i++ ) {
				if( values[i]!=null ) ret.append(values[i]);
				if( i<values.length-1 ) ret.append("\t");
			}
			
			return ret.toString();
		}
		
		// the record is empty if each value is null or equals to "" 
		boolean isEmpty() {
			if( values==null ) return true;
			
			for( int i=0; i<values.length; i++ ) {
				if( values[i]!=null && values[i].length()>0 ) return false;
			}
			return true;
		}
		
	}
	
	/** modèle de données pour la JTable gérant les paramètres */
	class ParamTableModel extends AbstractTableModel {
	     protected String[] columnNames;
	     protected Vector dataVector;

	     public ParamTableModel(int nbCol) {
	         initColumnNames(nbCol);
	         dataVector = new Vector();
	     }

	     public String getColumnName(int column) {
	         return columnNames[column];
	     }

	     public boolean isCellEditable(int row, int column) {
	         return true;
	     }

	     public Class getColumnClass(int column) {
	         return String.class;
	     }

	     public Object getValueAt(int row, int column) {
	     	if( column==-1 || row==-1 ) return null;
	         ParamRecord record = (ParamRecord)dataVector.get(row);
	         return record.getValue(column);
	     }

	     public void setValueAt(Object value, int row, int column) {
	     	ParamRecord record = (ParamRecord)dataVector.get(row);
	        record.setValue(column, value.toString());
	     	
	        fireTableCellUpdated(row, column);
	     }

	     public int getRowCount() {
	         return dataVector.size();
	     }

	     public int getColumnCount() {
	         return columnNames.length;
	     }
	     
	     private void initColumnNames(int nbCol) {
	     	columnNames = new String[nbCol];
	     	for( int i=0; i<nbCol; i++ ) columnNames[i] = "$"+(i+1);
	     	
	     	fireTableStructureChanged();
	     }

	     public void reset() {
	     	initColumnNames(0);
	     	dataVector = new Vector();
	     }
	     
	     public void addRecord(ParamRecord record) {
	     	if( record==null ) {
	     		record = new ParamRecord();
	     		record.setValues(new String[getColumnCount()]);
	     	}
	     	
	     	if( record.values!=null && record.values.length>getColumnCount() ) {
	     		initColumnNames(record.values.length);
	     	}
	     	dataVector.addElement(record);
	     	
	        fireTableRowsInserted(dataVector.size()-1, dataVector.size()-1);
	     }
	     
	     public void addRecord(String[] values) {
	     	ParamRecord record = new ParamRecord();
	     	record.setValues(values);
	     	addRecord(record);
	     }
	     
	     /** ajout d'une ligne supplémentaire par rapport aux données dont on dispose */  
	     public void initTable() {
	     	addEmptyRecord();
	     }
	     
	     public ParamRecord[] getRecords() {
	     	ParamRecord[] records = new ParamRecord[dataVector.size()];
	     	dataVector.copyInto(records);
	     	return records;
	     }
	     
	     public void addEmptyRecord() {
	     	addRecord((ParamRecord)null);
	     }
	     
	     public void addEmptyCol() {
	     	int nbCol = getColumnCount();
	     	initColumnNames(nbCol+1);
	     }
	     
	     public void deleteRecord(int idx) {
	     	if( idx<0 || idx>getRowCount()-1 ) return;
	     	dataVector.remove(idx);
	     	fireTableRowsDeleted(idx, idx);
	     }
	     
//	     public boolean addRowIfLastRowNotEmpty() {
//	     	if( !((ParamRecord)dataVector.get(dataVector.size()-1)).isEmpty() ) {
//	     		System.out.println("toto");
//	     		addEmptyRecord();
//	     		return true;
//	     	}
//	     	return false;
//	     }

	 }
	
	/** éditeur de cellules perso */
	class MyCellEditor extends DefaultCellEditor {

		private int row;
		
		public MyCellEditor() {
		  	super(new JTextField());

		}

		public Object getCellEditorValue() {
			// ajout nouvelle ligne si nécessaire
			String s = (String)super.getCellEditorValue();
			
			if( row==paramTableModel.getRowCount()-1 && s.length()>0 ) {
	         	paramTableModel.addEmptyRecord();
	         }
			
		    return s;
		}
		
		// pour récupérer l'indice de la ligne éditée
	    public Component getTableCellEditorComponent(JTable table, Object value,
				 boolean isSelected,
				 int row, int column) {
	    	this.row = row;
	    	return super.getTableCellEditorComponent(table, value, isSelected, row, column);
	    }
		
		
	}
	
	/**
	 * @return Returns the paramTableModel.
	 */
	protected ParamTableModel getParamTableModel() {
		return paramTableModel;
	}

	/**
	 * @return Returns the tableCellEditor.
	 */
	protected TableCellEditor getTableCellEditor() {
		return tableCellEditor;
	}
}
