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

import java.io.InputStreamReader;
import java.util.StringTokenizer;

import cds.aladin.MyProperties;
import cds.mocmulti.MultiMoc;
import cds.tools.Util;

public class HipsLint {
   
   static boolean flagColor=false;   // true si on a positionné -color ou -nocolor

   public HipsLint() { }
   
   private void check(String hipsListUrl ) throws Exception {
      boolean mocServerReading = true;
      String s;
      MyProperties prop;
      Context context = new Context();
      boolean flagError=false, flagWarning=false, flagException=false;
      
      context.info(Context.getTitle("CHECKING HiPSserver ["+hipsListUrl+"]",'='));
      InputStreamReader in = new InputStreamReader( Util.openAnyStream(hipsListUrl,false,false,BuilderLint.TIMEOUT), "UTF-8");
      
      while( mocServerReading ) {
         
         try {
            context = new Context();
            if( flagColor ) context.setTerm( TERM );
            prop = new MyProperties();
            mocServerReading = prop.loadRecord(in);
            String id=MultiMoc.getID(prop);
            if( prop.size()==0 || id==null ) continue;
            
            context.run(Context.getTitle("Checking HiPSList record ["+id+"]"));

           // Vérification des 4 keywords requis
            // creator_did, hips_release_date, hips_service_url, hips_status
            s = prop.get("creator_did");
            if( s==null ) {
               context.error("Lint[5.2] HiPSList \"creator_did\" keyword is mandatory");
               flagError=true;
            }
            s = prop.get("hips_release_date");
            if( s==null ) {
               context.error("Lint[5.2] HiPSList \"hips_release_date\" keyword is mandatory");
               flagError=true;
            } else {
               context.info("Lint: \"hips_release_date\" ["+s+"]");
               if( !BuilderLint.checkDate(s) ) {
                  context.error("Lint[4.4.1] HiPSList not ISO 8601 date ["+s+"]");
                  flagError=true;
               }
            }
            s = prop.get("hips_status");
            if( s==null ) {
               context.error("Lint[5.2] HiPSList \"hips_status\" keyword is mandatory");
               flagError=true;
            } else { 
               context.info("Lint: \"hips_status\" ["+s+"]");
               StringBuilder statusUnref = null;
               boolean flagPub=false,flagMirror=false,flagClone=false;
               StringTokenizer tok = new StringTokenizer(s," ");
               while( tok.hasMoreTokens() ) {
                  String s1 = tok.nextToken();
                  int i = Util.indexInArrayOf(s1, BuilderLint.STATUS_PUB);
                  if( i>=0 ) {
                     if( flagPub ) {
                        context.error("Lint[4.4.1] hips_status error redundant definition [private/public]");
                        flagError=true;
                     } else flagPub=true;
                  } else {
                     i = Util.indexInArrayOf(s1, BuilderLint.STATUS_MIRROR);
                     if( i>=0 ) {
                        if( flagMirror ) {
                           context.error("Lint[4.4.1] hips_status error redundant definition [master/mirror/partial]");
                           flagError=true;
                        } else flagMirror=true;
                     } else {
                        i = Util.indexInArrayOf(s1, BuilderLint.STATUS_CLONE);
                        if( i>=0 ) {
                           if( flagClone ) {
                              context.error("Lint[4.4.1] hips_status error redundant definition [clonable/unclonable/clonableOnce]");
                              flagError=true;
                           } else flagClone=true;
                        } else {
                           if( s1.indexOf(",")>0 ) {
                              context.error("Lint[4.4.1] hips_status comma separator error ["+s1+"]");
                              flagError=true;
                           } else {
                              if( statusUnref==null ) statusUnref = new StringBuilder(s1);
                              else statusUnref.append(","+s1);
                           }
                        }
                     }
                  }

                  if( statusUnref!=null ) {
                     context.warning("Lint: unreferenced hips_status keywords ["+statusUnref+"]");
                  }
               }
            }
            
            String u = s = prop.get("hips_service_url");
            if( s==null ) {
               context.error("Lint[5.2] HiPSList \"hips_service_url\" keyword is mandatory");
               flagError=true;
            }
            
            context.info( Context.getTitle("Checking HiPS ["+id+"]"));

            context.setOutputPath(u);
            BuilderLint builderLint = new BuilderLint( context );
            try {
               int rep = builderLint.lint();
               if( rep==0 ) flagError=true;
               else if( rep==-1 ) flagWarning=true;
            } catch( Exception e) { flagException = true; e.printStackTrace(); }
            
         } catch( Exception e ) {
            e.printStackTrace();
         }
      }
      in.close();
      
      if( flagException ) context.info("!!! ["+hipsListUrl+"] has not been fully checked => partial validation");
      if( flagError ) context.info("*** ["+hipsListUrl+"] is not IVOA HiPS 1.0 compatible");
      else if( flagWarning ) context.info("!!! ["+hipsListUrl+"] is IVOA HiPS 1.0 compatible but with warnings !");
      else context.info("*** ["+hipsListUrl+"] is fully IVOA HiPS 1.0 compatible");
   }
   
   private static boolean TERM = false;  // true si on passe en mode terminal couleur
   
   public static void main(String[] args) {
      if( args.length==0 || args[0].equals("-h") ) {
         System.out.println("Usage: java -jar Hipslint [-color|-nocolor] http://hips.server/hipslist [hipslisturl2 ...]");
         System.out.println("       HiPS server compatibility checker (IVOA HiPS 1.0 standard)");
         System.exit(0);
      }
      
      // On traite le paramètre -color (qu'on enleve du tableau le cas échéant)
      if( args.length>1 ) {
         String args1[] = new String[ args.length-1 ];
         for( int i=0,j=0; i<args.length; i++,j++ ) {
            if( args[i].equalsIgnoreCase("-color") ) { TERM=true; j--; flagColor=true; }
            else if( args[i].equalsIgnoreCase("-nocolor") ) { TERM=false; j--; flagColor=true; }
            else if( j<args1.length ) args1[j]=args[i];
         }
         if( flagColor ) args=args1;
      }
      
      try {
         HipsLint lint = new HipsLint();
         for( String hipsListUrl : args ) lint.check(hipsListUrl);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
}
