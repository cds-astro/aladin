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

package cds.moc;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.healpix.FlatHashList;
import cds.healpix.HealpixNested;
import cds.healpix.NeighbourList;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/** HEALPix Multi Order Coverage Map (MOC)
 * This object provides read, write and process methods to manipulate an HEALPix Multi Order Coverage Map (MOC)
 * A MOC is used to define a sky region by using HEALPix sky tesselation
 *
 * @authors Pierre Fernique [CDS], Martin Reinecke [Max Plank]
 * @version 5.1 Feb 2019 - TMOC implementation (temporal MOC)
 * @version 5.0 Sept 2017 - JSON and ASCII full support, add(order,long[]) + add(order,Collection<Lon>), Check npix over max limit bug fix
 * @version 4.8 July 2017 - isEmpty(), isIncluding(..) methods
 * @version 4.7 Dec 2016 - Undeprecated new HealpicMoc(Inputstream in, int mode) + isAscendant(int order, Array a) bug fix
 * @version 4.6 Apr 2016 - MocLint - IVOA 1.0 MOC recommendation compatibility checker
 * @version 4.5 Nov 2015 - JSON #MOCORDER patch
 * @version 4.4 Jun 2015 - Empty MOC FITS bug fix
 * @version 4.2 oct 2014 - setMinLimitOrder() bug fix
 * @version 4.1 nov 2013 - pixelIterator 4.0 bug fix
 * @version 4.0 sep 2013 - upgrade for MOC WD 1.0 1 sept 2013 compliance
 * @version 3.4 oct 2012 - operations by RangeSet
 * @version 3.3 July 2012 - PixelIterator() addition (low level pixel iterator)
 * @version 3.2 April 2012 - JSON ASCII support (the previous basic ASCII format is still supported)
 * @version 3.2 March 2012 - union, intersection,... improvements + refactoring isIntersecting(...)
 * @version 3.1 Dec 2011 - check()
 * @version 3.0 Dec 2011 - 1) Use HealpixInterface 2) replace unicityTest by testConsistency 3)code cleaning
 * @version 2.0 Oct 2011 - use of short, int and long, creation of MocIO class...
 * @version 1.3 Sept 2011 - Support for delete
 * @version 1.2 Sept 2011 - COORDSYS support
 * @version 1.1 Sept 2011 - used sorted MOC (speed improvement)
 * @version 1.0 June 2011 - first stable version
 * @version 0.9 May 2011 - creation
 */
public class SMoc extends Moc {
   
   static public final boolean RANGE = true;

   static public final int SHORT = 0;
   static public final int INT   = 1;
   static public final int LONG  = 2;
   
   // Max insertions before a automatic checkAndFix()
   static private final int MAXADDS = 500000;

   /** Provide the integer type for a given order */
   static public int getType(int order) { return order<6 ? SHORT : order<14 ? INT : LONG; }
   
   protected String sys;           // Coordinate system (HEALPix convention => G=galactic, C=Equatorial, E=Ecliptic)
   protected int minOrder;         // Min order supported (by default 0)
   protected int mocOrder;         // Max order supported (by default depending of Healpix library => typically 29)
   protected Array [] level;            // pixel list for each HEALPix orders
   private int nOrder;                  // The number of orders currently used
   private boolean testConsistency;     // true for checking the consistency during a MOC pixel addition (=> slower)
   private boolean isConsistant;        // true if we are sure that the MOC is consistant
   private int currentOrder=-1;         // last current order for pixel addition
   
   public Range range=null;

   /** HEALPix Multi Order Coverage Map (MOC) creation */
   public SMoc() {
      init("C",0,-1);
   }

   /** Moc Creation with a specified max limitOrder (by default 29) */
   public SMoc(int mocOrder) throws Exception {
      init("C",0,mocOrder);
   }

   /** Moc Creation with a specified min and max limitOrder (by default 0..29) */
   public SMoc(int minOrder,int mocOrder) throws Exception {
      init("C",minOrder,mocOrder);
   }

   /** Moc Creation with a specified min and max limitOrder (by default 0..29)
    * @deprecated Standard MOC must be equatorial
    */
   public SMoc(String sys, int minOrder,int mocOrder) throws Exception {
      init(sys,minOrder,mocOrder);
   }
   
   public SMoc( Moc moc ) throws Exception {
      if( moc instanceof STMoc )  moc = ((STMoc)moc).getSpaceMoc();
      init( moc.getSys(), 0, moc.getSpaceOrder());
      range = new Range( moc.getRange() );
      if( range!=null ) toMocSet();
   }
   
   public SMoc(Range r) throws Exception {
      this();
      range =r;
      toMocSet();
   }

   /** HEALPix Multi Order Coverage Map (MOC) creation and initialisation
    * via a string following ASCII or JSON MOC syntax
    * ex JSON:          { "order1":[npix1,npix2,...], "order2":[npix3...] }
    * ex basic ASCII:   order1/npix1-npix2 npix3 ... order2/npix4 ...
    * @param s list of MOC pixels
    */
   public SMoc(String s) throws Exception {
      this();
      add(s);
   }

   /** HEALPix Multi Order Coverage Map (MOC) creation and initialisation via a stream
    * @param in input stream
    */
   public SMoc(InputStream in) throws Exception {
      read(in);
   }

   /** HEALPix Multi Order Coverage Map (MOC) creation and initialisation
    * via a stream, either in JSON encoded format , ASCII encoded format or in FITS encoded format
    * @param in input stream
    * @param mode ASCII - ASCII encoded format, JSON encoded format, FITS - Fits encoded format
    */
   public SMoc(InputStream in, int mode) throws Exception {
      this();
      read(in,mode);
   }

   /** Clear the MOC */
   public void clear() {
      init(sys,minOrder,mocOrder);
   }

   /** Deep copy */
   public Moc clone() {
      SMoc moc = new SMoc();
      return clone1(moc);
   }

   /** Deep copy - internal method */
   protected Moc clone1( SMoc moc) {
      moc.sys=sys;
      moc.mocOrder=mocOrder;
      moc.minOrder=minOrder;
      moc.nOrder=nOrder;
      moc.testConsistency=testConsistency;
      moc.currentOrder=currentOrder;
      moc.range= (range==null) ? null : new Range(range);
      for( int order=0; order<nOrder; order++ ) {
         moc.level[order] = (Array)level[order].clone();
      }
      moc.property = (LinkedHashMap<String, String>)property.clone();
      moc.comment = (HashMap<String, String>)comment.clone();
      return moc;
   }

   /** Set the Min limit order supported by the Moc (by default 0)
    *  (and automatically switch on the testConsistency)
    * Any future addition of pixel with order smallest than minLimitOrder will be automatically replaced by the
    * addition of its 4 sons.
    */
   public void setMinOrder(int minOrder) throws Exception {
      if( minOrder==this.minOrder ) return;
      if( minOrder>MAXORDER ) throw new Exception("Min order exceed HEALPix library possibility ("+MAXORDER+")");
      if( minOrder<0 || mocOrder!=-1 && minOrder>mocOrder ) throw new Exception("Min limit greater than max limit order");
      isConsistant = false;
      this.minOrder=minOrder;
      setCheckConsistencyFlag(true);
   }
   
   public void setTimeOrder(int order) throws Exception { throw new Exception("No time dimension"); }
   public void setSpaceOrder(int order) throws Exception { setMocOrder(order); }

   /** Set the limit order supported by the Moc (-1 for Healpix library implementation)
    *  (and automatically switch on the testConsistency)
    * Any future addition of pixel with order exceeding limitOrder will be automatically replaced by the
    * addition of the corresponding pixel at the limitOrder. If there is no limitOrder set (-1), an exception
    * will thrown.
    */
   public void setMocOrder(int mocOrder) throws Exception {
      if( mocOrder==this.mocOrder ) return;
      if( mocOrder>MAXORDER ) throw new Exception("Moc order exceed HEALPix library possibility ("+MAXORDER+")");
      if( mocOrder!=-1 && mocOrder<minOrder ) throw new Exception("Max limit order smaller than min limit order");
      property.put("MOCORDER", ""+(mocOrder==-1 ? MAXORDER : mocOrder));
      this.mocOrder=mocOrder;
      isConsistant = false;
      if( getSize()>0 ) checkAndFix();
      if( mocOrder!=-1 ) nOrder=mocOrder+1;
   }

   
//   public void setMocOrderBis(int limitOrder) throws Exception {
//      if( limitOrder==maxLimitOrder ) return;
//      if( limitOrder>MAXORDER ) throw new Exception("Max limit order exceed HEALPix library possibility ("+MAXORDER+")");
//      if( limitOrder!=-1 && limitOrder<minLimitOrder ) throw new Exception("Max limit order smaller than min limit order");
//      property.put("MOCORDER", ""+(limitOrder==-1 ? MAXORDER : limitOrder));
//      maxLimitOrder=limitOrder;
//      spaceRange = spaceRange.degrade( (MAXORDER-maxLimitOrder)*2 );
//   }

   /** Provide the minimal limit order supported by the Moc (by default 0) */
   public int getMinOrder() { return minOrder; }

   /** Provide the limit order supported by the Moc (by default depends of the Healpix library implementation)
    * Si non défini, on prend la cellule la plus fine entrée en cours de saisie, ou à défaut en mémorisation
    */
   public int getMocOrder() {
      return mocOrder>=0 ? mocOrder : currentOrder>=0 ? currentOrder : nOrder-1;
   }

   /** Provide the greater order used in the MOC (may be less than MOC order) */
   public int getMaxUsedOrder() {
      return nOrder-1;
   }

   public int getSpaceOrder() { return getMocOrder(); }
   public int getTimeOrder()  { return -1; }  // No time dimension

   /** Set the MOC order. By default 29.
    * If the MOC already contains smaller cells, these cells will be replaced by
    * their first parent corresponding to the new MOC order
    * @param mocOrder MOC order (best cells order)
    */
   public void setMocOrder(String mocOrder) throws Exception {
      int n = Integer.parseInt(mocOrder);
      if( n==MAXORDER ) n=-1;
      setMocOrder(n);
   }

   /** Return the reference system (HEALPix convention: G-galactic, C-Equatorial, E-Ecliptic) */
   public String getSys() {
      return sys;
   }

   /** Specify the coordinate system (HEALPix convention: G-galactic, C-Equatorial, E-Ecliptic)
    * @deprecated Standard MOC must be equatorial
    */
   public void setSys(String sys) {
      this.sys=sys;
      initPropSys(sys);
   }

   // Use for parsing only
   protected void setCurrentOrder(int order ) { currentOrder=order; }

   /** Provide the number of Healpix pixels (for all MOC orders)
    * @return number of pixels
    */
   public int getSize() {
      int size=0;
      for( int order=0; order<nOrder; order++ ) size+= getSize(order);
      return size;
   }

   /** Return approximatively the memory used for this moc (in bytes) */
   public long getMem() {
      long mem=0L;
      for( int order=0; order<nOrder; order++ ) mem += level[order].getMem();
      return mem;
   }

   /** Provide the number of Healpix pixels for a dedicated order */
   public int getSize(int order) {
      return level[order].getSize();
   }

   /** Provide the Array of a dedicated order */
   public Array getArray(int order) {
      return level[order];
   }

   /** Provide the angular resolution (in degrees) of the MOC (sqrt of the smallest pixel area) */
   public double getAngularRes() {
      return Math.sqrt( getPixelArea( getMocOrder() ) );
   }

   /** Provide the greatest order really used by the MOC
    * WARNING: use getMocOrder() to know the MOC resolution
    * @return greatest MOC order, -1 if no order used
    */
//   public int getMaxOrder() { return nOrder-1; }

   /**
    * Set the check consistency flag.
    * "true" by default => redundancy check and hierarchy consistency check during addition (=> slower)
    * @param flag
    */
   public void setCheckConsistencyFlag(boolean flag) throws Exception {
      testConsistency=flag;
      if( testConsistency ) checkAndFix();
   }

   /** return the order of the first descendant, otherwise -1*/
   public int getDescendantOrder(int order,long npix) {
      long pix=npix/4L;
      for( int o=order-1; o>=0; o--,pix/=4L ) {
         if( level[o].find(pix)>=0 ) return o;
      }
      return -1;
   }

   /** Add a list of MOC pixels provided in a string format (JSON format or basic ASCII format)
    * ex JSON:          { "order1":[npix1,npix2,...], "order2":[npix3...] }
    * ex basic ASCII:   order1/npix1-npix2 npix3 ... order2/npix4 ...
    * Note : The string can be submitted in several times. In this case, the insertion will use the last current order
    * Note : in JSON, the syntax is not checked ( in fact {, [ and " are ignored)
    * @see setCheckUnicity(..)
    */
   public void add(String s) throws Exception {
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t{}");
      while( st.hasMoreTokens() ) {
         String s1 = st.nextToken();
         if( s1.length()==0 ) continue;
         addHpix(s1);
      }
   }

   /** Add directly a full Moc.
    * Note: The MOC consistency is checked and possibly fixed at the end of the insertion process
    * @param moc The Moc to be added
    */
   public void add(Moc m) throws Exception {
      SMoc moc = (SMoc)m;
      if( moc.getSize()>MAXADDS/2 ) setCheckConsistencyFlag(false);
      int nadds=0;
      for( int order=moc.nOrder-1; order>=0; order-- ) {
         for( long npix : moc.getArray(order) ) {
            if( nadds>MAXADDS ) { checkAndFix(); nadds=0; }
            add(order, npix);
            nadds++;
         }
      }
      setCheckConsistencyFlag(true);
   }
   
   /** Add a collection of npix, all of them at the same order.
    * Note: The MOC consistency is checked and possibly fixed at the end of the insertion process
    * @param order order of insertion
    * @param npixs list of npix to be inserted
    * @throws Exception
    */
   public void add(int order, long [] npixs) throws Exception {
      if( npixs.length>MAXADDS/2 ) setCheckConsistencyFlag(false);
      int nadds=0;
      for( long npix : npixs ) {
         if( nadds>MAXADDS ) { checkAndFix(); nadds=0; }
         add(order, npix);
         nadds++;
      }
      setCheckConsistencyFlag(true);
   }

   /** Add a collection of npix, all of them at the same order.
    * Note: The MOC consistency is checked and possibly fixed at the end of the insertion process
    * @param order order of insertion
    * @param npixs list of npix to be inserted
    * @throws Exception
    */
   public void add(int order, Collection<Long> a) throws Exception {
      if( a.size()>MAXADDS/2 ) setCheckConsistencyFlag(false);
      int nadds=0;
      for( long npix : a ) {
         if( nadds>MAXADDS ) { checkAndFix(); nadds=0; }
         add(order, npix);
         nadds++;
      }
      setCheckConsistencyFlag(true);
   }


   /** Add a Moc pixel
    * Recursive addition : since with have the 3 brothers, we remove them and add recursively their father
    * @param cell Moc cell
    * @return true if the cell (or its father) has been effectively inserted
    */
   public boolean add(MocCell cell) throws Exception {
      return add(cell.order,cell.npix);
   }

   /** Add a Moc pixel (at max order) corresponding to the alpha,delta position
    * Recursive addition : since with have the 3 brothers, we remove them and add recursively their father
    * @param alpha, delta position
    * @return true if the cell (or its father) has been effectively inserted
    */
   public boolean add(HealpixImpl healpix,double alpha, double delta) throws Exception {
      int order = getMocOrder();
      if( order==-1 ) return false;
      long npix = healpix.ang2pix(order, alpha, delta);
      return add(order,npix);
   }


   /** Add a MOC pixel
    * Recursive addition : since with have the 3 brothers, we remove them and add recursively their father
    * @param order HEALPix order
    * @param npix Healpix number  (-1 if no number => just for memorizing max MocOrder)
    * @param testHierarchy true if the ascendance and descendance consistance test must be done
    * @return true if something has been really inserted (npix or ascendant)
    */
   public boolean add(int order, long npix) throws Exception { return add(order,npix,true); }
   private boolean add(int order, long npix,boolean testHierarchy) throws Exception {
      range=null;
      
      // Fast insertion
      if( !testConsistency ) {
         isConsistant=false;
         return add1(order,npix);
      }

      if( npix>=0 ) {
         
         if( testHierarchy ) {
            // An ascendant is already inside ?
            if( order>minOrder && isDescendant(order, npix) ) return false;

            // remove potential descendants
            deleteDescendant(order, npix);
         }

         if( order>minOrder && deleteBrothers(order,npix) ) {
            return add(order-1,npix>>>2,testHierarchy);
         }
      }

      return add1(order,npix);

   }

   // Delete 3 others brothers if all present
   private boolean deleteBrothers(int order,long me) {
      return level[order].deleteBrothers(me);
   }

   /** remove a list of MOC pixels provided in a string format
    * (ex: "order1/npix1-npix2 npix3 ... order2/npix4 ...")
    */
   public void delete(String s) {
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t");
      while( st.hasMoreTokens() ) deleteHpix(st.nextToken());
   }

   /** Remove a MOC pixel
    * @param order HEALPix order
    * @param npix Healpix number
    * @return true if the deletion has been effectively done
    */
   public boolean delete(int order, long npix) {
      if( order>=nOrder ) return false;
      range=null;
      return level[order].delete(npix);
   }

   /** Remove all descendants of a MOC Pixel
    * @param order
    * @param npix
    * @return true if at least one descendant has been removed
    */
   public boolean deleteDescendant(int order,long npix) {
      range=null;
      long v1 = npix*4;
      long v2 = (npix+1)*4-1;
      boolean rep=false;
      for( int o=order+1 ; o<nOrder; o++, v1*=4, v2 = (v2+1)*4 -1) {
         rep |= getArray(o).delete(v1, v2);
      }
      return rep;
   }

   /** Sort each level of the Moc */
   public void sort() {
      for( int order=0; order<nOrder; order++ ) level[order].sort();
   }

   /** Return true if all Moc level is sorted */
   public boolean isSorted() {
      for( int order=0; order<nOrder; order++ ) {
         if( !level[order].isSorted() ) return false;
      }
      return true;
   }

   /** Check and fix the consistency of the moc
    * => remove cell redundancies
    * => factorize 4 brothers as 1 father (recursively)
    * => check and fix descendance consistancy
    *   [REMOVED => Trim the limitOrder if required ]
    */
   public void checkAndFix() throws Exception {
      range=null;
      if( nOrder==0 || isConsistant ) return;
      sort();
      SMoc res = new SMoc(sys,minOrder,mocOrder);
      int p[] = new int[nOrder];
      for( int npix=0; npix<12; npix++) checkAndFix(res,p,0,npix);

//      boolean flagTrim=true;
      for( int order=res.nOrder-1; order>=0; order-- ) {
         Array a = res.getArray(order);
         level[order]=a;
         // On ne change plus le nOrder pour conserver le MocOrder implicite
         //         if( flagTrim && a.getSize()!=0 ) { nOrder=order+1; flagTrim=false; }
      }
      nOrder= p.length; // res.nOrder;
      res=null;
      isConsistant=true;
      //      System.out.println("checkAndFix: nOrder="+nOrder+" minLimitOrder="+minLimitOrder+" maxLimitOrder="+maxLimitOrder);
   }

   // Recursive MOC tree scanning
   private void checkAndFix(SMoc res, int p[], int order, long pix) throws Exception {
      Array a;

      //      for( int j=0; j<order; j++ ) System.out.print("  ");
      //      System.out.print(order+"/"+pix);

      // Détermination de la valeur de la tête du niveaeu
      long t=-1;
      a = getArray(order);
      if( p[order]<a.getSize() ) t = a.get(p[order]);

      //      System.out.print(" t="+t);
      //
      // Ca correspond => on ajoute au résultat, et on supprime la descendance éventuelle
      if( t==pix ) {
         res.add(order,pix,false);
         for( int o=order; o<p.length; o++ ) {
            long mx=(pix+1)<<((o-order)<<1);
            a = getArray(o);
            while( p[o]<a.getSize() && a.get(p[o])<mx ) p[o]++;
         }
         //         System.out.println(" => Add + remove possible descendance => return");
         return;
      }

      // On n'a pas trouvé la cellule, s'il y a une descendance dans l'un ou l'autre
      // des arbres, on va continuer récursivement sur les 4 fils.
      boolean found=false;
      for( int o=order+1; o<p.length; o++ ) {
         long mx = (pix+1)<<((o-order)<<1);
         a = getArray(o);
         if( p[o]<a.getSize() && a.get(p[o])<mx ) { found=true; break; }
      }
      if( found ) {
         if( res.mocOrder!=-1 && order>res.mocOrder ) {
            //            System.out.println(" => rien mais descendance au dela de la limite, Add + remove possible descendance => return");
            res.add(order,pix,false);
            for( int o=order; o<p.length; o++ ) {
               long mx=(pix+1)<<((o-order)<<1);
               a = getArray(o);
               while( p[o]<a.getSize() && a.get(p[o])<mx ) p[o]++;
            }
         }

         else for( int i=0; i<4; i++ ) {
            //            System.out.println(" => rien mais descendance =>  parcours des fils...");
            checkAndFix(res,p,order+1,(pix<<2)+i);
         }
      }
      //    else System.out.println(" => aucun descendance => return");
   }


   /** True is the MOC pixel is an ascendant */
   public boolean isAscendant(int order, long npix) {
//      if( RANGE ) {
//         for( int o=order+1, shift=2; o<nOrder; o++, shift+=2 ) {
//            if( level[o].intersectRange(npix<<shift, (npix+1)<<shift -1) ) return true;
//         }
//      } else {
         long range=4L;
         for( int o=order+1; o<nOrder; o++,range*=4L ) {
            if( level[o].intersectRange(npix*range, (npix+1)*range -1) ) return true;
         }
//      }
      return false;
   }


   /** True if the MOC pixel is a descendant */
   public boolean isDescendant(int order, long npix) {
//      if( RANGE ) {
         long pix=npix>>>2;
         for( int o=order-1; o>=minOrder; o--,pix>>>=2 ) {
            if( level[o].find(pix)>=0 ) return true;
         }
//      } else {
//         long pix=npix/4;
//         for( int o=order-1; o>=minOrder; o--,pix/=4 ) {
//            if( level[o].find(pix)>=0 ) return true;
//         }
//      }
      return false;
   }

   /** True if the MOC pixel is present at this order */
   public boolean isIn(int order, long npix) {
      return level[order].find(npix)>=0;
   }

   /** MOC propertie setter
    * @param key => MOCORDER, COORDSYS, MOCTOOL, MOCTYPE, MOCID, DATE, ORIGIN, EXTNAME
    * @param value
    * @throws Exception
    */
   public void setProperty(String key, String value, String comment) throws Exception {

      // In case of a setProperty() direct call without using setMocOrder(...)
      if( key.equals("MOCORDER") ) {
         int mocOrder = Integer.parseInt(value);
         setMocOrder(mocOrder);
      }

      // In case of a setProperty() direct call without using setCoordSys(...)
      else if( key.equals("COORDSYS") ) {
         setSys(value);
      }

      // default
      else {
         property.put(key,value);
         if( comment==null ) this.comment.remove(key);
         else this.comment.put(key,comment);
      }
   }

   
   /** Return the fraction of the sky covered by the Moc [0..1] */
   public double getCoverage() {
      long usedArea = getNbCells();
      long area = getNbCellsFull();
      while( area>(long)Double.MAX_VALUE || usedArea>(long)Double.MAX_VALUE ) { area /= 2L; usedArea /= 2L; }
      if( area==0 ) return 0.;
      return (double)usedArea / area;
   }

   /** Return the number of low level pixels of the Moc  */
   public long getNbCells() {
      long n=0;
      long sizeCell = 1L;
      for( int order=getMocOrder(); order>=0; order--, sizeCell*=4L ) n += getSize(order)*sizeCell;
      return n;
   }

   /** return the area of the Moc computed in pixels at the most low level */
   public long getNbCellsFull() {
      long nside = pow2( getMocOrder() );
      return 12L*nside*nside;
   }

   /** Provide an Iterator on the MOC pixel List. Each Item is a couple of longs,
    * the first long is the order, the second long is the pixel number */
   public Iterator<MocCell> iterator() { return new HpixListIterator(); }

   /** Provide an Iterator on the low level pixel list covering all the MOC area.
    * => pixel are provided in ascending order */
   public Iterator<Long> pixelIterator() { 
      sort();
      return new PixelIterator();
   }
   
   /** Remove all the unused space (allocation reservation) */
   public void trim() {
      for( int order=0; order<nOrder; order++ ) level[order].trim();
   }

   // Juste pour du debogage
   public String todebug() {
      StringBuffer s = new StringBuffer();
      double coverage = (int)(getCoverage()*10000)/100.;
      s.append("mocOrder="+getMocOrder()+" ["+minOrder+".."+getMaxUsedOrder()+"] mem="+getMem()/1024L+"KB size="+getSize()+" coverage="+coverage+"%"
            +(isSorted()?" sorted":"")+(isConsistant?" consistant":"")+"\n");
      long oOrder=-1;
      Iterator<MocCell> it = iterator();
      for( int i=0; it.hasNext() && i<80; i++ ) {
         MocCell x = it.next();
         if( x.order!=oOrder ) s.append(" "+x.order+"/");
         else s.append(",");
         s.append(x.npix);
         oOrder=x.order;
      }
      if( it.hasNext() ) s.append("...\n");

      for( int order=0; order<nOrder; order++ ) {
         s.append(" "+order+":"+level[order].getSize());
      }
      return s.toString();
   }
   
   private static final int MAXWORD=20;
   private static final int MAXSIZE=80;

   public String toJSON() {
      StringBuilder res= new StringBuilder(getSize()*8);
      int order=-1;
      boolean flagNL = getSize()>MAXWORD;
      boolean first=true;
      int sizeLine=0;
      res.append("{");
      for( MocCell c : this ) {
         if( res.length()>0 ) {
            if( c.order!=order ) {
               if( !first ) res.append("],");
               if( flagNL ) { res.append("\n"); sizeLine=0; }
               else res.append(" ");
            } else {
               int n=(c.npix+"").length();
               if( flagNL && n+sizeLine>MAXSIZE ) { res.append(",\n "); sizeLine=3; }
               else { res.append(','); sizeLine++; }
            }
            first=false;
         }
         String s = c.order!=order ?  "\""+c.order+"\":["+c.npix : c.npix+"";
         res.append(s);
         sizeLine+=s.length();
         order=c.order;
      }
      int n = res.length();
      if( first ) res.append("}");   // MOC vide
      else if( res.charAt(n-1)==',' ) res.replace(n-1, n-1, "]"+(flagNL?"\n":" ")+"}");
      else res.append("]"+(flagNL?"\n":" ")+"}");
      return res.toString();
   }
   

   /*************************** Operations on MOCs ************************************************/
   
   
   /** Provide array of ranges at the deepest order */
   public Range getRange() {
      toRangeSet();
      return range;
   }

   // Store the MOC as a RangeSet if not yet done
   public void toRangeSet() { toRangeSet(false); }
   public void toRangeSet( boolean force) {
      if( !force && range!=null ) return;   // déjà fait
      sort();
      range = new Range( getSize() );
      Range rtmp=new Range();
      for (int order=0; order<nOrder; ++order) {
         rtmp.clear();
         int shift=2*(Healpix.MAXORDER-order);
         for( long npix : getArray(order) ) rtmp.append (npix<<shift,(npix+1)<<shift);
         if( !rtmp.isEmpty() ) range=range.union(rtmp);
      }
   }
   
   public void toMocSet() throws Exception {
      if( RANGE ) toMocSetR();
      else toMocSetFX();
   }
   
   // Generate the Moc tree structure from the rangeSet
   public void toMocSetR() throws Exception {
      clear();
      setCheckConsistencyFlag(false);
      Range r2 = new Range( getRange() );
      Range r3 = new Range();
      for( int o=0; o<=MAXORDER; ++o) {
         if( r2.isEmpty() ) break;
         int shift = 2*(MAXORDER-o);
         long ofs=(1L<<shift)-1;
         r3.clear();
         for( int i=0; i<r2.sz; i+=2 ) {
            long a=(r2.r[i]+ofs) >>>shift;
            long b= r2.r[i+1] >>>shift;
            if( a>=b ) continue;
            r3.append(a<<shift, b<<shift);
            for( long c=a; c<b; add1(o,c++) );
         }
         if( !r3.isEmpty() ) r2 = r2.difference(r3);
      }
   }
   
   public void toMocSetFX() throws Exception {
      clear();
      setCheckConsistencyFlag(false);     
      Range range = new Range( getRange() );
      for( int i=0; i<range.sz; i+=2 ) {   
         long l = range.r[i];
         long h = range.r[i+1];
         do {
            long len = h - l;
            assert len > 0;
            int ddMaxFromLen = (63 - Long.numberOfLeadingZeros(len)) >> 1;
            int ddMaxFromLow = Long.numberOfTrailingZeros(l) >> 1;
            int dd = Math.min(29, Math.min(ddMaxFromLen, ddMaxFromLow));
            int twiceDd = dd << 1;
            add1(29 - dd, l >> twiceDd);
            l += 1L << twiceDd;
         } while (l < h);
      }
   }
   
   static public void main(String b[] ) {
      try {
         long t,tot=0L;
         
         SMoc moc = new SMoc();
         moc.read("C:/Users/Pierre/Documents/Fits et XML/CDS-P-SDSS9-color-alt_MOC.fits");
         System.out.println("  Moc: "+moc.todebug());

         int N=10;
         for( int i=0; i<N; i++ ) {
            moc.read("C:/Users/Pierre/Documents/Fits et XML/CDS-P-SDSS9-color-alt_MOC.fits");
            t = Util.getTime();
            moc.accretion();
            long d = (Util.getTime()-t);
            System.out.println("  Moc: "+moc.todebug()+" in "+d+"ms");
            tot+=d;
         }
         System.out.println("Moyenne : "+(tot/N)+"ms");
         
     } catch( Exception e ) { e.printStackTrace(); }
   }

   /** True if the npix at the deepest order is in the MOC */
   public boolean contains(long npix) {
      toRangeSet();
      return range.contains(npix);
   }

   /** Fast test for checking if the cell is intersecting
    * the current MOC object
    * @return true if the intersection is not null
    */
   public boolean isIntersecting(int order,long npix) {
      
      // Plus rapide si on dispose déjà du range
      if( RANGE && range!=null ) {
         long shift = ((MAXORDER-order)<<1);
         return range.overlaps( npix<<shift, (npix+1)<<shift  );
      }
      
      return isIn(order,npix) || isAscendant(order,npix) || isDescendant(order,npix);
   }

   /** Fast test for checking if the parameter MOC is intersecting
    * the current MOC object
    * @return true if the intersection is not null
    */
   public boolean isIntersecting(Moc moc) {
      if( isFull() && !moc.isEmpty() 
            || moc.isFull() &&  !isEmpty() ) return true;
      
      // Un peu plus rapide si on dispose déjà des ranges
      if( RANGE && range!=null && ((SMoc)moc).range!=null ) {
         return range.contains( ((SMoc)moc).range );
      }
      
      sort();
      ((SMoc)moc).sort();
      for( int o=0; o<nOrder; o++ ) {
         Array a = ((SMoc)moc).getArray(o);
         if( isInTree(o,a) ) return true;
      }
      return false;
   }
   
   /** Fast test for checking if the parameter MOC is totally inside
    * the current MOC object
    * @return true if MOC is totally inside
    */
   public boolean isIncluding(SMoc moc) {
      if( isFull() ) return true;
      Iterator<MocCell> it = moc.iterator();
      while( it.hasNext() ) {
         MocCell c = it.next();
         if( !isIn(c.order,c.npix) && !isDescendant(c.order,c.npix) ) return false;
      }
      return true;
   }
   
   /** Retourne la composante spatiale du MOC */
   public SMoc getSpaceMoc() throws Exception { return this; }
   
   /** Retourne la composante temporelle du MOC */
   public TMoc getTimeMoc() throws Exception { throw new Exception("No temporal dimension"); }

   /** Retourne la composante en énergie du MOC */
   public EMoc getEnergyMoc() throws Exception { throw new Exception("No energy dimension"); }

   public Moc union(Moc moc) throws Exception {
      return operation(moc.getSpaceMoc(),0);
   }
   public Moc intersection(Moc moc) throws Exception {
      return operation(moc.getSpaceMoc(),1);
   }
   public Moc subtraction(Moc moc) throws Exception {
      return operation(moc.getSpaceMoc(),2);
   }

   public Moc difference(Moc moc) throws Exception {
      SMoc m = moc.getSpaceMoc();
      Moc inter = intersection(m);
      Moc union = union(m);
      return union.subtraction(inter);
   }


   public SMoc complement() throws Exception {
      SMoc allsky = new SMoc();
      allsky.add("0/0-11");
      allsky.toRangeSet();
      toRangeSet();
      SMoc res = new SMoc(sys,minOrder,mocOrder);
      res.range = allsky.range.difference(range);
      res.toMocSet();
      return res;
   }

   // Generic operation
   protected SMoc operation(SMoc moc,int op) throws Exception {
      testCompatibility(moc);
      toRangeSet();
      moc.toRangeSet();
      int min = Math.min(minOrder,moc.minOrder);
      int max = Math.max(getMocOrder(),moc.getMocOrder());
      SMoc res = new SMoc(sys,min,max);
      switch(op) {
         case 0 : res.range = range.union(moc.range); break;
         case 1 : res.range = range.intersection(moc.range); break;
         case 2 : res.range = range.difference(moc.range); break;
      }
      res.toMocSet();
      return res;
   }
   
   /** Return true if the MOC covers the whole sky */
   public boolean isFull() {
      return  getSize( minOrder ) == 12L*pow2(minOrder)*pow2(minOrder);
   }
   
   public boolean isSpace() { return true; }
   public boolean isTime()  { return false; }
   public boolean isEnergy()  { return false; }
   
   /** Return true if the MOC is empty */
   public boolean isEmpty() {
      return getSize()==0;
   }

   /** Equality test */
   public boolean equals(Object moc){
      if( this==moc ) return true;
      try {
         SMoc m = (SMoc) moc;
         testCompatibility(m);
         if( m.nOrder!=nOrder ) return false;
         for( int o=0; o<nOrder; o++ ) if( getSize(o)!=m.getSize(o) ) return false;
         for( int o=0; o<nOrder; o++ ) {
            if( !getArray(o).equals( m.getArray(o) ) ) return false;
         }
      } catch( Exception e ) {
         return false;
      }
      return true;
   }

   public SMoc queryCell(int order,long npix) throws Exception {
      return (SMoc)intersection(new SMoc(order+"/"+npix));
   }


   /**************************** Low level methods for fast manipulations **************************/

   /** Set the pixel list at the specified order (order>13 )
    * (Dedicated for fast initialisation)  */
   public void setPixLevel(int order,long [] val) throws Exception {
      if( getType(order)!=LONG ) throw new Exception("The order "+order+" requires long[] array");
      level[order]=new LongArray(val);
      if( nOrder<order+1 ) nOrder=order+1;
   }

   /** Set the pixel list at the specified order (6<=order<=13 )
    * (Dedicated for fast initialisation)  */
   public void setPixLevel(int order,int [] val) throws Exception {
      if( getType(order)!=INT ) throw new Exception("The order "+order+" requires int[] array");
      level[order]=new IntArray(val);
      if( nOrder<order+1 ) nOrder=order+1;
   }

   /** Set the pixel list at the specified order (order<6)
    * (Dedicated for fast initialisation)  */
   public void setPixLevel(int order,short [] val) throws Exception {
      if( getType(order)!=SHORT ) throw new Exception("The order "+order+" requires short[] array");
      level[order]=new ShortArray(val);
      if( nOrder<order+1 ) nOrder=order+1;
   }

   /** Provide a copy of the pixel list at the specified order (in longs) */
   public long [] getPixLevel(int order) {
      int size = getSize(order);
      long [] lev = new long[size];
      if( size==0 ) return lev;
      Array a = level[order];
      for( int i=0; i<size; i++ ) lev[i] = a.get(i);
      return lev;
   }

   /*************************** read and write *******************************************************/

   /** Read HEALPix MOC from a file.
    * Support all MOC syntax: FITS standard or any other old syntax (JSON or ASCII)
    * @param filename file name
    * @throws Exception
    */
   public void read(String filename) throws Exception {
      (new MocIO(this)).read(filename);
   }

   /** Read HEALPix MOC from a file.
    * @param filename file name
    * @param mode ASCII, JSON, FITS encoded format
    * @throws Exception
    * @deprecated see read(String)
    */
   public void read(String filename,int mode) throws Exception {
      (new MocIO(this)).read(filename,mode);
   }

   /** Read HEALPix MOC from a stream
    * Support all MOC syntax: FITS standard or any other old syntax (JSON or ASCII)
    */
   public void read(InputStream in) throws Exception { (new MocIO(this)).read(in); }

   /** Read HEALPix MOC from a stream.
    * @param in input stream
    * @param mode ASCII, JSON, FITS encoded format
    * @throws Exception
    * @deprecated see read(InputStream)
    */
   public void read(InputStream in,int mode) throws Exception {
      (new MocIO(this)).read(in,mode);
   }

   /** @deprecated see read(InputStream) */
   public void readASCII(InputStream in) throws Exception {
      (new MocIO(this)).read(in,ASCII);
   }

   /** @deprecated see read(InputStream) */
   public void readJSON(InputStream in) throws Exception {
      (new MocIO(this)).read(in,JSON);
   }

   /** Read HEALPix MOC from an Binary FITS stream
    * @deprecated see read(InputStream)
    */
   public void readFits(InputStream in) throws Exception {
      (new MocIO(this)).read(in,FITS);
   }

   /** Write HEALPix MOC to a file
    * @param filename name of file
    */
   public void write(String filename) throws Exception {
      check();
      (new MocIO(this)).write(filename);
   }

   /** Write HEALPix MOC to a file
    * @param filename name of file
    * @param mode encoded format (ASCII, JSON or FITS)
    */
   public void write(String filename,int mode) throws Exception {
      check();
      (new MocIO(this)).write(filename,mode);
   }

   /** Write HEALPix MOC to an output stream
    * @param out output stream
    * @param mode encoded format (ASCII, JSON or FITS)
    */
   public void write(OutputStream out,int mode) throws Exception {
      check();
      (new MocIO(this)).write(out,mode);
   }

   /** Write HEALPix MOC to an output stream IN JSON encoded format
    * @param out output stream
    */
   public void writeJSON(OutputStream out) throws Exception {
      check();
      (new MocIO(this)).writeJSON(out);
   }

   /** Write HEALPix MOC to an output stream in FITS encoded format
    * @param out output stream
    */
   public void writeFits(OutputStream out) throws Exception { writeFITS(out); }

   /** deprecated */
   public void writeFITS(OutputStream out) throws Exception {
      check();
      (new MocIO(this)).writeFits(out);
   }
   
   /** Write specifif FITS keywords
    * @param out
    * @return number of written bytes
    * @throws Exception
    */
   protected int writeSpecificFitsProp( OutputStream out  ) throws Exception {
      int n=0;
      out.write( MocIO.getFitsLine("TTYPE1","UNIQ","UNIQ pixel number") ); n+=80;
      out.write( MocIO.getFitsLine("PIXTYPE","HEALPIX","HEALPix magic code") );    n+=80;
      out.write( MocIO.getFitsLine("ORDERING","NUNIQ","NUNIQ coding method") );    n+=80;      
      out.write( MocIO.getFitsLine("COORDSYS",""+getSys(),"reference frame (C=ICRS)") );  n+=80;
      out.write( MocIO.getFitsLine("MOC","SPACE","Spacial MOC") );    n+=80;      
      out.write( MocIO.getFitsLine("MOCORDER",""+getMocOrder(),"MOC resolution (best order)") );    n+=80;      
      return n;
   }

   // Write the UNIQ FITS HDU Data in basic mode
   protected int writeSpecificData(OutputStream out) throws Exception {
      if( getSize()<=0 ) return 0;
      int nbytes=getType()==SMoc.LONG ? 8 : 4;  // Codage sur des integers ou des longs
      byte [] buf = new byte[nbytes];
      int size = 0;
      for( int order=0; order<nOrder; order++ ) {
         int n =getSize(order);
         if( n==0 ) continue;
         Array a = getArray(order);
         for( int i=0; i<n; i++) {
            long val = SMoc.hpix2uniq(order, a.get(i) );
            size+=MocIO.writeVal(out,val,buf);
         }
      }
      return size;
   }
   
   protected void readSpecificData( InputStream in, int naxis1, int naxis2, int nbyte, cds.moc.MocIO.HeaderFits header) throws Exception {
      byte [] buf = new byte[naxis1*naxis2];
      MocIO.readFully(in,buf);
      createUniq((naxis1*naxis2)/nbyte,nbyte,buf);
   }
   
   protected void createUniq(int nval,int nbyte,byte [] t) throws Exception {
      int i=0;
      long [] hpix = null;
      long val;
      for( int k=0; k<nval; k++ ) {
         int a =   ((t[i++])<<24) | (((t[i++])&0xFF)<<16) | (((t[i++])&0xFF)<<8) | (t[i++])&0xFF;
         if( nbyte==4 ) val = a;
         else {
            int b = ((t[i++])<<24) | (((t[i++])&0xFF)<<16) | (((t[i++])&0xFF)<<8) | (t[i++])&0xFF;
            val = (((long)a)<<32) | ((b)& 0xFFFFFFFFL);
         }
         hpix = SMoc.uniq2hpix(val,hpix);
         add( (int)hpix[0], hpix[1]);
      }
   }

   protected int getType() { return getType( getMaxUsedOrder() ); }


   /***************************************  Les classes privées **************************************/

   // Création d'un itérator sur la liste des pixels
   private class HpixListIterator implements Iterator<MocCell> {
      private int currentOrder=0;
      private int indice=-1;
      private boolean ready=false;

      public boolean hasNext() {
         goNext();
         return currentOrder<nOrder;
      }

      public MocCell next() {
         if( !hasNext() ) return null;
         ready=false;
         Array a = level[currentOrder];
         return new MocCell(currentOrder, a.get(indice));
      }

      public void remove() {  }

      private void goNext() {
         if( ready ) return;
         for( indice++; currentOrder<nOrder && indice>=getSize(currentOrder); currentOrder++, indice=0);
         ready=true;
      }
   }
   
//   // Iterator identique au précédent mais travaillant sur le range directement
//   // => Beaucoup plus lent (40x)
//   private class HpixListIterator implements Iterator<MocCell> {
//      Range r2,r3;
//      long a,b;
//      int o;
//      int i;
//      int shift;
//      long ofs;
//      boolean flagEnd;
//      boolean took;
//      
//      HpixListIterator() {
//         r2 = new Range( getRange() );
//         r3 = new Range();
//         o=0;
//         i=-2;
//         shift = 2*MAXORDER;
//         ofs=(1L<<shift)-1;
//         flagEnd=false;
//         took=true;
//      }
//
//      public boolean hasNext() {
//         goNext();
//         return !flagEnd;
//      }
//
//      public MocCell next() {
//         if( !hasNext() ) return null;
//         took=true;
//         return new MocCell(o,a++);
//      }
//
//      public void remove() {  }
//
//      private void goNext() {
//         if( flagEnd || !took ) return;
//         if( i>=0 && a<b ) return;
//         do {
//            for( i+=2; i<r2.sz; i+=2) {
//               a=(r2.r[i]+ofs) >>>shift;
//               b= r2.r[i+1] >>>shift;
//               if( a>=b ) continue;
//               r3.append(a<<shift, b<<shift);
//               took=false;
//               return;
//            }
//            if( !r3.isEmpty() ) r2 = r2.difference(r3);
//            if( o==MAXORDER || r2.isEmpty() ) { 
//               flagEnd=true; 
//               break;
//            }
//            o++;
//            shift = 2*(MAXORDER-o);
//            ofs=(1L<<shift)-1;
//            r3.clear();
//            i=-2;
//         } while( true );
//      }
//   }

   // Création d'un itérator sur la liste des pixels triés et ramenés au max order
   // Méthode : parcours en parallèle tous les niveaux, en conservant pour chacun d'eux l'indice de sa tête
   //           prends la plus petite "tête", et pour celle-là, itère dans l'intervalle (fonction de la différence par rapport
   //           à l'ordre max => 4 pour order-1, 16 pour order-2 ...).
   private class PixelIterator implements Iterator<Long> {
      private boolean ready=false;      // Le goNext() a été effectué et le pixel courant pas encore lu
      private long current;             // Pixel courant
      private int order=-1;             // L'ordre courant
      private long indice=0L;           // l'indice dans l'intervalle de l'ordre courant
      private long nb=0L;               // Nombre d'éléments dans l'intervalle courant
      private long currentTete;         // Valeur de la tete courante
      private boolean hasNext=true;     // false si on a atteind la fin du de tous les ordres
      private int p[] = new int[getMocOrder()+1];// indice courant pour chaque ordre

      public boolean hasNext() {
         goNext();
         return hasNext;
      }

      public Long next() {
         if( !hasNext() ) return null;
         ready=false;
         return current;
      }

      public void remove() {  }

      private void goNext() {
         if( ready ) return;

         // recherche de la plus petite tete parmi tous les orders
         if( indice==nb ) {
            long min = Long.MAX_VALUE;
            long fct=1L;
            long tete=-1L;
            int mocOrder = getMocOrder();
            order=-1;
            for( int o=mocOrder; o>=minOrder; o--, fct*=4 ) {
               Array a = level[o];
               if( a==null ) continue;
               tete = p[o]<a.getSize() ? a.get(p[o])*fct : -1;
               if( tete!=-1 && tete<min ) { min=tete; order=o; nb=fct; }
            }
            if( order==-1 ) { hasNext=false; ready=true; return; }
            currentTete=min;
            indice=0L;
         }

         // On énumère tous les pixels du range
         current = new Long(currentTete + indice);
         indice++;

         // Si on a terminé le range courant, on avance l'indice de sa tete
         if( indice==nb ) p[order]++;
         ready=true;
      }
   }

   // Internal initialisations => array of levels allocation
   protected void init(String sys,int minOrder,int mocOrder) {
      this.sys=sys;
      this.minOrder=minOrder;
      this.mocOrder=mocOrder;
      property = new LinkedHashMap<>();
      comment = new HashMap<>();
      if( mocOrder!=-1 ) property.put("MOCORDER",mocOrder+"");
      initPropSys(sys);
      property.put("MOCTOOL","CDSjavaAPI-"+VERSION);
      property.put("DATE",String.format("%tFT%<tR", new Date()));

      testConsistency=true;
      isConsistant=true;
      level = new Array[MAXORDER+1];
      for( int order=0; order<MAXORDER+1; order++ ) {
         int type = getType(order);
         int bloc = (1+order)*10;
         Array a = type==SHORT ? new ShortArray(bloc)
                   : type==INT ? new IntArray(bloc) : new LongArray(bloc);
         level[order]=a;
      }
   }

   protected void initPropSys(String sys) { property.put("COORDSYS",sys); }

   // Low level npixel addition.
   protected boolean add1(int order, long npix) throws Exception {
      if( order<minOrder ) return add2(order,npix,minOrder);
      if( mocOrder!=-1 && order>mocOrder ) return add(mocOrder, npix>>>((order-mocOrder)<<1));

      if( order>MAXORDER ) throw new Exception("Out of MOC order");
      if( order>=nOrder ) nOrder=order+1;
      
      if( npix<0 ) return false;

      return level[order].add(npix,testConsistency);
   }

   // Low level npixel multi-addition
   private boolean add2(int orderSrc, long npix, int orderTrg) throws Exception {
      if( orderTrg>MAXORDER ) throw new Exception("Out of MOC order");
      if( orderTrg>=nOrder ) nOrder=orderTrg+1;
      if( npix<0 ) return false;
      long fct = pow2(orderTrg-orderSrc);
      fct *= fct;
      npix *= fct;
      boolean rep=false;

      for( int i=0; i<fct; i++ ) rep |=level[orderTrg].add(npix+i,testConsistency);
      return rep;
   }

   // Ajout d'un pixel selon le format "order/npix[-npixn]".
   // Si l'order n'est pas mentionné, utilise le dernier order utilisé
   protected void addHpix(String s) throws Exception {
      if( s==null ) return;
      int i=s.indexOf('/');
      if( i<0 ) i=s.indexOf(':');
      if( i>0 ) {
         String s1= unQuote(s.substring(0,i));
         // Possible préfixe 's' (voire 't' pour un TMOC)
         if( s1.charAt(0)=='s' || s1.charAt(0)=='t') s1=s1.substring(1);
         currentOrder = Integer.parseInt( s1 );
      }
      int j=s.indexOf('-',i+1);
      if( j<0 ) {
         String s1 = unBracket( s.substring(i+1) );
            long npix = s1.trim().length()==0 ? -1 : Long.parseLong( s1 );
            add(currentOrder, npix );
      } else {
         long startIndex = Long.parseLong(s.substring(i+1,j));
         long endIndex = Long.parseLong(s.substring(j+1));
         
         // Accélération en passant par l'ordre le plus profond
         if( endIndex-startIndex>10 ) addRange(currentOrder,startIndex,endIndex); 
         
         // Sinon au compte goutte
         else for( long k=startIndex; k<=endIndex; k++ ) add(currentOrder, k);
      }
   }
   
   /** Add range (deepest order)
    * @param min start range - included in the range
    * @param max end range - included in the range
    */
   public void add(long min, long max) {
      Range rtmp=new Range();
      rtmp.append(min,max+1L);
      if( !rtmp.isEmpty() ) range=range.union(rtmp);
   }
   
   private void addRange(int order,long start, long end ) throws Exception {
      int shift = (MAXORDER-order)*2;
      start = start<<shift;
      end =  (end+1)<<shift;
      toRangeSet();
      range.add(start, end);
      toMocSet();
   }

   private String unQuote(String s) {
      int n=s.length();
      if( n>2 && s.charAt(0)=='"' && s.charAt(n-1)=='"' ) return s.substring(1,n-1);
      return s;
   }

   private String unBracket(String s) {
      int n=s.length();
      if( n<1 ) return s;
      int o1 = s.charAt(0)=='[' ? 1:0;
      int o2 = s.charAt(n-1)==']' ? n-1 : n;
      return s.substring(o1,o2);
   }

   // Suppression d'un pixel selon le format "order/npix[-npixn]".
   // Si l'order n'est pas mentionné, utilise le dernier order utilisé
   private void deleteHpix(String s) {
      int i=s.indexOf('/');
      if( i>0 ) currentOrder = Integer.parseInt(s.substring(0,i));
      int j=s.indexOf('-',i+1);
      if( j<0 ) delete(currentOrder, Integer.parseInt(s.substring(i+1)));
      else {
         int startIndex = Integer.parseInt(s.substring(i+1,j));
         int endIndex = Integer.parseInt(s.substring(j+1));
         for( int k=startIndex; k<=endIndex; k++ ) delete(currentOrder, k);
      }
   }

   // Sort and check before writting
   public void check() throws Exception {
      if( !testConsistency ) checkAndFix();
      else sort();
   }
   
   public void accretion() throws Exception { accretion(getMocOrder()); }
   
   public void accretion(int order) throws Exception {
      toRangeSet();
      SMoc m = new SMoc(this);
      m.setCheckConsistencyFlag(false);
      int n=0;
      int o=-1;
      HealpixNested h = null;
      
      Iterator<MocCell> it = iterator();
      while( it.hasNext() ) {
         MocCell p = it.next();
         if( o!=p.order ) { h = cds.healpix.Healpix.getNested(p.order); o=p.order; }
         
         long neibs[] = externalNeighbours(h,p.order,p.npix, order);
         
         // 12/26085355
//         if( p.order==12 && p.npix==26085355 ) {
//            System.out.println("J'y suis");
//         }
         long nside = pow2(order-p.order);
         int shift = (order-p.order)<<1;
         for( int i=0; i<neibs.length; i++  ) {
            long neib = neibs[i];
            if( isIntersecting(order, neib) ) {
               
               // Si la première cellule du bord du voisin est déjà dans le MoC
               // on va vérifier si le voisin ne serait pas lui-même entièrement dans le MOC
               // et si oui, on ne teste plus les autres cellules de ce bord
               if( nside>2 && i%(nside+1)==1 ) {
                  if( isIn(p.order,neib>>>shift) ) i+=(nside-1);
               }
               continue;
            }
            m.add1(order,neib);
            if( ++n==100000 ) { m.checkAndFix(); n=0; }
         }
      }
      m.setCheckConsistencyFlag(true);
      m.clone1(this);
   }   
   
   
   private long [] externalNeighbours(HealpixNested h, int order, long npix, int o) throws Exception {
      int deltaDepth = o-order;
      if( deltaDepth== 0 ) {
         NeighbourList nl = h.neighbours(npix);
         long [] n = new long[nl.size()];
         nl.arraycopy(0, n, 0, n.length);
         return n;
      }
      FlatHashList res = h.externalEdges(npix, deltaDepth);
      long [] neib = new long[ res.size() ];
      res.arraycopy(0, neib, 0, neib.length);
      return neib;
   }
   
//   public void accretion(int order) throws Exception {
//      long t = Util.getTime();
//      toRangeSet();
//      Range peri = new Range();
//      int shift = (MAXORDER-order)*2;
//      
//      Iterator<Long> it = pixelIterator();
//      while( it.hasNext() ) {
//         long npix = it.next();
//         for( long neib : CDSHealpix.neighbours(order,npix) ) {
//            if( neib<0 ) continue;  // voisin manquant
//            long a = neib<<shift;
//            long b = (neib+1)<<shift;
//            if( !range.contains(a) && !peri.contains(a, b)) {
//               peri.add( a, b );
//            }
//         }
//      }
//      range = range.union(peri);
//      toMocSet();
//      System.out.println("accretion : "+(Util.getTime()-t)+"ms");
//   }   

   

   
   // Retourne la liste des numéros HEALPix des 4 voisins directs ou -1 s'ils sont en dehors du MOC   
   // Ordre des voisins => W, N, E, S
   static private long [] getVoisins(int order, SMoc moc, int maxOrder,long npix) throws Exception {
      long [] voisins = new long[8];
      long [] neib = CDSHealpix.neighbours(order,npix);
      for( int i=0,j=0; i<voisins.length; i++, j++ ) {
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
         voisins[i] = moc.isIn(maxOrder, neib[j]) ? neib[j] : -1;
      }
      return voisins;
   }

   
   /** Retourne la liste des pixels HEALPix du périmètre d'un losange HEALPIX d'ordre plus petit */
   static class Bord implements Iterator<Long> {
      int nside,bord,i;
      public Bord(int nside) { this.nside=nside; i=0; bord=0;}
      public boolean hasNext() {
         if( nside<2 && i>0 ) return false;
         return  bord<3 || bord==3 && i<nside-1;
      }
      public Long next() {
         long res = bord==0 ? cds.tools.pixtools.Util.getHpxNestedNumber(0,i) : bord==1 
                            ? cds.tools.pixtools.Util.getHpxNestedNumber(i,nside-1) : bord==2
                            ? cds.tools.pixtools.Util.getHpxNestedNumber(nside-1,nside-i-1) 
                            : cds.tools.pixtools.Util.getHpxNestedNumber(nside-i-1,0);
         if( (++i)>=nside ) { bord++; i=1; }
         return res;
      }
   }


   // Throw an exception if the coordsys of the parameter moc differs of the coordsys
   protected void testCompatibility(SMoc moc) throws Exception {
      if( getSys().charAt(0)!=moc.getSys().charAt(0) ) throw new Exception("Incompatible MOC coordsys");
   }

   // Determine the best "find" strategie
   // @return: 0-no object at all
   //          1-dichotomy - first arg as external loop
   //          2 dichotomy - second arg as external loop
   //          3-parallel scanning
   private int strategie(int size1, int size2) {
      if( size1==0 || size2==0 ) return 0;
      double m1 = size1 * (1+Math.log(size2)/Math.log(2));
      double m2 = size2 * (1+Math.log(size1)/Math.log(2));
      double m3 = size1+size2;
      if( m1<m2 && m1<m3 ) return 1;
      if( m2<m1 && m2<m3 ) return 2;
      return 3;
   }

   // true if the Array intersects the level[order]
   // (determine automatically the best "find" strategy)
   private boolean isIn(int order, Array a) {
      Array a1 = level[order];
      Array a2 = a;
      int size2 = a2.getSize();
      int size1 = a1.getSize();
      if( !a1.intersectRange( a2.get(0), a2.get(size2-1) ) ) return false;
      switch( strategie(size1,size2) ) {
         case 0: return false;
         case 1:
            for( long x : a1 ) if( a2.find(x)>=0 ) return true;
            return false;
         case 2:
            for( long x : a2 ) if( a1.find(x)>=0 ) return true;
            return false;
         default :
            boolean incr1=true;
            long x1=a1.get(0),x2=a2.get(0);
            for( int i1=0,i2=0; i1<size1 && i2<size2; ) {
               if( incr1 ) x1 = a1.get(i1);
               else x2 = a2.get(i2);
               if( x1==x2 ) return true;
               incr1 = x1<x2;
               if( incr1 ) i1++;
               else i2++;
            }
      }
      return false;
   }

   // true if the Array intersect the descendance
   // (determine automatically the best "find" strategy)
   private boolean isAscendant(int order, Array a) {
      long range=4L;
      for( int o=order+1; o<nOrder; o++,range*=4L ) {
         Array a1=level[o];
         Array a2=a;
         int size2 = a2.getSize();
         int size1 = a1.getSize();
         if( !a1.intersectRange( a2.get(0)*range, (a2.get(size2-1)+1)*range -1 ) ) continue;
         switch( strategie(size1,size2) ) {
            case 0: break;
            case 1:
               long onpix=-1L;
               for( long x : a1 ) {
                  long npix = x/range;
                  if( npix==onpix ) continue;
                  if( a2.find(npix)>=0 ) return true;
                  onpix=npix;
               }
               break;
            case 2:
               for( long pix : a2 ) {
                  if( a1.intersectRange(pix*range, (pix+1)*range -1) ) return true;
               }
               break;
            default:
               boolean incr1=true;
               long x1=a1.get(0),x2=a2.get(0)*range,x3=(a2.get(0)+1)*range-1;
               for( int i1=0,i2=0; i1<size1 && i2<size2; ) {
                  if( incr1 ) x1 = a1.get(i1);
                  else { x2 = a2.get(i2)*range; x3 = (a2.get(i2)+1)*range-1; }
                  if( x2<=x1 && x1<=x3 ) return true;
                  incr1 = x1<x3;
                  if( incr1 ) i1++;
                  else i2++;
               }
               break;
         }

      }
      return false;
   }

   // true if the Array intersect the ascendance
   // (determine automatically the best "find" strategy)
   private boolean isDescendant(int order, Array a) {
      long range=4L;
      for( int o=order-1; o>=0; o--,range*=4L ) {
         Array a1=level[o];
         Array a2=a;
         int size2 = a2.getSize();
         int size1 = a1.getSize();
         if( !a1.intersectRange( a2.get(0)/range, a2.get(size2-1)/range ) ) continue;
         switch( strategie(size1,size2) ) {
            case 0: break;
            case 1:
               for( long x : a1 ) {
                  if( a2.intersectRange(x*range, (x+1)*range -1) ) return true;
               }
               break;
            case 2:
               long onpix=-1L;
               for( long x : a2 ) {
                  long npix = x/range;
                  if( npix==onpix ) continue;
                  if( a1.find(npix)>=0 ) return true;
                  onpix=npix;
               }
               break;
            default :
               boolean incr1=true;
               long x1=a1.get(0),x2=a2.get(0)/range;
               for( int i1=0,i2=0; i1<size1 && i2<size2; ) {
                  if( incr1 ) x1 = a1.get(i1);
                  else x2 = a2.get(i2)/range;
                  if( x1==x2 ) return true;
                  incr1 = x1<x2;
                  if( incr1 ) i1++;
                  else i2++;
               }
               break;
         }

      }
      return false;
   }

   // true if the array intersects the Moc
   private boolean isInTree(int order,Array a) {
      if( a==null || a.getSize()==0) return false;
      if( a.getSize()==1 ) return isIntersecting( order,a.get(0) );
      return isIn(order,a) || isAscendant(order,a) || isDescendant(order,a);
   }


   /***************************************  Utilities **************************************************************/

   /** Pixel area (in square degrees) for a given order */
   static public double getPixelArea(int order) {
      if( order<0 ) return SKYAREA;
      long nside = pow2(order);
      long npixels = 12*nside*nside;
      return SKYAREA/npixels;
   }

   static private final double SKYAREA = 4.*Math.PI*Math.toDegrees(1.0)*Math.toDegrees(1.0);
   
   /** Changement de référentiel si nécessaire */
   static public SMoc convertTo(SMoc moc, String coordSys) throws Exception {
      if( coordSys.equals( moc.getSys()) ) return moc;

      char a = moc.getSys().charAt(0);
      char b = coordSys.charAt(0);
      int frameSrc = a=='G' ? Localisation.GAL : a=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;
      int frameDst = b=='G' ? Localisation.GAL : b=='E' ? Localisation.ECLIPTIC : Localisation.ICRS;

      Healpix hpx = new Healpix();
      int order = moc.getMaxUsedOrder();
      SMoc moc1 = new SMoc(coordSys,moc.getMinOrder(),moc.getMocOrder());
      moc1.setCheckConsistencyFlag(false);
      long onpix1=-1;
      Iterator<Long> it = moc.pixelIterator();
      while( it.hasNext() ) {
         long npix = it.next();
         for( int i=0; i<4; i++ ) {
            double [] coo = hpx.pix2ang(order+1, (npix<<2)+i);
            Coord c = new Coord(coo[0],coo[1]);
            c = Localisation.frameToFrame(c, frameSrc, frameDst);
            long npix1 = hpx.ang2pix(order+1, c.al, c.del);
            if( npix1==onpix1 ) continue;
            onpix1=npix1;
            moc1.add(order,npix1>>>2);
         }

      }
      moc1.setCheckConsistencyFlag(true);
      return moc1;
   }
   


}
