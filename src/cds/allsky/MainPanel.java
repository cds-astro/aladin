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

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.Aladin;
import cds.aladin.Calib;
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
public class MainPanel extends JPanel implements ActionListener {

   protected Aladin aladin;
   
   private String s_ERR, s_ERRFITS;
   
   // Le formulaire multi-tab
   private JTabbedPane pTab;              // Le panel principale
   protected TabDesc  tabDesc;            // Le tab de la description du survey
   protected TabBuild tabBuild = null;    // Le tab de la construction HEALPix
   protected TabJpg   tabJpg;             // Le tab de la génération des JPEG associés
   protected TabPub   tabPub;             // Le tab pour la publication du survey HEALPix
   protected TabRgb   tabRgb;             // Le tab pour la génération d'un survey RGB HEALPix

   private int lastN3 = 0;
   private PlanBG planPreview;
   private boolean convertCut;

   protected int getLastN3() {
      return lastN3;
   }

   protected void setLastN3(int lastN3) {
      this.lastN3 = lastN3;
   }

   public MainPanel(Aladin aladin) {
      super();
      this.aladin = aladin;
      createChaine();
      createPanel();
      BuilderController.DEBUG = (Aladin.levelTrace > 0) ? true : false;

   }

   private void createPanel() {
      pTab = new JTabbedPane();
      
      tabBuild= new TabBuild(this);
      tabJpg  = new TabJpg(this);
      tabPub  = new TabPub(aladin,this);
      tabRgb  = new TabRgb(aladin);
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
   
   private String getString(String k) { return aladin.getChaine().getString(k); }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == tabDesc.getSourceDirField()) init();
   }
   
   private Fits lastOneFits = null;
   private String lastRootPath=null;
   
   /**
    * Cherche un fichier de type FITS dans le répertoire donné.
    * Utilise un cache une case pour éviter les recherches redondantes
    * @param aladinTree
    * @return null s'il y a eu une erreur ou le chemin du 1er fichier fits trouvé
    */
   private Fits getFits(String rootPath) {
      if( lastRootPath!=null && lastRootPath.equals(rootPath) ) return lastOneFits;
      lastOneFits = getFits1(rootPath);
      aladin.trace(2, "Will use this Fits file as reference => "+lastOneFits.getFilename());
      lastRootPath=rootPath;
      return lastOneFits;
   }
   
    private Fits getFits1(String rootPath) {
       File main = new File(rootPath);
       Fits fitsfile = new Fits();
       String[] list = main.list();
       String path = rootPath;
       if (list==null)
           return null;
       for (int f = 0 ; f < list.length ; f++) {
           if (!rootPath.endsWith(Util.FS)) {
               rootPath = rootPath+Util.FS;
           }
           path = rootPath+list[f];
           if ((new File(path)).isDirectory()) {
               if (!list[f].equals(Constante.SURVEY)) {
                   Fits f1 = getFits1(path);
                   if( f1!=null ) return f1;
               }
               else {
                   continue;
               }
           }
           try {
               // essaye de lire l'entete du fichier comme un fits
               fitsfile.loadHeaderFITS(path);
               // il n'y a pas eu d'erreur, donc c'est bien un FITS
               fitsfile.loadFITS(path);
               return fitsfile;
           }  catch (Exception e) {
//               System.err.println("Not a FITS file : " + path);
               continue;
           }
       }
       return null;
   }
   


   /**
    * Cherche un fichier fits dans l'arborescence et itialise les variables
    * bitpix et le cut avec Cherche aussi le meilleur nside pour la résolution
    * du fichier trouvé
    * 
    * @param text
    */
   public void init() {
      String text = tabDesc.getInputPath().trim();
      if (text != null && !text.equals("")) {
         try {
            // lit un fichier FITS dans le réperoire sélectionné
            Fits file = getFits(text);
            if (file == null || file.getCalib() == null) {
               JOptionPane.showMessageDialog(this, s_ERRFITS + text,
                     s_ERR, JOptionPane.ERROR_MESSAGE);
               return;
            }
            // récupère le bitpix
            tabBuild.setOriginalBitpix(file.bitpix);
            // récupère le bscale/bzero
            tabBuild.setBScaleBZero(file.bscale, file.bzero);
            // récupère le blank
            tabBuild.setBlank(file.blank);
            // récupère le min max pour le cut
            initCut();
            // calcule le meilleur nside
            long nside = healpix.core.HealpixIndex.calculateNSide(file
                  .getCalib().GetResol()[0] * 3600.);
            setSelectedOrder((int) Util.order((int)nside) - BuilderHpx.ORDER);

         } catch (Exception e1) {
            //				e1.printStackTrace();
         }
      }
   }

   protected void initCut() {
      String path = getInputPath();
      convertCut = (tabBuild.getBitpix() != getOriginalBitpix());
      if( path == null || "".equals(path)) {
         path = getOutputPath();
         convertCut=false;
      } else {
         Fits file = getFits(path);
         double[] cut = ThreadAutoCut.run(file);
         setCut(cut);
      }
      if (convertCut) convertCut(tabBuild.getBitpix());
   }
   private int setSelectedOrder(int val) {
      return tabBuild.setSelectedOrder(val);
   }

   protected double[] getCut() {
      return tabJpg.getCut();
   }
   
   protected void setCut(double [] cut) {
      tabJpg.setCut(cut);
   }

   protected void convertCut(int bitpix) {
      double[] cut = tabJpg.getCut();
      double [] oldminmax = new double[] {cut[2],cut[3]};
      cut[0] = Fits.toBitpixRange(cut[0], bitpix, oldminmax);
      cut[1] = Fits.toBitpixRange(cut[1], bitpix, oldminmax);
      setCut(cut);
   }

   public void updateCurrentCM() { tabJpg.updateCurrentCM(); }

   protected double[] getBScaleBZero() {
      return new double[]{tabBuild.getBscale(), tabBuild.getBzero()};
   }
   
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

   protected double getBlank() {
      double blank = tabBuild.getBlank();
      String s="";
      try { 
         s = tabDesc.getBlank().trim();
         if( s.length()>0 ) blank = Double.parseDouble(s);
      } catch( Exception e ) {
         tabDesc.blankTextField.setText("Unknown value => ["+s+"]");
      }
      return blank;
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
      // si un repertoire de sortie ALLSKY existe déjà, on change le nom du
      // bouton START
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
   //	public void resetJpg() {
   //		cds.tools.Util.deleteDir(new File(getOutputPath()),".*\\.jpg$");
   //	}
   protected boolean toFast() {
      return tabBuild.toFast();
   }

   protected boolean toFading() {
      return tabBuild.toFading();
   }

   protected void setInitDir(String txt) {
      tabBuild.setInitDir(txt);
   }

   /**
    * @return the keepBB
    */
   protected boolean isKeepBB() {
      return tabBuild.isKeepBB();
   }

   protected int getOriginalBitpix() {
      return tabBuild.getOriginalBitpix();
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
      initCut();
      tabBuild.displayNext();
      tabJpg.setStartEnabled(b);
      tabPub.setStartEnabled(b);
      if( tabRgb!=null ) tabRgb.setStartEnabled(b);
   }

   /**
    * Création/rafraichissemnt d'un allsky (en l'état) et affichage
    * 
    * @param last
    * */
   void preview(int last) {
      String mysky = tabDesc.getLabel();
      try {
         planPreview = (PlanBG) aladin.calque.getPlan(mysky);
         if (planPreview == null || planPreview.isFree() || planPreview.hasError() ) {
            double[] res = CDSHealpix.pix2ang_nest(cds.tools.pixtools.Util.nside(3), last);
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