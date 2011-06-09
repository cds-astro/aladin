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



package cds.savot.pull;

// java
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Hashtable;

// VOTable internal data model
import cds.savot.model.*;

// pull parser
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

/**
* <p>It has been tested with kXML Pull parser implementation </p>
* <p>but it is possible to use other pull parsers</p>
* <p>Designed to use with Pull parsers complient with Standard Pull Implementation v1</p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotPullParser {

  // the pull parser engine
  private SavotPullEngine engine = null;


  /**
   * Constructor
   * @param file a file to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
  */
  public SavotPullParser(String file, int mode) {
      this(file, mode, false);
  }

  /**
   * Constructor
   * @param file a file to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
  */
  public SavotPullParser(String file, int mode, boolean debug) {

    try {
      // new parser
      XmlPullParser parser = new KXmlParser();

      engine = new SavotPullEngine(parser, file, mode, debug);

      // parse the stream in the given mode
      if (mode == SavotPullEngine.FULL)
        engine.parse(parser, mode);
    } catch (IOException e){
      System.err.println(e);
    } catch (Exception f){
      System.err.println(f);
    }
  }

  /**
   * Constructor
   * @param url url to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
   * @param enc encoding (example : UTF-8)
   */
  public SavotPullParser(URL url, int mode, String enc) {
    this(url, mode, enc, false);
  }
  /**
   * Constructor
   * @param url url to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
   * @param enc encoding (example : UTF-8)
   */
  public SavotPullParser(URL url, int mode, String enc, boolean debug) {

    try {
      // new parser
      KXmlParser parser = new KXmlParser();

      engine = new SavotPullEngine(parser, url, mode, enc, debug);

      // parse the stream in the given mode
      if (mode == SavotPullEngine.FULL)
        engine.parse(parser, mode);

    } catch (IOException e){
      System.err.println(e);
    }
    catch (Exception f){
      System.err.println(f);
    }
   }

   /**
    * Constructor
    * @param instream stream to parse
    * @param mode FULL or SEQUENTIAL (for small memory size applications)
    * @param enc encoding (example : UTF-8)
    */
     public SavotPullParser(InputStream instream, int mode, String enc) {
       this(instream, mode, enc, false);
     }
  /**
   * Constructor
   * @param instream stream to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
   * @param enc encoding (example : UTF-8)
   */
    public SavotPullParser(InputStream instream, int mode, String enc, boolean debug) {
      try {
        // new parser
        KXmlParser parser = new KXmlParser();

        engine = new SavotPullEngine(parser, instream, mode, enc, debug);

        // parse the stream in the given mode
        if (mode == SavotPullEngine.FULL)
          engine.parse(parser, mode);

      } catch (IOException e){
        System.err.println(e);
      }
      catch (Exception f){
        System.err.println(f);
      }
   }

  /**
   * Get the next Resource (sequential mode only)
   * @return a SavotResource
   */
  public SavotResource getNextResource() {
      return engine.getNextResource();
  }

  /**
   * Get a reference to V0TABLE object
   * @return SavotVOTable
   */
  public SavotVOTable getVOTable() {
    return engine.getAllResources();
  }

  /**
  * Get the number of RESOURCE elements in the document (for statistics)
  * @return a long value
  */
  public long getResourceCount() {
    return engine.getResourceCount();
  }

  /**
  * Get the number of TABLE elements in the document (for statistics)
  * @return a long value
  */
  public long getTableCount() {
    return engine.getTableCount();
  }

  /**
  * Get the number of TR elements in the document (for statistics)
  * @return a long value
  */
  public long getTRCount() {
    return engine.getTRCount();
  }

  /**
  * Get the number of DATA elements in the document (for statistics)
  * @return a long value
  */
  public long getDataCount() {
    return engine.getDataCount();
  }

  /**
   * Get a reference on the Hashtable containing the link between ID and ref
   * @return a refernce to the Hashtable
   */
  public Hashtable getIdRefLinks() {
    return engine.getIdRefLinks();
  }

  /**
   * Search a RESOURCE corresponding to an ID ref
   * @param ref
   * @return a reference to a SavotResource object
   */
  public SavotResource getResourceFromRef(String ref) {
    return engine.getResourceFromRef(ref);
  }

  /**
   * Search a FIELD corresponding to an ID ref
   * @param ref
   * @return SavotField
   */
  public SavotField getFieldFromRef(String ref) {
    return engine.getFieldFromRef(ref);
  }

  /**
   * Search a PARAM corresponding to an ID ref
   * @param ref
   * @return SavotParam
   */
  public SavotParam getParamFromRef(String ref) {
    return engine.getParamFromRef(ref);
  }

  /**
   * Search a TABLE corresponding to an ID ref
   * @param ref
   * @return SavotTable
   */
  public SavotTable getTableFromRef(String ref) {
    return engine.getTableFromRef(ref);
  }

  /**
   * Search a RESOURCE corresponding to an ID ref
   * @param ref
   * @return SavotInfo
   */
  public SavotInfo getInfoFromRef(String ref) {
    return engine.getInfoFromRef(ref);
  }

  /**
   * Search a VALUES corresponding to an ID ref
   * @param ref
   * @return SavotValues
   */
  public SavotValues getValuesFromRef(String ref) {
    return engine.getValuesFromRef(ref);
  }

  /**
   * Search a LINK corresponding to an ID ref
   * @param ref
   * @return SavotLink
   */
  public SavotLink getLinkFromRef(String ref) {
    return engine.getLinkFromRef(ref);
  }

  /**
   * Search a COOSYS corresponding to an ID ref
   * @param ref
   * @return SavotCoosys
   */
  public SavotCoosys getCoosysFromRef(String ref) {
    return engine.getCoosysFromRef(ref);
  }

  /**
   * Get all resources
   * @return SavotVOTable
   */
  public SavotVOTable getAllResources() {
    return engine.getAllResources();
  }

  /**
   * Get Parser Version
   * @return String
   */
  public String getVersion() {
    return engine.SAVOTPARSER;
  }

  /**
   * For test only
   *
   */
  public void sequentialTester() {
    SavotResource currentResource = new SavotResource();
    do {
      currentResource = engine.getNextResource();
    }
    while ( currentResource != null );
  }

  /**
   * Enable debug mode
   * @param debug boolean
   */
  public void enableDebug(boolean debug) {
    engine.enableDebug(debug);
  }

  /** Main
    *
    * @param argv
    * @throws IOException
    */
  public static void main  (String [] argv) throws IOException {

    if (argv.length == 0)
      System.out.println("Usage: java SavotPullParser <xml document>");
    else {
      new SavotPullParser(argv[0], SavotPullEngine.FULL);
    }
  }
}
