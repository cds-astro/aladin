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

package cds.aladin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import cds.tools.Astrodate;
import cds.tools.Util;

/**
 * Gestion des tris associés à l'arbre des collections
 * @author Pierre Fernique [CDS]
 * @version 1.0 Janvier 2018 - création
 */
public class DirectorySort {
   
   public static final String OTHERS      = "Others";
   public static final String PROBLEMATIC = "Problematic";
   public static final String ADDS        = "Adds";
   
   public static final String[] BRANCHES   = { 
         "Image", "Data base", "Catalog", "Cube", "Outreach", OTHERS, PROBLEMATIC };
   
   public static final String[] PROTOCOLS = { "HiPS", "SIA2 (image|cube)", "SIA (image)",
         "SSA (spectrum)","CS (table)","TAP (table)"};
   public static final int HIPS=0, SIA2=1, SIA=2, SSA=3, CS=4, TAP=5;
   
   public static final String[] DATATYPES = { "Image","Cube","Table","Spectra" };
   public static final int IMAGE= 0, CUBE=1, TABLE=2, SPECTRUM=3;
   
   public static final String[] COVRANGES = {  "Whole sky" , "75% to 100%", "50% to 75%", ">25% to 50%" ,
         "10% to 25%", "1% to 10%", "less than 1%" };
   public static final int COV100=0, COV75=1, COV50=2, COV25=3, COV10=4, COV1=5, COV0=6;
   
   public static final String[] REGIMEHIPS = { 
         "Gamma-ray","X-ray","EUV","UV","Optical","Infrared","Millimeter","Radio","Gas-lines" };
   public static final String [][] REGIMEALIAS = {
         {"gammaray","gamma"},{"xray","x"},{"euv","extremeultraviolet","extremeuv"},{"uv","ultraviolet"},
         {"optical","optic","visible"},{"ir","infrared"},{"mm","millimeter"},
         {"radio","rad"},{"gaslines","gasline"} };
   
   public static final String[] CATCODE   = { 
         "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "B", "J" };
   public static final String[] CATLIB = { 
         "I-Astrometric Data", "II-Photometric Data", "III-Spectroscopic Data",
         "IV-Cross-Identifications", "V-Combined data", "VI-Miscellaneous", "VII-Non-stellar Objects",
         "VIII-Radio and Far-IR data", "IX-High-Energy data", "B-External databases, regularly updated",
         "Journal table"};
   public static final String[] PLANETS   = { 
         "Mercury","Venus","Earth","Mars","Saturn","Jupiter","Uranus","Neptune","Pluton","Charon" };
  
   static final public int DEFAULT    = 0;
   static final public int BRANCH     = 1;
   static final public int NAME       = 2;
   static final public int WAVELEN    = 3;
   static final public int DATE       = 4;
   static final public int COVERAGE   = 5;
   static final public int RESOL      = 6;
   static final public int REGIME     = 7;
   static final public int SIZE       = 8;
   static final public int POPULAR    = 9;
   static final public int ORIGIN     = 10;
   static final public int ID         = 11;
   static final public int PROTOCOL   = 12;
   static final public int VIZCODE    = 13;
   static final public int JOURNAL    = 14;
   static final public int JNLVOL     = 15;
   static final public int PLANET     = 16;
   static final public int CDS        = 17;
   static final public int ROWS       = 18;
   static final public int COLOR      = 19;
   static final public int YEAR       = 20;
   static final public int VIZIER     = 21;
   static final public int VIZCAT     = 22;
   static final public int BRANCH1    = 23;
   static final public int BRANCH2    = 24;
   static final public int DATATYPE   = 25;
   
   private Aladin aladin;
   
   // Liste des règles de tri associées à des branches (branche -> listes des règles de tri)
   // ex: "Catalog/Vizier/J -> (BRANCH, JOURNAL, -JNLVOL), (BRANCH, JOURNAL, DATE), etc..
   private SortedMap<String, BranchRules > AllRules;
   
   // Liste des règles de tri global
   private SortRule [] globalRules;
   private int currentGlobal = 0;
   
   /** Règle de tri : liste des tris à appliquer successivement */
   class SortRule {
      int rule[];         // Liste des tris à appliquer séquentiellement
      String label;       // Titre de cette règle de tri
      String description; // Description de cette règle de tri
      
      SortRule( String label, String description, int [] rule ) {
         this.label=label; this.description=description; this.rule=rule;
      }
   }
   
   class BranchRules {
      // Liste des règles de tri compatibles avec la branche
      ArrayList<SortRule> rules = new ArrayList<DirectorySort.SortRule>();
      
      // Règle de tri courante
      int current=0;
      
      BranchRules( SortRule [] rules ) {
         for( SortRule r : rules ) this.rules.add(r);
      }
      
      // Retourne la liste des tris à appliquer pour la règle courante
      int [] getCurrentRule() { return rules.get(current).rule; }
      
      // Positionne la règle de tri courante
      void setCurrent( int index ) {
         assert index>=0 && index<rules.size();
         current=index;
         setCurrentGlobal( 0 );
      }
   }
   
   protected DirectorySort(Aladin aladin) {
      this.aladin = aladin;
      initRules();
      initGlobal();
   }
   
   /** Construit la combobox des règles de tris associées à une branche
    * en sélectionnant la courante */
   protected JPopupMenu createBranchPopup( String branch ) {
      final BranchRules br = getBranchRules(branch);
      assert branch!=null;
      JPopupMenu c = new JPopupMenu();
      int i=0;
      ButtonGroup bg = new ButtonGroup();
      for( SortRule r : br.rules ) {
         JRadioButtonMenuItem mi = new JRadioButtonMenuItem( r.label );
         Util.toolTip(mi, r.description );
         mi.setActionCommand( i+"" );
         mi.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               int index = Integer.parseInt( e.getActionCommand() );
               br.setCurrent(index);
               aladin.directory.resumeSort();
            }
         });
         bg.add(mi);
         if( i==br.current ) mi.setSelected(true);
         c.add(mi);
         i++;
      }
      return c;
   }
   
   /** Construit la combobox des règles de tri global */
   protected JPopupMenu createGlobalPopup( ) {
      JPopupMenu c = new JPopupMenu();
     int i=0;
      ButtonGroup bg = new ButtonGroup();
      for( SortRule r : globalRules ) {
         JRadioButtonMenuItem mi = new JRadioButtonMenuItem( r.label );
         Util.toolTip(mi, r.description );
         mi.setActionCommand( i+"" );
         mi.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               int index = Integer.parseInt( e.getActionCommand() );
               setCurrentGlobal(index);
               aladin.directory.resumeSort();
            }
         });
         bg.add(mi);
         if( i==currentGlobal ) mi.setSelected(true);
         c.add(mi);
         i++;
      }
      return c;
   }
   
   protected void setCurrentGlobal(int index ) { currentGlobal = index; }
   
   /** Positionne la règle de tri à appliquer pour une branche donnée */
   protected void setCurrentRule( String branch, int index ) {
      BranchRules br = getBranchRules(branch);
      assert branch!=null;
      br.setCurrent( index );
   }
   
   // Initialise les règles de tri global
   private void initGlobal() {
      globalRules = new SortRule[] {
            new SortRule("Default (branch by branch)","Default sort/hierarchy based on branch rules managed by CDS",
                  new int[] { } ),
            new SortRule("Origin","By ascending name, grouped by origin",
                  new int[] { ORIGIN, NAME} ),
            new SortRule("Regime","By ascending wave lenght, grouped by regime",
                  new int[] { REGIME, WAVELEN} ),
            new SortRule("Protocol","By ascending name, grouped by protocol",
                  new int[] { PROTOCOL, NAME} ),
            new SortRule("Data type","By ascending name, grouped by data type",
                  new int[] { DATATYPE, NAME} ),
            new SortRule("Year","By descending date, grouped by year",
                  new int[] { -YEAR, -DATE, NAME} ),
            new SortRule("Flat name","By ascending name",
                  new int[] { NAME} ),
            new SortRule("Flat date","By descending date",
                  new int[] { -DATE} ),
            new SortRule("Flat wave lenght","By ascending wavelen",
                  new int[] { WAVELEN} ),
//            new SortRule("ID/flat","By ascending identifier",
//                  new int[] { ID} ),
//            new SortRule("ID/origin","By ascending identifier, grouped by origin",
//                  new int[] { ORIGIN, ID} ),
      };
      currentGlobal = 0;
   }

   
   // Initialise les règles de tris par défaut
   private void initRules() {
      Comparator<String> comparator = new Comparator<String>() {
         public int compare(String s1, String s2) {
            return (s2.length()-s1.length())*10000+s1.compareTo(s2);
         }           
      };
      AllRules = new TreeMap<String, BranchRules>(comparator);
      initBranchRules("",  
            new SortRule[] {
                  new SortRule("Name","By name, grouped by origin, CDS first",
                        new int[] { BRANCH, CDS, ORIGIN, NAME} ),
                  new SortRule("Regime","By regime, grouped by origin",
                        new int[] { BRANCH, ORIGIN, REGIME, NAME} ),
                  new SortRule("Date","Descending date, grouped by origin",
                        new int[] { BRANCH, ORIGIN, -DATE} )
      } );
      initBranchRules("Data base",  
            new SortRule[] {
                  new SortRule("Name","By name sorted, CDS first",
                        new int[] { BRANCH, CDS, NAME} ),
                  new SortRule("Origin","By name, grouped by origin",
                        new int[] { BRANCH, ORIGIN, BRANCH1, NAME } )
      } );
      initBranchRules("Image",  
            new SortRule[] {
                  new SortRule("Regime","Ascending wavelength, grouped by regime", 
                        new int[] { BRANCH, REGIME, BRANCH2, WAVELEN  } ),
                  new SortRule("Origin","Ascending wavelength, grouped by origin", 
                        new int[] { BRANCH, ORIGIN, BRANCH2, WAVELEN  } ),
                  new SortRule("Coverage","Ascending wavelength, grouped by coverage range", 
                        new int[] { BRANCH, COVERAGE, BRANCH2, WAVELEN  } )
      } );
      initBranchRules("Cube",  
            new SortRule[] {
                  new SortRule("Regime","Ascending wavelength, grouped by regime", 
                        new int[] { BRANCH, REGIME, WAVELEN  } ),
                  new SortRule("Origin","Ascending wavelength, grouped by origin", 
                        new int[] { BRANCH, ORIGIN, WAVELEN  } ),
                  new SortRule("Coverage","Ascending wavelength, grouped by coverage range", 
                        new int[] { BRANCH, COVERAGE, WAVELEN  } )
      } );
      initBranchRules("Catalog",  
            new SortRule[] {
                  new SortRule("Regime","By name, grouped by origin and regime, CDS first",
                        new int[] { BRANCH, CDS, ORIGIN, REGIME, NAME} ),
                  new SortRule("Date","Descending date, grouped by origin",
                        new int[] { BRANCH, CDS, ORIGIN, -YEAR, -DATE} )
      } );
      initBranchRules("Catalog/VizieR",  
            new SortRule[] {
                  new SortRule("Default",
                        "based on size, popularity and date (all descending), grouped by VizieR category and catalogue name",
                        new int[] { BRANCH, VIZIER, VIZCODE, JOURNAL, VIZCAT, -JNLVOL, SIZE, POPULAR, -DATE } ),
                  new SortRule("Regime",
                        "based on size, popularity and date (all descending), grouped by regime and catalogue name",
                        new int[] { BRANCH, VIZIER, REGIME, VIZCAT,SIZE, POPULAR, -DATE } ),
                  new SortRule("Size","Descending table size",
                        new int[] { BRANCH, VIZIER, ROWS } ),
                  new SortRule("Popularity","Descending popularity",
                        new int[] { BRANCH, VIZIER, POPULAR } ),
                  new SortRule("Date","Descending date, grouped by year and catalogue name",
                        new int[] { BRANCH, VIZIER, -YEAR, -DATE, VIZCAT } ),
                  new SortRule("Coverage","Descending coverage, grouped by coverage range",
                        new int[] { BRANCH, VIZIER, COVERAGE, ID } ),
      } );
      initBranchRules("Outreach",  
            new SortRule[] {
                  new SortRule("Name","By acending name",
                        new int[] { BRANCH, NAME } ),
                  new SortRule("Origin","By name, grouped by origin",
                        new int[] { BRANCH, ORIGIN, BRANCH1, NAME } )
      } );
      initBranchRules("Others",  
            new SortRule[] {
                  new SortRule("Protocol","By name, grouped by protocol and origin",
                        new int[] { BRANCH, PROTOCOL, ORIGIN, NAME } ),
                  new SortRule("Data type","By name, grouped by data type and origin",
                        new int[] { BRANCH, DATATYPE, ORIGIN, NAME } ),
                  new SortRule("Regime","Sorted by regime, grouped by protocol and origin",
                        new int[] { BRANCH, REGIME, ORIGIN, PROTOCOL, NAME } ),
                  new SortRule("Origin","Descending date, grouped by protocol and origin",
                        new int[] { BRANCH, ORIGIN, PROTOCOL, NAME } ),
                  new SortRule("Date","Descending date, grouped by protocol and origin",
                        new int[] { BRANCH, -YEAR, -DATE, ORIGIN, PROTOCOL, NAME } )
      } );
      initBranchRules("Planet",  
            new SortRule[] {
                  new SortRule("Planet","by resolution, grouped by Planet name",
                        new int[] { BRANCH, PLANET, RESOL, NAME } ),
                  new SortRule("Date","Descending date, grouped by year",
                        new int[] { BRANCH, YEAR, DATE } )
      } );
   }
   
   /** Positionnement d'une règle de tris pour la branche désignée
    * ex: Image/... => SORT_WAVELEN */
   private void initBranchRules(String branch, SortRule [] rule) {
      AllRules.put(branch, new BranchRules( rule ) );
   }

   /** Retourne la liste des règles de tris pour la branche désignée */
   private BranchRules getBranchRules(String branch) {
      for( String cat : AllRules.keySet() ) {
         if( branch.startsWith(cat) ) return AllRules.get(cat);
      }
      return null;
   }
   
   /** Retourne le mode de tri pour la branche désignée
    * (prendra en compte la première branche de tri dont le nom est le début de la branche indiquée
    * Commence par les branches dont la chaine est la plus longue)
    * ex: Catalog/VizieR/J... => SORT_WAVELEN, SORT_NAME, ... */
   private int [] getSortRule(String branch) {
      BranchRules rules = getBranchRules( branch );
      if( rules==null ) return null;
      return rules.getCurrentRule();
   }
   
   /** Retourne true si le mode de tri actuel est global et non spécifique à chaque branche */
   protected boolean isGlobalSorted() { return currentGlobal>0; }
   
   /** Génération de la clé de tri => mémorisation dans les prop sous le mot clé "internal_sort_key"
    * @param id   Identificateur
    * @param prop Propriétés associées
    * @return false si aucune clé de tri n'a été générée
    */
   protected boolean setInternalSortKey(String id, MyProperties prop) {
      int [] mode;
      StringBuilder k1 = new StringBuilder( );
      String branch = prop.getFirst("client_category");
      
      // Règle global de tri ?
      if( currentGlobal>0 ) {
         mode = globalRules[ currentGlobal].rule;
         
      // ou règle par branche ?
      } else {
         if( branch==null ) branch="";
         mode = getSortRule(branch);
      }

      // Construction de la clé de tri
      if( mode!=null ) {
         for( int i=0; i<mode.length; i++ ) {
            int m = mode[i];
            boolean flagLast = i==mode.length-1;
            String k = getSortKey(id, prop, m, flagLast );
            if( k!=null ) k1.append("/"+k);
         }
      }
      if( k1.length()>0  ) prop.replaceValue("internal_sort_key", k1.toString() );
      else prop.remove("internal_sort_key");
      
      // Construction de la branche
      k1 = new StringBuilder();
      if( mode!=null ) {
         for( int m : mode ) {
            String k = getBranchKey(id, prop, branch, m);
            if( k!=null ) {
               if( k1.length()>0 ) k1.append('/');
               k1.append(k);
            }
         }
      }
      
      prop.remove("internal_category");
      if( k1.length()>0 ) {
         String suffix = branch.startsWith(k1.toString()) ? branch.substring(k1.length()) : "";
         String s = k1.toString()+suffix;
         if( isGlobalSorted() || !s.equals(branch) ) prop.put("internal_category", s );
      }
     
      return true;
   }
   
   static boolean first=true;
   
   // Retourne l'indice de la branche (ex: Image/... => 0), -1 si non trouvé
   private int getBranchIndex(String branch) {
      return Util.indexInArrayOf(Util.getSubpath(branch, 0), BRANCHES);
   }
   
//   private int getRegimeIndex(String branch) {
//      return Util.indexInArrayOf(Util.getSubpath(branch, 1), REGIMEHIPS);
//   }
   
   // Retourne l'indice du régime (ex: Image/Optical... => 3), -1 si non trouvé
   static public int getRegimeIndex(String s ) {
      
      if( s==null ) return -1;
      
      // Mise en minuscules, sans tiret et autres séparateurs éventuels
      StringBuilder s2 = new StringBuilder();
      for( char c : s.toCharArray() ) {
         if( Character.isAlphabetic(c) ) s2.append( Character.toLowerCase(c));
      }
     String s3 = s2.toString();
      
      // Recherche d'un alias
      for( int i=0; i<REGIMEALIAS.length; i++ ) {
         for( String s1 : REGIMEALIAS[i] ) {
            if( s3.equals(s1) ) return i;
         }
      }
      return -1;
   }
   
   // Retourne l'indice du datatype, -1 si non trouvé
   static public int getDataTypeIndex(MyProperties prop) {
      if( prop.getProperty("hips_service_url") != null ) {
         String s = prop.getFirst("dataproduct_type");
         if( s!=null ) {
            if( Util.indexOfIgnoreCase(s, "catalog")>=0 ) return TABLE;
            if( Util.indexOfIgnoreCase(s, "cube")>=0 ) return CUBE;
         }
         return IMAGE;
      }
      if( prop.getProperty("sia_service_url") != null 
            || prop.getProperty("sia2_service_url") != null ) return IMAGE;
      if( prop.getProperty("ssa_service_url") != null ) return SPECTRUM;
      if( prop.getProperty("cs_service_url") != null
            || prop.getProperty("access_url") != null
            || prop.getProperty("tap_service_url") != null ) return TABLE;
      return -1;
   }
   
   // Retourne l'indice du protocole, -1 si non trouvé
   static public int getProtocolIndex(MyProperties prop) {
      if( prop.getProperty("hips_service_url") != null ) return HIPS;
      if( prop.getProperty("sia_service_url") != null  ) return SIA;
      if( prop.getProperty("sia2_service_url") != null ) return SIA2;
      if( prop.getProperty("ssa_service_url") != null  ) return SSA;
      if( prop.getProperty("cs_service_url") != null
            || prop.getProperty("access_url") != null  ) return CS;
      if( prop.getProperty("tap_service_url") != null  ) return TAP;
      return -1;
   }

   // Retourne une clé alphabétique sur nbDigit complétée par des Z si trop courte
   private String keyAlpha( String s, boolean flagReverse,int nbDigit ) {
      int len = s==null ? 0 : s.length();
      StringBuilder key = new StringBuilder();
      for( int i=0; i<nbDigit; i++ ) {
         if( i>=len ) key.append('.');
         else {
            char c = Character.toUpperCase( s.charAt(i) );
            if( flagReverse ) {               
               if( Character.isDigit(c) ) c = (char)( '9' - c );
               else if( Character.isAlphabetic(c) ) c = (char)( 'Z' - c );
            }
            key.append( c );
         }
      }
      return key.toString();
   }
   
   // Décompte du nombre de slashes
   private int countSlash(String s) {
      int i=0;
      for( char c : s.toCharArray() ) {
         if( c=='/' ) i++;
      }
      return i;
   }
   
   // Retourne une clé de tri en fonction du mode demandé
   private String getSortKey(String id, MyProperties prop, int mode, boolean flagLast ) {
      String branch = prop.getFirst("client_category");
      String key = getSortKey1(id, prop, branch, mode);
      
      // Si c'est le dernier tri de la règle, ou va ajouter un élément à la clé de tri
      // pour qu'à égalité, les branches les plus profondes arrivent en premier
      // (évite l'insertion d'un folder au milieu d'une série ex: 2MASS6X)
      if( branch!=null && flagLast ) {
         int c = countSlash(branch);
         assert c<=9;
         key = key+(9-c);
      }
      return key;
   }
   
   // Retourne une clé de tri en fonction du mode demandé
   private String getSortKey1(String id, MyProperties prop, String branch, int mode ) {
      int c;
      String s;
      
      // Sens inverse ?
      boolean flagReverse=false;
      if( mode<0 ) {
         flagReverse=true;
         mode = -mode;
      }
      
      switch( mode ) {
         
         // Tri sur la catégorie principale (Image, Data base, Catalog ...)
         case BRANCH:
            c = getBranchIndex( branch );
            if( c == -1 ) c = 99;
            else if( flagReverse ) c=98-c;
            return String.format("%02d", c);
         
         // Selon les 4 premières lettres du titre de la collection
         case NAME:
            String name = prop.getFirst("obs_title");
            if( name==null ) name = prop.getFirst("obs_collection");
            return keyAlpha(name,flagReverse,4);
            
         // Selon la date (début d'observation - resp fin d'observation en reverse
         // , et à défaut date de publication de l'article de ref)
         case YEAR:
         case DATE:
            double mjd = 99999;
            try {
               String date;
               if( !flagReverse ) date = prop.getFirst("t_min");
               else {
                  date = prop.getFirst("t_max");
                  
                  // Observations encore en cours  => date courante
                  if( date==null && prop.getFirst("t_min")!=null ) {
                     date = ""+Astrodate.JDToMJD( Astrodate.UnixToJD( System.currentTimeMillis()/1000L ) );
                  }
               }
               if( date!=null ) {
                  mjd = Double.parseDouble(date);
                  if( flagReverse ) mjd = 99998-mjd;
               } else {
                  date = prop.getFirst("bib_year");
                  if( date!=null ) {
                     mjd = Astrodate.JDToMJD( Astrodate.YdToJD( Double.parseDouble(date)) ) ;
                     if( flagReverse ) mjd = 99998-mjd;
                  }
               }
            } catch( Exception e1 ) { }
            return String.format("%05d", (int)mjd);
          
         // Selon le pourcentage de couverture
         case COVERAGE:
            double cov=9;
            try {
               double w = Double.parseDouble( prop.getFirst("moc_sky_fraction") );
               cov =  Math.log( 1+w );
               if( !flagReverse ) cov = 8-cov;
            } catch( Exception e1 ) { }
            return String.format("%01.6f", cov);

            // Selon la résolution angulaire du pixel HiPS
         case RESOL:
            int order=99;
            try {
               order = Integer.parseInt( prop.getFirst("hips_order") );
               if( !flagReverse ) order = 98-order;
            } catch( Exception e1 ) { }
            return String.format("%02d", order);

            // Selon la taille approximative (par quantiles en log)
         case SIZE:
            int size=99;
            try {
               long r = Long.parseLong( prop.getFirst("nb_rows") );
               size = (int)( Math.log( r+1 ) );
               if( !flagReverse ) size =98-size;
            } catch( Exception e1 ) { }
            return String.format("%02d", size);

            // Selon le nombre de lignes pour un catalogue
         case ROWS:
            long rows=0L;
            try {
               rows = Long.parseLong( prop.getFirst("nb_rows") );
               if( !flagReverse ) rows =99999999999999L - rows;
            } catch( Exception e1 ) { }
            return String.format("%014d", rows);

            // tri sur le log de 1+(em_min+em_max)/2  sur 14 digits
         case WAVELEN:
            double wl=9;  // valeur max par défaut
            String sw = prop.getFirst("em_min");
            
            // En se basant sur em_* ?
            if( sw!=null ) {
               try {
                  double w = Double.parseDouble( sw );
                  String wavelen = prop.getFirst("em_max");
                  if( wavelen!=null ) w = (w+Double.parseDouble(wavelen))/2;
                  wl =  Math.log( 1+w );
                  if( flagReverse ) wl = 9-wl;
               } catch( Exception e1 ) { }

               // sinon en se basant sur obs_regime ?
            } else {
               wl = getRegimeIndex( prop.getFirst("obs_regime") );
               if( wl==-1 ) wl=9;
               else if( flagReverse ) wl = 8-wl;
            }
            return String.format("%01.14f", wl);

            // Tri sur le régime HiPS, ou obs_regime (dans l'ordre de REGIMEHIPS[])
         case REGIME:
            // Si c'est un HiPS image, on cherche le régime dans la branche
            boolean isHips = prop.getProperty("hips_service_url") != null;
            boolean isImage = (s=prop.getFirst("dataproduct_type"))!=null && 
                  (s.indexOf("image")>=0 || (s.indexOf("catalog")<0 && s.indexOf("cube")<0));
            c=-1;
            if( isHips && isImage ) c = getRegimeIndex( Util.getSubpath(branch, 1) );
            
            // Sinon dans le mot clé obs_regime
            if( c<0 ) c = getRegimeIndex( prop.getFirst("obs_regime") );
            if( c==-1 ) c=9;
            else if( flagReverse ) c = 8-c;
            return c+"";

            // Tri sur le régime HiPS, ou obs_regime (dans l'ordre de REGIMEHIPS[])
         case PLANET:
            String planet = Util.getSubpath( branch , 1);
            c = Util.indexInArrayOf(planet, PLANETS);
            if( c==-1 ) c=9;
            else if( flagReverse ) c = 8-c;
            return c+"";

         // Tri sur le protocole (SIA2,SIA,CS,TAP,SSA,HiPS)
         case PROTOCOL:
            c = getProtocolIndex(prop);
            if( c==-1 ) c=9;
            if( flagReverse ) c = 8-c;
            return c+"";
            
         // Selon l'ordre des codes catalogues VizieR (I, II, III, IV ...)
         case VIZCODE:
            String code = Directory.getCatCode( id );
            c = Util.indexInArrayOf(code, CATCODE);
            if( c==-1 ) c=99;
            else if( flagReverse ) c = 98-c;
            return String.format("%02d", c);
            
         // Selon la popularité de VizieR
         case POPULAR:
            String popularity = prop.get("vizier_popularity");
            long pop = 99;
            if( popularity != null ) {
               try {
                  pop = Long.parseLong(popularity);
                  pop = (int)( Math.log(1+pop) );
                  if( !flagReverse ) pop = 98-pop;
               } catch( Exception e ) {}
            }
            return String.format("%02d",pop);

            // Alphabétique suivant le nom du journal
         case JOURNAL:
            // Concerne bien un journal ?
            if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) break;
            code = Directory.getCatCode(id);
            if( code == null  || !code.equals("J") ) break;

            return keyAlpha( Directory.getJournalCode(id), flagReverse,6);
            
            // Alphabétique suivant le numéro de vol et de page de la table du journal
         case JNLVOL:
            // Concerne bien un journal ?
            if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) break;
            code = Directory.getCatCode(id);
            if( code == null  || !code.equals("J") ) break;

            String num = Directory.getJournalNum(id);
            int vol=9999;
            int page=9999;
            int offset = num.indexOf('/');
            try {
               vol = Integer.parseInt( num.substring(0,offset) );
               if( flagReverse ) vol= 9998 - vol;
            } catch( Exception e )  { }
            try {
              for( offset++; offset<num.length() && !Character.isDigit( num.charAt(offset) ); offset++);
              page = Integer.parseInt( num.substring(offset) );
//              if( flagReverse ) page= 9998 - page;   // On inverse pas les pages
           } catch( Exception e )  { }
            return String.format("%04d",vol)+"/"+String.format("%04d",page);
            
            // Alphabétique suivant l'origine
         case ORIGIN:
            return keyAlpha( Util.getSubpath(id,0), flagReverse, 6);
           
         case DATATYPE:
            c = getDataTypeIndex(prop);
            if( c==-1 ) c=9;
            return c+"";
            
            // Alphabétique suivant l'ID
         case ID:
            return keyAlpha( id, flagReverse, 12);
            
         // CDS en premier
         case CDS:
            return id.startsWith("CDS") ? "0" : "1";
 
         // HiPS color en premier
         case COLOR:
            String color = prop.getFirst("dataproduct_subtype");
            return color!=null && color.indexOf("color")>=0 ? "0" : "1";

      }
      
      return null;
   }
   
   // Backslash le résultat de getBranchKey1
   private String getBranchKey(String id, MyProperties prop, String branch, int mode ) {
      String s = getBranchKey1(id,prop,branch,mode);
      if( s!=null && mode!=BRANCH1 && mode!=BRANCH2 ) s=s.replace("/","\\/");
      return s;
   }
   
   // Retourne le morceau de la branche associé au mode de tri
   private String getBranchKey1(String id, MyProperties prop, String branch, int mode ) {
      int c;
      String code;
      boolean isHips;
      String s;

      if( mode<0 ) { mode = -mode; }

      switch( mode ) {

         case BRANCH:
            c = getBranchIndex( branch );
            return c==-1 ? Util.getSubpath(branch, 0) : BRANCHES[c];
            
         case REGIME:
            // Si c'est un HiPS image, on cherche le régime dans la branche
            isHips = prop.getProperty("hips_service_url") != null;
            boolean isImage = (s=prop.getFirst("dataproduct_type"))!=null && 
                  (s.indexOf("image")>=0 || (s.indexOf("catalog")<0 && s.indexOf("cube")<0));
            c=-1;
            if( isHips && isImage ) c = getRegimeIndex( Util.getSubpath(branch, 1) );
            
            // Sinon dans le mot clé obs_regime
            if( c<0 ) c = getRegimeIndex( prop.getFirst("obs_regime") );
            if( c>=0 ) return REGIMEHIPS[c];
            return "Unknown regime";

         case ORIGIN:
            return Util.getSubpath(id, 0);
            
         case COVERAGE:
            try {
               double w = Double.parseDouble( prop.getFirst("moc_sky_fraction") )*100;
               c = w==100 ? COV100 : w>=75 ? COV75 : w>=50 ? COV50 : w>=25 ? COV25 
                     : w>=10 ? COV10 : w>=1 ? COV1 : COV0;
               return COVRANGES[ c ]; 
            } catch( Exception e1 ) { }
            return "Unknown coverage";
            
         case VIZIER:
            // Pas Vizier ? -> Retourne l'ORIGIN
            if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) return Util.getSubpath(id, 0);
            
            return "VizieR";
            
         case PLANET:
            return Util.getSubpath(branch, 1);
            
         case DATATYPE:
            c = getDataTypeIndex(prop);
            return c>=0 ? DATATYPES[ c ] : "Unknown datatype";

         case PROTOCOL:
            c = getProtocolIndex(prop);
            return c>=0 ? PROTOCOLS[ c ] : "Unknown protocol";

         case JOURNAL:
            // Concerne bien un journal ?
            if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) break;
            code = Directory.getCatCode(id);
            if( code == null  || !code.equals("J") ) break;
            
            String journal = Directory.getJournalCode(id);
            return journal; 

         case VIZCODE:
            // Concerne bien VizieR ?
           if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) break;
            code = Directory.getCatCode(id);
            if( code == null ) break;
            c = Util.indexInArrayOf(code, DirectorySort.CATCODE);
            return c==-1 ? "Unknown cat" : DirectorySort.CATLIB[c];
            
         case VIZCAT:
            // Concerne bien VizieR ?
            if( !id.startsWith("CDS/") || id.equals("CDS/Simbad") ) break;
            code = Directory.getCatCode(id);
            if( code == null  ) break;

            String parent = Directory.getCatParent(id);
            boolean hasMultiple = aladin.directory.hasMultiple(parent);
            if( hasMultiple ) return prop.get("obs_collection");
            return null;
         
         case YEAR:
            String year = prop.getFirst("bib_year");
            if( year==null ) {
               try {
                  String date;
                  date = prop.getFirst("t_min");
                  double mjd = Double.parseDouble(date);
                  year = ""+(int)( Astrodate.JDToYd( Astrodate.MJDToJD( mjd )) );
               } catch( Exception e1 ) { }
            }
            return year==null ? "Unknown date" : year;
            
         case BRANCH1:
            return Util.getSubpath(branch, 1,-1);
            
         case BRANCH2:
            return Util.getSubpath(branch, 2,-1);
      }
      
      return null;
   }
   
}

