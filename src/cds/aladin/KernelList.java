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

package cds.aladin;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import cds.tools.Util;

/**
 * Gestion d'une liste de convolution
 * @author Pierre Fernique [CDS]
 * @version 1.0 - octobre 2010
 */
public class KernelList {
   
   private Aladin aladin;
   private Vector<Kernel> list;   // Liste des noyaux
   
   // Pour fournir des noms uniques
   static private int NK = 0;
   static private String createConvName() { return "Kernel"+(++NK); }

   public KernelList(Aladin aladin) {
      this.aladin=aladin;
      list = new Vector<Kernel>();
      addDefaultKernels();
   }
   
   /** retourne l'indice d'un kernel déjà présent dans la liste, -1 si non trouvé */
   public int findKernel(String name) {
      Enumeration<Kernel> e = list.elements();
      for( int i=0; e.hasMoreElements(); i++ ) {
         Kernel k = e.nextElement();
         if( k.name.equalsIgnoreCase(name) ) return i;
      }
      return -1;
   }
   
   /** Ajout/remplacement d'une convolution à la liste. La définition de la convolution est passée
    * dans le paramètre "kdef" suivant les syntaxes ci-dessous :
    * [name=]nn0 nn1 nn2... nnX
    * [name=]gauss( {fwhm|sigma}=angle [,radius=x] )
    * Dans le deuxième cas, la résolution de l'image concernée doit être indiquée dans l'argument pixelRes.
    * Si le nom de la convolution existe déjà il y a écrasement de la précédente définition. Si le nom
    * est omis, un nom automatique sera généré.
    * @param kdef définition de la convolution
    * @param pixRes résolution angulaire du pixel de l'image concernée en degrés
    * @return l'indice dans le Vector list
    */
   public int addKernel(String kdef,double pixelRes) throws Exception  {
      String name;
      Kernel k=null;
      
      // Y a-t-il en préfixe un nom ?
      int offset = kdef.indexOf('=');
      int j= kdef.indexOf('(');
      if( offset>0 && (j==-1 || j>offset)) { name = kdef.substring(0,offset); kdef = kdef.substring(offset+1); }
      else name=createConvName();

      // S'agit-il d'une défintion sous la forme name=gauss(size,sigma)
      offset = kdef.indexOf('(');
      if( offset>=0 ) {
         double [] param = parseGaussCmd(kdef);
         if( param==null ) throw new Exception("usage: gauss( {fwhm|sigma}=angle [,radius=x] )");
         k = createGaussienMatrix(param[0],param[1],pixelRes,(int)param[2]);

      // matrice donnée par ses valeurs, ligne par ligne
      } else {
         k = new Kernel();
         k.matrix = parseKernel(kdef);
      }
      k.name = name;

      // Ajout ou remplacement ?
      int i = findKernel(name);
      if( i<0 ) { list.addElement(k); i=list.size()-1; }
      else list.setElementAt(k, i);
      
      // On prévient la combo box d'affichage dans FrameConvolution
      if( aladin.frameConvolution!=null ) aladin.frameConvolution.reloadComboKernel(name);
      return i;
   }
   
   /** Retourne la matrice désigné par son indice - méthode de test*/
   protected double[][] getKernel(int i) {
      return list.elementAt(i).matrix;
   }
   
   /** Retourne la liste des noms des convolutions. 1 par ligne - utilisé par la commande
    * script "kernel" sans paramètre */
   public String getKernelList() {
      StringBuffer s = new StringBuffer();
      Enumeration<Kernel> e = list.elements();
      while( e.hasMoreElements() ) s.append(e.nextElement().name+"\n");
      return s.toString();
   }
   
   /** Retourne la liste des noms des convolutions sous la forme d'une Vector */
   public Vector<String> getKernelListAsVector() {
      Vector<String> v = new Vector<String>();
      Enumeration<Kernel> e = list.elements();
      while( e.hasMoreElements() ) v.addElement(e.nextElement().name);
      return v;
   }
   
   /** Retourne tous les kernels qui correspondent à un masque de recherche
    * sous la forme d'une chaine - utilisé par la commande script
    * "kernel mask".
    * Rq: Cette méthode peut produire une très volumineuse chaine de caractères */
   public String getKernelDef(String mask) {
      StringBuffer s = new StringBuffer();
      Enumeration<Kernel> e = list.elements();
      while( e.hasMoreElements() ) {
         Kernel k = e.nextElement();
         if( mask!=null && !Util.matchMask(mask, k.name) ) continue;
         s.append(k);
      }
      return s.toString();
   }
   
   /** Parsing d'une commande suivante la syntaxe : gauss(fwhm=xxx,sigma=...,radius=...)
    * @return les 3 valeurs, 0 en cas d'absence, null si problem */
   static public double [] parseGaussCmd(String s) {
      double [] res = new double[3];
      try {
         int i = s.indexOf('(');
         if( !s.substring(0,i).trim().endsWith("gauss") ) return null;
         int j= s.lastIndexOf(')');
         StringTokenizer t = new StringTokenizer(s.substring(i+1,j),",");
         while( t.hasMoreTokens() ) {
            String s1 = t.nextToken();
            i = s1.indexOf('=');
            String cmd;
            if( i<0 ) cmd="fwhm";  // par défaut c'est la FWHM
            else cmd = s1.substring(0,i).trim();
            String value = s1.substring(i+1).trim();
                 if( cmd.equalsIgnoreCase("fwhm") )   res[0] = Server.getAngleInArcmin(value,Server.RADIUS)/60.;
            else if( cmd.equalsIgnoreCase("sigma") )  res[1] = Server.getAngleInArcmin(value,Server.RADIUS)/60.;
            else if( cmd.equalsIgnoreCase("radius") ) res[2] = Integer.parseInt(value);
            else return null;
         }
      } catch( Exception e ) { return null; }
      return res;
   }
   
   /** Analyse la chaine k qui représente un kernel pour une convolution.
    * Il s'agit d'une suite de nombres représentant une matrice supposée rangée
    * ligne par ligne. Ces nombres peuvent être séparés par une , ou un espace
    * Retourne le tableau de la matrice, ou null */
   static private double[][] parseKernel(String k) throws Exception {
      int i=0,j=0;
      Tok tok = new Tok(k);
      int size = (int)Math.sqrt(tok.countTokens());
      double kernel[][] = null;
      while( tok.hasMoreTokens() ) {
         double x = Double.parseDouble(tok.nextToken());
         if( kernel==null ) kernel = new double[size][size];
         kernel[i][j++] = x;
         if( j==size ) { j=0; i++; }
      }
      return kernel;
   }
      
   /** Retourne le kernel déterminé par son nom dans la liste des kernels prédéfinis,
    * ou directement par une matrice passée en paramètre qui peut prendre deux syntaxes :
    * soit : n0m0 n1m0 n2m0 n0m1.... soit name=n0m0 n1m0 n2m0 n0m1
    */
   public Kernel getKernel(String s,double pixRes) throws Exception {
      int i = findKernel(s);
      if( i<0 ) i= addKernel(s,pixRes);
      return list.elementAt(i);
   }
   
   /**
    * Création d'une matrice de convolution gaussienne.
    * fwhm et sigma sont exclusifs, l'un doit être renseigné, l'autre à zéro. Si aucun des deux n'est renseigné, le sigma est imposé à 0.5
    * le radius de la matrice est soit imposé, et si le paramètre est nul, il est déduit pour 3*fwhm en fonction de la résolution angulaire
    * indiquée en degrés.
    * Pour le calcul rapide de la gaussienne, on calcule également le vecteur central de la gaussienne normalisé.
    */
   static public Kernel createGaussienMatrix(double fwhm,double sigma,double pixelRes,int radius) throws Exception {
      double [] p = computeGaussParam(fwhm,sigma,pixelRes,radius);
      fwhm=p[0]; sigma=p[1]; radius=(int)p[2];
      if( radius>Kernel.MAXRADIUS ) throw new Exception("Too large kernel [radius="+radius+"]");
//      String s = "fwhm="+Coord.getUnit(fwhm)+" sigma="+Coord.getUnit(sigma)+" pixel="+Coord.getUnit(pixelRes)+" radius="+radius;
//      Aladin.aladin.command.printConsole(s);
//      Aladin.trace(4,"Kernel.createGaussienMatrix() fwhm="+Coord.getUnit(fwhm)+" sigma="+Coord.getUnit(sigma)+" pixelRes="+Coord.getUnit(pixelRes)+" radius="+radius);
      Kernel k = new Kernel();
      k.matrix = createGaussienMatrix(radius,sigma/pixelRes);
      k.gaussian = createFastGaussienMatrix(radius,sigma/pixelRes);
      return k;
   }
   
   static final double FCTSIGMA = 2*Math.sqrt(2.*Math.log(2.));
   
   /** Détermine les valeurs de fwhm en fonction de sigma, ou le contraire. Détermine
    * le radius de la matrice si non spécifié.
    * @param fwhm taille en degrés de la FWHM ou 0 si non spécifié
    * @param sigma taille en degrés du sigma ou 0 si non spécifié
    * @param pixelRes résolution angulaire du pixel de l'image concernée en degrés
    * @param radius rayon de la matrice, ou 0 s'il faut le calculé.
    * @return p[0]<-fwhm, p[1]<-sigma, p[2]<-radius
    */
   static public double [] computeGaussParam(double fwhm,double sigma,double pixelRes,int radius) {
      if( sigma==0 && fwhm==0 ) sigma=0.5;
      else if( fwhm!=0 ) sigma=fwhm/FCTSIGMA;
      else fwhm= FCTSIGMA*sigma;
      double r = 3*fwhm;
      double npix = r/pixelRes;
      if( radius==0 ) radius = (int)(Math.ceil(npix))/2;
      if( radius<1 ) radius=1;
      return new double[]{fwhm,sigma,radius};
   }
   
   /** Création d'une matrice gaussienne normalisée, de rayon et de sigma spécifié
    * un rayon de 1 fournit une matrice 3*3 */
   static public double [][] createGaussienMatrix(int rayon, double sigma) {
      int largeur = 2*rayon+1;
      double [][] matrix = new double[largeur][largeur];
      double factor=0;
      for( int y=0;y<largeur; y++ ) {
         double ky = y-rayon;
         for( int x=0; x<largeur; x++) {
            double kx = x-rayon;
            double e = Math.exp( -(kx*kx + ky*ky) / (2*sigma*sigma) );
            matrix[x][y] = e;
            factor += e;
         }
      }
      for( int y=0;y<largeur; y++ ) {
         for( int x=0; x<largeur; x++) matrix[x][y] /= factor;
      }
      return matrix;
   }
   
   /** Création d'un vecteur gaussien normalisé, de rayon et de sigma spécifié
    * un rayon de 1 fournit un vecteur de 3 */
   static public double [] createFastGaussienMatrix(int rayon, double sigma) {
      int largeur = 2*rayon+1;
      double []gaussian = new double[largeur];
      double factor=0;
      for( int col=0;col<largeur; col++ ) {
         double ky = col-rayon;
         int x=rayon;
         double kx = x-rayon;
         double e = Math.exp( -(kx*kx + ky*ky) / (2*sigma*sigma) );
         gaussian[col] = e;
         factor += e;
      }
      for( int col=0; col<largeur; col++) gaussian[col] /= factor;
      return gaussian;
   }
   
   
   /******************** Quelques noyaux de convolution principalement issus de S-extractor *****************/
   
   /** Crée les kernels prédéfinis dans Aladin */
   public void addDefaultKernels() {
      Kernel k;
      list.add(k=new Kernel("Gauss-1.5pix", GAUSS1_5)); k.normalize();
      list.add(k=new Kernel("Gauss-2pix",   GAUSS2));   k.normalize();
      list.add(k=new Kernel("Gauss-2.5pix", GAUSS2_5)); k.normalize();
      list.add(k=new Kernel("Gauss-3pix",   GAUSS3));   k.normalize();
      list.add(k=new Kernel("Gauss-4pix",   GAUSS4));   k.normalize();
      list.add(k=new Kernel("Gauss-5pix",   GAUSS5));   k.normalize();
      list.add(k=new Kernel("Mex-1.5pix",   MEX1_5));   k.normalize();
      list.add(k=new Kernel("Mex-2pix",     MEX2));     k.normalize();
      list.add(k=new Kernel("Mex-2.5pix",   MEX2_5));   k.normalize();
      list.add(k=new Kernel("Mex-3pix",     MEX3));     k.normalize();
      list.add(k=new Kernel("Mex-4pix",     MEX4));     k.normalize();
      list.add(k=new Kernel("Mex-5pix",     MEX5));     k.normalize();
      list.add(k=new Kernel("Tophat-1.5pix",TOPHAT1_5));k.normalize();
      list.add(k=new Kernel("Tophat-2pix",  TOPHAT2));  k.normalize();
      list.add(k=new Kernel("Tophat-2.5pix",TOPHAT2_5));k.normalize();
      list.add(k=new Kernel("Tophat-3pix",  TOPHAT3));  k.normalize();
      list.add(k=new Kernel("Tophat-4pix",  TOPHAT4));  k.normalize();
      list.add(k=new Kernel("Tophat-5pix",  TOPHAT5));  k.normalize();
      list.add(k=new Kernel("Blur-",    CONTRAST));     k.normalize();
      list.add(k=new Kernel("Blur+",    FLOU));         k.normalize();
      list.add(k=new Kernel("Pyramidal",PYRAMIDAL));    k.normalize();
      list.add(k=new Kernel("Edge",     BORD));         k.normalize();
   }
   
   static final private double CONTRAST[][] = { { 0,-1,0 }, { -1,5,-1 }, { 0,-1,0 }};
   static final private double FLOU[][]     = { { 1/9.,1/9.,1/9. }, { 1/9.,1/9.,1/9. }, { 1/9.,1/9.,1/9. }};
   static final private double BORD[][]     = { { 0,1,0 }, { 1,-4,1 }, { 0,1,0 }};
   static final private double PYRAMIDAL[][] = {
      {1,2,1},
      {2,4,2},
      {1,2,1}
   };
   static final private double GAUSS1_5[][]  = {
      {0.109853,0.300700,0.109853},
      {0.300700,0.823102,0.300700 },
      {0.109853,0.300700,0.109853}
   };
   static final private double GAUSS2[][]    = {
      {0.260856,0.483068,0.260856},
      {0.483068,0.894573,0.483068},
      {0.260856,0.483068,0.260856}
   };
   static final private double GAUSS2_5[][]  = {
      {0.034673,0.119131,0.179633,0.119131,0.034673},
      {0.119131,0.409323,0.617200,0.409323,0.119131},
      {0.179633,0.617200,0.930649,0.617200,0.179633},
      {0.119131,0.409323,0.617200,0.409323,0.119131},
      {0.034673,0.119131,0.179633,0.119131,0.034673}
   };
   static final private double GAUSS3[][]  = {
      {0.092163,0.221178,0.296069,0.221178,0.092163},
      {0.221178,0.530797,0.710525,0.530797,0.221178},
      {0.296069,0.710525,0.951108,0.710525,0.296069},
      {0.221178,0.530797,0.710525,0.530797,0.221178},
      {0.092163,0.221178,0.296069,0.221178,0.092163},
   };
   static final private double GAUSS4[][]  = {
      {0.047454,0.109799,0.181612,0.214776,0.181612,0.109799,0.047454},
      {0.109799,0.254053,0.420215,0.496950,0.420215,0.254053,0.109799},
      {0.181612,0.420215,0.695055,0.821978,0.695055,0.420215,0.181612},
      {0.214776,0.496950,0.821978,0.972079,0.821978,0.496950,0.214776},
      {0.181612,0.420215,0.695055,0.821978,0.695055,0.420215,0.181612},
      {0.109799,0.254053,0.420215,0.496950,0.420215,0.254053,0.109799},
      {0.047454,0.109799,0.181612,0.214776,0.181612,0.109799,0.047454},
   };
   static final private double GAUSS5[][]  = {
      {0.030531,0.065238,0.112208,0.155356,0.173152,0.155356,0.112208,0.065238,0.030531},
      {0.065238,0.139399,0.239763,0.331961,0.369987,0.331961,0.239763,0.139399,0.065238},
      {0.112208,0.239763,0.412386,0.570963,0.636368,0.570963,0.412386,0.239763,0.112208},
      {0.155356,0.331961,0.570963,0.790520,0.881075,0.790520,0.570963,0.331961,0.155356},
      {0.173152,0.369987,0.636368,0.881075,0.982004,0.881075,0.636368,0.369987,0.173152},
      {0.155356,0.331961,0.570963,0.790520,0.881075,0.790520,0.570963,0.331961,0.155356},
      {0.112208,0.239763,0.412386,0.570963,0.636368,0.570963,0.412386,0.239763,0.112208},
      {0.065238,0.139399,0.239763,0.331961,0.369987,0.331961,0.239763,0.139399,0.065238},
      {0.030531,0.065238,0.112208,0.155356,0.173152,0.155356,0.112208,0.065238,0.030531},
   };
   static final private double MEX1_5[][]  = {
      {-0.000109,-0.002374,-0.006302,-0.002374,-0.000109},
      {-0.002374,-0.032222,-0.025569,-0.032222,-0.002374},
      {-0.006302,-0.025569, 0.276021,-0.025569,-0.006302},
      {-0.002374,-0.032222,-0.025569,-0.032222,-0.002374},
      {-0.000109,-0.002374,-0.006302,-0.002374,-0.000109},
   };
   static final private double MEX2[][]  = {
     {-0.000006,-0.000132,-0.000849,-0.001569,-0.000849,-0.000132,-0.000006},
     {-0.000132,-0.002989,-0.017229,-0.028788,-0.017229,-0.002989,-0.000132},
     {-0.000849,-0.017229,-0.042689,0.023455,-0.042689,-0.017229,-0.000849},
     {-0.001569,-0.028788,0.023455,0.356183,0.023455,-0.028788,-0.001569},
     {-0.000849,-0.017229,-0.042689,0.023455,-0.042689,-0.017229,-0.000849},
     {-0.000132,-0.002989,-0.017229,-0.028788,-0.017229,-0.002989,-0.000132},
     {-0.000006,-0.000132,-0.000849,-0.001569,-0.000849,-0.000132,-0.000006},
   };
   static final private double MEX2_5[][] = {
     {-0.000284,-0.002194,-0.007273,-0.010722,-0.007273,-0.002194,-0.000284},
     {-0.002194,-0.015640,-0.041259,-0.050277,-0.041259,-0.015640,-0.002194},
     {-0.007273,-0.041259,-0.016356,0.095837,-0.016356,-0.041259,-0.007273},
     {-0.010722,-0.050277,0.095837,0.402756,0.095837,-0.050277,-0.010722},
     {-0.007273,-0.041259,-0.016356,0.095837,-0.016356,-0.041259,-0.007273},
     {-0.002194,-0.015640,-0.041259,-0.050277,-0.041259,-0.015640,-0.002194},
     {-0.000284,-0.002194,-0.007273,-0.010722,-0.007273,-0.002194,-0.000284},
   };
   static final private double MEX3[][] = {
     {-0.000041,-0.000316,-0.001357,-0.003226,-0.004294,-0.003226,-0.001357,-0.000316,-0.000041},
     {-0.000316,-0.002428,-0.010013,-0.022204,-0.028374,-0.022204,-0.010013,-0.002428,-0.000316},
     {-0.001357,-0.010013,-0.035450,-0.054426,-0.050313,-0.054426,-0.035450,-0.010013,-0.001357},
     {-0.003226,-0.022204,-0.054426,0.033057,0.164532,0.033057,-0.054426,-0.022204,-0.003226},
     {-0.004294,-0.028374,-0.050313,0.164532,0.429860,0.164532,-0.050313,-0.028374,-0.004294},
     {-0.003226,-0.022204,-0.054426,0.033057,0.164532,0.033057,-0.054426,-0.022204,-0.003226},
     {-0.001357,-0.010013,-0.035450,-0.054426,-0.050313,-0.054426,-0.035450,-0.010013,-0.001357},
     {-0.000316,-0.002428,-0.010013,-0.022204,-0.028374,-0.022204,-0.010013,-0.002428,-0.000316},
     {-0.000041,-0.000316,-0.001357,-0.003226,-0.004294,-0.003226,-0.001357,-0.000316,-0.000041},
   };
   static final private double MEX4[][] = {
     {-0.002250,-0.007092,-0.015640,-0.024467,-0.028187,-0.024467,-0.015640,-0.007092,-0.002250},
     {-0.007092,-0.021141,-0.041403,-0.054742,-0.057388,-0.054742,-0.041403,-0.021141,-0.007092},
     {-0.015640,-0.041403,-0.057494,-0.024939,0.008058,-0.024939,-0.057494,-0.041403,-0.015640},
     {-0.024467,-0.054742,-0.024939,0.145167,0.271470,0.145167,-0.024939,-0.054742,-0.024467},
     {-0.028187,-0.057388,0.008058,0.271470,0.459236,0.271470,0.008058,-0.057388,-0.028187},
     {-0.024467,-0.054742,-0.024939,0.145167,0.271470,0.145167,-0.024939,-0.054742,-0.024467},
     {-0.015640,-0.041403,-0.057494,-0.024939,0.008058,-0.024939,-0.057494,-0.041403,-0.015640},
     {-0.007092,-0.021141,-0.041403,-0.054742,-0.057388,-0.054742,-0.041403,-0.021141,-0.007092},
     {-0.002250,-0.007092,-0.015640,-0.024467,-0.028187,-0.024467,-0.015640,-0.007092,-0.002250},
   };
   static final private double MEX5[][] = {
     {-0.002172,-0.005657,-0.011702,-0.019279,-0.025644,-0.028106,-0.025644,-0.019279,-0.011702,-0.005657,-0.002172},
     {-0.005657,-0.014328,-0.028098,-0.042680,-0.052065,-0.054833,-0.052065,-0.042680,-0.028098,-0.014328,-0.005657},
     {-0.011702,-0.028098,-0.049016,-0.059439,-0.051288,-0.043047,-0.051288,-0.059439,-0.049016,-0.028098,-0.011702},
     {-0.019279,-0.042680,-0.059439,-0.030431,0.047481,0.093729,0.047481,-0.030431,-0.059439,-0.042680,-0.019279},
     {-0.025644,-0.052065,-0.051288,0.047481,0.235153,0.339248,0.235153,0.047481,-0.051288,-0.052065,-0.025644},
     {-0.028106,-0.054833,-0.043047,0.093729,0.339248,0.473518,0.339248,0.093729,-0.043047,-0.054833,-0.028106},
     {-0.025644,-0.052065,-0.051288,0.047481,0.235153,0.339248,0.235153,0.047481,-0.051288,-0.052065,-0.025644},
     {-0.019279,-0.042680,-0.059439,-0.030431,0.047481,0.093729,0.047481,-0.030431,-0.059439,-0.042680,-0.019279},
     {-0.011702,-0.028098,-0.049016,-0.059439,-0.051288,-0.043047,-0.051288,-0.059439,-0.049016,-0.028098,-0.011702},
     {-0.005657,-0.014328,-0.028098,-0.042680,-0.052065,-0.054833,-0.052065,-0.042680,-0.028098,-0.014328,-0.005657},
     {-0.002172,-0.005657,-0.011702,-0.019279,-0.025644,-0.028106,-0.025644,-0.019279,-0.011702,-0.005657,-0.002172},
   };
   static final private double TOPHAT1_5[][] = {
      {0.000000,0.180000,0.000000},
      {0.180000,1.000000,0.180000},
      {0.000000,0.180000,0.000000},
   };
   static final private double TOPHAT2[][] = {
      {0.080000,0.460000,0.080000},
      {0.460000,1.000000,0.460000},
      {0.080000,0.460000,0.080000},
   };
   static final private double TOPHAT2_5[][] = {
      {0.260000,0.700000,0.260000},
      {0.700000,1.000000,0.700000},
      {0.260000,0.700000,0.260000},
   };
   static final private double TOPHAT3[][] = {
      {0.560000,0.980000,0.560000},
      {0.980000,1.000000,0.980000},
      {0.560000,0.980000,0.560000},
   };
   static final private double TOPHAT4[][] = {
      {0.000000,0.220000,0.480000,0.220000,0.000000},
      {0.220000,0.990000,1.000000,0.990000,0.220000},
      {0.480000,1.000000,1.000000,1.000000,0.480000},
      {0.220000,0.990000,1.000000,0.990000,0.220000},
      {0.000000,0.220000,0.480000,0.220000,0.000000},
   };
   static final private double TOPHAT5[][] = {
      {0.150000,0.770000,1.000000,0.770000,0.150000},
      {0.770000,1.000000,1.000000,1.000000,0.770000},
      {1.000000,1.000000,1.000000,1.000000,1.000000},
      {0.770000,1.000000,1.000000,1.000000,0.770000},
      {0.150000,0.770000,1.000000,0.770000,0.150000},
   };
}

