// Copyright 2010 - UDS/CNRS
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

package cds.tools.pixtools;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import cds.aladin.FrameHeaderFits;
import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.fits.HeaderFits;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;

/** Gestion d'un ensemble de pixels Healpix, d'ordres variables
 * Notamment utilisé pour les Multi-Order Coverage Maps
 * @author Pierre Fernique [CDS]
 * @version 1.0 March 2011
 */
public final class HpixTree extends HealpixMoc {
   
   private FrameHeaderFits frameHeaderFits;
    
   private int frame;
   private int ordering;
   
   public HpixTree(String s) { super(s); }
   
   public HpixTree(InputStream in) throws Exception {
      super();
      read(in);
   }
   
   /** Positionne le système de coordonnées (Localisation.GAL, Localistion.ECLIPTIC, Localisation.ICRS) */
   public void setFrame(int frame) { this.frame=frame; }
   
   /** Positionne le système de numérotation NESTED ou RING */
   public void setOrdering(int ordering) { this.ordering=ordering; }
   
   /** Retourne le système de coordonnées utilisé (Localisation.GAL, Localistion.ECLIPTIC, Localisation.ICRS) */
   public int getFrame() { return frame; }
   
   /** Retourne le système de numérotation NESTED ou RING */
   public int getOrdering() { return ordering; }
   
   /** Retourne l'header Fits (s'il existe) */
   public FrameHeaderFits getHeaderFits() { return frameHeaderFits; }
   
   /** Ajout d'un pixel */
   public void add(int order,long npix) {
      
      // On n'accepte pas en dessous de l'ordre 3 => sinon impossible à tracer
      // On insère les fils d'ordre 3 équivalents
      if( order<3 ) {
         npix *=4;
         for( int i=0; i<4; i++ ) add(order+1,npix+i);
         return;
      }
      
      super.add(order,npix);
   }
   
   
   /** Read HEALPix tree from a file. Autodetect encoded format */
   public void read(String filename) throws Exception {
      File f = new File(filename);
      FileInputStream fi = new FileInputStream(f);
      read(fi);
      fi.close();
   }
   
   /** Read HEALPix tree from a stream. Autodetect encoded format */
   public void read(InputStream in) throws Exception {
      MyInputStream mis = (in instanceof MyInputStream) ? (MyInputStream)in : (new MyInputStream(in)).startRead();
      if( (mis.getType() & MyInputStream.FITS)!=0 ) readFITS(mis);
      else readASCII(mis);

   }
   
//   /** Read HEALPix tree from an Binary FITS MEF stream */
//   public void readFITS(InputStream in) throws Exception {
//      MyInputStream mis = (in instanceof MyInputStream) ? (MyInputStream)in : (new MyInputStream(in)).startRead();
//      HeaderFits header = new HeaderFits();
//      frameHeaderFits = new FrameHeaderFits();
//      frameHeaderFits.makeTA();
//      super.readFITS(mis);
//   }
   
   /** Read HEALPix tree from an Binary FITS MEF stream */
   public void readFITS(InputStream in) throws Exception {
      MyInputStream mis = (in instanceof MyInputStream) ? (MyInputStream)in : (new MyInputStream(in)).startRead();
      HeaderFits header = new HeaderFits();
      frameHeaderFits = new FrameHeaderFits();
      frameHeaderFits.makeTA();
      header.readHeader(mis);       // On mange la première entête FITS

      clear();
      try {
         header.readHeader(mis,frameHeaderFits);
         int naxis1 = header.getIntFromHeader("NAXIS1");
         int naxis2 = header.getIntFromHeader("NAXIS2");
         String tform = header.getStringFromHeader("TFORM1");
         String coordsys = header.getStringFromHeader("COORDSYS");
         frame=Localisation.GAL;
         if( coordsys!=null && coordsys.charAt(0)=='C' ) frame=Localisation.ICRS;
         int nbyte= tform.indexOf('K')>=0 ? 8 : tform.indexOf('J')>=0 ? 4 : -1;   // entier 64 bits, sinon 32
         if( nbyte<=0 ) throw new Exception("HEALPix Multi Order Coverage Map only requieres integers (32bits or 64bits)");
         byte [] buf = new byte[naxis1*naxis2];
         mis.readFully(buf);
         createUniq((naxis1*naxis2)/nbyte,nbyte,buf);
      } catch( EOFException e ) { }
   }
   
   private void createUniq(int nval,int nbyte,byte [] t) {
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
   
   /** Retourne true si le pixel est présent, lui, ou l'un de ses frères */
   public boolean isBrother(Hpix hpix) {
      long npix = hpix.getNpix();
      int order = hpix.getOrder();
      long [] lev = getPixLevel(order);
      for( int i=lev.length-1; i>=0; i-- ) {
         long pix = (lev[i]/4)*4;
         if( pix<=npix && npix<pix+4 ) return true;
      }
      return false;
   }
}
