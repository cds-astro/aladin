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
* <p>Attribute element, used for other </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotAttribute extends MarkupComment implements SimpleTypes {

  // attribute name
  protected char[] name = null;

  // attribute value
  protected char[] value = null;

  /**
   * Constructor
  */
  public SavotAttribute() {
  }

  /**
   * Set name attribute
   * @param name
   */
  public void setName(String name) {
    if (name != null)
      this.name = name.toCharArray();
  }

  /**
   * Get name attribute
   * @return String
   */
  public String getName() {
    if (name != null)
      return String.valueOf(name);
    else return "";
  }

  /**
   * Set value of attribute
   * @param value
   */
  public void setValue(String value) {
    if (value != null)
      this.value = value.toCharArray();
  }

  /**
   * Get value of attribute
   * @return String
   */
  public String getAttribute() {
    if (value != null)
      return String.valueOf(value);
    else return "";
  }
}
