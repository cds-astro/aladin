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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import cds.aladin.prop.PropPanel;
import cds.tools.Util;

/**
 * Pour gérer l'historique des cibles successives et pouvoir y revenir facilement
 * Format d'enregistrement: [label :] coord [FOX] | object
 * exemple => Ici : 1 2 3 +4 5 6 ICRS
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (jul 2022) Ajout du formulaire de saisie et du label en préfixe
 * @version 1.0 : (oct 2017) Creation
 */
public class TargetHistory {
   
   static final String SEP = ": ";          // La chaine séparant le label de la position
   static final String LAST = "Last"+SEP;   // Le préfixe complet pour la dernière position
   
   private Aladin aladin;
   private ArrayList<String> list;           // L'historique des positions
   private FrameMemoLoc frameMemoLoc=null;   // Le formulaire de saisie
   
   protected TargetHistory(Aladin aladin) {
      this.aladin = aladin;
      list = new ArrayList<>();
   }

   /** Fournit la liste de  l'historique des position (tel que) */
   protected ArrayList<String> getList() { return list; }
   
   /** Retourne le label (optionnel) en préfixe d'un target, null sinon
    * ex: Ma position: 1 2 3 +4 5 6 ICRS => "Ma position"
    */
   static protected String getLabel( String target ) {
      if( target==null ) return null;
      int i = target.indexOf(SEP);
      if( i<0 ) return null;
      return target.substring(0,i).trim();
   }
   
   /** Retourne la position d'un target = supprime un éventuel préfixe, null sinon
    * ex: Ma position: 1 2 3 +4 5 6 ICRS  => "1 2 3 +4 5 6 ICRS"
    */
   static protected String getLoc( String target ) {
      if( target==null ) return null;
      int i = target.indexOf(SEP);
      if( i<0 ) return target;
      return target.substring(i+SEP.length()).trim();
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
      if( label!=null && label.length()>0 ) target=label+SEP+loc;
      
      list.add( target );
   }
   
   /** Conversion de coordonnées sexagésimales avec séparateur 'espace'
    * en expression sexa avec séparateur ':' */
   private String sexaCommaSep(String loc) {
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
   
   /** Ajoute sur la liste la position courante avec le préfixe LAST */
   protected void setCurrentPos(String loc) {
      removeCurrentPos();
      list.add(LAST+loc);
   }
   
   /** Supprime le target "Last : ..." */
   private void removeCurrentPos() { removeLabel(LAST); }
   
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
   private int findLoc( String target ) {
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
   private int findLabel( String target ) {
      String label = getLabel( target);
      if( label==null ) return -1;
      for( int i=0; i<list.size(); i++ ) {
         if( label.equals( getLabel( list.get(i) )) ) return i;
      }
      return -1;
   }
   
   /** Supprime le suffixe Fox si non nécessaire */
   protected String removeFox(String loc) {
      String fox = " "+aladin.localisation.getFrameFox();
      if( loc!=null && loc.endsWith(fox) ) loc=loc.substring(0,loc.length()-fox.length());
      return loc;
   }
   
   /** Retourne la dernière target mémorisée sans le suffixe Fox si pas nécessaire*/
   protected String getLast() {
      if( list.size()==0 ) return "";
      String s = getLoc( list.get( list.size()-1 ) );
      return removeFox(s);
   }
   
   /** Retourne une liste de nb targets à partir de l'indice index. l'index 0 est celui
    * de la dernière target insérée, 1 pour l'avant-dernière, etc...
    * @param index
    * @param nb
    * @return
    */
   protected ArrayList<String> getTargets4Menu(int index, int nb) {
      ArrayList<String> a = new ArrayList<>(nb);
      int n=list.size()-1-index;
      for( int i=0; i<nb && n>=0; i++, n-- ) {
         String target = list.get(n);
         a.add( target );
      }
      if( n>0 ) a.add("...");
      return a;
   }
   
   /** Creation et affichage du formulaire de saisie */
   protected void createFrame(String loc, String name) {
      if( frameMemoLoc==null ) frameMemoLoc=new FrameMemoLoc(aladin);
      else frameMemoLoc.setVisible(true);
      frameMemoLoc.set(loc,name);
   }
   
   /**
    * Petit formulaire pour la mémorisation d'une position spécifique
    */
   class FrameMemoLoc extends JFrame {
      
      protected Aladin aladin;
      
      private JTextField fieldLoc;     // Le champ de saisie de la position
      private JTextField fieldLabel;   // Le nom associé à cette position
      
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
         final JTextField t = fieldLabel;
         SwingUtilities.invokeLater(new Runnable() {
              public void run() {
                  t.grabFocus();
                  t.requestFocus();
              }
         });
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
         
         // La position
         label = new JLabel( aladin.chaine.getString("MEMOLOC")+": " );
         label.setFont( label.getFont().deriveFont(Font.BOLD));
         p3 = new JPanel( new GridLayout(1,2) );
         fieldLoc = new JTextField(25);
         p3.add( fieldLoc );
         PropPanel.addCouple(p,label,p3, g,c);
         
         // Le label
         label = new JLabel( aladin.chaine.getString("MEMOLOCNAME")+": " );
         label.setFont( label.getFont().deriveFont(Font.BOLD));
         p3 = new JPanel( new GridLayout(1,2) );
         fieldLabel = new JTextField(25);
         fieldLabel.addKeyListener( new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
               if( e.getExtendedKeyCode()==KeyEvent.VK_ENTER
                     && fieldLoc!=null && fieldLoc.getText().length()>1 ) {
                  submit();
               }
            }
         });
         p3.add( fieldLabel );
         PropPanel.addCouple(p,label,p3, g,c);

         return p;
      }
      
      // Positionne les valeurs des champs dans le formulaire
      void set(String loc, String label) {
         fieldLoc.setText(loc);
         fieldLabel.setText(label);
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
            public void actionPerformed(ActionEvent e) { submit(); }
         });

         p.add( b=new JButton(aladin.chaine.getString("SFCANCEL"))); 
         b.addActionListener( new ActionListener() { 
            public void actionPerformed(ActionEvent e) { 
               frameMemoLoc=null;
               dispose(); }
         });
         
         p.add( b=new JButton(aladin.chaine.getString("MEMOCLEAN"))); 
         b.addActionListener( new ActionListener() { public void actionPerformed(ActionEvent e) {
            resetList();
            frameMemoLoc=null;
            dispose(); }
         });
         
         JButton h = Util.getHelpButton(this,aladin.chaine.getString("MEMOLOCHELP"));
         p.add(h);
         
         return p;
      }
      
      private void submit() {
         String label = fieldLabel.getText().trim();
         String loc   = fieldLoc.getText().trim();
         
         // Utilise-t-on déjà le même label, mais avec une localisation différente ?
         if( label.length()>0 ) {
            int indiceLabel = findLabel(label+SEP);
            if( indiceLabel>=0 ) {
               String s = getLoc( list.get(indiceLabel) );
               if( !loc.equals(s) ) {
                  if( !aladin.confirmation(this, aladin.chaine.getString("MEMOLOCDUP"))) return;
               }
            }
         }
         
         String target= loc;
         if( label.length()>0 ) target = label+SEP+loc;
         aladin.targetHistory.add( target );
         
         frameMemoLoc=null;
         dispose();
      }

      // Reset de tout l'historique
      private void resetList() {
         if( !aladin.confirmation(this,aladin.chaine.getString("MEMOLOCCONFRESET")) ) return;
         list.clear();
      }
   }

}
