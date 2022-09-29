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

package cds.xml;

import java.util.Hashtable;

/**
 * Interface of an XML event consumer.
 *
 * Rq: This interface has been inspired by SAX interface. The only difference
 * is that the tag parameter are memorized in a java Hashtable object
 * instead of the AttrList SAX object.
 *
 *
 * Example of usage :
 *
 * <PRE>
 *
 * import cds.xml.*;
 * import java.io.*;
 * import java.util.*;
 *
 * // Simple class to test the XML parser.
 * // Usage: java ParseDemo file.xml
 * public class ParseDemo implements XMLConsumer {
 *    XMLParser xp;
 *
 *    // Creat and launch the XML parsing
 *    ParseDemo(DataInputStream dis) {
 *       xp = new XMLParser(this);
 *       if( !xp.parse(dis) ) System.err.println( xp.getError() );
 *    }
 *
 *    // Method called for each start XML tag
 *    public void startElement(String name, Hashtable atts) {
 *       System.out.println("Begins tag: "+name);
 *
 *       Enumeration e = atts.keys();
 *       while( e.hasMoreElements() ) {
 *          String s = (String)e.nextElement();
 *          System.out.println("   ."+s+" = "+(String)atts.get(s) );
 *       }
 *
 *      if( xp.in("RESOURCE TABLE") )
 *         System.out.println("==> in RESOURCE TABLE");
 *    }
 *
 *    // Method called for each end XML tag
 *    public void endElement(String name) {
 *       System.out.println("Ends tag: "+name);
 *    }
 *
 *    // Method called to send the contain of the current XML tag
 *    public void characters(char [] ch, int start, int lenght) {
 *       System.out.println("tag contain: ["+ (new String(ch,start,lenght)) +"]");
 *    }
 *
 *    // The main method to test it
 *    static public void main(String [] arg) {
 *       try {
 *          // Open the first arg as a file
 *          DataInputStream dis = new DataInputStream( new FileInputStream(arg[0]));
 *
 *          // Parse the file
 *          new ParseDemo(dis);
 *
 *       } catch( Exception e ) {
 *          System.err.println("There is a problem: "+e);
 *          e.printStackTrace();
 *       }
 *    }
 * }
 * </PRE>
 *
 * @version 1.0 3 sep 99 Creation
 * @author P.Fernique [CDS]
 * @Copyright ULP/CNRS 1999
 */
public abstract interface XMLConsumer {

  /** This method is called by the XML parser when it reaches an
   * XML tag (ex: &lt;TAGNAME paramname=paramvalue ...>)
   * Rq: For the specials tags &lt;TAGNAME .../>, this method is always
   * called before the endElement() in order to transmit the eventual
   * parameters.
   * @param name The tag name (TAGNAME in the example)
   * @param atts The tag parameters in an Hashtable. The keys of the
   *             hashtable are the param name.
   */
   public abstract void startElement(String name,Hashtable atts);

  /** This method is called by the XML parser when it reaches an end
   * XML tag (ex: &lt;/TAGNAME> or &lt;TAGNAME .../>)
   * @param name The tag name (TAGNAME in the example)
   */
   public abstract void endElement(String name);

  /** This method is called by the XML parser to transmit the contain
   * of the current XML tag (ex: &lt;TAG> ... the contain ... &lt;/TAG>)
   * Rq: To know the tag name associated, the XML parser implements
   *     the Stack getStack() method. This stack is formed by the
   *     list of hierarchical tag names
   * @param ch The array of char
   * @param start the index of the first character
   * @param length the length of the contain
   * @throws Exception
   */
   public abstract void characters(char [] ch,int start,int length) throws Exception;
}
