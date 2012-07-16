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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.tools.Util;

/** Gère les infos d'un progéniteur 
 * (image originale qui ont permis de créer le survey Healpix)
 */
public class HealpixIndexItem {
   
   private String json;     // La chaine des infos
   
   public HealpixIndexItem(String s) { json=s; }
   
   public String getJson() { return json; }
   
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
         Aladin.trace(4,"PlanBG.resolveImageSourcePath ["+imageSourcePath+"] syntax error !");
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
//         System.out.println("tok["+i+"]="+param[i]);
      }
      
      String s = Glu.dollarSet(replacement, param, Glu.URL);
//      System.out.println("Glu => "+s);
      return s;
   }


}
