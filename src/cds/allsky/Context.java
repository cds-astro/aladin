package cds.allsky;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;

import cds.aladin.Aladin;
import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.aladin.PlanHealpix;
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
//   protected boolean skySub = false;         // true s'il faut appliquer une soustraction du fond (via le cacheFits)
   protected String skyvalName;                // Nom du champ à utiliser dans le header pour soustraire un valeur de fond (via le cacheFits)
   
   protected int bitpix = -1;                // BITPIX de sortie
   protected double blank = Double.NaN;      // Valeur du BLANK en sortie
   protected double bZero=0;                 // Valeur BZERO de la boule Healpix à générer
   protected double bScale=1;                // Valeur BSCALE de la boule HEALPix à générer
   protected boolean bscaleBzeroSet=false;   // true si le bScale/bZero de sortie a été positionnés
   protected double[] cut;   // Valeurs cutmin,cutmax, datamin,datamax pour la boule Healpix à générer
   protected TransfertFct fct = TransfertFct.LINEAR; // Fonction de transfert des pixels fits -> jpg
   private Method method = Context.Method.MEDIAN;
   
   protected int order = -1;                 // Ordre maximale de la boule HEALPix à générer              
   protected int frame = Localisation.ICRS;  // Système de coordonnée de la boule HEALPIX à générée
   protected HealpixMoc moc = null;          // Zone du ciel à traiter (décrite par un MOC)
   protected CacheFits cacheFits;            // Cache FITS pour optimiser les accès disques à la lecture
   protected boolean isRunning=false;        // true s'il y a un processus de calcul en cours
   
   protected long nbLowCells= -1;              // Pour les stats, nombre de cellules de bas niveaux à calculer (basé sur le moc de l'index)
   
   protected CoAddMode coAdd;                  // NORMALEMENT INUTILE DESORMAIS (méthode de traitement)
   
   public Context() {}

   // Getters
   public String getLabel() { return label; }
   public int[] getBorderSize() { return borderSize; }
   public int getOrder() { return order<0 ? 3 : order; }
   public int getFrame() { return frame; }
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
   public boolean hasAlternateBlank() { return hasAlternateBlank; }
   public HealpixMoc getMoc() { return moc; }
   public CoAddMode getCoAddMode() { return coAdd; }
   public double[] getCut() { return cut; }
   public double[] getCutOrig() { return cutOrig; }
   public String getSkyval() { return skyvalName; }
   public boolean isSkySub() { return skyvalName!=null; }
   public boolean isRunning() { return isRunning; }
   public boolean isColor() { return bitpixOrig==0; }
   public boolean isBScaleBZeroSet() { return bscaleBzeroSet; }
   public boolean isInMocTree(int order,long npix)  { return moc==null || moc.isInTree(order,npix); }
   public boolean isInMocLevel(int order,long npix) { return moc==null || moc.isIn(order,npix); }
   public boolean isMocDescendant(int order,long npix) { return moc==null || moc.isDescendant(order,npix); }
   
   // Setters
   public void setBorderSize(String borderSize) throws ParseException { this.borderSize = parseBorderSize(borderSize); }
   public void setBorderSize(int[] borderSize) { this.borderSize = borderSize; }
   public void setOrder(int order) { this.order = order; }
   public void setFrame(int frame) { this.frame=frame; }
   public void setFrameName(String frame) { this.frame= (frame.equalsIgnoreCase("G"))?Localisation.GAL:Localisation.ICRS; }
   public void setInitDir(String txt) { }
   public void setSkyValName(String s ) { skyvalName=s; }
   public void setInputPath(String path) { this.inputPath = path; 
   		// cherche le dernier mot et le met dans le label
   		label = path==null ? null : path.substring(path.lastIndexOf(Util.FS) + 1);
   }
   public void setOutputPath(String path) { this.outputPath = path; }
   public void sethpxFinderPath(String path) { hpxFinderPath = path; }
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
   public void setIsRunning(boolean flag) { isRunning=flag; }
   public void setAbort() { setIsRunning(false); }
   public void setCut(double [] cut) { this.cut=cut; }
   public void setPixelCut(String scut) throws Exception {
       StringTokenizer st = new StringTokenizer(scut," ");
       int i=0;
       if( cut==null ) cut = new double[4];
       while( st.hasMoreTokens() ) {
          String s = st.nextToken();
          try { 
             double d = Double.parseDouble(s);
             cut[i++]=d;
          } catch( Exception e) {
             setTransfertFct(s);
          }
          
       }
       if( i==1 || i>2 ) throw new Exception("pixelCut parameter error");

       setCutOrig(this.cut);
   }
   
   public String getTransfertFct() { return fct.toString().toLowerCase(); }
   
   public void setTransfertFct(String txt) {
      this.fct=TransfertFct.valueOf(txt.toUpperCase());
  }
   
   protected enum Method {
	   MEDIAN, MEAN;
	}
   
   /**
    * @param method the method to set
    * @see Context#MEDIAN
    * @see Context#MEAN
    */
   public void setMethod(Method method) {
	   this.method = method;
   }
   public void setMethod(String method) {
	   this.method = Method.valueOf(method.toUpperCase());
   }
   public Method getMethod() { return method; }

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
   
   // Mon propre split - sinon gros problème en cas de blancs dédoublés
   private String [] split(String s,String c) {
      StringTokenizer st = new StringTokenizer(s,c);
      String [] w = new String[st.countTokens()];
      for( int i=0; i<w.length; i++ ) w[i]=st.nextToken();
      return w;
   }
   
   /**
    * Lit et charge en mémoire un premier fichier de properties 
    */
   protected void loadProp() {
      try {
         prop = new Properties();
         InputStream in=null;
         propPathFile = getOutputPath()+Util.FS+LOGFILE;
         in = new FileInputStream(new File(propPathFile));
         if( in!=null ) {
            prop.load(in);
         }
      } catch( Exception e ) { /*error("No properties file found");*/ }
      
   }
   
   protected double coef;
   

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
       if (w > 1024) w = 1024;
       if (h > 1024) h = 1024;
       file.loadFITS(file.getFilename(), 0, 0, w, h);

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
            if( (in.getType()&MyInputStream.FITS) != MyInputStream.FITS ) continue;            
            Aladin.trace(4, "Context.findImgEtalon: "+path+"...");
            setImgEtalon(path);
            return true;
            
         }  catch( Exception e) { Aladin.trace(4, "findImgEtalon : " +e.getMessage()); continue; }
      }
      return false;
   }
   
   public void setSkyval(String fieldName) {
       this.skyvalName = fieldName.toUpperCase();
       if (cacheFits != null) cacheFits.setSkySub(skyvalName);
   }
   
   public void setCache(CacheFits cache) {
      this.cacheFits = cache;
      cache.setSkySub(skyvalName);
   }

   protected HealpixMoc setMoc(String s) {
      if( s.length()==0 ) return null;
      try {
         HealpixMoc m = new HealpixMoc(s);
         if( m.getSize()==0 ) throw new Exception();
         moc = m;
      } catch( Exception e ) {
         return null;
      }
      return moc;
   }

   public void setMoc(HealpixMoc region) {
      moc = region;
   }
   

   /** Initialisation des paramètres (ne sert que pour contextGui) */
   public void initParameters() {
      
      bitpix = getBitpix();
      cut = getCut();
      
      bitpixOrig = getBitpixOrig();
      cutOrig = getCutOrig();
      blankOrig = getBlankOrig();
      bZeroOrig = getBZeroOrig();
      bScaleOrig = getBScaleOrig();

      // Le blank de sortie est imposée
      blank = getDefaultBlankFromBitpix(bitpix);
     
      // Y a-t-il un changement de bitpix ?
      // Les cuts changent ainsi que le blank
      if( bitpix != bitpixOrig ) {
         cut[2] = bitpix==-64?Double.MIN_VALUE : bitpix==-32? Float.MIN_VALUE
               : bitpix==64?Long.MIN_VALUE+1 : bitpix==32?Double.MIN_VALUE+1 : bitpix==16?Short.MIN_VALUE+1:1;
         cut[3] = bitpix==-64?Double.MAX_VALUE : bitpix==-32? Float.MAX_VALUE
               : bitpix==64?Long.MAX_VALUE : bitpix==32?Double.MAX_VALUE : bitpix==16?Short.MAX_VALUE:255;
         coef = (cut[3]-cut[2]) / (cutOrig[3]-cutOrig[2]);

         cut[0] = (cutOrig[0]-cutOrig[2])*coef + cut[2];
         cut[1] = (cutOrig[1]-cutOrig[2])*coef + cut[2];

//         blank = getDefaultBlankFromBitpix(bitpix);
         bZero = bZeroOrig + bScaleOrig*(cutOrig[2] - cut[2]/coef);
         bScale = bScaleOrig/coef;
         
         Aladin.trace(3,"Change BITPIX from "+bitpixOrig+" to "+bitpix);
         Aladin.trace(3,"Map original pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] " +
                        "to ["+cut[2]+" .. "+cut[3]+"]");
         Aladin.trace(3,"Change BZERO,BSCALE,BLANK="+bZeroOrig+","+bScaleOrig+","+blankOrig
               +" to "+bZero+","+bScale+","+blank);
      
         // Pas de changement de bitpix
      } else {
//         blank=blankOrig;
//         if( Double.isNaN(blank) && bitpix>0 ) blank = getDefaultBlankFromBitpix(bitpix);
         bZero=bZeroOrig;
         bScale=bScaleOrig;
         Aladin.trace(3,"BITPIX kept "+bitpix+" BZERO,BSCALE,BLANK="+bZero+","+bScale+","+blank);
      }
      

      // si besoin redéfinit le blank 
//      if (isSkySub() && bitpix>0) {
//    	  blank=getDefaultBlankFromBitpix(bitpix);
//      }
   }
   
   public boolean verifCoherence() {
      if( coAdd==CoAddMode.REPLACEALL ) return true;
      String fileName=getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits";
      if( !(new File(fileName)).exists() ) return true;
      Fits fits = new Fits();
      try { fits.loadHeaderFITS(fileName); }
      catch( Exception e ) { return true; }
      if( fits.bitpix!=bitpix ) {
         warning("Incompatible BITPIX="+bitpix+" compared to pre-existing survey BITPIX="+fits.bitpix);
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
         warning("Incompatible BLANK="+blank+" compared to pre-existing survey BLANK="+fits.blank);
         return false;
      }
      
      int o = cds.tools.pixtools.Util.getMaxOrderByPath(getOutputPath());
      if( o!=getOrder() ) {
         warning("Incompatible order="+getOrder()+" compared to pre-existing survey order="+o);
         return false;
      }
      
      return true;
   }

   private double getDefaultBlankFromBitpix(int bitpix) {
      return bitpix<0 ? Double.NaN : bitpix==32 ? Double.MIN_VALUE : bitpix==16 ? Short.MIN_VALUE : 0;
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
      String path = getInputPath();
      if( path==null ) return false;
      return  (new File(path)).exists();
   }

   protected boolean isExistingAllskyDir() {
      String path = getOutputPath();
      if( path==null ) return false;
      return (new File(path+Util.FS+"Norder3")).exists();
   }
   
   /** Mémorise le nombre de cellules de bas niveau à calculer  */
   protected void setNbLowCells(long nbLowCells) {
      this.nbLowCells=nbLowCells;
   }
   
   /** Retourne le nombre de cellules à calculer (baser sur le MOC de l'index) */
   protected long getNbLowCells() { return nbLowCells; }
   
   /** Retourne le volume du Allsky en fits en fonction du nombre de cellules prévues et du bitpix */
   protected long getDiskMem() {
      if( nbLowCells==-1 || bitpix==0 ) return -1;
      long mem = nbLowCells * 512L*512L* Math.abs(bitpix);
      
      return mem;
   }

   protected void enableProgress(boolean selected, int mode) { }
   protected void setProgress(int value) { System.out.print(".");}
   protected void setProgress(int mode, int value) { System.out.println(value+"%");}
   protected void preview (int n3) { }

   private long statTime = System.currentTimeMillis();
   // Demande d'affichage des stats (dans le TabBuild)
   protected void showIndexStat(int statNbFile, int statNbZipFile, long statMemFile, long statMaxSize, 
         int statMaxWidth, int statMaxHeight, int statMaxNbyte) {
	   // affiche sur la sortie standard toutes les 30 sec
	   if ((System.currentTimeMillis()-statTime)>30000) {
		   String s;
		   if( statNbFile==-1 ) s = "--";
		   else {
			   s= statNbFile+" file"+(statNbFile>1?"s":"")
			   + (statNbZipFile==statNbFile ? " (all gzipped)" : statNbZipFile>0 ? " ("+statNbZipFile+" gzipped)":"")
			   + " using "+Util.getUnitDisk(statMemFile)
			   + (statNbFile>1 && statMaxSize<0 ? "" : " => biggest: ["+statMaxWidth+"x"+statMaxHeight+"x"+statMaxNbyte+"]");
		   }
		   nlstat(s);
		   statTime = System.currentTimeMillis();
	   }
   }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showBuildStat(int statNbThreadRunning, int statNbThread, long totalTime, 
         int statNbTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
         long statNodeAvgTime) {
	// affiche sur la sortie standard toutes les 30 sec
	   if (/* verbose &&*/ (System.currentTimeMillis()-statTime)>30000) {
		      long maxMem = Runtime.getRuntime().maxMemory();
		      long totalMem = Runtime.getRuntime().totalMemory();
		      long freeMem = Runtime.getRuntime().freeMemory();
		      long usedMem = totalMem-freeMem;
		      
		      String sNbCells = nbLowCells==-1 ? "" : "/"+nbLowCells;
		      String pourcentNbCells = nbLowCells==-1 ? "" : (Math.round( ( (double)statNbTile/nbLowCells )*1000)/10.)+"%) ";
		      
		      String s=statNbTile+sNbCells+" tiles computed in "+Util.getTemps(totalTime,true)+" ("
		          +pourcentNbCells
		          +Util.getTemps(statAvgTime)+" per tile ["+Util.getTemps(statMinTime)+" .. "+Util.getTemps(statMaxTime)+"]"
		          +" by "+statNbThreadRunning+"/"+statNbThread+" threads"
		          +" - RAM: "+Util.getUnitDisk(usedMem)+"/"+Util.getUnitDisk(maxMem)
		          +" (FITS cache size: "+Util.getUnitDisk(cacheFits.getStatMem())+")";

//		      String s= "thread: "+(statNbThreadRunning==-1?"":statNbThreadRunning+" / "+statNbThread)
//		      + " - cache: "+Util.getUnitDisk(cacheFits.getStatMem())
//		           +" (ram:"+cacheFits.getStatNbFind()
//		           +" disk:"+cacheFits.getStatNbOpen()+" free:"+cacheFits.getStatNbFree()+")"
//		      + " - mem: "+Util.getUnitDisk(usedMem)+"/"+Util.getUnitDisk(maxMem);
		      nlstat(s);
			  statTime = System.currentTimeMillis();
	   }
   }
   
   // Demande d'affichage des stats de fin de travail
   protected void showBuildStat(long totalTime,int statNbTile,int statNodeTile) {
      int nbRemoved = (int)(nbLowCells-statNbTile);
      String s = statNbTile+" tiles computed in "+Util.getTemps(totalTime,true);
      if( nbRemoved>0 ) s=s+" - "+nbRemoved+" tiles have been ignored (fully BLANK)";
      nldone(s);
      long mem = ((long)statNbTile+(long)statNodeTile)*512L*512L*(long)Math.abs(bitpix);
      info("All FITS tiles generated: "+Util.getUnitDisk(mem));
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
	   BuilderController.DEBUG=true;
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
   
   public void running(String string) {
      System.out.println("RUN   : "+string);
   }
   
   public void nldone(String string) {
      System.out.println("\nDONE  : "+string);
   }
   
   public void done(String string) {
      System.out.println("DONE  : "+string);
   }
   
   public void info(String string) {
      System.out.println("INFO  : "+string);
   }

   public void warning(String string) {
      System.out.println("WARN  : "+string);
   }

   public void error(String string) {
      System.out.println("ERROR : "+string);
   }

   public void action(String string) {
      System.out.println("ACTION: "+string);
   }

   public void nlstat(String string) {
      System.out.println("\nSTAT  : "+string);
   }

   public void doneIndex() {
      nldone("HEALPix index created !");
      info("The generated index covers "+getNbLowCells()+" low level HEALPix cells (depth="+getOrder()+")");

      prop.put("IndexCreation", DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
      try {
         prop.store(new FileOutputStream(propPathFile),null);
      } catch (IOException e) {
         error(e.getMessage());
      }
      
   }
   
   public void doneAllsky() {
      done("Allsky view created !");
      if (prop==null) { error("No properties file found"); return;}
      
      prop.put("AllskyCreation", DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
      try {
         prop.store(new FileOutputStream(propPathFile),null);
      } catch (IOException e) {
         error(e.getMessage());
      }

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
   private Properties prop = null;
   private String propPathFile = null;

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
