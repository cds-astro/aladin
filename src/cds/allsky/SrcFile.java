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

/**
 * 
 */
package cds.allsky;

import cds.aladin.Calib;
import cds.fits.Fits;

class SrcFile {
	Fits fitsfile;
	Calib calib;
	
	SrcFile() { }
	
    @Override
	public String toString() {
		return fitsfile.getFilename().toString();
	}

}