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

/*
 * Created on 2 févr. 2004
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.awt.Point;

/**
 * <p>Title : SwingWidgetFinder</p>
 * <p>Description : Interface to find and locate "home-made" widgets</p>
 * @author Thomas Boch [CDS]
 * @version 0.1 (kickoff : 02/02/2004)
 */
public interface SwingWidgetFinder {

   /** Find a "home-made" widget by its name
    *
    * @param name name of the widget we look for
    * @return boolean <i>true</i> if widget found, <i>false</i> otherwise
    */
   public boolean findWidget(String name);

   /** Get location of a "home-made" widget
    *
    * @param name name of the widget
    * @return Point location of the widget within the object
    */
   public Point getWidgetLocation(String name);
}
