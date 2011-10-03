package cds.allsky;

import java.text.ParseException;

import cds.aladin.Aladin;
import cds.tools.pixtools.HpixTree;

/**
 * Contexte pour la création d'un Allsky, via l'interface graphique
 * @author Oberto + Fernique
 */
public class ContextGui extends Context {

	private MainPanel mainPanel;       // Référence à l'interface graphique
	
	/** Positionnement de l'interface graphique associée au traitement */
	public void setMainPanel(MainPanel mainPanel) { this.mainPanel=mainPanel; }
	
	public int[] getBorderSize() {
	   try {
	      setBorderSize(mainPanel.getBorderSize());
	   } catch (ParseException e) {
	      mainPanel.tabDesc.borderTextField.setText("Border error => assume 0");
	      e.printStackTrace();
	   }
	   return borderSize;
	}

	public int getOrder() {
	   if (mainPanel.tabBuild.getOrder() != -1)  return mainPanel.tabBuild.getOrder();
	   if (mainPanel.planPreview != null) return mainPanel.planPreview.getMaxHealpixOrder();
	   return -1;
	}

	// Demande d'affichage des stats (dans le TabBuild)
	protected void showIndexStat(int statNbFile, int statNbZipFile, long statMemFile, long statMaxSize, 
			int statMaxWidth, int statMaxHeight, int statMaxNbyte) {
		mainPanel.tabBuild.buildProgressPanel.setSrcStat(statNbFile, statNbZipFile, statMemFile,statMaxSize,statMaxWidth,statMaxHeight,statMaxNbyte);
	}

	// Demande d'affichage des stats (dans le TabBuild)
	protected void showBuildStat(int statNbThreadRunning, int statNbThread, long totalTime, 
			int statNbTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
			long statNodeAvgTime) {
		mainPanel.tabBuild.buildProgressPanel.setMemStat(statNbThreadRunning,statNbThread,cacheFits);
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
       mainPanel.tabJpg.setStat(statNbFile, statSize, totalTime);
    }

    // Demande d'affichage des stats (dans le TabRgb)
    protected void showRgbStat(int statNbFile, long statSize, long totalTime) {
       mainPanel.tabRgb.setStat(statNbFile, statSize, totalTime);
    }
    
    protected void stop() {
    	mainPanel.stop();
    }

    public String getInputPath() {
		return mainPanel.getInputPath();
	}

	public String getOutputPath() {
		return mainPanel.getOutputPath();
	}

	public void setOutput(String output) {
		this.outputPath = output;
	}

	public void setInitDir(String txt) {
		mainPanel.setProgressIndexDir(txt);
	}


	public int getCoAdd() {
		return mainPanel.getCoAddMode();
	}

	public boolean isKeepBB() {
		return mainPanel.tabBuild.isKeepBB();
	}

	public int getBitpixOrig() {
		return mainPanel.tabBuild.getOriginalBitpix();
	}
	
	public double[] getBScaleBZero() {
		return mainPanel.getBScaleBZero();
	}

	public int getBitpix() {
		return mainPanel.getBitpix();
	}

	public double getBlank() {
		return mainPanel.getBlank();
	}

	public HpixTree getMoc() {
		return mainPanel.getHpixTree();
	}
	
	public void setIsRunning(boolean flag) { 
	   super.setIsRunning(flag);
	   mainPanel.setIsRunning(flag);
	}

	protected void enableProgress(boolean selected, int mode) {
		mainPanel.enableProgress(selected, mode);
	}
	
	protected void setProgress(int mode, int value) {
		mainPanel.setProgress(mode, value);
	}
	
	protected void preview (int n3) {
		mainPanel.preview(n3);
	}
	
	public String getLabel() {
		return mainPanel.getLabel();
	}

	public double[] getCut() {
		return mainPanel.tabJpg.getCut();
	}

	public void setCut(double [] cut) {
	   super.setCut(cut);
	   mainPanel.tabJpg.setCut(cut);
	}

	public void warning(String string) {
		Aladin.warning(mainPanel, string);
	}

}
