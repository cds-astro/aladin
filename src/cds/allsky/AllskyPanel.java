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

import static cds.allsky.AllskyConst.INDEX;
import static cds.allsky.AllskyConst.TESS;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Chaine;
import cds.aladin.Coord;
import cds.aladin.PlanBG;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.HpixTree;
import cds.tools.pixtools.Util;

public class AllskyPanel extends JPanel implements ActionListener {
   //	private AllskyStepsPanel pSteps = null;

   private String s_DESC, s_BUILD, s_DISP, s_PUBLISH, s_RGB, s_ERR,
   s_ERRFITS;
   private String tipDesc, tipBuild, tipDisplay, tipPublish, tipRGB;
   BuildPanel pBuild = null;
   private PublishPanel pPublish;
   private JPGPanel pDisplay;
   private RGBPanel pRGB;
   //	private CleanPanel pClean;

   protected Aladin aladin;

   BorderLayout bLay = new BorderLayout(20, 10);
   DescPanel pDesc;
   //	JPanel pBuildAll;

   // Onglet Open
   JTextField field;
   JTextArea glu;
   JPanel pView;
   private int bitpix = -1;
   private int order;
   private JTabbedPane pTab;

   private int lastN3 = 0;
   private PlanBG planPreview;
   private boolean convertCut;

   protected int getLastN3() {
      return lastN3;
   }

   protected void setLastN3(int lastN3) {
      this.lastN3 = lastN3;
   }

   public AllskyPanel(Aladin a) {
      super();
      aladin = a;
      createChaine();
      createPanel();
      DBBuilder.DEBUG = (Aladin.levelTrace > 0) ? true : false;

   }

   private void createPanel() {
      pTab = new JTabbedPane();
      pBuild = new BuildPanel(this);
      pDisplay = new JPGPanel(this);
      pPublish = new PublishPanel(aladin,this);

      pDesc = new DescPanel(aladin.getDefaultDirectory(), this);
      pDesc.getSourceDirField().addActionListener(this);

      pRGB = new RGBPanel(aladin);

      // ----
      // ajoute l'onglet dans le panel
      pTab.addTab(s_DESC, null, pDesc, tipDesc);
      pTab.addTab(s_BUILD, null, pBuild, tipBuild);
      pTab.addTab(s_DISP, null, pDisplay, tipDisplay);
      pTab.addTab(s_PUBLISH, null, pPublish, tipPublish);

      if (pRGB!=null) {
         pTab.addTab(s_RGB, null, pRGB, tipRGB);
         pTab.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
               if (pTab.getSelectedComponent() == pRGB)
                  pRGB.init();
            }
         });		
      }

      add(pTab, BorderLayout.CENTER);

   }

   private void createChaine() {
      s_RGB = getString("MRGB");
      s_DESC = getString("MDESC");
      s_BUILD = getString("MBUILD");
      s_DISP = getString("MDISPLAY");
      s_PUBLISH = getString("MPUBLISH");
      s_ERRFITS = getString("ERRFITS");
      s_ERR = getString("ERROR");
      tipBuild = getString("MTIPBUILD");
      tipPublish = getString("MTIPPUBLISH");
      tipRGB = getString("MTIPRGB");
   }
   
   private String getString(String k) { return aladin.getChaine().getString(k); }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == pDesc.getSourceDirField()) init();
   }

   /**
    * Cherche un fichier fits dans l'arborescence et itialise les variables
    * bitpix et le cut avec Cherche aussi le meilleur nside pour la résolution
    * du fichier trouvé
    * 
    * @param text
    */
   public void init() {
      String text = pDesc.getInputPath().trim();
      if (text != null && !text.equals("")) {
         try {
            // lit un fichier FITS dans le réperoire sélectionné
            Fits file = Fits.getFits(text);
            if (file == null || file.getCalib() == null) {
               JOptionPane.showMessageDialog(this, s_ERRFITS + text,
                     s_ERR, JOptionPane.ERROR_MESSAGE);
               return;
            }
            // récupère le bitpix
            bitpix = file.bitpix;
            pBuild.setOriginalBitpix(bitpix);
            // récupère le bscale/bzero
            pBuild.setBScaleBZero(file.bscale, file.bzero);
            // récupère le blank
            pBuild.setBlank(file.blank);
            // récupère le min max pour le cut
            initCut();
            // calcule le meilleur nside
            long nside = healpix.core.HealpixIndex.calculateNSide(file
                  .getCalib().GetResol()[0] * 3600.);
            order = (int) Util.order((int)nside);
            setSelectedOrder(order - HpxBuilder.ORDER);

         } catch (Exception e1) {
            //				e1.printStackTrace();
         }
      }
   }

   protected void initCut() {
      String path = getInputPath();
      convertCut = (pBuild.getBitpix() != getOriginalBitpix());
      if (path == null || "".equals(path)) {
         path = getOutputPath();
         convertCut=false;
      } else {
         Fits file = Fits.getFits(path);
         double[] cut = ThreadAutoCut.run(file);
         pDisplay.setCut(cut);
      }
      if (convertCut) convertCut(pBuild.getBitpix());
   }
   private int setSelectedOrder(int val) {
      return pBuild.setSelectedOrder(val);
   }

   protected double[] getCut() {
      return pDisplay.getCut();
   }

   protected void convertCut(int bitpix) {
      double[] cut = pDisplay.getCut();
      double [] oldminmax = new double[] {cut[2],cut[3]};
      cut[0] = Fits.toBitpixRange(cut[0], bitpix, oldminmax);
      cut[1] = Fits.toBitpixRange(cut[1], bitpix, oldminmax);
      pDisplay.setCut(cut);
   }

   public void updateCurrentCM() { pDisplay.updateCurrentCM(); }

   protected double[] getBScaleBZero() {
      return new double[]{pBuild.getBscale(), pBuild.getBzero()};
   }
   /**
    * 
    * @return order choisi ou -1 s'il doit etre calculé
    */
   protected int getOrder() {
      if (pBuild.getOrder() != -1) {
         return pBuild.getOrder();
      }
      if (planPreview != null)
         return planPreview.getMaxHealpixOrder();
      return -1;
   }
   
   protected int getBitpix() {
      return pBuild.getBitpix();
   }

   protected double getBlank() {
      double blank = pBuild.getBlank();
      String s="";
      try { 
         s = pDesc.getBlank();
         if( s!=null ) blank = Double.parseDouble(s);
      } catch( Exception e ) {
         pDesc.blankTextField.setText("Unknown value => ["+s+"]");
      }
      return blank;
   }

   protected String getInputPath() {
      if( pDesc==null ) return null;
      return pDesc.getInputPath();
   }

   public String getOutputPath() {
      if( pDesc==null ) return null;
      return pDesc.getOutputPath();
   }
   
   public int getCoAddMode() {
      return pDesc.getCoaddMode();
   }
   
   /** Retourne la liste des losanges HEALPix spécifiquement à traiter, null si tout le ciel */
   public HpixTree getHpixTree() {
      String s = pDesc.getSpecifNpix().trim();
      if( s.length()==0 ) return null;
      HpixTree hpixTree = new HpixTree(s);
      if( hpixTree.getSize()==0 ) return null;
      return hpixTree;
   }

   public void showDesc() {
      pTab.setSelectedComponent(pDesc);
   }

   public void showPublish() {
      pTab.setSelectedComponent(pPublish);
   }

   public void showRGB() {
      if (pRGB != null) // version beta
         pTab.setSelectedComponent(pRGB);
   }

   public void showDisplay() {
      pTab.setSelectedComponent(pDisplay);
   }
   public void showBuild() {
      pTab.setSelectedComponent(pBuild);
   }

   public void resetProgress() {
      enableProgress(true, INDEX);
      enableProgress(true, TESS);
      setProgress(INDEX, 0);
      setProgress(TESS, 0);
   }

   protected void enableProgress(boolean selected, int mode) {
      pBuild.enableProgress(selected, mode);
   }
   protected void setProgress(int mode, int value) {
      pBuild.setProgress(mode, value);
   }
   public String getLabel() {
      return pDesc.getLabel();
   }

   protected void newAllskyDir() {
      pPublish.newAllskyDir(AllskyConst.SURVEY);
      // si un repertoire de sortie ALLSKY existe déjà, on change le nom du
      // bouton START
      setStart();
      pDesc.resumeWidgetsStatus();
      if( isExistingAllskyDir() ) preview(0);
      
//      if (pDesc.getInputPath() != null
//            && (new File(pDesc.getInputPath())).exists()
//            && (new File(pDesc.getOutputPath())).exists()) {
//         // met le bouton Reset utilisable, mais pas selectionné
//         setResume();
//         preview(0);
//      } else if ((new File(pDesc.getOutputPath())).exists()) {
//         // met les boutons "Start" des autres onglets/actions utilisables
//         setStartEnabled(true);
//         preview(0);
//      }
   }
   
   protected boolean isExistingDir() {
      return pDesc!=null && pDesc.getInputPath() != null && (new File(pDesc.getInputPath())).exists();
   }

   protected boolean isExistingAllskyDir() {
      return pDesc!=null && pDesc.getOutputPath() != null && (new File(pDesc.getOutputPath())).exists();
   }
   
   private boolean isRunning=false;
   protected boolean isRunning() { return isRunning; }
   protected void setIsRunning(boolean flag) { isRunning=flag; }

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
      pBuild.stop();
   }

   public void toReset() {
      if( pDesc.isResetIndex() ) resetIndex();
      if (pDesc.isResetHpx()) resetHpx();
   }
   
   public void resetIndex() {
      cds.tools.Util.deleteDir(new File(getOutputPath()
            + AllskyConst.HPX_FINDER));
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
   //	public void resetJpg() {
   //		cds.tools.Util.deleteDir(new File(getOutputPath()),".*\\.jpg$");
   //	}
   protected boolean toFast() {
      return pBuild.toFast();
   }

   protected boolean toFading() {
      return pBuild.toFading();
   }

   protected void setInitDir(String txt) {
      pBuild.setInitDir(txt);
   }

   /**
    * @return the keepBB
    */
   protected boolean isKeepBB() {
      return pBuild.isKeepBB();
   }

   protected int getOriginalBitpix() {
      return pBuild.getOriginalBitpix();
   }

   public void displayStart() {
      pBuild.displayStart();
//      pDesc.setResetEnable(false);
//      pDesc.setResetSelected(false);
   }
   public void displayReStart() {
      pBuild.displayReStart();
   }

   public void displayResume() {
      pBuild.displayResume();
   }

   public void displayDone() {
      pBuild.displayDone();
   }

   protected void clearForms() {
      AllskyConst.SURVEY = AllskyConst.ALLSKY;
      pDesc.clearForms();
      pBuild.clearForms();

      pDisplay.clearForms();
      pPublish.clearForms();
      setStart();
   }

   protected void export(String path) {
      aladin.frameAllsky.export(planPreview, path);
   }

   public void done() {
      showDisplay();
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
      initCut();
      pBuild.displayNext();
      pDisplay.setStartEnabled(b);
      pPublish.setStartEnabled(b);
      if( pRGB!=null ) pRGB.setStartEnabled(b);
   }

   /**
    * Création/rafraichissemnt d'un allsky (en l'état) et affichage
    * 
    * @param last
    * */
   void preview(int last) {
      String mysky = pDesc.getLabel();
      try {
         planPreview = (PlanBG) aladin.calque.getPlan(mysky);
         if (planPreview == null || planPreview.isFree() || planPreview.hasError() ) {
            double[] res = CDSHealpix.pix2ang_nest(cds.tools.pixtools.Util
                  .nside(3), last);
            double[] radec = CDSHealpix.polarToRadec(new double[] { res[0],
                  res[1] });
            radec = Calib.GalacticToRaDec(radec[0], radec[1]);
            int n = aladin.calque.newPlanBG(getOutputPath(), "="+mysky,
                  Coord.getSexa(radec[0], radec[1]), "30");
            Aladin.trace(4, "AllskyTask.preview: create "+mysky);
            planPreview = (PlanBG) aladin.calque.getPlan(n);
            setStartEnabled(true);
         } else {
            planPreview.forceReload();
            aladin.calque.repaintAll();
            Aladin.trace(4, "AllskyTask.preview: update "+mysky);

         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
}

class ThreadAutoCut extends Thread {
   static Fits file = null;
   static double[] cut = null;

   protected static double[] run(Fits file) {
      ThreadAutoCut.file = file;
      (new ThreadAutoCut()).run();
      return cut;
   }


   public void run() {
      try {
         cut = file.findAutocutRange();
      } catch (Exception e) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
}