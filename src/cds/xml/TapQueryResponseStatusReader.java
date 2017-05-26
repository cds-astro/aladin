package cds.xml;

import static cds.aladin.Constants.STR_QUERY_STATUS;
import static cds.aladin.Constants.STR_RESULTS;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import cds.aladin.MyInputStream;

//Based on tap spec http://www.ivoa.net/documents/TAP/20100327/REC-TAP-1.0.pdf. Section 2.9
public class TapQueryResponseStatusReader implements XMLConsumer {
	//tag flags
	private boolean inVotable = false;
	private boolean inResultsResource = false;
	private boolean inInfo_query_status = false;
	
	//data
	private String query_status_value = null;
	private String query_status_message = null;
	
	public boolean load(InputStream in) throws Exception {
		XMLParser xmlParser = new XMLParser(this);
		boolean result = false;
		MyInputStream inputStream = null;
		
		resetFlags();
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
	private void resetFlags() {
		query_status_value = null;
		query_status_message = null;
		inVotable = false;
		inResultsResource = false;
		inInfo_query_status = false;
	}
	
	@Override
	public void startElement(String name, Hashtable atts) {
		// TODO Auto-generated method stub
		if (name.equals("VOTABLE")) {
			inVotable = true;
		} else if (inVotable && name.equals("RESOURCE")) {
			if (atts.containsKey("type")) {
				String type = (String)atts.get("type");
				if (type.equalsIgnoreCase(STR_RESULTS)) {
					inResultsResource = true;
				}
			}
		} else if (inResultsResource) {
			if (name.equals("INFO")) {
				String att = (String)atts.get("name");
				if (att.equalsIgnoreCase(STR_QUERY_STATUS)) {
					inInfo_query_status = true;
					query_status_value = (String)atts.get("value");
				}
			}
		}
	}

	@Override
	public void endElement(String name) {
		// TODO Auto-generated method stub
		if (inVotable && name.equals("VOTABLE")) {
			inVotable = false;
		} else if (inResultsResource && name.equals("RESOURCE")) {
			inResultsResource = false;
		} else if (inResultsResource && name.equals("INFO")) {
			inInfo_query_status = false;// anyway will reset
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws Exception {
		// TODO Auto-generated method stub
		String data = new String(ch, start, length);
		if (inVotable) {
			if (inResultsResource && inInfo_query_status) {
				query_status_message = data;
			}
		} 
	}

	public String getQuery_status_value() {
		return query_status_value;
	}

	public void setQuery_status_value(String query_status_value) {
		this.query_status_value = query_status_value;
	}

	public String getQuery_status_message() {
		return query_status_message;
	}

	public void setQuery_status_message(String query_status_message) {
		this.query_status_message = query_status_message;
	}

}
