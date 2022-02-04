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
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.StringTokenizer;

import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.fits.Fits;
import cds.moc.SMoc;
import cds.mocmulti.MultiMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Vérification de la conformité IVOA 1.0 HiPS
 * @author Pierre Fernique [CDS]
 * @version 1.2 - 15 novembre 2021 - getProperties()
 * @version 1.1 - 23 avril 2019 - ajustement pour les Norder0 à 2
 * @version 1.0 - 23 avril 2017
 */
public class BuilderLint extends Builder {
   
   public static int TIMEOUT = 20000;   // timeout de 20s sur les connections réseaux
   
   private String path;         // path (ou préfixe URL) du HiPS à valider
   private boolean flagRemote;  // true s'il s'agit d'un HiPS distant
   private String FS;           // "\" ou "/" suivant le cas
   
   
   // Les propriétés (chargée par lintProperties)
   private MyProperties prop=null;
   
   // Informations à récupérer depuis le fichier properties
   private String id;           // Identificateur du HiPS
   private boolean flagImage;   // true pour un HiPS image
   private boolean flagCatalog; // true pour un HiPS catalogue
   private boolean flagCube;    // true pour un HiPS cube
   private boolean flagICRS;    // true si la référence spatiale est ICRS
   private boolean flagCDS;     // true si on fait en plus des vérifs propres au CDS
   private double skyFraction;  // portion de couverture du ciel [0..1]
   private int order;           // ordre du HiPS 
   private int minOrder;        // ordre minimal du HiPS
   private int version;         // numéro de version (1.4 => 140)
   private int tileWidth;       // Taille des tuiles
   private int frame;           // Frame du Hips
   private int depth;           // Epaisseur pour un HiPS cube
   private int bitpix;          // bitpix utilisé pour un HiPS catalog tuiles Fits
   private SMoc moc;        // MOC associé au HiPS
   private ArrayList<String> extensions; // Liste des extensions des tuiles (le point inclus)
   private boolean flagMinOrderSet=false; // true si le hips_order_min a été spécifié explicitement
   
  
   private boolean flagError;   // true si le HiPS n'est pas conforme au standard HiPS IVOA 1.0
   private boolean flagWarning; // true si le HiPS n'est que partiellement conforme au standard HiPS IVOA 1.0
   
   // Liste des mots clés requis (MUST)
   static private String [] PROP_REQ = {    "creator_did","obs_title","hips_version","hips_release_date",
         "hips_status","hips_frame","hips_order","hips_tile_format",
         "dataproduct_type","hips_cube_depth"
   };

   // Liste des mots clés requis (MUST)
   static private String [] PROP_SHOULD = { "obs_description","prov_progenitor","obs_regime","hips_creation_date",
         "hips_cat_nrows","hips_initial_ra","hips_initial_dec","hips_initial_fov",
         "t_min","t_max","em_min","em_max"
   };

   // Liste des mots clés recommandés (SHOULD)
   static private String [] PROP_OTHERS = { "publisher_id","obs_collection","obs_ack","bib_reference",
         "bib_reference_url","obs_copyright","obs_copyright_url","data_ucd",
         "hips_builder","hips_creator","hips_service_url","hips_estsize",
         "hips_tile_width","hips_pixel_cut","hips_data_range","hips_sampling",
         "hips_overlay","hips_skyval","hips_pixel_bitpix","data_pixel_bitpix",
         "hips_progenitor_url","hips_cube_firstframe","data_cube_crpix3",
         "data_cube_crval3","data_cube_cdelt3","hips_pixel_scale",
         "s_pixel_scale","client_category","client_sort_key","addendum_did",
         "moc_sky_fraction",
         "dataproduct_subtype"
   };
   
   // Liste des mots clés dont les valeurs sont numériques
   static private String [] PROP_NUMBERS = {
         "hips_cube_depth","hips_cat_nrows","hips_initial_ra","hips_initial_dec","hips_initial_fov",
         "t_min","t_max","em_min","em_max", "hips_estsize","hips_tile_width","hips_pixel_bitpix",
         "data_pixel_bitpix","hips_cube_firstframe","data_cube_crpix3",
         "data_cube_crval3","data_cube_cdelt3","hips_pixel_scale",
         "s_pixel_scale","moc_sky_fraction"
   };

   // Liste des mots clés dont les valeurs sont deux numériques consécutifs (intervales)
   static private String [] PROP_2NUMBERS = {
         "hips_pixel_cut","hips_data_range"
   };

   // Liste des mots clés dont les valeurs sont un code FITS BITPIX
   static private String [] PROP_BITPIX = {
         "hips_pixel_bitpix","data_pixel_bitpix"
   };

   // Liste des mots utilisés pour le hips_status
   static protected String [] STATUS_PUB    = { "private","public" };
   static protected String [] STATUS_MIRROR = { "master","mirror","partial" };
   static protected String [] STATUS_CLONE  = { "clonable","unclonable","clonableOnce" };

   // Liste des mots utilisés pour le hips_status
   static private String [] TILE_FORMAT  = { "fits","jpeg","png","tsv" };

   // Liste des mots utilisés pour le obs_regime
   static private String [] OBS_REGIME  = { 
         "Radio","Millimeter","Infrared","Optical","UV","EUV","X-ray","Gamma-ray" 
   };

   protected BuilderLint(Context context) { super(context); }

   public Action getAction() { return Action.LINT; }
   
   public void validateContext() throws Exception { }
   
   public void run() throws Exception {
      lint();
   }
   
   /**
    * Provide the MyProperties objects used for lint check
    * @throws Exception
    */
   protected MyProperties getProperties() throws Exception {
      if( prop==null ) throw new Exception("Properties not loaded yet. Use lin() before !");
      return prop;
   }
   
   /**
    * Check IVOA HiPS 1.0 compatibility
    * @return 1 ok, -1 ok but with warning, 0 uncompatible
    * @throws Exception
    */
   protected int lint() throws Exception{
      flagError=false;
      flagWarning=false;
      flagImage=flagCatalog=flagCube=false;
      flagICRS=false;
      minOrder=3;     // Par défaut
      order=-1;
      bitpix=-1;
      tileWidth=-1;
      skyFraction=-1;
      depth=1;
      id="null";
      flagCDS = context.isCDSLint();
      
      extensions = new ArrayList<>();
      
      // Vérification du path du HiPS à checker
      path = context.getOutputPath();
      if( path==null ) {
         throw new Exception("filepath or URL required");
      } else if( path.startsWith("http:") || path.startsWith("https:") ) {
         context.info("Lint remote HiPS: "+path);
         flagRemote=true;
         FS="/";
      } else {
         context.info("Lint local HiPS: "+path);
         flagRemote=false;
         FS=Util.FS;
      }
      
      // Vérification du fichier properties
      lintProperties();
      
      // Vérification du MOC
      lintMoc();
      
      // Test sur une tuile au hasard
      lintTile( 3 );
      
      // Test allsky
      lintAllsky();
      
      // Test metadata.xml
      if( flagCatalog ) lintMetadata();
      
      // Test des autres fichiers optionels
      lintMiscFiles();
      
      if( flagError ) context.error("*** HiPS "+id+" is not IVOA HiPS 1.0 compatible");
      else if( flagWarning ) context.warning("!!! HiPS "+id+" is IVOA HiPS 1.0 compatible but with warnings");
      else context.info("HiPS "+id+" is fully IVOA HiPS 1.0 compatible");
      
      return flagError ? 0 : flagWarning ? -1 : 1;
   }
   
   /** Test du metadata.xml */
   private void lintMetadata( ) throws Exception {
      boolean flagError=false,flagWarning=false;
      
      String f = path+FS+"metadata.xml";
      try {
         MyInputStream in = Util.openAnyStream( f,false,false,TIMEOUT );
         long type=in.getType();
         if( (type&MyInputStream.VOTABLE)==0 ) {
            context.error("Lint[4.4.3] \"metadata.xml\" format error (expecting \"votable\", found ["+MyInputStream.decodeType(type)+"])");
            flagError=true;
         }
         in.close();
      } catch( Exception e) {
         context.error("Lint[4.4.3] \"metadata.xml\" is missing");
         flagError=true;
      }
      
      if( !flagError ) context.info("Lint: \"metadata.xml\" ok");
      this.flagError|=flagError;
      this.flagWarning|=flagWarning;
   }
   
   /** Test des autres fichiers optionels */
   private void lintMiscFiles( ) throws Exception {
      
      // preview.jpg
      String f = path+FS+"preview.jpg";
      try {
         MyInputStream in = Util.openAnyStream( f,false,false,TIMEOUT );
         long type=in.getType();
         if( type!=MyInputStream.JPEG ) {
            context.error("Lint[4.4.4] \"preview.jpg\" format error (expecting \"jpeg\", found ["+MyInputStream.decodeType(type)+"])");
            flagError=true;
         }
         in.close();
      } catch( Exception e) {
         context.info("Lint[4.4.4] no \"preview.jpg\" file");
      }
      
      // index.html
      f = path+FS+"index.html";
      try {
         MyInputStream in = Util.openAnyStream( f,false,false,TIMEOUT );
         in.close();
      } catch( Exception e) {
         context.info("Lint[4.4.5] no \"index.html\" file");
      }
   }
   
   /** Test des allsky */
   private void lintAllsky( ) throws Exception {
      for( String ext : extensions ) {
         boolean found=false;
         for( int o=0; o<=3; o++ ) {
            String suffix = "Norder"+o+FS+"Allsky"+ext;
            String f = path+FS+suffix;
            try {
               MyInputStream in = Util.openAnyStream(f,false,false,TIMEOUT);
               long type = in.getType();
               if(   ext.equals(".jpg") && type!=MyInputStream.JPEG
                     ||  ext.equals(".png") && type!=MyInputStream.PNG
                     ||  ext.equals(".fits") && (type&MyInputStream.FITS)!=MyInputStream.FITS
                     ||  ext.equals(".tsv") && (type&MyInputStream.CSV)!=MyInputStream.CSV
                     ) {
                  context.error("Lint[4.2.1.3] Allsky format error (expecting \""+ext+"\", found ["+MyInputStream.decodeType(type)+"])");
                  flagError=true;
               }
               
               if( ext.equals(".fits") &&  (type&MyInputStream.GZ)!=0 ) {
                  context.warning("Lint[4.2.1.3] Allsky.fits gzipped (deprecated method)");
               }

               Fits fits = null;
               try {
                  if( !flagRemote ) {
                     if( ext.equals(".fits") ) {
                        fits = new Fits();
                        fits.loadFITS(in);
                     } else if( ext.equals(".jpg") ||  ext.equals(".png") ) {
                        fits = new Fits();
                        fits.loadPreview(in);
                     }
                  }
                  context.info("Lint: Allsky found ["+suffix+"] ok");
                  found=true;
               }finally { in.close(); in=null; }
            } catch( Exception e ) { }
         }
         if( !found ) {
            context.info("Lint[4.3.2] Allsky not found (order 0 to 3)");
         }
      }
   }

   /** Retourne au hasard un indice HEALPix se situant dans le HiPS à la profondeur max */
   private long getOneNpix() throws Exception {

      // Méthode 1: On prend au hasard une cellule du MOC de couverture
      if( moc!=null && !moc.isEmpty() ) {

         // On prend l'élément central d'un intervalle au hasard
         int nb = moc.getNbRanges();
         int i = (int)( Math.random()*nb);
         long npix = (moc.seeRangeList().begins(i)+ (moc.seeRangeList().ends(i)-1))/2L;

         // On prend le npix central ramené à la résolution du HiPS
         int orderMoc = moc.getMocOrder();
         if( orderMoc<order ) {
            int shift = (order - orderMoc)*2;
            long npix1 = npix<<shift ;
            long npix2 = (npix+1)<<shift;
            npix = (npix1+npix2)/2;

            // On prend la tuile qui le contient
         } else if( orderMoc>order ) {
            int shift = (orderMoc - order)*2;
            npix = npix>>shift;
         }

         // Attention, le Hips et le MOC n'ont pas le même système de coord
         int frameMoc = context.getFrameVal( moc.getSpaceSys() );
         if( frame!=frameMoc ) {
            double radec[] = CDSHealpix.pix2ang_nest( order, npix);
            radec = CDSHealpix.polarToRadec(new double[] { radec[0], radec[1] });
            Coord co = new Coord(radec[0],radec[1]);
            co = Localisation.frameToFrame(co, frame, frameMoc);
            radec = CDSHealpix.radecToPolar(new double[] {co.al, co.del});
            npix = CDSHealpix.ang2pix_nest(order, radec[0], radec[1]);
         }

         return npix;
      }

      //  Méthode 2: On cherche une tuile dans le premier répertoire de l'ordre le plus profond
      if( !flagRemote ) {
         File f = new File(path+FS+"Norder"+order);
         File [] dirs = f.listFiles();
         int i = (int)( Math.random()*dirs.length);
         if( i>=dirs.length ) i=dirs.length-1;
         File dir = dirs[i];
         dirs = dir.listFiles();
         i = (int)( Math.random()*dirs.length);
         if( i>=dirs.length ) i=dirs.length-1;
         File npix = dirs[i];
         String name = npix.getName();
         i = name.lastIndexOf('.');
         if( i==-1 ) i=name.length();
         String s = name.substring(4, i);
         return Long.parseLong(s);
      }

      // Bon ben c'est loupé
      return -1;
   }
   
   /** Test sur n tuiles au hasard */
   private void lintTile( int n ) throws Exception {
      boolean flagError=false,flagWarning=false;
      int lowOrder=-1;
      HashSet<String> dejaTeste = new HashSet<>();  // pour éviter de tester 2x la même tuile

      for( int j=0; j<n; j++ ) {
         long npix1 = getOneNpix();
         if( npix1==-1 ) {
            context.info("Lint: tile test cancelled");
            return;
         }
         
         // Dans le cas d'un cube, on teste aussi d'autres frames que 0
         int z=0;
         if( j>0 ) {
            z = (int)(Math.random()*depth);
            if( z>=depth ) z=depth-1;
         }

         for( String ext : extensions ) {
            boolean found=false;
            long npix = npix1;
            String first=null,last=null;
            lowOrder=-1;
               for( int o=order; o>=0; o--, npix/=4L) {
               String suffix = getFilePath(o, npix, z, FS)+ext;
               String f = path+FS+suffix;
               if( lowOrder==-1 || o<lowOrder ) first=suffix;
//               System.out.println("Loading "+f+"...");
               
               if( dejaTeste.contains(f) ) {
                  // Conserve le plus petit ordre trouvé
                  if( lowOrder==-1 || o<lowOrder ) lowOrder=o; 
               } else {
                  dejaTeste.add(f);
                  try {
                     MyInputStream in = Util.openAnyStream(f,false,false,TIMEOUT);
                     long type = in.getType();
                     if(   ext.equals(".jpg") && type!=MyInputStream.JPEG
                           ||  ext.equals(".png") && type!=MyInputStream.PNG
                           ||  ext.equals(".fits") && (type&MyInputStream.FITS)!=MyInputStream.FITS
                           ||  ext.equals(".tsv") && (type&MyInputStream.CSV)!=MyInputStream.CSV
                           ) {
                        context.error("Lint[4.2.1.3] tile format error (expecting \""+ext+"\", found ["+MyInputStream.decodeType(type)+"])");
                        flagError=true;
                     }
                     if( ext.equals(".fits") &&  (type&MyInputStream.GZ)!=0 ) {
                        context.warning("Lint[4.2.1.3] fits tile gzipped (deprecated method) ["+suffix+"]");
                     }

                     // On ne lit plus le contenu à distance, c'est trop long
                     Fits fits = null;
                     try {
                        if( !flagRemote ) {
                           if( ext.equals(".fits") ) {
                              fits = new Fits();
                              fits.loadFITS(in);
                           } else if( ext.equals(".jpg") ||  ext.equals(".png") ) {
                              fits = new Fits();
                              fits.loadPreview(in);
                           }
                        }
                        
                        found=true;
                        if( last==null ) last=suffix;
                     } finally { in.close(); in=null;}

                     // Conserve le plus petit ordre trouvé
                     if( lowOrder==-1 || o<lowOrder ) lowOrder=o;

                     boolean ok=true;
                     if( fits!=null ) {
                        if( fits.width != fits.height ) {
                           context.error("Lint[4.2.1] not square tile ["+fits.width+"x"+fits.height+"]");
                           flagError=true;
                        }
                        double o1 = Math.log10(fits.width)/Math.log10(2);
                        if( o1!=(long)o1 ) {
                           context.error("Lint[4.2.1] tile width error ["+fits.width+"x"+fits.height+"]");
                           ok=false;
                        }
                        if( tileWidth!=-1 && tileWidth!=fits.width ) {
                           context.error("Lint[4.2.1] tile width not conform to hips_tile_width ["+fits.width+"!="+tileWidth+"]");
                           ok=false;
                        }
                        if( ext.equals(".fits") ) {
                           if( bitpix!=-1 && bitpix!=fits.bitpix ) {
                              context.error("Lint[4.2.1] tile bitpix not conform to hips_pixel_bitpix ["+fits.bitpix+"!="+bitpix+"]");
                              ok=false;
                           }
                        }
                     }
                     if( !ok ) flagError=true;
                     //                  else context.info("Lint: tile test on ["+suffix+"] ok");
                     
//                     System.out.println("  "+f+" achieved");

                  } catch( Exception e1 ) {
                     if( found) {
                        String s1 = e1.getMessage()!=null ? " ("+e1.getMessage()+")":"";
                        if( flagMinOrderSet && o>=minOrder ) {
                           context.error("Lint[4.1] tile missing ["+o+"/"+npix+" => "+f+"]"+s1);
                           flagError=true;
                        }
                     }
                  }
               }
            }
         
            if( !found ) {
               if( flagMinOrderSet && lowOrder>minOrder ) {
                  context.error("Lint[4.1] tile hierarchy missing ["+first+" ... "+last+"] claiming to start at order "+minOrder);
                  flagError=true;
               }
            } else if( found ) {
               context.info("Lint: tile test hierarchy ["+first+" ... "+last+"] ok");
            }
         }
         
         // Si erreur on ne va pas faire un autre test
         if( flagError ) n=j;
      }
      
//    if( !flagError ) context.info("Lint: tile tests & tile hierarchy tests ok");
      if( flagMinOrderSet && lowOrder!=-1 && lowOrder!=minOrder ) context.error("Lint[4.1] min order found in the tile hierarchy ["+lowOrder+"] not conform [hips_min_order="+minOrder+"]");
      else {
         if( lowOrder>0 ) context.info("Lint: not all low tile hierarchy is provided [realMinOrder="+lowOrder+" (greater than 0)]");
         else if( lowOrder==0 ) context.info("Lint: Low orders provided [0..2]");
      }
      
      this.flagError|=flagError;
      this.flagWarning|=flagWarning;
   }

   
   /** Vérification du MOC associé au HiPS */
   private void lintMoc( ) throws Exception {
      boolean flagError=false,flagWarning=false;

      // Pas de MOC requis dans certains cas
      if( !flagICRS && skyFraction<1 && skyFraction>=0 ) return; 

      MyInputStream in = null;
      try {
         String f = path+FS+"Moc.fits";
         in = Util.openAnyStream(f,false,false,TIMEOUT);
      } catch( Exception e ) {
         if( flagICRS ) {
            context.warning("lint[4.4.2] \"Moc.fits\" file missing");
            flagWarning=true;
         } else {
            context.warning("lint[4.4.2] no \"Moc.fits\" file");
         }
      }

      try {
         moc = new SMoc();
         moc.read(in);
         in.close();

         String frame = moc.getSpaceSys();
         if( !frame.equals("C") ) {
            context.warning("Lint[4.4.2] \"Moc.fits\" coordinate system error, ICRS expecting, found ["+frame+"]");
            flagWarning=true;
         }
      } catch( Exception e ) {
         context.error("Lint[4.4.2] \"Moc.fits\" error");
         flagError=true;
      }

      if( !flagError ) context.info("Lint: \"Moc.fits\" ok");
      this.flagError|=flagError;
      this.flagWarning|=flagWarning;
   }

   /** Vérification du fichier properties */
   private void lintProperties( ) throws Exception {
      boolean flagError=false,flagWarning=false;
      int i;
      String s;
      try {
         String f = path+FS+"properties";
         MyInputStream in = Util.openAnyStream(f,false,false,TIMEOUT);
         InputStreamReader isr = new InputStreamReader( in, "UTF-8" );
         prop = new MyProperties();
         prop.load( isr );
         isr.close();
      } catch( Exception e1 ) {
         context.error("Lint[4.4] \"properties\" file missing");
         flagError=true;
         return;
      }

      boolean [] propReq    = new boolean[ PROP_REQ.length ];
      boolean [] propShould = new boolean[ PROP_SHOULD.length ];
      StringBuilder propUnref = null;
      ArrayList<String> propUnrefArray = new ArrayList<>();
      
      // Vérification du numéro de version HiPS
      s = prop.get("hips_version");
      version = getVersion(s);
      if( version==-1 ) {
         context.error("Lint[4.4.1] hips_version syntax error ["+s+"]");
         flagError=true;
      } else if( version<140 ) {
         context.info("Lint: hips_version precedes the IVOA HiPS 1.0 standard (hips_version 1.4) ["+s+"]");
      } else if( version>140 ) {
         context.warning("Lint: hips_version supersedes the IVOA HiPS 1.0 standard (hips_version 1.4)  ["+s+"]");
      } 

      // Vérification de la présence des mots clés requis et recommandés
      for( String key : prop.getKeys() ) {
         if( key.startsWith("#") ) continue;
         if( key.trim().length()==0 ) continue;
         
         // Vérification de la ou des valeurs associées
         boolean flagMult=false;
         String s1 = prop.get( key );
         if( s1==null || s1.length()==0 ) {
            context.warning("Lint: No associated value for keyword ["+key+"]");
         } else if( s1.indexOf("\t")>=0 ) flagMult=true;
         
         i=Util.indexInArrayOf(key, PROP_REQ);
         if( i>=0 ) {
            propReq[i]=true;
            if( flagMult ) {
               context.error("Lint[4.4.1] redundant value for keyword ["+key+"] not allowed");
               flagError=true;
            }
         } else {
            i=Util.indexInArrayOf(key, PROP_SHOULD);
            if( i>=0 ) propShould[i]=true;
            else {
               String keyx = withoutNumSuffix(key);
               i=Util.indexInArrayOf(keyx, PROP_OTHERS);
               if( i<0 && !propUnrefArray.contains(keyx) ) {
                  propUnrefArray.add(keyx);
                  if( propUnref==null ) propUnref = new StringBuilder( keyx );
                  else propUnref.append(","+keyx);
               }
            }
         }
      }
      
      // Mots-clés non référencés dans le document IVOA
      if( propUnref!=null ) context.info("Lint: unreferenced properties keyword ["+propUnref+"]");

      // Mots-clés requis
      // Seul dans le cas d'un cube, "hips_cube_depth" doit être indiqué
      s = prop.get("dataproduct_type");
      if( s!=null && !s.equals("cube") ) propReq[ Util.indexInArrayOf("hips_cube_depth", PROP_REQ) ] = true;
      for( i=0; i<propReq.length; i++ ) {
         if( !propReq[i] ) { context.error("Lint[4.4.1] mandatory keyword missing ["+PROP_REQ[i]+"]"); flagError=true; }
      }

      // Mots-clés recommandés
      // Seul dans le cas d'un cube, "hips_cube_depth" doit être indiqué
      s = prop.get("dataproduct_type");
      if( s!=null && !s.equals("catalog") ) propShould[ Util.indexInArrayOf("hips_cat_nrows", PROP_SHOULD) ] = true;
      for( i=0; i<propShould.length; i++ ) {
         if( !propShould[i] ) { context.warning("Lint[4.4.1] recommended keyword missing ["+PROP_SHOULD[i]+"]"); flagWarning=true; }
      }
      
      // Vérification des types de valeurs de certains mots clés
      for( String key : prop.getKeys() ) {
         if( key.startsWith("#") ) continue;
         
         // Vérification de la ou des valeurs associées
         String s1 = prop.get( key );
         if( s1==null ) s1="";
         
         // Est-ce un nombre ?
         i=Util.indexInArrayOf(key, PROP_NUMBERS);
         if( i>=0 ) {
            try {
               Double.parseDouble(s1);
            } catch( Exception e ) {
               context.warning("Lint[4.4.1] numeric value required for keyword "+key+" ["+s1+"]");
               flagWarning=true;
            }
         }
         
         // Est-ce un intervale ?
         i=Util.indexInArrayOf(key, PROP_2NUMBERS);
         if( i>=0 ) {
            StringTokenizer tok = new StringTokenizer(s1, " ");
            try {
               double deb = Double.parseDouble(tok.nextToken());
               double fin = Double.parseDouble(tok.nextToken());
               if( deb>=fin ) {
                  context.warning("Lint[4.4.1] range syntax error for keyword "+key+" ["+s1+"]");
                  flagWarning=true;
               }
            } catch( Exception e ) {
               context.error("Lint[4.4.1] numeric range required for keyword "+key+" ["+s1+"]");
               flagError=true;
            }
         }
         
         // Est-ce un BITPIX ?
         i=Util.indexInArrayOf(key, PROP_BITPIX);
         if( i>=0 ) {
            try {
               int bitpix = Integer.parseInt(s1);
               if( bitpix!=8 && bitpix!=16 && bitpix!=32 && bitpix!=64 && bitpix!=-32 && bitpix!=-64 ) {
                  throw new Exception();
               }
               if( key.equals("hips_pixel_bitpix") ) this.bitpix=bitpix;
            } catch( Exception e ) {
               context.warning("Lint[4.4.1] erroneous BITPIX value for keyword "+key+" ["+s1+"]");
               flagWarning=true;
            }
         }
        
         // Est-ce une URL ?
         if( key.endsWith("_url") ) {
            if( !s1.startsWith("http://") && !s1.startsWith("https://") && !s1.startsWith("ftp://")) {
               context.warning("Lint[4.4.1] url value required for keyword "+key+" ["+s1+"]");
               flagWarning=true;
            }
         }
      }

      // Vérification du creator_did
      s = prop.get("creator_did");
      if( s!=null ) {
         if( !s.startsWith("ivo://") || s.indexOf(' ')>=0 ) {
            context.error("Lint[4.4.1] creator_did must be an IVOID ["+s+"]");
            flagError=true;
         }
      }
      
      // Vérification du obs_title
      s = prop.get("obs_title");
      if( s!=null ) {
         if( s.length()>130 ) {
            context.warning("Lint[4.4.1] too long obs_title ["+s+"]");
            flagWarning=true;
         }
      }
     
      // Vérification du dataproduct_type
      s = prop.get("dataproduct_type");
      if( s!=null ) {
         if( s.equals("image") ) flagImage=true;
         else if( s.equals("catalog") ) flagCatalog=true;
         else if( s.equals("cube") ) flagCube=true;
         else {
            context.warning("Lint[4.4.1] unreferenced dataproduct_type ["+s+"]");
            flagWarning=true;
         }
      }
      
      if( !flagImage && !flagCatalog && !flagCube ) {
         context.warning("Lint: unreferenced HiPS type (no image, nor catalog, nor cube)");
      }
     
      // Vérification du hips_release_date
      s = prop.get("hips_release_date");
      if( s!=null ) {
         if( !checkDate(s) ) {
            context.error("Lint[4.4.1] not ISO 8601 date ["+s+"]");
            flagError=true;
         }
      }
     
      // Vérification du hips_status
      s = prop.get("hips_status");
      if( s!=null ) {
         StringBuilder statusUnref = null;
         boolean flagPub=false,flagMirror=false,flagClone=false;
         StringTokenizer tok = new StringTokenizer(s," ");
         while( tok.hasMoreTokens() ) {
            String s1 = tok.nextToken();
            i = Util.indexInArrayOf(s1, STATUS_PUB);
            if( i>=0 ) {
               if( flagPub ) {
                  context.error("Lint[4.4.1] hips_status error redundant definition [private/public]");
                  flagError=true;
               } else flagPub=true;
            } else {
               i = Util.indexInArrayOf(s1, STATUS_MIRROR);
               if( i>=0 ) {
                  if( flagMirror ) {
                     context.error("Lint[4.4.1] hips_status error redundant definition [master/mirror/partial]");
                     flagError=true;
                  } else flagMirror=true;
               } else {
                  i = Util.indexInArrayOf(s1, STATUS_CLONE);
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
         }
         
         if( statusUnref!=null ) {
            context.warning("Lint: unreferenced hips_status keywords ["+statusUnref+"]");
         }
      }
         
      // Vérification du hips_tile_format
      s = prop.get("hips_tile_format");
      if( s!=null ) {
         StringBuilder formatUnref = null;
         boolean flagCat=false;
         StringTokenizer tok = new StringTokenizer(s," ");
         while( tok.hasMoreTokens() ) {
            String s1 = tok.nextToken();
            i = Util.indexInArrayOf(s1, TILE_FORMAT);
            if( i<0 ) {
               if( s1.indexOf(",")>0 ) {
                  context.error("Lint[4.4.1] hips_tile_format comma separator error ["+s1+"]");
                  flagError=true;
               } else {
                  if( formatUnref==null ) formatUnref = new StringBuilder(s1);
                  else formatUnref.append(","+s1);
               }
            } else if( s1.equals("tsv") ) flagCat=true;
            
            if( s1.equals("jpeg") ) extensions.add(".jpg");
            else extensions.add("."+s1);
         }
         
         if( flagCatalog && !flagCat ) {
            context.warning("Lint[4.4.1] HiPS catalog without [tsv] hips_tile_format");
            flagWarning=true;
         }
         
         if( formatUnref!=null ) {
            context.warning("Lint: unreferenced hips_status keywords ["+formatUnref+"]");
         }
      }
         
      // Vérification du hips_order
      s = prop.get("hips_order");
      if( s!=null ) {
         try { order = Integer.parseInt(s); }
         catch( Exception e ) { }
         if( order<0 || order>29) {
            context.error("Lint[4.4.1] hips_order error ["+s+"]");
            flagError=true;
         }
      }  
      
      // Vérification du hips_order_min si présent
      s = prop.get("hips_order_min");
      if( s!=null ) {
         try { minOrder = Integer.parseInt(s); flagMinOrderSet=true; }
         catch( Exception e ) { }
         if( minOrder<0 || minOrder>29) {
            context.error("Lint[4.4.1] hips_order_min error ["+s+"]");
            flagError=true;
         }
      } else if( flagCatalog ) minOrder=0;
      
      // Vérification du hips_tile_width
      s = prop.get("hips_tile_width");
      if( s!=null ) {
         try { tileWidth = Integer.parseInt(s); }
         catch( Exception e ) { }
         double x = Math.log10(tileWidth)/Math.log10(2);
         if( x<0 || x!=(int)x ) {
            context.error("Lint[4.2.1] hips_tile_width error ["+s+"]");
            flagError=true;
         }
      }  
      
      // Vérification du hips_service_url et de sa cohérence avec celui 
      if( path.startsWith("http") ) {
         s = prop.get("hips_service_url");
         if( s!=null ) {
            if( !s.equals(path) ) {
               context.error("Lint[4.2.1] hips_service_url error ["+s+"] => not coherent with HiPSlist definition");
               flagError=true;
            }
         }  
      }
      
      // Vérification du hips_frame
      s = prop.get("hips_frame");
      if( s!=null ) {
         if( !(flagICRS=s.equals("equatorial")) && !s.equals("galactic") && !s.equals("ecliptic") ) {
            context.warning("Lint[4.4.1] unreferenced hips_frame ["+s+"]");
            flagWarning=true;
         }
         frame=context.getFrameVal(s);
      }
      
      // Vérification du moc_sky_fraction
      s = prop.get("moc_sky_fraction");
      if( s!=null ) {
         try {
            skyFraction = Double.parseDouble(s);
         } catch( Exception e ) {}
         if( skyFraction<0 || skyFraction>1 ) {
            context.warning("Lint[4.4.1] moc_sky_fraction value error ["+s+"]");
            flagWarning=true;
         }
      }
      
      // Vérification du hips_cube_depth
      s = prop.get("hips_cube_depth");
      if( s!=null ) {
         try {
            depth = Integer.parseInt(s);
         } catch( Exception e ) {}
         if( depth<=0 ) {
            context.error("Lint[4.4.1] hips_cube_depth value error ["+s+"]");
            flagError=true;
         }
      }
      
      // Vérification du obs_regime
      StringBuilder unrefObsRegime = null;
      s = prop.get("obs_regime");
      if( s!=null ) {
         StringTokenizer tok = new StringTokenizer(s, "\t");
         while( tok.hasMoreTokens() ) {
            String s1 = tok.nextToken();
            i = Util.indexInArrayOf(s1, OBS_REGIME);
            if( i<0 ) {
               if( s1.indexOf(",")>0 || s1.indexOf(" ")>0 ) {
                  context.warning("Lint[4.4.1] obs_regime comma or space separator not allowed ["+s1+"]");
                  flagWarning=true;
               } else {
                  if( unrefObsRegime==null ) unrefObsRegime = new StringBuilder(s1);
                  else unrefObsRegime.append(","+s1);
               }
            }
         }
         if( unrefObsRegime!=null ) {
            context.warning("Lint[4.4.1] unreferenced obs_regime ["+unrefObsRegime+"]");
            flagWarning=true;
         }
      }
      
      // Vérifications propres au CDS
      if( flagCDS ) {
         s = prop.get("client_category");
         if( s==null ) context.warning("Lint[CDS] client_category missing");
      }
      
      // Génération de l'identificateur du HiPS
      id = MultiMoc.getID(prop);
      
      if( !flagError ) context.info("Lint: \"properties\" file ok");
      else if( !flagError ) context.info("Lint: \"properties\" file warning");
      this.flagError|=flagError;
      this.flagWarning|=flagWarning;
   }
   
   // Example: 1.4 => 140
   private static int getVersion(String s) {
      try {
         return (int)( Double.parseDouble(s)*100 );
      } catch( Exception e ) { }
      return -1;
   }

   // Example: 2016-05-09[T10:39[:00]][Z]
   static public boolean checkDate(String s) {
      int mode=0;
      
      int n = s.length();
      if( s.endsWith("Z") ) n--;
      
      for( int i=0; i<n; i++ ) {
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
   
   // Retourne un mot clé HiPS sans son éventuel suffixe numérique "_nn"
   // ex: hipsgen_params_1 => hipsgen_params
   static private String withoutNumSuffix( String s ) {
      int i = s.lastIndexOf('_');
      if( i<0 ) return s;
      try { 
         Integer.parseInt(s.substring(i+1) ); 
         return s.substring(0,i);
      } catch( Exception e ) {}
      return s;
   }

   static private String getFilePath(int order, long npix,int z, String FS) {
      return
      "Norder" + order + FS +
      "Dir" + ((npix / 10000)*10000) + FS +
      "Npix" + npix
      + ( z<=0 ? "" : "_"+z);
   }
}
