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

package cds.tools;

import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.JPanel;

/**
 * Un Label sur mesure pour Aladin.
 *
 * @author Pierre Fernique [CDS]
 * @version ??  : january 2002, part of a new package, cds.tools
 * @version 1.1 : (20 mars 01) Rupture des lignes > 60 caracteres
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class CDSLabel extends JPanel implements CDSConstants {
   static final int MARGE=10;

   Vector line=null;	// Le texte a afficher (par ligne)
   Font font;		// Font associee
   FontMetrics fm;	// La metrique de la font courante
   int mode;		// mode d'alignement Label.CENTER,...
   String text;

  /** Creation d'un label multi-lignes VIDE. */
   public CDSLabel() { super(); }

  /** Creation d'un label multi-lignes AVEC les options par defaut.
   * @param text Le texte du label
   */
   public CDSLabel(String text) { this(text,Label.CENTER,PLAIN); }

  /** Creation d'un label multi-lignes AVEC la fonte par defaut.
   * @param text Le texte du label
   * @param mode <I>Label.CENTER</I>, <I>Label.RIGHT</I> ou <I>Label.LEFT</I>
   */
   public CDSLabel(String text,int mode) { this(text,mode,PLAIN); }

  /** Creation d'un label multi-lignes.
   * @param text Le texte du label
   * @param mode <I>Label.CENTER</I>, <I>Label.RIGHT</I> ou <I>Label.LEFT</I>
   * @param font la font a utiliser
   */
   public CDSLabel(String text,int mode,Font font) {
      this.mode = mode;
      this.font=font;
      setFont(font);
      fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
      setText(text);
   }

  /** Recuperation du texte */
   public String getText() { return text; }

  /** Modification du texte du Label.
   * @param text Le nouveau texte du label
   */
   public void setText(String text) {
      String s;
      StringTokenizer st = new StringTokenizer(text,"\n");
      int w,max=0;

      this.text=text;
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
//      int h=line.size()*fm.getHeight()+fm.getDescent();
      int h=line.size()*GETHEIGHT+3;	// Cochonnerie de JAVA
      resize(max,h);
      reaffiche();
   }

   public Dimension preferredSize() { return size(); }
   public Dimension getPreferredSize() { return size(); }
   public Dimension minimumSize() { return size(); }
   public Dimension getMinimumSize() { return size(); }


   boolean flagReaffiche=false;

   // Gere l'effacement avant le repaint();
   private void reaffiche() {
      flagReaffiche=true;
      repaint();
   }

   public void update(Graphics g) { paint(g); }
   public void paint(Graphics g) {
      if( line==null ) return;
      String s;
      int i;
      int w,x;
//      int y=fm.getHeight();
      int y=GETHEIGHT;	// Cochonnerie de JAVA
      int n=line.size();
      int width = size().width;
      int height = size().height;

      if( flagReaffiche ) {
         g.setColor( getBackground() );
         g.fillRect(0,0,width,height);
         flagReaffiche=false;
         g.setColor( getForeground() );
      }

      for( i=0; i<n; i++ ) {
         s=(String)line.elementAt(i);
         w=fm.stringWidth(s);
         x=(mode==Label.LEFT)?0:(mode==Label.RIGHT)?width-w-MARGE:width/2-w/2;
         g.drawString(s,x,y);
//         y+=fm.getHeight();
         y+=GETHEIGHT;	// Cochonnerie de JAVA
      }
   }


}
