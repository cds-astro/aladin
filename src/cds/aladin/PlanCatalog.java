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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

import cds.tools.Util;

/**
 * Plan dedie a un catalogue (CATALOG)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class PlanCatalog extends Plan {
   URL url = null;

  /** Creation d'un plan de type CATALOG (via une fichier)
   * @param file  Le nom du fichier
   */
   protected PlanCatalog(Aladin aladin, String file, MyInputStream in,boolean skip,boolean doClose) {
      this.doClose=doClose;
      flagSkip = skip;
      String label = "Cat";
      this.dis=in;
      try {
         url = new URL("file:"+(new File(file)).getCanonicalPath());
      } catch( Exception e ) {
         String s =file+" not found";
         Aladin.warning(s,1);
         return;
      }

      if( file!=null ) {
         int i = file.lastIndexOf(Util.FS);
         label=(i>=0)?file.substring(i+1):file;	// Nom du fichier
      }
      
      // Subtilité: Si on ne doit pas fermer le flux, c'est que c'est un MEF.
      if( !doClose ) noBestPlacePost=true;

      flagLocal=true;
      flagWaitTarget=true;  // Voir Command.waitingPlanInProgress

      Suite(aladin,label,"","",null,null, null);
   }

  /** Creation d'un plan de type CATALOG (via un InputStream)
   * @param in InputStream
   * @param label le label du plan (proposé)
   * @param origin origine du catalogue
   */
   protected PlanCatalog(Aladin aladin, MyInputStream in,String label,String origin) {
      this.dis = in;
      if( label==null) label="VOApp";
      flagWaitTarget=true;
      Suite(aladin,label,"","",origin,null, null);
   }

   protected PlanCatalog(Aladin aladin, MyInputStream in,String label) {
   	this(aladin,in,label,null);
   }

   private HttpURLConnection httpConn;
   
   protected PlanCatalog(Aladin aladin, HttpURLConnection httpConn,String label) {
      this.httpConn = httpConn;
      if( label==null) label="HttpConn";
      flagWaitTarget=true;
      Suite(aladin,label,"","",null,null, null);
     }
	
	protected PlanCatalog(Aladin aladin, MyInputStream in, String label, Server server, String query) {
		this.dis = in;
		if (label == null)
			label = "VOApp";
		flagWaitTarget = true;
		Suite(aladin, label, "", "", null, server, query);
	}

  /** Creation d'un plan de type CATALOG (sans info)
   */
  protected PlanCatalog(Aladin aladin) {
    this.aladin = aladin;
    type = CATALOG;
    c          = Couleur.getNextDefault(aladin.calque);
    pcat       = new Pcat(this,c,aladin.calque,aladin.status,aladin);
    flagOk=true;
  }

  public PlanCatalog() {}

  /** Creation d'un plan de type CATALOG via une URL
   * @param aladin reference
   * @param u      l'URL qu'il va falloir appeler
   * @param label  le nom du plan (dans la pile des plans)
   * @param objet  le target central (objet ou coord)
   * @param param  les parametres du plan (radius...)
   * @param from   la provenance des donnees
   */
   protected PlanCatalog(Aladin aladin, URL u, MyInputStream in, String label,
                         String objet,String param,String from,Server server) {
      this.dis = in;
      this.u     = u;
      flagLocal  = false;

      Suite(aladin,label,objet,param,from,server, null);

   }

  /** Creation d'un plan de type CATALOG
   * @param aladin reference
   * @param label  le nom du plan (dans la pile des plans)
   * @param objet  le target central (objet ou coord)
   * @param param  les parametres du plan (radius...)
   * @param from   la provenance des donnees
   * @param server le serveur d'origine
 * @param query 
   */
   protected void Suite(Aladin aladin, String label,String objet,String param,String from,Server server, String query ) {
      setLogMode(true);
      this.aladin= aladin;
      type       = CATALOG;
      c          = Couleur.getNextDefault(aladin.calque);
      setLabel(label);
      id=this.label;
      this.objet = objet;
      this.param = param;
      this.copyright  = from;
      headerFits=null;
      this.server=server;
      this.query = query;
      if( server!=null ) {
         filters=server.filters;
         filterIndex=aladin.configuration.getFilter()==0? server.getFilterChoiceIndex() : -1;
      }
      pcat       = new Pcat(this,c,aladin.calque,aladin.status,aladin);
      aladin.calque.unSelectAllPlan();
      selected   = true;

      threading();
   }
   
   /** retourne le nom de la table associée à une source */
   protected String getTableName(Source o) {
      String s = o.leg==null ? null : o.leg.name;
      if( s==null ) {
         if( o.info==null ) return "Table";
         int i = o.info.indexOf('|');
         int j = o.info.indexOf('>');
         if( i==-1 || j==-1 ) return "Table";
         s = o.info.substring(i+1,j);
      }
      if( s.endsWith("/out") ) s=s.substring(0,s.length()-4);
      return s;
   }

   /** retourne le nom de la première table */
   protected String getFirstTableName() {
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         return getTableName( (Source)o );
      }
      return null;
   }
   
   private long lastFilterLock = -1;

   protected boolean isSync() {
      boolean hasSource = hasSources();
      boolean isSync = (flagOk && error==null
            || flagOk && pcat!=null && (hasSource || error!=null && !hasSource)
            || pcat!=null && error!=null && !hasSource);
      isSync = isSync && (planFilter==null || planFilter.isSync() );
      return  isSync;
   }

  /** Libere le plan.
   * cad met toutes ses variables a <I>null</I> ou a <I>false</I>
   */
   protected boolean Free() {
      if( getCounts()<=0 || isSED() ) return true;
      
      aladin.view.deSelect(this);
      if (Aladin.PROTO) {//TODO:: tintinproto
    	  TapManager.getInstance(aladin).updateDeleteUploadPlans(this);
      }
      super.Free();
      aladin.view.free(this);
      headerFits=null;
      // thomas
      FilterProperties.notifyNewPlan();
      
      return true;
   }
   
   protected boolean isCatalog() { return true; }


   protected String getDescription() {
      if( pcat.description==null ) return null;
      return pcat.description.toString();
   }

   /** Retourne le nombre d'objects */
   protected int getCounts() { return pcat==null ? 0 : pcat.getCount(); }

   protected Obj [] getObj() {
      return pcat.o;
   }

   /** Modifie (si possible) une propriété du plan */
   protected void setPropertie(String prop,String specif,String value) throws Exception {
      if( prop.equalsIgnoreCase("Shape") ) {
         int n = Source.getShapeIndex(value);
         if( n==-1 ) throw new Exception("Shape unknown");
         setSourceType(n);
         aladin.calque.repaintAll();
      } else if( prop.equalsIgnoreCase("Filter") ) {
         setFilter(value);
      } else super.setPropertie(prop,specif,value);
   }
   
   protected boolean setActivated() {
      if( !hasSources() ) return false;
      return super.setActivated();
   }


  /** Attente pendant la construction du plan.
   * @return <I>true</I> si ok, <I>false</I> sinon.
   */
   protected boolean waitForPlan() {
      int n=0;
      boolean flagError=false;
      
      // On n'a pas encore le MyInputStream, mais uniquement la connection http
      if( httpConn!=null ) {
         try {
            InputStream is;
            if( httpConn.getResponseCode() < 400 ) {
               is = httpConn.getInputStream();
            } else {
               flagError=true;
               is = httpConn.getErrorStream();;
            }
            dis = new MyInputStream( is );
         } catch( IOException e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
            return false;
         }
      }

      if( dis!=null ) n=pcat.setPlanCat(this,dis,null,true);
      else if( flagLocal ) n=pcat.setPlanCat(this,url,true);
      else n=pcat.setPlanCat(this,u,true);
      
      if( flagError ) n=-1;
      
      if( n==0 )  aladin.error = error = "EMPTY: No object found in the field!";
      if( n<=0 ) {
          callAllListeners(new PlaneLoadEvent(this, PlaneLoadEvent.ERROR, aladin.error));
          return false;
      }
      else {
         // En cas de chargement par un fichier local, mettre à jour objet
         if( (objet==null || objet.length()==0) && co!=null) {
            objet = co.getSexa();
            aladin.dialog.setDefaultTarget(objet);
            aladin.dialog.setDefaultTaille(this);
         }
         
         // Sélection de l'ordre d'affichage des champs
         setFieldOrder();

         // Peut être un nom dans EXTNAME ?
         setExtName();

         // Y a-t-il des filtres prédéfinis à activer ?
         setFilter(filterIndex);
         
         //to add loaded plan into upload options
         if (Aladin.PROTO) {//TODO:: tintinproto
        	 TapManager.getInstance(aladin).updateAddUploadPlans(this);
		}
      }

      if( getNbTable()>1 ) aladin.calque.splitCatalog(this);
      
      setActivated(true);
      aladin.view.setRepere(this);

     callAllListeners(new PlaneLoadEvent(this, PlaneLoadEvent.SUCCESS, null));
     return true;
   }

   /** Désactive tous les filtres dédiées */
   static protected void desactivateAllDedicatedFilters(Aladin aladin) {
      Plan [] allPlan = aladin.calque.getPlans();
      for( int i=0; i<allPlan.length; i++ ) {
         Plan p = allPlan[i];
         if( !p.isSimpleCatalog() ) continue;
         ((PlanCatalog)p).setFilter(-1);
      }
   }

   /** Retourne le nombre de tables qui composent le catalogue */
   protected int getNbTable() { return pcat.nbTable; }
   
   /** Accroit ou décroit la taille du type de source */
   void increaseSourceSize(int sens) { 
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         ((Source)o).increaseSourceSize(sens);
      }
   }

   /** Retourne la liste des légendes des tables qui composent le catalogue */
   protected Vector<Legende> getLegende() {
      Vector<Legende> leg = new Vector<Legende>(10);
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         Source s = (Source)o;
         if( s.leg==null ) continue;
         if( !leg.contains(s.leg) ) leg.addElement(s.leg);
      }
      return leg;
   }

   /** Retourne la première légende trouvée dans la liste des sources */
   protected Legende getFirstLegende() {
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         Source s = (Source)o;
         if( s.leg!=null ) return s.leg;
      }
      return null;
   }


   /** Retourne la progression du chargement */
    protected String getProgress() {
       if(!flagOk && error==null ) return " - "+pcat.getCount() + " object"+(pcat.getCount()<=1?"":"s")+" - in progress...";
       return super.getProgress();
    }

	 /** retourne true si le plan a des sources */
	 protected boolean hasSources() { return pcat!=null && pcat.hasObj(); }

	 /******************************************************** QUELQUES TESTS UNITAIRES *******************************************************/

	 static private boolean test1(Aladin aladin,String t,String s, String r) {
	    System.out.print("> PlanCatalog test : "+t+"...");
	    int trace=aladin.levelTrace;
	    aladin.levelTrace=0;
	    try {
	       MyByteArrayStream buf = new MyByteArrayStream();
	       buf.write(s);
	       MyInputStream in = new MyInputStream( buf.getInputStream() );

	       PlanCatalog p = new PlanCatalog(aladin, in, t);
	       while( !p.isReady() ) { Util.pause(1000); }
	       Source o = (Source) p.iterator().next();
	       String r1="row="+p.getCounts()+" col="+p.getFirstLegende().getSize()+" ra="+o.raj+" de="+o.dej+" id="+o.id;
	       if( !r1.equals(r) ) throw new Exception("respond test ["+r1+"] should be ["+r+"]");
	    } catch( Exception e ) {
	       e.printStackTrace();
	       aladin.levelTrace=trace;
	       System.out.println(" Error: "+e.getMessage());
	       return false;
	    }
        aladin.levelTrace=trace;
	    System.out.println(" OK");
	    return true;
	 }

	 static String TEST_TSV_TITLE = "TSV/1_header_line";
	 static String TEST_TSV_RESULT = "row=1 col=18 ra=77.405544 de=-63.777272 id=J050937.33-634638.1";
	 static String TEST_TSV = "globalSourceID\tsourceCatalog\tepoch\tdesignation\ttmass_designation\tra\tdec\tmagJ\tmagH\tmagK\tmag3_6\tdmag3_6\tmag4_5\tdmag4_5\tmag5_8\tdmag5_8\tmag8_0\tdmag8_0\n"+
     "1531664539\tiracc\tSMP SSTISAGEMC\tJ050937.33-634638.1\t05093732-6346387\t77.405544\t-63.777272\t6.976\t6.785\t\t6.715\t0.084\t6.719\t0.045\t6.743\t0.029\t6.716\t0.027";

	 static String TEST_VOTABLE_TITLE = "VOTABLE/classic_TABLEDATA";
	 static String TEST_VOTABLE_RESULT = "row=1 col=18 ra=1.2999999999999998 de=67.83333333333333 id=1";
	 static String TEST_VOTABLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
	 "<VOTABLE version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
	   "xmlns=\"http://www.ivoa.net/xml/VOTable/v1.1\"\n" +
	   "xsi:schemaLocation=\"http://www.ivoa.net/xml/VOTable/v1.1 http://www.ivoa.net/xml/VOTable/v1.1\">\n" +
	  "<DESCRIPTION>\n" +
	    "VizieR Astronomical Server: vizier.u-strasbg.fr  2010-07-01T11:59:16\n" +
	    "Explanations and Statistics of UCDs:         See LINK below\n" +
	    "In case of problem, please report to:    cds-question@unistra.fr\n" +
	  "</DESCRIPTION>\n" +
	 "<!-- VOTable description at http://www.ivoa.net/Documents/latest/VOT.html -->\n" +
	 "<DEFINITIONS>\n" +
	   "<COOSYS ID=\"J2000\" system=\"eq_FK5\" equinox=\"J2000\"/>\n" +
	 "</DEFINITIONS>\n" +
	 "<INFO ID=\"Ref\" name=\"-ref\" value=\"VIZ4c2c829613a3\"/>\n" +
	 "<INFO ID=\"MaxTuples\" name=\"-out.max\" value=\"50\"/>\n" +
	 "<INFO name=\"CatalogsExamined\" value=\"2\">\n" +
	   "2 catalogues with potential matches were examined.\n" +
	 "</INFO>\n" +
	 "<INFO ID=\"Target\" name=\"-c\" value=\"001.286805+67.840004,rm=2.\"/>\n" +
	 "<RESOURCE ID=\"yCat_3135\" name=\"III/135A\">\n" +
	   "<DESCRIPTION>Henry Draper Catalogue and Extension (Cannon+ 1918-1924; ADC 1989)</DESCRIPTION>\n" +
	   "<COOSYS ID=\"B1900_1900.000\" system=\"eq_FK4\" equinox=\"B1900\" epoch=\"1900.000\"/>\n" +
	   "<TABLE ID=\"III_135A_catalog\" name=\"III/135A/catalog\">\n" +
	     "<DESCRIPTION>The catalogue</DESCRIPTION>\n" +
	     "<!-- RowName:  ${HD} -->\n" +
	     "<!-- Now comes the definition of each field -->\n" +
	     "<FIELD name=\"_r\" ucd=\"pos.angDistance\" datatype=\"float\" width=\"3\" precision=\"1\" unit=\"arcmin\"><!-- ucd=\"POS_ANG_DIST_GENERAL\" -->\n" +
	       "<DESCRIPTION>Distance from center (RAB1900=24 00.0, DEB1900=+67 17) at Epoch=J1900.0</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"_RAJ2000\" ucd=\"pos.eq.ra;meta.main\" ref=\"J2000\" datatype=\"char\" arraysize=\"7\" unit=\"&quot;h:m:s&quot;\"><!-- ucd=\"POS_EQ_RA_MAIN\" -->\n" +
	       "<DESCRIPTION>Right ascension (FK5) Equinox=J2000.0 Epoch=J1900. (computed by VizieR, not part of the original data)</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"_DEJ2000\" ucd=\"pos.eq.dec;meta.main\" ref=\"J2000\" datatype=\"char\" arraysize=\"6\" unit=\"&quot;d:m:s&quot;\"><!-- ucd=\"POS_EQ_DEC_MAIN\" -->\n" +
	       "<DESCRIPTION>Declination (FK5) Equinox=J2000.0 Epoch=J1900. (computed by VizieR, not part of the original data)</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"HD\" ucd=\"meta.id;meta.main\" datatype=\"int\" width=\"6\"><!-- ucd=\"ID_MAIN\" -->\n" +
	       "<DESCRIPTION>[1/272150]+ Henry Draper Catalog (HD) number</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"DM\" ucd=\"meta.id\" datatype=\"char\" arraysize=\"12*\"><!-- ucd=\"ID_ALTERNATIVE\" -->\n" +
	       "<DESCRIPTION>Durchmusterung identification (1)</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"RAB1900\" ucd=\"pos.eq.ra\" ref=\"B1900_1900.000\" datatype=\"char\" arraysize=\"7\" unit=\"&quot;h:m:s&quot;\"><!-- ucd=\"POS_EQ_RA\" -->\n" +
	       "<DESCRIPTION>Hours RA, equinox B1900, epoch 1900.0</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"DEB1900\" ucd=\"pos.eq.dec\" ref=\"B1900_1900.000\" datatype=\"char\" arraysize=\"6\" unit=\"&quot;d:m:s&quot;\"><!-- ucd=\"POS_EQ_DEC\" -->\n" +
	       "<DESCRIPTION>Degrees Dec, equinox B1900, epoch 1900.0</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"q_Ptm\" ucd=\"meta.code.qual\" datatype=\"unsignedByte\" width=\"1\"><!-- ucd=\"CODE_QUALITY\" -->\n" +
	       "<DESCRIPTION>[0/1]? Code for Ptm: 0 = measured, 1 = value inferred from Ptg and spectral type</DESCRIPTION>\n" +
	       "<VALUES null=\" \" />\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"Ptm\" ucd=\"phot.mag;em.opt.V\" datatype=\"float\" width=\"5\" precision=\"2\" unit=\"mag\"><!-- ucd=\"PHOT_PHG_V\" -->\n" +
	       "<DESCRIPTION>? Photovisual magnitude (2)</DESCRIPTION>\n" +
	       "<VALUES null=\" \" />\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"n_Ptm\" ucd=\"meta.note\" datatype=\"char\" arraysize=\"1\"><!-- ucd=\"NOTE\" -->\n" +
	       "<DESCRIPTION>[C] 'C' if Ptm is combined value with Ptg</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"q_Ptg\" ucd=\"meta.code.qual\" datatype=\"unsignedByte\" width=\"1\"><!-- ucd=\"CODE_QUALITY\" -->\n" +
	       "<DESCRIPTION>[0/1]? Code for Ptg: 0 = measured, 1 = value inferred from Ptm and spectral type</DESCRIPTION>\n" +
	       "<VALUES null=\" \" />\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"Ptg\" ucd=\"phot.mag;em.opt\" datatype=\"float\" width=\"5\" precision=\"2\" unit=\"mag\"><!-- ucd=\"PHOT_PHG_MAG\" -->\n" +
	       "<DESCRIPTION>? Photographic magnitude (2)</DESCRIPTION>\n" +
	       "<VALUES null=\" \" />\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"n_Ptg\" ucd=\"meta.note\" datatype=\"char\" arraysize=\"1\"><!-- ucd=\"NOTE\" -->\n" +
	       "<DESCRIPTION>[C] 'C' if Ptg is combined value for this entry and the following or preceding entry</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"SpT\" ucd=\"src.spType\" datatype=\"char\" arraysize=\"3\"><!-- ucd=\"SPECT_TYPE_GENERAL\" -->\n" +
	       "<DESCRIPTION>Spectral type spectral types P are generally nebulae)</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"Int\" ucd=\"phot.count;em.opt\" datatype=\"char\" arraysize=\"2\"><!-- ucd=\"PHOT_INTENSITY_ESTIMATED\" -->\n" +
	       "<DESCRIPTION>[ 0-9B] Photographic intensity of spectrum (3)</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"Rem\" ucd=\"meta.note\" datatype=\"char\" arraysize=\"1\"><!-- ucd=\"REMARKS\" -->\n" +
	       "<DESCRIPTION>[DEGMR*] Remarks, see note (4)</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"Simbad\" ucd=\"DATA_LINK\" datatype=\"char\" arraysize=\"6*\"><!-- ucd=\"(unassigned)\" -->\n" +
	       "<DESCRIPTION>ask the FireBrick Simbad data-base about this object</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	     "<FIELD name=\"Tycho\" ucd=\"meta.ref.url\" datatype=\"char\" arraysize=\"5*\"><!-- ucd=\"DATA_LINK\" -->\n" +
	       "<DESCRIPTION>Cross-identification with Tycho-2 (Cat. IV/25)</DESCRIPTION>\n" +
	     "</FIELD>\n" +
	 "<DATA>      <TABLEDATA>\n" +
	 "<TR><TD>0.2</TD><TD>00 05.2</TD><TD>+67 50</TD><TD>1</TD><TD>BD+67 1599</TD><TD>00 00.0</TD><TD></TD><TD>0</TD><TD/><TD> </TD><TD>1</TD><TD>8.70</TD><TD> </TD><TD>K0 </TD><TD> 3</TD><TD> </TD><TD>Simbad</TD><TD>Tycho</TD></TR>\n" +
	 "</TABLEDATA></DATA>\n" +
	 "</TABLE>\n" +
	 "</RESOURCE>\n" +
	 "</VOTABLE>\n";

	 static protected boolean test(Aladin aladin) {
	    boolean rep=true;
        rep &= test1(aladin,TEST_TSV_TITLE,TEST_TSV,TEST_TSV_RESULT);
        rep &= test1(aladin,TEST_VOTABLE_TITLE,TEST_VOTABLE,TEST_VOTABLE_RESULT);
        return rep;
	 }

//	 @Test
	 public void test() throws Exception {
        Aladin.NOGUI=Aladin.STANDALONE=true;
        Aladin aladin = new Aladin();
        Aladin.startInFrame(aladin);

        assert test(aladin);
	 }
}
