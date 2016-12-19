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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

public class TreeNodeBasic  implements Comparator {
   Aladin aladin;
   String id;
   String label;
   String path;
   String ordre;
   boolean isIn;      // le noeud est valide = affiché en noir, sinon en gris clair
   boolean isHidden;  // true si le noeud n'est pas pris dans l'arbre
   int treeIndex=-1;     // Index dans l'arbre (au niveau du parent)

   protected JCheckBox checkbox;
   private JPanel panel;

   protected GridBagConstraints gc;
   protected GridBagLayout gb;
   protected static Color background = Color.white;

   public TreeNodeBasic() { }

   TreeNodeBasic(Aladin aladin, String id, String ordre, String label, String path) {
      this.aladin      = aladin;
      this.id  = id;
      this.label = label;
      this.path  = path;
      this.ordre = ordre==null ? "Z" : ordre;
      this.isIn=true;
      panel=createPanel();
   }

   String getID() { return id; }

   void noCheckbox() { checkbox=null; }
   boolean hasCheckBox() { return checkbox!=null; }
   void setCheckBox(boolean f) {
      if( !hasCheckBox() ) return;
      checkbox.setSelected(f);
   }
   boolean isCheckBoxSelected() {
      if( !hasCheckBox() ) return false;
      return checkbox.isSelected();
   }

   void setIn( boolean in ) { this.isIn=in; };
   boolean isIn() { return isIn; }
   
   void setHidden( boolean flag ) { this.isHidden=flag; };
   boolean isHidden() { return isHidden; }
   
   protected boolean isInStack() { return false; }

   JPanel getPanel() { return panel; }

   public void setForeground(Color fg) { checkbox.setForeground(fg); }

   public Color getForeground() { return checkbox.getForeground(); }

   protected JPanel createPanel() {
      
      checkbox = new JCheckBox(label);
      //      checkbox.setBackground(background);
      checkbox.setBorder(BorderFactory.createEmptyBorder());
      gc = new GridBagConstraints();
      gc.fill = GridBagConstraints.VERTICAL;
      gc.anchor = GridBagConstraints.CENTER;
      gc.gridx = GridBagConstraints.RELATIVE;
      //      gc.insets = new Insets(2,0,4,5);
      gc.insets = new Insets(0,0,0,5);
      gb = new GridBagLayout();
      
      JPanel panel = new JPanel(gb);
      panel.setBackground( aladin.getBackground() );
      panel.setOpaque(true);
      //      panel.setBackground(background);
      gb.setConstraints(checkbox,gc);
      panel.add(checkbox);
      return panel;
   }

   protected void submit() { };

   @Override
   public String toString() { return label; }

   /** Fournit un Comparator de mouvement pour les tris */
   static protected Comparator getComparator() { return new TreeNodeBasic(); }

   public int compare(Object o1, Object o2) {
      TreeNodeBasic a1 = (TreeNodeBasic)o1;
      TreeNodeBasic a2 = (TreeNodeBasic)o2;
      if( a1.ordre==a2.ordre ) return 0;
      if( a1.ordre==null ) return -1;
      if( a2.ordre==null ) return 1;
      return a1.ordre.compareTo(a2.ordre);
   }

   public boolean equals(Object o) {
      TreeNodeBasic a1= (TreeNodeBasic)o;
      return a1.id.equals(id);
   }
}
