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

import java.awt.Component;
import java.awt.Dimension;
import java.util.Map.Entry;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

class UploadTablesRenderer extends BasicComboBoxRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4372535217338044431L;
	
	private static UploadTablesRenderer instance;
	
	FrameUploadServer uploadServer;
	
	public UploadTablesRenderer(FrameUploadServer uploadServer) {
		// TODO Auto-generated constructor stub
		this.uploadServer = uploadServer;
	}
	
	public static synchronized UploadTablesRenderer getInstance(TapClient client) {
		if (instance == null) {
			FrameUploadServer uploadServer = client.initUploadFrame(); //not really necessary as we make sure uploadServer is instantiated already. but it is logically better to have it anyway.
			instance = new UploadTablesRenderer(uploadServer);
		}
		return instance;
	}
	
	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		String tooltip = null;
		String valueTxt = (String) value;
		String planeName = getUploadPlaneName(valueTxt);
		if (planeName != null) {
//			valueTxt = valueTxt+" ("+planeName+")";
//			valueTxt = planeName+" | "+valueTxt;
			valueTxt =  "("+planeName+") "+valueTxt;
			tooltip = getToolTip(planeName,  String.valueOf(value));
		}
		setText(valueTxt);
		setToolTipText(tooltip);
//		setIcon(null);
		setPreferredSize(new Dimension(320, Server.HAUT));
		return this;
	}
	
	/**
	 * Gets the plane name corresponding to the upload table name
	 * @param uploadTableName
	 * @return
	 */
	public String getUploadPlaneName(String uploadTableName) {
		String result = null;
		if (!this.uploadServer.uploadTableNameDict.isEmpty()
				&& this.uploadServer.uploadTableNameDict.containsValue(uploadTableName)) {
			for (Entry<String, String> entry : this.uploadServer.uploadTableNameDict.entrySet()) {
				if (entry.getValue().equals(uploadTableName)) {
					result = entry.getKey();
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Convenience method to create tooltip for upload tables dropdown
	 * @param planeName
	 * @param name
	 * @return
	 */
	public static String getToolTip(String planeName, String name) {
		StringBuffer tooltip = new StringBuffer("<html><p width=\"500\">");
		if (JoinFacade.UPLOADJOINTABLENAMETOOLTIP != null) {
			tooltip.append(String.format(JoinFacade.UPLOADJOINTABLENAMETOOLTIP, planeName, name));
		} else {
			tooltip.append(name).append(" can be used as ").append(name).append(" in your adql query. Click on settings to change the table name.");
		}
		tooltip.append("</p></html>");
		return tooltip.toString();
	}

}
