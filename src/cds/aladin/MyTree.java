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
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.HashMap;
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
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import cds.tools.Util;

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
   protected void createTree() {
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
      if( true ) return;
//      while( e.hasMoreElements() ) {
//         TreeNodeBasic noeud = (TreeNodeBasic)e.nextElement();
//         noeud.setHidden(false);
//         createTreeBranch((DefaultTreeModel)getModel(),root,noeud,0);
//      }
//      validate();
   }
   

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

   /** Activation ou non des branches de l'arbre en fonction de l'activation des feuilles */
   protected int populateFlagIn() { return populateFlagIn(root); }
   
   private int populateFlagIn(DefaultMutableTreeNode node) {
      TreeObj hips = (TreeObj) node.getUserObject();
      if( node.isLeaf() )  return hips.getIsIn();

      int rep=0;
      DefaultMutableTreeNode subNode = null;
      Enumeration e = node.children();
      while( e.hasMoreElements() ) {
         subNode = (DefaultMutableTreeNode) e.nextElement();
         if( populateFlagIn(subNode)==1 ) rep=1;
      }
      hips.setIn(rep);
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

   private void submit(TreeObj n) { n.loadHips(); }
   
   
   private DefaultMutableTreeNode lastParent=null;
   
   protected void setExpandedState(TreePath path, boolean state) {
      if( !lockExpand ) {
        super.setExpandedState(path, state);
      }
    }
   
   transient private boolean lockExpand = true;
   protected void lockExpand(boolean flag) { lockExpand=flag; }

   
   protected void createTreeBranch(TreeObj noeud) {
      DefaultTreeModel model = (DefaultTreeModel)getModel();
      if( createLeafWithLastParent( model, noeud) ) return;
      
      DefaultMutableTreeNode nodeUp [] = new DefaultMutableTreeNode[1];
      int index [] = new int[1];
      lastParent = createTreeBranch( model, root, noeud, 0, nodeUp, index);
      
      if( nodeUp[0]!=null ) model.nodesWereInserted( nodeUp[0], index);
   }
   
   private boolean createLeafWithLastParent(DefaultTreeModel model, TreeObj noeud) {
      if( lastParent==null ) return false;
      int pos = noeud.path.lastIndexOf('/');
      String path = noeud.path.substring(0, pos);
      
      TreeObj pere = (TreeObj) lastParent.getUserObject();
      if( !path.equals(pere.path) ) return false;
      
      int i = noeud.treeIndex;
      int n = lastParent.getChildCount();
      if( i==-1 || i>n ) i=n;
      lastParent.insert( new DefaultMutableTreeNode(noeud), i);
//      model.nodeChanged(lastParent);
//      model.insertNodeInto(lastParent, new DefaultMutableTreeNode(noeud), i);
      
      model.nodesWereInserted( lastParent, new int[]{i});
      
      return true;
   }

   /** Procédure récursive pour la construction de l'arbre.
    * @param node Noeud courant (au sens JTtree)
    * @param noeud Nouvelle feuille à insérer
    * @param opos indice de traitement dans la chaine noeud.path => permet de connaître la profondeur de la hiérarchie
    *             ex:  Nebulae/PN
    *                          ^   <= valeur de opos
    */
   protected DefaultMutableTreeNode createTreeBranch(DefaultTreeModel model, DefaultMutableTreeNode node, 
         TreeObj noeud, int opos, DefaultMutableTreeNode nodeUp [], int index []) {
      int pos;

      // On découpe par "/" mais sans prendre en compte "\/"
      int offset=opos;
      do  {
         pos=noeud.path.indexOf('/',offset);
         offset=pos;
         if( pos>1 && noeud.path.charAt(pos-1)=='\\') offset++;
         else offset=-1;
      } while( offset!=-1 );

      String label = pos<0 ? noeud.path.substring(opos) : noeud.path.substring(opos,pos);
      String path = pos<0 ? noeud.path : noeud.path.substring(0,pos);
      ((TreeObj)node.getUserObject()).noCheckbox();

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
            subNode = new DefaultMutableTreeNode( pos!=-1? new TreeObj(aladin,"",null,label,path) : noeud );
            int i = ((TreeObj)subNode.getUserObject()).treeIndex;
            int n = node.getChildCount();
            if( i==-1 || i>n ) i=n;
            node.insert(subNode,i);
            if( nodeUp[0]==null ) { nodeUp[0]=node; index[0]=i; }
//            model.nodeChanged(node);
//            model.insertNodeInto(subNode, node, i);
         }
         if( pos!=-1 ) return createTreeBranch(model, subNode, noeud, pos + 1, nodeUp, index);
         return node;

      } catch( Exception e ) {
         e.printStackTrace();
      }
      return null;
   }
   
   public static TreePath getPath(TreeNode treeNode) {
      ArrayList<Object> nodes = new ArrayList<Object>();
      if (treeNode != null) {
        nodes.add(treeNode);
        treeNode = treeNode.getParent();
        while (treeNode != null) {
          nodes.add(0, treeNode);
          treeNode = treeNode.getParent();
        }
      }
      return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
   }


   /** Signale que l'arbre a été modifié */
   public void fireTreeChanged() {
      ((DefaultTreeModel)getModel()).reload();
   }
   
   /** Suppression d'un noeud, et de la branche morte si nécessaire
    * @param obj le userObj associé au noeud qu'il faut supprimer
    */
   protected void removeTreeBranch(TreeObj obj) {
      
      // Il faut trouver le node correspondant au userobj
      boolean trouve = false;
      DefaultMutableTreeNode node=null;
      Enumeration e = root.preorderEnumeration();
      while( e.hasMoreElements() ) {
         node = (DefaultMutableTreeNode) e.nextElement();
         if( obj == (TreeObj) node.getUserObject() ) { trouve=true; break; }
      }
      if( !trouve ) return;
      
      removeTreeBranch((DefaultTreeModel) getModel(), node);
   }
   
   /** Suppression d'un node, et de la branche morte si nécessaire */
   private void removeTreeBranch( DefaultTreeModel model, DefaultMutableTreeNode node ) {
      DefaultMutableTreeNode fils=null;
      int index = -1;
      while( node!=root && node.isLeaf() ) {
         DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
         TreeObj hips = (TreeObj) node.getUserObject();
         index = hips.treeIndex = parent.getIndex(node);
         parent.remove(index);
         //        model.removeNodeFromParent(node);
         fils=node;
         node = parent;
      }
      if( fils!=null ) {
         model.nodesWereRemoved(node, new int[]{index}, new Object[]{fils} );
      }
   }

//   /** Suppression d'une feuille (désignée par son ID) et éventuellement de sa branche si c'était la dernière  */
//   protected boolean removeTreeBranch(DefaultMutableTreeNode node, String id ) {
//      DefaultMutableTreeNode subNode = null;
//      boolean rep=false;
//      Enumeration e = node.children();
//      while( e.hasMoreElements() ) {
//         subNode = (DefaultMutableTreeNode) e.nextElement();
//         TreeNodeBasic fils = (TreeNodeBasic) subNode.getUserObject();
//         String idFils = fils.getID();
//         if( idFils!=null && idFils.equals(id) ) { node.remove(subNode); rep=true;  break; }
//         if( removeTreeBranch(subNode,id) && node.getChildCount()==0 ) { node.remove(subNode); rep=true; break; }
//      }
//      return rep;
//   }

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
   
   
   private HashMap<String, ImageIcon> mapIcon = null;
   
   private void initMapIcon() {
      Image img;
      Color c = Color.yellow;
      
      try {
         mapIcon = new HashMap<String, ImageIcon>();
         img = aladin.getImagette("cds.png");
         mapIcon.put("CDS",new ImageIcon(img));
         mapIcon.put("CDS/color",new ImageIcon(setIconTag(img,c)));
         img = aladin.getImagette("esa.png");
         mapIcon.put("ESAVO",new ImageIcon(img));
         mapIcon.put("ESAVO/color",new ImageIcon(setIconTag(img,c)));
         img = aladin.getImagette("jaxa.png");
         mapIcon.put("JAXA",new ImageIcon(img));
         mapIcon.put("JAXA/color",new ImageIcon(setIconTag(img,c)));
         img = aladin.getImagette("irap.png");
         mapIcon.put("ov-gso",new ImageIcon(img));
         mapIcon.put("ov-gso/color",new ImageIcon(setIconTag(img,c)));
         img = aladin.getImagette("xcatdb.png");
         mapIcon.put("xcatdb",new ImageIcon(img));
         mapIcon.put("xcatdb/color",new ImageIcon(setIconTag(img,c)));
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   private ImageIcon getIcon(String id, int mode) {
      if( id==null ) return null;
      if( mapIcon==null ) initMapIcon();
      
      int i = id.indexOf('/');
      if( i==-1 ) i=id.length();
      String key = id.substring(0,i) + (mode==1?"/color":mode==2?"/fits":"");
      return mapIcon.get(key);
   }
   
   private Color r = new Color(255,100,100);
   private Color v = new Color(50,255,50);
   private Color b = new Color(100,100,255);
   
   private Image setIconTag(Image img,Color color) {
      int w = img.getWidth(aladin)+14;
//      int h =  Math.max(17,img.getHeight(aladin));
      int h = img.getHeight(aladin);
      BufferedImage img2 = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
      Graphics g = img2.getGraphics();
      g.drawImage(img,0,0,aladin);
      
      int x = w-8;
      int y = h/2;

      g.setColor(r);
      Util.fillCircle5(g, x, y-3);
//      g.setColor(Color.black);
//      Util.drawCircle5(g, x, y-3);

      g.setColor(v);
      Util.fillCircle5(g, x, y+2);
//      g.setColor(Color.black);
//      Util.drawCircle5(g, x, y+2);

      g.setColor(b);
      Util.fillCircle5(g, x+4, y);
//      g.setColor(Color.black);
//      Util.drawCircle5(g, x+4, y);
      
      return img2;
   }

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
         TreeObj n = (TreeObj)node.getUserObject();

         //         System.out.println("getTreeCellRendererComponent ["+node.toString()+"] => "+n.isOk());
         
         if( n!=null && n.hasCheckBox() ) {
            if( n.getIsIn()==1 ) n.checkbox.setForeground(Color.black);
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
         else if( n.getIsIn()!=1 ) c.setForeground(Aladin.ORANGE); //Color.orange);
         else {
            if( node.isLeaf() ) c.setForeground( Aladin.GREEN );
            else c.setForeground(Color.black);
         }
         
         // Dans les cas des hips locaux, le MocServer ne sert à rien => couleur noire
         if( n instanceof TreeObjHips ) {
            TreeObjHips hips = (TreeObjHips)n;
//            if( hips.isLocal() )  c.setForeground(Color.black);
            ImageIcon icon = getIcon(hips.internalId,hips.isColored() ? 1 : 0);
            if( icon!=null ) nonLeafRenderer.setIcon( icon );
         }

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
         if( n!=null &&  n.hasCheckBox() ) {
            if( n.getIsIn()==1 ) n.checkbox.setForeground(Color.black);
            else n.checkbox.setForeground(Color.lightGray);
            return n.getPanel();
         }
         Component c = renderer.getTreeCellRendererComponent(tree, obj, true, expanded, leaf, row, true);
         if( n.getIsIn()==1 ) c.setForeground( Color.black);
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

