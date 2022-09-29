// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.tools.Astrodate;
import cds.tools.Util;

/**
 * Gestion d'une fenêtre d'outils basiques sur les temps
 * - Affichage dans chaque système de temps (TCB, TT, TAI...)
 * Le temps initial peut être donnée soit :
 * - en entrant directement des valeurs dans le formulaire
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (juillet 2021) creation
 */
public class FrameTimeToolbox extends JFrame {
   
   static private final boolean CONVERT =false;   // POUR LE MOMENT? PAS DE CONVERTION

   protected Aladin aladin;
   
   static private final String [] LABELS = { "ISO time","JD","MJD","Julian years","Besselian years" };
   static private final int [] CODE =      { Astrodate.ISOTIME, Astrodate.JD, 
                                             Astrodate.MJD, Astrodate.YEARS, Astrodate.BES };
   static private final int NB = LABELS.length;
   
   
   static private final String [] SYS = { "TCB", "UTC", "TAI", "TT", "UT1", "TDB" }; 
   
   // Les champs de saisies des temps dans les différents formats
   private JTextField [] fromTime = new JTextField[NB];
   
   // Les chmaps d'affichage des temps dans un autre système temporel
   private JTextField [] toTime = new JTextField[NB];
   
   // Les sélecteurs des systèmes from et to
   private JComboBox<String> fromCombo, toCombo;
   
   // Le temps de référence
   private double jdtime = Double.NaN;
   
   protected FrameTimeToolbox(Aladin aladin) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      setTitle(aladin.chaine.getString("TIMETOOL"));
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, true, aladin);
      setLocation( Aladin.computeLocation(this) );
      getContentPane().add( createPanel(), BorderLayout.CENTER);
      getContentPane().add( getPanelBottom(), BorderLayout.SOUTH);
      pack();
      setVisible(true);
   }
   
   public void processWindowEvent(WindowEvent e) {
      if( e.getID() == WindowEvent.WINDOW_CLOSING ) aladin.frameTimeTool=null;
      super.processWindowEvent(e);
   }
   
   // Création du panel des temps dans les différents systèmes
   private JPanel createPanel() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0,0,0,0);

      JPanel p = new JPanel();
      p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      p.setLayout(g);

      // L'entête de la table des temps
      if( CONVERT ) {
         JPanel pHead = new JPanel( );
         pHead.add(new JLabel("Convert date from "));
         fromCombo = new JComboBox<>(SYS);
         fromCombo.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { resume(); }
         });
         JPanel p1 = new JPanel(new BorderLayout() );
         p1.add( fromCombo, BorderLayout.CENTER );
         pHead.add(p1);
         pHead.add(new JLabel(" to "));
         toCombo = new JComboBox<>(SYS);
         toCombo.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { resume(); }
         });

         JPanel p2 = new JPanel(new BorderLayout() );
         p2.add( toCombo, BorderLayout.CENTER );
         pHead.add(p2);
         PropPanel.addCouple(p,new JLabel(),pHead, g,c);
      }

      // La liste des différentes syntaxes
      for( int i=0; i<NB; i++ ) {
         JLabel label = new JLabel( LABELS[i]+": " );
         label.setFont( label.getFont().deriveFont(Font.BOLD));
         JPanel p3 = new JPanel( new GridLayout(1,2) );
         fromTime[i] = new JTextField(15);
         final int index = i;
         fromTime[i].addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { modify(index); }
         });
         p3.add( fromTime[i] );

         if( CONVERT ) {
            toTime[i] = new JTextField(15);
            toTime[i].setEditable(false);
            p3.add( toTime[i] );
         }
         PropPanel.addCouple(p,label,p3, g,c);
      }
      
      return p;
   }
   
   /** Construction du panel des boutons de validation
    * @return Le panel contenant les boutons Apply/Close
    */
   protected JPanel getPanelBottom() {
      JPanel p = new JPanel();
      p.setLayout( new FlowLayout(FlowLayout.CENTER));
      JButton b;
      p.add( b=new JButton(aladin.chaine.getString("CLEAR"))); 
      b.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) { reset(); }} );
      p.add( b=new JButton(aladin.chaine.getString("UPCLOSE"))); 
      b.addActionListener( new ActionListener() { 
         public void actionPerformed(ActionEvent e) { 
            aladin.frameTimeTool=null;
            dispose(); }
      });
      return p;
   }
   
   /** Détermine la date jd dans le système par défaut */
   protected void modify() { modify(0); }
   protected void modify(int from) {
      try {
         String s = fromTime[ from ].getText();
         if( s.trim().length()==0 ) jdtime = Double.NaN;
         else jdtime = Astrodate.parseTime( s, CODE[from] );
      } catch( Exception e ) { }
      resume();
   }
   
   // Remet à jour tout le tableau en fonction des valeurs courantes */
   private void resume() { resume(true); }
   private void resume(boolean flagLog) {


      for( int i=0; i<NB; i++ ) {
         if( Double.isNaN(jdtime) ) {
            fromTime[i].setText("");
            if( CONVERT ) toTime[i].setText(""); 
         } else {
            try {
               String s = Astrodate.editTime(jdtime,  CODE[i]);
               fromTime[i].setText(s);
            } catch( Exception e ) { fromTime[i].setText(""); }
            if( CONVERT ) {
               String fromSys = (String) fromCombo.getSelectedItem();
               String toSys = (String) toCombo.getSelectedItem();
               try {
//                  double jd = Astrodate.convertTime(jdtime, fromSys,toSys);  // PAS ENCORE IMPLANTE
//                  String s1 =  Astrodate.editTime(jd,  CODE[i]);
//                  toTime[i].setText(s1);
               } catch( Exception e ) { toTime[i].setText(""); }
            }
         }
      }      
      if( flagLog ) aladin.glu.log("TimeToolbox","");
   }

   // Reset complet du formulaire
   private void reset() {
      jdtime = Double.NaN;
      resume(false);
   }
}
