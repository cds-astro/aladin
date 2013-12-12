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


package cds.xml;

import java.util.Hashtable;
import java.util.regex.Pattern;

import cds.tools.Util;

/** Field description according to the Astrores XML standard.
 *
 * @Version 1.0 - 02/09/99
 * @Author P.Fernique [CDS]
 * @Copyright ULP/CNRS 1999
 */
final public class Field {

   /** Numeric datatype keys */
   static private String NUMDATATYPE = "JKEFGDI";

   /** Field Identifier */
   public String ID;

   /** The name of the field */
   public String name;

   /** The description of the field */
   public String description;

   /** The title of the field */
   public String title;

   /** To characterize the field
       hidden   for fields used typically for server/client exchange
       no_query for fields which specify some parameters
                (e.g. the equinox of a coordinate system)
       trigger  for fields which contain a parameter for an action */
   public String type;

   /** Unit used for the field */
   public String unit;

   /** UCD associated with the field */
   public String ucd;

   /** Datatype of field value (F-float, D-double, K-long, I-integer, S-short, A-ascii,
       L-boolean (logical), E-exponential) */
   public String datatype;

   /** Number of characters - ex: 8 */
   public String width;

   /** Utype associated with the field */
   public String utype;

   /** Number of characters, extension for VOTable compatibility */
   public String arraysize;

   /** Precision of field value: number of significant digits
       after the dot (ex: "3") */
   public String precision;

   /** Template for the html link */
   public String href;

   /** Template for the GLU link */
   public String gref;

   /** Template for the text of the link */
   public String refText;

   /** link type */
   public String refValue;
   
   /** null value */
   public String nullValue;

   /** true if it is a RA or DE field */
//   public boolean coo;

   /** Column size (edition only) */
   public int columnSize=10;

   /** Positional field signature */
   static public final int RA=1,DE=2,PMRA=3,PMDE=4,X=5,Y=6;
   static public final String [] COOSIGN = { "", "RA","DE","PMRA","PMDE","X","Y" };
   public int coo;
   
   static public final int FREQ=1,FLUX=2,FLUXERR=3,SEDID=4;
   static public final String SEDLABEL[] = { "","SED_FREQ","SED_FLUX","SED_FLUXERR","SED_SEDID" };
   public int sed;

//   /** True if it is the DE coordinate field */
//   public boolean isDE;

   /** XML internal reference, typically for coordinate frame reference */
   public String ref;

   /** Current sort on this field */
   public int sort;

   /** Visible flag */
   public boolean visible=true;

   // Pattern pour l'extration de la valeur du champ en fonction d'une série d'expression régulière *.
   public String hpxFinderPattern;
   
   static public final int UNSORT = 0;
   static public final int SORT_ASCENDING  = 1;
   static public final int SORT_DESCENDING = 2;
//   static public final int HISTO = 3;

  /** Field object creation.
   * @param atts Hashtable of XML attributs (ID, UCD-ucd, format, unit, datatype, wdith, precision, type
   *
   */
   public Field(Hashtable atts) {
      ID       =(String)atts.get("ID");
      name     =(String)atts.get("name");
      unit     =(String)atts.get("unit");

      // UCD in Astrores and ucd in V0Table
      ucd      =(String)atts.get("UCD");
      if (ucd == null) ucd =(String)atts.get("ucd");

      utype = (String)atts.get("utype");

      datatype = typeVOTable2Fits((String)atts.get("datatype"));
      width    =(String)atts.get("width");
//      if( width==null ) width="10";
      precision=(String)atts.get("precision");
      type     =(String)atts.get("type");
      arraysize=(String)atts.get("arraysize");
      ref=(String)atts.get("ref");
      sort     = UNSORT;
      computeColumnSize();
   }

   /** Duplication */
   public Field(Field f) {
      description = f.description;
      ID = f.ID;
      name = f.name;
      unit = f.unit;
      ucd = f.ucd;
      utype = f.utype;
      datatype = f.datatype;
      width = f.width;
      precision = f.precision;
      type = f.type;
      arraysize = f.arraysize;
      href = f.href;
      gref = f.gref;
      refText = f.refText;
      refValue = f.refValue;
      ref = f.ref;
      coo = f.coo;
      title = f.title;
      sort = UNSORT;
      visible = f.visible;
      columnSize = f.columnSize;
   }

   /** Get the field edition size (width, otherwise arraysize, otherwise 10) */
   public void computeColumnSize() {
      int n=0;
      if( width!=null ) {
         try { n = Integer.parseInt(width); }
         catch( Exception e) { n=0; }
      }
      if( n==0 && arraysize!=null ) {
         try {
            if( !arraysize.endsWith("*") ) n = Integer.parseInt(arraysize);
            else n = Integer.parseInt(arraysize.substring(0, arraysize.length()-1));
         } catch( Exception e) { n=0; }
      }
      if( n==0 ) n=10;
      columnSize=n;
   }


   /** Retourne true si les champs sont identiques (càd même utype
    * ou bien même nom, mêmes unités, même UCD et même type de données */
   public boolean equals(Field f) {
      if( this==f ) return true;
      if( utype!=null && this.utype.equals(f.utype) ) return true;
      if( name!=null && !name.equals(f.name) ) return false;
      if( unit!=null && !unit.equals(f.unit) ) return false;
      if( ucd!=null && !ucd.equals(f.ucd) ) return false;
//      if( datatype!=null && !datatype.equals(f.datatype) ) return false;
      return true;
   }
   
   /** Retourne le tag de la colonne (RA, DE, PMRA, PMDE, X ou Y) */
   public int getFieldSignature() { return coo; }
   
   public boolean isRa()   { return coo==RA; }
   public boolean isDe()   { return coo==DE; }
   public boolean isPmRa() { return coo==PMRA; }
   public boolean isPmDe() { return coo==PMDE; }
   public boolean isX()    { return coo==X; }
   public boolean isY()    { return coo==Y; }
   
   /** Return the positional Field signature (RA, DE, PMRA, PMDE, X, Y ou "") */
   public String getCooSignature() { return COOSIGN[coo]; }

  /** Field object creation.
   * @param name Field name
   */
   public Field(String name) { this.name=name; }

 /** Add addtional informations.
   * @param name  type of the information (name, description or title)
   * @param value value of the information
   */
   public void addInfo(String name, String value) {
           if( name.equals("DESCRIPTION") )
                description=(description==null?"":description)+value;
      else if( name.equals("TITLE") )
                title=(title==null?"":title)+value;
      else if( name.equals("href") )     href=value;
      else if( name.equals("gref") )     gref=value;
      else if( name.equals("refText") )  refText=value;
      else if( name.equals("refValue") ) refValue=value;
      else if( name.equals("sed") )      setSEDtag(value);
      else return;
   }

   /** True si le field est numérique */
   public boolean isNumDataType() {
      return datatype!=null && NUMDATATYPE.indexOf(datatype)>=0;
   }
   
   /** Récupère le label du flag SED du champ, ou null si aucun sur ce champ */
   public String getSEDtag() {
      if( sed==0 || sed>=SEDLABEL.length ) return null;
      return SEDLABEL[sed];
   }
   
   /** Positionne le flag SED en fonction du label passé en paramètre (provient d'une lecture d'une fichier AJ) */
   private void setSEDtag(String tag) {
      int i = Util.indexInArrayOf(tag, SEDLABEL, true);
      if( i==-1 ) i=0;
      sed = i;
   }

   /** Conversion d'un type de donnée exprimé dans le standard FITS
    * en standard VOTable */
   static public String typeFits2VOTable(String s) {
      if( s==null ) return null;
      if( s.length()>1 ) return s;

      if( s.equals("J") ) return "int";
      if( s.equals("K") ) return "long";
      if( s.equals("A") ) return "char";
      if( s.equals("E") ) return "float";
      if( s.equals("D") ) return "double";
      if( s.equals("F") ) return "double";
      if( s.equals("G") ) return "double";
      if( s.equals("L") ) return "boolean";
      if( s.equals("I") ) return "short";
      if( s.equals("X") ) return "bit";
      if( s.equals("B") ) return "unsignedByte";
      if( s.equals("C") ) return "floatComplex";
      if( s.equals("M") ) return "doubleComplex";
      return "float";
   }

   /** Conversion d'un type de donnée exprimé dans le standard VOTable
    * en standard FITS */
   static public String typeVOTable2Fits(String s) {
      if( s==null ) return null;
      if( s.length()<2 ) return s;

      if( s.startsWith("int") )       return "J";
      if( s.equals("long") )          return "K";
      if( s.startsWith("char") )      return "A";
      if( s.equals("string") )        return "A";
      if( s.equals("float") )         return "E";
      if( s.equals("double") )        return "D";
      if( s.equals("boolean") )       return "L";
      if( s.equals("short") )         return "I";
      if( s.equals("bit") )           return "X";
      if( s.equals("unsignedByte") )  return "B";
      if( s.equals("floatComplex") )  return "C";
      if( s.equals("doubleComplex") ) return "M";
      return "E";
   }
   

   public String toString() {
      return  (ID==null?       "":" ID="+ID)
             +(name==null?     "":" name="+name)
             +(unit==null?     "":" unit="+unit)
             +(ucd==null?      "":" ucd="+ucd)
             +(utype==null?    "":" utype="+utype)
             +(datatype==null? "":" datatype="+datatype)
             +(precision==null?"":" precision="+precision)
             +(type==null?     "":" type="+type)
             +" coo="+getCooSignature()
             +(arraysize==null?"":" arraysize="+arraysize)
             ;
   }
}