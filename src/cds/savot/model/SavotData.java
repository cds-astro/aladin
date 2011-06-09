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
* <p>Data element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotData extends MarkupComment implements SimpleTypes {

  // TABLEDATA element
  protected SavotTableData tableData = null;

  // BINARY element
  protected SavotBinary binary = null;

  // FITS element
  protected SavotFits fits = null;

  /**
   * Constructor
  */
  public SavotData() {
  }

  /**
   * Set the TABLEDATA element
   * @param tableData
   */
  public void setTableData(SavotTableData tableData) {
   this.tableData = tableData;
  }

  /**
   * Get the TABLEDATA element
   * @return SavotTableData
   */
  public SavotTableData getTableData() {
    return tableData;
  }

  /**
   * Set the BINARY element
   * @param binary
   */
  public void setBinary(SavotBinary binary) {
   this.binary = binary;
  }

  /**
   * Get the BINARY element
   * @return SavotBinary
   */
  public SavotBinary getBinary() {
    return binary;
  }

  /**
   * Set the FITS element
   * @param fits
   */
  public void setFits(SavotFits fits) {
   this.fits = fits;
  }

  /**
   * Get the FITS element
   * @return SavotFits
   */
  public SavotFits getFits() {
    return fits;
  }
}
