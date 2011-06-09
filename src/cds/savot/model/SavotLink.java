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
* <p>Link element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotLink extends MarkupComment implements SimpleTypes {

  // content
  protected char[] content = null;

  // ID attribute
  char[] id = null;

  // content-role attribute
  char[] contentRole = null;

  // content-type attribute
  char[] contentType = null;

  // title attribute
  char[] title = null;

  // value attribute
  char[] value = null;

  // href attribute
  char[] href = null;

  // gref attribute - removed in VOTable 1.1
  char[] gref = null;

  // action attribute
  char[] action = null;

  /**
   * Constructor
  */
  public SavotLink() {
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

  /**
  * Set ID attribute
  * @param id
  */
  public void setID(String id) {
    if (id != null)
      this.id = id.toCharArray();
  }

  /**
  * Get ID attribute
  * @return String
  */
  public String getID() {
    if (id != null)
      return String.valueOf(id);
    else return "";
  }

  /**
  * Set contentRole attribute
  * @param contentRole (query, hints, doc, location)
  */
  public void setContentRole(String contentRole) {
    if (contentRole != null)
      this.contentRole = contentRole.toCharArray();
  }

  /**
  * Get contentRole attribute
  * @return String
  */
  public String getContentRole() {
    if (contentRole != null)
      return String.valueOf(contentRole);
    else return "";
  }

  /**
  * Set contentType attribute
  * @param contentType
  */
  public void setContentType(String contentType) {
    if (contentType != null)
      this.contentType = contentType.toCharArray();
  }

  /**
  * Get contentType attribute
  * @return String
  */
  public String getContentType() {
    if (contentType != null)
      return String.valueOf(contentType);
    else return "";
  }

  /**
  * Set title attribute
  * @param title
  */
  public void setTitle(String title) {
    if (title != null)
      this.title = title.toCharArray();
  }

  /**
  * Get title attribute
  * @return String
  */
  public String getTitle() {
    if (title != null)
      return String.valueOf(title);
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
   * Set href attribute
   * @param href
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
   * Set gref attribute
   * removed in VOTable 1.1
   * @param gref
   */
  public void setGref(String gref) {
    if (gref != null)
      this.gref = gref.toCharArray();
  }

  /**
   * Get gref attribute
   * removed in VOTable 1.1
   * @return String
   */
  public String getGref() {
    if (gref != null)
      return String.valueOf(gref);
    else return "";
  }

  /**
   * Set action attribute
   * @param action
   */
  public void setAction(String action) {
    if (action != null)
      this.action = action.toCharArray();
  }

  /**
   * Get action attribute
   * @return String
   */
  public String getAction() {
    if (action != null)
      return String.valueOf(action);
    else return "";
  }
 }
