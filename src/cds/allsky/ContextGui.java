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

import java.io.File;
import java.text.ParseException;

import javax.swing.JProgressBar;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.PlanBG;
import cds.aladin.PlanImage;
import cds.aladin.TreeObjDir;
import cds.fits.CacheFits;
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
   protected void showIndexStat(int statNbFile, int statBlocFile, int statNbZipFile, long statMemFile, long statPixSize, long statMaxSize,
         int statMaxWidth, int statMaxHeight, int statMaxDepth, int statMaxNbyte,long statDuree) {
      mainPanel.tabBuild.buildProgressPanel.setSrcStat(statNbFile, statNbZipFile, statMemFile,statMaxSize,statMaxWidth,statMaxDepth,statMaxHeight,statMaxNbyte);
   }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showTilesStat(int statNbThreadRunning, int statNbThread, long totalTime,
         int statNbTile, int statNbEmptyTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
         long statNodeAvgTime,long usedMem,long deltaTime,long deltaNbTile) {

      if( statNbTile==0 ) return;
      
      int tileSide = getTileSide();
      mainPanel.tabBuild.buildProgressPanel.setMemStat(statNbThreadRunning,statNbThread,cacheFits);
      //      mainPanel.tabBuild.buildProgressPanel.setTimeStat(totalTime,statNbTile+statNodeTile,(long)( Constante.SIDE*Constante.SIDE*getNpix()));
      long nbLowCells = getNbLowCells();
      mainPanel.tabBuild.buildProgressPanel.setLowTileStat(statNbTile,statNbEmptyTile,nbLowCells,
            tileSide*tileSide*getNpix(),
            statMinTime,statMaxTime,statAvgTime);
      mainPanel.tabBuild.buildProgressPanel.setNodeTileStat(statNodeTile,
            tileSide*tileSide*getNpix(),
            statNodeAvgTime);

      long nbCells = getNbLowCells();
      long nbLowTile = statNbTile+statNbEmptyTile;
      long tempsTotalEstime = nbLowTile==0 ? 0 : nbCells==0 ? 0 : nbCells*(totalTime/nbLowTile)-totalTime;

      long nbTilesPerMin = (deltaNbTile*60000L)/deltaTime;

      mainPanel.tabBuild.buildProgressPanel.setTimeStat(totalTime,nbTilesPerMin,tempsTotalEstime);

      setProgress(statNbTile+statNbEmptyTile, nbLowCells);
   }

   protected void showMapStat(long  cRecord,long nbRecord, long cTime, CacheFits cache, String info ) {
      double pourcent = (double)cRecord/nbRecord;
      long totalTime = pourcent==0 ? 0 : (long)( cTime/pourcent);
      long endsIn = totalTime==0 ? 0 : totalTime-cTime;
      String s = Util.round(pourcent*100,1)+"% in " +Util.getTemps(cTime*1000L);
      if( endsIn>0 ) s = s+" ends n="+Util.getTemps(endsIn*1000L);
      mainPanel.tabBuild.buildProgressPanel.setTimeStat(s);
      s = "Records: "+cRecord+ " / "+nbRecord;
      mainPanel.tabBuild.buildProgressPanel.setLowTileStat(s);
      mainPanel.tabBuild.buildProgressPanel.setMemStat(1,1,cache);
      mainPanel.tabBuild.buildProgressPanel.srcFileStat(info);
      setProgress(cRecord,nbRecord);
   }


   // Demande d'affichage des stats (dans le TabJpeg)
   protected void showJpgStat(int statNbFile, long totalTime,int statNbThread,int statNbThreadRunning) {
      long nbLowCells = getNbLowCells();
      long tempsTotalEstime = nbLowCells==0 ? 0 : statNbFile==0 ? 0 : (long)( nbLowCells*(totalTime/statNbFile)-totalTime);

      String s1=statNbFile+" / "+nbLowCells+" tiles";
      String s2=Util.getTemps(totalTime*1000L);
      if( tempsTotalEstime>0 ) s2+=" - ends in "+Util.getTemps(tempsTotalEstime*1000L);

      mainPanel.tabJpg.setStat(s1,s2);
      setProgress(statNbFile, nbLowCells);
   }

   // Demande d'affichage des stats (dans le TabRgb)
   protected void showRGBStat(int statNbFile, long totalTime,int statNbThread,int statNbThreadRunning) {
      long nbLowCells = getNbLowCells();
      long tempsTotalEstime = nbLowCells==0 ? 0 : statNbFile==0 ? 0 : (long)( nbLowCells*(totalTime/statNbFile)-totalTime);

      String s1=statNbFile+" / "+nbLowCells+" tiles";
      String s2=Util.getTemps(totalTime*1000L);
      if( tempsTotalEstime>0 ) s2+=" - ends in "+Util.getTemps(tempsTotalEstime*1000L);

      mainPanel.tabRgb.setStat(s1,s2);
      setProgress(statNbFile, nbLowCells);
   }
//   
//   protected void showRgbStat(int statNbFile, long statSize, long totalTime) {
//      mainPanel.tabRgb.setStat(statNbFile, statSize, totalTime);
//   }

   private int lastShowAllSkyNorder3=-1;
   private PreviewThread previewThread=null;

   public void updateHipsPreview(boolean force) {
      if( !force ) {
         if( previewThread!=null ) return;   // déjà en cours
         if( lastShowAllSkyNorder3==lastNorder3 ) return;  // Déjà calculé
      } else {
         //         System.out.println("Preview force");
      }
      lastShowAllSkyNorder3=lastNorder3;
      if( previewThread!=null ) {
         previewThread.abort();
         previewThread=null;
      }
      previewThread = new PreviewThread(this);
      previewThread.start();
   }

   class PreviewThread extends Thread {
      Context context;
      BuilderAllsky builder=null;
      PreviewThread(Context context) {
         this.context = context;
      }
      protected void abort() { builder.abort(); }

      public void run() {
         try {
            //            System.out.println("Preview running...");
            String path = getOutputPath()+Util.FS+"Norder3";
            if( !isExistingAllskyDir() || !(new File(path)).isDirectory() ) throw new Exception("order3 tiles not found");

            builder = new BuilderAllsky(context);
            builder.run();

            String mysky = getTitle();
            if( mysky.trim().length()==0 ) mysky="MySky";
            int npix = lastShowAllSkyNorder3>=0 ? lastShowAllSkyNorder3 : 0;
            mainPanel.planPreview = (PlanBG) mainPanel.aladin.calque.getPlan(mysky);

            if (mainPanel.planPreview == null || mainPanel.planPreview.isFree() || mainPanel.planPreview.hasError() ) {
               double[] res = CDSHealpix.pix2ang_nest(3, npix);
               double[] radec = CDSHealpix.polarToRadec(new double[] { res[0], res[1] });
               radec = gal2ICRSIfRequired(radec);
               TreeObjDir gSky = new TreeObjDir(mainPanel.aladin, getOutputPath());
               int n = mainPanel.aladin.calque.newPlanBG(gSky,getOutputPath(), null, "="+mysky,
                     Coord.getSexa(radec[0], radec[1]), "30");
               Aladin.trace(4, "ContextGui.preview(): create "+mysky);
               mainPanel.planPreview = (PlanBG) mainPanel.aladin.calque.getPlan(n);
               mainPanel.planPreview.bScale=bscale;
               mainPanel.planPreview.bZero=bzero;
            } else {
               mainPanel.planPreview.forceReload();
               mainPanel.aladin.calque.repaintAll();
               Aladin.trace(4, "ContextGui.preview(): update "+mysky);
            }
            //            System.out.println("Preview done!");
         } catch (Exception e) {
            //            System.out.println("Preview aborted! "+e.getMessage());
         }
         previewThread=null;
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
      if( (action==Action.TILES || action==Action.JPEG
            || action==Action.PNG || action==Action.RGB) && lastNorder3>=0 ) updateHipsPreview(false);
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
      if( (action==Action.TILES || action==Action.MAPTILES || action==Action.JPEG
            || action==Action.PNG || action==Action.RGB) && lastNorder3>=0 ) updateHipsPreview(true);

      if( action==Action.INDEX ) mainPanel.tabBuild.resumeWidgets();
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

   //   public void setRgbPlans(Object [] plans) { plansRgb=plans; }
   public void setRgbOutput(String output) { outputRGB=output; }
   public void setHierarchyAlgo(ModeTree method) { hierarchyAlgo=method; }

   //   public Object [] getRgbPlans() { return plansRgb; }
   public String getRgbOutput() { return outputRGB; }
   public ModeTree getHierarchyAlgo() { return hierarchyAlgo; }

   public void setRgbFormat(int format) { targetColorMode=format; }

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
//         mainPanel.tabDesc.blankTextField.setText("Unknown value => ["+s+"]");
      }
      return b;
   }
   
   public String getBlankKey() {
      String s = mainPanel.tabDesc.getBlank().trim();
      if( s.length()>0 ) {
         
         // Ca ne doit pas être une valeur numérique
         try { Double.parseDouble(s); }
         catch( Exception e ) {
            hasAlternateBlank=false;
            return s;
         }
      }
      return null;
      
   }

   public int [] getHDU() {
      String s="";
      try {
         s = mainPanel.tabDesc.getHDU().trim();
         hdu = parseHDU(s);
      } catch( Exception e ) {
         mainPanel.tabDesc.blankTextField.setText("Syntax error => ["+s+"]");
      }
      return hdu;
   }

   /** Initialisation des paramètres (ne sert que pour contextGui) */
   public void initParameters() throws Exception {
      setMocArea( mainPanel.tabDesc.getMocField().trim() );
      setModeMerge( mainPanel.tabDesc.getCoaddModeField() );
      setSkyValName( mainPanel.tabDesc.getSkyvalField() );
      super.initParameters();
   }

   public String getSkyval() {
      skyvalName = mainPanel.tabDesc.getSkyvalField().toUpperCase();
      return skyvalName;
   }

   public String getTitle() {
      return mainPanel.tabDesc.getLabelField();
   }

   public TransfertFct getFct() throws Exception {
      if(  mainPanel.tabJpg.isCutFromPlanBase() ) {
         PlanImage p = (PlanImage)mainPanel.aladin.calque.getPlanBase();
         return TransfertFct.getFromCode(p.getTransfertFct());
      }
      return super.getFct();

   }

   public double[] getPixelRangeCut() throws Exception {
      double [] cut = new double[5];
      for( int i=0; i<4; i++ ) cut[i] = Double.NaN;
      if( mainPanel.tabJpg.isCutFromPlanBase() ) {
         PlanImage p = (PlanImage)mainPanel.aladin.calque.getPlanBase();
         cut[0]= p.getCutMin()*p.bScale+p.bZero;
         cut[1]= p.getCutMax()*p.bScale+p.bZero;

         //         cut[0]= ((p.getCutMin()*p.bScale+p.bZero)-bzero)/bscale;
         //         cut[1]= ((p.getCutMax()*p.bScale+p.bZero)-bzero)/bscale;

      } else {
         String cutMin = mainPanel.tabJpg.getCutMin();
         String cutMax = mainPanel.tabJpg.getCutMax();
         try { cut[0] = Double.parseDouble(cutMin); } catch( Exception e ) {}
         try { cut[1] = Double.parseDouble(cutMax); } catch( Exception e ) {}

         //         cut[0] = (Double.parseDouble(cutMin)-bzero)/bscale;
         //         cut[1] = (Double.parseDouble(cutMax)-bzero)/bscale;
      }

      return cut;
   }

   //   public double[] getCutOrig() throws Exception {
   //      if( cutOrig==null ) cutOrig = new double[4];
   //      if( mainPanel.tabJpg.isCutFromPlanBase() ) {
   //         PlanImage p = (PlanImage)mainPanel.aladin.calque.getPlanBase();
   //         cutOrig[0]= ((p.getCutMin()*p.bScale+p.bZero)-bZeroOrig)/bScaleOrig;
   //         cutOrig[1]= ((p.getCutMax()*p.bScale+p.bZero)-bZeroOrig)/bScaleOrig;
   //         //            cutOrig[2]= ((PlanImage)p).getDataMin();
   //         //            cutOrig[3]= ((PlanImage)p).getDataMax();
   //
   //      } else {
   //         String cutMin = mainPanel.tabJpg.getCutMin();
   //         String cutMax = mainPanel.tabJpg.getCutMax();
   //         cutOrig[0] = (Double.parseDouble(cutMin)-bZeroOrig)/bScaleOrig;
   //         cutOrig[1] = (Double.parseDouble(cutMax)-bZeroOrig)/bScaleOrig;
   //      }
   //
   //      return cutOrig;
   //   }

   public void setCutOrig(double [] c) {
      super.setCutOrig(c);
      if( c!=null ) {
         mainPanel.tabJpg.setCutMin( Util.myRound(c[Context.CUTMIN]*bScaleOrig+bZeroOrig) );
         mainPanel.tabJpg.setCutMax( Util.myRound(c[Context.CUTMAX]*bScaleOrig+bZeroOrig) );
      }
   }

   public void running(String string) { trace(3,"RUN   : "+string);  }
   public void nldone(String string)  { trace(3,"DONE  : "+string);  }
   public void done(String string)    { trace(3,"DONE  : "+string); }
   public void info(String string)    { trace(3,"INFO  : "+string); }
   public void warning(String string) { trace(3,"WARN  : "+string); }
   public void error(String string)   { mainPanel.aladin.error(mainPanel, string); trace(3,"ERROR : "+string);}
   public void action(String string)  { trace(3,"ACTION: "+string); }
   public void nlstat(String string)  { trace(3,"STAT  : "+string); }


   public void trace(int i, String string) {
      Aladin.trace(i, string);
   }
}
