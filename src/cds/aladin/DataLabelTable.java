package cds.aladin;

import java.util.List;
import java.util.Vector;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import cds.mocmulti.MocItem;

public class DataLabelTable implements TableModel {
	Aladin aladin;
	List<String> dataLabels;
	
	public DataLabelTable() {
		// TODO Auto-generated constructor stub
	}
	
	public DataLabelTable(Aladin aladin, List<String> dataLabels) {
		// TODO Auto-generated constructor stub
		this.aladin = aladin;
		this.dataLabels = dataLabels;
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// TODO Auto-generated method stub
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
		return null;
	}

	@Override
	public int getRowCount() {
		// TODO Auto-generated method stub
		int result = 0;
		if (this.dataLabels != null) {
			result = this.dataLabels.size();
		}
		return result;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		// TODO Auto-generated method stub
		MocItem mi = aladin.directory.multiProp.getItem(this.dataLabels.get(rowIndex));
		switch (columnIndex) {
		case 0:
			return this.dataLabels.get(rowIndex);
		case 1:
			return mi.prop.get("tap_service_url");
		case 2: {
			String desc = mi.prop.get("obs_title");
			if (desc == null)
				desc = mi.prop.get("obs_collection");
			return desc;
		}
		}
		return "";
	}
	
	public Vector<String> getDataLabelAt(int rowIndex) {
		// TODO Auto-generated method stub
		Vector<String> result = null;
		MocItem mi = aladin.directory.multiProp.getItem(this.dataLabels.get(rowIndex));
		String desc = mi.prop.get("obs_title");
		if (desc == null)
			desc = mi.prop.get("obs_collection");
		result = new Vector<String>();
		result.addElement(this.dataLabels.get(rowIndex));
		result.addElement(mi.prop.get("tap_service_url"));
		result.addElement(desc);
		return result;
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

