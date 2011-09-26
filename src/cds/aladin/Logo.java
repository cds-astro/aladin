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
import java.awt.event.*;
import javax.swing.*;

import cds.tools.Util;


/**
 * Gestion du logo CDS.
 * Il va s'afficher en haut a gauche de l'interface. Si on clique dessus,
 * le browser chargera la home page du CDS.
 * Le deplacement de la souris sur le logo affichera une page
 * de HELP ``pour demarrer''
 *
 * <BR><B>Rq:</B> en cas de fonctionnement standalone, le fichier du logo
 * sera cherche dans le repertoire d'installation d'AladinJava
 *
 * @See Aladin.HOME
 * @version 1.1 (19 mai 99) Generation d'un message special au demarrage
 *               si des donnees sont entrain d'etre chargees
 * @version 1.0 (4 mai 99) Toilettage du code
 * @version 0.9 Creation
 */
public final class Logo extends JComponent implements MouseListener {

   static final int W=90;                // Taille du logo
   static final int H=45;                // Hauteur du logo
   static final Dimension DIM = new Dimension(W,H);
   static final int DF=20;               // Taille de la font si le logo n'est pas accessible
   Image img=null;                       // L'image du logo
   Aladin aladin;                        // Reference
   boolean first=true;                   // Pour reperer le premier passage
   boolean mem_inHelp=false;             // Memorisation de l'etat du Help

 /** Creation du logo.
   * Procede par importation via HTTP ou par lecture
   * de fichier d'une image GIF
   * Rq : img est mis a jour
   * @param aladin reference a la hierarchie objet
   */
   protected Logo(Aladin aladin) {
      this.aladin=aladin;
      img = aladin.getImagette("logo.gif");
      addMouseListener(this);
      addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            consumeKey(e.getKeyChar());
         }
      });
      Util.toolTip(this, aladin.chaine.getString("TIPLOGO"));
   }

   public Dimension getPreferredSize() { return DIM; }
   public Dimension getMinimumSize() { return DIM; }
   public Dimension getMaximumSize() { return DIM; }
   public Dimension getSize() { return DIM; }

 /** Entree de la souris dans le logo.
   * Lorsque l'on rentre dans le logo, le curseur se change en petite main
   * afin de signifier que l'icone est cliquable
   */
   public void mouseEntered(MouseEvent e) {
      requestFocusInWindow(true);
      Aladin.makeCursor(this,Aladin.HANDCURSOR);
   }

 /** Sortie de la souris du logo.
   * Lorsque l'on sort du le logo, le curseur reprend sa forme initiale
   */
   public void mouseExited(MouseEvent e) {
      Aladin.makeCursor(this,Aladin.DEFAULT);
   }

 /** Clique sur le logo.
   * Cliquer dans le logo entraine l'appel a la marque GLU
   * Aladin.java.home
   */
   public void mousePressed(MouseEvent e) {
      aladin.glu.showDocument("Aladin.java.home","");
   }

  /** Changement du niveau de trace */
   public void consumeKey(char key) {
//      if( key=='h' ) PlanBG.switchHealpixMode();
      if( key=='d' ) {
         int n=Aladin.levelTrace;
         n++;
         if( n>Aladin.MAXLEVELTRACE ) n=0;
         aladin.setTraceLevel(n);
      }
      // pour rire
      else if(key=='t') aladin.view.taquin("3");
   }

 /** Gestion de l'affichage.
   * Dessine le logo apres avoir mise en place le fond
   * Si le logo est innacessible, se contente d'ecrire en grand
   * les lettres CDS
   */
   public void paintComponent(Graphics g) {
      if( first ) {
         first=false;
         g.setColor(getBackground());
         g.fillRect(0,0,W,H);
      }
      if( img!=null ) g.drawImage(img,W-90,0,this);
      else {
         g.setFont(Aladin.LLITALIC);
         g.setColor( Color.darkGray );
         g.drawString("CDS",W/2-30,H/2+DF/2);
      }
   }

 /** Generation du texte du Help.
   * Genere le Help qui va apparaitre lorsque l'utilisateur
   * deplace la souris sur le logo
   *
   * @Return : La chaine du Help (String)
   */
   protected String Help() {

      return (Aladin.OUTREACH|Aladin.BETA|Aladin.PROTO? 
         "!"+Aladin.FULLTITRE+" - "+Aladin.getReleaseNumber()+"\n"
            +aladin.chaine.getString(Aladin.OUTREACH?"PUBOUTREACH":
               Aladin.PROTO?"PUBPROTO":"PUBBETA") :

         "! \n!"+Aladin.FULLTITRE+" - "+Aladin.getReleaseNumber()+"\n"
         	+aladin.chaine.getString("PUB"))+"\n*"+Aladin.COPYRIGHT;
   }

 /** Generation du texte du Help.
   * Genere le Help qui va apparaitre lorsque l'utilisateur
   * lance Aladin Java avec un chargement immediat d'une image ou de donnees
   *
   * @Return : La chaine du Help (String)
   */
   protected String inProgress() {
      return ("! \n!"+Aladin.FULLTITRE+" - "+Aladin.getReleaseNumber()+"\n"
                +aladin.chaine.getString("PUBINPROG"))+"\n*"+Aladin.COPYRIGHT;
   }

   public void mouseClicked(MouseEvent e) { }
   public void mouseReleased(MouseEvent e) { }

}
