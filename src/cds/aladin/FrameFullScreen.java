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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.Timer;

import cds.tools.Util;

/**
 * Gestion d'un Frame pour l'affichage d'Aladin en mode plein écran
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : juin 08 preview + améliorations diverses
 * @version 1.0 : mars 08 création
 */
public final class FrameFullScreen extends JFrame implements ActionListener {
   
   static final int WINDOW = 0;             // en mode fenêtré - 1 unique panel
   static final int WINDOW_HIDDEN = 1;      // idem mais iconifié au démarrage
   static final int FULL = 2;               // En mode plein écran, mais avec gestion des fenêtres par l'OS
   static final int CINEMA = 3;             // idem mais en mode exclusif (pas d'overlay de fenêtre/menu possible)
   
   static private String [] MODE = { "Window","Window-hidden","Full","Cinema" };
   
   static GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
   static GraphicsDevice device = env.getDefaultScreenDevice();
   static DisplayMode display = device.getDisplayMode();

   private Aladin aladin=null;
   protected ViewSimple viewSimple; // La vue affichée en plein écran
   private Rectangle bounds;    // La taille précédédente de la vue (pour pouvoir la redimensionner)
   private Vector ligne=null;   // La ligne de mesure courante (null si aucune)
   private int ocursor=-1;      // Curseur courant (ex: Aladin.HAND)
//   private boolean full;        // true si plein écran, sinon une fenêtre simple
   private int mode;            // mode de fonctionnement (WINDOW, WINDOW_HIDDEN, FULL, CINEMA, ...)
   private Timer timer=null;    // Timer pour les choses qui clignotent
   private JPopupMenu popMenu;
   private JMenuItem menuDel,menuProp;
   private StringBuffer cmd = new StringBuffer();   // Commande en cours de saisie
   private boolean blinkState=true; // Etat du clignotement
   private Plan currentPlan=null;  // Plan de la checkbox sous la souris, null sinon
   private int XBLINK=-1,YBLINK;  //Position du voyant blink (-1 si aucun)
   private int XGRID=-1,YGRID;    // Position du logo de la grille (-1 si aucun)
   private int XARROW=-1,YARROW;  // Position du logo de l'image suivante (-1 si aucun)
   private int XSAVE=-1,YSAVE;  // Position du logo d'enregistrement (-1 si aucun)

   static final int YMARGE = 175; // Marge en ord. depuis le bas jusqu'au premier logo
   static final int XMARGE = 40;  // Marge en abs. depuis la droite jusqu'au premier logo
   static final int YGAP = 18;    // Distance entre les logos (checkboxes)
   static final int MAXCHECK=20;
   private int nCheck=0;        // Nombre de checkbox utilisés
   private Check [] memoCheck = null;  // Permet la mémorisation de la position d'une checkbox associée à un plan

   /** Object pour mémoriser le plan et la position d'une checkbox */
   class Check {
      int x,y;
      Plan p;
      boolean in(int a,int b) { return a>=x && a<=x+10 && b>=y && b<=y+10; }
   }

   /** Création d'une fenêtre plein écran contenant une unique vue */
   protected FrameFullScreen(Aladin aladin,ViewSimple v,/* boolean full,boolean startHidden,*/int mode) {
      super(env.getDefaultScreenDevice().getDefaultConfiguration());
      this.aladin=aladin;
      setTitle(Aladin.TITRE+" "+aladin.getReleaseNumber());
      Aladin.setIcon(this);
      viewSimple=v;
//      this.full=full;
      this.mode = mode;
      Aladin.trace(4,"FrameFullScreen(mode="+MODE[mode]+")");
      createPopupMenu();
      
      insertMenu();
     
      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { end(); /*full();*/ }
      },
      
      
      KeyStroke.getKeyStroke(KeyEvent.VK_F3,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { zoom(1); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_F2,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { zoom(-1); }
      },

      
      KeyStroke.getKeyStroke(KeyEvent.VK_F11,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { end(); /*window();*/ }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_F12,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { end1(); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );

      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { delete(); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );

      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { grid(); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_G,InputEvent.ALT_MASK),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );

      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { hpxGrid(); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_W,InputEvent.ALT_MASK),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );

      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { constellation(); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_C,InputEvent.ALT_MASK),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );


      if( mode!=CINEMA ) {
         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { dist(); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_D,InputEvent.ALT_MASK),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );

         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { prop(); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.ALT_MASK),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );

         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { pixel(); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_M,InputEvent.CTRL_MASK),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );

         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { open(); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_O,InputEvent.CTRL_MASK),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );

         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { openDialog(); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_L,InputEvent.CTRL_MASK),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );

         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { zoom(-1); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_F2,0),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );

         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { zoom(1); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_F3,0),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );

         v.setFocusTraversalKeysEnabled(false);
         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { next(1); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_TAB,InputEvent.SHIFT_MASK),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );
         getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) { next(-1); }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_TAB,0),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );
      }

      getContentPane().setBackground(Color.white);
      getContentPane().add(v,BorderLayout.CENTER);
      bounds = v.getBounds();
      v.setBounds(getBounds());
//      if( full ) full();
      if( mode==FULL || mode==CINEMA ) full();
      else window();
//      if( !startHidden ) setVisible(true);
      if( mode!=WINDOW_HIDDEN ) setVisible(true);
      if( !aladin.isApplet() || aladin.flagDetach ) aladin.f.setVisible(false);
   }
   
   
   private Component box1=javax.swing.Box.createGlue();
   private Component box2=javax.swing.Box.createGlue();
   private Component box3=javax.swing.Box.createGlue();
   
   private void insertMenu() {
      if( mode==CINEMA ) return;
      setJMenuBar(aladin.jBar);
      aladin.jBar.setVisible(false);
      
      // On enlève l'icone FullScreen
      aladin.jBar.remove( aladin.jBar.getMenuCount()-1 );  
      
      // On insère le menu des Frames
      aladin.jBar.add( box1,aladin.jbarLastIndex );
      aladin.jBar.add( box1,aladin.jbarLastIndex );
      aladin.jBar.add( getFrameMenu(),aladin.jbarLastIndex );
      aladin.jBar.add( getBookmarkMenu(),aladin.jbarLastIndex );
      aladin.jBar.add( box3,aladin.jbarLastIndex );
   }
   
   private void removeMenu() {
      if( mode==CINEMA ) return;
      // On enlève le menu Frame
      aladin.jBar.remove(menuBookmark);
      aladin.jBar.remove(menuFrame);
      aladin.jBar.remove(box1);
      aladin.jBar.remove(box2);
      aladin.jBar.remove(box3);
      
      // On remet l'icone FullScreen
      aladin.jBar.add( aladin.iconFullScreen );
     
      
      aladin.jBar.setVisible(true);
      aladin.setJMenuBar(aladin.jBar);
      aladin.jBar.setBorderPainted(true);
   }
   
   private JMenu menuFrame=null;
   
   private JMenu getFrameMenu() {
      if( menuFrame!=null ) return menuFrame;
      menuFrame = new JMenu("Frame");
      JRadioButtonMenuItem ji;
      ButtonGroup mg = new ButtonGroup();
      
      JComboBox c = aladin.localisation.getComboBox();
      int n=c.getItemCount();
      Vector<String> list = new Vector<String>(n);
      for( int i=0; i<n; i++ ) list.add( (String)c.getItemAt(i) );
      
      int select = c.getSelectedIndex();
      for( int i=0; i<n ; i++ ) {
         String s = list.get(i);
         ji= new JRadioButtonMenuItem(s);
         ji.setSelected( i==select ); 
         mg.add(ji);
         menuFrame.add(ji);
         ji.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               String s = ((JMenuItem)e.getSource()).getActionCommand();
               aladin.localisation.setPositionMode(s);
               
            }
         });

      }
      return menuFrame;
   }
   
   private JMenu menuBookmark=null;
   
   private JMenu getBookmarkMenu() {
      if( menuBookmark!=null ) return menuBookmark;
      menuBookmark = new JMenu("Bookmark");
      
      JToolBar c = aladin.bookmarks;
      int n=c.getComponentCount();
      JMenuItem ji;
      for( int i=0; i<n; i++ ) {
         Component c1 = c.getComponentAtIndex(i);
         if( !(c1 instanceof JButton) ) continue;
         JButton b = (JButton)c1;
         String label = b.getText();
         ji= new JMenuItem( label );
         ji.addActionListener( b.getActionListeners()[0] );
         menuBookmark.add(ji);
      }
      
      return menuBookmark;
   }
   

   private void prop() { Properties.createProperties(aladin.calque.getPlanBase()); }
   private void pixel() { aladin.pixel(); }
   private void open() {      ((ServerFile)aladin.dialog.localServer).browseFile(); }
   private void openDialog() { aladin.dialog.show(); }
   
   /** Retourne le mode d'affichage courant WINDOW, WINDOW_HIDDEN, FULL ou CINEMA */
   protected int getMode() { return mode; }
   
   private int modeReticle=-1;

   /**  Passage en plein écran */
   private void full() {

      setUndecorated(true);
      if( mode==FULL || (mode==CINEMA && aladin.winPlateform) ) {
         setLocation(0,0);
         setSize(Aladin.SCREENSIZE);
         
      } else {
         
         /* GraphicsDevice [] gds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
         GraphicsDevice gd;
         if( gds.length==2 ) gd = gds[1];
         else  */ 
         GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
         gd.setFullScreenWindow(this);
      }
      
      if( mode==CINEMA ) {
         modeReticle=aladin.calque.reticleMode;
         aladin.calque.setReticle(0);
      }
      
   }
   
   /** Passage en mode preview avec récupération de la position et de la
    * taille adéquate */
   private void window() {
      if( modeReticle!=-1 ) aladin.calque.setReticle(modeReticle);
      Dimension d = aladin.f.getSize();
      if( d.width<10 || d.height<10 ) {
         Rectangle r = aladin.configuration.getWinLocation();
         if( r.width<0 || r.height<0 ) r.width=r.height=600;
         setSize(r.width,r.height);
         setLocation(r.x,r.y);
      } else {
         setSize(d);
         setLocation(aladin.f.getLocation());
      }
   }
   
   public void processWindowEvent(WindowEvent e) {
      if( modeReticle!=-1 ) aladin.calque.setReticle(modeReticle);
      if( e.getID() == WindowEvent.WINDOW_CLOSING ) {
//         if( !full ) {
         if( mode!=FULL && mode!=CINEMA ) {
            if( aladin.isApplet() ) setVisible(false);
            else aladin.quit(0);
         }
         else end();
         return;
      }
      super.processWindowEvent(e);
   }


   /** Le premier ESC nettoie éventuellement la commande en cours */
   private void end1() {
      if( hasCmd() && !aladin.view.isFree() ) Util.resetString(cmd);
      else end();
   }

   /** Fin du mode plein écran => réintégration de la vue dans son container originale */
   protected void end() {
      
      viewSimple.aladin.fullScreen=null;
      viewSimple.setToolTipText(null);
      viewSimple.setBounds(bounds);
      aladin.view.adjustPanel();
      if( mode!=FULL && mode!=CINEMA ) {
         aladin.toolBox.calcConf(500);   // juste pour remettre les choses en place
         aladin.f.setSize(getSize());
      }
      
      removeMenu();

      aladin.f.setVisible(true);
      aladin.calque.repaintAll();
      
      memoCheck=null;
      currentPlan=null;
      dispose();
   }

   private void delete() {
      aladin.calque.Free(viewSimple.pref);
      aladin.view.findBestDefault();
   }

   /** Passage à la prochaine image */
   private void next(int sens) { aladin.view.next(sens); }

   //   /** Précédente position mémorisée */
   //   private void undo() { aladin.view.undo(false); }
   //
   //   /** Prochaine position mémorisée */
   //   private void redo() { aladin.view.redo(false); }

   /** Activation/désactivation de la grille */
   private void grid() {
      aladin.calque.setGrid(!aladin.calque.hasGrid(),true);
   }

   /** Activation/désactivation de la grille Healpix */
   private void hpxGrid() {
      aladin.calque.setOverlayFlag("hpxgrid", !aladin.calque.hasHpxGrid() );
      aladin.view.repaintAll();
   }
   
   // Activation/désactivation des constellations
   private void constellation() {
      boolean flag = !aladin.calque.hasConst();
      aladin.calque.setOverlayFlag("const", flag);
      aladin.console.printCommand("setconf overlay="+(flag?"+":"-")+"const");
      aladin.view.repaintAll();
   }

   /** Activation de l'outil de mesure de distance */
   private void dist() {
      aladin.graphic(ToolBox.DIST);
      aladin.makeCursor(viewSimple, Aladin.CROSSHAIRCURSOR);
   }

   /** Zoom (+1) ou unzoom (-1) */
   private void zoom(int sens) {
      aladin.command.execLater("zoom "+(sens==1?'+':'-'));
   }

   /** Initialisation de la mémorisation des checkboxes en superposition */
   protected void startMemo() {
      if( memoCheck==null ) memoCheck = new Check[MAXCHECK];
      nCheck=0;
   }

   /** retourne le plan correspondant à la checkbox sous la position x,y,
    * null si aucun */
   private Plan getCheckPlan(int x, int y) {
      if( memoCheck==null ) return null;
      try {
         for( int i=nCheck-1; i>=0; i-- ) {
            Check c = memoCheck[i];
            if( c.in(x,y) ) return c.p;
         }
      } catch( Exception e ) {}
      return null;
   }

   /** Mémorisation des checkboxes en superposition */
   protected void setCheck(Plan p) {
      if( nCheck>=MAXCHECK ) return;
      Check c = memoCheck[nCheck];
      if( c==null ) memoCheck[nCheck]=c=new Check();
      c.x=getWidth()-XMARGE;
      c.y=getHeight()-YMARGE - nCheck*YGAP - YGAP;
      c.p=p;
      nCheck++;
   }

   /** Dessin des checkboxes en superposition */
   protected void drawChecks(Graphics g) {
      if( memoCheck==null || aladin.calque.isFree() ) return;
      for(int i=nCheck-1; i>=0; i-- ) {
         Check c = memoCheck[i];
         Util.drawCheckbox(g, c.x, c.y, c.p.c , null, null, c.p.active);
      }
   }

   private Image cross=null,arrow=null,save=null;
   private int XOUT,YOUT;

   protected void drawIcons(Graphics g) {
      if( mode==CINEMA ) return;
      
      int ymarge=YMARGE;

//      // Dessin du logo de sortie du fullscreen
//      if( cross==null ) {
//         cross=aladin.getImagette("Preview.gif");
//         YOUT = 3;
//      }
//      XOUT = viewSimple.getWidth()-18;
//      try { g.drawImage(cross,XOUT,YOUT,viewSimple); }
//      catch( Exception e ) {}

      // Dessin du logo de la grille
      if( viewSimple.pref!=null && Projection.isOk(viewSimple.getProj()) ) {
         XGRID = getWidth()-(XMARGE+2);
         YGRID = getHeight()-ymarge+8;
         aladin.grid.fillBG(g,XGRID,YGRID,Color.white);
         aladin.grid.drawGrid(g,XGRID,YGRID,aladin.calque.hasGrid()?Aladin.COLOR_GREEN:Color.black);
      } else {
         XGRID=-1;
         //        ymarge=100;
      }

      // dessin du logo d'enregistrement
      if( viewSimple.pref!=null ) {
         if( save==null ) save=aladin.getImagette("Export.gif");
         try {
            XSAVE=getWidth()-(XMARGE+1);
            YSAVE=getHeight()-ymarge+YGAP+6;
            g.drawImage(save,XSAVE,YSAVE,viewSimple); }
         catch( Exception e ) { XSAVE=-1; }
      } else XSAVE=-1;

      // dessin du logo de l'image suivante
      if( aladin.calque.getNbPlanImg()>1 ) {
         if( arrow==null ) arrow=aladin.getImagette("Next.gif");
         try {
            XARROW=getWidth()-XMARGE;
            YARROW=getHeight()-ymarge+2*YGAP+6;
            g.drawImage(arrow,XARROW,YARROW,viewSimple); }
         catch( Exception e ) { XARROW=-1; }
      } else XARROW=-1;

   }

   /** Retourne true si le curseur est sur l'icone de sortie */
   private boolean inIconOut(int x,int y) {
      x-=XOUT; y-=YOUT;
      try {
         return x>=0 && x<cross.getWidth(viewSimple)
               && y>=0 && y<cross.getHeight(viewSimple);
      } catch( Exception e ) { }
      return false;
   }

   /** Retourne true si le curseur est sur l'icone de sortie */
   private boolean inIconArrow(int x,int y) {
      if( XARROW<0 ) return false;
      x-=XARROW; y-=YARROW;
      try {
         return x>=0 && x<arrow.getWidth(viewSimple)
               && y>=0 && y<arrow.getHeight(viewSimple);
      } catch( Exception e ) { }
      return false;
   }

   /** Retourne true si le curseur est sur l'icone d'enregistrement */
   private boolean inIconSave(int x,int y) {
      if( XSAVE<0 ) return false;
      x-=XSAVE; y-=YSAVE;
      try {
         return x>=0 && x<save.getWidth(viewSimple)
               && y>=0 && y<save.getHeight(viewSimple);
      } catch( Exception e ) { }
      return false;
   }

   /** retourne true s'il y a une commande en cours de saisie */
   protected boolean hasCmd() { return cmd.length()>0; }

   /** La commande en cours reçoit un nouveau caractère
    * @return true s'il faut regénérer le imgBuf de la vue
    */
   protected boolean sendKey(KeyEvent e) {
      boolean fullRepaint=false;

      int key = e.getKeyCode();
      char k = e.getKeyChar();

      aladin.endMsg();

      if( e.isControlDown() || e.isAltDown() ) return fullRepaint;
      blinkState=true;

      if( key==KeyEvent.VK_ENTER ) {
         aladin.execAsyncCommand(cmd.toString());
         cmd.delete(0, cmd.length());
         fullRepaint=true;
      }
      else if( key==KeyEvent.VK_BACK_SPACE || key==KeyEvent.VK_DELETE ) {
         if( cmd.length()>0 ) cmd.deleteCharAt(cmd.length()-1);
         if( cmd.length()==0 ) fullRepaint=true;

         // On insere un nouveau caractere
      } else if( k>=31 && k<=255 ) cmd.append(k);

      return fullRepaint;
   }


   private Image logo=null;

   /** Affichage en surimpression de la commande en cours */
   protected void drawBlinkInfo(Graphics g) {
      boolean blink = aladin.calque.isBlinking();
      boolean command = hasCmd();
      boolean form = aladin.view.isFree() && !Aladin.isApplet();
      if( !command && !blink && !form ) {
         if( timer!=null && timer.isRunning() ) timer.stop();
         return;
      }


      int YC=0;

      // Affichage de la commande en cours de saisie
      if( command || form ) {
         int w=200, h=20;
         int x = form && mode!=CINEMA ? getWidth()/2-w/2 : 10;
         int y = form && mode!=CINEMA ? getHeight()/2-100-h/2 : 10;
         YC=y;
         g.setColor(Color.white);
         g.fillRect(x, y, w, h);
         Util.drawEdge(g,x,y,w,h);
         g.setFont(Aladin.BOLD);
         String s = cmd.toString();
         if( form ) {
            g.setColor(Color.black);
            String label = Localisation.YOUROBJ;
            g.drawString(label,getWidth()/2-g.getFontMetrics().stringWidth(label)/2,
                  y+h+15);
         }
         g.setColor(Aladin.COLOR_GREEN);
         if( blinkState ) s=s+"_";
         g.drawString(s, x+5, y+h-5);
      }

      // Affichage du logo au-dessus de la commande si la pile est vide
      if( form && mode!=CINEMA ) {
         try {
            if( logo==null ) logo=aladin.getImagette("Aladin.png");
            else g.drawImage(logo,getWidth()/2-logo.getWidth(viewSimple)/2,
                  YC-logo.getHeight(viewSimple)-10,viewSimple);
         } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
      }

      // Affichage du voyant d'état
      WidgetControl voc = aladin.calque.select.getWidgetControl();
      if( blink && voc.isCollapsed() ) {
         XBLINK = voc.getX()+2;
         YBLINK = voc.getY()-10;
//         XBLINK=getWidth()-XMARGE;
//         YBLINK=getHeight()-YMARGE - nCheck*YGAP -YGAP;
         Slide.drawBall(g, XBLINK, YBLINK, blinkState ? Color.green : Color.white);
      } else XBLINK=-1;

      if( timer==null ) timer = new Timer(500,this);
      if( !timer.isRunning() ) timer.start();
   }

   public void actionPerformed(ActionEvent e) {
      Object src = e.getSource();
      if( src instanceof JMenuItem ) {
         if( currentPlan==null ) return;
         if( src==menuDel ) aladin.calque.Free(currentPlan);
         else if( src==menuProp ) Properties.createProperties(currentPlan);
         return;
      }
      blinkState=!blinkState;
      if( !Aladin.NOGUI ) repaint();
   }

   /** Affichage en surimpression des mesures associées à la première source sélectionnée */
   protected void showMesures() {
      int nbSource = aladin.mesure.getNbSrc();
      if( nbSource==0 || aladin.mesure.frameMesure!=null && aladin.mesure.frameMesure.isVisible() ) return;

      if( aladin.mesure.frameMesure==null ) aladin.mesure.split();
      else if( !aladin.mesure.isVisible() ) {
         aladin.mesure.setReduced(false);
         aladin.mesure.frameMesure.setVisible(true);
      }
   }

   //      protected void drawMesures(Graphics g) {
   //      int nbSource = aladin.mesure.getNbSrc();
   //      if( nbSource>0 ) {
   //         Source src = aladin.mesure.getFirstSrc();
   //         int y=getHeight()-120;
   //         int x=0;
   //         ligne = aladin.mesure.getWordLine(src);
   //         aladin.mesure.mcanvas.drawHead(g,x,y,src,-1);
   //         aladin.mesure.mcanvas.drawLigne(g,x,y+28,ligne,true,-1);
   //         src.print(g, x+20, y-6);
   //         g.setColor(Color.blue);
   //         g.setFont(Aladin.BOLD);
   //         String s = src.plan.label;
   //         if( nbSource>1 ) s=s+" (and "+(nbSource-1)+" other source"+(nbSource==2?"":"s")+")";
   //         g.drawString(s,x+30,y-2);
   //      } else ligne=null;
   //   }

   // Cree le popup menu associe au select
   private void createPopupMenu() {
      Select select = aladin.view.calque.select;
      popMenu = new JPopupMenu();
      JMenuItem j;
      popMenu.add( menuDel=j=new JMenuItem(select.MDEL));
      j.addActionListener(this);
      popMenu.add( menuProp=j=new JMenuItem(select.MPROP));
      j.addActionListener(this);
      getContentPane().add(popMenu);
   }

   // Affiche le popup
   private void showPopMenu(int x,int y) {
      popMenu.show(this,x,y);
   }

   /** Récupération d'une événement mousePressed (issu de ViwSimple.mousePressed())
    * @return true si l'évènement est pris en compte à ce niveau
    */
   protected boolean mousePressed(MouseEvent e1) {
      int x = e1.getX();
      int y = e1.getY();
      boolean rep=false;

      // Passage en interface complète
      if( inIconOut(x,y) ) {
         end();
         return true;
      }

      // Passage à l'image suivante (ou précédente avec SHIFT)
      if( inIconArrow(x,y) ) {
         next(e1.isShiftDown() ? 1 : -1);
         return true;
      }

      // Sauvegarde de la vue
      if( inIconSave(x,y) ) {
         aladin.save.saveFile(1,e1.isShiftDown() ? Save.JPEG : Save.PNG,-1);
         return true;
      }

      // Sous une checkbox ?
      Plan p = getCheckPlan(x, y);
      if( p!=null ) {
         rep=true;
         if( e1.isPopupTrigger() ) {
            currentPlan=p;
            showPopMenu(x,y);
         }
         else {
            p.setActivated(!p.active);
            aladin.view.repaintAll();
         }
      }

      // Dans le logo de la grille
      else if( XGRID>0 && x>=XGRID && x<=XGRID+15 && y>=YGRID && y<=YGRID+15 ) {
         aladin.calque.switchGrid(true);
         rep=true;
      }

      // Clic sur une ancre ou sur un bouton d'une mesure
      else {
         if( ligne!=null ) {
            Enumeration e = ligne.elements();
            Obj o = (Obj)e.nextElement();
            while( e.hasMoreElements() ) {
               Words w = (Words) e.nextElement();
               if( w.inside(x,y) ) {
                  rep=true;
                  if( !w.glu ) continue;
                  w.haspushed=true;
                  if( w.archive ) { end(); w.callArchive(aladin,o,false); }
                  else w.callGlu(aladin.glu,aladin.mesure.mcanvas);
                  return true;
               }
            }
         }
      }
      return rep;
   }
   
   private void menu(int y) {
      boolean a = aladin.jBar.isVisible();
      aladin.jBar.setVisible(y<50);
      if( a!=aladin.jBar.isVisible() ) viewSimple.repaint();
   }
   

   /** Récupération d'une événement mouseMoved (issu de ViwSimple.mousePressed())
    * @return true si l'évènement est pris en compte à ce niveau
    */
   protected boolean mouseMoved(int x,int y) {
      boolean rep=false;
      int cursor=Aladin.DEFAULTCURSOR;
      Plan p;
      currentPlan=null;
      
      menu(y);
      
      // Dans l'icone de sortie du mode fullscreen
      if( inIconOut(x,y)  ) {
         cursor= Aladin.HANDCURSOR;
         rep=true;
         Util.toolTip(viewSimple,aladin.FULLINT);
      }

      // Dans l'icone de l'image suivante
      else if( inIconArrow(x,y)  ) {
         cursor= Aladin.HANDCURSOR;
         rep=true;
         Util.toolTip(viewSimple,aladin.NEXT);
      }

      // Dans l'icone de la sauvegarde
      else if( inIconSave(x,y)  ) {
         cursor= Aladin.HANDCURSOR;
         rep=true;
         Util.toolTip(viewSimple,"Save currentview (PNG format)");
      }

      // Dans le voyant d'état blink
      else if( XBLINK>0 && x>=XBLINK && x<=XBLINK+10 && y>=YBLINK && y<=YBLINK+10 ) {
         String s=aladin.calque.getBlinkingInfo();
         if( s.length()>0 ) s="Waiting "+s+"...";
         Util.toolTip(viewSimple,s);
         rep=true;
      }

      // Dans le logo de la grille
      else if( XGRID>0 && x>=XGRID && x<=XGRID+15 && y>=YGRID && y<=YGRID+15 ) {
         cursor= Aladin.HANDCURSOR;
         Util.toolTip(viewSimple,aladin.grid.getHelpTip(),true);
         rep=true;
      }

      // Sous une checkbox ?
      else if( (p=getCheckPlan(x, y))!=null ) {
         cursor= Aladin.HANDCURSOR;
         rep=true;
         Util.toolTip(viewSimple,p.getInfo());
      }

      // Modification de l'apparence du curseur en fonction de la position éventuelle
      // de la souris sur une ancre ou sur un bouton d'une mesure
      else {
         Util.toolTip(viewSimple,"");
         if( ligne!=null ) {
            Enumeration e = ligne.elements();
            e.nextElement();
            while( e.hasMoreElements() ) {
               Words w = (Words) e.nextElement();
               if( w.inside(x,y) ) {
                  if( !w.glu ) continue;
                  cursor=Aladin.HANDCURSOR;
                  rep=true;
                  break;
               }
            }
         }
      }

      if( cursor!=ocursor ) { ocursor=cursor; Aladin.makeCursor(viewSimple, cursor); }
      return rep;
   }
}
