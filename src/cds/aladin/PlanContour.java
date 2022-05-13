// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import java.awt.Color;
import java.awt.Rectangle;

import cds.tools.Util;

/**
 * Plan dedie aux contours
 *
 * @author Thomas Boch[CDS]
 * @version 0.9 : (fevrier 2002) creation
 */
public final class PlanContour extends PlanTool {

  // constantes
  static final String OutOfMemoryMESSAGE = "\n\n Either the image is too big \n or there " +
         "are too many control points.\n Try to zoom in the part of the picture you are interested in \n" +
         "(and select Consider current zoom only) \n and/or reduce the number of contour levels";

  static final int MAXLEVELS = 20;      // nb maximum de niveaux
  // les couleurs de base (il doit y en avoir un nombre pair)
  static final Color[] couleursBase = {new Color(250,51,51),new Color(51,51,250),new Color(153,51,255),new Color(255,51,153),new Color(51,153,255),new Color(51,255,153)};
  //static final Color[] couleursBase = Couleur.DC;


  static int icouleursBase = 0;         // indice couleur suivante si plus de libre

  // variables pour l'ajustement d'un niveau
  boolean mustAdjustContour = false;    // si true, il s'agit juste d'un ajustement de contour
  double[] adjustTab=null;				// nouveau tableau des niveaux

  // references aux objets
  Calque calque;
  Plan p;

  boolean reduceNoise = true;           // faut-il faire une moyenne glisse sur les pixels de l'image
  boolean useOnlyCurrentZoom = false;   // faut-il dessiner les isocontours uniquement en considerant le zoom courant ?

  private Ligne[][] lines;     	// Tableau dont les elts sont des tableaux de Ligne
    	    	    	    	    	// permet de garder une reference sur les lignes
    	    	    	    	    	// afin de modifier leur couleur ou de les cacher

  private double partDessin = 0.15;     // part du pourcentage que l'on estime dedie a la creation des lignes

  private int xShift=0;
  private int yShift=0;     	    	// decalages vertical et horizontal (sert a dessiner les contours)

  Rectangle zoomv;   	    	    	// zoom courant

  boolean useSmoothing = false;         // utilise-t-on le lissage sur l'image ?

  int smoothingLevel = 2;               // dimension du cote du carre utilise pour lisser l'image

  byte[] orgPixels;         // pixels originaux de l'image
  short[] pixels;			// tableau des pixels de l'image à traiter


  int width = -1;			// largeur de l'image à traiter
  int height = -1;			// hauteur de l'image à traiter
  int orgWidth = -1;	    	    	// hauteur de l'image d'origine
  int orgHeight = -1;	    	    	// largeur de l'image d'origine

  int max;				// max des valeurs de niveau de gris
  int min;				// min des valeurs de niveau de gris

  private PlanImage pimg = null;
  private double[] levels = null;	    	// tableau des niveaux de gris (differe de orgLevels si useSmoothing=true)
  private double[] orgLevels = null; 	    	// tableau des niveaux initiaux (entres par l'utilisateur)


  PointD[][] contours = null;               // contient l'ensemble des contours
  Color[] couleursContours;             // contient les couleurs associees a chaque contour

  protected ContourAlgorithm cAlgo = null;  // algo utilise pour obtenir les isocontours

  protected int nbLevels;   	    	// nb de niveaux

  private int nbLevelsComputed = 0;

  /** Creation d'un PlanContour
   * @param levels  	    	Le tableau des niveaux
   * @param cAlgo   	    	L'algorithme de calcul des contours
   * @param useSmoothing    	Utilisation ou non du smoothing
   * @param useOnlyCurrentZoom  True si l'on ne prend en compte que la vue courante
   * @param couleurs   	    	Tableau des couleurs de chaque contour (si null, initialisation par defaut)
   */
  protected PlanContour(Aladin aladin, String label, PlanImage pimg, double[] levels, ContourAlgorithm cAlgo, boolean useSmoothing, 
        int smoothingLevel, boolean useOnlyCurrentZoom, boolean reduceNoise, Color[] couleurs, Color cPlanContour) {
      this(aladin, label);

      // S'il n'y a pas de plan de reference, on bloque les xy dans l'ecran
      if( aladin.calque.getPlanRef()==null ) hasXYorig=true;
      else hasXYorig=false;
      // fin test

      askActive=false;
      flagOk = false;

      cAlgo.pc = this;
      this.pimg = pimg;
      this.levels = levels;
      this.orgLevels = this.levels.clone();
      this.nbLevels = levels.length;

      this.cAlgo = cAlgo;
      this.useSmoothing = useSmoothing;
      this.smoothingLevel = smoothingLevel;
      this.useOnlyCurrentZoom = useOnlyCurrentZoom;
      this.reduceNoise = reduceNoise;

      initCouleurs(couleurs);
      this.c = cPlanContour;

      synchronized( this ) {
         runme = new Thread(this,"AladinContour");
         Util.decreasePriority(Thread.currentThread(), runme);
         runme.start();
      }

  }

  /** Creation d'un PlanContour
   * @param label le nom du plan (dans la pile des plans)
   */
   protected PlanContour(Aladin aladin, String label) {
      super(aladin, label);
      this.calque = aladin.calque;

      setPourcent(-1);
   }


  /** Creation d'un PlanContour (pour un backup) */
   protected PlanContour(Aladin aladin) {
      this(aladin,"");
   }
   
   /** les contours ne sont jamais sélectionnables (PF nov 2010) */
   public boolean isSelectable() { return false; }
   
   public boolean isMovable() { return false; }

  /** Attente pendant la construction du plan.
   * @return <I>true</I> si ok, <I>false</I> sinon.
   */
   protected boolean waitForPlan() {

     try {
       setPourcent(-1);


       // il s'agit juste d'un ajustement de niveau
       if (mustAdjustContour) {

         for (int i=0;i<orgLevels.length;i++) {
         	// si le nouveau niveau est différent du niveau original
			if( adjustTab[i] != orgLevels[i] )
				doAdjustContour(adjustTab[i], i);
         }
         mustAdjustContour = false;
         adjustTab = null;
         return true;
       }

       // il s'agit du calcul en entier
       else {

         // attente du zoom   BIZARRE PIERRE A REFLECHIR
         while(!calque.zoom.zoomView.zoomok ) {
            Util.pause(10);
           //r=calque.zoom.zoomView.getZoom();
         }

         //System.out.println(r.width);
         //System.out.println(r.height);
         //this.zoom = new Rectangle(r.width,r.height);
         this.zoomv = aladin.calque.zoom.getZoom();

         setPourcent(0);

         if (!getAllContours()) {
           Aladin.error(aladin.error+ OutOfMemoryMESSAGE);
           return false;

         }
         else drawAllContours();

         setPourcent(-1);
         return true;
       }

     } // fin try

     catch (OutOfMemoryError e) {aladin.error = e.toString();Aladin.error(e + OutOfMemoryMESSAGE);return false;}
     catch (Exception exc) {aladin.error = exc.toString();Aladin.error(aladin.error);return false;}
   }


   /** remplit le tableau pixels avec l'image du plan de base lissé au besoin
    *  fixe les valeurs de height et width
    *  @return true en cas de succes, false si une Exception a ete souleve */
   protected boolean getPixels() {

//       p = calque.getPlanBase();
       p = pimg==null ? calque.getPlanBase() : pimg;
       if ( p == null ) return false;
       objet = p.objet;
       body = p.body;

       orgWidth = ((PlanImage)p).width;
       orgHeight = ((PlanImage)p).height;
       
       // si le zoom est en dehors de l'image, on ajuste
       zoomv.x = zoomv.x>0?zoomv.x:0;
       zoomv.y = zoomv.y>0?zoomv.y:0;
       zoomv.width = zoomv.width>orgWidth?orgWidth:zoomv.width;
       zoomv.height = zoomv.height>orgHeight?orgHeight:zoomv.height;

       if( (zoomv.x+zoomv.width)>orgWidth) zoomv.width = orgWidth - zoomv.x;
       if( (zoomv.y+zoomv.height)>orgHeight) zoomv.height = orgHeight - zoomv.y;

       try {

          // data : tableau des pixels
       orgPixels = ((PlanImage)p).getBufPixels8();
       short[] data = new short[orgPixels.length];

       for (int i=data.length-1;i>=0;i--) {
           data[i] = (short)(orgPixels[i]<0 ? 256 + orgPixels[i] : orgPixels[i]);
       }

       // remplissage du tableau pixels
       if (useOnlyCurrentZoom) {
         width = zoomv.width;
         height = zoomv.height;

         //  useOnlyCurrentZoom et lissage activé
         if (useSmoothing) {

           // chaque dimension est divisee par smoothingLevel
           this.width = width/smoothingLevel;
           this.height = height/smoothingLevel;

           this.pixels = new short[width*height];

           makeSmoothing(data,pixels,width,height,orgWidth,orgHeight,zoomv.x,zoomv.y,smoothingLevel);
         }
         // useOnlyCurrentZoom sans lissage
         else {
           pixels = new short[width*height];
           for (int y=height-1;y>=0;y--) {
             for (int x=width-1;x>=0;x--) {
               pixels[y*width+x] = data[y*orgWidth+x+zoomv.x+orgWidth*zoomv.y];
             }
           }
         }
       }
       // traitement a effectuer sur toute l'image
       else {
         width = orgWidth;
         height = orgHeight;

         // lissage sur toute l'image
         if(useSmoothing) {
           // chaque dimension est divisee par smoothingLevel
           this.width = width/smoothingLevel;
           this.height = height/smoothingLevel;

           this.pixels = new short[width*height];

           makeSmoothing(data,pixels,width,height,orgWidth,orgHeight,0,0,smoothingLevel);

         }
         // pas de lissage a faire, on recupere l'image en entier
         else {
           pixels = new short[width*height];
           pixels = data;
         }
       }

       if(reduceNoise) {
          pixels = moyenne(pixels,width,height);
          width=width-1;
          height=height-1;
       }

       } // fin du try

       catch(  OutOfMemoryError e ) {aladin.error = e.toString(); aladin.gc(); return false;}
       catch(  Exception exc  ) {aladin.error = exc.toString();  return false;}


       p.sendLog("Contour","["+p.getLogInfo()+"]");
       return true;

   }





   /* calcule tous les contours pour les valeurs de levels */
   protected boolean getAllContours() {
        // initialisation du tableau contours
        contours = new PointD[levels.length][];

   	if (!getPixels()) {
   	  //System.out.println("Pas assez de memoire pour les contours");
   	  return false;
   	}

        adjustLevels();

        // on transmet a l'algorithme le tableau de pixels et la taille de l'image
        cAlgo.setData(pixels);
        cAlgo.setDimension(width,height);


       try {
         // boucle sur l'ensemble des niveaux
         for (int indiceLevel = 0; indiceLevel<levels.length ; indiceLevel++) {

   	      PointD[] contourCourant = getContour(levels[indiceLevel]);

   	      contours[indiceLevel] = contourCourant;
   	      nbLevelsComputed++;
   	 }
       }

       catch(OutOfMemoryError e) {aladin.error = e.toString();return false;}
       return true;

   }

   /** ajuste les niveaux au cas ou on utilise le lissage */
   private void adjustLevels() {
       if (useSmoothing) {
           for (int i=0;i<orgLevels.length;i++) {
               levels[i] = smoothingLevel * smoothingLevel * orgLevels[i];
           }
       }
   }

   /** retourne le contour - sous forme d'un vecteur de PointD - pour une valeur donnee
    * @param level  - le niveau pour lequel on veut les contours
    * @return tableau de points correspondant aux contours pour le niveau level
    * (les differents contours pour un meme niveau sont separes par un objet "null")
    */
   private PointD[] getContour(double level) {
   	cAlgo.setLevel(level);

   	return cAlgo.getContours();
   }
   
   /* dessine les contours d'un niveau defini
    * @param indLevel indice dans orgLevels du niveau du contour a dessiner
    */
   protected void drawContour(int indLevel) {
      int nbPoints = 0;

      // PIERRE
      // Plan base = calque.getPlanBase();
      ViewSimple v = aladin.view.getCurrentView();
      Plan base = v.pref;
      Projection projv=v.getProj();
      
      if (base == null) base = this;
      
      int coeff = 1; // coefficient multiplicateur (pour redonner la bonne position quand on a lisse l'image en entree)
      if (useSmoothing) coeff = smoothingLevel;

      // ajustement des variables de decalage pour le trace des lignes
      if (useOnlyCurrentZoom) {
         xShift += zoomv.x;
         yShift += zoomv.y;
      }
      
      // ajustement en raison d'un decalage avec smoothing et/ou avec reduceNoise
      if(useSmoothing) {
         if(reduceNoise) {
            xShift+=smoothingLevel;
            yShift+=smoothingLevel;
         }
         else {
            xShift+=smoothingLevel/2+smoothingLevel%2;
            yShift+=smoothingLevel/2+smoothingLevel%2;
         }
      }
      else if(reduceNoise) {
          xShift+=1;
          yShift+=1;
      }


      PointD[] cont = contours[indLevel];

      Ligne[] curLevLines = new Ligne[cont.length];


      int i=0;
      int j;
      Coord c = new Coord();

      while (i<cont.length-1) {
         i++;
         PointD point0 = cont[i];
         if(point0 == null) continue;


         double x1,y1; // coordonnees d'une extremite de la ligne a tracer

         // coordonnees dans le plan d'origine
         c.x = coeff*point0.x+xShift;
         c.y = coeff*point0.y+yShift;
         x1 = c.x;
         y1 = c.y;

         // projection dans le plan actuel
         // (peut se produire si on change d'image de référence en cours de calcul des contours)
         if(p.projd != projv) {
            c.x = v.HItoI(c.x);  // PF 06/2007
            c.y = v.HItoI(c.y);
            this.p.projd.getCoord(c); // recherche des coordonnes alpha, delta du point en fonction des x,y dans le plan d'origine
            projv.getXY(c);  // reprojection dans le plan courant
            x1 = v.ItoHI(c.x);
            y1 = v.ItoHI(c.y);
         }

         Ligne ligne0 = new Ligne(this, v, v.HItoI(x1), v.HItoI(y1),couleursContours[indLevel]);
         //   	    String lab = "Isocontour at level " + orgLevels[indLevel];
         //        ligne0.setWithLabel(true);
         //   	    ligne0.setText(lab);

         pcat.setObjet(ligne0);

         curLevLines[nbPoints] = ligne0;
         nbPoints++;

         // boucle sur tous les points de ce contour
         for (j=1;(i+j)<cont.length;j++) {
            PointD pointj = cont[i+j];
            if(pointj==null) break;

            // coordonnees dans le plan d'origine
            c.x = coeff*pointj.x+xShift;
            c.y = coeff*pointj.y+yShift;
            x1 = c.x;
            y1 = c.y;


            // projection dans le plan actuel
            if(p.projd != projv) {
               c.x = v.HItoI(c.x);  // PF 06/2007
               c.y = v.HItoI(c.y);
               this.p.projd.getCoord(c); // recherche des coordonnes alpha, delta du point en fonction des x,y dans le plan d'origine
               projv.getXY(c);  // reprojection dans le plan courant
               x1 = v.ItoHI(c.x);
               y1 = v.ItoHI(c.y);
            }

            Ligne lignej = new Ligne(this,v, v.HItoI(x1), v.HItoI(y1), ligne0,couleursContours[indLevel]);
            //   	        lignej.setWithLabel(true);
            //   	        lignej.setText(lab);

            pcat.setObjet(lignej);
            curLevLines[nbPoints] = lignej;
            nbPoints++;
            ligne0 = lignej;
         }
         i=i+j;


      }

      Ligne[] tmp = new Ligne[nbPoints];
      System.arraycopy(curLevLines,0,tmp,0,nbPoints);

      lines[indLevel] = tmp;
      xShift = 0;
      yShift = 0;
   }

   /** drawAllContours
    * dessine tous les contours
    * les contours doivent avoir ete calcules precedemment par getAllContours()
    */
   protected void drawAllContours() {

        if (couleursContours == null) {
        // initialisation de couleursContours qui contient la couleur de chaque niveau
          couleursContours = new Color[levels.length];
          Color[] base = Couleur.getBrighterColors(c,4);

          for(int i=0;i<couleursContours.length;i++)
             couleursContours[i] = base[i%base.length];

        }

        lines= new Ligne[levels.length][];

        // trace des polylignes correspondant aux differents niveaux
   	for (int i=0;i<levels.length;i++) {
   	    drawContour(i);
   	}
   	calque.repaint();
   }
   
   protected boolean isSync() {
      return flagOk && !(error==null && mustAdjustContour);
   }

    /** Nettoyage du plan pour aider le GC
     */
    protected boolean Free() {
       pixels=null;
       orgPixels=null;
       couleursContours=null;
       orgLevels = levels = adjustTab = null;
       contours = null;
       lines= null;
       zoomv=null;
       p=null;
       pimg=null;
       //cAlgo.Free();
       //cAlgo.pc=null;
       cAlgo = null;
       return super.Free();
    }

    protected void useSmoothing(boolean b) {
    	this.useSmoothing = b;
    }

    /** isViewable
     *  @param indice  - indice du niveau
     *  @return false si le contour correspondant est cache, true sinon
     */
    protected boolean isViewable(int indice) {
      if( lines==null ) {
          return true;
      }

      Ligne[] lignes = lines[indice];
      if (lignes.length!=0) {
        Ligne l = lignes[0];
        return !l.hidden;
      }

      return true;
    }

    /** setViewable
     *  methode permettant de cacher ou d'afficher le contour d'indice indLevel
     *  @param indLevel  - indice dans orgLevels du niveau concerne
     *  @param value  - si false, on cache les lignes de ce contour. Si true, on les affiche
     */
    protected void setViewable(int indLevel, boolean value) {
      if( lines==null ) {
          return;
      }
      Ligne[] lignesATraiter = lines[indLevel];
      for (int i=0;i<lignesATraiter.length;i++) {
        Ligne l = lignesATraiter[i];
        l.hidden = !value;
      }
    }

    protected void setPropertie(String prop,String specif,String value) throws Exception {
        if( prop.equalsIgnoreCase("Color") ) {
            Color color = Action.getColor(value);
            if( color==null ) throw new Exception("Syntax error in color function (ex: rgb(30,60,255) )");
            updateColorIfNeeded(color);
            Properties.majProp(this);
            aladin.calque.repaintAll();
        }
        else super.setPropertie(prop,specif,value);
    }

    /**
     * Modifie la couleur du plan
     * @param newColor la nouvelle couleur du PlanContour
     */
    protected void updateColorIfNeeded(Color newColor) {
        if( newColor!=null && ! newColor.equals(this.c)) {

            this.c = newColor;

            Color[] base = Couleur.getBrighterColors(this.c,4);
            // ajustement de la couleur
            for (int i=0;i<this.nbLevels;i++) {
                adjustColor(new Color(base[i%base.length].getRGB()),i);
            }
        }
    }

    /** adjustColor  - met a jour le tableau couleursContours
     *  change la couleur des lignes concernees
     *  @param c  - nouvelle couleur
     *  @param indice  - indice dans orgLevels du niveau concerne
     */
    protected void adjustColor(Color c, int indice) {
      // si la couleur est differente

      if (! couleursContours[indice].equals(c)) {
        couleursContours[indice] = new Color(c.getRGB());

        Ligne[] lignesATraiter = lines[indice];

        for (int i=0;i<lignesATraiter.length;i++) {

          Ligne l = lignesATraiter[i];

          l.couleur = new Color(c.getRGB());

        }
      }
    }

    /** adjustContour  - fixe les parametres
     *  lance le thread pour doAdjustContour
     * @param tab  - tableau des niveaux
     */
    protected void adjustContour(double[] tab) {

        adjustTab = tab;
        mustAdjustContour = true;

        synchronized( this ) {
           runme = new Thread(this,"AladinContourAdjust") {
               
           };
           Util.decreasePriority(Thread.currentThread(), runme);
//           runme.setPriority( Thread.NORM_PRIORITY -1);
           runme.start();
        }
    }

    /** methode appelee par les classes derivees de ContourAlgorithm
     * @param p  - pourcentage du contour courant deja effectue (0<=p<=1)
     */
    protected void updatePourcent(double p) {
        if(mustAdjustContour) return;
        setPourcent( (1-partDessin)*100*((nbLevelsComputed+p)/orgLevels.length) );
    }

   /** Generation du label du plan.
    * Retourne le label en fonction de l'etat courant du plan
    * Il s'agit simplement d'ajouter des "..." quand le plan est en
    * cours de construction
    * @return Le label genere
    */
//   protected String getLabel() {
//      int p = (int)getPourcent();
//      if( p>0 ) return label.substring(0,6)+"..  "+p+"%";
//      return super.getLabel();
//   }

   /** getNextColor
    *  @param calque  - calque reference
    *  @return la prochaine couleur libre
    */
   protected static Color getNextColor(Calque calque) {
      int i,j;

      for( j=0;j<couleursBase.length;j++ ) {

         for( i=0; i<calque.plan.length && (calque.plan[i].type==Plan.NO
              || couleursBase[j]!=calque.plan[i].c); i++);
         if( i==calque.plan.length ) break;

      }

      // si tout est pris
      if( j==couleursBase.length ) {
         icouleursBase++;
         if( icouleursBase==couleursBase.length ) icouleursBase=0;
         j=icouleursBase;
      }

      return couleursBase[j];
   }

   /**
    *
    * @return Retourne les niveaux utilisés pour le contour sous forme d'un tableau d'entiers<br>
    * Si les niveaux sont en train d'etre ajustés, on retourne les nouvelles valeurs
    */
    protected int[] getIntLevels() {
        int[] levels = new int[orgLevels.length];
        for( int i = 0; i < orgLevels.length; i++ ) {
            if( adjustTab!=null ) {
                levels[i] = (int)adjustTab[i];
            }
            else {
                levels[i] = (int)orgLevels[i];
            }
        }

        return levels;
    }

    /** methode privee qui effectue reellement le travail de adjustContour
     * met a jour orgLevels
     * recalcule le contour concerne
     * cette methode DOIT etre synchronized
     * @param level  - nouvelle valeur de niveau
     * @param indice  - indice dans orgLevels du niveau a ajuster
     */
    private synchronized void doAdjustContour(double level, int indice) {
    	aladin.trace(3,"Level adjusted for index "+indice);
        mustAdjustContour = true;
        orgLevels[indice] = level;
        levels[indice] = level;
        adjustLevels();

        PointD[] contourCourant = getContour(levels[indice]);

        contours[indice] = contourCourant;

        Ligne[] linesToDel = lines[indice];

        boolean viewable = true;
        for ( int i=0;i<linesToDel.length;i++ ) {
          Ligne l = linesToDel[i];
          // on teste le caractère viewable sur la première ligne
          if (i==0) viewable = ! l.hidden;
          pcat.delObjet(l);
        }

        drawContour(indice);
        setViewable(indice,viewable);

        calque.repaintAll();
    }

    /** initCouleurs  - initialise le tableau couleursContours
     *  @param couleurs  - tableau des couleurs
     */
    private void initCouleurs(Color[] couleurs) {
      if (couleurs !=null) {
         this.couleursContours = new Color[orgLevels.length];
         for (int i=0;i<this.couleursContours.length;i++) {
            this.couleursContours[i] = couleurs[i];
         }
      }
      else this.couleursContours = null;
    }

    /** fixe la valeur de max */
   /*private void setmax() {
   	int max = 0;
   	for (int i=0;i<pixels.length;i++) {
   	  if (pixels[i]>max) max=pixels[i];
   	}

   	this.max = max;

   }*/

   /** fixe la valeur de min */
   /*private void setmin() {
   	int min = 256;
   	for (int i=0;i<pixels.length;i++) {
   	  if (pixels[i]<min) min=pixels[i];
   	}

   	this.min = min;
   }*/

   /** effectue le lissage afin de faire baisser le nb de points a traiter
    *  @param data  - tableau de pixels de l'image originale (entre 0 et 255)
    *  @param pixels  - tableau de pixels de l'image smoothee (a remplir)
    *  @param orgWidth  - largeur image originale
    *  @param orgHeight  - hauteur image originale
    *  @param width  - largeur image smoothee
    *  @param height  - hauteur image smoothee
    *  @param decalX  - decalage en abscisse dans l'image originale
    *  @param decalY  - decalage en ordonnee dans l'image originale
    *  @param smoothLevel  - taille du carre de base de smoothing
    */
   private void makeSmoothing(short[] data, short[] pix, int width, int height,
                              int orgWidth, int orgHeight, int decalX, int decalY, int smoothLevel) {

             //System.out.println("debut smoothing");
             int x,y,i,j,somme;
             int a,b;
             int nbPixels = smoothLevel*smoothLevel;
             int pos[] = new int[nbPixels]; 	    // tableau des positions
             int p[] = new int[nbPixels];   	    // tableau des valeurs



             // on parcourt l'ensemble de l'image avec un carre de taille smoothingLevel
             for(y=height-1;y>=0;y--) {
                a=y*width;
                for(x=width-1;x>=0;x--)  {

                    b=smoothLevel*y*orgWidth + smoothLevel*x + decalX + decalY*orgWidth;

                    // calcul des elts de pos et de p
                    for(i=smoothLevel-1;i>=0;i--) {
                        pos[i] = b+i;
                        p[i] = data[pos[i]];

                        for(j=smoothLevel-1;j>=1;j--) {
                            int factor = j*smoothLevel;
                            pos[i+factor] = pos[i]+j*orgWidth;
                            p[i+factor] = data[pos[i+factor]];
                        }
                    }

                    // calcul de la somme
                    somme=0;
                    for(i=nbPixels-1;i>=0;i--) somme+=p[i];

                    pix[a+x] = (short)somme;

                }
            }//System.out.println("fin smoothing");
   }

   /** effectue la moyenne glissee */
   private short[] moyenne(short pix[],int width, int height) {
       //System.out.println("debut reduce noise");
       int i,j,a;
       short[] result = new short[(width-1)*(height-1)];
       short p1,p2,p3,p4;

       for(j=height-2;j>=0;j--) {
          a=j*width;
          for(i=width-2;i>=0;i--) {

             p1 = pix[a+i];
             p2 = pix[a+i+1];
             p3 = pix[a+i+width];
             p4 = pix[a+i+width+1];
             result[j*(width-1)+i] = (short) ((p1+p2+p3+p4)/4);

          }
       }
       //System.out.println("fin reduce noise");
       return result;
   }

}

