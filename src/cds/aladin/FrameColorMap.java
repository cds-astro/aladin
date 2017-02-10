// Copyright 2010 - UDS/CNRS
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


package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.image.IndexColorModel;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import cds.tools.Util;


/**
 * Gestion de la fenetre permettant le changement dynamique de la
 * table des couleurs de l'image de base
 *
 * @author Pierre Fernique [CDS]
 * @author Anais Oberto [CDS]
 *                avril 2006: prise en compte des CM personnalisées (Thomas)
 * @version 2.0 : dec 2014: reprise complète du code
 * @version 1.6 : nov 2004: modif vrai pixel.
 * @version 1.5 : juillet 2004 Stern Special par Robin Wetzel
 * @version 1.4 : fév 2004: re-cut...
 * @version 1.3 : (juin 2002) Log(1+X) + help + corr bug memoControl
 * @version 1.2 : (dec 2001) Incorporation image RGB (Anais Oberto)
 * @version 1.1 : (15 juin 2000) Recuperation memoire libre
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class FrameColorMap extends JFrame implements MouseListener {

   static public final String CM[]      = { "gray", "BB", "Red","Green","Blue", "A","stern"  };
   static public final String CMA[]     = { "gray", "BB", "Red","Green","Blue", "A","stern" };

   // Les chaines statiques
   private String CMTITRE,CMREVERSE,CMLIMITS,CMERRORRANGE,CMRESET,CMLOCALCUT,CMCLOSE,
   CMONVIEW,CMAPPLYALL,CMAPPLYCCD,CMMETHODERGB/*,CMNOFULLPIXEL*/,CMMESSAGE,
   CMPIXTOOL,CMPREVIEW,CMFULLFITS,CMLOCALCUTTIP,CMMESSAGE1,CMMESSAGE2,CMAPPLYALLTIP,CMAPPLYCCDTIP,CMMETHOD,
   CMRESET1TIP,CMRESET2TIP,CMBACK,CMBACKTIP,CMNOCUT,CMNOCUTTIP,CMPREVIEWTIP,CMFULLFITSTIP,CMPIXTOOLTIP,CMONVIEWTIP;

   protected String CMCM,CMRANGE,CMRANGE1;

   // Les references
   private PlanImage pimg;          // L'image concernee
   private boolean isPlanImageRGB=false;
   private boolean isPlanBGRGB=false;
   private Vector<PlanImage> vimg;  // Liste des images concernées s'il y a lieu
   private Aladin aladin;

   // Les composantes
   private CanvasPixelRange canvasPixelRange=null; // Le graphique du Range des pixels de l'image
   protected CanvasColorMap cm=null;               // Le graphique de dynamique de l'image
   protected CanvasColorMap cm2=null;              // Le graphique de dynamique de l'image
   protected CanvasColorMap cm3=null;              // Le graphique de dynamique de l'image
   private JPanel p=null;                          // Le panel courant
   private JTextField pixelCutMinField;            // Champ de saisie pour le pixelCutMax
   private JTextField pixelCutMaxField;            // Champ de saisie pour le pixelCutMax
   private JComboBox<String> comboCM;              // Pour le choix de la colormap

   private JButton localCutButton, getAllButton, resetAllButton, resetDistribButton, resetCMButton, applyOnAll;
   private JComboBox<String> cmCombo, fctCombo;
   private JCheckBox reverseCb;
   private JRadioButton rPreview,rFull;

   private DefaultConf defaultConf = new DefaultConf();   // Gère les resets

   //Les valeurs a memoriser
   private int imgID=-1;             // Memorisation de l'etat de l'image (numero de version)

   protected void createChaine() {
      CMTITRE = aladin.chaine.getString("CMTITRE");
      CMREVERSE = aladin.chaine.getString("CMREVERSE");
      CMLIMITS = aladin.chaine.getString("CMLIMITS");
      CMERRORRANGE = aladin.chaine.getString("CMERRORRANGE");
      CMRESET = aladin.chaine.getString("CMRESET");
      CMLOCALCUT = aladin.chaine.getString("CMLOCALCUT");
      CMLOCALCUTTIP = aladin.chaine.getString("CMLOCALCUTTIP");
      CMNOCUT = aladin.chaine.getString("CMNOCUT");
      CMNOCUTTIP = aladin.chaine.getString("CMNOCUTTIP");
      CMCLOSE = aladin.chaine.getString("CMCLOSE");
      CMAPPLYALL = aladin.chaine.getString("CMAPPLYALL");
      CMAPPLYCCD = aladin.chaine.getString("CMAPPLYCCD");
      CMAPPLYALLTIP = aladin.chaine.getString("CMAPPLYALLTIP");
      CMAPPLYCCDTIP = aladin.chaine.getString("CMAPPLYCCDTIP");
      CMMETHODERGB = aladin.chaine.getString("CMMETHODERGB");
      //      CMNOFULLPIXEL = aladin.chaine.getString("CMNOFULLPIXEL");
      CMMESSAGE = aladin.chaine.getString("CMMESSAGE");
      CMMESSAGE1 = aladin.chaine.getString("CMMESSAGE1");
      CMMESSAGE2 = aladin.chaine.getString("CMMESSAGE2");
      CMPIXTOOL = aladin.chaine.getString("CMPIXTOOL");
      CMONVIEW = aladin.chaine.getString("CMONVIEW");
      CMPREVIEW = aladin.chaine.getString("CMPREVIEW");
      CMFULLFITS = aladin.chaine.getString("CMFULLFITS");
      CMMETHOD = aladin.chaine.getString("CMMETHOD");
      CMRESET1TIP = aladin.chaine.getString("CMRESET1TIP");
      CMRESET2TIP = aladin.chaine.getString("CMRESET2TIP");
      CMBACK = aladin.chaine.getString("CMBACK");
      CMBACKTIP = aladin.chaine.getString("CMBACKTIP");
      CMCM = aladin.chaine.getString("CMCM");
      CMRANGE = aladin.chaine.getString("CMRANGE");
      CMRANGE1 = aladin.chaine.getString("CMRANGE1");
      CMPREVIEWTIP = aladin.chaine.getString("CMPREVIEWTIP");
      CMFULLFITSTIP = aladin.chaine.getString("CMFULLFITSTIP");
      CMPIXTOOLTIP = aladin.chaine.getString("CMPIXTOOLTIP");
      CMONVIEWTIP = aladin.chaine.getString("CMONVIEWTIP");
   }

   /** Creation de l'objet sans pour autant remplir la frame */
   protected FrameColorMap(Aladin aladin) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(CMTITRE);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, true,aladin);
      getContentPane().setLayout( new BorderLayout(0,0)) ;
      setLocation(Aladin.computeLocation(this));
      addMouseListener(this);
   }

   /** Mise à jour des widgets en cas de modif via un script */
   void majCMByScript(PlanImage p) {
      if( p!=pimg || p instanceof PlanImageRGB
            || p instanceof PlanBG && ((PlanBG)pimg).isColored() ) return;
      pixelCutMinField.setText(p.getDataMinInfo());
      pixelCutMaxField.setText(p.getDataMaxInfo());
      comboCM.setSelectedIndex(p.typeCM);
   }

   /** Mise a jour de l'objet en fonction de l'image sélectionnée.
    * Mise a jour de la Frame en fonction du bouton pixel de la barre
    * des boutons. La mise a jour se fera sur l'image de base
    * <P>
    * Rq : on test le precedent etat de l'image courante afin de s'eviter
    *      un travail deja fait
    * Rq : on cache la fenetre s'il n'y a pas d'imageadequate
    */
   protected void majCM() { majCM(false); }
   protected void majCM(boolean force) {
      if( force ) pimg=null;
      if( aladin.toolBox.tool[ToolBox.HIST].mode==Tool.DOWN ) {
         memoControl();

         PlanImage p=aladin.calque.getFirstSelectedPlanImage();
         if( pimg!=null && pimg.selected && !p.isPixel() ) p =pimg;  // On ne change pas de plan s'il est encore sélectionné

         //         PlanImage p=(PlanImage)aladin.calque.getPlanBase();
         //         if( pimg!=null && pimg.selected ) p =pimg;  // On ne change pas de plan s'il est encore sélectionné

         //         ViewSimple v = aladin.view.getLastClickView();
         //         PlanImage p = v.pref!=null && v.pref.isPixel() ? (PlanImage)v.pref : null;

         if( p!=null && p.flagOk ) {

            // On a changé d'image ?
            if( this.p==null || pimg!=p ) {
               setTitle(CMTITRE+": "+p.label);
               initCM(p);
               showCM();

               // Nécessaire pour remettre à jour r[],g[],b[] static
               if( !isPlanImageRGB && !isPlanBGRGB )  cm.getCM();

            } else {
               int nImgID = pimg.getImgID();
               if( imgID==-1 || imgID!=nImgID ) {
                  cm.repaint();
                  if( cm2!=null ) cm2.repaint();
                  if( cm3!=null ) cm3.repaint();
                  imgID=nImgID;
               }
            }
            if( !isVisible() ) setVisible(true);
            return;
         }
      }
      if( isVisible() ) dispose();
   }

   /** Force le rechargement de l'histogramme dans le cas d'un blink */
   protected void blinkUpdate(Plan pref) {
      if( pimg!=pref ) return;
      pimg.resetHist();
      imgID= -1;
      if( isVisible() ) majCM();
   }

   /** Met à jour juste les objets constituants, mais pas l'interface.
    * Nécessaire dans le cas d'un ajustement du contraste directement
    * dans la vue via le bouton de droite
    * (voir ViewSimple.drawColorMap()) */
   protected void initCM(PlanImage pimg) {
      if( pimg==null ) return;
      this.pimg = pimg;

      isPlanImageRGB = pimg instanceof PlanImageRGB;
      isPlanBGRGB = pimg instanceof PlanBG && ((PlanBG)pimg).isColored();

      if( isPlanImageRGB ) {
         PlanImageRGB prgb = (PlanImageRGB)pimg;
         if( prgb.flagRed )   cm  = new CanvasColorMap(this,pimg,0);
         if( prgb.flagGreen ) cm2 = new CanvasColorMap(this,pimg,1);
         if( prgb.flagBlue )  cm3 = new CanvasColorMap(this,pimg,2);
         cm.setOtherColorMap(cm2, cm3);
         if( cm2!=null ) cm2.setOtherColorMap(cm, cm3);
         if( cm3!=null ) cm3.setOtherColorMap(cm, cm2);

      } else if( isPlanBGRGB ) {
         cm  = new CanvasColorMap(this,pimg,0);
         cm2 = new CanvasColorMap(this,pimg,1);
         cm3 = new CanvasColorMap(this,pimg,2);
         cm.setOtherColorMap(cm2, cm3);
         cm2.setOtherColorMap(cm, cm3);
         cm3.setOtherColorMap(cm, cm2);

      } else {
         canvasPixelRange = new CanvasPixelRange(this,pimg);
         cm = new CanvasColorMap(this,pimg);
      }
   }

   // Change le mode d'affichage d'un Plan HiPS (preview <-> full dynamic)
   private void switchMode(PlanBG pbg) {
      pbg.switchFormat();
      showCM();
      aladin.view.repaintAll();
   }

   /** Construction du panel en fonction de l'image courante */
   protected void showCM() {
      if( isPlanImageRGB || isPlanBGRGB  ) showCMRGB();
      else showCMGrey();
      resumeWidgets();
   }

   // Gère les différents RESETs en mémorisant l'état initial
   class DefaultConf {
      double rawPixelCutMin,rawPixelCutMax; // plade des pixels visibles
      int cmIndex;    // Index de la table des couleurs mémorisée
      int fctIndex;   // Index de la fonction de transfert mémorisé
      int videoIndex; // Mode vidéo mémorisé

      long pimgHash;  // Hash de l'image (permet de repérer un changement d'image

      // Mémorise si nécessaire les information de l'image courante
      boolean memo() {
         if( pimg.hashCode() == pimgHash ) return false;   // on est toujours sur le même objets
         pimgHash = pimg.hashCode();
         rawPixelCutMin = pimg.pixelMin;
         rawPixelCutMax = pimg.pixelMax;
         cmIndex        = pimg.typeCM;
         fctIndex       = pimg.getTransfertFct();
         videoIndex     = pimg.video;
         return true;
      }

      // reset si nécessaire de l'ensemble
      void resetAll() { resetMinMax(); resetCM(); resetTriangle(); }

      // Une modif a été opérée
      boolean isModif() {
         return isModifCM() || isModifTriangle() || isModifMinMax();
      }

      // Une modif sur la table des couleur a été opérée
      boolean isModifCM() {
         return pimg.getTransfertFct()!=fctIndex || pimg.typeCM!=cmIndex || pimg.video!=videoIndex;
      }

      // reset si nécessaire de la table des couleurs
      void resetCM() {
         if( !isModifCM() ) return;
         pimg.setTransfertFct(fctIndex);
         pimg.typeCM = cmIndex;
         pimg.video = videoIndex;
         setCM( cm.getCM() );
      }

      // Une modif sur les triangles d'ajustement linéaire a été opérée
      boolean isModifTriangle() {
         return 0!=cm.triangle[0] || 128!=cm.triangle[1] || 255!=cm.triangle[2];
      }

      // reset si nécessaire des triangles d'ajustement linéaire de la table des couleurs
      void resetTriangle() {
         if( !isModifTriangle() ) return;
         cm.reset();
         setCM( cm.getCM() );
      }

      // Une modif sur le cut min ou max a été opérée
      boolean isModifMinMax() {
         return rawPixelCutMin!=pimg.pixelMin || rawPixelCutMax!=pimg.pixelMax;
      }

      // reset si nécessaire des cut min et max
      void resetMinMax() {
         if( !isModifMinMax() ) return;
         pimg.recut(rawPixelCutMin,rawPixelCutMax,false);
         setCM(cm.getCM());
         pixelCutMinField.setText(pimg.getPixelMinInfo());
         pixelCutMaxField.setText(pimg.getPixelMaxInfo());
      }
   }


   // réinitialisation de tous les widgets du panel
   protected void resumeWidgets() {
      int cmIndex = pimg.typeCM;
      int fctIndex =  pimg.getTransfertFct();
      int videoIndex = pimg.video;
      boolean fullPixel = pimg.hasAvailablePixels();
      boolean hasPreview = (pimg instanceof PlanBG) && ( ((PlanBG)pimg).inJPEG || ((PlanBG)pimg).inPNG) || !fullPixel;
      boolean hasFull    = (pimg instanceof PlanBG) && ((PlanBG)pimg).inFits || fullPixel;
      boolean hasAll = pimg.pixelMin==pimg.dataMin && pimg.pixelMax==pimg.dataMax;
      boolean hasSeveralImg = pimg.aladin.calque.getSelectedImagesWithPixels().size()>=2;

      if( localCutButton!=null ) localCutButton.setEnabled( fullPixel );
      if( getAllButton!=null )   getAllButton.setEnabled( fullPixel && !hasAll );
      if( applyOnAll!=null )     applyOnAll.setEnabled( hasSeveralImg && fullPixel || pimg.planMultiCCD!=null );
      
      if( applyOnAll!=null ) {
         applyOnAll.setText(pimg.planMultiCCD!=null ? CMAPPLYCCD : CMAPPLYALL);
         Util.toolTip(applyOnAll, pimg.planMultiCCD!=null ? CMAPPLYCCDTIP : CMAPPLYALLTIP, true);
      }

      if( rFull!=null ) {
         rFull.setSelected( fullPixel );
         rFull.setEnabled( hasFull );
      }
      if( rPreview!=null ) {
         rPreview.setSelected( !fullPixel );
         rPreview.setEnabled( hasPreview );
      }

      if( resetAllButton!=null )     resetAllButton.setEnabled( defaultConf.isModif() );
      if( resetDistribButton!=null ) resetDistribButton.setEnabled( defaultConf.isModifTriangle());
      if( resetCMButton!=null )      resetCMButton.setEnabled( defaultConf.isModifCM() );

      if( reverseCb!=null ) reverseCb.setSelected( videoIndex==PlanImage.VIDEO_INVERSE );

      if( cmCombo!=null )  cmCombo.setSelectedIndex( cmIndex );
      if( fctCombo!=null ) fctCombo.setSelectedIndex( fctIndex );

      if( pixelCutMinField!=null ) {
         pixelCutMinField.setText(pimg.getPixelMinInfo());
         pixelCutMinField.setEnabled( fullPixel );
      }
      if( pixelCutMaxField!=null ) {
         pixelCutMaxField.setText(pimg.getPixelMaxInfo());
         pixelCutMaxField.setEnabled( fullPixel );
      }

      if( aladin.frameAllsky!=null ) aladin.frameAllsky.updateCurrentCM();
      cm.resumePixelTool();

      if( canvasPixelRange!=null ) canvasPixelRange.repaint();
      cm.repaint();
      pimg.aladin.calque.repaintAll();

      defaultConf.memo();
   }

   private JPanel createPanelTop() {
      JButton b;
      JRadioButton r;
      ButtonGroup gr = new ButtonGroup();

      JPanel p = new JPanel( new BorderLayout(0,0) );

      // Le panel du choix Preview / Full dynamic
      JPanel pLeft = new JPanel( new FlowLayout() );
      pLeft.add( rPreview=r=new JRadioButton(CMPREVIEW) );    gr.add(r);
      Util.toolTip(r, CMPREVIEWTIP, true);
      r.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { switchMode( (PlanBG)pimg ); }
      } );
      pLeft.add( rFull=r=new JRadioButton(CMFULLFITS) );  gr.add(r);
      Util.toolTip(r, CMFULLFITSTIP, true);
      r.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { switchMode( (PlanBG)pimg); }
      } );
      p.add(pLeft, BorderLayout.WEST);

      // Le panel des Boutons Localcut, getAll, reset
      JPanel pRight = new JPanel( new FlowLayout() );

      localCutButton=b=getButton(CMLOCALCUT);
      Util.toolTip(b, CMLOCALCUTTIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { localcut(); resumeWidgets(); }
      });
      pRight.add( b );

      getAllButton=b=getButton(CMNOCUT);
      Util.toolTip(b, CMNOCUTTIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { getAll(); resumeWidgets(); }
      });
      pRight.add( b );

      resetAllButton=b=getButton(CMBACK);
      Util.toolTip(b, CMBACKTIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { defaultConf.resetAll(); resumeWidgets(); }
      });
      pRight.add( b );

      pRight.add(Util.getHelpButton(this,CMMESSAGE));

      p.add(pRight, BorderLayout.EAST);

      return p;
   }

   private JPanel createPanelDistribution() {

      JPanel p1 = new JPanel( new BorderLayout(0,2));
      p1.add( createPanelPixelCut(), BorderLayout.NORTH);
      p1.add( cm, BorderLayout.CENTER );
      p1.add( createPanelClose(), BorderLayout.SOUTH );

      JButton b;
      JPanel pHelp = new JPanel( new GridLayout(2,1,0,140) );
      b=Util.getHelpButton(this,CMMESSAGE1);
      pHelp.add( b );
      b=Util.getHelpButton(this,CMMESSAGE2);
      pHelp.add( b );

      JPanel p = new JPanel( new FlowLayout());
      p.add( pHelp );
      p.add( p1);
      p.add( createPanelDistributionButton());
      return p;
   }

   private JPanel createPanelDistributionButton() {
      GridBagLayout g = new GridBagLayout();
      JPanel p = new JPanel( g );

      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.NONE;
      c.anchor = GridBagConstraints.CENTER;
      c.insets = new Insets(2,2,2,2);

      JButton b;
      JLabel l;
      JComboBox<String> cb;
      JCheckBox cc;

      l = new JLabel( Util.fold(CMMETHOD,30,true) );
      l.setForeground( Aladin.MYBLUE );
      c.insets.top = c.insets.bottom = 10;
      c.gridwidth=2; c.gridx=0; c.gridy++;
      g.setConstraints(l, c);
      p.add(l);

      resetDistribButton=b=getButton(CMRESET);
      Util.toolTip(b, CMRESET1TIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { defaultConf.resetTriangle(); resumeWidgets(); }
      });
      c.gridwidth=1; c.insets.top=2; c.insets.bottom=2;
      c.gridy++;
      g.setConstraints(b, c);
      p.add(b);

      reverseCb=cc=new JCheckBox(CMREVERSE);
      cc.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { reverse(); resumeWidgets(); }
      });
      c.gridwidth=2; c.insets.top=25; c.gridy++; c.anchor=GridBagConstraints.LINE_START;
      g.setConstraints(cc, c);
      p.add(cc);

      JPanel p1 = new JPanel( new GridLayout(1,2,1,1) );
      cmCombo=cb = createComboCM();
      cb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeCM((JComboBox)e.getSource() ); resumeWidgets(); }
      });
      p1.add(cb);
      fctCombo=cb = createComboFct();
      cb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { changeTransfertFct(); resumeWidgets(); }
      });
      p1.add(cb);
      c.insets.top=2;
      c.gridy++; c.anchor=GridBagConstraints.CENTER;
      g.setConstraints(p1, c);
      p.add(p1);

      resetCMButton=b=getButton(CMRESET);
      Util.toolTip(b, CMRESET2TIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { defaultConf.resetCM(); resumeWidgets(); }
      });
      c.gridwidth=1; c.gridy++;
      g.setConstraints(b, c);
      p.add(b);

      b=getButton(CMONVIEW);
      Util.toolTip(b, CMONVIEWTIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            aladin.view.showRainbow(true);
            aladin.view.repaintAll();
         }
      });
      c.gridx++; c.anchor=GridBagConstraints.LINE_END;
      g.setConstraints(b, c);
      p.add(b);

      return p;
   }

   private JPanel createPanelPixelCut() {
      JPanel p = new JPanel( new BorderLayout(0,0));
      p.setBorder( BorderFactory.createEmptyBorder());

      pixelCutMinField=new JTextField( pimg.getPixelMinInfo() ,8);
      pixelCutMinField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) { if( e.getKeyCode()==KeyEvent.VK_ENTER ) changeCut(); }
      });
      p.add(pixelCutMinField,BorderLayout.WEST);
      pixelCutMaxField=new JTextField( pimg.getPixelMaxInfo() ,8);
      pixelCutMaxField.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) { if(  e.getKeyCode()==KeyEvent.VK_ENTER ) changeCut(); }
      });
      p.add(pixelCutMaxField,BorderLayout.EAST);

      // Le titre
      JPanel p1 = new JPanel();
      JLabel titre= new JLabel(CMLIMITS);
      titre.setForeground( Aladin.MYBLUE );
      p1.add(titre);
      p.add(p1,BorderLayout.CENTER);

      return p;
   }


   private JPanel createPanelClose() {
      JButton b;

      JPanel p2 = new JPanel(/* new FlowLayout(FlowLayout.RIGHT)*/);
      applyOnAll=b = getButton(CMAPPLYALL);
      Util.toolTip(b, CMAPPLYALLTIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { applyOnOtherImg(); resumeWidgets(); }
      });
      p2.add( b );

      b=getButton(CMPIXTOOL);
      Util.toolTip(b,CMPIXTOOLTIP, true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { aladin.pixelTool(); }
      });
      p2.add(b);

      b = getButton( CMCLOSE );
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { dispose(); }
      });
      p2.add( b );
      return p2;
   }


   // le Panel associé à une image en niveaux de gris
   private void showCMGrey() {
      if( p!=null ) getContentPane().remove(p);

      JPanel p1 = new JPanel( new BorderLayout(0,0) );
      p1.add( createPanelTop(), BorderLayout.NORTH);
      p1.add( canvasPixelRange, BorderLayout.CENTER);

      p = new JPanel( new BorderLayout(0,0) );
      p.add( p1, BorderLayout.NORTH );
      p.add( createPanelDistribution(), BorderLayout.CENTER );

      getContentPane().add(p,BorderLayout.CENTER);
      pack();
      setVisible(true);
      toFront();
   }

   // Le Panel associé à une image couleur
   private void showCMRGB() {
      JButton b;

      // J'enleve toute les precedentes composantes
      if( p!=null ) getContentPane().remove(p);

      p = new JPanel();
      GridBagLayout g =  new GridBagLayout();
      p.setLayout(g);
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.CENTER;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.insets = new Insets(0,0,0,0);


      // Indication pour remplir le formulaire
      JPanel p1 = new JPanel();
      JLabel methode = new JLabel(Util.fold(CMMETHODERGB,80,true));
      methode.setFont(methode.getFont().deriveFont(Font.ITALIC));
      p1.add(methode);

      p1.add(Util.getHelpButton(this,CMMESSAGE));
      g.setConstraints(p1,c);
      p.add(p1);

      if( isPlanBGRGB ) {
         g.setConstraints(cm,c);
         p.add(cm);
         g.setConstraints(cm2,c);
         p.add(cm2);
         g.setConstraints(cm3,c);
         p.add(cm3);

      } else {

         if( ((PlanImageRGB)pimg).flagRed ) {
            g.setConstraints(cm,c);
            p.add(cm);
         }
         if( ((PlanImageRGB)pimg).flagGreen ) {
            g.setConstraints(cm2,c);
            p.add(cm2);
         }
         if( ((PlanImageRGB)pimg).flagBlue ) {
            g.setConstraints(cm3,c);
            p.add(cm3);
         }
      }

      JPanel validation = new JPanel();
      b=getButton(CMREVERSE);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { reverse(); resumeWidgets(); }
      });
      validation.add(b);

      validation.add(new JLabel("  "));
      b=getButton(CMRESET);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { resetRGB(); resumeWidgets(); }
      });
      validation.add(b);

      b=getButton(CMCLOSE);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { dispose(); }
      });
      validation.add(b);

      g.setConstraints(validation,c);
      p.add(validation);
      getContentPane().add(p,BorderLayout.CENTER);

      pack();
      setVisible(true);
      toFront();
   }

   private void resetRGB() {
      if( isPlanBGRGB ) {
         ((PlanBG)pimg).filterRGB(new int[]{0, 128,255}, 0);
         ((PlanBG)pimg).filterRGB(new int[]{0, 128,255}, 1);
         ((PlanBG)pimg).filterRGB(new int[]{0, 128,255}, 2);
         cm.reset();
         cm2.reset();
         cm3.reset();
      } else {
         if( ((PlanImageRGB)pimg).flagRed )   cm.reset();
         if( ((PlanImageRGB)pimg).flagGreen ) cm2.reset();
         if( ((PlanImageRGB)pimg).flagBlue )  cm3.reset();
         ((PlanImageRGB)pimg).createImgRGB();
      }
   }

   static private Insets MARGIN = new Insets(1, 3, 1, 3);

   // Constructeur d'un bouton avec une petite marge
   private JButton getButton(String label) {
      JButton b=new JButton(label);
      b.setMargin(MARGIN);
      return b;
   }

   public Dimension getMinimumSize() { return new Dimension(200,300); }

   /** Création d'un Combo des CM possibles */
   protected static JComboBox<String> createComboCM() {
      JComboBox<String> c = new JComboBox<String>();
      for( String s : CanvasColorMap.getCMList() ) c.addItem(s);
      c.addItem(" -- ");
      return c;
   }

   /** Création d'un Combo des Fonctions de transferts possibles */
   protected static JComboBox<String> createComboFct() {
      JComboBox<String> c = new JComboBox<String>( PlanImage.TRANSFERTFCT );
      return c;
   }


   /** Retourne true si on est en train de draguer les triangles de la colormap
    * Voir ViewSimple.getImage() */
   final protected boolean isDragging() { return cm==null ? false : cm.isDragging(); };

   // Exécution d'un cut localisé autour du réticule
   private void localcut() {
      localcut(pimg);
      resumeWidgets();
   }
   public void localcut(PlanImage pimg) {
      if( pimg instanceof PlanBG ) ((PlanBG)pimg).forceReload();
      else pimg.recut(0, 0, true);
      aladin.view.repaintAll();
   }

   // Exécution d'une inversion de pixels
   private void reverse() {
      if( pimg.video==PlanImage.VIDEO_NORMAL ) pimg.video=PlanImage.VIDEO_INVERSE;
      else pimg.video=PlanImage.VIDEO_NORMAL;

      aladin.console.printCommand("cm "+(pimg.video==PlanImage.VIDEO_NORMAL?"noreverse":"reverse"));
      if( pimg.type==Plan.IMAGERGB ) {
         if( ((PlanImageRGB)pimg).flagRed )    { cm.pimg.video=pimg.video;  cm.repaint(); }
         if( ((PlanImageRGB)pimg).flagGreen )  { cm2.pimg.video=pimg.video; cm2.repaint(); }
         if( ((PlanImageRGB)pimg).flagBlue )   { cm3.pimg.video=pimg.video; cm3.repaint(); }

         ((PlanImageRGB)pimg).inverseRGB();
      }
      else {
         cm.repaint();
         setCM(cm.getCM());
      }
      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();
   }

   // Exécution de la récupération de la totalité de la dynamique
   private void getAll() {
      if( pimg==null ) return;
      Aladin.makeCursor(this, Aladin.WAITCURSOR);
      pixelCutMinField.setText(pimg.getDataMinInfo());
      pixelCutMaxField.setText(pimg.getDataMaxInfo());
      Aladin.makeCursor(this, Aladin.WAITCURSOR);
      if( !pimg.recut(pimg.dataMin,pimg.dataMax,false) ) {
         Aladin.makeCursor(this, Aladin.DEFAULTCURSOR);
         return;
      }
      Aladin.makeCursor(this, Aladin.DEFAULTCURSOR);
      aladin.console.printCommand("cm all");
      cm.repaint();
      canvasPixelRange.repaint();
      setCM(cm.getCM());

      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();
   }

   /** Changement de la fonction de transfert */
   private void changeTransfertFct() {
      int n = fctCombo.getSelectedIndex();

      if( pimg.getTransfertFct()==n ) return; // déjà fait
      aladin.console.printCommand("cm "+PlanImage.TRANSFERTFCT[n]);

      pimg.setTransfertFct(n);
      setCM(cm.getCM());

      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();
      repaint();

   }

   // Application des choix courants sur toutes les autres images sélectionnées
   private void applyOnOtherImg() {
      
      // S'il s'agit d'un plan faisant parti d'un multiCCD, on applique sur tous les autres CCD
      if( pimg.planMultiCCD!=null ) vimg = pimg.planMultiCCD.getCCD();
      
      // sinon, on applique sur les autres plans sélectionnés
      else vimg = aladin.calque.getSelectedImagesWithPixels();
      
      // On ne travaile pas sur le plan lui-même
      vimg.remove(pimg);

      final String mins = pixelCutMinField.getText();
      final String maxs = pixelCutMaxField.getText();
      final int video = pimg.video;
      final int typeCM = pimg.typeCM;
      final int indexFct = pimg.getTransfertFct();
      final int [] tr = new int[3];
      for( int i=0; i<3; i++ ) tr[i] = pimg.cmControl[i];

      //      Aladin.makeCursor(this, Aladin.WAITCURSOR);
      (new Thread("changeAutocut") {
         @Override
         public void run() {
            Enumeration<PlanImage> e = vimg.elements();
            while( e.hasMoreElements() ) {
               PlanImage pi = e.nextElement();
               double min = pi.getPixelValue(mins);
               double max = pi.getPixelValue(maxs);
               pi.recut(min,max,false);
               pi.setTransfertFct( indexFct );
               pi.video = video;
               pi.typeCM = typeCM;
               pi.setCM(cm.getCM( tr[0],tr[1],tr[2], video==PlanImage.VIDEO_INVERSE, typeCM, indexFct,pi.isTransparent()));
               for( int i=0; i<3; i++ ) pi.cmControl[i]=tr[i];
               aladin.view.repaintAll();
            }
            vimg=null;
         }
      }).start();
      //      Aladin.makeCursor(this, Aladin.DEFAULTCURSOR);

   }

   /** Force le cut pour des valeurs particulières à partir CanvasPixelRange */
   protected void setMinMax(double min, double max, boolean execute) {
      pixelCutMinField.setText( Util.myRound(min) );
      pixelCutMaxField.setText( Util.myRound(max) );
      if( execute ) {
         changeCut();
         resumeWidgets();
      }
   }


   /** Gestion du bouton REPLAY */
   private void changeCut() {
      try {
         double min = pimg.getPixelValue(pixelCutMinField.getText());
         double max = pimg.getPixelValue(pixelCutMaxField.getText());
         if( min>max ) {
            Aladin.warning(this,CMERRORRANGE);
            return;
         }
         aladin.console.printCommand("cm "+pixelCutMinField.getText()+".."+pixelCutMaxField.getText() );

         if( !pimg.recut(min,max,false) ) {
            return;
         }

         pimg.resetHist();
         setCM(cm.getCM());
         pixelCutMinField.setText(pimg.getPixelMinInfo());
         pixelCutMaxField.setText(pimg.getPixelMaxInfo());
         canvasPixelRange.repaint();
         aladin.view.repaintAll();
         aladin.calque.zoom.zoomView.repaint();

      }
      catch( Exception e) { e.printStackTrace(); }
   }

   /** Changement de la color map */
   private void changeCM(JComboBox<String> c) {
      int n = c.getSelectedIndex();
      if( n==pimg.typeCM ) return;
      pimg.typeCM = n;
      cm.repaint();
      if( n<CMA.length ) aladin.console.printCommand("cm "+CMA[n]);
      else {
         try { aladin.console.printCommand("cm "+CanvasColorMap.customCMName.get(n-CanvasColorMap.LAST_DEFAULT_CM_IDX-1)); }
         catch( Exception e ) {}
      }
      try { setCM(cm.getCM()); }
      catch( Exception e ) {}
      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();

   }

   /** Mise a jour par rapport a la nouvelle table des couleurs
    * @param ic la nouvelle CM
    */
   private void setCM(IndexColorModel ic) {
      pimg.setCM(ic);
      pimg.aladin.calque.zoom.zoomView.setCM(ic);
   }

   /** Memorisation des infos de controle de la tables des couleurs */
   private void memoControl() {
      if( pimg==null || cm==null ) return;

      // Memorisation du control de la table des couleurs
      if( isPlanImageRGB ) {
         for( int i=0; i<3; i++ ) {
            PlanImageRGB pRGB = (PlanImageRGB)pimg;
            if( pRGB.flagRed ) pRGB.RGBControl[i]=cm.triangle[i];
            if( pRGB.flagGreen ) pRGB.RGBControl[3+i]=cm2.triangle[i];
            if( pRGB.flagBlue ) pRGB.RGBControl[6+i]=cm3.triangle[i];
         }
      } else if( isPlanBGRGB ) {
         for( int i=0; i<3; i++ ) {
            PlanBG pbg = (PlanBG)pimg;
            pbg.RGBControl[i]=cm.triangle[i];
            pbg.RGBControl[3+i]=cm2.triangle[i];
            pbg.RGBControl[6+i]=cm3.triangle[i];
         }
      } else {
         for( int i=0; i<3; i++ ) pimg.cmControl[i]=cm.triangle[i];
      }

   }


   /** Libère la frameCM s'il s'agit du même plan. Appelé lorsque le plan
    * va être libéré (inutile de mémoriser...)
    * @param p
    */
   protected void disposeFrameCM(Plan p) {
      if( p!=pimg ) return;
      pimg=null;
      imgID=-1;
      if( isVisible() ) {
         aladin.toolBox.tool[ToolBox.HIST].setMode(Tool.UP);
         aladin.toolBox.repaint();
         setVisible(false);
      }
   }

   /** Fermeture de la Frame.
    * Cache la fenetre et remonte le bouton
    * des properties
    */
   @Override
   public void dispose() {
      setVisible(false);
      aladin.gc();
      memoControl();
      if( aladin.calque.getPlanBase()!=null )  aladin.toolBox.tool[ToolBox.HIST].setMode(Tool.UP);
      else aladin.toolBox.tool[ToolBox.HIST].setMode(Tool.UNAVAIL);
      aladin.toolBox.repaint();
      imgID=-1;
   }

   /** Trap sur l'evenement WINDOW_DESTROY    */
   @Override
   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) dispose();
      super.processWindowEvent(e);
   }

   @Override
   public void mouseClicked(MouseEvent e) {
      // TODO Auto-generated method stub

   }

   @Override
   public void mousePressed(MouseEvent e) {
      // TODO Auto-generated method stub

   }

   @Override
   public void mouseReleased(MouseEvent e) {
      // TODO Auto-generated method stub

   }

   @Override
   public void mouseEntered(MouseEvent e) {
      if( ignoreMouse(e) ) return;
      resumeWidgets();
   }

   @Override
   public void mouseExited(MouseEvent e) {
      if( ignoreMouse(e) ) return;
      if( pixelCutMinField!=null && pixelCutMaxField!=null ) {
         String mins = pixelCutMinField.getText();
         String maxs = pixelCutMaxField.getText();
         if( !mins.equals(pimg.getPixelMinInfo()) || !maxs.equals(pimg.getPixelMaxInfo()) ) resumeWidgets();
      } else resumeWidgets();
   }

   private boolean ignoreMouse(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      return !( x<10 || y<30 || x>getWidth()-10 || y>getHeight()-10 );
   }
   
}

