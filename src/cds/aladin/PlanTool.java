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
import java.util.Iterator;

import cds.tools.VOApp;
import cds.tools.VOObserver;


/**
 * Plan dedie a des objets graphiques (TOOL)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (nov 2010) Ajout des sources photom�triques
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public class PlanTool extends PlanCatalog {
   
   protected Legende legPhot = null;
   
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

   private void createPhotLegende() {
      setSourceRemovable(true);
      legPhot = Legende.adjustDefaultLegende(legPhot,Legende.NAME,     new String[]{  "ID",  "RA (ICRS)","DE (ICRS)","X",     "Y",      "FWHM_X", "FWHM_Y", "Angle",  "Peak",  "Background" });
      legPhot = Legende.adjustDefaultLegende(legPhot,Legende.DATATYPE, new String[]{  "char","char",     "char",     "double","double", "double", "double", "double", "double","double" });
      legPhot = Legende.adjustDefaultLegende(legPhot,Legende.UNIT,     new String[]{  "char","\"h:m:s\"","\"h:m:s\"","",      "",       "",       "",       "deg",    "",      "" });
      legPhot = Legende.adjustDefaultLegende(legPhot,Legende.WIDTH,    new String[]{  "15",   "13",      "13",       "8",    "8",      "10",     "10",      "5",      "10",    "10"   });
      legPhot = Legende.adjustDefaultLegende(legPhot,Legende.PRECISION,new String[]{  "",     "2",        "3",       "2",    "2",      "2",      "2",       "0",      "3",     "3"   });
      legPhot = Legende.adjustDefaultLegende(legPhot,Legende.DESCRIPTION,     
            new String[]{  "Identifier",  "Right ascension",  "Declination",  
                           "X image coordinate",     "Y image coordinate",      
                           "X Full Width at Half Maximum", "Y Full Width at Half Maximum",
                           "Angle",  "Source peak",  "image background" });
      legPhot = Legende.adjustDefaultLegende(legPhot,Legende.UCD,      
            new String[]{  "meta.id;meta.main","pos.eq.ra;meta.main","pos.eq.dec;meta.main",     
                           "pos.cartesian.x;obs.field","pos.cartesian.y;obs.field", 
                           "", "", 
                           "pos.posAng;obs.field", "","instr.background;obs.field" });
 
      addFilter("#Object elongation\nfilter obj_elong { draw ellipse(${FWHM_X}/2,${FWHM_Y}/2,270-${Angle}) }");
      setFilter("obj_elong");

   }
   
   /** retourne true si le plan a des sources */
   protected boolean withSource() { return legPhot!=null; }
   
   public Source addPhot(PlanImage planBase,double ra, double dec, double []iqe) {
      if( legPhot==null ) createPhotLegende();
      
      String id = pcat.getNextID()+"/"+planBase.label;
      Coord c = new Coord(ra,dec);
      String [] val = { id, c.getRA(), c.getDE()+"", iqe[0]+"", iqe[2]+"",iqe[1]+"",iqe[3]+"",iqe[4]+"",iqe[5]+"",iqe[6]+"" };
      Source o1 = addSource(id, ra, dec, val);
      o1.setShape(Obj.PLUS);
      aladin.view.newView(1);
      return o1;
   }
   
   private Source addSource(String id,double ra, double dec, String [] value) {
      StringBuffer s = new StringBuffer("<&_A>");
      for( int i=0; i<value.length; i++ ) {
         if( value[i].startsWith("http://") || value[i].startsWith("https://") ) s.append("\t<&Http "+value[i]+">");
         else s.append("\t"+value[i]);
      }
      Source o = new Source(this,ra,dec,id,s.toString());
      o.leg = legPhot;
      pcat.setObjetFast(o);
      return o;
   }

  /** Retourne la ligne d'informations concernant le plan dans le statut d'Aladin*/
   protected String getInfo() {

      if( type==NO ) return super.getInfo();
      return label+super.addDebugInfo();
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
   
   /** Envoi d'une commande aux observers de mesures pour indiquer un changement d'�tat d'un objet de mesure (cercle ou polygone) */
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

