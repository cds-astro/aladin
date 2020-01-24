// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.aladin.PlanBGRgb;
import cds.aladin.prop.PropPanel;
import cds.allsky.Context.JpegMethod;
import cds.tools.Util;

public class TabRgb extends JPanel implements ActionListener {
   
   static private int TEXTSIZE = 6;             // Nombre de caractères des champs JText
   static private final String [] RGB = new String[] { "Red","Green","Blue" };

   private static String B;
   private static String G;
   private static String R;

   private String REP_DEST;
   private String RGBALLSKY;
   private String BROWSE;
   private String HELP;
   private String CHOOSE="-- select a plane --";

   private JLabel		dirLabel = new JLabel();
   private JButton 	    browse = new JButton();
   private JTextField 	outputField = new JTextField(30);

   private JRadioButton radioMediane;               // selected si on est en calcul selon la médiane
   private JRadioButton formatJpeg;                 // selected si JPG sinon PNG
   private JCheckBox gauss;                         // selected si on doit appliqué un filtre gaussien
   private JRadioButton lupton,classic;

   JProgressBar progressBar = new JProgressBar(0,100);
   

   private JComboBox[] ch;
   private JButton preview,start,abort,pause;
   private JButton help = new JButton();
   private final Aladin aladin;
   private MainPanel mainPanel;
   private ContextGui context;
   private String titlehelp;
   private JButton reset;
   
   // Paramètres de la méthode RGB classique
   private JText cutMin[]    = new JText[3];  
   private JText cutMiddle[] = new JText[3];  
   private JText cutMax[]    = new JText[3];  
   private JText function[]  = new JText[3];     
   
   // Paramètres de la méthode Lupton
   private JText luptonM[] = new JText[3];  
   private JText luptonS[] = new JText[3];  
   private JText luptonQ;
   
   private ArrayList<JText>  arrayJText=new ArrayList<>();   // Liste des JText (pointeurs) concernés par un Reset
   private PlanBGRgb planPreview = null;     // PlanBGRgb de prévisualisation;

   public TabRgb(Aladin aladin,MainPanel mainPanel) {
      super(new BorderLayout());
      this.aladin = aladin;
      this.mainPanel = mainPanel;
      context = mainPanel.context;
      createChaine(Aladin.getChaine());
      help = Util.getHelpButton(this,HELP);

      GridBagConstraints c = new GridBagConstraints();
      c.fill=GridBagConstraints.BOTH;
      c.insets = new Insets(2,2,2,2);

      JPanel pCenter = new JPanel();
      GridBagLayout g = new GridBagLayout();
      pCenter.setLayout(g);
      pCenter.setBorder(BorderFactory.createEmptyBorder(5, 55, 5,55));

      JLabel info = new JLabel(Util.fold(RGBALLSKY,80,true));
      info.setFont(info.getFont().deriveFont(Font.ITALIC));
      c.gridwidth=GridBagConstraints.REMAINDER;
      c.insets.bottom=20;
      pCenter.add(info,c);

      // Création des lignes pour choisir les plans
      c.insets.bottom=2;
      int n=3;
      ch=new JComboBox[n];
      for (int i=0; i<n; i++) {
         ch[i]=new JComboBox();
         ch[i].addActionListener(this);
         ch[i].setPreferredSize(new Dimension(200,20));

         JLabel ll=new JLabel(getLabelSelector(i));
         ll.setForeground(getColorLabel(i));

         c.gridwidth=GridBagConstraints.RELATIVE;
         c.weightx=0.0;
         pCenter.add(ll,c);
         c.gridwidth=GridBagConstraints.REMAINDER;
         //			c.weightx=10.0;
         pCenter.add(ch[i],c);

         ch[i].addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
               init();
            }
         });
      }

      init();
      c.gridwidth=GridBagConstraints.RELATIVE;
      c.weightx=0;

      // Sélection du répertoire destination
      JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      dirLabel = new JLabel(REP_DEST);
      pCenter.add(dirLabel,c);
      browse.setText(BROWSE);
      browse.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { dirBrowser(outputField); }
      });
      dirPanel.add(outputField);
      outputField.addKeyListener( new KeyAdapter() {
         public void keyReleased(KeyEvent e) { resumeWidgets(); }
      });
      if( Aladin.aladin.configuration.isLookAndFeelJava() ) dirPanel.add(browse);
      c.gridwidth=GridBagConstraints.REMAINDER;
      pCenter.add(dirPanel,c);

      //		c.gridx=GridBagConstraints.RELATIVE; c.gridy=GridBagConstraints.RELATIVE;
      c.gridwidth=GridBagConstraints.REMAINDER;
      c.fill=GridBagConstraints.HORIZONTAL;
      
      int m;
      m=c.insets.top;
      pCenter.add( getParamPanel(),c);
      c.insets.top=m;

      // barre de progression
      progressBar.setStringPainted(true);
      JPanel pProgress = new JPanel(new BorderLayout());
      pProgress.setBorder(new EmptyBorder(50, 0, 15, 0));
      pProgress.add(progressBar,BorderLayout.CENTER);
      pProgress.add(createStatPanel(),BorderLayout.SOUTH);
      pCenter.add(pProgress,c);

      JPanel fin = new JPanel(new BorderLayout());
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());

      // Les boutons liés à la prévisulation et à l'obtention de la ligne de commande
      preview = new JButton(getString("SEEPREVIEW"));
      preview.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { createPreview(); }
      });
      pBtn.add(preview);
      JButton b = new JButton(getString("CMDLINE"));
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { seeCmd(); }
      });
      pBtn.add(b);
      pBtn.add( new JLabel(" - "));

      start = new JButton(getString("START"));
      start.addActionListener(this);
      pBtn.add(start);
      pause = new JButton(getString("PAUSE"));
      pause.addActionListener(this);
      pBtn.add(pause);
      abort = new JButton(getString("ABORT"));
      abort.addActionListener(this);
      pBtn.add(abort);
      pBtn.add(Box.createRigidArea(new Dimension(10,0)));
      pBtn.add(Box.createHorizontalGlue());
      fin.add(pBtn, BorderLayout.CENTER);
      fin.add(help, BorderLayout.EAST);
      
      // composition du panel principal
      add(pCenter, BorderLayout.CENTER);
      add(fin, BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
   }
   
   // Visualisation de la ligne de commande script pour effectuer le même calcul
   private void seeCmd() {
      String s = getCmdLine();
      aladin.copyToClipBoard( s );
      aladin.console.printInPad(s);
      aladin.console.show();
      aladin.info(this, getString("CMDLINEINFO"));
   }
   
   // Génère le JPanel dédié aux paramètres
   private JPanel getParamPanel() {
      Component f=this;
      GridBagLayout g = new GridBagLayout();
      JPanel p = new JPanel(g);
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;   
      c.insets = new Insets(0,0,0,0);
      
      ButtonGroup bg;
      JPanel p1;
      JRadioButton b;
      JCheckBox b1;
      
      final JTabbedPane pTab = new JTabbedPane();
      
      bg = new ButtonGroup();
      p1 = new JPanel();
      classic = b = new JRadioButton("Classic"); bg.add(b);  p1.add(b); b.setSelected(true);
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { pTab.setSelectedIndex(0); updatePreview(); }
      });
      lupton =b = new JRadioButton("Lupton");  bg.add(b);  p1.add(b);  
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { pTab.setSelectedIndex(1); updatePreview(); }
      });
      
      reset = new JButton("reset");
      reset.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { 
            SwingUtilities.invokeLater(new Runnable() {
               public void run() { resetTables(); }
            });
         }
      });
      p1.add(reset);
      
      PropPanel.addCouple(f, p,getString("RGBMETHOD")+" ", 
            getString("RGBMETHODH"),
            p1,g,c, GridBagConstraints.EAST);
      
      pTab.add("Classic", getClassicPanel());
      pTab.addChangeListener( new ChangeListener() {
         public void stateChanged(ChangeEvent e) {
            lupton.setSelected(pTab.getSelectedIndex()==1);
            classic.setSelected(pTab.getSelectedIndex()==0);
            updatePreview();
         }
      });
      pTab.add("Lupton", getLuptonPanel());
      pTab.setSelectedIndex(0);
      GridBagConstraints c1 = (GridBagConstraints) c.clone();
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.BOTH;
      g.setConstraints(pTab,c);
      p.add(pTab);
      c = c1;
      
      bg = new ButtonGroup();
      p1 = new JPanel();
      b = new JRadioButton(getString("MEDIANJPG")+" ");  bg.add(b);  p1.add(b); 
      radioMediane=b; b.setSelected(true);
      b = new JRadioButton(getString("AVERAGEJPG")); bg.add(b);  p1.add(b);  
      PropPanel.addCouple(f, p,getString("METHODJPG"), 
            getString("METHODJPGH"),
            p1,g,c, GridBagConstraints.EAST);

      p1 = new JPanel();
      gauss=b1 = new JCheckBox(getString("FILTERGAUSS")+" ");  p1.add(b1); 
      b1.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { updatePreview(); }
      });
      b1.setSelected(true);
      PropPanel.addCouple(f, p,getString("FILTERRGB"), 
            getString("FILTERRGBH"),
            p1,g,c, GridBagConstraints.EAST);

      bg = new ButtonGroup();
      p1 = new JPanel();
      b = new JRadioButton("JPEG");  bg.add(b);  p1.add(b); 
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { updatePreview(); }
      });
      formatJpeg=b; 
      b = new JRadioButton("PNG");   bg.add(b);  p1.add(b);  
      b.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) { updatePreview(); }
      });
      b.setSelected(true);
      PropPanel.addCouple(f, p, getString("FORMATTILES")+" ", 
            getString("FORMATTILESH"),
            p1,g,c, GridBagConstraints.EAST);
      
      return p;
   }
   
   // Classe permettant la gestion d'un champ de saisie soit automatique soit manuel
   class JText extends JTextField {
      boolean manual;
      
      public JText() { 
         super(TEXTSIZE);
         setHorizontalAlignment(JTextField.CENTER );
         addKeyListener( new KeyListener() {
            public void keyReleased(KeyEvent e) {
//               if( e.getKeyCode()==KeyEvent.VK_ENTER) 
              setManual( getText().trim().length()>0 );
               afterUpdate();
            }
            public void keyPressed(KeyEvent e) { }
            public void keyTyped(KeyEvent e) { }
         });
      }
      
      void setManual(boolean manual) {
         this.manual = manual;
         setFont( getFont().deriveFont(manual?Font.BOLD:Font.PLAIN));
         setForeground( manual?Color.red : Color.black );
      }
      void afterUpdate() {
         if( manual ) updatePreview();
         else updateTables();
         reset.setEnabled( manual || hasManual() );
      }
      
      public void setText(String s) {
         if( manual ) return;
         super.setText(s);
      }
      
      // Ajoute ou retranche 1/10 de la valeur (si elle est numérique)
      void moins() { incr(-1); }
      void plus()  { incr(1); }
      void incr(int sens) {
         try {
            double x = Double.parseDouble( getText() );
            double incr=x/10;
            x += incr*sens;
            super.setText(Util.myRound(x));
            setManual(true);
            afterUpdate();
         } catch( Exception e ) {};
      }
      public String toString() { return getText()+(manual?"/manual":""); }
   }
   
   // Classe gérant un Label disposant de boutons d'incréments (+) de de décrément (-) qui
   // s'appliqueront sur tous les JText passés en paramètres
   class JLab extends JPanel {
      JLabel label;
      final JText [] v;
      public JLab(String text,JText v[]) {
         this.v = v;
         JButton b = new JButton(" - ");
         b.setBorder( null );
         b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { moins(); }
         });
         add(b);
         label=new JLabel(text);
         add(label);
         b = new JButton(" + ");
         b.setBorder( null );
         b.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) { plus(); }
         });
         add(b);
      }
      void moins() { for( int i=0; i<v.length; i++ ) v[i].moins(); }
      void plus()  { for( int i=0; i<v.length; i++ ) v[i].plus(); }
   }
   
   // Génère le JPanel des paramètres de la méthode RGB classique
   private JPanel getClassicPanel() {
      JText t;
      for( int c=0; c<RGB.length; c++ ) {
         cutMin[c]    =t= new JText();  arrayJText.add(t);
         cutMiddle[c] =t= new JText();  arrayJText.add(t);
         cutMax[c]    =t= new JText();  arrayJText.add(t);
         function[c]       =t= new JText();  arrayJText.add(t);
      }
      JPanel p = new JPanel( new GridLayout(RGB.length+1,5) );
      p.add(new JLabel(""));
      p.add(new JLab("Cut min",cutMin));
      p.add(new JLab("Cut middle",cutMiddle));
      p.add(new JLab("Cut max",cutMax));
      p.add(new JLabel("Function",JLabel.CENTER));
      
      for( int c=0; c<RGB.length; c++ ) { 
         p.add( new JLabel(RGB[c],JLabel.CENTER));
         p.add( cutMin[c]);
         p.add( cutMiddle[c]);
         p.add( cutMax[c]);
         p.add( function[c]);
      }
      return p;
   }
   
   // Génère le JPanel des paramètres de la méthode RGB Lupton
   private JPanel getLuptonPanel() {
      JText t;
      JPanel p1 = new JPanel( new BorderLayout() );
      JPanel p = new JPanel( new GridLayout(RGB.length+1,3) );
      p.add( new JLabel(""));
      p.add( new JLab("Minimum",luptonM) );
      p.add( new JLab("Stretch",luptonS) );
      for( int c=0; c<RGB.length; c++ ) { 
         p.add( new JLabel(RGB[c],JLabel.CENTER));
         p.add( luptonM[c] =t= new JText());   arrayJText.add(t);
         p.add( luptonS[c] =t= new JText());   arrayJText.add(t);
      }
      p1.add(p, BorderLayout.CENTER);
      
      p = new JPanel( new GridLayout(2,1));
      p.add( new JLabel("Q",JLabel.CENTER) );
      p.add( luptonQ =t= new JText());  arrayJText.add(t);
      p1.add(p, BorderLayout.EAST);
      
      return p1;
   }
   
   // Met à jour le planBGRgb de préview (s'il existe)
   private void updatePreview() {
      if( planPreview==null ) return;
      planPreview.updatePreview();
   }

   // Crée/remplace le plan de prévisualisation montrant immédiatement le résultat du HiPS couleur qui sera généré
   // en fonction des paramètres sélectionnés.
   private void createPreview() {
      planPreview = aladin.calque.createPlanBGRgb( this, getSelectedPlan(0), getSelectedPlan(1), getSelectedPlan(2) );
   }
   
   /** Retourne le numéro de la composante manquante, -1 si aucune (0-red, 1-green, 2-blue) */
   public int getMissing() { 
      for( int c=0; c<3; c++ ) {
         Object p = getSelectedPlan(c);
         if( p==null ) return c;
      }
      return -1; 
   }
   
   // retourne le plan correspondant à la position du sélection numéro c
   protected PlanBG getSelectedPlan(int c) { 
      Object o = ch[c].getSelectedItem();
      return o instanceof String ? null : (PlanBG) ch[c].getSelectedItem(); 
   }

   
   /** Retourne true si le filtre de réduction de bruit (gauss) est activé */
   public boolean getGauss() { return gauss.isSelected(); }

   /** Retourne les 3 plans sélectionnés */
   public PlanBG [] getSelectedPlans() {
      return new PlanBG[] { getSelectedPlan(0), getSelectedPlan(1), getSelectedPlan(2) };
   }
   
   /** Indique la méthode de calcul RGB sélectionnée: 0-classic, 1-lupton */
   public int getRGBMethod() { return classic.isSelected() ? 0 : 1; }
   
   /** Retourne la méthode qu'il faudra utiliser pour construire les JPG couleur */
   public JpegMethod getHierarchyAlgo() {
      if( radioMediane.isSelected() ) return Context.JpegMethod.MEDIAN;
      return Context.JpegMethod.MEAN;
   }

   /** Retourne le format de codage pour les tuiles couleurs (JPEG ou PNG) */
   public int getFormat() {
      if( formatJpeg.isSelected() ) return Constante.TILE_JPEG;
      return Constante.TILE_PNG;
   }
   
   /** Retourne le paramètres min, middle et max (bscale et bzero déjà appliqués) pour la composante c */
   public double getPixelMin(int c)    { return Double.parseDouble( cutMin[c].getText() ); }
   public double getPixelMiddle(int c) { return Double.parseDouble( cutMiddle[c].getText() ); }
   public double getPixelMax(int c)    { return Double.parseDouble( cutMax[c].getText() ); }
   
   /** Retourne le paramètres cutmin, cutmiddle et cutmax (bscale et bzero non appliqués) pour la composante c */
   public double getCutMin(int c)      { return noScaleAndZero(c, Double.parseDouble( cutMin[c].getText()) ); }
   public double getCutMiddle(int c)   { return noScaleAndZero(c, Double.parseDouble( cutMiddle[c].getText()) ); }
   public double getCutMax(int c)      { return noScaleAndZero(c, Double.parseDouble( cutMax[c].getText()) ); }
   
   /** Retourne la fonction de transfert en paramètre pour la composante c */
   public String getTransfertFct(int c){ return function[c].getText(); }
   
   /** Retourne les différents paramètres de l'algo de Lupton pour la composante c */
   public double getLuptonM(int c)     { return Double.parseDouble( luptonM[c].getText() ); }
   public double getLuptonS(int c)     { return Double.parseDouble( luptonS[c].getText() ); }
   public double getLuptonQ()          { return Double.parseDouble( luptonQ.getText() ); }
   
   // "dés'applique" le bscale et le bzero sur la valeur passée en paramètre pour la composante couleur c
   private double noScaleAndZero(int c,double val) {
      PlanBG p = getSelectedPlan(c);
      return (val-p.bZero)/p.bScale;
   }
   
   /** Réinitialise les paramètres par défaut des algos classic et Lupton (supprime les choix manuels) et remet à jour les previez éventuel */
   public void resetTables() {
      for( JText t : arrayJText ) t.setManual(false);
      updateTables();
      updatePreview();
   }
   
   /** Retourne true si au-moins un paramètre a été modifié manuellement */
   public boolean hasManual() {
      for( JText t : arrayJText ) if( t.manual ) return true;
      return false;
   }
   
   /** Met à jour tous les paramètres par défaut des algos classic et Lupton en fonction des HiPS N&B sélectionnés */
   public void updateTables() { initTab( false ); }
  
   /** Initialise tous les paramètres par défaut des algos classic et Lupton en fonction des HiPS N&B sélectionnés */
   private void initTab() { initTab( true ); }
   public void initTab(boolean alsoQ) {
      if( cutMin[0]==null ) return;   // pas encore créé
      for( int c=0; c<3; c++ ) {
         PlanBG p = getSelectedPlan(c);
         cutMin[c].setText(    p==null ? "--" : Util.myRound( p.getPixelCtrlMin() ));
         cutMiddle[c].setText( p==null ? "--" : Util.myRound( p.getPixelCtrlMiddle()) );
         cutMax[c].setText(    p==null ? "--" : Util.myRound( p.getPixelCtrlMax()) );
         function[c].setText(       p==null ? "--" : p.getTransfertFctInfo() );
         luptonM[c].setText(    p==null ? "--" : Util.myRound( BuilderRgb.estimateLuptonM(p)) );
         luptonS[c].setText(    p==null ? "--" : Util.myRound( BuilderRgb.estimateLuptonS(p)) );
      }
      if( alsoQ ) luptonQ.setText("20");
   }
   
   
   protected String getLabelSelector(int i) {
      return i == 0?R:i == 1?G:i==2?B:"";
   }

   protected Color getColorLabel(int i) {
      return i == 0?Color.red:i == 1?Color.green:i==2?Color.blue:Color.black;
   }

   private void createChaine(Chaine chaine) {
      BROWSE = getString("FILEBROWSE");
      REP_DEST = getString("REPDALLSKY");
      RGBALLSKY = getString("RGBALLSKY");
      HELP = getString("HELPRGBALLSKY");
      titlehelp = getString("HHELP");
      R = getString("RGBRED");
      G = getString("RGBGREEN");
      B = getString("RGBBLUE");
   }

   private String getString(String k) { return mainPanel.aladin.getChaine().getString(k); }

   /** Recupere la liste des plans HiPS valides */
   protected PlanBG[] getPlan() {
      try {
         Vector<Plan> v = aladin.calque.getPlanBG();
         if( v==null ) return new PlanBG[0];
         
         // Dénombrement
         Object [] x = v.toArray();
         int n = x.length;
         for( Object p : x ) {
            if( ((PlanBG)p).isColored() ) n--;
            else if( !((PlanBG)p).canbeTruePixels() ) n--;
            else if( !Aladin.PROTO && !((PlanBG)p).isLocalAllSky() ) n--;
         }
         
         // Copie
         PlanBG [] pi = new PlanBG[n];
         int i=0;
         for( Object p : x ) {
            if( ((PlanBG)p).isColored() ) continue;
            else if( !((PlanBG)p).canbeTruePixels() ) continue;
            else if( !Aladin.PROTO && !((PlanBG)p).isLocalAllSky() ) continue;
            pi[i++]=(PlanBG)p;
         }
         
         // On tri en fonction des niveaux d'énergie
         Arrays.sort(pi,compa);
         
         return pi;
      } catch( Exception e ) {
         e.printStackTrace();
         return new PlanBG[]{};
      }
   }
   
   private Compa compa = new Compa();
   class Compa<T> implements Comparator<T> {
      public int compare(T o1, T o2) {
         Plan p1=(Plan)o1;
         Plan p2=(Plan)o2;
         return p1.getEnergy() == p2.getEnergy() ? 0 : p1.getEnergy() < p2.getEnergy() ? 1 : -1;
      }
   }

   private JLabel tileStat,timeStat;

   private JPanel createStatPanel() {
      GridBagLayout g = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,10,2,2);
      JPanel p = new JPanel(g);

      tileStat = new JLabel("--");
      PropPanel.addCouple(p, ".RGB tiles: ", tileStat, g, c);

      timeStat = new JLabel("--");
      PropPanel.addCouple(p, ".Time: ", timeStat, g, c);

      return p;
   }

   protected void setStat(String sTile,String sTime) {
      tileStat.setText(sTile);
      timeStat.setText(sTime);
   }

   public void actionPerformed(ActionEvent e) {
           if (e.getSource() == start )     start();
      else if (e.getSource() == abort)      abort();
      else if (e.getSource() == pause)      pause();
      resumeWidgets();
   }

   // Retourne la ligne de commande à utiliser pour faire la même chose en script
   private String getCmdLine() {
      int missing = getMissing();
      StringBuilder cmd = new StringBuilder();
      for (int c=0; c<3; c++) {
         if( c==missing ) continue;
         PlanBG p = getSelectedPlan(c);
         cmd.append( " in"+RGB[c]+"=\""+p.getUrl()+"\"");
      }
      cmd.append( getFormat()==Constante.TILE_PNG ? " color=png" :" color=jpg");
      if( getGauss() ) cmd.append(" filter=gauss");
      
      boolean flagLupton = getRGBMethod()==1;
      if( flagLupton ) {
         StringBuilder m = new StringBuilder();
         StringBuilder s = new StringBuilder();
         double mx=0,om=Double.NaN,sx=0,os=Double.NaN;
         boolean sameM=true, sameS=true;
         for (int c=0; c<3; c++) {
            if( m.length()>0 ) m.append('/');
            m.append( mx=getLuptonM(c));
            if( s.length()>0 ) s.append('/');
            s.append( sx=getLuptonS(c));
            if( !Double.isNaN(om) && mx!=om ) sameM=false;
            om=mx;
            if( !Double.isNaN(os) && sx!=os ) sameS=false;
            os=sx;
         }
         cmd.append(" luptonM="+(sameM ? mx+"" : m));
         cmd.append(" luptonS="+(sameS ? sx+"" : s));
         cmd.append(" luptonQ="+getLuptonQ());

         // Paramètre de la méthode RGB classique   
      } else {
         for (int c=0; c<3; c++) {
            if( c==missing ) continue;
            PlanBG p = getSelectedPlan(c);
            context.setRgbInput(p.getUrl(), c);
            String s = getPixelMin(c)+" "+getPixelMiddle(c)+" "+getPixelMax(c);
            String f = getTransfertFct(c);
            if( !f.equalsIgnoreCase("linear") ) s=s+" "+f;
            cmd.append( " cm"+RGB[c]+"=\""+s+"\"");
         }
      }
      
      cmd.append(" out=\""+outputField.getText()+"\" RGB");
      return cmd.toString();
   }
   
   
   // Lance la génération
   private void start() {
      
      if( !hasPlanSelection() ) {
         Aladin.error("Select two or three original HiPS");
         return;
      }

      // Verifie si le champ du répertoire de sortie n'est pas vide ou invalide
      String out = outputField.getText();
      if( "".equals(out) || !(new File(out)).getParentFile().isDirectory() ) {
         Aladin.error("Choose an output directory");
         return;
      }
      File fout = new File(out); 
      if( !fout.exists() ) fout.mkdir();
      if(  !fout.isDirectory() || !fout.canWrite()) {
         Aladin.error("Cannot create or write in the output directory");
         return;
      }
      
      try {
         boolean flagLupton = getRGBMethod()==1;
         int missing = getMissing();
         
         // Les paramètres Lupton
         if( flagLupton ) {
            StringBuilder m = new StringBuilder();
            StringBuilder s = new StringBuilder();
            for (int c=0; c<3; c++) {
               PlanBG p = getSelectedPlan(c);
               if( p!=null ) context.setRgbInput(p.getUrl(), c);
               if( m.length()>0 ) m.append('/');
               m.append( getLuptonM(c));
               if( s.length()>0 ) s.append('/');
               s.append( getLuptonS(c));
            }
            context.setRgbLuptonM(m.toString());
            context.setRgbLuptonS(s.toString());
            context.setRgbLuptonQ( getLuptonQ()+"");
            
            // Paramètre de la méthode RGB classique   
         } else {
            for (int c=0; c<3; c++) {
               if( c==missing ) continue;
               PlanBG p = getSelectedPlan(c);
               context.setRgbInput(p.getUrl(), c);
               String s = getPixelMin(c)+" "+getPixelMiddle(c)+" "+getPixelMax(c);
               String f = getTransfertFct(c);
               if( !f.equalsIgnoreCase("linear") ) s=s+" "+f;
               context.setRgbCmParam(s, c);
            }
         }

         // Les paramètres généraux
         context.setRgbOutput(outputField.getText());
         context.setHierarchyAlgo(getHierarchyAlgo());
         context.setRgbFormat(getFormat());
         if( getGauss() ) context.setFilter("gauss");

         setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         context.setProgressBar(progressBar);
         
         new Task(context, Action.RGB, false);

      } catch( Exception e1 ) {
         aladin.error(this, "RGB HiPS creation error");
         if( Aladin.levelTrace>=3 ) e1.printStackTrace();
      }
   }

   private void pause() {
      if( context.isTaskPause() ) {
         context.setTaskPause(false);
         pause.setText(getString("PAUSE"));
      } else {
         context.setTaskPause(true);
         pause.setText(getString("RESUME"));
      }
   }

   private void abort() {
      if( !Aladin.confirmation(mainPanel, "Do you really want to abort the HiPS RGB creation ?") ) return;
      context.taskAbort();
   }



   //   /** Ouverture de la fenêtre de sélection d'un fichier */
   //   private void dirBrowser(JTextField dir) {
   //      String path = dir.getText().trim();
   //      if( path.length()==0 ) path = aladin.getDefaultDirectory();
   //      String t = Util.dirBrowser(this,path);
   //      if( t==null ) return;
   //      dir.setText(t);
   //      actionPerformed(new ActionEvent(dir,-1, "dirBrowser Action"));
   //   }

   private void dirBrowser(JTextField dir) {
      String currentDirectoryPath = dir.getText().trim();
      String path = Util.dirBrowser("",currentDirectoryPath,dir,0);
      if( path==null ) return;
      actionPerformed(new ActionEvent(dir, -1, "dirBrowser Action"));
   }


   protected void init() {
      // sauvegarde les anciennes selections
      Object[] save = new Object[]{ch[0].getSelectedItem(),
            ch[1].getSelectedItem(),
            ch[2].getSelectedItem()};

      // rachaichit les combo box avec la liste des plans allsky
      PlanBG[] plans = getPlan();
      for (int i=0; i<3; i++) {
         ch[i].removeAllItems();
         ch[i].addItem(CHOOSE);
      }
      
      for (PlanBG planBG : plans) {
         for (int i=0; i<3; i++) {
            ch[i].addItem(planBG);
            // remet l'ancienne selection
            if (save[i]!=null && planBG==save[i]) ch[i].setSelectedItem(planBG);
         }
      }
      
      // Si aucune sélection on affecte les 3 premiers
      if( ch[0].getSelectedIndex()==0 && ch[1].getSelectedIndex()==0 && ch[2].getSelectedIndex()==0 ) {

         int n = ch[0].getItemCount();
         
         // si uniquement 2 plans possibles on affecte le red et le blue
         if( n==3 ) {
            ch[0].setSelectedIndex(1);
            ch[2].setSelectedIndex(2);
         } else {
            for( int i=0; i<3 && i<n-1; i++ ) ch[i].setSelectedIndex(i+1);
         }
      }

      initTab();
   }
   
   public void show() {
      super.show();
      init();
   }

   public void help() {
      JOptionPane.showMessageDialog(this, HELP, titlehelp,
            JOptionPane.INFORMATION_MESSAGE);
   }

   private boolean hasPlanSelection() {
      int nb=0;
      for( int i=0; i<ch.length; i++ ) {
         Object o = ch[i].getSelectedItem();
         if( o instanceof PlanBG ) nb++;
      }
      return nb>1;
   }

   protected void resumeWidgets() {
      try {
         boolean readyToDo = hasPlanSelection() && outputField.getText().trim().length()>0;
         boolean isRunning = context.isTaskRunning();
         start.setEnabled(readyToDo && !isRunning);
         pause.setEnabled(isRunning);
         abort.setEnabled(isRunning);
         preview.setEnabled(!isRunning);
         reset.setEnabled( hasPlanSelection() );
         updateTables();
         setCursor( isRunning ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
               : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) );
      } catch( Exception e ) { }
   }


}
