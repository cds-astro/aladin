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
* <p>Definitions element - removed in VOTable 1.1</p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotDefinitions extends MarkupComment implements SimpleTypes {

  // COOSYS elements
  protected CoosysSet coosys = null;

  // PARAM elements
  protected ParamSet params = null;

  /**
    * Constructor
  */
  public SavotDefinitions() {
  }

  /**
   * Get COOSYS set reference
   * removed in VOTable 1.1
   * @return a CoosysSet reference
  */
  public CoosysSet getCoosys() {
    if (coosys == null)
      coosys = new CoosysSet();
    return coosys;
  }

  /**
   * Set COOSYS set reference
   * removed in VOTable 1.1
   * @param coosys
   */
  public void setCoosys(CoosysSet coosys) {
    this.coosys = coosys;
  }

  /**
    * Get PARAM set reference
    * removed in VOTable 1.1
    * @return a ParamSet reference
  */
  public ParamSet getParams() {
    if (params == null)
      params = new ParamSet();
    return params;
  }

  /**
    * Set PARAM set reference
    * removed in VOTable 1.1
    * @param params
  */
  public void setParams(ParamSet params) {
    this.params = params;
  }
}
