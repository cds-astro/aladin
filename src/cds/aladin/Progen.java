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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.JPanel;
import javax.swing.tree.*;

/**
 * Le formulaire d'interrogation des progéniteurs
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : juillet 2012
 */
public class Progen extends JPanel  {
   
   private TreeNodeProgen showNode=null;
   private Aladin aladin;
   private TreeForProgen tree;
   
   Progen(Aladin a) {
      aladin=a;
      setLayout( new BorderLayout());
      tree = new TreeForProgen(aladin);
      add(tree,BorderLayout.CENTER);
      
      tree.addMouseMotionListener( new MouseMotionAdapter() {
         public void mouseMoved(MouseEvent e) { 
            int selRow = tree.getRowForLocation(e.getX(), e.getY()); 
            if( selRow==-1 ) return;
            TreePath path = tree.getPathForLocation(e.getX(), e.getY()); 
            TreeNode noeud = (TreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            if( !(noeud instanceof TreeNodeProgen) ) return;
//            System.out.println("Je suis sur "+noeud.getID());
            showNode=(TreeNodeProgen)noeud;
            aladin.view.repaintAll();
         } 
      });
      
      tree.addMouseListener( new MouseListener() {
         public void mouseReleased(MouseEvent e) { }
         public void mousePressed(MouseEvent e) { }
         public void mouseExited(MouseEvent e) {
            if( showNode!=null ) { showNode=null; aladin.view.repaintAll(); }
         }
         public void mouseEntered(MouseEvent e) { }
         public void mouseClicked(MouseEvent e) { }
      });
   }
   
   class TreeForProgen extends MyTree {
      protected TreeForProgen(Aladin aladin) { super(aladin); }
      protected void updateColor() { setColorByAge(); }
      protected void warning() { System.out.println("Il faut au-moins sélectionner un progéniteur"); }
      
//      /** Préparation de l'arbre afin qu'il "pré-ouvre" toutes les branches */
//      protected void defaultExpand() {
//         expandPath(new TreePath(root));
//         Enumeration e = root.preorderEnumeration();
//         while( e.hasMoreElements() ) {
//            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
//            /* if( node.isLeaf() ) collapsePath(new TreePath(node));
//            else */ expandPath(new TreePath(node));
//         }
//      }

   }
   
   public void submit() { tree.submit(); }
   
   protected void copyInPad() {
      for( TreeNode n : tree ) {
         if( !(n instanceof TreeNodeProgen) ) continue;
         aladin.console.printInPad( ( (TreeNodeProgen)n).label+"\n" );
      }
      aladin.console.setVisible(true);
   }
   
   protected void draw(Graphics g, ViewSimple v) {
      Color c = g.getColor();
      
      // affichage du progéniteur sous la souris
      if( showNode!=null )  { g.setColor(Color.red); showNode.draw(g, v); }
      
      // affichage des progéniteur sélectionnés
      g.setColor(Color.blue);
      for( TreeNode n : tree ) {
         if( !(n instanceof TreeNodeProgen) ) continue;
         if( n.checkbox.isSelected() ) ((TreeNodeProgen)n).draw(g,v);
      }
  
      g.setColor(c);
   }
   
   protected void updateCheckByMouse(ViewSimple v,int xview,int yview) {
      for( TreeNode n : tree ) {
         if( !(n instanceof TreeNodeProgen) ) continue;
         ((TreeNodeProgen)n).updateCheckByMouse(v,xview,yview);
      }
      repaint();
   }
   	
   private void setColorByAge() {
      for( TreeNode n : tree ) {
         if( !(n instanceof TreeNodeProgen) ) continue;
         ((TreeNodeProgen)n).updateColor();
      }
   }
   
   /** "Mise à jour" de l'arbre en fonction des progéniteurs récupérés */
   protected void updateTree(HealpixIndex hi,PlanBG planBG) {
      boolean modified=false;
      if( hi==null ) {
         tree.freeTree();
      } else {

         // Mémorisation des noeuds terminaux déjà existants dans l'arbre
         // IL FAUDRAIT MEMORISER LES NOEUDS TERMINAUX
         HashMap<String,TreeNode> v = new HashMap<String,TreeNode>();
         for( TreeNode n : tree ) {
//            System.out.println("J'ai dans l'arbre : "+n.getID());
            v.put(n.getID(),n );
         }

         // Ajout des noeuds manquants dans l'arbre
         for( String id : hi ) {
            TreeNode n;
            // Déjà présent dans l'arbre
            if( (n=v.get(id))!=null ) { 
               if( n instanceof TreeNodeProgen ) ((TreeNodeProgen)n).touch();
               v.remove(id);
               continue;
            }
            HealpixIndexItem hii = hi.get(id);
            TreeNodeProgen node = new TreeNodeProgen(planBG,hii);
//            System.out.println("J'ajoute "+id);
            modified=true;
            tree.createTreeBranch(tree.getRoot(),node,0);
         }

         // Suppression des noeuds encore en trop dans l'arbre
         for( String id : v.keySet() ) {
//            if( id.equals("root") ) continue;
            TreeNode n = v.get(id);
            if( !(n instanceof TreeNodeProgen )) continue;
//            if( ((TreeNodeProgen)n).inLive() ) {
//               System.out.println("Je devrais supprimer "+id+" mais pas encore assez vieux");
//               continue;
//            }
//            System.out.println("Je supprime "+id);
            modified=true;
            tree.removeTreeBranch(tree.getRoot(),id);
         }
      }
      if( modified ) {
         tree.fireTreeChanged();
         tree.defaultExpand();
      }
      else repaint();
      
   }
   
   
}
