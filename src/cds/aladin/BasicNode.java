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

/** <p>Title : BasicNode</p>
 *  <p>Description : Basic node for a tree</p>
 *  <p>Copyright: 2002</p>
 *  <p>Company: CDS </p>
 *  @author Thomas Boch
 *  @version 0.5 : 27 Novembre 2002 (Creation)
 */


package cds.aladin;

import java.util.Vector;
import java.util.Enumeration;

abstract public class BasicNode {

     Vector children;    // fils du noeud
     String name;
     String altName; // pour SIAP evol, un nom alternatif (PREVIEW, CUTOUT...)
     BasicNode father = null;

    int level;
    // Coordonnees du nom dans le canvas BasicTree
    int x,y;

    boolean isOpen=false;
    boolean isLeaf=false;
    // pour SIAP evol
    boolean isObs=false;
    int nbChildren=0;
    // true if this node must be hidden
    boolean hide = false;
    // true if the node is selected
    boolean isSelected=false;
    // true if the node lits up
    boolean litup=false;
    
    Aladin aladin;

	// Constructeur par recopie
	BasicNode(Aladin aladin, BasicNode n) {
	    super();
	    this.aladin = aladin;
	    this.name = n.name;
	    this.isLeaf = n.isLeaf;
	    this.hide = n.hide;
	    this.children = new Vector();
	}

    BasicNode(Aladin aladin) {
        this(aladin, "");
    }

    BasicNode(Aladin aladin, String name) {
    	this.aladin = aladin;
        this.name=name;
        children = new Vector();
    }

    BasicNode(Aladin aladin, String name, boolean isOpen, boolean isLeaf) {
        this(aladin, name);
        this.isOpen = isOpen;
        this.isLeaf = isLeaf;
    }

	abstract public BasicNode createNode(String name);

    public BasicNode addChild(String childName) {
        BasicNode newNode = createNode(childName);
        newNode.father = this;
        children.addElement(newNode);
        nbChildren++;
        return newNode;
    }

    public BasicNode addChild(String childName, boolean isOpen, boolean isLeaf) {
        BasicNode newNode = createNode(childName);
		newNode.isOpen = isOpen;
		newNode.isLeaf = isLeaf;
        newNode.father = this;
        children.addElement(newNode);
        nbChildren++;
        return newNode;
    }

    public BasicNode addChild(BasicNode child) {
        child.father = this;
        children.addElement(child);
        nbChildren++;
        return child;
    }

	public Enumeration getChildren() {
		return children.elements();
	}

	public int getNbOfChildren() {
		return children.size();
	}

	public BasicNode getChildrenAt(int index) {
		return (BasicNode)children.elementAt(index);
	}

	public BasicNode getParent() {
		return this.father;
	}
	

    /** Supprime le fils child de la liste des enfants
        @param child reference du noeud a supprimer
        @return true si le noeud a été effectivement supprimé, false sinon
     */
    public boolean removeChild(BasicNode child) {
        int index = children.indexOf(child);
        if( index>=0 ) {
            children.removeElementAt(index);
            nbChildren--;
            return true;
        }
        else return false;
    }
    
    /** Supprime tous les sous-noeuds */
    public void removeAllChild() {
        children.removeAllElements();
        nbChildren = 0;
    }

    public void changeState() {
        isOpen=!isOpen;
    }

    // retourne true si this est le dernier fils de son père
    public boolean isLastChild() {
        if( father==null ) return false;
        BasicNode last = father.lastChild();
        return (last!=null&&last.equals(this));
    }

    public BasicNode lastChild() {
        if(nbChildren>0) {
            return (BasicNode)children.elementAt(children.size()-1);
        }
        return null;
    }

    /** returns the corresponding node if this node has a (direct) child called s, null otherwise */
    public BasicNode getChild(String s) {
        Enumeration e = children.elements();
        BasicNode current;
        while( e.hasMoreElements() ) {
            current = (BasicNode)e.nextElement();
            if( current.name!=null && current.name.equals(s) ) return current;
        }
        return null;
    }

    /** Opens all children */
    /*
    public void openChildren() {
        Enumeration e = children.elements();
        BasicNode current;
        while( e.hasMoreElements() ) {
            current = (BasicNode)e.nextElement();
            current.isOpen = true;
        }
    }
    */

    public boolean isSelected() {
        if(!isLeaf) return false;
        return isSelected;
    }
}
