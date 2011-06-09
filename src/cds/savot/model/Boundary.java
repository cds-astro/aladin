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
* <p>For min and max </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class Boundary extends MarkupComment {

  // value attribute
  char[] value = null;

  // inclusive attribute
  char[] inclusive = "yes".toCharArray();

  // MIN, MAX, ... element content
  char[] content = null;

  /**
   * Constructor
  */
  public Boundary() {
  }

  /**
   * Set value attribute
   * @param value
  */
  public void setValue(String value) {
    this.value = value.toCharArray();
  }

  /**
   * Get value attribute
   * @return a String
  */
  public String getValue() {
    if (value != null)
      return String.valueOf(value);
    else return "";
  }

  /**
   * Set inclusive attribute
   * @param inclusive (yes, no)
  */
  public void setInclusive(String inclusive) {
    this.inclusive = inclusive.toCharArray();
  }

  /**
   * Get inclusive attribute
   * @return a String
  */
  public String getInclusive() {
    if (inclusive != null)
      return String.valueOf(inclusive);
    else return "";
  }

  /**
   * Set element content
   * @param content
  */
  public void setContent(String content) {
    this.content = content.toCharArray();
  }

  /**
   * Get element content
   * @return a String
  */
  public String getContent() {
    if (content != null)
      return String.valueOf(content);
    else return "";
  }
}
