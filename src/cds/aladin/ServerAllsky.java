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
 * Le formulaire d'interrogation de l'arbre des Allskys
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : juillet 2010
 */
public class ServerAllsky extends ServerTree  {
   private boolean populated=false;

   /** Initialisation des variables propres */
   @Override
   protected void init() {
      type        = APPLI;
      aladinLabel = "Allsky";
      aladinLogo  = "Allsky.gif";
   }

   @Override
   protected void createChaine() {
      super.createChaine();
      title = aladin.chaine.getString("ALLSKYTITLE");
      info = aladin.chaine.getString("ALLSKYINFO");
      info1 = null;
   }

   /** Creation du formulaire d'interrogation par arbre. */
   protected ServerAllsky(Aladin aladin) { super(aladin); }

   @Override
   protected int createPlane(String target,String radius,String criteria, String label, String origin) {
      String survey;
      int defaultMode=PlanBG.UNKNOWN;
      if( criteria==null || criteria.trim().length()==0 ) survey="DSS colored";
      else {
         Tok tok = new Tok(criteria,", ");
         survey = tok.nextToken();
         
         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( s.equalsIgnoreCase("Fits") ) defaultMode=PlanBG.FITS;
            else if( s.equalsIgnoreCase("Jpeg") || s.equalsIgnoreCase("jpg") ) defaultMode=PlanBG.JPEG;
         }
      }
      
      int j = aladin.glu.findGluSky(survey,2);
      if( j<0 ) {
         Aladin.warning(this,"Healpix allsky unknown ["+survey+"]",1);
         return -1;
      }

      TreeNodeAllsky gSky = aladin.glu.getGluSky(j);
      try { gSky.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.toStdoutAndConsole("!!! "+e.getMessage());
      }
      aladin.allsky(gSky,label,target,radius);
      
      return j;
      
//      int j=criteria==null || criteria.length()==0 ? 0 : aladin.glu.findGluSky((new Tok(criteria)).nextToken(),2);
//      if( j!=-1 ) aladin.allsky(aladin.glu.getGluSky(j),label,target,radius);
//      return j;
   }

   @Override
   protected boolean is(String s) { return s.equalsIgnoreCase(aladinLabel); }

   @Override
   protected void initTree() { 
      if( populated ) return;
      populated=true;
      freeTree();
      populateTree( aladin.glu.vGluSky.elements() );
   }

}
