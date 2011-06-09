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
import java.awt.Point;
import java.awt.Window;

import javax.swing.SwingUtilities;

/**
 * <p>Title : RobotAction</p>
 * <p>Description : action which can be done by the robot</p>
 * eg: PUSH myButton
 * @author Thomas Boch [CDS]
 * @version 0.1 (kickoff : 02/02/2004)
 */
public class RobotAction {
    // possible actions
    public static RobotAction PUSH = new RobotAction(); // push a button
    public static RobotAction TYPE = new RobotAction(); // type in some text
    public static RobotAction PAUSE = new RobotAction(); // pause
    public static RobotAction SELECT = new RobotAction(); // select an item in a Choice (drop down list)
    public static RobotAction TOFRONT = new RobotAction(); // toFront()
    public static RobotAction ADJUSTPOS = new RobotAction(); // adjust frame position in workspace
    public static RobotAction INFO = new RobotAction(); // special case for info command


    // describes the action
    private RobotAction action = null;
    // component on which the action is executed (eg : $1, $2)
    private String comp;
    // optional parameter (eg : text to type for TYPE)
    private String optParam;

    String paramSet;

    public RobotAction() {
    }

    /** Constructor
     *
     * @param action the action to use
     * @param comp string describing the component on which the action is executed
     * @param optParam optional parameter
     */
    public RobotAction(final RobotAction action, String comp, String optParam) {
        setAction(action);
        setComp(comp.trim());
        setParam(ScriptFactory.decode(optParam));
    }

    /** Execute the action at a given location
     *
     * @param loc location where to execute the action
     * @comp component sur lequel exécuter l'action (peut etre null)
     */
    public void doAction(Point loc, Component comp, Aladin aladinInst) {
        if( action==null ) {
            return;
        }

        if( action==PUSH ) {
            loc.setLocation(loc.x+5, loc.y+2);
            Window parentWindow = SwingUtilities.getWindowAncestor(comp);
            if( parentWindow!=null ) {
            	parentWindow.toFront();
            }
            MyRobot.moveTo(loc.x, loc.y, aladinInst);
            MyRobot.press(loc, comp);
        }
        else if( action==TYPE ) {
            loc.setLocation(loc.x+5, loc.y+7);
            MyRobot.moveTo(loc.x, loc.y, aladinInst);
            MyRobot.press(loc, comp);
            MyRobot.type(paramSet, comp);
        }
        else if( action==PAUSE ) {
            // default
            int nbMs = 1000;
            if( optParam!=null ) {
                try {
                    nbMs = Integer.parseInt(optParam);
                }
                catch(NumberFormatException e) {}
            }
            MyRobot.pause(nbMs);
        }
        else if( action==SELECT ) {
            MyRobot.select(paramSet, comp, loc);
        }
        else if( action==TOFRONT ) {
            MyRobot.toFront(comp);
        }
        else if( action==ADJUSTPOS ) {
            MyRobot.adjustPos(comp);
        }
        else if( action==INFO ) {
            MyRobot.info(optParam, aladinInst);
        }
    }

    public static RobotAction getActionFromString(String s) {
        if( s.equals("ADJUSTPOS") ) return ADJUSTPOS;
        else if( s.equals("PAUSE") ) return PAUSE;
        else if( s.equals("PUSH") ) return PUSH;
        else if( s.equals("SELECT") ) return SELECT;
        else if( s.equals("TOFRONT") ) return TOFRONT;
        else if( s.equals("TYPE") ) return TYPE;
        else if( s.equals("INFO") ) return INFO;
        else return null;
    }

    /**
     * @return RobotAction
     */
    public RobotAction getAction() {
        return action;
    }

    /**
     * Sets the action.
     * @param action The action to set
     */
    public void setAction(final RobotAction action) {
        this.action = action;
    }



    /**
     * @return String
     */
    public String getComp() {
        return comp;
    }

    /**
     * Sets the component on which the action is executed
     * @param comp The comp to set
     */
    public void setComp(String comp) {
        this.comp = comp;
    }

    /**
     * @return String
     */
    public String getParam() {
        return optParam;
    }

    /**
     * Sets the optional parameter
     * @param optParam The optParam to set
     */
    public void setParam(String optParam) {
        this.optParam = optParam;
    }

}
