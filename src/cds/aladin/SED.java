// Copyright 2012 - UDS/CNRS
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

import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JPanel;

import cds.tools.Util;
import cds.xml.Field;

/**
 * Gestion du chargement et du tracé simplifié d'un SED au format VizieR
 * @author Pierre Fernique [CDS]
 * @version 1.0 nov 2012
 */
class SED extends JPanel {
   static final int margeHaut=20,margeBas=35,margeDroite=30,margeGauche=10;
   
   static final char MU = '\u03BC';
   static final char NU = '\u03BD';
   static final char TILDE = '\u223C';
   
   private Aladin aladin;
   private PlanCatalog plan;    // Le planCatalog qui va recueillir les infos du SED, il ne sera pas affiché dans la pile
   private String source;       // Le nom de la source concernée par le SED
   private Repere simRep;       // Le repere à afficher dans la vue qui pointe sur la source du SED
   private String url;          // Pour mémoire
   private float transparency;  // Niveau de transparence d'affichage des points
   
   private double freqMin,freqMax;  // Intervalle de fréquences du SED
   private double fluxMin,fluxMax;  // Intervalle de flux du SED
   private double coefX,coefY;      // Facteur de l'homothétie de traçage (déterminé dans setPosition())
   private boolean readyToDraw;     // true si tout est prêt pour être tracé
   private boolean planeAlreadyCreated;  // true si le plan dans la pile a déjé été créé à partir du SED
   private boolean flagWavelength;  // true pour un affichage en longueur d'onde
   
   private ArrayList<SEDItem> sedList;  // Liste des points du SED sous une forme "prémachée"
   
   private Rectangle rCroix,rInfo,rWave;  // Position des icones sur le graphiques
   private String currentFreq=null; // Dernière fréquence sous la souris
   private String currentFlux=null; // Dernier flux sous la souris
   private int currentX,currentY;   // Dernière position de la souris
   private SEDItem siIn;            // !=null si sous la souris
   
   public SED(Aladin aladin) {
      this.aladin = aladin;
      transparency = 0.5f;
   }
   
   /** Mémorise le repère de la vue afin de pouvoir le réafficher ultérieurement
    * si l'utilisateur déplace la souris sur le SED */
   protected void setRepere(Repere simRep) { this.simRep=simRep; }
   
   /** Mémorise le source associée au SED */
   protected void setSource(String source) { this.source=source; }
   
   /** Nettoyage de la liste */
   public void clear() { 
      planeAlreadyCreated=readyToDraw=false;
      if( sedList!=null ) sedList.clear();
   }
   
   /** Etend un SED à partir d'un pcat déjà chargé, typiquement à partir d'un plan
    * catalogue déjà dans la pile */
   protected void addFromIterator(Iterator<Source> it) {
      planeAlreadyCreated=true;
      readyToDraw=false;
      try {
         plan = new PlanCatalog(aladin);
         createSEDlist(it);
         setPosition();
         aladin.calque.zoom.zoomView.flagSED=true;
         aladin.calque.repaintAll();
         
      } catch( Exception e ) {
         aladin.view.zoomview.setSED((String)null);
         aladin.command.printConsole("!!! VizieR SED parsing error => "+e.getMessage());
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   /** Indique la source (correspond à un point du SED) qu'il faut mettre en évidence */
   protected void setHighLight(Source o) {
      for( SEDItem si : sedList )  si.highLight = si.o==o;
   }
   
   /** Charge et crée un SED à partir d'un identificateur de source astronomique (à la Sésame) */
   protected void loadFromSource(String source) {
      planeAlreadyCreated=readyToDraw=false;
      this.source = source;
      try {
         aladin.trace(2,"VizieR SED loading for source \""+source+"\"...");
         url = "http://cdsarc.u-strasbg.fr/viz-bin/sed?-c="+URLEncoder.encode(source);
         aladin.trace(2,"SED loading: "+url);
         loadASync( Util.openAnyStream(url) );
      } catch( Exception e ) {
         aladin.view.zoomview.setSED((String)null);
         aladin.command.printConsole("!!! VizieR SED builder error ["+source+"] => "+e.getMessage());
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   // Creation d'un plan catalogue dans la pile à partir des données du SED
   // On ne peut le faire qu'une seule fois
   private void createStackPlane( boolean flagSelect ) {
      if( planeAlreadyCreated ) return;
      planeAlreadyCreated = true;
      plan.label="SED "+source;
      plan.objet = source;
      aladin.calque.newPlan(plan);
      plan.planReady(true);
      if( flagSelect ) {
         aladin.calque.selectPlan(plan);
         aladin.view.selectAllInPlan(plan);
      }
   }
   
   // Chargement et création d'un SED à partir d'un flux de manière asynchrone
   private void loadASync(MyInputStream in) {
      planeAlreadyCreated=readyToDraw=false;
      final MyInputStream inParam=in;
      
      plan = new PlanCatalog(aladin);
      plan.pcat = new Pcat(plan,Color.black,aladin.calque,aladin.status,aladin);
      (new Thread() {  
         public void run() {
            try {
               plan.pcat.tableParsing(inParam, "TABLE");
               clear();
               parseAndDraw();
            } catch( Exception e ) {
               aladin.view.zoomview.setSED((String)null);
               aladin.command.printConsole("!!! VizieR SED parsing error => "+e.getMessage());
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
      } ).start();
   }

   // Mise en place des listes de points du SED et des conversions de coordonnées
   // Et demande d'affichage
   private void parseAndDraw() throws Exception {
      createSEDlist( plan.iterator() );
      setPosition();
      aladin.calque.zoom.zoomView.flagSED=true;
      aladin.calque.repaintAll();
   }

   /** Retourne la fréquence en GHz du point "n" du SED */
   protected double getFreq(Source s,int n)    { return getSEDValue(s,n,Field.FREQ); }
   
   /** Retourne le flux en Jy du point "n" du SED */
   protected double getFlux(Source s,int n)    { return getSEDValue(s,n,Field.FLUX); }
   
   /** Retourne l'erreur sur le flux du point "n" du SED */
   protected double getFluxErr(Source s,int n) { return getSEDValue(s,n,Field.FLUXERR); }
   
   // Procédure interne d'accès aux valeurs numériques du SED
   private double getSEDValue(Source s,int n,int sed) {
      Legende leg = s.leg;
      for( int i = 0; i<leg.field.length; i++ ) {
         if( leg.field[i].sed == sed ) {
            String val = s.getValue(i);
            try {
               return Double.parseDouble(val.trim());
            } catch( Exception e ) {
               return Double.NaN;
            }
         }
      }
      return Double.NaN;
   }
   
   /** Retourne l'identificateur du filtre associé au point "n" du SED
    * ATTENTION: dans la version actuelle, si l'identicateur du filtre n'est pas explicite (genre @nu ou :=94Ghz)
    *            on retournera le nom de la table origine de la mesure. Ceci évoluera certainement
    */
   protected String getSEDId(Source s,int n) {
      Legende leg = s.leg;
      String sTable=null,sId=null;
      for( int i = 0; i<leg.field.length; i++ ) {
         if( leg.field[i].name.equals("_tabname") ) sTable = s.getValue(i);
         if( leg.field[i].sed == Field.SEDID ) sId = s.getValue(i);
      }
      if( sId==null && sTable==null ) return null;
      if( sId==null ) return sTable;
      return sId.startsWith("@") || sId.startsWith(":=")? sTable : sId;
   }
   
   // Génère la liste des points SED "prémachés" sous la forme d'un ArrayList de SEDItem
   private void createSEDlist(Iterator<?> it) {
       if( sedList==null ) sedList = new ArrayList<SEDItem>();
       for( int i=0; it.hasNext() ; i++ ) {
          Source s = (Source) it.next();
          double freq = getFreq(s,i);
          double flux = getFlux(s,i);
          if( Double.isNaN(freq) || Double.isNaN(flux) ) continue;
          if( freq<=0 || flux<=0 ) continue; // Pas possible en log
          double fluxErr = getFluxErr(s,i);
          String sedId = getSEDId(s,i);
          s.setSelect(true);
          SEDItem si = new SEDItem(s,freq,flux,fluxErr,sedId);
          sedList.add(si);
       }
   }
     
   // Détermine les intervalles de fréquence et de flux, et en déduit les positions
   // de traçage de chaque point du SED
   // lorsque c'est terminé, le tracé peut être opéré
   private void setPosition() {
      
      // Recherche des intervalles
      freqMin = fluxMin = Double.MAX_VALUE;
      freqMax = fluxMax = Double.MIN_VALUE;
      for( SEDItem si : sedList ) {
         if( freqMin > si.freq ) freqMin = si.freq;
         if( fluxMin > si.flux ) fluxMin = si.flux;
         if( freqMax < si.freq ) freqMax = si.freq;
         if( fluxMax < si.flux ) fluxMax = si.flux;
      }
      
      // Affectation des positions
      Dimension dim = getDimension();
      dim.width -= (margeDroite+margeGauche);
      dim.height -= (margeHaut+margeBas);
      double rangeFreq = Math.log(freqMax) - Math.log(freqMin);
      double rangeFlux = Math.log(fluxMax) - Math.log(fluxMin);
      coefX = dim.width / rangeFreq;
      coefY = dim.height / rangeFlux;
      for( SEDItem si : sedList ) {
         double x = ( Math.log(si.freq) - Math.log(freqMin)) * coefX;
         double y = ( Math.log(si.flux) - Math.log(fluxMin)) * coefY;
         si.setBox( (int)x, (int)(dim.height-y));
      }
      
      // Prêt à être dessiné
      readyToDraw = true;
   }
   
   // Retourne la fréquence courante sous la souris
   private double getCurrentFreq(double x) {
      if( !readyToDraw ) return Double.NaN;
      return Math.exp( x/coefX + Math.log(freqMin) );
   }
   
   // Retourne le flux courant sous la souris
   double getCurrentFlux(double y) {
      if( !readyToDraw ) return Double.NaN;
      return Math.exp( y/coefY + Math.log(fluxMin) );
   }
   
   /** Classe interne qui gère un point du SED */
   private class SEDItem {
      private double freq,flux,fluxErr;     // Fréquence, Flux et erreur sur Flux
      private String sedId;                 // identificateur du filtre associé
      private Source o;                     // Source aladin associée à la mesure
      private boolean highLight;            // true si on doit mettre en avant ce point
      private Rectangle r;                  // rectangle qui englobe le point

      static final int W = 6;       // taille du carré englobant
      
      private SEDItem(Source o,double freq,double flux,double fluxErr,String sedId) {
         this.o = o;
         this.freq = freq;
         this.flux = flux;
         this.fluxErr = fluxErr;
         this.sedId = sedId;
      }
      
      // Positionne la boite englobante du point
      private void setBox(int x, int y) { r = new Rectangle(x-W/2 +margeGauche, y-W/2 + margeHaut, W,W); }
      
      // Retourne true si la souris est dans la boite englobante
      private boolean contains(int x,int y) { return r.contains(x, y); }
      
      // Trace le point
      private void draw(Graphics g) {
         g.setColor( o.getColor() );
         Util.fillCircle5(g, r.x+W/2, r.y+W/2);

         // Mise en évidence de ce point particulièrement
         if( highLight ) {
            g.setColor( Aladin.GREEN);
            g.drawRect(r.x, r.y, r.width, r.height);
            
            // Affichage des infos sous le graphique
            g.setFont(Aladin.BOLD);
            Dimension dim = getDimension();
            FontMetrics fm = g.getFontMetrics();
            int height = fm.getHeight();
            g.drawString(sedId,dim.width/2-fm.stringWidth(sedId)/2, dim.height-height-2);
            
            String s;
            if( !flagWavelength ) s = Util.myRound(freq)+"GHz";
            else s = Util.myRound(getWaveLength(freq))+MU+"m";
            g.drawString(s, 5,getDimension().height-3);
            
            s=Util.myRound(flux)+"Jy";
            g.drawString(s, dim.width-5-fm.stringWidth(s),getDimension().height-3);
         }
      }
   }
   
   /** Retourne la dimension du graphique */
   public Dimension getDimension() { return new Dimension(ZoomView.SIZE,ZoomView.SIZE); }
   
   // Tracé du graphique
   protected void draw(Graphics g) {
      Dimension dim = getDimension();
      
      // Nettoyage
      g.setColor(Color.white);
      g.clearRect(0, 0, dim.width, dim.height);
      
      // Tracé des axes
      int arrow=3;
      g.setColor(Color.black);
      int haut=margeHaut-5, bas=dim.height-margeBas+5;
      int gauche=margeGauche-5, droite=dim.width-margeDroite+5;
      g.drawLine(gauche, haut, gauche, bas);
      g.drawLine(gauche, haut, gauche-arrow, haut+arrow);
      g.drawLine(gauche, haut, gauche+arrow, haut+arrow);
      g.drawLine(gauche, bas, droite, bas);
      g.drawLine(droite, bas, droite-arrow, bas-arrow);
      g.drawLine(droite, bas, droite-arrow, bas+arrow);
      
      // Légende
      g.setFont(Aladin.SSPLAIN);
      g.drawString("log f("+NU+")",gauche-2,haut-4);
      g.drawString(Util.myRound(fluxMax)+" Jy",gauche+4,haut+6);
      g.drawString("log "+NU,droite+2,bas);
      if( flagWavelength ) g.drawString(MU+"m",droite+2,bas+10);
      else g.drawString("GHz",droite+2,bas+10);
      
      // Tracé de la valeur sous la souris
      g.setColor(Color.lightGray);
      if( currentFreq!=null ) {
         g.drawLine(currentX,bas,currentX,bas-5);
         g.setFont(Aladin.SSPLAIN);
         g.drawString(currentFreq, currentX, bas-8);
      }
      if( currentFlux!=null ) {
         g.drawLine(gauche,currentY,gauche+5,currentY);
         g.setFont(Aladin.SSPLAIN);
         g.drawString(currentFlux, gauche+10, currentY);
      }
      
      // Les icones
      drawCroix(g);
      drawWave(g);
      drawInfo(g);

      // Tracé des points
      SEDItem siIn=null;
      if( sedList==null || !readyToDraw ) {
         g.setFont(Aladin.ITALIC);
         String s = aladin.chaine.getString("SEDLOADING");
         g.drawString(s,dim.width/2-g.getFontMetrics().stringWidth(s)/2,(haut+bas)/2+5);
         return;
      } else {
         g.setFont(Aladin.SPLAIN);
         Composite c = ((Graphics2D)g).getComposite();
         try {
            ((Graphics2D) g).setComposite(Util.getImageComposite( transparency ));
            for( SEDItem si : sedList ) {
               if( si.highLight ) siIn = si;
               si.draw(g);
            }
         } finally { ((Graphics2D)g).setComposite(c); }
         if( siIn!=null ) siIn.draw(g);
      }
      
      // Tracé de l'intervalle de Fréquence
      if( siIn==null ) {
         g.setColor(Color.black);
         g.setFont(Aladin.SPLAIN);
         String s =  Util.myRound( flagWavelength ? getWaveLength(freqMin) : freqMin );
         g.drawString(s,gauche,bas+10);
         s =  Util.myRound( flagWavelength ? getWaveLength(freqMax) : freqMax );
         g.drawString(s,droite - g.getFontMetrics().stringWidth(s),bas+10);
      }
      
      // Tracé du titre du graphique : le nom de la source
      if( source!=null ) {
         g.setColor(Aladin.GREEN);
         g.setFont(Aladin.BOLD);
         int size = g.getFontMetrics().stringWidth(source);
         g.drawString(source,dim.width/2-size/2, haut-2);
      }

   }
   
   static double getWaveLength(double freq) { return 2.998E5/freq; }
   
   // Trace l'icone de fermeture du graphique
   private void drawCroix(Graphics g) {
      int w=5;
      int width=getDimension().width;
      g.setColor(Aladin.BKGD);
      rCroix = new Rectangle(width-w-4,1,w+4,w+4);
      g.fillRect(rCroix.x,rCroix.y,rCroix.width,rCroix.height);
      g.setColor(Color.red);
      g.drawLine(width-w-3,2,width-3,w+2);
      g.drawLine(width-w-3,3,width-3,w+3);
      g.drawLine(width-w-3,w+2,width-3,2);
      g.drawLine(width-w-3,w+3,width-3,3);
   }
   
   // Trace l'icone de demande de plus d'info sur le SED
   private void drawInfo(Graphics g) {
      if( planeAlreadyCreated ) {
         rInfo = new Rectangle(0,0,0,0);
         return;
      }
      int w=5;
      int width=getDimension().width;
      g.setColor(Aladin.BKGD);
      rInfo = new Rectangle(width-w-4,6+w,w+4,w+4);
      g.fillRect(rInfo.x,rInfo.y,rInfo.width,rInfo.height);
      g.setColor(Color.blue);
      g.setFont(Aladin.SBOLD);
      g.drawString("?",rInfo.x+2,rInfo.y+8);
   }
   
   // Trace l'icone de demande de passage freq <-> longueur d'onde
   private void drawWave(Graphics g) {
      int w=5;
      Dimension dim=getDimension();
      g.setColor(Aladin.BKGD);
      rWave = new Rectangle(dim.width-w-4,11+2*w,w+4,w+4);
      g.fillRect(rWave.x,rWave.y,rWave.width,rWave.height);
      g.setColor( flagWavelength ? Color.red : Color.blue );
      g.setFont(Aladin.SBOLD);
      g.drawString(TILDE+"",rWave.x+1,rWave.y+8);
   }
   
   /** Actions à effectuer lors du relachement de la souris */
   protected void mouseRelease(int x,int y) {
      if( rCroix.contains(x,y) ) aladin.view.zoomview.setSED((String)null);
      else if( rInfo.contains(x,y) ) createStackPlane( false );
      else if( rWave.contains(x,y) ) {
         flagWavelength = !flagWavelength;
         aladin.view.zoomview.repaint();
      }
      else if( siIn!=null ) {
         createStackPlane( true );
         aladin.view.showSource(siIn.o, false, true);
         aladin.mesure.mcanvas.show(siIn.o, 2);
         aladin.calque.repaintAll();
      } 
   }
   
   /** Actions à effectuer lorsque la souris sort du cadre */
   protected void mouseExit() {
      currentFreq=currentFlux=null;
      aladin.view.simRep = null;
   }
   
   /** Actions à effectuer lorsque la souris entre dans le cadre */
   protected void mouseEnter() {
      aladin.view.simRep = simRep;
      if( simRep!=null ) aladin.view.repaintAll();
   }
   
   /** Actions à effectuer lorsque la souris survole le graphique */
   protected void mouseMove(int x, int y) {
      
      // Y a-t-il un point de SED sous la souris ?
      siIn=null;
      for( SEDItem si : sedList ) si.highLight=false;
      for( SEDItem si : sedList ) {
         if( si.contains(x,y) ) {
            si.highLight=true;
            siIn=si;
            break;
         }
      }
      
      // Dois-je montrer la source associée ?
      if( siIn!=null ) {
         aladin.view.showSource(siIn.o, false, true);
         aladin.mesure.mcanvas.show(siIn.o, 1);
      }
      
      // Quels sont le flux et la fréquence sous la souris
      Dimension dim = getDimension();
      if( x>margeGauche && x<dim.width-margeDroite ) {
         double freq = getCurrentFreq( (double)(x-margeGauche) );
         if( Double.isNaN(freq) ) currentFreq=null;
         else currentFreq = Util.myRound( Math.log(freq));
      } else currentFreq = null;

      if( y>margeHaut && y<dim.height-margeBas ) {
         double flux = getCurrentFlux( (double)( (dim.height-y-1)-margeHaut) );
         if( Double.isNaN(flux ) ) currentFlux=null;
         else currentFlux = Util.myRound( Math.log10(flux) );
      } else currentFlux = null;
      
      // Mémorisation de la position sous la souris
      currentX=x;
      currentY=y;
   }
   
   protected boolean mouseWheel( MouseWheelEvent e) {
      int n=e.getWheelRotation();
      if( n==0 ) return false;
      float x = transparency + n*0.1f;
      if( x>1 ) x=1f;
      if( x<0.2 ) x=0.2f;
      if( x==transparency ) return false;
      transparency=x;
      return true;

   }
   
}