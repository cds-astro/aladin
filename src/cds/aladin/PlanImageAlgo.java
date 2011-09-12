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

/**
 * Gestion d'un Plan d'image calculé
 * @author Pierre Fernique [CDS]
 * @version 1.0 : avril 2007 - Création
 */
public class PlanImageAlgo extends PlanImage {
   static final protected int PPV = 0;  // Plus proche voisin
   static final protected int BILINEAIRE = 1; // Méthode bilinéaire
   
   static final protected int ADD   = 0;
   static final protected int SUB   = 1;
   static final protected int MUL   = 2;
   static final protected int DIV   = 3;
   static final protected int NORM  = 4;    // Normalisation [min..max] par division par la moyenne
   static final protected int NORMCUT=5;    // Normalisation [minCut..maxCut]  par division par la moyenne
   static final protected int CONV   =6;    // Convolution
   static final protected int CONVG  =7;    // Convolution par une gaussienne
   static final protected int BITPIX =8;    // Conversion du BITPIX
   static final protected int BITPIXCUT =9;    // Conversion du BITPIX
   
   static protected String NAME[] = { "Add","Sub","Mul","Div","Norm","NormC","Conv","ConvG","Bitpix","BitpixC" };

   
   private PlanImage p1=null; // Le plan Image première opérande (ou null)
   private PlanImage p2=null; // Le plan Image deuxième opérande (ou null)
   private int fct; // la fonction +,-,*,/ (0,1,2 ou 3)
   private double coef; // Le coefficent ou NaN si inutilisé
   private String param; // Le nom de la convolution ou le kernel ou null si inutilisé
   
   // Mode de calcul
   protected int methode;                 // Méthode du resampling si nécessaire
   private boolean askNewView=false;      // true si on doit créer une nouvelle vue.

   protected PlanImageAlgo(Aladin aladin,String label,PlanImage p1,PlanImage p2,int fct,
                           double coef, String conv, int methode) {
      super(aladin,p1);
      type=IMAGEALGO;
      askNewView=true;
      isOldPlan = !aladin.calque.isNewPlan(label);
      setLabel(label == null ? "conv["+label+"]" : label);
      
      launchAlgo(p1,p2,fct,coef,conv,methode);
   }
   
   // Pour pouvoir faire une copie
   protected PlanImageAlgo(Aladin aladin,PlanImage p) { 
      super(aladin,p);
      type=IMAGEALGO;
   }
   
   protected PlanImageAlgo() {}
       
   // Pour pouvoir recharger du AJ
   protected PlanImageAlgo(Aladin aladin) { 
      super(aladin);
      type=IMAGEALGO;
   }
   
   /** 
    * Lance la génération de l'image.
    * Méthode générique supportant soit le calcul avec un coefficient, avec
    * une autre image ou une convolution prédéfinie ou passée en paramètre. Dans le cas d'un
    * calcul avec deux images, la première donne la solution astrométrique, et les pixels
    * correspondants de la deuxième image sont obtenus soit au plus proche, soit en bilinéaire.
    * Si aucune des images n'a de solution astrométrique, le calcul se fait pixel à pixel.
    * @param p1 plan de la première image opérande
    * @param p2 plan de la deuxième image opérande
    * @param fct type de calcul (ADD,SUB,MUL,ADD,NORM,NORMCUT,CONV
    * @param coef coefficient d'un calcul (p2 doit être alors égale à null)
    * @param param nom de la convolution prédéfinie ou matrice particulière (pour CONV et CONVG)
    *              BITPIX cible pour bitpix
    * @param methode PPV ou BILINEAIRE
    */
   protected void launchAlgo(PlanImage p1,PlanImage p2,int fct, double coef, String param,int methode) {
      this.methode=methode;
      
      
      this.p1 = p1==null ? null : new PlanImage(aladin,p1);
      this.p2 = p2==null ? null : new PlanImage(aladin,p2);
      this.p1 = p1;
      this.p2 = p2;
      
      this.fct = fct;
      this.coef = coef;
      this.param = param;
      
      from = "Computed by Aladin";
      param = "Computed: "+getFonction();
      setHasSpecificCalib();
      
      flagOk=false;
      flagProcessing=true;
      aladin.calque.select.repaint();
     
      sendLog("Compute"," [" + this + " = "+getFonction()
            +(methode==BILINEAIRE?"/bilinear":"")
            +"]");
      
      synchronized( this ) {
         runme = new Thread(this,"AladinBuildAlgo");
         Util.decreasePriority(Thread.currentThread(), runme);
         runme.start();
      }
   }
   
   /** retourne en language "clair" la fonction opérée
    * Exemple : DSS2 * DSS1 */
   protected String getFonction() {
      return (p1!= null ? p1.label : p2!=null ? coef+"" : "") +
             " "+(fct==CONV ? "conv "+param+"" 
                   : fct==BITPIX  ? "bitpix "+param+""
                   : fct==BITPIXCUT  ? "bitpix -cut "+param+""
                   : getFct(fct) + (p2!=null ? p2.label : coef+""))+" " ;
   }
   
   static private final String FCT = "+-*/~#";
   static protected String getFct(int fct) {
      return FCT.charAt(fct)+"";
   }
   
   /** J'overide cette méthode pour ne pas créer automatiquement une nouvelle vue */
   protected void planReady(boolean ready) {
      if( !ready || askNewView ) { super.planReady(ready); return; }
      
      setActivated(true);
      pourcent=-1;
      flagOk = true;
      
      aladin.calque.repaintAll();
   }
   
   /** Pour permettre de normaliser un plan avant calcul */
   static protected PlanImage normalise(PlanImage p) {
      PlanImageAlgo p1 = new PlanImageAlgo(p.aladin,null,p,null,PlanImageAlgo.NORM,0,null,0);
      while( p1!=null && !p1.isSync()) {
         Util.pause(500);
         Aladin.trace(4,"PlanImageAlgo.normalise:  waiting "+p+"...");
      }
      return (PlanImage)p1;
   }
   
       
   /** Effectue l'opération. Cette méthode
    * ne doit jamais être appelé directement mais via la méthode
    * launchAlgo()
    * @return
    */
   protected boolean compute() {
      
      flagProcessing=true;
      aladin.calque.select.repaint();
      
      // Chargement des pixels d'origine
      if( p1!=null ) { p1.setLockCacheFree(true); p1.pixelsOriginFromCache(); }
      if( p2!=null ) { p2.setLockCacheFree(true); p2.pixelsOriginFromCache(); }
      
      Aladin.trace(3,"Computing " + getFonction()+"...");
      Coord coo=new Coord();
      int x,y,x1,y1;
      double op2=0;
      double min=0,max=0,moy=0;
      boolean flagNorm=false;
      
      // Pour la normalisation, il faut connaître le min et le max
      // et la moyenne
      if( fct==NORM || fct==NORMCUT) {
         flagNorm=true;
         min = fct==NORM ? dataMin : pixelMin;
         max = fct==NORM ? dataMax : pixelMax;
         min = min*bScale+bZero;
         max = max*bScale+bZero;
         coef = max-min;
         if( fct==NORMCUT ) transfertFct=LINEAR;
         
         // calcul de la moyenne 
         for( y=0; y<height; y++ ) {
            int n=0;
            double pixLine=0;
            for( x=0; x<width; x++ ) { 
               try {
                  pixLine += p1.getPixel(x,y);
                  n++;
               } catch( Exception e ) { }
            }
            moy+=pixLine/n;     // Moyenne de la ligne
         }
         moy /=height;          // Moyenne de l'image
      }
      
      // On prépare le tableau des pixels résultats dans le bitpix le plus représentatif
      int bitpix1 = p1==null ? 8 : p1.bitpix;
      int bitpix2 = p2==null ? 8 : p2.bitpix;
      bitpix = Math.max(Math.abs(bitpix1),Math.abs(bitpix2));
      if( bitpix1<0 || bitpix2<0 ) bitpix=-bitpix;
      if( fct==CONV && bitpix!=-64 ) bitpix = -32;
      
      // Dans le cas de la présence d'un BSCALE ou BZERO je passe en floattant
      if( (p1!=null && (p1.bScale!=1 || p1.bZero!=0) ) 
       || (p2!=null && (p2.bScale!=1 || p2.bZero!=0) ) ) if( bitpix!=-64 ) bitpix=-32;
      
      // Dans le cas d'une conversion de BIPITX, y a pas le choix
      if( fct==BITPIX  || fct==BITPIXCUT ) bitpix = Integer.parseInt(param);
      
      npix = Math.abs(bitpix)/8;
      bZero=0; bScale=1;
      isBlank = false;
      dataMinFits = dataMaxFits = 0.;
      
      if( headerFits!=null ) {
         headerFits.setKeyValue("BSCALE", 1+"");
         headerFits.setKeyValue("BZERO", 0+"");
      }
      
      aladin.trace(4,"PlanImageAlgo.compute(): Target image: "+width+"x"+height+" bitpix="+bitpix);
      
      pixelsOrigin = new byte[width*height*npix];
      double pixval;
      
      // Convertion ?
      if( fct==BITPIX || fct==BITPIXCUT ) {
         double maxCoding = bitpix==-64?Double.MAX_VALUE : bitpix==-32? Float.MAX_VALUE
               : bitpix==64?Long.MAX_VALUE : bitpix==32?Integer.MAX_VALUE : bitpix==16?Short.MAX_VALUE:255;
         double minCoding = bitpix==-64?Double.MIN_VALUE : bitpix==-32? Float.MIN_VALUE
               : bitpix==64?Long.MIN_VALUE : bitpix==32?Integer.MIN_VALUE : bitpix==16?Short.MIN_VALUE:0;
         double pMin=p1.getPixelMin();
         double pMax=p1.getPixelMax();
         double coef = (maxCoding-minCoding)/(pMax-pMin);
         for( y=0; y<height; y++ ) {
            for( x=0; x<width; x++ ) { 
               pixval = p1.getPixel(x, y);
               if( fct==BITPIXCUT ) {
                  if( pixval<pMin ) pixval=pMin;
                  else if( pixval>pMax ) pixval=pMax;
                  pixval =(pixval-pMin)*coef + minCoding;
               } else {
                  if( pixval>maxCoding ) pixval=maxCoding;
                  else if( pixval<minCoding ) pixval=minCoding;
               }
               setPixelOriginInDouble(x,y,pixval);
            }
            // Pour laisser la main aux autres threads
            // et pouvoir afficher le changement de pourcentage
            if( y % 100 == 0 ) {
               if( type==NO ) break;  // En cas de suppression inopinée du plan
               pourcent=x*y * 100 / p1.getBufPixels8().length;
               if( Aladin.isSlow ) Util.pause(10);
            }
         }
         pixelMin=dataMin = minCoding;
         pixelMax=dataMax = maxCoding;
      }
      
      // Convolution ?
      else if( fct==CONV ) {
         double pixRes=1/3600.;
         try { pixRes=projd.getPixResDelta(); } catch( Exception e) {}
         Kernel k = null;
         try { k=aladin.kernelList.getKernel(param,pixRes); }
         catch( Exception e ) { 
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            error=e.getMessage();
            if( error==null ) error="conv error";
         }
         if( k==null ) {
            aladin.warning(error);
            flagOk=true;
            return false; 
         }
         int i;
         int cols = k.matrix.length;
         int cols2 = cols/2;
         
         // On va travailler sur un simple tableau de pixels pour aller plus vite (JVM JIT aime ça !)
         double [] inPixels = new double[width*height];
         for( i=y=0; y<height; y++ ) {
            for( x=0; x<width; x++ ) {
               double pix = p1.getPixel(x,y);
               if( Double.isNaN(pix) ) continue;
               inPixels[i++] = pix;
            }
         }
         
         // Convolution rapide propre à la gaussienne
         if( k.gaussian!=null ) {
            double [] outPixels = new double[width*height];
            convolveAndTranspose(k, inPixels, outPixels, width, height,0);
            convolveAndTranspose(k, outPixels, inPixels, height, width,(width*height)/2);
            for( i=y=0; y<height; y++ ) {
               for( x=0; x<width; x++ ) setPixelOriginInDouble(x,y, inPixels[i++]);
            }

         // Convolution classique
         } else {
            for( y=0; y<height; y++ ) {
               for( x=0; x<width; x++ ) { 
                  pixval=0;
                  for( y1=-cols2; y1<=cols2; y1++ ){
                     for( x1=-cols2; x1<=cols2; x1++ ) {
                        int x2 = x+x1;
                        int y2 = y+y1;
                        if( x2<0 ) x2=0;
                        else if( x2>=width ) x2=width-1;
                        if( y2<0 ) y2=0;
                        else if( y2>=height ) y2=height-1;
                        pixval += inPixels[y2*width+x2] * k.matrix[x1+cols2][y1+cols2];
                     }
                  }
                  setPixelOriginInDouble(x,y, pixval);
               }
               // Pour laisser la main aux autres threads
               // et pouvoir afficher le changement de pourcentage
               if( y % 100 == 0 ) {
                  if( type==NO ) break;  // En cas de suppression inopinée du plan
                  pourcent=x*y * 100 / p1.getBufPixels8().length;
                  if( Aladin.isSlow ) Util.pause(10);
               }
            }
         }
      
      // Fonctions anamorphiques ?
      } else {
         
         boolean sameCalib=false;
         if( p1!=null && !Projection.isOk(p1.projd) 
          && p2!=null && !Projection.isOk(p2.projd) ) sameCalib=true;
         else if( p1!=null && p2!=null ) sameCalib = p1.projd.c.TheSame(p2.projd.c); 
         
//sameCalib=false;
         
         boolean noCalib=false;
         if( p1!=null && !Projection.isOk(p1.projd) && p2!=null && !Projection.isOk(p2.projd) ) noCalib=true;
         
         // Si meme calib, inutile de passer par l'interpolation
         if( sameCalib ) methode=PPV;
         
         // réinitialisation de minPix et maxPix (rq les minPixCut et maxPixCut seront
         // réinitialisés via l'autocut)
         dataMin=Double.MAX_VALUE;
         dataMax=Double.MIN_VALUE;
         
         for( y=0; y<height; y++ ) {
            for( x=0; x<width; x++ ) { 
               pixval = Double.NaN;
               try {
                  pixval = p1.getPixel(x,y);

                  // Normalisation
                  if( flagNorm ) {
                     if( pixval<min ) pixval=min;
                     if( pixval>max ) pixval=max;
//                     pixval = (pixval-min)/coef;
                     pixval /= moy;

                  } else {

                     // Simple coéf ?
                     if( p2==null || fct==NORM ) op2 = coef;

                     // ou deuxième image ?
                     else {                        
                        coo.x=x; coo.y=y;
                        if( !sameCalib && !noCalib) {
                           coo.y=height-coo.y-1;
                           p1.projd.getCoord(coo);
                           if( Double.isNaN(coo.al) ) continue;
                           p2.projd.getXY(coo);
                           coo.y=p2.height-coo.y-1;
                        }
                        if( !Double.isNaN(coo.x) ) {
                           switch( methode ) {
                              case PPV:
                                 x1=(int) Math.round(coo.x);
                                 y1=(int) Math.round(coo.y);
                                 if( x1<0 || x1>=p2.width || y1<0 || y1>=p2.height ) { op2=Double.NaN; break; }
                                 op2 = p2.getPixel(x1,y1);
                                 break;

                              case BILINEAIRE:
                                 x1=(int)Math.round(coo.x - 0.5);
                                 y1=(int)Math.round(coo.y - 0.5);
                                 int x2=x1 + 1;
                                 int y2=y1 + 1;
                                 if( x1<0 || x2>=p2.width || y1<0 || y2>=p2.height ) { op2=Double.NaN; break; } 
                                 double a0=p2.getPixel(x1,y1);
                                 double a1=p2.getPixel(x2,y1);
                                 double a2=p2.getPixel(x1,y2);
                                 double a3=p2.getPixel(x2,y2);
                                 double d0,d1,d2,d3,pA,pB;
                                 if( coo.x==x1 ) { d0=1; d1=0; }
                                 else if( coo.x==x2 ) { d0=0; d1=1; }
                                 else { d0 = 1./(coo.x-x1); d1 = 1./(x2-coo.x); }
                                 if( coo.y==y1 ) { d2=1; d3=0; }
                                 else if( coo.y==y2 ) { d2=0; d3=1; }
                                 else { d2 = 1./(coo.y-y1); d3 = 1./(y2-coo.y); }
                                 pA = (a0*d0+a1*d1)/(d0+d1);
                                 pB = (a2*d0+a3*d1)/(d0+d1);
                                 op2 = (pA*d2+pB*d3)/(d2+d3);
                                 break;
                           }
                        }
                     }

                     if( !Double.isNaN(op2) ) {
                        pixval = fct==ADD ? pixval + op2 :
                                 fct==SUB ? pixval - op2 :
                                 fct==MUL ? pixval * op2 :
                                            pixval / op2;
                        if( pixval<dataMin ) dataMin=pixval;
                        if( pixval>dataMax ) dataMax=pixval;
                     }
                  }
               } catch( Exception e ) { e.printStackTrace(); return false; }

               setPixelOriginInDouble(x,y, pixval);
            }

            // Pour laisser la main aux autres threads
            // et pouvoir afficher le changement de pourcentage
            if( y % 100 == 0 ) {
               if( type==NO ) break;  // En cas de suppression inopinée du plan
               pourcent=x*y * 100 / p1.getBufPixels8().length;
               if( Aladin.isSlow ) Util.pause(10);
            }
         }
      }
      
      flagProcessing=false;
      if( fmt==JPEG ) fmt=UNKNOWN;   // Pour éviter une permutation des lignes (voir reUseOriginalPixels()
      reUseOriginalPixels();
      if( flagNorm ) { dataMin=0; dataMax=1; }
      selected = active = true;
      
      Aladin.trace(3,"Algo achieved...");
      pourcent= -1;

      changeImgID();
//      aladin.view.repaintAll();
//      aladin.calque.zoom.zoomView.repaint();
      
      if( p1!=null ) p1.setLockCacheFree(false);
      if( p2!=null ) p2.setLockCacheFree(false);
      p1=p2=null;
      return true;
   }
   
   /**
    * Calcul d'une convolution par une gaussienne en 2 passes (voir compute())
    * @param gap Juste pour mettre à jour le pourcentage de progression en tenant compte du calcul précédent
    */
   protected void convolveAndTranspose(Kernel k, double [] inPixels, double [] outPixels, int width, int height,int gap) {
      int cols = k.matrix.length;
      int cols2 = cols/2;
      
      for( int y=0; y<height; y++ ) {
         int index = y;
         int ioffset = y*width;
         for( int x=0; x<width; x++ ) { 
            double pixval=0;
            for( int col=-cols2; col<=cols2; col++ ){
               int ix = x+col;
               if( ix<0 ) ix=0;
               else if( ix>=width ) ix = width-1;
               pixval += inPixels[ioffset+ix] * k.gaussian[col+cols2];
            }
            outPixels[index] = pixval;
            index+=height;
         }

         // Pour laisser la main aux autres threads
         // et pouvoir afficher le changement de pourcentage
         if( y % 100 == 0 ) {
            if( type==NO ) break;  // En cas de suppression inopinée du plan
            pourcent= (gap+(y*width)/2) * 100 / inPixels.length;
            if( Aladin.isSlow ) Util.pause(10);
         }
      }
   }
}
