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
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.HpixTree;

/**
 * Classe pour unifier les accès aux paramètres nécessaires pour les calculs
 * + accès aux méthodes d'affichages qui sont redirigées selon l'interface
 * existante
 * @author oberto
 *
 */
public class Context {

	MainPanel mainPanel = null;

	private int[] borderSize = {0,0,0,0};
	private int order = -1;
	private int bitpix = -1;
	private int bitpixOrig = -1;
	private boolean keepBB = false;
	private boolean fading = false;
	private int frame = Localisation.ICRS;
	private boolean skySub = false;// condition d'application d'une soustraction du skyval au moment 
	//de la mise dans le cache

	protected CacheFits cacheFits;
	HpixTree hpixTree = null;
	
	private String input;
	private String output;

	private int coAdd;

	private double[] bb;

	private double blank;


	public Context() {}
	public Context(MainPanel mainPanel) {
		this.mainPanel=mainPanel;
	}
	
	/**
	 * @param borderSize the borderSize to set
	 */
	public void setBorderSize(int[] borderSize) {
		this.borderSize = borderSize;
	}
	public void setBorderSize(String s) throws ParseException {
		this.borderSize = parseBorderSize(s);
	}
	public int[] getBorderSize() {
		if (mainPanel!=null)
			try {
				setBorderSize(mainPanel.getBorderSize());
			} catch (ParseException e) {
				mainPanel.tabDesc.borderTextField.setText("Border error => assume 0");
				e.printStackTrace();
			}
		return borderSize;
	}


	/** Interprétation de la chaine décrivant les bords à ignorer dans les images sources,
	 * soit une seule valeur appliquée à tous les bords,
	 * soit 4 valeurs affectées à la java de la manière suivante : Nord, Ouest, Sud, Est 
	 * @throws ParseException */
	public int [] parseBorderSize(String s) throws ParseException {
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

	public void setOrder(int order) {
		this.order = order;
	}
	
	/**
	 * 
	 * @return order choisi ou -1 s'il doit etre calculé
	 */
	public int getOrder() {
		if (mainPanel != null) {
			if (mainPanel.tabBuild.getOrder() != -1) {
				order = mainPanel.tabBuild.getOrder();
			}
			else if (mainPanel.planPreview != null)
				order = mainPanel.planPreview.getMaxHealpixOrder();
			else return -1;
		}
		return order;
	}

	public void toFading(boolean fading) {
		this.fading = fading;
	}
	public boolean toFading() {
		return fading;
	}


	public boolean skySub() {
		return skySub;
	}

	public void skySub(boolean substract) {
		skySub = substract;
		if (cacheFits != null) cacheFits.skySub(substract);
	}

	public void setFrame(int frame) { this.frame=frame; }
	public int getFrame() { 
		return frame; 
		}
	public String getFrameName() { return Localisation.getFrameName(frame); }

	/**
	 * Donne le lien vers le cache des images FITS
	 * + lui définit si le skyval doit etre enlevé s'il a déjà été donné
	 * @param cache
	 */
	public void setCache(CacheFits cache) {
		this.cacheFits = cache;
		if (skySub) cache.skySub(true);
	}
	
	public CacheFits getCache() {
		return cacheFits;
	}

	// Demande d'affichage des stats (dans le TabBuild)
	protected void showIndexStat(int statNbFile, int statNbZipFile, long statMemFile, long statMaxSize, 
			int statMaxWidth, int statMaxHeight, int statMaxNbyte) {
		if( mainPanel==null ) return;
		mainPanel.tabBuild.buildProgressPanel.setSrcStat(statNbFile, statNbZipFile, statMemFile,statMaxSize,statMaxWidth,statMaxHeight,statMaxNbyte);
	}


	// Demande d'affichage des stats (dans le TabBuild)
	protected void showBuildStat(int statNbThreadRunning, int statNbThread, long totalTime, 
			int statNbTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
			long statNodeAvgTime) {
		if( mainPanel==null ) return;
		mainPanel.tabBuild.buildProgressPanel.setMemStat(statNbThreadRunning,statNbThread);
		mainPanel.tabBuild.buildProgressPanel.setTimeStat(totalTime);
		mainPanel.tabBuild.buildProgressPanel.setLowTileStat(statNbTile,
				(long)( BuilderController.SIDE*BuilderController.SIDE*Math.abs(bitpix)/8),
				statMinTime,statMaxTime,statAvgTime);
		mainPanel.tabBuild.buildProgressPanel.setNodeTileStat(statNodeTile,
				(long)( BuilderController.SIDE*BuilderController.SIDE*Math.abs(bitpix)/8),
				statNodeAvgTime);
	}
	

    // Demande d'affichage des stats (dans le TabJpeg)
    protected void showJpgStat(int statNbFile, long statSize, long totalTime) {
       if( mainPanel==null ) return;
       mainPanel.tabJpg.setStat(statNbFile, statSize, totalTime);
    }

    // Demande d'affichage des stats (dans le TabRgb)
    protected void showRgbStat(int statNbFile, long statSize, long totalTime) {
       if( mainPanel==null ) return;
       mainPanel.tabRgb.setStat(statNbFile, statSize, totalTime);
    }
    
    protected void stop() {
    	if( mainPanel==null ) return;
    	mainPanel.stop();
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

	protected String getInputPath() {
		if( mainPanel!=null ) {
			input = mainPanel.getInputPath();
		}
		 return input;
	}

	public void setInput(String input) {
		this.input = input;
	}
	public String getOutputPath() {
		if( mainPanel!=null ) {
			output = mainPanel.getOutputPath();
		}
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	protected void setInitDir(String txt) {
		if( mainPanel!=null ) 
			mainPanel.setInitDir(txt);
	}

	protected boolean isExistingDir() {
//			return mainPanel.tabDesc!=null && mainPanel.tabDesc.getInputPath() != null && 
//				(new File(mainPanel.tabDesc.getInputPath())).exists();
		return   (new File(getInputPath())).exists();
	}

	protected boolean isExistingAllskyDir() {
//		return tabDesc!=null && tabDesc.getOutputPath() != null && (new File(tabDesc.getOutputPath())).exists();
		return (new File(getOutputPath())).exists();
	}

	public int getCoAddMode() {
		if( mainPanel!=null ) {
			coAdd = mainPanel.getCoAddMode();
		}
		return coAdd;
	}
	public void setCoAdd(int coAdd) {
		this.coAdd = coAdd;
	}
	

	/**
	 * @return the keepBB
	 */
	protected boolean isKeepBB() {
		if( mainPanel!=null ) keepBB=mainPanel.tabBuild.isKeepBB();
		return keepBB;
	}

	protected int getOriginalBitpix() {
		if( mainPanel!=null ) bitpixOrig=mainPanel.tabBuild.getOriginalBitpix();
		return bitpixOrig;
	}
	
	protected double[] getBScaleBZero() {
		if (mainPanel!=null) bb = mainPanel.getBScaleBZero();
		return bb;
	}

	public void setBb(double[] bb) {
		this.bb = bb;
	}
	
	protected int getBitpix() {
		if (mainPanel!=null)
			bitpix = mainPanel.getBitpix();
		return this.bitpix;
	}

	public void setBitpix(int bitpix) {
		this.bitpix = bitpix;
	}

	protected double getBlank() {
		if (mainPanel!=null) {
			blank = mainPanel.getBlank();
		}
		return blank;
	}

	public void setBlank(double blank) {
		this.blank = blank;
	}
	
	/** Retourne la liste des losanges HEALPix spécifiquement à traiter, null si tout le ciel */
	public HpixTree getHpixTree() {
		if (mainPanel!=null) {
			hpixTree=mainPanel.getHpixTree();
		}
		return hpixTree;
	}
/*
	protected void setLastN3(int lastN3) {
		this.lastN3 = lastN3;
	}
*/
	private boolean isRunning=false;

	private String label;

	private double[] cut;
	protected boolean isRunning() { return isRunning; }
	protected void setIsRunning(boolean flag) { 
		isRunning=flag;
		if (mainPanel!=null)
			mainPanel.setIsRunning(flag);
	}


	protected void enableProgress(boolean selected, int mode) {
		if (mainPanel!=null) {
			mainPanel.enableProgress(selected, mode);
		}
	}
	protected void setProgress(int mode, int value) {
		if (mainPanel!=null) {
			mainPanel.setProgress(mode, value);
		}
	}
	protected void preview (int n3) {
		if (mainPanel!=null) mainPanel.preview(n3);
	}
	
	public String getLabel() {
		if (mainPanel!=null) {
			label = mainPanel.getLabel();
		}
		return label;
	}

	protected double[] getCut() {
		if (mainPanel!=null)
			return mainPanel.tabJpg.getCut();
		return cut;
	}

	protected void setCut(double [] cut) {
		if (mainPanel!=null)
			mainPanel.tabJpg.setCut(cut);
		else
			this.cut=cut;
	}

	protected void convertCut(int bitpix) {
	      double[] cut = getCut();
	      double [] oldminmax = new double[] {cut[2],cut[3]};
	      cut[0] = Fits.toBitpixRange(cut[0], bitpix, oldminmax);
	      cut[1] = Fits.toBitpixRange(cut[1], bitpix, oldminmax);
	      setCut(cut);
	}
	
	public void warning(String string) {
		if (mainPanel!=null)
			Aladin.warning(mainPanel, string);
		else
			Aladin.warning(string);
	}

}
