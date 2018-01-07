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

import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * Gestion d'un model associé au Directory Tree
 * @author Pierre Fernique [CDS]
 * @version 1.0 Janvier 2017 - création
 */
public class DirectoryModel extends DefaultTreeModel {
   protected DefaultMutableTreeNode root;
   private Aladin aladin;
   
   protected DirectoryModel(Aladin aladin) {
      super( new DefaultMutableTreeNode( new TreeObj(aladin,"root",null,Directory.ROOT_LABEL,"") ) );
      root = (DefaultMutableTreeNode) getRoot();
      this.aladin = aladin;
   }
   
   /** Colorations (noire, verte ou orange) des branches de l'arbre en fonction de l'état des feuilles */
   protected int populateFlagIn() { return populateFlagIn(root); }
   private int populateFlagIn(DefaultMutableTreeNode node) {
      TreeObj treeObj = (TreeObj) node.getUserObject();
      if( node.isLeaf() ) return treeObj.getIsIn();

      DefaultMutableTreeNode subNode = null;
      Enumeration e = node.children();
      int rep = -2;
      while( e.hasMoreElements() ) {
         subNode = (DefaultMutableTreeNode) e.nextElement();
         int isIn =  populateFlagIn(subNode);
         if( rep==-2 ) rep=isIn;
         else if( rep!=isIn ) rep=-1;
      }
      treeObj.setIn(rep);
      return rep;
   }
   
   /** Comptage de la descendance de chaque branche (nombre de noeuds terminaux d'une branche)
    * Mémorisation dans TreeObj, soit en tant que référence (hs!=null), soit
    * en tant que décompte courant
    * @param hs mémorisation des valeurs sous forme path=n (ex: /Image/Optical=32, ...)
    */
   protected int countDescendance() { return countDescendance(null); }
   protected int countDescendance(HashMap<String,Integer> hs) {
      if( root.isLeaf() ) return 0;
      return countDescendance(root.toString(),root,hs);
   }
   private int countDescendance(String prefix,DefaultMutableTreeNode parent,HashMap<String,Integer> hs) {
      TreeObj to = (TreeObj) parent.getUserObject();
      if( parent.isLeaf() ) return 1;

      int n=0;
      Enumeration e = parent.children();
      while( e.hasMoreElements() ) {
         DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
         n += countDescendance( prefix, node,hs );
      }
      
      // Mémorisation de référence
      if( hs!=null ) {
         hs.put(to.path, n ); 
         to.nb = to.nbRef = n;
         
      //Décompte temporaire
      } else to.nb = n;
      
      return n;
   }

   // Dernier noeud parent ayant eu une instertion => permet éventuellement un ajout ultérieur plus rapide
   private DefaultMutableTreeNode lastParentNode=null;
   
   /** Insertion d'un noeud, éventuellement avec les différents éléments de sa branche
    * si ceux-ci n'existent pas encore. Conserve le noeud parent de l'insertion
    * dans lastParentNode afin d'accélerer une éventuelle insertion ultérieure au même endroit
    * @param treeObj Le nouveau noeud à insérer
    */
   protected void createTreeBranch(TreeObj treeObj) {
      // Création immédiate car c'est le même parent que la précédente insertion
      if( createLeafWithLastParent( this, treeObj) ) return;
      
      // Création récursive (éventuellement pour la branche)
      DefaultMutableTreeNode nodeUp [] = new DefaultMutableTreeNode[1];
      int index [] = new int[1];
      lastParentNode = createTreeBranch( this, root, treeObj, 0, nodeUp, index);
      
      // Indication aux listeners du modèle qu'une branche a été inséré
//      if( nodeUp[0]!=null ) nodesWereInserted( nodeUp[0], index);
   }
   
   /** Méthode interne - Tentative d'insertion d'un noeud sur le parent de la dernière insertion. Retourne true
    * si l'insertion est effectivement possible, false sinon */
   private boolean createLeafWithLastParent(DefaultTreeModel model, TreeObj treeObj) {
      if( lastParentNode==null ) return false;
      int pos = lastSlash(treeObj.path);
      if( pos==-1 ) return true;
      String path = treeObj.path.substring(0, pos);
      
      TreeObj pere = (TreeObj) lastParentNode.getUserObject();
      if( !path.equals(pere.path) ) return false;
      
      lastParentNode.add( new DefaultMutableTreeNode(treeObj) );
      
      return true;
   }
   
   private int lastSlash( String s ) {
      return s.lastIndexOf('/');
      
      // BIZARRE, CA NE MARCHE PAS  !!
//      for( int i=s.length()-1; i>=0; i-- ) {
//         if( s.charAt(i)=='/' ) {
//            if( i>0 && s.charAt(i-1)=='\\' ) continue;
//            return i;
//         }
//      }
//      return -1;
   }

   /** Méthode interne - Insertion récursive d'un noeud en fonction du "path" du noeud à insérer.
    * Création évenutelle des noeuds des branches si ceux-ci n'existente pas encore
    * @param model Le modèle associé à l'arbre
    * @param parent Le noeud courant du parcours de l'arbre (root au début)
    * @param treeObj Le noeud à insérer
    * @param opos L'index courant dans le "path" du noeud, -1 si le path a été complètement parcouru
    *             (ex: Optical/Image/DSS2/color ) => pos = index du I de Image
    * @param parentUp tableau  (1er élement) servant à mémoriser le noeud parent de l'insertion de la branche
    *               la plus haute dans l'arbre (pour pouvoir avertir les listeners de la greffe de la branche)
    * @param childIndex tableau  (1er élement) servant à mémoriser l'indice de la brance greffée au plus haut
    *              dans l'arbre (pour pouvoir avertir les listeners de la greffe de la branche)
    * @return Le parent direct de l'insertion du noeud (afin de pouvoir insérer plus rapidement un autre noeud au même endroit)
    */
   private DefaultMutableTreeNode createTreeBranch(DefaultTreeModel model, DefaultMutableTreeNode parent, 
         TreeObj treeObj, int opos, DefaultMutableTreeNode parentUp [], int childIndex []) {

      // Détermination du prochain élément dans le path
      // Rq: On découpe par "/" mais sans prendre en compte "\/"
      int pos, offset=opos;
      do  {
         pos=treeObj.path.indexOf('/',offset);
         offset=pos;
         if( pos>1 && treeObj.path.charAt(pos-1)=='\\') offset++;
         else offset=-1;
      } while( offset!=-1 );

      // Détermination du label courant et de son path
      String label = pos<0 ? treeObj.path.substring(opos) : treeObj.path.substring(opos,pos);
      String path = pos<0 ? treeObj.path : treeObj.path.substring(0,pos);
      
      // Les noeuds utilisateurs n'utilisent pas de checkbox pour cet arbre (à supprimer si possible)
      ((TreeObj)parent.getUserObject()).noCheckbox();

      try {
         // Recherche du fils qui correspond à l'emplacement où la greffe doit avoir lieu
         DefaultMutableTreeNode subNode = null;
         Enumeration e = parent.children();
         while( e.hasMoreElements() ) {
            subNode = (DefaultMutableTreeNode) e.nextElement();
            TreeObj fils = (TreeObj) subNode.getUserObject();
            if( label.equals(fils.label) ) break;
            subNode=null;
         }

         // Aucun fils ne correspond, il faut donc créer la branche (ou insérer le noeud terminal si on est au bout)
         if( subNode==null ) {
            
            // Noeud terminal ? c'est donc celui à insérer
            if( pos==-1 ) subNode = new DefaultMutableTreeNode( treeObj );
            
            // Branche intermédiaire ? déjà connue ou non ?
            else  subNode = new DefaultMutableTreeNode( new TreeObj(aladin,"",null,label,path) );
            
            parent.add(subNode);
         }
         
         // On n'est pas au bout du path, il faut donc continuer récursivement
         // (en fait, une boucle serait plus adaptée, mais comme on ne descend jamais
         // bien profond, ça ne va pas gêner
         if( pos!=-1 ) return createTreeBranch(model, subNode, treeObj, pos + 1, parentUp, childIndex);
         
         // Retourne le noeud parent
         return parent;

      } catch( Exception e ) {
         e.printStackTrace();
      }
      return null;
   }
}

