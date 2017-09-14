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
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reader for an xhtml stream.
 * We are only interested reading "example"
 * For now, closing tag of parent tag with vocab is not detected, only the starting of this tag is detected.
 * 
 * <div vocab="ivo://ivoa.net/std/DALI-examples"> 
 * 		<div typeof="example"> 
 * 			<div property="name"> </div>
 * 			<div property="query"> </div>
 * 		</div>
 * </div>
 */
public class DaliExamplesReader2{
	
	/*public static void main(String[] args){
	    try {
	    	SAXParserFactory parserFactor = SAXParserFactory.newInstance();
	    	SAXParser parser = parserFactor.newSAXParser();
		    DaliExamplesReader2 handler = new DaliExamplesReader2();
		    URL examplesUrl = new URL("http://130.79.129.54:8080/view-source_gaia.ari.uni-heidelberg.de_tap_examples.xhtml");
//		    URL examplesUrl = new URL("http://gaia.ari.uni-heidelberg.de/tap/examples");
//		    new URL("http://130.79.129.54:8080/simbadExamples.xhtml"); //works
			parser.parse(examplesUrl.openStream(), handler);
			for (Object string : handler.examples.keySet().toArray()) {
				System.out.println(string);
				System.err.println(handler.examples.get(string));
				System.out.println("-----------\n\n");
			}
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
	
	//tag flags
	private boolean inExamplesTag = false;
	private boolean inExampleTag = false;
	private boolean inNameTag = false;
	private boolean inQueryTag = false;
	private String exampleTag = null;
	private String nameTag = null;
	private String queryTag = null;
	
	private String exampleLabel = null;
	
	private boolean wait = false; 
	private Stack tagStack;	
	private StringBuffer content = null;
	
	//data
	private Map<String, String> examples;
	
	public void parse(){
		// TODO Auto-generated method stub
//		super.startElement(uri, localName, qName, attributes);
		while (en.hasMoreElements()) {
			type type = (type) en.nextElement();
			
		}
		if (attributes.getValue("vocab") != null && attributes.getValue("vocab").equals("ivo://ivoa.net/std/DALI-examples")) {
			inExamplesTag = true;
			examples = new HashMap<String, String>();
		} else if (inExamplesTag) {
			if (inExampleTag) {
				tagStack.push(qName);
				if (attributes.getValue("property") != null) {
					String propertyValue = (String) attributes.getValue("property");
					if (propertyValue.equals("name")) {
						inNameTag = true;
						nameTag = qName;
					} else if (propertyValue.equals("query")) {
						inQueryTag = true;
						queryTag = qName;
					}
				}
			} else if (attributes.getValue("typeof") != null && attributes.getValue("typeof").equals("example")) {
				inExampleTag = true;
				tagStack = new Stack<String>();
				tagStack.push(qName);
				exampleTag = qName;
				content = new StringBuffer();
			}
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		// TODO Auto-generated method stub
//		super.endElement(uri, localName, qName);
		if (inExamplesTag && tagHierarchy == 0) {
			inExamplesTag = false;
		} else if (inExampleTag && tagHierarchy == 1) {
			inExampleTag = false;
		} else if (inName && tagHierarchy == 2) {
			inName = false;
		} else if (inQuery && tagHierarchy == 3) {
			if (exampleLabel == null) {
				exampleLabel = "Service provided example ";
				exampleLabel = generateQueryName(exampleLabel);
			}
			examples.put(exampleLabel, content.toString());
			content = new StringBuffer();
			inQuery = false;
		}
		
		if (inExampleTag) {
			String tagLeft = (String) tagStack.pop();
			if (tagStack.isEmpty() && tagLeft.equals(exampleTag)) {
				inExampleTag = false;
			} else if (inNameTag && tagLeft.equals(nameTag)) {
				inNameTag = false;
			} else if (inQueryTag && tagLeft.equals(queryTag)) {
				if (exampleLabel == null) {
					exampleLabel = "Service provided example ";
					exampleLabel = generateQueryName(exampleLabel);
				}
				examples.put(exampleLabel, content.toString());
				inQueryTag = false;
				resetInExampleFlags();
			}
		}
		
		
		if (inExamplesTag && inExampleTag && inQueryTag && qName.equals(queryTag)) {
			if (exampleLabel == null) {
				exampleLabel = "Service provided example ";
				exampleLabel = generateQueryName(exampleLabel);
			}
			examples.put(exampleLabel, content.toString());
			inQueryTag = false;
			resetInExampleFlags();
		}
	}
	

	@Override
	public void characters(char[] ch, int start, int length) {
		// TODO Auto-generated method stub
		String data = new String(ch, start, length);
		if (inExamplesTag) {
			if (inExampleTag) {
				if (inNameTag) {
					exampleLabel = data;
					inNameTag = false;
				} else if (inQueryTag) {
					content.append(data);
//					resetFlags();//this is because we assume this reading order. for now we do not look for anything other than this order
				}
			}
		}
	}
	
	*//**
	 * Method to generate a unique <i>upload</i> table name
	 * 
	 * @param uploadFrame
	 * @return
	 *//*
	public String generateQueryName(String prefix) {
		String uploadTableName = prefix + new Random().nextInt(Integer.SIZE - 1);
		if (!examples.containsKey(uploadTableName)) {
			return uploadTableName;
		}
		return generateQueryName(prefix);
	}
	
	*//**
	 * Method to reset all Flags
	 *//*
	private void resetAllFlags() {
		inExamplesTag = false;
		resetInExampleFlags();
	}
	
	*//**
	 * Method to resetFlags
	 *//*
	private void resetInExampleFlags() {
		inExampleTag = false;
		inNameTag = false;
		inQueryTag = false;
		exampleLabel = null;
		content = new StringBuffer();
	}

	public Map<String, String> getExamples() {
		if (examples != null && examples.isEmpty()) {
			examples = null;
		}
		return examples;
	}*/

}
