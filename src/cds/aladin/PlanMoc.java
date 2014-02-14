// Copyright 2012 - UDS/CNRS
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

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.Iterator;

import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Hpix;

/**
 * Génération d'un plan MOC à partir d'un flux
 * Attention : cette classe hérite de PlanBGCat, ce qui fait qu'un certain nombre de méthodes
 * doivent être surchargées "à vide".
 * 
 * @author Pierre Fernique [CDS]
 * @version 2.0 nov 2012 - remise en forme
 */
public class PlanMoc extends PlanBGCat {
   
   // Mode de tracé
   static final public int DRAW_BORDER   = 0x1;     // Tracé des bordures des cellules Healpix
   static final public int DRAW_DIAGONAL = 0x2;     // Tracé des diagonales des cellules Healpix
   static final public int DRAW_FILLIN   = 0x4;     // Remplissage avec aplat de demi-opacité
   
   protected HealpixMoc moc = null;                 // Le MOC
   private int wireFrame=DRAW_BORDER | DRAW_FILLIN; // Mode de tracage par défaut
   private boolean twoResMode=true;                 // Indique que le tracé se fera en deux modes de résolution distincts suivant le facteur de zoom
   
   private Hpix [] hpixList = null;                 // Liste des cellules correspondant au MOC
   private Hpix [] hpixListLow = null;              // Idem mais à faible résolution

   public PlanMoc(Aladin a) { super(a); }
      
   /** Création d'un Plan MOC à partir d'un MOC pré-éxistant */
   protected PlanMoc(Aladin aladin, HealpixMoc moc, String label, Coord c, double radius) {
      this(aladin,null,moc,label,c,radius);
   }
   
   /** Création d'un Plan MOC à partir d'un flux */
   protected PlanMoc(Aladin aladin, MyInputStream in, String label, Coord c, double radius) {
      this(aladin,in,null,label,c,radius);
   }
   
   protected PlanMoc(Aladin aladin, MyInputStream in, HealpixMoc moc, String label, Coord c, double radius) {
      super(aladin);
      this.dis   = in;
      this.moc   = moc;
      useCache = false;
      frameOrigin = Localisation.ICRS;
      if( moc!=null ) {
         String f = moc.getCoordSys();
         frameOrigin = f.equals("E")?Localisation.ECLIPTIC : 
            f.equals("G")?Localisation.GAL:Localisation.ICRS;
      }
      type = ALLSKYMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="MOC";
      setLabel(label);
      co=c;
      coRadius=radius;
      aladin.trace(3,"AllSky creation: "+Plan.Tp[type]+(c!=null ? " around "+c:""));
      suite();
   }
   
   /** Recopie du Plan à l'identique dans p1 */
   protected void copy(Plan p1) {
      super.copy(p1);
      PlanMoc pm = (PlanMoc)p1;
      pm.frameOrigin=frameOrigin;
      pm.moc = (HealpixMoc)moc.clone();
      pm.wireFrame=wireFrame;
      pm.twoResMode=twoResMode;
   }
   
   /** Changement de référentiel si nécessaire */
   public HealpixMoc toReferenceFrame(String coordSys) throws Exception {
      if( coordSys.equals(moc.getCoordSys()) ) return moc;
      
      char a = moc.getCoordSys().charAt(0);
      char b = coordSys.charAt(0);
      int frameSrc = a=='G' ? Localisation.GAL : a=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;
      int frameDst = b=='G' ? Localisation.GAL : b=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;
      
      Healpix hpx = new Healpix();
      int order = moc.getMaxOrder();
      aladin.trace(2,"Moc reference frame conversion: "+a+" => "+b);
      HealpixMoc moc1 = new HealpixMoc(coordSys,moc.getMinLimitOrder(),moc.getMocOrder());
      moc1.setCheckConsistencyFlag(false);
      long onpix1=-1;
      Iterator<Long> it = moc.pixelIterator();
      while( it.hasNext() ) {
         long npix = it.next();
         for( int i=0; i<4; i++ ) {
            double [] coo = hpx.pix2ang(order+1, npix*4+i);
            Coord c = new Coord(coo[0],coo[1]);
            c = Localisation.frameToFrame(c, frameSrc, frameDst);
            long npix1 = hpx.ang2pix(order+1, c.al, c.del);
            if( npix1==onpix1 ) continue;
            onpix1=npix1;
            moc1.add(order,npix1/4);
         }

      }
      moc1.setCheckConsistencyFlag(true);
      return moc1;
   }
   
   /** Retourne le Moc */
   protected HealpixMoc getMoc() { return moc; }
   
   protected void suiteSpecific() { isOldPlan=false; }
   protected boolean isLoading() { return false; }
   protected boolean isSync() { return isReady(); }
   protected void reallocObjetCache() { }
   
   public void setWireFrame(int wireFrame) { this.wireFrame=wireFrame; }
   public void switchWireFrame(int mask) { wireFrame= wireFrame & ~mask; }
   public int getWireFrame() { return wireFrame; }
   
   public boolean isDrawingBorder() { return (wireFrame & DRAW_BORDER) !=0; }
   public boolean isDrawingDiagonal() { return (wireFrame & DRAW_DIAGONAL) !=0; }
   public boolean isDrawingFillIn() { return (wireFrame & DRAW_FILLIN) !=0; }
   
   public void setDrawingBorder(boolean flag) { 
      if( flag ) wireFrame |= DRAW_BORDER;
      else wireFrame &= ~DRAW_BORDER;
   }

   public void setDrawingDiagonal(boolean flag) { 
      if( flag ) wireFrame |= DRAW_DIAGONAL;
      else wireFrame &= ~DRAW_DIAGONAL;
   }
   
   public void setDrawingFillIn(boolean flag) { 
      if( flag ) wireFrame |= DRAW_FILLIN;
      else wireFrame &= ~DRAW_FILLIN;
   }
   
   public void setTwoResMode(boolean flag) { twoResMode=flag; }
   public boolean getTwoResMode() { return twoResMode; }
   
   protected boolean hasSources() { return false; }
   protected int getCounts() { return 0; }

   protected boolean waitForPlan() {
      if( dis==null ) return true;
      super.waitForPlan();
      try {
         if( moc==null && dis!=null ) {
            moc = new HealpixMoc();
            moc.setMinLimitOrder(3);
            if(  (dis.getType() & MyInputStream.FITS)!=0 ) moc.readFits(dis);
            else moc.readASCII(dis);
         }
         String c = moc.getCoordSys();
         frameOrigin = ( c==null || c.charAt(0)=='G' ) ? Localisation.GAL : Localisation.ICRS;
         
         // Centrage sur la première cellule
//         if( moc.getSize()>0 && frameOrigin==Localisation.ICRS ) {
//            MocCell cell = moc.iterator().next();
//            aladin.execAsyncCommand(cell+"");
//         }
      }
      catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      }
      return true;
   }
   
   // Pas d'itérator, car pas d'objets -on hérite de PlanBgCat
   protected Iterator<Obj> iterator() { return null; }
   protected void resetProj(int n) { }
   protected boolean isDrawn() { return true; }
   
   // Fournit le MOC qui couvre le champ de vue courant
   private HealpixMoc getViewMoc(ViewSimple v,int order) throws Exception {
      Coord center = getCooCentre(v);
      long [] pix = getPixList(v,center,order);

      HealpixMoc m = new HealpixMoc();
      m.setCoordSys(moc.getCoordSys());
      for( int i=0; i<pix.length; i++ ) m.add(order,pix[i]);
      m.sort();
      return m;
   }
   
   // Retourne/construit la liste des cellules "graphiques" correspondantes au MOC
   // en faible ou haute résolution suivant la valeur du flag "low".
   private Hpix [] getHpixList(ViewSimple v,boolean low) {
      if( hpixList==null ) {
         hpixList = new Hpix[moc.getSize()];
         int n=0;
         Iterator<MocCell> it = moc.iterator();
         while( it.hasNext() ) {
            MocCell h = it.next();
            hpixList[n++] = new Hpix(h.order,h.npix,frameOrigin);
         }
         
         int N=6;
         hpixListLow = hpixList;
//         if( true || hpixList.length>12*Math.pow(4,N-1) ) {
         HealpixMoc hpixLow = (HealpixMoc)moc.clone();
         try { hpixLow.setMaxLimitOrder(N); } 
         catch( Exception e ) { e.printStackTrace(); }
         hpixListLow = new Hpix[moc.getSize()];
         n=0;
         it = hpixLow.iterator();
         while( it.hasNext() ) {
            MocCell h = it.next();
            hpixListLow[n++] = new Hpix(h.order,h.npix,frameOrigin);
         }
//         }

      }
      return low ? hpixListLow : hpixList;
   }
   
   private boolean oDrawAll=false; // dernier état connu pour le voyant d'état de la pile
   private boolean drawAll=true;  // true si la totalité de ce qui doit être dessiné l'a été
   
   /** Retourne true si tout a été dessinée, sinon false */
   protected boolean hasMoreDetails() { oDrawAll = drawAll; return !drawAll; }
   
   protected double getCompletude() { return -1; }
   
   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      long t1 = Util.getTime();
      g.setColor(c);
      int max = Math.min(maxOrder(v),maxOrder)+1;
      double taille = v.getTaille();
      
      try {
         HealpixMoc m = v.isAllSky() ? null : getViewMoc(v,max);
         int order=0;
         long t=0;
         int i;
         boolean lowMoc = taille>30 && twoResMode;
         Hpix [] hpixList = getHpixList(v,lowMoc);
//      System.out.println("lowMoc="+lowMoc+" mustDrawFast="+mustDrawFast+" canDrawAll="+canDrawAll+" lastDrawAll="+delai);

         for( i=0; i<hpixList.length; i++ ) {
            Hpix p = hpixList[i];
            if( p==null ) break;
            order=p.getOrder();
            if( m!=null && !m.isIntersecting(order, p.getNpix())) continue;
            if( p.isOutView(v) ) continue;
            
            // Tracé en aplat avec demi-niveau d'opacité
            if( isDrawingFillIn() && !lowMoc )  {
               if( g instanceof Graphics2D ) {
                  Graphics2D g2d = (Graphics2D)g;
                  Composite saveComposite = g2d.getComposite();
                  try {
                     g2d.setComposite( Util.getImageComposite(getOpacityLevel()/5f) );
                     p.fill(g, v);
                  } finally {
                     g2d.setComposite(saveComposite);
                  }
               } else p.fill(g, v);
            }
            
            // Tracé des bords et|ou des diagonales
            if( isDrawingBorder() || isDrawingDiagonal() )  p.draw(g, v, isDrawingBorder(), isDrawingDiagonal() && !lowMoc );
         }
         drawAll = i==hpixList.length;
         

         t = Util.getTime();
         statTimeDisplay = t-t1;
         if( drawAll!=oDrawAll ) aladin.calque.select.repaint();  // pour faire évoluer le voyant d'état
         
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
}

