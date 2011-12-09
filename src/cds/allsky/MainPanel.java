// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General protected License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General protected License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General protected License for more details.
//
//    The GNU General protected License is available in COPYING file
//    along with Aladin.
//

package cds.allsky;

import static cds.allsky.Constante.INDEX;
import static cds.allsky.Constante.TESS;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.PlanBG;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.HpixTree;
import cds.tools.pixtools.Util;

/**
 * Gère le formulaire de génération des surveys HEALPix
 * @author Anaïs Oberto + Pierre Fernique
 */
final public class MainPanel extends JPanel implements ActionListener {

   protected Aladin aladin;
   protected ContextGui context;

   // Le formulaire multi-tab
   private JTabbedPane pTab;              // Le panel principale
   protected TabDesc  tabDesc;            // Le tab de la description du survey
   protected TabBuild tabBuild = null;    // Le tab de la construction HEALPix
   protected TabJpg   tabJpg;             // Le tab de la génération des JPEG associés
   protected TabPub   tabPub;             // Le tab pour la publication du survey HEALPix
   protected TabRgb   tabRgb;             // Le tab pour la génération d'un survey RGB HEALPix

   PlanBG planPreview;

   public MainPanel(Aladin aladin,ContextGui context) {
      super();
      this.aladin = aladin;
      this.context = context;
      context.setMainPanel(this);
      createPanel();
      BuilderController.DEBUG = (Aladin.levelTrace > 0) ? true : false;
   }

   private void createPanel() {
      pTab = new JTabbedPane();

      tabBuild= new TabBuild(this);
      tabJpg  = new TabJpg(this);
      tabPub  = new TabPub(aladin,this);
      tabRgb  = new TabRgb(aladin,this);
      tabDesc = new TabDesc(aladin.getDefaultDirectory(), this);
      tabDesc.getSourceDirField().addActionListener(this);

      pTab.addTab( getString("MDESC"),   null, tabDesc,  null);
      pTab.addTab( getString("MBUILD"),  null, tabBuild, getString("MTIPBUILD"));
      pTab.addTab( getString("MDISPLAY"),null, tabJpg,   null);
      pTab.addTab( getString("MPUBLISH"),null, tabPub,   getString("MTIPPUBLISH"));

      pTab.addTab( getString("MRGBA"), null, tabRgb, getString("MTIPRGB"));
      pTab.addChangeListener(new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
            if (pTab.getSelectedComponent() == tabRgb)
               tabRgb.init();
         }
      });		

      add(pTab, BorderLayout.CENTER);
   }

   private String getString(String k) { return Aladin.getChaine().getString(k); }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == tabDesc.getSourceDirField()) {
         init();
      }
   }

   /**
    * Cherche un fichier fits dans l'arborescence et itialise les variables
    * bitpix et le cut avec Cherche aussi le meilleur nside pour la résolution
    * du fichier trouvé
    * 
    * @param text
    */
   public void init() {
      String path = getInputPath();
      boolean found = context.findImgEtalon(path);
      if( !found ) {
         context.warning("There is no available images in source directory !\n"
                + path);
         return;
      }
      String filename = context.getImgEtalon();
      Fits file = new Fits();
      try {
         file.loadHeaderFITS(filename);
      } catch (Exception e) {
         e.printStackTrace();
      }
      tabBuild.setOriginalBitpix(file.bitpix);
      
//      tabBuild.setBScaleBZero(file.bscale, file.bzero);
//      tabBuild.setBlank(file.blank);
      
      // calcule le meilleur nside
      long nside = healpix.core.HealpixIndex.calculateNSide(file.getCalib().GetResol()[0] * 3600.);
      setSelectedOrder((int) Util.order((int)nside) - Constante.ORDER);
      
      newAllskyDir();
   }
   
   private int setSelectedOrder(int val) {
      return tabBuild.setSelectedOrder(val);
   }

   public void updateCurrentCM() { tabJpg.updateCurrentCM(); }

   /**
    * 
    * @return order choisi ou -1 s'il doit etre calculé
    */
   protected int getOrder() {
      if (tabBuild.getOrder() != -1) {
         return tabBuild.getOrder();
      }
      if (planPreview != null)
         return planPreview.getMaxHealpixOrder();
      return -1;
   }

   protected int getBitpix() {
      return tabBuild.getBitpix();
   }


   protected String getBorderSize() {
      if( tabDesc==null ) return null;
      return tabDesc.getBorderSize().trim();
   }

   protected String getInputPath() {
      if( tabDesc==null ) return null;
      return tabDesc.getInputPath();
   }

   public String getOutputPath() {
      if( tabDesc==null ) return null;
      return tabDesc.getOutputPath();
   }

   public CoAddMode getCoAddMode() {
      return tabDesc.getCoaddMode();
   }

   /** Retourne la liste des losanges HEALPix spécifiquement à traiter, null si tout le ciel */
//   public HpixTree getMoc() {
//      String s = tabDesc.getSpecifNpix().trim();
//      return context.setMoc(s);
//   }
   
   public String getMoc() {
      return tabDesc.getSpecifNpix().trim();
   }

   public void showDescTab() {
      pTab.setSelectedComponent(tabDesc);
   }

   public void showBuildTab() {
      pTab.setSelectedComponent(tabBuild);
   }

   public void showJpgTab() {
      pTab.setSelectedComponent(tabJpg);
   }

   public void showPubTab() {
      pTab.setSelectedComponent(tabPub);
   }

   public void showRgbTab() {
      pTab.setSelectedComponent(tabRgb);
   }

   public void resetProgress() {
      enableProgress(true, INDEX);
      enableProgress(true, TESS);
      setProgress(INDEX, 0);
      setProgress(TESS, 0);
   }

   protected void enableProgress(boolean selected, int mode) {
      tabBuild.enableProgress(selected, mode);
   }
   protected void setProgress(int mode, int value) {
      tabBuild.setProgress(mode, value);
   }
   public String getLabel() {
      return tabDesc.getLabel();
   }

   protected void newAllskyDir() {
      tabPub.newAllskyDir(Constante.SURVEY);
      //      // si un repertoire de sortie ALLSKY existe déjà, on change le nom du
      //      // bouton START
      setStart();
      tabDesc.resumeWidgetsStatus();
      if( isExistingAllskyDir() ) preview(0);
   }

   protected boolean isExistingDir() {
      return tabDesc!=null && tabDesc.getInputPath() != null && (new File(tabDesc.getInputPath())).exists();
   }

   protected boolean isExistingAllskyDir() {
      return tabDesc!=null && tabDesc.getOutputPath() != null && (new File(tabDesc.getOutputPath())).exists();
   }

   private boolean isRunning=false;
   protected boolean isRunning() { return isRunning; }
   protected void setIsRunning(boolean flag) { 
      isRunning=flag;
      if (!isRunning) done();
   }
   
   public void setAbort() {
      isRunning=false;
      tabBuild.resumeWidgetsStatus();
      tabBuild.stop();
   }

   public void setRestart() {
      displayReStart();
      //      pDesc.setResetEnable(true);
      //      pDesc.setResetSelected(true);
   }
   public void setResume() {
      displayResume();
      //      pDesc.setResetEnable(true);
      //      pDesc.setResetSelected(false);
      setStartEnabled(true);
   }
   public void setDone() {
      displayDone();
      //      pDesc.setResetEnable(false);
      //      pDesc.setResetSelected(false);
   }
   public void setStart() {
      displayStart();
      //      pDesc.setResetEnable(false);
      //      pDesc.setResetSelected(false);
      setStartEnabled(false);
   }

   public void close() {
      aladin.frameAllsky.close();
   }

   public void stop() {
      context.setIsRunning(false);
      tabBuild.stop();
   }

   public void toReset() {
      if( tabDesc.isResetIndex() ) resetIndex();
      if (tabDesc.isResetHpx()) resetHpx();
   }

   public void resetIndex() {
      cds.tools.Util.deleteDir(new File(getOutputPath()
            + Constante.HPX_FINDER));
   }
   public void resetHpx() {
      File dir = new File(getOutputPath());
      File[] children = dir.listFiles();
      // pour tous répertoires Norder du répertoire principal
      for (int i=0; i<children.length; i++) {
         if (children[i].getName().startsWith("Norder")
               && children[i].isDirectory()) {
            cds.tools.Util.deleteDir(children[i]);
         }
      }
   }

   protected void setProgressIndexDir(String txt) {
      tabBuild.setProgressIndexDir(txt);
   }

   public void displayStart() {
      tabBuild.displayStart();
      //      pDesc.setResetEnable(false);
      //      pDesc.setResetSelected(false);
   }
   public void displayReStart() {
      tabBuild.displayReStart();
   }

   public void displayResume() {
      tabBuild.displayResume();
   }

   public void displayDone() {
      tabBuild.displayDone();
   }

   protected void clearForms() {
      Constante.SURVEY = Constante.ALLSKY;
      tabDesc.clearForms();
      tabBuild.clearForms();

      tabJpg.clearForms();
      tabPub.clearForms();
      setStart();
   }

   protected void export(String path) {
      aladin.frameAllsky.export(planPreview, path);
   }

   public void done() {
      tabBuild.resumeWidgetsStatus();
      showJpgTab();
      setDone();
   }

   public boolean hasJpg() {
      try {
         return cds.tools.Util.find(getOutputPath(),".jpg");
      } catch( Exception e ) {
         return false;
      }
   }

   public void setStartEnabled(boolean b) {
      tabBuild.displayNext();
      tabJpg.setStartEnabled(b);
      tabPub.setStartEnabled(b);
      if( tabRgb!=null ) tabRgb.setStartEnabled(b);
   }

   // Récupération des valeurs cutmin, cutmax de l'affichage
   protected void setCutFromPreview() {
      if( planPreview==null ) return;
      double cutmin = planPreview.getCutMin();
      double cutmax = planPreview.getCutMax();
      double datamin = planPreview.getDataMin();
      double datamax = planPreview.getDataMax();
      Aladin.trace(4, "MainPanel.setCutFromPreview: cutmin,cutmax = ["
            + cutmin + ".." + cutmax + "] datamin,datamax = [" + datamin
            + ".." + datamax + "] ");
      context.setCut(new double[]{cutmin,cutmax,datamin,datamax});
   }

   /**
    * Création/rafraichissemnt d'un allsky (en l'état) et affichage
    * @param last
    * */
   void preview(int last) {
      String mysky = tabDesc.getLabel();
      try {
         planPreview = (PlanBG) aladin.calque.getPlan(mysky);
         if (planPreview == null || planPreview.isFree() || planPreview.hasError() ) {
            double[] res = CDSHealpix.pix2ang_nest(cds.tools.pixtools.Util.nside(3), last);
            double[] radec = CDSHealpix.polarToRadec(new double[] { res[0], res[1] });
            radec = context.gal2ICRSIfRequired(radec);
            int n = aladin.calque.newPlanBG(getOutputPath(), "="+mysky,
                  Coord.getSexa(radec[0], radec[1]), "30");
            Aladin.trace(4, "MainPanel.preview: create "+mysky);
            planPreview = (PlanBG) aladin.calque.getPlan(n);
            setStartEnabled(true);
         } else {
            planPreview.forceReload();
            aladin.calque.repaintAll();
//            setCutFromPreview();
            Aladin.trace(4, "MainPanel.preview: update "+mysky);

         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }


}

//class ThreadAutoCut extends Thread {
//   static Fits file = null;
//   static double[] cut = null;
//
//   protected static double[] run(Fits file) {
//      ThreadAutoCut.file = file;
//      (new ThreadAutoCut()).run();
//      return cut;
//   }
//
//
//   public void run() {
//      try {
//         cut = file.findAutocutRange();
//      } catch (Exception e) {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//      }
//   }
//}