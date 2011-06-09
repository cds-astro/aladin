// Copyright 2010 - UDS/CNRS
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

import cds.xml.Field;


/** Un objet représentant les colonnes à conserver pour le cross-match et les UCDs associés */

public class ColFilter {

	Field[] fieldTab1;
	Field[] fieldTab2;
	
	String prefix1, prefix2;
	String suffix1, suffix2;
	
	
	protected ColFilter(Field[] fieldTab1, Field[] fieldTab2, String prefix1, String prefix2, String suffix1, String suffix2) {
		this.fieldTab1 = fieldTab1;
		this.fieldTab2 = fieldTab2;
		this.prefix1 = prefix1;
		this.prefix2 = prefix2;
		this.suffix1 = suffix1;
		this.suffix2 = suffix2;	
	}
	
	protected boolean inTab1(String field) {
		return inTab(field, fieldTab1);
	}
	
	protected boolean inTab2(String field) {
		return inTab(field, fieldTab2);
	}
	
	private boolean inTab(String field, Field[] fields) {
		if( fields==null ) return false;
		for( int i=0; i<fields.length; i++ ) {
			if( fields[i].name.equals(field) )  return true;
		}
		return false;
	}
	
	protected String getUcdTab1(String field) {
		return getUcdTab(field, fieldTab1);
	}
	
	protected String getUcdTab2(String field) {
		return getUcdTab(field, fieldTab2);
	}
	
	protected String getUcdTab(String field, Field[] fields) {
		if( fields==null ) return null;
		for( int i=0; i<fields.length; i++ ) {
			if( fields[i].name.equals(field) )  return fields[i].ucd;
		}
		return null;
	}
	
}