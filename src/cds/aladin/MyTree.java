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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Iterator;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
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
public class MyTree extends JTree implements Iterable<TreeNodeBasic>  {
   protected String info,info1;
   protected DefaultMutableTreeNode root;
   private Aladin aladin;

   protected MyTree(Aladin aladin) {
      this.aladin = aladin;
      createTree();
   }

   protected DefaultMutableTreeNode getRoot() {
      if( root==null ) root = new DefaultMutableTreeNode( new TreeNodeBasic(aladin,"root",null,"","") );
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
      setLargeModel(true);
   }

   /** Nettoyage de l'arbre */
   protected void freeTree() {
      if( root!=null && root.getChildCount()==0 ) return;
      DefaultMutableTreeNode r = new DefaultMutableTreeNode( new TreeNodeBasic(aladin,"root",null,"",""));
      ((DefaultTreeModel)getModel()).setRoot(r);
      root = r;
   }

   // Recupération d'un itérator sur tous les noeuds de l'arbre
   public Iterator<TreeNodeBasic> iterator() { return new TreeIterator(); }

   class TreeIterator implements Iterator<TreeNodeBasic> {
      private Enumeration e = root.preorderEnumeration();
      public boolean hasNext() { return e.hasMoreElements(); }
      public TreeNodeBasic next() { return (TreeNodeBasic) ((DefaultMutableTreeNode)e.nextElement()).getUserObject(); }
      public void remove() { }
   }


   /** "Peuplement" de l'arbre en fonction des enregistrements GLU recueillis */
   protected void populateTree(Enumeration e) {
      while( e.hasMoreElements() ) {
         TreeNodeBasic noeud = (TreeNodeBasic)e.nextElement();
         noeud.setHidden(false);
         createTreeBranch((DefaultTreeModel)getModel(),root,noeud,0);
      }
      validate();
      defaultExpand();
   }
   
   /** Supprime les feuilles et branches hidden */
   protected void elagueHidden() { elagueHidden(root, (DefaultTreeModel)getModel() ); }
   private void elagueHidden(DefaultMutableTreeNode node, DefaultTreeModel model ) {
       
      for( int i=node.getChildCount()-1; i>=0; i-- ) {
         elagueHidden( (DefaultMutableTreeNode)node.getChildAt(i), model );
      }

      if( node.isLeaf() ) {
         TreeNodeBasic fils = (TreeNodeBasic) node.getUserObject();
         if( fils.isHidden() && !node.equals(root)) {
            fils.treeIndex = node.getParent().getIndex(node);
            model.removeNodeFromParent(node);
         }
         return;
      }
   }

   /** Supprime les feuilles et branches !isIn */
   protected void elagueOut() { elagueOut(root, (DefaultTreeModel)getModel() ); }
   private void elagueOut(DefaultMutableTreeNode node, DefaultTreeModel model ) {
       
      for( int i=node.getChildCount()-1; i>=0; i-- ) {
         elagueOut( (DefaultMutableTreeNode)node.getChildAt(i), model );
      }

      if( node.isLeaf() ) {
         TreeNodeBasic fils = (TreeNodeBasic) node.getUserObject();
         if( !fils.isIn() && !node.equals(root)) {
            fils.treeIndex = node.getParent().getIndex(node);
            model.removeNodeFromParent(node);
         }
         return;
      }
   }


   /** Reset */
   public void reset() {
      for( TreeNodeBasic n : this ) n.setCheckBox(false);
      validate();
   }

   /** Interrogation */
   public void submit() {
      boolean ok=false;
      for( TreeNodeBasic n : this ) {
         if( !n.isCheckBoxSelected() ) continue;
         submit(n);
         ok=true;
      }
      if( !ok ) warning();
      reset();
   }

   /** Appelé en cas de problème lors du submit => à surcharger */
   protected void warning() {}
   

   //      // EN ATTENDANT QUE CELA SOIT ENLEVE DU GLU
   //      protected void createTreeBranch(DefaultMutableTreeNode node, TreeNode noeud, int opos) {
   //         if( noeud.path.startsWith("Progressive catalog")) return;
   //         super.createTreeBranch(node,noeud, opos);
   //      }

   /** Activation ou non des branches de l'arbre en fonction de l'activation des feuilles */
   protected boolean setInTree(DefaultMutableTreeNode node) {
      TreeNodeBasic gSky = (TreeNodeBasic) node.getUserObject();
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
      TreeNodeBasic gSky = (TreeNodeBasic) node.getUserObject();
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

   private void submit(TreeNodeBasic n) { n.submit(); }

   /** Procédure récursive pour la construction de l'arbre.
    * @param node Noeud courant (au sens JTtree)
    * @param noeud Nouvelle feuille à insérer
    * @param opos indice de traitement dans la chaine noeud.path => permet de connaître la profondeur de la hiérarchie
    *             ex:  Nebulae/PN
    *                          ^   <= valeur de opos
    */
   protected void createTreeBranch(DefaultTreeModel model, DefaultMutableTreeNode node, TreeNodeBasic noeud, int opos) {
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
      ((TreeNodeBasic)node.getUserObject()).noCheckbox();

      try {
         DefaultMutableTreeNode subNode = null;
         Enumeration e = node.children();
         while( e.hasMoreElements() ) {
            subNode = (DefaultMutableTreeNode) e.nextElement();
            TreeNodeBasic fils = (TreeNodeBasic) subNode.getUserObject();
            if( label.equals(fils.label) ) break;
            subNode=null;
         }

         if( subNode==null ) {
            subNode = new DefaultMutableTreeNode( pos!=-1? new TreeNodeBasic(aladin,"",null,label,"") : noeud );
//            node.add(subNode);
            int i = ((TreeNodeBasic)subNode.getUserObject()).treeIndex;
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
         TreeNodeBasic fils = (TreeNodeBasic) subNode.getUserObject();
         String idFils = fils.getID();
         if( idFils!=null && idFils.equals(id) ) { node.remove(subNode); rep=true;  break; }
         if( removeTreeBranch(subNode,id) && node.getChildCount()==0 ) { node.remove(subNode); rep=true; break; }
      }
      return rep;
   }

   protected boolean isDefaultExpand() {
      TreePath rootTp = new TreePath(root);
      if (root.getChildCount() >= 0) {
         for (Enumeration e = root.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = rootTp.pathByAddingChild(n);
            if( !isCollapsedRec(path) ) return false;
         }
      }
      return true;
   }
   
   public boolean isCollapsedRec(TreePath parent) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
      if (node.getChildCount() >= 0) {
         for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(n);
            if( !isCollapsedRec(path) ) return false;
         }
      }
      return isCollapsed(parent);
   }
   
   protected void defaultExpand() {
      TreePath rootTp = new TreePath(root);
      if (root.getChildCount() >= 0) {
         for (Enumeration e = root.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = rootTp.pathByAddingChild(n);
            collapseAll(path);
         }
      }
      expandPath(rootTp);
   }
   
   private void collapseAll(TreePath parent) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
      if (node.getChildCount() >= 0) {
         for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(n);
            collapseAll(path);
         }
      }
      collapsePath(parent);
   }

   
   /** Ouverture de toutes les branches de l'arbre */
   protected void allExpand() { expandAll(new TreePath(root)); }
   private void expandAll(TreePath parent) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
      if (node.getChildCount() >= 0) {
         for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode n = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(n);
            expandAll(path);
         }
      }
      expandPath(parent);
   }
   
   ImageIcon cds = null,jaxa=null,esa=null,irap=null,xcatdb=null; 

   /** Classe pour l'édition d'un noeud de l'arbre */
   class NoeudRenderer implements TreeCellRenderer {
      DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
      Color selectionForeground, selectionBackground, textForeground, textBackground;

      NoeudRenderer() {
         selectionForeground = UIManager.getColor("Tree.selectionForeground");
//         selectionBackground = UIManager.getColor("Tree.selectionBackground");
         selectionBackground = aladin.getBackground();
         textForeground = UIManager.getColor("Tree.textForeground");
//         textBackground = UIManager.getColor("Tree.textBackground");
         textBackground = aladin.getBackground();
         
         nonLeafRenderer.setBackgroundNonSelectionColor( aladin.getBackground() );
      }

      public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeNodeBasic n = (TreeNodeBasic)node.getUserObject();

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
         if( n.isInStack() ) c.setForeground( Color.green );
         else if( !n.isIn() ) c.setForeground(Aladin.ORANGE); //Color.orange);
         else {
            if( node.isLeaf() ) c.setForeground( Aladin.GREEN );
            else c.setForeground(Color.black);
         }
         
         // Dans les cas des hips locaux, le MocServer ne sert à rien => couleur noire
         if( n instanceof TreeNodeHips && ((TreeNodeHips)n).isLocal() ) c.setForeground(Color.black);
         
         if( n instanceof TreeNodeHips && ((TreeNodeHips)n).internalId!=null ) {
            if( ((TreeNodeHips)n).internalId.startsWith("CDS/") ) {
               if( cds==null ) cds = new ImageIcon(aladin.getImagette("cds.png"));
               nonLeafRenderer.setIcon( cds );
            } else if( ((TreeNodeHips)n).internalId.startsWith("ESAVO/") ) {
               if( esa==null ) esa = new ImageIcon(aladin.getImagette("esa.png"));
               nonLeafRenderer.setIcon( esa );
            } else if( ((TreeNodeHips)n).internalId.startsWith("JAXA/") ) {
               if( jaxa==null ) jaxa = new ImageIcon(aladin.getImagette("jaxa.png"));
               nonLeafRenderer.setIcon( jaxa );
            } else if( ((TreeNodeHips)n).internalId.startsWith("ov-gso/") ) {
               if( irap==null ) irap = new ImageIcon(aladin.getImagette("irap.png"));
               nonLeafRenderer.setIcon( irap );
            } else if( ((TreeNodeHips)n).internalId.startsWith("xcatdb/") ) {
               if( xcatdb==null ) xcatdb = new ImageIcon(aladin.getImagette("xcatdb.png"));
               nonLeafRenderer.setIcon( xcatdb );
           }
         }

         return c;
      }
   }
   
   /** Classe pour la modification d'un noeud de l'arbre => à savoir checkbox */
   class NoeudEditor extends AbstractCellEditor implements TreeCellEditor {
      JTree tree;
      NoeudRenderer renderer = new NoeudRenderer();
      TreeNodeBasic n1 = null;

      public NoeudEditor(JTree tree) {
         this.tree = tree;
      }

      @Override
      public boolean isCellEditable(EventObject event) {
         if (event instanceof MouseEvent) {
            TreePath path = tree.getPathForLocation( ((MouseEvent)event).getX(), ((MouseEvent)event).getY());
            TreeNodeBasic noeud = (TreeNodeBasic)((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
            return noeud.hasCheckBox();
         }
         return false;
      }
      public Component getTreeCellEditorComponent(JTree tree, Object obj, boolean isSelected, boolean expanded, boolean leaf, int row){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeNodeBasic n = (TreeNodeBasic)node.getUserObject();
         n1 = n;
         if( n!=null &&  n.hasCheckBox() ) {
            if( n.isIn() ) n.checkbox.setForeground(Color.black);
            else n.checkbox.setForeground(Color.lightGray);
            return n.getPanel();
         }
         Component c = renderer.getTreeCellRendererComponent(tree, obj, true, expanded, leaf, row, true);
         if( n.isIn() ) c.setForeground( Color.black);
         else c.setForeground( Color.lightGray );
         c.setBackground( Color.red );
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

