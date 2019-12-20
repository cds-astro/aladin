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

/**
 * 
 */
package cds.tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import cds.aladin.Aladin;

/**
 * Property file reader
 * @author chaitra
 *
 */
public class ConfigurationReader {

	private static ConfigurationReader instance = null;

	private String viewConfigurationFileName = "viewConfiguration.properties";
	private Properties properties = null;

	public static synchronized ConfigurationReader getInstance() {
		if (instance == null) {
			instance = new ConfigurationReader();
		}
		return instance;
	}

	private ConfigurationReader() {
		InputStream inputStream = null;
		try {
			properties = new Properties();
			inputStream = getClass().getClassLoader().getResourceAsStream(viewConfigurationFileName);

			if (inputStream != null) {
				properties.load(inputStream);
			}

		} catch (Exception e) {
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException fileCloseException) {
					if (Aladin.levelTrace >= 3)
						fileCloseException.printStackTrace();
				}
			}
		}
	}

	public String getPropertyValue(String key){
		String value = null;
		InputStream inputStream = null;
		try {
			Properties prop = new Properties();

			inputStream = getClass().getClassLoader().getResourceAsStream(viewConfigurationFileName);

			if (inputStream != null) {
				prop.load(inputStream);
			}
			value = prop.getProperty(key);

		} catch (IOException e) {
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (IOException e2) {
				if (Aladin.levelTrace >= 3)
					e2.printStackTrace();
			}
		}
		return value;
	}
}
