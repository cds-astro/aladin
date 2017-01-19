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
