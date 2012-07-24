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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.ImageObserver;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import javax.swing.SwingUtilities;

import cds.astro.Coo;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Gestion d'un plan image pour l'affichage du ciel de fond
 *
 * Les classes concern�es sont:
 * - PlanBG : cette classe
 * - HealpixKey : g�re un losange Healpix
 * - HealpixLoader (classe interne) : gestion asynchrone de la liste des HealpixKeys
 * - Loader (classe interne) : chargement asynchrone des donn�es
 *
 * METHODE :
 * Le fond du ciel est subdivis� dans des losanges HEALPIX accessibles directement via
 * des URLs du type http://serveur/SURVEY.NorderNNN.NpixXXX.hpx (ou .jpg). Les losanges
 * les plus appropri�s sont charg�es du r�seau ou d'un cache disque en commen�ant par les
 * plus mauvaises r�solutions. Le trac� des losanges est effectu� � chaque repaint() de chaque
 * vue d'Aladin (=> doit �tre au max de qq ms) par transform�e affine approchant les coordonn�es
 * c�lestes des 4 coins dans la solution astrom�trique de la vue. Les losanges sont gard�s en
 * m�moire tant qu'ils sont trac�s.
 *
 * Nota : Les coordonn�es des 4 coins peuvent �tre imm�diatement connues � partir de l'ordre
 * et du num�ro HEALPIX du losange.
 *
 * les losanges ont g�n�ralement 512x512 pixels utilisant eux-m�mes la subdivision HEALPIX d'ordre +9
 * par rapport � l'ordre du num�ro npix du fichier. Ces pixels sont ordonn�es en lignes
 * et colonnes "image", et non pas dans l'ordre de num�rotation HEALPIX nested (�a afficherait
 * n'importe quoi)
 *
 *
 * La liste des serveurs est fournie par des enregistrements GLU
 * => classe GluSky, liste dans Glu.vGluSky
 * Exemple :
 * %A 2MASSK.hpx
 * %D 2MASS K survey in HEALPIX access
 * %O CDS'aladin
 * %Z ALADIN
 * %U http://alinda.u-strasbg.fr/~oberto
 * %Aladin.Profile >=5.9 beta hpx
 * %Aladin.Label InfraRed (2MASS K)
 * %Aladin.Survey K
 *
 * L'algorithme de tracage fonctionne de la mani�re suivante (voir draw(...)) :
 * 1) Boucle depuis la plus mauvaise r�solution HEALPIX (ordre 2 ou 3)
 *    jusqu'� la r�solution courante de la vue
 *    1.1) D�termination de tous les losanges HEALPIX candidats (qui couvrent la vue)
 *         => voir classe HealpixKey
 *    1.2) Affichage (ou demande d'affichage => pixList) des losanges concern�es
 *         ssi leurs fils ne vont pas �tre tous affich�s
 *    1.3) R�veil du Thread de gestion des losanges (class HealpixLoader)
 *
 * L'algorithme du HealpixLoader fonctionne de la mani�re suivante :
 * 1) Sommeil
 * 2) R�veil (par timeout ou par wakeUp())
 *     2.1) Gestion du cache disque (suppression des plus vieux fichiers acc�d�s si n�cessaire)
 *     2.2) Purge des losanges en m�moire inutilis�e (temps de vie>dernier affichage forc�)
 *     2.3) Changement des �tats des losanges (ASKING=>TOBELOAD...) en vue de leur
 *          chargement par les threads de gestion du cache et de gestion du net
 *
 * 2 threads, les loaders, g�rent les chargements (cache et r�seau)
 * L'algorithme des Loader fonctionne de la mani�re suivante:
 * 1) Sommeil
 * 2) R�veil (par timeout ou wakeUp())
 *     2.1) Chargement du "meilleur" losange � �tre TOBELOAD..
 *
 * @author Pierre Fernique + Ana�s Oberto [CDS]
 */
public class PlanBG extends PlanImage {
   
   static final boolean NOALLSKY = false;
   static final boolean TEST = true;

   static final int DRAWPIXEL=0;
   static final int DRAWPOLARISATION=1;

   // pour l'affichage de la polarisation
   private float segmentLenFactor = 1f;
   private int segmentThickness = 1;
   private float segmentDensityFactor = 1f;

   private Color oc=null;
   protected int drawMode=DRAWPIXEL;
   protected void setDrawMode(int mode) {
      drawMode=mode;
      if( drawMode==DRAWPOLARISATION ) {
         if( opacityLevel<0.1 ) setOpacityLevel(1.0f);
         if( oc==null ) oc=c = Couleur.getNextDefault(aladin.calque);
         else c=oc;
      } else c=Color.black;

      if( mode==DRAWPOLARISATION ) type = ALLSKYPOL;
      else type = ALLSKYIMG;
   }


   static final String CACHE = "Background";
   static long MAXCACHE=4*1024*1024;                // taille max du cache en Ko
   static final protected int LIVETIME = 3*1000;            // temps de vie des losanges en m�moire (ms)

   protected String survey;        // Nom du background
   protected String verboseDescr;  // Baratin sur le survey
   protected String version="";    // Num�ro de version du background si existant (ex: -v1.2)
   protected String imageSourcePath; // Template d'acc�s aux prog�niteurs (ex: id=~/(.*)/http://machine/cgi?img=$1/ )
   protected String url;           // Pr�fixe de l'url permettant d'acc�der au background
   protected int minOrder=3;       // Ordre HEALPIX "fichier" min du background
   protected int maxOrder=14;       // Ordre HEALPIX "fichier" max du background
   protected Hashtable<String,HealpixKey> pixList;      // Liste des losanges disponibles
   protected HealpixKey allsky;    // Losanges sp�ciaux d'acc�s � tout le ciel niveau 3
   protected HealpixLoader loader;   // G�re le chargement des losanges
   protected boolean hasDrawnSomething=false;   // True si le dernier appel � draw() � dessiner au moins un losange
   protected boolean allWaitingKeysDrawn=false;   // true si tous les losanges de r�solution attendue ont �t� trac�s
   protected boolean useCache=true;
   protected boolean color=false;   // true si le survey est fourni en couleur
   protected boolean colorUnknown=false; // true si on ne sait pas a priori si le survey est en JPEG couleur ou non
   public boolean fitsGzipped=false; // true si le survey est fourni en true pixels (FITS) mais gzipp�
   public boolean truePixels=false;  // true si le survey est fourni en true pixels (FITS)
   protected boolean inFits=false;   // true: Les losanges originaux peuvent �tre fournis en FITS
   protected boolean inJPEG=false;   // true: Les losanges originaux peuvent �tre fournis en JPEG
   private boolean hasMoc=false;     // true si on on peut disposer du MOC correspondant au survey
   private boolean hasHpxFinder=false;     // true si on on peut disposer du HpxFinder correspondant au survey
   protected int frameOrigin=Localisation.ICRS; // Mode Healpix du survey (GAL, EQUATORIAL...)
   protected int frameDrawing=aladin.configuration.getFrameDrawing();   // Frame de trac�, 0 si utilisation du rep�re g�n�ral
   protected boolean localAllSky=false;
   
   protected PlanBGIndex planBGIndex=null;


   // Gestion du cache
//   static volatile long cacheSize=MAXCACHE-1024*2;   // Taille actuelle du cache
   static volatile long cacheSize=-1;   // Taille actuelle du cache
   static private Object cacheLock = new Object();

   // Polarisation : facteur multiplicatif pour longueur segments
   private int polaScaleFactor = 40;


   // Gestion de la m�moire
   protected long memSize=0;   // Taille actuelle de la m�moire utilis� par le fond duc iel
   
   // pour classes d�riv�es
   protected PlanBG(Aladin aladin) {
       super(aladin);
       initCache();
   }

   /**
    * Cr�ation d'un plan Healpix
    * @param aladin
    * @param gluSky
    * @param c Coordonn�e centrale ou null si non sp�cifi�e
    * @param radius Taille du champ en degr�s, ou <=0 si non sp�cifi�
    */
   protected PlanBG(Aladin aladin, TreeNodeAllsky gluSky, String label, Coord c,double radius,String startingTaskId) {
      super(aladin);
      this.startingTaskId = startingTaskId;
      initCache();

      url = gluSky.getUrl();
      survey = gluSky.label;
      version = gluSky.version;
      minOrder = gluSky.minOrder;
      maxOrder = gluSky.maxOrder;
      useCache = gluSky.useCache();
      frameOrigin=gluSky.frame;
      verboseDescr=gluSky.verboseDescr;
      co=c;
      coRadius=radius;
      if( label!=null && label.trim().length()>0 ) setLabel(label);
      from=gluSky.copyright!=null && gluSky.copyright.length()>0 ? gluSky.copyright : url;
      setSpecificParams(gluSky);
      aladin.trace(3,"AllSky creation: "+gluSky.toString1()+(c!=null ? " around "+c:""));
      suite();
   }
   
   protected void setSpecificParams(TreeNodeAllsky gluSky) {
      type = ALLSKYIMG;
      video=VIDEO_NORMAL;
      inFits = gluSky.isFits();
      inJPEG = gluSky.isJPEG();
      truePixels=gluSky.isTruePixels();
      color = gluSky.isColored();

      // Information suppl�mentaire par le fichier properties ?
      boolean local=!(url.startsWith("http:") || url.startsWith("https:") ||url.startsWith("ftp:"));
      java.util.Properties prop = new java.util.Properties();
      try {
         InputStream in=null;
         if( !local ) in = (new URL(url+"/"+PlanHealpix.PROPERTIES)).openStream();
         else in = new FileInputStream(new File(url+Util.FS+PlanHealpix.PROPERTIES));
         if( in==null ) throw new Exception();
         prop.load(in);
         in.close();

         Aladin.trace(4,"PlanBG.setSpecificParams() found a \"properties\" file");
         // Frame
         String strFrame = prop.getProperty(PlanHealpix.KEY_COORDSYS);
         char c1 = strFrame.charAt(0);
         int frame=-1;
         if( c1=='C' ) frame=Localisation.ICRS;
         else if( c1=='E' ) frame=Localisation.ECLIPTIC;
         else if( c1=='G' ) frame=Localisation.GAL;
         if( frame!=-1 && frame!=frameOrigin ) {
            aladin.trace(1,"Coordinate frame found in properties file ("+Localisation.getFrameName(frame)
                  +") differs from the GLU record ("+Localisation.getFrameName(frameOrigin)+") => assume "+Localisation.getFrameName(frame));
            frameOrigin=frame;
         }

         // ImageSourcePath
         imageSourcePath = prop.getProperty(PlanHealpix.KEY_IMAGESOURCEPATH);
         if( imageSourcePath!=null ) Aladin.trace(4,"PlanBG.setSpecificParams() found a progenitor access rule => "+imageSourcePath);
         
      } catch( Exception e ) { aladin.trace(3,"No properties file found ..."); }

   }
   
   protected PlanBG(Aladin aladin, String path, String label, Coord c, double radius,String startingTaskId) {
      super(aladin);
      this.startingTaskId=startingTaskId;
      initCache();
      aladin.trace(2,"Creating allSky directory plane ["+path+"]"); 
      type = ALLSKYIMG;
      video=VIDEO_NORMAL;
      File f = new File(path);
      url = f.getAbsolutePath();
      survey = f.getName();
      maxOrder=3;
      useCache = false;
      this.label=label;
      paramByTreeNode(new TreeNodeAllsky(aladin, url), c, radius);
      aladin.trace(3,"AllSky local... frame="+Localisation.getFrameName(frameOrigin)+" "+this+(c!=null ? " around "+c:""));
      suite();
   }
   
   protected PlanBG(Aladin aladin, URL u, String label, Coord c, double radius, String startingTaskId) {
      super(aladin);
      this.startingTaskId=startingTaskId;
      initCache();
      aladin.trace(2,"Creating allSky http plane ["+u+"]"); 
      type = ALLSKYIMG;
      video=VIDEO_NORMAL;
      url = u.toString();
      
      maxOrder = 3;
      useCache = true;
      localAllSky = false;
      co=c;
      coRadius=radius;
      paramByTreeNode(new TreeNodeAllsky(aladin, url),c,radius);
      int n = url.length();
      if( url.endsWith("/") ) n--;
      survey = this.label!=null && this.label.length()>0 ? this.label : url.substring(url.lastIndexOf('/',n-1)+1,n);
      aladin.trace(3,"AllSky http... "+this+(c!=null ? " around "+c:""));
      suite();
   }
   
   private void paramByTreeNode(TreeNodeAllsky gSky, Coord c, double radius) {
      if( label!=null && label.trim().length()>0 ) setLabel(label);
      else setLabel(gSky.label);
      maxOrder=gSky.getMaxOrder();
      inFits=gSky.isFits();
      inJPEG=gSky.isJPEG();
      color=gSky.isColored();
      frameOrigin=gSky.getFrame();
      losangeOrder=gSky.getLosangeOrder();
      localAllSky=gSky.isLocal();
      imageSourcePath = gSky.getImageSourcePath();
      version = gSky.getVersion();
      truePixels=inFits && localAllSky || !inJPEG && !localAllSky;
      useCache=!localAllSky && gSky.useCache();
      co=c!=null ? c : gSky.getTarget();
      coRadius= c!=null ? radius : gSky.getRadius();
   }
   
   private boolean testMoc=false; // true : la pr�sence d'un MOC a �t� test�
   
   /** Retourne true si le survey dispose d'un Moc associ�. Il doit �tre sur la racine
    * et avoir le nom Moc.fits */
   protected boolean hasMoc() {
      if( hasMoc || testMoc ) return hasMoc;
      String moc = url+"/Moc.fits";
      hasMoc = localAllSky ? (new File(moc)).exists() : Util.isUrlResponding(moc);
      testMoc=true;
      return hasMoc;
   }
   
   private boolean testHpxFinder=false; // true : la pr�sence d'un HpxFinder a �t� test�
   
   protected boolean hasHpxFinder() {
      if( hasHpxFinder || testHpxFinder ) return hasHpxFinder;
      String f = url+"/HpxFinder";
      hasHpxFinder = localAllSky ? (new File(f)).exists() : Util.isUrlResponding(f);
      testHpxFinder=true;
      return hasHpxFinder;
   }
   
   protected void frameProgenResume(Graphics g,ViewSimple v) {
      if( planBGIndex==null || aladin.frameProgen==null ) return;
      planBGIndex.updateHealpixIndex(v);
      HealpixIndex hi = ((PlanBGIndex)planBGIndex).getHealpixIndex();
      aladin.frameProgen.resume(hi,this);
      aladin.frameProgen.progen.draw(g,v);
   }
   
   /** Chargement du Moc associ� au survey */
   protected void loadMoc() {
      String moc = url+"/Moc.fits";
      aladin.execAsyncCommand("load "+moc);
   }
   
   /** Retourne le frame d'affichage, 0 si utilisation du frame g�n�ral */
   protected int getFrameDrawing() { return frameDrawing; }
   
   /** Positionne le frame d'affichage. 0 pour prendre le frame g�n�ral */
   protected void setFrameDrawing(int frame) {
      frameDrawing=frame;
//      System.out.println("PlanBG.setFrameDrawing("+Localisation.FRAME[frame]+")..");
      if( projd.frame!=getCurrentFrameDrawing() ) {
//         System.out.println("PlanBG.setFrameDrawing: => new proj = "+Localisation.REPERE[getFrame()]);
         ViewSimple v = aladin.view.getView(this);
         Coord c = new Coord(aladin.view.repere.raj,aladin.view.repere.dej);
         projd.frame = getCurrentFrameDrawing();
         aladin.view.newView(1);
         v.goToAllSky(c);
         aladin.view.repaintAll();
      }
   }
   
   /** Retourne le frame courant en fonction du s�lecteur du frame d'affichage */
   protected int getCurrentFrameDrawing() {
      if( frameDrawing==0 ) return aladin.localisation.getFrame();
      return Util.indexInArrayOf(Localisation.FRAME[frameDrawing], Localisation.REPERE);
   }
   
   /** retourne le systeme de coordonn�e natif de la boule Healpix */
   public int getFrameOrigin() { return frameOrigin; }
   
   static protected boolean isPlanBG(String path) {
      String s = path+Util.FS+"Norder3";
      File f = new File(s);
      return f.isDirectory();
   }
   
   protected void suite() {

      if( this.label==null || this.label.trim().length()==0) setLabel(survey);
      if( co==null ) {
         co = new Coord(0,0);
         co=Localisation.frameToFrame(co,aladin.localisation.getFrame(),Localisation.ICRS );
         coRadius=180;
      }
      if( coRadius<=0 ) coRadius=180;
      
      objet = co+"";
      Projection p = new Projection("allsky",Projection.WCS,co.al,co.del,60*4,60*4,250,250,500,500,0,false,Calib.SIN,Calib.FK5);
      p.frame = getCurrentFrameDrawing();
      if( Aladin.OUTREACH ) p.frame = Localisation.GAL;
      setNewProjD(p);
      setDefaultZoom(co,coRadius);
      suiteSpecific();
      threading();
      log();
   }
   
   protected void suiteSpecific() {
      dataMin=pixelMin=0;
      dataMax=pixelMax=255;
      active=selected=true;
      isOldPlan=false;

      pixList = new Hashtable<String,HealpixKey>(1000);
      allsky=null;
      if( error==null ) loader = new HealpixLoader();
      if( Aladin.PROTO ) planBGIndex = new PlanBGIndex(aladin,this);

      aladin.endMsg();
      creatDefaultCM();
      resetStats();
   }
   
   /** Positionne la taille initiale du champ. */
   protected void setDefaultZoom(Coord c,double radius) {
      initZoom=-1;
      if( radius>0 && c!=null ) {
         double projPixelRes = (projd.rm/60) / projd.r;
         double width = aladin.view.getCurrentView().getWidth();
         double taille = width * projPixelRes;
         double z = taille/radius;
         initZoom = aladin.calque.zoom.getNearestZoomFct(z);
      }
      if( initZoom==-1 ) initZoom = c==null ? 1./(Aladin.OUTREACH?64:32) : 16;
      aladin.trace(4,"PlanBG.setDefaultZoom("+c+","+Coord.getUnit(radius)+") => zoom = "+initZoom+" ie. "+aladin.calque.zoom.getItem(initZoom));
   }
   
   protected void log() {
      aladin.log(Plan.Tp[type],label);
   }

   protected void initCache() {
      if( cacheSize==-1 ) {
         cacheSize=aladin.configuration.getHpxCacheSize();
         Aladin.trace(3,"Cache Size => "+cacheSize);
      }

      if( Aladin.MAXMEM<50 ) {
         aladin.warning("Full sky Aladin mode requires atleast 64MB of RAM\n" +
         		"We strongly suggest to adjust the JAVA memory parameter and relaunch Aladin.\n" +
         		"See the corresponding Aladin FAQ entry available via the Help menu");
      }
   }

   @Override
   public String getUrl() {
      return url;
   }

   @Override
   protected void planReady(boolean ready) {
      super.planReady(ready);
      setPourcent(0);
      aladin.view.setRepere(co);
      flagOk=ready;
      aladin.synchroPlan.stop(startingTaskId);
   }

   @Override
   protected boolean waitForPlan() {
      return error==null;
   }

   /** Creation d'une table de couleurs par defaut */
   protected void creatDefaultCM() {
      transfertFct = aladin.configuration.getCMFct();
      typeCM       = aladin.configuration.getCMMap();
      cm = ColorMap.getCM(0,128,255,false, typeCM,transfertFct);
   }

   /** Modifie la table des couleurs */
   @Override
   protected void setCM(Object cm) {
      this.cm = (ColorModel)cm;
      changeImgID();
   }

   /** Lib�ration du plan Background */
   @Override
   protected boolean Free() {
      String stat = getShortStats();
      if( stat!=null ) aladin.log("HealpixStats",stat);
      
      hpx2xy = xy2hpx = null;
//      lastGeneratedFile = null;
      frameOrigin=Localisation.ICRS;
      FreePixList();
      return super.Free();
   }

   /** Lib�ration de la pixList du plan */
   protected void FreePixList() {
      try {
         if( pixList!=null ) {
            Enumeration<HealpixKey> e = pixList.elements();
            while( e.hasMoreElements() ) {
               HealpixKey healpix = e.nextElement();
               if( healpix!=null ) {
                  if( useCache && healpix.shouldBeCached() ) healpix.write();
                  healpix.free();
               }
            }
            pixList.clear();
         }
      } catch( Exception e ) { }
      if( allsky!=null ) { allsky.free(); allsky=null; }
   }

   @Override
   protected void setPropertie(String prop,String specif,String value) throws Exception {
       if( prop.equalsIgnoreCase("Projection") ) {
           Projection p = this.projd;
           this.modifyProj(null,Projection.SIMPLE,p.alphai,p.deltai,p.rm1,
                        p.cx,p.cy,p.r1,p.rot,p.sym,Projection.getProjType(value),p.system);
           aladin.view.newView(1);
           aladin.calque.repaintAll();
       } else {
           super.setPropertie(prop,specif,value);
       }
   }

   protected int getPolaScaleFactor() {
       return polaScaleFactor;
   }
   protected void setPolaScaleFactor(int polaScaleFactor) {
       this.polaScaleFactor = polaScaleFactor;
   }

   /** Lib�ration du maximum de m�moire possible */
   protected void clearBuf() {
      Enumeration<HealpixKey> e = pixList.elements();
      while( e.hasMoreElements() ) {
         HealpixKey healpix = e.nextElement();
         if( healpix!=null ) healpix.clearBuf();
      }
      if( allsky!=null ) allsky.clearBuf();
      gc();
   }

   // Positionn� � true afin d'�viter de refaire des gc() inutiles
   private boolean flagClearBuf=false;

   /** Suppression des images en m�moire cons�cutif � l'arr�t de visualisation du plan
    * Attention : le nom de la m�thode est ambigue
    */
   @Override
   protected boolean pixelsOriginIntoCache() {
      if( flagClearBuf ) return true;
      clearBuf();
      changeImgID();
      flagClearBuf=true;
      return true;
   }

  /** Retourne true si on dispose (ou peut disposer) des pixels originaux */
   @Override
   protected boolean hasOriginalPixels() { return truePixels; }

   @Override
   protected boolean recut(double min,double max,boolean autocut) {
//      if( !hasOriginalPixels() ) return false;
      FreePixList();
      changeImgID();
      double tmpMinPix=dataMin;
      double tmpMaxPix=dataMax;
      findMinMax( pixelsOrigin ,bitpix,width,height,min,max,autocut,0);
      dataMin=tmpMinPix;
      dataMax=tmpMaxPix;
      flagRecut=true;
      freeHist();
      return true;
   }

   protected boolean flagRecut=false;


   /** Postionne la taille approximative de tous les losanges en m�moire */
   protected void setMem() { return; }
//   protected void setMem() {
//      long mem=0L;
//      Enumeration<HealpixKey> e = pixList.elements();
//      while( e.hasMoreElements() ) {
//         HealpixKey healpix = e.nextElement();
//         if( healpix!=null )  mem+=healpix.getMem();
//      }
//      if( allsky!=null ) {
//         HealpixKey [] allSkyPixList = allsky.getPixList();
//         if( allSkyPixList!=null ) {
//            for( int j=0; j<allSkyPixList.length; j++ ) mem+=allSkyPixList[j].getMem();
//         }
//
//      }
//      memSize=mem;
//   }

   @Override
   protected boolean setActivated(boolean flag) {
      if( flag && aladin.calque.hasHpxGrid() ) System.out.println(getStats());
      return super.setActivated(flag);
   }

   // Pour �viter les tests � chaque fois
   private String dirCache=null;
   private boolean flagCache=false;

   /** Retourne le r�pertoire du Cache ou null si impossible � cr�er */
   protected String getCacheDir() {
      if( flagCache ) return dirCache;
      flagCache=true;

      // Cr�ation de $HOME/.aladin/Cache si ce n'est d�j� fait
      if( !aladin.createCache() ) return null;

      // Cr�ation de $HOME/.aladin/Cache si ce n'est d�j� fait
      String dir = System.getProperty("user.home")+Util.FS+Aladin.CACHE+Util.FS+Cache.CACHE;
      File f = new File(dir);
      if( !f.isDirectory() && !f.mkdir() ) return null;

      // Cr�ation de $HOME/.aladin/Cache/Background si ce n'est d�j� fait
      dir = dir+Util.FS+CACHE;
      f = new File(dir);
      if( !f.isDirectory() && !f.mkdir() ) return null;

      dirCache=dir;
      return dir;
   }

   static protected String getCacheDirPath() {
        return System.getProperty("user.home") + Util.FS + Aladin.CACHE
                + Util.FS + Cache.CACHE + Util.FS + CACHE;
    }
   
   /** Positionne le niveau max du cache en Ko (minimum 500 Mo) */
   static protected void setMaxCacheSize(long maxCacheSize) {
      if( maxCacheSize<512*1024 ) maxCacheSize=512*1024;
      
      MAXCACHE = maxCacheSize;
      Aladin.trace(4,"PlanBG.setMaxCacheSize() => "+MAXCACHE/1024+"MB");
   }

   protected int nbReady=0;

   /** Taille de la m�moire cache pour le fond du ciel (en Ko) */
   static protected long getCacheSize() {
      synchronized( cacheLock ) { return cacheSize; }
   }

   /** Augmentation de la taille du cache - size en Ko*/
   static void addInCache(long size) {
      synchronized( cacheLock ) { cacheSize+=size; }
   }

   /** Positionnement de la taille du cache - size en Ko*/
   static void setCacheSize(long size) {
      synchronized( cacheLock ) { cacheSize=size; }
   }

   static private Thread scanCache = null;

   /** Scan du cache et suppression des vieux fichiers */
   static synchronized void scanCache() {
      if( (cacheSize!=-1 && cacheSize<MAXCACHE) || scanCache!=null) return;

      (scanCache=new Thread("Scan cache") {
         @Override
         public void run() {
            long size=0;
            long t = System.currentTimeMillis();
            String dir = PlanBG.getCacheDirPath();
            if( dir==null ) {
               size=0;
               setCacheSize(0);
               return;
            }

            // Parcours du cache
            Vector<File> listCache = new Vector<File>(2000);
            size  = getCacheSize(new File(dir),listCache);
            size += getCacheSizePlanHealpix(new File(PlanHealpix.getCacheDirPath()), listCache);
            Collections.sort(listCache,(new Comparator() {
               public int compare(Object o1, Object o2) {
                  return (int)( ((File)o1).lastModified()-((File)o2).lastModified() );
               }
            }));


            // Suppression des vieux fichiers si n�cessaires
            Enumeration<File> e = listCache.elements();
            while( e.hasMoreElements() && size > (3*MAXCACHE)/4 ) {
               File f = e.nextElement();
               if( size > (3*MAXCACHE)/4 ) {

                  Aladin.trace(4,f+" ("+f.lastModified()+") deleted");
                  if( f.isFile() ) {
                      size-=f.length()/1024;
                      f.delete();
                  }
                  // cache HPX des fichiers locaux
                  else if( f.isDirectory()) {
                      long dirSize = Util.dirSize(f)/1024;
                      size-=dirSize;
                      // TODO : v�rifier qu'on n'efface pas des donn�es d'un PlanHealpix dans la pile
                      Util.deleteDir(f);
//                      System.out.println("je vire le repertoire "+f.getAbsolutePath()+" de taille "+dirSize);
                  }
               }
            }
            Aladin.trace(3," => Cache size="+(size/1024)+"MB maxCache="+(MAXCACHE/1024)+"MB scan in "+(System.currentTimeMillis()-t)+"ms");
            setCacheSize(size);
            scanCache=null;
         }
      }).start();
   }
   
   /** Nettoyage complet du cache Healpix */
   static void clearCache() {
      String dir = PlanBG.getCacheDirPath();
      if( dir!=null ) Util.deleteDir(new File(dir));
      dir = PlanHealpix.getCacheDirPath();
      if( dir!=null ) Util.deleteDir(new File(dir));
      setCacheSize(0);
   }

   static private long  NBFILE=0;

   /** M�thode r�cursive pour le scan du cache */
   static public long getCacheSize(File dir,Vector<File> listCache) {
      long size=0;
      File f[] = dir.listFiles();
      for( int i=0; f!=null && i<f.length; i++ ) {
         NBFILE++;
         if( NBFILE%100==0 ) Util.pause(50);    // On souffle un peu
         if( f[i].isDirectory() ) {
            long n = getCacheSize(f[i],listCache);
            if( n==0 ) f[i].delete();       // r�pertoire vide
            else size +=n;
         }
         else {
            size+=f[i].length()/1024;
            if( listCache!=null ) listCache.addElement(f[i]);
         }
      }
      return size;
   }

   /**
    * ne retourne que les repertoires qui nous interessent
    * @param dir
    * @param listCache
    * @return
    */
   static private long getCacheSizePlanHealpix(File dir, Vector<File> listCache) {
       File f[] = dir.listFiles();
       long size = 0;
       for (int i=0; f!=null && i<f.length; i++) {
           if ( ! f[i].isDirectory()) {
               continue;
           }
           size  += Util.dirSize(f[i]);
           listCache.addElement(f[i]);
           Util.pause(100);
       }

       return size/1024;
   }

   /** Construction de la cl� de Hashtable pour ce losange */
   protected String key(HealpixKey h) { return key(h.order,h.npix); }

   /** Construction d'une cl� pour les Hasptable */
   protected String key(int order, long npix) { return order+"."+npix; }

   /** Demande de chargement du losange rep�r� par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      HealpixKey pixAsk;

      readyAfterDraw=false;

      if( drawMode==DRAWPOLARISATION ) pixAsk = new HealpixKeyPol(this,order,npix);
      else pixAsk = new HealpixKey(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }

   // D�compte du nombre de losanges ayant lib�r�s de la m�moire afin de faire
   // un gc() de temps en temps
   protected int nbFlush=0;
   private long lastGc=0L;
   private long lastMemP=0L,lastMemI=0L;
   static private int MEMREQUIREDFORGC=40*1024*1024;
   private boolean memCpt=true;

   protected void gc() {
      if( System.currentTimeMillis()-lastGc<1000 ) return;

      // Si on a de la m�moire en rab, on s'assure qu'on va lib�rer au-moins
      // 40Mo pour lancer un gc(). Comme le gc prend du temps on ne peut pas mesurer
      // imm�diatement son r�sultat, on le fera au coup suivant en alternant lastMemP et lastMemI
      if( aladin.enoughMemory() ) {
         if( memCpt ) lastMemP=Runtime.getRuntime().freeMemory();
         else lastMemI=Runtime.getRuntime().freeMemory();
         if( Math.abs(lastMemP-lastMemI)<MEMREQUIREDFORGC ) {
//            System.out.println("GC inutile lastMemP="+lastMemP/(1024*1024)+"Mo lastMemI="+lastMemI/(1024*1024)+"Mo");
            return;
         }
//         System.out.println("GC derni�re lib�ration "+Math.abs(lastMemP-lastMemI)/(1024*1024)+"Mo");
      }

      (new Thread("gc"){
         @Override
         public void run() {
            memCpt=!memCpt;
            nbFlush=0;
            System.runFinalization();
            System.gc();
            lastGc=System.currentTimeMillis();
            aladin.setMemory();
            //      System.out.println("GC done");
//            if( Aladin.BETA ) System.out.println(HealpixKey.getStats()
//                  +"."+nbReady+" hpx in mem ("+memSize/(1024*1024)+"Mb) - "+getCacheSize()/1024+"Mb in cache");
         }
      }).start();
   }

   /** Suppression d'un losange */
   protected void purge(HealpixKey healpix) {
      nbFlush+=healpix.free();
      if( nbFlush>20  ) gc();
      pixList.remove( key(healpix) );
   }

   /** Lance un wakeUp sur le loader si n�cessaire */
   protected void tryWakeUp() {
      loader.wakeUp();
   }
   
   // Juste pour un message 
   private boolean firstSubtil=true;
   
   /** Acc�s ou chargement si n�cessaire d'un HealpixKey
    * @param mode HealpixKey.SYNC,SYNCONLYIFLOCAL,HealpixKey.ASYNC
    * @return null si le HealpixKey n'est pas READY
    */
   private HealpixKey getHealpixLowLevel(int order,long npix,int mode) {
      HealpixKey h = pixList.get( key(order,npix) );
      if( h==null) {
         h = new HealpixKey(this,order,npix,mode);
         pixList.put( key(order,npix), h);
      }
      if( h.getStatus()!=HealpixKey.READY ) return null;
      return h;
   }

   /** Retourne le losange Healpix s'il est charg�, sinon retourne null
    * et si flagLoad=true, demande en plus son chargement si n�cessaire */
   protected HealpixKey getHealpix(int order,long npix,boolean flagLoad) {
      
//      HealpixKey healpix = getHealpixFromAllSky(order,npix);
//      if( healpix!=null ) return healpix;
      
      HealpixKey healpix =  pixList.get( key(order,npix) );
      if( healpix!=null ) return healpix;
      
      // Peut �tre peut-on se servir du allsky.fits|.jpeg ?
      if( healpix==null ) {
         healpix = getHealpixFromAllSky(order,npix);
         if( healpix!=null ) return healpix;
      }
      
      if( flagLoad ) return askForHealpix(order,npix);
      return null;
   }
   
   // Si la map n'est pas profonde, les losanges Allsky feront l'affaire */
   protected HealpixKey getHealpixFromAllSky(int order,long npix) {
      if( order!=3 || allsky==null ) return null;
      
      int orderLosange= getLosangeOrder();
      if( orderLosange>0 && orderLosange <= getAllSkyOrder() ) {
         HealpixKey healpix = (allsky.getPixList())[ (int)npix ];

         if( healpix!=null ) {
            if( firstSubtil ) {
               aladin.trace(4,"PlanBG.getHealpix "+label+" will use Allsky for order 3 diamonds!");
               firstSubtil=false;
            }
            return healpix;
         }
      }
      return null;
   }

//   protected HealpixKey getHealpix(int order,long npix,boolean flagLoad) {
//      HealpixKey healpix =  (HealpixKey) pixList.get( key(order,npix) );
//      if( healpix!=null ) return healpix;
//      if( flagLoad ) return askForHealpix(order,npix);
//      return null;
//   }

   static long [] getNpixList(int order, Coord center, double radius) throws Exception {
      return CDSHealpix.query_disc(CDSHealpix.pow2(order),center.al, center.del, Math.toRadians(radius));
   }

   static protected String CURRENTMODE="";
   static protected boolean DEBUGMODE=false;

   /** Retourne le centre de la vue. */
   protected Coord getCooCentre(ViewSimple v) {
      Coord center = v.getCooCentre();
      if( center==null ) return null;
      center = Localisation.frameToFrame(center,Localisation.ICRS,frameOrigin);
      return center;
   }

   protected long [] getPixListView(ViewSimple v, int order) {
      return getPixList(v,null,order);
//      Projection proj = v.getProj();
//      ArrayList<double[]> vlist = new ArrayList<double[]>(4);
//      Coord coo = new Coord();
//      for( int i=0; i<4; i++ ) {
//         PointD p = v.getPosition((double)(i==0 || i==3 ? 0 : v.getWidth()),
//                                  (double)(i==0 || i==1 ? 0 : v.getHeight()) );
//         coo.x = p.x; coo.y = p.y;
//         proj.getCoord(coo);
//         if( Double.isNaN(coo.al) ) return new long[0];
//         coo = Localisation.frameToFrame(coo,Localisation.ICRS,frameOrigin);
//         if( coo.del < -90) coo.del = -180-coo.del;
//         if( coo.al < 0   ) coo.al  = 360+coo.al;
//         if( coo.del > 90 ) coo.del = 180-coo.del;
//         if( coo.al > 360 ) coo.al  = coo.al-360;
//
//         vlist.add( new double[]{coo.al,coo.del} );
//      }
//      long[] b=null;
//      try {
//         long nside = CDSHealpix.pow2(order);
//         b = CDSHealpix.query_polygon(nside,vlist);
//      } catch( Exception e ) {
//         e.printStackTrace();
//         b=new long[0];
//      }
//      return b;
   }

   /** Retourne la liste des losanges susceptibles de recouvrir la vue pour un order donn� */
   protected long [] getPixList(ViewSimple v, Coord center, int order) {

      long nside = CDSHealpix.pow2(order);
      double r1 = CDSHealpix.pixRes(nside)/3600;
      double r2 = Math.max(v.getTailleRA(),v.getTailleDE());
      double radius = Math.max(r1,r2);

      try {
         if( center==null ) center = getCooCentre(v);
         return getNpixList(order,center,radius);
      } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); return new long[]{}; }
   }

   private double RES[]=null;
   
   private static final int DEFAULTLOSANGEORDER = 9;

   private int losangeOrder=-1;
   
   /** Retourne le Nordre des losanges (images) */
   protected int getLosangeOrder() {
      if( losangeOrder==-1 ) return DEFAULTLOSANGEORDER;
      return losangeOrder;
   }
   
   // Positionne l'ordre des losanges (trouv� lors de la lecture du premier losange
   protected void setLosangeOrder(int losangeOrder) { 
      if( this.losangeOrder!=-1 || losangeOrder<=0 ) return;
      this.losangeOrder=losangeOrder;
   }
   
   private int allSkyOrder=-1;
   
   /** Retourne le Nordre des losanges de la liste allsky, -1 si inconnu */
   protected int getAllSkyOrder() {
      if( allSkyOrder==-1 ) {
         if( allsky!=null ) {
            HealpixKey healpix[] = allsky.getPixList();
            if( healpix!=null ) {
               allSkyOrder=healpix[0].getLosangeOrder();
            }
         }
      }
      return allSkyOrder;
   }
   
   /** Retourne true s'il est possible d'acc�der � la valeur
    * du pixel Origin courant par un acc�s disque direct */
   protected boolean pixelsOriginFromDisk() {
      return flagOk && !color && truePixels /* && !lockGetPixelInfo*/ ;
   }
   
//   protected boolean lockGetPixelInfo=false;

   
   /** Retourne l'order de l'affichage actuel */
   protected int getOrder() { 
      return Math.max(3,Math.min(maxOrder(),maxOrder));
   }
   
   protected int maxOrder() {
      ViewSimple v = aladin.view.getCurrentView();
      return maxOrder(v);
   }

   public String getSurveyDir() {
      if( localAllSky ) return url;
      else  return getCacheDir() + Util.FS + survey+version;
   }
   
   /** Retourne sous forme d'une chaine �ditable la valeur du pixel  */
   protected String getPixelInfo(double x,double y,int mode) {
      if( !pixelsOriginFromDisk() || mode!=View.REAL ) return "";
      double pixel = getPixelInDouble(x,y);
      if( Double.isNaN(pixel) ) return "";
      return Y( pixel );
    }
   
   /** Retourne la valeur 8 bits du pixel indiqu� en coordonn�es image*/
   public int getPixel8(int x,int y) {
      double pix = getPixelInDouble(x,y);
      return (int)( (pix - pixelMin)*256/(pixelMax-pixelMin) );
   }
   
   /** Retourne la valeur du pixel en x,y sous la forme d'un double, ou NaN si impossible */
   protected double getPixelOriginInDouble(int x,int y) {
      return getPixelInDouble(x,y);
   }

   /** Retourne la valeur du pixel en double  */
   protected double getPixelInDouble(int x,int y) { return getPixelInDouble((double)x,(double)y); }
   protected double getPixelInDouble(double x,double y) {
      if( !pixelsOriginFromDisk() ) return Double.NaN;
      int bitpix = getBitpix();
      int mynpix = Math.abs(bitpix)/8;
      
      byte onePixelOrigin[] = new byte[mynpix];
      if( !getOnePixelFromCache(onePixelOrigin,x,y) ) return Double.NaN;
      return getPixVal(onePixelOrigin,bitpix,0)*bScale+bZero;
   }

   /** Relecture dans le cache d'un unique pixel d'origine.
    * Si besoin, ouvre le flux sur le fichier cache, mais ne le referme pas (voir Free() )
    * @param projd pour pouvoir faire une extraction de pixel m�me si la projection courante est modifi� (crop) - PF - mars 2010
    * @param pixels le tableau qui va accueillir le pixel lu (doit avoir �t� taill� assez grand)
    * @param x,y coord du pixel � extraire  (attention, les lignes sont � l'envers !!!)
    * @return true si le pixel est disponible, sinon false
    */
   protected boolean getOnePixelFromCache(byte [] pixels, double x,double y) {
      return getOnePixelFromCache(projd,pixels,x,y);
   }

   protected boolean getOnePixelFromCache(Projection projd,byte [] pixels, double x,double y) {
      double val = getOnePixelFromCache(projd,x,y);
      if( Double.isNaN(val) ) return false;
      setPixVal(pixels, getBitpix() , 0, val);
      return true;
   }
   
   protected double getOnePixelFromCache(Projection projd, double x,double y) { return getOnePixelFromCache(projd,x,y,-1,HealpixKey.ONLYIFDISKAVAIL); }
   
   /**
    * @param order l'ordre Healpix pour lequel le pixel sera r�cup�r�, -1 si ordre courant de l'affichage
    * @param mode HealpixKey.NOW - les donn�es seront charg�es imm�diatement o� qu'elles soient, sinon asynchrone
    *             HealpixKey.ONLYIFDISKAVAIL - les donn�es seront charg�es imm�diatement si elles sont pr�sentes sur le disque locale, sinon asynchrone
    */
   protected double getOnePixelFromCache(Projection projd, double x,double y,int order,int mode) {
      double pixel = Double.NaN;
      if( order<=0 ) order = getOrder();   // L'ordre n'est pas mentionn�, on prend l'ordre de l'affichage courant
      int nSideFile = (int)CDSHealpix.pow2(order);

      try {
         Coord coo = new Coord();
         coo.x = x; coo.y = y;
         projd.getCoord(coo);
         coo = Localisation.frameToFrame(coo,Localisation.ICRS,frameOrigin);
         if( Double.isNaN(coo.al) || Double.isNaN(coo.del) ) return Double.NaN;
         double[] polar = CDSHealpix.radecToPolar(new double[] {coo.al, coo.del});
         long npixFile = CDSHealpix.ang2pix_nest( nSideFile, polar[0], polar[1]);
         
         pixel = getHealpixPixel(order,npixFile,polar[0], polar[1],mode);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }

      return pixel;
   }
   
   /**
    * Retourne la valeur du pixel Healpix qui se trouve en theta,phi pour un losange HealpixKey orderFile/npixFile
    * @param HealpixKey.NOW - les donn�es seront charg�es imm�diatement o� qu'elles soient, sinon asynchrone
    *             HealpixKey.ONLYIFDISKAVAIL - les donn�es seront charg�es imm�diatement si elles sont pr�sentes sur le disque locale, sinon asynchrone
    */
   protected double getHealpixPixel(int orderFile,long npixFile,double theta, double phi,int mode) {
      HealpixKey h = getHealpixLowLevel(orderFile,npixFile,mode==HealpixKey.NOW ? HealpixKey.SYNC : HealpixKey.SYNCONLYIFLOCAL);
      if( h==null ) return Double.NaN;
      long nside = (long)h.width * CDSHealpix.pow2(h.order);
      try {
         long healpixIdxPixel = CDSHealpix.ang2pix_nest(nside, theta, phi);
         return h.getPixelValue(healpixIdxPixel,mode);
      } catch( Exception e ) {
         return Double.NaN;
      } 
   }
      
   /**
    * Retourne la valeur du pixel Healpix qui se trouve en healpixIdxPixel pour un losange HealpixKey orderFile/npixFile
    * @param HealpixKey.NOW - les donn�es seront charg�es imm�diatement o� qu'elles soient, sinon asynchrone
    *             HealpixKey.ONLYIFDISKAVAIL - les donn�es seront charg�es imm�diatement si elles sont pr�sentes sur le disque locale, sinon asynchrone
    */
   protected double getHealpixPixel(int orderFile,long npixFile,long healpixIdxPixel,int mode) {
      HealpixKey h = getHealpixLowLevel(orderFile,npixFile,mode==HealpixKey.NOW ? HealpixKey.SYNC : HealpixKey.SYNCONLYIFLOCAL);
      if( h==null ) return Double.NaN;
      return h.getPixelValue(healpixIdxPixel,mode); 
   }
   
   /**
    * R�cup�ration d'une valeur de pixel HEALPix pour une coordonn�e particuli�re
    * par approximation lin�aire avec le pixel le plus proche
    * ainsi que les 4 voisins N-S-E-W.
    * @param order l'ordre Healpix pour lequel le pixel sera r�cup�r�, -1 si ordre courant de l'affichage
    */
   protected double getHealpixClosestPixel(double ra,double dec,int order) {
      double pixel = Double.NaN;
      int nSideFile = (int)CDSHealpix.pow2(order);

      try {
         double[] polar = CDSHealpix.radecToPolar(new double[] {ra, dec});
         long npixFile = CDSHealpix.ang2pix_nest( nSideFile, polar[0], polar[1]);
         
         HealpixKey h = getHealpixLowLevel(order,npixFile,HealpixKey.SYNC);
         if( h==null ) return Double.NaN;

         long nside = (long)h.width * CDSHealpix.pow2(h.order);
         long npixPixel = CDSHealpix.ang2pix_nest(nside, polar[0], polar[1]);

         HealpixKey h1 = getHealpixLowLevel(order,npixFile,HealpixKey.SYNC);
         if( h1==null ) return Double.NaN;
         pixel = h1.getPixelValue(npixPixel,HealpixKey.NOW);
         
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }

      return pixel;
   }

   /**
    * R�cup�ration d'une valeur de pixel HEALPix pour une coordonn�e particuli�re
    * par approximation lin�aire avec le pixel le plus proche
    * ainsi que les 4 voisins N-S-E-W.
    * @param order l'ordre Healpix pour lequel le pixel sera r�cup�r�, -1 si ordre courant de l'affichage
    */
   protected double getHealpixLinearPixel(double ra,double dec,double ra1,double dec1, int order) {
      double pixel = Double.NaN;
      int nSideFile = (int)CDSHealpix.pow2(order);

      try {
//         if( first1 ) {
//            aladin.execAsyncCommand("draw mode radec;draw newtool");
//         }
         double[] polar = CDSHealpix.radecToPolar(new double[] {ra, dec});
         double[] polar1 = CDSHealpix.radecToPolar(new double[] {ra1, dec1});
         long npixFile = CDSHealpix.ang2pix_nest( nSideFile, polar[0], polar[1]);
         
         HealpixKey h = getHealpixLowLevel(order,npixFile,HealpixKey.SYNC);
         if( h==null ) return Double.NaN;

         long nside = (long)h.width * CDSHealpix.pow2(h.order);
         long npixPixel = CDSHealpix.ang2pix_nest(nside, polar[0], polar[1]);
         
//         List<Long> voisins = CDSHealpix.neighbours_nest(nside,npixPixel);
         long [] voisins = CDSHealpix.neighbours(nside,npixPixel);
         
         // On ne va prendre 3 voisins (S,SW,W) + le pixel en question
         // pour l'interpolation
//         voisins.add(0, npixPixel);
         int m = 4;
         for( int i=m; i>=1; i-- ) voisins[i] = voisins[i-1];
         voisins[0]=npixPixel;
         double totalPixel=0,totalCoef=0;
         HealpixKey h1;
         for( int i=0; i<m; i++ ) {
            h1=h;
//            long nlpix = voisins.get(i).longValue();
            long nlpix = voisins[i];
            
            // Test au cas o� l'on d�borde du HealpixKey courant
            long startIdx =  h.npix * (long)h.width * (long)h.width;
            long pixOffset = nlpix-startIdx;
            if( pixOffset<0 || pixOffset>=h.width*h.width ) {
               long npixFile1 = nlpix/(h.width*h.width);
               HealpixKey htmp = getHealpixLowLevel(order,npixFile1,HealpixKey.SYNC);
               if( htmp==null ) continue;
               h1=htmp;
            }
            
            // Pond�ration en prenant comme coefficient la l'inverse de la distance sur les coordonn�es polaires
            try {
               double pix = h1.getPixelValue(nlpix,HealpixKey.NOW);
               if( Double.isNaN(pix) ) continue;
               double [] polar2 = CDSHealpix.pix2ang_nest(nside, nlpix);
//               if( first1 ) {
//                  double[] radec = CDSHealpix.polarToRadec(polar2);
//                  Coord coo1 = new Coord(radec[0],radec[1]);
//                  aladin.execAsyncCommand("draw line("+coo1.al+" "+coo1.del+" "+ra1+" "+dec1+")");
//               }
               double coef = Coo.distance(polar1[0],polar1[1],polar2[0],polar2[1]);
               if( coef==0 ) return pix;  // Je suis pile dessus
               double c = 1/coef;
               totalPixel += pix * c;
               totalCoef += c;
            } catch( Exception e ) { continue; }
         }
         pixel = totalPixel/totalCoef;
         
//         if( first1 ) {
//            Coord coo = new Coord(ra1,dec1);
//            coo = aladin.localisation.frameToFrame(coo, frameOrigin, Localisation.ICRS);
//            System.out.println("Res = "+aladin.localisation.J2000ToString(coo.al, coo.del)+" diff="+diff+" pix="+pixel);
//         }

      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }

      return pixel;
   }

   
   public static final int DIRSIZE = 10000;

   static public String getFilePath(String survey,int order, long npix) {
      return
           survey + Util.FS +
           "Norder" + order + Util.FS +
           "Dir" + ((npix / DIRSIZE)*DIRSIZE) + Util.FS +
           "Npix" + npix;
  }
   
   protected int [] xy2hpx = null;
   protected int [] hpx2xy = null;
   
   /** M�thode r�cursive utilis�e par createHealpixOrder */
   static private void fillUp(int [] npix, int nsize, int [] pos) {
      int size = nsize*nsize;
      int [][] fils = new int[4][size/4];
      int [] nb = new int[4];
      for( int i=0; i<size; i++ ) {
         int dg = (i%nsize) < (nsize/2) ? 0 : 1;
         int bh = i<(size/2) ? 1 : 0;
         int quad = (dg<<1) | bh;
         int j = pos==null ? i : pos[i];
         npix[j] = npix[j]<<2 | quad;
         fils[quad][ nb[quad]++ ] = j;
      }
      if( size>4 )  for( int i=0; i<4; i++ ) fillUp(npix, nsize/2, fils[i]);
   }

   /** Creation des tableaux de correspondance indice Healpix <=> indice XY */
   protected void createHealpixOrder(int order) throws Exception {
      if (order==0) {
         xy2hpx = hpx2xy = new int[] {0};
         return;
      }
      int nsize = (int)CDSHealpix.pow2(order);
      if( CDSHealpix.log2(nsize)!=order ) throw new Exception("Only HEALPix order power of 2 are supported");
      xy2hpx = new int[nsize*nsize];
      hpx2xy = new int[nsize*nsize];
      fillUp(xy2hpx,nsize,null);
      for( int i=0; i<xy2hpx.length; i++ ) hpx2xy[ xy2hpx[i] ] = i;
   }

   /** Retourne l'indice XY en fonction d'un indice Healpix
    * => n�cessit� d'initialiser au pr�alable avec createHealpixOrdre(int) */
   final public int xy2hpx(int hpxOffset) {
      return xy2hpx[hpxOffset];
   }

   /** Retourne l'indice XY en fonction d'un indice Healpix
    * => n�cessit� d'initialiser au pr�alable avec createHealpixOrdre(int) */
   final public int hpx2xy(int xyOffset) {
      return hpx2xy[xyOffset];
   }
  
   private long lastTouch=0L;
   protected void touchCache() {
      long t = System.currentTimeMillis();
      if( t-lastTouch<60*1000L ) return;     // On "touch" au mieux toutes les minutes
      lastTouch=t;
      String pathName = getCacheDir()+Util.FS+survey+version;
      (new File(pathName)).setLastModified(t);
      Date d = new Date();
      d.setTime(lastTouch);
      aladin.trace(4,"PlanBG.touchCache() : Date:"+d+" => "+pathName);
   }
   
   private long lastIz=-1;
   private int lastMaxOrder=3;
   
   protected int maxOrder(ViewSimple v) {
      long iz = v.getIZ();
      if( lastIz==iz ) return lastMaxOrder;
      lastIz=iz;
      
      if( RES==null ) {
         RES = new double[20];
         for( lastMaxOrder=0; lastMaxOrder<20; lastMaxOrder++ ) {
            long nside = CDSHealpix.pow2(lastMaxOrder+getLosangeOrder()+1);
            RES[lastMaxOrder]=CDSHealpix.pixRes(nside)/3600;
         }
      }
      double pixSize = v.getPixelSize();
      for( lastMaxOrder=2; lastMaxOrder<RES.length && RES[lastMaxOrder]>pixSize; lastMaxOrder++ );
      if( lastMaxOrder==2 && pixSize<0.04 ) lastMaxOrder=3;
      return lastMaxOrder;
   }
   
   /** Retourne l'indication du format de codage des losanges */
   protected String getFormat() {
      if( color ) {
         if( inFits ) return "FITS RGB color";
         else return "JPEG color";
      }
      if( truePixels ) return "FITS true pixels (BITPIX="+bitpix+")";
      else return "JPEG 8 bits pixels";
   }
   
   /** Change le format d'affichage truePixels (Fits) <=> 8bits (JPEG) */
   protected void switchFormat() {
      truePixels = !truePixels;
      forceReload();
      if( aladin.frameCM!=null ) aladin.frameCM.majCM(true);
   }
   
   /** force le rechargement des losanges */
   public void forceReload() {
      FreePixList();
      changeImgID();
      freeHist();
   }
   
   /** Retourne la r�solution angulaire du pixel au NSIDE max (en degr�s)
    *  avec une unit� ad�quate */
   protected String getMaxResolution() {
      return Coord.getUnit( getPixelResolution() );
   }
   
   /** Retourne l'ordre "fichier" maximal (plus grand r�pertoire NOrderNN) */
   public int getMaxFileOrder() { return maxOrder; }
   
   /** Retourne l'ordre Healpix max */
   public int getMaxHealpixOrder() {
      return maxOrder + getLosangeOrder();
   }
   
   /** Retourne la r�solution angulaire du pixel au NSIDE max (en degr�s) */
   public double getPixelResolution() {
      long nside = CDSHealpix.pow2(getMaxHealpixOrder());
      return CDSHealpix.pixRes(nside)/3600;
   }


// POUR FAIRE DES TESTS en utilisant la molette et le s�lecteur de frame
//   double chouilla=0;
//
//   boolean modifValue(int sens) {
//      boolean rep=false;
//      int mode = aladin.localisation.getFrameSelected();
//      switch(mode) {
//         case 1: chouilla+=sens; rep=true; break;
//      }
//      System.out.println("chouilla="+chouilla);
//      aladin.view.repaintAll();
//      return rep;
//   }
//

   /** Retourne true si l'image a �t� enti�rement "draw�" � la r�solution attendue */
   protected boolean isFullyDrawn() { return isDrawn () && allWaitingKeysDrawn; }
   
   /** Retourne true si le dernier appel � draw() � dessiner au moins un losange */
//   protected boolean isDrawn() { return pixList.size()>0 && hasDrawnSomething; }
   protected boolean isDrawn() { return readyDone; }

   /** Retourne true si l'image n'est pas pr�te � �tre affich� */
   protected boolean isLoading() { return !loader.isReadyForDrawing(); }
   
   /** retourne true s'il est encore possible de zoomer en avant pour avoir plus de d�tail */
   protected boolean hasMoreDetails() { return hasMoreDetails; }
   
   /** positionne le flage indiquant qu'il y a ou non encore plus de d�tails disponibles */
   protected void setHasMoreDetails(boolean flag) { hasMoreDetails=flag; }
   
   private boolean hasMoreDetails=true;

   @Override
   protected boolean isSync() {
      if( error!=null ) { 
         aladin.trace(4,"PlanBG.isSync()=true:"+label+" => in error (error!=null)");
         return true;
      }
      if( !flagOk ) {
         aladin.trace(4,"PlanBG.isSync()=false: "+label+" => not ready (!flagOk)");
         return false;
      }
      if( !active ) {
         aladin.trace(4,"PlanBG.isSync()=true: "+label+"=> not active (!active)");
         return true;
      }
      if( getOpacityLevel()==0f && !ref ) {
         aladin.trace(4,"PlanBG.isSync()=true: "+label+"=> transparent (!ref && opacity="+getOpacityLevel()+")");
         return true;
      }
      
      if( flagProcessing ) {
         aladin.trace(4,"PlanBG.isSync()=false: "+label+"=> is processing (flagProcessing)");
         return false;
      }
      if( isLoading() ) {
         aladin.trace(4,"PlanBG.isSync()=false: "+label+"=> is loading (isLoading())");
         return false;
      }
//      if( !isFullyDrawn() ) {
//         aladin.trace(4,"PlanBG.isSync()=false: "+label+"=> is not fully drawn at the best resolution (!isFullyDrawn()=> isDrawn()=="+isDrawn()+"[readyDone="+readyDone+",readyAfterDraw="+readyAfterDraw+"] && allWaitingKeysDrawn=="+allWaitingKeysDrawn+")");
//         return false;
//      }
      
//      aladin.trace(4,"PlanBG.isSync()=true: "+label+"=> ready");
      return true;

//      boolean rep = error!=null || flagOk && (!active || getOpacityLevel()==0f  && !ref || !flagProcessing && !isLoading() && (isFullyDrawn() /* || pourcent==-2*/) );
//      aladin.trace(4,"PlanBG.isSync()="+rep+" => thread="+this.hashCode()+" "+label+" error="+(error==null?"null":error)+
//            " active="+active+" ref="+ref+" opacity="+getOpacityLevel()+" flagProcessing="+flagProcessing+" isLoading="+isLoading()+" isDrawn="+isDrawn()+" isFullyDrawn="+isFullyDrawn()+
//            " pourcent="+pourcent);
//      return rep;
   }

   /** Retourne true si le all-sky est en couleur */
   public boolean isColored() { return color; }
   
   /** Retourne true si le all-sky est affich� en FITS */
   public boolean isTruePixels() { return truePixels; }
   
   /** Retourne true si le all-sky est local */
   public boolean isLocalAllSky() { return localAllSky; }

   private long [] children = null;

   /** Retourne true si tous les fils du losange "susceptibles" d'�tre
    * trac�s sont d�j� pr�ts � �tre dessin� */
   private boolean childrenReady(HealpixKey healpix,ViewSimple v) {
      int order = healpix.order+1;
//      if( tooSmall(v,order) ) return false;
      children = healpix.getChildren(children);
      for( int i=0; i<4; i++ ){
         HealpixKey fils = getHealpix(order,children[i],false);
         if( fils==null ) fils = new HealpixKey(this,order,children[i],HealpixKey.NOLOAD);
         if( fils.isOutView(v) ) continue;
         if( fils.getStatus()!=HealpixKey.READY ) return false;
      }
//System.out.println("***** "+ healpix+" inutile car a tout ses fils");
      return true;
   }

   /** Modification d'une projection */
   @Override
   protected void modifyProj(String label,int modeCalib,
         double alphai, double deltai, double rm,
         double cx, double cy,double r,
         double rot,boolean sym,int t,int system) {
      super.modifyProj(label,modeCalib,alphai,deltai,rm,cx,cy,r,rot,sym,t,system);

      // POUR LE MOMENT CE N'EST PAS UTILISE (PF FEV 2009)
//      for( int i=0; i<aladin.view.viewSimple.length; i++ ) {
//         ViewSimple v = aladin.view.viewSimple[i];
//         if( v.pref!=this || v.projLocal==null ) continue;
//         v.projLocal.modify(label,modeCalib,alphai,deltai,rm,rm,cx,cy,r,r,rot,sym,t);
//      }
   }

   /** Trac� de control des losanges Healpix d'ordre "order" visible dans
    * la vue */
//   protected void drawCtrl(Graphics g,ViewSimple v,int order) {
//      try {
//         long pix[];
//         pix = getPixListView(v,order);
//         if( pix.length>4096 ) return;
//         g.setColor( Couleur.getCouleur(order) );
//         for( int i=0; i<pix.length; i++ ) {
//            HealpixKey healpix = new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD);
//            if( healpix.isOutView(v) ) continue;
//            healpix.drawCtrl(g,v);
//         }
//      } catch( Exception e ) {}
//   }

   protected long [] pixDebugIn = new long[0];

   protected int isDebugIn(long pix) {
      if( !DEBUGMODE ) return 0;
      int n=0;
      if( pixDebugIn.length==0 ) return 0;
      for( int i=0; i< pixDebugIn.length; i++ ) {
         if( pix==pixDebugIn[i] ) n++;
      }
      return n;
   }

//   protected void setDebugIn(double raj,double dej,double radius) {
//      if( !DEBUGMODE ) return;
//      ViewSimple v = aladin.view.getCurrentView();
//      int order = Math.max(3,maxOrder(v));
//      long [] npix = null;
//      Coord c = new Coord(raj,dej);
//      try {
//         c = Localisation.frameToFrame(c,Localisation.ICRS,frameOrigin);
//         npix = getNpixList(order,c,radius);
//      } catch( Exception e ) {
//         e.printStackTrace();
//         return;
//      }
//
//      boolean diff = true;
//      if( npix.length == pixDebugIn.length ) {
//         diff = false;
//         for( int i=0; i<npix.length; i++ ) {
//            if( npix[i]!=pixDebugIn[i] ) { diff=true; break; }
//         }
//      }
//
//      if( diff ) {
//         pixDebugIn = npix;
//         changeImgID();
//         v.repaint();
//
//         System.out.print("querydisk("+aladin.localisation.J2000ToString(raj,dej)+", "+Coord.getUnit(radius)+")\n   2^"+order+" => ");
//         for( int i=0; i<pixDebugIn.length; i++ )  System.out.print(" "+pixDebugIn[i]);
//         System.out.println();
//      }
//   }

   
//   protected void setDebugIn(Ligne deb) {
//      if( !DEBUGMODE ) return;
//      ViewSimple v = aladin.view.getCurrentView();
//      int order = Math.max(3,maxOrder(v));
//      ArrayList<double[]> vlist = new ArrayList<double[]>();
//      Coord coo = new Coord();
//      Ligne tmp;
//      int i;
//      for( i=0, tmp=deb.getFirstBout(); tmp.finligne!=null; tmp=tmp.finligne, i++ ) {
//         coo.al = tmp.raj; coo.del = tmp.dej;
//         if( Double.isNaN(coo.al) ) return;
//         coo = Localisation.frameToFrame(coo,Localisation.ICRS,frameOrigin);
//         if( coo.del < -90) coo.del = -180-coo.del;
//         if( coo.al < 0   ) coo.al  = 360+coo.al;
//         if( coo.del > 90 ) coo.del = 180-coo.del;
//         if( coo.al > 360 ) coo.al  = coo.al-360;
//
//         vlist.add( new double[]{coo.al,coo.del} );
//      }
//      long[] npix=null;
//      try {
//         long nside = CDSHealpix.pow2(order);
//         npix = CDSHealpix.query_polygon(nside,vlist);
//      } catch( Exception e ) {
//         npix=new long[0];
//      }
//
//      boolean diff = true;
//      if( npix.length == pixDebugIn.length ) {
//         diff = false;
//         for( i=0; i<npix.length; i++ ) {
//            if( npix[i]!=pixDebugIn[i] ) { diff=true; break; }
//         }
//      }
//
//      if( diff ) {
//         pixDebugIn = npix;
//         changeImgID();
//         v.repaint();
//         StringBuffer s = new StringBuffer();
//         for( i=0; i<vlist.size(); i++ ) {
//            double a[] = vlist.get(i);
//            if( s.length()>0 ) s.append(", ");
//            s.append(Util.myRound(a[0]+"",2)+(a[1]<0?"":"+")+Util.myRound(a[1]+"",2));
//         }
//         System.out.print("query_polygon("+s+")\n   2^"+order+" => ");
//         for( i=0; i<pixDebugIn.length; i++ )  System.out.print(" "+pixDebugIn[i]);
//         System.out.println();
//      }
//   }


   private final int ALLSKYORDER = 3;

   /** Dessin du ciel complet en rapide � l'ordre indiqu� */
   protected boolean drawAllSky(Graphics g,ViewSimple v) {
      boolean hasDrawnSomething=false;
      if( allsky==null ) {
         if( drawMode==DRAWPOLARISATION ) allsky = new HealpixAllskyPol(this,ALLSKYORDER);
         else allsky =  new HealpixAllsky(this,ALLSKYORDER);
         pixList.put( key(ALLSKYORDER,-1), allsky);

         if( localAllSky ) allsky.loadFromNet();
         else {
            if( !useCache || !allsky.isCached() ) {
               tryWakeUp();
               g.setColor(Color.white);
               g.fillRect(0,0,v.getWidth(),v.getHeight());
               return true;

            } else allsky.loadFromCache();
         }
      }
      int status= allsky.getStatus();

      if( status==HealpixKey.ERROR ) return false;

      if( status==HealpixKey.READY ) {
//         long t = Util.getTime();
         statNbItems=0L;

         double taille = Math.min(v.getTailleRA(),v.getTailleDE());
         if( NOALLSKY ) return true;

         // Petite portion du ciel => recherche des losanges sp�cifiquement
         boolean partial=taille<40 && !v.isAllSky();
         if( partial ) {
            long [] pixList = getPixList(v, null, ALLSKYORDER);
            for( int i=0; i<pixList.length; i++ ) {
               HealpixKey healpix = (allsky.getPixList())[ (int)pixList[i] ];
               if( healpix==null || healpix.isOutView(v) ) continue;
               if( drawMode==DRAWPIXEL ) healpix.draw(g,v);
               else if( drawMode==DRAWPOLARISATION ) ((HealpixKeyPol)healpix).drawPolarisation(g, v);
               statNbItems++;
               hasDrawnSomething=true;
            }

         // Grande portion du ciel => trac� du ciel complet
         } else {
            HealpixKey [] allSkyPixList = allsky.getPixList();
            for( int  i=0; i<allSkyPixList.length; i++ ) {
               HealpixKey healpix = allSkyPixList[i];
               if( healpix==null  || healpix.isOutView(v) ) continue;
               if( drawMode==DRAWPIXEL ) healpix.draw(g,v);
               else if( drawMode==DRAWPOLARISATION ) ((HealpixKeyPol)healpix).drawPolarisation(g, v);
               statNbItems++;
               hasDrawnSomething=true;
            }
         }
         
         if( aladin.macPlateform && aladin.ISJVM15 ) {
            g.setColor(Color.red);
            g.drawString("Warning: Java1.5 under Mac is bugged.",5,30);
            g.drawString("Please update your java.", 5,45);
         }
//         long t1= (Util.getTime()-t);
//       System.out.println("drawAllSky "+(partial?"partial":"all")+" order="+ALLSKYORDER+" in "+t1+"ms");

      } else {
         if( drawMode==DRAWPIXEL ) {
            g.setColor(Color.red);
            g.drawString("Whole sky in progress...", 5,30);
         }
      }
      return hasDrawnSomething;
   }

//   /** Dessin du ciel complet en rapide � l'ordre indiqu� */
//   protected boolean drawAllSky(Graphics g,ViewSimple v,int order) {
//      if( allsky==null ) allsky = new HealpixAllsky[4];
//      if( allsky[order]==null ) {
//         allsky[order] = new HealpixAllsky(this,order);
//         pixList.put( key(order,-1), allsky[order]);
//
//         if( localAllSky ) allsky[order].loadFromNet();
//         else {
//            if( !useCache || !allsky[order].isCached() ) {
//               tryWakeUp();
//
//               // Si jamais on est � l'ordre 3 et que le allsky de l'ordre 2 est pr�t
//               // on va tout de m�me afficher l'ordre 2 en attendant
//               if(  order==3 && allsky[2]!=null &&
//                     allsky[2].getStatus()==HealpixKey.READY ) return drawAllSky(g,v,2);
//
//               // Si jamais on est � l'ordre 2 et que le allsky de l'ordre 3 est pr�t
//               // on va tout de m�me afficher l'ordre 3 en attendant
//               else if(  order==2 && allsky[3]!=null &&
//                     allsky[3].getStatus()==HealpixKey.READY ) return drawAllSky(g,v,3);
//
//               else {
//                  g.setColor(Color.white);
//                  g.fillRect(0,0,v.getWidth(),v.getHeight());
//                  return true;
//               }
//
//            } else allsky[order].loadFromCache();
//         }
//      }
//      int status= allsky[order].getStatus();
//
//      // Si jamais on est � l'ordre 3 et que le allsky de l'ordre 2 est pr�t
//      // on va tout de m�me afficher l'ordre 2 en attendant
//      if( !localAllSky ) {
//         if( order==3 && allsky[3].getStatus()!=HealpixKey.READY && allsky[2]!=null &&
//               allsky[2].getStatus()==HealpixKey.READY ) {
//            tryWakeUp();
//            return drawAllSky(g,v,2);
//         }
//
//         // Si jamais on est � l'ordre 2 et que le allsky de l'ordre 3 est pr�t
//         // on va tout de m�me afficher l'ordre 3 en attendant
//         if( order==2 && allsky[2].getStatus()!=HealpixKey.READY && allsky[3]!=null &&
//               allsky[3].getStatus()==HealpixKey.READY ) {
//            tryWakeUp();
//            return drawAllSky(g,v,3);
//         }
//      }
//      if( status==HealpixKey.ERROR ) return false;
//
//      if( status==HealpixKey.READY ) {
//         long t = Util.getTime();
//         statNbItems=0L;
//
//         double taille = Math.min(v.getTailleRA(),v.getTailleDE());
//         if( NOALLSKY ) return true;
//
//         // Petite portion du ciel => recherche des losanges sp�cifiquement
//         boolean partial=taille<40 && !v.isAllSky();
//         if( partial ) {
//            long [] pixList = getPixList(v, null, order);
////            long [] pixList = getPixListView(v, order);
//            nDrawAllSky+=pixList.length;
//            for( int i=0; i<pixList.length; i++ ) {
//               HealpixKey healpix = (allsky[order].getPixList())[ (int)pixList[i] ];
//               if( healpix.isOutView(v) ) { nOutAllSky++; continue; }
//               if( drawMode!=DRAWPOLARISATION ) healpix.draw(g,v);
//               else healpix.drawPolarisation(g, v);
//               statNbItems++;
//            }
//
//         // Grande portion du ciel => trac� du ciel complet
//         } else {
//            HealpixKey [] allSkyPixList = allsky[order].getPixList();
//            nDrawAllSky+=allSkyPixList.length;
//            for( int  i=0; i<allSkyPixList.length; i++ ) {
//               HealpixKey healpix = allSkyPixList[i];
//               if( healpix.isOutView(v) ) { nOutAllSky++; continue; }
//               if( drawMode!=DRAWPOLARISATION ) healpix.draw(g,v);
//               else healpix.drawPolarisation(g, v);
//               statNbItems++;
//            }
//         }
//         long t1= (Util.getTime()-t);
////System.out.println("drawAllSky "+(partial?"partial":"all")+" order="+order+" in "+t1+"ms");
//         if( order==3 && t1>200 ) minAllSky=2;
//         else if( order==2 && t1<30 ) minAllSky=3;
//
//      } else {
//         if( drawMode==DRAWPIXEL ) {
//            g.setColor(Color.red);
//            g.drawString("Whole sky in progress...", 5,30);
//         }
//      }
//      return true;
//   }

//   HealpixKey ben=null;
//   protected void draw1(Graphics g,ViewSimple v) {
//      if( ben==null ) ben = new HealpixKey(this);
//      ben.draw(g,v);
//   }

   /** Retourne un tableau de pixels d'origine couvrant la vue courante */
   protected void getCurrentBufPixels(PlanImage pi,RectangleD rcrop, double zoom,double resMult,boolean fullRes) {
      int w = (int)Math.round(rcrop.width*zoom);
      int h = (int)Math.round(rcrop.height*zoom);
      int bitpix= getBitpix()==-64 ? -64 : -32;
      int npix = Math.abs(bitpix)/8;
      byte [] pixelsOrigin = new byte[w*h*npix];
      byte [] onePixelOrigin = new byte[npix];
            
      double blank = Double.NaN;
      
      boolean flagClosest = maxOrder()*resMult>maxOrder+4;
      int order = fullRes ? maxOrder : (int)(getOrder()*resMult);
      int a=order;
      if( order<3 ) order=3;
      else if( order>maxOrder ) order=maxOrder;
      
      aladin.trace(4,"PlanBG.getCurrentBufPixels(bitpix="+bitpix+" resMult="+resMult+",fullRes="+fullRes+")" +
      		(flagClosest?" closest":" bilinear")+" order="+a+(a!=order?" ==> "+order:""));
      
      int offset=0;
      double fct = 100./h;
      Coord coo = new Coord();
      Coord coo1 = new Coord();
      for( int y=h-1; y>=0; y-- ) {
         pi.pourcent+=fct;
         for( int x=0; x<w; x++ ) {
            double val;
            
            // Point de r�f�rence milieu bord gauche du pixel d'arriv�e
            // Pour trouver au mieux les 4 pixels Healpix recouvrant le pixel d'arriv�e
            double x1 = rcrop.x + (x+0.5)/zoom;
            double y1 = rcrop.y + (y)/zoom;
            coo.x = x1; coo.y = y1;
            pi.projd.getCoord(coo);
            coo = Localisation.frameToFrame(coo,Localisation.ICRS,frameOrigin);
            
            // Point central du pixel d'arriv�e
            double x2 = rcrop.x + (x+1)/zoom;
            double y2 = rcrop.y + (y)/zoom;
            coo1.x = x2; coo1.y = y2;
            pi.projd.getCoord(coo1);
            coo1 = Localisation.frameToFrame(coo1,Localisation.ICRS,frameOrigin);
            
            if( Double.isNaN(coo.al) || Double.isNaN(coo.del) ) val = Double.NaN;
            else if( flagClosest ) val = getHealpixClosestPixel(coo1.al,coo1.del,order);
            else val = getHealpixLinearPixel(coo.al,coo.del,coo1.al,coo1.del,order);
            if( Double.isNaN(val) ) {
               setPixVal(onePixelOrigin, bitpix, 0, blank);
               if( !((PlanImage)pi).isBlank ) {
                  ((PlanImage)pi).isBlank=true;
                  ((PlanImage)pi).blank=blank;
//                  if( ((PlanImage)pi).headerFits==null ) ((PlanImage)pi).headerFits = new FrameHeaderFits();
                  if( bitpix>0 && ((PlanImage)pi).headerFits!=null) ((PlanImage)pi).headerFits.setKeyValue("BLANK", blank+"");
               }
            } else {
               val = val*bScale+bZero;
               setPixVal(onePixelOrigin, bitpix, 0, val);
            }

            System.arraycopy(onePixelOrigin, 0, pixelsOrigin, offset, npix);
            offset+=npix;
            if( offset>pixelsOrigin.length ) break;  // Le tableau est plein
         }
      }

      // Ajustement des variables en fonction du changement de bitpix
      pi.bitpix = bitpix;
      pi.pixelsOrigin = pixelsOrigin;
      pi.dataMin = dataMin*bScale+bZero;
      pi.dataMax = dataMax*bScale+bZero;
      pi.pixelMin = pixelMin*bScale+bZero;
      pi.pixelMax = pixelMax*bScale+bZero;
      pi.bScale=1; pi.bZero=0;
      pi.pixels = getPix8Bits(null,pi.pixelsOrigin,pi.bitpix,pi.width,pi.height,pi.pixelMin,pi.pixelMax,false);
      pi.invImageLine(pi.width,pi.height,pi.pixels);
      pi.colorBackground=Color.white;

   }
   
   boolean first1=false; //Aladin.PROTO;
   
   /** Retourne un tableau de pixels 8 bits de la zone d�limit�e par le rectangle rcrop (coordonn�es de la vue), ou la vue si null */
   protected byte [] getBufPixels8(ViewSimple v) {
      return getPixels8Area(v,new RectangleD(0,0,v.rv.width,v.rv.height),true);
   }
   protected byte [] getPixels8Area(ViewSimple v,RectangleD rcrop,boolean now) {
      int rgb [] = getPixelsRGBArea(v,rcrop,now);
      if( rgb==null ) return null;
      int taille = rgb.length;
      byte [] pixels = new byte[taille];
      for( int i=0; i<taille; i++ ) pixels[i] = (byte)(rgb[i] & 0xFF);
      rgb=null;
      return pixels;
   }

   /** Retourne un tableau de pixels couleurs de la zone d�limit�e par le rectangle rcrop (coordonn�es de la vue)*/
   protected int [] getPixelsRGBArea(ViewSimple v,RectangleD rcrop,boolean now) {
      if( v==null ) return null;
      BufferedImage imgBuf = new BufferedImage(v.rv.width,v.rv.height,BufferedImage.TYPE_INT_ARGB);
      Graphics g = imgBuf.getGraphics();
      drawLosanges(g, v, now);
      g.finalize(); g=null;

      int width=(int)Math.ceil(rcrop.width);
      int height=(int)Math.ceil(rcrop.height);
      int taille=width*height;
      int rgb[] = new int[taille];
      
      imgBuf.getRGB((int)Math.floor(rcrop.x), (int)Math.floor(rcrop.y), width, height, rgb, 0,width);
      imgBuf.flush(); imgBuf=null;

      return rgb;
   }

   /** Return une Image (au sens Java). M�morise cette image pour �viter de la reconstruire
    * si ce n'est pas n�cessaire 
    * @param now true s'il faut imm�diatement fournir une image compl�te � la r�solution ad�quate
    */
   @Override
   protected Image getImage(ViewSimple v,boolean now) {
	   if( now ) {
		   Image img = aladin.createImage(v.rv.width,v.rv.height);
		   Graphics g = img.getGraphics();
		   v.fillBackground(g);
		   drawLosanges(g,v,now);
		   g.dispose();
		   return img;
	   } 

	   if( v.imageBG!=null && v.ovizBG == v.iz
			   && v.oImgIDBG==imgID && v.rv.width==v.owidthBG && v.rv.height==v.oheightBG ) {
		   return v.imageBG;
	   }

	   if( v.imageBG==null || v.rv.width!=v.owidthBG || v.rv.height!=v.oheightBG ) {
		   if( v.imageBG!=null ) v.imageBG.flush();
		   if( v.g2BG!=null ) v.g2BG.dispose();
		   v.imageBG = aladin.createImage(v.rv.width,v.rv.height);
		   v.g2BG = v.imageBG.getGraphics();
	   }
	   v.oImgIDBG=imgID;
	   v.owidthBG=v.rv.width;
	   v.oheightBG=v.rv.height;
	   v.ovizBG=v.iz;
	   flagClearBuf=false;
	   v.fillBackground(v.g2BG);
	   drawLosanges(v.g2BG,v,now);

	   return v.imageBG;
   }

   /** Tracage de tous les losanges concern�s, utilisation d'un cache (voir getImage())
    * @param op niveau d'opacit�, -1 pour celui d�finit dans le plan
    * @param now true si l'afficahge doit �tre imm�diatement complet � la r�solution ad�quate
    */
   protected void draw(Graphics g,ViewSimple v, int dx, int dy,float op) {
      draw(g,v,dx,dy,-1,false);
   }
   protected void draw(Graphics g,ViewSimple v, int dx, int dy,float op,boolean now) {
      if( v==null ) return;
      if( op==-1 ) op=getOpacityLevel();
      if(  op<=0.1 ) return;
      
      if( g instanceof Graphics2D ) {
         Graphics2D g2d = (Graphics2D)g;
         Composite saveComposite = g2d.getComposite();
         try {
            if( op < 0.9 ) {
               Composite myComposite = Util.getImageComposite(op);
               g2d.setComposite(myComposite);
            }
            if( drawMode==DRAWPIXEL ) g2d.drawImage(getImage(v,now), dx, dy, aladin);
            else if( drawMode==DRAWPOLARISATION ) drawPolarisation(g2d, v);

         } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
         g2d.setComposite(saveComposite);

      } else {
         if( drawMode==DRAWPIXEL ) g.drawImage(getImage(v,now),dx,dy,aladin);
         else if( drawMode==DRAWPOLARISATION ) drawPolarisation(g, v);
      }
      
      setHasMoreDetails( maxOrder(v)<maxOrder ); 
      
      try { frameProgenResume(g,v); } 
      catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }

      readyDone = readyAfterDraw;
	   

   }

   boolean readyAfterDraw=false;
   boolean readyDone=false;

   /** Trace le losange HEALPix sous la position de la souris */
   protected void drawHealpixMouse(Graphics g,ViewSimple v) {
      try {
         Coord coo = new Coord(aladin.localisation.getLastCoord().al,aladin.localisation.getLastCoord().del);
         coo = Localisation.frameToFrame(coo,Localisation.ICRS,frameOrigin);
         int norder = aladin.getOrder();
         if( norder<0 ) return;
         double polar[] = CDSHealpix.radecToPolar(new double[]{coo.al,coo.del});
         long nside = CDSHealpix.pow2(norder);
         long npix = CDSHealpix.ang2pix_nest(nside, polar[0], polar[1]);
         HealpixKey hk = new HealpixKey(this,norder,npix,HealpixKey.NOLOAD);
         hk.drawCtrl(g, v);
         
         if( DEBUGMODE  ) {
            double [][] corners = CDSHealpix.corners(nside, npix);
            for( int i=0; i<4; i++ ) {
               coo = new Coord(corners[i][0],corners[i][1]);
               coo = Localisation.frameToFrame(coo,frameOrigin,Localisation.ICRS);
               Repere r = new Repere(this, coo );
               r.setType(Repere.CARTOUCHE);
               r.setWithLabel(true);
               r.id = ""+i;
               r.projection(v);
               r.draw(g,v,0,0);
            }
         }
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }

   protected int priority=0;
   protected void resetPriority() {
      Enumeration<HealpixKey> e = pixList.elements();
      while( e.hasMoreElements() ) {
         HealpixKey healpix = e.nextElement();
         if( healpix.npix!=-1 && healpix.priority<1000 ) healpix.priority+=1000;
      }
      priority=0;
   }

//   private void compareList(ViewSimple v,int order,long []p,long [] c) {
//      long [] c1 = new long[c.length];
//      System.arraycopy(c, 0, c1, 0, c.length);
//      StringBuffer pres = new StringBuffer();
//      StringBuffer cres = new StringBuffer();
//      int pn=0,cn=0;
//      for( int i=0; i<p.length; i++ ) {
//         boolean out =   (new HealpixKey(this, order, p[i], true)).isOutView(v);
//         boolean trouve = false;
//         for( int j=0; j<c1.length; j++ ) {
//            if( c1[j]==p[i] ) {
//               c1[j]=-1;
//               trouve=true;
//               break;
//            }
//         }
//         if( !trouve ) { pres.append(" "+(out?"["+p[i]+"]":p[i])); pn++; }
//      }
//      for( int i=0; i<c1.length; i++ ) {
//         if( c1[i]!=-1 ) {
//            boolean out =   (new HealpixKey(this, order, c1[i], true)).isOutView(v);
//            cres.append(" "+(out?"["+c1[i]+"]":"*"+c1[i]+"*")); cn++;
//      }
//      }
//
//      if( pn>0 ) System.out.println("diff Polygon ("+pn+"): "+pres);
//      if( cn>0 ) System.out.println("diff Circle  ("+cn+"): "+cres);
//
//   }

   /** Trac� des segments de polarisation disponibles dans la vue et demande de ceux manquants */
   protected void drawPolarisation(Graphics g,ViewSimple v) {
      long [] pix=null;
      int max = Math.min(maxOrder(v),maxOrder);
      int nb=0;
      if( v.getTaille()>20 && (hasDrawnSomething=drawAllSky(g, v))) return;

      setMem();
      resetPriority();

      int order=Math.max(3,max);
      pix = getPixListView(v,order);
      
      for( int i=0; i<pix.length; i++ ) {

         if( (new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;

         HealpixKey healpix = getHealpix(order,pix[i], true);

         // Inconnu => on ne dessine pas
         if( healpix==null ) continue;

         // Positionnement de la priorit� d'affichage
         healpix.priority=250-(priority++);

         int status = healpix.getStatus();

         // Losange erron� ?
         if( status==HealpixKey.ERROR ) continue;

         // On change d'avis
         if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true);

         // Losange � g�rer
         healpix.resetTimer();

         // Pas encore pr�t
         if( status!=HealpixKey.READY ) continue;

         nb+=((HealpixKeyPol)healpix).drawPolarisation(g,v);
      }

      hasDrawnSomething=nb>0;

      tryWakeUp();
   }



   static protected int nDraw1=0;
   static protected int nOut1=0;

//   /** Trac� des losanges disposibles pour une image */
//   protected void drawLosangesInImage(Graphics g,ViewSimple v) {
//      long [] pix=null;
//      int min = Math.max(3,minOrder);
//      int max = Math.min(maxOrder(v),maxOrder);
//
//      drawBackground(g, v);
//      drawAllSky(g,v);
//      redraw.clear();
//
//      for( int order=min; order<=max; order++ ) {
//         pix = getPixListView(v,order);
//         for( int i=0; i<pix.length; i++ ) {
//            if( (new HealpixKey(this,order,pix[i],true)).isOutView(v) ) { pix[i]=-1; continue; }
//            HealpixKey healpix = getHealpix(order,pix[i], false);
//            if( healpix==null ) continue;
//            int status = healpix.getStatus();
//            if( status==HealpixKey.READY ) {
//               healpix.draw(g,v);
//            }
//         }
//      }
//      redraw(g,v);
//   }
   
   
   static final int DRAWFASTMS = 150;     // Tps max autoris� pour un trac� complet, si > mode rapide
   private boolean computeDrawFast=true;  // true autorise une nouvelle �valuation du temps de trac�
   private boolean lastMustDrawFast=true; // Derni�re mesure du mustDrawFast
   
   /** Retourne true s'il faut afficher l'image le plus vite possible
    * Dans le cas o� c'est effectivement le cas, ce mode restera activ� jusqu'�
    * l'appel � resetDrawFastDetection() appel� notamment par le mouseRelease() de ViewSimple
    * (ce qui indique la fin d'un clic and drag)
    * @return
    */
   protected boolean mustDrawFast() {
      if( !computeDrawFast ) return lastMustDrawFast;
      boolean rep = aladin.view.mustDrawFast();
      lastMustDrawFast = !rep ? false : statTimeDisplay>DRAWFASTMS;
      if( lastMustDrawFast ) computeDrawFast=false;
      return lastMustDrawFast;
      
//      if( !rep ) return false;
//      return statTimeDisplay>DRAWFASTMS;
   }
   
   /** Autorise � nouveau la mesure du DrawFast (voir ViewSimple.mouseRelease()) */
   protected void resetDrawFastDetection() { computeDrawFast=true; }
   
   /** Trac� des losanges � la r�solution ad�quate dans la vue 
    * mais en mode synchrone */
   protected void drawLosangesNow(Graphics g,ViewSimple v) {
      int order = Math.max(ALLSKYORDER, Math.min(maxOrder(v),maxOrder) );
      boolean lowResolution = v.isAllSky() && order==ALLSKYORDER;
      
      if( lowResolution ) {
         if( allsky==null ) {
            allsky =  new HealpixAllsky(this,ALLSKYORDER);
            try { allsky.loadNow();
            } catch( Exception e ) { e.printStackTrace(); }
         }
      }
      
      drawBackground(g, v);
      Vector<HealpixKey> localRedraw = new Vector<HealpixKey>(100);
      long [] pix;
      if( v.isAllSky() ) {
         pix = new long[12*(int)CDSHealpix.pow2(order)*(int)CDSHealpix.pow2(order)];
         for( int i=0; i<pix.length; i++ ) pix[i]=i;
      } else pix = getPixList(v,getCooCentre(v),order); // via query_disc()
      
      for( int i=0; i<pix.length; i++ ) {
         if( (new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;
         HealpixKey healpix;
         if( lowResolution && allsky!=null ) healpix = (allsky.getPixList())[i];
         else {
            healpix = getHealpix(order,pix[i], true);
            try { healpix.loadNow(); }
            catch( Exception e ) { e.printStackTrace(); continue; }
         }
         if( healpix.status==HealpixKey.READY )  {
            healpix.resetTimer();
            healpix.draw(g,v,localRedraw);
         }
      }
      redraw(g,v,0,localRedraw);
      drawForeground(g,v);
   }
   
   // le synchronized permet d'�viter que 2 draw simultan�s s'entrem�lent (sur un crop par exemple)
   synchronized protected void drawLosanges(Graphics g,ViewSimple v, boolean now) {
      if( now ) drawLosangesNow(g,v);
      else drawLosangesAsync(g,v);
   }
   
   protected Vector<HealpixKey> redraw = new Vector<HealpixKey>(100);
   
   /** Trac� des losanges disposibles dans la vue et demande de ceux manquants */
   protected void drawLosangesAsync(Graphics g,ViewSimple v) {
      allWaitingKeysDrawn = false;

      long t1 = Util.getTime();
      int nb=0;
      long [] pix=null;
      int min = Math.max(ALLSKYORDER,minOrder);
      int max = Math.min(maxOrder(v),maxOrder);
      
//      System.out.println("Order="+max+" v="+v.rv.width+"x"+v.rv.height);
      
      // On acc�l�re pendant un clic-and-drag
      boolean fast = mustDrawFast();
      if( fast ) min=max;
      
      // Pas sur que ce soit utile mais j'ai d�j� eu des demandes de losanges order 2
      if( min<ALLSKYORDER ) min=max=ALLSKYORDER;

      // Position centrale
      double theta=0,phi=0;
      Coord center = getCooCentre(v);
      if( center!=null ) {
         theta = Math.PI/2 - Math.toRadians(center.del);
         phi = Math.toRadians(center.al);
      }
      
      // Recherche des losanges qui couvrent la vue � la r�solution max
      boolean allKeyReady=true;
      if( max<=ALLSKYORDER ) allKeyReady=false; 
      else {
         pix = getPixList(v,center,max);
         for( int i=0; i<pix.length; i++ ) {
            HealpixKey healpix = getHealpix(max,pix[i], false);
            if( healpix==null && (new HealpixKey(this,max,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) {
               pix[i]=-1; continue;
            }
            if( healpix==null || !healpix.isOutView(v) && healpix.status!=HealpixKey.READY ) {
               allKeyReady=false;
               break;
            }
         }
         if( !allKeyReady ) pix=null;
      }

      // j'affiche le allsky comme fond soit parce que je suis au niveau 3
      // soit parceque tous les losanges ne sont pas pr�ts, 
      boolean pochoir = !aladin.calque.hasHpxGrid();
      if( !allKeyReady  ) {
         if( pochoir ) drawBackground(g, v);
         if( drawAllSky(g,v) ) nb=1;
      }
      resetPriority();
      redraw.clear();
      HealpixKey healpix = null;
      int nOut=0;
      int cmin = allKeyReady ? max : min; // Math.max(min,max-2); 
      for( int order=cmin; order<=max; order++ ) {

         if( !allKeyReady ) {
            // via query_disc()
            /*if( pix==null ) */pix = getPixList(v,center,order); // via query_disc()

//            // Par multiplication par 4 du p�re
//            else {
//               int k=0;
//               long [] pixn = new long[(pix.length-nOut)*4];
//               for( int i=0; i<pix.length; i++ ) {
//                  if( pix[i]==-1 ) continue;
//                  long p = pix[i]*4;
//                  for( int j=0; j<4; j++ ) pixn[k++] = p+j;
//               }
//               pix=pixn;
//            }
            nOut=0;
            if( pix.length==0 ) break;

            nDraw1+=pix.length;

            // On place le losange central en premier dans la liste
            try {
               if( center!=null ) {
                  long firstPix = CDSHealpix.ang2pix_nest(CDSHealpix.pow2(order),theta, phi);

                  // Permutation en d�but de liste
                  for( int i=0; i<pix.length; i++ ) {
                     if( pix[i]==firstPix ) {
                        long a = pix[0]; pix[0] = pix[i]; pix[i]=a;
                        break;
                     }
                  }
               }
            } catch( Exception e ) { }
         }

         for( int i=0; i<pix.length; i++ ) {

            healpix = getHealpix(order,pix[i], false);
            HealpixKey testIn = healpix!=null ? healpix : new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD);
            
            if( !allKeyReady && testIn.isOutView(v) ) {
               nOut1++;
               nOut++;
               pix[i]=-1;
               continue;
            }

            if( healpix==null ) healpix = getHealpix(order,pix[i], true);

            // Inconnu => on ne dessine pas
            if( healpix==null ) continue;
            
            // Juste pour tester la synchro
//            Util.pause(100);

            // Positionnement de la priorit� d'affichage
            healpix.priority=order<max ? 500-(priority++) : priority++;

            int status = healpix.status;

            // Losange erron� ?
            if( status==HealpixKey.ERROR ) continue;

            // On change d'avis
            if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true);

            // Losange � g�rer
            healpix.resetTimer();

            // Pas encore pr�t
            if( status!=HealpixKey.READY && status!=HealpixKey.ERROR ) continue;
            
            // Tous les fils � tracer sont d�j� pr�ts => on passe
            if( order<max && childrenReady(healpix,v) ) {
               healpix.filsFree();
               healpix.resetTimeAskRepaint();
               continue;
            }

            nb+=healpix.draw(g,v);


         }
      }
//      if( healpix!=null ) pixels = healpix.pixels;// Pour que l'histogramme soit � jour

      nb+=redraw(g,v,t1,redraw);
      hasDrawnSomething=nb>0;
      if( hasDrawnSomething && pochoir ) drawForeground(g,v);
      
      //essai
      allWaitingKeysDrawn = allKeyReady || (max<=ALLSKYORDER && hasDrawnSomething);

      tryWakeUp();
//      long t2 = Util.getTime();
//      statTimeDisplay = t2-t1;
      statNbItems = nb;
//aladin.trace(4,"Draw["+min+".."+max+"] "+s1+" "+ +nb+" losanges in "+(statTimeDisplay)+"ms");
   }
   
   private int redraw(Graphics g,ViewSimple v,long t1,Vector<HealpixKey> redraw) {
      int n=0;
      if( redraw.size()>0 ) {
         Object list[] = redraw.toArray();
         for( int i=0; i<list.length; i++ ) {
            HealpixKey healpix = (HealpixKey)list[i];
            try { n += healpix.draw(g, v, 8,redraw); } catch( Exception e ) { }
         }
//      if( n>0 ) System.out.println("Redraw "+redraw.size()+" losanges => "+n+" objets");
      }
      statTimeDisplay = Util.getTime()-t1;
      return n;
   }
   
   /** Retourne le Fps du dernier trac� des losanges */
   protected double getFps() { return statTimeDisplay>0 ? 1000./statTimeDisplay : -1 ; }

   private boolean first=true;

   /** Demande de r�affichage des vues */
   protected void askForRepaint() {
      changeImgID();
      if( first ) {
         first=false;
         aladin.view.setRepere(this);
       }
      aladin.view.repaintAll();
   }

   private int x=0,y=0,rayon=0,grandAxe=0;
   private double angle=0;
   static final int M = 2; //4;
//   static final int EP =4; //12;

   /** Trac� d'un bord le long de projection pour att�nuer le ph�nom�ne de "feston" */
   private void drawForeground(Graphics gv,ViewSimple v) {
//      if( rayon<60 ) return;
      if( v.getTaille()<15 ) return;
      Projection projd = v.getProj();
      Graphics2D g = (Graphics2D) gv;
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(Color.white);
      Stroke st = g.getStroke();
      int m=2;
      g.setStroke(new BasicStroke(5));
      if( projd.t==Calib.SIN || projd.t==Calib.ARC || projd.t==Calib.ZEA) {
         g.drawOval(x-m,y-m,(rayon+m)*2,(rayon+m)*2);
      } else if( projd.t==Calib.AIT || projd.t==Calib.MOL) {
         if( angle==0 ) g.drawOval(x-m,y-m,(grandAxe+m)*2,(rayon+m)*2);
         else Util.drawEllipse(g, x+grandAxe,y+rayon, grandAxe+m, rayon+m, angle );

      }
      g.setStroke(st);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

   }

   /** Trac� d'un fond couvrant la forme de tout le ciel en fonction du type de projection
    * pour att�nuer le ph�nom�ne de "feston" */
   private void drawBackground(Graphics g,ViewSimple v) {
      Projection projd = v.getProj().copy();
      projd.frame=0;

      g.setColor(new Color(cm.getRed(0),cm.getGreen(0),cm.getBlue(0)));
      rayon=0;
      if( projd.t==Calib.TAN ) g.fillRect(0,0,v.getWidth(),v.getHeight());
      else if( projd.t==Calib.SIN || projd.t==Calib.ARC || projd.t==Calib.ZEA) {
         Coord c = projd.c.getProjCenter();
         projd.getXYNative(c);
         PointD center = v.getViewCoordDble(c.x, c.y);
         double signe = c.del<0?1:-1;
         c.del = c.del + signe*( projd.t==Calib.SIN ? 89 : 179);
         projd.getXYNative(c);
         PointD haut = v.getViewCoordDble(c.x, c.y);
         double deltaY = haut.y-center.y;
         double deltaX = haut.x-center.x;
         rayon = (int)(Math.abs(Math.sqrt(deltaX*deltaX+deltaY*deltaY)));
         x = (int)(center.x-rayon);
         y = (int)(center.y-rayon);
         g.fillOval(x,y,rayon*2,rayon*2);
      } else if( projd.t==Calib.AIT || projd.t==Calib.MOL) {
         Projection p =  projd.copy();
         angle = -p.c.getProjRot();
         p.setProjRot(0);
         p.frame = Localisation.ICRS;
         p.setProjCenter(0,0.1);
         Coord c =p.c.getProjCenter();
         p.getXYNative(c);
         PointD center = v.getViewCoordDble(c.x, c.y);
         double signe = c.del<0?1:-1;
         double del = c.del;
         c.del += signe*89;
         p.getXYNative(c);
         PointD haut = v.getViewCoordDble(c.x, c.y);
         c.del = del;
         c.al+=179;
         p.getXYNative(c);
         PointD droit = v.getViewCoordDble(c.x, c.y);
         rayon = (int)(Math.abs(haut.y-center.y));
         grandAxe = (int)(Math.abs(droit.x-center.x));
         x = (int)(center.x-grandAxe);
         y = (int)(center.y-rayon);
         if( angle==0 ) g.fillOval(x,y,grandAxe*2,rayon*2);
         else Util.fillEllipse(g, x+grandAxe,y+rayon, grandAxe, rayon, angle );
      } else g.fillRect(0, 0, v.rv.width, v.rv.height);
   }

   protected float getSegmentLenFactor() {
       return segmentLenFactor;
   }
   protected void setSegmentLenFactor(float segmentLenFactor) {
       this.segmentLenFactor = segmentLenFactor;
   }
   protected int getSegmentThickness() {
       return segmentThickness;
   }
   protected void setSegmentThickness(int segmentThickness) {
       this.segmentThickness = segmentThickness;
   }
   protected float getSegmentDensityFactor() {
       return segmentDensityFactor;
   }
   protected void setSegmentDensityFactor(float segmentDensityFactor) {
       this.segmentDensityFactor = segmentDensityFactor;
   }

   private long timerLastDrawBG = 0L;

   /** demande un r�affichage complet si le dernier affichage commence � se faire
    * vieux histoire de pouvoir purger les losanges d�sormais inutiles */
   private void shouldRefresh() {
      long t = System.currentTimeMillis();
      if( t - timerLastDrawBG < 500 ) return;
      timerLastDrawBG = t;
      if( pixList!=null ) {
         Enumeration<HealpixKey> e = pixList.elements();
         while( e.hasMoreElements() ) {
            HealpixKey healpix = e.nextElement();
            if( healpix!=null ) healpix.setTimeAskRepaint(t);
         }
      }
      changeImgID();
      aladin.view.repaintAll();
   }

   /**
    * G�re le chargement des losanges de mani�re asynchrone
    */
   class HealpixLoader implements Runnable {
      static final int DELAI =3000;   // delai en ms entre deux demandes de chargement des losanges

      private boolean loading;      // false s'il n'y a plus de losange en cours de chargement
      private boolean purging;      // false s'il n'y a plus aucun losange � purger
      private Thread thread=null;
      private Loader cacheLoader,netLoader;

      HealpixLoader() {
         loading=false;
         purging=false;
         cacheLoader = new Loader(0);
         netLoader = new Loader(1);
         wakeUp();
      }

      /** Retourne true si l'image de meilleure r�solution est pr�te */
      protected boolean isReadyForDrawing() { return readyAfterDraw;/* pourcent==-1; */ }

      /** Retourne true s'il y a encore au-moins un losange en cours de chargement */
      protected boolean isLoading() { return loading; }

      /** Retourne true s'il y a encore au-moins un losange � purger */
      protected boolean isPurging() { return purging; }

      /** Demande de parcours de la liste des losanges */
      protected void wakeUp() {
         
         if( thread!=null ) {
            loading=true;
            noSleep();
            if( isSleeping() ) thread.interrupt();
         }
         else {
            thread = new Thread(this,"HealpixLoader");
            Util.decreasePriority(Thread.currentThread(), thread);
//            thread.setPriority(Thread.currentThread().getPriority()-1);
            thread.start();
         }
      }

      volatile private boolean sleep;
      private boolean isSleeping=false;
      private Object lockSleep = new Object();

      private void noSleep() { synchronized( lockSleep ) { sleep=false; } }

      private boolean isSleeping() {
         synchronized( lockSleep ) { return isSleeping; }
      }

      private void setSleep(boolean flag) {
         synchronized( lockSleep ) { isSleeping=flag; }
      }

      /** retourne true tant que le thread du loader a quelque chose sur le feu */
      private boolean shouldRun() {
         return isLoading() || isPurging();
      }

      /** Boucle de surveillance des losanges en attentes */
      public void run() {
         Util.pause(100);
         if( useCache ) cacheLoader.start();
         netLoader.start();
         do {
            sleep=true;
            try {
               try { launchJob(); } catch( Exception e ) { e.printStackTrace(); };

               if( useCache ) scanCache();

               if( sleep ) {
                  setSleep(true);
// System.out.println("Thread dort !");
                  try { Thread.currentThread().sleep(DELAI); }
                  catch( Exception e ) {
//System.out.println("Thread r�veill� !");
                  }
                  setSleep(false);
               }

            } catch( Throwable e1 ) { e1.printStackTrace(); }
         } while ( shouldRun() );
         if( useCache ) cacheLoader.stop();
         netLoader.stop();
         thread=null;
//System.out.println("Plus rien � faire => Thread meurt");
         nbFlush=0;
         aladin.gc();
      }

      /** Parcours de la liste des losanges healpix et lancement du chargement de ceux
       * qui n'ont pas encore �t� demand�. Mise � jour du flag "loading" => voir isLoading()
       */
      private void launchJob() throws Exception {
//System.out.println("Thread working...");
         boolean stillOnePurge=false;
         boolean perhapsOneDeath=false;
         int [] nb = new int[HealpixKey.NBSTATUS];
         boolean flagVerbose =  aladin.calque.hasHpxGrid();
         
         boolean first=true;
         int n=0;

         // Parcours de la liste en commen�ant par les r�solutions les plus mauvaises
         try {
            Enumeration<HealpixKey> e = pixList.elements();
            while( e.hasMoreElements() ) {
               final HealpixKey healpix = e.nextElement();
               int status = healpix.getStatus();
               
               // Un peu de d�buging si besoin
               if( flagVerbose && status!=HealpixKey.ERROR ) {
                  System.out.println((first?"\n":"")+healpix);
                  first=false;
               }

               // Purge ?
               int live = healpix.getLive();
               if( live==HealpixKey.DEATH ) purge(healpix);
               else {
                  if( live==HealpixKey.MAYBEDEATH ) perhapsOneDeath=true;
                  else if( status==HealpixKey.READY ) healpix.purgeFils();
                  stillOnePurge=true;
               }

               switch( status ) {
                  case HealpixKey.ASKING:
                     if( !healpix.allSky && healpix.priority>=1000 ) healpix.setOld();
                     else {
                        if( useCache && healpix.isCached() ) healpix.setStatus(status=HealpixKey.TOBELOADFROMCACHE);
                        else healpix.setStatus(status=HealpixKey.TOBELOADFROMNET);
                     }
                     break;
                  case HealpixKey.READY:
                     if( useCache ) healpix.write();
                     break;
               }

               nb[status]++;

               if( healpix.priority<250 &&
                     !(status==HealpixKey.READY || status==HealpixKey.ERROR) ) n++;
               else if( healpix.npix==-1 &&
                     !(status==HealpixKey.READY || status==HealpixKey.ERROR) ) n+=10;

            }
         } catch( Exception e) { e.printStackTrace(); return; }


         loading= nb[HealpixKey.ASKING]>0 || nb[HealpixKey.TOBELOADFROMCACHE]>0
               || nb[HealpixKey.TOBELOADFROMNET]>0 || nb[HealpixKey.LOADINGFROMCACHE]>0
               || nb[HealpixKey.LOADINGFROMNET]>0;
         purging= stillOnePurge || nb[HealpixKey.PURGING]>0;

         pourcent = n==0 ? -2 : n>=10 ? 1 : (10-n)*10.;
         readyAfterDraw= n==0;

         // Pour du debug
         nbReady=nb[HealpixKey.READY];


//         System.out.print("HealpixKey loader (loading="+loading+" purging="+purging+"): ");
//         for( int i=0; i<HealpixKey.NBSTATUS; i++ ) {
//            if( nb[i]>0 ) System.out.print(HealpixKey.STATUS[i]+"="+nb[i]+" ");
//         }
//         System.out.println();

         if( error==null ) {
            if( nb[HealpixKey.READY]==0 && nb[HealpixKey.ERROR]>5 ) error="Server not available";
            else error=null;
         }

         // Eventuel arr�t du chargement en cours si priorit� d�sormais plus faible
         HealpixKey healpixMin=null,healpixNet=null;
         int min = Integer.MAX_VALUE;
         Enumeration<HealpixKey> e = pixList.elements();
         while( e.hasMoreElements() ) {
            HealpixKey healpix = e.nextElement();
            int status = healpix.getStatus();
            if( status!=HealpixKey.TOBELOADFROMNET && status!=HealpixKey.LOADINGFROMNET ) continue;
            if( status==HealpixKey.LOADINGFROMNET ) healpixNet=healpix;
            if( healpix.priority<min ) {
               min=healpix.priority;
               healpixMin=healpix;
            }
         }

         if( healpixNet!=null && healpixNet!=healpixMin ) healpixNet.abort();

         if( nb[HealpixKey.TOBELOADFROMCACHE]>0 ) cacheLoader.wakeUp();
         if( nb[HealpixKey.TOBELOADFROMNET]>0 )   netLoader.wakeUp();

         // Pour faire blinker le plan
         if( oLoading!=loading ) {
            oLoading=loading;
            aladin.calque.select.repaint();
         }

         if( perhapsOneDeath ) shouldRefresh();

      }
   }

   private boolean oLoading = false;

   /** G�re le chargement d'un losange de mani�re asynchrone
    */
   class Loader implements Runnable {

      boolean encore;
      Thread thread;
      int type;
      String label;

      Loader(int type) {
         this.type=type;
         label=type==0?"LoaderCache":"LoaderNet";
      }

      void start() {
         encore=true;
         if( thread!=null ) thread.interrupt();
         thread = new Thread(this,label);
         Util.decreasePriority(Thread.currentThread(), thread);
         thread.start();
      }

      void wakeUp() {
         if( thread==null ) start();
         else if( pause ) thread.interrupt();
      }

      private boolean pause=false;
      synchronized void setPause(boolean flag) { pause=flag; }

      void stop() {
         encore=false;
         if( thread!=null ) thread.interrupt();
      }

      /** M�morisation des pixels d'un losange d'ordre 3 dans le allsky correspondant
       * si cela n'a pas d�j� �tait fait, afin d'am�liorer la r�solution (64x64 => 128x128)
       * PEUT ETRE A SUPPRIMER DANS LE CAS D'UNE MACHINE TROP LENTE */
//      private void keepInAllSky(HealpixKey healpix) {
//if( !color ) return;
//         if( healpix.order!=3 || allsky==null || allsky[healpix.order]==null ) return;
//         int w=128;
//         HealpixKey h = allsky[healpix.order].elementAt((int)healpix.npix);
//         if( h==null || h.width==w ) return;      // d�j� fait
//
//         int [] rgbTmp=null;
//         byte [] pixelsTmp=null;
//         if( color ) rgbTmp = new int[w*w];
//         else pixelsTmp = new byte[w*w];
//
//         int gap = healpix.width/w;
//         for( int y=0; y<w; y++ ) {
//            for( int x=0; x<w; x++ ) {
//               if( color ) rgbTmp[y*w+x] = healpix.rgb[ (y*gap)*healpix.width +x*gap ];
////               else pixelsTmp[y*w+x] = (byte)( (255-(int)healpix.pixels[ (y*gap)*healpix.width +x*gap]) & 0xFF );
//               else pixelsTmp[y*w+x] = healpix.pixels[ (y*gap)*healpix.width +x*gap];
//            }
//         }
//         if( color ) h.rgb=rgbTmp;
//         else h.pixels=pixelsTmp;
//         h.height=h.width=w;
//         h.imgBuf=null;
//         h.filsFree();
////         System.out.println("Keep in AllSky in "+w+"x"+w+" "+healpix);
//      }

      public void run() {
         boolean flagLoad;

         while( encore ) {
            try {
//System.out.println(label+" running...");
               flagLoad=false;


               try {
                  Enumeration<HealpixKey> e = pixList.elements();
                  int min = Integer.MAX_VALUE;
                  HealpixKey minHealpix=null;
                  while( e.hasMoreElements() ) {
                     final HealpixKey healpix = e.nextElement();
                     int status = healpix.getStatus();
                     if( (type==0 && status==HealpixKey.TOBELOADFROMCACHE
                       || type==1 && status==HealpixKey.TOBELOADFROMNET)
                           && healpix.priority<min ) {
                        minHealpix=healpix;
                        min=healpix.priority;
                     }
                  }
                  if( minHealpix!=null ) {
                     if( type==0 ) minHealpix.loadFromCache();
                     else minHealpix.loadFromNet();
                     if( !minHealpix.allSky ) setLosangeOrder(minHealpix.getLosangeOrder());
//                     keepInAllSky(minHealpix);
                     flagLoad = true;
                  }
               } catch( Exception e) {}

               if( flagLoad ) loader.wakeUp();
               else {
//System.out.println(label+"'s sleeping");
                  try {
                     setPause(true);
                     Thread.currentThread().sleep(10000);
                     setPause(false);
                  } catch( Exception e ) {
//                     System.out.println(label+" wakeup !");
                  }
               }
            } catch( Throwable t ) { t.printStackTrace(); }
         }
         thread=null;
      }
   }

   public void center(Coord coord) {
	   aladin.view.setRepere(coord);
	   aladin.view.showSource();
	   aladin.view.zoomview.repaint();
	   aladin.calque.repaintAll();
   }
   
   
   /**************************************** Informations statistiques du m�canisme Allsky ******************************/
   
   // pour pouvoir mesurer la moyenne du temps des chargements
   protected long cumulTimeLoadNet=0;    // Temps cumul� de chargement via le Net
   protected long cumulTimeLoadCache=0;  // Temps cumul� de chargement via le Cache
   protected long cumulTimeWriteCache=0; // Temps cumul� d'�criture dans le Cache
   protected long cumulTimeDraw=0;       // Temps cumul� d'affichage des losanges
   protected long cumulTimeStream=0;     // Temps cumul� pour le d�chargement (via le Net)
   protected long cumulTimeJPEG=0;       // Temps cumul� pour le d�codage JPEG (via le Net)
   protected long cumulTimePixel=0;      // Temps cumul� pour l'extraction des pixels (via le Net)
   protected int nbLoadNet=0;            // Nombre de losanges lus via le Net
   protected int nbLoadCache=0;          // Nombre de losanges lus via le Cache
   protected int nbWriteCache=0;         // Nombre de losanges �cris dans le Cache
   protected int nbImgDraw=0;            // Nombre d'affichages de losange
   protected long nByteReadNet=0L;       // Nombre de bytes lus via le Net
   protected long nByteReadCache=0L;     // Nombre de bytes lus via le Cache
   protected long nByteWriteCache=0L;    // Nombre de bytes �crit dans le Cache
   protected int nbImgCreated=0;         // Nombre d'image cr��es
   protected int nbImgInBuf=0;           // Nombre d'image reprise en m�moire
   protected int nbCreated=0;            // Nombre de losanges cr��s (sans compter les fils)
   protected int nbAborted=0;            // Nombre de losanges interrompus
   protected int nbFree=0;               // Nombre de losanges supprim�s

   /** Reset des statistiques */
   protected void resetStats() {
      cumulTimeLoadNet=cumulTimeLoadCache=cumulTimeWriteCache=cumulTimeDraw=0L;
      cumulTimeStream=cumulTimeJPEG=cumulTimePixel=0L;
      nbLoadNet=nbLoadCache=nbWriteCache=0;
      nByteReadNet=nByteReadCache=nByteWriteCache=0L;
      nbImgCreated=nbImgInBuf=nbCreated=nbFree=nbAborted=nbImgDraw=0;
   }

   /** retourne une chaine donnant des stats sur l'usage des losanges Healpix */
   protected String getStats() {
      return "HealpixKey stats: "+label+":\n" +
            ".Created: "+nbCreated+"   Abort: "+nbAborted+"   Free: "+nbFree+"\n" +
            ".Net   : "+nbLoadNet  +" => "+Util.round(nByteReadNet/(1024*1024.),1)  +"Mb in ~" +Util.round(rateLoadNet(),1)  +"ms"
               +" "+streamJpegPixel()+"\n"+
            ".CacheR: "+nbLoadCache+" => "+Util.round(nByteReadCache/(1024*1024.),1)+"Mb in ~" +Util.round(rateLoadCache(),1)+"ms\n" +
            ".CacheW: "+nbWriteCache+" => "+Util.round(nByteWriteCache/(1024*1024.),1)+"Mb in ~" +Util.round(rateWriteCache(),1)+"ms\n" +
            ".Img created: "+nbImgCreated+"    reused:"+nbImgInBuf+"    drawn "+nbImgDraw+" in ~"+Util.round(averageTimeDraw(),2)+"ms\n"
            ;
   }
   
   /** retourne une chaine donnant des stats minimales sur l'usage des losanges Healpix */
   protected String getShortStats() { 
      if( nbLoadNet==0 ) return null;
      return label+(!url.startsWith("http:")?" Local:":" Net:")+nbLoadNet  +"/"+Util.round(nByteReadNet/(1024*1024.),2)  +"Mb/" +Util.round(rateLoadNet(),2)  +"ms"
      +" CacheR:"+nbLoadCache+"/"+Util.round(nByteReadCache/(1024*1024.),2)+"Mb/" +Util.round(rateLoadCache(),2)+"ms"
      +" CacheW:"+nbWriteCache+"/"+Util.round(nByteWriteCache/(1024*1024.),2)+"Mb/" +Util.round(rateWriteCache(),2)+"ms";
   }
   
   /** Retourne le temps moyen pour le chargement r�seau d'une image, sa d�compression JPEG, l'extraction des pixels */
   protected String streamJpegPixel() {
      if( nbLoadNet==0 ) return "";
      return "(stream="+Util.round(cumulTimeStream/nbLoadNet,1)
      +"/jpeg="+Util.round(cumulTimeJPEG/nbLoadNet,1)
      +"/getpix="+Util.round(cumulTimePixel/nbLoadNet,1)+")";
   }

   /** retourne le temps moyen pour l'affichage d'un losange en ms */
   protected double averageTimeDraw() {
      if( nbImgDraw==0 ) return 0;
      return ((double)cumulTimeDraw/nbImgDraw)/1000000;
   }

   /** Retourne le nombre de m�gaoctets trait�s par seconde en lecture sur le r�seau */
   protected double rateLoadNet() {
      if( cumulTimeLoadNet==0 ) return 0;
      return ((double)cumulTimeLoadNet/nbLoadNet);
   }

   /** Retourne le nombre de m�gaoctets trait�s par seconde en lecture depuis le cache */
   protected double rateLoadCache() {
      if( cumulTimeLoadCache==0 ) return 0;
      return ((double)cumulTimeLoadCache/nbLoadCache);
   }

   /** Retourne le nombre de m�gaoctets trait�s par seconde en �criture sur le cache */
   protected double rateWriteCache() {
      if( cumulTimeWriteCache==0 ) return 0;
      return ((double)cumulTimeWriteCache/nbWriteCache);
   }
   
   


}
