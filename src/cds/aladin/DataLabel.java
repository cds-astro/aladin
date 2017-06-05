// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
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

import javax.swing.JRadioButton;

public class DataLabel {
	private String label;
	private String value;
	private String description;
	protected JRadioButton gui;
	
	public DataLabel() {
		// TODO Auto-generated constructor stub
	}
	public DataLabel(String label, String value, String description){
		this.label = label;
		this.value = value;
		if (description!=null && !description.isEmpty()) {
//			this.description = "<html>"+description+"</html>";
			this.description = description;
		}
		
	}
	
	public DataLabel(String label, String value, String description, boolean setui){
		this(label, value, description);
		if (setui) {
			setUi();
		}
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setAll(String label, String value, String description) {
		this.label = label;
		this.value = value;
		this.description = description;
	}
	
	public void setUi() {
		gui = new JRadioButton(this.label+"  ::"+this.value);
		if (this.description!=null && !this.description.isEmpty()) {
			gui.setToolTipText("<html><p width=\"500\">"+this.description+"</p></html>");
		}
	}
	
	@Override
	public String toString() {
		StringBuffer toPrint = new StringBuffer();
//		toPrint.append("Selected ID: ").append(selectedId).append(", ")
		toPrint.append("Label: ").append(this.label).append(", ")
		.append("Value: ").append(this.value).append(", ")
		.append("Description: ").append(this.description);
		return toPrint.toString();
	}

}
