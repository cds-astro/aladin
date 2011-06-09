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
* <p>Table element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotTable extends MarkupComment implements SimpleTypes {

  // id attribute
  protected char[] id = null;

  // name attribute
  protected char[] name = null;

  // ucd attribute
  protected char[] ucd = null;

  // utype attribute
  protected char[] utype = null;

  // ref attribute
  protected char[] ref = null;

  // nrows attribute
  protected char[] nrows = null;

  // DESCRIPTION element
  protected char[] description = null;

  // FIELD element
  protected FieldSet fields = null;

  // PARAM element
  protected ParamSet params = null;

  // GROUP element
  protected GroupSet groups = null;

  // LINK element
  protected LinkSet links = null;

  // DATA element
  protected SavotData data = null;

  /**
   * Constructor
  */
  public SavotTable() {
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
   * @return a String
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
   * @return a String
  */
  public String getName() {
    if (name != null)
      return String.valueOf(name);
    else return "";
  }

  /**
   * Set ucd attribute
   * @param ucd
  */
  public void setUcd(String ucd) {
    if (ucd != null)
      this.ucd = ucd.toCharArray();
  }

  /**
   * Get ucd attribute
   * @return a String
  */
  public String getUcd() {
    if (ucd != null)
      return String.valueOf(ucd);
    else return "";
  }

  /**
   * Set utype attribute
   * @param utype
  */
  public void setUtype(String utype) {
    if (utype != null)
      this.utype = utype.toCharArray();
  }

  /**
   * Get utype attribute
   * @return a String
  */
  public String getUtype() {
    if (utype != null)
      return String.valueOf(utype);
    else return "";
  }

  /**
   * Set ref attribute
   * @param ref
  */
  public void setRef(String ref) {
    if (ref != null)
      this.ref = ref.toCharArray();
  }

  /**
   * Get ref attribute
   * @return a String
  */
  public String getRef() {
    if (ref != null)
      return String.valueOf(ref);
    else return "";
  }

  /**
   * Set nrows attribute
   * @param nrows
  */
  public void setNrows(String nrows) {
    if (nrows != null)
      this.nrows = nrows.toCharArray();
  }

  /**
   * Set nrows attribute
   * @param nrows
  */
  public void setNrowsValue(int nrows) {
    if (nrows >= 0)
      this.nrows = Integer.toString(nrows).toCharArray();
  }

  /**
   * Get nrows attribute
   * @return a String
  */
  public String getNrows() {
    if (nrows != null)
      return String.valueOf(nrows);
    else return "";
  }

  /**
   * Get nrows attribute
   * @return an int
  */
  public int getNrowsValue() {
    if (nrows != null)
      return (Integer.valueOf(String.valueOf(nrows))).intValue();
    else return 0;
  }

  /**
   * Set DESCRIPTION content
   * @param description
  */
  public void setDescription(String description) {
    if (description != null)
      this.description = description.toCharArray();
  }

  /**
   * Get DESCRIPTION content
   * @return a String
  */
  public String getDescription() {
    if (description != null)
      return String.valueOf(description);
    else return "";
  }

  /**
   * Get FIELD element set reference
   * @return FieldSet
  */
  public FieldSet getFields() {
    if (fields == null)
      fields = new FieldSet();
    return fields;
  }

  /**
   * Set FIELD element set reference
   * @param fields
   */
  public void setFields(FieldSet fields) {
    this.fields = fields;
  }

  /**
   * Get PARAM element set reference
   * @return ParamSet
  */
  public ParamSet getParams() {
    if (params == null)
      params = new ParamSet();
    return params;
  }

  /**
   * Set PARAM element set reference
   * @param params
   */
  public void setParams(ParamSet params) {
    this.params = params;
  }

  /**
   * Get GROUP element set reference
   * @return GroupSet
  */
  public GroupSet getGroups() {
    if (groups == null)
      groups = new GroupSet();
    return groups;
  }

  /**
   * Set GROUP element set reference
   * @param groups
   */
  public void setGroups(GroupSet groups) {
    this.groups = groups;
  }

  /**
   * Get LINK element set reference
   * @return LinkSet
  */
  public LinkSet getLinks() {
    if (links == null)
      links = new LinkSet();
    return links;
  }

  /**
    * Set LINK element set reference
    * @param links
   */
  public void setLinks(LinkSet links) {
    this.links = links;
  }

  /**
   * Set DATA element
   * @param data
  */
  public void setData(SavotData data) {
    this.data = data;
  }

  /**
   * Get DATA element
   * @return SavotData
  */
  public SavotData getData() {
    return data;
  }
}
