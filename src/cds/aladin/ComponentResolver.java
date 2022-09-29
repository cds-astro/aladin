// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

/*
 * Created on 2 févr. 2004
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.lang.reflect.Field;
import java.util.StringTokenizer;


/**
 * <p>Title : ComponentResolver</p>
 * <p>Description : resolves a component thanks to its name</p>
 * @author Thomas Boch [CDS]
 * @version 0.1 (kickoff : 02/02/2004)
 */
public class ComponentResolver {
    
    /** Constructor */
    public ComponentResolver() {
        
    }
    
    /** Find an object by a name of the form objectA.objectB.objectC
     * 
     * @param name name of the component we search, may be of the form objectA.objectB.objectC
     * @param start object from which we start the search
     * @return the searched object, null if not found
     */
    public Object findByFullName(String name, Object start) {
        StringTokenizer st = new StringTokenizer(name, ".");
        if( st.countTokens()==0 ) return null;
        
        Object curStart = start;
        Object curObject = null;
        while( st.hasMoreTokens() ) {
            String token = st.nextToken();
            token = ScriptFactory.decode(token);
            //System.out.println("searching "+token+" starting from "+curStart);
            curObject = findByName(token, curStart);
            if( curObject==null ) return null;
            curStart = curObject;
        }
        
        return curObject;
    }
    
    /** Find a component according to its name
     * 
     * @param name name of the component to search
     * @param start object from which we start the search
     * @return the searched object, null if not found
     * REMARQUE : pour le moment, la recherche ne se fait qu'à un niveau
     * On verra par la suite si une recherche récursive est nécessaire
     */
    public Object findByName(String name, Object start) {
        // first, test if start implements WidgetFinder
        if( start instanceof SwingWidgetFinder) {
            // we return start itself, as name is not a real object
            if( ((SwingWidgetFinder)start).findWidget(name) ) return start;
        }
        
        // second : if start does not implement WidgetFinder, 
        Class cl = start.getClass();
        int nbLevels=2; // nb de niveaux à remonter dans l'héritage
        int curLev = 0;
        Field field = null;
        boolean onContinue = true;
        while( onContinue && curLev<=nbLevels ) {
            Field[] fields = cl.getDeclaredFields();
            for( int i=0; i<fields.length; i++ ) {
//            	System.out.println(fields[i].getName());
//                System.out.println(fields[i]);
                if( fields[i].getName().equals(name)) {
                    field = fields[i];
                    onContinue = false;
                    break;
                }
            }
            if( field==null ) {
                curLev++;
                // let's also try in the superclass if necessary
                // (we restrain to 2 levels at the time being)
                cl = cl.getSuperclass();
            }
        }

        if( field==null ) return null;
        /*
        try {
            field = cl.getField(name);
            System.out.println("field : "+field);
        }
        catch( Exception e) {
            e.printStackTrace();
            return null;
        }
        */
        Object o;
        try {
            o = field.get(start);
        }
        catch( Exception e ) {
            e.printStackTrace();
            return null;
        }
        
        return o;
    }
    
    public boolean isHomeMade(String name, Object start) {
        if( start instanceof SwingWidgetFinder) {
            if( ((SwingWidgetFinder)start).findWidget(name) ) return true;
        }
        
        return false;
    }
}
