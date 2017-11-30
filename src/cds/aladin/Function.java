// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

/** Gestion d'une fonction script
 * Une fonction Aladin suit la syntaxe suivante
 *    function NAME($PARAM,$PARAM,...) {
 *       CODE
 *    }
 *    
 * Rq: Les paramètres sont facultatifs (parenthèses comprises)
 * */
public class Function {
   private StringBuffer name  = new StringBuffer();
   private StringBuffer param = new StringBuffer();
   private StringBuffer code  = new StringBuffer();
   private StringBuffer description  = new StringBuffer();
   private boolean localDefinition=false;
   private boolean bookmark = false;
   private int nbParam=-1;
   private int etat=0;
   private int nbAcc=0;
   private boolean NL=true;
   private boolean modif=false;
   
   public Function() {}
   
   public Function(String name, String param, String code, String description) {
      setName(name);
      setParam(param);
      setCode(code);
      setDescription(description);
   }
   
   public Function(String s ) throws Exception {
      if( !parseFunction(s) ) throw new Exception("Function truncated ["+s+"]");
   }
   
   public String getName() { return name.toString().trim(); }
   public String getCode() { return code.toString(); }
   public String getParam() { return param.toString(); }
   public String getDescription() { return description.toString(); }
   
   public void setName(String name)   { this.name =new StringBuffer(name==null?"":name); modif=true; }
   public void setCode(String code)   { this.code = new StringBuffer(code==null?"":code); modif=true; }
   public void setParam(String param) { this.param = new StringBuffer(param==null?"":param); nbParam=-1; modif=true; }
   public void setDescription(String description) { this.description = new StringBuffer(description==null?"":description); modif=true; }
   
   /** Indique que cette fonction a été définie localement par l'utilisateur */
   public void setLocalDefinition(boolean flag) { localDefinition=flag; }
   
   /** Indique que cette fonction doit être utilsée comme un bookmark */
   public void setBookmark(boolean flag) { bookmark=flag; }
   
   /** retourne true s'il s'agit d'une fonction définie localement par l'utilisateur */
   public boolean isLocalDefinition() { return localDefinition; }
   
   /** retourne true s'il s'agit d'une fonction bookmarkr */
   public boolean isBookmark() { return bookmark; }
   
   /** Positionne le drapeau de modification de la fonction */
   public void setModif(boolean flag) { modif=flag; }
   
   /** Retourne true si la fonction a été modifiée après sa première définition */
   public boolean hasBeenModif() { return modif; }
   
   /** Retourne le nombre de paramètres de la fonction */
   public int getNbParam() {
      if( nbParam<0 ) {
         if( param.toString().trim().length()==0 ) nbParam=0;
         else {
            nbParam=1;
            for( int i=param.length()-1; i>=0; i-- ) {
               if( param.charAt(i)==',' ) nbParam++;
            }
         }
      }
      return nbParam;
   }
   
   /** Retourne true si la fonction est complète et prête à être exécuter */
   public boolean isOk() { return etat==6; }
   
   /** Retourne la fonction sous sa forme éditée */
   public String toString() { return toString("\n"); }
   public String toString(String CR) {
      StringBuffer s = new StringBuffer(description.length()>0 ? "#"+description+CR:"");
      s.append("function "+getName());
      if( getNbParam()>0 ) s.append("("+param+")");
      s.append(" {"+CR);
      String code = getCode();
      if( !CR.equals("\n") ) code = code.replaceAll("\n",CR);
      s.append(code);
      s.append(CR+"}"+CR);
      return s.toString();
   }
   
   /** Execution de la fonction avec les paramètres passés en ligne de commande
    * Attention aux cas $TARGET et $RADIUS => voir targetRadiusSpecialCase()
    * @param aladin
    * @param param paramètres éventuelles de la fonction
    * @param flagLot Exécution en mode "lot" (garantie la séquentialité, mais pas l'autosync global
    */
   public String exec(Aladin aladin,String param,boolean flagLot) throws Exception {
      String codeWithParam = getCodeWithParam(aladin,param);
      if( !flagLot ) aladin.execAsyncCommand(codeWithParam);
      else aladin.console.addLot(codeWithParam);
//      else aladin.execCommand(codeWithParam);
      return "";
   }
   
   // Retourne le code à exécuter en ayant substitué les noms de variables par leurs valeurs
   private String getCodeWithParam(Aladin aladin,String param) throws Exception {
      String code = getCode();
      Tok p = new Tok(getParam(),", ");      // parcours des templates de paramètres
      Tok v = new Tok(param,", ");           // parcours parallèle des valeurs pour les paramètres
      while( p.hasMoreTokens() ) {
         String p1 = p.nextToken();
         String v1 = v.nextToken();
         v1 = targetRadiusSpecialCase(aladin,p1,v1);
//         System.out.println("Substitution ["+p1+"] par ["+v1+"]");
         code = code.replaceAll("\\"+p1, v1);      // substitution des paramètres par leur valeur
      }
      return code;
   }
   
   // Si la variable est égale à $TARGET ou $RADIUS et que la valeur est vide,
   // on remplace la valeur par le target par défaut, respectivement le rayon par défaut
   // sinon on retourne la valeur telle que.
   private String targetRadiusSpecialCase(Aladin aladin,String p1,String v1) {
      if( v1.length()==0 ) {
         if( p1.equals("$TARGET") ) return getTarget(aladin);
         else if( p1.equals("$RADIUS") ) return getRadius(aladin);
      }
      return v1;
   }
   
   // Retourne le target courant 
   protected String getTarget(Aladin aladin) {
      String target="";
      try {
         target = aladin.localisation.getTextSaisie().trim();
         if( target.length()==0 || target.equals(aladin.GETOBJ)
               || aladin.command.isCommand((new Tok(target)).nextToken()) ) target = aladin.view.getCurrentView().getCentre();
      } catch( Exception e ) {}
      return target;
   }
   
   // Retourne le radius courant
   protected String getRadius(Aladin aladin) {
      String radius="";
      try {
         double r = Util.round(aladin.view.getCurrentView().getTaille() * 60,1);
//         if( r>30*60 ) r=30*60;
         radius = r+"'";
      } catch( Exception e ) { }
      return radius;
   }
   
   /** Parse le code de la fonction, éventuellement en plusieurs fois,
    * Retourne true lorsque la fonction est complète */
   protected boolean parseFunction(String s) throws Exception {
      modif=true;
      char [] a = s.toCharArray();
      for( int i=0; i<a.length; i++ ) {
         char ch = a[i];
         if( ch=='\r' ) continue;
         if( etat!=5 && etat!=-1 && ch=='\n' ) ch=' ';
//         System.out.print(" "+ch+"/"+etat);
         switch(etat) {
            case -1: // Commentaire avant la fonction ?
              if( ch=='\n' || i==a.length-1 ) etat=0;
              if( ch!='\n' ) description.append(ch);
              break;
            case 0: // Recherche en début de ligne du mot clé "function" (ou d'un commentaire)
              if( Character.isSpaceChar(ch) ) break;
              if( ch=='#' ) { etat=-1; break; }
              if( !s.substring(i,i+8).equals("function") ) throw new Exception("Function syntax error ["+s+"]");
              etat=1; i+=7;
              break;
            case 1: // avant le nom de la fonction
              if( Character.isSpaceChar(ch) ) break;
              etat=2;
            case 2: // dans le nom de la fonction
               if( ch=='(' ) etat=4;
               else if( ch=='{' ) { etat=5; nbAcc=1; }
               else name.append(ch);
               break;
            case 3: // avant les paramètres ou l'accolage ouvrante
               if( Character.isSpaceChar(ch) ) break;
               if( ch=='(' ) etat=4;
               else if( ch=='{' ) { etat=5; nbAcc=1; }
               else throw new Exception("function syntax error ["+s+"]");
               break;
            case 4: // Dans les paramètres
               if( ch==')' ) etat=3;
               else param.append(ch);
               break;
            case 5: // Dans le code
               if( ch=='{' ) nbAcc++;
               else if( ch=='}' ) nbAcc--;
               if( nbAcc==0 ) { 
                  etat=6;
                  if( NL && code.length()>0 ) code.delete(code.length()-1,code.length());
                  return true;
               }
               if( NL && (ch=='\n' || ch==' ')) break;
               if( NL ) code.append("   ");
               NL=(ch=='\n');
               code.append(ch);
               if( i==a.length-1 && !NL ) { code.append('\n'); NL=true; }
               break;
         }
      }
      
      // Petit patch de compatibilité => pourra être viré lorsqu'il n'y aura
      // plus de bookmarks avec le server keyword "get allsky ..."
      int j=code.indexOf("get allsky");
      if( j>0 ) code = code.replace(j+4, j+10, "hips");
      
//      System.out.println();
      return false;
   }
}
