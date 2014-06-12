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

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.aladin.stc.STCObj;
import cds.aladin.stc.STCPolygon;
import cds.aladin.stc.STCStringParser;
import cds.tools.Util;

/** Gère les infos d'un progéniteur 
 * (image originale qui ont permis de créer le survey Healpix)
 */
public class HealpixProgenItem {
   
   private String json;     // La chaine des infos
   
   private List<STCObj> stcObjects = null; // liste des objets stc correspondant au footprint du progéniteur
   private boolean stcError=false; // true si on ne parvient pas à extraire le stc
   
   
   public HealpixProgenItem(String s) { json=s; }
   
   /** retourne la ligne brute JSON */
   public String getJson() { return json; }
   
   static final char SEP = Util.FS.charAt(0);
   
   /** Retourne l'ID associé à une entrée dans un fichier d'index Healpix
    * Il s'agit : soit du champ "name", soit du champ "path" (sans l'extension optionnelle [x,y,w,h])
    * et sinon la ligne en totalité (toujours sans l'extension optionnelle)
    */
   public String getID() {
      String key = cds.tools.Util.extractJSON("name", json);
      if( key==null ) {
         int first=-1;
         key = cds.tools.Util.extractJSON("path", json);
         if( key==null ) key=json;
         if( key.charAt(key.length()-1)==']' ) first = key.lastIndexOf('[');
         if( first>0 ) key = key.substring(0, first);
      }
      
      // On ne garde que le dernier élément après le dernier '/' pour éviter de construire une arborescence
      int offset = key.lastIndexOf(SEP);
      if( offset>=0 ) key = key.substring(offset+1,key.length() );
      return key;
   }

   /** Retourne le path original associé à une entrée dans un fichier d'index Healpix
    * Il s'agit : soit du champ "path", soit de la ligne en totalité (sans l'extension optionnelle [x,y,w,h])
    */
   public String getPath() {
      String path;
      int first=-1;
      path = cds.tools.Util.extractJSON("path", json);
      if( path==null ) path=json;
      if( path.charAt(path.length()-1)==']' ) first = path.lastIndexOf('[');
      if( first>0 ) path = path.substring(0, first);
      return path;
   }

   /** Retourne le STC propre à cette image, ou null s'il n'y en a pas */
   public String getSTC() {
      return cds.tools.Util.extractJSON("stc", json);
   }
   
   /** Retourne la liste des objets STC qui décrivent le footprint du progéniteur
    * ou null s'il y a un blème */
   public List<STCObj> getSTCObj() {
      if( stcError ) return null;       // déjà essayé et ça merdouille
      if( stcObjects==null ) {
         try {
            stcObjects = new STCStringParser().parse( getSTC() );
         } catch( Exception e ) {
            stcError=true;
            return null;
         } 
      }
      return stcObjects;
   }
   
   private long ovIZ;              // Signature de la vue/projection/zoom utilisée pour le calcul de coins[]
   private PointD[] viewCorners;   // les sommets en X,Y dans la vue repérée par ovIZ;
   private Coord[] corners;        // Coordonnées Ra,Dec ICRS des sommets du footprint
   private int nNull;              // Nombre de sommets indéfinis (derrière le ciel)
   
   private boolean computeCorners; // True si corners[] a déjà été calculé
   
   /** Retourne true si le XY (coord de la vue) se trouve dans le footprint */
   // ATTENTION, NE PREND PAS EN COMPTE QUE LE CAS D'UN UNIQUE POLYGONE
   public boolean isIn(ViewSimple v,int xview,int yview) {
      PointD [] somView = getProjViewCorners(v);
      int x[] = new int[somView.length];
      int y[] = new int[somView.length];
      for( int i=0; i<somView.length; i++ ) {
         x[i]=(int)somView[i].x;
         y[i]=(int)somView[i].y;
      }
      Polygon pol = new Polygon(x,y,x.length);
      return pol.contains(xview, yview);
   }
   
   /** Retourne les coordonnées des sommets */
   public Coord [] getCorners() {
      if( !computeCorners ) corners=computeCorners();
      return corners;
   }
      
   /** retourne la liste des sommets.
    * ATTENTION : NE PREND EN COMPTE QUE LE PREMIER POLYGONE STC!!    */
   private Coord [] computeCorners() {
      computeCorners=true;
      List<STCObj> list = getSTCObj();
      if( list==null ) return null;
      
      // Détermination du nombre de sommets
      int n=0;
      for( STCObj obj : list ){
         if( !(obj instanceof STCPolygon) ) continue;
         n += ((STCPolygon)obj).getxCorners().size();
         break;                                         // On ne prend en compte que les permier Polygone pour le moment
      }
      
      // Copie des sommets
      Coord [] coo = new Coord[n];
      int i=0;
      for( STCObj obj : list ){
         if( !(obj instanceof STCPolygon) ) continue;
         ArrayList<Double> x = ((STCPolygon)obj).getxCorners();
         ArrayList<Double> y = ((STCPolygon)obj).getyCorners();
         int m = x.size();
         for( int j=0; j<m; j++ ) coo[i++] = new Coord(x.get(j) , y.get(j));
         break;                                         // On ne prend en compte que les permier Polygone pour le moment
      }
      return coo;
   }

   /** Retourne les coordonnées X,Y des angles du stc dans la projection de
    * la vue ou null si problème */
   private PointD[] getProjViewCorners(ViewSimple v) {
      
      // déjà fait ?
      long vIZ = v.getIZ();
      if( ovIZ==vIZ ) {
         if( nNull>1 ) return null;
         return viewCorners;
      }
      
      Projection proj=v.getProj();
      Coord [] corners = getCorners();
      if( proj==null || corners==null ) return null;
      nNull=0;
      if( viewCorners==null ) viewCorners = new PointD[corners.length];

      // On calcul les xy projection des sommets
      for (int i = 0; i<corners.length; i++) {
         Coord c = corners[i];
         proj.getXY(c);
         if( Double.isNaN(c.x)) {
            nNull++;
            if( nNull>1 ) return null;
            else { viewCorners[i]=null; continue; }
         }
         if( viewCorners[i]==null ) viewCorners[i]=new PointD(c.x,c.y);
         else { viewCorners[i].x=c.x; viewCorners[i].y=c.y; }
      }
      
      // On calcule les xy de la vue
      for( int i=0; i<corners.length; i++ ) {
         if( viewCorners[i]!=null ) v.getViewCoordDble(viewCorners[i],viewCorners[i].x, viewCorners[i].y);
      }

      ovIZ=vIZ;
      return viewCorners;
   }
   
   
//   public boolean isOutView(ViewSimple v) {
//      boolean rep = isOutView1(v);
//      System.out.println(getID()+" => "+(rep?"out":"in"));
//      return rep;
//   }
   
   /** Retourne true si à coup sûr le footprint est hors de la vue courante */
   public boolean isOutView(ViewSimple v) {
      if( v.isAllSky() ) return false;
      int w = v.getWidth();
      int h = v.getHeight();
      PointD [] b = getProjViewCorners(v);
      if( b==null ) return false;    // On n'a pas les footprints STC? on suppose par défaut qu'on est dans la vue (!!!)

      double minX,maxX,minY,maxY;
      minX=maxX=b[0].x;
      minY=maxY=b[0].y;
      for( int i=1; i<b.length; i++ ) {
         if( b[i].x<minX ) minX = b[i].x;
         else if( b[i].x>maxX ) maxX = b[i].x;
         if( b[i].y<minY ) minY = b[i].y;
         else if( b[i].y>maxY ) maxY = b[i].y;
      }

      // Tout à droite ou tout à gauche
      if( minX<0 && maxX<0 || minX>=w && maxX>=w ) return true;

      // Au-dessus ou en dessous
      if( minY<0 && maxY<0 || minY>=h && maxY>=h ) return true;

      return false;  // Mais attention, ce n'est pas certain !!
   }
   
   // Extraction du path ou de l'url d'accès au progéniteur.
   // Utilise les possibililités de réécriture du champ "imageSourcePath" extrait du fichier "properties" du survey
   // Les règles de réécriture peuvent être multiple, séparées par un espace
   // La première règle qui matche est retenue
   // Une règle de réécriture suit la syntaxe suivante :
   //     jsonKey:/regex/replacement/
   // ou  jsonKey:replacement
   // L'expression "regex" doit contenir des expressions d'extraction au moyen de parenthèses
   // L'expression "replacement" peut réutiliser ces extractions par des variables $1, $2...
   // Si le remplacement se trouve dans la zone de paramètres d'une url, l'encodage HTTP est assuré
   // La "jsonKey" fait référence à une clé JSON de la chaine passée en paramètre.
   // Une clé vide (rien avant le ':') signifie que toute la chaine est prise en compte (path simple)
   public String resolveImageSourcePath(String imageSourcePath) {
      if( imageSourcePath==null ) return null;
      try {
         String jsonKey,value,pattern=null, replacement=null, result;
         Tok tok = new Tok(imageSourcePath," ");
         Aladin.trace(4,"HealpixIndexItem.resolveImageSourcePath()  imageSourcePath => "+imageSourcePath);
         while( tok.hasMoreTokens() ) {
            boolean flagRegex=false;
            String t = tok.nextToken();
//            System.out.println("==> "+t);
            
            int o = t.indexOf(":");
            if( o>=0 && t.charAt(o+1)=='/' ) flagRegex=true;
            
            // Recherche d'une règle de réécriture par une expression simple
            // (ex: id:http://monserveur/mycgi?img=$1)
            else {
               if( o<0 ) {
                  if( Aladin.levelTrace>=3 ) System.err.println("In \"properties\" imageSourcePath syntax error ["+t+"] => ignored");
                  continue;
               }
               pattern="^(.*)$";
               replacement = t.substring(o+1);
               
            }
            
            jsonKey = t.substring(0,o);
            if( jsonKey.length()==0 ) value=json;
            else if( jsonKey.equals("path") ) value=getPath();   // cas particulier du path qui pourrait être suffixé par [x,y,w,h]
            else value = Util.extractJSON(jsonKey, json);
            if( value==null ) continue;
               
            // Recherche d'une règle de réécriture par une expression régulière
            // (ex: path:/\/([^\/]+)\.fits/http:\/\/monserveur\/mycgi?img=$1/  )
            if( flagRegex ) {
               int o1 = o+1;
               while( (o1=t.indexOf('/',o1+1))!=-1 && t.charAt(o1-1)=='\\');
               if( o1<0 ) throw new Exception("regex not found");
               pattern = t.substring(o+2,o1);
               int n=t.length();
               if( t.charAt(n-1)=='/' ) n--;
               replacement = t.substring(o1+1,n);
            }
            result = replaceAll(value,pattern, replacement);
            Aladin.trace(4,"HealpixIndexItem.resolveImageSourcePath() jsonKey=["+jsonKey+"] pattern=["+pattern+"] value=["+value+"] => replacement=["+replacement+"] => resul=["+result+"]");
            return result;
         }
         throw new Exception("imageSourcePath syntax error");
      } catch( Exception e ) {
         Aladin.trace(4,"HealpixIndexItem.resolveImageSourcePath ["+imageSourcePath+"] syntax error !");
         imageSourcePath=null;
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         return null;
      }
   }
   
   // Remplacement par expression régulière avec support des encodages HTTP pour les URLs
   private String replaceAll(String value,String regex, String replacement) {
      Matcher m = Pattern.compile(regex).matcher(value);
      
      // s'il ne s'agit pas d'une url, pas besoin de se fatiguer
      if( !replacement.startsWith("http:") && !replacement.startsWith("https:") 
            && !replacement.startsWith("ftp:") ) return m.replaceAll(replacement);
      
      // On est obligé de faire un mapping bidon pour récupérer les groupes extraits
      // puis on effectue la substitution avec la méthode Glu.dollarSet(...)
      int n = m.groupCount();
      StringBuffer split = new StringBuffer();
      for( int i=0; i<n; i++ ) split.append("$"+(i+1)+"\n");
//      System.out.println("split=>"+split);
      String tmp =  m.replaceAll(split.toString());
//      System.out.println("tmp=>"+tmp);
      Tok tok = new Tok(tmp,"\n");
      String param [] = new String[tok.countTokens() ];
      for( int i=0; tok.hasMoreTokens(); i++ ) {
         param[i]=tok.nextToken();
//        System.out.println("tok["+i+"]="+param[i]);
      }
      
      String s = Glu.dollarSet(replacement, param, Glu.URL);
      System.out.println("Glu => "+s);
      return s;
   }


}
