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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import cds.mocmulti.MocItem;

public class MocDataLabelTable extends DataLabelTable {
	List<String> dataLabels;
	
	public MocDataLabelTable() {
		// TODO Auto-generated constructor stub
		super();
	}
	
	public MocDataLabelTable(Aladin aladin, List<String> dataLabels) {
		// TODO Auto-generated constructor stub
		super(aladin);
		this.dataLabels = dataLabels;
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
		case 1: {
			String desc = mi.prop.get("obs_title");
			if (desc == null)
				desc = mi.prop.get("obs_collection");
			return desc;
		}
		case 2: return mi.prop.get("tap_service_url");
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
		result.add(TapFrameServer.labelId, this.dataLabels.get(rowIndex));
		result.add(TapFrameServer.descriptionId, desc);
		result.add(TapFrameServer.urlId, mi.prop.get("tap_service_url"));
		return result;
	}
	
	@Override
	public void defaultSortServers() {
		Collections.sort(dataLabels, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int n = o1.compareTo(o2);
				if (n != 0)
					return n;
				MocItem mi1 = aladin.directory.multiProp.getItem(o1);
				MocItem mi2 = aladin.directory.multiProp.getItem(o2);
				return mi1.prop.get("tap_service_url").compareTo(mi2.prop.get("tap_service_url"));
			}
		});
		if (!ascSort)
			Collections.reverse(dataLabels);
		ascSort = !ascSort;
	}
	
	@Override
	public void notifyTableChanged() {
		int n = dataLabels.size();
		if (tableListener != null) {
			tableListener
					.tableChanged(new TableModelEvent(this, n, n, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		}
	}
	
}

