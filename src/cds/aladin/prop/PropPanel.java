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

package cds.aladin.prop;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.tools.Util;

/** G�re le panel associ� aux propri�t�s d'un objet
 * @date d�c 2011 - cr�ation
 * @author Pierre Fernique [CDS]
 */
public class PropPanel extends JPanel {
   private JFrame frame;            // frame parent, peut �tre null
   private Vector<Prop> propList;   // Liste des propri�t�s
   
   /**
    * Panel affichant une liste de propri�t�s
    * @param frame JFrame parent, peut �tre null
    * @param obj objet "Propable" pour lequel sont affich�es les propri�t�s
    */
   public PropPanel(Propable obj) { this(null,obj); }
   public PropPanel(JFrame frame,Propable obj) {
      super();
      this.frame = frame;
      propList = obj.getProp();
      createPanel();
   }
   
   /** M�thode � appeler pour valider toutes les modifications des propri�t�s faites
    * par l'utilsiateur
    * @return PropAction.NOTHING, PropAction.FAILED ou PropAction.SUCCESS suivant le cas
    */
   public int apply() {
      int rep=PropAction.NOTHING;
      for( Prop p : propList ) {
         int a = p.apply();
         if( a==PropAction.FAILED ) rep = a;
         else if( rep==PropAction.NOTHING && a==PropAction.SUCCESS ) rep = a;
      }
      return rep;
   }
   
   /** M�thode � appler pour r�initilisateur toutes les propri�t�s */
   public void resume() {
      for( Prop p : propList ) p.resume();
   }
   
   // Cr�ation du panel. Une ligne par propri�t�.
   private void createPanel() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g = new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,2,2,2);
      setLayout(g);
      for( Prop p : propList ) {
         PropPanel.addCouple(frame, this, p.getLabel(), p.getHelp(), p.getWidget(), g, c, GridBagConstraints.EAST);           
      }
   }
   
   
   /********************************** Methodes statitiques de mise en forme ******************************************/
   
   public static void addCouple(JFrame frame, JPanel p, Object titre, String help, Component valeur,
            GridBagLayout g, GridBagConstraints c) {
        addCouple(frame, p, titre, help, valeur, g, c, GridBagConstraints.WEST);
    }
   
   public static void addCouple(JPanel p, Object titre, Component valeur,
         GridBagLayout g, GridBagConstraints c) {
     addCouple(null, p, titre, null, valeur, g, c, GridBagConstraints.WEST);
   }
   
   /** Ajoute dans le JPanel un couple d'elements titre: valeur
   * @param frame  Le frame de r�f�rence (pour savoir o� afficher le help, null sinon)
   * @param p      Le panel sur lequel on travaille
   * @param titre  Le titre de l'element que l'on va ajouter
   * @param valeur L'element (Component) a ajouter
   * @param g      Le gestionnaire d'affichage
   * @param c      Les contraintes courantes sur le gestionnaire d'affichage
   */
   public static void addCouple(final JFrame frame, JPanel p, Object titre, final String help, Component valeur,
                GridBagLayout g, GridBagConstraints c, int titleAnchor) {
   
      Component t;
   
      if( titre instanceof String ) {
         JLabel l = new JLabel((String)titre);
         l.setFont(l.getFont().deriveFont(Font.ITALIC));
         t=l;
      } else t=(Component)titre;
      
      if( help!=null ) {
         JPanel p2 = new JPanel();
         p2.add(t);
         JButton h = Util.getHelpButton(frame,help);
         p2.add(h);
         t=p2;
      }
   
      c.anchor = titleAnchor;
      c.gridwidth = GridBagConstraints.RELATIVE;
      c.fill = GridBagConstraints.NONE;
      c.weightx = 0.0;
      g.setConstraints(t,c);
      p.add(t);
      
      if( valeur instanceof JButton ) {
         JPanel p1 = new JPanel();
         p1.add(valeur);
         t=p1;
      } else t=valeur;
   
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.fill = GridBagConstraints.NONE;
      c.weightx = 1.0;
      c.anchor = GridBagConstraints.WEST;
      g.setConstraints(t,c);
      p.add(t);
   
   }
   
//   public static void addFull(JPanel p, Component valeur, GridBagLayout g, GridBagConstraints c) {
//      c.gridwidth = GridBagConstraints.REMAINDER;
//      c.fill = GridBagConstraints.BOTH;
//      c.weightx = 1.0;
//      c.anchor = GridBagConstraints.CENTER;
//      g.setConstraints(valeur,c);
//      p.add(valeur);
//   }
   
   /** Ajoute d'un paragraphe centr� d'explications et passe � la ligne
   * @param p Le panel sur lequel on travaille
   * @param info Le texte d'explication (peut contenir des \n
   * @param g Le gestionnaire d'affichage
   * @param c les contraintes courantes sur le gestionnaire d'affichage
   */
   public static void addInfo(JPanel p, String info,GridBagLayout g, GridBagConstraints c) {
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.CENTER;
      c.fill = GridBagConstraints.HORIZONTAL;
      JLabel l = new JLabel(Util.fold("<center>"+info+"</center>",80,true),JLabel.CENTER);
      l.setFont(l.getFont().deriveFont(Font.ITALIC));
      g.setConstraints(l,c);
      p.add(l);
   }
   
   /** Ajoute d'un titre de section et passe a la ligne
    * @param p Le panel sur lequel on travaille
    * @param title Le titre de la nouvelle section
    * @param g Le gestionnaire d'affichage
    * @param c les contraintes courantes sur le gestionnaire d'affichage
    */
   public static void addSectionTitle(JPanel p, String title,GridBagLayout g, GridBagConstraints c) {
   	  JLabel l = new JLabel(title);
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.WEST;
      c.weightx = 1.0;
      g.setConstraints(l,c);
      p.add(l);
   }
   
   public static void addFilet(JPanel p, GridBagLayout g, GridBagConstraints c,int h,int type) {
      Filet f = new Filet(h,type);
      GridBagConstraints c1 = (GridBagConstraints) c.clone();
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.BOTH;
      g.setConstraints(f,c);
      p.add(f);
      c = c1;
   }
   
   /** Ajoute un filet et passe a la ligne
   * @param p Le panel sur lequel on travaille
   * @param g Le gestionnaire d'affichage
   * @param c les contraintes courantes sur le gestionnaire d'affichage
   */
   public static void addFilet(JPanel p, GridBagLayout g, GridBagConstraints c) { PropPanel.addFilet(p,g,c,5,1); }
   

}


