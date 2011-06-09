//
// Licenced under GPLv2, see licence.txt
// (c) K.M. Gorski, Nickolai Kuropatkin, Jan Kotek,
//
package org.asterope.healpix;

import cds.tools.pixtools.CDSHealpix;

/**
 *
 *
 * bit manipulation class derived from Healpix
 * fortran90 program.
 * 
 *  @author N Kuropatkin
 */
public final class BitManipulation {
	/**
	 * magic1 odd bits set constant
	 */
	public static long magic1 = 89478485; //  101010101010101010101010101 LSB
	/**
	 * magic2 - even bits set constant
	 */
	public static long magic2 = 178956970; //1010101010101010101010101010 MSB


	private BitManipulation() {}
	/**
	 * swaps low and high bits in the word i
	 * @param i  integer input word
	 * @return  int a word with odd and even bits interchanged
	 */
	 static public long swapLSBMSB(long i) {
		long res = 0;
		long lsb = (i & magic1);
		long msb = (i & magic2);
		res = msb/2 + lsb*2;
		return res;
	}
	 /**
	  * returns NOT i with even and odd bit positions interchanged
	  * @param i  int input word
	  * @return  int NOT (i with LSBMSB)
	  */
	 static public long invswapLSBMSB(long i) {
	 	long res = 0;
	 	long lsb = (i & magic1);
	 	long msb = (i & magic2);
	 	res = ~(msb/2+lsb*2);
	 	return res;
	 }
	 /**
	  * returns i with even bits inverted
	  * @param i int input word
	  * @return int word with modified bits
	  */
	 static public long invLSB(long i) {
	 	long res = 0;
	 	res = (i ^ magic1); // returns exclusive OR with odd bits
	 	return res;
	 }
	 /**
	  * returns i with odd bits inverted
	  * @param i int input word
	  * @return int word with modified bits
	  */
	 static public long invMSB(long i) {
	 	long res = 0;
	 	res = (i ^ magic2);
	 	return res;
	 }

    static public  int MODULO(int a, int b){
        return a%b;
    }

    public static long MODULO(long a, long b){
        return a%b;
    }

	 /**
	  * simulates behaviour of fortran90 MODULO function
	  * @param a  double
	  * @param b  double
	  * @return  double MODULO
	  */
	 public static double MODULO(double a, double b) {
	 	double res = 0.;
	 	long k = 0;
	 	if (a>0.) {
	 		if (b>0.) {
	 			k = (long) (a/b);
	 			res = a - k*b;
	 			return res;
	 		}
	 		if (b<0.) {
	 			k = (long)Math.rint(a/b);
	 			res = a - k*b;
	 			return res;
	 		}
	 	}
	 	if (a<=0.) {
	 		if (b<=0.) {
	 			k = (long)(a/b);
	 			res = a - k*b;
	 			return res;
	 		}
	 		if (b>0.) {
	 			k = (long)Math.rint(a/b);
	 			res = a - k*b;
	 			return res;
	 		}
	 	}
	 	return res;
	 }
     
    /**
     * the main program for simple test 
     * @param pars
     */
    public static void main(String[] pars) {
         int l=0;
         int k = 1;
         long lsb;
         long msb;
         long mag1=0;;
         long mag2=0;;
         for (int i = 0; i<32; i++) {
            mag1 += CDSHealpix.pow2(l);
            mag2 += CDSHealpix.pow2(k);
            System.out.println("l="+l+"  mag1="+ mag1+"  mag2="+mag2);
            l +=2;
            k +=2;
         }
         l = 0;
         k = 1;
         for (int i=0; i < 21; i++) {
             lsb = CDSHealpix.pow2(l);
             msb = CDSHealpix.pow2(k);
             System.out.println(" l="+l+" 2^l="+lsb+" l="+k+" 2^l="+msb);
             l +=2;
             k +=2;
             
         }
         double a= 6004765143422312.;
         double b = BitManipulation.MODULO(a, 16.);
         System.out.println("a="+a+" MODULO(1024)="+b);
     }
}
