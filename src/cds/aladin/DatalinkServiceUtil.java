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

import static cds.aladin.Constants.CONTENTLENGTH;
import static cds.aladin.Constants.CONTENTLENGTH_DISPLAY;
import static cds.aladin.Constants.DEFAULT_CONTENTLENGTH_UNITS;
import static cds.aladin.Constants.INPUTPARAM_NAME;
import static cds.aladin.Constants.SERVICE_DEF;
import static cds.aladin.Constants.SPACESTRING;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cds.savot.model.FieldSet;
import cds.savot.model.GroupSet;
import cds.savot.model.ParamSet;
import cds.savot.model.SavotField;
import cds.savot.model.SavotGroup;
import cds.savot.model.SavotParam;
import cds.savot.model.SavotResource;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import cds.tools.Util;

/**
 * Manager class for all functions relating to handling datalink in Aladin
 * @author chaitra
 *
 */
public class DatalinkServiceUtil {
	
	
	public DatalinkServiceUtil() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * This method calls the access url to retrieve the datalinks
	 * @param datalinkUrl 
	 * @return SavotPullParser
	 */
	public static SavotPullParser getDataSets(URL accessUrl) {
		SavotPullParser savotParser = null;
		try {
			if (accessUrl != null) {
				savotParser = new SavotPullParser(accessUrl, SavotPullEngine.FULL, null, false);
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return savotParser;
	}
	
	public static SavotGroup getInputParams(GroupSet groups) {
		SavotGroup group = null;
		if (groups!=null && groups.getItemCount()>0) {
			for (int i = 0; i < groups.getItemCount(); i++) {
				group = (SavotGroup) groups.getItemAt(i);
				if (group.getName().equalsIgnoreCase(INPUTPARAM_NAME)) {
					break;
				}
			}
		}
		return group;
	}
	
	public static SavotParam getInputParams(ParamSet params, String query) {
		SavotParam param = null;
		if (params != null && params.getItemCount() > 0) {
			for (int i = 0; i < params.getItemCount(); i++) {
				param = (SavotParam) params.getItemAt(i);
				if (param.getName().equalsIgnoreCase(query)) {
					break;
				}
				param = null;
			}
		}
		return param;
	}
	
	/**
	 * Method to populate datalink infos sent in the paramter.
	 * Here both the method to make the backend call as well as
	 * method to populate the model object are called.
	 * SimpleData list from xml.
	 * @param datalinkUrl 
	 * @param datalinksInfo - List to populate
	 */
	public static void populateDataLinksInfo(URL datalinkUrl, List<SimpleData> datalinksInfo) {
		if (datalinksInfo==null) {
			return;
		}
		SavotPullParser accessUrlResult = getDataSets(datalinkUrl);
		if (accessUrlResult != null) {	
			SavotResource resultsResource = Util.populateResultsResource(accessUrlResult);
			if (resultsResource != null) {
				SimpleData data = null;
				Map<String,String> params = null;
				for (int i = 0; i < resultsResource.getTableCount(); i++) {
					TRSet tableRows = resultsResource.getTRSet(i);
					FieldSet fieldSet = resultsResource.getFieldSet(i);

					if (fieldSet != null && tableRows != null) {
						for (int j = 0; j < tableRows.getItemCount(); j++) {
							TDSet theTDs = tableRows.getTDSet(j);
							data = new SimpleData();
							params = new HashMap<String,String>();
							String service_def = null;
							for (int k = 0; k < theTDs.getItemCount(); k++) {
								SavotField field = resultsResource.getFieldSet(i).getItemAt(k);
								String key = null;
								String value = theTDs.getContent(k);
								if (field.getId() == null || field.getId().isEmpty()) {
									key = field.getName();
								} else {
									key = field.getId();
								}
								if (key.equalsIgnoreCase(CONTENTLENGTH) && value != null && !value.isEmpty()) {
									try {
										int length = Integer.parseInt(value);
										if (length > 0) {
											String units = field.getUnit();
											if (units == null || units.isEmpty()) {
												units = DEFAULT_CONTENTLENGTH_UNITS;
											}
											value = value + SPACESTRING + units;
											params.put(CONTENTLENGTH_DISPLAY, value);
										}
									} catch (Exception e) {
										// TODO: handle exception
									}
								}
								params.put(key, value);
								
								if (field.getName().equalsIgnoreCase(SERVICE_DEF)) {
									service_def = theTDs.getContent(k);
								}
							}
							data.setParams(params);
							if (service_def!=null && !service_def.isEmpty()) {
								data.setMetaResource(accessUrlResult.getResourceFromRef(service_def));
								service_def = null;
							}
							
							datalinksInfo.add(data);
						}
					}
				}
			}
		}
		
	}
	
	/**
	 * Method checks for repeated elements before adding them to new list
	 * @param possibleRepeats
	 * @param newList
	 */
	public void addOriginalItems(List<SimpleData> possibleRepeats, List<SimpleData> newList) {
		// TODO Auto-generated method stub
		boolean isSame = false;
		for (SimpleData possibleRepeat : possibleRepeats) {
			for (SimpleData simpleData : newList) {
				if (possibleRepeat.isSameAs(simpleData)) {
					isSame = true;
				}
			}
			if (!isSame) {
				newList.add(possibleRepeat);
			}
		}
	}

}
