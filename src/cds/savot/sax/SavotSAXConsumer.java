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

import java.util.Vector;

/**
* <p>This interface must be implemented to use the Savot SAX parser </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/

public interface SavotSAXConsumer {

  // start elements

  public abstract void startVotable(Vector attributes);

  public abstract void startDescription();

  public abstract void startResource(Vector attributes);

  public abstract void startTable(Vector attributes);

  public abstract void startField(Vector attributes);

  public abstract void startFieldref(Vector attributes);

  public abstract void startValues(Vector attributes);

  public abstract void startStream(Vector attributes);

  public abstract void startTR();

  public abstract void startTD(Vector attributes);

  public abstract void startData();

  public abstract void startBinary();

  public abstract void startFits(Vector attributes);

  public abstract void startTableData();

  public abstract void startParam(Vector attributes);

  public abstract void startParamRef(Vector attributes);

  public abstract void startLink(Vector attributes);

  public abstract void startInfo(Vector attributes);

  public abstract void startMin(Vector attributes);

  public abstract void startMax(Vector attributes);

  public abstract void startOption(Vector attributes);

  public abstract void startGroup(Vector attributes);

  public abstract void startCoosys(Vector attributes);

  public abstract void startDefinitions(); // deprecated since VOTable 1.1

  // end elements

  public abstract void endVotable();

  public abstract void endDescription();

  public abstract void endResource();

  public abstract void endTable();

  public abstract void endField();

  public abstract void endFieldref();

  public abstract void endValues();

  public abstract void endStream();

  public abstract void endTR();

  public abstract void endTD();

  public abstract void endData();

  public abstract void endBinary();

  public abstract void endFits();

  public abstract void endTableData();

  public abstract void endParam();

  public abstract void endParamRef();

  public abstract void endLink();

  public abstract void endInfo();

  public abstract void endMin();

  public abstract void endMax();

  public abstract void endOption();

  public abstract void endGroup();

  public abstract void endCoosys();

  public abstract void endDefinitions(); // deprecated since VOTable 1.1

  // TEXT

  public abstract void textTD(String text);

  public abstract void textMin(String text);

  public abstract void textMax(String text);

  public abstract void textCoosys(String text);

  public abstract void textLink(String text);

  public abstract void textOption(String text);

  public abstract void textGroup(String text);

  public abstract void textInfo(String text);

  public abstract void textDescription(String text);

  public abstract void textStream(String text);

  // document

  public abstract void startDocument();

  public abstract void endDocument();
}
