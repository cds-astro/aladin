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
import java.util.*;

import javax.swing.*;
import javax.swing.tree.*;

/**
 * Formulaire d'interrogation sous la forme d'un arbre
 * @author Pierre Fernique [CDS]
 */
public abstract class ServerTree extends Server implements Iterable<TreeNode>  {
   protected String info,info1;
   protected DefaultMutableTreeNode root;
   protected TreeForServer tree;

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
      JLabel l;
      if( info!=null ) {
         l= new JLabel(info);
         l.setBounds(120,y,400, 20); y+=20;
         add(l);
      }

      // Target ?
      y=makeTarget(y);

      // L'arbre
      tree = new TreeForServer(aladin);
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

   abstract protected void initTree();


   @Override
   public void show() {
      initTree();
      super.show();
   }

   /** Reset */
   @Override
   protected void reset() { tree.reset(); }

   /** Interrogation */
   @Override
   public void submit() { tree.submit(); }

   public Iterator<TreeNode> iterator() { return tree.iterator(); }

   class TreeForServer extends MyTree {

      TreeForServer(Aladin aladin) { super(aladin); }

      protected void warning() {
         aladin.warning(aladin.dialog,WNEEDCHECK,1);
      }

      /** Activation ou non des branches de l'arbre en fonction de l'activation des feuilles */
      protected boolean setOkTree(DefaultMutableTreeNode node) {
         TreeNode gSky = (TreeNode) node.getUserObject();
         if( node.isLeaf() )  return gSky.isOk();

         boolean rep=false;
         DefaultMutableTreeNode subNode = null;
         //         System.out.println("setOkTree "+node+" #subnode="+node.getChildCount());
         Enumeration e = node.children();
         while( e.hasMoreElements() ) {
            subNode = (DefaultMutableTreeNode) e.nextElement();
            if( setOkTree(subNode) ) rep=true;
         }

         gSky.setOk(rep);
         //         System.out.println("*** "+gSky+" => "+rep);
         return rep;
      }
   }
}
