// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
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

public class TreeObj  implements Comparator {
   Aladin aladin;
   String id;
   String label;
   String path;
   String ordre;
   int isIn;            // 1-le noeud est isIn, 0-le noeud est isOut, -1-on ne sait pas encore
   boolean isHidden;    // true si le noeud n'est pas pris dans l'arbre
   int nb;   // Nombre de noeud terminaux dans sa descendance actuelle
   int nbRef; // Idem mais pour sa descendance initiale (avant d'éventuels élagages)
   boolean wasExpanded;  // true si le noeud est actuellement expanded

   protected JCheckBox checkbox;
   private JPanel panel;

   protected GridBagConstraints gc;
   protected GridBagLayout gb;
   protected static Color background = Color.white;

   public TreeObj() { }

   TreeObj(Aladin aladin, String id, String ordre, String label, String path) {
      this.aladin      = aladin;
      this.id  = id;
      this.label = label;
      this.path  = path;
      this.ordre = ordre==null ? "Z" : ordre;
      this.isIn=-1;
      this.isHidden = false;
      nb=-1;
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

   void setIn( int in ) { this.isIn=in; };
   int getIsIn() { return isIn; }
   
   // Juste pour compatibilité avec la version 9 => A VIRER
   boolean isIn() { return isIn!=0; }
   void setIn( boolean flag) { isIn= (flag ? 1 : 0); }
   
   void setHidden( boolean flag ) { this.isHidden=flag; };
   boolean isHidden() { return isHidden; }
   
   protected Color isInStack() { return null; }

   JPanel getPanel() { return panel; }

   protected JPanel createPanel() {
      
      checkbox = new JCheckBox(label);
      checkbox.setBorder(BorderFactory.createEmptyBorder());
      gc = new GridBagConstraints();
      gc.fill = GridBagConstraints.VERTICAL;
      gc.anchor = GridBagConstraints.CENTER;
      gc.gridx = GridBagConstraints.RELATIVE;
      gc.insets = new Insets(0,0,0,5);
      gb = new GridBagLayout();
      
      JPanel panel = new JPanel(gb);
      panel.setOpaque(true);
      gb.setConstraints(checkbox,gc);
      panel.add(checkbox);
      return panel;
   }

//   protected void loadHips() { };
   protected void submit() {};

   @Override
   public String toString() { return label; }

   /** Fournit un Comparator de mouvement pour les tris */
   static protected Comparator getComparator() { return new TreeObj(); }

   public int compare(Object o1, Object o2) {
      TreeObj a1 = (TreeObj)o1;
      TreeObj a2 = (TreeObj)o2;
      if( a1==null && a2==null ) return 0;
      if( a1==null ) return -1;
      if( a2==null ) return 1;
      if( a1.ordre==a2.ordre ) return 0;
      if( a1.ordre==null ) return -1;
      if( a2.ordre==null ) return 1;
      return a1.ordre.compareTo(a2.ordre);
   }

   public boolean equals(Object o) {
      TreeObj a1= (TreeObj)o;
      return a1.id.equals(id);
   }
}
