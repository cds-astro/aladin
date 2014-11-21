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

package cds.allsky;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

import cds.aladin.MyInputStream;
import cds.aladin.PlanImage;
import cds.fits.Fits;
import cds.fits.HeaderFits;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/** Permet de générer une arborescence de tuiles à partir d'une map Healpix
 * C'EST PAS TERMINE !!!
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderMapTiles extends Builder {
   private HeaderFits headerFits;
   private long initialOffsetHpx;
   private CacheFitsWriter cache;       // Cache en écriture des tuiels


   public BuilderMapTiles(Context context) {
      super(context);
   }
   
   public Action getAction() { return Action.MAPTILES; }
   
   public void run() throws Exception {
      build();
      if( !context.isTaskAborting() ) { (new BuilderMoc(context)).run();  context.info("MOC done"); }
      if( !context.isTaskAborting() && context.getOrder()>3 ) { (new BuilderTree(context)).run();  context.info("HiPS tree done"); }
      if( !context.isTaskAborting() ) {
         (new BuilderAllsky(context)).createAllSky(context.getOutputPath(), 3, 64, 0);
         context.info("Allsky done");
      }
   }
   
   public void validateContext() throws Exception {
      validateMap();
      validateOutput();
      // Il faudrait faire un validatCut avec les pixels de la map eux-même (ça va pas être simple)
   }
   
   private void validateMap() throws Exception {
      String map = context.getInputPath();
      MyInputStream in=null;
      try {
         in = new MyInputStream( new FileInputStream(map));
         headerFits = new HeaderFits(in);
         int naxis = headerFits.getIntFromHeader("NAXIS");
         // S'agit-il juste d'une entête FITS indiquant des EXTENSIONs
         if( naxis<=1 && headerFits.getStringFromHeader("EXTEND")!=null ) {
            // Je saute l'éventuel baratin de la première HDU
            try {
               int naxis1 = headerFits.getIntFromHeader("NAXIS1");
               in.skip(naxis1);
            } catch( Exception e) {}

            // On se cale sur le prochain segment de 2880
            in.skipOnNext2880();
            headerFits = new HeaderFits(in);
         }

         initialOffsetHpx = in.getPos();
      } finally { if( in!=null ) in.close(); }
   }
   
   
   private long startTime=0;
   private long nbRecord=-1,cRecord=-1;
   
   private void initStat(long nbRecord) {
      this.nbRecord = nbRecord;
      cRecord=0;
      context.setProgressMax(100);
      startTime = System.currentTimeMillis();
   }
   
   // Mise à jour des stats
   private void updateStat(long cRecord) {
      this.cRecord = cRecord;
   }

   /** Demande d'affichage des statistiques (via Task()) */
   public void showStatistics() {
      long now = System.currentTimeMillis();
      long cTime = now-startTime;
      if( cTime<2000 ) return;
      double pourcent = (double)cRecord/nbRecord;
      long totalTime = (long)( cTime/pourcent);
      long endsIn = totalTime-cTime;
      context.nlstat(Util.round(pourcent*100,1)+"% in " +Util.getTemps(cTime, true)+" endsIn="+Util.getTemps(endsIn, true)
            + " (record="+cRecord+"/"+nbRecord+")");
      if( cache!=null && cache.getStatNbOpen()>0 ) context.stat(cache+"");
   }
   
   private void build() throws Exception {
      int idxTForm      = 0;                                          // Indice du TFORM concerné
      String path       = context.getOutputPath();                    // Répertoire pour la génération du HiPS
      long nside        = headerFits.getIntFromHeader("NSIDE");       // NSIDE de la map
      int sizeRecord;                                                 // Taille des enregistrements
      long naxis1       = headerFits.getIntFromHeader("NAXIS1");      // Longueur des lignes
      long naxis2       = headerFits.getIntFromHeader("NAXIS2");      // Nombre de colonnes
      int nField        = headerFits.getIntFromHeader("TFIELDS");     // Nombre de champs par enregistrement
      double badData    = Double.NaN;                                 // Une valeur BADDATA particulière ?
      String frame      = "G";                                        // Référence spatiale (G par défaut)
      double bzeroOrig  = 0;                                          // BZERO éventuel
      double bscaleOrig = 1;                                          // BSCALE éventuel
      double blankOrig  = Double.NaN;                                 // BLANK éventuel
      boolean isPartial=false;                                        // S'agit-il d'un HEALPIX partiel ou global
      boolean isNested=true;                                          // Mode d'encodage HEALPix
      String s;
      
      try { badData  = Double.parseDouble(headerFits.getStringFromHeader("BAD_DATA")); } catch( Exception e ) { }
      try { s = headerFits.getStringFromHeader("COORDSYS"); if( s!=null ) frame=s;     } catch (Exception e ) { }
      try { bzeroOrig    = headerFits.getDoubleFromHeader("BZERO");                    } catch( Exception e ) { }
      try { bscaleOrig   = headerFits.getDoubleFromHeader("BSCALE");                   } catch( Exception e ) { }
      try { blankOrig    = headerFits.getDoubleFromHeader("BLANK");                    } catch( Exception e ) { }
      try { 
         s    = headerFits.getStringFromHeader("ORDERING");    
         if( s!=null && s.equals("RING") ) isNested=false;
      } catch( Exception e ) { }
      try { 
         s = headerFits.getStringFromHeader("OBJECT");
         if( s!=null ) { if( s.equalsIgnoreCase("PARTIAL") ) isPartial=true; }
         else {
            s = headerFits.getStringFromHeader("TTYPE1");
            if( s.equalsIgnoreCase("PIXEL") ) isPartial=true;
         }
      } catch( Exception e ) { }
      context.setFrameName(frame);
      
      // A modifier par la suite pour le mettre en paramètre
      if( isPartial ) {
         idxTForm=1;
      }
      context.info("Map "+(isPartial?" PARTIAL":"")+" nside="+nside+" ordering="+(isNested?"NESTED":"RING")+" frame="+context.getFrameName()
            +" pixelIndex="+idxTForm);
      
      // Détermination du nombre de niveaux et de la taille des tuiles level 3
      int maxTileWidth = 512;
      if( nside<maxTileWidth ) maxTileWidth= (int) nside;      // Pour pouvoir charger des "tout-petits cieux"
      int tileWidth = 2*maxTileWidth;
      int maxOrder;
      do {
         tileWidth /= 2;
         maxOrder = getLevelImage(nside, tileWidth);
      } while( maxOrder<3 );
      
      context.info("HiPS maxOrder="+maxOrder+" tileWidth="+tileWidth);
      context.setOrder(maxOrder);
      
      // Détermination de la taille des enregistrements et de la position
      // de chaque valeur de pixel Healpix (ainsi que l'indication de sa position si mode PARTIAL)
      // et du nombre de valeur par segment si c'est le cas
      char type='D';
      char typePixel='J';
      int offsetPix=0;
      int offsetVal=0;
      int nbValPerSegment=1;
      sizeRecord=0;
      int sizeFieldVal=8,sizeFieldPix=4;
      for( int i=1; i<=nField; i++ ) {
         String form = headerFits.getStringFromHeader("TFORM"+i).trim();
         if( i==idxTForm+1) {
            type = form.charAt(form.length()-1);
            sizeFieldVal = Util.binSizeOf(type, 1);
            if( form.length()>1 ) nbValPerSegment = Integer.parseInt(form.substring(0,form.length()-1));
            offsetVal=sizeRecord;
         } else if( isPartial && i==0 ) {
            typePixel = form.charAt(form.length()-1);
            sizeFieldPix = Util.binSizeOf(typePixel, 1);
            offsetPix=sizeRecord;
         }
//         sizeRecord+=Util.binSizeOf(form.charAt(form.length()-1),1);
         sizeRecord+=Util.binSizeOf(form);
      }
      
      long nRecord = (naxis1 * naxis2) / sizeRecord;
      initStat(nRecord);
      
      // Détermination des paramètres FITS BZERO,BSCALE,BLANK en sortie 
      int bitpixOrig = getBitpixFromFormat(type);
      context.setBitpixOrig(bitpixOrig);
      
      context.setBlankOrig(blankOrig);
      context.setBZeroOrig(bzeroOrig);
      context.setBScaleOrig(bscaleOrig);
       
      context.info("Original BITPIX="+bitpixOrig+" BLANK="+blankOrig + (bzeroOrig!=0 ?" BZERO="+bzeroOrig:"") + (bscaleOrig!=1 ?" BSCALE="+bscaleOrig:""));
      context.initParameters();
      
      double bscale    = context.getBScale();
      double bzero     = context.getBZero();
      double blank     = context.getBScale();
      int bitpix       = context.getBitpix();
      double [] cutOrig= context.getCutOrig();
      double [] cut    = context.getCut();
      
      int bitpixP       = getBitpixFromFormat(typePixel);
      long nbPixPerTile = (long)( tileWidth * tileWidth );
      int tileOrder     = (int) CDSHealpix.log2(tileWidth);
      int hpx2xy[]      = cds.tools.pixtools.Util.createHpx2xy(tileOrder);
      
      
      context.info("nbRecord="+nRecord+" nbValPerSegment="+nbValPerSegment+" valType="+type);
      
      // Lecture séquentiel enr par enr du fichier Map et création ou maj des tuiles corresdantes
      // On retient la dernière tuile pour gagner du temps
      RandomAccessFile f = null;
      Fits fits=null, lastFits=null;
      String lastFile="";
      cache = new CacheFitsWriter( 512*1024*1024L);
      
      int sizeBuf = 512;
      int nbRecordInBuf = sizeBuf/sizeRecord;
      if( nbRecordInBuf<1 ) nbRecordInBuf=1;
      int cRecordInBuf=nbRecordInBuf;
      sizeBuf = nbRecordInBuf * sizeRecord;
      byte buf [] = new byte[ sizeBuf ];
      
      System.out.println("nbRecordInBuf="+nbRecordInBuf+" sizeBuf="+sizeBuf);
      
      long count=0,npix;
      try {
         f = new RandomAccessFile(context.getInputPath(), "rw");
         f.seek(initialOffsetHpx);
         
         for( long n=0; n<nRecord; n++, cRecordInBuf++ ) {
            
            if( cRecordInBuf==nbRecordInBuf ) {
               f.readFully(buf);
               cRecordInBuf = 0;
            }
            
            if( context.isTaskAborting() ) throw new Exception("Task abort !");
            updateStat(n);
            

            for( int i=0; i<nbValPerSegment; i++ ){

               // Détermination du numéro de pixel HEALPix
               // soit séquentiel, soit explicite dans le cas d'une map PARTIAL
               // en prenant en compte l'ordering RING ou NESTED
               if( isPartial ) npix = getNpix(buf,bitpixP,offsetPix + cRecordInBuf*sizeRecord +i*sizeFieldPix);
               else npix = count++;
               if( !isNested ) npix = CDSHealpix.ring2nest(nside,npix);

               // Récupération de la valeur associée
               double val = getVal(buf,bitpixOrig,offsetVal + cRecordInBuf*sizeRecord +i*sizeFieldVal);
               if( val==badData ) val=blankOrig;

               // Changement de bitpix et/ou d'échelle
               if( bitpix!=bitpixOrig ) {
                  val = Double.isNaN(val) || val==blankOrig ? blank
                        : val<=cutOrig[2] ? cut[2]
                              : val>=cutOrig[3] ? cut[3]
                                    : (val-cutOrig[2])*context.coef + cut[2];
                              if( bitpix>0 && (long)val==blank && val!=blank ) val+=0.5;
               }

               // Ouverture de la tuile correspondante au pixel HEALPix
               long npixFile = npix / nbPixPerTile ;
               String file = cds.tools.pixtools.Util.getFilePath(path, maxOrder, npixFile)+".fits";

               // Continue-t-on à travailler sur la tuile précédente ?
               if( file.equals(lastFile) ) fits=lastFits;

               // Sinon, on va aller la chercher
               else {

                  // Récupération d'une tuile déjà existante
                  try { fits = cache.getFits(file); }

                  // Création de la tuile requise
                  catch( FileNotFoundException e ) {
                     fits = new Fits(tileWidth,tileWidth,bitpix);
                     fits.setBlank(blank);
                     fits.setBzero(bzero);
                     fits.setBscale(bscale);
                     for( int y=0; y<tileWidth; y++) {
                        for( int x=0; x<tileWidth; x++ )  fits.setPixelDouble(x, y, blank);
                     }
                     cache.addFits(file, fits);
                  }
                  lastFits = fits;
               }

               // Détermination de la position dans la tuile
               // et écriture de la valeur
               long startIdx =  npixFile * (long)tileWidth * (long)tileWidth;
               int idx = hpx2xy[ (int)(npix-startIdx) ];

               fits.setPixValDouble(fits.pixels, bitpix, idx, val);

//               if( n==0 && i<5 ) System.out.println("npix="+npix+" => npixFile "+npixFile+" idx="+idx+" val="+val);
            }
         }
         
      } finally {
         if( f!=null ) f.close(); 
      }
      
      // On flushe le cache
      cache.close();
   }
   
   static final public long getNpix(byte[] t,int bitpix,int pos) {
      try {
         switch(bitpix) {
            case   8: return PlanImage.getByte(t,pos);
            case  16: return PlanImage.getShort(t,pos);
            case  32: return PlanImage.getInt(t,pos);
            case  64: return PlanImage.getLong(t,pos);
         }
         return 0;
      } catch( Exception e ) { return 0; }
   }
   
   static final public double getVal(byte[] t,int bitpix,int pos) {
      try {
         switch(bitpix) {
            case   8: return PlanImage.getByte(t,pos);
            case  16: return PlanImage.getShort(t,pos);
            case  32: return PlanImage.getInt(t,pos);
            case  64: return PlanImage.getLong(t,pos);
            case -32: return PlanImage.getFloat(t,pos);
            case -64: return PlanImage.getDouble(t,pos);
         }
         return Double.NaN;
      } catch( Exception e ) { return Double.NaN; }
   }
   
   private int getLevelImage(long nside, long tileWidth) {
      long nbPix = 12L * nside * nside;
      long nbNeedTiles = nbPix / (tileWidth * tileWidth);
      if( nbNeedTiles < 1 ) return -1;
      long nsideTile = (long) Math.sqrt( nbNeedTiles/12L);
      return (int) CDSHealpix.log2(nsideTile);
   }

   // Retourne le BITPIX correspondondant à un type de données
   private int getBitpixFromFormat(char t) throws Exception {
      int bitpix = t=='B' ? 8 : t=='I' ? 16 : t=='J' ? 32 : t=='K' ? 64
            : t=='E' ? -32 : t=='D' ? -64 : 0;
      if( bitpix==0 ) throw new Exception("Unsupported data type => ["+t+"]");
      return bitpix;
   }

   
   public boolean isAlreadyDone() { return false; }
   

}