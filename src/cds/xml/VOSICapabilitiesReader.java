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

package cds.xml;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.aladin.TapManager;

public class VOSICapabilitiesReader implements XMLConsumer {
	//tag flags
	private boolean inTableAccessCapabilityTag = false;
	private int readHardUploadLimit = -1;
	
	//data
	private boolean uploadAllowed = false;
	private long uploadHardLimit = -1;
	private boolean tabledataFormatAllowed = false;
	
	public boolean load(URL capabilitiesUrl) {
		XMLParser xmlParser = new XMLParser(this);
		boolean result = false;
		MyInputStream inputStream = null;
		resetFlags();
		try {
			long startTime = TapManager.getTimeToLog();
			inputStream = new MyInputStream(capabilitiesUrl.openStream());
			long time = TapManager.getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println("getTapCapabilities got inputstream: "+time+" total time taken: "+(time - startTime));
			startTime = TapManager.getTimeToLog();
			result = xmlParser.parse(inputStream);
			time = TapManager.getTimeToLog();
			if (Aladin.levelTrace >= 4) System.out.println("getTapCapabilities parsed: "+time+" total time taken: "+(time - startTime));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Method to resetFlags
	 */
	private void resetFlags() {
		inTableAccessCapabilityTag = false;
		readHardUploadLimit = -1;
	}
	
	private boolean inOutPutFormat=false;
	private boolean inOutputFormatMime=false;
	
//	<outputFormat ivo-id="ivo://ivoa.net/std/TAPRegExt#output-votable-td">
//     <mime>application/x-votable+xml;serialization=TABLEDATA</mime>
//     <alias>text/xml</alias><alias>votable/td</alias><alias>votabletd</alias>
//  </outputFormat>
	
	@Override
	public void startElement(String name, Hashtable atts) {
	   
	  if( name.equalsIgnoreCase("outputFormat") ) {
	     inOutPutFormat=true;
	     if( atts.containsKey("ivo-id") && atts.get("ivo-id").equals("ivo://ivoa.net/std/TAPRegExt#output-votable-td") ) {
	        tabledataFormatAllowed=true;
//	        System.err.println("BINGO - ivo-id TABLEDATA ****************");
	     }
	  } 
	  else if( inOutPutFormat && name.equalsIgnoreCase("mime") ) inOutputFormatMime=true;
      else  if (name.equals("capability")) {
			if (atts.containsKey("standardid") && atts.get("standardid").equals("ivo://ivoa.net/std/TAP")
					&& atts.containsKey("xsi:type") && atts.get("xsi:type").equals("tr:TableAccess")) {
				inTableAccessCapabilityTag = true;
		   }
			
		} else if (inTableAccessCapabilityTag) {
			if (name.equals("uploadMethod")) {
				uploadAllowed = true;
			} else if (name.equals("uploadLimit")) {
				readHardUploadLimit++;
			} else if (name.equals("hard") && readHardUploadLimit == 0) {
				readHardUploadLimit++;
			}
			
		}  

	}

	@Override
	public void endElement(String name) {
		if( name.equals("outputFormat") ) inOutPutFormat=false;
		else if( name.equals("mime") ) inOutputFormatMime=false;
		else if (inTableAccessCapabilityTag && name.equals("capability")) {
			inTableAccessCapabilityTag = false;
		} else if (name.equals("uploadLimit")) {
			readHardUploadLimit = -1;
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws Exception {
		// TODO Auto-generated method stub
		String data = new String(ch, start, length);
		if (inTableAccessCapabilityTag && readHardUploadLimit == 1) {
			uploadHardLimit = Long.parseLong(data);
		} else if( inOutputFormatMime ) {
		   if( data.equalsIgnoreCase("application/x-votable+xml;serialization=TABLEDATA") ) {
		      tabledataFormatAllowed=true;
//		      System.err.println("BINGO - TABLEDATA ****************");
		   }
		}
	}

	public boolean isUploadAllowed() {
		return uploadAllowed;
	}

	public void setUploadAllowed(boolean uploadAllowed) {
		this.uploadAllowed = uploadAllowed;
	}

	public long getUploadHardLimit() {
		return uploadHardLimit;
	}

	public void setUploadHardLimit(long uploadHardLimit) {
		this.uploadHardLimit = uploadHardLimit;
	}
	
	public boolean tabledataFormatAllowed() { return tabledataFormatAllowed; }

}
