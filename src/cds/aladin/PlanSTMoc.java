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

import java.awt.Graphics;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import cds.moc.Moc;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Hpix;

/**
 * Génération d'un plan TMOC
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 Création - fév 2018
 */
public class PlanSTMoc extends PlanTMoc {
   
   private PlanMocFromST  smocH = null; // Le plan SMOC pour l'highlight de la visu spatiale
   private PlanTMocFromST tmocH = null; // Le plan TMOC pour l'highlight de la visu temporelle
   private PlanMocFromST  smocS = null; // Le plan SMOC pour la sélection de la visu spatiale
   private PlanTMocFromST tmocS = null; // Le plan TMOC pour la sélection de la visu temporelle

   
   public PlanSTMoc(Aladin a) {
      super(a);
      type = ALLSKYSTMOC;
      initST();
   }
   
   protected PlanSTMoc(Aladin aladin, MyInputStream in, String label, Coord c, double radius, String url) {
      super(aladin);
      this.dis   = in;
      this.url = url;
      type = ALLSKYSTMOC;
      useCache = false;
      frameOrigin = Localisation.ICRS;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="STMOC";
      setLabel(label);
      co=c;
      coRadius=radius;
      aladin.trace(3,"STMOC creation: "+Plan.Tp[type]+(c!=null ? " around "+c:""));
      suite();
   }
   
   protected PlanSTMoc(Aladin aladin, STMoc moc, String label, Coord c, double radius, String url) {
      super(aladin);
      this.moc = moc;
      this.url = url;
      useCache = false;
      frameOrigin = Localisation.ICRS;
      type = ALLSKYSTMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="STMOC";
      setLabel(label);
      co=c;
      coRadius=radius;
      aladin.trace(3,"STMOC creation: "+Plan.Tp[type]+(c!=null ? " around "+c:""));
      suite();
   }
   
   protected void suiteSpecific() { super.suiteSpecific(); initST(); }
   
   /** Retourne true si le STMOC ne contient qu'un range de temps, potentiellement modifiable */
   protected boolean isOneTimeRange() { return false; }
   
   protected void changeTimeRange(double jdmin, double jdmax) throws Exception {
      if( !isOneTimeRange() ) throw new Exception("Not a oneTimeRange STMOC");
      long min = (long)(jdmin*TMoc.DAYMICROSEC);
      long max = (long)(jdmax*TMoc.DAYMICROSEC)+1L;
      STMoc m = (STMoc)moc;
      m.range.r[0]=min;
      m.range.r[1]=max;
   }
   
   private double cov=-1;
   private double getFullCoverage() {
      if( cov>=0 ) return cov;
      if( cov==-2 ) return -1;
      cov=-2;
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            try {
               cov = (((STMoc) moc).getSpaceMoc()).getCoverage();
            } catch( Exception e ) { }
        }
      });

      return cov;
   }

   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      STMoc stmoc = (STMoc) moc;
      boolean timeEmpty = stmoc.getTimeMin()== -1;
      if( !timeEmpty ) {
         ADD( buf, "\n* Start: ",Astrodate.JDToDate( stmoc.getTimeMin()));
         ADD( buf, "\n* End: ",Astrodate.JDToDate( stmoc.getTimeMax()));
      }
      ADD( buf,"\n* # ranges: ",stmoc.getNbRanges()+"");
      int timeOrder = stmoc.getTimeOrder();
      ADD( buf,"\n* Time res: ",Util.getTemps(  TMoc.getDuration(timeOrder)));
      int drawOrder = timeEmpty ? -1 : getDrawOrder();
      ADD( buf,"\n","* Time order: "+ (timeOrder==drawOrder ? timeOrder+""  : "draw:"+drawOrder+"/"+timeOrder));

      double cov=getFullCoverage();
      boolean spaceEmpty = cov==0;
      double degrad = Math.toDegrees(1.0);
      double skyArea = 4.*Math.PI*degrad*degrad;
      ADD( buf, "\n \n* Space: ",spaceEmpty?"--empty--":Coord.getUnit(skyArea*cov, false, true)+"^2, "+Util.round(cov*100, 3)+"% of sky");
      int spaceOrder =stmoc.getSpaceOrder();
      ADD( buf,"\n* Space res: ",( Coord.getUnit( CDSHealpix.pixRes(spaceOrder)/3600.) ));
      drawOrder = spaceEmpty ? -1 : getSpaceDrawOrder();
      ADD( buf,"\n","* Space order: "+ (drawOrder==-1 ? spaceOrder : spaceOrder==drawOrder ? spaceOrder+"" : "draw:"+drawOrder+"/"+spaceOrder) );
      
      if( !spaceEmpty || !timeEmpty ) ADD( buf,"\n \nRAM: ",Util.getUnitDisk( stmoc.getMem() ) );

   }
   
   protected int getSpaceDrawOrder() { return lastOrderDrawn; }

   /** Retourne le time stamp minimal */
   protected double getTimeMin() { 
      double tmin = ((STMoc)moc).getTimeMin();
      if( tmin==-1 ) tmin=Double.NaN;
      return tmin;
   }

   /** Retourne le time stamp maximal */
   protected double getTimeMax() { 
      double tmax = ((STMoc)moc).getTimeMax();
      if( tmax==-1 ) tmax=Double.NaN;
      return tmax;
   }

   protected boolean waitForPlan() {
      if( dis!=null ) {
         error=null;
         try {
            if( moc==null && dis!=null ) {
               moc = new STMoc();
               readMoc(moc,dis);
            }
//            if( moc.isEmpty() ) error="Empty STMOC";
         }
         catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            error="MOC error";
            return false;
         }
      }

      return true;
   }
   
   /** Retourne le SMoc correspondant à l'intervalle temporelle courant pour la vue courante,
    * ou null si impossible à définir */
   protected SMoc getCurrentSpaceMoc(ViewSimple v) { return getCurrentSpaceMoc(v,false); }
   protected SMoc getCurrentSpaceMoc(ViewSimple v, boolean echoCommand) {
      double t[] = v.getTimeRange();
      if( v==aladin.view.getCurrentView() ) oLastDrawTimeRange = t;
      
      long tmin = Double.isNaN( t[0]) ?            -1L :  (long)( t[0]*TMoc.DAYMICROSEC );
      long tmax = Double.isNaN( t[1]) ? Long.MAX_VALUE :  (long)( t[1]*TMoc.DAYMICROSEC );
      
      // echo de la commande script équivalente
      if( echoCommand ) {
         String range = 
               Double.isNaN( t[0] ) && Double.isNaN( t[1] ) ? "":
               Double.isNaN( t[0] ) ? " -timeRange=NaN/"+Astrodate.JDToDate(t[1]):
               Double.isNaN( t[1] ) ? " -timeRange="+Astrodate.JDToDate(t[0])+"/NaN":
                  " -timeRange="+Astrodate.JDToDate(t[0])+"/"+Astrodate.JDToDate(t[1]);
                  ;
         aladin.console.printCommand("cmoc"+range+" "+Tok.quote(this.label));
      }
      
      try {
         return ((STMoc)moc).getSpaceMoc(tmin, tmax);
       } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return null;
   }
   
   protected SMoc getCurrentSpaceMoc(ViewSimple v, STMoc moc ) {
      double t[] = v.getTimeRange();
      if( v==aladin.view.getCurrentView() ) oLastDrawTimeRange = t;
      
      long tmin = Double.isNaN( t[0]) ?            -1L :  (long)( t[0]*TMoc.DAYMICROSEC );
      long tmax = Double.isNaN( t[1]) ? Long.MAX_VALUE :  (long)( t[1]*TMoc.DAYMICROSEC );
      try {
         return moc.getSpaceMoc(tmin, tmax);
       } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return null;
   }
   
   /** Retourne le TMoc correspondant au champ spatial de la vue courante,
    * ou null si impossible à définir */
   protected TMoc getCurrentTimeMoc() {
      try {
         return ((STMoc)moc).getTimeMoc( isDisplayedInView() ? oLastDrawMoc : null );
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return null;
   }
   
   /** Retourne le STMoc correspondant à la sélection courante */
   protected STMoc getCurrentSpaceTimeMoc() {
      try {
         return new STMoc( (TMoc)tmocS.moc, (SMoc)smocS.moc);
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return null;
   }
   
   /** Retourne true s'il y a une sélection d'une sous-partie du STMOC en cours */
   protected boolean hasSelection() { return true; }   // <= DEVRAI TETRE PLUS TATILLON
   
   private double [] oLastDrawTimeRange = null;
   private SMoc oLastDrawMoc = null;
   
   
   private boolean isDisplayedInView() {
      if( Aladin.TIMETEST ) return false;
      
      int m=aladin.view.getNbView();
      for( int i=0; i<m; i++ ) {
         ViewSimple v = aladin.view.viewSimple[i];
         if( v.isFree() ) continue;
         if( v.selected && !v.isPlotTime() ) return true;
      }
      return false;
   }
   
   // Retourne la première vue TimePlot dans laquelle le STMOC est tracé
   // null si aucune
   private ViewSimple getViewPlot() { 
      int m=aladin.view.getNbView();
      for( int i=0; i<m; i++ ) {
         if( aladin.view.viewSimple[i].isPlotTime() ) return aladin.view.viewSimple[i];
      }
      return null;
   }
   
   // Retourne true s'il y a au moins une vue qui affiche le STMOC sous
   // la forme temporelle
   private boolean isDisplayedInPlot() { 
      ViewSimple v = getViewPlot();
      return v!=null && v.selected; 
   }
   
   protected void memoNewTime() {
//      System.out.println("memoNewTime");
      ViewSimple v = aladin.view.getCurrentView();
      oLastDrawTimeRange = v.getTimeRange();
      oiz=-1;
      askForRepaint();
   }

   protected void memoNewSpace() {
//      System.out.println("memoNewSpace");
      oLastDrawMoc = isDisplayedInView() ? getLastDrawMoc() : null;
      mocTimeLowReset();
      oiz=-1;
      askForRepaint();
   }

   protected boolean isSpaceModified () {
      if( !isDisplayedInPlot() ) return false;
      Moc m = isDisplayedInView() ? getLastDrawMoc() : null;
      boolean rep = !mocEquals(m,oLastDrawMoc);
//      System.out.println("isSpaceModified = "+rep);
      return rep;
   }
   
   /** Return true if the two MOCs are equals. May b null */
   public boolean mocEquals(Moc m1, Moc m2) {
      if( m1==null && m2==null ) return true;
      if( m1==null || m2==null ) return false;
      return ((SMoc)m1).equals(m2);
   }

   protected boolean isTimeModified () {
      ViewSimple v = aladin.view.getCurrentView();
      double t[] = v.getTimeRange();
      boolean rep = (Double.isNaN(t[0]) && Double.isNaN(oLastDrawTimeRange[0]) || t[0]==oLastDrawTimeRange[0])
                 && (Double.isNaN(t[1]) && Double.isNaN(oLastDrawTimeRange[1]) || t[1]==oLastDrawTimeRange[1]);
      return !rep;
   }
   
   private SMoc lastCurrentSpaceMoc = null;
   private TMoc lastCurrentTimeMoc = null;
   
   protected Moc getSpaceMocLow(ViewSimple v,int order,int gapOrder) {
      STMoc m1 = (STMoc) getSpaceMocLow1(v,order,gapOrder);
      SMoc m = getCurrentSpaceMoc(v, m1);
      try { m.setMinOrder(3); } catch( Exception e ) { }
      return m;
   }

   protected void initArrayMoc(int order) {
      if( arrayMoc==null ) arrayMoc = new Moc[SMoc.MAXORD_S+1];
      arrayMoc[order] = new STMoc();   // pour éviter de lancer plusieurs threads sur le meme calcul
   }
   
   protected SMoc getSpaceMoc() {
      ViewSimple v = aladin.view.getCurrentView();
      if( lastCurrentSpaceMoc!=null && !isTimeModified() ) return lastCurrentSpaceMoc;
      SMoc m = getCurrentSpaceMoc( v );
      lastCurrentSpaceMoc = m;
      memoNewTime();
      return lastCurrentSpaceMoc;
   }

   
   protected TMoc getTimeMoc() {
      if( lastCurrentTimeMoc!=null && !isSpaceModified() ) return lastCurrentTimeMoc;
      TMoc m = getCurrentTimeMoc();
      lastCurrentTimeMoc = m;
      memoNewSpace();
      return lastCurrentTimeMoc;
   }

   
   /**************************** Gestion des affichages Highligh et Select SMOC et TMOC courant *************************/
   
   // Création des plans SMOC et TMOC qui permettent l'affichage des éléments du STMOC à visualiser explicitement
   private void initST() {
      smocH = new PlanMocFromST(this,false);
      tmocH = new PlanTMocFromST(this,false);
      smocS = new PlanMocFromST(this,true);
      tmocS = new PlanTMocFromST(this,true);
   }
   
   // Demande de réaffichage
   private void repaintST() {
      smocH.oiz = tmocH.oiz = smocS.oiz = tmocS.oiz = -2;
      aladin.view.repaintAll();
   }
   
   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      if( v.isPlotTime() ) {
         drawInTimeView(g,v);
         if( aladin.view.isMultiView() ) {
            tmocS.drawInTimeView(g,v);
            tmocH.drawInTimeView(g,v);
         }
         
      } else {
         drawInSpaceView(g,v);
         if( aladin.view.isMultiView() ) {
            smocS.drawInSpaceView(g,v);
            smocH.drawInSpaceView(g,v);
         }
      }
   }
   
   // Retourne true si les deux mocs sont différents
   private boolean isDiff(Moc s1, Moc s2) { return s1==null && s2!=null  || s1!=null && !s1.equals(s2); }

   private RectangleD lastR = null;  // Dernière case temporelle sous la souris connue
   
   /** Appelé lors du déplacement (select=false) ou d'un clic (select=true) de la souris
    * dans la vue v temporelle, afin de sélectionner le SMOC et le TMOC extrait du STMOC pour la case
    * temporelle sous la souris.
    */
   public boolean inTimeView( ViewSimple v, MouseEvent e, boolean select ) {
      if( lastRectDrawn==null  ) return false;
      
      String info=null;
      
      int xview=e.getX();
      int yview=e.getY();
      
      boolean trouve = false;
      SMoc sh = (SMoc) smocH.moc;
      TMoc th = (TMoc) tmocH.moc;
      SMoc ss = (SMoc) smocS.moc;
      TMoc ts = (TMoc) tmocS.moc;
      
      for( MyRect r : lastRectDrawn ) {
         
         // On prend éventuellement un peu de largeur pour que la souris puisse tomber "dedans"
         MyRect r1 = r;
         if( r.width<2 ) r1 = new MyRect(r.x-1,r.y,3.,r.height);
         
         if( r1.contains(xview, yview) ) {
            if( select && lastR==r ) return true;
            lastR=r;
                  
            try {
               th = new TMoc();
               th.add(TMoc.MAXORD_T,r.start,r.end-1L);
               STMoc m = (STMoc) moc;

               // Si trop lourd, on prend en altervative le MocLow précalculé
               if( !select && m.getNbCells()>10000 && r.order<=30 ) {
                  STMoc m1 = (STMoc) getLastLowMoc();
                  if( m1!=null)  m = m1;
               }

               sh = m.getSpaceMoc(r.start, r.end-1L);
               trouve=true;
               //  System.out.println("inTimeView: tmoc="+th.seeRangeList()+" smoc="+sh.toDebug());

               // Je clique sur une zone non sélectionné => je change la sélection
               // sinon je change (ou j'étends) la sélection
               if( select ) {
                  if( ts!=null && th.isIntersecting(ts) ) { ss=null; ts=null; }
                  else if( ts!=null && e.isShiftDown() ) { ss=ss.union(sh); ts=ts.union(th); }
                  else if( ss==null || e.isControlDown() ) { ss = sh; ts = th; }
               }
               
               String start =  Astrodate.JDToDate(r.start/TMoc.DAYMICROSEC, true, true);
               int t1 = start.indexOf('T');
               String end =  Astrodate.JDToDate((r.end-1L)/TMoc.DAYMICROSEC, true, true);
               int t2 = end.indexOf('T');
             
               info = "\n \n* Start: "+start.substring(0,t1)+"\n   "+start.substring(t1+1)+"\n"
                     + "\n \n* End: "+end.substring(0,t2)+"\n   "+end.substring(t2+1)+"\n"
                     +"\n \n*  Res: "+Util.getTemps((r.end-r.start));
             
            } catch( Exception e1 ) { e1.printStackTrace(); }
            break;
         }
      }
      
      // En dehors de tout => aucune surbrillance
      if( !trouve ) { sh = null; th = null; }

      // Repaint nécessaire si l'un des Mocs a changé
      if( isDiff(sh,smocH.moc) || isDiff(sh,smocH.moc)
       || isDiff(ss,smocS.moc) || isDiff(ts,tmocS.moc) ) {
         repaintST();
         aladin.calque.select.setMessageInfo( info );
      }
      
      smocH.moc = sh;
      tmocH.moc = th;
      smocS.moc = ss;
      tmocS.moc = ts;
      
      return trouve;
   }
   
   private Hpix lastHpix=null; // Dernière case spatiale sous la souris connue
   
   /** Appelé lors du déplacement (select=false) ou d'un clic (select=true) de la souris
    * dans la vue v spatiale , afin de sélectionner le SMOC et le TMOC extrait du STMOC pour la case
    * spatiale sous la souris.
    */
   public boolean inSpaceView( ViewSimple v, MouseEvent e, boolean select  ) {
      if( arrayHpix==null  ) return false;
      
      int xview = e.getX();
      int yview = e.getY();
      
      boolean trouve = false;
      SMoc sh = (SMoc) smocH.moc;
      TMoc th = (TMoc) tmocH.moc;
      SMoc ss = (SMoc) smocS.moc;
      TMoc ts = (TMoc) tmocS.moc;
      
      for( Hpix r : arrayHpix ) {
         if( r.contains(v,xview, yview) ) {
            if( !select && lastHpix==r ) return true;
            lastHpix=r;
            try {
               sh = new SMoc();
               sh.add(r.order,r.start);
               th = ((STMoc)moc).getTimeMoc( sh );
               trouve=true;
//               System.out.println("inSpaceView: "+r.order+"/"+r.start+" tmoc="+th.seeRangeList());

               // Je clique sur une zone non sélectionné => je change la sélection
               // sinon je change (ou j'étends) la sélection
               if( select ) {
                  if( ss!=null && sh.isIntersecting(ss) ) { ss=null; ts=null; }
                  else if( ss!=null && e.isShiftDown() ) { ss=ss.union(sh); ts=ts.union(th); }
                  else if( ss==null || e.isControlDown() ) { ss = sh; ts = th; }
               }
            } catch( Exception e1 ) { e1.printStackTrace(); }
            break;
         }
      }
      
      // En dehors de tout => aucune surbrillance
      if( !trouve ) { sh = null; th = null; }
      
      // Repaint nécessaire si l'un des Mocs a changé
      if( isDiff(sh,smocH.moc) || isDiff(sh,smocH.moc) 
       || isDiff(ss,smocS.moc) || isDiff(ts,tmocS.moc) ) repaintST();

      smocH.moc = sh;
      tmocH.moc = th;
      smocS.moc = ss;
      tmocS.moc = ts;

      return trouve;
   }

   protected void planReadyPost() {
      aladin.view.createView4TMOC(this);
   }


}

