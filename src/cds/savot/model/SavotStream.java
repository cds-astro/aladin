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
* <p>Stream element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotStream extends MarkupComment implements SimpleTypes {

  // content
  protected char[] content = null;

  // type attribute (locator, other)
  protected char[] type = "locator".toCharArray(); // default

  // href attribute
  protected char[] href = null;

  // actuate attribute
  protected char[] actuate = null;

  // width encoding
  protected char[] encoding = null;

  // expires attribute
  protected char[] expires = null;

  // rights attribute
  protected char[] rights = null;

  /**
   * Constructor
  */
  public SavotStream() {
  }

  /**
   * Set type attribute
   * @param type (locator, other)
   */
  public void setType(String type) {
    if (type != null)
      this.type = type.toCharArray();
  }

  /**
   * Get type attribute
   * @return String
   */
  public String getType() {
    if (type != null)
      return String.valueOf(type);
    else return "";
  }

  /**
   * Set href attribute
   * @param href (URI)
   */
  public void setHref(String href) {
    if (href != null)
      this.href = href.toCharArray();
  }

  /**
   * Get href attribute
   * @return String
   */
  public String getHref() {
    if (href != null)
      return String.valueOf(href);
    else return "";
  }

  /**
   * Set actuate attribute
   * @param actuate (onLoad, onRequest, other, none)
   */
  public void setActuate(String actuate) {
    if (actuate != null)
      this.actuate = actuate.toCharArray();
  }

  /**
   * Get actuate attribute
   * @return String
   */
  public String getActuate() {
    if (actuate != null)
      return String.valueOf(actuate);
    else return "";
  }

  /**
   * Set encoding attribute
   * @param encoding (gzip, base64, dynamic, none)
   */
  public void setEncoding(String encoding) {
    if (encoding != null)
      this.encoding = encoding.toCharArray();
  }

  /**
   * Get encoding attribute
   * @return String
   */
  public String getEncoding() {
    if (encoding != null)
      return String.valueOf(encoding);
    else return "";
  }

  /**
   * Set expires attribute
   * @param expires
   */
  public void setExpires(String expires) {
    if (expires != null)
      this.expires = expires.toCharArray();
  }

  /**
   * Get width attribute
   * @return String
   */
  public String getExpires() {
    if (expires != null)
      return String.valueOf(expires);
    else return "";
  }

  /**
   * Set rights attribute
   * @param rights
   */
  public void setRights(String rights) {
    if (rights != null)
      this.rights = rights.toCharArray();
  }

  /**
   * Get rights attribute
   * @return String
   */
  public String getRights() {
    if (rights != null)
      return String.valueOf(rights);
    else return "";
  }

  /**
   * Set content
   * @param content
   */
  public void setContent(String content) {
    if (content != null)
      this.content = content.toCharArray();
  }

  /**
   * Get content
   * @return String
   */
  public String getContent() {
    if (content != null)
      return String.valueOf(content);
    else return "";
  }
}
