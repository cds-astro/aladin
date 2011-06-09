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

import javax.swing.JPanel;


/**
 * Un Label sur mesure pour Aladin.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (20 mars 01) Rupture des lignes > 60 caracteres
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class MyLabel extends JPanel {
   static final int MARGE=10;

   Vector line=null;	// Le texte a afficher (par ligne)
   Font font;		// Font associee
   FontMetrics fm;	// La metrique de la font courante
   int mode;		// mode d'alignement Label.CENTER,...
   String text;

  /** Creation d'un label multi-lignes VIDE. */
   protected MyLabel() { super(); }

  /** Creation d'un label multi-lignes AVEC les options par defaut.
   * @param text Le texte du label
   */
   protected MyLabel(String text) { this(text,Label.CENTER,Aladin.PLAIN); }

  /** Creation d'un label multi-lignes AVEC la fonte par defaut.
   * @param text Le texte du label
   * @param mode <I>Label.CENTER</I>, <I>Label.RIGHT</I> ou <I>Label.LEFT</I>
   */
   protected MyLabel(String text,int mode) { this(text,mode,Aladin.PLAIN); }

  /** Creation d'un label multi-lignes.
   * @param text Le texte du label
   * @param mode <I>Label.CENTER</I>, <I>Label.RIGHT</I> ou <I>Label.LEFT</I>
   * @param font la font a utiliser
   */
   public MyLabel(String text,int mode,Font font) {
      this.mode = mode;
      this.font=font;
      setFont(font);
      fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
      dim = new Dimension(10,10);
      setText(text);
   }

  /** Recuperation du texte */
   public String getText() { return text; }

   /** Pour une �ventuelle surcharge - voir Copyright par exemple */
   protected String pubNews(String text) { return text; }

  /** Modification du texte du Label.
   * @param text Le nouveau texte du label
   */
   public void setText(String text) {
      text = pubNews(text);
      if( this.text!=null && this.text.equals(text) ) return;
      
      this.text=text;
      String s;
      
      StringTokenizer st = new StringTokenizer(text,"\n");
      int w,max=0;

      line = new Vector(10);
      while( st.hasMoreTokens() ) {
         s = st.nextToken();
         StringTokenizer st1 = new StringTokenizer(s," ",true);
         StringBuffer s1=new StringBuffer();
         while( st1.hasMoreTokens() ) {
            s1.append(st1.nextToken());
            if( s1.length()>80 ) {
               line.addElement(s1.toString());
               w = fm.stringWidth(s1.toString());
               if( max<w ) max=w;
               s1 = new StringBuffer();
            }
         }
         if( s1.length()>0 ) {
            line.addElement(s1.toString());
            w = fm.stringWidth(s1.toString());
            if( max<w ) max=w;
         }
      }
      if( mode==Label.RIGHT ) max+=MARGE;
      int h=line.size()*fm.getHeight()+fm.getDescent();
//      int h=line.size()*Aladin.GETHEIGHT+3;	// Cochonnerie de JAVA
      dim = new Dimension(max+4,h);
//      resize(max+10,h);
      validate();
      reaffiche();
   }

   private Dimension dim;
   public Dimension getPreferredSize() { return dim; }

   boolean flagReaffiche=false;

   // Gere l'effacement avant le repaint();
   protected void reaffiche() {
      flagReaffiche=true;
      repaint();
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      if( line==null ) return;
      String s;
      int i;
      int w,x;
      int y=fm.getHeight();
//      int y=Aladin.GETHEIGHT;	// Cochonnerie de JAVA
      int n=line.size();
      int width = getSize().width;
      int height = getSize().height;

      if( flagReaffiche ) {
         g.setColor( getBackground() );
         g.fillRect(0,0,width,height);
         flagReaffiche=false;
         g.setColor( getForeground() );
      }

      for( i=0; i<n; i++ ) {
         if( i>=line.size() ) {
             break;
         }
         s=(String)line.elementAt(i);
         w=fm.stringWidth(s);
         x=(mode==Label.LEFT)?0:(mode==Label.RIGHT)?width-w-MARGE:width/2-w/2;
         if(x<0 ) x=0;
         g.drawString(s,x,y);
         y+=fm.getHeight();
      }
   }
}
