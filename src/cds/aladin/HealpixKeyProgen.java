// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cds.xml.Field;

public class HealpixKeyProgen extends HealpixKeyCat {

   protected HealpixKeyProgen(PlanBG planBG) { super(planBG); }

   protected HealpixKeyProgen(PlanBG planBG,int order, long npix) {
      super(planBG,order,npix);
   }
   
   /** Fournit un Inputstream � partir du bloc de byte lu */
   protected InputStream getInputStreamFromStream() throws Exception {
      try {
         return json2TSV(stream);
      } catch( Exception e ) {
         if( planBG.aladin.levelTrace>=3 ) e.printStackTrace();
         throw e;
      }
   }
   
//   static boolean flagP=true;
   
   // Retourne un InputStream en TSV qui reprend les informations contenues dans en JSON HpxFinder/NorderX/DirY/NpixH
   // sous la forme d'une table classique
   private InputStream json2TSV(byte [] stream) throws Exception {
      Legende leg = planBG.getFirstLegende();
      MyByteArrayStream out = new MyByteArrayStream();
      BufferedReader reader =  new BufferedReader( new InputStreamReader( new ByteArrayInputStream(stream) ));
      String s,s1;
      
      boolean first=true;
      while( (s=reader.readLine())!=null ) {
         if( s.trim().length()==0 ) continue;
         if( first ) {
            first=false;
            s1 = addLeg(leg);
//            if( flagP ) System.out.print("\n"+s1);
            out.write( s1 );
         }
         
         s1 = parseJson(s,leg);
//         if( flagP ) System.out.print(s1);
         out.write( s1 );
      }
//      flagP=false;
      reader.close();
      InputStream in = out.getInputStream();
      out.close();
      return in;
   }
   
   // Cr�e un header TSV � partir de la l�gende
   private String addLeg(Legende leg) {
      StringBuilder res = new StringBuilder(1000);
      for( int i=0; i<leg.field.length; i++ ) {
         if( res.length()>0 ) res.append('\t');
         res.append(leg.field[i].name);
      }
      res.append('\n');
      return res.toString();
   }
   
   // Cr�e une ligne TSV � partir de la l�gende et des patterns
   // d'expressions r�guli�res d'extractions
   // syntaxe : XXX $[nom:regex] YYY ...
   // => recherche dans la ligne JSON la valeur associ�e � la cl� "nom",
   //    puis applique l'expression r�guli�re si elle est pr�sente. Celle-ci doit
   //    comporter des groupes d'extractions.
   //    Il peut y avoir plusieurs expressions de ce type �ventuellement
   //    s�par�es par du texte libre.
   // ex : file: $[path:(.*)_drz]  va extraire de la chaine "12345_drz"
   //      le r�sultat => file: 12345
   //
   private String parseJson(String json, Legende leg) throws Exception {
      double [] coo=null;
      Field [] field = leg.field;
      
      // Pour r�cup�rer la valeur associ�e � une cl� JSON
      ArrayList<String> jsonVal = getJsonVal(json);
      String [] jsonKey = new String [jsonVal.size()];
      getJsonLeg(json).toArray(jsonKey);
      
      String s;
      
      // Parcours de chaque champ de la table � produire avec
      // construction it�rative de la ligne TSV
      StringBuilder res = new StringBuilder(1000);
      for( int i=0; i<field.length; i++ ) {
         String pattern = field[i].hpxFinderPattern;
         
         // Pas de pattern ? simple r�cup�ration positionnel (normalement inutilis�)
         if( pattern==null ) s = jsonVal.get(i);
         else {
            // Parsing du pattern en 3 �l�ments
            // => prefix$[nom:regex]  => retourne nv[] = {prefix,nom,regex}
            String [] nv = new String[3]; 
            int offset=0;
            StringBuilder s1 = new StringBuilder(20);
            while( (offset=getSimplePattern(nv,pattern,offset))!=-1 ) {
               String prefix= nv[0];
               String nom = nv[1];
               String regex = nv[2];
               
//               System.out.println("prefix=\""+prefix+"\" variable=\""+nom+"\" regex=\""+regex+"\"");
               // Ajout du pr�fixe
               if( prefix!=null ) s1.append(prefix);
               
               if( nom!=null ) {
                  int indJson = cds.tools.Util.indexInArrayOf(nom, jsonKey);
                  
                  // Si le champ n'existe pas dans le JSON, on ignore
                  if( indJson==-1 ) {
                     
                     // Sauf cas particulier pour r�cup�rer des coordonn�es
                     // � partir du centre du FoV exprim� en STC
                     // RQ: POURRA ETRE EXPLICITE SI AMELIORATION DE PROGEN
                     if( nom.equals("_RAJ2000") || nom.equals("ra")) {
                        if( coo==null ) coo=getJsonCenter(json);
                        s1.append(coo[0]+"");
                     } else if( nom.equals("_DEJ2000") || nom.equals("dec")) {
                        if( coo==null ) coo=getJsonCenter(json);
                        s1.append(coo[1]+"");
                     }
                     break;
                  }
                  
                  // R�cup�ration de la valeur associ�e � la cl� JSON
                  s = jsonVal.get(indJson);
                  
                  // Si aucune regex, simple concat�nation
                  if( regex==null ) s1.append(s);
                  
                  // Sinon concat�nation des groupes de la regex
                  else {
                     Matcher m=null;
                     try {
                        Pattern p = Pattern.compile(regex);
                        m = p.matcher(s);
                     } catch( Exception e ) {
                        e.printStackTrace();
                     }
                     if( m.matches() ) {
                        int n = m.groupCount();
                        for( int j=1; j<=n; j++ ) {
                           String s2 =  m.group(j);
                           s1.append(s2);
//                           System.out.println(".regex=["+regex+"] n="+n+" group["+j+"]="+s2);
                        }
                     } else s1.append("**Regexp_error**");
                  }
               }
            }
            s = s1.toString();
         }
//         System.out.print("==> s=["+s+"]");
         if( res.length()>0 ) res.append('\t');
         res.append(s);
      }
      res.append('\n');
      return res.toString();
   }
   
   // Extration des 3 champs d'un pattern 
   // voir le commentaire de parseJson(...)
   private int getSimplePattern(String [] nv,String s,int offset) {
      int mode=0;
      int debDeb=offset;
      int deb=offset;
      int nb=0;
      int len=s.length();
      nv[0]=nv[1]=nv[2]=null;
      if( offset>=len ) return -1;
      for( ;offset<len; offset++ ) {
         char ch = s.charAt(offset);
         switch(mode) {
            case 0: // parsing du pr�fixe, j'attends le 1er '$['
               if( ch=='$' && s.charAt(offset+1)=='[' ) { 
                  if( offset>deb ) nv[0] = s.substring(deb,offset) ; 
                  offset++;
                  mode=1; deb=offset+1; 
               }
               break;
            case 1: // parsing du nom, j'attends le ']' fermant ou un ':' s�parateur
               if( ch==']' ) { nv[1] = s.substring(deb,offset); return offset+1; }
               if( ch==':' ) { mode=2; nb=0; nv[1] = s.substring(deb,offset); deb=offset+1; }
               break;
            case 2: // parsing de la regex, j'attends la fin de la regex par ']'
               if( nb==0 && ch==']' ) { nv[2]=s.substring(deb,offset); return offset+1; }
               if( ch=='[' && s.charAt(offset-1)!='\\' ) nb++;
               else if( nb>0 && ch==']' && s.charAt(offset-1)!='\\') nb--;
               break;
         }
      }
      
      // Le pr�fixe �tait tout seul, sans nom ni regex
      if( deb>=0 ) {
         nv[0] = s.substring(debDeb,offset);
         nv[1] = nv[2] = null;
      }
      return offset;
      
   }
   
   
   // Extraction des cl�s des champs JSON pour g�n�rer une ligne d'ent�te des noms de colonnes en TSV
   private ArrayList<String> getJsonLeg(String s) { return getJson(s,0); }
   
   // Extraction des valeurs des champs JSON pour g�n�rer une ligne de valeurs de colonnes en TSV
   private ArrayList<String> getJsonVal(String s) { return getJson(s,1); }
   
   // Proc�dure interne : mode=0 pour r�cup�rer les cl�s, mode=1 pour les valeurs
   private ArrayList<String> getJson(String s,int mode) {
      ArrayList<String> res = new ArrayList<>(5);
      
      char a[] = s.toCharArray();
      int m=0;
      int start=0;
      int crochet=0;
      boolean backslash=false;
      char quote=0;
      
      for( int i=0; i<a.length; i++ ) {
         char c = a[i];
         switch( m ) {
            case 0 : // Avant le premier champ
               if( c=='"' | c=='\'' ) { m=1; quote=c; start=i; }
               break;
            case 1 : // Dans le nom du champ
               if( c==quote && !backslash ) { if( mode==0 ) res.add( Tok.unQuote(s.substring(start,i+1)) ); m=2; }
               break;
            case 2 : // Avant le ":"
               if( c==':' ) m=3;
               break;
            case 3 : // Avant la valeur
               if( c=='[' ) { m=4; start=i; }
               else if( !Character.isSpace(c) ) { 
                  start=i;
                  if( c=='"' || c=='\'' ) { m=5; quote=c; }
                  else m=6;  
               }
               break;
            case 4 : // Dans un tableau
               if( c=='[' && !backslash ) crochet++;
               else if( c==']' && !backslash ) {
                  crochet--;
                  if( crochet==0 ) { if( mode==1 ) res.add( s.substring(start,i+1) ); m=0; }
               }
               break;
            case 5: // Dans une valeur quot�e
               if( c==quote && !backslash ) { if( mode==1 ) res.add( Tok.unQuote(s.substring(start,i+1)) ); m=0; }
               break;
            case 6 : // Dans une valeur non quot�e
               if( Character.isSpace(c) || c=='}' ) { if( mode==1 ) res.add( s.substring(start,i) ); m=0; }
               break;
         }
         backslash = c=='\\';
      }
      return res;
   }
   
//   private ArrayList<String> getJson(String s,int mode) {
//      ArrayList<String> res = new ArrayList<>(5);
//      char quote='"';
//      int start=s.indexOf(quote);
//      if( start==-1 ) {
//         quote='\'';
//         start=s.indexOf(quote);
//      }
//      int end=0;
//      for( int n=0; start>=0; n++, start = s.indexOf(quote,end+1) ) {
//         end = s.indexOf(quote,start+1);
//         if( end==-1 ) break;
//         if( (n%2) != mode ) continue;
//         String k = s.substring(start+1,end);
//         res.add(k);
//      }
//      return res;
//   }

   
   private static String STC = "POLYGON J2000 ";
   // M�thode pour r�cup�rer le centre de chaque observation � partir du STC d'une ligne JSON progen
   private double [] getJsonCenter(String s) throws Exception {
      int i = s.indexOf(STC);
      char quote='"';
      int j = s.indexOf(quote,i);
      if( j==-1 ) {
         quote='\'';
         j=s.indexOf(quote,i);
      }
      
      String listCoo = s.substring(i+STC.length(),j);
      StringTokenizer st = new StringTokenizer(listCoo);
      double coo [] = new double[2];
      int n;
      for( n=0; st.hasMoreTokens(); n++ ) {
         double ra = Double.parseDouble( st.nextToken() );
         double de = Double.parseDouble( st.nextToken() );
         coo[0]+=ra; coo[1]+=de;
      }
      coo[0] /= n;
      coo[1] /= n;
      return coo;
   }
   
  /** Retourne true s'il n'y a pas de descendant */
   protected boolean isLast() { return false; }
   
   /** Retourne true si on sait qu'il n'y a plus de descendance � charger */
   protected boolean isReallyLast(ViewSimple v) {
      return false;
   }
   
   protected int draw(TreeMap<String, Source> progen) {
      int nb=0;
      if( pcat==null || !pcat.hasObj() ) return 0;
      Iterator<Obj> it = pcat.iterator();
      while( it.hasNext() ) {
         Source src = (Source)it.next();
         String id = src.id+src.raj+src.dej;
         if( progen.get(id)!=null ) continue;
         progen.put(id,src);
         nb++;
      }
      resetTimer();
      return nb;
   }

}
