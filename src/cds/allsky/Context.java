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

   protected String label;                   // Nom du survey
   
   protected String inputPath;               // R�pertoire des images origales
   protected String outputPath;              // R�pertoire de la boule HEALPix � g�n�rer
   protected String hpxFinderPath;           // R�pertoire de l'index Healpix (null si d�faut => dans outputPath/HpxFinder)
   protected String imgEtalon;               // Nom (complet) de l'image qui va servir d'�talon
   
   protected String regex = "*.fits";        // Expression r�guli�re � appliquer pour s�lectionner les images � traiter
   
   protected int bitpixOrig = -1;            // BITPIX des images originales
   protected double blankOrig;               // Valeur du BLANK en entr�e
   protected double bZeroOrig=0;             // Valeur BZERO d'origine
   protected double bScaleOrig=1;            // Valeur BSCALE d'origine
   protected double[] cutOrig;               // Valeurs cutmin,cutmax, datamin,datamax des images originales
   protected int[] borderSize = {0,0,0,0};   // Bords � couper sur les images originales
   protected boolean skySub = false;         // true s'il faut appliquer une soustraction du fond (via le cacheFits)
   
   protected int bitpix = -1;                // BITPIX de sortie
   protected double blank;                   // Valeur du BLANK en sortie
   protected double bZero=0;                 // Valeur BZERO de la boule Healpix � g�n�rer
   protected double bScale=1;                // Valeur BSCALE de la boule HEALPix � g�n�rer
   protected double[] cut;                   // Valeurs cutmin,cutmax, datamin,datamax pour la boule Healpix � g�n�rer
   
   protected boolean fading = false;         // true pour appliquer un "fondu-enchain�" sur les recouvrements
   protected int order = -1;                 // Ordre maximale de la boule HEALPix � g�n�rer              
   protected int frame = Localisation.ICRS;  // Syst�me de coordonn�e de la boule HEALPIX � g�n�r�e
   protected HpixTree moc = null;            // Zone du ciel � traiter (d�crite par un MOC)
   protected CacheFits cacheFits;            // Cache FITS pour optimiser les acc�s disques � la lecture
   protected boolean isRunning=false;        // true s'il y a un processus de calcul en cours
//   protected boolean isColor=false;          // true si les images d'entr�e sont des jpeg couleur 
   
   protected CoAddMode coAdd;                      // NORMALEMENT INUTILE DESORMAIS (m�thode de traitement)
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
   public boolean isSkySub() { return skySub; }
   public boolean isRunning() { return isRunning; }
   public boolean isColor() { return bitpixOrig==0; }
   
   // Setters
   public void setBorderSize(String borderSize) throws ParseException { this.borderSize = parseBorderSize(borderSize); }
   public void setBorderSize(int[] borderSize) { this.borderSize = borderSize; }
   public void setOrder(int order) { this.order = order; }
   public void setFading(boolean fading) { this.fading = fading; }
   public void setFrame(int frame) { this.frame=frame; }
   public void setRegex(String regex) { this.regex = regex; }
   public void setInputPath(String path) { this.inputPath = path; }
   public void setOutputPath(String path) { this.outputPath = path; }
   public void sethpxFinderPath(String path) { hpxFinderPath = path; }
   public void setImgEtalon(String filename) { imgEtalon = filename; }
   public void setInitDir(String txt) { }
   public void setCoAddMode(CoAddMode coAdd) { this.coAdd = coAdd; }
   public void setBScaleOrig(double x) { bScale = bScaleOrig = x; }
   public void setBZeroOrig(double x) { bZero = bZeroOrig = x; }
   public void setBitpixOrig(int bitpix) { bitpixOrig = this.bitpix = bitpix; }
   public void setBitpix(int bitpix) { this.bitpix = bitpix; }
   public void setBlankOrig(double blankOrig) { this.blank = this.blankOrig = blankOrig; }
   public void setBlank(double blank) { this.blank = blank;}
   public void setColor(boolean color) { if(color) this.bitpixOrig=0;}
   public void setIsRunning(boolean flag) { isRunning=flag; }
   public void setCut(double [] cut) { this.cut=cut; }
   public void setCut(String cut) {
	   String vals[] = cut.split(" ");
	   if (vals.length==2)
		   this.cut = new double[] {Double.parseDouble(vals[0]),Double.parseDouble(vals[1]),0,0};
	   else if (vals.length==4)
		   this.cut = new double[] {Double.parseDouble(vals[0]),Double.parseDouble(vals[1]),
			   Double.parseDouble(vals[2]),Double.parseDouble(vals[3])};
   }
   
   public void setCutData(String cut) {
	   String vals[] = cut.split(" ");
	   if (vals.length==2 && this.cut.length != 0)
		   this.cut = new double[] {this.cut[0],this.cut[1],Double.parseDouble(vals[2]),Double.parseDouble(vals[3])};
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
	   if (w > 1024)
		   w = 1024;
	   if (h > 1024)
		   h = 1024;
	   try {
		   file.loadFITS(file.getFilename(), 0, 0, w, h);
		   double[] cut = file.findAutocutRange();
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
         // s'il n'y a pas eu d'erreur �a peut servir d'�talon
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
      bZero = bZeroOrig + bScaleOrig*(cutOrig[2] - cut[2]/coef);
      bScale = bScaleOrig/coef;
   }
   
   public void setSkySub(boolean skySub) {
      this.skySub = skySub;
      if (cacheFits != null) cacheFits.setSkySub(skySub);
   }
   public void setCache(CacheFits cache) {
      this.cacheFits = cache;
      if (skySub) cache.setSkySub(true);
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
      Aladin.warning(string);
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


}
