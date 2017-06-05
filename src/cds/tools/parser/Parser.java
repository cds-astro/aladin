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

package cds.tools.parser;

import java.text.ParseException;
import java.util.*;
import java.io.*;
import java.lang.reflect.Constructor;

import cds.astro.Unit;

/** Classe Parser
 *  Cette classe permet le "parsing" d'une chaine representant
 *  une expression mathematique pouvant comporter des variables .
 *  Les operateurs possibles sont +, -, *, /, (, ), ^
 *
 *  @version 1.1 Octobre 2007 Amélioration considérable des performances
 *
 */

public final class Parser {
    // différentes fonctions disponibles
    static private HashMap functions;

    // "opérateurs" disponibles
    static private HashMap operators;

    // fonctions disponibles
    static private Function[] AVAIL_FUNC = {
        new Sin(),
        new Cos(),
        new Tan(),
        new Log(),
        new NepLog(),
        new Abs(),
        new Deg2Rad(),
        new Rad2Deg(),
        new Sqrt(),
        new Exp(),
        new Atan()
    };

    // différentes classes des opérateurs
    static private Class[] AVAIL_OP_CLASSES = {
    	SinOp.class, CosOp.class, TanOp.class, LogOp.class, NepLogOp.class, AbsOp.class,
		Deg2RadOp.class, Rad2DegOp.class, SqrtOp.class, ExpOp.class, AtanOp.class,
    };

    static private String[] OP_CLASSES_KW = {
    	"sin", "cos", "tan", "log", "ln", "abs",
		"deg2rad", "rad2deg", "sqrt", "exp", "atan"
    };

    // initialisation statique : correspondance entre noms de fonctions et fonctions
    static {
    	functions = new HashMap(10);
    	operators = new HashMap(10);
    	initFunc();
    	initOperators();
    }

    // contient les differentes variables
//  private HashMap vars;
    private HashMap varsOp;

    // etat lors du parsing de la chaine
    private int state = 0;

    // les operateurs
    private static final int ADD          =  '+';
    private static final int SUBTRACT     =  '-';
    private static final int DIVIDE       =  '/';
    private static final int MULTIPLY     =  '*';
    private static final int POWER        =  '^';
    private static final int GRP        =  '(';
    private static final int ENDGRP        =  ')';

    private static final int UNDERSCORE = '_';
    private static final int OPSQBR = '[';
    private static final int CLSQBR = ']';


    private static final  BasicOperator OP_ADD = new BasicOperator(Parser.precedence(ADD),ADD);
    private static final  BasicOperator OP_MUL = new BasicOperator(Parser.precedence(MULTIPLY),MULTIPLY);
    private static final  BasicOperator OP_DIV = new BasicOperator(Parser.precedence(DIVIDE),DIVIDE);
    private static final  BasicOperator OP_SUB = new BasicOperator(Parser.precedence(SUBTRACT),SUBTRACT);
    private static final  BasicOperator OP_GRP = new BasicOperator(0,GRP);
    private static final  BasicOperator OP_ENDGRP = new BasicOperator(0,ENDGRP);
    private static final  BasicOperator OP_POW = new BasicOperator(Parser.precedence(POWER),POWER);

    // noeud racine de l'arbre
    private  Node root;
    // l'expression mathematique
    private String str;


    /** Constructeur - cree un nouveau parser vide */
    public Parser() {
    	root = null;
//		vars = new HashMap(5);
		varsOp = new HashMap(5);
//		functions= new HashMap(5);
//		initFunc();

		str = "0";
    }

    /** Constructeur - cree un nouveau parser
     *	@param s - l'expression a parser
     */
    public Parser(String s) {
    	root = null;
//    	vars = new HashMap(5);
    	varsOp = new HashMap(5);
//		functions = new HashMap(5);
//		initFunc();

    	if (!s.equals("")) str = new String(s);
    	else str ="0";
    }

	/**
	 * Initialise la hashmap functions en y plaçant les fonctions reconnues
	 */
	static private void initFunc() {
        for( int i=0; i<AVAIL_FUNC.length; i++ )
            addFunc(AVAIL_FUNC[i]);
	}

	/**
	 * Initialise la hashmap operators en y plaçant les opérateurs reconnus
	 */
	static private void initOperators() {
        for( int i=0; i<OP_CLASSES_KW.length; i++ )
            operators.put(OP_CLASSES_KW[i], AVAIL_OP_CLASSES[i]);
	}

    /**
     * Parse la chaine passee en parametre
     * @param s - la chaine a parser
     */
    public void parseString(String s) {
    	this.str = s;
    	parseString();
    }

    static protected String encodeExpr(String myExpr, HashMap vars) {
    	String[] varNames = new String[vars.size()];

    	Iterator itVar = vars.keySet().iterator();

    	int k=0;
    	String tmp;
    	while( itVar.hasNext() ) {
    		tmp = (String)itVar.next();
    		varNames[k] = tmp;
    		k++;
    	}

    	// tri dans l'ordre décroissant de longueur des noms de variables
        for( int i = 0 ; i < varNames.length ; i++ ) {
            tmp = varNames[i];
            for( int j = i+1 ; j < varNames.length ; j++ ) {
                if( varNames[j].length() > tmp.length() ) {
                    // echanger varNames[i] et varNames[j]
                	String tmp2 = tmp;
                	tmp = varNames[j];
                	varNames[j] = tmp2;
                	varNames[i] = tmp;
                }
            }
        }

        String[] encodedVarNames = new String[vars.size()];
        for( int i=0; i<varNames.length; i++ ) {
        	encodedVarNames[i] = encodeVariable(varNames[i]);
        }



//    	for( int i=0; i<varNames.length; i++ ) System.out.println(varNames[i]);

    	return putEncodedVariables(myExpr, varNames, encodedVarNames);
    }

    /**
     *  @param s : the string to parse with unencoded variables
     *  @param vars: array with unencoded variables, already sorted by length of string
     *  @param encodedVars: array with encoded variables, already sorted by length of string
     *  @return : the string to parse with encoded variables
     */
    static private String putEncodedVariables(
        String s,
        String[] vars,
        String[] encodedVars) {
        int begin, end;
//        int i = 0;
        int oend = 0;
        StringBuffer strBuf = new StringBuffer();

        String ret = new String(s);
        for( int k=0; k<vars.length; k++ ) {
        	ret = replace(ret, vars[k], encodedVars[k], -1);
        }

        return ret;

        /*
        for (;;) {
            if (i == vars.length)
                break;
            begin = s.indexOf(vars[i], oend);
            if (begin < 0) {
            	i++;
                continue;
//                break;
            }
            end = begin + vars[i].length();

            //System.out.println(oend+"\t"+begin);
            strBuf.append(s.substring(oend, begin));
            strBuf.append(encodedVars[i]);
            oend = end;
//            i++;
        }

        if (oend < s.length())
            strBuf.append(s.substring(oend, s.length()));

        return strBuf.toString();

        */
    }


    /**
     * Effectue le parsing
     */
    public void parseString() {
    	// l'expression doit etre encodée pour prendre en compte l'encodage des variables

//    	System.out.println(str);
    	str = encodeExpr(str, varsOp);
//    	System.out.println(str);
    	// pile des objets
    	Stack st_ob = new Stack();
    	// pile des operateurs
    	Stack st_op = new Stack();

    	StringReader sr = new StringReader(str);
    	StreamTokenizer tokenizer = new StreamTokenizer(sr);

    	// ajustement du tokenizer
    	tokenizer.parseNumbers();
    	tokenizer.lowerCaseMode(false);
    	tokenizer.ordinaryChar(ADD);
    	tokenizer.ordinaryChar(SUBTRACT);
    	tokenizer.ordinaryChar(MULTIPLY);
    	tokenizer.ordinaryChar(DIVIDE);
    	tokenizer.ordinaryChar(POWER);
    	tokenizer.wordChars(UNDERSCORE,UNDERSCORE);
    	tokenizer.wordChars(OPSQBR,CLSQBR);
    	tokenizer.wordChars('{','{');
    	tokenizer.wordChars('}','}');
        // ajout démo AVO
        tokenizer.wordChars(' ',' ');

    	// for encoding characters :
    	tokenizer.wordChars('@','@');
    	tokenizer.wordChars('!','!');
    	tokenizer.wordChars('~','~');
    	tokenizer.wordChars('&','&');
    	tokenizer.wordChars('`','`');
    	tokenizer.wordChars('\\','\\');

    	tokenizer.wordChars('$','$');

    	// pour support UCD1+
    	tokenizer.wordChars(';',';');
    	// pour les utype-lovers
    	tokenizer.wordChars(':', ':');

    	try {
    	    state = 0;

    	    int type;
    	    while ( (type=tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
    	    	switch (state) {
    	    	    case 0:
    	    	    	parserState0(tokenizer,st_ob,st_op);
    	    	    	break;
    	    	    case 1:
    	    	    	parserState1(tokenizer,st_ob,st_op);
    	    	    	break;
    	    	    case 2:
    	    	    	parserState2(tokenizer,st_ob,st_op);
    	    	    	break;
    	    	    case 3:
    	    	    	break;
    	    	    default :
    	    	    	break;
    	    	}

    	    }

    	    parserState2(tokenizer,st_ob,st_op);
    	    root = (Node) st_ob.elementAt(0);
    	}
    	catch (IOException e) {System.err.println(e);}
    	catch (EmptyStackException e2) {throw new ParserException();}

    	// finalement, on construit l'objet opérateur qui va bien
    	buildOperator();
    }

    AbstractOperateur rootOperator;
    /** construit l'objet (héritant d'AbstractOperateur)
     * qui permettra une évaluation rapide de l'expression
     *
     */
    private void buildOperator() {
    	rootOperator = buildOperator(root);
    }

    /** On présuppose que node ne contient que des variables et fontions connues */
    private AbstractOperateur buildOperator(Node node) {
    	switch(node.type) {
    		case Node.FUNCTION :
    			return getOperateurForFunc(node.svalue, buildOperator(node.left));

    		case Node.OP :
    			return getOperateurForOp(node.op, buildOperator(node.left), buildOperator(node.right));

    		case Node.VAR :
    			return findVarOp(decodeVariable(node.svalue));

    		case Node.VALUE :
    			return new ConstantValOp(node.getValue());

    		default :
    			return null; // should never happen
    	}
    }

    static Class[] FUNC_OPERATOR = {};
    private AbstractOperateur getOperateurForFunc(String func, AbstractOperateur op) {
    	AbstractOperateur returnOp;
    	try {
    		Class c = (Class)operators.get(func);
    		Constructor ct = c.getConstructor(new Class[] {AbstractOperateur.class});
    		returnOp = (AbstractOperateur)ct.newInstance(new Object[] {op});
    	}
    	catch(Exception e) {return null;}// should never happen

    	return returnOp;
    }

    private AbstractOperateur getOperateurForOp(int op, AbstractOperateur op1, AbstractOperateur op2) {
    	switch(op) {
    		case ADD : return new AdditionOp(op1, op2);
    		case SUBTRACT : return new SubtractOp(op1, op2);
    		case DIVIDE : return new DivideOp(op1, op2);
    		case MULTIPLY : return new MultOp(op1, op2);
    		case POWER : return new PowerOp(op1, op2);
			default : return null; // should never happen !
    	}
    }



	public final double eval() {
		// ancienne méthode, moins efficace (moins rapide)
//		return eval(this.root);

		return rootOperator.compute();
	}

	// pour des tests uniquement
//	public final double eval2() {
//		return rootOperator.compute();
//	}

	// ancienne méthode d'évaluation
	public final double oldEval() {
		return eval(this.root);
	}

	/** ajoute une fonction à celles reconnues par le parser
	 *
	 * @param f fonction à ajouter
	 */
	public static void  addFunc(Function f) {
		functions.put(f.keyword(), f);
	}

    /**
     * Ajoute une variable
     * @param name - nom de la variable a ajouter
     */
    public void addVar(String name) {
    	varsOp.put(name, new VariableOp());
    }

	/**
	 *	Fixe l'unite d'une variable
	 *	@param name - nom de la variable
	 *	@param unitStr - unite de la variable
	 *	@return true if success, false otherwise
	 */
	public boolean setVarUnit(String name, String unitStr) {
		VariableOp x = (VariableOp)varsOp.get(name);
    	if (x == null)	throw new ParserException("method setVar : Unknown variable "+name+" !!!");
        // Que faire dans ce cas précis ??
        if( unitStr == null ) unitStr="";
		try {
			Unit u = new Unit(unitStr);
			u.setValue(x.getValue());
			x.setUnit(u);
		}
		catch(java.text.ParseException e) {return false;}
		return true;
	}

    /**
     * Fixe la valeur d'une variable
     * @param name - nom de la variable
     * @param value - valeur de la variable
     */
    public void setVar(String name, double value) {
    	try {
    		VariableOp xo = (VariableOp)varsOp.get(name);
    		xo.setValue(value);
    	}
    	catch(NullPointerException e) {
    		throw new ParserException("method setVar : Unknown variable "+name+" !!!");
    	}
    }

    /** Pour obtenir l'ensemble des variables
     *	@return l'ensemble des variables de ce parser
     */
    public Iterator getVariables() {
    	return varsOp.keySet().iterator();
    }

	/** pour savoir si le parser a une valeur constante, cad si il n'y a pas de variable
	 *	@return true si le parser a une valeur constante
	 */
	public boolean isConstant() {
		return (!getVariables().hasNext());
	}

    /**
     * Retourne la valeur d'une variable
     * @param name - nom de la variable, the keyword of a variable.
     * @return valeur de la variable name
     */
    private double getVar(String name) {
    	VariableOp x = (VariableOp) varsOp.get(name);
    	if (x == null)	throw new ParserException("methode getVar : La variable "+name+" est inconnue!!!");

    	return x.getValue();
    }

    private VariableOp findVarOp(String name) {
    	return (VariableOp)varsOp.get(name);
    }



    // Methodes privees

    /** Retourne l'objet Operator correspondant a l'entier op */
    private static BasicOperator getOp(int op) {
    	switch (op) {
    	    case ADD:
    	    	return OP_ADD;
    	    case SUBTRACT:
    	    	return OP_SUB;
    	    case MULTIPLY:
    	    	return OP_MUL;
    	    case DIVIDE:
    	    	return OP_DIV;
    	    case POWER:
    	    	return OP_POW;
    	    case GRP:
    	    	return OP_GRP;
    	    case ENDGRP:
    	    	return OP_ENDGRP;
    	}

	return null;
    }


    /** Evaluation d'un noeud */
    private double eval(Node node) {
    	double value = 0.;

    	if (node == null) throw new  ParserException("methode eval : noeud null !!!");

    	switch (node.type) {
    	    case Node.OP:
    	    	value = evalOp(node);
    	    	break;
    	    case Node.VALUE:
    	    	value = node.getValue();
    	    	break;
    	    case Node.VAR:
    	    	value = evalVar(node);
    	    	break;
    	    case Node.FUNCTION:
    	    	value = evalFunc(node);
    	    	break;
    	    default:
    	    	throw new ParserException("methode eval : Ce noeud est de type inconnu !!!");
    	}


    	return value;
    }

    private double evalOp(Node node) {
    	double value = 0.0;

    	switch (node.op) {
    	    case ADD:
    	    	if(node.left != null) value = eval(node.left);
    	    	value += eval(node.right);
    	    	break;
    	    case SUBTRACT:
    	    	if(node.right != null) value = eval(node.right);
    	    	value = eval(node.left) - value;
    	    	break;
    	    case DIVIDE:
    	    	value = eval(node.left);
    	    	value /= eval(node.right);
    	    	break;
    	    case MULTIPLY:
    	    	value = eval(node.left);
    	    	value *= eval(node.right);
    	    	break;
    	    case POWER:
    	    	value = Math.pow(eval(node.left),eval(node.right));
    	    	break;
    	    default:
    	    	throw new ParserException("methode evalOp : Operateur inconnu !!!");
    	}
    	return value;
    }

	/** Evaluation de la valeur d'un noeud par une fonction */
	private double evalFunc(Node node) {
		Function f = (Function)functions.get(node.svalue);
		if ( f==null ) throw new ParserException("method evalFunc : la fonction "+node.svalue+" est inconnue !!!");
		return f.eval(eval(node.left));
	}

    /** Evaluation d'une variable */
    private double evalVar(Node node) {
    	VariableOp v = (VariableOp)varsOp.get(decodeVariable(node.svalue));
    	if ( v==null ) throw new ParserException("methode evalVar : la variable "+node.svalue+" est inconnue !!!");
    	return v.getValue();
    }

//////// Methodes permettant l'evaluation de l'unite ////////

	/** Retourne l'évaluation de l'unité complète (valeur+symbole)
	 *  Attention : l'unité de chaque variable doit avoir été fixée avec setVarUnit
	 *  sans quoi le résultat de evalUnit sera inexact
	 */
	public Unit evalUnit() throws java.text.ParseException {
		return this.evalUnit(root);
	}

	private Unit evalUnit(Node node) throws java.text.ParseException {
    	Unit unit = null;

    	if (node == null) throw new ParserException("methode eval : noeud null !!!");

    	switch (node.type) {
    	    case Node.OP:
    	    	unit = evalOpUnit(node);
    	    	break;
    	    case Node.VALUE:
				try {
    	    		unit= new Unit(new Double(node.getValue()).toString());
				}
				catch(java.text.ParseException e) {System.out.println("Error for a VALUE node");throw e;}

    	    	break;
    	    case Node.VAR:
    	    	unit = evalVarUnit(node);
    	    	break;
    	    case Node.FUNCTION:
    	    	unit = evalFuncUnit(node);
    	    	break;
    	    default:
    	    	throw new ParserException("methode eval : Ce noeud est de type inconnu !!!");
		}

		return unit;

	}

	/** Evaluation de l'unité d'une fonction */
	private Unit evalFuncUnit(Node node) throws ParseException{
		Function f = (Function)functions.get(node.svalue);
		if ( f==null ) throw new ParserException("method evalFunc : la fonction "+node.svalue+" est inconnue !!!");
		return f.evalUnit(evalUnit(node.left));
	}

    /** Evaluation de l'unite d'une variable */
    private Unit evalVarUnit(Node node) throws java.text.ParseException {
    	Unit unit;
    	VariableOp x = (VariableOp) varsOp.get(decodeVariable(node.svalue));
    	if (x == null)	throw new ParserException("methode evalVar : la variable "+node.svalue+" est inconnue !!!");
    	unit = new Unit(x.getUnit());
    	return unit;
    }


	private Unit evalOpUnit(Node node) throws java.text.ParseException {
		// TO BE CONTINUED
		Unit unit = null;

    	switch (node.op) {
    	    case ADD:
    	    	if(node.left != null) unit = new Unit(evalUnit(node.left));
    	    	unit.plus(evalUnit(node.right));
    	    	break;
    	    case SUBTRACT:
    	    	if(node.right != null) unit = new Unit(evalUnit(node.right));
                //System.out.println(node.left.valueIsSet());
				// for unary operator minus
                if( !node.left.valueIsSet() ) {
                    unit.setValue(-unit.value);
                }
                else {
                    Unit temp;
                    temp = evalUnit(node.left);
                    temp.minus(unit);
                    unit = temp;
                }
    	    	break;
    	    case DIVIDE:
    	    	unit = new Unit(evalUnit(node.left));
    	    	unit.div(evalUnit(node.right));
    	    	break;
    	    case MULTIPLY:
    	    	unit = new Unit(evalUnit(node.left));
    	    	unit.mult(evalUnit(node.right));
    	    	break;
            // pour ce cas, je ne sais pas bien comment faire (pour les unités)
            case POWER:
                if(node.right.type==Node.VALUE) {

                    double power = eval(node.right);
                    int powerInt = (int)power;
//                    if( Double.parseDouble(powerInt+"")==power ) {
                    if( Double.valueOf(powerInt+"").doubleValue()==power ) {
                        //unit = new Unit(evalOp(node)+"("+evalUnit(node.left).symbol+")"+powerInt);
                        unit = new Unit(evalUnit(node.left));
                        //System.out.println("unit vaut : "+unit);
                        //System.out.println("unit vaut SI : "+unit.inSI());
                        //System.out.println("on power à la puissance "+powerInt);
                        unit.power(powerInt);
                        //System.out.println("unit vaut maintenant : "+unit);
                        //System.out.println("unit vaut maintenant SI : "+unit.inSI()+"\n");
                        //System.out.println(unit);
                    }
                    else unit = new Unit(evalOp(node)+"("+evalUnit(node.left).symbol+")"+eval(node.right));

                    /*
                    int test = (int)eval(node.right);
                    System.out.println(test);
                    Unit orgUnit = new Unit(evalUnit(node.left).symbol);
                    unit = new Unit(orgUnit);
                    System.out.println("unit originale : "+orgUnit);
                    for( int i=1; i<test; i++ ) {
                        System.out.println("toto);
                        unit.mult(orgUnit);
                    }
                    if( test<1 ) {
                        test = (int)(1/eval(node.right));
                        System.out.println("test 2 : "+test);
                        orgUnit = new Unit("1/("+evalUnit(node.left).symbol+")");
                        for( int i=1; i<test; i++ ) unit.mult(orgUnit);
                    }

                    System.out.println("unit : "+unit);
                    */
                    //unit.setValue(Math.pow(evalUnit(node.left).value,eval(node.right)));
                }
				// ce cas la, je sais pas
				else if(node.left.type==Node.VALUE) {
					unit = new Unit(evalUnit(node.left));
				}
				// celui-la encore moins --> a voir
				else {
					unit = new Unit(evalUnit(node.left));
				}
    	    	break;
    	    default:
    	    	throw new ParserException("methode evalOp : Operateur inconnu !!!");
    	}

		return unit;
	}

/////// Fin des methodes pour evaluation de l'unite ////////

    /** Retourne la precedence d'un operateur */
    private static int precedence(int op) {
    	switch (op) {
    	    case ADD:
    	    	return 1;
    	    case SUBTRACT:
    	    	return 2;
    	    case MULTIPLY:
    	    	return 3;
    	    case DIVIDE:
    	    	return 3;
    	    case POWER:
    	    	return 5;
    	}
	return -1;
    }

    /** Traitement lorsque state==0 */
    private void parserState0(StreamTokenizer tokenizer, Stack ob, Stack op) throws IOException {
    	switch (tokenizer.ttype) {
    		// on tombe sur un mot : soit une variable, soit une fonction
    	    case StreamTokenizer.TT_WORD :
    	    	VariableOp x = (VariableOp) varsOp.get(decodeVariable(tokenizer.sval));
    	    	// il s'agit d'une variable
    	    	if (x != null) {
    	    	    Node node = new Node();
    	    	    node.type = Node.VAR;
    	    	    node.svalue = tokenizer.sval;
    	    	    ob.push(node);
    	    	    state = 2;
    	    	}
    	    	// il s'agit d'une fonction
    	    	else {
    	    		Function f = (Function)functions.get(tokenizer.sval);
    	    		if( f!=null ) {
    	    			op.push(f);
    	    			state = 0;
    	    		}
					// le mot n'a pas été reconnu : envoi d'une exception
					else throw new ParserException("Le mot "+tokenizer.sval+" est inconnu !");
    	    	}

    	    	break;

    	    case StreamTokenizer.TT_NUMBER :
    	    	Node node = new Node();
    	    	node.type = Node.VALUE;
    	    	node.setValue(tokenizer.nval);
    	    	ob.push(node);
    	    	state = 2;
    	    	break;

    	    case   ADD :
    	    	op.push(getOp(ADD));
    	    	node = new Node();
    	    	node.type = Node.VALUE;
    	    	//node.setValue(0.);
    	    	ob.push(node);
    	    	state = 1;
    	    	break;

    	    case   SUBTRACT :
    	    	op.push(getOp(SUBTRACT));
    	    	node = new Node();
    	    	node.type = Node.VALUE;
    	    	//node.setValue(0.);
    	    	ob.push(node);
    	    	state = 1;
    	    	break;

    	    case   GRP :
    	    	op.push(getOp(GRP));
    	    	state = 0;
    	    	break;

    	    default:
    	    	throw new ParserException("Le parsing a echoue !!");
    	}
    }

    /** Traitement lorsque state==1 */
    private void parserState1(StreamTokenizer tokenizer, Stack ob, Stack op) throws IOException {
    	switch (tokenizer.ttype) {
    		// on tombe sur un mot : soit une variable, soit une fonction
    	    case StreamTokenizer.TT_WORD :
    	    	VariableOp x = (VariableOp) varsOp.get(decodeVariable(tokenizer.sval));
    	    	if (x != null) {
    	    	    Node node = new Node();
    	    	    node.type = Node.VAR;
    	    	    node.svalue = tokenizer.sval;
    	    	    ob.push(node);
    	    	    state = 2;
    	    	}
    	    	// VERIFIER LES POPS !!
				else {
					Function f = (Function)functions.get(tokenizer.sval);
					if( f!=null ) {
						op.push(f);
						state = 0;
					}
					// le mot n'a pas été reconnu : envoi d'une exception
					else throw new ParserException("Le mot "+tokenizer.sval+" est inconnu !");
				}
    	    	break;

    	    case StreamTokenizer.TT_NUMBER :
    	    	Node node = new Node();
    	    	node.type = Node.VALUE;
    	    	node.setValue(tokenizer.nval);
    	    	ob.push(node);
    	    	state = 2;
    	    	break;

    	    case   GRP :
    	    	op.push(getOp(GRP));
    	    	state = 0;
    	    	break;

    	    default:
    	    	throw new ParserException("Le parsing a echoue !!");
    	}
    }

    /** Traitement lorsque state==2 */
    private void parserState2(StreamTokenizer tokenizer, Stack ob, Stack op) throws IOException {
		BasicOperator op_read;
    	switch (tokenizer.ttype) {
    	    case   ADD: case SUBTRACT : case MULTIPLY: case DIVIDE: case POWER:
    	    	op_read = getOp(tokenizer.ttype);
    	    	if (!op.empty()) {
					Operator tmpi = (Operator)op.peek();
    	    	    while ( tmpi.precedence() >= op_read.precedence() ) {
    	    	    	if ( tmpi.type==Operator.BASIC ) {
							BasicOperator tmp = (BasicOperator)op.pop();
    	    	    	    Node deux = (Node)ob.pop();
    	    	    	    Node un = (Node)ob.pop();
    	    	    	    Node node = new Node();
    	    	    	    node.type = Node.OP;
    	    	    	    node.op = tmp.op;
    	    	    	    node.left = un;
    	    	    	    node.right = deux;

    	    	    	    ob.push(node);
    	    	    	}
    	    	    	else if( tmpi.type==Operator.FUNC ) {
    	    	    		Function tmp = (Function)op.pop();
    	    	    		Node un = (Node)ob.pop();
    	    	    		Node node = new Node();
    	    	    		node.type = Node.FUNCTION;
    	    	    		node.svalue = tmp.keyword();
    	    	    		node.left = un;
    	    	    		node.right = null;

    	    	    		ob.push(node);
    	    	    	}

    	    	    	if (!op.empty()) tmpi = (Operator)op.peek();
    	    	    	else break ;
    	    	    }
    	    	}

    	    	op.push(op_read);
    	    	state = 1;
    	    	break;

    	    	case   ENDGRP : {
    	    	    op_read = getOp(tokenizer.ttype);
    	    	    if (!op.empty()) {
						Operator tmp = (Operator)op.pop();
    	    	    	while ( !tmp.keyword().equals("(") ) {
    	    	    	    if ( tmp.type==Operator.BASIC ) {
								BasicOperator tmpi = (BasicOperator)tmp;
    	    	    	    	Node deux = (Node)ob.pop();
    	    	    	    	Node un = (Node)ob.pop();
    	    	    	    	Node node = new Node();
    	    	    	    	node.type=Node.OP;
    	    	    	    	node.op = tmpi.op;
    	    	    	    	node.left = un;
    	    	    	    	node.right = deux;

    	    	    	    	ob.push(node);
    	    	    	    }
							else if( tmp.type==Operator.FUNC ) {
								Function tmpi = (Function)tmp;
								Node un = (Node)ob.pop();
								Node node = new Node();
								node.type = Node.FUNCTION;
								node.svalue = tmpi.keyword();
								node.left = un;
								node.right = null;

								ob.push(node);
							}

    	    	    	    if (!op.empty()) tmp = (Operator)op.pop();
    	    	    	    else break ;
    	    	    	}
    	    	    }
    	    	    state = 2;
    	    	    break;
    	    	}

    	    	case StreamTokenizer.TT_EOF :
    	    	    while ( !op.empty() ) {
						Operator tmp = (Operator)op.pop();
    	    	    	if ( tmp.type==Operator.BASIC ) {
							BasicOperator tmpi = (BasicOperator)tmp;
    	    	    	    Node deux = (Node)ob.pop();
    	    	    	    Node un = (Node)ob.pop();
    	    	    	    Node node = new Node();
    	    	    	    node.type = Node.OP;
    	    	    	    node.op = tmpi.op;
    	    	    	    node.left = un;
    	    	    	    node.right = deux;

    	    	    	    ob.push(node);
    	    	    	}
						else if( tmp.type==Operator.FUNC ) {
							Function tmpi = (Function)tmp;
							Node un = (Node)ob.pop();
							Node node = new Node();
							node.type = Node.FUNCTION;
							node.svalue = tmpi.keyword();
							node.left = un;
							node.right = null;

							ob.push(node);
						}

    	    	    }
    	    	    state = 0;
    	    	    break;

    	    	default:
    	    	    throw new ParserException("Le parsing a echoue !!");
    	}
    }



    // pour tester
    /*
    public static void main(String[] arg) {


	Parser parser = new Parser("sin(2*x)+y");
	parser.addVar("x");
	parser.addVar("y");
	parser.parseString();

	parser.setVar("x",4.125);
	parser.setVar("y",3);

	System.out.println(parser.eval());

	parser.setVarUnit("x","arcmin");
	parser.setVarUnit("y","arcsec");
	try{
		//System.out.println(parser.evalUnit());
		System.out.println("TOTO" +parser.evalUnit().symbol);
		Unit u = new Unit("2.ms");
		System.out.println(u.symbol);
    }
    catch(Exception e) {e.printStackTrace();}
    }*/



    /** returns available functions names
     * @return String[]
     */
    public static String[] getAvailFunc() {
        String[] func = new String[AVAIL_FUNC.length];
        for( int i=0; i<AVAIL_FUNC.length; i++ )
            func[i] = AVAIL_FUNC[i].keyword();

        return func;
    }

    /**
     * Encode a variable, to avoid confusion with arithmetical symbols
     *
     * The idea is to use characters which are explicitely forbidden in the labels of VizieR columns
     * (see http://vizier.u-strasbg.fr/doc/catstd-3.3.htx)
     * @param s
     * @return
     */
    static protected final String encodeVariable(final String s) {
    	if( s==null ) return null;

    	String ret = new String(s);
    	// opérateurs mathématiques
        ret = ret.replace('+', '@');
        ret = ret.replace('-', '!');
        ret = ret.replace('*', '~');
        ret = ret.replace('/', '&');
        ret = ret.replace('.', '`');
        // parenthèses
        ret = ret.replace('(', '\\');
        ret = ret.replace(')', '^');

        return ret;
    }

    static protected String decodeVariable(final String s) {
    	if( s==null ) return null;

    	String ret = new String(s);
        ret = ret.replace('@', '+');
        ret = ret.replace('!', '-');
        ret = ret.replace('~', '*');
        ret = ret.replace('&', '/');
        ret = ret.replace('`', '.');
        // parenthèses
        ret = ret.replace('\\', '(');
        ret = ret.replace('^', ')');

        return ret;
    }

	/**
	 * @return Returns the expression
	 */
	public String getExpr() {
		return str;
	}

    /**
    *
    * @param text text to search and replace in
    * @param repl String to search for
    * @param with String to replace with
    * @param max Maximum number of values to replace. If -1, replace all occurences of repl
    * @return String the string with replacements processed
    */
   public static String replace(String text, String repl, String with, int max) {
       if (text == null || repl == null || with == null || repl.length() == 0 || max == 0)
           return text;

       StringBuffer buf = new StringBuffer(text.length());
       int start = 0, end = 0;
       while ((end = text.indexOf(repl, start)) != -1) {
           buf.append(text.substring(start, end)).append(with);
           start = end + repl.length();

           if (--max == 0)
               break;
       }
       buf.append(text.substring(start));
       return buf.toString();
   }

}

