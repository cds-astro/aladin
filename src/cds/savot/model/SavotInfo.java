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
* <p>Info element  - deprecated since VOTable 1.1</p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotInfo extends MarkupComment implements SimpleTypes {

  // id attribute
  char[] id = null;

  // name attribute
  char[] name = null;

  // value attribute
  char[] value = null;

  // INFO element content
  char[] content = null;

  /**
   * Constructor
  */
  public SavotInfo() {
  }

  /**
  * Set ID attribute
  * @param id
  */
  public void setId(String id) {
    if (id != null)
      this.id = id.toCharArray();
  }

  /**
  * Get ID attribute
  * @return String
  */
  public String getId() {
    if (id != null)
      return String.valueOf(id);
    else return "";
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
  * Set value attribute
  * @param value
  */
  public void setValue(String value) {
    if (value != null)
      this.value = value.toCharArray();
  }

  /**
  * Get value attribute
  * @return String
  */
  public String getValue() {
    if (value != null)
      return String.valueOf(value);
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
