// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import cds.aladin.prop.PropPanel;
import cds.astro.AstroMath;
import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.fits.HeaderFits;
import cds.tools.Util;

/**
 * Gestion de la fenetre associee a la creation d'une calib manuelle
 *
 * @author Pierre Fernique [CDS], François Bonnarel [CDS]
 * @version 1.2 : (11 avril 2005) Incorporation du bestPoint de François B.
 * @version 1.1 : (20 aout 2002) Mise en place de la methode WCS
 * @version 1.0 : (23 avril 2002) Creation
 */
public final class FrameNewCalib extends JFrame
             implements ActionListener,ChangeListener {
   static final int SIMPLE=0;		// Methode par parametres
   static final int QUADRUPLET=1;	// Methode par echantillonage
   static final int WCS=2;		// Methode par header WCS

   static int num=0;			// Numero de la projection perso

   String TITLE,SUBMIT,MODIFY,UNDO,REDO,HELP,RESET,CLEAR,CANCEL,
          ONE,TWO,THREE,HELPSTRING,MYPROJ,ADJUST1,ADJUST2,TRUE,FALSE,
          CHOOSECAL,LABEL,FRAME,COORD,XY,PIXSIZE,PROJECT,ROT,RASYM,QUADMSG,
          XYPOS,ERRNOXY,ERRNOBEST/*,COPYCAL*/,ERRIMG;


   // Les references aux objets
   Aladin a;
   Plan   plan;			// Le plan pour lequel on cree la nouvelle calibration
   Projection oldp;		// En cas de modif, precedente projection

   // Les widgets
   MyLabel titleFrame;
   JTextField labelT,cooT,xyT,xyS,rotT;
   JTextArea wcsT;		          // Editeur pour le WCS
   JComboBox frameChoice=null;    // Choice du système de coordonnées
   JComboBox projChoice=null;     // Choice des projections
   JRadioButton trueSym, falseSym;// Checkbox pour la symetrie
   ButtonGroup symRadio=null;	  // Boutons radios pour la symetrie
   JPanel pSym;			          // JPanel des boutons trueSym et falseSym
   JButton modifyButton,resetButton;
   int maxPosT=7;		          // Le nombre max d'etoiles
   FrameNewCalibTextField xyPosT[];		      // Textfields des XY
   FrameNewCalibTextField cooPosT[];		      // textfields des coordonnees associees

   // Les variables correspondantes aux widgets
   String label;
   double raj,dej,cx,cy,rm,r,rot;
   boolean sym;
   int t;			// Type de la projection
   int system;      // Systeme de coordonnées

   JTextField focusTextField=null;// Component ayant le focus (parmi xyPosT[] et cooPosT[])
   boolean flagXY=false;	     // true si le focus est sur un element de xyPosT[]

   JPanel panelOne,panelTwo,panelThree;	// JPanels methode One, Two et Three
//   CardLayout cardMethod;               // Gere la permutation entre les methodes
   JTabbedPane panelMethod;
   private JButton undoButton,redoButton;
   int modeCalib;			 // methode SIMPLE, QUADRUPLET ou WCS

   private Stack undo,redo; // Gestion du UNDO et REDO

   protected void createChaine() {
      TITLE = a.chaine.getString("NCTITLE");
      SUBMIT = a.chaine.getString("NCSUBMIT");
      MODIFY = a.chaine.getString("NCMODIFY");
      UNDO = a.chaine.getString("NCUNDO");
      REDO = a.chaine.getString("NCREDO");
      HELP = a.chaine.getString("NCHELP");
      RESET = a.chaine.getString("NCRESET");
      CLEAR = a.chaine.getString("NCCLEAR");
      CANCEL = a.chaine.getString("NCCANCEL");
      ONE = a.chaine.getString("NCONE");
      TWO = a.chaine.getString("NCTWO");
      THREE = a.chaine.getString("NCTHREE");
      HELPSTRING = a.chaine.getString("NCHELPSTRING");
      MYPROJ = a.chaine.getString("NCMYPROJ");
      ADJUST1 = a.chaine.getString("NCADJUST1");
      ADJUST2 = a.chaine.getString("NCADJUST2");
      TRUE = a.chaine.getString("NCTRUE");
      FALSE = a.chaine.getString("NCFALSE");
      CHOOSECAL = a.chaine.getString("NCCHOOSECAL");
      LABEL = a.chaine.getString("NCLABEL");
      FRAME = a.chaine.getString("UPFRAMEB");
      COORD = a.chaine.getString("NCCOORD");
      XY = a.chaine.getString("NCXY");
      PIXSIZE = a.chaine.getString("NCPIXSIZE");
      PROJECT = a.chaine.getString("NCPROJECT");
      ROT = a.chaine.getString("NCROT");
      RASYM = a.chaine.getString("NCRASYM");
      QUADMSG = a.chaine.getString("NCQUADMSG");
      XYPOS = a.chaine.getString("NCXYPOS");
      ERRNOXY = a.chaine.getString("NCERRNOXY");
      ERRIMG = a.chaine.getString("NCERRIMG");
      ERRNOBEST = a.chaine.getString("NCERRNOBEST");
//      COPYCAL = a.chaine.getString("NCCOPYCAL");

   }


  /** Creation du Frame gerant la creation/modification d'une projection
   * @param aladin Reference
   * @param p Le plan pour lequel la projection est faite
   * @param oldp une eventuelle precedente projection a modifier (ou null sinon)
   */
   protected FrameNewCalib(Aladin aladin,Plan p,Projection oldp) {
      super();
      this.a = aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(TITLE);

      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false, aladin);
      this.plan=p;
      this.oldp=oldp;
      setLocation(Aladin.computeLocation(this));

      createPanel();
      pack();
      majFrameNewCalib(p,oldp);
   }

  /** Mise a jour du Frame gerant la creation/modification d'une projection
   * @param p Le plan pour lequel la projection est faite
   * @param oldp une eventuelle precedente projection a modifier (ou null sinon)
   */
   protected void majFrameNewCalib(Plan p) { majFrameNewCalib(p,null); }
   protected void majFrameNewCalib(Plan p,Projection oldp) {
      plan=p;
      this.oldp=oldp;
      modifyButton.setText(oldp==null?SUBMIT:MODIFY);
      initUndo();
      setData();
      show();
      a.calque.zoom.wenOn();
      resumeFlagPlanRecalibrating();
   }

   /** Initialisation des structures pour la gestion de UNDO REDO */
   private void initUndo() {
      undo = new Stack();
      redo = new Stack();
      undoButton.setEnabled(false);
      redoButton.setEnabled(false);
   }

   /** Mémorisation d'une projection pour un UNDO ultérieur */
   private void setUndo(Projection p) {
      redo = new Stack();
      undo.push(p.copy());
   }

   /** Retourne true si un UNDO est possible */
   private boolean undoable() { return !undo.empty(); }

   /** Retourne true si un REDO est possible */
   private boolean redoable() { return !redo.empty(); }

   /** Effectue un UNDO */
   private void undo() throws Exception {
      Projection p = (Projection)undo.pop();
      redo.push(plan.projd.copy());
      oldp=plan.projd=p;
      setData(p,true);
      submit(true);
   }

   /** Effectue un REDO */
   private void redo() throws Exception {
      Projection p = (Projection)redo.pop();
      undo.push(plan.projd.copy());
      oldp=plan.projd=p;
      setData(p,true);
      submit(true);
   }

  /* Retourne un nouveau nom de projection */
   private String getNewLabel() {
      return MYPROJ+" "+(++num);
   }

//   /** Positionne la projection du plan dans le formulaire */
//   protected void setThisProj(Plan p) {
//      if( plan == p ) return;
//      if( !Aladin.confirmation(a,COPYCAL) ) return;
//      modeCalib = WCS;
//      setData(p.projd,false);
//      modifyButton.setEnabled(true);
//   }

   // Initialisation des Widgets en fonction des variables de classes
   // utilise a l'ouverture de la fenetre ou lors d'un reset
   private void setData() { setData(plan.projd,false); }
   private void setData(Projection proj,boolean modifModeCalib) {
      double r1,rm1;	// En cas de calibration d'image non carré
      r1=rm1=0.;

      titleFrame.setText(oldp==null?
                              ADJUST1+" \""+plan.label+"\"":
                              ADJUST2+" \""+plan.label+"\"");


      // Si je n'ai pas de XY originaux, je vais les prendre
      // à partir de la dernière projection utilisée
      if( plan.isSimpleCatalog() && !plan.hasXYorig ) ((PlanCatalog)plan).setXYorig();

      if( Projection.isOk(proj) ) {
         label=(oldp!=null)?proj.label:getNewLabel();
         try {
            Coord co = proj.c.getProjCenter();
            raj=co.al;
            dej=co.del;
            cx=co.x;
            cy=co.y;
            rm=proj.c.getImgWidth()*60;
            rm1=proj.c.getImgHeight()*60;
            r=proj.c.getImgSize().width;
            r1=proj.c.getImgSize().height;
            rot=proj.c.getProjRot();
            sym=proj.c.getProjSym();
            system=proj.c.getSystem();
            t=proj.c.getProj();
         } catch( Exception e) { System.err.println("Error on projd: "+e); }
      } else {
         label=getNewLabel();
         raj=dej=cx=cy=r=r1=rot=0.0;
         sym=false;
         system=Calib.ICRS;
         t=1;
         if( plan.isImage() ) {
            r=((PlanImage)plan).naxis1;
            r1=((PlanImage)plan).naxis2;
            cx=((PlanImage)plan).naxis1/2;
            cy=((PlanImage)plan).naxis2/2;
            rm=rm1=r/60.;   // 1" par défaut
            if( plan.hasFitsHeader() ) {
               try {
                  String ra = plan.headerFits.getStringFromHeader("OBJCTRA");
                  String de = plan.headerFits.getStringFromHeader("OBJCTDEC");
                  Astrocoo c = new Astrocoo();
                  c.set(ra+" "+de);
                  raj=c.getLon();
                  dej=c.getLat();
               } catch( Exception e ) {}
            }
         } else if( plan.isSimpleCatalog() && plan.hasXYorig ) {
            double m[] = new double[4];
            ((PlanCatalog)plan).getXYRange(m);
            r1=r=Math.max(m[1]-m[0],m[3]-m[2]);   // r = plus grands deltaX ou deltaY
            cx = m[0]+(m[1]-m[0])/2;           // centre du nuage en X
            cy = m[2]+(m[3]-m[2])/2;           // centre du nuage en Y
         }
      }

      labelT.setText(label);
      cooT.setText(Coord.getSexa(raj,dej,"s"));
      xyT.setText(Util.myRound(cx+"",2)+" "+Util.myRound(cy+"",2));
      xyS.setText(rm==0||r==0?"1\"":Coord.getUnit((rm/60.)/r));
      rotT.setText(rot+"");
      setSymRadio(sym);
      setFrameChoice(system);
      setProjChoice(t>=0?t:0);
      setCoo(proj);

      // Determination du mode de creation/modification de la projection
      if( modifModeCalib ) {
         if( proj!=null ) {
            modeCalib=SIMPLE;
            if( proj.modeCalib==Projection.QUADRUPLET ) modeCalib=QUADRUPLET;
            else if( proj.modeCalib==Projection.WCS ) modeCalib=WCS;
         }
      }

      panelMethod.setSelectedIndex(modeCalib);
      if( modeCalib==QUADRUPLET ) {
//         xyPosT[0].requestFocus();
         setFocusPos(xyPosT[0]);
         a.calque.repaintAll();
      }

      // Dans le cas ou l'on veut editer le WCS
      if( proj==null ) {
         getWCS(new Projection(label,Projection.SIMPLE,raj,dej,rm,rm1,cx,cy,r,r1,rot,sym,Calib.TAN,system,plan));
      } else getWCS(proj);

      pack();
   }

   /**
    * Modification du Widget du pixel du centre de la projection en cours
    * d'un glissement par clic-and-drag.
    * METHODE : Calcul la différence en alpha et delta correspondante au déplacement
    * grâce à la projection associée à la vue, reporte cette différente sur le centre
    * de projection RA,DEC de la calibration à modifier
    * @param orig point d'origine du déplacement dans v (pixels images)
    * @param orig point d'arrivée du déplacement dans v (pixels images)
    * @param v vue du déplacement
    */
   protected void newXY(PointD orig, PointD dest,ViewSimple v) {
      if( modeCalib!=SIMPLE ) return;
//System.out.println("Je déplace de "+orig+" à "+dest+" dans "+v);
      Coord c1 = new Coord();
      Coord c2 = new Coord();
      double diffAlpha=0.,diffDelta=0.;

//      double dx,dy;
      c1.x=orig.x; c1.y=orig.y;
      c2.x=dest.x; c2.y=dest.y;
//      dx = c1.x-c2.x;
//      dy = c1.y-c2.y;
      Plan ref = v.pref;
      Projection proj=v.getProj();
      try {
         if( ref!=null && Projection.isOk(proj) ) {
            proj.getCoord(c1);
            proj.getCoord(c2);
            if( !Double.isNaN(c1.al) && !Double.isNaN(c2.al) ) {
               double dra = c2.al-c1.al;
               double dde = c2.del - c1.del;
               double drac = dra*AstroMath.cosd(c1.del);
               diffAlpha = drac;
               diffDelta = dde;
            }
         }

         Coord co = new Coord(raj,dej);
//System.out.println("Centre projection de p "+co.getSexa()+" al="+co.al+", del="+co.del);
         co.al+=diffAlpha;
         co.del+=diffDelta;
//System.out.println("   Nouveau centre "+co.getSexa()+" al="+co.al+", del="+co.del);

         cooT.setText(co.getSexa(" "));
         modifyButton.setEnabled(true);


      } catch( Exception e ) { return; }
   }

   // Clear des Widgets
   private void clear() {
     cooT.setText("");
     xyT.setText("");
     xyS.setText("");
     rotT.setText("0");
     setSymRadio(sym);
     setFrameChoice(system);
     setProjChoice(t >= 0 ? t : 0);
     setCoo(null);
     getWCS(null);
   }
   /** Construction de l'entete WCS FITS dans le TextArea wcsT concerne
    * en fonction de la projection, ou vide sinon
    */
    private void getWCS(Projection p) {
       wcsT.setText("");
       if( p==null ) return;
       wcsT.setText( p.getWCS() );
    }

  // Mise a jour du champ rm en fonction du champ xyS et r
   private void updaterm() {
      double x = Server.getAngleInArcmin(xyS.getText(),Server.RADIUS);
      if( r<=0 ) r=1024;
      rm = x*r;
   }
   
   // Creation ou mise a jour du choix du système de coordonnées
   private void setFrameChoice(int system) {
      if( frameChoice==null ) frameChoice = Localisation.createFrameComboBis();
      try {
         frameChoice.setSelectedIndex(system);
      } catch( Exception e ) {
         frameChoice.setSelectedIndex(0);
      }
   }

   // Creation ou mise a jour du choix des projections
   // @param type l'indice de la projection courante
   private void setProjChoice(int type) {
      if( projChoice==null ) projChoice = new JComboBox(Projection.getAlaProj());
      int i = Projection.getAlaProjIndex( Calib.getProjName(type));
      if( i>=Projection.getAlaProj().length ) i=Calib.TAN;
      projChoice.setSelectedIndex(i);
   }

   // Creation ou mise a jour du choix de la symetrie
   // @param sym symetrie courante
   private void setSymRadio(boolean sym) {
      if( symRadio==null ) {
         symRadio = new ButtonGroup();
         trueSym=new JRadioButton(TRUE);
         symRadio.add(trueSym);
         trueSym.setSelected(true);
         falseSym=new JRadioButton(FALSE);
         symRadio.add(falseSym);
         pSym = new JPanel();
         pSym.add(trueSym);
         pSym.add(falseSym);
      }
      if( sym ) trueSym.setSelected(true);
      else falseSym.setSelected(true);
   }

   // Creation du panel de la fenetre
   private void createPanel() {
      JPanel p = new JPanel(new BorderLayout());

      // le titre de la fenetre
      JPanel panelT = new JPanel();
      titleFrame = new MyLabel(CHOOSECAL,
                          Label.CENTER,Aladin.ITALIC);
      panelT.add(titleFrame);

      JPanel panelH = new JPanel();
      panelH.setLayout(new BorderLayout());
      Aladin.makeAdd(panelH,panelT,"North");

      JPanel panelN = new JPanel();
      panelN.setFont(Aladin.BOLD);
      panelN.add(new JLabel(LABEL));
      panelN.add(labelT=new JTextField(20));
      Aladin.makeAdd(panelH,panelN,"Center");
//      Aladin.makeAdd(panelH,new Filet(),"South");

      Aladin.makeAdd(p,panelH,"North");

      panelT.setBackground(Aladin.BLUE);
      panelH.setBackground(Aladin.BLUE);
      panelN.setBackground(Aladin.BLUE);
      titleFrame.setBackground(Aladin.BLUE);
      p.setBackground(Aladin.BLUE);
      getContentPane().setBackground(Aladin.BLUE);


      // Le panel des methodes possibles
      panelOne   = createPanelOne();
      panelTwo   = createPanelTwo();
      panelThree = createPanelThree();
      panelMethod = new JTabbedPane();
      panelMethod.addChangeListener(this);
      panelMethod.add(ONE,panelOne);
      panelMethod.add(TWO,panelTwo);
      panelMethod.add(THREE,panelThree);

      Aladin.makeAdd(p,panelMethod,"Center");

      // Les boutons de validation
      JPanel v = valid();
      Aladin.makeAdd(p,v,"South");

      ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
      Aladin.makeAdd(this,p,"Center");
   }

   // Le panel de la methode SIMPLE
   private JPanel createPanelOne() {
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.WEST;
      c.insets.bottom=2;

      JPanel p = new JPanel(g);

      cooT =new JTextField(20);
      xyT  =new JTextField(20);
      xyS  =new JTextField(20);
      rotT =new JTextField(20);
      frameChoice = Localisation.createFrameComboBis();
      setSymRadio(sym);
      setProjChoice(t);

      PropPanel.addCouple(p,COORD, cooT, g,c);
      PropPanel.addCouple(p,XY, xyT, g,c);
      PropPanel.addCouple(p,PIXSIZE, xyS, g,c);
      PropPanel.addCouple(p,FRAME, frameChoice, g,c);
      PropPanel.addCouple(p,PROJECT, projChoice, g,c);
      PropPanel.addCouple(p,ROT, rotT, g,c);
      PropPanel.addCouple(p,RASYM, pSym, g,c);

      return p;
   }

   // Le panel de la methode QUADRUPLET
   private JPanel createPanelTwo() {
      JPanel p = new JPanel(new BorderLayout(0,0));

      xyPosT  = new FrameNewCalibTextField[maxPosT];
      cooPosT = new FrameNewCalibTextField[maxPosT];

      int scrollWidth = 150;
      int scrollHeight = 200;

      JPanel panelL = new JPanel();
      panelL.setLayout( new BorderLayout());
      panelL.setFont(Aladin.ITALIC);
      MyLabel l0 = new MyLabel(QUADMSG);
      JPanel l1 = new JPanel(); l1.setLayout( new FlowLayout(FlowLayout.CENTER));
      l1.add(new JLabel(XYPOS));
      JPanel l2 = new JPanel(); l2.setLayout( new FlowLayout(FlowLayout.CENTER));
      l2.add(new JLabel("hh mm ss +dd mm ss"));

      Aladin.makeAdd(panelL,l0,"North");
      Aladin.makeAdd(panelL,l1,"West");
      Aladin.makeAdd(panelL,l2,"East");
      Aladin.makeAdd(p,panelL,"North");

      JPanel panelScroll = new JPanel();
      panelScroll.setLayout(new GridLayout(0,2));

      for( int i=0; i<maxPosT; i++ ) {
         xyPosT[i]  = new FrameNewCalibTextField();
         cooPosT[i] = new FrameNewCalibTextField();
         panelScroll.add(xyPosT[i]);
         panelScroll.add(cooPosT[i]);
      }
      JScrollPane scroll = new JScrollPane(panelScroll);
      scroll.setPreferredSize(new Dimension(scrollWidth,scrollHeight));
      Aladin.makeAdd(p,scroll,"Center");

      return p;
   }

   // Le panel de la methode WCS
   private JPanel createPanelThree() {
      JPanel p = new JPanel();
      p.setLayout( new BorderLayout(0,0) );

      JPanel panelScroll = new JPanel();
      panelScroll.setLayout(new BorderLayout(0,0) );

      wcsT = new JTextArea(50,70);
      wcsT.setFont( Aladin.COURIER );
      Aladin.makeAdd(panelScroll,wcsT,"Center");

      JScrollPane scroll = new JScrollPane(panelScroll);
      scroll.setPreferredSize(new Dimension(150,200));
      Aladin.makeAdd(p,scroll,"Center");

      return p;
   }

  /** Construction du panel des boutons de validation
   * @return Le panel contenant les boutons Apply/Close
   */
   protected JPanel valid() {
      JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
      p.setFont( Aladin.LBOLD );
      p.setBackground(Aladin.BLUE);

      JButton b;
      Insets m = new Insets(2,2,2,2);
      p.add( modifyButton=b=new JButton(oldp==null?SUBMIT:MODIFY) );
      b.setFont(b.getFont().deriveFont(Font.BOLD));
      b.addActionListener(this);
      b.setMargin(m);
      b.setOpaque(false);
      p.add( undoButton=b=new JButton(UNDO) );
      b.addActionListener(this);
      b.setMargin(m);
      b.setOpaque(false);
      p.add( redoButton=b=new JButton(REDO) );
      b.addActionListener(this);
      b.setMargin(m);
      b.setOpaque(false);
      p.add( b=new JButton(HELP));
      b.addActionListener(this);
      b.setMargin(m);
      b.setOpaque(false);
      p.add( resetButton=b=new JButton(RESET));
      b.addActionListener(this);
      b.setMargin(m);
      b.setOpaque(false);
      p.add( b=new JButton(CLEAR));
      b.addActionListener(this);
      b.setMargin(m);
      b.setOpaque(false);
      p.add( b=new JButton(CANCEL));
      b.addActionListener(this);
      b.setMargin(m);
      b.setOpaque(false);
      return p;
   }

   /** Retourne la méthode de recalibration courante */
   protected int getModeCalib() { return modeCalib; }
   
   private long oplanHashCode=-1;

   // Action associee au menu de changement de methode
   private void changeMethod() {
      int mode = panelMethod.getSelectedIndex();
      modeCalib= mode;

      if( modeCalib==QUADRUPLET ) {
         xyPosT[0].requestFocus();
         setFocusPos(xyPosT[0]);
         if( !plan.hasNoReduction() && plan.hashCode()!=oplanHashCode ) {
            if( Aladin.confirmation(this,a.chaine.getString("NCALIBAGAIN")+"\n=> "+plan.label) ) {
               oplanHashCode=plan.hashCode();
            }
         }
      }

      resumeFlagPlanRecalibrating();
   }

   /** Pour pouvoir ou non déplacer le plan catalogue en cours de recalibration */
   private void resumeFlagPlanRecalibrating() {
      return;
//      plan.recalibrating = modeCalib==SIMPLE && plan.type==Plan.CATALOG;
   }

  /** reset de la frame */
   protected void reset() {
      setData();
      setFocusPos(xyPosT[0]);
    }

  /** Positionnement des TextField xyPosT[] et cooPosT[] en fonction d'une
   * projection de type Quadruplet
   * @param p la projection
   */
   private void setCoo(Projection p) {
      String xy,co;

//      double hauteur = getHauteur();
      for( int i=0; i<maxPosT; i++ ) {
         if( p==null || p.coo==null || i>=p.coo.length ) xy=co="";
         else {
            xy=p.coo[i].x+" "+p.coo[i].y; /* (hauteur-p.coo[i].y); */
            co=p.coo[i].getSexa();
         }
         xyPosT[i].setText(xy);
         cooPosT[i].setText(co);
      }
   }

  /** Generation d'un tableau de Coord[] en fonction des champs TextField
   * de la liste des quadruplets (x,y,alpha,delta)
   */
   private Coord[] getCoo(/*int imageHeight*/) throws Exception {
      int i=0,n;
      Component comp=xyPosT[0];		// permet de reperer le champ en cas d'erreur
      Vector v = new Vector();
      Coord coo[] = null;

      try {

         // Analyse des champs
         for( i=0; i<maxPosT; i++ ) {
            String xy = xyPosT[i].getText().trim();
            String co = cooPosT[i].getText().trim();
            if( xy.length()==0 || co.length()==0 ) continue;
            comp=cooPosT[i];
            Coord c = new Coord(co);
            comp=xyPosT[i];
            StringTokenizer st = new StringTokenizer(xy);
            c.x = Double.valueOf(st.nextToken()).doubleValue();
            c.y = Double.valueOf(st.nextToken()).doubleValue();
//            if( plan.isImage() ) c.y = imageHeight-c.y;
            v.addElement(c);
         }

         // Recopie dans le tableau Coord[]
         n=v.size();
         Enumeration e = v.elements();
         coo = new Coord[n];
         for( i=0; i<n; i++ ) coo[i]=(Coord)e.nextElement();

      } catch( Exception e ) {
         comp.requestFocus();
         setFocusPos(comp);
e.printStackTrace();
         throw new Exception("Error on field "+i);
      }
      return coo;
   }

   // Retourne la hauteur du plan sous-jacent dans le cas où il n'y
   // aurait pas encore de projection
   private int getHauteur() {
      if( plan.isImage() ) return ((PlanImage)plan).height;

      double m[] = new double[4];
      ((PlanCatalog)plan).getXYRange(m);
      return (int)Math.round(m[3]-m[2]);
   }


  /** Creation de la nouvelle calibration ou modification de la précédente */
   private void submit(boolean flagModif) {
      Projection p=null;
      String label   = labelT.getText();
      String error=null;
      double r1=0.,rm1=0.;
      
      // Le frame associée à la projection est remis à ICRS par défaut.
      // (==> Cas du changement de référentiel a posteriori pour les Allsky)
      if( flagModif && oldp!=null ) oldp.frame=Localisation.ICRS;
      
      try {

         // Methode par parametres
         if( modeCalib==SIMPLE ) {
            error="coordinate";
            Astrocoo c = new Astrocoo( Astroframe.create("ICRS"),cooT.getText());
//            Astrocoo c = new Astrocoo(new ICRS(),cooT.getText());
            double raj=c.getLon();
            double dej=c.getLat();
            StringTokenizer st = new StringTokenizer(xyT.getText());
            error="XY position";
            double cx  = Double.valueOf(st.nextToken()).doubleValue();
            double cy  = Double.valueOf(st.nextToken()).doubleValue();
            error="Pixel size";
            updaterm();
            
            // Petite mise au point pas très propre dans le cas d'image rectangulaire
            r1=r;
            rm1=rm;
            if( plan.isImage() ) {
               r1 = ((PlanImage)plan).naxis2;
              rm1 = rm* r1/r;
            }

            error="rotation";
            double rot = Double.valueOf( rotT.getText()).doubleValue();
            int type   = Projection.getProjType( (String)projChoice.getSelectedItem() );
            
//            // On remet le centre dans le système d'arrivée
//            int syst = Localisation.getFrameComboValue( (String)projChoice.getSelectedItem() );
//            Coord center = Localisation.frameToFrame(new Coord(raj,dej), Localisation.ICRS, syst);
//            raj=center.al; dej=center.del;
//            Aladin.trace(4,"FreamNewCalib.submit() syst="+syst+" center="+center);
            
            // Indice du système d'arrivée (à la BOF)
            int system = Localisation.getFrameComboBisValue( (String)frameChoice.getSelectedItem() );
            boolean sym  = trueSym.isSelected();
            error=null;

            if( flagModif && oldp!=null ) {
               p=oldp;
               p.modify(label,Projection.SIMPLE,raj,dej,rm,rm1,cx,cy,r,r1,rot,sym,type,system);
            } else {
               p = new Projection(label,Projection.SIMPLE,raj,dej,rm,rm1,cx,cy,r,r1,rot,sym,type,system,plan);
            }

         // Methode par quadruplets
         } else if( modeCalib==QUADRUPLET ) {
            flagModif=false;    // TOUJOURS CAR SINON CA NE MARCHE PAS BIEN => PF JAN 2023
            Coord coo[] = getCoo(/*getHauteur()*/);
            if( coo==null ) return;
            if( flagModif ) p=oldp;
            else {
               if( plan.projd!=null ) p = plan.projd.copy();
               else {
                  p=new Projection(label,Projection.SIMPLE,
                        coo[0].al,coo[0].del,cx,cy,coo[0].x,coo[0].y,cx*2,cy*2,0,false,Calib.TAN,system,plan);
               }
            }
            p.modify(label,coo);

         // Methode par edition du WCS
         } else if( modeCalib==WCS ) {
            String s = wcsT.getText();
            HeaderFits headerFits = new HeaderFits(s);
            Calib c = new Calib(headerFits);
            if( flagModif ) p=oldp;
            else p = new Projection(label,Projection.WCS,c.alphai,c.deltai,/*raj,dej,*/rm,rm1,cx,cy,r,r1,rot,sym,Calib.TAN,system,plan);
            p.modify(label,c);
         }

         // Recalibration si plan XY
         if( plan.hasXYorig ) plan.pcat.setCoord(p);

         // Modification des projections
         if( !flagModif ) plan.setNewProjD(p);
         else plan.projd=p;

         // Repositionnement du centre du plan
         plan.co=new Coord(raj,dej);

         Properties.majProp();
         plan.setHasSpecificCalib();

         a.view.newView(1);
         a.calque.repaintAll();

         // Passage en mode Modify
         oldp=p;
         modifyButton.setEnabled(false);
         resetButton.setEnabled(false);
         setData();

         a.log("Recalibration","["+plan.getLogInfo()+"]");
//         methodChoice.setEnabled(false);

      } catch( Exception e ) {
e.printStackTrace();
         if( error==null ) error="Calibration error: \n"+e;
         else error="Calibration error\non \""+error+"\" field";
         Aladin.error(this,error,1);
      }
      
//      if( a.calque.getIndex(drawPlan)==-1) {
//         drawPlan =a.calque.newPlanTool("Calibration");
//      }
//      a.command.execScript("rm "+drawPlan.label,false,false);
      a.view.repaintAll();

   }

   /** Appelé lorsque le JTabbedPane est modifié */
   public void stateChanged(ChangeEvent e) {
      if( !(e.getSource() instanceof JTabbedPane) ) return;
      changeMethod();
      if( undoButton!=null ) undoButton.setEnabled( undoable() );
      if( redoButton!=null ) redoButton.setEnabled( redoable() );
   }

   // Gestion des evenement
   public void actionPerformed(ActionEvent evt) {

      if( panelMethod==null ) return;

      Object s = evt.getSource();
      if( s instanceof JButton ) {
         String what = ((JButton)s).getActionCommand();

              if( CANCEL.equals(what) ) hide();
         else if( UNDO.equals(what) )   try { undo(); } catch( Exception e ) { e.printStackTrace(); }
         else if( REDO.equals(what) )   try { redo(); } catch( Exception e ) { e.printStackTrace(); }
         else if( HELP.equals(what) )   Aladin.info(this,HELPSTRING);
         else if( CLEAR.equals(what) )   clear();
         else if( RESET.equals(what) ) {
            reset();
            if( modifyButton.getText().equals(MODIFY) ) modifyButton.setEnabled(false);
         }
         else if( MODIFY.equals(what) ) {
            setUndo(plan.projd);
            submit(true);
         }
         else if( SUBMIT.equals(what) ) {
            Projection p = plan.projd;
            submit(false);
            if( p!=null ) { setUndo(p.copy()); undoButton.setEnabled(true); }
            modifyButton.setText(MODIFY);
         }
      }

      undoButton.setEnabled( undoable() );
      redoButton.setEnabled( redoable() );
   }

   // Retourne l'indice des tableaux xyPosT[] et cooPosT[] pour un
   // widget donne, -1 si non trouve
   private int getIndexPos(Component c) {
      for( int i=0; i<maxPosT; i++ ) {
         if( c==xyPosT[i] || c==cooPosT[i] ) return i;
      }
      return -1;
   }

   // Positionne le Focus sur le Textfield xyPosT[] ou cooPosT[]
   // correspondant au widget passe en parametre.
   // Mise a jour des variables de classes focusTextField et flagXY
   private void setFocusPos(Component c) {
      String s;
      int i=getIndexPos(c);
      if( focusTextField!=null ) focusTextField.setBackground(Color.white);
      if( i==-1 ) { focusTextField=null; return; }
      flagXY=(c==xyPosT[i]);
      focusTextField=(JTextField)c;
      focusTextField.setBackground(Color.yellow);
      if( flagXY ) s = xyPosT[i].getText().trim();
      else s = cooPosT[i].getText().trim();
      setRepere(s,flagXY);
      c.requestFocus();

  }

   /** Déplacement du repère en fonction de la valeur XY ou RADEC
    * donnée en s */
   private void setRepere(String s,boolean flagXY) {
      if( s.length()==0 ) return;
      Coord coo;

      // En XY
      if( flagXY ) {
         try {
            StringTokenizer st = new StringTokenizer(s);
            coo = new Coord();
            coo.x = Double.valueOf(st.nextToken()).doubleValue()-0.5;
            coo.y = Double.valueOf(st.nextToken()).doubleValue()-0.5;
            coo.y=getHauteur()-coo.y;

            plan.projd.getCoord(coo);
            if( Double.isNaN(coo.al) ) return;
          } catch( Exception e) { return; };

      // En RADEC
      } else {
         try { coo = new Coord(s); } catch( Exception e) { return; };
      }

      a.view.setRepere(coo);

   }

   // Fait avancer le focus sur le prochain widget pour la methode
   // QUADRUPLET
   private void changeFocus() {
      int i=getIndexPos(focusTextField);
      if( i==-1 ) { focusTextField=null; return; }
      focusTextField.setBackground(Color.white);
      if( focusTextField==xyPosT[i] ) { focusTextField=cooPosT[i]; flagXY=false; }
      else {
         i++;
         if( i==maxPosT ) focusTextField=null;
         else {
            focusTextField=xyPosT[i];
            flagXY=true;
         }
      }
      if( focusTextField!=null ) {
         focusTextField.setBackground(Color.yellow);
         focusTextField.requestFocus();
      }
   }


  public static  int W=20;
//	 private static int [] source= new int[W*W] ;
//  private static int testi =0 ;   // Commenté par PF 111/4/05, inutile, sans doute du vieux débogage, à demander à François B

  /** Utilisé pour la correction a appliquee a la recherche d'une etoile
    * @author François Bonnarel
	*/

  protected static void test(int i, int j, int src, double pix[], int source[], double sigma, double fond) {
      int ii, jj;
      int sidetest;

//      testi++;
      //						System.out.println("testi "+testi);

      for( sidetest = 1; sidetest <= 4; sidetest++ ) {
         switch( sidetest ) {
         case 1: ii = i - 1; jj = j; break;
         case 2: ii = i + 1; jj = j; break;
         case 3: ii = i; jj = j - 1; break;
         case 4: ii = i; jj = j + 1; break;
         default: ii = i; jj = j;
         }
         if( (ii >= 0) && (ii < W) && (jj >= 0) && (jj < W)
               && (source[jj * W + ii] == 0) ) {
            if( pix[jj * W + ii] - fond > 3 * sigma ) {
               source[jj * W + ii] = src;
               //                             System.out.println("ii "+ii+"jj "+jj+"src "+src);
               test(ii, jj, src, pix, source, sigma, fond);
            }

         }
      }
//      testi--;
      //						System.out.println("testi "+testi);
      return;
   }

  /** Determine une correction a appliquee a la recherche d'une etoile
   * @author François Bonnarel
   * @param pix le carre des pixels entourant le point clique
   * @return la correction en x et y (positive ou negative) a appliquee
   *         a l'objet clique
   */
   protected static PointD[] bestPoint(double pix[], Plan lePlan) throws Exception {
      int W = (int)Math.sqrt(pix.length);   // Ajout Pierre Mars 2006
	  double x=0,y=0;
	  double alsig1 ;
	  double alsig=0 ;
	  double max=0;
	  double fond1 = 0 ;
	  double fond = 0, w = 0, nfond;
	  double  sigma1 = 0 ;
	  double sigma = 0. ;
	  int i,j;
	  double a,b,oldfond,oldsigma,bb ;
	  int [] source= new int[W*W] ;
//				 System.out.println("W="+W);

	  for( i=0; i<W; i++ ) {
		 for( j=0; j<W; j++ ) {
			if( max<pix[i*W+j] ) {
			   max=pix[i*W+j] ;
			   x=j; y=i;
//				 System.out.println("i="+i+" j="+j+" =>"+max);
			}
		 }
	  }
//		System.out.println("x="+x+" y="+y+" =>"+max);
	  for( i=0; i<W; i++ ) {
		 fond1 = 0 ;
		 for( j=0; j<W; j++ ) {
			   fond1 += pix[i*W+j] ;
		  }
		  fond += fond1/ W ;
	  }
	  fond /= W ;
	  oldfond = fond ;
//		 System.out.println("fond: "+fond);
	  for( i=0; i<W; i++ ) {
		 sigma1 = 0 ;
		 for( j=0; j<W; j++ ) {
					 sigma1 += (pix[i*W+j]-fond) * (pix[i*W+j]-fond) ;
		  }
		  sigma += sigma1/ W ;
	  }
	  sigma /= W ;
	  sigma = Math.sqrt(sigma) ;
	  oldsigma = sigma ;
//		System.out.println("sigma: "+sigma);
/*
	  for( i=0; i<W; i++ ) {
		 for( j=0; j<W; j++ ) {
			if( pix[i*W+j] -fond > 3*sigma ) {
//			   System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
				y += (pix[i*W+j] -fond)*i ;
				x += (pix[i*W+j] -fond)*j ;
				w += pix[i*W+j] -fond ;
			}
		 }

	  x /= w ;
	  y /= w ;
//		System.out.println("x="+x+" y="+y+"w =>"+w) ;
*/
	  nfond = 0 ;
	  fond = 0 ;
	  for( i=0; i<W; i++ ) {
		 fond1 = 0 ;
		 for( j=0; j<W; j++ ) {
			if( pix[i*W+j] -oldfond < 4*oldsigma ) {
//				System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
			   fond1 += pix[i*W+j] ;
			   nfond++ ;
			}
		 }
		  fond += fond1/ W ;
	  }
//		System.out.println("fond: "+fond+"nfond "+nfond+" "+(nfond/W));
	  fond /= (nfond/W);
//		System.out.println("fond: "+fond+"nfond "+nfond+" "+(nfond/W));
	  sigma = 0;
	  for( i=0; i<W; i++ ) {
		 sigma1 = 0 ;
		 for( j=0; j<W; j++ ) {
			if( pix[i*W+j] -oldfond < 4*oldsigma ) {
					 sigma1 += (pix[i*W+j]-fond) * (pix[i*W+j]-fond) ;
					}
		  }
		  sigma += sigma1/ nfond ;
	  }
//		sigma /= W ;
	  sigma = Math.sqrt(sigma) ;
//		System.out.println("sigma: "+sigma);
	  int flag = 0 ;
	for( i=0; i<W; i++ ) {
		 for( j=0; j<W; j++ ) {
				  source[ i*W+j] = 0 ;
			   }
			}
	  for( i=0; i<W; i++ ) {
		 for( j=0; j<W; j++ ) {
				 if(source[i*W+j] == 0) {
				   if( pix[i*W+j] -fond > 3*sigma ) {
//			 System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
			  source[ i*W+j] = ++flag ;
			  test(j,i,flag,pix,source,sigma,fond);
//				System.out.println("avant i "+i+"j "+j+"flag "+flag);
//				System.out.println("apres i "+i+"j "+j+"flag "+flag);
			   }
			  }
			}
		 }
	double mindist= W*W/2.;
	int selsource = 1;
	for( i=0; i<W; i++ ) {
		 for( j=0; j<W; j++ ) {
		  if ((source[ i*W+j] !=0)&&(mindist>(W/2.-i)*(W/2.-i)+(W/2.-j)*(W/2.-j)))
				 {
				  mindist = (W/2.-i)*(W/2.-i)+(W/2.-j)*(W/2.-j) ;
				  selsource = source[ i*W+j] ;
//				System.out.println("selsource "+selsource+"mindist "+mindist);
				 }
			   }
			}
	  x = 0 ;
	  y = 0 ;
	  w = 0 ;
	  for( i=0; i<W; i++ ) {
		 for( j=0; j<W; j++ ) {
				 if(source[ i*W+j] == selsource) {
//			if( pix[i*W+j] -fond > 3*sigma ) {
//			   System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
				y += (pix[i*W+j] -fond)*i ;
				x += (pix[i*W+j] -fond)*j ;
				w += pix[i*W+j] -fond ;
			}
		 }
	  }
	  if (w!=0)
	  {
	  x /= w ;
	  y /= w ;
	  }
	  else
	  {
	   x = W/2.;
	   y = W/2.;
//	   PointD[] p = {new PointD(x-W/2+0.5,y-W/2+0.5)} ;
	   throw new Exception("Too large source for the centering algorithm");
	  }
 //     System.out.println("x="+x+" y="+y+"w =>"+w) ;
//		System.out.println("x="+(x-W/2)+" y="+(y-W/2)+"w =>"+w) ;
		i = (int)y ;
		j = (int)x ;
		a = pix[i*W+j] -fond ;
		b = 256 ;
		alsig1 = 0 ;
		 for( j=0; j<(int)x+1; j++ ) {
			if( pix[i*W+j] -fond > 3*sigma ) {
//			   System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
			 bb = pix[i*W+j] -fond -a/2 ;
			 if(bb < 0 )bb = -bb ;
			 if(bb < b){b  = bb ;  alsig1 = x -j;}
//		System.out.println("a="+a+" b="+b+"jj "+jj) ;
			}
		 }
		alsig += alsig1 ;
		b = 256 ;
		 for( j=(int)x+1; j<W; j++ ) {
			if( pix[i*W+j] -fond > 3*sigma ) {
//			   System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
			 bb = pix[i*W+j] -fond -a/2 ;
			 if(bb < 0 )bb = -bb ;
			 if(bb < b){b  = bb ; alsig1 = j-x;}
//		System.out.println("a="+a+" b="+b+"jj "+jj) ;
			}
		 }
		alsig += alsig1 ;
		i = (int)y ;
		j = (int)x ;
		a = (((int)pix[i*W+j])&0xFF) -fond ;
		b = 256 ;
		 for( i=0; i<(int)y+1; i++ ) {
			if( pix[i*W+j] -fond > 3*sigma ) {
//			   System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
			 bb = pix[i*W+j] -fond -a/2 ;
			 if(bb < 0 )bb = -bb ;
			 if(bb < b){b  = bb ;  alsig1 = y -i;}
//		System.out.println("a="+a+" b="+b+"jj "+jj) ;
			}
		 }
		alsig += alsig1 ;
		b = 256 ;
		 for( i=(int)y+1; i<W; i++ ) {
			if( pix[i*W+j] -fond > 3*sigma ) {
//			   System.out.println("i="+i+" j="+j+" =>"+pix[i*W+j]);
			 bb = pix[i*W+j] -fond -a/2 ;
			 if(bb < 0 )bb = -bb ;
			 if(bb < b){b  = bb ; alsig1 = i-y;}
 //     System.out.println("a="+a+" b="+b+"jj "+jj) ;
			}
		 }
		alsig += alsig1 ;
		alsig /= 4 ;
		double fwhm = 2*alsig ;

		 double Min = ((PlanImage)lePlan).pixelMin ;
		 double Max = ((PlanImage)lePlan).pixelMax ;
	  //System.out.println("Min "+Min+"Max "+Max) ;
			 fond *= (Max-Min)/256 ;
			 a *= (Max-Min)/256 ;
			 sigma *= (Max-Min)/256;
			 fond += Min ;
//		System.out.println("Min "+Min+"Max "+Max+"fac "+(Max-Min)/256) ;
//		System.out.println("fond "+fond+"sigma "+sigma+"a "+a+"fwhm "+fwhm);
	  PointD[] p = {new PointD(x-W/2.+0.5,y-W/2.+0.5), new PointD(fond,sigma),new PointD(a,fwhm)};
//		 System.out.println("Corrections: "+p[0]);
//		 System.out.println("Corrections: "+p[1]);
	  return p;
   }

   /** Retourne true si on est entrain de récupérer les XY originaux d'une source
    * lors de la recalibration d'un catalogue */
   protected boolean isGettingOriginalXY() { return plan.hasXYorig && flagXY; }


   private Obj prevObj=null;
   Plan drawPlan = null;

  /** Fonction appelee lors d'un clic souris dans l'image ou dans la fenetre
   * des mesures pour mettre a jour les TextFields dans la methode QUADRUPLET
   * @param x,y la position clique en coordonnees de l'image
   * @param pi. Le plan d'origine qui permettront d'avoir des pixels pour le centroid ou null sinon
   * @param o L'objet catalogue/repere ou null
   */
   protected PointD mouse(double x,double y,PlanImage pi,Position o) {
      PointD rep=null;
      Point pp=null;
      if( modeCalib!=QUADRUPLET ) return rep;

      Aladin.trace(4,"FrameNewCalib.mouse() x="+x+" y="+y+
                   (o!=null?" source="+o.id:"")/* +(pix!=null?" avec pix[]":"") */);
      if( focusTextField==null ) return rep;

      if( plan.isCatalog()
             && !plan.hasXYorig
             && !Projection.isOk(plan.projd) ) return rep;

      String s;

      // Position (X,Y) simplement
      if( flagXY ) {

         // Mode catalogue
         if( plan.hasXYorig ) {
            if( o==null ) {
               a.error(ERRNOXY);
               return null;
            }
            x=o.x;
            y=o.y;
            prevObj=o;

         // mode image
         } else {
//            x+=0.5; y-=0.5;         // Demi pixel
            //            if( pix!=null ) {
            if( pi!=plan ) {
               a.error(ERRIMG);
               return null;
            }
            prevObj=null;
            try {
               pp = new Point((int)x,(int)y);
               double [] iqe = pi.getPixelStats(pp);
               if( iqe==null ) throw new Exception("IQE autocenter failed => use XY mouse position");
//               if( iqe==null ) {
//                  Aladin.trace(4,"FrameNewCalib.mouse(): autocenter by barycenter instead of iqe !");
//                  // IL FAUDRAIT QUE JE PUISSE EGALEMENT RECUPERER LES PIXELS COULEURS
//                  double pix[]=null;
//                  if( pi!=null /* && pi.hasAvailablePixels()*/ ) {
//                     int w = FrameNewCalib.W;
//                     pp = new Point((int)x,(int)y);
//                     pp.y = pi.height - pp.y;
//                     pix = new double[w*w];
//                     try {
//                        pi.getPixelsCentroid(pix,pp,w);
//                     } catch( Exception e ) { }
//                     pp.y = pi.height - pp.y;
//                  }
//                  PointD pCorr[] = bestPoint(pix,plan);
//                  x=pp.x+pCorr[0].x;
//                  y=pp.y-pCorr[0].y;
//               } else {
                  x = iqe[0];
                  y = iqe[2];
//               }
            } catch( Exception e ) {
//               if( Aladin.levelTrace>=3 ) e.printStackTrace();
               a.status.setText(e.getMessage()+"\n "+ERRNOBEST);
            }
            //            }
         }

         rep=new PointD(x,y);

         // Complément sur l'ordonnée pour compter du bas
          if( plan.isImage() ) y = ((PlanImage)plan).naxis2-y;

          s=Util.myRound(""+x,4)+"  "+Util.myRound(""+y,4);
      }

      // Position J2000
      else {
         if( Projection.isOk(a.calque.getPlanRef().projd) || plan.hasXYorig ) {
            String s1=null;

            // Recherche du RA/DEC de l'objet le plus proche qui ne doit
            // pas appartenir au plan XY bien sur.
            if( o!=null && o!=prevObj ) {
               s=Coord.getSexa(o.raj,o.dej);
               s1=Coord.getSexa(o.raj,o.dej,":");

            // Recherche du RA/DEC en fonction de l'astrometrie du
            // plan de reference
            } else {
              Plan pl = a.calque.getPlanRef();
              Projection p = pl==null || pl.projd==null?null:pl.projd;
              if( p==null ) {
                 a.error("No RA/DEC reference plane !");
                 return null;
              }
              try {
                 pp = new Point((int)x,(int)y);
                 double [] iqe = ((PlanImage)pl).getPixelStats(pp);
                 if( iqe==null ) throw new Exception("IQE autocenter failed => assume XY mouse");
                 x = iqe[0];
                 y = iqe[2];
//                 PointD pCorr[] = bestPoint(pix,pl);
//                 x=pp.x+pCorr[0].x;
//                 y=pp.y-pCorr[0].y;
              } catch( Exception e ) {
//                 if( Aladin.levelTrace>=3 ) e.printStackTrace();
//                 a.warning(e.getMessage()+"\n "+ERRNOBEST);
              }
              Coord  c = new Coord();
              c.x=x; c.y=y;
              p.getCoord(c);
              if( Double.isNaN(c.al) ) s="";
              else {
                 s=Coord.getSexa(c.al,c.del);
                 s1=Coord.getSexa(c.al,c.del,":");
}
            }
//            if( s1!=null ) {
//               if( a.calque.getIndex(drawPlan)==-1) {
//                  drawPlan = a.calque.newPlanTool("Calibration");
//               }
//               a.command.execScript("select \""+drawPlan.label+"\";draw tag("+s1+")",false,false);
//               a.view.repaintAll();
//            }
         } else {
            if( o==null || o==prevObj ) {
               a.error("No object near the pointer, try again !!");
               return null;
            }
            s=Coord.getSexa(o.raj,o.dej);
         }
      }



      focusTextField.setText(s);
// CA MERDOUILLE SI ON LAISSE CA
//      if( !flagXY || plan.isImage() ) setRepere(s,flagXY);
      changeFocus();

      return rep;
   }

   public void hide() {
      if( plan!=null ) {
//         plan.recalibrating=false;
         plan=null;
      }
      super.hide();
   }

   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) hide();
      super.processWindowEvent(e);
   }

   // Gestion des evenement
//   public boolean handleEvent(Event e) {
//      if( e.id==Event.WINDOW_DESTROY )hide();
//      return super.handleEvent(e);
//   }

   class FrameNewCalibTextField extends JTextField implements MouseListener {
      FrameNewCalibTextField() { super(15); addMouseListener(this); }
      public void mouseClicked(MouseEvent e) { }
      public void mouseEntered(MouseEvent e) { }
      public void mouseExited(MouseEvent e) { }
      public void mousePressed(MouseEvent e) { setFocusPos(this); }
      public void mouseReleased(MouseEvent e) { }
      public void setText(String s) {
         super.setText(s);
         modifyButton.setEnabled(true);
         resetButton.setEnabled(true);
      }
   }
}
