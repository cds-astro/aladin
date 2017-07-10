// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

package cds.mocmulti;

import java.io.BufferedInputStream;

// Copyright 2011 - UDS/CNRS
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.astro.Astroframe;
import cds.astro.Coo;
import cds.astro.Ecliptic;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.moc.MocCell;


/**
 * Array of MOCs manager
 * 
 * @author Pierre Fernique [CDS]
 * @version 0.9 march 2015 prototype creation
 * @version 1.0 feb 2015 ready for production
 * @version 1.1 Apr 2015 Lot of improvements...
 */
public class MultiMoc implements Iterable<MocItem> {
   
   // Clés spécials
   static public String KEY_ID        = "ID";                // identificateur de l'enregistrement (ex: CDS/P/DSS2/color)
   static public String KEY_TIMESTAMP = "TIMESTAMP";         // Pour l'estampillage des propriétés
   static public String KEY_REMOVE    = "MOCSERVER_REMOVE";  // Pour indiquer un enregistrement supprimé
   
   final private String COORDSYS ="C";   // Coordinate system (HEALPix convention => G=galactic, C=Equatorial, E=Ecliptic)
   
   protected HashMap<String, MocItem> map; // Liste des MocItem repéré par leur ID (ex: CDS/P/2MASS/J)
   private ArrayList<String> tri;        // Liste des IDs afin de pouvoir les parcourirs en ordre alphanumérique
   protected int mocOrder=-1;              // Better MOC order
   private ArrayList<MyProperties> except = null;   // List of exceptions and associating rewriting rules
   private MyProperties example = null;  // List of existing properties with examples
   
   public MultiMoc() {
      map = new HashMap<String, MocItem>(30000);
      tri = new ArrayList<String>(30000);
   }
   
   /** MultiMoc creation from a binary dump file
    * @param binaryDumpFile Binary dump file path
    * @return Multimoc 
    * @throws Exception
    */
   static public MultiMoc createFromDump(String binaryDumpFile) throws Exception {
      BinaryDump dump = new BinaryDump();
      return dump.load(binaryDumpFile);
   }
   
   /** Add or replace a MOC to the MultiMoc. The MOC is sorted (if required) for fast access
    * @param mocId  MOC identifier (unique)
    * @param moc MOC to memorize
    */
   public void add(String mocId, HealpixMoc moc, MyProperties prop, long dateMoc, long dateProp) throws Exception {
      if( moc!=null ) {
         int o = moc.getMocOrder();
         if( o==HealpixMoc.MAXORDER ) o = moc.getMaxOrder();  // A cause du bug
         if( mocOrder<o) mocOrder=o;
         moc.sort();
      }
      MocItem mi = new MocItem(mocId,moc,prop,dateMoc,dateProp);
      add( mi );
   }
   
   /** Add or replace a MyProperties to the MultiMoc. 
    * Le MOC reste à null, la date du MOC à 0. La date du MyProperties
    * est prise directement de la valeur de la propriété TIMESTAMP (si présent)
    * @param mocId Identificateur de l'enregistrement
    * @param prop Propriétés
    * @throws Exception
    */
   public void add(MyProperties prop ) throws Exception {
      String id = getID(prop);
      MocItem mi = new MocItem(id,prop);
      add( mi );
   }
   
   /** Remove a MOC
    * @param mocId MOC identifier
    */
   public void remove(String mocId) {
      MocItem mi = map.get(mocId);
      if( mi==null ) return;
      map.remove(mocId);
      tri.remove(mocId);
   }
   
   /** Add directly a MocItem */
   public void add(MocItem mi) {
      if( map.put(mi.mocId,mi)==null ) tri.add(mi.mocId);
   }
   
   /** Return directly a MocItem */
   public MocItem getItem(String mocId) {
      return map.get(mocId);
   }
   
   /** Return the MOC associated to a mocId, null if not found */
   public HealpixMoc getMoc(String mocId) {
      MocItem mi = map.get(mocId);
      return mi==null ? null : mi.moc;
   }
   
   /** Return the Properties associated to a mocId, null if not found */
   public MyProperties getProperties(String mocId) {
      MocItem mi = map.get(mocId);
      return mi==null ? null : mi.prop;
   }
   
   
   /** Return the list of current properties known by the MultiMoc, with one example by property */
   public MyProperties getProperties() {
      MyProperties prop = new MyProperties();
      
      for( MocItem mi : this ) {
         if( mi.prop==null ) continue;
         
//         Enumeration<String> e = mi.prop.keys();
//         while( e.hasMoreElements() ) {
//            String k = e.nextElement();
         
         for( String k : mi.prop.getKeys() ) {
            if( prop.get(k)!=null ) continue;  // déjà pris en compte
            String s = mi.prop.get(k);
            if( s==null ) s="null for "+mi.mocId;
            int i = s.indexOf('\t');
            if( i>=0 ) s=s.substring(0,i);
            if( s.length()>=70 ) s = s.substring(0,67)+"...";
            prop.put(k,s+" [ex in "+mi.mocId+"]");
         }
      }
      return prop;
   }
   

   /** Clear the multiMoc */
   public void clear() {
      map.clear();
      tri.clear();
   }
   
   static private Healpix hpx = new Healpix();
   
   public String adjustProp(MyProperties prop, String id, HealpixMoc moc) {
      
      String s;
      String mocId = getID(prop,id);
      
      // On ajoute manu-militari un id canonique
      prop.insert(KEY_ID, mocId);
      
      // Ajout de propriétés propres au MOC
      if( moc!=null ) {
         prop.replaceValue("moc_sky_fraction", Unite.myRound( moc.getCoverage() )+"" );

         s = moc.getProperty("MOCORDER");

         // Petit bricolage horrible pour contourner les MocOrder que l'on a oublié
         // d'indiquer
         if( s==null || s.equals(""+HealpixMoc.MAXORDER) || s.equals("0") || s.equals("-1")) {
            int maxOrder = moc.getMaxOrder();
            if( maxOrder==0 ) {
               String s1 = prop.get("hips_order");
               if( s1!=null ) s=s1;
               else s="10";
            } else s=maxOrder+"";
         }
         prop.replaceValue("moc_order",s);
         
         // Détermination d'une position initiale si non encore renseignée
         String ra,dec,fov;
         ra  = prop.get("obs_initial_ra");
         dec = prop.get("obs_initial_dec");
         fov = prop.get("obs_initial_fov");
         
         if( ra==null || dec==null ) {
            ra  = prop.get("hips_initial_ra");
            dec = prop.get("hips_initial_dec");
         }
         
         if( ra==null || dec==null || fov==null ) {
            if( fov==null ) {
               
               // On va préférer prendre le moc_order indiqué dans les properties
               // pour éviter de récupérer le bug sur le MocOrder
               try {
                  int n = Integer.parseInt( prop.get("moc_order"));
                  HealpixMoc mm = new HealpixMoc();
                  mm.setMocOrder(n);
                  fov =  mm.getAngularRes()+"";
               } catch( Exception e) {
                  fov = moc.getAngularRes()+"";
               }
            }
            if( ra==null || dec==null ) {
               if( moc.isAllSky() ) { ra="0"; dec="+0"; }
               else {
                  try {
                     int order = moc.getMocOrder();
                     long pix = moc.pixelIterator().next();
                     double coo[] = hpx.pix2ang(order,pix);
                     ra = coo[0]+"";
                     dec = coo[1]+"";
                  } catch( Exception e ) { }
               }
            }
            if( ra!=null )  prop.replaceValue("obs_initial_ra",ra);
            if( dec!=null ) prop.replaceValue("obs_initial_dec",dec);
            prop.replaceValue("obs_initial_fov",fov);
         }
      }
      
      return mocId;
   }
   
   
   /**
    * Addition/update of all MOCs of a dedicated directory with possible comparaison with previous state
    * @param dirs input directory list
    * @param exceptFile Exception.prop file path, or null
    * @param oMM previous MultiMoc, or null
    * @param flagWithMoc false for avoiding to load Moc (only Prop)
    * @param out
    * @return true if something has been changed compared to oMM
    * @throws Exception
    */
   public boolean reload(String [] dirs, String exceptFile, MultiMoc oMM,boolean flagWithMoc, PrintWriter out) throws Exception {
      int i=0; 
      String mocId=null;
      long dateMoc,dateProp;
      int nbCreation=0;
      int nbReused=0;
      int originalSize = oMM==null ? 0 : oMM.map.size();
      long t = System.currentTimeMillis();
      String s;
      
      
      // (Re)chargement du fichiers d'exceptions
      try { loadException(exceptFile); } catch( Exception e) {}
      
      long t1=System.currentTimeMillis();
      
      // Parcours des fichiers présents dans les répertoires
      for( String dir : dirs) {
         File f = new File(dir);

         File [] listF = f.listFiles();
         if( listF==null ) continue;

         for( File f1 : listF ) {
            String name = f1.getAbsolutePath();

            // On ne prend pas individuellement en compte les fichiers de properties
            // si le MOC existe
            if( name.endsWith(".prop") ) {
               int offset = name.lastIndexOf('.');
               String mocFile = name.substring(0,offset)+".fits";
               if( (new File(mocFile)).exists()) continue;
            }

            HealpixMoc moc;
            MyProperties prop;

            try {
               //            System.out.print("Loading "+f1.getName()+"...");
               String filename = f1.getAbsolutePath();
               dateMoc = (new File(filename)).lastModified();
               //            dateMoc = !(new File(filename)).exists() ? 0L :Files.getLastModifiedTime(FileSystems.getDefault().getPath(filename)).toMillis();

               // Changement d'extension => ".prop"
               String propname = filename;
               int offset = propname.lastIndexOf('.');
               if( offset==-1 ) offset=propname.length();
               propname = propname.substring(0, offset)+".prop";
               File fprop = new File(propname);
               dateProp = !fprop.exists() ? 0L : fprop.lastModified();
               //            dateProp = !fprop.exists() ? 0L :Files.getLastModifiedTime(FileSystems.getDefault().getPath(propname)).toMillis();

               mocId = getMocId( f1.getName() );

               // Chargement du Moc et des Prop
               moc = loadMoc(filename);
               prop = loadProp(propname);
               if( prop==null && moc==null ) continue;
               if( prop==null && moc!=null ) prop = new MyProperties();
               mocId = adjustProp(prop,mocId,moc);
               if( moc!=null && !moc.getCoordSys().equals(COORDSYS) ) moc=null; // Incompatible MOC coordsys

               if( !exceptProp(prop,mocId) ) { prop=null; moc=null; }

               // Dans le cas où je ne mémorise pas le Moc
               // Mais j'ai été obligé de le lire pour mettre à jour les champs obs_initial_ra, etc.
               if( !flagWithMoc ) {  moc=null; dateMoc=0L; }

               // En fin compte, y a rien à voir !
               if( moc==null && prop==null ) continue;

               MocItem mi=null;

               // Comparaison et réutilisation avec celui déjà chargé s'il existe
               boolean flagCreation=true;
               
               if( oMM!=null && (mi=oMM.getItem(mocId))!=null ) {
                  
                  // Pas de changement ? => on le réutilise
                  if( dateMoc==mi.dateMoc && prop!=null && mi.prop!=null && prop.equals(mi.prop) ) {
                     add(mi);
                     flagCreation=false;
                     nbReused++;
//                     System.out.println("Réutilisation de "+mocId);
                  }
               }
               
               if( flagCreation ) {
                  add(mocId,moc,prop,dateMoc,dateProp);
//                  System.out.println("Ajout de "+mocId);
                  nbCreation++;
               }
               
               
//               if( oMM!=null && (mi=oMM.getItem(mocId))!=null 
//                     && dateMoc==mi.dateMoc && dateProp==mi.dateProp ) {
//
//                  // On travaille sur une copie, sinon on risque de modifier le MultiMoc courant
//                  mi = mi.copy();
//
////                  // On teste tout de même le contenu (cas d'un lien symbolique)
////                  if( prop==null && mi.prop!=null 
////                        || prop!=null && !prop.equals(mi.prop) ) { mi.prop=prop; mi.dateProp=dateProp; flagChange=true; }
////
////                  // le Moc a changé ?
////                  if( moc==null && mi.moc!=null 
////                        || moc!=null && !moc.equals(mi.moc) ) { mi.moc=moc; mi.dateMoc=dateMoc; flagChange=true;}
//                  
//                  // SANS LIENS SYMBOLIQUES, INUTILE DE REGARDER LE CONTENU, LA DATE SUFFIT
//                  flagChange=true;
//
//                  add(mi);
//
//                  // sinon création
//               } else {
//                  add(mocId,moc,prop,dateMoc,dateProp);
//                  flagChange=true;
//               }

               i++;
               if( i%1000==0 && out!=null ) {
                  long t2 = System.currentTimeMillis();
                  s=i+"... in "+Unite.getTemps(t2-t)+" => "+(int)(1000./((t2-t1)/1000.))+" per sec\n";
                  print(out,s);
                  System.out.print(s);
                  t1=t2;
               }

            } catch( Exception e ) {
               e.printStackTrace();
               print(out,mocId+" ["+e.getMessage()+"] => ignored\n");
            }
         }
      }
      
      if( out!=null && i%1000!=0 ) {
         double nb = i%1000;
         long t2 = System.currentTimeMillis();
         s=i+"... in "+Unite.getTemps(t2-t)+" => "+(int)(nb/((t2-t1)/1000.))+" per sec\n";
         print(out,s);
         System.out.print(s);
      }

      
      example=null;
      return nbCreation>0 || nbReused!=originalSize ;
   }
   
   // Just to print with flushing   
   private void print(PrintWriter out,String s) {
      if( out==null ) return;
      out.print(s);
      out.flush();
   }
   
   // Apply Exception rules and prop, return false if the record must be deleted (creator_did absent => removed)
   public boolean exceptProp(MyProperties prop,String id) {
      if( except==null || prop==null ) return true;
      for( MyProperties p : except ) prop.exceptions(p,id);
      return prop.get(KEY_ID)==null
            || prop.get("MOCSERVER_REMOVE")==null;   // A virer plus tard
   }
   
   // Loading Exceptions as Array of property records
   //   #
   //   # Post traitement des proprietes du MocServer
   //   #
   //   # S'applique apres le demarrage du Multimoc (creation ou relance)
   //   #
   //   # Syntaxe: regles de reecriture constituees par des paragraphes, dont :
   //   #
   //   #    - les champs classiques (qui ne commencent pas par >)
   //   #    permettent de selectionner les enregistrements MocServer concernes
   //   #    (les jokers * et ? sont possibles ainsi que les symboles !, < et >
   //   #    en 1er caractere pour indiquer une negation ou un test date|numerique)
   //   #    Rq: Toutes les regles doivent etre validees.
   //   #
   //   #    - les champs speciaux (qui commencent par >) indiquent les regles de
   //   #    reecriture:
   //   #       >toto = xxx  : remplacement de la valeur de la propriete "toto"
   //   #       >toto = +xxx : ajout de la valeur "xxx" a la propriete "toto"
   //   #                      (creation de la propriete si necessaire)
   //   #       >toto = -xxx : suppression de la valeur "xxx" a la propriete "toto"
   //   #                      (suppression de la propriete si vide)
   //   #       >toto =      : suppression de la propriete "toto"
   //   #
   //   #       >publisher_did = : entraine la suppression de l'enregistrement
   //   #
   //   # Rq: Tous les paragraphes sont appliques dans l'ordre sequentiel
   //   #     a tous les enregistrements du MocServer
   //   #     
   private void loadException(String file) {
      if( file==null ) return;
      ArrayList<MyProperties> p = new ArrayList<MyProperties>();
      BufferedReader br = null;
      try {
         br = new BufferedReader(new FileReader( file ));
         String s;
         MyProperties prop = null;
         while( (s = br.readLine()) != null ) {
            if( s.startsWith("#") ) continue;
            if( s.trim().length() == 0 ) {
               if( prop!=null ) {
                  p.add(prop);
                  prop=null;
               }
               continue;
            }
            if( prop==null ) prop=new MyProperties();
            prop.add(s);
         }
         if( prop!=null ) p.add(prop);
      } catch( Exception e) {
         System.out.println("Exception file ["+file+"] error => "+e.getMessage());
      } finally {
         if( br!=null ) try { br.close(); } catch( Exception e ) {}
      }

      if( p.size()>0 ) except = p;
   }
   
   
  /** Retourne true si s est un nom de champ utilisé dans le MultiMoc */
   public boolean isFieldName(String s) {
      if( example==null ) example = getProperties();
      return example.get(s)!=null;
   }

   /** Tri des Mocs en ordre alphanum sur les ID */
   public void sort() { Collections.sort(tri); }
   
   /** Tri des Mocs en fonction de la valeur de champs particuliers
    * La clé de tri prend en compte une succession de champs */
   public void sort(final String [] keys) {
      Collections.sort(tri, new Comparator<String>() {

         @Override
         public int compare(String o1, String o2) {
            MocItem mi1 = o1==null ? null : map.get(o1);
            MocItem mi2 = o2==null ? null : map.get(o2);

            String k1,k2;
            for( String key : keys ) {
               k1 = mi1==null || mi1.prop==null ? null : mi1.prop.get(key);
               k2 = mi2==null || mi2.prop==null ? null : mi2.prop.get(key);
               if( k1==null ) k1="";
               if( k2==null ) k2="";
               int rep = k1.compareTo(k2);
               if( rep!=0 ) return rep;
            }
            return 0;
         }
      });
   }
   
   // Extraction of MOC id from MOC file name
   private String getMocId(String filename) {
      String id = filename.replace("_","/");
      int offset = id.lastIndexOf('.');
      if( offset==-1 ) return id;
      return id.substring(0,offset);
   }
   
   /** Changement de référentiel d'un MOC */
   static final public HealpixMoc convertToICRS(HealpixMoc moc) throws Exception {
      char a = moc.getCoordSys().charAt(0);
      
      // Déjà en ICRS
      if( a!='G' && a!='E' ) return moc;   // déjà en ICRS
      
      // Ca va prendre trop de temps si on garde la résolution max
      if( moc.getMaxOrder()>10 && moc.getCoverage()>0.99 && !moc.isAllSky() ) {
         moc.setMocOrder(10);
         
      // Pour convertir, il faut avoir un cran de marge
//      } else {
//         if( moc.getMaxOrder()==HealpixMoc.MAXORDER ) {
//            System.out.println("Changement de order de 29 à 28");
//            moc.setMocOrder(HealpixMoc.MAXORDER-1);
//            System.out.println("C'est fait");
//         }
      }
      
      // Ciel complet => cas trivial
      if( moc.isAllSky()) {
         moc.setCoordSys("C");
         return moc;
      }
      
      Astroframe frameSrc = a=='G' ? new Galactic() : a=='E' ? new Ecliptic() : new ICRS();
      Healpix hpx = new Healpix();
      int order = moc.getMaxOrder();
      HealpixMoc moc1 = new HealpixMoc(moc.getMinLimitOrder(),moc.getMocOrder());
      moc1.setCheckConsistencyFlag(false);
      long onpix1=-1;
      int n=0;
      Coo c =new Coo();
      Iterator<Long> it = moc.pixelIterator();
      while( it.hasNext() ) {
         long npix = it.next();
         for( int i=0; i<4; i++ ) {
            double [] coo = hpx.pix2ang(order+1, npix*4+i);
            c.set(coo[0],coo[1]);
            frameSrc.toICRS(c);
            long npix1 = hpx.ang2pix(order+1, c.getLon(), c.getLat());
            if( npix1==onpix1 ) continue;
            onpix1=npix1;
            
            moc1.add(order,npix1/4);
            if( n>100000 ) { moc1.checkAndFix(); n=0; }
            n++;
         }

      }
      moc1.setCheckConsistencyFlag(true);
      return moc1;
   }


   
   // Moc Loading
   private HealpixMoc loadMoc(String filename) {
      HealpixMoc moc=null;
      try {
         moc = new HealpixMoc();
         moc.read(filename);
         if( moc.getSize()==0 ) moc=null;
         
         // Pas dans le bon système de référence
         String sys=moc.getCoordSys();
         if( !sys.equals(COORDSYS) ) {
            long t = System.currentTimeMillis();
            try {
               moc = convertToICRS(moc);
               System.out.println(filename+" MOC convert from "+sys+" to "+COORDSYS+" in "+ (System.currentTimeMillis()-t)+"ms");
            } catch( Exception e ) {
               System.out.println(filename+" MOC conversion error => ignored");
//               e.printStackTrace();
               throw e;
            }
         }
 
      } catch( Exception e ) { moc=null; }
      return moc;
   }
   
   // Properties loading 
   private MyProperties loadProp(String propname) {
      
      // Recherche du fichier de properties
      File f = new File(propname);
      if( !f.exists() ) return null;
      
      // Chargement des propriétés
      InputStreamReader in=null;
      MyProperties prop = null;
      try {
         prop = new MyProperties();
         in = new InputStreamReader( new BufferedInputStream( new FileInputStream(f) ));
         prop.load( in );
      } 
      catch( Exception e) {}
      finally {
         if( in!=null ) try {  in.close(); } catch( Exception e ) {}         
      }
      return prop;
   }

   /** Iterator on MocItem */
   public Iterator<MocItem> iterator() { return new ItemIterator(); }
   
   private class ItemIterator implements Iterator<MocItem> {
      int i=0;
      public boolean hasNext() { return i<tri.size(); }
      public MocItem next() { return map.get(  tri.get(i++) ); }
      public void remove() { }
   }
   
   /**
    * Search by HEALPix cell.
    * @param order order of the HEALPix
    * @param npix pixel number of the HEALPix (nested, same coordsys)
    * @return list of MOC identifiers
    */
   public ArrayList<String> scan(int order,long npix) { return scan(order,npix, (String)null); }
   
   /**
    * Search by HEALPix cell.
    * @param order order of the HEALPix
    * @param npix pixel number of the HEALPix (nested, same coordsys)
    * @param mask wildcard mask for pre-filtering on MOC identifiers (wildcard syntax: *,?,! (not as first char))
    * @return list of MOC identifiers
    */
   public ArrayList<String> scan(int order,long npix,String mask ) {
      boolean match=false;
      if( mask.charAt(0)=='!' ) { match=true; mask=mask.substring(1); }
      ArrayList<String> res = new ArrayList<String>();
      for( MocItem mi : this ) {
         if( mask!=null && MyProperties.matchMask(mask, mi.mocId )==match ) continue;
         if( mi.moc!=null && mi.moc.isIntersecting(order, npix) ) res.add(mi.mocId);
      }
      return res;
   }
   
   /**
    * Search by MOC.
    * @param moc MOC describing input sky region
    * @return list of MOC identifiers
    */
   public ArrayList<String> scan(HealpixMoc moc) { return scan(moc, (HashMap<String,String[]>)null, true, -1, false); }
   
   
   // Détermination de l'ID, soit par le creator_did, ou le publisher_did sinon creator_id?obs_id ou publisher_id?obs_id
   // et encore sinon, avec le filename passé en paramètre, enfin null si rien à faire
   // sans le préfixe ivo://
   static public String getID(MyProperties prop) { return getID(prop,null); }
   
   static public String getID(MyProperties prop,String filename) {   
      
      // L'ID est donné explicitement (ex: CDS/P/DSS2/color)
      String id = prop.getProperty("ID");
      if( id!=null ) return id;
      
      // l'ID doit être construit à partir des différents champs possibles
      id = prop.get("creator_did");
      if( id==null ) id = prop.get("publisher_did");
      if( id==null ) {
         String o =  prop.get("obs_id");
         if( o==null && filename!=null ) id = "ivo://"+filename.replace("_","/");
         else {
            if( o==null ) o="id"+(System.currentTimeMillis()/1000);
            String p = prop.get("creator_id");
            if( p==null ) p = prop.get("publisher_id");
            if( p==null ) {
               return null;
//               p = "ivo://UNK.AUT";
            }
            id = p+"/"+o;
         }
      }
      // On enlève le préfixe ivo://
      if( id.startsWith("ivo://") ) id = id.substring(6);
      
      // On remplace les éventuels ? par des / (merci Markus !)
      id = id.replace('?', '/');
      return id;
   }
   
   private String toDebug(HashMap<String,String[]> mapFilter, String listKey) {
      StringBuilder r=null;
      for( String s : mapFilter.get(listKey) ) {
         if( r==null ) r = new StringBuilder(listKey+"="+s);
         else r.append(","+s);
      }
      return r.toString();
   }
   
   static private final boolean DEBUGMATCH = false;
   
   // Retourne true si les Propriétés du MocItem mi correspondent aux contraintes passées dans le mapFilter
   private boolean match(MocItem mi, HashMap<String,String[]> mapFilter, boolean casesens, boolean andLogic) {
      if( mapFilter==null ) return true;
      if( mi.prop==null ) return false;
      
      boolean rep=andLogic;

      for( String listKey : mapFilter.keySet()) {
         
         boolean rep1=false;
         
         
         // Détermination de la logique si le premier caractère est '!' => AndLogic sinon OrLogic
         String [] masks = mapFilter.get( listKey );
         boolean internalAndLogic = masks!=null && masks.length>0 && masks[0].startsWith("!");
         
         if( DEBUGMATCH) System.out.println("Expr: "+toDebug(mapFilter,listKey)+" => andLogic="+internalAndLogic);
         
         // Plusieurs keywords (genre ID,CDS=...);
         if( listKey.indexOf(',')>0 ) {
            if( DEBUGMATCH) System.out.println("=> plusieurs keys...");
            Tok tok = new Tok(listKey,",");
            while( tok.hasMoreTokens() ) {
               String key = tok.nextToken();
               rep1 |= matchKey(mi,mapFilter,listKey,key,casesens,internalAndLogic);
               if( !andLogic && !internalAndLogic && rep1 ) {
                  if( DEBUGMATCH) System.out.println("   => matchKey("+key+",...)==true (orLogic) => return true)");
                  return true;
               }
            }
            
         // Un seul keyword
         } else {
            if( DEBUGMATCH) System.out.println("=> une seule key...");
            rep1 =  matchKey(mi,mapFilter,listKey,listKey,casesens,internalAndLogic);
         }
         
         // Si que des OU et que c'est ok, on peut conclure que c'est bon
         if( !andLogic && !internalAndLogic && rep1 ) return true;
         
         
         if( andLogic ) {
            rep &= rep1;
            if( !rep ) return false;
         }
         
      }

      return rep;
   }
   
   private boolean matchKey(MocItem mi, HashMap<String,String[]> mapFilter, 
         String listKey, String key, boolean casesens, boolean andLogic) {
      
      // Jokers sur le nom du champ ?
      if( key.indexOf('?')>=0 || key.indexOf('*')>=0 ) {
         if( DEBUGMATCH) System.out.println("==> jokers ? * présents dans ["+key+"]...");

         boolean rep=false;
         for( String mask : mapFilter.get(listKey) ) {
            rep=false;
            
            if( DEBUGMATCH) System.out.println("   Test sur "+mask+"...");
            
            Iterator<String> it = mi.prop.getKeys().iterator();
            while( it.hasNext() && !rep ) {
               String k1 = it.next();
               if( !MyProperties.matchMask(key, k1) ) continue;  // forçage pour le cas de l'identificateur
               boolean cs = k1.equals(KEY_ID) ? true : casesens;
               rep |= matchList(mask,mi.prop.get(k1),cs, andLogic);
            }
            
            if( DEBUGMATCH) System.out.println("   => matchList = "+rep+(!rep?" (return dans matchKey)":""));
            if( !rep ) return false;
         }
         if( !rep ) return false;           // <= Ne faudrait-il pas ausii ici une logique OR ?
         
      // Nom de champ explicite ?
      } else {
         if( DEBUGMATCH) System.out.println("==> Champ explicite ["+key+"]...");
         if( key.equals(KEY_ID) ) casesens=true;  // forçage pour le cas de l'identificateur
         boolean rep = matchProp(mapFilter.get(listKey),mi.prop.get(key),casesens, andLogic);
         if( DEBUGMATCH) System.out.println("   matchProp = "+rep+(!rep?" (return dans matchKey)":""));
         if( !rep) return false;
      }
      
      if( DEBUGMATCH) System.out.println("MatchList return true");
      return true;
   }
   
   private boolean matchProp(String [] listMask, String vProp, boolean casesens, boolean andLogic) {
      
      // Le champ n'existe pas ? la réponse est true
      // sauf si tous les masques sont "ne contient pas..."
      if( vProp==null ) {
         if( DEBUGMATCH) System.out.println("      vProp==null...");
         return andLogic;
      }

      // Le champ existe => 
      // Logic ET : toutes les propositions et tous les masques sont vérifiés par au-moins une proposition
      if( andLogic ) {
         
         for( String mask : listMask ) {
            if( !matchAndList(mask,vProp, casesens) ) {
               if( DEBUGMATCH) System.out.println("      matchAndProp("+mask+","+vProp+")==false, andLogic => return false");
               return false;
            } else {
               if( DEBUGMATCH) System.out.println("      matchAndProp("+mask+","+vProp+")==true andLogic => continue");
            }
         }
         if( DEBUGMATCH) System.out.println("      matchProp(allmask,"+vProp+")==true, andLogic => return true");
         return true;
      }

      // logic OU : toutes les propositions et tous les masques (ex: Novae, Binaries*) aient au moins
      //            un champ qui le vérifie (ex: champ xxx \t yyy \t ...)
      for( String mask : listMask ) {
         if( matchOrList(mask,vProp, casesens) ) {
            if( DEBUGMATCH) System.out.println("      matchOrList("+mask+","+vProp+")==true, ordLogic => return true");
            return true;
         } else {
            if( DEBUGMATCH) System.out.println("      matchOrList("+mask+","+vProp+")==false orLogic => continue");
         }
      }
      if( DEBUGMATCH) System.out.println("      matchProp(allmask,"+vProp+")==false, orLogic => return false");
      return false;
   }
   
   private boolean matchList(String mask,String vProp, boolean casesens, boolean andLogic ) {
      if( andLogic ) return matchAndList( mask, vProp, casesens);
      return matchOrList( mask, vProp, casesens);
   }

   private boolean matchOrList(String mask,String vProp, boolean casesens ) {
      if( vProp==null ) return false;
      Tok st1 = new Tok(mask,",");
      while( st1.hasMoreTokens() ) {
         String mask1 = st1.nextToken();
         Tok st = new Tok(vProp,"\t");
         while( st.hasMoreTokens() ) {
            String v = st.nextToken();
            if( match(mask1,v,casesens)) {
               if( DEBUGMATCH) System.out.println("      matchOrList("+mask1+","+v+")==true => return true");
               return true;
            }
         }
      }
      if( DEBUGMATCH) System.out.println("      matchOrList(allMask,"+vProp+")==false => return false");
      return false;
   }
   
   private boolean matchAndList(String mask,String vProp, boolean casesens ) {
      if( vProp==null ) return false;
      Tok st1 = new Tok(mask,",");
      while( st1.hasMoreTokens() ) {
         String mask1 = st1.nextToken();
         Tok st = new Tok(vProp,"\t");
         
         boolean rep=false;
         while( st.hasMoreTokens() ) {
            if( match(mask1,st.nextToken(),casesens)) { rep=true; break; }
         }
         if( DEBUGMATCH) if( !rep ) System.out.println("      matchAndList(oneMask,"+vProp+")==false => return false");
         if( !rep ) return false;
      }
      if( DEBUGMATCH) System.out.println("      matchAndList(allMask,"+vProp+")==true => return true");
      return true;
   }
   
   private boolean match(String mask,String value, boolean casesens) {
      boolean match=true;
      if( mask.length()==0 ) {
         if( MyProperties.matchMask(mask,value) ) return true;
         return false;
      }
      char c = mask.charAt(0);
      
      // Inégalité ?
      if( c=='>' || c=='<' ) {
         mask = mask.substring(1);
         boolean strict=true;
         if( mask.startsWith("=") ) { strict=false;  mask = mask.substring(1); }
         return MyProperties.testInequality(c,strict,mask,value);
      }
      
      // Une différence plutôt qu'une égalité ?
      if( mask.charAt(0)=='!' ) { match=false; mask=mask.substring(1); }
      
      // Intervalle ?
      int i = mask.indexOf("..");
      if( i>0 ) {
         try {
            Double min = Double.parseDouble( mask.substring(0,i).trim() );
            Double max = Double.parseDouble( mask.substring(i+2).trim() );
            Double val = Double.parseDouble( value.trim() );
            return (min<=val && val<=max) == match;
            
         } catch( Exception e ) { }
      }
      
      // Prise en compte de la case ?
      if( !casesens ) { mask=mask.toUpperCase(); value=value.toUpperCase(); }
      
      return MyProperties.matchMask(mask,value) == match;
   }
   
   /**
    * Vérifie que les properties associées à l'id passé en paramètre
    * est à jour. 
    * @param id Identification des properties (ex: CDS/P/DSS2/color)
    * @param timestamp estampillage de référence (date Unix de la dernière maj)
    * @param flagDiff true si on fait un simple test de différence, sinon inf.strict
    * @return -1 : l'enregistrement n'existe pas/plus (ou a été renommé)
    *          0 : l'enregistrement n'est plus à jour (timestamp < ou !=  de la nouvelle date)
    *          1 : l'enregistrement est à jour
    */
   public int isUpToDate(String id, long timestamp,boolean flagDiff) {
      MocItem mi = getItem(id);
      if( mi==null ) return -1;   // n'existe pas/plus
      if( flagDiff  && timestamp!=mi.getPropTimeStamp() ) return 0;   // doit être maj
      if( !flagDiff && timestamp<mi.getPropTimeStamp() )  return 0;   // doit être maj
      return 1; // est à jour
   }
   
   /**
    * Search by MOC.
    * @param moc MOC describing input sky region
    * @param mapFilter propKey="wildcard mask" list for pre-filtering on MOC identifiers (wildcard syntax: *,?,! (not as first char))
    * @param casesensitive case sensitive (true by default) - never applied for ID field
    * @return list of MOC identifiers
    */
   public ArrayList<String> scan(HealpixMoc moc,HashMap<String, String[]> mapFilter, boolean casesens, int top, boolean inclusive ) {
      ArrayList<String> res = new ArrayList<String>();

      int n=0;
      for( MocItem mi : this ) {
         if( mapFilter!=null && !match(mi,mapFilter,casesens,true)) continue;
         if( moc!=null ) {
            if( mi.moc==null ) continue;
            if( !inclusive ) {
               if( !mi.moc.isIntersecting(moc) ) continue;
            } else {
               if( !isIncluded(moc,mi.moc) ) continue;
            }
         }
         res.add(mi.mocId);
         if( top!=-1 && (++n)>=top ) return res;
      }

      return res;
   }
   
   /**
    * Search by wildcard mask.
    * @param mask mask wildcard mask for selecting MOC identifiers (wildcard syntax: *,?,! => not if in first pos))
    * @return
    */
   public ArrayList<String> scan(HashMap<String,String[]> mapFilter) {
      return scan( (HealpixMoc)null, mapFilter, true, -1, false);
   }
   
   /**
    * Scanning by MOC and logical expression based on set algebra
    * @param moc MOC describing input sky region
    * @param expr expression based on set algebra
    * @param casesensitive case sensitive (true by default) - never applied for ID field
    * @param inclusive true implies that all sky region must be inside
    * @return list of IDs (keep the original MultiMoc order)
    */
   public ArrayList<String> scan( HealpixMoc moc, String expr, boolean casesens, int top, boolean inclusive ) throws Exception {
      ArrayList<String> res = new ArrayList<String>();
      
      // Détermination des IDs candidats
      HashSet<String> candidateIds = scanExpr(expr,casesens);
      if( candidateIds.size()==0 ) return res;

      int n=0;
      for( MocItem mi : this ) {
         if( !candidateIds.contains(mi.mocId) ) continue;
         if( moc!=null ) {
            if( mi.moc==null ) continue;
            if( !inclusive ) {
               if( !mi.moc.isIntersecting(moc) ) continue;
            } else {
               if( !isIncluded(moc,mi.moc) ) continue;
            }
         }
         res.add(mi.mocId);
         if( top!=-1 && (++n)>=top ) return res;
      }

      return res;
   }
   
   private boolean isIncluded(HealpixMoc region, HealpixMoc moc) {
      if( moc.isAllSky() ) return true;
      Iterator<MocCell> it = region.iterator();
      while( it.hasNext() ) {
         MocCell c = it.next();
         if( !(moc.isIn(c.order,c.npix) || moc.isDescendant(c.order,c.npix)) ) return false;
      }
      return true;
   }

   /**
    * Full scanning.
    * @return list of IDs (keep the original MultiMoc order)
    */
   public ArrayList<String> scan() { 
      ArrayList<String> res = new ArrayList<String> (this.size() );
      for( MocItem mi : this ) res.add(mi.mocId);
      return res;
//      IDENTIQUE A L'EXPRESSION SUIVANTE QUI EST PLUS LENTE
//      try { return scan("*"); }
//      catch( Exception e) { return new ArrayList<String>(); }
   }
   
   /**
    * Scanning by wildcard mask on IDs
    * @param mask mask wildcard mask for selecting MOC identifiers (wildcard syntax: *,?,! (not as first char))
    * @param casesensitive case sensitive (true by default) - never applied for ID field
    * @return list of IDs (keep the original MultiMoc order)
    */
   public ArrayList<String> scan(String mask) throws Exception { return scan( mask, true); }
   public ArrayList<String> scan(String mask, boolean casesensitive) throws Exception {
      return scan( (HealpixMoc)null, mask, casesensitive, -1, false);
   }
   

   
   // Opérande dans une expression ensembliste
   // ex:  ID=CDS* || obs_title=*CDS* && hips_*_url=* &! datatype_subtype=catalog
   // op1 => ID=CDS* ||
   // 
   private class Op {
      String expr;           // Expression de sélection
      HashSet<String> res;   // Ensemble des IDs correspondants à l'expression
      int logic;             // opérateur à appliquer: 0-Union=||, 1-Intersection=&&,  2-Soustraction=&!,    
      boolean terminal=false;// true si l'expression de sélection est terminal dans l'arbre des expressions 
                             // (pas d'opérateur ni de parenthèse interne)
      // Juste pour débogage
      public String toString() { return expr+(logic==2?"&!": logic==0?"||": logic==1?"&&": ""); }
   }
   
   /**
    * Retourne l'expression nettoyée de ces espaces en bout, et de 1 niveau de parenthèse
    * éventuellement présent
    * @param a   la chaine à traiter
    * @param deb l'index du début de la chaine à traiter
    * @param fin l'index suivant la fin de la chaine à traiter
    * @return
    */
   private String getExpr(char a[], int deb, int fin) {
      
      // On enlève les blancs en bout
      while( deb<a.length && a[deb]==' ' ) deb++;
      while( fin>0 && a[fin-1]==' ' ) fin--;
      
      // On enlève 1 niveau de parenthèse éventuel
      if( deb<a.length && a[deb]=='(' && fin>1 && a[fin-1]==')' ) { deb++; fin--; }
      
      // On enlève les blancs en bout
      while( deb<a.length && a[deb]==' ' ) deb++;
      while( fin>0 && a[fin-1]==' ' ) fin--;
      
      return new String(a,deb,fin-deb);
   }
   
   /**
    * Retourne l'expression nettoyée de ces espaces en bout, et de 1 niveau de parenthèse
    * éventuellement présent
    * @param a   la chaine à traiter
    * @param deb l'index du début de la chaine à traiter (jusqu'au bout de la chaine)
    * @return
    */
   private String getExpr(char a[], int deb) { return getExpr(a,deb,a.length); }
   
   
   static private enum MgetOp { DEBUT, AVANT, DEDANS_PREF, DEDANS_QUOTE, SLASH, DEDANS, FIN }
   
   /**
    * Extrait la prochaine opérande dans une expression ensembliste logique
    * @param op  paramètre de retour: contient l'opérande et l'opérateur qui suit
    * @param a   la chaine à traiter
    * @param pos la position de début de chaine à traiter
    * @return    la prochaine position à traiter, -1 si fin de chaine atteind
    */
   private int getOp(Op op, char [] a, int pos) {
      int i;
      MgetOp mode = MgetOp.DEBUT;
      char quote=' ';
      int par=0;
      op.terminal=true;
      for( i=pos; i<a.length && mode!=MgetOp.FIN; i++ ) {
         char c = a[i];
         
         switch(mode) {
            case DEBUT:
               if( !Character.isWhitespace(c) ) {
                  if( c=='(' ) { par++; op.terminal=false; }
                  else mode=MgetOp.AVANT; 
               }
               break;
            case DEDANS_PREF:
               if( !Character.isWhitespace(c) ) {
                  if( c=='"' || c=='\'' ) { quote=c; mode= MgetOp.DEDANS_QUOTE; }
                  else mode = MgetOp.DEDANS;
               }
               break;
            case DEDANS_QUOTE:
               if( c=='\\' ) mode = MgetOp.SLASH;
               else if( c==quote ) mode = MgetOp.DEDANS;
               break;
            case SLASH:
               mode =  MgetOp.DEDANS_QUOTE;
               break;
            case AVANT:
               if( c=='=' ) { mode = MgetOp.DEDANS_PREF; break; }
            case DEDANS:
               if( c==')' ) par--;
               if( i>0 && (a[i-1]=='|' && c=='|' 
                      || a[i-1]=='&' && c=='&' 
                      || a[i-1]=='&' && c=='!' ) ) {
                  if( par==0 ) { mode = MgetOp.FIN; i--; }
                  else mode = MgetOp.AVANT;
               }
               break;
         }
      }
      
      // Fin de la chaine ?
      if( i>=a.length ) {
         op.expr = getExpr(a,pos);
         op.logic=-1;   // => il n'y a pas d'opérateur qui suit
         return -1;
      } 
      
      // Extraction de l'expression (suppression des blancs et d'un niveau de parenthèse éventuel
      op.expr = getExpr(a,pos,i-1);
      op.logic =  a[i]=='!' ? 2 : a[i]=='|' ? 0 : 1;
      
      return i+1;
   }

// METHODE BASIQUE - SANS POSSIBILITE D'AVOIR DES PARENTHESES OU DES &&, ||, &! DANS LE TEXTE DES CONTRAINTES
//   /**
//    * Extrait la prochaine opérande dans une expression ensembliste logique
//    * @param op  paramètre de retour: contient l'opérande et l'opérateur qui suit
//    * @param a   la chaine à traiter
//    * @param pos la position de début de chaine à traiter
//    * @return    la prochaine position à traiter, -1 si fin de chaine atteind
//    */
//   private int getOp(Op op, char [] a, int pos) {
//      int i;
//      int par=0;
//      op.terminal=true;
//      for( i=pos; i<a.length; i++ ) {
//         char c = a[i];
//         if( c=='(' ) par++;
//         else if( c==')' ) par--;
//         if( par>0 ) op.terminal=false;
//         if( par==0 && i>0 
//               && (a[i-1]=='|' && c=='|' 
//               || a[i-1]=='&' && c=='&' 
//               || a[i-1]=='&' && c=='!' ) ) break;
//      }
//
//      // Fin de la chaine ?
//      if( i>=a.length ) {
//         op.expr = getExpr(a,pos);
//         op.logic=-1;   // => il n'y a pas d'opérateur qui suit
//         return -1;
//      } 
//
//      // Extraction de l'expression (suppression des blancs et d'un niveau de parenthèse éventuel
//      op.expr = getExpr(a,pos,i-1);
//      op.logic =  a[i]=='!' ? 2 : a[i]=='|' ? 0 : 1;
//
//      return i+1;
//   }
   
   // Pour débogage (calcul d'un préfixe d'indentation)
   private String indent(int niv) { 
      StringBuilder s = new StringBuilder();
      for( int j=0; j<niv*3; j++ ) s.append(' ');
      return s.toString();
   }
   
   /**
    * Ajustement de syntaxe par substitution
    * 1) key!=val1,val2  =>  key=!val1,!val2
    * 2) key>val         =>  key=>val
    * 3) key<val         =>  key=<val
    * @param s
    * @return
    */
   private String adjustExpr(String s) {
      if( s==null || s.length()==0 ) return s;
      
      //Unquote éventuelle de la valeur
      s = unQuote(s);
      
      // Traitement de key!=val1,val2  =>  key=!val1,!val2
      int pos = s.indexOf('!');
      if( pos>0 && pos<s.length()-1 && s.charAt(pos+1)=='=' ) {
         StringBuilder res = null;
         Tok tok = new Tok( s.substring(pos+2), ",");
         while( tok.hasMoreTokens() ) {
            String val = tok.nextToken();
            if( res==null ) res=new StringBuilder( s.substring(0,pos)+"=!"+val);
            else res.append(",!"+val);
         }
         return res.toString();
      }
      
      // Traitement de key>val =>  key=>val
      int pos1 = s.indexOf('=');
      pos = s.indexOf('>');
      if( pos>0 && (pos1==-1 || pos1>pos) ) return s.substring(0,pos)+"=>"+s.substring(pos+1);
      
      // Traitement de key<val =>  key=<val
      pos = s.indexOf('<');
      if( pos>0 && (pos1==-1 || pos1>pos) ) return s.substring(0,pos)+"=<"+s.substring(pos+1);
      
      return s;
   }
   
   /**
    * suppression des cotes éventuelles sur la valeur
    * cle="xxx" => cle=xxx
    * cle!="xxx" => cle!=xxx
    */
   private String unQuote(String s) {
      int i = s.indexOf('=');
      if( i<0 ) return s;
      if( s.indexOf('"',i)<0 && s.indexOf('\'')<0 ) return s;
      return s.substring(0,i+1) + Tok.unQuote( s.substring(i+1) );
   }

   /**
    * Calcul récursif d'une expression ensembliste par algorithme à pile.
    * *
    * L'algorithme est le suivant:
    * 0) Je mémorise le niveau de la pile
    * 1) Je lis la première opérande + opérateur éventuel
    *    => si fin de chaine 
    *          si terminal j'initialise la valeur associé à l'élément
    *          sinon je retourne le résultat de l'appel récursif pour cette opérande
    *    => sinon j'empile (opérande+opérateur)
    * 2) Je lis l'opérande suivante + opérateur éventuel
    *    => je la traite récursivement (=> retourne son résultat)
    *    => je garde son résultat
    * 3) Je traite la pile
    *   Tant que la pile n'est pas revenu à son niveau initial
    *      Tant que l'opérateur du haut de la pile est moins prioritaire
    *      que le précédent
    *        j'effectue le calcul avec le haut de la pile + précédent opérateur
    *        et remplace le résultat par sur l'opérande courante (sans changer
    *        l'opérateur associé)
    * 4) Si l'expression n'est pas terminée
    *   => j'empile le résultat + opérateur courant
    *      et je reprends en 2)
    * 5) Si la pile n'est pas revenue à son niveau initial => erreur
    * 6) Je retourne le dernier opérateur
    *
    * @param niv  Niveau de la récursivité (ne sert que pour le débogage)
    * @param stack  La pile des résultats intermédiaires
    * @param s      La chaine de l'expression ensembliste 
    *               ex: (ID=CDS* || obs_title=*CDS*) && hips_*_url=* &! datatype_subtype=catalog
    *                => [ [ID=CDS* || obs_title=*CDS*] && [hips_*_url=* &! datatype_subtype=catalog] ]
    * @param casesens case sensitive or not (default is case sensitive)  - never applied for ID field
    * @return L'opérande de fin de calcul (contient les élements matchant l'expression
    * @throws Exception en cas d'erreur de syntaxe et autres
    */
   private Op calculExpr( int niv, Stack<Op>stack, String s, boolean casesens) throws Exception {
      
      if( niv>20 ) throw new Exception("Expression syntax error");
      
      char [] a = s.toCharArray();
      int stLimit = stack.size();  // taille actuelle de la pile
      
      // Première opérande
      Op op = new Op();
      int pos=getOp(op,a,0);
      
      if( pos==-1 ) {
         if( !op.terminal ) op = calculExpr(niv+1, stack, op.expr, casesens);
         else {
//            System.out.println(indent(niv)+"Init "+op.expr);
            initScanItem(op,casesens);
         }
         return op;
      }
      
      int logic = op.logic;
      op = calculExpr(niv+1,stack,op.expr, casesens);
      op.logic=logic;
      
//      System.out.println(indent(niv)+"Je lis op1: "+op);
      
      // sinon on empile
      stack.push(op); 
//      System.out.println(indent(niv)+"J'empile "+op);
      
      // et on continue l'expression tant que possible
      while( pos!=-1 ) {
         op = new Op();
         pos = getOp(op,a,pos);
         
//         System.out.println(indent(niv)+"Je lis op2: "+op);

         // Recherche de l'opérande suivante
         logic = op.logic;
         op = calculExpr( niv+3, stack, op.expr, casesens );
         op.logic = logic;
         
         // Peut-on combiner le dernier résultat avec le haut de la pile ?
         while( stack.size()>stLimit ) {

            Op op0 = stack.peek();

            // L'opérateur qui va suivre est de plus faible priorité que celui associée
            // à la dernière opérande => on peut faire le calcul
            if( op.logic <= op0.logic ) {
               op.expr  = op0.expr+(op0.logic==2?" &! " : op0.logic==1?" && ":" || ")+op.expr;
//               System.out.println(indent(niv)+"Je calcule "+op.expr);
               op.res=combine(op0.res,op.res,op0.logic);
               op.expr = "["+op.expr+"]";
               op0 = stack.pop();
//               System.out.println(indent(niv)+"Je dépile "+op0);

            } else {
//               System.out.println(indent(niv)+"Fin traitement de pile");
               break;
            }
         }
         
         if( pos!=-1 ) {
            // J'empile le dernier résultat
            stack.push(op);
//            System.out.println(indent(niv)+"J'empile "+op);
         }
      }
      
      if( stack.size()!=stLimit ) {
         throw new Exception("Expression error");
      }
      return op;
   }
   
   /**
    * Initialisation des éléments correspondants à une expression terminale
    * (càd qui peut être traité par un scan(..)
    * @param op L'opérande à initiatiliser (contient l'expression et le résultat)
    * @param casesens case sensitive or not (default is case sensitive) - never applied for ID field
    */
   private void initScanItem(Op op, boolean casesens) throws Exception {
      
//      System.out.println("J'initialise "+op);
      
      // Peaufinage de syntaxe (!=, >, < )
      op.expr = adjustExpr(op.expr);
      
      // Découpage de l'expression terminal en paramètres utilisable par scan(...)
      int pos = op.expr.indexOf('=');
      String key;
      
      // Dans le cas où il n'y a pas de champ de filtrage spécifié, il s'agit de ID
      key= pos==-1 ? KEY_ID : op.expr.substring(0, pos).trim();
      String val = op.expr.substring(pos+1).trim();
      
      
      // Peut être s'agit-il d'un ID unique (ex: CDS/P/2MASS/J) ? on peut donc aller plus vite
      if( (pos==-1 || key.equals(KEY_ID))
            && val.indexOf('*')<0 && val.indexOf('?')<0 && val.indexOf(',')<0) {

         // Est-ce qu'il n'y a que la contrainte sur l'ID ? alors je peux y accéder directement
         op.res = new HashSet<String>( 10 );   // <= je crée une Hashset petite car ce ne sera probablement 
                                               //    jamais elle qui sera conservé lors des opérations ensemblistes 
         MocItem mi1 = getItem(val);
//         System.out.println("Accès direct pour "+val);
         if( mi1!=null ) op.res.add(mi1.mocId);
         return;
      }
      
     
      HashMap<String, String[]> mapFilters = new HashMap<String, String[]>();
      mapFilters.put( key, new String[] { val } );
      
      // Scanning du multimoc et mémorisation des éléments qui correspondent
      ArrayList<String> res = scan( (HealpixMoc)null, mapFilters, casesens, -1, false);
      op.res = new HashSet<String>( Math.max(2*res.size(), size()) );   // <= Je crée une HashSet assez grande, car elle est susceptible
                                                                        //    de contenir l'ensemble des résultats. On pourrait affiner en
      op.res.addAll( res );
   }
   
   /**
    * Traitement des ensembles.
    * @param a     L'ensemble A
    * @param b     L'ensemble B
    * @param logic 0-UNION, 1-INTERSECTION, 2-EXCEPT (ex: ABC &! A => BC)
    * @return      L'ensemble résultant (peut être vide, mais jamais null)
    * ATTENTION: l'ensemble retourné utilise l'un ou l'autre des ensembles initiaux
    */
   private HashSet<String> combine( HashSet<String> a, HashSet<String> b ,int logic) {
      
      int aSize = a.size();
      int bSize = b.size();
      
      // Logique || => UNION
      if( logic==0 ) {
         if( bSize>aSize ) { b.addAll(a); return b; }
         a.addAll(b);
         return a;
      }
      
      // Logique && => INTERSECTION
      if( logic==1 ) {
         
         if( bSize>aSize ) { b.retainAll(a); return b; }
         a.retainAll(b);
         return a;
      }
      
      // logique &! => EXCEPT
      if( bSize>aSize) { b.removeAll(a); return b; }
      a.removeAll(b);
      return a;
   }
   
   /**
    * Scanning du Multimoc par une expression ensembliste portant sur les propriétés uniquement
    * ex: (ID=CDS* || obs_title=*CDS*) && hips_*_url=* &! datatype_subtype=catalog
    * => retourne tous les ID commençant par CDS ou dont le obs_title contient le mot CDS
    *           et qui ont une URL HiPS, mais sans prendre en compte les catalogues
    * @param s  L'expression ensembliste (voir ci-dessus)
    * @param casesens case sensitive or not (default is case sensitive) - never applied for ID field
    * @return   L'ensemble des ID qui matchent l'expression
    * @throws Exception
    */
   private HashSet<String> scanExpr(String s, boolean casesens ) throws Exception {
      Op op = calculExpr( 0, new Stack<Op>(), s, casesens );
      return op.res;
   }
   
  
   /**
    * Get the coordinate system
    * @return Coordinate system (HEALPix convention: G-galactic, C-Equatorial, E-Ecliptic)
    */
   public String getCoordSys() { return COORDSYS; }
   
   /**
    * Best MOC order contains in the MultiMoc
    * @return best MOC order
    */
   public int getMocOrder() { return mocOrder; }

   /**
    * Number of MOCS.
    * @return The number of MOCs
    */
   public int size() { return map.size(); }
   
   /**
    * Size in memory.
    * @return size in memory in bytes
    */
   public long getMem() {
      long size=0;
      for( MocItem mi : this ) {
         if( mi.moc!=null ) size += mi.moc.getMem();
      }
      size+=getPropMem();
      return size;
   }
   
   public long getPropMem() {
      long size=0;
      for( MocItem mi : this ) {
         if( mi.prop!=null ) size += mi.prop.getMem();
      }
      return size;
   }
   
   /**
    * Debug string
    */
   public String toString() {
      return "MultiMoc: nbmoc="+size()+" mem="+getMem()/(1024L*1024L)+"MB";
   }   
   
   
   /** Pour tester */
   static public void main(String [] s) {
      try {
         MultiMoc m = new MultiMoc();
         
         long t0 = System.currentTimeMillis();
         String s3 = "/Users/Pierre/.aladin/Cache/Multiprop.bin";
         m = (new BinaryDump()).load(s3);
         System.out.println("Multiprop loaded ("+m.size()+" rec.) in "+(System.currentTimeMillis()-t0)+"ms...");

         
//         moc_order=8  hips_order=11
//               9 .. 12


         
//         String s2 = "client_application=* &! data*type=catalog";
//         String s2 = "obs_title,ID=!CDS/B/denis/denis,!CDS/C/CALIFA/V500/DR2";
//         String s2 = "CDS/P/DSS2/Color || CDS/B/denis/Denis";
//         String s2 = "obs_title!=\"*(*\" && ID=ESA*";
         String s2 = "(ID=ESA* || ID=*arche* || ID=\"toto(titi*\") && (client_application=* || ID=*25*)";
        
         System.out.println("calculer: "+s2);
         
         t0 = System.currentTimeMillis();
         ArrayList<String> res = m.scan(s2,false);
         System.out.println("Résultat ("+res.size()+" rec.) in "+(System.currentTimeMillis()-t0)+"ms...");
         
         int i=0;
         for( String s1 : res ) {
            i++;
            System.out.println("   "+s1);
            if( i>20 ) { System.out.println("   ..."); break; }
         }
         
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }


   
}
