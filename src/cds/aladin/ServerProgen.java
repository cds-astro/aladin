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

import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.tree.*;

/**
 * Le formulaire d'interrogation par cat�gorie sous la forme d'un arbre
 * D�velopements dans le cadre du projet WFP5 AIDA
 * 
 * M�thode : t�l�charge � l'ouverture du formulaire les d�finitions GLU qui d�vrivent
 * l'arbre des cat�gories (voir loadRemoteTree())
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : aout 2009
 */
public class ServerProgen extends ServerTree  {
   
   TreeNodeProgen showNode=null;
   	
  /** Initialisation des variables propres */
   protected void init() {
      type    = APPLI;
      aladinLabel     = "Progen";
   }
   
   protected void createChaine() {
      super.createChaine();
      title = "Access to original images";
      info = "Select a progenitor, and press SUBMIT";
      info1 = "";
   }

 /** Creation du formulaire d'interrogation par arbre. */
   protected ServerProgen(Aladin aladin) { super(aladin); suite(); }
   
   protected void initTree() { };
   
   protected void updateColor() {
      for( TreeNode n : this ) {
         if( !(n instanceof TreeNodeProgen) ) continue;
         ((TreeNodeProgen)n).updateColor();
      }
   }
   
   /** "Mise � jour" de l'arbre en fonction des prog�niteurs r�cup�r�s */
   protected void updateTree(HealpixIndex hi,PlanBG planBG) {
      boolean modified=false;
      if( hi==null ) {
         freeTree();
      } else {

         // M�morisation des noeuds terminaux d�j� existants dans l'arbre
         // IL FAUDRAIT MEMORISER LES NOEUDS TERMINAUX
         HashMap<String,TreeNode> v = new HashMap<String,TreeNode>();
         for( TreeNode n : this ) {
            System.out.println("J'ai dans l'arbre : "+n.getID());
            v.put(n.getID(),n );
         }

         // Ajout des noeuds manquants dans l'arbre
         for( String id : hi ) {
            TreeNode n;
            // D�j� pr�sent dans l'arbre
            if( (n=v.get(id))!=null ) { 
               if( n instanceof TreeNodeProgen ) ((TreeNodeProgen)n).touch();
               v.remove(id);
               continue;
            }
            HealpixIndexItem hii = hi.get(id);
            TreeNodeProgen node = new TreeNodeProgen(planBG,hii);
            System.out.println("J'ajoute "+id);
            modified=true;
            createTreeBranch(root,node,0);
         }

         // Suppression des noeuds encore en trop dans l'arbre
         for( String id : v.keySet() ) {
//            if( id.equals("root") ) continue;
            TreeNode n = v.get(id);
            if( !(n instanceof TreeNodeProgen )) continue;
            if( ((TreeNodeProgen)n).inLive() ) {
               System.out.println("Je devrais supprimer "+id+" mais pas encore assez vieux");
               continue;
            }
            System.out.println("Je supprime "+id);
            modified=true;
            removeTreeBranch(root,id);
         }
      }
      if( modified ) fireTreeChanged();
      else repaint();
   }
   
   protected void suite() {
      tree.addMouseMotionListener( new MouseMotionAdapter() {
         public void mouseMoved(MouseEvent e) { 
            int selRow = tree.getRowForLocation(e.getX(), e.getY()); 
            if( selRow==-1 ) return;
            TreePath path = tree.getPathForLocation(e.getX(), e.getY()); 
            TreeNodeProgen noeud = (TreeNodeProgen)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            System.out.println("Je suis sur "+noeud.getID());
            showNode=noeud;
            aladin.view.repaintAll();
         } 
      });
   }
   
}
