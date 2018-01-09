// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.image;



public class Iqefunc {
   
   
   /**************   MIDAS original C code, translated in java by P.Fernique [CDS] in nov 2010 *********************/


   /*===========================================================================
   Copyright (C) 1995 European Southern Observatory (ESO)

   This program is free software; you can redistribute it and/or 
   modify it under the terms of the GNU General Public License as 
   published by the Free Software Foundation; either version 2 of 
   the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public 
   License along with this program; if not, write to the Free 
   Software Foundation, Inc., 675 Massachusetts Ave, Cambridge, 
   MA 02139, USA.

   Correspondence concerning ESO-MIDAS should be addressed as follows:
     Internet e-mail: midas@eso.org
     Postal address: European Southern Observatory
             Data Management Division 
             Karl-Schwarzschild-Strasse 2
             D 85748 Garching bei Muenchen 
             GERMANY
 ===========================================================================*/

 /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
 .COPYRIGHT  (c)  1996  European Southern Observatory
 .IDENT      iqefunc.c
 .LANGUAGE   C
 .AUTHOR     P.Grosbol,  IPG/ESO
 .KEYWORDS   Image Quality Estimate, PSF
 .PURPOSE    Routines for Image Quality Estimate
  holds
  iqe, iqebgv, iqemnt, iqesec, iqefit
 .VERSION    1.0  1995-Mar-16 : Creation, PJG
 .VERSION    1.1  1995-Jun-22 : Correct derivatives in 'g2efunc', PJG
 .VERSION    1.2  1996-Dec-03 : Code clean-up, PJG

 000427

 ------------------------------------------------------------------------*/

   static double  hsq2 = 0.7071067811865475244;    /* constant 0.5*sqrt(2) */

   static final int    MA = 6;        /* No. of variables                */
   static final int    MITER = 64;    /* Max. no. of iterations          */

   static     double []  pval;
   static     int       mx, mp, winsize;
   static     double [] w = new double[9];
   static     double [] xi = new double[9];
   static     double [] yi = new double[9];
   

   static public int iqe(double [] pfm, int mx, int my, double [] parm, double [] sdev)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Estimate parameters for the Image Quality using a small
           frame around the object. The following parameters are
           estimated and given in the array 'parm':
                parm[0] = mean X position within array, first pixel = 0
                parm[1] = FWHM in X
                parm[2] = mean Y position within array, first pixel = 0
                parm[3] = FWHM in Y
                parm[4] = angle of major axis, degrees, along X = 0
                parm[5] = peak value of object above background
                parm[6] = mean background level
           Further, estimates of the standard deviation of the parameters
           are given in 'sdev'. The routine is just a sequence of calls
           to 'iqebgv', 'iqemnt', 'iqesec' and 'iqefit'.
.RETURN    status,  0: OK, <0: estimate failed,
------------------------------------------------------------------------*/
   {
      int      n, nbg[] = new int[1];
      double    bgv[] = new double[1], bgs[] = new double[1], s2f, r2d;
      double    [] ap = new double[6];
      double    [] cv = new double[6];
      double    [] est = new double[6];
      double    [] sec = new double[6];

      s2f = (double)( 2.0*Math.sqrt(2.0*Math.log(2.0)) );               /* Sigma to FWHM constant */
      r2d = (double)( 45.0/Math.atan(1.0) );                       /* Radian to Degrees      */
      for (n=0; n<7; n++) parm[n] = sdev[n] = 0.0f;

      winsize = (mx * my) - 1;            /* size of sub window */

      if (iqebgv(pfm, mx, my, bgv, bgs, nbg)!=0) return -1;
      parm[6] = bgv[0];
      sdev[6] = bgs[0];

     if (iqemnt(pfm, mx, my, bgv[0], bgs[0], est)!=0) return -2;
      parm[0] = est[1];
      parm[1] = s2f*est[2];
      parm[2] = est[3];
      parm[3] = s2f*est[4];
      parm[5] = est[0];
      
      if (iqesec(pfm, mx, my, bgv[0], est, sec)!=0) return -3;
      parm[4] = r2d*sec[5];

      if (iqefit(pfm, mx, my, bgv[0], sec, ap, cv)<0) return -4;
      parm[0] = ap[1]; 
      sdev[0] = cv[1];
      parm[1] = s2f*ap[2]; 
      sdev[1] = s2f*cv[2];
      parm[2] = ap[3]; 
      sdev[2] = cv[3];
      parm[3] = s2f*ap[4]; 
      sdev[3] = s2f*cv[4];
      parm[4] = (double) fmod(r2d*ap[5]+180.0, 180.0); 
      sdev[4] = r2d*cv[5];
      if (sdev[4] > 180.) sdev[4] = 180.0f;        /* max is: Pi */
      parm[5] = ap[0]; 
      sdev[5] = cv[0];

      return 0;
   }

   static public int iqebgv(double [] pfm, int mx, int my, double [] bgm, double [] bgs, int [] nbg)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Estimate background level for subimage
.RETURN    status,  0: OK, -1:no buffer space, -2: no points left 
------------------------------------------------------------------------*/
   {
      int      n, m, ns, ms, nt, mt;
      double  pfb[];
      int pwb,pw,pf;
      int    pf0, pf1, pf2, pf3, pfs0, pfs1, pfs2, pfs3;
      double   val, fks, ba, bm, bs;

      bgm[0] = 0.0f;
      bgs[0] = 0.0f;
      nbg[0] = 0;
      
      pfs0 = 0;
      pfs1 = mx - 1;
      pfs2 = mx*(my-1);
      pfs3 = mx*my - 1;

      ns = (mx<my) ? mx - 1 : my - 1;
      ms  = (mx<my) ? mx/4 : my/4;
      pfb = new double[8*ns*ms];
      pwb = 4*ns*ms;

      /* extrat edge of matrix from each corner  */

      nt = 0;
      pf = 0; pw = pwb;
      for (m=0; m<ms; m++) {
         pf0 = pfs0; pf1 = pfs1; pf2 = pfs2; pf3 = pfs3;
         for (n=0; n<ns; n++) {
            pfb[pf++] = pfm[ pf0++ ];
            pfb[pf++] = pfm[ pf1 ]; pf1 += mx;
            pfb[pf++] = pfm[ pf2 ]; pf2 -= mx;
            pfb[pf++] = pfm[ pf3-- ];
         }
         nt += 4*ns;
         ns -= 2;
         pfs0 += mx + 1;
         pfs1 += mx - 1;
         pfs2 -= mx - 1;
         pfs3 -= mx + 1;
      }

      /*  skip all elements with zero weight and sort clean array */

      pf = pf0 = 0; pw = pwb;
      n = nt; mt = 0;
      mt = nt;
      while (n-- !=0) pfb[pw++]= 1.0f;
      hsort(mt, pfb);
      nt = mt;
      
      /* first estimate of mean and rms   */

      m = mt/2; n = mt/20;
      ba = pfb[m];
      bs = 0.606*(ba-pfb[n]);                     /*  5% point at 1.650 sigma */
      if (bs<=0.0) bs = Math.sqrt(Math.abs(ba));      /* assume sigma of Poisson dist. */
      bgm[0] = (double) ba;

      /* then do 5 loops kappa sigma clipping  */

      for (m=0; m<5; m++) {
         pf = 0; pw = pwb;
         fks = 5.0 * bs;
         bm = bs = 0.0; mt = 0;
         for (n=0; n<nt; n++, pw++) {
            val = pfb[pf++];
            if (0.0<pfb[pw] && Math.abs(val-ba)<fks) {
               bm += val; bs += val*val; mt++;
            }
            else pfb[pw] = 0.0f;
         }
         if (mt<1) return -2;
         ba = bm/mt; bs = bs/mt - ba*ba;
         bs = (0.0<bs) ? Math.sqrt(bs) : 0.0;
      }

      /* set return values and clean up     */

      bgm[0] = (double) ba;
      bgs[0] /= (double) bs;
      nbg[0] = mt;

      return 0;
   }

   static public int iqemnt(double [] pfm, int mx, int my, double bgv, double bgs, double [] amm)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Find center of object and do simple moment analysis
.COMMEMTS  The parameter array 'amm' is as follows:
              amm[0] = amplitude over background
              amm[1] = X center,   amm[2] = X sigma;
              amm[3] = Y center,   amm[4] = Y sigma;
              amm[5] = angle of major axis
.RETURN    status,  0: OK,  -1: mean<0.0
------------------------------------------------------------------------*/
   {
      int      n, nx, ny, nt, nxc, nyc, ndx=0, ndy=0, ioff;
      int      k, ki, ks, kn, psize;
      double    av[]=new double[1] , dx[]=new double[1], dy[]=new double[1];
      int    pf;
      double   val, x, y, dv, xm, ym;
      double   am, ax, ay, axx, ayy, axy;

      dv = 5.0*bgs;
      xm = mx - 1.0;
      ym = my - 1.0;
      for (nx=0; nx<6; nx++) amm[nx] = 0.0f;

      /* get approx. center of object by going up along the gradient    */

      n = nx = ny = 1;
      nxc = mx/2; nyc = my/2;
      nt = (nxc<nyc) ? nxc : nyc;
      while (nt-- !=0)
      {
         if (estm9p(pfm, mx, my, nxc, nyc, av, dx, dy)!=0) break;

         if (n!=0) 
            n = 0;
         else 
         {
            if (dx[0]*ndx<0.0) nx = 0;
            if (dy[0]*ndy<0.0) ny = 0;
         }
         if (nx==0 && ny==0 ) break;

         ndx = (0.0<dx[0]) ? nx : -nx;
         ndy = (0.0<dy[0]) ? ny : -ny;
         nxc += ndx;
         nyc += ndy;
      }

      /* then try a simple moment of pixels above 5 sigma  */

      y = 0.0;
      nt = 0; ny = my;
      pf = 0; 
      ax = ay = 0.0;
      while (ny-- !=0)
      {
         x = 0.0;
         nx = mx;             /* ojo! */
         while (nx-- !=0)
         {
            val = pfm[pf++] - bgv;
            if (dv<val) 
            {
               ax  += x;
               ay  += y;
               nt++;
            }
            x += 1.0;
         }
         y += 1.0;
      }
      if (nt<1) return -1;
      nx = (int)Math.floor(ax/nt); ny = (int)Math.floor(ay/nt);
      val = pfm[nx+mx*ny];
      if (av[0]<val) { nxc = nx; nyc = ny; }         /* the higher peak wins  */

      /* finally, compute moments just around this position  */

      nt = 0; nx = 1;
      x = nxc; y = nyc;
      ioff = nxc+mx*nyc;
      n = (mx<my) ? mx-1 : my-1;
      pf = ioff;
      psize = ioff;
      
      if ((psize < 0) || (psize > winsize)) return -99;

      val = pfm[pf] - bgv;
      am  = val;
      ax  = val*x;
      ay  = val*y;
      axx = val*x*x;
      ayy = val*y*y;
      axy = val*x*y;
      nt++;

      ki = ks = kn = 1;
      while (n-- !=0 )
      {
         k = kn;
         if (ki==0 && ks==-1)
         {
            if (nx!=0) 
               nx = 0;
            else 
               break;
         }
         ioff = (ki!=0) ? ks : ks*mx;
         while (k-- !=0)
         {
            if (ki!=0) x += ks; else y += ks;
            if (x<0.0 || y<0.0 || xm<x || ym<y) break;

            pf += ioff;
            psize = ioff;
            if ((psize < 0) || (psize > winsize)) break;

            val = pfm[pf] - bgv;
            if ( dv<val ) 
            {
               am  += val;
               ax  += val*x;
               ay  += val*y;
               axx += val*x*x;
               ayy += val*y*y;
               axy += val*x*y;
               nt++; nx++;
            }
         }
         ki = ki!=0 ? 0 : 1;
         if( ki!=0 ) { ks = -ks; kn++; }
      }
      if (am<=0.0) return -1;

      /* normalize the moments and put them in to the output array   */

      amm[1] = (double)( ax/am );
      amm[3] = (double)( ay/am );
      axx = axx/am - amm[1]*amm[1];
      amm[2] = (double)( (0.0<axx) ? Math.sqrt(axx) : 0.0 );
      ayy = ayy/am - amm[3]*amm[3];
      amm[4] = (double)( (0.0<ayy) ? Math.sqrt(ayy) : 0.0 );
      axy = (axy/am-amm[1]*amm[3])/axx;
      amm[5] = (double) fmod(Math.atan(axy)+4.0*Math.atan(1.0), 4.0*Math.atan(1.0));
      nx = (int) amm[1]; ny = (int) amm[3];
      amm[0] = pfm[nx+ny*mx] - bgv;

      return 0;
   }

//   static private double fmod1(double x, double y) {
//      double div = x/y;
//      return (div - Math.floor(div) ) * y;
//   }
   
   static private double fmod(double a, double b) {
      double res = 0.;
      long k = 0;
      if (a > 0.) {
          if (b > 0.) {
              k = (long) (a / b);
              res = a - k * b;
              return res;
          }
          if (b < 0.) {
              k = (long) Math.rint(a / b);
              res = a - k * b;
              return res;
          }
      }
      if (a <= 0.) {
          if (b <= 0.) {
              k = (long) (a / b);
              res = a - k * b;
              return res;
          }
          if (b > 0.) {
              k = (long) Math.rint(a / b);
              res = a - k * b;
              return res;
          }
      }
      return res;
  }

   
   static public int estm9p(double [] pfm, int mx, int my, int nx, int ny, double[] rm, double[] dx, double []dy) 
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Estimate parameters for 3x3 pixel region
.RETURN    status, 0:OK, -1:out of range, 
------------------------------------------------------------------------*/
   {
      int      n, nt, ix, iy, idx[] = new int[9];
      double    a, am;
      int      pfb,pwb;
      double    fb[] = new double[9], wb[] = new double[9];



      /* check if 3x3 region is fully within frame   */

      if (nx<1 || mx<nx-2 || ny<1 || my<ny-2) return -1;


      /* extract region into local array and generate a rank index for it */

      iy = 3;
      pfb = 0; pwb = 0; 
      int i=nx-1 + mx*(ny-1);
      while(iy-- !=0) 
      {
         ix = 3;
         while (ix-- !=0) 
         {
            fb[pfb++] = pfm[i++];
            wb[pwb++] = 1.0f;
         }
         i += mx - 3;
      }
      indexx(9, fb, idx);


      /* omit largest value and estimate mean     */

      wb[idx[8]] = 0.0f;

      nt = 0;
      am = 0.0f;
      for (n=0; n<9; n++) {
         if (0.0<wb[n]) { am += fb[n]; nt++; }
      }
      rm[0] = (double)( am/nt );


      /* calculate mean gradient in X and Y */

      a = am = 0.0f; ix = iy = 0;
      for (n=0; n<9; n +=3) {
         if (0.0<wb[n]) { a += fb[n]; ix++; }
         if (0.0<wb[n+2]) { am +=fb[n+2]; iy++; }
      } 
      dx[0] = (double)( 0.5*(am/iy - a/ix) );

      a = am = 0.0f; ix = iy = 0;
      for (n=0; n<3; n++) {
         if (0.0<wb[n]) { a += fb[n]; ix++; }
         if (0.0<wb[n+6]) { am +=fb[n+6]; iy++; }
      }

      dy[0] = (double)( 0.5*(am/iy - a/ix) );

      return 0;
   }


   static public int iqesec(double []pfm, int mx, int my, double bgv, double [] est, double [] sec)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Perform a sector analysis of object. Estimates for center and
           size are given in 'est' which is used for bootstrap.
.COMMEMTS  The parameter arrays 'est' and 'sec' are as follows
              sec[0] = amplitude over background
              sec[1] = X center,   sec[2] = X sigma;
              sec[3] = Y center,   sec[4] = Y sigma;
              sec[5] = angle of major axis
.RETURN    status, 0:OK, -1: no buffer,
------------------------------------------------------------------------*/
   {
      int      n, k, ki, ks, kn, nxc, nyc, ioff, idx;
      int      ns[] = new int[8], psize;
      int      pf;
      double    f;
      double   x, y, xm, ym, xc, yc, fac, dx, dy;
      double    r, rl, rh, sb[] = new double[8],a2r, a2i;



      /* initiate basic variables    */

      fac = 1.0/Math.atan(1.0);
      for (n=0; n<6; n++) sec[n] = 0.0f;
      for (n=0; n<8; n++) { sb[n] = 0.0; ns[n] = 0; }
      xc = x = est[1]; xm = mx - 1.0;
      yc = y = est[3]; ym = my - 1.0;
      if (est[2]<est[4]) {
         rl = 2.0*est[2]; rh = 4.0*est[4]; n = (int) Math.ceil(16.0*est[4]);
      }
      else {
         rl = 2.0*est[4]; rh = 4.0*est[2]; n = (int) Math.ceil(16.0*est[2]);
      }

      /* extract the sectors around the center of the object  */

      nxc = (int) Math.floor(x+0.5); nyc = (int) Math.floor(y+0.5);
      ioff = nxc+mx*nyc;
      pf = ioff;

      ki = ks = kn = 1;
      while (n-- !=0) {
         k = kn;
         ioff = (ki!=0) ? ks : ks*mx;
         while (k-- !=0) {
            if (ki!=0) x += ks; else y += ks;
            if (x<0.0 || y<0.0 || xm<x || ym<y) break;

            pf += ioff;
            psize = ioff;
            if ((psize < 0) || (psize > winsize)) break;

            dx = x - xc; dy = y - yc;
            r = Math.sqrt(dx*dx + dy*dy);
            if (rl<r && r<rh) {
               f = pfm[pf] - bgv;
               idx = ((int) (fac*Math.atan2(y-yc,x-xc)+8.5))%8;
               sb[idx] += (0.0<f) ? f : 0.0;
               ns[idx]++;
            }
         }
         ki = ki!=0 ? 0 : 1;
         if( ki!=0) { ks = -ks; kn++; }
      }

      /* normalize the sector array and do explicit FFT for k=1,2  */

      for (n=0; n<8; n++) {
         if (ns[n]<1) ns[n] = 1;
         sb[n] /= ns[n];
      }

      a2r = sb[0]-sb[2]+sb[4]-sb[6];
      a2i = sb[1]-sb[3]+sb[5]-sb[7];

      for (n=0; n<6; n++) sec[n] = est[n];        /* copy estimates over  */
      if (a2r==0.0 && a2i==0.0) return -2;
      sec[5] = (double) fmod(0.5*Math.atan2(a2i,a2r), 4.0/fac);

      return 0;
   }


   static public int iqefit(double [] pfm, int mx, int my, double bgv, double [] est, double [] ap, double [] cm)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Fit 2D Gaussian function to PSF
.COMMEMTS  The parameter arrays 'est' and 'ap' are as follows
              ap[0] = amplitude over background
              ap[1] = X center,   ap[2] = X sigma;
              ap[3] = Y center,   ap[4] = Y sigma;
              ap[5] = angle of major axis
.RETURN    no. of iterations, <0: error, -10: no buffer
------------------------------------------------------------------------*/
   {
      int      n, ix, iy, nx, ny, nxs, nys, psize;
      int      pf;
      double [] pfb;
      double   chi[] = new double[1];


      /* initialize basic variables    */

      for (n=0; n<6; n++) ap[n] = cm[n] = 0.0f;

      /* allocate buffer for a 4 sigma region around the object */

      nxs = (int) Math.floor(est[1] - 4.0*est[2]);
      if (nxs<0) nxs = 0;
      nys = (int) Math.floor(est[3] - 4.0*est[4]);
      if (nys<0) nys = 0;
      nx = (int) Math.ceil(8.0*est[2]);
      if (mx<nxs+nx) nx = my - nxs;
      ny = (int) Math.ceil(8.0*est[4]);
      if (my<nys+ny) ny = my - nys;

      pfb = new double[2*nx*ny];

      /* extract region from external buffer */
      int i = nxs + mx*nys;
      psize = i;
      if ((psize < 0) || (psize > winsize)) return -99;


      pf = 0;
      iy = ny;
      while (iy-- !=0) 
      {
         ix = nx;
         while (ix-- !=0)
         {
            pfb[pf++] = pfm[i++] - bgv;
            psize = pf;
            if (psize > winsize) return -99;

         }
         i += mx - nx;
         psize = i;
         if ((psize < 0) || (psize > winsize)) return -99;
      }

      /* initialize parameters for fitting    */

      ap[0] = est[0];
      ap[1] = est[1] - nxs;
      ap[2] = est[2];
      ap[3] = est[3] - nys;
      ap[4] = est[4];
      ap[5] = est[5];


      /* perform actual 2D Gauss fit on small subimage  */

      n = g2efit(pfb, nx, ny, ap, cm, chi);

      /* normalize parameters and uncertainties, and exit   */

      ap[1] += nxs;
      ap[3] += nys;

      return n;
   }


   static public int g2einit(double [] val, int nx, int ny)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Initiate gauss fit function, set pointers to data and weights
.RETURN    status,  0: OK, -1: error - bad pixel no.
------------------------------------------------------------------------*/
   {
      double   fh, w1, w2, w3;

      if (nx<1) {                     /* if NO x-pixel set to NULL          */
         pval  = null;
         mx = mp = 0;
         return -1;
      }

      pval = val;                     /* otherwise initiate static varables */
      mx = nx;
      mp = (0<ny) ? ny*nx : nx;

      fh = 0.5*Math.sqrt(3.0/5.0);      /* positions and weights for integration */
      w1 = 16.0/81.0;
      w2 = 10.0/81.0;
      w3 = 25.0/324.0;

      xi[0] = 0.0; yi[0] = 0.0; w[0] = w1;
      xi[1] = 0.0; yi[1] =  fh; w[1] = w2;
      xi[2] = 0.0; yi[2] = -fh; w[2] = w2;
      xi[3] =  fh; yi[3] = 0.0; w[3] = w2;
      xi[4] = -fh; yi[4] = 0.0; w[4] = w2;
      xi[5] =  fh; yi[5] =  fh; w[5] = w3;
      xi[6] = -fh; yi[6] =  fh; w[6] = w3;
      xi[7] =  fh; yi[7] = -fh; w[7] = w3;
      xi[8] = -fh; yi[8] = -fh; w[8] = w3;

      return 0;
   }


   static public int g2efunc(int idx, double [] val, double [] fval, double [] psig, double [] a, double [] dyda, int ma)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   evaluate function value for given index
.RETURN    status,  0: OK, 1: error - bad pixel no.
------------------------------------------------------------------------*/
   {
      int      n;
      double   ff, sum, ci, si;
      double   xc, yc, xx, yy, x, y;

      if (idx<0 || mp<=idx) return -1;              /* check index          */
      if (a[2]<=0.0 || a[4]<=0.0) return -2;        /* negative sigmas      */

      xc = (double) (idx%mx) - a[1];
      yc = (double) (idx/mx) - a[3];

      val[0] = pval[idx];
      psig[0] = 1.0f;
      si = Math.sin(a[5]);
      ci = Math.cos(a[5]);

      sum = 0.0;
      for (n=0; n<9; n++) {
         x  = xc + xi[n];
         y  = yc + yi[n];
         xx = (ci*x + si*y)/a[2];
         yy = (-si*x + ci*y)/a[4];
         sum += w[n]*Math.exp(-0.5*(xx*xx+yy*yy));
      }
      xx = (ci*xc + si*yc)/a[2];
      yy = (-si*xc + ci*yc)/a[4];

      ff    = a[0]*sum;
      fval[0] = (double) ff;

      dyda[0] = (double) sum;
      dyda[1] = (double)( ff*(ci*xx/a[2] - si*yy/a[4]) );
      dyda[2] = (double)( ff*xx*xx/a[2] );
      dyda[3] = (double)( ff*(si*xx/a[2] + ci*yy/a[4]) );
      dyda[4] = (double)( ff*yy*yy/a[4] );
      dyda[5] = (double)( ff*((si*xc-ci*yc)*xx/a[2] + (ci*xc+si*yc)*yy/a[4]) );

      return 0;
   }

   public static int g2efit(double [] val, int nx, int ny, double [] ap, double [] cv, double [] pchi)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
.PURPOSE   Perform 2D Gauss fit
.RETURN    status,  no. of iterations, else  -1: error - bad pixel no,
                                             -2: error - iteration.
------------------------------------------------------------------------*/
   {
      int      mt, n, na, ni, lista[]=new int[MA];
      double    apo[] = new double[MA];
      double   c2, a1[] = new double[1], a2, pi, alpha[] = new double[MA*MA], cvm[] = new double[MA*MA];



      if (g2einit(val,nx, ny)!=0) return -1;

      pi = 4.0*Math.atan(1.0);
      a1[0] = -1.0;
      mt = nx * ny;
      for (n=0; n<MA; n++) { lista[n] = n; cv[n] = 0.0f; }

      pchi[0] = c2 = 0.0; a2 = 0.0; na = 0;
      for (ni=0; ni<MITER; ni++) {
         for (n=0; n<MA; n++) apo[n] = ap[n];
         if (mrqmin(mt, ap, MA, lista, MA, cvm, alpha, pchi, a1)!=0)
            return -2;
         if (a1[0]<a2 && Math.abs(pchi[0]-c2)<1.0e-5*c2) break;
         if (a1[0]<a2) { c2 = pchi[0]; na = 0; } else na++;
         a2 = a1[0];
         if (5<na) break;
         if (ap[0]<=0.0) ap[0] = (double)( 0.5 * apo[0] );
         if (ap[2]<=0.0) ap[2] = (double)( 0.5 * apo[2] );
         if (ap[4]<=0.0) ap[4] = (double)( 0.5 * apo[4] );
         ap[5] = (double)fmod(ap[5], pi);
         if (ap[1]<0.0 || nx<ap[1] || ap[3]<0.0 || ny<ap[3]) return -3;
      }

      a1[0] = 0.0;
      if (mrqmin(mt, ap, MA, lista, MA, cvm, alpha, pchi, a1)!=0)
         return -2;

      ap[5] = (double) fmod(ap[5]+pi, pi);
      for (n=0; n<MA; n++) cv[n] = (double) Math.sqrt(cvm[n+n*MA]);

      return ((MITER<=ni) ? -4 : ni);
   }

   
   /* @(#)mrqfit.c  10.1.1.2 (ES0-DMD) 12/18/95 18:21:10 */
   /*===========================================================================
     Copyright (C) 1995 European Southern Observatory (ESO)
    
     This program is free software; you can redistribute it and/or 
     modify it under the terms of the GNU General Public License as 
     published by the Free Software Foundation; either version 2 of 
     the License, or (at your option) any later version.
    
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
    
     You should have received a copy of the GNU General Public 
     License along with this program; if not, write to the Free 
     Software Foundation, Inc., 675 Massachusetss Ave, Cambridge, 
     MA 02139, USA.
    
     Corresponding concerning ESO-MIDAS should be addressed as follows:
       Internet e-mail: midas@eso.org
       Postal address: European Southern Observatory
               Data Management Division 
               Karl-Schwarzschild-Strasse 2
               D 85748 Garching bei Muenchen 
               GERMANY
   ===========================================================================*/

   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .COPYRIGHT  (c)  1995  European Southern Observatory
   .IDENT      mrqfit.c
   .LANGUAGE   C
   .AUTHOR     P.Grosbol,  IPG/ESO
   .COMMENT    Algorithm taken from 'Numerical Recipes in C' s.14.4, p.545
               Combination of mrqmin() and mrqcof() with modified func().
               NOTE: Data arrays start  with index 0.
                     FORTRAN order> a[ir][ic] = a[ir+ic*n]
   .KEYWORDS   Nonlinear Model fit
   .VERSION    1.0  1994-May-23 : Creation, PJG
   .VERSION    1.1  1995-Apr-29 : Correct problem when mfit!=ma, PJG
   ------------------------------------------------------------------------*/
   static final int     MMA =16;   /* Max. no. of variables         */

   static private double []         atry = new double[MMA];
   static private double []           da = new double[MMA];
   static private double []        oneda = new double[MMA];
   static private double []         beta = new double[MMA];
   static private double []       cv = new double[MMA*MMA];
   static private double            ochisq;

   static public int mrqmin(int ndata,double [] a,int ma,int [] lista,int mfit,
         double [] covar,double [] alpha,double [] chisq, /* funcs,*/ double [] alamda)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .PURPOSE   
   .RETURN    status,  0: OK, -1: Bad permutation LISTA 1,
                      -2: Bad permutation LISTA 2, -3: too many variables,
                      -4: No points (chisq<=0), -5: error in matrix inversion
   ------------------------------------------------------------------------*/
   {
      int     k, kk, j, ihit;

      if (alamda[0] < 0.0) {
         if (MMA<ma || ma<mfit) return -3;
         kk = mfit;
         for (j=0; j<ma; j++) {
            ihit = 0;
            for (k=0; k<mfit; k++)
               if (lista[k] == j) ihit++;
            if (ihit == 0)
               lista[kk++] = j;
            else if (ihit > 1) return -1;
         }
         if (kk != ma) return -2;
         alamda[0] = 0.001;
         mrqcof(ndata, a, ma, lista, mfit, alpha, beta, chisq /*, funcs */);
         if (chisq[0]<=0.0) return -4;
         ochisq = (chisq[0]);
      }

      for (j=0; j<mfit; j++) {
         for (k=0; k<mfit; k++) covar[j+k*ma] = cv[j+k*mfit] = alpha[j+k*ma];
         covar[j+j*ma] = cv[j+j*mfit] = alpha[j+j*ma]*(1.0+alamda[0]);
         oneda[j] = beta[j];
      }

      if (gaussj(cv, mfit, oneda, 1)!=0) return -5;
      for (j=0; j<mfit; j++) da[j] = oneda[j];

      if (alamda[0] == 0.0) {
         for (j=0; j<mfit; j++)
            for (k=0; k<mfit; k++) covar[j+k*ma] = cv[j+k*mfit];
         covsrt(covar, ma, lista, mfit);
         return 0;
      }

      for (j=0; j<ma; j++) atry[j] = a[j];
      for (j=0; j<mfit; j++)
         atry[lista[j]] = (double) (a[lista[j]]+da[j]);

      mrqcof(ndata, atry, ma, lista, mfit, covar, da, chisq/* , funcs*/);
      if (0.0<chisq[0] && chisq[0]<ochisq) {
         alamda[0] *= 0.1;
         ochisq = chisq[0];
         for (j=0; j<mfit; j++) {
            for (k=0; k<mfit; k++) alpha[j+k*ma] = covar[j+k*ma];
            beta[j] = da[j];
            a[lista[j]] = atry[lista[j]];
         }
      } else {
         alamda[0] *= 10.0;
         chisq[0] = ochisq;
      }

      return 0;
   }

   static public int mrqcof(int ndata,double [] a,int ma,int [] lista,int mfit,double [] alpha,double []veta,
         double [] chisq/* ,funcs*/ )
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .PURPOSE   compute covarient matrix and chisq from all values
   .RETURN    always 0
   ------------------------------------------------------------------------*/
   {
     int     k, j, i;
     double   ymod[] = new double[1], wt, sig2i[] = new double[1], dy, y[] = new double[1];
     double   dyda[] = new double[MMA];

     for (j=0; j<mfit; j++) {
        for (k=0; k<=j; k++) alpha[j+k*ma] = 0.0;
        veta[j] = 0.0;
      }

     chisq[0] = 0.0;
     for (i=0; i<ndata; i++) {
//        if ((*funcs)(i, &y, &ymod, &sig2i, a, dyda, ma)) continue;
//        int g2efunc(int idx, double [] val, double [] fval, double [] psig, double [] a, double [] dyda, int ma)
        if( g2efunc(i, y, ymod, sig2i, a, dyda, ma)!=0 ) continue;
        dy = y[0] - ymod[0];
        for (j=0; j<mfit; j++) {
       wt = dyda[lista[j]]*sig2i[0];
       for (k=0; k<=j; k++)
         alpha[j+k*ma] += wt*dyda[lista[k]];
       veta[j] += dy*wt;
         }
        chisq[0] += dy*dy*sig2i[0];
      }

     for (j=1; j<mfit; j++)
       for (k=0; k<j; k++)
          alpha[k+j*ma] = alpha[j+k*ma];

     return 0;
   }
   
   
   /* @(#)indexx.c  10.1.1.2 (ES0-DMD) 12/18/95 18:21:10 */
   /*===========================================================================
     Copyright (C) 1995 European Southern Observatory (ESO)
    
     This program is free software; you can redistribute it and/or 
     modify it under the terms of the GNU General Public License as 
     published by the Free Software Foundation; either version 2 of 
     the License, or (at your option) any later version.
    
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
    
     You should have received a copy of the GNU General Public 
     License along with this program; if not, write to the Free 
     Software Foundation, Inc., 675 Massachusetss Ave, Cambridge, 
     MA 02139, USA.
    
     Corresponding concerning ESO-MIDAS should be addressed as follows:
       Internet e-mail: midas@eso.org
       Postal address: European Southern Observatory
               Data Management Division 
               Karl-Schwarzschild-Strasse 2
               D 85748 Garching bei Muenchen 
               GERMANY
   ===========================================================================*/

   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .COPYRIGHT  (c)  1990  European Southern Observatory
   .IDENT      indexx.c
   .LANGUAGE   C
   .AUTHOR     P.Grosbol,  IPG/ESO
   .COMMENT    Algorithm taken from 'Numerical Recipes in C' p.248
   .KEYWORDS   heapsort, index
   .VERSION    1.0  1990-Dec-14 : Creation, PJG
   .VERSION    1.1  1995-Mar-10 : Split into double/double functions, PJG
   ----------------------------------------------------------------------*/

   static public void indexx(int n,double [] arrin,int [] indx)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .PURPOSE   compute indx[] so that arrin[indx[0..n]] is ascenting
   .RETURN    none
   ----------------------------------------------------------------------*/
   {
      int     l, j, ir, indxt, i;
      double   q;

      for (j=0; j<n; j++) indx[j] = j;
      l =  n >> 1;
      ir = n - 1;
      while( true ) {
         if (l>0) {
            indxt = indx[--l];
            q = arrin[indxt];
         }
         else {
            indxt = indx[ir];
            q = arrin[indxt];
            indx[ir] = indx[0];
            if (--ir == 0) {
               indx[0] = indxt;
               return;
            }
         }
         i = l;
         j = (l<<1) + 1;
         while (j<=ir) {
            if (j<ir && arrin[indx[j]]<arrin[indx[j+1]]) j++;
            if (q<arrin[indx[j]]) {
               indx[i] = indx[j];
               j += (i=j) + 1;
            }
            else break;
         }
         indx[i] = indxt;
      }
   }
   
   /* @(#)gaussj.c  10.1.1.2 (ES0-DMD) 12/18/95 18:21:09 */
   /*===========================================================================
     Copyright (C) 1995 European Southern Observatory (ESO)
    
     This program is free software; you can redistribute it and/or 
     modify it under the terms of the GNU General Public License as 
     published by the Free Software Foundation; either version 2 of 
     the License, or (at your option) any later version.
    
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
    
     You should have received a copy of the GNU General Public 
     License along with this program; if not, write to the Free 
     Software Foundation, Inc., 675 Massachusetss Ave, Cambridge, 
     MA 02139, USA.
    
     Corresponding concerning ESO-MIDAS should be addressed as follows:
       Internet e-mail: midas@eso.org
       Postal address: European Southern Observatory
               Data Management Division 
               Karl-Schwarzschild-Strasse 2
               D 85748 Garching bei Muenchen 
               GERMANY
   ===========================================================================*/

   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .COPYRIGHT  (c)  1994  European Southern Observatory
   .IDENT      gaussj.c
   .LANGUAGE   C
   .AUTHOR     P.Grosbol,  IPG/ESO
   .KEYWORDS   Matrix inversion, linear equaton solution, Gauss-Jordan
   .COMMENT    Algorithm taken from 'Numerical Recipes in C' s2.1, p.36
               NOTE: Data arrays  a[0..n-1][0..n-1] and b[0..n-1][0..m-1]
                     FORTRAN order> b[ir][ic] = b[ir+ic*n]
   .VERSION    1.0  1994-Jan-28 : Creation, PJG
   ------------------------------------------------------------------------*/

//   #define    MMA                 16    /* Max. size of matrix             */
   
   static public void swap(double [] a, int i, int j) {
      double t = a[i]; a[i] = a[j]; a[j] = t;
   }

   static public int gaussj(double [] a,int n,double [] b, int m)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .PURPOSE   Inverse matrix using Gauss-Jordan elimination
   .RETURN    status,  0: OK, -1: Singular matrix 1, -2: Singular matrix 2,
                      -3: matrix too big
   ------------------------------------------------------------------------*/
   {
      int      i, icol=0, irow=0, j, k, l, ll;
      int      indxc[] = new int[MMA], indxr[] = new int[MMA], ipiv[] = new int[MMA];
      double   big, dum, pivinv;

      if (MMA<n) return -3;
      for (j=0; j<n; j++) ipiv[j]=0;

      for (i=0; i<n; i++) {
         big = 0.0;
         for (j=0; j<n; j++)
            if (ipiv[j] != 1) {
               for (k=0; k<n; k++) {
                  if (ipiv[k] == 0) {
                     dum = Math.abs(a[j+k*n]);
                     if (dum >= big) {
                        big = dum;
                        irow = j;
                        icol = k;
                     }
                  } else if (ipiv[k] > 1) return -1;
               }
            }

         ++(ipiv[icol]);
         if (irow != icol) {
            for (l=0; l<n; l++) swap(a,irow+l*n,icol+l*n);
            for (l=0; l<m; l++) swap(b,irow+l*n,icol+l*n);
         }

         indxr[i] = irow;
         indxc[i] = icol;
         if (a[icol+icol*n] == 0.0) return -2;
         pivinv = 1.0/a[icol+icol*n];
         a[icol+icol*n] = 1.0;
         for (l=0; l<n; l++) a[icol+l*n] *= pivinv;
         for (l=0; l<m; l++) b[icol+l*n] *= pivinv;
         for (ll=0; ll<n; ll++)
            if (ll != icol) {
               dum = a[ll+icol*n];
               a[ll+icol*n] = 0.0;
               for (l=0; l<n; l++) a[ll+l*n] -= a[icol+l*n]*dum;
               for (l=0; l<m; l++) b[ll+l*n] -= b[icol+l*n]*dum;
            }
      }

      for (l=n-1; l>=0; l--) {
         if (indxr[l] != indxc[l])
            for (k=0; k<n; k++) swap(a,k+indxr[l]*n,k+indxc[l]*n);
      }    

      return 0;
   }

   /* @(#)covsrt.c  10.1.1.2 (ES0-DMD) 12/18/95 18:21:09 */
   /*===========================================================================
     Copyright (C) 1995 European Southern Observatory (ESO)
    
     This program is free software; you can redistribute it and/or 
     modify it under the terms of the GNU General Public License as 
     published by the Free Software Foundation; either version 2 of 
     the License, or (at your option) any later version.
    
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
    
     You should have received a copy of the GNU General Public 
     License along with this program; if not, write to the Free 
     Software Foundation, Inc., 675 Massachusetss Ave, Cambridge, 
     MA 02139, USA.
    
     Corresponding concerning ESO-MIDAS should be addressed as follows:
       Internet e-mail: midas@eso.org
       Postal address: European Southern Observatory
               Data Management Division 
               Karl-Schwarzschild-Strasse 2
               D 85748 Garching bei Muenchen 
               GERMANY
   ===========================================================================*/

   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .COPYRIGHT  (c)  1995  European Southern Observatory
   .IDENT      covsrt.c
   .LANGUAGE   C
   .AUTHOR     P.Grosbol,  IPG/ESO
   .COMMENT    Algorithm taken from 'Numerical Recipes in C' s14.3, p534
               NOTE: Data array is covar[0..ma-1][0..ma-1]
                     FORTRAN order> cvm[ir][ic] = cvm[ir+ic*ma]
   .KEYWORDS   Covariance matrix
   .VERSION    1.0  1994-Jan-28 : Creation, PJG
   .VERSION    1.1  1995-Apr-29 : Correct index error, PJG
   ------------------------------------------------------------------------*/

   static public int covsrt(double [] covar,int ma,int [] lista,int mfit)
   /*++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .PURPOSE   compute covariance matrix
   .RETURN    status, always 0: OK
   ------------------------------------------------------------------------*/
   {
      int      i, j;
      double   swap;

      for (j=0; j<ma-1; j++)
         for (i=j+1; i<ma; i++) covar[i+j*ma] = 0.0;

      for (i=0; i<mfit-1; i++)
         for (j=i+1; j<mfit; j++) {
            if (lista[j] > lista[i])
               covar[lista[j]+lista[i]*ma] = covar[i+j*ma];
            else
               covar[lista[i]+lista[j]*ma] = covar[i+j*ma];
         }

      swap = covar[0];
      for (j=0; j<ma; j++) {
         covar[j*ma] = covar[j+j*ma];
         covar[j+j*ma] = 0.0;
      }

      covar[lista[0]+lista[0]*ma] = swap;
      for (j=1; j<mfit; j++) covar[lista[j]+lista[j]*ma] = covar[j*ma];
      for (j=1; j<ma; j++)
         for (i=0; i<j; i++) covar[i+j*ma] = covar[j+i*ma];

      return 0;
   }
   
   /* @(#)sort.c    10.1.1.2 (ES0-DMD) 12/18/95 18:21:12 */
   /*===========================================================================
     Copyright (C) 1995 European Southern Observatory (ESO)
    
     This program is free software; you can redistribute it and/or 
     modify it under the terms of the GNU General Public License as 
     published by the Free Software Foundation; either version 2 of 
     the License, or (at your option) any later version.
    
     This program is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
     GNU General Public License for more details.
    
     You should have received a copy of the GNU General Public 
     License along with this program; if not, write to the Free 
     Software Foundation, Inc., 675 Massachusetss Ave, Cambridge, 
     MA 02139, USA.
    
     Corresponding concerning ESO-MIDAS should be addressed as follows:
       Internet e-mail: midas@eso.org
       Postal address: European Southern Observatory
               Data Management Division 
               Karl-Schwarzschild-Strasse 2
               D 85748 Garching bei Muenchen 
               GERMANY
   ===========================================================================*/

   /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .COPYRIGHT   (c)  1995  European Soutern Observatory
   .IDENT       hsort.c
   .LANGUAGE    C
   .AUTHOR      P.Grosbol,  IPG/ESO
   .ENVIRON     UNIX
   .KEYWORDS    sort, heapsort
   .COMMENT     Algorithm is adapted from 'Numerical Recipes in C' p.247
   .VERSION     1.0  1995-Mar-09 : Creation,  PJG
   -----------------------------------------------------------------------*/

   static public void hsort(int n, double [] ra)
   /*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
   .PURPOSE   sort array in place using heapsort
   .RETURN    none
   -----------------------------------------------------------------------*/
   {
      int      l, j, ir, i;
      double    rra;

      l = n >> 1;
         ir = n - 1;

         while (true) {
            if (l>0)
               rra = ra[--l];
            else {
               rra = ra[ir];
               ra[ir] = ra[0];
               if (--ir == 0) {
                  ra[0] = rra;
                  return;
               }
            }
            i = l;
            j = (l << 1) + 1;
            while (j<=ir) {
               if (j<ir && ra[j]<ra[j+1]) ++j;
               if (rra<ra[j]) {
                  ra[i] = ra[j];
                  j += (i=j) + 1;
               }
               else j = ir + 1;
            }
            ra[i] = rra;
         }
   }


}
