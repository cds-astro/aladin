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
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
* Chargement et affichage des constellations
* 
* @author Pierre Fernique [CDS]
* @version 1.0 : nov 2015 - creation
*/
public class Constellation {
   
   static private final String CONSTELLATION_FILE = "Constellation.txt";
   static private Color COLOR = new Color(108,171,236);
//   static private Color COLOR = Color.yellow;
   
   private Aladin aladin;
   private HashMap<String, ArrayList<Position>> cstMap = null;
   
   protected Constellation(Aladin aladin) {
      this.aladin = aladin;
      try { load(); } catch(Exception e) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
   }
   
   /** Chargement du fichier des constellations sous la forme d'une
    * HashMap dont les clés sont les noms des constellations et les valeurs
    * une arrayList des coordonnées
    * @throws Exception
    */
   protected void load() throws Exception {
      BufferedReader is = null;
      try {
         is = new BufferedReader(new InputStreamReader(
                   Aladin.class.getResourceAsStream("/"+CONSTELLATION_FILE)));
         
         cstMap = new HashMap<String, ArrayList<Position>>();
         String s,oCst=null;
         ArrayList<Position> cc = null;
         LigneConst oc = null;
         Projection proj = null;
         double minX=0,maxX=0,minY=0,maxY=0;
         boolean flagInit=true;
         boolean encore=true;
         
         while( encore ) {
            s=is.readLine();
            if( s==null ) encore=false;
            
            Tok tok = new Tok(s," ");
            try {
               double ra=0,dec=0;
               String cst;
               if( !encore ) cst="";
               else {
                  ra = Double.parseDouble(tok.nextToken());
                  dec = Double.parseDouble(tok.nextToken());
                  cst = tok.nextToken();
               }
               // Changement de constellation
               if( !cst.equals(oCst) ) {
                  
                  // fermeture du polygone
                  // et mémorisation de le précédente constellation
                  if( cc!=null ) {
                     
                     // Mise en place du dernier élément qui boucle sur le premier
                     LigneConst c=(LigneConst)cc.get(0);
                     c = new LigneConst(oc.raj,oc.dej,oc);
                     c.bout=3;
                     cc.add(c);
                     
                     // Mise en place du label au barycentre
                     Coord coo = new Coord();
                     
                     coo.x = (minX+maxX)/2;
                     coo.y = (minY+maxY)/2;
                     
                     // Bidouillage pour cette constellation, car le barycentre est en dehors
                     if( oCst.equals("DRA")  ) coo.x -= (maxX-minX)/3;
                     else if( oCst.equals("ERI")  ) coo.x -= (maxX-minX)/4;
                     else if( oCst.equals("PUP")  ) coo.x -= (maxX-minX)/4;
                     else if( oCst.equals("PSC")  ) coo.x -= (maxX-minX)/4;
                     else if( oCst.equals("SER2")  ) coo.y -= (maxX-minX)/6;
                     else if( oCst.equals("SCO")  ) coo.y += (maxX-minX)/5;

                     proj.getCoordNative(coo);
                     TagConstellation tag = new TagConstellation(coo,oCst);
                     tag.setColor(COLOR);
                     cc.add(tag);
                     
                     
                     // Mémorisation de la constellation
                     cstMap.put(cst,cc);
                     oc=null;
                     flagInit=true;
                     
                     if( !encore ) break;
                  }
                  
                  // Préparation de la liste des coordonnées de la nouvelle constellation
                  cc = new ArrayList<Position>();
                  
                  // Generation d'une projection centrée sur la constellation
                  // afin de déterminer la position du label au barycentre
                  proj = new Projection(null,Projection.WCS,ra,dec,60,0,0,60,0,false,Calib.SIN,Calib.FK5);
               }
               oCst = cst;
               
               // Ajout de la coordonnée courante
               LigneConst c = new LigneConst(ra, dec, oc);
               c.setColor(COLOR);
               cc.add(c);
               oc=c;
               
               // Recherche du barycentre dans un plan projeté
               // afin de déterminer les coordonnées du label
               Coord coo = new Coord(ra,dec);
               proj.getXYNative(coo);               
               if( minX>coo.x || flagInit ) minX=coo.x;
               if( maxX<coo.x || flagInit ) maxX=coo.x;
               if( minY>coo.y || flagInit ) minY=coo.y;
               if( maxY<coo.y || flagInit ) maxY=coo.y;
               flagInit=false;
               
            } catch( Exception e ) { if( Aladin.levelTrace>=3 ) e.printStackTrace(); }
         }
      }
      finally {
         if( is!=null ) is.close();
      }
   }
   
   // Mémorisation des numéros des dernières projections pour chaque vue
   // afin de s'éviter un recalcul inutile
   private short [] projOk = new short[ViewControl.MAXVIEW];
   
   protected  void draw(Graphics g, ViewSimple v, int dx, int dy) {
      boolean flagProj = projOk[v.n]!=v.iz;
      for( ArrayList<Position> cst : cstMap.values() ) {
         for( Position o : cst ) {
            if( flagProj ) o.projection(v);
            o.draw(g, v, dx, dy);
         }
      }
      projOk[v.n]=v.iz;
   }
   
   
   
}
