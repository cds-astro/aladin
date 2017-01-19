package cds.tools;

import java.util.Stack;

import cds.astro.Coo;
import cds.tools.parser.Exp;

/**
 * Gestion d'une calculatrice d'expressions arithmétiques
 * supportant les fonctions mathématiques classiques.
 * La syntaxe TOPcat équivalente est supportée (mais non documentée)
 * 
 * Il s'agit d'une machine à empilement de registres qui supporte
 * l'ensemble des fonctions java Math classiques ainsi
 * que les opérateurs courants +, -, *, / et moins courants ^, %
 * Les nombres peuvent être exprimés normalement, ou en notation scientifique
 * ou en sexagesimal (séparateur ":" - signe obligatoire pour la déclinaison)
 * Par defaut, le "." est le caractère décimal et la "," le séparateur de paramètres
 *
 * Copyright: 2004-2013, Pierre Fernique
 */
public final class Computer  {
   
   private static boolean DEBUG=false;
   
   static final String [] FCTSPARAM 
      = { "atan2","atan2d","atan2Deg","min","max","dist","skydist","skyDistDegrees",
          "dmsToDegrees","dmsToDegrees" };
   static final    int [] FCTNPARAM 
      = {      2,       2,         2,    2,    2,      4,     4,               4,
                      3,             3 };
   
   static final String [] HELP = {
      "x,+,*,/,%,^: addition, subtraction, multiplication, division, modulo, power",
      "exp(x):   Euler's number 'e' raised to the power of x",
      "ln(x):    the natural logarithm (base 'e') of x",
      "log(x):   the base 10 logarithm of x",
      "sqrt(x):  the correctly rounded positive square root of x",
      "ceil(x):  the largest (closest to positive infinity) integer value",
      "floor(x): the smallest (closest to negative infinity) integer value",
      "round(x): the value of the argument rounded to the nearest integer",
      "abs(x):   the absolute value of the argument",
      "sin(x):   trigonometric sine of an angle (x in radians)",
      "cos(x):   trigonometric cosine of an angle (x in radians)",
      "tan(x):   trigonometric tangent of an angle (x in degrees)",
      "asin(x):  the arc sine of a value (result in radians)",
      "acos(x):  the arc cosine of a value (result in radians)",
      "atan(x):  the arc tangent of a value (result in radians)",
      "sinh(x):  the hyperbolic sine of x",
      "cosh(x):  the hyperbolic cosine of x",
      "tanh(x):  the hyperbolic tangent of x",
      "sind,cosd,tand:   trigonometric functions of an angle (x in degress)",
      "asind,acosd,atad: the arc trigonometric functions (result in degrees)",
      "rad2deg(a):  the measurement of the angle a in radians",
      "deg2rad(a):  the measurement of the angle a in degrees",
      "min(x,y):    the smaller of x and y",
      "max(x,y):    the larger of x and y",
      "atan2(x,y):  the angle (in radians) corresponding to x,y in cartesian coordinates",
      "atan2d(x,y): the angle (in degrees) corresponding to x,y in cartesian coordinates",
      "dist(x1,y1,x2,y2):        cartesian distance between x1,y1 and x2,y2",
      "skydist(ra1,de1,ra2,de2): spherical distance (coord in degrees or sexa with : as separator",
   };
   
   static private char SEP = ',';
   private int step=0;   // Numéro de l'étape (pour le débuging)
   
   // Retourne le nombres de paramètres que la fonction requière
   private int getNbParam(String s) { 
      int i = Util.indexInArrayOf(s, FCTSPARAM);
      return i<0 ? 1 : FCTNPARAM[i];
   }
   
   // Execution de l'opération en haut de la pile
   private boolean operate(Stack<Val> sV,Stack<Op> sO) throws Exception {
      if( sO.size()==0 || sV.size()==0 ) return false;
      Op op = sO.peek();
      step++;
      Val v;
      if( op.nbParam==1 ) {
         v = sV.peek();
         if( DEBUG ) System.out.print(step+")\t"+op.op+"("+v+")");
         
              if( op.op.equals("-") )         v.v = -v.v;
         else if( op.op.equals("+") )         v.v = v.v;
         else if( op.op.equals("exp") )       v.v = Math.exp(v.v);
         else if( op.op.equals("sin") )       v.v = Math.sin(v.v);
         else if( op.op.equals("sind")
               || op.op.equals("sinDeg") )    v.v = Math.sin(Math.toRadians(v.v));
         else if( op.op.equals("cos") )       v.v = Math.cos(v.v);
         else if( op.op.equals("cosd")
               || op.op.equals("cosDeg") )    v.v = Math.cos(Math.toRadians(v.v));
         else if( op.op.equals("tan") )       v.v = Math.tan(v.v);
         else if( op.op.equals("tand")
               || op.op.equals("tanDeg") )    v.v = Math.tan(Math.toRadians(v.v));
         else if( op.op.equals("asin") )      v.v = Math.asin(v.v);
         else if( op.op.equals("asind")
               || op.op.equals("asinDeg") )   v.v = Math.toDegrees(Math.asin(v.v));
         else if( op.op.equals("acos") )      v.v = Math.acos(v.v);
         else if( op.op.equals("acosd")
               || op.op.equals("acosDeg") )   v.v = Math.toDegrees(Math.acos(v.v));
         else if( op.op.equals("atan") )      v.v = Math.atan(v.v);
         else if( op.op.equals("atand")
               || op.op.equals("atanDeg") )   v.v = Math.toDegrees(Math.atan(v.v));
         else if( op.op.equals("sinh") )      v.v = Math.sinh(v.v);
         else if( op.op.equals("cosh") )      v.v = Math.cosh(v.v);
         else if( op.op.equals("tanh") )      v.v = Math.tanh(v.v);
         else if( op.op.equals("sqrt") )      v.v = Math.sqrt(v.v);
         else if( op.op.equals("ln") )        v.v = Math.log(v.v);
         else if( op.op.equals("log1p") )     v.v = Math.log1p(v.v);
         else if( op.op.equals("log") )       v.v = Math.log10(v.v);
         else if( op.op.equals("sqrt") )      v.v = Math.sqrt(v.v);
         else if( op.op.equals("round") )     v.v = Math.round(v.v);
         else if( op.op.equals("ceil") )      v.v = Math.ceil(v.v);
         else if( op.op.equals("floor") )     v.v = Math.floor(v.v);
         else if( op.op.equals("rint") )      v.v = Math.rint(v.v);
         else if( op.op.equals("abs") )       v.v = Math.abs(v.v);
         else if( op.op.equals("toRadians") 
               || op.op.equals("deg2rad") )   v.v = Math.toRadians(v.v);
         else if( op.op.equals("toDegrees")
               || op.op.equals("rad2deg") ) v.v = Math.toDegrees(v.v);
         else throw new Exception("Unknown function ["+op.op+"]");

      } else {
         if( sV.size()<op.nbParam ) return false;
         Val [] vx = new Val[op.nbParam-1];
         for( int i=vx.length-1 ; i>=0; i-- ) vx[i]=sV.pop();
         v = sV.peek();
         if( DEBUG ) {
            if( isUniqCharOp(op.op.charAt(0)) ) System.out.print(step+")\t"+v+" "+op.op+" "+vx[0]);
            else {
               System.out.print(step+")\t"+op.op+"("+v.v);
               for( Val v1 : vx ) System.out.print(", "+v1.v);
               System.out.print(")");
            }
         }
              if( op.op.equals("-") )        v.v -= vx[0].v;
         else if( op.op.equals("+") )        v.v += vx[0].v;
         else if( op.op.equals("*") )        v.v *= vx[0].v;
         else if( op.op.equals("/") )        v.v /= vx[0].v;
         else if( op.op.equals("%") )        v.v =  v.v % vx[0].v;
         else if( op.op.equals("^") )        v.v = Math.pow(v.v,vx[0].v);
         else if( op.op.equals("max") )      v.v = Math.max(v.v,vx[0].v);
         else if( op.op.equals("min") )      v.v = Math.min(v.v,vx[0].v);
         else if( op.op.equals("atan2") )    v.v = Math.atan2(v.v,vx[0].v);
         else if( op.op.equals("atan2d")
               || op.op.equals("atan2Deg") ) v.v = Math.atan2(Math.toRadians(v.v),Math.toRadians(vx[0].v));
         else if( op.op.equals("dist") )    v.v = Math.hypot(v.v-vx[1].v,vx[0].v-vx[2].v);
         else if( op.op.equals("skydist")
               || op.op.equals("skyDistDegrees") )  v.v = Coo.distance(v.v,vx[0].v,vx[1].v,vx[2].v);
         else if( op.op.equals("hms2Degrees") )  v.v = 15*(v.v + vx[0].v/60 + vx[1].v/3600);
         else if( op.op.equals("dms2Degrees") )  v.v = v.v + vx[0].v/60 + vx[1].v/3600;
         else throw new Exception("Unknown function ["+op.op+"]");
         
      }
      
      if( DEBUG ) System.out.println(" => "+v.v);
      sO.pop();
      return true;
   }
   
   // Retourne vrai tant qu'on est dans un nombre (notation scientifique supportée)
   // La virgule à la française est supportée uniquement si le caractère
   // utilisé comme séparateur des paramètres des fonctions n'est pas la virgule
   private boolean isNumber(Expr e,int deb) {
      char ch1,ch = e.ch();
      if( Character.isDigit(ch) || ch=='.' ) return true;
      if( e.pos>deb && ch==':' ) return true;  // nombre sexa
      if( SEP!=',' && ch==',' ) return true;
      if( e.pos>deb && !e.isEnd() ) {
         if( (ch=='E' || ch=='e') && ((ch1=e.s[e.pos+1])=='+' || ch1=='-' || Character.isDigit(ch1)) ) return true;
         if( (ch=='+' || ch=='-' || Character.isDigit(ch)) && ((ch1=e.s[e.pos-1])=='E' || ch1=='e') ) return true;
      }
      return false;
   }
   
   // Retourne vrai tant qu'on est dans un nom de fonction
   private boolean isInFctName(Expr e,int deb) {
      char ch = e.ch();
      return   e.pos==deb && !Character.isDigit(ch) && ch!='(' && ch!=')'
            || e.pos>deb  && Character.isJavaIdentifierPart(ch) ;
   }
   
   // Retourne vrai s'il s'agit d'une opération sur un seul caractère
   private boolean isUniqCharOp(char ch) {
      return ch=='+' || ch=='-' || ch=='*' || ch=='/' || ch=='^' || ch=='%';
   }
   
   // Retourne la prochaine opération mais sans avancer dans l'expression
   private Op seeNextOp(Expr e) {
      int oPos = e.pos;
      Op op = getNextOp(e);
      e.pos = oPos;
      return op;
   }
   
   // Retourne la prochaine opération
   private Op getNextOp(Expr e) {
      e.skipBlank();
      if( e.ch()=='(' ) return null;
      int deb = e.pos;
      if( !e.isEnd() && isUniqCharOp( e.ch() ) ) e.pos++;
      else while( !e.isEnd() && isInFctName(e,deb) ) e.pos++;
      if( deb==e.pos ) return null;
      Op op = new Op( e.get(deb,e.pos-deb), 2);
      return op;
   }
   
   // Retourne la prochaine fonction
   private Op getNextFct(Expr e) {
      Op op = getNextOp(e);
      if( op==null ) return null;
      op.nbParam= getNbParam(op.op);
      return op;
   }
   
   // Extraction d'une sous-chaine entre parenthèses (sans les parenthèses)
   private String getSubExpr(Expr e) throws Exception {
      int deb = e.pos;
      int par = 0;
      if( e.ch()!='(' ) throw new Exception("Missing '(' : "+e.error());
      while( !e.isEnd() ) {
         char ch= e.ch();
         e.pos++;
         if( ch==')' ) par--;
         else if( ch=='(' ) par++;
         if( par==0 ) break;
      }
      if( par>0 ) throw new Exception("Unbalanced parenthesis :"+e.error());
      return e.get(deb+1,e.pos-deb-2);
   }
   
   // Extraction de la prochaine opérande
   private Val getNextVal(Expr e) throws Exception {
      e.skipBlank();
      
      // Appel récursif car l'opérande est en fait une expression
      if( !e.isEnd() && e.ch()=='(' )  {
         String s =  getSubExpr(e);
         double x = computeInternal( s );
         return new Val( x );
      }
      
      // Extraction de l'opérande
      int deb = e.pos;
      while( !e.isEnd() && isNumber(e,deb) ) e.pos++;
      if( deb==e.pos ) return null;
      return  new Val( e.get(deb,e.pos-deb) );
   }
   
   // Extraction du paramètre courant dans une liste de paramètres
   private Val getNextParam(Expr e) throws Exception {
      e.skipBlank();
      int par=0;
      int deb=e.pos;
      char ch=' ';
      while ( !e.isEnd() ) {
         ch = e.ch();
         e.pos++;
         if( ch=='(' ) par++;
         else if( ch==')' ) par--;
         if( par==0 && ch==SEP ) break;
      }
      if( par>0 ) throw new Exception("Unbalanced parenthesis :"+e.error());
      String s = e.get(deb,e.pos-deb-(ch==SEP?1:0));
      s = sexa2deg(s);
      return new Val( computeInternal( s ) );
   }
   
   // Empilement des paramètres d'une fonction, puis exécution de cette fonction
   private void stackAndComputeParam(Stack<Val> sV,Stack<Op> sF,Expr e) 
   throws Exception {
      int n = sF.peek().nbParam;
      e.skipBlank();
      Expr e1 =  new Expr( getSubExpr(e) );
      for( int i=0; i<n; i++ ) {
         Val v = getNextParam(e1);
         if( v==null ) throw new Exception("Missing parameter :"+e.error());
         sV.push(v);
      }
      if( !e1.isEnd() ) throw new Exception("Unknown parameter: "+e1.error());
      operate(sV,sF);
   }
   
   // Lecture de l'opérande courante, éventuellement précédée d'une ou plusieurs
   // fonctions, puis traitement des opérations possibles en fonction
   // de l'état des piles et de la priorité de l'opérateur qui suit
   private void read(Stack<Val> sV,Stack<Op> sO,Stack<Op> sF, Expr e) throws Exception {
      
      // Lecture d'éventuels "-" et "+" en préfixe
      // On utilise la pile à fonctions
      Op f;
      while( (f = getNextFct(e))!=null && isUniqCharOp(f.op.charAt(0)) ) {
         sF.push(f);
         f=null;   // mis à null pour indiquer qu'il faudra les traiter à posteriori
      }
      
      // Traitement d'éventuelles fonctions cos,sin,tan2,...
      if( f!=null ) {
         sF.push(f);
         stackAndComputeParam(sV, sF, e);
         
      // Lecture d'une opérande
      } else { 
         Val val = getNextVal(e);
         if( val==null ) throw new Exception("Missing number :"+e.error());
         sV.push(val);
      }
      
      // Traitement des "+" et "-" en préfixe
      while( operate(sV,sF) );

      // Exécution de toutes les opérations en attente dans la pile
      // en fonction de la priorité du prochain opérateur
      Op op = seeNextOp(e);
      while( !sO.isEmpty() && (op==null || op.pri<=sO.peek().pri) ) {
         if( !operate(sV,sO) ) break;
      }
   }
   
   // Lit un opérateur binaire et l'opérande suivante
   private void readNext(Stack<Val> sV,Stack<Op> sO,Stack<Op> sF, Expr e) throws Exception {
      
      e.skipBlank();
      if( e.isEnd() ) return;
      
      Op op = getNextOp(e);
      if( op==null ) throw new Exception("Missing operation :"+e.error());
      sO.push(op);
      
      read(sV,sO,sF,e);
   }
   
   // Lit la première opérande
   private void readFirst(Stack<Val> sV,Stack<Op> sO,Stack<Op> sF, Expr e) throws Exception {
      read(sV,sO,sF,e);
   }
   
   // Conversion d'un nombre sexagesimal en degrés si nécessaire
   // Syntaxe reconnue : HH:MM:SS ou +DD:MM:SS
   private String sexa2deg(String s) throws Exception {
      if( s.length()<5 || s.indexOf(':')<=0) return s;
      for( int i=0; i<s.length(); i++ ) {
         char ch=s.charAt(i);
         if( !Character.isDigit(ch) && ch!='+' && ch!='-' && ch!=':' && ch!='.' ) return s;
      }
      int deb=0,fin;
      double f=15;
      if( s.charAt(0)=='-' ) { f=-1; deb++; }
      else if( s.charAt(0)=='+' ) { f=1;  deb++; }
      fin=s.indexOf(':',deb);
      double a1 = Double.parseDouble( s.substring(deb,fin));
      deb=fin+1; fin=s.indexOf(':',deb);
      double a2 = Double.parseDouble( s.substring(deb,fin));
      deb=fin+1;
      double a3 = Double.parseDouble( s.substring(deb));
      String d = f*(a1 + a2/60 + a3/3600)+"";
      if( DEBUG ) System.out.println((++step)+")\t"+s+" => "+d);
      return d;
   }
   
   /** Compute a mathematical expression
    * @param expression math. expression
    * @return resulting value
    * @throws Exception
    */
   public double computeInternal(String expression) throws Exception {
      Stack<Val> sV = new Stack<Val>();
      Stack<Op>  sO = new Stack<Op>();
      Stack<Op>  sF = new Stack<Op>();
      Expr expr = new Expr(expression);
      
      readFirst(sV,sO,sF,expr);
      while( !expr.isEnd() ) readNext(sV,sO,sF,expr);
      
      if( !expr.isEnd() || sV.size()>1 || sO.size()>0 || sF.size()>0 ) 
         throw new Exception("Truncated expression :"+expr.error());
      
      return sV.pop().v;
   }
   
   
   // Pour la mémorisation d'une valeur réelle
   class Val {
      double v;  // La valeur à mémoriser
      Val( double v ) { this.v = v; }
      Val( String v ) { this.v=Double.parseDouble(v); }
      public String toString() { return v+""; }
   }
   
   // Pour la mémorisation d'une opération
   class Op {
      String op;   // L'opérateur ou la fonction
      int nbParam; // Le nombre d'opérandes/paramètres requis
      int pri;     // Le niveau de priorité
      
      Op( String op, int nbParam) {
         this.op=op; 
         this.nbParam=nbParam; 
         pri=0;
         char ch=op.charAt(0);
         if( ch=='*' || ch=='/' || ch=='%' ) pri=1;
         else if( ch=='^' ) pri=2;
      }
   }
   
   // Pour le traitement de l'expression en cours
   class Expr {
      char [] s;   // La chaine à traiter
      int pos;     // La position courante
      
      // Initialisation de l'expression à mémoriser
      Expr(String expr) { s=expr.toCharArray(); pos=0; }
      
      // Retourne true si la position courante est en fin de chaine
      boolean isEnd() { return pos>=s.length; }
      
      // Retourne le caractère courant ou 0 si fin de chaine
      char ch() { return isEnd() ? 0 : s[pos]; }
      
      // Retourne la sous-chaine indiquées
      String get(int deb,int count) { return new String(s,deb,count); }
      
      // Affiche la chaine en indiquant l'emplacement courant
      String error() { return new String(s,0,pos)+" ?? "+new String(s,pos,s.length-pos); }
      
      // Passe les blancs
      void skipBlank() {
         while( pos<s.length && Character.isSpaceChar( s[pos] ) ) pos++;
      }
   }
   
   /** Return a short help about supported functions */
   static public String help() {
      StringBuilder s = new StringBuilder();
      for( String s1 : HELP ) s.append(s1+"\n");
      return s.toString();
   }
   
   /**
    *  Switch On or off the debug mode
    * @param flag
    */
   static public void setDebug(boolean flag) { DEBUG=flag; }
   
   /** Specify the character used as function parameter separator.
    * By default ',', choose another one (';') for supporting ',' as decimal separator
    * @param sep
    */
   static public void setSeparator(char sep) { SEP=sep; }
   
   /**
    * Compute the arithmetic expression
    * @param s The expression to compute
    * @return The result
    */
   static public double compute(String s) throws Exception {
      if( DEBUG ) System.out.println("Computing:\n"+s);
      return (new Computer()).computeInternal(s);
    }
   
   
   // Juste pour tester
   static public void main(String argv[]) {
      try {
         String s = " -skydist(05:34:43.68,+21:59:28.1,184.50849,-05.79883)*-60 " +
         		"-sin ( -round( 100+1e-03)%(3*2) + -cos(32.2+8^(7-5*max(3,1/8.7) )  )) * -(6-2E+5)";
//         s = "1.23456789E10*2";
         Computer.setDebug(true);
         double x = Computer.compute(s);
         System.out.println(s+" = "+x);
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
}