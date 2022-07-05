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
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.aladin.prop.PropPanel;
import cds.tools.Util;

/**
 * Pour gérer l'historique des cibles successives et pouvoir y revenir facilement
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (oct 2017) Creation
 */
public class TargetHistory {
   Aladin aladin;
   ArrayList<String> list;
   
   protected TargetHistory(Aladin aladin) {
      this.aladin = aladin;
      list = new ArrayList<>();
   }
   
   /** Retourne le label (optionnel) en préfixe d'un target, null sinon
    * ex: Ma position: 1 2 3 +4 5 6 ICRS => "Ma position"
    */
   static protected String getLabel( String target ) {
      if( target==null ) return null;
      int i = target.indexOf(SEPARATOR);
      if( i<0 ) return null;
      return target.substring(0,i).trim();
   }
   
   /** Retourne la location d'un target = supprime un éventuel préxife, null sinon
    * ex: Ma position: 1 2 3 +4 5 6 ICRS  => "1 2 3 +4 5 6 ICRS"
    */
   static protected String getLoc( String target ) {
      if( target==null ) return null;
      int i = target.indexOf(SEPARATOR);
      if( i<0 ) return target;
      return target.substring(i+SEPARATOR.length()).trim();
   }
   
   /** Ajoute le target à la liste. Ce target peut être ou non préfixé par un nom.
    * Si la localisation existe déjà, supprime l'ancienne définition
    * Si la localisation est spécifié par des coordonnées, elles doivent être
    * suffixé par le frame FOX. Si ce n'est pas le cas, il sera ajouté en fonction
    * du frame courant.
    * @param target : le target à ajouter (ex: ici: 1 2 3 +4 5 6 ICRS)
    * @param check : true s'il faut vérifier qu'il n'y a pas de doublon et que le suffixe est bien mis
    */
   protected void add(String target) { add(target,true); }
   protected void add(String target, boolean check) {
      
      String label = getLabel( target);
      String loc  = getLoc(target);
      if( loc==null || loc.length()==0 ) return;
      
      loc = sexaCommaSep(loc);
      
      if( check ) {
         // Ajout si nécessaire du frame FOX en suffixe
         if( !Command.isDateCmd(loc) && !Localisation.notCoord(loc) && !Localisation.hasFoxSuffix(loc) ) {
            loc = loc+" "+aladin.localisation.getFrameFox();
         }

         // Suppression des précédentes occurences possibles de la position (sans tenir compte du nom)
         removeLoc(target);

         // Suppression des précédentes occurences possibles du label
         removeLabel(target);
      }
      
      target = loc;
      
      // Ajout en préfixe d'un nom (optionel)
      if( label!=null && label.length()>0 ) target=label+SEPARATOR+loc;
      
      list.add( target );
   }
   
   /** Conversion de coordonnées sexagésimales avec séparateur 'espace'
    * en expression sexa avec séparateur ':' */
   static private String sexaCommaSep(String loc) {
      if( Localisation.notCoord(loc) ) return loc;
      
      String fox="";
      String loc1=loc;
      if( Localisation.hasFoxSuffix(loc) ) {
         int i = loc.lastIndexOf(' ');
         fox = loc.substring(i);
         loc = loc.substring(0,i);
      }
      if( (new Tok(loc," ").countTokens()<3 ) ) return loc1;
      
      int j = loc.indexOf('+');
      if( j<0 ) j=loc.indexOf('-');
      
      Tok tok = new Tok(loc.substring(0,j)," ");
      StringBuilder res1 = new StringBuilder();
      while( tok.hasMoreTokens() ) {
         if( res1.length()>0 ) res1.append(':');
         res1.append(tok.nextToken());
      }
      tok = new Tok(loc.substring(j)," ");
      StringBuilder res2 = new StringBuilder();
      while( tok.hasMoreTokens() ) {
         if( res2.length()>0 ) res2.append(':');
         res2.append(tok.nextToken());
      }
      
      return res1+" "+res2+fox;
   }
   
   protected void memoTarget(String target) {
      createFrame(target,"");
   }
   
   static final String SEPARATOR = ": ";
   static final String CURRENTPOS = "Last"+SEPARATOR;
   
   protected void setCurrentPos(String loc) {
      removeCurrentPos();
      list.add(CURRENTPOS+loc);
   }
   
   /** Supprime le target "Last : ..." */
   private void removeCurrentPos() { removeLabel(CURRENTPOS); }
//      int i;
//      for( i=0; i<list.size(); i++ ) {
//         if( list.get(i).startsWith(CURRENTPOS) ) break;
//      }
//      if( i<list.size() ) list.remove(i);
//   }
   
   /** Supprime toutes les occurences de la position du target (sans tenir compte
    * d'un éventuel nom en préfixe)
    * @return true si au-moins une occurence a été supprimé
    */
   protected boolean removeLoc( String target ) {
      boolean rep=false;
      int i;
      while( (i = findLoc( target ))>=0 ) { list.remove(i); rep=true; }
      return rep;
   }
   
   /** Retourne l'indice de la première occurence de la position du target (sans tenir
    * compte d'un éventuel nom en préfixe) */
   protected int findLoc( String target ) {
      String loc = getLoc( target);
      for( int i=0; i<list.size(); i++ ) {
         if( getLoc( list.get(i) ).equals( loc ) ) return i;
      }
      return -1;
   }
   
   /** Supprime toutes les occurences du label du target (sans tenir compte
    * de la position associée)
    * @return true si au-moins une occurence a été supprimé
    */
   protected boolean removeLabel( String target ) {
      boolean rep=false;
      int i;
      while( (i = findLabel( target ))>=0 ) { list.remove(i); rep=true; }
      return rep;
   }
   
   /** Retourne l'indice de la première occurence du label (sans tenir
    * de la position associée) - case insensitive  */
   protected int findLabel( String target ) {
      String label = getLabel( target);
      if( label==null ) return -1;
      for( int i=0; i<list.size(); i++ ) {
         if( label.equalsIgnoreCase( getLabel( list.get(i) )) ) return i;
      }
      return -1;
   }
   
   protected int size() { return list.size(); }
   
   /** Retourne la dernière target mémorisée */
   protected String getLast() { return list.size()==0 ? "" : getLoc( list.get( list.size()-1 ) ); }
   
   /** Retourne une liste de nb targets à partir de l'indice index. l'index 0 est celui
    * de la dernière target insérée, 1 pour l'avant-dernière, etc...
    * @param index
    * @param nb
    * @return
    */
   protected ArrayList<String> getTargets(int index, int nb) {
      ArrayList<String> a = new ArrayList<>(nb);
      int n=list.size()-1-index;
      for( int i=0; i<nb && n>=0; i++, n-- ) {
         String target = list.get(n);
         a.add( target );
      }
      if( n>0 ) a.add("...");
      return a;
   }
   
   
   private FrameMemoLoc frameMemoLoc=null;
   
   private void createFrame(String loc, String name) {
      if( frameMemoLoc==null ) frameMemoLoc=new FrameMemoLoc(aladin);
      else frameMemoLoc.setVisible(true);
      frameMemoLoc.set(loc,name);
   }
   
   /**
    * Petit formulaire pour la mémorisation d'une position spécifique
    *
    * @author Pierre Fernique [CDS]
    * @version 1.0 : (juillet 2022) creation
    */
   class FrameMemoLoc extends JFrame {
      
      protected Aladin aladin;
      
      private JTextField fieldLocation;   // Le champ de saisie de la position
      private JTextField fieldName;       // Le nom associé à cette position
      
      protected FrameMemoLoc(Aladin aladin) {
         super();
         this.aladin = aladin;
         Aladin.setIcon(this);
         setTitle(aladin.chaine.getString("MEMOLOCTITLE"));
         enableEvents(AWTEvent.WINDOW_EVENT_MASK);
         Util.setCloseShortcut(this, true, aladin);
         setLocation( Aladin.computeLocation(this) );
         getContentPane().add( createPanel(), BorderLayout.CENTER);
         getContentPane().add( getPanelBottom(), BorderLayout.SOUTH);
         pack();
         setVisible(true);
      }
      
      public void processWindowEvent(WindowEvent e) {
         if( e.getID() == WindowEvent.WINDOW_CLOSING ) frameMemoLoc=null;
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
         
         
         JLabel label;
         JPanel p3;
         
         label = new JLabel( aladin.chaine.getString("MEMOLOCNAME")+": " );
         label.setFont( label.getFont().deriveFont(Font.BOLD));
         p3 = new JPanel( new GridLayout(1,2) );
         fieldName = new JTextField(25);
         p3.add( fieldName );
         PropPanel.addCouple(p,label,p3, g,c);
         
         label = new JLabel( aladin.chaine.getString("MEMOLOC")+": " );
         label.setFont( label.getFont().deriveFont(Font.BOLD));
         p3 = new JPanel( new GridLayout(1,2) );
         fieldLocation = new JTextField(25);
         p3.add( fieldLocation );
         PropPanel.addCouple(p,label,p3, g,c);

         return p;
      }
      
      void set(String loc, String name) {
         fieldLocation.setText(loc);
         fieldName.setText(name);
      }
      
      /** Construction du panel des boutons de validation
       * @return Le panel contenant les boutons Apply/Close
       */
      protected JPanel getPanelBottom() {
         JPanel p = new JPanel();
         p.setLayout( new FlowLayout(FlowLayout.CENTER));
         JButton b;
         p.add( b=new JButton(aladin.chaine.getString("MEMOSUBMIT"))); 
         b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { 
               submit(); 
               frameMemoLoc=null;
               dispose();
            }
         });
         p.add( b=new JButton(aladin.chaine.getString("CLEAR"))); 
         b.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) { reset(); }} );
         p.add( b=new JButton(aladin.chaine.getString("UPCLOSE"))); 
         b.addActionListener( new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
               frameMemoLoc=null;
               dispose(); }
         });
         
         JButton h = Util.getHelpButton(this,aladin.chaine.getString("MEMOLOCHELP"));
         p.add(h);
         
         return p;
      }
      
      private void submit() {
         String name = fieldName.getText().trim();
         String loc = fieldLocation.getText().trim();
         
         // Utilise-t-on déjà le même label, mais avec une localisation différente ?
         if( name.length()>0 ) {
            int indiceLabel = findLabel(name+SEPARATOR);
            if( indiceLabel>=0 ) {
               String s = getLoc( list.get(indiceLabel) );
               if( !loc.equals(s) ) {
                  if( !aladin.confirmation(this, aladin.chaine.getString("MEMOLOCDUP"))) return;
               }
            }
         }
         
         String target= loc;
         if( name.length()>0 ) target = name+SEPARATOR+loc;
         aladin.targetHistory.add( target );
      }

      // Reset complet du formulaire
      private void reset() {
         fieldLocation.setText("");
         fieldName.setText("");
      }
   }

}
