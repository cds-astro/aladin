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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.*;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import cds.tools.Util;

/**
 * Classe permettant la sélection d'un ou plusieurs plans en vue de la création
 * d'une RGB ou d'une séquence BLINK. Cette classe doit être dérivée.
 * @author Pierre Fernique [CDS]
 * @version 1.0 : Nov 2004 - Creation
 */
public abstract class FrameRGBBlink extends JFrame
                implements ActionListener {

   String SUBMIT, RESET,HELP,CANCEL,NONE;

   // Les references aux objets
   Aladin a;

   // Les composantes de l'objet
   boolean flagHide=true;   // Vrai si la fenetre est cache
   protected JComboBox ch[];   // Liste des Choices pour la liste des plans
   Plan choicePlan[];  // Pour faire la correspondance choice -> plan[]

   JButton submitBtn; // pour mode robot

   protected void createChaine() {
      SUBMIT = a.chaine.getString("IMGCREATE");
      RESET  = a.chaine.getString("IMGRESET");
      HELP   = a.chaine.getString("IMGHELP");
      CANCEL = a.chaine.getString("IMGCLOSE");
      NONE   = a.chaine.getString("IMGNONE");
   }

   /**
    * Génération d'une Frame permettant la sélection de plusieurs plans images.
    * @param aladin
    */
   protected FrameRGBBlink(Aladin aladin) {
      super();
      this.a=aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(getTitre());

      // raccourci pour fermeture rapide de la frame
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false, aladin);

      setLocation(Aladin.computeLocation(this));
      choicePlan=new Plan[0];
      createPanel();
   }

   /** Doit retourner le titre de la Frame */
   protected abstract String getTitre();

   /** Retourne le texte du Help */
   protected abstract String getHelp();

   /** Retourne la phrase d'explication en début de Frame */
   protected abstract String getInformation();

   /** Retourne le nombre de sélecteurs de plans à afficher */
   protected abstract int getNb();

   /** Retourne le label précédent chaque sélecteur de plans */
   protected abstract String getLabelSelector(int i);

   /** Retourne la couleur du label précédent chaque sélecteur */
   protected abstract Color getColorLabel(int i);

   /** Retourne un JPanel additionnel en fin de Frame */
   protected abstract JPanel getAddPanel();

   /** Retourne une liste de boutons additionnels le cas échéant */
   protected JButton [] getAddButtons() { return null; }

   /** Retourne le numéro du bouton ToolBox associé à la Frame */
   protected abstract int getToolNumber();

   /** Création du JPanel de la Frame */
   protected void createPanel() {
      GridBagConstraints c=new GridBagConstraints();
      GridBagLayout g=new GridBagLayout();
      c.fill=GridBagConstraints.BOTH; // J'agrandirai les composantes
      c.insets = new Insets(2,2,2,2);

      JPanel p=new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5,5));
      p.setLayout(g);

      // le titre de la fenetre
      JLabel l=new JLabel(Util.fold("<center>"+getInformation()+"</center>",80,true),JLabel.CENTER);
      c.gridwidth=GridBagConstraints.REMAINDER;
      c.weightx=1.0;
      c.weighty=1.0;
      g.setConstraints(l, c);
      p.add(l);
      
      // Création des lignes Choice pour choisir les plans
      GridBagLayout g1 = new GridBagLayout();
      JPanel p1 = new JPanel(g1);
      int n=getNb();
      ch=new JComboBox[n];
      for (int i=0; i<n; i++) {
         ch[i]=new JComboBox();
         ch[i].addActionListener(this);
         ch[i].setFont(Aladin.BOLD);

         JLabel ll=new JLabel(getLabelSelector(i));
         ll.setForeground(getColorLabel(i));
         ll.setFont(Aladin.BOLD);

         c.gridwidth=GridBagConstraints.RELATIVE;
         c.weightx=0.0;
         g1.setConstraints(ll, c);
         p1.add(ll);
         c.gridwidth=GridBagConstraints.REMAINDER;
         c.weightx=10.0;
         g1.setConstraints(ch[i], c);
         p1.add(ch[i]);
      }
      
      if( n<=6 ) {
         g.setConstraints(p1, c);
         p.add(p1);
      } else {
         JScrollPane pane = new JScrollPane(p1);
         pane.setPreferredSize(new Dimension(300, 30*6));
         g.setConstraints(pane, c);
         p.add(pane);
      }

      // Ajout d'un panel additionnel si besoin est
      JPanel add=getAddPanel();
      if (add!=null) {
         g.setConstraints(add, c);
         p.add(add);
      }

      // Ajout d'un panel pour les boutons de validation
      JPanel v=valid();
      g.setConstraints(v, c);
      p.add(v);

      getContentPane().add(p,BorderLayout.CENTER);
      pack();
   }

   /**
    * Mise a jour d'un menu deroulant contenant les labels des plans images
    * @param default L'item du menu par defaut
    */
//   private void adjustImageChoice(JComboBox c) {
//      adjustImageChoice(c, -1);
//   }
   protected void adjustImageChoice(JComboBox c, int defaut) {
      int i=c.getSelectedIndex();
      String s=(i>=0) ? (String)c.getItemAt(i) : null;
      c.removeAllItems();
      setItems(c);
      if (defaut>=0) c.setSelectedIndex(defaut);
      else if (s==null) c.setSelectedIndex(0);
      else c.setSelectedItem(s);
   }

   // Procedure interne utilisee par createImageChoice() et adjustImageChoice()
   synchronized private void setItems(JComboBox c) {
      c.addItem(NONE);
      for (int i=0; i<choicePlan.length; i++) {
         c.addItem(choicePlan[i].label+" - \""+choicePlan[i].objet+"\"");
      }
   }

   /** Recupere la liste des plans images valides */
   protected Plan[] getPlan() {
      Vector<Plan> v = a.calque.getPlanImg();
      if( v==null ) return new PlanImage[0];
      Plan pi [] = new PlanImage[v.size()];
      v.copyInto(pi);
      return pi;
   }

   /** Reset de la fenetre */
   protected void reset() {
      for (int i=getNb()-1; i>=0; i--) ch[i].setSelectedIndex(0);
   }

   /** Permet d'ajuster les Widgets en fonction des choix courants */
   protected abstract void adjustWidgets();

   /** Retourne l'ordre d'apparition des noms de plans pour chaque CHOICE
    *  Le premier en première place, le deuxième en deuxième place...
    */
   synchronized protected int [] getChoiceOrder() {
      int n=getNb();
      int def[]=new int[n];
      int nbdef=0;

      for (int i=0; i<choicePlan.length && nbdef<n; i++) {
         if (choicePlan[i].selected) def[nbdef++]=i+1;
      }
      return def;
   }

   /** Affichage de la fenêtre */
   @Override
public void show() {
      choicePlan=getPlan();
      int n=getNb();
      int [] def = getChoiceOrder();

      if( this instanceof FrameRGB && def.length==2 ) {
         adjustImageChoice(ch[0], def[0]);
         adjustImageChoice(ch[1], 0);
         adjustImageChoice(ch[2], def[1]);
      } else {
         for (int i=0; i<n; i++) adjustImageChoice(ch[i], i<def.length ? def[i] : 0);
      }
      adjustWidgets();

      if (flagHide) super.show();
      flagHide=false;
   }

   /** Remontée du bouton du ToolBox associé à la Frame */
   protected void toolButtonUp() {
      int n=getToolNumber();
      if( n<0 ) return;
      a.toolBox.setMode(n, Tool.UP);
   }

   /** Mise a jour de la fenetre si necessaire */
   protected void maj() {
      int n = getToolNumber();
      if( n==-1 ) return;
      if( n==-2 || a.toolBox.tool[n].mode==Tool.DOWN ) {
         Plan pi[]=getPlan();
         if( !flagHide && pi.length==choicePlan.length ) return; // A priori inutile
         show();
      } else hide();
   }

   /**
    * Cache la fenetre des Properties et remonte le bouton des properties
    */
   @Override
public void hide() {
      choicePlan=null;
      flagHide=true;
      toolButtonUp();
      super.hide();
   }

   /** Construction du panel des boutons de validation */
   protected JPanel valid() {
      JPanel p=new JPanel();
      p.setLayout(new FlowLayout(FlowLayout.CENTER));
      p.setFont(Aladin.LBOLD);
      JButton b;
      Insets insets = new Insets(4,5,4,5);
      
      JButton [] tb = getAddButtons();
      if( tb!=null ) {
         for( int i=0; i<tb.length; i++ ) {
            p.add(tb[i]);  tb[i].setMargin(insets);
         }
      }
      
      if( tb!=null ) p.add(new JLabel(" - "));
      
      p.add(submitBtn=b=new JButton(SUBMIT));  b.addActionListener(this); b.setMargin(insets);
      p.add(b=new JButton(RESET));   b.addActionListener(this); b.setMargin(insets);
      p.add(b=new JButton(CANCEL));  b.addActionListener(this); b.setMargin(insets);
      p.add(b=Util.getHelpButton(this, getHelp()));
      return p;
   }

   /** Retourne la reference au plan correspondant au Choice (utilise le tableau
    * choicePlan[] pour la correspondance Retourne null si aucun plan n'est
    * selectionne
    */
   protected Plan getPlan(JComboBox c) {
      int i=c.getSelectedIndex()-1;
      if (i<0) return null;
      return choicePlan[i];
   }

   /** Permet de gérer l'action à entreprendre en cas de SUBMIT */
   protected abstract void submit();

   public void actionPerformed(ActionEvent arg0) {
      Object c = arg0.getSource();

      if( c instanceof JButton ) execute( ((JButton)c).getActionCommand() );
      else adjustWidgets();
   }

   // Gestion des evenement
   public void execute(String menu) {
           if (CANCEL.equals(menu)) hide();
      else if (SUBMIT.equals(menu)) submit();
      else if (HELP.equals(menu))   Aladin.info(this, getHelp());
      else if (RESET.equals(menu))  reset();

//      // Ajustement du panel en cas de changement de choix
//      else if (evt.target instanceof Choice
//            || evt.target instanceof Checkbox ) adjustWidgets();

   }

   @Override
protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) hide();
      super.processWindowEvent(e);
   }

}
