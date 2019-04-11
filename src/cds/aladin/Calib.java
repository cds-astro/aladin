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

package cds.aladin;

import java.awt.Dimension;
import java.io.DataInputStream;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Ecliptic;
import cds.astro.FK4;
import cds.astro.FK5;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.astro.Supergal;
import cds.fits.HeaderFits;
import cds.tools.FastMath;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

//import healpix.newcore.FastMath;


/**
 * Gestion d'une calibration aladin
 *
 * @author Francois Bonnarel [CDS]
 * @version 0.9 : (??) creation
 * July 2007 : correction pour images Spitzer (projection galactique et equinoxe)
 */

public final class Calib  implements Cloneable {

   /** Retourne la taille approximative en bytes - PF oct12*/
   public long getMem() { return 22*4+448*8; }

   int    aladin ;
   double [] xyapoly = new double[12];
   double [] xydpoly = new double[12];
   double [] adypoly = new double[12];
   double [] adxpoly = new double[12];
   double [][] sip_a   = new double[10][10];
   int         order_a ;
   double [][] sip_b   = new double[10][10];
   int         order_b ;
   double [][] sip_ap  = new double[10][10]; 
   int         order_ap ;
   double [][] sip_bp  = new double[10][10];
   int         order_bp ;
   double epoch ;
   int    flagepoc =0 ;
   double equinox ;
   double alpha ;
   double delta ;
   double yz ;
   double xz  ;
   double focale ;
   double Xorg ;
   double Yorg ;
   double incX ;
   double incY ;
   double alphai ;
   double deltai ;
   double incA ;
   double incD ;
   double Xcen ;
   double Ycen ;
   double widtha ;
   double widthd ;
   int    xnpix ;
   int    ynpix ;
   double rota ;
   double cdelz;
   double sdelz;
   String type1;
   String type2;
   double [][] CD = new double[2][2];
   double [][] ID = new double[2][2];

   public static int FK4 = 1;
   public static int GALACTIC = 2;
   public static int SUPERGALACTIC = 3 ;
   public static int ECLIPTIC = 4 ;
   public static int FK5 = 5;
   public static int ICRS = 6 ;
   public static int XYLINEAR = 7;

   // PF - Jan 2011 - Différentes valeurs des mots clés en fonction du système de coordonnées
   static final String[] RADECSYS    = { "", "FK4", "",       "",         "",        "FK5", "ICRS", "" };

   protected int system = ICRS;
   protected int proj ;

   static private double deg_to_rad = Math.PI/180. ;
   static private double rad_to_deg = 180./Math.PI ;
   //             static Vector WCSKeys = new Vector() ;
   static String[] WCSKeys = {"NAXIS1", "NAXIS2","CRPIX1","CRPIX2",
      "CRVAL1","CRVAL2","CD1_1","CD1_2","CD2_1","CD2_1","CD2_2","CDELT1",
      "CDELT2","CROTA1","CROTA2","PC001001","PC001002","PC002001",
      "PC002002","EPOCH","EQUINOX","EPOCH","CTYPE1","CTYPE2","RADECSYS",
      "PLTRAM","PLTRAH","PLTDECSN","PLTDECS","PLTDECM","PLTDECD",
      "PLTSCALE","PPO3","PPO6","AMDX1","AMDY1","AMDX2","AMDY2","AMDX3",
      "AMDY3","AMDX4","AMDY4","AMDX5","AMDY5","AMDX6","AMDY6","AMDX7",
      "AMDY8","AMDY8","AMDX9","AMDY9","AMDX10","AMDY10","AMDX11","AMDY11"
      ,"AMDX12","AMDY12","XPIXELSZ","YPIXELSZ","CNPIX1","CNPIX2"} ;

   static public final int SIN = 1;
   static public final int TAN = 2;
   static public final int ARC = 3;
   static public final int AIT = 4;
   static public final int ZEA = 5;
   static public final int STG = 6;
   static public final int CAR = 7;
   static public final int NCP = 8;
   static public final int ZPN = 9;
   static public final int SOL = 10;
   static public final int MOL = 11;
   static public final int SIP = 12 ;
   static public final int FIE = 13 ;
   static public final int TPV = 14 ;
   static public final int SINSIP = 15 ;
   static public final int GLS = 16 ;

   // Signature dans les mots clés FITS des différentes projections (l'indice dans le tableau doit correspondre
   // aux constantes statics ci-dessus
   static final String[] projType = {"", "SIN", "TAN", "ARC", "AIT", "ZEA", "STG", "CAR", "NCP", "ZPN", "SOL", "MOL","TAN-SIP","FIE" , "TPV", "SIN-SIP", "GLS" };

   /** Retourne l'indice de la signature de la projection (code 3 lettres), -1 si non trouvé */
   static int getProjType(String s) {//System.out.println("ssss "+s);
   return Util.indexInArrayOf(s, projType); }

   /** Retourne l'indice de la signature de la projection (code 3 lettres)
    * en se contentant éventuellement de ne trouver qu'une sous chaine, -1 si non trouvé */
   static int getSubProjType(String s1) { 
      int i = getProjType(s1);
      if( i>0 ) return i;
      String [] array = projType;
      String s = s1.toUpperCase();
      
      if( s.length()==0  ){
         i=TAN;
         String err = "!!! Undetermined projection: assume "+projType[i];
         if( Aladin.aladin!=null && Aladin.aladin.command!=null ) Aladin.aladin.command.printConsole(err);
         else Aladin.aladin.trace(3, err);
         return i;
      }
      
      for( i=1; i<array.length; i++ ) {

         // En attendant de supporter TANSIP
         if( s.indexOf(array[i])>=0 ) {
             
            String err = "!!! Unknown projection ["+s1+"] : assume "+projType[i];
            if( Aladin.aladin!=null && Aladin.aladin.command!=null ) Aladin.aladin.command.printConsole(err);
            else Aladin.aladin.trace(3, err);
            return i;
         }
      }

      // En attendant de supporter TNX
      if( s.indexOf("TNX")>=0 || s.indexOf("COE")>=0 ) {
         i=TAN;
         String err = "!!! Unknown projection ["+s1+"] : assume "+projType[i];
         if( Aladin.aladin!=null && Aladin.aladin.command!=null ) Aladin.aladin.command.printConsole(err);
         else Aladin.aladin.trace(3, err);
         return i;
      }
      return -1;
   }

   /** Retourne la signature de la projection (code 3 lettres) de l'indice passé en paramètre */
   static String getProjName(int indice ) { return projType[indice]; }

   //#ifndef PIERRE
   //    /** Retourne true si le type de projection est reconnu par Calib
   //     * AJOUT PF nov 09 pour supporter les projections linéaires CRVAL+CDELT, mÃ¯Â¿Â½me
   //     * si le type n'edst pas "Solar"
   //     */
   //    private boolean isUnknown(String type) {
   //       if( type==null ) return true;
   //       for( int i=0; i<PROJ_SIGNATURE.length; i++ ) {
   //          if( type.indexOf(PROJ_SIGNATURE[i])>=0 ) return false;
   //       }
   //       return true;
   //   }

   // PF. 12/06 - Modif liées à l'utilisation de la nouvelle classe de Fox
   // pour les conversions de coordonnées. On crée à l'avance les différents
   // Astroframe et Astrocoo nécessaires aux manip. pour éviter les créations
   // d'objets java à répétition
   //#endif
   static private Astroframe AF_FK4 = new FK4();
   static private Astroframe AF_FK5 = new FK5();
   static private Astroframe AF_ICRS = new ICRS();
   static private Astroframe AF_GAL = new Galactic();
   static private Astroframe AF_SGAL = new Supergal() ;
   static private Astroframe AF_ECL = new Ecliptic() ;

   /** Clonage d'une Calib */
   static public Calib copy(Calib c) {
      Calib a = new Calib();
      a.aladin = c.aladin;
      a.xyapoly = new double[12]; System.arraycopy(c.xyapoly,0,a.xyapoly,0,10);
      a.xydpoly = new double[12]; System.arraycopy(c.xydpoly,0,a.xydpoly,0,10);
      a.adypoly = new double[12]; System.arraycopy(c.adypoly,0,a.adypoly,0,10);
      a.adxpoly = new double[12]; System.arraycopy(c.adxpoly,0,a.adxpoly,0,10);
      a.epoch = c.epoch;
      a.flagepoc = c.flagepoc;
      a.equinox = c.equinox;
      a.alpha = c.alpha;
      a.delta = c.delta;
      a.yz = c.yz;
      a.xz = c.xz;
      a.focale = c.focale;
      a.Xorg = c.Xorg;
      a.Yorg = c.Yorg;
      a.incX = c.incX;
      a.incY = c.incY;
      a.alphai = c.alphai;
      a.deltai = c.deltai;
      a.incA = c.incA;
      a.incD = c.incD;
      a.Xcen = c.Xcen;
      a.Ycen = c.Ycen;
      a.widtha = c.widtha;
      a.widthd = c.widthd;
      a.xnpix = c.xnpix;
      a.ynpix = c.ynpix;
      a.rota = c.rota;
      a.cdelz = c.cdelz;
      a.sdelz = c.sdelz;
      a.type1 = c.type1;
      a.type2 = c.type2;
      a.CD = new double[2][2]; for( int i=0; i<2; i++ ) System.arraycopy(c.CD[i],0,a.CD[i],0,2);
      a.ID = new double[2][2]; for( int i=0; i<2; i++ ) System.arraycopy(c.ID[i],0,a.ID[i],0,2);
      a.system = c.system;
      a.proj = c.proj;

      return a;
   }

   static public String[] getWCSKeys() {


      //                     String [] rep = new String[WCSKeys.size()] ;
      //                     Enumeration e = WCSKeys.elements() ;
      //                     int index = 0 ;
      //                     while(e.hasMoreElements())
      //                           rep[index++] = (String)e.nextElement() ;

      return WCSKeys ; }


   public Calib flipBU() { 
      Calib a = new Calib() ;
      try {a =  (Calib)this.clone();}
      catch (Exception e) {}
      if(aladin == 1)
      {
         a.Yorg = a.Yorg + ynpix*a.incY ;
         a.incY = -a.incY ;
      }
      else
      {
         a.CD[1][1] = -a.CD[1][1] ;
         a.CD[0][1] = -a.CD[0][1] ;
         a.ID[1][1] = -a.ID[1][1] ;
         a.ID[1][0] = -a.ID[1][0] ;
         a.Ycen  = a.ynpix -(a.Ycen-1) ;  // PF nov07 => dÃ¯Â¿Â½calÃ¯Â¿Â½ de 1 aprÃ¯Â¿Â½s vÃ¯Â¿Â½rif sur la mire
      }
      return a;
   }

   public Calib flipRL() {
      Calib b = new Calib() ;
      try {b = (Calib)this.clone();}
      catch (Exception e) {//System.out.println("error");
          
      }
      if (aladin == 1)
      {
         b.Xorg = b.Xorg + xnpix*b.incX ;
         b.incX = -b.incX ;
      } else {
         b.CD[1][0] = -b.CD[1][0] ;
         b.CD[0][0] = -b.CD[0][0] ;
         b.ID[0][1] = -b.ID[0][1] ;
         b.ID[0][0] = -b.ID[0][0] ;
         b.Xcen  = b.xnpix -(b.Xcen-1) ;   // PF nov07 => dÃ¯Â¿Â½calÃ¯Â¿Â½ de 1 aprÃ¯Â¿Â½s vÃ¯Â¿Â½rif sur la mire
      }
      return b;
   }

   public Calib resize(int scale) {
      double cd00,cd01,cd10,cd11 ;
      double X_cen, Y_cen ;
      int xpix,ypix ;

      if(aladin == 1){ try {GetWCS_i() ;}
      catch (Exception e) {}}
      X_cen = Xcen /scale ;
      Y_cen = Ycen /scale  ;
      xpix = xnpix / scale ;
      ypix = ynpix / scale ;
      cd00 = CD[0][0]*scale ;
      cd01 = CD[0][1]*scale ;
      cd10 = CD[1][0]*scale ;
      cd11 = CD[1][1]*scale ;
      Calib newcal = new Calib(alphai,deltai,X_cen,Y_cen,
            xpix,ypix,cd00,cd01,cd10,cd11,equinox,epoch,proj);
      return newcal ;
   }

   public Calib resize(int scale,double cx, double cy , int xpix, int ypix) {
      double cd00,cd01,cd10,cd11 ;
      double X_cen, Y_cen ;

      X_cen = (Xcen-cx) *scale ;
      Y_cen = (Ycen-cy) *scale  ;
      xpix = xpix * scale ;
      ypix = ypix * scale ;
      cd00 = CD[0][0]/scale ;
      cd01 = CD[0][1]/scale ;
      cd10 = CD[1][0]/scale ;
      cd11 = CD[1][1]/scale ;
      Calib newcal = new Calib(alphai,deltai,X_cen,Y_cen,
            xpix,ypix,cd00,cd01,cd10,cd11,equinox,epoch,proj);
      return newcal ;
   }


   public Calib recalibrate(Coord coo[]) {
      double A =0. ;
      double B =0. ;
      double xx ;
      double yy ;
      double x = 0.;
      double y = 0. ;
      double X = 0. ;
      double Y = 0. ;
      double z = 0. ;
      double XxYy = 0.;
      double XyYx = 0. ;
      double det  ;
      double c1,c2,c3,c4 ;
      double b1,b2,b3,b4 ;
      double x0 = 0,y0 =0;
      double X_cen, Y_cen ;
      double CD00,CD10,CD01,CD11 ;
      double rot, inc_A,inc_D;
      int sign1 = 0 ;
      int sign2 = 0 ;


      int i = 0;
      try {
         if(aladin == 1) {
            GetWCS_i() ;
            det = CD[0][0]* CD[1][1]-CD[0][1]*CD[1][0] ;
            ID[0][0] = CD[1][1]/det ;
            ID[0][1] = -CD[0][1]/det ;
            ID[1][0] = -CD[1][0]/det ;
            ID[1][1] = CD[0][0]/det ;
            incA = Math.sqrt(CD[0][0]*CD[0][0]+CD[0][1]*CD[0][1]) ;
            incD = Math.sqrt(CD[1][0]*CD[1][0]+CD[1][1]*CD[1][1]) ;
            widtha = xnpix * Math.abs(incA) ;
            widthd = ynpix * Math.abs(incD) ;
            cdelz = FastMath.cos(deltai*deg_to_rad);
            sdelz = FastMath.sin(deltai*deg_to_rad);
            aladin =0;
         }
         //         System.out.println("CD "+CD[0][0]+" "+CD[1][0]+" "+CD[0][1]+" "+CD[1][1]);

         double  inx = coo[0].dx ;
         double  iny = coo[0].dy ;
         double  inX = coo[0].xstand ;
         double  inY = coo[0].ystand ;
         double AAx = 0 ;
         double BBx = 0 ;
         double Detx ;
         double AAy = 0;
         double BBy = 0 ;
         double Dety ;

         for (i =1 ; i < coo.length ; i++) {
            GetXYstand(coo[i]) ;
            Detx = -(coo[i].dx)*iny +(coo[i].dy)*inx ;
            if (Detx != 0)
            {
               AAx +=  (-iny*(coo[i].xstand)+ inX*(coo[i].dy))/Detx;
               BBx += (-(coo[i].dx)*inX +(coo[i].xstand)*inx)/Detx ;
               //           System.out.println("AAx "+AAx+" "+BBx);
               Dety = (coo[i].dx)*iny -(coo[i].dy)*inx ;
               AAy +=  (inY*(coo[i].dx) - inx*(coo[i].ystand))/Dety;
               BBy += ((coo[i].ystand)*iny -(coo[i].dy)*inY)/Dety ;

               //           System.out.println("AAy "+AAy+" "+BBy);
            }
            //      DX = coo[i].xstand -inX ;
            //      DY = coo[i].ystand -inY ;
            inx = coo[i].dx ;
            iny = coo[i].dy ;
            inX = coo[i].xstand ;
            inY = coo[i].ystand ;
            //           System.out.println("DDDDD"+Dx+" "+Dy+" "+DX+" "+DY) ;

         }
         if (AAx < 0) sign1 = -1 ;
         else sign1 = 1 ;
         if (AAy < 0) sign2 = -1 ;
         else sign2 = 1 ;
         //     System.out.println("sign "+sign1+" "+sign2);
         for (i =1 ; i < coo.length ; i++) {
            GetXYstand(coo[i]) ;
            //                  System.out.println("coo "+coo[i].dx+" "+coo[i].dy+" "+coo[i].xstand +" "+coo[i].ystand);
            // x  -= (CD[0][0]/Math.abs(CD[0][0]))*coo[i].dx;
            // y  -= (CD[1][1]/Math.abs(CD[1][1]))*coo[i].dy;
            // x  -= coo[i].dx;
            // y  -= coo[i].dy;

            // MODIF IMPORT
            // if (CD[0][0] < 0) sign1 = -1 ;
            //  else sign1 = 1 ;
            // if (CD[1][1] < 0) sign2 = -1 ;
            // else sign2 = 1 ;
            // System.out.println("sign "+sign1+" "+sign2);
            //sign1 = 1 ;
            //sign2 = 1 ;
            x  +=   sign1*coo[i].dx;
            //   x  +=   coo[i].dx;
            y  +=   sign2*coo[i].dy;
            //  y  +=   coo[i].dy;
            X  += coo[i].xstand ;
            Y  += coo[i].ystand ;
            // System.out.println(" "+coo[i].dx+" "+coo[i].dy);
            // System.out.println(" "+coo[i].xstand+" "+coo[i].ystand);
            z  +=
                  coo[i].dx * coo[i].dx + coo[i].dy * coo[i].dy ;
            //if (x*X > 0) sign1 = 1 ;
            //else sign1 = -1 ;
            //sign2 = 1 ;
            //sign1 = 1 ;
            //if (y*Y>0)  sign2 = 1 ;
            //else sign2 = -1 ;
            //  XxYy -=  (CD[0][0]/Math.abs(CD[0][0]))*coo[i].dx * coo[i].xstand +  (CD[1][1]/Math.abs(CD[1][1]))*coo[i].dy * coo[i].ystand ;
            //  XyYx += (CD[0][0]/Math.abs(CD[0][0]))* coo[i].dx * coo[i].ystand - (CD[1][1]/Math.abs(CD[1][1]))*coo[i].dy * coo[i].xstand ;
            // XxYy -=  sign1 * coo[i].dx * coo[i].xstand + sign2 * coo[i].dy * coo[i].ystand ;
            XxYy +=  sign1 * coo[i].dx * coo[i].xstand + sign2 * coo[i].dy * coo[i].ystand ;
            //  XyYx +=  sign1 * coo[i].dx * coo[i].ystand - sign2 * coo[i].dy * coo[i].xstand ;
            XyYx += - sign1 * coo[i].dx * coo[i].ystand + sign2 * coo[i].dy * coo[i].xstand ;
            // System.out.println(" "+coo[i].dx * coo[i].xstand+" "+coo[i].dy * coo[i].ystand);
            // System.out.println(" "+coo[i].dx * coo[i].ystand+" "+coo[i].dy * coo[i].xstand);
            // System.out.println("XxYy "+XxYy +" "+XyYx);
         }
         //x  /= coo.length * (-sign1) ;
         x /= coo.length ;
         // y  /= coo.length * (-sign2) ;
         y /= coo.length ;
         z  /= coo.length ;
         b1 = X/coo.length ;
         b2 = Y/coo.length ;
         b3 = XxYy/coo.length ;
         b4 = XyYx/coo.length ;
         //  System.out.println("xyz "+x+" "+y+" "+z) ;
         //   System.out.println("b1b2b3b4 "+b1 +" "+b2 +" "+b3 +" "+b4);
         c1 = x*x*x + x*y*y - x*z ;
         // System.out.println("xxx "+x*x*x +" "+x*y*y +" "+x*z);
         c2 = x*x*y + y*y*y - y*z ;
         c3 = -x*x - y*y +z ;
         c4 = -x*x*z - y*y*z + z*z ;
         // System.out.println("c1c2c3c4 "+c1 +" "+c2 +" "+c3 +" "+c4);
         // System.out.println("b1c1 b2c2 b3c3 "+b1*c1+" "+b2*c2+" "+b3*c3) ;
         // System.out.println("b2c1 b1c2 b4c3 "+b2*c1+" "+b1*c2+" "+b4*c3) ;

         det = x*x*x*x + 2* x*x*y*y +y*y*y*y -2* x*x*z -2* y*y*z +z*z ;
         A = (b1*c1 +b2*c2 +b3*c3)/det ;
         // System.out.println("det "+det) ;
         B = (-b2*c1 +b1*c2 +b4*c3)/det ;
         // B = (b2*c1 -b1*c2 +b4*c3)/det ;
         x0 = (b3*c1 +b4*c2 +b1*c4)/det ;
         y0 = (-b4*c1 +b3*c2 +b2*c4)/det ;
         // System.out.println("ABxy0 "+A+" "+B+" "+x0+" "+y0);

         //X_cen = Xcen + (A*x0-B*y0)*(CD[0][0]/Math.abs(CD[0][0]))/(A*A+B*B) ;
         //Y_cen =  Ycen + (B*x0 +A*y0)*(CD[1][1]/Math.abs(CD[1][1]))/(A*A+B*B)  ;
         //   X_cen = Xcen - (A*x0-B*y0)/(A*A+B*B) ;
         X_cen = Xcen +sign1*(-A*x0+B*y0)/(A*A+B*B);
         Y_cen =  Ycen + sign2*(-B*x0 -A*y0)/(A*A+B*B)  ;
         // System.out.println("ABxy0 "+A+" "+B+" "+X_cen+" "+Y_cen);
         // CD00 = -A*(CD[0][0]/Math.abs(CD[0][0]))*rad_to_deg ;
         // CD10 = B*(CD[0][0]/Math.abs(CD[0][0]))*rad_to_deg ;
         // CD01 = -B*(CD[1][1]/Math.abs(CD[1][1]))*rad_to_deg ;
         // CD11 = -A*(CD[1][1]/Math.abs(CD[1][1]))*rad_to_deg ;
         // System.out.println("AB "+A*rad_to_deg+" "+B*rad_to_deg);
         // CD00 = -sign1 * A * rad_to_deg ;
         CD00 = sign1 * A*rad_to_deg ; 
         CD10 = -sign1 * B * rad_to_deg ;
         CD01 =sign2 * B * rad_to_deg ;
         //CD01 = - sign1 * B * rad_to_deg ;
         //  CD11 = -sign2 * A*rad_to_deg ;
         CD11 = sign2 * A*rad_to_deg ;

         //                      inc_A = Math.sqrt(CD00*CD00+CD01*CD01)*(CD00/Math.abs(CD00)) ;
         //                      inc_D = Math.sqrt(CD10*CD10+CD11*CD11)*(CD11/Math.abs(CD11)) ;
         // Et la rotation
         //                      rot =  rad_to_deg*Math.acos(CD00/inc_A);

         //                   widtha = xnpix * Math.abs(incA) ;
         //                   widthd = ynpix * Math.abs(incD) ;
         // cos delta au centre de l'image
         //                   cdelz = FastMath.cos((deltai/180.)*Math.PI);
         //                   sdelz = FastMath.sin((deltai/180.)*Math.PI);
         //                   cdelz = FastMath.cos(deltai*deg_to_rad);
         //                   sdelz = FastMath.sin(deltai*deg_to_rad);

         //                 Calib newcal = new Calib (alphai,deltai,X_cen,Y_cen,
         //                                   (double)xnpix,xnpix*inc_A*60.,rot,proj-1,true);
         //             System.out.println(CD00+" "+CD01+" "+CD10+" "+CD11);
         Calib newcal = new Calib(alphai,deltai,X_cen,Y_cen,
               xnpix,ynpix,CD00,CD01,CD10,CD11,equinox,epoch,proj);

         return newcal ;
         //             System.out.println(coo[i].x+" "+coo[i].y+" "+coo[i].xstand +" "+coo[i].ystand);}
      } catch (Exception e) {}
      return this; 
   }

   /** Creation de la calibration.
    * @param Methode triviale
    */
   public Calib (double ra,double de, double cx, double cy,
         int xpix, int ypix, double cd00, double cd01, double cd10, double cd11, double equin, double epo, int proje) {
      aladin = 0 ;
      xnpix = xpix ;
      ynpix = ypix ;
      Xcen = cx ;
      Ycen = cy ;
      alphai = ra ;
      deltai = de ;
      CD[0][0] = cd00 ;
      CD[0][1] = cd01 ;
      CD[1][0] = cd10 ;
      CD[1][1] = cd11 ;
      equinox = equin ;
      epoch = epo ;
      proj  = proje ;
      incA = Math.sqrt(CD[0][0]*CD[0][0]+CD[0][1]*CD[0][1])*(CD[0][0]/Math.abs(CD[0][0])) ;
      incD = Math.sqrt(CD[1][0]*CD[1][0]+CD[1][1]*CD[1][1])*(CD[1][1]/Math.abs(CD[1][1])) ;
      //                 if (CD[1][1] < 0)
      //                 rota = 180. - Math.acos(CD[0][0]/incA)*(180./Math.PI) ;
      //                 else
      //                 rota = Math.acos(CD[0][0]/incA)*(180./Math.PI);
      rota = Math.atan2(CD[0][1]/incA,CD[1][1]/incD)*(180./Math.PI) ;
      widtha = xnpix * Math.abs(incA) ;
      widthd = ynpix * Math.abs(incD) ;
      cdelz = FastMath.cos(deltai*deg_to_rad);
      sdelz = FastMath.sin(deltai*deg_to_rad);
      double det = CD[0][0]* CD[1][1]-CD[0][1]*CD[1][0] ;
      ID[0][0] = CD[1][1]/det ;
      ID[0][1] = -CD[0][1]/det ;
      ID[1][0] = -CD[1][0]/det ;
      ID[1][1] = CD[0][0]/det ;

      // PF sept 2010 - C'est plus generique comme ca
      type1 = "RA---"+projType[proj];
      type2 = "DEC--"+projType[proj];
      //                switch(proj)
      //                 {
      //                  case 1:
      //                  type1 = "'RA---SIN'" ;
      //                  type2 = "'DEC--SIN'" ;
      //                  break ;
      //                  case 2:
      //                  type1 = "'RA---TAN'" ;
      //                  type2 = "'DEC--TAN'" ;
      //                  break ;
      //                  case 3:
      //                  type1 = "'RA---ARC'" ;
      //                  type2 = "'DEC--ARC'" ;
      //                  break ;
      //                  case 4:
      //                  type1 = "'RA---AIT'" ;
      //                  type2 = "'DEC--AIT'" ;
      //                  break ;
      //                  case 5:
      //                  type1 = "'RA---ZEA'" ;
      //                  type2 = "'DEC--ZEA'" ;
      //                  break ;
      //                  case 6:
      //                  type1 = "'RA---STG'" ;
      //                  type2 = "'DEC--STG'" ;
      //                  break ;
      //                  case 7:
      //                  type1 = "'RA---CAR'" ;
      //                  type2 = "'DEC--CAR'" ;
      //                  break ;
      //                 }

      flagepoc = 1 ;
   }

   public Calib (double ra,double de, double cx, double cy,
         double width, double height, double radius, double radius1, double rot, int proje,  boolean sym,
         int systeme) {
      aladin = 0 ;
      xnpix = (int)width ;
      ynpix = (int)height ;
      Xcen = cx ;
      // PF1          Ycen = height-cy ;
      Ycen = cy ;
      alphai = ra ;
      deltai = de ;
      cdelz = FastMath.cos(deltai*deg_to_rad);
      sdelz = FastMath.sin(deltai*deg_to_rad);
      double cosdd = FastMath.cos(rot*deg_to_rad);
      double sindd = FastMath.sin(rot*deg_to_rad);
      // System.out.println ("New width "+width+"height "+height+"radius "+radius+"radius1 "+radius+"rot"+rot);
      radius /= 60. ;
      radius1 /= 60. ;
      // System.out.println ("New width "+width+"height "+height+"radius "+radius+"radius1 "+radius+"rot"+rot);
      if(sym == false)
         incA = - radius/width ;
      else incA = radius/width ;
      incD = radius1/height ;
      widtha =  radius ;
      widthd =  radius1 ;
      // Pourquoi ce Math.abs (FB 2/09/2005)
      //                CD[0][0] = incA*Math.abs(cosdd) ;
      //                CD[0][1] = incA*Math.abs(sindd) ;
      CD[0][0] = incA*cosdd ;
      CD[0][1] = incA*sindd ;
      CD[1][0] = -incD*sindd ;
      CD[1][1] = incD*cosdd ;
      // System.out.println ("New Xcen "+Xcen+" Ycen "+Ycen+"incA "+incA+"incD "+incD+" "+CD[0][0]+"xnpix "+xnpix+"ynpix "+ynpix);
      double   det = CD[0][0]* CD[1][1]-CD[0][1]*CD[1][0] ;
      ID[0][0] = CD[1][1]/det ;
      ID[0][1] = -CD[0][1]/det ;
      ID[1][0] = -CD[1][0]/det ;
      ID[1][1] = CD[0][0]/det ;
      rota = rot ;
      equinox = 2000.0 ;
      epoch = 2000.0;
      flagepoc = 0 ;
      proj = proje ;
      system=systeme;

      // PF - sept 2010 - C'est plus generique comme cela
      type1 = "RA---"+projType[proj];
      type2 = "DEC--"+projType[proj];
      //                switch(proj)
      //                 {
      //                  case 1:
      //                  type1 = "'RA---SIN'" ;
      //                  type2 = "'DEC--SIN'" ;
      //                  break ;
      //                  case 2:
      //                  type1 = "'RA---TAN'" ;
      //                  type2 = "'DEC--TAN'" ;
      //                  break ;
      //                  case 3:
      //                  type1 = "'RA---ARC'" ;
      //                  type2 = "'DEC--ARC'" ;
      //                  break ;
      //                  case 4:
      //                  type1 = "'RA---AIT'" ;
      //                  type2 = "'DEC--AIT'" ;
      //                  break ;
      //                  case 5:
      //                  type1 = "'RA---ZEA'" ;
      //                  type2 = "'DEC--ZEA'" ;
      //                  break ;
      //                  case 6:
      //                  type1 = "'RA---STG'" ;
      //                  type2 = "'DEC--STG'" ;
      //                  break ;
      //                  case 7:
      //                  type1 = "'RA---CAR'" ;
      //                  type2 = "'DEC--CAR'" ;
      //                  break ;
      //                 }
   }

   public Calib (double ra,double de, double cx, double cy,
         double width,  double radius,  double rot, int proj,  boolean sym ) {
      this(ra,de,cx,cy,width,width,radius,radius,rot,proj,sym,FK5);         
   }

   /** Creation de la calibration.
    * @param dataflux le flux de donnees contenant la calibration
    */
   public Calib ( DataInputStream dataflux) throws Exception {
      String s ;
      String st ;
      StringTokenizer tok;
      int sign = 1;

      // Calib vient d'aladin ;
      aladin = 1 ;
      // L'equinoxe vaut 2000
      equinox = 2000.0 ;
      // L'epoque
      s=dataflux.readLine();
      epoch = (new Double(s)).doubleValue();
      flagepoc = 1 ;

      // focale du telescope
      s = dataflux.readLine();
      //              System.out.println(s);
      focale = (new Double(s)).doubleValue();
      //              System.out.println("focale "+focale);

      // alpha centre de plaque
      s = dataflux.readLine();
      //              System.out.println(s);
      tok =  new StringTokenizer (s," ");
      st = tok.nextToken();
      //              System.out.println(st);
      alpha =  (new Double(st)).doubleValue();
      st = tok.nextToken();
      //              System.out.println(st);
      alpha += ((new Double(st)).doubleValue())/60.;
      st = tok.nextToken();
      //              System.out.println(st);
      alpha += ((new Double(st)).doubleValue())/3600.;
      alpha *= 15.;
      //              System.out.println("alpha = "+alpha);

      // delta centre de plaque
      s = dataflux.readLine();
      //               System.out.println(s);
      tok =  new StringTokenizer (s," ");
      st = tok.nextToken();
      //               System.out.println(st);
      if (st.startsWith( "-"))sign = -1;
      delta =  (new Double(st)).doubleValue();
      if (sign == -1)  delta = -delta ;
      st = tok.nextToken();
      //               System.out.println(st);
      delta += ((new Double(st)).doubleValue())/60.;
      st = tok.nextToken();
      //               System.out.println(st);
      delta += ((new Double(st)).doubleValue())/3600.;
      if (sign == -1)  delta = -delta ;
      //               System.out.println("delta = "+delta);

      // x y centre de plaque (en mm).
      s = dataflux.readLine();
      //               System.out.println(s);
      tok =  new StringTokenizer (s," ");
      st = tok.nextToken();
      //               System.out.println(st);
      yz =  (new Double(st)).doubleValue();
      st = tok.nextToken();
      //               System.out.println(st);
      xz =  (new Double(st)).doubleValue();
      //               System.out.println("xz "+xz+" yz "+yz);


      // six champs nonj utilsses
      for ( int i=0; i<6 ; i++)
         dataflux.readLine();

      // coeffs du polynome alpha delta --> x y
      for ( int i=0; i<10 ; i++)
      {
         s = dataflux.readLine();
         //               System.out.println(s);
         tok =  new StringTokenizer (s," ");
         st = tok.nextToken();
         //               System.out.println(st);
         adypoly[i] = (new Double(st)).doubleValue();
         st = tok.nextToken();
         //               System.out.println(st);
         adxpoly[i] = (new Double(st)).doubleValue();
         //               System.out.println("adxy "+adxpoly[i]+" "+adypoly[i]);
      }

      // coeffs du polynome x y  --> alpha delta
      for ( int i=0; i<12 ; i++)
      {
         s = dataflux.readLine();
         //               System.out.println(s);
         tok =  new StringTokenizer (s," ");
         st = tok.nextToken();
         //               System.out.println(st);
         xyapoly[i] = (new Double(st)).doubleValue();
         st = tok.nextToken();
         //               System.out.println(st);
         xydpoly[i] = (new Double(st)).doubleValue();
         //               System.out.println("xyad "+xyapoly[i]+" "+xydpoly[i]);
      }

      // dx dy d'un pixel en micron
      s = dataflux.readLine();
      //               System.out.println(s);
      tok =  new StringTokenizer (s," ");
      st = tok.nextToken();
      //               System.out.println(st);
      incY = (new Double(st)).doubleValue();
      st = tok.nextToken();
      //               System.out.println(st);
      incX = (new Double(st)).doubleValue();
      //               System.out.println("inc "+incX+" "+incY);

      // x y coin de l'image ( en microns)
      s = dataflux.readLine();
      //               System.out.println(s);
      tok =  new StringTokenizer (s," ");
      st = tok.nextToken();
      //               System.out.println(st);
      st = tok.nextToken();
      //               System.out.println(st);
      Yorg = (new Double(st)).doubleValue();
      st = tok.nextToken();
      //               System.out.println(st);
      Xorg = (new Double(st)).doubleValue();
      //               System.out.println("org "+Xorg+" "+Yorg);

      // alpha delta centre image , largeur hauteur (degres decimaux)
      s = dataflux.readLine();
      //               System.out.println(s);
      tok =  new StringTokenizer (s," ,=/");
      st = tok.nextToken();
      //               System.out.println(st);
      st = tok.nextToken();
      //               System.out.println(st);
      alphai = (new Double(st)).doubleValue();
      st = tok.nextToken();
      //               System.out.println(st);
      deltai = (new Double(st)).doubleValue();
      st = tok.nextToken();
      //               System.out.println(st);
      st = tok.nextToken();
      //               System.out.println(st);
      widtha = (new Double(st)).doubleValue();
      //               System.out.println("WIDTHA "+widtha);
      st = tok.nextToken();
      //               System.out.println(st);
      widthd = (new Double(st)).doubleValue();
      //               System.out.println("center width "+alphai+" "+deltai+" "+widtha+" "+widthd);

      // nombre de pixels en x et en y de l'image
      s = dataflux.readLine();
      //               System.out.println(s);
      tok =  new StringTokenizer (s," ,=/");
      st = tok.nextToken();
      //              System.out.println(st);
      st = tok.nextToken();
      //               System.out.println(st);
      xnpix = (new Integer(st)).intValue();
      st = tok.nextToken();
      //               System.out.println(st);
      ynpix = (new Integer(st)).intValue();
      //               System.out.println("npix "+xnpix+" "+ynpix);

      //               cdelz = FastMath.cos((delta/180.)*Math.PI);
      //               sdelz = FastMath.sin((delta/180.)*Math.PI);

      cdelz = FastMath.cos(delta*deg_to_rad);
      sdelz = FastMath.sin(delta*deg_to_rad);
      incA =  (xyapoly[2]*incX/focale/1000)*rad_to_deg ;
      incD =  (xydpoly[1]*incY/focale/1000)*rad_to_deg  ;
      //              System.out.println("incAD "+incA+" "+incD );
      Coord cc = new Coord() ;
      cc.al = alphai ;
      cc.del = deltai ;
      GetXY(cc) ;
      Xcen = cc.x ;
      Ycen = cc.y ;
   }


   public Calib () {}
   public Calib (HeaderFits hf) throws Exception {
      //ifndef PIERRE
      //              boolean flagSolar=false;
      //#endif /* ! PIERRE */
      double det ;     

      // Calib ne vient pas d'aladin ;
      aladin = 0 ;
      // Requis nombre de pixels en X et Y
      xnpix = hf.getIntFromHeader("NAXIS1  ");
      ynpix = hf.getIntFromHeader("NAXIS2  ");
      //                   if(flagadd == 1) WCSKeys.addElement("NAXIS1  ") ; 
      String plate = hf.getStringFromHeader("PLATE") ; 
      if (plate != null) aladin = 3 ; 
      else   aladin = 0 ;
    //  System.out.println("aladin "+aladin);
      
      try {
         alpha = hf.getDoubleFromHeader("PLTRAS  ");
         Dss( hf) ;
         return ;
      }
      
      catch (Exception e0) {
         // Requis: pixel central      
         Xcen = hf.getDoubleFromHeader("CRPIX1  ");
         Ycen = hf.getDoubleFromHeader("CRPIX2  ");

         // Requis Type de projection
         type1 = hf.getStringFromHeader("CTYPE1  ");
         type2 = hf.getStringFromHeader("CTYPE2  ");

         // Requis: position (alph delt) du centre sauf si CTYPE SOLAR
         // Problème dans le cas ou le type est UNknown ? 
         if( type1.startsWith("Solar") || type1.startsWith("solar") ) { 
            try {
               alphai = hf.getDoubleFromHeader("CRVAL1  ");
               deltai = hf.getDoubleFromHeader("CRVAL2  ");  }
            catch (Exception e00) {
               alphai = 0.0 ;
               deltai = 0.0 ;
            }
         } else { 

            alphai = hf.getDoubleFromHeader("CRVAL1  ");
            deltai = hf.getDoubleFromHeader("CRVAL2  ");

         }

         //                   if(flagadd == 1) WCSKeys.addElement("CRVAL2  ") ;
         //                  System.out.println("alphai "+alphai+"deltai "+deltai);
         try {                     
            // Version Finale du WCS matrice CDn_j de transformation

            CD[0][0] = hf.getDoubleFromHeader("CD1_1    ");


            //                   if(flagadd == 1) WCSKeys.addElement("CD1_1    ") ;
            try {
               CD[0][1] = hf.getDoubleFromHeader("CD1_2    ");
            }
            catch (Exception e112) {CD[0][1] = 0.0 ;}
            //                   if(flagadd == 1) WCSKeys.addElement("CD1_2    ") ;
            try {
               CD[1][0] = hf.getDoubleFromHeader("CD2_1    ");
            }
            catch (Exception e113) {CD[1][0] = 0.0 ;}
            //                   if(flagadd == 1) WCSKeys.addElement("CD2_1    ") ;

            CD[1][1] = hf.getDoubleFromHeader("CD2_2    ");


            //if(flagadd == 1) WCSKeys.addElement("CD2_2    ") ;
            // Dans ce cas on recalcule les increments par pixel

// MODIF PIERRE POUR EVITER LA DIVISION PAR ZERO
//                  incA = Math.sqrt(CD[0][0]*CD[0][0]+CD[0][1]*CD[0][1])*(CD[0][0]/Math.abs(CD[0][0])) ;
//                  incD = Math.sqrt(CD[1][0]*CD[1][0]+CD[1][1]*CD[1][1])*(CD[1][1]/Math.abs(CD[1] [1])) ;
            int sgnA = CD[0][0]<0 ? -1 : 1;
            int sgnD = CD[1][1]<0 ? -1 : 1;
            incA = Math.sqrt(CD[0][0]*CD[0][0]+CD[0][1]*CD[0][1])*sgnA ;
            incD = Math.sqrt(CD[1][0]*CD[1][0]+CD[1][1]*CD[1][1])*sgnD ;
            // Et la rotation
            //                   if (CD[1][1] < 0)
            //                    rota = 180. + Math.acos(CD[0][0]/incA)*(180./Math.PI) ;
            //                    else
            //                     rota = Math.acos(CD[0][0]/incA)*(180./Math.PI);
            rota = Math.atan2(CD[0][1]/incA, CD[1][1]/incD)*180.0/Math.PI;
            //                   System.out.println("CD "+CD[0][0]+" "+CD[0][1]+" "+CD[1][0]+" "+CD[1][1]);
            //                  System.out.println("incA "+incA);
         }
         catch (Exception e1) {
            // Dans les autres versions du WCS on a toujours
            // les increments                     
            incA = hf.getDoubleFromHeader("CDELT1  ");
            //if(flagadd == 1) WCSKeys.addElement("CDELT1  ") ;
            incD = hf.getDoubleFromHeader("CDELT2  ");
            //                   if(flagadd == 1) WCSKeys.addElement("CDELT2  ") ;
            // System.out.println("incA "+incA+"incD "+incD);
            // System.out.println("CRPIX1"+Xcen+"CRPIX2 "+Ycen);
            try {
               //  Et soit l'angle de rotation (1 ou 2) ...
               double rota1=0,rota2=0 ;                         
               try {                         
                  rota1 = hf.getDoubleFromHeader("CROTA1  ");
                  //if(flagadd == 1) WCSKeys.addElement("CROTA1  ") ;
                  //                        System.out.println("CROTA1 "+rota1);
                  rota2 = hf.getDoubleFromHeader("CROTA2  ");
                  //                   if(flagadd == 1) WCSKeys.addElement("CROTA2  ") ;
                  //                        System.out.println("CROTA2 "+rota2);
               }  catch (Exception e6) {
                  try {                           
                     rota1 = hf.getDoubleFromHeader("CROTA1  ");
                     //if(flagadd == 1) WCSKeys.addElement("CROTA1  ") ;
                     //                       System.out.println("CROTA1 "+rota1);
                  } catch (Exception e7) {                       
                     rota2 = hf.getDoubleFromHeader("CROTA2  ");
                     //if(flagadd == 1) WCSKeys.addElement("CROTA2  ") ;
                     //                      ("CROTA2 "+rota2);
                  }
               }


               rota = rota1 ;
               if (rota1 == 0) rota = rota2 ;
               else if (rota2 != 0) rota = (rota1+rota2 )/2 ;
               // Auquel cas on calcule la matrice de transformation
               CD[0][0] = incA*FastMath.cos((rota/180.)*Math.PI) ;
               CD[0][1] = -incD*FastMath.sin((rota/180.)*Math.PI) ;
               CD[1][0] = incA*FastMath.sin((rota/180.)*Math.PI) ;
               CD[1][1] = incD*FastMath.cos((rota/180.)*Math.PI) ;
               //System.out.println("CD "+CD[0][0]+"CD "+CD[0][1]);
            }
            catch (Exception e2) {
               try {
                  // Soit la matrice de rotation,
                  // d'ou l'on deduit la matrice
                  // de transformation  ...
                  CD[0][0] = hf.getDoubleFromHeader("PC001001")*incA;
                  CD[0][1] = hf.getDoubleFromHeader("PC001002")*incA;
                  CD[1][0] = hf.getDoubleFromHeader("PC002001")*incD;
                  CD[1][1] = hf.getDoubleFromHeader("PC002002")*incD;
                  //if(flagadd == 1) WCSKeys.addElement("PC001001") ;
                  //if(flagadd == 1) WCSKeys.addElement("PC001002") ;
                  //if(flagadd == 1) WCSKeys.addElement("PC002001") ;
                  //if(flagadd == 1) WCSKeys.addElement("PC002002") ;
                  // Et l'angle de rotation
                  rota=Math.acos(hf.getDoubleFromHeader("PC001001"))*(180./Math.PI);
               }
               catch (Exception e22) {
                  try {
                     // Soit la matrice de rotation,
                     // d'ou l'on deduit la matrice
                     // de transformation  ...
                     try {
                        CD[0][0] = hf.getDoubleFromHeader("PC1_1")*incA;
                     }
                     catch (Exception e222) { CD[0][0] = incA ; }
                     try {
                        CD[0][1] = hf.getDoubleFromHeader("PC1_2")*incA;
                     }
                     catch (Exception e223) { CD[0][1] = 0.0; }
                     try {
                        CD[1][0] = hf.getDoubleFromHeader("PC2_1")*incD;
                     }
                     catch (Exception e224) { CD[1][0] = 0.0 ; }
                     try {
                        CD[1][1] = hf.getDoubleFromHeader("PC2_2")*incD;
                     }
                     catch (Exception e225) { CD[1][1] = incA ; }

                     //if(flagadd == 1) WCSKeys.addElement("PC001001") ;
                     //if(flagadd == 1) WCSKeys.addElement("PC001002") ;
                     //if(flagadd == 1) WCSKeys.addElement("PC002001") ;
                     //if(flagadd == 1) WCSKeys.addElement("PC002002") ;
                     // Et l'angle de rotation
                     rota=Math.acos(hf.getDoubleFromHeader("PC1_1"))*(180./Math.PI);
                  }
                  catch (Exception e3) {
                     // s'il n'ya ni angle de rotation ni matrice
                     // de rotation la transformation est une homothetie
                     // modifie pour DENIS
                     rota = 0.0 ;
                     CD[0][0] = incA ;
                     CD[0][1] = 0 ;
                     CD[1][0] = 0 ;
                     CD[1][1] = incD ;
                     // ci dessous pour tenir compte des vieilles
                     // images DENIS !!!!
                     String origin ;
                     origin = hf.getStringFromHeader("ORIGIN  ");
                     if (origin!=null && origin.startsWith("DeNIS"))
                     {
                        if (CD[0][0] >0 ) CD[0][0] = - CD[0][0] ;
                        if (CD[1][1] >0 ) CD[1][1] = - CD[1][1] ;
                     }
                  } // Fin de e3
               } // Fin de e22
            } // Fin de e2
         } // Fin de e1
      } // Fin de e0
      //  System.out.println("ICI");
      try {
         epoch = hf.getDoubleFromHeader("EPOCH   ");
         //if(flagadd == 1) WCSKeys.addElement("EPOCH   ") ;
         flagepoc = 1 ;
      }
      catch(Exception e6) {
         epoch = 0.0 ;
      }

      try {

         equinox = hf.getDoubleFromHeader("EQUINOX ");
         //if(flagadd == 1) WCSKeys.addElement("EQUINOX ") ;

      }
      catch(Exception e5) {
         try {
            // Epoque d'observation
            equinox = hf.getDoubleFromHeader("EPOCH  ") ;
            //if(flagadd == 1) WCSKeys.addElement("EPOCH   ") ;
            epoch = equinox ;
            flagepoc = 1 ;
         }
         catch (Exception e4) {
            equinox = 2000.0 ;
            epoch = 2000.0;
            flagepoc = 0 ;
         }
      }

      //if(flagadd == 1) WCSKeys.addElement("CTYPE1  ") ;
      //if(flagadd == 1) WCSKeys.addElement("CTYPE2  ") ;
      //                   System.out.println(type1+" "+type2);
      if (type1.startsWith("DEC"))
      {
         double tmp_invert ;
         tmp_invert = deltai ;
         deltai = alphai ;
         alphai = tmp_invert ;

         tmp_invert = CD[0][0] ;
         CD[0][0] = CD[1][0] ;
         CD[1][0] = tmp_invert ;

         tmp_invert = CD[1][1] ;
         CD[1][1] = CD[0][1] ;
         CD[0][1] = tmp_invert ;

         tmp_invert =  incA ;
         incA = incD ;
         incD = tmp_invert ;

         //   System.out.println("DEC");
      }
      try {
         String  Syst = hf.getStringFromHeader("RADECSYS") ;
         //if(flagadd == 1) WCSKeys.addElement("RADECSYS") ;
         if (Syst.indexOf("ICRS")>=0) system = ICRS;
         if (Syst.indexOf("FK5")>=0) system = FK5 ;
         if (Syst.indexOf("FK4")>=0) system = FK4 ;
         //        System.out.println("system "+system);
      } catch(Exception e10) {}
      if (type1.startsWith("ELON"))
      {
         //                       System.out.println("ELON");
         system = ECLIPTIC ;
      }
      if (type1.startsWith("GLON"))
      {
         //                     System.out.println(type1);
         //                     System.out.println(type2) ;
         //                       System.out.println("GLON");
         system = GALACTIC ;
      }
      if (type1.startsWith("SLON"))
      {
         //                       System.out.println("SLON");
         system = SUPERGALACTIC ;
      }
      // plus de flagSolar pour tenir compte de plusieurs cas ?
      if (type1.startsWith("Solar"))
      {
         system = XYLINEAR ;
      }
      if (type1.startsWith("solar"))
      {
         system = XYLINEAR ;
      }  
      if ((equinox == 1950.0)&&( system != GALACTIC) &&  ( system != ECLIPTIC) && ( system != SUPERGALACTIC)
            && (system != XYLINEAR)) system = FK4 ;
      // System.out.println("Avant les PV") ;
      try {
         adxpoly[0] = hf.getDoubleFromHeader("PV2_0   ");
         xydpoly[0] = adxpoly[0] ;
         
      } catch(Exception e11) {}
      try {
         adxpoly[1] = hf.getDoubleFromHeader("PV2_1   "); 
         xydpoly[1] = adxpoly[1] ;
      } catch(Exception e11) {}
      try {
         adxpoly[2] = hf.getDoubleFromHeader("PV2_2   "); 
         xydpoly[2] = adxpoly[2] ;
      } catch(Exception e11) {}
      try {
         adxpoly[3] = hf.getDoubleFromHeader("PV2_3   ");
         xydpoly[3] = adxpoly[3] ;
      } catch(Exception e11) {}
      try {
         adxpoly[4] = hf.getDoubleFromHeader("PV2_4   "); 
         xydpoly[4] = adxpoly[4] ;
      } catch(Exception e11) {}
      try {
         adxpoly[5] = hf.getDoubleFromHeader("PV2_5   ");
         xydpoly[5] = adxpoly[5] ;
      } catch(Exception e11) {}
      try {
         adxpoly[6] = hf.getDoubleFromHeader("PV2_6   ");   
         xydpoly[6] = adxpoly[6] ;
      } catch(Exception e11) {}
      try {
         adxpoly[7] = hf.getDoubleFromHeader("PV2_7   ");  
         xydpoly[7] = adxpoly[7] ;
      } catch(Exception e11) {}
      try {
         adxpoly[8] = hf.getDoubleFromHeader("PV2_8   "); 
         xydpoly[8] = adxpoly[8] ;
      } catch(Exception e11) {}
      try {
         adxpoly[9] = hf.getDoubleFromHeader("PV2_9   ");
         xydpoly[9] = adxpoly[9] ;
         //                  System.out.println("adx: "+adxpoly[1]+" "+adxpoly[3]);
      } catch(Exception e11) {}
      try {
          adxpoly[10] = hf.getDoubleFromHeader("PV2_10   ");
          xydpoly[10] = adxpoly[10] ;
          //                  System.out.println("adx: "+adxpoly[1]+" "+adxpoly[3]);
       } catch(Exception e11) {}
    //  System.out.println("Milieu des PV") ;
      try {
         xyapoly[0] = hf.getDoubleFromHeader("PV1_0   ");  
      } catch(Exception e11) {}
      try {
         xydpoly[0] = hf.getDoubleFromHeader("PV2_0   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[1] = hf.getDoubleFromHeader("PV1_1   "); 
      } catch(Exception e11) {}
      try {
         xydpoly[1] = hf.getDoubleFromHeader("PV2_1   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[2] = hf.getDoubleFromHeader("PV1_2   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[2] = hf.getDoubleFromHeader("PV2_2   "); 
      } catch(Exception e11) {}
      try {
         xyapoly[3] = hf.getDoubleFromHeader("PV1_3   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[3] = hf.getDoubleFromHeader("PV2_3   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[4] = hf.getDoubleFromHeader("PV1_4   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[4] = hf.getDoubleFromHeader("PV2_4   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[5] = hf.getDoubleFromHeader("PV1_5   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[5] = hf.getDoubleFromHeader("PV2_5   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[6] = hf.getDoubleFromHeader("PV1_6   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[6] = hf.getDoubleFromHeader("PV2_6   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[7] = hf.getDoubleFromHeader("PV1_7   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[7] = hf.getDoubleFromHeader("PV2_7   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[8] = hf.getDoubleFromHeader("PV1_8   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[8] = hf.getDoubleFromHeader("PV2_8   ");   
      } catch(Exception e11) {}
      try {
         xyapoly[9] = hf.getDoubleFromHeader("PV1_9   ");   
      } catch(Exception e11) {}
      try {
         xydpoly[9] = hf.getDoubleFromHeader("PV2_9   ");  
      } catch(Exception e11) {}
      try {
         xyapoly[10] = hf.getDoubleFromHeader("PV1_10   "); 
      } catch(Exception e11) {}
      try {
         xydpoly[10] = hf.getDoubleFromHeader("PV2_10   ");  
      } catch(Exception e12) {}
      try {
          xyapoly[11] = hf.getDoubleFromHeader("PV1_11   ");  
       } catch(Exception e13) {}
      try {
          xydpoly[11] = hf.getDoubleFromHeader("PV2_11   ");  
       } catch(Exception e13) {}
                  
                                  
    //              System.out.println(adxpoly[1]+" "+adxpoly[3]+" "+xydpoly[1]+" "+xydpoly[3]);
           //     System.out.println(adxpoly[25]+" "+adxpoly[27]+" "+xydpoly[25]+" "+xydpoly[27]);
      // else System.out.println("RA");

      // PF - sept 2010 - C'est plus generique comme cela
      // PF - mai 2013 - on vérifie les deux axes (pour rotation cube)
      try { proj = getSubProjType(type2.length()>=5 ? type2.substring(5):""); } 
      catch( Exception e ) { proj=-1; }
    
      if( proj!=-1 ) {
         try { proj = getSubProjType(type1.length()>=5 ? type1.substring(5):""); } 
         catch( Exception e ) { proj=-1; }
      }
      // System.out.println("proj "+proj) ;
      // Petit patch pour EGRET et autres vieilles missions
      if( proj==-1 && hf.getStringFromHeader("GRIDTYPE")!=null ) {
         proj=CAR; 
         System.err.println("No projection specified in CTYPE, GRIDTYPE present => assuming CAR"); 
      }
      //                   proj = 0 ;
      //                   System.out.println("type1 "+type1+" type2"+type2);
      //                   if (type1.indexOf("SIN")>= 0) proj = 1 ;
      //                   if (type1.indexOf("TAN")>= 0) proj = 2 ;
      //                   if (type1.indexOf("ARC")>= 0) proj = 3 ;
      //                   if (type1.indexOf("AIT")>= 0) proj = 4 ;
      //                   if (type1.indexOf("ZEA")>= 0) proj = 5 ;
      //                   if (type1.indexOf("STG")>= 0) proj = 6 ;
      //                   if (type1.indexOf("CAR")>= 0) proj = 7 ;
      //                   if (type1.indexOf("NCP")>= 0) proj = 8 ;
      //                   if (type1.indexOf("ZPN")>= 0) proj = 9 ;
      //                   if (type1.indexOf("Solar")>=0) proj = 10;
      //                   if (type1.indexOf("solar")>=0) proj = 10;
      if ( Util.indexOfIgnoreCase(type1,"solar")>=0) proj = SOL;
      // PIERRE POUR GOODS
      //#ifndef PIERRE
      //if (type1.indexOf("COE")>= 0) proj = 2 ;
      //                   if (type1.indexOf("ARC")>= 0) proj = 3 ;
      //                   if (type1.indexOf("AIT")>= 0) proj = 4 ;
      //                   if (type1.indexOf("ZEA")>= 0) proj = 5 ;
      //                   if (type1.indexOf("STG")>= 0) proj = 6 ;
      //                   if (type1.indexOf("CAR-")>= 0) proj = 7 ;
      //                   if (type1.indexOf("NCP")>= 0) proj = 8 ;
      //                   if (type1.indexOf("ZPN")>= 0) proj = 9 ;
      //                   if( flagSolar ) proj = 10;
      //                   if (type1.indexOf("Solar")>=0) proj = 10;
      //                   if (type1.indexOf("solar")>=0) proj = 10;
      //                   if (proj == 0) {
      //#endif /* PIERRE */
      
      if ((proj == SIP)||(proj == SINSIP)) {
        //  if (proj == SIP) {    
        //  System.out.println("proj "+proj) ;
         try {
            try { order_a = hf.getIntFromHeader("A_ORDER") ; } catch( Exception e ) { order_a=1; }
            try { order_b= hf.getIntFromHeader("B_ORDER") ;  } catch( Exception e ) { order_b=1; }

       //     for (int order = 2;  order < order_a+1 ; order++)
            for (int order = 0;  order <= order_a ; order++)
            {
               for (int powx =0 ; powx <= order ; powx++ )
               {
                //  for (int j = 0 ; j < order-powx + 1 ; j++)
            	   int j = order -powx ; 
                     try {
                        sip_a[powx][j] = hf.getDoubleFromHeader("A_"+(new Integer(powx).toString())+"_"+(new Integer(j).toString())+"  ");
                       // System.out.println("sip_a "+powx+" "+j+" "+sip_a[powx][j]) ;
                     }
                  catch (Exception ee) { sip_a[powx][j] = 0.0 ;}
               }
            }   
            
  //          for (int order = 2;  order < order_b+1 ; order++)
            for (int order = 0;  order <= order_b ; order++)
            {
               for (int powx =0 ; powx <= order ; powx++ )
               {
    //              for (int j = 0 ; j < order-powx + 1 ; j++)
            	    int j = order -powx ;
                     try {
                        sip_b[powx][j] = hf.getDoubleFromHeader("B_"+(new Integer(powx).toString())+"_"+(new Integer(j).toString())+"  ");
                        //                                          System.out.println("sip_b "+powx+" "+j+" "+sip_b[powx][j])  ;
                     }
                  catch (Exception ee) { sip_b[powx][j] = 0.0 ;}
               }
            }   
            
            //    a[0][2] = hf.getDoubleFromHeader("A_"+(new Integer(0).toString())+"_2  "); 

            //    a[1][1] = hf.getDoubleFromHeader("A_1_1  ");

            //    a[2][0] = hf.getDoubleFromHeader("A_2_0  ");

            //    b[0][2] = hf.getDoubleFromHeader("B_0_2  "); 
            //    b[1][1] = hf.getDoubleFromHeader("B_1_1  ");
            //    b[2][0] = hf.getDoubleFromHeader("B_2_0 ");

            //    ap[0][2] = hf.getDoubleFromHeader("AP_0_2 ");
            //    ap[1][1] = hf.getDoubleFromHeader("AP_1_1 ");
            //    ap[2][0] = hf.getDoubleFromHeader("AP_2_0 ");

            //    bp[0][2] = hf.getDoubleFromHeader("BP_0_2 "); 
            //    bp[1][1] = hf.getDoubleFromHeader("BP_1_1 ");
            //    bp[2][0] = hf.getDoubleFromHeader("BP_2_0 ");

            //    if (order_a > 2)
            //    {
            //        a[3][0] = hf.getDoubleFromHeader("A_3_0  ");
            //        a[0][3] = hf.getDoubleFromHeader("A_0_3  ");
            //    a[1][2] = hf.getDoubleFromHeader("A_1_2  ");
            //    a[2][1] = hf.getDoubleFromHeader("A_2_1  ");
            //    }
            //     if (order_b > 2)
            //     {           


            //    b[1][2] = hf.getDoubleFromHeader("B_1_2  ");
            //    b[0][3] = hf.getDoubleFromHeader("B_0_3  ");
            //    b[2][1] = hf.getDoubleFromHeader("B_2_1  ");
            //    b[3][0] = hf.getDoubleFromHeader("B_3_0  ");
            //     }
            //     if (order_ap > 2)
            //     {       
            //    
            //    ap[3][0] = hf.getDoubleFromHeader("AP_3_0 ");
            //    ap[2][1] = hf.getDoubleFromHeader("AP_2_1 ");
            //    ap[1][2] = hf.getDoubleFromHeader("AP_1_2 ");
            //    ap[0][3] = hf.getDoubleFromHeader("AP_0_3 ");
            //     }
            //     if (order_bp > 2)
            //     {             
            //    bp[3][0] = hf.getDoubleFromHeader("BP_3_0 ");
            //    bp[2][1] = hf.getDoubleFromHeader("BP_2_1 ");
            //    bp[1][2] = hf.getDoubleFromHeader("BP_1_2 ");
            //    bp[0][3] = hf.getDoubleFromHeader("BP_0_3 ");
            //     }
        
            try { order_ap = hf.getIntFromHeader("AP_ORDER") ; } catch( Exception e) { order_ap = 1; }
            try { order_bp = hf.getIntFromHeader("BP_ORDER") ; } catch( Exception e) { order_bp = 1; }
     //       for (int order = 2;  order < order_ap+1 ; order++)
            for (int order = 0; order <= order_ap ; order++)
            {
               for (int powx =0 ; powx <= order ; powx++ )
               {
              //    for (int j = 0 ; j < order-powx + 1 ; j++)
            	  int j = order -powx ;
                     try {
                        sip_ap[powx][j] = hf.getDoubleFromHeader("AP_"+(new Integer(powx).toString())+"_"+(new Integer(j).toString())+"  ");
                        //                                  System.out.println("sip_ap "+powx+" "+j+" "+sip_ap[powx][j]) ;
                     }
                  catch (Exception ee) { sip_ap[powx][j] = 0.0 ;}
               }
            }   
            //      System.out.println("ici") ;
            // for (int order = 2;  order < order_bp+1 ; order++)
            for (int order = 0;  order <= order_bp ; order++)
            {
               for (int powx =0 ; powx <= order ; powx++ )
               {
            	   int j = order -powx ;
                 // for (int j = 0 ; j < order-powx + 1 ; j++)
                     try {
                        sip_bp[powx][j] = hf.getDoubleFromHeader("BP_"+(new Integer(powx).toString())+"_"+(new Integer(j).toString())+"  ");
                        //                                      System.out.println("sip_bp "+powx+" "+j+" "+sip_bp[powx][j]) ;
                     }
                  catch (Exception ee) { sip_bp[powx][j] = 0.0 ;}
               }        


            }       
            //     System.out.println("ici") ;
         }
         catch (Exception e15 ) { 
            e15.printStackTrace();
            proj=-1;
         }
      }
       
      if (type1.indexOf("COE")>= 0 && type2.indexOf("COE")>= 0) proj = TAN ;
      if (proj == -1) {
//         System.err.println(
//               "Calib warning:CTYPE "+type1+"/"+type2+" is not yet supported "
//                     +"by Aladin") ;
         throw new Exception("CTYPE "+type1+"/"+type2+" is not yet supported "
               +"by Aladin") ;
      }


      // calcul de la largeur et de la hauteur de l'iamge
      widtha = xnpix * Math.abs(incA) ;
      widthd = ynpix * Math.abs(incD) ;
      // cos delta au centre de l'image
      //                   cdelz = FastMath.cos((deltai/180.)*Math.PI);
      //                   sdelz = FastMath.sin((deltai/180.)*Math.PI);
      cdelz = FastMath.cos(deltai*deg_to_rad);
      sdelz = FastMath.sin(deltai*deg_to_rad);
      // calcul de la transformation inverse
      det = CD[0][0]* CD[1][1]-CD[0][1]*CD[1][0] ;
      ID[0][0] = CD[1][1]/det ;
      ID[0][1] = -CD[0][1]/det ;
      ID[1][0] = -CD[1][0]/det ;
      ID[1][1] = CD[0][0]/det ;
      //                    System.out.println("ID "+ID[0][0]+" "+ID[0][1]+" "+ID[1][0]+" "+ID[1][1]);


 //      System.out.println("COCO "+xyapoly[0]+" "+xydpoly[0]);
 //      System.out.println("COCO "+xyapoly[5]+" "+xydpoly[5]);

  //    System.out.println("proj "+proj) ;
   }

   public Calib (int order,  long npix, int frame, int width) {
      try {
         double [][] bord = CDSHealpix.borders(order, npix, 2) ;
         double [] center = CDSHealpix.pix2ang_nest( order, npix) ;
         double [] centerRadec = CDSHealpix.polarToRadec(center);
         alphai = centerRadec[0] ;
         deltai = centerRadec[1] ;
         // System.out.println("alpha delta "+alphai+" "+deltai);
         //   double X1 = 0.5*(bord[0][0]+bord[1][0] );
         //   double Y1 = 0.5*(bord[0][1]+bord[1][1] );
         //  double X2 = 0.5*(bord[3][0]+bord[2][0] );
         //   double Y2 = 0.5*(bord[3][1]+bord[2][1] );
         //double X3 = 0.5*(bord[0][0]+bord[3][0] );
         //    double Y3 = 0.5*(bord[0][1]+bord[3][1] );
         //   double X4 = 0.5*(bord[1][0]+bord[2][0] );
         //   double Y4 = 0.5*(bord[1][1]+bord[2][1] );
         double cdelz1, sdelz1 ;
         double X1= bord[0][0] ;
         double Y1= bord[0][1] ;
         double X2= bord[1][0] ;
         double Y2= bord[1][1] ;
         double X3= bord[2][0] ;
         double Y3= bord[2][1] ;
         double X4= bord[3][0] ;
         double Y4= bord[3][1] ;
         double X5= bord[4][0] ;
         double Y5= bord[4][1] ;
         double X6= bord[5][0] ;
         double Y6= bord[5][1] ;
         double X7= bord[6][0] ;
         double Y7= bord[6][1] ;
         double X8= bord[7][0] ;
         double Y8= bord[7][1] ;

         //System.out.println("X Y "+X1+" "+Y1);
         //System.out.println("X Y "+X2+" "+Y2);
         //System.out.println("X Y "+X3+" "+Y3);
         //System.out.println("X Y "+X4+" "+Y4);
         //System.out.println("X Y "+X5+" "+Y5);
         //System.out.println("X Y "+X6+" "+Y6);
         //System.out.println("X Y "+X7+" "+Y7);
         //System.out.println("X Y "+X8+" "+Y8);
         cdelz = FastMath.cos((deltai/180.)*Math.PI);
         sdelz = FastMath.sin((deltai/180.)*Math.PI);
         cdelz1 = cdelz ;
         sdelz1 = sdelz ;

         // CD[0][0] = -(x_y_1.al*cdelz1+x_y_1.del)*2/xnpix;
         // CD[0][1] = -(x_y_1.al*cdelz1-x_y_1.del)*2/ynpix;
         double xst1, yst1,deno;
         double xst2, yst2, xst3, yst3, xst4, yst4 ;
         double xst5, yst5, xst6, yst6, xst7, yst7, xst8, yst8 ;
         deno = FastMath.sin(Y1*Math.PI/180.)*sdelz1
  +FastMath.cos(Y1*Math.PI/180.)*cdelz1
  *FastMath.cos((X1-alphai)*Math.PI/180.) ;
         xst1 = FastMath.cos(Y1*Math.PI/180.)
 *FastMath.sin((X1-alphai)*Math.PI/180.)
 / deno ;
         yst1 = ( FastMath.sin(Y1*Math.PI/180.)*cdelz1
 -FastMath.cos(Y1*Math.PI/180.)*sdelz1
 *FastMath.cos((X1-alphai)*Math.PI/180.))
 / deno; 
         //System.out.println("detail "+FastMath.sin(Y1*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y1*Math.PI/180.)*sdelz1*FastMath.cos((X1-alphai)*Math.PI/180.)
         //    / deno) ;
         //System.out.println("detail "+FastMath.sin(Y1*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y1*Math.PI/180.)*sdelz1 ) ;
         //   CD[0][0] = - xst1 / 256 ;
         //   CD[1][0] =  yst1 / 256 ;

         //System.out.println("CD "+CD[0][0]+" "+CD[1][0]);
         //System.out.println("xst yst"+xst1+" "+yst1) ;
         deno = FastMath.sin(Y2*Math.PI/180.)*sdelz1
               +FastMath.cos(Y2*Math.PI/180.)*cdelz1
               *FastMath.cos((X2-alphai)*Math.PI/180.) ;
         xst2 = FastMath.cos(Y2*Math.PI/180.)
               *FastMath.sin((X2-alphai)*Math.PI/180.)
               / deno ;
         yst2 = (FastMath.sin(Y2*Math.PI/180.)*cdelz1
               -FastMath.cos(Y2*Math.PI/180.)*sdelz1
               *FastMath.cos((X2-alphai)*Math.PI/180.))
               / deno;

         //System.out.println("detail "+FastMath.sin(Y2*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y2*Math.PI/180.)*sdelz1*FastMath.cos((X2-alphai)*Math.PI/180.)
         /// deno) ;
         //System.out.println("detail "+FastMath.sin(Y2*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y2*Math.PI/180.)*sdelz1) ;
         //CD[0][0] =  CD[0][0] + xst2 / 256 ;
         //CD[1][0] =  CD[1][0] - yst2 / 256 ;
         //CD[0][0] /= 2 ;
         //CD[1][0] /= 2 ;
         //System.out.println("CD "+CD[0][0]+" "+CD[1][0]);
         //System.out.println("xst yst"+xst2+" "+yst2) ;

         deno = FastMath.sin(Y3*Math.PI/180.)*sdelz1
               +FastMath.cos(Y3*Math.PI/180.)*cdelz1
               *FastMath.cos((X3-alphai)*Math.PI/180.) ;
         xst3 = (FastMath.cos(Y3*Math.PI/180.)
               *FastMath.sin((X3-alphai)*Math.PI/180.))
               / deno ;
         yst3 = FastMath.sin(Y3*Math.PI/180.)*cdelz1
               -FastMath.cos(Y3*Math.PI/180.)*sdelz1
               *FastMath.cos((X3-alphai)*Math.PI/180.)
               / deno;
         //System.out.println("detail "+FastMath.sin(Y3*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y3*Math.PI/180.)*sdelz1*FastMath.cos((X3-alphai)*Math.PI/180.)
         // / deno) ;
         //System.out.println("detail "+FastMath.sin(Y3*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y3*Math.PI/180.)*sdelz1 ) ;
         //CD[0][1] =   xst3 / 256 ;
         //CD[1][1] =   yst3 / 256 ;
         //System.out.println("CD "+CD[0][1]+" "+CD[1][1]);
         //System.out.println("xst yst"+xst3+" "+yst3) ;
         deno = FastMath.sin(Y4*Math.PI/180.)*sdelz1
               +FastMath.cos(Y4*Math.PI/180.)*cdelz1
               *FastMath.cos((X4-alphai)*Math.PI/180.) ;
         xst4 = FastMath.cos(Y4*Math.PI/180.)
               *FastMath.sin((X4-alphai)*Math.PI/180.)
               / deno ;
         yst4 = (FastMath.sin(Y4*Math.PI/180.)*cdelz1
               -FastMath.cos(Y4*Math.PI/180.)*sdelz1
               *FastMath.cos((X4-alphai)*Math.PI/180.))
               / deno;
         // System.out.println("detail "+FastMath.sin(Y4*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y4*Math.PI/180.)*sdelz1*FastMath.cos((X4-alphai)*Math.PI/180.)
         // / deno) ;
         // System.out.println("detail "+FastMath.sin(Y4*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y4*Math.PI/180.)*sdelz1 ) ;
         //CD[0][1] = CD[0][1] - xst4 / 256 ;
         //CD[1][1] =  CD[1][1] - yst4 / 256 ;
         deno = FastMath.sin(Y5*Math.PI/180.)*sdelz1
               +FastMath.cos(Y5*Math.PI/180.)*cdelz1
               *FastMath.cos((X5-alphai)*Math.PI/180.) ;
         xst5 = FastMath.cos(Y5*Math.PI/180.)
               *FastMath.sin((X5-alphai)*Math.PI/180.)
               / deno ;
         yst5 = (FastMath.sin(Y5*Math.PI/180.)*cdelz1
               -FastMath.cos(Y5*Math.PI/180.)*sdelz1
               *FastMath.cos((X5-alphai)*Math.PI/180.))
               / deno;
         //System.out.println("detail "+FastMath.sin(Y5*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y5*Math.PI/180.)*sdelz1*FastMath.cos((X5-alphai)*Math.PI/180.)
         // / deno) ;
         // System.out.println("detail "+FastMath.sin(Y5*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y5*Math.PI/180.)*sdelz1 ) ;
         deno = FastMath.sin(Y6*Math.PI/180.)*sdelz1
               +FastMath.cos(Y6*Math.PI/180.)*cdelz1
               *FastMath.cos((X6-alphai)*Math.PI/180.) ;
         xst6 = FastMath.cos(Y6*Math.PI/180.)
               *FastMath.sin((X6-alphai)*Math.PI/180.)
               / deno ;
         yst6 = (FastMath.sin(Y6*Math.PI/180.)*cdelz1
               -FastMath.cos(Y6*Math.PI/180.)*sdelz1
               *FastMath.cos((X6-alphai)*Math.PI/180.))
               / deno;
         // System.out.println("detail "+FastMath.sin(Y6*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y6*Math.PI/180.)*sdelz1*FastMath.cos((X6-alphai)*Math.PI/180.)
         // / deno) ;
         // System.out.println("detail "+FastMath.sin(Y6*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y6*Math.PI/180.)*sdelz1 ) ;
         deno = FastMath.sin(Y7*Math.PI/180.)*sdelz1
               +FastMath.cos(Y7*Math.PI/180.)*cdelz1
               *FastMath.cos((X7-alphai)*Math.PI/180.) ;
         xst7 = FastMath.cos(Y7*Math.PI/180.)
               *FastMath.sin((X7-alphai)*Math.PI/180.)
               / deno ;
         yst7 = (FastMath.sin(Y7*Math.PI/180.)*cdelz1
               -FastMath.cos(Y7*Math.PI/180.)*sdelz1
               *FastMath.cos((X7-alphai)*Math.PI/180.))
               / deno;
         // System.out.println("detail "+FastMath.sin(Y7*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y7*Math.PI/180.)*sdelz1*FastMath.cos((X7-alphai)*Math.PI/180.)
         // / deno) ;
         // System.out.println("detail "+FastMath.sin(Y7*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y7*Math.PI/180.)*sdelz1 ) ;
         deno = FastMath.sin(Y8*Math.PI/180.)*sdelz1
               +FastMath.cos(Y8*Math.PI/180.)*cdelz1
               *FastMath.cos((X8-alphai)*Math.PI/180.) ;
         xst8 = FastMath.cos(Y8*Math.PI/180.)
               *FastMath.sin((X8-alphai)*Math.PI/180.)
               / deno ;
         yst8 = (FastMath.sin(Y8*Math.PI/180.)*cdelz1
               -FastMath.cos(Y8*Math.PI/180.)*sdelz1
               *FastMath.cos((X8-alphai)*Math.PI/180.))
               / deno;
         // System.out.println("detail "+FastMath.sin(Y8*Math.PI/180.)*cdelz1/deno+" "+FastMath.cos(Y8*Math.PI/180.)*sdelz1*FastMath.cos((X8-alphai)*Math.PI/180.)
         // / deno) ;
         // System.out.println("detail "+FastMath.sin(Y8*Math.PI/180.)*cdelz1+" "+FastMath.cos(Y8*Math.PI/180.)*sdelz1 ) ;
         CD[0][0]= rad_to_deg * (-xst1 -xst2 -xst3 + xst5 + xst6 +xst7 )/6/256 ;
         CD[0][1]= rad_to_deg * (xst1 -xst3 -xst4 -xst5 + xst7 +xst8 )/6/256 ;
         CD[1][0]= -rad_to_deg * (-yst1 -yst2 -yst3 +yst5 +yst6 +yst7)/6/256;
         CD[1][1]= rad_to_deg * (yst1 -yst3 -yst4 -yst5 +yst7 +yst8 )/6/256;
         // System.out.println("CD "+CD[0][0]+" "+CD[0][1]+" "+CD[1][0]+" "+CD[1][1]);
         // System.out.println("xst "+xst1+" "+xst2+" "+xst3+" "+xst4+" "+xst5+" "+xst6+" "+xst7+" "+xst8);
         // System.out.println("yst "+yst1+" "+yst2+" "+yst3+" "+yst4+" "+yst5+" "+yst6+" "+yst7+" "+yst8);
         xnpix = 512 ;
         ynpix = 512 ;
         Xcen = 255 ;
         Ycen = 255 ;
         proj = TAN ;
         equinox = 2000.0 ;
         Coord ccc = new Coord() ;
         ccc.x =  0 ;
         ccc.y = 0 ;
         GetCoord( ccc);

         // System.out.println("ccc "+ccc.al+" "+ccc.del);

         ccc.x = 0 ;
         ccc.y = 256 ;
         GetCoord( ccc);

         // System.out.println("ccc "+ccc.al+" "+ccc.del);

         ccc.x = 0;
         ccc.y = 128 ;
         GetCoord( ccc);




         // System.out.println("ccc "+ccc.al+" "+ccc.del);

         ccc.x = 512 ;
         ccc.y = 512  ;
         GetCoord( ccc );

   //System.out.println("ccc "+ccc.al+" "+ccc.del);
   //System.out.println("xst yst"+xst8+" "+yst8) ;
   //   System.out.println("X Y"+X1+" "+Y1);
   //   System.out.println("X Y"+X2+" "+Y2);
   //   System.out.println("X Y"+X3+" "+Y3);
   //   System.out.println("X Y"+X4+" "+Y4);
   //   System.out.println("bord 0 "+bord[0][0]+" "+bord[0][1]);
   //   System.out.println("bord 1 "+bord[1][0]+" "+bord[1][1]);
   //   System.out.println("bord 2 "+bord[2][0]+" "+bord[2][1]);
   //   System.out.println("bord 3 "+bord[3][0]+" "+bord[3][1]);

   //   System.out.println("1-2 alpha "+0.5*(bord[0][0]+bord[1][0])+" "+(bord[0][0]-bord[1][0]));
   //   System.out.println("1-2 delta "+0.5*(bord[0][1]+bord[1][1])+" "+(bord[0][1]-bord[1][1]));
   //   System.out.println("4-3 alpha "+0.5*(bord[3][0]+bord[2][0])+" "+(bord[3][0]-bord[2][0]));
   //   System.out.println("4-3 delta "+0.5*(bord[3][1]+bord[2][1])+" "+(bord[3][1]-bord[2][1]));
   //   System.out.println("1-4 alpha "+0.5*(bord[0][0]+bord[3][0])+" "+(bord[3][0]-bord[0][0]));
   //   System.out.println("1-4 delta "+0.5*(bord[0][1]+bord[3][1])+" "+(bord[3][1]-bord[0][1]));
   //   System.out.println("2-3 alpha "+0.5*(bord[1][0]+bord[2][0])+" "+(bord[1][0]-bord[2][0]));
   //   System.out.println("2-3 delta "+0.5*(bord[1][1]+bord[2][1])+" "+(bord[1][1]-bord[2][1]));

   double [][] bord1 = CDSHealpix.borders(order, npix, 2) ; 

   //       System.out.println("bord 0 "+bord1[0][0]+" "+bord1[0][1]);
   //       System.out.println("bord 1 "+bord1[1][0]+" "+bord1[1][1]);
   //       System.out.println("bord 2 "+bord1[2][0]+" "+bord1[2][1]);
   //       System.out.println("bord 3 "+bord1[3][0]+" "+bord1[3][1]);
   //       System.out.println("bord 4 "+bord1[4][0]+" "+bord1[4][1]);
   //       System.out.println("bord 5 "+bord1[5][0]+" "+bord1[5][1]);
   //       System.out.println("bord 6 "+bord1[6][0]+" "+bord1[6][1]);
   //       System.out.println("bord 7 "+bord1[7][0]+" "+bord1[7][1]);

  // System.out.println("center "+centerRadec[0]+" "+centerRadec[1]); 
      }
      catch (Exception e17) {}
   }

   protected void Dss (HeaderFits hf) throws Exception {
   int sign = 1;
   double det ;


   // PF - Jan 2011 - La méthode de calibration DSS ne marche pas actuellement avec les imagettes
   // => dans les mains de François B. En attendant, je fais un gros patch
   //               if( hf.getDoubleFromHeader("NAXIS1")<10000 ) throw new Exception("Certainely not a full plate");


   proj = TAN ; // projection TAN ;
   alpha += hf.getIntFromHeader("PLTRAM  ")*60. ;
   //WCSKeys.addElement("PLTRAM  "); 
   alpha += hf.getIntFromHeader("PLTRAH  ")*3600. ;
   //WCSKeys.addElement("PLTRAH  "); 
   alpha /= 240. ;
   if(hf.getStringFromHeader("PLTDECSN").startsWith( "-"))sign = -1 ;
   //WCSKeys.addElement("PLTDECSN"); 
   delta = hf.getDoubleFromHeader("PLTDECS ") ;
   //WCSKeys.addElement("PLTDECS "); 
   delta += hf.getIntFromHeader("PLTDECM ")*60. ;
   //WCSKeys.addElement("PLTDECM "); 
   delta += hf.getIntFromHeader("PLTDECD ")*3600. ;
   //WCSKeys.addElement("PLTDECD "); 
   delta /= 3600. ;
   delta *= sign ;
   //            System.out.println(" Header Getimage: "+alphai+"   "+deltai);
   focale = hf.getDoubleFromHeader("PLTSCALE");
   //WCSKeys.addElement("PLTSCALE"); 
   focale = 180.*3600./Math.PI/focale ;
   //              System.out.println("Dss focale: "+focale);
   equinox = hf.getDoubleFromHeader("EQUINOX ");
   //WCSKeys.addElement("EQUINOX"); 
   try {
      // Epoque d'observation
      epoch = hf.getDoubleFromHeader("EPOCH  ") ;
      flagepoc = 1 ;
      //WCSKeys.addElement("EPOCH  "); 
   }
   catch (Exception e1) {
      epoch = equinox ;
      flagepoc = 0 ;
   }

   xz = hf.getDoubleFromHeader("PPO3    ");
   //WCSKeys.addElement("PPO3    "); 
   xz /= 1000. ;
   yz = hf.getDoubleFromHeader("PPO6    ");
   //WCSKeys.addElement("PPO6    "); 
   yz /= 1000.;
   xyapoly[2] = hf.getDoubleFromHeader("AMDX1   ") ;
   //WCSKeys.addElement("AMDX1   "); 
   xydpoly[1] = hf.getDoubleFromHeader("AMDY1   ") ;
   //WCSKeys.addElement("AMDY1   "); 
   //         System.out.println(focale+" "+xz+" "+yz+" "+xyapoly[2]+" "+xydpoly[2]);
   xyapoly[1] = hf.getDoubleFromHeader("AMDX2   ") ;
   //WCSKeys.addElement("AMDX2   "); 
   xydpoly[2] = hf.getDoubleFromHeader("AMDY2   ") ;
   //WCSKeys.addElement("AMDY2   "); 
   xyapoly[0] = hf.getDoubleFromHeader("AMDX3   ") ;
   //WCSKeys.addElement("AMDX3   "); 
   xydpoly[0] = hf.getDoubleFromHeader("AMDY3   ") ;
   //WCSKeys.addElement("AMDY3   "); 
   xyapoly[4] = hf.getDoubleFromHeader("AMDX4   ") ;
   //WCSKeys.addElement("AMDX4   "); 
   xydpoly[3] = hf.getDoubleFromHeader("AMDY4   ") ;
   //WCSKeys.addElement("AMDY4    "); 
   xyapoly[5] = hf.getDoubleFromHeader("AMDX5   ") ;
   //WCSKeys.addElement("AMDX5   "); 
   xydpoly[5] = hf.getDoubleFromHeader("AMDY5   ") ;
   //WCSKeys.addElement("AMDY5   "); 
   xyapoly[3] = hf.getDoubleFromHeader("AMDX6   ") ;
   //WCSKeys.addElement("AMDX6   "); 
   xydpoly[4] = hf.getDoubleFromHeader("AMDY6   ") ;
   //WCSKeys.addElement("AMDY6   "); 
   xyapoly[4] += hf.getDoubleFromHeader("AMDX7   ") ;
   //WCSKeys.addElement("AMDX7   "); 
   xydpoly[4] += hf.getDoubleFromHeader("AMDY7   ") ;
   //WCSKeys.addElement("AMDY7   "); 
   xyapoly[3] += hf.getDoubleFromHeader("AMDX7   ") ;
   xydpoly[3] += hf.getDoubleFromHeader("AMDY7   ") ;
   xyapoly[7] = hf.getDoubleFromHeader("AMDX8   ") ;
   //WCSKeys.addElement("AMDX8   "); 
   xydpoly[6] = hf.getDoubleFromHeader("AMDY8   ") ;
   //WCSKeys.addElement("AMDY8   "); 
   xyapoly[9] = hf.getDoubleFromHeader("AMDX9   ") ;
   //WCSKeys.addElement("AMDX9   "); 
   xydpoly[8] = hf.getDoubleFromHeader("AMDY9   ") ;
   //WCSKeys.addElement("AMDY9   "); 
   xyapoly[8] = hf.getDoubleFromHeader("AMDX10  ") ;
   //WCSKeys.addElement("AMDX10  "); 
   xydpoly[9] = hf.getDoubleFromHeader("AMDY10  ") ;
   //WCSKeys.addElement("AMDY10  "); 
   xyapoly[6] = hf.getDoubleFromHeader("AMDX11  ") ;
   //WCSKeys.addElement("AMDX11  "); 
   xydpoly[7] = hf.getDoubleFromHeader("AMDY11  ") ;
   //WCSKeys.addElement("AMDY11  "); 
   xyapoly[7] += hf.getDoubleFromHeader("AMDX12  ") ;
   //WCSKeys.addElement("AMDX12  "); 
   xydpoly[6] += hf.getDoubleFromHeader("AMDY12  ") ;
   //WCSKeys.addElement("AMDY12  "); 
   xyapoly[8] += hf.getDoubleFromHeader("AMDX12  ") ;
   xydpoly[9] += hf.getDoubleFromHeader("AMDY12  ") ;
   xyapoly[0] /= focale ;
   xydpoly[0] /= focale ;
   xyapoly[1] *= -1.;
   xydpoly[1] *= -1.;
   xyapoly[2] *= -1.;
   xydpoly[2] *= -1.;
   xyapoly[3] *= focale ;
   xydpoly[3] *= focale ;
   xyapoly[4] *= focale ;
   xydpoly[4] *= focale ;
   xyapoly[5] *= focale ;
   xydpoly[5] *= focale ;
   xyapoly[6] *= -focale*focale ;
   xydpoly[6] *= -focale*focale ;
   xyapoly[7] *= -focale*focale ;
   xydpoly[7] *= -focale*focale ;
   xyapoly[8] *= -focale*focale ;
   xydpoly[8] *= -focale*focale ;
   xyapoly[9] *= -focale*focale ;
   xydpoly[9] *= -focale*focale ;
   int i ;
   for (i=0; i<=9;i++)
   {
      xyapoly[i] /= (180*3600/Math.PI/focale) ;
      xydpoly[i] /= (180*3600/Math.PI/focale) ;
      //                  System.out.println("xyapoly "+ xyapoly[i] +" " +xydpoly[i]   );
   }
   incX = hf.getDoubleFromHeader("XPIXELSZ") ;
   //WCSKeys.addElement("XPIXELSZ"); 
   incY = hf.getDoubleFromHeader("YPIXELSZ") ;
   //WCSKeys.addElement("YPIXELSZ"); 
   xnpix = hf.getIntFromHeader("NAXIS1  ");
   //WCSKeys.addElement("NAXIS1  "); 
   ynpix = hf.getIntFromHeader("NAXIS2  ");
   //WCSKeys.addElement("NAXIS2  "); 
   Xorg = incX *  hf.getDoubleFromHeader("CNPIX1  ") ;
   // CNPX1 double for DFBS WCSKeys.addElement("CNPIX1  "); 
   Yorg = incY *(23040 - hf.getDoubleFromHeader("CNPIX2  ") -ynpix ) ;
   //CNPIX2 double for DFBS WCSKeys.addElement("CNPIX2  "); 
   yz   = incY * 23040 / 1000. -yz ;

   cdelz = FastMath.cos(delta*deg_to_rad);
   sdelz = FastMath.sin(delta*deg_to_rad);
   alphai = alpha ;
   deltai = delta ;

   //    aladin = 1;

   //       GetWCS_i() ;
   //
   //       det = CD[0][0]* CD[1][1]-CD[0][1]*CD[1][0] ;
   //      ID[0][0] = CD[1][1]/det ;
   //      ID[0][1] = -CD[0][1]/det ;
   //      ID[1][0] = -CD[1][0]/det ;
   //      ID[1][1] = CD[0][0]/det ;
   //      incA = Math.sqrt(CD[0][0]*CD[0][0]+CD[0][1]*CD[0][1]) ;
   //      incD = Math.sqrt(CD[1][0]*CD[1][0]+CD[1][1]*CD[1][1]) ;
   //      widtha = xnpix * Math.abs(incA) ;
   //      widthd = ynpix * Math.abs(incD) ;

   //    cdelz = FastMath.cos(deltai*deg_to_rad);
   //    sdelz = FastMath.sin(deltai*deg_to_rad);


   //     type1 = "'RA---TAN'" ;
   //     type2 = "'DEC--TAN'" ;
   incA =  (xyapoly[2]*incX/focale/1000)*rad_to_deg ;
   incD =  (xydpoly[1]*incY/focale/1000)*rad_to_deg  ;

   widtha = xnpix * Math.abs(incA) ;
   widthd = ynpix * Math.abs(incD) ;

   //      System.out.println("Dss inC "+incA+" "+incD);

   // Pourquoi y repasser ? On garde la solution linÃ¯Â¿Â½aire dans ce cas.
   aladin = 2 ;


   }


   protected void GetXYstand(Coord c) throws Exception {
   double x_obj =1.;
   double y_obj =1.;
   double x_objr ;
   double y_objr ;
   double x_tet_phi;
   double y_tet_phi;
   double y_stand =0.03;
   double x_stand =0.03;
   double delrad ;
   double alrad ;
   double dr;
   double al,del;

   // System.out.println("aladin "+c.al+" "+c.del);
   // System.out.println("aldel_i "+alphai+" "+deltai);
   if(aladin == 1)
   {
      //               cdelz = FastMath.cos((delta/180.)*Math.PI);
      //               sdelz = FastMath.sin((delta/180.)*Math.PI);


      // Methode aladin = methode plaque ....
      //             delrad = (c.del/180.)*Math.PI;
      delrad = c.del*deg_to_rad;
      alrad  = (c.al - alpha)*deg_to_rad;
      double sin_delrad = FastMath.sin (delrad) ;
      double cos_delrad = FastMath.cos (delrad) ;
      double sin_alrad  = FastMath.sin(alrad) ;
      double cos_alrad  = FastMath.cos(alrad) ;
      dr = sin_delrad * sdelz
            + cos_delrad * cdelz * cos_alrad;
      x_stand =  cos_delrad
            * sin_alrad/dr ;
      y_stand = (sin_delrad *  cdelz
            - cos_delrad * sdelz
            * cos_alrad) / dr;
      //            System.out.println("xy_stand "+x_stand+" " +y_stand);
   }
   else
   {
      al = c.al ;
      del = c.del ;

      c.dx = c.x - Xcen /* PF +1 */;
      //                 c.dy = ynpix - Ycen -c.y;
      c.dy = c.y - Ycen ;
      //               if (equinox == 0.0 )  system = FK4 ;
      if (system ==  FK4)
         //             if ((equinox != 2000.0)&&(system != GALACTIC))
         // Ancine test supprimé en 04/2012  
      {
         // PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //                Astroframe j2000 = new Astroframe() ;
         //                Astroframe natif = new Astroframe(1,equinox) ;
         //                j2000.set(al,del) ;
         //                j2000.convert(natif) ;
         //                al = natif.getLon() ;
         //                del = natif.getLat() ;                               
         Astrocoo ac = new Astrocoo(AF_ICRS,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_FK4);
         al = ac.getLon();
         del = ac.getLat();
      }
      if (system ==  FK5)
         //                 if ((equinox != 2000.0)&&(system != GALACTIC))
         // Ancine test supprimé en 04/2012  
      {
         // PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //                    Astroframe j2000 = new Astroframe() ;
         //                    Astroframe natif = new Astroframe(1,equinox) ;
         //                    j2000.set(al,del) ;
         //                    j2000.convert(natif) ;
         //                    al = natif.getLon() ;
         //                    del = natif.getLat() ;                               
         Astrocoo ac = new Astrocoo(AF_ICRS,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_FK5);
         al = ac.getLon();
         del = ac.getLat();
      }
      if (system == GALACTIC)
      {
         // PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //                 Astroframe fk5 = new Astroframe() ;
         //                 Astroframe natif =  new Astroframe(2,equinox);
         //                 fk5.set(al,del) ;
         //                 fk5.convert(natif);
         //                 al = natif.getLon() ;
         //                 del = natif.getLat() ;
         Astrocoo ac = new Astrocoo(AF_ICRS,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_GAL);
         al = ac.getLon();
         del = ac.getLat();
         //      System.out.println(c.al+" "+c.del);
      }
      if (system == ECLIPTIC)
      {
         //PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //               Astroframe fk5 = new Astroframe() ;
         //               Astroframe natif =  new Astroframe(2,equinox);
         //               fk5.set(al,del) ;
         //               fk5.convert(natif);
         //               al = natif.getLon() ;
         //               del = natif.getLat() ;
         Astrocoo ac = new Astrocoo(AF_ICRS,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_ECL);
         al = ac.getLon();
         del = ac.getLat();
         //      System.out.println(c.al+" "+c.del);
      }

      if (system == SUPERGALACTIC)
      {
         //PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //               Astroframe fk5 = new Astroframe() ;
         //               Astroframe natif =  new Astroframe(2,equinox);
         //               fk5.set(al,del) ;
         //               fk5.convert(natif);
         //               al = natif.getLon() ;
         //               del = natif.getLat() ;
         Astrocoo ac = new Astrocoo(AF_ICRS,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_SGAL);
         al = ac.getLon();
         del = ac.getLat();
         //      System.out.println(c.al+" "+c.del);
      }




      //                System.out.println("c.al c.del "+al+" "+del);
      // Methode Header FITS WCS
      double ddel = (del-deltai)*deg_to_rad ;
      double dalpha =  (al- alphai)*deg_to_rad;
      //System.out.println("dalpha "+ al +" " + alphai + " " + deg_to_rad );
      double cos_del = FastMath.cos(del*deg_to_rad);
      double sin_del = FastMath.sin(del*deg_to_rad);
      double sin_dalpha = FastMath.sin(dalpha);
      double cos_dalpha = FastMath.cos(dalpha);
      //                 x_tet_phi = FastMath.cos(del*Math.PI/180.)
      //                            *FastMath.sin((al - alphai)*Math.PI/180.) ;
      x_tet_phi = cos_del *sin_dalpha ;
      //                 y_tet_phi = FastMath.sin(del*Math.PI/180.)
      //                             *FastMath.cos(deltai*Math.PI/180.)
      //                             - FastMath.cos(del*Math.PI/180.)
      //                             *FastMath.sin(deltai*Math.PI/180.)
      //                             *FastMath.cos((al - alphai)*Math.PI/180.);
      //               if (Math.abs(dalpha) < Math.PI/2 )
      y_tet_phi = sin_del * cdelz -  cos_del * sdelz * cos_dalpha ;
      //               else y_tet_phi = sin_del * cdelz + cos_del * sdelz * cos_dalpha ;
      //               System.out.println("c.al c.del "+al+" "+del+" "+cos_dalpha);
      double phi ;
      double tet ;
      switch(proj)
      {
         case SIN : // SIN proj
         case NCP :
            x_stand = x_tet_phi ;
            y_stand = y_tet_phi ;
            if ((xydpoly[1] != 0 ) && (xydpoly[2] != 0 ))
            {
               x_stand += xydpoly[1]*(1-(sin_del*cdelz - cos_del*sdelz*cos_dalpha)) ;
               y_stand += xydpoly[2]*(1-(sin_del*cdelz - cos_del*sdelz*cos_dalpha)) ;
            }
            break ;
         case TAN: // TAN proj
            double den  = sin_del * sdelz + cos_del * cdelz ;
            x_stand =  x_tet_phi / den ;
            y_stand =  y_tet_phi / den ;
            break ;
         case ZPN:
         case ARC: // Arc proj
            //                        System.out.println("al del"+al+" "+del);
            if((sin_del*cdelz- cos_del*sdelz *cos_dalpha)!=0)
               phi = Math.atan(-cos_del *sin_dalpha
                     / (sin_del*cdelz- cos_del*sdelz *cos_dalpha));
            else if(-cos_del *sin_dalpha < 0 )phi = Math.PI/2 ;
            else phi = - Math.PI/2 ;
            if (sin_del*cdelz - cos_del*sdelz*cos_dalpha >= 0)
               phi =  Math.PI + phi ;
            tet = Math.asin (
                  //   tet = Math.atan( 
                  sin_del*sdelz+ cos_del*cdelz *cos_dalpha);
            double rteta ;
            if (proj == ZPN)
            { rteta = 0.0 ;
            for (int order = 9;  order >= 0 ; order--)
            { rteta = (rteta )*(Math.PI/2-tet)+adxpoly[order];}

            //   rteta = adxpoly[1]*(Math.PI/2 -tet) +adxpoly[3]*(Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet) 
            //            + adxpoly[5]* (Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet);
            }
            else rteta = (Math.PI/2 -tet) ;
          //  if (rteta > Math.PI) System.out.println("rteta"+rteta) ;
            if (rteta < 0 ) System.out.println("rteta"+rteta) ;
            //x_stand = (Math.PI/2 -tet)*FastMath.sin(phi) ;
            x_stand = rteta*FastMath.sin(phi) ;
            // y_stand = -(Math.PI/2 -tet)*FastMath.cos(phi) ;
            y_stand = - rteta*FastMath.cos(phi) ;
          //  System.out.println("x_stand y_stand"+c.al+" "+c.del+" "+x_stand+" "+y_stand);
            break ;
         case AIT:  // AIT proj
            if (al > 180.) dalpha -= 2*Math.PI ;
            double cos_ddel = FastMath.cos(ddel) ;
            double alph =
                  Math.sqrt(2/(1+cos_ddel*FastMath.cos(dalpha/2.)));
            x_stand = 2*alph*cos_ddel*FastMath.sin(dalpha/2.) ;
            y_stand = alph*FastMath.sin(ddel) ;
            if(dalpha/2. > Math.PI) x_stand = -x_stand ;
            break ;
         case ZEA: // ZEA projection
            if((sin_del*cdelz- cos_del*sdelz *cos_dalpha)!=0)
               phi = Math.atan(-cos_del *sin_dalpha
                     / (sin_del*cdelz- cos_del*sdelz *cos_dalpha));
            else if(-cos_del *sin_dalpha < 0 )phi = Math.PI/2 ;
            else phi = - Math.PI/2 ;
            //                       phi = Math.atan(-cos_del *sin_dalpha
            //                        / (sin_del*cdelz- cos_del*sdelz *cos_dalpha));



            if (sin_del*cdelz - cos_del*sdelz*cos_dalpha > 0)
               phi =  Math.PI + phi ;

            tet = Math.asin (
                  sin_del*sdelz+ cos_del*cdelz *cos_dalpha);
            double rtet = Math.sqrt(2*(1-FastMath.sin(tet)));


            x_stand = rtet*FastMath.sin(phi) ;
            y_stand = - rtet*FastMath.cos(phi) ;


            break ;
         case STG:
            den     = 1 + sin_del*sdelz + cos_del*cdelz*cos_dalpha;
            x_stand =  2*x_tet_phi / den ;
            y_stand =  2*y_tet_phi / den ;
            break ;
         case CAR: // CARTESIEN
            x_stand = (al-alphai)*deg_to_rad ;
            y_stand = (del-deltai)*deg_to_rad ;
            break ;
         default:
            break ;
      }


   }
   c.xstand = x_stand ;
   c.ystand = y_stand ;

   }


   public void GetCoord(Coord c) throws Exception {
   double x_obj =1.;   // PFOPT: CETTE INITIALISATION EST-ELLE VRAIMENT REQUISE SYSTEMATIQUEMENT ?
   double y_obj =1.;   // PFOPT: CETTE INITIALISATION EST-ELLE VRAIMENT REQUISE SYSTEMATIQUEMENT ?
   double x_objr ;
   double y_objr ;
   double posx ;
   double posy ;

   // System.out.println("GetCoord "+c.x+" "+c.y+" "+aladin);
   // PFOPT: PROBABLEMENT LE CAS LE PLUS RARE => METTRE EN CLAUSE ELSE
   if((aladin == 1) || (aladin ==2))
   {
      //               cdelz = FastMath.cos((delta/180.)*Math.PI);
      //               sdelz = FastMath.sin((delta/180.)*Math.PI);

      // Methode aladin = methode plaque ....

      x_obj = (c.x*incX +Xorg)/1000. ;
      y_obj = (c.y*incY + Yorg)/1000. ;
      //       System.out.println("GetCoord xyobj "+x_obj+" " +y_obj);
      x_objr = (x_obj -xz) / focale ;
      y_objr = (y_obj -yz) / focale ;
      //         System.out.println("GetCoord xz yz "+xz+" "+yz);
      //         System.out.println("GetCoord xyobjr "+x_objr+" " +y_objr);
      
      // PFOPT: INITIALISER c.al=c.del=Double.NaN; ET FAIRE UN SIMPLE return; POUR EVITER L'EXCEPTION
      if (x_objr*x_objr +y_objr*y_objr > 0.19)
         throw new Exception("No coordinates") ;

      // PFOPT:  PASSER PAR DES VARIABLES INTERMEDIAIRE x2 et y2
//      double x2 = x_objr*x_objr;
//      double y2 = y_objr*y_objr;
//      posx =  xyapoly[0] +
//            xyapoly[1]*y_objr +
//            xyapoly[2]*x_objr +
//            xyapoly[3]*y2 +
//            xyapoly[4]*x2 +
//            xyapoly[5]*y_objr*x_objr +
//            xyapoly[6]*y2*y_objr +
//            xyapoly[7]*x2*x_objr +
//            xyapoly[8]*y2*x_objr +
//            xyapoly[9]*y_objr*x2 ;
      posx =  xyapoly[0] +
            xyapoly[1]*y_objr +
            xyapoly[2]*x_objr +
            xyapoly[3]*y_objr*y_objr +
            xyapoly[4]*x_objr*x_objr +
            xyapoly[5]*y_objr*x_objr +
            xyapoly[6]*y_objr*y_objr*y_objr +
            xyapoly[7]*x_objr*x_objr*x_objr +
            xyapoly[8]*y_objr*y_objr*x_objr +
            xyapoly[9]*y_objr*x_objr*x_objr ;

      posy =  xydpoly[0] +
            xydpoly[1]*y_objr +
            xydpoly[2]*x_objr +
            xydpoly[3]*y_objr*y_objr +
            xydpoly[4]*x_objr*x_objr +
            xydpoly[5]*y_objr*x_objr +
            xydpoly[6]*y_objr*y_objr*y_objr +
            xydpoly[7]*x_objr*x_objr*x_objr +
            xydpoly[8]*y_objr*y_objr*x_objr +
            xydpoly[9]*y_objr*x_objr*x_objr ;


      //   System.out.println("GetCoord pos "+posx+" " +posy);

      c.al = alpha
            + (Math.atan(posx/(cdelz-posy*sdelz)))*rad_to_deg ;
      c.del = Math.atan(FastMath.cos((c.al-alpha)*deg_to_rad)
            *(sdelz +posy *cdelz)/(cdelz-posy*sdelz))
            //                     *(180./Math.PI);
            *rad_to_deg ;


      if((c.del * delta< 0)&&(Math.abs(delta) > 87.))
      {
         c.al += 180.;
         c.del = -c.del;
      }

      // PFOPT: AJOUTER UN else AVANT LE DEUXIEME if
      if(c.al > 360.) c.al -= 360.;
      if(c.al <   0.) c.al += 360.;
      // System.out.println("Getcoord "+c.al+" " +c.del);
   }
   else
   {
      // Methode Header FITS WCS
      //    System.out.println("xy Coord"+c.x+" "+c.y);

      x_obj = c.x - Xcen;
      y_obj = ynpix - Ycen -c.y;
      if ((proj == TAN) && (xyapoly[1] != 0)&&(xyapoly[1] != 1) && (aladin ==3)
            && (xydpoly[2]*CD[1][1] <0 ))
         y_obj = c.y - Ycen ;
     if ((proj == SIP)||(proj == SINSIP))
    //    if (proj == SIP) 
      {
         double xint = x_obj;
         double yint = y_obj;
         // PFOPT: NE PAS FAIRE DE CALCUL DANS LE TEST DE FIN DE BOUCLE
         // REMPLACER order < order_a+1 PAR order <=order_a
         for (int order = 0;  order <= order_a ; order++)
         {
            // PFOPT: IDEM
            for (int powx =0 ; powx <= order ; powx++ )
            {
                  x_obj = x_obj + sip_a[powx][order-powx]*Math.pow(xint,(powx))*Math.pow(yint,order-powx);
            }
         }
         // PFOPT: IDEM
        for (int order = 0;  order <= order_b ; order++)
         {
           // PFOPT: IDEM
            for (int powx =0 ; powx <= order ; powx++ )
            {
                  y_obj = y_obj + sip_b[powx][order-powx]*Math.pow(xint,(powx))*Math.pow(yint,order-powx);
            }
         }
         //x_obj = xint + 
               //        a[0][2]*yint*yint +
         //        a[0][3]*yint*yint*yint +
         //        a[1][1]*xint*yint +
         //        a[1][2]*xint*yint*yint +
         //        a[2][0]*xint*xint +
         //        a[2][1]*xint*xint*yint +
         //        a[3][0]*xint*xint*xint ;
         //y_obj = yint + 
         //b[0][2]*yint*yint +
         //b[0][3]*yint*yint*yint +
         //b[1][1]*xint*yint +
         //b[1][2]*xint*yint*yint +
         //b[2][0]*xint*xint +
         //b[2][1]*xint*xint *yint+
         //b[3][0]*xint*xint*xint;
      }

      //              System.out.println("xyobj "+x_obj+" "+y_obj);
      //                   y_obj = Ycen -c.y ;
      //   y_obj = c.y -Ycen ;

      //                 x_objr = (CD[0][0]*x_obj +CD[0][1]*y_obj)/cdelz  ;
      //                 System.out.println("CD "+CD[0][0]+" "+CD[0][1]);
      //                 System.out.println("CD "+CD[1][0]+" "+CD[1][1]);
      x_objr = (CD[0][0]*x_obj +CD[0][1]*y_obj)  ;
      y_objr = (CD[1][0]*x_obj +CD[1][1]*y_obj) ;
      //                 System.out.println("xyobjr "+x_objr+" "+y_objr);
      //                 x_objr *= Math.PI/180. ;
      //                 y_objr *= Math.PI/180. ;
      //          if (x_obj == 0.0) System.out.println("xyobjr "+x_objr+" "+y_objr);   
      x_objr *= deg_to_rad ;
      y_objr *= deg_to_rad ;
      double  yy = y_objr ;

      //          System.out.println("xyobjr "+x_objr+" "+y_objr);

      double X ;
      double tet ;
      //  System.out.println("Proj "+proj);
      
      // PFOPT: PLACER LES case LES PLUS PROBABLE EN DEBUT DU switch
      // PFOPT: UTILISER x2=x_objr*x_objr et y2=y_objr*y_objr
      switch(proj)
      {
         case SIN: // projection en SINUS
         case SINSIP:    
            //                        c.del = (180./Math.PI)
            //                               *(Math.asin(y_objr*FastMath.cos(deltai*Math.PI/180.)
            //                               +FastMath.sin(deltai*Math.PI/180.)
            //                               *Math.sqrt(1-y_objr*y_objr - x_objr*x_objr)));
            c.del = rad_to_deg
            *(Math.asin(y_objr*cdelz
                  +sdelz
                  *Math.sqrt(1-y_objr*y_objr - x_objr*x_objr)));
            X = x_objr /
                  //                                   (FastMath.cos(deltai*Math.PI/180.)
                  (cdelz
                        *Math.sqrt(1-y_objr*y_objr - x_objr*x_objr)
                        //                                   - y_objr*FastMath.sin(deltai*Math.PI/180.));
                        - y_objr*sdelz);
            c.al  = alphai + rad_to_deg*Math.atan(X) ;
            //                        double sign ;
            //                        if (deltai == 0) sign = 1;
            //                        else sign = deltai / Math.abs(deltai) ;
            //                        if( sign*y_objr -cdelz > 0)
            if ( cdelz*Math.sqrt(1-y_objr*y_objr - x_objr*x_objr)
                  - y_objr*sdelz < 0) c.al += 180. ;
           
                 if ((xydpoly[1] != 0 ) && (xydpoly[2] != 0 ))
                 {
                   double xi = xydpoly[1] ;
                   double eta = xydpoly[2] ;
                   double a = xi*xi + eta*eta +1 ;
                   double b = (x_objr-xi)*xi + eta*(y_objr-eta);
                   double C = (x_objr-xi)*(x_objr-xi) + (y_objr-eta)*(y_objr-eta) - 1 ;
                 //  System.out.println("x y "+x_obj+" "+y_obj) ;
                  // System.out.println("xy_objr "+x_objr+" "+y_objr);
                 //  System.out.println ("a b c "+a+" "+b+" "+C) ;
                   double arg = (-b + Math.sqrt (b*b-a*C))/a ; 
                 //  System.out.println(" racine "+ (b*b-a*C));
                 //  System.out.println("arg "+arg);
                    tet = Math.asin(arg) ;
                    double phi = Math.atan2(x_objr - xi*(1-arg), -(y_objr - eta*(1-arg))) ;         
                 //   System.out.println("tet phi "+tet+" "+phi);
                 //   System.out.println("x-stand "+ (Math.cos(tet)*Math.sin(phi)+xi*(1-Math.sin(tet))));
                  //  System.out.println("y-stand "+ (Math.cos(tet)*Math.cos(phi)-eta*(1-Math.sin(tet))));
                  c.del = - rad_to_deg*
                  Math.asin(-sdelz*FastMath.sin(tet)+
                        cdelz*FastMath.cos(tet)*FastMath.cos(phi));

            
            double arg11 = (FastMath.sin(tet)*cdelz
                  - FastMath.cos(tet)*sdelz*FastMath.cos(phi));
            double argg ;
          
            argg = -(FastMath.cos(tet)*FastMath.sin(phi));
           
           // System.out.println("deltai alphai"+deltai+" "+alphai+" "+rad_to_deg*Math.atan2(argg,arg11)) ;
            
            // PFOPT: Remplacer Math.abs(deltai) != 90. PAR deltai!=90 && deltai!=-90
            if (Math.abs(deltai) != 90.)
             
               c.al = alphai - rad_to_deg*Math.atan2(argg,arg11) ;
            else if (deltai == 90.)c.al = rad_to_deg*(phi+Math.PI) ;
            else c.al = rad_to_deg*(-phi);
           // System.out.println("0000 C.aldel"+c.del+" "+c.al+"\n");
            if((c.del*c.del > 90.*90.)&&(Math.abs(deltai) > 65.))
            {
               c.al = 180. - c.al ;
               c.del = 2*deltai - c.del ;
            }
                           //    System.out.println(" C.aldel"+c.del+" "+c.al+"\n");
                 }
            break ; 
         case NCP:

            X = x_objr / (cdelz - y_objr*sdelz);
            c.al  = alphai + rad_to_deg*Math.atan(X) ;
            c.del = rad_to_deg
                  *(Math.acos((cdelz- yy*sdelz)/FastMath.cos((c.al-alphai)*deg_to_rad))) ;
            if((cdelz- yy*sdelz)/FastMath.cos((c.al- alphai)*deg_to_rad)>1)  
            { c.del = -32000.0 ; c.al = -32000.0;}
            if (sdelz <0) c.del = -c.del ;
            if (c.del > 90.)
            { c.del = 180. -c.del ;
            c.al = 180. + c.al ;
            }
            if (c.del < - 90.)
            { c.del = -180. -c.del ;
            c.al = 180. + c.al ;
            }

            break ;
         case TPV:   
         case TAN:  // projection en TAN
         case SIP:     
           //    System.out.println("xyobjr "+x_objr+" " +yy);
                        
       
               if ((xyapoly[1] != 0)&&(xyapoly[1] != 1) && (aladin == 0))
            {
                 
               x_objr *= rad_to_deg ;
               yy     *= rad_to_deg ;
               double r = Math.sqrt(x_objr*x_objr+yy*yy) ;
            /* Changes for SCAMP */
     
         // System.out.println("9 "+xyapoly[9]+ " "+ xydpoly[9]);
           // System.out.println("10 "+xyapoly[10]+ " "+ xydpoly[10]);
               posx =  xyapoly[0] +
               xyapoly[1]*x_objr+
               xyapoly[2]*yy +
               xyapoly[3]*Math.sqrt(x_objr*x_objr+yy*yy) +
               xyapoly[4]*x_objr*x_objr +
               xyapoly[5]*yy*x_objr +
               xyapoly[6]*yy*yy +
               xyapoly[7]*x_objr*x_objr*x_objr +
               xyapoly[8]*yy*x_objr*x_objr +
               xyapoly[9]*yy*yy*x_objr +
               xyapoly[10]*yy*yy*yy +
               xyapoly[11]*r*r*r ;

         posy =  xydpoly[0] +
               xydpoly[1]*yy +
               xydpoly[2]*x_objr +
               xydpoly[3]*Math.sqrt(x_objr*x_objr+yy*yy) +
               xydpoly[4]*yy*yy +
               xydpoly[5]*yy*x_objr +
               xydpoly[6]*x_objr*x_objr  +
               xydpoly[7]*yy*yy*yy +
               xydpoly[8]*yy*yy*x_objr +
               xydpoly[9]*yy*x_objr*x_objr  +
               xydpoly[10]*x_objr*x_objr*x_objr +
               xydpoly[11]*r*r*r ;
         x_objr =  posx * deg_to_rad ;
         yy = posy * deg_to_rad ;
            }
               if ((xyapoly[1] != 0)&&(xyapoly[1] != 1) && (aladin == 3))
               {
                     
                   x_objr *= rad_to_deg ;
                   yy     *= rad_to_deg ;
                posx =  xyapoly[0] +
                     xyapoly[2]*yy +
                     xyapoly[1]*x_objr +
                     xyapoly[5]*yy*yy +
                     xyapoly[3]*x_objr*x_objr +
                     xyapoly[4]*yy*x_objr +
                     xyapoly[9]*yy*yy*yy +
                     xyapoly[6]*x_objr*x_objr*x_objr +
                     xyapoly[8]*yy*yy*x_objr +
                     xyapoly[7]*yy*x_objr*x_objr ;

               posy =  xydpoly[0] +
                     xydpoly[2]*yy +
                     xydpoly[1]*x_objr +
                     xydpoly[5]*yy*yy +
                     xydpoly[3]*x_objr*x_objr +
                     xydpoly[4]*yy*x_objr +
                     xydpoly[9]*yy*yy*yy +
                     xydpoly[6]*x_objr*x_objr*x_objr +
                     xydpoly[8]*yy*yy*x_objr +
                     xydpoly[7]*yy*x_objr*x_objr ; 
                 x_objr =  posx  ;
                 yy =  posy  ;
               // x_objr = posx ;
               // yy = posy ;
             //   System.out.println("pos "+posx+" " +posy);
            }
 
           // System.out.println("cdel"+cdelz+" "+sdelz);
            //                       double deno = FastMath.cos(deltai*Math.PI/180.)
            double deno = cdelz
                  //                                     -yy*FastMath.sin(deltai*Math.PI/180.);
                  -yy*sdelz;
            double d_al = Math.atan(x_objr/deno) ;
            c.del = (180./Math.PI)*Math.atan(FastMath.cos(d_al)

                  //                                *(FastMath.sin(deltai*Math.PI/180.)
                  *(sdelz +yy*cdelz) / deno ) ;
            //                System.out.println("d_al "+x_objr+" "+c.del) ;
            //                                +y_objr*FastMath.cos(deltai*Math.PI/180.))
            //                        c.al = alphai + d_al*180./Math.PI;
            c.al = alphai + d_al*rad_to_deg;
            // Pourquoi Ã¯Â¿Â½Ã¯Â¿Â½ ?
            //                        if((c.del * deltai< 0)&&(Math.abs(deltai) > 87.))
            //                           {
            //                             c.al += 180.;
            //                             c.del = -c.del;
            //                            }
            if (deno < 0.0) 
            {
               c.al += 180.;
               c.del = -c.del ;
            }
            //                          System.out.println("c al del  "+c.al+" "+c.del);
            break ;
         case ZPN: 
         case ARC: //ARC proj
             //System.out.println("ARC ARC");
            //        tet =  Math.sqrt(x_objr*x_objr+y_objr*y_objr);
            double rteta = Math.sqrt(x_objr*x_objr+y_objr*y_objr);
            if (proj == ZPN) 
            {
              // System.out.println("ZPN");
               tet = 0 ;
               int niter = 20 ;
               double dtet ;
               int iter = 0 ;
               //        System.out.println("adxpoly "+adxpoly[1]+" "+adxpoly[3]+" "+adxpoly[5]);
               while (iter < niter)
               {   double rrr =0.0 ;
               double drrr = 0.0 ;
               for (int order = 9;  order >= 0 ; order--)
               { rrr = (rrr )*tet+adxpoly[order];}
               for (int order = 8;  order >= 0 ; order--)
               {  
                  drrr = drrr * tet + (order+1)*adxpoly[order+1];
               }
               //   dtet = (rteta- adxpoly[1]*tet -adxpoly[3]* tet*tet*tet-adxpoly[5]*tet*tet*tet*tet*tet)/(adxpoly[1]+3*adxpoly[3]* tet*tet+5*adxpoly[5]*tet*tet*tet*tet) ;
               dtet = (rteta -rrr)/drrr ;  
               tet += dtet ;
               //     System.out.println("tet"+iter+" "+rteta+" "+tet+" "+dtet);
               iter++ ;
               }
            }
            else tet = rteta;
            
            if (rteta == 0.0) { c.del = deltai ; c.al = alphai; }
            else c.del = rad_to_deg*Math.asin(+y_objr*cdelz*FastMath.sin(tet)/ rteta +sdelz*FastMath.cos(tet));

            if (tet==0.0 ) c.al = alphai ;
            else if ((tet < Math.PI/2)||(x_objr ==0.0))
               c.al =alphai + rad_to_deg*Math.asin(FastMath.sin(tet)*x_objr/ (rteta*FastMath.cos(c.del*deg_to_rad)));

            else c.al =alphai + 180. - rad_to_deg*Math.asin(FastMath.sin(tet)*x_objr/(rteta*FastMath.cos(c.del*deg_to_rad))) ;
//            }
            //         if(tet==0.0)c.al=alphai;
          //  System.out.println("tete "+tet+" "+(alphai + rad_to_deg*Math.asin(FastMath.sin(tet)*x_objr/ (rteta*FastMath.cos(c.del*deg_to_rad))))+" "+(alphai + 180. - rad_to_deg*Math.asin(FastMath.sin(tet)*x_objr/(rteta*FastMath.cos(c.del*deg_to_rad)))));
     // if (tet == 0.0)     System.out.println("xobj yobj"+x_objr+" "+y_objr);
      // if (tet == 0.0)     System.out.println("tet alphai"+tet+" "+c.al+" "+alphai);
            break;

         case AIT:  // projection AITOFF
            // Il faut gÃ¯Â¿Â½rer le cdelp, sdelp de telle sorte que
            // la position du pole des coordonnes locales reste toujours
            // inferieur a 90 degres
            double cdelp = FastMath.cos(deltai*deg_to_rad+Math.PI/2);
            double sdelp = FastMath.sin(deltai*deg_to_rad+Math.PI/2);
            if (deltai > 0.)
            {
               cdelp = FastMath.cos((90.-deltai)*deg_to_rad);
               sdelp = FastMath.sin((90.-deltai)*deg_to_rad);
            }
            double phi ;
            double z = 1 - x_objr*x_objr/16 -y_objr*y_objr/4;
            if (z < 0.5) throw new Exception("No coordinates") ;
            //{ c.del = -32000.0 ; c.al = -32000.0;}
            else
            {
               double Z =  Math.sqrt(z);
               tet = Math.asin(y_objr*Z) ;
               phi = 2*Math.atan((x_objr*Z/2)/(2*Z*Z-1));
               // ci dessous permet de gerer la position du pole "vrai" par rapport au point de contact et au
               // pole des coordonnÃ¯Â¿Â½es locales
               if (deltai <0) phi += Math.PI ;
               //               System.out.println("tet phi Coord"+tet+" "+phi);
               //                System.out.println("Le sin de delt"+sdelp*FastMath.sin(tet));
               //                System.out.println("Le sin de delt"+cdelp*FastMath.cos(tet)*FastMath.cos(phi));
               //                System.out.println("Le sin de delt"+(sdelp*FastMath.sin(tet)+cdelp*FastMath.cos(tet)*FastMath.cos(phi)));
               
               double ctet = FastMath.cos(tet); 
               double stet = FastMath.sin(tet);  // PFOPT: PEUT ETRE PRENDRE DIRECTEMENT ctet = y_objr*Z
               double cphi = FastMath.cos(phi);
               c.del = rad_to_deg* Math.asin((sdelp*stet+ cdelp*ctet*cphi));
               double arg1 = -(stet*cdelp - ctet*sdelp*cphi);
               double arg  = (ctet*FastMath.sin(phi));
               //      if (Math.abs(deltai) != 90.)
               c.al = alphai + rad_to_deg*Math.atan2(arg,arg1) ;
               if (deltai < 0.) c.al = c.al + 180. ;
               //               c.al = alphai + rad_to_deg*phi ;

               //   System.out.println((rad_to_deg*Math.atan2(arg,arg1))+" ");
//               if((c.del*c.del > 90.*90.)&&(Math.abs(deltai) > 65.))
//               {
                  // c.al = 180. - c.al ;
                  // c.del = 2*deltai - c.del ;
//               }
            }
            //              System.out.println("al del Coord"+c.al+" "+c.del);

            break ;
         case MOL:
            cdelp = FastMath.cos(deltai*deg_to_rad+Math.PI/2);
            sdelp = FastMath.sin(deltai*deg_to_rad+Math.PI/2) ;

            double x =  x_objr;
            double y =  y_objr;

            double rSq = x * x / 8 + y * y / 2 ;
            if (rSq >1 ) throw new Exception("No coordinates") ;

            double theta = Math.asin(y / Math.sqrt(2)) ;
            double psi = theta * 2;

            double Tetha = Math.asin((psi + FastMath.sin(psi)) / Math.PI);
            double Phi =(Math.PI/(2*Math.sqrt(2)))*( x / FastMath.cos(theta));

            c.del =  rad_to_deg* Math.asin((sdelp*FastMath.sin(Tetha)
                  - cdelp*FastMath.cos(Tetha)*FastMath.cos(Phi)));

            double arg3 = (FastMath.sin(Tetha)*cdelp
                    + FastMath.cos(Tetha)*sdelp*FastMath.cos(Phi));

            double arg2 = (FastMath.cos(Tetha)*FastMath.sin(Phi));

            c.al = alphai + rad_to_deg*Math.atan2(arg2,arg3) ;

            break;
        case GLS:
            
            cdelp = FastMath.cos(deltai*deg_to_rad+Math.PI/2);
            sdelp = FastMath.sin(deltai*deg_to_rad+Math.PI/2) ;

  
             
            Tetha = y_objr ;
            Phi = x_objr/ Math.cos(y_objr);

            c.del =  rad_to_deg* Math.asin((sdelp*FastMath.sin(Tetha)
                  - cdelp*FastMath.cos(Tetha)*FastMath.cos(Phi)));

            arg3 = (FastMath.sin(Tetha)*cdelp
                    + FastMath.cos(Tetha)*sdelp*FastMath.cos(Phi));

            arg2 = (FastMath.cos(Tetha)*FastMath.sin(Phi));

            c.al = alphai + rad_to_deg*Math.atan2(arg2,arg3) ;

            break;
            

               //            double x =  x_objr;
               //            double y =  y_objr;
               //            double rSq = x * x / 8  + y * y / 2;
               //            if( rSq > 1 ) throw new Exception("No coordinates");
               //            double theta = Math.asin(y / Math.sqrt(2));
               //            double psi = theta * 2;
               //            double delta = Math.asin((psi + FastMath.sin(psi)) / Math.PI);
               //            double alpha = (Math.PI/(2*Math.sqrt(2))) * (x / FastMath.cos(theta));
               //            c.al = alphai + alpha * rad_to_deg;
               //            c.del = /* deltai + */delta * rad_to_deg;
               //            break;
            
         case ZEA:  // projection ZEA
            double rtet =
            Math.sqrt(x_objr*x_objr +y_objr*y_objr)/2.;
            tet = Math.PI/2. - 2*Math.asin(rtet);
            if(y_objr != 0.0) phi = Math.atan(-x_objr/y_objr);
            else if (x_objr != 0)phi = Math.PI/2. * (-x_objr /x_objr) ;
            else phi = Math.PI/2. ;
            if(y_objr < 0.0) phi = phi+Math.PI ;
            //               System.out.println("phi tet"+phi+" "+tet);
            //               System.out.println("Le sin de delt"+sdelz*FastMath.sin(tet));
            //               System.out.println("Le sin de delt"+cdelz*FastMath.cos(tet)*FastMath.cos(phi));
            //               System.out.println("Le sin de delt"+(sdelz*FastMath.sin(tet)+cdelz*FastMath.cos(tet)*FastMath.cos(phi)));
            c.del = rad_to_deg*
                  Math.asin(sdelz*FastMath.sin(tet)+
                        cdelz*FastMath.cos(tet)*FastMath.cos(phi));

            //    if((y_objr < 0.0)) c.del = 2*deltai - c.del ;

            //                   double arg1 = (FastMath.sin(tet)*FastMath.cos(deltai*Math.PI/180.)
            //                   - FastMath.cos(tet)*FastMath.sin(deltai*Math.PI/180.)*FastMath.cos(phi));
            double arg1 = (FastMath.sin(tet)*cdelz
                  - FastMath.cos(tet)*sdelz*FastMath.cos(phi));
            double arg ;
            //                   if(arg1 != 0)
            //                   arg = -(FastMath.cos(tet)*FastMath.sin(phi))/ arg1 ;
            //                   else arg = 0.0 ;
            arg = -(FastMath.cos(tet)*FastMath.sin(phi));
            // if(y_objr < 0.0) arg = -arg ;
            if (Math.abs(deltai) != 90.)
               //                   c.al = alphai + (180./Math.PI)*Math.atan(arg) ;
               //                   else if (deltai == 90.)c.al = (180./Math.PI)*(phi+Math.PI) ;
               //                   else c.al = (180./Math.PI)*(-phi);

               //                   c.al = alphai + rad_to_deg*Math.atan(arg) ;
               c.al = alphai + rad_to_deg*Math.atan2(arg,arg1) ;
            else if (deltai == 90.)c.al = rad_to_deg*(phi+Math.PI) ;
            else c.al = rad_to_deg*(-phi);
            if((c.del*c.del > 90.*90.)&&(Math.abs(deltai) > 65.))
            {
               c.al = 180. - c.al ;
               c.del = 2*deltai - c.del ;
            }
            //                   System.out.println(" C.aldel"+c.del+" "+c.al+"\n");
            break ;
         case STG:    // STEREOGRAPHIC
            double sintet =
            FastMath.sin(Math.PI/2
                  - 2*Math.atan(Math.sqrt(y_objr*y_objr + x_objr*x_objr)/2));
            //                            c.del = (180./Math.PI)
            //                            *Math.asin(FastMath.cos(deltai*Math.PI/180.)*y_objr/2
            //                               + sintet *(FastMath.sin(deltai*Math.PI/180.) +
            //                                  FastMath.cos(deltai*Math.PI/180.)*y_objr/2));
            c.del = rad_to_deg
                  *Math.asin(cdelz*y_objr/2
                        + sintet *(sdelz + cdelz*y_objr/2));
            deno =
                  //                      sintet* (2*cdelz-y_objr*FastMath.sin(deltai*Math.PI/180.)) -y_objr*FastMath.sin(deltai*Math.PI/180.);
                  sintet* (2*cdelz-y_objr*sdelz) -y_objr*sdelz;
            //                      c.al = alphai + (180./Math.PI) * Math.atan(
            //                      c.al = alphai + rad_to_deg  * Math.atan(
            //                          x_objr*(1+sintet) /deno) ;
            c.al = alphai + rad_to_deg * Math.atan2(x_objr*(1+sintet),deno) ;
            break ;
         case FIE:
             tet = Math.sqrt(x_objr*x_objr+y_objr*y_objr);
             phi= Math.atan2(y_objr,x_objr) ;
             c.del = rad_to_deg*
             Math.asin(sdelz*FastMath.cos(tet)-
                   cdelz*FastMath.sin(tet)*FastMath.cos(phi));
             double arg11 = (FastMath.cos(tet)*cdelz
                     - FastMath.sin(tet)*sdelz*FastMath.cos(phi));
             double argg ;
               argg = -(FastMath.sin(tet)*FastMath.sin(phi));
               //      if (Math.abs(deltai) != 90.)
               c.al = alphai + rad_to_deg*Math.atan2(argg,arg11) ;
             break ;
             
             
         case CAR: // CARTESIEN
            //         System.out.println("x_objr "+x_objr+" "+y_objr) ;
            c.al = alphai +x_objr*rad_to_deg ;
            c.del= deltai +y_objr*rad_to_deg ;
            //       System.out.println("x_objr "+x_objr * rad_to_deg+" "+y_objr * rad_to_deg) ;
            //       System.out.println("aldel "+c.al+" "+c.del);
            break ;
         case SOL: // SOLAR
            c.al = alphai +x_objr* rad_to_deg;
            c.del = deltai +y_objr * rad_to_deg;
            break;
         default:
            break ;
      }
      if (system == XYLINEAR) return ;

      //     if ((equinox != 2000.0) && (system != GALACTIC))
      if (system == FK4)
      {
         // PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //                 Astroframe j2000 = new Astroframe() ;
         //                 Astroframe natif = new Astroframe(1,Astroframe.MAS+1,equinox) ;
         //                 natif.set(c.al,c.del) ;
         //                 natif.convert(j2000) ;
         //                 c.al = j2000.getLon() ;
         //                 c.del = j2000.getLat() ;
         Astrocoo ac = new Astrocoo(AF_FK4,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_ICRS);
         c.al = ac.getLon();
         c.del = ac.getLat();
      }
      if (system == FK5)
      {
         // PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //                  Astroframe j2000 = new Astroframe() ;
         //                  Astroframe natif = new Astroframe(1,Astroframe.MAS+1,equinox) ;
         //                  natif.set(c.al,c.del) ;
         //                  natif.convert(j2000) ;
         //                  c.al = j2000.getLon() ;
         //                  c.del = j2000.getLat() ;
         Astrocoo ac = new Astrocoo(AF_FK5,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_ICRS);
         c.al = ac.getLon();
         c.del = ac.getLat();
      }
      if (system == GALACTIC)
      {
         // PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //                 Astroframe fk5 = new Astroframe() ;
         //                 Astroframe natif =  new Astroframe(2,Astroframe.MAS+1,equinox);
         //                 natif.set(c.al,c.del) ;
         //                 natif.convert(fk5);
         //                 c.al = fk5.getLon() ;
         //                 c.del = fk5.getLat() ;
         Astrocoo ac = new Astrocoo(AF_GAL,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_ICRS);
         c.al = ac.getLon();
         c.del = ac.getLat();
      }
      if (system == SUPERGALACTIC)
      {
         //PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //               Astroframe fk5 = new Astroframe() ;
         //               Astroframe natif =  new Astroframe(2,Astroframe.MAS+1,equinox);
         //               natif.set(c.al,c.del) ;
         //               natif.convert(fk5);
         //               c.al = fk5.getLon() ;
         //               c.del = fk5.getLat() ;
         Astrocoo ac = new Astrocoo(AF_SGAL,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_ICRS);
         c.al = ac.getLon();
         c.del = ac.getLat();
      }
      if (system == ECLIPTIC)
      {
         //PF 12/06 - Modif pour utilisation nouvelles classes Astrocoo de Fox                
         //               Astroframe fk5 = new Astroframe() ;
         //               Astroframe natif =  new Astroframe(2,Astroframe.MAS+1,equinox);
         //               natif.set(c.al,c.del) ;
         //               natif.convert(fk5);
         //               c.al = fk5.getLon() ;
         //               c.del = fk5.getLat() ;
         Astrocoo ac = new Astrocoo(AF_ECL,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(AF_ICRS);
         c.al = ac.getLon();
         c.del = ac.getLat();
      }
      //              System.out.println("GetCoord "+c.al+" " +c.del);


      // PF. 26 nov 2017 - SURTOUT PAS !!
//      if(c.al >= 360.) c.al -= 360.;
//      if(c.al <   0.) c.al += 360.;
      
      //   System.out.println("coord "+c.al+" " +c.del);
   }
   }

   protected boolean TheSame(Calib cal) {

   if (aladin == 1) return false ;
   if (xnpix != cal.xnpix) return false ;
   if (ynpix != cal.ynpix) return false ;
   if (Xcen != cal.Xcen) return false ;
   if (Ycen != cal.Ycen) return false ;
   if (alphai != cal.alphai) return false ;
   if (deltai != cal.deltai) return false ;
   if (CD[0][0] != cal.CD[0][0]) return false ;
   if (CD[0][1] != cal.CD[0][1]) return false ;
   if (CD[1][0] != cal.CD[1][0]) return false ;
   if (CD[1][1] != cal.CD[1][1]) return false ;
   if (equinox != cal.equinox) return false ;
   if (proj != cal.proj) return false ;
   return true ;
   }
   public void GetXY(Coord c) throws Exception { GetXY(c,true); }
   public void GetXY(Coord c,boolean withTest) throws Exception {
   double x_obj =1.;
   double y_obj =1.;
   double x_tet_phi;
   double y_tet_phi;
   double y_stand =0.03;
   double x_stand =0.03;
   double delrad ;
   double alrad ;
   double dr;
   double al,del ;

   //   System.out.println("GetXY aladin"+aladin+" "+c.al+" "+c.del+" "+system);

     // System.out.println("GetXY aladin"+aladin+" "+c.al+" "+c.del+" "+system);

   if(aladin == 1)
   {
      //               cdelz = FastMath.cos((delta/180.)*Math.PI);
      //               sdelz = FastMath.sin((delta/180.)*Math.PI);
      double cos_del = FastMath.cos(c.del*deg_to_rad);
      //                 double sin_del = FastMath.sin(c.del*deg_to_rad);  PF => jamais utilisÃ¯Â¿Â½
      //                 double dalpha =  (c.al- alphai)*deg_to_rad;   PF => jamais utilisÃ¯Â¿Â½
      double distalpha = Math.min(Math.abs(c.al-alphai),360.-Math.abs(c.al-alphai));
      if (cos_del*(distalpha)*cos_del*(distalpha)+(c.del-deltai)*(c.del-deltai)>625.0)
         throw new Exception("Outside the projection") ;
      // Methode aladin = methode plaque ....
      //             delrad = (c.del/180.)*Math.PI;
      delrad = c.del*deg_to_rad;
      alrad  = (c.al - alpha)*deg_to_rad;
      double sin_delrad = FastMath.sin (delrad) ;
      double cos_delrad = FastMath.cos (delrad) ;
      double sin_alrad  = FastMath.sin(alrad) ;
      double cos_alrad  = FastMath.cos(alrad) ;
      dr = sin_delrad * sdelz
            + cos_delrad * cdelz * cos_alrad;
      x_stand =  cos_delrad
            * sin_alrad/dr ;
      y_stand = (sin_delrad *  cdelz
            - cos_delrad * sdelz
            * cos_alrad) / dr;
      //            ("xy_stand "+x_stand+" " +y_stand);

      x_obj =  adxpoly[0] +
            adxpoly[1]*x_stand +
            adxpoly[2]*y_stand +
            adxpoly[3]*x_stand*x_stand +
            adxpoly[4]*y_stand*y_stand +
            adxpoly[5]*y_stand*x_stand +
            adxpoly[6]*x_stand*x_stand*x_stand +
            adxpoly[7]*y_stand*y_stand*y_stand +
            adxpoly[8]*y_stand*x_stand*x_stand +
            adxpoly[9]*y_stand*y_stand*x_stand ;

      y_obj =  adypoly[0] +
            adypoly[1]*x_stand +
            adypoly[2]*y_stand +
            adypoly[3]*x_stand*x_stand +
            adypoly[4]*y_stand*y_stand +
            adypoly[5]*y_stand*x_stand +
            adypoly[6]*x_stand*x_stand*x_stand +
            adypoly[7]*y_stand*y_stand*y_stand +
            adypoly[8]*y_stand*x_stand*x_stand +
            adypoly[9]*y_stand*y_stand*x_stand ;

      //            System.out.println("coord "+x_obj+" " +y_obj);
      
      x_obj = x_obj *focale +xz;
      y_obj = y_obj *focale +yz;

      //            System.out.println("coord "+x_obj+" " +y_obj);

      //PIERRE      c.xf = (x_obj *1000.0 - Xorg)/incX;
      //PIERRE      c.yf = (y_obj *1000.0 - Yorg)/incY ;
      c.x = (x_obj *1000.0 - Xorg)/incX;
      c.y = (y_obj *1000.0 - Yorg)/incY ;


      //            System.out.println("coord "+c.x+" " +c.y);

   }
   else
   {
      al = c.al ;
      del = c.del ;
      // System.out.println(c.al+" "+c.del);
      if( system!=ICRS && system!=XYLINEAR ) {
         Astroframe af = system==FK4           ? AF_FK4 :
                         system==FK5           ? AF_FK5 :
                         system==GALACTIC      ? AF_GAL :
                         system==SUPERGALACTIC ? AF_SGAL:
                         system==ECLIPTIC      ? AF_ECL : null;
         Astrocoo ac = new Astrocoo(AF_ICRS,c.al,c.del);
         ac.setPrecision(Astrocoo.MAS+1);
         ac.convertTo(af);
         al = ac.getLon();
         del = ac.getLat();
      }
      
      //                System.out.println("c.al c.del "+c.al+" "+del);
      // Methode Header FITS WCS
      //                 double ddel = (del-deltai)*deg_to_rad ;    PF => Non utilisÃ¯Â¿Â½
      //                 double cos_ddel = FastMath.cos(ddel) ;         PF => Non utilisÃ¯Â¿Â½
      double dalpha =  (al- alphai)*deg_to_rad;
      //                System.out.println("dalpha "+ c.al +" " + alphai + " " + deltai + " " + deg_to_rad );
      double cos_del = FastMath.cos(del*deg_to_rad);
      double sin_del = FastMath.sin(del*deg_to_rad);
      double sin_dalpha = FastMath.sin(dalpha);
      double cos_dalpha = FastMath.cos(dalpha);
      //                 x_tet_phi = FastMath.cos(del*Math.PI/180.)
      //                            *FastMath.sin((al - alphai)*Math.PI/180.) ;
      x_tet_phi = cos_del *sin_dalpha ;
      //                 y_tet_phi = FastMath.sin(del*Math.PI/180.)
      //                             *FastMath.cos(deltai*Math.PI/180.)
      //                             - FastMath.cos(del*Math.PI/180.)
      //                             *FastMath.sin(deltai*Math.PI/180.)
      //                             *FastMath.cos((al - alphai)*Math.PI/180.);
      //             if (Math.abs(dalpha) < Math.PI/2)
      y_tet_phi = sin_del * cdelz -  cos_del * sdelz * cos_dalpha ;
      //             else y_tet_phi = sin_del * cdelz + cos_del * sdelz * cos_dalpha ;
      //             System.out.println("al del "+al+" "+del+" "+cos_dalpha+" "+y_tet_phi+" "+x_tet_phi);

      double phi ;
      double tet ;
         //      int goodness = 1;

      if( withTest ) {
         switch(proj) {
            case SIN:
            case TAN: // TAN proj
            case SINSIP:    
            case NCP : // NCP
            case SIP:   
            case TPV:
               if (dalpha > Math.PI )   dalpha = -2*Math.PI +dalpha ;
               if (dalpha < -Math.PI )  dalpha = + 2*Math.PI +dalpha ;
               if ((-sin_del * sdelz)/(cos_del * cdelz) > 1  )
                  //                       { x_stand= 0.0 ; y_stand = 0.0 ; goodness = 0;}
                  throw new Exception("Outside the projection") ;
               else    if (((-sin_del * sdelz)/(cos_del * cdelz) > -1 )&& (Math.abs(dalpha) > Math.acos((-sin_del * sdelz)/(cos_del * cdelz)) ))
                  //   { x_stand= 0.0 ; y_stand = 0.0 ; goodness = 0 ;}
                  throw new Exception("Outside the projection") ;
            default : 
               break ;
         }
      }
         //      if (goodness == 1)
         //      {
         switch(proj)
         { 

          //  case TAN: // TAN proj
          //     double den  = sin_del * sdelz + cos_del * cdelz *cos_dalpha;
          //     x_stand =  (x_tet_phi / den) * rad_to_deg ;
          //     y_stand =  (y_tet_phi / den) * rad_to_deg;
          //     break;
             case SIN : // SIN proj
             case SINSIP:    
               //                        x_stand   = 180./Math.PI*x_tet_phi ;
               //                        y_stand   = 180./Math.PI*y_tet_phi ;
               x_stand   = rad_to_deg*x_tet_phi ;
               y_stand   = rad_to_deg*y_tet_phi ;
                 //      System.out.println("x_tet_phi"+x_tet_phi+" "+y_tet_phi);
               //           if((al - alphai)>+180.) x_stand = -x_stand ;
               //           if((al - alphai)<-180.) x_stand = -x_stand ;
             //  if (xyapoly[1] != 0 ) System.out.println("xyapoly 1 "+ xyapoly[1]);
            //   if (xyapoly[2] != 0 ) System.out.println("xyapoly 2 "+ xyapoly[2]);
             //  if (xydpoly[1] != 0 ) System.out.println("xydpoly 1 "+ xydpoly[1]);
            //   if (xydpoly[2] != 0 ) System.out.println("xydpoly 2 "+ xydpoly[2]);
             //  if (adxpoly[1] != 0 ) System.out.println("adxpoly 1"+ adxpoly[1]);
            //   if (adypoly[1] != 0 ) System.out.println("adypoly 1"+ adypoly[1]);
           //    if (adxpoly[2] != 0 ) System.out.println("adxpoly 2"+ adxpoly[2]);
            //   if (adypoly[2] != 0 ) System.out.println("adypoly 2"+ adypoly[2]);
               if ((xydpoly[1] != 0 ) && (xydpoly[2] != 0 ))
               {
                   x_stand += rad_to_deg*xydpoly[1]*(1-(sin_del*sdelz + cos_del*cdelz*cos_dalpha)) ;
                   y_stand += rad_to_deg*xydpoly[2]*(1-(sin_del*sdelz + cos_del*cdelz*cos_dalpha)) ;
               }
               break ;
            case NCP : // NCP
               x_stand   = rad_to_deg*x_tet_phi ;
               if (sdelz == 0) y_stand = rad_to_deg*y_tet_phi ;
               else
                  if (sdelz*sin_del > 0)
                     y_stand   = rad_to_deg*y_tet_phi + (cdelz/sdelz)*rad_to_deg
                     *(1- Math.sqrt(1-cos_del*cos_del*sin_dalpha*sin_dalpha
                           -sin_del*sin_del*cdelz*cdelz-cos_del*cos_del*sdelz*sdelz*cos_dalpha*cos_dalpha +2*sin_del*cos_del*sdelz*cdelz*cos_dalpha));
                  else {x_stand = 0.0 ; y_stand = 0.0 ;}
               break ;
          case TAN:
          case SIP: 
          case TPV:   
               //                         double den     = FastMath.sin(del*Math.PI/180.)
               //                                  *FastMath.sin(deltai*Math.PI/180.) +
               //                                   FastMath.cos(del*Math.PI/180.)
               //                                  *FastMath.cos(deltai*Math.PI/ 180.) ;
               double den  = sin_del * sdelz + cos_del * cdelz *cos_dalpha;
               x_stand =  x_tet_phi / den ;
               y_stand =  y_tet_phi / den ;
               //             System.out.println("alphai "+alphai+" "+deltai);
               //             System.out.println("xystand"+x_stand+" "+y_stand);
               //                        x_stand *= 180./Math.PI ;
               //                        y_stand *= 180./Math.PI ;
               //            System.out.println("xystand"+x_stand+" "+y_stand);
               //                        System.out.println("proj 2\n");
               
               // PFOPT: ACCELERATION
               if( aladin>3 || xyapoly[1]==0 || xyapoly[1]==1 ) {
                  x_stand *= rad_to_deg;
                  y_stand *= rad_to_deg;
               }
               else if ((xyapoly[1] != 0)&&(xyapoly[1] != 1)&&(aladin == 0))
               {
                   x_stand *= rad_to_deg ;
                   y_stand *= rad_to_deg ;
                  //  double X = xyapoly[0]  * deg_to_rad ;
                  //  double Y = xydpoly[0] * deg_to_rad ;
                  double X = xyapoly[0];
                  double Y = xydpoly[0];
                  double dx ;
                  double dy ;
                  double xx=0 ;
                  double yy=0 ;
                  int niter = 20 ;
                  int iter = 0 ;
                  double m1,m2,m3,m4;
                  
                  while (iter < niter)
                  {
                       /* Changes for SCAMP */
                     iter++ ;
                     double r ;
                     
                     if ((xx == 0.0 )&(yy == 0.0)) r= 1;
                     else r = Math.sqrt(xx*xx+yy*yy) ;                     
                     m1 = xyapoly[1]+
                        xyapoly[3]*xx/r +
                           2*xyapoly[4]*xx +
                           xyapoly[5]*yy  +
                           3*xyapoly[7]*xx*xx +
                           xyapoly[9]*yy*yy +
                           2*xyapoly[8]*yy*xx +
                         3*xyapoly[11]*xx*Math.sqrt(xx*xx+yy*yy) ;
                     //  m1  *= deg_to_rad ;
                     //System.out.println("m1 "+iter+" "+ xyapoly[3]*xx/r+ " " +2*xyapoly[4]*xx )  ;
                     m2  = xydpoly[2]+
                         xydpoly[3]*xx/r +
                           2*xydpoly[6]*xx +
                           xydpoly[5]*yy  +
                          3*xydpoly[10]*xx*xx +
                           xydpoly[8]*yy*yy +
                           2*xydpoly[9]*yy*xx +
                           3*xydpoly[11]*xx* Math.sqrt(xx*xx+yy*yy) ;
                     // System.out.println("m2 "+iter+" "+ xydpoly[3]*xx/r + " " +2*xydpoly[6]*xx )  ;
                     // m2  *= deg_to_rad ;

                     m3  = xyapoly[2] +
                      xyapoly[3]*yy/r +
                           2*xyapoly[6]*yy +
                           xyapoly[5]*xx +
                           3*xyapoly[10]*yy*yy +
                           2*xyapoly[9]*yy*xx +
                           xyapoly[8]*xx*xx +
                           3*xyapoly[11]*yy*Math.sqrt(xx*xx+yy*yy) ;
                     // m3  *= deg_to_rad ;
                     // System.out.println("m3 "+iter+" "+ xyapoly[3]*yy/r+ " " +2*xyapoly[6]*yy )  ;
                     m4  = xydpoly[1] +
                     xydpoly[3]*yy/r +
                           2*xydpoly[4]*yy +
                           xydpoly[5]*xx  +
                           3*xydpoly[7]*yy*yy +
                           2*xydpoly[8]*yy*xx +
                           xydpoly[9]*xx*xx +
                           3*xydpoly[11]*yy* Math.sqrt(xx*xx+yy*yy) ;
                     // System.out.println("m4 "+iter+" "+ xydpoly[3]*yy/r+ " " +2*xyapoly[4]*yy )  ;      
                     //  m4  *= deg_to_rad ;
                     double det = m1 * m4 - m2 * m3 ;
                     double tmp = m4 / det ;
                     m2 /= -det ;
                     m3 /= -det ;
                     m4 = m1 /det ;
                     m1 = tmp ;

                     //                               System.out.println("matrice "+m1+" "+m2+" "+m3+" "+m4) ;
                     dx = m1 * (x_stand - X) + m3 * (y_stand - Y) ;
                     dy = m2 * (x_stand - X) + m4 * (y_stand - Y) ;

                     xx += dx ;
                     yy += dy ;
                     r = Math.sqrt(xx*xx+yy*yy) ;

                     //                  System.out.println("iterations dXY"+iter+" "+(x_stand - X)+" "+(y_stand-Y));
                     //                  System.out.println("iterations XY"+iter+" "+X+" "+Y);
                     X =  xyapoly[0] +
                           xyapoly[2]*yy +
                           xyapoly[1]*xx +
                           xyapoly[3]*Math.sqrt(xx*xx+yy*yy) +
                           xyapoly[6]*yy*yy +
                           xyapoly[4]*xx*xx +
                           xyapoly[5]*yy*xx  +
                            xyapoly[10]*yy*yy*yy +
                           xyapoly[7]*xx*xx*xx +
                           xyapoly[9]*yy*yy*xx +
                           xyapoly[8]*yy*xx*xx +
                           xyapoly[11]*r*r*r ;
                     //   X  *= deg_to_rad ;
                     Y  =  xydpoly[0] +
                           xydpoly[1]*yy +
                           xydpoly[2]*xx +
                           xydpoly[3]*Math.sqrt(xx*xx+yy*yy) +
                           xydpoly[4]*yy*yy +
                           xydpoly[6]*xx*xx +
                           xydpoly[5]*yy*xx  +
                           xydpoly[7]*yy*yy*yy +
                           xydpoly[10]*xx*xx*xx +
                           xydpoly[8]*yy*yy*xx +
                           xydpoly[9]*yy*xx*xx +
                           xydpoly[11]*r*r*r ;
                           
                     //   Y *= deg_to_rad ;
                     //                             System.out.println("iterations "+iter+" "+xx+" "+yy);
                  }
                  
                  x_stand = xx  ;
                  y_stand = yy ;
                  /*
                  x_stand = xx * rad_to_deg ;
                  y_stand = yy * rad_to_deg ;
                  */
                  
               }
               else if ((xyapoly[1] != 0)&&(xyapoly[1] != 1)&&(aladin == 3))
               {
                   
                  //  double X = xyapoly[0]  * deg_to_rad ;
                  //  double Y = xydpoly[0] * deg_to_rad ;
                  double X = xyapoly[0];
                  double Y = xydpoly[0];
                  double dx ;
                  double dy ;
                  double xx=0 ;
                  double yy=0 ;
                  int niter = 20 ;
                  int iter = 0 ;
                  double m1,m2,m3,m4;

                  while (iter < niter)
                  {
                      
                     iter++ ;
                     m1 = xyapoly[1]+
                    
                           2*xyapoly[3]*xx +
                           xyapoly[4]*yy  +
                           3*xyapoly[6]*xx*xx +
                           xyapoly[8]*yy*yy +
                           2*xyapoly[7]*yy*xx ;
                     //  m1  *= deg_to_rad ;

                     m2  = xydpoly[1]+
                    // xydpoly[3]*yy/Math.sqrt(xx*xx+yy*yy) +
                           2*xydpoly[3]*xx +
                           xydpoly[4]*yy  +
                          3*xydpoly[6]*xx*xx +
                           xydpoly[8]*yy*yy +
                           2*xydpoly[7]*yy*xx ;
                     // m2  *= deg_to_rad ;

                     m3  = xyapoly[2] +
                  //   xyapoly[3]*yy/Math.sqrt(xx*xx+yy*yy) +
                           2*xyapoly[5]*yy +
                           xyapoly[4]*xx +
                          3*xyapoly[9]*yy*yy +
                           2*xyapoly[8]*yy*xx +
                           xyapoly[7]*xx*xx ;
                     // m3  *= deg_to_rad ;

                     m4  = xydpoly[2] +
                //     xydpoly[3]*xx/Math.sqrt(xx*xx+yy*yy) +
                           2*xydpoly[5]*yy +
                           xydpoly[4]*xx  +
                           3*xydpoly[9]*yy*yy +
                           2*xydpoly[8]*yy*xx +
                           xydpoly[7]*xx*xx ;
                     //  m4  *= deg_to_rad ;
                     double det = m1 * m4 - m2 * m3 ;
                     double tmp = m4 / det ;
                     m2 /= -det ;
                     m3 /= -det ;
                     m4 = m1 /det ;
                     m1 = tmp ;

                     //                               System.out.println("matrice "+m1+" "+m2+" "+m3+" "+m4) ;
                     dx = m1 * (x_stand - X) + m3 * (y_stand - Y) ;
                     dy = m2 * (x_stand - X) + m4 * (y_stand - Y) ;

                     xx += dx ;
                     yy += dy ;

                     //                  System.out.println("iterations dXY"+iter+" "+(x_stand - X)+" "+(y_stand-Y));
                     //                  System.out.println("iterations XY"+iter+" "+X+" "+Y);
                     X =  xyapoly[0] +
                           xyapoly[2]*yy +
                           xyapoly[1]*xx +
                         //  xyapoly[3]*Math.sqrt(xx*xx+yy*yy) +
                           xyapoly[5]*yy*yy +
                           xyapoly[3]*xx*xx +
                           xyapoly[4]*yy*xx  +
                           xyapoly[9]*yy*yy*yy +
                           xyapoly[6]*xx*xx*xx +
                           xyapoly[8]*yy*yy*xx +
                           xyapoly[7]*yy*xx*xx ;
                     //   X  *= deg_to_rad ;
                     Y  =  xydpoly[0] +
                           xydpoly[2]*yy +
                           xydpoly[1]*xx +
                          // xydpoly[3]*Math.sqrt(xx*xx+yy*yy) +
                           xydpoly[5]*yy*yy +
                           xydpoly[3]*xx*xx +
                           xydpoly[4]*yy*xx  +
                           xydpoly[9]*yy*yy*yy +
                           xydpoly[6]*xx*xx*xx +
                           xydpoly[8]*yy*yy*xx +
                           xydpoly[7]*yy*xx*xx ;
                     //   Y *= deg_to_rad ;
                     //                             System.out.println("iterations "+iter+" "+xx+" "+yy);
                  }
                  
                  x_stand = xx  ;
                  y_stand = yy ;
                  /*
                  x_stand = xx * rad_to_deg ;
                  y_stand = yy * rad_to_deg ;
                  */
                  
               }
               else if ((xyapoly[1] != 0)&&(xyapoly[1] != 1)&&(aladin == 2))
               {
                  double X = xyapoly[0];
                  double Y = xydpoly[0];
                  double dx ;
                  double dy ;
                  double xx=0 ;
                  double yy=0 ;

                  int niter = 5 ;
                  int iter = 0 ;
                  double m1,m2,m3,m4;
                  //        System.out.println("XY "+X+" "+Y) ;
                 // System.out.println("XY "+X*rad_to_deg+" "+Y*rad_to_deg) ;
                  while (iter < niter)
                  {
                     iter++ ;
                     m1 = xyapoly[2]+
                           2*xyapoly[4]*xx +
                           xyapoly[5]*yy +
                           3*xyapoly[7]*xx*xx +
                           xyapoly[8]*yy*yy +
                           2*xyapoly[9]*yy*xx ;
                     //  m1  *= deg_to_rad ;

                     m2  = xydpoly[2]+
                           2*xydpoly[4]*xx +
                           xydpoly[5]*yy +
                           3*xydpoly[7]*xx*xx +
                           xydpoly[8]*yy*yy +
                           2*xydpoly[9]*yy*xx ;
                     // m2  *= deg_to_rad ;

                     m3  = xyapoly[1] +
                           2*xyapoly[3]*yy +
                           xyapoly[5]*xx +
                           3*xyapoly[6]*yy*yy +
                           2*xyapoly[8]*yy*xx +
                           xyapoly[9]*xx*xx ;
                     // m3  *= deg_to_rad ;

                     m4  = xydpoly[1] +
                           2*xydpoly[3]*yy +
                           xydpoly[5]*xx +
                           3*xydpoly[6]*yy*yy +
                           2*xydpoly[8]*yy*xx +
                           xydpoly[9]*xx*xx ;
                     //  m4  *= deg_to_rad ;
                     double det = m1 * m4 - m2 * m3 ;
                     double tmp = m4 / det ;
                     m2 /= -det ;
                     m3 /= -det ;
                     m4 = m1 /det ;
                     m1 = tmp ;

                     //          
                     //       System.out.println("matrice "+m1+" "+m2+" "+m3+" "+m4) ;
                     dx = m1 * (x_stand - X) + m3 * (y_stand - Y) ;
                     dy = m2 * (x_stand - X) + m4 * (y_stand - Y) ;
                     //        System.out.println("dx dy dxstand dystand "+dx+" "+dy+" "+(x_stand-X)+" "+(y_stand-Y)); 
                     xx += dx ;
                     yy += dy ;
                     X =  xyapoly[0] +
                           xyapoly[1]*yy +
                           xyapoly[2]*xx +
                           xyapoly[3]*yy*yy +
                           xyapoly[4]*xx*xx +
                           xyapoly[5]*yy*xx +
                           xyapoly[6]*yy*yy*yy +
                           xyapoly[7]*xx*xx*xx +
                           xyapoly[8]*yy*yy*xx +
                           xyapoly[9]*yy*xx*xx ;
                     //   X  *= deg_to_rad ;
                     Y  =  xydpoly[0] +
                           xydpoly[1]*yy +
                           xydpoly[2]*xx +
                           xydpoly[3]*yy*yy +
                           xydpoly[4]*xx*xx +
                           xydpoly[5]*yy*xx +
                           xydpoly[6]*yy*yy*yy +
                           xydpoly[7]*xx*xx*xx +
                           xydpoly[8]*yy*yy*xx +
                           xydpoly[9]*yy*xx*xx ;
                     //   Y *= deg_to_rad ;

                     // System.out.println("iterations XY"+iter+" "+X*rad_to_deg+" "+Y*rad_to_deg);
                     // System.out.println("iterations "+iter+" "+xx+" "+yy);
                     // System.out.println("iterations XY"+iter+" "+X+" "+Y);
                     // System.out.println("iterations "+iter+" "+xx*rad_to_deg+" "+yy*rad_to_deg);
                  }
                  //      System.out.println("iterations "+iter+" "+xx+" "+yy);
                  //      System.out.println("inC toto"+incA+" "+incD);
                  x_stand = xx ;
                  y_stand = yy ;

               }
               else {
                  x_stand *= rad_to_deg;
                  y_stand *= rad_to_deg;
               }
               //System.out.println("xystand"+x_stand+" "+y_stand);
               break ;
            case ZPN:
            case ARC:
               // System.out.println("al del"+al+" "+del);
               if((sin_del*cdelz- cos_del*sdelz *cos_dalpha)!=0)
                  phi = Math.atan(-cos_del *sin_dalpha
                        / (sin_del*cdelz- cos_del*sdelz *cos_dalpha));
               else if(-cos_del *sin_dalpha < 0 )phi = Math.PI/2 ;
               else phi = - Math.PI/2 ;
               //    System.out.println("num"+(cos_del *sin_dalpha));
               //   System.out.println("crit"+(sin_del*cdelz- cos_del*sdelz *cos_dalpha));
               //   System.out.println("phi dans ARC"+phi);
               if (sin_del*cdelz - cos_del*sdelz*cos_dalpha > 0)
                  phi =  Math.PI + phi ;
             //    System.out.println("phi1"+phi);
              //   System.out.println("cosphi"+FastMath.cos(phi));
               tet = Math.asin (
                     sin_del*sdelz+ cos_del*cdelz *cos_dalpha);
               double rteta ;
               if (proj == ZPN)
               { rteta = 0.0 ;
               // System.out.println("rteta "+rteta);
               //                           rteta = adxpoly[1]*(Math.PI/2 -tet) +adxpoly[3]*(Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet) +adxpoly[5]*(Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet)*(Math.PI/2 -tet);
               for (int order = 9;  order >= 0 ; order--)
               { rteta = (rteta )*(Math.PI/2-tet)+adxpoly[order];}}
               else rteta = (Math.PI/2 -tet) ;
            //   System.out.println("rteta "+rteta);
            //   if (rteta > Math.PI)
                
             //    System.out.println("tet rteta "+(Math.PI/2 -tet)+" "+rteta);
              // if (rteta < 0) System.out.println("tet rteta "+(Math.PI/2 -tet)+" "+rteta);
               
               x_stand = rteta*FastMath.sin(phi) ;
               //                        y_stand = -(Math.PI/2 -tet)*FastMath.cos(phi) ;
               y_stand = -rteta*FastMath.cos(phi) ;
               x_stand *= rad_to_deg;
               y_stand *= rad_to_deg;
               
   //  if ((Math.abs(x_stand) > 5000) || (Math.abs(y_stand) > 5000))     System.out.println("xy "+c.al+" "+c.del+" "+x_stand+" "+y_stand+"\n\n");
               //                        System.out.println("proj 2\n");

               break ;

            case AIT :  // AIT proj
               // dans le cas des projections pseudo-cylindriques comme AITOFF
               // deltai, alphai n'est pas le pole des coordonÃ¯Â¿Â½es locales !!!
               // (meme chose dans GetCoord ....)
               double cdelp = FastMath.cos(deltai*deg_to_rad+Math.PI/2);
               double sdelp = FastMath.sin(deltai*deg_to_rad+Math.PI/2) ;

               phi = Math.atan2(cos_del *sin_dalpha,-(sin_del*cdelp - cos_del*sdelp *cos_dalpha));

               tet =  Math.asin(sin_del*sdelp + cos_del*cdelp *cos_dalpha);
               //                  System.out.println("phi tet"+phi+" "+tet);
               if (phi > Math.PI )   phi = -2*Math.PI +phi ;
               //                  if (phi < -Math.PI )  phi = + 2*Math.PI +phi ;

               double alph = Math.sqrt(2/(1+FastMath.cos(tet)*FastMath.cos(phi/2.)));
               x_stand = 2*alph*FastMath.cos(tet)*FastMath.sin(phi/2);
               y_stand = alph*FastMath.sin(tet) ;
               //                     System.out.println("xy "+x_stand+" "+y_stand+"\n");
               //                     x_stand *= 180./Math.PI ;
               //                     y_stand *= 180./Math.PI ;
               x_stand *= rad_to_deg ;
               y_stand *= rad_to_deg ;

               //                        if(phi/2 > Math.PI) x_stand = -x_stand ;
               //                        System.out.println("xy deg"+x_stand+" "+y_stand+"\n");
               break ;
            case GLS : // GLS proj
                // dans le cas des projections pseudo-cylindriques comme AITOFF
                // deltai, alphai n'est pas le pole des coordonÃ¯Â¿Â½es locales !!!
                // (meme chose dans GetCoord ....)
                 cdelp = FastMath.cos(deltai*deg_to_rad+Math.PI/2);
                sdelp = FastMath.sin(deltai*deg_to_rad+Math.PI/2) ;

                phi = Math.atan2(cos_del *sin_dalpha,-(sin_del*cdelp - cos_del*sdelp *cos_dalpha));

                tet =  Math.asin(sin_del*sdelp + cos_del*cdelp *cos_dalpha);
                //                  System.out.println("phi tet"+phi+" "+tet);
                if (phi > Math.PI )   phi = -2*Math.PI +phi ;
                //                  if (phi < -Math.PI )  phi = + 2*Math.PI +phi ;

                x_stand = phi*FastMath.cos(tet);
                y_stand = tet ;
                //                     System.out.println("xy "+x_stand+" "+y_stand+"\n");
                //                     x_stand *= 180./Math.PI ;
                //                     y_stand *= 180./Math.PI ;
                x_stand *= rad_to_deg ;
                y_stand *= rad_to_deg ;

                //                        if(phi/2 > Math.PI) x_stand = -x_stand ;
                //                        System.out.println("xy deg"+x_stand+" "+y_stand+"\n");
                break ;

            case MOL:
               cdelp = FastMath.cos(deltai*deg_to_rad+Math.PI/2);
               sdelp = FastMath.sin(deltai*deg_to_rad+Math.PI/2) ;

               phi = Math.atan2(cos_del *sin_dalpha, -(sin_del*cdelp - cos_del*sdelp *cos_dalpha));
               tet =  Math.asin(sin_del*sdelp + cos_del*cdelp *cos_dalpha);

               double psi = 2 * Math.asin(2 * tet / Math.PI);
               double previous = 0;
               for( int i=0; i<200; i++ ) {
                  previous = psi;
                  psi -= (psi + FastMath.sin(psi) - Math.PI * FastMath.sin(tet))
                      / (1 + FastMath.cos(psi));
                  if (Double.isNaN(psi)) {psi=previous; break; } // Pierre Jan 2015
                  if( Math.abs(psi - previous) > 0.0001 ) break;
               }
               double theta = psi / 2;
               x_stand = (2*Math.sqrt(2)/Math.PI)*phi*FastMath.cos(theta)*rad_to_deg;
               y_stand = Math.sqrt(2)*FastMath.sin(theta)*rad_to_deg ;
               break;

//               double alpha1 =   (al- alphai)*deg_to_rad;
//               double delta1 =  (del/*- deltai*/)*deg_to_rad;
//
//               // Adjust alpha1 to the range +/- PI
//               while(alpha1 <= 0) alpha1 += 2*Math.PI;
//               while(alpha1 > Math.PI)  alpha1 -= 2*Math.PI;
//
//               // Don't plot quite up to poles to avoid strange effects
//               //                     if(Math.abs(delta1) > Math.toRadians(89.99)) return;
//
//               double psi = 2 * Math.asin(2 * delta1 / Math.PI);
//               double previous = 0;
//               for( int i=0; i<200; i++ ) {
//                  previous = psi;
//                  psi -= (psi + FastMath.sin(psi) - Math.PI * FastMath.sin(delta1)) / (1 + FastMath.cos(psi));
//                  if( Math.abs(psi - previous) > 0.0001 ) break;
//               }
//               double theta = psi / 2;
//               x_stand = (2*Math.sqrt(2)/Math.PI) * alpha1 * FastMath.cos(theta)*rad_to_deg;
//               y_stand = Math.sqrt(2) * FastMath.sin(theta)*rad_to_deg;
//               break;
               //               double alpha1 =   (al- alphai)*deg_to_rad;
               //               double delta1 =  (del/*- deltai*/)*deg_to_rad;
               //
               //               // Adjust alpha1 to the range +/- PI
               //               while(alpha1 <= 0) alpha1 += 2*Math.PI;
               //               while(alpha1 > Math.PI)  alpha1 -= 2*Math.PI;
               //
               //               // Don't plot quite up to poles to avoid strange effects
               //               //                     if(Math.abs(delta1) > Math.toRadians(89.99)) return;
               //
               //               double psi = 2 * Math.asin(2 * delta1 / Math.PI);
               //               double previous = 0;
               //               for( int i=0; i<200; i++ ) {
               //                  previous = psi;
               //                  psi -= (psi + FastMath.sin(psi) - Math.PI * FastMath.sin(delta1)) / (1 + FastMath.cos(psi));
               //                  if( Math.abs(psi - previous) > 0.0001 ) break;
               //               }
               //               double theta = psi / 2;
               //               x_stand = (2*Math.sqrt(2)/Math.PI) * alpha1 * FastMath.cos(theta)*rad_to_deg;
               //               y_stand = Math.sqrt(2) * FastMath.sin(theta)*rad_to_deg;
               //               break;

            case ZEA: // ZEA projection

               //                  System.out.println(" phi:"+180*phi/Math.PI+"\n") ;

               //                   if (FastMath.sin(del*Math.PI/180)*FastMath.cos(deltai*Math.PI/180)
               //                   - FastMath.cos(del*Math.PI/180)*FastMath.sin(deltai*Math.PI/180)
               //                   *FastMath.cos((al -alphai)*Math.PI/180) >0)
               //                        System.out.println("al del"+al+" "+del);
               if((sin_del*cdelz- cos_del*sdelz *cos_dalpha)!=0)
                  phi = Math.atan(-cos_del *sin_dalpha
                        / (sin_del*cdelz- cos_del*sdelz *cos_dalpha));
               else if(-cos_del *sin_dalpha < 0 )phi = Math.PI/2 ;
               else phi = - Math.PI/2 ;
               //                      phi = Math.atan2(-cos_del *sin_dalpha,
               //                      (sin_del*cdelz- cos_del*sdelz *cos_dalpha));
               if ((sin_del*cdelz - cos_del*sdelz*cos_dalpha > 0)
                     && (Math.abs(FastMath.sin(phi)) != 1.0))
                  phi =  Math.PI + phi ;
               tet = Math.asin (
                     //                   FastMath.sin(del*Math.PI/180)*FastMath.sin(deltai*Math.PI/180)+
                     //                   FastMath.cos(del*Math.PI/180)*FastMath.cos(deltai*Math.PI/180)
                     //                   *FastMath.cos((al -alphai)*Math.PI/180));
                     sin_del*sdelz+ cos_del*cdelz *cos_dalpha);
               //                    System.out.println("tet phi XY"+tet+" "+phi);
               //                   double rtet = (180/Math.PI)*Math.sqrt(2*(1-FastMath.sin(tet)));
               double rtet = rad_to_deg*Math.sqrt(2*(1-FastMath.sin(tet)));

               x_stand = rtet*FastMath.sin(phi) ;
               y_stand = - rtet*FastMath.cos(phi) ;


               break ;
            case STG: // STEREOGRAPHIC
               //                        den     = 1 + FastMath.sin(del*Math.PI/180.)
               //                                  *FastMath.sin(deltai*Math.PI/180.) +
               //                                   FastMath.cos(del*Math.PI/180.)
               //                                  *FastMath.cos(deltai*Math.PI/ 180.)
               //                                  *FastMath.cos((al - alphai)*Math.PI/180.) ;
               den     = 1 + sin_del*sdelz + cos_del*cdelz*cos_dalpha;
               x_stand =  2*x_tet_phi / den ;
               y_stand =  2*y_tet_phi / den ;
               //                        x_stand *= 180./Math.PI ;
               //                        y_stand *= 180./Math.PI ;
               x_stand *= rad_to_deg ;
               y_stand *= rad_to_deg ;
               break ;
            case FIE:
                phi = Math.atan2(-cos_del *sin_dalpha, (sin_del*cdelz - cos_del*sdelz *cos_dalpha));
                tet =  Math.asin(sin_del*sdelz + cos_del*cdelz *cos_dalpha);
                x_stand = tet*Math.cos(phi) ;
                y_stand = tet*Math.sin(phi) ;
                x_stand *= rad_to_deg ;
                y_stand *= rad_to_deg ;
                break ;
                
            case CAR: // CARTESIEN
               //                        x_stand = -(al-alphai)*cos_del ;
               //           System.out.println("proj 7\n");

               x_stand = al-alphai ;
               y_stand = del-deltai ;
               double xshift =0. ;
               //                        System.out.println("center x_stand"+x_stand+" "+y_stand);
               //                        System.out.println("center shift"+((CD[0][0]*Xcen +CD[0][1]*Ycen)+x_stand)) ;   
               // Avec ces tests il s'agit de verifier que le x-stand va se retrouver
               //   entre les limites de l'image. On teste modulo 360 et modulo -360
               //                      System.out.println("Xcen "+CD[0][0]*Xcen + CD[0][1]*Ycen+"   "+
               //                             CD[0][0]*(Xcen-xnpix) +CD[0][1]*Ycen+" "+x_stand) ;
               
               
               if (((x_stand+ 360.) > Math.min(CD[0][0]*(-Xcen) + CD[0][1]*Ycen,
                     CD[0][0]*(xnpix-Xcen) +CD[0][1]*Ycen))
                     &&
                     ((x_stand +360.) < Math.max(CD[0][0]*(-Xcen) +CD[0][1]*Ycen,
                           CD[0][0]*(xnpix-Xcen) +CD[0][1]*Ycen)))
                  xshift = 360.;
               if(((x_stand- 360.) > Math.min(CD[0][0]*(-Xcen) +CD[0][1]*Ycen,CD[0][0]*(xnpix-Xcen) +CD[0][1]*Ycen))&&
                     ((x_stand -360.) < Math.max(CD[0][0]*(-Xcen) +CD[0][1]*Ycen,CD[0][0]*(xnpix-Xcen) +CD[0][1]*Ycen)) )
                  xshift = -360.; 
               x_stand += xshift ;
               
               
               //                  System.out.println("Xshift "+ xshift) ;
               //                        if (x_stand > 180.) x_stand -= 360. ;
               //                        if (x_stand < -180.)x_stand += 360. ;
               //                       System.out.println("center "+al+" "+del) ;   
               //                       System.out.println("center "+alphai+" "+deltai) ;
               //                       System.out.println("center x_stand"+x_stand+" "+y_stand);                               

               //                      System.out.println("center CD"+CD[0][0]+" "+CD[0][1] ) ;
               break ;
            case SOL: // SOLAR
               x_stand = al-alphai ;
               y_stand = del-deltai ;
               //  System.out.println("xystand"+x_stand+" "+y_stand);
               break ;
            default:
               //                       System.out.println("proj default\n");
               break ;
         }
         //      }





      // System.out.println("ID "+x_stand+" "+ID[0][0]+" "+y_stand+" "+ID[0][1]+" "+Xcen+" "+xnpix+" "+CD[0][0]+" "+CD[0][1]+" "+cos_del+" "+sin_del);
      // System.out.println("ID "+x_stand+" "+ID[1][0]+" "+y_stand+" "+ID[1][1]+" "+Ycen+" "+ynpix+" "+CD[1][0]+" "+CD[1][1]+" "+cdelz+" "+ sdelz+" "+cos_dalpha);

      if (aladin != 2)
      {
                       
         c.x = (ID[0][0]*x_stand +ID[0][1]*y_stand)+ Xcen;
         c.y =  -(ID[1][0]*x_stand +ID[1][1]* y_stand) + ynpix - Ycen;
       //  if ((Math.abs(c.x) > 20000) || (Math.abs(c.y )> 10000))   System.out.println("on est là c.y c.x x_stand y_stand"+c.y+" "+c.x+" "+x_stand+" "+y_stand+" "+c.al+" "+c.del);
         if ((xyapoly[1] != 0)&&(xyapoly[1] != 1)&&((proj==TAN)||(proj==SIP)) && (aladin == 3)  && (xydpoly[2]*ID[1][1] <0 )) 
         {    
                 //           System.out.println("on est ici") ;
           c.y = (ID[1][0]*x_stand +ID[1][1]* y_stand) +  Ycen  ;
       //    if (aladin == 3) c.y = (ID[1][0]*x_stand +ID[1][1]* y_stand) + Ycen ; 
           }
                    // System.out.println("c.y "+c.y);
  //       if ((proj == SIP)||(proj == SINSIP))
            if (proj == SIP )
         {
            if ((order_ap == 0)||(order_bp == 0))
            {
               double X = 0;
               double Y = 0;
               double dx ;
               double dy ;
               double xx=0 ;
               double yy=0 ;
               int niter = 20 ;
               int iter = 0 ;
               double m1,m2,m3,m4;

               while (iter < niter)
               {
                  iter++ ;
                  m1 = 1 ;
                  for (int order = 0;  order <= order_a ; order++)
                  {
                     for (int powx =0 ; powx <= order ; powx++ )
                     {
                           m1 = m1 + powx*sip_a[powx][order-powx]*Math.pow(xx,powx-1)*Math.pow(yy,order-powx);
                     }
                  }
                  m2 = 0 ;
                  for (int order = 0;  order <= order_a ; order++)
                  {
                     for (int powx =0 ; powx <= order ; powx++ )
                     {
                           m2 = m2 + (order-powx)*sip_a[powx][order-powx]*Math.pow(xx,(powx))*Math.pow(yy,order-powx-1);
                     }
                  } 
                  m3 = 1 ;
                  for (int order = 0;  order <= order_b ; order++)
                  {
                     for (int powx =0 ; powx <= order ; powx++ )
                     {
                           m3 = m3 + powx*sip_b[powx][order-powx]*Math.pow(xx,powx-1)*Math.pow(yy,order-powx);
                     }
                  }
                  m4 = 0 ;
                  for (int order = 0;  order <= order_b ; order++)
                  {
                     for (int powx =0 ; powx <= order ; powx++ )
                     {
                           m4 = m4 + (order-powx)*sip_b[powx][order-powx]*Math.pow(xx,(powx))*Math.pow(yy,order-powx-1);
                     }
                  }   
                  double det = m1 * m4 - m2 * m3 ;
                  double tmp = m4 / det ;
                  m2 /= -det ;
                  m3 /= -det ;
                  m4 = m1 /det ;
                  m1 = tmp ;
                  double DX ;
                  double DY ;
                  DX = (ID[0][0]*(x_stand -X) +ID[0][1]*(y_stand-Y));
                  DY =  (ID[1][0]*(x_stand-X) +ID[1][1]* (y_stand-Y)) ;
                  //                               System.out.println("matrice "+m1+" "+m2+" "+m3+" "+m4) ;
                  dx = m1 * DX + m3 * DY ;
                  dy = m2 * DX + m4 * DY ;

                  xx += dx ;
                  yy += dy ;
                  double xint = xx ;   
                  double yint = yy ;
                  double    px = xint ;
                  double    py = yint ; 
                  for (int order = 0;  order <= order_a ; order++)
                  {
                     for (int powx =0 ; powx <= order ; powx++ )
                     {
                           px = px + sip_a[powx][order-powx]*Math.pow(xint,(powx))*Math.pow(yint,order-powx);
                     }
                  }

                  for (int order = 0;  order <= order_b ; order++)
                  {
                     for (int powx =0 ; powx <= order ; powx++ )
                     {
                           py = py + sip_b[powx][order-powx]*Math.pow(xint,(powx))*Math.pow(yint,order-powx);
                     }
                  }
                  X = CD[0][0]*px +CD[0][1]*py ;
                  Y = CD[1][0]*px +CD[1][1]*py ;                         
               }  
               c.x = xx + Xcen ;
               c.y = yy + ynpix -Ycen ;
            }
            else
            {               
               double xint= c.x -Xcen ;
               double yint= -(c.y -ynpix + Ycen) ;
               c.x = xint ;
 //              for (int order = 2;  order < order_ap+1 ; order++)
              for (int order = 0;  order <= order_ap ; order++)
               {
                  for (int powx =0 ; powx <= order ; powx++ )

                  {
 //                    for (int j = 0 ; j < order-powx + 1 ; j++)
                     // {
                     int  j = order - powx ;
                        //                           System.out.println("powx j "+powx+" "+j + " "+ sip_ap[powx][j]) ;

                           c.x = c.x + sip_ap[powx][j]*Math.pow(xint,(powx))*Math.pow(yint,(j));
                  }
               }

               c.x =c.x +Xcen ;

               //       ap[0][2]*Math.pow(yint,2.0) +
               //       ap[0][3]*yint*yint*yint +
               //       ap[1][1]*xint*yint +
               //       ap[1][2]*xint*yint*yint +
               //       ap[2][0]*Math.pow(xint,2.0) +
               //       ap[2][1]*xint*xint*yint +
               //       bp[3][0]*xint*xint*xint + Xcen ;

               c.y = yint ;
  //             for (int order = 2;  order < order_bp+1 ; order++)
               for (int order = 0; order <= order_bp ; order++)
               {
                  for (int powx =0 ; powx <= order ; powx++ )
                  {
    //                 for (int j = 0 ; j < order-powx + 1 ; j++)
      //               {
                        //                           System.out.println("powx j "+powx+" "+j+" "+sip_bp[powx][j])  ;

                          int j = order -powx ;

                           c.y = c.y + sip_bp[powx][j]*Math.pow(xint,(powx))*Math.pow(yint,(j));
                        //                           System.out.println("c.y "+c.y);
                     //}

                     // c.y = yint + 
                     // bp[0][2]*Math.pow(yint,(double)(nnnn)) +
                     // bp[0][3]*yint *yint*yint +
                     // bp[1][1]*xint*yint +
                     // bp[1][2]*xint*yint*yint +
                     // bp[2][0]*Math.pow(xint,2.0) +
                     // bp[2][1]*xint*xint*yint +
                     // bp[3][0]*xint*xint*xint + ynpix -Ycen ;
                  }    
               }
               c.y = - c.y +ynpix -Ycen ;

            }
         }
      }
      else
      {
         //*

         //   System.out.println("x y xz yz"+(x_stand)*1000.0/incX+" "+(y_stand)*1000.0/incY+" "+(xz*1000.0/incX)+ " "+(yz*1000.0/incY));  
         c.x= ((x_stand*focale)*1000.0  +xz*1000.0 - Xorg)/incX ;
         c.y= ((y_stand*focale)*1000.0 + yz*1000.0 - Yorg)/incY  ; 
         // System.out.println("center cxy"+c.x+" "+c.y ) ;
         // System.out.println("Xorg Yorg"+Xorg+" "+Yorg+" "+incX+" "+incY) ;
         //  System.out.println("c.xy "+c.x+" "+c.y);
      }
      //  System.out.println("center cxy"+c.x+" "+c.y ) ;
      //  System.out.println("Xorg Yorg"+Xorg+" "+Yorg+" "+incX+" "+incY) ;
      //      System.out.println("c.xy "+c.x+" "+c.y);
      //   double xx = (ID[0][0]*x_stand +ID[0][1]*(-y_stand))+ Xcen;
      //   double yy =  -(ID[1][0]*x_stand +ID[1][1]* (-y_stand)) + Ycen;
      //  if ((xyapoly[1] != 0)&&(xyapoly[1] != 1)&&(proj==2) && (aladin == 0) && (xydpoly[2]*ID[1][1] <0 )) 
      //  {yy =  (ID[1][0]*x_stand +ID[1][1]* (-y_stand)) + ynpix - Ycen  /* PF -1 */;
      //             c.x = c.x /* PF +1 */; 
      //}
      //System.out.println("c.xy "+xx+" "+yy+" "+(-(ID[1][0]*x_stand +ID[1][1]* (-y_stand)))+" "+(-(ID[1][0]*x_stand +ID[1][1]* y_stand)));
      //            c.yf = - (ID[1][0]*x_stand +ID[1][1]* y_stand) +  Ycen;
   }
   }





   public  double [] GetResol() {
   double inc[] = new double[2];
   // Pierre - sept 2011 : depuis la dernière mouture de Calib, il peut y avoir des valeurs négatives
   //                    inc[0]= incA ;
   //                    inc[1]= incD ;
   inc[0]= Math.abs(incA) ;
   inc[1]= Math.abs(incD) ;
   return inc;
   }


   protected  void GetWCS_i()   throws Exception {

   Coord a_d   = new Coord() ;
   Coord x_y_1 = new Coord() ;
   Coord x_y_2 = new Coord() ;
   Coord x_y_3 = new Coord() ;
   Coord x_y_4 = new Coord() ;
   double alpha1,delta1 ;
   double alpha2,delta2 ;
   double alpha3,delta3 ;
   double alpha4,delta4 ;

   
 //  System.out.println("aladin "+aladin) ;
 //  System.out.println(:qpoly[0]+" "+ xydpoly[0]);
   if(aladin == 1)
      // calcul du header WCS si l'image vient d'aladin
   {

      Xcen = (xz*1000.0-Xorg)/incX ;
      Ycen = (yz*1000.0-Yorg)/incY ;
      alphai = alpha ;
      deltai = delta ;
      CD[0][0] = 1.0 ;
      CD[0][1] = 0.0 ; 
      CD[1][0] = 0.0 ; 
      CD[1][1] = 1.0 ; 
      ID[0][0] = 1.0 ;
      ID[0][1] = 0.0 ; 
      ID[1][0] = 0.0 ; 
      ID[1][1] = 1.0 ; 

      Xcen = xnpix/2. ;
      Ycen = ynpix/2. ;

      //PIERRE       a_d.xf = Xcen ;
      //PIERRE       a_d.yf = Ycen ;
      //PIERRE       a_d.x = (int)Math.rint(a_d.xf) ;
      //PIERRE       a_d.y = (int)Math.rint(a_d.yf) ;
      a_d.x = Xcen ;
      a_d.y = Ycen ;

      //              System.out.println("XYcen "+a_d.xf+" "+a_d.yf);
      GetCoord(a_d) ;
      alphai = a_d.al ;
      deltai = a_d.del ;
      //             System.out.println("XYcen "+Xcen+" "+Ycen);
      //            System.out.println("alp delt"+alphai+" "+deltai);

      //PIERRE       x_y_1.xf = Xcen  - xnpix/4. ;
      //PIERRE       x_y_1.yf = Ycen  - ynpix/4. ;
      //PIERRE       x_y_1.x = (int)Math.rint(x_y_1.xf) ;
      //              System.out.println("x_y_1 "+x_y_1.xf+" "+x_y_1.yf);
      //PIERRE       x_y_1.y = (int)Math.rint(x_y_1.yf) ;
      x_y_1.x = Xcen  - xnpix/4. ;
      x_y_1.y = Ycen  - ynpix/4. ;

      GetCoord(x_y_1);
      //             System.out.println("alp delt"+alphai+" "+deltai);
      //             System.out.println("alp delt"+x_y_1.al+" "+x_y_1.del);
      double cdelz1, sdelz1 ;
      cdelz1 = FastMath.cos((deltai/180.)*Math.PI);
      sdelz1 = FastMath.sin((deltai/180.)*Math.PI);

      //                    CD[0][0] = -(x_y_1.al*cdelz1+x_y_1.del)*2/xnpix;
      //                    CD[0][1] = -(x_y_1.al*cdelz1-x_y_1.del)*2/ynpix;
      double xst, yst,deno;
      deno = FastMath.sin(x_y_1.del*Math.PI/180.)*sdelz1
            +FastMath.cos(x_y_1.del*Math.PI/180.)*cdelz1
            *FastMath.cos((x_y_1.al-alphai)*Math.PI/180.) ;
      xst = FastMath.cos(x_y_1.del*Math.PI/180.)
            *FastMath.sin((x_y_1.al-alphai)*Math.PI/180.)
            / deno ;
      yst = FastMath.sin(x_y_1.del*Math.PI/180.)*cdelz1
            -FastMath.cos(x_y_1.del*Math.PI/180.)*sdelz1
            *FastMath.cos((x_y_1.al-alphai)*Math.PI/180.)
            / deno;
      CD[0][0] = -(ynpix*xst+xnpix*yst)*2/ynpix/xnpix;
      CD[0][1] = +(ynpix*xst-xnpix*yst)*2/xnpix/ynpix;

      //PIERRE       x_y_2.xf = Xcen  + xnpix/4.  ;
      //PIERRE       x_y_2.yf = Ycen  - ynpix/4.  ;
      //PIERRE       x_y_2.x = (int) Math.rint(x_y_2.xf) ;
      //PIERRE       x_y_2.y = (int)Math.rint(x_y_2.yf) ;
      x_y_2.x = Xcen  + xnpix/4.  ;
      x_y_2.y = Ycen  - ynpix/4.  ;

      //              System.out.println("x_y_2 "+x_y_2.xf+" "+x_y_2.yf);
      GetCoord(x_y_2);
      //             System.out.println("alp delt"+alphai+" "+deltai);
      //             System.out.println("alp delt"+x_y_2.al+" "+x_y_2.del);
      //                    CD[0][0] -= (x_y_2.al*cdelz1-x_y_2.del)*2/xnpix;
      //                    CD[0][1] += (x_y_2.al*cdelz1+x_y_2.del)*2/ynpix;
      deno = FastMath.sin(x_y_2.del*Math.PI/180.)*sdelz1
            +FastMath.cos(x_y_2.del*Math.PI/180.)*cdelz1
            *FastMath.cos((x_y_2.al-alphai)*Math.PI/180.) ;
      xst = FastMath.cos(x_y_2.del*Math.PI/180.)
            *FastMath.sin((x_y_2.al-alphai)*Math.PI/180.)
            / deno ;
      yst = FastMath.sin(x_y_2.del*Math.PI/180.)*cdelz1
            -FastMath.cos(x_y_2.del*Math.PI/180.)*sdelz1
            *FastMath.cos((x_y_2.al-alphai)*Math.PI/180.)
            / deno;
      //System.out.println("stl "+xst+" "+yst+" ");
      CD[0][0] += (ynpix*xst-xnpix*yst)*2/ynpix/xnpix;
      CD[0][1] += (ynpix*xst+xnpix*yst)*2/xnpix/ynpix;
      //System.out.println("CD "+CD[0][0]+" "+CD[0][1]+" ");

      x_y_3.x = Xcen  - xnpix/4.  ;
      x_y_3.y = Ycen  + ynpix/4.  ;

      //              System.out.println("x_y_3 "+x_y_3.xf+" "+x_y_3.yf);
      GetCoord(x_y_3);

      //            System.out.println("alp delt"+x_y_3.al+" "+x_y_3.del);
      deno = FastMath.sin(x_y_3.del*Math.PI/180.)*sdelz1
            +FastMath.cos(x_y_3.del*Math.PI/180.)*cdelz1
            *FastMath.cos((x_y_3.al-alphai)*Math.PI/180.) ;
      xst = FastMath.cos(x_y_3.del*Math.PI/180.)
            *FastMath.sin((x_y_3.al-alphai)*Math.PI/180.)
            / deno ;
      yst = FastMath.sin(x_y_3.del*Math.PI/180.)*cdelz1
            -FastMath.cos(x_y_3.del*Math.PI/180.)*sdelz1
            *FastMath.cos((x_y_3.al-alphai)*Math.PI/180.)
            / deno;

      //                   CD[0][0] += (x_y_3.al*cdelz1-x_y_3.del)*2/xnpix;
      //                   CD[0][1] -= (x_y_3.al*cdelz1+x_y_3.del)*2/ynpix;
      CD[0][0] -= (ynpix*xst-xnpix*yst)*2/ynpix/xnpix;
      CD[0][1] -= (xst*ynpix+yst*xnpix)*2/xnpix/ynpix;

      x_y_4.x = Xcen  + xnpix/4. ;
      x_y_4.y = Ycen  + ynpix/4. ;

      GetCoord(x_y_4);
      //              System.out.println("x_y_4 "+x_y_4.xf+" "+x_y_4.yf);
      //            System.out.println("alp delt"+x_y_4.al+" "+x_y_4.del);

      //                  CD[0][0] += (x_y_4.al*cdelz1+x_y_4.del)*2/xnpix;
      //                  CD[0][1] -= (x_y_4.al*cdelz1-x_y_4.del)*2/ynpix;
      deno = FastMath.sin(x_y_4.del*Math.PI/180.)*sdelz1
            +FastMath.cos(x_y_4.del*Math.PI/180.)*cdelz1
            *FastMath.cos((x_y_4.al-alphai)*Math.PI/180.) ;
      xst = FastMath.cos(x_y_4.del*Math.PI/180.)
            *FastMath.sin((x_y_4.al-alphai)*Math.PI/180.)
            / deno ;
      yst = FastMath.sin(x_y_4.del*Math.PI/180.)*cdelz1
            -FastMath.cos(x_y_4.del*Math.PI/180.)*sdelz1
            *FastMath.cos((x_y_4.al-alphai)*Math.PI/180.)
            / deno;
      CD[0][0] += (ynpix*xst+xnpix*yst)*2/ynpix/xnpix;
      CD[0][1] -= (xst*ynpix-yst*xnpix)*2/xnpix/ynpix;
      CD[0][0] *= 180./Math.PI/4. ;
      CD[0][1] *= 180./Math.PI/4. ;
      CD[1][0] = CD[0][1] ;
      CD[1][1] =  -CD[0][0] ;
      equinox = 2000.0 ;
      proj = TAN ;
      //System.out.println("CD "+CD[0][0]+" "+CD[0][1]+" ");

   }

   }


   protected  void GetWCS(Vector key, Vector value)   throws Exception {

   GetWCS_i() ;
   // ce qui suitvest fait dans les deux cas, simple recopie
   // dans le cas aladin == 0
   // PIERRE : POURQUOI AJOUTER NAXIS1, NAXIS2 et EQUINOX
   key.addElement( "NAXIS1  ");
   value.addElement(new Integer(xnpix).toString());
   key.addElement( "NAXIS2  ");
   value.addElement(new Integer(ynpix).toString());
   key.addElement("CRPIX1  ");
   value.addElement(new Double(Xcen).toString());
   key.addElement("CRPIX2  ");
   value.addElement(new Double(Ycen).toString());
   key.addElement("EQUINOX ");
   value.addElement(new Double(equinox).toString());
   boolean flagPermute = aladin!=1 && type1!=null
         && type1.startsWith("DEC");
   key.addElement("CRVAL1  ");
   value.addElement(new Double(flagPermute?deltai:alphai).toString());
   key.addElement("CRVAL2  ");
   value.addElement(new Double(flagPermute?alphai:deltai).toString());
   key.addElement("CTYPE1  ");
   if (aladin == 1) value.addElement("'RA---TAN'");
   // PIERRE : ATTENTION type1 PEUT ETRE NULL -- NORMALEMENT, PLUS FB
   else value.addElement(type1);
   key.addElement("CTYPE2  ");
   if (aladin == 1) value.addElement("'DEC--TAN'");
   // PIERRE : ATTENTION type2 PEUT ETRE NULL -- NORMALEMENT, PLUS FB
   if (aladin == 1) value.addElement("'DEC--TAN'");
   else value.addElement(type2);

   // Le mot clé RADECSYS n'est concerné que par les systèmé équatoriaux
   // Modif PF Jan 2011
   if( RADECSYS[system].length()>0 ) {
      key.addElement("RADECSYS");
      value.addElement(RADECSYS[system]);
   }
   //               key.addElement("RADECSYS");
   //               switch(system)
   //               
   //                   {
   //                    case 1 :
   //                          value.addElement("FK4") ;
   //                          break ;
   //                    case 2 :
   //                          value.addElement("GLON") ;
   //                          break ;
   //                    case 3 :
   //                          value.addElement("SLON") ;
   //                          break ;
   //                    case 4 :
   //                          value.addElement("ELON") ;
   //                          break ;
   //                    default:
   //                    case 5 :
   //                          value.addElement("FK5") ;
   //                          break ;
   //                    case 6 :
   //                          value.addElement("ICRS") ;
   //                          break ;
   //                    case 7 :
   //                         value.addElement("Solar");
   //                         break ;
   //                   }

   key.addElement("CD1_1   ");
   //               value.addElement(new Double(flagPermute?CD[1][0]:CD[0][0]).toString());
   value.addElement(new Double(CD[0][0]).toString());
   key.addElement("CD1_2   ");
   //               value.addElement(new Double(flagPermute?CD[1][1]:CD[0][1]).toString());
   value.addElement(new Double(CD[0][1]).toString());
   key.addElement("CD2_1   ");
   //               value.addElement(new Double(flagPermute?CD[0][0]:CD[1][0]).toString());
   value.addElement(new Double(CD[1][0]).toString());
   key.addElement("CD2_2   ");
   //               value.addElement(new Double(flagPermute?CD[0][1]:CD[1][1]).toString());
   value.addElement(new Double(CD[1][1]).toString());
   //               System.out.println("CD "+CD[0][0] +" "+CD[1][1]);
      //   System.out.println("xyad "+xyapoly[0]+" "+xydpoly[0]);
   if (xyapoly[0] != 0.0)
   {
      //   System.out.println("PV");
   key.addElement("PV1_0");
   value.addElement(new Double(xyapoly[0]).toString()) ;
   }
   if (xydpoly[0] != 0.0)
   {
   key.addElement("PV2_0");
   value.addElement(new Double(xydpoly[0]).toString()) ;
   }
   if (xyapoly[1] != 0.0)
   {
   key.addElement("PV1_1");
   value.addElement(new Double(xyapoly[1]).toString()) ;
   }
   if (xydpoly[1] != 0.0)
   {
   key.addElement("PV2_1");
   value.addElement(new Double(xydpoly[1]).toString()) ;
   }
   if (xyapoly[2] != 0.0)
   {
   key.addElement("PV1_2");
   value.addElement(new Double(xyapoly[2]).toString()) ;
   }
   if (xydpoly[2] != 0.0)
   {
   key.addElement("PV2_2");
   value.addElement(new Double(xydpoly[2]).toString()) ;
   }
   if (xyapoly[3] != 0.0)
   {
         //   System.out.println("PV bis");
   key.addElement("PV1_3");
   value.addElement(new Double(xyapoly[3]).toString()) ;
   }
   if (xydpoly[3] != 0.0)
   {
   key.addElement("PV2_3");
   value.addElement(new Double(xydpoly[3]).toString()) ;
   }
   if (xyapoly[4] != 0.0)
   {
  // System.out.println("PV ter");
   key.addElement("PV1_4");
   value.addElement(new Double(xyapoly[4]).toString()) ;
   }
   if (xydpoly[4] != 0.0)
   {
   key.addElement("PV2_4");
   value.addElement(new Double(xydpoly[4]).toString()) ;
   }
   if (xyapoly[5] != 0.0)
   {
   key.addElement("PV1_5");
   value.addElement(new Double(xyapoly[5]).toString()) ;
   }
   if (xydpoly[5] != 0.0)
   {
   key.addElement("PV2_5");
   value.addElement(new Double(xydpoly[5]).toString()) ;
   }
   if (xyapoly[6] != 0.0)
   {
   key.addElement("PV1_6");
   value.addElement(new Double(xyapoly[6]).toString()) ;
   }
   if (xydpoly[6] != 0.0)
   {
   key.addElement("PV2_6");
   value.addElement(new Double(xydpoly[6]).toString()) ;
   }
   if (xyapoly[7] != 0.0)
   {
   key.addElement("PV1_7");
   value.addElement(new Double(xyapoly[7]).toString()) ;
   }
   if (xydpoly[7] != 0.0)
   {
   key.addElement("PV2_7");
   value.addElement(new Double(xydpoly[7]).toString()) ;
   }
   if (xyapoly[8] != 0.0)
   {
      key.addElement("PV1_8");
   value.addElement(new Double(xyapoly[8]).toString()) ;
   }
   if (xydpoly[8] != 0.0)
   {
   key.addElement("PV2_8");
   value.addElement(new Double(xydpoly[8]).toString()) ;
   }
   if (xyapoly[9] != 0.0)
   {
   key.addElement("PV1_9");
   value.addElement(new Double(xyapoly[9]).toString()) ;
   }
   if (xydpoly[9] != 0.0)
   {
   key.addElement("PV2_9");
   value.addElement(new Double(xydpoly[9]).toString()) ;
 //  System.out.println("PV fin");
   }  
   if (xyapoly[10] != 0.0)
   {
  // key.addElement("PV1_10");
   value.addElement(new Double(xyapoly[10]).toString()) ;
   }
   if (xydpoly[10] != 0.0)
   {
 //  key.addElement("PV2_10");
   value.addElement(new Double(xydpoly[10]).toString()) ;
         //   System.out.println("PV fin");
   }  
   }
   protected  void GetWCSP(Vector key, Vector value)   throws Exception {

   //GetWCS_i() ;
   // ce qui suitvest fait dans les deux cas, simple recopie
   // dans le cas aladin == 0
   //PIERRE : POURQUOI AJOUTER NAXIS1, NAXIS2 et EQUINOX
   key.addElement( "NAXIS1  ");
   value.addElement(new Integer(xnpix).toString());
   key.addElement( "NAXIS2  ");
   value.addElement(new Integer(ynpix).toString());
   key.addElement("CRPIX1  ");
   if (aladin != 1)
      value.addElement(new Double(Xcen).toString());
   else value.addElement(new Double((xz*1000-Xorg)/incX).toString());
   key.addElement("CRPIX2  ");
   if (aladin != 1)
      value.addElement(new Double(Ycen).toString());
   else value.addElement(new Double((yz*1000.-Yorg)/incY).toString());
   key.addElement("EQUINOX");
   value.addElement(new Double(equinox).toString());
   boolean flagPermute = aladin!=1 && type1!=null
         && type1.startsWith("DEC");
   key.addElement("CRVAL1  ");
   if (aladin != 1)
      value.addElement(new Double(flagPermute?deltai:alphai).toString());
   else value.addElement(new Double(alpha).toString());
   key.addElement("CRVAL2  ");
   if (aladin != 1)
      value.addElement(new Double(flagPermute?alphai:deltai).toString());
   else value.addElement(new Double(delta).toString());
   key.addElement("CTYPE1  ");
   if (aladin == 1) value.addElement("'RA---TAN'");
   //PIERRE : ATTENTION type1 PEUT ETRE NULL -- NORMALEMENT, PLUS FB
   else value.addElement(type1);
   key.addElement("CTYPE2  ");
   if (aladin == 1) value.addElement("'DEC--TAN'");
   //PIERRE : ATTENTION type2 PEUT ETRE NULL -- NORMALEMENT, PLUS FB
   else value.addElement(type2);
   // key.addElement("RADECSYS");
   if( RADECSYS[system].length()>0 ) {
      key.addElement("RADECSYS");
      value.addElement(RADECSYS[system]);
   }
   // switch (system)
   //     {
   //      case 1 :
   //            value.addElement("FK4") ;
   //            break ;
   //      case 2 :
   //            value.addElement("GLON") ;
   //            break ;
   //      case 3 :
   //             value.addElement("SLON") ;
   //            break ;
   //      case 4 :
   //            value.addElement("ELON") ;
   //            break ;
   //      case 5 :
   //            value.addElement("FK5") ;
   //            break ;
   //      case 6 :
   //            value.addElement("ICRS") ;
   //            break ;
   //      case 7 :
   //        value.addElement("Solar") ;
   //          break ; 
   //     }
   if (aladin == 1)
   {
      //            double sca = incX/(1000.0*focale) ;
      CD[0][0]  = (incX/(1000.0*focale)) * rad_to_deg ;
      CD[0][1]  = 0 ;
      CD[1][0]  = 0 ;
      CD[1][1]  = (incY/(1000.0*focale)) * rad_to_deg ;
      //           System.out.println("CD "+CD[0][0] +" "+CD[1][1]);

      ID[0][0]  = ((1000.0*focale)/incX) * deg_to_rad ; 
      ID[0][1]  = 0 ;
      ID[1][0]  = 0 ;
      ID[1][1]  = ((1000.0*focale)/incY) * deg_to_rad ;
      double sca = deg_to_rad ;
      flagPermute = false ;
      key.addElement("PV1_0");
      value.addElement(new Double(xyapoly[0]).toString()) ;
      key.addElement("PV2_0");
      value.addElement(new Double(xydpoly[0]).toString()) ;
      key.addElement("PV1_1");
      value.addElement(new Double(xyapoly[2]*sca).toString()) ;
      key.addElement("PV2_1");
      value.addElement(new Double(xydpoly[2]*sca).toString()) ;
      key.addElement("PV1_2");
      value.addElement(new Double(xyapoly[1]*sca).toString()) ;
      key.addElement("PV2_2");
      value.addElement(new Double(xydpoly[1]*sca).toString()) ;
      key.addElement("PV1_3");
      value.addElement(new Double(xyapoly[4]*sca*sca).toString()) ;
      key.addElement("PV2_3");
      value.addElement(new Double(xydpoly[4]*sca*sca).toString()) ;
      key.addElement("PV1_4");
      value.addElement(new Double(xyapoly[5]*sca*sca).toString()) ;
      key.addElement("PV2_4");
      value.addElement(new Double(xydpoly[5]*sca*sca).toString()) ;
      key.addElement("PV1_5");
      value.addElement(new Double(xyapoly[3]*sca*sca).toString()) ;
      key.addElement("PV2_5");
      value.addElement(new Double(xydpoly[3]*sca*sca).toString()) ;
      key.addElement("PV1_6");
      value.addElement(new Double(xyapoly[7]*sca*sca*sca).toString()) ;
      key.addElement("PV2_6");
      value.addElement(new Double(xydpoly[7]*sca*sca*sca).toString()) ;
      key.addElement("PV1_7");
      value.addElement(new Double(xyapoly[9]*sca*sca*sca).toString()) ;
      key.addElement("PV2_7");
      value.addElement(new Double(xydpoly[9]*sca*sca*sca).toString()) ;
      key.addElement("PV1_8");
      value.addElement(new Double(xyapoly[8]*sca*sca*sca).toString()) ;
      key.addElement("PV2_8");
      value.addElement(new Double(xydpoly[8]*sca*sca*sca).toString()) ;
      key.addElement("PV1_9");
      value.addElement(new Double(xyapoly[6]*sca*sca*sca).toString()) ;
      key.addElement("PV2_9");
      value.addElement(new Double(xydpoly[6]*sca*sca*sca).toString()) ;
   }
   key.addElement("CD1_1   ");
   //            value.addElement(new Double(flagPermute?CD[1][0]:CD[0][0]).toString());
   value.addElement(new Double(CD[0][0]).toString());
   key.addElement("CD1_2   ");
   //            value.addElement(new Double(flagPermute?CD[1][1]:CD[0][1]).toString());
   value.addElement(new Double(CD[0][1]).toString());
   key.addElement("CD2_1   ");
   //            value.addElement(new Double(flagPermute?CD[0][0]:CD[1][0]).toString());
   value.addElement(new Double(CD[1][0]).toString());
   key.addElement("CD2_2   ");
   //            value.addElement(new Double(flagPermute?CD[0][1]:CD[1][1]).toString());
   value.addElement(new Double(CD[1][1]).toString());
   //            System.out.println("CD "+CD[0][0] +" "+CD[1][1]);
   }

   protected void SetEquinox(double equin) {

   equinox = equin ;

   }
   protected double GetEquinox() {

   /* Equinox =0 : absence d'equinoxe */
   return equinox ;

   }
   protected double GetEpoch() {

   /* Epoch =0 : absence d'epoque */
   if (flagepoc != 0) return epoch ;
   else return Double.NaN ;     // PF. nov 07 (Ã¯Â¿Â½ la place de 0.0)
   }

   /*
 * Retourne le centre de l'image en coord J2000 et en pixels
 */
   public Coord getImgCenter() throws Exception {
   Coord c = new Coord();
   c.x = xnpix/2.;
   c.y = ynpix/2.;
  // System.out.println("avant ImgCenter");
   GetCoord(c);
   return c;
   }

   /**
 * Retourne le centre de la projection en coord J2000 et en pixels
 */
   public Coord getProjCenter()  {
   Coord c = new Coord();
   c.x = Xcen;
   //      c.y = ynpix-Ycen;
   c.y = Ycen;
         try { GetCoord(c); } catch( Exception e ) { }
      //   c.al=alphai;
      //   c.del=deltai;
   return c;
   }
   /**
 * Retourne la rotation de la projection par rapport au NORD dans
 * le sens ???? (unite : le degre)
 */
   public double getProjRot() {
   return rota;
   }

   /**
 * Retourne la largeur du champ en degres
 */
   public double getImgWidth() { return widtha; }

   /**
 * Retourne la largeur du champ en degres
 */
   public double getImgHeight() { return widthd; }

   /**
 * Retourne true si les RA sont inverses
 */
   public boolean getProjSym() { return incA>0; }

   /**
 * Retourne Le type de projection (indice du tableau projection)
 */
   public int getProj() { return proj; }

   /**
 * Retourne Le système de coordonnées
 */
   public int getSystem() { return system; }

   /**
 * Retourne La dimension en pixels de l'image
 */
   public Dimension getImgSize() { return new Dimension(xnpix,ynpix); }

   /**
 * Modifie la calibration astrométrique pour prendre en compte une sous-image
 * @param offx,offy : coin haut gauche (si absent, centré)
 * @param  w,h : taille de la sous-image
 */
   protected void cropping(double w,double h) { cropping((xnpix -w)/2.,(ynpix -h)/2.,w,h); }
   protected void cropping(double offx, double offy, double w,double h ) {


   if(( aladin == 0)||(aladin == 3)) {
      Xcen -=  offx ;
      // toujours faire attention quand on part du haut. calib, comme FITS et WCS compte
      // du bas et il y a la hauteur à enlever en prime ...
      Ycen -= (ynpix -offy -h ) ;
   }
   else {
      Xorg += offx *incX ;
      Yorg += offy *incY ;
   }
   xnpix = (int)Math.round(w) ;
   ynpix = (int)Math.round(h) ;

   widtha = xnpix * Math.abs(incA) ;
   widthd = ynpix * Math.abs(incD) ;
   }

   // thomas, 19/11/2007
   // TODO : François, peux tu me vérifier cette méthode ?
   /** S'agit-il d'une calib avec rotation dans le sens direct */
   protected boolean sensDirect() {
   //  System.out.println("CD "+CD[0][0]+" "+CD[1][1]);
   //  System.out.println("xyapoly[2] "+xyapoly[2]+" "+xyapoly[1]);
   //  System.out.println("xydpoly[2] "+xydpoly[1]+" "+xydpoly[2]) ;
   double xyd =xydpoly[2];
   double xya ;


   if ((aladin == 0)||(aladin==3)){ xya = xyapoly[1];
   if ( xya> 0) { 
      return CD[0][0]*CD[1][1]>0.0; }
   if ( xya < 0) {
      return (-CD[0][0]*CD[1][1])>0.0; }

   return CD[0][0]*CD[1][1]>0.0; 
   }
   else if (aladin == 2) {
      return CD[0][0]*CD[1][1]>0.0; 
   }

   else { xya = xyapoly[2];
   return xya*xyd>0.0 ;
   }

   }

   /** Code modifié par Pierre F. Juillet 2010 - à vérifier par François B. SVP - code original ci-dessous
 * Remodifié par Pierre F. en Mars 2011 - toujours à vérifier par François B. SVP */
   protected void cropAndZoom(double deltaX, double deltaY, double w, double h, double zoom) {

   incX = incX/zoom ;
   incY = incY/zoom ;
   Xcen = Xcen*zoom ;
   Ycen = Ycen*zoom ;
   xnpix = (int)Math.round(xnpix * zoom) ;  // Modif PF juillet 2010
   ynpix = (int)Math.round(ynpix * zoom) ;  // Modif PF juillet 2010
   CD[0][0] = CD[0][0]/zoom ;
   CD[0][1] = CD[0][1]/zoom ;
   CD[1][0] = CD[1][0]/zoom ;
   CD[1][1] = CD[1][1]/zoom ;
   ID[0][0] = ID[0][0]*zoom ;
   ID[0][1] = ID[0][1]*zoom ;
   ID[1][0] = ID[1][0]*zoom ;
   ID[1][1] = ID[1][1]*zoom ;

   incA /=zoom;   // Modif PF mars 2011
   incD /=zoom;   // Modif PF mars 2011

   cropping(deltaX*zoom,deltaY*zoom,w*zoom,h*zoom) ;
   }

   /** Code original de François B. qui ne peut pas marcher pour zoom<1 (voir ci-dessus) */
   //   protected void cropAndZoom(double deltaX, double deltaY, double w, double h, double zoom) {
   //      incX = incX/zoom ;
   //      incY = incY/zoom ;
   //      Xcen = Xcen*zoom ;
   //      Ycen = Ycen*zoom ;
   //       int zzoom = (int)Math.round(zoom);
   //      xnpix = xnpix * zzoom ;
   //      ynpix = ynpix * zzoom ;
   //      CD[0][0] = CD[0][0]/zoom ;
   //      CD[0][1] = CD[0][1]/zoom ;
   //      CD[1][0] = CD[1][0]/zoom ;
   //      CD[1][1] = CD[1][1]/zoom ;
   //      ID[0][0] = ID[0][0]*zoom ;
   //      ID[0][1] = ID[0][1]*zoom ;
   //      ID[1][0] = ID[1][0]*zoom ;
   //      ID[1][1] = ID[1][1]*zoom ;
   //     cropping(deltaX*zzoom,deltaY*zzoom,w*zzoom,h*zzoom) ;
   //  }


   static double [][] testCenter = { {0,0}, {15,-1}, {259.9,89.99}, {0.1,-89.99} };


   static double EPSILON = 1./(60.*60.*1000.*1000.);   // Milli arcseconde

   static boolean equalEpsilon(double a, double b) {
      if( Double.isNaN(a) || Double.isNaN(b) ) return false;
      //    if (Math.abs(a-b) >= EPSILON) 
      //        { System.out.println("Diff "+(Math.abs(a-b))+" "+EPSILON) ;
      //        System.out.println(" "+a+" "+b+" "+EPSILON) ;}
      return Math.abs(a-b)<=EPSILON ;
   }

   //      test();
   //   }
   static public boolean test1() {
      boolean toutestbon=true;
      Calib C = new Calib(3,424,0,1) ;
      Calib Cp = new Calib(3,0,0,1 ) ;

      return toutestbon ;
   }

   static public boolean test() {
      boolean toutestbon=true;
      try {
         //         System.out.println("Test des projections ï¿½ la milliarcseconde ("+EPSILON+")\n" +
         //                " - centre de projection dans les 2 sens pour 4 valeurs clefs...\n" +
         //                " - bijectivitï¿½ pour des valeurs alï¿½atoires...\n" 
         //                );
         System.out.println("> Calib test...");
         //for( int proj=1; proj<projType.length; proj++ ) {
         int  proj = ARC ;  
         boolean erreur=false;
         System.out.print("   Test"+projType[proj]+"...");
         for( int j=0; j<testCenter.length; j++ ) {
            double ra  = testCenter[j][0];
            double dec = testCenter[j][1];
            double cxPix=250,cyPix=250;
            double widthPix=500,heightPix=500;
            double widthAng=90,heightAng=90;     //arcmin
            double rot=0;
            boolean sym=false;
            String centre = "   coo=("+ra+","+dec+")<=>xy=("+cxPix+","+cyPix+") : ";
            Calib c = new Calib(ra,dec,cxPix,cyPix,widthPix,heightPix,widthAng,heightAng,rot,proj,sym,FK5);

            Coord coo = new Coord();

            // Test de la projection dans les deux sens pour le centre de projection
            double x=cxPix,y=cyPix;
            coo.x=x; coo.y=y;
            c.GetCoord(coo);
            System.out.print("\nfixe"+centre+"  coo=("+coo.al+","+coo.del+") => xy=("+coo.x+","+coo.y+")");
            if( !equalEpsilon(coo.al,ra) || !equalEpsilon(coo.del,dec) ) {
               if( !erreur ) System.out.print(" Error");
               erreur=true;
               System.out.print("\nfixe"+centre+" Wrong celestian center: xy=("+x+","+y+") => coo=("+coo.al+","+coo.del+") ");
            }
            coo.al=ra; coo.del=dec;
            c.GetXY(coo);
            if( !equalEpsilon(coo.x,x) || !equalEpsilon(coo.y,y) ) {
               if( !erreur ) System.out.print(" Error");
               erreur=true;
               System.out.print("\nfixe"+centre+" Wrong projected center: coo=("+coo.al+","+coo.del+") => xy=("+coo.x+","+coo.y+")");
            }

            try {
               // Test de la bijectivitï¿½ pour des valeurs alï¿½atoires
               Random rand = new Random(System.currentTimeMillis());
               // System.out.println("try ");

               for( int i=0; i<1000; i++ ) {
                  //          x=coo.x = rand.nextDouble()*widthPix;
                  //          y=coo.y = rand.nextDouble()*heightPix;
                  //                     System.out.println("random avant GetCoord"+coo.x+" "+coo.y);
                  //          c.GetCoord(coo);
                  //   coo.al = rand.nextDouble()*Math.PI*2.0;
                  //   coo.del = (rand.nextDouble()-0.5)*Math.PI;
                  coo.al = Math.PI * (1-1.0/1000.0) ;
                  coo.del =  Math.PI*(-0.5 + i/1000.0) ;

                  c.GetXY(coo);
                  if (Math.abs(coo.y)>30000.0) {
                     System.out.println(" coo.aldel"+coo.al+" "+coo.del) ;
                     System.out.println(" coo.XY"+coo.x+" "+coo.y) ;}
                  // System.out.println("Grand coo.xy"+coo.x+" "+coo.y+" "+coo.al+" "+coo.del) ;
                  //      if ((Math.abs(coo.x) > 30000)||(Math.abs(coo.y) > 30000))
                  //    if (Math.abs(coo.x) > 20000)
                  //        System.out.println("Grand coo.xy"+coo.x+" "+coo.y+" "+coo.al+" "+coo.del) ;
                  //        if( !equalEpsilon(coo.x,x) || !equalEpsilon(coo.y,y) ) {
                  //           if( !erreur ) System.out.print(" Error");
                  //           erreur=true;
                  //           System.out.print("\n"+centre+" no bijective: xy=("+x+","+y+") => coo=("+coo.al+","+coo.del+") => xy=("+coo.x+","+coo.y+")");
                  //   break;
                  //}
               }
            } catch( Exception e ) {
               if( !erreur ) System.out.print("random Error");
               erreur=true;
               System.out.print("\n"+centre+" Java exception: xy=("+x+","+y+") => "+e.getMessage());
            }
         }
         if( !erreur ) System.out.println(" OK");
         else System.out.println();
         toutestbon &= erreur;
         //         return !erreur;
         //     } 
      } catch( Exception e ) { e.printStackTrace(); toutestbon=false; }
      return toutestbon;
   }
   
   
   
   
   // Pour tests FX
   static public void main( String [] args) {
      double cra=0., cdec=0.; // centre de la projection
      double withPix=1000.;   // taille en pixel
      double withAng=60.;     // taille angulaire (en arcmin)
      int proj=AIT;           // Type de projection
      
//      public Calib (double ra,double de, double cx, double cy,
//            double width, double height, double radius, double radius1, double rot, int proje,  boolean sym,
//            int systeme) {

      try {
         Calib c = new Calib(cra,cdec,withPix/2.,withPix/2.,withPix,withPix, withAng, withAng, 0. ,proj, false, FK5);

         Coord coo = new Coord();
         long t = System.currentTimeMillis();
         for( int i=0; i<1000000; i++ ) {
            coo.al= Math.random()*360.;
            coo.del = Math.random()*180. - 90.;
//            System.out.print(coo.al+","+coo.del);
            try {
               c.GetXY(coo);
//               System.out.print(" => "+coo.x+","+coo.y);
               c.GetCoord(coo);
//               System.out.println(" => "+coo.al+","+coo.del);
            } catch( Exception e ) {
//               System.out.println(" => hors projection");
            }
         }
         long t1 = System.currentTimeMillis();
         System.out.println("Test terminé en "+(t1-t)+"ms");
      } catch( Exception e ) { e.printStackTrace(); }
   }
}
