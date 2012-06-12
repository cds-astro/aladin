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

import cds.aladin.Pcat.PlanObjetIterator;

/**
 * Formulaire d'interrogation sous la forme d'un arbre
 * @author Pierre Fernique [CDS]
 */
public abstract class ServerTree extends Server implements Iterable<TreeNode>  {
   protected String info,info1;
   protected DefaultMutableTreeNode root;
   protected JTree tree;

   /** Creation du formulaire d'interrogation par arbre. */
   protected ServerTree(Aladin aladin) {
      this.aladin = aladin;
      createChaine();
      init();

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      int y=10;

      // Le titre
      JPanel tp = new JPanel();
      Dimension d = makeTitle(tp,title);
      tp.setBackground(Aladin.BLUE);
      tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);

      // Indication
      JLabel l = new JLabel(info);
      l.setBounds(120,y,400, 20); y+=20;
      add(l);

      // Target ?
      y=makeTarget(y);

      // L'arbre
      tree = createTree();
      JScrollPane scrollTree = new JScrollPane(tree);
      scrollTree.setBounds(XTAB1,y,XWIDTH,217); y+=215;
      add(scrollTree);
      setMaxComp(scrollTree);

      // Indication
      if( info1!=null ) {
         l = new JLabel(info1);
         l.setFont(l.getFont().deriveFont(Font.ITALIC));
         l.setBounds(30,y,440, 20); y+=20;
         add(l);
      }
      
      // Ajout en fin de formulaire si nécessaire
      y = addTailPanel(y);

      modeCoo = COO|SIMBAD;
      modeRad = RADIUS;
   }
   
   /** A surcharger si on veut compléter la fin du formulaire (voir par exemple ServerAllsky) */
   protected int addTailPanel(int y) {return y; }

   protected int makeTarget(int y) { return y; }

   abstract protected void init();

   protected DefaultMutableTreeNode getRoot() {
      if( root==null ) root = new DefaultMutableTreeNode( new TreeNode(aladin,"root",null,"","") );
      return root;
   }

   /** Création de l'arbre */
   private JTree createTree() {
      root = getRoot();
      tree = new JTree(root);
      tree.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 0));

      tree.setRootVisible(false);
      tree.setShowsRootHandles(true);
      NoeudRenderer nr = new NoeudRenderer();
      tree.setCellRenderer(nr);
      tree.setCellEditor(new NoeudEditor(tree));
      tree.setEditable(true);

      return tree;
   }

   @Override
   public void show() {
      initTree();
      super.show();
   }

   abstract protected  void initTree();

   /** Nettoyage de l'arbre */
   protected void freeTree() {
      DefaultMutableTreeNode r = new DefaultMutableTreeNode( new TreeNode(aladin,"root",null,"",""));
      ((DefaultTreeModel)tree.getModel()).setRoot(r);
      root = r;
   }
   
   /** "Mise à jour" de l'arbre en fonction des enregistrements GLU recueillis */
   protected void updateTree(Enumeration e1) {
      
      ArrayList<TreeNode> v = new ArrayList();
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
   
   // Recupération d'un itérator sur tous les noeuds de l'arbre
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
   @Override
   protected void reset() {
      for( TreeNode n : this ) n.setCheckBox(false);
      tree.validate();
      repaint();
   }

   /** Interrogation */
   @Override
   public void submit() {
      boolean ok=false;
      for( TreeNode n : this ) {
         if( !n.isCheckBoxSelected() ) continue;
         submit(n);
         ok=true;
      }
//      Enumeration e = root.preorderEnumeration();
//      while( e.hasMoreElements() ) {
//         DefaultMutableTreeNode node = (DefaultMutableTreeNode)e.nextElement();
//         TreeNode n = (TreeNode) node.getUserObject();
//         if( !n.isCheckBoxSelected() ) continue;
//         submit(n);
//         ok=true;
//      }
      if( !ok ) aladin.warning(aladin.dialog,WNEEDCHECK,1);
      reset();
   }

   public void submit(TreeNode n) { n.submit(); }


   /** Procédure récursive pour la construction de l'arbre.
    * @param node Noeud courant (au sens JTtree)
    * @param noeud Nouvelle feuille à insérer
    * @param opos indice de traitement dans la chaine noeud.path => permet de connaître la profondeur de la hiérarchie
    *             ex:  Nebulae/PN
    *                          ^   <= valeur de opos
    */
   protected void createTreeBranch(DefaultMutableTreeNode node, TreeNode noeud, int opos) {
      int pos;
      String label = (pos=noeud.path.indexOf('/',opos))<0 ? noeud.path.substring(opos) : noeud.path.substring(opos,pos);
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
         } else createTreeBranch(subNode, noeud, pos + 1);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   /** Préparation de l'arbre afin qu'il "pré-ouvre" les branches terminales */
   protected void defaultExpand() {
      tree.expandPath(new TreePath(root));
      Enumeration e = root.preorderEnumeration();
      while( e.hasMoreElements() ) {
         DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
         if( node.isLeaf() ) tree.collapsePath(new TreePath(node));
      }
   }

   /** Classe pour l'édition d'un noeud de l'arbre */
   class NoeudRenderer implements TreeCellRenderer {
      DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
      Color selectionForeground, selectionBackground, textForeground, textBackground;

      NoeudRenderer() {
         selectionForeground = UIManager.getColor("Tree.selectionForeground");
         selectionBackground = UIManager.getColor("Tree.selectionBackground");
         textForeground = UIManager.getColor("Tree.textForeground");
         textBackground = UIManager.getColor("Tree.textBackground");
      }

      public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeNode n = (TreeNode)node.getUserObject();

         if( n.hasCheckBox() ) {
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
         else {
            return nonLeafRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
         }
      }
   }

   /** Classe pour la modification d'un noeud de l'arbre => à savoir checkbox */
   class NoeudEditor extends AbstractCellEditor implements TreeCellEditor {
      JTree tree;
      NoeudRenderer renderer = new NoeudRenderer();

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
         if( n.hasCheckBox() ) {
            return n.getPanel();
         }
         return renderer.getTreeCellRendererComponent(tree, obj, true, expanded, leaf, row, true);
      }
      public Object getCellEditorValue() {
         return null;
      }
   }

}
