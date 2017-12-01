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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
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
 * @version 2.0 Janvier 2017 - reprise complète à partir de MyTree
 */
public class DirectoryTree extends JTree {
   protected DefaultMutableTreeNode root;
   private Aladin aladin;

   protected DirectoryTree(Aladin aladin, Color cbg) {
      this.aladin = aladin;
      
      setUI( new MyTreeUI(cbg ));

      setModel( new DirectoryModel(aladin) );
      
//      setBackground( cbg );
      setOpaque(true);
//      setRootVisible(false);
      
      setBorder( BorderFactory.createEmptyBorder(10, 0, 5, 0));
      setShowsRootHandles(true);
      NoeudRenderer nr = new NoeudRenderer();
      setCellRenderer(nr);
      
      // Pour accélérer tout ça
      setLargeModel(true);
      Component c =  nr.getTreeCellRendererComponent( this, getModel().getRoot(), false, false, false, 1, false);
      int rowHeight = c.getPreferredSize().height;
      setRowHeight(rowHeight);
   }
   
   public boolean hasBeenExpanded(TreePath path) {
      return false;
  }
   
   public void setModel( TreeModel model ) {
      root = (DefaultMutableTreeNode) model.getRoot();
      super.setModel(model);
   }
   
   /** Ouvre l'arbre en montrant la branche/le noeud associé au path donnée
    * @param p le path à montrer
    * @return true si possible, false si non visible pour le moment
    */
   protected boolean showBranch(String p) {
      if( p==null ) return false;
      
      TreePath path = findBranch(new TreePath(root),p);
      if( path==null ) return false;
         
      // Et on ouvre uniquement les sous-branches
      collapseRec(new TreePath(root));
      expandPath(path);
      
      // Selection du noeud trouvé
      setSelectionPath(path);
      
      // Scrolling s'il n'est pas visible
      Rectangle bounds = getPathBounds(path);
      bounds.height = getVisibleRect().height;
      scrollRectToVisible(bounds);
      
      return true;
   }
   
   /** Retourne le path de la branche/noeud associé au "path", ou null si non trouvé */
   private TreePath findBranch(TreePath parent,String p) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
      TreeObj to = (TreeObj)node.getUserObject();
      if( to.path.equals( p ) ) return parent;
      
      if (node.getChildCount() >= 0) {
         for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(subNode);
            TreePath tp = findBranch(path,p);
            if( tp!=null ) return tp;
         }
      }
      return null;
   }

   
   /** Ouvre l'arbre en montrant le noeud associé à l'id spécifié */
   protected void showTreeObj(String id) { 
      if( id==null ) return;
      TreePath path = findTreeObj(new TreePath(root),id);
      if( path==null ) return;
         
      // Selection du noeud trouvé
      setSelectionPath(path);
      
      // Scrolling s'il n'est pas visible
      Rectangle bounds = getPathBounds(path);
      bounds.height = getVisibleRect().height;
      scrollRectToVisible(bounds);
   }
   
   /** Retourne le path du noeud associé à l'id, ou null si non trouvé */
   private TreePath findTreeObj(TreePath parent,String id) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) parent.getLastPathComponent();
      TreeObj to = (TreeObj)node.getUserObject();
      if( to instanceof TreeObjDir && ((TreeObjDir)to).internalId.endsWith(id) ) return parent;
      
      if (node.getChildCount() >= 0) {
         for (Enumeration e = node.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode subNode = (DefaultMutableTreeNode) e.nextElement();
            TreePath path = parent.pathByAddingChild(subNode);
            TreePath tp = findTreeObj(path,id);
            if( tp!=null ) return tp;
         }
      }
      return null;
   }


   /** true si tous les noeuds sont collapsés sauf ceux au plus haut niveau (sous root) */
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
   
   /** true si tous les noeuds sous le parent indiqué par son TreePath sont collapsés */
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
   
   /** Déploie si nécessaire les noeuds de plus haut niveau */
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
   
   protected void expandAll(TreePath parent) {
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
   
   // Map d'icones qui servent à montrer l'origine des HiPS */
   private HashMap<String, MyImageIcon> mapIcon = null;
   
   // Initialisation de la map des Icons et de leurs différents états */
   private void initMapIcon() {
      Image img;
      
      try {
         mapIcon = new HashMap<String, MyImageIcon>();
         
         img = aladin.getImagette("Folder.png");
         mapIcon.put("folder",new MyImageIcon(img));
         
         img = aladin.getImagette("FolderUnsupervised.png");
         mapIcon.put("folderunsupervised",new MyImageIcon(img));
         
         img = aladin.getImagette("FolderProblematic.png");
         mapIcon.put("folderproblematic",new MyImageIcon(img));
         
         img = aladin.getImagette("FolderVizieR.png");
         mapIcon.put("foldervizieR",new MyImageIcon(img));
         
         img = aladin.getImagette("cds.png");
         mapIcon.put("cds",new MyImageIcon(img));
         mapIcon.put("cds.vizier",new MyImageIcon(img));
         mapIcon.put("cds/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("nasa.png");
         mapIcon.put("nasa.heasarc",new MyImageIcon(img));
         mapIcon.put("nasa.heasarc/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("eso.png");
         mapIcon.put("eso.org",new MyImageIcon(img));
         mapIcon.put("eso.org/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("ipac.png");
         mapIcon.put("ned.ipac",new MyImageIcon(img));
         mapIcon.put("irsa.ipac",new MyImageIcon(img));
         mapIcon.put("irsa.ipac/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("mast.png");
         mapIcon.put("mast.stsci",new MyImageIcon(img));
         mapIcon.put("mast.stsci/color",new MyImageIcon(setIconTag(img)));
         mapIcon.put("archive.stsci.edu",new MyImageIcon(img));
         mapIcon.put("archive.stsci.edu/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("cadc.png");
         mapIcon.put("cadc.nrc.ca",new MyImageIcon(img));
         mapIcon.put("cadc.nrc.ca/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("esa.png");
         mapIcon.put("esavo",new MyImageIcon(img));
         mapIcon.put("esavo/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("jaxa.png");
         mapIcon.put("jaxa",new MyImageIcon(img));
         mapIcon.put("jaxa/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("irap.png");
         mapIcon.put("ov-gso",new MyImageIcon(img));
         mapIcon.put("ov-gso/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("xcatdb.png");
         mapIcon.put("xcatdb",new MyImageIcon(img));
         mapIcon.put("xcatdb/color",new MyImageIcon(setIconTag(img)));
         
         img = aladin.getImagette("wfau.png");
         mapIcon.put("wfau.roe.ac.uk",new MyImageIcon(img));
         mapIcon.put("wfau.roe.ac.uk/color",new MyImageIcon(img));
         
         img = aladin.getImagette("inaf.png");
         mapIcon.put("ia2.inaf.it",new MyImageIcon(img));
         mapIcon.put("ia2.inaf.it/color",new MyImageIcon(img));
         
         img = aladin.getImagette("cfa.png");
         mapIcon.put("cfa.tdc",new MyImageIcon(img));
         mapIcon.put("cfa.tdc/color",new MyImageIcon(img));
         
         img = aladin.getImagette("nrao.png");
         mapIcon.put("nrao",new MyImageIcon(img));
         mapIcon.put("nrao/color",new MyImageIcon(img));
         
         img = aladin.getImagette("cvo.png");
         mapIcon.put("cvo.naoc",new MyImageIcon(img));
         mapIcon.put("cvo.naoc/color",new MyImageIcon(img));
         
         img = aladin.getImagette("nova.png");
         mapIcon.put("ar.nova",new MyImageIcon(img));
         mapIcon.put("ar.nova/color",new MyImageIcon(img));
         
         img = aladin.getImagette("svo.png");
         mapIcon.put("svo.cab",new MyImageIcon(img));
         mapIcon.put("svo.cab/color",new MyImageIcon(img));
         mapIcon.put("svo.ifca",new MyImageIcon(img));
              
         img = aladin.getImagette("ucl.png");
         mapIcon.put("mssl.ucl.ac.uk",new MyImageIcon(img));
         mapIcon.put("mssl.ucl.ac.uk/color",new MyImageIcon(img));
         
         img = aladin.getImagette("gavo.png");
         mapIcon.put("org.gavo.dc",new MyImageIcon(img));
         mapIcon.put("org.gavo.dc/color",new MyImageIcon(img));
         
        
         mapIcon.put("f",new MyImageIcon(img));
         
         mapIcon.put("defaut",new MyImageIcon());
         
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   // Retourne l'icone qui correspond à un Identificateur de HiPS et à un mode partiulier */
   private MyImageIcon getIcon(String id, int mode) {
      if( id==null ) return null;
      if( mapIcon==null ) initMapIcon();
      
      int i = id.indexOf('/');
      if( i==-1 ) i=id.length();
      String key = id.substring(0,i) + (mode==1?"/color":mode==2?"/fits":"");
      return mapIcon.get(key.toLowerCase());
   }
   
   private static final Color r = new Color(255,100,100);
   private static final Color v = new Color(50,255,50);
   private static final Color b = new Color(100,100,255);
   
   /** Ajout d'un logo RGB à une image d'icone (sert pour repérer les HiPS couleur */
   private Image setIconTag( Image img ) {
      int w = img.getWidth(aladin)+14;
      int h = img.getHeight(aladin);
      BufferedImage img2 = new BufferedImage(w,h, BufferedImage.TYPE_INT_ARGB);
      Graphics g = img2.getGraphics();
      
      g.drawImage(img,1,0,aladin);
      drawRGBIconTag(g,w-8,h/2);
      
      return img2;
   }
   
   /** Tracé des 3 cercles RGB pour indiquer qu'il s'agit d'une collection HiPS couleur */
   static public void drawRGBIconTag(Graphics g, int x, int y) {
      g.setColor(r);
      Util.fillCircle5(g, x, y-3);

      g.setColor(v);
      Util.fillCircle5(g, x, y+2);

      g.setColor(b);
      Util.fillCircle5(g, x+4, y);
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
         
         nonLeafRenderer.setBackgroundNonSelectionColor( getBackground() );
      }

      public Component getTreeCellRendererComponent(JTree tree, Object obj, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus){
         DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
         TreeObj n = (TreeObj)node.getUserObject();
         
         Component c = nonLeafRenderer.getTreeCellRendererComponent(tree, obj, selected, expanded, leaf, row, hasFocus);
         
         try {
            // Affichage des compteurs
            if( !node.isLeaf() && c instanceof JLabel ) {
               int nb = n.nb;
               int ref = n.nbRef;
               String s = "<font color=\"gray\"> &rarr; "+ (nb==ref || nb==-1 ? +ref 
                     : " "+nb+" / "+ref )+"</font>";
               JLabel lab = (JLabel)c;
               lab.setText("<html>"+lab.getText()+s+"</html>" );
            }

            boolean flagHighLighted=false;
            TreeObjDir tohl = aladin.directory.getTreeObjDirHighLighted();
            if( node.isLeaf() && tohl!=null && ((TreeObjDir)n).internalId.equals(tohl.internalId) ) {
               ((DefaultTreeCellRenderer)c).setBackgroundNonSelectionColor( Aladin.COLOR_STACK_HIGHLIGHT );
               flagHighLighted=true;
            } else {
               ((DefaultTreeCellRenderer)c).setBackgroundNonSelectionColor( getBackground() );
            }
            
            int isIn = n.getIsIn();
            
            Color fg = Aladin.COLOR_CONTROL_FOREGROUND;
            if( isIn== 0 ) fg = Aladin.ORANGE;
            else if( isIn== 1 ) fg = Aladin.COLOR_GREEN;
            if( aladin.directory.iconInside.isActivated() 
                  && !node.isLeaf() && n.nb!=n.nbRef ) fg = Aladin.COLOR_CONTROL_FOREGROUND;
//            if( aladin.directory.iconInside.isActivated() ) fg = Aladin.COLOR_GREEN;
            if( flagHighLighted || selected ) fg = fg.brighter();
            c.setForeground( fg );

            boolean flagTestInside = aladin.directory.iconInside.isAvailable();
            
            MyImageIcon icon=null;
            if( n instanceof TreeObjDir) {
               TreeObjDir to = (TreeObjDir)n;
               icon = getIcon( to.internalId,to.isColored() ? 1 : 0);
               if( icon==null ) icon = getIcon("defaut",0);
            } else if( !node.isLeaf() ) {
               /* if( n.path.endsWith("/VizieR") ) icon=getIcon( "FolderVizieR", 0);
               else */ 
               if( n.path.startsWith(Directory.OTHERS) ) icon=getIcon( "FolderUnsupervised", 0);
               else if( n.path.startsWith(Directory.PROBLEMATIC) ) icon=getIcon( "FolderProblematic", 0);
               else icon=getIcon("Folder", 0);
            }
            if( icon!=null ) {
               icon.setColor( n.isInStack() );
               
               boolean hasMoc = true;
               boolean isNew = false;
               if( n instanceof TreeObjDir ) {
                  TreeObjDir to = (TreeObjDir)n;
                  isNew = to.isNew();
                  
                  if( flagTestInside ) {
                     hasMoc = isIn!=-1 || to.hasMoc();
                     if( !hasMoc && to.isScanning() ) { hasMoc = aladin.directory.blinkState; }
                  }
               }
               icon.setNew( isNew );
               icon.setMoc( hasMoc );
               nonLeafRenderer.setIcon( icon );
            }
         } catch( Exception e ) { }
         
         c.setMinimumSize( new Dimension( 150,c.getMinimumSize().height));

         return c;
      }
   }
   
   class MyImageIcon extends ImageIcon {
      Color color;
      boolean defaut;
      boolean hasMoc;
      boolean isNew;
      
      public MyImageIcon() {
         super();
         defaut=true;
         hasMoc=true;
         color=null;
      }
      public MyImageIcon(Image image) {
         super(image);
      }
      
      void setColor(Color color)  { this.color=color; }
      void setMoc(boolean hasMoc) { this.hasMoc = hasMoc; }
      void setNew(boolean isNew)  { this.isNew = isNew; }
      
      public int getIconWidth() {
         if( defaut ) return 12;
         return super.getIconWidth();
      }
      
      public int getIconHeight() {
         if( defaut ) return 12;
         return super.getIconHeight();
      }
      
      public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
         if( defaut ) {
            g.setColor( Color.gray.darker() );
            Util.fillCircle7(g, x+5, y+6);
            
         } else super.paintIcon(c,g,x,y);
         
         if( color!=null ) Util.drawCheck(g,-3,-2,color==Color.black ? color.lightGray : color);
         if( !hasMoc ) Util.drawWarning(g,0,8,Aladin.ORANGE,Color.black);
         if( isNew ) Util.drawNew(g,9,3/*getIconHeight()-2*/,Color.yellow);
      }
   }
}

