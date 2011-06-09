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
* <p>Values element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotValues extends MarkupComment implements SimpleTypes {

  // ID attribute
  char[] id = null;

  // type attribute
  char[] type = "legal".toCharArray();

  // null content
  char[] nul = null;

  // ref content
  char[] ref = null;

  // invalid content - deprecated since VOTable 1.1
  char[] invalid = null;

  // MIN element
  SavotMin min = null;

  // MAX element
  SavotMax max = null;

  // OPTION element
  OptionSet options = null;

  /**
   * Constructor
  */
  public SavotValues() {
  }

  /**
   * Set the id attribute
   * @param id String
   */
  public void setId(String id) {
    if (id != null)
      this.id = id.toCharArray();
  }

  /**
   * Get the id attribute
   * @return a String
   */
  public String getId() {
    if (id != null)
      return String.valueOf(id);
    else return "";
  }

  /**
   * Set the type attribute
   * @param type String (legal, actual)
   */
  public void setType(String type) {
    if (type != null)
      this.type = type.toCharArray();
  }

  /**
   * Get the type attribute
   * @return a String
   */
  public String getType() {
    if (type != null)
      return String.valueOf(type);
    else return "";
  }

  /**
   * Set the null attribute
   * @param nul String
   */
  public void setNull(String nul) {
    if (nul != null)
      this.nul = nul.toCharArray();
  }

  /**
   * Get the null attribute
   * @return a String
   */
  public String getNull() {
    if (nul != null)
      return String.valueOf(nul);
    else return "";
  }

  /**
   * Set the ref attribute
   * @param ref ref
   */
  public void setRef(String ref) {
    if (ref != null)
      this.ref = ref.toCharArray();
  }

  /**
   * Get the ref attribute
   * @return a String
   */
  public String getRef() {
    if (ref != null)
      return String.valueOf(ref);
    else return "";
  }

  /**
   * Set the invalid attribute
   * deprecated since VOTable 1.1
   * @param invalid String
   */
  public void setInvalid(String invalid) {
    if (invalid != null)
      this.invalid = invalid.toCharArray();
  }

  /**
   * Get the invalid attribute
   * deprecated since VOTable 1.1
   * @return a String
   */
  public String getInvalid() {
    if (invalid != null)
      return String.valueOf(invalid);
    else return "";
  }

  /**
   * Set MIN element
   * @param min
  */
  public void setMin(SavotMin min) {
    this.min = min;
  }

  /**
   * Get MIN element
   * @return a SavotMin object
  */
  public SavotMin getMin() {
    return min;
  }

  /**
   * Set MAX element
   * @param max
  */
  public void setMax(SavotMax max) {
    this.max = max;
  }

  /**
   * Get MAX element
   * @return a SavotMax object
  */
  public SavotMax getMax() {
    return max;
  }

  /**
   * Get OPTION element set reference
   * @return OptionSet object
  */
  public OptionSet getOptions() {
    if (options == null)
      options = new OptionSet();
    return options;
  }

  /**
    * Set OPTION element set reference
    * @param options
  */
  public void setOptions(OptionSet options) {
    this.options = options;
  }
}
