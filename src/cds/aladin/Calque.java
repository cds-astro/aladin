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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Scrollbar;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import cds.moc.HealpixMoc;
import cds.tools.Util;

/**
 * Objet de gestion des plans et de tout ce qui est associe
 *
 * @author Pierre Fernique [CDS]
 * @version 1.2 : dec 03 - resetScroll() methode
 * @version 1.1 : (15 janvier 2000) Clien d'oeil on/off
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.91 : Revisite le 1 dec 98
 * @version 0.9  : (??) creation
 */
public final class Calque extends JPanel implements Runnable {

   // les variables generiques
   int FIRSTBLOC = 25;
   int BLOC = 50;

   // Les references aux objets
   Aladin aladin;

   // Les composantes de l'objet
   Select select;         // Le selecteur de manipulation des plans
   Zoom  zoom;            // Le zoom
   protected Plan plan []; // les plans
//   PlanBG planBG;         // Fond du ciel

//   Vector plan;
   int maxPlan=0; 	  // Nombre total de plan actuellement alloue

   // Les valeurs a memoriser
   int current;         // Le plan courant
   int runme_n= -1;
   ScrollbarStack scroll;		 // La scrollbar verticale si necessaire
   protected int reticleMode;    //  1-normal, 2-large
   protected boolean flagOverlay;// True si l'echelle doit etre affichee
   protected boolean flagHpxPolar;// True si la polarisation HEALPix doit etre affichee
   protected boolean flagAutoDist; // True si l'outil de mesure automatique des distances est activé
   protected boolean flagSimbad; // True si la résolution quickSimbad est lancé
   protected boolean flagVizierSED;  // True si la résolution SED est lancé
   protected boolean flagTip;    // True si les tooptips s'affichent sur les sources pointées
//   protected boolean flagSyncView=false; // True si le zoom est synchronisé entre les vues

   protected Fov[] curFov; // FOVs pour le noeud courant
   Projection fovProj; // pour forcer la projection de curFov
   protected Fov[] cutoutFov = null; // champs (FOV) pour cutout
   
   protected int overlayFlag;
   
   static final private int SCALE   = 1;
   static final private int LABEL   = 2;
   static final private int SIZE    = 4;
   static final private int GRID    = 8;
   static final private int NE      = 16;
   static final private int RETICLE = 32;
   static final private int TARGET  = 64;
   static final private int PIXEL = 128;
   static final private int HPXGRID = 256;
   
   static final private String [] OVERLAYFLAG = { "scale","label","size","grid","NE","reticle","target","pixel","HPXgrid" };
   static final private int [] OVERLAYFLAGVAL = { SCALE,  LABEL,  SIZE,  GRID,  NE,  RETICLE,  TARGET,  PIXEL,  HPXGRID };
   
   /** Retourne le champ de bits qui contrôle les overlays */
   public int getOverlayFlag() { return overlayFlag; }
   
   /** Indique les flags d'overlays actifs */
   public boolean hasScale()   { return (overlayFlag & SCALE)   == SCALE; }
   public boolean hasLabel()   { return (overlayFlag & LABEL)   == LABEL; }
   public boolean hasSize()    { return (overlayFlag & SIZE)    == SIZE; }
   public boolean hasGrid()    { return (overlayFlag & GRID)    == GRID; }
   public boolean hasNE()      { return (overlayFlag & NE)      == NE; }
   public boolean hasReticle() { return (overlayFlag & RETICLE) == RETICLE; }
   public boolean hasTarget()  { return (overlayFlag & TARGET)  == TARGET; }
   public boolean hasPixel()   { return (overlayFlag & PIXEL)   == PIXEL; }
   public boolean hasHpxGrid() { return (overlayFlag & HPXGRID) == HPXGRID; }
   
   /** Positionnement d'un flag d'overlay - ex: setOverlayFlag("grid",true);
    * @param name le nom de la fonction d'overlay
    * @param flag true pour l'activation, false pour la désactivation
    * @return false si problème
    */
   public boolean setOverlayFlag(String name,boolean flag) {
      int i = Util.indexInArrayOf(name, OVERLAYFLAG, true);
      if( i<0 ) return false;
      if( flag ) overlayFlag |= OVERLAYFLAGVAL[i];
      else overlayFlag &= ~OVERLAYFLAGVAL[i];
      return true;
   }
   
   /** Mise à jour des flags d'overlays (les noms séparés par une simple virgule).
    * Si la liste commence par '+' ou '-', il s'agit d'une mise à jour */
   public void setOverlayList(String names) {
      int mode = 0;
      if( names.length()>1 ) {
         if( names.charAt(0)=='+' ) { mode=1; names=names.substring(1); }
         else if( names.charAt(0)=='-' ) { mode=-1; names=names.substring(1); }
      }
      if( mode==0 ) { overlayFlag=0; mode=1; }
      
      int mask=0;
      Tok tok = new Tok(names,",");
      while( tok.hasMoreTokens() ) {
         String name = tok.nextToken().trim();
         int i = Util.indexInArrayOf(name, OVERLAYFLAG, true);
         if( i>=0 ) mask |= OVERLAYFLAGVAL[i];
      }
      if( mode==1 ) overlayFlag |= mask;
      else overlayFlag &= ~mask;
   }
   
   /** Retourne sous la forme d'une chaine de caractères, tous les overlays actifs 
    * ex : grid,scale,NE */
   public String getOverlayList() {
      StringBuffer s = new StringBuffer();
      for( int i=0; i<OVERLAYFLAG.length; i++ ) {
         if( (overlayFlag & OVERLAYFLAGVAL[i]) ==  OVERLAYFLAGVAL[i] ) {
            if( s.length()>0 ) s.append(',');
            s.append(OVERLAYFLAG[i]);
         }
      }
      return s.toString();
   }

   protected void createChaine() {

   }
   
//   protected Source sourceToShow; // source à montrer (thomas, votech)
//   protected Source osourceToShow; // ancienne source à montrer (thomas, votech)

   /** Creation de l'objet calque */
   protected Calque(Aladin aladin) {
      this.aladin = aladin;

      select = new Select(aladin);
      zoom = new Zoom(aladin);
      scroll = new ScrollbarStack(aladin,Scrollbar.VERTICAL,FIRSTBLOC-1,1,0,FIRSTBLOC);

      // Creation des composantes de l'objet (plan, select et zoom)
      reallocPlan();
      flagOverlay = true;
      reticleMode=aladin.configuration.get(Configuration.RETICLE)!=null ? 2 : 1;
      flagTip=aladin.configuration.get(Configuration.TOOLTIP)!=null;
      flagAutoDist = aladin.configuration.getAutoDist();
      flagSimbad = aladin.configuration.getSimbadFlag();
      flagVizierSED = aladin.configuration.getVizierSEDFlag();
      
      setOverlayList("label,scale,size,NE,target,reticle,target,pixel");

      // Panel principal : contient le selecteur de plans et le zoom
      setLayout( new BorderLayout(0,5) );
      add(select,BorderLayout.CENTER);
      add(zoom,BorderLayout.SOUTH);
   }
   
   /** Insère ou enlève la scrollbar verticale de la pile si nécessaire
    * @return true s'il y a eu un changement d'état
    */
   protected boolean scrollAdjustement() {
      int lastPlan = scroll.getLastVisiblePlan();
      if( lastPlan==-1 ) return false;
      boolean hideScroll = !scroll.getRequired();

//      System.out.println("lastPlan="+lastPlan+" required="+scroll.getRequired()+" value="+scroll.getValue()+" ["+scroll.getMinimum()+".."+scroll.getMaximum()+"] hideScroll => "+hideScroll);
      if( scroll.isShowing() && hideScroll  ) { remove(scroll); validate(); return true; }
      else if( !scroll.isShowing() && !hideScroll  ) { add(scroll,"East"); validate(); return true; }
      return false;
   }

   public Dimension getPreferredSize() { 
      return new Dimension(select.frMax+select.sizeLabel+MyScrollbar.LARGEUR,200);
//      return new Dimension(select.getPreferredSize().width- (Aladin.NEWLOOK_V7 && scroll.isShowing()?scroll.getPreferredSize().width:0),200);
   }

   /** Verrou d'accès à plan[] */
   volatile protected Object pile = new Object();

   /** Retourne la taille de la pile */
   protected int getNbPlan() { return plan.length; }

   /** Retourne le plan dont le hashcode correspond */
   protected Plan getPlanByHashCode(int hashCode) {
      synchronized( pile ) {
         for( int i=0; i<plan.length; i++ ) {
            if( plan[i].hashCode()==hashCode ) return plan[i];
         }
      }
      return null;
   }

   /** Retourne un plan repéré par son indice dans la pile */
   public Plan getPlan(int index) {
      try {
         return plan[index];
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return null;
      }
   }

   /** Retourne une copie du tableau de tous les plans de la pile */
   protected Plan [] getPlans() {
//      synchronized( pile ) {
//return plan;
         Plan [] p = new Plan[plan.length];
         System.arraycopy(plan, 0, p, 0, plan.length);
         return p;
//      }
   }

   /** Retourne le premier Plan Folder qui limite le scope du plan, null si aucun */
   private Plan getMyScopeFolder(Plan p) { return getMyScopeFolder(plan,p); }
   protected Plan getMyScopeFolder(Plan [] allPlan,Plan p) {
      int nivCherche = p.folder-1;   // Niveau folder à chercher

      for( int i=getIndex(p)-1; nivCherche>=0 && i>=0; i-- ) {
         if( allPlan[i].type==Plan.FOLDER ) {
            if( ((PlanFolder)allPlan[i]).localScope ) {
               if( allPlan[i].folder==nivCherche ) return allPlan[i];
            }
            nivCherche--;
         }
      }
      return null;
   }

   /** Retourne le PlanFolder contenant le plan f, null si aucun */
   protected Plan getFolder(Plan f) {
      if( f.type==Plan.FOLDER ) return f;
      int n=getIndex(f);
      for( int i=n-1; i>=0; i-- ) {
         if( plan[i].type==Plan.FOLDER && plan[i].folder<f.folder ) return plan[i];
      }
      return null;
   }

   /** Retourne les plans d'un folder
    * @param f le plan (de type FOLDER)
    * @param all true tous les plans contenus (récursivité)
    * @return le tableau des plans contenus dans le folder f
    */
   protected Plan[] getFolderPlan(Plan f) { return getFolderPlan(f,true); }
   protected Plan[] getFolderPlan(Plan f,boolean all) {
      if( f.type!=Plan.FOLDER ) return null;
      Vector v = new Vector(10);
      int n = f.folder;	// niveau du folder
      int i;
      boolean trouve=false;

      for( i=0; i<plan.length; i++ ) {
         Plan pc = plan[i];

         // avant le folder f
         if( !trouve ) trouve=pc==f;

         // dans le folder f
         else {
            if( pc.folder<=n ) break;  	           // Fin du folder
            if( all || pc.folder==f.folder+1 ) v.addElement(pc); // Memorisation
         }
      }

      // Recopie dans un tableau
      Plan p[] = new Plan[v.size()];
      Enumeration e=v.elements();
      for( i=0; e.hasMoreElements(); p[i++]=(Plan)e.nextElement());
      return p;
   }

  /** Retourne vrai si le folder est ferme. Cherche simplement si
   * le plan suivant est collapse */
   protected boolean isCollapsed(Plan f) {
      int i;
      if( f.type!=Plan.FOLDER ) return false;
      Plan[] plan = getPlans();
      for( i=0; i<plan.length && plan[i]!=f; i++ );
      if( i>=plan.length-1) return false;
      return plan[i+1].collapse;
   }

   /** Activation ou desactivation de tous les plans d'un folder */
   protected void setActiveFolder(Plan f,boolean flag) {
      Plan p[] = getFolderPlan(f);
      for( int i=0; i<p.length; i++ ) p[i].setActivated(flag);
      f.active=flag;
      repaintAll();
   }

   // Allocation ou reallocation des plans
   private int reallocPlan() {
      int n;
      int bloc=maxPlan>0?BLOC:FIRSTBLOC;

      synchronized( pile ) {
         // Allocation d'une nouvelle tranche de plans
         Plan newP[] = new Plan[maxPlan+bloc];

         //Recopie avec decalage
         if( maxPlan>0 ) {
            System.arraycopy(plan,0,newP,bloc,maxPlan);
//            newP[0] = plan[0];
            //         n=scroll.getValue()-bloc+1;
            n=scroll.getValue()+bloc;
         } else {
            n=bloc;
         }

         // Initialisation du nouveau bloc de plans
         for( int i=0; i<bloc; i++ ) newP[i] = new PlanFree(aladin);

         // Ajustement des variables
         plan = newP;
         maxPlan+=bloc;

         if( SwingUtilities.isEventDispatchThread() ) {
            scroll.setValues(n-1,1,0,maxPlan);
         } else {
            final int [] param = new int[1];
            param[0]=n;
            SwingUtilities.invokeLater(new Runnable() {
               public void run() { scroll.setValues(param[0]-1,1,0,maxPlan); }
            });
         }
      }
      return n;
   }

   // reinitialisation du stack
   private void reinitPlan() {
      maxPlan=FIRSTBLOC;

      synchronized( pile ) {
         // Reallocation initiale des plans
         Plan newP[] = new Plan[maxPlan];

         // Initialisation du nouveau bloc de plans
         for( int i=0; i<maxPlan; i++ ) newP[i] = new PlanFree(aladin);

         // Ajustement des variables
         plan = newP;
      }
      if( SwingUtilities.isEventDispatchThread() ) {
         scroll.setValues(maxPlan-1,1,0,maxPlan);
      } else {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { scroll.setValues(maxPlan-1,1,0,maxPlan);  }
         });
      }

      taggedSrc=false;
   }

  /** Retourne le nombre de plans actuellement sélectionnés */
   protected int getNbSelectedPlans() {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type!=Plan.NO && plan[i].flagOk && plan[i].selected ) n++;
      }
      return n;
   }
   

  /** Retourne le nombre de plans actuellement utilises */
   protected int getNbUsedPlans() {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type!=Plan.NO && plan[i].flagOk ) n++;
      }
      return n;
   }
   
   /** Retourne lea moyenne des FPS des plans allsky image dans la pile */
   protected double getFps() {
      Plan [] plan = getPlans();
      double fps=0;
      int n=0;
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type!=Plan.ALLSKYIMG || !plan[i].flagOk ) continue;
         double x;
         try { x = ((PlanBG)plan[i]).getFps(); }
         catch( Exception e ) { continue; }
         if( x<=0 ) continue;
         fps += x;
         n++;
      }
      return fps/n;
   }

   /** Retourne le nombre de plans images actuellement utilises */
   protected int getNbPlanImg() { return getNbPlanImg(true); }
   protected int getNbPlanImg(boolean withBG) {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( !plan[i].isReady() ) continue;
         if( plan[i].isImage() ) n++;
         if( withBG && plan[i].type==Plan.ALLSKYIMG ) n++;
      }
      return n;
   }

   /**
    * @param class
    * @return le nombre de plans instance of c
    */
   protected int getNbPlanByClass(Class<?> c) {
       int n=0;
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          if( c.isInstance(plan[i]) && plan[i].flagOk ) {
             n++;
          }
       }
       return n;
    }

   /** Retourne le nombre d'images (non BG) sélectionnées */
   protected int getNbPlanImgSelected() {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i] instanceof PlanImage && plan[i].flagOk
               && !(plan[i] instanceof PlanBG) && plan[i].selected ) n++;
      }
      return n;

   }

   /** Retourne le nombre de plans images dont on peut modifier la transparence */
   protected int getNbPlanTranspImg() {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i] instanceof PlanImage && plan[i].flagOk && plan[i].hasCanBeTranspState() ) n++;
      }
      return n;
   }

   /** Retourne le nombre de plans Catalog et assimilés actuellement utilises */
   protected int getNbPlanCat() {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].isCatalog() && plan[i].flagOk ) n++;
      }
      return n;
   }
   
   /** Retourne true s'l y a au-moins un plan actif dont les objets 
    * sont sélectionnables */
   protected boolean hasSelectableObjects() {
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         Plan p = plan[i];
         if( !p.flagOk || !p.active ) continue;
         if( p instanceof PlanContour ) continue;
         if( p.hasObj() || p.hasSources() ) return true;
      }
      return false;
   }

   /** Retourne le nombre de plans Tool actuellement utilises */
   protected int getNbPlanTool() {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i] instanceof PlanTool && plan[i].flagOk ) n++;
      }
      return n;
   }

   /** Retourne le nombre de plans Moc actuellement utilises */
   protected int getNbPlanMoc() {
      int n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type==Plan.ALLSKYMOC && plan[i].flagOk ) n++;
      }
      return n;
   }

   /** Retourne le nombre de sources chargées dans l'ensemble des plans */
   protected long getNbSrc() {
      long n=0;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].isCatalog() && plan[i].flagOk ) n+=plan[i].getCounts();
      }
      return n;
   }

   /** Positionnement du mode du réticule 0-sans, 1-normal, 2-large */
   protected void setReticle(int mode) { 
      reticleMode=mode;
      setOverlayFlag("reticle", mode>0);
   }

   /** Activation/desactivation de la grille */
   protected void setGrid(boolean flag,boolean verbose) {
      if( flag==!hasGrid() ) switchGrid(verbose);
   }

   /** Active/Désactive la grille */
   protected void switchGrid(boolean verbose) {
      if( !hasGrid() ) {
         setOverlayFlag("grid", true);
//         aladin.view.activeGrid();
         if( verbose ) aladin.console.printCommand("grid on");
      }
      else {
         setOverlayFlag("grid", false);
//         aladin.view.unactiveGrid();
         if( verbose ) aladin.console.printCommand("grid off");
      }
      repaintAll();
   }

//   /** Active/Désactive le background */
//   protected void switchPlanBG() {
//      if( planBG.survey==null ) return;
//      ViewSimple v = aladin.view.getCurrentView();
//      if( v.isFree() && !planBG.active ) v.setPlanRef(planBG, false);
//      planBG.active = !planBG.active;
//      aladin.view.repaint();
//   }

   /** Activation/desactivation des informations  */
   protected void setOverlay(boolean flag) { flagOverlay=flag; }
   
   private boolean flagFirstAutoDist=true;
   /** Activation/desactivation de l'outil de mesure automatique des distances */
   protected void setAutoDist(boolean flag) {
      if( flagFirstAutoDist && flag && aladin.configuration.isHelp() ) {
         aladin.configuration.showHelpIfOk("HAUTODIST");
         flagFirstAutoDist=false;
      }
      flagAutoDist=flag;
   }

   private boolean flagFirstSimbad=true;
   /** Activation/desactivation du quick Simbad  */
   protected void setSimbad(boolean flag) {
      if( flagFirstSimbad && flag && aladin.configuration.isHelp() ) {
         aladin.configuration.showHelpIfOk("HFINGER");
         flagFirstSimbad=false;
      }
      flagSimbad=flag;
   }

   private boolean flagFirstVizierSED=true;
   /** Activation/desactivation du quick VizierSED  */
   protected void setVizierSED(boolean flag) {
      if( flagFirstVizierSED && flag && aladin.configuration.isHelp()) {
         aladin.configuration.showHelpIfOk("HFINGERVIZIERSED");
         flagFirstVizierSED=false;
      }
      flagVizierSED=flag;
   }

   /** Retourne le premier plan image qui contient la coordonnée, sinon null */
   protected Plan contains(Coord coo) {
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         Plan p = plan[i];
         if( !p.flagOk || !(p instanceof PlanImage) ) continue;
         if( p.contains(coo) ) return p;
      }
      return null;
   }

  /** Test si un plan est deja charge dans la pile.
   * @param type  Le type de plan
   * @param objet l'objet ou les coordonnees au centre
   * @param param les parametres du plan
   *              (Le format de la chaine param reprend celui de la class Plan)
   * @param other dependant du type de plan, eventuellement null
   * 		  pour IMAGE: concatenation fmt et resol
   * @return      <I>true</I> si le plan decrit n'est pas deja, sinon <I>false</I>
   */
   protected boolean dejaCharge(int type,String objet,String param) {
      return dejaCharge(type,objet,param,null);
   }
   protected boolean dejaCharge(int type,String objet,String param,String other) {
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].theSame(type,objet,param,other) ) return true;
      }
      return false;
   }

   /** Retourne vrai si la pile est vide à part le plan passé en paramètre
    * ou s'il y a une vue libre */
    protected boolean isFreeX(Plan p) {
//       View view = aladin.view;
//       for( int i=0;i<view.getModeView(); i++) {
//          if( view.viewSimple[i].isFree() ) return true;
//       }

       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          if( plan[i]==p ) continue;
           if( plan[i].type!=Plan.NO &&
                 (plan[i] instanceof PlanBG && ((PlanBG)plan[i]).flagOk
                 || plan[i].isSync()) ) {
//              System.out.println(plan[i]+" is sync");
              return false;
           }
       }
       return true;
    }

    /** Retourne vrai si la pile contient au moins une image (ne prend pas en compte
     * le plan passé en paramètre */
    protected boolean hasImage(Plan p) {
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          if( plan[i]==p ) continue;
          if( plan[i].isSync() && (plan[i].isImage() || plan[i].type==Plan.ALLSKYIMG) ) return true;
       }
       return false;
    }

    /** Test s'il n'y a aucun plan utilise */
    protected boolean isFree() { return getNbPlans(false)==0; }

    /** retourne le nombre de plans dans la pile */
    protected int getNbPlans(boolean onlyReady) {
       int n=0;
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type!=Plan.NO ) {
            if( !onlyReady || plan[i].flagOk ) n++;
         }
       }
      return n;
   }

    /** Retourne La liste des noms de plans en cours de traitement (séparés par ,) */
    protected String getBlinkingInfo() {
       StringBuffer s = new StringBuffer();
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          Plan p = plan[i];
          if( p.type==Plan.NO ) continue;
          if( !p.flagOk && p.error==null
                || p.flagProcessing
                || p.type==Plan.IMAGEHUGE && ((PlanImageHuge)p).isExtracting ) {
             if( s.length()>0 ) s.append(", ");
             s.append(p.label);
          }
       }
       return s.toString();
    }

    /** Retourne true si au moins un plan est en cours de traitement */
    protected boolean isBlinking() {
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          Plan p = plan[i];
          if( p.type==Plan.NO ) continue;
          if( !p.flagOk && p.error==null ) return true;
          if( p.flagProcessing ) return true;
          if( p.type==Plan.IMAGEHUGE && ((PlanImageHuge)p).isExtracting ) return true;

       }
       return false;
    }

    /** Retourne true si j'attends le premier plan */
    protected boolean waitingFirst() {
//       if( !aladin.command.isSync() ) return true;
       boolean rep=false;
       for( int i=0; i<plan.length; i++ ) {
          Plan p = plan[i];
          if( p.type==Plan.NO ) continue;
          rep=true;
          if( p.flagOk ) return false;
       }
       return rep;
    }

   /** Libere le plan courant */
   protected void Free() {
      int n;
      if( (n=getFirstSelected())<0 ) return;
      Free(n);
   }

   /**  Libère le plan passé en paramètre */
   protected void Free(Plan p) { Free( getIndex(p)); }

  /** Libere le plan indique
   * @param n numero du plan dans la pile
   */
   protected void Free(int n) {
      if( n!=-1 ) {
         final int [] param = new int[1];
         param[0]=n;
         synchronized( pile ) {
            plan[n].Free();
            if( SwingUtilities.isEventDispatchThread() ) {
               scroll.rm(param[0]);
            } else {
               SwingUtilities.invokeLater(new Runnable() {
                  public void run() { scroll.rm(param[0]); }
               });
            }
         }
      }
      if( SwingUtilities.isEventDispatchThread() ) {
         repaintAll();
      } else {
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { repaintAll(); }
         });
      }
   }

   /** Libere tous les plans */

   protected void FreeAll() {
      Aladin.makeCursor(this,Aladin.WAITCURSOR);
      aladin.view.freeAll();
      synchronized( pile ) {
         for( int i=0; i<plan.length; i++ ) plan[i].Free();
         reinitPlan();
      }
      repaintAll();
      Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
   }

   /** Active/Désactive tous les plans sélectionnés */
   protected void setActivatedSet(boolean flagShow) {

      // Décompte du nombre d'images pour éventuellement passer en mode multiview
      if( flagShow ) {
         int nb=0;
         for( int i=0; i<plan.length; i++ ) {
            if( !plan[i].selected ) continue;
            if( plan[i].isImage() && canBeRef(plan[i]) ) nb++;

         }
         if( nb>aladin.view.getModeView() ) {
            aladin.view.setModeView(
                  nb<=ViewControl.MVIEW2L ? ViewControl.MVIEW2L :
                     nb<=ViewControl.MVIEW4 ? ViewControl.MVIEW4 :
                        nb<=ViewControl.MVIEW9 ? ViewControl.MVIEW9 :
                           ViewControl.MVIEW16);
         }
      }
      for( int i=0; i<plan.length; i++ ) {
         if( !plan[i].selected ) continue;
         if( !flagShow || !plan[i].isImage() ) plan[i].setActivated(flagShow);
         else showPlan(plan[i], true);
      }
   }

   /** Libère tous les plans en erreur ou n'ayant aucun objet catalog */
   protected void FreeEmpty() {
      int i,j;

      synchronized( pile ) {

         // Suppression effective (par décalage)
         for( i=plan.length-1; i>=0; i-- ) {
            if( plan[i].isEmpty() ) {
               if( !plan[i].Free() ) continue;  // Le plan n'est pas libérable
               scroll.rm(i);
               Plan ptmp = plan[i];
               for( j=i; j>1; j-- ) plan[j]=plan[j-1];
               i++;
               plan[1]=ptmp;
            }
         }
      }

      aladin.view.findBestDefault();
      repaintAll();
   }



   /** Libère tous les plans du groupe en cours en les decalant vers le bas */
   protected void FreeSet(boolean verbose) {
      int i,j;

      synchronized( pile ) {
         // Gestion des folders (sélection de tous les plans qui s'y trouvent)
         for( i=plan.length-1; i>=0; i-- ) {
            if( plan[i].selected && plan[i].type==Plan.FOLDER ) {
               Plan p[] = getFolderPlan(plan[i]);
               for( j=0; j<p.length; j++ ) p[j].selected=true;
            }
         }

         // Suppression effective (par décalage)
         for( i=plan.length-1; i>=0; i-- ) {
            if( plan[i].selected ) {
               if( plan[i].type==Plan.NO) continue; // on ne supprime pas un plan vide
               if( verbose ) aladin.console.printCommand("rm "+Tok.quote(plan[i].label));
               if( !plan[i].Free() ) continue;  // Le plan n'est pas libérable
               scroll.rm(i);
//               Plan ptmp = plan[i];
               for( j=i; j>1; j-- ) plan[j]=plan[j-1];
               i++;
//               plan[1]=ptmp;
               plan[0]=new PlanFree(aladin);
            }
         }
      }

      if( isFree() ) zoom.zoomView.free();
      aladin.view.findBestDefault();
      repaintAll();
   }

   // Juste pour accélérer un peu
   protected boolean taggedSrc=false;

   /** Retourne true s'il y a au-moins un objet tagué dans la lsite des plans sélectionnés */
   protected boolean hasTaggedSrc() {
      if( !taggedSrc ) return false;

      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( !plan[i].isCatalog() || !plan[i].flagOk ) continue;
         Iterator<Obj> it = plan[i].iterator();
         while( it.hasNext() ) {
            Obj o = it.next();
            if( !(o instanceof Source) ) continue;
            Source s = (Source)o;
            if( s.isTagged() ) return true;
         }
      }
      return false;
   }

  /** Selection de tous les objets dans les plans selectionnes */
   protected void selectAllObjectInPlans() {
      selectAllObject(1);
   }
   /** Sélection de tous les objets des plans activés
    * @param mode 0-dans tous les plans, 1-dans les plans sélectionnés, 2-les objets tagués
    * @param all true => tous les plans, sinon seulement ceux sélectionnés
    */
   protected void selectAllObject(int mode) {
      aladin.view.deSelect();
      for( int i=0; i<plan.length; i++ ) {
         if( !plan[i].hasObj() ) continue;
         if( plan[i].flagOk && plan[i].active &&(mode==0 || mode==2 || plan[i].selected) ) {
            aladin.view.selectAllInPlanWithoutFree(plan[i],mode==2?1:0);
         }
      }
      // TODO : l'envoi d'un tel message doit il se faire automatiquement ? à réfléchir
      // envoi d'un message PLASTIC de sélection des objets
      if( Aladin.PLASTIC_SUPPORT && aladin.getMessagingMgr().isRegistered() ) {
         try {
      	 	aladin.getMessagingMgr().sendSelectObjectsMsg();
         } catch( Throwable e) {}
      }
      
      aladin.view.repaintAll();
      aladin.mesure.mcanvas.repaint();
   }

   /** Enlève tous les tags sur les sources */
   protected void untag() {
      for( int i=0; i<plan.length; i++ ) {
         if( !plan[i].isCatalog() || !plan[i].flagOk ) continue;
         Iterator<Obj> it = plan[i].iterator();
         while( it.hasNext() ) {
            Obj o = it.next();
            if( !(o instanceof Source) ) continue;
            Source s = (Source)o;
            s.setTag(false);
         }
      }
      repaintAll();
      aladin.mesure.mcanvas.repaint();

   }

  /** Selectionne tous les objets qui se trouve dans le rectangle.
   * Exception faite pour les plans de type FIELD
   * @param r rectangle de selection
   * @return vecteur des objets trouves
   * @see aladin.PlanCat#setMultiSelect(java.awt.Rectangle)
   */
   protected Vector<Obj> setMultiSelect(ViewSimple v,RectangleD r) {
      int i;
      Vector<Obj> res = new Vector<Obj>(5000);

      Plan folder = getMyScopeFolder(v.pref);

      for( i=0; i<plan.length; i++ ) {
         if( !plan[i].active || !plan[i].hasObj()  ) continue;
         if( plan[i].type==Plan.APERTURE ) continue;

         // Pas le même scope
         if( folder!=getMyScopeFolder(plan[i]) ) continue;

         Enumeration<Obj> e = plan[i].setMultiSelect(v,r).elements();
         while( e.hasMoreElements() ) res.addElement(e.nextElement());
      }
      return res;
   }

  /** Retourne tous les objets qui contiennent (x,y).
   * Dans le cas d'un plan FIELD, il suffit qu'un seul objet contienne
   * (x,y) pour que tous les objets du plan soient pris.
   * @param (x,y) coordonnees image
   * @return vecteur des objets trouves
   * @see aladin.PlanCat#getObjWith(int,int)
   */
   protected Vector<Obj> getObjWith(ViewSimple v,double x,double y) {
      int i;
      Vector<Obj> res = new Vector<Obj>(500,500);

      Plan folder = getMyScopeFolder(v.pref);

      Plan [] plan = getPlans();
      for( i=0; i<plan.length; i++ ) {
         Plan p = plan[i];
         if( !p.active ) continue;
         // Selection pour un pylogone
         if( p.type==Plan.TOOL ) 
         
         // Selection pour une FIELD
         if( (!p.isCatalog()
                 && p.type!=Plan.TOOL
                 && p.type!=Plan.APERTURE ) ) continue;
         if( folder!=getMyScopeFolder(p) ) continue;
         Enumeration<Obj> e = p.getObjWith(v,x,y).elements();
         while( e.hasMoreElements() ) res.addElement(e.nextElement());
      }
      return res;
   }

  /** Retourne toutes les Sources indiquees par leur OID
   * @param oid le tableau des OID a trouver
   * @return Le tableau des Sources
   */
   protected Source[] getSources(String oid[]) {
      int i,j,k;
      if( oid==null ) return new Source[0];
      Vector<Source> v = new Vector<Source>(500,500);

      // Pour chaque OID a trouver, parcours de tous les plans CATALOG
      Plan [] plan = getPlans();
      for( k=0; k<plan.length; k++ ) {
         Plan p = plan[k];
         if( p.isSimpleCatalog() || p.pcat==null ) continue;
         for( i=0; i<oid.length; i++ ) {
            Iterator<Obj> it = p.iterator();
            while( it.hasNext() ) {
               Obj o = it.next();
               if( !(o instanceof Source) ) continue;
               Source s = (Source)o;
               String cOid = s.getOID();
               if( cOid!=null && cOid.equals(oid[i]) ) {
                  v.addElement(s);
                  break;
               }
            }
         }
      }

      // Preparation du resultat sous forme d'un tableau
      Source s[] = new Source[v.size()];
      i=0;
      Enumeration<Source> e = v.elements();
      while( e.hasMoreElements() )  s[i++] = e.nextElement();

      return s;
   }

  /** Traitement à  faire suite à un changement de frame */
   protected void resumeFrame() {
      
      int frame = aladin.localisation.getFrame();
      aladin.command.setDrawMode( frame==Localisation.XY ? Command.DRAWXY : Command.DRAWRADEC);

      // Modification des libelles de reperes
      // Modification des frame des projections des plans AllSky
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type==Plan.NO ) continue;
         if( plan[i].type==Plan.TOOL ) ((PlanTool)plan[i]).setIdAgain();
         if( plan[i] instanceof PlanBG && Projection.isOk(plan[i].projd)) {
            if( plan[i].projd.frame != ((PlanBG)plan[i]).getCurrentFrameDrawing() ) {
               plan[i].projd.frame = ((PlanBG)plan[i]).getCurrentFrameDrawing();
               plan[i].syncProjLocal();
               plan[i].resetProj();
            }
         }
      }
      
      // Réaffichage éventuel du formulaire courant pour convertir
      // la position par défaut
//      if( aladin.dialog.isVisible() ){
         Server srv = aladin.dialog.server[aladin.dialog.current];

         // on le vide juste avant en cas de formulaire avec tree, sinon impossible à modifier le target
         if( srv.target!=null && srv.tree!=null ) srv.target.setText("");

//      }
         aladin.dialog.adjustParameters();
         aladin.dialog.resume();
      
      // Conversion de la position d'un cut out pour le frameInfo courant (s'il y a lieu)
      if( aladin.frameInfo!=null ) aladin.frameInfo.initTarget();

      // Invalidation des grilles de coordonnées
      aladin.view.grilAgain();
      aladin.view.repaintAll();
      
   }

   /** Traitement d'un changement de mode d'affichage pixel */
    protected void resumePixel() {
       if( aladin.frameCM!=null ) aladin.frameCM.changePixelUnit();
       if( aladin.calque.freeUnusedPixelsOrigin() ) aladin.gc();
    }

  /** Insertion d'un objet graphique dans le plan courant.
   * Le plan courant doit etre du type TOOL
   * @param newobj l'objet a inserer
   */
   private Obj oNewobj=null;
   protected void setObjet(Obj newobj) {
      if( newobj==oNewobj ) return;   // Déjà inséré juste avant
      Plan pc = selectPlanTool();
      pc.pcat.setObjet(newobj);
      oNewobj=newobj;
   }

  /** Suppression d'un objet.
   * L'objet doit se trouver dans l'un des plans TOOL visibles
   * @param newobj l'objet a supprimer
   */
   protected boolean  delObjet(Obj obj) {
      Plan p = ((Position)obj).plan;
      if( !p.active || p.pcat==null ) return false;
      return p.pcat.delObjet(obj);
   }

   /** Mise à jour des flag des plans:
    *  1) Met à jour les flags ref de tous les plans de la pile
    *  en fonction du nombre de vues qui utilisent ces plans.
    *  Si au-moins une vue utilise un plan particulier, son flag
    *  ref est positionné à true sinon false
    *  2) On redemande l'activation/desactivation souhaité par l'utilisateur
    */
   protected void majPlanFlag() {

   	  // maj des ref
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type==Plan.NO ) continue;
         if( canBeRef(plan[i]) ) {
            plan[i].ref=aladin.view.isUsed(plan[i]);
         }
         // thomas : on ignore les filtres
         if( plan[i].type==Plan.FILTER ) continue;
         plan[i].setActivated();
      }
   }

   /** Met dans le cache et libère le tableau sur les pixels d'origine
    * pour tous les plans images qui ne sont pas visible, ou pour tous les plans
    * images dans le cas où le mode Pixel courant est 8 bits.
    * @return true s'il y a eu une libération effective, sinon false
    */
   protected boolean freeUnusedPixelsOrigin() {
      boolean rep=false;
//      boolean flagFreeAll = aladin.pixel.getPixelMode()==Pixel.LEVEL;
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].type!=Plan.IMAGE && !(plan[i] instanceof PlanBG) || !plan[i].flagOk ) continue;
         if( /* flagFreeAll || */ !plan[i].active
               || ((PlanImage)plan[i]).pixelsOriginFromDisk() ) rep |= ((PlanImage)plan[i]).pixelsOriginIntoCache();
      }
      return rep;
   }


   /** Retourne vrai si le plan peut etre de reference
   * @param p le plan à tester
   */
   protected boolean canBeRef(Plan p) {
      if( p==null ) return false;
      if( p.hasXYorig ) return true;
   	  // ajout thomas : PlanFov peut être un plan de référence
      return (p.isImage() || p.type==Plan.ALLSKYIMG || p.isPlanBGOverlay()
              || p.isCatalog()
              || p.isTool() );
//              || p.type==Plan.APERTURE || p.type==Plan.FOV
//              || p.type==Plan.TOOL && !Projection.isOk(p.projd)) ;
   }
   
  /** Positionne le plan comme etant de reference.
   * On passe l'objet plan lui-meme (pas terrible... je sais, mais il faut
   * que je retrouve son numero).
   * @param p le plan
   * @return <I>true</I> si ca a marche, sinon <I>false</I>
   */
   protected boolean setPlanRef(Plan p) { return setPlanRef(p,aladin.view.getLastNumView(p)); }
   protected boolean setPlanRef(Plan p,int nview) {

      // Seuls les plans images et catalogues peuvent etre de reference
      if( nview<0 || !canBeRef(p) )  return false;

      // On positionne le flag de reference sur le plan d'indice n
      p.ref=true;

      // On fait tout ce qu'il faut pour que la vue soit proprement
      // sélectionnée comme si on avait cliqué sur le logo du plan
      aladin.view.setSelect(nview);

      // On affecte le plan de référence à sa vue
      aladin.view.setPlanRef(nview,p);

      // On sélectionne le plan dans la pile
      selectPlan(p);
      
      return true;
   }
   
   /** Positionne le plan BG comme étant de référence. Si la vue que l'on va utilisée
    * est déjà un plan BG, centre automatiquement la nouvelle vue sur le même champ
    * Si pas possible, garde la position par défaut
    */
   protected boolean setPlanRefOnSameTarget(PlanBG p) {
      int nview = aladin.view.getLastNumView(p);
      if( nview<0 || !canBeRef(p) )  return false;
      ViewSimple v = aladin.view.viewSimple[nview];
      
      if( v.isFree() || !Projection.isOk(v.pref.projd) ) return setPlanRef(p,nview);  // pas possible de se mettre à la même position
      Coord c = v.getProj().getProjCenter();
      aladin.trace(4,"Calque.setPlanRefOnSameTarget() sur "+c);
      double z = v.zoom;
      double fct=1;
      try { fct = p.projd.getPixResAlpha()/v.getProj().getPixResAlpha(); }
      catch( Exception e ) {}
      setPlanRef(p,nview);
      v.getProj().setProjCenter(c.al,c.del);
      v.newView(1);
      v.setZoomRaDec(z*fct,c.al,c.del);
      return true;
   }

  /** Positionne le plan de reference
   * @param n le numero du plan
   * @param nview le numero de la vue
   * @return <I>true</I> si ca a marche, sinon <I>false</I>
   */
   protected boolean setPlanRef(int n) { return setPlanRef(n,aladin.view.getLastNumView(plan[n])); }
   protected boolean setPlanRef(int n,int nview) {
      return setPlanRef(getPlan(n),nview);
   }

   /** Sélection d'un plan particulier */
   protected void selectPlan(Plan p) {
      unSelectAllPlan();
      if( p!=null ) {
         p.selected=true;
         p.setActivated(true);
      }
      select.showSelectedPlan();
   }

   /** Spécifie le plan sous la souris */
   protected void selectPlanUnderMouse(Plan p) {
      unSelectUnderMouse();
      if( p!=null ) p.underMouse=true;
   }

   /** Deselection de tous les plans underMouse */
   protected void unSelectUnderMouse() {
      for( int i=0; i<plan.length; i++ ) plan[i].underMouse=false;
   }

   /** Deselection de tous les plans et leur flag underMouse */
   protected void unSelectAllPlan() {
      for( int i=0; i<plan.length; i++ ) plan[i].selected=plan[i].underMouse=false;
   }

   /** Activation de tous les plans visibles
   protected void activeAllPlan() {
      for( int i=0; i<plan.length; i++ ) {
        if( plan[i].type!=Plan.TOOL && plan[i].type!=Plan.FILTER && plan[i].isViewable() ) {
	       plan[i].setActivated(true);
				// ajout thomas
				if( plan[i].type == Plan.CATALOG ) {
					PlanFilter.updatePlan(plan[i]);
	 }
      }
   }
	}*/

   /** Change le nom d'un plan et réaffiche le tout */
   protected void rename(Plan p,String name) {
      p.setLabel(name);
      repaintAll();
   }

   /** Activation du plan p avec éventuellement scrolling sur
    * la première vue qui le montre.
    * @param p Plan à activer
    * @param flagPaint true si on réaffiche après
    */
   protected void showPlan(Plan p) { showPlan(p,true); }
   protected void showPlan(Plan p,boolean flagPaint) {
//      if( p.active ) return;
      if( select.canBeNewRef(p) ) {
         if( setPlanRef(p) ) aladin.view.newView();
      } else if( !aladin.view.tryToShow(p) ) p.setActivated(true);
      if( flagPaint ) repaintAll();
   }
   
   /** Désactivation de tous les plans BG au-dessus du plan passé en paramètre qui le cache */
   protected void switchOffBGOver(Plan p) {
      Plan [] plan = getPlans();
      int n = getIndex(p);
      for( int i=n-1; i>=0; i-- ) {
         Plan pc = plan[i];
         if( !pc.flagOk ) continue;
         if( !(pc instanceof PlanBG && !pc.isOverlay()) ) continue;
         pc.setActivated(false);
      }
      p.setActivated(true);
   }

   private boolean memoClinDoeil=false; // Vrai si on a mémorisé un état via le clin d'oeil
   
   protected boolean hasClinDoeil() { return memoClinDoeil; }

   /** Pour "oublier" qu'on utilisait l'oeil */
   protected void resetClinDoeil() { memoClinDoeil=false; }

   /** Lorsque l'oeil est cliqué, il y aura mémorisation de l'état d'activation
    * ou non des plans (qui ne sont pas de référence), puis désactivation de ces
    * plans.
    * Si on reclique sur l'oeil, ce sera l'état préalablement mémorisé qui sera
    * restitué.
    * Utilise la variable Claque.memoClinDoeil pour savoir si on a déjà
    * mémorisé un état antérieur. Et Plan.memoClinDoeil pour mémoriser individuellement
    * chaque état de plan.
    */
   protected void clinDoeil() {
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         Plan pc = plan[i];
         if( !pc.flagOk ) continue;
         if( pc.ref ) continue;
         if( memoClinDoeil ) {
            pc.setActivated(pc.memoClinDoeil);
         } else {
            pc.memoClinDoeil=pc.active;
            pc.setActivated(false);
         }
      }
      memoClinDoeil=!memoClinDoeil;
      aladin.view.newView();
   }

   /** Demande l'activation de tous les plans possibles */
   protected void activateAll() {
      boolean atLeastOne=false;
      Plan plan[] = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( !plan[i].flagOk ) continue;
         atLeastOne|=plan[i].setActivated(true);
      }
      if( atLeastOne ) aladin.view.newView();
   }

   /** Descativation des plans qui sont hors vue
    * @return true si au-moins un plan n'est plus visible
   protected boolean unActiveOldPlan() {
      boolean rep=false;

      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].active && !plan[i].isViewable() ) {
            plan[i].setActivated(false);
            rep=true;
         }
      }
      return rep;
   }
   */

   /** Retourne l'index d'un plan, -1 si non trouvé */
   protected int getIndex(Plan p) {
      return getIndex(plan,p);
   }

   protected int getIndex(Plan [] plan,Plan p) {
      if( p==null ) return -1;
      for( int i=0; i<plan.length; i++ ) if( plan[i].type!=Plan.NO && plan[i]==p ) return i;
      return -1;
   }

   /**
    * Retourne l'index du premier plan dont le label correspond
    * au masque passe en parametre
    * @param mask le pattern de recherche avec jokers à la msdos
    * @param mode 0-avec jokers, 1-simple égalité stricte des chaines
    * @return le numero du plan correspondant, -1 si aucun
    */
   protected int getIndexPlan(String mask) { return getIndexPlan(mask,0); }
   protected int getIndexPlan(String mask,int mode) {
      for( int i=0; i<plan.length; i++ ) {
         if( mode==0 && Util.matchMask(mask,plan[i].label)
               || mode==1 && mask.equals(plan[i].label)  ) return i;
      }
      return -1;
   }

   /**
    * Retourne le premier plan dont le label correspond
    * au masque passe en parametre
    * @param mask le pattern de recherche avec jokers à la msdos
    * @param mode 0-avec jokers, 1-simple égalité stricte des chaines
    * @return le plan correspondant, null si aucun
    */
   public Plan getPlan(String mask) { return getPlan(mask,0); }
   protected Plan getPlan(String mask,int mode) {
      int i = getIndexPlan(mask,mode);
      return i==-1 ? null : plan[i];
   }


   /**
    * Retourne la liste des plans valides dont le label correspond
    * au masque passe en parametre
    * @return le Vector des plans, ou null si aucun
    */
   protected Vector getPlans(String mask) {
      Vector v=null;

      // Recherche des plans qui correspondent
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         Plan pc = plan[i];
         if( pc.hasError() ) continue;
         if( Util.matchMask(mask,pc.label) ) {
            if( v==null ) v = new Vector(plan.length);
            v.addElement(pc);
         }
      }
      return v;
   }

   static final int COLIMG  = 0x0100;  // Images couleurs
   static final int CALIB   = 0x0200;  // calibrées astrométriquement
   static final int TRUEIMG = 0x0400;  // Images avec vrais pixels accessibles
   static final int IMG     = 0x0800;  // Tous les types d'images
   static final int SELECTED= 0x0F00;  // Plan sélectionné

   /** Retourne true si le paramètre est une masque de bits indiquant un type
    * spécial de plan  (càd > à 0xFF - on n'utilise pas le premier octet à droite) */
   private boolean isSpecialType(int type) { return type>0xFF; }

   /** Détermine le masque de bits en fonction du type de plan et de ses propriétés */
   private int getSpecialType(Plan p) {
      int type = 0x0000;
      if( p instanceof PlanImage )    type |= IMG;
      if( p instanceof PlanImageRGB ) type |= COLIMG;
      if( !p.hasNoReduction()  )      type |= CALIB;
      if( p.hasAvailablePixels() )    type |= TRUEIMG;
      if( p.selected )                type |= SELECTED;
      return type;
   }

   /**
    * Retourne la liste des plans valides d'un certain type
    * @param type IMAGE|CATALOG... ou 999 pour IMAGE avec pixels originaux
    * @return le Vector des plans, ou null si aucun
    */
   protected Vector<Plan> getPlans(int type) {
      Vector<Plan> v=null;

      // Recherche des plans qui correspondent
      for( int i=0; i<plan.length; i++ ) {
         Plan pc = plan[i];
         if( pc.hasError() ) continue;

         // Sélection par type "spécial" de plan
         if( isSpecialType(type) )  {
            int planSpecialType = getSpecialType(pc);
            if( (type & planSpecialType) == 0 ) continue;

         // Sélection "à l'ancienne" des images (SANS DOUTE A VIRER)
         } else if( type==Plan.IMAGE ) {
            if( pc.type!=Plan.IMAGE
                  && pc.type!=Plan.IMAGEHUGE
                  && pc.type!=Plan.IMAGERSP
                  && pc.type!=Plan.IMAGEALGO
                  && pc.type!=Plan.IMAGEMOSAIC ) continue;

            // Sélectin par types simple de plan
         } else if( pc.type!=type ) continue;

         if( v==null ) v = new Vector<Plan>(plan.length);
         v.addElement(pc);
      }
      return v;
   }

   /**
    * Retourne la liste des plans valides d'une certaine classe
    * @param c la classe Java qui nous interesse
    * @return le Vector des plans, ou null si aucun
    */
   protected Vector<Plan> getPlans(Class<?> c) {
      Vector<Plan> v=null;

      // Recherche des plans qui correspondent
      for( int i=0; i<plan.length; i++ ) {
         Plan pc = plan[i];
         if( pc.hasError() ) continue;

         if( ! c.isInstance(pc)) {
            continue;
         }

         if( v==null ) v = new Vector<Plan>(plan.length);
         v.addElement(pc);
      }
      return v;
   }



   /**
    * Retourne la liste des plans valides d'un certain type
    * @param type IMAGE|CATALOG... ou 999 pour IMAGE avec pixels originaux
    * @return le Vector des plans, ou null si aucun
    */
//   protected Vector getPlans(int t) {
//      Vector v=null;
//
//      int type = t==999 ? Plan.IMAGE : t;
//
//      // Recherche des plans qui correspondent
//      synchronized( pile ) {
//         for( int i=0; i<plan.length; i++ ) {
//            Plan pc = plan[i];
//            if( pc.hasError() ) continue;
//            if( type==Plan.IMAGE ) {
////               if( !(pc instanceof PlanImage) ) continue;
//               if( pc.type!=Plan.IMAGE
//                     && pc.type!=Plan.IMAGEHUGE
//                     && pc.type!=Plan.IMAGERSP
//                     && pc.type!=Plan.IMAGEALGO
//                     && pc.type!=Plan.IMAGEMOSAIC ) continue;
//               if( t==999 && !((PlanImage)pc).hasOriginalPixels() ) continue;
//            } else if( pc.type!=type ) continue;
//            if( v==null ) v = new Vector(plan.length);
//            v.addElement(pc);
//         }
//      }
//      return v;
//   }

   /** Recupere la liste des plans images valides */
   protected Vector<Plan> getPlanImg() { return getPlans(Plan.IMAGE); }

   /** Recupere la liste des plans images valides */
   public Vector<Plan> getPlanBG() { return getPlans(Plan.ALLSKYIMG); }

   /** Recupere la liste des plans catalogues valides */
   protected Vector<Plan> getPlanCat() { return getPlans(Plan.CATALOG); }

   /** Recupère la liste de tous les plans images */
   protected Vector<Plan> getPlanAllImg() { return getPlans(IMG); }

   /** Récupère la liste des plans sélectionnés */
   protected Vector<Plan> getPlanSelected() { return getPlans(SELECTED); }


  /** Retourne le plan de reference courant.
   * @return le plan de ref ou <I>null</I> si aucun
   */
   protected Plan getPlanRef() {
      ViewSimple v = aladin.view.getCurrentView();
      return v==null?null:v.pref;
  }

  /** Retourne l'index du plan de reference
   * @return le numero du plan de ref ou <I>-1</I> si aucun
   */
   protected int getIndexPlanRef() { return getIndex(getPlanRef()); }

  /** Retourne le plan de base (premier raster non-transparent)
   * @return le plan de base ou <I>null</I> si aucun
   */
   public Plan getPlanBase() {
      Plan p = getPlanRef();
      return p!=null && p.isPixel() ? p: null;
   }
   
   /** true si le plan de référence est un plan en mode Allsky 
    * => changement de centre de projection */
   public boolean isModeAllSky() {
      Plan p = getPlanRef();
      return p!=null && p instanceof PlanBG;
   }

   /** Retourne le prochain plan image dans la pile */
   protected Plan nextImage(Plan pref,int sens) {
      int n= getIndex(pref);
      for( int i=n+sens; i!=n; i+=sens ) {
         if( i>=plan.length ) i=0;
         if( i<0 ) i=plan.length-1;
         Plan p = plan[i];
         if( p instanceof PlanImage && p.flagOk ) return p;
      }
      return pref;
   }

  /** Retourne l'index plan de base (plan ref si image et actif)
   * @return le numero du plan de base ou <I>-1</I> si aucun
   */
   protected int getIndexPlanBase() {
      int i = getIndexPlanRef();
      if( i==-1 || !plan[i].isImage() || !plan[i].active ) return -1;
      return i;
  }

   /** Retourne la liste des labels des plans en commençant par celui du
    * bas de la pile */
   protected String [] getStackLabels() {
      Vector v = new Vector();
      for( int i=plan.length-1; i>=0; i-- ) {
         if( plan[i].type==Plan.NO ) continue;
         v.add(plan[i].label);
      }
      String s [] = new String[v.size()];
      Enumeration e = v.elements();
      for( int i=0; e.hasMoreElements(); i++ ) s[i] = (String)e.nextElement();
      return s;
   }

   /** Découpage du chaine de mesures d'info statistiques sur les pixels
    * afin de préparer une entrée dans le plan "Photometry" */
//   private String [] splitVal(String id,Position o) {
//      String [] r = new String[9];
//      Coord c = new Coord(o.raj,o.dej);
//      r[0] = id;
//      r[1] = o.raj+"";
//      r[2] = o.dej+"";
//      StringTokenizer tok = new StringTokenizer(o.id,"/");
//      for( int i=3; i<=7; i++ ) {
//         String s = tok.nextToken().trim();
//         int offset = s.indexOf(' ');
//         r[i] = s.substring(offset+1);
//      }
//      r[8] = o instanceof Repere ? "Circle "+Coord.getUnit( ((Repere)o).getRadius()) : "Polygon";
//      return r;
//   }
   
   
   /** Met à jour le plan "Photometry" en fonction des paramètres de iqe */
   protected void updatePhotometryPlane(Repere r, double [] iqe) {
      
      final PlanTool p = selectPlanTool();
      final Source s = p.addPhot( (PlanImage)aladin.view.getMouseView().pref, r.raj, r.dej, iqe);
      
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            Util.pause(100);
            s.setSelected(true);
            p.updateDedicatedFilter();
            repaintAll();
         }
      });
   }
   
   /** Met à jour le plan "Tag" */
   protected void updateTagPlane(Tag tag) {
      
      final PlanTool p = selectPlanTool();
      final Source s = p.addTag( (PlanImage)aladin.view.getMouseView().pref, tag.raj, tag.dej);
      
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            Util.pause(100);
            s.setSelected(true);
            p.updateDedicatedFilter();
            repaintAll();
         }
      });
   }
   

   Plan planRotCenter=null; // mémorise le PlanField donc on déplace uniquement le centre de rotation

  /** Recalcul les plans FoV en fonction de leur centre en x,y,al,del
   * et de leur rotation
   *  Necessaire apres une translation par la souris
   * @param flag ViewSimple.MOVE | ViewSimple.ROLL | ViewSimple.MOVECENTER
   * @param aperture la liste des plans apertures concernés
   * @param n nombre de plans dans aperture
   */
   protected void resetPlanField(int flag,PlanField[] plan,int n) {
      for( int i=0; i<n; i++ ) {
//         if( plan[i]==planRotCenter ) plan[i].moveRotCenter();
         plan[i].reset(flag);
      }
   }

  /** Cree un nouveau plan Filter
   * @param aladinLabel Le nom du plan
   * @param script le code initial du script
   * @return le plan créé
   */
   protected Plan newPlanFilter() { return newPlanFilter(null,null); }
   protected Plan newPlanFilter(String label,String script) {
      Plan pc = null;
      int n=getStackIndex();
      plan[n] = pc = new PlanFilter(aladin,label,script);
      PlanFilter.updateAllFilters(aladin);
      return pc;
   }

   protected Plan createFolder(String label,int folderNiv,boolean localScope) {
      return plan[ newFolder(label,folderNiv,localScope) ];
   }

  /** Cree un nouveau plan Folder
   * @param label Le nom du plan
   * @param le niveau de folder
   * @param Etendue des prorpriétés du folder
   * @return le numero du plan dans la pile
   */
   protected int newFolder(String label,int folderNiv,boolean localScope) {
      int n= n=getStackIndex();
      plan[n] = new PlanFolder(aladin,label,folderNiv,localScope);
      return n;
   }
   
   static final String  DMAPGLU = "getDMap";
   
   /** Chargement de la carte de densité associée à un catalogue */
   protected int newPlanDMap(String catID) throws Exception {
      String u = ""+aladin.glu.getURL(DMAPGLU,aladin.glu.quote(catID));
      String label = "DMAP "+catID;
      int n=getStackIndex(label);
      try {
         plan[n] = new PlanHealpixDMap(aladin,u, label);
      } catch( Exception e ) {
         plan[n].error=e.getMessage();
         throw e;
      }
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }

  /** Enregistre une image RGB dans le prochain plan libre.
   * @param r le plan Red
   * @param g le plan Green
   * @param b le plan Blue
   * @param ref le plan de reference du reechantillonage
   * @param label le nom du futur plan , ou null si défaut
   * @param diff true s'il s'agit d'une difference sur 2 plans
   */
   protected int newPlanImageRGB(PlanImage r,PlanImage g,PlanImage b,
                                 PlanImage ref,String label,boolean diff) {
      int n=getStackIndex(label);
      label=prepareLabel(label);
      plan[n] = new PlanImageRGB(aladin,r,g,b,ref,label,diff);
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }

   /** Modifie un planImage RGB en PlanImage (passage de la couleur en niveau de gris
    * @param porig Le plan original
    */
   protected int newPlanImage(PlanImageRGB porig) {
      PlanImage p2 = greyPlan(porig);

      // Je substitue la vue sur le plan d'origine par la vue sur
      // le plan resamplé
      ViewSimple v = aladin.view.getFirstSelectedView(porig);
      if( v!=null ) { v.pref=p2; v.repaint(); }


      return 1;
   }

   /** Créé (ou modifie) un plan resamp
    * @param porig Le plan original (ou déjà resamplé)
    * @param p Le plan dont on utilise l'astronomie
    * @param methode PPV ou BIL
    * @param fullPixel true si on travaille sur les vrais pixels
    * @param keepOrig true si on doit garder l'image d'origine (si non déjà résamplée)
    */
   protected int newPlanImageResamp(PlanImage porig, PlanImage p,
         String label,int methode,boolean fullPixel,boolean keepOrig) {

      // Faut-il créer un clone du plan ou travailler directement sur
      // le plan ?
      PlanImageResamp p2;
      if( porig.type != Plan.IMAGERSP ) {
         if( keepOrig ) {
            try { p2=(PlanImageResamp)dupPlan(porig,label,Plan.IMAGERSP,true); }
            catch( Exception e ) { p2=null; }
            porig.selected=false;
         } else {
            p2 = rspPlan(porig);
         }
         if( label==null ) p2.setLabel("Rsp " + p2.label);

         // Je substitue la vue sur le plan d'origine par la vue sur
         // le plan resamplé
         ViewSimple v = aladin.view.getFirstSelectedView(porig);
         if( v!=null ) { v.pref=p2; v.repaint(); }

      } else p2=(PlanImageResamp) porig;
      p2.launchResampleBy(p,methode,fullPixel);
      return 1;
   }
   
   /** Crée un plan MOC en faisant un crop */
   protected int newPlanMoc(String label,PlanMoc source,Coord [] coo) {
      int n;
      PlanMoc pa;

      if( label==null && source!=null ) {
         PlanMoc p1 = source;
         label = "="+p1.getUniqueLabel("["+p1.getLabel()+"]");
      }

      n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = pa = new PlanMocAlgo(aladin,label,source,coo);
      if( isNewPlan(label) ) { n=bestPlace(n); pa.folder=0; }
      suiteNew(pa);
      return n;
   }


   
   /** Crée un plan MOC en fonction d'un ou plusieurs plans MOCs et d'un opérateur */
   protected int newPlanMoc(String label,PlanMoc [] pList,int op,int order) {
      int n;
      PlanMoc pa;

      if( label==null ) {
         PlanMoc p1 = pList[0];
         label = "="+p1.getUniqueLabel("["+p1.getLabel()+"]");
      }

      n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = pa = new PlanMocAlgo(aladin,label,pList,op,order);
      if( isNewPlan(label) ) { n=bestPlace(n); pa.folder=0; }
      suiteNew(pa);
      return n;
   }
   
   /** Crée un plan MOC à la résolution indiquée à partir d'une liste d'images et de catalogues. */
   protected int newPlanMocColl(Aladin aladin,String label,String directory,int order,
         boolean strict,boolean recursive,double blank,int [] hdu) {
      int n;
      PlanMoc pa;

      n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = pa = new PlanMocColl(aladin,label,directory,order,strict,recursive,blank,hdu);
      if( isNewPlan(label) ) { n=bestPlace(n); pa.folder=0; }
      suiteNew(pa);
      return n;
   }


   /** Crée un plan MOC à la résolution indiquée à partir d'une liste d'images et de catalogues. */
   protected int newPlanMoc(String label,Plan [] p,int res,double radius, double pixMin, double pixMax) {
      int n;
      PlanMoc pa;

      if( label==null ) label = "="+p[0].getUniqueLabel("["+p[0].getLabel()+"]");

      n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = pa = new PlanMocGen(aladin,label,p,res,radius,pixMin,pixMax);
      if( isNewPlan(label) ) { n=bestPlace(n); pa.folder=0; }
      suiteNew(pa);
      return n;
   }


   /** Crée une image Algo sur la pile avec l'algo suivant : "p1 fct p2" ou "p1 fct coef" si p2
    * est nul. Si p1 est nul, la première opérande sera le plan de base lui-même et le résultat
    * sera affecté au plan de base (pas de création de plan dans la pile)
    * @param p1 le plan correspondant à la première opérande ou nul si plan de base
    * @param p2 le plan corresondant à la deuxième opérande
    * @param fct 0:add, 1:sub, 2:mul, 3:div, 4:norm
    * @param coef si p2 est nul, opérande 2 constante
    * @param conv nom de la convolution ou null si inutilisé
    * @param methode 0:PPV, 1:BILINEAR
    * @return le numéro du plan dans la pile
    */
   protected int newPlanImageAlgo(String label,PlanImage p1, PlanImage p2,int fct,double coef,String conv,int methode) {
      int n;
      PlanImageAlgo pa;

      // Travaille sur le plan de base
      if( p1==null ) {
         p1 = (PlanImage)getPlanBase();
         if( label==null ) label = "="+p1.getLabel();

      // Va créer un nouveau plan pour le résultat
      } else if( label==null ) label = "="+p1.getUniqueLabel("["+p1.getLabel()+"]");

      n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = pa = new PlanImageAlgo(aladin,label,p1,p2,fct,coef,conv,methode);
      if( isNewPlan(label) ) { n=bestPlace(n); pa.folder=0; }
      suiteNew(pa);
      return n;
   }

   /** Crée une image HealpixAlgo sur la pile avec l'algo suivant : "p1 fct p2" ou "p1 fct coef" si p2
    * est nul. Si p1 est nul, la première opérande sera le plan de base lui-même et le résultat
    * sera affecté au plan de base (pas de création de plan dans la pile)
    * @param p1 le plan correspondant à la première opérande ou nul si plan de base
    * @param p2 le plan corresondant à la deuxième opérande
    * @param fct 0:add, 1:sub, 2:mul, 3:div, 4:norm
    * @param coef si p2 est nul, opérande 2 constante
    * @return le numéro du plan dans la pile
    */
   protected int newPlanHealpixAlgo(String label,PlanHealpix p1, PlanHealpix p2,int fct,double coef) {
      int n;
      PlanHealpixAlgo pa;

      // Travaille sur le plan de base
      if( p1==null ) {
         p1 = (PlanHealpix)getPlanBase();
         if( label==null ) label = "="+p1.getLabel();

      // Va créer un nouveau plan pour le résultat
      } else if( label==null ) label = "="+p1.getUniqueLabel("["+p1.getLabel()+"]");

      n=getStackIndex(label);
      label = prepareLabel(label);

      System.out.println(p1.getNSideFile());
      System.out.println(p2.getNSideFile());
      if(p2!=null) {
         if(p1.getNSideFile()!=p2.getNSideFile() || p1.getCoordsys()!=p2.getCoordsys()) {
            Aladin.warning("Operation on planes with different nside or coordinate system not available yet !", 1);
            return -1;
         }
      }

      plan[n] = pa = new PlanHealpixAlgo(aladin,label,p1,p2,fct,coef);
      if( isNewPlan(label) ) n=bestPlace(n);
      suiteNew(pa);
      return n;
   }

  /** Enregistre une image Blink dans le prochain plan libre. */
   protected int newPlanImageBlink(PlanImage p[],String label,int delay) {
      int n=getStackIndex(label);
      label=prepareLabel(label);
      plan[n] = new PlanImageBlink(aladin,p,label,delay);
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }

   /** Enregistre une image Mosaic dans le prochain plan libre.
    * Remplace la vue passée en paramètre si != null
    * */
   protected int newPlanImageMosaic(PlanImage p[],String label,ViewSimple v) {
      int n=getStackIndex(label);
      label=prepareLabel(label);
      plan[n] = new PlanImageMosaic(aladin,p,label,v);
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }

  /** Enregistre une image Aladin dans le prochain plan libre.
   * @param u     l'URL qu'il va falloir appeler
   * @param label le nom du plan (dans la pile des plans)
   * @param objet le target central (objet ou coord)
   * @param param les parametres de l'image (SERC J MAMA...)
   * @param fmt   le format de l'image (JPEG, FITS...)
   * @param res La resolution de l'image (FULL, PLATE...)
   */
   protected int newPlanImage(URL u,int orig, String label, String objet,
                                    String param,String from,
                                    int fmt,int res,
                                    Obj o) {
      return newPlanImage(u,null,orig,label,objet,param,from,fmt,res,o);
   }
   /** Enregistre une image Aladin dans le prochain plan libre.
    * @param u     l'URL qu'il va falloir appeler
    * @param label le nom du plan (dans la pile des plans)
    * @param objet le target central (objet ou coord)
    * @param param les parametres de l'image (SERC J MAMA...)
    * @param fmt   le format de l'image (JPEG, FITS...)
    * @param res La resolution de l'image (FULL, PLATE...)
    * @param imgNode noeud image décrivant l'image à charger
    */
    protected int newPlanImage(URL u,int orig, String label, String objet,
                                     String param,String from,
                                     int fmt,int res,
                                     Obj o, ResourceNode imgNode) {
       int n=getStackIndex(label);
       label = prepareLabel(label);
       plan[n] = new PlanImage(aladin,null,orig,u,label,objet,param,from,
             fmt,res,o,imgNode);
       n=bestPlace(n);
       suiteNew(plan[n]);
       return n;
    }
   protected int newPlanImage(URL u,MyInputStream inImg,int orig, String label, String objet,
                                    String param,String from,
                                    int fmt,int res,
                                    Obj o) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = new PlanImage(aladin,inImg,orig,u,label,objet,param,from,
            fmt,res,o);
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }

   protected int newPlanImage(URL u,MyInputStream inImg,int orig, String label, String objet,
         String param,String from,
         int fmt,int res,
         Obj o, ResourceNode imgNode) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      long type=0;
      try { type=inImg.getType();  } catch( Exception e ) {};
      if( (type&MyInputStream.CUBE)!=0 ) {
         plan[n] = new PlanImageCube(aladin,null,inImg,label,from,o,imgNode,false,true,null);
      } else {
         plan[n] = new PlanImage(aladin,inImg,orig,u,label,objet,param,from,
               fmt,res,o, imgNode);
      }
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }

   // du bricolo pas beau !
   // c'est vraiment le bordel entre les url et les input stream
   protected int newPlanImageCube(URL u,MyInputStream inImg,int orig, String label, String objet,
	        String param,String from,
	        int fmt,int res,
	        Obj o, ResourceNode imgNode) {
      if( inImg==null ) {
         try {
            inImg = Util.openStream(u);
//            inImg = new MyInputStream(u.openStream());
//            inImg = inImg.startRead();
         }
         catch(Exception e) {
            e.printStackTrace();
         }
      }
      int n=getStackIndex();

      plan[n] = new PlanImageCube(aladin,null,inImg,label,from,o,imgNode,false,true,null);
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
	}


   /** Determine la meilleure place A PRIORI dans la pile des plans
    * pour le prochain plan image en fonction de l'objet central
    * et du type des plans deja presents
    * @param n place actuelle du plan a placer
    */
   private int bestPlace(int n) {
      int i;

      Plan [] plan = getPlans();
      Plan p = plan[n];
      int folder = p.folder;

      // Les plans BG se mettent tjs en bas de la pile
      // juste au dessous du dernier plan qui n'est pas BG
      if( plan[n].type==Plan.ALLSKYIMG ) {
         for( i=plan.length-1; i>=0; i-- ) {
            if( plan[i].type!=Plan.ALLSKYIMG ) break;
         }
         if( i+1==n ) return n; // inutile, c'est moi-même
         if( i!=n ) permute(plan[n],plan[i]);
         p.folder=folder;
         return i;
      }

      String t1 = plan[n].getTargetQuery();
      Plan pc;

      for( i=n+1; i<plan.length &&
      ((pc=plan[i]).type==Plan.FILTER || pc.type==Plan.FOV ||
         pc.isOverlay() && (t1.equals(pc.getTargetQuery()) || pc instanceof PlanBG)); i++);
      if( i>plan.length || i<=0 || i-1==n ) return n;
      permute(plan[n],plan[i-1]);   //On permute les plans pour prendre en compte le folder
      p.folder=folder;
      return i-1;
   }

   /** Determine la meilleure place A POSTERIORI dans la pile des plans
    * pour le prochain plan image en fonction de la projection
    * @param val place actuelle du plan a placer
    */
   protected void bestPlacePost(Plan p) {
      int i;
      if( p.noBestPlacePost || p instanceof PlanBG /* || p.isImage() */ || !Projection.isOk(p.projd) ) return;
//System.out.println("BestPlacePost pour "+p+" "+Thread.currentThread().getId());

      boolean overlay = p.isOverlay();
      int folder = p.folder;
      Plan pc;
      int n = getIndex(p);
//      for( i=n+1; i<plan.length &&
//      ((pc=plan[i]).type==Plan.FILTER || pc.type==Plan.FOV || !pc.flagOk ||
//            (pc.isCatalog()
//                  || pc.type==Plan.FOLDER
//                  || pc.type==Plan.APERTURE
//                  || pc.type==Plan.TOOL && pc instanceof PlanContour)
//                  && p.projd.agree(pc.projd,null)); i++);
      
      for( i=n+1; i<plan.length &&
      ((pc=plan[i]).type==Plan.FILTER || pc.type==Plan.FOV || !pc.flagOk ||
            !overlay && pc.isOverlay() && p.projd.agree(pc.projd,null)); i++);

      if( i>plan.length || i<=0 || i-1==n ) return;
      permute(p,plan[i-1]);   //On permute les plans pour prendre en compte le folder
      p.folder=folder;
   }

   /** Duplique le plan courant en insérant la copie juste au-dessus
    * du plan original dans le stack.
    * NE MARCHE POUR LE MOMENT QUE POUR
    * LES IMAGES simple. IL FAUDRAIT AJOUTER LES METHODES copy() POUR LES
    * PLANS CATALOGUES... POUR QUE CELA MARCHE POUR TOUT TYPE
    * DE PLAN.
    * JE N'EN AI PAS BESOIN POUR LE MOMENT
    * @param p le plan à copier
    * @param label le label du nouveau plan (si null, ancien_nom~nn)
    * @param type le type de la classe du Plan (PlanImage, PlanImageResamp...), ou -1
    *     si même classe que le type d'origine
    * @param flagIns Insertion du plan juste au-dessus, sans création de nouvelle vue
    */
   protected Plan dupPlan(PlanImage p,String label,int type,boolean flagIns) throws Exception {
      if( !(p.isImage() || p instanceof PlanBG) ) throw new Exception("Not yet supported for this kind of plane");
//      if( !p.hasAvailablePixels() ) throw new Exception("Not yet supported for this kind of plane");
       if( !p.flagOk ) throw new Exception("This plane is not yet ready");

       Plan pc=null;
       synchronized( pile ) {
          int n = getStackIndex(label);
          int m = getIndex(p);

          // Insertion du plan juste au-dessus, sans création de nouvelle vue
          if( flagIns ) {
             for( int i=n; i<m-1; i++ ) plan[i]=plan[i+1];
             n = m-1;
          }

          // Duplication effective
          if( n!=m ) {
             if( type==-1 ) type = p.type;
             switch( type ) {
                case Plan.IMAGERGB:
                case Plan.IMAGECUBERGB :
                   pc = new PlanImageRGB(aladin,p);
                   break;
                case Plan.IMAGEALGO :
                   pc = new PlanImageAlgo(aladin,p);
                   break;
                case Plan.IMAGERSP :
                   pc = new PlanImageResamp(aladin,p);
                   break;
                case Plan.IMAGEMOSAIC :
                   pc = new PlanImageMosaic(aladin,p);
                   break;
                case Plan.IMAGE :
                case Plan.IMAGEBLINK :
                case Plan.IMAGECUBE :
                   pc = new PlanImage(aladin,p);
                   break;
                case Plan.ALLSKYIMG :
                   if( ((PlanBG)p).color ) pc = new PlanImageRGB(aladin,p);
                   else pc = new PlanImage(aladin,p);
                   break;
                case Plan.IMAGEHUGE :
                   pc = new PlanImage(aladin,p);
                   ((PlanImage)pc).initZoom /= ((PlanImageHuge)p).getStep();
                   break;
             }
             plan[n]=pc;
             label = prepareLabel(label);
             if( label==null ) label="["+p.label+"]";
             pc.setLabel(label);
             
             // VOIR MODIF PF JAN 12 ci-dessous (ligne à supprimer pour revenir à l'état antérieur)
//             pc.isOldPlan=false;

          }  else {
             p.isOldPlan=true;
             pc=p;
          }
       }

       suiteNew(pc);

       // MODIF PF JAN 12 pour que le nouveau plan deviennent celui de référence
       if( flagIns ) {
          pc.selected=false;
          pc.active=false;
       } else {
          pc.folder=0;
          pc.planReady(true);
       }
//       if( !flagIns ) pc.folder=0;
//       pc.planReady(true);
       
       return pc;
    }

   protected PlanImageResamp rspPlan(PlanImage p) {
      if( p.type!=Plan.IMAGE ) return null;
      Plan pc=null;
      int n=getIndex(p);
      plan[n] = pc = new PlanImageResamp(aladin,p);
      suiteNew(pc);
      return (PlanImageResamp)pc;
   }

   protected PlanImageAlgo algoPlan(PlanImage p) {
      if( !p.isSimpleImage() ) return null;
      Plan pc=null;
      int n=getIndex(p);
      plan[n] = pc = new PlanImageAlgo(aladin,p);
      suiteNew(pc);
      return (PlanImageAlgo)pc;
   }

   /** Conversion d'un plan image couleur en plan image niveau de gris */
   protected PlanImage greyPlan(PlanImageRGB p) {
      if( p.type!=Plan.IMAGERGB ) return null;
      PlanImage p2 = new PlanImage(aladin,p);
      p2.type = Plan.IMAGE;
      p2.pixelsOrigin = p.getGreyPixels();
      p2.bitpix=8;
      p2.npix=1;
      p2.setBufPixels8(p2.getPix8Bits(null, p2.pixelsOrigin, 8, p.width, p.height, p.dataMinFits, p.dataMaxFits, false));
      p2.calculPixelsZoom();
      p2.fmt=PlanImage.JPEG;   // Comme ça on n'inverse pas les pixels à la sauvegarde !!
      p2.video=PlanImage.VIDEO_NORMAL;
      p2.cm = ColorMap.getCM(0,128,255,p2.video==PlanImage.VIDEO_INVERSE, 
            aladin.configuration.getCMMap(),aladin.configuration.getCMFct());
      p2.cmControl[0] = 0; p2.cmControl[1] = 128; p2.cmControl[2] = 255;
      p2.pixMode = PlanImage.PIX_256;
      p2.changeImgID();
      p2.setPourcent(-1);

      int n = getIndex(p);
      plan[n]=p2;
      suiteNew(p2);
      p.Free();
      return p2;
   }

   /** Pour l'interface VOApp */
   protected int newPlanImage(MyInputStream inImg,String label) {
      Plan pc=null;
      int n=-1;
      n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = pc = new PlanImage(aladin,inImg,label);
      suiteNew(pc);
      return n;
   }

   protected int newPlanImage(InputStream inImg,String label, String from) {
       Plan pc=null;
       int n=-1;
       n=getStackIndex(label);
       label = prepareLabel(label);
       plan[n] = pc = new PlanImage(aladin,inImg,label,from);
       suiteNew(pc);
       return n;
    }

   /**
    * Interprétation de numéros d'extensions fits qui auraient été indiqués
    * en suffixe du nom du fichier suivant la syntaxe suivante
    * [a1,a2,a3-a4...] (la virgule peut être remplacé par un ;
    * @param file le nom du fichier
    * @return le tableau contenant les numéros d'extension ou null si pas d'indication
    * ATTENTION, le tableau n'est pas nécessairement trié et il peut y avoir des
    * redondances.
    */
   protected int [] getNumExt(String file) {
      String s = file;
      int x[] = new int[1000];
      int nx=0;
      try {
         int i = s.lastIndexOf('[');
         if( i<0 ) return null;
         int j = s.indexOf(']',i);
         if( j<0 || j!=s.length()-1 ) return null;
         s = s.substring(i+1,j);
         StringTokenizer st = new StringTokenizer(s,",;");
         while( st.hasMoreTokens() ) {
            String t = st.nextToken();
            i=t.indexOf('-');
            if( i>0 ) {
               int a1 = Integer.parseInt(t.substring(0,i));
               int a2 = Integer.parseInt(t.substring(i+1));
               for( i=a1; i<=a2; i++ ) x[nx++] = i;
            } else x[nx++] = Integer.parseInt(t);
            if( nx==1000 ) {
               aladin.warning(this,"Too many Fits extension/frame designation ");
               return null;
            }
         }
      } catch( Exception e ) {
         aladin.warning(this,"Bad FITS extension/frame designation "+s);
         return null;
      }
      int y[] = new int[nx];
      System.arraycopy(x,0,y,0,nx);
      return y;
   }


   /** Création des plans associés à un flux FITSEXTENSION.
    * Utilise un PlanFolder qui contiendra toutes les extensions
    */
   protected void newFitsExt(String file,MyInputStream in,String label,Obj o,String target,String radius) {
      if( file!=null && label==null ) {
         int i = file.lastIndexOf(Util.FS);
         label=(i>=0)?file.substring(i+1):file;
      } else if( label==null ) label="Fits ext";

      waitLock();
      _target=target;
      _radius=radius;
      _file = file;
      _in   = in;
      _firstPlan = new PlanImage(aladin);
      _firstPlan.dis=in;
      _firstPlan.setLabel(label);
      _firstPlan.flagOk=false;
      synchronized( pile ) {
         int n=getStackIndex();
         plan[n] =_firstPlan;
      }
      int n = label.lastIndexOf('.');
      _label = n>0 ? label.substring(0,n) : label;
      _o = o;
      select.repaint();

      Thread runme = new Thread(this,"AladinFitsExtQuery");
      Util.decreasePriority(Thread.currentThread(), runme);
//      runme.setPriority( Thread.NORM_PRIORITY -1);
      Plan.aladinQueryThread(runme);
      
      runme.start();
   }

   private String _target;
   private String _radius;
   private String _file;
   private MyInputStream _in;
   private Plan _firstPlan;
   private String _label;
   private Obj _o;

   // Gestion d'un lock pour passer les arguments au thread
   private boolean lock;
   private final Object lockObj= new Object();
   private void waitLock() { while( !getLock() ) Util.pause(10); }
   private void unlock() { lock=false; }
   private boolean getLock() {
      synchronized( lockObj ) {
         if( lock ) return false;
         lock=true;
         return true;
      }
   }

   public void run() { newFitsExtThread(); }

   /**
    * Indique si l'extension n doit être retenue. Pour cela, soit numext==null
    * ou n est présent dans le tableau. Si c'est le cas, la valeur est mise à -1 pour pouvoir
    * détecter lorsque l'on aura trouvé toutes les extensions souhaitées (voir allFitsExt())
    */
   private boolean keepFitsExt(int n,int numext[] ) {
      if( numext==null ) return true;
      for( int i=0; i<numext.length; i++ ) if( numext[i]==n ) { numext[i]=-1; return true; }
      return false;
   }

   /**
    * Retourne true si on a récupéré toutes les extensions fits souhaitées,
    * soit numext==null soit toutes ses valeurs sont à -1
    * @param numext la liste des extensions souhaitées
    * @return true si toutes les extensions ont été trouvées
    */
   private boolean allFitsExt(int numext[] ) {
      if( numext==null ) return false;   // si null, on va jusqu'au bout
      for( int i=0; i<numext.length; i++ ) if( numext[i]!=-1 ) return false;
      return true;
   }

   /**
    * Chargement d'un fichier Fits extension.
    * Crée un folder (clignotant le temps du chargement) qui contient
    * tous les élements.
    * 1) Se fait dans un thread particulier (inhibition du threading interne
    * à PlanImage() et PlanCatalog()) pour séquentialiser le chargement.
    * 2) Le stream n'est pas fermé dans PlanImage() et PlanCatalog().
    * 3) Le traitement des erreurs est particulier dans le sens où une erreur
    * n'empêche pas le chargement du plan suivant. Il a donc fallu utiliser
    * une ruse pour détecter correctement la fin du flux (via le champ
    * error du dernier plan qui prend alors la valeur _END-XFITS_).
    */
   private void newFitsExtThread() {
      Plan p=null;
      String target    = _target;
      String radius    = _radius;
      String file      = _file;
      MyInputStream in = _in;
      String label     = _label;
      Plan folder = new PlanFolder(aladin,label);
      Plan firstPlan=_firstPlan;
      Obj o = _o;
      int step=0;

      int numext[]=null;
      if( file!=null ) numext = getNumExt(file);

      unlock();
      
      Vector v = new Vector();
      try {
         for( int nExt=0; !allFitsExt(numext); nExt++ )  {
            boolean keepIt = keepFitsExt(nExt,numext);  // Pour savoir s'il faut garder cette extension
            p=null;
            in.resetType();
            long type = in.getType();
            Aladin.trace(3,"MultiExtension "+nExt+" detect => "+MyInputStream.decodeType(type));

            if( (type & MyInputStream.FITS)!=0 /* || (type & MyInputStream.HPX)!=0 */ ) {
               PlanImage pi = null;
               if( (type&MyInputStream.CUBE)!=0 ) {
                  if( (type&MyInputStream.ARGB)!=0) pi = new PlanImageCubeRGB(aladin,file,in,label,null,o,null,false,false,null);
                  else pi = new PlanImageCube(aladin,file,in,label+"["+nExt+"]",null,o,null,!keepIt,false,firstPlan);
               } else if( (type & MyInputStream.HUGE)!=0 ) {
                  pi = new PlanImageHuge(aladin,file,in,label+"["+nExt+"]",null,o,null,!keepIt,false,firstPlan);
               } else if( (type & MyInputStream.HEALPIX)!=0 ) {
                  pi = new PlanHealpix(aladin,file,in,label+"["+nExt+"]",PlanBG.DRAWPIXEL,0, false,
                        getTargetBG(target, null),getRadiusBG(target, radius, null));
               } else {
                  pi = new PlanImage(aladin,file,in,label+"["+nExt+"]",null,o,null,!keepIt,false,firstPlan);
               }

               if( nExt==0 && pi.error!=null && pi.error.equals("_HEAD_XFITS_") ) {
                  ((PlanFolder)folder).headerFits = pi.headerFits;
               } else {
                  if( pi.error==null || pi.error.equals("_END_XFITS_") ) pi.pixelsOriginIntoCache();
                  p = pi;
               }
            } else if( (type & (MyInputStream.FITST|MyInputStream.FITSB))!=0 ) {
               if( (type & MyInputStream.RICE)!=0 ) {
                  p = new PlanImageRice(aladin,file,in,label,null,o,null,!keepIt,false,firstPlan);
               } else if( (type & MyInputStream.AIPSTABLE)!=0 ) {
                  Aladin.trace(3,"MEF AIPS CC table detected => ignored !");
                  new PlanCatalog(aladin,"",in,true,false);  // Juste pour le manger
               } else {
                  PlanCatalog pc = new PlanCatalog(aladin,""/*file*/,in,!keepIt,false);
                  if( pc.label.equals("") ) pc.setLabel(file);
                  p=pc;
                  if( /* aladin.OUTREACH && */ pc.pcat.badRaDecDetection 
                        && nExt>0 && v.size()>0 && ((Plan)v.elementAt(0)).isImage() ) {
                     p=null; // pour eviter les extensions DSS
                     aladin.command.printConsole("!!! Table MEF extension ignored => seems to be reduction information");
                  }
               }
           } else {
              Aladin.trace(3,"One MEF extension not supported => ignored!");
              break;
            }

            if( folder!=null ) folder.setPourcent(nExt);

            if( p!=null ) {

               // Pas terrible: pour détecter la fin de fichier on passe par
               // le p.error avec une chaine particulière
               if( p.error!=null && p.error.equals("_END_XFITS_") ) break;

               if( keepIt ) {

                  p.askActive= true;
                  p.selected = false;
                  v.add(p);

                  // Placement d'un label si ce n'est déjà fait
                  if( p.label==null || p.label.equals("") || p.label.startsWith("~")) {
                     p.setLabel(label+ (v.size()>0?"["+nExt+"]":""));
                  }

                  // J'affiche le premier plan, ou je le remplace par le folder
                  if( step<2 ) {
                     synchronized( pile ) {
                        int n = getIndex(firstPlan);
                        if( step==0 ) firstPlan = plan[n]= p;
                        else if( step==1 ) plan[n] =folder;
                        step++;
                     }
                     select.repaint();
                  }
               }
            }

            // On se cale sur le prochain segment de 2880
            long pos = in.getPos();
            if( pos%2880!=0 ) {
               long offset = ((pos/2880)+1) *2880  -pos;
               in.skip(offset);
            }

         }
      } catch( Exception e) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }

      try { in.close(); } catch(Exception e) { e.printStackTrace(); } 

      folder.planReady(true);

      // Aucun plan dans ce fits extension
      if( v.size()==0 ) return;

      // Plan actif par défaut
      p = (Plan)v.elementAt(0);
      if( v.size()==1 ) {
         if( label.charAt(0)=='=' ) label=label.substring(1);
         p.label = label; // On récupère le label du folder
      }
      p.planReady(true);

      // On met tout ça dans la pile
      if( v.size()>1 ) {
         Enumeration e = v.elements();
         while( e.hasMoreElements() ) {
            synchronized( pile ) {
               int n=getStackIndex();
               plan[n] = (Plan)e.nextElement();
               if( folder!=null ) permute(plan[n],folder);
            }
         }
      }
      
   }

   /**
    * Positionnement un flux au début d'une extension FITS particulière
    * @param in Le flux non encore entamé
    * @param numExt le numéro d'extension (commence à 0)
    * @throws Exception
    */
   public void seekFitsExt(MyInputStream in, int numExt) throws Exception {
      Plan p;

      for( int nExt=0 ; nExt<numExt; nExt++ ) {
         p=null;
         in.resetType();
         long type = in.getType();

         if( (type & MyInputStream.FITS)!=0 ) {
            p = new PlanImage(aladin,"",in,"",null,null,null,true,false,null);
         } else if( (type & (MyInputStream.FITST|MyInputStream.FITSB))!=0 ) {
            p = new PlanCatalog(aladin,"",in/*,label+"~"+i*/,true,false);
         } else {
            Aladin.trace(3,"Extension type not supported !");
            break;
         }

         // Pas terrible: pour détecter la fin de fichier on passe par
         // le p.error avec une chaine particulière
         if( p!=null && p.error!=null && p.error.equals("_END_XFITS_") ){
            throw new EOFException();
         }

         // On se cale sur le prochain segment de 2880
         long pos = in.getPos();
         if( pos%2880!=0 ) {
            long offset = ((pos/2880)+1) *2880  -pos;
            in.skip(offset);
         }
      }
   }

    /** Enregistre une image LOCALE dans le prochain plan libre.
     * @param file   Le nom du fichier .fits, .fits.h ou .fits.gz
     */
   protected int newPlanImage(String file,MyInputStream inImg,String label,String from,Obj o,ResourceNode imgNode) {
      int n=getStackIndex(label);
      label = prepareLabel(label);

      long type=0;
      try { type = inImg.getType(); } catch( Exception e ) {}

      if( (type&MyInputStream.CUBE)!=0 && (type&MyInputStream.ARGB)!=0)
         plan[n] = new PlanImageCubeRGB(aladin,file,inImg,label,from,o,imgNode,false,true,null);

      else if( (type&MyInputStream.CUBE)!=0 )
         plan[n] = new PlanImageCube(aladin,file,inImg,label,from,o,imgNode,false,true,null);

      else if( (type&MyInputStream.HUGE)!=0 )
         plan[n] = new PlanImageHuge(aladin,file,inImg,label,from,o,imgNode,false,true,null);

      else
         plan[n] = new PlanImage(aladin,file,inImg,label,from,o,imgNode,false,true,null);

      if( isNewPlan(label) ) n=bestPlace(n);

      suiteNew(plan[n]);
      return n;
   }

    /** Retourne true si le label indique que l'on veut un nouveau plan
     * (ne commence pas par =)
     * ou au contraire réutiliser un plan du même nom déjà existant */
    static protected boolean isNewPlan(String label) {
       return !(label!=null && label.length()>0 && label.charAt(0)=='=');
    }

    /** Crée un plan Image sur la pile avec le label indiqué.
     * Cette méthode est dédiée au plugin (voir Aladin.createAladinImage()
     * @param name nom du plan proposé
     * @return nom du plan effectif
     */
    protected String newPlanPlugImg(String name) {
       int n=getStackIndex();
       PlanImage p;
       plan[n] = p = new PlanImage(aladin);
       p.setLabel(name);
       p.creatDefaultCM();
       p.orig = PlanImage.COMPUTED;
       p.flagOk=true;
       return p.getLabel();
    }

    /** Crée un plan Catalogue sur la pile avec le label indiqué.
     * Cette méthode est dédiée au plugin (voir Aladin.createAladinCatalog()
     * @param name nom du plan proposé
     * @return nom du plan effectif
     */
    protected String newPlanPlugCat(String name) {
       int n=getStackIndex();
       Plan p;
       plan[n] = p = new PlanCatalog(aladin);
       p.setLabel(name);
       return p.getLabel();
    }

//    protected int newPlanImage(String file,MyInputStream inImg) {
//    	return newPlanImage(file, inImg, null, null, null);
//    }

    /** Enregistre une image RGB/FITS dans le prochain plan libre.
     * @param file   Le nom du fichier
     */
    protected int newPlanImageRGB(String file,URL u, MyInputStream inImg) {
       int n=getStackIndex();
       if( n<0 ) return -1;
       plan[n] = new PlanImageRGB(aladin,file,u,inImg);

       bestPlace(n);
       suiteNew(plan[n]);
       return n;
    }

    /** Enregistre une image RGB/FITS dans le prochain plan libre. */
    protected int newPlanImageRGB(URL u,MyInputStream inImg,int orig, String label, String objet,
          String param,String from,
          int fmt,int res,
          Obj o, ResourceNode imgNode) {
       int n=getStackIndex(label);
       label = prepareLabel(label);
       plan[n] = new PlanImageRGB(aladin,inImg,orig,u,label,objet,param,from,
             fmt,res,o, imgNode);
       n=bestPlace(n);
       suiteNew(plan[n]);
       return n;
    }

    /** Enregistre une image RGB/FITS dans le prochain plan libre.
     * @param file   Le nom du fichier
     * @param imgNode noeud décrivant l'image
     */
     protected int newPlanImageRGB(String file,URL u, MyInputStream inImg,ResourceNode imgNode) {
        int n=getStackIndex();
        plan[n] = new PlanImageRGB(aladin,file,u,inImg,imgNode);

        n=bestPlace(n);
        suiteNew(plan[n]);
        return n;
     }

     /** Enregistre un Allsky HEALPIX dans le prochain plan libre.
      * @param file   Le nom du fichier
      * @param imgNode noeud décrivant l'image
      * @param label le label du plan, null sinon
      * @param mode PlanBG.[DRAWPIXEL|DRAWPOLARISATION|DRAWANGLE]
      * @param indice champ à lire
      */
     protected int newPlanHealpix(String file,MyInputStream inImg, String label,
           int mode, int idxFieldToRead, boolean fromProperties) {
        return newPlanHealpix(file,inImg,label,mode,idxFieldToRead,fromProperties,null,null);
     }
     protected int newPlanHealpix(String file,MyInputStream inImg, String label,
              int mode, int idxFieldToRead, boolean fromProperties,String target,String radius) {
         Coord c=getTargetBG(target,null);
         double rad=getRadiusBG(target,radius,null);
         int n=getStackIndex();
         plan[n] = new PlanHealpix(aladin,file,inImg,label,mode,idxFieldToRead,fromProperties,c,rad);

         n=bestPlace(n);
         suiteNew(plan[n]);
         return n;
      }

   /** Enregistre une image COLOR dans le prochain plan libre.
    * @param file   Le nom du fichier
    */
    protected int newPlanImageColor(String file,URL u, MyInputStream inImg) {
       int n=getStackIndex();
       plan[n] = new PlanImageColor(aladin,file,u,inImg);

       n=bestPlace(n);
       suiteNew(plan[n]);
       return n;
    }

    /** Enregistre une image COLOR dans le prochain plan libre. */
    protected int newPlanImageColor(URL u,MyInputStream inImg,int orig, String label, String objet,
          String param,String from, int fmt,int res, Obj o, ResourceNode imgNode) {
       int n=getStackIndex(label);
       label = prepareLabel(label);
       plan[n] = new PlanImageColor(aladin,inImg,orig,u,label,objet,param,from,
             fmt,res,o, imgNode);
       n=bestPlace(n);
       suiteNew(plan[n]);
       return n;
    }

    /** Enregistre une image COLOR dans le prochain plan libre.
     * @param file   Le nom du fichier
     * @param imgNode noeud décrivant l'image
     */
     protected int newPlanImageColor(String file,URL u, MyInputStream inImg,ResourceNode imgNode) {
        int n=getStackIndex();
        plan[n] = new PlanImageColor(aladin,file,u,inImg,imgNode);

        n=bestPlace(n);
        suiteNew(plan[n]);
        return n;
     }
     
     // Retourne true si le plan passé en paramètre peut servir à ajouter des outils draws
     protected boolean planToolOk(Plan p, boolean flagWithFoV) {
        return (p.type==Plan.TOOL || flagWithFoV && p.type==Plan.APERTURE) && p.isReady() && p.isSelectable();
     }
     
     /** sélectionne et retourne le plan tool le plus adéquat, où le crée si nécessaire */
     protected PlanTool selectPlanTool() { return (PlanTool)selectPlanTool1(false); }
     
     /** sélectionne et retourne le plan tool ou FoV (APERTURE), où crée un plan tool si nécessaire */
     protected Plan selectPlanToolOrFoV() { return selectPlanTool1(true); }
     
     private Plan selectPlanTool1(boolean flagWithFoV) {
        try {
           Plan p = getFirstSelectedPlan();
           ViewSimple v = aladin.view.getCurrentView();
           int indexView = getIndex(v.pref);
           if( planToolOk(p,flagWithFoV) && getIndex(p)<indexView ) return p;
           Plan [] plan = getPlans();
           for( int i=0; i<plan.length && i<indexView; i++ ) {
              if( planToolOk(plan[i],flagWithFoV) ) {
                 selectPlan(plan[i]);
                 return plan[i];
              }
           }
        } catch( Exception e ) { }
        return createPlanTool(null);
     }
     
     protected PlanTool newPlanTool(String label) { return createPlanTool(label); }

     protected PlanTool createPlanTool(String label) {
        int n=getStackIndex();
        Plan p;
        plan[n] = p = new PlanTool(aladin,label);
        p.selected=true;
        p.active = true;
        Plan pref = getPlanRef();
        p.projd = pref==null || !Projection.isOk(pref.projd)? null : 
           new Projection("Myproj",Projection.WCS,pref.projd.alphai,pref.projd.deltai,
                 90*60,250,250,500,0,false,Calib.AIT,Calib.FK5);
        suiteNew(p);
        return (PlanTool)p;
     }

     protected PlanField createPlanField(String label,Coord center,double angle,boolean canbeRoll,boolean canbeMove) {
        int n=getStackIndex();
        Plan p;
        plan[n] = p = new PlanField(aladin,label,center,angle,canbeRoll,canbeMove);
        p.active = true;
        suiteNew(p);
        return (PlanField)p;
     }

     protected int newPlanFov(String label, Fov[] fov) {
        int n=getStackIndex();
        plan[n] = new PlanFov(aladin,label,fov);
        suiteNew(plan[n]);
        return n;
    }


   /** le prochain plan devient un plan contenant les contours
   * @param label  - nom du plan
   * @param levels  - tableau des niveaux
   * @param cAlgo  - algorithme de contour utilise
   * @param useSmoothing  - utilisation du lissage ?
   * @param useOnlyCurrentZoom  - calcul sur la vue courante uniquement ?
   * @param indiceCouleurs  - tableau des indices dans Couleur.DC des couleurs de chaque niveau
   */
   protected int newPlanContour(String label, PlanImage pimg, double[] levels,
                                ContourAlgorithm cAlgo, boolean useSmoothing,
                                int smoothingLevel, boolean useOnlyCurrentZoom,
                                boolean reduceNoise, Color[] couleurs) {
      int n=getStackIndex();
      if (levels == null) plan[n] = new PlanContour(aladin,label);
      else {
         Color coul = PlanContour.getNextColor(aladin.calque);
         plan[n] = new PlanContour(aladin,label, pimg, levels, cAlgo, useSmoothing,
                                   smoothingLevel, useOnlyCurrentZoom,
                                   reduceNoise, couleurs, coul);
      }
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }


  /** le prochain plan libre devient un plan de champ de vue.
   * @param target Nm de l'objet ou coordonnees
   * @param roll Angle de rotation par rapport au nord
   * @param instr le nom du plan et de l'instrument
   */
   protected int newPlanField(String target,double roll,String instr,String label) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = new PlanField(aladin,target,roll,instr,instr);

      // Truc tordu pour qu'il y ait une demande de resolution Simbad
      // Rq: pb du cache Simbad...
      if( ((PlanField)plan[n]).needTarget && plan[n].objet!=null ) aladin.view.setRepere(plan[n]);

      //      view.repaintAll();
      suiteNew(plan[n]);
      plan[n].planReady(!((PlanField)plan[n]).needTarget);
      return n;
   }


   // thomas
   protected int newPlanField(FootprintBean fpBean, String target, String label,double roll) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = new PlanField(aladin,target,fpBean,label,roll);

      // Truc tordu pour qu'il y ait une demande de resolution Simbad
      // Rq: pb du cache Simbad...
      if( ((PlanField)plan[n]).needTarget && plan[n].objet!=null ) aladin.view.setRepere(plan[n]);

      suiteNew(plan[n]);
      plan[n].planReady(!((PlanField)plan[n]).needTarget);
      return n;
   }

   protected int newPlanField(PlanField pf, String label) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = pf;
      pf.setLabel(label);

      suiteNew(plan[n]);
      plan[n].planReady(!((PlanField)plan[n]).needTarget);
      return n;
   }


  /** Positionnement du centre des plans FoV en attente de resolution
   * Simbad. Ces plans ont leur flagOk a false.
   */
   protected void setCenterForField(double al,double del) {
      int i;
      for( i=0; i<plan.length; i++ ) {
         if( plan[i].type!=Plan.APERTURE || !((PlanField)plan[i]).needTarget ) continue;
         ((PlanField)plan[i]).resolveTarget(al,del);
      }
   }
   
   /** Extraction d'un tranche d'un cube. Si n==-1, tranche courante */
   protected void newPlanImageFromBlink(PlanImageBlink cube, int frame) throws Exception {
      if( frame==-1 ) frame = aladin.view.getView(cube).cubeControl.lastFrame;
      cube.activePixelsOrigin(frame);
      PlanImage pi = (PlanImage)aladin.calque.dupPlan(cube, null,cube.type,false);
      pi.setLabel(cube.label+"#"+frame);
      pi.setHasSpecificCalib();
      pi.headerFits.setKeyValue("NAXIS", "2");
      pi.isOldPlan=false;
      pi.selected=false;
      pi.pourcent=-1;
      pi.flagOk=true;
      pi.type=Plan.IMAGE;
      pi.copyright = "Extract from "+cube.label+" (frame #"+(frame+1)+")";
//      pi.setBufPixels8( cube.getFrame(n) );
   }
   
   /** Création d'un plan image à partir des pixels visibles dans la vue passée en paramètre */
   protected void newPlanImageByCrop(final ViewSimple v,final RectangleD rcrop,final double resMult,final boolean fullRes) {
      final double zoom = v.zoom;
      (new Thread("crop"){
        @Override
        public void run() {
           v.cropArea(rcrop,null,zoom,resMult,fullRes,true);
           repaintAll();
        }
      }).start();
   }

   /** Création d'un nouveau plan catalogue en concatenant toutes les tables des plans
    * passés en paramètres, ou à défaut ceux sélectionnés dans la pile.
    * @param pList : liste des plans concernés ou null pour ceux sélectionnés dans la pile
    * @param uniqTable : true si concaténation dans une unique table homogène
    */
   protected void newPlanCatalogByCatalogs(Plan []pList,boolean uniqTable) {
      Plan [] p = pList!=null ? pList : getPlans();
      Vector<Source> v = new Vector<Source>(100000);
      for( int i=0; i<p.length; i++ ) {
         if( !p[i].isCatalog() || !p[i].flagOk ) continue;
         if( pList==null && !p[i].selected ) continue;
         Iterator<Obj> it = p[i].iterator();
         while( it.hasNext() ) {
            Obj o = it.next();
            if( !(o instanceof Source) ) continue;
            Source s = (Source)o;
            v.addElement(s);
         }
      }
      newPlanCatalogBySources(v, "Concat",uniqTable);
      repaintAll();
   }

   /** Création d'un nouveau plan catalogue avec les sources
    *  sélectionnées. Je sélectionne tous les nouveaux objets
   */
   protected void newPlanCatalogBySelectedObjet(boolean uniqTable) {newPlanCatalogBySelectedObjet("Select.src",uniqTable); }
   protected void newPlanCatalogBySelectedObjet(String name,boolean uniqTable) {
      Vector v = aladin.view.getSelectedObjet();
      PlanCatalog p = newPlanCatalogBySources(v, name,uniqTable);
      if( p!=null ) aladin.view.selectAllInPlan(p);
   }

   protected PlanCatalog newPlanCatalogBySources(Vector vSources,String name,boolean uniqTable) {
      Source s, newSource;
      if( vSources==null ) return null;
      int indice = newPlanCatalog();
      PlanCatalog p = (PlanCatalog)plan[indice];
      p.setLabel(name==null || name.length()==0?"New.cat":name);

      // Cas simple et rapide
      if( !uniqTable ) {
         Vector legs = new Vector(10);      // Juste pour compter le nombre de tables différentes
         Enumeration e = vSources.elements();
         while( e.hasMoreElements() ) {
            Obj o = (Obj)e.nextElement();
            if( !(o instanceof Source) ) continue;
            s = (Source)o;
            p.pcat.setObjetFast(newSource = new Source(p, s.raj, s.dej, s.id, s.info));
            newSource.isSelected = s.isSelected;
            newSource.values = s.values;
            newSource.actions = s.actions;
            newSource.leg = s.leg;
            if( !legs.contains(s.leg) ) legs.addElement(s.leg);
         }
         p.pcat.nbTable= legs.size();

         // Fusion en une table unique homogène
      } else {

         // Génération d'une légende générique
         ArrayList leg = new ArrayList(10);
         Enumeration e = vSources.elements();
         while( e.hasMoreElements() ) {
            Obj o = (Obj)e.nextElement();
            if( !(o instanceof Source) ) continue;
            s = (Source)o;
            if( !leg.contains(s.leg) ) leg.add(s.leg);
         }
         Legende legGen = new Legende(leg);

         // Insertion de toutes les sources
         e = vSources.elements();
         while( e.hasMoreElements() ) {
            Obj o = (Obj)e.nextElement();
            if( !(o instanceof Source) ) continue;
            s = (Source)o;

            // Création de la nouvelle ligne de mesures en fonction de la légende générique
            String info = createInfo(s,legGen);

            p.pcat.setObjetFast(newSource = new Source(p, s.raj, s.dej, s.id, info));
            p.pcat.nbTable=1;
            newSource.leg=legGen;
         }
      }

      if(aladin.calque.getPlanRef()!=null) p.objet = aladin.calque.getPlanRef().objet;
      p.setActivated(true);
      p.pcat.createDefaultProj();
//      p.setSourceType(Source.getDefaultType(vSources.size()));

      return p;
   }

   // Creation d'une chaine de mesures issues d'une source en fonction d'une légende générique particulière
   // (remettre les valeurs dans les bonnes colonnes)
   private String createInfo(Source s,Legende leg) {
      String [] v = new String[leg.getSize()];
      int offset=s.info.indexOf('\t');          // On passe le premier champ <&_nom du cata...>
      int start=offset+1;
      for( int i=0; offset!=-1; i++ ) {
         offset = s.info.indexOf('\t',start);
         int j = leg.find(s.leg.field[i]);
         if( j==-1 ) continue;
         v[j] = offset!=-1 ? s.info.substring(start,offset) : s.info.substring(start);
         start=offset+1;
      }

      StringBuffer info = new StringBuffer("<&_X|X>");   // le premier élément ne sert à rien, juste pour compatibilité
      for( int i=0; i<v.length; i++ ) {
         info.append( "\t" +(v[i]==null ? " " : v[i]) );
      }

      return info.toString();
   }

  /** Enregistre un catalogue dans le prochain plan libre.
   * @param u     l'URL qu'il va falloir appeler
   * @param label le nom du plan (dans la pile des plans)
   * @param objet le target central (objet ou coord)
   * @param param les parametres du plan (radius...)
   * @param from  l'origine des donnees
   * @param Server le serveur d'origine
   */
   protected int newPlanCatalog(URL u,String label, String objet,String param,String from, Server server) {
      return newPlanCatalog(u,null,label,objet,param,from,server);
   }
   protected int newPlanCatalog(URL u,MyInputStream in,String label, String objet,String param,String from,
         Server server) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = new PlanCatalog(aladin,u,in,label,objet,param,from,server);
      suiteNew(plan[n]);
      return n;
   }

  /** Enregistre un catalog LOCAL dans le prochain plan libre.
   * @param file   Le nom du fichier FITS
   */
   protected int newPlanCatalog(String file) { return newPlanCatalog(file,null); }
   protected int newPlanCatalog(String file,MyInputStream in) {
      int n=getStackIndex();
      plan[n] = new PlanCatalog(aladin,file,in,false,true);
      suiteNew(plan[n]);
      //PlanFilter.newPlan(plan[n]);
      return n;
   }

   protected Plan createPlanCatalog(MyInputStream in,String label) {
      int n =  newPlanCatalog(in,label);
      return plan[n];
   }
   
   /** Ajoute un plan déjà préparé dans la pile */
   protected int newPlan(Plan p) {
      int n=getStackIndex();
      plan[n] = p;
      suiteNew(p);
      return n;
   }

  /** Pour VOApp et ExtApp
   * @param in  L'input Stream
   */
   protected int newPlanCatalog(MyInputStream in,String label) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = new PlanCatalog(aladin,in,label);
      suiteNew(plan[n]);
      return n;
   }

   protected int newPlanCatalog(MyInputStream in,String label,String origin) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      plan[n] = new PlanCatalog(aladin,in,label,origin);
      suiteNew(plan[n]);
      return n;
   }
   
   /** Subtilité. Si le nom du plan désigne commence par =, Aladin
    * doit réutiliser une case de la pile.
    * Si le plan désigné est un numéro (=@nn), je retourne le label du plan
    * pré-existant au null si cela correspong à une case vide dans la pile
    * S'il s'avère que le plan désigné n'existe pas =XXXX, je supprime
    * simplement le "=" pour qu'Aladin en crée un nouveau
    */
   protected String prepareLabel(String label) {
      if( !isNewPlan(label) ) {
         if( label.charAt(1)=='@' ) {
            int n;
            try {
               n = Integer.parseInt(label.substring(2));
               n = plan.length-n;
            } catch( Exception e ) { return null; }
            if( plan[n].type==Plan.NO || plan[n].type==Plan.X ) return null;
            return "="+plan[n].label;
         }
         String s = label.substring(1);
         if( getIndexPlan(s,1)<0 ) return s;
      }
      return label;
   }

   // Procédure interne de découpage d'un plan Catalogue en plusieurs Plan, ou
   // pour chaque table
   private Vector<Plan> splitCatalog1(PlanCatalog p) {
      Vector<Plan> v = new Vector<Plan>(10);
      PlanCatalog p1=null;
      Legende leg=null;
      int folder = p.folder;
      PlanFolder fold=new PlanFolder(aladin,p.label,folder,false);
      fold.label=p.label;
      fold.active=true;
      fold.projd=p.projd;
      fold.u=p.u;
      fold.pcat = new Pcat(fold,null,this,null,aladin);
      fold.pcat.description = p.pcat.description;
      fold.pcat.parsingInfo = p.pcat.parsingInfo;
      v.addElement(fold);
      Iterator<Obj> it = (p).iterator();
      while( it.hasNext() ) {
         Obj o1 = it.next();
         if( !(o1 instanceof Source) ) continue;
         Source o = (Source)o1;
         if( o.leg!=leg ) {
            p1 = new PlanCatalog(aladin);
            p1.server = p.server;
            p1.c = p.c;
            p1.folder=folder+1;
            p1.copyright=p.copyright;
            p1.co=p.co;
            p1.param=p.param;
            p1.objet=p.objet;
            p1.pcat.nbTable=1;
            p1.sourceType=p.sourceType;
            p1.fullSource=p.fullSource;
            p1.planFilter=p.planFilter;

            p1.projd = p.projd.copy();
            p1.setLabel(p1.getTableName(o));
            v.addElement(p1);
            leg=o.leg;
         }
         o.plan=p1;
         p1.pcat.setObjetFast(o);
      }
      return v;
   }
   
   /** Découpage d'un catalogue : une table par plan */
   protected void splitCatalog(PlanCatalog p) {
      if( p.getNbTable()==1 ) return;
      Vector v = splitCatalog1(p);
      int m=v.size()-1;
      getStackIndex(null,m);
      synchronized( pile ) {
         int n = getIndex(p);
         for( int i=0; i<n-m; i++) plan[i]=plan[i+m];
         for( int i=0; i<=m; i++ ) {
            Plan p1 = (Plan)v.elementAt(i);
            plan[n-m+i]= p1;
            p1.setActivated(true);
            if( i>1 ) p1.c = Couleur.getNextDefault(this);
         }
      }
   }

   // thomas (AVO)
   /** Enregistre un catalog via InputStream dans le prochain plan libre.
	* @param dis  L'input Stream
	* @param aladinLabel nom du plan
	* @param origin origine du catalogue
	*
	protected int newPlanCatalog(MyInputStream in, String label, String origin) {
	   int n=getFirstFree();
	   if( n<0 ) return -1;
	   plan[n] = new PlanCatalog(aladin,in,label,origin);
	   suiteNew();
	   return n;
	}


// thomas
  /** Cree un nouveau PlanCatalog vide
   */
   protected int newPlanCatalog() {
      int n=getStackIndex();
      plan[n] = new PlanCatalog(aladin);
      
//	  // la projection est celle du plan de reference
//	  Plan pRef = getPlanRef();
//	  plan[n].projd = (pRef!=null)?pRef.projd:null;
	  
	  // La projection est celle de la vue de base
	  Projection proj = aladin.view.getCurrentView().getProj();
	  plan[n].projd = proj==null ? null : proj.copy();

      suiteNew(plan[n]);
      return n;
   }
// fin thomas


   /** Retourne true si le plan de base est un plan Background */
   boolean isBackGround() {
      Plan p = getPlanBase();
      return p!=null && p.type==Plan.ALLSKYIMG;
   }

//   private int planBGLaunching = 0;
//   synchronized private void launchPlanBG() { planBGLaunching++; }
//   synchronized protected void planBGLaunched() { planBGLaunching--; }
//   
//   protected boolean isPlanBGSync() {
//      if( planBGLaunching>0  ) {
//         aladin.trace(3,"Waiting planBG (in creation phase)");
//         return false;
//      }
////      aladin.trace(3,"All planBG has been launched");
//      return true;
//   }
   
   // Détermination du target de démarrage pour un plan BG
   private Coord getTargetBG(String target,TreeNodeAllsky gSky) {
      Coord c=null;
      if( target!=null && target.length()>0) {
         try {
            if( !View.notCoord(target) ) c = new Coord(target);
            else c = aladin.view.sesame(target);
         } catch( Exception e ) { e.printStackTrace(); }
         
      } else {
         if( gSky!=null && gSky.getTarget()!=null ) c=gSky.getTarget();
         else {
            if( !aladin.view.isFree() && aladin.view.repere!=null && !Double.isNaN(aladin.view.repere.raj) ) c = new Coord(aladin.view.repere.raj,aladin.view.repere.dej);
         }
      }
      return c;
   }
   
   // Détermination du radius de démarrage pour un plan BG
   private double getRadiusBG(String target,String radius,TreeNodeAllsky gSky) {
      double rad=-1;
      if( radius!=null && radius.length()>0 ) {
         try {
            rad = Server.getAngle(radius, Server.RADIUS )/60.;
         } catch( Exception e ) { e.printStackTrace(); }
      }
      else if( gSky!=null && gSky.getRadius()!=-1 ) rad=gSky.getRadius();
      else if( (target==null || target.length()==0) && rad==-1 ) {
         try {
            rad = aladin.view.getCurrentView().getTaille();
         } catch( Exception e ) { }
      }
      return rad;
   }
   
   /** Création d'un plan Healpix Multi-Order Coverage Map à partir d'un flux */
   protected int newPlanMOC(MyInputStream in,String label) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      Coord c=getTargetBG(null,null);
      double rad=getRadiusBG(null,null,null);
      plan[n] = new PlanMoc(aladin,in,label,c,rad);
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }
   
   /** Création d'un plan Healpix Multi-Order Coverage Map à partir d'un MOC */
   protected int newPlanMOC(HealpixMoc moc,String label) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      Coord c=getTargetBG(null,null);
      double rad=getRadiusBG(null,null,null);
      plan[n] = new PlanMoc(aladin,moc,label,c,rad);
      n=bestPlace(n);
      suiteNew(plan[n]);
      return n;
   }
   
   /** Création d'un plan BG */
   public int newPlanBG(String path, String label, String target,String radius) { return newPlanBG(null,path,null,label,target,radius); }
   public int newPlanBG(TreeNodeAllsky gSky, String label, String target,String radius) { return newPlanBG(gSky,null,null,label,target,radius); }
   public int newPlanBG(URL url, String label, String target,String radius) { return newPlanBG(null,null,url,label,target,radius); }
   
   
   public int newPlanBG(TreeNodeAllsky gSky,String path,URL url, String label, String target,String radius) {
      int n=getStackIndex(label);
      label = prepareLabel(label);
      Coord c=getTargetBG(target,gSky);
      double rad=getRadiusBG(target,radius,gSky);
      
      Plan p;
      String startingTaskId = aladin.synchroPlan.start("Calque.newPlanBG/creating"+(label==null?"":"/"+label));
      if( gSky!=null ) {
         plan[n] = p = gSky.isProgen()  ? new PlanBGProgen(aladin,gSky,label, c, rad,startingTaskId) :
                       gSky.isCatalog() ? new PlanBGCat(aladin,gSky,label, c, rad,startingTaskId) :
                       gSky.isMap()     ? new PlanHealpix(aladin,gSky,label, c,rad,startingTaskId) :
                       gSky.isCube()    ? new PlanBGCube(aladin, gSky, label, c,rad,startingTaskId):
                                          new PlanBG(aladin, gSky, label, c,rad,startingTaskId);
      } else {
         plan[n] = p = path!=null ? new PlanBG(aladin, path, label, c, rad,startingTaskId) 
                                  : new PlanBG(aladin, url, label, c, rad, startingTaskId);
      }
      n=bestPlace(n);
      suiteNew(p);
      return n;
   }
   
   
//   /** Création d'un plan BG à partir d'un répertoire local */
//   public int newPlanBG(String path,String label) {
//	   return newPlanBG(path, label,null,-1);
//   }
//   /**
//    *
//    * @param path
//    * @param coo coordonnées de la position centrale de la vue en J2000, ou null si non spécifiée
//    * @param radius taille du champ en degrés ou <=0 si non spécifié
//    * @return
//    */
//   public int newPlanBG(String path, String label, Coord coo, double radius) {
//      launchPlanBG();
//      int n=getStackIndex(label);
//      label = prepareLabel(label);
//      PlanBG p;
//      plan[n] = p = new PlanBG(aladin, path, label, coo, radius);
//      suiteNew(p);
//      p.modifyProj( Calib.getProjName(Calib.AIT) );
//      //      p.initZoom = 1./32;
//      //      aladin.calque.repaintAll();
//      return n;
//   }

   /** Creation d'un plan à partir d'un nom de fichier ou d'une url
    * issu d'une source */
   protected int newPlan(String filename,String label,String origin,Obj o) {
      return ((ServerFile)aladin.dialog.localServer).creatLocalPlane(filename,label,origin,o,null,null,null,null,null);
   }

   protected Plan createPlan(String filename,String label,String origin,Server server) {
      int n = ((ServerFile)aladin.dialog.localServer).creatLocalPlane(filename,label,origin,null,null,null,server,null,null);
      return plan[n];
   }

   /** Creation d'un plan à partir d'un nom de fichier ou d'une url */
   protected int newPlan(String filename,String label,String origin) {
      return newPlan(filename,label,origin,null,null);
   }
   protected int newPlan(String filename,String label,String origin,String target,String radius) {
      return ((ServerFile)aladin.dialog.localServer).creatLocalPlane(filename,label,origin,null,null,null,null,target,radius);
   }

   /** Creation d'un plan à partir d'un stream ouvert */
   protected int newPlan(InputStream inputStream, String label,String origin) {
      return ((ServerFile)aladin.dialog.localServer).creatLocalPlane(null,label,origin,null,null,inputStream,null,null,null);
   }

   /** Creation d'un plan à partir d'un stream ouvert */
   protected Plan createPlan(InputStream inputStream, String label,String origin) throws Exception {
      int n = ((ServerFile)aladin.dialog.localServer).creatLocalPlane(null,label,origin,null,null,inputStream,null,null,null);
      if( n==-2 ) return null;   // De fait aucune création de plan (cas du FOV)
      if( n<0 ) throw new Exception("plane creation error");
      return plan[n];
   }

  /** Actions a faire apres la demande de creation d'un nouveau plan */
   protected void suiteNew(Plan p) {
      if( p==null ) return;
//      bestPlacePost(p);

      // Affectation du plan aux vues qui utilisaient son prédécesseur
      // dans le cas d'une réutilisation de plan
      aladin.view.adjustViews(p);
      
      select.repaint();

//      if( select!=null ) select.clinDoeil();
      aladin.toolBox.toolMode();
   }

  /** Test d'un eventuel plan selectionne (label enfonce)
   * @return <I>true</I> si au moins un plan, sinon <I>false</I>
   */
   protected boolean noSelected() {
      Plan [] plan = getPlans();
      for( int i=0; i<plan.length; i++ ) {
         if( plan[i].selected ) return false;
      }
      return true;
   }

   private void permute(int s,int t,int n) {
      int sens=(s<t)?1:-1;
      if( sens==-1) t++;
//System.out.println("de "+s+"/"+plan[s].label+" vers "+t+"/"+plan[t].label+" pour "+n+"x (sens="+sens+")");

      for( int i=0; i<n; i++ ) {
         int k=(sens==-1)?s+i:s;
         int m=(sens==-1)?t+i:t;
         //             System.out.println("***etape "+i+" de "+k+" vers "+m);
         Plan p = plan[k];
         while( k!=m ) {
            plan[k] = plan[k+sens];
            k+=sens;
         }
         plan[m] = p;
      }
   }

   /** Permutation des plans avec ajustement des niveaux de folder
    * et expansion du folder d'arrivée s'il est collapsé
    * @param source Le plan a deplacer
    * @param target Le nouvel emplacement
    */
    public void permute(Plan source, Plan target) {
       int i,k=0,m=0;
       int n=1;      // nombre de plans a permuter
       int targetFolder=0;

 //System.out.println("Permutation "+source.label+" vers "+target.label);
       boolean isCollapsed = isCollapsed(target) || target.collapse;

       // Determination du niveau du folder (plan target)
       targetFolder=target.folder;
       if( target.type==Plan.FOLDER ) targetFolder++;

       synchronized( pile ) {

          // Y a-t-il plusieurs plans consecutifs a permuter (folder)
          if( source.type==Plan.FOLDER ) {
             Plan p[] = getFolderPlan(source);
             int deltaFolder = targetFolder-source.folder;
             //System.out.println("deltaFolder="+deltaFolder);

             source.folder+=deltaFolder;
             for( i=0; i<p.length; i++ ) p[i].folder+=deltaFolder;
             n=p.length+1;

          } else source.folder=targetFolder;

          for( i=0; i<plan.length; i++ ) {
             if( source==plan[i] ) k=i;
             else if( target==plan[i] ) m=i;
          }
          permute(k,m,n);

          // Si le target était un folder collapsé, on le décollapse
          if( isCollapsed ) {
             Plan p[] = getFolderPlan( getFolder(target) );
             for( i=0; i<p.length; i++ ) p[i].collapse=false;
          }
       }

       // thomas
       // si la source ou la cible est un PlanCatalog, l'update pour un filtre (voir ci-dessous) est inutile
       if( source.isCatalog() ) {
          PlanFilter.updatePlan(source,k,m);
          return;
       }
       if( target.isCatalog() ) {
          PlanFilter.updatePlan(target,m,k);
          return;
       }

       if( source.type==Plan.FILTER ) {
          ((PlanFilter)source).positionChange();
       }
       if( target.type==Plan.FILTER ) {
          ((PlanFilter)target).positionChange();
       }
    }

    /** Retourne le premier plan catalogue de la pile
     */
      protected Plan getFirstCatalog() {
         for( int i=0; i<plan.length; i++ ) {
            if( plan[i].isReady() && plan[i].isSimpleCatalog() ) return plan[i];
         }
         return null;
      }

  /** Retourne le numero du premier plan selectionne
   * @return le numero du plan, <I>-1</I> si aucun
   */
    protected int getFirstSelected() {
       for( int i=0; i<plan.length; i++ ) {
          if( plan[i].selected ) return i;
       }
       return -1;
    }

   /** Retourne le premier plan sélectionné, ou null si aucun */
    protected Plan getFirstSelectedPlan() {
       int n = getFirstSelected();
       if( n<0 ) return null;
       return plan[n];
    }

   /** Retourne le premier plan Catalog sélectionné, ou null si aucun */
    protected PlanCatalog getFirstSelectedPlanCatalog() {
       for( int i=0; i<plan.length; i++ ) {
          if( plan[i].selected && plan[i].isSimpleCatalog() ) return (PlanCatalog)plan[i];
       }
       return null;
   }

   /** Retourne le premier plan Image sélectionné, ou null si aucun */
    protected PlanImage getFirstSelectedPlanImage() {
       for( int i=0; i<plan.length; i++ ) {
          if( plan[i].selected
                && (plan[i].isImage() || plan[i].type==Plan.ALLSKYIMG) ) return (PlanImage)plan[i];
       }
       return null;
   }

    /** Retourne le premier plan Image simple sélectionné, ou null si aucun */
    protected PlanImage getFirstSelectedSimpleImage() {
       for( int i=0; i<plan.length; i++ ) {
          if( plan[i].selected && plan[i].isImage() ) return (PlanImage)plan[i];
       }
       return null;
   }

    /** Retourne le premier plan Image sélectionné, ou null si aucun */
    protected PlanImage getFirstSelectedImage() {
       for( int i=0; i<plan.length; i++ ) {
          if( plan[i].selected && plan[i].isPixel() ) return (PlanImage)plan[i];
       }
       return null;
   }


   /** Retourne le numero du premier plan selectionne
    * @return le numero du plan, <I>-1</I> si aucun
    */
    protected int getLastSelected() {
       for( int i=plan.length-1; i>=0; i-- ) {
          if( plan[i].selected ) return i;
       }
       return -1;
    }

    /** Retourne la liste des plans sélectionnés */
    protected Vector getSelectedPlanes() {
       Vector v = new Vector(10);

       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          if( !plan[i].selected ) continue;
          v.addElement(plan[i]);
       }
       return v;
    }

    /** Retourne la liste des plans sélectionnés */
    protected Vector getSelectedSimpleImage() {
       Vector v = new Vector(10);

       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          if( !plan[i].flagOk || !plan[i].selected || !plan[i].isSimpleImage() ) continue;
          v.addElement(plan[i]);
       }
       return v;
    }

    /** Sélectionne tous les plans images simples et en retourne la liste */
    protected Vector setSelectedSimpleImage() {
       Vector v = new Vector(10);

       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          if( !plan[i].flagOk || !plan[i].isSimpleImage() ) continue;
          plan[i].selected=true;
          v.addElement(plan[i]);
       }
       return v;
    }

    /** Change pour toute la pile le niveau d'opacité des images */
    protected void setOpacityLevelImage(float opacity) {
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          Plan p = plan[i];
          if( !p.flagOk || !p.isSimpleImage() ) continue;
          p.setOpacityLevel(opacity);
       }
    }
    
    
    /** Change pour tous les plans sélectionnés le niveau d'opacité */
    protected void setOpacityLevel(float opacity) {
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          Plan p = plan[i];
          if( !p.flagOk || !p.selected || !p.hasCanBeTranspState() ) continue;
          p.setOpacityLevel(opacity);
          if( opacity>=0.1 ) p.setActivated(true);
       }
    }

    /** Change pour tous les plans sélectionnés le facteur de taille des sources */
    protected void setScalingFactor(float scalingFactor) {
       Plan [] plan = getPlans();
       for( int i=0; i<plan.length; i++ ) {
          Plan p = plan[i];
          if( !p.flagOk || !p.selected || !(p.isCatalog() || p.type==Plan.TOOL) ) continue;
          p.setScalingFactor(scalingFactor);
       }
    }

    /** Détermine si le plan doit être activé comme un plan de référence ou simplement afficher en overlay */
    protected boolean mustBeSetPlanRef(Plan p) {
       boolean setRef=false;
       
       // prochaine vue à utiliser
       ViewSimple v = aladin.view.viewSimple[ aladin.view.getLastNumView(p) ];
       
       // Juste pour du débuging
       String sDebug=null;

       // La pile est vide => ref
       if( aladin.calque.isFreeX(p) ) { setRef=true; sDebug="Stack Vide"; }

       // Il s'agit d'un simple remplacement de plan => activate 
       else if( p.isOldPlan ) { setRef=false; sDebug="Flag IsOldPlan=true"; }

       // Dans une case vide sans être un simple overlay => ref
       else if( v.isFree() && !p.isOverlay() ) { setRef=true; sDebug="Image dans la prochaine view vide"; }

       // Le plan de ref est une image normal et on charge une autre image => ref
       else if( v.pref!=null && v.pref.isImage() && p.isImage() ) { setRef=true; sDebug="Image sur image"; }

       // Le plan de ref est catalogue normal  et on charge image ou un plan allsky => ref
       else if( v.pref!=null && (v.pref.isSimpleCatalog() && (p.isImage() || p instanceof PlanBG)) ) { setRef=true; sDebug="Image ou Allsky sur catalogue"; }

       // Le plan de ref n'est pas allsky et le catalogue n'est pas visible
       else if( v.pref!=null && !(v.pref instanceof PlanBG) && p.isSimpleCatalog() && !p.isViewable() ) { setRef=true; sDebug="Catalogue non visible autrement"; }
       
       // Dans le cas d'un multiview on priviligiera la création du plan
       else if( aladin.view.isMultiView() && p.isImage() ) { setRef=true; sDebug="Image sur multivue"; }
       
       aladin.trace(4,"Calque.mustBeSetPlanRef("+p.label+") => "+setRef+(sDebug!=null?" ("+sDebug+")":""));
       return setRef;
    }


    /** Retourne true si le plan passé en paramètre peut être transparent
     *  Vérifie que la compatibilité des projections
     */
    protected boolean canBeTransparent(Plan p) {
       boolean isRefForVisibleView = p!=null && p.isRefForVisibleView();
       if( p==null || p.type==Plan.FILTER || !isFree() && isRefForVisibleView && !p.isOverlay() ) {
          if (p!=null ) p.setDebugFlag(Plan.CANBETRANSP,false);
          return false;
       }
       if( p.isOverlay() ) {
          p.setDebugFlag(Plan.CANBETRANSP,true);
          return true;
       }
       if( (p.isImage() || p.type==Plan.ALLSKYIMG) && !aladin.configuration.isTransparent() ) return false;
       if( p.type==Plan.ALLSKYIMG && !isRefForVisibleView && p.flagOk && !p.isUnderImg() ) { p.setDebugFlag(Plan.CANBETRANSP,true); return true; }

       // S'il s'agit d'un folder, il faut qu'il contienne au moins un plan qui peut être transparent
       boolean folderTrans=true;
       if( p instanceof PlanFolder ) {
          folderTrans=false;
          Plan [] list = getFolderPlan(p);
          for( Plan p1 : list ) {
             if( canBeTransparent(p1) ) { folderTrans=true; break; }
          }
       }
       if( folderTrans ) {
          p.setDebugFlag(Plan.CANBETRANSP,true);
          return true;
       }
       
       if( !p.flagOk  || !folderTrans /* !planeTypeCanBeTrans(p) */
             || !Projection.isOk(p.projd) || p.isRefForVisibleView()
             || p.isImage() && p.projd.isLargeField() ) {
           p.setDebugFlag(Plan.CANBETRANSP,false);
           return false;
       }

       ViewSimple vc = aladin.view.getCurrentView();
       Plan plan[] = getPlans();
       boolean audessus=true;
       for( int i=0; i<plan.length; i++ ) {
          if( audessus ) { if( plan[i]==p ) audessus=false; continue; }
          if( /*!plan[i].ref*/ !plan[i].isRefForVisibleView() || !Projection.isOk(plan[i].projd)) continue;
          if( plan[i].projd.agree(p.projd,vc) ) { p.setDebugFlag(Plan.CANBETRANSP,true); return true; }
       }
       p.setDebugFlag(Plan.CANBETRANSP,false);
       return false;
    }

    /** Vérifie si un plan peut etre transparent (vérification uniquement au niveau du type du plan)
     */
//    protected boolean planeTypeCanBeTrans(Plan p) {
//       if( p instanceof PlanFolder ) return false;
//       return true;
//    }

    protected void addOnStack(Plan p) {
       int n = aladin.calque.getStackIndex();
       aladin.calque.plan[n]=p;
    }

  /** Retourne l'emplacement à utiliser dans la pile. Si on passe un label préfixé
   * par le caractère "=", on cherchera l'emplacement d'un éventuel plan pré-existant
   * ayant le même label, sinon on utilise un plan libre.
   * Si on passe un nombre, s'assure qu'il y a au-moins autant de plan libre que demandé
   */
    private int getStackIndex() { return getStackIndex(null,1); }
    private int getStackIndex(String label) { return getStackIndex(label,1); }
    private int getStackIndex(String label,int nombre) {
       int i;
       // Remplacement d'un plan déjà utilisé ?
       // Le label doit commencer par '='( ex: =toto). Il peut également s'agir d'un numéro
       // de plan dans la pile suivant la syntaxe "=@nnn"
       if( !isNewPlan(label) ) {
          int n;
          if( label.charAt(1)=='@' ) {
             try { n = Integer.parseInt(label.substring(2)); } catch( Exception e ) { n=-1; }
             n = plan.length-n;
          } else n=getIndexPlan(label.substring(1), 1);
          if( n>=0 ) return n;
       }

       for( i=0; i<plan.length && plan[i].type==Plan.NO; i++ );
       if( i>nombre ) i--;
       else {
          reallocPlan();
          return getStackIndex(null,nombre);
       }

       return i;
    }

  /** Re-affichage de l'ensemble des composantes du calque. */
   public void repaintAll() {
   	  if( select!=null  ) {
         select.repaint();
         zoom.zoomSliderReset();
         zoom.zoomView.repaint();
         aladin.view.repaintAll();
         aladin.toolBox.toolMode();
   	  }
   }

   /**
	 * Update footprint opacity level for already loaded footprint planes
	 * The level is updated if and only if the current level is equal to oldLevel
	 *
	 * @param oldLevel
	 * @param newLevel
	 */
	public void updateFootprintOpacity(float oldLevel, float newLevel) {
		for (int i = 0; i < plan.length; i++) {
			if (Aladin.isFootprintPlane(plan[i])) {

				if (plan[i].getOpacityLevel() == oldLevel) {
					plan[i].setOpacityLevel(newLevel);
				}
			}
		}
	}

}
