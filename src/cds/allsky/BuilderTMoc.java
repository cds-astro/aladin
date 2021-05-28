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

package cds.allsky;
import java.io.File;
import java.io.FileInputStream;
import java.util.Iterator;

import cds.aladin.HealpixProgen;
import cds.moc.SMoc;
import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.pixtools.Util;

/** Construction d'un TMOC à partir des données HpxFinder
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
    * Création du générateur de l'arbre des index.
    * @param context
    */
   public BuilderTMoc(Context context) {
      super(context);
   }

   public Action getAction() { return Action.TMOC; }

   public void run() throws Exception {
      build();
   }

   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      validateIndex();

      // détermination de l'ordre minimum pour les tuiles concaténées
      // soit indiqué via le parametre "order", soit déterminé à partir du order
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
            double tmin=0;
            double tmax=0;
            double exptime;
            
            try {
               if( mode==UNKNOWN ) {
                  mode = detectMode(json);
                  context.info("Time extraction from "+getTimeMode( mode )+" keywords");
               }

               if( mode==TMIN ) {
                  String s = cds.tools.Util.extractJSON("T_MIN", json);
                  if( s==null ) continue;
                  tmin = Double.parseDouble( s );
                  s= cds.tools.Util.extractJSON("T_MAX", json);
                  if( s==null ) tmax=tmin;
                  else tmax = Double.parseDouble(s  );

               } else if( mode==DATEOBS12 ) {
                  String s = cds.tools.Util.extractJSON("DATEOBS1", json);
                  if( s==null ) continue;
                  tmin = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
                  s = cds.tools.Util.extractJSON("DATEOBS2", json);
                  if( s==null ) continue;
                  tmax = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
                  if( Double.isNaN(tmax) ) tmax=tmin;
//                  System.out.println("tmin="+tmin+" tmax="+tmax+" date="+s);
                  
               } else if( mode==MJD ) {
                  String s= cds.tools.Util.extractJSON("MJD-OBS", json);
                  if( s==null ) continue;
                  tmin = Double .parseDouble( s );
                  s = cds.tools.Util.extractJSON("EXPTIME", json);
                  if( s==null ) tmax=tmin;
                  else {
                     exptime = Double.parseDouble( s );
                     tmax = tmin+exptime;
                  }

               } else if(mode==DATEOBS ) {
                  String s= cds.tools.Util.extractJSON("DATE-OBS", json);
                  if( s==null ) continue;
                  tmin = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
                  s = cds.tools.Util.extractJSON("EXPTIME", json);
                  if( s==null ) tmax=tmin;
                  else {
                     exptime = Double.parseDouble( s );
                     tmax = tmin+exptime;
                  }
                  
               } else if(mode==OBSDATE ) {
                  String s= cds.tools.Util.extractJSON("OBS-DATE", json);
                  if( s==null ) continue;
                  String s1 = cds.tools.Util.extractJSON("TIME-OBS", json);
                  s1= s1==null ? "" : "T"+s1;
                  tmin = Astrodate.JDToMJD( Astrodate.parseTime(s+s1, Astrodate.ISOTIME));
                  s = cds.tools.Util.extractJSON("EXPTIME", json);
                  if( s==null ) tmax=tmin;
                  else {
                     exptime = Double.parseDouble( s );
                     tmax = tmin+exptime;
                  }
               }

               double jdtmin = tmin+2400000.5;
               double jdtmax = tmax+2400000.5;
               
               addIt(maxOrder,npix,jdtmin,jdtmax,json);
               
            } catch( Exception e ) {
               context.warning("parsing error => "+json);
               if( mode==UNKNOWN ) throw e;
               continue;
            }
         }
         context.setProgress( progress++ );
      }
      writeIt();

   }
   
   protected void initIt() {
      tmoc = new TMoc();
      tmoc.bufferOn();
   }
   
   protected void addIt(int order, long npix, double jdtmin, double jdtmax,String json) {
      try {
         if( jdtmax<jdtmin ) {
            context.warning("Bad time range ["+jdtmin+".."+jdtmax+"] => assuming jdtmax..jdtmin =>["+json+"]");
            double t=jdtmax;
            jdtmax=jdtmin;
            jdtmin=t;
         }
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

   static private final int UNKNOWN        = 0;
   static private final int TMIN           = 1;
   static private final int MJD            = 2;
   static private final int DATEOBS        = 3;
   static private final int DATEOBS12      = 4;
   static private final int OBSDATE        = 5;

   
   private final String [] TIMEMODE = { "UNKNOWN", "T_MIN/T_MAX", "MJD-OBS/EXPTIME",
         "DATE-OBS/EXPTIME","DATEOBS1/DATEOBS2","OBS-DATE+TIME-OBS" };
   
   private String getTimeMode(int i) { return TIMEMODE[i]; }
   
   private int detectMode(String json ) throws Exception {
      String s1,s2;
      s1 = cds.tools.Util.extractJSON("T_MIN", json);
      if( s1!=null ) return TMIN;
      
      s1 = cds.tools.Util.extractJSON("MJD-OBS", json);
      if( s1!=null ) return MJD;
      
      s1 = cds.tools.Util.extractJSON("DATE-OBS", json);
      if( s1!=null ) return DATEOBS;
      
      s1 = cds.tools.Util.extractJSON("OBS-DATE", json);
      if( s1!=null ) return OBSDATE;
      
      s1 = cds.tools.Util.extractJSON("DATEOBS1", json);
      s2 = cds.tools.Util.extractJSON("DATEOBS2", json);
      if( s1!=null && s2!=null ) return DATEOBS12;
      
      throw new Exception("Not able to determine HpxFinder time keywords (ex: T_MIN [and T_MAX] or MJD-OBS [and EXPTIME],"
            + " or DATE-OBS [and EXPTIME],  or DATEOBS1 and DATEOBS2, or OBS-DATE+TIME-OBS");
   }

   private void initStat() { statNbFile=0; startTime = System.currentTimeMillis(); }

   // Mise à jour des stats
   private void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }

   /** Construction d'une tuile terminale. Lit le fichier est map les entrées de l'index
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
