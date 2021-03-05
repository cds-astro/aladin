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
      if( treeObj.isHidden && node.isLeaf() ) return -2;
      if( node.isLeaf() ) return treeObj.getIsIn();

      DefaultMutableTreeNode subNode = null;
      Enumeration e = node.children();
      int rep = -2;
// Méthode de colaration initiale => le vert n'était pas contaminant
//      while( e.hasMoreElements() ) {
//         subNode = (DefaultMutableTreeNode) e.nextElement();
//         int isIn =  populateFlagIn(subNode);
//         if( rep==-2 ) rep=isIn;
//         else if( rep!=isIn ) rep=-1;
//      }
      while( e.hasMoreElements() ) {
         subNode = (DefaultMutableTreeNode) e.nextElement();
         int isIn =  populateFlagIn(subNode);
         if( isIn==-2 ) continue;
         if( rep==-2 ) rep=isIn;
         else if( isIn==1 ) rep=isIn;             // Le vert est contaminant
         else if( isIn==-1 && rep==0 ) rep=isIn;  // Le blanc aussi mais uniquement si orange précédemment
      }

      treeObj.setIn(rep);
      return rep;
   }
   
   /** Comptage de la descendance de chaque branche (nombre de noeuds terminaux d'une branche)
    * Mémorisation dans TreeObj, soit en tant que référence (hs!=null), soit
    * en tant que décompte courant
    * @param hpanel mémorisation des valeurs sous forme path=n (ex: /Image/Optical=32, ...)
    */
   protected int countDescendance() { return countDescendance(null); }
   protected int countDescendance(HashMap<String,Integer> hs) {
      if( root.isLeaf() ) return 0;
      int nb=countDescendance(root.toString(),root,hs);
      return nb;
  }
   private int countDescendance(String prefix,DefaultMutableTreeNode parent,HashMap<String,Integer> hs) {
      TreeObj to = (TreeObj) parent.getUserObject();
      if( parent.isLeaf() )  return 1;

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
      } else {
         to.nb = n;
     }
      
      return n;
   }

   // Mémorisation des noeuds parents dans une hashmap pour les retrouver rapidement
   private HashMap<String, DefaultMutableTreeNode> fastAccess=null;
   
   /** Réinitialisation des éléments accélérateurs de la création de l'arbre */
   protected void resetCreate() {
      fastAccess = new HashMap<>();
   }
   
   // Mémorisation d'un noeud par son path afin de le retrouver rapidement
   private void memoFast( DefaultMutableTreeNode node ) {
      TreeObj to = (TreeObj) node.getUserObject();
      fastAccess.put( to.path, node );
   }
   
   // Retourne le noeud correspondant au path, null sinon
   private DefaultMutableTreeNode findFast( String path ) {
      return fastAccess.get( path );
   }
   
   /** Insertion d'un noeud, éventuellement avec les différents éléments de sa branche
    * si ceux-ci n'existent pas encore. Conserve le noeud parent de l'insertion
    * dans lastParentNode afin d'accélerer une éventuelle insertion ultérieure au même endroit
    * @param treeObj Le nouveau noeud à insérer
    */
   protected void createTreeBranch(TreeObj treeObj) {
      // Création immédiate car c'est le même parent que la précédente insertion
      if( createWithExistingParent( treeObj) ) return;
      
      // Création récursive (éventuellement pour la branche)
      DefaultMutableTreeNode nodeUp [] = new DefaultMutableTreeNode[1];
      int index [] = new int[1];
      DefaultMutableTreeNode lastParentNode = createTreeBranch( this, root, treeObj, 0, nodeUp, index);
      if( lastParentNode!=null ) memoFast(lastParentNode);
   }
   
   /** Méthode interne - Tentative d'insertion d'un noeud sur le parent de la dernière insertion. Retourne true
    * si l'insertion est effectivement possible, false sinon */
   private boolean createWithExistingParent( TreeObj treeObj) {
      int pos = lastSlash(treeObj.path);
      if( pos==-1 ) return true;
      String path = treeObj.path.substring(0, pos);
      
      DefaultMutableTreeNode node = findFast( path );
      if( node==null ) return false;
      node.add( new DefaultMutableTreeNode(treeObj) );
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
    * Création éventuelle des noeuds des branches si ceux-ci n'existent pas encore
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
            if( pos==-1 ) {
               subNode = new DefaultMutableTreeNode( treeObj );
            }
            
            // Branche intermédiaire ? déjà connue ou non ?
            else  subNode = new DefaultMutableTreeNode( new TreeObj(aladin,"",null,label,path) );
            
            // Cas tordu où le père était en fait une branche terminal à tord
            // Je greffe alors l'objet terminal associé en tant que fils, et je rectifie la nature
            // de l'objet associé au père.
            TreeObj pere = (TreeObj) parent.getUserObject();
            if( pere!=null && pere instanceof TreeObjDir) {
               if( Aladin.levelTrace>=3 ) System.err.println("Directory tree clash on "+pere.path+" (supposed to be a leaf, and in fact a node)");
               DefaultMutableTreeNode pereNode = new DefaultMutableTreeNode( pere );
               parent.add( pereNode );
               parent.setUserObject( new TreeObj(aladin,"",null,pere.label,pere.path)  );
            }
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

