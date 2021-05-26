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

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Toolkit;


/**
 * Objet graphique representant une mesure photométrique manuelle
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 fév 2021: Découpage SourceStat et SourceInfo
 */
public class SourceInfo extends SourceTag {
   
   static protected Legende legende=createLegende();
   
   /** Création ou maj d'une légende associée à un SourceInfo */
   static protected Legende createLegende() {
      if( legende!=null ) return legende;
      legende = Legende.adjustDefaultLegende(legende,Legende.NAME,     new String[]{  "_RAJ2000","_DEJ2000","ID",  "s_region", "Origin", "RA (ICRS)","DE (ICRS)","Count",  "Sum",   "Sigma",  "Min",   "Avg",  "Max",   "Area",       });
      legende = Legende.adjustDefaultLegende(legende,Legende.DATATYPE, new String[]{  "double",  "double",  "char","char",     "char",  "char",     "char",     "integer","double","double", "double","double","double","double"  });
      legende = Legende.adjustDefaultLegende(legende,Legende.UNIT,     new String[]{  "deg",     "deg",     "",    "",         "",      "\"h:m:s\"","\"h:m:s\"","pixel",  "",      "",       "",      "",      "",      "arcmin^2"       });
      legende = Legende.adjustDefaultLegende(legende,Legende.WIDTH,    new String[]{  "10",      "10",      "6",   "5",        "10",    "10",       "10",       "6",      "8",     "6",     "6",     "6",      "6",     "6"    });
      legende = Legende.adjustDefaultLegende(legende,Legende.PRECISION,new String[]{  "6",       "6",       "",    "",         "",      "4",        "5",        "2",      "4",     "4",     "4",     "4",      "4" ,    "4",         });
      legende = Legende.adjustDefaultLegende(legende,Legende.DESCRIPTION,
            new String[]{  "RA","DEC", "Identifier",  "Field of View", "Reference image", "Right ascension",  "Declination","Pixel count","Sum of pixel values","Median of the distribution", "Minimum value","Average value", "Maximum value",
                           "Area (pixels)" });
      legende = Legende.adjustDefaultLegende(legende,Legende.UCD,
            new String[]{  "pos.eq.ra;meta.main","pos.eq.dec;meta.main","meta.id;meta.main","","","pos.eq.ra","pos.eq.dec","","","","","","","" });
      legende.name="Pixel statistics";
      hideRADECLegende(legende);
      return legende;
   }
   
   protected int dw,dh;          // mesure du label
   
   protected boolean draw(Graphics g,ViewSimple v,int dx,int dy) { 
      return super.draw(g,v,dx,dy);
   }
   
   /** Creation pour les backups */
   protected SourceInfo(Plan plan) { super(plan); }

   /** Création d'une SourceInfo à partir d'une SourceStat (Cercle) */
   protected SourceInfo( SourceStat s ) {
      super(s.plan);
      id = "Circle "+nextIndice();
      setInfo( s );
   }
   
   /** Création d'une SourceInfo à partir d'une ligne (Polygone) */
   protected SourceInfo( Ligne s ) {
      super(s.plan);
      id = "Polygon "+nextIndice();
      setInfo( s );
   }
   
   protected SourceInfo(Plan plan,ViewSimple v, Coord c,String id) {
      super(plan,v,c,id);
   }
   
   protected SourceInfo(Plan plan, ViewSimple v, double x, double y,String id) {
      super(plan,v,x,y,id);
   }

   /** Génération de la ligne des infos et du FoV */
   private  void setInfo( Obj s ) {
      raj = s.raj;
      dej = s.dej;
      setLeg(legende);

      double stat[] = null;
      int z=-1;
      
      // On travaille toujours sur le plan de base
      planBase = plan.aladin.calque.getPlanBase();
      
      String nomPlan = planBase.label;
      if( planBase.isCube() ) {
         z=(int)planBase.getZ();
         int d = 1+z;
         nomPlan+="/"+d;
      }
      
      // regénération des stats (au cas où)
      try { stat = s.getStatistics(planBase,z); }
      catch( Exception e ) { stat=null; }
      
      String cnt  = stat==null ? " " : ""+stat[0];
      String tot  = stat==null ? " " : ""+stat[1];
      String avg  = stat!=null && stat[0]>0 ? ""+(stat[1]/stat[0]) : " ";;
      String sig  = stat==null ? " " : ""+stat[2];
      String surf = stat==null ? " " : ""+stat[3]*3600;
      String min  = stat==null ? " " : ""+stat[4];
      String max  = stat==null ? " " : ""+stat[5];
      
      Coord c = new Coord(s.raj,s.dej);
      
      // Dans le cas d'un HiPS on ajoute à l'origine l'order
      if( planBase instanceof PlanBG ) {
         PlanBG pbg = (PlanBG)planBase;
         int orderFile=pbg.getOrder();
         nomPlan= nomPlan+" (HiPS order "+orderFile+")";
      }
      
      // Génération du FoV associé
      String fov = s instanceof SourceStat ? ((SourceStat)s).getFoV( ) : s instanceof Ligne ? ((Ligne) s).getFoV() : null;
      if( fov!=null ) {
         setFootprint(fov);
         setIdxFootprint(3);
//         setShowFootprint(true,false);   // On ne montre pas le FoV par défaut
      }
      
      // Génération de la ligne des infos
      info = "<&_A Phots>\t"+raj+"\t"+dej+"\t"+id+"\t"+fov+"\t"+nomPlan+"\t"+c.getRA()+"\t"+c.getDE()+"\t"+cnt+"\t"+tot+"\t"+sig+"\t"+min+"\t"+avg+"\t"+max+"\t"+surf;
   }
   
   /** Retourne le type d'objet */
   static private final String C= "|";

   /** Retourne le type d'objet */
   public String getObjType() { return "Phot"; }

   /** Determine le decalage pour ecrire l'id */
   void setD() {
      FontMetrics m = Toolkit.getDefaultToolkit().getFontMetrics(DF);
      dw = m.stringWidth(id)+4;
      dh=HF;
   }
}
