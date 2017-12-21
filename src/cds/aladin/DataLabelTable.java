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

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public abstract class DataLabelTable implements TableModel {
	Aladin aladin;
	public TableModelListener tableListener;
	int idxSortedCol = 1; // indice de la colonne sur laquelle on trie
	boolean ascSort;
	
	public DataLabelTable() {
		// TODO Auto-generated constructor stub
	}
	
	public DataLabelTable(Aladin aladin) {
		// TODO Auto-generated constructor stub
		this.aladin = aladin;
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub
		tableListener = l;
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		// TODO Auto-generated method stub
		return String.class;
	}

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return 3;
	}

	@Override
	public String getColumnName(int columnIndex) {
		// TODO Auto-generated method stub
		switch(columnIndex) {
        case 0: return "Label";
        case 1: return "Description";
        case 2: return "Url";
     }
     return "";
	}
	
	@Override
	public abstract int getRowCount();

	@Override
	public abstract Object getValueAt(int rowIndex, int columnIndex);
	
	public abstract Vector<String> getDataLabelAt(int rowIndex);
	
	public abstract void defaultSortServers();
	
	public abstract void notifyTableChanged();
	
	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
	}}

