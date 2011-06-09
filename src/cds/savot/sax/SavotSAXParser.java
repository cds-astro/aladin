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



package cds.savot.sax;

// java
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

// pull parser
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

/**
* <p>It has been tested with kXML parser implementation </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotSAXParser {

  // the parser engine
  private SavotSAXEngine engine = null;

  /**
   * Constructor
   * @param file a file to parse
  */
  public SavotSAXParser(SavotSAXConsumer consumer, String file) {
      this(consumer, file, false);
  }

 /**
  * Constructor
  * @param consumer SavotSAXConsumer
  * @param file a file to parse
  * @param debug boolean
  */
 public SavotSAXParser(SavotSAXConsumer consumer, String file, boolean debug) {

    try {
      // new parser
      XmlPullParser parser = new KXmlParser();

      engine = new SavotSAXEngine(consumer, parser, file, debug);

      // parse the stream
//      engine.parse(parser);

//    } catch (IOException e){
//      System.err.println("SavotSAXParser : " + e);
    } catch (Exception f){
      System.err.println("SavotSAXParser : " + f);
    }
  }

  /**
   * Constructor
   *
   * @param consumer SavotSAXConsumer
   * @param url url to parse
   * @param enc encoding (example : UTF-8)
   */
  public SavotSAXParser(SavotSAXConsumer consumer, URL url, String enc) {
    this(consumer, url, enc, false);
  }

  /**
   * Constructor
   * @param consumer SavotSAXConsumer
   * @param url url to parse
   * @param enc encoding (example : UTF-8)
   * @param debug boolean
   */
  public SavotSAXParser(SavotSAXConsumer consumer, URL url, String enc, boolean debug) {

    try {
      // new parser
      KXmlParser parser = new KXmlParser();

      engine = new SavotSAXEngine(consumer, parser, url, enc, debug);

      // parse the stream
//      engine.parse(parser);

//    } catch (IOException e){
//      System.err.println("SavotSAXParser : " + e);
    }
    catch (Exception f){
      System.err.println("SavotSAXParser : " + f);
    }
  }

  /**
   * Constructor
   *
   * @param consumer SavotSAXConsumer
   * @param instream stream to parse
   * @param enc encoding (example : UTF-8)
   */
  public SavotSAXParser(SavotSAXConsumer consumer, InputStream instream, String enc) {
    this(consumer, instream, enc, false);
  }

  /**
   * Constructor
   *
   * @param consumer SavotSAXConsumer
   * @param instream stream to parse
   * @param enc encoding (example : UTF-8)
   * @param debug boolean
   */
  public SavotSAXParser(SavotSAXConsumer consumer, InputStream instream, String enc, boolean debug) {
    try {
      // new parser
      KXmlParser parser = new KXmlParser();

      engine = new SavotSAXEngine(consumer, parser, instream, enc, debug);

      // parse the stream
//      engine.parse(parser);

//    } catch (IOException e){
//      System.err.println("SavotSAXParser : " + e);
    }
    catch (Exception f){
      System.err.println("SavotSAXParser : " + f);
    }
  }

  /**
   * Get Parser Version
   * @return String
   */
  public String getVersion() {
    return engine.SAVOTPARSER;
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
      System.out.println("Usage: java SavotSAXParser <xml document>");
    else {
      //     new SavotSAXParser(consumer, argv[0]);
    }
  }
}
