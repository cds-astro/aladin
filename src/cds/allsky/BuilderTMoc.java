// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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

package cds.allsky;
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import cds.aladin.HealpixProgen;
import cds.moc.SMoc;
import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.pixtools.Util;

/** Construction d'un TMOC � partir des donn�es HpxFinder
 * TEST TEST TEST TEST
 *
 * @author Pierre Fernique
 */
public class BuilderTMoc extends Builder {

   static public final int MINORDER = 3;   // niveau minimal pris en compte

   protected int maxOrder;
   protected int statNbFile;
   protected long startTime,totalTime;

   /**
    * Cr�ation du g�n�rateur de l'arbre des index.
    * @param context
    */
   public BuilderTMoc(Context context) {
      super(context);
   }

   public Action getAction() { return Action.TMOC; }

   public void run() throws Exception {
      build();
   }

   // Valide la coh�rence des param�tres pour la cr�ation des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      validateIndex();

      // d�termination de l'ordre minimum pour les tuiles concat�n�es
      // soit indiqu� via le parametre "order", soit d�termin� � partir du order
      // de l'index et de la taille typique d'une image.
      int o = context.getOrder();
      if( o==-1 ) {
         maxOrder = Util.getMaxOrderByPath( context.getHpxFinderPath() );
         if( maxOrder==-1 ) throw new Exception("HpxFinder seems to be not yet ready ! (order=-1)");
         context.info("Order retrieved from HpxFinder => "+maxOrder);

         context.setOrder(maxOrder); // juste pour que les statistiques de progression s'affichent correctement
         
      } else maxOrder = o;

      context.mocIndex=null;
      context.initRegion();
   }

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, totalTime,0,0);
   }
   
   
   static private final int UNKNOWN    = 0;
   static private final int TMINMAX    = 1;
   static private final int MJDEXPTIME = 2;
   static private final int DATEOBS    = 3;
   
   private int mode=UNKNOWN;
   
   private TMoc tmoc = null; 

   public void build() throws Exception {
      initStat();

      String output = context.getOutputPath();
      String hpxFinder = context.getHpxFinderPath();
      
      SMoc moc = new SMoc();
      moc.read(hpxFinder+Util.FS+"Moc.fits");
      moc.setMocOrder(maxOrder);
      
      long progress=0L;
      context.setProgressMax(moc.getNbValues());
      
      initIt();
      
      Iterator<Long> it = moc.valIterator();
      while( it.hasNext() ) {
         long npix = it.next();
         String file = Util.getFilePath(hpxFinder, maxOrder, npix);
         HealpixProgen out = createLeave(file);
         if( out==null ) {
            context.warning("Missing HpxFinder tile "+maxOrder+"/"+npix+" => ignored ("+file+")");
            continue;
         }
         for( String key : out ) {
            String json = out.get(key).getJson();
            double tmin;
            double tmax;
            double exptime;
            
            try {
               if( mode==UNKNOWN ) mode = detectMode(json);

               if( mode==TMINMAX ) {
                  String s = cds.tools.Util.extractJSON("T_MIN", json);
                  if( s==null ) continue;
                  tmin = Double.parseDouble( s );
                  s= cds.tools.Util.extractJSON("T_MAX", json);
                  if( s==null ) continue;
                  tmax = Double.parseDouble(s  );
                  
               } else if( mode==DATEOBS ) {
                  String s = cds.tools.Util.extractJSON("DATEOBS1", json);
                  if( s==null ) continue;
                  tmin = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
                  s = cds.tools.Util.extractJSON("DATEOBS2", json);
                  if( s==null ) continue;
                  tmax = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
                  if( Double.isNaN(tmax) ) tmax=tmin;
//                  System.out.println("tmin="+tmin+" tmax="+tmax+" date="+s);
                  
               } else {
                  String s= cds.tools.Util.extractJSON("MJD-OBS", json);
                  if( s==null ) continue;
                  tmin = Double .parseDouble( s );
                  s = cds.tools.Util.extractJSON("EXPTIME", json);
                  if( s==null ) continue;
                  exptime = Double.parseDouble( s );
                  tmax = tmin+exptime;

               }
               
               double jdtmin = tmin+2400000.5;
               double jdtmax = tmax+2400000.5;
               
               addIt(maxOrder,npix,jdtmin,jdtmax);
               
            } catch( Exception e ) {
               e.printStackTrace();
               context.warning("parsing error => "+json);
               continue;
            }
         }
         context.setProgress( progress++ );
      }
      writeIt();

   }
   
   protected void initIt() {
      tmoc = new TMoc();
   }
   
   protected void addIt(int order, long npix, double jdtmin, double jdtmax) {
      try {
         tmoc.add(jdtmin,jdtmax);
      } catch( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   
   protected void writeIt() throws Exception {
      String file = context.getOutputPath()+Util.FS+"TMoc.fits";
//      tmoc.toMocSet();
      tmoc.write(file);
   }
   
   private int detectMode(String json ) throws Exception {
      String s = cds.tools.Util.extractJSON("T_MIN", json);
      if( s!=null ) return TMINMAX;
      s = cds.tools.Util.extractJSON("EXPTIME", json);
      if( s!=null ) return MJDEXPTIME;
      s = cds.tools.Util.extractJSON("DATEOBS1", json);
      if( s!=null ) return DATEOBS;
      throw new Exception("Not able to determine HpxFinder time keywords (ex: T_MIN and T_MAX or MJD-OBS and EXPTIME, or DATEOBS1 and DATEOBS2");
   }

   private void initStat() { statNbFile=0; startTime = System.currentTimeMillis(); }

   // Mise � jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }

   /** Construction d'une tuile terminale. Lit le fichier est map les entr�es de l'index
    * dans une TreeMap */
   private HealpixProgen createLeave(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
      HealpixProgen out = new HealpixProgen();
      out.loadStream( new FileInputStream(f));
      updateStat();
      return out;
   }
}
