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

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;

/**
 * Gestion d'un arbre dynamique
 * @author Pierre Fernique [CDS]
 */
public class MyTree extends JTree implements Iterable<TreeNode>  {
   protected String info,info1;
   protected DefaultMutableTreeNode root;
   private Aladin aladin;

   protected MyTree(Aladin aladin) {
      this.aladin = aladin;
      createTree();
   }

   protected DefaultMutableTreeNode getRoot() {
      if( root==null ) root = new DefaultMutableTreeNode( new TreeNode(aladin,"root",null,"","") );
      return root;
   }

   /** Cr�ation de l'arbre */
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
      DefaultMutableTreeNode r = new DefaultMutableTreeNode( new TreeNode(aladin,"root",null,"",""));
      ((DefaultTreeModel)getModel()).setRoot(r);
      root = r;
   }

   /** "Mise � jour" de l'arbre en fonction des enregistrements GLU recueillis */
   synchronized protected void updateTree(Enumeration e1) {

      ArrayList<TreeNode> v = new ArrayList<TreeNode>();
      for( TreeNode n : this ) v.add(n);
      freeTree();

      while( e1.hasMoreElements() ) {
         TreeNode noeud = (TreeNode)e1.nextElement();
         int i = v.indexOf(noeud);
         if( i>=0 ) {
            TreeNode oldNoeud = v.get(i);
            noeud.setCheckBox( oldNoeud.isCheckBoxSelected() );
         }
         createTreeBranch(root,noeud,0);
      }
      defaultExpand();
   }

   // Recup�ration d'un it�rator sur tous les noeuds de l'arbre
   public Iterator<TreeNode> iterator() { return new TreeIterator(); }

   class TreeIterator implements Iterator<TreeNode> {
      private Enumeration e = root.preorderEnumeration();
      public boolean hasNext() { return e.hasMoreElements(); }
      public TreeNode next() { return (TreeNode) ((DefaultMutableTreeNode)e.nextElement()).getUserObject(); }
      public void remove() { }
   }


   /** "Peuplement" de l'arbre en fonction des enregistrements GLU recueillis */
   protected void populateTree(Enumeration e) {
      while( e.hasMoreElements() ) {
         TreeNode noeud = (TreeNode)e.nextElement();
         createTreeBranch(root,noeud,0);
      }
      defaultExpand();
   }

   /** Reset */
   public void reset() {
      for( TreeNode n : this ) n.setCheckBox(false);
      validate();
   }

   /** Interrogation */
   public void submit() {
      boolean ok=false;
      for( TreeNode n : this ) {
         if( !n.isCheckBoxSelected() ) continue;
         submit(n);
         ok=true;
      }
      if( !ok ) warning();
      reset();
   }

   /** Appel� en cas de probl�me lors du submit => � surcharger */
   protected void warning() {}

   /** Met � jour les couleurs des widgets avant de les tracer => � surcharger */
   protected void updateColor() {}

   private void submit(TreeNode n) { n.submit(); }

   /** Proc�dure r�cursive pour la construction de l'arbre.
    * @param node Noeud courant (au sens JTtree)
    * @param noeud Nouvelle feuille � ins�rer
    * @param opos indice de traitement dans la chaine noeud.path => permet de conna�tre la profondeur de la hi�rarchie
    *             ex:  Nebulae/PN
    *                          ^   <= valeur de opos
    */
   protected void createTreeBranch(DefaultMutableTreeNode node, TreeNode noeud, int opos) {
      int pos;

      // On d�coupe par "/" mais sans prendre en compte "\/"
      int index=opos;
      do  {
         pos=noeud.path.indexOf('/',index);
         index=pos;
         if( pos>1 && noeud.path.charAt(pos-1)=='\\') index++;
         else index=-1;
      } while( index!=-1 );

      String label = pos<0 ? noeud.path.substring(opos) : noeud.path.substring(opos,pos);
      ((TreeNode)node.getUserObject()).noCheckbox();

      try {
         DefaultMutableTreeNode subNode = null;
         Enumeration e = node.children();
         while( e.hasMoreElements() ) {
            subNode = (DefaultMutableTreeNode) e.nextElement();
            TreeNode fils = (TreeNode) subNode.getUserObject();
            if( label.equals(fils.label) ) break;
            subNode=null;
         }

         if( subNode==null ) {
            subNode = new DefaultMutableTreeNode( pos!=-1? new TreeNode(aladin,"",null,label,"") : noeud );
            node.add(subNode);
            if( pos!=-1 ) createTreeBranch(subNode, noeud, pos + 1);
         } else if( pos!=-1 ) createTreeBranch(subNode, noeud, pos + 1);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   /** Signale que l'arbre a �t� modifi� */
   public void fireTreeChanged() {
      ((DefaultTreeModel)getModel()).reload();
   }

   /** Suppression d'une feuille (d�sign�e par son ID) et �ventuellement de sa branche si c'�tait la derni�re  */
   protected boolean removeTreeBranch(DefaultMutableTreeNode node, String id ) {
      DefaultMutableTreeNode subNode = null;
      boolean rep=false;
      Enumeration e = node.children();
      while( e.hasMoreElements() ) {
         subNode = (DefaultMutableTreeNode) e.nextElement();
         TreeNode fils = (TreeNode) subNode.getUserObject();
         String idFils = fils.getID();
         if( idFils!=null && idFils.equals(id) ) { node.remove(subNode); rep=true;  break; }
         if( removeTreeBranch(subNode,id) && node.getChildCount()==0 ) { node.remove(subNode); rep=true; break; }
      }
      return rep;
   }

   /** Suppression d'une branche (d�sign�e par son label) */
   //   protected boolean removeTreeTrunk(DefaultMutableTreeNode node, String label ) {
   //      DefaultMutableTreeNode subNode = null;
   //      boolean rep=false;
   //      Enumeration e = node.children();
   //      while( e.hasMoreElements() ) {
   //         subNode = (DefaultMutableTreeNode) e.nextElement();
   //         TreeNode fils = (TreeNode) subNode.getUserObject();
   //         if( fils.label!=null && fils.label.equals(label) ) { node.remove(subNode); rep=true;  break; }
   //      }
   //      return rep;
   //   }

   /** Pr�paration de l'arbre afin qu'il "pr�-ouvre" les branches terminales */
   protected void defaultExpand() {
      expandPath(new TreePath(root));
      Enumeration e = root.preorderEnumeration();
      while( e.hasMoreElements() ) {
         DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
         if( node.isLeaf() ) collapsePath(new TreePath(node));
      }
   }

   /** Classe pour l'�dition d'un noeud de l'arbre */
   class NoeudRenderer implements TreeCellRenderer {
      DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
      Color selectionForeground, selectionBackground, textForeground, textBackground;

      NoeudRenderer() {
         selectionForeground = UIManager.getColor("Tree.selectionForeground");
         selectionBackground = UIManager.getColor("Tree.selectionBackground");
         textForeground = UIManager.getColor("Tree.textForeground");
         textBackground = UIManager.getColor("Tree.textBackground");
      }

      public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeNode n = (TreeNode)node.getUserObject();

         //         System.out.println("getTreeCellRendererComponent ["+node.toString()+"] => "+n.isOk());

         if( n!=null && n.hasCheckBox() ) {
            if( n.isOk() ) n.checkbox.setForeground(Color.black);
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
         if( n.isOk() ) c.setForeground( Color.black);
         else c.setForeground( Color.lightGray );
         return c;
      }
   }
   
   /** Classe pour la modification d'un noeud de l'arbre => � savoir checkbox */
   class NoeudEditor extends AbstractCellEditor implements TreeCellEditor {
      JTree tree;
      NoeudRenderer renderer = new NoeudRenderer();
      TreeNode n1 = null;

      public NoeudEditor(JTree tree) {
         this.tree = tree;
      }

      @Override
      public boolean isCellEditable(EventObject event) {
         if (event instanceof MouseEvent) {
            TreePath path = tree.getPathForLocation( ((MouseEvent)event).getX(), ((MouseEvent)event).getY());
            TreeNode noeud = (TreeNode)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            return noeud.hasCheckBox();
         }
         return false;
      }
      public Component getTreeCellEditorComponent(JTree tree, Object obj, boolean isSelected, boolean expanded, boolean leaf, int row){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeNode n = (TreeNode)node.getUserObject();
         n1 = n;
         if( n!=null &&  n.hasCheckBox() ) {
            if( n.isOk() ) n.checkbox.setForeground(Color.black);
            else n.checkbox.setForeground(Color.lightGray);
            return n.getPanel();
         }
         Component c = renderer.getTreeCellRendererComponent(tree, obj, true, expanded, leaf, row, true);
         if( n.isOk() ) c.setForeground( Color.black);
         else c.setForeground( Color.lightGray );
         return c;

      }
      public Object getCellEditorValue() {
         return n1;
      }
   }

   public void paint(Graphics g) {
      updateColor();
      super.paint(g);
   }


}

