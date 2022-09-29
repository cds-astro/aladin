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

/* Class CDSXMatch
 * Permet le cross-match entre 2 catalogues, sur la base du cross-match développé au CDS
 * par Sébastien Derrière
 *
 */


package cds.aladin;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import cds.astro.Astrocoo;
import cds.astro.Unit;
import cds.tools.Util;
import cds.xml.Field;

/**
 * @author T. Boch [CDS]
 */
public final class CDSXMatch /*implements XMatchInterface*/ {

    static final int BESTMATCH = 1;
    static final int ALLMATCH = 2;
    static final int NOMATCH = 4;

    static final int POSXMATCH = 1;
    static final int JOIN = 2;
    static final int POSXMATCH_ELLIPSES = 3;

    private ColFilter colFilter;

    private Aladin aladin;


    /** Constructeur
     *
     */
    protected CDSXMatch(Aladin aladin) {
        this.aladin = aladin;
    }


    /** Merge rows on a given field
     * @param p1
     * @param p2
     * @param index1 column index in p1
     * @param index2 column index in p2
     * @param aladin reference to Aladin
     */
    public void xID(Plan p1, Plan p2, String label,int index1, int index2, Aladin aladin) {
        double begin = System.currentTimeMillis();

        // création dès le début du calcul du plan résultat
        PlanCatalog pc = initPlaneCreation(p1,label);

        String[] array1 = new String[p1.getCounts()];
        fillXIDArray(p1.pcat, array1, index1);

        String[] array2 = new String[p2.getCounts()];
        fillXIDArray(p2.pcat, array2, index2);

        double beginXmatch = System.currentTimeMillis();
        Aladin.trace(3, "Total time for extracting fields : "+(beginXmatch-begin));
        XMatchResult[] result = LocalXMatch.xID(array1, array2);
        double end = System.currentTimeMillis();
        Aladin.trace(3, "Total time for xid : "+(end-beginXmatch));
        fillResultPlane(pc, result, p1, p2, null, null, JOIN);

        // log
        aladin.log("xmatch", "xid");
    }

    private void fillXIDArray(Pcat pcat, String[] array, int index) {
        Source o;
        Iterator<Obj> it = pcat.iterator();
        for( int i=0; it.hasNext(); i++ ) {
            o = (Source)it.next();
            array[i] = o.getValue(index);
        }
    }

    public void posXMatch(Plan p1, Plan p2, String label,int[] coordTab1, int[] coordTab2,
            double[] seuils, int method, Aladin aladin) {
    	posXMatch(p1,p2,label,coordTab1,coordTab2,seuils,method,aladin,false);
    }

    /** Cross-match positionnel (effectué en local)
     *
     * @param p1
     * @param p2
     * @param seuils
     * @param method
     * @param aladin
     */
    public void posXMatch(Plan p1, Plan p2, String label,int[] coordTab1, int[] coordTab2,
                        double[] seuils, int method, Aladin aladin, boolean fromScript) {

        double begin = System.currentTimeMillis();



        if( coordTab1==null ) {
            coordTab1 = findCoord(p1);
            if( coordTab1==null ) {
                Aladin.error(Aladin.chaine.getString("NOCOOR")+" "+p1.label);
                return;
            }
        }
        if( coordTab2==null ) {
            coordTab2 = findCoord(p2);
            if( coordTab2==null ) {
                Aladin.error(Aladin.chaine.getString("NOCOOR")+" "+p2.label);
                return;
            }
        }

        // création dès le début du calcul du plan résultat
        PlanCatalog pc = initPlaneCreation(p1,label);

        double[][] array1 = new double[p1.getCounts()][2];
        boolean[] ignoreFlag1 = new boolean[p1.getCounts()];
        fillXMatchArray(p1.pcat, array1, coordTab1, ignoreFlag1);

        double[][] array2 = new double[p2.getCounts()][2];
        boolean[] ignoreFlag2 = new boolean[p2.getCounts()];
        fillXMatchArray(p2.pcat, array2, coordTab2, ignoreFlag2);

        if( !fromScript ) aladin.console.printCommand("xmatch "+Tok.quote(p1.label)+" "+Tok.quote(p2.label)+" "+seuils[1]);
        double beginXmatch = System.currentTimeMillis();
        Aladin.trace(3, "Total time for extracting coordinates : "+(beginXmatch-begin));
        // TODO : ajouter la possibilité d'interrompre un xmatch en cours en effaçant le plan résultat
        XMatchResult[] result = LocalXMatch.xMatch(array1, array2, ignoreFlag1, ignoreFlag2, seuils, method);
        double end = System.currentTimeMillis();
        Aladin.trace(3, "Total time for xmatch : "+(end-beginXmatch));
        fillResultPlane(pc, result, p1, p2, coordTab1, array1, POSXMATCH);
        Aladin.trace(3, "Total time for creation of the plane : "+(System.currentTimeMillis()-end));

        // log
        aladin.log("xmatch", "positional");
    }

    /** Cross-match positionnel avec ellipses
     *
     * @param p1
     * @param p2
     * @param seuils
     * @param method
     * @param aladin
     */
    public void posXMatchEllipses(Plan p1, Plan p2, String label,int[] coordTab1, int[] coordTab2,
                        int[] paramEllipses1, int[] paramEllipses2, double nbSigmaMin, double nbSigmaMax, int method, Aladin aladin) {

        double begin = System.currentTimeMillis();



        if( coordTab1==null ) {
            coordTab1 = findCoord(p1);
            if( coordTab1==null ) {
                Aladin.error("Coordinates columns not found for plane "+p1.label);
                return;
            }
        }
        if( coordTab2==null ) {
            coordTab2 = findCoord(p2);
            if( coordTab2==null ) {
                Aladin.error("Coordinates columns not found for plane "+p2.label);
                return;
            }
        }

        // création dès le début du calcul du plan résultat
        PlanCatalog pc = initPlaneCreation(p1,label);

        double[][] array1 = new double[p1.getCounts()][2];
        boolean[] ignoreFlag1 = new boolean[p1.getCounts()];
        fillXMatchArray(p1.pcat, array1, coordTab1, ignoreFlag1);

        double[][] array2 = new double[p2.getCounts()][2];
        boolean[] ignoreFlag2 = new boolean[p2.getCounts()];
        fillXMatchArray(p2.pcat, array2, coordTab2, ignoreFlag2);

        double[] maj1 = new double[p1.getCounts()];
        double[] min1 = new double[p1.getCounts()];
        double[] pa1 = new double[p1.getCounts()];

        fillEllipsesParamArray(p1.pcat, maj1, min1, pa1, paramEllipses1, ignoreFlag1);

        double[] maj2 = new double[p2.getCounts()];
        double[] min2 = new double[p2.getCounts()];
        double[] pa2 = new double[p2.getCounts()];

        fillEllipsesParamArray(p2.pcat, maj2, min2, pa2, paramEllipses2, ignoreFlag2);

        // TODO : faire la commande script correspondante !!
//        if( !fromScript ) aladin.pad.setCmd("xmatch "+Tok.quote(p1.label)+" "+Tok.quote(p2.label)+" "+seuils[1]);
        double beginXmatch = System.currentTimeMillis();
        Aladin.trace(3, "Total time for extracting coordinates : "+(beginXmatch-begin));
        // TODO : faut il permettre de passer le minimum pour nbSigma ?
        XMatchResult[] result = LocalXMatch.xMatchEllipse(array1, array2, maj1, min1, pa1, maj2, min2, pa2, ignoreFlag1, ignoreFlag2, new double[] {nbSigmaMin,nbSigmaMax}, method);
        double end = System.currentTimeMillis();
        Aladin.trace(3, "Total time for ellipses xmatch : "+(end-beginXmatch));
        fillResultPlane(pc, result, p1, p2, coordTab1, array1, POSXMATCH_ELLIPSES);

        // log
        aladin.log("xmatch", "ellipses");

    }

    /**
     *
     * @param pcat
     * @param array
     * @param coordTab
     * @param flagIgnore
     */
    static private void fillXMatchArray(Pcat pcat, double[][] array, int[] coordTab, boolean[] flagIgnore) {
        Source o;
        String content;
        Iterator<Obj> it = pcat.iterator();
        for( int i=0; it.hasNext(); i++) {
            o = (Source)it.next();
            // not needed
//            flagIgnore[i] = false;
            try {
                // ra
                content = o.getValue(coordTab[0]);
                if( isSexa(content) ) content = sexa2Deg(content, true);
                //array[i][0] = Double.parseDouble(content);
                array[i][0] = Double.valueOf(content).doubleValue();
                // dec
                content = o.getValue(coordTab[1]);
                if( isSexa(content) ) content = sexa2Deg(content, false);
                //array[i][1] = Double.parseDouble(content);
                array[i][1] = Double.valueOf(content).doubleValue();
            }
            catch(NumberFormatException e) {
                e.printStackTrace();
                // ignore this source
                flagIgnore[i] = true;
//                array[i][0] = array[i][1] = 0.0;
            }
            catch(NullPointerException npe) {
                npe.printStackTrace();
                // ignore this source
                flagIgnore[i] = true;
            }
        }
    }

    static private void fillEllipsesParamArray(Pcat pcat, double[] maj, double[] min, double[] pa, int[] ellipsesParamIdx, boolean[] flagIgnore) {
        Source o;
        String content;

        // approche utilisée pour la conversion des unités :
        // on essaye de convertir les 3 param. en degrés
        // si c'est impossible, on considère que la valeur brute est en degrés ...
        Source s = (Source)pcat.iterator().next();
        double multFactMaj, multFactMin, multFactPa; // facteur multiplicatif pour maj, min et pa
        Unit uDeg, uArcSec, uMaj, uMin, uPa;
        Unit uDegTemplate=null;
        Unit uArcSecTemplate =null;
        uMaj = uDeg = null;
        try {
        	uDegTemplate = new Unit("1 deg");
        	uArcSecTemplate = new Unit("1 arcsec");

        }
        catch(ParseException e) {} // l'exception ne devrait jamais etre levée !


//        A PRENDRE EN COMPTE !!
        // par défaut, grand axe en arcsec
        try {
        	uArcSec = new Unit(uArcSecTemplate);
        	uMaj = new Unit("1 "+s.getUnit(ellipsesParamIdx[0]));
        	uArcSec.convertFrom(uMaj);
        	multFactMaj = uArcSec.getValue();
        }
		catch(java.text.ParseException e) {multFactMaj=1.0;}
		catch(ArithmeticException e2) {multFactMaj=1.0;}
        System.out.println("facteur pour MAJ : "+multFactMaj);

        // par défaut, petit axe en arcsec
        try {
        	uArcSec = new Unit(uArcSecTemplate);
        	uMin = new Unit("1 "+s.getUnit(ellipsesParamIdx[1]));
        	uArcSec.convertFrom(uMin);
        	multFactMin = uArcSec.getValue();
        }
		catch(java.text.ParseException e) {multFactMin=1.0;}
		catch(ArithmeticException e2) {multFactMin=1.0;}
        System.out.println("facteur pour MIN : "+multFactMin);

        try {
        	uDeg = new Unit(uDegTemplate);
        	uPa = new Unit("1 "+s.getUnit(ellipsesParamIdx[2]));
        	uDeg.convertFrom(uPa);
        	multFactPa = uDeg.getValue();
        }
		catch(java.text.ParseException e) {multFactPa=1.0;}
		catch(ArithmeticException e2) {multFactPa=1.0;}
		System.out.println("facteur pour PA : "+multFactPa);

//        System.out.println("nb_o "+pcat.nb_o);
		Iterator<Obj> it = pcat.iterator();
        for( int i=0; it.hasNext(); i++) {
            o = (Source)it.next();
            // not needed
//            flagIgnore[i] = false;
            try {
            	// TODO : conversion d'unités pour avoir tout en degrés !!
                // major axis (en tenant compte du facteur multiplicatif)
                content = o.getValue(ellipsesParamIdx[0]);
//                System.out.println(ellipsesParamIdx[0]);
                maj[i] = multFactMaj * Double.valueOf(content).doubleValue();
                // minor axis (en tenant compte du facteur multiplicatif)
                try {
                	content = o.getValue(ellipsesParamIdx[1]);
                	min[i] = multFactMin * Double.valueOf(content).doubleValue();
                }
                // if this happens, we take the same value as the major axis !
                catch(NumberFormatException e) {
                	min[i] = maj[i];
                }
                // position angle (en tenant compte du facteur multiplicatif)
                try {
                    content = o.getValue(ellipsesParamIdx[2]);
                    pa[i] = multFactPa * Double.valueOf(content).doubleValue();
                }
                catch(NumberFormatException e) {pa[i]=0.0;}
            }
            catch(NumberFormatException e) {
            	// on flague cette source comme étant à ignorer
            	flagIgnore[i] = true;
            	maj[i]=min[i]=pa[i]= 0.0;
            }
        }
//        System.out.println("nb_o "+pcat.nb_o);
    }

    static int xMatchNb = 0;

    /** Converts a sexa part of a coordinate (either RA or DEC part) into degrees */
    static private Astrocoo frame = new Astrocoo();
    static private String sexa2Deg(String sexa, boolean isRA) {
        String str;
        str = isRA?sexa+" +0 0 0.0":"0 0 0.0 "+sexa;
        try {
            frame.set(str);
        }
        catch(Exception e) {return "0";}
        return isRA?frame.getLon()+"":frame.getLat()+"";
    }

    /** Return true if the coord is in Sexagesimal format */
   static private boolean isSexa(String s) {
      char a[] = s.toCharArray();
      int nbb;      // Nombre de separateurs
      int i;

      for( i=nbb=0; i<a.length; i++ ) {
			if (a[i] == ':' || a[i] == ' ' || a[i] == '\t')
				nbb++;

			//if( nbb>1 ) return true;
			// correction thomas, 20/12/04
			if (nbb >= 1) return true;
      }
      return false;
   }

	/** Réécrit les UCDs de la légende selon le principe suivant :
	    s'il y a 2 UCDs avec l'attribut main, le premier le conserve, le second le perd
	*/
	private void ucdRewriting(Legende leg) {
		Field[] f = leg.field;
		// v contiendra les UCD avec l'attribut main
		Vector<String> v = new Vector<>();

	   	String ucd, ucdOrg;
	   	int k;
	   	for( int i=0; i<f.length ; i++ ) {
	   		ucdOrg = f[i].ucd;
	   		if( ucdOrg==null ) continue;
	   		ucd = ucdOrg.toLowerCase();
	   		if( (k=ucd.indexOf("_main"))>=0 || (k=ucd.indexOf(";meta.main"))>=0 ) {
	   			if( !v.contains(ucd) ) v.addElement(ucd);
	   			else f[i].ucd = ucdOrg.substring(0,k);
	   		}
	   	}
	}

   private static final String UCD_DIST = "POS_ANG_DIST_GENERAL";
   private static final String UCD_SIGMA = "stat.stdev";
   private static final String DEFAULT_PREFIX_T1 = "";
   private static final String DEFAULT_PREFIX_T2 = "";
   private static final String DEFAULT_SUFFIX_T1 = "_tab1";
   private static final String DEFAULT_SUFFIX_T2 = "_tab2";

	private PlanCatalog initPlaneCreation(Plan p1,String label) {
		xMatchNb++;
		int n = aladin.calque.newPlanCatalog();
		PlanCatalog pc = (PlanCatalog)aladin.calque.plan[n];
		// indispensable, sinon on ne peut plus rien charger comme données !!
		pc.param = "xmatch";
		pc.setLabel(label!=null ? label:"XMatch");
		if( p1.objet!=null ) pc.objet = p1.objet;
		else pc.objet = "";
		pc.body = p1.body;
		pc.flagOk = false;
		aladin.calque.select.repaint();

		return pc;
	}

   /** Fills the result plane, result from a cross-match, and finalizes it (change its status)
    *
    * @param pc reference to the plane to fill
    * @param result array result of the cross-match
    * @param p1
    * @param p2
    * @param coordTab1
    * @param array1
    */
	private void fillResultPlane(PlanCatalog pc, XMatchResult[] result, Plan p1, Plan p2,
                                   int[] coordTab1, double[][] array, int xmatchType) {
		boolean ellXMatch = (xmatchType==POSXMATCH_ELLIPSES);

		Obj[] o1 = p1.pcat.getObj();
		Obj[] o2 = p2.pcat.getObj();
		int idx1, idx2;
		Source s1, s2;
		double dist;
		String newInfo;
		Legende leg = null;
		Hashtable<String, Legende> legMemory = new Hashtable<>(); // mémoire des légendes créées
		Legende saveLeg1, saveLeg2;
		saveLeg1 = saveLeg2 = null;
		Hashtable<Legende, ArrayList<Source>> legToSources = new Hashtable<>(); // correspondance légende --> sources résultats
		ArrayList<Source> tmp;
		Source srcResult = null;
		for( int i=0; i<result.length; i++ ) {
			idx1 = result[i].idx1;
			idx2 = result[i].idx2;
			dist = result[i].dist;

			s1 = (Source)o1[idx1];
			s2 = (dist==-1.0)?null:(Source)o2[idx2];

			// doit-on créer une nouvelle légende ?
			if( saveLeg1==null || saveLeg2==null || s1.getLeg()!=saveLeg1 || (s2!=null && s2.getLeg()!=saveLeg2)  ) {
				saveLeg1 = s1.getLeg();
				saveLeg2 = s2==null?null:s2.getLeg();

				String id = s1.getLeg().toString()+(s2==null?"null":s2.getLeg().toString());
				if( (leg=legMemory.get(id))==null ) {

					leg = createLeg(xmatchType, ellXMatch, s1, s2, coordTab1);
					legMemory.put(id, leg);
				}
			}

			//System.out.println("***"+((Source)o1[idx1]).info+"***");
			if( xmatchType==JOIN ) newInfo = "<&_getReadMe "+pc.label+" >"+"\t"+getOnlyInfo((Source)o1[idx1], true)+"\t"+getOnlyInfo((Source)o2[idx2], false);
			// non-match
			else if( dist==-1.0 ) newInfo = "<&_getReadMe "+pc.label+" >"+"\t"+Util.round(dist,4)+"\t"+getOnlyInfo((Source)o1[idx1], true);
			else newInfo = "<&_getReadMe "+pc.label+" >"+"\t"+Util.round(dist,4)+"\t"+getOnlyInfo((Source)o1[idx1], true)+"\t"+getOnlyInfo((Source)o2[idx2], false);

			if( array==null ) {
				Source s = (Source)o1[idx1];
				srcResult = new Source(pc, s.raj, s.dej, "", newInfo, leg);
			}
			else {
				srcResult = new Source(pc, array[idx1][0], array[idx1][1], "", newInfo, leg);
			}

			// on trie la source nouvellement créée selon la légende associée
			tmp = legToSources.get(leg);
			if( tmp==null ) legToSources.put(leg, tmp = new ArrayList<>());
			tmp.add(srcResult);

		}

		// ajout des sources créées dans le plan catalogue, regroupées par légende (pour plans multi-tables)
		Enumeration<ArrayList<Source>> enumTables = legToSources.elements();
		while( enumTables.hasMoreElements() ) {
			tmp = enumTables.nextElement();
			Iterator<Source> it = tmp.iterator();
			while( it.hasNext() ) pc.pcat.setObjetFast(it.next());
		}

		// finalisation de la création du plan
		pc.setSourceType(Source.getDefaultType(result.length));
		pc.setActivated(true);
		pc.flagOk = true;
		aladin.calque.select.repaint();
		aladin.view.repaintAll();
		// maj du popup d'aide pour les filtres
		FilterProperties.notifyNewPlan();

	}

	private Legende createLeg(int xmatchType, boolean ellXMatch, Source s1, Source s2, int[] coordTab1) {
		Vector<Field> v = new Vector<>();

		// ajout du champ distance (sauf en cas de JOIN)
		if( xmatchType!=JOIN ) {
			Field distField = ellXMatch?new Field("NSigma"):new Field("dist");
			distField.ucd = ellXMatch?UCD_SIGMA:UCD_DIST;
			distField.datatype = "D";
			distField.description = ellXMatch?"Distance in sigmas":"Distance between 2 cross-matched sources";
			distField.unit = ellXMatch?"---":"arcsec";
			distField.width = "7";
			distField.computeColumnSize();

			v.addElement(distField);
		}

		Source o = s1;
		String prefix, suffix;
		prefix = colFilter==null?DEFAULT_PREFIX_T1:colFilter.prefix1;
		suffix = colFilter==null?DEFAULT_SUFFIX_T1:colFilter.suffix1;
		addField(true, o, v, prefix, suffix, coordTab1, true);
		o = s2;
		prefix = colFilter==null?DEFAULT_PREFIX_T2:colFilter.prefix2;
		suffix = colFilter==null?DEFAULT_SUFFIX_T2:colFilter.suffix2;
		if( o!=null ) addField(false, o, v, prefix, suffix, null, false);
		Legende leg = new Legende(v);

		ucdRewriting(leg);

		return leg;
	}

	int[] idxColToKeep1;
	int[] idxColToKeep2;

	private String getOnlyInfo(Source s, boolean tab1) {
		String ret;
		int idx = s.info.indexOf('\t');
		if( idx>=0 ) ret = s.info.substring(idx+1);
		else ret = s.info;


		if( colFilter!=null ) {
			int[] idxColToKeep = tab1?idxColToKeep1:idxColToKeep2;
			String[] values = cds.tools.Util.split(ret, "\t", ':', ':');
			ret = "";
			for( int i=0; i<idxColToKeep.length; i++ ) {
				ret += values[idxColToKeep[i]];
				if( i<idxColToKeep.length-1 ) ret += "\t";
       	   }
		}
		return ret;
	}

	/** ajout des champs de la source s */
   private void addField(boolean tab1, Source s, Vector<Field> v, String prefix, String suffix, int[] coordTab, boolean coo) {
       if( tab1 ) idxColToKeep1 = null;
       else idxColToKeep2 = null;

       if( colFilter!=null ) {
           if( tab1 ) idxColToKeep1 = new int[colFilter.fieldTab1.length];
           else idxColToKeep2 = new int[colFilter.fieldTab2.length];
       }

       Field f, curF;
       int k =0;
       for( int i=0; i<s.getLeg().field.length; i++ ) {
           curF = s.getLeg().field[i];

           // doit on conserver le field courant ?
           boolean keep = true;
           if( colFilter!=null ) {
               if( tab1 ) keep = colFilter.inTab1(curF.name);
               else keep = colFilter.inTab2(curF.name);

               if( keep ) {
                   if( tab1 ) idxColToKeep1[k++] = i;
                   else idxColToKeep2[k++] = i;
               }
           }
           if( !keep ) continue;

           f = new Field("");
           f.ID = curF.ID;
           f.name = prefix+curF.name+suffix;
           f.description = curF.description;
           f.title = prefix+curF.title+suffix;
           f.type = curF.type;
           f.unit = curF.unit;
           if( colFilter!=null ) f.ucd = tab1?colFilter.getUcdTab1(curF.name):colFilter.getUcdTab2(curF.name);
           else f.ucd = curF.ucd;
           f.datatype = curF.datatype;
           f.width = curF.width;
           f.nullValue = curF.nullValue;
           f.arraysize = curF.arraysize;
           f.columnSize = curF.columnSize;
           f.precision = curF.precision;
           f.href = curF.href;
           f.gref = curF.gref;
           f.refText = curF.refText;
           f.refValue = curF.refValue;
//           if( coo && coordTab!=null && (i==coordTab[0] || i==coordTab[1]) ) f.coo = true;
           if( coo && coordTab!=null && i==coordTab[0] ) f.coo = Field.RA;
           if( coo && coordTab!=null && i==coordTab[1] ) f.coo = Field.DE;
           v.addElement(f);
       } // end of loop on different fields
   }

    static final String UCD_RA = "POS_EQ_RA";
    static final String UCD_DEC = "POS_EQ_DEC";
    static final String UCD1PLUS_RA = "pos.eq.ra";
    static final String UCD1PLUS_DEC = "pos.eq.dec";
    /** Find coordinates columns indexes on the basis of UCDs (or column name if no appropriate UCDs were found)
     *
     * @param p plane for which we search coordinates
     * @return array of index of coordinates, <i>null</i> if coordinates were not found
     */
    // TODO : privilégier le meta.main aux autres UCDs candidats !!!
    static protected int[] findCoord(Plan p) {
//       Field[] fields = ((Source)p.pcat.o[0]).leg.field;
        Field[] fields = p.getFirstLegende().field;
        boolean foundRA=false;
        boolean foundDEC=false;
        int indexRA = -1;
        int indexDEC = -1;
        int indexRAByName = -1;
        int indexDECByName = -1;

        String ucd, ucdLC, name;
        for( int i=0; i<fields.length && (!foundRA || !foundDEC); i++ ) {
            ucd = fields[i].ucd;
            ucdLC = ucd==null?null:ucd.toLowerCase();

            if( !foundRA && ucd!=null && ( ucdLC.startsWith(UCD1PLUS_RA) || ucd.startsWith(UCD_RA) ) ) {
                foundRA = true;
                indexRA = i;
            }
            else if( !foundDEC && ucd!=null && ( ucdLC.startsWith(UCD1PLUS_DEC) || ucd.startsWith(UCD_DEC) ) ) {
                foundDEC = true;
                indexDEC = i;
            }

            name = fields[i].name;
            if( name==null ) continue;
            name = name.toLowerCase();
            if( name.startsWith("_") ) name = name.substring(1);

            // TODO : on pourrait pour homogénéiser tout ça se servir de :
            // cds.xml.TableParser.raName/deName
            if( indexRAByName<0 && ( name.startsWith("ra") || name.startsWith("alpha") ) ) indexRAByName = i;
            else if( indexDECByName<0 && ( name.startsWith("de") || name.startsWith("delta") ) ) indexDECByName = i;

        }

        // si possible, on détecte sur la base des UCDs
        if( foundRA && foundDEC )
        	return new int[] {indexRA, indexDEC};
        // sinon, on se rabat sur les noms de colonne
        else if( indexRAByName>=0 && indexDECByName>=0)
        	return new int[] {indexRAByName, indexDECByName};
        // sinon : on retourne null
        else return null;
    }

    /**
     * Retourne l'index du champ ayant un des UCDs passé en paramètre
     * @param p plan catalogue pour lequel on recherche un champ
     * @param ucds tableau d'UCD recherchés, classé par ordre de préférence
     * @return l'indice correspondant au champ trouvé, -1 si aucun champ correspondant trouvé
     */
    static protected int findIdx(Plan p, String[] ucds) {
    	if( ucds==null || ucds.length==0 ) return -1;
//        Field[] fields = ((Source)p.pcat.o[0]).leg.field;
        Field[] fields = p.getFirstLegende().field;
    	int[] indexes = new int[ucds.length];
    	int nbMatch=0; // nb d'UCDs trouvé
    	String ucdLC;

    	// conversion en lowercase des UCDs recherchés
    	for( int i=0; i<ucds.length; i++ ) {
    		ucds[i] = ucds[i].toLowerCase();
    		indexes[i] = -1;
    	}

    	for( int i=0; i<fields.length && indexes[0]<0 && nbMatch<ucds.length ; i++ ) {
    		ucdLC = fields[i].ucd;
    		if( ucdLC==null ) continue;
    		ucdLC = ucdLC.toLowerCase();


    		for( int j=0; j<ucds.length; j++ ) {
    			if( indexes[j]<0 && ucds[j].equals(ucdLC) ) {
    				indexes[j] = i;
    				nbMatch++;
    				break;
    			}
    		}
    	}

    	// on recherche le premier indice non strictement négatif, ie le meilleur match
    	for( int i=0; i<indexes.length; i++ ) {
    		if( indexes[i]>=0 ) return indexes[i];
    	}

    	return -1;

    }

    /**
     * Sets a ColFilter for this cross-match (to choose the columns to keep)
     * @param colFilter the ColFilter to set
     */
    protected void setColFilter(ColFilter colFilter) {
    	this.colFilter = colFilter;
    }

}
