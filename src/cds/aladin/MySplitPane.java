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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Gestion spécifique des SplitPane propre à Aladin.
 * @authors Thomas & Pierre
 */
public class MySplitPane extends JSplitPane {
   private int memo=-1;             // position avant la fermeture du panel (double clic)
   private int defaultSize=-1;      // en cas de génération position fermée, valeur du memo à utiliser
   
   private boolean flagMesure=false; // Concerne explicitement le panel des mesures
   private Aladin aladin;            // reference
   
   private int mode;   // 0- si le premier component qui nous intéresse, 1 - c'est le second
   
   /**
    * @param orientation
    * @param first
    * @param second
    * @param mode  0- si le component de référence est first, 1 si c'est second
    */
   public MySplitPane(Aladin aladin,int orientation, Component first, Component second, int mode ) {
      super(orientation,true,first,second);
      setBackground( aladin.getBackground());
      setBorder( BorderFactory.createEmptyBorder() );
      setUI(new MyBasicSplitPaneUI());
      this.mode=mode;
      this.aladin = aladin;
      
      if( second instanceof Mesure )  {
         flagMesure=true;
         setDividerSize(7);
      }
   }
   
   
   /** True si le component de référence est réduit au max (invisible) */
   public boolean isReduced() { return getCompSize()<=getDividerSize(); }
   
   /** Cache/Restore le component de référence */
   public void setReduced(boolean flag) {
      if( isReduced()==flag ) return;  // déjà fait
      
      if( flag ) {
         
         int m=getDividerLocation();
         int ref = mode==0 ? getMinimumDividerLocation() : getMaximumDividerLocation();
         if( m!=ref ) {
            memo=m;
            setDividerLocation(ref);
         }
         
      } else {

         // Aucune mémorisation n'a encore été faite car le panel a été généré fermé.
         // Peut être y a-t-il une valeur par défaut (defaultSplit)
         if( memo==-1 ) {
            if( defaultSize!=-1 ) memo = mode==0 ? defaultSize : getMaximumDividerLocation() - defaultSize;
            else return;
         }
         setDividerLocation(memo);
      }
      
      if( flagMesure ) {
         // On cache ou on montre le bandeau de recherche suivant la taille de la fenêtre des mesures
         if( aladin.search.hideSearch( isReduced() ) ) revalidate();
      }
   }
   
   /** Permute l'état Caché/restoré du component de référence */
   public void switchReduced() { setReduced( !isReduced() ); }

   /** Retourne la taille courante du composante de référence */
   public int getCompSize() {
      int max = getOrientation()==VERTICAL_SPLIT ? getHeight() : getWidth();
      return mode==0 ? getDividerLocation() : max - getDividerLocation();
   }
   
   public int getMemo() { return memo; }
   
   public void setDefaultSplit(int splitSize) { this.defaultSize = splitSize; }
   
   /** 
    * Une languette avec 3 petits triangles pour ouvrir ou fermée la fenêtre
    * @param g
    * @param pos 0:Nord, 1:Ouest, 2:Sud, 3:Est
    * @param closed true si tracé en position "fermé"
    * @param h hauteur des triangles
    * @param x,y coin haut-gauche du premier triangle
    */
   static public Rectangle drawLanguette(Graphics g, int pos, boolean closed,int h, int x1, int y1, Color bg, Color fg) {
      
      Rectangle r;
      int w=h*4+h/3;
      
      g.setColor(bg);
      g.fillRect(x1, y1, pos==0 || pos==2 ? w : h, pos==0 || pos==2 ? h : w);
      g.setColor(fg);
      
      // Les bords de la languette
      if( pos==0 || pos==2 ) { 
         g.drawLine(x1,y1,x1,y1+h); g.drawLine(x1+w,y1,x1+w,y1+h);
         if( pos==2 ) g.drawLine(x1,y1,x1+w,y1);
         else g.drawLine(x1,y1+h,x1+w,y1+h);
         r = new Rectangle(x1,y1,w,h);
      } else {
         g.drawLine(x1,y1,x1+h,y1); g.drawLine(x1,y1+w,x1+h,y1+w);
         if( pos==3 ) g.drawLine(x1,y1,x1,y1+w);
         else g.drawLine(x1+h,y1,x1+h,y1+w);
         r = new Rectangle(x1,y1,h,w);
      }
       
      if( pos==0 || pos==1 ) closed=!closed;  // si c'est en haut (resp. à gauche), on inverse simplement le sens des triangles
      
      // Les 3 petits triangles dans le bon sens
      for( int i=0; i<3; i++ ) {
         int x,y;
         if( pos==1 || pos==3 ) { x = x1;             y = y1+h/2+(h+1)*i; }
                           else { x = x1+h/2+(h+1)*i; y = y1+2; }
         
         Polygon p = new Polygon();
         if( pos==0 || pos==2 ) {
            if( !closed ) { p.addPoint(x, y);     p.addPoint(x+h,y);     p.addPoint(x+h/2,y+h); }
                     else { p.addPoint(x, y+h-1); p.addPoint(x+h,y+h-1); p.addPoint(x+h/2,y-1); }
         } else {
            if( !closed ) { p.addPoint(x, y);     p.addPoint(x,y+h);     p.addPoint(x+h,y+h/2); }
                     else { p.addPoint(x+h-1, y); p.addPoint(x-1+h,y+h); p.addPoint(x-1,y+h/2); }
         }
         g.fillPolygon(p);
//         g.drawPolygon(p);
      }
      
      return r;
   }

   class MyBasicSplitPaneUI extends BasicSplitPaneUI {
      public BasicSplitPaneDivider createDefaultDivider() {
         return new MySplitPaneDivider(this);
      }
   }
   
   class MySplitPaneDivider extends BasicSplitPaneDivider  implements MouseListener {
      public MySplitPaneDivider(BasicSplitPaneUI ui) { super(ui); addMouseListener(this);}
      public void paint(Graphics g) {
         if( flagMesure ) {
            if( getCompSize()<10 ) drawMesureLanguette(g);
            else rLanguette=null;
         }
      }
      
      private Rectangle rLanguette=null;  // Dernière position de la languette
      
      private void drawMesureLanguette(Graphics g) {
         int h = getHeight();
         int w = h*4;
         int x = getWidth()-h*7;
         int y = 0;
         
         if( rLanguette==null ) rLanguette = new Rectangle(x,y,w,h);
         
         g.setColor(Color.lightGray);
         g.drawLine(2*getWidth()/3,h-1,x,h-1);
         g.drawLine(x+w-1,h-1,getWidth(),h-1);
         
         drawLanguette(g, 2, true ,h-1, x, y, aladin.getBackground(), Color.gray);
      }
      
      public void mousePressed(MouseEvent e) {
         // EN ATTENDANT DE CREER L'ICONE EN HAUT A DROITE POUR EXTERNALISER CETTE FENETRE
//         if( flagMesure && e.getClickCount()==2 ) { external(); return; }
         
         if( rLanguette==null && e.getClickCount()!=2 ) return;
         if( rLanguette!=null && !rLanguette.contains( e.getPoint() ) && e.getClickCount()!=2 ) return;
         switchReduced();
      }
      
//      public void external() {
//         new FrameExternal(aladin);
//      }
      
      public void mouseClicked(MouseEvent e) { }
      public void mouseReleased(MouseEvent e) { 
         if( flagMesure ) {
            if( aladin.mesure.isMesureOut() ) aladin.mesure.split();
            else {
               // On cache ou on montre le bandeau de recherche suivant la taille de la fenêtre des mesures
               if( aladin.search.hideSearch( isReduced() ) ) revalidate();
            }
         }
      }
      public void mouseEntered(MouseEvent e) { }
      public void mouseExited(MouseEvent e) { }
   }
   
//   MySplitPane() { super(); }
//   
//   // Test pour Vincenzo
//   static public void main(String [] args) {
//      new MySplitPane().new TestFrame();
//   }
//   
//   public final class TestFrame extends JFrame implements VOObserver{
//      protected TestFrame() {
//         super();
//         enableEvents(AWTEvent.WINDOW_EVENT_MASK);
//         
//         JPanel p = (JPanel)getContentPane();
//         p.setLayout( new FlowLayout() );
//         JButton b = new JButton("Launch");
//         b.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//               System.out.println("Aladin starting...");
//               (new Thread() { public void run() { launch(); } }).start();
//            }
//         });
//         p.add(b);
//         
//         b = new JButton("Exec commands");
//         b.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent e) {
//               System.out.println("Exec");
//               (new Thread() { public void run() { exec(); } }).start();
//            }
//         });
//         p.add(b);
//
//
//         pack();
//         setVisible(true);
//      }
//      
//      Aladin aladin=null;
//
//      private void launch() {
//         aladin = Aladin.launch("-debug -nosamp -nobanner");
//         aladin.addObserver(this, VOApp.POSITION);
//      }
//      
//      private void exec() {
//         while( aladin==null ) {
//            try{  Thread.sleep(300); } catch( Exception e) {}
//         }
//         System.out.println("start");
//         aladin.execCommand("setconf frame J2000");
//         aladin.execCommand("load D:/ArmProbeNasmyth.vot");
//         aladin.execCommand("load D:/ArmProbeCassegrain.vot");
//         System.out.println("Stop");
//      }
//      
//
//      
//      public Dimension getPreferredSize() { return new Dimension(300,100); }
//      
//      protected void processWindowEvent(WindowEvent e) {
//         if( e.getID()==WindowEvent.WINDOW_CLOSING ) { System.exit(0); }
//         super.processWindowEvent(e);
//      }
//
//      @Override
//      public void position(double raJ2000, double deJ2000) {
//         System.out.println("Received position("+raJ2000+","+deJ2000);
//      }
//
//      @Override
//      public void pixel(double pixValue) {
//         // TODO Auto-generated method stub
//         
//      }
//   }
//   
//   
//   public final class FrameExternal extends JFrame  {
//
//      // Les references aux objets
//      Aladin a;
//
//     /** Creation du Frame gerant les mesures lorsqu'elles sont dans une fenêtre externe
//      * @param aladin Reference
//      */
//      protected FrameExternal(Aladin aladin) {
//         super("Aladin Java measurements frame");
//         this.a = aladin;
//         Aladin.setIcon(this);
//         enableEvents(AWTEvent.WINDOW_EVENT_MASK);
//         getRootPane().registerKeyboardAction(new ActionListener() {
//               public void actionPerformed(ActionEvent e) { close(); }
//            }, 
//            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
//            JComponent.WHEN_IN_FOCUSED_WINDOW
//         );
//         // pour le bug sous KDE
////       super.show();
//         // test (bug KDE) , le move est effectué entre un show() et un hide()
//         if( !Aladin.LSCREEN ) setLocation(200,300);
//         else setLocation(200,400);
////       super.hide();
//
////         a.mesurePanel.remove(a.mesure);
//         if( a.splitMesureHeight.getBottomComponent()!=null ) aladin.splitMesureHeight.remove(a.mesure);
//         a.mesure.scrollV.setValue(0);
//         a.validate();
//         a.f.validate(); // pour maj frame principale sous Mac
//         a.repaint();
//         
//         JPanel p = (JPanel)getContentPane();
//         p.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
//         p.add(a.mesure,"Center");
//         a.mesure.split(true);
//         pack();
//         setVisible(true);
//      }
//      
//      public Dimension getPreferredSize() {
//         return new Dimension(800,512);
//      }
//      
//      protected void close() {
//         remove(a.mesure);
//         dispose();
//         a.mesure.split(false);
////         Aladin.makeAdd(a.mesurePanel,a.mesure,"Center");
//         if( a.splitMesureHeight.getBottomComponent()==null ) a.splitMesureHeight.setBottomComponent(a.mesure);
//         a.mesure.setPreferredSize(new Dimension(100,150));
//         a.mesure.setMinimumSize(new Dimension(100,0));
//         a.mesure.setReduced(false);
//         if( !Aladin.OUTREACH ) a.search.hideSearch(false);
//         a.getContentPane().validate();
////         a.search.setIcon();
//         a.f.validate(); // pour maj frame principale sous Mac
//         a.getContentPane().repaint();
////         a.split.in();
//      }
//      
//      protected void processWindowEvent(WindowEvent e) {
//         if( e.getID()==WindowEvent.WINDOW_CLOSING ) close();
//         super.processWindowEvent(e);
//      }
//   }

}

