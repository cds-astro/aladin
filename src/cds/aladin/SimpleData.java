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

/**
 * 
 */
package cds.aladin;

import static cds.aladin.Constants.*;
import java.util.Map;

import cds.savot.model.ParamSet;
import cds.savot.model.SavotParam;
import cds.savot.model.SavotResource;

/**
 * Model class for representing services linked to datalink
 * @author chaitra
 *
 */
public class SimpleData {
	
	private String displayString;
	private String type;
	
	private Map<String,String> params;
	private SavotResource metaResource;
	
	public SimpleData() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * Constructor when parsing using generic parser where values can be
	 * obtained from array
	 * 
	 * @param parameters
	 */
	public SimpleData(String[] parameters) {
		super();
	}
	
	public String getDisplayString() {
		if (this.displayString==null || this.displayString.isEmpty()) {
			this.setDisplayString();
		}
		return displayString;
	}
	
	public void setDisplayString() {
		StringBuffer defaultLinkLabel = new StringBuffer();
		if (this.params.get(DESCRIPTION) != null && !this.params.get(DESCRIPTION).isEmpty()) {
			defaultLinkLabel.append(this.params.get(DESCRIPTION));
		} else {
			this.setType();
			if (this.type == null) {
				defaultLinkLabel.append(this.getParams().get(SEMANTICS));
			} else if (/*this.type.equals("DATALINK_CUTOUT") ||*/ this.type.equals("DATALINK_SERVICE")){
				//get id
				ParamSet inputParams = metaResource.getParams();
				SavotParam idParam = DatalinkManager.getInputParams(inputParams, STANDARDID);
				String standardId = null;
				if (idParam != null) {
					standardId = SimpleData.processStandardIdDisplayString(idParam.getValue());
				}
				if (standardId != null && !standardId.isEmpty()) {
					defaultLinkLabel.append(standardId);
				} else {
					defaultLinkLabel.append(Aladin.chaine.getString(this.type));
				}
				
			} else {
				defaultLinkLabel.append(Aladin.chaine.getString(this.type));
			}
			
		}
		if (this.params.get(CONTENTLENGTH) != null && !this.params.get(CONTENTLENGTH).isEmpty()) {
			defaultLinkLabel.append(" (size ").append(this.params.get(CONTENTLENGTH)).append(" bytes)");
		}
		this.displayString = defaultLinkLabel.toString();
	}

	private static String processStandardIdDisplayString(String value) {
		// TODO Auto-generated method stub
		String displayStandardIdLabel = null;
		if (value != null && value.contains("ivo://ivoa.net/std/")) {
			displayStandardIdLabel = value.replace("ivo://ivoa.net/std/", EMPTYSTRING);
		}
		return displayStandardIdLabel;
	}

	public void setDisplayString(String displayString) {
		this.displayString = displayString;
	}

	public String getType() {
		return type;
	}
	
	/**
	 * Method to set the type of a datalink element.
	 */
	public void setType() {
		String semantic = this.params.get(SEMANTICS);
		String contentType= this.params.get(CONTENTTYPE);
		
		if (semantic.equalsIgnoreCase("#this")) {
			this.type = "DATALINK_THISDATASET";
		} else if (semantic.equalsIgnoreCase("#cutout")) {
			this.type = "DATALINK_CUTOUT";
		} else if (semantic.equalsIgnoreCase("#proc")) {
			this.type = "DATALINK_SERVICE";
		} else if (semantic.equalsIgnoreCase("#preview-image") || semantic.equalsIgnoreCase("#dark")
				|| semantic.equalsIgnoreCase("#flat")) {
			this.type = "DATALINK_IMAGE";
		} else if (contentType!=null) {
			this.type = "DATALINK_METADATA";
			if (contentType.equalsIgnoreCase(CONTENT_TYPE_TEXTXML)) {
				this.type = "DATALINK_METADATA";
			} else if (contentType.equalsIgnoreCase(CONTENT_TYPE_TEXTHTML)) {
				this.type = "DATALINK_MOREINFO";
			} else if (contentType.equalsIgnoreCase(CONTENT_TYPE_APPLICATIONIMG)) {
				this.type = "DATALINK_HIPS";
			} else if (contentType.equalsIgnoreCase(CONTENT_TYPE_APPLICATIONIMG)) {
				this.type = "DATALINK_HIPS";
			} else if (contentType.contains(DATATYPE_DATALINK)) {
				this.type = "DATALINK";
			}
			
			
		}
	}
	
	/**
	 * Method to check if the content of the datasets are same
	 * @param dataToCompare
	 * @return
	 */
	public boolean isSameAs(SimpleData dataToCompare) {
		boolean result = false;
		if (this.toString().equalsIgnoreCase(dataToCompare.toString())) {
			result = true;
		}
		return result;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public SavotResource getMetaResource() {
		return metaResource;
	}

	public void setMetaResource(SavotResource metaResource) {
		this.metaResource = metaResource;
	}

	public Map<String,String> getParams() {
		return params;
	}

	public void setParams(Map<String,String> params) {
		this.params = params;
	}
	
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		StringBuffer stringToPrint = new StringBuffer();
		stringToPrint.append("Display string:").append(this.getDisplayString()).append(", ")
		.append("type:").append(this.getType()).append(", ")
		.append("Params:").append(this.params);
		return stringToPrint.toString();
	}


}
