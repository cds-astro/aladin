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

import cds.tools.Util;

/**
 * Un Simple Tokenizer g�rant les " et les '
 * @author  P.Fernique [CDS]
 */
public final class Tok {
   private char a[];	// Le string en cours d'analyse
   private int i;       // La position courante de l'analyse
   private char c;      // La quote courante
   private String separator;  // Liste des s�parateurs, isSpace par d�faut
 
   
   public Tok(String s) { this(s,null); }
   public Tok(String s,String separator) {
      this.separator = separator;
      a = s.toCharArray();
      i=0;
   }
   
   /** Quote la chaine si c'est n�cessaire */
   static public String quote(String s) {
      char a[] = s.toCharArray();
      for( int i=0; i<a.length; i++ ) {
         if( Character.isSpace(a[i]) || a[i]==',' ) return "\""+s+"\"";
      }
      return s;
   }
   
   /** Unquote la chaine si n�cessaire */
   private String unQuote(StringBuffer s) { return unQuote(s.toString()); }
   
   /** Unquote la chaine si n�cessaire */
   static public String unQuote(String s) {
      int n;
      if( (n=s.length())<2 ) return s;
      char c = s.charAt(0);
      
      if( (c=='\"' || c=='\'') && c==s.charAt(n-1) ) return s.substring(1,n-1);
      return s;
   }
   
   /** Compte le nombre de tokens restants */
   public  int countTokens() {
      int memoI = i;
      int j=0;
      while( nextToken().length()>0 ) j++;
      i=memoI;
      return j;
   }
   
   /** Retourne sous forme d'un tableau de chaines tous les
    * tokens restants    */
   public  String [] getStrings() {
      String s [] = new String[countTokens()];
      for( int i=0; i<s.length; i++ ) s[i]=nextToken();
      return s;
   }
   
   /** Retourne vrai si il y encore quelque chose � retourner */
   public  boolean hasMoreTokens() { return i<a.length; }
   
   /** Retourne la position courante */
   public  int getPos() { return i; }
   
   private boolean isSeparator(char c) {
      if( separator==null ) return Character.isSpace(c);
      return separator.indexOf(c)>=0; 
   }
   
   
   private StringBuffer curTok = new StringBuffer();
   
   /** Retourne le prochain Token, soit s�par� par un des d�limiteurs sp�cifi�s
    * soit d�limit� par des "" ou des '' */
   public  String nextToken() {
      Util.resetString(curTok);
      boolean quote=false;
      
      for( ; i<a.length; i++) {
         if( a[i]=='"' || a[i]=='\'') {
            if( !quote ) { c=a[i]; quote=true; continue;}
            else if( a[i]==c ) { quote=false; continue; }
         }
// if( a[i]=='"' ) { quote=!quote; continue; }
         if( !quote && isSeparator(a[i]) ) {
            while( isSeparator(a[++i]) );
            return unQuote(curTok);
         }
         curTok.append(a[i]);
      }
      return unQuote(curTok);
   }
}
