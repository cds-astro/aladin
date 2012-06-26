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
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * JPanel de gestion du Zoom et de la loupe
 *
 * @author Pierre Fernique [CDS]
 * @author Anais Oberto [CDS]
 * @version 1.3 : (fev 2003) Petit peaufinage sur changeValue()
 * @version 1.2 : (dec 2001) Incorporation mode RGB
 * @version 1.1 : (27 mai 99) Correction du centrage du zoom
 * @version 1.0 : (11 mai 99) Toilettage du code
 * @version 0.91 : Revisite le 1 dec 98
 * @version 0.9 : (??) creation
 */
public final class Zoom extends JPanel {

   // Les valeurs generiques
   static int mzn[] = {    1,  1,  1,  1, 1, 1, 1, 2 }; // Valeur zoom < 1, Numerateur
   static int mzd[] = {  128, 64, 32, 16, 8, 4, 2, 3 }; // Valeur zoom < 1, Denominateur
   static final int MINZOOM=mzn.length; // Nombre de valeurs zoom <1
   static final int MAXZOOM=25;   // en puissance de 2, valeur maximal du zoom
   
   static public final int MINSLIDER=2;
   static public final int MAXSLIDER=18;

   // Les conposantes de l'objet
   ZoomView   zoomView;          // Le canvas associe au Zoom
   JComboBox   cZoom;               // Le Choice des differentes valeurs de zoom
   protected ZoomChoice zoomChoicePanel;
   protected SliderSize sizeSlider;
   protected SliderOpacity opacitySlider;
   protected SliderZoom zoomSlider;
   
   static boolean SLIDER_LOOK = false;

   // Les references aux objets
//   protected ViewSimple v;      // La vue associ�e au zoom
   Aladin aladin;

  /** Creation du JPanel du zoom.
   * @param calque,aladin References
   */
   protected Zoom(Aladin aladin) {
      int i;
      this.aladin = aladin;
      zoomView = new ZoomView(aladin);
     
      SLIDER_LOOK = Aladin.NEWLOOK_V7;
      setLayout( new BorderLayout(5,SLIDER_LOOK?10:0) );

      cZoom = new JComboBox();
      cZoom.setFont(cZoom.getFont().deriveFont(Font.PLAIN));
      for( i=0; i<MINZOOM; i++ ) cZoom.addItem(mzn[i]+"/"+mzd[i]+"x");
      for( i=0; i<MAXZOOM; i++ ) cZoom.addItem((0x1<<i)+"x");
      cZoom.setSelectedIndex(MINZOOM);   // Selectionne par defaut le zoom a 1x
      zoomChoicePanel = new ZoomChoice(aladin,cZoom);
      cZoom.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { submit(); }
      });
      cZoom.addMouseWheelListener( zoomView );
      
      if( SLIDER_LOOK ) {
         /* if( Aladin.PROTO ) */sizeSlider = new SliderSize(aladin);
         opacitySlider = new SliderOpacity(aladin);
         zoomSlider = new SliderZoom(this);
         
         JPanel sliderPanel = new JPanel( new BorderLayout(2,2));
         if( sizeSlider!=null ) sliderPanel.add(sizeSlider,BorderLayout.NORTH);
         sliderPanel.add(opacitySlider,BorderLayout.CENTER);
         sliderPanel.add(zoomSlider,BorderLayout.SOUTH);
         
         add(sliderPanel,BorderLayout.NORTH);
      }
      
      Aladin.makeAdd(this,zoomView,SLIDER_LOOK?"Center" : "East");
   }
   
   /** Retourne le JPanel contenant le menu d�roulant du s�lecteur
    *  du facteur du zoom
    */
   protected JPanel getZoomChoicePanel() { return (JPanel)zoomChoicePanel; }

   /** Retourne le facteur de zoom existant le plus proche de
    *  celui pass� en param�tre
    */
   protected double getNearestZoomFct(double z) {
      int n=cZoom.getItemCount();
      double min=Double.MAX_VALUE;
      double nz=getValue(0);
      for( int i=1; i<n; i++ ) {
         double x=getValue(i);
         double diff = Math.abs(z-x);
         if( diff<min ) { min=diff; nz=x; }
      }
      return nz;
   }

  /** Retourne le numero d'index du zoom.
   * @param sZoom libelle du zoom (1/16x, ... , 1x, ... 32x)
   * @return index dans le selecteur, -1 si non trouve
   */
   protected int getIndex(String sZoom) {
      for( int i=cZoom.getItemCount()-1; i>=0; i-- ) {
         String s=(String)cZoom.getItemAt(i);
         if( s.equals(sZoom) ) return i;
      }
      return -1;
   }

   /** Retourn le facteur de zoom le plus proche pour un angle donn�.
    * L'angle est par d�faut en ARCMIN, mais peut �tre suivi d'une unit�
    * Le calcul est op�r� sur la vue par d�faut */
   protected double getNearestZoomFromRadius(String radius) {
      try {
         double deg = Server.getAngle(radius, Server.RADIUS)/60.;
         ViewSimple v = aladin.view.getCurrentView();
         double pixelSize = v.getProj().getPixResDelta();
         double nbPixel = deg / pixelSize;
         double viewSize = v.getHeight();
         double z = viewSize / nbPixel;
         return getNearestZoomFct(z);
      } catch( Exception e ) {
         return -1;
      }
   }
   
  /** Retourne la valeur courante du zoom.
   * @return le facteur d'agrandissement du zoom courant
   * (x1/4,x1/3,x1/2,x1,x2,x4,x8...x32)
   */
   protected double getValue() {
      if( zoomSlider!=null ) {
         int n = zoomSlider.getValue();
         cZoom.setSelectedIndex(n);
      }
      return getValue(cZoom.getSelectedIndex());
   }
   protected double getValue(int i) {
      if( i>=MINZOOM ) return (0x1<<(i-MINZOOM));
      return (double)mzn[i]/mzd[i];
   }

  /** Retourne le nombre de pixels "sources" pour un zoom donn� */
   protected int getNbPixelSrc(double z) {
      int i = this.getIndex(z);
      return (i>=MINZOOM)?1:mzd[i];
   }

  /** Retourne le nombre de pixels "destinations" pour un zoom donn� */
   protected int getNbPixelDst(double z) {
      int i = this.getIndex(z);
      return (i>=MINZOOM)?(0x1<<(i-MINZOOM)):mzn[i];
   }
      
   /** Retourne l'item (fraction) correspondant au zoom */
   protected String  getItem(double z) { return (String)cZoom.getItemAt(getIndex(z)); }

  /** Retourne l'index courant du zoom.
   * @return l'index du zoom
   */
   protected int getIndex() {
      return cZoom.getSelectedIndex();
   }
   
  /** Retourne la prochaine valeur du zoom.
   * @param sens Sens de la modif 1 -> plus grand,-1 -> plus petit.
   * @return le nouveau fct de zoom, ou -1 si probl�me
   */
   protected double getNextValue(double z,int sens) {
      int i = getIndex(z);
      if( i+sens<0 || i+sens>MINZOOM+MAXZOOM ) return -1;
      i=i+sens;
      return getValue(i);
   }


   /** Positionne le zoom � un facteur donn� et demande un r�affichage
    *  Utilis� par Command
    *  @return true si possible, false sinon
    */
   protected boolean setZoom(String fct) {
      if( fct.equals("+") || fct.equals("plus")) incZoom(1);
      else if( fct.equals("-") ) incZoom(-1);
      else {
         double z = -1;
         int i = getIndex(fct);                // Valeur particuli�re de zoom ? ex: 4x
         if( i>=0 ) z = getValue(i);
         else z = getNearestZoomFromRadius(fct);   // Expression d'une angle sur le ciel ? ex: 1�
         if( z<0 ) return false;
         aladin.view.setZoomRaDecForSelectedViews(z,null);
      }
      return true;
   }
   
   /** Incr�ment, ou d�cr�ment du zoom */
   protected void incZoom(int sens) {
      aladin.view.setZoomRaDecForSelectedViews(getNextValue(getValue(),sens),null);
   }
   
  /** Recalcul le zoom a l'emplacement courant */
   protected void newZoom() {
      ViewSimple v = aladin.view.getCurrentView();
      if( v!=null ) zoomView.newZoom(v.xzoomView,v.yzoomView);
   }
   
   /** Action � faire si le cZoom a �t� modifi�
   protected void submit() {
      v.setZoom(getValue(),v.xzoomView,v.yzoomView);
      newZoom();
      v.repaint();
   }
    */
   
   // Pour �viter que la mise � jour du choice du zoom effectue une synchronisation intempestive
   private boolean flagNoAction=false;

   /** Action � faire si le cZoom a �t� modifi� */
   protected void submit() {
// Ca fait trop de baratin de la console      
//      aladin.pad.setCmd("zoom "+cZoom.getSelectedItem());
      if( !flagNoAction ) aladin.view.setZoomRaDecForSelectedViews(getValue(),null);
   }

  /** Obtention du zoom courant.
   * Retourne le rectangle du zoom dans les coord. de l'image courante
   * de la vue courante
   * @param Le rectangle du zoom ou <I>null</I> si pb.
   */
   protected Rectangle getZoom() {
      ViewSimple v = aladin.view.getCurrentView();
      if( v==null || v.rzoom==null ) return null;
      return new Rectangle(floor(v.rzoom.x),floor(v.rzoom.y),top(v.rzoom.width),top(v.rzoom.height));
   }

  /** Positionne la valeur courante du zoom en fonction d'un double */
   protected void setValue(double z) {
      int i=getIndex(z);
      if( i!=-1 && cZoom.getSelectedIndex()!=i ) {
         flagNoAction=true;
         cZoom.setSelectedIndex(i);
         if( zoomSlider!=null ) zoomSlider.setValue(i);
         flagNoAction=false;
      }
  }

  /** Retourne l'indice du Choice en fonction d'une valeur r�elle
   *  de zoom. -1 si rien ne correspond
   */
   private int getIndex(double z) {
      int n=cZoom.getItemCount();
      
      for( int i=0; i<n; i++ ) {
         if( getValue(i)>=z ) return i;
      }
      return -1;
   }
   
   public void zoomSliderReset() {
      if( zoomSlider!=null ) zoomSlider.setEnabled( !aladin.calque.isFree() );
   }

  /** R�initialise le zoom
   * @param withImagette true si on resette �galement l'imagette
   */
   protected void reset() { reset(true); }
   protected void reset(boolean withImagette) {
      
      // Pour que le zoomView reextrait une imagette
      if( withImagette ) zoomView.resetImgID();
      
// FAIT DANS ZOOMVIEW.CALCULZOOM
//      ViewSimple v = aladin.view.getCurrentView();
//      if( v!=null ) setValue(v.zoom);
      
      newZoom();
   }
   
  /** Activation de la loupe */
   protected void wenOn() {
      if( aladin.toolBox.tool[ToolBox.WEN].mode==Tool.DOWN ) zoomView.wenOn();
   }

  /** Desactivation de la loupe */
   protected void wenOff() {
      if( aladin.toolBox.tool[ToolBox.WEN].mode!=Tool.UP ) zoomView.wenOff();
   }

  /** Mise a jour la loupe si necessaire.
   * @param x,y Centre courant
   */
   protected boolean redrawWen(double x, double y) {
      if( aladin.toolBox.tool[ToolBox.WEN].mode==Tool.DOWN ) { zoomView.wen(x,y); return true; }
      return false;
   }
   
   /** Mise a jour la coupe si necessaire */
    protected void redrawCut() {
       if( zoomView.flagCut ) zoomView.repaint();
    }
    
   // Arrondi plus facile � �crire que (int)Math.round()
   static protected int round(double x) { return (int)(x+0.5); }
   static protected int floor(double x) { return (int)x; }
   static protected int top(double x) { return (int)x==x ? (int)x : (int)(x+1); }

}
