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
* <p>Field element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotField extends MarkupComment implements SimpleTypes {

  // ID attribute
  protected char[] id = null;

  // unit attribute
  protected char[] unit = null;

  // datatype attribute
  protected char[] datatype = null;

  // precision attribute
  protected char[] precision = null;

  // width attribute
  protected char[] width = null;

  // ref attribute
  protected char[] ref = null;

  // name attribute
  protected char[] name = null;

  // ucd attribute
  protected char[] ucd = null;

  // utype attribute
  protected char[] utype = null;

  // arraysize attribute
  protected char[] arraysize = null;

  // type attribute - removed in VOTable 1.1
  protected char[] type = null;

  // DESCRIPTION element
  protected char[] description = null;

  // VALUES element
  protected SavotValues values = null;

  // LINK elements
  protected LinkSet links = null;

  // internal variable, used to resolve the GROUP element adding, gives the index of a FIELD in a TABLE
  protected long index = 0;

  /**
   * Constructor
  */
  public SavotField() {
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
   * Get unit attribute
   * @return String
   */
  public String getUnit() {
    if (unit != null)
      return String.valueOf(unit);
    else return "";
  }

  /**
   * Set unit attribute
   * @param unit
   */
  public void setUnit(String unit) {
   if (unit != null)
     this.unit = unit.toCharArray();
  }

  /**
   * Get datatype attribute
   * @return String
   */
  public String getDataType() {
    if (datatype != null)
      return String.valueOf(datatype);
    else return "";
  }

  /**
   * Set datatype attribute
   * @param datatype (boolean, bit, unsignedByte, short, int, long, char, unicodeChar, float, double, floatComplex, doubleComplex)
   */
  public void setDataType(String datatype) {
    if (datatype != null)
      this.datatype = datatype.toCharArray();
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
   * Set precision attribute
   * @param precision ([EF]?[1-0][0-9]*)
   */
  public void setPrecision(String precision) {
    if (precision != null)
      this.precision = precision.toCharArray();
  }

  /**
   * Get precision attribute
   * @return String
   */
  public String getPrecision() {
    if (precision != null)
      return String.valueOf(precision);
    else return "";
  }

  /**
   * Set width attribute
   * @param width
   */
  public void setWidth(String width) {
    if (width != null)
      this.width = width.toCharArray();
  }

  /**
   * Set width attribute
   * @param width
   */
  public void setWidthValue(int width) {
    if (width >= 0)
      this.width = Integer.toString(width).toCharArray();
  }

  /**
   * Get width attribute
   * @return String
   */
  public String getWidth() {
    if (width != null)
      return String.valueOf(width);
    else return "";
  }

  /**
   * Get width attribute
   * @return String
   */
  public int getWidthValue() {
    if (width != null)
      return (Integer.valueOf(String.valueOf(width))).intValue();
    else return 0;
  }

  /**
   * Set arraysize attribute
   * @param arraysize
   */
  public void setArraySize(String arraysize) {
    if (arraysize != null)
      this.arraysize = arraysize.toCharArray();
  }

  /**
   * Get arraysize attribute
   * @return String
   */
  public String getArraySize() {
    if (arraysize != null)
      return String.valueOf(arraysize);
    else return "";
  }

  /**
   * Set type attribute
   * @param type (hidden, no_query, trigger, location)
   * warning : deprecated in VOTable 1.1
   */
  public void setType(String type) {
    if (type != null)
      this.type = type.toCharArray();
  }

  /**
   * Get type attribute
   * @return String
   * warning : deprecated in VOTable 1.1
   */
  public String getType() {
    if (type != null)
      return String.valueOf(type);
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
   * Set DESCRIPTION content
   * @param description
   */
  public void setDescription(String description) {
    if (description != null)
      this.description = description.toCharArray();
  }

  /**
   * Get DESCRIPTION content
   * @return String
   */
  public String getDescription() {
    if (description != null)
      return String.valueOf(description);
    else return "";
  }

  /**
   * Set the VALUES element
   * @param values
   */
  public void setValues(SavotValues values) {
   this.values = values;
  }

  /**
   * Get the VALUES element
   * @return SavotValues
   */
  public SavotValues getValues() {
    return values;
  }

  /**
  * Get LINK elements set reference
  * @return LinkSet
  */
  public LinkSet getLinks() {
    if (links == null)
      links = new LinkSet();
    return links;
  }

  /**
   * Set LINK elements set reference
   * @param links
  */
  public void setLinks(LinkSet links) {
    this.links = links;
  }
}
