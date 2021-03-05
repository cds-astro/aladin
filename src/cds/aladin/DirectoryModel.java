// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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
 * Gestion d'un model associ� au Directory Tree
 * @author Pierre Fernique [CDS]
 * @version 1.0 Janvier 2017 - cr�ation
 */
public class DirectoryModel extends DefaultTreeModel {
   protected DefaultMutableTreeNode root;
   private Aladin aladin;
   
   protected DirectoryModel(Aladin aladin) {
      super( new DefaultMutableTreeNode( new TreeObj(aladin,"root",null,Directory.ROOT_LABEL,"") ) );
      root = (DefaultMutableTreeNode) getRoot();
      this.aladin = aladin;
   }
   
   /** Colorations (noire, verte ou orange) des branches de l'arbre en fonction de l'�tat des feuilles */
   protected int populateFlagIn() { return populateFlagIn(root); }
   private int populateFlagIn(DefaultMutableTreeNode node) {
      TreeObj treeObj = (TreeObj) node.getUserObject();
      if( treeObj.isHidden && node.isLeaf() ) return -2;
      if( node.isLeaf() ) return treeObj.getIsIn();

      DefaultMutableTreeNode subNode = null;
      Enumeration e = node.children();
      int rep = -2;
// M�thode de colaration initiale => le vert n'�tait pas contaminant
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
         else if( isIn==-1 && rep==0 ) rep=isIn;  // Le blanc aussi mais uniquement si orange pr�c�demment
      }

      treeObj.setIn(rep);
      return rep;
   }
   
   /** Comptage de la descendance de chaque branche (nombre de noeuds terminaux d'une branche)
    * M�morisation dans TreeObj, soit en tant que r�f�rence (hs!=null), soit
    * en tant que d�compte courant
    * @param hpanel m�morisation des valeurs sous forme path=n (ex: /Image/Optical=32, ...)
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
      
      // M�morisation de r�f�rence
      if( hs!=null ) {
         hs.put(to.path, n ); 
         to.nb = to.nbRef = n;
         
      //D�compte temporaire
      } else {
         to.nb = n;
     }
      
      return n;
   }

   // M�morisation des noeuds parents dans une hashmap pour les retrouver rapidement
   private HashMap<String, DefaultMutableTreeNode> fastAccess=null;
   
   /** R�initialisation des �l�ments acc�l�rateurs de la cr�ation de l'arbre */
   protected void resetCreate() {
      fastAccess = new HashMap<>();
   }
   
   // M�morisation d'un noeud par son path afin de le retrouver rapidement
   private void memoFast( DefaultMutableTreeNode node ) {
      TreeObj to = (TreeObj) node.getUserObject();
      fastAccess.put( to.path, node );
   }
   
   // Retourne le noeud correspondant au path, null sinon
   private DefaultMutableTreeNode findFast( String path ) {
      return fastAccess.get( path );
   }
   
   /** Insertion d'un noeud, �ventuellement avec les diff�rents �l�ments de sa branche
    * si ceux-ci n'existent pas encore. Conserve le noeud parent de l'insertion
    * dans lastParentNode afin d'acc�lerer une �ventuelle insertion ult�rieure au m�me endroit
    * @param treeObj Le nouveau noeud � ins�rer
    */
   protected void createTreeBranch(TreeObj treeObj) {
      // Cr�ation imm�diate car c'est le m�me parent que la pr�c�dente insertion
      if( createWithExistingParent( treeObj) ) return;
      
      // Cr�ation r�cursive (�ventuellement pour la branche)
      DefaultMutableTreeNode nodeUp [] = new DefaultMutableTreeNode[1];
      int index [] = new int[1];
      DefaultMutableTreeNode lastParentNode = createTreeBranch( this, root, treeObj, 0, nodeUp, index);
      if( lastParentNode!=null ) memoFast(lastParentNode);
   }
   
   /** M�thode interne - Tentative d'insertion d'un noeud sur le parent de la derni�re insertion. Retourne true
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

   /** M�thode interne - Insertion r�cursive d'un noeud en fonction du "path" du noeud � ins�rer.
    * Cr�ation �ventuelle des noeuds des branches si ceux-ci n'existent pas encore
    * @param model Le mod�le associ� � l'arbre
    * @param parent Le noeud courant du parcours de l'arbre (root au d�but)
    * @param treeObj Le noeud � ins�rer
    * @param opos L'index courant dans le "path" du noeud, -1 si le path a �t� compl�tement parcouru
    *             (ex: Optical/Image/DSS2/color ) => pos = index du I de Image
    * @param parentUp tableau  (1er �lement) servant � m�moriser le noeud parent de l'insertion de la branche
    *               la plus haute dans l'arbre (pour pouvoir avertir les listeners de la greffe de la branche)
    * @param childIndex tableau  (1er �lement) servant � m�moriser l'indice de la brance greff�e au plus haut
    *              dans l'arbre (pour pouvoir avertir les listeners de la greffe de la branche)
    * @return Le parent direct de l'insertion du noeud (afin de pouvoir ins�rer plus rapidement un autre noeud au m�me endroit)
    */
   private DefaultMutableTreeNode createTreeBranch(DefaultTreeModel model, DefaultMutableTreeNode parent, 
         TreeObj treeObj, int opos, DefaultMutableTreeNode parentUp [], int childIndex []) {

      // D�termination du prochain �l�ment dans le path
      // Rq: On d�coupe par "/" mais sans prendre en compte "\/"
      int pos, offset=opos;
      do  {
         pos=treeObj.path.indexOf('/',offset);
         offset=pos;
         if( pos>1 && treeObj.path.charAt(pos-1)=='\\') offset++;
         else offset=-1;
      } while( offset!=-1 );

      // D�termination du label courant et de son path
      String label = pos<0 ? treeObj.path.substring(opos) : treeObj.path.substring(opos,pos);
      String path = pos<0 ? treeObj.path : treeObj.path.substring(0,pos);
      
      // Les noeuds utilisateurs n'utilisent pas de checkbox pour cet arbre (� supprimer si possible)
      ((TreeObj)parent.getUserObject()).noCheckbox();

      try {
         // Recherche du fils qui correspond � l'emplacement o� la greffe doit avoir lieu
         DefaultMutableTreeNode subNode = null;
         Enumeration e = parent.children();
         while( e.hasMoreElements() ) {
            subNode = (DefaultMutableTreeNode) e.nextElement();
            TreeObj fils = (TreeObj) subNode.getUserObject();
            if( label.equals(fils.label) ) break;
            subNode=null;
         }

         // Aucun fils ne correspond, il faut donc cr�er la branche (ou ins�rer le noeud terminal si on est au bout)
         if( subNode==null ) {
            
            // Noeud terminal ? c'est donc celui � ins�rer
            if( pos==-1 ) {
               subNode = new DefaultMutableTreeNode( treeObj );
            }
            
            // Branche interm�diaire ? d�j� connue ou non ?
            else  subNode = new DefaultMutableTreeNode( new TreeObj(aladin,"",null,label,path) );
            
            // Cas tordu o� le p�re �tait en fait une branche terminal � tord
            // Je greffe alors l'objet terminal associ� en tant que fils, et je rectifie la nature
            // de l'objet associ� au p�re.
            TreeObj pere = (TreeObj) parent.getUserObject();
            if( pere!=null && pere instanceof TreeObjDir) {
               if( Aladin.levelTrace>=3 ) System.err.println("Directory tree clash on "+pere.path+" (supposed to be a leaf, and in fact a node)");
               DefaultMutableTreeNode pereNode = new DefaultMutableTreeNode( pere );
               parent.add( pereNode );
               parent.setUserObject( new TreeObj(aladin,"",null,pere.label,pere.path)  );
            }
            parent.add(subNode);
         }
         
         // On n'est pas au bout du path, il faut donc continuer r�cursivement
         // (en fait, une boucle serait plus adapt�e, mais comme on ne descend jamais
         // bien profond, �a ne va pas g�ner
         if( pos!=-1 ) return createTreeBranch(model, subNode, treeObj, pos + 1, parentUp, childIndex);
         
         // Retourne le noeud parent
         return parent;

      } catch( Exception e ) {
         e.printStackTrace();
      }
      return null;
   }
}

