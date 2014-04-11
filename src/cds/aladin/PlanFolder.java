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

import cds.tools.Util;


/**
 * Plan dédié à la manipulation d'un folder
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : mars 2006 - création
 */
public class PlanFolder extends Plan {
   
   protected boolean localScope;	// true si les overlays ne se font qu'à l'intérieur
                                    // du folder

   /** Creation d'un plan de type FOLDER dédié à du Fits Extension
    * @param label le nom du plan (dans la pile des plans)
    */
    protected PlanFolder(Aladin aladin, String label) {
       this.aladin=aladin;
       type=FOLDER;
       folder=0;
       flagOk=false;
       setLabel(label);
       c=Color.black;
       askActive=true;
       localScope=false;
       headerFits=null;
    }
    

   /** Creation d'un plan de type FOLDER
   * @param label le nom du plan (dans la pile des plans)
   * @param folderNiv le niveau du folder.
   * @param localScope true si le scope est restreint
   */
   protected PlanFolder(Aladin aladin, String label,int folderNiv,boolean localScope) {
      this.aladin=aladin;
      type=FOLDER;
      folder=folderNiv;
      flagOk=true;
      if( label==null ) label="Fold";
      setLabel(label);
      c=Color.black;
      askActive=true;
      headerFits=null;
      this.localScope=localScope;
   }
   
   /** Creation d'un plan de type Folder (sans info)
    */
   protected PlanFolder(Aladin aladin) {
     this.aladin = aladin;
     type = FOLDER;
     flagOk=true;
   }
   
   /** Modifie (si possible) une propriété du plan (dépend du type de plan) */
   protected void setPropertie(String prop,String specif,String value) throws Exception {
      if( prop.equalsIgnoreCase("LocalScope") ) { 
         localScope= value.equalsIgnoreCase("true");
      } else if( prop.equalsIgnoreCase("scope") ) {
         localScope= value.equalsIgnoreCase("local");
      }
   }
   
   /** Retourne sous forme de paragraphe dédié à la commande "status" la liste des plans
    * contenus dans le folder (sans récursivité) ou null si aucun */
   protected String getStatusItems() {
      StringBuffer rep=null;
      Plan p[] = aladin.calque.getFolderPlan((Plan)this,false);
      for( int i=0; i<p.length; i++ ) {
         if( rep==null ) rep = new StringBuffer();
         rep.append("Item    "+Util.align("["+Plan.Tp[p[i].type]+"]",12)+p[i].label+"\n");
      }
      return rep==null ? null : rep.toString();
   }
   
   /** Retourne le target utilisé pour la requête du plan qui suit le folder */
   protected String getTargetQuery() {
      try {
         synchronized( aladin.calque ) {
            int n= aladin.calque.getIndex(this);      
            return aladin.calque.plan[n+1].getTargetQuery();
         }
      } catch( Exception e ) {}
      return "";
   }

   
   /** Dans le cas d'un folder, le pourcent sert à indiquer le nombre de plans en cours
    * de chargement dans le folder (Fits extension)
    */
   protected String getLabel() {
      int p = (int)getPourcent();
      if( p>0 ) return Util.align(label,7,"..")+p;
      return super.getLabel();
      
   }
   protected boolean Free() {
      super.Free();
      localScope=false;
      headerFits=null;
      return true;
   }
   
   protected boolean isSync() { return flagOk; }
   
   /** Positionne le niveau d'opacité [0..1] (0: entièrement transparent, 1: entièrement opaque) */
   public void setOpacityLevel(float opacityLevel) {
      super.setOpacityLevel(opacityLevel);
      Plan [] list = aladin.calque.getFolderPlan(this,false);
      for( Plan p : list ) {
         if( aladin.calque.canBeTransparent(p) ) p.setOpacityLevel(opacityLevel);
      }
   }

}   
