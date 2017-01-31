package cds.astrores.sax;

//Copyright 2002-2014 - UDS/CNRS
//The SAVOT library is distributed under the terms
//of the GNU General Public License version 3.
//
//This file is part of SAVOT.
//
//SAVOT is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, version 3 of the License.
//
//SAVOT is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//The GNU General Public License is available in COPYING file
//along with SAVOT.
//
//SAVOT - Simple Access to VOTable - Parser
//
//Author, Co-Author:  Andre Schaaff (CDS), Laurent Bourges (JMMC)
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

// parser
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;

import cds.savot.model.SavotCoosys;
// model
import cds.savot.model.SavotData;
import cds.savot.model.SavotDefinitions;
import cds.savot.model.SavotInfo;
import cds.savot.model.SavotLink;
import cds.savot.model.SavotOption;
import cds.savot.model.SavotValues;
import cds.table.TableModel;

/**
 * Savot SAX Engine, it has been tested with kXML Pull parser implementation
 * 
 * @author Andre Schaaff 
 */
@SuppressWarnings({"deprecation", "UseOfSystemOutOrSystemErr"})
public class AstroresSAXEngine {

    // markups
    private static String ASTRO = "ASTRO";
    private static String TABLE = "TABLE";
    private static String FIELD = "FIELD";
    private static String TABLEDATA = "TABLEDATA";
    private static String DESCRIPTION = "DESCRIPTION";
    private static String DATA = "DATA";
    private static String RESOURCE = "RESOURCE";
    private static String DEFINITIONS = "DEFINITIONS";
    private static String LINK = "LINK";
    private static String INFO = "INFO";
    private static String ROW = "ROW";
    private static String CELL = "CELL";
    private static String COOSYS = "COOSYS";
    private static String SYSTEM = "SYSTEM";
    private static String OPTION = "OPTION";
    private static String VALUES = "VALUES";
    private static String CSV = "CSV";

    // attributes
    private static String ARRAYSIZE = "arraysize";
    private static String DATATYPE = "datatype";
    private static String EPOCH = "epoch";
    private static String EQUINOX = "equinox";
//    private static String INCLUSIVE = "inclusive";
//    private static String MAX = "max";
//    private static String MIN = "min";
    private static String PRECISION = "precision";
    private static String REF = "ref";
    private static String TYPE = "type";
    private static String UTYPE = "utype"; /* new 1.1 */

    private static String UCD = "ucd";
    private static String UNIT = "unit";
    private static String VALUE = "value";
    private static String WIDTH = "width";
    private static String ID = "ID";
    private static String CONTENTROLE = "content-role";
    private static String CONTENTTYPE = "content-type";
    private static String HREF = "href";
    private static String GREF = "gref";
    private static String ACTION = "action";
//    private static String VERSION = "version";
    private static String INVALID = "invalid";

    // element or attribute
    private static String NAME = "NAME";
    private static String TITLE = "TITLE";
    private static String NULL = "NULL";

    // data model global objects
    private ArrayList<TableModel> allTables = new ArrayList<TableModel>(); // contains all tables

    private boolean trace = false;

    TableModel tb = new TableModel();

    // needed for sequential parsing
    protected XmlPullParser parser = null;

    // Astrores SAX consumer
    protected AstroresSAXConsumer consumer;

    // Statistics
    @SuppressWarnings("unused")
    private int resourceCounter;
    @SuppressWarnings("unused")
    private int tableCounter;
    @SuppressWarnings("unused")
    private int dataCounter;
    @SuppressWarnings("unused")
    private int rowCounter;

    /**
     * Constructor
     * 
     * @param parser
     * @param file
     *            a file to parse
     */
    public AstroresSAXEngine(AstroresSAXConsumer consumer,
                             XmlPullParser parser, String file, boolean debug) {

        try {
            this.parser = parser;
            this.consumer = consumer;

            // set the input of the parser
            FileInputStream inStream = new FileInputStream(new File(file));
            BufferedInputStream dataBuffInStream = new BufferedInputStream(inStream);

            parser.setInput(dataBuffInStream, "UTF-8");

            parse(parser);

        } catch (IOException e) {
            System.err.println("AstroresSAXEngine : " + e);
        } catch (Exception f) {
            System.err.println("AstroresSAXEngine : " + f);
        }
    }

    /**
     * Constructor
     * 
     * @param parser
     * @param url
     *            url to parse
     * @param enc
     *            encoding (example : UTF-8)
     */
    public AstroresSAXEngine(AstroresSAXConsumer consumer,
                             XmlPullParser parser, URL url, String enc, boolean debug) {

        try {
            this.parser = parser;
            this.consumer = consumer;

            // set the input of the parser (with the given encoding)
            parser.setInput(new DataInputStream(url.openStream()), enc);

            parse(parser);

        } catch (IOException e) {
            System.err.println("AstroresSAXEngine : " + e);
        } catch (Exception f) {
            System.err.println("AstroresSAXEngine : " + f);
        }
    }

    /**
     * Constructor
     * 
     * @param parser
     * @param instream
     *            stream to parse
     * @param enc
     *            encoding (example : UTF-8)
     */
    public AstroresSAXEngine(AstroresSAXConsumer consumer,
                             XmlPullParser parser, InputStream instream, String enc,
                             boolean debug) {
        try {
            this.parser = parser;
            this.consumer = consumer;

            // DataInputStream dataInStream = new DataInputStream(instream);
            BufferedInputStream dataBuffInStream = new BufferedInputStream(instream);

            // set the input of the parser (with the given encoding)
            parser.setInput(dataBuffInStream, enc);

            parse(parser);

        } catch (IOException e) {
            System.err.println("AstroresSAXEngine : " + e);
        } catch (Exception f) {
            System.err.println("AstroresSAXEngine : " + f);
        }
    }

    /**
     * 
     * @param buffer
     *            String
     */
    @SuppressWarnings("unchecked")
    private void CSVCut(String buffer) {

        int begin = 0;
        int linecount = 0;
        int headlines = 3;
        char colsep = 9; // TAB
        char recsep = 10; // TAB

        if (trace) {
            System.err.println("Entree CSVCut");
        }

        for (int i = 0; i < buffer.length(); i++) {

            if (buffer.charAt(i) == recsep) {
                linecount++;

                @SuppressWarnings("rawtypes")
                ArrayList row = new ArrayList();

                if (linecount < headlines) {
                    System.err.println("L" + linecount + " ---> " + buffer.substring(begin, i));
                }
                int begin2 = begin;
                for (int j = begin; j <= i; j++) {

                    if (buffer.charAt(j) == colsep || buffer.charAt(j) == recsep) {
                        if (linecount < headlines) {
                            System.err.println("colsep-->");
                            System.err.println("---> "
                                    + buffer.substring(begin2, j));
                        } else { // line storage
                            // add value to the current row
                            row.add(buffer.substring(begin2, j));
                        }
                        begin2 = j + 1;
                    }
                }
                begin = i + 1;
                tb.addRow(row);
            }

            /*
             * if (buffer.charAt(i) == 10) { System.err.println("---> " +
             * buffer.substring(debut, i)); StringTokenizer sb = new
             * StringTokenizer(buffer.substring(debut, i)); while
             * (sb.hasMoreTokens()) System.err.println(sb.nextToken());
             * 
             * debut = i + 1; }
             */
        }
        System.err.println("row count : " + tb.getRowCount());
        /*
         * StringTokenizer sb = new StringTokenizer(buffer);
         * System.err.println("Token count : " + sb.countTokens()); while
         * (sb.hasMoreTokens()) System.err.println(sb.nextToken());
         */
    }

    /**
     * Parsing engine
     * 
     * @param parser
     *            an XML pull parser (example : kXML)
     * @throws IOException
     * 
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void parse(XmlPullParser parser) throws IOException {

        String name = "";
        ArrayList father = new ArrayList();
        String currentMarkup = "XML";

        ArrayList currentRow = new ArrayList();
        StringBuffer description = new StringBuffer(); // global description
        @SuppressWarnings("unused")
        String id = null; // current resource id
        @SuppressWarnings("unused")
        String type = null; // current resource type

        ArrayList fieldPropertyNames = new ArrayList();
        ArrayList fieldPropertyValues = new ArrayList();

        @SuppressWarnings("unused")
        SavotData currentData = new SavotData();
        SavotValues currentValues = new SavotValues();
        // SavotTableData currentTableData = new SavotTableData();
        String currentDescription = "";
        SavotLink currentLink = new SavotLink();
        SavotInfo currentInfo = new SavotInfo();
        SavotOption currentOption = new SavotOption();
        SavotCoosys currentCoosys = new SavotCoosys();
        SavotDefinitions currentDefinitions = new SavotDefinitions();

        try {

            // envent type
            int eventType = parser.getEventType();
            int previousDepth = 0;

            // while the end of the document is not reach
            while (eventType != XmlPullParser.END_DOCUMENT) {
                // treatment depending on event type
                switch (eventType) {
                    // if a start tag is reach
                    case KXmlParser.START_TAG:
                        try {
                            // the name of the current tag
                            name = parser.getName();

                            if (trace) {
                                System.err.println("Name ---> " + parser.getName());
                            }

                            if (name != null) {

                                // ASTRO
                                if (name.equalsIgnoreCase(ASTRO)) {
                                    if (trace) {
                                        System.err.println("VOTABLE begin");
                                    }
                                    currentMarkup = ASTRO;
                                } else // DESCRIPTION
                                if (name.equalsIgnoreCase(DESCRIPTION)) {
                                    currentMarkup = DESCRIPTION;
                                    if (trace) {
                                        System.err.println("DESCRIPTION begin");
                                    }
                                } // RESOURCE
                                else if (name.equalsIgnoreCase(RESOURCE)) {

                                    if (trace) {
                                        System.err.println("RESOURCE begin");
                                    }
                                    currentMarkup = RESOURCE;

                                    // for statistics only
                                    resourceCounter++;

                                    // all resource properties init
                                    id = "";
                                    type = "";

                                    if (parser.getAttributeCount() != 0) {
                                        if (parser.getAttributeValue(null, TYPE) != null) {
                                            type = parser.getAttributeValue(null, TYPE);
                                        }
                                        if (parser.getAttributeValue(null, ID) != null) {
                                            id = parser.getAttributeValue(null, ID);
                                        }
                                    }
                                } // TABLE
                                else if (name.equalsIgnoreCase(TABLE)) {
                                    tb = new TableModel();
                                    currentMarkup = TABLE;

                                    // for statistics only
                                    tableCounter++;

                                    if (parser.getAttributeCount() != 0 && parser.getAttributeValue(null, ID) != null) {
                                        tb.setId(parser.getAttributeValue(null, ID));
                                    }
                                } // FIELD
                                else if (name.equalsIgnoreCase(FIELD)) {
                                    fieldPropertyNames = new ArrayList();
                                    fieldPropertyValues = new ArrayList();
                                    currentMarkup = FIELD;
                                    if (parser.getAttributeCount() != 0) {
                                        if (parser.getAttributeValue(null, UNIT) != null) {
                                            fieldPropertyNames.add(UNIT);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, UNIT));
                                        }
                                        if (parser.getAttributeValue(null, DATATYPE) != null) {
                                            fieldPropertyNames.add(DATATYPE);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, DATATYPE));
                                        }
                                        if (parser.getAttributeValue(null, PRECISION) != null) {
                                            fieldPropertyNames.add(PRECISION);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, PRECISION));
                                        }
                                        if (parser.getAttributeValue(null, WIDTH) != null) {
                                            fieldPropertyNames.add(WIDTH);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, WIDTH));
                                        }
                                        if (parser.getAttributeValue(null, REF) != null) {
                                            fieldPropertyNames.add(REF);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, REF));
                                        }
                                        if (parser.getAttributeValue(null, NAME) != null) {
                                            fieldPropertyNames.add(NAME);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, NAME));
                                        }
                                        if (parser.getAttributeValue(null, UCD) != null) {
                                            fieldPropertyNames.add(UCD);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, UCD));
                                        }
                                        if (parser.getAttributeValue(null, ARRAYSIZE) != null) {
                                            fieldPropertyNames.add(ARRAYSIZE);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, ARRAYSIZE));
                                        }
                                        if (parser.getAttributeValue(null, TYPE) != null) {
                                            fieldPropertyNames.add(TYPE);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, TYPE));
                                        }
                                        if (parser.getAttributeValue(null, UTYPE) != null) {
                                            fieldPropertyNames.add(UTYPE);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, UTYPE));
                                        }
                                        if (parser.getAttributeValue(null, ID) != null) {
                                            fieldPropertyNames.add(ID);
                                            fieldPropertyValues.add(parser.getAttributeValue(null, ID));
                                        }
                                    }
                                } // VALUES
                                else if (name.equalsIgnoreCase(VALUES)) {
                                    currentValues = new SavotValues();
                                    currentMarkup = VALUES;
                                    if (parser.getAttributeCount() != 0) {
                                        if (parser.getAttributeValue(null, TYPE) != null) {
                                            currentValues.setType(parser.getAttributeValue(null, TYPE));
                                        }
                                        if (parser.getAttributeValue(null, NULL) != null) {
                                            currentValues.setNull(parser.getAttributeValue(null, NULL));
                                        }
                                        if (parser.getAttributeValue(null, INVALID) != null) /*
                                         * 1.0
                                         * mais
                                         * non
                                         * 1.1
                                         */ {
                                            currentValues.setInvalid(parser.getAttributeValue(null, INVALID));
                                        }
                                        if (parser.getAttributeValue(null, REF) != null) /*
                                         * 1.0
                                         * mais
                                         * non
                                         * 1.1
                                         */ {
                                            currentValues.setRef(parser.getAttributeValue(null, REF));
                                        }
                                        if (parser.getAttributeValue(null, ID) != null) {
                                            currentValues.setId(parser.getAttributeValue(null, ID));
                                        }
                                    }
                                } // ROW
                                else if (name.equalsIgnoreCase(ROW)) {
                                    if (trace) {
                                        System.err.println("ROW begin");
                                    }
                                    currentMarkup = ROW;

                                    // create a new row
                                    currentRow = new ArrayList();
                                } // CELL
                                else if (name.equalsIgnoreCase(CELL)) {
                                    if (trace) {
                                        System.err.println("CELL begin");
                                    }
                                    currentMarkup = CELL;

                                    // for statistics only
                                    dataCounter++;
                                } // DATA
                                else if (name.equalsIgnoreCase(DATA)) {
                                    currentData = new SavotData();
                                    currentMarkup = DATA;
                                } // TABLEDATA
                                else if (name.equalsIgnoreCase(TABLEDATA)) {
                                    // currentTableData = new SavotTableData();
                                    currentMarkup = TABLEDATA;
                                } // LINK
                                else if (name.equalsIgnoreCase(LINK)) {
                                    currentLink = new SavotLink();
                                    currentMarkup = LINK;

                                    if (parser.getAttributeCount() != 0) {
                                        if (parser.getAttributeValue(null, CONTENTROLE) != null) {
                                            currentLink.setContentRole(parser.getAttributeValue(null, CONTENTROLE));
                                        }
                                        if (parser.getAttributeValue(null, CONTENTTYPE) != null) {
                                            currentLink.setContentType(parser.getAttributeValue(null, CONTENTTYPE));
                                        }
                                        if (parser.getAttributeValue(null, TITLE) != null) {
                                            currentLink.setTitle(parser.getAttributeValue(null, TITLE));
                                        }
                                        if (parser.getAttributeValue(null, VALUE) != null) {
                                            currentLink.setValue(parser.getAttributeValue(null, VALUE));
                                        }
                                        if (parser.getAttributeValue(null, HREF) != null) {
                                            currentLink.setHref(parser.getAttributeValue(null, HREF));
                                        }
                                        if (parser.getAttributeValue(null, GREF) != null) {
                                            currentLink.setGref(parser.getAttributeValue(null, GREF));
                                        }
                                        if (parser.getAttributeValue(null, ACTION) != null) {
                                            currentLink.setAction(parser.getAttributeValue(null, ACTION));
                                        }
                                        if (parser.getAttributeValue(null, ID) != null) {
                                            currentLink.setId(parser.getAttributeValue(null, ID));
                                        }
                                    }
                                    if (trace) {
                                        System.err.println("LINK");
                                    }
                                } // INFO
                                else if (name.equalsIgnoreCase(INFO)) {
                                    currentInfo = new SavotInfo();
                                    currentMarkup = INFO;
                                    if (parser.getAttributeCount() != 0) {
                                        if (parser.getAttributeValue(null, NAME) != null) {
                                            currentInfo.setName(parser.getAttributeValue(null, NAME));
                                        }
                                        if (parser.getAttributeValue(null, VALUE) != null) {
                                            currentInfo.setValue(parser.getAttributeValue(null, VALUE));
                                        }
                                        if (parser.getAttributeValue(null, ID) != null) {
                                            currentInfo.setId(parser.getAttributeValue(null, ID));
                                        }
                                    }
                                    if (trace) {
                                        System.err.println("INFO");
                                    }
                                } // OPTION
                                else if (name.equalsIgnoreCase(OPTION)) {
                                    currentMarkup = OPTION;
                                    if (trace) {
                                        System.err.println("OPTION");
                                    }
                                    currentOption = new SavotOption();
                                    if (parser.getAttributeCount() != 0) {
                                        if (parser.getAttributeValue(null, NAME) != null) {
                                            currentOption.setName(parser.getAttributeValue(null, NAME));
                                        }
                                        if (parser.getAttributeValue(null, VALUE) != null) {
                                            currentOption.setValue(parser.getAttributeValue(null, VALUE));
                                        }
                                    }
                                } else if (name.equalsIgnoreCase(COOSYS)) {
                                    currentMarkup = COOSYS;
                                    if (trace) {
                                        System.err.println("COOSYS");
                                    }
                                    currentCoosys = new SavotCoosys();
                                    if (parser.getAttributeCount() != 0) {
                                        if (parser.getAttributeValue(null, EQUINOX) != null) {
                                            currentCoosys.setEquinox(parser.getAttributeValue(null, EQUINOX));
                                        }
                                        if (parser.getAttributeValue(null, EPOCH) != null) {
                                            currentCoosys.setEpoch(parser.getAttributeValue(null, EPOCH));
                                        }
                                        if (parser.getAttributeValue(null, SYSTEM) != null) {
                                            currentCoosys.setSystem(parser.getAttributeValue(null, SYSTEM));
                                        }
                                        if (parser.getAttributeValue(null, ID) != null) {
                                            currentCoosys.setId(parser.getAttributeValue(null, ID));
                                        }
                                    }
                                } // DEFINITIONS
                                else if (name.equalsIgnoreCase(DEFINITIONS)) {
                                    currentMarkup = DEFINITIONS;
                                    currentDefinitions = new SavotDefinitions();
                                    if (trace) {
                                        System.err.println("DEFINITIONS");
                                    }
                                }
                            }
                            currentMarkup = name;
                        } catch (Exception e) {
                            System.err.println("START_TAG : " + e);
                        }
                        break;

                    // if an end tag is reach
                    case KXmlParser.END_TAG:
                        name = parser.getName();
                        try {

                            if (trace) {
                                System.err.println("End ---> " + name);
                            }

                            // DESCRIPTION - several fathers are possible
                            if (name.equalsIgnoreCase(DESCRIPTION)) {
                                if (((String) father.get(father.size() - 2)).equalsIgnoreCase(ASTRO)) {
                                    description.append(currentDescription);
                                    currentMarkup = "";
                                } else if (((String) father.get(father.size() - 2)).equalsIgnoreCase(RESOURCE)) {
                                    description.append(currentDescription);
                                    currentMarkup = "";
                                } else if (((String) father.get(father.size() - 2)).equalsIgnoreCase(TABLE)) {
                                    tb.setDescription(currentDescription);
                                    currentMarkup = "";
                                } else if (((String) father.get(father.size() - 2)).equalsIgnoreCase(FIELD)) {
                                    fieldPropertyNames.add(DESCRIPTION);
                                    fieldPropertyValues.add(currentDescription);
                                    currentMarkup = "";
                                }
                            } // TABLE
                            else if (name.equalsIgnoreCase(TABLE)) {
                                allTables.add(tb);
                                currentMarkup = "";

                                if (trace) {
                                    System.err.println(tb.getName());
                                }
                            } // FIELD - several fathers are possible
                            else if (name.equalsIgnoreCase(FIELD)) {
                                if (((String) father.get(father.size() - 2)).equalsIgnoreCase(TABLE)) {
                                    tb.setTableProperties(fieldPropertyNames, fieldPropertyValues);
                                    if (trace) {
                                        System.err.println("FIELD from TABLE father = " + father);
                                    }
                                }
                            } // TR
                            else if (name.equalsIgnoreCase(ROW)) {
                                if (trace) {
                                    System.err.println("TR end");
                                }
                                currentMarkup = "";

                                // add the row to the table
                                tb.addRow((String[]) currentRow.toArray());

                                // for statistics only
                                rowCounter++;

                                if (trace) {
                                    System.err.println("ADD row");
                                }

                            } // DATA
                            else if (name.equalsIgnoreCase(DATA)) {
                                currentMarkup = "";
                                // currentTable.setData(currentData);
                            } // CELL
                            else if (name.equalsIgnoreCase(CELL)) {
                                currentMarkup = "";
                                if (trace) {
                                    System.err.println("CELL end");
                                }
                            } // RESOURCE
                            else if (name.equalsIgnoreCase(RESOURCE)) {
                                if (trace) {
                                    System.err.println("RESOURCE end");
                                }
                                currentMarkup = "";
                            } // OPTION
                            else if (name.equalsIgnoreCase(OPTION)) {
                                if (trace) {
                                    System.err.println("OPTION end");
                                }
                                currentMarkup = "";
                            } // TABLEDATA
                            else if (name.equalsIgnoreCase(TABLEDATA)) {
                                currentMarkup = "";
                                // currentData.setTableData(currentTableData);
                                if (trace) {
                                    System.err.println(tb.getName());
                                }
                            } // COOSYS
                            else if (name.equalsIgnoreCase(COOSYS)) {
                                currentMarkup = "";
                                // COOSYS - several fathers are possible
                                if (((String) father.get(father.size() - 2)).equalsIgnoreCase(DEFINITIONS)) {
                                    currentDefinitions.getCoosys().addItem(currentCoosys);
                                    if (trace) {
                                        System.err.println("COOSYS from DEFINITIONS father = " + father);
                                    }
                                } else if (((String) father.get(father.size() - 2)).equalsIgnoreCase(RESOURCE)) {
                                    // currentResource.getCoosys().addItem(currentCoosys);
                                    if (trace) {
                                        System.err.println("COOSYS from RESOURCE father = " + father);
                                    }
                                }
                            } // LINK
                            else if (name.equalsIgnoreCase(LINK)) {
                                currentMarkup = "";
                                // LINK - several fathers are possible
                                if (((String) father.get(father.size() - 2)).equalsIgnoreCase(RESOURCE)) {
                                    // currentResource.getLinks().addItem(currentLink);
                                    if (trace) {
                                        System.err.println("LINK from RESOURCE father = " + father);
                                    }
                                } else if (((String) father.get(father.size() - 2)).equalsIgnoreCase(TABLE)) {
                                    // currentTable.getLinks().addItem(currentLink);
                                    if (trace) {
                                        System.err.println("LINK from TABLE father = " + father);
                                    }
                                } else if (((String) father.get(father.size() - 2)).equalsIgnoreCase(FIELD)) {
                                    // currentField.getLinks().addItem(currentLink);
                                    if (trace) {
                                        System.err.println("LINK from FIELD father = " + father);
                                    }
                                }
                            } // VALUES
                            else if (name.equalsIgnoreCase(VALUES)) {
                                currentMarkup = "";
                                // VALUES - several fathers are possible
                                if (((String) father.get(father.size() - 2)).equalsIgnoreCase(FIELD)) {
                                    // currentField.setValues(currentValues);
                                    if (trace) {
                                        System.err.println("VALUES from FIELD father = " + father + " ID : " + currentValues.getId());
                                    }
                                }
                            } // INFO
                            else if (name.equalsIgnoreCase(INFO)) {
                                currentMarkup = "";
                                if (trace) {
                                    System.err.println("INFO father = " + father);
                                }
                                // INFO - several fathers are possible
                                if (((String) father.get(father.size() - 2)).equalsIgnoreCase(ASTRO)) {
                                    if (trace) {
                                        System.err.println("INFO from VOTABLE father = " + father);
                                    }
                                } else if (((String) father.get(father.size() - 2)).equalsIgnoreCase(RESOURCE)) {
                                    if (trace) {
                                        System.err.println("INFO from RESOURCE father = " + father);
                                    }
                                }
                            } // DEFINITIONS
                            else if (name.equalsIgnoreCase(DEFINITIONS)) {
                                currentMarkup = "";
                                if (trace) {
                                    System.err.println("DEFINITIONS");
                                }
                                // allResources.setDefinitions(currentDefinitions);
                            }
                        } catch (Exception e) {
                            System.err.println("FATHER : " + father + " END_TAG ("
                                    + name + ") : " + e);
                        }
                        break;

                    case KXmlParser.END_DOCUMENT:
                        try {
                            if (trace) {
                                System.err.println("Document end reached!");
                            }
                        } catch (Exception e) {
                            System.err.println("END_DOCUMENT : " + e);
                        }
                        break;

                    case KXmlParser.TEXT:
                        try {
                            if (currentMarkup.equalsIgnoreCase(CSV)) {
                                // System.err.println((parser.getText()).trim());
                                CSVCut((parser.getText()).trim());
                                if (trace) {
                                    System.err.println((parser.getText()).trim());
                                }

                                // currentRow.addElement(parser.getText());
                            } else // add a data to the current row
                            if (currentMarkup.equalsIgnoreCase(CELL)) {
                                if (trace) {
                                    System.err.println("CELL : " + (parser.getText()).trim());
                                }
                                currentRow.add(parser.getText());
                            } else if (currentMarkup.equalsIgnoreCase(DESCRIPTION)) {
                                if (trace) {
                                    System.err.println("DESCRIPTION : " + (parser.getText()).trim());
                                }
                                currentDescription = (parser.getText()).trim();
                            } else if (currentMarkup.equalsIgnoreCase(COOSYS)) {
                                if (trace) {
                                    System.err.println("COOSYS : " + (parser.getText()).trim());
                                }
                                currentCoosys.setContent((parser.getText()).trim());
                            } else if (currentMarkup.equalsIgnoreCase(LINK)) {
                                if (trace) {
                                    System.err.println("LINK : " + (parser.getText()).trim());
                                }
                                currentLink.setContent((parser.getText()).trim());
                            } else if (currentMarkup.equalsIgnoreCase(OPTION)) {
                                if (trace) {
                                    System.err.println("OPTION : " + (parser.getText()).trim());
                                }
                            } else if (currentMarkup.equalsIgnoreCase(INFO)) {
                                currentInfo.setContent((parser.getText()).trim());
                                if (trace) {
                                    System.err.println("INFO : " + (parser.getText()).trim());
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("TEXT : " + e);
                        }
                        break;

                    case KXmlParser.START_DOCUMENT:
                        break;

                    default:
                        if (trace) {
                            System.err.println(" ignoring some other (legacy) event at line : " + parser.getLineNumber());
                        }
                }

                // save previous values
                previousDepth = parser.getDepth();

                // new values
                eventType = parser.next();
                if (parser.getDepth() > previousDepth) {
                    father.add((parser.getName()));
                } else if (parser.getDepth() == previousDepth) {
                    if (((String) father.get(father.size() - 1)).equals((parser.getName()))) {
                        father.remove(father.size() - 1);
                        father.add((parser.getName()));
                    }
                } else {
                    father.remove(father.size() - 1);
                }
                if (trace) {
                    System.err.println("father = " + father);
                }
            }

        } catch (Exception f) {
            if (trace) {
                System.err.println("parse : " + f);
            }
        }
    }
}
