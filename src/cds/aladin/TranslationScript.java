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

/**
 * <p>Title : TranslationScript</p>
 * <p>Description : Broker between a command script and actions to be executed by the robot</p>
 * A translation script describes how to translate a given command to a list of actions
 * @author Thomas Boch [CDS]
 * @version 0.1 (kickoff : 02/02/2004)
 */
public class TranslationScript {
    // command name for this script
    public String cmdName;
    // actions for this script
    public RobotAction[] actions;
    
    /** Constructor
     * 
     * @param cmdName command name
     * @param actions array of actions corresponding to this command
     */
    public TranslationScript(String cmdName, RobotAction[] actions) {
        this.cmdName = cmdName;
        this.actions = actions;
    }
    
}
