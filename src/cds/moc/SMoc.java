package cds.moc;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import cds.healpix.HealpixNested;

/**
 * The SMoc class implements the methods specific to spatial MOCs.
 * It is based on the HEALpix tesselation of the sphere.
 * See:  IVOA MOC 2.0 standard => https://www.ivoa.net/documents/MOC/
 * @author Pierre Fernique [CDS]
 * @version 1.0 - April 2021 - creation
 *
 */
public class SMoc extends Moc1D {
   
   static final public int  MAXORD_S = 29;       // Max order (<=> deepest HEALPix level)
   static final public int  FACT_S   = 4;        // Factor between two consecutive order
   static final public char DIM_S    = 's';      // Char signature for SMoc
   static final public long NBVAL_S  = pow2( MAXORD_S ) * pow2( MAXORD_S ) * 12L;   // nb of cells at the deepest order
   
   private int minOrder;                         // Min order, 0 by default
   private String altCoosys;                     // Alternative Coosys. Always 'C' for celestial sphere (ICRS),
   
   /** Return the deepest possible order (ex: 29 for SMoc, 61 for TMoc) */
   public final int  maxOrder() { return MAXORD_S;  }
   
   /** Return the number of bit shifting between two consecutive orders (ex: 2 for SMoc, 1 for TMoc) */
   public final int  shiftOrder() { return FACT_S/2;   }
   
   /** Return the Moc signature character (ex: 's' for SMoc, 't' for TMOC) */
   public final char cDim() { return DIM_S; }
   
   /** Return the number of values at the deepest order (ex: 2x2^29x2^29 for SMoc, 2^61 for TMoc) */
   public final long maxVal() { return NBVAL_S; }
   
   public SMoc() { super(); }
   public SMoc( int mocOrder ) { super( mocOrder ); }
   public SMoc( String s ) throws Exception { super(s); }
   public SMoc( SMoc moc ) throws Exception { super( moc ); }
   public SMoc( InputStream in ) throws Exception { super(in); }
   
   /** Reinitialisation of the MOC - data only (not the properties) */
   public void clear() {
      super.clear();
      altCoosys=null;
      minOrder=0;
   }
   
   /** Clone Moc (deep copy) */
   public SMoc clone() throws CloneNotSupportedException {
      SMoc moc = dup();
      clone1( moc );
      return moc;
   }
   
   /** Deep copy - internal method */
   protected void clone1( Moc moc ) throws CloneNotSupportedException {
      if( !(moc instanceof SMoc) ) throw new CloneNotSupportedException("Uncompatible type of MOC for clone. Must be SMoc");
      super.clone1( moc );
   }
      
   /** Create and instance of same class, same sys, but no data nor mocorder */
   public SMoc dup() {
      SMoc moc = new SMoc();
      moc.altCoosys=altCoosys;
      return moc;
   }
   
   /** Return the number of bytes used for coding each FITS value (4 for integer, 8 for long) */
   public int sizeOfCoding() { return getDeepestOrder()<14 ? 4 : 8; }
   
   /** Return the number of values to write in FITS serialization */
   public int getNbCoding() { return getNbCells(); }
   
   /** Add directly a SMoc */
   public void add(Moc moc) throws Exception { 
      if( !(moc instanceof SMoc) ) throw new Exception("Uncompatible Moc for adding");
      super.add(moc);
   }
   
   /*********************************** Pour assurer la migration d'Aladin et du Mocserver *************************/
   
   public void setCheckConsistencyFlag(boolean flag) throws Exception { }
   public void checkAndFix() throws Exception { }
   public void check() throws Exception {}
   public void sort() {}
   public void toRangeSet() {}
   public void toMocSet() throws Exception {}
   
   /************************************ SMoc specifical methods => HEALPix *****************************************/
   
   /** Add a Moc pixel (at max order) corresponding to the alpha,delta position
    * Recursive addition : since with have the 3 brothers, we remove them and add recursively their father
    * @param alpha, delta position
    * @return true if the cell (or its father) has been effectively inserted
    */
   public void add(HealpixImpl healpix,double alpha, double delta) throws Exception {
      int order = getMocOrder();
      if( order==-1 ) throw new Exception("Undefined Moc order");
      long npix = healpix.ang2pix(order, alpha, delta);
      add(order,npix,npix);
   }
   
   /** Check if the spherical coord is inside the MOC. The coordinate system must be compatible
    * with the MOC coordinate system.
    * @param alpha in degrees
    * @param delta in degrees
    * @return true if the coordinates is in one MOC pixel
    * @throws Exception
    */
   public boolean contains(HealpixImpl healpix,double alpha, double delta) throws Exception {
      int order = getDeepestOrder();
      if( order==-1 ) return false;
      long npix = healpix.ang2pix(order, alpha, delta);
      return isIncluding(order,npix);
   }

   /**
    * Provide Moc pixels totally or partially inside a circle
    * @param alpha circle center (in degrees)
    * @param delta circle center (in degrees)
    * @param radius circle radius (in degrees)
    * @return an HealpixMox containing the list of pixels
    * @throws Exception
    */
   public SMoc queryDisc(HealpixImpl healpix,double alpha, double delta,double radius) throws Exception {
      int order = getDeepestOrder();
      if( order==-1 ) return null;
      SMoc mocA = dup();
      long [] list = healpix.queryDisc(order, alpha, delta, radius);
      for( long npix : list ) mocA.add(order,npix,npix);
      return intersection(mocA);
   }   
   
   /** Set alternative Coosys. All celestial SMoc must be expressed in ICRS (see IVOA MOC 2.0)
    * but alternative is possible for other sphere coverage notably the planets
    * @param coosys alternative coosys keyword (not standardized in IVOA document)
    */
   public void setSys( String coosys ) { altCoosys = coosys; }
   
   /** Get the Coosys. See setCooSys() */
   public String getSys() { return altCoosys!=null ? altCoosys : "C"; }
   
   /** Set min Moc min Order. The hierarchical view will be start at this order. 0 by default */
   public void setMinOrder( int minOrder ) throws Exception {
      if( minOrder==this.minOrder ) return;
      if( minOrder<0 || minOrder>maxOrder() ) throw new Exception("MinOrder error ("+minOrder+" not in [0.."+maxOrder()+"])");
      if( mocOrder!=-1 && minOrder>mocOrder ) throw new Exception("MinOrder cannot be bigger that Moc order");
      this.minOrder = minOrder;
      resetCache();
   }
   
   /** get min Moc min Order. The hierarchical view will be start at this order. 0 by default */
   public int getMinOrder() { return minOrder; }
   
   /** Provide the angular resolution (in degrees) of the SMoc (sqrt of the smallest pixel area) */
   public double getAngularRes() {
      return Math.sqrt( Healpix.getPixelArea( getMocOrder() ) );
   }

   /** True if the npix at the deepest order is in the MOC */
   public boolean contains(long npix) { return range.contains(npix); }

   
   /********************************************  Prototypes *******************************************************/
   
   public void accretion() throws Exception { accretion(getMocOrder()); }
   
   public void accretion(int order) throws Exception {
      SMoc m = dup();
      int n=0;
      int o=-1;
      HealpixNested h = null;
      
      Iterator<MocCell> it = iterator();
      while( it.hasNext() ) {
         MocCell p = it.next();
         if( o!=p.order ) { h = cds.healpix.Healpix.getNested(p.order); o=p.order; }
         long neibs[] = Healpix.externalNeighbours(h,p.order,p.start, order);
         
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
               
               // EN ATTENDANT DE REIMPLANTE isIn(...)
//               if( nside>2 && i%(nside+1)==1 ) {
//                  if( isIn(p.order,neib>>>shift) ) i+=(nside-1);
//               }
               continue;
            }
            m.add(order,neib);
         }
      }
      m.clone1(this);
   }   

   /***************************************************** Operations ******************************************/
   
   /** Return true if the moc is compatible for operation (same coosys) */
   private boolean isCompatible( SMoc moc ) {
      return moc.altCoosys==altCoosys || moc.altCoosys!=null && moc.altCoosys.equals(altCoosys);
   }
   
   /** Return the Union with another Moc */
   public SMoc union(Moc moc) throws Exception {
      if( moc instanceof STMoc ) moc = ((STMoc)moc).getSpaceMoc();
      else if( !(moc instanceof SMoc ) ) throw new Exception("Uncompatible Moc type for SMoc union");
      if( !((SMoc)moc).isCompatible( this ) ) throw new Exception("Uncompatible coosys");
      return (SMoc) super.union(moc);
   }

   /** Return the Intersection with another Moc */
   public SMoc intersection(Moc moc) throws Exception {
      if( moc instanceof STMoc ) moc = ((STMoc)moc).getSpaceMoc();
      else if( !(moc instanceof SMoc ) ) throw new Exception("Uncompatible Moc type for SMoc subtraction");
      if( !((SMoc)moc).isCompatible( this ) ) throw new Exception("Uncompatible coosys");
      return (SMoc) super.intersection(moc);
   }

   /** Return the subtraction with another Moc */
   public SMoc subtraction(Moc moc) throws Exception {
      if( moc instanceof STMoc ) moc = ((STMoc)moc).getSpaceMoc();
      else if( !(moc instanceof SMoc ) ) throw new Exception("Uncompatible Moc type for SMoc subtraction");
      if( !((SMoc)moc).isCompatible( this ) ) throw new Exception("Uncompatible coosys");
      return (SMoc) super.subtraction(moc);
   }
   
   /** Return the complement */
   public SMoc complement() throws Exception { return (SMoc) super.complement(); }

   
   /*************************************************************** I/O *****************************************************/
   
   /** Internal method: read FITS data according to the type of MOC.
    * @param in The input stream
    * @param naxis1 size of FITS row (in bytes) (generally ==nbyte, but may be 1024 for buffering)
    * @param naxis2 number of values
    * @param nbyte size of each value (in bytes)
    * @param header HDU1 header
    * @throws Exception
    */
   protected  void readSpecificData(InputStream in, int naxis1, int naxis2, int nbyte, HeaderFits header) throws Exception {
      
      // Moc order detection
      int mocOrder;
      String val = header.getStringFromHeader("MOCORD_S");
      if( val==null ) val = header.getStringFromHeader("MOCORDER");   // Compatibility with MOC 1.0 and 1.1
      try { mocOrder = Integer.parseInt(val); }
      catch( Exception e ) { throw new Exception("Missing MOC order in FITS header (MOCORD_S)"); }
      setMocOrder( mocOrder );
      
      String type = header.getStringFromHeader("ORDERING");

      // We anticipate the SMOC coded in RANGE (not possible in IVOA M0C 2.0) => Range coding
      if( type!=null && type.equals("RANGE") ) readSpecificDataRange(in, naxis1, naxis2, nbyte);

      // Regular SMOC coding => NUNIQ
      else readSpecificDataUniq(in, naxis1, naxis2, nbyte);
      

   }

   /** Write specifical SMOC properties */
   protected int writeSpecificFitsProp( OutputStream out ) throws Exception {
      int n=0;
      out.write( getFitsLine("TTYPE1","UNIQ","UNIQ pixel number") );        n+=80;
      out.write( getFitsLine("ORDERING","NUNIQ","NUNIQ coding method") );   n+=80;      
      out.write( getFitsLine("COORDSYS",getSys(),"Reference frame (C=ICRS)") );  n+=80;
      out.write( getFitsLine("MOCDIM","SPACE","Physical dimension") );      n+=80;      
      out.write( getFitsLine("MOCORD_S",""+getMocOrder(),"MOC resolution (best order)") );        n+=80;      
      out.write( getFitsLine("MOCORDER",""+getMocOrder(),"=MOCORD_S (backward compatibility)") );    n+=80;      
      return n;
   }

   /** Write SMOC data */
   protected int writeSpecificData(OutputStream out) throws Exception {
      int size = 0;
      byte [] buf = new byte[ sizeOfCoding() ];
      Iterator<MocCell> it = cellIterator( true );
      while( it.hasNext() ) {
         MocCell cell = it.next();
         for( long val=cell.start; val<cell.end; val++ ) {
            long nuniq = hpix2uniq(cell.order, val );
            size+=writeVal(out,nuniq,buf);
         }
      }
      return size;
   }
   

}
