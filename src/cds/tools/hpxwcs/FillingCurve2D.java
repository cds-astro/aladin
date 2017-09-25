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

package cds.tools.hpxwcs;

/**
 * Interface defining a two-dimensional space-filling curve.
 * It provides method to transform a pair of 2d-coordinates in a single index in the curve.
 * The index value is here called hash value since the intput parameters are discritized
 * coordinates, and information is lost during the discretization.  
 * 
 * @author F.-X. Pineau
 *
 */
public interface FillingCurve2D {
  /**
   * The default implementation (not implemented to stay compatible with Java Version < 8)
   * should be: {@code return ij2hash((int) x, (int) y)}.
   * @param x coordinate along the horizontal axis
   * @param y coordinate along the vertical axis
   * @return the space filling curve index (or hash value) associated
   *     to the given 2d-coordinates.
   */
  long xy2hash(double x, double y);

  /**
   * Transforms coordinates in a discretized 2d-plane into a space
   * filling curve index. This can be thought of as computing a hash
   * value from 2d-coordinates.
   * @param i discretized coordinate along the horizontal axis
   * @param j discretized coordinate along the vertical axis
   * @return the space filling curve index (or hash value) associated
   *     to the given discretized 2d-coordinates.
   */
  long ij2hash(int i, int j);
  
  /**
   * Special case of {@link #ij2hash(int, int)} in which the discretized coordinate along
   * the vertical axis equals zero.
   * @param i discretized coordinate along the horizontal axis
   * @return the space filling curve index (or hash value) associated to the given discretized
   *     horizontal coordinate, assuming the discretized vertical coordinate equal zero.
   */
  long i02hash(int i);

  /**
   * Transforms the given space filling curve index (or hash value) into
   * a single value from which it is straightforward to extract the
   * associated 2d-coordinates using methods {@link #ij2i(long)} and
   * {@link #ij2j(long)}.
   * @param hash the space filling curve index (or hash value)
   * @return a single value from which it is straightforward to extract the associated
   *     2d-coordinates using methods {@link #ij2i(long)} and {@link #ij2j(long)}.
   */
  long hash2ij(long hash);
  
  /**
   * Special case of {@link #hash2ij(long)} in which the discretized coordinate along
   * the vertical axis equals zero.
   * @param hash the space filling curve index (or hash value)
   * @return a single value, knowing the vertical coordinate equals zero, from which it is
   *     straightforward to extract the associated horizontal coordinate using method
   *     {@link #ij2i(long)}.
   */
  long hash2i0(long hash);

  /**
   * Extract the discretized horizontal coordinates from the result of the method
   * {@link #hash2ij(long)}.
   * @param ij result of the method {@link #hash2ij(long)}
   * @return the discretized horizontal coordinate stored in the given parameter {@code ij}
   */
  int ij2i(long ij);

  /**
   * Extract the discretized vertical coordinates from the result of the method
   * {@link #hash2ij(long)}.
   * @param ij result of the method {@link #hash2ij(long)}
   * @return the discretized horizontal coordinate stored in the given parameter {@code ij}
   */
  int ij2j(long ij);

}
