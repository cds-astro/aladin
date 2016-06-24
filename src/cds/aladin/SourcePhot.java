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
 * Objet graphique correspondant a un tag Phot manuel sous forme de Source modifiable (avec des mesures associées)
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : june 2016 - création
 */
public class SourcePhot extends SourceTag  {
   
   protected Repere rep;
   
   /** Création ou maj d'une légende associée à un SourcePhot */
   static protected Legende createLegende() {
      if( legende!=null ) return legende;
      legende = Legende.adjustDefaultLegende(legende,Legende.NAME,     new String[]{  "ID",  "RA (ICRS)","DE (ICRS)","Radius","Count",  "Sum",   "Mean",  "Sigma", "Area",  "Median" });
      legende = Legende.adjustDefaultLegende(legende,Legende.DATATYPE, new String[]{  "char","char",     "char",     "double","integer","double","double","double","double","double"});
      legende = Legende.adjustDefaultLegende(legende,Legende.UNIT,     new String[]{  "char","\"h:m:s\"","\"h:m:s\"","arcmin","pixel",  "",      "",      "",      "arcmin^2","" });
      legende = Legende.adjustDefaultLegende(legende,Legende.WIDTH,    new String[]{  "15",   "13",      "13",       "10",    "10",     "10",    "10",    "10",    "10",      "" });
      legende = Legende.adjustDefaultLegende(legende,Legende.PRECISION,new String[]{  "",     "2",        "3",       "2",     "2",      "2",     "2",     "2",     "2",       "" });
      legende = Legende.adjustDefaultLegende(legende,Legende.DESCRIPTION,
            new String[]{  "Identifier",  "Right ascension",  "Declination","Radius","Pixel count","Sum of pixel values","Mean of pixel values","Sigma of pixel list","Area", "Median of pixel list" });
      legende = Legende.adjustDefaultLegende(legende,Legende.UCD,
            new String[]{  "meta.id;meta.main","pos.eq.ra;meta.main","pos.eq.dec;meta.main","","","","","","","" });
      return legende;
   }
   
   public SourcePhot(Plan plan, PlanImage planBase, Repere rep) {
      super(plan,planBase,rep.raj,rep.dej, rep.id );
      this.rep = rep;
   }
   
   protected void suite() {
      leg=createLegende();
   }
   
   protected void setInfo(double raj, double dej,String info) {
      Coord c = new Coord(raj,dej);
      if( this.id==null ) this.id= "Phot "+plan.pcat.getNextID();
      this.info = "<&_A>\t"+id+"\t"+c.getRA()+"\t"+c.getDE() +"\t"+get("Rad",info)+"\t"+get("Cnt",info)+"\t"
                   +get("Sum",info)+"\t"+get("Avg",info)+"\t"+get("Sigma",info)+"\t"+get("Surf",info)+"\t"+get("Med",info);
   }
   
   // Extraction de la valeur repérée par son mot clé, sur une chaine au format
   // Cnt 6446 / Sum 7221 / Sigma 0.4649 / Min 0.4734 / Avg 1.12 / Med 0.9713 / Max 3.274 / Rad 45.6" / Surf 1.818'²
   private String get(String key,String info) {
      int offset = info.indexOf(key+" ");
      if( offset==-1 ) return " ";
      int a = offset+key.length()+1;
      int b = info.indexOf(' ',a);
      if( b==-1 ) b=info.length();
      return info.substring(a,b);
   }
   

}

