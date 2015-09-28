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
import java.text.NumberFormat;
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
   
   static private final String SEDGLUTAG = "VizieR.sed"; 

   static final int margeHaut=20,margeBas=30,margeDroite=30,margeGauche=10;
   
   static final char MU = '\u03BC';
   static final char NU = '\u03BD';
   static final char TILDE = '\u223C';
   
   private static final double EPSILON = 1E-1;
   private static final double WAVEMIN = 0.2;
   private static final double WAVEMAX = 30;
   private static final double FREQMIN = 1E4;
   private static final double FREQMAX = 1E6;
   private static final double WAVEOPTMIN = 0.3;
   private static final double WAVEOPTMAX = 1;
   private static final double FREQOPTMIN = wave2Freq(WAVEOPTMAX);
   private static final double FREQOPTMAX = wave2Freq(WAVEOPTMIN);
   
   static private final Color LIGHTGRAY = new Color(125,125,125);

   private Aladin aladin;
   private PlanCatalog plan;    // Le planCatalog qui va recueillir les infos du SED, il ne sera pas affiché dans la pile
   private String source;       // Le nom de la source concernée par le SED
   private double radius;       // Le rayon de recherche de la SED (en arcsec)
   private Repere simRep;       // Le repere à afficher dans la vue qui pointe sur la source du SED
   private String url;          // Pour mémoire
   private float transparency;  // Niveau de transparence d'affichage des points
   
   private double absMin,absMax;    // Intervalle de fréquences/longueurs d'onde du SED
   private double fluxMin,fluxMax;  // Intervalle de flux du SED
   private double coefX,coefY;      // Facteur de l'homothétie de traçage (déterminé dans setPosition())
   private boolean readyToDraw;     // true si tout est prêt pour être tracé
   private boolean planeAlreadyCreated;  // true si le plan dans la pile a déjé été créé à partir du SED
   private boolean flagWavelength;  // true pour un affichage en longueur d'onde
   private int xOptMin,xOptMax;     // Position de la bande optique
   
   private ArrayList<SEDItem> sedList;  // Liste des points du SED sous une forme "prémachée"
   
   private Rectangle rCroix,rWave,rHelp,rMore;  // Position des icones sur le graphiques
   private double currentAbs=Double.NaN;  // Dernière fréquence/longueur d'onde sous la souris
   private double currentFlux=Double.NaN; // Dernier flux sous la souris
   private int currentX,currentY;         // Dernière position de la souris
   private SEDItem siIn;                  // !=null si sous la souris
   
   public SED(Aladin aladin) {
      this.aladin = aladin;
      transparency = 0.5f;
      flagWavelength = aladin.configuration.getSEDWave();
      radius = 5;
   }
   
   /** Mémorise le repère de la vue afin de pouvoir le réafficher ultérieurement
    * si l'utilisateur déplace la souris sur le SED */
   protected void setRepere(Repere simRep) { this.simRep=simRep; }
   
   
   static boolean first=true;
   
   /** Mémorise le source associée au SED */
   protected void setSource(String source) { 
      if( source==null && first ) {
         System.out.println("SED.setSource("+source+")");
         first=false;
         try { throw new Exception("ICI"); }
         catch( Exception e ) { e.printStackTrace(); }
      }
      this.source=source;
   }
   
   /** Mémorise le rayon associé au SED */
   protected void setRadius(double radius) { this.radius = radius; }
   
   /** Retourne le nombre de points du SED, -1 si non encore chargée */
   public int getCount() {
      if( sedList==null ) return -1;
      return sedList.size();
   }
   
   /** Nettoyage de la liste */
   public void clear() { 
      planeAlreadyCreated=readyToDraw=false;
      xOptMax=xOptMin=0;
      fluxMin=fluxMax=0;
      absMin=absMax=0;
      if( sedList!=null ) sedList.clear();
   }
   
   /** Etend un SED à partir d'un pcat déjà chargé, typiquement à partir d'un plan
    * catalogue déjà dans la pile */
   protected void addFromIterator(Iterator<Source> it) {
      planeAlreadyCreated=true;
      readyToDraw=false;
      try {
         if( plan==null ) plan = new PlanCatalog(aladin);
         createSEDlist(it);
         setPosition();
         aladin.calque.zoom.zoomView.flagSED=true;
         aladin.calque.repaintAll();
         
      } catch( Exception e ) {
         aladin.view.zoomview.setSED((String)null);
         aladin.command.printConsole("!!! VizieR photometry parsing error => "+e.getMessage());
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   /** Indique la source (correspond à un point du SED) qu'il faut mettre en évidence */
   protected void setHighLight(Source o) {
      for( SEDItem si : sedList )  si.highLight = si.o==o;
   }
   
   /** Charge et crée un SED à partir d'un identificateur de source astronomique (à la Sésame) */
   protected void loadFromSource(String source) {
      clear();
      this.source = source;
      try {
         aladin.trace(2,"VizieR photometry loading around source \""+source+"\"...");
         url = ""+aladin.glu.getURL(SEDGLUTAG,Glu.quote(source)+" "+radius);
//         url = "http://cdsarc.u-strasbg.fr/viz-bin/sed?-c="+URLEncoder.encode(source) +"&-c.rs="+radius;
         aladin.trace(2,"Phot. loading: "+url);
         loadASync( Util.openAnyStream(url) );
      } catch( Exception e ) {
         aladin.view.zoomview.setSED((String)null);
         aladin.command.printConsole("!!! VizieR photometry builder error ["+source+"] => "+e.getMessage());
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   // Creation d'un plan catalogue dans la pile à partir des données du SED
   // On ne peut le faire qu'une seule fois
   private void createStackPlane() {
      if( planeAlreadyCreated ) return;
      planeAlreadyCreated = true;
      plan.label="PHOT "+source;
      plan.objet = source;
      try { plan.u=new URL(url); } catch( Exception e ) { }
      aladin.calque.newPlan(plan);
      plan.planReady(true);
      aladin.calque.selectPlan(plan);   // sélectionne également les sources du plan
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
               aladin.command.printConsole("!!! VizieR photometry parsing error => "+e.getMessage());
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            } finally {
               if( inParam!=null ) try { inParam.close(); } catch( Exception e ) {}
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

   /** Retourne la fréquence en GHz de la source */
   protected double getFreq(Source s)    { return getSEDValue(s,Field.FREQ); }
   
   /** Retourne le flux en Jy de la source */
   protected double getFlux(Source s)    { return getSEDValue(s,Field.FLUX); }
   
   /** Retourne l'erreur sur le flux de la source */
   protected double getFluxErr(Source s) { 
      double x = getSEDValue(s,Field.FLUXERR);
      if( Double.isNaN(x) ) return 0;
      return x;
   }
   
   // Procédure interne d'accès aux valeurs numériques du SED
   private double getSEDValue(Source s,int sed) {
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
   
   /** Retourne l'identificateur du filtre associé à la source */
   protected String getSEDId(Source s) {
      Legende leg = s.leg;
      String sId=null;
      for( int i = 0; i<leg.field.length; i++ ) {
         if( leg.field[i].sed == Field.SEDID ) sId = s.getValue(i);
      }
      return sId;
   }
   
   // Génère la liste des points SED "prémachés" sous la forme d'un ArrayList de SEDItem
   private void createSEDlist(Iterator<?> it) {
       if( sedList==null ) sedList = new ArrayList<SEDItem>();
       while( it.hasNext() ) {
          Source s = (Source) it.next();
          double freq = getFreq(s);
          double flux = getFlux(s);
          if( Double.isNaN(freq) || Double.isNaN(flux) ) continue;
          if( freq<=0 || flux<=0 ) continue; // Pas possible en log
          double fluxErr = getFluxErr(s);
          String sedId = getSEDId(s);
          s.setSelect(true);
          SEDItem si = new SEDItem(s,freq,flux,fluxErr,sedId);
          sedList.add(si);
       }
   }
   
   /** Retourne true si l'absisse est en longueur d'onde plutôt qu'en fréquence */
   protected boolean getSEDWave() { return flagWavelength; }
   
   // Détermine les intervalles de fréquence et de flux, et en déduit les positions
   // de traçage de chaque point du SED
   // lorsque c'est terminé, le tracé peut être opéré
   private void setPosition() {
      
      // Recherche des intervalles
      absMin = fluxMin = Double.MAX_VALUE;
      absMax = fluxMax = Double.MIN_VALUE;
      for( SEDItem si : sedList ) {
         if( absMin > si.freq ) absMin = si.freq;
         if( si.flux-si.fluxErr>0 ) {  if( fluxMin > si.flux-si.fluxErr ) fluxMin = si.flux-si.fluxErr; }
         else if( fluxMin > EPSILON ) fluxMin=EPSILON;
         if( absMax < si.freq ) absMax = si.freq;
         if( fluxMax < si.flux+si.fluxErr ) fluxMax = si.flux+si.fluxErr;
      }
      
      // Permutation fréquences <=> longueurs d'onde
      if( flagWavelength ) {
         double fr = freq2Wave(absMin);
         absMin = freq2Wave(absMax);
         absMax = fr;
         if( absMin>WAVEMIN ) absMin=WAVEMIN;
         if( absMax<WAVEMAX ) absMax=WAVEMAX;
      } else {
         if( absMin>FREQMIN ) absMin=FREQMIN;
         if( absMax<FREQMAX ) absMax=FREQMAX;
      }
      
      // Affectation des positions
      Dimension dim = getDimension();
      dim.width -= (margeDroite+margeGauche);
      dim.height -= (margeHaut+margeBas);
      double rangeAbs = LOG(absMax) - LOG(absMin);
      double rangeFlux = LOG(fluxMax) - LOG(fluxMin);
      coefX = dim.width / rangeAbs;
      coefY = dim.height / rangeFlux;
      for( SEDItem si : sedList ) {
         double fr = flagWavelength ? freq2Wave(si.freq) : si.freq;
         double x = ( LOG(fr) - LOG(absMin)) * coefX;
         double y = ( LOG(si.flux) - LOG(fluxMin)) * coefY;
         si.setBox( (int)x, (int)(dim.height-y));
         
         if( si.fluxErr!=0 ) {
            double hy=0; 
            if( (si.flux-si.fluxErr)>0 ) hy = ( LOG(si.flux-si.fluxErr) - LOG(fluxMin)) * coefY;
            double by = ( LOG(si.flux+si.fluxErr) - LOG(fluxMin)) * coefY;
            si.setBoxErr((int)(dim.height-by),(int)(dim.height-hy));
         }
      }
      
      // Mémorisation de la portion optique
      if( flagWavelength ) {
         xOptMin = (int) ( ( LOG( freq2Wave(FREQOPTMAX)) - LOG(absMin)) * coefX );
         xOptMax = (int) ( ( LOG( freq2Wave(FREQOPTMIN)) - LOG(absMin)) * coefX );
      } else {
         xOptMin = (int) ( ( LOG(FREQOPTMIN) - LOG(absMin)) * coefX );
         xOptMax = (int) ( ( LOG(FREQOPTMAX) - LOG(absMin)) * coefX );
      }
      
      // Prêt à être dessiné
      readyToDraw = true;
   }
   
   // Retourne la fréquence/longueur d'onde courante sous la souris
   private double getCurrentAbs(double x) {
      if( !readyToDraw ) return Double.NaN;
      return POW( x/coefX + LOG(absMin) );
   }
   
   // Retourne le flux courant sous la souris
   double getCurrentFlux(double y) {
      if( !readyToDraw ) return Double.NaN;
      return POW( y/coefY + LOG(fluxMin) );
   }
   
   /** Classe interne qui gère un point du SED */
   private class SEDItem {
      private double freq,flux,fluxErr;     // Fréquence, Flux et erreur sur Flux
      private String sedId;                 // identificateur du filtre associé
      private Source o;                     // Source aladin associée à la mesure
      private boolean highLight;            // true si on doit mettre en avant ce point
      private Rectangle r;                  // rectangle qui englobe le point
      private int by,hy;                    // ordonnées de la barre d'erreur verticale

      static final int W = 6;               // taille du carré englobant
      
      private SEDItem(Source o,double freq,double flux,double fluxErr,String sedId) {
         this.o = o;
         this.freq = freq;
         this.flux = flux;
         this.fluxErr = fluxErr;
         this.sedId = sedId;
         by=hy=0;
      }
      
      // Positionne la boite englobante du point
      private void setBox(int x, int y) { r = new Rectangle(margeGauche+x-W/2, margeHaut+y-W/2, W,W); }
      
      // Positionne les bornes de la barre d'erreur verticale
      private void setBoxErr(int by, int hy) { this.by= margeHaut+by; this.hy= margeHaut+hy; }
      
      // Retourne true si la souris est dans la boite englobante
      private boolean contains(int x,int y) { return r.contains(x, y); }
      
      // Trace le point
      private void draw(Graphics g) {
         g.setColor( o.getColor() );
         Util.fillCircle5(g, r.x+W/2, r.y+W/2);
         
         // Barre d'erreur
         if( Math.abs(by-hy)>5 ) {
            g.drawLine(r.x+W/2,by,r.x+W/2,hy);
            g.drawLine(r.x+1,by,r.x+W-1,by);
            g.drawLine(r.x+1,hy,r.x+W-1,hy);
         }

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
            if( !flagWavelength ) s = getUnitFreq(freq);
            else s = getUnitWave(freq2Wave(freq));
            g.drawString(s, 5,getDimension().height-3);
            
            s=getUnitJy(flux);
            g.drawString(s, dim.width-5-fm.stringWidth(s),getDimension().height-3);
         }
      }
   }
   
   /** Retourne la dimension du graphique */
   public Dimension getDimension() { return new Dimension(ZoomView.SIZE,ZoomView.SIZE); }
   
   static final private Color COLOROPT = new Color(234,234,255);
   
   // Tracé du graphique
   protected void draw(Graphics g) {
      Dimension dim = getDimension();
      int haut=margeHaut, bas=dim.height-margeBas;
      int gauche=margeGauche, droite=dim.width-margeDroite;
      
      // Nettoyage
      g.setColor(Color.white);
      g.clearRect(0, 0, dim.width, dim.height);
      
      // Bande optique
      if( xOptMin!=xOptMax ) {
         g.setColor(COLOROPT);
         g.fillRect(gauche+Math.min(xOptMin,xOptMax), haut, Math.abs(xOptMax-xOptMin+1), bas-haut);
      }
      
      // Tracé des axes
      int arrow=3;
      g.setColor(Color.gray);
      g.drawLine(gauche, haut, gauche, bas);
      g.drawLine(gauche, haut, gauche-arrow, haut+arrow);
      g.drawLine(gauche, haut, gauche+arrow, haut+arrow);
      g.drawLine(gauche, bas, droite, bas);
      g.drawLine(droite, bas, droite-arrow, bas-arrow);
      g.drawLine(droite, bas, droite-arrow, bas+arrow);
      
      // Légende
      g.setColor(Color.black);
      g.setFont(Aladin.SSPLAIN);
      g.drawString("log f("+NU+")",gauche-2,haut-4);
      g.drawString((fluxMax==0 ? "":getUnitJy(fluxMax)),gauche+4,haut+6);
      g.drawString("log "+(flagWavelength ? MU : NU),droite+2,bas);
      
      // Tracé de la valeur sous la souris
      g.setColor(LIGHTGRAY);
      String s;
      if( !Double.isNaN(currentAbs) ) {
         g.drawLine(currentX,bas,currentX,bas-5);
         g.setFont(Aladin.SSPLAIN);
         g.drawString( Util.myRound( LOG(currentAbs)+"",1), currentX, bas-8);
         if( siIn==null ) {
            g.setFont(Aladin.BOLD);
            if( !flagWavelength ) s =getUnitFreq(currentAbs);
            else s = getUnitWave(currentAbs);
            g.drawString(s, 5,dim.height-3);
         }
      }
      if( !Double.isNaN(currentFlux) ) {
         g.drawLine(gauche,currentY,gauche+5,currentY);
         g.setFont(Aladin.SSPLAIN);
         g.drawString( Util.myRound( LOG(currentFlux)+"",1), gauche+10, currentY);
         if( siIn==null ) {
            g.setFont(Aladin.BOLD);
            s=getUnitJy(currentFlux);
            g.drawString(s, dim.width-5-g.getFontMetrics().stringWidth(s),dim.height-3);
         }
      }
      
      // Les icones
      drawCroix(g);
      drawWave(g);
      drawMore(g);
//      drawInfo(g);
      drawHelp(g);

      SEDItem siIn=null;
      
      // Pas encore prêt
      if( sedList==null || !readyToDraw ) {
         g.setFont(Aladin.ITALIC);
         s = aladin.chaine.getString("SEDLOADING");
         int y = (haut+bas)/2-20;
         g.drawString(s,dim.width/2-g.getFontMetrics().stringWidth(s)/2,y+=18);
         s = Coord.getUnit(radius/3600.)+" around";
         g.drawString(s,dim.width/2-g.getFontMetrics().stringWidth(s)/2,y+=18);
         s = source;
         g.drawString(s,dim.width/2-g.getFontMetrics().stringWidth(s)/2,y+=18);
         if( sedList==null || !readyToDraw ) return;
         
      // Tracé des points
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
         s = flagWavelength ? getUnitWave(absMin) :  getUnitFreq(absMin);
         g.drawString(s,gauche,bas+10);
         s = flagWavelength ? getUnitWave(absMax) :  getUnitFreq(absMax);
         g.drawString(s,20+droite - g.getFontMetrics().stringWidth(s),bas+10);
      }
      
      // Tracé du titre du graphique : le nom de la source
      if( source!=null ) {
         g.setColor(Aladin.GREEN);
         g.setFont(Aladin.BOLD);
         String s1 = "VizieR Phot. at "+Coord.getUnit(radius/3600.);
         int size = g.getFontMetrics().stringWidth(s1);
         int x = dim.width/2-size/2;
         if( x<50 ) x=50;
         g.drawString(s1,x, haut-4);
         if( Double.isNaN(currentFlux) && Double.isNaN(currentAbs) && siIn==null ) {
            s1 = source;
            size = g.getFontMetrics().stringWidth( s1 );
            x = dim.width/2-size/2;
            if( x<50 ) x=50;
            g.drawString(s1,x, dim.height-7);
         }
      }

   }
   
   static double freq2Wave(double freq) { return 2.998E5/freq; }
   static double wave2Freq(double wave) { return 2.998E5/wave; }
   
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
//   private void drawInfo(Graphics g) {
//      if( planeAlreadyCreated ) {
//         rInfo = new Rectangle(0,0,0,0);
//         return;
//      }
//      int w=5;
//      int width=getDimension().width;
//      g.setColor(Aladin.BKGD);
//      rInfo = new Rectangle(width-w-4,6+w,w+4,w+4);
//      g.fillRect(rInfo.x,rInfo.y,rInfo.width,rInfo.height);
//      g.setColor(Color.blue);
//      g.setFont(Aladin.SBOLD);
//      g.drawString("^",rInfo.x+2,rInfo.y+10);
//   }
   
   // Trace l'icone de demande de chargement du SED dans l'outil Web
   private void drawMore(Graphics g) {
      int w=5;
      Dimension dim=getDimension();
      g.setColor(Aladin.BKGD);
      rMore = new Rectangle(dim.width-w-4,16+3*w,w+4,w+4);
      g.fillRect(rMore.x,rMore.y,rMore.width,rMore.height);
      g.setColor( flagWavelength ? Color.blue : Color.red );
      g.setFont(Aladin.SBOLD);
      g.drawString("+",rMore.x+2,rMore.y+8);
   }
   
   // Trace l'icone de demande de passage freq <-> longueur d'onde
   private void drawWave(Graphics g) {
      int w=5;
      Dimension dim=getDimension();
      g.setColor(Aladin.BKGD);
      rWave = new Rectangle(dim.width-w-4,11+2*w,w+4,w+4);
      g.fillRect(rWave.x,rWave.y,rWave.width,rWave.height);
      g.setColor( flagWavelength ? Color.blue : Color.red );
      g.setFont(Aladin.SBOLD);
      g.drawString(TILDE+"",rWave.x+1,rWave.y+8);
   }
   
   // Trace l'icone du help
   private void drawHelp(Graphics g) {
      int w=5;
      Dimension dim=getDimension();
      g.setColor(Aladin.BKGD);
      rHelp = new Rectangle(dim.width-w-4,6+w,w+4,w+4);
      g.fillRect(rHelp.x,rHelp.y,rHelp.width,rHelp.height);
      g.setColor( Color.blue );
      g.setFont(Aladin.SBOLD);
      g.drawString("?",rHelp.x+2,rHelp.y+8);
   }
   
   /** Actions à effectuer lors du relachement de la souris */
   protected void mouseRelease(int x,int y) {
      if( rCroix.contains(x,y) ) aladin.view.zoomview.setSED((String)null);
//      else if( rInfo.contains(x,y) ) createStackPlane();
      else if( rMore.contains(x,y) ) more();
      else if( rHelp.contains(x,y) ) help();
      else if( rWave.contains(x,y) ) {
         flagWavelength = !flagWavelength;
         setPosition();
         aladin.view.zoomview.repaint();
      } else if( siIn!=null ) {
         int bloc=1;
         if( !planeAlreadyCreated ) createStackPlane();
         else bloc=2;
         aladin.view.showSource(siIn.o, false, true);
         aladin.mesure.mcanvas.show(siIn.o, bloc);
         aladin.calque.repaintAll();
      } 
   }
   
   private void more() {
      if( source==null ) {
         System.out.println("SED.more() source=null");
         return;
      }
      System.out.println("SED.more() source="+source);
      
      // Je dois utiliser le %20 plutôt que le '+' pour l'encodage des blancs
      // parce que l'outil VizieR photometry ne les supporte pas
      // CE N'EST PLUS LA PEINE
//      String target = URLEncoder.encode(source);
//      target = target.replaceAll("[+]","%20");
//      aladin.glu.showDocument("Widget.VizPhot", target+" "+radius);
      aladin.glu.showDocument("Widget.VizPhot", Glu.quote(source)+" "+radius);
   }
   
   // Affiche un message explicatif sur le SED
   private void help() {
      aladin.info( aladin.chaine.getString("SEDHELP"));
   }
   
   /** Actions à effectuer lorsque la souris sort du cadre */
   protected void mouseExit() {
      currentAbs=currentFlux=Double.NaN;
      aladin.view.simRep = null;
   }
   
   /** Actions à effectuer lorsque la souris entre dans le cadre */
   protected void mouseEnter() {
      aladin.view.simRep = simRep;
      if( simRep!=null ) aladin.view.repaintAll();
   }
   
   /** Associe le bon tooltip */
   private void toolTip(String k) {
      String s = k==null ? null : aladin.chaine.getString(k);
      Util.toolTip(aladin.view.zoomview, s);
   }
   
   /** Actions à effectuer lorsque la souris survole le graphique */
   protected void mouseMove(int x, int y) {
      
      // Tooltips ?
      if( rCroix.contains(x,y) )     { toolTip("SEDCLOSE");       return; }
      if( rMore.contains(x,y) )      { toolTip("SEDMORE");       return; }
//      else if( rInfo.contains(x,y) ) { toolTip("SEDCREATEPLANE"); return; }
      else if( rWave.contains(x,y) ) { toolTip("SEDFREQWAVE");    return; }
      else if( rHelp.contains(x,y) ) { toolTip("SEDHELPTIP");     return; }
      else toolTip(null);

      // Y a-t-il un point de SED sous la souris ?
      siIn=null;
      if( sedList==null ) return;
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
         currentAbs = getCurrentAbs( x-margeGauche );
      } else currentAbs = Double.NaN;

      if( y>margeHaut+14 && y<dim.height-margeBas-(x>margeGauche+30?0:14) ) {
         currentFlux = getCurrentFlux( (double)(dim.height - (margeBas+margeHaut)) - (double)(y-margeHaut) ) ;
      } else currentFlux = Double.NaN;
      
      // Mémorisation de la position sous la souris
      currentX=x;
      currentY=y;
   }
   
   /** Actions à effectuer sur le mouvement de la roulette de la souris */
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
   
   static public double LOG(double x) { return Math.log(x)/Math.log(10.); }
   static public double POW(double x) { return Math.exp(x * Math.log(10.) ); }
   
   static final private String UNITFREQ[] = {"Hz","kHz", "MHz","GHz","THz","PHz","EHz"};
   static final private String UNITWAVE[] = {"nm",MU+"m", "mm","m","km","Mm","Gm"};
   static final private String UNITJY[]   = {MU+"Jy","mJy","Jy","kJy","MJy","GJy","TJy","PJy"};
  
   /** Affichage en fréquence : val donnée en GHz */
   static final public String getUnitFreq(double val) {
      return getUnit(val*1000000000.,UNITFREQ);
   }

   /** Affichage en Jansky : val donnée en GJy */
   static final public String getUnitJy(double val) {
      return getUnit(val*1000000.,UNITJY);
   }

   /** Affichage en longueur d'onde : val donnée en micron-mètre */
   static final public String getUnitWave(double val) {
      return getUnit(val*1000.,UNITWAVE);
   }

   static final private String getUnit(double val,String [] unit) {
      int u = 0;
      while (val >= 1000 && u<unit.length-1) { u++; val /= 1000L; }
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(1);
      return nf.format(val)+unit[u];
   }



   
}