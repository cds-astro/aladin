// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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

import java.io.RandomAccessFile;
import java.util.Date;

import cds.tools.Util;

/**
 * Gestion d'un plan image Cube
 *
 * @version 1.0 : juillet 2006
 */
public class PlanImageCube extends PlanImageBlink {

   private double crval3,crpix3,cdelt3;
   private String cunit3;
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
   
   /** Retourne la valeur physique correspondant au num�ro du canal 
    * suffix�e par l'unit� si elle est connue */
   protected String getCanalValue(int n) {
      return getCanalValue(depth-n,crpix3,cdelt3,crval3,cunit3);
   }

   /** Retourne la valeur physique correspondant au num�ro du canal 
    * en fonction des param�tres indiques,
    * suffix�e par l'unit� si elle est connue */
   static public String getCanalValue(int n, double crpix3, double cdelt3, double crval3, String cunit3) {
      double val = (n+1.-crpix3)*cdelt3 + crval3;
      String s = cunit3==null ? null : SED.getUnitFreq(val, cunit3);
      if( s==null && cunit3!=null ) s=SED.getUnitWave(val, cunit3);
      if( s==null ) {
         s = Util.myRound(val);
         if( cunit3!=null ) s=s+" "+cunit3;
      }
      return s;
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

      // Il s'agit juste d'une ent�te FITS indiquant des EXTENSIONs
      if( naxis<=1 && headerFits.getStringFromHeader("EXTEND")!=null ) {
         error="_HEAD_XFITS_";

         // Je saute l'�ventuel baratin de la premi�re HDU
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

      setPixMode( PIX_TRUE );
      npix = n = Math.abs(bitpix)/8;  // Nombre d'octets par valeur
      taille=(long)width*height*depth*n;	  // Nombre d'octets
      setPourcent(0);
      Aladin.trace(3," => NAXIS1="+width+" NAXIS2="+height+" NAXIS3="+depth+" BITPIX="+bitpix+" => size="+taille);

      //Les param�tres FITS facultatifs
      loadFitsHeaderParam(headerFits);

      // Les param�tres Fits de la 3eme dimension
      try {
         crpix3 = headerFits.getDoubleFromHeader("CRPIX3");
         crval3 = headerFits.getDoubleFromHeader("CRVAL3");
         cdelt3 = headerFits.getDoubleFromHeader("CDELT3");
         cunit3 = headerFits.getStringFromHeader("CUNIT3");
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

         long required =  (long)width*height*depth*(npix+1);
         boolean loadInRam = aladin.getMem() - required/(1024*1024) > Aladin.MARGERAM;
         boolean partialInRam = false;
         Aladin.trace(4,"PlanImageCube.loadImageFits() ask for "+Util.getUnitDisk( required)+" "+(loadInRam?"try in Ram":"=> not enough space in RAM"));
         if( !loadInRam ) {
            aladin.freeSomeRam(required,this);
            long freeMem = (long)aladin.getMem()*1024L*1024L;
            System.out.println("freeMem => "+Util.getUnitDisk(freeMem));
            loadInRam = (freeMem - required)/20L > Aladin.MARGERAM*1024L*1024L;
            //            double t1 = (double)width*height*npix;
            //            double dispMem=aladin.getMem();
            //            double free = (aladin.getMem() - Aladin.MARGERAM)*1024*1024 - width*height*depth;
            //            frameToBeLoad = (int)( free/t1);
            long t1 = (long)width*height*npix;
//            System.out.println("One frame => "+Util.getUnitDisk(t1));
            long dispMem= freeMem - Aladin.MARGERAM*1024L*1024L;
//            System.out.println("disp Mem => "+Util.getUnitDisk(dispMem));
            long pixel8Mem = (long)width*height*depth;
//            System.out.println("pixel8Mem => "+Util.getUnitDisk(pixel8Mem));
            long free = dispMem - pixel8Mem;
            if( free<0 ) free=0L;
//            System.out.println("free => "+Util.getUnitDisk(free));
            frameToBeLoad = (int)( free/t1);
//            System.out.println("frameToBeLoad => "+frameToBeLoad);
            //            Aladin.trace(4,"PlanImageCube.loadImageFits() [2nd test]  ask for "+requiredMo+"MB "
            //                +(loadInRam?"try in Ram "+frameToBeLoad+" frames":"not in RAM")
            //                + " freeRAM="+free/(1024L*1024L)+"MB, one frame="+t1/(1024L*1024L)+"MB");
            Aladin.trace(4,"PlanImageCube.loadImageFits() [2nd test]  ask for "+Util.getUnitDisk( required)
            +(loadInRam?"try in Ram "+frameToBeLoad+" frames":" not in RAM")
            + " freeRAM="+Util.getUnitDisk(free)+", one frame="+Util.getUnitDisk(t1));
         }

         cacheFromOriginalFile = setCacheFromFile(dis);     // Positionnement de l'acc�s direct aux pixels sur fichier d'origine
         RandomAccessFile f=null;

         int tailleImg = floor(taille/depth);
         pixelsOrigin = new byte[tailleImg];

         int m = (depth/2)+1;     // Tranche souhait�e pour l'autocut
         byte[] buffer = null;    // Buffer pour la recherche de la tranche utilis�e pour l'autocut
         long pos2emeLecture=0L;  // En cas de relecture du d�but du cube, m�morisation de la position initiale

         try {
            // Pas de 2�me passe possible => on m�morise dans buffer jusqu'� la tranche pr�vue
            // pour l'autocut
            if( !Aladin.STANDALONE ) {
               dejaCharge = true;
               int maxM = floor( (taille/4)/tailleImg );
               if( m>maxM ) m=maxM;   // Tranche trop loin, on prend plus pr�s du d�but sinon on va exploser la m�moire
               buffer = new byte[round(tailleImg * m) ];
               dis.readFully(buffer);
               //System.out.println("Chargement d'un coup de "+buffer.length+" octets ("+m+" tranches) du d�but du cube"     );

               System.arraycopy(buffer,(m-1)*tailleImg,pixelsOrigin,0,tailleImg);

               // Autocut en 2 passes possibles, on va travailler tranche par tranche �conomiser
               // la m�moire
            } else {
               dejaCharge=false;
               buffer = pixelsOrigin;

               // C'est pas un fichier local, il faut copier les donn�es dans le cache
               if( !cacheFromOriginalFile ) {
                  //System.out.println("Copie des "+m+" premi�res tranches dans le cache (par tranche de "+tailleImg+" octets)");
                  for( int i=0; i<m; i++ ) {
                     dis.readFully(buffer);
                     if( i==0 ) f=beginInCache(buffer);
                     else f.write(buffer);
                  }

                  // G�nial, un fichier local, on va juste faire un skip, et revenir en arri�re
               } else {
                  //System.out.println("Placement direct � la position en skippant "+((m-1)*tailleImg)+" octets du fichier original");
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
            String s=null;
            for( int i=0; i<depth; i++ ) {
               try {
                  s = (fromCanal ? getCanalValue(i): label);
                  mem = aladin.getMem();
                  if( !loadInRam && mem<Aladin.MARGERAM-10 ) throw new OutOfMemoryError();
                  if( aladin.levelTrace==4 ) {
                     delta = amem-mem;
                     if( Math.abs(adelta-delta)>0.2 || mem<Aladin.MARGERAM-1 ) {
                        aladin.trace(4,"PlanImageCube.cacheImageFits(): frame "+i+" freeRam="+mem+"MB delta="+delta+"MB");
                     }
                     amem=mem;
                     adelta=delta;
                  }

                  try { 
                     setBufPixels8(new byte[width*height]);
                     if( mem<Aladin.MARGERAM ) throw new OutOfMemoryError();
                  } catch( OutOfMemoryError e ) {
                     //                  e.printStackTrace();
                     Aladin.trace(4,"PlanImageCube.loadImageFits(): out of memory1 freeRam="+aladin.getMem()+"MB => inRam=false...");
                     loadInRam=false;
                     if( !freeRam() ) throw new OutOfMemoryError();
                     setBufPixels8(new byte[width*height]);
                  }

                  // Je suis avant la tranche qui a servi pour l'autocut
                  if( i<m ) {

                     // En une passe => tout est en m�moire
                     if( dejaCharge ) System.arraycopy(buffer,i*tailleImg,pixelsOrigin,0,tailleImg);
                     else {
                        if( i==0 ) {

                           // Il s'agit d'un fichier local et non du cache (puisque pos2emeLecture!=0),
                           // il faut r�ouvrir le fichier pour ne pas saboter le dataInputStream
                           if( pos2emeLecture!=0 ) {
                              //System.out.println("R�ouverture du fichier d'origine � la position "+pos2emeLecture);
                              f = new RandomAccessFile(cacheID,"r");
                           }
                           f.seek( pos2emeLecture );
                        }
                        f.readFully(pixelsOrigin);
                     }

                     // Je suis apr�s la tranche de l'autocut
                  }else {
                     dis.readFully(pixelsOrigin);
                     if( f!=null && !cacheFromOriginalFile ) f.write(pixelsOrigin);
                  }

                  to8bits(getBufPixels8(),0,pixelsOrigin,pixelsOrigin.length/npix,bitpix,
                        /*isBlank,blank,*/pixelMin,pixelMax,true);

                  invImageLine(width,height,getBufPixels8());
 

                  if( loadInRam && frameToBeLoad<=0 ) {
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
                  //                  e.printStackTrace();
                  Aladin.trace(4,"PlanImageCube.loadImageFits(): out of memory freeRam="+aladin.getMem()+"MB => inRam=false...");
                  loadInRam=false;
                  freeRam();
                  error="Out of memory";
                  addFrame(s,getBufPixels8(),null,cacheFromOriginalFile,cacheID,cacheOffset);
                  return false;
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
      z=depth/2;      // On commence � afficher la tranche m�diane du cube
      setPourcent(-1);
      return true;
   }

}
