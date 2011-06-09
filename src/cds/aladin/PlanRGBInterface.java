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

/**
 * Plan disposant de la couleur
 * @version 1.0 : mars 2011
 */
public abstract interface PlanRGBInterface {
   abstract public int [] getPixelsRGB();
   abstract public void getPixels(int [] newpixels,int x,int y,int w,int h);
   abstract public int [] getPixelsZoomRGB();
   abstract public void calculPixelsZoomRGB();
}
