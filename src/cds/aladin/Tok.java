// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

/**
 * Un Simple Tokenizer gérant les " et les '
 * @author  P.Fernique [CDS]
 */
public final class Tok {
   private char a[];	// Le string en cours d'analyse
   private int i;       // La position courante de l'analyse
   private char c;      // La quote courante
   private String separator;  // Liste des séparateurs, isSpace par défaut
 
   
   public Tok(String s) { this(s,null); }
   public Tok(String s,String separator) {
      this.separator = separator;
      a = s.toCharArray();
      i=0;
   }
   
   /** Quote la chaine si c'est nécessaire (et backquote les " internes) */
   static public String quote(String s) { return quote(s,false); }
   
   /** Quote la chaine (et backquote les " internes) */
   static public String quote(String s,boolean force) {
      int i;
      char a[] = s.toCharArray();
      if( !force ) {
         for( i=0; i<a.length && !Character.isSpace(a[i]) && a[i]!=',' 
               && a[i]!='&' && a[i]!='|' && a[i]!=')' && a[i]!='(' && a[i]!='\\' && a[i]!='\'' && a[i]!='"'; i++ );
         if( i==a.length ) return s;
      }

      StringBuilder s1 = new StringBuilder(a.length);
      s1.append('"');
      for( i=0; i<a.length; i++ ) {
         if( a[i]=='"' || a[i]=='\\' ) s1.append('\\');
         s1.append(a[i]);
      }
      s1.append('"');
      return s1.toString();
   }
   
   /** Unquote la chaine si nécessaire */
   private String unQuote(StringBuffer s) { return unQuote(s.toString()); }
   
   /** Unquote la chaine si nécessaire */
   static public String unQuote(String s) {
      int n;
      if( (n=s.length())<2 ) return s;
      char c = s.charAt(0);
      
      if( !( (c=='\"' || c=='\'') && c==s.charAt(n-1) )) return s;
      char [] a = s.toCharArray();
      StringBuffer s1 = new StringBuffer(a.length);
      boolean backslash=false;
      for( int i=1; i<n-1; i++ ) {
         c=a[i];
         if( backslash && (c=='"' || c=='\'' ||c=='\\') ) s1.replace(s1.length()-1,s1.length(),c+"");
         else s1.append(c);
         backslash= c=='\\';
      }
      return s1.toString();
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
   
   /** Retourne vrai si il y encore quelque chose à retourner */
   public  boolean hasMoreTokens() { return i<a.length; }
   
   /** Retourne la position courante */
   public  int getPos() { return i; }
   
   
   private char lastSeparator=0;
   
   private boolean isSeparator(char c) {
      boolean rep=false;
      if( separator==null )  rep = Character.isSpace(c);
      else rep = separator.indexOf(c)>=0;
      if( rep ) lastSeparator=c;
      return rep;
   }
   
   public char getLastSeparator() { return lastSeparator; } 
   
   private StringBuffer curTok = new StringBuffer();
   
   private void resetString( StringBuffer s ) {
      int n = s.length();
      if( n==0 ) return;
      s.delete(0,n);
   }
   
   /** Retourne le prochain Token, soit séparé par un des délimiteurs spécifiés
    * soit délimité par des "" ou des '' */
   public  String nextToken() {
      resetString(curTok);
      boolean quote=false;
      boolean backslash=false;
      boolean first=true;
      
      for( ; i<a.length; i++) {
         if( !backslash && (a[i]=='"' || a[i]=='\'') ) {
            if( !quote && first) { c=a[i]; quote=true; }
            else if( a[i]==c ) quote=false;
         }
         backslash = a[i]=='\\';
// if( a[i]=='"' ) { quote=!quote; continue; }
         if( !quote && isSeparator(a[i]) ) {
            while( ++i<a.length && isSeparator(a[i]) );
            return unQuote(curTok);
         }
         first=false;
         curTok.append(a[i]);
      }
      return unQuote(curTok);
   }
   
   
   
   static private String TEST = "global color=green dashlist=8 3 width=1 font=\"helvetica 10 normal\" select=1 file=\"\\Root\\file\"";
   static private String TEST1 = "global(color=green,dashlist=8,3,width=1,\"font=\\\"helvetica 10 normal\\\"\",select=1)";
   static private String TEST2 = "box(83.468685,22.0908,163.33361\",367.7002\",96.508724)";
   static public void main(String [] argv) {
//      Tok tok = new Tok("\"font=\\\"helvetica 10 normal\\\"\"  ,  bidule","(, )");
//      while( tok.hasMoreTokens() ) {
//         System.out.println("["+tok.nextToken()+"]");
//      }
      
      System.out.println("==> "+TEST2);
      Tok tok = new Tok(TEST2,"( ,)");
      StringBuffer s = new StringBuffer(tok.nextToken()+"(");
      boolean first=true;
      while( tok.hasMoreTokens() ) {
         if( !first ) s.append(',');
         first=false;
         String p1 = tok.nextToken();
         String p2 = Tok.quote(p1);
         System.out.println(p1+" ==> "+ p2);
         s.append(p2);
      }
      s.append(')');
      System.out.println("==> "+s);
   }
}
