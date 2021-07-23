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

import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;

import cds.moc.Healpix;
import cds.moc.Moc;
import cds.moc.Moc1D;
import cds.moc.MocCell;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Hpix;

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

   protected Moc moc = null;        // Le Moc
   protected int wireFrame=DRAW_BORDER | DRAW_FILLIN; // Mode de tracage par défaut
           
   static int MAXGAPORDER=3;
   protected int gapOrder=0;

   protected Moc [] arrayMoc =null;        // Le MOC à tous les ordres */
   protected ArrayList<Hpix> arrayHpix = null;    // Liste des cellules correspondant aux cellules tracés (order courant)
   private ArrayList<Hpix> arrayPeri = null;      // Liste des cellules correspondant au périmètre tracé (ordre courant)
   
   public PlanMoc(Aladin a) { super(a); type = ALLSKYMOC; }
   
   protected int getTimeStackIndex() { return 0; }

   /** Création d'un Plan MOC à partir d'un MOC pré-éxistant */
   protected PlanMoc(Aladin aladin, SMoc moc, String label, Coord c, double radius,String url) {
      this(aladin,null,moc,label,c,radius,url);
   }

   /** Création d'un Plan MOC à partir d'un flux */
   protected PlanMoc(Aladin aladin, MyInputStream in, String label, Coord c, double radius,String url) {
      this(aladin,in,null,label,c,radius,url);
   }
   
   protected PlanMoc(Aladin aladin, MyInputStream in, SMoc moc, String label, Coord c, double radius,String url) {
      super(aladin);
      this.dis   = in;
      this.moc   = moc;
      this.url = url;
      useCache = false;
      frameOrigin = Localisation.ICRS;
      if( moc!=null ) {
         String f = moc.getSys();
         frameOrigin = f.equals("E")?Localisation.ECLIPTIC :
            f.equals("G")?Localisation.GAL:Localisation.ICRS;
         
         // Si le MOC est petit, affichage immédiat à la résolution max
         if( moc.getNbCells()<10000 ) setMaxGapOrder();
      }
      type = ALLSKYMOC;
      this.c = Couleur.getNextDefault(aladin.calque);
      setOpacityLevel(1.0f);
      if( label==null ) label="MOC";
      setLabel(label);
      co=c;
      coRadius=radius;
      aladin.trace(3,"MOC creation: "+Plan.Tp[type]+(c!=null ? " around "+c:""));
      suite();
   }

   /** Recopie du Plan à l'identique dans p1 */
   protected void copy(Plan p1) {
      super.copy(p1);
      PlanMoc pm = (PlanMoc)p1;
      pm.frameOrigin=frameOrigin;
      try {
         pm.moc = moc!=null ? moc.clone() : null;
      } catch( CloneNotSupportedException e ) {
         e.printStackTrace();
      }
      pm.wireFrame=wireFrame;
      pm.gapOrder=gapOrder;
      pm.arrayHpix = arrayPeri = null;
//      pm.arrayMoc = new Moc[CDSHealpix.MAXORDER+1];
   }
   
   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      SMoc m = (SMoc)moc;
      double cov = m.getCoverage();
      ADD( buf, "\n* Space: ",Coord.getUnit(Healpix.SKYAREA*cov, false, true)+"^2, "+Util.round(cov*100, 3)+"% of sky");
      ADD( buf, "\n* Resolution: ",Coord.getUnit(m.getAngularRes()));
      
      int order = m.getMocOrder();
      int drawOrder = getDrawOrder();
      ADD( buf,"\n","* Order: "+ (drawOrder==-1 ? order : order==drawOrder ? order+"" : "draw:"+drawOrder+"/"+order) );
      ADD( buf,"\n \nRAM: ",Util.getUnitDisk( moc.getMem() ) );
   }

   /** Changement de référentiel si nécessaire */
   public SMoc toReferenceFrame(String coordSys) throws Exception {
      SMoc m = (SMoc)moc;
      SMoc moc1 = Util.convertTo(m,coordSys);
      if( m!=moc1 ) {
         aladin.trace(2,"Moc reference frame conversion: "+m.getSys()+" => "+moc1.getSys());
      }
      return moc1;
   }

//   /** Changement de référentiel si nécessaire */
//   static public SMoc convertTo(SMoc moc, String coordSys) throws Exception {
//      if( coordSys.equals( moc.getCoordSys()) ) return moc;
//
//      char a = moc.getCoordSys().charAt(0);
//      char b = coordSys.charAt(0);
//      int frameSrc = a=='G' ? Localisation.GAL : a=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;
//      int frameDst = b=='G' ? Localisation.GAL : b=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;
//
//      Healpix hpx = new Healpix();
//      int order = moc.getMaxOrder();
//      SMoc moc1 = new SMoc(coordSys,moc.getMinLimitOrder(),moc.getMocOrder());
//      moc1.setCheckConsistencyFlag(false);
//      long onpix1=-1;
//      Iterator<Long> it = moc.pixelIterator();
//      while( it.hasNext() ) {
//         long npix = it.next();
//         for( int i=0; i<4; i++ ) {
//            double [] coo = hpx.pix2ang(order+1, npix*4+i);
//            Coord c = new Coord(coo[0],coo[1]);
//            c = Localisation.frameToFrame(c, frameSrc, frameDst);
//            long npix1 = hpx.ang2pix(order+1, c.al, c.del);
//            if( npix1==onpix1 ) continue;
//            onpix1=npix1;
//            moc1.add(order,npix1/4);
//         }
//
//      }
//      moc1.setCheckConsistencyFlag(true);
//      return moc1;
//   }
   
   /** Retourne le Moc.maxOrder réel, même pour les vieux MOCs dont le Norder est généralement
    * faux */
//   static protected int getRealMaxOrder(SMoc m) { return Math.max(m.getMocOrder(),m.getDeepestOrder()); }
   static protected int getRealMaxOrder(SMoc m) { return m.getMocOrder(); }
   
   // POUR LE MOMENT ON N'UTILISE PAS ENCORE CETTE FONCTION
   // Création du Pcat qui contient les lignes du périmètre du MOC
//   private Pcat createPerimetre(ViewSimple v,SMoc moc) throws Exception {
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
   static public String createPerimeterString(SMoc moc) throws Exception {
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
   static public ArrayList<double[]> getPerimeter(SMoc moc) throws Exception {
      if( moc==null ) return null;
      int maxOrder = getRealMaxOrder(moc);
      ArrayList<double[]> a = new ArrayList<>();
      if( maxOrder==-1 || moc.isFull() ) return a;
//      SMoc done = new SMoc( moc.getSys(), moc.getMinOrder(),maxOrder );
      SMoc done = moc.dup();
//      long nside = CDSHealpix.pow2( maxOrder );

      Iterator<Long> it = moc.valIterator();
      while( it.hasNext() ) {
         long pix = it.next();
         parcoursBord(maxOrder,moc,done,a,maxOrder,pix,0,0);
         if( a.size()>0 && a.get(a.size()-1)!=null ) a.add(null);
      }

      return a;
   }
   
   static private void parcoursBord(int order, SMoc moc, SMoc done, ArrayList<double[]> a, 
         int maxOrder, long pix, int sens, int rec ) throws Exception {
      if( done.isIntersecting(maxOrder,pix) ) return;
      
      if( rec>10000 ) return;
      
      done.add(maxOrder,pix);
      
      long [] voisins = getVoisins(order,moc,maxOrder,pix);
      double [][] corners = null;
      for( int j=0; j<4; j++ ) {
         int i = (sens+j)%4;
         long voisin = voisins[i];
         if( voisin!=-1 ) continue;
         if( corners==null ) corners = getCorners(order,pix);
         
         boolean flagAdd=true;
         if( flagAdd && a.size()>0 ) {
            double [] lastCorner = a.get( a.size()-1 );
            flagAdd = lastCorner==null || lastCorner[0]!=corners[i][0] || lastCorner[1]!=corners[i][1];
         }
         
         if( flagAdd ) a.add( corners[i]);
         a.add( corners[ i<3?i+1:0] );
         
         long nextVoisin = voisins[ i<3 ? i+1 : 0 ];
         if( nextVoisin!=-1 ) {
            
            long [] vVoisins = getVoisins(order,moc,maxOrder,nextVoisin);
            long nNextVoisin = vVoisins[i];
            if( nNextVoisin!=-1 ) {
               parcoursBord(order,moc,done,a,maxOrder,nNextVoisin,i==0?3:i-1,rec+1);
            }
            else parcoursBord(order,moc,done,a,maxOrder,nextVoisin,i,rec+1);
            if( a.size()>0 && a.get(a.size()-1)!=null ) a.add(null);
         }
      }
   }
   
   static final private int [] A = { 2, 1, 0, 3 };

   // Retourne les coordonnées des 4 coins du pixel HEALPix indiqué
   // Ordre des coins => S, W, N, E
   static private double [][] getCorners(int order,long pix) throws Exception {
      double [][] radec =  CDSHealpix.borders(order,pix,1);
      double [][] corners = new double[radec.length][2];
      for (int i=0; i<radec.length; ++i) {
         int j=A[i];
         corners[j] = CDSHealpix.polarToRadec( radec[i] );
      }
      return corners;
   }

//   static private double [][] getCorners(long nside,long pix) throws Exception {
//      Vec3[] tvec = hpx.boundaries(pix,1);   // N W S E
//      double [][] corners = new double[tvec.length][2];
//      for (int i=0; i<tvec.length; ++i) {
//         Pointing pt = new Pointing(tvec[i]);
//         int j=A[i];
//         corners[j][0] = ra(pt);
//         corners[j][1] = dec(pt);
//      }
//      return corners;
//   }
//   
//   public static final double cPr = Math.PI / 180;
//   static private double dec(Pointing ptg) { return (Math.PI*0.5 - ptg.theta) / cPr; }
//   static private double ra(Pointing ptg) { return ptg.phi / cPr; }

   // Retourne la liste des numéros HEALPix des 4 voisins directs ou -1 s'ils sont en dehors du MOC   
   // Ordre des voisins => W, N, E, S
   static private long [] getVoisins(int order, SMoc moc, int maxOrder,long npix) throws Exception {
      long [] voisins = new long[4];
      long [] neib = CDSHealpix.neighbours(order,npix);
      for( int i=0,j=0; i<voisins.length; i++, j+=2 ) {
         voisins[i] = moc.isIntersecting(maxOrder, neib[j]) ? neib[j] : -1;
      }
      return voisins;
   }
   
   // Retourne la liste des numéros HEALPix des 4 voisins directs ou -1 s'il n'y en a pas du même ordre   
   // Ordre des voisins => W, N, E, S
   private long [] getVoisinsSameOrder(int order, SMoc moc, int maxOrder,long npix) throws Exception {
      long [] voisins = new long[4];
      long [] neib = CDSHealpix.neighbours(order,npix);
      for( int i=0,j=0; i<voisins.length; i++, j+=2 ) {
//         voisins[i] = moc.isIn(maxOrder, neib[j]) ? neib[j] : -1;
         voisins[i] = moc.isIncluding(maxOrder, neib[j]) ? neib[j] : -1;
      }
      return voisins;
   }
      
   protected int getMocOrder() { return ((SMoc)moc).getMocOrder(); }

   /** Retourne le Moc */
   protected Moc getMoc() { return moc; }

   protected void suiteSpecific() { isOldPlan=false; }
   protected boolean isSync() { return isReady() && !isLoading(); }
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
   public String getPropMocOrder() { return Math.max( ((SMoc)moc).getMocOrder(),getRealMaxOrder( (SMoc)moc))+""; }

   protected boolean isCatalog() { return false; }
   protected boolean isTime() { return false; }
   protected boolean hasSources() { return false; }
   protected boolean hasCatalogInfo() { return false; }
   protected int getCounts() { return 0; }
   
   // Pas de MOc à charger pour un plan Moc
   protected void planReadyMoc() {}
   

   // Lecture du MOC, avec son entête FITS si possible
   protected void readMoc(Moc moc, MyInputStream dis ) throws Exception {
      
      if(  (dis.getType() & MyInputStream.FITS)!=0 ) {
         // Lecture de l'entete Fits en se préparant pour reprendre le flux
         dis.mark(10000);
         headerFits = new FrameHeaderFits(this,dis);
         headerFits = new FrameHeaderFits(this,dis);   // On veut la deuximème HDU

         // puis reprise de la lecture complete
         dis.reset();
         moc.readFITS(dis);
         
      } 
      else if(  (dis.getType() & MyInputStream.JSON)!=0 ) moc.readASCII(dis);
      else moc.readASCII(dis);
   }

   protected boolean waitForPlan() {
      if( dis!=null ) {
         super.waitForPlan();
         try {
            if( moc==null && dis!=null ) {
               moc = new SMoc();
               readMoc(moc,dis);
            }
            String c = ((SMoc)moc).getSys();
            frameOrigin = ( c==null || c.charAt(0)=='G' ) ? Localisation.GAL : Localisation.ICRS;
            if( moc.isEmpty() ) error="Empty MOC";
         }
         catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            error="MOC error";
            return false;
         }
      }
      
      // Détermination ou correction du target
      if( !(/* flagNoTarget || */ moc==null || moc.isEmpty()) ) {
         
         // est-ce que le target tombe bien dans le moc ? sinon on va forcer le recalcul sur la première cellule du MOC
         if( co!=null ) {
            int order = ((SMoc)moc).getMocOrder();
            Coord c = Localisation.frameToFrame(co, Localisation.ICRS, frameOrigin);
            double[] radec = CDSHealpix.radecToPolar(new double[] { c.al, c.del });
            try {
               long npix = CDSHealpix.ang2pix_nest(order, radec[0], radec[1]);
               if( !((SMoc)moc).isIntersecting(order, npix) ) co=null;
            } catch( Exception e ) {}
         }

         // Mémorisation en tant que target de la position de la première cellule
         if( co==null || co.al==0 && co.del==0  ) {
            try {
               MocCell cell = moc.iterator().next();
               double res[] = CDSHealpix.pix2ang_nest(cell.order, cell.start);
               double[] radec = CDSHealpix.polarToRadec(new double[] { res[0], res[1] });
               co = Localisation.frameToFrame( new Coord(radec[0],radec[1]) , frameOrigin, Localisation.ICRS);
               aladin.trace(3,"MOC target (re)computed from the first MOC HEALPix cell => "+co);
            } catch( Exception e ) {
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
      }

      // Je force le MOC minOrder à 3 pour que l'affichage soit toujours propre
      if( moc!=null && ((SMoc)moc).getMinOrder()<3 ) {
         try {
            if( ((SMoc)moc).getMocOrder()<3 ) ((SMoc)moc).setMocOrder(3);   
            ((SMoc)moc).setMinOrder(3);
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            error="MOC error";
            return false;
         }
      }

      return true;
   }

   // Pas d'itérator, car pas d'objets -on hérite de PlanBgCat
   protected Iterator<Obj> iterator() { return null; }
   protected void resetProj(int n) { }
   protected boolean isDrawn() { return true; }

//   // Fournit le MOC qui couvre le champ de vue courant
//   protected Moc getViewMoc(ViewSimple v,int order) throws Exception {
//      return v.getMoc(order);
//   }
//   protected SMoc getViewMoc(ViewSimple v,int order) throws Exception {
//      Coord center = getCooCentre(v);
//      long [] pix = getPixList(v,center,order);
//      if( pix==null ) {
//         if( Aladin.levelTrace>=3 ) System.err.println("Bizarre PlanMoc.getViewMoc=null center="+center+" order="+order);
//         return null;
//      }
//
//      SMoc m = new SMoc();
//      m.setCheckConsistencyFlag(false);
//      m.setCoordSys(moc.getCoordSys());
//      for( int i=0; i<pix.length; i++ ) m.add(order,pix[i]);
//      m.setCheckConsistencyFlag(true);
//      return m;
//   }

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
   
   protected SMoc getSpaceMoc() { return (SMoc)moc; }
   
   protected  boolean isTimeModified() { return false; }
   
   
   protected Moc getSpaceMocLow(ViewSimple v,int order,int gapOrder) {
      SMoc m = (SMoc) getSpaceMocLow1(v,order,gapOrder);
      try { m.setMinOrder(3); } catch( Exception e ) { }
      return m;
   }
   
   /** Retourne l'order à utiliser pour l'affichage courant en fonction de la position
    * du slider "densité" */
   protected int getLowOrder(int order, int gapOrder) {
      int mo = -1;
      mo = moc.getSpaceOrder();
      if( mo==-1 ) {
         try { mo = getRealMaxOrder( (SMoc)moc); } catch( Exception e ) {  e.printStackTrace(); }
      }
      if( mo<3 ) mo=3;
      order += 5;
      if( order<7 ) order=7;
      order += gapOrder;
      if( order<5 ) order=5;
      if( order>mo ) order=mo;
      
      return order;
   }
   
   // retourne/construit la liste du MOC à l'ordre courant (mode progressif)
   protected Moc getSpaceMocLow1(ViewSimple v,int order,int gapOrder) {
      lastOrderDrawn = moc.getSpaceOrder();
      if( aladin.NOGUI ) return moc;
      
      int mo = moc.getDeepestOrder();
      if( mo<3 ) mo=3;
      order = getLowOrder(order,gapOrder);
      
      lastLowMoc=null;
      
      // Pour les petits champs, on travaille sans cache, en découpant le MOC à la bonne
      // résolution et à la bonne taille.
      if( v.getTaille()<20 ) {
         try {
            // On améliore un peu la résolution
//            if( order<moc.getSpaceOrder() ) order++;
            
            // Peut être y a-t-il dans le cache des MocLows la Moc complet à la bonne résolution ?
            // Sinon on prend le MOC d'origine
            Moc mocP = arrayMoc!=null && arrayMoc[order]!=null 
                  && !arrayMoc[order].isEmpty() ? arrayMoc[order] : moc;
                  
            Moc1D mv = v.getMoc();
            
            // On découpe la zone concernée
            Moc moclow = mocP.intersection( mv );
            
            // Ajustement du MocOrder si on a travaillé directement sur le MOC original
            if( order<moclow.getSpaceOrder() ) moclow.setSpaceOrder(order);
            return moclow;
            
         } catch( Exception e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }
      
      // Pour les grands champs on va précalculer les MOC à chaque résolution pour pouvoir
      /// fournir rapidement celui à la résolution la plus adéquate.
      if( arrayMoc==null || arrayMoc[order]==null ) {
         initArrayMoc(order);
         BuildLow t = new BuildLow(moc,order,mo);
         
         // Si petit, je ne threade pas
         if( moc.getNbCells()<100000 || (lastBuildingTime>=0 && lastBuildingTime<30) ) t.run();
         else t.start();
      }
      // peut être y a-t-il déjà un MOC de plus basse résolution déjà prêt
      if( arrayMoc[order].isEmpty() ) {
         isLoading=true;
         int i=order;
         for( ; i>=5 && (arrayMoc[i]==null || arrayMoc[i].isEmpty()); i--);
         if( i>=5 ) order=i;
      } else isLoading=false;

      lastOrderDrawn = order;
      lastLowMoc = arrayMoc[order];
      return arrayMoc[order];
   }
   
   protected void initArrayMoc(int order) {
      if( arrayMoc==null ) arrayMoc = new Moc[SMoc.MAXORD_S+1];
      arrayMoc[order] = new SMoc(order);   // pour éviter de lancer plusieurs threads sur le meme calcul
   }
   
   private long lastBuildingTime=-1;
   
   class BuildLow extends Thread {
      int myOrder,myMo;
      Moc moc;

      BuildLow(Moc moc,int myOrder,int myMo) {
         super("BuidLow order="+myOrder);
         setPriority( getPriority()-1 );
         this.moc = moc;
         this.myOrder= myOrder;
         this.myMo = myMo;
      }
      
      public void run() {
         long t0 = System.currentTimeMillis();
         Moc mocLow=null;
         if( myOrder==myMo && !(moc instanceof STMoc) ) mocLow = moc;
         else {
            // Si déjà calculé, on va dégrader le MOC à partir d'un autre à une meilleure résolution la plus proche
            for( int o=myOrder; o<myMo; o++ ) {
               if( arrayMoc[o]!=null && !arrayMoc[o].isEmpty()) {
                  try { mocLow = arrayMoc[o].clone(); } catch( CloneNotSupportedException e ) { e.printStackTrace(); }
                  break;
               }
            }
            if( mocLow==null ) {
               try { mocLow = moc.clone(); } catch( CloneNotSupportedException e ) { e.printStackTrace(); }
            }
         }
//         Moc mocLow = myOrder==myMo ? moc : moc.clone();
         
         // Réduction de la résolution
         try {
            mocLow.setSpaceOrder(myOrder);
            if( moc instanceof STMoc ) {
               while( mocLow.getTimeOrder()>9 && mocLow.getNbRanges()>10000 ) {
                  mocLow.setTimeOrder( mocLow.getTimeOrder()-1);
//                  System.out.println("****mocLow spaceOrder="+mocLow.getSpaceOrder()+" timeOrder="+mocLow.getTimeOrder()+" nbRange="+mocLow.getNbRanges()+" nbCells="+mocLow.getNbCells());
               }
            }
        } catch( Exception e ) { e.printStackTrace(); }
         
         arrayMoc[myOrder]=mocLow;
         lastBuildingTime = System.currentTimeMillis() - t0;
         Aladin.trace(4,"PlanMoc.getSpaceMocLow1("+myOrder+") done in "+lastBuildingTime+"ms");
         askForRepaint();
      }
   }

   protected boolean isLoading=false;
   protected boolean isLoading() { return isLoading; }

   protected int lastOrderDrawn=-1;   // Dernier order tracé

   /** Retourne true si tout a été dessinée, sinon false */
   protected boolean hasMoreDetails() {
      if( !isReady() ) return false;
      if( flagProcessing ) return false;
      return moc!=null && lastOrderDrawn < moc.getDeepestOrder();
   }
   
   /** Reset de la dernière valeur connue du lastOrderDrawn */
   private void resetLastOrderDrawn() { } //lastOrderDrawn=-1; }
   
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
   
   
   protected void memoNewTime() { }
   
   protected long oiz=-1;
   private boolean oFlagPeri;
   private int oGapOrder;
   
   // Tracé du MOC visible dans la vue
   protected void draw(Graphics g,ViewSimple v) {
      if( v.isPlot() ) return;
      drawInSpaceView(g,v);
   }
   
   private SMoc lastDrawMoc=null;
   protected SMoc getLastDrawMoc() { return lastDrawMoc; }
   
   private Moc lastLowMoc=null;
   protected Moc getLastLowMoc() { return lastLowMoc; }

   protected void drawInSpaceView(Graphics g, ViewSimple v) {
      if( moc==null ) return;
      if( v.isPlot() ) return;  // Il ne s'agit pas d'une View space
           
      long t1 = Util.getTime();
      g.setColor(c);
      
      try {
         int max = Math.min(maxOrder(v),maxOrder)+1;
         
         int myOrder = max+ (v.isAllSky()?0:1);
         
         resetLastOrderDrawn();
         
         int drawingOrder = 0;
         Moc lowMoc = null;
         boolean flagPeri   = isDrawingPerimeter();
         boolean flagBorder = isDrawingBorder();
         boolean flagFill   = isDrawingFillIn();

         int gapOrder = this.gapOrder;
         
         
         // En cas de ralentissement de l'affichage, on dégrade d'un cran la résolution
         if( mustDrawFast() ) gapOrder--;

         // Génération des Hpix concernées par le champ de vue
         if( oiz!=v.getIZ() || flagPeri!=oFlagPeri || gapOrder!=oGapOrder ) {
            lowMoc = getSpaceMocLow(v,myOrder,gapOrder);
            drawingOrder = getRealMaxOrder((SMoc)lowMoc);
            if( drawingOrder==-1 ) return;
//            System.out.println("Récupération Hpix drawingOrder="+drawingOrder+" myOrder="+myOrder+" gapOrder="+gapOrder
//                  +" lowMoc.getMocOrder="+((SMoc)lowMoc).getMocOrder());
            //            long drawingNside = CDSHealpix.pow2(drawingOrder);

            Moc x = v.getMoc();
            if( !(x instanceof SMoc) ) {
               if( aladin.levelTrace>=3 ) {
                  System.out.println("Bizarre v.getMoc() ne me retourne pas un SMOC => "+x.toDebug());
                  try {
                     throw new Exception ();
                  } catch( Exception e ) { e.printStackTrace(); }
               }
               return;
            }
            lastDrawMoc =(SMoc) x;
            
            String coordsys = moc instanceof SMoc ? ((SMoc)moc).getSys() : "C";
            boolean notEquatorial = moc!=null && coordsys!=null && !coordsys.equals("C");
            SMoc viewMoc = v.isAllSky() || notEquatorial ? null : lastDrawMoc ;
            ArrayList<Hpix> a1 = new ArrayList<>(10000);
            ArrayList<Hpix> a2 = !flagPeri ? null : new ArrayList<Hpix>(10000);
            
            
            Iterator<MocCell> it = lowMoc.iterator();
            while( it.hasNext() ) {
               MocCell c = it.next();
               if( viewMoc!=null && !viewMoc.isIntersecting(c.order, c.start)) continue;
               Hpix p = new Hpix(c.order, c.start, frameOrigin);
               if(  viewMoc==null && p.isOutView(v) ) continue;
               a1.add(p);
               
               if( flagPeri )  {
                  long [] vo = getVoisinsSameOrder(p.order, (SMoc)lowMoc, p.order, p.start);
                  if( vo[0]!=-1 && vo[1]!=-1 && vo[2]!=-1 && vo[3]!=-1 ) continue;
                  long base = p.start << 2*(drawingOrder-p.order);
                  Bord bord = new Bord((int)CDSHealpix.pow2(drawingOrder-p.order));
                  while( bord.hasNext() ) {
                     long b = bord.next();
                     long pix = base | b;

                     vo = getVoisins(drawingOrder, (SMoc)lowMoc, drawingOrder, pix);
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
                  boolean small = flagBorder && p.getDiag2(v)<25;
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

      } catch( Exception e ) {
         if( Aladin.levelTrace>=4 ) e.printStackTrace();
      }
   }
   
   protected float factorOpacity = 0.5f;
   protected float getFactorOpacity() { return factorOpacity; }
   
}

