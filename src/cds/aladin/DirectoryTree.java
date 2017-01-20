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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import cds.tools.Util;

/**
 * Gestion d'un arbre dynamique
 * @author Pierre Fernique [CDS]
 * @version 2.0 Janvier 2017 - reprise compl�te � partir de MyTree
 */
public class DirectoryTree extends JTree {
   protected DefaultMutableTreeNode root;
   private Aladin aladin;

   protected DirectoryTree(Aladin aladin, Color cbg) {
      this.aladin = aladin;
      setModel( new DirectoryModel(aladin) );
      
      setBackground( cbg );
      setBorder( BorderFactory.createEmptyBorder(10, 0, 10, 0));
      setShowsRootHandles(true);
      NoeudRenderer nr = new NoeudRenderer();
      setCellRenderer(nr);
      
      // Pour acc�l�rer tout �a
      setLargeModel(true);
      Component c =  nr.getTreeCellRendererComponent( this, getModel().getRoot(), false, false, false, 1, false);
      int rowHeight = c.getPreferredSize().height;
      setRowHeight(rowHeight);
   }
   
   public void setModel( TreeModel model ) {
      root = (DefaultMutableTreeNode) model.getRoot();
      super.setModel(model);
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
   protected boolean isCollapsedRec(TreePath parent) {
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
   
   /** D�ploie si n�cessaire les noeuds de plus haut niveau */
   protected void minimalExpand() {
      TreePath rootTp = new TreePath(root);
      expandPath(rootTp);
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
   protected void collapseRec(TreePath parent) {
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
   protected void allExpand() { allExpand( new TreePath(root) ); } 
   protected void allExpand(TreePath path) {

      List<TreeExpansionListener> expListeners = Arrays.asList( getTreeExpansionListeners() );
      for( TreeExpansionListener listener : expListeners) removeTreeExpansionListener(listener);

      expandAll( path );

      for( TreeExpansionListener listener : expListeners) addTreeExpansionListener(listener);

      collapsePath(path);
      expandPath(path);
   }
   
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
         img = aladin.getImagette("Folder.png");
         mapIcon.put("Folder",new ImageIcon(img));
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
         mapIcon.put("f",new ImageIcon(img));
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
         
         nonLeafRenderer.setBackgroundNonSelectionColor( getBackground() );
      }

      public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeObj n = (TreeObj)node.getUserObject();
         
         Component c = nonLeafRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
         
         try {
            if( n.isInStack() ) c.setForeground( Color.green );
            else if( !aladin.directory.inside.isActivated() ) {
               int isIn = n.getIsIn();
               c.setForeground( isIn==0 ? Aladin.ORANGE : isIn==1 ? Aladin.GREEN : Color.black );
            }
            
            // Affichage des compteurs
            if( !node.isLeaf() && c instanceof JLabel ) {
               int nb = n.nb;
               int ref = n.nbRef;
               String s = "<font color=\"gray\"> &rarr; "+ (nb==ref || nb==-1 ? +ref 
                     : " "+nb+" / "+ref )+"</font>";
               JLabel lab = (JLabel)c;
               lab.setText("<html>"+lab.getText()+s+"</html>" );
               lab.invalidate();
            }
            
            ImageIcon icon=null;
            if( n instanceof TreeObjDir) {
               TreeObjDir to = (TreeObjDir)n;
               icon = getIcon( to.internalId,to.isColored() ? 1 : 0);
            } else if( !node.isLeaf() ) icon=getIcon( "Folder", 0);
            if( icon!=null ) nonLeafRenderer.setIcon( icon );
         } catch( Exception e ) { }
         
         c.setMinimumSize( new Dimension( 150,c.getMinimumSize().height));

         return c;
      }
   }
}
