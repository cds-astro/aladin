package cds.aladin;

import java.util.Vector;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class DataLabelTable2 implements TableModel {
	Aladin aladin;
	Vector<Vector<String>> fullData;
	public TableModelListener tableListener;
	
	public DataLabelTable2() {
		// TODO Auto-generated constructor stub
	}
	
	public DataLabelTable2(Aladin aladin, Vector<Vector<String>> dataLabels) {
		// TODO Auto-generated constructor stub
		this.aladin = aladin;
		this.fullData = dataLabels;
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
	public int getRowCount() {
		// TODO Auto-generated method stub
		int result = 0;
		if (this.fullData != null) {
			result = this.fullData.size();
		}
		return result;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		Vector<String> data = this.fullData.get(rowIndex);
		if (columnIndex <= data.size()) {
			return data.elementAt(columnIndex);
		}
		return "";
	}
	
	public Vector<String> getDataLabelAt(int rowIndex) {
		// TODO Auto-generated method stub
		return this.fullData.get(rowIndex);
	}
	
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

