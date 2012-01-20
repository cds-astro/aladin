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

import cds.tools.Util;

import java.awt.*;
import java.awt.image.*;
import java.io.FileInputStream;
import java.net.*;
import java.util.*;


/**
 * Gestion d'un plan image RGB
 *
 * @author Anais Oberto + Pierre Fernique [CDS]
 * @version 1.3 : février 2006 support des images FITS couleurs
 * @version 1.2 : fevrier 2002 deduction automatique du plan de reference
 * @version 1.1 : janvier 2002 ajustements pour la version officielle (P.Fernique)
 * @version 1.0 : decembre 2001 creation
 */
public class PlanImageRGB extends PlanImage implements PlanRGBInterface {

   protected int [] pixelsRGB;        // Tableau des pixels de l'image (sur 4x8 bits)
   protected int [] pixelsZoomRGB; // Tableau des pixels de l'imagette sur 4*8 bits
   protected byte [] red;        // Tableau des pixels de l'image Rouge
   protected byte [] green;        // Tableau des pixels de l'image Verte
   protected byte [] blue;        // Tableau des pixels de l'image Bleue
   protected boolean twoColors;
   protected int RGBCONTROL[] = { 0,128, 255 , 0,128, 255 , 0,128, 255 };
   protected int RGBControl[];
   protected int [] pi = new int[3];
   protected String [] labels = new String[3];
   protected String labelRed,labelGreen,labelBlue; // Temporaires lors du chargement via un fichier AJ
   protected PlanImage planRed,planGreen,planBlue,pRef; // Les plans d'origines
   protected boolean flagRed,flagGreen,flagBlue;  // true si la composante est donnée
   protected boolean diff;	// true s'il s'agit d'une difference sur 2 plans

   private boolean mustResample=false;	// true si on l'image RGB provient d'une construction Aladin (voir waitForPlan())
   
   // Pour pouvoir recharger du AJ
   protected PlanImageRGB(Aladin aladin) { 
      super(aladin);
      type=IMAGERGB;
   }

   /** Creation d'un plan de type IMAGE (via 2 ou 3 autres plans)
   * @param r    Le plan correspondant a la bande rouge
   * @param g    Le plan correspondant a la bande verte
   * @param b    Le plan correspondant a la bande bleue
   * @param ref  Le plan de reference pour le reechantillonage
   * @param label
   * @param d    true s'il s'git d'une différence
   */
   protected PlanImageRGB(Aladin aladin, PlanImage r, PlanImage g,
                          PlanImage b, PlanImage ref, String label, boolean d) {
      super(aladin);
      mustResample=true;  // Pour que le waitForPlan() face le resampling
      type=IMAGERGB;
      isOldPlan=false;

      planRed=r; planGreen=g; planBlue=b;
      flagRed=planRed!=null;
      flagGreen=planGreen!=null;
      flagBlue=planBlue!=null;

      // Determination du plan de reference si ce n'est pas indique
      double x=Double.MAX_VALUE;
      if( ref==null ) {
         if( !Projection.isOk(r.projd) || !Projection.isOk(r.projd) 
               || !Projection.isOk(r.projd) ) ref= r!=null ? r : g!=null ? g:b;
         else {
            if( r!=null &&  Math.abs(r.projd.c.incA)<x ) { ref=r; x=Math.abs(r.projd.c.incA); }
            if( g!=null &&  Math.abs(g.projd.c.incA)<x ) { ref=g; x=Math.abs(g.projd.c.incA); }
            if( b!=null &&  Math.abs(b.projd.c.incA)<x ) { ref=b; x=Math.abs(b.projd.c.incA); }
         }
      }

      pRef=ref;
      diff=d;

      // Initialisation des parametres communs
      init(label,pRef);

      synchronized( this ) {
         runme = new Thread(this,"AladinBuildRGB");
         Util.decreasePriority(Thread.currentThread(), runme);
//         runme.setPriority( Thread.NORM_PRIORITY -1);
         runme.start();
      }
    }
   
//   public PlanImageRGB(Aladin aladin, String fRed, double [] minMaxRed, 
//                                         String fGreen, double [] minMaxGreen, 
//                                         String fBlue, double [] minMaxBlue) throws Exception {
//      super(aladin);
//      type=IMAGERGB;
//      
//      planRed   = fRed==null ? null : new PlanImage(aladin,fRed);
//      planGreen = fGreen==null ? null : new PlanImage(aladin,fGreen);
//      planBlue  = fBlue==null ? null : new PlanImage(aladin,fBlue);
//      flagRed = planRed!=null;
//      flagGreen = planGreen!=null;
//      flagBlue = planBlue!=null;
//      pRef=(planRed!=null)?planRed:(planGreen!=null)?planGreen:planBlue;
//      if( flagRed && minMaxRed!=null ) planRed.recut(minMaxRed[0], minMaxRed[1], false);
//      if( flagGreen && minMaxGreen!=null ) planGreen.recut(minMaxGreen[0], minMaxGreen[1], false);
//      if( flagBlue && minMaxBlue!=null ) planBlue.recut(minMaxBlue[0], minMaxBlue[1], false);
//      if( !flagRed ) pRef=planGreen;
//      diff=false;
//      init("RGB",pRef);
//      mustResample=true;
//      waitForPlan();
//   }

   protected PlanImageRGB(Aladin aladin, String file,URL u,MyInputStream inImg, ResourceNode imgNode) {
      this(aladin,file,u,inImg);
      this.imgNode = imgNode;
   }

   protected PlanImageRGB(Aladin aladin, String file,URL u,MyInputStream inImg) {
      super(aladin,file,inImg);
      if( u!=null ) this.u = u;	// C'est pas beau hein ?! En fait u est modifié comme pour un fichier dans super(), faut bien lui remettre les idées en place
      type=IMAGERGB;
      active=true;
      flagRed=flagGreen=flagBlue=true;
      initCMControl();
      labels[0]="red";labels[1]="green";labels[2]="blue";
      flagOk=false;
   }

   protected PlanImageRGB(Aladin aladin,
                          MyInputStream inImg,
                          int orig, URL u,
                          String label,String objet,
                          String param, String from,
                          int fmt,int res,
                          Obj o, ResourceNode imgNode) {
      super(aladin,inImg,orig,u,label,objet,param,from,fmt,res,o,imgNode);
      type=IMAGERGB;
      active=true;
      flagRed=flagGreen=flagBlue=true;
      initCMControl();
      labels[0]="red";labels[1]="green";labels[2]="blue";
   }
   
   protected void initCMControl() {
      RGBControl = new int[RGBCONTROL.length];
      for( int i=0; i<RGBCONTROL.length; i++) RGBControl[i] = RGBCONTROL[i];
   }

   // Pour pouvoir faire une copie
   protected PlanImageRGB(Aladin aladin,PlanImage p) {
      super(aladin,p);
      type=IMAGERGB;
   }
   
   public int [] getPixelsRGB() { return pixelsRGB; }
   public int [] getPixelsZoomRGB() { return pixelsZoomRGB; }

   protected void copy(Plan p1) {
      super.copy(p1);
      if( !(p1 instanceof PlanImageRGB) ) return;
      PlanImageRGB p = (PlanImageRGB)p1;
      p.pixelsRGB = getPixelsRGB();
      p.red = red;
      p.green = green;
      p.blue = blue;
      p.twoColors = twoColors;
      p.RGBControl = new int[RGBCONTROL.length];
      System.arraycopy(RGBControl, 0, p.RGBControl, 0, RGBControl.length);
      p.pi = new int[ pi.length ];
      System.arraycopy(pi, 0, p.pi, 0, pi.length);
      p.planRed=planRed;
      p.planGreen=planGreen;
      p.planBlue=planBlue;
      p.labels = labels;
      p.flagRed=flagRed;
      p.flagGreen=flagGreen;
      p.flagBlue=flagBlue;
      p.diff=diff;
      p.mustResample=mustResample;
      p.cm = cm;
   }

   protected boolean crop(double x,double y, double w, double h,boolean repaint) {

      // En cas de hors image
      int test=0;
      if( x<=0 ) { w +=x; x=0; test++; }
      if( y<=0 ) { h +=y; y=0; test++; }
      if( x+w>=width ) { w = width-x; test++; }
      if( y+h>=height ) { h = height-y; test++; }

      // inutile, le crop couvre toute l'image
      if( test==4 ) return false;

      // Extraction des pixelsRGB
      int [] npixelsRGB = new int[(int)(w*h)];
      for( int j=0; j<h; j++ ) {
         int srcPos = (int)( (y+j)*width+x );
         int destPos = (int)( j*w );
         System.arraycopy(pixelsRGB, srcPos, npixelsRGB, destPos, (int)w);
      }
      pixelsRGB = npixelsRGB;

      // On perd la référence aux pixels d'origine (A VOIR ?)
      red = green = blue = null;
//      flagRed = flagGreen = flagBlue = false;

      crop1(x,y,w,h,repaint);
      return true;
   }


   protected boolean cacheImageFits(MyInputStream dis) throws Exception {

      int i;
      int taille;		// nombre d'octets a lire
      int n;			// nombre d'octets pour un pixel
      boolean isARGB = (dis.getType() & MyInputStream.ARGB) != 0;

Aladin.trace(2,"Loading "+(isARGB?"A":"")+"RGB FITS image");

      // Lecture de l'entete Fits si ce n'est deja fait
      if( headerFits==null ) headerFits = new FrameHeaderFits(dis);

      bitpix = headerFits.getIntFromHeader("BITPIX");
      if( bitpix!=8 && !isARGB ) aladin.command.printConsole("RGB BITPIX!=8 => autocutting each color component !\n");
      naxis1=width = headerFits.getIntFromHeader("NAXIS1");
      if (width  <= 0) return false;
      naxis2=height = headerFits.getIntFromHeader("NAXIS2");
      if (height  <= 0) return false;
      npix = Math.abs(bitpix)/8;
      taille=width*height*3;	// Nombre d'octets
      setPourcent(0);
Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" NAXIS3=3 BITPIX="+bitpix+" => size="+taille);

      // Pour des stats
      Date d = new Date();
      Date d1;
      int temps;

      video = VIDEO_NORMAL;
      red = new byte[width*height];
      green = new byte[width*height];
      blue = new byte[width*height];
      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
      Aladin.trace(3," => Allocating ("+taille+"b) in "+temps+" ms");
      
      // Mode ARGB => les valeurs des composantes sont rangées dans un entier 32 bits en mode ARGB
      if( isARGB ) {
         byte [] buf = new byte[width*4];   // On lira ligne par ligne
         int offset=0;                      // position dans le bloc de pixel
         i=0;                               // position dans l'image
         while( offset<width*height*4 ) {
            dis.readFully(buf);
            offset+=buf.length;
            setPourcent((( offset/(width*height*4)) )*0.8);
            for( int j=0; j<buf.length; j++,i++) {
               final int mod = i%4;
               int pos = i/4;
               switch(mod) {
                  case 1: red[pos]  =buf[j]; break; 
                  case 2: green[pos]=buf[j]; break; 
                  case 3: blue[pos] =buf[j]; break; 
               }
            }
         }
         buf=null;
         
      // mode RGB => les valeurs des composantes sont rangées dans trois tableaux consécutifs R, G et B
      } else {
         
         byte [] buf;
         for( i=0; i<3; i++ ) {
            buf = i==0 ? red : i==1 ? green : blue;
            readColor(buf,dis,width,height,bitpix);
            setPourcent(pourcent+33);
         }
      }

      d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
Aladin.trace(3," => Reading in "+temps+" ms");

      // Retournement des images (les lignes ne sont pas rangees dans le meme ordre
      // en FITS et en JAVA)
      invImageLine(width,height,red);
      invImageLine(width,height,green);
      invImageLine(width,height,blue);

      createImgRGB();

      cm = ColorModel.getRGBdefault();
      setPourcent(99);
      return true;
   }
   
   // Lecture d'une couleur
   private void readColor(byte [] pOut, MyInputStream dis, int width, int height, int bitpix) throws Exception {
      if( bitpix==8 ) dis.readFully(pOut);
      else {
         int taille = width*height * Math.abs(bitpix)/8;
         byte [] pIn = new byte[taille];
         dis.readFully(pIn);
         getPix8Bits(pOut,pIn,bitpix,width,height,0.,0.,true);
         pIn=null;
      }
   }

   /**
    * Calcul les pixels de l'imagette pour le ZoomView en prenant le pixel au plus proche
    * C'est très rapide et le rendu visuel est quasi le même que par interpolation
    */
   public void calculPixelsZoomRGB() { pixelsZoomRGB = calculPixelsZoomRGB1(pixelsZoomRGB,pixelsRGB,width,height); }
   
   static public int [] calculPixelsZoomRGB1(int [] pixelsZoomRGB,int [] pixelsRGB,int width,int height) {
      // calcul du rapport Largeur/Hauteur de l'image
      int W = ZoomView.SIZE;
      int H = (int)(((double)ZoomView.SIZE/width)*height);
      if( H>W ) {
         W = (int)((double)W*W / (double)H);
         H = ZoomView.SIZE;
      }

      double fctX = (double)width/W;
      double fctY = (double)height/H;

      if( pixelsZoomRGB==null ) pixelsZoomRGB = new int[ZoomView.SIZE*ZoomView.SIZE];
      else for( int i=0; i<pixelsZoomRGB.length; i++ ) pixelsZoomRGB[i]=0;

      for( int y=0; y<H; y++ ) {
         int i = y*ZoomView.SIZE;
         int j = (int)(y*fctY);
         for( int x=0; x<W; x++ ) {
            pixelsZoomRGB[i++] = pixelsRGB[ j*width + (int)(x*fctX) ];
         }
      }
      return pixelsZoomRGB;
   }

   synchronized void changeImgID() { super.changeImgID(); pixelsZoomRGB=null; }

   /** Conversion des composantes de l'image couleur  en niveau de gris
    * en vue d'une conversion en image non couleur */
   protected byte[] getGreyPixels() {
      byte []pix = new byte[pixelsRGB.length];
      for( int i=0; i<pixelsRGB.length; i++ ) {
         pix[i] = (byte) getGreyPixel(pixelsRGB[i]);
      }
      return pix;
   }
   
   /** Conversion en double niveau de gris d'un pixel ARGB */
   static public double getGreyPixel(int pixRGB) {
      int red   = getPixRGB(pixRGB, 0);
      int green = getPixRGB(pixRGB, 1);
      int blue  = getPixRGB(pixRGB, 2);
      return red*0.299  + green*0.587 + blue*0.114;
   }
   
   /** Retourne la valeur du pixel en double (niveau de gris issu des 3 composantes RGB */
   protected double getPixelInDouble(int x,int y) {
      int pixelRGB = pixelsRGB[ (height-y-1)*width+x ];
      return getGreyPixel(pixelRGB);
   }

   /** Retourne la chaine d'explication de la taille et du codage de l'image
    * d'origine */
   protected String getSizeInfo() {
      return width + "x" + height +"x3 pixels" ;
   }

//   /** Crée les tableaux des composantes en fonction de l'image courante */
//   protected void createRGB() { }
//
//   /** Retourne la composante rouge initiale */
//   protected byte[] getRed() { return red; }
//
//   /** Retourne la composante bleue initiale */
//   protected byte[] getBlue() { return blue; }
//
//   /** Retourne la composante verte initiale */
//   protected byte[] getGreen() { return green; }
//
   /** Crée les tableaux des composantes en fonction de l'image courante */
   protected void createRGB() {
      if( pixelsRGB==null ) return;  // Sera fait dans l'autre sens reg+green+blue=>pixelsRGB
      if( red!=null && red.length==pixelsRGB.length ) return;       // Déjà fait
      int size = pixelsRGB.length;
      red   = new byte[size];
      green = new byte[size];
      blue  = new byte[size];
      for( int i=0; i < size; i++ ) {
         red[i]  =(byte) ( (pixelsRGB[i]>>16) & 0xFF);
         green[i]=(byte) ( (pixelsRGB[i]>> 8) & 0xFF);
         blue[i] =(byte) (  pixelsRGB[i]      & 0xFF);
      }
   }

   /** Crée un tableau de bytes avec les composantes RGB de chaque pixel sans la composante alpha */
   protected byte [] getByteRGB() {
      if( pixelsRGB==null ) return null;  // Sera fait dans l'autre sens reg+green+blue=>pixelsRGB
      int size = pixelsRGB.length*3;
      byte [] rgb = new byte[size];
      for( int i=0; i < size; i+=3 ) {
         int pix = pixelsRGB[i/3];
         rgb[i]  =(byte) ( (pix>>16) & 0xFF);
         rgb[i+1]=(byte) ( (pix>> 8) & 0xFF);
         rgb[i+2] =(byte) ( pix      & 0xFF);
      }
      return rgb;
   }

   /** Utilise un tableau de bytes avec les composantes RGB de chaque pixel sans la composante alpha 
    * pour créer le tableau pixelRGB[] */
   protected void setByteRGB(byte [] rgb) {
      int size = rgb.length;
      pixelsRGB = new int[size/3];
      for( int i=0; i < size; i+=3 ) {
         pixelsRGB[i/3]  = (((int)rgb[i] & 0xFF) <<16) | (((int)rgb[i+1] & 0xFF) <<8) | ((int)rgb[i+2] & 0xFF);
      }
   }

   /** Extration d'une couleur de l'image RGB sans création de buffer supplémentaire */
   protected byte[] getColor(byte c[],int color) {
      for( int i=0; i<c.length; i++ ) c[i] = (byte)( 0xFF & getPixRGB(pixelsRGB, i, color));
      return c;
   }

   /** Retourne la composante rouge initiale */
   protected byte[] getRed() {
      if( red==null || red.length!=pixelsRGB.length ) createRGB();
      return red;
   }

   /** Retourne la composante bleue initiale */
   protected byte[] getBlue() {
      if( blue==null && blue.length!=pixelsRGB.length) createRGB();
      return blue;
   }

   /** Retourne la composante verte initiale */
   protected byte[] getGreen() {
      if( green==null && green.length!=pixelsRGB.length) createRGB();
      return green;
   }
   
   /** Retourne les pixels 8 bits correspondants à l'image en N&B 
    *  pour par exemple l'extraction des contours */
   protected byte [] getBufPixels8() {
      return getGreyPixels();
   }

   /** Nettoyage du plan pour aider le GC
    */
    protected boolean Free() {
       if( !super.Free() ) return false;
       pixelsRGB=null;
       red=green=blue=null;
       planRed=planGreen=planBlue=null;
       flagRed=flagGreen=flagBlue=false;
       pi[0]=pi[1]=pi[2]=-1;
       return true;
    }

  /** Copie les parametres
   * @param p le plan de reference pour le reechantillonage
   */
   protected void init(String label, PlanImage p){

      flagOk=false;
      askActive=true;
//      selected = true;

      headerFits = null;
      if( Projection.isOk(p.projd) ) setHasSpecificCalib();
      naxis1=width = p.width;
      naxis2=height = p.height;
//      video = diff?VIDEO_INVERSE:VIDEO_NORMAL;
      video = VIDEO_NORMAL;
      fmt = RGB;
      transfertFct=LINEAR;
      res = UNDEF;
      orig = COMPUTED;
      status = "Re-sampling (at the nearest pixel):";
      progress = "computing...";
      objet = p.objet;
      if( label==null ) label= "RGB img";
      setLabel(label);
      co = p.co;
      c = p.c;
      projd = p.projd;
      projD = p.projD==null ? null : (Hashtable)p.projD.clone();
      from = "Colored composition by Aladin";

/*
      // La memorisation du dernier zoom associe au plan
      xzoom = p.xzoom;
      yzoom = p.yzoom;
      zoom = p.zoom;
*/
      // Acces vers les PlanImage sources
      pi[0] = flagRed?planRed.hashCode():0;
      pi[1] = flagGreen?planGreen.hashCode():0;
      pi[2] = flagBlue?planBlue.hashCode():0;
      labels[0] = planRed!=null?planRed.label:"none";
      labels[1] = planGreen!=null?planGreen.label:"none";
      labels[2] = planBlue!=null?planBlue.label:"none";
      RGBControl = new int[RGBCONTROL.length];
      for( int i=0; i<RGBCONTROL.length; i++) RGBControl[i] = RGBCONTROL[i];

      param =  "R:"+labels[0]+
              " G:"+labels[1]+
              " B:"+labels[2];
   }

   /** Positionne la composante couleur d'un pixel directement dans le tableau ARGB
    * @param pixelsRGB Tableau ARGB
    * @param offset position dans le tableau
    * @param color numéro de la composante 0-Red, 1-Green, 2-Blue
    * @param value valeur du pixel pour la composante concernée
    */
   private void setPixRGB(int pixelsRGB[], int offset, int color, int value) {
      switch(color) {
         case 0: // Red
            pixelsRGB[offset] &= 0xFF00FFFF;
            pixelsRGB[offset] |= 0x00FF0000 & (value<<16);
            break;
         case 1: // Green
            pixelsRGB[offset] &= 0xFFFF00FF;
            pixelsRGB[offset] |= 0x0000FF00 & (value<<8);
            break;
         case 2: // Blue
            pixelsRGB[offset] &= 0xFFFFFF00;
            pixelsRGB[offset] |= 0x000000FF & (value);
            break;
      }
   }

   /** Récupère la composante couleur d'un pixel directement depuis le tableau ARGB
    * @param pixelsRGB Tableau ARGB
    * @param offset position dans le tableau
    * @param color numéro de la composante 0-Red, 1-Green, 2-Blue
    */
   static protected int getPixRGB(int pixelsRGB[], int offset, int color) {
      return getPixRGB(pixelsRGB[offset],color);
   }
   
   /** Récupère la composante couleur d'un pixel directement depuis sa valeur ARGB
    * @param pixelRGB pixel ARGB
    * @param color numéro de la composante 0-Red, 1-Green, 2-Blue
    */
   static private int getPixRGB(int pixelRGB, int color) {
      switch( color ) {
         case 0 : return 0xFF & (pixelRGB >> 16);
         case 1 : return 0xFF & (pixelRGB >> 8);
         default: return 0xFF & (pixelRGB);
      }
   }

  /** Attente pendant la construction du plan.
   * @return <I>true</I> si ok, <I>false</I> sinon.
   */
   protected boolean waitForPlan() {
      if( !mustResample ) {
         if( !super.waitForPlan() ) return false;
         calculPixelsZoomRGB();
         return true;
      }

      Aladin.trace(3,"Resampling (R:"+labels[0]+",G:"+labels[1]+",B:"+labels[2]+" astro from "+pRef.label+")...");
      int tR=0,tA=1,tB=2;
      PlanImage pA=null,pB=null;
      byte [] refCm,pACm,pBCm;

           if( pRef==planRed )   { pA=planGreen; pB=planBlue;  tR=0; tA=1; tB=2; }
      else if( pRef==planGreen ) { pA=planRed;   pB=planBlue;  tR=1; tA=0; tB=2; }
      else if( pRef==planBlue )  { pA=planRed;   pB=planGreen; tR=2; tA=0; tB=1; }

     //Reechantillonage
      Coord coo = new Coord();
      int x=0,y=0;
      int w = pRef.width;
      int i;

      pixelsRGB = new int[ pRef.width*pRef.height ];
      for( i=0; i<pixelsRGB.length; i++ ) pixelsRGB[i]=0xFF000000;

      // Pour s'eviter des calculs inutiles
      boolean pAeqRef=true, pBeqRef=true;
      if( !( pA!=null && !Projection.isOk(pA.projd) || pB!=null && !Projection.isOk(pB.projd) || !Projection.isOk(pRef.projd) ) ) {
         pAeqRef = pA!=null && pRef.projd.c.TheSame(pA.projd.c);
         pBeqRef = pB!=null && pRef.projd.c.TheSame(pB.projd.c);
      }

      // Pour inverser les pixels le cas échéant
      boolean refRev = pRef.video==PlanImage.VIDEO_INVERSE;
      boolean pARev = pA!=null && pA.video==PlanImage.VIDEO_INVERSE;
      boolean pBRev = pB!=null && pB.video==PlanImage.VIDEO_INVERSE;
      
      // Pour accélérer l'accès aux tables des couleurs
      refCm = pRef==null ? null : Util.getTableCM(pRef.cm, 2);
      pACm  = pA==null   ? null : Util.getTableCM(pA.cm, 2);
      pBCm  = pB==null   ? null : Util.getTableCM(pB.cm, 2);

      for( i=0; i<pRef.pixels.length; i++ ) setPixRGB( pixelsRGB, i, tR,
            refRev ? 255-refCm[0xff & pRef.pixels[i]] : refCm[0xff & pRef.pixels[i]] );
      if( pAeqRef && pA!=null ) for( i=0; i<pA.pixels.length; i++ ) setPixRGB( pixelsRGB, i, tA,
            pARev ? 255-pACm[0xff & pA.pixels[i]] : pACm[0xff & pA.pixels[i]]) ;
      if( pBeqRef && pB!=null ) for( i=0; i<pB.pixels.length; i++ ) setPixRGB( pixelsRGB, i, tB,
            pBRev ? 255-pBCm[0xff & pB.pixels[i]] : pBCm[0xff & pB.pixels[i]] );

      if( !pAeqRef || !pBeqRef ) {
         for( i=0; i<pRef.pixels.length; i++ ) {
            coo.x = i%w;
            coo.y = i/w;
            pRef.projd.getCoord(coo);
            if( Double.isNaN(coo.al) ) continue;

            if( !pAeqRef ) {
               if( pA!=null ) {
                  pA.projd.getXY(coo);
                  if( !Double.isNaN(coo.x) ) {
                     x=(int)Math.round(coo.x);
                     y=(int)Math.round(coo.y);
                     if( x>=0 && x<pA.width && y>=0 && y<pA.height )
                        setPixRGB( pixelsRGB, i, tA,
                              pARev ? 255-pACm[0xff & pA.pixels[y*pA.width+x]]
                                    : pACm[0xff & pA.pixels[y*pA.width+x]] );
                  }
               }
            }

            if( !pBeqRef ) {
               if( pB!=null ) {
                  pB.projd.getXY(coo);
                  if( !Double.isNaN(coo.x) ) {
                     x=(int)Math.round(coo.x);
                     y=(int)Math.round(coo.y);
                     if( x>=0 && x<pB.width && y>=0 && y<pB.height )
                        setPixRGB( pixelsRGB, i, tB,
                              pBRev ? 255-pBCm[0xff & pB.pixels[y*pB.width+x]]
                                    : pBCm[0xff & pB.pixels[y*pB.width+x]] );
                  }
               }
            }

            // Pour laisser la main aux autres threads
            // et pouvoir afficher le changement de pourcentage
            if( i%10000==0 ) {
               setPourcent(i*100L/pRef.pixels.length);
               if( Aladin.isSlow ) Util.pause(10);
            }
         }
      }
      setPourcent(-1);

      Aladin.trace(3,"Resampling achieved...");

      // Personnalisation des parametres
      cm = ColorModel.getRGBdefault();
      bitpix=8;

      // Specification dans le cas de 2 couleurs
      twoColors = planRed==null || planGreen==null || planBlue==null;

           if( planRed==null )   createLastColor(pixelsRGB,0);
      else if( planGreen==null ) createLastColor(pixelsRGB,1);
      else if( planBlue==null )  createLastColor(pixelsRGB,2);

      calculPixelsZoomRGB();
      changeImgID();

      sendLog("RGB"," [R:"+labels[0]+",G:"+labels[1]+",B:"+labels[2]+"]");

      // Pour ne pas garder de références inutiles
//      Red=Green=Blue=Ref=null;

      return true;
   }

   /** Creation du tableau des pixels couleurs a partir des
   *     3 tableaux de pixels sources.
   */
   protected void createImgRGB() {
      createRGB();      // Au cas où les composantes n'auraient pas été extraites au préalable
      int size = width * height;
      pixelsRGB = new int[size];

      for (int y = 0 ; y < size ; y++) {
         pixelsRGB[y] = 0xFF000000 |
                          ( ( ((int) red[y])   & 0xFF ) << 16) |
                          ( ( ((int) green[y]) & 0xFF ) << 8) |
                            ( ((int) blue[y])  & 0xFF );
      }

      // Dans le cas de l'inverse video on recalcule l'image
//      if( video==PlanImage.VIDEO_INVERSE ) inverseRGB();

      // Et on calcule encore l'imagette pour le zoomView
      calculPixelsZoomRGB();
      changeImgID();
   }

   /** Creation du tableau des pixels de la derniere couleur
    * dans le cas de 2 couleurs affecte la moyenne des valeurs
    */
  protected void createLastColor(int pixelsRGB[],int color) {
      int j,k;

      // j et k : indice des couleurs existantes
      // color : indice de la couleur à créer
      if( color==0 ) { j=1; k=2; }
      else if( color==1 ) { j=0; k=2; }
      else { j=0; k=1; }

      int size=width*height;
      for (int i = 0 ; i < size ; i++) {

         int a1 = getPixRGB(pixelsRGB,i,j);
         int b1 = getPixRGB(pixelsRGB,i,k);

         // difference entre les 2 bandes
         if( diff ) {
            if( a1==b1 ) pixelsRGB[i]=0xFF000000;
            else if ( (a1-b1)>0 ) {
               setPixRGB(pixelsRGB, i, j, a1-b1);
               setPixRGB(pixelsRGB, i, k, 0);
               setPixRGB(pixelsRGB, i, color, 0);
            } else {
               setPixRGB(pixelsRGB, i, k, b1-a1);
               setPixRGB(pixelsRGB, i, j, 0);
               setPixRGB(pixelsRGB, i, color, 0);
            }
         } else {
            setPixRGB(pixelsRGB, i, color, (a1+b1)/2 );
         }
      }
   }

   /** Creation du tableau des pixels de la derniere couleur dans le cas de 2 couleurs
   *    Affecte la moyenne des valeurs
   */
//   protected void createLastColor(byte x[],byte a[],byte b[]) {
//
//      int size=width*height;
//      for (int i = 0 ; i < size ; i++) {
//
//         // difference entre les 2 bandes
//         if( diff ) {
//            int a1 = ((int) a[i]) & 0xFF;
//            int b1 = ((int) b[i]) & 0xFF;
//            if( a1==b1 ) a[i]=b[i]=x[i]=0;
//            else if ( (a1-b1)>0 ) {
//               a[i] = (byte)(0xFF & (a1-b1));
//               b[i]=x[i] = 0;
//            } else {
//               b[i] = (byte)(0xFF & (b1-a1));
//               a[i]=x[i] = 0;
//            }
////         } else x[i] = (byte)((int) Math.min ( ((int) a[i]) & 0xFF,
////               ((int) b[i]) & 0xFF )  & 0xFF );
//           } else x[i] = (byte)( 0xFF & ((((int)(a[i])&0xFF) + ((int)(b[i])&0xFF))/2) );
//      }
//
//      // Normalisation pour augmenter le constraste des différences
//      if( diff ) normalisation(a,b);
//   }

   /** Normalisation conjointes des deux composantes produites par la soustraction
    * de deux images afin d'augmenter les contrastes
    * @param a première composante (différences positives)
    * @param b deuxième composante (différences négatives)
    */
   private void normalisation(byte a[],byte b[]) {
       double min = Double.POSITIVE_INFINITY;
       double max = Double.NEGATIVE_INFINITY;
       double c;

       for( int i=0; i<2; i++ ) {
          byte x[] = i==0 ? a : b;
          for( int j=0; j<x.length; j++ ) {
             c = ((int)x[j]) & 0xFF;
             if( c<min ) min=c;
             if( c>max ) max=c;
          }
       }
//System.out.println("RGBdiff min="+min+" max="+max);

       double r = 256./(max - min);
       for( int i=0; i<2; i++ ) {
          byte x[] = i==0 ? a : b;
          for( int j=0; j<x.length; j++ ) {
             c = ((int)x[j]) & 0xFF;
             x[j] = (byte)( c<=min?0x00:c>=max?0xff:(int)( ((c-min)*r) ) & 0xff);
          }
       }

   }

   /** Modifie le tableau des pixels couleurs lors d'un changement dans
   *    l'inverse video
   */
   protected void inverseRGB () {
      int size = width * height;

      for (int y = 0 ; y < size ; y++) pixelsRGB[y] = pixelsRGB[y] ^ 0x00FFFFFF;
      changeImgID();
      aladin.calque.select.repaint();
   }


   /** Filtre le tableau de pixels couleurs en fonction de l'histogramme
   * @param triangle []   Tableau des positions des triangles
   * @param size          Taille totale de la zone de l'image a parcourir
   * @param x,y          Position initiale sur l'image
   * @param color         Couleur de l'histogramme modifie :
                        0 -> rouge
                        1 -> vert
                        2 -> bleu
   */
//   public void filterRGB(int [] triangle, int size, int x, int y, int color) {
//      int w = x+size;
//      int h = y+size;
//      int [] out = aladin.view.getCurrentView().pixelsCMRGB;
//
//      changeImgID();
//
//      int tr0 = triangle[0];
//      int tr1 = triangle[1];
//      int tr2 = triangle[2];
//      int r,g,b;
//
//      for ( int i=y; i<h; i++ ) {
//         for ( int j=x; j<w; j++ ) {
//            int pos=i*width+j;
//            // recuperation des couleurs courantes
//            int pixel = pixelsRGB[pos];
//            r = (int) ( (pixel & 0x00FF0000) >> 16 ) ;
//            g = (int) ( (pixel & 0x0000FF00) >> 8 ) ;
//            b = (int) ( (pixel & 0x000000FF) ) ;
//
//            // modification uniquement de celle concernee
//                 if (color==0) r = filter(tr0,tr1,tr2,(int)red[pos]&0xFF);
//            else if (color==1) g = filter(tr0,tr1,tr2,(int)green[pos]&0xFF);
//            else b = filter(tr0,tr1,tr2,(int)blue[pos]&0xFF);
//
//            // inversion si necessaire
//            if( video==VIDEO_INVERSE ) {
//               if( color==0 ) r=~r;
//               else if( color==1 ) g=~g;
//               else b=~b;
//
//               if (twoColors) {
//                  if( !flagR ) r = Math.max(g & 0xFF, b & 0xFF);
//                  else if( !flagG ) g = Math.max(r & 0xFF, b & 0xFF);
//                  else b = Math.max(r & 0xFF, g & 0xFF);
//               }
//
//            } else {
//               if (twoColors) {
//                  if( !flagR ) r = Math.min(g & 0xFF, b & 0xFF);
//                  else if( !flagG ) g = Math.min(r & 0xFF, b & 0xFF);
//                  else b = Math.min(r & 0xFF, g & 0xFF);
//               }
//            }
//
//            // ecriture en sortie
//            out[(i-y)*(w-x)+j-x]=  0xFF000000 | ((r&0xFF)<<16) | ((g&0xFF)<<8) | (b&0xFF);
//         }
//      }
//   }


   /** Filtre tout le tableau de pixels couleurs en fonction de l'histogramme
   * @param triangle []   Tableau des positions des triangles
   * @param color         Couleur de l'histogramme modifie :
                        0 -> rouge
                        1 -> vert
                        2 -> bleu
   */
   public void filterRGB(int [] triangle, int color) {

      changeImgID();
      
      if( red==null ) createRGB();

      int tr0 = triangle[0];
      int tr1 = triangle[1];
      int tr2 = triangle[2];

      int size = width*height;
      int r,g,b ;

      for ( int pos=0; pos<size; pos++ ) {
         // Recuperation des couleurs courantes
         r = (int) ( (pixelsRGB[pos] & 0x00FF0000) >> 16 ) ;
         g = (int) ( (pixelsRGB[pos] & 0x0000FF00) >> 8 ) ;
         b = (int) ( (pixelsRGB[pos] & 0x000000FF) ) ;

         // Modification uniquement de celle concernee
         if (color == 0) r = filter(tr0,tr1,tr2,(int)red[pos]&0xFF);
         else if (color == 1) g = filter(tr0,tr1,tr2,(int)green[pos]&0xFF);
         else b = filter(tr0,tr1,tr2,(int)blue[pos]&0xFF);

         // inversion si necessaire
         if( video==VIDEO_INVERSE ) {
            if( color==0 ) r=~r;
            else if( color==1 ) g=~g;
            else b=~b;

            if (twoColors) {
               if( !flagRed ) r = Math.max(g & 0xFF, b & 0xFF);
               else if( !flagGreen ) g = Math.max(r & 0xFF, b & 0xFF);
               else b = Math.max(r & 0xFF, g & 0xFF);
            }

          } else {
             if (twoColors) {
                if( !flagRed ) r = ((g & 0xFF) +  (b & 0xFF))/2;
                else if( !flagGreen ) g = ((r & 0xFF) +  (b & 0xFF))/2;
                else b = ((r & 0xFF) +  (g & 0xFF))/2;
             }
          }

         // ecriture en sortie
         pixelsRGB[pos]= 0xFF000000 | ((r&0xFF)<<16) | ((g&0xFF)<<8) | (b&0xFF);
      }
   }


   /** Calcul des nouvelles valeurs ds pixels suivant l'histogramme
   * Retourne la valeur calculee
   * @param tr0, tr1, tr2   Les positions des triangles
   * @param var            La valeur initiale
   */
   protected int filter(int tr0, int tr1, int tr2, int var){
      double dy = 128.0;
      double dx, alpha, beta;

      // Seuil bas et haut
      if (var<=tr0)
         return 0;
      else if (var>=tr2)
         return 255;
      else if (var>tr0 && var<=tr1) {
         // Premier segment de droite pour la 1ere moitie de la dynamique
         dx = (double)(tr1-tr0);
         if( dx>0.0 ) {
            alpha = dy/dx;
            beta = -alpha*tr0;
               return (byte)(var*alpha+beta);
         }
      } else {
         // Deuxieme segment de droite pour la 2eme moitie de la dynamique
         dx = (double)(tr2-tr1);
         if( dx>0.0 ) {
            alpha = dy/dx;
            beta = 128.0-alpha*tr1;
               return (byte)(var*alpha+beta);
         }
      }
      return var;
   }


   /** Retournement de l'image
    * @param methode 0-N/S, 1-D/G, 2-N/S+D/G
    */
    protected void flip(int methode) {
       createRGB();
       if( methode==0 || methode==2 ) invImageLine(width,height,pixelsRGB);
       if( methode==1 || methode==2 ) invImageRow(width,height,pixelsRGB);
       for( int i=0; i<3; i++ ) {
          byte [] pixels = i==0 ? red : i==1 ? green : blue;
          if( methode==0 || methode==2 ) invImageLine(width,height,pixels);
          if( methode==1 || methode==2 ) invImageRow(width,height,pixels);
       }
       if( Projection.isOk(projd) ) projd.flip(methode);
       changeImgID();

       calculPixelsZoomRGB();
       aladin.calque.zoom.zoomView.repaint();

       aladin.view.newView(1);
       aladin.view.repaintAll();
    }

    /** Retournement de l'image (Inversion des colonnes)
     * @param width largeur de l'image
     * @param height hauteur de l'image
     * @param pixelsRGB Tableau des pixels (en entree et en sortie)
     */
    protected static void invImageRow(int width, int height,int [] pixelsRGB) {
       int tmp;
       for( int h=0; h<height; h++) {
          int offset1=h*width;
          for( int w=width/2-1; w>=0; w-- ) {
             int offset2=offset1+width-w-1;
             tmp = pixelsRGB[offset1+w];
             pixelsRGB[offset1+w]=pixelsRGB[offset2];
             pixelsRGB[offset2]=tmp;
          }
       }
    }

    /** Retournement de l'image (inversion des lignes)
     * @param width largeur de l'image
     * @param height hauteur de l'image
     * @param pixelsRGB Tableau des pixels (en entree et en sortie)
     */
    protected static void invImageLine(int width, int height,int [] pixelsRGB) {
       int[] tmp = new int[width];
       for( int h=height/2-1; h>=0; h-- ) {
          int offset1=h*width;
          int offset2=(height-h-1)*width;
          System.arraycopy(pixelsRGB,offset1, tmp,0, width);
          System.arraycopy(pixelsRGB,offset2, pixelsRGB,offset1, width);
          System.arraycopy(tmp,0, pixelsRGB,offset2, width);
       }
    }

    /** Return une Image (au sens Java). Mémorise cette image pour éviter de la reconstruire
     * si ce n'est pas nécessaire */
    protected Image getImage(ViewSimple v,boolean now) {
       if( oImgID==imgID ) return image;
       image = Toolkit.getDefaultToolkit().createImage(
             new MemoryImageSource(width,height,cm, pixelsRGB, 0, width));
       oImgID=imgID;
       return image;
    }

   /** Retourne les 3 composantes du pixel repéré dans l'image */
   public int getPixel8(int x,int y) {
      return pixelsRGB[y*width+x];
   }
   
   // Pour ne vaire postAJLoad() qu'une fois
   private boolean postAJDone=false;
   
   // Après une lecture d'un fichier AJ, il faut éventuellement récupérer les plans correspondant
   // au labelRed, labelGreen et labelBlue (si présent dans la pile) afin de pouvoir
   // visualiser les valeurs des pixels d'origine
   // => voir getPixelInfo()
   private void postAJLoad() {
      postAJDone=true;
      try {
         if( labelRed!=null )   {
            planRed   = (PlanImage)aladin.calque.getPlan(labelRed, 1);  
            flagRed = planRed!=null;
            if( flagRed ) pi[0] = planRed.hashCode();
         }
         if( labelGreen!=null )   {
            planGreen   = (PlanImage)aladin.calque.getPlan(labelGreen, 1);  
            flagGreen = planGreen!=null;
            if( flagGreen ) pi[1] = planGreen.hashCode();
         }
         if( labelBlue!=null )   {
            planBlue   = (PlanImage)aladin.calque.getPlan(labelBlue, 1);  
            flagBlue = planBlue!=null;
            if( flagBlue ) pi[2] = planBlue.hashCode();
         }
      } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
   }

   /** Retourne les valeurs des trois pixels d'origine */
   protected String getPixelInfo(int x,int y,int mode) {
//      if( mode!=Pixel.LEVEL ) return UNK;
      
      if( !Projection.isOk(projd) || x<0 || x>=width || y<0 || y>=height ) return "";
      
      // Post traitement nécessaire éventuellement après le chargement d'un RGB issu
      // d'un fichier AJ
      if( !postAJDone ) postAJLoad();
      
      // On recherche les valeurs des pixels dans les images originales
      // si elles sont encore présentes dans la pile
      Coord c = new Coord();
      c.x=x; c.y=y;
      projd.getCoord(c);
      StringBuffer s1 = new StringBuffer();
      String s;
      int n=0;
      for( int j=0; j<3; j++ ) {
         PlanImage p = j==0 ? planRed : j==1 ? planGreen : planBlue;
         if( p==null || p.type==NO ) s="";
         else {
            p.projd.getXY(c);
            p.pixelsOriginFromCache();
            s = p.getPixelInfo((int)c.x, (int)c.y, mode);
            n++;
         }
         if( s1.length()>0 ) s1.append(' ');
         s1.append( (j==0?"R":j==1?"G":"B")+":"+Util.align(s,6));
      }
      return n==0 ? "" : s1.toString();

//      int i = y*width+x;
//      if( i>=pixelsRGB.length || i<0 ) return "";
//      return "R:"+Util.align3((pixelsRGB[i]>>16) & 0xFF)+"  "+
//             "G:"+Util.align3((pixelsRGB[i]>> 8) & 0xFF)+"  "+
//             "B:"+Util.align3((pixelsRGB[i]    ) & 0xFF);
    }

   /** Extraction d'une portion de l'image.
    * Retourne une portion de l'image sur la forme d'un tableau
    * de pixels en niveau de gris
    * @param newpixels Le tableau a remplir (il doit etre assez grand)
    * @param x,y,w,h   Le rectangle de la zone a extraire
    */
   protected void getPixels(byte [] newpixels,int x,int y,int w,int h) {
       int i,n;
       int k=0;
       int aw,ah;	// Difference en abs et ord lorsqu'on depasse l'image

       // Ajustement de la taille en cas de depassement
       aw=ah=0;
       if( x+w>width )  { aw = x+w-width;  w-=aw; }
       if( y+h>height ) { ah = y+h-height; h-=ah;}

       for( i=y, n=y+h; i<n; i++ ) {
          for( int j=0; j<w; j++, k++ ) {
             int c = pixelsRGB[i*width+x+j];
             newpixels[k] = (byte)( 0xFF & ( ( (c & 0xFF0000)>>16 +
                                               (c & 0xFF00)>>8 +
                                               (c & 0xFF) )/3) );
          }
       }
    }

    /** Extraction d'une portion de l'image en double (moyenne des R, G et B
     * Retourne une portion de l'image sur la forme d'un tableau de pixels
     * sens des lignes FITS
     * @param newpixels Le tableau a remplir (il doit etre assez grand)
     * @param x,y,w,h   Le rectangle de la zone a extraire
     * A VERIFIER LE SENS DE LA BOITE
     */
    protected void getPixels(double [] newpixels,int x,int y,int w,int h) {
       int i,j;
       int aw,ah;   // Difference en abs et ord lorsqu'on depasse l'image

       // Ajustement de la taille en cas de depassement
       aw=ah=0;
       if( x+w>width )  { aw = x+w-width;  w-=aw; }
       if( y+h>height ) { ah = y+h-height; h-=ah; }

       for( i=0; i<h; i++ ) {
          for( j=0; j<w; j++ ) {
             int c = pixelsRGB[(height-(i+y)-1)*width+j+x];
             newpixels[i*w+j] = ((0xFF &(c>>24)) + (0xFF &(c>>16)) + (0xFF &c) ) /3;
          }
       }
    }


   /** Extraction d'une portion de l'image en entier ARGB.
   * Retourne une portion de l'image sur la forme d'un tableau de pixels
   * sens des lignes JPEG
   * @param newpixels Le tableau a remplir (il doit etre assez grand)
   * @param x,y,w,h   Le rectangle de la zone a extraire
   */
   public void getPixels(int [] newpixels,int x,int y,int w,int h) {
      int i,n;
      int k=0;
      int aw,ah;   // Difference en abs et ord lorsqu'on depasse l'image
      
      // Ajustement de la taille en cas de depassement
      aw=ah=0;
      if( x+w>width )  { aw = x+w-width;  w-=aw; }
      if( y+h>height ) { ah = y+h-height; h-=ah; }

      for( i=y, n=y+h; i<n; i++ ) {
         System.arraycopy(pixelsRGB,i*width+x, newpixels,k, w);
         k+=w+aw;
      }
   }

}
