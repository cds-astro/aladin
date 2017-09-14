// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

import java.awt.image.IndexColorModel;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import cds.aladin.CanvasColorMap;
import cds.aladin.KernelList;
import cds.aladin.MyProperties;
import cds.aladin.PlanImage;
import cds.aladin.Tok;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

public class BuilderRgb extends BuilderTiles {


   private String [] inputs;         // Les paths des HiPS red, green, blue
   private int [] cubeIndex;         // Les indices des frames des composantes red, green, blue en cas de cubes d'origine (0 par défaut)
   private HealpixMoc [] moc;        // Les Mocs de chaque composante
   private MyProperties [] prop;     // Les propriétés de chaque composante
   private String [] labels;         // Les labels des HiPS red, green blue
   private String [] transfertFcts;  // Fonction de transfert à appliquer à chaque composante
   private double [] pixelMin,pixelMax,pixelMiddle; // valeur min, middle et max des pixels à utiliser pour définir la colormap et la plage
   // du pixelCut - les valeurs sont exprimées en valeur coding (bzero et bscale non appliqués)

   private String output;
   private int width=-1;
   private double [] blank;
   private double [] bscale;
   private double [] bzero;
   private byte [][] tcm;
   private int [] bitpix;
   private int maxOrder=-1;
   private String frame=null;
   private int missing=-1;
   private boolean flagGauss;
   
   private int statNbFile;

   private Mode coaddMode=Mode.REPLACETILE;
   private int format;

   public BuilderRgb(Context context) {
      super(context);
   }

   public Action getAction() { return Action.RGB; }

   public void run() throws Exception {
      Fits.setToolKit();
      build();
      context.setPropriete(Constante.KEY_HIPS_PROCESS_HIERARCHY, context.getJpegMethod().toString().toLowerCase());

      if( !context.isTaskAborting() ) (new BuilderMoc(context)).createMoc(output);
      if( !context.isTaskAborting() ) { (new BuilderAllsky(context)).run(); context.info("ALLSKY file done"); }
   }
   
   protected void activateCache(long size,long sizeCache) { }
   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showRGBStat(statNbFile, totalTime,statNbThread,statNbThreadRunning);
//      if( !(context instanceof ContextGui ) ) super.showStatistics();
   }
   
   /** Mise à jour de la barre de progression en mode GUI */
   protected void setProgressBar(int npix) { context.setProgress(npix); }

   private void initStat() {
      context.setProgressMax(768);
      statNbFile=0;
      startTime = System.currentTimeMillis();
   }

   // Mise à jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }
   
   
   protected int getBitpix0() { return 0; }

   public void build() throws Exception {
      initStat();
      super.build();
   }

   
   // Transforme un path à la mode cube (ex: xxx/Allsky.fits[1]) par sa
   // syntaxe HiPS (ex: xxx/Allsky_1.fits)
   // sinon retourne le path original
   private String pathCube2path(String path) {
      if( !path.endsWith("]") ) return path;
      int i = path.lastIndexOf('[');
      if( i<=0 ) return path;
      try {
         int n = Integer.parseInt(path.substring(i+1, path.length()-1) );
         String suff = n==0 ? "": ("_"+n);
         int j = path.lastIndexOf(".",i);
         if( j==-1 ) return path.substring(0,i)+suff;   // s'il n'y a pas d'extension, ou met _nnn à la fin
         return path.substring(0,j)+suff+path.substring(j,i);  // sinon on l'insère juste avant l'extension
         
      } catch( Exception e) { }
      
      return path;
   }

   // Supprime le suffixe d'un path à la mode cube (ex: xxx/Allsky.fits[1] => xxx/Allsky.fits
   // sinon retourne le path original
   private String removeCubeSuffixe( String path ) {
      if( !path.endsWith("]") ) return path;
      int i = path.lastIndexOf('[');
      if( i<=0 ) return path;
      try {
         Integer.parseInt(path.substring(i+1, path.length()-1) );
         return path.substring(0,i);
      } catch( Exception e) { }
      return path;
   }
   
   // Retourne l'indice cube d'un path  à la mode cube (ex: xxx/Allsky.fits[1] => xxx/Allsky.fits
   // sinon retourne 0
   int getCubeIndex( String path ) {
      if( !path.endsWith("]") ) return 0;
      int i = path.lastIndexOf('[');
      if( i<=0 ) return 0;
      try {
         return Integer.parseInt(path.substring(i+1, path.length()-1) );
      } catch( Exception e) { }
      return 0;
   }
   
   public void validateContext() throws Exception {

      String path = context.getRgbOutput();
      
      format= context.getRgbFormat();
      coaddMode = context.getMode();
      if( coaddMode!=Mode.KEEPTILE && coaddMode!=Mode.REPLACETILE ) {
         if( context instanceof ContextGui ) {
            context.setMode(Mode.REPLACETILE);
         } else throw new Exception("Only KEEPTILE and REPLACETILE modes are supported for RGB HiPS generation");

      }

      String pathRef=null;

      inputs = new String[3];
      cubeIndex = new int[3];
      labels = new String[3];
      moc = new HealpixMoc[3];
      prop = new MyProperties[3];
      pixelMin = new double[3];
      pixelMiddle = new double[3];
      pixelMax = new double[3];
      transfertFcts = new String[3];

      bitpix = new int[3];
      for( int c=0; c<3; c++ ) bitpix[c]=0;
      blank = new double[3];
      bzero = new double[3];
      bscale = new double[3];
      tcm = new byte[3][];

      // Un frame indiqué => on mémorise sa première lettre.
      if( context.hasFrame() ) frame = context.getFrameName();

      for( int c=0; c<3; c++ ) {
         inputs[c] = removeCubeSuffixe( context.plansRGB[c] );
         cubeIndex[c] = getCubeIndex( context.plansRGB[c] );
         if( inputs[c]==null ) {
            if( missing!=-1 ) throw new Exception("HiPS RGB generation required at least 2 original components");
            missing=c;
         } else {
            
            if( !context.isExistingAllskyDir( inputs[c] ) ) {
               throw new Exception("Input HiPS missing ["+inputs[c]+"]");
            }
                  
            if( pathRef==null ) pathRef=inputs[c];

            // Récupération des propriétés
            prop[c] = loadProperties( inputs[c] );

            // recupération du label de la composante (juste pour de l'info)
            labels[c] = getLabelFromProp( prop[c], inputs[c] );

            // Analyse des paramètres de descriptions du pixelCut et de la colormap
            String s = context.cmsRGB[c];
            if( s==null ) {
               s = getCmParamFromProp( prop[c] );
               if( s==null ) throw new Exception("Unknown pixelcut for "+labels[c]);
            }
            setCmParamExact(s, c);

            // Mémorisation de l'ordre le plus profond
            int o = getOrderFromProp( prop[c], inputs[c] );
            if( o>maxOrder ) maxOrder=o;

            // Ajustement de la région qu'il faudra calculer
            HealpixMoc m = moc[c] = loadMoc( inputs[c] );
            if( context.moc==null ) context.moc = m;
            else context.moc = context.moc.union(m);
            
           // Vérification de la cohérence des systèmes de coordonnées
            String f = getFrameFromProp( prop[c] );
            if( frame==null ) frame=f;
            else if( !frame.equals(f) ) throw new Exception("Uncompatible coordsys for "+labels[c]);

         }
      }
      
      // Si le répertoire de destination est absent, je donne une valeur par défaut
      if( path==null ) {
         int n = pathRef.length()-1;
         int offset = Math.max(pathRef.lastIndexOf('/',n),pathRef.lastIndexOf('\\',n));
         path = pathRef.substring(0,offset+1)+"RGBHiPS";
         context.warning("Missing \"out\" parameter. Assuming \""+path+"\"");
      }
      this.output = path;

      if( context instanceof ContextGui ) {
         HealpixMoc m = context.moc;
         ((ContextGui)context).mainPanel.clearForms();
         context.moc = m;
      }

      // Pas d'ordre indiqué => on va utiliser l'ordre de la composante la plus profonde
      if( context.getOrder()==-1 ) {
         context.setOrder(maxOrder);
         context.info("Using order = "+maxOrder);
      } else maxOrder=context.getOrder();

      // Mise à jour du context
      if( !context.hasFrame() ) {
         context.setFrameName(frame);
         context.info("Using coordys = "+context.getFrameName());
      }
      context.setOutputPath(path);

      context.setBitpixOrig(0);


      // mémorisation des informations de colormaps
      for( int c=0; c<3; c++) {
         if( c==missing ) continue;
         String info =  labels[c]+" ["+pixelMin[c]+" "+pixelMiddle[c]+" "+pixelMax[c]+" "+transfertFcts[c]+"]";
         if( c==0 ) context.redInfo=info;
         else if( c==1 ) context.greenInfo=info;
         else context.blueInfo=info;
      }

      // détermination de la zone à calculer
      if( context.mocArea!=null ) context.moc = context.moc.intersection( context.mocArea );
      
      // Faut-il un filtre gaussien
      if( context.gaussFilter ) context.info("Gauss filter activated...");
      flagGauss = context.gaussFilter;

      context.writeMetaFile();
   }

   // Chargement d'un MOC, et par défaut, d'un MOC couvrant tout le ciel
   private HealpixMoc loadMoc( String path ) throws Exception {
      String s = path+Util.FS+"Moc.fits";
      if( !(new File(s)).canRead() ) return new HealpixMoc("0/0-11");
      HealpixMoc m = new HealpixMoc();
      m.read(s);
      return m;
   }

   // Récupération de la première lettre du frame à partir de son fichier de properties
   // et à défaut c'est du Galactic
   private String getFrameFromProp( MyProperties prop ) throws Exception {
      String s = prop.getProperty( Constante.KEY_HIPS_FRAME );
      if( s==null ) s = prop.getProperty( Constante.OLD_HIPS_FRAME );
      if( s==null ) s="G";
      return context.getCanonicalFrameName(s);
   }

   // Récupération du pixelcut d'une composante à partir de son fichier de properties
   private String getCmParamFromProp( MyProperties prop ) throws Exception {
      String s = prop.getProperty( Constante.KEY_HIPS_PIXEL_CUT );
      if( s==null ) s = prop.getProperty( Constante.OLD_HIPS_PIXEL_CUT );
      return s;
   }

   // Récupération du label d'une composante à partir de son fichier de properties
   // et à défaut de son nom de répertoire
   private String getLabelFromProp(MyProperties prop,String path) throws Exception {
      String s=null;
      if( prop!=null ) {
         s = prop.getProperty( Constante.KEY_OBS_COLLECTION);
         if( s==null ) prop.getProperty( Constante.OLD_OBS_COLLECTION);
      }
      if( s==null ) {
         int offset = path.lastIndexOf('/');
         if( offset==-1 ) offset = path.lastIndexOf('\\');
         s=path.substring(offset+1);
      }
      return s;
   }

   // Récupération du order d'une composante à partir de son fichier de properties
   // et à défaut en scannant son répertoire
   private int getOrderFromProp(MyProperties prop,String path) throws Exception {
      int order=-1;
      String s=null;
      if( prop!=null ) {
         s = prop.getProperty( Constante.KEY_HIPS_ORDER);
         if( s==null ) prop.getProperty( Constante.OLD_HIPS_ORDER);
         try { order = Integer.parseInt(s); } catch( Exception e) {}
      }
      if( order==-1 ) order = Util.getMaxOrderByPath( path );
      return order;
   }

   // Chargement d'un fichier de propriétés
   private MyProperties loadProperties(String path) throws Exception {
      MyProperties prop;
      String propFile = path+Util.FS+Constante.FILE_PROPERTIES;
      prop = new MyProperties();
      File f = new File( propFile );
      if( f.exists() ) {
         if( !f.canRead() ) throw new Exception("Propertie file not available ! ["+propFile+"]");
         InputStreamReader in = new InputStreamReader( new BufferedInputStream( new FileInputStream(propFile) ), "UTF-8");
         prop.load(in);
         in.close();
      }
      return prop;
   }

   /** Préparation de la table des couleurs pour la composante c
    * @param s : format: min [med] max [fct]  (représente les valeurs physiques des pixels comme représentées sur l'histogramme des pixels)
    */
   private void setCmParamExact(String s,int c) throws Exception {
      Tok tok = new Tok(s);
      double min;
      double max;
      try {
         min = Double.parseDouble(tok.nextToken());
         max = Double.parseDouble(tok.nextToken());
      } catch( Exception e1 ) {
         throw new Exception("Colormap parameter error ["+s+"] => usage: min [middle] max [fct]");
      }
      int transfertFct;

      if( max<=min ) throw new Exception("Colormap parameter error ["+s+"] => max<=min");
      double med = Double.NaN;
      String fct = null;
      if( tok.hasMoreTokens() ) {
         String s1 = tok.nextToken();
         double x;
         try {
            x = Double.parseDouble(s1);
            med=max;
            max=x;
         } catch( Exception e ) {
            fct=s1;
         }
      }
      if( tok.hasMoreTokens() ) fct=tok.nextToken();

      if( fct!=null ) {
         int i = cds.tools.Util.indexInArrayOf(fct, PlanImage.TRANSFERTFCT, true);
         if( i<0 ) throw new Exception("Colormap parameter error ["+s+"] => Unknown transfert function ["+fct+"]");
         transfertFct=i;

      } else transfertFct=PlanImage.LINEAR;

      transfertFcts[c] = PlanImage.getTransfertFctInfo( transfertFct);

      // récupération du bzero et bscale
      double bzero=0,bscale=1;
      Fits f = new Fits();
      String name = inputs[c]+Util.FS+"Norder3"+Util.FS+"Allsky.fits";
      File file = new File( name );
      if( !file.canRead() ) throw new Exception("Cannot determine BZERO and BSCALE for component "+c+" => missing Allsky.fits");
      f.loadHeaderFITS( name );
      try { bzero = f.headerFits.getDoubleFromHeader("BZERO"); } catch( Exception e ) { }
      try { bscale = f.headerFits.getDoubleFromHeader("BSCALE"); } catch( Exception e ) { }

      pixelMin[c]    = (min-bzero)/bscale;
      pixelMiddle[c] = (med-bzero)/bscale;
      pixelMax[c]    = (max-bzero)/bscale;

      int tr1 = 128;
      if( !Double.isNaN(med) ) {
         if( med<min || med>max ) throw new Exception("Colormap parameter error ["+s+"] => med<min || med>max");
         tr1 = (int)( ( (med-min)/(max-min) ) *255 );
      }

      context.info("Using pixelCut "+L(c)+" = "+min+(Double.isNaN(med)?"":" "+med)+" "+max+
            (transfertFct!=PlanImage.LINEAR ?" "+PlanImage.getTransfertFctInfo(transfertFct):""));

      IndexColorModel cm = CanvasColorMap.getCM(0, tr1, 255, false, PlanImage.CMGRAY, transfertFct, true );
      tcm[c] = cds.tools.Util.getTableCM(cm, 2);
   }

   private String L(int c) { return c==0?"red":c==1?"green":"blue"; }


//   public void build() throws Exception  {
//      initStat();
//      
//      for( int i=0; i<768; i++ ) {
//         if( context.isTaskAborting() ) new Exception("Task abort !");
//         // Si le fichier existe déjà on ne l'écrase pas
//         String rgbfile = Util.getFilePath(output,3, i)+Constante.TILE_EXTENSION[format];
//         if( !context.isInMocTree(3, i) ) continue;
//         if ((new File(rgbfile)).exists()) continue;
//         createRGB(3,i);
//         context.setProgressLastNorder3(i);
//      }
//   }
   
   
   protected Fits createLeaveHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix, int z) throws Exception {
      long t = System.currentTimeMillis();
      Fits [] in = getLeaves(order, npix);
      Fits rgb = in==null ? null : createLeaveRGB(in);
      if( rgb==null ) {
         long duree = System.currentTimeMillis()-t;
         updateStat(0,0,1,duree,0,0);
         return null;
      }

      write(file,rgb);
      long duree = System.currentTimeMillis()-t;
      updateStat(0,1,0,duree,0,0);
      updateStat();

      return rgb;
   }
   
//   protected void write(String file, Fits out) throws Exception {
//      String filename = file+context.getTileExt();
//      out.writeCompressed(filename,0,0,null, Constante.TILE_MODE[ context.targetColorMode ]);
//   }
   
   // génération du RGB à partir des composantes
   private Fits createLeaveRGB(Fits [] in) throws Exception {
      
      // Application d'un filtre ?
      if( flagGauss ) {
         for( int i=0; i<3; i++ ) gaussian(in[i]);
      }
      
      double gap=0;
      double range=256;
      if( Fits.isTransparent(format==Constante.TILE_PNG ? Fits.PIX_255 : Fits.PIX_256) ) {
         range = 255;
         gap = 1;
      }
      
      Fits rgb = new Fits(width,width,0);
      double [] pix = new double[3];
      int i=0;
      for( int y=0; y<width; y++ ) {
         for( int x=0; x<width; x++,i++ ) {

            double tot = 0;  // Pour faire la moyenne en cas d'une composante manquante
            for( int c=0; c<3; c++ ) {
               if( c==missing || in[c]==null ) continue;
               double v = in[c].getPixelDouble(x,y); //width-y-1);
               if( in[c].isBlankPixel(v) ) pix[c] = 0;
               else {
                  pix[c] = gap+ ( v< pixelMin[c] ? 0 : v> pixelMax[c] ? range-gap-1
                        : range *( (v-pixelMin[c]) / (pixelMax[c] - pixelMin[c]) ) );
                  tot += pix[c];
               }
            }
            if( missing!=-1 ) pix[missing] = tot/2;
            
            int pixel;
            double[] pix8 = new double[3];
            if( tot==0 ) pixel=0x00;
            else {
               pixel = 0xFF;
               for( int c=0; c<3; c++ ) { 
                  int itcm =  (int)Math.floor( pix[c] );
                  if( tcm[c]==null ) pix8[c] = itcm;
                  else if( itcm>=255 ) pix8[c] = tcm[c][255];
                  else if( itcm<=gap ) pix8[c] = tcm[c][(int)gap];
                  else {
                     double d1 = pix[c] - itcm;
                     if( d1==0 ) pix8[c] = tcm[c][ itcm ] & 0xFF;
                     else {
                        double d2 = 1-d1;
                        double v1 = tcm[c][ itcm ] & 0xFF;
                        double v2 = tcm[c][ itcm+1 ] & 0xFF;
                        pix8[c] = (int)Math.round( v1*d2  + v2*d1 );
                     }
                  }
                  pixel = (pixel<<8) | ((int)pix8[c] & 0xFF);
               }
            }

            rgb.rgb[i]=pixel;
         }
      }

      return rgb;
   }
   
   
   static final double [] kernel = KernelList.createFastGaussienMatrix(2, 0.8);
   
   static public void gaussian(Fits in) {
      if( in==null ) return;
      double [] inPixels = new double[in.width*in.height];
      int x,y,i;
      for( i=y=0; y<in.height; y++ ) {
         for( x=0; x<in.width; x++ ) inPixels[i++] = in.getPixelDouble(x, y);
      }
      double [] outPixels = new double[ inPixels.length ];
      convolveAndTranspose(kernel, inPixels, outPixels, in.width, in.height);
      convolveAndTranspose(kernel, outPixels, inPixels, in.height, in.width);
      for( i=y=0; y<in.height; y++ ) {
         for( x=0; x<in.width; x++ ) in.setPixelDouble(x,y, inPixels[i++]);
      }
   }
   
   /**
    * Calcul d'une convolution par une gaussienne en 2 passes (voir compute())
    * @param gap Juste pour mettre à jour le pourcentage de progression en tenant compte du calcul précédent
    */
   static private void convolveAndTranspose(double [] k, double [] inPixels, double [] outPixels, int width, int height) {
      int cols = k.length;
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
               pixval += inPixels[ioffset+ix] * k[col+cols2];
            }
            outPixels[index] = pixval;
            index+=height;
         }
      }
   }




   // Génération des RGB récursivement en repartant du niveau de meilleure résolution
   // afin de pouvoir utiliser un calcul de médiane sur chacune des composantes R,G et B
//   private Fits [] createRGB(int order, long npix) throws Exception {
//
//      if( context.isTaskAborting() ) new Exception("Task abort !");
//
//      // si on n'est pas dans le Moc, il faut retourner les 4 fits de son niveau
//      // pour la construction de l'arborescence...
//      if( !context.isInMocTree(order,npix) ) return createLeaveRGB(order, npix);
//
//      // si le losange a déjà été calculé on renvoie les 4 fits de son niveau
//      if( coaddMode==Mode.KEEPTILE ) {
//         if( findLeaf(order, npix) ) {
//            Fits [] oldOut = createLeaveRGB(order, npix);
//            HealpixMoc moc = context.getRegion();
//            moc = moc.intersection(new HealpixMoc(order+"/"+npix));
//            int nbTiles = (int)moc.getUsedArea();
//            updateStat(nbTiles);
//            return oldOut;
//         }
//      }
//
//      // S'il n'existe pas au-moins 1 tuile de la composante à cette position, c'est une branche morte
//      boolean trouve=false;
//      for( int c=0; !trouve && c<3; c++ ) {
//         if( c==missing ) continue;
//         trouve = moc[c].isIntersecting(order, npix);
//      }
//      if( !trouve ) return null;
//
//      Fits [] out = null;
//
//      // Branche terminale
//      boolean leaf=false;
//      if( order==maxOrder ) {
//         out = createLeaveRGB(order, npix);
//         leaf=true;
//      }
//
//      // Noeud intermédiaire
//      else {
//         Fits [][] fils = new Fits[4][3];
//         boolean found = false;
//         for( int i=0; i<4; i++ ) {
//            fils[i] = createRGB(order+1,npix*4+i);
//            if( fils[i] != null && !found ) found = true;
//         }
//         if( found ) out = createNodeRGB(fils);
//      }
//      if( out!=null ) generateRGB(out, order, npix, leaf);
//      return out;
//   }

//   private boolean findLeaf(int order, long npix) throws Exception {
//      String filename = Util.getFilePath(output,order, npix)+Constante.TILE_EXTENSION[format];
//      File f = new File(filename);
//      return f.exists();
//   }

   // Génération d'un noeud par la médiane pour les 3 composantes
   // (on ne génère pas encore le RGB (voir generateRGB(...))
//   private Fits [] createNodeRGB(Fits [][] fils) throws Exception {
//
//      Fits [] out = new Fits[3];
//      if( context.isTaskAborting() ) throw new Exception("Task abort !");
//
//      for( int c=0; c<3; c++ ) {
//         out[c] = new Fits(width,width,bitpix[c]);
//         out[c].setBlank(blank[c]);
//         out[c].setBzero(bzero[c]);
//         out[c].setBscale(bscale[c]);
//      }
//
//      for( int dg=0; dg<2; dg++ ) {
//         for( int hb=0; hb<2; hb++ ) {
//            int quad = dg<<1 | hb;
//            int offX = (dg*width)/2;
//            int offY = ((1-hb)*width)/2;
//
//            for( int c=0; c<3; c++ ) {
//               if( c==missing ) continue;
//               Fits in = fils[quad]==null ? null : fils[quad][c];
//               double p[] = new double[4];
//               double coef[] = new double[4];
//
//               for( int y=0; y<width; y+=2 ) {
//                  for( int x=0; x<width; x+=2 ) {
//
//                     double pix=blank[c];
//                     if( in!=null ) {
//
//                        // On prend la moyenne (sans prendre en compte les BLANK)
//                        if( method==Context.JpegMethod.MEAN ) {
//                           double totalCoef=0;
//                           for( int i=0; i<4; i++ ) {
//                              int dx = i==1 || i==3 ? 1 : 0;
//                              int dy = i>=2 ? 1 : 0;
//                              p[i] = in.getPixelDouble(x+dx,y+dy);
//                              if( in.isBlankPixel(p[i]) ) coef[i]=0;
//                              else coef[i]=1;
//                              totalCoef+=coef[i];
//                           }
//                           if( totalCoef!=0 ) {
//                              pix = 0;
//                              for( int i=0; i<4; i++ ) {
//                                 if( coef[i]!=0 ) pix += p[i]*(coef[i]/totalCoef);
//                              }
//                           }
//
//                           // On garde la valeur médiane (les BLANK seront automatiquement non retenus)
//                        } else {
//
//                           double p1 = in.getPixelDouble(x,y);
//                           if( in.isBlankPixel(p1) ) p1=Double.NaN;
//                           double p2 = in.getPixelDouble(x+1,y);
//                           if( in.isBlankPixel(p2) ) p1=Double.NaN;
//                           double p3 = in.getPixelDouble(x,y+1);
//                           if( in.isBlankPixel(p3) ) p1=Double.NaN;
//                           double p4 = in.getPixelDouble(x+1,y+1);
//                           if( in.isBlankPixel(p4) ) p1=Double.NaN;
//
//                           if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
//                           else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
//                           else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
//                           else pix=p4;
//                        }
//                     }
//
//                     out[c].setPixelDouble(offX+(x/2), offY+(y/2), pix);
//                  }
//               }
//            }
//         }
//      }
//
//      for( int j=0; j<4; j++ ) {
//         for( int c=0; c<3; c++ ) {
//            if( c==missing ) continue;
//            if( fils[j]!=null && fils[j][c]!=null ) fils[j][c].free();
//         }
//      }
//
//      return out;
//   }

   // Génération d'une feuille terminale (=> simple chargement des composantes)
   private Fits [] getLeaves(int order, long npix) throws Exception {
      if( context.isTaskAborting() ) new Exception("Task abort !");
      Fits[] out =null;
      out = new Fits[3];

      // Chargement des 3 (ou éventuellement 2) composantes
      for( int c=0; c<3; c++ ) {
         if( c==missing ) continue;

         if( !moc[c].isIntersecting(order, npix)) out[c]=null;
         else {
            try {
               out[c] = createSubLeaveRGB(order,npix,c);
            } catch( Exception e ) { out[c]=null; }
         }

         // Initialisation des constantes pour cette composante
         if( out[c]!=null && bitpix[c]==0 ) {
            bitpix[c]=out[c].bitpix;
            blank[c]=out[c].blank;
            bscale[c]=out[c].bscale;
            bzero[c]=out[c].bzero;
            if( width==-1 ) width = out[c].width;  // La largeur d'un losange est la même qq soit la couleur
         }
      }
      if( out[0]==null && out[1]==null && out[2]==null ) out=null;
      return out;
   }

   // Dans le cas où l'ordre max d'un HiPS est inférieur à la demande, il
   // faut prendre la tuile du premier niveau supérieur qui contient la zone,
   // et générer à la volée la sous-tuile
   private Fits createSubLeaveRGB(int order, long npix,int c) throws Exception {
      int o = order;
      String file=null;

      // Détermination du premier ordre parent qui dispose d'une tuile
      long n=npix;
      for( o=order; o>=3; o--, n/=4 ) {
         file =  Util.getFilePath( inputs[c], o, n, cubeIndex[c])+".fits";
         if( (new File(file)).exists() ) break;
      }

      // A priori aucune => bizarre !! le moc devait être non renseigné
      if( o<3 ) return null;

      // Chargement de la tuile "ancetre"
      Fits fits = new Fits();
      fits.loadFITS( file );

      // On est à l'ordre demandé => rien à faire
      if( o==order ) return fits;

      // Détermination de la position (xc,yc coin sup gauche) et de la taille
      // de la zone de pixel à extraire (width*width), et de la taille du pixel final (gap)
      int xc=0, yc=fits.height-1;
      int width = fits.width;
      for( int i=order; i>o; i--) width/=2;

      int gap=1;
      int w = width;
      for( int i=order; i>o; i--, npix/=4L, w*=2) {
         gap *= 2;
         int child = (int)( npix%4L);
         int offsetX = child==2 || child==3 ? w : 0;
         int offsetY = child==1 || child==3 ? w : 0;
         xc = xc + offsetX;
         yc = yc - offsetY;
      }

      int length = Math.abs(fits.bitpix)/8;

      // Extraction des pixels dans un buffer temporaire
      byte [] pixels = new byte[ width*width*length ];
      for( int y=width-1; y>=0; y--) {
         for( int x=0; x<width; x++) {
            int srcPos  = ( (yc-(width-y-1))*fits.width + (xc+x) )*length;
            int destPos = ( y*width+x )*length;
            System.arraycopy(fits.pixels, srcPos, pixels, destPos, length);
         }
      }

      // Agrandissement des pixels
      for( int y=width-1; y>=0; y--) {
         for( int x=0; x<width; x++ ) {
            int srcPos = ( y*width+x ) *length;
            for( int gapy=0; gapy<gap; gapy++) {
               for( int gapx=0; gapx<gap; gapx++ ) {
                  int destPos = ( (y*gap+gapy)*fits.width+(x*gap+gapx) ) *length;
                  System.arraycopy(pixels, srcPos, fits.pixels, destPos, length);
               }
            }
         }
      }

      return fits;
   }
   
   // génération du RGB à partir des composantes
//   private void generateRGB(Fits [] out, int order, long npix, boolean leaf) throws Exception {
//      
//      double gap=0;
//      double range=256;
//      if( Fits.isTransparent(format==Constante.TILE_PNG ? Fits.PIX_255 : Fits.PIX_256) ) {
//         range = 255;
//         gap = 1;
//      }
//
//      Fits rgb = new Fits(width,width,0);
//      double [] pix = new double[3];
//      int i=0;
//      for( int y=0; y<width; y++ ) {
//         for( int x=0; x<width; x++,i++ ) {
//
//            double tot = 0;  // Pour faire la moyenne en cas d'une composante manquante
//            for( int c=0; c<3; c++ ) {
//               if( c==missing || out[c]==null ) continue;
//               double v = out[c].getPixelDouble(x,width-y-1);
//               if( out[c].isBlankPixel(v) ) pix[c] = 0;
//               else {
//                  pix[c] = gap+ ( v< pixelMin[c] ? 0 : v> pixelMax[c] ? range-gap-1
//                        : range *( (v-pixelMin[c]) / (pixelMax[c] - pixelMin[c]) ) );
//                  tot += pix[c];
//               }
//            }
//            if( missing!=-1 ) pix[missing] = tot/2;
//            
//            int pixel;
//            double[] pix8 = new double[3];
//            if( tot==0 ) pixel=0x00;
//            else {
//               pixel = 0xFF;
//               for( int c=0; c<3; c++ ) { 
//                  int itcm =  (int)Math.floor( pix[c] );
//                  if( tcm[c]==null ) pix8[c] = itcm;
//                  else if( itcm>=255 ) pix8[c] = tcm[c][255];
//                  else if( itcm<=gap ) pix8[c] = tcm[c][(int)gap];
//                  else {
//                     double d1 = pix[c] - itcm;
//                     if( d1==0 ) pix8[c] = tcm[c][ itcm ] & 0xFF;
//                     else {
//                        double d2 = 1-d1;
//                        double v1 = tcm[c][ itcm ] & 0xFF;
//                        double v2 = tcm[c][ itcm+1 ] & 0xFF;
//                        pix8[c] = (int)Math.round( v1*d2  + v2*d1 );
//                     }
//                  }
//                  pixel = (pixel<<8) | ((int)pix8[c] & 0xFF);
//               }
//            }
//
//            rgb.rgb[i]=pixel;
//         }
//      }
//      String file="";
//
//      file = Util.getFilePath(output,order, npix)+Constante.TILE_EXTENSION[format];
//      rgb.writeRGBPreview(file,Constante.TILE_MODE[format]);
//      rgb.free();
//
//      if( leaf ) {
//         File f = new File(file);
//         updateStat(f);
//      }
//   }
   
   
   // génération du RGB à partir des composantes
//   private void generateRGB(Fits [] out, int order, long npix, boolean leaf) throws Exception {
//      byte [][] pixx8 = new byte [3][];
//
//      // Passage en 8 bits pour chaque composante
//      for( int c=0; c<3; c++ ) {
//         if( c==missing || out[c]==null ) continue;
//         pixx8[c] = out[c].toPix8( pixelMin[c],pixelMax[c], tcm[c],
//               format==Constante.TILE_PNG ? Fits.PIX_255 : Fits.PIX_256);
//      }
//
//      Fits rgb = new Fits(width,width,0);
//      int [] pix8 = new int[3];
//      for( int i=width*width-1; i>=0; i-- ) {
//         
//         int tot = 0;  // Pour faire la moyenne en cas d'une composante manquante
//         for( int c=0; c<3; c++ ) {
//            if( c==missing ) continue;
//            if( out[c]==null ) pix8[c]=0;
//            else {
//               pix8[c] = 0xFF & pixx8[c][i];
//               tot += pix8[c];
//            }
//         }
//         if( missing!=-1 ) pix8[missing] = tot/2;
//         int pix;
//         if( tot==0 ) pix=0x00;
//         else {
//            pix = 0xFF;
//            for( int c=0; c<3; c++ ) pix = (pix<<8) | pix8[c];
//         }
//         rgb.rgb[i]=pix;
//      }
//      String file="";
//
//      file = Util.getFilePath(output,order, npix)+Constante.TILE_EXTENSION[format];
//      rgb.writeRGBPreview(file,Constante.TILE_MODE[format]);
//      rgb.free();
//
//      if( leaf ) {
//         File f = new File(file);
//         updateStat(f);
//      }
//   }

}



