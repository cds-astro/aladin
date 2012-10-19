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

import java.text.ParseException;

import javax.swing.JProgressBar;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.aladin.PlanImage;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Context pour la création d'un Allsky, via l'interface graphique
 * @author Oberto + Fernique
 */
public class ContextGui extends Context {

   protected MainPanel mainPanel;       // Référence à l'interface graphique

   /** Positionnement de l'interface graphique associée au traitement */
   public void setMainPanel(MainPanel mainPanel) { this.mainPanel=mainPanel; }
   
//   public CoAddMode getCoAddMode() { coAdd = mainPanel.tabDesc.getCoaddMode(); return coAdd;}
   public int[] getBorderSize() {
      try {
         setBorderSize(mainPanel.tabDesc.getBorderSize().trim());
      } catch (ParseException e) {
         mainPanel.tabDesc.borderTextField.setText("Border error => assume 0");
         e.printStackTrace();
      }
      return borderSize;
   }
   
   public void setOrder(int order) {
      mainPanel.tabBuild.setOrder(order);
   }

   public int getOrder() {
      return mainPanel.tabBuild.getOrder();
   }
   
   // Demande d'affichage des stats (dans le TabBuild)
   protected void showIndexStat(int statNbFile, int statNbZipFile, long statMemFile, long statMaxSize, 
         int statMaxWidth, int statMaxHeight, int statMaxNbyte) {
      mainPanel.tabBuild.buildProgressPanel.setSrcStat(statNbFile, statNbZipFile, statMemFile,statMaxSize,statMaxWidth,statMaxHeight,statMaxNbyte);
   }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showTilesStat(int statNbThreadRunning, int statNbThread, long totalTime, 
         int statNbTile, int statNbEmptyTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
         long statNodeAvgTime,long usedMem,long freeMem) {
      mainPanel.tabBuild.buildProgressPanel.setMemStat(statNbThreadRunning,statNbThread,cacheFits);
      mainPanel.tabBuild.buildProgressPanel.setTimeStat(totalTime,statNbTile+statNodeTile,(long)( Constante.SIDE*Constante.SIDE*getNpix()));
      long nbLowCells = getNbLowCells();
      mainPanel.tabBuild.buildProgressPanel.setLowTileStat(statNbTile,statNbEmptyTile,nbLowCells,
            (long)( Constante.SIDE*Constante.SIDE*getNpix()),
            statMinTime,statMaxTime,statAvgTime);
      mainPanel.tabBuild.buildProgressPanel.setNodeTileStat(statNodeTile,
            (long)( Constante.SIDE*Constante.SIDE*getNpix()),
            statNodeAvgTime);
      
      setProgress(statNbTile+statNbEmptyTile, nbLowCells);
   }
   
   // Demande d'affichage des stats (dans le TabJpeg)
   protected void showJpgStat(int statNbFile, long statSize, long totalTime) {
      mainPanel.tabJpg.setStat(statNbFile, statSize, totalTime);
   }

   // Demande d'affichage des stats (dans le TabRgb)
   protected void showRgbStat(int statNbFile, long statSize, long totalTime) {
      mainPanel.tabRgb.setStat(statNbFile, statSize, totalTime);
   }
   
   private int lastShowAllSkyNorder3=-1;
   public void updateAllskyPreview() {
      try {
         if( !isExistingAllskyDir() ) return;
         if( lastShowAllSkyNorder3==lastNorder3 ) return;  // Déjà calculé
         lastShowAllSkyNorder3=lastNorder3;
         (new BuilderAllsky(this)).run();

         String mysky = getLabel();
         if( mysky.trim().length()==0 ) mysky="MySky";
         int npix = lastShowAllSkyNorder3>=0 ? lastShowAllSkyNorder3 : 0;
         mainPanel.planPreview = (PlanBG) mainPanel.aladin.calque.getPlan(mysky);
         if (mainPanel.planPreview == null || mainPanel.planPreview.isFree() || mainPanel.planPreview.hasError() ) {
            double[] res = CDSHealpix.pix2ang_nest(cds.tools.pixtools.Util.nside(3), npix);
            double[] radec = CDSHealpix.polarToRadec(new double[] { res[0], res[1] });
            radec = gal2ICRSIfRequired(radec);
            int n = mainPanel.aladin.calque.newPlanBG(getOutputPath(), "="+mysky,
                  Coord.getSexa(radec[0], radec[1]), "30");
            Aladin.trace(4, "ContextGui.preview(): create "+mysky);
            mainPanel.planPreview = (PlanBG) mainPanel.aladin.calque.getPlan(n);
            //            mainPanel.setStartEnabled(true);
         } else {
            mainPanel.planPreview.forceReload();
            mainPanel.aladin.calque.repaintAll();
            Aladin.trace(4, "ContextGui.preview(): update "+mysky);

         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public void progressStatus() {
      if( progressBar==null ) { super.progressStatus(); return; }
      if( progressMax<=0 ) {
         progressBar.setIndeterminate(true);
         progressBar.setValue((int)progress);
      } else {
         progressBar.setIndeterminate(false);
         progressBar.setMaximum((int)progressMax);
         progressBar.setValue((int)progress);
      }
      if( (action==Action.TILES || action==Action.RGB) && lastNorder3!=-1 ) updateAllskyPreview();
   }
   
   public void endAction() throws Exception { 
      if( progressBar!=null ) {
         progressBar.setIndeterminate(false);
         progressBar.setMaximum(100);
         progressBar.setValue(100);

         if( action==null ) progressBar.setString("Already done !");
         else if( taskAborting ) progressBar.setString("Aborted !");
         else progressBar.setString("Done !");
      }
      if( (action==Action.TILES || action==Action.RGB) && lastNorder3!=-1 ) updateAllskyPreview();
      super.endAction();
   }
   
   public void setTaskPause(boolean flag) {
      super.setTaskPause(flag);
      if( progressBar!=null ) {
         if( flag ) {
            progressBar.setIndeterminate(true);
            progressBar.setString("pause");
         } else {
            progressBar.setIndeterminate(false);
            progressBar.setString(null);
         }
      }
   }
   
   public void enableProgress(boolean flag) {
      if( progressBar==null ) super.enableProgress(flag);
      else progressBar.setEnabled(flag);
   }
   
   public void resumeWidgets() { mainPanel.resumeWidgets(); }

   public void setProgressBar(JProgressBar bar) { progressBar=bar; progressBar.setString(null); }
   
   public void setRgbPlans(Object [] plans) { plansRgb=plans; }
   public void setRgbOutput(String output) { outputRgb=output; }
   public void setRgbMethod(JpegMethod method) { methodRgb=method; }
   
   public Object [] getRgbPlans() { return plansRgb; }
   public String getRgbOutput() { return outputRgb; }
   public JpegMethod getRgbMethod() { return methodRgb; }

   public String getInputPath() {
      return mainPanel.tabDesc.getInputField();
   }

   public String getOutputPath() {
      return mainPanel.tabDesc.getOutputField();
   }

   public void setOutputPath(String output) {
      mainPanel.tabDesc.setOutputField(output);
   }

   public int getBitpixOrig() {
      return mainPanel.tabBuild.getOriginalBitpixField();
   }

   public int getBitpix() {
      return mainPanel.tabBuild.getBitpixField();
   }

   public double getBlankOrig() {
      double b = Double.NaN;
      hasAlternateBlank=false;
      String s="";
      try { 
         s = mainPanel.tabDesc.getBlank().trim();
         if( s.length()>0 ) {
            b = Double.parseDouble(s);
            hasAlternateBlank=true;
         }
      } catch( Exception e ) {
         mainPanel.tabDesc.blankTextField.setText("Unknown value => ["+s+"]");
      }
      return b;
   }
   
   /** Initialisation des paramètres (ne sert que pour contextGui) */
   public void initParameters() throws Exception {
      setMocArea( mainPanel.tabDesc.getMocField().trim() );
      setCoAddMode( mainPanel.tabDesc.getCoaddModeField() );
      setSkyValName( mainPanel.tabDesc.getSkyvalField() );
      super.initParameters();
   }
   
   public String getSkyval() {
	   skyvalName = mainPanel.tabDesc.getSkyvalField().toUpperCase();
	   return skyvalName;
   }
   
   public String getLabel() {
      return mainPanel.tabDesc.getLabelField();
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
         cut[1] = cut[3] = 255;
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
   
   public void running(String string) { trace(3,"RUN   : "+string);  }
   public void nldone(String string)  { trace(3,"DONE  : "+string);  }
   public void done(String string)    { trace(3,"DONE  : "+string); }
   public void info(String string)    { trace(3,"INFO  : "+string); }
   public void warning(String string) { mainPanel.aladin.warning(mainPanel, string); trace(3,"WARN  : "+string); }
   public void error(String string)   { mainPanel.aladin.warning(mainPanel, string); trace(3,"ERROR : "+string);}
   public void action(String string)  { trace(3,"ACTION: "+string); }
   public void nlstat(String string)  { trace(3,"STAT  : "+string); }


   public void trace(int i, String string) {
	   Aladin.trace(i, string);
   }
}
