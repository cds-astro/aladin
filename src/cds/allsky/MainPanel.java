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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ProgressBarUI;

import cds.aladin.Aladin;
import cds.aladin.PlanBG;
import cds.fits.Fits;
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
      BuilderTiles.DEBUG = (Aladin.levelTrace > 0) ? true : false;
   }
   
   protected JProgressBar getProgressBarTile()  { return tabBuild.buildProgressPanel.getProgressBarTile(); } 
   protected JProgressBar getProgressBarIndex() { return tabBuild.buildProgressPanel.getProgressBarIndex(); } 

   private void createPanel() {
      pTab = new JTabbedPane();

      tabBuild= new TabBuild(this);
      tabJpg  = new TabJpg(this);
      tabPub  = new TabPub(aladin,this);
      tabRgb  = new TabRgb(aladin,this);
      tabDesc = new TabDesc(aladin.getDefaultDirectory(), this);
      tabDesc.inputField.addActionListener(this);

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

   private String getString(String k) { return aladin.getChaine().getString(k); }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == tabDesc.inputField )  init();
   }
   
   /**
    * Cherche un fichier fits dans l'arborescence et initialise les variables
    * bitpix et le cut avec Cherche aussi le meilleur nside pour la résolution
    * du fichier trouvé
    * 
    * @param text
    */
   public void init() {
      String path = context.getInputPath();
      if( path.trim().length()==0 ) return;
      boolean found = context.findImgEtalon(path);
      if( !found ) {
         context.warning("There is no available images in source directory !\n" + path);
         return;
      }
      String filename = context.getImgEtalon();
      Fits file = new Fits();
      try { file.loadHeaderFITS(filename); }
      catch (Exception e) { e.printStackTrace(); }
      tabBuild.setOriginalBitpixField(file.bitpix);
      
      // calcule le meilleur nside
      long nside = healpix.core.HealpixIndex.calculateNSide(file.getCalib().GetResol()[0] * 3600.);
      tabBuild.setSelectedOrder((int) Util.order((int)nside) - Constante.ORDER);
      
      newAllskyDir();
   }
   
   public void updateCurrentCM() { tabJpg.updateCurrentCM(); }
   public void showDescTab() { pTab.setSelectedComponent(tabDesc); }
   public void showBuildTab() { pTab.setSelectedComponent(tabBuild); }
   public void showJpgTab() { pTab.setSelectedComponent(tabJpg); }
   public void showPubTab() { pTab.setSelectedComponent(tabPub); }
   public void showRgbTab() { pTab.setSelectedComponent(tabRgb); }

   protected void newAllskyDir() {
      tabPub.newAllskyDir(Constante.SURVEY);
      try { context.loadMocIndex(); } catch( Exception e ) { }
      resumeWidgets();
   }
   
   protected void resumeWidgets() {
      tabDesc.resumeWidgets();
      tabBuild.resumeWidgets();
      tabJpg.resumeWidgets();
      tabRgb.resumeWidgets();
   }

   public void close() {
      aladin.frameAllsky.close();
   }

   protected void clearForms() {
      Constante.SURVEY = Constante.ALLSKY;
      context.reset();
      tabDesc.clearForms();
      tabBuild.clearForms();
      tabJpg.clearForms();
      tabPub.clearForms();
   }

   protected void export(String path) {
      if( planPreview==null ) context.updateAllskyPreview();
      aladin.frameAllsky.export(planPreview, path);
   }


   public boolean hasJpg() {
      try {
         return cds.tools.Util.find(context.getOutputPath(),".jpg");
      } catch( Exception e ) {
         return false;
      }
   }

}

