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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;

import cds.aladin.prop.Filet;
import cds.tools.Util;


/**
 * Gestion de la fenetre permettant le changement dynamique de la
 * table des couleurs de l'image de base
 *
 * @author Pierre Fernique [CDS]
 * @author Anais Oberto [CDS]
 *                avril 2006: prise en compte des CM personnalisées (Thomas)
 * @version 1.6 : nov 2004: modif vrai pixel.
 * @version 1.5 : juillet 2004 Stern Special par Robin Wetzel
 * @version 1.4 : fév 2004: re-cut...
 * @version 1.3 : (juin 2002) Log(1+X) + help + corr bug memoControl
 * @version 1.2 : (dec 2001) Incorporation image RGB (Anais Oberto)
 * @version 1.1 : (15 juin 2000) Recuperation memoire libre
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class FrameCM extends JFrame implements ActionListener {

   static final String CM[]      = { "gray", "BB", "A","stern" };
   static final String CMA[]     = { "gray", "BB", "A","stern" };

   // Les chaines statiques
   String TITRE,REVERSE,RANGE,LIMITS,ALGO,ERRORRANGE,RESET,CLOSE,HELP,
          NOCUT,REPLAY,ALL,METHODE,METHODEX,METHODERGB,NOFULLPIXEL,MESSAGE,CMM,CMCO;

   // Les references
   PlanImage pimg;          // L'image concernee
   Vector<PlanImage> vimg;             // s'il y a plusieurs images grey simultanément
   Aladin aladin;

   // Les composantes
   ColorMap cm=null;        // Le graphique de dynamique de l'image
   ColorMap cm2=null;       // Le graphique de dynamique de l'image
   ColorMap cm3=null;       // Le graphique de dynamique de l'image
   JPanel p=null;           // Le panel courant
   JPanel cmPanel=null;
   JTextField minCut,maxCut;
   JLabel labelOriginalPixel;
   JCheckBox autocutBox;     // Pour rejouer l'autocut avec l'algo Aladin
   ButtonGroup transfertCBG;    // Pour indiquer la fonction de transfert
   JRadioButton[] transfertCB;  // Pour indiquer la fonction de transfert
   JComboBox choiceCM;          // Pour le choix de la colormap

   //Les valeurs a memoriser
   int imgID=-1;             // Memorisation de l'etat de l'image (numero de version)

   protected void createChaine() {
      TITRE = aladin.chaine.getString("CMTITRE");
      REVERSE = aladin.chaine.getString("CMREVERSE");
      RANGE = aladin.chaine.getString("CMRANGE");
      LIMITS = aladin.chaine.getString("CMLIMITS");
      ALGO = aladin.chaine.getString("CMALGO");
      ERRORRANGE = aladin.chaine.getString("CMERRORRANGE");
      RESET = aladin.chaine.getString("CMRESET");
      CLOSE = aladin.chaine.getString("CMCLOSE");
      HELP = aladin.chaine.getString("CMHELP");
      NOCUT = aladin.chaine.getString("CMNOCUT");
      CMM = aladin.chaine.getString("UPCMM");
      CMCO = aladin.chaine.getString("UPCMCO");
      REPLAY = aladin.chaine.getString("CMREPLAY");
      ALL = aladin.chaine.getString("CMALL");
      METHODE = aladin.chaine.getString("CMMETHODE");
      METHODEX = aladin.chaine.getString("CMMETHODEX");
      METHODERGB = aladin.chaine.getString("CMMETHODERGB");
      NOFULLPIXEL = aladin.chaine.getString("CMNOFULLPIXEL");
      MESSAGE = aladin.chaine.getString("CMMESSAGE");
   }

  /** Creation de l'objet sans pour autant remplir la frame */
   protected FrameCM(Aladin aladin) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(TITRE);

      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, true,aladin);
      
      getContentPane().setLayout( new BorderLayout()) ;

//// Pour contourner le bug sous Linux/KDE/
//super.show();
      setLocation(Aladin.computeLocation(this));
//      setBackground(Aladin.BKGD);
//super.hide();
   }

   /** Mise à jour des widgets en cas de modif via un script */
   void majCMByScript(PlanImage p) {
      if( p!=pimg || p instanceof PlanImageRGB ) return;

      if( transfertCBG!=null ) {
         transfertCBG.setSelected(transfertCB[p.getTransfertFct()].getModel(), true);
      }
      minCut.setText(p.getDataMinInfo());
      maxCut.setText(p.getDataMaxInfo());
      choiceCM.setSelectedIndex(p.typeCM);
   }

   private boolean theSame(Vector vimg, Vector v) {
      if( vimg==null && v!=null || vimg.size()!=v.size() ) return false;
      Enumeration e1 = vimg.elements();
      Enumeration e2 = v.elements();
      while( e1.hasMoreElements() ) if( e1.nextElement()!=e2.nextElement() ) return false;
      return true;
   }

   private void majCMX(Vector v) {
      if( theSame(vimg,v) ) return;
      vimg = v;
      cm = new ColorMap((PlanImage)v.elementAt(0),-1);  // Juste pour qu'il y ait au-moins une c initialisée
      showCMX();
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
      if( aladin.toolbox.tool[ToolBox.HIST].mode==Tool.DOWN ) {
         boolean flagRGB = pimg instanceof PlanImageRGB;
         memoControl();

         // Peut être y a-t-il plusieurs plans images simultanément
         Vector v = aladin.calque.getSelectedSimpleImage();
         if( v.size()>1 ) {
            pimg=null;
            majCMX(v);
            return;
         }
         vimg=null;

         // Sinon on prend juste la première image sélectionné
         PlanImage p=aladin.calque.getFirstSelectedPlanImage();
         if( p instanceof PlanBG && ((PlanBG)p).color ) p=null;

         if( p!=null && p.flagOk ) {

            // On a changé d'image ?
            if( this.p==null || pimg!=p ) {
               initCM(p);
               showCM();
               if( !flagRGB ) cm.getCM(); // Nécessaire pour remettre à jour r[],g[],b[] static

            } else {

               // L'image a changé d'état
               int nImgID = p.getImgID();
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
      pimg.histOk(false);
      imgID= -1;
      if( isVisible() ) majCM();
   }


   /** Met à jour juste les objets constituants, mais pas l'interface.
    * Nécessaire dans le cas d'un ajustement du contraste directement
    * dans la vue via le bouton de droite
    * (voir ViewSimple.drawColorMap()) */
   protected void initCM(PlanImage pimg) {
      boolean isPlanRGB = pimg!=null && pimg instanceof PlanImageRGB;
      this.pimg = pimg;
      if( isPlanRGB ) {
         if( ((PlanImageRGB)pimg).flagRed ) {
            cm = new ColorMap(pimg,0);
         }
         if( ((PlanImageRGB)pimg).flagGreen ) {
            cm2 = new ColorMap(pimg,1);
         }
         if( ((PlanImageRGB)pimg).flagBlue ) {
            cm3 = new ColorMap(pimg,2);
         }
         cm.setOtherColorMap(cm2, cm3);
         if( cm2!=null ) cm2.setOtherColorMap(cm, cm3);
         if( cm3!=null ) cm3.setOtherColorMap(cm, cm2);

      } else {
         cm = new ColorMap(pimg);
      }
   }

   private boolean isMultiImg() { return vimg!=null; }

   private void selectMultiImg() { }

   private int getTransfertFct() {
      if( isMultiImg() ) {
         int tfct=-1;
         Enumeration e = vimg.elements();
         while( e.hasMoreElements() ) {
            PlanImage pi = (PlanImage)e.nextElement();
            int i = pi.getTransfertFct();
            if( tfct!=-1 && i!=tfct ) return PlanImage.MULTFCT;
            tfct = i;
         }
         return tfct;
      }
      return pimg.getTransfertFct();
   }

   private int getTypeCM() {
      if( isMultiImg() ) {
         int typeCM=-1;
         Enumeration e = vimg.elements();
         while( e.hasMoreElements() ) {
            PlanImage pi = (PlanImage)e.nextElement();
            int i = pi.typeCM;
            if( typeCM!=-1 && i!=typeCM ) return choiceCM.getItemCount()-1;
            typeCM = i;
         }
         return typeCM;
      }
      return pimg.typeCM;
   }

   private void setTypeCM(int n) {
      if( isMultiImg() ) {
         Enumeration e = vimg.elements();
         while( e.hasMoreElements() ) {
            PlanImage pi = (PlanImage)e.nextElement();
            pi.typeCM=n;
         }
      } else pimg.typeCM=n;
   }

   private void setTransfertFct(int n) {
      if( isMultiImg() ) {
         Enumeration e = vimg.elements();
         while( e.hasMoreElements() ) {
            PlanImage pi = (PlanImage)e.nextElement();
            pi.setTransfertFct(n);
         }
      } else pimg.setTransfertFct(n);
   }

   private String getMinPix() { return getPix1(0); }
   private String getMaxPix() { return getPix1(1); }
   private String getMinPixCut() { return getPix1(2); }
   private String getMaxPixCut() { return getPix1(3); }

   private String getPix1(int mode) {
      if( isMultiImg() ) {
         String val=null;
         Enumeration<PlanImage> e = vimg.elements();
         while( e.hasMoreElements() ) {
            PlanImage pi = e.nextElement();
            String s=null;
            switch( mode ) {
               case 0: s=pi.getDataMinInfo(); break;
               case 1: s=pi.getDataMaxInfo(); break;
               case 2: s=pi.getPixelMinInfo(); break;
               default: s=pi.getPixelMaxInfo();
            }
            if( val==null ) val=s;
            else {
               double val1 = Double.parseDouble(val);
               double s1 = Double.parseDouble(s);
               if( mode==0 || mode==2 ) { if( s1<val1 ) val=s; }
               else if( s1>val1 ) val=s;
            }
         }
         return val;
      }
      switch( mode ) {
         case 0: return pimg.getDataMinInfo();
         case 1: return pimg.getDataMaxInfo();
         case 2: return pimg.getPixelMinInfo();
         default: return pimg.getPixelMaxInfo();
      }
   }

   /** Construction du panel en fonction de l'image en parametre.
    * @param pimg l'image courante
    */
    protected void showCMX() {
       JPanel x;
       JButton b;

       // J'enleve toute les precedentes composantes
       if( p!=null ) getContentPane().remove(p);

       p = new JPanel();
       GridBagLayout g =  new GridBagLayout();
       p.setLayout(g);
       GridBagConstraints c = new GridBagConstraints();
       c.fill = GridBagConstraints.BOTH;
       c.gridwidth = GridBagConstraints.REMAINDER;
       c.insets = new Insets(0,0,0,0);
       c.anchor = GridBagConstraints.CENTER;

       // Indication pour remplir le formulaire
       JPanel p1 = new JPanel();
       JLabel methode = new JLabel(Util.fold(METHODEX,80,true));
       methode.setFont(methode.getFont().deriveFont(Font.ITALIC));
       p1.add(methode);

       g.setConstraints(p1,c);
       p.add(p1);

       // Les fonctions de transfert
       x = new JPanel();
       x.add(Aladin.createLabel(CMCO+":"));
       transfertCBG = new ButtonGroup();
       transfertCB = new JRadioButton[PlanImage.TRANSFERTFCT.length];
       for( int i=0; i<PlanImage.TRANSFERTFCT.length; i++ ) {
          transfertCB[i] = new JRadioButton(PlanImage.TRANSFERTFCT[i],true);
          noBold(transfertCB[i]);
          transfertCB[i].addActionListener(this);
          transfertCBG.add(transfertCB[i]);
          if( i!=PlanImage.TRANSFERTFCT.length-1 ) x.add(transfertCB[i]);
       }
       transfertCBG.setSelected(transfertCB[getTransfertFct()].getModel(), true);

       g.setConstraints(x,c);
       p.add(x);

       JPanel validation = new JPanel();
       validation.setLayout( new FlowLayout(FlowLayout.RIGHT));

       // Les colormaps
       validation.add(Aladin.createLabel(CMM+":"));
       choiceCM = createChoiceCM(); choiceCM.addActionListener(this);
       choiceCM.setSelectedIndex(getTypeCM());

       validation.add(choiceCM);

       // Les boutons
       b=new JButton(REVERSE); b.addActionListener(this); validation.add(small(b));
       validation.add(new JLabel("  "));
       b=new JButton(RESET); b.addActionListener(this); validation.add(small(b));
       g.setConstraints(validation,c);
       p.add(validation);

       Filet f = new Filet(20,1);
       c.fill=GridBagConstraints.HORIZONTAL;
       g.setConstraints(f,c);
       c.fill=GridBagConstraints.NONE;
       p.add(f);

       x = new JPanel();
       labelOriginalPixel =  new JLabel(RANGE+" ["+getMinPix()+" .. "+getMaxPix()+"]     ");
       labelOriginalPixel.setFont( Aladin.BOLD);
       x.add(labelOriginalPixel);
       b=new JButton(NOCUT); b.addActionListener(this); x.add(b);
       g.setConstraints(x,c);
       p.add(x);

       x = new JPanel(new FlowLayout(FlowLayout.CENTER,5,0));
       minCut=new JTextField(getMinPixCut(),8);
       maxCut=new JTextField(getMaxPixCut(),8);
       x.add( noBold(new JLabel(LIMITS+" [")));
       x.add(minCut);
       x.add( new JLabel(".."));
       x.add(maxCut);
       x.add( noBold(new JLabel("]")));
       g.setConstraints(x,c);
       p.add(x);

       autocutBox = new JCheckBox(ALGO,false); autocutBox.addActionListener(this);
       g.setConstraints(noBold(autocutBox),c);
       p.add(autocutBox);

       x = new JPanel();
       b=new JButton(REPLAY); b.addActionListener(this); x.add(b);
       b=new JButton(CLOSE); b.addActionListener(this); x.add(b);
       g.setConstraints(x,c);
       p.add(x);

       getContentPane().add("Center",p);

       pack();
       setVisible(true);
       toFront();
//       setResizable(false);
    }
    
    private void switchMode(PlanBG pbg) {
       pbg.switchFormat();
       showCM();
       aladin.view.repaintAll();
       if( pbg.truePixels && pbg.useCache ) {
          aladin.info(this,"The true all sky pixel mode requires a large network bandwidth\n" +
             "Your sky is being reloaded... Look for it in the Aladin stack\n" +
             "and be patient...");
       }
    }

  /** Construction du panel en fonction de l'image en parametre.
   * @param pimg l'image courante
   */
   protected void showCM() {
      JPanel x;
      JButton b;
      boolean isPlanRGB = pimg instanceof PlanImageRGB;
      boolean isPlanBlink = pimg instanceof PlanImageBlink;

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
      
      // Switch en true pixels pour un plan Healpix
      if( pimg instanceof PlanBG ) {
         final PlanBG pbg = (PlanBG) pimg;
         if( pbg.inFits && pbg.inJPEG ) {
            JPanel p2 = new JPanel();
            p2.setBackground(Color.yellow);
            p2.add( new JLabel("<HTML><B>Mode:</B> "+pbg.getFormat()+"</HTML>"));
            JButton bt = new JButton( pbg.truePixels ? "Switch to fast 8 bit pixels" : "Switch to (slow) true pixels");
            bt.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) { switchMode(pbg); }
            } );
            p2.add(bt);
            g.setConstraints(p2, c);
            p.add(p2);
         }
      }
      
      // Indication pour remplir le formulaire
      JPanel p1 = new JPanel();
      JLabel methode = new JLabel(Util.fold(isPlanRGB?METHODERGB:METHODE,80,true));
      methode.setFont(methode.getFont().deriveFont(Font.ITALIC));
      p1.add(methode);

      p1.add(Util.getHelpButton(this,MESSAGE));
      g.setConstraints(p1,c);
      p.add(p1);

      /* anais */
      // Le gestionnaire de CM
      if( isPlanRGB ) {
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

      } else {
         cmPanel = new JPanel();
         cmPanel.add(cm);
         g.setConstraints(cmPanel,c);
         p.add(cmPanel);
      }      

      if( !isPlanRGB  ) {

         // Les fonctions de transfert
         x = new JPanel();
         x.add(Aladin.createLabel(CMCO+":"));
         transfertCBG = new ButtonGroup();
         transfertCB = new JRadioButton[PlanImage.TRANSFERTFCT.length];
         for( int i=0; i<PlanImage.TRANSFERTFCT.length-1; i++ ) {
            transfertCB[i] = new JRadioButton(PlanImage.TRANSFERTFCT[i],true);
            noBold(transfertCB[i]);
            transfertCB[i].addActionListener(this);
            transfertCBG.add(transfertCB[i]);
            x.add(transfertCB[i]);
         }
         transfertCBG.setSelected(transfertCB[getTransfertFct()].getModel(), true);
         g.setConstraints(x,c);
         p.add(x);
      }

      JPanel validation = new JPanel();
//      validation.setLayout( new FlowLayout(FlowLayout.RIGHT));
      // Les colormaps
      if( !isPlanRGB ) {
         validation.add(Aladin.createLabel(CMM+":"));
         choiceCM = createChoiceCM(); choiceCM.addActionListener(this);
         choiceCM.setSelectedIndex(getTypeCM());
         validation.add(choiceCM);
      }
      // Les boutons
      b=new JButton(REVERSE); b.addActionListener(this); validation.add(small(b));
      validation.add(new JLabel("  "));
      b=new JButton(RESET); b.addActionListener(this); validation.add(small(b));
      if( isPlanRGB || isPlanBlink ) {
         b=new JButton(CLOSE); b.addActionListener(this); validation.add(b);
      }
      g.setConstraints(validation,c);
      p.add(validation);
      

      if( !isPlanRGB && !Aladin.OUTREACH  && pimg.hasOriginalPixels()  ) {

         Filet f = new Filet(20,1);
         c.fill=GridBagConstraints.HORIZONTAL;
         g.setConstraints(f,c);
         p.add(f);
         c.fill=GridBagConstraints.NONE;

         x = new JPanel();
         labelOriginalPixel =  new JLabel(RANGE+" ["+getMinPix()+" .. "+getMaxPix()+"]     ");
         labelOriginalPixel.setFont( Aladin.BOLD);
         x.add(labelOriginalPixel);
         b=new JButton(NOCUT); b.addActionListener(this); x.add(b);
         g.setConstraints(x,c);
         p.add(x);
         
         x = new JPanel();
         minCut=new JTextField(getMinPixCut(),8);
         maxCut=new JTextField(getMaxPixCut(),8);
         x.add( noBold(new JLabel(LIMITS+" [")));
         x.add(minCut);
         x.add( new JLabel(".."));
         x.add(maxCut);
         x.add( noBold(new JLabel("]")));
         g.setConstraints(x,c);
         p.add(x);

         autocutBox = new JCheckBox(ALGO,false); autocutBox.addActionListener(this);
         g.setConstraints(noBold(autocutBox),c);
         p.add(autocutBox);

         x = new JPanel();
         b=new JButton(REPLAY); b.addActionListener(this); x.add(b);
         b=new JButton(ALL); b.addActionListener(this); x.add(b);
         b=new JButton(CLOSE); b.addActionListener(this); x.add(b);
         g.setConstraints(x,c);
         p.add(x);
      }

      getContentPane().add(p,BorderLayout.CENTER);

      pack();
      setVisible(true);
      toFront();
//      setResizable(false);
   }
   
   public Dimension getMinimumSize() { return new Dimension(200,300); }

   protected JComponent noBold(JComponent c) {
      c.setFont(c.getFont().deriveFont(Font.PLAIN));
      return c;
   }

   protected JComponent small(JButton b) {
      b.setMargin(new Insets(2,2,2,2) );
      return b;
   }

   /** Création d'un choice des CM possibles */
   protected static JComboBox createChoiceCM() {
      JComboBox c = new JComboBox();
      // ajout des CM par défaut
      for( int i=0; i<CM.length; i++ ) c.addItem(CM[i]);
      // ajout des CM "custom"
      if( ColorMap.customCMName!=null ) {
      	Enumeration e = ColorMap.customCMName.elements();
      	while( e.hasMoreElements() ) {
      		c.addItem(e.nextElement());
      	}
      }
      // Ajout du ' --'
      c.addItem(" -- ");
      return c;
   }

     /** Réaffiche les valeurs des pixels dans l'unité courante */
     protected void changePixelUnit() {
         if( !pimg.hasAvailablePixels() ) return;

         minCut.setText(pimg.getPixelMinInfo());
         maxCut.setText(pimg.getPixelMaxInfo());
         labelOriginalPixel.setText(RANGE+" ["+pimg.getDataMinInfo()+" .. "+pimg.getDataMaxInfo()+"]");
         cm.repaint();
     }

   /** Retourne true si on est en train de draguer les triangles de la colormap
    * Voir ViewSimple.getImage() */
   final protected boolean isDragging() { return cm==null ? false : cm.isDragging(); };

   private void reset() {
      if( isMultiImg() ) {
         Enumeration e = vimg.elements();
         while( e.hasMoreElements() ) {
            PlanImage pi = (PlanImage)e.nextElement();
            pi.typeCM=aladin.configuration.getCMMap();
            choiceCM.setSelectedIndex(pi.typeCM);
            pi.video=aladin.configuration.getCMVideo();
            int dtf = aladin.configuration.getCMFct();
            transfertCBG.setSelected(transfertCB[dtf].getModel(), true);
            pi.setTransfertFct(dtf);
            cm.reset();
            setCM(cm.getCM());
            if( minCut!=null ) minCut.setText(getMinPixCut());
            if( maxCut!=null ) maxCut.setText(getMaxPixCut());
         }
      } else {
         if( pimg.type==Plan.IMAGERGB ) {
            if( ((PlanImageRGB)pimg).flagRed ) cm.reset();
            if( ((PlanImageRGB)pimg).flagGreen ) cm2.reset();
            if( ((PlanImageRGB)pimg).flagBlue ) cm3.reset();
            ((PlanImageRGB)pimg).createImgRGB();

         } else {
            aladin.console.setCommand("cm");
            pimg.typeCM=aladin.configuration.getCMMap();
            choiceCM.setSelectedIndex(pimg.typeCM);
            pimg.video=aladin.configuration.getCMVideo();
            if( pimg.type==Plan.ALLSKYIMG ) pimg.video=PlanImage.VIDEO_NORMAL;
            int dtf = aladin.configuration.getCMFct();
            transfertCBG.setSelected(transfertCB[dtf].getModel(), true);
            pimg.setTransfertFct(dtf);
            cm.reset();
            setCM(cm.getCM());
            if( minCut!=null ) minCut.setText(pimg.X(pimg.pixelMin));
            if( maxCut!=null ) maxCut.setText(pimg.X(pimg.pixelMax));
         }
      }
      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();

   }

  /** Gestion du bouton REVERSE */
   private void reverse() {
      if( isMultiImg() ) {
         Enumeration e = vimg.elements();
         while( e.hasMoreElements() ) {
            PlanImage pi = (PlanImage)e.nextElement();
            if( pi.video==PlanImage.VIDEO_NORMAL ) pi.video=PlanImage.VIDEO_INVERSE;
            else pi.video=PlanImage.VIDEO_NORMAL;
         }
         cm.repaint();
         setCM(cm.getCM());
      } else  {
         if( pimg.video==PlanImage.VIDEO_NORMAL ) pimg.video=PlanImage.VIDEO_INVERSE;
         else pimg.video=PlanImage.VIDEO_NORMAL;

         aladin.console.setCommand("cm "+(pimg.video==PlanImage.VIDEO_NORMAL?"noreverse":"reverse"));
         if( pimg.type==Plan.IMAGERGB ) {
            if( ((PlanImageRGB)pimg).flagRed )  { cm.pimg.video=pimg.video;  cm.repaint(); }
            if( ((PlanImageRGB)pimg).flagGreen )  { cm2.pimg.video=pimg.video; cm2.repaint(); }
            if( ((PlanImageRGB)pimg).flagBlue )  { cm3.pimg.video=pimg.video; cm3.repaint(); }

            ((PlanImageRGB)pimg).inverseRGB();
         }
         else {
            cm.repaint();
            setCM(cm.getCM());
         }
      }
      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();

   }

   /** Gestion du bouton "get all" */
   private void getAll() {
      if( isMultiImg() ) {
         Aladin.makeCursor(this, Aladin.WAITCURSOR);
         (new Thread("changeAutocut") {
            @Override
            public void run() {
               Enumeration e = vimg.elements();
               while( e.hasMoreElements() ) {
                  PlanImage pi = (PlanImage)e.nextElement();
                  minCut.setText(pi.getDataMinInfo());
                  maxCut.setText(pi.getDataMaxInfo());
                  pi.recut(pi.dataMin,pi.dataMax,false);
                  aladin.view.repaintAll();
               }
            }
         }).start();
         minCut.setText(getMinPix());
         maxCut.setText(getMaxPix());
      } else {
         minCut.setText(pimg.getDataMinInfo());
         maxCut.setText(pimg.getDataMaxInfo());
         Aladin.makeCursor(this, Aladin.WAITCURSOR);
         if( !pimg.recut(pimg.dataMin,pimg.dataMax,false) ) {
            Aladin.makeCursor(this, Aladin.DEFAULT);
            aladin.warning(this,NOFULLPIXEL);
            return;
         }
      }
      Aladin.makeCursor(this, Aladin.DEFAULT);
      aladin.console.setCommand("cm all");
      cm.repaint();
      setCM(cm.getCM());

      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();

   }

   // Juste pour transmettre le paramètre au thread
//   private int transfertFctTmp=-1;

   /** Changement de la fonction de transfert */
   private void changeTransfertFct() {
      int n;
      for( n=0; n<transfertCB.length && !transfertCB[n].isSelected(); n++ ) ;
      if( getTransfertFct()==n ) return; // déjà fait
      aladin.console.setCommand("cm "+PlanImage.TRANSFERTFCT[n]);

//      transfertFctTmp=n;
//      Thread t = new Thread(this,"AladinColorMap");
//      t.start();

      setTransfertFct(n);
      setCM(cm.getCM());

      aladin.view.repaintAll();
      aladin.calque.zoom.zoomView.repaint();
      repaint();

   }

//   /** Modification de fonction de transfert Threadé */
//   public void run() {
//      changeTransfertFctThread(transfertFctTmp);
//   }
//
//   /** Changement de la fonction de transfert */
//   private void changeTransfertFctThread(int transfertFct) {
//      aladin.makeCursor(this,Aladin.WAIT);
//      if( !pimg.setTransfertFct(transfertFct,true) ) {
//         aladin.warning(this,NOFULLPIXEL);
//         transfertCBG.setSelectedCheckbox(transfertCB[pimg.getTransfertFct()]);
//      } else {
//         cm.generateHist();
//         cm.repaint();
//         setCM(cm.getCM());
//      }
//      aladin.makeCursor(this,Aladin.DEFAULT);
//   }

   private void all() {
      vimg = aladin.calque.setSelectedSimpleImage();
      changeAutocut();
   }

  /** Gestion du bouton REPLAY */
   private void changeAutocut() {
      if( isMultiImg() ) {
         Aladin.makeCursor(this, Aladin.WAITCURSOR);

         (new Thread("changeAutocut") {
            @Override
            public void run() {
               Enumeration<PlanImage> e = vimg.elements();
               double min=0,max=0;
               boolean first=true;
               while( e.hasMoreElements() ) {
                  PlanImage pi = e.nextElement();
                  if( first ) {
                     first =false;
                     min = pi.getPixelValue(minCut.getText());
                     max = pi.getPixelValue(maxCut.getText());
                  }
                  if( min<max ) {
                     pi.recut(min,max,autocutBox.isSelected());
                     aladin.view.repaintAll();
                  }
               }
            }
         }).start();
         Aladin.makeCursor(this, Aladin.DEFAULT);

         setCM(cm.getCM());
         minCut.setText(getMinPixCut());
         maxCut.setText(getMaxPixCut());
         aladin.view.repaintAll();
         aladin.calque.zoom.zoomView.repaint();
         return;
      }
      try {
         double min = pimg.getPixelValue(minCut.getText());
         double max = pimg.getPixelValue(maxCut.getText());

         if( min>max ) {
            Aladin.warning(this,ERRORRANGE);
            return;
         }

         aladin.console.setCommand("cm "+minCut.getText()+".."+maxCut.getText()
               +(!autocutBox.isSelected()?" noautocut":""));

         Aladin.makeCursor(this, Aladin.WAITCURSOR);
         if( !pimg.recut(min,max,autocutBox.isSelected()) ) {
            Aladin.makeCursor(this, Aladin.DEFAULT);
            aladin.warning(this,NOFULLPIXEL);
            return;
         }
         Aladin.makeCursor(this, Aladin.DEFAULT);

         pimg.freeHist();
         setCM(cm.getCM());
         minCut.setText(pimg.getPixelMinInfo());
         maxCut.setText(pimg.getPixelMaxInfo());
         aladin.view.repaintAll();
         aladin.calque.zoom.zoomView.repaint();

      } catch( Exception e) { e.printStackTrace(); }
   }

  /** Changement de la color map */
   private void changeCM(JComboBox c) {
      int n = c.getSelectedIndex();
      if( n==getTypeCM() ) return;
      setTypeCM(n);
      cm.repaint();
      if( n<CMA.length ) aladin.console.setCommand("cm "+CMA[n]);
      else {
         try { aladin.console.setCommand("cm "+ColorMap.customCMName.get(n-ColorMap.LAST_DEFAULT_CM_IDX-1)); }
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
       if( isMultiImg() ) {
          Enumeration<PlanImage> e = vimg.elements();
          while( e.hasMoreElements() ) {
             PlanImage pi = e.nextElement();
             pi.setCM(ic);
          }
       } else {
          pimg.setCM(ic);
          pimg.aladin.calque.zoom.zoomView.setCM(ic);
       }
    }

   /** Memorisation des infos de controle de la tables des couleurs */
   private void memoControl() {
      if( pimg==null || cm==null ) return;

      // Memorisation du control de la table des couleurs
      /*anais*/
      if( pimg instanceof PlanImageRGB ) {
         for( int i=0; i<3; i++ ) {
            PlanImageRGB pRGB = (PlanImageRGB)pimg;
            if( pRGB.flagRed ) pRGB.RGBControl[i]=cm.triangle[i];
            if( pRGB.flagGreen ) pRGB.RGBControl[3+i]=cm2.triangle[i];
            if( pRGB.flagBlue ) pRGB.RGBControl[6+i]=cm3.triangle[i];
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
         aladin.toolbox.tool[ToolBox.HIST].setMode(Tool.UP);
         aladin.toolbox.repaint();
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
      if( aladin.calque.getPlanBase()!=null )  aladin.toolbox.tool[ToolBox.HIST].setMode(Tool.UP);
      else aladin.toolbox.tool[ToolBox.HIST].setMode(Tool.UNAVAIL);
      aladin.toolbox.repaint();
      imgID=-1;
   }

  /** Trap sur l'evenement WINDOW_DESTROY    */
   @Override
   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) dispose();
      super.processWindowEvent(e);
   }
   
   public void actionPerformed(ActionEvent e) {
      String s = e.getActionCommand();
           if( CLOSE.equals(s) )     dispose();
      else if( RESET.equals(s) )     reset();
      else if( REVERSE.equals(s) )   reverse();
      else if( NOCUT.equals(s) )     getAll();
      else if( REPLAY.equals(s) )    changeAutocut();
      else if( ALL.equals(s) )       all();
      else if( e.getSource() instanceof JRadioButton ) changeTransfertFct();
      else if( e.getSource() instanceof JComboBox ) changeCM((JComboBox)e.getSource() );
           
     if( aladin.frameAllsky!=null ) aladin.frameAllsky.updateCurrentCM();

   }

}

