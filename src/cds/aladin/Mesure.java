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

package cds.aladin;
import static cds.aladin.Constants.ACCESSFORMAT_UCD;
import static cds.aladin.Constants.ACCESSURL;
import static cds.aladin.Constants.CONTENTTYPE;
import static cds.aladin.Constants.CONTENT_TYPE_PDF;
import static cds.aladin.Constants.CONTENT_TYPE_TEXTHTML;
import static cds.aladin.Constants.CONTENT_TYPE_TEXTPLAIN;
import static cds.aladin.Constants.DATATYPE_DATALINK;
import static cds.aladin.Constants.SEMANTICS;
import static cds.aladin.Constants.SEMANTIC_ACCESS;
import static cds.aladin.Constants.SEMANTIC_CUTOUT;
import static cds.aladin.Constants.SEMANTIC_PROC;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import cds.tools.Util;
import cds.xml.Field;

/**
 * Element de l'interface d'affichage des mesures
 *
 * @author Pierre Fernique [CDS]
 * @version 2   : (21 janvier 2004) Changement de mode de mémorisation
 * @version 1.2 : (25 juillet 2002) VOTable s'ajoute a Astrores
 * @version 1.1 : (28 mars 00) ReToilettage du code
 * @version 1.0 : (10 mai 99)  Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Mesure extends JPanel implements Runnable,Iterable<Source>,Widget {
   Aladin aladin;                 // Reference
   MCanvas mcanvas;               // Canvas des infos et mesures
   MyScrollbar scrollV;           // Scrollbar verticale
   MyScrollbar scrollH;           // Scrollbar horizontale
   JPanel haut;			  // Le panel haut
   Status status;		  // status propre a une fenetre independante
   boolean flagSplit;		  // true si la fenetre est independante
   int previousHeight=0;	  // Hauteur en fenetree
   Search search;           // Champ du search dans si la fenêtre est détachée

   // Gestion des Sources
   static private int DEFAULTBLOC = 100;
   static private int MAXBLOC = 100000;
   protected Source src[] = new Source[DEFAULTBLOC];   // Sources gérées
   protected int nbSrc=0;                            // Nb de sources gérées
   protected FrameMesure frameMesure=null;

   // Mémorisation des WordLines qui ont été affichées dans MCanvas afin
   // d'éviter de les regénérer à chaque fois et de perdre du coup
   // les paramètres associées au tracé (position, hauteur et largeur)
   private Hashtable memoWL = new Hashtable(DEFAULTBLOC);
   private JButton cross;
   
   protected DatalinkManager datalinkManager;
   public Words activeDataLinkWord = null;
   public Source activeDataLinkSource = null;
   public  SimpleData activeDataLinkGlu;
   
   JPopupMenu additionalServiceMenu;
   int datalinkshowX = -1;
   int datalinkshowY= -1;
   
   /** Creation du JPanel des mesures.
    * Il s'agit de creer MCanvas et la scrollV associee
    * @param aladin Reference
    */
   protected Mesure(Aladin aladin) {
      this.aladin = aladin;
      scrollV = new MyScrollbar(Scrollbar.VERTICAL,0,0,0,0);
      scrollH = new MyScrollbar(Scrollbar.HORIZONTAL,0,0,0,0);
      mcanvas = new MCanvas(aladin,scrollV,scrollH);
      scrollV.addAdjustmentListener(mcanvas);
      scrollH.addAdjustmentListener(mcanvas);
      status = new Status(aladin,"");
      status.setBackground( aladin.getBackground());
      flagSplit=false;

      search = new Search(aladin,false); search.setEnabled(true);
      search.setBackground( aladin.getBackground() );
      haut = new JPanel();
      haut.setBackground( aladin.getBackground() );
      haut.setLayout( new BorderLayout(2,2) );
      haut.add(status,BorderLayout.CENTER);
//      Aladin.makeAdd(haut,status,"Center");
      JPanel x = new JPanel(); x.add(search);
      x.setBackground( aladin.getBackground() );
//      Aladin.makeAdd(haut,x,"East");
      haut.add(x,BorderLayout.EAST);

      //       cross = new JButton(new ImageIcon(aladin.getImagette("Out.gif")));
      //       cross.setMargin(new Insets(0,0,0,0));
      //       cross.setBorderPainted(false);
      //       cross.setContentAreaFilled(false);
      //       cross.setToolTipText(aladin.chaine.getString("SPLITH"));
      //       cross.addActionListener( new ActionListener() {
      //          public void actionPerformed(ActionEvent e) { split(); }
      //       });

//      JPanel est = new JPanel(new BorderLayout(0,0));
//      est.add(scrollV,BorderLayout.CENTER);

      setLayout( new BorderLayout(0,0) );
      setBackground( aladin.getBackground() );
      add(haut,BorderLayout.NORTH);
      add(mcanvas,BorderLayout.CENTER);
      add(scrollV,BorderLayout.EAST);
      add(scrollH,BorderLayout.SOUTH);
      
      haut.setVisible(false);

      MFSEARCH=aladin.chaine.getString("MFSEARCH");
      //       MFSEARCHINFO=aladin.chaine.getString("MFSEARCHINFO");
      MFSEARCHO=aladin.chaine.getString("MFSEARCHO");
      MFSEARCHBAD=aladin.chaine.getString("MFSEARCHBAD");
      
      this.datalinkManager = new DatalinkManager();
   }
   
   /**
	* Method to display a datalink pop-up for the particular dataset user hovers on
	* @param datalinksInfo
	*/
   public void datalinkPopupShow(List<SimpleData> datalinksInfo) {
	   
		if (aladin.mesure.isEnabledDatalinkPopUp) {

			if (datalinksInfo != null && !datalinksInfo.isEmpty()) {
				aladin.makeCursor(mcanvas, Aladin.DEFAULTCURSOR);
				createAdditionalServiceMenu(datalinksInfo);
				additionalServiceMenu.show(this, this.datalinkshowX, this.datalinkshowY);
				//this.datalinkshowX = -1;
				//this.datalinkshowY = -1;
				aladin.mesure.isEnabledDatalinkPopUp = false;
			}
		}
      
   }
   
   /**
    * Method to show datalink pop-up
    * @param datalinksInfo
    */
   private void createAdditionalServiceMenu(List<SimpleData> datalinksInfo) {
	      additionalServiceMenu = new JPopupMenu();
	      additionalServiceMenu.setLightWeightPopupEnabled(false);
	      JMenuItem j;
	      
	      for (int i = 0; i < datalinksInfo.size(); i++) {
	    	SimpleData datalinkInfo = datalinksInfo.get(i);
			additionalServiceMenu.add(j = new JMenuItem(datalinkInfo.getDisplayString()), i);
			j.setActionCommand(String.valueOf(i));
			j.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent clickEvent) {
					clickEvent.getSource();
					int menuIndex = Integer.parseInt(clickEvent.getActionCommand());
					aladin.mesure.getFormInfo(menuIndex);
					
				}
			});
		}
		add(additionalServiceMenu);
	 }
   
   protected boolean isEnabledDatalinkPopUp;
   
   /**
    * Method to handle datasets which are linked to a datalink
    * Images,cutout,html/text,tables are handled.
    * @param menuIndex
    */
   public void getFormInfo(int menuIndex) {
		activeDataLinkGlu = this.activeDataLinkWord.datalinksInfo.get(menuIndex);
		try {
			String accessUrl = activeDataLinkGlu.getParams().get(ACCESSURL);
			
			if (activeDataLinkGlu != null) {
				Map<String,String> params = activeDataLinkGlu.getParams();
				if (params!=null) {
					String semantics = activeDataLinkGlu.getParams().get(SEMANTICS);
					String contentType = activeDataLinkGlu.getParams().get(CONTENTTYPE);
					
					if (semantics.equalsIgnoreCase(SEMANTIC_CUTOUT) || semantics.equalsIgnoreCase(SEMANTIC_ACCESS)
							|| semantics.equalsIgnoreCase(SEMANTIC_PROC)) {//TODO:: remove access semantic. added to facilitate testing.
						if (Aladin.BETA) {
							if (aladin.datalinkGlu == null) {
								aladin.datalinkGlu = new DataLinkGlu(aladin);
							}
							aladin.datalinkGlu.createDLGlu(this.datalinkManager.resultsResource, this.activeDataLinkSource, activeDataLinkGlu);
						} else {
							Aladin.warning(DatalinkManager.NOCUTOUTCLIENTSUPPORT, 1);
						}
					} /*else if (semantics.startsWith(SEMANTIC_PREVIEW) && accessUrl != null
							&& (contentType.equalsIgnoreCase(CONTENT_TYPE_JPEG)
							|| contentType.equalsIgnoreCase(CONTENT_TYPE_PNG))) { //for now we just load plot image or votable in Aladin
						aladin.glu.showDocument("Http", accessUrl, true);
					}*/ else if (contentType != null && accessUrl != null
							&& (contentType.equalsIgnoreCase(CONTENT_TYPE_TEXTHTML)
									|| contentType.equalsIgnoreCase(CONTENT_TYPE_TEXTPLAIN)
									|| contentType.equalsIgnoreCase(CONTENT_TYPE_PDF))) {
						aladin.glu.showDocument("Http", accessUrl, true);
					} else if (contentType!=null && accessUrl!=null && contentType.contains(DATATYPE_DATALINK)) {
						aladin.mesure.isEnabledDatalinkPopUp = true;
						aladin.makeCursor(mcanvas, Aladin.WAITCURSOR);
						this.activeDataLinkWord.callArchive(aladin, activeDataLinkSource, true);
					} else if (accessUrl!=null && !accessUrl.isEmpty()) {
						aladin.calque.newPlan(activeDataLinkGlu.getParams().get(ACCESSURL), null, null);//TODO::change to access
					} else {
						Aladin.warning("Error in loading datalink",1);
					}
				}
			} 
		} catch (Exception e) {
			Aladin.warning("Error in loading datalink",1);
		}
		
	}

   protected boolean isMesureOut() { return frameMesure!=null; }
   
   protected void split() {
      if( frameMesure==null ) frameMesure = new FrameMesure(aladin);
      else { frameMesure.close(); frameMesure=null; }
   }

   private boolean isSorting=false;
   synchronized protected void setSorting(boolean flag) { isSorting=flag; }
   synchronized protected boolean isSorting() { return isSorting; }

   public void run() {
      showStatus("...sorting...");
      setSorting(true);
      try {
         Util.pause(100);
         Arrays.sort(src,Source.getComparator());
      } catch( Exception e ) {}
      setSorting(false);
      scrollV.setValue(0);
      showStatus("");
      aladin.makeCursor(mcanvas,Aladin.DEFAULTCURSOR);
      memoWordLineClear();
      mcanvas.repaint();
   }

   /** Appelé par le popup menu */
   protected void tri(boolean ascendant) {
      mcanvas.tri(mcanvas.sCourante,mcanvas.sortField,ascendant);
   }

   /** Extrait un tableau de valeurs sous la forme de doubles
    * @param xCell Le tableau à remplir ou null s'il faut le régénérer
    * @param o l'objet qui sert d'étalon pour connaitre le type de Source
    * @param nField l'indice du champ
    * @return le tableau x ou sa regénération si x==null au préalable ou x.length modifié
    */
   synchronized protected double [] getFieldNumericValues(Source o,int nField) {
      // Décompte
      int nb=0;
      for( int i=0; i<nbSrc; i++ ) {
         if( src[i].leg!=o.leg ) continue;
         nb++;
      }

      double [] x = new double[nb];

      // Récupération des valeurs du champ indiqué
      for( int i=0,j=0; i<nbSrc; i++ ) {
         if( src[i].leg!=o.leg ) continue;
         x[j]=Double.NaN;
         String s = src[i].getValue(nField);
         int n = s.length();
         //          boolean neuf=true;
         //          if( n>3 ) for( int k=n-1; neuf && k>=0; k-- ) neuf=s.charAt(k)=='9';    // que des 9999 => ignoré
         if( n> 0 /* && !neuf */) {
            if( !Character.isDigit(s.charAt(n-1)) ) s=s.substring(0,n-1);  // Une unité accolée ?
            try { x[j] = Double.parseDouble(s); }
            catch( Exception e ) { x[j]=Double.NaN; }
         }
         j++;
      }

      return x;
   }

   /** Extrait un tableau de valeurs sous la forme de Chaine
    * @param xCell Le tableau à remplir ou null s'il faut le régénérer
    * @param o l'objet qui sert d'étalon pour connaitre le type de Source
    * @param nField l'indice du champ
    * @return le tableau x ou sa regénération si x==null au préalable ou x.length modifié
    */
   synchronized protected String [] getFieldStringValues(Source o,int nField) {
      // Décompte
      int nb=0;
      for( int i=0; i<nbSrc; i++ ) {
         if( src[i].leg!=o.leg ) continue;
         nb++;
      }

      String [] x = new String[nb];

      // Récupération des valeurs du champ indiqué
      for( int i=0,j=0; i<nbSrc; i++ ) {
         if( src[i].leg!=o.leg ) continue;
         x[j] = src[i].getValue(nField);
         j++;
      }

      return x;
   }

   protected void tri(Source o,int nField,boolean ascendant) {
      if( isSorting() ) return;
      aladin.makeCursor(mcanvas,Aladin.WAITCURSOR);
      Source.setSort(o,nField,ascendant ? 1:-1);
      if( src.length>75000) {
         Thread t = new Thread(this,"AladinSort");
         Util.decreasePriority(Thread.currentThread(), t);
         t.start();
      }
      else run();
   }

   /** Tague toutes les sources de la fenêtre des mesures */
   protected void tag() {
      for( int i=0; i<nbSrc; i++ ) src[i].setTag(true);
      mcanvas.repaint();
      aladin.view.repaintAll();
   }

   /** Untague toutes les sources de la fenêtre des mesures */
   protected void untag() {
      for( int i=0; i<nbSrc; i++ ) src[i].setTag(false);
      mcanvas.repaint();
      aladin.view.repaintAll();
   }

   /** Conserve dans la fenêtre des mesures les sources non taguées */
   protected void keepUntag() { keepTag1(false); }

   /** Conserve dans la fenêtre des mesures les sources taguées */
   protected void keepTag() { keepTag1(true); }

   /** Procédure interne pour keepTag et keepUntag
    * @param keep true je garde le objets tagués, sinon je les vire
    */
   private void keepTag1(boolean keep) {
      for( int i=nbSrc-1; i>=0; i-- ) {
         if( src[i].isTagged()!=keep ) { src[i].setSelect(false); rmSrc(i); }
      }
      scrollV.setMaximum(nbSrc);
      aladin.view.majSelect();
      mcanvas.repaint();
   }

   private int nOccurence;
   private String oMasq=null;

   static private String MFSEARCH,/*MFSEARCHINFO,*/
   MFSEARCHO,MFSEARCHBAD;

   /** Recherche d'une chaine.  */
   protected int searchString(String s,int mode) {
      int rep = search(s,mode==-1?-1:1);
      //       if( s.length()>0 && nbSrc>0 ) infoSearch(nOccurence);
      return rep;

      //System.out.println("J'ai cherché ["+s+"] mode="+mode+" trouve="+nOccurence);
   }

   /** Affichage des infos sur la dernière recherche dans le status aladin */
   private void infoSearch(int nOccurence) {
      if( nOccurence>=0) showStatus(MFSEARCH+" => "
            + nOccurence+" "+MFSEARCHO+(nOccurence>1?"s":"")/*+"        "
             + MFSEARCHINFO*/);
      else showStatus(MFSEARCHBAD);
   }

   //    String text;
   //    boolean flagBrowse;
   //
   //    synchronized private void setFlagBrowse(boolean flag) { flagBrowse=flag; }
   //
   //    /** Fait défiler les objets dans la liste des mesures */
   //    protected void browse() {
   //       if( flagBrowse ) return;
   //       text=aladin.mesure.getText();
   //       (new Thread("BrowseMesure"){
   //          public void run() {
   //             while( flagBrowse && text.equals(getText())) {
   //                aladin.mesure.searchString(text,1);
   //                Util.pause(1000);
   //             }
   //          }
   //       }).start();
   //    }


   /** Sélection des objets en fonction d'une chaine dans tous les objets présents
    * dans les catalogues (pas nécessairement sélectionnés)
    * @param s la chaine à chercher
    * @param clear true si on doit effacer la liste préalable
    * @return true si au moins une source trouvée
    */
   protected boolean selectByString(String s,int clear) {
      aladin.view.selectSrcByString(s,clear);
      infoSearch(nbSrc);
      oMasq="";
      return nbSrc>0;
   }

   Source lastOcc=null;

   /** Recherche et sélection de la prochaine mesure qui match la chaine "masq"
    * @param masq sous-chaine à rechercher
    * @param fromField numéro de champ courant (lié à la dernière recherche)
    * @param sens -1 en arrière, 0 première recherche, 1 en avant
    * @return 0-pas trouvé, 1- trouvé et montré, -1 trouvé mais hors images
    */
   protected int search(String masq,int sens) {
      int n=-1;
      Source t=null;
      int nMasq = masq.length();
      boolean flagSame = nMasq==0 || masq.equals(oMasq);
      if( !flagSame ) nOccurence=0;
      oMasq=masq;


      if( mcanvas.objSelect!=null ) lastOcc=mcanvas.objSelect;
      //       if( mcanvas.currentsee==-1 || lastOcc==null || mcanvas.objSelect==null ) n=mcanvas.currentsee;
      //       else {
      for( int i=0; i<nbSrc; i++ ) {
         if( src[i]==lastOcc ) { n = i; break; }
      }
      //       }

      n+=sens;
      if( n==-1 ) n=sens<0 ? nbSrc-1: 0;

      //System.out.println("Je cherche ["+masq+"] à partir de "+n+" nOccurence="+nOccurence+" flagSame="+flagSame);

      // Analyse de la recherche (ex: FLU*>10, OTYPE=X, Star, ...)
      StringBuffer col = new StringBuffer();    // pour récupérer un éventuel nom de colonne
      StringBuffer v = new StringBuffer();      // pour récupérer la valeur à chercher
      int mode = aladin.view.getAdvancedSearch(col,v,masq);    // type de recherche voir View.EGAL...
      masq = v.toString();
      boolean abs=false;                        // true si on travaille en valeur absolue
      if( col.length()>0 ) abs = aladin.view.getAbsSearch(col);

      int colIndex = -1;             // Index de la colonne dans le cas où un nom de champ aurait été spécifié
      double numS=Double.MAX_VALUE;  // Valeur numérique de la valeur à chercher si mode numérique
      boolean numeric=false;         // mode de recherche littéral ou numérique
      Legende oLeg=null;             // Légende de la source précédente

      fini:
         for( int i=0; i<nbSrc; i++,n+=sens) {
            if( n>=nbSrc ) n=0;
            else if( n<0 ) n=nbSrc-1;

            Source s = src[n];
            if( s==null ) continue;
            lastOcc=s;

            // Y a-t-il un nom de colonne précisée ? dans le cas où je change de légende
            if( col.length()>0 && oLeg!=s.leg) {
               colIndex = s.leg.matchIgnoreCaseColIndex(col.toString());
               if( colIndex==-1 ) break;  // Pas dans ce plan
               numeric = s.leg.isNumField(colIndex);
               if( numeric ) {
                  try { numS = Double.parseDouble(masq); }
                  catch(Exception e) {}
               }
               //System.out.println(s.plan.label+" mode="+mode+" col="+col+" val="+v+" colIndex="+colIndex+" dataType="+dataType+" numeric="+numeric+" numS="+numS);
               oLeg=s.leg;
            }

            String val[] = s.getValues();

            // Un nom de colonne précisée ?
            if( colIndex>=0 ) {
               if( aladin.view.advancedSearch(mode,numeric,abs,val[colIndex],masq,numS) ) {
                  if( t==null ) t=s;
                  if( flagSame ) break fini;  // C'est la même requête, inutile de faire le tour
                  nOccurence++;
               }
            }

            // Sinon on cherche dans toutes les colonnes
            else for( int j=0; j<val.length; j++ ) {
               if( nMasq==0 || Util.indexOfIgnoreCase(val[j],masq)>=0 ) {
                  if( t==null ) t=s;
                  if( flagSame ) break fini;   // C'est la même requête, inutile de faire le tour
                  nOccurence++;
                  break;
               }
            }
         }

      int rep=0;
      if( t!=null ) {
         mcanvas.show(t,2);
         rep = aladin.view.setRepere(new Coord(t.raj,t.dej)) ? 1 : -1;
         if( !Aladin.NOGUI ) aladin.view.showSource(t);
         //          aladin.view.memoUndo(aladin.view.getCurrentView(), new Coord(t.raj,t.dej),t);
         //System.out.println("J'ai trouve en pos = "+n+" field="+field);
      }

      return rep;
   }

   /** réaffichage les mesures. Utilisé dans le cas d'une modif d'attributs de la table
    * via Legende.setValueAt() */
   protected void redisplay() {
      mcanvas.reloadHead();
      mcanvas.repaint();
   }

   /** Ajout d'une source (tableau dynamique) */
   synchronized private void addSrc(Source s) {
      if( frameMesure==null ) setReduced(false);
      if( nbSrc==src.length ) {
         Source srcBis[] = new Source[ src.length>MAXBLOC?src.length+MAXBLOC:src.length*2];
         System.arraycopy(src,0,srcBis,0,src.length);
         src=srcBis;
         srcBis=null;
      }
      src[nbSrc++]=s;

      scrollV.setMaximum(nbSrc);
      //      aladin.trace(4,"Mesure.scrollV("+s.id+") nbSrc="+nbSrc);

      mcanvas.unselect();
      aladin.calque.zoom.zoomView.stopHist();
      aladin.calque.zoom.zoomView.resumeSED();
      aladin.console.setEnabledDumpButton(true);
      if( s.leg!=null && s.leg.isSorted() ) { s.leg.clearSort(); mcanvas.reloadHead(); }
      if( mcanvas.triTag!=Field.UNSORT ) mcanvas.triTag=Field.UNSORT;
   }

   /** Suppression de toutes les sources */
   synchronized private void rmAllSrc() {
      if( nbSrc>MAXBLOC ) src = new Source[DEFAULTBLOC];
      else for( int i=0; i<nbSrc; i++ ) src[i]=null;	// Pour le GC
      nbSrc=0;
      mcanvas.unselect();
      aladin.calque.zoom.zoomView.stopHist();
      aladin.calque.zoom.zoomView.resumeSED();
      aladin.console.setEnabledDumpButton(false);
   }

   /** Suppresssion de toutes les sources d'un plan particulier */
   protected void rmPlanSrc(Plan p) {
      synchronized ( this ) {
         int n=0,i;
         for( i=0; i<nbSrc; i++ ) {
            if( src[i].plan==p ) continue;
            if( i!=n ) src[n]=src[i];
            n++;
         }
         nbSrc=n;
      }

      mcanvas.currentsee=-1;
      mcanvas.currentselect=-2;
      scrollV.setMaximum(nbSrc);
      mcanvas.unselect();
      mcanvas.repaint();
      aladin.calque.zoom.zoomView.stopHist();
      aladin.calque.zoom.zoomView.resumeSED();
      aladin.console.setEnabledDumpButton(nbSrc>0);
   }

   /** Suppression d'une source particulière - PAS UTILISE */
   //   synchronized private void rmSrc(Source s) {
   //      int i=findSrc(s);
   //      if( i!=-1 ) rmSrc(i);
   //   }

   /** Retourne une copie de la liste des sources sélectionnées */
   protected Source [] getSources() {
      synchronized( this ) {
         Source [] s = new Source[nbSrc];
         System.arraycopy(src, 0, s, 0, nbSrc);
         return s;
      }
   }

   /** Suppresssion d'une liste de sources repéré par leurs indices (ordonnés) */
   protected void rmSrc(ArrayList list) {
      synchronized ( this ) {
         int n=0,i,m=0;
         int index = m<list.size() ? ((Integer)list.get(m)).intValue() : -1;
         for( i=0; i<nbSrc; i++ ) {
            if( i==index ) {
               m++;
               index = m<list.size() ? ((Integer)list.get(m)).intValue() : -1;
               continue;
            }
            if( i!=n ) src[n]=src[i];
            n++;
         }
         nbSrc=n;
      }
      aladin.calque.zoom.zoomView.stopHist();
      aladin.calque.zoom.zoomView.resumeSED();
      aladin.console.setEnabledDumpButton(nbSrc>0);
   }


   /** Suppression de la source d'indice i */
   synchronized protected void rmSrc(int i) {
      for( ; i<src.length-1; i++ ) src[i]=src[i+1];
      src[i]=null;
      nbSrc--;
      mcanvas.unselect();
      aladin.calque.zoom.zoomView.stopHist();
      aladin.calque.zoom.zoomView.resumeSED();
      aladin.console.setEnabledDumpButton(nbSrc>0);
   }

   /** Repérage de l'indice de la source s, -1 si non trouvé  */
   protected int findSrc(Source s) {
      for( int i=0; i<nbSrc; i++) if( src[i]==s ) return i;
      return -1;
   }

   public Iterator<Source> iterator() { return new SourceIterator(); }
   class SourceIterator implements Iterator<Source> {
      int i=nbSrc;
      public boolean hasNext() { return i>0; }
      public Source next() { return src[--i]; }
      public void remove() { }
   }

   // Retourne la première source du tableau
   protected Source getFirstSrc() { return nbSrc<1 ? null : src[0]; }

   private Object verrou = new Object();

   /** Retourne le nombre de source actuellement gérées */
   protected int getNbSrc() { synchronized( verrou ) { return nbSrc; } }

   /** Reset de la mémorisation des WordLines tracées dans le MCanvas */
   protected void memoWordLineClear() {
      synchronized( verrou ) {
         memoWL.clear();
      }
   }

   /** Mémorisation d'une wordLine (uniquement appelé par MCanvas.update()),
    * La clé est l'indice dans le tableau src[] */
   protected void memoWordLine(Vector wl,int i) {
      synchronized( verrou ) {
         memoWL.put(new Integer(i),wl);
      }
   }

   /** Récupération (si mémorisé dans memoWL) ou création de la WordLine
    * associée à la source d'indice i dans src[] */
   protected Vector getWordLine(int i) {
      synchronized( verrou ) {
         Vector wl = (Vector)memoWL.get(new Integer(i));
         if( wl!=null ) return wl;
         return getWordLine(src[i],i);
      }
   }

   /** Génération de la HeadLine associée à la source passée en paramètre */
   protected Vector getHeadLine(Source o) {
      Vector wordLine;
      Legende leg = o.leg;
      wordLine = new Vector(leg.field.length+2);

      wordLine.addElement(o);           // L'objet lui-meme est tjrs en premiere place
      wordLine.addElement(new Words("",-1)); // A la place du repère

      for( int i=0; i<leg.field.length; i++ )  {
         int nField = leg.fieldAt[i];
         if( !leg.isVisible(nField) ) continue;
         Words w = new Words(leg.field[nField].name,null,o.leg.getWidth(nField),o.leg.getPrecision(nField),
               Words.CENTER,o.leg.computed.length==0?false:o.leg.computed[nField],
                     leg.field[nField].sort,-1,false);
         w.pin = i==0;
         wordLine.addElement(w);
      }
      return wordLine;
   }

   // Liste des liens qui ont déjà été cliqué (on mémorise le hashcode de l'obj et l'index
   // de la colonne concernée
   private HashSet<String> haspushedSet = new HashSet<String>();
   protected void setHaspushed(Obj o, int numField) {
      String key = o.hashCode()+"/"+numField;
      haspushedSet.add( key );
   }
   private boolean hasBeenPushed(Obj o, int numField) {
      String key = o.hashCode()+"/"+numField;
      return haspushedSet.contains( key );
   }

   /** Génération de la WordLine associée à la source passée en paramètre */
   protected Vector getWordLine(Source o,int num) {
      if( o==null ) return null;
      
      Vector wordLine;
      String s =(o.info!=null)?o.info:o.id; // Faute de grive...

      StringTokenizer st = new StringTokenizer(s,"\t");
      int n = st.countTokens();
      wordLine = new Vector(n+1);
      wordLine.addElement(o);           // L'objet lui-meme est tjrs en premiere place

      int indexFootPrint = o.getIdxFootprint(); // position d'un Fov, -1 si aucun

      boolean isDatalink= isValueOfSpecifiedUcdField(o, ACCESSFORMAT_UCD, DATATYPE_DATALINK);
      
      String [] tags = new String[ n-1 ];
      String triangle = st.nextToken();  
      for (int i = 0; i<tags.length; i++) tags[i] = st.nextToken();

      for (int i = 0; i<n; i++) {
         String tag;

         Words w;
         if( i==0 ) {
            tag=triangle;
            w = new Words(tag,num);	// Le triangle n'a pas de taille
         } else {
            int nField=0;
            try {
               nField = o.leg.fieldAt[i-1];
            } catch( Exception e ) {
               // Y a un problème
               System.out.println("Y a un prob. =>"+s);
               continue;
            }
            tag = tags[nField];

            if( !o.leg.isVisible(nField) ) continue;

            // Determination de l'alignement en fonction du type de donnees
            int align= o.leg.isNumField(nField) ? Words.RIGHT : Words.LEFT;

            // Creation d'un mot dans le cas d'un footprint associé (Thomas, VOTech)
            if( indexFootPrint==nField ) {
               w = new Words("  FoV",o.leg.getWidth(i-1),o.leg.getPrecision(nField),Words.LEFT,
                     false,true,num);
            }
            // Creation du nouveau mot
            else {
               if( o.leg.isNullValue(tag, i-1) ) tag="";
               w = new Words(tag,o.leg.getRefText(nField),o.leg.getWidth(nField),
                     o.leg.getPrecision(nField),align,
                     o.leg.computed.length==0?false:o.leg.computed[nField],Field.UNSORT,num,isDatalink);
            }
         }
         w.show= (o==mcanvas.objSelect || o==mcanvas.objShow );
         w.haspushed = hasBeenPushed(o, i);
         wordLine.addElement(w);
         if( w.glu && w.size<tag.length() ) {
            tag = tag.substring(w.size+1,tag.length());
            wordLine.addElement(new Words(tag,num));
         }
      }
      return wordLine;
   }
   
   
	/**
	 * Method to check the value of a specified column. 
	 * Column specification is with ucd
	 * 
	 * @param source
	 * @param ucd
	 * @param expectedValue
	 * @return result
	 */
	public static boolean isValueOfSpecifiedUcdField(Source source, String ucd, String expectedValue) {
		boolean result = false;
		int formatIndex = source.findUtype("obscore:Access.Format");
		if (formatIndex!=-1) {
			String value = source.getValue(formatIndex);
			if (value.contains(expectedValue)) {
				result = true;
			}
			/*if (expectedValue.equalsIgnoreCase(value)) {
				result = true;
			}*/
		}
		return result;
	}
	
	/** retourne true si les mesures sont dans une fenêtre indépendantes */
	protected boolean isSplitted() { return flagSplit; }

   /** Ajustement du panel pour une visualisation dans une fenetre independante */
   protected void split(boolean flagSplit) {
      if( flagSplit==this.flagSplit ) return;
      this.flagSplit=flagSplit;
      if( flagSplit ) {
         previousHeight = mcanvas.getSize().height;
         haut.setVisible(true);
         status.setSize(status.getSize().width,status.H);
         mcanvas.setSize(mcanvas.getSize().width, 600);
         scrollV.setSize(scrollV.getSize().width, 600);
      } else {
         haut.setVisible(false);
         mcanvas.setSize(mcanvas.getSize().width, previousHeight);
         scrollV.setSize(scrollV.getSize().width, previousHeight);
//         if( !flagReduced ) aladin.splitMesureHeight.restoreSlit();
      }
      setSize( getPreferredSize());
   }

//   /** retourne l'état courant de la fenêtre des mesures (réduite ou agrandie) */
//   protected boolean isReduced() { return flagReduced; }

//   /** permute l'état réduit/agrandit de la fenêtre des mesures */
//   protected void switchReduced() {
//      if( f!=null ) split();
//      setReduced(!flagReduced);
//   }

//   protected boolean flagReduced=true;
   protected boolean flagDorepaintForScroll=false;
   
   protected void setReduced(boolean flag) {
      aladin.splitMesureHeight.setReduced( flag );
   }


//   protected void setReduced(boolean flag) {
//      if( flagReduced==flag ) return;
//      flagReduced=flag;
//      if( flagReduced ) {
//         aladin.search.hideSearch(true);
//         if( aladin.splitMesureHeight.getBottomComponent()!=null ) aladin.splitMesureHeight.remove(this);
//      } else {
//         aladin.search.hideSearch(false);
//         if( aladin.splitMesureHeight.getBottomComponent()==null ) aladin.splitMesureHeight.setBottomComponent(this);
//         flagDorepaintForScroll=true;
//         aladin.splitMesureHeight.restoreSlit();
//      }
//   }

   /**
    * Update the status string
    * @param text
    */
   protected void showStatus(String text) {
      if( flagSplit ) status.setText(text);
      else aladin.status.setText(text);
   }
   
   private Plan oldPlanHighlighted=null;
   
   /** Affichage d'une info au dessous de la pile */
   protected void showInfo(String s, Plan p) {
      if( s==null || s.length()==0 ) aladin.calque.select.hideMessage();
      else aladin.calque.select.setMessageInfo( s );

      if( oldPlanHighlighted!=p ) aladin.calque.select.repaint();
      if( oldPlanHighlighted!=null ) oldPlanHighlighted.isHighlighted=false;
      if( p!=null ) p.isHighlighted=true;
      oldPlanHighlighted = p;
   }

   /** Ajout des mesures d'une source.
    * Analyse et ajout des infos/mesures associees a la source SANS reaffichage
    * @param o La source pour laquelle il faut afficher les mesures
    */
   protected void insertInfo(Source o) {
      addSrc(o);
      mcanvas.currentselect=-2;
   }

   /** Retourne la valeur courante sous la souris */
   protected String getCurObjVal() {
      if( mcanvas.sCourante==null || mcanvas.indiceCourant==-1 )return "";
      return mcanvas.sCourante.getValue(mcanvas.indiceCourant);
   }

   /** Retourne l'URL courante sous la souris */
   protected String getCurObjURL() {
      if( mcanvas.sCourante==null || mcanvas.indiceCourant==-1 )return "";
      String tag = mcanvas.sCourante.getCodedValue(mcanvas.indiceCourant);
      Words w = new Words(tag,-1);
      return w.getURL(aladin);
   }

   /**  Retourne les coordonnées de l'objet sous la souris */
   protected String getCurObjCoord() {
      Source s = mcanvas.objSelect;
      if( s==null ) return "";
      return aladin.localisation.J2000ToString(s.raj, s.dej);
   }

   protected String getText() {
      return getText(false);
   }

   //
   /** Retourne les mesures sous forme de texte.
    *  Utilise par le Pad, et par les popupCopy...
    *
    * @param ascii si true, on utilisera des blancs comme séparateurs, sinon une tabulation
    *
    */
   protected String getText(boolean ascii) {
      int i;
      StringBuffer res = new StringBuffer();
      String sep = ascii?"    ":"\t";

      for( i=0; i<nbSrc; i++ ) {
         Vector v = getWordLine(i);

         Enumeration e = v.elements();
         e.nextElement();                  // Je saute l'objet lui-meme
         int k=-1;
         while( e.hasMoreElements() ) {
            Words w = (Words) e.nextElement();   // Les mots

            if( ascii && k>=0 ) {
               int length;
               if( w.text.length()>w.width ) length = 1;
               else length = w.width-w.text.length()+1;

               sep = Util.fillWithBlank("", length);
            }

            if( w.repere ) {
               int deb=w.text.indexOf('"');
               int fin=w.text.lastIndexOf('"');
               if( ascii ) sep = "    ";
               if( deb>=0 && fin>deb ) res.append( w.text.substring(deb+1,fin)+":"+sep);
            } else {
               res.append(w.text);
               if( e.hasMoreElements() ) res.append(sep);
            }
            k++;
         }
         res.append(Util.CR);
      }
      return res.toString();
   }

   // retourne sous forme de chaine la ligne des mesures pour l'objet couramment sélectionné (en TSV ou en JSON)
   protected String getCurObjMeasurement(boolean json) {
      StringBuilder sb = new StringBuilder();
      Source s = mcanvas.objSelect;
      
      Legende leg = s.leg;
      String [] values = s.getValues();
      boolean first=true;
      
      if( json ) sb.append("{ ");
      
      for( int i=0; i<leg.field.length; i++ ) {
         if( !leg.field[i].visible ) continue;
         if( json ) {
            if( !first ) sb.append(", ");
            sb.append( "\""+Util.escapeJSON(leg.field[i].name)+"\": \""+Util.escapeJSON(values[i])+"\"");
            
         } else {
            if( !first ) sb.append('\t');
            sb.append(values[i]);            
         }
         first=false;
      }
      
      if( json ) sb.append(" }");

      
      
//      if( s==null ) return "";
//
//      Vector v = getWordLine(s);
//      
//      Enumeration e = v.elements();
//      e.nextElement();                  // Je saute l'objet lui-meme
//      
//      if( json ) sb.append("{ ");
//
//      while( e.hasMoreElements() ) {
//         Words w = (Words) e.nextElement();   // Les mots
//         if( w.repere ) {
//            int deb=w.text.indexOf('"');
//            int fin=w.text.lastIndexOf('"');
//            if( deb>=0 && fin>deb ) sb.append( w.text.substring(deb+1,fin)+":"+"\t");
//         }
//         else {
//            sb.append(w.text);
//            if( e.hasMoreElements() ) sb.append("\t");
//         }
//      }

      return sb.toString();
   }

   /** Vidange.
    * Enleve toutes les mesures.
    */
   protected void removeAllElements() {
      mcanvas.currentselect=-2;
      mcanvas.oleg=null;
      rmAllSrc();
      scrollV.setValues(0,1,0,1);
   }

   /** Suppression de la ligne d'une source particulière
    */
   protected void remove(Source s) {
      boolean dopaint = false;
      // Le lock sur text est necessaire en raison du thread qui calcule les filtres --> risque de pb sans ce lock
      synchronized(this) {
         for( int i=0; i<nbSrc; i++ ) {
            Source o = src[i];
            if( o==s ) {
               dopaint=true;
               rmSrc(i);
               scrollV.setMaximum(nbSrc);
               break;
            }
         }
      }
      if( dopaint ) display();
   }

   /** Réaffiche les mesures */
   protected void display() {
      mcanvas.currentsee=-1;
      mcanvas.currentselect=-2;

      Source s = aladin.mesure.getFirstSrc();
      if( s==null && aladin.view.zoomview.flagSED || s!=null && s.leg!=null && s.leg.isSED() ) {
         aladin.view.zoomview.setSED(s);
      }
      mcanvas.repaint();
   }

   /** Insertion des infos d'une source AVEC reaffichage
    * @param o La source pour laquelle il faut afficher les mesures
    */
   protected void setInfo(Source o) {
      insertInfo(o);
      adjustScroll();
   }

   protected void adjustScroll() {
      mcanvas.initX();
      int nl = mcanvas.nbligne;
      int val =nbSrc-nl;
      if( val<0 ) val=0;
      int extend = nl;
      scrollV.setValues(val, extend, 0,nbSrc-1);
      scrollV.setBlockIncrement(nl-1);
   }

   private WidgetControl voc=null;

   @Override
   public WidgetControl getWidgetControl() { return voc; }

   @Override
   public void createWidgetControl(int x, int y, int width, int height, float opacity,JComponent parent) {
      voc = new WidgetControl(this,x,y,width,height,opacity,parent);
      voc.setResizable(true);
   }

   @Override
   public void paintCollapsed(Graphics g) { }



}
