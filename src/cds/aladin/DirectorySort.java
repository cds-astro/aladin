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
   public static final String[] REGIMEHIPS = { 
         "Gamma-ray","X","UV","Optical","Infrared","Radio","Gas-lines" };
   public static final String[] CATCODE   = { 
         "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "B", "J" };
   public static final String[] CATLIB = { 
         "I-Astrometric Data", "II-Photometric Data", "III-Spectroscopic Data",
         "IV-Cross-Identifications", "V-Combined data", "VI-Miscellaneous", "VII-Non-stellar Objects",
         "VIII-Radio and Far-IR data", "IX-High-Energy data", "B-External databases, regularly updated",
         "Journal tables"};
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
   
   public static final String [] SORTNAME = {
      "default","category","name","wavelen","date","coverage","resol",
      "regime","size","popular","origin","id","protocol","vizcode","journal","jnlvol",
      "planet","cds","rows","color"
   };
   
   private Aladin aladin;
   
   // Liste des règles de tri associées à des branches (branche -> listes des règles de tri)
   // ex: "Catalog/Vizier/J -> (BRANCH, JOURNAL, -JNLVOL), (BRANCH, JOURNAL, DATE), etc..
   private SortedMap<String, BranchRules > AllRules;
   
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
      void setCurrent( int index ) { assert index>=0 && index<rules.size(); current=index; }
   }
   
   protected DirectorySort(Aladin aladin) {
      this.aladin = aladin;
      initRules();
   }
   
   /** Construit la combobox des règles de tris associées à une branche
    * en sélectionnant la courante */
   protected JPopupMenu createPopup( String branch ) {
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
   
   /** Positionne la règle de tri à appliquer pour une branche donnée */
   protected void setCurrentRule( String branch, int index ) {
      BranchRules br = getBranchRules(branch);
      assert branch!=null;
      br.setCurrent( index );
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
                  new SortRule("CDS default","Alpha sorted, grouped by origin, CDS first",
                        new int[] { BRANCH, CDS, ORIGIN, NAME} ),
                  new SortRule("Regime","Sorted by regime, grouped by origin",
                        new int[] { BRANCH, ORIGIN, REGIME, WAVELEN} ),
                  new SortRule("Date","Descending date, grouped by origin",
                        new int[] { BRANCH, ORIGIN, -DATE} ),
                  new SortRule("Date (reverse)","Ascending date, grouped by origin",
                        new int[] { BRANCH, ORIGIN, DATE} )
      } );
      initBranchRules("Image",  
            new SortRule[] {
                  new SortRule("Wave lenght","Ascending ave length, grouped by regime", 
                        new int[] { BRANCH, REGIME , WAVELEN  } ),
//                  new SortRule("Wave lenght (reverse)","Descending wave length, grouped by regime", 
//                        new int[] { BRANCH, -REGIME , -WAVELEN  } ),
                  new SortRule("Color","Ascending wave lenght, but colored first, grouped by regime", 
                        new int[] { BRANCH,  REGIME , COLOR, WAVELEN  } ),
                  new SortRule("Resolution","Resolution ascending, grouped by regime", 
                        new int[] { BRANCH, REGIME , RESOL , WAVELEN  } )
      } );
      initBranchRules("Catalog/VizieR",  
            new SortRule[] {
                  new SortRule("VizieR default",
                        "based on size, popularity and date (all descending), grouped by VizieR category",
                        new int[] { BRANCH, VIZCODE, SIZE, POPULAR, -DATE } ),
                  new SortRule("Size","Descending size, grouped by VizieR category",
                        new int[] { BRANCH, VIZCODE, SIZE } ),
                  new SortRule("Popularity","By popularity, grouped by VizieR category",
                        new int[] { BRANCH, VIZCODE, POPULAR } ),
                  new SortRule("Date","Descending date, grouped by VizieR category",
                        new int[] { BRANCH, VIZCODE, -DATE } ),
//                  new SortRule("Date (reverse)","Ascending date, grouped by VizieR category",
//                        new int[] { BRANCH, VIZCODE, DATE } ),
                  new SortRule("VizieR ID","Descending VizieR ID, grouped by VizieR category",
                        new int[] { BRANCH, VIZCODE, -ID } )
      } );
      initBranchRules("Catalog/VizieR/Journal",  
            new SortRule[] {
                  new SortRule("Journal default","Descending date, grouped by journal name",
                        new int[] { BRANCH, JOURNAL, -JNLVOL } ),
                  new SortRule("Date","Ascending date, grouped by journal name",
                        new int[] { BRANCH, JOURNAL, JNLVOL } ),
                  new SortRule("Size","Descending size, grouped by journal name",
                        new int[] { BRANCH, JOURNAL, SIZE } ),
                  new SortRule("Popularity","by popularity, grouped by journal name",
                        new int[] { BRANCH, JOURNAL, POPULAR } )
      } );
      initBranchRules("Others",  
            new SortRule[] {
                  new SortRule("Protocol default","Alpha sorted, grouped by protocol and origin",
                        new int[] { BRANCH, PROTOCOL, ORIGIN, NAME } ),
                  new SortRule("Regime","Sorted by regime, grouped by protocol and origin",
                        new int[] { BRANCH, PROTOCOL, ORIGIN, REGIME, WAVELEN } ),
                  new SortRule("Date","Descending date, grouped by protocol and origin",
                        new int[] { BRANCH, PROTOCOL, ORIGIN, -DATE } ),
                  new SortRule("Date (reverse)","Ascending date, grouped by protocol and origin",
                        new int[] { BRANCH, PROTOCOL, ORIGIN, DATE } )
      } );
      initBranchRules("Planet",  
            new SortRule[] {
                  new SortRule("Planet default","by resolution, grouped by Planet name",
                        new int[] { BRANCH, PLANET, RESOL, NAME } ),
                  new SortRule("Date","Descending date, grouped by Planet name",
                        new int[] { BRANCH, PLANET, RESOL, -DATE } ),
                  new SortRule("Date (reverse)","Asscending date, grouped by Planet name",
                        new int[] { BRANCH, PLANET, RESOL, DATE } )
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
   
   
   /** Génération de la clé de tri => mémorisation dans les prop sous le mot clé "internal_sort_key"
    * @param id   Identificateur
    * @param prop Propriétés associées
    * @return false si aucune clé de tri n'a été générée
    */
   protected boolean setInternalSortKey(String id, MyProperties prop) {

      StringBuilder k1 = new StringBuilder( );
      String branch = prop.getFirst("client_category");
      if( branch==null ) branch="";

      int [] mode = getSortRule(branch);
      if( mode!=null ) {
         for( int i=0; i<mode.length; i++ ) {
            int m = mode[i];
            boolean flagLast = i==mode.length-1;
            String k = getSortKey(id, prop, m, flagLast );
            if( k!=null ) k1.append("/"+k);
         }
      }
      if( k1.length()>0  ) {
         prop.replaceValue("internal_sort_key", k1.toString() );
         return true;
      }
      
      return false;
   }
   
   static boolean first=true;
   
   // Retourne l'indice de la branche (ex: Image/... => 0), -1 si non trouvé
   private int getBranchIndex(String branch) {
      return Util.indexInArrayOf(Util.getSubpath(branch, 0), BRANCHES);
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
      if( flagLast ) {
         int c = countSlash(branch);
         assert c<=9;
         key = key+(9-c);
      }
      return key;
   }
   
   // Retourne une clé de tri en fonction du mode demandé
   private String getSortKey1(String id, MyProperties prop, String branch, int mode ) {
      int c;
      
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
                  if( flagReverse ) cov = 8-cov;
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
            double rows=9;
            try {
               long r = Long.parseLong( prop.getFirst("nb_rows") );
               rows = Math.log( r+1 );
               if( !flagReverse ) rows =8-rows;
            } catch( Exception e1 ) { }
            return String.format("%01.6f", rows);

            // tri sur le log de 1+(em_min+em_max)/2  sur 14 digits
         case WAVELEN:
            double wl=9;  // valeur max par défaut
            try {
               double w = Double.parseDouble( prop.getFirst("em_min") );
               String wavelen = prop.getFirst("em_max");
               if( wavelen!=null ) w = (w+Double.parseDouble(wavelen))/2;
               wl =  Math.log( 1+w );
               if( flagReverse ) wl = 9-wl;
            } catch( Exception e1 ) { }
            return String.format("%01.14f", wl);

        // Tri sur le régime HiPS, ou obs_regime (dans l'ordre de REGIMEHIPS[])
         case REGIME:
            String regime = Util.getSubpath( branch , 1);
            if( regime==null ) regime = prop.getFirst("obs_regime");
            c = Util.indexInArrayOf(regime, REGIMEHIPS);
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
            c = 9;
                 if( prop.getProperty("sia2_service_url")!=null ) c=0;
            else if( prop.getProperty("sia_service_url")!=null )  c=1;
            else if( prop.getProperty("cs_service_url")!=null )   c=2;
            else if( prop.getProperty("tap_service_url")!=null )  c=3;
            else if( prop.getProperty("ssa_service_url")!=null )  c=4;
            else if( prop.getProperty("hips_service_url")!=null ) c=5;
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
            return keyAlpha( Directory.getJournalCode(id), flagReverse,6);
            
            // Alphabétique suivant le numéro de vol et de page de la table du journal
         case JNLVOL:
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
   
}

