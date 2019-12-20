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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.StringTokenizer;

/** HEALPix Multi Order Coverage Map (MOC) IO routines
 * Compliante with IVOA MOC Rec 1.0 June 2014
 *
 * Example : SpaceMoc moc = new SpaceMoc();
 *           (new IO(moc).read(String filename);
 *
 * @author Pierre Fernique [CDS]
 * @version 1.5 Mar 2018 - TMOC patch
 * @version 1.4 Nov 2015 - JSON #MOCORDER patching
 * @version 1.3 Sep 2013 - WD 1.0 10/9/2013 compliante
 * @version 1.2 Avr 2012 - JSON ASCII support
 * @version 1.1 Mar 2012 - Healpix FITS header adjustements
 * @version 1.0 Oct 2011 - Dedicated class (removed from SpaceMoc)
 */
public final class MocIO {

   static public final int FITS = SpaceMoc.FITS;         // Standard format
   static public final int ASCII = SpaceMoc.ASCII;       // ASCII format
   static public final int JSON = SpaceMoc.JSON;         // JSON format (suggested in IVOA REC)
   
   static public final int JSON0 = SpaceMoc.JSON0;       // JSON obsolete format (only reading supported for compatibility)

   static private String CR = System.getProperty("line.separator");

   /** Standardized MOC FITS keyword list */
   private String [][] FITSKEY = {
         {"COORDSYS","Reference frame"},
         {"MOCORDER","MOC resolution (best order)"},
         {"MOCTOOL", "Name of the MOC generator"},
         {"MOCTYPE", "Source type (IMAGE or CATALOG)"},
         {"MOCID",   "Identifier of the collection"},
         {"ORIGIN",  "MOC origin"},
         {"DATE",    "MOC creation date"},
         {"EXTNAME", "MOC name"}
   };

   static final public String OLDSIGNATURE = "HPXMOC";   // FITS keywords used as signature (obsoleted syntax)
   static final public String SIGNATURE = "MOCORDER";    // FITS keywords used as signature

   private Moc moc;
   private byte firstChar=0; // Use for storing first character (mode detection)

   public MocIO(Moc m) { moc=m; }

   /** Read HEALPix MOC from a file.
    * Support standard FITS format and ASCII non standard alternatives (JSON, ASCII)
    * @param filename file name
    * @throws Exception
    */
   public void read(String filename) throws Exception {
      File f = new File(filename);
      FileInputStream fi = null;
      BufferedInputStream bf = null;
      try {
         fi = new FileInputStream(f);
         bf = new BufferedInputStream(fi);
         read( bf );
      } finally {
         if( bf!=null ) bf.close();
         else if( fi!=null ) fi.close();
      }
   }

   /** Read HEALPix MOC from a file.
    * @param filename file name
    * @param mode MOC format:  FITS, JSON, ASCII
    * @throws Exception
    * @deprecated see read(String)
    */
   public void read(String filename,int mode) throws Exception {
      read(filename);
   }

   /** Read HEALPix MOC from a stream.
    * Support standard FITS format and ASCII non standard alternatives (JSON, ASCII)
    * @param in input stream
    * @throws Exception
    */
   public void read(InputStream in) throws Exception {
      // Read the first charactere for deciding FITS or ASCII
      byte [] b = new byte[1];
      in.read(b);
      firstChar = b[0];
      int mode = firstChar=='S' ? FITS : ASCII;
      read(in,mode);
   }

   /** Read HEALPix MOC from a stream.
    * @param in input stream
    * @param mode MOC format:  FITS, JSON, ASCII
    * @throws Exception
    * @deprecated see read(InputStream)
    */
   public void read(InputStream in,int mode) throws Exception {
      if( mode==FITS ) readFits(in);
      else readASCII(in);
      moc.trim();
   }

   /** Read MOC from an ASCII stream
    * Support simultaneously various ASCII formats (OBSOLETE, ASCII & JSON)
    *
    * OBSOLETE
    *    #MOCORDER MAX
    *    ORDER|NSIDE=xxx1
    *    nn1
    *    nn2-nn3 nn4
    *    ...
    *    NSIDE|ORDER=xxx2
    *    ...
    *    
    * ASCII (with a first comment for providing the MOCORDER)
    *    #MOCORDER MAX
    *    ORDER1/nn1,nn2-nnX ORDER2/...
    *    
    * ASCII (with an explicit MAXORDER possibly empty list)
    *    ORDER1/nn1,nn2-nnX ORDER2/... MAXORDER/
    *    
    * JSON (with a first comment for providing the MOCORDER => Not well formed JSON)
    *    #MOCORDER MAX
    *    {"ORDER1":[nn1,nn2,...],
    *     "ORDER2":[nn
    *     ...
    *    }
    *    
    * JSON (with an explicit MAXORDER possibly empty list)
    *    #MOCORDER MAX
    *    {"ORDER1":[nn1,nn2,...],
    *     "ORDER2":[nn...]
    *     ...
    *     "MAXORDER":[]
    *    }
    *    
    * TMOC (in JD ranges)
    *    #TMOC
    *    jdmin jdmax jdmin jdmax...
    *
    * @param in input stream
    * @throws Exception
    */
   public void readASCII(InputStream in) throws Exception {
      BufferedReader dis = new BufferedReader(new InputStreamReader(in));
      moc.clear();
      moc.setCheckConsistencyFlag(false); // We assume that the input MOC is well formed
      String s;
      int mocOrder=SpaceMoc.MAXORDER;
      boolean flagTMoc = false;
      boolean flagMocOrder=false;
      for( int line=0; (s=dis.readLine())!=null; line++ ) {
         if( firstChar!=0 ) { s=(char)firstChar+s; firstChar=0; }
         if( s.length()==0 ) continue;
         if( line==0 ) {
            if( s.startsWith("#MOCORDER ") ) {
               String v = s.substring(10);
               if( v.length()>0 ) { flagMocOrder=true; moc.setProperty("MOCORDER", v); }
            } else if( s.startsWith("#TMOC") ) {
               String v = s.substring(5);
               try {
                  mocOrder = Integer.parseInt(v.trim());
                  if( mocOrder<29 ) flagMocOrder=true;
               } catch( Exception e ) {}
               flagTMoc=true;
               continue;
            }
         } 
         if( flagTMoc ) parseTMocASCIILine(s);
         else parseASCIILine(s);
      }
      
      // If the MocOrder is found by the content
      if( !flagMocOrder ) {
//         moc.setProperty("MORORDER", moc.getMaxOrder()+"" );
         moc.setMocOrder(moc.getMaxOrder());
      }
      
      if( flagTMoc ) {
         moc.toHealpixMoc();
         if( mocOrder!=29 ) moc.setMocOrder(mocOrder);
      }
   }
   
   // Parse une ligne d'un flux TMOC par intervalles JD en notation floattante
   private void parseTMocASCIILine(String s) throws Exception {
      StringTokenizer st = new StringTokenizer(s," ;,\n\r\t");
      while( st.hasMoreTokens() ) {
         String s1 = st.nextToken();
         String s2 = st.nextToken();
         ((TimeMoc)moc).add( Double.parseDouble(s1), Double.parseDouble(s2));
      }
   }
   
   /** Read HEALPix MOC from an Binary FITS stream */
   public void readFits(InputStream in) throws Exception {
      moc.clear();
      moc.setCheckConsistencyFlag(false); // We assume that the Moc is already consistent
      HeaderFits header = new HeaderFits();
      header.readHeader(in);

      //For compatibility
      String v = header.getStringFromHeader("HPXMOC");
      if( v!=null ) moc.setProperty("MOCORDER", v);

      try {
         header.readHeader(in);

         //For compatibility
         v = header.getStringFromHeader("HPXMOC");
         if( v!=null ) moc.setProperty("MOCORDER", v);

         for( int i=0; i<FITSKEY.length; i++ ) {
            String key = FITSKEY[i][0];
            String value = header.getStringFromHeader(key);
            if( value!=null ) moc.setProperty(key, value);
         }

         int naxis1 = header.getIntFromHeader("NAXIS1");
         int naxis2 = header.getIntFromHeader("NAXIS2");
         String tform = header.getStringFromHeader("TFORM1");
         int nbyte= tform.indexOf('K')>=0 ? 8 : tform.indexOf('J')>=0 ? 4 : -1;   // entier 64 bits, sinon 32
         if( nbyte<=0 ) throw new Exception("Multi Order Coverage Map only requieres integers (32bits or 64bits)");

         moc.readSpecificData( in, naxis1,naxis2, nbyte, header);
      } catch( EOFException e ) { }
   }

   /** Write HEALPix MOC to a file
    * @param filename name of file
    */
   public void write(String filename) throws Exception {
      write(filename,FITS);
   }

   /** Write HEALPix MOC to a file
    * @param filename name of file
    * @param mode encoded format (FITS or JSON)
    */
   public void write(String filename,int mode) throws Exception {
      if( mode!=FITS && mode!=ASCII && mode!=JSON ) throw new Exception("Unknown MOC format !");
      File f = new File(filename);
      if( f.exists() ) f.delete();
      FileOutputStream fo = null;
      BufferedOutputStream fb = null;

      try {
         fo = new FileOutputStream(f);
         fb = new BufferedOutputStream(fo);
         if( mode==FITS ) writeFits(fb);
         else if( mode==JSON  ) writeJSON(fb);
         else if( mode==ASCII ) writeASCII(fb);
      } finally {
         if( fb!=null ) fb.close();
         else if( fo!=null ) fo.close();
      }
   }

   /** Write HEALPix MOC to an output stream
    * @param out output stream
    */
   public void write(OutputStream out) throws Exception {
      write(out,FITS);
   }

   /** Write HEALPix MOC to an output stream
    * At the end, the stream is not closed
    * @param out output stream
    * @param mode encoded format (FITS or JSON)
    */
   public void write(OutputStream out,int mode) throws Exception {
      if( mode!=FITS && mode!=ASCII && mode!=JSON ) throw new Exception("Unknown MOC format !");
      if( mode==FITS ) writeFits(out);
      else if( mode==JSON ) writeJSON(out);
      else if( mode==ASCII ) writeASCII(out);
   }

   private void testMocNotNull() throws Exception {
      if( moc==null ) throw new Exception("No MOC assigned (use setMoc(SpaceMoc))");
   }

   private static final int MAXWORD=20;
   private static final int MAXSIZE=80;

   /** Write HEALPix MOC to an output stream IN ASCII encoded format
    * @param out output stream
    */
   public void writeASCII(OutputStream out) throws Exception {
      testMocNotNull();
      
//      if( moc instanceof SpaceTimeMoc ) {
//         moc.writeASCII(OutputStream out);
//         return;
//      }
      
//      out.write(("#"+SIGNATURE+" "+moc.getMocOrder()+CR).getBytes());
      StringBuilder res= new StringBuilder(moc.getSize()*8);
      int order=-1;
      boolean flagNL = moc.getSize()>MAXWORD;
      int sizeLine=0;
      int j=0;
      for( MocCell c : moc ) {
         if( res.length()>0 ) {
            if( c.order!=order ) {
               if( flagNL ) { res.append("\n"); sizeLine=0; j++; }
               else res.append(" ");
            } else {
               int n=(c.npix+"").length();
//               if( flagNL && n+sizeLine>MAXSIZE ) { res.append(",\n "); sizeLine=3; j++; }
//               else { res.append(','); sizeLine++; }
               if( flagNL && n+sizeLine>MAXSIZE ) { res.append("\n "); sizeLine=2; j++; }
               else { res.append(' '); sizeLine++; }
            }
            if( j>15) { writeASCIIFlush(out,res,false); j=0; }
         }
         String s = c.order!=order ?  c.order+"/"+c.npix : c.npix+"";
         res.append(s);
         sizeLine+=s.length();
         order=c.order;
      }
      
      // Ajout de la resolution max si nécessaire
      int mocOrder = moc.getMocOrder();
      if( order<mocOrder ) {
         StringBuilder s= new StringBuilder(10);
         if( flagNL && sizeLine!=2 ) { res.append('\n'); sizeLine=0; }
         else s.append(' ');
         s.append(mocOrder+"/");
         sizeLine+=s.length();
         res.append(s);
      } 

//      int n = res.length();
//      if( n>0 && res.charAt(n-1)==',' ) res.replace(n-1, n-1, (flagNL?"\n":" "));
//      else res.append((flagNL?"\n":" "));
      if( flagNL && sizeLine!=2 ) res.append('\n');

      writeASCIIFlush(out,res);
   }

   /** Write HEALPix MOC to an output stream IN JSON encoded format
    * @param out output stream
    */
   public void writeJSON(OutputStream out) throws Exception {
      testMocNotNull();
      
      if( moc instanceof SpaceTimeMoc ) throw new Exception("JSON format not supported for SpaceTimeMoc");
      
      // Nouvelle version JSON
      // On remplace tout de même l'ancienne signature non compatible JSON par une ligne vide
      // pour essayer d'éviter de planter les vieux clients JSON qui saute la première ligne
      out.write(CR.getBytes());
      
      StringBuilder s = new StringBuilder(2048);
      int nOrder = moc.getMaxOrder()+1;
      s.append("{");
      boolean first=true;
      int order;
      for( order=0; order<nOrder; order++) {
         int n = moc.getSize(order);
         if( n==0 && order<nOrder-1 ) continue;
         Array a = moc.getArray(order);
         if( !first ) s.append("],"+CR);
         first = false;
         s.append("\""+order+"\":[");
         int j=0;
         for( int i=0; i<n; i++ ) {
            s.append( a.get(i)+(i==n-1?"":",") );
            j++;
            if( j==15 ) { writeASCIIFlush(out,s); j=0; }
         }
      }
      
      if( !first ) s.append("]");
      s.append("}");
      writeASCIIFlush(out,s);
   }

   /** Write HEALPix MOC to an output stream in FITS encoded format
    * @param out output stream
    */
   public void writeFits(OutputStream out) throws Exception {
      testMocNotNull();
      writeHeader0(out);
      writeHeader1(out);
      writeData(out);
   }

   /*********************************************** Private methods  *****************************************/

   // Parsing de la ligne NSIDE=nnn et positionnement de l'ordre courant correspondant
   private void setCurrentParseOrder(String s) throws Exception {
      int i = s.indexOf('=');
      try {
         moc.setCurrentOrder( (int)SpaceMoc.log2(Long.parseLong(s.substring(i+1))) );
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
            moc.setCurrentOrder( Integer.parseInt(s.substring(i+1)) );
         } catch( Exception e ) {
            throw new Exception("HpixList.setOrder: syntax error ["+s+"]");
         }
         return;
      }

      // C'est ORDERING=NESTED|RING => ignoré
   }

   // Parsing de la ligne COORDSYS=G|C
   private void setCoord(String s) throws Exception {
      int i = s.indexOf('=');
      moc.setCoordSys(s.substring(i+1));
   }

   // Parse une ligne d'un flux (reconnait JSON et basic ASCII)
   private void parseASCIILine(String s) throws Exception {
      char a = s.charAt(0);
      if( a=='#' ) return;
      if( a=='N' ) setCurrentParseOrder(s);
      else if( a=='C' ) setCoord(s);
      else if( a=='O' ) setOrder(s);
      else {
         StringTokenizer st = new StringTokenizer(s," ;,\n\r\t{}");
         while( st.hasMoreTokens() ) {
            String s1 = st.nextToken();
            if( s1.length()==0 ) continue;
            moc.addHpix(s1);
         }
      }
   }

   private void writeASCIIFlush(OutputStream out, StringBuilder s) throws Exception {
      writeASCIIFlush(out, s, true);
   }
   
   private void writeASCIIFlush(OutputStream out, StringBuilder s,boolean nl) throws Exception {
      if( nl ) s.append(CR);
      out.write(s.toString().getBytes());
      s.delete(0,s.length());
   }

   // Write the primary FITS Header
   private void writeHeader0(OutputStream out) throws Exception {
      int n=0;
      out.write( getFitsLine("SIMPLE","T","Written by MOC java API "+SpaceMoc.VERSION) ); n+=80;
      out.write( getFitsLine("BITPIX","8") ); n+=80;
      out.write( getFitsLine("NAXIS","0") );  n+=80;
      out.write( getFitsLine("EXTEND","T") ); n+=80;
      out.write( getEndBourrage(n) );
   }
   
   // Write the FITS HDU Header for the UNIQ binary table
   private void writeHeader1(OutputStream out) throws Exception {
      int n=0;
      int nbytes = moc.getType()==SpaceMoc.LONG ? 8 : 4;
      int naxis2 = moc.getSize();
      out.write( getFitsLine("XTENSION","BINTABLE","Multi Order Coverage map") ); n+=80;
      out.write( getFitsLine("BITPIX","8") ); n+=80;
      out.write( getFitsLine("NAXIS","2") );  n+=80;
      out.write( getFitsLine("NAXIS1",nbytes+"") );  n+=80;
      out.write( getFitsLine("NAXIS2",""+naxis2 ) );  n+=80;
      out.write( getFitsLine("PCOUNT","0") ); n+=80;
      out.write( getFitsLine("GCOUNT","1") ); n+=80;
      out.write( getFitsLine("TFIELDS","1") ); n+=80;
      out.write( getFitsLine("TFORM1",nbytes==4 ? "1J" : "1K") ); n+=80;
      
      n+=moc.writeSpecificFitsProp( out );
      out.write( getFitsLine("MOCTOOL","CDSjavaAPI-"+SpaceMoc.VERSION,"Name of the MOC generator") );    n+=80;      

      for( int i=0; i<FITSKEY.length; i++ ) {
         String key = FITSKEY[i][0];
         if( key.equals("COORDSYS")) continue;
         if( key.equals("MOCORDER")) continue;
         if( key.equals("MOCTOOL")) continue;
         String value = moc.getProperty(key);
         if( value==null ) continue;
         out.write( getFitsLine(key,value,FITSKEY[i][1]) );
         n+=80;
      }
      out.write( getEndBourrage(n) );
   }

   // Write the UNIQ FITS HDU Data in basic mode
   protected void writeData(OutputStream out) throws Exception {
      int size = moc.writeSpecificData( out);
      out.write( getBourrage(size) );
//      System.out.println("write size="+size+" getSize="+moc.getSize()*8);
   }

   static protected int writeVal(OutputStream out,long val,byte []buf) throws Exception {
      for( int j=0,shift=(buf.length-1)*8; j<buf.length; j++, shift-=8 ) buf[j] = (byte)( 0xFF & (val>>shift) );
      out.write( buf );
      return buf.length;
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
   static protected byte [] getFitsLine(String key, String value, String comment) {
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
   static protected byte [] getBourrage(int currentPos) {
      int size = 2880 - currentPos%2880;
      byte [] b = new byte[size];
      return b;
   }

   /** Fully read buf.length bytes from in input stream */
   static public void readFully(InputStream in, byte buf[]) throws IOException {
      readFully(in,buf,0,buf.length);
   }

   /** Fully read len bytes from in input stream and store the result in buf[]
    * from offset position. */
   static public void readFully(InputStream in,byte buf[],int offset, int len) throws IOException {
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
   static private boolean isFitsString(String s) {
      if( s.length()==0 ) return true;
      char c = s.charAt(0);
      if( s.length()==1 && (c=='T' || c=='F') ) return false;   // boolean
      if( !Character.isDigit(c) && c!='.' && c!='-' && c!='+' ) return true;
      try {
         Double.valueOf(s);
         return false;
      } catch( Exception e ) { return true; }
   }

   static private char [] formatFitsString(char [] a) {
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

         header = new Hashtable<>(200);
         byte[] buffer = new byte[fieldsize];

         while (true) {
            if( firstChar==0 ) readFully(dis,buffer);

            // The first character may be already read to determine the mode
            else {
               buffer[0] = firstChar;
               firstChar=0;
               readFully(dis,buffer,1,buffer.length-1);
            }

            key =  getKey(buffer);
            if( linesRead==0 && !key.equals("SIMPLE") && !key.equals("XTENSION") ) throw new Exception("Not a MOC FITS format");
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
      public int getIntFromHeader(String key) throws NumberFormatException,NullPointerException {
         String s = header.get(key.trim());
         return (int)Double.parseDouble(s.trim());
      }

      /**
       * Provide string value associated to a FITS key
       * @param key FITs key (with or without trailing blanks)
       * @return corresponding string value without quotes (')
       */
      public String getStringFromHeader(String key) throws NullPointerException {
         String s = header.get(key.trim());
         if( s==null || s.length()==0 ) return s;
         if( s.charAt(0)=='\'' ) return s.substring(1,s.length()-1).trim();
         return s;
      }
   }
}
