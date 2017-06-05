// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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
import java.awt.Color;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import cds.tools.Util;
/**
 * Gestion de la fenetre associee a la creation d'un plan Contour
 *
 * @author Thomas Boch [CDS]
 * @version 1.1 : (dec. 2007) Passage à Swing
 *          1.0 : (22 fevrier 2002) Creation
 */
public final class FrameContour extends JFrame implements ActionListener {
        protected String TITRE, CLOSE, RESET, SUBMIT, NEWLEVEL, SHOWHELP,
                         HIDEHELP, SMOOTHINFO, NOISEINFO, ZOOMINFO,
                         CHOOSELEVEL, GENERATE, SMOOTH, LEVEL, MANUALLY,
                         USESM, REDUCE, CONSIDER, TITRE2;

       static final Font HELPFONT = new Font("Monospaced",Font.PLAIN,10);

       boolean showHelp = false;	    // faut-il montrer les labels d'aide ?

       boolean flagHide = true;

       JPanel p; 	    	    	    // panel principal

       JLabel lab1;
       MyLabel smoothInfo;
       MyLabel noiseInfo;
       MyLabel zoomInfo;

       JButton helpBtn;        // bouton pour afficher/masquer l'aide

       JButton submitBtn;      // bouton pour lancer le calcul des contours

       JCheckBox smoothCb;     // utilise-t-on le smoothing ?

       JCheckBox noisecb;      // reduction du bruit ?


       PlanImage pimg;

       JComboBox nbLevelsChoice;

       JComboBox smooothLevelChoice;

       JCheckBox currentZoomOnly; // considère-t-on le zoom courant uniquement ?

       private Histogramme hist;

       private Color[] couleurs = null;            // tableau des couleurs pour les curseurs

       private double[] levels; // tableaux des valeurs entres par l'utilisateur

       // Les references aux objets
       Aladin a;

       Curseur curs;

       int etat = -4;              // // Memorisation de l'etat de l'image

       protected void createChaine() {
          TITRE = a.chaine.getString("CPTITRE");
          TITRE2 = a.chaine.getString("CPTITRE2");
          CLOSE = a.chaine.getString("CLOSE");
          RESET = a.chaine.getString("RESET");
          SUBMIT = a.chaine.getString("CPSUBMIT");
          NEWLEVEL = a.chaine.getString("CPNEWLEVEL");
          SHOWHELP = a.chaine.getString("CPSHOWHELP");
          HIDEHELP = a.chaine.getString("CPHIDEHELP");
          SMOOTHINFO = a.chaine.getString("CPSMOOTHINFO");
          NOISEINFO = a.chaine.getString("CPNOISEINFO");
          ZOOMINFO = a.chaine.getString("CPZOOMINFO");
          CHOOSELEVEL = a.chaine.getString("CPCHOOSELEVEL");
          GENERATE = a.chaine.getString("CPGENERATE");
          SMOOTH = a.chaine.getString("CPSMOOTH");
          LEVEL = a.chaine.getString("CPLEVEL");
          MANUALLY = a.chaine.getString("CPMANUALLY");
          USESM = a.chaine.getString("CPUSESM");
          REDUCE = a.chaine.getString("CPREDUCE");
          CONSIDER = a.chaine.getString("CPCONSIDER");
       }

      /** Creation du Frame gerant la creation d'un plan Contour.
       * @param aladin Reference
       */
       protected FrameContour(Aladin aladin) {
          super();
          this.a = aladin;
          Aladin.setIcon(this);

          createChaine();
          setTitle(TITRE);

          lab1 = new JLabel(" "+SMOOTH);
          smoothInfo = new MyLabel(SMOOTHINFO,Label.LEFT,HELPFONT);
          noiseInfo = new MyLabel(NOISEINFO,Label.LEFT,HELPFONT);
          zoomInfo = new MyLabel(ZOOMINFO,Label.LEFT,HELPFONT);

//          pimg = (PlanImage) a.calque.getPlanBase();
          pimg = a.calque.getFirstSelectedSimpleImage();
          setLocation(350,200);

          hist = new Histogramme();
          curs = new Curseur(hist);

          enableEvents(AWTEvent.WINDOW_EVENT_MASK);
          Util.setCloseShortcut(this, false, aladin);

          initCouleurs();
          fillCouleurTriangle();
       }

       /**
        * Creation du panel contenant l'histogramme et permettant de choisir le nb de niveaux
        *
        */
       private JPanel getTopPanel() {
           GridBagLayout g =  new GridBagLayout();
           GridBagConstraints c = new GridBagConstraints();
           c.fill = GridBagConstraints.NONE;
           c.gridwidth = GridBagConstraints.REMAINDER;
           c.insets = new Insets(1,3,2,3);
           JPanel p = new JPanel();
           p.setLayout(g);

           p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                   BorderFactory.createTitledBorder(null, CHOOSELEVEL, TitledBorder.LEADING, TitledBorder.TOP, Aladin.PLAIN),
                   BorderFactory.createEmptyBorder(5,5,3,5))));

           // affichage de l'histogramme
           hist.setImage(pimg);
           c.anchor = GridBagConstraints.CENTER;
           g.setConstraints(hist, c);
           p.add(hist);

           // affichage du slider
           g.setConstraints(curs, c);
           p.add(curs);

           // panel de generation automatique des niveaux
           JPanel pGenerate = new JPanel();
           JLabel l;
           pGenerate.setLayout( new FlowLayout(FlowLayout.LEFT,7,5));
           pGenerate.add(l = new JLabel(GENERATE));
           l.setFont(Aladin.PLAIN);
           nbLevelsChoice = new JComboBox();
           nbLevelsChoice.addActionListener(this);
           for(int i=1;i<=10;i++) {
             nbLevelsChoice.addItem(new Integer(i));
           }
           for(int i=12;i<=PlanContour.MAXLEVELS;i+=2) {
             nbLevelsChoice.addItem(new Integer(i));
           }
           int defaultVal = 4;
           nbLevelsChoice.setSelectedIndex(defaultVal-1);
           pGenerate.add(nbLevelsChoice);
           pGenerate.add(l=new JLabel(LEVEL));
           l.setFont(Aladin.PLAIN);
           createLevels(defaultVal);
           c.anchor = GridBagConstraints.WEST;
           g.setConstraints(pGenerate, c);
           p.add(pGenerate);

           JPanel pNewLevel = new JPanel();
           pNewLevel.setLayout( new FlowLayout(FlowLayout.LEFT,7,5));
           pNewLevel.add(l = new JLabel(MANUALLY));
           l.setFont(Aladin.PLAIN);
           pNewLevel.add(createButton(NEWLEVEL));
           c.insets = new Insets(0,0,0,0);
           g.setConstraints(pNewLevel, c);
           p.add(pNewLevel);


           return p;
       }

       /** Creation du panel d'options
        *
        * @return le panel d'options
        */
       private JPanel getOptionsPanel() {
           GridBagLayout g =  new GridBagLayout();
           GridBagConstraints c = new GridBagConstraints();
           c.fill = GridBagConstraints.NONE;
           c.gridwidth = GridBagConstraints.REMAINDER;
           c.insets = new Insets(1,3,2,3);
           c.anchor = GridBagConstraints.WEST;
           JPanel p = new JPanel();
           p.setLayout(g);

           p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5), BorderFactory.createCompoundBorder(
                   BorderFactory.createTitledBorder(null, "", TitledBorder.LEADING, TitledBorder.TOP, Aladin.PLAIN),
                   BorderFactory.createEmptyBorder(5,5,3,5))));

           // label d'aide sur le lissage
           JPanel smoothPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));
           smoothInfo = new MyLabel(SMOOTHINFO,Label.LEFT,HELPFONT);
           smoothInfo.setVisible(showHelp);
           smoothPanel.add(smoothInfo);
           g.setConstraints(smoothPanel,c);
           p.add(smoothPanel);

           // panel utilisation du lissage
           JPanel pLissage = new JPanel();
           pLissage.setLayout( new FlowLayout(FlowLayout.LEFT,7,0));
           smoothCb = new JCheckBox(USESM,null,true);
           smoothCb.setFont(Aladin.PLAIN);
           smoothCb.addActionListener(this);
           smooothLevelChoice = new JComboBox();
           for( int i=2; i<=4; i++ ) {
               smooothLevelChoice.addItem(new Integer(i));
           }
           smooothLevelChoice.setSelectedIndex(0);
           smooothLevelChoice.setEnabled(true);
           lab1.setFont(Aladin.PLAIN);
           lab1.setEnabled(true);
           pLissage.add(smoothCb);
           pLissage.add(lab1);
           pLissage.add(smooothLevelChoice);
           g.setConstraints(pLissage,c);
           p.add(pLissage);

           // label d'aide sur reduction du bruit
           JPanel noiseInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));
           noiseInfo = new MyLabel(NOISEINFO,Label.LEFT,HELPFONT);
           noiseInfo.setVisible(showHelp);
           noiseInfoPanel.add(noiseInfo);
           g.setConstraints(noiseInfoPanel,c);
           p.add(noiseInfoPanel);

           // reduction du bruit
           JPanel noisecbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));
           noisecb = new JCheckBox(REDUCE,null,true);
           noisecb.setFont(Aladin.PLAIN);
           noisecbPanel.add(noisecb);
           g.setConstraints(noisecbPanel,c);
           p.add(noisecbPanel);

           // label d'aide sur current view
           JPanel zoomInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));
           zoomInfo = new MyLabel(ZOOMINFO,Label.LEFT,HELPFONT);
           zoomInfo.setVisible(showHelp);
           zoomInfoPanel.add(zoomInfo);
           g.setConstraints(zoomInfoPanel,c);
           p.add(zoomInfoPanel);

           // checkbox "dessine t on les contours sur toute l'image" ?
           JPanel currentZoomOnlyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,7,0));
           currentZoomOnly = new JCheckBox(CONSIDER,false);
           currentZoomOnly.setFont(Aladin.PLAIN);
           currentZoomOnlyPanel.add(currentZoomOnly);
           g.setConstraints(currentZoomOnlyPanel,c);
           p.add(currentZoomOnlyPanel);
           try {
              currentZoomOnly.setEnabled( a.calque.getFirstSelectedSimpleImage().ref );
           } catch( Exception e ) {
              currentZoomOnly.setEnabled( false );
           }

           // bouton pour afficher l'aide
           helpBtn = createButton(showHelp?HIDEHELP:SHOWHELP);
           c.anchor = GridBagConstraints.CENTER;
           c.insets = new Insets(0,0,0,0);
           g.setConstraints(helpBtn,c);
           p.add(helpBtn);


           return p;
       }

       /** Construction du panel du bas (avec boutons Submit et Close) */
       private JPanel getBottomPanel() {
           // panel des boutons
           Insets myInsets = new Insets(0,4,0,4);
           JPanel pButtons = new JPanel();
           pButtons.setLayout( new FlowLayout(FlowLayout.CENTER));
           submitBtn = createButton(SUBMIT);
           submitBtn.setFont(Aladin.BOLD);
           submitBtn.setMargin(myInsets);
           pButtons.add( submitBtn);
           pButtons.add( createButton(RESET));
           pButtons.add( createButton(CLOSE));

           return pButtons;
       }


       // Creation de l'ensemble de la frame
       private void createAllPanels() {
          getContentPane().removeAll();

          getContentPane().setLayout(new BorderLayout());

          JPanel p = new JPanel();
          p.setLayout(new BoxLayout(p, BoxLayout.PAGE_AXIS));

          // Le titre
          p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,5,5,5),
                      BorderFactory.createTitledBorder(null, TITRE2, TitledBorder.CENTER, TitledBorder.TOP, Aladin.LBOLD)));


          p.add(getTopPanel());
          JPanel optionsPanel = getOptionsPanel();
          if( !Aladin.OUTREACH ) p.add(optionsPanel);
          getContentPane().add(p, BorderLayout.CENTER);
          getContentPane().add(getBottomPanel(), BorderLayout.SOUTH);

          pack();
          show();
          flagHide = false;
        }



       // Visualisation de la fenetre
       @Override
    public void show() {
          initCouleurs();
          fillCouleurTriangle();

          super.show();
          flagHide=false;
       }


       @Override
    public void hide() {
          if( a.calque.getFirstSelectedPlanImage()!=null )  a.toolBox.tool[ToolBox.CONTOUR].mode=Tool.UP;
          else a.toolBox.tool[ToolBox.CONTOUR].mode=Tool.UNAVAIL;
          a.toolBox.repaint();
          flagHide=true;
          etat=-1;
          super.hide();
       }

       
       private RectangleD memoRzoom=null;

      /** Mise a jour de la fenetre si necessaire*/
       protected void majContour() {
          if( a.toolBox.tool[ToolBox.CONTOUR].mode==Tool.DOWN ) {
             
             PlanImage p = (PlanImage)a.calque.getFirstSelectedPlanImage();
             
//             Plan p1 = a.calque.getPlanBase();
//             if( !(p1 instanceof PlanImage) ) return;
//             PlanImage p = (PlanImage) p1;
             
             if( p!=null && p.flagOk && p.isPixel() ) {
                int newEtat = p.getImgID();

                if( etat!=newEtat) {
                   etat=newEtat;
                   try {
                      
                      // Dans le cas d'un plan BG, il est inutile de faire le crop à chaque changement d'etats
                      // Je me base sur la valeur du rzoom de la vue 
                     if( p instanceof PlanBG ) {
                        ViewSimple v = a.view.getView(p);
                        if( !v.rzoom.equals(memoRzoom) ) {
                           this.pimg = getCropImage(p);
                           memoRzoom=v.rzoom;
                        } else return;
                     } else this.pimg = p;
                  } catch( Exception e ) {
                    a.warning(this,e.getMessage());
                    return;
                  }
                   createAllPanels();
                }

             }
             return;
          }

          hide();
       }

       /** Fermeture de la Frame.
        */
       /*public void dispose() {
          if( flagHide ) return;

          if( a.calque.getPlanBase()!=null )  a.toolbox.tool[ToolBox.CONTOUR].mode=Tool.UP;
          else a.toolbox.tool[ToolBox.CONTOUR].mode=Tool.UNAVAIL;
          a.toolbox.repaint();
          flagHide=true;
          etat=-1;
          super.dispose();
       }*/
       
       PlanImage getCropImage(Plan p ) throws Exception {
          if( !(p instanceof PlanBG) ) throw new Exception("Contour cropping only on all-sky image");
          ViewSimple v = a.view.getCurrentView();
          if( v.pref!=p )  throw new Exception("All-sky image contour is only available on current view !");
          return a.calque.createCropImage(v);
       }


       // remplit curs.couleurTriangle en fonction de indicesCouleurs
       private void fillCouleurTriangle() {
         int i;
         for(i=0;i<curs.couleurTriangle.length;i++) {
           curs.couleurTriangle[i] = couleurs[i];
         }
       }

       // initialise le tableau couleurs
       private void initCouleurs() {
         int i;
         if(couleurs==null) couleurs = new Color[PlanContour.MAXLEVELS];

         Color couleurBase = PlanContour.getNextColor(a.calque);

         Color[] base = Couleur.getBrighterColors(couleurBase,4);

         for(i=0;i<couleurs.length;i++)
            couleurs[i] = base[i%base.length];
       }

      /** enable/disable la liste deroulante de choix du smoothlevel */
      private void adjustsmooothLevelChoice() {
          if(!smoothCb.isSelected()) {smooothLevelChoice.setEnabled(false);lab1.setEnabled(false);}
          else {smooothLevelChoice.setEnabled(true);lab1.setEnabled(true);}
      }

      // parse les niveaux entres par l'utilisateur
       private void parseLevels() {

          //remplissage de levels
          levels = new double[curs.nbNiveaux];
          for(int i=0;i<curs.nbNiveaux;i++) {
            levels[i] = curs.niveaux[i];
          }

          // tri de levels pour qu'ils apparaissent dans l'ordre croissant dans les Properties
          for (int i = 0 ; i < levels.length ; i++) {
            double m = levels[i] ;
            Color couleurm = couleurs[i];

            for (int j = i+1 ; j < levels.length ; j++) {
              if (levels[j] < m) {
                // echanger m et levels[j]
                double t = m ;
                m = levels[j] ;
                levels[j] = t;

                // echanger couleurs[i] et couleurs[j] (sinon, les couleurs ne correspondront plus)

                Color couleurt = couleurm;
                couleurm = couleurs[j];
                couleurs[j] = couleurt;
                couleurs[i] = couleurm;

              }
            }
            levels[i] = m;
          }

       }

       // genere un tableau de n niveaux egalement repartis entre 0 et 255
       static protected int[] generateLevels(int n) {
           int min = 0;
           int max = 255;
           int[] tab = new int[n];

           double step = (max-min)/(n+1);

           for (int i=0;i<n;i++) tab[i] = (int) (step + step*i);

           return tab;

       }


   /** Reset : tous les curseurs sont effaces, on en remet un avec une valeur initiale */
   protected void reset() {
       curs.reset();
       curs.niveaux[0]=100;
       curs.nbNiveaux=1;
   }

   private void createLevels(int nbLevels) {
       curs.nbNiveaux = 0;
       int[] lev = generateLevels(nbLevels);
       for (int i=0;i<lev.length;i++) curs.addCurseur(lev[i]);
   }

   // Gestion des evenements
   public void actionPerformed(ActionEvent ae) {
       String what = ae.getActionCommand();
       Object target = ae.getSource();

       if( CLOSE.equals(what) ) {hide();}
       else if( RESET.equals(what) ) reset();
       else if( nbLevelsChoice.equals(target) ) {
           int nbNiveaux = ((Integer)nbLevelsChoice.getSelectedItem()).intValue();
           createLevels(nbNiveaux);

       }
       else if(smoothCb.equals(target) ) {
           adjustsmooothLevelChoice();
       }
       else if( helpBtn.equals(target) ) {
           showHelp = !showHelp;
           if( showHelp ) {
               helpBtn.setText(HIDEHELP);

           }
           else {
               helpBtn.setText(SHOWHELP);

           }
           smoothInfo.setVisible(showHelp);
           noiseInfo.setVisible(showHelp);
           zoomInfo.setVisible(showHelp);
           //algoInfo.setVisible(showHelp);
           if (showHelp) setLocation(getLocation().x,70);
           else setLocation(getLocation().x,200);
           pack();
       }
       else if( SUBMIT.equals(what) ) {
           hide();

           parseLevels();

           // création du plan contour
           a.calque.newPlanContour("Contours",pimg,this.levels,new ContourPlot(),smoothCb.isSelected(),((Integer)smooothLevelChoice.getSelectedItem()).intValue(),currentZoomOnly.isSelected(),noisecb.isSelected(),couleurs);
           // écriture sur la console de la commande script équivalente
           a.console.printCommand("contour "+levels.length+(smoothCb.isSelected()?"":" nosmooth")+(currentZoomOnly.isSelected()?" zoom":""));


           // un nouveau plan a ete cree, il faut mettre a jour les couleurs
           initCouleurs();
           fillCouleurTriangle();
           curs.repaint();
       }
       else if ( NEWLEVEL.equals(what) ) {
           curs.addCurseur();
       }
   }


   static private final Insets BUTTON_INSETS = new Insets(0,2,0,2);
   private JButton createButton(String s) {
       JButton b = new JButton(s);
       b.setFont(Aladin.PLAIN);
       b.setMargin(BUTTON_INSETS);
       b.addActionListener(this);

       return b;
   }


   // Gestion des evenements
   @Override
public boolean handleEvent(Event e) {

       if( e.id==Event.WINDOW_DESTROY ) {hide();}
       return super.handleEvent(e);
   }






}
