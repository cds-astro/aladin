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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.swing.JProgressBar;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.aladin.PlanBG;
import cds.aladin.PlanHealpix;
import cds.aladin.PlanImage;
import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Classe pour unifier les accès aux paramètres nécessaires pour les calculs
 * + accès aux méthodes d'affichages qui sont redirigées selon l'interface
 * existante
 * @author Oberto + Fernique
 *
 */
public class Context {

   static final public String LOGFILE = "Skygen.log";
   
   private static boolean verbose = false;
   protected String label;                   // Nom du survey
   
   protected String inputPath;               // Répertoire des images origales
   protected String outputPath;              // Répertoire de la boule HEALPix à générer
   protected String hpxFinderPath;           // Répertoire de l'index Healpix (null si défaut => dans outputPath/HpxFinder)
   protected String imgEtalon;               // Nom (complet) de l'image qui va servir d'étalon
   
   protected int bitpixOrig = -1;            // BITPIX des images originales
   protected double blankOrig= Double.NaN;   // Valeur du BLANK en entrée
   protected boolean hasAlternateBlank=false;// true si on a indiqué une valeur BLANK alternative
   protected double bZeroOrig=0;             // Valeur BZERO d'origine
   protected double bScaleOrig=1;            // Valeur BSCALE d'origine
   protected double[] cutOrig; // Valeurs cutmin,cutmax, datamin,datamax des images originales
   protected int[] borderSize = {0,0,0,0};   // Bords à couper sur les images originales
   protected String skyvalName;                // Nom du champ à utiliser dans le header pour soustraire un valeur de fond (via le cacheFits)
   protected double coef;                    // Coefficient permettant le calcul dans le BITPIX final => voir initParameters()
   
   protected int bitpix = -1;                // BITPIX de sortie
   protected double blank = Double.NaN;      // Valeur du BLANK en sortie
   protected double bZero=0;                 // Valeur BZERO de la boule Healpix à générer
   protected double bScale=1;                // Valeur BSCALE de la boule HEALPix à générer
   protected boolean bscaleBzeroSet=false;   // true si le bScale/bZero de sortie a été positionnés
   protected double[] cut;   // Valeurs cutmin,cutmax, datamin,datamax pour la boule Healpix à générer
   protected TransfertFct fct = TransfertFct.LINEAR; // Fonction de transfert des pixels fits -> jpg
   private JpegMethod jpegMethod = Context.JpegMethod.MEDIAN;
   protected CoAddMode coAdd=CoAddMode.getDefault();  // Methode de traitement par défaut
   protected int maxNbThread=-1;             // Nombre de threads de calcul max imposé par l'utilisateur
   
   protected int order = -1;                 // Ordre maximale de la boule HEALPix à générer              
   protected int frame = Localisation.ICRS;  // Système de coordonnée de la boule HEALPIX à générée
   protected HealpixMoc mocArea = null;      // Zone du ciel à traiter (décrite par un MOC)
   protected HealpixMoc mocIndex = null;     // Zone du ciel correspondant à l'index Healpix
   protected HealpixMoc moc = null;          // Intersection du mocArea et du mocIndex => regénérée par setParameters()
   protected int diffOrder=4;           // Lors du calcul du MOC, différence entre ordre du MOC et ordre optimum
   protected CacheFits cacheFits;            // Cache FITS pour optimiser les accès disques à la lecture

   public Context() {}
   
   public void reset() {
      mocArea=mocIndex=moc=null;
      coAdd=CoAddMode.getDefault();
      hasAlternateBlank=false;
      bscaleBzeroSet=false;
      imgEtalon=hpxFinderPath=inputPath=outputPath=null;
      lastNorder3=-2;
      validateOutputDone=validateInputDone=validateCutDone=false;
      prop=null;
   }

   // Getters
   public String getLabel() { return label; }
   public int[] getBorderSize() { return borderSize; }
   public int getOrder() { return order; }
   public int getFrame() { return frame; }
   public String getFrameName() { return Localisation.getFrameName(frame); }
   public CacheFits getCache() { return cacheFits; }
   public String getInputPath() { return inputPath; }
   public String getOutputPath() { return outputPath; }
   public String getHpxFinderPath() { return hpxFinderPath!=null ? hpxFinderPath : Util.concatDir( getOutputPath(),Constante.HPX_FINDER); }
   public String getImgEtalon() { return imgEtalon; }
   public int getBitpixOrig() { return bitpixOrig; }
   public int getBitpix() { return bitpix; }
   public int getNpix() { return isColor() || bitpix==-1 ? 4 : Math.abs(bitpix)/8; }  // Nombre d'octets par pixel
   public int getNpixOrig() { return isColor() || bitpixOrig==-1 ? 4 : Math.abs(bitpixOrig)/8; }  // Nombre d'octets par pixel
   public double getBScaleOrig() { return bScaleOrig; }
   public double getBZeroOrig() { return bZeroOrig; }
   public double getBZero() { return bZero; }
   public double getBScale() { return bScale; }
   public double getBlank() { return blank; }
   public double getBlankOrig() { return blankOrig; }
   public boolean hasAlternateBlank() { return hasAlternateBlank; }
   public HealpixMoc getArea() { return mocArea; }
   public CoAddMode getCoAddMode() { return isColor() ? CoAddMode.REPLACETILE : coAdd; }
   public double[] getCut() { return cut; }
   public double[] getCutOrig() { return cutOrig; }
   public String getSkyval() { return skyvalName; }
   public boolean isSkySub() { return skyvalName!=null; }
   public boolean isColor() { return bitpixOrig==0; }
   public boolean isBScaleBZeroSet() { return bscaleBzeroSet; }
   public boolean isInMocTree(int order,long npix)  { return moc==null || moc.isInTree(order,npix); }
   public boolean isInMoc(int order,long npix) { return moc==null || moc.isIntersecting(order,npix); }
   public boolean isMocDescendant(int order,long npix) { return moc==null || moc.isDescendant(order,npix); }
   public int getMaxNbThread() { return maxNbThread; }
   public int getDiffOrder() { return diffOrder; }

   // Setters
   public void setMaxNbThread(int max) { maxNbThread = max; }
   public void setBorderSize(String borderSize) throws ParseException { this.borderSize = parseBorderSize(borderSize); }
   public void setBorderSize(int[] borderSize) { this.borderSize = borderSize; }
   public void setOrder(int order) { this.order = order; }
   public void setDiffOrder(int diffOrder) { this.diffOrder = diffOrder; }
   public void setFrame(int frame) { this.frame=frame; }
   public void setFrameName(String frame) { this.frame= (frame.equalsIgnoreCase("G"))?Localisation.GAL:Localisation.ICRS; }
   public void setSkyValName(String s ) { skyvalName=s; }
   public void setInputPath(String path) { this.inputPath = path; 
   		// cherche le dernier mot et le met dans le label
   		label = path==null ? null : path.substring(path.lastIndexOf(Util.FS) + 1);
   }
   public void setOutputPath(String path) { this.outputPath = path; }
   public void setImgEtalon(String filename) throws Exception { imgEtalon = filename; initFromImgEtalon(); }
   public void setCoAddMode(CoAddMode coAdd) { this.coAdd = coAdd; }
   public void setBScaleOrig(double x) { bScaleOrig = x; }
   public void setBZeroOrig(double x) { bZeroOrig = x; }
   public void setBScale(double x) { bScale = x; bscaleBzeroSet=true; }
   public void setBZero(double x) { bZero = x; bscaleBzeroSet=true; }
   public void setBitpixOrig(int bitpixO) { 
	   this.bitpixOrig = bitpixO; 
	   if (this.bitpix==-1) this.bitpix = bitpixO;
   }
   public void setBitpix(int bitpix) { this.bitpix = bitpix; }
   public void setBlankOrig(double x) {  blankOrig = x; hasAlternateBlank=true; }
   public void setColor(boolean color) { if(color) this.bitpixOrig=0;}
   public void setCut(double [] cut) { this.cut=cut; }
   public void setPixelCut(String scut) throws Exception {
       StringTokenizer st = new StringTokenizer(scut," ");
       int i=0;
       while( st.hasMoreTokens() ) {
          String s = st.nextToken();
          try { 
             double d = Double.parseDouble(s);
             if( cut==null ) cut = new double[4];
             cut[i++]=d;
          } catch( Exception e) {
             setTransfertFct(s);
          }
          
       }
       if( i==1 || i>2 ) throw new Exception("pixelCut parameter error");

       if( cut!=null ) setCutOrig(this.cut);
   }
   
   public String getTransfertFct() { return fct.toString().toLowerCase(); }
   
   public void setTransfertFct(String txt) {
      this.fct=TransfertFct.valueOf(txt.toUpperCase());
  }
   
   protected enum JpegMethod { MEDIAN, MEAN; }
   
   /**
    * @param jpegMethod the method to set
    * @see Context#MEDIAN
    * @see Context#MEAN
    */
   public void setJpegMethod(JpegMethod jpegMethod) {
	   this.jpegMethod = jpegMethod;
   }
   public void setMethod(String jpegMethod) {
	   this.jpegMethod = JpegMethod.valueOf(jpegMethod.toUpperCase());
   }
   public JpegMethod getJpegMethod() { return jpegMethod; }

   public void setDataCut(String scut) throws Exception {
      StringTokenizer st = new StringTokenizer(scut," ");
      int i=2;
      if( cut==null ) cut = new double[4];
      while( st.hasMoreTokens() && i<4 ) {
         String s = st.nextToken();
         double d = Double.parseDouble(s);
         cut[i++]=d;
      }
      if( i<4 ) throw new Exception("Missing dataCut parameter");
      setCutOrig(this.cut);
   }

   public void setCutOrig(double [] cutOrig) {
      this.cutOrig=cutOrig;
      cut = new double[cutOrig.length];
      System.arraycopy(cutOrig, 0, cut, 0, cutOrig.length);
   }
   
   /**
    * Lit l'image etalon, et affecte les données d'origines (bitpix, bscale, bzero, blank, cut)
    * @throws Exception s'il y a une erreur à la lecture du fichier
    */
   protected void initFromImgEtalon() throws Exception {
	   String path = imgEtalon;
	   Fits fitsfile = new Fits();

	   int code = fitsfile.loadHeaderFITS(path);
//	   if( fitsfile.getCalib()==null ) throw new Exception("No calib !");
	   
       setBitpixOrig(fitsfile.bitpix);
       if( !isColor() ) {
          setBZeroOrig(fitsfile.bzero);
          setBScaleOrig(fitsfile.bscale);
          if( !Double.isNaN(fitsfile.blank) ) setBlankOrig(fitsfile.blank);
       }
       
       // Vérifie s'il s'agit d'un image avec extension
       if ( (code & Fits.XFITS)!=0 ){
    	   
       }
    	   
       // Il peut s'agit d'un fichier .hhh (sans pixel)
       try { initCut(fitsfile); } catch( Exception e ) { 
    	   Aladin.trace(4,"initFromImgEtalon :"+ e.getMessage()); }
   }
   
   /**
    * Lit l'image et calcul son autocut : affecte les datacut et pixelcut *Origines*
    * @param file
    */
   protected void initCut(Fits file) throws Exception {
       int w = file.width;
       int h = file.height;
       int x=0, y=0;
       if (w > 1024) { w = 1024; x=file.width/2 - 512; }
       if (h > 1024) { h = 1024; y=file.height/2 -512; }
       file.loadFITS(file.getFilename(), x, y, w, h);

       double[] cut = file.findAutocutRange();
       if (isSkySub()) {
          double val = file.headerFits.getDoubleFromHeader(getSkyval());
          cut[0] -= val;
          cut[1] -= val;
          cut[2] -= val;
          cut[3] -= val;
       }
       setCutOrig(cut);
   }

   /**
    * Sélectionne un fichier de type FITS (ou équivalent) dans le répertoire donné => va servir d'étalon
    * @return true si trouvé
    */
   boolean findImgEtalon(String rootPath) {
      File main = new File(rootPath);
      String[] list = main.list();
      if( list==null ) return false;
      String path = rootPath;
      for( int f = 0 ; f < list.length ; f++ ) {
         if( !rootPath.endsWith(Util.FS) ) rootPath = rootPath+Util.FS;
         path = rootPath+list[f];
         if( (new File(path)).isDirectory() ) {
            if( list[f].equals(Constante.SURVEY) ) continue;
            return findImgEtalon(path);
         }
         
         // essaye de lire l'entete fits du fichier
         // s'il n'y a pas eu d'erreur ça peut servir d'étalon
         try {
            MyInputStream in = (new MyInputStream( new FileInputStream(path))).startRead();
            if( (in.getType()&MyInputStream.FITS) != MyInputStream.FITS 
                  && !in.hasCommentCalib() ) { in.close(); continue; }    
            in.close();
            Aladin.trace(4, "Context.findImgEtalon: "+path+"...");
            setImgEtalon(path);
            return true;
            
         }  catch( Exception e) { Aladin.trace(4, "findImgEtalon : " +e.getMessage()); continue; }
      }
      return false;
   }
   
   String justFindImgEtalon(String rootPath) {
      File main = new File(rootPath);
      String[] list = main.list();
      if( list==null ) return null;
      String path = rootPath;
      for( int f = 0 ; f < list.length ; f++ ) {
         if( !rootPath.endsWith(Util.FS) ) rootPath = rootPath+Util.FS;
         path = rootPath+list[f];
         if( (new File(path)).isDirectory() ) {
            if( list[f].equals(Constante.SURVEY) ) continue;
            return justFindImgEtalon(path);
         }
         
         // essaye de lire l'entete fits du fichier
         // s'il n'y a pas eu d'erreur ça peut servir d'étalon
         try {
            // cas particulier d'un survey couleur en JPEG ou PNG avec calibration externe
            if( path.endsWith(".hhh") ) return path;
            
            MyInputStream in = (new MyInputStream( new FileInputStream(path))).startRead();
            long type = in.getType();
            if( (type&MyInputStream.FITS) != MyInputStream.FITS ) { in.close(); continue; }            
            in.close();
            return path;
            
         }  catch( Exception e) { Aladin.trace(4, "justFindImgEtalon : " +e.getMessage()); continue; }
      }
      return null;
   }
   
   protected Object [] plansRgb;
   protected String outputRgb;
   protected JpegMethod methodRgb;
   
   static final private String LABELRGB [] = {"red","gree","blue"};
   
   public void setRgbInput(String path,int c) {
      if( plansRgb==null ) plansRgb = new Object[3];
      plansRgb[c] = new PlanBG(Aladin.aladin,path, LABELRGB[c], new Coord(0,0), 0, null);
      ((PlanImage)plansRgb[c]).transfertFct=PlanImage.LINEAR;
   }
   
   public void setRgbCmParam(String cmParam,int c) throws Exception {
      if( plansRgb==null || plansRgb[c]==null ) throw new Exception("Color component folder must be defined first");
      ((PlanImage)plansRgb[c]).setCmParam(cmParam);
   }
   
   public void setSkyval(String fieldName) {
       this.skyvalName = fieldName.toUpperCase();
       if (cacheFits != null) cacheFits.setSkySub(skyvalName);
   }
   
   public void setCache(CacheFits cache) {
      this.cacheFits = cache;
      cache.setSkySub(skyvalName);
   }

   protected void setMocArea(String s) throws Exception {
      if( s.length()==0 ) return;
      mocArea = new HealpixMoc(s);
      if( mocArea.getSize()==0 ) throw new Exception("MOC sky area syntax error");
   }

   public void setMocArea(HealpixMoc area) throws Exception {
      mocArea = area;
   }
   
   public double getSkyArea() { 
      if( moc==null ) return 1;
      return moc.getCoverage();
   }

   /** Initialisation des paramètres */
   public void initParameters() throws Exception {
      
      bitpix = getBitpix();
      cut = getCut();
      
      bitpixOrig = getBitpixOrig();
      cutOrig = getCutOrig();
      blankOrig = getBlankOrig();
      bZeroOrig = getBZeroOrig();
      bScaleOrig = getBScaleOrig();

      // Le blank de sortie est imposée
      blank = getDefaultBlankFromBitpix(bitpix);
      
      // si les dataCut d'origine sont nuls ou incorrects, on les mets au max
      if( cutOrig[2]>=cutOrig[3] ) {
         cutOrig[2] = bitpixOrig==-64?-Double.MAX_VALUE : bitpixOrig==-32? -Float.MAX_VALUE
               : bitpixOrig==64?Long.MIN_VALUE+1 : bitpixOrig==32?Integer.MIN_VALUE+1 : bitpixOrig==16?Short.MIN_VALUE+1:1;
         cutOrig[3] = bitpixOrig==-64?Double.MAX_VALUE : bitpixOrig==-32? Float.MAX_VALUE
               : bitpixOrig==64?Long.MAX_VALUE : bitpixOrig==32?Integer.MAX_VALUE : bitpix==16?Short.MAX_VALUE:255;
      }
      
      // Y a-t-il un changement de bitpix ?
      // Les cuts changent 
      if( bitpix != bitpixOrig ) {
         cut[2] = bitpix==-64?-Double.MAX_VALUE : bitpix==-32? -Float.MAX_VALUE
               : bitpix==64?Long.MIN_VALUE+1 : bitpix==32?Integer.MIN_VALUE+1 : bitpix==16?Short.MIN_VALUE+1:1;
         cut[3] = bitpix==-64?Double.MAX_VALUE : bitpix==-32? Float.MAX_VALUE
               : bitpix==64?Long.MAX_VALUE : bitpix==32?Integer.MAX_VALUE : bitpix==16?Short.MAX_VALUE:255;
         coef = (cut[3]-cut[2]) / (cutOrig[3]-cutOrig[2]);

         cut[0] = (cutOrig[0]-cutOrig[2])*coef + cut[2];
         cut[1] = (cutOrig[1]-cutOrig[2])*coef + cut[2];

         bZero = bZeroOrig + bScaleOrig*(cutOrig[2] - cut[2]/coef);
         bScale = bScaleOrig/coef;
         
         Aladin.trace(3,"Change BITPIX from "+bitpixOrig+" to "+bitpix);
         Aladin.trace(3,"Map original pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] " +
                        "to ["+cut[2]+" .. "+cut[3]+"]");
         Aladin.trace(3,"Change BZERO,BSCALE,BLANK="+bZeroOrig+","+bScaleOrig+","+blankOrig
               +" to "+bZero+","+bScale+","+blank);
      
      // Pas de changement de bitpix
      } else {
         bZero=bZeroOrig;
         bScale=bScaleOrig;
         Aladin.trace(3,"BITPIX kept "+bitpix+" BZERO,BSCALE,BLANK="+bZero+","+bScale+","+blank);
      }
      
      // Détermination de la zone du ciel à calculer
      initRegion();
   }
   
   /** Détermination de la zone du ciel à calculer (appeler par initParameters()) ne pas utiliser tout
    * seul sauf si besoin explicite */
   protected void initRegion() throws Exception {
      try {
         if( mocIndex==null ) loadMocIndex();
      } catch( Exception e ) {
         warning("No MOC index found => assume all sky");
         mocIndex=new HealpixMoc("0/0-11");  // par défaut tout le ciel
      }
      if( mocArea==null ) moc = mocIndex;
      else moc = mocIndex.intersection(mocArea);
   }
   
   /** Chargement du MOC de l'index */
   protected void loadMocIndex() throws Exception {
      HealpixMoc mocIndex = new HealpixMoc();
      mocIndex.read( getHpxFinderPath()+Util.FS+BuilderMoc.MOCNAME);
      this.mocIndex=mocIndex;
   }
   
   /** Chargement du MOC réel */
   protected void loadMoc() throws Exception {
      HealpixMoc mocIndex = new HealpixMoc();
      mocIndex.read( getOutputPath()+Util.FS+BuilderMoc.MOCNAME);
      this.mocIndex=mocIndex;
   }   

   protected HealpixMoc getMocIndex() { return mocIndex; }

   
   public boolean verifCoherence() {
      if( coAdd==CoAddMode.REPLACETILE ) return true;
      String fileName=getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits";
      if( !(new File(fileName)).exists() ) return true;
      Fits fits = new Fits();
      try { fits.loadHeaderFITS(fileName); }
      catch( Exception e ) { return true; }
      if( fits.bitpix!=bitpix ) {
         warning("Uncompatible BITPIX="+bitpix+" compared to pre-existing survey BITPIX="+fits.bitpix);
         return false;
      }
      boolean nanO = Double.isNaN(fits.blank);
      boolean nan = Double.isNaN(blank);
      
      // Cas particulier des Survey préexistants sans BLANK en mode entier. Dans ce cas, on accepte
      // tout de même de traiter en sachant que le blank défini par l'utilisateur sera
      // considéré comme celui du survey existant. Mais il faut nécessairement que l'utilisateur
      // renseigne ce champ blank explicitement
      if( bitpix>0 && nanO ) {
         nan = !Double.isNaN(getBlankOrig()); 
      }
      
      if( nanO!=nan || !nan && fits.blank!=blank ) {
         warning("Uncompatible BLANK="+blank+" compared to pre-existing survey BLANK="+fits.blank);
         return false;
      }
      
      int o = cds.tools.pixtools.Util.getMaxOrderByPath(getOutputPath());
      if( o!=getOrder() ) {
         warning("Uncompatible order="+getOrder()+" compared to pre-existing survey order="+o);
         return false;
      }
      
      return true;
   }

   private double getDefaultBlankFromBitpix(int bitpix) {
      return bitpix<0 ? Double.NaN : bitpix==32 ? Integer.MIN_VALUE : bitpix==16 ? Short.MIN_VALUE : 0;
   }

   /** Interprétation de la chaine décrivant les bords à ignorer dans les images sources,
    * soit une seule valeur appliquée à tous les bords,
    * soit 4 valeurs affectées à la java de la manière suivante : Nord, Ouest, Sud, Est 
    * @throws ParseException */
   private int [] parseBorderSize(String s) throws ParseException {
       int [] border = { 0,0,0,0 };
       try { 
           StringTokenizer st = new StringTokenizer(s," ,;-");
           for( int i=0; i<4 && st.hasMoreTokens(); i++ ) {
               String s1 = st.nextToken();
               border[i] = Integer.parseInt(s1);
               if( i==0 ) border[3]=border[2]=border[1]=border[0];
           }
           int x = border[0]; border[0] = border[2]; border[2] = x;  // Permutations pour respecter l'ordre North West South East
       } catch( Exception e ) {
           throw new ParseException("Border error => assume 0", 0);
       }
       return border;
   }

   protected boolean isExistingDir() {
      String path = getInputPath();
      if( path==null ) return false;
      return  (new File(path)).isDirectory();
   }

   protected boolean isExistingAllskyDir() {
      String path = getOutputPath();
      if( path==null ) return false;
      File f = new File(path);
      if( !f.exists() ) return false;
      int order = cds.tools.pixtools.Util.getMaxOrderByPath(path);
      return order!=-1;
   }
   
   protected boolean isExistingIndexDir() {
      String path = getHpxFinderPath();
      if( path==null ) return false;
      File f = new File(path);
      if( !f.exists() ) return false;
      for( File fc : f.listFiles() ) { if( fc.isDirectory() && fc.getName().startsWith("Norder") ) return true; }
      return false;
   }

  /** Positionne le MOC correspondant à l'index */
   protected void setMocIndex(HealpixMoc m) throws Exception {
      mocIndex=m;
   }
   
   /** Retourne le nombre de cellules à calculer (baser sur le MOC de l'index et le MOC de la zone) */
   protected long getNbLowCells() { 
      if( moc==null || getOrder()==-1 ) return -1;
      long nbcells = moc.getUsedArea();
      return nbcells *= (long) Math.pow(4, (getOrder() - moc.getMaxOrder()) );
   }
   
   /** Retourne le volume du Allsky en fits en fonction du nombre de cellules prévues et du bitpix */
   protected long getDiskMem() {
      long nbLowCells = getNbLowCells();
      if( nbLowCells==-1 || bitpix==0 ) return -1;
      long mem = nbLowCells * 512L*512L* (Math.abs(bitpix)/8);
      
      return mem;
   }

   protected int lastNorder3=-2;
   protected void setProgressLastNorder3 (int lastNorder3) { this.lastNorder3=lastNorder3; }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showIndexStat(int statNbFile, int statNbZipFile, long statMemFile, long statMaxSize, 
         int statMaxWidth, int statMaxHeight, int statMaxNbyte) {
      String s;
      if( statNbFile==-1 ) s = "--";
      else {
         s= statNbFile+" file"+(statNbFile>1?"s":"")
         + (statNbZipFile==statNbFile ? " (all gzipped)" : statNbZipFile>0 ? " ("+statNbZipFile+" gzipped)":"")
         + " using "+Util.getUnitDisk(statMemFile)
         + (statNbFile>1 && statMaxSize<0 ? "" : " => biggest: ["+statMaxWidth+"x"+statMaxHeight+"x"+statMaxNbyte+"]");
      }
      nlstat(s);
   }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showTilesStat(int statNbThreadRunning, int statNbThread, long totalTime, 
         int statNbTile, int statNbEmptyTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
         long statNodeAvgTime,long usedMem,long freeMem) {

      long nbLowCells = getNbLowCells();
      
      String sNbCells = nbLowCells==-1 ? "" : "/"+nbLowCells;
      String pourcentNbCells = nbLowCells==-1 ? "" : 
         (Math.round( ( (double)(statNbTile+statNbEmptyTile)/nbLowCells )*1000)/10.)+"% ";
      long nbTilesPerSec = (totalTime)==0 ? 0 : ((statNbTile+statNbEmptyTile)/(totalTime/60000));
      String s=(statNbTile+"+"+statNbEmptyTile)+sNbCells+" tiles computed in "+Util.getTemps(totalTime,true)+" ("
      +pourcentNbCells+ nbTilesPerSec+" tiles/mn) "
      +Util.getTemps(statAvgTime)+"/tile ["+Util.getTemps(statMinTime)+" .. "+Util.getTemps(statMaxTime)+"]"
      +" by "+statNbThreadRunning+"/"+statNbThread+" threads"
      +" using "+Util.getUnitDisk(usedMem)+" ("+Util.getUnitDisk(freeMem)+" free)";

      nlstat(s);

      setProgress(statNbTile+statNbEmptyTile, nbLowCells);
   }

   // Demande d'affichage des stats (dans le TabJpeg)
   protected void showJpgStat(int statNbFile, long statSize, long totalTime) {
      long maxMem = Runtime.getRuntime().maxMemory();
      long totalMem = Runtime.getRuntime().totalMemory();
      long freeMem = Runtime.getRuntime().freeMemory();
      long usedMem = totalMem-freeMem;
      long nbLowCells = getNbLowCells();
      String pourcentNbCells = nbLowCells==-1 ? "" : 
         (Math.round( ( (double)statNbFile/nbLowCells )*1000)/10.)+"%) ";
      String s=statNbFile+"/"+nbLowCells+" tiles computed in "+Util.getTemps(totalTime,true)+" ("
      +pourcentNbCells +" - Ram: "+Util.getUnitDisk(usedMem)+"/"+Util.getUnitDisk(maxMem)+")";

      nlstat(s);
   }

   // Demande d'affichage des stats (dans le TabRgb)
   protected void showRgbStat(int statNbFile, long statSize, long totalTime) { }
   
   protected Action action=null;      // Action en cours (voir Action)
   protected double progress=-1;       // Niveau de progression de l'action en cours, -1 si non encore active, =progressMax si terminée
   protected double progressMax=Double.MAX_VALUE;   // Progression max de l'action en cours (MAX_VALUE si inconnue)
   protected JProgressBar progressBar=null;  // la progressBar attaché à l'action
   protected MyProperties prop=null;
   
   
   protected boolean ignoreStamp;
   public void setIgnoreStamp(boolean flag) { ignoreStamp=true; }
   
   private boolean taskRunning=false;        // true s'il y a un processus de calcul en cours
   public boolean isTaskRunning() { return taskRunning; }
   public void setTaskRunning(boolean flag) { 
      if( flag ) taskAborting=false;            // Si la dernière tache a été interrompue, il faut reswitcher le drapeau
      else progressBar=null;
      taskRunning=flag; 
      resumeWidgets();
   }
   private boolean taskPause=false;          // true si le processus de calcul est en pause
   public boolean isTaskPause() { return taskPause; }
   public void setTaskPause(boolean flag) {
      taskPause=flag;
      resumeWidgets();
   }
   
   protected boolean taskAborting=false;       // True s'il y a une demande d'interruption du calcul en cours
   public void taskAbort() {taskAborting=true; taskPause=false; }
   public boolean isTaskAborting() { 
      if( taskAborting ) return true; 
      while( taskPause ) Util.pause(500);
      return false;
   }
   
   static private SimpleDateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
   static private String getNow() { return DATEFORMAT.format( new Date() ); }
   static long getTime(String date) throws Exception { return DATEFORMAT.parse(date).getTime(); }

   static private String getKeyActionStart(Action a) { return "Processing."+a+".start"; }
   static private String getKeyActionEnd(Action a)   { return "Processing."+a+".end"; }

   public void startAction(Action a) throws Exception { 
      action=a; 
      running(action+" in progress...");
//      updateProperties( getKeyActionStart(action), getNow(),true);
      setProgress(0,-1);
   }
   public void endAction() throws Exception {
      if( isTaskAborting() )  nldone(action+" abort");
      else {
         nldone(action+" done");
//         updateProperties( getKeyActionEnd(action), getNow(),true);
      }
      action=null;
   }
   public Action getAction() { return action; }
   
   /** true si l'action a été correctement estampillée comme terminée dans le fichier des propriétés */
   public boolean actionAlreadyDone(Action a) {
      if( ignoreStamp ) return false;
      try {
         if( prop==null ) loadProperties();
         if( prop==null ) return false;
         String end = prop.getProperty( getKeyActionEnd(a) );
         if( end==null ) return false;    // Jamais encore terminée
         String start = prop.getProperty( getKeyActionStart(a) );
         if( start==null ) return false;  // Jamais encore commencée
//         System.out.println("ActionAlready done: "+a+" start="+start+" end="+end);
         if( getTime(end)<getTime(start) ) return false; // Certainement relancée, mais non-achevée
      } catch( Exception e ) {
         e.printStackTrace();
         return false;
      }
      return true;
   }
   
   /** true si les deux actions sont terminées et que la première précède la seconde */
   public boolean actionPrecedeAction(Action avant,Action apres) {
      if( ignoreStamp ) return false;
      try {
         if( prop==null ) loadProperties();
         if( prop==null ) return false;
         if( !actionAlreadyDone(avant) || !actionAlreadyDone(apres)) return false; // L'une des 2 actions n'a pas été terminée
         String endAvant = prop.getProperty( getKeyActionEnd(avant) );
         String endApres = prop.getProperty( getKeyActionEnd(apres) );
//         System.out.println("actionPrecedeAction done: "+avant+"="+endAvant+" "+apres+"="+endApres);
         if( getTime(endApres)<getTime(endAvant) ) return false;  // L'action avant est postérieure
      } catch( Exception e ) {
         e.printStackTrace();
         return false;
      }
      return true;
   }
   
   public void setProgress(double progress,double progressMax) { setProgress(progress); setProgressMax(progressMax); }
   public void setProgress(double progress) { this.progress=progress; }
   public void setProgressMax(double progressMax) { this.progressMax=progressMax; }
   public void progressStatus() { System.out.print('.'); }
   public void enableProgress(boolean flag) { System.out.println("progress ["+action+"] enable="+flag); }
   public void setProgressBar(JProgressBar bar) { }
   public void resumeWidgets() { }
   
   public void trace(int i, String string) {
	   if (Aladin.levelTrace>=i)
	   System.out.println(string);
   }
   public void setTrace(int trace) {
	   Aladin.levelTrace = trace;
   }

   /**
    * @param verbose the verbose level to set
    */
   public static void setVerbose(boolean verbose) {
	   Context.verbose = verbose;
	   BuilderTiles.DEBUG=true;
   }
   
   /** Verbose or not ? */
   public static int getVerbose() { return Aladin.levelTrace; }

   /**
    * Niveau de verbosité : 
    * -1    rien
    * 0     stats
    * 1-4   traces habituelles d'Aladin
    * @param verbose the verbose to set
    */
   public static void setVerbose(int level) {
	   if (level>=0) {
		   Context.verbose = true;
		   Aladin.levelTrace = level;
	   }
	   else {
		   Context.verbose = false;
		   Aladin.levelTrace = 0;
	   }
   }
   
   public void running(String string)  { System.out.println("RUN   : "+string); }
   public void nldone(String string)   { System.out.println("\nDONE  : "+string);  }
   public void done(String string)     { System.out.println("DONE  : "+string); }
   public void info(String string)     { System.out.println("INFO  : "+string); }
   public void nlwarning(String string){ System.out.println("\nWARN  : "+string); }
   public void warning(String string)  { System.out.println("WARN  : "+string); }
   public void error(String string)    { System.out.println("ERROR : "+string); }
   public void action(String string)   { System.out.println("ACTION: "+string); }
   public void nlstat(String string)   { System.out.println("\nSTAT  : "+string); }
   public void stat(String string)     { System.out.println("STAT  : "+string); }
   
   private boolean validateOutputDone=false;
   public boolean isValidateOutput() { return validateOutputDone; }
   public void setValidateOutput(boolean flag) { validateOutputDone=flag; }
   
   private boolean validateInputDone=false;
   public boolean isValidateInput() { return validateInputDone; }
   public void setValidateInput(boolean flag) { validateInputDone=flag; }

   private boolean validateCutDone=false;
   public boolean isValidateCut() { return validateCutDone; }
   public void setValidateCut(boolean flag) { validateCutDone=flag; }
  
   static private final Astrocoo COO_GAL = new Astrocoo(new Galactic());
   static private final Astrocoo COO_EQU = new Astrocoo(new ICRS());
   static private Astroframe AF_GAL1 = new Galactic();
   static private Astroframe AF_ICRS1 = new ICRS();
   
   
   /** Création, ou mise à jour du fichier des Properties associées au survey */
   protected void writePropertiesFile() throws Exception {
      
      // Propriétés à mettre à jour de toutes manières
      updateProperties(
            new String[] { PlanHealpix.KEY_PROCESSING_DATE, PlanHealpix.KEY_COORDSYS,
                           PlanHealpix.KEY_ISCOLOR,         PlanHealpix.KEY_ALADINVERSION,
                           PlanHealpix.KEY_LABEL,           PlanHealpix.KEY_MAXORDER
                          },
            new String[] { getNow(),
                           getFrame()==Localisation.ICRS ? "C" : getFrame()==Localisation.ECLIPTIC ? "E" : "G",
                           isColor()+"",
                           Aladin.VERSION,
                           getLabel(),
                           getOrder()+""
                         },
            true);
      
      // Propriétés à mettre que si elles n'existent pas encore
      String order = getOrder()==-1 ? (String)null : getOrder()+"";
      updateProperties( new String[]{ PlanHealpix.KEY_IMAGESOURCEPATH, PlanHealpix.KEY_MAXORDER}, 
                       new String[]{ "path:$1",                       order},
                       false );
   }

   /** Mise à jour d'une propriété => voir updatePropertie(String [],String []) */
   protected void updateProperties(String key, String value, boolean overwrite) throws Exception { 
      updateProperties( new String[] { key }, new String [] { value }, overwrite );
   }
   
   /** Mise à jour du fichier des propriétés associées au survey HEALPix (propertie file dans la racine)
    * Conserve les clés/valeurs existantes. 
    * @param key liste des clés à mettre à jour
    * @param value liste des valuers associées
    * @param overwrite si false, ne peut modifier une clé/valeur déjà existante
    * @throws Exception
    */
   protected void updateProperties(String[] key, String[] value,boolean overwrite) throws Exception {
      
      waitingPropertieFile();
      try {
         String propFile = getOutputPath()+Util.FS+PlanHealpix.PROPERTIES;

         // Chargement des propriétés existantes
         prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            if( !f.canRead() || !f.canWrite() ) throw new Exception("Propertie file not available ! ["+propFile+"]");
            FileInputStream in = new FileInputStream(propFile); 
            prop.load(in);
            in.close();
         }

         // Mise à jour des propriétés
         for( int i=0; i<key.length; i++ ) {

            // insertion ou remplacement
            if( overwrite ) {
               if( value[i]==null ) prop.remove(key[i]);
               else if( value[i]!=null ) prop.setProperty(key[i], value[i]);

               // insertion que si nouveau
            } else {
               String v = prop.getProperty(key[i]);
               if( v==null && value[i]!=null ) prop.setProperty(key[i], value[i]);
            }
         }

         // Remplacement du précédent fichier
         String tmp = getOutputPath()+Util.FS+PlanHealpix.PROPERTIES+".tmp";
         File ftmp = new File(tmp);
         if( ftmp.exists() ) ftmp.delete();
         File dir = new File( getOutputPath() );
         if( !dir.exists() && !dir.mkdir() ) throw new Exception("Cannot create output directory");
         FileOutputStream out = new FileOutputStream(ftmp);
         prop.store( out, null);
         out.close();

         if( f.exists() && !f.delete() ) throw new Exception("Propertie file locked ! (cannot delete)");
         if( !ftmp.renameTo(new File(propFile)) ) throw new Exception("Propertie file locked ! (cannot rename)");

      }
      finally { releasePropertieFile(); }
   }
   
   /** Lecture des propriétés */
   protected void loadProperties() throws Exception {
      waitingPropertieFile();
      try {
         String propFile = getOutputPath()+Util.FS+PlanHealpix.PROPERTIES;
         prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            if( !f.canRead() || !f.canWrite() ) throw new Exception("Propertie file not available ! ["+propFile+"]");
            FileInputStream in = new FileInputStream(propFile); 
            prop.load(in);
            in.close();
         }
      }
      finally { releasePropertieFile(); }
   }

   // Gestion d'un lock pour accéder de manière exclusive aux fichiers des propriétés
   transient private boolean lock;
   private final Object lockObj= new Object();
   private void waitingPropertieFile() {
      while( !getLock() ) {
         try { Thread.currentThread().sleep(100); } catch( InterruptedException e ) {  }
      }
   }
   private void releasePropertieFile() { lock=false; }
   private boolean getLock() {
      synchronized( lockObj ) {
         if( lock ) return false;
         lock=true;
         return true;
      }
   }

   protected double[] gal2ICRSIfRequired(double al, double del) { return gal2ICRSIfRequired(new double[]{al,del}); }
   protected double[] gal2ICRSIfRequired(double [] aldel) {
      if( frame==Localisation.ICRS ) return aldel;
      Astrocoo coo = (Astrocoo) COO_GAL.clone(); 
      coo.set(aldel[0],aldel[1]);
      coo.convertTo(AF_ICRS1);
      aldel[0] = coo.getLon();
      aldel[1] = coo.getLat();
      return aldel;
   }
   protected double[] ICRS2galIfRequired(double al, double del) { return ICRS2galIfRequired(new double[]{al,del}); }
   protected double[] ICRS2galIfRequired(double [] aldel) {
      if( frame==Localisation.ICRS ) return aldel;
      Astrocoo coo = (Astrocoo) COO_EQU.clone(); 
      coo.set(aldel[0], aldel[1]);
      coo.convertTo(AF_GAL1);
      aldel[0] = coo.getLon();
      aldel[1] = coo.getLat();
      return aldel;
   }
   
   private int[] xy2hpx = null;
   private int[] hpx2xy = null;

   /** Méthode récursive utilisée par createHealpixOrder */
   private void fillUp(int[] npix, int nsize, int[] pos) {
      int size = nsize * nsize;
      int[][] fils = new int[4][size / 4];
      int[] nb = new int[4];
      for (int i = 0; i < size; i++) {
         int dg = (i % nsize) < (nsize / 2) ? 0 : 1;
         int bh = i < (size / 2) ? 1 : 0;
         int quad = (dg << 1) | bh;
         int j = pos == null ? i : pos[i];
         npix[j] = npix[j] << 2 | quad;
         fils[quad][nb[quad]++] = j;
      }
      if (size > 4)
         for (int i = 0; i < 4; i++)
            fillUp(npix, nsize / 2, fils[i]);
   }

   /** Creation des tableaux de correspondance indice Healpix <=> indice XY */
   public void createHealpixOrder(int order) {
      int nsize = (int) CDSHealpix.pow2(order);
      xy2hpx = new int[nsize * nsize];
      hpx2xy = new int[nsize * nsize];
      fillUp(xy2hpx, nsize, null);
      for (int i = 0; i < xy2hpx.length; i++)
         hpx2xy[xy2hpx[i]] = i;
   }

   /**
    * Retourne l'indice XY en fonction d'un indice Healpix => nécessité
    * d'initialiser au préalable avec createHealpixOrdre(int)
    */
   final public int xy2hpx(int hpxOffset) {
      return xy2hpx[hpxOffset];
   }

   /**
    * Retourne l'indice XY en fonction d'un indice Healpix => nécessité
    * d'initialiser au préalable avec createHealpixOrdre(int)
    */
   final public int hpx2xy(int xyOffset) {
      return hpx2xy[xyOffset];
   }
}
