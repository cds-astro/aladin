// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
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
import static cds.aladin.Constants.SODA_SYNC_FORM;
import static cds.aladin.Constants.SODA_TIMEINDEX;
import static cds.aladin.Constants.SODA_URL_PARAM;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.STANDARDID;
import static cds.aladin.Constants.S_REGION;
import static cds.aladin.Constants.TIME;
import static cds.aladin.Constants.T_MAX;
import static cds.aladin.Constants.T_MIN;
import static cds.aladin.Constants.UTF8;

import java.awt.Dimension;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCStringParser;
import cds.moc.HealpixMoc;
import cds.savot.model.ParamSet;
import cds.savot.model.SavotParam;
import cds.savot.model.SavotResource;
import cds.xml.Field;

public final class DataLinkGlu {
	
	public Aladin aladin;

	protected static Vector vGluDLServer;
	
	public DataLinkGlu() {
		// TODO Auto-generated constructor stub
	}

	protected DataLinkGlu(Aladin aladin) {
		this.aladin = aladin;
		vGluDLServer = new Vector(10);
	}

	public String getStandardActionGlu(String formName) {
		return StandardFormsReader.getInstance().getStdServerForms().get(formName);
	}
	
	/**
	 * Creates glu related to invoking services linked to a datalink
	 * Incase of a SODA sync, a specialised form is generated.
	 * @param resultsResource
	 * @param activeDataLinkSource
	 * @param selectedDatalink
	 * @throws Exception
	 */
	protected void createDLGlu(SavotResource resultsResource, Source activeDataLinkSource, SimpleData selectedDatalink) throws Exception {
		boolean isNonStandardService = true;
		
		Hashtable<String,String> paramDescription = new Hashtable<String,String>();
		Hashtable<String,String> paramDataType = new Hashtable<String,String>();
		Hashtable<String,String> paramValue = new Hashtable<String,String>();
		HashMap<String, String[]> paramRange = new HashMap<String, String[]>();
		StringBuilder urlString = null;
		
		SavotResource metaResource = selectedDatalink.getMetaResource();
		ParamSet resourceParams = metaResource.getParams();
		SavotParam resourceParam = null;
		ParamSet inputParams = null;
		HealpixMoc sourceMoc = null;
		ByteArrayInputStream dicStream = null;
		
		try {
			for (int i = 0; i < resourceParams.getItemCount(); i++) {
				resourceParam = resourceParams.getItemAt(i);
				if(resourceParam.getName().equalsIgnoreCase("accessURL") || resourceParam.getName().equalsIgnoreCase("U") || resourceParam.getName().equalsIgnoreCase("Url") ) {
					urlString = new StringBuilder(resourceParam.getValue());
				}
				
				//Check if this is a standard SODA service
				//TODO:: handle SODA async as well.
				//Note: TODO:: tintin if we do not get any values from the original table to set in cutout client, then we use values form the datalink response.
				if (resourceParam.getName().equalsIgnoreCase(STANDARDID) && resourceParam.getValue().equalsIgnoreCase(SODA_STANDARDID)) {
					String sodaGluRecord = aladin.datalinkGlu.getStandardActionGlu(SODA_SYNC_FORM);
					
					inputParams = DatalinkManager.getInputParams(metaResource.getGroups()).getParams();
					SavotParam idParam = DatalinkManager.getInputParams(inputParams, ID);
					paramValue.put(String.valueOf(SODA_IDINDEX), this.getParamValue(idParam, selectedDatalink));
					
					if (sodaGluRecord!=null) {
						dicStream = new ByteArrayInputStream(sodaGluRecord.getBytes(StandardCharsets.UTF_8));
						this.setSODAFormTimeBandPol(activeDataLinkSource, paramDescription, paramDataType, paramValue, paramRange);
					} else {
						paramDescription.put(String.valueOf(SODA_IDINDEX), ID);
						paramDataType.put(String.valueOf(SODA_IDINDEX), resourceParam.getDataType());
						this.setCompleteSODAForm(activeDataLinkSource, paramDescription, paramDataType, paramValue, paramRange);
					}
					
					int sRegionIndex = activeDataLinkSource.findColumn(S_REGION);
					if (sRegionIndex != -1) {
						String boundaryArea_stcs = activeDataLinkSource.getValue(sRegionIndex);
						
						STCStringParser parser = new STCStringParser();
						List<STCObj> stcObjects = parser.parse(boundaryArea_stcs);
						sourceMoc = aladin.createMocRegion(stcObjects);
					}
					
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
				inputParams = DatalinkManager.getInputParams(metaResource.getGroups()).getParams();
				urlString.append(QUESTIONMARK_CHAR);
				String index;
				SavotParam param = null;
				for (int i = 0; i < inputParams.getItemCount(); i++) {
					index = String.valueOf(i+1);
					param = (SavotParam) inputParams.getItemAt(i);
					paramDescription.put(index, param.getName());
					paramDataType.put(index, param.getDataType());
					paramValue.put(index, this.getParamValue(param, selectedDatalink));
					addIndexedQueryParameter(urlString, param.getName(), index);
//					System.out.println(urlString.toString());
				}
				aladin.glu.aladinDic.put(DATALINK_FORM, urlString.toString());
				
			} else {
				urlString.append(SODA_URL_PARAM);
				aladin.glu.aladinDic.put(DATALINK_FORM, urlString.toString());
			}
			
			Vector serverVector = null;
			if (dicStream!=null) { //in case this is soda sync and form is loaded from configuration. Hence using generic glu
				aladin.glu.vGluServer = new Vector(50);
				aladin.glu.loadGluDic(new DataInputStream(dicStream),true,false);
	            serverVector = aladin.glu.vGluServer;
			} else {
				Vector  aladinFilter = new Vector(10);
				StringBuffer record =new StringBuffer(1000);
			    record.append("%A ").append(DATALINK_FORM).append("\n%D Cutout prototype for SODA sync server");
			    serverDataLinks(DATALINK_FORM, "Cutout service", null, null, null, DATALINK_CUTOUT_FORMLABEL,
		   	         "Cutout service", null, paramDescription, paramDataType, paramValue, paramRange,
		   	         "Mime(application/xml)", null, aladinFilter, null, null, null, record, null);
				serverVector = vGluDLServer;
			}
			
			int n = serverVector.size();
	        if( n == 0 ) return;
	        ServerGlu dlGlu = (ServerGlu) serverVector.get(0);
	        if (!isNonStandardService) {
	        	dlGlu.setPosBounds(sourceMoc);
	        	if (dicStream!=null) {
	        		dlGlu.setAdditionalGluParams(paramRange, paramValue);
				}
	            
			}
			aladin.datalinkGlu.reload(false, serverVector);
			
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	    
	}
	
	/**
	 * Method assumes one datalink glu at a time.
	 * @return
	 */
	public ServerGlu getDataLinkServerGlu() {
		return (ServerGlu) vGluDLServer.get(0);
	}
	
	
	/**
	    * This method extracts the input paramters required to make a standard SODA call.
	    * 
	    * @param activeDatalinkSource
	    * @param paramRange 
	    * @return 
	    */
	   	public void setCompleteSODAForm(Source activeDatalinkSource, Hashtable<String,String> paramDescription, Hashtable<String,String> paramDataType, Hashtable<String,String> paramValue, HashMap<String, String[]> paramRange) {
	   		if (activeDatalinkSource == null) return;
		
			//POS
	   		setSODATargetParameter(paramDescription, paramDataType, paramValue);
			
	   		//others
	   		setSODAFormTimeBandPol(activeDatalinkSource, paramDescription, paramDataType, paramValue, paramRange);

		}
	   	
	   	public void setSODAFormTimeBandPol(Source activeDatalinkSource, Hashtable<String,String> paramDescription, Hashtable<String,String> paramDataType, Hashtable<String,String> paramValue, HashMap<String, String[]> paramRange) {
	   		// time
	   		setSODAFormParam(activeDatalinkSource, String.valueOf(SODA_TIMEINDEX), paramDescription, paramDataType, DateTimeMJD , paramValue, SETFORMVALUES, paramRange,
					TIME, activeDatalinkSource.findColumn(T_MIN), activeDatalinkSource.findColumn(T_MAX));

			// band
			setSODAFormParam(activeDatalinkSource, String.valueOf(SODA_BANDINDEX), paramDescription, paramDataType, BandSODAForm, paramValue, SETFORMVALUES, paramRange, 
					BAND, activeDatalinkSource.findColumn(EM_MIN), activeDatalinkSource.findColumn(EM_MAX));

			// pol
			int polStatesIndex = activeDatalinkSource.findColumn(POL_STATES);
			String pol_states = null;
			if (polStatesIndex!=-1) {
				pol_states = activeDatalinkSource.getValue(polStatesIndex);
			}
			if (pol_states==null || pol_states.isEmpty()) {
				pol_states = POL_DEFAULT_VALUES;
			}
			setSODAFormParam(activeDatalinkSource, String.valueOf(SODA_POLINDEX), paramDescription, paramDataType, SODAPOL_DATATYPE , paramValue, pol_states, paramRange, 
					POL, activeDatalinkSource.findColumn(POL_STATES), -1);
		}
	   	
	   	public void setSODATargetParameter(Hashtable<String,String> paramDescription, Hashtable<String,String> paramDataType, Hashtable<String,String> paramValue) {
			
			//POS
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
	   	 */
		public static void setSODAFormParam(Source activeDatalinkSource, String paramFormIndex,
				Hashtable<String, String> paramDescription, Hashtable<String, String> paramDataType,
				String paramDataTypeDefault, Hashtable<String, String> paramValue, String paramDefaultValue,
				HashMap<String, String[]> paramRange, String paramName, int lowerLimitIndex, int upperLimitIndex) {
			Field[] field = activeDatalinkSource.leg.field;
			paramDescription.put(paramFormIndex, paramName);
			  
			if (paramDataTypeDefault == null) {
				if (lowerLimitIndex!=-1) {
					paramDataType.put(paramFormIndex, field[lowerLimitIndex].datatype);
				}
			} else {
				paramDataType.put(paramFormIndex, paramDataTypeDefault);
			}
			
			if (upperLimitIndex != -1) {
				String lowerLimit = activeDatalinkSource.getValue(lowerLimitIndex);
				String upperLimit =  activeDatalinkSource.getValue(upperLimitIndex);
				
				String[] anArray = new String[NUMBEROFOPTIONS];
				anArray[0] = lowerLimit;
				anArray[1] = upperLimit;
				paramRange.put(paramFormIndex, anArray);
				
				if (lowerLimit!=null && !lowerLimit.isEmpty() && upperLimit!=null && !upperLimit.isEmpty()) {
					if (paramDefaultValue.equalsIgnoreCase(SETFORMVALUES)) {
						String delimiter = SPACESTRING;
						if (paramName.equalsIgnoreCase(TIME)) {
							delimiter = COMMA_CHAR;
						}
						StringBuffer paramValueDisplay = new StringBuffer(lowerLimit).append(delimiter).append(upperLimit);
						paramValue.put(paramFormIndex, paramValueDisplay.toString());
					}
				} 
				/*paramHint.put(paramFormIndex, getSODAParamHint(activeDatalinkSource.getValue(lowerLimit),
						activeDatalinkSource.getValue(upperLimit))+ "\n" + hintMessage);*/
			} else if (!paramDefaultValue.equalsIgnoreCase(SETFORMVALUES)) {
				if (paramDefaultValue.contains("/")) {
					paramValue.put(paramFormIndex, paramDefaultValue.replaceAll("/", "\t"));
					String [] paramDefaultValues= paramDefaultValue.split("/");
					String[] anArray = Arrays.copyOf(paramDefaultValues, NUMBEROFOPTIONS);
					paramRange.put(paramFormIndex, anArray);
				} else {
					paramValue.put(paramFormIndex, paramDefaultValue);
				}
				
			}
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
	
	public void serverDataLinks(String actionName, String description, String verboseDescr,
	         String aladinMenu, String aladinMenuNumber,String aladinLabel,
	         String aladinLabelPlane, String docUser, Hashtable paramDescription1,
	         Hashtable paramDataType1, Hashtable paramValue1,
	         HashMap<String, String[]> paramRange2, String resultDataType, String institute, Vector aladinFilter1,
	         String aladinLogo,String dir,String system,StringBuffer record,String aladinProtocol) {
	   int i;
	   if( paramDescription1 == null ) return;
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
					institute, aladinFilter, aladinLogo, dir, system, record, aladinProtocol, null, null, null);
			vGluDLServer.clear();
			vGluDLServer.addElement(g);
		}
  }
	
	void reload(boolean clearBefore, Vector server) {
		ServerDialog oldDialog = aladin.additionalServiceDialog;

		Point p = null;
		Dimension d = null;
		try {
			if (oldDialog != null && oldDialog.isVisible()) {
				p = aladin.additionalServiceDialog.getLocationOnScreen();
				d = aladin.additionalServiceDialog.getSize();
			}
		} catch (Exception e) {
			e.printStackTrace();
			p = null;
		}
		// aladin.gluSkyReload();

		aladin.additionalServiceDialog = new ServerDialog(aladin, server);
		// aladin.additionalServiceDialog.setCurrent(aladin.additionalServiceDialog.getLastGluServerIndice());

		if (p != null) {
			aladin.additionalServiceDialog.flagSetPos = true;
			aladin.additionalServiceDialog.setLocation(p);
			aladin.additionalServiceDialog.setSize(d);
		}

		aladin.additionalServiceDialog.showNow();
		if ((oldDialog != null && oldDialog.isVisible())) {
			oldDialog.dispose();
		}
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
	public String getParamValue(SavotParam param, SimpleData selectedDatalink) {
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
