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

import java.awt.Color;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.SwingUtilities;

import cds.astro.Astropos;
import cds.astro.Astrotime;
import cds.astro.Unit;
import cds.tools.Util;
import cds.xml.TableParser;

/**
 * gestion des plans
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : (dec 2003) Ajout de logs
 * @version 1.1 : (28 mars 00) ReToilettage du code
 * @version 1.0 : (5 mai 99)   Toilettage du code
 * @version 0.91 : Revisite le 23 nov 98
 * @version 0.9 : (??) creation
 */
public class Plan implements Runnable {

   static String NOREDUCTION;

   // Les diff�rents �tats possibles d'un plan
   static final protected int STATUS_UNKNOWN              = 0x0;
   static final protected int STATUS_INPROGRESS           = 0x1;
   static final protected int STATUS_MOREDETAILSAVAILABLE = 0x1<<1;
   static final protected int STATUS_ERROR                = 0x1<<2;
   static final protected int STATUS_EMPTYMOC             = 0x1<<3;
   static final protected int STATUS_OVERFLOW             = 0x1<<4;
   static final protected int STATUS_NOCALIB              = 0x1<<5;
   static final protected int STATUS_EMPTYCAT             = 0x1<<6;
   static final protected int STATUS_LOADING              = 0x1<<7;
   static final protected int STATUS_READY                = 0x1<<8;

   static final private String STATUS[] = { "STATUS_UNKNOWN","STATUS_INPROGRESS","STATUS_MOREDETAILSAVAILABLE",
      "STATUS_ERROR","STATUS_EMPTYMOC","STATUS_OVERFLOW","STATUS_NOCALIB","STATUS_EMPTYCAT","STATUS_LOADING",
   "STATUS_READY" };

   // Les valeurs decrivant les differents types de plan
   static final int NO       = 0;  // Le plan est vide
   static final int IMAGE    = 1;  // Le plan contient une image raster
   static final int IMAGERGB = 2;  // Le plan contient une image RGB
   static final int IMAGEBLINK=3;  // Le plan contient une liste d'images � BLINKER
   static final int IMAGECUBE =4;  // Le plan contient un cube d'images homog�nes
   static final int IMAGERSP = 5;  // Le plan est une image resampl�e
   static final int IMAGEMOSAIC=6; // Le plan est une image mosaique
   static final int IMAGEALGO= 7;  // Le plan est une image g�n�r�e alg�briquement
   static final int CATALOG  = 8;  // Le plan contient des sources issues d'un serveur de donnees
   static final int TOOL     = 9;  // Le plan contient des surcharges graphiques
   static final int APERTURE =10;  // Le plan contient des champs de vue (CCD...)
   static final int FOLDER   =11;  // Le plan est en fait un Folder
   static final int FILTER   =12;  // Le plan contient un filtre
   static final int FOV      =13;  // Le plan contient un field of view (d'une image)
   static final int X        =14;  // Le plan est en cours de creation
   static final int IMAGEHUGE=15;  // Le plan contient un une image tr�s large
   static final int ALLSKYIMG=16;  // Le plan contient les infos pour un background
   static final int ALLSKYPOL=17; // Le plan contient des segments de polarisation
   static final int ALLSKYCAT=18; // Le plan contient des segments de polarisation
   static final int ALLSKYMOC=19; // Le plan contient un Multi-Order Coverage map Healpix
   static final int IMAGECUBERGB =20;  // Le plan contient un cube d'images homog�nes couleurs
   static final int ALLSKYFINDEX=21; // Plan HiPS Finder (de fait pas un vrai plan => voir PlanBG)
   static final int ALLSKYCUBE=22; // Plan HiPS cube

   static String [] Tp       = { "","Image","RGB","Blink","Cube","Resampled","Mosaic","Algo",
      "Catalog",
      "Tool","Aperture","Folder","Filter",
      "Image FoV","In progress","ImageHuge",
      "HipsImage","HipsPolarisation","HipsCatalog",
      "MOC","CubeColor","HipsFinder","HipsCube",
   };

   protected int type;           // Type de plan: NO, IMAGE, CATALOG, TOOL, APERTURE,...
   protected int folder;	     // niveau du folder, 0 si aucun
   protected Slide slide=null;   // Slide pour la pile
   protected boolean isOldPlan;  // True s'il s'agit d'un plan r�utilis� (algo) (voir Plan.planReady());
   protected boolean noBestPlacePost; // true s'il ne faut pas passer le plan � la m�thode bestPlacePost() apr�s son chargement
   protected boolean collapse;	 // true si le plan est collapse dans la pile
   protected String objet;       // Target du plan (celui qui a ete indique a l'interrogation)
   public String label;          // Label du plan; (celui qui apparait dans le "plane stack"
   protected String param;       // Les parametres d'interrogation du serveur
   protected String description=null;
   protected String verboseDescr=null;   // De l'information d�taill�e sur le plan
   protected String ack=null;    // L'acknowledgement
   protected String copyright=null;      // L'origine du plan (mention du copyright)
   protected String copyrightUrl=null;   // Lien vers l'origine ou vers le copyright

   protected double coRadius;      // le rayon du champ de vue demand�e (J2000 deg) => voir allsky
   protected Coord co;           // Les coordonnees J2000 du target de l'interrogation
   // ou null si non encore calcule
   //   protected Thread	sr;	         // Thread pour la resolution Simbad */
   protected Color c;            // La couleur associee au plan
   protected Astrotime epoch;    // Epoque pour catalogue (par d�faut J2000)
   protected Projection projd;   // La projection PAR DEFAUT associee au plan
   protected Projection projInit; // La projection initiale associee au plan
   protected Hashtable projD = null;  // La liste des projections associ�es au plan
   protected FrameHeaderFits headerFits;  // Dans le cas o� il y aurait une ent�te fits associ�e
   private boolean hasSpecificCalib; // true si la calibration astrom�trique n'est pas celle du FITS d'origine
   protected String filename;    // Nom du fichier des donn�es si origine locale, sinon null
   protected Server server;      // Le serveur d'origine
   protected float opacityLevel = 1.0f; // TB, 26/09/2007 : niveau de transparence pour superposition  (ou FoV) sur image
   protected Color colorBackground = null; // Couleur du fond (null si automatique ou Color.white, Color.black)
   protected String startingTaskId = null; // ID de la tache de d�marrage du plan (voir aladin.synchroPlan)

   // thomas
   /** pour les filtres */
   boolean[] influence = new boolean[PlanFilter.LIMIT]; // tableau d'influence des filtres
   protected boolean log=true;   // indique si on doit envoyer un log ou non

   // Filtres pr�d�finis
   protected String filters[]; // Les filtres pr�d�finis
   protected int filterIndex=-1;    // l'indice du filtre pr�d�fin � appliquer, -1 si aucun
   protected PlanFilter planFilter=null;


   // Les parametres qui decrivent l'etat du plan
   boolean    flagOk;          // Vrai si le plan est disponible
   boolean    flagSkip;        // Vrai si le plan doit en fait �tre ignor� (cas d'un MEF dont on saute une extension)
   boolean 	  flagProcessing;  // Vrai si le plan est en cours de modif (affiche un banner en travers des vues qui l'utilise comme r�f.)
   boolean    flagUpdating;    // true si on est entrain de proc�der � une mise � jour du plan qui n'emp�che pas l'affichage (pile qui clignote uniquement)
   boolean    flagWaitTarget;  // Ce plan est bloquant pour le target/radius (voir Command.waitingPlanInProgress)
   boolean    active;          // vrai si le plan est actif (non transparent)
   boolean 	  askActive;	   // vrai si l'utilisateur demande l'activation du plan (si possible)
   boolean    selected;        // vrai si le plan est selectionne
   boolean    isLastVisible;   // vrai si c'est le dernier visible dans la pile (en fct du scroll)
   boolean    underMouse;      // vrai si le plan est actuellement sous la souris
   boolean    ref;             // vrai si c'est le plan de reference pour la projection courante
   int hasPM=-1;               // le plan a du PM : -1 - on ne sait pas encore, 0 - non,  1 - oui
   protected boolean memoClinDoeil; // Vrai si ce plan devra �tre r�activ� si on clique sur l'oeil
   Projection proj[] = new Projection[ViewControl.MAXVIEW];
   // Les projections COURANTES associees au plan
   String     error;           // La chaine d'erreur en cas de probleme
   int        status;          // Code du status
   boolean    flagLocal;       // Le plan est issu d'un fichier local
   protected boolean hasXYorig;   // true si dans le cas d'un plan objet on bloque les xy
   protected boolean recalibrating; // true si on est en train de recalibrer le plan (catalogue)
   protected boolean isSelectable=true; // false si le plan n'a pas d'objets s�lectionnable
   protected boolean doClose=true; // si false, ne pas fermer le flux une fois le plan cr��

   protected double initZoom=1;	// Facteur de zoom par d�faut

   // infos pour l'affectation � une vue d�j� occup�e afin de pouvoir simuler
   // le fonctionnement ancien d'Aladinen MVIEW1
   protected double lastZoomView=0.;
   protected double lastXZoomView;
   protected double lastYZoomView;

   // Quelques stats pour les calculs de perfomances
   long statNbComputing = 0L;       // Nombre de fois ou le calcul de projection a �t� fait
   long statTimeComputing = 0L;     // Temps n�cessaire au dernier calcul de projection effective
   long statTimeDisplay=0L;         // Temps n�cessaire au dernier redisplay
   long statNbItems=0L;             // Nombre d'�l�ments dessin�s

   // Les composantes dependantes du type de plan (on les garde a ce niveau
   // pour eviter les castings intempestifs)
   Pcat pcat;                  // Les objets du plan si CATALOG ou TOOL ou FIELD
   int sourceType=Obj.SQUARE;  // Le type de source dans le cas d'un plan CATALOG ou PlanBGCAT
   boolean fullSource=false;   // Source pleine ou non (colori�e)
   MyInputStream dis=null;     // Le flux de donnees
   //   int streamType=0;           // le type de stream trouv� par MyInputStream

   // Les variables et objets de travail
   Thread runme;               // Pour charger les images et les plans en asynchrone
   URL    u;                   // L'URL qui va nous servir pour la prochaine requete
   //   String plasticID;
   Vector<String> plasticIDs;          // ensemble des ID Plastic pour ce plan

   // Les references aux autres objets
   Aladin aladin;                // Reference a l'objet Aladin

   // Appel� par Chaine directement
   static protected void createChaine(Chaine chaine) {
      NOREDUCTION = chaine.getString("NORED");
   }

   protected Plan() { type=X; aladin=Aladin.aladin; flagOk=false; label=""; startTime = System.currentTimeMillis(); }
   protected Plan(Aladin aladin) { this.aladin=aladin; }

   /** retourne la commande script qui a permit de cr�er le plan */
   private String bookmarkCode=null;
   protected String getBookmarkCode() { return bookmarkCode; }
   protected void setBookmarkCode(String code) { bookmarkCode=code; }

   /** Duplication du Plan */
   protected void copy(Plan p) {
      p.type=type;
      p.folder=folder;
      p.collapse=collapse;
      p.objet=objet;
      p.param=param;
      p.label=label;
      p.description = description;
      p.verboseDescr = verboseDescr;
      p.ack = ack;
      p.copyright=copyright;
      p.copyrightUrl = copyrightUrl;
      p.co=co;
      p.c=c;
      p.projd=projd==null ? null : projd.copy();
      p.projInit=projInit;
      p.projD=projD;
      p.hasSpecificCalib=hasSpecificCalib;
      p.influence = new boolean[influence.length];
      System.arraycopy(influence,0,p.influence,0,influence.length);
      p.log=log;
      p.flagOk=flagOk;
      p.active=active;
      p.askActive=askActive;
      p.selected=selected;
      p.underMouse=underMouse;
      p.ref=ref;
      p.proj = new Projection[proj.length];
      System.arraycopy(proj,0,p.proj,0,proj.length);
      p.error=error;
      p.flagLocal=flagLocal;
      p.hasPM = hasPM;
      p.hasXYorig=hasXYorig;
      p.initZoom=initZoom;
      p.lastZoomView=lastZoomView;
      p.lastXZoomView=lastXZoomView;
      p.lastYZoomView=lastYZoomView;
      p.pcat=pcat;
      p.u=u;
      p.filename = filename;
      p.opacityLevel = opacityLevel;
      p.active=active;
      p.ref=ref;
   }

   public void finalize()  throws Throwable { Free(); }

   /** Retourne true si ce plan contient un SED
    * (on ne test que le premier �l�ment) */
   protected boolean isSED() {
      if( getCounts()==0 ) return false;
      Obj s = iterator().next();
      return s instanceof Source && ((Source)s).leg.isSED();
   }

   // Il s'agit d'un plan qui s'applique en overlay d'une image */
   protected boolean isOverlay() {
      return isCatalog() || isPlanBGOverlay() || this instanceof PlanTool
            || this instanceof PlanField || this instanceof PlanFov || this instanceof PlanFilter;
   }

   /** Retourne true s'il s'agit d'un plan avec pixel */
   protected boolean isPixel() { return isImage() || type==ALLSKYIMG; }

   /** Retourne true s'il s'agit d'un plan image */
   final protected boolean isImage() { return type==IMAGE || type==IMAGERGB || type==IMAGEHUGE
         || type==IMAGEBLINK || type==IMAGECUBE || type==IMAGERSP
         || type==IMAGEALGO || type==IMAGEMOSAIC || type==IMAGECUBERGB; }

   /** Retourne true s'il s'agit d'un plan image "simple" */
   final protected boolean isSimpleImage() { return type==IMAGE || type==IMAGERSP || type==IMAGEALGO
         || type==IMAGEMOSAIC || type==IMAGEHUGE
         ; }

   /** Retourne true s'il s'agit d'un plan qui a des pixels */
   final public boolean hasAvailablePixels() {
      //      return isSimpleImage() ||  type==IMAGEBLINK || type==IMAGECUBE
      //      || this instanceof PlanBG && type==Plan.ALLSKYIMG && ((PlanBG)this).hasOriginalPixels();
      return hasOriginalPixels();
   }

   /** Retourne ture s'il s'agit d'un plan qui a des pixels avec leur valeur d'origine */
   protected boolean hasOriginalPixels() { return false; };

   /** Retourne true s'il s'agit d'un plan qui n'a pas de r�duction astrom�trique */
   final protected boolean hasNoReduction() { return error==Plan.NOREDUCTION /* || !Projection.isOk(projd)*/ ; }

   /** Retourne true si le plan est pr�t */
   protected boolean isReady() { return type!=NO && flagOk && (error==null || hasNoReduction()); }


   /** Retourne la description du statut du plan (souris sur le voyant d'�tat dans la pile) */
   protected String getStackStatus() {
      StringBuilder rep = new StringBuilder();
      if( status!=0 ) {
         int code=1;
         for( int i=1; i<STATUS.length; i++, code<<=1 ) {
            if( (code & status) != 0) {
               if( rep.length()>0 ) rep.append("\n");
               rep.append(aladin.chaine.getString(STATUS[i]));
            }
         }
      }
      if( this instanceof PlanBG ) {
         if( rep.length()>0 ) rep.append("\n");
         rep.append( "HiPS order: "+((PlanBG)this).getInfoDetails() );
      }
      return rep.toString();
   }

   /** Retourne true s'il s'agit d'un plan en erreur
    * => c�d avec un message d'erreur avec l'exception du message d'absence de r�duction
    * astrom�trique */
   public boolean hasError() {
      if( !flagOk ) return false;   // pas encore pr�t
      if( hasNoReduction() && error!=null ) return false;  // Exception
      return error!=null;
   }

   /** Specification de la forme des objet.
    * Permet le changement du type de representation de toutes les sources
    * du plan
    * @param sourceType type de representation (Source.CARRE...)
    * @see aladin.Source
    */
   protected void setSourceType(int sourceType) {
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         Source s = (Source)o;
         s.setSourceType(sourceType);
      }
      this.sourceType=sourceType;
   }

   /** Positionne le flag indiquant que le plan poss�de des objets s�lectionnables, ou non
    * (par d�faut, true) */
   public void setSelectable(boolean flag) { isSelectable=flag; }

   /** True si les objets appartenant au plan sont s�lectionnable */
   public boolean  isSelectable() { return isSelectable; }

   /** retourne true si le plan est vide */
   public boolean isFree() { return type==NO; }

   /** Retourne true si le plan est en erreur ou s'il ne contient aucun objet */
   protected boolean isEmpty() {
      if( type==NO) return false;
      return hasError() || ( (isCatalog() || type==Plan.TOOL) && getCounts()==0);
   }

   /** Retourne true si le plan dispose d'une header Fits */
   protected boolean hasFitsHeader() {
      return headerFits!=null;
      //      if( this instanceof PlanImage   && ((PlanImage)this).headerFits!=null )   return true;
      //      if( this instanceof PlanCatalog && ((PlanCatalog)this).headerFits!=null ) return true;
      //      if( this instanceof PlanFolder  && ((PlanFolder)this).headerFits!=null )  return true;
      //      return false;
   }

   /** retourne true si le plan a des sources */
   protected boolean hasSources() { return isCatalog() && iterator().hasNext(); }

   /** Retourne true si c'est un plan qui a des objets (ex: catalogue) */
   protected boolean hasObj() { return pcat!=null && pcat.hasObj(); }

   /** Retourne true s'il s'agit d'un PlanBG overlay (ex: Polarisation ou PlanBGCat) */
   protected boolean isPlanBGOverlay() {
      return type==ALLSKYPOL || type==ALLSKYCAT;
   }

   /** Retourne true si le point (xImg,yImg) est bien sur un pixel */
   protected boolean isOnPixel(int xImg, int yImg) { return false; }

   /** Il s'agit d'un plan de type catalogue */
   protected boolean isCatalog() {
      return   type==CATALOG
            || type==TOOL && (((PlanTool)this).legPhot!=null || ((PlanTool)this).hasTag())
            || type==ALLSKYCAT;
   }

   /** Retourne true si le plan est un cube */
   protected boolean isCube() { return false; }

   /** Active le frame propre au cube */
   protected void activeCubePixels(ViewSimple v) {}

   /** Retourne la profondeur du plan dans le cas d'un cube (1 sinon) */
   public int getDepth() { return 1; }

   /** retourne la tranche courante (s'il s'agit d'un cube, sinon 0) */
   protected double getZ(ViewSimple v) { return 0; }
   protected double getZ() { return 0; }

   /** Positionne le Frame courant (s'il s'agit d'un cube) */
   protected void setZ(double z) { }

   /** Pr�vu pour les cubes */
   protected byte getPixel8bit(int z,double x,double y) { return 0; }

   /** gestion de la pause pour le d�filement d'un cube */
   protected void setPause(boolean t,ViewSimple v) { }
   protected boolean isPause() { return true; }

   protected int getInitDelay() { return 400; }

   /** Juste pour pouvoir le d�riv�e tranquillement => ne s'applique qu'aux images*/
   synchronized void changeImgID() { }

   /** Juste pour pouvoir le d�riv�e tranquillement => ne s'applique qu'aux cubes*/
   protected boolean setCubeFrame(double frame) { return false; }

   /** Il s'agit d'un plan de type Tools */
   protected boolean isTool() {
      return type==TOOL || type==ALLSKYMOC || type==APERTURE || type==Plan.FOV;
   }

   /** Il s'agit d'un plan catalogue non progressif */
   protected boolean isSimpleCatalog() {
      return type==CATALOG || type==TOOL &&
            ( ((PlanTool)this).legPhot!=null || ((PlanTool)this).legTag!=null);
   }

   /** Retourne true si le plan catalogue peut effacer les sources individuellement */
   protected boolean isSourceRemovable() { return pcat!=null && pcat.removable; }

   /** true si les objets du plan peuvent �tre d�plac�s */
   protected boolean isMovable() { return true; }

   /** Modifie le statut d'un plan catalogue afin de rendre possible la suppression
    * individuelle de sources */
   protected void setSourceRemovable(boolean flag) {
      //      if( !isSimpleCatalog() ) return;
      pcat.removable=flag;
   }

   /** Retourne une chaine de statistiques sur le nombre de sources, filtr�es, s�lectionn�es */
   protected String getStats() throws Exception {
      if( !isCatalog() ) throw new Exception("Not a PlanCatalog");
      int nbSelected=0,nbFiltered=0;
      boolean filter = PlanFilter.allFilters.length>0;
      int nbo=0;
      Iterator<Obj> it = iterator();
      for( nbo=0; it.hasNext(); nbo++ ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         Source s = (Source)o;
         if( s.isSelected() ) nbSelected++;
         if( filter && s.isSelectedInFilter() ) nbFiltered++;
      }
      return nbo+" src"+
      (filter ? "/"+nbFiltered+" filt":"")+
      (nbSelected!=0?"/"+nbSelected+" sel":"");
   }

   protected Legende getFirstLegende() { return null; }
   protected Vector<Legende> getLegende() { return null; }
   protected int getNbTable() { return 0; }
   protected int getCounts() { return 0; }
   protected void reallocObjetCache() { if( pcat!=null ) pcat.reallocObjetCache(); }

   protected Astrotime getEpoch() {
      try {
         if( epoch==null ) epoch = new Astrotime("J2000");
      } catch( ParseException e ) { }
      return epoch;
   }

   /** Positionne une nouvelle epoque, et recalcule les positions de tous les objets
    * en fonction de cette nouvelle epoque */
   protected void setEpoch(String s) throws Exception {
      if( Character.isDigit( s.charAt(0)) ) s = "J"+s;
      if( epoch==null ) epoch = new Astrotime(s);
      else epoch.set(s);
      if( !recomputePosition() ) throw new Exception("Unknown proper motion fields !");
   }

   protected Obj[] getObj() { return new Obj[0]; }

   protected float scalingFactor = 1.0f; // facteur d'�chelle pour l'affichage des filtres (circle et proper motion)

   public float getScalingFactor() {
      return scalingFactor;
   }

   public void setScalingFactor(float scalingFactor) {
      this.scalingFactor = scalingFactor;
   }

   protected Vector<Obj> setMultiSelect(ViewSimple v,RectangleD r) {

      // Le plan n'a pas d'objet s�lectionnable
      if( !isSelectable() ) return new Vector<Obj>();

      // objets ne sont pas projetable dans cette vue (ACCELERATION POUR PLAN SIMPLE)
      if( !(this instanceof PlanBG) && pcat!=null && !pcat.isDrawnInSimpleView(v.n) ) return new Vector<Obj>();

      Vector<Obj> res = new Vector<Obj>(5000);

      Iterator<Obj> it = iterator(v);
      while( it!=null && it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Position) ) continue;
         Position p = (Position)o;
         if( p.plan.type==Plan.FOV || p instanceof Forme) continue;

         // on ne s�lectionne que les sources "filtr�es"
         if( p.inRectangle(v,r) &&
               ( !( p instanceof Source) || ((Source)p).noFilterInfluence() || ((Source)p).isSelectedInFilter() ) ) {
            p.setSelect(true);
            res.addElement(p);
         }
      }
      return res;
   }

   /** Test d'appartenance.
    * Retourne tous les objets qui contiennent (x,y).
    * Dans le cas d'un plan FIELD, il suffit qu'un seul objet contienne
    * (x,y) pour que tous les objets du plan soient pris. Sauf s'il s'agit du centre
    * de rotation, dans ce cas, il y a m�morisation du plan concern�
    * Dans le cas d'un polygone, si on a cliqu� � l'int�rieur de la surface, on s�lectionne �galement tous les points de controle
    * @param x,y Position de la souris (coordonnees Image)
    * @return Le vecteur des objets qui contient le point
    */
   protected Vector<Obj> getObjWith(ViewSimple v,double x, double y) {
      int i;

      // Le plan n'a pas d'objet s�lectionnable
      if( !isSelectable() ) return new Vector<Obj>();

      // objets ne sont pas projetable dans cette vue
      if( pcat!=null && !pcat.isDrawnInSimpleView(v.n) ) return new Vector<Obj>(1);

      Vector<Obj> res = new Vector<Obj>(500);

      Iterator<Obj> it = iterator(v);
      if( it==null ) return new Vector<Obj>(1);

      if( type==Plan.APERTURE ) {
         for( i=0; it.hasNext() ; i++ ) {
            Obj o = it.next();
            if( o.in(v,x,y) ) {
               if( i==0 && Aladin.ROTATEFOVCENTER
                     && ((PlanField)this).isRollable() && ((PlanField)this).isCenterRollable() ) {
                  aladin.calque.planRotCenter=((Repere)o).plan;
                  res.addElement(o);
               } else {
                  aladin.calque.planRotCenter=null;
                  Iterator<Obj> it1 = iterator(v);
                  while( it1.hasNext() ) res.addElement(it1.next());
               }
               break;
            }
         }

      } else {
         Vector<Ligne> vo = new Vector<Ligne>();
         while( it.hasNext() ) {
            Obj o = it.next();

            // On m�morise les points de controles d'un polygone si on a cliqu� dessus
            if( o instanceof Ligne && ((Ligne)o).inPolygon(v, (int)x, (int)y) ) vo.addElement((Ligne)o);
            boolean in = o instanceof Cercle ? o.in(v,x,y) : o.inside(v,x,y);
            if( in && ( !(o instanceof Source) ||
                  ((Source)o).noFilterInfluence() || ((Source)o).isSelectedInFilter() ) ) {
               res.addElement(o);
            }
         }

         // On s�lectionne tous les points de controle du polygone dans lequel on a cliqu� (sur la surface)
         for( Ligne o : vo ) {
            for( o=o.getFirstBout(); o!=null; o = o.finligne ) {
               if( !res.contains(o) ) res.addElement(o);
            }
         }
      }

      return res;
   }

   /** Retourne true si le plan a des champs indiquant un mouvement Propre */
   public boolean hasPM() {
      if( !flagOk || !isCatalog() ) return false;
      if( hasPM<0 ) {
         Vector<Legende> legs = getLegende();
         if( legs==null ) return false;
         Iterator<Legende> it = legs.iterator();
         while( it.hasNext() ) {
            Legende leg = it.next();
            if( leg==null ) continue;
            if( leg.getPmRa()>0 &&  leg.getPmDe()>0 ) { hasPM=1; return true; }
         }
         hasPM=0;
         return false;
      }
      return hasPM==1;
   }

   /** Recalcule toutes les positions internes
    * @return true si au moins une position a �t� effectivement modifi�
    */
   public boolean recomputePosition() {
      boolean rep=false;
      Vector<Legende> legs = getLegende();
      if( legs==null ) return false;
      //       aladin.trace(3,label+": reprocessing all internal coordinates...");
      Iterator<Legende> it = legs.iterator();
      while( it.hasNext() ) {
         Legende leg = it.next();
         int npmra = leg.getPmRa();
         int npmde = leg.getPmDe();
         if( npmra<=0 || npmde<=0 ) continue;  // Inutile, pas de PM
         int nra   = leg.getRa();
         int nde   = leg.getDe();
         recomputePosition(iterator(),leg,nra,nde,npmra,npmde);
         rep=true;
      }
      if( rep ) aladin.view.newView(1);

      return rep;
   }

   /** recalcule les positions internes de toutes les sources ayant la l�gende indiqu� */
   public void recomputePosition(Iterator<Obj> it,Legende leg, int nra,int ndec,int npmra,int npmde) {
      double epoch = getEpoch().getJyr();
      int format = TableParser.FMT_UNKNOWN;
      int nError=0;

      double J2000=Double.NaN;
      try { J2000 = (new Astrotime("J2000")).getJyr(); }
      catch( Exception e ) {}

      //       boolean first=true;

      Astropos c = new Astropos();
      while( it.hasNext() ) {
         try {
            Source s = (Source)it.next();
            if( s.leg!=leg ) continue;
            String ra = s.getValue(nra);
            String dec= s.getValue(ndec);
            format = TableParser.getRaDec(c, ra, dec, format);
            //             if( first ) System.out.println("c="+c);
            if( npmra>0 && npmde>0 ) {
               try {
                  Unit mu1 = new Unit();
                  try {
                     mu1.setUnit(s.getUnit(npmra));
                     mu1.setValue(s.getValue(npmra));
                  } catch( Exception e1 ) { e1.printStackTrace(); }
                  Unit mu2 = new Unit();
                  try {
                     mu2.setUnit(s.getUnit(npmde));
                     mu2.setValue(s.getValue(npmde));
                  } catch( Exception e1 ) { e1.printStackTrace();  }
                  if( mu1.getValue()!=0 || mu2.getValue()!=0 ) {
                     try {
                        mu1.convertTo(new Unit("mas/yr"));
                     } catch( Exception e) {
                        // Il faut reinitialiser parce que mu1 a chang� d'unit� malgr� l'�chec !
                        mu1.setUnit(s.getUnit(npmra));
                        mu1.setValue(s.getValue(npmra));
                        mu1.convertTo(new Unit("ms/yr"));
                        double v = 15*mu1.getValue()*Math.cos(c.getLat()*Math.PI/180);
                        mu1 = new Unit(v+"mas/yr");
                     };

                     double pmra = mu1.getValue();
                     //                   if( first ) System.out.println("pmra="+s1+" => mu1="+mu1+" => val="+pmra);
                     mu2.convertTo(new Unit("mas/yr"));
                     double pmde = mu2.getValue();
                     //                   if( first ) System.out.println("pmde="+s1+" => mu2="+mu2+" => val="+pmde);

                     c.set(c.getLon(),c.getLat(),J2000,pmra,pmde);
                     //                   if( first ) System.out.println("set c : "+c);
                     c.toEpoch(epoch);
                     //                   if( first ) System.out.println("set epoch="+epoch+" : "+c);
                     //                   if( pmra!=0 || pmde!=0 ) first=false;
                  }
               } catch( Exception e ) {
                  nError++;
                  if( nError>100 ) {
                     if( aladin.levelTrace>=3 ) e.printStackTrace();
                     aladin.warning("Too many error during proper motion computation !\n"
                           + e.getMessage());
                     break;
                  }
               }
            }

            s.raj = c.getLon();
            s.dej = c.getLat();

         } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
      }

   }

   /** Modification des champs utilis�s pour la position c�leste */
   public void modifyRaDecField(Legende leg, int nra,int ndec,int npmra,int npmde) {
      aladin.trace(3,label+" new J2000 => RA pos="+(nra+1)+" DE pos="+(ndec+1)
            +" PMRA pos="+(npmra+1)+" PMDE pos="+(npmde+1));

      recomputePosition(iterator(),leg,nra,ndec,npmra,npmde);

      if( hasXYorig ) {
         hasXYorig=false;
         error=null;
      }

      aladin.view.newView(1);
      aladin.view.repaintAll();

      String s = "New J2000 fields for "+label+"\n=> RA column "+(nra+1)+" -  DE column "+(ndec+1)
            +(npmra>0 ? " -  PMRA column "+(npmra+1):"")
            +(npmde>0 ? " -  PMDEC column "+(npmde+1):"");
      aladin.trace(2,s);
      aladin.info(aladin,s);
   }

   /** Modification des champs utilis�s pour la position en XY */
   public void modifyXYField(Legende leg, int nx,int ny) {

      aladin.trace(3,label+" new XY coordinate fields => X pos="+(nx+1)+" Y pos="+(ny+1));
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         try {
            Source s = (Source)it.next();
            if( s.leg!=leg ) continue;
            s.x = Double.parseDouble( s.getValue(nx) );
            s.y = Double.parseDouble( s.getValue(ny) );
         } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
      }

      if( !hasXYorig ) {
         hasXYorig=true;
         error=Plan.NOREDUCTION;
      }

      aladin.view.newView(1);
      aladin.view.repaintAll();

      aladin.info(aladin,"New XY fields for "+label+"\n=> X column "+(nx+1)+" -  Y column "+(ny+1) );
   }





   /** M�morisation des informations de zoom issues de la vue v
    *  afin de pouvoir reg�n�rer une vue sur le plan ayant le m�me zoom
    */
   protected void memoInfoZoom(ViewSimple v) {
      lastZoomView=v.zoom; lastXZoomView=v.xzoomView; lastYZoomView=v.yzoomView;
   }

   /** Affectation a la vue v des infos zoom m�moris�es dans le plan
    *  @return true si cela a �t� fait, false si pas possible
    */
   protected boolean initInfoZoom(ViewSimple v) {
      if( lastZoomView==0. ) return false;
      v.zoom=lastZoomView; v.xzoomView=lastXZoomView; v.yzoomView=lastYZoomView;
      return true;
   }

   /** Test d'equivalence de plan.
    * Retourne vrai si les parametres decrivent le meme plan
    * @param type  Le typ du plan
    * @param objet l'objet ou les coordonnees au centre
    * @param param les parametres de description du plan (ex: SERC J MAMA)
    * @param other dependant du type de plan, sinon null
    * 		  pour IMAGE: concatenation "code_fmt/code_resol"
    * @return <I>true</I> si ok, <I>false</I> sinon.
    */
   protected boolean theSame(int type,String objet,String param) {
      return theSame(type,objet,param,null);
   }
   protected boolean theSame(int type,String objet,String param,String other) {
      if( type!=this.type ) return false;
      if( !egale(this.objet,objet) ) return false;
      if( !egale(this.param,param) ) return false;
      if( other!=null ) {
         String s="";
         switch(type) {
            case IMAGE:
            case IMAGEHUGE:
               PlanImage p=(PlanImage)this;
               s=p.fmt+"/"+p.res; break;
         }
         if( !other.equals(s) ) return false;
      }
      return true;
   }

   private boolean egale(String s,String t) {
      if( s==t ) return true;
      if( s==null && t==null || s!=null && t!=null && s.equals(t) ) return true;
      return false;
   }

   /** Test d'equivalence de plan.
    * Retourne vrai si il s'agit du meme plan dans le meme etat
    * @param p le plan a comparer avec le plan courant
    * @return <I>true</I> si ok, <I>false</I> sinon.
    */
   protected boolean equals(Plan p ) {

      // Meme plan ?
      if( !theSame(p.type,p.objet,p.param) ) return false;

      // Resolution et format identique pour les plans images ?
      if( this instanceof PlanImage && p instanceof PlanImage ) {
         if( !PlanImage.sameFmtRes(this,p) ) return false;
      }

      // Meme etat ?
      if( (error==null && p.error!=null) || (error!=null && p.error==null) ) return false;
      if( error!=null && p.error!=null   &&!error.equals(p.error) ) return false;
      if( flagOk!=p.flagOk ) return false;
      if( projd!=p.projd ) return false;
      return true;
   }

   double pourcent=-1;	// Pourcentage du chargement de l'image

   /**
    * Retourne le pourcentage charge de l'image chargee
    *  -1 si non encore en chargement
    *   0 si en preambule de chargement
    * 100 si totalement chargee
    */
   protected double getPourcent() { return pourcent; }

   protected void setPourcent(double x) { pourcent=x; }

   /**
    * Retourne un debit en octets/s en unites plus raisonnable
    */
   static protected String throughput(double deb) {
      String u="B";
      if( deb>1024 ) { deb/=1024; u="KB"; }
      if( deb>1024 ) { deb/=1024; u="MB"; }
      return Util.myRound(deb+"",2)+" "+u+"/s";
   }

   /**
    * Envoi d'un log si autorise
    * @param s la chaine a loguer
    */
   protected void sendLog(String id,String s) {
      s=s.replace('\n',' ');
      s=s.replace('\r',' ');
      if( log ) aladin.log(id,s);
   }

   /**
    * Indique si on doit envoyer d'eventuels logs ou non ?
    * @param mode true si on doit envoyer un log, false sinon
    */
   void setLogMode(boolean mode) { log=mode; }

   /** Libere le plan.
    * cad met toutes ses variables a <I>null</I> ou a <I>false</I>
    */
   protected boolean Free() {
      setLogMode(false);

      // Tentatite d'arr�t d'un flux en cours
      // JE NE LE FAIS PAS POUR LES PLANS ISSUS DU SERVEUR ALADIN, PARCE QUE CA PEUT
      // TOUT PLANTER
      try { if( dis!=null &&
            (server==null || server!=aladin.dialog.server[ServerDialog.ALADIN]) ) dis.close(); }
      catch( Exception e ) { }

      // Mise � jour des formulaires serveurs (Choice input)
      if( aladin.dialog!=null )
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { aladin.dialog.resume(); }
         });

      // Suppression de la fenetre des proprietes associee au plan si necessaire
      final Plan [] param1 = new Plan[1];
      param1[0]=this;
      SwingUtilities.invokeLater(new Runnable() {
         public void run() { Properties.disposeProperties(param1[0]); }
      });

      if( aladin.frameCM!=null )
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { aladin.frameCM.disposeFrameCM(param1[0]); }
         });

      if( aladin.frameNewCalib!=null && aladin.frameNewCalib.plan==this )
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { aladin.frameNewCalib.hide(); }
         });

      if( server!=null ) {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { if( server!=null ) server.setStatus(); server=null; }
         });
      }

      if( pcat!=null ) pcat.free();
      if( headerFits!=null ) headerFits.free();
      init();

      return true;
   }

   /** Initialise toutes les variables */
   protected void init() {
      type = NO;
      hasPM=-1;
      flagOk=false;
      flagWaitTarget=isOldPlan=false;
      hasSpecificCalib=false;
      objet=error=label=param=description=verboseDescr=ack=copyright=copyrightUrl=null;
      flagSkip=false;
      co=null;
      projd=projInit = null;
      projD=null;
      pcat = null;
      headerFits=null;
      memoClinDoeil=collapse=flagOk=selected=active=ref
            =hasXYorig=underMouse=recalibrating = false;
      resetProj();
      folder=0;
      initZoom=1;
      filename=null;
      dis=null;
      server=null;
      filters=null;
      slide=null;
   }

   /** Bloque ou nom le mode de calcul des x,y des objets.
    * @param lock <I>true</I> les objets gardent leurs x,y, sinon <I>false</I>
    */
   protected void setXYorig(boolean lock) {
      hasXYorig = lock;
   }

   /** Retourne l'etat du plan tool (locke ou non).
    * @return <I>true</I> les objets gardent leurs x,y, sinon <I>false</I>
    */
   protected boolean hasXYorig() { return hasXYorig; }

   /**
    * G�n�re des XY "natifs" en fonction de la projection courante
    * en vue d'une recalibration (si besoin est en fonction du flag xyLock)
    * ATTENTION NE MARCHE QUE POUR LES PLANS QUI N'ONT QUE DES OBJETS POSITIONS
    */
   protected void setXYorig() {
      Aladin.trace(3,"create original XY from RA,DEC for plane "+this);
      Iterator<Obj> it = pcat.iterator();
      while( it.hasNext() ) ((Position)it.next()).setXY(projd);
      hasXYorig=true;
   }

   /** Retourne la description du filtre actif */
   protected String getFilterDescription() {
      if( filterIndex<0 || filters==null ) return "no dedicated filter";
      return Server.getFilterDescription(filters[filterIndex]);
   }

   /** Retourne le filtre pr�d�fini associ� au plan (UN SEUL POUR L4INSTANT),
    * null si aucun */
   protected PlanFilter getFilter() { return planFilter; }

   /** Positionnement d'un filtre pr�d�fini en passant une sous chaine de son nom */
   protected void setFilter(String s) {
      if( filters==null ) return;
      int i = findFilter(s);
      setFilter(i);

      //      for( int i=0; i<filters.length; i++ ) {
      //         String fs = Server.getFilterDescription(filters[i]);
      //         if( fs!=null ) {
      //            if( Util.indexOfIgnoreCase(fs,s)>=0 ) { setFilter(i); return; }
      //         }
      //         fs = Server.getFilterName(filters[i]);
      //         if( fs!=null ) {
      //            if( Util.indexOfIgnoreCase(fs,s)>=0 ) { setFilter(i); return; }
      //         }
      //      }
      //      setFilter(-1);
   }

   /** Retourne l'indice du filter, ou -1 si non trouv� */
   private int findFilter(String s ) {
      if( filters==null ) return -1;
      for( int i=0; i<filters.length; i++ ) {
         String fs = Server.getFilterDescription(filters[i]);
         if( fs!=null ) {
            if( Util.indexOfIgnoreCase(fs,s)>=0 ) return i;
         }
         fs = Server.getFilterName(filters[i]);
         if( fs!=null ) {
            if( Util.indexOfIgnoreCase(fs,s)>=0 ) return i;
         }
      }
      return -1;
   }

   /** Ajout d'un filtre d�di� (un par un via une balise INFO dans VOTable) */
   protected void addFilter(String filter) {
      String tmp[];
      int i=findFilter(filter);
      if( i>=0 ) {
         filters[i] = filter;
         aladin.trace(4,"Plan.addFilter(): replace:"+ Server.getFilterName(filter));
      }
      else {
         if( filters==null ) tmp = new String[1];
         else {tmp = new String[filters.length+1]; System.arraycopy(filters, 0, tmp, 0, filters.length); }
         tmp[tmp.length-1]=filter;
         filters=tmp;
         aladin.trace(4,"Plan.addFilter(): add:"+ Server.getFilterName(filter));
      }
      if( filterIndex==-1 ) filterIndex=aladin.configuration.getFilter();
   }

   /**
    * Postionnnement du filtre pr�d�fini
    * ATTENTION: actuellement on ne peut positionner qu'un seul filtre � la fois
    * @param n indice du filtre � cr�er (dans filters[]) -1 si aucun
    */
   protected void setFilter(int n) {

      if( filters==null ) return;
      filterIndex = n;

      // JE PREFERERAIS UTILISER LE active DU FILTRE, MAIS JE N'Y PARVIENS PAS
      // A VOIR AVEC THOMAS
      if( n<0 ) {
         planFilter=null;
         planFilter.updateAllFilters(aladin);
         aladin.calque.repaintAll();
         return;
      }

      planFilter.updateAllFilters(aladin);
      String name = Server.getFilterName(filters[n]);
      String script = Server.getFilterScript(filters[n]);

      if( planFilter==null ) {
         planFilter = new PlanFilter(aladin,name,script,this);
         planFilter.applyFilter();
         PlanFilter.updateAllFilters(aladin);
         //System.out.println("XXXXXXX Creation du filtre "+name+" ["+script+"]");
      }
      else {
         planFilter.updateDefinition(script,name,null);
         //System.out.println("XXXXXXX Application du filtre "+name+" ["+script+"]");
      }
   }

   protected void updateDedicatedFilter() {
      if( planFilter==null ) return;
      PlanFilter.updateNow();
   }

   /** Transforme le filtre d�di� courant en filtre g�n�rique */
   protected void toGenericalFilter() {
      if( filterIndex<0 ) return;
      int i=filterIndex;
      setFilter(-1);
      PlanFilter p = aladin.command.createFilter(Server.getFilter(filters[i]));
      if( p!=null ) Properties.createProperties(p);
   }

   /** Retourne true si le plan a un filtre d�di� s�lectionn� */
   protected boolean hasDedicatedFilter() { return filterIndex>=0; }

   /**
    * D�termination des minx,maxx, miny, maxy respectivement m�moris�s
    * dans m[0],m[1],m[2] et m[3]
    * ATTENTION NE MARCHE QUE POUR LES PLANS QUI N'ONT QUE DES OBJETS POSITIONS
    */
   protected void getXYRange(double m[]) {
      m[0]=m[2]=Double.POSITIVE_INFINITY;
      m[1]=m[3]=Double.NEGATIVE_INFINITY;
      Iterator<Obj> it = pcat.iterator();
      while( it.hasNext() ) {
         Position a = (Position)it.next();
         if( a.x<m[0] ) m[0]=a.x;
         if( a.x>m[1] ) m[1]=a.x;
         if( a.y<m[2] ) m[2]=a.y;
         if( a.y>m[3] ) m[3]=a.y;
      }
   }

   /** Modification d'une projection (par le nom de projection) */
   protected void modifyProj(String projName) {
      int t = Projection.getProjType(projName);
      Projection p = projd;
      if( p==null || t==-1 ) return;
      modifyProj(null,Projection.SIMPLE,p.alphai,p.deltai,p.rm1,p.cx,p.cy,p.r1,p.rot,p.sym,t,p.system);
      aladin.view.newView(1);
      aladin.calque.repaintAll();
   }

   /** Modification d'une projection */
   protected void modifyProj(String label,int modeCalib,
         double alphai, double deltai, double rm,
         double cx, double cy,double r,
         double rot,boolean sym,int t,int system) {
      projd.modify(label,modeCalib,alphai,deltai,rm,rm,cx,cy,r,r,rot,sym,t,system);
      syncProjLocal();
   }


   /** Mise � jour des projections adapt�es � chaque vue (pour les planBG) */
   protected void syncProjLocal() { }

   /** Force le recalcul de la projection n */
   protected void resetProj(int n) { proj[n]=null; }

   /** Force le recalcul de toutes les projections */
   protected void resetProj() {
      for( int i=0; i<ViewControl.MAXVIEW; i++ ) resetProj(i);
   }

   /** Positionne le flag indiquant que la calibration de ce plan n'est plus
    *  celle d'origine (fichier FITS WCS)
    */
   protected void setHasSpecificCalib() { hasSpecificCalib=true; }

   /** Retourne true si la calib du plan n'est pas celle d'origine FITS */
   protected boolean hasSpecificCalib() { return hasSpecificCalib; }

   /** Memorisation d'une nouvelle projection possible pour le plan
    * qui devient la projection par defaut (projd)
    * Rq: mise a jour de projD
    * @param name le nom de la projection (il doit etre unique pour le plan
    * @param p la nouvelle projection
    */
   protected void setNewProjD(Projection p) {
      if( projD==null ) { projD = new Hashtable(); projInit=p; }
      projD.put(p.label,p);
      projd=p;

      // Suppression du message NOREDUCTION si necessaire
      if( error!=null && hasNoReduction() ) error=null;
   }

   /** Determination de tous les plans de reference possibles pour le plan
    * @return le tableau des plans de reference possibles
    */
   protected Plan [] getAvailablePlanRef() {
      Vector<Plan> v = new Vector<Plan>();
      if( isImage() || this instanceof PlanBG /* type==IMAGE || type==IMAGEHUGE */ ) v.addElement(this);
      else {
         Plan [] allPlan = aladin.calque.getPlans();
         for( int i=0;i<allPlan.length; i++ ) {
            Plan p = allPlan[i];
            if( !p.isImage() && !p.isCatalog() ) continue;
            if( !aladin.calque.canBeRef(p) || !p.flagOk  ) continue;
            if( !Projection.isOk(projd) || !projd.agree(p.projd,null) ) continue;
            v.addElement(p);
         }
      }
      int n=v.size();
      Plan pRef[] = new Plan[n];
      Enumeration<Plan> e = v.elements();
      for( int i=0; i<n; i++ ) pRef[i]=e.nextElement();

      return pRef;
   }

   /** Determination de toutes les projections par defaut utilisable pour
    * le plan
    * @return le tableau des projection
    */
   protected Projection [] getAvailableProj() {
      Projection p;
      int i;
      Vector v = new Vector();
      Enumeration e;

      if( projD!=null ) {
         e = projD.keys();
         while( e.hasMoreElements() ) {
            p = (Projection)projD.get(e.nextElement());
            v.addElement(p);
         }
      }
      int n=v.size();
      Projection projs[] = new Projection[n];
      e = v.elements();
      for( i=0; i<n; i++ ) projs[i]=(Projection)e.nextElement();

      return projs;
   }


   /** V�rifie que le plan n'est pas de r�f�rence pour une vue visible actuellement */
   protected boolean isRefForVisibleView() {
      try {
         for( int i=aladin.view.getModeView()-1; i>=0; i-- ) {
            ViewSimple v = aladin.view.viewSimple[i];
            if( v.pref==this ) { setDebugFlag(REFFORVISIBLEVIEW,true); return true; }
         }
         setDebugFlag(REFFORVISIBLEVIEW,false);
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return false;
   }

   // Diff�rents �tats possibles du plan (pour d�bugging)
   static final int REFFORVISIBLEVIEW = 0x1;
   static final int CANBETRANSP       = 0x1<<1;
   static final int OUTOFVIEW         = 0x1<<2;
   static final int VIEWABLE          = 0x1<<3;
   static final int ACTIVATED         = 0x1<<4;
   static final int ASKACTIVATED      = 0x1<<5;
   static final int REF               = 0x1<<6;
   static final int FLAGOK            = 0x1<<7;
   static final int FLAGPROCESSING    = 0x1<<8;
   static final int FLAGUPDATING      = 0x1<<9;
   static final int SELECTED          = 0x1<<10;
   static final int UNDERIMG          = 0x1<<11;
   static final int UNDERBKGD         = 0x1<<12;

   static final String DEBUGFLAG[] = { "","RefForVisibleView","CanBeTransp","OutOfView","Viewable","Activated",
      "AskActivated","Ref","FlagOk","FlagProcessing","FlagUpdating","Selected","UnderImg","UnderBkg"};

   private int debugFlag = 0x0;   // flag d'�tat du plan pour d�bugging

   /** Retourne true si l'�tat CANBETRANSP a �t� positionn� sur le plan
    * (�vite de refaire tous les calculs juste pour activer le menu Aladin.TRANSPON) */
   protected boolean hasCanBeTranspState() { return (debugFlag & CANBETRANSP) == CANBETRANSP; }

   /** Retourne true si l'�tat UNDERIMG|UNDERBKGD a �t� positionn� sur le plan */
   protected boolean isUnderImg() { return (debugFlag & (UNDERIMG|UNDERBKGD)) !=0; }
   protected boolean isUnderImgBkgd() { return (debugFlag & UNDERBKGD) !=0; }

   private boolean hasCheckBox=false;
   protected void setHasCheckBox(boolean flag) { hasCheckBox=flag; }
   protected boolean hasCheckBox() { return hasCheckBox; }


   /** positionnement d'un flag de d�bugging du plan */
   protected void setDebugFlag(int type,boolean flag) {
      if( flag ) debugFlag |= type;
      else debugFlag &= ~type;
   }

   /** r�cup�ration d'une chaine d�crivant l'�tat du plan (pour d�bugging) */
   protected String getDebugFlag() {
      int mask=0x1;
      StringBuffer rep= new StringBuffer();

      setDebugFlag(ACTIVATED,active);
      setDebugFlag(ASKACTIVATED,askActive);
      setDebugFlag(REF,ref);
      setDebugFlag(FLAGOK,flagOk);
      setDebugFlag(FLAGPROCESSING,flagProcessing);
      setDebugFlag(FLAGUPDATING,flagUpdating);
      setDebugFlag(SELECTED,selected);

      for( int i=1; i<32; i++ ) {
         if( (mask & debugFlag) == mask ) {
            if( rep.length()>0 ) rep.append(", ");
            rep.append(DEBUGFLAG[i]);
         }
         mask = mask<<1;
      }
      return rep.toString();
   }

   /** Retourne vrai si le plan peut etre visible dans au moins une des vues visibles
    * @return <I>true</I> si ok, <I>false</I> sinon.
    */
   protected boolean isViewable() {
      if( hasXYorig || isSED() ) { setDebugFlag(VIEWABLE,true); return true; }
      if( type==NO || type==X || !flagOk ) { setDebugFlag(VIEWABLE,false); return false; }
      if( !isCatalog() && !isPlanBGOverlay()
            && !(isImage() || type==ALLSKYIMG) ) { setDebugFlag(VIEWABLE,true); return true; }

      // Parcours de l'ensemble des vues visibles
      for( int i=aladin.view.getModeView()-1; i>=0; i-- ) {
         ViewSimple v = aladin.view.viewSimple[i];
         if( v.isFree() ) continue;

         // Sans astrom�trie, il suffit que le plan soit en r�f�rence
         // pour au moins une vue visible
         if( projd==null ) {
            if( v.pref==this ) { setDebugFlag(VIEWABLE,true); return true; }

            // Sinon il faut que le plan soit compatible avec au-moins
            // une vue visible
         } else {
            if( isCompatibleWith(v) ) { setDebugFlag(VIEWABLE,true); return true; }
         }
      }

      setDebugFlag(VIEWABLE,false);
      return false;
   }

   /** retourne vrai s'il s'agit d'un plan overlay qui peut �tre vu en transparence sur au-moins un autre plan
    * qui se trouve en dessous de lui dans la pile */
   //   protected boolean isOverlayable() {
   //      setDebugFlag(OVERLAYABLE,false);
   //
   //
   //      if( !isReady() ) return false;
   //      if( !isOverlay() ) return false;
   //      int n = aladin.calque.getIndex(this);
   //
   //      Plan [] p = aladin.calque.getPlans();
   //      for( int i=n+1; i<p.length; i++ ) {
   //         if( !p[i].isReady() ) continue;
   //         if( p[i] instanceof PlanBG || isCompatibleWith(p[i]) ) {
   //            setDebugFlag(OVERLAYABLE,true);
   //            return true;
   //         }
   //      }
   //      return false;
   //   }

   /** Ce plan devrait avoir une checkbox afin de pouvoir le s�lectionner dans la pile
    * ==> D�pend des autres plans pr�sents dans la pile */
   protected boolean shouldHaveARefCheckBox() {

      if( !isReady() ) return false;
      if( ref ) return true;

      //      // Un plan pris en r�f�rence quel qu'il soit doit avoir une coche
      //      // s'il n'est pas tout seul dans la pile
      //      if( ref && aladin.calque.getNbUsedPlans()>1 ) return true;

      boolean isImage = isImage();
      boolean isCatalog = isSimpleCatalog();
      boolean isImgBg = type==ALLSKYIMG;
      boolean isCatBg = isPlanBGOverlay();

      // r�ponse par d�faut
      boolean rep = isCatalog ? true : false;

      int n = aladin.calque.getIndex(this);

      Plan [] p = aladin.calque.getPlans();
      for( int i=0; i<p.length; i++ ) {
         if( !p[i].isReady() || p[i]==this ) continue;

         // pour une image, la checkbox doit appara�tre...
         if( isImage ) {
            // s'il y a au-moins un catalogue dont la projection n'est pas compatible
            if( p[i].isSimpleCatalog() && !isCompatibleWith(p[i])
                  // Ou qu'il y a une autre image, ou un allsky image
                  || p[i].isImage() || p[i].type==ALLSKYIMG ) return true;
         }

         // pour un catalogue...
         if( isCatalog ) {
            // si j'ai une image ou allskyimg compatible , c'est inutile
            if( p[i].isImage() && isCompatibleWith(p[i]) || p[i] instanceof PlanBG  ) return false;

            // Si j'ai un catalogue situ� en dessous compatible, c'est inutile
            if( p[i].isSimpleCatalog() && isCompatibleWith(p[i]) && i>n ) return false;
         }

         // Pour un allsky image...
         if( isImgBg ) {
            // Il suffit d'un autre plan BG image ou d'une image
            if( p[i].isImage() || p[i].type==ALLSKYIMG ) return true;
         }

         // Et ce n'est jamais le cas pour un allsky cat
      }
      return rep;
   }


   /** Retourne true si la coordonn�e est dans le plan */
   protected boolean contains(Coord c) { return false; }

   /** Positionne comme label du plan le nom trouv� dans le mot cl� HDUNAME ou EXTNAME
    * et suffixe ce nom par le num�ro de l'extension
    * @return true si la substitution a pu �tre op�r�e, sinon false
    */
   protected boolean setExtName() {
      try {
         if( headerFits!=null ) {
            String s;
            try {  s = headerFits.getStringFromHeader("HDUNAME"); }
            catch( Exception e ) { s = headerFits.getStringFromHeader("EXTNAME"); }
            s=s.trim();
            if( s.length()!=0 ) {
               int pos;
               if( (pos=label.lastIndexOf('['))>=0 ) s=s+label.substring(pos);
               setLabel(s);
            }
         }
      } catch( Exception e ) { return false; }
      return true;
   }

   /** Retourne true si le plan est synchronis� */
   protected boolean isSync() { return true; }

   /**
    * Retourne true si le plan pass� en param�tre couvre une r�gion du ciel
    * superposable
    */
   protected boolean isCompatibleWith(Plan p) {
      return Projection.isOk(p.projd) && p.projd.agree(projd,aladin.view.getCurrentView());
   }

   /** Retourne true si le plan pass� est susceptible d'�tre visible en partie dans la vue indiqu�e */
   protected boolean isCompatibleWith(ViewSimple v) {
      if( v.isFree() || !Projection.isOk(v.getProj()) ) return false;
      if( v.pref==this ) {
         setUnderBackGroundFlag(v);   // Faut tout de m�me positionner ce flag
         return true;
      }

      if( !(aladin.view.isMultiView() && v.pref.ref)
            && v.pref.isImage() && (type==Plan.IMAGE /* || type==Plan.ALLSKYIMG */  ) && aladin.calque.getIndex(this)>aladin.calque.getIndex(v.pref)) {
         setDebugFlag(UNDERIMG, true);
         //System.out.println("IMG:"+label+" sous IMG?:"+v.pref+" dans le pile");
         return false;

      } else setDebugFlag(UNDERIMG, false);

      // Peut �tre sous un plan Background ?
      if( !setUnderBackGroundFlag(v) ) {
         return false;
      }

      boolean rep = isOutView(v);
      //System.out.println(this+" isOutView("+v+") = "+rep);
      return !rep;
   }

   // Peut �tre sous un plan Background ?
   private boolean setUnderBackGroundFlag(ViewSimple v) {
      boolean under=false;

      // Si l'on force l'affichage des overlays qq soit leur position dans la pile
      // on peut simplifier comme suit
      if( ViewSimple.OVERLAYFORCEDISPLAY && !isPixel() ) {
         setDebugFlag(UNDERBKGD, under);
         return !under;
      }

      Plan [] allPlan = aladin.calque.getPlans();
      int n = aladin.calque.getIndex(allPlan,this);
      for( int i=n-1; i>=0; i-- ) {
         Plan p=allPlan[i];
         if( p.type==ALLSKYIMG && p.active
               && (p.getOpacityLevel()==1 || p.isRefForVisibleView())
               && !((PlanImage)p).isTransparent() ) {
            under=true;
            break;
         }
      }
      if( under && aladin.view.isMultiView() ) under=false;

      setDebugFlag(UNDERBKGD, under);
      return !under;

   }


   /** Retourne true si l'image est � coup s�r hors de la vue - je teste
    * uniquement les cas en-dessous, au-dessous, � droite ou � gauche */
   protected boolean isOutView(ViewSimple v) {
      int w = v.getWidth();
      int h = v.getHeight();

      if( this instanceof PlanBG ) {
         setDebugFlag(OUTOFVIEW,false);
         return false;

      } else if( isImage() ) {
         PointD [] b= ((PlanImage)this).getBords(v);
         if( b==null ) return true;
         double minX,maxX,minY,maxY;

         minX=maxX=b[0].x;
         minY=maxY=b[0].y;
         for( int i=1; i<4; i++ ) {
            if( b[i].x<minX ) minX = b[i].x;
            else if( b[i].x>maxX ) maxX = b[i].x;
            if( b[i].y<minY ) minY = b[i].y;
            else if( b[i].y>maxY ) maxY = b[i].y;
         }

         // Tout � droite ou tout � gauche, Au-dessus ou en dessous
         if( minX<0 && maxX<0 || minX>=w && maxX>=w
               || minY<0 && maxY<0 || minY>=h && maxY>=h ) {
            setDebugFlag(OUTOFVIEW,true);
            return true;
         }

         setDebugFlag(OUTOFVIEW,false);
         return false;  // Mais attention, ce n'est pas certain !!

      } else {
         //         boolean rep = !v.pref.projd.agree(projd,aladin.view.getCurrentView(),false);
         boolean rep = !v.getProj().agree(projd,aladin.view.getCurrentView(),false);
         setDebugFlag(OUTOFVIEW,rep);
         return rep;
      }
   }



   /**
    * Retourne l'etat d'avancement du chargement du plan
    */
   protected String getProgress() {
      if( type!=NO && !flagOk  && error==null ) return " - in progress...";
      return "";
   }

   /** Retourne la ligne d'informations concernant le plan dans le statut d'Aladin*/
   protected String getInfo() { return getInfo(false); }
   protected String getInfo(boolean full) {
      try {
         String s=null;
         if( type==NO ) s="";
         else if( error!=null ) s=label+ " *** " + error;
         else {
            String from = getFrom();
            String progress = getProgress();
            if( progress.length()>0 ) s=label+progress;
            else {
               try {
                  if( isCatalog() ) s=label+" - "+getStats();
               } catch( Exception e ) { e.printStackTrace(); }
            }
            if( s==null ) s=label;
            if( full && s!=null ) {
               s=s+(objet!=null?"\nAround "+objet:"");
               s=s+(from!=null?"\n"+from:"");
            }
            int n = aladin.calque.plan.length-aladin.calque.getIndex(this);
            s="[Plane @"+n+"] - "+s;
         }
         s=s+addDebugInfo();
         return s;
      } catch( Exception e ) {}
      return "";
   }

   /** Ajout d'info de debugging et de stat */
   protected String addDebugInfo() {
      if( Aladin.levelTrace<2  ) return "";
      StringBuffer s = new StringBuffer();

      if( active ) {
         if( statNbComputing!=0 ) {
            s.append("\n - "+statNbComputing+" projection"+(statNbComputing>1?"s":"")
                  +" ("+statTimeComputing+"ms)");
         }
         if( statNbItems!=0 ) {
            s.append("\n - "+statNbItems+" object"+(statNbItems>1?"s":"")+" drawn ("+statTimeDisplay+"ms)");
         } else {
            s.append("\n - drawn in "+statTimeDisplay+"ms");
         }
      }

      // Flags de d�bugging
      String a = getDebugFlag();
      if( a.length()>0 ) s.append("\n["+a+"]");

      return s.toString();
   }

   /** Generation du label du plan.
    * Retourne le label en fonction de l'etat courant du plan
    * Il s'agit simplement d'ajouter des "..." quand le plan est en
    * cours de construction
    * @return Le label genere
    */
   protected String getLabel() {
      String s=label;
      int p= (int)getPourcent();
      //      int c= (int)getCompletude();
      if( p>0 && p<100) s=Util.align((label.length()>6 ? label.substring(0,6):label)+"... ",9)+p+"%";
      else if( error==null && !flagOk ) s= label+"...";
      //      else if( c>0 && c<100) s=Util.align((label.length()>9 ? label.substring(0,9):label),11)+c+"%";
      return s;
   }

   // Pr�vu pour les cubes
   protected String getFrameLabel(int frame) { return label; }

   protected double getCompletude() { return -1; }

   /** Retourne l'url qui a permis de g�n�rer le plan */
   protected String getUrl() { return u==null ? null : u.toString(); }

   /** Retourne true ssi l'url qui a permis de g�n�rer le plan ssi existe
    * et n'est pas un fichier locale */
   protected boolean hasRemoteUrl() {
      return u!=null && !u.toString().startsWith("file:");
   }

   /** Retourne le niveau d'opacit� [0..1] (0: enti�rement transparent, 1: enti�rement opaque) */
   public float getOpacityLevel() {
      return this.opacityLevel;
   }

   /** Positionne le niveau d'opacit� [0..1] (0: enti�rement transparent, 1: enti�rement opaque) */
   public void setOpacityLevel(float opacityLevel) {
      if( opacityLevel<0 ) opacityLevel=0;
      if( opacityLevel>1 ) opacityLevel=1;
      this.opacityLevel = opacityLevel;
   }

   // TODO : � terme, on pourra se d�barasser de cette m�thode
   /**
    * Retourne la derni�re ID "PLASTIC" du plan � avoir �t� donn�
    * Si aucune ID Plastic n'a �t� fix�e, on retourne l'URL du plan
    * @return
    */
   protected String getLastPlasticID() {
      if( plasticIDs!=null && plasticIDs.size()>0 )
         return plasticIDs.get(plasticIDs.size()-1);
      return getUrl();
      //      return plasticID!=null ? plasticID : getUrl();
   }

   /**
    * retourne l'ID PLASTIC de ce plan
    * @return
    */
   protected String getPlasticID() {
      if( plasticIDs!=null && plasticIDs.size()>0 )
         return plasticIDs.get(0);
      return getUrl();
   }

   /**
    * Teste si id est l'un des identifiants PLASTIC du plan
    *
    * @param id
    */
   protected boolean hasPlasticID(String id) {
      if( id==null ) return false;
      String str;
      try {
         if( ! id.startsWith("file") ) str = id;
         else {
            URL url = new URL(id);
            //	pour �viter des pbs avec les URL locales !!
            str = url.getFile();
         }
      }
      catch(MalformedURLException mue) {
         str = id;
      }
      if( plasticIDs!=null ) {
         Enumeration<String> enumID = plasticIDs.elements();
         String tmp;
         while( enumID.hasMoreElements() ) {
            tmp = enumID.nextElement();
            if( tmp.startsWith("file") ) {
               try {
                  URL url = new URL(tmp);
                  tmp = url.getFile();
               }
               catch(MalformedURLException mue) {}
            }
            if( str.equals(tmp) ) return true;
         }
      }

      // en dernier recours
      String url = getUrl();
      if( url!=null && id.equals(url) ) return true;
      return false;
   }

   /** Ajoute la chaine s comme ID Plastic de ce plan
    *
    * @param s
    */
   protected void addPlasticID(String id) {
      if (id==null) {
         return;
      }

      if( plasticIDs==null ) plasticIDs = new Vector<String>();

      if( plasticIDs.indexOf(id)<0 ) plasticIDs.addElement(id);
   }

   /** Enregistrement du label du plan (ce qui apparaitra a cote du logo
    * du plan) en le modifiant �ventuellement pour qu'il soit unique
    */
   protected void setLabel(String label) {
      String x = getUniqueLabel(label);
      if( x==null ) return;
      this.label = x;
   }

   /**
    * Retourne un label pour qu'il soit unique dans la pile
    * Change tous les \n en ' ' et les '/' en '.'
    * Suffixe le plan par ~n pour avoir un nom unique dans la pile
    * @param label le label propos�
    * @return le label retenue ou null si cas sp�ciaux (commen�ant par '=')
    */
   protected String getUniqueLabel(String label) {
      char [] a = label.toCharArray();

      // Les labels qui commencent par = servent � indiquer que l'on voulait
      // surcharger un plan d�j� existant du m�me nom. Donc inutile de tester
      // l'existence d'un doublon, cela a d�j� �t� fait.
      if( a.length>0 && a[0]=='=' ) {
         if( a[1]!='@' ) this.label = label.substring(1);
         isOldPlan=true;
         return null;
      }

      for( int i=0; i<a.length; i++ ) {
         if( a[i]=='\n' ) a[i]=' ';
         //         if( a[i]=='/' ) a[i]='.';
      }
      String s=new String(a);
      String x =s;
      if( isExistingLabel(x,false) ) {
         int j = s.indexOf('~');
         if( j>0 ) s = s.substring(0,j);
         x = s;
         int n=1;

         while( isExistingLabel(x,false) ) x=s+"~"+(n++);
      }
      return x;
   }

   /** Retourne true si le label du plan ne peut �tre utilis�,
    * soit qu'il soit d�j� pris, soit qu'il puisse y avoir confusion
    * avec l'identificateur d'une vue (ex B2) */
   protected boolean isExistingLabel(String s,boolean withTestView) {

      if( s.trim().length()==0 ) return true;
      if( withTestView ) {
         if( aladin.view.getNViewFromID(s)>=0 ) return true;
         if( s.equals("ROI") ) return true;
         if( s.equals("all") ) return true;
      }
      Plan [] allPlan = aladin.calque.getPlans();
      for( int i=0; i<allPlan.length; i++ ) {
         Plan p = allPlan[i];
         if( p==this ) continue;
         if( p!=null && p.label!=null && s.equals(p.label)) return true;
      }
      return false;
   }

   /** Retourne l'origine en tronquant la chaine si trop longue */
   protected String getFrom() {
      if( copyright==null ) return null;
      int n=copyright.length();
      if( n>40 ) {
         int end = copyright.lastIndexOf(Util.FS,copyright.length()-15);
         int first = copyright.indexOf(Util.FS,4);
         if( end!=-1 && first!=-1 && first<end ) return copyright.substring(0,first+1)+"..."+copyright.substring(end);
         return copyright.substring(0,40)+"...";
      }
      return copyright;
   }

   /** Positionnement de l'origine */
   protected void setFrom(String from) {
      this.copyright = from;
   }

   /** Indique l'objet central et les parametres de l'image pour un log */
   protected String getLogInfo() {
      return (objet==null?"null":objet)+"/"+(param==null?"null":param);
   }

   /** Retourne le target utilis�e pour la requ�te, ou s'il n'y en a pas,
    * la coordonn�e barycentrique, sinon ""
    */
   protected String getTargetQuery() {
      if( objet!=null ) return objet;
      if( co!=null ) return co.getSexa();
      return "";
   }

   /** Retourne le Target du plan en J2000 ou null si non d�fini */
   protected String getTarget() {
      if( co==null ) return null;
      return co.getSexa(":");
   }

   /** Modifie (si possible) une propri�t� du plan (d�pend du type de plan) */
   protected void setPropertie(String prop,String specif,String value) throws Exception {
      if( prop.equalsIgnoreCase("PlaneID") ) {
         if( isExistingLabel(value,false) ) throw new Exception("PlaneID already existing");
         setLabel(value);
      } else if( prop.equalsIgnoreCase("proj") ) {
         modifyProj(value);
      } else if( prop.equalsIgnoreCase("epoch") ) {
         if( !isCatalog() ) throw new Exception("Epoch can be modified only for catalog planes");
         if( !hasPM() ) throw new Exception("Unknown proper motion fields");
         setEpoch(value);
         aladin.calque.repaintAll();
      } else if( prop.equalsIgnoreCase("info") ) {
         verboseDescr=value;
      } else if( prop.equalsIgnoreCase("ack") ) {
         ack=value;
      } else if( prop.equalsIgnoreCase("Color") ) {
         Color c = Action.getColor(value);
         if( c==null ) throw new Exception("Syntax error in color function (ex: rgb(30,60,255) )");
         this.c = c;
         aladin.calque.repaintAll();
      } else if( prop.equalsIgnoreCase("Status") ) {
         if( value.indexOf("shown")>=0 ) setActivated(true);
         else if( value.indexOf("hidden")>=0 ) setActivated(false);
         else throw new Exception("Status unknown");
      } else if( prop.equalsIgnoreCase("Background") ) {
         Color c = value.equalsIgnoreCase("white") ? Color.white : value.equalsIgnoreCase("black") ? Color.black : null;
         colorBackground = c;
         aladin.calque.repaintAll();
      } else if( prop.equalsIgnoreCase("Opacity") ) {
         if( !aladin.calque.canBeTransparent(this) /* planeTypeCanBeTrans(this)*/ ) throw new Exception("opacity property only applicable for images and footprints !");
         float f;
         try {
            f = Float.parseFloat(value);
         }
         catch(NumberFormatException nfe) {
            throw new Exception(value+" is not a valid value for opacity level !");
         }
         if( f>100 || f<0 ) throw new Exception("Opacity value must be between 0 and 100 !");
         setOpacityLevel(f/100f);
         aladin.calque.repaintAll();
      } else if( prop.equalsIgnoreCase("scalingFactor") || prop.equalsIgnoreCase("size")) {
         float n;
         try {
            n = Float.parseFloat(value);
            if( n<=0 ) throw new Exception();
         } catch( Exception e ) {
            throw new Exception(value+" is not a valid value for scalingFactor ]0..5] !");
         }
         setScalingFactor(n);
         aladin.calque.repaintAll();

      } else if( prop.startsWith("FITS:") ) {
         prop = prop.substring(5);
         if( headerFits==null ) headerFits=new FrameHeaderFits(this,prop+"="+value);
         else {
            if( value.length()==0 ) value=null;
            headerFits.setToHeader(prop,value);
         }
         try {
            setNewProjD(new Projection(Projection.WCS,new Calib(headerFits.getHeaderFits())));
            setHasSpecificCalib();
            aladin.command.console("Astrometrical calibration has been modified on "+label);
            aladin.view.newView();
            aladin.view.repaintAll();
         } catch( Exception e ) { }

      } else throw new Exception("Unknown plane propertie ["+prop+"]");

      Properties.majProp(this);
   }


   private long startCheckBoxBlink=0L;  // Date de d�but d'une s�quence de checkbox blink

   /** Lance la s�quence de blink de la check box du plan (voir isCheckBoxBlink() */
   protected void startCheckBoxBlink() {
      startCheckBoxBlink = System.currentTimeMillis();
   }

   /** Retourne true si la checkbox du plan (dans la pile) doit �tre affich�
    * temporairement en rouge afin d'indiquer � l'utilisateur qu'il doit �ventuellement l'utiliser */
   protected boolean isCheckBoxBlink() {
      long t = System.currentTimeMillis();
      return t-startCheckBoxBlink<4000;
   }

   /** Demande d'activation/d�sactivation d'un plan
    * On m�morise le dernier �tat demand� au cas o� ce n'est pas possible
    * imm�diatement. Si aucun param�tre, on tente d'activer le dernier
    * �tat demand�.
    * @param flag true pour activer le plan, false sinon
    * @return true si le plan a pu �tre activ�, false sinon
    */
   protected boolean setActivated(boolean flag) {
      askActive = flag;
      return setActivated();
   }

   /** Demande d'activation suivant le dernier choix m�moris� */
   protected boolean setActivated() {
      boolean flag=askActive;

      // V�rification que l'activation est possible en fonction des vues visibles
      if( askActive && !isViewable() ) flag=false;

      // Inutile, d�j� fait
      if( active==flag ) return active;

      // Activation/Desactivation effective
      active=flag;
      //   	  if( active && getOpacityLevel()<0.1f && !ref ) setOpacityLevel(1f);
      if( !active ) aladin.view.deSelect(this);
      else aladin.view.addTaggedSource(this);

      // Activation automatique du SED associ� au plan (le cas �ch�ant)
      if( active && isSED() ) {
         Source sEtalon = aladin.view.addSEDSource(this);
         //         System.out.println("Plan.setActivated() setSED("+sEtalon+")");
         if( sEtalon!=null ) aladin.view.zoomview.setSED(sEtalon);
      }

      return active;
   }

   /** Arret de chargement en parallele du plan */
   synchronized public void stop() {
      //      runme.stop();
      runme = null;
   }

   /** Dessin du plan
    * @param op niveau d'opacit� forc�, -1 si prendre celui du plan
    */
   //   protected void draw(Graphics g,ViewSimple v,int dx, int dy, float op) {
   //      if( v==null ) return;
   //      if( op==-1 ) op=getOpacityLevel();
   //      if(  op<=0.1 ) return;
   //
   //      if( g instanceof Graphics2D ) {
   //         Graphics2D g2d = (Graphics2D)g;
   //         Composite saveComposite = g2d.getComposite();
   //         try {
   //            if( op < 0.9 ) {
   //               Composite myComposite = Util.getImageComposite(op);
   //               g2d.setComposite(myComposite);
   //            }
   //            draw(g2d, v, dx, dy);
   //         } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
   //         g2d.setComposite(saveComposite);
   //
   //      } else draw(g, v, dx, dy);
   //   };
   //
   //   protected void draw(Graphics g,ViewSimple v,int dx, int dy) { }


   /** Attente pendant la construction du plan.
    * Il est necessaire de voir les specialisations des cette methode
    * dans les classes PlanImage et PlanObjet pour comprendre
    * @return <I>true</I> si ok, <I>false</I> sinon.
    * @see aladin.PlanImage#waitForPlan()
    * @see aladin.PlanCatalog#waitForPlan()
    */
   protected boolean waitForPlan() { return false; }

   private Vector listeners = new Vector();
   /**
    * ajout d'un listener s'int�ressant � l'�v�nement "chargement du plan"
    * Utilis� par SAMP
    * @param listener
    */
   protected synchronized void addPlaneLoadListener(PlaneLoadListener listener) {
      this.listeners.add(listener);
   }

   /**
    * Supprime un listener
    * @param listener le listener � supprimer
    * @return si listener faisait partie de la liste des listeners
    */
   protected synchronized boolean removeListener(PlaneLoadListener listener) {
      return this.listeners.remove(listener);
   }

   protected void callAllListeners(PlaneLoadEvent ple) {
      Enumeration eListeners = this.listeners.elements();
      while( eListeners.hasMoreElements() ) {
         ((PlaneLoadListener)eListeners.nextElement()).planeLoaded(ple);
      }
   }

   protected static final String ALADINQUERY = "AladinQuery";

   /** Marque un thread comme �tant un thead d'interrogation Aladin.
    * Le but est d'�viter de cr�er des threads en s�rie pour des fits extensions
    * cf threading()
    */
   static protected void aladinQueryThread(Thread t) { t.setName(ALADINQUERY); }

   /** D�termine s'il faut threader au non */
   protected void threading() {
      if( Thread.currentThread().getName().equals(ALADINQUERY) ) run();
      else {
         runme = new Thread(this,"AladinQueryBis");
         Util.decreasePriority(Thread.currentThread(), runme);
         //         runme.setPriority( Thread.NORM_PRIORITY -1);
         aladinQueryThread(runme);
         runme.start();
      }

   }

   /** Lance le chargement du plan */
   public void run() {
      Aladin.trace(1,(flagSkip?"Skipping":"Creating")+" the "+Tp[type]+" plane "+label);
      if( server!=null ) server.setStatus();
      boolean rep=true;
      try { rep = waitForPlan();
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      planReady( rep );
      if( server!=null ) server.setStatus();
   }

   private long startTime;

   /** Positionne le flag ``ready'' du plan.
    * En fonction de la valeur du flag "ready" positionne les
    * parametres de l'etat courant du plan et demande les
    * re-affichages necessaires
    * @param ready l'etat a positionner
    */
   protected void planReady(boolean ready) {
      if( flagSkip ) return;

      if( doClose && dis!=null ) {
         try { dis.close(); } catch( IOException e ) {
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
      }

      aladin.endMsg();

      if( !ready ) {
         if( error==null ) error = aladin.error;
         aladin.calque.select.repaint();
         aladin.toolBox.toolMode();

         return;
      }

      long t = System.currentTimeMillis();;
      if( type!=FOLDER ) Aladin.trace(3,"Plane ["+label+"] load&build in "+(t-startTime)+"ms" );


      // Dans le cas d'une image ou si la pile est vide (cas d'un catalogue
      // qui arriverait en premier), le plan devient un plan de r�f�rence
      //      if( !isOldPlan && isImage() || aladin.calque.isFreeX() ) aladin.calque.setPlanRef(this);
      //      else setActivated(true);


      setPourcent(-1);
      flagOk = ready;


      //      if( aladin.calque.mustBeSetPlanRef(this) ) {
      //         aladin.calque.setPlanRef(this);
      //         setOpacityLevel( isOverlay()?1f : 0f);
      //
      //      } else setActivated(true);


      boolean un,deux,trois;
      un=deux=trois=false;

      if( (un=!isOldPlan && (isImage() || type==ALLSKYIMG && !(this instanceof PlanBGCat)))
            || (deux=aladin.calque.isFreeX(this))
            || (trois=isSimpleCatalog() && !isViewable() && !aladin.calque.isBackGround() ) ) {
         aladin.calque.setPlanRef(this);
         if( !isOverlay() )  setOpacityLevel(0f);
         //         setOpacityLevel( isOverlay()?1f : 0f);
      } else {
         if( !isViewable() ) aladin.view.syncPlan(this);
         setActivated(true);

         // Voir si ca marche
         aladin.calque.unSelectAllPlan();
         selected=true;
      }


      // Ajout thomas : on avertit qu'un nouveau plan a ete cree pour mettre a jour les filtres
      // et pour mettre a jour les FilterProperties
      if( isCatalog() ) {
         PlanFilter.newPlan(this);
         FilterProperties.notifyNewPlan();
      }

      if( !isOldPlan ) {
         aladin.calque.bestPlacePost(this);

         // Mise � jour des formulaires serveurs (gratit et Choice input)
         if( aladin.dialog!=null ) aladin.dialog.resume();
      }

      // Lib�ration de l'attente possible sur le target (voir Command.waitingPlanInProgress)
      flagWaitTarget=false;

      flagProcessing = false;

      aladin.calque.repaintAll();
   }

   public String toString() {
      return label+"["+Tp[type]+"]";
   }

   // Arrondi plus facile � �crire que (int)Math.round()
   static protected int round(double x) { return (int)(x+0.5); }
   static protected int floor(double x) { return (int)x; }
   static protected int top(double x) { return (int)x==x ? (int)x : (int)(x+1); }

   // Recup�ration d'un it�rator sur les Obj
   protected Iterator<Obj> iterator() { return pcat==null ? null : pcat.iterator(); }

   // Recup�ration d'un it�rator sur les objets visible dans la vue v (voir sp�cificit� PlanBGCat)
   protected Iterator<Obj> iterator(ViewSimple v) { return iterator(); }


   /**
    *
    * @return true if there are at least one source with one associated source
    */
   protected boolean hasAssociatedFootprints() {
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         Source s = (Source)o;
         if( s.getFootprint() != null) return true;
      }
      return false;
   }

   /**
    * Shows or hides all footprints associated to sources in the plane
    * @param show
    */
   protected void showFootprints(boolean show) {
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         Source s = (Source)o;
         s.setShowFootprint(show,false);
      }

      aladin.calque.repaintAll();
   }


   // thomas, pour realloc des objets constituant un footprint associ�
   protected void reallocFootprintCache() {
      if( pcat==null ) return;   //PF 06/07/2006 - en cas de Free concurrent
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( !(o instanceof Source) ) continue;
         Source s = (Source)o;
         if( s.getFootprint()!=null ) {
            PlanField pf = s.getFootprint().getFootprint();
            if (pf != null) {
               pf.pcat.reallocObjetCache();
            }
         }
      }

   }



}
