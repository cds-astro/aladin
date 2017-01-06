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
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import cds.tools.Util;

/**
 * Gestion d'un arbre dynamique
 * @author Pierre Fernique [CDS]
 * @version 2.0 Janvier 2017 - reprise compl�te � partir de MyTree
 */
public class RegTree extends JTree {
   protected DefaultMutableTreeNode root;
   private Aladin aladin;

   protected RegTree(Aladin aladin) {
      this.aladin = aladin;
      createTree();
   }

   protected DefaultMutableTreeNode getRoot() {
      if( root==null ) root = new DefaultMutableTreeNode( new TreeObj(aladin,"root",null,"","") );
      return root;
   }

   /** Cr�ation de l'arbre */
   protected void createTree() {
      root = getRoot();
      ((DefaultTreeModel)getModel()).setRoot(root);
      setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 0));

      setRootVisible(false);
      setShowsRootHandles(true);
      NoeudRenderer nr = new NoeudRenderer();
      setCellRenderer(nr);
      setLargeModel(true);
   }

   /** Colorations (noire, verte ou orange) des branches de l'arbre en fonction de l'�tat des feuilles */
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
         else if( rep==0 && (isIn==-1 || isIn==1) ) rep=isIn;
         else if( rep==1 && (isIn==-1 || isIn==0) ) rep=-1;
      }
      treeObj.setIn(rep);
      return rep;
   }
   
   /** Comptage de la descendance de chaque branche (nombre de noeuds terminaux d'une branche)
    * M�morisation dans TreeObj, soit en tant que r�f�rence (flagRef=true), soit
    * en tant que d�compte courant */
   protected int countDescendance(boolean flagRef) { return countDescendance(root,flagRef); }
   private int countDescendance(DefaultMutableTreeNode node,boolean flagRef) {
      TreeObj treeObj = (TreeObj) node.getUserObject();
      if( node.isLeaf() )  return 1;

      int n=0;
      Enumeration e = node.children();
      while( e.hasMoreElements() ) {
         DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) e.nextElement();
         n += countDescendance( subNode,flagRef );
      }
      if( flagRef ) treeObj.nbRefDescendance = n;
      else treeObj.nbDescendance = n;
      return n;
   }

   // Dernier noeud parent ayant eu une instertion => permet �ventuellement un ajout ult�rieur plus rapide
   private DefaultMutableTreeNode lastParentNode=null;
   
   /** Surchage pour bloquer temporairement le d�veloppement/r�duction des branches
    * de l'arbre => controler par isLockExpand */
   protected void setExpandedState(TreePath path, boolean state) {
      if( isLockExpand() ) return;
      super.setExpandedState(path, state);
    }
   
   transient private boolean lockExpand = true;
   private Object lock = new Object();
   protected void setLockExpand(boolean flag) { synchronized( lock ) { lockExpand=flag; } }
   private boolean isLockExpand() { synchronized( lock ) { return lockExpand; } }
   
   /** Insertion d'un noeud, �ventuellement avec les diff�rents �l�ments de sa branche
    * si ceux-ci n'existent pas encore. Conserve le noeud parent de l'insertion
    * dans lastParentNode afin d'acc�lerer une �ventuelle insertion ult�rieure au m�me endroit
    * @param treeObj Le nouveau noeud � ins�rer
    */
   protected void createTreeBranch(TreeObj treeObj) {
      DefaultTreeModel model = (DefaultTreeModel)getModel();
      
      // Cr�ation imm�diate car c'est le m�me parent que la pr�c�dente insertion
      if( createLeafWithLastParent( model, treeObj) ) return;
      
      // Cr�ation r�cursive (�ventuellement pour la branche)
      DefaultMutableTreeNode nodeUp [] = new DefaultMutableTreeNode[1];
      int index [] = new int[1];
      lastParentNode = createTreeBranch( model, root, treeObj, 0, nodeUp, index);
      
      // Indication aux listeners du mod�le qu'une branche a �t� ins�r�
      if( nodeUp[0]!=null ) model.nodesWereInserted( nodeUp[0], index);
   }
   
   /** M�thode interne - Tentative d'insertion d'un noeud sur le parent de la derni�re insertion. Retourne true
    * si l'insertion est effectivement possible, false sinon */
   private boolean createLeafWithLastParent(DefaultTreeModel model, TreeObj treeObj) {
      if( lastParentNode==null ) return false;
      int pos = treeObj.path.lastIndexOf('/');
      String path = treeObj.path.substring(0, pos);
      
      TreeObj pere = (TreeObj) lastParentNode.getUserObject();
      if( !path.equals(pere.path) ) return false;
      
      int i = treeObj.treeIndex;
      int n = lastParentNode.getChildCount();
      if( i==-1 || i>n ) i=n;
      lastParentNode.insert( new DefaultMutableTreeNode(treeObj), i);
      
      model.nodesWereInserted( lastParentNode, new int[]{i});
      
      return true;
   }

   /** M�thode interne - Insertion r�cursive d'un noeud en fonction du "path" du noeud � ins�rer.
    * Cr�ation �venutelle des noeuds des branches si ceux-ci n'existente pas encore
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
            if( pos==-1 ) subNode = new DefaultMutableTreeNode( treeObj );
            
            // Branche interm�diaire ? d�j� connue ou non ?
            else {
               TreeObj obj = retrieveOldBranch(path);
               if( obj==null ) obj = new TreeObj(aladin,"",null,label,path);
               subNode = new DefaultMutableTreeNode( obj );
            }
            int i = ((TreeObj)subNode.getUserObject()).treeIndex;
            int n = parent.getChildCount();
            if( i==-1 || i>n ) i=n;
            parent.insert(subNode,i);
            
            // M�morisation du parent et de l'indice du fils pour la 1�re greffe op�r�e
            if( parentUp[0]==null ) { parentUp[0]=parent; childIndex[0]=i; }
         }
         
         // On n'est pas au bout du path, il faut donc continuer r�cursivement
         // (en fait, une boucle serait plus adapt�e, mais comme on ne descend jamais
         // bien profond, �a ne va pas g�rer
         if( pos!=-1 ) return createTreeBranch(model, subNode, treeObj, pos + 1, parentUp, childIndex);
         
         // Retourne le noeud parent
         return parent;

      } catch( Exception e ) {
         e.printStackTrace();
      }
      return null;
   }
   
   // Permet la m�morisation des vielles branches lors
   // d'un �lagage afin de pouvoir les r�ins�rer au bon endroit le cas �ch�ant
   private HashMap<String, TreeObj> memoPathIndex = null;
   
   /** Retrouve la branche qui aurait �t� supprim�e pr�c�demment afin de l'ins�rer au bon endroit */
   private TreeObj retrieveOldBranch(String path ) {
      if( memoPathIndex==null ) return null;
      TreeObj treeObj = memoPathIndex.get(path);
      if( treeObj==null ) return null;
      treeObj.isIn=-1;
      return treeObj;
   }
   
   /** M�morisation de la position de la branche dans l'arbre afin de pouvoir la r�ins�rer au bon endroit */
   private void memorizeOldBranche(TreeObj treeObj) {
      if( memoPathIndex==null ) memoPathIndex = new HashMap<String, TreeObj>(10000);
      memoPathIndex.put(treeObj.path,treeObj);
   }
   
   /** Suppression d'un noeud, et de la branche morte si n�cessaire
    * @param treeObj l'objet associ� au noeud qu'il faut supprimer
    */
   protected void removeTreeBranch(TreeObj treeObj) {
      
      // Il faut trouver le node correspondant au treeObj
      boolean trouve = false;
      DefaultMutableTreeNode node=null;
      Enumeration e = root.preorderEnumeration();
      while( e.hasMoreElements() ) {
         node = (DefaultMutableTreeNode) e.nextElement();
         if( treeObj == (TreeObj) node.getUserObject() ) { trouve=true; break; }
      }
      if( !trouve ) return;
      
      removeTreeBranch((DefaultTreeModel) getModel(), node);
   }
   
   /** Suppression d'un node, et de la branche morte si n�cessaire */
   private void removeTreeBranch( DefaultTreeModel model, DefaultMutableTreeNode node ) {
      DefaultMutableTreeNode fils=null;
      int index = -1;
      while( node!=root && node.isLeaf() ) {
         DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
         TreeObj treeObj = (TreeObj) node.getUserObject();
         index = parent.getIndex(node);
         
         // M�morisation de l'index afin de pouvoir r�ins�rer la branche au bon endroit
         treeObj.treeIndex = index;
         
         // S'il s'agit d'un noeud non terminal, on va le m�moriser pour pouvoir
         // le r�sins�rer � la bonne place le cas �ch�ant
         if( !(treeObj instanceof TreeObjReg) ) memorizeOldBranche(treeObj);
         
         parent.remove(index);
         fils = node;
         node = parent;
      }
      
      // On alerte les listeners qu'une branche a �t� supprim�e
      if( fils!=null ) model.nodesWereRemoved(node, new int[]{index}, new Object[]{fils} );
   }

   /** true si tous les noeuds sont collaps�s sauf ceux au plus haut niveau (sous root) */
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
   
   /** true si tous les noeuds sous le parent indiqu� par son TreePath sont collaps�s */
   private boolean isCollapsedRec(TreePath parent) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
      if (node.getChildCount() >= 0) {
         for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(subNode);
            if( !isCollapsedRec(path) ) return false;
         }
      }
      return isCollapsed(parent);
   }
   
   /** Collapse tous les noeuds sont ceux au plus haut niveau (sous root) */
   protected void defaultExpand() {
      TreePath rootTp = new TreePath(root);
      if (root.getChildCount() >= 0) {
         for (Enumeration e = root.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = rootTp.pathByAddingChild(subNode);
            collapseRec(path);
         }
      }
      expandPath(rootTp);
   }
   
   /** Collapse tous les noeuds sous le parent */
   private void collapseRec(TreePath parent) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
      if (node.getChildCount() >= 0) {
         for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(subNode);
            collapseRec(path);
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
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(subNode);
            expandAll(path);
         }
      }
      expandPath(parent);
   }
   
   // Map d'icones qui servent � montrer l'origine des HiPS */
   private HashMap<String, ImageIcon> mapIcon = null;
   
   // Initialisation de la map des Icons et de leurs diff�rents �tats */
   private void initMapIcon() {
      Image img;
      
      try {
         mapIcon = new HashMap<String, ImageIcon>();
         img = aladin.getImagette("cds.png");
         mapIcon.put("CDS",new ImageIcon(img));
         mapIcon.put("CDS/color",new ImageIcon(setIconTag(img)));
         img = aladin.getImagette("esa.png");
         mapIcon.put("ESAVO",new ImageIcon(img));
         mapIcon.put("ESAVO/color",new ImageIcon(setIconTag(img)));
         img = aladin.getImagette("jaxa.png");
         mapIcon.put("JAXA",new ImageIcon(img));
         mapIcon.put("JAXA/color",new ImageIcon(setIconTag(img)));
         img = aladin.getImagette("irap.png");
         mapIcon.put("ov-gso",new ImageIcon(img));
         mapIcon.put("ov-gso/color",new ImageIcon(setIconTag(img)));
         img = aladin.getImagette("xcatdb.png");
         mapIcon.put("xcatdb",new ImageIcon(img));
         mapIcon.put("xcatdb/color",new ImageIcon(setIconTag(img)));
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   // Retourne l'icone qui correspond � un Identificateur de HiPS et � un mode partiulier */
   private ImageIcon getIcon(String id, int mode) {
      if( id==null ) return null;
      if( mapIcon==null ) initMapIcon();
      
      int i = id.indexOf('/');
      if( i==-1 ) i=id.length();
      String key = id.substring(0,i) + (mode==1?"/color":mode==2?"/fits":"");
      return mapIcon.get(key);
   }
   
   private static final Color r = new Color(255,100,100);
   private static final Color v = new Color(50,255,50);
   private static final Color b = new Color(100,100,255);
   
   /** Ajout d'un logo RGB � une image d'icone (sert pour rep�rer les HiPS couleur */
   private Image setIconTag( Image img ) {
      int w = img.getWidth(aladin)+14;
      int h = img.getHeight(aladin);
      BufferedImage img2 = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
      Graphics g = img2.getGraphics();
      g.drawImage(img,0,0,aladin);
      
      int x = w-8;
      int y = h/2;

      g.setColor(r);
      Util.fillCircle5(g, x, y-3);

      g.setColor(v);
      Util.fillCircle5(g, x, y+2);

      g.setColor(b);
      Util.fillCircle5(g, x+4, y);
      
      return img2;
   }

   /** Classe pour l'�dition d'un noeud de l'arbre */
   class NoeudRenderer implements TreeCellRenderer {
      DefaultTreeCellRenderer nonLeafRenderer = new DefaultTreeCellRenderer();
      Color selectionForeground, selectionBackground, textForeground, textBackground;

      NoeudRenderer() {
         selectionForeground = UIManager.getColor("Tree.selectionForeground");
         selectionBackground = aladin.getBackground();
         textForeground = UIManager.getColor("Tree.textForeground");
         textBackground = aladin.getBackground();
         
         nonLeafRenderer.setBackgroundNonSelectionColor( aladin.getBackground() );
      }

      public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeObj n = (TreeObj)node.getUserObject();
         
         Component c = nonLeafRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
         if( n.isInStack() ) c.setForeground( Color.green );
         else if( !aladin.hipsStore.prune.isActivated() ) {
            int isIn = n.getIsIn();
            c.setForeground( isIn==0 ? Aladin.ORANGE : isIn==1 ? Aladin.GREEN : Color.black );
         }
         
         // Affichage des compteurs
         if( !node.isLeaf() && c instanceof JLabel ) {
            int nb = n.nbDescendance;
            int ref = n.nbRefDescendance;
            String s = "<font color=\"gray\"> &rarr; "+ (nb==ref || nb==-1 ? +ref 
                  : " "+nb+" / "+ref )+"</font>";
//            : " "+nb+"<font size=\"1\">/"+ref+"</font>")+"</font>";
            JLabel lab = (JLabel)c;
            lab.setText("<html>"+lab.getText()+s+"</html>" );
         }
         
         if( n instanceof TreeObjReg ) {
            TreeObjReg hips = (TreeObjReg)n;
//            if( hips.isLocal() )  c.setForeground(Color.black);
            ImageIcon icon = getIcon(hips.internalId,hips.isColored() ? 1 : 0);
            if( icon!=null ) nonLeafRenderer.setIcon( icon );
            
         }

         return c;
      }
   }
}

