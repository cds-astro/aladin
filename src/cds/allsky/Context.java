package cds.allsky;

import java.io.File;
import java.text.ParseException;
import java.util.StringTokenizer;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.aladin.PlanBG;
import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.HpixTree;

/**
 * Classe pour unifier les accès aux paramètres nécessaires pour les calculs
 * + accès aux méthodes d'affichages qui sont redirigées selon l'interface
 * existante
 * @author Oberto + Fernique
 *
 */
public class Context {

   protected String label;                   // Nom du survey
   
   protected String inputPath;               // Répertoire des images origales
   protected String outputPath;              // Répertoire de la boule HEALPix à générer
   protected String hpxFinderPath;           // Répertoire de l'index Healpix (null si défaut => dans outputPath/HpxFinder)
   protected String imgEtalon;               // Nom (complet) de l'image qui va servir d'étalon
   
   protected String regex = "*.fits";        // Expression régulière à appliquer pour sélectionner les images à traiter
   
   protected int bitpixOrig = -1;            // BITPIX des images originales
   protected double blankOrig;               // Valeur du BLANK en entrée
   protected double bZeroOrig=0;             // Valeur BZERO d'origine
   protected double bScaleOrig=1;            // Valeur BSCALE d'origine
   protected double[] cutOrig;               // Valeurs cutmin,cutmax, datamin,datamax des images originales
   protected int[] borderSize = {0,0,0,0};   // Bords à couper sur les images originales
//   protected boolean skySub = false;         // true s'il faut appliquer une soustraction du fond (via le cacheFits)
   private String skyvalName;                // Nom du champ à utiliser dans le header pour soustraire un valeur de fond (via le cacheFits)
   
   protected int bitpix = -1;                // BITPIX de sortie
   protected double blank;                   // Valeur du BLANK en sortie
   protected double bZero;                   // Valeur BZERO de la boule Healpix à générer
   protected double bScale;                  // Valeur BSCALE de la boule HEALPix à générer
   protected boolean bscaleBzeroSet=false;   // true si le bScale/bZero de sortie a été positionnés
   protected double[] cut;                   // Valeurs cutmin,cutmax, datamin,datamax pour la boule Healpix à générer
   private HpixTree region;                  // Definition des losanges à traiter sous forme Norder/Npix
   
   protected boolean fading = false;         // true pour appliquer un "fondu-enchainé" sur les recouvrements
   protected int order = -1;                 // Ordre maximale de la boule HEALPix à générer              
   protected int frame = Localisation.ICRS;  // Système de coordonnée de la boule HEALPIX à générée
   protected HpixTree moc = null;            // Zone du ciel à traiter (décrite par un MOC)
   protected CacheFits cacheFits;            // Cache FITS pour optimiser les accès disques à la lecture
   protected boolean isRunning=false;        // true s'il y a un processus de calcul en cours
//   protected boolean isColor=false;          // true si les images d'entrée sont des jpeg couleur 
   
   protected CoAddMode coAdd;                      // NORMALEMENT INUTILE DESORMAIS (méthode de traitement)
//   protected boolean keepBB = false;         // true pour conserver le BZERO et BSCALE originaux
   
   public Context() {}

   // Getters
   public String getLabel() { return label; }
   public int[] getBorderSize() { return borderSize; }
   public int getOrder() { return order<0 ? 3 : order; }
   public int getFrame() { return frame; }
   public String getRegex() { return regex; }
   public String getFrameName() { return Localisation.getFrameName(frame); }
   public CacheFits getCache() { return cacheFits; }
   public String getInputPath() { return inputPath; }
   public String getOutputPath() { return outputPath; }
   public String getHpxFinderPath() { return hpxFinderPath!=null ? hpxFinderPath : Util.concatDir( getOutputPath(),Constante.HPX_FINDER); }
   public String getImgEtalon() { return imgEtalon; }
   public int getBitpixOrig() { return bitpixOrig; }
   public int getBitpix() { return bitpix; }
   public double getBScaleOrig() { return bScaleOrig; }
   public double getBZeroOrig() { return bZeroOrig; }
   public double getBZero() { return bZero; }
   public double getBScale() { return bScale; }
   public double getBlank() { return blank; }
   public double getBlankOrig() { return blankOrig; }
   public HpixTree getMoc() { return moc; }
   public CoAddMode getCoAddMode() { return coAdd; }
   public double[] getCut() { return cut; }
   public double[] getCutOrig() { return cutOrig; }
   public boolean isFading() { return fading; }
   public boolean isSkySub() { return skyvalName!=null; }
   public boolean isRunning() { return isRunning; }
   public boolean isColor() { return bitpixOrig==0; }
   public boolean isBScaleBZeroSet() { return bscaleBzeroSet; }
   
   // Setters
   public void setBorderSize(String borderSize) throws ParseException { this.borderSize = parseBorderSize(borderSize); }
   public void setBorderSize(int[] borderSize) { this.borderSize = borderSize; }
   public void setOrder(int order) { this.order = order; }
   public void setFading(boolean fading) { this.fading = fading; }
   public void setFrame(int frame) { this.frame=frame; }
   public void setFrameName(String frame) { this.frame=
       (frame.equalsIgnoreCase("G"))?Localisation.GAL:Localisation.ICRS; }
   public void setRegex(String regex) { this.regex = regex; }
   public void setInitDir(String txt) { }
   public void setInputPath(String path) { this.inputPath = path; }
   public void setOutputPath(String path) { this.outputPath = path; }
   public void sethpxFinderPath(String path) { hpxFinderPath = path; }
   public void setImgEtalon(String filename) { imgEtalon = filename; }
   public void setCoAddMode(CoAddMode coAdd) { this.coAdd = coAdd; }
   public void setBScaleOrig(double x) { bScale = bScaleOrig = x; }
   public void setBZeroOrig(double x) { bZero = bZeroOrig = x; }
   public void setBScale(double x) { bScale = x; bscaleBzeroSet=true; }
   public void setBZero(double x) { bZero = x; bscaleBzeroSet=true; }
   public void setBitpixOrig(int bitpix) { bitpixOrig = this.bitpix = bitpix; }
   public void setBitpix(int bitpix) { this.bitpix = bitpix; }
   public void setBlankOrig(double blankOrig) { this.blank = this.blankOrig = blankOrig; }
   public void setBlank(double blank) { this.blank = blank;}
   public void setColor(boolean color) { if(color) this.bitpixOrig=0;}
   public void setIsRunning(boolean flag) { isRunning=flag; }
   public void setCut(double [] cut) { this.cut=cut; }
   public void setPixelCut(String cut) {
       String vals[] = cut.split(" ");
	   if (vals.length==2 && this.cut !=null) {
		   this.cut[0] = Double.parseDouble(vals[0]);
		   this.cut[1] = Double.parseDouble(vals[1]);
	   }
       else if (vals.length==4)
           this.cut = new double[] {Double.parseDouble(vals[0]),Double.parseDouble(vals[1]),
               Double.parseDouble(vals[2]),Double.parseDouble(vals[3])};
   }
   
   public void setDataCut(String cut) {
       String vals[] = cut.split(" ");
	   if (vals.length==2 && this.cut != null) {
		   this.cut[2] = Double.parseDouble(vals[0]);
		   this.cut[3] = Double.parseDouble(vals[1]);
	   }
       else if (vals.length==4)
           this.cut = new double[] {Double.parseDouble(vals[0]),Double.parseDouble(vals[1]),
               Double.parseDouble(vals[2]),Double.parseDouble(vals[3])};

   }

   public void setCutOrig(double [] cutOrig) {
      this.cutOrig=cutOrig;
      cut = new double[cutOrig.length];
      System.arraycopy(cutOrig, 0, cut, 0, cutOrig.length);
   }
   
   protected double coef;
   

   protected void initCut(Fits file) {
       int w = file.width;
       int h = file.height;
       if (w > 1024) w = 1024;
       if (h > 1024) h = 1024;
       try {
           file.loadFITS(file.getFilename(), 0, 0, w, h);
           double[] cut = file.findAutocutRange();
           setCutOrig(cut);
       } catch (Exception e) {
           e.printStackTrace();
       }
   }

   /**
    * Sélectionne un fichier de type FITS (ou équivalent) dans le répertoire donné => va servir d'étalon
    * Utilise un cache une case pour éviter les recherches redondantes
    * @return true si trouvé
    */
   boolean findImgEtalon(String rootPath) {
      File main = new File(rootPath);
      Fits fitsfile = new Fits();
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
            Aladin.trace(4, "MainPanel.findImgEtalon: loading header "+path+"...");
            fitsfile.loadHeaderFITS(path);
            setImgEtalon(path);
            setBitpixOrig(fitsfile.bitpix);
            if( !isColor() ) {
               setBZeroOrig(fitsfile.bzero);
               setBScaleOrig(fitsfile.bscale);
               setBlankOrig(fitsfile.blank);
            }
            initCut(fitsfile);
            return true;
            
         }  catch (Exception e) { continue; }
      }
      return false;
   }

   public void initChangeBitpix() {
      int bitpix = getBitpix();
      cut[2] = bitpix==-64?Double.MIN_VALUE : bitpix==-32? Float.MIN_VALUE
            : bitpix==64?Long.MIN_VALUE+1 : bitpix==32?Double.MIN_VALUE+1 : bitpix==16?Short.MIN_VALUE+1:1;
      cut[3] = bitpix==-64?Double.MAX_VALUE : bitpix==-32? Float.MAX_VALUE
            : bitpix==64?Long.MAX_VALUE : bitpix==32?Double.MAX_VALUE : bitpix==16?Short.MAX_VALUE:255;
      coef = (cut[3]-cut[2]) / (cutOrig[3]-cutOrig[2]);
      
      cut[0] = (cutOrig[0]-cutOrig[2])*coef + cut[2];
      cut[1] = (cutOrig[1]-cutOrig[2])*coef + cut[2];
      
      blank = bitpix<0 ? Double.NaN : bitpix==32 ? Double.MIN_VALUE : bitpix==16 ? Short.MIN_VALUE : 0;
      setBZero( bZeroOrig + bScaleOrig*(cutOrig[2] - cut[2]/coef) );
      setBScale( bScaleOrig/coef );
   }
   
   public void setSkyval(String fieldName) {
       this.skyvalName = fieldName;
       if (cacheFits != null) cacheFits.setSkySub(skyvalName);
   }
//   public void setSkySub(boolean skySub) {
//      this.skySub = skySub;
//      if (cacheFits != null) cacheFits.setSkySub(skySub);
//   }
   public void setCache(CacheFits cache) {
      this.cacheFits = cache;
      cache.setSkySub(skyvalName);
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

   protected void stop() { }
   
   protected boolean isExistingDir() {
      return  (new File(getInputPath())).exists();
   }

   protected boolean isExistingAllskyDir() {
      return (new File(getOutputPath())).exists();
   }

   protected void enableProgress(boolean selected, int mode) { }
   protected void setProgress(int mode, int value) { }
   protected void preview (int n3) { }


   // Demande d'affichage des stats (dans le TabBuild)
   protected void showIndexStat(int statNbFile, int statNbZipFile, long statMemFile, long statMaxSize, 
         int statMaxWidth, int statMaxHeight, int statMaxNbyte) {
   }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showBuildStat(int statNbThreadRunning, int statNbThread, long totalTime, 
         int statNbTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
         long statNodeAvgTime) {
   }

   // Demande d'affichage des stats (dans le TabJpeg)
   protected void showJpgStat(int statNbFile, long statSize, long totalTime) { }

   // Demande d'affichage des stats (dans le TabRgb)
   protected void showRgbStat(int statNbFile, long statSize, long totalTime) { }
   
//   public void convertCut(int bitpix) {
//      double[] cut = getCut();
//      double [] oldminmax = new double[] {cut[2],cut[3]};
//      cut[0] = Fits.toBitpixRange(cut[0], bitpix, oldminmax);
//      cut[1] = Fits.toBitpixRange(cut[1], bitpix, oldminmax);
//      setCut(cut);
//   }

   public void warning(String string) {
       String s_WARN    = "WARNING";//Aladin.getChaine().getString("WARNING");
       System.out.println(s_WARN+" "+string);
   }

   static private final Astrocoo COO_GAL = new Astrocoo(new Galactic());
   static private final Astrocoo COO_EQU = new Astrocoo(new ICRS());
   static private Astroframe AF_GAL1 = new Galactic();
   static private Astroframe AF_ICRS1 = new ICRS();

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



   protected HpixTree setRegion(String s) {
       if( s.length()==0 ) return null;
       HpixTree hpixTree = new HpixTree(s);
       if( hpixTree.getSize()==0 ) return null;
       this.region = hpixTree;
       return hpixTree;
   }

   /**
    * @param region the region to set
    */
   public void setRegion(HpixTree region) {
       this.region = region;
   }



}
