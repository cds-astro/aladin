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

package cds.allsky;

import java.io.RandomAccessFile;
import java.util.Iterator;

import cds.fits.Fits;
import cds.moc.Healpix;
import cds.moc.SMoc;
import cds.tools.pixtools.Util;

/**
 * Action mémorisant les cutmin et cutmax en vue de la génération d'un HiPS PNG ou JPG
 * ou encore RGB. Fractionne le MOC en autant de régions distinctes. Et pour chacune d'elle
 * évalue le cutmin et cutmax sur un échantillon représentatif de pixels de cette région extrait
 * des tuiles FITS. Les seuils cutmin et cutmax sont mémorisés directement dans les entêtes 
 * des tuiles FITS en utilisant les mots clés CUTMIN et CUTMAX (valeurs raw)
 * @version 1.0 - février 2023
 * @author P.Fernique [CDS]
 *
 */
public class BuilderCut extends Builder {


   public BuilderCut(Context context) { super(context); }

   public Action getAction() { return Action.CUT; }
   
   public void run() throws Exception { build(); }

   public void build() throws Exception {
      context.loadMoc();
      String path = context.getOutputPath();
      SMoc moc = new SMoc();
      moc.read( path+cds.tools.Util.FS+Constante.FILE_MOC);
      int order = Util.getMaxOrderByPath(path);
      if( context.getOrder()==-1 ) context.setOrder(order);
      moc.setMocOrder( order );
      initStat((int)moc.getNbValues());
      SMoc [] mocs = moc.split(new Healpix(), true);
      context.info(mocs.length+" independent regions found");
      context.info("Updating FITS tile headers with CUTMIN and CUTMAX values by region...");
      
      double pourcentMin = Fits.POURCENT_MIN;
      double pourcentMax = Fits.POURCENT_MAX;
      if( context.hasPourcentCut( context.pixelRangeCut ) ) {
         pourcentMin=context.pixelRangeCut[Context.POURCMIN];
         pourcentMax=context.pixelRangeCut[Context.POURCMAX];
      }
      context.info("Adjusting cuts from "+cds.tools.Util.getPourcent(pourcentMin)
      +" to "+cds.tools.Util.getPourcent(pourcentMax)+" "
      + "of the pixel distribution for each region...");

      for( SMoc m: mocs ) setCut(path,order,m);
   }
   
   static int NB=0;
   
   /**
    * Analyse les tuiles FITS contenues dans le MOC pour en déterminer les cuts (cutmin..cutmax)
    * en se basant sur les premières tuiles dans le MOC, puis mémorise ces valeurs
    * dans l'entête de toutes les tuiles concernées
    * @param path L'emplacement du HiPS
    * @param order l'ordre le plus profond du HiPS
    * @param moc le MOC concerné
    * @throws Exception
    */
   private void setCut(String path, int order, SMoc moc) throws Exception {
      
      double [] cut = evaluateCut(path,order,moc);
      
      String cutmin = cut[Context.CUTMIN]+"";
      String cutmax = cut[Context.CUTMAX]+"";
      
      Iterator<Long> it = moc.valIterator();
      while( it.hasNext() ) {
         long npix = it.next();
         String file = Util.getFilePath(path,order,npix)+".fits";
         Fits fits = new Fits();
         int code;
         try {
            code = fits.loadHeaderFITS(file);
         } catch( Exception e ) {
            updateStat();
            continue;
         }
         if( code!=0 ) throw new Exception("FITS tile header update error (not a regular FITS tile))");
         byte [] buf1 = fits.headerFits.makeHeaderBuf();
         fits.headerFits.setKeyword("CUTMIN", cutmin);
         fits.headerFits.setKeyword("CUTMAX", cutmax);
         
         byte [] buf2 = fits.headerFits.makeHeaderBuf();
         if( buf2.length!=buf1.length ) throw new Exception("FITS tile header update error (>2880)");
         RandomAccessFile f = new RandomAccessFile(file,"rw");
         f.write(buf2);
         f.close();
         
         updateStat();
      }
   }
   
   /**
    * Evaluation des cutmin..cutmax pour les tuiles se trouvant dans le MOC
    * passé en paramètre. Effectue son évaluation sur les 4 première tuiles
    * du MOC, et en retourne les extrema
    * @param path Répertoire du HiPS
    * @param order ordre le plus profond du HiPS
    * @param moc le MOC de la sous-composante du HiPS à évaluer
    * @return { cutmin, cutmax }
    * @throws Exception
    */
   private double [] evaluateCut(String path, int order,SMoc moc) throws Exception {
      int i=0;
      Iterator<Long> it = moc.valIterator();
      Fits test=null;
      int offset=0;
      final int testW=1024;
      while( it.hasNext()  ) {
         long npix = it.next();
         String file = Util.getFilePath(path,order,npix)+".fits";
         Fits fits;
         try { 
            fits = new Fits(file);
            if( test==null ) {
               test = new Fits(testW,testW,fits.bitpix);
               test.setBlank(fits.blank);
               test.setBscale(fits.bscale);
               test.setBzero(fits.bzero);
               test.initBlank();
            }
            int size=fits.width*fits.height;
            for( int j=0; j<size && offset<testW*testW; j++ ) {
               double c = fits.getPixValDouble(fits.pixels, fits.bitpix, j);
               if( fits.isBlankPixel(c) ) continue;
               test.setPixValDouble(test.pixels, test.bitpix, offset++, c);
            }
            if( offset==testW*testW ) break;
            
         } catch( Exception e ) { i--; continue; }
      }
      double cutMinPourcent = -1;
      double cutMaxPourcent = -1;
      if( Context.hasPourcentCut( context.pixelRangeCut) ) {
         cutMinPourcent=context.pixelRangeCut[Context.POURCMIN];
         cutMaxPourcent=context.pixelRangeCut[Context.POURCMAX];
      }
      double [] cut = test.findAutocutRange(0,0,cutMinPourcent,cutMaxPourcent,true);
//      if( NB++<10 ) {
//         context.info(NB+") cut="+cut[0]+" .. "+cut[1]+" range="+cut[2]+" .. "+cut[3]);
//         moc.write("/Data/A/AMOC"+NB+".fits");
//         test.writeFITS("/Data/A/ATest"+NB+".fits");
//      }
      return cut;

   }

   public void validateContext() throws Exception {      
      validateOutput();
   }

   public boolean isAlreadyDone() { return false; }

   protected long startTime;                 // Date de lancement du calcul
   protected long totalTime;                 // Temps depuis le début du calcul
   private int statNbFile;

   private void initStat(int n) {
      context.setProgressMax(n);
      statNbFile=0;
      startTime = System.currentTimeMillis();
   }

   // Mise à jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }
   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, totalTime,1,1);
   }

}
