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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;

import cds.tools.Util;

public class Histogramme extends JComponent implements MyListener, MouseListener {
    // Les constantes d'affichage
    static final int mX = 10;      // Marge en abscisse
    static final int mY = 0;      // Marge en ordonnee
    static final int Hp = 150;     // Hauteur du graphique
    static final int W = 256+2*mX; // Largeur totale du graphique
    static final int H = Hp+mY; // Hauteur totale du graphique

    // titre de l'histogramme
    static final String TITLE = "Pixel values histogram";

    // valeurs a memoriser
    double [] hist;                   // histogramme courant de la repartition des niveaux de gris
    Image img=null;                // Image du double buffer contenant l'histogramme

    private int level = -1;          // niveau courant
    private boolean showPixVal = true;

    boolean flag = false;

    // les references
    private PlanImage pimg;                // le plan image concerne
    private byte [] pixels;                // Les pixels de l'image courante


    protected Histogramme() {
        addMouseListener(this);
    }

    protected Histogramme(PlanImage pimg) {
        this();
      if ( (pimg != null) && !(pimg instanceof PlanImageRGB) ) {
        setImage(pimg);
      }
    }

    /** Constructeur prenant un tableau de pixels en paramètre (nécessaire pour PlanContour)
     *
     * @param pixels
     */
    /*
    protected Histogramme(byte[] pixels) {
        this.pixels = pixels;
        computeHist();
    }
    */

    private void computeHist() {
        double max=0;
        int i;

        // Calcul de la repartition des couleurs
        hist = new double[256];

        for( i=0; i<pixels.length; i++ ) {
          int j = pixels[i]<0?256+pixels[i]:pixels[i];

          double c=hist[j]++;
          if( c>max ) max=c;
        }

        for( i=0; i<hist.length; i++ ) {
// Pierre: pour homogeneiser les histo.
// rem. Thomas : je ne suis vraiment pas convaincu par cette modification :
// si il y a des trous dans l'histogramme, pourquoi les cacher ?
// rem Pierre : Après tout pourquoi pas !
          hist[i] = Math.log(1+hist[i]);
//          hist[i] = (i>0 && hist[i]==0)?hist[i-1]:Math.log( hist[i]+1 );
        }

        max = Math.log(1+max);


        // Mise a l'echelle de l'histogramme
        max+=max/5;

        for( i=0; i<hist.length; i++ ) {
          hist[i] = (hist[i]*Hp)/max;
        }

//        setBackground(Aladin.BKGD);
        resize(W,H);
    }

    public boolean mouseMove(Event e,int x,int y) {
        flag = true;
        if( showPixVal && (x-mX)!=level ) {
            setLevel(x-mX);
            repaint();
        }
        return true;
    }

    public boolean mouseExit(Event e,int x,int y) {
        setLevel(-1);
        return true;
    }

    public void paint(Graphics g) {
      if (pimg == null && pixels == null) return;

      if( img==null ) { update(g); return; }
      g.drawImage(img,mX,mY,pimg.aladin);

      if( showPixVal ) drawInfo(g);

   }

   public void update(Graphics gr) {
      //System.out.println(flag);
      if( img==null ) img = createImage(256,Hp);
      else if( flag ) { paint(gr); return; }

      Graphics g=img.getGraphics();
      g.setColor( Color.white );
      g.fillRect(0,0,256,Hp);
      g.setColor( Color.blue );
      for( int i=0; i<256; i++ ) g.drawLine( i,Hp, i,Hp-(int)hist[i] );
      g.setColor( Color.black );
      g.drawRect(0,0,255,Hp-1);

      g.setFont( Aladin.SPLAIN );
      g.drawString(TITLE,W/2-65,14);

      g.dispose();
      paint(gr);
   }

    private void drawInfo(Graphics g) {
        if( level<0 ) return;

        String s1 = pimg.getPixelInfoFromGrey(level);
        String s2 = pimg.getPixelInfoFromGrey(level+1);
        String s = s1.equals(s2)?s1:"["+s1+".."+s2+"]";
        String pixStr = "pixel: "+s;
        // on dessine une bordure autour du texte pour le rendre plus visible
        if( g instanceof Graphics2D ) {
            Graphics2D g2d = (Graphics2D)g;
            Composite saveComposite = g2d.getComposite();
            FontMetrics fm = g2d.getFontMetrics();
            int strWidth = fm.stringWidth(pixStr);
            int strHeight = fm.getMaxAscent()+fm.getMaxDescent();
            g2d.setComposite(Util.getImageComposite(0.55f));
            g2d.setColor(Color.black);
            g2d.fillRoundRect(2+mX, Hp-35-strHeight, mX+strWidth+7, strHeight+6, 20, 20);
            g2d.setComposite(saveComposite);
        }
        g.setColor(Color.magenta);
        g.drawLine(mX+level,Hp+mY,mX+level,0);
        g.drawString(pixStr,7+mX,Hp-35);
    }

    protected void setLevel(int level) {
        if( level==this.level ) return;
        this.level = (level>255)?255:(level<-1)?-1:level;
        repaint();
    }

   public Dimension preferredSize() { return new Dimension(W,H); }
   public Dimension getPreferredSize() { return preferredSize(); }

   protected void setImage(PlanImage pimg) {
       this.pimg = pimg;
       this.pixels = pimg instanceof PlanBG ? ((PlanBG)pimg).getBufPixels8(pimg.aladin.view.getCurrentView())
                         : pimg.getBufPixels8();
       computeHist();
       // on reinitialise img
       img = null;
       flag = false;
       showPixVal = false;
   }

   // Methodes implementant MyListener
   public void fireStateChange(String state) {
       if( state==null ) {
           showPixVal = false;
           repaint();
       }
   }
   public void fireStateChange(int i) {
       showPixVal = true;
       setLevel(i);
   }
   
   public void mousePressed(MouseEvent e) {
       showPixVal = false;
       repaint();
   }
   public void mouseReleased(MouseEvent e) {}
   public void mouseClicked(MouseEvent e) {}
   public void mouseEntered(MouseEvent e) {}
   public void mouseExited(MouseEvent e) {}

}
