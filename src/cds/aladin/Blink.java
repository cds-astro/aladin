// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
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

/**
 *  Le blinking consiste � d�signer une source pour la rep�rer ais�ment.
 *  La m�thode consiste � faire des XOR de carr�s de plus en plus grands
 *  puis de repasser dessus afin de les effacer.
 *  L'int�r�t de la m�thode est qu'il est inutile de m�moriser ce qu'il
 *  y a sous la source pour restituer la source apr�s l'avoir montr�e.
 *  Sa difficult� consiste � faire un nombre pair de XOR, malgr� les
 *  affichages asynchrones.
 *
 * 1. La souris rentre dans une source dans la vue n
 *    (ou respectivement en sort)
 *  - appel � la fonction View.showSource(Source s) qui va transmettre
 *    � toutes les vues que la nouvelle source � montrer est s (s null
 *    si sort d'une source) => ViewSimple.changeBlinkSource(Source s)
 *  - Lance le thread de blinking si ce n'est d�j� fait, sinon
 *    lui passe une interruption afin que la prise en compte
 *    soi imm�diate.
 *
 * 2. Thread de blinking (View.runC())
 *  - incr�mente le mode blink (View.modeBlink)
 *  - demande le repaint (blinkRepaint()) pour toutes les vues
 *      qui ont quelques choses � faire (au-moins une source
 *      � montrer ou � restituer)
 *  - si aucune, s'arr�te sinon attend 0.3 secondes et boucle
 *
 * 3. La modification d'une source � montrer pour une vue
 *  - cherche dans sa liste de Blinks, toutes les sources
 *    qu'il faut restituer et en fait la demande
 *  - si dans la liste de Blinks (ViewSimple.showSource) dont il dispose
 *    il y a un Blink libre, il l'utilise pour ins�rer la nouvelle source
 *    � montrer et demande son clignotement
 *  - s'il n'y a plus de place dans la liste des Blinks, il
 *    ajoute un nouveau Blink d�crivant la nouvelle source
 *    � montrer
 *
 * 3. Le repaint des vues en mode blink
 *  - restitue ou montre les sources qui doivent l'�tre
 *    (parcours du Vector showSource contenant des objets Blink
 *
 * 4. Le paint d'un blink
 *  - Un Blink dispose de 4 �tats (UNBLINK,START,BLINK et STOP)
 *    START et STOP peuvent �tre demand� par start() et stop()
 *    UNBLINK correspond � un Blink libre
 *    BLINK correspond � l'�tat de clignotement
 *  - L'�tat d'un blink change de fait uniquement lors de l'appel
 *    � paint(Graphics g) car il doit disposer du contexte graphique
 *    pour op�rer.
 *    *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (11 mai 2004) cr�ation
 */
public final class Blink {
   static private final int L=2;    // Taille du premier carr�
   static protected int STEP=3;     // Nombre de carr�s autour de la source

   static private final int START  =1;       // Mode de d�marrage du blink
   static private final int STOP   =2;       // Mode d'arr�t du blink
   static private final int NOBLINK=3;       // Mode "libre"
   static private final int BLINK=4;         // Mode de blinking


   private int mode;                // Mode courant du blink
   private boolean step[];          // Etat de chaque carr� XOR (true-pair, false-impair)

   private ViewSimple v;            // Vue � laquelle appartient le Blink
   private Source s;
   private Point p;                 // Position de la source dans la vue
   private Calib c;

   /** Cr�ation d'un blink libre */
   protected Blink(ViewSimple v) {
      this.v=v;
      
      mode=NOBLINK;
      step = new boolean[STEP];
   }

   /** Retourne true si le blink est libre */
   synchronized protected boolean isFree() { return mode==NOBLINK; }

   /** Demande le d�marrage du blink pour une source donn�e s */
   synchronized protected void start(Source s) {
      this.s=s;
      c = v.getProjSyncView().getProj().c;
      p = s.getViewCoord(v.getProjSyncView(),s.getL(),s.getL());
      if( p==null ) { mode=NOBLINK; return; }
      mode=START;
   }

   /** Demande d'arr�t du mode blink */
   synchronized protected void stop() { mode=STOP; }

   /** Paint du blink suivant le mode courant et/ou demand� */
   synchronized protected void paint(ViewSimple v,Graphics g) {
      Point p = s.getViewCoord(v.getProjSyncView(),s.getL(),s.getL());
      g.setXORMode(Color.green);
      
//      g.setColor( Color.magenta );

      if( mode==STOP || mode==START ) {
         resetPaint(g,p);
         mode=mode==START?BLINK:NOBLINK;
      }
      if( mode==BLINK ) {
         int i = v.aladin.view.blinkMode%STEP;
         g.drawRect(p.x-L-i,p.y-L-i, (L+i)*2, (L+i)*2);
         step[i]=!step[i];
      }
      g.setPaintMode();
   }

   /** Positionne � "pair" tous les carr�s XOR. Typiquement n�cessaire
    *  lorsque la vue a �t� redessin� totalement
    */
   synchronized protected void reset() { 
      p = s.getViewCoord(v.getProjSyncView(),s.getL(),s.getL());
      if( p==null ) mode=NOBLINK;
      for( int i=0; i<STEP; i++ ) step[i]=false;
   }

   /** Restitue la source en reaffichant les carr�s XOR n�cessaire */
   synchronized protected void resetPaint(Graphics g,Point p) {
      for( int i=0; i<STEP; i++ ) {
          if( step[i] ) {
             g.drawRect(p.x-L-i,p.y-L-i, (L+i)*2, (L+i)*2);
             step[i]=false;
          }
      }
   }
}
