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


package cds.savot.model;

/**
 * <p>Row element </p>
 * @author Andre Schaaff
 * @version 2.6 Copyright CDS 2002-2005
 *  (kickoff 31 May 02)
 */
public class SavotTR
    extends MarkupComment {

  // TR element set
  protected TDSet TDs = null;
  protected char TDarray[][];
  protected long lineInXMLFile = 0;

  /**
   * Constructor
   */
  public SavotTR() {
  }

  /**
   * Create a TR element from a Separated Value String
   * @param svline String, line with separated values
   * @param sv char, separated value
   */
  public void SVtoTR(String svline, char sv) {

    try {
      int index = 0;
      String token;
      TDs = new TDSet();
      // cut sv following the separator

      // tabulation
      do {
        if ( (index = svline.indexOf(sv)) >= 0) {
          token = svline.substring(0, index);
          svline = svline.substring(index + 1);
        }
        else { // last element
          token = svline;
        }
        SavotTD td = new SavotTD();
        td.setContent(token);
        TDs.addItem(td);
      }
      while (index >= 0);
    }
    catch (Exception e) {
      System.err.println("TSVtoTR :  " + e);
    }
  }

  /**
   * Get the TD set (same as getTDSet)
   * TDSet
   * @return TDSet
   */
  public TDSet getTDs() {
    if (TDs == null) {
      TDs = new TDSet();
    }
    return TDs;
  }

  /**
   * Get the TD set (same as getTDs)
   * TDSet
   * @return TDSet
   */
  public TDSet getTDSet() {
    if (TDs == null) {
      TDs = new TDSet();
    }
    return TDs;
  }

  /**
   * Set the TD set (same as setTDSet)
   * TDSet
   * @param TDs
   */
  public void setTDs(TDSet TDs) {
    this.TDs = TDs;
  }

  /**
   * Set the TD set (same as setTDs)
   * TDSet
   * @param TDs
   */
  public void setTDSet(TDSet TDs) {
    this.TDs = TDs;
  }

  /**
   * Get the TD set
   * TDarray
   * @return TDarray
   */
  public char[][] getTDarray() {
    return TDarray;
  }

  /**
   * Set the TD array
   * TDarray
   * @param TDarray
   */
  public void setTDs(char TDarray[][]) {
    this.TDarray = TDarray;
  }

  /**
   * Get the corresponding line in the XML file or flow
   * @return lineInXMLFile
   */
  public long getLineInXMLFile() {
    return lineInXMLFile;
  }

  /**
   * Set the corresponding line in the XML file or flow during the parsing
   * @param lineInXMLFile
   */
  public void setLineInXMLFile(long lineInXMLFile) {
    this.lineInXMLFile = lineInXMLFile;
  }
}
