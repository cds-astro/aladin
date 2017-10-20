package cds.aladin;

import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.event.TableModelEvent;

public class SimpleDataLabelTable extends DataLabelTable {
	Vector<Vector<String>> fullData;
	
	public SimpleDataLabelTable() {
		// TODO Auto-generated constructor stub
		super();
	}
	
	public SimpleDataLabelTable(Aladin aladin, Vector<Vector<String>> dataLabels) {
		// TODO Auto-generated constructor stub
		super(aladin);
		this.fullData = dataLabels;
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
	
	@Override
	public void defaultSortServers() {
		Collections.sort(fullData, new Comparator<Vector<String>>() {
			@Override
			public int compare(Vector<String> o1, Vector<String> o2) {
				int n = o1.get(0).compareTo(o2.get(0));
				if (n != 0)
					return n;
				return o1.get(2).compareTo(o2.get(2));
			}
		});
		if (!ascSort)
			Collections.reverse(fullData);
		ascSort = !ascSort;
	}
	
	public Vector<String> getDataLabelAt(int rowIndex) {
		// TODO Auto-generated method stub
		return this.fullData.get(rowIndex);
	}
	
	@Override
	public void notifyTableChanged() {
		int n = fullData.size();
		if (tableListener != null) {
			tableListener
					.tableChanged(new TableModelEvent(this, n, n, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		}
	}
	
}

