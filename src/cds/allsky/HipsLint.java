// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.TimeZone;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.mocmulti.MultiMoc;
import cds.tools.Util;

public class HipsLint {
   
   static boolean flagColor=false;     // true si on a positionné -color ou -nocolor
   static boolean flagTileTest=true;   // false si on a positionné -nodeeptest
   static boolean flagMirrorTest=false;    // true si on veut également checker les HiPS mirrors

   public HipsLint() { }
   
   private void check(String hipsListUrl ) throws Exception {
      boolean mocServerReading = true;
      String s;
      MyProperties prop;
      Context context = new Context();
      boolean flagError=false, flagWarning=false, flagException=false, mirror=false;
      
      context.info("Starting HipsLint "+SDF.format(new Date())+" (based on Aladin "+Aladin.VERSION+")...");
      context.info(Context.getTitle("CHECKING HiPSserver ["+hipsListUrl+"]",'='));
      
      // On charge la totalité de la HipsList (bug Renaud)
      MyInputStream mis = null;
      byte [] buf;
      try {
         mis = Util.openAnyStream(hipsListUrl,false,false,BuilderLint.TIMEOUT);
         buf = mis.readFully();
         mis.close();
         mis=null;
      } catch( Exception e ) {
         context.error("Lint[5.1] HiPSList not available or timeout-"+(BuilderLint.TIMEOUT/1000)+"s)");
         return;
      } finally { if( mis!=null ) mis.close(); }
      
      InputStreamReader in = new InputStreamReader( new ByteArrayInputStream(buf), "UTF-8");
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
               mirror=false;
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
                     if( i==1 ) mirror=true;
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
            
            if( mirror && !flagMirrorTest ) {
               context.info("Lint: HiPS mirror skipped ["+id+"]");
               
            } else {

               context.info( Context.getTitle("Checking HiPS ["+id+"]"));
               context.hipslintTileTest = flagTileTest;
               if( !flagTileTest ) context.info("Lint: lite test => no random tile test");
               context.setOutputPath(u);
               BuilderLint builderLint = new BuilderLint( context );
               try {
                  int rep = builderLint.lint();
                  if( rep==0 ) flagError=true;
                  else if( rep==-1 ) flagWarning=true;

                  if( !checkHipslistProp( context, prop, builderLint.getProperties() ) ) flagError=true;

               } catch( Exception e) { flagException = true; e.printStackTrace(); }
            }
            
            
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
   
   /**
    * Check the properties described in the HiPSlist and the internal properties of the HiPS
    * @param context
    * @param propHipslist
    * @param p 
    * @return true if all is ok.
    */
   private boolean checkHipslistProp( Context context, MyProperties propHipslist, MyProperties p) {
      boolean flagError=false;
      for( String key : propHipslist.getKeys() ) {
         String val = propHipslist.get(key);
         String v = p.get(key);
         if( v!=null && !v.equals(val) ) {
            context.error("Lint[5.2] HiPSList incoherency: said \""+key+"="+val+"\", found \""+v+"\"");
            flagError=true;
         }
      }
      return flagError;
   }
   
   private static boolean TERM = false;  // true si on passe en mode terminal couleur
   
   public static void main(String[] args) {
      if( args.length==0 || args[0].equals("-h") ) {
         System.out.println("Usage: java -jar Hipslint [-color] [-mirrortest] [-notiletest] http://hips.server/hipslist [hipslisturl2 ...]");
         System.out.println("       HiPS server compatibility checker (IVOA HiPS 1.0 standard)");
         System.out.println("       -color: colourized console messages");
         System.out.println("       -mirrortest: also tests mirrored HiPS (by default discarded)");
         System.out.println("       -notiletest: basic testing only by avoiding random tile checks (faster)");
        
         
         System.exit(0);
      }
      
      Util.setUserAgent("Hipslint Aladin/"+Aladin.VERSION.substring(1));
      
      // On traite les paramètres optionnels (qu'on enleve du tableau le cas échéant)
      ArrayList<String> args1 = new ArrayList<>();
      for( String s : args ) {
         if( s.equalsIgnoreCase("-color") ) { TERM=true; flagColor=true; }
         else if( s.equalsIgnoreCase("-nocolor") ) { TERM=false; flagColor=true; }
         else if( s.equalsIgnoreCase("-notiletest") ) { flagTileTest=false; }
         else if( s.equalsIgnoreCase("-mirrortest") ) { flagMirrorTest=false; }
         else args1.add(s);
      }
      
      try {
         HipsLint lint = new HipsLint();
         for( String hipsListUrl : args1 ) lint.check(hipsListUrl);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
   static public SimpleDateFormat SDF;
   static {
       SDF = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
       SDF.setTimeZone(TimeZone.getDefault());
   }


}
