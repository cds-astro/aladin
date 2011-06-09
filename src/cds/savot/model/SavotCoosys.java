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
* <p>Coosys element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotCoosys extends MarkupComment implements SimpleTypes {

  // ID attribute
  protected char[] id = null;

  // equinox attribute
  protected char[] equinox = null;

  // epoch attribute
  protected char[] epoch = null;

  // system attribute eq_FK4, eq_FK5, ICRS, ecl_FK4, ecl_FK5, galactic, supergalactic, xy, barycentric, geo_app
  protected char[] system = "eq_FK5".toCharArray(); // default

  // element content
  char[] content = null;

  /**
   * Constructor
  */
  public SavotCoosys() {
  }

  /**
   * Set the id attribute
   * @param id
   */
  public void setId(String id) {
    if (id != null)
      this.id = id.toCharArray();
  }

  /**
   * Get the id attribute value
   * @return String
   */
  public String getId() {
    if (id != null)
      return String.valueOf(id);
    else return "";
  }

  /**
   * Set the equinox attribute
   * @param equinox ([JB]?[0-9]+([.][0-9]*)?)
   */
  public void setEquinox(String equinox) {
    if (equinox != null)
      this.equinox = equinox.toCharArray();
  }

  /**
   * Get the equinox attribute value
   * @return String
   */
  public String getEquinox() {
    if (equinox != null)
      return String.valueOf(equinox);
    else return "";
  }

  /**
   * Set the epoch attribute
   * @param epoch ([JB]?[0-9]+([.][0-9]*)?)
   */
  public void setEpoch(String epoch) {
    if (epoch != null)
      this.epoch = epoch.toCharArray();
  }

  /**
   * Get the epoch attribute value
   * @return String
   */
  public String getEpoch() {
    if (epoch != null)
      return String.valueOf(epoch);
    else return "";
  }

  /**
   * Set the system attribute
   * @param system (eq_FK4, eq_FK5, ICRS, ecl_FK4, ecl_FK5, galactic, supergalactic, xy, barycentric, geo_app)
   */
  public void setSystem(String system) {
    if (system != null)
      this.system = system.toCharArray();
  }

  /**
   * Get the system attribute value
   * @return String
   */
  public String getSystem() {
    if (system != null)
      return String.valueOf(system);
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
