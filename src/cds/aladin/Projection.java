// Copyright 2010 - UDS/CNRS
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

import java.util.Enumeration;
import java.util.Vector;

import cds.fits.HeaderFits;
import cds.tools.Util;

/**
 * Classe gerant les projections associees au plan
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : 24 aout 2005 - prise en compte de la calibration des images rectangulaires
 * @version 1.1 : 20 aout 2002 - Gros bidouillage en lien avec FrameNewCalib
 *                               et Calib
 * @version 1.1 : 28 mai 99 - Integration de la projection WCS
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Projection {

   // Les constantes
   static final int NO       =0;
   static final int ALADIN   =1;
   static final int WCS      =2;
   static final int SIMPLE   =3;
   static final int QUADRUPLET=4;
   static final int PLOT     =5;
   static final String [] NAME = { "-", "Aladin reduction",
                                   "WCS reduction",
                                   "Simple reduction","Matching star red.","Squattered plot" };

   // Les parametres de la projection
   protected int frame = Localisation.ICRS;
   protected String label=null;		// Label de la projection
   protected double raj,dej;		// Centre de l'image (J2000)
   protected double alphai,deltai;	// Centre de la projection (J2000)
   protected double cx,cy;		    // Pixels correspondants a alphai, deltai
   protected double rm;			    // Largeur du champ en arcmin
   protected double rm1; 			// hauteur du champ en arcmin
   protected double r;			    // Largeur du champ en pixels
   protected double r1; 			// hauteur du champ en pixels
   protected double rot;	        // rotation en degres (sens horaire / nord )
   protected boolean sym;	        // symetrie des alphas
   protected int system;            // le système de coordonnée selon Bof
   protected int t;		            // type de la projection (SIN|TAN...)
   protected int modeCalib;			// mode de projection (NO,ALADIN,WCS);
   protected boolean toNorth;       // True s'il s'agit d'une calibration orientée vers le Nord
//   protected int frame = Localisation.ICRS; // Repere
//   protected double fct;            // Facteur d'échelle (rm/r => PROBLEME SI rm!=rm1 OU r!=r1


   protected Calib c; 		        // Calibration François B. correspondante  DESORMAIS TOUJOURS UTILISE

   // Les infos associées à une recalibration en cours
   protected Coord coo[];			// Liste de quadruplets pour methode QUADRUPLET de recalibration
   
   // Liste des projections comme elles apparaissent dans Aladin, et correspondances dans Calib
   static final String [] alaProj            = {"SINUS", "TANGENTIAL", "AITOFF", "ZENITAL_EQUAL_AREA", "STEREOGRAPHIC", "CARTESIAN", "MOLLWEIDE" };
   static final String [] alaProjToType      = {"SIN",   "TAN",        "AIT",    "ZEA",                "STG",           "CAR",       "MOL" };

   /** Retourne l'indice de la signature de la projection (case insensitive, qu'il s'agisse de son nom complet
    * apparaissant dans Aladin, ou sa signature */
   static public int getProjType(String projectionName) {
      int i = Util.indexInArrayOf(projectionName,alaProj,true);
      if( i>= 0 ) projectionName = alaProjToType[i];
      return Util.indexInArrayOf(projectionName,Calib.projType,true);
   }
   
   /** Retourne l'indice de la projection passée en paramètre (case insensitive)
    * dans le tableau des projections compatibles Aladin, -1 si non trouvée */
   static public int getAlaProjIndex(String projectionName) {
      int i = Util.indexInArrayOf(projectionName,alaProj,true);
      if( i>=0 ) return i;
      return Util.indexInArrayOf(projectionName,alaProjToType,true);
   }
   
   /** Retourne la liste des projections supportées par Aladin */
   static public String[] getAlaProj() { return alaProj ; }


   /** Aucune projection */
   protected Projection() { modeCalib=NO; }
   
  /** Creation d'une projection à partir d'une autre projection */
   protected Projection(Projection p) {
      c=p.c;
      adjustParamByCalib(c);
   }

   protected double getRaMax() {
      return t==Calib.SIN || t==Calib.TAN ? 180 :360;
   }

   protected double getDeMax() {
      return t==Calib.SIN || t==Calib.TAN || t==Calib.AIT || t==Calib.CAR || t==Calib.MOL ? 180 : 360;
   }
   
    protected Projection(double refX,double refY,double x, double y, double refW, double refH, double w, double h, 
          boolean flipX, boolean flipY,boolean logX, boolean logY) {
       modeCalib=PLOT;
       raj=alphai = refX; dej=deltai = refY;
       cx = x; cy = y;
       rm = refW; rm1 = refH;
       r = w; r1 = h;
       flipPlotX = flipX ? -1 : 1;
       flipPlotY = flipY ? -1 : 1;
       logPlotX = logX;
       logPlotY = logY;
//       t=Calib.XYLINEAR;
    }
    
    protected double getFctXPlot() { return r/rm; }
    protected double getFctYPlot() { return r1/rm1; }
    
    protected Coord getXYPlot(Coord coo) {
       if( Double.isNaN(coo.al) ) { coo.x=Double.NaN; coo.y=Double.NaN; return coo; }
       try {
          double valX = logPlotX ? Math.log(coo.al - alphai) : coo.al - alphai;
          double valY = logPlotY ? Math.log(coo.del - deltai) : coo.del - deltai;
          coo.x = ( valX * r/rm * flipPlotX ) + cx;
          coo.y = ( valY * -r1/rm1 * flipPlotY ) + cy;
       } catch( Exception e ) {
          e.printStackTrace();
          coo.x = Double.NaN;
          coo.y = Double.NaN;
       }
       return coo;
    }
    
    protected Coord getCoordPlot(Coord coo) {
       if( Double.isNaN(coo.x) ) { coo.al=Double.NaN; coo.del=Double.NaN; return coo; }
       try {
          double valX = logPlotX ? Math.exp(coo.x - cx) : coo.x - cx;
          double valY = logPlotY ? Math.exp(coo.y - cy) : coo.y - cy;
          coo.al  = ( valX * rm/r * flipPlotX ) + alphai;
          coo.del = ( valY * -rm1/r1 * flipPlotY ) + deltai;
       } catch( Exception e ) {
          coo.al = Double.NaN;
          coo.del = Double.NaN;
       }
       return coo;
    }
    
    private double flipPlotX = 1;
    private double flipPlotY = 1;
    private boolean logPlotX = false;
    private boolean logPlotY = false;
    
    protected  boolean isFlipXPlot() { return flipPlotX==-1; }
    protected  boolean isFlipYPlot() { return flipPlotY==-1; }
    protected  boolean isLogXPlot() { return logPlotX; }
    protected  boolean isLogYPlot() { return logPlotY; }
    
    protected void flipPlot(int n,boolean flag) {
       if( n==0 ) flipPlotX = flag ? -1 : 1;
       else flipPlotY = flag ? -1 : 1;
    }
    
    protected void logPlot(int n,boolean flag) {
       if( n==0 ) logPlotX=flag;
       else logPlotY=flag;
    }

    
  /*
   * Creation d'une projection en fonction des parametres standards
   * @param label   label de la projection
   * @param type    type de la projection (mode ALADIN|WCS...)
   * @param alphai,deltai centre de la projection (J2000 en degres)
   * @param rm      diametre/largeur du champ (arcmin)
   * @param rm1     [hauteur du champ (arcmin) - si différent de rm]
   * @param cx,cy   Centre de la projection (pixels)
   * @param r       Taille de la projection en pixels
   * @param r1      [Hauteur de la projection en pixels - si différente de r]
   * @param rot     Rotation (degre / nord)
   * @param sym     Symetrie RA
   * @param t       Type de la projection mathematique (TAN|SIN...)
   */
   protected Projection(String label,int type,
         double alphai, double deltai, double rm,
         double cx, double cy,double r,
         double rot,boolean sym,int t,int system) {
      this(label,type,alphai,deltai,rm,rm,cx,cy,r,r,rot,sym,t,system);
   }
   protected Projection(String label,int type,
                        double alphai, double deltai, double rm,double rm1,
                        double cx, double cy,double r, double r1,
                        double rot,boolean sym,int t,int system) {

      c=new Calib(alphai,deltai,cx,cy,r,r1,rm,rm1,rot,t,sym,system);
      adjustParamByCalib(c);
      this.modeCalib=type;
      this.label=label;
      if( this.label==null ) this.label = getName(type,t);
   }

  /*
   * Creation d'une projection en fonction d'une Calibration
   * @param type type de la projection (mode ALADIN|WCS...)
   * @param c    Calibration
   */
   public Projection(int type,Calib c) {
      this.c=c;
      adjustParamByCalib(c);
      this.modeCalib=type;
      label = getName(type,0);
   }

   /** Crée un clone de la projection (calibration comprise) */
   protected Projection copy() {
      Projection p = new Projection();
      p.frame=frame;
      p.raj=raj;		p.dej=dej;
      p.alphai=alphai; 	p.deltai=deltai;
      p.cx=cx;			p.cy=cy;
      p.rm=rm;			p.r=r;
      p.rm1=rm1;		p.r1=r1;
      p.rot=rot;		p.sym=sym;
      p.t=t;			p.system=system;
      p.modeCalib=modeCalib;		p.label=label==null?null:new String(label);
      if( coo!=null ) {
         p.coo = new Coord[coo.length];
         System.arraycopy(coo,0,p.coo,0,coo.length);
      }
      p.c = c==null ? null : Calib.copy(c);
      
      return  p;
   }

   private Projection projNorth=null;

   /** Génère une projection identique mais orientée le nord du frame vers le Haut, l'Est vers la gauche
    * @param angle entre le Nord du système de référence et le Nord équatorial */
   protected Projection toNorth(double angle) {
      if( rot==angle ) return this;
      if( projNorth==null || projNorth.c.getProjRot()!=angle ) {
         projNorth = new Projection();
         projNorth.c=new Calib(alphai,deltai,cx,cy,r,r1,rm,rm1,angle,t,false,system);
         projNorth.adjustParamByCalib(projNorth.c);
         projNorth.modeCalib=modeCalib;
         projNorth.label = getName(modeCalib,t);
//         projNorth.frame = frame;
         projNorth.coo = null;
      }
      return projNorth;
   }

   /** Ajustement de la projection suite à un cropping */
   protected void crop(double x,double y,double w, double h) {
      c.cropping(x,y,w,h);
      adjustParamByCalib(c);
   }

   protected void  cropAndZoom(double x, double y, double w, double h, double zoom){
      c.cropAndZoom(x,y,w,h,zoom);
      adjustParamByCalib(c);
   }

   /** Modification d'une projection */
//   protected void modify(String label,int modeCalib,
//         double alphai, double deltai, double rm,
//         double cx, double cy,double r,
//         double rot,boolean sym,int t,int system) {
//      modify(label,modeCalib,alphai,deltai,rm,rm,cx,cy,r,r,rot,sym,t,system);
//   }
   protected void modify(String label,int modeCalib,
            double alphai, double deltai, double rm,double rm1,
            double cx, double cy,double r,double r1,
            double rot,boolean sym,int t,int system) {
      c=new Calib(alphai,deltai,cx,cy,r,r1,rm,rm1,rot,t,sym,system);
      adjustParamByCalib(c);
      this.modeCalib=modeCalib;
      if( label==null ) this.label = getName(modeCalib,t);
      this.coo = null;
   }

   protected void setProjCenter(double ra,double dec) {
      Coord c = new Coord(ra,dec);
      if( frame!=Localisation.ICRS ) c = Localisation.frameToFrame(c, Localisation.ICRS, frame);
      modify(label,modeCalib,c.al,c.del,rm,rm1,cx,cy,r,r1,rot,sym,t,system);
   }
   
   protected void setProjRot(double rota) {
      modify(label,modeCalib,alphai,deltai,rm,rm,cx,cy,r,r,rota,sym,t,system);
   }
   
   protected void deltaProjRot(double drot) {
      double rota = rot+drot;
      if( rota>360 ) rota-=360;
      else if( rota<0 ) rota+=360;
      modify(label,modeCalib,alphai,deltai,rm,rm,cx,cy,r,r,rota,sym,t,system);
   }

   protected void deltaProjCenter(double dra,double ddec) {
      double ra = alphai+dra;
      double de = deltai+ddec;
      double rota = rot;
      if( de>89.95 ) { de=180-de; ra+=180; rota+=180; }
      else if( de<-89.95 ) { de=-de-180; ra+=180; rota-=180; }
      if( ra>360 ) ra-=360;
      else if( ra<0 ) ra+=360;
      if( rota>360 ) rota-=360;
      else if( rota<0 ) rota+=360;
      modify(label,modeCalib,ra,de,rm,rm,cx,cy,r,r,rota,sym,t,system);
   }

   protected void deltaProjXYCenter(double deltaX,double deltaY) {
      c.Xcen+=deltaX;
      c.Ycen+=deltaY;
      adjustParamByCalib(c);
   }

   /** Retourne true s'il s'agit d'une vraie projection astrométrique */
   static protected boolean isOk(Projection p) {
      return p!=null && p.modeCalib!=NO;
   }

   /** Retourne le type de projection adéquate (géré par Calib)
    * en fonction du rayon
    * @param radius rayon en degres
    * @return le type de projection
    */
   static protected int getDefaultType(double radius) {
      return radius<2.? Calib.TAN: radius<60.? Calib.ZEA:  Calib.AIT;
   }

  /** Modification de la projection par recalibration au moyen
   * d'une liste de quadruplets (x,y,alpha,delta)
   */
   protected void modify(String label,Coord coo[]) {
      if( label==null ) label = getName(modeCalib,t);
      this.label=label;
      this.modeCalib=QUADRUPLET;
      this.coo=coo;
//      Aladin.aladin.command.toStdoutln("Recalibration by correspondances:");
//      for( int i=0; i<coo.length; i++ ) {
//         Aladin.trace(3,"   "+i+") xy="+coo[i].x+","+coo[i].y+" ra,dec="+coo[i].getSexa());
//      }

      c = c.recalibrate(coo);
      adjustParamByCalib(c);
   }

  /** Modification de la projection par changement de calibration */
   protected void modify(String label,Calib c) {
      if( label==null ) label = getName(modeCalib,t);
      this.label=label;
      this.modeCalib=WCS;
      this.coo=coo;
      this.c = c;
//      if( frame!=Localisation.ICRS ) {
//         Coord c1 = Localisation.frameToFrame(new Coord(c.alphai,c.deltai), Localisation.ICRS, frame);
//         c.alphai=c1.al;
//         c.deltai=c1.del;
//      }
      this.coo = null;
      adjustParamByCalib(c);
   }

  /** Recupere les parametres de la projection a partir de la calib passee
   * en parametre
   * @param c la calib
   */
   protected void adjustParamByCalib(Calib c) {
      try {
         Coord co = c.getProjCenter();
         cx = co.x;
         cy = co.y;
         alphai = co.al;
         deltai = co.del;
         co = c.getImgCenter();
         if( frame!=Localisation.ICRS ) co = Localisation.frameToFrame(co, frame,Localisation.ICRS);
//         co.x = c.xnpix/2.;
//         co.y = c.ynpix/2.;
//         getCoord(co);
         raj = co.al;
         dej = co.del;
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }

      double w = c.getImgWidth();
      double h = c.getImgHeight();
      rm=w*60;
      rm1=h*60;
      r=c.getImgSize().width;
      r1=c.getImgSize().height;
      rot=c.getProjRot();
      sym=c.getProjSym();
      system=c.getSystem();
      t=c.getProj();
   }
   


   /** Retourne la résolution angulaire en degrées en pixel en alpha */
   protected double getPixResAlpha() throws Exception {
      return c.getImgWidth()/c.getImgSize().width;
   }

   /** Retourne la résolution angulaire en degrées en pixel en alpha */
   protected double getPixResDelta() throws Exception {
      return c.getImgHeight()/c.getImgSize().height;
   }

  /** Modification de la projection pour MRCOMP
   * @param scale facteur d'echelle
   * @param cx,cy nouveau centre en pixel
   * @param xpix,ypix largeur en pixel
   */
   protected void resize(int scale) {
      try { c = c.resize(scale); } catch( Exception e) { return; }
      adjustParamByCalib(c);
   }

   protected void resize(int scale,double cx, double cy , int xpix, int ypix) {
      c = c.resize(scale,cx,cy,xpix,ypix);
      adjustParamByCalib(c);
   }

  /** Modification de la projection par flip horizontal ou vertical
   * @param methode 0 Haut/Bas, 1 Gauche/Droite, 2 les deux
   */
   protected void flip(int methode) {
      if( methode==0 || methode==2 ) c = c.flipBU();
      if( methode==1 || methode==2 ) c = c.flipRL();
      adjustParamByCalib(c);
   }

   @Override
   public String toString() {
      return (label==null?"":label+" ")+getName(modeCalib,t)+" "+
             round(alphai)+","+round(deltai)+"=>"+((int)cx)+","+((int)cy)+" "+
             "("+rm+"x"+rm1+")/("+r+"x"+r1+") "+
             (rot!=0.?"rot="+rot+"deg ":"")+
             (sym?"RA_symetry ":"")+
             "system="+system+
             " "+Localisation.REPERE[frame];
   }

   /** retourne true si la projection couvre une zone supérieure à 45° */
   protected boolean isLargeField() {
      return (rm>45*60 || rm1>45*60) && !isXYLinear();
   }

  /** Retourne true si la projection peut etre modifiable */
   protected boolean isModifiable() {
      return modeCalib==SIMPLE || modeCalib==QUADRUPLET || modeCalib==WCS;
   }

   /** Il s'agit d'un calcul de coordonnées particulier (dédié au SOLAIRE) qui ne consiste
    * qu'à une homothétie via CDELT et CRVAL
    */
   protected boolean isXYLinear() {
      return modeCalib==PLOT || c!=null && c.system == 7;
   }

  /** Retourne le nom de la projection
   * @param type mode de la projection
   * @param t methode de la projection
   * @return nom de la projection
   */
   static protected String getName(int type,int t) {
      try { return type==SIMPLE ? Calib.getProjName(t) : NAME[type];
      } catch( Exception e ) { return "noname"; }
   }

   /** Retourne le nom de la projection */
   protected String getName() { return getName(modeCalib,t); }
   
   /** Construction de l'entete WCS FITS dans le TextArea wcsT concerne
    * en fonction de la projection, ou vide sinon */
   protected String getWCS() {
      StringBuffer s = new StringBuffer();

      Vector key   = new Vector(20);
      Vector value = new Vector(20);
//      try { c.GetWCS(key,value); }
      try { getWCS(key,value); }
      catch( Exception e ) { System.err.println("GetWCS error"); return null; }
      Enumeration ekey   = key.elements();
      Enumeration evalue = value.elements();
      while( ekey.hasMoreElements() ) {
         String skey   = (String)ekey.nextElement();
         String svalue = (String)evalue.nextElement();
         s.append(skey+"= "+svalue+"\n");
      }
      return s.toString();
   }
   
   /** La liste des mots clés WCS retournée par Calib va être modifié en fonction du frame propre à la classe Projection
    * Ceci est nécessaire parce que par défaut Calib ne supportait que l'équatorial et qu'il avait fallu
    * traiter les changement de référentiel pour le mode Allsky au niveau de la classe Projection (variable frame)
    * Le but est de fournir la Calib WCS dans le frame de visu courante (sélecteur Localisation)
    * A noter que le centre de la projection CRVAL1 et CRVAL2 est déjà exprimé dans le frame de la projection (beurk)
    * et n'a donc plus à être converti (cf setProjCenter(...))
    * 
    * Lorsque Calib saura correctement gérer tous les référentiels, on pourra directement l'utiliser
    *
    * @param key Liste des mots clés WCS
    * @param value Liste des valeurs correspondantes
    * @throws Exception
    */
   protected void getWCS(Vector key, Vector value) throws Exception {
      c.GetWCS(key,value);
      if( isEquatorial() /* isFrameEqualsCalibSyst() */ ) return;
      
      Enumeration ekey   = key.elements();
      Enumeration evalue = value.elements();
      for( int i=0; ekey.hasMoreElements(); i++ ) {
         String skey   = (String)ekey.nextElement();
         String svalue = (String)evalue.nextElement();
         
         if( skey.startsWith("CTYPE1") ) {
            String ctype = Localisation.CTYPE1[frame];
            svalue = ctype+svalue.substring(ctype.length());
            value.setElementAt(svalue,i);
         }
         
         else if( skey.startsWith("CTYPE2") ) {
            String ctype = Localisation.CTYPE2[frame];
            svalue = ctype+svalue.substring(ctype.length());
            value.setElementAt(svalue,i);
         }

         else if( skey.startsWith("RADECSYS") ) {
            String radecsys = Localisation.RADECSYS[frame];
            if( radecsys==null ) { key.remove(i); value.remove(i); }
            else value.setElementAt(radecsys,i);
         }
      }
   }
   /** Construction d'une projection dont le frame est géré par Calib et non par Projection 
    * => Actuellement uniquement possible pour Equatorial et Galactique => J'attends FB pour le reste
    */
   static protected Projection getEquivalentProj(Projection p) throws Exception {
      if( p.isEquatorial() || p.system!=Calib.GALACTIC /* p.isFrameEqualsCalibSyst() */ ) return p;        // Rien à faire, pas encore possible
      p.c = new Calib( new HeaderFits(p.getWCS()) );
      p.adjustParamByCalib(p.c);
      p.frame=Localisation.ICRS;
      return p;
   }
   
   private boolean isEquatorial() {
      return frame==Localisation.ICRS || frame==Localisation.ICRSD
      || frame==Localisation.J2000 || frame==Localisation.J2000D
      || frame==Localisation.B1950 || frame==Localisation.B1950D
      || frame==Localisation.B1900
      || frame==Localisation.B1875 ;
   }
   
  /** Arrondi.
   * Applique au centre de la projection pour permettre les superpositions
   * legerement decalees (lors de l'interrogation)
   * @param x la valeur a arrondir
   * @return  la valeur arrondie
   */
   static protected double round(double x) { return Math.ceil(x*10)/10; }

  /** Positionnement d'une calibration Aladin
   * @param c La calibration aladin qu'il faut associe a la projection
   protected void setCalib(Calib c) { this.c = c; }
   */

  /** Test de ``superposabilite'' de deux projections.
   * Retourne vrai si la projection passee en parametre est compatible
   * avec l'objet projection
   * @param p la projection a comparer avec la projection courante
   * @param v la vue courante
   * @param testBG true si on écarte le cas d'une superposition sur un plan BG, notamment
   *               pour éviter que les losanges de PlanBGCat ne puissent être affichés
   * @return <I>true</I> c'est ok - <I>false</I> c'est mauvais
   */
   protected boolean agree(Projection p,ViewSimple v) { return agree(p,v,true); }
   protected boolean agree(Projection p,ViewSimple v,boolean testBG) {

      if( p==null ) return false;

      // La meme projection
      if( this==p ) return true;

      double z=1;
      if( v!=null ) {
         // sur un background
         if( testBG && v.pref!=null && v.pref instanceof PlanBG ) return true;
         z = v.getZoom();
      }

      // La distance entre les deux centres d'images
      // doit etre inferieure a la somme de deux demi tailles*sqrt(2)
      Coord c1 = new Coord(); c1.al=p.raj; c1.del=p.dej;
//System.out.println("Centre 1 : "+Coord.getSexa(c1.al,c1.del));
      Coord c2 = new Coord(); c2.al=raj;   c2.del=dej;
//System.out.println("Centre 2 : "+Coord.getSexa(c2.al,c2.del));
      double dist = Coord.getDist(c1,c2);

      // Si fond du ciel, 10° comme test de superposabilité
//      if( dist<10 ) return true;

//System.out.println("p.rm="+Coord.getUnit(p.rm/120)+" rm="+ Coord.getUnit(rm/120));
      double somme = (rm+p.rm)*Math.sqrt(2)/120;
      if( z<1 ) somme/=z;
//System.out.println("La distance entre les deux centres est de : "+Coord.getUnit(dist));
//System.out.println("La somme des deux demi-tailles est de : "+Coord.getUnit(somme));
//System.out.println("Projection "+(dist<=somme?"possible":"refusee"));

      // Champs très grands => toujours superposables
      if( somme>45 ) return true;

      return dist<=somme;
   }

   private Coord cotmp = new Coord();

  /** Retourne les coordonnees J2000 du x,y en fonction de la projection.
   * @param coo  position dans la projection (x,y doivent y etre renseignes)
   * @return     les coordonnees (class Coord) ou <I>null</I> si pb
   */
   public Coord getCoord(Coord coo) {
      Coord c = getCoordNative(coo);
      if( modeCalib==PLOT ) return c;
      if( frame==Localisation.ICRS || coo.al==Double.NaN ) return c;
      Localisation.frameToFrame(c, frame,Localisation.ICRS);
      return c;

   }

   /** Retourne les coordonnees Native (dépendant du frame de la projection)
    *  du x,y en fonction de la projection.
    * @param coo  position dans la projection (x,y doivent y etre renseignes)
    * @return     les coordonnees (class Coord) ou <I>null</I> si pb
    */
   protected Coord getCoordNative(Coord coo) {
      if( modeCalib==PLOT )  return getCoordPlot(coo);
      if( Double.isNaN(coo.x) ) { coo.al=Double.NaN; coo.del=Double.NaN; return coo; }
      
      try {
         cotmp.x=coo.x+0.5; cotmp.y=coo.y-0.5;
         c.GetCoord(cotmp);
         coo.al=cotmp.al; coo.del=cotmp.del;

      } catch( Exception e ) { coo.al=Double.NaN; coo.del=Double.NaN; }
      return coo;
   }

  /** Retourne les x,y en fonction des coord et de la projection.
   * @param coo position dans la projection (alphai,deltai doivent y etre renseignes)
   * @return    les coordonnees (class Coord) ou null si pb
   */
   public Coord getXY(Coord coo) {
      if( modeCalib==PLOT )  return getXYPlot(coo);
      if( Double.isNaN(coo.al) ) { coo.x=Double.NaN; coo.y=Double.NaN; return coo; }
      
      try {
         if( frame==Localisation.ICRS ) c.GetXY(coo);
         else {
            cotmp.al=coo.al; cotmp.del=coo.del;
            cotmp = Localisation.frameToFrame(cotmp, Localisation.ICRS, frame);
            c.GetXY(cotmp);
            coo.x=cotmp.x; coo.y=cotmp.y;
         }
         if( !Double.isNaN(coo.x)) { coo.x-=0.5; coo.y+=0.5; }
      } catch( Exception e ) { coo.x=Double.NaN; coo.y=Double.NaN; }
      return coo;
   }

   /** Retourne les x,y en fonction des coord et de la projection.
    * @param coo position dans la projection (alphai,deltai doivent y etre renseignes)
    * @return    les coordonnees (class Coord) ou null si pb
    */
    protected Coord getXYNative(Coord coo) {
       if( modeCalib==PLOT )  return getXYPlot(coo);
       if( Double.isNaN(coo.al) ) { coo.x=Double.NaN; coo.y=Double.NaN; return coo; }
       
       try {
          c.GetXY(coo);
          if( !Double.isNaN(coo.x)) { coo.x-=0.5; coo.y+=0.5; }
       } catch( Exception e ) { coo.x=Double.NaN; coo.y=Double.NaN; }
       return coo;
    }
    

    protected Coord getProjCenter()  {
       if( modeCalib==PLOT ) {
          Coord c = new Coord(alphai,deltai);
          c.x=cx;
          c.y=cy;
          return c;
       }
       
       Coord coo = c.getProjCenter();
       return Localisation.frameToFrame(coo,frame,Localisation.ICRS);
    }

   // thomas, 19/11/2007
   /** S'agit-il d'une projection dans le sens direct */
   protected boolean sensDirect() {
       // ce cas m'embete un peu ...
       if( c==null ) return false;

       return c.sensDirect();
   }


/*
   static final int HM=20;
   private boolean testProj() {
      double x,y;
      Coord c = new Coord();
      int deltaX[] = new int[HM];
      int deltaY[] = new int[HM];
      int deltaMaxX=0;
      int deltaMaxY=0;
      int error=0;
      for( x=0; x<1024; x+=10. ) {
         for( y=0; y<1024; y+=10. ) {
            c.x=x; c.y=y;
            getCoord(c);
            if( c.al==c.del && c.al==0.0 || c.al<0. ) continue;
            getXY(c);
            int dx = (int)Math.round(Math.abs(10*(c.x-x)));
            int dy = (int)Math.round(Math.abs(10*(c.y-y)));
            if( dx>0 || dy>0 ) error++;
            if( dx>=HM ) deltaMaxX++;
            else deltaX[dx]++;
            if( dy>=HM ) deltaMaxY++;
            else deltaY[dy]++;
         }
      }
      if( error==0 ) { return true;
      }
      System.out.println("*** Problème sur projection "+this);
      System.out.println("*** Test X,Y -> alpha,delta -> X,Y sur ([0..1024],[0..1024] de 10 en 10)");
      for( int i=1; i<HM; i++ ) {
         if( deltaX[i]==0 && deltaY[i]==0 ) continue;
         System.out.println("   .Décalage de "+(i/10.)+" pixel(s) (à 0.05 prêt): en X:"+deltaX[i]+" en Y:"+deltaY[i]);
      }
      if( deltaMaxX!=0 || deltaMaxY!=0 ) {
         System.out.println("   .Décalage de plus de "+((HM-1)/10.)+ " pixel(s): en X:"+deltaMaxX+" en Y:"+deltaMaxY);
      }
      return false;
   }
*/
}
