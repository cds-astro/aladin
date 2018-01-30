// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;

import cds.moc.Array;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Hpix;
import healpix.essentials.HealpixBase;
import healpix.essentials.Pointing;
import healpix.essentials.Vec3;

/**
 * Génération d'un plan MOC à partir d'un flux
 * Attention : cette classe hérite de PlanBGCat, ce qui fait qu'un certain nombre de méthodes
 * doivent être surchargées "à vide".
 *
 * @author Pierre Fernique [CDS]
 * @version 2.1 jul 2016 - Moc avec périmètre
 * @version 2.0 nov 2012 - remise en forme
 */
public class PlanMoc extends PlanBGCat {

   // Mode de tracé
   static final public int DRAW_BORDER   = 0x1;      // Tracé des bordures des cellules Healpix
   static final public int DRAW_FILLIN   = 0x4;      // Remplissage avec aplat de demi-opacité
   static final public int DRAW_PERIMETER     = 0x8; // Tracé du périmètres

   protected HealpixMoc moc = null;                 // Le MOC
   private int wireFrame=DRAW_BORDER | DRAW_FILLIN; // Mode de tracage par défaut

   private HealpixMoc [] arrayMoc =null;          // Le MOC à tous les ordres */
   private ArrayList<Hpix> arrayHpix = null;      // Liste des cellules correspondant aux cellules tracés (order courant)
   private ArrayList<Hpix> arrayPeri = null;      // Liste des cellules correspondant au périmètre tracé (ordre courant)
   
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
      arrayMoc = new HealpixMoc[CDSHealpix.MAXORDER+1];
      this.dis   = in;
      this.moc   = moc;
      useCache = false;
      frameOrigin = Localisation.ICRS;
      if( moc!=null ) {
         String f = moc.getCoordSys();
         frameOrigin = f.equals("E")?Localisation.ECLIPTIC :
            f.equals("G")?Localisation.GAL:Localisation.ICRS;
         
         // Si le MOC est petit, affichage immédiat à la résolution max
         if( moc.getSize()<10000 ) setMaxGapOrder();
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
      pm.arrayHpix = arrayPeri = null;
      pm.arrayMoc = new HealpixMoc[CDSHealpix.MAXORDER+1];
   }
   
   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      double cov = moc.getCoverage();
      double degrad = Math.toDegrees(1.0);
      double skyArea = 4.*Math.PI*degrad*degrad;
      ADD( buf, "\n* Space: ",Coord.getUnit(skyArea*cov, false, true)+"^2, "+Util.round(cov*100, 3)+"% of sky");
      ADD( buf, "\n* Best ang.res: ",Coord.getUnit(moc.getAngularRes()));
      
      int order = getRealMaxOrder(moc);
      int drawOrder = getDrawOrder();
      ADD( buf,"\n","* MOC order: "+ (order==drawOrder ? order+"" : drawOrder+"/"+order));
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
   
   /** Retourne le Moc.maxOrder réel, même pour les vieux MOCs dont le Norder est généralement
    * faux */
   static private int getRealMaxOrder(HealpixMoc m) {
      int nOrder = m.getMaxOrder();
      if( nOrder<=0 ) return nOrder;
      Array a;
      while( ( (a=m.getArray(nOrder))==null || a.getSize()==0) && nOrder>0 ) nOrder--;
      return nOrder;
   }
   
   // POUR LE MOMENT ON N'UTILISE PAS ENCORE CETTE FONCTION
   // Création du Pcat qui contient les lignes du périmètre du MOC
//   private Pcat createPerimetre(ViewSimple v,HealpixMoc moc) throws Exception {
//      Pcat pcat = new Pcat(this);
//      ArrayList<double[]> a = getPerimeter(moc);
//      Ligne oo=null;
//
//      for( int i=a.size()-1; i>=0; i-- ) {
//         double [] c = a.get(i);
//
////         for( double [] c : a ) {
//            if( c==null ) { oo=null; continue; }
//         LigneConst o = new LigneConst(c[0], c[1], this, v, oo);
//                  o.id = pcat.nb_o+"";
//                  o.setWithLabel(true);
//         pcat.setObjetFast(o);
//         oo=o;
//         }
//         return pcat;
//      }
//   
 
   /**
    * Crée une chaine donnant la liste des coordonnées du perimetre
    * @param v
    * @param moc
    * @return
    * @throws Exception
    */
   static public String createPerimeterString(HealpixMoc moc) throws Exception {
      StringBuilder res = null;
      ArrayList<double[]> a = getPerimeter(moc);

      for( int i=a.size()-1; i>=0; i-- ) {
         double [] c = a.get(i);
         if( c==null ) continue;
         if( res==null ) res = new StringBuilder(c[0]+","+c[1]);
         res.append(","+c[0]+","+c[1]);
      }
      return res.toString();
   }

   
   /**
    * Retourne la liste des coordonnées du périmètres
    * @param moc
    * @return
    * @throws Exception
    */
   static public ArrayList<double[]> getPerimeter(HealpixMoc moc) throws Exception {
      if( moc==null ) return null;
      int maxOrder = getRealMaxOrder(moc);
      ArrayList<double[]> a = new ArrayList<double[]>();
      if( maxOrder==-1 || moc.isAllSky() ) return a;
      HealpixMoc done = new HealpixMoc( moc.getCoordSys(), moc.getMinLimitOrder(),maxOrder );
      HealpixBase hpx= CDSHealpix.getHealpixBase(maxOrder);

      Iterator<Long> it = moc.pixelIterator();
      while( it.hasNext() ) {
         long pix = it.next();
         parcoursBord(hpx,moc,done,a,maxOrder,pix,0,0);
         if( a.size()>0 && a.get(a.size()-1)!=null ) a.add(null);
      }

      return a;
   }
   
   static private void parcoursBord(HealpixBase hpx, HealpixMoc moc, HealpixMoc done, ArrayList<double[]> a, 
         int maxOrder, long pix, int sens, int rec ) throws Exception {
      if( done.isIntersecting(maxOrder,pix) ) return;
      
      if( rec>10000 ) return;
      
      done.add(maxOrder,pix);
      
      long [] voisins = getVoisins(hpx,moc,maxOrder,pix);
      double [][] corners = null;
      for( int j=0; j<4; j++ ) {
         int i = (sens+j)%4;
         long voisin = voisins[i];
         if( voisin!=-1 ) continue;
         if( corners==null ) corners = getCorners(hpx,pix);
         
         boolean flagAdd=true;
         if( flagAdd && a.size()>0 ) {
            double [] lastCorner = a.get( a.size()-1 );
            flagAdd = lastCorner==null || lastCorner[0]!=corners[i][0] || lastCorner[1]!=corners[i][1];
         }
         
         if( flagAdd ) a.add( corners[i]);
         a.add( corners[ i<3?i+1:0] );
         
         long nextVoisin = voisins[ i<3 ? i+1 : 0 ];
         if( nextVoisin!=-1 ) {
            
            long [] vVoisins = getVoisins(hpx,moc,maxOrder,nextVoisin);
            long nNextVoisin = vVoisins[i];
            if( nNextVoisin!=-1 ) {
               parcoursBord(hpx,moc,done,a,maxOrder,nNextVoisin,i==0?3:i-1,rec+1);
            }
            else parcoursBord(hpx,moc,done,a,maxOrder,nextVoisin,i,rec+1);
            if( a.size()>0 && a.get(a.size()-1)!=null ) a.add(null);
         }
      }
   }
   
   static final private int [] A = { 2, 1, 0, 3 };

   // Retourne les coordonnées des 4 coins du pixel HEALPix indiqué
   // Ordre des coins => S, W, N, E
   static private double [][] getCorners(HealpixBase hpx,long pix) throws Exception {
      Vec3[] tvec = hpx.boundaries(pix,1);   // N W S E
      double [][] corners = new double[tvec.length][2];
      for (int i=0; i<tvec.length; ++i) {
         Pointing pt = new Pointing(tvec[i]);
         int j=A[i];
         corners[j][0] = ra(pt);
         corners[j][1] = dec(pt);
      }
      return corners;
   }
   
   public static final double cPr = Math.PI / 180;
   static private double dec(Pointing ptg) { return (Math.PI*0.5 - ptg.theta) / cPr; }
   static private double ra(Pointing ptg) { return ptg.phi / cPr; }

   // Retourne la liste des numéros HEALPix des 4 voisins directs ou -1 s'ils sont en dehors du MOC   
   // Ordre des voisins => W, N, E, S
   static private long [] getVoisins(HealpixBase hpx, HealpixMoc moc, int maxOrder,long npix) throws Exception {
      long [] voisins = new long[4];
      long [] neib = hpx.neighbours(npix);
      for( int i=0,j=0; i<voisins.length; i++, j+=2 ) {
         voisins[i] = moc.isIntersecting(maxOrder, neib[j]) ? neib[j] : -1;
      }
      return voisins;
   }
   
   // Retourne la liste des numéros HEALPix des 4 voisins directs ou -1 s'il n'y en a pas du même ordre   
   // Ordre des voisins => W, N, E, S
   private long [] getVoisinsSameOrder(HealpixBase hpx, HealpixMoc moc, int maxOrder,long npix) throws Exception {
      long [] voisins = new long[4];
      long [] neib = hpx.neighbours(npix);
      for( int i=0,j=0; i<voisins.length; i++, j+=2 ) {
         voisins[i] = moc.isIn(maxOrder, neib[j]) ? neib[j] : -1;
      }
      return voisins;
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
   public boolean isDrawingPerimeter()   { return (wireFrame & DRAW_PERIMETER) !=0; }

   public void setDrawingBorder(boolean flag) {
      if( flag ) wireFrame |= DRAW_BORDER;
      else wireFrame &= ~DRAW_BORDER;
   }

   public void setDrawingFillIn(boolean flag) {
      if( flag ) wireFrame |= DRAW_FILLIN;
      else wireFrame &= ~DRAW_FILLIN;
   }

   public void setDrawingPerimeter(boolean flag) {
      if( flag ) wireFrame |= DRAW_PERIMETER;
      else wireFrame &= ~DRAW_PERIMETER;
   }
   
   /** Retourne une chaine décrivant les propriétés d'affichage courantes */
   public String getPropDrawingMethod() {
      StringBuilder s = new StringBuilder();
      if( isDrawingPerimeter() ) {
         if( s.length()>0 ) s.append(',');
         s.append("perimeter");
      }
      if( isDrawingBorder() ) {
         if( s.length()>0 ) s.append(',');
         s.append("border");
      }
      if( isDrawingFillIn() ) {
         if( s.length()>0 ) s.append(',');
         s.append("fill");
      }
      return s.toString();
   }
   
   /** Retourne une chaine décrivant le pourcentage de couverture */
   public String getPropCoverage() { return Util.myRound(moc.getCoverage()); }

   /** Retourne une chaine décrivant le MOC order */
   public String getPropMocOrder() { return getRealMaxOrder(moc)+""; }

   protected boolean isCatalog() { return false; }
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
            if( moc.getMocOrder()<3 ) moc.setMocOrder(3);   
            moc.setMinLimitOrder(3);
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            error="MOC error";
            return false;
         }
      }
      
      // Je rectifie une éventuelle erreur de déclaration du maxOrder
      int maxOrder = getRealMaxOrder(moc);
      int o = moc.getMaxOrder();
      if( maxOrder!=o ) {
         try {
            moc.setMocOrder(maxOrder);
            aladin.console.printError("Moc order probably wrong ("+o+"), assuming "+maxOrder);
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
      m.setCheckConsistencyFlag(false);
      m.setCoordSys(moc.getCoordSys());
      for( int i=0; i<pix.length; i++ ) m.add(order,pix[i]);
      m.setCheckConsistencyFlag(true);
      return m;
   }

   static int MAXGAPORDER=3;
   private int gapOrder=0;
   
   protected void setMaxGapOrder() { setGapOrder(MAXGAPORDER); }

   protected int getGapOrder() { return gapOrder; }
   protected void setGapOrder(int gapOrder) {
      if( Math.abs(gapOrder)>MAXGAPORDER ) return;
      this.gapOrder=gapOrder;
   }
   
   /** Modifie (si possible) une propriété du plan */
   protected void setPropertie(String prop,String specif,String value) throws Exception {
      int a=-1,b=-1,c=-1;
      if( prop.equalsIgnoreCase("drawing") ) {
         Tok tok = new Tok(value,", ");
         while( tok.hasMoreTokens() ) {
            String v = tok.nextToken();
            if( (a=Util.indexOfIgnoreCase(v,"perimeter"))>=0 ) setDrawingPerimeter(a>0 && v.charAt(a-1)=='-'? false : true);
            if( (b=Util.indexOfIgnoreCase(v,"fill"))>=0 )  {
               setDrawingFillIn   (b>0 && v.charAt(b-1)=='-'? false : true);
               b=v.indexOf(":",b);
               if( b>0 ) {
                  try {
                     float f = Float.parseFloat(v.substring(b+1));
                     if( f<0.1f || f>1f ) throw new Exception();
                     factorOpacity = f;
                  } catch( Exception e ) { throw new Exception("set drawing parameter unknown ["+v+"]");
                  }
               }
            }
            if( (c=Util.indexOfIgnoreCase(v,"border"))>=0 )    setDrawingBorder   (c>0 && v.charAt(c-1)=='-'? false : true);
            if( a<0 && b<0 && c<0 ) throw new Exception("set drawing parameter unknown ["+v+"]"); 
         }
         Properties.majProp(this);
      } else {
         super.setPropertie(prop,specif,value);
      }
   }
   
   // retourne/construit la liste du MOC
   // à l'ordre courant (mode progressif)
   private HealpixMoc getHealpixMocLow(int order,int gapOrder) {
      
      // On fournit le meilleur MOC dans le cas de la génération d'une image
      if( aladin.NOGUI ) return moc;

      int mo = moc.getMaxOrder();
      if( mo<3 ) mo=3;
      order += 5;
      if( order<7 ) order=7;
      order += gapOrder;
      if( order<5 ) order=5;
      if( order>mo ) order=mo;
      //      System.out.println("getHpixListProg("+o+") => "+order);
      if( arrayMoc[order]==null ) {
         arrayMoc[order] = new HealpixMoc();   // pour éviter de lancer plusieurs threads sur le meme calcul
         final int myOrder = order;
         final int myMo=mo;
         (new Thread("PlanMoc building order="+order){

            public void run() {
               Aladin.trace(4,"PlanMoc.getHealpixMocLow("+myOrder+") running...");
               HealpixMoc mocLow = myOrder==myMo ? moc : (HealpixMoc)moc.clone();
               try { mocLow.setMocOrder(myOrder); }
               catch( Exception e ) { e.printStackTrace(); }
               arrayMoc[myOrder]=mocLow;
               Aladin.trace(4,"PlanMoc.getHealpixMocLow("+myOrder+") done !");
               askForRepaint();
            }

         }).start();

      }
      // peut être y a-t-il déjà un MOC de plus basse résolution déjà prêt
      if( arrayMoc[order].getSize()==0 ) {
         isLoading=true;
         int i=order;
         for( ; i>=5 && (arrayMoc[i]==null || arrayMoc[i].getSize()==0); i--);
         if( i>=5 ) order=i;
      } else isLoading=false;

      lastOrderDrawn = order;
      return arrayMoc[order];
   }

   private boolean isLoading=false;
   protected boolean isLoading() { return isLoading; }

   private int lastOrderDrawn=-1;   // Dernier order tracé

   /** Retourne true si tout a été dessinée, sinon false */
   protected boolean hasMoreDetails() {
      return moc!=null && lastOrderDrawn < moc.getMaxOrder();
   }
   
   /** Retourne l'ordre du dernier MOC dessiné effectivement */
   protected int getDrawOrder() { return lastOrderDrawn; }

   protected double getCompletude() { return -1; }
   
   /** Retourne la liste des pixels HEALPix du périmètre d'un losange HEALPIX d'ordre plus petit */
   static class Bord implements Iterator<Long> {
      int order,bord,i;
      public Bord(int order) { this.order=order; i=0; bord=0;}
      public boolean hasNext() {
         if( order<2 && i>0 ) return false;
         return  bord<3 || bord==3 && i<order-1;
      }
      public Long next() {
         long res = bord==0 ? cds.tools.pixtools.Util.getHpxNestedNumber(0,i) : bord==1 
                            ? cds.tools.pixtools.Util.getHpxNestedNumber(i,order-1) : bord==2
                            ? cds.tools.pixtools.Util.getHpxNestedNumber(order-1,order-i-1) 
                            : cds.tools.pixtools.Util.getHpxNestedNumber(order-i-1,0);
         if( (++i)>=order ) { bord++; i=1; }
         return res;
      }
	@Override
	public void remove() {
		// TODO Auto-generated method stub
	}
   }
   
   private long oiz=-1;
   private boolean oFlagPeri;
   private int oGapOrder;
   
   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
           
      long t1 = Util.getTime();
      g.setColor(c);
      int max = Math.min(maxOrder(v),maxOrder)+1;
      
      try {
         HealpixMoc m = v.isAllSky() ? null : getViewMoc(v,max);
         
         long t=0;
         int myOrder = max+ (v.isAllSky()?0:1);
         
         t = System.currentTimeMillis();
         
         int drawingOrder = 0;
         HealpixBase hpx=null;
         HealpixMoc lowMoc = null;
         boolean flagPeri = isDrawingPerimeter();
         boolean flagBorder = isDrawingBorder();
         boolean flagFill = isDrawingFillIn();

         int gapOrder = this.gapOrder;
         if( mustDrawFast() ) {
            gapOrder--;
         }

         // Génération des Hpix concernées par le champ de vue
         if( oiz!=v.getIZ() || flagPeri!=oFlagPeri || gapOrder!=oGapOrder ) {
            lowMoc = getHealpixMocLow(myOrder,gapOrder);
            drawingOrder = getRealMaxOrder(lowMoc);
            if( drawingOrder==-1 ) return;
            //            System.out.println("Récupération Hpix order "+drawingOrder);
            hpx= CDSHealpix.getHealpixBase(drawingOrder);
            ArrayList<Hpix> a1 = new ArrayList<Hpix>(10000);
            ArrayList<Hpix> a2 = !flagPeri ? null : new ArrayList<Hpix>(10000);
            Iterator<MocCell> it = lowMoc.iterator();
            while( it.hasNext() ) {
               MocCell c = it.next();
               if( m!=null && !m.isIntersecting(c.order, c.npix)) continue;
               Hpix p = new Hpix(c.order, c.npix, frameOrigin);
               if( p.isOutView(v) ) continue;
               a1.add(p);

               if( flagPeri )  {
                  long [] vo = getVoisinsSameOrder(CDSHealpix.getHealpixBase(p.order), lowMoc, p.order, p.npix);
                  if( vo[0]!=-1 && vo[1]!=-1 && vo[2]!=-1 && vo[3]!=-1 ) continue;

                  //                  long deb = p.npix << 2*(drawingOrder-p.order);
                  //                  long fin = (p.npix+1) << 2*(drawingOrder-p.order);
                  //                  for( long pix=deb; pix<fin; pix++ ) {

                  long base = p.npix << 2*(drawingOrder-p.order);
                  Bord bord = new Bord((int)CDSHealpix.pow2(drawingOrder-p.order));
                  while( bord.hasNext() ) {
                     long b = bord.next();
                     long pix = base | b;

                     vo = getVoisins(hpx, lowMoc, drawingOrder, pix);
                     int mask = (vo[0]==-1?0x1:0) | (vo[1]==-1?0x2:0) | (vo[2]==-1?0x4:0) | (vo[3]==-1?0x8:0);
                     if( mask!=0 ) {
                        Hpix p1 = new Hpix(drawingOrder, pix, frameOrigin);
                        p1.setBorderMask(mask);
                        a2.add(p1);
                     }
                  }
               }

            }
            arrayHpix=a1;
            arrayPeri=a2;
            oiz=v.getIZ();
            oFlagPeri=flagPeri;
            oGapOrder=gapOrder;
         }
         
         // Tracé en aplat avec demi-niveau d'opacité
         if( flagFill && arrayHpix!=null && g instanceof Graphics2D ) {
            Graphics2D g2d = (Graphics2D)g;
            Composite saveComposite = g2d.getComposite();
            try {
               g2d.setComposite( Util.getImageComposite(getOpacityLevel()*getFactorOpacity()) );
               for( Hpix p : arrayHpix ) {
                  boolean small = isDrawingBorder() && p.getDiag2(v)<25;
                  if( !small ) p.fill(g, v);
               }
            } finally {
               g2d.setComposite(saveComposite);
            }
         }


         // Tracé des Hpix concernés par le champ de vue
         if( flagBorder && arrayHpix!=null ) {
            for( Hpix p : arrayHpix ) p.draw(g, v);
         }

         // Tracé des périmètres
         if( flagPeri && arrayPeri!=null ) {
            for( Hpix p : arrayPeri ) p.draw(g,v);
         }

//         t1 = System.currentTimeMillis();
//         System.out.println("draw " in "+(t1-t)+"ms"+(n>0 ? " => "+(double)n/(t1-t)+"/ms":"") );

         t = Util.getTime();
         statTimeDisplay = t-t1;

      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }
   
   protected float factorOpacity = 0.5f;
   protected float getFactorOpacity() { return factorOpacity; }
}

