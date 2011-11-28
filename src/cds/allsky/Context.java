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
 * Classe pour unifier les acc�s aux param�tres n�cessaires pour les calculs
 * + acc�s aux m�thodes d'affichages qui sont redirig�es selon l'interface
 * existante
 * @author Oberto + Fernique
 *
 */
public class Context {

	private static boolean verbose = false;
	protected int trace=0;					// Niveau de debugging
   protected String label;                   // Nom du survey
   
   protected String inputPath;               // R�pertoire des images origales
   protected String outputPath;              // R�pertoire de la boule HEALPix � g�n�rer
   protected String hpxFinderPath;           // R�pertoire de l'index Healpix (null si d�faut => dans outputPath/HpxFinder)
   protected String imgEtalon;               // Nom (complet) de l'image qui va servir d'�talon
   
   protected int bitpixOrig = -1;            // BITPIX des images originales
   protected double blankOrig= Double.NaN;   // Valeur du BLANK en entr�e
   protected double bZeroOrig=0;             // Valeur BZERO d'origine
   protected double bScaleOrig=1;            // Valeur BSCALE d'origine
   protected double[] cutOrig;               // Valeurs cutmin,cutmax, datamin,datamax des images originales
   protected int[] borderSize = {0,0,0,0};   // Bords � couper sur les images originales
//   protected boolean skySub = false;         // true s'il faut appliquer une soustraction du fond (via le cacheFits)
   protected String skyvalName;                // Nom du champ � utiliser dans le header pour soustraire un valeur de fond (via le cacheFits)
   
   protected int bitpix = -1;                // BITPIX de sortie
   protected double blank = Double.NaN;      // Valeur du BLANK en sortie
   protected double bZero=0;                 // Valeur BZERO de la boule Healpix � g�n�rer
   protected double bScale=1;                // Valeur BSCALE de la boule HEALPix � g�n�rer
   protected boolean bscaleBzeroSet=false;   // true si le bScale/bZero de sortie a �t� positionn�s
   protected double[] cut;                   // Valeurs cutmin,cutmax, datamin,datamax pour la boule Healpix � g�n�rer
   
   protected int order = -1;                 // Ordre maximale de la boule HEALPix � g�n�rer              
   protected int frame = Localisation.ICRS;  // Syst�me de coordonn�e de la boule HEALPIX � g�n�r�e
   protected HpixTree moc = null;            // Zone du ciel � traiter (d�crite par un MOC)
   protected CacheFits cacheFits;            // Cache FITS pour optimiser les acc�s disques � la lecture
   protected boolean isRunning=false;        // true s'il y a un processus de calcul en cours
//   protected boolean isColor=false;          // true si les images d'entr�e sont des jpeg couleur 
   
   protected CoAddMode coAdd;                      // NORMALEMENT INUTILE DESORMAIS (m�thode de traitement)
   
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
   public HpixTree getMoc() { return moc; }
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
   public void setInputPath(String path) { this.inputPath = path; }
   public void setOutputPath(String path) { this.outputPath = path; }
   public void sethpxFinderPath(String path) { hpxFinderPath = path; }
   public void setImgEtalon(String filename) throws Exception { imgEtalon = filename; initFromImgEtalon();}
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
   public void setBlankOrig(double x) {  blankOrig = x; }
   public void setColor(boolean color) { if(color) this.bitpixOrig=0;}
   public void setIsRunning(boolean flag) { isRunning=flag; }
   public void setAbort() { setIsRunning(false); }
   public void setCut(double [] cut) { this.cut=cut; }
   public void setPixelCut(String cut) {
       String vals[] = cut.split(" ");
       if (this.cut==null) {
    	   this.cut = new double[4];
       }
	   if (vals.length==2) {
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
   

   /**
    * Lit l'image etalon, et affecte les donn�es d'origines (bitpix, bscale, bzero, blank, cut)
    * @throws Exception s'il y a une erreur � la lecture du fichier
    */
   protected void initFromImgEtalon() throws Exception {
	   String path = imgEtalon;
	   Fits fitsfile = new Fits();

	   fitsfile.loadHeaderFITS(path);
       setBitpixOrig(fitsfile.bitpix);
       if( !isColor() ) {
          setBZeroOrig(fitsfile.bzero);
          setBScaleOrig(fitsfile.bscale);
          if( !Double.isNaN(fitsfile.blank) ) setBlankOrig(fitsfile.blank);
       }
       initCut(fitsfile);
   }
   
   /**
    * Lit l'image et calcul son autocut : affecte les datacut et pixelcut *Origines*
    * @param file
    */
   protected void initCut(Fits file) {
       int w = file.width;
       int h = file.height;
       if (w > 1024) w = 1024;
       if (h > 1024) h = 1024;
       try {
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
       } catch (Exception e) {
           e.printStackTrace();
       }
   }

   /**
    * S�lectionne un fichier de type FITS (ou �quivalent) dans le r�pertoire donn� => va servir d'�talon
    * Utilise un cache une case pour �viter les recherches redondantes
    * @return true si trouv�
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
         // s'il n'y a pas eu d'erreur �a peut servir d'�talon
         try {
            Aladin.trace(4, "Context.findImgEtalon: loading header "+path+"...");
            setImgEtalon(path);
            return true;
            
         }  catch (Exception e) { continue; }
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

   protected HpixTree setMoc(String s) {
      if( s.length()==0 ) return null;
      HpixTree hpixTree = new HpixTree(s);
      if( hpixTree.getSize()==0 ) return null;
      moc = hpixTree;
      return hpixTree;
   }

   public void setMoc(HpixTree region) {
      moc = region;
   }
   

   private boolean skysubDone = false;
   
   /** Initialisation des param�tres (ne sert que pour contextGui) */
   public void initParameters() {
      
      bitpix = getBitpix();
      cut = getCut();
      
      bitpixOrig = getBitpixOrig();
      cutOrig = getCutOrig();
      blankOrig = getBlankOrig();
      bZeroOrig = getBZeroOrig();
      bScaleOrig = getBScaleOrig();
     
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

         blank = getDefaultBlankFromBitpix(bitpix);
         bZero = bZeroOrig + bScaleOrig*(cutOrig[2] - cut[2]/coef);
         bScale = bScaleOrig/coef;
         
         Aladin.trace(3,"Change BITPIX from "+bitpixOrig+" to "+bitpix);
         Aladin.trace(3,"Map original pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] " +
                        "to ["+cut[2]+" .. "+cut[3]+"]");
         Aladin.trace(3,"Change BZERO,BSCALE,BLANK="+bZeroOrig+","+bScaleOrig+","+blankOrig
               +" to "+bZero+","+bScale+","+blank);
      
         // Pas de changement de bitpix
      } else {
         blank=blankOrig;
         if( Double.isNaN(blank) && bitpix>0 ) blank = getDefaultBlankFromBitpix(bitpix);
         bZero=bZeroOrig;
         bScale=bScaleOrig;
         Aladin.trace(3,"BITPIX kept "+bitpix+" BZERO,BSCALE,BLANK="+bZero+","+bScale+","+blank);
      }

      // si besoin red�finit le blank 
      if (isSkySub() && bitpix>0) {
    	  blank=getDefaultBlankFromBitpix(bitpix);
      }
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
      
      // Cas particulier des Survey pr�existants sans BLANK en mode entier. Dans ce cas, on accepte
      // tout de m�me de traiter en sachant que le blank d�fini par l'utilisateur sera
      // consid�r� comme celui du survey existant. Mais il faut n�cessairement que l'utilisateur
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

   /** Interpr�tation de la chaine d�crivant les bords � ignorer dans les images sources,
    * soit une seule valeur appliqu�e � tous les bords,
    * soit 4 valeurs affect�es � la java de la mani�re suivante : Nord, Ouest, Sud, Est 
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
   protected void setProgress(int value) { System.out.print("*");}
   protected void setProgress(int mode, int value) { System.out.println(value+"%");}
   protected void preview (int n3) { }

   private long statTime = System.currentTimeMillis();
   // Demande d'affichage des stats (dans le TabBuild)
   protected void showIndexStat(int statNbFile, int statNbZipFile, long statMemFile, long statMaxSize, 
         int statMaxWidth, int statMaxHeight, int statMaxNbyte) {
	   // affiche sur la sortie standard toutes les 3 sec
	   if ((System.currentTimeMillis()-statTime)>3000) {
		   String s;
		   if( statNbFile==-1 ) s = "--";
		   else {
			   s= statNbFile+" file"+(statNbFile>1?"s":"")
			   + (statNbZipFile==statNbFile ? " (all gzipped)" : statNbZipFile>0 ? " ("+statNbZipFile+" gzipped)":"")
			   + " using "+Util.getUnitDisk(statMemFile)
			   + (statNbFile>1 && statMaxSize<0 ? "" : " => biggest: ["+statMaxWidth+"x"+statMaxHeight+"x"+statMaxNbyte+"]");
		   }
		   System.out.println(s);
		   statTime = System.currentTimeMillis();
	   }
   }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showBuildStat(int statNbThreadRunning, int statNbThread, long totalTime, 
         int statNbTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
         long statNodeAvgTime) {
	// affiche sur la sortie standard toutes les 30 sec
	   if (verbose && (System.currentTimeMillis()-statTime)>30000) {
		      long maxMem = Runtime.getRuntime().maxMemory();
		      long totalMem = Runtime.getRuntime().totalMemory();
		      long freeMem = Runtime.getRuntime().freeMemory();
		      long usedMem = totalMem-freeMem;

		      String s= "thread: "+(statNbThreadRunning==-1?"":statNbThreadRunning+" / "+statNbThread)
		      + " - cache: "+Util.getUnitDisk(cacheFits.getStatMem())
		           +" (ram:"+cacheFits.getStatNbFind()
		           +" disk:"+cacheFits.getStatNbOpen()+" free:"+cacheFits.getStatNbFree()+")"
		      + " - mem: "+Util.getUnitDisk(usedMem)+"/"+Util.getUnitDisk(maxMem);
		      System.out.println(s);
			   statTime = System.currentTimeMillis();
	   }
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
	   if (trace>=i)
	   System.out.println(string);
   }
   public void setTrace(int trace) {
	   this.trace = trace;
   }

   /**
    * @param verbose the verbose level to set
    */
   public static void setVerbose(boolean verbose) {
	   Context.verbose = verbose;
   }

   /**
    * Niveau de verbosit� : 
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

public void warning(String string) {
       String s_WARN    = "WARNING :";//Aladin.getChaine().getString("WARNING");
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

   /** M�thode r�cursive utilis�e par createHealpixOrder */
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
    * Retourne l'indice XY en fonction d'un indice Healpix => n�cessit�
    * d'initialiser au pr�alable avec createHealpixOrdre(int)
    */
   final public int xy2hpx(int hpxOffset) {
      return xy2hpx[hpxOffset];
   }

   /**
    * Retourne l'indice XY en fonction d'un indice Healpix => n�cessit�
    * d'initialiser au pr�alable avec createHealpixOrdre(int)
    */
   final public int hpx2xy(int xyOffset) {
      return hpx2xy[xyOffset];
   }



}
