package cds.table;

//Copyright 2002-2014 - UDS/CNRS
//The SAVOT library is distributed under the terms
//of the GNU General Public License version 3.
//
//This file is part of SAVOT.
//
//SAVOT is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, version 3 of the License.
//
//SAVOT is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//The GNU General Public License is available in COPYING file
//along with SAVOT.
//
//SAVOT - Simple Access to VOTable - Parser
//
//Author, Co-Author:  Andre Schaaff (CDS), Laurent Bourges (JMMC)
import java.util.ArrayList;

public final class TableModel {

    // resource properties
    //  private String resource_description; // Astrores
    // private String resource_name; // Astrores
    // private String resource_title;// Astrores
    // table properties
    private String id;

    private String description;
    private String name;
//    private String title;
//    private String info;

    private String[] properties;

    private ArrayList propertyNames = new ArrayList(); // in each element a table of property

    private ArrayList propertyValues = new ArrayList(); // in each element a table of property

    private final ArrayList rows = new ArrayList(); // in each row a table of values ( char [] )

    // data about resource
    /**
     *
     */
    public TableModel() {
    }

    /**
     * Returns the Table properties (description, resource)
     * 
     * @return String[]
     */
    public String[] getTableProperties() {
        return properties;
    }

    /**
     * Sets the Table properties (description, resource)
     */
    public void setTableProperties(String[] properties) {
        this.properties = properties;
    }

    /**
     * 
     * @param fieldPropertyNames
     *            Vector
     * @param fieldPropertyValues
     *            Vector
     */
    @SuppressWarnings("unchecked")
    public void setTableProperties(ArrayList fieldPropertyNames, ArrayList fieldPropertyValues) {
        this.propertyNames = (ArrayList) fieldPropertyNames.clone();
        this.propertyValues = (ArrayList) fieldPropertyValues.clone();
    }

    /**
     * Returns a property value
     * 
     * @param tablePropName
     *            String
     * @return String
     */
    public String getTableProperty(String tablePropName) {
        return null;
    }

    /**
     * Sets a property value
     * 
     * @param tablePropName
     *            String
     * @param value
     *            String
     */
    public void setTableProperty(String tablePropName, String value) {
    }

    /**
     * Returns a row value of the table
     * 
     * @param index
     *            int
     * @return String[]
     */
    public String[] getRow(int index) {
        return (String[]) rows.get(index);
    }

    /**
     * Adds a row to the table
     * 
     * @param row
     *            String[]
     */
    public void addRow(String[] row) {
        // System.out.println("une de plus");
        rows.add(row);
    }

    /**
     * Adds a row to the table
     * 
     * @param values
     *            String[]
     */
    public void addRow(ArrayList values) {
        // System.out.println("une de plus");
        rows.add(values.toArray());
    }

    /**
     * Sets (replace) a row value of the table
     * 
     * @param index
     *            int
     * @param values
     *            String[]
     */
    public void setRow(int index, String[] values) {
        rows.set(index, values);
    }

    /**
     * Returns the value of a cell
     * 
     * @param row
     *            int
     * @param col
     *            int
     * @return String
     */
    public String getValueAt(int row, int col) {
        String[] localrow;
        localrow = (String[]) rows.get(row);
        return localrow[col];
    }

    /**
     * Sets the value of a cell
     * 
     * @param row
     *            int
     * @param col
     *            int
     * @param value
     *            String
     */
    @SuppressWarnings("unchecked")
    public void setValueAt(int row, int col, String value) {
        String[] localrow;
        localrow = (String[]) rows.get(row);
        localrow[col] = value;
        rows.set(row, localrow);
    }

    /**
     * Returns the property name list of a column (VOTable FIELD content for
     * example (name, ID, unit, description, UCD, ...)
     * 
     * @param col
     *            int
     * @return String[]
     */
    public String[] getProperties(int col) {
        return null;
    }

    /**
     * Sets the property name list of a column (VOTable FIELD content for
     * example (name, ID, unit, description, UCD, ...)
     * 
     * @param col
     *            int
     * @param values
     *            String[]
     */
    public void getProperties(int col, String[] values) {
    }

    /**
     * Returns the value of a given column
     * 
     * @param col
     *            int
     * @param propName
     *            String
     * @return String
     */
    public String getProperties(int col, String propName) {
        return null;
    }

    /**
     * Returns the value of a given column
     * 
     * @param col
     *            int
     * @param propName
     *            String
     * @param value
     *            String
     */
    public void setProperty(int col, String propName, String value) {
    }

    // Le tout serait accessible via une classe TableParser qui pourrait fournir
    // qq chose comme :
    // Object creation
    // TableParser pt = new TableParser(InputStream in);
    // Lancement de l'analyse
    // Table table[] = pt.parse();
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * 
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * 
     * @param id
     *            String
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * 
     * @return String
     */
    public String getId() {
        return id;
    }

    /**
     * 
     * @param name
     *            String
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return int
     */
    public int getRowCount() {
        return rows.size();
    }

}
