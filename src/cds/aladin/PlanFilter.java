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
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import cds.tools.Util;

/**
 * Plan dedie a un filter (FILTER)
 *
 * @author Pierre Fernique [CDS] - Thomas Boch [CDS]
 * @version 1.0 : 29 octobre 2002 creation
 */
public final class PlanFilter extends Plan {
   // incrément pour les tableaux liés aux filtres
   private static final int INCREMENT = 4/*20*/;

   // taille actuelle des tableaux liés aux filtres
   protected static int LIMIT = INCREMENT;

   // Definition des filtres predefinies
   // Le premier mot de la première ligne de commentaire est le nom du filtre par défaut
   static final String[] PREDEFFILTERS = {
   // magnitude circles
   "# Mag.Circle\n# This filter draws for each source a circle \n# whose radius is proportional to the magnitude\n" +
   "{draw circle(-$[phot.mag*])}",
   // ellipses
   "# Ellipses\n# This example shows how to draw \n# an ellipse associated with a source\n\n" +
   "# The first action draws the source itself,\n"+
   "# the second action draws the ellipse ; \n# parameters are semi-major axis, semi-minor axis, and position angle\n" +
   "{\ndraw\n" +
   "draw ellipse(0.5*$[phys.angSize.smajAxis],0.5*$[phys.angSize.sminAxis],$[pos.posAng])\n}",
   // magnitude cut
   "# Mag.Cut\n# Only sources with a magnitude \n# brighter than 16 are displayed\n" +
   "$[phot.mag*]<16 {draw}\n",
   // display text
   "# Disp.Text\n# This filter draws for each source the content\n" +
	   "# of the column tagged by the UCD \"src.class\"\n" +
   "{\ndraw $[src.class]\n}",
   // parameterized colors
   "# Param.Colors\n# In this filter, the green and blue components\n" +
   "# of the color of each source are defined\n" +
   "# according to the value of the magnitude\n" +
   "{\ndraw rgb(255,-$[phot.mag*],$[phot.mag*]) square\n}",
   // object type
   "# Obj.Type\n# We draw a different symbol according to\n" +
   "# the object type (value of the column with UCD \"src.class\")\n" +
   "$[src.class]=\"Star\" {draw red square}\n"+
   "$[src.class]=\"Radio\" {draw blue rhomb}\n"+
   "$[src.class]=\"Galaxy\" || $[src.class]=\"Seyfert\" {draw green plus}\n"+
   "# etc ...\n",
   // mouvements propres
   "# Prop.motions\n# Draws an arrow representing\n" +
   "# the proper motion of the source\n\n" +
   "# Remark : You are suggested to modify the factor \n# if the arrows are too small or too long\n" +
   "{draw pm(5*$[pos.pm;pos.eq.ra],5*$[pos.pm;pos.eq.dec])}\n",

   // fonction rainbow (color index)
   "# Color.Index\n# This filters aims to visualize the color of stars\n" +
	   "# according to their color index B-V.\n" +
	   "# The optional parameters -0.3 and 1 mean that :\n" +
   "# - any source with a color index lesser than -0.3\n#   is displayed in blue\n" +
   "# - any source with a color index greater than 1\n#    is displayed in red\n" +
   "{draw rainbow($[phot.color;em.opt.B;em.opt.V],-0.3,1)}",
   // filtres sur les types spectraux
   "# Spec.Types\n# This filter assigns colors related\n# to the spectral type of sources.\n" +
   "# The association spectral type --> color\n# is the following:\n" +
   "# O : violet\n" +
   "# B : blue/violet\n" +
   "# A : blue\n" +
   "# F : green/yellow\n" +
   "# G : yellow\n" +
   "# K : orange\n" +
   "# M R N S C : red\n" +
   "# T L : brown\n" +
   "# W : violet\n" +
   "# D : gray\n\n" +
   "$[src.spType*] = \"*B*\" {draw #8a2be2}\n" +
   "$[src.spType*] = \"*A*\" {draw blue}\n" +
   "$[src.spType*] = \"*F*\" {draw #adff2f}\n" +
   "$[src.spType*] = \"*G*\" {draw yellow}\n" +
   "$[src.spType*] = \"*K*\" {draw orange}\n" +
   "$[src.spType*] = \"*M*\" || \n$[src.spType*] = \"*R*\" || \n$[src.spType*] = \"*N*\" || \n" +
   "$[src.spType*] = \"*S*\" || \n$[src.spType*] = \"*C*\" {draw red}\n" +
   "$[src.spType*] = \"*T*\" || \n$[src.spType*] = \"*L*\" {draw #a52a2a}\n" +
   "$[src.spType*] = \"*O*\" {draw #ee82ee}\n" +
   "$[src.spType*] = \"*W*\" {draw #ee82ee}\n" +
   "$[src.spType*] = \"*D*\" {draw gray}\n",
   // filtre avec unité
   "# Unit.Conversion\n" +
   "# You can specify a unit when writing a constraint\n" +
   "# Unit conversion is automatically computed (when possible)\n" +
   "$[phot.flux;em.X-ray]>10^-8 erg/m^2/s {\ndraw circle($[phot.flux;em.X-ray])\n}"
   };


   // Labels des filtres predefinies
   static final String[] PREDEFLABELS = {"Magnitude circle", "Ellipses", "Magnitude cut",
      "Display text", "Parameterized colors", "Select object type", "Proper motions", "Color index",
      "Spectral types", "Unit conversion"};

    // Mémoire des def. des filtres
   static String[] saveFilters = new String[LIMIT];

    // Mémoire des labels des filtres
   static String[] saveLabels = new String[LIMIT];

   String script;	// Code courant du script (A MODIFIER/UTILISER PAR THOMAS)

   // numero du filtre (ie nombre de filtres deja crees - 1)
   static int num=-1;

   static PlanFilter[] allFilters = new PlanFilter[0];

   // identifiant pour le filtre
   int numero;

   // objet qui effectue reellement le filtrage
   private UCDFilter filter;



// objets mémorisant les plans influencés par le filtre
   private Vector<Plan> memPlan, omemPlan;

   // true si il faut reappliquer le filtre (changement au niveau
   // des sources sur lesquellles le filtre s'applique)
   private boolean mustUpdate = true;

   boolean mustRepaint = true;

   boolean initPlanMem = true;

   protected Plan plan; // PIERRE 23/08/05, dans le cas d'un filtre prédéfini, planCatalog de rattachement

  /////// Constructeurs ///////

  /** Creation d'un plan de type FILTER
   * @param a Reference interne
   * @param label le nom du plan (ou null si aucun)
   * @param script le code initial (ou null si aucun)
   * @param planCatalog dans le cas d'un filtre attaché à un plan Catalogue, null sinon (PIERRE 23/05/08)
   */
   protected PlanFilter(Aladin aladin,String label,String script) { this(aladin,label,script,null); }
   protected PlanFilter(Aladin aladin,String label,String script,Plan plan) {
      this.plan = plan; // (PIERRE 23/05/08)
      this.aladin = aladin;
      flagOk=true;
      type=FILTER;
      c = Color.black;
      askActive=true;       // PIERRE 23/05/08
      selected=true;
      // si num>=LIMIT, on doit agrandir les tableaux de source (methode de source)
      numero = ++num;

      memPlan = new Vector();
      omemPlan = new Vector();

      // Si on doit reallouer allFilters et les tableaux de Source
      if(num>=LIMIT) {
         realloc();
      }


      if( script!=null) {
         createFilter(script, label);
      } else {
         createFilter("", "Filter"+num);
      }

      this.script = filter.definition;
      this.label = filter.name;


      String orgName = filter.name;

      // chaque nom doit etre unique
      uniqueName(orgName);
      // sauvegarde du nom et de la definition
      saveDef();
      if( script!=null ) doLog();

   }

   /////// EOF Constructeurs ///////

   // Crée un label unique à partir de orgName
   private void uniqueName(String orgName) {
      int compteur=1;
      String name = orgName;
      while( getFilterByName(name,aladin) != null ) {
            name = orgName+compteur++;
      }
      this.label = name;
   }

   private void saveDef() {
      saveFilters[numero] = script;
      saveLabels[numero] = label;
   }

   /** Methode permettant de modifier la definition du filtre
    *  @param script - la definition du filtre
    *  @param name - nom du filtre
    *  @param fp - FilterProperties d'ou on appelle cette méthode
    */
   protected void updateDefinition(String script, String name, FilterProperties fp) {
      String oldDef = this.script;
      if( name!=null ) name = UCDFilter.skipSpaces(name);
      this.script = script;
      createFilter(script,name);
      this.script = filter.definition;
      this.label = filter.name.length()==0?this.label:filter.name;

      String save = this.label;
      // on met un label bidon pour que uniqueName ne trouve pas celui de this
      this.label="";
      uniqueName(save);

      saveDef();
      doLog();
	  // surement à modifier, pb des espaces dans les chaines ${sp}="t" != ${sp}=" t "
	  // doit on updater à chaque fois ?
	  // TESTER avec 1 2 3 4 ... plans éteints
      if( !UCDFilter.skipSpaces(this.script).equals(UCDFilter.skipSpaces(oldDef)) ) {
      	setPlanMemory();
		//System.out.println("on doit updater : "+morePlans(omemPlan,memPlan));
		updateInfluence();
		mustUpdate = true;
		/*
//		remplacer ligne ci dessous par la même action mais sans le resetActions qui me semble inutile

         setMustUpdate();
         if( !mustUpdate ) {
         	mustUpdate = true;
         	// Je pense que la ligne ci dessous est inutile
         	filter.resetActions();
         }
         */
      }


      if( !isValid() ) {
         if( fp==null || !fp.isShowing() ) {
            Aladin.error(aladin.chaine.getString("BADFILTER"),1);
         }
         this.setActivated(false);
         aladin.calque.select.repaint();
         aladin.view.setMesure();

         return;
      }
      // a faire, maj de l affichage si necessaire
      if( isOn() ) applyFilter();
   }

	/** Logue le nom et la définition du filtre */
	private void doLog() {
	    // les '\n' sont remplacés par un backslash pour que la définition ne prenne qu'une ligne dans le fichier de log
//	    sendLog("Filter","Label: "+this.label+"\tDef: "+this.script.replace('\n','\\'));
	    // 13/02/07 : on ne conserve plus que le nom du filtre au niveau des logs
	    sendLog("Filter","Label: "+this.label);
	}

   /** Methode privee permettant d'initialiser l'objet filter
    *  a partir de la definition d'un filtre
    *  @param def - la definition du filtre
    *  @param name - nom du filtre (ou null si il est contenu dans def)
    *  Si name==null, on suppose que le nom est inclus dans la definition du filtre
    */
   private void createFilter(String def, String name) {
      // dans le cas ou le nom est inclus dans la definition
      if(name==null) {
         filter = new UCDFilter(def,aladin,this);
      } else { // nom et contraintes separees

         filter = new UCDFilter(name, def, aladin,this);
      }

      filter.setNumero(this.numero);

      if( !isValid() ) {
         error = "ERROR";
      } else {
         error = null;
      }

   }

   /** Methode appelee a chaque activation/desactivation du plan */
   protected void updateState () {
      //System.out.println("update state");
      if( isOn() ) {
         if( !isValid() ) {
            Aladin.error(aladin.chaine.getString("BADFILTER"),1);
            this.setActivated(false);
            aladin.calque.select.repaint();
            aladin.view.setMesure();

            return;
         }
         mustRepaint = true;
         applyFilter();
      } else {
         aladin.view.setMesure();
         //aladin.view.repaintAll();
      }
   }

   protected void applyFilter() {
   	  // nécessaire pour ne pas avoir de pb avec la commande "sync" [thomas 03/02/2005]
   	  if( mustUpdate ) {
   	  	flagOk = false;
   	  }
      if( initPlanMem ) {
         setPlanMemory();
         updateInfluence();
         initPlanMem = false;
      }
      // On arrête d'abord le précédent thread (si on réappuie sur apply avant la fin)
      stopFilterThread();
      synchronized( this ) {
         runme = new Thread(this,"AladinFilterApply");
         Util.decreasePriority(Thread.currentThread(), runme);
//         runme.setPriority( Thread.NORM_PRIORITY -1);
         runme.start();
      }
   }

   /** arrete proprement le thread courant */
   private synchronized void stopFilterThread() {
	   if( runme==null ) return;

	   Thread oldThread = runme;
	   // en settant runme à null, on va forcer le thread à s'arreter (test dessus dans la boucle)
	   runme = null;
	   // anciene méthode --> beurk !!
//	   oldThread.stop();
	   try {
//		   System.out.println("debut join");
		   // on attend que le thread finisse
		   oldThread.join();
//		   System.out.println("fin join");
	   }
	   catch(InterruptedException ie) {
//		   ie.printStackTrace();
	   }
	   catch(Exception e) {
		   if( Aladin.levelTrace>=3 ) e.printStackTrace();
	   }
//	   System.out.println("stop filter thread !");
   }
   
   
   private long lastFilterLock=-1; // Date du dernier test de isSync() sur le filtre. Si on dépasse 4s, on relance
   private boolean relaunchDone=false; // true si on vient de relancer manuellement un filtre (sur un isSync() bloquant)
   
   /** retourne true si le filtre a été appliqué. Sinon mémorise la date, et si la durée du blocage dépasse 4s, 
    * on relance manuellement le filtre (PF. janvier 2011)
    * Nécessaire dans le cas de script, on peut se retrouver dans un cas où Command. attend indéfiniment la synchronisation
    * du plan filtre, mais celui-là n'est jamais relancé.
    */
   protected boolean isSync() {
      if( isSync1() ) {
         lastFilterLock=-1;
         return true;
      }

      long time = System.currentTimeMillis();
      
      if( lastFilterLock==-1 ) lastFilterLock=time;
      
      // Ca fait longtemps que ça bloque ?!
      else if( time-lastFilterLock>4000 ) {

         // déjà relancé une fois, tant pis on dit que c'est bon
         if( relaunchDone ) {
            if( aladin.levelTrace>=3 ) System.err.println("PlanFilter.isSync()=false, last manual relaunch did not work => assume isSync()=true");
            lastFilterLock=-1; relaunchDone=false;
            return true;
         }

         // On tente de relancer une fois manuellement
         if( aladin.levelTrace>=3 ) System.err.println("PlanFilter.isSync()=false but delay exceed 4s => relaunch filter manually...");
         relaunchDone=true;
         doApplyFilter();
         lastFilterLock=time;
      }
      return false;
   }

   private boolean isSync1() {
      return flagOk && !(error==null && mustUpdate);
   }

   /** Methode appliquant le filtre sur tous les PlanCatalog concernés
    * @param inThread si true, on appelle la méthode depuis le thread du filtre
    */
   protected void doApplyFilter(boolean inThread) {
      if( mustUpdate) {
         Aladin.trace(1,"Updating filter results");
         if( !isValid() ) {
            setActivated(false);
            flagOk = true;
            return;
         }
         else {
            flagOk = false;
            aladin.calque.repaintAll();
            Source[] sources = getSources(aladin, inThread);
            if( inThread && runme==null ) return;
            resetFlags();
            filter.getFilteredSources(sources, inThread);
            if( inThread && runme==null ) return;
            flagOk = true;
            setPourcent(-1);
         }
      }

      if( mustRepaint ) {
         synchronized(aladin.mesure) {
         	aladin.view.setMesure();
         }
      }
      mustUpdate = false;

   }

   protected void doApplyFilter() {
	   doApplyFilter(false);
   }

   /** returns all sources of active plans and which are influenced by the filter
    *  @param aladin - reference to the Aladin object
    *  @param inThread si true, on appelle la méthode depuis le thread du filtre
    *  @return the array of sources of all active plans
    */
   protected Source[] getSources(Aladin a, final boolean inThread) {
      int i;
      Plan p = null;
      Vector<Source> vec = new Vector<>();
      Plan[] plans = getConcernedPlans();
      int k=1;

      // loop on all plans and selection of catalogs which are active
      // we retrieve all sources in active plans
      for( i=plans.length-1; i>=0; i-- ) {
         p = plans[i];
         //if( p.type == Plan.CATALOG && p.flagOk && p.active ) {
            Iterator<Obj> it = p.iterator();
            if( it==null ) continue;
            while( it.hasNext() ) {
               Obj s= it.next();

               // should we stop the processing
               if( inThread && k%1000==0 && runme==null ) {
            	   break;
               }

//               if( s instanceof Source && s!=null) {
               if( s!=null && s.asSource() ) {
                  vec.addElement((Source)s);
                  k++;
               }
            }
         //}
      }

      Source[] sources = new Source[vec.size()];
      vec.copyInto(sources);
      vec = null;

      return sources;
   }
   protected Source[] getSources(Aladin a) {
	   return getSources(a, false);
   }


   /** Retourne le folder dans lequel se trouve this, null si dans aucun */
   private Plan getFolder() {
      Plan p;
      Plan[] plansOfFolder;
      Plan [] allPlan = aladin.calque.getPlans();
      for( int i=allPlan.length-1;i>=0;i-- ) {
         p = allPlan[i];
         if( p.type!=Plan.FOLDER ) continue;
         plansOfFolder = aladin.calque.getFolderPlan(p);
         for( int j=0;j<plansOfFolder.length;j++) {
            if( plansOfFolder[j]==this ) {return p;}
         }
      }

      return null;
   }


/** retourne tous les plans d'un folder donne (plans des sous-folders inclus) */
   private Vector<Plan> getAllPlansOfFolder(Plan folder) {
      Vector<Plan> vec = new Vector<>(10);
      getAllPlansOfFolder(folder,vec);
      return vec;
   }

   /** Methode recursive utilisee par getAllPlansOfFolder(Plan folder) */
   private void getAllPlansOfFolder(Plan folder, Vector<Plan> vec) {
      Plan curPlan;
      Plan[] plans = aladin.calque.getFolderPlan(folder);
      for( int i=0;i<plans.length;i++ ) {
         curPlan = plans[i];
         if( curPlan.type==Plan.FOLDER ) {
            getAllPlansOfFolder(curPlan,vec);
         } else {
            vec.addElement(curPlan);
         }
      }
   }

   /** morePlans
    *  @param oldMem ancienne memoire
    *  @param newMem nouvelle memoire
    *  @return true si newMem contient des plans qui n'étaient pas dans oldMem
   */
   private boolean morePlans(Vector oldMem, Vector newMem) {
      if( newMem.size() > oldMem.size() ) return true;
      Enumeration e = newMem.elements();
      while( e.hasMoreElements() ) {
         if( !oldMem.contains(e.nextElement() ) ) return true;
      }
      return false;
   }

   /** Met à jour la mémoire des plans influencées par le filtre
       PREconditions : omemPlan et memPlan ne sont pas null
   */
   private void setPlanMemory() {
      Plan[] plans = getConcernedPlans();
      omemPlan = memPlan;
      memPlan = new Vector<>();
      for( int i=0; i<plans.length; i++ ) {
         memPlan.addElement(plans[i]);
      }
      //System.out.println("avant : "+omemPlan.size()); //System.out.println("apres : "+memPlan.size());
   }

   /** met mustUpdate à true si nécessaire */
   protected void setMustUpdate() {
      setPlanMemory();
      //System.out.println("on doit updater : "+morePlans(omemPlan,memPlan));
      updateInfluence();
      if( morePlans(omemPlan, memPlan) ) {
      	 //System.out.println("mustUpdate nécessaire");
         mustUpdate = true;
         filter.resetActions();
      }
   }

   /** met à jour les influences
       PREconditions : memPlan est à jour
   */
   private void updateInfluence() {
      Plan p;
      for( int i=aladin.calque.plan.length-1;i>=0;i-- ) {
         p = aladin.calque.plan[i];
         if( !p.isCatalog() ) continue;
         p.influence[this.numero] = memPlan.contains(p);
      }
   }


   /** Retourne les plans concernes par le filtre, ie les plans catalogue dans le meme folder et sous le PlanFilter
    *  (PIERRE 23/08/05 ou le planCatalog associé dans le cas d'un filtre prédéfini)
    *  @return le tableau des plans concernes
    */
   protected Plan[] getConcernedPlans() {

      // PIERRE 23/08/05 - cas d'un filtre prédéfini pour un plan Catalog
      if( plan!=null ) return new Plan[]{plan};

      int pos = getPositionOfPlan(this); // position du plan this
      //System.out.println("Position : "+pos);
      Plan p; // plan courant
      Vector plans = new Vector();
      Plan folder = getFolder();
      Vector plansOfThis = null;

      if(folder!=null) {
         plansOfThis = getAllPlansOfFolder(getFolder());
      }

      for( int i=aladin.calque.plan.length-1;i>=pos;i-- ) {
         p = aladin.calque.plan[i];
         if( !p.isCatalog() || !p.flagOk || !p.active ) {continue;}
         // si le plan est dans un folder
         if( folder==null || plansOfThis.contains(p) ) {
            plans.addElement(p);
         }
      }
      // Copie dans un tableau avant de le retourner
      Plan[] ret = new Plan[plans.size()];
      plans.copyInto(ret);
      return ret;
   }

   /** Retourne la position dans aladin.calque.plan du plan
    *  @param plan - le plan dont on cherche la position
    *  @return la position du plan, -1 si non trouve
    */
   private int getPositionOfPlan(Plan plan) {
      Plan curPlan;
      for( int i=aladin.calque.plan.length-1;i>=0;i-- ) {
         curPlan = aladin.calque.plan[i];
         if( curPlan==plan ) {
            return i;
         }
      }

      return -1;
   }

   /** reset isSelected flags of all sources, reset influence flags of all PlanCatalog and set them to false
    *
    */
   private void resetFlags() {
      int i;
      Plan p = null;

      // loop on all plans and selection of catalogs
      // note : faut-il le faire seulement sur les cats actifs ou sur tous ?
      for( i=aladin.calque.plan.length-1; i>=0; i-- ) {
         p = aladin.calque.plan[i];
         if( p.isCatalog() && p.flagOk ) {
            Iterator<Obj> it = p.iterator();
            while( it.hasNext()) {
               Obj o = it.next();
//               if( !(o instanceof Source) ) continue;
               if( !o.asSource() ) continue;
               Source s = (Source)o;
               if( s!=null) {
                  // allocation mémoire pour le tableau "isSelected" liés aux sources
    			  if( s.isSelected==null ) s.isSelected = new boolean[PlanFilter.LIMIT];

                  s.isSelected[numero] = false;
               }
            }
         }
      }
   }

   /** Selection de toutes les sources filtrees par le plan */
   protected void select() {
      filter.select(getSources(aladin));
         aladin.view.setMesure();
         //aladin.view.repaintAll();
   }

   /** Exportation des sources filtrees vers un nouveau PlanCatalog */

   protected void export() {
      Source[] selectedSources = filter.getFilteredSources(getSources(aladin));
      PlanCatalog p = aladin.calque.newPlanCatalogBySources(new Vector(Arrays.asList(selectedSources)), "Filter.src",false);
      if( p!=null ) aladin.view.selectAllInPlan(p);

      setPourcent(-1);
      aladin.view.repaintAll();
   }


   /** Methode appelee lorsque la position du PlanFilter sur le stack a ete modifiee */
   protected void positionChange() {
      //System.out.println("position change");
      //System.out.println("\nPosition change pour "+this.label);
      updateAllFilters(aladin); // permet de prendre en compte l'ordre des filtres
      setMustUpdate();
      if( isOn() ) applyFilter();
   }

   /** Le plan filtre est-il actif */
   protected boolean isOn() {
      if( plan!=null ) return true; // PIERRE 23/08/05 POUR FORCER L'ACTIVATION - A VOIR EN DETAIL AVEC THOMAS
      return active;
   }

   /** Teste la validite syntaxique du filtre
    *  @return true si la syntaxe est correcte, false sinon
    */
   protected boolean isValid() {
      return !filter.badSyntax;
   }

   /** PF oct 2007 - J'ai dû surcharger cette méthode sinon il y avait un changement
    * de sélection de plan intempestif pour Simbad et NED (filtre prédéfini)
    */
   protected void planReady(boolean ready) { }


   /** Methode appelee par le thread de calcul */
   protected boolean waitForPlan() {
//      System.out.println("debut thread");
      doApplyFilter(true);
//      System.out.println("finthread");
      return true;
   }

   /** Realloue de la memoire pour les differents tableaux */
   private void realloc() {
      LIMIT += INCREMENT;

      Plan p;
      Source s;

      for( int i=aladin.calque.plan.length-1;i>=0;i-- ) {
         p = aladin.calque.plan[i];
         if( p.isCatalog() ) {
            // realloc du tableau influence
            boolean[] tmp2 = new boolean[LIMIT];
            System.arraycopy(p.influence,0,tmp2,0,p.influence.length);
            p.influence = tmp2;

            Iterator<Obj> it = p.iterator();
            if( it!=null ) while( it.hasNext() ) {
               Obj o = it.next();
//               if( !(o instanceof Source) ) continue;
               if( !o.asSource() ) continue;
               s = (Source)o;

               // realloc de isSelected
               boolean[] tmp = new boolean[LIMIT];
               if( s.isSelected==null ) s.isSelected = new boolean[PlanFilter.LIMIT];
               System.arraycopy(s.isSelected,0,tmp,0,s.isSelected.length);
               s.isSelected = tmp;


               // realloc de actions
               if( s.actions==null ) s.actions = new Action[PlanFilter.LIMIT][];
               Action[][] ac = new Action[LIMIT][];
               for( int k=0;k<LIMIT-INCREMENT;k++ ) {
                  ac[k] = s.actions[k];
               }
               s.actions = ac;

               // realloc de values
               if( s.values==null ) s.values = new double[PlanFilter.LIMIT][][];
               double[][][] val = new double[LIMIT][][];
               for( int k=0;k<LIMIT-INCREMENT;k++ ) {
                  val[k] = s.values[k];
               }
               s.values = val;

            }
         }
      }
      // realloc de saveFilters
      String[] tmp = new String[LIMIT];
      System.arraycopy(saveFilters,0,tmp,0,saveFilters.length);
      saveFilters = tmp;

      // realloc de saveLabels
      tmp = new String[LIMIT];
      System.arraycopy(saveLabels,0,tmp,0,saveLabels.length);
      saveLabels = tmp;
   }


   public UCDFilter getUCDFilter() {
       return filter;
   }

   // met a jour le tableau allFilters
   static protected void updateAllFilters(Aladin a) {
      allFilters = getAllFilters(a);
   }

   /** Methode permettant d'activer tous les filtres */
   static void activateAllFilters() {
      PlanFilter pf = null;
      if(allFilters==null) return;
      for( int i=0;i<allFilters.length;i++) {
         pf = allFilters[i];
         // on ne veut le repaint qu a la fin
         pf.mustRepaint = false;
         pf.applyFilter();
         /*if( pf.mustUpdate) {
            Source[] sources = pf.getSources(pf.aladin);
            pf.resetFlags();
            pf.filter.getFilteredSources(sources);
         }
         pf.mustUpdate = false;*/
         pf.setActivated(true);
      }

      if(pf!=null) {
         pf.aladin.view.setMesure();
         //pf.aladin.view.repaintAll();
         pf.aladin.calque.select.repaint();

         pf.mustRepaint = true;
      }

   }

   /** Methode permettant de desactiver tous les filtres */
   static void desactivateAllFilters() {
      PlanFilter pf = null;
      if(allFilters==null) return;
      for( int i=0;i<allFilters.length;i++) {
         pf = allFilters[i];
         pf.setActivated(false);
      }

      if(pf!=null) {
         pf.aladin.view.setMesure();
         //pf.aladin.view.repaintAll();
         pf.aladin.calque.select.repaint();
      }
   }

   /** Renvoie tous les filtres */
   static private PlanFilter[] getAllFilters(Aladin a) {
      Vector v = new Vector();
      Plan p;

      for( int i=a.calque.plan.length-1;i>=0;i-- ) {
         p = a.calque.plan[i];
         if( p.type == Plan.FILTER ) {
            v.addElement(p);
         }

         // TEST PIERRE 23/8/05, pour ajouter à la liste des filtres ceux qui sont
         // directement associés à des plans catalogues via les filtres prédéfinis
         if( p.isCatalog() ) {
            PlanFilter planFilter = p.getFilter();
            if( planFilter!=null ) v.addElement(planFilter);
         }
      }

      PlanFilter[] ret = new PlanFilter[v.size()];
      v.copyInto(ret);
      return ret;
   }

   /** Liberation de la memoire */
   protected boolean Free() {
      stopFilterThread();
      memPlan = null;
      omemPlan = null;
      filter.Free();
      super.Free();
      // maj des filtres existant
      PlanFilter.updateAllFilters(aladin);
      // maj du Choice avec la liste des filtres
      FilterProperties.majFilterProp(true,false);
      return true;
   }

   static protected PlanFilter getFilterByName(String s, Aladin a) {
      Plan p;

      for( int i=a.calque.plan.length-1;i>=0;i-- ) {
         p = a.calque.plan[i];
         if( p.type == Plan.FILTER && p.label.equals(s)) {
            return (PlanFilter)p;
         }
      }

     return null;
   }

   /** Methode appelee lorsqu'un PlanCatalog a bouge
    *  @param p - le plan qui a bouge
    *  @param oldPos - ancienne position du plan
    *  @param newPos - nouvelle position du plan
    */
   static void updatePlan(Plan p, int oldPos, int newPos) {
    //System.out.println("update plan pos");
      PlanFilter pf = null;
      if(allFilters==null) return;
      for( int i=0;i<allFilters.length;i++) {
         pf = allFilters[i];
         pf.setMustUpdate();
         if( pf.isOn() ) pf.applyFilter();
      }
   }


   /** Methode appelee lorsqu'on allume un plan */
   static void updatePlan(Plan p) {
//    System.out.println("update plan");
      PlanFilter pf = null;
      if(allFilters==null) return;
      for( int i=0;i<allFilters.length;i++) {
         pf = allFilters[i];
//         System.out.println("appel de mustUpdate pour filtre "+pf.label);
         pf.setMustUpdate();
         if( pf.isOn() ) pf.applyFilter();
      }
   }

   /** Methode appelee lorsqu'un nouveau plan catalogue est charge */
   static void newPlan(Plan p) {
//    System.out.println("new plan");
      PlanFilter pf = null;
      if(allFilters==null) return;
      for( int i=0;i<allFilters.length;i++) {
         pf = allFilters[i];
         pf.setMustUpdate();
         if( pf.isOn() ) pf.applyFilter();
      }
   }

   /** Test Pierre pour PlanBGCat */
   static void updateNow() {
      PlanFilter pf = null;
      if(allFilters==null) return;
      for( int i=0;i<allFilters.length;i++) {
         pf = allFilters[i];
//         System.out.println("updateNow. filter "+i+" pf="+pf+" pf.isOn="+pf.isOn());
         pf.setMustUpdate();
         pf.setPlanMemory();
//         pf.updateInfluence();
         pf.mustUpdate = true;
         if( pf.isOn() ) pf.applyFilter();
      }
   }

/** Generation du label du plan.
 * Retourne le label en fonction de l'etat courant du plan
 * Il s'agit simplement d'ajouter des "..." quand le plan est en
 * cours de construction ainsi que le pourcentage de loading
 * @return Le label genere
 */
// protected String getLabel() {
//	int p = (int)getPourcent();
//	if( p>0 ) {
//	   return label.substring(0,5>label.length()?label.length():5)+"..  "+p+"%";
//	}
//	return super.getLabel();
// }
}
