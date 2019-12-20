// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

package cds.xml;

import static cds.aladin.Constants.EMPTYSTRING;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;

import cds.aladin.MyInputStream;
import cds.aladin.UWSJob;

public class UWSReader implements XMLConsumer {
	//tag flags
	private boolean inUWSJobTag = false;
	private boolean inParameters = false;
	private boolean inResults = false;
	private boolean inErrorSummary = false;
	private boolean inJobInfo = false;
	private String tag;
	
	//data
	private UWSJob uwsJob;
	private String paramId;
	
	public boolean load(InputStream in, UWSJob job) throws Exception {
		this.uwsJob = job;
		XMLParser xmlParser = new XMLParser(this);
		boolean result = false;
		MyInputStream inputStream = null;
		
		resetFlags(this.uwsJob);
		try {
			inputStream = new MyInputStream(in);
			result = xmlParser.parse(inputStream);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
		return result;
	}
	
	/**
	 * Method to resetFlags
	 */
	private void resetFlags(UWSJob job) {
		inUWSJobTag = false;
		job.setParameters(new HashMap<String,String>());
		job.setResults(new HashMap<String,String>());
	}
	
	@Override
	public void startElement(String name, Hashtable atts) {
		// TODO Auto-generated method stub
		if (name.equals("job")) {
			inUWSJobTag = true;
			tag = EMPTYSTRING;
			if (atts.containsKey("version")) {
				uwsJob.setVersion(Double.parseDouble((String) atts.get("version")));
			}
		} else if (inUWSJobTag) {
			if (name.equals("jobId")) {
				tag = "jobId";
			} else if (name.equals("runId")) {
				tag = "runId";
			} else if (name.equals("ownerId")) {
				tag = "ownerId";
			} else if (name.equals("phase")) {
				tag = "phase";
			} else if (name.equals("quote")) {
				tag = "quote";
			} else if (name.equals("startTime")) {
				tag = "startTime";
			} else if (name.equals("endTime")) {
				tag = "endTime";
			} else if (name.equals("creationTime")) {
				tag = "creationTime";
			} else if (name.equals("executionDuration")) {
				tag = "executionDuration";
			} else if (name.equals("destruction")) {
				tag = "destruction";
			} else if (name.equals("parameters")) {
				inParameters = true;
				tag = "parameters";
			} else if (inParameters && name.equals("parameter")) {
				if (atts.containsKey("id")) {
					paramId = (String)atts.get("id");
				}
				tag = "parameter";
			} else if (name.equals("results")) {
				inResults = true;
				tag = "results";
			} else if (inResults && name.equals("result")) {
				tag = "result";
				if (atts != null && atts.size() > 0) {
					String idForResultsMap = null;
					if (atts.containsKey("id")) {
						idForResultsMap = (String)atts.get("id");
					} else {
						idForResultsMap = "result"+uwsJob.getResults().size();
					}
					
					if (atts.containsKey("xlink:href")) {
						uwsJob.getResults().put(idForResultsMap, (String)atts.get("xlink:href"));
					} else if (atts.containsKey("href")) {
						uwsJob.getResults().put(idForResultsMap, (String)atts.get("href"));
					} /*else if (atts.containsKey("ns2:href")) {
						uwsJob.getResults().put(id, (String)atts.get("ns2:href"));
					} */else if (atts.size() > 0) {
						Set potentialHrefs= atts.keySet();
						String potentialHref = null;
						for (Object object : potentialHrefs) {
							potentialHref = String.valueOf(object);
							if (potentialHref.contains("href") || potentialHref.contains("HREF")) {
								break;
							}
						}
						if (potentialHref!=null && atts.get(potentialHref)!=null) {
							uwsJob.getResults().put(idForResultsMap, (String)atts.get(potentialHref));
						}
					}
				}
			}  else if (name.equals("errorSummary")) {
				tag = "errorSummary";
				inErrorSummary = true;
				if (atts.containsKey("type")) {//will contain, both type and hasDetails are required attris
					uwsJob.setErrorType((String)atts.get("type"));
				}
				if (atts.containsKey("hasDetail")) {
					String hasDetail = (String) atts.get("hasDetail");
					if (hasDetail.equalsIgnoreCase("true")) {
						uwsJob.setHasErrorDetail(true);
					}
				}
			} else if (inErrorSummary && name.equals("message")) {
				tag = "message";
			} else if (name.equals("jobInfo")) {
				tag = "jobInfo";
				inJobInfo = true;
			}
			
		}
	}

	@Override
	public void endElement(String name) {
		// TODO Auto-generated method stub
		if (inUWSJobTag && name.equals("job")) {
			inUWSJobTag = false;
		} else if (inParameters && name.equals("parameters")) {
			inParameters = false;
			tag = EMPTYSTRING;
		} else if (inResults && name.equals("results")) {
			inResults = false;
			tag = EMPTYSTRING;
		} else if (inJobInfo && name.equals("jobInfo")) {
			inJobInfo = false;
			tag = EMPTYSTRING;
		} else if (inUWSJobTag) {
			tag = EMPTYSTRING;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws Exception {
		// TODO Auto-generated method stub
		String data = new String(ch, start, length);
		if (inUWSJobTag) {
			if (inParameters && tag.equals("parameter")) {
				uwsJob.getParameters().put(paramId, data);
			} else if (inErrorSummary && tag.equals("message")) {
				uwsJob.setErrorMessage(data);
			} else if (inJobInfo) {
				uwsJob.getJobInfoXml().append(data);
			} else {
				setOthers(tag, data);
			}
			
		} 
	}
	
	public void setOthers(String tag, String data) {
	        if( tag.equals("jobId") )        uwsJob.setJobId(data);
	   else if( tag.equals("runId") )        uwsJob.setRunId(data);
	   else if( tag.equals("ownerId") )      uwsJob.setOwnerId(data);
	   else if( tag.equals("phase") )        uwsJob.setCurrentPhase(data);
	   else if( tag.equals("quote") )        uwsJob.setQuote(data);
	   else if( tag.equals("startTime") )    uwsJob.setStartTime(data);
	   else if( tag.equals("endTime") )      uwsJob.setEndTime(data);
	   else if( tag.equals("creationTime") ) uwsJob.setCreationTime(data);
	   else if( tag.equals("executionDuration") ) uwsJob.setExecutionDuration(Long.parseLong(data));
	   else if( tag.equals("destruction") )  uwsJob.setDestructionTime(data);
	   
	   // PAS COMPATIBLE Java 1.7 - PF. Jan 2017
//		switch (tag) {
//		case "jobId":
//			uwsJob.setJobId(data);
//			break;
//		case "runId":
//			uwsJob.setRunId(data);
//			break;
//		case "ownerId":
//			uwsJob.setOwnerId(data);
//			break;
//		case "phase":
//			uwsJob.setCurrentPhase(data);
//			break;
//		case "quote":
//			uwsJob.setQuote(data);
//			break;
//		case "startTime":
//			uwsJob.setStartTime(data);
//			break;
//		case "endTime":
//			uwsJob.setEndTime(data);
//			break;
//		case "creationTime":
//			uwsJob.setCreationTime(data);
//			break;
//		case "executionDuration":
//			uwsJob.setExecutionDuration(Long.parseLong(data));
//			break;
//		case "destruction":
//			uwsJob.setDestructionTime(data);
//			break;
//		default:
//			break;
//		}
	}

	public boolean isInUWSJobTag() {
		return inUWSJobTag;
	}

	public void setInUWSJobTag(boolean inUWSJobTag) {
		this.inUWSJobTag = inUWSJobTag;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

}
