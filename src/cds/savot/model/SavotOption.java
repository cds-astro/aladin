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
* <p>Option element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotOption extends MarkupComment implements SimpleTypes {

  // name attribute
  protected char[] name = null;

  // value attribute
  protected char[] value = null;

  // OPTION elements
  protected OptionSet options = null;

  /**
   * Constructor
  */
  public SavotOption() {
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
   * Get OPTION elements reference
   * @return OptionSet
  */
  public OptionSet getOptions() {
    if (options == null )
      options = new OptionSet();
    return options;
  }

  /**
   * Set OPTION elements reference
   * @param options
  */
  public void setOptions(OptionSet options) {
    this.options = options;
  }
}
