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

package cds.xml;

import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;

import cds.aladin.MyInputStream;

public class VOSICapabilitiesReader implements XMLConsumer {
	//tag flags
	private boolean inTableAccessCapabilityTag = false;
	private int readHardUploadLimit = -1;
	
	//data
	private boolean uploadAllowed = false;
	private long uploadHardLimit = -1;
	
	public boolean load(URL capabilitiesUrl) {
		XMLParser xmlParser = new XMLParser(this);
		boolean result = false;
		MyInputStream inputStream = null;
		
		resetFlags();
		try {
			inputStream = new MyInputStream(capabilitiesUrl.openStream());
			result = xmlParser.parse(inputStream);
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
	
	@Override
	public void startElement(String name, Hashtable atts) {
		// TODO Auto-generated method stub
		if (name.equals("capability")) {
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
		// TODO Auto-generated method stub
		if (inTableAccessCapabilityTag && name.equals("capability")) {
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

}
