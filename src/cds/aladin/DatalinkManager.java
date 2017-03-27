/**
 * 
 */
package cds.aladin;

import static cds.aladin.Constants.INPUTPARAM_NAME;
import static cds.aladin.Constants.SERVICE_DEF;

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
public class DatalinkManager {
	
	SavotPullParser savotParser;
	URL accessUrl;
	SavotResource resultsResource;
	
	public DatalinkManager() {
		// TODO Auto-generated constructor stub
	}
	
	public DatalinkManager(URL accessUrl) {
		this.accessUrl = accessUrl;
	}
	
	/**
	 * This method calls the access url to retrieve the datalinks
	 * @return SavotPullParser
	 */
	public SavotPullParser getDataSets() {
		try {
			if (this.accessUrl!=null) {
				savotParser = new SavotPullParser(this.accessUrl, SavotPullEngine.FULL, null, false);
				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return savotParser;
	}
	
	/**
	 * Method to get a resource related to a service
	 * @param id
	 * @return SavotResource
	 */
	public SavotResource getResourceById(String id) {
		return this.savotParser.getResourceFromRef(id);
		
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
		if (params!=null && params.getItemCount()>0) {
			for (int i = 0; i < params.getItemCount(); i++) {
				param = (SavotParam) params.getItemAt(i);
				if (param.getName().equalsIgnoreCase(query)) {
					break;
				}
			}
		}
		return param;
	}
	
	/**
	 * Method to populate datalink infos sent in the paramter.
	 * Here both the method to make the backend call as well as
	 * method to populate the model object are called.
	 * SimpleData list from xml.
	 * @param datalinksInfo - List to populate
	 */
	public void populateDataLinksInfo(List<SimpleData> datalinksInfo) {
		if (datalinksInfo==null) {
			return;
		}
		SavotPullParser accessUrlResult = this.getDataSets();
		if (accessUrlResult!=null) {		
			populateResultsResource();
			if (resultsResource != null) {
				SimpleData data = null;
				Map<String,String> params = null;
				for (int i = 0; i < resultsResource.getTableCount(); i++) {
					TRSet tableRows = resultsResource.getTRSet(i);
					FieldSet fieldSet = resultsResource.getFieldSet(i);

					if (fieldSet != null && tableRows!=null) {
						for (int j = 0; j < tableRows.getItemCount(); j++) {
							TDSet theTDs = tableRows.getTDSet(j);
							data = new SimpleData();
							params = new HashMap<String,String>();
							String service_def = null;
							for (int k = 0; k < theTDs.getItemCount(); k++) {
								SavotField field = resultsResource.getFieldSet(i).getItemAt(k);
								if (field.getId() == null || field.getId().isEmpty()) {
									params.put(field.getName(), theTDs.getContent(k));
								} else {
									params.put(field.getId(), theTDs.getContent(k));
								}
								
								if (field.getName().equalsIgnoreCase(SERVICE_DEF)) {
									service_def = theTDs.getContent(k);
								}
							}
							data.setParams(params);
							if (service_def!=null && !service_def.isEmpty()) {
								data.setMetaResource(this.getResourceById(service_def));
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
	 * Method to extract resource of type="results"
	 * @param resourceSet
	 * @return results typed SavotResource
	 */
	public SavotResource populateResultsResource() {
		this.resultsResource = Util.populateResultsResource(this.savotParser);
		return this.resultsResource;
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
	
	
	public URL getAccessUrl() {
		return accessUrl;
	}

	public void setAccessUrl(URL accessUrl) {
		this.accessUrl = accessUrl;
	}

	public SavotResource getResultsResource() {
		if (resultsResource == null) {
			populateResultsResource();
		}
		return resultsResource;
	}

	public void setResultsResource(SavotResource resultsResource) {
		this.resultsResource = resultsResource;
	}


}
