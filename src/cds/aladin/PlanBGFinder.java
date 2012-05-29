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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.TreeMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cds.tools.Util;


public class PlanBGFinder extends PlanBG {


   private PlanBG planBG;
   
   protected boolean isVerbose() { return false; }

   protected PlanBGFinder(Aladin aladin,PlanBG planBG) {
      super(aladin);
      this.planBG=planBG;
      type=ALLSKYFINDER;
      url = planBG.url;
      survey = planBG.survey;
      version = planBG.version;
      useCache = planBG.useCache;
      frameOrigin=planBG.frameOrigin;
      imageSourcePath=planBG.imageSourcePath;
      pixList = new Hashtable<String,HealpixKey>(1000);
      loader = new HealpixLoader();
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
   protected String resolveImageSourcePath(String json) {
      if( imageSourcePath==null ) return null;
      try {
         String jsonKey,value,pattern=null, replacement=null, result;
         Tok tok = new Tok(imageSourcePath," ");
         System.out.println("ANALYSE de imageSourcePath => "+imageSourcePath);
         while( tok.hasMoreTokens() ) {
            boolean flagRegex=false;
            String t = tok.nextToken();
            System.out.println("==> "+t);
            
            int o = t.indexOf(":");
            if( o>=0 && t.charAt(o+1)=='/' ) flagRegex=true;
            
            // Recherche d'une règle de réécriture par une expression simple
            // (ex: id:http://monserveur/mycgi?img=$1)
            else {
               if( o<0 ) {
                  if( aladin.levelTrace>=3 ) System.err.println("In \"properties\" file of \""+survey+"\": imageSourcePath syntax error ["+t+"] => ignored");
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
            System.out.println("jsonKey=["+jsonKey+"] pattern=["+pattern+"] value=["+value+"] => replacement=["+replacement+"] => resul=["+result+"]");
            return result;
         }
         throw new Exception("imageSourcePath syntax error");
      } catch( Exception e ) {
         aladin.trace(4,"PlanBG.resolveImageSourcePath ["+imageSourcePath+"] syntax error !");
         imageSourcePath=null;
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         return null;
      }
   }
   
   // Remplacement par expression régulière avec support des encodages HTTP pour les URLs
   private  String replaceAll(String value,String regex, String replacement) {
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

   protected void log() { }
   protected void clearBuf() { }
   protected HealpixKey getHealpixFromAllSky(int order,long npix) { return null; }

   /** Demande de chargement du losange repéré par order,npix */
   public HealpixKey askForHealpix(int order,long npix) {
      HealpixKey pixAsk = new HealpixKeyFinder(this,order,npix);
      pixList.put( key(order,npix), pixAsk);
      return pixAsk;
   }

   /** Retourne l'order max du dernier affichage */
   protected int getCurrentMaxOrder(ViewSimple v) { return Math.max(2,Math.min(maxOrder(v),maxOrder)); }
   
   /** Retourne le losange Healpix s'il est chargé, sinon retourne null
    * et si flagLoad=true, demande en plus son chargement si nécessaire */
   protected HealpixKey getHealpix(int order,long npix,boolean flagLoad) {
      HealpixKey healpix =  pixList.get( key(order,npix) );
      if( healpix!=null ) return healpix;
      if( flagLoad ) return askForHealpix(order,npix);
      return null;
   }

   protected void draw(Graphics g,ViewSimple v) {
      long [] pix=null;
      TreeMap<String,TreeNodeProgen> set = new TreeMap<String,TreeNodeProgen>();
      int order = planBG.getMaxFileOrder();
//      System.out.println("maxOrder="+maxOrder(v)+" order="+order+" isAllsky="+v.isAllSky());
      
      // On n'a pas assez zoomé pour afficher le contenu des losanges
      if( maxOrder(v)<order-2 || v.isAllSky() ) return;
      
      int nb=0;
      boolean allKeyReady=true;

      hasDrawnSomething=false;

      setMem();
      resetPriority();

      pix = getPixListView(v,order);
      
      for( int i=0; i<pix.length; i++ ) {

         if( (new HealpixKey(this,order,pix[i],HealpixKey.NOLOAD)).isOutView(v) ) continue;

         HealpixKeyFinder healpix = (HealpixKeyFinder)getHealpix(order,pix[i], true);

         // Juste pour tester la synchro
         //            Util.pause(100);

         // Inconnu => on ne dessine pas
         if( healpix==null ) continue;
         
         // Positionnement de la priorité d'affichage
         healpix.priority=250-(priority++);

         int status = healpix.getStatus();

         // Losange erroné ?
         if( status==HealpixKey.ERROR ) continue;

         // On change d'avis
         if( status==HealpixKey.ABORTING ) healpix.setStatus(HealpixKey.ASKING,true);

         // Losange à gérer
         healpix.resetTimer();

         if( status!=HealpixKey.READY ) allKeyReady=false;

         // Pas encore prêt
         if( status!=HealpixKey.READY ) continue;

         nb += healpix.draw(g,v,set);
      }
     
      this.set = set;

      allWaitingKeysDrawn = allKeyReady;

      hasDrawnSomething=hasDrawnSomething || nb>0;

      if( pix!=null && pix.length>0  ) tryWakeUp();
   }
   
   TreeMap<String,TreeNodeProgen> set=null;
   
   protected TreeMap<String,TreeNodeProgen> getLastProgen() { return set; }
   

   /** Demande de réaffichage des vues */
   protected void askForRepaint() {
      aladin.view.repaintAll();
   }
   
   protected void gc() { }
}
