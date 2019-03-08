// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

/**
 *  Le blinking consiste à désigner une source pour la repérer aisément.
 *  La méthode consiste à faire des XOR de carrés de plus en plus grands
 *  puis de repasser dessus afin de les effacer.
 *  L'intérêt de la méthode est qu'il est inutile de mémoriser ce qu'il
 *  y a sous la source pour restituer la source après l'avoir montrée.
 *  Sa difficulté consiste à faire un nombre pair de XOR, malgré les
 *  affichages asynchrones.
 *
 * 1. La souris rentre dans une source dans la vue n
 *    (ou respectivement en sort)
 *  - appel à la fonction View.showSource(Source s) qui va transmettre
 *    à toutes les vues que la nouvelle source à montrer est s (s null
 *    si sort d'une source) => ViewSimple.changeBlinkSource(Source s)
 *  - Lance le thread de blinking si ce n'est déjà fait, sinon
 *    lui passe une interruption afin que la prise en compte
 *    soi immédiate.
 *
 * 2. Thread de blinking (View.runC())
 *  - incrémente le mode blink (View.modeBlink)
 *  - demande le repaint (blinkRepaint()) pour toutes les vues
 *      qui ont quelques choses à faire (au-moins une source
 *      à montrer ou à restituer)
 *  - si aucune, s'arrête sinon attend 0.3 secondes et boucle
 *
 * 3. La modification d'une source à montrer pour une vue
 *  - cherche dans sa liste de Blinks, toutes les sources
 *    qu'il faut restituer et en fait la demande
 *  - si dans la liste de Blinks (ViewSimple.showSource) dont il dispose
 *    il y a un Blink libre, il l'utilise pour insérer la nouvelle source
 *    à montrer et demande son clignotement
 *  - s'il n'y a plus de place dans la liste des Blinks, il
 *    ajoute un nouveau Blink décrivant la nouvelle source
 *    à montrer
 *
 * 3. Le repaint des vues en mode blink
 *  - restitue ou montre les sources qui doivent l'être
 *    (parcours du Vector showSource contenant des objets Blink
 *
 * 4. Le paint d'un blink
 *  - Un Blink dispose de 4 états (UNBLINK,START,BLINK et STOP)
 *    START et STOP peuvent être demandé par start() et stop()
 *    UNBLINK correspond à un Blink libre
 *    BLINK correspond à l'état de clignotement
 *  - L'état d'un blink change de fait uniquement lors de l'appel
 *    à paint(Graphics g) car il doit disposer du contexte graphique
 *    pour opérer.
 *    *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (11 mai 2004) création
 */
public final class Blink {
   static private final int L=2;    // Taille du premier carré
   static protected int STEP=3;     // Nombre de carrés autour de la source

   static private final int START  =1;       // Mode de démarrage du blink
   static private final int STOP   =2;       // Mode d'arrêt du blink
   static private final int NOBLINK=3;       // Mode "libre"
   static private final int BLINK=4;         // Mode de blinking


   private int mode;                // Mode courant du blink
   private boolean step[];          // Etat de chaque carré XOR (true-pair, false-impair)

   private ViewSimple v;            // Vue à laquelle appartient le Blink
   private Source s;
   private Point p;                 // Position de la source dans la vue
   private Calib c;

   /** Création d'un blink libre */
   protected Blink(ViewSimple v) {
      this.v=v;
      
      mode=NOBLINK;
      step = new boolean[STEP];
   }

   /** Retourne true si le blink est libre */
   synchronized protected boolean isFree() { return mode==NOBLINK; }

   /** Demande le démarrage du blink pour une source donnée s */
   synchronized protected void start(Source s) {
      if( s==null ) return;
      try {
         this.s=s;
         c = v.getProjSyncView().getProj().c;
         p = s.getViewCoord(v.getProjSyncView(),s.getL(),s.getL());
         if( p==null ) { mode=NOBLINK; return; }
         mode=START;
      } catch( Exception e ) { }
   }

   /** Demande d'arrêt du mode blink */
   synchronized protected void stop() { mode=STOP; }

   /** Paint du blink suivant le mode courant et/ou demandé */
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

   /** Positionne à "pair" tous les carrés XOR. Typiquement nécessaire
    *  lorsque la vue a été redessiné totalement
    */
   synchronized protected void reset() { 
      p = s.getViewCoord(v.getProjSyncView(),s.getL(),s.getL());
      if( p==null ) mode=NOBLINK;
      for( int i=0; i<STEP; i++ ) step[i]=false;
   }

   /** Restitue la source en reaffichant les carrés XOR nécessaire */
   synchronized protected void resetPaint(Graphics g,Point p) {
      for( int i=0; i<STEP; i++ ) {
          if( step[i] ) {
             g.drawRect(p.x-L-i,p.y-L-i, (L+i)*2, (L+i)*2);
             step[i]=false;
          }
      }
   }
}
