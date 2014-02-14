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

package cds.allsky;

import java.io.FileInputStream;
import java.io.RandomAccessFile;

import cds.aladin.MyInputStream;
import cds.fits.HeaderFits;

/** Permet de générer une arborescence de tuiles à partir d'une map Healpix
 * C'EST PAS TERMINE !!!
 * @author Anaïs Oberto & Pierre Fernique [CDS]
 */
public class BuilderMapTiles extends Builder {
   private HeaderFits headerFits;
   private long initialOffsetHpx;

   public BuilderMapTiles(Context context) {
      super(context);
   }
   
   public Action getAction() { return Action.MAPTILES; }
   
   public void run() throws Exception {
   }
   
   public void validateContext() throws Exception {
      validateMap();
      validateOutput();
   }
   
   private void validateMap() throws Exception {
      String map = context.getInputPath();
      MyInputStream in=null;
      try {
         in = new MyInputStream( new FileInputStream(map));
         headerFits = new HeaderFits(in);
         int naxis = headerFits.getIntFromHeader("NAXIS");
         // S'agit-il juste d'une entête FITS indiquant des EXTENSIONs
         if( naxis<=1 && headerFits.getStringFromHeader("EXTEND")!=null ) {
            // Je saute l'éventuel baratin de la première HDU
            try {
               int naxis1 = headerFits.getIntFromHeader("NAXIS1");
               in.skip(naxis1);
            } catch( Exception e) {}

            // On se cale sur le prochain segment de 2880
            in.skipOnNext2880();
            headerFits = new HeaderFits(in);
         }

         initialOffsetHpx = in.getPos();
      } finally { if( in!=null ) in.close(); }
   }
   
   private void build() throws Exception {
      int idxTForm    = 0;                                            // Indice du TFORM concerné
      long nside      = headerFits.getIntFromHeader("NSIDE");         // NSIDE de la map
      int sizeRecord  = headerFits.getIntFromHeader("NAXIS1");        // Taille des enregistrements
      int nRecord     = headerFits.getIntFromHeader("NAXIS2");        // Nombre d'enregistrements
      int nField      = headerFits.getIntFromHeader("TFIELDS");       // Nombre de champs par enregistrement
      String ordering = headerFits.getStringFromHeader("ORDERING");   // Mode d'ordonnancement HEALPIX
      double badData  = Double.NaN;                                   // Une valeur BADDATA particulière ?
      String coordsys = "G";                                          // Référence spatiale (G par défaut)
      double bzero    = 0;                                            // BZERO éventuel
      double bscale   = 1;                                            // BSCALE éventuel
      double blank    = Double.NaN;                                   // BLANK éventuel
      boolean isPartial=false;                                        // S'agit-il d'un HEALPIX partiel ou global
      int bitpix;                                                     // BITPIX correspondant au type de données

      try { badData  = Double.parseDouble(headerFits.getStringFromHeader("BAD_DATA")); } catch( Exception e ) { }
      try { coordsys = headerFits.getStringFromHeader("COORDSYS");                     } catch (Exception e ) { }
      try { bzero    = headerFits.getDoubleFromHeader("BZERO");                        } catch( Exception e ) { }
      try { bscale   = headerFits.getDoubleFromHeader("BSCALE");                       } catch( Exception e ) { }
      try { blank    = headerFits.getDoubleFromHeader("BLANK");                        } catch( Exception e ) { }
      
      
      // Détermination des noms et des types de champs
      char [] typeHpx = new char[nField];           // Type de données pour le champ
      int [] lenHpx = new int[nField];              // Nombre d'items du champ
      String [] tfieldNames = new String[nField];   // Nom pour le champ

      for( int i=0; i<nField; i++ ) {

         // Récupération du nom du champ
         String fName = headerFits.getStringFromHeader("TTYPE"+(i+1));
         if (fName==null) fName = "TTYPE"+(i+1);
         tfieldNames[i] = fName;

         // Récupération du format d'entrée
         String s=null;
         try { s = headerFits.getStringFromHeader("TFORM"+(i+1)); } catch( Exception e1 ) {}
         int k;

         // Nombre d'items par champ
         lenHpx[i]=0;
         for( k=0; k<s.length() && Character.isDigit(s.charAt(k)); k++ ) {
            lenHpx[i] = lenHpx[i]*10 + (s.charAt(k)-'0');
         }
         if( k==0 ) lenHpx[i]=1;
         typeHpx[i] = s.charAt(k);
      }
      
      // Détermination du BITPIX
      bitpix = typeHpx[idxTForm];

      
      RandomAccessFile f = null;
      try {
         f = new RandomAccessFile(context.getInputPath(), "rw");
         
      } finally { if( f!=null ) f.close(); }
   }

   // Retourne le BITPIX correspondondant à un type de données
   private int getBitpixFromFormat(char t) throws Exception {
      int bitpix = t=='B' ? 8 : t=='I' ? 16 : t=='J' ? 32 : t=='K' ? 64
            : t=='E' ? -32 : t=='D' ? -64 : 0;
      if( bitpix==0 ) throw new Exception("Unsupported data type => ["+t+"]");
      return bitpix;
   }

   
   public boolean isAlreadyDone() { return false; }
   

}