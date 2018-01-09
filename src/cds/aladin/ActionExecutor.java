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

import java.awt.*;

import cds.tools.Util;

/**
 * <p>Title : ActionExecutor</p>
 * <p>Description : executes a list of actions (RobotAction), given the list of arguments</p>
 * @author Thomas Boch [CDS]
 * @version 0.1 (kickoff : 02/02/2004)
 */
public class ActionExecutor implements Runnable {
    private RobotAction[] actions;

    private ComponentLocator cl;
    private ComponentResolver cr;

    Thread runme;
    Aladin a;



    // flag pour notifier fin d'exécution (pour synchronisation)
    static boolean ready = true;

    /** Constructor
     *
     * @param actions list of actions to execute
     * @param args list of arguments
     * @param a reference to Aladin object
     */
    ActionExecutor(RobotAction[] actions, Aladin a) {
        this.actions = actions;
        this.a = a;
        cl = new ComponentLocator();
        cr = new ComponentResolver();
    }

    /** execute the whole list of actions */
    public synchronized void execute() {
        ready = false;
        runme = new Thread(this,"AladinRobot");
        Util.decreasePriority(Thread.currentThread(),runme);
//        runme.setPriority( Thread.NORM_PRIORITY -1);
        runme.start();
    }


    // to be set to true if one wants to interrupt the current action
    static boolean interruptAction = false;
    public void run() {
        String comp;
        String extraParam;
        for( int i=0; i<actions.length /*&& !stopAction*/; i++ ) {

            if( actions[i]==null ) continue;
            comp = actions[i].getComp();
            extraParam = actions[i].getParam();

            execute(actions[i], comp, extraParam);
        }
        // raz
        raz();

    }

    private void raz() {
        ready = true;
        //stopAction = false;
        interruptAction = false;
    }

    private void execute(RobotAction action, String comp, String param) {
        // first, resolve the component
        Object o = cr.findByFullName(comp, a);
        Aladin.trace(3, "Searching for component "+comp);
        //System.out.println("search for : "+comp);
        //System.out.println("comp : "+o);
        if( o==null || !(o instanceof Component) ) {
        	Aladin.trace(3, "Component "+comp+" not found !!");
        	return;

        }
        // then find location
        Component c = (Component)o;
        Point location;
        String singleName = comp;
        int dotIndex = comp.lastIndexOf('.');
        if( dotIndex>=0 ) singleName = comp.substring(dotIndex+1);
        singleName = ScriptFactory.decode(singleName);
        try {
           if( cr.isHomeMade(singleName, c) ) location = cl.getLocation(singleName, (SwingWidgetFinder)c);
           else location = cl.getLocation(c);
        } catch( Exception e ) { location = cl.getLocation(c); }

        action.paramSet = param;

        // finally do the action
        action.doAction(location, c, a);
    }

}
