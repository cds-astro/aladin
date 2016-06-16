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
   static final public int DRAW_FILLIN   = 0x4;     // Remplissage avec aplat de demi-opacité

   protected HealpixMoc moc = null;                 // Le MOC
   private int wireFrame=DRAW_BORDER | DRAW_FILLIN; // Mode de tracage par défaut

   private Hpix[][] arrayHpix = null;      // Liste des cellules correspondant au MOC (pour chaque ordre)
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
      arrayHpix = new Hpix[CDSHealpix.MAXORDER+1][];
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
      pm.gapOrder=gapOrder;
      pm.arrayHpix = new Hpix[CDSHealpix.MAXORDER+1][];
   }

   /** Changement de référentiel si nécessaire */
   public HealpixMoc toReferenceFrame(String coordSys) throws Exception {
      HealpixMoc moc1 = convertTo(moc,coordSys);
      if( moc!=moc1 ) {
         aladin.trace(2,"Moc reference frame conversion: "+moc.getCoordSys()+" => "+moc1.getCoordSys());
      }
      return moc1;
   }

   /** Changement de référentiel si nécessaire */
   static public HealpixMoc convertTo(HealpixMoc moc, String coordSys) throws Exception {
      if( coordSys.equals( moc.getCoordSys()) ) return moc;

      char a = moc.getCoordSys().charAt(0);
      char b = coordSys.charAt(0);
      int frameSrc = a=='G' ? Localisation.GAL : a=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;
      int frameDst = b=='G' ? Localisation.GAL : b=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;

      Healpix hpx = new Healpix();
      int order = moc.getMaxOrder();
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

   protected int getMocOrder() { return moc.getMocOrder(); }

   /** Retourne le Moc */
   protected HealpixMoc getMoc() { return moc; }

   protected void suiteSpecific() { isOldPlan=false; }
   protected boolean isSync() { return isReady(); }
   protected void reallocObjetCache() { }

   public void setWireFrame(int wireFrame) { this.wireFrame=wireFrame; }
   public void switchWireFrame(int mask) { wireFrame= wireFrame & ~mask; }
   public int getWireFrame() { return wireFrame; }

   public boolean isDrawingBorder() { return (wireFrame & DRAW_BORDER) !=0; }
   public boolean isDrawingFillIn() { return (wireFrame & DRAW_FILLIN) !=0; }

   public void setDrawingBorder(boolean flag) {
      if( flag ) wireFrame |= DRAW_BORDER;
      else wireFrame &= ~DRAW_BORDER;
   }

   public void setDrawingFillIn(boolean flag) {
      if( flag ) wireFrame |= DRAW_FILLIN;
      else wireFrame &= ~DRAW_FILLIN;
   }

   protected boolean hasSources() { return false; }
   protected int getCounts() { return 0; }

   protected boolean waitForPlan() {
      if( dis!=null ) {
         super.waitForPlan();
         try {
            if( moc==null && dis!=null ) {
               moc = new HealpixMoc();
               if(  (dis.getType() & MyInputStream.FITS)!=0 ) moc.readFits(dis);
               else moc.readASCII(dis);
            }
            String c = moc.getCoordSys();
            frameOrigin = ( c==null || c.charAt(0)=='G' ) ? Localisation.GAL : Localisation.ICRS;
            if( moc.getSize()==0 ) error="Empty MOC";
         }
         catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            return false;
         }

      }

      // Mémoristion de la position de la première cellule
      if( (co==null || flagNoTarget ) && moc.getSize()>0 && frameOrigin==Localisation.ICRS ) {
         try {
            MocCell cell = moc.iterator().next();
            double res[] = CDSHealpix.pix2ang_nest(CDSHealpix.pow2(cell.getOrder()), cell.getNpix());
            double[] radec = CDSHealpix.polarToRadec(new double[] { res[0], res[1] });
            co = new Coord(radec[0],radec[1]);
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }

      // Je force le MOC minOrder à 3 pour que l'affichage soit propre
      if( moc!=null && moc.getMinLimitOrder()<3 ) {
         try {
            moc.setMinLimitOrder(3);
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            return false;
         }
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


   static int MAXGAPORDER=3;
   private int gapOrder=0;

   protected int getGapOrder() { return gapOrder; }
   protected void setGapOrder(int gapOrder) {
      if( Math.abs(gapOrder)>MAXGAPORDER ) return;
      this.gapOrder=gapOrder;
   }

   // retourne/construit la liste des cellules "graphiques" correspondantes au MOC
   // à l'ordre courant (mode progressif)
   private Hpix [] getHpixListProg(int order) {
      //      int o = order;

      int mo = moc.getMaxOrder();
      if( mo<3 ) mo=3;
      order += 5;
      if( order<7 ) order=7;
      order += gapOrder;
      if( order<5 ) order=5;
      if( order>mo ) order=mo;
      //      System.out.println("getHpixListProg("+o+") => "+order);
      if( arrayHpix[order]==null ) {
         arrayHpix[order] = new Hpix[0];   // pour éviter de lancer plusieurs threads sur le meme calcul
         final int myOrder = order;
         final int myMo=mo;
         (new Thread("PlanMoc building order="+order){

            public void run() {
               Aladin.trace(4,"PlanMoc.getHpixListProg("+myOrder+") running...");
               HealpixMoc mocLow = myOrder==myMo ? moc : (HealpixMoc)moc.clone();
               try { mocLow.setMocOrder(myOrder); }
               catch( Exception e ) { e.printStackTrace(); }
               Hpix [] hpixLow = new Hpix[moc.getSize()];
               int n=0;
               Iterator<MocCell> it = mocLow.iterator();
               while( it.hasNext() ) {
                  MocCell h = it.next();
                  hpixLow[n++] = new Hpix(h.order,h.npix,frameOrigin);
               }
               arrayHpix[myOrder] = hpixLow;
               Aladin.trace(4,"PlanMoc.getHpixListProg("+myOrder+") done !");
               askForRepaint();
            }

         }).start();

      }
      // peut être y a-t-il déjà un MOC de plus basse résolution déjà prêt
      if( arrayHpix[order].length==0 ) {
         isLoading=true;
         int i=order;
         for( ; i>=5 && (arrayHpix[i]==null || arrayHpix[i].length==0); i--);
         if( i>=5 ) order=i;
      } else isLoading=false;

      lastOrderDrawn = order;
      return arrayHpix[order];
   }

   private boolean isLoading=false;
   protected boolean isLoading() { return isLoading; }

   private int lastOrderDrawn=-1;   // Dernier order tracé

   /** Retourne true si tout a été dessinée, sinon false */
   protected boolean hasMoreDetails() {
      return moc!=null && lastOrderDrawn < moc.getMaxOrder();
   }

   protected double getCompletude() { return -1; }

   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      long t1 = Util.getTime();
      g.setColor(c);
      int max = Math.min(maxOrder(v),maxOrder)+1;

      try {
         HealpixMoc m = v.isAllSky() ? null : getViewMoc(v,max);
         int order=0;
         long t=0;
         int i;
         Hpix [] hpixList = getHpixListProg(max+ (v.isAllSky()?0:1));

         for( i=0; i<hpixList.length; i++ ) {
            Hpix p = hpixList[i];
            if( p==null ) break;
            order=p.getOrder();
            if( m!=null && !m.isIntersecting(order, p.getNpix())) continue;
            if( p.isOutView(v) ) continue;

            boolean small = p.getDiag2(v)<25 && isDrawingBorder();

            // Tracé en aplat avec demi-niveau d'opacité
            if( isDrawingFillIn() /* && !lowMoc */ && !small )  {
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
            if( isDrawingBorder() )  p.draw(g, v, isDrawingBorder());
         }

         t = Util.getTime();
         statTimeDisplay = t-t1;

      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
}

