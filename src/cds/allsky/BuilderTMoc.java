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

import cds.moc.SMoc;
import cds.moc.TMoc;
import cds.tools.Astrodate;
import cds.tools.pixtools.Util;

/** Construction d'un TMOC à partir des données HpxFinder
 * @author Pierre Fernique
 */
public class BuilderTMoc extends BuilderSMoc {

   private int mode=UNKNOWN;
   private TMoc tmoc = null;       

   /**
    * Création du générateur du TMOC
    * @param context
    */
   public BuilderTMoc(Context context) {
      super(context);
   }

   public Action getAction() { return Action.TMOC; }
   
   // Conversion en double en prenant en compte le cas d'un point décimal final
   private double parseDouble( String s ) throws Exception {
      int n = s.length()-1;
      if( s.charAt(n)=='.' ) s=s.substring(0,n);
      return Double.parseDouble(s);
   }
   
   /** Extraction du TMOC à partir des informations temporelles dans les propriétés JSON de la tuile */
   protected TMoc getTMoc(int order, String json) throws Exception { 
      double tmin=0;
      double tmax=0;
      double exptime;

      if( mode==UNKNOWN ) {
         mode = detectMode(json);
         context.info("Time extraction from "+getTimeMode( mode )+" keywords");
      }

      try {
         if( mode==TMIN ) {
            String s = cds.tools.Util.extractJSON("T_MIN", json);
            if( s==null ) throw new Exception();
            tmin = parseDouble( s );
            s= cds.tools.Util.extractJSON("T_MAX", json);
            if( s==null ) tmax=tmin;
            else tmax = parseDouble(s  );

         } else if( mode==DATEOBS12 ) {
            String s = cds.tools.Util.extractJSON("DATEOBS1", json);
            if( s==null ) throw new Exception();
            tmin = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
            s = cds.tools.Util.extractJSON("DATEOBS2", json);
            if( s==null ) throw new Exception();
            tmax = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
            if( Double.isNaN(tmax) ) tmax=tmin;
            //         System.out.println("tmin="+tmin+" tmax="+tmax+" date="+s);

         } else if( mode==MJD ) {
            String s= cds.tools.Util.extractJSON("MJD-OBS", json);
            if( s==null ) throw new Exception();
            tmin = Double .parseDouble( s );
            s = cds.tools.Util.extractJSON("EXPTIME", json);
            if( s==null ) tmax=tmin;
            else {
               exptime = parseDouble( s );
               tmax = tmin+exptime;
            }

         } else if(mode==DATEOBS ) {
            String s= cds.tools.Util.extractJSON("DATE-OBS", json);
            if( s==null ) throw new Exception();
            tmin = Astrodate.JDToMJD( Astrodate.parseTime(s, Astrodate.ISOTIME));
            s = cds.tools.Util.extractJSON("EXPTIME", json);
            if( s==null ) tmax=tmin;
            else {
               exptime = parseDouble( s );
               tmax = tmin+exptime;
            }

         } else if(mode==OBSDATE ) {
            String s= cds.tools.Util.extractJSON("OBS-DATE", json);
            if( s==null ) throw new Exception();
            String s1 = cds.tools.Util.extractJSON("TIME-OBS", json);
            s1= s1==null ? "" : "T"+s1;
            tmin = Astrodate.JDToMJD( Astrodate.parseTime(s+s1, Astrodate.ISOTIME));
            s = cds.tools.Util.extractJSON("EXPTIME", json);
            if( s==null ) tmax=tmin;
            else {
               exptime = parseDouble( s );
               tmax = tmin+exptime;
            }
         }

         double jdtmin = tmin+2400000.5;
         double jdtmax = tmax+2400000.5;

         if( jdtmax<jdtmin ) {
            context.warning("Bad time range ["+jdtmin+".."+jdtmax+"] => assuming jdtmax..jdtmin =>["+json+"]");
            double t=jdtmax;
            jdtmax=jdtmin;
            jdtmin=t;
         }

         TMoc tmoc = new TMoc(order);
         tmoc.add(jdtmin,jdtmax);
         return tmoc;

      } catch( Exception e ) {
         e.printStackTrace();
      }
      return null;
   }

   
   protected void initIt() throws Exception {
      tmoc = new TMoc(timeOrder);
      tmoc.bufferOn();
   }
   
   protected void info() {
      String s = maxSize>0 ? " maxSize="+cds.tools.Util.getUnitDisk(maxSize):"";
      String s1 = tmoc.getMem()>0 ? " currentSize="+cds.tools.Util.getUnitDisk(tmoc.getMem()):"";
      context.info("TMOC generation (timeOrder="+tmoc.getTimeOrder()+s+s1+")...");
   }
   
   protected void addIt(TMoc tmoc1, SMoc smoc1) throws Exception {
      tmoc.add(tmoc1);
      adjustSize(tmoc,false);
   }
   
   protected void cleanIt() throws Exception {
      String file = context.getOutputPath()+Util.FS+"TMoc.fits";
      (new File(file)).delete();      
   }
   
   protected void writeIt() throws Exception {
      adjustSize(tmoc,true);
      if( tmoc.isEmpty() ) throw new Exception("Empty MOC => not generated");
      String file = context.getOutputPath()+Util.FS+"TMoc.fits";
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
      
      context.error("Not able to determine HpxFinder time keywords (ex: T_MIN [and T_MAX] or MJD-OBS [and EXPTIME],"
            + " or DATE-OBS [and EXPTIME],  or DATEOBS1 and DATEOBS2, or OBS-DATE+TIME-OBS");
      
      throw new MocParsingException();
   }
}
