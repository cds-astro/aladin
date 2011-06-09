// Copyright 2011 - UDS/CNRS
// This Healpix MOC java class is distributed under the terms
// of the GNU General Public License version 3.
//

package cds.moc;

import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import cds.moc.operations.Intersection;
import cds.moc.operations.Operation;
import cds.moc.operations.Union;

/** HEALPix Multi Order Coverage Map (MOC)
 * This object provides read, write and process methods to manipulate an HEALPix Multi Order Coverage Map (MOC)
 * A MOC is used to define a sky region by using HEALPix sky tesselation
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 June 2011 - first stable version
 * @version 0.9 May 2011 - creation
 */
public class HealpixMoc {

   /** Coord is out of the MOC */
   static final public int OUT = 0;
   /** Coord is in the MOC */
   static final public int IN = 1;
   /** Coord is at the border of the MOC, may be in, may be out */
   static final public int INBORDER= 2;

   /** ASCII encoding format */
   static public final int ASCII = 0;
   /** FITS encoding format */
   static public final int FITS  = 1;

   private static String CR = System.getProperty("line.separator");
   static final public String SIGNATURE = "HPXMOC";   // FITS keywords used as signature

   private int blocSize = 128;
   private boolean testUnicity = false; // true for checking the unicity during a MOC pixel addition (=> slower)
   public LongArray [] level;           // Contient les listes de pixels pour chaque ordre (indice du tableau)
   private int nOrder;                  // Le nombre courant d'orders utilisés
   private int currentOrder=-1;         // dernier ordre connu pour l'insertion de nouveaux pixels

   /** HEALPix Multi Order Coverage Map (MOC) creation */
   public HealpixMoc() { this(128); }

   /** HEALPix Multi Order Coverage Map (MOC) creation and initialisation
    * via a string (ex: "order1/npix1-npix2 npix3 ... order2/npix4 ...")
    * @param s list of MOC pixels
    */
   public HealpixMoc(String s) {
      this(128);
      add(s);
   }

   /** HEALPix Multi Order Coverage Map (MOC) creation and initialisation
    * via a stream, either in ASCII encoded format or in FITS encoded format
    * @param in input stream
    * @param mode ASCII - ASCII encoded format, FITS - Fits encoded format
    */
   public HealpixMoc(InputStream in, int mode) throws Exception {
      this(128);
      read(in,mode);
   }

   // Internal creation => blocsize for longArray[] increment
   private HealpixMoc(int blocSize) {
      init(blocSize);
   }

   /** Clear the MOC */
   public void clear() {
      init(blocSize);
   }

   // Internal initialisations => array of levels allocation
   private void init(int blocSize) {
      this.blocSize = blocSize;
      level = new LongArray[Healpix.MAXORDER];
      for( int i=0; i<level.length; i++) level[i] = new LongArray(blocSize);
   }

   /** Provide the number of Healpix pixels (for all MOC orders)
    * @return number of pixels
    */
   public int getSize() {
      int size=0;
      for( int order=0; order<nOrder; order++ ) size+=level[order].getSize();
      return size;
   }

   /** Provide the last order of the MOC, or -1 */
   public int getLastOrder() { return nOrder; }

   /** Provide the angular resolution of the MOC (sqrt of the smallest pixel area) */
   public double getAngularRes() {
      return Math.sqrt( Healpix.getPixelArea( getMaxOrder() ) );
   }

   /** Provide the greatest order used by the MOC
    * @return greatest MOC order, -1 if no order used
    */
   public int getMaxOrder() { return nOrder-1; }

   /**
    * Set the check unicity flag.
    * "flase" by default => there is no doublon check during addition (=> faster)
    * @param flag
    */
   public void setCheckUnicity(boolean flag) { testUnicity=flag; }

   /** Add a MOC pixel
    * @param order HEALPix order
    * @param npix Healpix number
    * @see setCheckUnicity(..)
    */
   public void add(int order, long npix) {
      if( order>=nOrder ) nOrder=order+1;
      level[order].add(npix,testUnicity);
   }

   /** Add a list of MOC pixels provided in a string format
    * (ex: "order1/npix1-npix2 npix3 ... order2/npix4 ...")
    * @see setCheckUnicity(..)
    */
   public void add(String s) {
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t");
      while( st.hasMoreTokens() ) addHpix(st.nextToken());
   }

   /**
    * Performs the union between the MOC references by the current object and another MOC,
    * at the nside of the current object
    * The two MOCs do not need to be at the same deepest nside
    *
    * @param moc the MOC to perform the union with
    *
    * @return a new MOC, result of the union of the current object with moc
    *
    */
   public HealpixMoc union(HealpixMoc moc) {
       Operation union = new Union(this, moc, (int)Math.pow(2, this.getMaxOrder()));
       return union.compute();
   }

   /**
    * Performs the intersection between the MOC references by the current object and another MOC,
    * at the nside of the current object
    * The two MOCs do not need to be at the same deepest nside
    *
    * @param moc the MOC to perform the union with
    *
    * @return a new MOC, result of the union of the current object with moc
    *
    */
   public HealpixMoc intersection(HealpixMoc moc) {
       Operation intersection = new Intersection(this, moc, (int)Math.pow(2, this.getMaxOrder()));
       return intersection.compute();
   }

   /** Check if the galactic coord is inside the MOC.
    * If the position is at the MOC border, the method return MAYBE
    * @param lon galactic longitude
    * @param lat galactic latitude
    * @return IN, OUT or MAYBE
    * @throws Exception
    */
   public int contains(double lon, double lat) throws Exception {
      int order = getMaxOrder();
      if( order==-1 ) return OUT;
      long npix = Healpix.ang2pix(order, lon, lat);
      if( level[ order ].find( npix )>=0 ) return INBORDER;
      if( isDescendant(order,npix) ) return IN;
      return OUT;
   }

   /** true is the MOC pixel is an ascendant */
   public boolean isAscendant(int order, long npix) {
      long range=4L;
      for( int o=order+1; o<nOrder; o++,range*=4L ) {
         for( int i=level[o].getSize()-1; i>=0; i--) {
            long pix = level[o].val[i];
            if( npix*range<=pix && pix< (npix+1)*range ) return true;
         }
      }
      return false;
   }

   /** true if the MOC pixel is a descendant */
   public boolean isDescendant(int order, long npix) {
      long pix=npix/4L;
      for( int o=order-1; o>=0; o--,pix/=4L ) {
         for( int i=level[o].getSize()-1; i>=0; i--) {
            if( pix==level[o].val[i] ) return true;
         }
      }
      return false;
   }

   /** true if the MOC pixel is present at this order */
   public boolean isIn(int order, long npix) {
      return level[order].find(npix)>=0;
   }

   public long [] getPixLevel(int order) {
      long [] lev = new long[level[order].getSize()];
      System.arraycopy(level[order], 0, lev, 0, level[order].getSize());
      return lev;
   }

   /** Provide an Iterator on the MOC pixel List. Each Item is a couple of longs,
    * the first long is the order, the second long is the pixel number
    */
   public Iterator<long []> iterator() { return new HpixListIterator(); }

   /** Read HEALPix MOC from a file.
    * @param filename file name
    * @param mode ASCII encoded format or FITS encoded format
    * @throws Exception
    */
   public void read(String filename,int mode) throws Exception {
      File f = new File(filename);
      FileInputStream fi = new FileInputStream(f);
      read(fi,mode);
      fi.close();
   }

   /** Read HEALPix MOC from a stream.
    * @param in input stream
    * @param mode ASCII encoded format or FITS encoded format
    * @throws Exception
    */
   public void read(InputStream in,int mode) throws Exception {
      if( mode==FITS ) readFITS(in);
      else readASCII(in);
   }

   /** Read MOC from an ASCII stream
    *    ORDER|NSIDE=xxx1
    *    nn1
    *    nn2-nn3 nn4
    *    ...
    *    NSIDE|ORDER=xxx2
    *    ...
    *
    * @param in input stream
    * @throws Exception
    */
   public void readASCII(InputStream in) throws Exception {
      BufferedReader dis = new BufferedReader(new InputStreamReader(in));
      clear();
      String s;
      while( (s=dis.readLine())!=null ) {
         if( s.length()==0 ) continue;
         parseASCIILine(s);
      }
   }

   /** Read HEALPix MOC from an Binary FITS stream */
   public void readFITS(InputStream in) throws Exception {
      HeaderFits header = new HeaderFits();
      header.readHeader(in);
      String signature = header.getStringFromHeader(SIGNATURE);
      if( signature==null ) signature = header.getStringFromHeader(SIGNATURE+"M");
      if( signature==null ) throw new Exception("Not an HEALPix Multi-Level Fits map ("+SIGNATURE+" not found)");

      clear();
      try {
         header.readHeader(in);
         int naxis1 = header.getIntFromHeader("NAXIS1");
         int naxis2 = header.getIntFromHeader("NAXIS2");
         String tform = header.getStringFromHeader("TFORM1");
//         String numbering = header.getStringFromHeader("ORDERING");
//         if( !numbering.equals("UNIQ") ) throw new Exception("Healpix MOC support only UNIQ ordering");
         int nbyte= tform.indexOf('K')>=0 ? 8 : tform.indexOf('J')>=0 ? 4 : -1;   // entier 64 bits, sinon 32
         if( nbyte<=0 ) throw new Exception("HEALPix Multi Order Coverage Map only requieres integers (32bits or 64bits)");
         byte [] buf = new byte[naxis1*naxis2];
         readFully(in,buf);
         createUniq((naxis1*naxis2)/nbyte,nbyte,buf);
      } catch( EOFException e ) { }
   }

   /** Write HEALPix MOC to a file
    * @param filename name of file
    * @param mode encoded format (ASCII or FITS)
    */
   public void write(String filename,int mode) throws Exception {
      if( mode!=FITS && mode!=ASCII ) throw new Exception("Unknown encoded format ("+mode+")");
      File f = new File(filename);
      if( f.exists() ) f.delete();
      FileOutputStream fo = new FileOutputStream(f);
      if( mode==FITS ) writeFITS(fo);
      else writeASCII(fo);
      fo.close();
   }

   /** Write HEALPix MOC to an output stream
    * @param out output stream
    * @param mode encoded format (ASCII or FITS)
    */
   public void write(OutputStream out,int mode) throws Exception {
      if( mode!=FITS && mode!=ASCII ) throw new Exception("Unknown encoded format ("+mode+")");
      if( mode==FITS ) writeFITS(out);
      else writeASCII(out);
   }

   /** Write HEALPix MOC to an output stream IN ASCII encoded format
    * @param out output stream
    */
   public void writeASCII(OutputStream out) throws Exception {
      out.write(("#"+SIGNATURE+CR).getBytes());
      StringBuffer s = new StringBuffer(2048);
      for( int order=0; order<nOrder; order++) {
         int n = level[order].getSize();
         if( n==0 ) continue;
         s.append(CR+"ORDER="+order+CR);
         int j=0;
         for( int i=0; i<n; i++ ) {
            s.append( (j>0?" ":"") + level[order].val[i]);
            j++;
            if( j==10 ) { writeASCIIFlush(out,s); j=0; }
         }
         if( j!=0 ) writeASCIIFlush(out,s);
      }
   }

   /** Write HEALPix MOC to an output stream in FITS encoded format
    * @param out output stream
    */
   public void writeFITS(OutputStream out) throws Exception { writeFITS(out,true); }
   public void writeFITS(OutputStream out,boolean compressed) throws Exception {
      writeHeader0(out);
      int nbytes=nOrder<12 ? 4 : 8;  // Codage sur des integers ou des longs
      writeHeader1(out,nbytes,compressed);
      writeData(out,nbytes,compressed);
   }

   /*********************************************** Private methods  *****************************************/

   // Provide number of pixel value, using range compression mode
   private int getSizeCompressed() {
      int size=0;
      for( int order=0; order<nOrder; order++ ) size+=level[order].getSizeCompressed();
      return size;
   }

   // Ajout d'un pixel selon le format "order/npix[-npixn]".
   // Si l'order n'est pas mentionné, utilise le dernier order utilisé
   private void addHpix(String s) {
      int i=s.indexOf('/');
      if( i>0 ) currentOrder = Integer.parseInt(s.substring(0,i));
      int j=s.indexOf('-',i+1);
      if( j<0 ) add(currentOrder, Integer.parseInt(s.substring(i+1)));
      else {
         int startIndex = Integer.parseInt(s.substring(i+1,j));
         int endIndex = Integer.parseInt(s.substring(j+1));
         for( int k=startIndex; k<=endIndex; k++ ) add(currentOrder, k);
      }
   }

   // Parsing de la ligne NSIDE=nnn et positionnement de l'ordre courant correspondant
   private void setCurrentParseOrder(String s) throws Exception {
      int i = s.indexOf('=');
      try {
         currentOrder=(int)Healpix.log2(Long.parseLong(s.substring(i+1)));
      } catch( Exception e ) {
         throw new Exception("HpixList.setNside: syntax error ["+s+"]");
      }
   }

   // Parsing de la ligne ORDERING=NESTED|RING et positionnement de la numérotation correspondante
   // ou Parsing de la ligne ORDER=xxx et positionnement de l'ordre correspondant
   private void setOrder(String s) throws Exception {
      int i = s.indexOf('=');

      // C'est ORDER=nnn
      if( s.charAt(i-1)=='R' ) {
         try {
            currentOrder=Integer.parseInt(s.substring(i+1));
         } catch( Exception e ) {
            throw new Exception("HpixList.setOrder: syntax error ["+s+"]");
         }
         return;
      }

      // C'est ORDERING=NESTED|RING => ignoré
   }

   // Parse une ligne d'un flux
   private void parseASCIILine(String s) throws Exception {
      char a = s.charAt(0);
      if( a=='#' ) return;
      if( a>='0' && a<='9' ) {
         // Un par ligne
         if( s.indexOf(' ')<0 && s.indexOf(',')<0 ) addHpix(s);

         // Plusieurs par ligne
         else {
            StringTokenizer tok = new StringTokenizer(s,", ");
            while( tok.hasMoreTokens() ) addHpix(tok.nextToken());
         }
      }
      else if( a=='N' ) setCurrentParseOrder(s);
      else if( a=='O' ) setOrder(s);
   }

   public void createUniq(int nval,int nbyte,byte [] t) {
      int i=0;
      long [] hpix = null;
      long oval=-1;
      for( int k=0; k<nval; k++ ) {
         long val=0;

         int a =   ((t[i])<<24) | (((t[i+1])&0xFF)<<16) | (((t[i+2])&0xFF)<<8) | (t[i+3])&0xFF;
         if( nbyte==4 ) val = a;
         else {
            int b = ((t[i+4])<<24) | (((t[i+5])&0xFF)<<16) | (((t[i+6])&0xFF)<<8) | (t[i+7])&0xFF;
            val = (((long)a)<<32) | ((b)& 0xFFFFFFFFL);
         }
         i+=nbyte;

         long min = val;
         if( val<0 ) { min = oval+1; val=-val; }
         for( long v = min ; v<=val; v++) {
            hpix = Healpix.uniq2hpix(v,hpix);
            int order = (int)hpix[0];
            add( order, hpix[1]);
         }
         oval=val;
      }
   }

   private void writeASCIIFlush(OutputStream out, StringBuffer s) throws Exception {
      s.append(CR);
      out.write(s.toString().getBytes());
      s.delete(0,s.length());
   }

   // Write the primary FITS Header
   private void writeHeader0(OutputStream out) throws Exception {
      int n=0;
      out.write( getFitsLine("SIMPLE","T","Written by Aladin") ); n+=80;
      out.write( getFitsLine("BITPIX","8") ); n+=80;
      out.write( getFitsLine("NAXIS","0") );  n+=80;
      out.write( getFitsLine(SIGNATURE,""+getMaxOrder(),"HEALPix MultiOrder Coverage map (max order)") ); n+=80;
      out.write( getFitsLine("EXTEND","T") ); n+=80;
      out.write( getEndBourrage(n) );
   }

   // Write the FITS HDU Header for the UNIQ binary table
   private void writeHeader1(OutputStream out,int nbytes,boolean compressed) throws Exception {
      int n=0;
      int naxis2 = (compressed ? getSizeCompressed() : getSize());
      out.write( getFitsLine("XTENSION","BINTABLE","HEALPix coverage map") ); n+=80;
      out.write( getFitsLine("BITPIX","8") ); n+=80;
      out.write( getFitsLine("NAXIS","2") );  n+=80;
      out.write( getFitsLine("NAXIS1",nbytes+"") );  n+=80;
      out.write( getFitsLine("NAXIS2",""+naxis2 ) );  n+=80;
      out.write( getFitsLine("PCOUNT","0") ); n+=80;
      out.write( getFitsLine("GCOUNT","1") ); n+=80;
      out.write( getFitsLine("TFIELDS","1") ); n+=80;
      out.write( getFitsLine("TFORM1",nbytes==4 ? "1J" : "1K") ); n+=80;
      out.write( getFitsLine("TTYPE1","PIXEL","Pixel HEAPix UNIQ number") ); n+=80;
      out.write( getFitsLine("OBS_NPIX",""+getSize(),"Number of pixels") ); n+=80;
      out.write( getFitsLine("ORDERING","NUNIQ","Merging NSIDE and NESTEDPIX") ); n+=80;
      out.write( getFitsLine("COORDSYS","G","Only G (galactic) is allowed") ); n+=80;
      out.write( getEndBourrage(n) );
   }

   // Write the UNIQ FITS HDU Data
   private void writeData(OutputStream out,int nbytes,boolean compressed) throws Exception {
      if( compressed ) writeDataCompressed(out,nbytes);
      else writeData(out,nbytes);
   }

   // Write the UNIQ FITS HDU Data in basic mode
   private void writeData(OutputStream out,int nbytes) throws Exception {
      byte [] buf = new byte[nbytes];
      int size = 0;
      for( int order=0; order<nOrder; order++ ) {
         int n = level[order].getSize();
         if( n==0 ) continue;
         for( int i=0; i<n; i++) {
            long val = Healpix.hpix2uniq(order, level[order].val[i]);
            size+=writeVal(out,val,buf);
         }
      }
      out.write( getBourrage(size) );
   }

   // Write the UNIQ FITS HDU Data in compressed mode
   private void writeDataCompressed(OutputStream out,int nbytes) throws Exception {
      byte [] buf = new byte[nbytes];
      int size = 0;
      long oval=-1,min=-1,max=-1;
      for( int order=0; order<nOrder; order++ ) {
         int n = level[order].getSize();
         if( n==0 ) continue;
         for( int i=0; i<n; i++) {
            long val = Healpix.hpix2uniq(order, level[order].val[i]);
            if( val != oval+1 ) {
               if( min!=-1 ) size+=writeVal(out,min,buf);
               if( max!=-1 ) size+=writeVal(out, min+1==max ? max:-max ,buf);
               min=val;
               max=-1;

            } else max=val;
            oval=val;
         }
         if( min!=-1 ) size+=writeVal(out,min,buf);
         if( max!=-1 ) size+=writeVal(out,min+1==max ? max:-max,buf);
      }
      out.write( getBourrage(size) );
   }

   private int writeVal(OutputStream out,long val,byte []buf) throws Exception {
      for( int j=0,shift=(buf.length-1)*8; j<buf.length; j++, shift-=8 ) buf[j] = (byte)( 0xFF & (val>>shift) );
      out.write( buf );
      return buf.length;
   }

   // Juste pour du debogage
   public String toString() {
      StringBuffer s = new StringBuffer();
      for( int order=0; order<nOrder; order++ ) {
         int n = Math.min(10, level[order].getSize());
         if( n==0 ) continue;
         s.append(order+"/");
         for( int j=0; j<n; j++ ) s.append( (j==0?"":" ") + level[order].val[j] );
         s.append( n<level[order].getSize() ? "...\n":"\n" );
      }
      return s.toString();
   }


   /***************************************  Les classes privées **************************************/

   // Création d'un itérator sur la liste des pixels
   private class HpixListIterator implements Iterator<long []> {
      private int currentOrder=0;
      private int indice=-1;
      private boolean ready=false;

      public boolean hasNext() {
         goNext();
         return currentOrder<nOrder;
       }

      public long [] next() {
         if( !hasNext() ) return null;
         ready=false;
         return new long[]{currentOrder,level[currentOrder].val[indice]};
      }

      public void remove() {  }

      private void goNext() {
         if( ready ) return;
         for( indice++; currentOrder<nOrder && indice>=level[currentOrder].getSize(); currentOrder++, indice=0);
         ready=true;
      }
   }

   // Tableau extensibles de longs
   private class LongArray {
      private long [] val=null;     // Le tableau des valeurs
      private int size=0;           // La taille utilisée
      private int sizeBloc=128;     // la taille des blocs d'augmentation de la taille du tableau

      LongArray(int initSize) {
         adjustSize(sizeBloc=initSize);
      }

      // Retourne la taille utilisée
      int getSize() { return size; }

      // Retourne la taille utilisée si on compresse les données par la méthode des intervalles
      int getSizeCompressed() {
         int reduce=0;
         boolean oConsecutif=false;
         for( int i=1; i<size; i++ ) {
            boolean consecutif= ( val[i]==val[i-1] +1 );
            if( oConsecutif && consecutif ) reduce++;
            oConsecutif=consecutif;
         }
         return size-reduce;
      }

      // Ajout d'une valeur à la fin du tableau si valeur non encore présente
      void add(long v, boolean testUnicity) {
         if( testUnicity && find(v)>=0 ) return;
         adjustSize(1);
         val[size++]=v;
      }

      // Retourne la position d'une valeur dans le tableau, -1 si non trouvé
      int find(long v) {
         for( int i=0; i<size; i++ ) if( val[i]==v ) return i;
         return -1;
      }

      // Ajuste la taille du tableau si n valeurs ne peuvent être insérées
      // sans modifier la capacité actuelle
      // => procède alors à une recopie
      private void adjustSize(int n) {
         if( val!=null && size+n < val.length ) return;
         long [] nval = new long[ (1+ (size+n)/sizeBloc) * sizeBloc ];
         if( val!=null ) System.arraycopy(val, 0, nval, 0, size);
         val=nval;
         nval=null;
      }
   }

   /****************** Utilitaire Fits **************************/

   /** Generate FITS 80 character line => see getFitsLine(String key, String value, String comment) */
   private byte [] getFitsLine(String key, String value) {
      return getFitsLine(key,value,null);
   }

   /**
    * Generate FITS 80 character line.
    * @param key The FITS key
    * @param value The associated FITS value (can be numeric, string (quoted or not)
    * @param comment The commend, or null
    * @return the 80 character FITS line
    */
   private byte [] getFitsLine(String key, String value, String comment) {
      int i=0,j;
      char [] a;
      byte [] b = new byte[80];

      // The keyword
      a = key.toCharArray();
      for( j=0; i<8; j++,i++) b[i]=(byte)( (j<a.length)?a[j]:' ' );

      // The associated value
      if( value!=null ) {
         b[i++]=(byte)'='; b[i++]=(byte)' ';

         a = value.toCharArray();

         // Numeric value => right align
         if( !isFitsString(value) ) {
            for( j=0; j<20-a.length; j++)  b[i++]=(byte)' ';
            for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];

            // string => format
         } else {
            a = formatFitsString(a);
            for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];
            while( i<30 ) b[i++]=(byte)' ';
         }
      }

      // The comment
      if( comment!=null && comment.length()>0 ) {
         if( value!=null ) { b[i++]=(byte)' ';b[i++]=(byte)'/'; b[i++]=(byte)' '; }
         a = comment.toCharArray();
         for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte) a[j];
      }

      // Bourrage
      while( i<80 ) b[i++]=(byte)' ';

      return b;
   }

   /** Generate the end of a FITS block assuming a current block size of headSize bytes
    * => insert the last END keyword */
   private byte [] getEndBourrage(int headSize) {
      int size = 2880 - headSize%2880;
      if( size<3 ) size+=2880;
      byte [] b = new byte[size];
      b[0]=(byte)'E'; b[1]=(byte)'N';b[2]=(byte)'D';
      for( int i=3; i<b.length; i++ ) b[i]=(byte)' ';
      return b;
   }

   /** Generate the end of a FITS block assuming a current block size of headSize bytes */
   private byte [] getBourrage(int currentPos) {
      int size = 2880 - currentPos%2880;
      byte [] b = new byte[size];
      return b;
   }

   /** Fully read buf.length bytes from in input stream */
   private void readFully(InputStream in, byte buf[]) throws IOException {
      readFully(in,buf,0,buf.length);
   }

   /** Fully read len bytes from in input stream and store the result in buf[]
    * from offset position. */
   private void readFully(InputStream in,byte buf[],int offset, int len) throws IOException {
      int m;
      for( int n=0; n<len; n+=m ) {
         m = in.read(buf,offset+n,(len-n)<512 ? len-n : 512);
         if( m==-1 ) throw new EOFException();
      }
   }

   /**
    * Test si c'est une chaine à la FITS (ni numérique, ni booléen)
    * @param s la chaine à tester
    * @return true si s est une chaine ni numérique, ni booléenne
    * ATTENTION: NE PREND PAS EN COMPTE LES NOMBRES IMAGINAIRES
    */
   private boolean isFitsString(String s) {
      if( s.length()==0 ) return true;
      char c = s.charAt(0);
      if( s.length()==1 && (c=='T' || c=='F') ) return false;   // boolean
      if( !Character.isDigit(c) && c!='.' && c!='-' && c!='+' ) return true;
      try {
         Double.valueOf(s);
         return false;
      } catch( Exception e ) { return true; }
   }

   private char [] formatFitsString(char [] a) {
      if( a.length==0 ) return a;
      StringBuffer s = new StringBuffer();
      int i;
      boolean flagQuote = a[0]=='\''; // Chaine déjà quotée ?

      s.append('\'');

      // recopie sans les quotes
      for( i= flagQuote ? 1:0; i<a.length- (flagQuote ? 1:0); i++ ) {
         if( !flagQuote && a[i]=='\'' ) s.append('\'');  // Double quotage
         s.append(a[i]);
      }

      // bourrage de blanc si <8 caractères + 1ère quote
      for( ; i< (flagQuote ? 9:8); i++ ) s.append(' ');

      // ajout de la dernière quote
      s.append('\'');

      return s.toString().toCharArray();
   }


   /** Manage Header Fits */
   class HeaderFits {

      private Hashtable<String,String> header;     // List of header key/value
      private int sizeHeader=0;                    // Header size in bytes

      /** Pick up FITS value from a 80 character array
       * @param buffer line buffer
       * @return Parsed FITS value
       */
      private String getValue(byte [] buffer) {
         int i;
         boolean quote = false;
         boolean blanc=true;
         int offset = 9;

         for( i=offset ; i<80; i++ ) {
            if( !quote ) {
               if( buffer[i]==(byte)'/' ) break;   // on the comment
            } else {
               if( buffer[i]==(byte)'\'') break;   // on the next quote
            }

            if( blanc ) {
               if( buffer[i]!=(byte)' ' ) blanc=false;
               if( buffer[i]==(byte)'\'' ) { quote=true; offset=i+1; }
            }
         }
         return (new String(buffer, 0, offset, i-offset)).trim();
      }

      /** Pick up FITS key from a 80 character array
       * @param buffer line buffer
       * @return Parsed key value
       */
      private String getKey(byte [] buffer) {
         return new String(buffer, 0, 0, 8).trim();
      }

      /** Parse FITS header from a stream until next 2880 FITS block after the END key.
       * Memorize FITS key/value couples
       * @param dis input stream
       */
      private void readHeader(InputStream dis) throws Exception {
         int blocksize = 2880;
         int fieldsize = 80;
         String key, value;
         int linesRead = 0;
         sizeHeader=0;

         header = new Hashtable<String,String>(200);
         byte[] buffer = new byte[fieldsize];

         while (true) {
            readFully(dis,buffer);
            key =  getKey(buffer);
            if( linesRead==0 && !key.equals("SIMPLE") && !key.equals("XTENSION") ) throw new Exception();
            sizeHeader+=fieldsize;
            linesRead++;
            if( key.equals("END" ) ) break;
            if( buffer[8] != '=' ) continue;
            value=getValue(buffer);
            header.put(key, value);
         }

         // Skip end of last block
         int bourrage = blocksize - sizeHeader%blocksize;
         if( bourrage!=blocksize ) {
            byte [] tmp = new byte[bourrage];
            readFully(dis,tmp);
            sizeHeader+=bourrage;
         }
      }

      /**
       * Provide integer value associated to a FITS key
       * @param key FITs key (with or without trailing blanks)
       * @return corresponding integer value
       */
      private int getIntFromHeader(String key) throws NumberFormatException,NullPointerException {
         String s = header.get(key.trim());
         return (int)Double.parseDouble(s.trim());
      }

      /**
       * Provide string value associated to a FITS key
       * @param key FITs key (with or without trailing blanks)
       * @return corresponding string value without quotes (')
       */
      private String getStringFromHeader(String key) throws NullPointerException {
         String s = header.get(key.trim());
         if( s==null || s.length()==0 ) return s;
         if( s.charAt(0)=='\'' ) return s.substring(1,s.length()-1).trim();
         return s;
      }
   }
}
