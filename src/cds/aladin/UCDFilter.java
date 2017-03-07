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

import java.text.ParseException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import cds.astro.Unit;
import cds.tools.Util;
import cds.tools.parser.Parser;
import cds.tools.parser.ParserException;

/**
 * Gestion des filtres avec contraintes sur les UCD
 *
 * @author Thomas Boch [CDS]
 * @version 0.9 : Juillet 2002 - Creation
 */

public class UCDFilter {

	// fonction pour selectionner une source ne possedant pas une certaine colonne ou UCD
	static final String UNDEFINED = "undefined";

	static String ERR1,ERR2,ERR3,ERR4,ERR5,ERR6,ERR7,ERR8,ERR9,ERR10,ERR11,ERR12,ERR13;

	// different strings representing available operators
	private static final String GT = ">"; // greather than
	private static final String GE = ">="; // greather or equal
	private static final String LT = "<"; // less than
	private static final String LE = "<="; // less or equal
	private static final String EQ = "="; // equals
	private static final String NE = "!="; // not equal

    //  true if a problem arose during the conversion
	private boolean convertProblem = false;
    //  nb of sources for which conversion problems happened
	private int nbConvertProblem = 0;


	private int numero;

    //  useful in containsOperator to store the position of the operator
	private int position;

	private String curOperator = null; // equals to the current operator

	// vector of ConstraintsBlock for the filter
	private Vector<ConstraintsBlock> constraintsBlocks;

	// work variable
	private ConstraintsBlock block;

	// definition of  the filter, as entered by the user
	protected String definition;

	// true if the filter is validated
	boolean isValidated = false;

	// true if the syntax is false
	boolean badSyntax = false;

	// name of the filter
	String name = null;

	// Reference to Aladin
	Aladin a;
	// Reference to PlanFilter
	PlanFilter pf;

	protected void createChaine() {
		ERR1 = a.chaine.getString("UFERR1");
		ERR2 = a.chaine.getString("UFERR2");
		ERR3 = a.chaine.getString("UFERR3");
		ERR4 = a.chaine.getString("UFERR4");
		ERR5 = a.chaine.getString("UFERR5");
		ERR6 = a.chaine.getString("UFERR6");
		ERR7 = a.chaine.getString("UFERR7");
		ERR8 = a.chaine.getString("UFERR8");
		ERR9 = a.chaine.getString("UFERR9");
		ERR10= a.chaine.getString("UFERR10");
		ERR11= a.chaine.getString("UFERR11");
		ERR12= a.chaine.getString("UFERR12");
		ERR13= a.chaine.getString("UFERR13");
	}

	// constructor with name and constraints string
	UCDFilter(String name, String constraints, Aladin a, PlanFilter pf) {
		this.a = a;
		createChaine();
		this.name = name;
		this.pf = pf;
		//action = new Action(ACTION+Action.SHOW,a);
		decodeConstraints(constraints);
		if (!badSyntax)
			isValidated = true;
	}

	/*// constructor with name and constraints array (each element being one constraint)
	UCDFilter(String name, String[] constraints, Aladin a) {
	this.a = a;
	this.name = name;
	//action = new Action(ACTION+Action.SHOW,a);
		decodeConstraints(constraints);
		if(!badSyntax) isValidated=true;
	}*/

	// constructor with a full definition (for scripting)
	// example : filter f1 { ... }
	UCDFilter(String definition, Aladin a, PlanFilter pf) {
		this.a = a;
		this.pf = pf;
		//action = new Action(ACTION+Action.SHOW,a);
		decodeDefinition(definition);
		if (!badSyntax)
			isValidated = true;
	}

	/** modify the definition of the filter */
	protected void changeDefinition(String newDef) {
		isValidated = false;
		badSyntax = false;
		String oldDef = definition;
		decodeConstraints(newDef);
		if (!badSyntax)
			isValidated = true;
	}

	void setNumero(int numero) {
		this.numero = numero;
	}

	// reenclenche les actions, ie demande le recalcul de la size par exemple, etc ...
	protected void resetActions() {
		Enumeration e = constraintsBlocks.elements();
		ConstraintsBlock cb = null;
		int i;
		while (e.hasMoreElements()) {
			cb = (ConstraintsBlock) e.nextElement();
			for (i = 0; i < cb.actions.length; i++) {
				cb.actions[i].reset();
			}
		}
	}

	// decode the different constraints and set variables
	private void decodeConstraints(String def) {
		//System.out.println("DEF : "+def);
		constraintsBlocks = new Vector();

		// skip '\n' at the beginning of the definition
		while (def.length() > 0 && def.charAt(0) == '\n') {
			def = def.substring(1);
		}

		// save of the original definition
		this.definition = new String(def);

		// replacement of '\t' by a blank character (avoid problems later)
		def = def.replace('\t', ' ');

		String tmp = new String("");
		String curTok;
		// skip the remarks
		StringTokenizer st = new StringTokenizer(def, "\n");
		while (st.hasMoreTokens()) {
			curTok = st.nextToken();
			if (!skipSpaces(curTok).startsWith("#"))
				tmp += curTok + "\n";
		}
		//System.out.println(tmp);
		//System.out.println(def);

		def = tmp;

		// basic check
		if (Action.countNbOcc('{', def) != Action.countNbOcc('}', def)) {
			badSyntax = true;
			Aladin.warning(ERR1, 1);
			return;
		}

		if (Action.countNbOcc('[', def) != Action.countNbOcc(']', def)) {
			badSyntax = true;
			Aladin.warning(ERR2, 1);
			return;
		}

		int longueur = def.length();
		int indice = 0;
		int beginAction, endAction;
		String actionStr, constraintsStr;

		while (indice < longueur) {
			block = new ConstraintsBlock();
			// search for a beginning bracket of a block of actions
			beginAction = getOpeningBracket(def, indice);
			if (beginAction < 0) {
				// traitement pour permettre de ne pas avoir d'accolades quand on a juste une action sans contrainte
				// exemple : "draw blue plus" devient "{draw blue plus}"
				if( indice==0 && def.trim().startsWith(Action.DRAW) ) {
					def = "{"+def+"}";
					this.definition = "{\n"+this.definition+"\n}";
					longueur += 2;
					beginAction=0;
				}
				else break;
			}

			endAction = getClosingBracket(def, beginAction + 1);
			if (endAction < 0) {
				badSyntax = true;
				Aladin.warning(ERR3, 1);
				return;
			}

			// maintenant, on peut supprimer les $ (de '${')
			constraintsStr = def.replace('$', ' ').substring(indice, beginAction);
			// pas besoin de skipSpaces ici, on conserve les espaces. Ils seront enleves si besoin est dans la classe Action
			actionStr = def.replace('$', ' ').substring(beginAction + 1, endAction);

			// getActions qui renvoie un tableau des actions
			// retrieve different actions for the current block
			block.actions = getActions(actionStr);
			// check if syntax is OK
			for (int i = 0; i < block.actions.length; i++) {
				if (block.actions[i].badSyntax) {
					badSyntax = true;
					return;
				}
			}

			// replacement of \n with spaces
			constraintsStr = constraintsStr.replace('\n', ' ');
			// if the constraint string is empty, we put a trivial constraint (to avoid a bug when no constraint is set)
			if ( skipSpaces(constraintsStr).length() == 0 )
				constraintsStr = "1=1";

			// On effectue ici un traitement sur constraintsStr pour enlever
			// les espaces sauf entre les guillmets où ils doivent être conservés
			// Il n'y a PAS de skipSpaces sur constraintsStr à un autre endroit !!
			int index=0;
			String str = new String();
			while( index<constraintsStr.length() ) {
				int i1 = constraintsStr.substring(index).indexOf("\"");
				if( i1>=0 ) {
					i1 += index;
					int i2 = constraintsStr.substring(i1+1).indexOf("\"");
					if( i2>=0 ) {
						i2 += i1+1;
						// d'abord, recopie de index jusqu'à i1 en skippant les espaces
						str += skipSpaces(constraintsStr.substring(index,i1));
						// ensuite, recopie sans toucher à rien de i1 jusqu'à i2
						str += constraintsStr.substring(i1,i2+1);
						index = i2+1;
					}
					else {
						str += skipSpaces(constraintsStr.substring(index));
						index = constraintsStr.length();
					}
				}
				else {
					str += skipSpaces(constraintsStr.substring(index));
					index = constraintsStr.length();
				}
			}
			//System.out.println("avant : "+constraintsStr);
			constraintsStr = MetaDataTree.replace(str,"==","=",-1);
			//System.out.println("apres : "+constraintsStr);

			// we get the condition strings(7+3)*y=18
			String[] conditions = getConditions(constraintsStr);
			block.valueConstraints = new Constraint[conditions.length];

			// for each condition string, get the corresponding constraint
			for (int i = 0; i < conditions.length; i++) {
				// Processing for an "undefined" constraint
				if (conditions[i].startsWith(UNDEFINED)) {
					block.valueConstraints[i] =
						decodeUndefinedConstraint(conditions[i]);

				}

				// Processing for a classic constraint
				else {
					if (!containsOperator(conditions[i])) {
						Aladin.warning(ERR9,1);
						badSyntax = true;
						return;
					}
					block.valueConstraints[i] =
						decodeValueConstraint(conditions[i]);
				}

				if (badSyntax)
					return;
			}
			// replacement of each condition by "\[index]", with index reference to the  position in block.valueConstraints //
			for (int l = 0; l < conditions.length; l++) {
				constraintsStr =
					replace(constraintsStr, conditions[l], "\\" + l);
			}

			// finally, replacement of "&&" with "*" and "||" with "+"
			constraintsStr =
				replace(replace(constraintsStr, "||", "+"), "&&", "*");
			block.checkExpr = constraintsStr;

//			System.out.println("**"+constraintsStr+"**");
			// when all constraints are "decoded", add the current block to the vector of all blocks
			constraintsBlocks.addElement(block);
			// and we initialize block for the next step
			block = new ConstraintsBlock();

			indice = endAction + 1;
		}
	}

	/** Searches all "conditions" in the string s, and return them
	 *  as an array of string.
	 *	E.g: when applying to the string "(((x)>4)&&(y<8||(7+3)*y=18))||x>9"
	 * it returns the elements "(x)>4","y<8","(7+3)*y=18","x>9"
	 * PRE : spaces were already skipped where appropriate
	 *	@param s - the string holding the expression
	 *	@return a String array holding all constraints
	 */
	private String[] getConditions(String s) {
		String condStr;
		StringTokenizer st = new StringTokenizer(s, "|&");
		String curToken, workStr, tmp, saveToken;
		workStr = null;
		String[] conditions = new String[st.countTokens()];
		int i = 0;

		while (st.hasMoreTokens()) {
			curToken = st.nextToken();
			saveToken = new String(curToken);
			// traitement special pour undefined
			if (curToken.startsWith(UNDEFINED)) {
				conditions[i] = curToken;
				i++;
				continue;
			}

			// Traitement spécial pour les double quote (cas d'une condition sur une chaine), on enlève les parenthèses, et on les remplace par X
			if (curToken.indexOf("\"") > 0) {
				int lIndex,rIndex;
				lIndex = curToken.indexOf("\"");
				rIndex = curToken.lastIndexOf("\"");
				if( lIndex!=rIndex ) {
					String btw = curToken.substring(lIndex,rIndex+1);
					btw = btw.replace('(','X');
					btw = btw.replace(')','X');
					btw = btw.replace('{','X');
					btw = btw.replace('}','X');
					btw = btw.replace('[','X');
					btw = btw.replace(']','X');
					curToken = saveToken.substring(0,lIndex)+btw+saveToken.substring(rIndex+1);
				}
			}



			//System.out.println("curToken : "+curToken);
			containsOperator(curToken);
			tmp = curToken.substring(0, position);

			int pos;
			if (tmp.indexOf("(") >= 0) {
				int length = tmp.length();
				pos = length;

				workStr = "";

				while (pos > 0
					&& Action.countNbOcc(')', workStr)
						>= Action.countNbOcc('(', workStr)) {
					//System.out.println(workStr);
					workStr = tmp.substring(--pos);

				}
				if (Action.countNbOcc('(', workStr)
					> Action.countNbOcc(')', workStr)) {
					workStr = workStr.substring(1);
					pos++;
				}
			} else
				pos = 0;

			// on recherche une eventuelle parenthese fermante apres l'operateur (parenthese qui ne ferait pas partie de la condition)
			int finCond = curToken.substring(position).indexOf(')');
			//System.out.println("la position vaut: "+finCond);
			if (finCond < 0)
				finCond = curToken.length();
			else
				finCond += position;
			// patch dans le cas d'une condition sur une string
			//if( Action.countNbOcc('\"',curToken)==2 ) finCond = curToken.lastIndexOf("\"");

			workStr = saveToken.substring(pos, finCond);

			conditions[i] = workStr;
			i++;
		}

		return conditions;
	}

	/** decode an "undefined" constraint */
	private Constraint decodeUndefinedConstraint(String constraint) {
		int begin, end;
		String undefVar;

		begin = constraint.indexOf("(");
		end = constraint.indexOf(")");

		if (begin < 0 || end < 0 || begin > end) {
			Aladin.warning(ERR10+" " + UNDEFINED + " "+ERR11, 1);
			badSyntax = true;
			return null;
		}

		undefVar = constraint.substring(begin + 1, end);
		return new Constraint(undefVar);
	}

	// decode a constraint on UCDs values
	// returns a Constraint object
	private Constraint decodeValueConstraint(String constraint) {
		//System.out.println(constraint);
		int indexOperator = constraint.indexOf(curOperator);
		String strToParse = constraint.substring(0, indexOperator);
		boolean isStringConstraint = false;
		// true if the constaint is a "string value constraint" (in contrast with decimal value constraints)

		String op = curOperator;
		double value = 0;
		String strValue = null;
		String valueStr =
			constraint.substring(
				indexOperator + curOperator.length(),
				constraint.length());

		Unit unit = null;
		// in case of a string contraint
		if( isStrValue(valueStr) ) {
			// EQ and NE are the only possible values for the operator
			if (curOperator.equals(EQ) || curOperator.equals(NE)) {
				strValue = valueStr;
				isStringConstraint = true;
			} else {
				//System.out.println(valueStr);
				Aladin.warning(ERR12,1);
				badSyntax = true;
				return null;
			}
		}
		else {
			try {
				unit = new Unit(valueStr);
			}
			catch( ParseException e ) {
				Aladin.warning(ERR12,1);
				badSyntax = true;
				return null;
			}
			catch (ArithmeticException aExc) {
				badSyntax = true;
				Aladin.warning(ERR4, 1);
				return null;
			}
		}
//		Unit unit = null;
//		try {
//			unit = new Unit(valueStr);
//		} catch (java.text.ParseException e) {
//			// if we are in string value constraint mode, the only possible
//			// operators are EQ or NE
//			if (curOperator.equals(EQ) || curOperator.equals(NE)) {
//				strValue = valueStr;
//				isStringConstraint = true;
//			} else {
//				//System.out.println(valueStr);
//				Aladin.warning(ERR12,1);
//				badSyntax = true;
//				return null;
//			}
//		} catch (ArithmeticException aExc) {
//			badSyntax = true;
//			Aladin.warning(ERR4, 1);
//			return null;
//		}

		// case of a string value constraint
		if (isStringConstraint) {
			int nQuote = Action.countNbOcc('\"', strValue);
			if (nQuote != 0 && nQuote != 2) {
				Aladin.warning(ERR5, 1);
				badSyntax = true;
				return null;
			}
			return new Constraint(strToParse, op, strValue);
		}

		// else we have a decimal value constraint
		// creation of the parser
		Parser parser;
		try {
			parser = createParser(strToParse, a);
		} catch (ParserException e) {
			Aladin.warning(ERR13+"\n" + e.getMessage(),1);
			badSyntax = true;
			return null;
		}

		value = unit.value;
		// do we have a unit symbol ?
		if (unit.symbol.length() > 0) {
			/*System.out.println("unti not null");*/
			return new Constraint(parser, op, value, unit);
		} else { /*System.out.println("symbol null");*/
			return new Constraint(parser, op, value);
		}
	}

	/**
	 * tests whether s represents a string value
	 * @param s
	 */
	private boolean isStrValue(String s) {
		return s.trim().charAt(0)=='"';
	}

	/** replace each occurence of "pattern" within "source" with "replace"
	 *	@param source - the original string
	 *	@param pattern - the pattern to be replaced
	 *	@param replace - the replacement string
	 */
	public static String replace(
		String source,
		String pattern,
		String replace) {
		final int len = pattern.length();
		StringBuffer sb = new StringBuffer();
		int found = -1;
		int start = 0;

		while ((found = source.indexOf(pattern, start)) != -1) {
			sb.append(source.substring(start, found));
			sb.append(replace);
			start = found + len;
		}

		sb.append(source.substring(start));

		return sb.toString();
	}

	/** encode a UCD (or a column name), so that there is no problem with the operator characters
	 *	@param s - the string to be encoded
	 *	@return the encoded string

	protected static String encodeUCD(String s) {
		String ret = new String(s);
		ret = ret.replace('+', '@');
		ret = ret.replace('-', '!');
		ret = ret.replace('*', '~');
		ret = ret.replace('/', '&');
		return ret;
	}
	*/

	/** decode an encoded UCD (or a column name), to retrieve its original form
	 *	@param s - the string to be decoded
	 *	@return the decoded string

	protected static String decodeUCD(String s) {
		String ret = new String(s);
		ret = ret.replace('@', '+');
		ret = ret.replace('!', '-');
		ret = ret.replace('~', '*');
		ret = ret.replace('&', '/');
		return ret;
	}
	*/

	/**
	 *  @param s : the string to parse with unencoded variables
	 *	@param vars: array with unencoded variables
	 *  @param encodedVars: array with encoded variables
	 *  @return : the string to parse with encoded variables

	static private String putEncodedVariables(
		String s,
		String[] vars,
		String[] encodedVars) {
		int begin, end;
		int i = 0;
		int oend = 0;
		StringBuffer strBuf = new StringBuffer();

		for (;;) {
			if (i == vars.length)
				break;
			begin = s.indexOf(vars[i], oend);
			if (begin < 0)
				break;
			end = begin + vars[i].length();

			//System.out.println(oend+"\t"+begin);
			strBuf.append(s.substring(oend, begin));
			strBuf.append(encodedVars[i]);
			oend = end;
			i++;
		}

		if (oend < s.length())
			strBuf.append(s.substring(oend, s.length()));

		return strBuf.toString();
	}
	*/

	// returns the array of strings corresponding to variables in str
	// ( i.e corresponding to UCDs, which are enclosed by square brackets [] )
	static private String[] getVariables(String str, Aladin aladin) {
		Vector vec = new Vector();
		String variable = null;
		int beginUCD, beginCol, begin, end;

		while (str.length() > 0) {

			beginUCD = str.indexOf("[");
			beginCol = str.indexOf("{");

			// no more variables
			if (beginUCD < 0 && beginCol < 0)
				break;

			// analyze the UCD variable
			if (beginCol < 0 || (beginUCD >= 0 && beginUCD < beginCol)) {
				begin = beginUCD;
				end = str.indexOf("]");
				variable = str.substring(begin, end + 1);

				try {
					// loop to catch all [ .. [..] ..] in the variable
					while (Action.countNbOcc('[', variable)
						!= Action.countNbOcc(']', variable)) {
						end++;
						variable = str.substring(begin, end + 1);
					}
				} catch (StringIndexOutOfBoundsException e) {
					Aladin.warning(ERR6, 1);
					return null;
				}

			}

			// analyze the col variable
			else {
				begin = beginCol;
				end = str.indexOf("}");
				variable = str.substring(begin, end + 1);

				try {
					// loop to catch all { .. {..} ..} in the variable
					while (Action.countNbOcc('{', variable)
						!= Action.countNbOcc('}', variable)) {
						end++;
						variable = str.substring(begin, end + 1);
					}
				} catch (StringIndexOutOfBoundsException e) {
					Aladin.warning(ERR7);
					return null;
				}
			}
            vec.addElement(variable);

			str = str.substring(end + 1, str.length());

		} // end of while loop

		String[] ret = new String[vec.size()];
		vec.copyInto(ret);

		return ret;
	}

	// returns true if the string str contains one of the operator
	// note : faire un tableau comme variable statique de classe contenant tous les operateurs
	private boolean containsOperator(String str) {

		int pos;
		// test a placer avant le test de EQ, sinon on aura un pb !
		if ((pos = str.indexOf(NE)) >= 0) {
			curOperator = NE;
			position = pos;
			return true;
		}
		if ((pos = str.indexOf(GE)) >= 0) {
			curOperator = GE;
			position = pos;
			return true;
		}
		if ((pos = str.indexOf(LE)) >= 0) {
			curOperator = LE;
			position = pos;
			return true;
		}
		if ((pos = str.indexOf(EQ)) >= 0) {
			curOperator = EQ;
			position = pos;
			return true;
		}
		if ((pos = str.indexOf(GT)) >= 0) {
			curOperator = GT;
			position = pos;
			return true;
		}
		if ((pos = str.indexOf(LT)) >= 0) {
			curOperator = LT;
			position = pos;
			return true;
		}

		return false;
	}

	// delete spaces from a string
	static public String skipSpaces(String str) {

		StringTokenizer sTok = new StringTokenizer(str, " ", false);

		StringBuffer result = new StringBuffer();

		while (sTok.hasMoreTokens())
			result.append(sTok.nextToken());

		return result.toString();
	}

	// decode a whole filter definition
	void decodeDefinition(String def) {
		int posBeginDef = -1;
		this.name="";
		this.definition=def;

		// basic check
		if ( def.indexOf("}")<0
			|| (posBeginDef = def.indexOf("{")) < 0) {
			badSyntax = true;
			Aladin.warning(ERR8,1);
			return;
		}

		// let's retrieve the name of the filter
		int posBeginName = def.indexOf("filter") + 6;
		if( posBeginName<0 ) {
			badSyntax = true;
			Aladin.warning(ERR8,1);
			return;
		}
		this.name = skipSpaces(def.substring(posBeginName, posBeginDef));


		int posEnd = def.lastIndexOf("}");

		// we skip the from the definition the "preamble"
		def = def.substring(posBeginDef + 1, posEnd);

		// skip a possible starting CR
        if( def.startsWith(Util.CR) ) {
            def = def.substring(Util.CR.length());
        }

		// skip a possible final CR
		if( def.endsWith(Util.CR) ) {
			def = def.substring(0, def.lastIndexOf(Util.CR) );
		}
        // or a possible final "\n"
        else if( def.endsWith("\n") ) {
            def = def.substring(0, def.lastIndexOf("\n") );
        }

		decodeConstraints(def);
	}

	/** Select sources according to the filter definition
	 *	@param sources - array of sources at the entry of the filter
	 */
	protected void select(Source[] sources) {
		ConstraintsBlock curBlock = null;
		Vector remainingSources = new Vector();
		nbConvertProblem = 0;

		Enumeration eBlocks = constraintsBlocks.elements();

		// first deselect all sources
		a.view.deSelect();

		// loop on all blocks
		while (eBlocks.hasMoreElements()) {
			curBlock = (ConstraintsBlock) eBlocks.nextElement();
			remainingSources.removeAllElements();

			for (int i = sources.length - 1; i >= 0; i--) {

				if (verifyValueConstraints(sources[i], curBlock)) {

					sources[i].setSelect(true);
					a.view.vselobj.addElement(sources[i]);

				} else if (convertProblem) {
					//System.out.println("Problem");
					nbConvertProblem++;
				} else {
					remainingSources.addElement(sources[i]);
				}
			}

			// copy the remaining sources into sources
			if (remainingSources.size() > 0) {
				sources = new Source[remainingSources.size()];
				remainingSources.copyInto(sources);
			} else
				break;
		}

		// for the remaining sources, we set select to false
		if (remainingSources.size() > 0) {
			for (int i = sources.length - 1; i >= 0; i--)
				sources[i].setSelect(false);
		}

		if (nbConvertProblem > 0) {
			Aladin.warning(
				"Warning : there were conversion problems for "
					+ nbConvertProblem
					+ " sources",
				1);
		}
	}

	/** Filters sources according to the different constraints and set isSelected to true when a source correpsonds to the constraints
	 *  @param sources array of sources one wants to filter
	 *  @param inThread - si true, on appelle la méthode depuis le thread PlanFilter.runme
	 *	@return array of all selected sources
	 */
	protected Source[] getFilteredSources(Source[] sources, boolean inThread) {
		long start = System.currentTimeMillis();
		Vector filteredSources = new Vector();

		int nbSources = sources.length; // nb de sources total

		nbConvertProblem = 0;

		Enumeration eBlocks = constraintsBlocks.elements();

		ConstraintsBlock curBlock = null;
		ConstraintsBlock[] blocks = new ConstraintsBlock[constraintsBlocks.size()];

		int l=0;
		while( eBlocks.hasMoreElements() ) {
			blocks[l]=(ConstraintsBlock)eBlocks.nextElement();
			l++;
		}

        // vecBlockSources[i] is the vector of selected sources for block blocks[i]
        Vector[] vecBlockSources = new Vector[blocks.length];
        for( int i=0; i<vecBlockSources.length; i++ ) vecBlockSources[i] = new Vector();


		// we test all sources
		for (int i = sources.length - 1; i >= 0; i--) {
			// allocation mémoire pour le tableau "values" liés aux sources
			if( sources[i].values==null ) sources[i].values = new double[PlanFilter.LIMIT][][];
			// allocation mémoire pour le tableau "isSelected" liés aux sources
			if( sources[i].isSelected==null ) sources[i].isSelected = new boolean[PlanFilter.LIMIT];
			// allocation mémoire pour le tableau "actions" liés aux sources
			if( sources[i].actions==null ) sources[i].actions = new Action[PlanFilter.LIMIT][];


			// Pour laisser la main aux autres threads
			if ( i % 50 == 0 ) {
				// maj du pourcentage
				pf.setPourcent(100.0*((double)(nbSources-i)/(double)(nbSources)));
				//System.out.println(pf.pourcent);
			}

            // should we stop the processing
            if( inThread && i%100==0 && pf.runme==null ) {
         	   return null;
            }

			boolean accomplished=false;
			// loop on all blocks (on teste une par une les contraintes jusqu'à ce que la source en vérifie une)
			for( int k=0;!accomplished&&k<blocks.length;k++ ) {
				curBlock = blocks[k];
				if (verifyValueConstraints(sources[i], curBlock)) {
					filteredSources.add(sources[i]);
                    vecBlockSources[k].add(sources[i]);
					accomplished=true;

					sources[i].isSelected[numero] = true;
					sources[i].actions[numero] = curBlock.actions;

					// initialisation of values array for each source
//					sources[i].values[numero] = new double[sources[i].actions[numero].length][4];
					sources[i].values[numero] = new double[sources[i].actions[numero].length][4];
					for(int j = 0;j < sources[i].actions[numero].length; j++) {
                        sources[i].actions[numero][j].computeValues(sources[i],numero,j);
					}
				} else if (convertProblem) {
					//System.out.println("Problem");
					nbConvertProblem++;
					accomplished=true;
				}
			} // fin de la boucle for sur tous les blocks

			// on set actions à null si aucune contrainte n'est vérifiée
			if( !accomplished ) {
				sources[i].actions[numero] = null;
			}
		} // fin de la boucle for sur toutes les sources

        ///////////////////////////////////////////////////////////
        // blockSources[i] is the array of selected sources for blocks[i]
        Source[][] blockSources = new Source[blocks.length][];
        for( int i=0; i<vecBlockSources.length; i++ ) {
            blockSources[i] = new Source[vecBlockSources[i].size()];
            vecBlockSources[i].copyInto(blockSources[i]);
            vecBlockSources[i] = null;
        }

        // A ce stade blockSources contient les sources sélectionnées pour chaque block
        // Boucle sur l'ensemble des blocks
        for( int i=0; i<blocks.length; i++ ) {

            // Boucle sur les actions pour calcul des extrema
            for( int k=0; k<blocks[i].actions.length; k++) {
                // calcul de l'extremum
                blocks[i].actions[k].computeExtremum(blockSources[i]);
            }

            // Boucle sur les sources
            for( int j=0; j<blockSources[i].length; j++) {
                // should we stop the processing
                if( inThread && j%100==0 && pf.runme==null ) {
             	   return null;
                }

                // Boucle sur les actions
                for( int k=0; k<blocks[i].actions.length; k++ ) {
                    blocks[i].actions[k].finalcomputeValues(blockSources[i][j],numero,k);
                }
            }

            //System.out.println("Nb sources sélectionnées pour block "+i+" : "+blockSources[i].length);
        }



        ///////////////////////////////////////////////////////////

		Source[] sourceArray = new Source[filteredSources.size()];
		filteredSources.copyInto(sourceArray);
		//System.out.println("nb selected sources: "+sourceArray.length);

		if (nbConvertProblem > 0)
			Aladin.warning(
				"Warning : there were conversion problems for "
					+ nbConvertProblem
					+ " sources",
				1);
		long end = System.currentTimeMillis();
        Aladin.trace(3,"Total time for filtering: "+(end-start));
		return sourceArray;
	}

	protected Source[] getFilteredSources(Source[] sources) {
		return getFilteredSources(sources, false);
	}

	protected boolean hasRainbowFunction() {
	    return this.definition.toLowerCase().indexOf("rainbow")>=0;
	}

	protected double[] getRainbowMinMax() {
	    for (ConstraintsBlock constraint: constraintsBlocks) {
	        if (constraint.actions==null) {
	            continue;
	        }
	        for (Action action: constraint.actions) {
	            if (action.rainbowMinValue!=action.rainbowMaxValue) {
	                return new double[] {action.rainbowMinValue, action.rainbowMaxValue};
	            }
	        }
	    }
	    return new double[] {0.0, 0.0};
	}



    protected boolean verifyValueConstraints(Source s, int indexBlock) {
        return verifyValueConstraints(s, constraintsBlocks.elementAt(indexBlock));
    }

	/** check whether s verifies the value constraints
	 *	@param s source
	 *	@param b block of constraints
	 *	@return  true if s verifies value constraints
	 */
	private boolean verifyValueConstraints(Source s, ConstraintsBlock b) {
		int i;
		Constraint curConst = null;

		if (b.valueConstraints == null)
			return true;

		final int length = b.valueConstraints.length;

		// array storing if b.valueConstraints[i] is verified
		boolean[] check = new boolean[length];

		for (i = 0; i < length; i++) {
			curConst = b.valueConstraints[i];

			check[i] = verifyOneValueConstraint(s, curConst);
			if( convertProblem ) { /*System.out.println("Convert problem");*/
				return false;
			}

		}

		final String un = "1";
		final String zero = "0";
		String foo;
		String checkStr = new String(b.checkExpr);
		// we replace the "\[index]" by the value (0==false, 1==true)
 		for( i = length-1; i >= 0; i-- ) {
			foo = check[i] ? un : zero;
			checkStr = MetaDataTree.replace(checkStr, "\\" + i, foo,1);
		}

		Parser parserCheck = new Parser(checkStr);
		parserCheck.parseString();
//		System.out.println("string vaut : "+checkStr);
		return (parserCheck.eval() > 0);
	}

	/** checks on a source the validity of a constraint (used by verifyValueConstraints)
	 *	@param s - the source
	 *	@param cons - the constraint
	 *  @return  true if s verifies the constraint cons, false otherwise
	 */
	private boolean verifyOneValueConstraint(Source s, Constraint cons) {
		double leftValue = 0.0;
		String strLeftValue = null;
		convertProblem = false;

		// case of a string value constraint
		if (cons.stringConstraint) {
			// can be a UCD name or a column name
			String colOrUCD = cons.ucd.substring(1, cons.ucd.length() - 1);
			int pos;
			//System.out.println(colOrUCD);

			// case of a UCD
			if (cons.ucd.startsWith("["))
				pos = s.findUCD(colOrUCD.toUpperCase());
			// case of a column
			else
				pos = s.findColumn(colOrUCD);

			// if the position was not found, return false
			if (pos < 0)
				return false;
			strLeftValue = s.getValue(pos);

			// ce cas peut arriver lorsque il y a un field sans valeur associee
			if (strLeftValue == null)
				return false;

			if (cons.operator.equals(EQ)) {
				//if (!strLeftValue.equals(cons.strValue))
				return ( match(cons.strValue, strLeftValue) );
			} else if (cons.operator.equals(NE)) {
				//if (strLeftValue.equals(cons.strValue))
				return ( !match(cons.strValue, strLeftValue) );
			}
		}

		// case of an undefined constraint
		else if (cons.undefinedConstraint) {
			String undefVar = cons.ucd.substring(1, cons.ucd.length() - 1);

			// true if the UCD/column is NOT found
			// case of a UCD
			if (cons.ucd.startsWith("["))
				return (s.findUCD(undefVar.toUpperCase()) < 0);
			// case of a column
			else
				return (s.findColumn(undefVar) < 0);

		}

		// case of a decimal value constraint
		else {
			//if( ! Action.setAllVariables(cons.parser, s, false) ) return false;
			if (!Action.setAllVariables(cons.parser, s, false, cons.convertUnit))
				return false;

			leftValue = cons.parser.eval();

			// conversion dans l'unite de reference
			if (cons.convertUnit) {
				Unit refUnit, evalUnit;
				try {
					//System.out.println("Avant conversion : "+cons.parser.evalUnit());
					refUnit = new Unit(cons.unit);
                    evalUnit = cons.parser.evalUnit();
                    /*
					System.out.println("refUnit : "+refUnit);
                    System.out.println("refUnit SI: "+refUnit.getSIunit());
					System.out.println("evalUnit : "+evalUnit);
                    System.out.println("evalUnit SI : "+evalUnit.getSIunit());
                    System.out.println(refUnit.getSIunit());
                    System.out.println(evalUnit.getSIunit());
                    */
					refUnit.convertFrom(evalUnit);
				} catch (ArithmeticException aExc) {
					System.err.println(aExc);
					convertProblem = true;
					return false;
				} catch (java.text.ParseException pExc) {
					System.err.println(pExc);
					convertProblem = true;
					return false;
				}

				leftValue = refUnit.value;
				//System.out.println("Apres conversion : "+refUnit);

				//System.out.println(refUnit);
			} else
				leftValue = cons.parser.eval();

			return (checkExpr(leftValue, cons.operator, cons.value));
		}

		return true;

	}

    /**
     * checks whether lVal op rVal
     * @param lVal left value
     * @param op string representing the operator
     * @param rVal right value
     * @return boolean true if lVal op rVal
     */
	private boolean checkExpr(double lVal, String op, double rVal) {
        if (op.equals(EQ))
			return (lVal == rVal);

		if (op.equals(GT))
			return (lVal > rVal);
		if (op.equals(GE))
			return (lVal >= rVal);

		if (op.equals(LT))
			return (lVal < rVal);
		if (op.equals(LE))
			return (lVal <= rVal);

		if (op.equals(NE))
			return (lVal != rVal);

		return false;

	}

	/** Checks whether word matches the mask. If mask does not contain '*' or '?' wildcards,
	 *  the equals native method is called (should be faster)
	 * @param mask mask
	 * @param word word to check
	 * @return boolean true if word matches mask
	 */
	private boolean match(String mask, String word) {
		/*
		if( mask.indexOf("*")>=0 || mask.indexOf("?")>=0 )
			return UCDFilter.matchMask(mask, word);
		else return word.equals(mask);
		*/
		// thomas (AVO 2005)
		if( Source.useWildcard(mask) )
			return Util.matchMask(mask, word);
		else return word.equals(MetaDataTree.replace(mask, "\\*", "*", -1));

	}

	/** creates a parser and sets variables
	 *  @param strToParse - the string which has to be parsed
	 *  @return the parser
	 */
	static protected Parser createParser(String strToParse, Aladin aladin)
		throws ParserException {
		String[] vars = getVariables(strToParse, aladin);
		if (vars == null) {
			throw new ParserException();
		}
		String[] encodedVars = new String[vars.length];
		// array of encoded variables

		// fill the encoded variables array
		for (int i = 0; i < encodedVars.length; i++) {
			encodedVars[i] = vars[i];
		}

//		strToParse = putEncodedVariables(strToParse, vars, encodedVars);
		// TODO : à supprimer
		strToParse = new String(strToParse);

		// creation of a new parser
		Parser parser = new Parser(strToParse);
		// we add the variables to the parser
		for (int i = 0; i < vars.length; i++) {
			parser.addVar(encodedVars[i]);
		}

		try {
			parser.parseString();
		}
		catch (ParserException e) {
		    e.printStackTrace();
			throw new ParserException("Maybe a problem with your variables names");
		}

		return parser;
	}

	/** parse a string and returns the corresponding action array
	 *	@param s - string with the action
	 *	@return array of Action objects corresponding to the different actions found in s (different actions are separated by a \'n\
	 */
	private Action[] getActions(String s) {
		// petite bidouille pour permettre a une action d'etre sur plusieurs lignes
		StringTokenizer st = new StringTokenizer(s, "\n");
		String curToken;
		String cleanedStr = new String("");
		while (st.hasMoreTokens()) {
			curToken = st.nextToken();
			while (st.hasMoreTokens()
				&& (Action.countNbOcc('(', curToken)
					!= Action.countNbOcc(')', curToken))) {
				curToken += st.nextToken();
			}
			curToken.replace('\n', ' ');
			cleanedStr += curToken + "\n";
		}
		s = cleanedStr;

		// Les actions sont separees soit par un retour a la ligne soit par un ";"
		// Cette partie est a revoir : on peut - dans l'absolu - avoir des noms de colonne contenant un ";"
		// TODO : prise en compte des ; dans les noms de colonnes ou d'ucd !
		// *** début révision
		Vector v = new Vector();
		StringBuffer tmp = new StringBuffer();
		char[] charArray = s.toCharArray();
		int opening1, opening2, closing1, closing2;
		opening1 = opening2 = closing1 = closing2 = 0;
		char c;
		String strTmp;
		for( int i=0; i<charArray.length; i++) {
		    c = charArray[i];
		    // on a trouvé un véritable séparateur
		    if( c=='\n' || (c==';' && opening1==closing1 && opening2==closing2) ) {
		        strTmp = tmp.toString();
		        if( strTmp.trim().length()>0 ) v.addElement(tmp.toString());
		        // RAZ des variables
		        tmp = new StringBuffer();
		        opening1 = opening2 = closing1 = closing2 = 0;
		    }
		    else {
		        if( c=='{' ) opening1++;
		        else if( c=='}' ) closing1++;
		        else if( c=='[' ) opening2++;
		        else if( c==']' ) closing2++;

		        tmp.append(c);
		    }
		}
		strTmp = tmp.toString();
		if( strTmp.trim().length()>0 ) v.addElement(strTmp);

		// à ce stade, v contient l'ensemble des chaines des actions
		Action[] ret;
		// DRAWOBJECT comme action par défaut !
		if( v.size()==0 ) {
		    ret = new Action[]{new Action(Action.DRAWOBJECT, a, pf)};
		}
		else {
		    ret = new Action[v.size()];
			Enumeration eActions = v.elements();
			int i=0;
			while( eActions.hasMoreElements() ) {
			    ret[i++] = new Action((String)eActions.nextElement(), a, pf);
			}
		}

		// test
//		Enumeration eActions = v.elements();
//		while( eActions.hasMoreElements() ) {
//		    System.out.println((String)eActions.nextElement());
//		}
//		if( v.size()==0 ) System.out.println("VIDE");

		//*** fin révision

		// ancienne version, non adaptée aux UCD1+
/*
		st = new StringTokenizer(s, "\n;");
		Action[] ret;
		int size = st.countTokens();
		if (size > 0) {
			ret = new Action[size];

			int i = 0;
			while (st.hasMoreTokens()) {
				ret[i] = new Action(st.nextToken(), a, pf);
				i++;
			}
		} else {
			ret = new Action[1];
			ret[0] = new Action(Action.DRAWOBJECT, a, pf);
		}
*/
		return ret;
	}


	/** searches the first index of an opening bracket '{' beginning at index begin in string s
	 *  '${', indicating the beginning of a column variable is skipped
	 *	@param s - the string
	 *	@param begin - index where one begins to search
	 *	@returns the position of the first '{' in s, -1 if not found
	 */
	private int getOpeningBracket(String s, int begin) {
		int result;
		while (true) {
			result = s.indexOf("{", begin);
			if (result == -1)
				return -1;
			// we skip the '${'
			if (result > 0 && s.charAt(result - 1) == '$')
				begin = result + 1;
			else
				return result;
		}
	}

	/** searches the closing bracket '}' of an action
	 *
	 *	@param s - the string
	 *	@param begin - index where one begins to search
	 *	@returns the position of '}' in s, skipping the {} variables, -1 if not found
	 */
	private int getClosingBracket(String s, int begin) {
		int result;
		int oBegin = begin;
		String subStr;
		while (true) {
			result = s.indexOf("}", begin);
			if (result == -1)
				return -1;

			subStr = s.substring(oBegin, result + 1);
			//System.out.println(subStr);
			if (Action.countNbOcc('{', subStr) + 1
				== Action.countNbOcc('}', subStr))
				return result;
			else
				begin = result + 1;
		}
	}

	protected void Free() {
		if (constraintsBlocks != null) {
			constraintsBlocks.removeAllElements();
			constraintsBlocks = null;
		}
		block = null;
	}

	// inner class to describe constraints
	class Constraint {
		Parser parser;
		// value of the right part of constraint
		double value;
		// unit for value
		Unit unit;
		// operator of constraint
		String operator;
		// true if the constraint is a string constraint :
		// example : $[CLASS_STAR/GALAXY]="A"
		boolean stringConstraint;
		// true if there is a need of converting units
		boolean convertUnit = false;
		// true if the constraint is an "undefined" constraint
		boolean undefinedConstraint = false;

		// value in term of string
		String strValue = null;
		// used for string and undefined constraints
		String ucd = null;

		// constructor for a decimal value constraint
		Constraint(Parser parser, String operator, double value) {
			this.parser = parser;
			this.operator = operator;
			this.value = value;
			stringConstraint = false;
		}

		// constructor for a decimal value constraint with defined unit
		Constraint(Parser parser, String operator, double value, Unit unit) {
			this(parser, operator, value);
			this.unit = unit;
			convertUnit = true;
		}

		// constructor for a string value constraint
		Constraint(String ucd, String operator, String strValue) {
			this.ucd = ucd;
			this.operator = operator;
			this.strValue = strValue;
			stringConstraint = true;

			// petit traitement pour permettre les strValue entre guillemets ( eg : ${otyp}="Cloud" )
			if (strValue.length() > 0
				&& strValue.charAt(0) == '"'
				&& strValue.charAt(strValue.length() - 1) == '"')
				this.strValue = strValue.substring(1, strValue.length() - 1);
		}

		/** constructor for an "undefined" constraint
		 *	@param ucd - ucd or column which must be undefined
		 */
		Constraint(String ucd) {
			this.ucd = ucd;
			undefinedConstraint = true;
		}
	}

	// inner class to describe a block of constraints
	class ConstraintsBlock {
		// array of actions associated with this block (default : DRAW)
		//Action action = new Action(ACTION+Action.DRAW,a);
		Action[] actions = null;

		// array of Constraint objects representing constraints on UCD (or column) values
		Constraint[] valueConstraints;

		// expression which will be used to check whether a source is selected by a filter
		String checkExpr;

		ConstraintsBlock() {
		}
	}

}
