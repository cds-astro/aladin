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

import cds.tools.Util;

import java.io.*;
import java.util.*;

/**
 * Gestion d'un plan image Cube
 *
 * @version 1.0 : juillet 2006
 */
public class PlanImageCube extends PlanImageBlink {

   private double crval3,crpix3,cdelt3;
   protected boolean fromCanal;

   /** Creation d'un plan de type IMAGECUBE (via un stream)
    * @param in le stream
    */
   protected PlanImageCube(Aladin aladin,String file,MyInputStream in,String label,String from,
         Obj o,ResourceNode imgNode,boolean skip,boolean doClose,Plan forPourcent) {
      super(aladin,file,in,label,from,o,imgNode,skip,doClose,forPourcent);
      type=IMAGECUBE;
      initDelay=400;
   }

   private int precision = -1;

   /** Retourne la valeur physique correspondant au numéro du canal */
   protected String getCanalValue(int n) {
      if( precision==-1 ) {
         double f = Math.abs(cdelt3);
         precision = f<0.001 ? 3 : f<0.01 ? 2 : f<100 ? 1 : 0;
      }
      //      return Util.myRound( ""+(n-crpix3)*cdelt3+crval3,precision);
      return ""+(int)Math.round(1000 * ((n+1-crpix3)*cdelt3+crval3))/1000.;
   }

   protected boolean cacheImageFits(MyInputStream dis) throws Exception {
      int naxis;
      long taille;		// nombre d'octets a lire
      int n;			// nombre d'octets pour un pixel

      Aladin.trace(2,"Loading FITS "+Tp[type]);

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

      naxis1=width = headerFits.getIntFromHeader("NAXIS1");
      naxis2=height = headerFits.getIntFromHeader("NAXIS2");
      depth = headerFits.getIntFromHeader("NAXIS3");

      pixMode = PIX_TRUE;
      npix = n = Math.abs(bitpix)/8;  // Nombre d'octets par valeur
      taille=(long)width*height*depth*n;	  // Nombre d'octets
      setPourcent(0);
      Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" NAXIS3="+depth+" BITPIX="+bitpix+" => size="+taille);

      //Les paramètres FITS facultatifs
      loadFitsHeaderParam(headerFits);

      // Les paramètres Fits de la 3eme dimension
      try {
         crpix3 = headerFits.getDoubleFromHeader("CRPIX3");
         crval3 = headerFits.getDoubleFromHeader("CRVAL3");
         cdelt3 = headerFits.getDoubleFromHeader("CDELT3");
         fromCanal=true;
      } catch( Exception e ) { fromCanal=false; }

      // Il s'agit d'une image MEF que l'on ne va pas garder, on se contente de skipper l'image
      if( flagSkip ) {
         dis.skip( taille );

         // Lecture effective
      } else {
         // Pour des stats
         Date d = new Date();
         Date d1;
         int temps;
         boolean dejaCharge=false;
         int frameToBeLoad = depth;

         double requiredMo =  (double)width*height*depth*(npix+1)/(1024.*1024);
         boolean loadInRam = aladin.getMem() - requiredMo > Aladin.MARGERAM;
         boolean partialInRam = false;
         Aladin.trace(4,"PlanImageCube.loadImageFits() ask for "+requiredMo+"MB "+(loadInRam?"try in Ram":"=> not enough space in RAM"));
         if( !loadInRam ) {
            aladin.freeSomeRam((long)(requiredMo*1024*1024),this);
            loadInRam = aladin.getMem() - requiredMo/10 > Aladin.MARGERAM;
            double t1 = (double)width*height*npix;
            double free = (aladin.getMem() - Aladin.MARGERAM)*1024*1024 - width*height*depth;
            frameToBeLoad = (int)( free/t1);
            Aladin.trace(4,"PlanImageCube.loadImageFits() [2nd test]  ask for "+requiredMo+"MB "+(loadInRam?"try in Ram "+frameToBeLoad+" frames":"not in RAM")+ " freeRAM="+free/(1024*1024.)+"MB, one frame="+t1/(1024*1024.)+"MB");
         }

         cacheFromOriginalFile = setCacheFromFile(dis);     // Positionnement de l'accès direct aux pixels sur fichier d'origine
         RandomAccessFile f=null;

         int tailleImg = floor(taille/depth);
         pixelsOrigin = new byte[tailleImg];

         int m = (depth/2)+1;     // Tranche souhaitée pour l'autocut
         byte[] buffer = null;    // Buffer pour la recherche de la tranche utilisée pour l'autocut
         long pos2emeLecture=0L;  // En cas de relecture du début du cube, mémorisation de la position initiale

         try {
            // Pas de 2ème passe possible => on mémorise dans buffer jusqu'à la tranche prévue
            // pour l'autocut
            if( !Aladin.STANDALONE ) {
               dejaCharge = true;
               int maxM = floor( (taille/4)/tailleImg );
               if( m>maxM ) m=maxM;   // Tranche trop loin, on prend plus près du début sinon on va exploser la mémoire
               buffer = new byte[round(tailleImg * m) ];
               dis.readFully(buffer);
               //System.out.println("Chargement d'un coup de "+buffer.length+" octets ("+m+" tranches) du début du cube"     );

               System.arraycopy(buffer,(m-1)*tailleImg,pixelsOrigin,0,tailleImg);

               // Autocut en 2 passes possibles, on va travailler tranche par tranche économiser
               // la mémoire
            } else {
               dejaCharge=false;
               buffer = pixelsOrigin;

               // C'est pas un fichier local, il faut copier les données dans le cache
               if( !cacheFromOriginalFile ) {
                  //System.out.println("Copie des "+m+" premières tranches dans le cache (par tranche de "+tailleImg+" octets)");
                  for( int i=0; i<m; i++ ) {
                     dis.readFully(buffer);
                     if( i==0 ) f=beginInCache(buffer);
                     else f.write(buffer);
                  }

                  // Génial, un fichier local, on va juste faire un skip, et revenir en arrière
               } else {
                  //System.out.println("Placement direct à la position en skippant "+((m-1)*tailleImg)+" octets du fichier original");
                  pos2emeLecture = dis.getPos();
                  dis.skip((m-1)*(long)tailleImg);
                  dis.readFully(buffer);
               }
            }

            // Autocut sur le plan au 1/4 du cube
            Aladin.trace(3," => Cube autocut uses the frame "+m);
            boolean cut = aladin.configuration.getCMCut();
            findMinMax(pixelsOrigin,bitpix,width,height,dataMinFits,dataMaxFits,cut,0,0,0,0);

            double mem,amem=0,delta,adelta=0;

            for( int i=0; i<depth; i++ ) {
               if( aladin.levelTrace==4 ) {
                  mem = aladin.getMem();
                  delta = amem-mem;
                  if( Math.abs(adelta-delta)>0.2 || mem<Aladin.MARGERAM-1 ) {
                     aladin.trace(4,"PlanImageCube.cacheImageFits(): frame "+i+" freeRam="+mem+"MB delta="+delta+"MB");
                  }
                  amem=mem;
                  adelta=delta;
               }

               try { setBufPixels8(new byte[width*height]); }
               catch( OutOfMemoryError e ) {
                  e.printStackTrace();
                  Aladin.trace(4,"PlanImageCube.loadImageFits(): out of memory1 freeRam="+aladin.getMem()+"MB => inRam=false...");
                  loadInRam=false;
                  freeRam();
                  setBufPixels8(new byte[width*height]);
               }

               // Je suis avant la tranche qui a servi pour l'autocut
               if( i<m ) {

                  // En une passe => tout est en mémoire
                  if( dejaCharge ) System.arraycopy(buffer,i*tailleImg,pixelsOrigin,0,tailleImg);
                  else {
                     if( i==0 ) {

                        // Il s'agit d'un fichier local et non du cache (puisque pos2emeLecture!=0),
                        // il faut réouvrir le fichier pour ne pas saboter le dataInputStream
                        if( pos2emeLecture!=0 ) {
                           //System.out.println("Réouverture du fichier d'origine à la position "+pos2emeLecture);
                           f = new RandomAccessFile(cacheID,"r");
                        }
                        f.seek( pos2emeLecture );
                     }
                     f.readFully(pixelsOrigin);
                  }

                  // Je suis après la tranche de l'autocut
               }else {
                  dis.readFully(pixelsOrigin);
                  if( f!=null && !cacheFromOriginalFile ) f.write(pixelsOrigin);
               }

               to8bits(getBufPixels8(),0,pixelsOrigin,pixelsOrigin.length/npix,bitpix,
                     /*isBlank,blank,*/pixelMin,pixelMax,true);

               invImageLine(width,height,getBufPixels8());
               String s = (fromCanal ? getCanalValue(i): label);
               try {
                  if( loadInRam && frameToBeLoad<0 ) {
                     Aladin.trace(4,"PlanImageCube.loadImageFits(): low memory2 (frame="+i+") => other frames not in RAM...");
                     partialInRam=true;
                     loadInRam=false;
                  }
                  addFrame(s,getBufPixels8(),loadInRam ? pixelsOrigin : null,cacheFromOriginalFile,cacheID,cacheOffset);
                  if( loadInRam ) {
                     pixelsOrigin = new byte[tailleImg];
                     frameToBeLoad--;
                  }
               } catch( OutOfMemoryError e ) {
                  e.printStackTrace();
                  Aladin.trace(4,"PlanImageCube.loadImageFits(): out of memory freeRam="+aladin.getMem()+"MB => inRam=false...");
                  loadInRam=false;
                  freeRam();
                  addFrame(s,getBufPixels8(),null,cacheFromOriginalFile,cacheID,cacheOffset);
               }
               cacheOffset+=pixelsOrigin.length;

               setPourcent((99.*i)/depth);

               // On calcule l'imagette du zoom
               if( i==0 ) calculPixelsZoom();

            }
         } finally { if( f!=null ) f.close(); }

         buffer=null;
         noOriginalPixels();

         d1=new Date(); temps = (int)(d1.getTime()-d.getTime()); d=d1;
         Aladin.trace(3," => Reading "+(!dejaCharge?"(2 pass) ":"")+"and analyzing "+getDepth()+" frames in "+Util.round(temps/0.001,3)+" s => "
               +(temps!=0 ? Util.round((taille/temps)/1024*1.024,2)+" Mbyte/s" : "--")
               +(loadInRam ? " (fully in RAM)": partialInRam?" (partially in RAM)":""));

         //         if( u!=null ) networkPerf(u.toString(),pixelsOrigin.length,temps);
      }



      // On se recale si jamais il y a encore une extension FITS qui suit
      if( naxis>3 ) {
         try {
            long offset=n*width*height;
            for( int i=3; i<naxis; i++ ) offset *= headerFits.getIntFromHeader("NAXIS"+(i+1));
            offset -= n*width*height;
            dis.skip(offset);
         } catch( Exception e ) { e.printStackTrace(); return false; }
      }

      // Dans le cas d'un MEF dont on skippe l'image, on peut sortir tout de suite
      if( flagSkip ) return true;

      creatDefaultCM();

      setPourcent(-1);
      return true;
   }

}
