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

/**
 * 
 */
package cds.aladin;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

/**
 * Reader for Standard Forms
 * @author chaitra
 *
 */
public class StandardFormsReader {
	
	private static StandardFormsReader instance = null;
	
	private Hashtable<String,String> stdServerForms = null;
	
	public static synchronized StandardFormsReader getInstance() {
		if (instance == null) {
			instance = new StandardFormsReader();
		}
		return instance;
	}
	
	private StandardFormsReader() {
		DataInputStream inputStream = null;
		try {
			stdServerForms = new Hashtable<String,String>();
			inputStream = new DataInputStream(getClass().getResourceAsStream("/" + Constants.STANDARDGLUs));
			String fileLine;
			String name;
			String value;
			String stdForm = null;
			StringBuilder record = new StringBuilder();
			
			while ((fileLine = inputStream.readLine()) != null) {
				if (fileLine.equals("") || fileLine.charAt(0) == '#') continue;
				if ((name = Glu.getName(fileLine)) == null) continue;
				if ((value = Glu.getValue(fileLine, inputStream)) == null) continue;
				if( name.equals(Constants.STDFORM_IDENTIFIER)) {
					if (record.length()!=0) {
						setStdServerForms(stdForm, record);
					}
					stdForm= value;
	            }
				record.append(fileLine).append("\n");
			}
			
			if (stdForm!=null) {// for the last form 
				setStdServerForms(stdForm, record);
			}
		} catch (Exception e) {
			if( Aladin.levelTrace>=3 ) {
	            System.err.println("Standardforms.dic not loaded error: " + e);
	            e.printStackTrace();
	         }
			Aladin.warning("Standardforms.dic not loaded !", 1);
		} finally {
			if (inputStream!=null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					if( Aladin.levelTrace>=3 ) {
			            System.err.println("error when closing Standardforms.dic: " + e);
			            e.printStackTrace();
			         }
				}
			}
		}
	}

	public Hashtable<String, String> getStdServerForms() {
		return stdServerForms;
	}
	
	public void setStdServerForms(String formName, StringBuilder record) {
		this.stdServerForms.put(formName, record.toString());
		record.setLength(0);
	}

	public void setStdServerForms(Hashtable<String, String> stdServerForms) {
		this.stdServerForms = stdServerForms;
	}

}
