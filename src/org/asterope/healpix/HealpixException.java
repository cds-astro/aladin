//
// Licenced under GPLv2, see licence.txt
// (c) K.M. Gorski, Nickolai Kuropatkin, Jan Kotek,
//
package org.asterope.healpix;

/**
 * handles exceptions from this package
 * 
 *@author N. Kuropatkin
 */
public class HealpixException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * message - String to be printed in case of exception
	 * @param message
	 */
	public HealpixException(String message) {
		super(message);
	}
}
