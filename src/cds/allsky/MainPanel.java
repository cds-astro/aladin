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
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.StringTokenizer;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.Aladin;
import cds.aladin.Calib;
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
import cds.tools.pixtools.Util;

/**
 * Gère le formulaire de génération des surveys HEALPix
 * @author Anaïs Oberto + Pierre Fernique
 */
final public class MainPanel extends JPanel implements ActionListener {

   protected Aladin aladin;
   protected ContextGui context;

   private String s_ERR, s_ERRFITS;

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
      createChaine();
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

   private void createChaine() {
      s_ERRFITS= getString("ERRFITS");
      s_ERR    = getString("ERROR");
   }

   private String getString(String k) { return Aladin.getChaine().getString(k); }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == tabDesc.getSourceDirField()) {
         init();
      }
   }

   /**
    * Sélectionne un fichier de type FITS (ou équivalent) dans le répertoire donné => va servir d'étalon
    * Utilise un cache une case pour éviter les recherches redondantes
    * @return true si trouvé
    */
   private boolean findImgEtalon(String rootPath) {
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
         
         // essaye de lire l'entete fits du fichier et tente d'en extraire une calib.
         // s'il n'y a pas eu d'erreur ça peut servir d'étalon
         try {
            Aladin.trace(4, "MainPanel.findImgEtalon: loading header "+path+"...");
            fitsfile.loadHeaderFITS(path);
            fitsfile.getCalib();
            context.setImgEtalon(path);
            return true;
            
         }  catch (Exception e) { continue; }
      }
      return false;
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
      boolean found = findImgEtalon(path);
      if( !found ) {
         context.warning("There is no available images in source directory !\n"+s_ERRFITS+path);
         return;
      }
      String filename = context.getImgEtalon();
      Fits file = new Fits();
      try { file.loadHeaderFITS(filename); } 
      catch( Exception e ) { e.printStackTrace(); }
      context.setBitpixOrig(file.bitpix);
      tabBuild.setOriginalBitpix(file.bitpix);
      if( !context.isColor() ) {
         context.setBZeroOrig(file.bzero);
         context.setBScaleOrig(file.bscale);
         context.setBlankOrig(file.blank);
      }
      
//      tabBuild.setBScaleBZero(file.bscale, file.bzero);
//      tabBuild.setBlank(file.blank);
      
      // calcule le meilleur nside
      long nside = healpix.core.HealpixIndex.calculateNSide(file.getCalib().GetResol()[0] * 3600.);
      setSelectedOrder((int) Util.order((int)nside) - Constante.ORDER);
      
      initCut(file);

      newAllskyDir();
   }
   
   protected void initCut(Fits file) {
      int w = file.width;
      int h = file.height;
      if( w>1024 ) w=1024;
      if( h>1024 ) h=1024;
      try {
         file.loadFITS(file.getFilename(),0,0,w,h); 
         double [] cut = file.findAutocutRange();
         context.setCutOrig( cut );
      } catch( Exception e ) { e.printStackTrace(); }
   }



//   private boolean debugPierre=false;   // BEURK - sinon le repaint Swing doit attendre la fin du init() ci-dessous
//
//   /**
//    * Cherche un fichier fits dans l'arborescence et itialise les variables
//    * bitpix et le cut avec Cherche aussi le meilleur nside pour la résolution
//    * du fichier trouvé
//    * 
//    * @param text
//    */
//   public void init() {
//      final String text = getInputPath().trim();
//      debugPierre=true;
//      if (text != null && !text.equals("")) {
//         try {
//            (new Thread("Autocut"){
//               public void run() {
//                  cds.tools.Util.pause(100);
//                  tabDesc.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//                  // lit un fichier FITS dans le réperoire sélectionné
//                  Fits file = findImgEtalon(text);
//                  if (file == null || file.getCalib() == null) {
//                     System.err.println(s_ERRFITS + text);
//                     //                     JOptionPane.showMessageDialog(this, s_ERRFITS + text,
//                     //                           s_ERR, JOptionPane.ERROR_MESSAGE);
//                     return;
//                  }
//                  // récupère le bitpix
//                  tabBuild.setOriginalBitpix(file.bitpix);
//                  // récupère le bscale/bzero
//                  tabBuild.setBScaleBZero(file.bscale, file.bzero);
//                  // récupère le blank
//                  tabBuild.setBlank(file.blank);
//                  // récupère le min max pour le cut
//                  initCut();
//                  // calcule le meilleur nside
//                  long nside = healpix.core.HealpixIndex.calculateNSide(file
//                        .getCalib().GetResol()[0] * 3600.);
//                  setSelectedOrder((int) Util.order((int)nside) - BuilderHpx.ORDER);
//                  debugPierre=false;
//                  tabDesc.setCursor(Cursor.getDefaultCursor());
//                  newAllskyDir();
//               }
//            }).start();
//
//         } catch (Exception e1) {
//            //				e1.printStackTrace();
//         }
//      }
//   }

//   protected void initCut() {
//      String path = getInputPath();
//      boolean convertCut = (tabBuild.getBitpix() != tabBuild.getOriginalBitpix());
//      if( path == null || "".equals(path)) {
//         path = getOutputPath();
//         convertCut=false;
//      } else {
//         final String finalPath=path;
//         try {
//            //            (new Thread("Autocut"){
//            //               public void run() {
//            //               double[] cut = ThreadAutoCut.run(file);
//            final Fits file = findImgEtalon(finalPath);
//            double[] cut;
//            try {
//               cut = file.findAutocutRange();
//               context.setCut(cut);
//            } catch( Exception e ) {
//               e.printStackTrace();
//            }
//            //               }
//         //            }).start();
//         } catch( Throwable e1 ) {
//            e1.printStackTrace();
//            return;
//         }
//      }
//      if (convertCut) context.convertCut(tabBuild.getBitpix());
//   }
   
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

   public int getCoAddMode() {
      return tabDesc.getCoaddMode();
   }

   /** Retourne la liste des losanges HEALPix spécifiquement à traiter, null si tout le ciel */
   public HpixTree getHpixTree() {
      String s = tabDesc.getSpecifNpix().trim();
      if( s.length()==0 ) return null;
      HpixTree hpixTree = new HpixTree(s);
      if( hpixTree.getSize()==0 ) return null;
      return hpixTree;
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
      double cutmax = planPreview.getDataMin();
      double datamin = planPreview.getDataMin();
      double datamax = planPreview.getDataMax();
      Aladin.trace(4,"MainPanel.setCutFromPreview: cutmin,cutmax = ["+cutmin+".."+cutmax+"] datamin,datamax = ["+datamin+".."+datamax+"] ");
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