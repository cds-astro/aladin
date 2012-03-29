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


package cds.aladin;

import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.Enumeration;

import javax.swing.tree.*;

/**
 * Le formulaire d'interrogation par catégorie sous la forme d'un arbre
 * Dévelopements dans le cadre du projet WFP5 AIDA
 * 
 * Méthode : télécharge à l'ouverture du formulaire les définitions GLU qui dévrivent
 * l'arbre des catégories (voir loadRemoteTree())
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : aout 2009
 */
public class ServerProgen extends ServerTree  {
   
   TreeNodeProgen showNode=null;
   	
  /** Initialisation des variables propres */
   protected void init() {
      type    = APPLI;
      aladinLabel     = "watch";
      aladinLogo    = "Watch.gif";
   }
   
   protected void createChaine() {
      super.createChaine();
      title = "Access to original images";
      info = "Select a progenitor, and press SUBMIT";
      info1 = "";
   }

 /** Creation du formulaire d'interrogation par arbre. */
   protected ServerProgen(Aladin aladin) { super(aladin); suite(); }
   
   protected DefaultMutableTreeNode getRoot() {
      if( root==null ) root = new DefaultMutableTreeNode( new TreeNodeProgen(aladin,"root","","",null,null) );
      return root;
   }
   
   
   protected void initTree() { };
   
   protected void suite() {
      tree.addMouseMotionListener( new MouseMotionAdapter() {
         public void mouseMoved(MouseEvent e) { 
            int selRow = tree.getRowForLocation(e.getX(), e.getY()); 
            if( selRow==-1 ) return;
            TreePath path = tree.getPathForLocation(e.getX(), e.getY()); 
            TreeNodeProgen noeud = (TreeNodeProgen)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            System.out.println("Je suis sur "+noeud+" => "+noeud.stc);
            showNode=noeud;
            aladin.view.repaintAll();
         } 
      });
   }
   
}
