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

package cds.xml;

/**
 * Interface of an TableParser event consumer.
 * This interface is used by TableParser object when it parses
 * A table (TSV,CSV,Astrores,VOTable,VOTable with CSV).
 *
 * These documents describes astronomical resources.
 * Astronomical resources means in this context:
 *    . excerpt tables or complete tables
 *    . lists of table descriptions
 *    . lists of server descriptions
 * The aim is to give enough knowledge to the user interfaces
 * to manipulate these resources (units, formats, anchors, natural
 * language descriptions, nomenclatures, value ranges...).
 *
 * Example of usage :
 *
 * <PRE>
 * import cds.xml.*;
 * import java.io.*;
 * import java.util.*;
 *
 * public class TableParserDemo implements TableParserConsumer {
 *    TableParser ap;
 *
 *    // Create and launch the TableParser parsing
 *    TableParserDemo(DataInputStream dis) {
 *       ap = new TableParser(this);
 *       if( !ap.parse(dis) ) System.err.println( ap.getError() );
 *    }
 *
 *    // Method called at the beginning of a resource
 *    public void startResource(String ID) {
 *       System.out.println("Resource: "+ID);
 *    }
 *
 *    // Method called to give additionnal information about the current resource
 *    public void setResourceInfo(String name,String value) {
 *       System.out.println("   .Resource info: "+name+"="+value);
 *    }
 *
 *    // Method called at the end of a resource
 *    public void endResource() {
 *       System.out.println("End resource");
 *    }
 *
 *    // Method called at the beginning of a table
 *    public void startTable(String ID) {
 *       System.out.println("   - Table: "+ID);
 *    }
 *
 *    // Method called to give additionnal information about the current table
 *    public void setTableInfo(String name,String value) {
 *       System.out.println("      .Table info: "+name+"="+value);
 *    }
 *
 *    // Method called at the end of a table
 *    public void endTable() {
 *       System.out.println("    End table");
 *    }
 *
 *    // Method called to transmit a field description from the current table
 *    // See the Field class attributs
 *    public void setField(Field f) {
 *       System.out.println("      .Field: "+f.name+": "+f.description);
 *    }
 *
 *    // Method called to transmit a record from the current table
 *    public void setRecord(double ra,double dec,String [] field) {
 *       System.out.print("      .Record: ("+ra+","+dec+") ");
 *       for( int i=0; i<3 && i<field.length; i++ ) System.out.print(" "+field[i]);
 *       System.out.println("...");
 *    }
 *
 *    // Method called to transmit a target information
 *    public void setTarget(String target) {
 *       System.out.println("Target: "+target);
 *    }
 *
 *    // The main method to test it
 *    static public void main(String [] arg) {
 *       try {
 *          // Open the first arg as a file
 *          DataInputStream dis = new DataInputStream( new FileInputStream(arg[0]));
 *
 *          // Parse the file
 *          new TableParserDemo(dis);
 *
 *       } catch( Exception e ) {
 *          System.err.println("There is a problem: "+e);
 *          e.printStackTrace();
 *       }
 *    }
 * }
 * </PRE>
 *
 * @version 1.0 10/5/05 Rename
 * @version 1.0 3/09/99 Creation
 * @author P.Fernique [CDS]
 * @Copyright ULP/CNRS 1999-2005
 */
public abstract interface TableParserConsumer {

  /** This method is called by the TableParser parser when the XML
   * tag <RESOURCE> is encountered.
   * @param ID the ID of the resource, or null if there isn't
   */
   public abstract void startResource(String ID);

  /** This method is called by the TableParser parser when elements
   * in a RESOURCE tag are encountered (ex: &lt;TITLE>...contain...&lt;/TITLE>)
   * @param name The name of the embedded element
   * @param contain The contain of the embedded element
   */
   public abstract void setResourceInfo(String name,String contain);

  /** This method is called by the TableParser parser when the XML
   * /RESOURCE is encountered.
  */
   public abstract void endResource();

  /** This method is called by the TableParser parser when the XML
   * tag TABLE is encountered.
   * @param ID the ID of the resource, or null if there isn't
   */
   public abstract void startTable(String ID);

  /** This method is called by the TableParser parser when elements
   * in a TABLE tag are encountered (ex: &lt;TITLE>...contain...&lt;/TITLE>)
   * @param name The name of the embedded element
   * @param value The contain of the embedded element
   */
   public abstract void setTableInfo(String name,String value);

  /** This method is called by the TableParser parser when the XML
   * </TABLE> is encountered.
   */
   public abstract void endTable();
   
   /**
    * This method is called by the TableParser for transmitting
    * the RA and DEC column indices found by the parse
    * @param nRa RA column index (0=first column)
    * @param nDec DEC column index (0=first column)
    * @param nPmRa PMRA column index (0=first column)
    * @param nPmDec PMDEC column index (0=first column)
    * @param nX X column index (0=first column)
    * @param nY Y column index (0=first column)
    */
   public abstract void setTableRaDecXYIndex(int nRa, int nDec,int nPmRa, int nPmDec, int nX, int nY, boolean badDetection);

  /** This method is called by the TableParser parser when the XML
   * /FIELD is encountered (the end of the element FIELD).
   * ==> See Field object to have more details
   * @param f the Field
   */
   public abstract void setField(Field f);

  /** This method is called by the TableParser parser for each line
   * of a DATA element.
   * @param ra the position of the associated astronomical object 
   * @param dec
   * @param jdtime an time-stamp associated to the associated astronomical object 
   * @param field each field of the line
   */
   public abstract void setRecord(double ra,double dec,double jdtime, String [] field);

  /** This method is called by the TableParser parser when the XML
   * tag INFO ID="Target"... is encountered.
   * @param target the target of the table in ASU format
   *       (ex:001.286804+67.840004,bm=14.100000/14.1)
   */
   public abstract void setTarget(String target);
   
   /** This method is called by the TableParserConsumer for
    * delivering not crucial error
    */
   public abstract void tableParserWarning(String msg);
   
   /** This method is called by the TableParserConsumer for
    * delivering parsing information
    */
   public abstract void tableParserInfo(String msg);
   
   /** This method is called by the TableParserConsumer for
    * delivering filter information
    */
   public abstract void setFilter(String filter);
}

