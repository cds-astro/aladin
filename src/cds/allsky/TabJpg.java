package cds.allsky;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.ColorModel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.aladin.PlanImage;
import cds.aladin.Properties;
import cds.aladin.Tool;
import cds.aladin.ToolBox;
import cds.tools.Util;

public class TabJpg extends JPanel implements ActionListener {

   private static final String OK = "Build JPGs";

   private String CUT_MAX = "Max";
   private String CUT_MIN = "Min";
   private JTextField tCutMin = new JTextField(10);
   private JTextField tCutMax = new JTextField(10);
   private JRadioButton radioManual;                      // selected si on est en mode manuel
   private JRadioButton radioAllsky;                      // selected si on est en mode allsky
   private JLabel labelMethod;                            // Texte décrivant la méthode à utiliser
   private JRadioButton radioMediane;                     // selected si on est en calcul selon la médiane
   private JRadioButton radioMoyenne;                     // selected si on est en calcul selon la moyenne
   private JLabel currentCM;                              // info détaillant le cut de la vue courante

   JButton ok = new JButton(OK);
//   private JButton bHelp = new JButton();
   protected JButton bNext = new JButton();
   protected JButton bPrevious = new JButton();
   JProgressBar progressJpg = new JProgressBar(0,100);
   private String NEXT;
   private String PREVIOUS;

   double[] cut = new double[4];
   private final MainPanel mainPanel;

   private String getString(String k) { return mainPanel.aladin.getChaine().getString(k); }

   public TabJpg(final MainPanel parent) {
      super(new BorderLayout());
      createChaine(Aladin.getChaine());
      bPrevious = new JButton(PREVIOUS);
      bPrevious.setEnabled(false);
      bPrevious.addActionListener(this);
      bNext = new JButton(NEXT);
      bNext.setEnabled(false);
      bNext.addActionListener(this);

      JRadioButton rb;
      ButtonGroup bg = new ButtonGroup();

      mainPanel = parent;
      JLabel label;
      GridBagConstraints c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.anchor = GridBagConstraints.WEST;
      c.fill = GridBagConstraints.NONE;
      c.gridwidth = GridBagConstraints.REMAINDER;

      JPanel pCenter = new JPanel();
      pCenter.setLayout(new GridBagLayout());
      pCenter.setBorder(BorderFactory.createEmptyBorder(5, 25, 5,25));

      // Texte d'intro
      //		c.fill = GridBagConstraints.HORIZONTAL;
      label = new JLabel(Util.fold(getString("JPEGINFOALLSKY"),80,true));
      label.setFont(label.getFont().deriveFont(Font.ITALIC));
      c.gridheight = 5;
      c.insets.bottom=20;
      pCenter.add(label,c);
      c.insets.bottom=0;
      c.gridy++;c.gridy++;c.gridy++;c.gridy++;c.gridy++;
      c.gridheight = 1;

      // détermine le mode par défaut (automatique si vue courante est un all-sky en fits, sinon manuel)
      currentCM = new JLabel();
      boolean manualSelected = !updateCurrentCM();

      c.gridx = 0;
      c.gridy++;
      radioManual = rb = new JRadioButton(getString("JPEGMCUTALLSKY"));
      rb.setSelected(manualSelected);
      tCutMin.setEnabled(manualSelected);
      tCutMax.setEnabled(manualSelected);
      rb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean enable = radioManual.isSelected();
            tCutMin.setEnabled(enable);
            tCutMax.setEnabled(enable);
         }
      });
      rb.setFont( rb.getFont().deriveFont(Font.BOLD));
      bg.add(rb);
      pCenter.add(rb,c);
      c.gridx++;
      c.gridwidth=GridBagConstraints.REMAINDER;
      pCenter.add(Util.getHelpButton(this,getString("HELPJPEGMCUTALLSKY")),c);

      c.gridwidth=2;
      c.gridx = 0;
      c.gridy++;
      JPanel minmax = new JPanel(new FlowLayout());
      label = new JLabel(CUT_MIN);
      minmax.add(label);
      minmax.add(tCutMin);
      label = new JLabel(CUT_MAX);
      minmax.add(label);
      minmax.add(tCutMax);
      pCenter.add(minmax, c);

      c.gridwidth=1;
      c.gridx = 0;
      c.gridy++;
      int m=c.insets.top;
      c.insets.top=20;
      radioAllsky = rb = new JRadioButton(getString("JPEGCUTALLSKY"));
      rb.setSelected(!manualSelected);
      rb.addActionListener( new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            boolean enable = radioManual.isSelected();
            tCutMin.setEnabled(enable);
            tCutMax.setEnabled(enable);
         }
      });
      rb.setFont( rb.getFont().deriveFont(Font.BOLD));
      rb.setSelected(true);
      bg.add(rb);
      pCenter.add(rb,c);
      
      c.gridx++;
      c.gridwidth=GridBagConstraints.REMAINDER;
      pCenter.add(Util.getHelpButton(this,getString("HELPJPEGCUTALLSKY")),c);
      c.insets.top=m;
      
      c.gridx = 0;
      c.gridy++;
      pCenter.add(currentCM,c);

      c.gridx=0;
      c.gridy++;
      m=c.insets.top;
      c.insets.top=20;
      JPanel p = new JPanel();
      JLabel l;
      labelMethod = l = new JLabel(getString("METHODJPG"));
      l.setFont(l.getFont().deriveFont(Font.BOLD));
      p.add(l);
      ButtonGroup bg1 = new ButtonGroup();
      radioMediane = rb = new JRadioButton(getString("MEDIANJPG"));
      rb.setSelected(true);
      bg1.add(rb);
      p.add(rb);
      radioMoyenne = rb = new JRadioButton(getString("AVERAGEJPG"));
      bg1.add(rb);
      p.add(rb);
      pCenter.add(p,c);
      c.insets.top=m;

      c.gridwidth=1;
      c.gridx=0;
      c.gridy++;
      c.insets.top=30;
      ok.addActionListener(this);
      ok.setEnabled(false);
      c.insets.top=1;
      pCenter.add(ok, c);

      // barre de progression
      progressJpg.setStringPainted(true);
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = GridBagConstraints.REMAINDER;
      c.gridy++;c.gridx=0;
      JPanel pProgress = new JPanel(new BorderLayout());
      pProgress.setBorder(new EmptyBorder(0, 55, 15, 55));
      pProgress.add(progressJpg,BorderLayout.CENTER);
      pProgress.add(createStatPanel(),BorderLayout.SOUTH);
      
      // boutons
      JPanel fin = new JPanel(new BorderLayout());
      JPanel pBtn = new JPanel();
      pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
      pBtn.add(Box.createHorizontalGlue());
      pBtn.add(bPrevious);
      ok.setText(getString("JPEGBUILDALLSKY"));
      pBtn.add(ok);
      pBtn.add(Box.createRigidArea(new Dimension(10,0)));
      pBtn.add(bNext);
      pBtn.add(Box.createHorizontalGlue());
      fin.add(pProgress, BorderLayout.NORTH);
      fin.add(pBtn, BorderLayout.CENTER);

      // composition du panel principal
      add(pCenter, BorderLayout.CENTER);
      add(fin, BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
   }

   private void createChaine(Chaine chaine) {
      NEXT = chaine.getString("NEXT");
      PREVIOUS = chaine.getString("PREVIOUS");
   }
   
   private JLabel tileStat,timeStat;
   
   private JPanel createStatPanel() {
      GridBagLayout g = new GridBagLayout();
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(2,10,2,2);
      JPanel p = new JPanel(g);

      tileStat = new JLabel("--");
      Properties.addCouple(p, ".Jpeg tiles: ", tileStat, g, c);           

      timeStat = new JLabel("--");
      Properties.addCouple(p, ".Time: ", timeStat, g, c);           

      return p;
   }
   
   protected void setStat(int nbTile,long sizeTile,long time) {
      String s;
      if( nbTile==-1 ) s="";
      else s= nbTile+" tile"+(nbTile>1?"s":"") + " for "+Util.getUnitDisk(sizeTile);
      tileStat.setText(s);
      if( time==-1 ) s="";
      else s= Util.getTemps(time,true);
      timeStat.setText(s);
   }
   
   protected void resumeWidgetsStatus() {
      boolean hasData = mainPanel.isExistingDir();
      boolean readyToDo = hasData && mainPanel.isExistingAllskyDir();
      boolean isRunning = mainPanel.isRunning();
      bPrevious.setEnabled(hasData && !isRunning);
      bNext.setEnabled(readyToDo && !isRunning);
      tCutMin.setEnabled(hasData && !isRunning);
      tCutMax.setEnabled(hasData && !isRunning);
      radioManual.setEnabled(readyToDo && !isRunning);
      labelMethod.setEnabled(readyToDo && !isRunning);
      radioAllsky.setEnabled(readyToDo && !isRunning);
      radioMediane.setEnabled(readyToDo && !isRunning);
      radioMoyenne.setEnabled(readyToDo && !isRunning);
      progressJpg.setEnabled(readyToDo && !isRunning);
      ok.setEnabled(readyToDo && !isRunning);
      setCursor( isRunning ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR) ); 
   }

   public void clearForms() {
      tCutMin.setText("");
      tCutMax.setText("");
      cut = new double[4];
      progressJpg.setValue(0);
   }

   public void setStartEnabled(boolean enabled) {
      ok.setEnabled(enabled);
      bNext.setEnabled(enabled);
   }

   public void setCut(double[] cut) {
      // affiche les valeurs réelles avec bscale et bzero
      double[] bb = mainPanel.getBScaleBZero();
      tCutMin.setText(Util.myRound(cut[0]*bb[0]+bb[1]));
      tCutMax.setText(Util.myRound(cut[1]*bb[0]+bb[1]));
      this.cut = cut;
   }

   public double[] getCut() {

      // Manuellement, il faut récupérer les valeurs dans les champs de saisie
      if( radioManual.isSelected() ) {
         String s = tCutMin.getText();
         // convertit pour garder les valeurs codées sans bscale et bzero
         double[] bb = mainPanel.getBScaleBZero();
         try {
            cut[0] = (Double.parseDouble(s)-bb[1])/bb[0];
            s = tCutMax.getText();
            cut[1] = (Double.parseDouble(s)-bb[1])/bb[0];
         } catch (NumberFormatException e) {
            cut[0] = 0; cut[1] = 0;
         }

         // Sinon il faut chercher le min..max dans le plan de la vue courante
      } else {
         Plan p = mainPanel.aladin.calque.getPlanBase();
         cut[0]= ((PlanImage)p).getCutMin();
         cut[1]= ((PlanImage)p).getCutMax();
      }
      return cut;
   }

   /** Retourne la table des couleurs de la vue courante, ou null si le mode de cut est positionné manuellement */
   public ColorModel getCM() {
      if( radioManual.isSelected() ) return null;
      return ((PlanImage) mainPanel.aladin.calque.getPlanBase() ).getCM();
   }
   
   /**   retourne la méthode qu'il faudra utiliser pour construire les JPG */
   public int getMethod() {
      if( radioMediane.isSelected() ) return BuilderJpg.MEDIANE;
      return BuilderJpg.MOYENNE;
   }

   //	public void setTransfertFct(int fct) {
   //		transfertFct = fct;
   //	}

   public void show() {
      updateCurrentCM();
      resumeWidgetsStatus();
      super.show(); 
   }

   public boolean updateCurrentCM() {
      boolean rep=true;
      String s;
      try {
         PlanBG p = (PlanBG) mainPanel.aladin.calque.getPlanBase();
         if( !p.isTruePixels() ) throw new Exception();
         s="<html><i>"+"Pixels:<b> "+p.getPixelMinInfo()+" .. "+p.getPixelMaxInfo()+"</b> from "+p.getDataMinInfo()+" .. "+p.getDataMaxInfo()+" - " +
         "Transfert function: <b>"+p.getTransfertFctInfo()+"</b></i>";
      } catch( Exception e1 ) {
         s="<html><i>No allsky presently displayed !</i>";
         rep=false;
      }
      currentCM.setText(s);
      return rep;
   }

   protected JComponent noBold(JComponent c) {
      c.setFont(c.getFont().deriveFont(Font.PLAIN));
      return c;
   }

   public void setProgress(int value) {
      progressJpg.setValue(value);
   }

   public void actionPerformed(ActionEvent e) {
      if (e.getSource() == ok ) {

         // Juste pour vérifier qu'on a bien un plan all-sky valable en cours de visualisation
         if( !radioManual.isSelected() ) {
            try {
               PlanBG p = (PlanBG) mainPanel.aladin.calque.getPlanBase();
               if( !p.isTruePixels() ) throw new Exception();
            } catch( Exception e1 ) {
               mainPanel.aladin.warning(mainPanel,"<html>There is no current view,<br>or the current view is not an all-sky view in true pixel mode");
               return;
            }
         }
         setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         BuilderJpg builderJpg = new BuilderJpg(getCut(), getCM(), getMethod(), mainPanel.context );
         builderJpg.start();
         (new ThreadProgressBar(builderJpg)).start();
         
      } else if (e.getSource() == bNext) {
         mainPanel.showPubTab();
         
      } else if (e.getSource() == bPrevious) {
         mainPanel.showBuildTab();
      }
   }

   class ThreadProgressBar implements Runnable {
      Object builderJpg;
      public ThreadProgressBar(Object source) {
         builderJpg = source;
      }

      public void start(){
         // lance en arrière plan le travail
         (new Thread(this)).start();
      }
      public void run() {
         int value = 0;
         while(builderJpg != null && value < 99) {
            value = (int)((BuilderJpg)builderJpg).getProgress();
            setProgress(value);
            try {
               Thread.currentThread().sleep(200);
            } catch (InterruptedException e) {
            }
         }
         setProgress(100);
         setCursor(null);
         mainPanel.showPubTab();
      }
   }

}
