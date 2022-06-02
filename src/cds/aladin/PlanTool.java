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

import java.util.Enumeration;
import java.util.Iterator;

import cds.tools.VOApp;

/**
 * Plan dedie a des objets graphiques (TOOL)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (nov 2010) Ajout des sources photométriques
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class PlanTool extends PlanCatalog {

//   protected Legende legPhot    = null;
//   protected Legende legTag     = null;
//   protected Legende legPhotMan = null;

   /** Creation d'un plan de type TOOL
    * @param label le nom du plan (dans la pile des plans)
    */
   protected PlanTool(Aladin aladin, String label) {
      setLogMode(true);
      this.aladin= aladin;
      type       = TOOL;
      c          = Couleur.getNextDefault(aladin.calque);
      if( label==null ) label="Drawing";
      setLabel(label);
      pcat       = new Pcat(this,c,aladin.calque,aladin.status,aladin);
      flagOk     = true;
      askActive=true;
      aladin.calque.selectPlan(this);

      // S'il n'y a pas de plan de reference, on bloque les xy dans l'ecran
      Plan pref = aladin.calque.getPlanRef();
      if( pref==null || !Projection.isOk(pref.projd) ) {
         projd=new Projection();
         hasXYorig=true;
      } else hasXYorig=false;
   }

   /** Creation d'un plan de type TOOL (pour un backup) */
   protected PlanTool(Aladin aladin) {
      setLogMode(true);
      this.aladin= aladin;
      type       = TOOL;
      c          = Couleur.getNextDefault(aladin.calque);
      pcat       = new Pcat(this,c,aladin.calque,aladin.status,aladin);
      flagOk     = true;
      askActive  = true;
   }
   
   protected boolean Free() {

      // Pour suspendre éventuellement l'affichage des histogrammes
      // associé à un objet contenu dans le plan
      if( aladin.view.zoomview.flagCut || aladin.view.zoomview.flagHist ) {
         Iterator<Obj> it = iterator();
         while( it.hasNext() ) {
            Obj o = it.next();
            if( o.isSelected() ) o.remove();
         }
      }
      return super.Free();
   }
   
   /** Ajoute des infos sur le plan */
   protected void addMessageInfo( StringBuilder buf, MyProperties prop ) {
      String s;
      int n;
      if( (n=getCounts())>0 ) {
         ADD( buf, "\n* Objects: ",String.format("%,d", n));
      }
   }
   
   public SourceTag addTag(ViewSimple v,double ra, double dec) {
      SourceTag o = new SourceTag(this, v, new Coord(ra,dec),null);
      pcat.insertSource(o);
      aladin.view.newView(1);
      setSourceRemovable(true);
      return o;
   }

   public SourcePhot addPhot(ViewSimple v,double ra, double dec, double []iqe) {
      SourcePhot o = new SourcePhot(this,v,new Coord(ra,dec), iqe);
      pcat.insertSource(o);
      aladin.view.newView(1);
      setSourceRemovable(true);
      return o;
   }
     
      
   /** Retourne la ligne d'informations concernant le plan dans le statut d'Aladin*/
   protected String getInfo() {

      if( type==NO ) return super.getInfo();
      return label+super.addDebugInfo();
   }

   /** Modifie (si possible) une propriété du plan */
   protected void setPropertie(String prop,String specif,String value) throws Exception {
      if( prop.equalsIgnoreCase("movable") ) {
         setMovable(value);
      } else super.setPropertie(prop,specif,value);
   }

   protected boolean movable=true; // True si les objets du plan peuvent être déplacés

   protected boolean isMovable() { return movable; }
   
   protected boolean isCatalog() {
      if( hasTag() ) return true;
      return false;
   }
   
   /** Retourne vrai si le plan tool contient au moins un objet SourceTag */
   protected boolean hasTag() {
      if( pcat==null ) return false;
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( o instanceof SourceTag ) return true;
      }
      return false;
   }

//   /** Retourne vrai si le plan tool contient au moins un objet SourcePhot */
//   protected boolean hasPhot() {
//      if( pcat==null ) return false;
//      Iterator<Obj> it = iterator();
//      while( it.hasNext() ) {
//         Obj o = it.next();
//         if( o instanceof SourceStat ) return true;
//      }
//      return false;
//   }

   protected void setMovable(String v) throws Exception {
      if( v.equalsIgnoreCase("On") ) movable=true;
      else if( v.equalsIgnoreCase("Off") ) movable=false;
      else throw new Exception("Syntax error => movable=on|off");
   }

   // Regeneration des libelles des reperes
   protected void setIdAgain() {
      if( pcat==null ) return;
      Iterator<Obj> it = iterator();
      while( it.hasNext() ) {
         Obj o = it.next();
         if( o instanceof Repere ) ((Repere)o).setId();
      }
   }

   /** Envoi d'une commande aux observers de mesures pour indiquer un changement d'état d'un objet de mesure (cercle ou polygone) */
   protected void sendMesureObserver(Obj o,boolean cont) {
      if( aladin.VOObsMes==null || aladin.VOObsMes.size()==0 ) return;
      int objIndex = pcat.getIndex(o);
      String s="set "+Tok.quote(label+"/"+objIndex)+" measure="+o.id+(cont?" ...":"");
      Aladin.trace(4,"sendMesureObserver(): ["+s+"]");
      Enumeration e = aladin.VOObsMes.elements();
      while( e.hasMoreElements() ) {
         try { ((VOApp)e.nextElement()).execCommand(s); }
         catch( Exception e1 ) { if( aladin.levelTrace>=3 ) e1.printStackTrace(); }
      }
   }


}

