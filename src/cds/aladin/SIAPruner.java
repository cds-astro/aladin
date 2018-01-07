// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * Une classe permettant de filtrer des ResourceNode décrivant des ressources SIAP/SSAP
 * afin de n'en conserver qu'un certai nombre, sur la base de certains critères
 *
 * @author Thomas Boch [CDS]
 *
 */
public class SIAPruner {

    private ResourceNode[] nodesToPrune;
    private String[] constraints;

    public SIAPruner(ResourceNode[] nodesToPrune, String[] constraints) {
        setNodesToPrune(nodesToPrune);
        setConstraints(constraints);
    }

    public ResourceNode[] prune() {
        if( nodesToPrune==null ) {
            return null;
        }

        ArrayList nodesToKeep = new ArrayList(Arrays.asList(nodesToPrune));
        SIAPrunerConstraint prunerConstraint = new SIAPrunerConstraintFactory().create(constraints);
        Aladin.trace(1, "Constraint is: "+prunerConstraint);

        if( prunerConstraint.orderBy>0 ) {
            // sort by distance to center
            if( prunerConstraint.orderBy==prunerConstraint.ORDERBYDIST ) {
                Collections.sort(nodesToKeep, new Comparator() {
                    public int compare(Object o1, Object o2) {
                       ResourceNode n1 = (ResourceNode)o1;
                       ResourceNode n2 = (ResourceNode)o2;
                       double dist1 = n1.getDistanceToCenter();
                       double dist2 = n2.getDistanceToCenter();
                       return dist1==dist2 ? 0 : dist1>dist2 ? 1 : -1;
                    }
                 });

            }
        }

        ArrayList nodesToRemove = new ArrayList();
        Iterator itNodes = nodesToKeep.iterator();
        ResourceNode node;
        // loop on all nodes
        while( itNodes.hasNext() ) {
            node = (ResourceNode)itNodes.next();
            // handle field constraints
            Enumeration e = prunerConstraint.fieldConstraints.keys();
            String key, value;
            while( e.hasMoreElements() ) {
                key = (String)e.nextElement();
                value = (String)prunerConstraint.fieldConstraints.get(key);
                value = Tok.unQuote(value);   // PF - nov 2014
                if( ! node.matchFieldConstraint(key, value, true) ) {
                    nodesToRemove.add(node);
                    continue;
                }
            }

            // handle free constraints
            Iterator itFreeCons = prunerConstraint.freeConstraints.iterator();
            String constraint;
            while( itFreeCons.hasNext() ) {
                constraint = (String)itFreeCons.next();
                if( ! node.matchFreeConstraint(constraint, true) ) {
                    nodesToRemove.add(node);
                    continue;
                }
            }
        }

        nodesToKeep.removeAll(nodesToRemove);

        // return the number of nodes requested
        if( nodesToKeep.size()==0 ) {
            Aladin.error("Could not find any data corresponding to your request");
            return null;
        }

        int nbToCopy = Math.min(prunerConstraint.nbRequested, nodesToKeep.size());

        ResourceNode[] retNodes = new ResourceNode[nbToCopy];
        System.arraycopy(nodesToKeep.subList(0, nbToCopy).toArray(), 0, retNodes, 0, nbToCopy);

        return retNodes;
    }

    public void setNodesToPrune(ResourceNode[] nodesToPrune) {
        this.nodesToPrune = nodesToPrune;
    }

    public void setConstraints(String[] constraints) {
        this.constraints = constraints;
    }

    /**
     * Classe construisant un SIAPrunerConstraint à partir d'une chaîne de caractères
     * décrivant les constraintes, tel que passée dans une commande script
     *
     * A typical constraint array looks like:
     * {number=3,orderby=dist,keyword=2MASS,instrument=WFI}
     *
     * keyword=... constraints can be mapped anywhere
     *
     */
    class SIAPrunerConstraintFactory {
        private SIAPrunerConstraint create(String[] constraints) {
            SIAPrunerConstraint prunerConstraints = new SIAPrunerConstraint();
            String token;
            String key, value;
            key = value = null;
            int idx;
            boolean keyValue = false;
            for( int i=0; i<constraints.length; i++ ) {
                token = constraints[i];
                if( token==null ) continue;

                token = token.trim();
                if( (idx=token.indexOf('='))>=0 ) {
                    key = token.substring(0, idx).trim();
                    value = token.substring(idx+1).trim();
                    if( ! key.toLowerCase().equals("keyword") ) {
                        keyValue = true;
                    }
                    else {
                        keyValue = false;
                        token = value;
                    }
                }
                else {
                    keyValue = false;
                }

                // request for a sort
                if( keyValue && key.equalsIgnoreCase("sortby") ) {
                    if( value.equalsIgnoreCase("dist") ) {
                        prunerConstraints.orderBy = prunerConstraints.ORDERBYDIST;
                    }
                }
                // specifying the number of resources to keep
                else if( keyValue && key.equalsIgnoreCase("number") ) {
                    try {
                        int nb = Integer.parseInt(value);
                        if( nb>0 ) {
                            prunerConstraints.nbRequested = nb;
                        }
                    }
                    catch(NumberFormatException nfe) {
                        Aladin.error("Can't parse "+value+" as an integer !\nConstraint ignored");
                    }
                }
                // constraint on a field
                else if( keyValue ) {
                    prunerConstraints.fieldConstraints.put(key, value);
                }
                // free constraint
                else {
                    prunerConstraints.freeConstraints.add(token);
                }
            }
            return prunerConstraints;
        }
    }

    /**
     * Classe décrivant une contrainte à appliquer sur un ensemble de ResourceNode
     *
     */
    class SIAPrunerConstraint {
        final int ORDERBYDIST = 1;

        int orderBy = -1;
        int nbRequested = 1; // by default, we keep only one resource
//        boolean getAll = false;
        ArrayList freeConstraints = new ArrayList(); // constraints that can be matched on any field
        Hashtable fieldConstraints = new Hashtable(); // constraints associated to field names (hashtable key : field name, value = field value)

        public String toString() {
            String s = "";
            s += "orderBy="+orderBy;
            s += ", nbRequested="+nbRequested;
            s += ", freeConstraints="+freeConstraints;
            s += ", fieldConstraints="+fieldConstraints;
            return s;
        }
    }

}
