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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.JComponent;

import cds.tools.Util;

/**
 * Affichage des logos du controleur de vues + le bouton de synchronisation des vues
 * @author Pierre Fernique [CDS]
 */
public final class ViewControl extends JComponent implements
                  MouseMotionListener, MouseListener
                  {
   static final int MVIEW1  = 1;
   static final int MVIEW2C = 2;
   static final int MVIEW2L  = 3;
   static final int MVIEW4  = 4;
   static final int MVIEW9  = 9;
   static final int MVIEW16 = 16;
   static final int DEFAULT = MVIEW1;
   static final int[] MODE = { MVIEW1,MVIEW2C,MVIEW2L,MVIEW4,MVIEW9,MVIEW16 };
   static final int MAXVIEW = MVIEW16;
   
   String INFOMVIEW,INFOSYNC,LABEL;
   static final int SL = 18;     // Taille du sync
   static final int L = 12;      // Taille d'un logo
   static final int H = 24;      // Hauteur de la fenetre
   static final int W  = (L+2)*MODE.length+2;

   protected int modeView=DEFAULT;    // Mode courant
   private int nMode=-1;              // Dernière position de la souris

   Aladin aladin;

  /** Creation de l'element du split.
   * @param aladin reference
   */
   protected ViewControl(Aladin aladin) {
      this.aladin=aladin;
      INFOMVIEW = aladin.chaine.getString("MVIEWDESC");
      LABEL = aladin.chaine.getString("MVIEWLABEL");
      addMouseMotionListener(this);
      addMouseListener(this);
   }
   
   public Dimension getPreferredSize() { return new Dimension(W,H); }
   
   /** Retourne l'indice du mode d'affichage en fonction du nombre de vues m */
   static protected int getLevel(int m) {
      int level;
      for( level=0; level<MODE.length; level++ ) { if( MODE[level]==m ) return level; }
      return -1;
   }

   /** Retourne le prochain ModeView après m, -1 si on a atteind le max */
   static protected int nextModeView(int m) {
      int level=getLevel(m)+1;
      if( level>=MODE.length ) return -1;
      return MODE[level];
   }

   /** Copie ou déplacement de vues
    *  @param v la liste des vues
    *  @param s,t les indices source et destination
    *  @param flagCopy true s'il s'agit d'une copie
    */
   static protected void moveViewOrder(ViewSimple[] v,int s,int t,boolean flagCopy) {
//System.out.println("Je dois "+(flagCopy?"copier":"déplacer")+" v["+s+"] vers v["+t+"]");
      v[s].copyIn(v[t]);
      if( !flagCopy ) v[s].free();
      setGoodViewNumber(v);
   }

   /** Remise à jour des indices des vues dans les objets ViewSimple */
   static protected void setGoodViewNumber(ViewSimple[] v) {
      for( int i=0; i<MAXVIEW; i++ ) v[i].n=i;
   }
   
   /** Retourne le nombre de lignes de vues en fonction du mode */
   protected int getNbLig() { return getNbLig(modeView); }
   protected int getNbLig(int mode) {
      if( mode==MVIEW2L ) return 1;
      else if( mode==MVIEW2C ) return 2;
      return (int)Math.sqrt(mode);
   }
   
   /** Retourne le nombre de colonnes de vues en fonction du mode */
   protected int getNbCol() { return getNbCol(modeView); }
   protected int getNbCol(int mode) {
      if( mode==MVIEW2L ) return 2;
      else if( mode==MVIEW2C ) return 1;
      return (int)Math.sqrt(mode);
   }

  /** Affichage du panneau de contrôle des vues multiples */
   private void drawLogo(Graphics g) {
      g.setColor( getBackground() );
      g.fillRect(0,0,W,H);
      
      for( int i=0; i<MODE.length; i++ ) {
         int mode = MODE[i];
         boolean down = modeView==mode;
         boolean in = !down && nMode==i;
         int nlig = getNbLig(mode);
         int ncol = getNbCol(mode);         
         int mw = L/ncol;              // entre 2 vignette en absisse
         int mh = L/nlig;              // entre 2 vignette en absisse
         int w = L/ncol-2;             // largeur d'une vignette
         int h = L/nlig-2;             // hauteur d'une vignette
         for( int j =0; j<ncol; j++ ) {
            for( int k=0; k<nlig; k++ ) {
               int x=5+i*(L+2)+j*mw;
               int y=2+k*mh;
               if( down || in ) {
               	  g.setColor( !enabled ? Aladin.MYGRAY : in ? Aladin.MYBLUE : Aladin.DARKBLUE);
               	  g.fillRect(x,y,w,h);
               }
               g.setColor(!enabled ? Aladin.MYGRAY : down || in ?Color.black:Color.white);
               g.drawLine(x,y,x+w,y); g.drawLine(x,y,x,y+h);
               g.setColor(!enabled ? Aladin.MYGRAY : !down || in ?Color.black:Color.white);
               g.drawLine(x+w,y,x+w,y+h); g.drawLine(x,y+h,x+w,y+h);
            }
         }
      }
      g.setColor(!enabled ? Aladin.MYGRAY : Color.black);
      g.setFont(Aladin.SPLAIN);
      g.drawString(LABEL,(W+6)/2-g.getFontMetrics().stringWidth(LABEL)/2,H-2);
   }
   
   /* mémorise le mode de vue multiple courant en fonction de la position
    * du curseur en abscisse */
   private void computeModeView(int x) {
      modeView=MODE[getN(x)];
   }
   
   /* Calcul le mode de vue multiple courant en fonction de la position
    * du curseur en abscisse 
    */
   private int getN(int x) {
      int n = x/(W/MODE.length);
      if( n>=MODE.length ) n=MODE.length-1;
      return n;
   }

   /** Modification "a posteriori" du niveau de modeView sans interaction
    *  avec View, juste pour changer le logo enfoncé
    */
   protected void setModeView(int m) {
      modeView=m;
      repaint();
   }

   public void mouseEntered(MouseEvent e) {
      if( aladin.inHelp ) { aladin.help.setText(Help()); return; }
      Aladin.makeCursor(this,Aladin.HANDCURSOR);
   }
   public void mouseExited(MouseEvent e) {
      Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
      nMode=-1;
      repaint();
   }

   public void mousePressed(MouseEvent e) {
      if( aladin.inHelp ) return;
      
      // Effectue un simple réaffichage du controleur de vue
      // suivant la nouvelle configuration
      computeModeView(e.getX());
      repaint();
   }

   public void mouseReleased(MouseEvent e) {
      if( aladin.inHelp ) { aladin.helpOff(); return; }
      
      // Effectue réellement le changement de nombre de vue
      Aladin.makeCursor(this,Aladin.WAITCURSOR);
      computeModeView(e.getX());
      aladin.view.setModeView(modeView);
      aladin.console.printCommand("modeview "+modeView);
      aladin.toolBox.toolMode();
      Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
      repaint();
   }

  /** On se deplace sur le bouton du split */
   public void mouseMoved(MouseEvent e) {
      if( aladin.inHelp ) return;
      int n = getN(e.getX());
      if( n!=nMode ) { nMode=n; repaint(); }
//      aladin.status.setText(n==-2 ? INFOSYNC : INFOMVIEW);
      Util.toolTip(this,n==-2 ? INFOSYNC : INFOMVIEW);
   }
   
   private boolean enabled=false;

   public void paintComponent(Graphics gr) {
      enabled = !aladin.msgOn;
      drawLogo(gr);
   }
   
  /** Recuperation de la chaine de help */
   protected String Help() { return aladin.chaine.getString("ViewControl.HELP"); }

   public void mouseDragged(MouseEvent e) { }
   public void mouseClicked(MouseEvent e) { }
}
