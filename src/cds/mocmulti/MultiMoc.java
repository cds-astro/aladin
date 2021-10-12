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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;

import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.astro.Astroframe;
import cds.astro.Coo;
import cds.moc.Healpix;
import cds.moc.Moc;
import cds.moc.SMoc;
import cds.moc.STMoc;
import cds.moc.TMoc;


/**
 * Array of MOCs manager
 * 
 * @author Pierre Fernique [CDS]
 * @version 0.9 march 2015 prototype creation
 * @version 1.0 feb 2015 ready for production
 * @version 1.1 Apr 2015 Lot of improvements...
 * @version 1.2 July 2017 Compatible with Aladin internal usage
 * @version 2.0 July 2021 MOC 2.0 compliante (SMOC + TMOC + STMOC support)
 */
public class MultiMoc implements Iterable<MocItem> {
   
   static final boolean DEBUG = true;
   
   // Clés spécials
   static public String KEY_ID        = "ID";                // identificateur de l'enregistrement (ex: CDS/P/DSS2/color)
   static public String KEY_TIMESTAMP = "TIMESTAMP";         // Pour l'estampillage des propriétés
   static public String KEY_REMOVE    = "MOCSERVER_REMOVE";  // Pour indiquer un enregistrement supprimé
   
   // Mode de recouvrement pour un scan par région
   static public final int OVERLAPS = 0;
   static public final int ENCLOSED = 1;
   static public final int COVERS   = 2;
   
   static public final String [] INTERSECT = { "overlaps","enclosed","covers" };
   
   final private String COORDSYS ="C";   // Coordinate system (HEALPix convention => G=galactic, C=Equatorial, E=Ecliptic)
   
   protected HashMap<String, MocItem> map; // Liste des MocItem repéré par leur ID (ex: CDS/P/2MASS/J)
//   private ArrayList<String> tri;        // Liste des IDs afin de pouvoir les parcourirs en ordre alphanumérique
   protected int mocOrder=-1;              // Better MOC order
   private ArrayList<MyProperties> except = null;   // List of exceptions and associating rewriting rules
   private MyProperties example = null;  // List of existing properties with examples
//   private int nbThomas2Vizier;          // Nombre d'enregistrements convertis du format Thomas au format original
   private int nbConvertFromGtoC;        // Nombre de MOC converti de Galactic en Equatorial
   private int nbReduceMem;              // Nombre de MOC dont ont a réduit la résolution pour réduire la RAM
   
   public MultiMoc() {
      map = new HashMap<>(30000);
//      tri = new ArrayList<>(30000);
//      nbThomas2Vizier=0;
      nbConvertFromGtoC=0;
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
   
   /** Add or replace a MOC to the MultiMoc.
    * @param mocId  MOC identifier (unique)
    * @param moc MOC to memorize
    */
   public void add(String mocId, Moc moc, MyProperties prop, long dateMoc, long dateProp) throws Exception {
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
//      tri.remove(mocId);
   }
   
   /** Add directly a MocItem */
   public void add(MocItem mi) {
      map.put(mi.mocId,mi);
//      if( map.put(mi.mocId,mi)==null ) tri.add(mi.mocId);
   }
   
   /** Return directly a MocItem */
   public MocItem getItem(String mocId) {
      return map.get(mocId);
   }
   
   /** Return the MOC associated to a mocId, null if not found */
   public Moc getMoc(String mocId) {
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
      HashMap<String, Integer> count = new HashMap<>();
      
      for( MocItem mi : this ) {
         if( mi.prop==null ) continue;
         
         for( String k : mi.prop.getKeys() ) {
            
            // Décompte de l'usage de champ mot clé
            Integer a = count.get(k);
            if( a==null ) a=0;
            count.put(k,a+1);
            
            if( isNumSuffix(k) ) continue;
            if( prop.get(k)!=null ) continue;  // déjà pris en compte
            String s = mi.prop.get(k);
            if( s==null ) s="null for "+mi.mocId;
            int i = s.indexOf('\t');
            if( i>=0 ) s=s.substring(0,i);
            if( s.length()>=70 ) s = s.substring(0,67)+"...";
//            prop.put(k,"ex: "+s+" (from"+mi.mocId+")");
            prop.put(k,"ex: "+s);
         }
      }
      
      // Ajout des décompte
      for( String k : prop.getKeys() ) {
         if( isNumSuffix(k) ) continue;
         String v = prop.get(k);
         Integer a = count.get(k);
         String suffixUsage = suffixCounts(count,k);
         v = "("+a+"x"+suffixUsage+") "+v;
         prop.replaceValue(k,v);
      }
      return prop;
   }
   
   // Retourne true si la chaine se termine par "..._nnn" où nnn est numérique
   private boolean isNumSuffix(String s) {
      int i = s.lastIndexOf('_');
      if( i<0 ) return false;
      try {
         Integer.parseInt(s.substring(i+1));
         return true;
      } catch( Exception e) {}
      return false;
   }
   
   // Construit une chaine qui indique le nombre de propriétés existantes pour les dérivées de s suffixés par _nn
   // (ex: [+_1:924, 2:923, 3:694, 4:56 ... _7:10])
   private String suffixCounts( HashMap<String, Integer> count, String k) {
      if( !isNumSuffix(k+"_1") ) return "";
      int max=5;   // Nombre max de suffixes explicitement décomptés
      StringBuilder rep=null;
      int i;
      Integer a=null;
      for( i=0; true; i++ ) {
         String kSuf = k+"_"+i;
         a = count.get(kSuf);
         if( a==null ) {
            if( i>0 ) break;
            continue;
         }
         if( i<max ) {
            if( rep==null ) rep=new StringBuilder(" [+_");
            else rep.append(" _");
            rep.append(i+":"+a);
         }
      }
      if( rep!=null ) {
         if( i>=max ) {
            String kSuf = k+"_"+(i-1);
            a=count.get(kSuf);
            rep.append(" ... _"+(i-1));
            if( a!=null ) rep.append(":"+a);
         }
         rep.append("]");
      }
      return rep==null ? "" : rep.toString();
   }
   

   /** Clear the multiMoc */
   public void clear() {
      map.clear();
//      tri.clear();
   }
   
   static private Healpix hpx = new Healpix();
   
   public String adjustProp(MyProperties prop, String id, Moc moc) {
      
      String s;
      String mocId = getID(prop,id);
      
      // On ajoute manu-militari un id canonique
      prop.insert(KEY_ID, mocId);
      
      // Ajout de propriétés propres au MOC
      if( moc!=null ) {
         
         // On mémorise le type de MOC que l'on a
         String mocType =  moc instanceof STMoc ? "stmoc" : moc instanceof TMoc ? "tmoc" : "smoc";
         prop.replaceValue("moc_type", mocType);
         
         if( moc.isTime() ) {
            prop.replaceValue("moc_time_order",moc.getTimeOrder()+"");
            prop.replaceValue("moc_time_range",moc.getNbRanges()+"");
         }
         
         if( moc.isSpace() ) {
            try {
               SMoc m = moc.getSpaceMoc();
               prop.replaceValue("moc_sky_fraction", Unite.myRound( m.getCoverage() )+"" );

         s = moc.getProperty("MOCORDER");
         if( s==null ) s = moc.getProperty("MOCORD_S");

         // Petit bricolage horrible pour contourner les MocOrder que l'on a oublié
         // d'indiquer
         if( s==null || s.equals(""+SMoc.MAXORD_S) || s.equals("0") || s.equals("-1")) {
                  int maxOrder = m.getDeepestOrder();
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
                  SMoc mm = new SMoc();
                  mm.setMocOrder(n);
                  fov =  mm.getAngularRes()+"";
               } catch( Exception e) {
                        fov = m.getAngularRes()+"";
               }
            }
            if( ra==null || dec==null ) {
                     if( m.isFull() ) { ra="0"; dec="+0"; }
               else {
                  try {
                           int order = m.getMocOrder();
                           long pix = m.valIterator().next();
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
         } catch( Exception e ) {
            e.printStackTrace();
         }
      }
      }
      
      return mocId;
   }
   
//   // Détection des enregistrements manipulés par Thomas
//   // @return 0 - non, 1 - (table), 2 - obs_collection
//   private int isThomas(MyProperties prop) {
//      
//      // 1ère possibilité
//      String label = prop.getFirst("obs_label");
//      String title = prop.get("obs_title");
//      if( title!=null && label!=null ) {
//         int i = title.lastIndexOf('(');
//         if( i>0 ) {
//            String table = title.substring(i+1,title.length()-1);
//            if( label.equals(table) ) return 1;
//         }
//      }
//      
//      // 2ème possibilité
//      String collection = prop.get("obs_collection");
//      String collectionLabel = prop.getFirst("obs_collection_label");
//      if( collection!=null && collectionLabel!=null ) {
//         if( collection.equals(collectionLabel) ) return 2;
//      }
//      
//      return 0;
//   }
   
      
   /************************* PEAUFINAGE POUR LES CATALOGUE VIZIER EN ATTENDANT LE RETOUR CANONIQUE ****************/
      
   // POURRA ETRE VIRE DES QUE LES MODIFS DE THOMAS B. AURONT ETE SUPPRIMEES - PF JUIN 2021
//   private MyProperties thomas2VizieR(String id, MyProperties prop) {
//      int i;
//      if( prop==null ) return null;
//      
//      // Ne concerne pas VizieR => on ne bidouille pas dans le client
//      String type = prop.getProperty("dataproduct_type");
//      if( type == null || type.indexOf("catalog") < 0 ) return prop;
//      if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) return prop;      
//      
//      if( id.equals("CDS/I/317/sample") ) {
//         System.out.println("j'y suis AVANT\n"+prop);
//      }
//
//      int th = isThomas(prop);
//      if( th==0 ) return prop;
//      
//      prop = prop.clone();
//      
//      String title = prop.get("obs_title");
//      if( th==1 ) {
//         i = title.lastIndexOf('(');
//         title = title.substring(0, i - 1);
//      }
//      prop.remove("obs_collection");
//      prop.replaceKey("obs_title","obs_collection");
//      prop.replaceValue("obs_collection",title);
//      prop.replaceKey("obs_description","obs_title");
////      prop.replaceValue("client_category", "Catalog/VizieR");
////      System.out.println("\n\n"+prop);
//      
//      nbThomas2Vizier++;
//      
//      return prop;
//    }
//   
//   // POURRA ETRE VIRE DES QUE LS VIEUX CLIENTS UNIQUEMENT COMPATIBLES 
//   // AVEC L'ANCIENNE FORMULE THOMAS AURONT DISPARUS - PF JUIN 2021
//   public MyProperties vizier2Thomas(String id, MyProperties prop) {
//      if( prop==null ) return null;
//      
//      // Ne concerne pas VizieR => on ne bidouille pas dans le client
//      String type = prop.getProperty("dataproduct_type");
//      if( type == null || type.indexOf("catalog") < 0 ) return prop;
//      if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) return prop;      
//      
//      prop = prop.clone();
////      System.out.println("Multimoc.peaufinageVizieR(...) => vizier2Thomas ["+id+"]");
//     
//      String label = prop.getFirst("obs_label");
//      String t = prop.get("obs_collection")+" ("+label+")";
//
//      prop.replaceKey("obs_title","obs_description");
//      prop.replaceKey("obs_collection","obs_title");
//      prop.replaceValue("obs_title",t);
//      
//      String collection = prop.getFirst("obs_collection_label");
//      if( collection!=null ) prop.add("obs_collection", collection);
//      
//      return prop;
//    }
     
      
   /********************************************** FIN DU PEAUFINAGE VIZIER ***********************************/
   

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

      String s;
      
      
      // (Re)chargement du fichiers d'exceptions
      try { loadException(exceptFile); } catch( Exception e) {}
      
      // taille originale
      int originalSize = oMM==null ? 0 : oMM.map.size();

      // Détermination de tous les fichiers concernés
      ConcurrentLinkedQueue<File> list = new ConcurrentLinkedQueue<>();
      
      long t0=System.currentTimeMillis();
      
      // Constitution de la liste des fichiers à traiter présents dans les répertoires
      for( String dir : dirs) {
         File f = new File(dir);

         File [] listF = f.listFiles();
         if( listF==null ) continue;

         for( File f1 : listF ) {
            String name = f1.getAbsolutePath();

            // On ne prend pas individuellement en compte les fichiers de properties si le MOC existe
            if( name.endsWith(".prop") ) {
               int offset = name.lastIndexOf('.');
               String mocFile = name.substring(0,offset)+".fits";
               if( (new File(mocFile)).exists()) continue;
            }

            list.add(f1);
         }
      }
      
      // Détermination du nombre de Readers (comme il y aura des IO, on peut largement
      // dépasser le nombre de threads dispo sans tuer la machine)
      int nbReaders = Runtime.getRuntime().availableProcessors()-1;
      if( nbReaders>10 ) nbReaders=10;
      
      if( list.size()<nbReaders ) nbReaders=list.size();
      if( nbReaders<1 ) nbReaders=1;
      s="MultiMoc loading/parsing "+list.size()+" files by "+nbReaders+" threads...\n";
      print(out,s);
     
      // Partage des taches, et exécution en parallèle
      Reader reader [] = new Reader[ nbReaders ];
      HashSet<String> listId = new HashSet<>(list.size() );
      for( int i=0; i<reader.length; i++ ) {
         reader[i] = new Reader(oMM,flagWithMoc,out,list,listId);
         reader[i].start();
      }
      
      // Attente de la fin des travaux
      boolean encore = true;
      int oNbFiles=0;
      long t1 = System.currentTimeMillis();
      while( encore ) {
         int nbFiles=0;
         encore=false;
         int nth=0;
         for( Reader r : reader ) {
            if( r.running() ) nth++;
            encore |= r.running();
            nbFiles+= r.nbFiles;
         }
         long t2 = System.currentTimeMillis();
         if( (t2-t1)>5000 ) {
            s=nbFiles+"... in "+Unite.getTemps(t2-t0)
            +" => "+(int)(((double)nbFiles-oNbFiles)/((t2-t1)/1000.))+"/s"
                  +(nth<nbReaders ? " ("+nth+" readers running)\n":"\n");
            print(out,s);
            t1=t2;
            oNbFiles=nbFiles;
         }
         try { Thread.sleep(1000); } catch( Exception e ) {}
      }
      
      if( list.size()>0 ) {
         s="Loading process not achieved (not enough RAM ?) => keep MocServer as it was before";
         print(out,s);
         throw new Exception(s);
      }
      
      // Traitement pour règler le retour aux données originales (cf. modif contenu Thomas B.)
//      for( MocItem mi : this )  mi.prop = thomas2VizieR(mi.mocId, mi.prop);

      // Mise à jour des compteurs
      int nbFiles=0;
      int nbCreation=0;
      int nbReused=0;
      for( Reader r : reader ) {
         nbFiles+= r.nbFiles;
         nbCreation+= r.nbCreation;
         nbReused+= r.nbReused;
      }

      double nb = nbFiles%1000;
      long t2 = System.currentTimeMillis();
      s=nbFiles+" loaded in "+Unite.getTemps(t2-t0)+" => avg: "+(int)(nb/((t2-t0)/1000.))+"/s\n";
//      s=s+(nbThomas2Vizier>0?" - "+nbThomas2Vizier+" prop re-translated to orig":"");
      s=s+(nbConvertFromGtoC>0?" - "+nbConvertFromGtoC+" moc converted from G to C":"")
            +(nbReduceMem>0?" - "+nbReduceMem+" moc RAM reduced":"")
         +"\n";

      print(out,s);

      if( oMM!=null ) {
         s = " => "+nbCreation+" created or updated - "+nbReused+" reused as is\n";
         print(out,s);
      }
      
      example=null;
      return nbCreation>0 || nbReused!=originalSize ;
   }

   /** Reader de Moc et Prop */
   class Reader extends Thread {
      ConcurrentLinkedQueue<File> list; // La liste des fichiers à lire
      boolean running;                  // true si le Thread est encore en train de travailler
      MultiMoc oMM;
      HashSet<String> listId;           // Liste des mocId déjà traités
      boolean flagWithMoc;
      PrintWriter out;
      int nbFiles;                      // Nombre de fichiers traités par le reader
      int nbCreation,nbReused;          // Compteurs propres au reader
     
      Reader(MultiMoc oMM,boolean flagWithMoc, PrintWriter out,
            ConcurrentLinkedQueue<File> list, HashSet<String> listId) {
         this.oMM=oMM;
         this.flagWithMoc=flagWithMoc;
         this.out=out;
         this.list = list;
         this.listId=listId;
         running=true;
      }
      
      boolean running() { return running; }
      
      public void run() {
         try { reload( this ); } 
         catch( Throwable e) { }
         running=false;
      }
   }
   
   private long lastGc = 0L;    // Last manual GC time
   
   /** Retourne le nombre d'octets disponibles en RAM */
   private long getFreeMem() {
      return Runtime.getRuntime().maxMemory()-
            (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
   }
   
   // Check if there is enough memory to process "more" bytes
   private boolean checkMem(long more ) {
      long freeMem = getFreeMem();
      if( freeMem > more*5 ) return true;
      synchronized( this ) {
         freeMem = getFreeMem();
         if( freeMem > more*5 ) return true;
         long t = System.currentTimeMillis();
         if( t-lastGc>1000 ) {
//            System.out.println("Launching manual GC...");
            lastGc=t;
            System.gc();
            freeMem = getFreeMem();
            if( freeMem > more*3 ) return true;
         }
      }
      return false;
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
   public void reload( Reader r) throws Exception {
      r.nbFiles=0; 
      r.nbCreation=0;
      r.nbReused=0;
      String mocId=null;
      long dateMoc,dateProp;
//      TEST=0;

      File f1;
      String filename=null,propname=null;
      while( (f1 = r.list.poll())!=null ) {
         Moc moc;
            MyProperties prop;

            try {
               //            System.out.print("Loading "+f1.getName()+"...");
            filename = f1.getAbsolutePath();
               dateMoc = (new File(filename)).lastModified();
               //            dateMoc = !(new File(filename)).exists() ? 0L :Files.getLastModifiedTime(FileSystems.getDefault().getPath(filename)).toMillis();

               // Changement d'extension => ".prop"
            propname = filename;
               int offset = propname.lastIndexOf('.');
               if( offset==-1 ) offset=propname.length();
               propname = propname.substring(0, offset)+".prop";
               File fprop = new File(propname);
               dateProp = !fprop.exists() ? 0L : fprop.lastModified();
               //            dateProp = !fprop.exists() ? 0L :Files.getLastModifiedTime(FileSystems.getDefault().getPath(propname)).toMillis();

               mocId = getMocId( f1.getName() );
            prop=null;
            try {
               prop = loadProp(propname);
            } catch( Exception e ) {
               print(r.out,"! PropError "+mocId+" ["+e.getMessage()+"] => file ["+propname+"] ignored\n");
            }
            try {
               if( filename.endsWith(".prop") ) moc=null;
               else {
                   if( !checkMem((new File(filename).length()) ) ) {
                      if( DEBUG ) System.out.println("LowMemory during "+f1+" process => stop a thread and redo");
                      r.list.add(f1);
//                      throw new Exception("LowMemory");
                      return;
                  }
                  moc=null;
                  try { 
                   moc =  loadMoc(filename,prop);
                  } catch( Exception e ) {
                     print(r.out,"! MocError "+mocId+" ["+e.getMessage()+"] => file ["+filename+"] ignored\n");
                  }
               }
            } catch( OutOfMemoryError e ) {
               if( DEBUG ) System.out.println("OutOfMemory during "+f1+" process => stop a thread and redo");
               r.list.add(f1);
//               throw e;
               return;
            }
               if( prop==null && moc==null ) continue;
               if( prop==null && moc!=null ) prop = new MyProperties();
               mocId = adjustProp(prop,mocId,moc);
               if( !exceptProp(prop,mocId) ) { prop=null; moc=null; }

               // Dans le cas où je ne mémorise pas le Moc
               // Mais j'ai été obligé de le lire pour mettre à jour les champs obs_initial_ra, etc.
            if( !r.flagWithMoc ) {  moc=null; dateMoc=0L; }

               // En fin compte, y a rien à voir !
               if( moc==null && prop==null ) continue;

               MocItem mi=null;

               // Comparaison et réutilisation avec celui déjà chargé s'il existe
               boolean flagCreation=true;
               
            synchronized( this ) {
               
               // Pour éviter les doublons
               if( r.listId.contains(mocId) ) throw new Exception("Duplicate ID");
               r.listId.add(mocId);
               
               if( r.oMM!=null && (mi=r.oMM.getItem(mocId))!=null ) {
                  
                  // Pas de changement ? => on le réutilise
                  if( (dateMoc==0L || dateMoc==mi.dateMoc) 
                        && prop!=null && mi.prop!=null && prop.equals(mi.prop) ) {
                     add(mi);
                     flagCreation=false;
                     r.nbReused++;
//                     System.out.println("Réutilisation de "+mocId);
                  }
               }
               
               if( flagCreation ) {
//                  if( TEST<10 ) { 
//                     System.out.println("Ajout de "+mocId+" datemoc="+dateMoc+" dateProp="+dateProp);
//                     if( prop!=null && mi!=null ) System.out.println("   Test d'égalité: "+prop.equals(mi.prop));
//                     if( mi!=null ) System.out.println("   Test des dates: dateMoc="+dateMoc+" mi.dateMoc="+mi.dateMoc+" => "+(dateMoc==mi.dateMoc));
//                     TEST++; 
//                     if( mi!=null && mi.prop!=null && prop!=null ) showDiff(mocId,mi.prop,prop);
//                  }  
                  add(mocId,moc,prop,dateMoc,dateProp);
                  r.nbCreation++;
               }
            }

            r.nbFiles++;
            
         } catch( Exception e ) {
            try {
               int x = propname.lastIndexOf('.');
               String file = propname.substring(0,x);
               print(r.out,"!"+mocId+" ["+e.getMessage()+"] => files ["+file+".xxx] ignored\n");
            } catch( Exception e1 ) { e1.printStackTrace(); }
         }
      }
   }
   
//   static int TEST=0;
   
//   private void showDiff(String id,MyProperties pa,MyProperties p) {
//      
//      System.out.println("Comparaison pour "+id);
//      // Tests rapides
//      if( Math.abs( pa.size()-p.size() )>1 ) {
//         System.out.println(".Plus d'un champ différent => false");
//      }
//
//      // On compare les clés une à une
//      for( String k : pa.getKeys() ) {
//         if( k.equals(" ") || k.equals("#") ) continue; // On ne compare les commentaires
//         if( k.equals("TIMESTAMP") ) continue;          // On ne compare pas sur l'estampillage
//         String v1 = p.get(k);
//         String v = pa.get(k);
//         if( v1==v ) continue;
//         if( v1==null && v!=null || v1!=null && v==null ) {
//            System.out.println(".Un des champs de "+k+" est null: v1="+v1+" v="+v+" => false");
//            return;
//         }
//         
//         // Deux valeurs simples ?
//         if( v1.indexOf('\t')<0 && v.indexOf('\t')<0 ) {
//            
//            // Cas particulier de la popularite VizieR
//            // On estime différent si variation d'au moins 10%
//            if( k.equals("vizier_popularity") ) {
//               try {
//                  int pop  = Integer.parseInt(v);
//                  int pop1 = Integer.parseInt(v1);
//                  double var = (double)pop/pop1;
//                  if( var<0.9 || var>1.1 ) {
//                     System.out.println(".vizier_popularity trop différente: v1="+v1+" v="+v+" => false");
//                     return;
//                  }
//               } catch( Exception e ) { 
//                  System.out.println(".vizier_popularity erreur ["+e.getMessage()+"] => false");
//                  return;
//         }
//               
//            } else if( !v1.equals(v) ) {
//               System.out.println(".Valeurs différentes: pour ["+k+"] v1="+v1+"\n\t\tv="+v+" => false");
//               return;
//               }
//               
//         // Des valeurs multiples => il faut comparer chaque possibilité de valeur
//         } else if( !v1.equals(v) ) {
//            int n=0,n1=0;
//            Tok tok = new Tok(v,"\t");
//            HashMap<String, String> hash = new HashMap<>(100);
//            while( tok.hasMoreTokens() ) { hash.put(tok.nextToken(),""); n++; }
//
//            tok = new Tok(v1,"\t");
//            while( tok.hasMoreTokens() ) {
//               String it = tok.nextToken();
//               if( hash.get(it)==null ) {
//                  System.out.println(".Valeurs multiples pour ["+k+"] , item non trouvé ["+it+"] => false");
//                  return;
//               }
//               n1++; 
//            }
//            if( n!=n1 ) {
//               System.out.println(".Valeurs multiples ["+k+"] => pas le même nombres d'items => false");
//            }
//         }
//   }
//   }
               
//   /**
//    * Addition/update of all MOCs of a dedicated directory with possible comparaison with previous state
//    * @param dirs input directory list
//    * @param exceptFile Exception.prop file path, or null
//    * @param oMM previous MultiMoc, or null
//    * @param flagWithMoc false for avoiding to load Moc (only Prop)
//    * @param out
//    * @return true if something has been changed compared to oMM
//    * @throws Exception
//    */
//   public boolean reload(String [] dirs, String exceptFile, MultiMoc oMM,boolean flagWithMoc, PrintWriter out) throws Exception {
//      int i=0; 
//      String mocId=null;
//      long dateMoc,dateProp;
//      int nbCreation=0;
//      int nbReused=0;
//      int originalSize = oMM==null ? 0 : oMM.map.size();
//      long t = System.currentTimeMillis();
//      String s;
//      
//      
//      // (Re)chargement du fichiers d'exceptions
//      try { loadException(exceptFile); } catch( Exception e) {}
//      
//      long t1=System.currentTimeMillis();
//      
//      // Parcours des fichiers présents dans les répertoires
//      for( String dir : dirs) {
//         File f = new File(dir);
//
//         File [] listF = f.listFiles();
//         if( listF==null ) continue;
//
//         for( File f1 : listF ) {
//            String name = f1.getAbsolutePath();
//
//            // On ne prend pas individuellement en compte les fichiers de properties
//            // si le MOC existe
//            if( name.endsWith(".prop") ) {
//               int offset = name.lastIndexOf('.');
//               String mocFile = name.substring(0,offset)+".fits";
//               if( (new File(mocFile)).exists()) continue;
//            }
//
//            SMoc moc;
//            MyProperties prop;
//
//            try {
//               //            System.out.print("Loading "+f1.getName()+"...");
//               String filename = f1.getAbsolutePath();
//               dateMoc = (new File(filename)).lastModified();
//               //            dateMoc = !(new File(filename)).exists() ? 0L :Files.getLastModifiedTime(FileSystems.getDefault().getPath(filename)).toMillis();
//
//               // Changement d'extension => ".prop"
//               String propname = filename;
//               int offset = propname.lastIndexOf('.');
//               if( offset==-1 ) offset=propname.length();
//               propname = propname.substring(0, offset)+".prop";
//               File fprop = new File(propname);
//               dateProp = !fprop.exists() ? 0L : fprop.lastModified();
//               //            dateProp = !fprop.exists() ? 0L :Files.getLastModifiedTime(FileSystems.getDefault().getPath(propname)).toMillis();
//
//               mocId = getMocId( f1.getName() );
//
//               // Chargement du Moc et des Prop
//               moc = loadMoc(filename);
//               prop = loadProp(propname);
//               if( prop==null && moc==null ) continue;
//               if( prop==null && moc!=null ) prop = new MyProperties();
//               mocId = adjustProp(prop,mocId,moc);
//               if( moc!=null && !moc.getSys().equals(COORDSYS) ) moc=null; // Incompatible MOC coordsys
//
//               if( !exceptProp(prop,mocId) ) { prop=null; moc=null; }
//
//               // Dans le cas où je ne mémorise pas le Moc
//               // Mais j'ai été obligé de le lire pour mettre à jour les champs obs_initial_ra, etc.
//               if( !flagWithMoc ) {  moc=null; dateMoc=0L; }
//
//               // En fin compte, y a rien à voir !
//               if( moc==null && prop==null ) continue;
//
//               MocItem mi=null;
//
//               // Comparaison et réutilisation avec celui déjà chargé s'il existe
//               boolean flagCreation=true;
//                  
//               if( oMM!=null && (mi=oMM.getItem(mocId))!=null ) {
//
//                  // Pas de changement ? => on le réutilise
//                  if( dateMoc==mi.dateMoc && prop!=null && mi.prop!=null && prop.equals(mi.prop) ) {
//                  add(mi);
//                     flagCreation=false;
//                     nbReused++;
////                     System.out.println("Réutilisation de "+mocId);
//                  }
//               }
//
//               if( flagCreation ) {
//                  add(mocId,moc,prop,dateMoc,dateProp);
////                  System.out.println("Ajout de "+mocId);
//                  nbCreation++;
//               }
//
//               i++;
//               if( i%1000==0 && out!=null ) {
//                  long t2 = System.currentTimeMillis();
//                  s=i+"... in "+Unite.getTemps(t2-t)+" => "+(int)(1000./((t2-t1)/1000.))+" per sec\n";
//                  print(out,s);
//                  System.out.print(s);
//                  t1=t2;
//               }
//
//            } catch( Exception e ) {
//               e.printStackTrace();
//               print(out,mocId+" ["+e.getMessage()+"] => ignored\n");
//            }
//         }
//      }
//      
//      if( out!=null && i%1000!=0 ) {
//         double nb = i%1000;
//         long t2 = System.currentTimeMillis();
//         s=i+"... in "+Unite.getTemps(t2-t)+" => "+(int)(nb/((t2-t1)/1000.))+" per sec\n";
//         print(out,s);
//         System.out.print(s);
//      }
//      
//      if( out!=null && oMM!=null ) {
//         s = " => "+nbCreation+" created or updated, "+nbReused+" reused as is\n";
//         print(out,s);
//         System.out.print(s);
//      }
//
//      
//      example=null;
//      return nbCreation>0 || nbReused!=originalSize ;
//               }

   // Just to print with flushing   
   private void print(PrintWriter out,String s) {
      if( out==null || s==null ) return;
      if( s.startsWith("!!") ) s="<font color=\"red\">"+s+"</font>";
      else if( s.startsWith("!") ) s="<font color=\"orange\">"+s+"</font>";
      else if( s.startsWith(".") ) s="<font color=\"grey\">"+s+"</font>";
      out.print(s);
      out.flush();
   }
   
   // Apply Exception rules and prop, return false if the record must be deleted (creator_did absent => removed)
   public boolean exceptProp(MyProperties prop,String id) {
      if( except==null || prop==null ) return true;
      for( MyProperties p : except ) prop.exceptions(p,id);
      return prop.get(KEY_ID)==null || prop.get("MOCSERVER_REMOVE")==null;   // A virer plus tard
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
      ArrayList<MyProperties> p = new ArrayList<>();
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

   public void sort() {  }
//   /** Tri des Mocs en ordre alphanum sur les ID */
//   public void sort() { Collections.sort(tri); }
//   
//   /** Tri des Mocs en fonction de la valeur de champs particuliers
//    * La clé de tri prend en compte une succession de champs */
//   public void sort(final String [] keys) {
//      Collections.sort(tri, new Comparator<String>() {
//
//         @Override
//         public int compare(String o1, String o2) {
//            MocItem mi1 = o1==null ? null : map.get(o1);
//            MocItem mi2 = o2==null ? null : map.get(o2);
//
//            String k1,k2;
//            for( String key : keys ) {
//               k1 = mi1==null || mi1.prop==null ? null : mi1.prop.get(key);
//               k2 = mi2==null || mi2.prop==null ? null : mi2.prop.get(key);
//               if( k1==null ) k1="";
//               if( k2==null ) k2="";
//               int rep = k1.compareTo(k2);
//               if( rep!=0 ) return rep;
//            }
//            return 0;
//         }
//      });
//   }
   
   // Extraction of MOC id from MOC file name
   private String getMocId(String filename) {
      String id = filename.replace("_","/");
      int offset = id.lastIndexOf('.');
      if( offset==-1 ) return id;
      return id.substring(0,offset);
   }
   
   /** Changement de référentiel d'un MOC */
   static final public SMoc convertToICRS(SMoc moc) throws Exception {
      String altsys = moc.getSys();
      if( altsys.equals("C") 
            || altsys.equalsIgnoreCase("equatorial") || altsys.equalsIgnoreCase("equ")) return moc;   // déjà en ICRS
      
      if( altsys.equalsIgnoreCase("galactic") || altsys.equalsIgnoreCase("gal")) altsys="G";
      else if( altsys.equalsIgnoreCase("ecliptic") || altsys.equalsIgnoreCase("ecl")) altsys="E";
      char a = altsys.charAt(0);
      
      // Système de coordonnée non supporté par le MocServer (probablement du planéto)
      if( a!='G' && a!='E' ) return null;
      
      // Ca va prendre trop de temps si on garde la résolution max
      if( moc.getDeepestOrder()>10 && moc.getCoverage()>0.99 && !moc.isFull() ) {
         moc.setMocOrder(10);
      }
      
      // Ciel complet => cas trivial
      if( moc.isFull()) { moc.setSys("C"); return moc; }
      
//      Astroframe frameSrc = a=='G' ? new Galactic() : a=='E' ? new Ecliptic() : new ICRS();
      Astroframe frameSrc = Astroframe.create( a=='G' ?"Galactic" : a=='E' ? "Ecliptic" : "ICRS");
      Healpix hpx = new Healpix();
      int order = moc.getDeepestOrder();
      SMoc moc1 = moc.dup();
      moc1.bufferOn();
      long onpix1=-1;
      Coo c =new Coo();
      Iterator<Long> it = moc.valIterator();
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
         }
      }
      moc1.bufferOff();
      return moc1;
   }


   
   // Moc Loading
   private Moc loadMoc(String filename, MyProperties prop) throws Exception {
      Moc moc=null;
      FileInputStream fi=null;
      try {
         fi = new FileInputStream( new File(filename));
         moc = Moc.createMoc(fi);
         if( moc.reduction(20 *1024L*1024L) ) {
            nbReduceMem++;
//            if( Data.DEBUG) System.err.println("Moc info: resolution reduced to "+moc.getMem()/(1024L*1024L)+"MB => "+filename);
         }
         fi.close(); fi=null;
         if( moc.isEmpty() ) moc=null;
         
         // Check validity
            moc.seeRangeList().checkConsistency();

         if( moc instanceof SMoc ) {
            
         // Pas dans le bon système de référence
            String sys=((SMoc)moc).getSys();
         if( !sys.equals(COORDSYS) ) {
            long t = System.currentTimeMillis();
            try {
                  moc = convertToICRS(((SMoc)moc));
                  if( moc==null ) throw new Exception("Moc coordsys not supported by MocServer");
                  nbConvertFromGtoC++;
//                  if( Data.DEBUG) System.out.println(filename+" MOC convert from "+sys+" to "+COORDSYS+" in "+ (System.currentTimeMillis()-t)+"ms");
            } catch( Exception e ) {
//                  if( Data.DEBUG) System.out.println(filename+" MOC conversion error or COORDSYS not supported ("+((SMoc)moc).getSys()+") => ignored");
//               e.printStackTrace();
               throw e;
            }
         }
            
            // Peut être ai-je une indication TMIN, TMAX ? => on fait un STMOC sur un range temporel
            double jdRange [] = getTimeRange(prop);
            if( jdRange!=null ) {
               STMoc moc1 = new STMoc(41, moc.getSpaceOrder() );
               moc1.add( jdRange[0],jdRange[1], (SMoc)moc );
               moc = moc1;
            }
         }
      } finally { if( fi!=null ) { try { fi.close(); } catch( Exception e ) {} } }
      return moc;
   }
   
   /** Extrait le range temporelle d'un Prop à partir des t_min et t_max (Attention MJD -> JD ), null sinon */
   private double [] getTimeRange(MyProperties prop) {
      if( prop==null ) return null;
      double tmin=Double.NaN;
      double tmax=Double.NaN;
      try { tmin = Double.parseDouble( prop.get("t_min") )+2400000.5; } catch( Exception e ) {}
      try { tmax = Double.parseDouble( prop.get("t_max") )+2400000.5; } catch( Exception e ) {}
      if( Double.isNaN(tmin) && Double.isNaN(tmax) ) return null;
      if( tmin>tmax ) { double x=tmin; tmin=tmax; tmax=x; }
      return new double[] { tmin, tmax };
   }

   // Properties loading 
   private MyProperties loadProp(String propname) throws Exception {
      
      // Recherche du fichier de properties
      File f = new File(propname);
      if( !f.exists() ) return null;
      
      // Chargement des propriétés
      InputStreamReader in=null;
      MyProperties prop = null;
      try {
         prop = new MyProperties();
         in = new InputStreamReader( new BufferedInputStream( new FileInputStream(f) ), "UTF8");
         prop.load( in );
      } finally {
         if( in!=null ) try {  in.close(); } catch( Exception e ) {}         
      }
      return prop;
   }

   public Iterator<MocItem> iterator() { return iterator(false); }
   
   /** Retourne un itérateur sur les valeurs du MultiMoc. Si flagCopy==true, il s'agira d'une 
    * copy de la liste des références our éviter les soucis d'accès concurrents
    * @param flagCopy
    * @return
    */
   public Iterator<MocItem> iterator(boolean flagCopy) {
      if( !flagCopy ) return map.values().iterator();
      ArrayList<MocItem> list = new ArrayList<>( map.size() );
      for( MocItem mi : map.values() ) list.add(mi);
      return list.iterator();
   }
   
//   /** Iterator on MocItem */
//   public Iterator<MocItem> iterator1() { return new ItemIterator(); }
//   
//   private class ItemIterator implements Iterator<MocItem> {
//      int i=0;
//      public boolean hasNext() { return i<tri.size(); }
//      public MocItem next() { return map.get(  tri.get(i++) ); }
//      public void remove() { }
//   }
   
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
      ArrayList<String> res = new ArrayList<>();
      for( MocItem mi : this ) {
         if( mask!=null && MyProperties.matchMask(mask, mi.mocId )==match ) continue;
         try {
            if( mi.moc!=null && mi.moc.getSpaceMoc().isIntersecting(order, npix) ) res.add(mi.mocId);
         } catch( Exception e ) { continue; }
      }
      Collections.sort(res);
      return res;
   }
   
   /**
    * Search by MOC.
    * @param moc MOC describing input sky region
    * @return list of MOC identifiers
    */
   public ArrayList<String> scan(SMoc moc) { return scan(moc, (HashMap<String,String[]>)null, true, -1, OVERLAPS); }
   
   
   // Détermination de l'ID, soit par le creator_did, ou le publisher_did sinon creator_id?obs_id ou publisher_id?obs_id
   // et encore sinon, avec le filename passé en paramètre, enfin null si rien à faire
   // sans le préfixe ivo://
   static public String getID(MyProperties prop) { return getID(prop,null); }
   
   static public String getID(MyProperties prop,String filename) {   
      
      // L'ID est donné explicitement (ex: CDS/P/DSS2/color)
      String id = prop.getProperty("ID");
      if( id==null ) {

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
      }
      
      // On met en minuscules l'authority
//      int i = id.indexOf('/');
//      if( i==-1 ) i=id.length();
//      id = id.substring(0,i).toLowerCase()+id.substring(i);
      
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
   public ArrayList<String> scan(Moc moc,HashMap<String, String[]> mapFilter, boolean casesens, int top, int intersect ) {
      ArrayList<String> res = new ArrayList<>();

      boolean scanTime = moc!=null && moc.isTime();
      boolean scanSpace = moc!=null && moc.isSpace();
      
      int n=0;
      for( MocItem mi : this ) {
         if( mapFilter!=null && !match(mi,mapFilter,casesens,true)) continue;
         if( moc!=null ) {
            if( mi.moc==null ) continue;
            if( scanSpace && !mi.moc.isSpace() ) continue;
            if( scanTime && !mi.moc.isTime() ) continue;
            try {
               if( intersect==OVERLAPS ) {
                  if( !moc.isIntersecting(mi.moc) ) continue;
               } else if( intersect==ENCLOSED ) {
                  if( !mi.moc.isIncluding(moc) ) continue;
               } else { // COVERS
                  if( !moc.isIncluding(mi.moc) ) continue;
               }
            }  catch( Exception e ) { continue; }
         }
         res.add(mi.mocId);
         if( top!=-1 && (++n)>=top ) return res;
      }

      Collections.sort(res);
      return res;
   }
   
   /**
    * Search by wildcard mask.
    * @param mask mask wildcard mask for selecting MOC identifiers (wildcard syntax: *,?,! => not if in first pos))
    * @return
    */
   public ArrayList<String> scan(HashMap<String,String[]> mapFilter) {
      return scan( (SMoc)null, mapFilter, true, -1, OVERLAPS);
   }
   
   /**
    * Scanning by MOC and logical expression based on set algebra
    * @param moc MOC describing input sky region
    * @param expr expression based on set algebra
    * @param casesensitive case sensitive (true by default) - never applied for ID field
    * @param intersect true implies that all sky region must be inside
    * @return list of IDs (keep the original MultiMoc order)
    */
   public ArrayList<String> scan( Moc moc, String expr, boolean casesens, int top, int intersect ) throws Exception {
      ArrayList<String> res = new ArrayList<>();
      
      // Détermination des IDs candidats
      HashSet<String> candidateIds = scanExpr(expr,casesens);
      if( candidateIds.size()==0 ) return res;

      boolean scanTime = moc!=null && moc.isTime();
      boolean scanSpace = moc!=null && moc.isSpace();

      int n=0;
      for( MocItem mi : this ) {
         if( !candidateIds.contains(mi.mocId) ) continue;
         if( moc!=null ) {
            if( mi.moc==null ) continue;
            if( scanSpace && !mi.moc.isSpace() ) continue;
            if( scanTime && !mi.moc.isTime() ) continue;
            try {
            if( intersect==OVERLAPS ) {
                  if( !moc.isIntersecting(mi.moc) ) continue;
            } else if( intersect==ENCLOSED ) {
               if( !mi.moc.isIncluding(moc) ) continue;
            } else { // COVERS
               if( !moc.isIncluding(mi.moc) ) continue;
            }
            }  catch( Exception e ) { continue; }
         }
         res.add(mi.mocId);
         if( top!=-1 && (++n)>=top ) return res;
      }

      Collections.sort(res);
      return res;
   }
   
   /**
    * Full scanning.
    * @return list of IDs (keep the original MultiMoc order)
    */
   public ArrayList<String> scan() { 
      ArrayList<String> res = new ArrayList<> (this.size() );
      for( MocItem mi : this ) res.add(mi.mocId);
      Collections.sort(res);
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
      return scan( (SMoc)null, mask, casesensitive, -1, OVERLAPS);
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
         op.res = new HashSet<>( 10 );   // <= je crée une Hashset petite car ce ne sera probablement 
                                               //    jamais elle qui sera conservé lors des opérations ensemblistes 
         MocItem mi1 = getItem(val);
//         System.out.println("Accès direct pour "+val);
         if( mi1!=null ) op.res.add(mi1.mocId);
         return;
      }
      
     
      HashMap<String, String[]> mapFilters = new HashMap<>();
      mapFilters.put( key, new String[] { val } );
      
      // Scanning du multimoc et mémorisation des éléments qui correspondent
      ArrayList<String> res = scan( (SMoc)null, mapFilters, casesens, -1, OVERLAPS);
      op.res = new HashSet<>( Math.max(2*res.size(), size()) );   // <= Je crée une HashSet assez grande, car elle est susceptible
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

   /** Return the biggest TIMESTAMP used, 0 if unkonwn */
   public long getBiggestTimeStamp() {
      long t=0L;
      for( MocItem mi : this ) {
         if( mi.dateProp>t ) t=mi.dateProp;
      }
      return t;
   }

   /**
    * Number of MOCS.
    * @return The number of MOCs
    */
   public int size() { return map.size(); }
   
   
   /**
    * Number of MOCS of a dedicated class (0-SMoc, 1-TMoc, 2-STMoc)
    * @return The number of MOCs of this class
    */
   public int size( int typeMoc ) {
      int size=0;
      for( MocItem mi : this ) {
         if( mi.moc==null ) continue;
         if( typeMoc==0 && mi.moc instanceof SMoc ) size++;
         else if( typeMoc==1 && mi.moc instanceof TMoc ) size++;
         else if( typeMoc==2 && mi.moc instanceof STMoc ) size++;
      }
      return size;
   }
   
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
   
   /**
    * Size in memory for properties only (no MOCs).
    * @return size in memory in bytes
    */
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
      return "MultiMoc: hash="+hashCode()+" nbmoc="+size()+" mem="+getMem()/(1024L*1024L)+"MB";
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
