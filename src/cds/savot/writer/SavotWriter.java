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


package cds.savot.writer;

import java.io.*;

import cds.savot.model.*;

/**
 * <p>VOTable document generation from memory</p>
 * @author Andre Schaaff
 * @version 2.6 Copyright CDS 2002-2005
 * 6 June 2005 : the user can now write a VOTable document flow step by step, the previous method is avilable too (writing of a whole document)
 *  (kickoff 31 May 02)
 */
public class SavotWriter {

  // default xml top
  private static String top1 = "<?xml version=" + '"' + "1.0" + '"' +
      " encoding=" + '"' + "UTF-8" + '"' + "?>";

  private static String top2 = "\n<VOTABLE xmlns:xsi=" + '"' +
      "http://www.w3.org/2001/XMLSchema-instance" + '"' +
      "\n" + "xsi:noNamespaceSchemaLocation=" + '"' +
      "xmlns:http://www.ivoa..net/xml/VOTable-1.1.xsd" + '"' + " version="; // + "1.1" + '"' +">";

  private static boolean attributeEntities = true;
  private static boolean elementEntities = true;

  private static OutputStream outStream = null;
  private static DataOutputStream dataOutStream = null;
  private static BufferedOutputStream dataBuffOutStream = null;

  static final byte tdbegin1[] = "<TD".getBytes();
  static final byte tdbegin2[] = ">".getBytes();
  static final byte tdend[] = "</TD>".getBytes();
  static final byte trbegin[] = "<TR>".getBytes();
  static final byte trend[] = "</TR>\n".getBytes();
  static final byte commentbegin[] = "<!-- ".getBytes();
  static final byte commentend[] = " -->\n".getBytes();
  static final byte tabledatabegin[] = "\n<TABLEDATA>\n".getBytes();
  static final byte tabledataend[] = "</TABLEDATA>".getBytes();
  static final byte databegin[] = "\n<DATA>".getBytes();
  static final byte dataend[] = "\n</DATA>".getBytes();
  static final byte tablebegin[] = "\n<TABLE>".getBytes();
  static final byte tableend[] = "\n</TABLE>".getBytes();
  static final byte resourcebegin[] = "\n<RESOURCE>".getBytes();
  static final byte resourceend[] = "\n</RESOURCE>".getBytes();
  static final byte infobegin[] = "<INFO>\n".getBytes();
  static final byte infoend[] = "</INFO>\n".getBytes();
  static final byte descriptionbegin[] = "\n<DESCRIPTION>".getBytes();
  static final byte descriptionend[] = "</DESCRIPTION>".getBytes();
  static final byte groupbegin[] = "\n<GROUP>".getBytes();
  static final byte groupend[] = "\n</GROUP>".getBytes();
  static final byte definitionsbegin[] = "\n<DEFINITIONS>".getBytes();
  static final byte definitionsend[] = "\n</DEFINITIONS>".getBytes();
  static final byte parambegin[] = "\n<PARAM>".getBytes();
  static final byte paramend[] = "\n</PARAM>".getBytes();
  static final byte fieldbegin[] = "\n<FIELD>".getBytes();
  static final byte fieldend[] = "\n</FIELD>".getBytes();
  static final byte linkbegin[] = "\n<LINK>".getBytes();
  static final byte linkend[] = "</LINK>".getBytes();
  static final byte valuesbegin[] = "\n<VALUES>".getBytes();
  static final byte valuesend[] = "\n</VALUES>".getBytes();
  static final byte fitsbegin[] = "\n<FITS>".getBytes();
  static final byte fitsend[] = "\n</FITS>".getBytes();
  static final byte binarybegin[] = "\n<BINARY>".getBytes();
  static final byte binaryend[] = "\n</BINARY>".getBytes();
  static final byte coosysbegin[] = "\n<COOSYS>".getBytes();
  static final byte coosysend[] = "</COOSYS>".getBytes();
  static final byte streambegin[] = "\n<STREAM>".getBytes();
  static final byte streamend[] = "\n</STREAM>".getBytes();
  static final byte minbegin[] = "\n<MIN>".getBytes();
  static final byte minend[] = "</MIN>".getBytes();
  static final byte maxbegin[] = "\n<MAX>".getBytes();
  static final byte maxend[] = "</MAX>".getBytes();
  static final byte optionbegin[] = "\n<OPTION>".getBytes();
  static final byte optionend[] = "\n</OPTION>".getBytes();

  /**
   * Enable or disable Attribute entities mapping
   * @param entities true if Attribute entities are taken into account
   */
  public void enableAttributeEntities(boolean entities) {
    this.attributeEntities = entities;
  }

  /**
   * Enable or disable Element entities mapping
   * @param entities true if Element entities are taken into account
   */
  public void enableElementEntities(boolean entities) {
    this.elementEntities = entities;
  }

  /**
   * Enable or disable Attribute and Element entities mapping
   * @param entities true if all entities are taken into account
   */
  public void enableEntities(boolean entities) {
    this.attributeEntities = entities;
    this.elementEntities = entities;
  }

  /**
   * internal method
   * @param src
   * @param oldPattern
   * @param newPattern
   * @return String
   */
  private static String replace(String src, String oldPattern,
                                String newPattern) {

    String dst = ""; // the new bult up string based on src
    int i; // index of found token
    int last = 0; // last valid non token string data for concat
    boolean done = false; // determines if we're done.

    // while we'er not done, try finding and replacing
    while (!done) {
      // search for the pattern...
      i = src.indexOf(oldPattern, last);
      // if it's not found from our last point in the src string....
      if (i == -1) {
        // we're done.
        done = true;
        // if our last point, happens to be before the end of the string
        if (last < src.length()) {
          // concat the rest of the string to our dst string
          dst = dst.concat(src.substring(last, (src.length())));
        }
      }
      else {
        // we found the pattern
        if (i != last) {
          // if the pattern's not at the very first char of our searching point....
          // we need to concat the text up to that point..
          dst = dst.concat(src.substring(last, i));
        }
        // update our last var to our current found pattern, plus the lenght of the pattern
        last = i + oldPattern.length();
        // concat the new pattern to the dst string
        dst = dst.concat(newPattern);
      }
    }
    // finally, return the new string
    return dst;
  }

  /**
   * Encode special characters to entities
   * @param src
   * @return src
   */
  public static String encodeAttribute(String src) {
    if (attributeEntities == true) {
      src = replace(src, "&", "&amp;");
      src = replace(src, "\"", "&quot;");
//              src = replace(src, "'", "&apos;");
      src = replace(src, "<", "&lt;");
      src = replace(src, ">", "&gt;");
    }
    return src;
  }

  /**
   * Encode special characters to entities
   * @param src
   * @return src
   */
  public static String encodeElement(String src) {
    if (elementEntities == true) {
      src = replace(src, "&", "&amp;");
      src = replace(src, "\"", "&quot;");
//              src = replace(src, "'", "&apos;");
      src = replace(src, "<", "&lt;");
      src = replace(src, ">", "&gt;");
    }
    return src;
  }

  /**
   * Decode special characters to entities
   * @param src
   * @return
   *
     private static String decode(String src) {
    src = replace(src, "&amp;", "&");
    src = replace(src, "&quot;", "\"");
    src = replace(src, "&apos;", "'");
    src = replace(src, "&lt;", "<");
    src = replace(src, "&gt;", ">");
    return src;
     }
   */
  /**
   * Generates a VOTable XML document corresponding to the internal model
   * The result is sent to the standard output
   * @param votable object corresponding to the savot internal model
   */
  public void generateDocument(SavotVOTable votable) {
    generateDocument(votable, null, null);
  }

  /**
   * Generates a VOTable XML document corresponding to the internal model
   * @param votable object corresponding to the savot internal model
   * @param stream the result is sent to this stream
   */
  public void generateDocument(SavotVOTable votable, OutputStream stream) {
    generateDocument(votable, null, stream);
  }

  /**
   * Generates a VOTable XML document corresponding to the internal model
   * @param votable object corresponding to the savot internal model
   * @param file is sent to this file
   */
  public void generateDocument(SavotVOTable votable, String file) {
    generateDocument(votable, file, null);
  }

  /**
   * Generates a VOTable XML document corresponding to the internal model
   * @param votable SavotVOTable
   * @param file String
   * @param stream OutputStream
   */
  public void generateDocument(SavotVOTable votable, String file,
                               OutputStream stream) {

    try {

      if (file == null) {
        if (stream == null) {
          outStream = System.out;
        }
        else {
          outStream = stream;
        }
        dataOutStream = new DataOutputStream(outStream);
        dataBuffOutStream = new BufferedOutputStream(dataOutStream);
      }
      else {
        outStream = new FileOutputStream(new File(file));
        dataOutStream = new DataOutputStream(outStream);
        dataBuffOutStream = new BufferedOutputStream(dataOutStream);
      }

      dataBuffOutStream.write(top1.getBytes());
      if (votable.getAbove() != "") {
        String comment = "<!-- " + votable.getAbove() + " -->";
        dataBuffOutStream.write(comment.getBytes());
      }

      if (votable.getXmlns().compareTo("") != 0 ||
          votable.getXmlnsxsi().compareTo("") != 0 ||
          votable.getXsischema().compareTo("") != 0 ||
          votable.getXmlns().compareTo("") != 0) {
        top2 = "\n<VOTABLE";
        if (votable.getXmlns().compareTo("") != 0) {
          top2 = top2 + " xmlns=" + '"' + votable.getXmlns() + '"';
        }

        if (votable.getXmlnsxsi().compareTo("") != 0) {
          top2 = top2 + " xmlns:xsi=" + '"' + votable.getXmlnsxsi() + '"';
        }

        if (votable.getXsischema().compareTo("") != 0) {
          top2 = top2 + " xsi:schemaLocation=" + '"' + votable.getXsischema() +
              '"';
        }

        if (votable.getXsinoschema().compareTo("") != 0) {
          top2 = top2 + " xsi:noNamespaceSchemaLocation=" + '"' +
              votable.getXsinoschema() + '"';
        }
        top2 = top2 + " version=";
      }

      if (votable.getVersion().compareTo("") != 0) {
        if (votable.getId().compareTo("") != 0) {
          dataBuffOutStream.write( (top2 + '"' + votable.getVersion() + '"' +
                                    " ID=" + '"' + votable.getId() + '"' + '>').
                                  getBytes());
        }
        else {
          dataBuffOutStream.write( (top2 + '"' + votable.getVersion() + '"' +
                                    '>').
                                  getBytes());
        }
      }
      else {
        if (votable.getId().compareTo("") != 0) {
          dataBuffOutStream.write( (top2 + top2 + '"' + "1.1" + '"' + " ID=" +
                                    '"' + votable.getId() + '"' + '>').
                                  getBytes());
        }
        else {
          dataBuffOutStream.write( (top2 + '"' + "1.1" + '"' + '>').getBytes());
        }
      }

      if (votable.getBelow() != "") {
        String comment = "<!-- " + votable.getBelow() + " -->";
        dataBuffOutStream.write(comment.getBytes());
      }

      if (votable.getDescription() != null) {
        // DESCRIPTION
        String description = "";
        if (votable.getDescription() != null &&
            !votable.getDescription().equals("")) {
          if (votable.getAbove() != "") {
            description = "\n<!-- " + votable.getAbove() + " -->";
          }
          description = description + "\n<DESCRIPTION>";
          if (votable.getBelow() != "") {
            description = description + "\n" + "<!-- " + votable.getBelow() +
                " -->" + "\n";
          }
          description = description + encodeElement(votable.getDescription()) +
              "</DESCRIPTION>";
          dataBuffOutStream.write(description.getBytes());
        }
      }

      if (votable.getDefinitions() != null) {
        // DEFINITIONS begin
        dataBuffOutStream.write(definitionsbegin);

        // COOSYS elements
        CoosysSet coosysSet = votable.getDefinitions().getCoosys();

        // write COOSYS elements
        writeCoosys(coosysSet);

        // PARAM elements
        ParamSet paramSet = votable.getDefinitions().getParams();

        // write PARAM elements
        writeParam(paramSet);

        // DEFINITIONS end
        dataBuffOutStream.write(definitionsend);
      }

      // COOSYS
      CoosysSet coosyset = votable.getCoosys();

      // write COOSYS elements
      writeCoosys(coosyset);

      // PARAM
      ParamSet paramset = votable.getParams();

      // write PARAM elements
      writeParam(paramset);

      // INFO
      InfoSet infoset = votable.getInfos();

      // write INFO elements
      writeInfo(infoset);

      // RESOURCE
      ResourceSet resourceset = votable.getResources();
      writeResource(resourceset);
      dataBuffOutStream.write("\n</VOTABLE>\n".getBytes());
      dataBuffOutStream.flush();
    }
    catch (Exception e) {
      System.err.println("generateDocument : " + e);
    }
  }

  /**
   * Generates a VOTable XML document corresponding to the internal model
   * @param votable SavotVOTable
   * @param file String
   * @param stream OutputStream
   */
  public void writeDescription(String description) {
    try {
      String line = "";

      // DESCRIPTION
      line = "<DESCRIPTION>" + encodeElement(description) +
          "</DESCRIPTION>";
      dataBuffOutStream.write(line.getBytes());
      dataBuffOutStream.flush();
    }
    catch (Exception e) {
      System.err.println("writeDescription : " + e);
    }
  }

  /**
   * Init the Stream for the output
   * @param file String
   */
  public void initStream(String file) {

    try {
      outStream = new FileOutputStream(new File(file));
      dataOutStream = new DataOutputStream(outStream);
      dataBuffOutStream = new BufferedOutputStream(dataOutStream);

    }
    catch (Exception e) {
      System.err.println("initStream : " + e);
    }
  }

  /**
   * Init the Stream for the output
   * @param stream OutputStream
   */
  public void initStream(OutputStream stream) {

    try {
      if (stream == null) {
        outStream = System.out;
      }
      else {
        outStream = stream;
      }
      dataOutStream = new DataOutputStream(outStream);
      dataBuffOutStream = new BufferedOutputStream(dataOutStream);
    }
    catch (Exception e) {
      System.err.println("initStream : " + e);
    }
  }

  /**
   * Write a comment
   * @param comment String
   */
  public void writeComment(String comment) {
    try {
      dataBuffOutStream.write( ("<!--" + comment + "-->").getBytes());
    }
    catch (Exception e) {
      System.err.println("writeComment : " + e);
    }
  }

  /**
   * Write a VOTable XML head
   * @param votable
   */
  public void writeDocumentHead(SavotVOTable votable) {

    try {
      dataBuffOutStream.write(top1.getBytes());

      if (votable.getXmlns().compareTo("") != 0 ||
          votable.getXmlnsxsi().compareTo("") != 0 ||
          votable.getXsischema().compareTo("") != 0 ||
          votable.getXmlns().compareTo("") != 0) {
        top2 = "\n<VOTABLE";
        if (votable.getXmlns().compareTo("") != 0) {
          top2 = top2 + " xmlns=" + '"' + votable.getXmlns() + '"';
        }

        if (votable.getXmlnsxsi().compareTo("") != 0) {
          top2 = top2 + " xmlns:xsi=" + '"' + votable.getXmlnsxsi() + '"';
        }

        if (votable.getXsischema().compareTo("") != 0) {
          top2 = top2 + " xsi:schemaLocation=" + '"' + votable.getXsischema() +
              '"';
        }

        if (votable.getXsinoschema().compareTo("") != 0) {
          top2 = top2 + " xsi:noNamespaceSchemaLocation=" + '"' +
              votable.getXsinoschema() + '"';
        }
        top2 = top2 + " version=";
      }

      if (votable.getVersion().compareTo("") != 0) {
        if (votable.getId().compareTo("") != 0) {
          dataBuffOutStream.write( (top2 + '"' + votable.getVersion() + '"' +
                                    " ID=" + '"' + votable.getId() + '"' + '>').
                                  getBytes());
        }
        else {
          dataBuffOutStream.write( (top2 + '"' + votable.getVersion() + '"' +
                                    '>').
                                  getBytes());
        }
      }
      else {
        if (votable.getId().compareTo("") != 0) {
          dataBuffOutStream.write( (top2 + top2 + '"' + "1.1" + '"' + " ID=" +
                                    '"' + votable.getId() + '"' + '>').
                                  getBytes());
        }
        else {
          dataBuffOutStream.write( (top2 + '"' + "1.1" + '"' + '>').getBytes());
        }
      }

      if (votable.getDefinitions() != null) {
        // DEFINITIONS begin
        dataBuffOutStream.write(definitionsbegin);

        // COOSYS elements
        CoosysSet coosysSet = votable.getDefinitions().getCoosys();

        // write COOSYS elements
        writeCoosys(coosysSet);

        // PARAM elements
        ParamSet paramSet = votable.getDefinitions().getParams();

        // write PARAM elements
        writeParam(paramSet);

        // DEFINITIONS end
        dataBuffOutStream.write(definitionsend);
      }

      dataBuffOutStream.flush();
    }
    catch (Exception e) {
      System.err.println("writeDocumentHead : " + e);
    }
  }

  /**
   * Write a VOTable XML end
   * @param votable
   */
  public void writeDocumentEnd() {

    try {

      dataBuffOutStream.write("\n</VOTABLE>\n".getBytes());
      dataBuffOutStream.flush();

    }
    catch (Exception e) {
      System.err.println("writeDocumentEnd : " + e);
    }
  }

  /**
   * Write a COOSYS set
   * @param coosysSet
   */
  public void writeCoosys(CoosysSet coosysSet) {

    try {
      for (int i = 0; i < coosysSet.getItemCount(); i++) {
        String coosysline = "";

        SavotCoosys coosys = (SavotCoosys) coosysSet.getItemAt(i);

        if (coosys.getAbove() != "") {
          coosysline = coosysline + "\n<!-- " + coosys.getAbove() + " -->";
        }

        coosysline = coosysline + "\n<COOSYS";

        if (coosys.getId() != null && !coosys.getId().equals("")) {
          coosysline = coosysline + " ID=" + '"' + coosys.getId() + '"';
        }

        if (coosys.getEquinox() != null && !coosys.getEquinox().equals("")) {
          coosysline = coosysline + " equinox=" + '"' + coosys.getEquinox() +
              '"';
        }

        if (coosys.getEpoch() != null && !coosys.getEpoch().equals("")) {
          coosysline = coosysline + " epoch=" + '"' + coosys.getEpoch() + '"';
        }

        if (coosys.getSystem() != null && !coosys.getSystem().equals("")) {
          coosysline = coosysline + " system=" + '"' + coosys.getSystem() + '"';
        }

        if (coosys.getContent() != null && !coosys.getContent().equals("")) {
          coosysline = coosysline + ">";
          if (coosys.getBelow() != "") {
            coosysline = coosysline + "\n<!-- " + coosys.getBelow() + " -->\n";
          }
          coosysline = coosysline + coosys.getContent();
          dataBuffOutStream.write(coosysline.getBytes());
          dataBuffOutStream.write(coosysend);
        }
        else { /* no content */
          coosysline = coosysline + "/>";
          if (coosys.getBelow() != "") {
            coosysline = coosysline + "\n<!-- " + coosys.getBelow() + " -->\n";
          }
          dataBuffOutStream.write(coosysline.getBytes());
        }
      }
    }
    catch (Exception e) {
      System.err.println("writeCoosys : " + e);
    }
  }

  /**
   * Write a PARAM set
   * @param params
   */
  public void writeParam(ParamSet params) {
    if (params == null) {
      return;
    }
    try {
      for (int i = 0; i < params.getItemCount(); i++) {
        String paramline = "";
        SavotParam param = (SavotParam) params.getItemAt(i);

        if (param.getAbove() != "") {
          paramline = paramline + "\n<!-- " + param.getAbove() + " -->";
        }

        paramline = paramline + "\n<PARAM";

        if (param.getId() != null && !param.getId().equals("")) {
          paramline = paramline + " ID=" + '"' + encodeAttribute(param.getId()) +
              '"';
        }

        if (param.getUnit() != null && !param.getUnit().equals("")) {
          paramline = paramline + " unit=" + '"' +
              encodeAttribute(param.getUnit()) + '"';
        }

        if (param.getDataType() != null && !param.getDataType().equals("")) {
          paramline = paramline + " datatype=" + '"' +
              encodeAttribute(param.getDataType()) + '"';
        }

        if (param.getPrecision() != null && !param.getPrecision().equals("")) {
          paramline = paramline + " precision=" + '"' +
              encodeAttribute(param.getPrecision()) + '"';
        }

        if (param.getWidth() != null && !param.getWidth().equals("")) {
          paramline = paramline + " width=" + '"' +
              encodeAttribute(param.getWidth()) + '"';
        }

        if (param.getRef() != null && !param.getRef().equals("")) {
          paramline = paramline + " ref=" + '"' + encodeAttribute(param.getRef()) +
              '"';
        }

        if (param.getName() != null && !param.getName().equals("")) {
          paramline = paramline + " name=" + '"' +
              encodeAttribute(param.getName()) + '"';
        }

        if (param.getUcd() != null && !param.getUcd().equals("")) {
          paramline = paramline + " ucd=" + '"' + encodeAttribute(param.getUcd()) +
              '"';
        }

        if (param.getUtype() != null && !param.getUtype().equals("")) {
          paramline = paramline + " utype=" + '"' +
              encodeAttribute(param.getUtype()) + '"';
        }

        if (param.getValue() != null && !param.getValue().equals("")) {
          paramline = paramline + " value=" + '"' +
              encodeAttribute(param.getValue()) + '"';
        }

        if (param.getArraySize() != null && !param.getArraySize().equals("")) {
          paramline = paramline + " arraysize=" + '"' +
              encodeAttribute(param.getArraySize()) + '"';
        }

        if (param.getValues() != null || param.getLinks().getItemCount() != 0 ||
            (param.getDescription() != null &&
             !param.getDescription().equals(""))) {
          paramline = paramline + ">";

          if (param.getBelow() != "") {
            paramline = paramline + "\n<!-- " + param.getBelow() + " -->\n";
          }

          // write DESCRIPTION element
          if (param.getDescription() != null &&
              !param.getDescription().equals("")) {
            paramline = paramline + "\n<DESCRIPTION>" + param.getDescription() +
                "</DESCRIPTION>";
          }

          dataBuffOutStream.write(paramline.getBytes());

          // write VALUES element
          writeValues(param.getValues());

          // write LINK elements
          writeLink(param.getLinks());

          // write PARAM end
          dataBuffOutStream.write(paramend);
        }
        else {
          paramline = paramline + "/>";

          if (param.getBelow() != "") {
            paramline = paramline + "\n<!-- " + param.getBelow() + " -->\n";
          }
          dataBuffOutStream.write(paramline.getBytes());
        }
      }
    }
    catch (Exception e) {
      System.err.println("writeParam : " + e);
    }
  }

  /**
   * Write a PARAMref set
   * @param refparams
   */
  public void writeParamRef(ParamRefSet refparams) {
    if (refparams == null) {
      return;
    }
    try {
      for (int i = 0; i < refparams.getItemCount(); i++) {
        String paramline = "";
        SavotParamRef paramref = (SavotParamRef) refparams.getItemAt(i);

        if (paramref.getAbove() != "") {
          paramline = paramline + "\n<!-- " + paramref.getAbove() + " -->";
        }

        paramline = paramline + "\n<PARAMref";

        if (paramref.getRef() != null && !paramref.getRef().equals("")) {
          paramline = paramline + " ref=" + '"' +
              encodeAttribute(paramref.getRef()) + '"';
        }
        paramline = paramline + "/>";

        if (paramref.getBelow() != "") {
          paramline = paramline + "\n<!-- " + paramref.getBelow() + " -->\n";
        }

        dataBuffOutStream.write(paramline.getBytes());

        // write PARAM end
        dataBuffOutStream.write(paramend);
      }
    }
    catch (Exception e) {
      System.err.println("writeParamRef : " + e);
    }
  }

  /**
   * Write a LINK set
   * @param linkSet
   */
  public void writeLink(LinkSet linkSet) {

    try {
      for (int k = 0; k < linkSet.getItemCount(); k++) {
        String linkline = "";
        SavotLink link = (SavotLink) linkSet.getItemAt(k);

        if ( ( (SavotLink) linkSet.getItemAt(k)).getAbove() != "") {
          linkline = linkline + "\n<!-- " +
              ( (SavotLink) linkSet.getItemAt(k)).getAbove() + " -->";
        }

        linkline = linkline + "\n<LINK";

        if (link.getID() != null && !link.getID().equals("")) {
          linkline = linkline + " ID=" + '"' + encodeAttribute(link.getID()) +
              '"';
        }

        if (link.getContentRole() != null && !link.getContentRole().equals("")) {
          linkline = linkline + " content-role=" + '"' +
              encodeAttribute(link.getContentRole()) + '"';
        }

        if (link.getContentType() != null && !link.getContentType().equals("")) {
          linkline = linkline + " content-type=" + '"' +
              encodeAttribute(link.getContentType()) + '"';
        }

        if (link.getTitle() != null && !link.getTitle().equals("")) {
          linkline = linkline + " title=" + '"' + encodeAttribute(link.getTitle()) +
              '"';
        }

        if (link.getValue() != null && !link.getValue().equals("")) {
          linkline = linkline + " value=" + '"' + encodeAttribute(link.getValue()) +
              '"';
        }

        if (link.getHref() != null && !link.getHref().equals("")) {
          linkline = linkline + " href=" + '"' + encodeAttribute(link.getHref()) +
              '"';
        }

        if (link.getGref() != null && !link.getGref().equals("")) {
          linkline = linkline + " gref=" + '"' + encodeAttribute(link.getGref()) +
              '"';
        }

        if (link.getAction() != null && !link.getAction().equals("")) {
          linkline = linkline + " action=" + '"' +
              encodeAttribute(link.getAction()) + '"';
        }

        if (link.getContent() != null && !link.getContent().equals("")) {
          linkline = linkline + ">";
          if (link.getBelow() != "") {
            linkline = linkline + "\n<!-- " + link.getBelow() + " -->\n";
          }
          linkline = linkline + link.getContent();
          dataBuffOutStream.write(linkline.getBytes());
          dataBuffOutStream.write(linkend);
        }
        else { /* no content */
          linkline = linkline + "/>";
          if (link.getBelow() != "") {
            linkline = linkline + "\n<!-- " + link.getBelow() + " -->\n";
          }
          dataBuffOutStream.write(linkline.getBytes());
        }
      }
    }
    catch (Exception e) {
      System.err.println("writeLink : " + e);
    }
  }

  /**
   * Write an INFO set
   * @param infoSet
   */
  public void writeInfo(InfoSet infoSet) {
    try {
      if (infoSet != null) {
        int infocount = infoSet.getItemCount();

        for (int j = 0; j < infocount; j++) {
          // INFO
          String info = "";

          if ( ( (SavotInfo) infoSet.getItemAt(j)).getAbove() != "") {
            info = info + "\n<!-- " +
                ( (SavotInfo) infoSet.getItemAt(j)).getAbove() + " -->";
          }

          info = info + "\n<INFO";

          if ( ( (SavotInfo) infoSet.getItemAt(j)).getId().equals("") == false) {
            info = info + " ID=" + '"' +
                encodeAttribute( ( (SavotInfo) infoSet.getItemAt(j)).getId()) +
                '"';
          }

          if ( ( (SavotInfo) infoSet.getItemAt(j)).getName().equals("") == false) {
            info = info + " name=" + '"' +
                encodeAttribute( ( (SavotInfo) infoSet.getItemAt(j)).getName()) +
                '"';
          }

          if ( ( (SavotInfo) infoSet.getItemAt(j)).getValue().equals("") == false) {
            info = info + " value=" + '"' +
                encodeAttribute( ( (SavotInfo) infoSet.getItemAt(j)).getValue()) +
                '"';
          }

          if ( ( (SavotInfo) infoSet.getItemAt(j)).getBelow() != "") {
            info = info + "\n<!-- " +
                ( (SavotInfo) infoSet.getItemAt(j)).getBelow() + " -->\n";
          }

          if ( ( (SavotInfo) infoSet.getItemAt(j)).getContent() != null &&
              ( (SavotInfo) infoSet.getItemAt(j)).getContent().compareTo("") !=
              0) {
            info = info + ">" + ( (SavotInfo) infoSet.getItemAt(j)).getContent() +
                "</INFO>";
          }
          else {
            info = info + "/>";
          }
          dataBuffOutStream.write(info.getBytes());
        }
      }
    }
    catch (Exception e) {
      System.err.println("writeInfo : " + e);
    }
  }

  /**
   * Write a FIELD set
   * @param fieldSet
   */
  public void writeField(FieldSet fieldSet) {
    try {

      for (int m = 0; m < fieldSet.getItemCount(); m++) {
        SavotField field = (SavotField) fieldSet.getItemAt(m);

        String fieldline = "";

        if (field.getAbove() != "") {
          fieldline = fieldline + "\n<!-- " + field.getAbove() + " -->";
        }

        fieldline = fieldline + "\n<FIELD";

        if (field.getId() != null && !field.getId().equals("")) {
          fieldline = fieldline + " ID=" + '"' + encodeAttribute(field.getId()) +
              '"';
        }

        if (field.getName() != null && !field.getName().equals("")) {
          fieldline = fieldline + " name=" + '"' +
              encodeAttribute(field.getName()) + '"';
        }

        if (field.getDataType() != null && !field.getDataType().equals("")) {
          fieldline = fieldline + " datatype=" + '"' +
              encodeAttribute(field.getDataType()) + '"';
        }

        if (field.getPrecision() != null && !field.getPrecision().equals("")) {
          fieldline = fieldline + " precision=" + '"' +
              encodeAttribute(field.getPrecision()) + '"';
        }

        if (field.getWidth() != null && !field.getWidth().equals("")) {
          fieldline = fieldline + " width=" + '"' +
              encodeAttribute(field.getWidth()) + '"';
        }

        if (field.getRef() != null && !field.getRef().equals("")) {
          fieldline = fieldline + " ref=" + '"' + encodeAttribute(field.getRef()) +
              '"';
        }

        if (field.getUcd() != null && !field.getUcd().equals("")) {
          fieldline = fieldline + " ucd=" + '"' + encodeAttribute(field.getUcd()) +
              '"';
        }

        if (field.getUtype() != null && !field.getUtype().equals("")) {
          fieldline = fieldline + " utype=" + '"' +
              encodeAttribute(field.getUtype()) + '"';
        }

        if (field.getArraySize() != null && !field.getArraySize().equals("")) {
          fieldline = fieldline + " arraysize=" + '"' +
              encodeAttribute(field.getArraySize()) + '"';
        }

        if (field.getType() != null && !field.getType().equals("")) {
          fieldline = fieldline + " type=" + '"' +
              encodeAttribute(field.getType()) + '"';
        }

        if (field.getUnit() != null && !field.getUnit().equals("")) {
          fieldline = fieldline + " unit=" + '"' +
              encodeAttribute(field.getUnit()) + '"';
        }

        if ( (field.getDescription() != null &&
              !field.getDescription().equals("")) || field.getValues() != null ||
            field.getLinks().getItemCount() != 0) {
          fieldline = fieldline + ">";
          if (field.getBelow() != "") {
            fieldline = fieldline + "\n<!-- " + field.getBelow() + " -->\n";
          }

          if (field.getDescription() != null &&
              !field.getDescription().equals("")) {
            if (field.getAbove() != "") {
              fieldline = "\n<!-- " + field.getAbove() + " -->";
            }
            fieldline = fieldline + "\n<DESCRIPTION>";
            if (field.getBelow() != "") {
              fieldline = fieldline + "\n" + "<!-- " + field.getBelow() +
                  " -->" + "\n";
            }
            fieldline = fieldline + encodeElement(field.getDescription()) +
                "</DESCRIPTION>";
          }
          dataBuffOutStream.write(fieldline.getBytes());

          // VALUES element
          if (field.getValues() != null) {
            // write VALUES element
            writeValues(field.getValues());
          }

          // LINK elements
          if (field.getLinks().getItemCount() != 0) {
            LinkSet links = (LinkSet) field.getLinks();
            // write LINK elements
            writeLink(links);
          }
          dataBuffOutStream.write(fieldend);
        }
        else {
          fieldline = fieldline + "/>";
          dataBuffOutStream.write(fieldline.getBytes());
        }
      }
    }
    catch (Exception e) {
      System.err.println("writeField : " + e);
    }
  }

  /**
   * Write a FIELD set
   * @param fieldRefSet
   */
  public void writeFieldRef(FieldRefSet fieldRefSet) {
    try {

      for (int m = 0; m < fieldRefSet.getItemCount(); m++) {
        SavotFieldRef fieldref = (SavotFieldRef) fieldRefSet.getItemAt(m);

        String fieldline = "";

        if (fieldref.getAbove() != "") {
          fieldline = fieldline + "\n<!-- " + fieldref.getAbove() + " -->";
        }

        fieldline = fieldline + "\n<FIELDref";

        if (fieldref.getRef() != null && !fieldref.getRef().equals("")) {
          fieldline = fieldline + " ref=" + '"' +
              encodeAttribute(fieldref.getRef()) + '"';
        }

        fieldline = fieldline + "/>";
        dataBuffOutStream.write(fieldline.getBytes());
      }
    }
    catch (Exception e) {
      System.err.println("writeFieldref : " + e);
    }
  }

  /**
   * Write a STREAM element
   * @param stream
   */
  public void writeStream(SavotStream stream) {
    try {

      String streamline = "";
      if (stream.getAbove() != "") {
        streamline = streamline + "\n<!-- " + stream.getAbove() + " -->";
      }

      streamline = streamline + "\n<STREAM";

      if (stream.getType() != null && !stream.getType().equals("")) {
        streamline = streamline + " type=" + '"' +
            encodeAttribute(stream.getType()) + '"';
      }

      if (stream.getHref() != null && !stream.getHref().equals("")) {
        streamline = streamline + " href=" + '"' +
            encodeAttribute(stream.getHref()) + '"';
      }

      if (stream.getActuate() != null && !stream.getActuate().equals("")) {
        streamline = streamline + " actuate=" + '"' +
            encodeAttribute(stream.getActuate()) + '"';
      }

      if (stream.getEncoding() != null && !stream.getEncoding().equals("")) {
        streamline = streamline + " encoding=" + '"' +
            encodeAttribute(stream.getEncoding()) + '"';
      }

      if (stream.getExpires() != null && !stream.getExpires().equals("")) {
        streamline = streamline + " expires=" + '"' +
            encodeAttribute(stream.getExpires()) + '"';
      }

      if (stream.getRights() != null && !stream.getRights().equals("")) {
        streamline = streamline + " rights=" + '"' +
            encodeAttribute(stream.getRights()) + '"';
      }

      streamline = streamline + ">";

      if (stream.getBelow() != "") {
        streamline = streamline + "\n<!-- " + stream.getBelow() + " -->\n";
      }

      dataBuffOutStream.write(streamline.getBytes());
      if (stream.getContent() != null && !stream.getContent().equals("")) {
        dataBuffOutStream.write(stream.getContent().getBytes());
      }

      dataBuffOutStream.write(streamend);
    }
    catch (Exception e) {
      System.err.println("writeStream : " + e);
    }
  }

  /**
   * Write a BINARY element
   * @param binary
   */
  public void writeBinary(SavotBinary binary) {
    try {
      String binaryline = "";
      if (binary.getStream() != null) {
        if (binary.getAbove() != "") {
          binaryline = "\n<!-- " + binary.getAbove() + " -->";
          dataBuffOutStream.write(binaryline.getBytes());
        }

        dataBuffOutStream.write(binarybegin);
        if (binary.getBelow() != "") {
          binaryline = "\n<!-- " + binary.getBelow() + " -->";
          dataBuffOutStream.write(binaryline.getBytes());
        }
        writeStream(binary.getStream());
        dataBuffOutStream.write(binaryend);
      }
    }
    catch (Exception e) {
      System.err.println("writeBinary : " + e);
    }
  }

  /**
   * Write a VALUES element
   * @param values
   */
  public void writeValues(SavotValues values) {
    try {
      if (values == null) {
        return;
      }
      String valuesline = "";
      if (values.getAbove() != "") {
        valuesline = valuesline + "\n<!-- " + values.getAbove() + " -->";
      }

      valuesline = valuesline + "\n<VALUES";

      if (values.getId() != null && !values.getId().equals("")) {
        valuesline = valuesline + " ID=" + '"' + encodeAttribute(values.getId()) +
            '"';
      }

      if (values.getType() != null && !values.getType().equals("")) {
        valuesline = valuesline + " type=" + '"' +
            encodeAttribute(values.getType()) + '"';
      }

      if (values.getNull() != null && !values.getNull().equals("")) {
        valuesline = valuesline + " null=" + '"' +
            encodeAttribute(values.getNull()) + '"';
      }

      if (values.getRef() != null && !values.getRef().equals("")) {
        valuesline = valuesline + " ref=" + '"' + encodeAttribute(values.getRef()) +
            '"';
      }

      if (values.getInvalid() != null && !values.getInvalid().equals("")) {
        valuesline = valuesline + " invalid=" + '"' +
            encodeAttribute(values.getInvalid()) + '"';
      }

      valuesline = valuesline + ">";

      if (values.getBelow() != "") {
        valuesline = valuesline + "\n<!-- " + values.getBelow() + " -->\n";
      }

      dataBuffOutStream.write(valuesline.getBytes());
      valuesline = "";

      // MIN element
      if (values.getMin() != null) {
        SavotMin min = values.getMin();
        writeMin(min);
      }

      // MAX element
      if (values.getMax() != null) {
        SavotMax max = values.getMax();
        writeMax(max);
      }

      // OPTION elements
      if (values.getOptions() != null) {
        OptionSet options = (OptionSet) values.getOptions();
        // write OPTION elements
        writeOption(options);
      }
      dataBuffOutStream.write(valuesend);
    }
    catch (Exception e) {
      System.err.println("writeValues : " + e);
    }
  }

  /**
   * Write a FITS element
   * @param fits
   */
  public void writeFits(SavotFits fits) {
    try {

      String fitsline = "";

      if (fits.getAbove() != "") {
        fitsline = fitsline + "\n<!-- " + fits.getAbove() + " -->";
      }

      fitsline = fitsline + "\n<FITS";

      if (fits.getExtnum() != null && !fits.getExtnum().equals("")) {
        fitsline = fitsline + " extnum=" + '"' + encodeAttribute(fits.getExtnum()) +
            '"';
      }

      fitsline = fitsline + ">";

      if (fits.getBelow() != "") {
        fitsline = fitsline + "\n<!-- " + fits.getBelow() + " -->\n";
      }

      dataBuffOutStream.write(fitsline.getBytes());
      fitsline = "";

      // STREAM element
      if (fits.getStream() != null) {
        SavotStream stream = (SavotStream) fits.getStream();
        // write STREAM element
        writeStream(stream);
      }
      dataBuffOutStream.write(fitsend);
    }
    catch (Exception e) {
      System.err.println("writeFits : " + e);
    }
  }

  /**
   * Write a MIN element
   * @param min
   */
  public void writeMin(SavotMin min) {
    try {

      String minline = "";

      if (min.getAbove() != "") {
        minline = minline + "\n<!-- " + min.getAbove() + " -->";
      }

      minline = minline + "\n<MIN";

      if (min.getValue() != null && !min.getValue().equals("")) {
        minline = minline + " value=" + '"' + encodeAttribute(min.getValue()) +
            '"';
      }

      if (min.getInclusive() != null && !min.getInclusive().equals("")) {
        minline = minline + " inclusive=" + '"' +
            encodeAttribute(min.getInclusive()) + '"';
      }

      if (min.getContent() != null && !min.getContent().equals("")) {
        minline = minline + ">";
        if (min.getBelow() != "") {
          minline = minline + "\n<!-- " + min.getBelow() + " -->\n";
        }
        minline = minline + min.getContent();
        dataBuffOutStream.write(minline.getBytes());
        dataBuffOutStream.write(minend);
      }
      else { /* no content */
        minline = minline + "/>";
        if (min.getBelow() != "") {
          minline = minline + "\n<!-- " + min.getBelow() + " -->\n";
        }
        dataBuffOutStream.write(minline.getBytes());
      }
    }
    catch (Exception e) {
      System.err.println("writeMin : " + e);
    }
  }

  /**
   * Write a MAX element
   * @param max
   */
  public void writeMax(SavotMax max) {
    try {

      String maxline = "";

      if (max.getAbove() != "") {
        maxline = maxline + "\n<!-- " + max.getAbove() + " -->";
      }

      maxline = maxline + "\n<MAX";

      if (max.getValue() != null && !max.getValue().equals("")) {
        maxline = maxline + " value=" + '"' + encodeAttribute(max.getValue()) +
            '"';
      }

      if (max.getInclusive() != null && !max.getInclusive().equals("")) {
        maxline = maxline + " inclusive=" + '"' +
            encodeAttribute(max.getInclusive()) + '"';
      }

      if (max.getContent() != null && !max.getContent().equals("")) {
        maxline = maxline + ">";
        if (max.getBelow() != "") {
          maxline = maxline + "\n<!-- " + max.getBelow() + " -->\n";
        }
        maxline = maxline + max.getContent();
        dataBuffOutStream.write(maxline.getBytes());
        dataBuffOutStream.write(maxend);
      }
      else { /* no content */
        maxline = maxline + "/>";
        if (max.getBelow() != "") {
          maxline = maxline + "\n<!-- " + max.getBelow() + " -->\n";
        }
        dataBuffOutStream.write(maxline.getBytes());
      }
    }
    catch (Exception e) {
      System.err.println("writeMax : " + e);
    }
  }

  /**
   * Write an OPTION set
   * @param optionSet
   */
  public void writeOption(OptionSet optionSet) {
    try {
      for (int m = 0; m < optionSet.getItemCount(); m++) {
        String optionline = "";
        SavotOption option = (SavotOption) optionSet.getItemAt(m);

        if (option.getAbove() != "") {
          optionline = optionline + "\n<!-- " + option.getAbove() + " -->";
        }

        optionline = optionline + "\n<OPTION";

        if (option.getName() != null && !option.getName().equals("")) {
          optionline = optionline + " name=" + '"' +
              encodeAttribute(option.getName()) + '"';
        }

        if (option.getValue() != null && !option.getValue().equals("")) {
          optionline = optionline + " value=" + '"' +
              encodeAttribute(option.getValue()) + '"';
        }

        // write recursive options
        if (option.getOptions().getItemCount() != 0) {
          optionline = optionline + ">";
          if (option.getBelow() != "") {
            optionline = optionline + "\n<!-- " + option.getBelow() + " -->\n";
          }
          OptionSet options = option.getOptions();
          dataBuffOutStream.write(optionline.getBytes());
          writeOption(options);
          dataBuffOutStream.write(optionend);
        }
        else {
          optionline = optionline + "/>";
          if (option.getBelow() != "") {
            optionline = optionline + "\n<!-- " + option.getBelow() + " -->\n";
          }
          dataBuffOutStream.write(optionline.getBytes());
        }
      }
    }
    catch (Exception e) {
      System.err.println("writeOption : " + e);
    }
  }

  /**
   * Write a GROUP set
   * @param groupSet
   */
  public void writeGroup(GroupSet groupSet) {
    try {
      for (int m = 0; m < groupSet.getItemCount(); m++) {
        String groupline = "";
        SavotGroup group = (SavotGroup) groupSet.getItemAt(m);

        if (group.getAbove() != "") {
          groupline = groupline + "\n<!-- " + group.getAbove() + " -->";
        }

        groupline = groupline + "\n<GROUP";

        if (group.getId() != null && !group.getId().equals("")) {
          groupline = groupline + " ID=" + '"' + encodeAttribute(group.getId()) +
              '"';
        }

        if (group.getName() != null && !group.getName().equals("")) {
          groupline = groupline + " name=" + '"' +
              encodeAttribute(group.getName()) + '"';
        }

        if (group.getRef() != null && !group.getRef().equals("")) {
          groupline = groupline + " ref=" + '"' + encodeAttribute(group.getRef()) +
              '"';
        }

        if (group.getUcd() != null && !group.getUcd().equals("")) {
          groupline = groupline + " ucd=" + '"' + encodeAttribute(group.getUcd()) +
              '"';
        }

        if (group.getUtype() != null && !group.getUtype().equals("")) {
          groupline = groupline + " utype=" + '"' +
              encodeAttribute(group.getUtype()) + '"';
        }

        groupline = groupline + ">";

        if (group.getBelow() != "") {
          groupline = groupline + "\n<!-- " + group.getBelow() + " -->\n";
        }

        // write DESCRIPTION element
        if (group.getDescription() != null && !group.getDescription().equals("")) {

          if (group.getAbove() != "") {
            groupline = "\n<!-- " + group.getAbove() + " -->";
          }
          groupline = groupline + "\n<DESCRIPTION>";
          if (group.getBelow() != "") {
            groupline = groupline + "\n" + "<!-- " + group.getBelow() + " -->" +
                "\n";
          }
          groupline = groupline + encodeElement(group.getDescription()) +
              "</DESCRIPTION>";
        }

        dataBuffOutStream.write(groupline.getBytes());

        // write FIELDref elements
        if (group.getFieldsRef().getItemCount() != 0) {
          FieldRefSet reffields = group.getFieldsRef();
          writeFieldRef(reffields);
        }

        // write PARAMref elements
        if (group.getParamsRef().getItemCount() != 0) {
          ParamRefSet refgroups = group.getParamsRef();
          writeParamRef(refgroups);
        }

        // write PARAM elements
        if (group.getParams().getItemCount() != 0) {
          ParamSet groups = group.getParams();
          writeParam(groups);
        }

        // write recursive groups
        if (group.getGroups().getItemCount() != 0) {
          GroupSet groups = group.getGroups();
          writeGroup(groups);
        }
        dataBuffOutStream.write(groupend);
      }
    }
    catch (Exception e) {
      System.err.println("writeGroup : " + e);
    }
  }

  /**
   * Write a TABLE begin
   * @param table SavotTable
   */
  public void writeTableBegin(SavotTable table) {
    try {

      // RESOURCE
      String tableline = "";

      tableline = tableline + "\n<TABLE";

      if (table.getId() != null && !table.getId().equals("")) {
        tableline = tableline + " ID=" + '"' +
            encodeAttribute(table.getId()) +
            '"';
      }

      if (table.getName() != null && !table.getName().equals("")) {
        tableline = tableline + " name=" + '"' +
            encodeAttribute(table.getName()) +
            '"';
      }

      if (table.getRef() != null && !table.getRef().equals("")) {
        tableline = tableline + " ref=" + '"' +
            encodeAttribute(table.getRef()) +
            '"';
      }

      if (table.getUcd() != null && !table.getUcd().equals("")) {
        tableline = tableline + " ucd=" + '"' +
            encodeAttribute(table.getUcd()) +
            '"';
      }

      if (table.getUtype() != null && !table.getUtype().equals("")) {
        tableline = tableline + " utype=" + '"' +
            encodeAttribute(table.getUtype()) +
            '"';
      }

      if (table.getNrows() != null && !table.getNrows().equals("")) {
        tableline = tableline + " nrows=" + '"' +
            encodeAttribute(table.getNrows()) +
            '"';
      }

      tableline = tableline + ">";
      dataBuffOutStream.write(tableline.getBytes());

      // Description
      if (table.getDescription() != null && !table.getDescription().equals("")) {
        writeDescription(table.getDescription());
      }
    }
    catch (Exception e) {
      System.err.println("writeResourceBegin : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a TABLE end
   */
  public void writeTableEnd() {
    try {
      // </TABLE>
      dataBuffOutStream.write(tableend);
    }
    catch (Exception e) {
      System.err.println("writeTableEnd : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a RESOURCE begin
   * @param resource SavotResource
   */
  public void writeResourceBegin(SavotResource resource) {
    try {

      // RESOURCE
      String line = "";

      line = line + "\n<RESOURCE";

      if (resource.getName().equals("") == false) {
        line = line + " name=" + '"' +
            encodeAttribute(resource.getName()) +
            '"';
      }

      if (resource.getId().equals("") == false) {
        line = line + " ID=" + '"' +
            encodeAttribute(resource.
                            getId()) +
            '"';
      }

      if (resource.getUtype().equals("") == false) {
        line = line + " utype=" + '"' +
            encodeAttribute(resource.
                            getUtype()) +
            '"';
      }

      if (resource.getType().equals("") == false) {
        line = line + " type=" + '"' +
            encodeAttribute(resource.
                            getType()) +
            '"';
      }

      line = line + ">";
      dataBuffOutStream.write(line.getBytes());
    }
    catch (Exception e) {
      System.err.println("writeResourceBegin : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a RESOURCE end
   */
  public void writeResourceEnd() {
    try {
      // </RESOURCE>
      dataBuffOutStream.write(resourceend);
    }
    catch (Exception e) {
      System.err.println("writeResourceEnd : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a TABLEDATA begin
   */
  public void writeTableDataBegin() {
    try {
      // </TABLEDATA>
      dataBuffOutStream.write(tabledatabegin);
    }
    catch (Exception e) {
      System.err.println("writeTableDataBegin : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a TABLEDATA end
   */
  public void writeTableDataEnd() {
    try {
      // </TABLEDATA>
      dataBuffOutStream.write("\n".getBytes());
      dataBuffOutStream.write(tabledataend);
    }
    catch (Exception e) {
      System.err.println("writeTableDataEnd : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a DATA begin
   */
  public void writeDataBegin() {
    try {
      // </DATA>
      dataBuffOutStream.write(databegin);
    }
    catch (Exception e) {
      System.err.println("writeDataBegin : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a DATA end
   */
  public void writeDataEnd() {
    try {
      // </DATA>
      dataBuffOutStream.write("\n".getBytes());
      dataBuffOutStream.write(dataend);
    }
    catch (Exception e) {
      System.err.println("writeDataEnd : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a TR
   */
  public void writeTR(SavotTR tr) {
    try {
      TDSet tds = tr.getTDSet();
      // <TR>
      dataBuffOutStream.write(trbegin);

      for (int r = 0; r < tds.getItemCount(); r++) {
        // <TD>
        dataBuffOutStream.write(tdbegin1);

        // TD attributes
        if ( ( (SavotTD) tds.getItemAt(r)).getEncoding() != null &&
            ! ( ( (SavotTD) tds.getItemAt(r)).getEncoding()).
            equals(
                "")) {
          dataBuffOutStream.write( (" encoding=" + '"' +
                                    ( (SavotTD) tds.getItemAt(r)).
                                    getEncoding() + '"').getBytes());
        }

        dataBuffOutStream.write(tdbegin2);

        if (elementEntities) {
          dataBuffOutStream.write(encodeElement( (String) tds.
                                                getContent(r)).
                                  getBytes());
        }
        else {
          dataBuffOutStream.write(tds.getByteContent(r));
        }
        // </TD>
        dataBuffOutStream.write(tdend);
      }
      // </TR>
      dataBuffOutStream.write(trend);
    }
    catch (Exception e) {
      System.err.println("writeTREnd : " + e);
      e.printStackTrace();
    }
  }

  /**
   * Write a RESOURCE set
   * @param resourceset ResourceSet
   */
  public void writeResource(ResourceSet resourceset) {
    try {
      if (resourceset != null) {
        int resourcecount = resourceset.getItemCount();

        for (int i = 0; i < resourcecount; i++) {
          // RESOURCE
          String resource = "";

          if ( ( (SavotResource) resourceset.getItemAt(i)).getAbove() != "") {
            resource = resource + "\n<!-- " +
                ( (SavotResource) resourceset.getItemAt(i)).getAbove() +
                " -->";
          }

          resource = resource + "\n<RESOURCE";

          if ( ( (SavotResource) resourceset.getItemAt(i)).getName().equals(
              "") == false) {
            resource = resource + " name=" + '"' +
                encodeAttribute( ( (SavotResource) resourceset.getItemAt(i)).
                                getName()) +
                '"';
          }

          if ( ( (SavotResource) resourceset.getItemAt(i)).getId().equals("") == false) {
            resource = resource + " ID=" + '"' +
                encodeAttribute( ( (SavotResource) resourceset.getItemAt(i)).
                                getId()) +
                '"';
          }

          if ( ( (SavotResource) resourceset.getItemAt(i)).getUtype().equals(
              "") == false) {
            resource = resource + " utype=" + '"' +
                encodeAttribute( ( (SavotResource) resourceset.getItemAt(i)).
                                getUtype()) +
                '"';
          }

          if ( ( (SavotResource) resourceset.getItemAt(i)).getType().equals(
              "") == false) {
            resource = resource + " type=" + '"' +
                encodeAttribute( ( (SavotResource) resourceset.getItemAt(i)).
                                getType()) +
                '"';
          }

          resource = resource + ">";

          if ( ( (SavotResource) resourceset.getItemAt(i)).getBelow() != "") {
            resource = resource + "\n<!-- " +
                ( (SavotResource) resourceset.getItemAt(i)).getBelow() +
                " -->\n";
          }

          dataBuffOutStream.write(resource.getBytes());

          // DESCRIPTION
          if ( ( (SavotResource) resourceset.getItemAt(i)).getDescription().
              equals("") == false) {
            String description = "";

            description = "\n<DESCRIPTION>" +
                encodeElement( ( (SavotResource) resourceset.getItemAt(i)).
                              getDescription()) + "</DESCRIPTION>";
            dataBuffOutStream.write(description.getBytes());
          }

          // INFO
          InfoSet infoset = ( (SavotResource) resourceset.getItemAt(i)).
              getInfos();

          // write INFO elements
          writeInfo(infoset);

          // COOSYS elements
          CoosysSet coosysSet = ( (SavotResource) resourceset.getItemAt(i)).
              getCoosys();

          // write COOSYS elements
          writeCoosys(coosysSet);

          // PARAM elements
          ParamSet params = ( (SavotResource) resourceset.getItemAt(i)).
              getParams();

          // write PARAM elements
          writeParam(params);

          // LINK elements
          LinkSet linkSet = ( (SavotResource) resourceset.getItemAt(i)).
              getLinks();

          // write LINK elements
          writeLink(linkSet);

          // TABLE elements
          TableSet tableSet = ( (SavotResource) resourceset.getItemAt(i)).
              getTables();
          String tableline = "";

          for (int k = 0; k < tableSet.getItemCount(); k++) {
            SavotTable table = (SavotTable) tableSet.getItemAt(k);

            tableline = "";

            if ( ( (SavotTable) table).getAbove() != "") {
              tableline = tableline + "\n<!-- " +
                  ( (SavotTable) table).getAbove() + " -->";
            }

            tableline = tableline + "\n<TABLE";

            if (table.getId() != null && !table.getId().equals("")) {
              tableline = tableline + " ID=" + '"' +
                  encodeAttribute(table.getId()) +
                  '"';
            }

            if (table.getName() != null && !table.getName().equals("")) {
              tableline = tableline + " name=" + '"' +
                  encodeAttribute(table.getName()) +
                  '"';
            }

            if (table.getRef() != null && !table.getRef().equals("")) {
              tableline = tableline + " ref=" + '"' +
                  encodeAttribute(table.getRef()) +
                  '"';
            }

            if (table.getUcd() != null && !table.getUcd().equals("")) {
              tableline = tableline + " ucd=" + '"' +
                  encodeAttribute(table.getUcd()) +
                  '"';
            }

            if (table.getUtype() != null && !table.getUtype().equals("")) {
              tableline = tableline + " utype=" + '"' +
                  encodeAttribute(table.getUtype()) +
                  '"';
            }

            if (table.getNrows() != null && !table.getNrows().equals("")) {
              tableline = tableline + " nrows=" + '"' +
                  encodeAttribute(table.getNrows()) +
                  '"';
            }

            tableline = tableline + ">";

            if ( ( (SavotTable) table).getBelow() != "") {
              tableline = tableline + "\n<!-- " +
                  ( (SavotTable) table).getBelow() + " -->\n";
            }

            // DESCRIPTION
            if (table.getDescription().equals("") == false) {
              String description = "\n<DESCRIPTION>" +
                  encodeElement(table.getDescription()) + "</DESCRIPTION>";
              tableline = tableline + description;
            }

            dataBuffOutStream.write(tableline.getBytes());
            tableline = "";

            // FIELD elements
            FieldSet fieldSet = (FieldSet) table.getFields();

            // write FIELD elements
            writeField(fieldSet);

            // PARAM elements
            ParamSet paramSet = (ParamSet) table.getParams();

            // write PARAM elements
            writeParam(paramSet);

            // GROUP elements
            GroupSet groupSet = (GroupSet) table.getGroups();

            // write GROUP elements
            writeGroup(groupSet);

            // LINK elements
            linkSet = (LinkSet) table.getLinks();

            // write LINK elements
            writeLink(linkSet);

            if (table.getData() != null) {
              // <DATA>
              dataBuffOutStream.write(databegin);
              SavotData data = table.getData();
              if (data.getTableData() != null) {
                // <TABLEDATA>
                dataBuffOutStream.write(tabledatabegin);
                SavotTableData tableData = data.getTableData();
                TRSet trs = (TRSet) tableData.getTRs();
                for (int p = 0; p < trs.getItemCount(); p++) {
                  TDSet tds = trs.getTDSet(p);

                  // <TR>
                  dataBuffOutStream.write(trbegin);

                  for (int r = 0; r < tds.getItemCount(); r++) {
                    if ( ( (SavotTD) tds.getItemAt(r)).getAbove() != "") {
                      dataBuffOutStream.write( ("\n<!-- " +
                                                ( (SavotTD) tds.getItemAt(r)).
                                                getAbove() + " -->\n").
                                              getBytes());
                    }
                    // <TD>
                    dataBuffOutStream.write(tdbegin1);

                    // TD attributes
                    if ( ( (SavotTD) tds.getItemAt(r)).getEncoding() != null &&
                        ! ( ( (SavotTD) tds.getItemAt(r)).getEncoding()).
                        equals(
                            "")) {
                      dataBuffOutStream.write( (" encoding=" + '"' +
                                                ( (SavotTD) tds.getItemAt(r)).
                                                getEncoding() + '"').getBytes());
                    }

                    dataBuffOutStream.write(tdbegin2);

                    if ( ( (SavotTD) tds.getItemAt(r)).getBelow() != "") {
                      dataBuffOutStream.write( ("\n<!-- " +
                                                ( (SavotTD) tds.getItemAt(r)).
                                                getBelow() + " -->\n").
                                              getBytes());
                    }
                    if (elementEntities) {
                      dataBuffOutStream.write(encodeElement( (String) tds.
                          getContent(r)).
                                              getBytes());
                    }
                    else {
                      dataBuffOutStream.write(tds.getByteContent(r));
                    }
                    // </TD>
                    dataBuffOutStream.write(tdend);
                  }
                  // </TR>
                  dataBuffOutStream.write(trend);
                }
                // </TABLE>
                dataBuffOutStream.write(tabledataend);
              }

              // write BINARY element
              if (data.getBinary() != null) {
                writeBinary(data.getBinary());
              }

              // write FITS element
              if (data.getFits() != null) {
                writeFits(data.getFits());
              }

              // </DATA>
              dataBuffOutStream.write(dataend);
            }
            // </TABLE>
            dataBuffOutStream.write(tableend);
          }
          if ( ( (SavotResource) resourceset.getItemAt(i)).getResources().
              getItemCount() != 0) {
            writeResource( ( (SavotResource) resourceset.getItemAt(i)).
                          getResources());
          }
          // </RESOURCE>
          dataBuffOutStream.write(resourceend);
        }
      }
    }
    catch (Exception e) {
      System.err.println("writeResource : " + e);
      e.printStackTrace();
    }
  }
}
