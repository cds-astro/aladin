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

import cds.tools.Util;
import cds.xml.Field;

/**
 * Gestion des entites (suite de mots) composants une mesure.
 * Peut etre un triangle de repere, un texte simple, une ancre
 * <P>
 * - Les Triangles sont des mots qui commencent simplement par "_"<BR>
 * - Les ancres sont des tags GLU (a la syntaxe la plus simple)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (11 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Words implements Runnable {

   // Differents types d'alignement possibles
   static final int LEFT   = 0;
   static final int RIGHT  = 1;
   static final int CENTER = 2;
   static final int COORD  = 3;	// Sur le signe + ou - de la declinaison

   // Les composantes de l'objet
   String text;		    // Texte a afficher (sauf repere)
   int width;		    // Nbre de caracteres, 0 si non cadre
   int precision;       // Nbre de digits derrière la virgule
   String id;		    // Identificateur pour une marque GLU
   String param;	    // Parametres pour une marque GLU
   int x,y;		        // Position (reference au texte)
   int w,h;		        // Largeur et hauteur (totales)
   int align;	    	// LEFT, RIGHT, CENTER ou COORD
   int size;		    // Utilise par le tag GLU ??????
   int sort;            // pour une entête, permet d'indiquer le tri courant

   boolean computed = false; // vrai s'il s'agit d'une colonne calculée

   // Type de mots
   boolean glu;		    // Marque GLU
   boolean repere;	    // checkbox
   boolean pin;         // Epinglette
   boolean archive;	    // Bouton (acces a une archive FITS)
   boolean samp;        // Bouton (utilisera SAMP)
   boolean footprint;   // Footprint associé

   // Les variables d'etat
   boolean onMouse;	        // Mot sous la souris
   boolean show;            // Mot surligné pour être désigné
   boolean pushed=false;    // Ancre qui vient d'etre cliquee -> en rouge
   boolean haspushed=false; // Ancre qui a ete cliquee -> violet

   // Variable de travail
   Thread thread;	    // Utilise lors de l'appel d'une marque GLU

   // Les References
   MCanvas  m;		    // Canvas pour l'affichage des mesures
   Glu     g;		    // Pour faire appel au GLU

   /** Creation d'une sequence de mots.
    * Determine s'il s'agit d'un tag GLU, d'un repere (triangle) ou
    * d'une simple sequence.
    * @param tag La sequence
    * @param naxis1 le nombre de caracteres d'affichage
    * @param align le type d'alignement
    * @param computed s'agit-il d'un champ calculé ?
    */
   protected Words(String tag) { this(tag,null,0,-1,LEFT, false,Field.UNSORT); }
   protected Words(String tag,int width) { this(tag,null,width,-1,LEFT, false,Field.UNSORT); }
   protected Words(String tag,int width,int precision,int align) {
      this(tag, null,width, precision,align, false,Field.UNSORT);
   }
   protected Words(String tag,String defText,int width,int precision,int align,boolean computed,int sort) {
      this.width = width;
      this.precision = precision;
      this.align = align;
      this.computed = computed;
      this.sort=sort;
      char [] a = tag.toCharArray();
      if( !(glu=tagGlu(a)) ) text=tag;
      if( defText!=null ) text=defText;
      setRepere();
   }

   protected Words(String tag,int width,int precision,int align,boolean computed,boolean footprint) {
      this(tag,width,precision,align);
      this.computed = computed;
      this.footprint = footprint;
   }

   /** Positionne le flag repere et archive a true si c'est le cas */
   void setRepere() {
      repere=false;                // Par defaut

      if( !glu ) return;

      // Recuperation du premier caractere de l'identificateur GLU
      String type = id.substring(0,1);

      // Les reperes sont des tags GLU dont l'id commence par _
      if( type.equals("_") ) repere=true;
      else if( type.equals("^") ) archive=true;
      else if( type.equals("£") ) { archive=true; samp=true; }
      else return;

      // On enleve le premier caractere
      id = id.substring(1);
   }

   /** Modifie la position.
    * @param x,y Nouvelle position
    */
   protected void setPosition(int x,int y) { this.x = x; this.y = y;}

   /** Retourne le texte associe a Word */
   protected String getText() { return text; }

   /** Affiche dans aladin.urlStatus l'URL ou la marque GLU associee */
   protected void urlStatus(MyLabel urlStatus) {
      String s;
      if( id.equals("Http") ) s=param;
      else s=(param.length()>0)?"Glu tag: <&"+id+" "+param+">":"Glu: <&"+id+">";
      urlStatus.setText(s);
   }

   /** Modifie la position et la taille.
    * @param x,y Nouvelle position
    * @param w,h Nouvelle taille
    */
   protected void setPosition(int x,int y, int w, int h) { this.x = x; this.y = y; this.w = w; this.h = h; }

   /** Analyse de chaine GLU.
    * Met a jour la variable text avec un eventuel texte d'ancre (<&...|texte>)
    * @param a La chaine en cours d'analyse
    * @param i L'indice de la position courante
    * @return  Indice du prochain caratere a analyser, ou <I>-1</I> si fini
    */
   int anchorGlu(char [] a, int i) {
      int j;
      StringBuffer anchor = new StringBuffer();

      for( j=i+1; j<a.length && a[j]!='>'; j++) anchor.append(a[j]);
      if( j==i ) return j;
      this.text = anchor.toString();
      return (j==a.length)?-1:j;
   }

   /** Analyse de chaine GLU.
    * Met a jour la variable param avec d'eventuels parametres (<&..params|...>)
    * @param a La chaine en cours d'analyse
    * @param i L'indice de la position courante
    * @return  Indice du prochain caratere a analyser, ou <I>-1</I> si fini
    */
   int paramGlu(char [] a, int i) {
      int j;
      StringBuffer param = new StringBuffer();

      for( j=i; j<a.length && a[j]==' ' && a[j]!='>' && a[j]!='|'; j++);
      for( ; j<a.length && a[j]!='>' && a[j]!='|'; j++) param.append(a[j]);
      if( j==i ) return j;
      this.param = this.text = param.toString();
      return (j==a.length)?-1:j;
   }

   /** Analyse de chaine GLU.
    * Met a jour la variable id avec un eventuel identificateur (<&id ...|...>)
    * @param a La chaine en cours d'analyse
    * @param i L'indice de la position courante
    * @return  Indice du prochain caratere a analyser, ou <I>-1</I> si fini
    */
   int idGlu(char [] a, int i) {
      int j;
      StringBuffer id = new StringBuffer();

      for( j=i; j<a.length && a[j]!=' ' && a[j]!='>' && a[j]!='|'; j++) id.append(a[j]);
      if( j==i ) return -1;
      this.id = this.text = id.toString();
      return (j==a.length)?-1:j;
   }

   /** Analyse de chaine GLU.
    * Met a jour les variables id,params,text associees a une
    * marque GLU (<&id params|text>)
    * @param a La chaine a analyser
    * @return <I>true</I> s'il s'agit effectivement d'un tag GLU,
    *         sinon <I>false</I>
    */
   protected boolean tagGlu(char [] a ) {
      if( a.length<2 || a[0]!='<' || a[1]!='&' ) return false;
      int i=2;
      if( (i=idGlu(a,i))<0 ) return false;
      if( (i=paramGlu(a,i))<0 ) return false;
      if( a[i]=='|' ) if( (i=anchorGlu(a,i))<0 ) return false;
      if( a[i]!='>' ) return false;
      size = i+1;
      return true;
   }

   /** Test d'appartenance.
    * @param xc,yc Position de la souris
    * @return <I>true</I> si la position est dans la sequence de mots
             sinon <I>false</I>
    */
   protected boolean inside(int xc,int yc) {
      return xc>=x-4 && xc<=x+w+2 && yc>=y-1 && yc<=y+h+1;
   }

   /** Retourne true si on est sur le bord droit */
   protected boolean onBord(int xc,int yc) {
      return xc>=x+w-3 && xc<=x+w+3 && yc>=y-1 && yc<=y+h+1;
   }

   /** Appel au GLU.
    * L'appel au GLU se fait par un Thread independant.
    * @param g Reference au GLU
    * @param m Rerefence au Canvas des mesures
    */
   protected void callGlu(Glu g,MCanvas m) {
      this.g = g;
      this.m = m;
      haspushed=pushed=true;

      /*
      // ATTENTION, CETTE METHODE n'AUTORISE QU'UN SEUL PARAMETRE
      // POUR LA MARQUE GLU
      if( !id.equals("Http") ) {
         param= (Glu.cutParam(param))[0];
         param = URLEncoder.encode(param);
      }
       */
      thread = new Thread(this,"AladinCallGlu");
      thread.setPriority( Thread.NORM_PRIORITY -1);
      thread.start();
   }

   public void run() {
      if( callArchive ) { callArchive=false; callArchive1(_aladin,_o); }
      else g.showDocument(id,param,id.equals("Http"));
      Util.pause(3000);
      pushed=false;
      if( m!=null ) m.repaint();
   }

   private boolean callArchive=false;
   private Aladin _aladin;
   private Obj _o;

   protected void callArchive(Aladin aladin,Obj o) {
      haspushed=pushed=true;
      callArchive=true;
      _aladin=aladin;
      _o=o;
      thread = new Thread(this,"AladinCallGlu");
      thread.setPriority( Thread.NORM_PRIORITY -1);
      thread.start();
   }

   private void callArchive1(Aladin aladin,Obj o) {
      String label = param;
      boolean flagHttp = id.equals("Http");
      String url=null;

      // URL ou nom de fichier
      if( flagHttp ) {
         url = param;

         // tag Glu
      } else {
         try { url = aladin.glu.getURL(id,param,flagHttp)+"";
         if( url==null ) throw new Exception("Error during GLU resolution !"); }
         catch( Exception e) {
            aladin.warning(aladin,"URL error");
            if( Aladin.levelTrace>=3 ) e.printStackTrace();
            return;
         }
      }

      // Les noms basé sur une url son généralement trop long
      if( label.startsWith("http://") || label.startsWith("https://")
            || label.startsWith("ftp://") ) label=text;

      // Cas particulier où il faut transmettre l'URL à une application tierce via SAMP
      if( samp ) {
         //         System.out.println("Je dois transmettre à SAMP les données ["+label+"] via l'URL suivante : "+url);
         aladin.mesure.mcanvas.toSamp(url,x+w/2,y);
         return;
      }

      aladin.calque.newPlan(url,label,"provided by the original archive server", o);

      //      String objet=Coord.getSexa(((Position)o).raj,((Position)o).dej,"s");
      //      aladin.calque.newPlanImage(url,PlanImage.OTHER,
      //                                 label,objet,param,
      //                                 "provided by the original archive server",
      //                                 PlanImage.UNKNOWN,PlanImage.UNDEF,
      //                                 o);
   }
}
