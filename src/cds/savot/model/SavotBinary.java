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
* <p>Binary element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotBinary extends MarkupComment implements SimpleTypes {

  // STREAM element
  protected SavotStream stream = null;

  /**
   * Constructor
  */
  public SavotBinary() {
  }

  /**
   * Set the STREAM element
   * @param stream
   */
  public void setStream(SavotStream stream) {
   this.stream = stream;
  }

  /**
   * Get the STREAM element
   * @return SavotStream
   */
  public SavotStream getStream() {
    return stream;
  }
}
