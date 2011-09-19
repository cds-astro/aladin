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
import cds.moc.HealpixMoc;

/** Gestion d'un ensemble de pixels Healpix, d'ordres variables
 * Notamment utilis� pour les Multi-Order Coverage Maps
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
   
   /** Positionne le syst�me de coordonn�es (Localisation.GAL, Localistion.ECLIPTIC, Localisation.ICRS) */
   public void setFrame(int frame) { this.frame=frame; }
   
   /** Positionne le syst�me de num�rotation NESTED ou RING */
   public void setOrdering(int ordering) { this.ordering=ordering; }
   
   /** Retourne le syst�me de coordonn�es utilis� (Localisation.GAL, Localistion.ECLIPTIC, Localisation.ICRS) */
   public int getFrame() { return frame; }
   
   /** Retourne le syst�me de num�rotation NESTED ou RING */
   public int getOrdering() { return ordering; }
   
   /** Retourne l'header Fits (s'il existe) */
   public FrameHeaderFits getHeaderFits() { return frameHeaderFits; }
   
   /** Ajout d'un pixel */
   public void add(int order,long npix) {
      
      // On n'accepte pas en dessous de l'ordre 3 => sinon impossible � tracer
      // On ins�re les fils d'ordre 3 �quivalents
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
      header.readHeader(mis);       // On mange la premi�re ent�te FITS
//      String signature = header.getStringFromHeader(SIGNATURE);
//      if( signature==null ) signature = header.getStringFromHeader(SIGNATURE+"M");
//      if( signature==null ) throw new Exception("Not an HEALPix Multi-Level Fits map ("+SIGNATURE+" not found)");

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
   
   /** Retourne true si le pixel est un ascendant */
   public boolean isAscendant(Hpix hpix) { return isAscendant(hpix.getOrder(),hpix.getNpix()); }
   
   /** Retourne true si le pixel est un descendant */
   public boolean isDescendant(Hpix hpix) { return isDescendant(hpix.getOrder(),hpix.getNpix()); }
   
   /** Retourne true si le pixel est pr�sent */
   public boolean isIn(Hpix hpix) { return isIn(hpix.getOrder(),hpix.getNpix()); }
   
   /** Retourne true si le pixel est pr�sent, lui, ou l'un de ses fr�res */
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
