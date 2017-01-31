package cds.astrores.sax;

//Copyright 2002-2014 - UDS/CNRS
//The SAVOT library is distributed under the terms
//of the GNU General Public License version 3.
//
//This file is part of SAVOT.
//
// SAVOT is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, version 3 of the License.
//
// SAVOT is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// The GNU General Public License is available in COPYING file
// along with SAVOT.
//
//SAVOT - Simple Access to VOTable - Parser
//
//Author, Co-Author:  Andre Schaaff (CDS), Laurent Bourges (JMMC)
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

// pull parser
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

/**
 * <p>
 * Savot Pull Parser, t has been tested with kXML Pull parser implementation
 * </p>
 * <p>
 * but it is possible to use other pull parsers
 * </p>
 * <p>
 * Designed to use with Pull parsers complient with Standard Pull Implementation
 * v1
 * </p>
 * 
 * @author Andre Schaaff 
 */
@SuppressWarnings({"deprecation", "UseOfSystemOutOrSystemErr"})
public class AstroresSAXParser {

    // the pull parser engine
    private AstroresSAXEngine engine = null;

    /**
     * Constructor
     * 
     * @param consumer
     *            AstroresSAXConsumer
     * @param file
     *            a file to parse
     */
    public AstroresSAXParser(AstroresSAXConsumer consumer, String file) {
        this(consumer, file, false);
    }

    /**
     * Constructor
     * 
     * @param consumer
     *            AstroresSAXConsumer
     * @param file
     *            a file to parse
     * @param debug
     *            boolean
     */
    public AstroresSAXParser(AstroresSAXConsumer consumer, String file,
                             boolean debug) {

        try {
            // new parser
            XmlPullParser parser = new KXmlParser();

            engine = new AstroresSAXEngine(consumer, parser, file, debug);

            engine.parse(parser);

        } catch (IOException e) {
            System.err.println("AstroresSAXParser : " + e);
        } catch (Exception f) {
            System.err.println("AstroresSAXParser : " + f);
        }
    }

    /**
     * Constructor
     * 
     * @param consumer
     *            AstroresSAXConsumer
     * @param url
     *            url to parse
     * @param enc
     *            encoding (example : UTF-8)
     */
    public AstroresSAXParser(AstroresSAXConsumer consumer, URL url, String enc) {
        this(consumer, url, enc, false);
    }

    /**
     * Constructor
     * 
     * @param consumer
     *            AstroresSAXConsumer
     * @param url
     *            url to parse
     * @param enc
     *            encoding (example : UTF-8)
     * @param debug
     *            boolean
     */
    public AstroresSAXParser(AstroresSAXConsumer consumer, URL url, String enc,
                             boolean debug) {

        try {
            // new parser
            KXmlParser parser = new KXmlParser();

            engine = new AstroresSAXEngine(consumer, parser, url, enc, debug);

            engine.parse(parser);

        } catch (IOException e) {
            System.err.println("AstroresSAXParser : " + e);
        } catch (Exception f) {
            System.err.println("AstroresSAXParser : " + f);
        }
    }

    /**
     * Constructor
     * 
     * @param consumer
     *            AstroresSAXConsumer
     * @param instream
     *            stream to parse
     * @param enc
     *            encoding (example : UTF-8)
     */
    public AstroresSAXParser(AstroresSAXConsumer consumer,
                             InputStream instream, String enc) {
        this(consumer, instream, enc, false);
    }

    /**
     * Constructor
     * 
     * @param consumer
     *            AstroresSAXConsumer
     * @param instream
     *            stream to parse
     * @param enc
     *            encoding (example : UTF-8)
     * @param debug
     *            boolean
     */
    public AstroresSAXParser(AstroresSAXConsumer consumer,
                             InputStream instream, String enc, boolean debug) {
        try {
            // new parser
            KXmlParser parser = new KXmlParser();

            engine = new AstroresSAXEngine(consumer, parser, instream, enc,
                    debug);

            engine.parse(parser);

        } catch (IOException e) {
            System.err.println("AstroresSAXParser : " + e);
        } catch (Exception f) {
            System.err.println("AstroresSAXParser : " + f);
        }
    }

    /**
     * Main
     * 
     * @param argv
     * @throws IOException
     */
    public static void main(String[] argv) throws IOException {

        if (argv.length == 0) {
            System.err.println("Usage: java AstroresSAXParser <xml document>");
        } else {
            // new AstroresSAXParser(argv[0]);
        }
    }
}
