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

package cds.tools;
import java.io.*;

/**
 * Interface between two Virtual Observatory applications, such as
 * Aladin (CDS), VOPlot( VO-India), TOPcat, APT (STScI) to listen current
 * J2000 position and pixel value from another VO application.
 * see also VOApp interface and notably addObserver() method.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : 14 october 2005
 */
public abstract interface VOObserver {
   
     
   /** The observer method to call on a position VO event
    * in order to transmit the current coordinates J2000 position.
    * Generally called by a java mouse event, it is strongly recommended
    * to implemented this method as short as possible.
    * @param raJ2000 Right ascension in degrees J2000 
    * @param deJ2000 Declination in degrees J2000
    */
   public abstract void position(double raJ2000,double deJ2000);
   
   /** The observer method to call on a pixel VO event
    * in order to transmit the current pixel value
    * Generally called by a java mouse event, it is strongly recommended
    * to implemented this method as short as possible.
    * @param pixValue pixel value under the mouse
    */
   public abstract void pixel(double pixValue);
}
