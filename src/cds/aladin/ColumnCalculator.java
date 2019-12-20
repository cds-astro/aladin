// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

/*
 * Created on 17 déc. 2003
 *
 *
 */
package cds.aladin;

import java.util.Iterator;
import java.util.Vector;

import cds.savot.model.SavotField;
import cds.tools.parser.Parser;
import cds.tools.parser.ParserException;
import cds.xml.Field;

/** This class aims at providing manipulations on table columns
 * @author Thomas Boch [CDS]
 */
public class ColumnCalculator {
    private SavotField[] fields;
    private String[] expr;
    private Plan p;
    
    private Expression[] parsers;
    
    private String error = "";
    
    // reference to Aladin object
    private Aladin a;
    
    // nombre de décimales à conserver
    private int nbDec;
    
    /** 
     * 
     * @param fields tableau décrivant les metadata des nouveaux champs
     * @param expr tableau des expressions arithmétiques permettant de calculer les nouveaux champs
     * @param p le plan catalogue pour lequel on souhaite de nouvelles colonnes
     * @param a reference à l'objet Aladin
     */
    protected ColumnCalculator(SavotField[] fields, String[] expr, Plan p, int nbDec, Aladin a) {
        this(a);
        setFields(fields);
        setExpr(expr);
        setPlan(p);
        setNbDec(nbDec);
    }
    
    protected ColumnCalculator(Aladin a) {
        this.a = a;
    }
    
    /** 
     * 
     * @param nbDec nombre de décimales à conserver
     */
    protected void setNbDec(int nbDec) {
        this.nbDec = nbDec;
    }
    
    /** 
     * 
     * @param fields nouveau tableau décrivant les champs
     */
    protected void setFields(SavotField[] fields) {
        this.fields = fields;
    }
    
    /**
     * 
     * @param expr nouveau tableau d'expressions
     */
    protected void setExpr(String[] expr) {
        this.expr = expr;
        // raz de parsers
        parsers = null;
    }
    
    /**
     * 
     * @param p nouveau plan catalogue sur lequel on ajoutera les colonnes
     */
    protected void setPlan(Plan p) {
        this.p = p;
    }
    
    /** Crée les différents parsers nécessaires
     * 
     * @return boolean <i>true</i> si tout est OK, <i>false</i> sinon
     */
    protected boolean createParser() {
        error = "";
        // on vérifie qu'aucun paramètre n'est null
        if( expr==null || fields==null || p==null ) {
            error = "One of the parameters was null";
            return false;
        }
        // vérification de la validité de chaque expression
        int i=0;
        parsers = new Expression[expr.length];
        try {
            for( i=0; i<expr.length; i++ ) {
                parsers[i] = new Expression(expr[i]);
                if( ! parsers[i].createParser() ) throw new ParserException();
                
                //parsers[i] = UCDFilter.createParser(expr[i].replace('$', ' ').trim(),a);
            }
        }
        catch(ParserException e) {
            //error = "Incorrect syntax for expression "+expr[i]+":\n"+parsers[i].error;
            error = parsers[i].error;
            return false;
        }
        
        return true;
    }
    
    protected String getError() {
        return error;
    }
    
    /** Effectue les différents calculs demandés
     * 
     * @return boolean
     */
    protected boolean compute() {
        // si parsers n'est pas null, la vérification a déja été faite
        if( parsers==null ) {
            if( !createParser() ) return false;
        }
        
        Source[] sources = getSources();
        if( sources==null ) return true;
        
        // on crée les Field nécessaires
        Field[] myFields = new Field[fields.length];
        for( int i=0; i<myFields.length; i++ ) {
            myFields[i] = new Field(fields[i].getName());
            
            myFields[i].arraysize = fields[i].getArraySize();
            // A VERIFIER
            myFields[i].datatype = "D";
            myFields[i].description = fields[i].getDescription();
            myFields[i].ID = fields[i].getId();
            myFields[i].precision = fields[i].getPrecision();
            //myFields[i].type = fields[i].getType();
            myFields[i].ucd = fields[i].getUcd();
            myFields[i].unit = fields[i].getUnit();
            //myFields[i].width = fields[i].getWidth();
            myFields[i].width = "10";
            myFields[i].computeColumnSize();
        }
        
        Source s;
        Expression parser;
        String value;
        //double start, end;
        //start = System.currentTimeMillis();
        
        // boucle sur toutes les sources
        for( int i=0; i<sources.length; i++ ) {
            s = sources[i];
            // boucle sur chaque parser
            for( int j=0; j<parsers.length; j++ ) {
                parser = parsers[j];
                //value = "";
                value = parser.eval(s, nbDec);
                //if( Action.setAllVariables(parser, s, false) ) value = Double.toString(parser.eval());
                addCol(myFields[j], value, s);
            }
        }
        //end = System.currentTimeMillis();
        //System.out.println("Total time : "+(end-start));
        
        a.view.setMesure();
        
        FilterProperties.majFilterProp(false, true);
        
        // log
        for( int i=0; i<parsers.length; i++ ) {
            a.log("newcolumn", "name="+fields[i].getName()+" expr="+expr[i]+
                      " catplane="+p.label);
        }
        
        return true;
    }
    
    /** Ajoute une colonne à une source donnée
     * @param field description de la nouvelle colonne
     * @param value valeur de la nouvelle colonne
     * @param s source
     */
    private void addCol(Field field, String value, Source s) {
        // on agrandit s.leg.field et on ajoute le nouveau champ si il n'a pas déja été créé
        if( s.getLeg().field[s.getLeg().field.length-1]==field ) {}
        else {
            Field[] newFields = new Field[s.getLeg().field.length+1];
            boolean[] newComputed = new boolean[s.getLeg().computed.length+1];
            int[] newTri = new int[s.getLeg().computed.length+1];
            System.arraycopy(s.getLeg().field, 0, newFields, 0, s.getLeg().field.length);
            System.arraycopy(s.getLeg().computed, 0, newComputed, 0, s.getLeg().computed.length);
            System.arraycopy(s.getLeg().fieldAt, 0, newTri, 0, s.getLeg().fieldAt.length);
            newFields[s.getLeg().field.length] = field;
            newComputed[s.getLeg().computed.length] = true;
            newTri[s.getLeg().computed.length] = s.getLeg().computed.length;
            s.getLeg().field = newFields;
            s.getLeg().computed = newComputed;
            s.getLeg().fieldAt = newTri;
        }
        
        // ajout de la nouvelle valeur
        s.info = new String(s.info+"\t"+value);
        // pb des colonnes vide qui engendrent un décalage
        s.fixInfo();
    }
    
    /** Retourne toutes les sources pour un plan donné
     * 
     * @param a
     * @return Source[]
     */
    private Source[] getSources() {
        Vector<Source> vec = new Vector<Source>();

        // loop on all plans and selection of catalogs which are active
        // we retrieve all sources in active plans
        Iterator<Obj> it = p.iterator();
        while( it.hasNext() ) {
           Obj o = it.next();
           if( !(o instanceof Source) ) continue;
           Source s = (Source)o;
           if( s!=null ) vec.addElement(s);
        }
        
        Source[] sources = new Source[vec.size()];
        vec.copyInto(sources);
        vec = null;

        return sources;
    }
    
    /** Arrondit et limite le nombre de décimales
     * @param d nombre à arrondir
     * @param nbDec nb de décimales à conserver
     * @return le nombre arrondi en conservant nbDec décimales
     */
    public static double round(double d, int nbDec) {
         if( d==Double.NEGATIVE_INFINITY || d==Double.POSITIVE_INFINITY ) return d;
         double fact = Math.pow(10,nbDec);
         return Math.round(d*fact)/fact;
     }
     
     public static String format(double d, int nbDec) {
     	
//     	 d = round(d, nbDec); // ???
     	 String s = Double.toString(d);
         // we keep only nbDec significant digits
         int sepIndex = s.indexOf(".");
         if( sepIndex>=0 ) {
             int expIndex = s.indexOf("E");
             // on cherche le minimum pour savoir ou s'arreter
             int end = s.length()>sepIndex+nbDec+1?sepIndex+nbDec+1:s.length();
             if( expIndex>=0 ) end = expIndex>end?end:expIndex;
             String saveText = new String(s);
             s = s.substring(0, end);
             if( expIndex>=0 ) s += saveText.substring(expIndex);
         }
         return s;
     }

    
    /** Une Expression est soit une expression arithmétique,
     *  soit qqch de la forme : condition ? expr1 : expr2
     */
    class Expression {
        String error = "";
        String s;
        UCDFilter condFilter;
        boolean conditional = false;
        Parser parser1, parser2;
        
        Expression(String s) {
            this.s = s;
        }
        
        boolean createParser() {
            // on vérifie si c'est une expr conditionnelle
            int qMarkIndex = s.indexOf('?');
            int dbPointIndex = s.lastIndexOf(':');
            if( qMarkIndex>=0 && dbPointIndex>=0 ) {
                conditional = true;
            }
            
            // expression conditionnelle
            if( !conditional ) {
                try {
                    parser1 = UCDFilter.createParser(s.replace('$', ' ').trim(),a);
                }
                catch(ParserException e) {
                    error = "Incorrect syntax for expression "+s;
                    return false;
                }
            }
            // pas de condition
            else {
                String condStr = s.substring(0,qMarkIndex);
                String parser1Str = s.substring(qMarkIndex+1, dbPointIndex);
                String parser2Str = s.substring(dbPointIndex+1);
                
                //System.out.println(condStr);
                //System.out.println(parser1Str);
                //System.out.println(parser2Str);
                
                condFilter = new UCDFilter("test", condStr+" {}", a, null);
                if( condFilter.badSyntax ) {
                    error = "Incorrect syntax for condition "+condStr;
                    return false;
                }
                
                try {
                    parser1 = UCDFilter.createParser(parser1Str.replace('$', ' ').trim(),a);
                }
                catch(ParserException e) {
                    error = "Incorrect syntax for expression "+parser1Str;
                    return false;
                }
                
                try {
                    parser2 = UCDFilter.createParser(parser2Str.replace('$', ' ').trim(),a);
                }
                catch(ParserException e) {
                    error = "Incorrect syntax for expression "+parser2Str;
                    return false;
                }
            }
            
            return true;
        }
        
        String eval(Source s, int nbDec) {
            String value = "";
            if( conditional ) {
                if( condFilter.verifyValueConstraints(s, 0) ) {
                    if( Action.setAllVariables(parser1, s, false) ) value = format(parser1.eval(), nbDec);
                }
                else {
                    if( Action.setAllVariables(parser2, s, false) ) value = format(parser2.eval(), nbDec);
                }
            }
            else {
                if( Action.setAllVariables(parser1, s, false) ) value = format(parser1.eval(), nbDec);
            }
            return value;
        }
    }
           
}
