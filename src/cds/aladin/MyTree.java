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

package cds.aladin;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Iterator;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

/**
 * Gestion d'un arbre dynamique
 * @author Pierre Fernique [CDS]
 */
public class MyTree extends JTree implements Iterable<TreeObj>  {
   protected String info,info1;
   protected DefaultMutableTreeNode root;
   private Aladin aladin;

   protected MyTree(Aladin aladin) {
      this.aladin = aladin;
      createTree();
   }

   protected DefaultMutableTreeNode getRoot() {
      if( root==null ) root = new DefaultMutableTreeNode( new TreeObj(aladin,"root",null,"","") );
      return root;
   }

   /** Création de l'arbre */
   private void createTree() {
      root = getRoot();
      ((DefaultTreeModel)getModel()).setRoot(root);
      setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 0));

      setRootVisible(false);
      setShowsRootHandles(true);
      NoeudRenderer nr = new NoeudRenderer();
      setCellRenderer(nr);
      setCellEditor(new NoeudEditor(this));
      setEditable(true);
   }

   /** Nettoyage de l'arbre */
   protected void freeTree() {
      if( root!=null && root.getChildCount()==0 ) return;
      DefaultMutableTreeNode r = new DefaultMutableTreeNode( new TreeObj(aladin,"root",null,"",""));
      ((DefaultTreeModel)getModel()).setRoot(r);
      root = r;
   }

   // Recupération d'un itérator sur tous les noeuds de l'arbre
   public Iterator<TreeObj> iterator() { return new TreeIterator(); }

   class TreeIterator implements Iterator<TreeObj> {
      private Enumeration e = root.preorderEnumeration();
      public boolean hasNext() { return e.hasMoreElements(); }
      public TreeObj next() { return (TreeObj) ((DefaultMutableTreeNode)e.nextElement()).getUserObject(); }
      public void remove() { }
   }


   /** "Peuplement" de l'arbre en fonction des enregistrements GLU recueillis */
   protected void populateTree(Enumeration e) {
      while( e.hasMoreElements() ) {
         TreeObj noeud = (TreeObj)e.nextElement();
         noeud.setHidden(false);
         createTreeBranch((DefaultTreeModel)getModel(),root,noeud,0);
      }
      validate();
      defaultExpand();
   }
   
//   /** Supprime les feuilles et branches hidden */
//   protected void elagueHidden() {
//      elagueHidden(root, (DefaultTreeModel)getModel() );
//   }
   
//   private void elagueHidden(DefaultMutableTreeNode node, DefaultTreeModel model ) {
//       
//      for( int i=node.getChildCount()-1; i>=0; i-- ) {
//         elagueHidden( (DefaultMutableTreeNode)node.getChildAt(i), model );
//      }
//
//      if( node.isLeaf() ) {
//         TreeObj fils = (TreeObj) node.getUserObject();
//         if( fils.isHidden() && !node.equals(root)) {
//            fils.treeIndex = node.getParent().getIndex(node);
//            model.removeNodeFromParent(node);
//         }
//         return;
//      }
//   }

   /** Reset */
   public void reset() {
      for( TreeObj n : this ) n.setCheckBox(false);
      validate();
   }

   /** Interrogation */
   public void submit() {
      boolean ok=false;
      for( TreeObj n : this ) {
         if( !n.isCheckBoxSelected() ) continue;
         submit(n);
         ok=true;
      }
      if( !ok ) warning();
      reset();
   }

   /** Appelé en cas de problème lors du submit => à surcharger */
   protected void warning() {}
   

   
   protected void populateFlagIn() { setInTree( getRoot() ); }

   /** Activation ou non des branches de l'arbre en fonction de l'activation des feuilles */
   protected boolean setInTree(DefaultMutableTreeNode node) {
      TreeObj gSky = (TreeObj) node.getUserObject();
      if( node.isLeaf() )  return gSky.isIn();

      boolean rep=false;
      DefaultMutableTreeNode subNode = null;
      Enumeration e = node.children();
      while( e.hasMoreElements() ) {
         subNode = (DefaultMutableTreeNode) e.nextElement();
         if( setInTree(subNode) ) rep=true;
      }
      gSky.setIn(rep);
      return rep;
   }

   /** Activation ou non des branches de l'arbre en fonction de l'activation des feuilles */
   protected boolean setHiddenTree(DefaultMutableTreeNode node) {
      TreeObj gSky = (TreeObj) node.getUserObject();
      if( node.isLeaf() )  return gSky.isHidden();

      boolean rep=true;
      DefaultMutableTreeNode subNode = null;
      Enumeration e = node.children();
      while( e.hasMoreElements() ) {
         subNode = (DefaultMutableTreeNode) e.nextElement();
         if( !setHiddenTree(subNode) ) rep=false;
      }
      gSky.setHidden(rep); 
      return rep;
   }

   /** Met à jour les couleurs des widgets avant de les tracer => à surcharger */
   protected void updateColor() {}

   private void submit(TreeObj n) { n.submit(); }

   /** Procédure récursive pour la construction de l'arbre.
    * @param node Noeud courant (au sens JTtree)
    * @param noeud Nouvelle feuille à insérer
    * @param opos indice de traitement dans la chaine noeud.path => permet de connaître la profondeur de la hiérarchie
    *             ex:  Nebulae/PN
    *                          ^   <= valeur de opos
    */
   protected void createTreeBranch(DefaultTreeModel model, DefaultMutableTreeNode node, TreeObj noeud, int opos) {
      int pos;

      // On découpe par "/" mais sans prendre en compte "\/"
      int index=opos;
      do  {
         pos=noeud.path.indexOf('/',index);
         index=pos;
         if( pos>1 && noeud.path.charAt(pos-1)=='\\') index++;
         else index=-1;
      } while( index!=-1 );

      String label = pos<0 ? noeud.path.substring(opos) : noeud.path.substring(opos,pos);
 
      try {
         DefaultMutableTreeNode subNode = null;
         Enumeration e = node.children();
         while( e.hasMoreElements() ) {
            subNode = (DefaultMutableTreeNode) e.nextElement();
            TreeObj fils = (TreeObj) subNode.getUserObject();
            if( label.equals(fils.label) ) break;
            subNode=null;
         }

         if( subNode==null ) {
            subNode = new DefaultMutableTreeNode( pos!=-1? new TreeObj(aladin,"",null,label,"") : noeud );
//            node.add(subNode);
//            int i = ((TreeObj)subNode.getUserObject()).treeIndex;
            int i=-1;
            int n = node.getChildCount();
            if( i==-1 || i>n ) i=n;
            model.insertNodeInto(subNode, node, i);
         }
         if( pos!=-1 ) createTreeBranch(model, subNode, noeud, pos + 1);

      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   /** Signale que l'arbre a été modifié */
   public void fireTreeChanged() {
      ((DefaultTreeModel)getModel()).reload();
   }

   /** Suppression d'une feuille (désignée par son ID) et éventuellement de sa branche si c'était la dernière  */
   protected boolean removeTreeBranch(DefaultMutableTreeNode node, String id ) {
      DefaultMutableTreeNode subNode = null;
      boolean rep=false;
      Enumeration e = node.children();
      while( e.hasMoreElements() ) {
         subNode = (DefaultMutableTreeNode) e.nextElement();
         TreeObj fils = (TreeObj) subNode.getUserObject();
         String idFils = fils.getID();
         if( idFils!=null && idFils.equals(id) ) { node.remove(subNode); rep=true;  break; }
         if( removeTreeBranch(subNode,id) && node.getChildCount()==0 ) { node.remove(subNode); rep=true; break; }
      }
      return rep;
   }

   /** Préparation de l'arbre afin qu'il "pré-ouvre" les branches terminales */
   protected void defaultExpand() {
      expandPath(new TreePath(root));
//      if( true ) return;
//      Enumeration e = root.preorderEnumeration();
//      while( e.hasMoreElements() ) {
//         DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
//         if( node.isLeaf() ) collapsePath(new TreePath(node));
//      }
   }

   /** Classe pour l'édition d'un noeud de l'arbre */
   class NoeudRenderer implements TreeCellRenderer {
      DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
      Color selectionForeground, selectionBackground, textForeground, textBackground;

      NoeudRenderer() {
         selectionForeground = UIManager.getColor("Tree.selectionForeground");
         selectionBackground = UIManager.getColor("Tree.selectionBackground");
//         selectionBackground = aladin.getBackground();
         textForeground = UIManager.getColor("Tree.textForeground");
         textBackground = UIManager.getColor("Tree.textBackground");
//         textBackground = aladin.getBackground();
         
//         nonLeafRenderer.setBackgroundNonSelectionColor( aladin.getBackground() );
      }

      public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeObj n = (TreeObj)node.getUserObject();
         
         if( !node.isLeaf() ) n.noCheckbox();

         //         System.out.println("getTreeCellRendererComponent ["+node.toString()+"] => "+n.isOk());

         if( n!=null && n.hasCheckBox() ) {
            if( n.isIn() ) n.checkbox.setForeground(Color.black);
            else n.checkbox.setForeground(Color.lightGray);

            JPanel panel = n.getPanel();
            if (selected) {
               panel.setForeground(selectionForeground);
               panel.setBackground(selectionBackground);
            } else {
               panel.setForeground(textForeground);
               panel.setBackground(textBackground);
            }
            return panel;
         }

         Component c = nonLeafRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
         if( n.isIn() ) c.setForeground( Color.black);
         else c.setForeground( Color.lightGray );

         return c;
      }
   }
   
   /** Classe pour la modification d'un noeud de l'arbre => à savoir checkbox */
   class NoeudEditor extends AbstractCellEditor implements TreeCellEditor {
      JTree tree;
      NoeudRenderer renderer = new NoeudRenderer();
      TreeObj n1 = null;

      public NoeudEditor(JTree tree) {
         this.tree = tree;
      }

      @Override
      public boolean isCellEditable(EventObject event) {
         if (event instanceof MouseEvent) {
            TreePath path = tree.getPathForLocation( ((MouseEvent)event).getX(), ((MouseEvent)event).getY());
            TreeObj noeud = (TreeObj)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            return noeud.hasCheckBox();
         }
         return false;
      }
      public Component getTreeCellEditorComponent(JTree tree, Object obj, boolean isSelected, boolean expanded, boolean leaf, int row){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeObj n = (TreeObj)node.getUserObject();
         n1 = n;
         if( n!=null && n.hasCheckBox() ) {
            if( n.isIn() ) n.checkbox.setForeground(Color.black);
            else n.checkbox.setForeground(Color.lightGray);
            return n.getPanel();
         }
         Component c = renderer.getTreeCellRendererComponent(tree, obj, true, expanded, leaf, row, true);
         if( n.isIn() ) c.setForeground( Color.black);
         else c.setForeground( Color.lightGray );
         return c;

      }
      public Object getCellEditorValue() {
         return n1;
      }
   }

   public void paint(Graphics g) {
      try {
         updateColor();
         super.paint(g);
      } catch( Exception e ) { }
   }


}

