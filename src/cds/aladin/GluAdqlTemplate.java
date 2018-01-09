// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import static cds.aladin.Constants.ALL;
import static cds.aladin.Constants.DISTINCT;
import static cds.aladin.Constants.DOT_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.FROM;
import static cds.aladin.Constants.GLU_FROM;
import static cds.aladin.Constants.GLU_SELECT;
import static cds.aladin.Constants.GLU_WHERE;
import static cds.aladin.Constants.TOP;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import adql.query.ADQLQuery;/**
 * Helper class for mapping of tables and ADQL query elements
 *
 */
public class GluAdqlTemplate extends ADQLQuery {
	
	private String[] gluTop = null; //for tap glu adql top
	private String[] gluAllOrDistinct = null; //for tap glu adql AllOrDistinct
	private String[] gluTable = null; //for tap glu adql table
	private Hashtable<String,String> gluSelect = null; //for tap glu adql select elements
	private Hashtable<String,String> gluFrom = null; //for tap glu adql from elements
	private Hashtable<String,String> gluWhere = null; //for tap glu adql where elements
	
	public GluAdqlTemplate() {
		// TODO Auto-generated constructor stub
		this.gluSelect = new Hashtable<String,String>();
		this.gluFrom = new Hashtable<String,String>();
		this.gluWhere = new Hashtable<String,String>();
	}
	
	public GluAdqlTemplate(Hashtable<String,String> gluSelect, Hashtable<String,String> gluFrom, Hashtable<String,String> gluWhere) {
		// TODO Auto-generated constructor stub
		this.gluSelect = gluSelect;
		this.gluFrom = gluFrom;
		this.gluWhere = gluWhere;
	}
	
	public GluAdqlTemplate(Hashtable<String,String> gluSelect, Hashtable<String,String> gluFrom, Hashtable<String,String> gluWhere, Hashtable<String, String> adqlFunc, Hashtable<String, String> adqlFuncParams) {
		// TODO Auto-generated constructor stub
		this(gluSelect, gluFrom, gluWhere);
	}
	
	public String getQueryClause(String key) {
		String result = EMPTYSTRING;
		if (this.gluFrom.containsKey(key)) {
			result = GLU_FROM;
		} else if (this.gluSelect.containsKey(key)) {
			result = GLU_SELECT;
		} else if (this.gluWhere.containsKey(key)) {
			result = GLU_WHERE;
		}
		return result;
	}
	
	/**
	 * Method sets TOP glu param is that is specified by the user
	 * @param v
	 * @param gluQuery
	 */
	public void setGluQueryTop(Vector v, StringBuffer gluQuery) {
		setSingleGluQueryParam(this.gluTop, v, gluQuery, null);
	}
	
	/**
	 * Method sets the ALL/DISTINCT glu param if that is specified by the user
	 * @param v
	 * @param gluQuery
	 */
	public void setGluQueryAllOrDistinct(Vector v, StringBuffer gluQuery) {
		setSingleGluQueryParam(this.gluAllOrDistinct, v, gluQuery, null);
	}
	
	/**
	 * Method sets the glu main table selected by the user
	 * @param v
	 * @param gluQuery
	 */
	public void setGluQueryColumns(Vector v, StringBuffer gluQuery) {
		if (!this.gluSelect.isEmpty() && this.gluSelect.size()>=1) {
			String colGluKey = this.gluSelect.keys().nextElement();
			setSingleGluQueryParam(colGluKey, this.gluSelect.get(colGluKey), v, gluQuery, null, " * ");
		} else {
			gluQuery.append(" * ");
		}
	}
	
	/**
	 * Method sets the glu main table selected by the user
	 * @param v
	 * @param gluQuery
	 */
	public void setGluQueryTable(Vector v, StringBuffer gluQuery) {
		setSingleGluQueryParam(this.gluTable, v, gluQuery, FROM);
	}
	
	public void setSingleGluQueryParam(String[] gluParam, Vector v, StringBuffer gluQuery, String param) {
		if (gluParam!=null && gluParam.length>1) {
			setSingleGluQueryParam(gluParam[0], gluParam[1], v, gluQuery, param, EMPTYSTRING);
		}
	}
	
	/**
	 * Method to set query from glu params if the user input for the field is valid
	 * @param gluKey
	 * @param gluParam
	 * @param v
	 * @param gluQuery
	 * @param param
	 * @param defaultAppend
	 */
	public void setSingleGluQueryParam(String gluKey, String gluParam, Vector v, StringBuffer gluQuery, String param, String defaultAppend) {
		boolean notSetFlag = true;
		if (gluParam!=null && gluKey!=null) {
			String value = null;
			  int k = Integer.valueOf(gluKey)-1;
			  if (v.size()>k) {
				  value = (String) v.get(Integer.valueOf(gluKey)-1);
				  if (value!=null && !value.isEmpty() && !value.equals("NaN")) {
					  if (param!=null) {
						  gluQuery.append(param).append(" ");
					  }
					  gluQuery.append(gluParam).append(" ");
					  notSetFlag = false;
	    		  }
			  }
		}
		
		if (notSetFlag) {
			gluQuery.append(defaultAppend);
		}
	}
	
	/**
	 * Method sets the glu where constaints set by the user
	 * @param v
	 * @param gluQuery
	 * @param currentSelectedTapTable 
	 * @param adqlFuncParams 
	 * @param adqlFunc 
	 */
	public void setGluQueryWheres(Vector v, StringBuffer gluQuery, String currentSelectedTapTable, Hashtable<String, String> adqlFunc, Hashtable<String, String> adqlFuncParams) {
		StringBuffer whereQuery = new StringBuffer();
		String value = null;
		String gluKey;
		boolean isfirst = true;
		Enumeration<String> keys = this.gluWhere.keys();
		while (keys.hasMoreElements()) {
			gluKey = keys.nextElement();
			int k = Integer.valueOf(gluKey) - 1;
			if (v.size() > k) {
				value = (String) v.get(Integer.valueOf(gluKey) - 1);
			}

			if (value != null && !value.isEmpty() && !value.equals("NaN")) {
				if (!isfirst) {
					whereQuery.append(" AND ");
				}
				if (adqlFunc!=null && adqlFuncParams!=null && adqlFunc.containsKey(this.gluWhere.get(gluKey))) {
					String function = adqlFunc.get(this.gluWhere.get(gluKey));
					String params = adqlFuncParams.get(this.gluWhere.get(gluKey)+DOT_CHAR+currentSelectedTapTable);
					try {
						if (function!=null && params!=null) {
							function = String.format(function, params.split("\t"));
						}
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
						continue;
					}
					whereQuery.append(function);
				} else {
					whereQuery.append(this.gluWhere.get(gluKey));
				}
				isfirst = false;
			}
		}
		if (whereQuery.length()>0) {
			gluQuery.append("WHERE ").append(whereQuery);
		}
	}
	
	/**
	 * Method return query string formed by the glu template(set from AlaGlu.dic) and user inputs
	 * @param v
	 * @param currentSelectedTapTable 
	 * @param adqlFuncParams 
	 * @param adqlFunc 
	 * @param gluQuery
	 * @return gluQuery
	 */
	public String getGluQuery(Vector v, String currentSelectedTapTable, Hashtable<String, String> adqlFunc, Hashtable<String, String> adqlFuncParams) {
		StringBuffer query = new StringBuffer("SELECT ");
		this.setGluQueryAllOrDistinct(v, query);
		this.setGluQueryTop(v, query);
		this.setGluQueryColumns(v, query);
		this.setGluQueryTable(v, query);
		this.setGluQueryWheres(v, query, currentSelectedTapTable, adqlFunc, adqlFuncParams);
		return query.toString();
	}
	
	public static void copy(String prefixe, String gluIndex, GluAdqlTemplate sourceGluAdqlTemplate, GluAdqlTemplate queryToUpdate) {
		String valueSet = EMPTYSTRING;
		if (sourceGluAdqlTemplate.gluWhere.containsKey(gluIndex)) {
			queryToUpdate.gluWhere.put(gluIndex, sourceGluAdqlTemplate.gluWhere.get(gluIndex));
		} else if (sourceGluAdqlTemplate.gluSelect.containsKey(gluIndex)) {
			valueSet = sourceGluAdqlTemplate.gluSelect.get(gluIndex);
			if (prefixe.equalsIgnoreCase(TOP)) {
				queryToUpdate.gluTop = new String[2];
				queryToUpdate.gluTop[0] = gluIndex;
				queryToUpdate.gluTop[1] = valueSet;
			} else if (prefixe.equalsIgnoreCase(ALL) || prefixe.equalsIgnoreCase(DISTINCT)) {
				queryToUpdate.gluAllOrDistinct = new String[2];
				queryToUpdate.gluAllOrDistinct[0] = gluIndex;
				queryToUpdate.gluAllOrDistinct[1] = valueSet;
			} else {
				queryToUpdate.gluSelect.put(gluIndex, valueSet);
			}
		} else if (sourceGluAdqlTemplate.gluFrom.containsKey(gluIndex)) {
			valueSet = sourceGluAdqlTemplate.gluFrom.get(gluIndex);
			queryToUpdate.gluFrom.put(gluIndex, valueSet);
			queryToUpdate.gluTable = new String[2];//prefixe.equalsIgnoreCase(GLU_TABLES)
			queryToUpdate.gluTable[0] = gluIndex;
			queryToUpdate.gluTable[1] = valueSet;//TODO:: tintin delete this if table is the only from
		}
		
	}
	
	public void addFrom(String gluIndex, GluAdqlTemplate sourceGluAdqlTemplate) {
		this.gluFrom.put(gluIndex, sourceGluAdqlTemplate.gluFrom.get(gluIndex));
	}
	
	public void addWhere(String gluIndex, GluAdqlTemplate sourceGluAdqlTemplate) {
		this.gluWhere.put(gluIndex, sourceGluAdqlTemplate.gluWhere.get(gluIndex));
	}
	
	public void addSelect(String gluIndex, GluAdqlTemplate sourceGluAdqlTemplate) {
		this.gluSelect.put(gluIndex, sourceGluAdqlTemplate.gluSelect.get(gluIndex));
	}
	
	
	public Hashtable<String,String> getGluFrom() {
		return gluFrom;
	}
	public void setGluFrom(Hashtable<String,String> gluFrom) {
		this.gluFrom = gluFrom;
	}
	public Hashtable<String,String> getGluSelect() {
		return gluSelect;
	}
	public void setGluSelect(Hashtable<String,String> gluSelect) {
		this.gluSelect = gluSelect;
	}
	public Hashtable<String,String> getGluWhere() {
		return gluWhere;
	}
	public void setGluWhere(Hashtable<String,String> gluWhere) {
		this.gluWhere = gluWhere;
	}
	

	
}
