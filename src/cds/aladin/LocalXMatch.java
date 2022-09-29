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

/*
 * Created on 12 févr. 2004
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.util.Vector;

/** This class aims at providing a positionnal cross-match in Java
 * Algorithms have been adapted from Sebastien's C algorithms
 *
 * @author T. Boch, S .Derriere [CDS]
 * @version 0.1 : kickoff 12/02/2004
 */

public final class LocalXMatch {
    // singleton pour fonction xID
    static LocalXMatch localXMatch;


    /** Perform cross-id, having 2 array of coordinates
     *
     * @param array1
     * @param array2
     * @return XMatchResult[]
     */
    public static XMatchResult[] xID(String[] array1, String[] array2) {
        if( localXMatch==null ) localXMatch = new LocalXMatch();

        Vector resultVec = new Vector();
        int len1 = array1.length;
        int len2 = array2.length;
        XIDElem[] pos1 = new XIDElem[len1];
        XIDElem[] pos2 = new XIDElem[len2];

        for( int i=0; i<len1; i++ ) pos1[i] = localXMatch.new XIDElem(array1[i].toLowerCase(), i);
        for( int i=0; i<len2; i++ ) pos2[i] = localXMatch.new XIDElem(array2[i].toLowerCase(), i);

        MetaDataTree.sort(pos1, null, true);
        MetaDataTree.sort(pos2, null, true);

        int k = 0;
        for( int j=0; j<len1; j++) {
            while( k<len2 && pos1[j].str.compareTo(pos2[k].str) > 0 ) {
                k++;
            }
            while( k<len2 && pos1[j].str.compareTo(pos2[k].str) == 0 ) { /* pour tenir compte de plusieurs id similaires dans 2eme table*/
                resultVec.addElement(new XMatchResult(pos1[j].idx , pos2[k].idx, 0));
                //obj_item = (*env)->NewObject(env, cls_item, mid_newitem, pos1[j].idx , pos2[k].idx, 0);
                //(*env)->CallBooleanMethod(env, list, mid_add, obj_item);
                k++;
            }
        }
        XMatchResult[] resultArr = new XMatchResult[resultVec.size()];
        resultVec.copyInto(resultArr);
        resultVec = null;
        return resultArr;

    }

    /** Perform positionnal cross-match, having 2 arrays of coordinates
     *
     * @param array1 array of coordinates for first table array1[i][0] is RA, array1[i][1] is DEC
     * @param array2 array of coordinates for second table
     * @param seuil thresholds
     * @param fmtOut  output: 1 = best match <br>
     *                        2 = all matches <br>
     *                        4 = non-matches
     * @param flag1 ignore flags for data of the 1st table (ignore data i in table 1 if flag1[i]==true
     * @param flag2 ignore flags for data of the 2nd table
     * @return array of XMatchResult
     */
    // TODO : vérifier que les source flagués comme étant à ignorer ne sont pas prises en compte lors du no match !!
    public static XMatchResult[] xMatch(double[][] array1, double[][] array2,
                                        boolean[] flag1, boolean[] flag2,
                                        double[] seuil, int fmtOut) {
        Vector resultVec = new Vector();
        int len1 = array1.length;
        int len2 = array2.length;
        XMatchElem[] pos1 = new XMatchElem[len1];
        XMatchElem[] pos2 = new XMatchElem[len2];
        fillElem(pos1, array1);
        //for( int i=0; i<pos1.length ; i++) System.out.println("before : "+pos1[i]);
        sortPos(pos1);
        //for( int i=0; i<pos1.length ; i++) System.out.println("after : "+pos1[i]);

        fillElem(pos2, array2);
        sortPos(pos2);

        int k_inf = 0;
        int k_sup = 0;

        double dstBest, dst;
        int j,k;
        int kBest;
        XMatchResult result;

        for (j=0; j<len1; j++) {
            if( flag1[j] ) continue;

            while (pos2[k_inf].dec < pos1[j].dec-seuil[1]/3600. && k_inf < (len2-1) ) {
                k_inf++;
            }
            while ( pos2[k_sup].dec <= pos1[j].dec+seuil[1]/3600. && k_sup<(len2-1) ) {
                k_sup++;
            }
            //System.out.println("ksup : "+k_sup);
            kBest = -1;
            dstBest = seuil[1]+1.0; /* make it a non-match */
            for (k=k_inf; k<=k_sup; k++) {
                if( flag2[k] ) continue;
                //System.out.println(k+" "+j);
                dst = 3600.0*sphDst(pos1[j].ra, pos1[j].dec, pos2[k].ra, pos2[k].dec);
                if (dst <= seuil[1] && dst >= seuil[0]) {
                    if ((fmtOut & 2)>0) { /* print all matches */
                        result = new XMatchResult(pos1[j].idx , pos2[k].idx, dst);
                        resultVec.addElement(result);
                    }
                    if (dst < dstBest) {
                        kBest = k;
                        dstBest = dst;
                    }
                }
            } /* end for k ...*/
            if (kBest >=0 && (fmtOut & 1)>0 && (fmtOut & 2)==0) { /* print best, but only if not already done */
                result = new XMatchResult(pos1[j].idx, pos2[kBest].idx, dstBest);
                resultVec.addElement(result);
            }
            if (kBest < 0 && (fmtOut & 4)>0) { /* print non matches : distance is set to -1 */
                result = new XMatchResult(pos1[j].idx, kBest, -1.0);
                resultVec.addElement(result);
            }
        }
        XMatchResult[] resultArr = new XMatchResult[resultVec.size()];
        resultVec.copyInto(resultArr);
        resultVec = null;

        return resultArr;
    }

    /**************************************************************
	Positional cross match between two tables, taking into
	account the positional errors:
		'pos1' and 'pos2', are arrays of respective length len1 and len2
        We assume that pos1 and pos2 are ALREADY sorted in ascending dec
        seuil is given as n-sigmas limits
        fmtOut is the output: 1 = best match, 2 = all matches 4 = non-matches
	Returns: list of matches stored in 'result', and number of matches
	Major and minor axis in arcsec
	Position angle in degrees
	@param flag1 ignore flags for data of the 1st table (ignore data i in table 1 if flag1[i]==true
	@param flag2 ignore flags for data of the 2nd table
	*/
	static public XMatchResult[] xMatchEllipse (double[][] array1, double[][] array2,
			          double[] maj1, double[] min1, double[] pa1,
			          double[] maj2, double[] min2, double[] pa2, boolean[] flag1, boolean[] flag2,
					  double[] seuil, int fmtOut) {

        Vector resultVec = new Vector();
        int len1 = array1.length;
        int len2 = array2.length;
        XMatchEllipseElem[] pos1 = new XMatchEllipseElem[len1];
        XMatchEllipseElem[] pos2 = new XMatchEllipseElem[len2];
        fillElem(pos1, array1, maj1, min1, pa1);
        sortPos(pos1);
        fillElem(pos2, array2, maj2, min2, pa2);
        sortPos(pos2);

        int j, k, kBest, nb=0;
        double sig, span, dst, dstBest; /* span is in deg... distances are n-sigmas */
        int kInf=0, kSup=0, fkInf=0, fkSup=0;
        XMatchResult result = null;

        /* First we need to find 'span': the proper scanning window size (declination interval
           in catalogue 2 where we make the comparison)
           This is tricky because thresholds are given as n-sigmas, and each source
           can have different ellipse size...
           We adopt a conservative approach where 'span' is simply
           the largest major-axis found in any of the 2 catalogues times the upper
           sigma threshold: that way we never miss a possible association */

        sig = seuil[1]/3600.0;
        span = sig * ( Math.sqrt (  Math.pow(getMaxEllipse(maj1), 2)
                             + Math.pow(getMaxEllipse(maj2), 2) ));

        for (j=0; j<len1; j++) {
//        	UTILISER FLAG ICI
			if( flag1[j] ) continue;

        	/* fkInf and fkSup are the max window boundaries
               they grow monotonically with j */
        	while (pos2[fkInf].dec < pos1[j].dec-span && fkInf < len2-1) {
        		fkInf++;
        	}
        	while (pos2[fkSup].dec <= pos1[j].dec+span && fkSup < len2-1) {
        		fkSup++;
        	}
        	/* here we refine the window boundaries, the interval will be smaller,
        	   allowing faster computation */
        	for (kInf = fkInf; pos1[j].dec-pos2[kInf].dec > sig*Math.sqrt(pos1[j].maj*pos1[j].maj+pos2[kInf].maj*pos2[kInf].maj)
                 && kInf < fkSup; kInf++) ;
        	for (kSup = fkSup; pos2[kSup].dec-pos1[j].dec > sig*Math.sqrt(pos1[j].maj*pos1[j].maj+pos2[kSup].maj*pos2[kSup].maj)
                 && kSup > fkInf; kSup--) ;
//        	if (true) { System.out.println("j="+j+", f="+fkInf+"-"+fkSup+", k="+kInf+"-"+kSup); }


        	kBest = -1;
        	dstBest = seuil[1]+1.0; /* make it a non-match */
        	for (k=kInf; k<=kSup; k++) {
//        		UTLISER FLAG ICI
				if( flag2[k] ) continue;

        		/* petite ruse: on calcule la distance pour faire une selection grossiere en RA... */
        		if (sphDst(pos1[j].ra, pos1[j].dec, pos2[k].ra, pos2[k].dec) > span) continue;
        		/* et on passe au k suivant de la boucle for(k...) ... */
        		dst = nSigmaEllipse(pos1[j], pos2[k]);
        		if (dst <= seuil[1] && dst >= seuil[0]) {
        			if ((fmtOut & 2)>0) { /* print all matches */
        				result = new XMatchResult(pos1[j].idx, pos2[k].idx, dst);
//        				result.idx1 = pos1[j].idx;
//        				result.idx2 = pos2[k].idx;
//        				result.dist = dst;
        				resultVec.addElement(result);
        				nb++;
        			}
        			if (dst < dstBest) {
        				kBest = k;
        				dstBest = dst;
        			}
        		}
        	} /* end for k ...*/
        	if (kBest >=0 && (fmtOut & 1)>0 && (fmtOut & 2)==0) { /* print best, but only if not already done */
        		result = new XMatchResult(pos1[j].idx, pos2[kBest].idx, dstBest);
        		resultVec.addElement(result);
        		nb++;
        	}
        	if (kBest < 0 && (fmtOut & 4)>0) { /* print non matches : distance is set to -1 */
        		result = new XMatchResult(pos1[j].idx, kBest, -1.0);
        		resultVec.addElement(result);
        		nb++;
        	}
        }
        XMatchResult[] resultArr = new XMatchResult[resultVec.size()];
        resultVec.copyInto(resultArr);
        resultVec = null;
        return resultArr;
   }


	/**************************************************************
	 Computes distance between 2 ellipses expressed as a merged 'sigma'
	 Returns the value of the distance
	*/
	static private double nSigmaEllipse(XMatchEllipseElem tab1, XMatchEllipseElem tab2)
	{
	   double theta, dra, ddec, dcos, dst;
	   double siang, coang, sig, sig1, sig2;

	   dra = tab2.ra-tab1.ra;
	   ddec = Math.abs(tab2.dec-tab1.dec);
	   dcos = Math.cos(deg2rad(tab2.dec));

	   siang = dcos*Math.sin(deg2rad(dra));
	   coang = Math.sin(deg2rad(tab2.dec))*Math.cos(deg2rad(tab1.dec)) - dcos*Math.sin(deg2rad(tab1.dec))*Math.cos(deg2rad(dra));

	   /* orientation angle between the 2 sources... */
	   theta = (180.0/Math.PI)*Math.atan2(siang,coang);

	   coang = Math.cos(deg2rad(theta+tab1.pa))/tab1.maj;
	   siang = Math.sin(deg2rad(theta+tab1.pa))/tab1.min;
	   sig1 = 1.0/(coang*coang+siang*siang);
	   coang = Math.cos(deg2rad(theta+tab2.pa))/tab2.maj;
	   siang = Math.sin(deg2rad(theta+tab2.pa))/tab2.min;
	   sig2 = 1.0/(coang*coang+siang*siang);

	   dst = sphDst(tab1.ra, tab1.dec, tab2.ra, tab2.dec); /* dst in in dec degrees */
	   sig = 3600*dst/Math.sqrt(sig1+sig2); /* sig1 and sig2 are in arcsec... */
	   return sig;
	}


	/**************************************************************
	 Maximum major-axis in a pos array
	 Returns the value of maximum
	*/
	static private double getMaxEllipse(double[] tableau) {
	   double max=0.0;
	   int i;

	   for (i=0; i<tableau.length; i++) {
	      if( tableau[i]>max ) max = tableau[i];
	   }

	   return max;
	}

    /** fill a XMatchElem array from a table of coordinates */
    private static void fillElem(XMatchElem[] pos, double[][] array) {
        for( int i=0; i<array.length; i++ ) {
            pos[i] = new XMatchElem(array[i][0], array[i][1], i);
        }
    }

    /** fill a XMatchEllipseElem array from a table of coordinates,
     * a table of maj, a table of min, a table of pa */
    private static void fillElem(XMatchEllipseElem[] pos, double[][] array,
    		                     double[] maj, double[] min, double[] pa) {
        for( int i=0; i<array.length; i++ ) {
            pos[i] = new XMatchEllipseElem(array[i][0], array[i][1], i, maj[i], min[i], pa[i]);
        }
    }

    /**
     * Computes spherical distance between 2 pairs of coordinates,
     * given in decimal degrees.
     * Uses the haversine function (thanks to F-X Pineau for the suggestion)
     *
     * @param ra1  right ascension of 1st source
     * @param dec1 declination of 1st source
     * @param ra2  right ascension of 2nd source
     * @param dec2 declination of 2nd source
     *
     * @return distance in degrees between 2 sources
     */
    private static double sphDst(final double ra1, final double dec1,
                                           final double ra2, final double dec2) {
        final double ra1Rad  = deg2rad(ra1);
        final double dec1Rad = deg2rad(dec1);
        final double ra2Rad  = deg2rad(ra2);
        final double dec2Rad = deg2rad(dec2);

        double d = Math.pow(Math.sin((dec1Rad-dec2Rad)/2), 2);
        d += Math.pow(Math.sin((ra1Rad-ra2Rad)/2), 2)*Math.cos(dec1Rad)*Math.cos(dec2Rad);

        return rad2deg(2*Math.asin(Math.sqrt(d)));
    }

    static private double deg2rad(double angle) {
        return angle*Math.PI/180.0;
    }

    static private double rad2deg(double angle) {
        return angle*180.0/Math.PI;
    }


    /**************************************************************
      Sorts an array of pos by ascending declination
    */
    static private void sortPos(XMatchElem[] rec) {

        int n = rec.length;

        double[] dec = new double[n];

        for (int j=0; j<n; j++)
            dec[j] = rec[j].dec;

        int[] iwksp = new int[n];
        XMatchElem[] wksp = new XMatchElem[n];
        dindexx(dec, iwksp);

        for (int j=0; j<n; j++)
            wksp[j] = rec[j];

        for (int j=0; j<n; j++) {
            rec[j] = wksp[iwksp[j]]; /* rec is now ordered */
            rec[j].idx = iwksp[j]; /* we keep track of the original index that way */
        }


    }

    /**************************************************************
      NR function for efficient sorting of an array of double
      Sort is in ASCENDING order
    */
    static private void dindexx(double[] arr, int[] indx) {
        int n = arr.length;
        int indxt,ir=n-1,itemp,k,l=0;
        int jstack=0;
        double a;
        int i,j;

        int NSTACK = 50;
        int M = 7;
        int[] istack= new int[NSTACK];
        for (j=0;j<n;j++) indx[j]=j;

        for(;;) {
            if (ir-l < M) {
                //for (j=l;j<ir;j++) {
                for (j=l+1;j<=ir;j++) {
                    indxt=indx[j];
                    a=arr[indxt];
                    //for (i=j;i>=l;i--) {
                    for (i=j-1;i>=l;i--) {
                        if (arr[indx[i]] <= a) break;
                        indx[i+1]=indx[i];
                    }
                    indx[i+1]=indxt;
                }
                if (jstack == 0) break;
                ir=istack[jstack--];
                l=istack[jstack--];
            } else {
                k=(l+ir) >> 1;
                swap(indx, k, l+1);
                if(arr[indx[l]] > arr[indx[ir]]) {
                    swap(indx, l, ir);
                }
                if (arr[indx[l+1]] > arr[indx[ir]]) {
                    swap(indx, l+1,ir);
                }
                if (arr[indx[l]] > arr[indx[l+1]]) {
                    swap(indx, l, l+1);
                }
                i=l+1;
                j=ir;
                indxt=indx[l+1];
                a=arr[indxt];
                for(;;) {
                    do i++; while (arr[indx[i]] < a);
                    do j--; while (arr[indx[j]] > a);
                    if (j < i) break;
                    swap(indx, i, j);
                }
                indx[l+1]=indx[j];
                indx[j]=indxt;
                jstack += 2;
                if (jstack > NSTACK) System.out.println("NSTACK too small in indexx");
                if (ir-i+1 >= j-l) {
                    istack[jstack]=ir;
                    istack[jstack-1]=i;
                    ir=j-1;
                } else {
                    istack[jstack]=j-1;
                    istack[jstack-1]=l;
                    l=i;
                }
            }
        }
    }

    private static void swap(int[] arr, int idx1, int idx2) {
        int tmp = arr[idx1];
        arr[idx1] = arr[idx2];
        arr[idx2] = tmp;
    }

    class XIDElem {
        String str;
        int idx;

        XIDElem(String str, int idx) {
            super();
            this.str = str;
            this.idx = idx;
        }

        public String toString() {
            return str;
        }
    }

    /*
    public static void main(String[] args) {
        double[] test = {1,56,84,-6,89,45,72,13,5,9,1.5,0.2,9.7};
        int[] idx = new int[test.length];
        dindexx(test,idx);
        for( int i=0; i<idx.length; i++ ) System.out.println(idx[i]);
    }
    */


}
