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
* <p>Resource element </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
*/
public class SavotResource extends MarkupComment implements SimpleTypes {

  // name attribute
  protected char[] name = null;

  // id attribute
  protected char[] id = null;

  // type attribute (results, meta)
  protected char[] type = "results".toCharArray(); // default

  // utype attribute
  protected char[] utype = null;

  // DESCRIPTION element
  protected char[] description = null;

  // COOSYS element set
  protected CoosysSet coosys = null;

  // PARAM element set
  protected ParamSet params = null;

  // INFO element set
  protected InfoSet infos = null;

  // LINK element set
  protected LinkSet links = null;

  // TABLE element set
  protected TableSet tables = null;

  // RESOURCE element set (recursive usage)
  protected ResourceSet resources = null;

  // Other element set
  protected OtherSet other = null;

  /**
   * Constructor
  */
  public SavotResource() {
  }

  /**
   * Set the description
   * @param description String
   */
  public void setDescription(String description) {
    if (description != null)
      this.description = description.toCharArray();
  }

  /**
   * Get the description
   * @return a String
   */
  public String getDescription() {
    if (description != null)
      return String.valueOf(description);
    else return "";
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
   * Set the Link elements
   * @param links
  */
  public void setLinks(LinkSet links) {
    this.links = links;
  }

  /**
   * Get the Link elements
   * @return a LinkSet object
   */
  public LinkSet getLinks() {
    if (links == null)
      links = new LinkSet();
    return links;
  }

  /**
   * Set the Table elements
   * @param tables
   */
  public void setTables(TableSet tables) {
    this.tables = tables;
  }

  /**
   * Get the Table elements
   * @return a TableSet object
   */
  public TableSet getTables() {
    if (tables == null)
      tables = new TableSet();
    return tables;
  }

  /**
   * Set the Resource elements
   * @param resources
  */
  public void setResources(ResourceSet resources) {
    this.resources = resources;
  }

  /**
   * Get the Resource elements
   * @return a ResourceSet object
   */
  public ResourceSet getResources() {
    if (resources == null)
      resources = new ResourceSet();
    return resources;
  }

  /**
   * Set the name attribute
   * @param name String
   */
  public void setName(String name) {
    this.name = name.toCharArray();
  }

  /**
   * Get the name attribute
   * @return a String
   */
  public String getName() {
    if (name != null)
      return String.valueOf(name);
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
   * Set the type attribute
   * @param type String (results, meta)
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
   * Set the utype attribute
   * @param utype String
   */
  public void setUtype(String utype) {
    if (utype != null)
      this.utype = utype.toCharArray();
  }

  /**
   * Get the utype attribute
   * @return a String
   */
  public String getUtype() {
    if (utype != null)
      return String.valueOf(utype);
    else return "";
  }

  /**
   * init a SavotResource object
   */
  public void init() {
    name = null;
    id = null;
    type = null;
    description = null;
    infos = null;
    coosys = null;
    params = null;
    links = null;
    tables = null;
    resources = null;
    other = null;
  }

  /**
   * Get the number of TR object for table index tableIndex
   * (shortcut)
   * @param tableIndex
   * @return int
   */
  public int getTRCount(int tableIndex) {
    try {
      return ((TRSet)(((SavotTable)(this.getTables()).getItemAt(tableIndex)).getData()).getTableData().getTRs()).getItemCount();
    } catch (Exception e) {System.err.println("getTRCount : " + e);};
    return 0;
  }

  /**
   * Get a TRSet object for table index tableIndex
   * (shortcut)
   * @param tableIndex
   * @return TRSet
   */
  public TRSet getTRSet(int tableIndex) {
    try {
      return (TRSet)(((SavotTable)(this.getTables()).getItemAt(tableIndex)).getData()).getTableData().getTRs();
    } catch (Exception e) {System.err.println("getTRSet : " + e);};
    return null;
  }

  /**
   * Get a TR object for table index tableIndex and the corresponding row index rowIndex of this table
   * (shortcut)
   * @param tableIndex
   * @param rowIndex
   * @return SavotTR
   */
  public SavotTR getTR(int tableIndex, int rowIndex) {
    try {
      return (SavotTR)((TRSet)(((SavotTable)(this.getTables()).getItemAt(tableIndex)).getData()).getTableData().getTRs()).getItemAt(rowIndex);
    } catch (Exception e) {System.err.println("getTR : " + e);};
    return null;
  }

  /**
   * Return the number of tables contained in the resource
   * this value doesn't contain the tables of included resources
   * (shortcut)
   * @return int
   */
  public int getTableCount() {
    try {
    return this.getTables().getItemCount();
    } catch (Exception e) {System.err.println("getTableCount : " + e);};
    return 0;
  }

  /**
   * Get a FieldSet object for table index tableIndex
   * (shortcut)
   * @param tableIndex
   * @return FieldSet
   */
  public FieldSet getFieldSet(int tableIndex) {
    try {
      return (FieldSet)(((SavotTable)(this.getTables()).getItemAt(tableIndex))).getFields();
    } catch (Exception e) {System.err.println("getFieldSet : " + e);};
    return null;
  }

  /**
   * Get a LinkSet object for table index tableIndex
   * (shortcut)
   * @param tableIndex
   * @return LinkSet
   */
  public LinkSet getLinkSet(int tableIndex) {
    try {
      return (LinkSet)(((SavotTable)(this.getTables()).getItemAt(tableIndex))).getLinks();
    } catch (Exception e) {System.err.println("getLinkSet : " + e);};
    return null;
  }

  /**
   * Get a Description object (String) for table index tableIndex
   * (shortcut)
   * @param tableIndex
   * @return String
   */
  public String getDescription(int tableIndex) {
    try {
      return (String)(((SavotTable)(this.getTables()).getItemAt(tableIndex))).getDescription();
    } catch (Exception e) {System.err.println("getDescription : " + e);};
    return null;
  }

  /**
   * Get a SavotData object for table index tableIndex
   * (shortcut)
   * @param tableIndex
   * @return SavotData
   */
  public SavotData getData(int tableIndex) {
    try {
      return (SavotData)(((SavotTable)(this.getTables()).getItemAt(tableIndex))).getData();
    } catch (Exception e) {System.err.println("getData : " + e);};
    return null;
  }
}
