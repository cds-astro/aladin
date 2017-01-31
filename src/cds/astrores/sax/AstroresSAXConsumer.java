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
import java.util.ArrayList;

/**
 * @author Andre Schaaff 
 */
public interface AstroresSAXConsumer {

    // start elements
    public abstract void startAstrores(@SuppressWarnings("rawtypes") ArrayList attributes);

    /*
     * public abstract void startDescription();
     * 
     * public abstract void startResource(ArrayList attributes);
     * 
     * public abstract void startTable(ArrayList attributes);
     * 
     * public abstract void startField(ArrayList attributes);
     * 
     * public abstract void startFieldref(ArrayList attributes);
     * 
     * public abstract void startValues(ArrayList attributes);
     * 
     * public abstract void startStream(ArrayList attributes);
     * 
     * public abstract void startTR();
     * 
     * public abstract void startTD(ArrayList attributes);
     * 
     * public abstract void startData();
     * 
     * public abstract void startBinary();
     * 
     * public abstract void startFits(ArrayList attributes);
     * 
     * public abstract void startTableData();
     * 
     * public abstract void startParam(ArrayList attributes);
     * 
     * public abstract void startParamRef(ArrayList attributes);
     * 
     * public abstract void startLink(ArrayList attributes);
     * 
     * public abstract void startInfo(ArrayList attributes);
     * 
     * public abstract void startMin(ArrayList attributes);
     * 
     * public abstract void startMax(ArrayList attributes);
     * 
     * public abstract void startOption(ArrayList attributes);
     * 
     * public abstract void startGroup(ArrayList attributes);
     * 
     * public abstract void startCoosys(ArrayList attributes);
     * 
     * public abstract void startDefinitions();
     */
    // end elements
    public abstract void endAstrores();

    /*
     * 
     * public abstract void endDescription();
     * 
     * public abstract void endResource();
     * 
     * public abstract void endTable();
     * 
     * public abstract void endField();
     * 
     * public abstract void endFieldref();
     * 
     * public abstract void endValues();
     * 
     * public abstract void endStream();
     * 
     * public abstract void endTR();
     * 
     * public abstract void endTD();
     * 
     * public abstract void endData();
     * 
     * public abstract void endBinary();
     * 
     * public abstract void endFits();
     * 
     * public abstract void endTableData();
     * 
     * public abstract void endParam();
     * 
     * public abstract void endParamRef();
     * 
     * public abstract void endLink();
     * 
     * public abstract void endInfo();
     * 
     * public abstract void endMin();
     * 
     * public abstract void endMax();
     * 
     * public abstract void endOption();
     * 
     * public abstract void endGroup();
     * 
     * public abstract void endCoosys();
     * 
     * public abstract void endDefinitions(); }
     */
    // TEXT
    /*
     * public abstract void textTD(String text);
     * 
     * public abstract void textMin(String text);
     * 
     * public abstract void textMax(String text);
     * 
     * public abstract void textCoosys(String text);
     * 
     * public abstract void textLink(String text);
     * 
     * public abstract void textOption(String text);
     * 
     * public abstract void textGroup(String text);
     * 
     * public abstract void textInfo(String text);
     * 
     * public abstract void textDescription(String text); }
     */
    // document
    public abstract void startDocument();

    public abstract void endDocument();
}
