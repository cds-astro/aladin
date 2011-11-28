package cds.allsky;

import java.text.ParseException;

import cds.aladin.Aladin;
import cds.aladin.Plan;
import cds.aladin.PlanImage;
import cds.tools.Util;
import cds.tools.pixtools.HpixTree;

/**
 * Contexte pour la création d'un Allsky, via l'interface graphique
 * @author Oberto + Fernique
 */
public class ContextGui extends Context {

   private MainPanel mainPanel;       // Référence à l'interface graphique

   /** Positionnement de l'interface graphique associée au traitement */
   public void setMainPanel(MainPanel mainPanel) { this.mainPanel=mainPanel; }
//   public CoAddMode getCoAddMode() { coAdd = mainPanel.tabDesc.getCoaddMode(); return coAdd;}
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
            (long)( Constante.SIDE*Constante.SIDE*Math.abs(bitpix)/8),
            statMinTime,statMaxTime,statAvgTime);
      mainPanel.tabBuild.buildProgressPanel.setNodeTileStat(statNodeTile,
            (long)( Constante.SIDE*Constante.SIDE*Math.abs(bitpix)/8),
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

   public void setOutputPath(String output) {
      this.outputPath = output;
   }

   public void setInitDir(String txt) {
      mainPanel.setProgressIndexDir(txt);
   }

   public int getBitpixOrig() {
      return mainPanel.tabBuild.getOriginalBitpix();
   }

   public int getBitpix() {
      return mainPanel.getBitpix();
   }

   public double getBlankOrig() {
      double b = Double.NaN;
      String s="";
      try { 
         s = mainPanel.tabDesc.getBlank().trim();
         if( s.length()>0 ) b = Double.parseDouble(s);
      } catch( Exception e ) {
         mainPanel.tabDesc.blankTextField.setText("Unknown value => ["+s+"]");
      }
      return b;
   }
   
   /** Initialisation des paramètres (ne sert que pour contextGui) */
   public void initParameters() {
      setMoc( mainPanel.getMoc() );
      setCoAddMode( mainPanel.tabDesc.getCoaddMode() );
      setSkyValName( mainPanel.tabDesc.getSkyval() );
      super.initParameters();
   }
   public String getSkyval() {
	   skyvalName = mainPanel.tabDesc.getSkyval().toUpperCase();
	   return skyvalName;
   }

   public void setIsRunning(boolean flag) { 
      super.setIsRunning(flag);
      mainPanel.setIsRunning(flag);
   }
   
   public void setAbort() {
      super.setIsRunning(false);
      mainPanel.setAbort();
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
      if( cut==null ) cut = new double[4];
      try {
         if( mainPanel.tabJpg.isCutFromPlanBase() ) {
            PlanImage p = (PlanImage)mainPanel.aladin.calque.getPlanBase();
            cut[0]= p.getCutMin();
            cut[1]= p.getCutMax();
            cut[2]= p.getDataMin();
            cut[3]= p.getDataMax();
            for( int i=0; i<4; i++ ) cut[i] = (((cut[i]*p.bScale)+p.bZero)-bZero)/bScale;

         } else {
            String cutMin = mainPanel.tabJpg.getCutMin();
            String cutMax = mainPanel.tabJpg.getCutMax();
            cut[0] = (Double.parseDouble(cutMin)-bZero)/bScale;
            cut[1] = (Double.parseDouble(cutMax)-bZero)/bScale;
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         cut[0] = cut[2] = 0;
         cut[1] = cut[3] = 1;
      }

      return cut;
   }
   
   public double[] getCutOrig() {
      if( cutOrig==null ) cutOrig = new double[4];
      try {
         if( mainPanel.tabJpg.isCutFromPlanBase() ) {
            Plan p = mainPanel.aladin.calque.getPlanBase();
            cutOrig[0]= ((PlanImage)p).getCutMin();
            cutOrig[1]= ((PlanImage)p).getCutMax();
            cutOrig[2]= ((PlanImage)p).getDataMin();
            cutOrig[3]= ((PlanImage)p).getDataMax();

         } else {
            String cutMin = mainPanel.tabJpg.getCutMin();
            String cutMax = mainPanel.tabJpg.getCutMax();
            cutOrig[0] = (Double.parseDouble(cutMin)-bZeroOrig)/bScaleOrig;
            cutOrig[1] = (Double.parseDouble(cutMax)-bZeroOrig)/bScaleOrig;
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         cutOrig[0] = cutOrig[2] = 0;
         cutOrig[1] = cutOrig[3] = 1;
      }

      return cutOrig;
   }

   public void setCutOrig(double [] c) {
      super.setCutOrig(c);
      mainPanel.tabJpg.setCutMin( Util.myRound(c[0]*bScaleOrig+bZeroOrig) );
      mainPanel.tabJpg.setCutMax( Util.myRound(c[1]*bScaleOrig+bZeroOrig) );
   }

   public void warning(String string) {
      Aladin.warning(mainPanel, string);
   }

   public void trace(int i, String string) {
	   Aladin.trace(i, string);
   }
}
