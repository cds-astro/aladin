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

import cds.tools.*;

import java.io.*;
import java.util.*;

/**
 * Plan dedie a une image tres grande (IMAGEHUGE)
 *
 * Principe de fonctionnement:
 * Pour économiser du temps de lecture disque et de la RAM, on va charger une image
 * sous-échantillonnée en ne prenant qu'un pixel tous les "step" pixels. C'est cette
 * image qui servira pour toutes les manipulations classiques (pan, contour, superposition)..
 * Lorque l'utilisateur choisira un niveau de zoom élevé, on ira charger une sous-image
 * pleine résolution couvrant le champ de vue courant.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : juin 2007 - création
 */
public class PlanImageHuge extends PlanImage implements Runnable {

   // Dimension max pour une sous-image pleine résolution (par défaut)
   // ON POURRAIT IMAGINER LE FAIRE DEPENDRE DU TEMPS DE CHARGEMENT POUR
   // RESTER EN DECA DE 4 A 6 SECONDES
   static final int LIMIT = 1024*4;

   protected int step;              // Pas du sous-échantillonnage (une puissance de 2)
   protected byte [] pixelsSub;     // Sous-image pleine résolution
   protected int ox,oy,ow,oh;       // coordonnonnées de la pixelsSub dans l'image rééchantillonnée
   private boolean restart;         // flag de demande de relance de l'extraction de pixelsSub[]
   private boolean lock;            // verrou pour le passage des paramètres du thread
   private Thread thread;           // thead d'extraction
   protected boolean isExtracting;  // true si le thread d'extraction est running
   private boolean toSubImage;      // flag pour pouvoir surcharger la méthode run() sans mettre le bouz

   /** Creation d'un plan de type IMAGEHUGE (via un stream)
    * @param in le stream
    */
   protected PlanImageHuge(Aladin aladin,String file,MyInputStream in,String label,String from,
         Obj o,ResourceNode imgNode,boolean skip,boolean doClose,Plan forPourcent) {
      super(aladin,file,in,label,from,o,imgNode,skip,doClose,forPourcent);
      type=IMAGEHUGE;
   }

   protected boolean isSync() {
      return super.isSync() && !isExtracting;
   }

   /** Retourne le pas d'échantillonnage */
   protected int getStep() { return step; }

   /** Retourne true si on doit extraire une sous-image pleine résolution. Ceci dépend
    * du champ de vue (wview x hview), du facteur de zoom courant
    * et du pas du sous-échantillonnage. Le but est d'éviter de charger des portions
    * pleines résolutions inutilement grosses (voir createZoomPixels());
    */
   protected boolean fromSubImage(double zoom, int wview, int hview) {
      return  (wview*step)/zoom < LIMIT && (hview*step)/zoom < LIMIT;
   }

   /** Extrait les pixels de la sous-image pleine résolution repérés par le rectangle
    * indiqué en paramètre (coord. de l'image sous-échantillonnée)
    */
   protected byte [] cropPixels(int x,int y,int w, int h) {
      int xa = (x-ox)*step;
      int ya = (y-oy)*step;
      int wa = w*step;
      int ha = h*step;
      byte [] pixels = new byte[wa * ha];

      //System.out.println("cropSubImage "+x+","+y+" "+w+"x"+h+" =>"+xa+","+ya+" "+wa+"x"+ha);
      getPixels(pixels,pixelsSub,ow*step,oh*step, xa,ya,wa,ha);
      return pixels;
   }

   /** Demande le chargement d'une sous-image pleine résolution contenant l'image
    * x,y w*h. On va prendre un peu plus grand que nécessaire
    * afin d'anticiper un éventuelle déplacement.
    */
   protected boolean loadSubImage(int x,int y,int w,int h) {
      int gap = 128/step;
      //int ax=x,ay=y,aw=w,ah=h;
      x-=gap; y-=gap; w+=2*gap; h+=2*gap;
      if( x<0 ) x=0;
      if( y<0 ) y=0;
      if( (x+w)*step>=naxis1 ) w = (naxis1/step)-x-1;
      if( (y+h)*step>=naxis2 ) h = (naxis2/step)-y-1;
      //System.out.println(ax+","+ay+" "+aw+"x"+ah+" => "+x+","+y+" "+w+"x"+h);
      return getSubImage(x,y,w,h);
   }

   /** Demande le verrou pour positionné des paramètres du thread */
   private void waitLock() {
      while( !getLock() ) Util.pause(10);
   }

   /** Tentative pour récupérer le lock */
   synchronized private boolean getLock() {
      if( lock ) return false;
      lock=true;
      return true;
   }

   /** Positionne le verrou de passage des paramètres du thread */
   synchronized private void unlock() {
      lock=false;
      //      System.out.println("setLock("+flag+") "+Thread.currentThread());
   }

   /** Retourne true si la portion indiquée est incluse dans une sous-image pleine résolution.
    * Les coord et les tailles sont données dans l'image sous-échantillonnée
    * @param x,y coin haut gauche (référence image sous-échantillonnée)
    * @param w,h dimension
    */
   protected boolean inSubImage(int x, int y, int w, int h){
      return selectPixelsSub(x,y,w,h);
   }

   /** Permet de mémoriser une liste de buffers à pleine résolution */
   private PixelsSub vPixelsSub[] = new PixelsSub[ViewControl.MAXVIEW+1];
   private int nbPixelsSub=0;        // Taille utilisée de vPixelsSub
   private int nextPixelsSub=0;      // Prochaine case à utiliser dans vPixelsSub

   // Un élément de vPixelsSub
   class PixelsSub {
      int ox,oy,ow,oh;
      byte []pixels;

      PixelsSub(byte pixelsSub[],int x,int y,int w,int h) {
         pixels=pixelsSub;
         ox=x; oy=y;
         ow=w; oh=h;
      }

      boolean agree(int x,int y,int w,int h) {
         return  x>=ox && y>=oy && x+w<=ox+ow && y+h<=oy+oh;
      }
   }

   /** Permet d'ajouter un buffer pleine résolution à la liste
    * @return l'indice dans le tableau vPixelsSub[]
    */
   private int addPixelsSub(byte pixelsSub[],int x,int y,int w,int h) {
      synchronized( vPixelsSub ) {

         if( nbPixelsSub<vPixelsSub.length ) nextPixelsSub=nbPixelsSub++;
         else {
            // Trouve la prochaine case à utiliser (non utilisé par un plan)
            nextPixelsSub=0;
            for( int i=0; i<vPixelsSub.length; i++ ) {
               PixelsSub px = vPixelsSub[i];
               for( int j=0; j<aladin.view.getModeView(); j++ ) {
                  ViewSimple v = aladin.view.viewSimple[j];
                  if( v.isFree() || v.pref!=this || !v.flagHuge ) continue;
                  if( px.agree(v.xHuge,v.yHuge,v.wHuge,v.hHuge) ) continue;
                  nextPixelsSub=j;
                  break;
               }
            }
         }

         vPixelsSub[nextPixelsSub]=new PixelsSub(pixelsSub,x,y,w,h);
         //System.out.println("Ajouté pixelsSub["+nextPixelsSub+"] => "+x+","+y+" "+w+"x"+h);
         return nextPixelsSub;
      }
   }

   private void resetPixelSub() {
      synchronized( vPixelsSub ) {
         for( int i=0; i<nbPixelsSub; i++ ) vPixelsSub[i]=null;
         nbPixelsSub=0;
      }
   }

   /** Permet de position le buffer pleine résolution courant s'il existe dans
    * la liste. Retourne false sinon */
   private boolean selectPixelsSub(int x,int y,int w,int h) {
      synchronized( vPixelsSub ) {
         for( int i=0; i<nbPixelsSub; i++ ) {
            PixelsSub px = vPixelsSub[i];
            if( px.agree(x,y,w,h) ) {
               pixelsSub=px.pixels;
               ox=px.ox; oy=px.oy;
               ow=px.ow; oh=px.oh;
               //System.out.println("Trouvé pixelsSub["+i+"] => "+x+","+y+" "+w+"x"+h);
               return true;
            }
         }
         return false;
      }
   }


   // Paramètres à passer au thread getSubImageThread()
   private int _x,_y,_w,_h;

   /** Lance l'extraction d'une sous-image pleine résolution indiquée par le rectangle
    * passé en paramètre (coord de l'image sous-échantillonnée).
    * Fait appel à un thread dédié, ou demande à ce thread de recommencer une
    * nouvelle extraction s'il n'a pas encore terminé.
    */
   private boolean getSubImage(int x, int y, int w, int h) {
      if( inSubImage(x,y,w,h) ) return true;
      flagUpdating=true;
      aladin.calque.select.repaint();
      waitLock();
      _x=x; _y=y; _w=w; _h=h;;
      if( isExtracting )  restart=true;
      else {
         toSubImage=true;
         thread = new Thread(this,"HugeSubImage");
         thread.start();
      }
      return false;
   }

   /** Lancement du thread d'extraction. Le flag toSubImage permet de ne pas s'emméler
    * les pinceaux avec le run() de la classe PlanImage
    */
   public void run() {
      if( !toSubImage ) { super.run(); return; }
      toSubImage=false;
      //System.out.println("Start getSubImageThread()... thread "+Thread.currentThread());
      isExtracting=true;
      aladin.calque.select.repaint();
      getSubImageThread();
   }

   // Buffers de travails
   private byte [] pixelsWork;
   private byte [] buf;

   /** Méthode du thread d'extraction d'une sous-image pleine résolution. Peut-être a tout
    * moment relancé si le flag "restart" passe à true. Le cut se fait immédiatement dans
    * cette méthode (pas d'appel à to8bits()) car on connait déjà tous les paramètres
    * nécessaires à l'opération (minPixCut, maxPixCut...). C'est la variable "isReady" qui
    * détermine si l'image est prête. On travaille sur un tableau temporaire "pixelsWork[]"
    * afin de ne pas couper l'herbe sous les pieds de la classe d'affichage (ViewSimple)
    */
   private void getSubImageThread() {
      int x =_x,y=_y,w=_w,h=_h;
      ox=x; oy=y; ow=w; oh=h;
      restart=false;
      unlock();
      long t = System.currentTimeMillis();
      double r = 255./(pixelMax - pixelMin);
      try {
         int size = w*h*step*step;
         pixelsWork = new byte[size];
         buf = new byte[w*step*npix];
         int len = w*step;
         int pos=0;

         for( int i=naxis2-(y+h)*step; i<naxis2-y*step; i++ ) {
            fCache.seek( cacheOffset + (i*(long)naxis1 + x*step) * npix );
            fCache.readFully(buf);

            for( int j=0; j<len; j++ ) {
               double c = getPixVal(buf,bitpix,j);
               if( Double.isNaN(c) || isBlank && c==blank ) { pixelsWork[pos++] = 0; continue; }
               pixelsWork[pos++] = (byte)( 1+ (c<=pixelMin?0x00:c>=pixelMax?0xfe
                     :(int)( ((c-pixelMin)*r)) ) & 0xff);
            }

            if( restart ) {
               restart=false;
               //System.out.println("Restart getSubImageThread()... thread="+Thread.currentThread());
               getSubImageThread();
               return;
            }
         }
         isExtracting=false;
         invImageLine(w*step,h*step,pixelsWork);
         pixelsSub=pixelsWork;

         int n=addPixelsSub(pixelsSub,ox,oy,ow,oh);
         Aladin.trace(3,"getSubImage["+n+"] from "+label+" ("+x*step+","+y*step+" "+w*step+"x"+h*step+") in "+(System.currentTimeMillis()-t)+"ms");
         nextImgID();
      } catch( Exception e ) { e.printStackTrace(); }
      flagUpdating=false;
      aladin.view.repaintAll();
   }

   /** Retourne le pixel 8 bit repéré par la coordonnée x,y full résolution. Si L'imagette
    * full résolution à été chargée, prend la valeur dans cette image, sinon dans
    * l'image basse résolution */
   public int getPixel8(int x,int y) {
      if( inSubImage(x/step,y/step,1,1) ) return pixelsSub[(y-oy*step)*ow*step + (x-ox*step)] & 0xFF;
      return getBufPixels8()[(y/step)*width+x/step];
   }

   /**
    * Retourne sous forme d'une chaine éditable
    * la valeur du pixel suivant le mode courant (PIXEL,INFILE,REAL)
    * et de la position (x,y) dans l'image full résolution
    */
   protected String getPixelInfo(int x,int y,int mode) {
      if( !flagOk || y<0 || y>=naxis2 || x<0 || x>=naxis1 ) return "";
      switch(mode) {
         case View.LEVEL:
            byte b;
            if( inSubImage(x/step,y/step,1,1) ) b=pixelsSub[(y-oy*step)*ow*step + (x-ox*step)];
            else b=getBufPixels8()[(y/step)*width+x/step];
            return Util.align3(b & 0xFF)+" / 255";
         case View.INFILE:
            if( onePixelOrigin==null ) onePixelOrigin = new byte[npix];
            if( !getOnePixelFromCache(onePixelOrigin,npix,x,y) ) return UNK;
            return Y(getPixVal(onePixelOrigin,bitpix,0));
         case View.REAL:
            if( onePixelOrigin==null ) onePixelOrigin = new byte[npix];
            if( !getOnePixelFromCache(onePixelOrigin,npix,x,y) ) return UNK;

            //             return Y(getPixVal(onePixelOrigin,bitpix,0)*bScale+bZero);
            double val = getPixVal(onePixelOrigin,bitpix,0)*bScale+bZero;
            if( aladin.levelTrace<4 ) return Y(val);
            double infileVal=getPixVal1(onePixelOrigin,bitpix,0);
            return Y(val)+(Double.isNaN(infileVal) || val!=infileVal?"("+infileVal+")":"")+(isBlank && infileVal==blank ? " BLANK":"");

      }
      return null;
   }
   /**
    * Rejoue l'autocut en fonction d'un min et d'un max donnes
    * par l'utilisateur.
    * @param min pixel min
    * @param max pixel max
    * @param autocut true s'il faut appliquer l'algo d'autocut, sinon effectue
    *                simplement un changement d'echelle de [min..max] vers
    *                les 256 niveaux de gris
    * @return false si impossible de recharger les pixels d'origine
    */
   protected boolean recut(double min,double max,boolean autocut) {

      if( min==-1 && max==-1 ) { min=dataMinFits; max=dataMaxFits; }

      // Juste pour faire clignoter
      flagUpdating=true;
      flagOk=false;
      aladin.calque.select.repaint();

      int xc=0, yc=0, wc=0;

      if( autocut && Projection.isOk(projd) ) {
         Coord c = new Coord(aladin.view.repere.raj,aladin.view.repere.dej);
         projd.getXY(c);
         xc = (int)c.x;
         yc = (int)c.y;
         ViewSimple v = aladin.view.getCurrentView();
         if( v.pref==this ) {
            try {
               wc = (int)( v.getTaille()/projd.getPixResDelta() )/2 ;
            } catch( Exception e ) { }
         }
      }


      try {
         int w=Math.min(1024,width);
         int h=Math.min(1024,height);
         int x = width/2  -w/2;
         int y = height/2 -h/2;

         if( wc>0 ) {
            w = h = Math.min(1024, wc);
            x = xc-w/2;
            y = yc-w/2;
         }

         byte buf[] = new byte[w*h*npix];
         getPixelsFromCache(buf,npix,x,y,w,h);
         findMinMax(buf,bitpix,w,h,min,max,autocut,0,0,0,0);
         min=pixelMin; max=pixelMax;

         // Chargement de l'image
         loadHugeImage();

         // Reset des imagettes pleines résolutions
         resetPixelSub();

      } catch( Exception e ) { e.printStackTrace(); return false; }

      setPourcent(-1);
      ow=-1;           // Pour forcer le rechargement de la subimage le cas échéant
      flagUpdating=false;
      flagOk=true;
      freeHist();

      return true;
   }

   /** Lecture et rééchantillonnage d'une image Huge */
   private void loadHugeImage() throws Exception {
      int h,w;
      byte buf[] = new byte[512];
      long ntaille = (long)width*height* npix;
      int pos=0;
      offsetLoad=0;        // octets effectivement lus

      try {
         for( h=0; h<height; h++ ) {
            for( w=0; w<width; w++ ) {
               fCache.seek(cacheOffset+((h*step+step/2)*(long)naxis1+(w*step+step/2))*npix);
               fCache.readFully(buf,pos,npix);
               pos+=npix;

               if( pos==buf.length ) {
                  to8bits(getBufPixels8(),offsetLoad/npix,buf,pos/npix,bitpix,
                        /*isBlank,blank,*/pixelMin,pixelMax,true);
                  offsetLoad+=pos;
                  pos=0;
                  setPourcent(offsetLoad*99./ntaille);
               }
            }
         }
      } catch( Exception e ) {
         //          if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
      if( pos>0 ) {
         to8bits(getBufPixels8(),offsetLoad/npix,buf,pos/npix,bitpix,
               /*isBlank,blank,*/pixelMin,pixelMax,true);
         offsetLoad+=pos;
         setPourcent(offsetLoad*99./ntaille);
      }

      invImageLine(width,height,getBufPixels8());
   }


   /** Préparation d'une image sous-échantillonnée. On va créer une image plus petite
    * que celle stockée sur le disque en ne prenant qu'un pixel tous les "step" pixels.
    * On travaille directement sur le fichier FITS d'origine (fCache) ou éventuellement le
    * cache afin de pouvoir faire des seek()
    */
   protected boolean cacheImageFits(MyInputStream dis) throws Exception {

      int naxis = 2;
      int i;
      long taille;		// nombre d'octets a lire
      int n;			// nombre d'octets pour un pixel

      Aladin.trace(2,"Loading Huge FITS image");

      // Lecture de l'entete Fits si ce n'est deja fait
      if( headerFits==null ) headerFits = new FrameHeaderFits(this,dis);

      bitpix = headerFits.getIntFromHeader("BITPIX");
      naxis = headerFits.getIntFromHeader("NAXIS");

      // Il s'agit juste d'une entête FITS indiquant des EXTENSIONs
      if( naxis<=1 && headerFits.getStringFromHeader("EXTEND")!=null ) {
         error="_HEAD_XFITS_";

         // Je saute l'éventuel baratin de la première HDU
         if( naxis==1 ) {
            try {
               naxis1 = headerFits.getIntFromHeader("NAXIS1");
               dis.skip(naxis1);
            } catch( Exception e ) { e.printStackTrace(); }
         }

         return false;
      }

      width = naxis1 = headerFits.getIntFromHeader("NAXIS1");
      height = naxis2 = headerFits.getIntFromHeader("NAXIS2");
      npix = n = Math.abs(bitpix)/8;

      taille=(long)width*height*n;	// Nombre d'octets
      setPourcent(0);
      Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" BITPIX="+bitpix+" => size="+taille);

      // Les paramètres FITS facultatifs
      loadFitsHeaderParam(headerFits);

      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      // Accès au fichier en local obligatoirement (dump si gzip ou remote)
      if( !setCacheFromFile(dis) ) {
         aladin.trace(2,"Dumping huge image in local cache...");
         int len = 512;
         byte [] buffer = new byte[len];
         long offsetLoad=0;      // octets effectivement lus
         RandomAccessFile f=null;
         try {
            while( offsetLoad<taille) {
               if( taille-offsetLoad<len ) len=(int)(taille-offsetLoad);
               dis.readFully(buffer,0,len);
               if( offsetLoad==0 ) f=beginInCache(buffer);
               else f.write(buffer,0,len);
               offsetLoad+=len;
               setPourcent(offsetLoad*85./taille);
            }
            fCache=f;
         } catch( Exception e ) {
            error=aladin.error="Loading error: "+e.getMessage();
            e.printStackTrace();
            close();
            return false;
         }
      }

      // Allocation du buffer d'arrivée
      tailleLoad=taille;	// nombres d'octets a lire
      boolean cut = aladin.configuration.getCMCut();

      // Lecture directe d'un bloc central de l'image de 1024*1024 pour détermination min/max
      int w=Math.min(1024,width);
      int h=Math.min(1024,height);
      int x = width/2  -w/2;
      int y = height/2 -h/2;
      byte buf [] = new byte[w*h*npix];
      getPixelsFromCache(buf,npix,x,y,w,h);
      findMinMax(buf,bitpix,w,h,dataMinFits,dataMaxFits,cut,0,0,0,0);

      int len=512;		// Taille des blocs - Optimisation pour la lecture
      buf = new byte[len];

      // Calcul du pas de la grille du sous-échantillonnage (une puissance de 2)
      int mx = Math.max(width,height);
      for( step=1; mx/step>2048; step*=2);

      width = top((double)width/step);
      height = top((double)height/step);
      Aladin.trace(3,"Huge image ("+naxis1+"x"+naxis2+") step="+step+" => ("+width+"x"+height+")");

      // Chargement de l'image
      setBufPixels8(new byte[width*height]);
      loadHugeImage();

      // On se recale si jamais il y a encore une extension FITS qui suit
      if( naxis>2 ) {
         try { dis.skip( taille ); }
         catch( Exception e ) { e.printStackTrace(); return false; }
      }

      // Dans le cas d'un MEF dont on skippe l'image, on peut sortir tout de suite
      if( flagSkip ) return true;

      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
      Aladin.trace(3," => Reading "+(cut?"and autocutting ":"")+"in "+Util.round(temps/1000.,3)+" s => "+Util.round(((double)offsetLoad/temps)/(1024*1.024),2)+" Mbyte/s");

      // On calcule l'imagette du zoom
      calculPixelsZoom();

      creatDefaultCM();
      setPourcent(99);
      return true;
   }
}

