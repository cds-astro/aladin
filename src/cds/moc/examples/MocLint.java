// Copyright 2011 - Unistra/CNRS
// The MOC API project is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of MOC API java project.
//
//    MOC API java project is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    MOC API java project is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with MOC API java project.
//
package cds.moc.examples;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import cds.moc.Moc;
import cds.moc.Range;


/**
 * MOC lint - verifying the IVOA 2.0, 1.1 and 1.0 MOC recommendation compatibility
 * @author P.Fernique [CDS]
 * @version 2.0 - April 2021 MOC 2.0 + ASCII + JSON
 * @version 1.0 - April 2016 MOC 1.0
 *
 */
public class MocLint {
   
   static final private int MAXERROR = 20;
   
   // Serialization of MOC
   static private final int FITS = 0;    // Binary serialization (FITS binary table)
   static private final int ASCII = 1;   // ASCII serialization (ex: 
   
   // MOC version
   static private final int MOC1=1;
   static private final int MOC2PROTO=2;
   static private final int MOC2=3;
   
   // Type of MOC
   static private final int UNKNOWN=-1;
   static private final int SPACE=0;
   static private final int TIME=1;
   static private final int TIMESPACE=2;
   
   // Just for test
   public static void main(String[] args) {
      try { 
         if( args.length==0 || args[0].equals("-h") ) {
            System.out.println(
                  "Usage:       MocLint MocFileName\n"
                + "Description: Check compliance with MOC IVOA recommendations (1.0, 1.1 and 2.0).\n"
                + "Author:      P.Fernique [CDS]\n"
                + "Version:     2.0 - april 2021 (first version 2016)");
             System.exit(2);
         }
         check(args[0]); }
      catch( Exception e ) { e.printStackTrace(); }
   }
   
   /** Check the IVOA 2.0, 1.1 and 1.0 MOC recommendation compatibility
    * @param filename name of the file containing the MOC in FITS container
    * @return true if MOC is compatible
    */
   public static boolean check(String filename) throws Exception {
      FileInputStream in = null;
      try {
         in = new FileInputStream(filename);
         return check(in);
      } finally { in.close(); }
   }
   
   /** Check the IVOA 2.0, 1.1 and 1.0 MOC recommendation compatibility
    * @param in stream containing the MOC in FITS or ASCII container
    * @return true if MOC is compatible
    */
   public static boolean check(InputStream in) {
      StringBuilder out = new StringBuilder();
      int rep=0;

      try {
         BufferedInputStream bis=new BufferedInputStream(in, 32 * 1024);
         
         // Read the first charactere for deciding FITS or ASCII, and reset the stream
         bis.mark(10);
         byte [] b = new byte[1];
         bis.read(b);
         bis.reset();
         int mode = b[0]=='S' ? FITS : ASCII; 
          
         switch( mode ) {
            case FITS:  rep = checkFits( out,bis);  break;
            case ASCII: rep = checkAscii( out,bis); break;
         }
         
         System.out.print(out.toString());

      } catch( Exception e ) { e.printStackTrace(); }
      
      return rep!=0;
   }
   
   private static int  error(StringBuilder out,String s)   { out.append("ERROR   "+s+"\n"); return 1; }
   private static void info(StringBuilder out,String s)    { out.append("INFO    "+s+"\n"); }
   private static void status(StringBuilder out,String s)  { out.append("STATUS  "+s+"\n"); }
   private static int  warning(StringBuilder out,String s) { out.append("WARNING "+s+"\n"); return 1; }
   
   private static void tooMany(StringBuilder out) throws Exception {
      out.append("ERROR   Too many errors. Is it really a MOC ?"+"\n");
      throw new Exception();
   }
   
   
   /*******************************************  FITS parser ************************************************************/
   
   // Example: 2016-05-09[T10:39[:00]]
   private static boolean checkDate(String s) {
      int mode=0;
      for( int i=0; i<s.length(); i++ ) {
         char ch = s.charAt(i);
         switch(mode) {
            case 0: if( ch=='-' ) mode=1;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 1: if( ch=='-' ) mode=2;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 2: if( ch=='T' ) mode=3;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 3: if( ch==':' ) mode=4;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 4: if( ch==':' ) mode=5;
                    else if( !Character.isDigit(ch) ) return false;
                    break;
            case 5: if( !Character.isDigit(ch) ) return false;
                    break;
         }
      }
      return mode==2 || mode==4 || mode==5;
   }
   
   // Extract FITS value from FITS header line. Remove the quotes if required
   private static String getVal(byte [] buffer) {
      int i;
      boolean quote = false;
      boolean blanc=true;
      int offset = 9;

      for( i=offset ; i<80; i++ ) {
         if( !quote ) {
            if( buffer[i]==(byte)'/' ) break;   // on a atteint le commentaire
         } else {
            if( buffer[i]==(byte)'\'') break;   // on a atteint la prochaine quote
         }

         if( blanc ) {
            if( buffer[i]!=(byte)' ' ) blanc=false;
            if( buffer[i]==(byte)'\'' ) { quote=true; offset=i+1; }
         }
      }
      return (new String(buffer, 0, offset, i-offset)).trim();
  }
   
   // Extract FITS keyword from FITS header line.
   private static String getKey(byte [] buffer) {
      return new String(buffer, 0, 0, 8).trim();
   }

   // Convert s in integer
   private static long getInt(String s) {
      long v;
      try {
         v = Long.parseLong(s);
         return v;
      } catch( Exception e) { }
      return -1;
   }
      
   // Convert 4 or 8 bytes in the corresponding Long value
   static private long decode(byte [] t, int nbyte) {
      long val = 0;
      int a = ((t[0]) << 24) | (((t[1]) & 0xFF) << 16) | (((t[2]) & 0xFF) << 8) | (t[3]) & 0xFF;
      if( nbyte == 4 ) val = a;
      else {
         int b = ((t[4]) << 24) | (((t[5]) & 0xFF) << 16) | (((t[6]) & 0xFF) << 8) | (t[7]) & 0xFF;
         val = (((long) a) << 32) | ((b) & 0xFFFFFFFFL);
      }
      return val;
   }
   
   /**
    * Check the IVOA 2.0, 1.1 or 1.0 MOC recommendation compatibility 
    * @param out Trace of the validator
    * @param in stream containing the MOC in FITS container
    * @return 1-ok, 0-error, -1-warning
    */
   public static int checkFits(StringBuilder out, InputStream in) {
      long naxis = -1, naxis1 = -1, naxis2 = -1, pcount = 0, gcount = 1, tfields = -1; 
      long mocorder = -1, mocord_s = -1, mocord_t = -1;
      String tform1 = "",  pixtype = "", ordering = "", coordsys = "";
      String moctool = "", date = "", origin = "", moctype = "", mocid = "", extname = "";
      String  mocvers = "", mocdim = "", timesys = "";
      int mocv=-1;   // MOC version: MOC1, MOC2 ...
      int moct=-1;   // Type of MOC: SPACE, TIME, TIMESPACE, ... 
      int w=0;       // Number of warnings
      int e=0;       // Number of errors
      boolean moc1compatible=true;

      try {
         byte buf[] = new byte[80];
         int n;
         String extend = "";
         int line = 0;
         int size = 0;
         
         // Reading first FITS HDU
         while( (n = in.read(buf)) != 0 ) {
            size += n;
            line++;
            if( buf[0] == 'E' && buf[1] == 'N' && buf[2] == 'D' ) break;
            String key = getKey(buf);
            if( key.equals("COMMENT") || key.equals("HISTORY") ) continue;
            String val = getVal(buf);
            String s = (new String(buf,0,0,n)).trim();
            if( ((char) buf[8]) != '=' ) {
               e+=error(out,"[4.3.1] HDU0 line " + line + ": missing \"=\" character ["+s+"]");
            }
            if( line == 1 && (!key.equals("SIMPLE") || key.equals("SIMPLE") && !val.equals("T")) ) {
               e+=error(out,"[4.3.1] HDU0 line "+ line + ": SIMPLE=T missing ["+s+"]");
            }
            if( key.equals("EXTEND") ) extend = val;
            if( e>MAXERROR ) tooMany(out);
         }
         if( !extend.equals("T") ) {
            w+=warning(out,"[4.3.1] HDU0: EXTEND=T required");
         }
         
         // Skipping end of primary HDU
         int skip = 2880 - size % 2880;
         if( skip != 2880 ) {
            size += in.skip(skip);
         }
         // Reading the second HDU
         line = 0;
         while( (n = in.read(buf)) != 0 ) {
            size += n;
            line++;
            if( buf[0] == 'E' && buf[1] == 'N' && buf[2] == 'D' ) break;
            String key = getKey(buf);
            if( key.equals("COMMENT") || key.equals("HISTORY") ) continue;
            String val = getVal(buf);
            String s = (new String(buf,0,0,n)).trim();
            if( ((char) buf[8]) != '=' ) {
               e+=error(out,"[4.3.1] HDU1 line " + line + ": missing \"=\" character ["+s+"]");
            }
            if( line == 1 && (!key.equals("XTENSION") || key.equals("XTENSION") && !val.equals("BINTABLE")) ) {
               e+=error(out,"[4.3.1] HDU1 line " + line + ": XTENSION=BINTABLE missing");
            }
            else if( key.equals("NAXIS") )    naxis = getInt(val);
            else if( key.equals("NAXIS1") )   naxis1 = getInt(val);
            else if( key.equals("NAXIS2") )   naxis2 = getInt(val);
            else if( key.equals("PCOUNT") )   pcount = getInt(val);
            else if( key.equals("GCOUNT") )   gcount = getInt(val);
            else if( key.equals("TFIELDS") )  tfields = getInt(val);
            else if( key.equals("TFORM1") )   tform1 = val;
            
            else if( key.equals("MOCVERS") )  mocvers = val;
            else if( key.equals("MOCDIM") )   mocdim = val;
            else if( key.equals("ORDERING") ) ordering = val;
            else if( key.equals("COORDSYS") ) coordsys = val;
            else if( key.equals("TIMESYS") )  timesys = val;
            else if( key.equals("MOCTOOL") )  moctool = val;
            else if( key.equals("MOCTYPE") )  moctype = val;
            else if( key.equals("MOCORD_S") ) mocord_s = getInt(val);
            else if( key.equals("MOCORD_T") ) mocord_t = getInt(val);
            else if( key.equals("MOCORDER") ) mocorder = getInt(val);
            else if( key.equals("PIXTYPE") )  pixtype = val;

            else if( key.equals("DATE") ) date = val;
            else if( key.equals("ORIGIN") ) origin = val;
            else if( key.equals("EXTNAME") ) extname = val;
            else if( key.equals("MOCID") ) mocid = val;
            if( e>MAXERROR ) tooMany(out);
         }
         
         // General information
         info(out,"Fits MOC serialization");
         if( moctool.length()>0 ) info(out,"Generated by: "+moctool);
         if( date.length()>0 )    info(out,"Date: "+date);
         if( origin.length()>0 )  info(out,"Origin: "+origin);
         if( mocid.length()>0 )   info(out,"Moc id: "+mocid);
         if( extname.length()>0 ) info(out,"Extname: "+extname);
         if( moctype.length()>0 ) info(out,"Moc type: "+moctype);
         if( naxis2!=-1 )         info(out,"Number of rows: "+naxis2);
         if( tform1.length()>0 )  info(out,"Coding: " +(tform1.endsWith("J")?"32 bits integer":tform1.endsWith("K")?"64 bits long":tform1));

         // FITS compliance
         if( gcount != 1 ) w+=warning(out,"[4.3.1]: only GCOUNT=1 authorized in HDU1");
         if( pcount != 0 ) w+=warning(out,"[4.3.1]: only PCOUNT=0 authorized in HDU1");
         if( tfields != 1 ) e+=error(out,"[4.3.1]: TFIELDS=1 required in HDU1");
         if( tform1.length() > 1 && tform1.charAt(0) != '1' ) e+=error(out,"[4.3.1]: TFORM1=1J or 1K required in HDU1");
         if( tform1.length() > 1 ) tform1 = tform1.substring(1);
         if( !tform1.equals("J") && !tform1.equals("K") ) e+=error(out,"[4.3.1]: TFORM1=1J or 1K required in HDU1");
         if( naxis != 2 ) e+=error(out,"[4.3.1]: only NAXIS=2 authorized in HDU1");
         if( tform1.equals("J") && naxis1 != 4 ) e+=error(out,"[4.3.1]: only NAXIS1=4 compatible with TFORM1=J in HDU1");
         if( tform1.equals("K") && naxis1 != 8 ) e+=error(out,"[4.3.1]: only NAXIS1=8 compatible with TFORM1=K in HDU1");
         if( naxis2 < 0 ) e+=error(out,"[4.3.1]: NAXIS2 error in HDU1");
         if( date.length()>0 && !checkDate(date) ) w+=warning(out,"[4.3.1]: DATE syntax error: no FITS convention ["+date+"]");

         mocv = mocvers.length()==0 ? MOC1 : MOC2;
         
         // Check for MOC1.0 and 1.1 compliance
         if( mocv==MOC1 ) {
            moct=SPACE;
            info(out,"Moc version: <2.0");
            if( mocorder!=-1 ) info(out,"Moc order: "+mocorder);
            
            if( ordering.equals("RANGE29") ) {
               w+=warning(out,"[0]: ORDERING=RANGE29 is a prototype of STMOC => not standard");
               moct=TIMESPACE;
               mocv=MOC2PROTO;
            } else {
               if( !pixtype.equals("HEALPIX") ) w+=warning(out,"[6.l]: PIXTYPE=HEALPIX mandatory in HDU1");
               if( mocorder ==-1 ) w+=warning(out,"[6.k]: MOCORDER is mandatory in HDU1");
               else if( mocorder < 0 || mocorder > 29 ) e+=error(out,"[3.1]: MOCORDER=n where n in [0..29] required in HDU1");
               //            if( mocorder==29 ) info(out,"(!) mocOrder 29 is probably a wrong default value rather than a deliberated choice - check it!");
               if( coordsys.length()==0 ) w+=warning(out,"[6.d]: COORDSYS=C mandatory in HDU1 for celestial coverage");
               if( !ordering.equals("NUNIQ") ) w+=warning(out,"[6.c]: ORDERING=NUNIQ mandatory in HDU1");
               if( tform1.equals("J") && mocorder > 13 ) info(out,"(!) mocOrder>13 may require 64 rather than 32 bits integer coding (TFORM1=1K) - check it!");
               if( coordsys.length()>0 && !coordsys.equals("C") ) w+=warning(out,"[6.d]: wrong COORDSYS ["+coordsys+"]. MOC must use ICRS (C) only");
            }
            
         // Check for MOC2.0 compliance
         } else {
            info(out,"Moc version: "+mocvers);
            info(out,"Moc dimension: "+mocdim);
            if( mocord_s!=-1 ) info(out,"Space order: "+mocord_s);
            if( mocord_t!=-1 ) info(out,"Time order: "+mocord_t);
            
            if( !tform1.equals("K") || naxis1!=8 ) e+=error(out,"[4.3.1]: NAXIS1=8 / TFORM1=K required in HDU1");
            if( !mocvers.equals("2.0") ) w+=warning(out,"[6]: MOCVERS ["+mocvers+"] not supported for this MOC lint tool (should be 2.0, or not specified)");
            if( mocdim.length()==0 ) e+=error(out,"[6.k]: MOCDIM is mandatory in HDU1");
            moct = mocdim.equals("SPACE") ? SPACE : mocdim.equals("TIME") ? TIME : mocdim.equals("TIME.SPACE") ? TIMESPACE : -1;
            if( moct==-1 ) e+=error(out,"[6.b]: unvalid MOCDIM value ["+mocdim+"]. Must be SPACE, TIME or TIME.SPACE");
            
            if( moct==SPACE) {
               if( mocorder ==-1 ) {
                  info(out,"[6.k]: MOCORDER is suggested (=MOCORD_S) in HDU1 for backward compatibility (required in MOC1.0 & MOC1.1)");
                  moc1compatible=false;
               }
               if( !ordering.equals("NUNIQ") ) w+=warning(out,"[6.c]: ORDERING=NUNIQ mandatory in HDU1");
               if( tform1.equals("J") && mocorder > 13 ) info(out,"(!) mocOrder>13 may require 64 rather than 32 bits integer coding (TFORM1=1K) - check it!");
            }
            
            if( moct==SPACE || moct==TIMESPACE ) {
               if( mocord_s == -1 ) e+=error(out,"[6.i]: MOCORD_S is mandatory in HDU1");
               else if( mocord_s < 0 || mocord_s > 29 ) e+=error(out,"[3.1]: MOCORD_S=n where n in [0..29] required in HDU1");
               if( coordsys.length()>0 && !coordsys.equals("C") ) e+=error(out,"[6.d]: wrong COORDSYS ["+coordsys+"]. MOC must use ICRS (C) only");
            }
            
            if( moct==TIME || moct==TIMESPACE ) {
               if( mocord_t == -1 ) e+=error(out,"[6.j]: MOCORD_T is mandatory in HDU1");
               else if( mocord_t < 0 || mocord_t > 61 ) e+=error(out,"[3.2]: MOCORD_T=n where n in [0..61] required in HDU1");
               if( timesys.length()>0 && !timesys.equals("TCB") ) e+=error(out,"[6.e]: wrong TIMEDSYS ["+timesys+"]. MOC must use TCB only");
               
               if( moct==TIME && !ordering.equals("RANGE") && !ordering.equals("NUNIQ")) e+=error(out,"[4.3.1]: ORDERING=RANGE|NUNIQ mandatory in HDU1");
               else if( moct==TIMESPACE && !ordering.equals("RANGE") ) e+=error(out,"[5.2]: ORDERING=RANGE mandatory in HDU1");
            }
         }
         
         // Skipping end of secondary HDU
         skip = 2880 - size % 2880;
         if( skip != 2880 ) {
            size += in.skip(skip);
         }
         
         // Checking NUNIQ coding (for SMOC and TMOC)
         if( ordering.equals("NUNIQ") ) {
            
            int lmt = 29;
            if( mocv==MOC2 ) {
               mocorder = (moct==SPACE) ? mocord_s : mocord_t;
               if( moct==TIME ) lmt=61;
            }
            int prev_order = -1;
            long prev_val = -1L;
            boolean sorted=true;

            // Reading binary elements
            long[] hpix = null;
            int nbyte = tform1.equals("J") ? 4 : 8;
            byte t[] = new byte[nbyte];
            for( long i = 0; i < naxis2; i++ ) {
               n=0;
               int m1;
               while( (m1=in.read(t,n,t.length-n))!=0 ) { n+=m1; if( n==t.length ) break; }
               size += n;
               if( n != t.length ) e+=error(out,"[4.3.1]: truncated FITS table after row " + i);

               long rawval = decode(t,nbyte);
               hpix = Moc.uniq2hpix(rawval, hpix);
               int order = (int) hpix[0];
               long val = hpix[1];
               if( order < 0 || order > lmt ) e+=error(out,"[3.1]: order error in row " + i+ " ["+order+"]");
               if( mocorder>=0 && order > mocorder ) w+=warning(out,"[3.1]: order greater than mocorder in row " + i+ " ["+order+"]");
               
               long maxval = Moc.pow2(order);
               if( moct==SPACE ) {  maxval *= maxval; maxval *= 12L; }
               if( val < 0 ) e+=error(out,"[3.1]: negative val error in row " + i+ " ["+val+"]");
               if( val >= maxval ) e+=error(out,"[3.1]: too high val for the current order in row " + i+ " ["+val+"]");
               
               if( sorted ) {
                  if( order<prev_order ) { w+=warning(out,"[4.3.1]: not ascending orders (row "+i+")"); sorted=false; }
                  if( order!=prev_order ) {
                     prev_val=-1;                     
                     prev_order=order;
                  }
                  if( val<=prev_val ) { w+=warning(out,"[4.3.1]: not ascending npixs (row "+i+")"); sorted=false; }
                  prev_val=val;
               }
               
               if( e>MAXERROR ) tooMany(out);
            }

         // Checking RANGE coding (for STMOC and TMOC)
         } else {
            long prev_valt=-1;           // Previous time val
            long prev_vals=-1;           // Previous space val
            long lastval=-1;             // last val read
            boolean sorted=true;         // flag for avoiding too many warnings
            boolean smocAggreg=true;     // idem
            boolean timeAggreg=true;     // idem
            boolean spaceAggreg=true;    // idem
            int nbyte = 8;
            byte t[] = new byte[nbyte];
            long maxt = Moc.pow2(62)-1L;                     // Highest possible time value
            long maxs = 12L * Moc.pow2(29) * Moc.pow2(29);   // Highest possible space value
            Range prev_sr = null;                            // Last Space coverage
            Range sr = new Range();                          // Current space coverage
            
            int mode=0;   // parsing mode: 0-time start, 1-time end, 2-space start, 3-space end
            int omode=-1; // previous parsing mode
            
            if( naxis%2L!=0 ) e+=error(out,"[4.3.2]: Odd number of values. RANGE coding required a list of ranges");
            
            for( long i = 0; i < naxis2; i++ ) {
               
               n=0;
               int m1;
               while( (m1=in.read(t,n,t.length-n))!=0 ) { n+=m1; if( n==t.length ) break; }
               size += n;
               if( n != t.length ) e+=error(out,"[4.3.1]: truncated FITS table after row " + i);

               long val = lastval = decode(t,nbyte);
               
               // Parsing mode determination
               if( moct==TIME ) mode = (int)( i%2L );
               else {
                  mode = (int)( i%2L );
                  if( val>=0 ) mode+=2;
               }
               
               if( moct==TIME ) {
                  if( val < 0 ) e+=error(out,"[3.2]: val negative error in row " + i+ " ["+val+"]");
                  if( val >= maxt ) e+=error(out,"[3.2]: val too high in row " + i+ " ["+val+"]");

                  if( sorted ) {
                     if( mode==0 && val<=prev_valt 
                           || mode==1 && val< prev_valt ) { w+=warning(out,"[4.3.1]: not ascending ranges (row "+i+")"); sorted=false; }
                     prev_valt=val;
                  }
                  
               // TIME.SPACE
               } else {
                  
                  // in time range
                  if( i<2 || mode<2 ) {
                     if( val >= 0 ) e+=error(out,"[5.2]: val error in row " + i+ " ["+val+"]. Time range must be coded as negative value");
                     val = -val;
                     if( mode==0 && val >= maxt ) e+=error(out,"[3.2]: time val too high in row " + i+ " ["+val+">=2^62-1]");
                     if( mode==1 && val >  maxt ) e+=error(out,"[3.2]: time val too high in row " + i+ " ["+val+">2^62-1]");
                     
                  // in space range
                  } else {
                     if( val < 0 ) e+=error(out,"[5.2]: val error in row " + i+ " ["+val+"]. Space range must be coded as positive value");
                     if( mode==2 && val >= maxs ) e+=error(out,"[3.2]: space val too high in row " + i+ " ["+val+">=12x2^29x2^29]");
                     if( mode==3 && val >  maxs ) e+=error(out,"[3.2]: space val too high in row " + i+ " ["+val+">12x2^29x2^29]");
                     
                     sr.push(val);
                  }
                  
                  if( timeAggreg ) {
                     if( mode==0 && omode==1 && val==prev_valt ) { 
                        w+=warning(out,"[4.3.1]: not agregating consecutive time ranges (first found at row "+i+")"); timeAggreg=false; }
                  }
                  
                  if( spaceAggreg ) {
                     if( mode==3 && omode==2 && val==prev_vals ) { 
                        w+=warning(out,"[4.3.1]: not agregating consecutive space ranges (first found at row "+i+")"); spaceAggreg=false; }
                  }
                  
                 if( mode<2 ) {
                     if( sorted && val<prev_valt  ) { w+=warning(out,"[4.3.1]: not ascending time ranges (row "+i+")"); sorted=false; }
                     prev_valt = val;
                     prev_vals = -1;
                  } else {
                     if( sorted ) {
                        if( mode==2 && val<=prev_vals 
                              || mode==3 && val< prev_vals ) { w+=warning(out,"[4.3.1]: not ascending space ranges (row "+i+")"); sorted=false; }
                     }
                     prev_vals = val;
                  }

                  if( smocAggreg ) {
                     if( mode==0 && omode==3 ) {
                        if( prev_sr!=null && prev_sr.equals(sr) )  {
                           w+=warning(out,"[5.2]: there are identical space coverages for consecutive time ranges (first found at row "+(i-1)+")");
                           smocAggreg=false;
                        }
                        prev_sr = sr;
                     }
                  }
                  
                  omode = mode;
               }

               if( e>MAXERROR ) tooMany(out);
            }
            if( moct==TIMESPACE && lastval<0 ) e+=error(out,"[3.2]: last space coverage is missing => Wrong interleave time/space ranges");
         }
         
         
         // Finishing FITS stream
         skip = 2880 - size % 2880;
         if( skip != 2880 ) {
            n = (int) in.skip(skip);
            size+=n;
            if( n < skip ) w+=warning(out,"[4.3.1]: FITS not aligned on 2880 byte blocks");
         }
         info(out,"FITS size: "+size+" bytes");
      } catch( Exception e1 ) {
         e+=error(out,"Unrecovered exception !");
      }
      
      if( w==0 && e==0 ) {
         status(out,"OK! MOC compliant with IVOA MOC "+(mocv==MOC1?"1.1":"2.0")+" recommendation");
         return 1;
      } else if( e==0 ) {
         if( mocv==MOC2 && !moc1compatible ) {
            status(out,"OK! MOC compliant with IVOA MOC 2.0 recommendation, but not compatible with IVOA MOC 1.0 and 1.1");
            return 1;
         } 
         if( mocv==MOC2PROTO ) {
            status(out,"WARNING! MOC proto 2 ok but not compliant with IVOA final recommendation");
            return -1;
         }
         status(out,"WARNING! MOC ok but not fully compliant with IVOA MOC "+(mocv==MOC1?"1.1":"2.0")+" recommendation");
         return -1;
      } else {
         status(out,"ERROR! MOC error, not usable");
         return 0;
      }
   }
   
   /******************************************************   ASCII parser *************************************************/
   
   // States of ASCII parsing
   static final int INUNKNOWN = 0;   // Undetermined MOC dimension     
   static final int INTIME = 1;      // Parsing TIME MOC dimension     
   static final int INSPACE = 2;     // Parsing SPACE MOC dimension     
   
   /** Return true if s is a 1 char string, and this char is in lst string */
   private static boolean in( String s, String lst ) {
      if( s.length()!=1 ) return false;
      int i = lst.indexOf(s.charAt(0));
      return i>=0;
   }
   
   /** Unquote string if required ("xxx" => xxx) */
   private static String unQuote(String s) {
      int n=s.length();
      if( n>2 && s.charAt(0)=='"' && s.charAt(n-1)=='"' ) return s.substring(1,n-1);
      return s;
   }

   /** Unbracket string if required ([xxx or xxx] or [xxx] => xxx) */
   private static String unBracket(String s) {
      int n=s.length();
      if( n<1 ) return s;
      int o1 = s.charAt(0)=='[' ? 1:0;
      int o2 = s.charAt(n-1)==']' ? n-1 : n;
      return s.substring(o1,o2);
   }
   
   /**
    * Check the IVOA 2.0, 1.1 MOC recommendation compatibility 
    * @param out Trace of the validator
    * @param in stream containing the MOC in ASCII container
    * @return 1-ok, 0-error, -1-warning
    */
   public static int checkAscii(StringBuilder out, InputStream in) {
      int w=0;       // Number of warnings
      int e=0;       // Number of errors
      int nbval=0;  
      boolean sepAsciiOk=true;
      boolean json=false;
      int mocv = MOC2;
      int line=0;
      long order=-1;
      long val1=-1,val2=-2;
      int mode=INUNKNOWN;
      long maxval=-1;
      int moct=-1;
      
      long mocOrdS=-1;
      long mocOrdT=-1;
      long mocOrd=-1;
      
      long lastVal=-1;
      long lastValT=-1;
      long lastValS=-1;
      long lastOrder=-1;
      long lastOrderS=-1;
      long lastOrderT=-1;
      
      String tok="";
      
      info(out,"ASCII MOC serialization");
      try {
         
         BufferedReader dis = new BufferedReader(new InputStreamReader(in));
         String s;
         while( (s=dis.readLine())!=null ) {
            line++;
            if( s.length()==0 ) continue;
            if( s.charAt(0)=='#' ) continue;

            StringTokenizer st = new StringTokenizer(s," ;,\t{}",true);
            while( st.hasMoreTokens() ) {
               String s1 = tok = st.nextToken();
               if( s1.length()==0 ) continue;
               
               // Json non standard alternative ?
               if( json ) {
                  int doublepoint = s1.indexOf(':');
                  if( doublepoint>0 ) {
                     s1 = unQuote(s1.substring(0,doublepoint)) +"/"+ unBracket(s1.substring(doublepoint+1));
                  } else s1=unBracket(s1);
                  
               } else {
                  if( in(s1,"{}") )  {
                     w+=warning(out,"[4.3.2]: seems to be a JSON serialization => Checking MOC structure only, not JSON syntax...");
                     json=true;
                     continue;
                  }
               }
               
               // Separator checking
               if( !json && in(s1,";,\t") ) {
                  if( sepAsciiOk ) { w+=warning(out,"[4.3.2]: there are no standard separator (first found ["+s1+"] line "+line+")"); sepAsciiOk=false; }
                  continue;
               }
               
               if( in(s1," {},") ) continue;
               
//               System.out.println("Tok: ["+s1+"]");
               
               int start=0;
               int slashPos = s1.indexOf('/');
               if( slashPos==0 ) e+=error(out,"[4.3.2]: error before '/'(line "+line+"). Separator between order and '/' is not allowed");
               
               if( slashPos==s1.length()-1 ) {
                  // ICI IL FAUDRAIT DETECTER LE MOCORDER ET LE MEMORISER
               }
               
               // There is a / in the expression => analyze of order part
               if( slashPos>0 ) {
                  char cdim = s1.charAt(0);
                  if( cdim=='t' ) {
                     if( moct==SPACE ) e+=error(out,"[4.3.2]: Bad structure. Time ranges must preceed space ranges in STMOC (line "+line+")");
                     if( moct==UNKNOWN ) moct=TIME;
                     if( mode==INTIME ) w+=warning(out,"[4.3.2]: consecutive 't' order prefix (line "+line+"). Not required");
                     mode=INTIME;
                     start=1;
                  } else if( cdim=='s' ) {
                     if( mode==INTIME ) moct=TIMESPACE;
                     else if( moct==UNKNOWN ) moct=SPACE;
                     if( mode==INSPACE ) w+=warning(out,"[4.3.2]: consecutive 's' order prefix (line "+line+"). Not required");
                     mode=INSPACE;
                     start=1;
                  } else if( !Character.isDigit(cdim) ) e+=error(out,"[4.3.2]: Order prefix error (line "+line+"). Must be 's' or 't'. ");
                  
                  order = getInt( s1.substring(start,slashPos) );
                  
                  // order checking
                  if( order<0 ) e+=error(out,"[3.1]: order error (token ["+tok+"] line:"+line+"). Must be a positive integer");
                  if( mode==INSPACE ) {
                     if( lastOrderS!=-1 && lastValS==-1 ) w+=warning(out,"[4.3.2]: previous space order has no value (token ["+tok+"] line:"+line+").");
                     if( order>29 ) e+=error(out,"[3.2]: space order error (token ["+tok+"] line:"+line+"). Must be in [0..29]");
                     if( order==lastOrderS ) w+=warning(out,"[4.3.2]: redundant consecutive space order (token ["+tok+"] line:"+line+").");
                     if( order<lastOrderS ) w+=warning(out,"[4.3.2]: space order values should be ascending (token ["+tok+"] line:"+line+").");
                     if( order>mocOrdS ) mocOrdS=order;
                     lastOrderS=order;
                     lastValS=-1;
                  }
                  if( mode==INTIME ) {
                     if( lastOrderT!=-1 && lastValT==-1 ) w+=warning(out,"[4.3.2]: previous time order has no value (token ["+tok+"] line:"+line+").");
                     if( order>61 ) e+=error(out,"[3.3]: time order error (token ["+tok+"] line:"+line+"). Must be in [0..61]");
                     if( order<lastOrderT ) w+=warning(out,"[4.3.2]: time order values should be ascending (token ["+tok+"] line:"+line+").");
                     if( order>mocOrdT ) mocOrdT=order;
                     lastOrderT=order;
                     lastOrderS=-1;
                     lastValT=-1;
                  }
                  if( mode==INUNKNOWN ) {
                     if( lastOrder!=-1 && lastVal==-1 ) w+=warning(out,"[4.3.2]: previous order has no value (token ["+tok+"] line:"+line+").");
                     if( order>61 ) e+=error(out,"[3.1]: too high order (token ["+tok+"] line:"+line+").");
                     if( order==lastOrder ) w+=warning(out,"[4.3.2]: redundant consecutive order (token ["+tok+"] line:"+line+").");
                     if( order<lastOrder ) w+=warning(out,"[4.3.2]: order values should be ascending (token ["+tok+"] line:"+line+").");
                     if( order>mocOrd ) mocOrd=order;
                     lastOrder=order;
                     lastVal=-1;
                  }
                  
                  // Determine max val
                  maxval = Moc.pow2(order);
                  if( mode!=INTIME ) maxval=12L*maxval*maxval;
                  
                  // continue with following val or range
                  s1 = s1.substring(slashPos+1);
               }
               
               if( s1.length()==0 ) continue;
               
                  
               // Analyze of val, or range
               if( order==-1 ) e+=error(out,"[4.3.2]: Unknown order (line "+line+")");
               else {
                  int dash = s1.indexOf('-');
                  if( dash>0 ) { val1=getInt( s1.substring(0,dash) ) ; val2=getInt( s1.substring(dash+1) ); s1=s1.substring(0,dash); }
                  else { val1=getInt( s1 ); val2=-2; }
                  
                  // First val checking
                  nbval++;
                  if( val1<0 ) e+=error(out,"[3.1]: value error (token ["+tok+"] line:"+line+"). Must be a positive integer");
                  if( val1>maxval ) e+=error(out,"[3.1]: too high value (token ["+tok+"] line:"+line+") ["+val1+">"+maxval+"]");
                  
                  if( mode==INSPACE ) {
                     if( val1<=lastValS ) w+=warning(out,"[4.3.2]: space values should be ascending (token ["+tok+"] line:"+line+").");
                     lastValS = Math.max(val1,val2);
                  }
                  if( mode==INTIME ) {
                     if( val1<=lastValT ) w+=warning(out,"[4.3.2]: time values should be ascending (token ["+tok+"] line:"+line+").");
                     lastValT = Math.max(val1,val2);
                  }
                  if( mode==INUNKNOWN ) {
                     if( val1<=lastVal ) w+=warning(out,"[4.3.2]: values should be ascending (token ["+tok+"] line:"+line+").");
                     lastVal = Math.max(val1,val2);
                  }
                  
                  // second val checking
                  if( val2!=-2 ) {
                     nbval++;
                    if( val1<0 ) e+=error(out,"[3.1]: end range error (token ["+tok+"] line:"+line+") ["+s1.substring(dash+1)+"]. Must be a positive integer");
                    if( val2<=val1 ) e+=error(out,"[4.3.2]: end range must be greater than start range (token ["+tok+"] line:"+line+") ["+val1+"-"+val2+"]. ");
                  }
               }
               
               if( e>MAXERROR ) tooMany(out);
               
            }
         }
         
         if( moct==TIMESPACE && mode==INTIME ) e+=error(out,"[4.3.2]: Bad structure. Missing space ranges at the end of the STMOC (token ["+tok+"] line:"+line+")");

      } catch( Exception e1 ) {
         e+=error(out,"Unrecovered exception !");
      }
      
      if( !json ) info(out,"Moc version: "+(moct!=UNKNOWN || mocOrd>29?"MOC2.0":"MOC1.1"));
      info(out,"Moc dimension: "+(moct==TIMESPACE?"TIME.SPACE" : moct==SPACE?"SPACE" : moct==TIME?"TIME": mocOrd>29?"TIME":"SPACE or TIME"));
      if( mocOrdS!=-1 ) info(out,"Space order: "+mocOrdS);
      if( mocOrdT!=-1 ) info(out,"Time order: "+mocOrdT);
      if( mocOrd!=-1 ) info(out,"Moc order: "+mocOrd);
      info(out,"Moc ASCII size: "+nbval+" values");

      if( w==0 && e==0 ) {
         status(out,"OK! MOC compliant with IVOA MOC "+(mocv==MOC1?"1.1":"2.0")+" recommendation");
         return 1;
      } else if( e==0 ) {
         if( json ) {
            status(out,"WARNING! JSON Moc seems ok, but not standard in IVOA MOC recommendation");
            return -1;
         }
         status(out,"WARNING! MOC ok but not fully compliant with IVOA MOC "+(mocv==MOC1?"1.1":"2.0")+" recommendation");
         return -1;
      } else {
         status(out,"ERROR! MOC error, not usable");
         return 0;
      }
   }
   

}
