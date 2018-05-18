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

import static cds.aladin.Constants.AMPERSAND_CHAR;
import static cds.aladin.Constants.BAND;
import static cds.aladin.Constants.BandSODAForm;
import static cds.aladin.Constants.COMMA_CHAR;
import static cds.aladin.Constants.DATALINK_CUTOUT_FORMLABEL;
import static cds.aladin.Constants.DATALINK_FORM;
import static cds.aladin.Constants.DEC_STRING;
import static cds.aladin.Constants.DIMENSIONS;
import static cds.aladin.Constants.DOLLAR_CHAR;
import static cds.aladin.Constants.DateTimeMJD;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.EM_MAX;
import static cds.aladin.Constants.EM_MIN;
import static cds.aladin.Constants.EQUALS_CHAR;
import static cds.aladin.Constants.ID;
import static cds.aladin.Constants.LIMIT_UNKNOWN_MESSAGE;
import static cds.aladin.Constants.NUMBEROFOPTIONS;
import static cds.aladin.Constants.POL;
import static cds.aladin.Constants.POL_DEFAULT_VALUES;
import static cds.aladin.Constants.POL_STATES;
import static cds.aladin.Constants.QUESTIONMARK_CHAR;
import static cds.aladin.Constants.RANGE_DELIMITER;
import static cds.aladin.Constants.RA_STRING;
import static cds.aladin.Constants.SETFORMVALUES;
import static cds.aladin.Constants.SODAPOL_DATATYPE;
import static cds.aladin.Constants.SODA_BANDINDEX;
import static cds.aladin.Constants.SODA_IDINDEX;
import static cds.aladin.Constants.SODA_POLINDEX;
import static cds.aladin.Constants.SODA_POSINDEX1;
import static cds.aladin.Constants.SODA_POSINDEX2;
import static cds.aladin.Constants.SODA_POSINDEX3;
import static cds.aladin.Constants.SODA_STANDARDID;
import static cds.aladin.Constants.SODA_FORM;
import static cds.aladin.Constants.SODA_TIMEINDEX;
import static cds.aladin.Constants.SODA_URL_PARAM;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.STANDARDID;
import static cds.aladin.Constants.S_REGION;
import static cds.aladin.Constants.TIME;
import static cds.aladin.Constants.T_MAX;
import static cds.aladin.Constants.T_MIN;
import static cds.aladin.Constants.T_XEL;
import static cds.aladin.Constants.STCPREFIX_CIRCLE;
import static cds.aladin.Constants.STCPREFIX_POLYGON;
import static cds.aladin.Constants.SODAASYNC_STANDARDID;
import static cds.aladin.Constants.DATALINK_FORM_SYNC;
import static cds.aladin.Constants.DATALINK_FORM_ASYNC;

import static cds.aladin.Constants.UTF8;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import cds.savot.model.ParamSet;
import cds.savot.model.SavotOption;
import cds.savot.model.SavotParam;
import cds.savot.model.SavotResource;
import cds.savot.model.SavotValues;
import cds.tools.Util;
import cds.xml.Field;

public final class DataLinkGlu {
	
	public Aladin aladin;

	protected static Vector vGluDLServer;
	public static String PARAMDISABLEDTOOLTIP;
	
	FrameSimple serviceClientFrame;
	
	public DataLinkGlu() {
		// TODO Auto-generated constructor stub
	}

	protected DataLinkGlu(Aladin aladin) {
		this.aladin = aladin;
		vGluDLServer = new Vector(10);
	}
	
	static {
		PARAMDISABLEDTOOLTIP = Aladin.getChaine().getString("PARAMDISABLEDTOOLTIP");
	}
	

	public String getStandardActionGlu(String formName) {
		return StandardFormsReader.getInstance().getStdServerForms().get(formName);
	}
	
	/**
	 * Creates glu related to invoking services linked to a datalink
	 * In case of a SODA sync, a specialized form is generated.
	 * @param activeDataLinkSource
	 * @param selectedDatalink
	 * @throws Exception
	 */
	protected void createDLGlu(Source activeDataLinkSource, SimpleData selectedDatalink) throws Exception {
		boolean isNonStandardService = true;
		boolean isSODAASync = false;
		
		Hashtable<String,String> paramDescription = new Hashtable<String,String>();
		Hashtable<String,String> paramDataType = new Hashtable<String,String>();
		Hashtable<String,String> paramValue = new Hashtable<String,String>();
		HashMap<String, String[]> paramRange = new HashMap<String, String[]>();
		StringBuilder urlString = null;
		
		SavotResource metaResource = selectedDatalink.getMetaResource();
		ParamSet resourceParams = metaResource.getParams();
		SavotParam resourceParam = null;
		ParamSet inputParams = null;
		String boundaryArea_stcs = null;
		ByteArrayInputStream dicStream = null;
		int syncAsyncMode = -1;
		
		try {
			for (int i = 0; i < resourceParams.getItemCount(); i++) {
				resourceParam = resourceParams.getItemAt(i);
				if (resourceParam.getName().equalsIgnoreCase("accessURL")
						|| resourceParam.getName().equalsIgnoreCase("U")
						|| resourceParam.getName().equalsIgnoreCase("Url")) {
					urlString = new StringBuilder(resourceParam.getValue());
				} else if (resourceParam.getName().equalsIgnoreCase(STANDARDID)
						&& (resourceParam.getValue().equalsIgnoreCase(SODA_STANDARDID)
								|| resourceParam.getValue().equalsIgnoreCase(SODAASYNC_STANDARDID))) {
					
					String sodaGluRecord = aladin.datalinkGlu.getStandardActionGlu(SODA_FORM);
					
					inputParams = DatalinkServiceUtil.getInputParams(metaResource.getGroups()).getParams();
					SavotParam idParam = DatalinkServiceUtil.getInputParams(inputParams, ID);
					paramValue.put(String.valueOf(SODA_IDINDEX), getParamValue(idParam, selectedDatalink));
					
					boolean noParamSet = true;
					if (resourceParam.getValue().equalsIgnoreCase(SODAASYNC_STANDARDID)) {
						isSODAASync = true;
					}
					syncAsyncMode = setSyncAsyncMode(isSODAASync);
					sodaGluRecord = addSyncAsyncParam(sodaGluRecord, syncAsyncMode);
					
					if (sodaGluRecord != null) {
						dicStream = new ByteArrayInputStream(sodaGluRecord.getBytes(StandardCharsets.UTF_8));
						noParamSet = this.setSODAFormParams(activeDataLinkSource, paramDescription, paramDataType, paramValue, paramRange);
					} else {
						paramDescription.put(String.valueOf(SODA_IDINDEX), ID);
						paramDataType.put(String.valueOf(SODA_IDINDEX), resourceParam.getDataType());
						noParamSet = this.setCompleteSODAForm(activeDataLinkSource,selectedDatalink , paramDescription, paramDataType, paramValue, paramRange);
					}
					
					int sRegionIndex = activeDataLinkSource.findColumn(S_REGION);
					if (sRegionIndex != -1) {
						boundaryArea_stcs = activeDataLinkSource.getValue(sRegionIndex);
						if (boundaryArea_stcs != null && !boundaryArea_stcs.isEmpty()) {
							noParamSet = false;
						}
					}
					
					//Note: noParamSet: if we do not get any values from the original table to set in service client, then we use values from the datalink response.
					if (noParamSet) {
						this.setSODAFormParams(selectedDatalink, paramDescription, paramDataType, paramValue, paramRange);
						boundaryArea_stcs = getFovFromDatalinkResponse(inputParams);
					}
					
					/*if (boundaryArea_stcs != null) {
						STCStringParser parser = new STCStringParser();
						List<STCObj> stcObjects = parser.parse(boundaryArea_stcs);
						sourceMoc = aladin.createMocRegion(stcObjects);
					}*/
					
					/*if (sourceMoc!=null) {
						Command command = new Command(aladin);
			        	String stcresult = "Draw moc("+(sourceMoc.getMocOrder()+1)+"/"+sourceMoc.rangeSet.toString().replaceAll(";",",").replaceAll("\\[", "")+")";
						System.out.println(stcresult.replaceAll("[\\{\\}]", ""));
						command.drawFromCode(stcresult.replaceAll("[\\{\\}]", ""));
					}*/
					
					isNonStandardService = false;
				}
			}
			
			if (isNonStandardService) {
				//Generic service
				inputParams = DatalinkServiceUtil.getInputParams(metaResource.getGroups()).getParams();
				urlString.append(QUESTIONMARK_CHAR);
				String index;
				SavotParam param = null;
				for (int i = 0; i < inputParams.getItemCount(); i++) {
					index = String.valueOf(i+1);
					param = (SavotParam) inputParams.getItemAt(i);
					paramDescription.put(index, param.getName());
					paramDataType.put(index, param.getDataType());
					paramValue.put(index, getParamValue(param, selectedDatalink));
					addIndexedQueryParameter(urlString, param.getName(), index);
//					System.out.println(urlString.toString());
				}
				aladin.glu.aladinDic.put(DATALINK_FORM, urlString.toString());
			} else {
//				urlString.append("?POS=circle+$1+$2+$3&TIME=$4&BAND=$5&POL=$6&ID=$7");//cadc works with this not CIRCLE...
				urlString.append(SODA_URL_PARAM);
				aladin.glu.aladinDic.put(DATALINK_FORM, urlString.toString());
				if (syncAsyncMode > 0) {
					if (isSODAASync) {
						aladin.glu.aladinDic.put(DATALINK_FORM_ASYNC, urlString.toString());
					} else {
						aladin.glu.aladinDic.put(DATALINK_FORM_SYNC, urlString.toString());
					}
				}
			}
			
			Vector serverVector = null;
			if (dicStream!=null) { //in case this is soda sync and form is loaded from configuration. Hence using generic glu
				aladin.glu.vGluServer = new Vector(50);
				aladin.glu.loadGluDic(new DataInputStream(dicStream),true,false);
	            serverVector = aladin.glu.vGluServer;
			} else {
				Vector  aladinFilter = new Vector(10);
				StringBuffer record =new StringBuffer(1000);
			    record.append("%A ").append(DATALINK_FORM).append("\n%D Cutout interface for SODA server");
			    ServerGlu g = serverDataLinks(DATALINK_FORM, "Cutout service", null, null, null, DATALINK_CUTOUT_FORMLABEL,
			    		"Cutout service", null, paramDescription, paramDataType, paramValue, paramRange,
		   	         "Mime(application/xml)", syncAsyncMode, null, aladinFilter, null, null, null, record, null);
				serverVector = vGluDLServer;
				if (g != null && g.grab != null) {
					g.grab.setEnabled(true);//Default true for datalink forms
				}
			}
			
			int n = serverVector.size();
	        if( n == 0 ) return;
	        ServerGlu dlGlu = (ServerGlu) serverVector.get(0);
	        if (!isNonStandardService) {
//	        	dlGlu.setPosBounds(sourceMoc);
	        	dlGlu.setBoundaryAreaStcs(boundaryArea_stcs);
	        	dlGlu.setDataLinkSource(activeDataLinkSource);
	        	if (dicStream != null) {
	        		dlGlu.setAdditionalGluParams(paramRange, paramValue);
				}
	        	//decide to have time param
	        	boolean disableTime = true;
	        	int tXelIndex = activeDataLinkSource.findColumn(T_XEL);
	        	if (tXelIndex > 0) {
	        		String tXelValue = activeDataLinkSource.getValue(tXelIndex);
					if (tXelValue != null) {
						//logic now is if t_xel =1 then time is known factor and hence we can constrain time
						//logic added as we saw a usecase with califa from gavo
						//as discussed with Francois Bonnarel, we don't do this for other soda params for now
						try {
							int tXelIntValue = Integer.parseInt(tXelValue);
							if (tXelIntValue == 1) {
								disableTime = false;
							}
						} catch (Exception e) {
							// TODO: handle exception
						}
					}
	        		
				}
	        	if (disableTime) {
	        		dlGlu.disableDateField(String.format(PARAMDISABLEDTOOLTIP, TIME));
				}
			}
	        String label = getFormLabel(urlString.toString());
		    String planeLabel = null;
		    if (label != null) {
				if (!isNonStandardService || isSODAASync) {
					planeLabel = "[SODA]" + label;
				} else {
					planeLabel = "[Cutout]"+ label;
				}
			}
		    dlGlu.aladinLabel = label;
		    dlGlu.planeLabel = planeLabel;
//			aladin.datalinkGlu.reload(false, serverVector);
			
			if (serviceClientFrame == null) {
				serviceClientFrame = new FrameSimple(aladin);
			}
//			Aladin.makeCursor(tapPanelFromTree, WAIT);
			serviceClientFrame.show(dlGlu, "Service "+dlGlu.aladinLabel);
//			Aladin.makeCursor(tapPanelFromTree, DEFAULT);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	    
	}

	private String getFormLabel(String url) {
		// TODO Auto-generated method stub
		String result = null;
		try {
			result = Util.getDomainNameFromUrl(url);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	public String addSyncAsyncParam(String gluRecord, int syncAsyncMode) {
		gluRecord = gluRecord.concat("%ResultSubmitType ")+syncAsyncMode;
		return gluRecord;
	}
	
	public int setSyncAsyncMode(boolean isSODAASync) {
		int syncAsyncMode = -1;
		if (isSODAASync) {
			String url = checkOtherOptionExistsAndGetUrl(SODA_STANDARDID);
			if (url != null) {
				syncAsyncMode = 2;
				url = url.concat(SODA_URL_PARAM);
				aladin.glu.aladinDic.put(DATALINK_FORM_SYNC, url);
			} else {
				syncAsyncMode = 0;
			}
		} else {
			String url = checkOtherOptionExistsAndGetUrl(SODAASYNC_STANDARDID); 
			if (url != null) {
				syncAsyncMode = 1;
				url = url.concat(SODA_URL_PARAM);
				aladin.glu.aladinDic.put(DATALINK_FORM_ASYNC, url);
			}
			
		}
		return syncAsyncMode;
	}
	
	/**
	 * Checks if the resource with input standardId exists in the datalink options
	 * @param standardId
	 * @return
	 */
	public String checkOtherOptionExistsAndGetUrl(String standardId) {
		boolean exists = false;
		String urlString = null;
		
		datalinkInfoLoop:
		for (SimpleData datalinkInfo : this.aladin.mesure.activeDataLinkWord.datalinksInfo) {
			urlString = null;
			SavotResource metaResource = datalinkInfo.getMetaResource();
			if (metaResource != null) {
				ParamSet resourceParams = metaResource.getParams();
				SavotParam resourceParam = null;
				for (int i = 0; i < resourceParams.getItemCount(); i++) {
					resourceParam = resourceParams.getItemAt(i);
					if (resourceParam.getName().equalsIgnoreCase("accessURL")
							|| resourceParam.getName().equalsIgnoreCase("U")
							|| resourceParam.getName().equalsIgnoreCase("Url")) {
						urlString = resourceParam.getValue();
					} else if (resourceParam.getName().equalsIgnoreCase(STANDARDID)
							&& resourceParam.getValue().equalsIgnoreCase(standardId)) {
						exists = true;
					}
					if (exists && urlString != null) {
						break datalinkInfoLoop;
					}
				}
			}
		}
		return urlString;
	}

	/**
	 * Method extracts FOV form datalink reponse
	 * Priority is for polygon
	 * and if that is not present we aim to take in circle
	 * Logic is to construct the stc string by taking parameter value with name = "polygon" or "circle"
	 * we do not check other params because:
	 * utypes- do not match
		gavo does not have xtype=polygon
		so just take name and append values 
	 * @param selectedDatalink
	 * @return
	 */
	private String getFovFromDatalinkResponse(ParamSet inputParamSet) {
		// TODO Auto-generated method stub
		String stcString = null;
		String shape = STCPREFIX_POLYGON;
		SavotParam param = DatalinkServiceUtil.getInputParams(inputParamSet, "POLYGON");
		if (param == null) {
			param = DatalinkServiceUtil.getInputParams(inputParamSet, "CIRCLE");
			shape = STCPREFIX_CIRCLE;
		}
		
		if (param != null && param.getValues() != null && param.getValues().getMax() != null
				&& param.getValues().getMax().getValue() != null && !param.getValues().getMax().getValue().isEmpty()) {
			stcString = shape + param.getValues().getMax().getValue();
		}
		return stcString;
	}

	/**
	 * Method assumes one datalink glu at a time.
	 * 
	 * @return
	 */
	public ServerGlu getDataLinkServerGlu() {
		return (ServerGlu) vGluDLServer.get(0);
	}

	/**
	 * This method extracts the input paramters required to make a standard SODA
	 * call.
	 * 
	 * @param activeDatalinkSource
	 * @param selectedDatalink
	 * @param paramRange
	 * @return 
	 * @return
	 */
	public boolean setCompleteSODAForm(Source activeDatalinkSource, SimpleData selectedDatalink,
			Hashtable<String, String> paramDescription, Hashtable<String, String> paramDataType,
			Hashtable<String, String> paramValue, HashMap<String, String[]> paramRange) {

		if (activeDatalinkSource == null)
			return false;

		// POS
		setSODATargetParameter(paramDescription, paramDataType, paramValue);

		// others
		return setSODAFormParams(activeDatalinkSource, paramDescription, paramDataType, paramValue,
				paramRange);

	}

	public boolean setSODAFormParams(Source activeDatalinkSource,
			Hashtable<String, String> paramDescription, Hashtable<String, String> paramDataType,
			Hashtable<String, String> paramValue, HashMap<String, String[]> paramRange) {

		// time
		boolean noFieldSet = setSODAFormParam(activeDatalinkSource,
				String.valueOf(SODA_TIMEINDEX), paramDescription, paramDataType, DateTimeMJD, paramValue, SETFORMVALUES,
				paramRange, TIME, activeDatalinkSource.findColumn(T_MIN), activeDatalinkSource.findColumn(T_MAX));

		// band
		noFieldSet = noFieldSet
				| setSODAFormParam(activeDatalinkSource, String.valueOf(SODA_BANDINDEX),
						paramDescription, paramDataType, BandSODAForm, paramValue, SETFORMVALUES, paramRange, BAND,
						activeDatalinkSource.findColumn(EM_MIN), activeDatalinkSource.findColumn(EM_MAX));

		// pol
		int polStatesIndex = activeDatalinkSource.findColumn(POL_STATES);
		String pol_states = null;
		if (polStatesIndex != -1) {
			pol_states = activeDatalinkSource.getValue(polStatesIndex);
		}
		if (pol_states == null || pol_states.isEmpty()) {
			pol_states = POL_DEFAULT_VALUES;
		} else {
			noFieldSet = false;
		}

		setSODAFormParam(activeDatalinkSource, String.valueOf(SODA_POLINDEX), paramDescription,
				paramDataType, SODAPOL_DATATYPE, paramValue, pol_states, paramRange, POL,
				activeDatalinkSource.findColumn(POL_STATES), -1);

		// if no parameter values are present in the original table: then we
		// load value from the datalink response if it contains any parameters
		// values
		// offcourse pos is not done
		// but band, time, pol are set
		// and standard id is anyway taken form the datalink response
		
		return noFieldSet;
	}
	
	public boolean setSODAFormParams(SimpleData selectedDatalink,
			Hashtable<String, String> paramDescription, Hashtable<String, String> paramDataType,
			Hashtable<String, String> paramValue, HashMap<String, String[]> paramRange) {

		setSODAFormParam(selectedDatalink, String.valueOf(SODA_TIMEINDEX), paramDescription,
				paramDataType, DateTimeMJD, paramValue, SETFORMVALUES, paramRange, TIME);

		// band
		setSODAFormParam(selectedDatalink, String.valueOf(SODA_BANDINDEX),
						paramDescription, paramDataType, BandSODAForm, paramValue, SETFORMVALUES, paramRange, BAND);

		String pol_states = POL_DEFAULT_VALUES;
		setSODAFormParam(selectedDatalink, String.valueOf(SODA_POLINDEX), paramDescription,
				paramDataType, SODAPOL_DATATYPE, paramValue, pol_states, paramRange, POL);
		
		return true;
	}

	public static boolean setSODAFormParam(SimpleData selectedDatalink, String paramFormIndex,
			Hashtable<String, String> paramDescription, Hashtable<String, String> paramDataType,
			String paramDataTypeDefault, Hashtable<String, String> paramValue, String paramDefaultValue,
			HashMap<String, String[]> paramRange, String paramName) {

		paramDescription.put(paramFormIndex, paramName);

		ParamSet inputParams = DatalinkServiceUtil.getInputParams(selectedDatalink.getMetaResource().getGroups()).getParams();
		SavotParam ipParam = DatalinkServiceUtil.getInputParams(inputParams, paramName);
		
		if (paramDataTypeDefault == null) {
			if (ipParam != null && ipParam.getDataType() != null && !ipParam.getDataType().isEmpty()) {
				paramDataType.put(paramFormIndex, Field.typeVOTable2Fits(ipParam.getDataType()));
			}
		} else {
			paramDataType.put(paramFormIndex, paramDataTypeDefault);
		}
		
		String lowerLimit = null;
		String upperLimit = null;
		
		if (ipParam != null) {//range
			String dlParamValue = getParamValue(ipParam, selectedDatalink);
			if (dlParamValue == null || dlParamValue.isEmpty()) {
				SavotValues savotValues = ipParam.getValues();
				if (savotValues != null) {
					if (savotValues.getMin() != null && savotValues.getMax() != null) {
						lowerLimit = savotValues.getMin().getValue();
						upperLimit = savotValues.getMax().getValue();
					} else if (savotValues.getOptions() != null) {
						StringBuffer optionString = new StringBuffer();
						boolean isNotFirst = false;
						for (SavotOption option : savotValues.getOptions().getItems()) {
							if (isNotFirst) {
								optionString.append("/");
							}
							optionString.append(option.getValue());
							isNotFirst = true;
						}
						paramDefaultValue = optionString.toString();
					}
					
				}
			}
		}
		return setValueAndRange(paramFormIndex, paramValue, paramDefaultValue, paramRange, paramName, lowerLimit, upperLimit);
	}

	public void setSODATargetParameter(Hashtable<String, String> paramDescription,
			Hashtable<String, String> paramDataType, Hashtable<String, String> paramValue) {
		// POS
		paramDescription.put(String.valueOf(SODA_POSINDEX1), RA_STRING);
		paramDescription.put(String.valueOf(SODA_POSINDEX2), DEC_STRING);
		paramDescription.put(String.valueOf(SODA_POSINDEX3), DIMENSIONS);
		paramDataType.put(String.valueOf(SODA_POSINDEX1), "Target(RAd)");
		paramDataType.put(String.valueOf(SODA_POSINDEX2), "Target(DEd)");
		paramDataType.put(String.valueOf(SODA_POSINDEX3), "Field(STRINGd)");
		paramValue.put(String.valueOf(SODA_POSINDEX1), EMPTYSTRING);
		paramValue.put(String.valueOf(SODA_POSINDEX2), EMPTYSTRING);
		paramValue.put(String.valueOf(SODA_POSINDEX3), EMPTYSTRING);
	}
	   	
	   	/**
	   	 * Method to set one form field's parameters
	   	 * @param activeDatalinkSource
	   	 * @param paramFormIndex
	   	 * @param paramDescription
	   	 * @param paramDataType
	   	 * @param paramDataTypeDefault
	   	 * @param paramValue
	   	 * @param paramDefaultValue
	   	 * @param paramRange
	   	 * @param paramName
	   	 * @param lowerLimitIndex
	   	 * @param upperLimitIndex
	   	 * @return 
	   	 */
	public static boolean setSODAFormParam(Source activeDatalinkSource, String paramFormIndex,
			Hashtable<String, String> paramDescription, Hashtable<String, String> paramDataType,
			String paramDataTypeDefault, Hashtable<String, String> paramValue, String paramDefaultValue,
			HashMap<String, String[]> paramRange, String paramName, int lowerLimitIndex, int upperLimitIndex) {

		paramDescription.put(paramFormIndex, paramName);
		if (paramDataTypeDefault == null) {
			if (activeDatalinkSource != null) {
				if (lowerLimitIndex != -1) {
					Field[] field = activeDatalinkSource.getLeg().field;
					paramDataType.put(paramFormIndex, field[lowerLimitIndex].datatype);
				}
			}
		} else {
			paramDataType.put(paramFormIndex, paramDataTypeDefault);
		}
		
		String lowerLimit = null;
		String upperLimit = null;
		
		if (upperLimitIndex != -1) {
			lowerLimit = activeDatalinkSource.getValue(lowerLimitIndex);
			upperLimit = activeDatalinkSource.getValue(upperLimitIndex);
		}
		
		return setValueAndRange(paramFormIndex, paramValue, paramDefaultValue, paramRange, paramName, lowerLimit, upperLimit);
	}
	
	public static boolean setValueAndRange(String paramFormIndex, Hashtable<String, String> paramValue,
			String paramDefaultValue, HashMap<String, String[]> paramRange, String paramName,
			String lowerLimit, String upperLimit) {
		boolean noFieldSet = true;
		if (upperLimit != null) {
			String[] anArray = new String[NUMBEROFOPTIONS];
			anArray[0] = lowerLimit;
			anArray[1] = upperLimit;
			paramRange.put(paramFormIndex, anArray);

			if (lowerLimit != null && !lowerLimit.isEmpty() && upperLimit != null && !upperLimit.isEmpty()) {
				if (paramDefaultValue.equalsIgnoreCase(SETFORMVALUES)) {
					String delimiter = SPACESTRING;
					if (paramName.equalsIgnoreCase(TIME)) {
						delimiter = COMMA_CHAR;
					}
					StringBuffer paramValueDisplay = new StringBuffer(lowerLimit).append(delimiter).append(upperLimit);
					paramValue.put(paramFormIndex, paramValueDisplay.toString());
					noFieldSet = false;
				}
			}
			/* paramHint.put(paramFormIndex, getSODAParamHint(activeDatalinkSource.getValue(lowerLimit), activeDatalinkSource.getValue(upperLimit))+ "\n" + hintMessage);*/
		} else if (!paramDefaultValue.equalsIgnoreCase(SETFORMVALUES)) {
			if (paramDefaultValue.contains("/")) {
				paramValue.put(paramFormIndex, paramDefaultValue.replaceAll("/", "\t"));
				String[] paramDefaultValues = paramDefaultValue.split("/");
				String[] anArray = Arrays.copyOf(paramDefaultValues, NUMBEROFOPTIONS);
				paramRange.put(paramFormIndex, anArray);
			} else {
				paramValue.put(paramFormIndex, paramDefaultValue);
			}

		}
		return noFieldSet;
	}
		
		/**
		 * Method to set a form paramter hint
		 * @param lowerLimitValue
		 * @param upperLimitValue
		 * @return hintString
		 */
		public static String getSODAParamHint(String lowerLimitValue, String upperLimitValue) {
			String result = LIMIT_UNKNOWN_MESSAGE;
			if (lowerLimitValue != null && upperLimitValue != null
					&& !lowerLimitValue.replaceAll("\\s", "").equalsIgnoreCase(EMPTYSTRING)
					&& !upperLimitValue.replaceAll("\\s", "").equalsIgnoreCase(EMPTYSTRING)) {
				
				StringBuffer sodaHint = new StringBuffer("Valid Range: ").append(lowerLimitValue);
				sodaHint.append(RANGE_DELIMITER).append(upperLimitValue);
				result = sodaHint.toString();
			}
			return result;
		}
	
	public ServerGlu serverDataLinks(String actionName, String description, String verboseDescr, String aladinMenu,
			String aladinMenuNumber, String aladinLabel, String aladinLabelPlane, String docUser,
			Hashtable paramDescription1, Hashtable paramDataType1, Hashtable paramValue1,
			HashMap<String, String[]> paramRange2, String resultDataType, int showSyncAsync, String institute,
			Vector aladinFilter1, String aladinLogo, String dir, String system, StringBuffer record,
			String aladinProtocol) {
	   int i;
	   if( paramDescription1 == null ) return null;
	      int n = paramDescription1.size();
	      String[] paramDescription = new String[n];
	      for( i = 1; i <= n; i++ )
	         paramDescription[i - 1] = (String) paramDescription1.get(i + "");

	      String[] paramDataType = new String[n];
	      for( i = 1; i <= n; i++ )
	         paramDataType[i - 1] = (String) paramDataType1.get(i + "");

	      String[] paramValue = new String[n];
	      for( i = 1; i <= n; i++ )
	         paramValue[i - 1] = (String) paramValue1.get(i + "");
	      
	      String[][] paramRange = new String[n][NUMBEROFOPTIONS];
	      
	      for( i = 1; i <= n; i++ ){
	    	  String[] paramRanges = paramRange2.get(i + "");
	    	  if (paramRanges!=null) {
	    		  for (int j = 0; j < NUMBEROFOPTIONS; j++) {
	    			  paramRange[i - 1][j] = String.valueOf(paramRange2.get(i + "")[j]);
				}
	    	  } else {
	    		  paramRange[i - 1][0] = null;
		    	  paramRange[i - 1][1] = null;
	    	  }
	    	  
	      }

	      String aladinFilter[] = null;
	      if( aladinFilter1 != null && (n = aladinFilter1.size()) > 0 ) {
	         aladinFilter = new String[n];
	         Enumeration e = aladinFilter1.elements();
	         for( i = 0; i < n; i++ )
	            aladinFilter[i] = (String) e.nextElement();
	      }

	      if( system!=null && system.trim().length()==0 ) system=null;
	      if( institute == null ) institute = description;
	      
		ServerGlu g = null;
		if (aladin != null) {
			g = new ServerGlu(aladin, actionName, description, verboseDescr, aladinMenu, aladinMenuNumber, aladinLabel,
					aladinLabelPlane, docUser, paramDescription, paramDataType, paramValue, paramRange, resultDataType,
					showSyncAsync, institute, aladinFilter, aladinLogo, dir, system, record, aladinProtocol, null, null, 
					null, false);
			vGluDLServer.clear();
			vGluDLServer.addElement(g);
		}
		return g;
  }
	
	/**
	 * Method to append query parameter
	 * @param queryString
	 * @param name
	 * @param value
	 * @throws UnsupportedEncodingException 
	 */
	public static void addIndexedQueryParameter(StringBuilder queryString, String name, Object value) throws UnsupportedEncodingException {
		if (queryString!=null) {
			try {
				queryString.append(URLEncoder.encode(name, UTF8)).append(EQUALS_CHAR).append(DOLLAR_CHAR)
						.append(URLEncoder.encode(value.toString(), UTF8)).append(AMPERSAND_CHAR);;
			} catch (UnsupportedEncodingException e) {
				throw e;
			}
		}
	}
	
	/**
	 * Method to get value from a savot resource element
	 * @param param
	 * @param selectedDatalink
	 * @return
	 */
	public static String getParamValue(SavotParam param, SimpleData selectedDatalink) {
		String value = "";
		if (param.getRef()!=null && !param.getRef().isEmpty()) {
			value = selectedDatalink.getParams().get(param.getRef());
		}
		else {
			value = param.getValue();
		}
		return value;
	}

	
}
