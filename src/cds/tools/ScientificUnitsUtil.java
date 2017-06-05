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
package cds.tools;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.aladin.Aladin;
import cds.astro.Unit;

/**
 * This class is to implement certain scientific formulae
 * 
 * @author chaitra
 *
 */
public class ScientificUnitsUtil {

	final static double light_speed_m_per_second = 2.998e8; //speed of light in m/s
	final static double hc_wavelength_to_ev_constant = 1.24E-6; // hc in eV-m; hc where h= 6.626 × 10 -34 joules-s 
	final static String FREQUENCYUNIT_STRING= "Hz";
	final static String METERUNIT_STRING = "m";
	final static String eVUNIT_STRING = "eV";
	final static String EXPONENT_STRING = "E";
	final static String NUMBER_FORMATTER_STRING = "##0.#####E0";
	final static Map<String, String> metricPrefixes; 
	final static String endOfWordPrefix = "[a-zA-Z]$";
	
	static {
		metricPrefixes = new HashMap<String, String>();
		metricPrefixes.put("-24", "y");
		metricPrefixes.put("-21", "z");
		metricPrefixes.put("-18", "a");
		metricPrefixes.put("-15", "f");
		metricPrefixes.put("-12", "p");
		metricPrefixes.put("-9", "n");
		metricPrefixes.put("-6", "\u00B5");
		metricPrefixes.put("-3", "m");
		metricPrefixes.put("0", "");
		metricPrefixes.put("3", "k");
		metricPrefixes.put("6", "M");
		metricPrefixes.put("9", "G");
		metricPrefixes.put("12", "T");
		metricPrefixes.put("15", "P");
		metricPrefixes.put("18", "E");
		metricPrefixes.put("21", "Z");
		metricPrefixes.put("24", "Y");
	}
	
	/**
	 * Method to convert values to meter
	 * Recognises values with metric prefixes: y/z/a/f/p/n/u/m/k/M/G/T/P/E/Z/Y
	 * @param input
	 * @return valueConvertedToMeters
	 * @throws ParseException 
	 */
	public static double getUnitInMeters(String input) throws ParseException {
		double result;
		try {
			
			Pattern alphabetsPrefix = Pattern.compile(endOfWordPrefix);
			Matcher m = alphabetsPrefix.matcher(input);
			if (m.find()) {
				String s = m.group(0);
				Unit inputUnit = new Unit(s+"m");
				inputUnit.value = Double.parseDouble(input.replaceAll(endOfWordPrefix, ""));
				convertToMeters(inputUnit);
				result = inputUnit.value;
			} else {
				result = Double.parseDouble(input);
			}
			
		} catch (ParseException e) {
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
			throw e;
		}
		
		return result;
	}
	
	/**
	 * Convert input unit to meters
	 * @param input
	 */
	public static void convertToMeters(Unit input) {
		try {
			Unit unitInMeter = new Unit("m");
			input.convertTo(unitInMeter);
		} catch (ParseException e) {
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
		}
	}
	
	/**
	 * Method to convert wavelength (in meter) to frequency (Hz)
	 * @param wavelength in meters
	 * @return  {@link Unit} (in Hz)
	 */
	public static Unit convertMeter2Frequency(Double wavelength) {
		Unit frequency = null;
		try {
			Double frequencyValue = light_speed_m_per_second/wavelength;
			frequency= new Unit(FREQUENCYUNIT_STRING);
			frequency.value = frequencyValue; 
		} catch (ParseException e) {
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
		}
		return frequency;
	}
	
	/**
	 * Method to convert wavelength (in meter) to energy (in eV)
	 * @param wavelength in meters
	 * @return  {@link Unit} (in eV)
	 */
	public static Unit convertMeter2eV(Double wavelength) {
		Unit energy = null;
		try {
			Double energyValue = hc_wavelength_to_ev_constant/wavelength;
			energy= new Unit(eVUNIT_STRING);
			energy.value = energyValue;
		} catch (ParseException e) {
			if (Aladin.levelTrace >= 3)
				e.printStackTrace();
		}
		return energy;
	}
	
	/**
	 * Method to format numbers. If necessary
	 * the appropriate metric prefixes are added to it.
	 * 
	 * @param unitToProcess
	 * @return
	 */
	public static String prefixProcessing(Unit unitToProcess) {
		String unit = unitToProcess.getUnit();
		
		String metricPrefix = null;
		NumberFormat formatter = new DecimalFormat(NUMBER_FORMATTER_STRING);
		String valueInProcess = formatter.format(unitToProcess.value);
		StringBuffer displayString;
		
		if (valueInProcess.contains(EXPONENT_STRING)) {
			String numeral = valueInProcess.split(EXPONENT_STRING)[0];
			String basePower = valueInProcess.split(EXPONENT_STRING)[1];
			metricPrefix = metricPrefixes.get(basePower);
			displayString = new StringBuffer(numeral).append(metricPrefix).append(unit);
		} else {
			displayString = new StringBuffer(valueInProcess).append(unit);
		}
		return displayString.toString();
	}

	
}
