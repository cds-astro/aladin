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

package cds.aladin;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import cds.tools.Astrodate;
import cds.tools.Util;

/**
 * Gestion d'un graphique associé à une ViewSimple. (cf. plot variable)
 * 
 * @author Pierre Fernique [CDS]
 * @version 1.0 - septembre 2010 - création
 * @version 2.0 avril 2018 - Plot revisité pour gérer les Times séries
 */
public class Plot {
   
   static public final String PROJBODY = "__PLOT__";  // Body bidon associé à la projection du Plot (voir Projection.isUncompatibleBody()
   
   // les références
   protected Aladin aladin;
   protected ViewSimple viewSimple;

   protected Projection plotProj = null;              // la projection du graphique
   protected ArrayList<PlotItem> plotTable = null;       // la liste des plots à réaliser
   protected boolean flagTime=false;                  // true si l'absisse est du temps
   
   public Plot(ViewSimple viewSimple) {
      this.aladin=viewSimple.aladin;
      this.viewSimple = viewSimple;
      
      // Pour pouvoir passer une référence à un plan pour les projections utilisées dans les plot
      if( planFree==null ) planFree= new PlanFree(aladin,PROJBODY);
   }
   
   /** Retourne la projection associée au plot */
   public Projection getProj() { return plotProj; }
   
   /** Retourne le titre du plot */
   public String getPlotLabel() {
      StringBuffer s = new StringBuffer();
      if( isPlotTime() ) {
         if( !hasTable() ) s.append("Time plot");
         else {
            PlotItem p = getFirstPlotItem();
            Legende leg = p.plan.getFirstLegende();
            String name = leg.getName(p.index[1]);
            String desc = leg.getDescription(p.index[1]);
            String unit = leg.getUnit(p.index[1]);
            if( name!=null && name.length()>0 ) s.append(name);
            if( unit!=null && unit.length()>0 ) s.append(" ["+unit+"]");
            if( desc!=null && desc.length()>0 ) {
               if( desc.length()>40 ) desc = desc.substring(0,36)+"...";
               s.append(": "+desc);
            }
         }
      } else {
         if( hasTable() ) {
            for( int i=0; i<2 && i<plotTable.size(); i++ ) { 
               PlotItem p = plotTable.get(i);
               if( s.length()>0 ) s.append(" + ");
               try {
                  Legende leg = p.plan.getFirstLegende();
                  s.append(p.plan.label+"("+leg.getName(p.index[0])+","+ leg.getName(p.index[1])+")");
               } catch( Exception e ) { }
            }
            if( plotTable.size()>2 ) s.append("...");
         }
      }
      return s.toString();
   }
   
   
   // Libération du plot
   public void free() {
      plotTable=null;
      SwingUtilities.invokeLater( new Runnable() {
         public void run() {
            Properties.majProp(2);
         }
      });
   }
   
   /** retourne true si le plot concerne au moins une table */
   public boolean hasTable() { return plotTable!=null && plotTable.size()>0; }
   
   /** Retourne true si le plot est temporel */
   public boolean isPlotTime() { return flagTime; }
   public boolean isPlotTimeWithoutTable() { return flagTime && !hasTable(); }
   
//   protected void mouseMove(MouseEvent e) {
//      Coord coo = new Coord();
//      PointD p   = viewSimple.getPosition( (double)e.getX(), (double)e.getY());
//      coo.x = p.x;
//      coo.y = p.y;
//      plotProj.getCoord(coo);
//      String s=Astrodate.JDToDate(coo.al);
//      System.out.println("Date: "+s);
//   }
   
   // Retourne le premier PlotItem du graphique
   private PlotItem getFirstPlotItem() {
      if( plotTable==null || plotTable.size()==0 ) return null;
      return plotTable.get(0);
   }

   // initialisation du plot
   protected void initPlot() {
      if( plotTable==null ) plotTable = new ArrayList<>(10);
   }
   
   // recopie du plot pour la vue passée en paramètre
   protected Plot copyIn(ViewSimple v) {
      Plot plot = new Plot(v);
      plot.plotProj = plotProj;
      plot.plotTable = plotTable==null ? null : (ArrayList) plotTable.clone();
      plot.flagTime = flagTime;
      return plot;
   }
  
   /** Ajuste la projection et le zoom afin de contenir la totalité du nuage de point.
    * @param p si null, prend en compte toutes les tables du plot, sinon uniquement celle spécifiée
    */
   public void adjustPlot() { adjustPlot(null); }
   public void adjustPlot(PlotItem p) {
      try {
         boolean first=true;
         double maxX,max1X,minX,min1X;
         double maxY,max1Y,minY,min1Y;
         maxX=max1X=minX=min1X=maxY=max1Y=minY=min1Y=0;
         
         if( hasTable() ) {
//            p = getFirstPlotItem();
            
            PlotItem [] list;
            if( p!=null ) list = new PlotItem[]{ p }; 
            else {
               list = new PlotItem[ plotTable.size() ];
               plotTable.toArray(list);
            }

            int n=0;
            for( PlotItem p1 : list ) {
               Iterator<Obj> it = p1.plan.iterator();
               if( it==null ) continue;
               double [] val = null;
               while( it.hasNext() ) {
                  Obj o = it.next();
                  if( !o.asSource() ) continue;
                  Source s = (Source)o;
                  val = getValues(val,s);
                  if( Double.isNaN(val[0]) || Double.isNaN(val[1]) ) continue;
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

               min1X=minX; max1X=maxX;
               min1Y=minY; max1Y=maxY;
            }

            aladin.trace(4,"ViewSimple.adjustPlot: "+(isPlotTime()?" [plotTime]":"")+" nsrc="+n+" X=["+minX+" ("+min1X+") .. ("+max1X+") "+maxX+"] Y=["+minY+" ("+min1Y+") .. ("+max1Y+") "+maxY+"]");
         }
         double w = viewSimple.getWidth(); // *0.9;
         double h = viewSimple.getHeight() *0.8; 
//         plotProj = new Projection(0,0,0,0, (max1X-min1X)*1.1, (max1Y-min1Y)*1.1, w, h, 
//               plotProj.isFlipXPlot(), plotProj.isFlipYPlot(), plotProj.isLogXPlot(), plotProj.isLogYPlot());
         
         // En cas d'un PlotTime, il faut également voir les TMOC de la pile qui peuvent s'afficher en superposition
         // également mettre en peu de marge en haut et en bas (zoom qu'en abscisse)
         if( isPlotTime() || first ) {
            boolean flagNoSerie = first;
            for( Plan p1 : aladin.calque.getPlans() ) {
               if( !p1.flagOk || !p1.active ) continue;
//               if( !p1.isTimeMoc() ) continue;
               
               double [] timeRange = p1.getTimeRange();
               double tmin = timeRange[0];
               double tmax = timeRange[1];
              
               if( first || tmin<min1X ) { min1X = tmin; first=false; }
               if( first || tmax>max1X ) { max1X = tmax; first=false; }
            }
            
            // Pas de séries temporelles ? on donne tout de même une largeur pour que la projection
            // fonction pour les TMOC éventuels.
            if( flagNoSerie && !first ) {  max1Y = h; min1Y=0; setTimePlot( true ); }
         }
         
         memoMaxTimeRange(min1X, max1X, min1Y, max1Y);
         
         if( plotProj!=null ) {
            plotProj = new Projection(0,0,0,0, (max1X-min1X), (max1Y-min1Y), w, h, 
                  plotProj.isFlipXPlot(), plotProj.isFlipYPlot(), plotProj.isLogXPlot(), plotProj.isLogYPlot(),planFree);
         } else {
            plotProj = new Projection(0,0,0,0, (max1X-min1X), (max1Y-min1Y), w, h, false,false,false,false,planFree);
         }
         viewSimple.newView(1);
         viewSimple.setZoomRaDec(1, (min1X+max1X)/2, (min1Y+max1Y)/2);
         adjustWidgets();
         
      } catch( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   
   static private PlanFree planFree = null;
   
   private double [] maxTimeRange = null;
   private void memoMaxTimeRange(double jdmin, double jdmax, double ymin, double ymax ) { 
      maxTimeRange = new double [] {jdmin,jdmax,ymin,ymax};
   }
   
   /** Positionne la tranche de temps à visualiser dans le PlotTIme courant
    * @param t => jdmin et jdmax (si Double.NaN utilise la plus petite (resp grande) valeur possible
    */
   protected void setTimeRange( double [] t ) {
      if( !isPlotTime() ) return;
      
      double w = viewSimple.getWidth(); // *0.9;
      double h = viewSimple.getHeight() *0.8; 
      double min1X = Double.isNaN( t[0] ) ? maxTimeRange[0] : t[0];
      double max1X = Double.isNaN( t[1] ) ? maxTimeRange[1] : t[1];
      
      plotProj = new Projection(0,0,0,0, (max1X-min1X), plotProj.rm1, w, h, 
            plotProj.isFlipXPlot(), plotProj.isFlipYPlot(), plotProj.isLogXPlot(), plotProj.isLogYPlot(),planFree);

      viewSimple.setZoomRaDec(1, (min1X+max1X)/2, (maxTimeRange[2]+maxTimeRange[3])/2);
      viewSimple.newView(1);
   }
   
   protected double [] getTimeRange() {
      return new double [] { getMin(), getMax() };
   }
   
   /** Retourne les valeurs de la source s pour le graphe */
   public double [] getValues(Source s) { return getValues(null,s); }

   /** Retourne les valeurs de la source s pour le graphe */
   public double [] getValues(double [] val, Source s) {
      if( val==null ) val = new double[2];
      try {
//         int [] index = getPlotIndex(s.plan);
         
         PlotItem p = findPlotTable(s.plan);
         int [] index =  p.index;
         
         if( p.isTime() ) val[0] = s.jdtime;
         else {
            String sx = s.getValue( index[0] ).trim();
            val[0] = Double.parseDouble(sx);
         }
         String sy = s.getValue( index[1] ).trim();
         val[1] = Double.parseDouble(sy);
         
      } catch( Exception e ) {
         val[0] = Double.NaN;
         val[1] = Double.NaN;
      }
      return val;
   }
   
   /** Retourne les positions XY de la source s dans le graphe (sans tenir compte du zoom de la vue) */
   public double [] getXY(Source s) { return getXY(null,s); }
   public double [] getXY(double [] xy, Source s) {
      if( xy==null ) xy = new double[2];
      xy = getValues(xy,s);
      if( Double.isNaN(xy[0]) ) return xy;
      Coord c = new Coord();
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
   
   /** Suppression du plot concernant le plan passé en paramètre
    * Met à jour les Properties du plan si elles sont visibles
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

   /** Ajout du plot du plan passé en paramètre pour les colonnes d'indice indexX et indexY
    * Ouvre d'emblée les properties du plan. Si indexX ou indexY sont à -1, recherche automatique des deux premiers champs numériques
    * @return les indices effectifs des colonnes désignées
    */
   public int [] addPlotTable(final Plan plan, int indexX, int indexY,boolean openProp) {
      if( plotTable==null ) initPlot();
      if( plan.getLegende().size()>1 ) {
         aladin.error("Only the first table of this plane\ncan be drawn in a scatter plot.");
      }
      PlotItem p = findPlotTable(plan);
      boolean modify=true;
      boolean isTime=false;
      if( p==null ) { p = new PlotItem(); plotTable.add(p); modify=false; }
      if( indexX==-1 || indexY==-1 ) {
         Legende leg = plan.getFirstLegende();
         indexX=leg.getTime();
         isTime= indexX>=0;
         if( !isTime ) {
            indexX = leg.getIndexNumericField();
            indexY = leg.getIndexNumericField(indexX);
            
         // Si c'est un graphe temporel, on va d'abord chercher une colonne de flux pour l'ordonnée
         } else {
            indexY = leg.getIndexFluxField();
            if( indexY==-1 ) {
               indexY = leg.getIndexNumericField();
               if( indexY==indexX ) indexY = leg.getIndexNumericField(indexY);
            }
         }
      } else {
         Legende leg = plan.getFirstLegende();
         isTime = leg.field[ indexX ].isTime();
      }
      p.set( plan, indexX, indexY, isTime);
      
      aladin.trace(4,"ViewSimple.addPlotTable: "+(modify?"modify":"add")+" plan="+plan.label+" indexX="+indexX+" indexY="+indexY+" isTime="+isTime);
      
      final boolean flagMainPlot=isMainPlot(p);
      final boolean flagIsTime=isTime;
      if( flagMainPlot ) setTimePlot( isTime );
      adjustPlot(p);
      if( openProp ) {
         SwingUtilities.invokeLater( new Runnable() {
            public void run() {
               Properties.createProperties(plan);
               Properties.majProp(2);
               if( flagMainPlot && flagIsTime ) timePlotButton.setEnabled(false);
            }
         });
      } 
      return new int[] { indexX, indexY };
   }
   
   // retourne true s'il s'agit du plot principale (celui qui détermine la projection)
   private boolean isMainPlot(PlotItem p) { return p==getFirstPlotItem(); }
   
   
   /** Construit le Panel de contrôle pour le plan passé en paramètre */
   public JPanel getPlotControlPanelForPlan(Plan plan) {
      PlotItem p = findPlotTable(plan);
      if( p==null ) return null;
      return getPlotControlPanelForOneTable(p);
   }
     
   // Construire le panel de contrôle concernant un plot particulier
   private JPanel getPlotControlPanelForOneTable(PlotItem p) {
      JPanel panel = new JPanel(new BorderLayout(0,0));
      panel.add( getPlotControlPanelForOneIndex(p, 0), BorderLayout.NORTH);
      panel.add( getPlotControlPanelForOneIndex(p, 1), BorderLayout.CENTER);
      panel.add( getPlotButtonPanel(p), BorderLayout.SOUTH);
      return panel;
   }
   
   private JButton timePlotButton=null;
   private JComboBox<String> comboX=null, comboY=null;
   private JCheckBox flipX=null, flipLog=null;
   
   // Choisit automatiquement la colonne de temps en abscisse
   protected void toTimePlot(PlotItem p) {
      Legende leg = p.plan.getFirstLegende();
      int nindex=leg.getTime();
      if( nindex<0 ) return;
      p.setIndex(0,nindex);
      comboX.setSelectedItem( leg.getNameAndDescription(nindex) );
      setTimePlot( p.isTime() );
      adjustPlot();
      viewSimple.repaint();
   }
   
   private void setTimePlot( boolean flag ) {
      flagTime = flag;
      if( !flag ) {
      try {
         throw new Exception();
      } catch( Exception e ) {
         e.printStackTrace();
      }
      }
      
   }
   
   // Mets à jour l'état des widgets de contrôle
   private void adjustWidgets( ) {
      if( timePlotButton!=null ) timePlotButton.setEnabled( !flagTime );
      if( flipX!=null ) flipX.setEnabled( !flagTime );
      if( flipLog!=null ) flipLog.setEnabled( !flagTime );
   }
   
   // Construit le panel des boutons de contrôle
   private JPanel getPlotButtonPanel(final PlotItem p) {
      JPanel panel = new JPanel();
      JButton b;
      
      if( p.plan.getFirstLegende().getTime()>=0 ) {
         timePlotButton = b = new JButton("Time plot");
         Util.toolTip(b, "Choose automatically the Time stamp column as X");
         b.setMargin(new Insets(2,2,2,2) );
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               toTimePlot(p);
            }
         });
         panel.add(b);
      }

      b = new JButton("zoom all");
      b.setMargin(new Insets(2,2,2,2) );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           adjustPlot();
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
   
   // Récupère le nom du champ en supprimant la description qui peut suivre
   // syntaxe ex:  RAJ2000 - Right ascension
   private String getLegNameFromCombo( String s) {
      int index = s.indexOf(" - ");
      return index<0 ? s : s.substring(0,index);
   }
   
   // Construit le panel de contrôle concernant un axe pour un plot particulier
   private JPanel getPlotControlPanelForOneIndex(final PlotItem p,final int n) {
      JPanel panel = new JPanel();
      
      final Plan plan = p.plan;
      panel.add( new JLabel( n==0?"X:":"Y:") );
      Legende leg = plan.getFirstLegende();
      final JComboBox<String> combo = leg.createCombo(true);

      if( n==0 ) comboX=combo;
      else comboY=combo;
      combo.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            int nindex = p.plan.getFirstLegende().find( getLegNameFromCombo( (String)combo.getSelectedItem() ) );
//            Aladin.trace(4,"getPlotControlPanelForOneIndex: plan="+p.plan.label+" table="+p.leg.name+" col="+n+" index="+nindex);
            if( nindex==p.index[ n ] ) return;
            p.setIndex(n,nindex);
            setTimePlot( p.isTime() );
            adjustPlot();
            viewSimple.repaint();
         }
      });
      combo.setSelectedItem( leg.getNameAndDescription( Math.abs(p.index[n])) );
      panel.add(combo);
      
      final JCheckBox b = new JCheckBox("Flip");
      if( n==0 ) flipX = b;
      if( plotProj!=null ) b.setSelected( n==0 ? plotProj.isFlipXPlot() : plotProj.isFlipYPlot() );
      b.setEnabled( n==1 || !p.isTime() );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
           aladin.trace(4,"Plot.getPlotControlPanelForOneIndex: flipPlot("+n+" => "+b.isSelected());
           plotProj.flipPlot(n,b.isSelected());
           adjustPlot();
           viewSimple.repaint();
         }
      });
      panel.add(b);
      
      final JCheckBox b1 = new JCheckBox("Log");
      if( n==0 ) flipLog = b1;
      if( plotProj!=null ) b1.setSelected( n==0 ? plotProj.isLogXPlot() : plotProj.isLogYPlot() );
      b1.setEnabled( n==1 || !p.isTime() );
      b1.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            aladin.trace(4,"Plot.getPlotControlPanelForOneIndex: flipLog("+n+" => "+b1.isSelected()+")");
            plotProj.logPlot(n,b1.isSelected());
            adjustPlot();
            viewSimple.repaint();
         }
      });
      panel.add(b1);
      
      
      return panel;
   }
   
   // détermine le niveau d'arrondi des valeurs pour le tracé de la grille de coordonnées
   private int nbRound(double val) {
      if( val==0 ) return 0;
      int n;
      val = Math.abs(val);
      if( val>1 ) return 0;
      for( n=0; val<1 && n<10; n++ ) val *=10;
      return n;
   }
   
   
   /** Détermine récursivement le niveau d'incrément de la grille (en valeur et non en X ou Y),
    * de proche en proche jusqu'à trouver un espacement raisonnable pour le tracé
    * @param val L'incrément en cours (1 au départ
    * @param index 0 - concerne X, 1 - concerne Y
    * @param sens indique le sens de la progression: 0 - indéterminée, 1 - on augmente, -1 - on diminue
    * @param n Au cas où : pour éviter une boucle infinie
    * @return  l'incrément le plus adapté pour le pas de la grille
    */
   private double getIncr(double val,int index, int sens,int n) {
      double fct = index==0 ? plotProj.getFctXPlot() : plotProj.getFctYPlot(); 
      double zoom = index==1 && isPlotTime() ? 1 : viewSimple.getZoom(); 
      double x = val * fct * zoom;
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
   
//   private void drawReticle(Graphics g) {
//      if( !isPlotTime() && viewSimple!=null && viewSimple.lastView!=null ) return;
//      int x = (int)viewSimple.lastView.x;
//      g.setColor(Color.magenta);
//      g.drawLine(x, 0, x, viewSimple.getHeight()-20);
//   }
   
   /** Tracé de la grille */
   public void drawGrid(Graphics g,int dx,int dy) {
//      drawReticle(g);
      try {
         double incrX = plotProj.isLogXPlot() ? 1e-10 : getIncrX();
         double incrY = plotProj.isLogYPlot() ? 1e-10 : getIncrY();
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
         
         double initValX = plotProj.isLogXPlot() ? 0 : (int)(c0.al/incrX) * incrX;
         double initValY = plotProj.isLogYPlot() ? 0 : (int)(c0.del/incrY) * incrY;
         int nbRoundX = nbRound(incrX);
         int nbRoundY = nbRound(incrY);
         
         Coord c = new Coord();
         String s1;
         int i;
         Composite saveComposite = null;
         Stroke st = null;

         try {
            if( g instanceof Graphics2D ) {
               saveComposite = ((Graphics2D)g).getComposite();
               ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, aladin.view.opaciteGrid));
               ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
               st = ((Graphics2D)g).getStroke();
               float epaisseur = aladin.configuration.getGridThickness();
               if( epaisseur<=0 ) {
                  epaisseur = w<1500 ? 0.5f : w/1500f;
                  if( epaisseur<0.5f ) epaisseur=0.5f;
                  else if( epaisseur>2f ) epaisseur=2f;
               }
               ((Graphics2D)g).setStroke(new BasicStroke(epaisseur));
           }

            String prevDate = null;
            
            c.del = plotProj.isLogYPlot() ? 0.1 :initValY;
            for( c.al=initValX, i=-10; i<200; c.al+=incrX, i++ ) {
               plotProj.getXY(c);
               Point p1 = viewSimple.getViewCoord(c.x, c.y);
               if( p1!=null ) {
                  g.setColor( Color.lightGray );
                  g.drawLine(p1.x+dx,0+dy,p1.x+dx,h+dy);
                  //               g.setColor(Color.black);
                  if( isPlotTime() ) {
                     s1 = Astrodate.JDToDate( c.al );
                     int offset = s1.indexOf('T');
                     if( offset>0 ) {
                        String d1 = s1.substring(0,offset);
                        String h1 = s1.substring(offset+1);
                        s1 = d1.equals(prevDate) ? h1 : d1;
                        prevDate=d1;
                     }
                  }
                  else {
                     if( plotProj.isLogXPlot() )  s1 = i<0 ? "1e"+i : i==0 ? "1" : "1e+"+i;
                     else s1 = c.al+"";
                  }
                  String s2 = s1.indexOf(':')>=0 ? s1 : Util.myRound(s1,nbRoundX);
                  g.drawString(s2,p1.x+2+dx, h-5+dy);
               }
               if( p1==null || p1.x+dx>w ) break;
               if( plotProj.isLogXPlot() ) incrX*=10;
            }
            if( hasTable() ) {
               c.al = plotProj.isLogXPlot() ? 0.1 : initValX;
               for( c.del=initValY, i=-10; i<200; c.del = c.del + (plotProj.isLogYPlot() ? incrY : -incrY), i++ ) {
                  plotProj.getXY(c);
                  Point p1 = viewSimple.getViewCoord(c.x, c.y);
                  if( p1!=null ) {
                     g.setColor( Color.lightGray );
                     g.drawLine(0+dx,p1.y+dy,w+dx,p1.y+dy);
                     //               g.setColor(Color.black);
                     if( plotProj.isLogYPlot() )  s1 = i<0 ? "1e"+i : i==0 ? "1" : "1e+"+i;
                     else s1 = c.del+"";
                     g.drawString(Util.myRound(s1,nbRoundY),4+dx,p1.y-5+dy);
                  }
                  if( p1==null || p1.y+dy>h ) break;
                  if( plotProj.isLogYPlot() ) incrY*=10;
               }
            }
         } finally {
            // on restaure le composite
            if( g!=null ) {
               ((Graphics2D)g).setComposite(saveComposite);
               if( st!=null ) {
                  ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                  ((Graphics2D)g).setStroke(st);
               }
            }
         }


      } catch( Exception e ) { e.printStackTrace(); }
   }

   // Retourne le PlotItem concernant le plan passé en paramètre sinon null
   protected PlotItem findPlotTable(Plan plan) {
      if( !hasTable() ) return null;
      for( PlotItem p : plotTable ) {
         if( p.plan==plan ) return p;
      }
      return null;
   }
   
   /** Retourne la valeur minimale correspondante à l'absisse 0 de la vue du plot */
   public double getMin() { return getValue(0); }
   
   /** Retourne la valeur maximale correspondante à l'absisse max de la vue du plot */
   public double getMax() { return getValue( viewSimple.getWidth() ); }
   
   /** Retourne le temps (JD) correspondant à la position souris dans la vue du plot */
   public double getValue(double xview) {
      double val;
      try {
         PointD p = viewSimple.getPosition(xview, 0);
         Coord c = new Coord();
         c.x=p.x; c.y=p.y;
         plotProj.getCoord(c);
         val = c.al;
      } catch( Exception e ) { val = Double.NaN; }
      return val;
   }
   
   // Contient les informations nécessaires au plot, à savoir le plan d'origine et les indices des deux colonnes
   // concernées. Attention, ne prend que la première table du plan.
   protected class PlotItem {
      Plan plan;
      int [] index = new int[2];
      boolean flagIsTime=false;
      
      void set(Plan p, int indexX, int indexY, boolean isTime ) {
         plan      = p;
         index[0]  = indexX;
         index[1]  = indexY;
         flagIsTime= isTime;
      }

      void setIndex(int n, int nindex) {
         
         // si c'est une ordonnée, cas trivial 
         if( n==1 ) index[1] = nindex;
         
         // Sinon il faut vérifier que ce n'est pas le champ temporel, et si c'est le cas l'indiquer
         else {
            index[0]=nindex;
            Legende leg = plan.getFirstLegende();
            flagIsTime=(leg!=null && leg.getTime()==nindex);
         }
      }
      
      // Retourne true s'il s'agit d'un plot temporel
      boolean isTime() { return flagIsTime; }
      
      public String toString() { return "Plot["+(plan==null?"null":plan.label)+"] index="+index[0]+","+index[1]+" flagisTime="+flagIsTime; }
   }

   
}
