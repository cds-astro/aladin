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

import cds.fits.HeaderFits;
import cds.tools.Util;

import java.io.*;
import java.util.*;

/**
 * Gestion d'un plan image Blink
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : novembre 2004 ajustements pour la version officielle (P.Fernique)
 * @version 1.0 : septembre 2004 creation
 */
public class PlanImageBlink extends PlanImage {

   protected PlanImage pRef;
   protected int initDelay;		// Le delai initial
   protected boolean initPause; // en pause lors de la régénération du blinkController
   protected double initFrame;     // La frame initiale
   protected Vector<PlanImageBlinkItem> vFrames;
   protected int depth;
   private PlanImage tmpP[];    // Subtilité de programmation pour traiter la création
                                // au même titre qu'un ajout
   protected boolean flagAppend;  // True si la prochaine action sur run() ajoutera un frame

   /** Creation d'un plan de type IMAGEBLINK (via 2 plans ou plus)
    * @param p[]  La liste des plans, le premier étant celui de référence
    * @param label le label du plan
    * @param delay le délai en ms souhaité entre deux images (0 si arrêté)
    */
   protected PlanImageBlink(Aladin aladin, PlanImage p[], String label,int delay) {
      super(aladin);
      type=IMAGEBLINK;
      initDelay=delay;
      initPause=false;
      initFrame=0;
      isOldPlan=false;

      pRef=p[0];
      vFrames=new Vector<PlanImageBlinkItem>();
       
      Aladin.trace(3,"Blink ref plane: " + pRef.label);

      // Initialisation des parametres communs
      init(label,pRef);

      // Mémorisation des plans à traiter (sauf le premier qui est
      // celui de référence
      tmpP=new PlanImage[p.length - 1];
      System.arraycopy(p,1,tmpP,0,p.length - 1);

      StringBuffer s = new StringBuffer();
      for( int i=0; i<p.length; i++ ) s.append((i>0?"/":"")+p[i]);
      sendLog("Blink","["+s+"]");

      flagAppend=false;
      synchronized( this ) {
         runme=new Thread(this,"AladinBuildBlink");
         Util.decreasePriority(Thread.currentThread(), runme);
         runme.start();
      }
   }
   
   protected PlanImageBlink(Aladin aladin,String file,MyInputStream in,String label,String from,
         Obj o,ResourceNode imgNode,boolean skip,boolean doClose,Plan forPourcent) {
      super(aladin,file,in,label,from,o,imgNode,skip,doClose,forPourcent);
   }
   
   /** Nettoyage du plan pour aider le GC
    */
   protected boolean Free() {
      if( !super.Free() ) return false;
      freeRam();
      pRef=null;
      vFrames=null;
      if( fCacheBis!=null ) try { fCacheBis.close(); } catch( Exception e) {}
      cacheIDBis=null;
      return true;
   }
   
   /** Copie les parametres
    * @param p le plan de reference pour le reechantillonage
    */
   protected void init(String label,PlanImage p) {
      
      p.copy(this);
      type=IMAGEBLINK;
      flagOk=false;
      askActive=true;
      headerFits=null;
      pixelsZoom = null;
      fmt=UNKNOWN;
      res=UNDEF;
      orig=COMPUTED;
      status="Re-sampling...";
      progress="computing...";
      if( label==null ) label="Blk img";
      setLabel(label);
      copyright="Blink sequence by Aladin";
      param="";
      
      // On met une table de couleur linéaire car on va appliquer à chaque pixel 8 bits à insérer
      // la table des couleurs et la fonction de transfert de son image d'origine
      transfertFct=PlanImage.LINEAR;
      video = VIDEO_NORMAL;
      typeCM = p.typeCM;
      cmControl[0] = 0; cmControl[1] = 128; cmControl[2] = 255;
      cm = ColorMap.getCM(0,128,255,false,typeCM,transfertFct);
      vFrames.addElement( new PlanImageBlinkItem(p));
      dataMin=pixelMin=0;
      dataMax=pixelMax=255;
      bZero=0;
      bScale=1;
   }
   
   static public final int PERM0 = 0; // PERM0 : naxis1 x naxis2 sur naxis3 niveaux
   static public final int PERM1 = 1; // PERM1 : naxis1 x naxis3 sur naxis2 niveaux
   static public final int PERM2 = 2; // PERM2 : naxis3 x naxis2 sur naxis1 niveaux
   
   static public final int W2D  = 0;  // Permute Width et Depth
   static public final int H2D  = 1;  // Permute height et Depth
   static public final int CP   = 2;  // Cycle naxis3 -> naxis1 -> naxis2 -> naxis3
   static public final int CM   = 3;  // Cycle inverse
   
   
   /** Permutation courante */
   private int modePerm = PERM0;
   
   /** Retourne la permutation courante */
   public int getPermutation() { return modePerm; }
   
   /** Détermine et effectue la permutation à effectuer en fonction de l'état courant
    * et de celui à obtenir */
   public void permutation(int m) {
      int action=-1;
      if( m==modePerm ) return;     // déjà fait
           if( modePerm==PERM0 && m==PERM1 
            || modePerm==PERM1 && m==PERM0 ) action=H2D;
      else if( modePerm==PERM0 && m==PERM2 
            || modePerm==PERM2 && m==PERM0 ) action=W2D;
      else if( modePerm==PERM1 && m==PERM2 ) action=CP;
      else if( modePerm==PERM2 && m==PERM1 ) action=CM;
           
      boolean full = modePerm==PERM0 ? loadInRam(0, depth) : isFullyInRam(0, depth);
      
      flagProcessing=true;
      pourcent=-1;
      aladin.calque.repaintAll();
      (new ThreadPermute(m,action,full)).start();
   }
   
   class ThreadPermute extends Thread {
      int m;
      int action;
      boolean full;
      
      public ThreadPermute(int m,int action,boolean full) {
         super();
         this.m=m; this.action=action; this.full=full;
      }
      public void run() {
         doPermute(action,full);
         permuteCalib(m);
         modePerm=m;
         flagProcessing=false;
         pourcent= -1;
         changeImgID();
         aladin.calque.repaintAll();
      }
   }
   
   // Effectue une permutation particulière
   private void doPermute(int a,boolean full) {
      
      // Nouvelles dimensions
      int w = a==W2D || a==CP ? depth : a==CM ? height : width;
      int h = a==H2D || a==CM ? depth : a==CP ? width : height;
      int d = a==CP || a==H2D ? height : width;
      
      ArrayList<PlanImageBlinkItem> v=null;
      
      
      // On teste 2 fois en full, en cas de OutOfMemory
      for( int j=full?1:0; j<2; j++ )  {
         
         // On ne peut pas travailler sur les true pixels, autant faire de
         // la place avant.
         if( !full ) {
            for( PlanImageBlinkItem p : vFrames ) p.pixelsOrigin=null;
            aladin.gc();
         }

         try {
            // Initialisation
            v = new ArrayList<PlanImageBlinkItem>(d);
            for( int i=0; i<d; i++ ) {
               byte [] pixels = new byte[ w*h ];
               byte [] pixelsOrign = full ? new byte [ w*h*npix ] : null;
               v.add( new PlanImageBlinkItem(label,pixels,pixelsOrign,false,null,0) );
            }
            break; // C'est bon
            
         } catch( OutOfMemoryError e ) {
            aladin.console.printError("!!! Not enough memory => trying Cube permutation without true pixels...");
            full=false;
            v=null;
            System.gc();
         }
      }
      
      double deltaPourcent = 100./depth;
      
      // Permutation des pixels
      for( int z=0; z<depth; z++ ) {
         pourcent+=deltaPourcent;
         PlanImageBlinkItem pi = vFrames.get(z);
         for( int y=0; y<height; y++ ) {
            for( int x=0; x<width; x++ ) {
               PlanImageBlinkItem p = v.get( a==CP || a==H2D ? y : x );
               int src = y*width + x;
               int trg = (a==H2D || a==CM ? z : a==CP ? x : y)* w 
                       + (a==W2D || a==CP ? z : a==CM ? y : x);
               p.pixels[ trg ] = pi.pixels[ src ];
               if( full ) {
                  System.arraycopy(pi.pixelsOrigin,src*npix,p.pixelsOrigin,trg*npix,npix);
               }
               
            }
         }
      }
      
      // Mise sous forme vecteur
      Vector<PlanImageBlinkItem> v1 = new Vector<PlanImageBlinkItem>(d);
      for( PlanImageBlinkItem p : v ) v1.add(p);
      
      // Remplacement
      synchronized( this ) {
         vFrames = v1;
         naxis1=width = w;
         naxis2=height = h;
         depth = d;
         oLastFrame=-1;
         activePixels(0);
      }
   }
   
   
   // Ajustement de la calibration originale en fonction de la permutation demandée
   // Travaille par substitution des préfixes numériques des mots clés FITS originaux
   private void permuteCalib(int m) {
      try {
         if( headerFits==null ) return;
         String originalHeaderFits = headerFits.getOriginalHeaderFits();
              if( m==PERM1 ) originalHeaderFits = modifCalib(originalHeaderFits,new String[] { "2-3","3-2" });
         else if( m==PERM2 ) originalHeaderFits = modifCalib(originalHeaderFits,new String[] { "1-3","3-1" });
         Projection proj = new Projection(Projection.WCS,new Calib( new HeaderFits(originalHeaderFits) ));
         setNewProjD(proj);
         setHasSpecificCalib();
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         projd=null;
         projD=null;
         return;
      }
   }
   
   // Substitutions dans le headerfits d'un certain nombre de suffixes numériques
   // des mots clés. Par exemple rules = { "2-3", "3-2" } signifie que tous
   // les mots clés finissant par "2" vont être remplacé par "3" et réciproquement
   // (NAXIS2 = ... va être remplacé par NAXIS3 = ....)
   private String modifCalib(String s,String [] rules) {
      StringTokenizer st = new StringTokenizer(s,"\n");
      StringBuffer res = new StringBuffer();
      while( st.hasMoreTokens() ) {
         String line = st.nextToken();
         if( !line.startsWith("COMMENT") && !line.startsWith("HISTORY")) {
            int pos = line.indexOf('=');
            if( pos!=-1 ) {
               char ch=' ';
               for( pos--; pos>0 && Character.isWhitespace( ch=line.charAt(pos)); pos--);
               if( pos>0 && Character.isDigit(ch) ) {
                  for( int i=0; i<rules.length; i++ ) {
                     char avant = rules[i].charAt(0);
                     char apres = rules[i].charAt( rules[i].length()-1 );
                     if( ch==avant ) {
                        line = line.substring(0,pos)+apres+line.substring(pos+1); 
                        break;
                     }
                  }
               }
            }
         }
         res.append(line+"\n");
      }
      return res.toString();
   }
   
//   protected void noOriginalPixels() {
//      vFrames.elementAt(0).cacheID=null;
//      super.noOriginalPixels();
//   }
   
   //   protected boolean setActivated() { return setActivated(askActive); }
   protected boolean setActivated(boolean flag) {
      flag=super.setActivated(flag);
      if( flag ) aladin.view.startTimer();
      return flag;
   }

   /** Attente pendant la construction du plan.
    * @return <I>true</I> si ok, <I>false</I> sinon.
    */
   protected boolean waitForPlan() {
      addFrame(tmpP);
      return true;
   }

   /** Lance le chargement du plan */
   public void run() {
      if( flagRecut ) { flagRecut=false; runRecut(); return; }
      if( !flagAppend ) {
         Aladin.trace(1,"Creating the " + Tp[type] + " plane " + label);
         if( this instanceof PlanImageCube ) planReady(super.waitForPlan());
         else {
            planReady(waitForPlan());
            pRef=null;
         }
         flagAppend=true;
      } else {
         Aladin.trace(1,"Adding planes to " + label);
         addFrame(tmpP);
         flagOk=true;
      }
      aladin.view.startTimer();
   }

   synchronized protected void addPlan(PlanImage p) {
      flagOk=false;
      flagProcessing=true;
      tmpP=new PlanImage[1];
      tmpP[0]=p;

      aladin.calque.select.repaint();

      synchronized( this ) {
         runme=new Thread(this,"AladinBlinkAdd");
         Util.decreasePriority(Thread.currentThread(), runme);
         runme.start();
      }
   }
   
   synchronized protected void addFrame(String label,byte pixels[],byte pixelsOrigin[],
         boolean cacheFromOriginalFile,String cacheID, long cacheOffset) {
      
      if( vFrames==null ) vFrames = new Vector<PlanImageBlinkItem>();
      vFrames.addElement( new PlanImageBlinkItem(label,pixels,pixelsOrigin,cacheFromOriginalFile,cacheID,cacheOffset));
   }
   
   synchronized protected void addFrame(PlanImage p[]) {

      Aladin.trace(3,"Adding "+p.length+" frame(s)...");
      
      Coord coo=new Coord();
      int x=0, y=0;
      int w=width;
      int taille = width*height;
      int i;
      
      byte c[][]=new byte[p.length][taille];
      boolean theSame[]=new boolean[p.length];
      boolean flagAllTheSame=true;
      for( int n=0; n < p.length; n++ ) {
         if( (theSame[n]=!Projection.isOk(projd) && !Projection.isOk(p[n].projd))
               || (theSame[n]=projd.c.TheSame(p[n].projd.c)) ) {
            p[n].getLinearPixels8(c[n]);
            Aladin.trace(4,"PlanImageBlink.addFrame() : "+p[n].label);
         } else {
            Aladin.trace(4,"PlanImageBlink.addFrame() : "+p[n].label+" (resampled)");
            flagAllTheSame=false;
         }
      }

      if( !flagAllTheSame ) {
         for( i=0; i < taille; i++ ) {
            coo.x=i % w;
            coo.y=i / w;
            projd.getCoord(coo);
            if( Double.isNaN(coo.al) ) continue;

            for( int n=0; n < p.length; n++ ) {
               if( theSame[n] ) continue;
               PlanImage p2=p[n];

               p2.projd.getXY(coo);
               if( !Double.isNaN(coo.x) ) {
                  x=(int) Math.round(coo.x);
                  y=(int) Math.round(coo.y);
                  if( x >= 0 && x < p2.width && y >= 0 && y < p2.height ) {
                     int pix = 0xff & (int)p2.getBufPixels8()[y* p2.width + x];
                     c[n][i]=(byte) p2.cm.getBlue(pix);
                  }
               }

               // Pour laisser la main aux autres threads
               // et pouvoir afficher le changement de pourcentage
               if( (i * p.length) % 10000 == 0 ) {
                  setPourcent(i * 100 / taille);
                  if( Aladin.isSlow ) Util.pause(10);
               }
            }
         }
      }

      // Mémorisation du résultat
      for( int n=0; n < p.length; n++ ) {
         PlanImage pi = p[n];
         vFrames.addElement( new PlanImageBlinkItem(pi.label,c[n], null,
                                     pi.cacheFromOriginalFile,pi.cacheID,pi.cacheOffset));
      }
      setPourcent(-1);
      flagOk=true;
      flagProcessing=false;

      Aladin.trace(3,"Adding a frame achieved...");

      // Personnalisation des parametres
      changeImgID();
      aladin.view.repaintAll();
   }
   
   /** Retourne true si on dispose (ou peut disposer) des pixels originaux */
   protected boolean hasOriginalPixels() {
     PlanImageBlinkItem p = vFrames.elementAt(0);
     return p.pixelsOrigin!=null || p.cacheID!=null;
   }
     
   
   private boolean  flagRecut=false;   // Pour l'aiguillage du thread sur le recutCube
   private double _min,_max;           // passage de paramètres pour le thread
   private boolean _autocut;           // idem
   private boolean _restart;           // Flag de relance du recutCube
   protected Thread threadRecut=null;  // thread du recut, null si aucun
   
   
   /** Recut sur le plan courant, puis lancement ou réinitialisation du thread pour
    * le recut du cube complet */
   protected boolean recut(double min,double max,boolean autocut) {
      if( !hasOriginalPixels() ) return false;
      getLock();
      flagUpdating=true;
      if( min==-1 && max==-1 ) { min=dataMinFits; max=dataMaxFits; }
      _min=min; _max=max; _autocut=autocut; flagRecut=true; _restart=false;
            
      ViewSimple vc = aladin.view.getCurrentView();
      int frame = vc.blinkControl.lastFrame;
      activePixelsOrigin(frame);
      PlanImageBlinkItem pbi = vFrames.elementAt(frame);
      pixelsOriginFromCache();
      getPix8Bits(pbi.pixels,pixelsOrigin,bitpix,width,height,min,max,autocut);
      invImageLine(width,height,pbi.pixels);
      changeImgID();
      calculPixelsZoom(pbi.pixels);
      aladin.calque.select.repaint();
      aladin.calque.zoom.zoomView.repaint();
      vc.repaint();
      
      // Pour que le cut soit homogène sur tout le cube
      _autocut=false;
      _min=pixelMin;
      _max=pixelMax;

      if( !isThreadingRecut() ) setThreadRecut( new Thread(this,"AladinBlinkRecut") ).start();
      else recutCubeAgain();
      

      return true;
   }
   
   synchronized Thread setThreadRecut(Thread t) { threadRecut=t; return t; }
   synchronized boolean isThreadingRecut() { return threadRecut!=null; }
   
   // Gestion d'un lock pour le passage de paramètres au thread du recut */
   private boolean lock;
   private synchronized void setLock(boolean l) { lock=l; }
   private synchronized boolean isLocked() { return lock; }
   private void getLock() {
      while( isLocked() ) Util.pause(10);
   }
   
   synchronized void setRestart(boolean f) { _restart=f; }
   synchronized boolean isRestart() { return _restart; }
   
   /** Relance du thread du recutCube() via le positionnement du flag _restart */
   private void recutCubeAgain() {
//System.out.println("Restart recut all cube");
      setRestart(true);
   }
   
   /** Initialisation du thread du recutCube() */
   private void runRecut() {
//System.out.println("Start recut all cube");
      recutCube(_min,_max,_autocut);
   }

   /** Recut du cube complet par accès aux pixels d'origine via le cache ou le fichier
    * local d'origine */
   protected boolean recutCube(double min,double max,boolean autocut) {
      setRestart(false);
      setLock(false);
                  
      Date d=new Date(); 
      String ocacheID=null;
      
      // Travaille sur son propre buffer pour ne pas interférer avec pixelsOrigin[]
      byte [] buffer = new byte[width*height*npix];
      byte [] buf = null;

      RandomAccessFile f=null;
      Aladin.trace(3,"Original cube pixels reloaded frame by frame");            
      
      for( int frame=0; frame<depth; frame++ ) {
         if( frame%5==0 ) Util.pause(10);  // petite pause pour souffler
         try {
            
            // Relance du recut récursivement (par commodité)
            if( isRestart() ) {
               if( f!=null ) f.close();
               buffer=null;  // Pour éviter d'empiler les buffer
               return recutCube(_min,_max,_autocut);
            }
            
            // Traitement de la frame j avec fermeture/ouverture en cas de changement de fichier
            // par rapport à la frame j-1
            PlanImageBlinkItem pbi = vFrames.elementAt(frame);
            if( pbi.pixelsOrigin!=null ) buf = pbi.pixelsOrigin;
            else {
               if( ocacheID!=pbi.cacheID ) {
                  if( f!=null ) f.close();
                  f = new RandomAccessFile(new File(pbi.cacheID),"r");
                  ocacheID=pbi.cacheID;
               }
               f.seek(pbi.cacheOffset);
               f.readFully(buffer);
               buf = buffer;
            }
            getPix8Bits(pbi.pixels,buf,bitpix,width,height,min,max,autocut);
            invImageLine(width,height,pbi.pixels);
            setPourcent((99.*frame)/depth);      
         } catch( Exception e ) { System.err.println("Error on frame "+frame); e.printStackTrace(); }
      }
      
      try { f.close(); } catch( Exception e ) {}
      Date d1=new Date(); long temps = (int)(d1.getTime()-d.getTime()); d=d1;
      Aladin.trace(3," => Full cube contrast adjustement in "+temps+" ms");
      
      buffer=null;
//      permutation(modePerm);
      
      flagOk=true;    
      flagUpdating=false;
      
      changeImgID();
//      sendLog("RecutPixel","["+getLogInfo()+"]");

      setPourcent(-1);
      aladin.view.repaintAll();
      
      // Pour éviter de tuer trop vite le thread si c'est un excité de la souris
      Util.pause(1000);           
      if( _restart )  return recutCube(_min,_max,_autocut);

//System.out.println("End of recut thread");
      getLock();
      setThreadRecut(null);
      setLock(false);
      return true;
   }
   
   private RandomAccessFile fCacheBis=null;
   private String cacheIDBis=null;
   private byte bufCache[]=null;
   
   protected boolean getFromCache() {
      if( pixelsOrigin!=null ) return true;      
      if( oLastFrame!=-1 ) {
         PlanImageBlinkItem pbi = vFrames.elementAt(oLastFrame);
         if( pbi.pixelsOrigin!=null ) { pixelsOrigin = pbi.pixelsOrigin; return true; }
      }
      return super.getFromCache();
   }
   
   /** Libération de toutes les tranches pixelsOrigin du cube encore en mémoire RAM */
   synchronized protected boolean freeRam() { 
      if( loadInRamInProgress ) loadInRamAborting=true;
      return freeRam(-1)>0;
   }
   
   /** Libération de suffisamment de tranches pixelsOrigin du cube pour askMem octets, ou la totalité si -1
    * @return le montant de la mémoire libérée
    */
   synchronized protected long freeRam(long askMem) {
      long mem=0;
      if( vFrames==null ) return 0;
      Enumeration<PlanImageBlinkItem> e = vFrames.elements();
      while( e.hasMoreElements() ) {
         PlanImageBlinkItem pbi = e.nextElement();
         if( pbi.pixelsOrigin==null ) continue;
         mem += pbi.pixelsOrigin.length;
         pbi.pixelsOrigin=null;
         if( askMem!=-1 && mem>askMem ) break;
      }
      if( mem>0 ) aladin.trace(4,"PlanImageBlink.freeRam("+askMem+") ["+label+"] (free "+mem/(1024.*1024)+"MB) ...");
      return mem;
   }
   
   synchronized boolean isFullyInRam(int z1, int d) {
      for( int z=z1; z<depth && z<z1+d; z++ ) { 
         if( vFrames.elementAt(z).pixelsOrigin==null ) return false;
      }
      return true;
   }
   
   private boolean loadInRamInProgress = false;   // true si on est en train de recharger les pixels d'origine d'un cube
   private boolean loadInRamAborting=false;       // true si on demande d'interrompre le chargement des pixels d'origine d'un cube
   
   // return true si le cube est soit déjà en RAM soit a pu être chargé
   synchronized private boolean loadInRam(int z1, int d) {
      if( isFullyInRam(z1,d) ) {
         Aladin.trace(4,"PlanImageBlink.loadInRam("+z1+","+d+"): reloading not required (fully in Ram)");
         return true;  // déjà fait
      }
      try {
         long taille = (long)width*height*d*npix;
         if( taille>= Integer.MAX_VALUE ) return false;  // c'est trop grand pour un tableau de byte[]
         if( loadInRamInProgress ) {
            Aladin.trace(4,"PlanImageBlink.loadInRam("+z1+","+d+"): loading still in progress => no launch new one");
            return false;   
         }
         loadInRamAborting=false;
         loadInRamInProgress=true;
         
         double requiredMo = (double)width*height*d*(npix+1) / (1024.*1024);
         boolean ok = aladin.getMem()-requiredMo > Aladin.MARGERAM;
         if( !ok && aladin.freeSomeRam((long)(requiredMo*1024*1024),this)>0 ) {
            ok = aladin.getMem()-requiredMo > Aladin.MARGERAM;
         }
         Aladin.trace(4,"PlanImageBlink.loadInRam("+z1+","+d+"): ask for "+requiredMo+"Mo : "+(ok ? "enough space => loading...":"Not enough space => ignored"));

         if( !ok ) {
            loadInRamInProgress=false;
            return false;       // Pas assez de place
         }
         
         // On charge les tranches une à une
         int length = width*height*npix;
         for( int z=z1; z<depth && z<z1+d; z++ ) {
            PlanImageBlinkItem pbi = vFrames.elementAt(z);
            if( pbi.pixelsOrigin!=null ) continue;  // Tranche encore en mémoire
            pbi.pixelsOrigin = new byte[length];
            if( cacheIDBis!=pbi.cacheID ) {
               if( fCacheBis!=null ) try { fCacheBis.close();  } catch( Exception e) {}
               fCacheBis = new RandomAccessFile(new File(cacheIDBis=pbi.cacheID),"r");
            }
            if( loadInRamAborting ) throw new Exception("LoadInRam abort by freeRam call");
//            if( z%(depth/10)==0 ) Aladin.trace(4,"PlanImageBlink.loadInRam("+z1+","+d+"): loading in progress (frame "+z+"...)");
            seekAndRead(fCacheBis,pbi.cacheOffset,pbi.pixelsOrigin,0,length);
         }
         loadInRamInProgress=false;
//         Aladin.trace(4,"PlanImageBlink.loadInRam("+z1+","+d+"): done");
         return true;
      } catch( Exception e ) { 
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
      loadInRamInProgress=false;
      return false;
   }
   
   /** Retourne 1 pixel depuis le disque (sert pour les plugins) */
   protected double getPixel(int x, int y, int z) throws Exception {
//      loadInRam();
      PlanImageBlinkItem pbi = vFrames.elementAt(z);
      byte [] pixelsOrigin;
      if( (pixelsOrigin=pbi.pixelsOrigin)!=null ) return getPixVal(pixelsOrigin,bitpix,y*width+x)*bScale+bZero;
      
      if( cacheIDBis!=pbi.cacheID ) {
         if( fCacheBis!=null ) try { fCacheBis.close();  } catch( Exception e) {}
         fCacheBis = new RandomAccessFile(new File(cacheIDBis=pbi.cacheID),"r");
      }
      if( bufCache==null ) bufCache=new byte[npix];
      seekAndRead(fCacheBis,pbi.cacheOffset + npix*(y*width+x),bufCache,0,bufCache.length);
      return getPixVal(bufCache,bitpix,x)*bScale+bZero;
   }
   
   long t,t1;
   
   
   /** Déplacement et lecture atomique pour éviter les problèmes en cas de threading */
   synchronized private void seekAndRead(RandomAccessFile f,long pos,byte buf[],int offset, int length) throws Exception {
      f.seek(pos);
      f.readFully(buf,offset,length);
   }

   /** Extration d'un sous-cube depuis les pixels d'origine, avec conversion
    * en pixels double (sert pour les plugins)
    * return null si problème */
   protected double [][][] getCube(int x, int y, int z, int w, int h, int d) throws Exception {
      return getCube(null,x,y,z,w,h,d);
   }
   protected double [][][] getCube(double [][][] cube,int x, int y, int z, int w, int h, int d) throws Exception {
      if( cube==null ) cube = new double[w][h][d];
      loadInRam(z,d);
      
      // Travaille sur son propre buffer pour ne pas interférer avec pixelsOrigin[]
      byte [] buffer = new byte[w*npix];

      // On charge chaque tranche concernée
      for( int z1=z; z1<z+d; z1++ ) {
         PlanImageBlinkItem pbi = vFrames.elementAt(z1);
         
         // Cool les pixels sont en mémoire
         byte [] pixelsOrigin;
         if( (pixelsOrigin=pbi.pixelsOrigin)!=null ) {
            // On balaye chaque ligne de chaque tranche
            for( int y1=y; y1<y+h; y1++ ) {
               for( int x1=x; x1<x+w; x1++ ) {
                  cube[x1-x][y1-y][z1-z] = getPixVal(pixelsOrigin,bitpix,y1*width+x1)*bScale+bZero;
               }
            }
            
         // Va falloir les chercher sur le disque
         } else {
            if( cacheIDBis!=pbi.cacheID ) {
               if( fCacheBis!=null ) try { fCacheBis.close();  } catch( Exception e) {}
               fCacheBis = new RandomAccessFile(new File(cacheIDBis=pbi.cacheID),"r");
            }

            // On balaye chaque ligne de chaque tranche
            for( int y1=y; y1<y+h; y1++ ) {
               seekAndRead(fCacheBis,pbi.cacheOffset + npix*(y1*width+x),buffer,0,buffer.length);

               for( int x1=0; x1<w; x1++ ) {
                  cube[x1][y1-y][z1-z] = getPixVal(buffer,bitpix,x1)*bScale+bZero;
               }
            }
         }
      }
      
      return cube;
   }

   
   /** Retourne la chaine d'explication de la taille et du codage de l'image
    * d'origine */
   protected String getSizeInfo() {
      return width + "x" + height +" pixels (8bits kept)" ;
   }

   /** Retourne le nombre de Frames */
   protected int getNbFrame() {
      return vFrames==null ?  0 : vFrames.size();
   }
      
   protected String getFrameLabel(int i) {
      if( !active ) return label;
      return vFrames.elementAt(i).label;
   }

   /** Retourne la frame numéro n */
   protected byte[] getFrame(int n) {
      return vFrames.elementAt(n).pixels;
   }
   
   /** Retourne le Pixel x,y de la frame n ATTENTION, SANS DOUTE LENT */
   protected byte getPixel8bit(int n,int x,int y) {
      return vFrames.elementAt(n).pixels[y*width+x];
   }
   
   // POur ne pas recharger tous le temps les pixels courants lorsque l'on déplace
   // simplement la souris sur l'image
   private int ooLastFrame=-1, oLastFrame = -1; 
   
   /** Rend active la tranche courante de pixels (pour un contour...)
    * en profite pour remettre à jour le zoomview
    */
   protected void activePixels(ViewSimple v) {
      if( flagUpdating ) return;
      if( ooLastFrame==v.blinkControl.lastFrame ) return;
      if( v.blinkControl.mode==BlinkControl.PAUSE ) activePixelsOrigin(v);
      else ((PlanImage)v.pref).noOriginalPixels();
      if( oLastFrame==v.blinkControl.lastFrame ) return;
      oLastFrame=v.blinkControl.lastFrame;
      PlanImageBlinkItem pbi = vFrames.elementAt(oLastFrame);
      setBufPixels8(pbi.pixels);
      pixelsOrigin=pbi.pixelsOrigin;
//      if( type==IMAGECUBERGB ) ((PlanRGBInterface)this).calculPixelsZoomRGB();
//      else calculPixelsZoom();
      aladin.calque.zoom.zoomView.resetImgID();
      aladin.calque.zoom.zoomView.repaint();
   }
   
   protected void activePixels(int frame) {
      if( flagUpdating ) return;
      if( oLastFrame==frame ) return;
      initFrame=oLastFrame=frame;
      PlanImageBlinkItem pbi = vFrames.elementAt(oLastFrame);
      setBufPixels8(pbi.pixels);
      pixelsOrigin=pbi.pixelsOrigin;
   }
   
   /** Rend active la tranche courante des pixels d'origine, soit pour le planBlink lui-même
    * soit pourun planImage désigné (dans le cas d'une extraction d'une frame */
   protected void activePixelsOrigin(ViewSimple v) { activePixelsOrigin(v,this); }
   protected void activePixelsOrigin(int frame) { activePixelsOrigin(this,frame); }   
   protected void activePixelsOrigin(ViewSimple v,PlanImage p) { activePixelsOrigin(p,v.blinkControl.lastFrame); }
   
   private void activePixelsOrigin(PlanImage p,int frame) {
      ooLastFrame = frame;
      PlanImageBlinkItem pbi = vFrames.elementAt(frame);
      p.cacheID = pbi.cacheID;
      p.cacheOffset = pbi.cacheOffset;
      p.cacheFromOriginalFile=pbi.cacheFromOriginalFile;
      p.pixelsOrigin=pbi.pixelsOrigin;
   }
   
   /** Extraction d'une portion de l'image.
    * Retourne une portion de l'image sur la forme d'un tableau de pixels
    * @param newpixels Le tableau a remplir (il doit etre assez grand)
    * @param x,y,w,h   Le rectangle de la zone a extraire
    * @param frame le numéro de la frame
    * @param transparency [0..1] niveau de transparence entre la frame courante
    *         et la suivante (ou la première si fin de série), -1 si non demandé
    */
   protected void getPixels(byte[] newpixels, int x, int y, int w, int h,
         int frame,double transparency) {
      int i,j,n,m;
      int k=0;
      int aw, ah; // Difference en abs et ord lorsqu'on depasse l'image

      // Ajustement de la taille en cas de depassement
      aw=ah=0;
      if( x + w > width  ) { aw=x + w - width;  w-=aw; }
      if( y + h > height ) { ah=y + h - height; h-=ah; }

      // Pas de fondu enchainé avec la frame suivante
      if( transparency==-1 || transparency==0 || getNbFrame()==1 ) {
         byte pixels[]=getFrame(frame);
         for( i=y, n=y + h; i < n; i++ ) {
            System.arraycopy(pixels,i * width + x,newpixels,k,w);
            k+=w + aw;
         }
         return;
      }
      
      // Fondu enchainé avec la prochaine frame
      byte p1[] = getFrame(frame);
      byte p2[] = getFrame(frame==getNbFrame()-1 ? 0 : frame+1);
      double complement = 1-transparency;
      
      for( i=y, n=y + h; i<n; i++ ) {
         for( j=x, m=x+w; j<m; j++ ) {
            int q = i*width +j;
            double pix = ((int)p1[q]&0xFF)*complement + ((int)p2[q]&0xFF)*transparency;
            newpixels[k++] = (byte)((int)pix & 0xFF);
         }
         if( aw!=0 ) k+=aw;
      }

   }

}
