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

package cds.tools.parser;

import cds.astro.Unit;

/** Classe Node - Brique de base pour le parser */
public class Node {
    // les differents types de noeuds (null/operateur/valeur/variable/fonction)	
    public static final int NULL = -1;
    public static final int OP = 0;
    public static final int VALUE  = 1;
    public static final int VAR = 2;
    public static final int FUNCTION = 3;
        
    Node left;
    Node right;
    private double value;
    private boolean valSet = false;
    int op;
    int type;
    String svalue;
	protected Unit unit;
  
        
    Node() {
        type = NULL;
        left = null;
        right = null;
        op = NULL;
        value = 0.0;
		svalue="";
    }
    
    // used for debugging purposes
    /*
    public String toString() {
        String s = "";
        s += "left : "+(left==null?"null":left.toString())+"\n";
        s += "right : "+(right==null?"null":right.toString())+"\n";
        s += "op : "+op+"\n";
        s += "type : "+type+"\n";
        s += "value : "+value+"\n";
        s += "svalue : "+svalue+"\n";
        s += "unit : "+(unit==null?"null":unit.toString())+"\n";
        
        return s;
    }
    */
    
    public double getValue() {
        return value;
    }
    
    public void setValue(double d) {
        value = d;
        valSet = true;
    }
    
    public boolean valueIsSet() {
        return valSet;
    }
    
}
