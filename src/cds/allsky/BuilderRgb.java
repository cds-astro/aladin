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

import java.awt.image.IndexColorModel;
import java.io.File;

import cds.aladin.CanvasColorMap;
import cds.aladin.KernelList;
import cds.aladin.MyProperties;
import cds.aladin.PlanBG;
import cds.aladin.PlanImage;
import cds.aladin.Tok;
import cds.fits.Fits;
import cds.moc.SMoc;
import cds.tools.pixtools.Util;

public class BuilderRgb extends BuilderRunner {


   private String [] inputs;         // Les paths des HiPS red, green, blue
   private int [] cubeIndex;         // Les indices des frames des composantes red, green, blue en cas de cubes d'origine (0 par défaut)
   private SMoc [] moc;        // Les Mocs de chaque composante
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
   
   private boolean flagLupton;
   private double luptonS[];
   private double luptonM[];
   private double luptonQ;
   
   private int statNbFile;

   private ModeMerge coaddMode=ModeMerge.mergeOverwriteTile;
   private int format;

   public BuilderRgb(Context context) {
      super(context);
   }

   public Action getAction() { return Action.RGB; }

   public void run() throws Exception {
      Fits.setToolKit();
      build();
      context.resetCheckCode();
      context.setPropriete(Constante.KEY_HIPS_COADD, 
               context.getModeOverlay().toString()+" "+
               context.getModeMerge().toString()+" "+
               context.getModeTree().toString() );

      if( !context.isTaskAborting() ) (new BuilderMoc(context)).createMoc(output);
      if( !context.isTaskAborting() ) { (new BuilderAllsky(context)).run(); context.done("ALLSKY file done"); }
   }
   
   protected void activateCache(long size,long sizeCache) { }
   
   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showRGBStat(statNbFile, totalTime,getNbThreads(), getNbThreadRunning());
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
   
   // Supprime le suffixe d'un path à la mode cube (ex: xxx/Allsky.fits[1] => xxx/Allsky.fits
   // sinon retourne le path original
   private String removeCubeSuffixe( String path ) {
      if( path==null ) return null;
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
      if( path==null ) return 0;
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
      coaddMode = context.getModeMerge();
      if( coaddMode!=ModeMerge.mergeKeepTile && coaddMode!=ModeMerge.mergeOverwriteTile ) {
         if( context instanceof ContextGui ) {
            context.setModeMerge(ModeMerge.mergeOverwriteTile);
         } else throw new Exception("Only KEEPTILE and REPLACETILE modes are supported for RGB HiPS generation");

      }

      String pathRef=null;

      inputs = new String[3];
      cubeIndex = new int[3];
      labels = new String[3];
      moc = new SMoc[3];
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
            SMoc m = moc[c] = loadMoc( inputs[c] );
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

      if( context instanceof ContextGui ) context.resetProgressParam();
      
//      if( context instanceof ContextGui ) {
//         SMoc m = context.moc;
//         ((ContextGui)context).mainPanel.clearForms();
//         context.moc = m;
//      }

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

      // Est-ce un traitement à la Lupton ?
      if( context.flagLupton ) initLuptonParam();

      // mémorisation des informations de colormaps
      for( int c=0; c<3; c++) {
         if( c==missing ) continue;
         String info;
         if( flagLupton ) info = labels[c]+" [lupton min="+R(luptonM[c])+" scale="+R(luptonS[c])+" Q="+luptonQ+"]";
         else info =  labels[c]+" ["+pixelMin[c]+" "+pixelMiddle[c]+" "+pixelMax[c]+" "+transfertFcts[c]+"]";
         if( c==0 ) context.redInfo=info;
         else if( c==1 ) context.greenInfo=info;
         else context.blueInfo=info;
      }

      // détermination de la zone à calculer
      if( context.mocArea!=null ) context.moc = context.moc.intersection( context.mocArea );
      
//      // Faut-il un filtre gaussien
//      if( context.gaussFilter ) context.info("Gauss filter activated...");
//      flagGauss = context.gaussFilter;

      context.writeMetaFile();
   }
   
   
   /** Estimation du paramètre S lupton (Scale) */
   static public double estimateLuptonS( PlanBG p ) {
      return 10/( p.getCutCtrlMax()- p.getCutCtrlMin() );
   }

   /** Estimation du paramètre M lupton (minimum) */
   static public double estimateLuptonM( PlanBG p ) {
      return -(( (p.getCutCtrlMin()+( p.getCutCtrlMax() - p.getCutCtrlMin())/20) * p.bScale + p.bZero) *  estimateLuptonS(p) ); 
   }

   // Initialisation des paramètres de l'algo Lupton en fonction de cutmin et cutmax
   // (uniquement pour les valeurs non déterminées explicitement)
   private void initLuptonParam() {
      flagLupton=true;
      luptonQ = !Double.isNaN(context.luptonQ) ? context.luptonQ : 20;
      luptonS = new double[3];
      luptonM = new double[3];
      for(int c=0; c<3; c++ ) {
         double z = 10/((pixelMax[c]*bscale[c]+bzero[c]) - (pixelMin[c]*bscale[c]+bzero[c]));
         luptonS[c] = !Double.isNaN(context.luptonS[c]) ? context.luptonS[c] : z;
         luptonM[c] = !Double.isNaN(context.luptonM[c]) ? context.luptonM[c]
               : -(( (pixelMin[c]+(pixelMax[c]-pixelMin[c])/20)*bscale[c]+bzero[c]) * z); 
      }
      
      String sm = (luptonM[0]==luptonM[1] && luptonM[1]==luptonM[2]) ? R(luptonM[0])
            : R(luptonM[0])+"/"+R(luptonM[1])+"/"+R(luptonM[2]);
      String ss = (luptonS[0]==luptonS[1] && luptonS[1]==luptonS[2]) ? R(luptonS[0])
            : R(luptonS[0])+"/"+R(luptonS[1])+"/"+R(luptonS[2]);
      context.info("Lupton algo: Q="+luptonQ+" m="+sm+" scale="+ss);
   }
   
   private String R(double x) { return cds.tools.Util.myRound(x); }

   // Chargement d'un MOC, et par défaut, d'un MOC couvrant tout le ciel
   private SMoc loadMoc( String path ) throws Exception {
      String s = path+Util.FS+"Moc.fits";
      if( !(new File(s)).canRead() ) return new SMoc("0/0-11");
      SMoc m = new SMoc();
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
         s = prop.getProperty( Constante.KEY_OBS_TITLE);
         if( s==null ) prop.getProperty( Constante.KEY_OBS_COLLECTION);
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
         
         // Pas fournis ? On va récupérer les cuts depuis les properties
         String s1 = getCmParamFromProp( prop[c] );
         Tok tok1 = new Tok(s1);
         try {
            min = Double.parseDouble(tok1.nextToken());
            max = Double.parseDouble(tok1.nextToken());
            tok = new Tok(s);
         } catch( Exception e2 ) {
            throw new Exception("Colormap parameter error ["+s+"] => usage: min [middle] max [fct]");
         }
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
      
//      String name = inputs[c]+Util.FS+"Norder3"+Util.FS+"Allsky.fits";
//      if( !file.canRead() ) throw new Exception("Cannot determine BZERO and BSCALE for component "+c+" => missing Allsky.fits");
      String name = context.findOneNpixFile(inputs[c],"fits");
      if( name==null ) throw new Exception("Cannot determine BZERO and BSCALE for component "+c+" => no fits tile");

      f.loadHeaderFITS( name );
      try { bzero = f.headerFits.getDoubleFromHeader("BZERO"); } catch( Exception e ) { }
      try { bscale = f.headerFits.getDoubleFromHeader("BSCALE"); } catch( Exception e ) { }

      this.bscale[c] = bscale;
      this.bzero[c] = bzero;
      
      pixelMin[c]    = (min-bzero)/bscale;
      pixelMiddle[c] = (med-bzero)/bscale;
      pixelMax[c]    = (max-bzero)/bscale;

      int tr1 = 128;
      if( !Double.isNaN(med) ) {
         if( med<min || med>max ) throw new Exception("Colormap parameter error ["+s+"] => med<min || med>max");
         tr1 = (int)( ( (med-min)/(max-min) ) *255 );
      }

      if( !context.flagLupton ) context.info("Using pixelCut "+L(c)+" = "+min+(Double.isNaN(med)?"":" "+med)+" "+max+
            (transfertFct!=PlanImage.LINEAR ?" "+PlanImage.getTransfertFctInfo(transfertFct):""));

      IndexColorModel cm = CanvasColorMap.getCM(0, tr1, 255, false, PlanImage.CMGRAY, transfertFct, true );
      tcm[c] = cds.tools.Util.getTableCM(cm, 2);
   }

   private String L(int c) { return c==0?"red":c==1?"green":"blue"; }

   protected Fits createLeafHpx(ThreadBuilderTile hpx, String file,String path,int order,long npix, int z) throws Exception {
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
   
   // génération du RGB
   private Fits createLeaveRGB(Fits [] in) throws Exception {
      
      // Quel traitement ?
      return flagLupton ? createLeaveRGBLupton(in,width,format,flagGauss,missing,luptonQ,luptonM,luptonS)
            : createLeaveRGBClassic(in,width,format,flagGauss,missing,pixelMin,pixelMax,tcm);
   } 
   
   static double arcsinh( double v ) {
      return Math.log(v + Math.sqrt(Math.pow(v, 2.)+1));
   }
   

   // Génération du RGB à partir des composantes (méthode Lupton)
   // Une des composantes peut être manquante (remplacée par la moyenne des deux autres)
   // r = np.maximum(0, r + m)
   // g = np.maximum(0, g + m)
   // b = np.maximum(0, b + m)
   // I = (r+g+b)/3.
   // fI = np.arcsinh(Q * I) / np.sqrt(Q)
   // I += (I == 0.) * 1e-6
   // R = fI * r / I
   // G = fI * g / I
   // B = fI * b / I
   public static Fits createLeaveRGBLupton(Fits [] in,int width,int format, boolean flagGauss, int missing,
         double luptonQ, double [] luptonM, double [] luptonS) throws Exception {
      
      // Application d'un filtre ?
      if( flagGauss ) {
         for( int i=0; i<3; i++ ) gaussian(in[i]);
      }

      double Q2 = Math.sqrt(luptonQ);
      
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
            
            double I=0;
            int tot=0;
            boolean allBlank=true;
            for( int c=0; c<3; c++ ) {
               if( c==missing || in[c]==null ) continue;
               double v = in[c].getPixelFull(x,y);   // getPixelDouble(x,y);
               if( in[c].isBlankPixel(v) ) pix[c] = 0;
               else {
                  allBlank=false;
                  pix[c] = v * luptonS[c] + luptonM[c];
                  if( pix[c]<0 ) pix[c]=0;
               }
               I += pix[c];
               tot++;
            }
            if( allBlank ) { rgb.rgb[i]=0; continue; }
            I /= tot;
            if( I==0 ) I=1e-6;
            double fI = arcsinh(luptonQ * I) / Q2;
            if( missing!=-1 ) pix[missing] = I;
            
            int pixel = 0xFF;
            for( int c=0; c<3; c++ ) {
               double v = fI * pix[c] / I;
               if( v>1-1e-6 ) v=1-1e-6;
               v = v*range + gap;
               v = v<gap ? gap : v>range ? range : v;
               pixel = (pixel<<8) | ((int)v & 0xFF);
            }

            rgb.rgb[i]=pixel;
         }
      }
      
      return rgb;
   }
   
   // Génération du RGB à partir des composantes (méthode classique -> basée sur la colormap)
   // Une des composantes peut être manquante (remplacée par la moyenne des deux autres)
   static public Fits createLeaveRGBClassic(Fits [] in,int width,int format,boolean flagGauss, int missing,
         double [] pixelMin, double [] pixelMax, byte [][] tcm) throws Exception {

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
               double v = in[c].getPixelDouble(x,y); 
               if( in[c].isBlankPixel(v) ) pix[c] = 0;
               else {
                  pix[c] = gap+ ( v< pixelMin[c] ? 0 : v> pixelMax[c] ? range-gap-1
                        : range *( (v-pixelMin[c]) / (pixelMax[c] - pixelMin[c]) ) );
                  tot += pix[c];
               }
            }
//            if( missing!=-1 ) pix[missing] = tot/2;
            
            int pixel;
            double[] pix8 = new double[3];
            if( tot==0 ) pixel=0x00;
            else {
               tot=0;
               for( int c=0; c<3; c++ ) { 
                  if( c==missing ) continue;
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
                  tot += pix8[c];
               }
               
               if( missing!=-1 ) pix8[missing]=tot/2.;
               pixel = 0xFF;
               for( int c=0; c<3; c++ ) { 
                  pixel = (pixel<<8) | ((int)pix8[c] & 0xFF);
               }
            }

            rgb.rgb[i]=pixel;
         }
      }

      return rgb;
   }
   
   
   static final double [] kernel = KernelList.createFastGaussienMatrix(2, 0.8);
   
   /** Convolution par une gaussienne */
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
   
   
   
}



