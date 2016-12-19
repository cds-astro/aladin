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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;


/** Gère les noeuds de l'arbre des catégories (formulaire ServerCategory) */
public class TreeNodeCategory extends TreeNodeBasic {
   String url;
   String docUser;
   String exampleUrl;
   String origin;

   TreeNodeCategory(Aladin aladin, String ID, String nom, String path) {
      this(aladin,ID,nom,path,null,null,null);
   }

   TreeNodeCategory(Aladin aladin,String actionName,String description,String path,
         String url,String docUser,String aladinUrlDemo) {
      super(aladin,actionName,null,description,path);
      this.url=url;
      if( docUser!=null ) setDocUSer(docUser);
      if( aladinUrlDemo!=null ) setExUrl(aladinUrlDemo);
   }

   @Override
   protected void submit() {
      aladin.calque.newPlan(aladin.glu.getURL(id)+"",label,origin);
   }

   void loadExample() { aladin.execCommand("get File("+exampleUrl+")"); }
   
   void loadDocUser() { aladin.glu.showDocument(docUser); }

   void setUrl(String url) { this.url=url; }
   void setExUrl(String exUrl) {
      this.exampleUrl = exUrl;
      JButton b = new JButton(aladin.chaine.getString("TREEEXAMPLE"));
      b.setFont(b.getFont().deriveFont(Font.ITALIC));
      b.setForeground(Color.blue);
      b.setBackground(background);
      b.setContentAreaFilled(false);
      b.setBorder( BorderFactory.createMatteBorder(0, 0, 1, 0, Color.blue) );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { loadExample(); }
      });
      gc.insets.bottom=7;
      gb.setConstraints(b,gc);
      getPanel().add(b);
   }
   void setDocUSer(String docUser) {
      this.docUser = docUser;
      JButton b = new JButton(aladin.chaine.getString("TREEMORE"));
      b.setFont(b.getFont().deriveFont(Font.ITALIC));
      b.setForeground(Color.blue);
      b.setBackground(background);
      b.setContentAreaFilled(false);
      b.setBorder( BorderFactory.createMatteBorder(0, 0, 1, 0, Color.blue) );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { loadDocUser(); }
      });
      gc.insets.bottom=7;
      gb.setConstraints(b,gc);
      getPanel().add(b);
   }
}
