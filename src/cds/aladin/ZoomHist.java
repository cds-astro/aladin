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

/**
 * Gestion d'une case de l'histogramme de répartition des valeurs d'une colonne
 * pour les mesures affichées dans MCanvas (voir Zoomview.setHist())
 */
package cds.aladin;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import cds.tools.Util;

class ZoomHist implements Runnable {

   int MARGE = 8;
   int NBHIST = 12;
   int GAP = 3;
   static final int WCROIX = 5;

   Aladin aladin;
   String titre;    // Titre de l'histogramme (typiquement le nom de la colonne)
   Source o;        // Source étalon permettant de connaître la table concernée
   int nField;      // indice du champ concerné dans les mesures

   HistItem[] hist;         // L'histogramme de la colonne courante des mesures
   HistItem onMouse=null;   // La case de l'histogramme sous la souris
   int nb;                  // Nombre de sources
   int nbCategorie;         // Nombre de catégories dans le cas non numérique
   boolean flagHistPartial; // true si l'histogramme n'affiche pas toutes les catégories
   int width=100;           // Largeur totale de l'histogramme
   int height=100;          // Hauteur totale de l'histogramme
   String texte=null;       // Texte en surcharge, ou null (retour à la ligne par "/")

   // Paramètres pour un histogramme de pixels
   boolean flagHistPixel;   // true s'il s'agit d'un histogramme de pixels
   StatPixels pixelStats;   // Statistisques et valeur des pixels
   

   ZoomHist(Aladin aladin) {
      this.aladin=aladin;
      pixelStats = new StatPixels();
   }

   /** Positionnement d'un texte en surcharge de l'histogramme, null si aucun */
   protected void setText(String s) { texte=s; }

   /** retourne du texte pour le clipboard */
   protected String getCopier() { return texte; }

   /** Commence ou recommence la mémorisation des pixels d'un histogramme de pixels */
   protected void startHistPixel() {
      flagHistPixel=true;
      texte=null;
      
      pixelStats.reinit();
   }

   protected boolean isOverFlow() { return false; }

   /** Ajoute une valeur pour un future histogramme de pixels */
   protected boolean addPixel(double pix) {
      pixelStats.addPix(0, 0, pix);
      return true;
   }

   /** Construction de l'histogramme de pixels en fonction de la liste des valeurs passées */
   protected void createHistPixel(String titre) {
      this.titre = titre;
      setHist(pixelStats);
   }


   class HistItem {
      int nb;              // Nombre d'élément
      double prop;         // proportion
      double min,max;      // pour le cas numérique, bornes inf et sup des valeurs concernées
      String categorie;    // sinon texte représentant la catégorie
      int larg,haut;       // largeur et hauteur de la case pour le tracage de l'histogramme
      int x,y;             // position coin SG de la case pour le tracçage de l'histogramme

      HistItem() { nb=0; }

      /** Positionne le flag onMouse : true si le point passé en paramètre se trouve dans la case */
      boolean in(int xc,int yc) {
         return xc>=x && xc<=x+larg;
      }

      /** Retourne true si la valeur du pixel se trouve entre les bornes */
      boolean contains(double value) {
         return min<=value && value<max;
      }
   }

   /** Génération de l'histogramme de répartition des valeurs du champ nField
    * de toutes les sources de même légende que la source étalon o
    * @return true si l'histogramme a pu être généré
    */
   protected boolean init(Source o,int nField) {
      if( thread!=null ) return false;        // Pour le moment, un thread après l'autre uniquement
      flagHistPixel=false;
      texte=null;
      this.o=o;
      this.nField=nField;
      if( nField==-1 ) { hist=null; return false; }
      titre=o.getLeg().field[nField].name;
      return init();
   }

   volatile private Thread thread=null;

   /** Si trop de sources, on va threader */
   protected boolean init() {
      if( flagHistPixel ) { setHist(pixelStats); return true; }
      else {
         if( aladin.mesure.getNbSrc()>10000 ) {
            thread = new Thread(this,"Histo");
            thread.start();
            return false;
         } else return initThread();
      }
   }

   public void run() {
      if( initThread() ) {
         aladin.calque.zoom.zoomView.flagHist=true;
         aladin.calque.repaintAll();
      }
      thread=null;
   }

   protected boolean initThread() {
      if( o.getLeg().isNumField(nField) ) {
         double [] xHist = aladin.mesure.getFieldNumericValues(o, nField);
         
         pixelStats.reinit();
         for( double x : xHist) pixelStats.addPix(0, 0, x);
         flagHistPixel=true;
         setHist(pixelStats);
         
      } else {
         String [] sHist=aladin.mesure.getFieldStringValues(o, nField);
         nb=sHist.length;
         setHist(sHist);
      }

      return hist!=null;
   }

   /** Ajustement du nombre de cases par la roulette de la souris */
   boolean mouseWheelMoved(MouseWheelEvent e) {
      int n=e.getWheelRotation();
      if( n==0 ) return false;
      n=NBHIST+(NBHIST>2 ? 2*n : n);
      setNbHist(n);
      return true;
   }

   /** Ajustement du nombre de cases */
   void setNbHist(int n) {
      NBHIST=n;
      if( NBHIST<4 ) NBHIST=4;
      if( NBHIST>30 ) NBHIST=30;

      if( NBHIST>25 ) GAP=0;
      else if( NBHIST>15 ) GAP=2;
      else GAP=3;

      init();
   }

   boolean mouseDragged(MouseEvent e) {
      int y = e.getY();
      int n = (int)( y / ((height - WCROIX-15.)/30) );
      setNbHist(n);
      return true;
   }
   
   protected void setHist(StatPixels pixelStats) { setHist( pixelStats.getStatisticPix() ); }

   /**
    * Initialisation de l'histogramme avec une liste de valeurs numériques
    */
   protected void setHist(double [] x) { setHist(x,x.length); }
   protected void setHist(double [] x, int length) {

//      int length = flagHistPixel ? nPix : x.length;

      // Recherche du min et max
      double min=Double.MAX_VALUE;
      double max=-min;
      for( int i=0; i<length; i++ ) {
         double c = x[i];
         if( Double.isNaN(c) ) continue;
         if( c<min ) min=c;
         if( c>max ) max=c;
      }

      if( min==Double.MAX_VALUE || max==-Double.MAX_VALUE
            || min==max || length<=1 ) { hist=null; return; }

      hist = new HistItem[NBHIST];
      flagHistPartial=false;
      nbCategorie=-1;
      double range = max-min;
      double sizeBean = range/hist.length;

      // Initialisation et Labellisation de l'histogramme
      for( int i=0; i<hist.length; i++ ) {
         hist[i] = new HistItem();
         hist[i].min = i==0 ? min : hist[i-1].max;
         hist[i].max = hist[i].min + sizeBean;
      }

      // répartition dans l'histogramme
      for( int i=0; i<length; i++ ) {
         if( Double.isNaN(x[i]) ) continue;
         int index = (int) ( (x[i]-min)/sizeBean );
         if( index>hist.length-1 ) {
            index=hist.length-1;
            hist[index].max=x[i]+0.00000000001;
         }
         hist[index].nb++;
      }

      int maxNb = Integer.MIN_VALUE;
      // Recherche de nombre maximal
      for( int i=0; i<hist.length; i++ ) {
         if( hist[i].nb>maxNb ) maxNb=hist[i].nb;
      }

      // Calcul des hauteurs et des proportions
      int hautMax = height-40;
      double coef = (double)hautMax/maxNb;
      for( int i=0; i<hist.length; i++ ) {
         hist[i].haut = (int)( hist[i].nb * coef);
         hist[i].prop = ((double)hist[i].nb/length)*100;
      }
   }

   /**
    * Initialisation de l'histogramme avec une liste de catégories
    */
   protected void setHist(String [] x) {

      class Int {
         int val;
         Int() { val=1; }
         void incr() { val++; }
         void clear() { val=-1; }
      }

      // Décompte de chaque catégorie
      HashMap map = new HashMap(300);
      for( int i=0; i<x.length; i++ ) {
         String key=x[i];
         if( key.length()==0 ) continue;
         Int n = (Int)map.get(key);
         if( n!=null ) n.incr();
         else map.put(key, new Int());
      }

      // On initialise l'histogramme
      int nb=nbCategorie=map.keySet().size();
      if( nb<=1 ) { hist=null; return; }

      flagHistPartial = nb>NBHIST;
      if( flagHistPartial ) nb=NBHIST;
      if( hist==null || hist.length!=nb ) hist = new HistItem[nb];

      // On conserve que les N meilleures
      for( int i=0; i<hist.length; i++ ) {
         hist[i] = new HistItem();
         String maxKey=null;
         int max=-1;
         Iterator it = map.keySet().iterator();
         while( it.hasNext() ) {
            String key = (String) it.next();
            Int n = (Int)map.get(key);
            if( n.val>max ) { max=n.val; maxKey=key; }
         }

         // L'histogramme est totalement plat => on sort
         if( i==0 && max==1 ) { hist=null; return; }

         if( max!=-1 ) {
            Int n = (Int)map.get(maxKey);
            hist[i].nb=n.val;
            hist[i].categorie=maxKey;
            n.clear();
         }
      }

      int maxNb = Integer.MIN_VALUE;
      // Recherche de nombre maximal
      for( int i=0; i<hist.length; i++ ) {
         if( hist[i].nb>maxNb ) maxNb=hist[i].nb;
      }

      // Calcul des hauteurs
      int hautMax = height-40;
      double coef = (double)hautMax/maxNb;
      for( int i=0; i<hist.length; i++ ) {
         hist[i].haut = (int)( hist[i].nb * coef);
         hist[i].prop = ((double)hist[i].nb/x.length)*100;
      }
   }


   /** Sélection de toutes les sources highlightées */
   protected void selectHighlightSource() {
      if( onMouse==null || flagHistPixel ) return;
      // Mémorisation
      Source [] nsrc = new Source[onMouse.nb];
      int j=0;
      for( int i=0; i<aladin.mesure.nbSrc; i++ ) {
         if( aladin.mesure.src[i].isHighlighted() ) nsrc[j++]=aladin.mesure.src[i];
      }

      // Reset et regénération
      aladin.view.deSelect();
      for( int i=0; i<nsrc.length; i++ ) {
         nsrc[i].setSelected(true);
      }
      aladin.mesure.adjustScroll();

      // envoi message PLASTIC/SAMP
      aladin.appMessagingMgr.sendSelectObjectsMsg();

      // Regénération de l'histogramme
      init();
   }

   /** Suppression du highlighting */
   protected void resetHighlightSource() {
      if( flagHistPixel ) return;
      for( int i=0; i<aladin.mesure.nbSrc; i++ ) aladin.mesure.src[i].setHighlight(false);

   }

   /** Highlight de toutes les sources de la case sous la souris */
   protected void setHighlightSource() {
      if( flagHistPixel ) return;

      // D'abord un reset complet
      resetHighlightSource();

      // Positionnement des nouvelles sources à hightlighter
      if( onMouse!=null ) {
         for( int i=0; i<aladin.mesure.nbSrc; i++ ) {
            if( aladin.mesure.src[i].getLeg()!=o.getLeg() ) continue;

            String s = aladin.mesure.src[i].getValue(nField);
            if( onMouse.categorie!=null && onMouse.categorie.equals(s)) {
               aladin.mesure.src[i].setHighlight(true);
            } else {
               try {
                  double x = Double.parseDouble(s);
                  if( x>=onMouse.min && x<onMouse.max ) aladin.mesure.src[i].setHighlight(true);
               } catch( Exception e ) { }
            }
         }
      }
   }

   /** Cherche et positionne la case sous la souris s'il y en a une
    * @param x,y position de la souris
    * @return true s'il y a eu un changement avec l'état antérieur
    */
   boolean setOnMouse(int x, int y) {
      if( hist==null ) return false;
      HistItem oOnMouse=onMouse;
      for( int i=0; i<hist.length; i++ ) {
         if( hist[i].in(x,y) ) {
            onMouse=hist[i];
            return onMouse!=oOnMouse;
         }
      }
      boolean rep = onMouse!=null;
      onMouse=null;
      return rep;
   }

   /** Retourne true si la coord se trouve dans l'icone pour fermer l'histogramme */
   protected boolean inCroix(int x, int y ) {
      return x>=width-WCROIX-2 && y<=WCROIX+2;
   }

   /** Retourne true s'il s'agit d'un histogramme sur des nombres */
   boolean isNumeric() { return hist!=null && hist.length>0 && hist[0].categorie==null; }

   private HistItem oOnMouse=null;  // pour éviter de faire plusieurs fois la même chose

   /** Dessin de l'histogramme */
   protected void draw(Graphics g) { draw(g,0,0); }
   protected void draw(Graphics g,int dx,int dy) {
      
      width = aladin.calque.zoom.zoomView.getWidth();
      height = aladin.calque.zoom.zoomView.getHeight();

      // Nettoyage
      g.setColor( Aladin.COLOR_BACKGROUND );
      g.fillRect(1+dx,1+dy,width-2,height-2);
      if( hist==null ) return;

      int gap = GAP;
      double larg = ( width - MARGE - (gap*(hist.length+1)) )/(double)hist.length;
      double x=gap+MARGE;
      int y=height-20;
      String s;
      g.setFont(Aladin.SSBOLD);
      FontMetrics fm = g.getFontMetrics();

      if( !isOverFlow() ) for( int i=0; i<hist.length; i++) {

         g.setColor( Aladin.COLOR_FOREGROUND );
         // Le label min et max en dessous de l'histogramme dans le cas numérique
         if( hist[i].categorie==null ) {
            if( i==0 ) g.drawString(Util.myRound(hist[i].min),2+dx,height-2+dy);
            else if( i==hist.length-1 ) {
               s=Util.myRound(hist[i].max);
               g.drawString(s,width-fm.stringWidth(s)+dx,height-2+dy);
            }

            // Les labels des catégories sinon
         } else {
            s=hist[i].categorie;
            while( fm.stringWidth(s)>hist[i].larg ) s=s.substring(0,s.length()-1);
            if( s!=hist[i].categorie ) s=s+"..";
            g.drawString(s,hist[i].x+hist[i].larg/2-fm.stringWidth(s)/2+dx,height-2+dy);
         }

         // Histogramme non complet ? et dernière case, mettre des points de suspension
         if( flagHistPartial && i==hist.length-1 ) {
            for( int j=0; j<3; j++ ) {
               g.setColor( Aladin.COLOR_FOREGROUND );
               Util.fillCircle5(g, (int)(x+gap+j*6)+dx,y-15+dy);
            }

            // Tracé de la case courante
         } else {
            g.setColor( hist[i]==onMouse ? Aladin.COLOR_GREEN : Aladin.COLOR_CONTROL_FOREGROUND_UNAVAILABLE );
            g.fillRect((int)x+dx, y-hist[i].haut+dy, (int)larg, hist[i].haut);
//            g.setColor( Aladin.COLOR_CONTROL_FOREGROUND ); 
            g.setColor( Aladin.COLOR_MEASUREMENT_FOREGROUND_SELECTED_LINE );
            g.drawRect((int)x+dx, y-hist[i].haut+dy, (int)larg, hist[i].haut);
            hist[i].x=(int)x; hist[i].y=y-hist[i].haut; hist[i].larg=(int)larg; // voir setIn()
            x+=larg+gap;
         }
      }

      aladin.view.flagHighlight = onMouse!=null && !flagHistPixel;

      // repérage des sources concernées par la case de l'histogramme sous la souris
      if( onMouse!=oOnMouse ) {
         setHighlightSource();
         aladin.view.repaint();
      }
      oOnMouse=onMouse;

      // Information textuelle
      g.setFont(Aladin.BOLD);
      g.setColor( Aladin.COLOR_GREEN );
      fm = g.getFontMetrics();

      // Sous la souris ?
      if( onMouse!=null ) {
         g.setColor( Aladin.COLOR_RED );
         s=onMouse.nb+"";
         g.drawString(s,5,14);
         int pos = 8+fm.stringWidth(s);

         g.setColor( Aladin.COLOR_GREEN );
         s=Util.myRound(onMouse.prop+"",1)+"%";
         int pos2=width-fm.stringWidth(s)-12;
         g.drawString(s,pos2,14);

         if( onMouse.categorie==null ) s=Util.myRound(onMouse.min)+","+Util.myRound(onMouse.max);
         else s=onMouse.categorie;
         x=(width-WCROIX)/2-fm.stringWidth(s)/2;
         if( x<pos ) {
            x=(pos+pos2)/2-fm.stringWidth(s)/2;
            if( x<pos ) x=pos;
         }
         g.drawString(s,(int)x,14);

         // Général
      } else {
         s=titre;
         if( nbCategorie>0 ) {
            s=s+" ("+nbCategorie+" item"+(nbCategorie>1?"s)":")");
         }
         if( s!=null ) g.drawString(s,5,14);
      }

      // Le trait du bas du graphique
      g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
      g.drawLine(gap-2+MARGE,y,width-2*gap+2,y);

      // Le curseur de la résolution de l'histogramme
      if( !isOverFlow() && hist.length>=4 ) {
         int pos = (int)( hist.length * (height - WCROIX-15.)/30 );
         int xc=MARGE/2+1;
         g.setColor(Color.gray);
         g.drawLine( xc-1, WCROIX+16, xc-1, height-15);
         g.setColor(Color.black);
         g.fillPolygon(new Polygon(new int [] {xc-3 ,xc+2,xc-3},
               new int [] {pos-3,pos,pos+3}, 3));
      }

      // L'icone pour fermer l'histogramme
      if( !flagHistPixel ) {
         int w=WCROIX;
         g.setColor(Aladin.COLOR_BUTTON_BACKGROUND);
         g.fillRect(width-w-4,1,w+4,w+4);
         g.setColor(Color.red);
         g.drawLine(width-w-3,2,width-3,w+2);
         g.drawLine(width-w-3,3,width-3,w+3);
         g.drawLine(width-w-3,w+2,width-3,2);
         g.drawLine(width-w-3,w+3,width-3,3);
      }

      // Des informations textuelles en surcharge (on écarte Stat.. qui concerne les cercles)
      if( flagHistPixel ) {
         g.setFont(Aladin.BOLD);
         fm = g.getFontMetrics();
         g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
         StringTokenizer st = new StringTokenizer(getStringStat(),"/");
         y = 17;
         int h= fm.getHeight();
         int h1 = fm.getAscent();
         while( st.hasMoreTokens() ) {
            s = st.nextToken().trim();
            int len = fm.stringWidth(s);
            int x1 = width - len-5;
//            Util.drawStringOutline(g, s, x1, y+h1, aladin.COLOR_RED, Color.white);
            g.drawString(s, x1, y+h1);
            y+=h;
         }
      }
   }
      
   // Retourne une chaine reprenant les infos statistiques à afficher en superposition
   String getStringStat() {
      // nb, sum, sigma, surface, min, max, median
      double x [] = pixelStats.getStatistics(true);
      return "Cnt "+Util.myRound(x[0])
            +" / Sum "+Util.myRound(x[1])
            +" / Sigma "+Util.myRound(x[2])
//            +" / Min "+Util.myRound(x[4])
            +" / Avg "+Util.myRound((x[1]/x[0]))
            +(Double.isNaN(x[6])?"":" / Med "+Util.myRound(x[6]))
//            +" / Max "+Util.myRound(x[5])
            ;

   }
}
