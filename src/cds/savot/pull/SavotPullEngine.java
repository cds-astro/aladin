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
import java.io.*;
import java.net.URL;
import java.util.Vector;
import java.util.Hashtable;

// kXML packages
import org.kxml2.io.*;
import org.xmlpull.v1.*;

// VOTable internal data model
import cds.savot.model.*;

/**
* <p>It has been tested with kXML Pull parser implementation </p>
* <p>but it is possible to use other pull parsers</p>
* <p>Designed to use with Pull parsers complient with Standard Pull Implementation v1</p>
*
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotPullEngine implements cds.savot.common.Markups {

  // parsing mode
  public static int FULL = 0;
  public static int SEQUENTIAL = 1;

  // data model global objects
  private SavotVOTable currentVOTable = new SavotVOTable();
  private SavotResource currentResource = new SavotResource();

  // used for statistics
  private long rowCounter = 0;
  private long resourceCounter = 0;
  private long tableCounter = 0;
  private long dataCounter = 0;
  private boolean trace = false;

  // used for recursive management
  Vector father = new Vector();

  // used for recursive resources, LIFO mode
  private Vector resourcestack = new Vector();

  // used for recursive options, LIFO mode
  private Vector optionstack = new Vector();

  // used for recursives groups, LIFO mode
  private Vector groupstack = new Vector();

  /**
   * Hashtable containing object references which have an ID
   * So it is possible to retrieve such object reference
   * Used to resolve ID ref
   */
  public Hashtable idRefLinks = new Hashtable();

  // needed for sequential parsing
  protected XmlPullParser parser = null;

  /**
   * Constructor
   * @param parser
   * @param file a file to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
   */
  public SavotPullEngine(XmlPullParser parser, String file, int mode, boolean debug) {

    try {
      this.parser = parser;
      enableDebug(debug);

      // set the input of the parser
      FileInputStream inStream = new FileInputStream(new File(file));
      BufferedInputStream dataBuffInStream = new BufferedInputStream(inStream);

      parser.setInput(dataBuffInStream, "UTF-8");

      // parser the stream in the given mode
      if (mode == SavotPullEngine.FULL)
        parse(parser, mode);

    } catch (IOException e){
      System.err.println("Exception SavotPullEngine : "+ e);
    } catch (Exception f){
      System.err.println("Exception SavotPullEngine : "+ f);
    }
  }

  /**
   * Constructor
   * @param parser
   * @param url url to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
   * @param enc encoding (example : UTF-8)
   */
  public SavotPullEngine(XmlPullParser parser, URL url, int mode, String enc, boolean debug) {

    try {
      this.parser = parser;
      enableDebug(debug);
      // set the input of the parser (with the given encoding)
      parser.setInput(new DataInputStream(url.openStream()), enc);

      // parser the stream in the given mode
      if (mode == SavotPullEngine.FULL)
        parse(parser, mode);

    } catch (IOException e){
      System.err.println("Exception SavotPullEngine : "+ e);
    }
    catch (Exception f){
      System.err.println("Exception SavotPullEngine : "+ f);
    }
  }

  /**
   * Constructor
   * @param parser
   * @param instream stream to parse
   * @param mode FULL or SEQUENTIAL (for small memory size applications)
   * @param enc encoding (example : UTF-8)
   */
  public SavotPullEngine(XmlPullParser parser, InputStream instream, int mode, String enc, boolean debug) {
    //      public SavotPullEngine(XmlPullParser parser, InputStream instream, int mode, String enc) {
    try {
      this.parser = parser;
      enableDebug(debug);

      // DataInputStream dataInStream = new DataInputStream(instream);
      BufferedInputStream dataBuffInStream = new BufferedInputStream(instream);

      // set the input of the parser (with the given encoding)
      //        parser.setInput(new DataInputStream(instream), enc);
      parser.setInput(dataBuffInStream, enc);

      // parser the stream in the given mode
      if (mode == SavotPullEngine.FULL)
        parse(parser, mode);

    } catch (IOException e){
      System.err.println("Exception SavotPullEngine : "+ e);
    }
    catch (Exception f){
      System.err.println("Exception SavotPullEngine : "+ f);
    }
  }

  /**
   * Reset of the engine before another parsing
   */
  public void reset() {
    // data model global classes
    currentVOTable = new SavotVOTable();
    currentResource = new SavotResource();
    rowCounter = 0;
    resourceCounter = 0;
    tableCounter = 0;
    dataCounter = 0;
    idRefLinks.clear();
    // used for recursive resources, LIFO mode
    resourcestack.removeAllElements();
    // used for recursive options, LIFO mode
    optionstack.removeAllElements();
    // used for recursive groups, LIFO mode
    groupstack.removeAllElements();
  }

  /**
   * Put a resource on the resourcestack
   * @param res
   */
  private void putResourceStack(SavotResource res) {
    resourcestack.addElement(res);
  }

  /**
   * Put an option on the optionstack
   * @param res
   */
  private void putOptionStack(SavotOption res) {
    optionstack.addElement(res);
  }

  /**
   * Put a group on the groupstack
   * @param res
   */
  private void putGroupStack(SavotGroup res) {
    groupstack.addElement(res);
  }

  /**
   * Get the last element from the resourcestack
   * @return SavotResource
   */
  private SavotResource getResourceStack() {
    SavotResource res = (SavotResource)resourcestack.lastElement();
    resourcestack.removeElementAt(resourcestack.size() - 1);
    return res;
  }

  /**
   * Get the last element from the optionstack
   * @return SavotOption
   */
  private SavotOption getOptionStack() {
    SavotOption res = (SavotOption)optionstack.lastElement();
    optionstack.removeElementAt(optionstack.size() - 1);
    return res;
  }

  /**
   * Get the last element from the groupstack
   * @return SavotGroup
   */
  private SavotGroup getGroupStack() {
    SavotGroup res = (SavotGroup)groupstack.lastElement();
    groupstack.removeElementAt(groupstack.size() - 1);
    return res;
  }

  /**
   * Parsing engine
   * @param parser an XML pull parser (example : kXML)
   * @param parsingType mode FULL or SEQUENTIAL
   * @return SavotResource
   * @throws IOException
   *
   */
  public SavotResource parse (XmlPullParser parser, int parsingType) throws IOException {
    try {
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
    } catch (XmlPullParserException e1) {
        e1.printStackTrace();
    }
    String name = new String();
    String currentMarkup = "XML";

    // used for sequential parsing
    boolean resourceComplete = false;

    // for multi level resource
    int includedResource = 0;
    // for multi level option
    int includedOption = 0;
    // for multi level group
    int includedGroup = 0;

    SavotTable currentTable = new SavotTable();
    SavotField currentField = new SavotField();
    SavotFieldRef currentFieldRef = new SavotFieldRef();
    SavotGroup currentGroup = new SavotGroup(); /* new in VOTable 1.1 */
    SavotParam currentParam = new SavotParam();
    SavotParamRef currentParamRef = new SavotParamRef();
    SavotTR currentTR = new SavotTR();
    SavotTD currentTD = new SavotTD();
    SavotData currentData = new SavotData();
    SavotValues currentValues = new SavotValues();
    SavotTableData currentTableData = new SavotTableData();
    String currentDescription = new String();
    SavotLink currentLink = new SavotLink();
    SavotInfo currentInfo = new SavotInfo();
    SavotMin currentMin = new SavotMin();
    SavotMax currentMax = new SavotMax();
    SavotOption currentOption = new SavotOption();
    SavotCoosys currentCoosys = new SavotCoosys();
    SavotDefinitions currentDefinitions = new SavotDefinitions();
    SavotBinary currentBinary = new SavotBinary();
    SavotFits currentFits = new SavotFits();
    SavotStream currentStream = new SavotStream();

    try {

      // envent type
      int eventType = parser.getEventType();

      // while the end of the document is not reach
      while (eventType != parser.END_DOCUMENT) {
        // treatment depending on event type
        switch (eventType) {
          // if a start tag is reach
          case KXmlParser.START_TAG:
            try {
              // the name of the current tag
              name = parser.getName();

              if (trace) System.err.println("Name ---> " + parser.getName());

              if (name != null) {

                // VOTABLE
                if (name.equalsIgnoreCase(VOTABLE)) {
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, VERSION) != null)
                      currentVOTable.setVersion(parser.getAttributeValue(null, VERSION));

                    if (parser.getAttributeValue(null, XMLNSXSI) != null)
                      currentVOTable.setXmlnsxsi(parser.getAttributeValue(null, XMLNSXSI));

                    if (parser.getAttributeValue(null, XSINOSCHEMA) != null)
                      currentVOTable.setXsinoschema(parser.getAttributeValue(null, XSINOSCHEMA));

                    if (parser.getAttributeValue(null, XSISCHEMA) != null)
                      currentVOTable.setXsischema(parser.getAttributeValue(null, XSISCHEMA));

                    if (parser.getAttributeValue(null, XMLNS) != null)
                      currentVOTable.setXmlns(parser.getAttributeValue(null, XMLNS));

                    if (parser.getAttributeValue(null, ID) != null)
                      currentVOTable.setId(parser.getAttributeValue(null, ID));
                  }
                  if (trace) System.err.println ("VOTABLE begin");
                  currentMarkup = VOTABLE;
                }
                else // DESCRIPTION
                if (name.equalsIgnoreCase(DESCRIPTION)) {
                  currentMarkup = DESCRIPTION;
                  if (trace) System.err.println ("DESCRIPTION begin");
                } //
                else if (name.equalsIgnoreCase(RESOURCE)) {

                  if (includedResource > 0) {
                    // inner case (multi level resources)
                    putResourceStack(currentResource);
                    if (trace) System.err.println ("RESOURCE - included");
                  }
                  else
                  if (trace) System.err.println ("RESOURCE - not included");
                  includedResource++;

                  if (trace == true) System.err.println ("RESOURCE begin");
                  currentMarkup = RESOURCE;

                  // for statistics only
                  resourceCounter++;

                  if (parsingType == FULL || currentResource == null)
                    currentResource = new SavotResource();
                  else
                    currentResource.init();

                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, NAME) != null)
                      currentResource.setName(parser.getAttributeValue(null, NAME));
                    if (parser.getAttributeValue(null, TYPE) != null)
                      currentResource.setType(parser.getAttributeValue(null, TYPE));
                    else
                      currentResource.setType(""); // correct the "results" default value
                    if (parser.getAttributeValue(null, UTYPE) != null)
                      currentResource.setUtype(parser.getAttributeValue(null, UTYPE));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentResource.setId(parser.getAttributeValue(null, ID));
                      if (parsingType == FULL)
                        idRefLinks.put(parser.getAttributeValue(null, ID), currentResource);
                    }
                  }
                }
                // TABLE
                else if (name.equalsIgnoreCase(TABLE)) {
                  currentTable = new SavotTable();
                  currentMarkup = TABLE;
                  if (trace) System.err.println("on passe dans Name ---> " + "TABLE");

                  // for statistics only
                  tableCounter++;

                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, NAME) != null)
                      currentTable.setName(parser.getAttributeValue(null, NAME));
                    if (parser.getAttributeValue(null, UCD) != null) // new since VOTable 1.1
                      currentTable.setUcd(parser.getAttributeValue(null, UCD));
                    if (parser.getAttributeValue(null, UTYPE) != null) // new since VOTable 1.1
                      currentTable.setUtype(parser.getAttributeValue(null, UTYPE));
                    if (parser.getAttributeValue(null, REF) != null)
                      currentTable.setRef(parser.getAttributeValue(null, REF));
                    if (parser.getAttributeValue(null, NROWS) != null) // new since VOTable 1.1
                      currentTable.setNrows(parser.getAttributeValue(null, NROWS));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentTable.setId(parser.getAttributeValue(null, ID));
                      if (parsingType == FULL) {
                        idRefLinks.put(parser.getAttributeValue(null, ID), currentTable);
                        if (trace) System.err.println (parser.getAttributeValue(null, ID));
                      }
                    }
                  }
                } // FIELD
                else if (name.equalsIgnoreCase(FIELD)) {
                  currentField = new SavotField();
                  currentMarkup = FIELD;
                  if (trace) System.err.println("on passe dans Name ---> " + "FIELD");
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, UNIT) != null)
                      currentField.setUnit(parser.getAttributeValue(null, UNIT));
                    if (parser.getAttributeValue(null, DATATYPE) != null)
                      currentField.setDataType(parser.getAttributeValue(null, DATATYPE));
                    if (parser.getAttributeValue(null, PRECISION) != null)
                      currentField.setPrecision(parser.getAttributeValue(null, PRECISION));
                    if (parser.getAttributeValue(null, WIDTH) != null)
                      currentField.setWidth(parser.getAttributeValue(null, WIDTH));
                    if (parser.getAttributeValue(null, REF) != null)
                      currentField.setRef(parser.getAttributeValue(null, REF));
                    if (parser.getAttributeValue(null, NAME) != null)
                      currentField.setName(parser.getAttributeValue(null, NAME));
                    if (parser.getAttributeValue(null, UCD) != null)
                      currentField.setUcd(parser.getAttributeValue(null, UCD));
                    if (parser.getAttributeValue(null, ARRAYSIZE) != null)
                      currentField.setArraySize(parser.getAttributeValue(null, ARRAYSIZE));
                    if (parser.getAttributeValue(null, TYPE) != null) // deprecated since VOTable 1.1
                      currentField.setType(parser.getAttributeValue(null, TYPE));
                    if (parser.getAttributeValue(null, UTYPE) != null)
                      currentField.setUtype(parser.getAttributeValue(null, UTYPE));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentField.setId(parser.getAttributeValue(null, ID));
                      idRefLinks.put(parser.getAttributeValue(null, ID), currentField);
                    }
                  }
                } // FIELDREF
                else if (name.equalsIgnoreCase(FIELDREF)) {
                  currentFieldRef = new SavotFieldRef();
                  currentMarkup = FIELDREF;
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, REF) != null)
                      currentFieldRef.setRef(parser.getAttributeValue(null, REF));
                  }
                } // VALUES
                else if (name.equalsIgnoreCase(VALUES)) {
                  currentValues = new SavotValues();
                  currentMarkup = VALUES;
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, TYPE) != null)
                      currentValues.setType(parser.getAttributeValue(null, TYPE));
                    if (parser.getAttributeValue(null, NULL) != null)
                      currentValues.setNull(parser.getAttributeValue(null, NULL));
                    if (parser.getAttributeValue(null, INVALID) != null) /* deprecated since VOTable 1.1 */
                      currentValues.setInvalid(parser.getAttributeValue(null, INVALID));
                    if (parser.getAttributeValue(null, REF) != null)
                      currentValues.setRef(parser.getAttributeValue(null, REF));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentValues.setId(parser.getAttributeValue(null, ID));
                      idRefLinks.put(parser.getAttributeValue(null, ID), currentValues);
                    }
                  }
                } // STREAM
                else if (name.equalsIgnoreCase(STREAM)) {
                  currentStream = new SavotStream();
                  currentMarkup = STREAM;
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, TYPE) != null)
                      currentStream.setType(parser.getAttributeValue(null, TYPE));
                    if (parser.getAttributeValue(null, HREF) != null)
                      currentStream.setHref(parser.getAttributeValue(null, HREF));
                    if (parser.getAttributeValue(null, ACTUATE) != null)
                      currentStream.setActuate(parser.getAttributeValue(null, ACTUATE));
                    if (parser.getAttributeValue(null, ENCODING) != null)
                      currentStream.setEncoding(parser.getAttributeValue(null, ENCODING));
                    if (parser.getAttributeValue(null, EXPIRES) != null)
                      currentStream.setExpires(parser.getAttributeValue(null, EXPIRES));
                    if (parser.getAttributeValue(null, RIGHTS) != null)
                      currentStream.setRights(parser.getAttributeValue(null, RIGHTS));
                  }
                } // TR
                else if (name.equalsIgnoreCase(TR)) {
                  if (trace) System.err.println ("TR begin");
                  currentMarkup = TR;

                  // create a new row
                  currentTR = new SavotTR();
                  currentTR.setLineInXMLFile(parser.getLineNumber());
                } // TD
                else if (name.equalsIgnoreCase(TD)) {
                  if (trace) System.err.println ("TD begin");
                  currentMarkup = TD;

                  // create a new data
                  currentTD = new SavotTD();
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, ENCODING) != null) /* new 1.1 */
                      currentTD.setEncoding(parser.getAttributeValue(null, ENCODING));
                  }

                  // for statistics only
                  dataCounter++;
                } // DATA
                else if (name.equalsIgnoreCase(DATA)) {
                  currentData = new SavotData();
                  currentMarkup = DATA;
                } // BINARY
                else if (name.equalsIgnoreCase(BINARY)) {
                  currentBinary = new SavotBinary();
                  currentMarkup = BINARY;
                } // FITS
                else if (name.equalsIgnoreCase(FITS)) {
                  currentFits = new SavotFits();
                  currentMarkup = FITS;
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, EXTNUM) != null)
                      currentFits.setExtnum(parser.getAttributeValue(null, EXTNUM));
                  }
                } // TABLEDATA
                else if (name.equalsIgnoreCase(TABLEDATA)) {
                  currentTableData = new SavotTableData();
                  currentMarkup = TABLEDATA;
                } // PARAM
                else if (name.equalsIgnoreCase(PARAM)) {
                  currentParam = new SavotParam();
                  currentMarkup = PARAM;
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, UNIT) != null)
                      currentParam.setUnit(parser.getAttributeValue(null, UNIT));
                    if (parser.getAttributeValue(null, DATATYPE) != null)
                      currentParam.setDataType(parser.getAttributeValue(null, DATATYPE));
                    if (parser.getAttributeValue(null, PRECISION) != null)
                      currentParam.setPrecision(parser.getAttributeValue(null, PRECISION));
                    if (parser.getAttributeValue(null, WIDTH) != null)
                      currentParam.setWidth(parser.getAttributeValue(null, WIDTH));
                    if (parser.getAttributeValue(null, REF) != null)
                      currentParam.setRef(parser.getAttributeValue(null, REF));
                    if (parser.getAttributeValue(null, NAME) != null)
                      currentParam.setName(parser.getAttributeValue(null, NAME));
                    if (parser.getAttributeValue(null, UCD) != null)
                      currentParam.setUcd(parser.getAttributeValue(null, UCD));
                    if (parser.getAttributeValue(null, UTYPE) != null)
                      currentParam.setUtype(parser.getAttributeValue(null, UTYPE));
                    if (parser.getAttributeValue(null, VALUE) != null)
                      currentParam.setValue(parser.getAttributeValue(null, VALUE));
                    if (parser.getAttributeValue(null, ARRAYSIZE) != null)
                      currentParam.setArraySize(parser.getAttributeValue(null, ARRAYSIZE));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentParam.setId(parser.getAttributeValue(null, ID));
                      idRefLinks.put(parser.getAttributeValue(null, ID), currentParam);
                    }
                  }
                } // PARAMREF
                else if (name.equalsIgnoreCase(PARAMREF)) {
                  currentParamRef = new SavotParamRef();
                  currentMarkup = PARAMREF;
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, REF) != null)
                      currentParamRef.setRef(parser.getAttributeValue(null, REF));
                  }
                }
                // LINK
                else if (name.equalsIgnoreCase(LINK)) {
                  currentLink = new SavotLink();
                  currentMarkup = LINK;

                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, CONTENTROLE) != null)
                      currentLink.setContentRole(parser.getAttributeValue(null, CONTENTROLE));
                    if (parser.getAttributeValue(null, CONTENTTYPE) != null)
                      currentLink.setContentType(parser.getAttributeValue(null, CONTENTTYPE));
                    if (parser.getAttributeValue(null, TITLE) != null)
                      currentLink.setTitle(parser.getAttributeValue(null, TITLE));
                    if (parser.getAttributeValue(null, VALUE) != null)
                      currentLink.setValue(parser.getAttributeValue(null, VALUE));
                    if (parser.getAttributeValue(null, HREF) != null)
                      currentLink.setHref(parser.getAttributeValue(null, HREF));
                    if (parser.getAttributeValue(null, GREF) != null) // deprecated since VOTable 1.1
                      currentLink.setGref(parser.getAttributeValue(null, GREF));
                    if (parser.getAttributeValue(null, ACTION) != null)
                      currentLink.setAction(parser.getAttributeValue(null, ACTION));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentLink.setID(parser.getAttributeValue(null, ID));
                      idRefLinks.put(parser.getAttributeValue(null, ID), currentLink);
                    }
                  }
                  if (trace) System.err.println ("LINK");
                }
                // INFO
                else if (name.equalsIgnoreCase(INFO)) {
                  currentInfo = new SavotInfo();
                  currentMarkup = INFO;
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, NAME) != null)
                      currentInfo.setName(parser.getAttributeValue(null, NAME));
                    if (parser.getAttributeValue(null, VALUE) != null)
                      currentInfo.setValue(parser.getAttributeValue(null, VALUE));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentInfo.setId(parser.getAttributeValue(null, ID));
                      idRefLinks.put(parser.getAttributeValue(null, ID), currentInfo);
                    }
                  }
                  if (trace) System.err.println ("INFO");
                } // MIN
                else if (name.equalsIgnoreCase(MIN)) {
                  if (trace) System.err.println ("MIN");
                  currentMarkup = MIN;
                  currentMin = new SavotMin();
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, VALUE) != null)
                      currentMin.setValue(parser.getAttributeValue(null, VALUE));
                    if (parser.getAttributeValue(null, INCLUSIVE) != null)
                      currentMin.setInclusive(parser.getAttributeValue(null, INCLUSIVE));
                  }
                }
                // MAX
                else if (name.equalsIgnoreCase(MAX)) {
                  if (trace) System.err.println ("MAX");
                  currentMarkup = MAX;
                  currentMax = new SavotMax();
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, VALUE) != null)
                      currentMax.setValue(parser.getAttributeValue(null, VALUE));
                    if (parser.getAttributeValue(null, INCLUSIVE) != null)
                      currentMax.setInclusive(parser.getAttributeValue(null, INCLUSIVE));
                  }
                }
                // OPTION
                else if (name.equalsIgnoreCase(OPTION)) {
                  if (includedOption > 0) {
                    // inner case (multi level options)
                    putOptionStack(currentOption);
                    if (trace) System.err.println ("OPTION - included");
                  }
                  else
                  if (trace) System.err.println ("OPTION - not included");
                  includedOption++;

                  currentMarkup = OPTION;
                  if (trace) System.err.println ("OPTION");
                  currentOption = new SavotOption();
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, NAME) != null)
                      currentOption.setName(parser.getAttributeValue(null, NAME));
                    if (parser.getAttributeValue(null, VALUE) != null)
                      currentOption.setValue(parser.getAttributeValue(null, VALUE));
                  }
                } // GROUP new 1.1
                else if (name.equalsIgnoreCase(GROUP)) {
                  if (includedGroup > 0) {
                    // inner case (multi level groups)
                    putGroupStack(currentGroup);
                    if (trace) System.err.println ("GROUP - included");
                  }
                  else
                  if (trace) System.err.println ("GROUP - not included");
                  includedGroup++;

                  currentMarkup = GROUP;
                  if (trace) System.err.println ("GROUP");
                  currentGroup = new SavotGroup();
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, REF) != null)
                      currentGroup.setRef(parser.getAttributeValue(null, REF));
                    if (parser.getAttributeValue(null, NAME) != null)
                      currentGroup.setName(parser.getAttributeValue(null, NAME));
                    if (parser.getAttributeValue(null, UCD) != null)
                      currentGroup.setUcd(parser.getAttributeValue(null, UCD));
                    if (parser.getAttributeValue(null, UTYPE) != null)
                      currentGroup.setUtype(parser.getAttributeValue(null, UTYPE));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentGroup.setId(parser.getAttributeValue(null, ID));
                      idRefLinks.put(parser.getAttributeValue(null, ID), currentGroup);
                    }
                  }
                } // COOSYS
                else if (name.equalsIgnoreCase(COOSYS)) {
                  currentMarkup = COOSYS;
                  if (trace) System.err.println ("COOSYS");
                  currentCoosys = new SavotCoosys();
                  if (parser.getAttributeCount() != 0) {
                    if (parser.getAttributeValue(null, EQUINOX) != null)
                      currentCoosys.setEquinox(parser.getAttributeValue(null, EQUINOX));
                    if (parser.getAttributeValue(null, EPOCH) != null)
                      currentCoosys.setEpoch(parser.getAttributeValue(null, EPOCH));
                    if (parser.getAttributeValue(null, SYSTEM) != null)
                      currentCoosys.setSystem(parser.getAttributeValue(null, SYSTEM));
                    if (parser.getAttributeValue(null, ID) != null) {
                      currentCoosys.setId(parser.getAttributeValue(null, ID));
                      idRefLinks.put(parser.getAttributeValue(null, ID), currentCoosys);
                    }
                  }
                }
                // DEFINITIONS - deprecated since VOTable 1.1
                else if (name.equalsIgnoreCase(DEFINITIONS)) {
                  currentMarkup = DEFINITIONS;
                  currentDefinitions = new SavotDefinitions();
                  if (trace) System.err.println ("DEFINITIONS");
                }
                else
                  System.err.println("VOTable markup error : " + name + " at line " + parser.getLineNumber());
              }
              currentMarkup = name;
            }
            catch(Exception e) {System.err.println("Exception START_TAG : " + e + " at line " + parser.getLineNumber());}
            break;

            // if an end tag is reach
          case KXmlParser.END_TAG:
            name = parser.getName();
            try {

              if (trace) System.err.println ("End ---> " + name);

                // DESCRIPTION - several fathers are possible
              if (name.equalsIgnoreCase(DESCRIPTION)) {
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(VOTABLE)) {
                  currentVOTable.setDescription(currentDescription);
                  currentMarkup = "";
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(RESOURCE)) {
                  currentResource.setDescription(currentDescription);
                  currentMarkup = "";
                  //  return currentResource;
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(PARAM)) {
                  currentParam.setDescription(currentDescription);
                  currentMarkup = "";
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(TABLE)) {
                  currentTable.setDescription(currentDescription);
                  currentMarkup = "";
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(FIELD)) {
                  currentField.setDescription(currentDescription);
                  currentMarkup = "";
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(GROUP)) {
                  currentGroup.setDescription(currentDescription);
                  currentMarkup = "";
                }
              } // TABLE
              else
              if (name.equalsIgnoreCase(TABLE)) {
                currentResource.getTables().addItem(currentTable);
                currentMarkup = "";

                if (trace) System.err.println(currentTable.getName());
              } // FIELD - several fathers are possible

              else if (name.equalsIgnoreCase(FIELD)) {

                if (trace) System.err.println("FIELD from father = " + (String)father.elementAt(father.size() - 1));

                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(TABLE)) {
                  currentTable.getFields().addItem(currentField);
                  if (trace) System.err.println("FIELD from TABLE father = " + father);
                }
              } // FIELDREF
              else if (name.equalsIgnoreCase(FIELDREF)) {
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(GROUP)) {
                  currentGroup.getFieldsRef().addItem(currentFieldRef);
                  if (trace) System.err.println("FIELDRef from GROUP father = " + father);
                }
              } // TR
              else if (name.equalsIgnoreCase(TR)) {
                if (trace) System.err.println ("TR end");
                currentMarkup = "";

                // add the row to the table
                currentTableData.getTRs().addItem(currentTR);

                // for statistics only
                rowCounter++;

                if (trace) System.err.println("ADD row");

              } // DATA
              else if (name.equalsIgnoreCase(DATA)) {
                currentMarkup = "";
                currentTable.setData(currentData);

              } // TD
              else if (name.equalsIgnoreCase(TD)) {
                currentMarkup = "";
                if (trace) System.err.println ("TD end");
                currentTR.getTDs().addItem(currentTD);

              } // RESOURCE
              else if (name.equalsIgnoreCase(RESOURCE)) {
                if (trace) System.err.println ("RESOURCE end");
                currentMarkup = "";
                if (includedResource > 1) {
                  SavotResource tempo = currentResource;
                  currentResource = getResourceStack();
                  currentResource.getResources().addItem(tempo);
                }
                else {
                  if (parsingType == FULL)
                    currentVOTable.getResources().addItem(currentResource);
                  if (trace) System.err.println(">>>>>>>> RESOURCE COMPLETED");
                  resourceComplete = true;
                }
                includedResource--;
              } // OPTION
              else if (name.equalsIgnoreCase(OPTION)) {
                if (trace) System.err.println ("OPTION end");
                currentMarkup = "";
                if (includedOption > 1) {
                  SavotOption tempo = currentOption;
                  currentOption = getOptionStack();
                  currentOption.getOptions().addItem(tempo);
                  includedOption--;
                }
                else {
                  if (parsingType == FULL)
                    // OPTION - several fathers are possible
                    if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(VALUES)) {
                      currentValues.getOptions().addItem(currentOption);
                      if (trace) System.err.println("OPTION from VALUES father = " + father);
                      includedOption--;
                    }
                }
              } // GROUP
              else if (name.equalsIgnoreCase(GROUP)) {
                if (trace) System.err.println ("GROUP end");
                currentMarkup = "";
                if (includedGroup > 1) {
                  SavotGroup tempo = currentGroup;
                  currentGroup = getGroupStack();
                  currentGroup.getGroups().addItem(tempo);
                  includedGroup--;
                }
                else {
                  if (parsingType == FULL)
                    // GROUP
                    if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(TABLE)) {
                      currentTable.getGroups().addItem(currentGroup);
                      if (trace) System.err.println("GROUP from TABLE father = " + father);
                      includedGroup--;
                    }
                }
              }
              // TABLEDATA
              else if (name.equalsIgnoreCase(TABLEDATA)) {
                currentMarkup = "";
                currentData.setTableData(currentTableData);
                if (trace) System.err.println(currentTable.getName());
              }
              // COOSYS
              else if (name.equalsIgnoreCase(COOSYS)) {
                currentMarkup = "";
                // COOSYS - several fathers are possible
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(DEFINITIONS)) {
                  // deprecated since VOTable 1.1
                  currentDefinitions.getCoosys().addItem(currentCoosys);
                  if (trace) System.err.println("COOSYS from DEFINITIONS father = " + father);
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(RESOURCE)) {
                  currentResource.getCoosys().addItem(currentCoosys);
                  if (trace) System.err.println("COOSYS from RESOURCE father = " + father);
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(VOTABLE)) {
                  currentVOTable.getCoosys().addItem(currentCoosys);
                  if (trace) System.err.println("COOSYS from VOTABLE father = " + father);
                }
              }
              // PARAM - several fathers are possible
              else if (name.equalsIgnoreCase(PARAM)) {
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(DEFINITIONS)) {
                  // deprecated since VOTable 1.1
                  currentDefinitions.getParams().addItem(currentParam);
                  if (trace) System.err.println("PARAM from DEFINITIONS father = " + father);
                }
                else
                if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(RESOURCE)) {
                  currentResource.getParams().addItem(currentParam);
                // 7 MAI            resourceComplete = true;
                if (trace) System.err.println("PARAM from RESOURCE father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(TABLE)) {
                currentTable.getParams().addItem(currentParam);
                if (trace) System.err.println("PARAM from TABLE father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(GROUP)) {
                currentGroup.getParams().addItem(currentParam);
                if (trace) System.err.println("PARAM from GROUP father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(VOTABLE)) {
                currentVOTable.getParams().addItem(currentParam);
                if (trace) System.err.println("PARAM from VOTABLE father = " + father);
              }
            }
            else if (name.equalsIgnoreCase(PARAMREF)) {
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(GROUP)) {
                currentGroup.getParamsRef().addItem(currentParamRef);
                if (trace) System.err.println("PARAMRef from GROUP father = " + father);
              }
            }
            // LINK
            else if (name.equalsIgnoreCase(LINK)) {
              currentMarkup = "";
              // LINK - several fathers are possible
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(RESOURCE)) {
                currentResource.getLinks().addItem(currentLink);
                if (trace) System.err.println("LINK from RESOURCE father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(TABLE)) {
                currentTable.getLinks().addItem(currentLink);
                if (trace) System.err.println("LINK from TABLE father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(FIELD)) {
                currentField.getLinks().addItem(currentLink);
                if (trace) System.err.println("LINK from FIELD father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(PARAM)) {
                currentParam.getLinks().addItem(currentLink);
                if (trace) System.err.println("LINK from PARAM father = " + father);
              }
            } // VALUES
            else if (name.equalsIgnoreCase(VALUES)) {
              currentMarkup = "";
              // VALUES - several fathers are possible
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(PARAM)) {
                currentParam.setValues(currentValues);
                if (trace) System.err.println("VALUES from PARAM father = " + father + " ID : " + currentValues.getId());
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(FIELD)) {
                currentField.setValues(currentValues);
                if (trace) System.err.println("VALUES from FIELD father = " + father + " ID : " + currentValues.getId());
              }
            } // MIN
            else if (name.equalsIgnoreCase(MIN)) {
              currentMarkup = "";
              currentValues.setMin(currentMin);
              if (trace) System.err.println("MIN");
            } // MAX
            else if (name.equalsIgnoreCase(MAX)) {
              currentMarkup = "";
              currentValues.setMax(currentMax);
              if (trace) System.err.println("MAX");
            }
            // STREAM
            else if (name.equalsIgnoreCase(STREAM)) {
              currentMarkup = "";
              // STREAM - several fathers are possible
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(BINARY)) {
                currentBinary.setStream(currentStream);
                if (trace) System.err.println("STREAM from BINARY father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(FITS)) {
                currentFits.setStream(currentStream);
                if (trace) System.err.println("STREAM from FITS father = " + father);
              }
            }
            // BINARY
            else if (name.equalsIgnoreCase(BINARY)) {
              currentMarkup = "";
              currentData.setBinary(currentBinary);
              if (trace) System.err.println("BINARY");
            }
            // FITS
            else if (name.equalsIgnoreCase(FITS)) {
              currentMarkup = "";
              currentData.setFits(currentFits);
              if (trace) System.err.println("FITS");
            }
            // INFO
            else if (name.equalsIgnoreCase(INFO)) {
              currentMarkup = "";
              if (trace) System.err.println("INFO father = " + father);
                // INFO - several fathers are possible
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(VOTABLE)) {
                currentVOTable.getInfos().addItem(currentInfo);
                if (trace) System.err.println("INFO from VOTABLE father = " + father);
              }
              else
              if (((String)father.elementAt(father.size() - 1)).equalsIgnoreCase(RESOURCE)) {
                currentResource.getInfos().addItem(currentInfo);
                if (trace) System.err.println("INFO from RESOURCE father = " + father);
              }
            }
            // DEFINITIONS
            else if (name.equalsIgnoreCase(DEFINITIONS)) {
              // deprecated since VOTable 1.1
              currentMarkup = "";
              if (trace) System.err.println ("DEFINITIONS");
              currentVOTable.setDefinitions(currentDefinitions);
            }
            // VOTABLE
            else if (name.equalsIgnoreCase(VOTABLE)) {
              currentMarkup = "";
              if (trace) System.err.println ("VOTABLE");
            }
            else
              System.err.println("VOTable markup error : " + name + " at line " + parser.getLineNumber());
          }
          catch(Exception e) {System.err.println("Exception FATHER : " + father + " END_TAG (" + name + ") : " +  e + " at line " + parser.getLineNumber());}
          break;

        case KXmlParser.END_DOCUMENT:
          try {
            if (trace) System.err.println ("Document end reached!");
          }
          catch(Exception e) {System.err.println("Exception END_DOCUMENT : " + e + " at line " + parser.getLineNumber());}
          break;

        case KXmlParser.TEXT:
          try {
            // add a data to the current row
            if (currentMarkup.equalsIgnoreCase(TD)) {
              if (trace) System.err.println ("TD : " + (parser.getText()).trim());
              currentTD.setContent((parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(STREAM)) {
              if (trace) System.err.println ("STREAM : " + (parser.getText()).trim());
              currentStream.setContent((parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(DESCRIPTION)) {
              if (trace) System.err.println ("DESCRIPTION : " + (parser.getText()).trim());
              currentDescription = (parser.getText()).trim();
            }
            else
            if (currentMarkup.equalsIgnoreCase(MIN)) {
              if (trace) System.err.println ("MIN : " + (parser.getText()).trim());
              currentMin.setContent((parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(MAX)) {
              if (trace) System.err.println ("MAX : " + (parser.getText()).trim());
              currentMax.setContent((parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(COOSYS)) {
              if (trace) System.err.println ("COOSYS : " + (parser.getText()).trim());
              currentCoosys.setContent((parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(LINK)) {
              if (trace) System.err.println ("LINK : " + (parser.getText()).trim());
              currentLink.setContent((parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(OPTION)) {
              if (trace) System.err.println ("OPTION : " + (parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(GROUP)) {
              if (trace) System.err.println ("GROUP : " + (parser.getText()).trim());
            }
            else
            if (currentMarkup.equalsIgnoreCase(INFO)) {
              currentInfo.setContent((parser.getText()).trim());
              if (trace) System.err.println ("INFO : " + (parser.getText()).trim());
            }
          }
          catch(Exception e) {System.err.println("Exception TEXT : " + e + " at line " + parser.getLineNumber());}
          break;

        case KXmlParser.START_DOCUMENT:
          break;

        default:
          if (trace)
            System.err.println(" ignoring some other (legacy) event at line : " + parser.getLineNumber());
      }

      // new values
      eventType = parser.next();

      // start tag
      if (parser.getEventType() == KXmlParser.START_TAG) {
        if (trace) System.err.println("> FATHER, add : " + parser.getName());
        father.addElement( (parser.getName()));
      }
      else // end tag
      if (parser.getEventType() == KXmlParser.END_TAG) {
        if (parser.getName() != null) {
          if (trace) System.err.println("> FATHER, remove : " + parser.getName());
          father.removeElementAt(father.size() - 1);
        }
        else
        if (trace) System.err.println("> FATHER, case null"); // when a lf or cd is reached
      }

      if ((parsingType == SEQUENTIAL) && resourceComplete) {
        eventType = parser.END_DOCUMENT;
        if (trace) System.err.println(">>>>>>>>>>>>>>> SEQUENTIAL case : RESOURCE end");
      }
    }
  }
  catch (Exception f) {
    if (trace)
      System.err.println("Exception parse : " + f + " at line " + parser.getLineNumber());
  }
  return null;
}

/**
 * Get the next Resource (warning : SEQUENTIAL mode only)
 * @return a SavotResource (always NULL if FULL mode)
 */
public SavotResource getNextResource() {
  currentResource = null;
  try {
    parse(parser, SEQUENTIAL);
  } catch (IOException e){
    if (trace)
      System.err.println("Exception getNextResource : " + e);
  }
  return currentResource;
}

/**
 * Get a reference to V0TABLE object
 * @return SavotVOTable
 */
public SavotVOTable getVOTable() {
  return currentVOTable;
}

/**
 * For test only
 *
 */
public void sequentialTester() {
  do {
    currentResource = getNextResource();
  }
  while ( currentResource != null );
}

/**
 * Get the number of RESOURCE elements in the document (for statistics)
 * @return a long value
 */
public long getResourceCount() {
  return resourceCounter;
}

/**
 * Get the number of TABLE elements in the document (for statistics)
 * @return a long value
 */
public long getTableCount() {
  return tableCounter;
}

/**
 * Get the number of TR elements in the document (for statistics)
 * @return a long value
 */
public long getTRCount() {
  return rowCounter;
}

/**
 * Get the number of DATA elements in the document (for statistics)
 * @return a long value
 */
public long getDataCount() {
  return dataCounter;
}

/**
 * Get a reference on the Hashtable containing the link between ID and ref
 * @return a refernce to the Hashtable
 */
public Hashtable getIdRefLinks() {
  return idRefLinks;
}

/**
 * Search a RESOURCE corresponding to an ID ref
 * @param ref
 * @return a reference to a SavotResource object
 */
public SavotResource getResourceFromRef(String ref) {
  return (SavotResource)idRefLinks.get(ref);
}

/**
 * Search a FIELD corresponding to an ID ref
 * @param ref
 * @return SavotField
 */
public SavotField getFieldFromRef(String ref) {
  return (SavotField)idRefLinks.get(ref);
}

/**
 * Search a FIELDref corresponding to an ID ref
 * @param ref
 * @return SavotFieldRef
 */
public SavotFieldRef getFieldRefFromRef(String ref) {
  return (SavotFieldRef)idRefLinks.get(ref);
}

/**
 * Search a PARAM corresponding to an ID ref
 * @param ref
 * @return SavotParam
 */
public SavotParam getParamFromRef(String ref) {
  return (SavotParam)idRefLinks.get(ref);
}

/**
 * Search a PARAMref corresponding to an ID ref
 * @param ref
 * @return SavotParamRef
 */
public SavotParamRef getParamRefFromRef(String ref) {
  return (SavotParamRef)idRefLinks.get(ref);
}

/**
 * Search a TABLE corresponding to an ID ref
 * @param ref
 * @return SavotTable
 */
public SavotTable getTableFromRef(String ref) {
  return (SavotTable)idRefLinks.get(ref);
}

/**
 * Search a GROUP corresponding to an ID ref
 * @param ref
 * @return SavotGROUP
 */
public SavotGroup getGroupFromRef(String ref) {
  return (SavotGroup)idRefLinks.get(ref);
}

/**
 * Search a RESOURCE corresponding to an ID ref
 * @param ref
 * @return SavotInfo
 */
public SavotInfo getInfoFromRef(String ref) {
  return (SavotInfo)idRefLinks.get(ref);
}

/**
 * Search a VALUES corresponding to an ID ref
 * @param ref
 * @return SavotValues
 */
public SavotValues getValuesFromRef(String ref) {
  return (SavotValues)idRefLinks.get(ref);
}

/**
 * Search a LINK corresponding to an ID ref
 * @param ref
 * @return SavotLink
 */
public SavotLink getLinkFromRef(String ref) {
  return (SavotLink)idRefLinks.get(ref);
}

/**
 * Search a COOSYS corresponding to an ID ref
 * @param ref
 * @return SavotCoosys
 */
public SavotCoosys getCoosysFromRef(String ref) {
  return (SavotCoosys)idRefLinks.get(ref);
}

/**
 * Get current VOTable (all resources)
 * @return SavotVOTable
 */
public SavotVOTable getAllResources() {
  return currentVOTable;
}

/**
 * Enable debug mode
 * @param debug boolean
 */
public void enableDebug(boolean debug) {
  trace = debug;
}
}
