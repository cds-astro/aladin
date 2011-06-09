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
* <p>Other element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotOther extends MarkupComment implements SimpleTypes {

  // attributes
  protected AttributeSet attributes;

  // elements
  protected OtherSet elements;

  // content
  protected char[] content;

  // markup name
  protected char[] name;

  /**
   * Constructor
  */
  public SavotOther() {
  }

  /**
  * Set markup name
  * @param name
  */
  public void setName(String name) {
    if (name != null)
      this.name = name.toCharArray();
  }

  /**
  * Get markup name
  * @return String
  */
  public String getName() {
    if (name != null)
      return String.valueOf(name);
    else return "";
  }

  /**
   * Set the content
   * @param content String
   */
  public void setContent(String content) {
    if (content != null)
      this.content = content.toCharArray();
  }

  /**
   * Get the content
   * @return a String
   */
  public String getContent() {
    if (content != null)
      return String.valueOf(content);
    else return "";
  }

  /**
   * Set the Other elements
   * @param elements
   */
  public void setOther(OtherSet elements) {
    this.elements = elements;
  }

  /**
   * Get the Other elements
   * @return a OtherSet object
   */
  public OtherSet getOther() {
    if (elements == null)
      elements = new OtherSet();
    return elements;
  }

  /**
   * Set the Attribute elements
   * @param attributes
  */
  public void setInfos(AttributeSet attributes) {
    this.attributes = attributes;
  }

  /**
   * Get the Attribute elements
   * @return a AttributeSet object
   */
  public AttributeSet getAttributes() {
    if (attributes == null)
      attributes = new AttributeSet();
    return attributes;
  }
}
