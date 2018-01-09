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

import static cds.aladin.Constants.TAPv1;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class ServerGluTemplate {
	
private StringBuffer record;
private String actionName;
private String aladinLogo;
private String aladinLabel;
private String description;
private String verboseDescr;
private String aladinMenu;
private String institute;
private String planeLabel;
private String docUser;
private String ordre;
private String system;
private String dir;
private String aladinProtocol;
private String[] tapTables;
Hashtable<String,String> adqlSelect; 
Hashtable<String,String> adqlFrom; 
Hashtable<String,String> adqlWhere; 
Hashtable<String, String> adqlFunc;
Hashtable<String, String> adqlFuncParams;
String[] aladinFilter;
String resultDataType;

private void memoServer(String actionName, String description, String verboseDescr,
        String aladinMenu, String aladinMenuNumber, String aladinLabel,
        String aladinLabelPlane, String docUser, String[] paramDescription,
		String[] paramDataType, String[] paramValue,
        String resultDataType, String institute, Vector aladinFilter1,
        String aladinLogo,String dir,boolean localFile, String system,StringBuffer record,String aladinProtocol, String[] aladinFilter,
        String[] tapTables, Hashtable<String,String> adqlSelect, Hashtable<String,String> adqlFrom, 
        Hashtable<String,String> adqlWhere, Hashtable<String, String> adqlFunc, Hashtable<String, String> adqlFuncParams) {
     
	this.record      = record;
    this.actionName  = actionName;
    this.aladinLogo  = aladinLogo;
    this.aladinLabel = aladinLabel;
    this.description = description;
    this.verboseDescr= verboseDescr;
    this.aladinMenu  = aladinMenu;
    this.institute   = institute;
    this.planeLabel  = aladinLabelPlane;
    this.docUser     = docUser;
    this.ordre       = aladinMenuNumber;
    this.system      = system;
    this.dir         = dir;
    this.adqlSelect = adqlSelect;
	this.adqlFrom = adqlFrom;
	this.adqlWhere = adqlWhere;
	this.adqlFunc = adqlFunc;
	this.adqlFuncParams = adqlFuncParams;
	this.aladinProtocol = aladinProtocol;
	this.tapTables = tapTables;
	this.aladinFilter= aladinFilter;
	this.resultDataType = resultDataType;


     if( system!=null && system.trim().length()==0 ) system=null;
     if( institute == null ) institute = description;

     ServerGlu g=null;
    /* if(TAPv1.equalsIgnoreCase(aladinProtocol)) {
         GluAdqlTemplate gluAdqlTemplate = new GluAdqlTemplate(adqlSelect, adqlFrom, adqlWhere, adqlFunc, adqlFuncParams);
     	g = new ServerGlu(aladin, actionName, description, verboseDescr, aladinMenu,
                 aladinMenuNumber, aladinLabel, aladinLabelPlane, docUser, paramDescription, paramDataType, paramValue,
                 null, resultDataType, institute, aladinFilter, aladinLogo, dir, system, record, aladinProtocol, tapTables, gluAdqlTemplate);
        	 g.setAdqlFunc(adqlFunc);
   		 g.setAdqlFuncParams(adqlFuncParams);
   		 TapManager.tapServerPanelCache.put(actionName, g);
   		 g.HIDDEN = true;
   		 if (localFile) {//changing tapserver here wont work. ServerDialog that containts the instance of tapserver is reloaded at glu reload.
   			lastTapGluServer = g;
//   			tapManager.loadTapServer(g);
			}
   		
    }*/
  }
  

}
