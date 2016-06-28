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

/**
 * Objet graphique representant une mesure photométrique extraite automatiquement
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 (juin 2016): Refonte complète depuis de code dispersé à droite, à gauche
 */
public class SourcePhot extends SourceTag {
   
   
   static private final String FILTERNAME = "obj_elong";
   static private final String FILTER     = "#Object elongation\nfilter "+FILTERNAME
                                           +" { draw ellipse(${FWHM_X}/2,${FWHM_Y}/2,270-${Angle}) }";

   static protected Legende legende=createLegende();
   
   /** Création ou maj d'une légende associée à un SourcePhot */
   static protected Legende createLegende() {
      if( legende!=null ) return legende;
      legende = Legende.adjustDefaultLegende(legende,Legende.NAME,     new String[]{  "_RAJ2000","_DEJ2000","ID",  "Image",   "RA (ICRS)","DE (ICRS)","X",     "Y",      "FWHM_X", "FWHM_Y", "Angle",  "Peak",  "Background" });
      legende = Legende.adjustDefaultLegende(legende,Legende.DATATYPE, new String[]{  "double",  "double",  "char","char",    "char",     "char",     "double","double", "double", "double", "double", "double","double" });
      legende = Legende.adjustDefaultLegende(legende,Legende.UNIT,     new String[]{  "deg",     "deg",     "",    "",        "",         "",         "deg",   "",       "" ,       "",       "",       "",     ""});
      legende = Legende.adjustDefaultLegende(legende,Legende.WIDTH,    new String[]{  "10",      "10",      "15",  "10",      "13",       "13",       "8",     "8",      "10",     "10",      "5",     "10",    "10"   });
      legende = Legende.adjustDefaultLegende(legende,Legende.PRECISION,new String[]{  "6",       "6",       "",    "",        "2",        "3",        "2",     "2",      "2",      "2",       "0",     "3",     "3"   });
      legende = Legende.adjustDefaultLegende(legende,Legende.DESCRIPTION,
            new String[]{  "RA","DEC", "Identifier",  "Reference image", "Right ascension",  "Declination",
            "X image coordinate",     "Y image coordinate",
            "X Full Width at Half Maximum", "Y Full Width at Half Maximum",
            "Angle",  "Source peak",  "image background" });
      legende = Legende.adjustDefaultLegende(legende,Legende.UCD,
            new String[]{  "pos.eq.ra;meta.main","pos.eq.dec;meta.main","meta.id;meta.main","","pos.eq.ra","pos.eq.dec",
            "pos.cartesian.x;obs.field","pos.cartesian.y;obs.field",
            "", "",
            "pos.posAng;obs.field", "","instr.background;obs.field" });
      legende.name="Pixel statistics";
      hideRADECLegende(legende);
      return legende;
   }
   
   private double [] iqe = null;
   
   /** Creation à partir d'une position céleste
    * @param plan plan d'appartenance
    * @param v vue de référence qui déterminera le PlanBase
    * @param c coordonnées
    * @param id identificateur spécifique, ou null pour attribution automatique
    */
   protected SourcePhot(Plan plan,ViewSimple v, Coord c,double [] iqe ) {
      super(plan,v,c,null);
      this.iqe = iqe;
      resumeMesures();
      if( ((PlanTool)plan).findFilter(FILTERNAME)<0 ) {
         ((PlanTool)plan).addFilter(FILTER);
      }
      ((PlanTool)plan).setFilter(-1);   // On n'allume pas le filtre
   }
   
   /** Post-traitement lors de la création */
   protected void suite() {
      leg=legende;
      setId();
      setShape(Obj.RETICULE);
   }

   /** (Re)énération de la ligne des infos (détermine les mesures associées) */
   protected void resumeMesures() {
      Coord c = new Coord(raj,dej);
      info = "<_A Phots>\t"+raj+"\t"+dej+"\t"+id+"\t"+planBase.label+"\t"+"\t"+c.getRA()+"\t"+c.getDE()
             +"\t"+iqe[0]+"\t"+iqe[2]+"\t"+iqe[1]+"\t"+iqe[3]+"\t"+iqe[4]+"\t"+iqe[5]+"\t"+iqe[6];
   }

   /** Retourne le type d'objet */
   public String getObjType() { return "Phot"; }

   /** Positionne l'id par defaut */
   void setId() {
      id="Phot "+ nextIndice();
   }
   
   protected int getL() { return super.getL()*3; }
}
