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

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cds.tools.Util;

/** Genère un Label, éventuellement sur plusieurs lignes, qui peut avoir à la fin un lien (more...) pour de l'info
 * additionnel, ou une url complète associée. Les deux simultanément ne sont pas possibles.
 * @Version 2.0 Dec 2016 - classe extraite de Properties.java
 * @Author P.Fernique [CDS]
 */
class MyAnchor extends JLabel {
   private String url;
   private Aladin aladin;

   /**
    * @param text Texte du baratin (ou null si début du texte supplémentaire à afficher)
    * @param width nombre de caractères avant repli (-1 si pas de repli), ou césure si text==null
    * @param more texte supplémentaire accessible par (more...), null sinon
    * @param url url associée, null sinon
    */
   MyAnchor(Aladin aladin,String text,int width, String more,final String url) {
      super();
      
      this.aladin = aladin;

      if( text==null && more==null && url!=null ) text=url;

      if( text==null && more!=null ) {
         if( more.length()>width ) {
            int n = more.lastIndexOf(' ',width);
            if( n<=0 ) n=width;
            text=more.substring(0,n); //+"...";
         }
         else { text=more; more=null; }
      }
      if( text==null ) text="";
      this.url=url;
      if( width>0 ) {
         if( (text.startsWith("http://") || text.startsWith("ftp://")) && text.length()>width ) text=text.substring(0,width)+"...";
         else {
            if( url!=null ) text = Util.fold(text,width,true);
            else if( more==null && text.length()>width ) { more=text; text=text.substring(0,width); }
            else text = Util.fold(text,width);
         }
      }
      if( url!=null ) {
         text = "<html><A HREF=\"\">"+text+"</A></html>";
//         setToolTipText(url);
      }
      if( more!=null ) {
         text = "<html>"+text+" <A HREF=\"\">...</A></html>";
      }
      if( url!=null ) setToolTipText(url);
      else if( more!=null && more.startsWith("http") ) setToolTipText(more);
      
      setText(text);
      setFont(getFont().deriveFont(Font.ITALIC));
      final String more1 = more;
      if( url!=null || more!=null ) {
         final Component c = this;
         addMouseMotionListener(new MouseMotionListener() {
            public void mouseMoved(MouseEvent e) { Aladin.makeCursor(c,Aladin.HANDCURSOR); }
            public void mouseDragged(MouseEvent e) { }
         });
         addMouseListener(new MouseListener() {
            public void mouseReleased(MouseEvent e) {
               if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 ) return;
               if( url!=null ) showDocument(url);
               else showInfo(c,more1.replace("\\n","\n"));
            }
            public void mousePressed(MouseEvent e)  { 
               if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 ) {
                  showPopMenu(e.getX(),e.getY());
               }
            }
            public void mouseExited(MouseEvent e)   { Aladin.makeCursor(c,Aladin.DEFAULTCURSOR); }
            public void mouseEntered(MouseEvent e)  { }
            public void mouseClicked(MouseEvent e) { }
         });
      }
   }
   
   /**
    * Texte suivi d'un " ..." auquel on a associé une action qui sera effectué
    * si on clique sur le "..." ou le texte
    * @param aladin
    * @param text
    * @param flagMore true si on veut un "...", false pour avoir un lien souligné directement (les caractères
    *          non alphanumériques ne sont pas soulignés)
    * @param action
    */
   MyAnchor(Aladin aladin,String text, final ActionListener action) { this(aladin,text,true,action); }
   MyAnchor(Aladin aladin,String text, boolean flagMore, final ActionListener action) {
      super();
      if( flagMore ) {
         text =  "<html>"+text+" <A HREF=\"\">...</A></html>";
      } else {
         int i=0;
         int n = text.length();
         while( !Character.isLetterOrDigit( text.charAt(i) ) && i<n) i++;
         int j=n-1;
         while( !Character.isLetterOrDigit( text.charAt(j) ) && j>0) j--;
         j++;
         if( i>=j ) text = "<html> <A HREF=\"\">"+text+"</A></html>";
         else text = "<html>"+text.substring(0,i)+"<A HREF=\"\">"+text.substring(i,j)+"</A>"+text.substring(j)+"</html>";
      }
      setText(text);
      setFont(getFont().deriveFont(Font.ITALIC));
      final Component c = this;
      addMouseMotionListener(new MouseMotionListener() {
         public void mouseMoved(MouseEvent e) { Aladin.makeCursor(c,Aladin.HANDCURSOR); }
         public void mouseDragged(MouseEvent e) { }
      });
      addMouseListener(new MouseListener() {
         public void mouseReleased(MouseEvent e) {
            if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 ) return;
            action.actionPerformed(null);
         }
         public void mousePressed(MouseEvent e)  { }
         public void mouseExited(MouseEvent e)   { Aladin.makeCursor(c,Aladin.DEFAULTCURSOR); }
         public void mouseEntered(MouseEvent e)  { }
         public void mouseClicked(MouseEvent e) { }
      });
   }
   
   /**
    * Texte sous forme de lien auquel on a associé une action qui sera effectué si on clique dessus
    */
   MyAnchor(String text, final ActionListener action) {
      super();
      text = "<html>"+text+" <A HREF=\"\">...</A></html>";
      setText(text);
      setFont(getFont().deriveFont(Font.ITALIC));
      final Component c = this;
      addMouseMotionListener(new MouseMotionListener() {
         public void mouseMoved(MouseEvent e) { Aladin.makeCursor(c,Aladin.HANDCURSOR); }
         public void mouseDragged(MouseEvent e) { }
      });
      addMouseListener(new MouseListener() {
         public void mouseReleased(MouseEvent e) {
            if( (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0 ) return;
            action.actionPerformed(null);
         }
         public void mousePressed(MouseEvent e)  { }
         public void mouseExited(MouseEvent e)   { Aladin.makeCursor(c,Aladin.DEFAULTCURSOR); }
         public void mouseEntered(MouseEvent e)  { }
         public void mouseClicked(MouseEvent e) { }
      });
   }
   


   
   // Affiche dans un navigateur Web
   private void showDocument(String url) { aladin.glu.showDocument(url); }
   
   // Affiche dans un fenêtre popup, à moins que ce soit une URL, alors dans un navigateur
   private void showInfo(Component c, String s) { 
      try {
         new URL(s);
         showDocument(s);
      } catch( Exception e ) {
         aladin.info(c,s); 
      }
   }
   
   // Affiche le Menu popup
   private void showPopMenu(int x,int y) {
      JPopupMenu popMenu = new JPopupMenu();
      popMenu.setLightWeightPopupEnabled(false);
      JMenuItem j=new JMenuItem(aladin.chaine.getString("MFCOPYURL"));
      popMenu.add(j);
      j.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            aladin.copyToClipBoard(url);
         }
      });
      popMenu.show(this,x,y);
   }
}

