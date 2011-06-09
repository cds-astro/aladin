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


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import cds.tools.Util;

/**
 * Gestion dun graphique de nuage de points associ� � une ViewSimple. (cf. plot variable)
 * Le principe consiste � consid�rer les deux valeurs � tracer comme une coordonn�es c�leste (raj,dej)
 * et � utiliser exactement les m�mes m�canismes, simplement avec une projection lin�aire, �ventuellement en log
 * 
 * ATTENTION : Seule la premi�re table du plan peut �tre trac�e.
 * 
 * @author Pierre Fernique [CDS] - septembre 2010
 */
public class Plot {
   
   // les r�f�rences
   private Aladin aladin;
   private ViewSimple viewSimple;

   private Vector<PlotItem> plotTable = null;       // la liste des plots � r�aliser
   private Projection plotProj = null;              // la projection du graphique
   
   public Plot(ViewSimple viewSimple) {
      this.aladin=viewSimple.aladin;
      this.viewSimple = viewSimple;
   }
   
   /** Retourne la projection associ�e au plot */
   public Projection getProj() { return plotProj; }
   
   /** Retourne le titre du plot */
   public String getPlotLabel() {
      StringBuffer s = new StringBuffer();
      for( int i=0; i<2 && i<plotTable.size(); i++ ) { 
         PlotItem p = plotTable.elementAt(i);
         if( s.length()>0 ) s.append(" + ");
         Legende leg = p.plan.getFirstLegende();
         s.append(p.plan.label+"("+leg.getName(p.index[0])+","+ leg.getName(p.index[1])+")");
      }
      if( plotTable.size()>2 ) s.append("...");
      return s.toString();
   }
   
   // Lib�ration du plot
   public void free() {
      plotTable=null;
      plotProj=null;
      viewSimple.newView(1);
      SwingUtilities.invokeLater( new Runnable() {
         public void run() {
            Properties.majProp(2);
         }
      });
   }

   // initialisation du plot
   private void initPlot() {
      if( plotTable==null ) plotTable = new Vector<PlotItem>(10);
      plotProj = new Projection(0,0,250,250,500,500,500,500,false,false,false,false);
   }
   
   // recopie du plot pour la vue pass�e en param�tre
   protected Plot copyIn(ViewSimple v) {
      Plot plot = new Plot(v);
      plot.plotProj = plotProj;
      plot.plotTable = (Vector) plotTable.clone();
      return plot;
   }
   
   /** Suppression du plot concernant le plan pass� en param�tre
    * Met � jour les Properties du plan si elles sont visibles
    */
   public void rmPlotTable(Plan plan) {
      PlotItem p  = findPlotTable(plan);
      if( p==null ) return;
      plotTable.remove(p);
      if( plotTable.size()==0 ) viewSimple.free();
      else viewSimple.newView(1);
      SwingUtilities.invokeLater( new Runnable() {
         public void run() {
            Properties.majProp(2);
         }
      });

   }

   /** Ajout du plot du plan pass� en param�tre pour les colonnes d'indice indexX et indexY
    * Ouvre d'embl�e les properties du plan
    */
   public void addPlotTable(final Plan plan, int indexX, int indexY,boolean openProp) {
      if( plotTable==null ) initPlot();
      if( plan.getLegende().size()>1 ) {
         aladin.warning("Only the first table of this plane\ncan be drawn in a scatter plot.");
      }
      PlotItem p = findPlotTable(plan);
      boolean modify=true;
      if( p==null ) { p = new PlotItem(); plotTable.addElement(p); modify=false; }
      p.index[0] = indexX;
      p.index[1] = indexY;
      p.plan=plan;
      aladin.trace(4,"ViewSimple.addPlotTable: "+(modify?"modify":"add")+" plan="+plan.label+" indexX="+indexX+" indexY="+indexY);
      if( isMainPlot(p) ) adjustPlot(p);
      viewSimple.newView(1);
      if( openProp ) {
         SwingUtilities.invokeLater( new Runnable() {
            public void run() {
               Properties.createProperties(plan);
               Properties.majProp(2);
            }
         });
      }
   }
   
   // retourne true s'il s'agit du plot principale (celui qui d�termine la projection)
   private boolean isMainPlot(PlotItem p) { return p==plotTable.elementAt(0); }
   
   // Juste pour �viter des allocations � r�p�tition
   private Coord c = new Coord();
   
   /** Retourne les positions XY de la source s dans le graphe (sans tenir comtpe du zoom de la vue) */
   public double [] getXY(Source s) { return getXY(null,s); }
   public double [] getXY(double [] xy, Source s) {
      if( xy==null ) xy = new double[2];
      xy = getValues(xy,s);
      if( Double.isNaN(xy[0]) ) return xy;
      try {
         c.al = xy[0];
         c.del = xy[1];
         plotProj.getXY(c);
         xy[0] = c.x;
         xy[1] = c.y;
      } catch( Exception e ) {
         xy[0] = Double.NaN;
         xy[1] = Double.NaN;
       }
      return xy;
   }
   
   /** Retourne les valeurs de la source s pour le graphe */
   public double [] getValues(Source s) { return getValues(null,s); }
   public double [] getValues(double [] val, Source s) {
      if( val==null ) val = new double[2];
      try {
         int [] index = getPlotIndex(s.plan);
         String sx = s.getValue( index[0] ).trim();
         String sy = s.getValue( index[1] ).trim();
         val[0] = Double.parseDouble(sx);
         val[1] = Double.parseDouble(sy);
      } catch( Exception e ) {
         val[0] = Double.NaN;
         val[1] = Double.NaN;
      }
      return val;
   }
   
   /** Ajuste la projection et le zoom afin de contenir la totalit� du nuage de point. Ne prend en comtpe
    * que le plot principale (celui du premier plan)
    * @param p
    */
   public void adjustPlot(PlotItem p) {
      try {
         if( p==null ) p = plotTable.elementAt(0);
         
         double maxX,max1X,minX,min1X;
         double maxY,max1Y,minY,min1Y;
         maxX=max1X=minX=min1X=maxY=max1Y=minY=min1Y=0;
         int n=0;
         boolean first=true;
         Iterator<Obj> it = p.plan.iterator();
         double [] val = null;
         while( it.hasNext() ) {
            Obj o = it.next();
            if( !(o instanceof Source) ) continue;
            Source s = (Source)o;
            val = getValues(val,s);
            if( Double.isNaN(val[0]) ) continue;
            double cX=val[0],cY=val[1];
            n++;
            if( first ) {
               maxX=max1X=minX=min1X=cX;
               maxY=max1Y=minY=min1Y=cY;
               first=false;
            }

            if( minX>cX ) minX=cX;
            else if( maxX<cX ) maxX=cX;
            if( cX<min1X && cX>minX || min1X==minX && cX<max1X ) min1X=cX;
            else if( cX>max1X && cX<maxX || max1X==maxX && cX>min1X ) max1X=cX;

            if( minY>cY ) minY=cY;
            else if( maxY<cY ) maxY=cY;
            if( cY<min1Y && cY>minY || min1Y==minY && cY<max1Y ) min1Y=cY;
            else if( cY>max1Y && cY<maxY || max1Y==maxY && cY>min1Y ) max1Y=cY;
         }
         
         if( min1X==max1X ) { min1X=minX; max1X=maxX; }
         if( min1Y==max1Y ) { min1Y=minY; max1Y=maxY; }
         
         aladin.trace(4,"ViewSimple.adjustPlot: nsrc="+n+" X=["+minX+" ("+min1X+") .. ("+max1X+") "+maxX+"] Y=["+minY+" ("+min1Y+") .. ("+max1Y+") "+maxY+"]");
         int w = viewSimple.getWidth();
         int h = viewSimple.getHeight();
         plotProj = new Projection(0,0,0,0, (max1X-min1X)*1.1, (max1Y-min1Y)*1.1, w, h, 
               plotProj.isFlipXPlot(), plotProj.isFlipYPlot(), plotProj.isLogXPlot(), plotProj.isLogYPlot());
         viewSimple.newView(1);
         viewSimple.setZoomRaDec(1, (min1X+max1X)/2, (min1Y+max1Y)/2);
      } catch( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   
   // d�termine le niveau d'arrondi des valeurs pour le trac� de la grille de coordonn�es
   private int nbRound(double val) {
      if( val==0 ) return 0;
      int n;
      val = Math.abs(val);
      if( val>1 ) return 0;
      for( n=0; val<1 && n<10; n++ ) val *=10;
      return n;
   }
   
   
   /** D�termine r�cursivement le niveau d'incr�ment de la grille (en valeur et non en X ou Y),
    * de proche en proche jusqu'� trouver un espacement raisonnable pour le trac�
    * @param val L'incr�ment en cours (1 au d�part
    * @param index 0 - concerne X, 1 - concerne Y
    * @param sens indique le sens de la progression: 0 - ind�termin�e, 1 - on augmente, -1 - on diminue
    * @param n Au cas o� : pour �viter une boucle infinie
    * @return  l'incr�ment le plus adapt� pour le pas de la grille
    */
   private double getIncr(double val,int index, int sens,int n) {
      double fct = index==0 ? plotProj.getFctXPlot() : plotProj.getFctYPlot(); 
      double x = val * fct * viewSimple.getZoom();
      if( n>20 ) return 0;
      if( x>=50 && x<=200 ) return val;
      if( x<50 ) {
         if( sens==-1 ) return val*2;
         return getIncr(val*10,index,1,++n);
      }
      else {
         if( sens==1 ) return val/2;
         return getIncr(val/10,index,-1,++n);
      }
   }
   
   // Voir getIncr(...
   private double getIncrX()  { return getIncr(1.,0,0,0); }
   private double getIncrY()  { return getIncr(1.,1,0,0); }
   
   /** Trac� de la grille */
   public void drawPlotGrid(Graphics g,int dx,int dy) {
      try {
         double incrX = getIncrX();
         double incrY = getIncrY();
         if( incrX==0 || incrY==0 ) return;
         
         int w = viewSimple.getWidth();
         int h = viewSimple.getHeight();
         Coord c0 = new Coord();
         PointD p = viewSimple.getPosition(0., 0.);
         c0.x=p.x; c0.y=p.y;
         plotProj.getCoord(c0);
         Coord c1 = new Coord();
         p = viewSimple.getPosition((double)w,(double)h);
         c1.x=p.x; c1.y=p.y;
         plotProj.getCoord(c1);
         
         double initValX = (int)(c0.al/incrX) * incrX;
         double initValY = (int)(c0.del/incrY) * incrY;
         int nbRoundX = nbRound(incrX);
         int nbRoundY = nbRound(incrY);
         
         Coord c = new Coord();
         c.del = initValY;
         int i;
         
         for( c.al=initValX, i=0; i<20; c.al+=incrX, i++ ) {
            plotProj.getXY(c);
            Point p1 = viewSimple.getViewCoord(c.x, c.y);
            if( p1==null ) continue;
            g.setColor( Color.lightGray );
            g.drawLine(p1.x+dx,0+dy,p1.x+dx,h+dy);
            g.setColor(Color.black);
            g.drawString(Util.myRound(c.al+"",nbRoundX),p1.x+2+dx, h-5+dy);
         }
         c.al = initValX;
         for( c.del=initValY, i=0; i<20; c.del-=incrY, i++ ) {
            plotProj.getXY(c);
            Point p1 = viewSimple.getViewCoord(c.x, c.y);
            if( p1==null ) continue;
            g.setColor( Color.lightGray );
            g.drawLine(0+dx,p1.y+dy,w+dx,p1.y+dy);
            g.setColor(Color.black);
            g.drawString(Util.myRound(c.del+"",nbRoundY),4+dx,p1.y-5+dy);
         }
         
      } catch( Exception e ) { e.printStackTrace(); }
   }

   // Recherche les colonnes � tracer pour le plot concernant le Plan plan
   private int [] getPlotIndex(Plan plan) {
      PlotItem p = findPlotTable(plan);
      return p.index;
   }
   
   // Retourne le PlotItem concernant le plan pass� en param�tre sinon null
   private PlotItem findPlotTable(Plan plan) {
      Enumeration<PlotItem> e = plotTable.elements();
      while( e.hasMoreElements() ) {
         PlotItem p = e.nextElement();
         if( p.plan==plan ) return p;
      }
      return null;
   }
   
   /** Construit le Panel de contr�le pour le plan pass� en param�tre */
   public JPanel getPlotControlPanelForPlan(Plan plan) {
      PlotItem p = findPlotTable(plan);
      if( p==null ) return null;
      return getPlotControlPanelForOneTable(p);
   }
     
   // Construire le panel de contr�le concernant un plot particulier
   private JPanel getPlotControlPanelForOneTable(PlotItem p) {
      JPanel panel = new JPanel(new BorderLayout(0,0));
      panel.add( getPlotControlPanelForOneIndex(p, 0), BorderLayout.NORTH);
      panel.add( getPlotControlPanelForOneIndex(p, 1), BorderLayout.CENTER);
      panel.add( getPlotButtonPanel(p), BorderLayout.SOUTH);
      return panel;
   }
   
   // Construit le panel des boutons de contr�le
   private JPanel getPlotButtonPanel(final PlotItem p) {
      JPanel panel = new JPanel();
      JButton b;
      b = new JButton("zoom all");
      b.setMargin(new Insets(2,2,2,2) );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           adjustPlot(p);
           viewSimple.repaint();
         }
      });
      panel.add(b);
      b = new JButton("delete");
      b.setMargin(new Insets(2,2,2,2) );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           rmPlotTable(p.plan);
           viewSimple.repaint();
         }
      });
      panel.add(b);
      return panel;
   }
   
   // Construit le panel de contr�le concernant un axe pour un plot particulier
   private JPanel getPlotControlPanelForOneIndex(final PlotItem p,final int n) {
      JPanel panel = new JPanel();
      panel.add( new JLabel( n==0 ? "X: " : "Y: ") );
      final Plan plan = p.plan;
      final JComboBox combo = plan.getFirstLegende().createCombo();
      combo.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            PlotItem p = findPlotTable(plan);
            int nindex = combo.getSelectedIndex();
//            Aladin.trace(4,"getPlotControlPanelForOneIndex: plan="+p.plan.label+" table="+p.leg.name+" col="+n+" index="+nindex);
            if( nindex==p.index[ n ] ) return;
            p.index[n]=nindex;
            adjustPlot(p);
            viewSimple.repaint();
         }
      });
      combo.setSelectedIndex(p.index[n]);
      panel.add(combo);
      
      final JCheckBox b = new JCheckBox("Flip");
      b.setSelected( n==0 ? plotProj.isFlipXPlot() : plotProj.isFlipYPlot() );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           aladin.trace(4,"Plot.getPlotControlPanelForOneIndex: flipPlot("+n+" => "+b.isSelected());
           plotProj.flipPlot(n,b.isSelected());
           adjustPlot(p);
           viewSimple.repaint();
         }
      });
      panel.add(b);
      
      final JCheckBox b1 = new JCheckBox("Log");
      b1.setSelected( n==0 ? plotProj.isLogXPlot() : plotProj.isLogYPlot() );
      b1.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            aladin.trace(4,"Plot.getPlotControlPanelForOneIndex: flipLog("+n+" => "+b1.isSelected());
            plotProj.logPlot(n,b1.isSelected());
            adjustPlot(p);
            viewSimple.repaint();
         }
      });
      panel.add(b1);
      
      return panel;
   }
   
   // Contient les informations n�cessaires au plot, � savoir le plan d'origine et les indices des deux colonnes
   // concern�es. Attention, ne prend que la premi�re table du plan.
   private class PlotItem {
      Plan plan;
      int [] index = new int[2];
   }
}
