// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.Timer;

import cds.tools.Util;
import cds.xml.Field;

/**
 * Gestion du chargement et du trac� simplifi� d'un SED au format VizieR
 * @author Pierre Fernique [CDS]
 * @version 1.0 nov 2012
 */
public class SED extends JPanel {
   
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
   private static final double REFMIN = 1.;
   private static final double REFMAX = 10.;
   private static final double FREQOPTMIN = wave2Freq(WAVEOPTMAX);
   private static final double FREQOPTMAX = wave2Freq(WAVEOPTMIN);
   
   static private final Color LIGHTGRAY = new Color(125,125,125);

   private Aladin aladin;
   private PlanCatalog plan;    // Le planCatalog qui va recueillir les infos du SED, il ne sera pas affich� dans la pile
   private String source;       // Le nom de la source concern�e par le SED
   private double radius;       // Le rayon de recherche de la SED (en arcsec)
   private Repere simRep;       // Le repere � afficher dans la vue qui pointe sur la source du SED
   private String url;          // Pour m�moire
   private float transparency;  // Niveau de transparence d'affichage des points
   
   private double absMin,absMax;    // Intervalle de fr�quences/longueurs d'onde du SED
   private double fluxMin,fluxMax;  // Intervalle de flux du SED
   private double coefX,coefY;      // Facteur de l'homoth�tie de tra�age (d�termin� dans setPosition())
   private boolean readyToDraw;     // true si tout est pr�t pour �tre trac�
   private boolean planeAlreadyCreated;  // true si le plan dans la pile a d�j� �t� cr�� � partir du SED
   private boolean flagWavelength;  // true pour un affichage en longueur d'onde
   private int xOptMin,xOptMax;     // Position de la bande optique
   private int yRefMin,yRefMax;     // Position de la bande d'�nergie de r�f�rence
   
   private ArrayList<SEDItem> sedList;  // Liste des points du SED sous une forme "pr�mach�e"
   
   private int w;                         // Taille des icones
   private Rectangle rCroix,rWave,rHelp,rMore;  // Position des icones sur le graphiques
   private double currentAbs=Double.NaN;  // Derni�re fr�quence/longueur d'onde sous la souris
   private double currentFlux=Double.NaN; // Dernier flux sous la souris
   private int currentX,currentY;         // Derni�re position de la souris
   private SEDItem siIn;                  // !=null si sous la souris
   
   private TimeIcon timeIcon;       // Le bouton pour afficher un Time plot
   private TableIcon tableIcon;       // Le bouton pour afficher la table du SED
   
   static private Color COLOROPT;
   static private Color COLORREF;
   

   
   public SED(Aladin aladin) {
      this.aladin = aladin;
      transparency = 0.5f;
      flagWavelength = aladin.configuration.getSEDWave();
      radius = 5;
      
      COLOROPT = Aladin.DARK_THEME ? new Color(47,68,83) : new Color(234,234,255);
      COLORREF = Aladin.DARK_THEME ? Aladin.COLOR_STACK_HIGHLIGHT : new Color(255,234,234);
      
      w = Math.round(5*aladin.getUIScale());
      timeIcon = new TimeIcon();
      tableIcon = new TableIcon();
   }
   
   /** M�morise le rep�re de la vue afin de pouvoir le r�afficher ult�rieurement
    * si l'utilisateur d�place la souris sur le SED */
   protected void setRepere(Repere simRep) { this.simRep=simRep; }
   
   
   static boolean first=true;
   
   /** M�morise le source associ�e au SED */
   protected void setSource(String source) { 
      if( source==null && first ) {
         System.out.println("SED.setSource("+source+")");
         first=false;
      }
      this.source=source;
   }
   
   /** M�morise le rayon associ� au SED */
   protected void setRadius(double radius) { this.radius = radius; }
   
   /** Retourne le nombre de points du SED, -1 si non encore charg�e */
   public int getCount() {
      if( sedList==null ) return -1;
      return sedList.size();
   }
   
   /** Nettoyage de la liste */
   public void clear() { 
      planeAlreadyCreated=readyToDraw=false;
      xOptMax=xOptMin=0;
      yRefMax=yRefMin=0;
      fluxMin=fluxMax=0;
      absMin=absMax=0;
      sedList=null;
//      if( sedList!=null ) sedList.clear();
   }
   
   /** Etend un SED � partir d'un pcat d�j� charg�, typiquement � partir d'un plan
    * catalogue d�j� dans la pile */
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
         aladin.view.zoomview.setSED((String)null,(String)null);
         aladin.command.printConsole("!!! VizieR photometry parsing error => "+e.getMessage());
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   /** Indique la source (correspond � un point du SED) qu'il faut mettre en �vidence */
   protected void setHighLight(Source o) {
      for( SEDItem si : sedList )  si.highLight = si.o==o;
   }
   
   /** Charge et cr�e un SED � partir d'un identificateur de source astronomique (� la S�same)
    * @param position position de la source (r�sultat S�same)
    * @param source identificateur de la source
    */
   protected void loadFromSource(String position, String source) {
      clear();
      this.source = source;
      try {
         aladin.trace(2,"VizieR photometry loading around source \""+source+"\"...");
         url = ""+aladin.glu.getURL(SEDGLUTAG,Glu.quote(position)+" "+radius);
         aladin.trace(2,"Phot. loading: "+url);
         loadASync( url );
      } catch( Exception e ) {
         aladin.view.zoomview.setSED((String)null,(String)null);
         aladin.command.printConsole("!!! VizieR photometry builder error ["+source+"] => "+e.getMessage());
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   // Creation d'un plan catalogue dans la pile � partir des donn�es du SED
   // On ne peut le faire qu'une seule fois
   private void createStackPlane() {
      if( planeAlreadyCreated ) return;
      planeAlreadyCreated = true;
      plan.label="PHOT "+source;
      plan.objet = source;
      try { plan.u=new URL(url); } catch( Exception e ) { }
      aladin.calque.newPlan(plan);
      plan.planReady(true);
      aladin.calque.selectPlan(plan);   // s�lectionne �galement les sources du plan
   }
   
   private boolean isLoading=false;
   
   /** True si on est en train de charger un SED */
   protected synchronized boolean isLoading() {
      return isLoading;
   }
   
   private synchronized void setIsLoading(boolean flag) { isLoading=flag; }
   
   // Chargement et cr�ation d'un SED � partir d'un flux de mani�re asynchrone
   private void loadASync( final String url) {
      planeAlreadyCreated=readyToDraw=false;
      setIsLoading(true);
      clear();
      aladin.view.zoomview.repaint();

      plan = new PlanCatalog(aladin);
      plan.pcat = new Pcat(plan,Color.black,aladin.calque,aladin.status,aladin);
      (new Thread() {  
         public void run() {
            Util.pause(10);
            MyInputStream inParam =  null;
            try {
               inParam = Util.openAnyStream(url,10000);
               plan.pcat.tableParsing(inParam, "TABLE");
               parseAndDraw();
               
            } catch( Exception e ) {
               
               // On fait tout de suite un deuxi�me essai car VizieR a souvent ses vapeurs
               try {
                  inParam = Util.openAnyStream(url,20000);
                  plan.pcat.tableParsing(inParam, "TABLE");
                  parseAndDraw();
                 
               } catch( Exception e1 ) {
                  aladin.view.zoomview.setSED((String)null,(String)null);
                  aladin.command.printConsole("!!! VizieR photometry parsing error => "+e1.getMessage());
                  if( aladin.levelTrace>=3 ) e1.printStackTrace();
               }
            } finally {
               if( inParam!=null ) try { inParam.close(); } catch( Exception e ) {}
                setIsLoading(false);
            }
         }
      } ).start();
   }

   // Mise en place des listes de points du SED et des conversions de coordonn�es
   // Et demande d'affichage
   private void parseAndDraw() throws Exception {
      createSEDlist( plan.iterator() );
      setPosition();
      aladin.calque.zoom.zoomView.flagSED=true;
      aladin.calque.repaintAll();
   }
   
   // Active le bouton TimePlot si le SED contient une colonne de temps avec au moins 2 valeurs
   private void activateTimePlot() {
      boolean rep=false;
      int ntime=-1;
      int n=0;
      String val0=null;
      try {
         for( SEDItem si: sedList ) {
            Source s = si.o;
            
            // rep�rage du champ qui porte le temps
            if( ntime<0 ) {
               ntime=getField(s, Field.TIME);
               if( ntime<0 ) break;        // Pas de colonne de temps
            }
            
            // V�rification qu'il y a au-moins 2 sources qui ont un temps diff�rent
            String val = s.getValue( ntime ).trim();
            
            if( val.length()>0 && (val0==null || !val0.equals(val)) ) { val0=val; n++; }
            if( n>1 ) { rep=true; break; }
            
         }
      } catch( Exception e ) {}
      timeIcon.activate(  rep );
   }
   
   // Retourne true s'il existe d�j� un Plot temporel pour ce SED
   private boolean hasTimePlot() {
      if( !planeAlreadyCreated ) return false;
      for( int i=0; i<aladin.view.getNbView(); i++ ) {
         ViewSimple v = aladin.view.viewSimple[i];
         if( v.isPlotTime() && v.pref==plan ) return true;
      }
      return false;
   }

   /** Retourne la fr�quence en GHz de la source */
   protected double getFreq(Source s)    { return getSEDValue(s,Field.FREQ); }
   
   /** Retourne le flux en Jy de la source */
   protected double getFlux(Source s)    { return getSEDValue(s,Field.FLUX); }
   
   /** Retourne l'erreur sur le flux de la source */
   protected double getFluxErr(Source s) { 
      double x = getSEDValue(s,Field.FLUXERR);
      if( Double.isNaN(x) ) return 0;
      return x;
   }
   
   // retourne l'indice du champ de type sed (Field.FREQ, Field.FLUX, Field.TIME...), -1 si non trouv�
   private int getField(Source s, int sed ) {
      Legende leg = s.getLeg();
      for( int i = 0; i<leg.field.length; i++ ) {
         if( leg.field[i].sed == sed ) return i;
      }
      return -1;
   }
   
   // Proc�dure interne d'acc�s aux valeurs num�riques du SED
   private double getSEDValue(Source s,int sed) {
      try {
         int i = getField(s,sed);
         String val = s.getValue(i);
         return Double.parseDouble(val.trim());
      } catch( Exception e ) {  
         return Double.NaN;
      }
   }
   
   /** Retourne l'identificateur du filtre associ� � la source */
   protected String getSEDId(Source s) {
      int i = getField(s,Field.SEDID);
      try {
         return s.getValue(i);
      } catch( Exception e ) {
         return "Unknown";
      }
   }
   
   // G�n�re la liste des points SED "pr�mach�s" sous la forme d'un ArrayList de SEDItem
   private void createSEDlist(Iterator<?> it) {
       if( sedList==null ) sedList = new ArrayList<>();
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
       
       // Active le bouton time plot si possible
       activateTimePlot();
   }
   
   /** Retourne true si l'absisse est en longueur d'onde plut�t qu'en fr�quence */
   protected boolean getSEDWave() { return flagWavelength; }
   
   // D�termine les intervalles de fr�quence et de flux, et en d�duit les positions
   // de tra�age de chaque point du SED
   // lorsque c'est termin�, le trac� peut �tre op�r�
   private void setPosition() {
      
      if( sedList==null ) return;
      
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
      
      // Permutation fr�quences <=> longueurs d'onde
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
      
      // M�morisation de la bande d'�nergie de r�f�rence
      yRefMin = (int) ( ( LOG(REFMIN) - LOG(fluxMin)) * coefY );
      yRefMax = (int) ( ( LOG(REFMAX) - LOG(fluxMin)) * coefY );

      // M�morisation de la portion optique
      if( flagWavelength ) {
         xOptMin = (int) ( ( LOG( freq2Wave(FREQOPTMAX)) - LOG(absMin)) * coefX );
         xOptMax = (int) ( ( LOG( freq2Wave(FREQOPTMIN)) - LOG(absMin)) * coefX );
      } else {
         xOptMin = (int) ( ( LOG(FREQOPTMIN) - LOG(absMin)) * coefX );
         xOptMax = (int) ( ( LOG(FREQOPTMAX) - LOG(absMin)) * coefX );
      }
      
      // Pr�t � �tre dessin�
      readyToDraw = true;
   }
   
   // Retourne la fr�quence/longueur d'onde courante sous la souris
   private double getCurrentAbs(double x) {
      if( !readyToDraw ) return Double.NaN;
      return POW( x/coefX + LOG(absMin) );
   }
   
   // Retourne le flux courant sous la souris
   double getCurrentFlux(double y) {
      if( !readyToDraw ) return Double.NaN;
      return POW( y/coefY + LOG(fluxMin) );
   }
   
   /** Classe interne qui g�re un point du SED */
   private class SEDItem {
      private double freq,flux,fluxErr;     // Fr�quence, Flux et erreur sur Flux
      private String sedId;                 // identificateur du filtre associ�
      private Source o;                     // Source aladin associ�e � la mesure
      private boolean highLight;            // true si on doit mettre en avant ce point
      private Rectangle r;                  // rectangle qui englobe le point
      private int by,hy;                    // ordonn�es de la barre d'erreur verticale

      static final int W = 6;               // taille du carr� englobant
      
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

         // Mise en �vidence de ce point particuli�rement
         if( highLight ) {
            g.setColor( Aladin.COLOR_GREEN);
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
//   public Dimension getDimension() { return new Dimension(ZoomView.getSIZE(),ZoomView.getSIZE()); }
   public Dimension getDimension() { return new Dimension(aladin.calque.zoom.zoomView.getWidth(),aladin.calque.zoom.zoomView.getHeight()); }
   
   
   private int lastWidth=0,lastHeight=0;
   
   // Trac� du graphique
   protected void draw(Graphics g) {
      Dimension dim = getDimension();
      if( dim.width!=lastWidth  || dim.height!=lastHeight ) {
         setPosition();
         lastWidth=dim.width;
         lastHeight=dim.height;
      }
      int haut=margeHaut, bas=dim.height-margeBas;
      int gauche=margeGauche, droite=dim.width-margeDroite;
      
      // Nettoyage
      g.setColor( Aladin.COLOR_BACKGROUND );
      g.fillRect(0, 0, dim.width, dim.height);
      
      // Bande d'�nergie (0.5..1mJy) de r�f�rence
      if( yRefMin!=yRefMax ) {
         g.setColor(COLORREF);
         g.fillRect(gauche,bas-Math.max(yRefMin,yRefMax), droite-gauche, Math.abs(yRefMax-yRefMin+1) );
      }

      // Bande optique de r�f�rence
      if( xOptMin!=xOptMax ) {
         g.setColor(COLOROPT);
         g.fillRect(gauche+Math.min(xOptMin,xOptMax), haut, Math.abs(xOptMax-xOptMin+1), bas-haut);
      }

      // Trac� des axes
      int arrow=3;
      g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
      g.drawLine(gauche, haut, gauche, bas);
      g.drawLine(gauche, haut, gauche-arrow, haut+arrow);
      g.drawLine(gauche, haut, gauche+arrow, haut+arrow);
      g.drawLine(gauche, bas, droite, bas);
      g.drawLine(droite, bas, droite-arrow, bas-arrow);
      g.drawLine(droite, bas, droite-arrow, bas+arrow);
      
      // L�gende
      g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
      g.setFont(Aladin.SSPLAIN);
      g.drawString("log f("+NU+")",gauche-2,haut-4);
      g.drawString((fluxMax==0 ? "":getUnitJy(fluxMax)),gauche+4,haut+6);
      g.drawString("log "+(flagWavelength ? MU : NU),droite+2,bas);
      
      // Trac� de la valeur sous la souris
      g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
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
      
      g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
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
      
      // Pas encore pr�t
      if( sedList==null || !readyToDraw ) {
         
         s = aladin.chaine.getString("SEDLOADING");
         int x = dim.width/2-g.getFontMetrics().stringWidth(s)/2;
         int y = (haut+bas)/2-20;
         drawBlink(g,x-15,y+10);
         
         g.setFont(Aladin.ITALIC);
         g.setColor( Aladin.COLOR_CONTROL_FOREGROUND); //Aladin.COLOR_GREEN );
         g.drawString(s,x,y+=18);
         s = Coord.getUnit(radius/3600.)+" around";
         g.drawString(s,dim.width/2-g.getFontMetrics().stringWidth(s)/2,y+=18);
         s = source;
         g.drawString(s,dim.width/2-g.getFontMetrics().stringWidth(s)/2,y+=18);
         if( sedList==null || !readyToDraw ) return;
         
      // Trac� des points
      } else {
         stopBlink();
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
      
      // Trac� de l'intervalle de Fr�quence
      if( siIn==null ) {
         g.setColor( Aladin.COLOR_CONTROL_FOREGROUND );
         g.setFont(Aladin.SPLAIN);
         s = flagWavelength ? getUnitWave(absMin) :  getUnitFreq(absMin);
         g.drawString(s,gauche,bas+10);
         s = flagWavelength ? getUnitWave(absMax) :  getUnitFreq(absMax);
         g.drawString(s,20+droite - g.getFontMetrics().stringWidth(s),bas+10);
      }
      
      // Trac� du titre du graphique : le nom de la source
      if( source!=null ) {
         g.setColor(Aladin.COLOR_GREEN);
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
      
      timeIcon.draw(g);     
      tableIcon.draw(g);     

   }
   
   static public double freq2Wave(double freq) { return 2.998E5/freq; }
   static public double wave2Freq(double wave) { return 2.998E5/wave; }
   
   static private String TIMEPLOT = null;
   static private String TABLEICON = null;
   
   class TimeIcon {
      int w = 18;
      Rectangle in;
      boolean inside=false;
      boolean activate=false;
      
      TimeIcon() {
         if( TIMEPLOT==null ) TIMEPLOT = aladin.chaine.getString("TIMEPLOT");
      }
      
      void activate(boolean flag) { activate=flag; }

      boolean inside(int x, int y) { inside = !hasTimePlot() && activate && in!=null && in.contains(x, y); return inside; }

      void draw(Graphics g) {
         if( !activate || hasTimePlot() ) return;
         Color c = inside ? Aladin.COLOR_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND;
         g.setFont( Aladin.SSPLAIN );
         FontMetrics fm = g.getFontMetrics();
         Dimension dim = getDimension();
         int x = dim.width-fm.stringWidth(TIMEPLOT)-25;
         int y = 5;
         Tool.drawPlot(g,x,y,w,w,c,Aladin.COLOR_MEASUREMENT_BACKGROUND,true);
         Slide.drawClock(g, x+2, y+7, 4, Color.black, Color.white);
         g.drawString(TIMEPLOT, x+w/2-fm.stringWidth(TIMEPLOT)/2, y+w+fm.getHeight());
         in = new Rectangle(x,y,w,w+fm.getHeight());
      }
   }
   
   class TableIcon {
      int w = 18;
      Rectangle in;
      boolean inside=false;
      
      TableIcon() {
         if( TABLEICON==null ) TABLEICON = "table"; //aladin.chaine.getString("TABLEICON");
      }
      
      boolean inside(int x, int y) { inside = !planeAlreadyCreated && in!=null && in.contains(x, y); return inside; }

      void draw(Graphics g) {
         if( planeAlreadyCreated ) return;
         Color c = inside ? Aladin.COLOR_FOREGROUND : Aladin.COLOR_CONTROL_FOREGROUND;
         g.setColor( c );
         g.setFont( Aladin.SSPLAIN );
         FontMetrics fm = g.getFontMetrics();
         Dimension dim = getDimension();
         int x = dim.width-fm.stringWidth(TIMEPLOT)-55;
         int y = 5;
         int L=8;
         int dx = 8;
         for( int i=0; i<5; i++ ) {
            int h = y+i*2+10;
            g.drawLine(dx+x-L-i,h, dx+x-5-i,h);
            g.drawLine(dx+x-2-i,h, dx+x,h);
            g.drawLine(dx+x+3,h, dx+x+5+i,h);
            g.drawLine(dx+x+8+i,h, dx+x+L+1+i,h);
         }
         g.drawString(TABLEICON, x+w/2-fm.stringWidth(TABLEICON)/2, y+w+fm.getHeight());
         in = new Rectangle(x,y,w+5,w+fm.getHeight());
      }
   }
   
   // Trace l'icone de fermeture du graphique
   private void drawCroix(Graphics g) {
      int width=getDimension().width;
      g.setColor(Aladin.COLOR_BUTTON_BACKGROUND);
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
//      int width=getDimension().width;
//      g.setColor(Aladin.BKGD);
//      rInfo = new Rectangle(width-w-4,6+w,w+4,w+4);
//      g.fillRect(rInfo.x,rInfo.y,rInfo.width,rInfo.height);
//      g.setColor(Color.blue);
//      g.setFont(Aladin.SBOLD);
//      g.drawString("^",rInfo.x+2,rInfo.y+10);
//   }
   
   
   private boolean blinkState=false;
   
   private Timer timerBlink=null;

   // Le voyant clignotant d'attente
   private void drawBlink(Graphics g, int x, int y) {
      if( timerBlink==null ) timerBlink = new Timer(500,new ActionListener() {
         public void actionPerformed(ActionEvent e) { aladin.calque.zoom.zoomView.repaint(); }
      });
      timerBlink.start();
      Slide.drawBall(g, x, y, blinkState ? Color.white : Color.green );
      blinkState = !blinkState;
   }
   
   private void stopBlink() {
      if( timerBlink==null ) return;
      timerBlink.stop();
   }

   // Trace l'icone de demande de chargement du SED dans l'outil Web
   private void drawMore(Graphics g) {
      Dimension dim=getDimension();
      g.setColor(Aladin.COLOR_BUTTON_BACKGROUND);
      rMore = new Rectangle(dim.width-w-4,16+3*w,w+4,w+4);
      g.fillRect(rMore.x,rMore.y,rMore.width,rMore.height);
      g.setColor( flagWavelength ? Color.blue : Color.red );
      g.setFont(Aladin.SBOLD);
      g.drawString("+",rMore.x+w/2,rMore.y+3+w);
   }
   
   // Trace l'icone de demande de passage freq <-> longueur d'onde
   private void drawWave(Graphics g) {
      Dimension dim=getDimension();
      g.setColor(Aladin.COLOR_BUTTON_BACKGROUND);
      rWave = new Rectangle(dim.width-w-4,11+2*w,w+4,w+4);
      g.fillRect(rWave.x,rWave.y,rWave.width,rWave.height);
      g.setColor( flagWavelength ? Color.blue : Color.red );
      g.setFont(Aladin.SBOLD);
      g.drawString(TILDE+"",rWave.x+w/4,rWave.y+3+w);
   }
   
   // Trace l'icone du help
   private void drawHelp(Graphics g) {
      Dimension dim=getDimension();
      g.setColor(Aladin.COLOR_BUTTON_BACKGROUND);
      rHelp = new Rectangle(dim.width-w-4,6+w,w+4,w+4);
      g.fillRect(rHelp.x,rHelp.y,rHelp.width,rHelp.height);
      g.setColor( Color.blue );
      g.setFont(Aladin.SBOLD);
      g.drawString("?",rHelp.x+w/2,rHelp.y+3+w);
   }
   
   // Cr�ation d'une courbe de lumi�re (Temps vs Flux)
   private void createTimePlot() {
      if( !planeAlreadyCreated ) createStackPlane();
      Source s = sedList.get(0).o;
      int nTime = getField(s, Field.TIME);
      int nFlux = getField(s, Field.FLUX);
      aladin.createPlotCat( plan, nTime, nFlux, false);
   }
   
   /** Actions � effectuer lors du relachement de la souris */
   protected void mouseRelease(int x,int y) {
      
      if( timeIcon.inside(x, y) ) createTimePlot();
      if( tableIcon.inside(x, y) ) { if( !planeAlreadyCreated ) createStackPlane(); }
      else if( rCroix.contains(x,y) ) aladin.view.zoomview.setSED((String)null,(String)null);
      else if( rMore.contains(x,y) ) more();
      else if( rHelp.contains(x,y) ) help();
      else if( rWave.contains(x,y) ) {
         flagWavelength = !flagWavelength;
         setPosition();
         aladin.view.zoomview.repaint();
         
      // On a cliqu� sur un point, on va donc g�n�rer la table correspondante
      // et l'afficher
      } else if( siIn!=null ) {
         int bloc=1;
         if( !planeAlreadyCreated ) createStackPlane();
         else bloc=2;
         aladin.view.showSource(siIn.o, false, true);
         aladin.mesure.mcanvas.show(siIn.o, bloc);
         aladin.calque.repaintAll();
         
      // On a cliqu� en dehors de tout point, on ne fait que d�placer le rep�re
      // sur l'emplacement du sed
      }  else if( simRep!=null ) {
         aladin.view.setRepere(new Coord(simRep.raj,simRep.dej),true);
         aladin.calque.repaintAll();
      }
   }
   
   private void more() {
      if( source==null )  return;
      
      // Je dois utiliser le %20 plut�t que le '+' pour l'encodage des blancs
      // parce que l'outil VizieR photometry ne les supporte pas
      // CE N'EST PLUS LA PEINE
//      String target = URLEncoder.encode(source);
//      target = target.replaceAll("[+]","%20");
//      aladin.glu.showDocument("Widget.VizPhot", target+" "+radius);
//      aladin.glu.showDocument("Widget.VizPhot", Glu.quote(source)+" "+radius);
      aladin.glu.showDocument("VizieR.sed.home", Glu.quote(source)+" "+radius);
   }
   
   // Affiche un message explicatif sur le SED
   private void help() {
      aladin.info( aladin.chaine.getString("SEDHELP"));
   }
   
   /** Actions � effectuer lorsque la souris sort du cadre */
   protected void mouseExit() {
      currentAbs=currentFlux=Double.NaN;
//      aladin.view.simRep = null;
   }
   
   /** Actions � effectuer lorsque la souris entre dans le cadre */
//   protected void mouseEnter() {
//      aladin.view.simRep = simRep;
//      if( simRep!=null ) aladin.view.repaintAll();
//   }
   
   /** Associe le bon tooltip */
   private void toolTip(String k) {
      String s = k==null ? null : aladin.chaine.getString(k);
      Util.toolTip(aladin.view.zoomview, s, true);
   }
   
   /** Actions � effectuer lorsque la souris survole le graphique */
   protected void mouseMove(int x, int y) {
      
      
      // Tooltips ?
      if( timeIcon.inside(x,y) )     { toolTip("TIMEPLOTHELP"); return; }
      if( tableIcon.inside(x,y) )    { toolTip("TABLEICONHELP"); return; }
      if( rCroix!=null && rCroix.contains(x,y) )     { toolTip("SEDCLOSE");       return; }
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
      
      // Dois-je montrer la source associ�e ?
      if( siIn!=null ) {
         aladin.view.showSource(siIn.o, false, true);
         aladin.mesure.mcanvas.show(siIn.o, 1);
      }
      
      // Quels sont le flux et la fr�quence sous la souris
      Dimension dim = getDimension();
      if( x>margeGauche && x<dim.width-margeDroite ) {
         currentAbs = getCurrentAbs( x-margeGauche );
      } else currentAbs = Double.NaN;

      if( y>margeHaut+14 && y<dim.height-margeBas-(x>margeGauche+30?0:14) ) {
         currentFlux = getCurrentFlux( (double)(dim.height - (margeBas+margeHaut)) - (double)(y-margeHaut) ) ;
      } else currentFlux = Double.NaN;
      
      // M�morisation de la position sous la souris
      currentX=x;
      currentY=y;
   }
   
   /** Actions � effectuer sur le mouvement de la roulette de la souris */
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
   
   
   /** Affichage avec la bonne unit� : val donn�e en u
    * @return la fr�quence dans la bonne unit�, null si unit� inconnue
    */
   static final public String getUnitFreq(double val,String u) { return getUnit(val,u,UNITFREQ); }
   static final public String getUnitWave(double val,String u) { return getUnit(val,u,UNITWAVE); }
   static final public String getUnit(double val,String u,String [] UNIT) {
      int i=Util.indexInArrayOf(u, UNIT, true);
      if( i<0 ) return null;
      double fct=1.;
      for( ;i>0; i-- ) fct*=1000.;
      return getUnit(val*fct,UNIT,3);
   }
  
   /** Affichage en fr�quence : val donn�e en GHz */
   static final public String getUnitFreq(double val) {
      return getUnit(val*1000000000.,UNITFREQ);
   }

   /** Affichage en Jansky : val donn�e en GJy */
   static final public String getUnitJy(double val) {
      return getUnit(val*1000000.,UNITJY);
   }

  
   /** Affichage en longueur d'onde : val donn�e en micron-m�tre */
   static public String getUnitWave(double val) {
      return getUnit(val*1000.,UNITWAVE);
   }

   static private String getUnit(double val,String [] unit) { return getUnit(val,unit,1); }
   static private String getUnit(double val,String [] unit,int decimal) {
      int u = 0;
      while (val >= 1000 && u<unit.length-1) { u++; val /= 1000L; }
      NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
      nf.setMaximumFractionDigits(decimal);
      return nf.format(val)+unit[u];
   }



   
}
