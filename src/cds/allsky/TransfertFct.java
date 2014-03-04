// Copyright 2012 - UDS/CNRS
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

package cds.allsky;

import cds.aladin.PlanImage;

public enum TransfertFct {
	LOG (PlanImage.LOG), SQRT (PlanImage.SQRT), LINEAR (PlanImage.LINEAR), 
	ASINH (PlanImage.ASINH), POW2 (PlanImage.SQR);
	
	private final int code;
	TransfertFct(int i) { code = i; }
	
	static public TransfertFct getFromCode(int i) throws Exception {
       if( i==PlanImage.LOG )    return LOG;
       if( i==PlanImage.SQRT )   return SQRT;
       if( i==PlanImage.LINEAR ) return LINEAR;
       if( i==PlanImage.ASINH )  return ASINH;
       if( i==PlanImage.SQR )    return POW2;
       throw new Exception("TransfertFct code error");
   }
	
	int code() { return code;}
}
