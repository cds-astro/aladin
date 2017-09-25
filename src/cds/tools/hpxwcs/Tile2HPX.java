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


import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


/**
 * 
 * @author F.-X. Pineau
 *
 */
public final class Tile2HPX {
  
  public enum WCSFrame {
    EQU ("RA--", "DEC-"),
    GAL ("GLON", "GLAT"),
    ECL ("ELON", "ELAT");

    private final String lonLabel;
    private final String latLabel;

    private WCSFrame(final String lonLabel, final String latLabel) {
      this.lonLabel = lonLabel;
      this.latLabel = latLabel;
    }
  }
  
  private static final double PI_OVER_FOUR = 0.25 * Math.PI;
  /**
   * Z-Order Curve (ZOC) implementation in which the vertical coordinate carry the most significant
   * bit (VMSB). This implementation is based on the bitwise OR operator to interleave the bits of
   * the discretized 2d-coordinates. We assume that each discretized coordinates is coded on maximum
   * 32 bits.
   * The algorithm is a slightly adapted and extended version of the outer perfect shuffle defined
   * p. 106 of "Hacker's Delight" (Henry S. Warren, Jr).
   */
  public static final FillingCurve2D FC = new FillingCurve2D() {

    @Override
    public final long xy2hash(final double x, final double y) {
      return ij2hash((int) x, (int) y);
    }

    @Override
    public final long ij2hash(int i, int j) {
      long h = ((long) j ) << 32;
      h |= i;
      h = (0x00000000FFFF0000L & h) <<  16
          | (0x0000FFFF00000000L & h) >>> 16
          | (0xFFFF00000000FFFFL & h);
      h = (0x0000FF000000FF00L & h) <<  8
          | (0x00FF000000FF0000L & h) >>> 8
          | (0xFF0000FFFF0000FFL & h);
      h = (0x00F000F000F000F0L & h) <<  4
          | (0x0F000F000F000F00L & h) >>> 4
          | (0xF00FF00FF00FF00FL & h);
      h = (0x0C0C0C0C0C0C0C0CL & h) <<  2
          | (0x3030303030303030L & h) >>> 2
          | (0xC3C3C3C3C3C3C3C3L & h);
      h = (0x2222222222222222L & h) <<  1
          | (0x4444444444444444L & h) >>> 1
          | (0x9999999999999999L & h);
      return h;
    }

    @Override
    public final long i02hash(int i) {
      long h = i;
      h = ((0x00000000FFFF0000L & h) << 16) | (0x000000000000FFFF & h);
      h = ((h << 8) | h) & 0x00FF00FF00FF00FFL;
      h = ((h << 4) | h) & 0x0F0F0F0F0F0F0F0FL;
      h = ((h << 2) | h) & 0x3333333333333333L;
      h = ((h << 1) | h) & 0x5555555555555555L;
      return h;
    }

    @Override
    public final long hash2ij(long h) {
      h = (0x2222222222222222L & h) <<  1
          | (0x4444444444444444L & h) >>> 1
          | (0x9999999999999999L & h);
      h = (0x0C0C0C0C0C0C0C0CL & h) <<  2
          | (0x3030303030303030L & h) >>> 2
          | (0xC3C3C3C3C3C3C3C3L & h);
      h = (0x00F000F000F000F0L & h) <<  4
          | (0x0F000F000F000F00L & h) >>> 4
          | (0xF00FF00FF00FF00FL & h);
      h = (0x0000FF000000FF00L & h) <<  8
          | (0x00FF000000FF0000L & h) >>> 8
          | (0xFF0000FFFF0000FFL & h);
      h = (0x00000000FFFF0000L & h) <<  16
          | (0x0000FFFF00000000L & h) >>> 16
          | (0xFFFF00000000FFFFL & h);
      return h;
    }

    @Override
    public final long hash2i0(long h) {
      // Verify all bit of j are set to 0
      assert (0x3333333333333333L & h) == 0;
      h = ((h >> 1) | h) & 0x3333333333333333L;
      h = ((h >> 2) | h) & 0x0F0F0F0F0F0F0F0FL;
      h = ((h >> 4) | h) & 0x00FF00FF00FF00FFL;
      h = ((h >> 8) | h) & 0x0000FFFF0000FFFFL;
      return h;
    }

    @Override
    public final int ij2i(final long ij) {
      return (int) ij;
    }

    @Override
    public final int ij2j(final long ij) {
      return (int) (ij >>> 32);
    }
  };

  // Input parameters
  private final int order;
  private final int inNside;
  private final WCSFrame frame;
  // Precomputed quantities
  private final int nsideTile;
  private final int nsidePix;
  private final int twiceDepth;
  private final long xyMask;
  

  public Tile2HPX(final int tileOrder, final int inTileNside, final WCSFrame wcsFrame) {
    this.order = tileOrder;
    this.inNside = inTileNside;
    this.frame = wcsFrame;
    // Derivated quantities
    this.nsideTile = 1 << this.order;
    this.nsidePix = this.nsideTile * this.inNside;
    this.twiceDepth = tileOrder << 1;
    this.xyMask = (1L << this.twiceDepth) - 1;
  }

  /**
   * Returns the coordinates of the center of the cell defined by the given hash in the HEALPix
   * projection plane.
   * @param hash value defining the cell we want the coordinate of the center
   * @param result object used to store the result
   */
  public void center(long hash, final SetableXY result) {
    // Pull apart the hash elements
    final int d0h = (int) (hash >> this.twiceDepth);
    hash = FC.hash2ij(hash & this.xyMask);
    final int iInD0h = FC.ij2i(hash);
    final int jInD0h = FC.ij2j(hash);
    // Compute coordinates from the center of the base pixel with  x-axis = W-->E, y-axis = S-->N
    final int lInD0h = iInD0h - jInD0h;
    final int hInD0h = iInD0h + jInD0h - (this.nsideTile - 1);
    // Compute coordinates of the base pixel in the projection plane
    final int d0hBy4Quotient = d0h >> 2;
    final int d0hMod4 = d0h - (d0hBy4Quotient << 2);
    final int hD0h = 1 - d0hBy4Quotient;
    int lD0h = d0hMod4 << 1;
    if ((hD0h == 0 && (lD0h == 6 || (lD0h == 4 && lInD0h > 0))) // case equatorial region
        || (hD0h != 0 && ++lD0h > 3)) { // case polar caps regions
      lD0h -= 8;
    }
    // Finalize computation
    result.setXY(PI_OVER_FOUR * (lD0h + lInD0h / (double) nsideTile),
                 PI_OVER_FOUR * (hD0h + hInD0h / (double) nsideTile));
  }

 
  public final Map<String, String> toFitsHeader(final long tileIpix) throws Exception {

    final SetableXY xy = new SetableXYImpl();
    this.center(tileIpix, xy);

    final double centreX = Math.toDegrees(xy.x());
    final double centreY = Math.toDegrees(xy.y());

    final double scale = 45d / nsidePix;

    // cooAvantDeproj = [CD][cooPixel - CRPIX]
    // cooPixel - (inNside + 1) / 2.0 => put origin at the center of the tile
    // [CD][T] = [DeltaX, DeltaY] => T = [CD]^-1  [DeltaX, DeltaY]

    final double crPix1 = +((inNside + 1) / 2.0) - 0.5 * (-centreX / scale + centreY / scale);
    final double crPix2 = +((inNside + 1) / 2.0) - 0.5 * (-centreX / scale - centreY / scale);

    final Map<String, String> output = new LinkedHashMap<String, String>();
    output.put("NAXIS  ", "                    2 / number of data axes");
    output.put("NAXIS1 ", String.format(Locale.US, "%21d / length of data axis 1", inNside));
    output.put("NAXIS2 ", String.format(Locale.US, "%21d / length of data axis 1", inNside));

    output.put("CRPIX1 ", String.format(Locale.US, "%21.1f / Coordinate reference pixel", crPix1));
    output.put("CRPIX2 ", String.format(Locale.US, "%21.1f / Coordinate reference pixel", crPix2));

    // Solution using CDs
    output.put("CD1_1  ", String.format(Locale.US, "%21.13E / Transformation matrix (rot + scale)", -scale));
    output.put("CD1_2  ", String.format(Locale.US, "%21.13E / Transformation matrix (rot + scale)", -scale));
    output.put("CD2_1  ", String.format(Locale.US, "%21.13E / Transformation matrix (rot + scale)", +scale));
    output.put("CD2_2  ", String.format(Locale.US, "%21.13E / Transformation matrix (rot + scale)", -scale));

    /*// Solution using PC and CDELT
    output.put("PC1_1  ", String.format(Locale.US, "%21.13f / Transformation matrix (rot + scale)", -0.5 * Math.sqrt(2)));
    output.put("PC1_2  ", String.format(Locale.US, "%21.13f / Transformation matrix (rot + scale)", -0.5 * Math.sqrt(2)));
    output.put("PC2_1  ", String.format(Locale.US, "%21.13f / Transformation matrix (rot + scale)", +0.5 * Math.sqrt(2)));
    output.put("PC2_2  ", String.format(Locale.US, "%21.13f / Transformation matrix (rot + scale)", -0.5 * Math.sqrt(2)));
    output.put("CDELT1 ", String.format(Locale.US, "%21.13f / [deg] Coordinate increment", scale / (0.5 * Math.sqrt(2))));
    output.put("CDELT2 ", String.format(Locale.US, "%21.13f / [deg] Coordinate increment", scale / (0.5 * Math.sqrt(2))));
     */

    output.put("CTYPE1 ", " '" + frame.lonLabel + "-HPX'           / Longitude in an HPX projection");
    output.put("CTYPE2 ", " '" + frame.latLabel + "-HPX'           /  Latitude in an HPX projection");

    output.put("CRVAL1 ", "                   0. / [deg] Longitude at the reference point");
    output.put("CRVAL2 ", "                   0. / [deg]  Latitude at the reference point");
    output.put("PV2_1  ", "                   4  / HPX H parameter (longitude)");
    output.put("PV2_2  ", "                   3  / HPX K parameter  (latitude)");

    return output;
  }


  public static final Map<String, String> toFitsHeader(final int tileOrder,
      final long tileIpix, final int nPixelX, final WCSFrame frame) throws Exception {
    return new Tile2HPX(tileOrder, nPixelX, frame).toFitsHeader(tileIpix);
  }

//  public static void main(final String[] args) throws Exception {
//    int order = Integer.parseInt(args[0]);
//    long ipix = Long.parseLong(args[1]);
//    int nPixelsX = Integer.parseInt(args[2]);
//
//    // final Map<String, String> map = toFitsHeader(8, 300373L/*289450*/, 512, WCSFrame.EQU);
//    final Map<String, String> map =  toFitsHeader(order, ipix, nPixelsX,
//        args.length > 3 ? WCSFrame.valueOf(args[3].toUpperCase()): WCSFrame.EQU);
//    for (final Map.Entry<String, String> e : map.entrySet()) {
//      System.out.println(e.getKey() + " =" + e.getValue());
//    }
//  }

}
