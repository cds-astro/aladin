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

public enum Action {
   
   INDEX, TILES, JPEG, PNG, MOC, MOCINDEX, MOCHIGHT, ALLSKY, GZIP, GUNZIP,
   FINDER,  // Pour compatibilité
   CLEAN, CLEANINDEX, CLEANTILES, CLEANFITS, CLEANJPEG, CLEANPNG, CHECK,
   RGB, TREE, CONCAT, PROGEN,
   ABORT, PAUSE, RESUME,
	
}
