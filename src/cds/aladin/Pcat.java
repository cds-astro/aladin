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

package cds.aladin;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import cds.astro.Unit;
import cds.tools.Astrodate;
import cds.tools.ConfigurationReader;
import cds.tools.ScientificUnitsUtil;
import cds.tools.Util;
import cds.xml.Field;
import cds.xml.TableParser;
import cds.xml.TableParserConsumer;

//import cds.savot.pull.SavotPullEngine;
//import cds.savot.pull.SavotPullParser;
//import cds.xml.VOTableConsumer;
//import cds.savot.model.SavotTR;
//import cds.savot.model.SavotTable;
//import cds.xml.VOTable;

/**
 * Gestionnaire des objets d'un plan catalogue ou tool.
 *
 * @author P. Fernique CDS
 * @version 1.5 : (14 mars 2003) Recherche d'un éventuel champ _OID, et
 *                 appelle a Source.setOID() dans ce cas
 * @version 1.5 : (25 juillet 2002) VOTable s'ajoute a Astrores
 * @version 1.4 : (21 mars 2002) 2 tentatives d'ouverture de l'URL
 * @version 1.3 : (8 mars 2001) Suppression limite objets pour donnees locales
 * @version 1.2 : (19 janvier 2001) Correction bug de l'objet unique en TSV
 * @version 1.1 : (23 nov 2000) Correction bug du clip lors d'un Draw()
 * @version 1.0 : (11 mai 99) Toilettage du code
 * @version 0.9 - 31 mars 1998
 */
//public final class PlanObjet implements SavotSAXLikeConsumer {
public final class Pcat implements TableParserConsumer/* , VOTableConsumer */ {

   static String OUTOFMEMORY,CATABORT;
   static final int DEFAULTBLOC = 200;
   static final int MAXBLOC = 100000;

   // Composantes de l'objet
   protected Obj [] o;                     // Tableau des objets
   protected int nb_o;                       // Nombre d'objets
   Color c;			   // Couleur des objets par defaut
   int nbTable=0;
   int iz=-1;			   // Numero d'ordre du dernier zoom
   //   int nRa,nDec;           // Indice des columnes RA et DEC si connues, sinon -1
   int nId=-1;		       // Indice de la colonne de l'identificateur
   int nIdVraisemblance=0; // 10-nom commence par ID, 20-nom contient "name" ou "designation", 30-ucd=ID_main 40-ucd=meta.id,meta.main
   boolean badRaDecDetection;       // true si la détection des colonnes RA et DEC est plus qu'incertaine
   boolean flagVOTable=false;       // True si on est sûr a priori que c'est du VOTable (évite le test)
   boolean flagSIAV2 = false;
   boolean flagSIA = false;
   boolean flagEPNTAP = false;
   boolean flagLabelFromData=false; // True si on laisse possible le renommage du plan par le contenu

   protected StringBuffer parsingInfo=null;    // Information éventuelle sur le parsing des données
   protected StringBuffer description=null;  // Information de description des tables concernées

   // Tableau indiquant dans quelle ViewSimple les objets peuvent être
   // projeté (mis à jour dans projection(v))
   private final boolean drawnInViewSimple[] = new boolean[ViewControl.MAXVIEW];

   // References
   Calque calque;
   Plan plan;
   Status status;
   Aladin aladin;

   static protected void createChaine(Chaine chaine) {
      OUTOFMEMORY = chaine.getString("POOUTOFMEMORY");
      CATABORT = chaine.getString("POCATABORT");
   }

   /** Creation de l'objet.
    * @param plan Plan d'appartenance des objets
    * @param c    Couleur par defaut
    * @param calque,status,aladin References
    */
   protected Pcat(Plan plan,Color c,Calque calque,
         Status status,Aladin aladin) {
      this.aladin = aladin;
      this.status = status;
      this.calque = calque;
      this.plan = plan;
      this.c=c;                        // Couleur du plan
      nb_o = 0;
      //      nRa=nDec=-1;
   }

   protected void free() {
      nb_o=0;
      o=null;
   }

   protected Pcat(Aladin aladin) {
      this.aladin = aladin;
      nb_o=0;
   }

   protected Pcat(PlanBG p) {
      plan = p;
      aladin = p.aladin;
      nb_o=0;
   }


   /** Retourne la référence au tableau d'objet (uniquement pour les méthodes déprecated de AladinData */
   protected Obj[] getObj() { return o; }

   protected void reallocObjetCache() {
      for( int i=0; i<nb_o; i++ ) {
         if( o[i] instanceof Position ) ((Position)o[i]).createCacheXYVP();
      }
   }

   /**
    * Retourne true si les objets sont projetables (projetés) dans
    * la simpleView d'indice n.
    */
   protected boolean isDrawnInSimpleView(int n) { return drawnInViewSimple[n]; }

   // Nécessaire pour les planBGCat qui possèdent autant de PlanObjet que de HealpixKeyCat
   protected Projection [] projpcat = new Projection[ViewControl.MAXVIEW];
   
   
   protected void  resetDrawnInView(ViewSimple v) { drawnInViewSimple[v.n]=false; }

   /** Projection de tous les objets en fonction du plan de reference courant.
    * La projection n'est effective que si necessaire
    */
   protected void projection(ViewSimple v) {
      long t1 = Util.getTime();

      drawnInViewSimple[v.n]=false;    // Par défaut, pas projetable

      if( v.isFree() ) return;

      Projection proj = v.getProj();
      if( !Projection.isOk( proj ) ) return;
      
      if( plan.proj[v.n]==proj /* && Projection.isOk(proj) */
            && (!(plan instanceof PlanBGCat)
               ||  plan instanceof PlanBGCat && !(plan instanceof PlanMoc) && projpcat[v.n]==proj )   // Dans le cas d'un planBGCat
            ) {
//         Aladin.trace(3,"NO Proj. ra/dec->XY (view "+v.n+") of \""+plan.label+"\" on \""
//                        +v+"\" => déjà fait !");
         drawnInViewSimple[v.n]=true;
         return;        // Deja fait
      }

      // Test sur le recouvrement des champs
      if( /* Projection.isOk(proj) && */plan.type!=Plan.TOOL && plan.type!=Plan.APERTURE
            && !(plan.isSimpleCatalog() && (plan.hasXYorig || v.isPlot()) )
            && !proj.agree(plan.projd,v) ) return;

      // Memorisation de la projection courante appliquee
      plan.proj[v.n]=proj;

      // Dans le cas d'une PlanBGCat
      projpcat[v.n]=proj;

      // Projection a appliquer sur chaque source
      for( int i=0; i<nb_o; i++ ) {
         try {
            o[i].projection(v);   // On applique la projection a chaque source
         } catch( Exception e ) {}
      }

      // Pour que les (x,y) soient recalcules dans la vue courante
      v.newView();
      //Aladin.trace(3,(proj==null?"Copie xy natif":"Proj. ra/dec")+"->XY (view "+v.n+") of \""+plan.label+"\" on \""
      //               +v+"/"+(plan.projd==null?"null":plan.projd.label)+"\"");
      drawnInViewSimple[v.n]=true;

      long t2 = Util.getTime();
      plan.statTimeComputing = t2-t1;
      plan.statNbComputing++;
   }

   /** Positionne les coordonnees RA/DE de tous les objets du plan (CATALOG)
    * en fonction des coordonnees x,y
    */
   protected void setCoord(Projection proj) {
      if( !plan.hasXYorig || plan.hasNoPos ) {
         System.err.println("Recalibration on a no-XYlocked planed !!! Aborted");
         return;
      }

      ViewSimple v = aladin.view.getCurrentView();
      if( v==null ) {
         System.out.println("Y a un probs !");
      }
      Aladin.trace(3,"Recalibration \""+proj.label+"\" XY->ra/dec on \""+plan.label+"\"");
      for( int i=0; i<nb_o; i++ ) {
         Position p = (Position)o[i];
         p.xv[v.n] = p.x;
         p.yv[v.n] = proj.r1-p.y;
         p.setCoord(v,proj);
      }
      v.newView(1);
   }

   String catalog;
   String table;
   double rajc,dejc,rm;
   public Legende leg=null;
   Vector vField= new Vector(10);
   boolean flagXY;			// true si la table est passee en XY
   boolean flagTarget=false;
   boolean flagEndResource;
   boolean flagFirstRecord=true;		// True si on n'a pas encore traite le premier enr.
   long timeStartTable;                 // Date du début du parsing de la table
   int lastNb_o;                        // juste pour connaitre le nombre d'objets dans chaque table
   double minRa,maxRa,minDec,maxDec;

   /** L'interface AstroRes */
   public void startResource(String name) {
      if( plan.label.equals("PLASTIC") && name!=null && name.length()>0 ) {
         plan.setLabel(name);
      }
      catalog=plan.label;
      table=plan.label;
   }

   /** Interface pour le positionnement d'un filtre dédié */
   public void setFilter(String filter) {
      plan.addFilter(filter);
   }

   /** L'interface AstroRes */
   public void setResourceInfo(String name,String value) {
      if( description==null ) description= new StringBuffer();
      if( name.equals("NAME") ) {
         catalog=value;
         if( plan.label==null || plan.label.length()==0 ) plan.setLabel(value);
         description.append("\nRESOURCE name: "+value+"\n");
      }
      else if(name.equals("TITLE"))                description.append("        title: "+value+"\n");
      else if(name.equals("DESCRIPTION"))          description.append(value+"\n");
   }

   /** L'interface AstroRes */
   public void endResource() {
      flagEndResource=true;

      if( plan instanceof PlanBGCat ) return;

      // Calcul du target s'il n'a pas ete mentionne
      if( !flagTarget && !flagXY && nb_o>0 ) computeTarget();

      // Appel au post traitement
      postJob(rajc,dejc,rm,true);
   }

   /** Post treatement du chargement d'objet (source) pour mettre a jour
    * le type de forme par defaut et la projection par defaut
    */
   protected void postJob(double rajc,double dejc,double rm,boolean setSourceType) {

      if( plan.type==Plan.X ) return;

      // Mise en place de la forme des sources en fontions de nombre de sources
      // Non utilisé si on vient d'un plugin Aladin
      if( setSourceType ) ((PlanCatalog)plan).setSourceType(Source.getDefaultType(nb_o));
      
      // Pas de projection associee
      if( flagXY ) {

         plan.hasXYorig=true;
         flagXY=false;   // on resete ce flag sinon ça posera souci en cas de positionnement ultérieur d'une projection via RA,DEC manuel

         //         aladin.info(aladin.chaine.getString("INFOXY"));
         //
         //         // Creation d'une astrometrie
         //         if( aladin.frameNewCalib==null ) {
         //            aladin.frameNewCalib = new FrameNewCalib(aladin,plan,null);
         //         } else aladin.frameNewCalib.majFrameNewCalib(plan);
         return;

      }


      // Positionnement de la projection et memorisation des infos de projection dans le plan
      int typeProj = Projection.getDefaultType(rm/60.);

      plan.setNewProjD(new Projection(null,Projection.SIMPLE,
            rajc,dejc,rm*2,
            250.0,250.0,500.0,
            0.0,false,
            typeProj,Calib.FK5));

      // Positionnement du centre que si ce n'est pas la valeur par défaut 0,0
      if( rajc!=0 && dejc!=0 ) plan.co=new Coord(rajc,dejc);
   }
   
   
   /** L'interface AstroRes */
   public void startTable(String name) {
      Aladin.trace(3,"startTable "+name);
      if( plan.label.equals("PLASTIC") && name!=null && name.length()>0 ) {
         plan.setLabel(name);
         catalog = plan.label;
         table = plan.label;
      }
      if( name!=null && name.length()>0 ) table=name;
      flagFirstRecord=true;
      flagXY=false;
      vField = new Vector(10);
      nId=-1;
      nIdVraisemblance=0;
      group=null;
      timeStartTable=Util.getTime();
      lastNb_o=nb_o;
   }

   private Vector<String> group=null;  // Liste des GROUP servant aux définitions des systèmes de coordonnées ou des Flux

   // Ajoute un GROUP à la liste des GROUPs à associer à la prochaine légende qui sera créée
   private void addGroup(String s) {
      if( s==null ) { group=null; return; } // reset forcé
      if( group==null ) group = new Vector<>();
      group.addElement(s);
   }

   /** L'interface AstroRes */
   public void setTableInfo(String name,String value) {
      if( description==null ) description= new StringBuffer();

      if( name.equals("NAME") ) { table=value; description.append("  TABLE name: "+value+"\n"); }
      else if( name.equals("TITLE"))            description.append("       title: "+value+"\n");
      else if( name.equals("DESCRIPTION"))      description.append(value+"\n");
      else if( name.equals("GROUP"))      addGroup(value);

      // La table aura des positions en XY uniquement
      else if( name.equals("__XYPOS") && value.equals("true") ) {
         flagXY=true;
         plan.error=Plan.NOREDUCTION;
      }
      
      // La table n'a a priori aucune position
      else if( name.equals("__NOCOO") && value.equals("true") ) {
         plan.hasNoPos=true;
         plan.error=Plan.NOPOSITION;
      }
   }

   /** L'interface AstroRes */
   public void endTable() { 
      long t = Util.getTime() - timeStartTable;
      int nbObj = nb_o-lastNb_o;
      tableParserInfo("   -Table loaded & parsed in "+Util.getTemps(t)
            + " for "+nbObj+" object"+(nbObj>1?"s":"")
            + (nbObj<1000?"":" ("+Util.myRound(""+1000.*nbObj/t)+" objects per sec)"));
   }

   /** L'interface AstroRes */
   public void setField(Field f) {
      Aladin.trace(3,"setField "+f);

      // tentatives de reperage de la position de l'identificateur
      if( nIdVraisemblance==0 ) {
         int pos=vField.size();
         if( f.ucd!=null && f.ucd.equalsIgnoreCase("meta.id;meta.main")
               && nIdVraisemblance<40 ) { nIdVraisemblance=40; nId=pos; }
         else if( f.ucd!=null && (f.ucd.equals("ID_MAIN") || f.ucd.startsWith("meta.id"))
               && nIdVraisemblance<30 ) { nIdVraisemblance=30; nId=pos; }
         else if( f.name!=null && (f.name.equalsIgnoreCase("name") || f.name.equalsIgnoreCase("designation"))
               && nIdVraisemblance<20 ) { nIdVraisemblance=20; nId=pos; }
         else if( f.name!=null && f.name.length()>1
               && (f.name.charAt(0)=='I' ||  f.name.charAt(0)=='i') && (f.name.charAt(1)=='D' ||  f.name.charAt(1)=='d')
               && nIdVraisemblance<20 ) { nIdVraisemblance=10; nId=pos; }
      }
      vField.addElement(f);
   }

   /** Retourne le numero d'ordre du Field repere par son name (ou son ID) */
   private int getFieldIndex(String name) {
      Enumeration e = vField.elements();
      for( int i=0; e.hasMoreElements(); i++ ) {
         Field f = (Field)e.nextElement();
         if( f.name!=null && f.name.equals(name) ) return i;
      }
      for( int i=0; e.hasMoreElements(); i++ ) {
         Field f = (Field)e.nextElement();
         if( f.ID!=null && f.ID.equals(name) ) return i;
      }
      return -1;
   }

   /** Substitution des variable ${XXX} ou $XXX par leur valeur
    * @param s La chaine a filtrer
    * @param value[] Les valeurs correspondantes a chaque variable
    * @param methode 0: sans encodage http
    *                1: avec encodage http (test automatiquement si on est avant le ? ou après)
    */
   private String dollarSub(String s,String [] value,int methode) {
      StringBuffer res = new StringBuffer();
      int mode =0;
      char [] a = s.toCharArray();
      int prev=0;
      int startVar=0;
      boolean acco=false;

      //System.out.println("*** resolution de "+s);


      if( methode==1 ) methode=2;

      for( int i=0; i<=a.length; i++) {
         char c=(i==a.length)?0:a[i];
         switch(mode) {

            // Je cherche un $
            case 0:
               if( methode==2 && c=='?' ) methode=1;
               if( c=='$' ) { mode=1; res.append(a,prev,i-prev); prev=i;}
               break;

               // Je test la premiere lettre d'un nom de variable (cas {} )
               // et memorise son emplacement
            case 1:
               startVar=i;
               if( c=='{' ) { mode=10; acco=true; }
               else { mode=2; acco=false; }
               break;

               // Je cherche le prochain '}'
            case 10: if( c=='}' ) mode=3;
            break;

            // Je cherche le prochain caractere particulier
            case 2:
               if( !(c>='a' && c<='z' || c>='A' && c<='Z'
               || c>='0' && c<='9' || c=='_' ) ) mode=3;
               break;

               // Je tente de remplacer le nom de variable par sa valeur
            case 3:
               boolean httpEncode=false;
               if( acco ) startVar++;
               int length=i-startVar-1;

               httpEncode=(methode==1);	// L'encodage HTTP est demande par parametre

               // Cas particulier du signe -, + ou * en debut de nom de variable
               if( a[startVar]=='-' ) { httpEncode=false; startVar++; length--; }
               if( a[startVar]=='+' ) { httpEncode=true; startVar++; length--; }
               if( a[startVar]=='*' ) { startVar++; length--; }

               String var = new String(a,startVar,length);
               int n=getFieldIndex(var);
               //System.out.println("*** var=["+var+"] n="+n+" value="+(n>=0?value[n]:"null")+" httpEncode="+httpEncode);
               if( n<0 ) { mode=0; break; }
               // ici ca plante
               res.append(httpEncode?URLEncoder.encode(value[n]):value[n]);
               //
               prev=acco?i:i-1;
               mode=0;
               i--;
               break;
         }
      }
      res.append(a,prev,a.length-prev);
      return res.toString();
   }



   boolean [] hiddenField;	         // Tableau des champs caches (1 pour cache 0 pour ok);
   boolean firstTrace=true;	         // Pour afficher la premiere source en cas de trace
   private int indexAccessUrl=-1;    // Position de la colonne pour in access_url éventuel
   private int indexAccessFormat=-1; // Position de la colonne pour un access_format éventuel
   private int indexDataProductType=-1; // Position de la colonne pour un dataproduct_type éventuel
   private int indexSTC=-1;          // Position de la colonne pour un FOV STC éventuel
   private int indexOID=-1;          // Position de la colonne OID eventuelle
   private TableParser res;          // Parser utilisé pour créer les objets
   private StringBuilder line = new StringBuilder(500);
   private Map<Integer, Field> standardisedColumns = new HashMap<>();


//   // Légende générique qui vient remplacer toutes les légendes propres
//   private Legende genericLeg=null;
//
//   /** Positionnement d'une légende générique pour tous les objets (concerne PlanBGCat) */
//   protected void setGenericLegende(Legende leg) {
//      genericLeg=leg;
//   }

   /** L'interface TableParserConsumer */
   public void setRecord(double ra, double dec, double jdTime, String[] value) {
      int n;
      String oid = null; // OID trouve s'il y a lieu

      try {
         // Construction de la legende associe a ces sources
         // uniquement fait a la premiere source (test sur flagFirstRecord)

         if( flagFirstRecord ) {
            leg=null;   // Reinitialisation si plusieurs tables dans une même resource
            firstTrace = true; // Pour afficher la premiere source en cas de trace
            indexAccessFormat=indexAccessUrl=indexDataProductType=-1;
            n = vField.size();
            Vector v = new Vector(n); // Ne contiendra que les champs conserves

            // Memorisation des champs caches
            // et regeneration de la liste des champs conserves
            hiddenField = new boolean[n];
            Enumeration e = vField.elements();
            indexOID = -1;
            int underRA, underDE, RA, DE;
            underRA = underDE = RA = DE = -1;
            Field fRA = null, fDE = null;
            String siaStandardColumns = null;
            String siaHideColumns = null;
            if (flagSIAV2) {
                //column to standardize in SIAV2 results table;
            	siaStandardColumns = ConfigurationReader.getInstance().getPropertyValue("SIAV2StandardColumns"); 
            	//column to hide in SIAV2 results table;
            	siaHideColumns = ConfigurationReader.getInstance().getPropertyValue("SIAV2HideColumns"); 
			}
            
            for( int i = 0; e.hasMoreElements(); i++ ) {
               Field f = (Field) e.nextElement();
               if( f == null ) continue;

               // Memorisation de l'index du champ OID s'il existe
               if( indexOID == -1 && f.name != null && f.name.equals("_OID") ) indexOID = i;

               // Pour répérer où se trouve les champs _RAJ2000 et _DEJ2000
               if( f.name != null ) {
                  if( f.name.equals("_RAJ2000") ) {
                     underRA = i;
                     fRA = f;
                  } else if( f.name.equals("_DEJ2000") ) {
                     underDE = i;
                     fDE = f;
                  }
                  else if( f.name.equals("RAJ2000") )   RA = i;
                  else if( f.name.equals("DEJ2000") )   DE = i;
                  else if( f.name.equals("RA(ICRS)") )  RA = i;
                  else if( f.name.equals("DE(ICRS)") )  DE = i;
                  else if( f.name.equals("s_region") )  indexSTC=i;
               }

               if( f.type != null
                     && (f.type.indexOf("hidden") >= 0 || f.type.indexOf("trigger") >= 0) ) {
                  hiddenField[i] = true;
                  f.visible=false;
               }

               v.addElement(f);
               if(flagSIAV2 && f.name!=null ){
            	   if (siaHideColumns!=null && siaHideColumns.indexOf(f.name)>-1) {
            		   hiddenField[i] = true;
                       f.visible=false;
            	   }
            	   
            	   if (siaStandardColumns != null && siaStandardColumns.indexOf(f.name)>-1) {
            		   Field displayField = new Field(f);
            		   
            		   // COde CHaitra => bien trop lent
                       displayField.name = Aladin.getChaine().getString(f.name);
//                       displayField.name = f.name;
                       
            		   //below logic to check if standardized columns are to be hidden
            		   if (siaHideColumns != null && siaHideColumns.indexOf(displayField.name)>-1) {
            			   displayField.visible = false;
                	   } else {
                		   displayField.visible = true;
                	   }
                	   v.addElement(displayField);
                	   this.standardisedColumns.put(i, displayField);
            	   }
			   }
            }

            // Si champs RAJ2000 et DEJ2000 on cache _RAJ2000 et _DEJ2000
            if( RA != -1 && DE != -1 && underRA != -1 && underDE != -1 ) {
               hiddenField[underRA] = hiddenField[underDE] = true;
               //               v.removeElement(fRA);
               //               v.removeElement(fDE);
               fRA.visible = fDE.visible = false;
            }

            // En cas de plan HiPS catalog, la légende est globale et externe
            if( plan instanceof PlanBGCat ) leg=((PlanBGCat)plan).getFirstLegende();
            if( leg==null ) {
               leg = new Legende(v);
               leg.name=table;
            }

            // Ajout de GROUPs éventuels
            if( group!=null ) leg.setGroup(group);
            
            nbTable++;
            flagFirstRecord = false;
         }

         // Dans le cas de la génération a posterio de la légende pour une table vide
         if( value==null ) return;

         // Limite de chargement ?
         // On agrandi le tableau avec un petit gag sur l'indice de nb_o
         if( nb_o == o.length ) {
            nextIndex();
            nb_o--;
         }

         // Generation de la ligne d'info
          line = new StringBuilder(1000);
//         Util.resetString(line);
         //         line.append(table);
         if( catalog!=null && catalog.equals("Simbad") ) line.append("<&_SIMBAD |Simbad>");
         else if( catalog!=null && (catalog.equals("NED") || catalog.equals("Ned")) ) line.append("<&_NED |NED>");
         else line = line.append("<&_getReadMe " + table + " |" + table + ">");

         // Construction de la ligne des mesures
         n = value.length;
         int j = -1; // Veritable index de la mesure (en fonction des champs
         // caches)
         for( int i = 0; i < n; i++ ) {
            if( value[i]==null ) value[i]="";	// En cas de VOTable <TD/>

            // Memorisation d'un eventuel OID
            if( indexOID >= 0 && i == indexOID ) oid = value[i];

            //            if( hiddenField != null && i < hiddenField.length && hiddenField[i] ) continue;
            j++;

            // pas d'info sur la mesure ou mesure vide ou nulle
            String a=value[i].trim();
            if( leg == null || !leg.hasInfo(j) || a.length() == 0
                  || a.equals("0") || a.equals("-") /* || a.equalsIgnoreCase("null") */ 
                  ) {
            	String displayString = "\t" + ((a.length() == 0) ? " " : value[i]);
            	line.append(displayString);
            	if (flagSIAV2 && standardisedColumns.containsKey(i)) {
            		line.append(displayString);
           		}
            	continue;
            }

            // Construction des ancres
            String href = leg.getHref(j);
            String gref = leg.getGref(j);
//            String refText = leg.getRefText(j);
            String flagArchive = leg.getRefValue(j);
            String utype = leg.getUtype(j);
            String name = leg.getName(j);
            
//            try {
//               if( indexSTC==-1 && /* flagEPNTAP && */ leg.getID(j).equals("s_region") ) {
//                  indexSTC=j;
//               }
//            } catch( Exception e ) { }
            
            if( indexSTC==-1 && Util.indexOfIgnoreCase( value[i], "Polygon ")==0 ) {
               indexSTC=j;
            }
            
            // On met un lien sur les urls ?
            if( href==null && (value[i].startsWith("http://") || value[i].startsWith("https://") || value[i].startsWith("ftp://"))) {
               href=value[i];
               
               // SIA 1.0
               if( flagArchive==null ) {
                  String ucd = leg.getUCD(j);
                  if( ucd!=null && Util.indexOfIgnoreCase(ucd,"Image_AccessReference")>=0 ) {
                     flagArchive="image/fits";
                     indexAccessUrl=-2;  // Pour éviter de le traiter par la suite
                  }
               }
               
               // SSA - spectrum
               if( flagArchive==null ) {
                  if( utype!=null && Util.indexOfIgnoreCase(utype,"ssa:Access.Reference")>=0 ) {
                     flagArchive="spectrum/???";
                     indexAccessUrl=-2;  // Pour éviter de le traiter par la suite
                  }
               }
               // SSA + preview
               if( flagArchive==null ) {
                  String ucd = leg.getUCD(j);
                  if( ucd!=null && Util.indexOfIgnoreCase(ucd,"meta.ref.url;datalink.preview")>=0 ) {
                     flagArchive="image/???";
                  }
               }
               
               // datalink uniquement basé sur le nom de la colonne (genre Markus)
               if( flagArchive==null && name!=null ) {
                  if( name.equalsIgnoreCase("datalink") ) flagArchive="data/???";
               }
            }
            
            String tag = (gref != null) ? gref : (href != null) ? "Http " + href : null;

            // JE LE REMETS ACTIF DE MANIERE GENERIQUE POUR N'IMPORTE QUEL SPECTRE - PF sept 2012
            if( tag!=null && flagArchive!=null && (flagArchive.startsWith("spectr") && flagArchive.indexOf('/')>0) ) tag="£"+tag;
            else if( tag != null && flagArchive != null && flagArchive.indexOf('/')>0  ) tag = "^" + tag;
            
            // Juste pour se rappeler que ce champ va porter un bouton vers une archive
            if( tag!=null && (tag.charAt(0)=='^' || tag.charAt(0)=='£') ) leg.field[j].flagArchive=true;
            
             // utype "a la obscore"
            if( indexAccessUrl==-1 && Util.indexOfIgnoreCase(utype,"Access.Reference")>=0 ) indexAccessUrl=j;
            if( indexAccessFormat==-1 && Util.indexOfIgnoreCase(utype,"Access.Format")>=0 ) indexAccessFormat=j;
            if( indexDataProductType==-1 && Util.indexOfIgnoreCase(utype,"Obs.dataProductType")>=0 ) indexDataProductType=j;

            // DESORMAIS LE TEXTE FORCE EST MIS A LA VISUALISATION DES MESURES (A FAIRE)
//            String text = (refText != null) ? refText : value[i];
            String text = value[i];
            
            // Les TABs ne peuvent être présents dans les valeurs individuelles (au risque de ne plus pouvoir relire les données
            // correctement). Je les remplace par un espace
            if( text.indexOf('\t')>=0 ) text=text.replace("\t"," ");
            
            line.append('\t');
            if( tag != null ) {
               line.append("<&" + dollarSub(tag, value, (href != null) ? 1 : 0));
               if( text != null ) line.append("|" + dollarSub(text, value, 0));
               line.append('>');
            } else {
            	line.append(text);
            	if (standardisedColumns.containsKey(i)) {
            		line.append('\t');
            		line.append(this.processValuesToStandardRepresentation(standardisedColumns.get(i), text));
    			}
            }
         }

         // Calcul en vue de definir la target
         if( flagXY || !flagTarget ) {
            if( ra < minRa ) minRa = ra;
            if( ra > maxRa ) maxRa = ra;
            if( dec < minDec ) minDec = dec;
            if( dec > maxDec ) maxDec = dec;
         }

         // Determination de label de la source
         String lab = (nId >= 0) ? value[nId] : "Source #" + (nb_o+1);

         if( firstTrace ) {

            // Pour le debogage (ATTENTION, n'indique que la premiere source)
            Aladin.trace(3, "setRecord "
                  + (oid != null ? "(oid=" + oid + ")" : "") + " \"" + lab
                  + "\" " + (flagXY ? "XY" : "pos") + "=(" + ra + "," + dec
                  + ")"+(!Double.isNaN(jdTime)?" time="+Astrodate.JDToDate(jdTime):"")+" [" + line + "]");
            firstTrace = false;

            // Dans le cas d'un résultat ObsTAP, on devra post-traiter le tag sur le champ "access_url" en fonction
            // de la valeur MIME du champ "access_format" (alternativement content-type)
            if( indexAccessUrl==-1 )    indexAccessUrl    = leg.find("access_url");
            if( indexAccessFormat==-1 ) indexAccessFormat = leg.find("access_format");
            if( indexAccessFormat==-1 ) indexAccessFormat = leg.find("content_type");
            if( indexDataProductType==-1 ) indexDataProductType = leg.find("dataproduct_type");
         }

         // Creation de la source, soit en XY, soit en alph,delta
         Source source;
         if( flagXY ) source = new Source(plan, ra, dec, 0, 0, Obj.XY, lab, line.toString(), leg);
         else source = new Source(plan, ra, dec, lab, line.toString(), leg);
         source.jdtime = jdTime;
         if( oid != null ) source.setOID(oid);
         o[nb_o++] = source;

         // Fov STCS attaché ?
         int idxSTCS = source.findUtype(TreeBuilder.UTYPE_STCS_REGION1);
         if( idxSTCS<0 ) idxSTCS = source.findUtype(TreeBuilder.UTYPE_STCS_REGION2);
         if( idxSTCS<0 ) idxSTCS = indexSTC;
         if (idxSTCS>=0) {
            try {
               String val = source.getValue(idxSTCS);
               
               // Attention des petits rigolos (Markus) utilisent parfois des tableaux de réels où ils alternent lon/lat
               if( leg.isNumField(idxSTCS) && leg.field[idxSTCS].arraysize!=null ) {
                  val="Polygon UNKNOWNFrame UNKNOWNREFPOS "+val;
//                  source.setValue(idxSTCS, val);
               }
               source.setFootprint(val);
               source.setIdxFootprint(idxSTCS);
            } catch(Exception e) {
               e.printStackTrace();
            }
            
         // Peut être un FoV peut être tout de même généré par SIA1 ou SSA ?
         } else 
         
         if( flagSIA && !flagSIAV2 ) {
            String fov = source.createSIAFoV();
            if( fov!=null ) {
               source.setFootprint(fov);
               int iScale = leg.findUCD("VOX:Image_Scale");
               source.setIdxFootprint(iScale);  // on l'associe manu militari sur le colonne des tailles des pixels
            }
         }

         // Post-traitement ObsTap => on remplace le <&xxx par <^xxx ou <£xxx e la colonne "access_url"
         // en fonction du type de donnée de la colonne dataproduct_type et sinon du MIME type de la colonne "access_format"
         if( indexAccessUrl>=0 || indexDataProductType>=0 ) {
            try {
               String type = indexDataProductType==-1 ? "" : source.getCodedValue(indexDataProductType);
               String fmt = indexAccessFormat==-1 ? "" : source.getCodedValue(indexAccessFormat);
               String val = source.getCodedValue(indexAccessUrl);
               if( val.startsWith("<&") && fmt.indexOf("html")<0 && fmt.indexOf("plain")<0 ) {
                  String tag="^";
                  if( type.length()>=0 && type.startsWith("spectr")) tag="£";
                  else if( fmt.startsWith("spectr") && fmt.indexOf('/')>0 ) tag="£";
                  val = "<&"+tag+val.substring(2);
                  source.setValue(indexAccessUrl, val);
                  source.getLeg().field[indexAccessUrl].flagArchive=true;
               }
            } catch( Exception e ) {
               e.printStackTrace();
            }
         }

      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) System.err.println("Pcat setRecord (3p) exception " + e);
         e.printStackTrace();
      }
   }
   
   /**
    * Method to process data to standardised representation.
    * Currently time(in MJD) and spectral data(in m) are supported.
    * This method is used because in case of siav2 we know date is in MJD 
    * as opposed to other results where date could be in JD 
    * 
    * @param field
    * @param text
    * @return formated string
    */
   
   // J'INVALIDE CES CONVERSIONS QUI RALENTISSENT BIEN TROP LE CHARGEMENT - PF 7/5/2019
	public String processValuesToStandardRepresentation(Field field, String text) {
		if( !flagSIAV2) {//for now only siav2 are known to get time in MJD and spectral data in m.
			return text;
		}
		
		String standardRepresentation = text;
		try {
			if (text != null && !text.trim().isEmpty() && field != null && field.ucd != null) {
			   
			      int i;
                  String s = ((i=field.ucd.indexOf('.'))<0 ? field.ucd : field.ucd.substring(0,i)).toLowerCase();
			      
	              if( s.equals("time") && "D".equalsIgnoreCase(field.datatype) && "d".equalsIgnoreCase(field.unit)) {
	                 if( isNumber(text) ) standardRepresentation = this.convertMJDToISO(text);
	              } else if( s.equals("em") && "D".equalsIgnoreCase(field.datatype) && "m".equalsIgnoreCase(field.unit)) {
                     standardRepresentation = this.setStandardSpectralRepresentation(text);
                }

// Code Chaitra => bien trop lent [le split est à fuir]
//				if (field.ucd.split("\\.")[0].equalsIgnoreCase("time") && "D".equalsIgnoreCase(field.datatype)
//						&& "d".equalsIgnoreCase(field.unit)) {
//					Pattern regex = Pattern.compile(REGEX_NUMBER);
//					Matcher matcher = regex.matcher(text);
//					if (matcher.find()) {
//						standardRepresentation = this.convertMJDToISO(text);
//					}
//				} else if (field.ucd.split("\\.")[0].equalsIgnoreCase("em") && "D".equalsIgnoreCase(field.datatype)
//						&& "m".equalsIgnoreCase(field.unit)) {
//					standardRepresentation = this.setStandardSpectralRepresentation(text);
//				}
			}
		} catch (Exception e) {
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			standardRepresentation = text;
		}
		return standardRepresentation;
	}
	
	private boolean isNumber( String s) {
	   try { Double.parseDouble(s); return true; } catch( Exception e ) { }
	   return false;
	   
//       Pattern regex = Pattern.compile(REGEX_NUMBER);
//       Matcher matcher = regex.matcher(s);
//       return matcher.find();
	}

   public String convertMJDToISO(String timeWord) {
		double valueInProcess= Astrodate.MJDToJD(Double.valueOf(timeWord)); 
		return Astrodate.JDToDate(valueInProcess);
	}
	
   /**
    * Method to process spectral data
    * The input wavelength (in m): will be converted to either 
    * 		- eV(higher frequencies: Gamma, X-ray)
    * 		- m (mid: EUV, UV, optical, IR)
    * 		- Hz(lower frequencies: micro, radio)
    * The boundaries for the above categorization is defined in configuration file as
    * 		- WAVELENGTHRANGE1(Between X-ray and UV) 
    * 		- WAVELENGTHRANGE2(between IR and Radio)
    * @param text
    * @return formatted spectral param
    */
	public String setStandardSpectralRepresentation(String text) {
		String defaultWavelengthUnit = "m";
		Double spectralVal = Double.parseDouble(text);
		Unit unitToProcess = null;
		Double wavelengthRange1 = Double.parseDouble(ConfigurationReader.getInstance().getPropertyValue("WAVELENGTHRANGE1"));
		Double wavelengthRange2 = Double
				.parseDouble(ConfigurationReader.getInstance().getPropertyValue("WAVELENGTHRANGE2"));
		try {
			if (spectralVal != null && spectralVal != 0.0d) {
				if (spectralVal < wavelengthRange1) {
					// convert to eV
					unitToProcess = ScientificUnitsUtil.convertMeter2eV(spectralVal);

				} else if (spectralVal >= wavelengthRange1 && spectralVal < wavelengthRange2) {
					// Do no conversions
					unitToProcess = new Unit(defaultWavelengthUnit);
					unitToProcess.value = spectralVal;
				} else if (spectralVal >= wavelengthRange2) {
					// Convert to hertz
					unitToProcess = ScientificUnitsUtil.convertMeter2Frequency(spectralVal);
				}

				if (unitToProcess != null) {
					text = ScientificUnitsUtil.prefixProcessing(unitToProcess);
				}
			} else {
				text = "0.0";
			}
		} catch (ParseException e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		return text;
	}

   /** Retourne true si on a des infos sur le catalogue */
   protected boolean hasCatalogInfo() { return parsingInfo!=null || description!=null; }

   /** retourne true si au-moins un objet est sélectionné */
   protected boolean hasSelectedOrTaggedObj() {
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( o.isSelected() ) return true;
         if( o instanceof Source && ((Source)o).isTagged() ) return true;
      }
      return false;
   }
   
   /** Retourne true si on dispose d'informations techniques sur le parsing */
   protected boolean hasParsingInfo() { return parsingInfo!=null || description!=null; }

   /** Affichage de la fenêtre contenant les informations sur le parsing
       et les données */
   protected void seeCatalogInfo() {
      if( !hasParsingInfo() ) return;
      JFrame f = new JFrame("Catalog information");
      Util.setCloseShortcut(f, false, aladin);
      //      f.setBackground(Aladin.BKGD);
      f.setIconImage(aladin.getImagette("AladinIconSS.gif"));
      JTextArea t = new JTextArea(25,80);
      t.setFont( Aladin.COURIER );
      if( description!=null ) t.setText(description.toString()+"\n\n");
      if( parsingInfo!=null ) t.append("Parsing information:\n\n"+parsingInfo.toString());
      JScrollPane sp = new JScrollPane(t);
      Aladin.makeAdd(f,sp,"Center");
      f.pack();
      f.show();
   }

   /** This method is called by the TableParserConsumer for
    * delivering not crucial error
    */
   public void tableParserWarning(String msg) {
      if( msg.startsWith("!!!") ) {
         if( msg.indexOf("OVERFLOW")>=0 ) plan.error=msg;
         if( msg.indexOf("ERROR")>=0 ) plan.error=msg;
      }
      tableParserInfo(msg);
   }
   
   /** Positionnement de l'époque originale */
   public void setOriginalEpoch(String s ) throws Exception { plan.setOriginalEpoch(s); }

   /** This method is called by the TableParserConsumer for
    * delivering parsing information
    */
   public void tableParserInfo(String msg) {
      if( parsingInfo==null ) parsingInfo = new StringBuffer();
      parsingInfo.append(msg+"\n");
      Aladin.trace(3,msg);
   }

   /** This method is called by the TableParserConsumer for
    * delivering RA,DEC,X,Y column index (-1 means not found)
    */
   public void setTableRaDecXYIndex(int nRa, int nDec, int nPmRa, int nPmDec,int nX, int nY, boolean badDetection) {
      int n=vField.size();
      if( nRa>=0  && nRa<n  ) ((Field)vField.elementAt(nRa)).coo=Field.RA;
      if( nDec>=0 && nDec<n ) ((Field)vField.elementAt(nDec)).coo=Field.DE;
      if( nPmRa>=0 && nPmRa<n ) ((Field)vField.elementAt(nPmRa)).coo=Field.PMRA;
      if( nPmDec>=0 && nPmDec<n ) ((Field)vField.elementAt(nPmDec)).coo=Field.PMDE;
      if( nX>=0   && nX<n   ) ((Field)vField.elementAt(nX)).coo=Field.X;
      if( nY>=0   && nY<n   ) ((Field)vField.elementAt(nY)).coo=Field.Y;
      badRaDecDetection = badDetection;
      if( plan!=null ) plan.hasPM=-1;
   }

   //   /** Retourne l'indice de la colonne RA si connu, sinon -1 */
   //   public int getRaIndex()  { return nRa; }
   //
   //   /** Retourne l'indice de la colonne DEC si connu, sinon -1 */
   //   public int getDecIndex() { return nDec; }


   // PEUT ETRE FUSIONNER setTarget() et parseTarget()
   public void setTarget(String target) {
      double []tmp = parseTarget(target);
      if( tmp==null ) return;
      flagTarget = true;		// Il sera inutile de calculer le target en fct des donnees
      rajc=tmp[0]; dejc=tmp[1]; rm=tmp[2];
   }
   
   /** Positionnement à l'avance du target finale => nécessairement en ICRS */
   public boolean setTargetCoord( String target ) {
      if( Localisation.notCoord(target) ) return false;
      aladin.trace(6,"Pcat.setTargetCoord("+target+")");
      Coord c;
      try { c = new Coord(target); } catch( Exception e ) { return false; }
      if( Double.isNaN(c.al) || Double.isNaN(c.del) ) return false;
      rajc=c.al; dejc=c.del; rm=14;
      flagTarget=true;
      return true;
   }

   /** Data access via XML/VOTable
    *
    * @param dis
    * @return
    * @throws Exception
    */
   //   private int votableParsing(MyInputStream dis) throws Exception {
   //     try {
   //        o= new Objet[DEFAULTBLOC];
   //        nb_o = 0;
   //        catalog=plan.label;
   //        table=plan.label;
   //        leg=null;
   //        flagTarget=false;
   //        minRa=minDec = Double.MAX_VALUE;
   //        maxRa=maxDec = -Double.MAX_VALUE;
   //        hiddenField=null;
   //        flagEndResource = false;
   //        VOTable res = new VOTable(this);
   //        boolean ok = res.parse(dis);
   //
   //        if( ok ) {
   //           if( !flagEndResource ) endResource();
   //           if( rm == 0.0 ) {
   //              plan.error = "no RA or DE rows";
   //              aladin.error = plan.error;
   //           }
   //        } else {
   //           plan.error = "Error: "+res.getError();
   //           aladin.error = plan.error;
   //        }
   //        if( plan.error!=null )
   //          System.out.println("!!! "+plan.label+": "+plan.error);
   //        return ok?nb_o:-1;
   //        } catch( Exception e) {
   //          System.out.println("votableParsing : " + e);
   //          plan.sendLog("Error","votableParsing() ["+e+"] u="+(plan.u==null?"null":plan.u.toString()));
   //        }
   //        return -1;
   //   }

   /**
    * Parsing de la table
    * @param dis Le flux à parser
    * @param endTag en cas de parsing partiel, le tag de fin, sinon null
    * @return le nombre d'objets dans la table
    * @throws Exception
    */
   protected int tableParsing(MyInputStream dis,String endTag) throws Exception {
      o= new Obj[DEFAULTBLOC];
      nb_o = 0;
      if( plan!=null ) {
         catalog=plan.label;
         table=plan.label;
      }
      leg=null;
//      flagTarget=false;
      minRa=minDec = Double.MAX_VALUE;
      maxRa=maxDec = -Double.MAX_VALUE;
      hiddenField=null;
      flagEndResource = false;

      long d = System.currentTimeMillis();
      long type = dis.getType();
      boolean ok;

      // Parsing FITS table
      if( (type & (MyInputStream.FITST|MyInputStream.FITSB))!=0 ) {
         plan.headerFits = new FrameHeaderFits(plan,dis);
         res = new TableParser(aladin,this,((PlanCatalog)plan).headerFits.getHeaderFits(),plan.flagSkip);
         ok = res.parse(dis);

         // Parsing XML/CSV
      } else {
         String sep;
         if( plan instanceof PlanBGCat ) sep = "\t";
         else if( (type&MyInputStream.BSV) == MyInputStream.BSV  ) sep = " ";
         else if( dis.getSepCSV()!=-1 ) sep = dis.getSepCSV()+"";
         else sep = aladin.CSVCHAR;

         res = new TableParser(aladin,this, sep);
         res.setFileName( dis.getFileName() );
         ok = res.parse(dis,endTag);
      }

      // Cas particulier pour un plan hiérarchique
      if( ok && plan instanceof PlanBGCat ) {
         if( nb_o==0 ) setRecord(0, 0, Double.NaN, null);  // pour initialiser tout de même la légende
         return nb_o;
      }

      if( ok ) {
         if( !flagEndResource ) endResource();
         long duree=System.currentTimeMillis()-d;
         String s = "Catalog queried, loaded and parsed in "+Util.getTemps(duree);
         tableParserInfo("\n"+s);
         Aladin.trace(3,s);
         if( !flagXY && rm==0.0 ) plan.error = aladin.error = "no RA or DE columns";
      } else plan.error = aladin.error = "Error: "+res.getError();
      if( plan.error!=null ) System.out.println("!!! "+plan.label+": "+plan.error);
      return ok && nb_o>=0?nb_o:-1;
   }

   /** (Re)génération de la projection par défaut associée à la liste des objets en prenant
    * comme centre de projection le barycentre */
   protected void createDefaultProj() {
      for( int i=0; i<nb_o; i++ ) {
         Position s = (Position)o[i];
         if( i==0 || s.raj < minRa )  minRa  = s.raj;
         if( i==0 || s.raj > maxRa )  maxRa  = s.raj;
         if( i==0 || s.dej < minDec ) minDec = s.dej;
         if( i==0 || s.dej > maxDec ) maxDec = s.dej;
      }
      computeTarget();
      postJob(rajc,dejc,rm,false);
   }

   /** Determine le target des donnees en fonction.
    * en fonction de minRa, maxRa, minDec, maxDec et
    * met a jour rajc, dejc et rm en fonction
    */
   private void computeTarget() {
//      if( maxDec-minDec>90 ) {
      if( maxDec-minDec>90 || maxRa-minRa>180.) {
         dejc=rajc=0;
         rm=180*60.;
      } else {
         if( maxRa-minRa>180. ) {
            double alpha = 360-maxRa;
            rajc = (minRa+alpha)/2 - alpha;
         } else rajc = (minRa+maxRa)/2;
         dejc = (minDec+maxDec)/2;
//         double r = Math.max(Math.abs(minRa-rajc)*Math.cos( (dejc*Math.PI)/180.0 ), Math.abs(minDec-dejc));
//         rm = (nb_o==1 || r==0.)?7:r*60.0*1.4142;
         double r = Coord.getDist( new Coord(minRa, minDec), new Coord(maxRa, maxDec) )/2;
         rm = (nb_o==1 || r==0.)?7:r*60.0;

      }
      Aladin.trace(3,"computeTarget ra=["+minRa+".."+maxRa+"]=>"+rajc+" de=["+minDec+".."+maxDec+"]=>"+dejc+" rm=["+rm+"]");
   }

   // Analyse de la chaine indiquant le target
   // REMARQUE : Cette procedure a ete ecrite en deux temps trois mouvements, il faudra
   // ABSOLUMENT la betonner
   double [] parseTarget(String target) {
      double [] tmp = new double[3];
      StringTokenizer st;
      boolean neg;

      st = new StringTokenizer(target,"+-,=/");

      try {
         tmp[0] = Double.valueOf(st.nextToken().trim()).doubleValue();
      } catch( Exception e1 ) {  return null; }

      // Determination du signe de la declinaison
      neg = ( target.lastIndexOf('-')>0 );

      try {
         tmp[1] = Double.valueOf(st.nextToken().trim()).doubleValue();
         if( neg ) tmp[1]=-tmp[1];
      } catch( Exception e2 ) { return null; }

      try {
         st.nextToken();        // On passe sur rm/bm sans s'inquieter
         tmp[2] = Double.valueOf(st.nextToken().trim()).doubleValue();
         if( target.indexOf("bm")>0 ) tmp[2] /= 2;   // Une boite
      } catch( Exception e3 ) { tmp[2]=11; }

      return tmp;
   }

   /** Remplissage d'un Plan de catalogue
    * @param plan Le plan d'appartenance
    * @param u L'URL d'acces aux données a analyser
    * @param verbose true si on peut afficher des messages d'erreur
    * @return le nombre d'objets ou <I>-1</I> si probleme
    */
   protected int setPlanCat(Plan plan, URL u,boolean verbose) {
      return setPlanCat(plan,u,null,null,verbose);
   }

   /** Remplissage d'un Plan de catalogue
    * @param plan Le plan d'appartenance
    * @param dis le flux d'acces axdonnées a analyser (ou null)
    * @param endTag en cas de parsing partiel, le tag de fin
    * @param verbose true si on peut afficher des messages d'erreur
    * @return le nombre d'objets ou <I>-1</I> si probleme
    */
   protected int setPlanCat(Plan plan, MyInputStream dis,String endTag,boolean verbose) {
      return setPlanCat(plan,null,dis,endTag,verbose);
   }

   /** Remplissage d'un Plan de catalogue
    * @param plan Le plan d'appartenance
    * @param u L'URL d'acces au TSV a analyser	(ou null)
    * @param dis le flux d'acces aux données a analyser (ou null)
    * @param endTag en cas de parsing XML partiel, le tag de fin, sinon null
    * @param verbose true si on peut afficher des messages d'erreur
    * @return le nombre d'objets ou <I>-1</I> si probleme
    */
   protected int setPlanCat(Plan plan, URL u, MyInputStream dis,String endTag,boolean verbose) {
      int nb=-1;
      boolean flagFootprint = false;
      // Construction et appel de l'URL
      try {

         // Pour de l'info
         if( u!=null ) tableParserInfo(u+"\n");

         // Deux tentatives... cochonnerie de JAVA
         if( dis==null ) {
            try { dis =Util.openStream(u); }
            catch( Exception efirst ) { dis =Util.openStream(u); }
         }
         plan.dis=dis;

         long type=dis.getType();
         flagVOTable=(type & MyInputStream.VOTABLE)!=0;
         flagFootprint = (type & MyInputStream.FOV)!=0;
//         flagSIAV2 = (type & MyInputStream.SIAV2)!= 0;
         flagSIA = (type & MyInputStream.SIA)!= 0;
         flagEPNTAP = (type & MyInputStream.EPNTAP)!= 0;
         if( flagFootprint ) {
            // devrait etre RESOURCE, mais il y a un bug dans getUnreadBuffer (mange un tag trop en avant)
            endTag = "TABLE";
         }
         //System.out.println("flagFOV : "+flagFootprint);
         nb = tableParsing(dis,endTag);
      }
      catch( OutOfMemoryError e ) {
         aladin.error = OUTOFMEMORY;;
         if( verbose )  {
            System.out.println("!!! "+(plan==null ? "":plan.label+": ")+aladin.error);
            Aladin.error(aladin.error);
         }
         o= new Obj[DEFAULTBLOC];
         nb_o = 0;
         aladin.gc();
      }
      catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         aladin.error = CATABORT+"\n --> "+e;
         o= new Obj[DEFAULTBLOC];
         nb_o = 0;
         aladin.gc();
         if( verbose ) {
            System.out.println("!!! "+(plan==null ? "":plan.label+": ")+aladin.error);
            Aladin.error(aladin.error);
         }
         //         if( plan!=null ) plan.sendLog("Error","setPlanCat() ["+e+"] u="+(plan.u==null?"null":plan.u.toString()));
         nb=-1;
      }

      // s'il s'agit d'un stream FOV, on doit encore parser la suite !!
      if( flagFootprint ) {
         FootprintParser fParser = new FootprintParser(dis, res.getUnreadBuffer());
         Hashtable<String, FootprintBean> idToFootprint = fParser.getFooprintHash();
         attachFootprintToSources(idToFootprint);
      }

      // par défaut, on montre les footprints associés à un plan catalogue
      // NON, PAS UNE BONNE IDEE
      //       if (plan instanceof PlanCatalog && ((PlanCatalog) plan).hasAssociatedFootprints()) {
      //           ((PlanCatalog) plan).showFootprints(true);
      //       }

      return nb;
   }

   /**
    * Attache les footprint aux objets précédemment créés
    *
    * @param hash le hash donnant la correspondance ID --> footprint
    */
   private void attachFootprintToSources(Hashtable<String, FootprintBean> hash) {
      Source s;
      int idx = -1;
      String key;
      FootprintBean footprint;
      PlanField pf;
      for( int i=0; i<nb_o; i++ ) {
         if( ! (o[i] instanceof Source) ) continue;
         s = (Source)o[i];
         pf=null;

         idx = s.findColumn("FoVRef");
         if( idx<0 ) idx = s.findUtype("char:SpatialAxis.coverage.support.id");
         if( idx<0 )  continue;

         key = s.getValue(idx);
         footprint = hash.get(key);
         s.setIdxFootprint(idx);
         // we attach the found footprint to the current source
         if( footprint!=null ) {
            s.setFootprint(pf = new PlanField(aladin, footprint, key));
         }
         // we will now attach the position angle to the source
         idx = s.findUCD("pos.posAng");
         if( idx<0 ) continue;

         String angle = s.getValue(idx);
         if( angle==null ) angle = "";

         double angleD;
         try {
            angleD = Double.valueOf(angle).doubleValue();
         }
         catch(NumberFormatException e) {angleD=0;}
         if( pf != null ) {
            pf.make(s.raj, s.dej, angleD);
         }
      }
   }

   private int nextID=0;

   /** Retourne un indice unique afin de pouvoir générer un ID de l'objet.
    * Celui-ci peut être différent de l'index de l'objet, notamment si des objets
    * sont supprimés en cours de traitement */
   protected int getNextID() { return nextID; }

   // Retourne le prochain index libre dans le tableau des objets
   // et met à jour un numéro unique pour pouvoir faire  un identificateur (voir getnextID())
   int nextIndex() {
      if( o==null ) {
         o = new Obj[DEFAULTBLOC];
         nb_o=0;
      }
      nextID++;
      if( nb_o<o.length ) return(nb_o++);
      else {

         // On augmente la taille du tableau
         Obj [] otmp = new Obj[o.length>MAXBLOC?o.length+MAXBLOC:o .length*2];
         System.arraycopy(o,0,otmp,0,o.length);
         o = otmp;
         otmp=null;
         return nb_o++;
      }
   }

   /** Ajout d'un nouvel objet
    * Retourne la position d'insertion dans le tableau o[], -1 si problème */
   protected int setObjet(Obj newobj) {
      int i = nextIndex();
      if( i<0 ) return -1;         // C'est plein

      // Pour gerer l'insertion d'un texte
      if( newobj instanceof Tag ) {
         Tag t = (Tag) newobj;
         t.setEditing(false);

         // Pour gerer le blocNote
      } else if( newobj instanceof Cote ) {
         Cote c = (Cote)newobj;
         if( c.debligne!=null ) {
            c.setId();
            aladin.console.printInPad(c.id+"\n");
         }
      }
      o[i] = newobj;
      return i;
   }
   
   /**Insertion de la source après la dernière source de même légende, sinon à la fin */
   protected void insertSource(Source src) {
      for( int i=nb_o-1; i>=0; i-- ) {
         if( !(o[i] instanceof Source) ) continue;
         
         // On a trouvé ?
         if( ((Source)o[i]).getLeg()==src.getLeg() ) {
            int n = nextIndex();
            for( int j=n; j>i+1; j--) o[j] = o[j-1];  // décalage
            o[i+1] = src;
            return;
         }
      }
      
      setObjetFast(src);
   }

   // Ajout d'un nouvel objet (non interractivement)
   protected void setObjetFast(Obj newobj) {
      int i = nextIndex();
      o[i] = newobj;
   }

   /** Vérifie et fixe le nombre de champs de toutes les objets Source
    * ayant la même légende que celle passée en paramètre.
    * @param leg La légende "étalon"
    */
   protected void fixInfo(Legende leg) {
      int j=0;
      for( int i=0; i<nb_o; i++ ) {
         if( !(o[i] instanceof Source) ) continue;
         Source s = (Source)o[i];
         if( s.getLeg()==leg ) { s.fixInfo(); j++; }
      }
      //System.out.println("FIX J'ai fixé "+j+" sources du plan "+plan);
   }

   /** Retourne l'indice d'un objet, ou -1 si non trouvé */
   protected int getIndex(Obj x) {
      for( int i=0; i<nb_o; i++ ) if( x==o[i] ) return i;
      return -1;
   }

   protected boolean removable = false;     // Possibilité de changer le statut d'un plan catalogue

   /** Suppression d'un objet.
    * Suppression d'un objet par ecrasement de sa reference
    * avec le dernier element du tableau
    * @param obj L'objet a supprimer
    * @param force true si on peut supprimer même les sources des catalogues
    * @return <I>true</I> si trouve, sinon <I>false</I>
    */
   protected boolean delObjet(Obj obj) { return delObjet(obj,removable); }
   protected boolean delObjet(Obj obj,boolean force) {
      int i;

      // Les sources ne peuvent etre supprimer individuellement
      if( !force && obj instanceof Source ) return false;

      // Peut être faut-il qu'il mette à jour certains paramètres (ex. Ligne)
      obj.remove();

      // Parcours de tous les objets du plan
      for( i=0; i<nb_o && obj!=o[i]; i++ );
      if( i<nb_o ) {
         //         if( i!=nb_o-1 ) o[i] = o[nb_o-1];
         for( ; i<nb_o-1; i++ ) o[i]=o[i+1];  // Pour conserver l'ordonnancement
         nb_o--;
         return true;
      }
      return false;
   }

   /** Affiche l'id associe a un objet.
    * @param i Indice de l'objet
    */
   protected void showBaratin(int i) { o[i].status(aladin); }

   /** Avant un Draw ou un writeLink, refait la projection si nécessaire,
    * et vérifie si le plan doit être affiché dans la vue
    * @param v la vue concernée
    * @param draw réponse initiale (permet de retourner false et tout de même reprojeter
    * @return true si la source est visible, false sinon
    */
   protected boolean computeAndTestDraw(ViewSimple v,boolean draw) {


      // Projection suivant la vue
      if( plan.isCatalog() || plan.type==Plan.TOOL ||
            plan.type==Plan.APERTURE || plan.type==Plan.FOV ) projection(v);

      // Ces objets sont-ils projetables dans cette vue ?
      if( !drawnInViewSimple[v.n] ) return false;

      return draw;
   }

   /** Dessin des objets.
    * Affiche tous les objets du plan qui se trouve dans le rectangle
    * @param g Le contexte graphique
    * @param r Le rectangle qui delimite la zone concernee
    * @param draw <I>true</I> pour que l'affichage soit effectif,
    *             sinon il n'y aura qu'un simple calcul de position
    * @param dx,dy Offset pour le tracage
    */
   protected int draw(Graphics g, Rectangle r,ViewSimple v,boolean draw,int dx,int dy) {
      return draw(g,r,v,draw,false,dx,dy);
   }
   synchronized protected int draw(Graphics g, Rectangle r,ViewSimple v,boolean draw,boolean onlySelected,int dx,int dy) {

      if( !computeAndTestDraw(v,draw) ) return 0;

      long t1 = Util.getTime();

      int nb=0;
      try  {

         // gestion de la transparence
         // Le test d'impression est fait par dx==0 car à l'écran, il n'y a pas d'offset
         if( dx==0 && plan!=null && Aladin.isFootprintPlane(plan) &&
               Aladin.ENABLE_FOOTPRINT_OPACITY && plan.getOpacityLevel()>0.02 && g instanceof Graphics2D ) {
            drawFovInTransparency(g, r, v, draw, dx, dy);
         }

         g.setColor(c);
         for( int i=0; i<nb_o; i++ ) {
            if( r!=null && !o[i].inClip(v,r) ) continue;
            if( onlySelected && !o[i].isSelected() ) continue;
            if( o[i].draw(g,v,dx,dy) ) nb++;
         }

         long t2 = Util.getTime();
         plan.statTimeDisplay=t2-t1;
      } catch( Exception e) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
      plan.statNbItems=nb;
      return nb;
   }

   private void drawFovInTransparency(Graphics g, Rectangle r, ViewSimple v, boolean draw, int dx, int dy) {
      Graphics2D g2d=null;
      Composite saveComposite=null;
      g2d = (Graphics2D)g;
      g2d.setColor(plan.c);
      saveComposite = g2d.getComposite();
      Composite myComposite = Util.getFootprintComposite(plan.getOpacityLevel());
      g2d.setComposite(myComposite);

      ArrayList linesToProcess = new ArrayList(); // pour retrouver l'ensemble des lignes
      for( int i=0; i<nb_o; i++ ) {

         // cas d'un Cercle
         if( o[i] instanceof Cercle ) {
            // on ne fait rien ici, intégré dans Cercle.draw
         }

         // cas d'un Polygone
         else if( o[i] instanceof Ligne ) {
            linesToProcess.add(o[i]);
         }
      }

      // traitement et affichage des polylignes trouvés
      if( linesToProcess.size()>0 ) {
         Ligne[] lArray = (Ligne[])linesToProcess.toArray(new Ligne[linesToProcess.size()]);
         Ligne curLine, startLine;
         ArrayList<Ligne> polyLine = new ArrayList();
         Point[] points;
         int[] x;
         int[] y;
         int k;
         // parcours du tableau en partant de la fin
         for( int i=lArray.length-1; i>=0; i-- ) {
            // déja traité ? on passe au suivant
            if( ! linesToProcess.contains(lArray[i]) ) continue;

            polyLine.clear();
            curLine = lArray[i];
            startLine = curLine;
            linesToProcess.remove(curLine);
            // parcours du polygone
            while( curLine.debligne!=null && curLine.debligne!=startLine ) {
               curLine = curLine.debligne;
               polyLine.add(curLine);
               linesToProcess.remove(curLine);
            }
            // dessin du polygone trouvé
            if( polyLine.size()>0 ) {
               points = new Point[polyLine.size()];
               x = new int[polyLine.size()];
               y = new int[polyLine.size()];
               Iterator<Ligne> it = polyLine.iterator();
               k = 0;
               while( it.hasNext() ) {
                  points[k] = (it.next()).getViewCoord(v);
                  if( points[k]==null ) {
                     g2d.setComposite(saveComposite);
                     return;
                  }
                  x[k] = points[k].x;
                  y[k] = points[k].y;
                  k++;
               }
               g2d.setColor(curLine.getColor());
               // TODO : toute la gestion de la transparence serait simplifié avec un objet PolyLigne
               if( curLine.isVisible() ) g2d.fill(new Polygon(x, y, k));
            }
         }
      }

      g2d.setComposite(saveComposite);
   }


   /** Génération des lignes des liens pour une carte HTML cliquable
    * Affiche tous les objets du plan sous la forme
    * NOMDUPLAN <TAB> x <TAB> y <TAB> id <TAB> url
    * @param out le flux de sortie
    * @param draw <I>true</I> pour que l'affichage soit effectif,
    *             sinon il n'y aura qu'un simple calcul de position
    */
   synchronized protected void writeLink(OutputStream out, ViewSimple v,boolean draw) throws Exception {
      if( !computeAndTestDraw(v,draw) ) return;
      for( int i=0; i<nb_o; i++ ) o[i].writeLink(out,v);
   }

   synchronized protected void writeLinkFlex(OutputStream out, ViewSimple v,boolean draw) throws Exception {
      if( !computeAndTestDraw(v,draw) ) return;
      for( int i=0; i<nb_o; i++ ) {
         if( ! (o[i] instanceof Source) ) continue;
         ((Source)o[i]).writeLinkFlex(out,v);
      }
   }

   /** retourne le nombre d'objets */
   protected int getCount() { return nb_o; }

   /** retourne true si le plan contient des objets */
   protected boolean hasObj() { return nb_o>0; }

   /** retourne l'objet à l'index précisé */
   protected Obj getObj(int index) { return index>=nb_o ? null : o[index]; }

   // Recupération d'un itérator sur les objets
   protected Iterator<Obj> iterator() { return new PlanObjetIterator(null); }

   // Recupération d'un itérator sur les objets visibles dans la vue
   protected Iterator<Obj> iterator(ViewSimple v) { return new PlanObjetIterator(v); }
   
   class PlanObjetIterator implements Iterator<Obj> {
      
      private int index=0;
      boolean visible=true;
      
      PlanObjetIterator(ViewSimple v) {
         index=0;
         if( v!=null ) visible=drawnInViewSimple[v.n];
         else visible=true;
      }
      public boolean hasNext() { return visible && index<nb_o; }
      public Obj next() { return o[index++]; }
      public void remove() { }
   }



}

