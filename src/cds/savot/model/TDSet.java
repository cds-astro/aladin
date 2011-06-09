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
* <p>Set of TD elements </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
* @see SavotSet
*/
public class TDSet extends SavotSet{

  /**
   * Constructor
  */
  public TDSet() {
  }

  /**
   * Get the content at the TDIndex position of the TDSet
   * @param TDIndex
   * @return String
   */
  public String getContent(int TDIndex) {
    return ((SavotTD)this.getItemAt(TDIndex)).getContent();
  }

  /**
   * Get the content at the TDIndex position of the TDSet
   * @param TDIndex
   * @return bytes array
   */
  public byte[] getByteContent(int TDIndex) {
    return getContent(TDIndex).getBytes();
  }
}
