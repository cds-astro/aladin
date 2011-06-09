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
* <p>A data (of a row) </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
* @see SavotSet
*/
public class SavotTD extends MarkupComment {

  // encoding attribute
  char[] encoding = null;

  // TD content
  char[] content = null;

  /**
   * Constructor
  */
  public SavotTD() {
  }

  /**
   * Set the encoding
   * @param encoding String (gzip, base64, dynamic, none)
   */
  public void setEncoding(String encoding) {
    if (encoding != null)
      this.encoding = encoding.toCharArray();
  }

  /**
   * Get the encoding
   * @return a String
   */
  public String getEncoding() {
    if (encoding != null)
      return String.valueOf(encoding);
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

  /**
 * Get element content
 * @return a String
*/
public byte[] getContentBytes() {
  if (content != null)
    return (new String(content)).getBytes();
  else return null;
}

}
