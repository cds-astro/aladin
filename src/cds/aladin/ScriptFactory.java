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

/*
 * Created on 18 févr. 2004
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

/** This class aims at producing a TranslationScript, given a command name and its arguments
 * @author Thomas Boch[CDS]
 * @version 0.1 (kickoff: 18/02/2004)
 */
public class ScriptFactory {

    // hold all translation scripts for simple cases
    private Hashtable scripts;

    private String[] args;

    static private String[] defaultScripts = {
        // contour
        "PUSH toolbox.contour\n" +
        "PAUSE toolbox.contour 2000\n" +
        "ADJUSTPOS frameContour\n" +
        "PUSH frameContour.nbLevelsChoice\n "+
        "SELECT frameContour.nbLevelsChoice $$1\n" +
        "PUSH frameContour.submitBtn",

        // filter
        "PUSH toolbox.filter\n" +
        "PAUSE toolbox.filter 2000\n" +
        "ADJUSTPOS lastFilterCreated\n" +
        "TYPE lastFilterCreated.label $$1\n" +
        "TYPE lastFilterCreated.filterDef $$2\n" +
        "PUSH lastFilterCreated.applyBtn\n" +
        "TOFRONT f",

        // getlocal
        "PUSH searchdata\n" +
        "PAUSE searchdata 2000\n" +
        "PUSH dialog.local\n" +
        "TYPE dialog.localServer.file $$2\n" +
        "PUSH dialog.submit\n" +
        "TOFRONT f",

        // rgb
        "PUSH toolbox.rgb\n" +
        "PAUSE toolbox.rgb 2000\n" +
        "ADJUSTPOS frameRGB\n" +
        "PUSH frameRGB.cR\n" +
        "SELECT frameRGB.cR $$1\n" +
        "PUSH frameRGB.cG\n" +
        "SELECT frameRGB.cG $$2\n" +
        "PUSH frameRGB.cB\n" +
        "SELECT frameRGB.cB $$3\n" +
        "PUSH frameRGB.submitBtn\n" +
        "TOFRONT f",

        // getsimbadned
        "PUSH searchData\n" +
        "PAUSE searchData 2000\n" +
        "ADJUSTPOS dialog\n" +
        "PUSH dialog.$$1\n" +
        "PAUSE dialog 2000\n" +
        "TYPE dialog.curServer.target $$2\n" +
        "PUSH dialog.submit\n" +
        "TOFRONT f",

        // getaladin
        "PUSH searchData\n" +
        "PAUSE searchData 2000\n" +
        "ADJUSTPOS dialog\n" +
        "PUSH dialog.aladin\n" +
        "PAUSE dialog 2000\n" +
        "TYPE dialog.curServer.target $$2\n" +
        "PUSH dialog.submit\n" +
        "PAUSE searchData 9000\n" +
        "PUSH dialog.curServer.tree.$$3\n" +
        "PUSH dialog.submit\n" +
        "TOFRONT f",

        // getvizier
        "PUSH searchData\n" +
        "PAUSE searchData 2000\n" +
        "ADJUSTPOS dialog\n" +
        "PUSH dialog.vizier\n" +
        "PAUSE dialog 2000\n" +
        "TYPE dialog.vizierServer.target $$2\n" +
        "TYPE dialog.vizierServer.catalog $$3\n" +
        "PUSH dialog.submit\n" +
        "TOFRONT f",

        // info
        "INFO f $$1",

        // show/hide
        "PUSH calque.select.$$1",

        // ref
        "PUSH calque.select.ref$$1"
    };

    static private String[] defaultScriptsLabels = {
        "contour",
        "filter",
        "getlocal",
        "rgb",
        "getsimbadned",
        "getaladin",
        "getvizier",
        "info",
        "show",
        "ref"
    };

    public ScriptFactory() {
        scripts = new Hashtable();
        addDefaultScripts();
    }

    /** Build a TranslationScript corresponding to the command line
     *
     * @param cmdName name of the command
     * @param argsStr arguments string
     * @return the script corresponding to this command, <i>null</i> if there is no script for this cmd
     */
    public TranslationScript getScript(String cmdName, String argsStr) {
    	args = new String[3];
        String[] argsTmp = split(argsStr, " ");
        for( int i=0; i<argsTmp.length; i++ ) {
            if( argsTmp[i]!=null && i<args.length ) args[i] = argsTmp[i];
        }

        // d'abord les cas délicats
        if( cmdName.equals("get") ) {
            return processGet(cmdName, argsStr);
        }

        if( cmdName.equals("filter")) {
            return processFilter(cmdName, argsStr);
        }

        else if( cmdName.equals("rgb") ) {
            return processRGB(cmdName, argsStr);
        }

        else if( cmdName.equals("info") ) {
            return processInfo(cmdName, argsStr);
        }

        else if( cmdName.equals("hide") || cmdName.equals("show") ) {
            return processShow(cmdName, argsStr);
        }

        TranslationScript script = getScriptWithArgs(cmdName);

        if( script==null ) return null;

        return script;
    }


    /** Add a translation script to the robot knowledge
     *
     * @param script the translation script to add to the list of known scripts
     */
    public void addScript(TranslationScript script) {
        scripts.put(script.cmdName, script);
    }

    private void addDefaultScripts() {
        StringTokenizer st1, st2;

        // loop on available default scripts
        for( int i=0; i<defaultScripts.length; i++ ) {
            st1 = new StringTokenizer(defaultScripts[i], "\n");
            RobotAction a;
            String curLine;
            String actionStr, comp, param;
            Vector v = new Vector();
            // traitement de chaque ligne
            while( st1.hasMoreTokens() ) {
                curLine = st1.nextToken();

                st2 = new StringTokenizer(curLine, " ");

                actionStr = st2.nextToken();
                a = RobotAction.getActionFromString(actionStr);
                comp = st2.nextToken();
                if( st2.hasMoreTokens() ) param = st2.nextToken();
                else param = null;

                v.addElement(new RobotAction(a, comp, param));
            }
            RobotAction[] actions = new RobotAction[v.size()];
            v.copyInto(actions);
            addScript(new TranslationScript(defaultScriptsLabels[i], actions));
        }

    }

    /** special case for "info" command */
    private TranslationScript processInfo(String cmdName, String argsStr) {
        args[0] = argsStr;
        return getScriptWithArgs(cmdName);
    }

    /** special case for "show/hide" command */
    private TranslationScript processShow(String cmdName, String argsStr) {
        Plan p = Aladin.aladin.command.getNumber(argsStr,1,false,false);
        if( p==null ) return null;
        boolean doNothing = (cmdName.equals("show") && p.active) || (cmdName.equals("hide")) && !p.active;
        if( doNothing ) return null;

        return getScriptWithArgs("show");
    }

    /** special case for "filter" command */
    private TranslationScript processFilter(String cmdName, String argsStr) {
        //System.out.println(cmdName);
        //System.out.println(argsStr);
        if( argsStr.length()<7) return null;
        argsStr = argsStr.trim().substring(6);
        int begin = argsStr.indexOf('{');
        int end = argsStr.lastIndexOf('}');
        if( begin<0 || end<0 ) return null;

        args[0] = argsStr.substring(0,begin).trim();
        args[1] = argsStr.substring(begin+1, end);

        return getScriptWithArgs(cmdName);
    }

    /** special case for "RGB" command */
    private TranslationScript processRGB(String cmdName, String argsStr) {
        argsStr = argsStr.trim();
        StringTokenizer st = new StringTokenizer(argsStr, " ");
        int index;
        try {
            args[0] = getRGBLabel(Aladin.aladin.calque.plan[getNumber(st.nextToken(), 0)]);
            args[1] = getRGBLabel(Aladin.aladin.calque.plan[getNumber(st.nextToken(), 0)]);
            args[2] = getRGBLabel(Aladin.aladin.calque.plan[getNumber(st.nextToken(), 0)]);
            //for( int i=0; i<3; i++ ) System.out.println(args[i]);
        }
        catch(NumberFormatException e) {return null;}

        return getScriptWithArgs(cmdName);
    }

    /** special case for "get" command */
    private TranslationScript processGet(String cmdName, String argsStr) {
        StringBuffer servers = new StringBuffer();
        StringBuffer t = new StringBuffer();
        StringBuffer r = new StringBuffer();
        if( !Aladin.aladin.command.splitGetCmd(servers, t, r, argsStr,true ) )
            return null;

        String target = t.toString();
        String radius = r.toString();
        TranslationScript curTs;
        StringTokenizer stServer = new StringTokenizer(servers.toString(), ",");
        Vector v = new Vector();
        // loop on all servers
        while( stServer.hasMoreTokens() ) {
            curTs = getScriptForServer(stServer.nextToken(), target, radius);
            if( curTs==null ) return null;
            for( int i=0; i<curTs.actions.length; i++ ) v.addElement(curTs.actions[i]);
        }

        RobotAction[] actions = new RobotAction[v.size()];
        v.copyInto(actions);
        v = null;
        return new TranslationScript("get", actions);
    }

    /** returns the script for ONE server */
    TranslationScript getScriptForServer(String server, String target, String radius) {
        int lindex = server.indexOf('(');
        String serverName = server;
        String param = "";
        if( lindex>=0 ) {
            serverName = server.substring(0, lindex);
            int rindex = server.lastIndexOf(')');
            if( rindex>0 ) param = server.substring(lindex+1,rindex);
        }
        serverName = serverName.trim().toLowerCase();

        args[0] = serverName;

        if( serverName.equalsIgnoreCase("local")
              || serverName.equalsIgnoreCase("mydata")
              || serverName.equalsIgnoreCase("file")) {
            args[1] = param;

            return getScriptWithArgs("getlocal");
        }

        if( serverName.equals("aladin") ) {
            args[1] = target;
            if( param.length()>0 ) args[2] = param;
            else args[2] = "DSS";

            TranslationScript script = getScriptWithArgs("getaladin");
            Server as = Aladin.aladin.dialog.server[ServerDialog.ALADIN];
            // pour eviter une incoherence entre le target actuel et celui lors de l'execution
            Aladin.aladin.dialog.setDefaultParameters(Aladin.aladin.dialog.current,0);
            // si meme target, l'arbre a déja été construit, on supprime les actions inutiles
            if( as.tree!=null && !as.tree.isEmpty() && as.target.getText().equals(target) ) {
                for( int i=5; i<=7; i++ )
                    script.actions[i] = null;
            }
            //script.actions[] = null;
            return script;
        }

        if( serverName.equals("simbad") || serverName.equals("ned") ) {
            args[1] = target;

            return getScriptWithArgs("getsimbadned");
        }

        if( serverName.equals("vizier") ) {
            args[1] = target;
            if( param.length()>0 ) args[2] = param;
            else args[2] = "GSC2";

            return getScriptWithArgs("getvizier");
        }

        return null;
    }

    private TranslationScript getScriptWithArgs(String name) {
        TranslationScript orgScript = (TranslationScript)scripts.get(name);
        if( orgScript==null ) return null;
        RobotAction[] actions = new RobotAction[orgScript.actions.length];
        RobotAction a;
        for( int i=0; i<orgScript.actions.length; i++ ) {
            a = orgScript.actions[i];
            actions[i] = new RobotAction(a.getAction(), setArgs(a.getComp()), setArgs(a.getParam()));
        }

        return new TranslationScript(name, actions);
    }

    private String setArgs(String s) {
        if( s==null ) return null;
        String tmp = new String(s);
        int dollarIndex;
        int index;
        int dotIndex;
        while( (dollarIndex=tmp.indexOf("$$")) >= 0 ) {
            dotIndex = dollarIndex+3;

            index = Integer.parseInt(tmp.substring(dollarIndex+2, dotIndex));
            // pour eviter de partir en boucle infinie
            if( args[index-1]==null ) args[index-1] = "null";

            tmp = MetaDataTree.replace(tmp, "$$"+index, encode(args[index-1]), -1);
        }
        //System.out.println(tmp);
        return tmp;
    }

    static protected String encode(String s) {
        return MetaDataTree.replace(s, ".", "@@", -1);
    }

    static protected String decode(String s) {
        return MetaDataTree.replace(s, "@@", ".", -1);
    }

    /** Retourne le résultat d'un tokenizer sous forme de tableau
     *
     * @param str la chaine a splitter
     * @param sep caractère séparateur
     * @return le tableau de String
     */
    static protected String[] split(String str,String sep) {
        StringTokenizer st = new StringTokenizer(str,sep);
        String[] ret = new String[st.countTokens()];
        int i=0;
        while( st.hasMoreTokens() ) {
            ret[i] = st.nextToken();
            i++;
        }
        return ret;
    }

    private int getNumber(String s,int methode) {
       int n=0;
        Aladin a = Aladin.aladin;
       try{ n = Integer.parseInt(s); }
       catch(Exception e) {}
       if( n<1 || n>a.calque.plan.length ) {
          if( methode==1 ) {
             n=a.calque.getIndexPlan(s);
             if( n<0 ) {
                System.out.println("!!! Plane \"" + s + "\" not found !");
                return -1;
             }
             return n;
          }
          System.out.println("!!! Error on plane number "+s);
          return -1;
       }
       n=a.calque.plan.length-n;
       if( a.calque.plan[n].type==Plan.NO ) {
          System.out.println("!!! plane number "+s+" not assigned");
          return -1;
       }
       return n;
    }

    private String getRGBLabel(Plan p) {
        return p.label+" - \""+p.objet+"\"";
    }
}
