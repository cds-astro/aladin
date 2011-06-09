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
* <p>VOTable element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
* (kickoff 31 May 02)
*/
public class SavotVOTable extends MarkupComment implements SimpleTypes {

  // xmlns attribute
  protected char[] xmlns = null;

  // xmlns:xsi attribute
  protected char[] xmlnsxsi = null;

  // xsi:NoNamespaceSchemaLocation attribute
  protected char[] xsinoschema = null;

  // xsi:schemaLocation attribute
  protected char[] xsischema = null;

  // id attribute
  protected char[] id = null;

  // version attribute (default : 1.1)
  protected char[] version = "1.1".toCharArray();

  // DESCRIPTION element
  protected char[] description = null;

  // COOSYS element set
  protected CoosysSet coosys = null;

  // PARAM element set
  protected ParamSet params = null;

  // INFO element set
  protected InfoSet infos = null;

  // DEFINITIONS element
  protected SavotDefinitions definitions = null;

  // RESOURCE element
  protected ResourceSet resources = null;

  /**
   * Constructor
  */
  public SavotVOTable() {
  }

  /**
   * Set the global attributes (<VOTABLE .. global attributes .. version="1.1">) (used for the writer)
   * @param xmlns String
  */
  public void setXmlns(String xmlns) {
    if (xmlns != null)
      this.xmlns = xmlns.toCharArray();
  }

  /**
   * Get the global attributes (<VOTABLE .. global attributes .. version="1.1"> (used for the writer)
   * @return String
  */
  public String getXmlns() {
    if (xmlns != null)
      return String.valueOf(xmlns);
    else return "";
  }

   /**
    * Set the global attributes (<VOTABLE .. global attributes .. version="1.1">) (used for the writer)
    * @param xmlnsxsi String
   */
   public void setXmlnsxsi(String xmlnsxsi) {
     if (xmlnsxsi != null)
       this.xmlnsxsi = xmlnsxsi.toCharArray();
   }

   /**
    * Get the global attributes (<VOTABLE .. global attributes .. version="1.1"> (used for the writer)
    * @return String
   */
   public String getXmlnsxsi() {
     if (xmlnsxsi != null)
       return String.valueOf(xmlnsxsi);
     else return "";
   }

   /**
    * Set the global attributes (<VOTABLE .. global attributes .. version="1.1">) (used for the writer)
    * @param xsinoschema String
   */
   public void setXsinoschema(String xsinoschema) {
     if (xsinoschema != null)
       this.xsinoschema = xsinoschema.toCharArray();
   }

   /**
    * Get the global attributes (<VOTABLE .. global attributes .. version="1.1"> (used for the writer)
    * @return String
   */
   public String getXsinoschema() {
     if (xsinoschema != null)
       return String.valueOf(xsinoschema);
     else return "";
   }

    /**
     * Set the global attributes (<VOTABLE .. global attributes .. version="1.1">) (used for the writer)
     * @param xsischema String
    */
    public void setXsischema(String xsischema) {
      if (xsischema != null)
        this.id = xsischema.toCharArray();
    }

    /**
     * Get the global attributes (<VOTABLE .. global attributes .. version="1.1"> (used for the writer)
     * @return String
    */
    public String getXsischema() {
      if (xsischema != null)
        return String.valueOf(xsischema);
      else return "";
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
   * @return String
  */
  public String getId() {
    if (id != null)
      return String.valueOf(id);
    else return "";
  }

  /**
   * Set the version attribute
   * @param version String
  */
  public void setVersion(String version) {
    if (version != null)
      this.version = version.toCharArray();
  }

  /**
   * Get the version attribute
   * @return String
  */
  public String getVersion() {
    if (version != null)
      return String.valueOf(version);
    else return "";
  }

  /**
   * Set DESCRIPTION element
   * @param description
   */
  public void setDescription(String description) {
    if (description != null)
      this.description = description.toCharArray();
  }

  /**
   * Get DESCRIPTION element
   * @return a String
   */
  public String getDescription() {
    if (description != null)
      return String.valueOf(description);
    else return "";
  }

  /**
   * Set DEFINITIONS element
   * @param definitions
   */
  public void setDefinitions(SavotDefinitions definitions) {
    this.definitions = definitions;
  }

  /**
   * Get DEFINITIONS element
   * @return SavotDefinitions
   */
  public SavotDefinitions getDefinitions() {
    return definitions;
  }

  /**
   * Set the Coosys elements
   * @param coosys
   */
  public void setCoosys(CoosysSet coosys) {
    this.coosys = coosys;
  }

  /**
   * Get the Coosys elements
   * @return a CoosysSet object
   */
  public CoosysSet getCoosys() {
    if (coosys == null)
      coosys = new CoosysSet();
    return coosys;
  }

  /**
   * Set the Infos elements
   * @param infos
  */
  public void setInfos(InfoSet infos) {
    this.infos = infos;
  }

  /**
   * Get the Infos elements
   * @return a CoosysSet object
   */
  public InfoSet getInfos() {
    if (infos == null)
      infos = new InfoSet();
    return infos;
  }

  /**
   * Set the Param elements
   * @param params
  */
  public void setParams(ParamSet params) {
    this.params = params;
  }

  /**
   * Get the Param elements
   * @return a ParamSet object
   */
  public ParamSet getParams() {
    if (params == null)
      params = new ParamSet();
    return params;
  }

  /**
   * Get RESOURCE set reference (FULL mode only)
   * @return ResourceSet (always NULL in SEQUENTIAL mode)
   */
  public ResourceSet getResources() {
    if (resources == null)
      resources = new ResourceSet();
    return resources;
  }

  /**
   * Set RESOURCE set reference
   * @param resources
   */
  public void setResources(ResourceSet resources) {
      this.resources = resources;
  }
}
