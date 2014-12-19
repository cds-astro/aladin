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

import javax.swing.JProgressBar;

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
   private CacheFitsWriter cache;       // Cache en écriture des tuiels

   protected int bitpixOrig=-1;
   protected int maxOrder=-1;
   private long nside=-1;               // NSIDE de la map
   private String ordering=null;        // ORDERING de la map
   private long initialOffsetHpx;       // Position dans la map des DATA

   private long startTime=0;
   private long nbRecord=-1,cRecord=-1;
   private String info="";

   private Builder b=null;

   public BuilderMapTiles(Context context) {
      super(context);
   }

   public Action getAction() { return Action.MAPTILES; }

   public void run() throws Exception {
      build();

      if( !context.isTaskAborting() ) {
         (b=new BuilderMoc(context)).run(); b=null;

         if( context.getOrder()<=3 ) { (new BuilderAllsky(context)).run(); context.info("Allsky done"); }
         else { (new BuilderTree(context)).run();  context.info("HiPS tree done"); }

         context.setProgressLastNorder3(1);
      }
   }

   public void validateContext() throws Exception {
      validateMap();
      validateOutput();

      if( context instanceof ContextGui ) {
         JProgressBar bar = ((ContextGui)context).mainPanel.getProgressBarTile();
         context.setProgressBar(bar);
      }
   }

   protected void validateMap() throws Exception {
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

               // Peut être une vieille version de map HEALPix dont les paramètres
               // sont dans la HDU0
               try { nside = headerFits.getIntFromHeader("NSIDE"); }
               catch( Exception e) {}
               try { ordering = headerFits.getStringFromHeader("ORDERING"); }
               catch( Exception e) {}

               in.skip(naxis1);
            } catch( Exception e) {}

            // On se cale sur le prochain segment de 2880
            in.skipOnNext2880();
            headerFits = new HeaderFits(in);
            context.setHeader(headerFits);
         }

         initialOffsetHpx = in.getPos();
      } finally { if( in!=null ) in.close(); }
   }

   private void initStat(long nbRecord) {
      this.nbRecord = nbRecord;
      cRecord=0;
      context.setProgressMax(nbRecord);
      startTime = System.currentTimeMillis();
   }

   // Mise à jour des stats
   private void updateStat(long cRecord) {
      this.cRecord = cRecord;
   }

   /** Demande d'affichage des statistiques (via Task()) */
   public void showStatistics() {
      if( b!=null ) { b.showStatistics(); return; }

      if( nbRecord<=0 ) return;
      long now = System.currentTimeMillis();
      long cTime = now-startTime;
      if( cTime<2000 ) return;

      context.showMapStat(cRecord, nbRecord, cTime,cache, info);
   }

   protected void build() throws Exception { build(false); }
   protected void build(boolean flagSimpleLook) throws Exception {
      int idxTForm      = 0;                                          // Indice du TFORM concerné
      String path       = context.getOutputPath();                    // Répertoire pour la génération du HiPS
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

      // récupération du NSIDE si pas déjà fait dans la première HDU
      if( nside==-1 ) nside = headerFits.getIntFromHeader("NSIDE");

      try { badData  = Double.parseDouble(headerFits.getStringFromHeader("BAD_DATA")); } catch( Exception e ) { }
      try { s = headerFits.getStringFromHeader("COORDSYS"); if( s!=null ) frame=s;     } catch (Exception e ) { }
      try { bzeroOrig    = headerFits.getDoubleFromHeader("BZERO");                    } catch( Exception e ) { }
      try { bscaleOrig   = headerFits.getDoubleFromHeader("BSCALE");                   } catch( Exception e ) { }
      try { blankOrig    = headerFits.getDoubleFromHeader("BLANK");                    } catch( Exception e ) { }

      // récupération du ORDERING si pas déjà fait dans la première HDU
      try {
         s    = ordering!=null ? ordering : headerFits.getStringFromHeader("ORDERING");
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
      if( isPartial )  idxTForm=1;

      info = "HEALPix FITS map "+(isPartial?" PARTIAL":"")+" nside="+nside+" ordering="+(isNested?"NESTED":"RING")+" frame="+context.getFrameName();
      if( !flagSimpleLook ) context.info( info );

      // Détermination du nombre de niveaux et de la taille des tuiles level 3
      int maxTileWidth = context.getTileSide();
      if( nside<maxTileWidth ) maxTileWidth= (int) nside;      // Pour pouvoir charger des "tout-petits cieux"

      // Détermination automatique du order et de la taille de la tuile en fonction de la résolution initiale
      int tileWidth;
      if( context.getOrder()==-1 ) {
         tileWidth = 2*maxTileWidth;
         do {
            tileWidth /= 2;
            maxOrder = getLevelImage(nside, tileWidth);
         } while( maxOrder<3 );

         // Order explicite
      } else {
         maxOrder=context.getOrder();
         tileWidth=maxTileWidth;
      }

      long nsideFile = CDSHealpix.pow2(maxOrder);

      if( !flagSimpleLook ) context.info("HiPS maxOrder="+maxOrder+" tileWidth="+tileWidth);
      context.setOrder(maxOrder);

      // en cas de sous-échantillonnage, il faudra convertir le numéro HEALPix de chaque pixel de la map sur celui du HiPS final
      int div = (int) ( CDSHealpix.log2(nside) - CDSHealpix.log2(tileWidth * nsideFile) ) *2;

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
         } else if( isPartial && i==1 ) {
            typePixel = form.charAt(form.length()-1);
            sizeFieldPix = Util.binSizeOf(typePixel, 1);
            offsetPix=sizeRecord;
         }
         sizeRecord+=Util.binSizeOf(form);
      }

      long nbRecord = (naxis1 * naxis2) / sizeRecord;

      // Détermination des paramètres FITS BZERO,BSCALE,BLANK en sortie
      bitpixOrig = getBitpixFromFormat(type);

      // C'était juste pour initialiser maxOrder et bitpixOrig
      if( flagSimpleLook ) return;

      context.setBitpixOrig(bitpixOrig);
      context.setBlankOrig(blankOrig);
      context.setBZeroOrig(bzeroOrig);
      context.setBScaleOrig(bscaleOrig);

      context.info("Original BITPIX="+bitpixOrig+" BLANK="+blankOrig + (bzeroOrig!=0 ?" BZERO="+bzeroOrig:"") + (bscaleOrig!=1 ?" BSCALE="+bscaleOrig:""));

      double bscale    = context.getBScale();
      double bzero     = context.getBZero();
      double blank     = context.getBScale();
      int bitpix       = context.getBitpix();
      double [] pixelRangeCut = context.getPixelRangeCut();
      double [] cutOrig=null;
      double [] cut=null;

      int bitpixP       = getBitpixFromFormat(typePixel);
      long nbPixPerTile = tileWidth * tileWidth;
      int tileOrder     = (int) CDSHealpix.log2(tileWidth);
      int hpx2xy[]      = cds.tools.pixtools.Util.createHpx2xy(tileOrder);

      context.info("MAP structure: nbRecord="+nbRecord+" nbValPerSegment="+nbValPerSegment+" valType="+type);

      // Lecture séquentiel enr par enr du fichier Map et création ou maj des tuiles corresdantes
      // On retient la dernière tuile pour gagner du temps
      RandomAccessFile f = null;
      Fits fits=null, lastFits=null;
      String lastFile="";
      long nbTiles = 12*nsideFile*nsideFile;
      long tileMem = (nbPixPerTile * (Math.abs(bitpix)/8) + 4000);
      long reqMem = nbTiles * tileMem;
      long mem = (long)(context.getMem() * 0.75);
      if( reqMem<mem ) mem=reqMem;
      context.info("Writer cache RAM="+Util.getUnitDisk(mem)+" ("+(mem/tileMem)+" tiles)");
      cache = new CacheFitsWriter( mem );
      cache.setContext( context );

      int sizeBuf = 512;
      int nbRecordInBuf = sizeBuf/sizeRecord;
      if( nbRecordInBuf<1 ) nbRecordInBuf=1;
      int cRecordInBuf;
      sizeBuf = nbRecordInBuf * sizeRecord;
      byte buf [] = new byte[ sizeBuf ];

      //      System.out.println("nbRecordInBuf="+nbRecordInBuf+" sizeBuf="+sizeBuf);

      long count,npix;
      int posSample;
      Fits sample = null;
      int nbValInSample=0;
      try {

         f = new RandomAccessFile(context.getInputPath(), "rw");

         // En cas de changement de bitpix sans valeur de cut indiquées
         // on procéde en deux tours, l'un pour mesurer la dynamique,
         // l'autre pour HiPSizer
         boolean missingCut   = pixelRangeCut==null || Double.isNaN(pixelRangeCut[0]);
         boolean missingRange = pixelRangeCut==null || Double.isNaN(pixelRangeCut[2]);
         int nbStep =  missingCut || missingRange ? 2 : 1;
         int gapSample=1;

         if( nbStep==2 ) {
            int sizeVal = Math.abs(bitpixOrig)/8;
            long nbPossibleVal = mem/sizeVal;
            int nbVal = nbValInSample = (int)( nbRecord * nbValPerSegment );
            int w = (int) Math.sqrt(nbValInSample);
            for( gapSample=1; nbValInSample>nbPossibleVal || w>4096; gapSample++ ) {
               nbValInSample = nbVal/gapSample;
               w = (int) Math.sqrt(nbValInSample);
            }
            nbValInSample = w*w;
            sample = new Fits(w,w,bitpixOrig);
            sample.setBlank(blankOrig);
            sample.setBzero(bzeroOrig);
            sample.setBscale(bscaleOrig);
            context.info("Pixel dynamic estimation on "+nbValInSample+" values"
                  +(gapSample>1?" (gapSample="+gapSample+")":"")+"...");
         }

         STEP2: for( int step=0; step<nbStep; step++ ) {

            if( step==nbStep-1 ) {

               initStat(nbRecord);

               if( sample!=null ) context.initCut(sample);
               cutOrig= context.getCutOrig();
               if( pixelRangeCut!=null && !Double.isNaN(pixelRangeCut[0]) ) {
                  if( cutOrig==null ) cutOrig = new double[5];
                  cutOrig[0]=pixelRangeCut[0];
                  cutOrig[1]=pixelRangeCut[1];
               }
               if( pixelRangeCut!=null && !Double.isNaN(pixelRangeCut[2]) ) {
                  if( cutOrig==null ) cut = new double[5];
                  cutOrig[2]=pixelRangeCut[2];
                  cutOrig[3]=pixelRangeCut[3];
               }
               context.setCutOrig(cutOrig);
               context.initParameters();
               cut    = context.getCut();
               if( bitpixOrig!=bitpix ) {
                  blank = context.getBlank();
                  bzero = context.getBZero();
                  bscale = context.getBScale();
               }
               if( sample!=null )sample.free();
               context.setValidateCut(true);
               context.info("Pixel dynamic range=["+ip(cut[2],bzero,bscale)+" .. "+ip(cut[3],bzero,bscale)
                     +"] cut=["+ip(cut[0],bzero,bscale)+" .. "+ip(cut[1],bzero,bscale)+"]");
               context.setTileOrder(tileOrder);
            }

            posSample=0;
            count=0;
            cRecordInBuf=nbRecordInBuf;
            f.seek(initialOffsetHpx);

            for( long n=0; n<nbRecord; n++, cRecordInBuf++ ) {

               if( cRecordInBuf==nbRecordInBuf ) {
                  f.readFully(buf);
                  cRecordInBuf = 0;
               }

               if( context.isTaskAborting() ) throw new Exception("Task abort !");
               if( step==nbStep-1 ) updateStat(n);

               for( int i=0; i<nbValPerSegment; i++ ){

                  // Détermination du numéro de pixel HEALPix
                  // soit séquentiel, soit explicite dans le cas d'une map PARTIAL
                  // en prenant en compte l'ordering RING ou NESTED
                  if( isPartial ) npix = getNpix(buf,bitpixP,offsetPix + cRecordInBuf*sizeRecord +i*sizeFieldPix);
                  else npix = count;
                  count++;

                  if( !isNested ) npix = CDSHealpix.ring2nest(nside,npix);

                  // Récupération de la valeur associée
                  double val = getVal(buf,bitpixOrig,offsetVal + cRecordInBuf*sizeRecord +i*sizeFieldVal);
                  if( val==badData ) val=blankOrig;

                  // Estimation de la dynamique et passage à la deuxième étape
                  // dès que l'échantillon est suffisant
                  if( step==0 && nbStep==2 ) {
                     if( posSample>=sample.width*sample.width ) continue STEP2;

                     if( count%gapSample==0 ) sample.setPixValDouble(sample.pixels, bitpixOrig, posSample++, val);
                     continue;
                  }

                  // Changement de bitpix et/ou d'échelle
                  if( bitpix!=bitpixOrig ) {
                     val = Double.isNaN(val) || val==blankOrig ? blank
                           : val<=cutOrig[2] ? cut[2]
                                 : val>=cutOrig[3] ? cut[3]
                                       : (val-cutOrig[2])*context.coef + cut[2];
                                 if( bitpix>0 && (long)val==blank && val!=blank ) val+=0.5;
                  }

                  if( div!=0 ) npix = npix >>> div;

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
      long startIdx =  npixFile * tileWidth * tileWidth;
      int idx = hpx2xy[ (int)(npix-startIdx) ];

      fits.setPixValDouble(fits.pixels, bitpix, idx, val);

      //                                 if( count<5 ) System.out.println("npix="+npix+" => npixFile "+npixFile+" idx="+idx+" val="+val);
               }

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