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

/*
 * Created on 2 févr. 2004
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JButton;
import javax.swing.JComboBox;

/**
 * <p>Title : ComponentLocation</p>
 * <p>Description : finds absolute screen coordinates of a component on the basis of its name</p>
 * @author Thomas Boch [CDS]
 * @version 0.1 (kickoff : 02/02/2004)
 */
public class ComponentLocator {
    
    /** Constructor */
    public ComponentLocator() {
    }
    
    /** Return absolute coordinates of a given component
     * 
     * @param comp component whose location is searched
     * @return Point point corresponding to the coordinates on the screen of this component, <i>null</i> if comp is null
     */
    public Point getLocation(Component comp) {
        if( comp==null ) return null;
        
        Point p = comp.getLocation();
        Component c = comp;
        
        while( (c = c.getParent()) != null ) {
            Point loc = c.getLocation();
            p.x += loc.x;
            p.y += loc.y;
        }
        
        
        Dimension dim = comp.getSize();
        if( comp instanceof JComboBox ) {
            p.x += dim.getWidth()-15;
            p.y += dim.getHeight()/2;
        }
        
        // adjustement in order to click in the middle of the component
        else if( comp instanceof JButton || comp instanceof MyButton ) {
            p.x += dim.getWidth()/2;
            p.y += dim.getHeight()/2;
        }
        
        
        return p;
    }
    
    public Point getLocation(Component comp, Component stop) {
        if( comp==null ) return null;
        
        Point p = comp.getLocation();
        Component c = comp;
        //System.out.println("stop : "+stop);
        
        while( (c = c.getParent()) != null && !c.equals(stop) ) {
            //System.out.println("comp : "+c);
            Point loc = c.getLocation();
            p.x += loc.x;
            p.y += loc.y;
        }
        
        Dimension dim = comp.getSize();
        if( comp instanceof JComboBox || comp instanceof MyButton ) {
            p.x += dim.getWidth()/2;
            p.y += dim.getHeight()/2;
        }
        
        // adjustement in order to click in the middle of the component
        else if( comp instanceof JButton ) {
            p.x += dim.getWidth()/2;
            p.y += dim.getHeight()/2;
        }
        
        return p;
    }
    
    /** Return absolute coordinates of a home-made widget, given its name
     * 
     * @param widgetName name of the widget whose location is searched
     * @param comp WidgetFinder where stands widgetName
     * @return Point points corresponding to the coordinates on the screen of this component, <i>null</i> if widgetName was not found
     */
    public Point getLocation(String widgetName, WidgetFinder comp) {
        Point loc = comp.getWidgetLocation(widgetName);
        Point compLoc = new Point(0,0);
        if( comp instanceof Component ) {
            compLoc = getLocation((Component)comp);
        }
        return new Point(loc.x+compLoc.x, loc.y+compLoc.y);
    }
}
