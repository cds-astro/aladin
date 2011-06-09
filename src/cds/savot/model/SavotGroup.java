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
* <p>Group element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotGroup extends MarkupComment implements SimpleTypes {
  // ID attribute
  protected char[] id = null;

  // name attribute
  protected char[] name = null;

  // ref attribute
  protected char[] ref = null;

  // ucd attribute
  protected char[] ucd = null;

  // utype attribute
  protected char[] utype = null;

  // description element
  protected char[] description = null;

  // FIELDRef elements
  protected FieldRefSet fieldsref = null;

  // PARAM elements
  protected ParamSet params = null;

  // PARAMRef elements
  protected ParamRefSet paramsref = null;

  // GROUP elements
  protected GroupSet groups = null;

  /**
   * Constructor
  */
  public SavotGroup() {
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
   * Get id attribute
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
   * Set ref attribute
   * @param ref
   */
  public void setRef(String ref) {
    if (ref != null)
      this.ref = ref.toCharArray();
  }

  /**
   * Get ref attribute
   * @return String
   */
  public String getRef() {
    if (ref != null)
      return String.valueOf(ref);
    else return "";
  }

  /**
   * Set ucd attribute
   * @param ucd ([A-Za-z0-9_.,-]*)
   */
  public void setUcd(String ucd) {
    if (ucd != null)
      this.ucd = ucd.toCharArray();
  }

  /**
   * Get ucd attribute
   * @return String
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
   * @return String
   */
  public String getUtype() {
    if (utype != null)
      return String.valueOf(utype);
    else return "";
  }

  /**
  * Set DESCRIPTION element content
  * @param description
  */
  public void setDescription(String description) {
    if (description != null)
      this.description = description.toCharArray();
  }

  /**
  * Get DESCRIPTION element content
  * @return String
  */
  public String getDescription() {
    if (description != null)
      return String.valueOf(description);
    else return "";
  }

  /**
  * Get PARAM elements set reference
  * @return ParamSet
  */
  public ParamSet getParams() {
    if (params == null)
      params = new ParamSet();
    return params;
  }

  /**
   * Set PARAM elements set reference
   * @param params
  */
  public void setParams(ParamSet params) {
    this.params = params;
  }

  /**
   * Set PARAMref elements set reference
   * @param paramsref
  */
  public void setParamsRef(ParamRefSet paramsref) {
    this.paramsref = paramsref;
  }

  /**
  * Get PARAMref elements set reference
  * @return ParamRefSet
  */
  public ParamRefSet getParamsRef() {
    if (paramsref == null)
      paramsref = new ParamRefSet();
    return paramsref;
  }

  /**
  * Get FIELDref elements set reference
  * @return FieldRefSet
  */
  public FieldRefSet getFieldsRef() {
    if (fieldsref == null)
      fieldsref = new FieldRefSet();
    return fieldsref;
  }

  /**
   * Set FIELDref elements set reference
   * @param fieldsref
  */
  public void setFieldsRef(FieldRefSet fieldsref) {
    this.fieldsref = fieldsref;
  }

  /**
  * Get GROUP elements set reference
  * @return GroupSet
  */
  public GroupSet getGroups() {
    if (groups == null)
      groups = new GroupSet();
    return groups;
  }

  /**
   * Set GROUP elements set reference
   * @param groups
  */
  public void setGroups(GroupSet groups) {
    this.groups = groups;
  }
}
