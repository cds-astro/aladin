// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

package cds.vizier;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import cds.tools.TwoColorJTable;
import cds.tools.Util;



/**
 * VizieR catalog table
 * <P>
 * Two important things : a text field and a button can be added from outside
 * <P>
 * Example of button use : show additional informations for a selected catalog
 * <P>
 * Text field use is fixed : selected catalog names are added to this field with a comma as separator
 * <P>
 * Multiple selection are allowed.
 * @author Thomas Boch [CDS]
 * @version 1.1 : (august 2010) fix the bug appearing when the -kw.Wavelength is not there
 * @version 1.0 : (november 2007) creation. Inspired from cds.vizier.VizieRList
 */
public final class VizieRTable extends TwoColorJTable {
    //// 2 available display modes : survey and search ////

    // in survey mode, we display : name    description    nb of krows
    static final public int SURVEY_MODE = 0;
    // in search mode, we display : name    category    density    description
    static final public int SEARCH_MODE = 1;


    protected JTextField catalog;    // Catalog name field
    protected JButton getReadMe;     // Button used to get (for example) informations about a selected catalog
    protected JButton getMoc;        // Pour récupérer le MOC
    protected Vector vCats;          // vecteur des VizieRCatalog à afficher
    protected Vector v;

    private final int mode;

    static final boolean LSCREEN= Toolkit.getDefaultToolkit().getScreenSize().width>1000;

    /** Taille moyenne des fonts */
    static protected final int  SIZE   = LSCREEN?12:10;

    protected static Font COURIER= new Font("Monospaced",Font.PLAIN, SIZE);

    /** Constructor Table creation
     * Set the selected catalogs
     * @param catalog text field (can be optional)
     * @param getReadMe button (can be optional)
     * @param v vector of rows to put into the table
     */
    public VizieRTable(JTextField catalog, JButton getReadMe, JButton getMoc, Vector v) {
        this(catalog, getReadMe, getMoc, v, 20, SURVEY_MODE);
    }

    /** Constructor Table creation
     *
     * @param catalog text field (can be optional)
     * @param getReadMe button (can be optional)
     * @param v vector of rows to put into the list
     * @param rows number of rows to show at one time
     * @param mode display mode : SURVEY_MODE or SEARCH_MODE
     */
    public VizieRTable(JTextField catalog, JButton getReadMe, JButton getMoc, Vector v, int rows, int mode) {
        super();

        this.mode = mode;
        this.v = v;
        this.vCats = createObjectsFromRows(v);
        this.catalog = catalog;
        this.getReadMe = getReadMe;
        this.getMoc = getMoc;

        // setting grid color
        this.setGridColor(Color.lightGray);
        // setting table model
        this.setModel(new VizieRTableModel());
        // no reordering of columns
        this.getTableHeader().setReorderingAllowed(false);
        // listener allowing to sort rows when clicking on the table header
        this.getTableHeader().addMouseListener(new VizieRTableHeaderListener());
        // listener for checkboxes in SEARCH_MODE
        if( mode==SEARCH_MODE ) {
            this.addMouseListener(new VizieRTableMouseListener());
        }

        // setting columns width
        if( mode==SURVEY_MODE ) {
            this.getColumnModel().getColumn(0).setPreferredWidth(80);
            this.getColumnModel().getColumn(1).setPreferredWidth(300);
            this.getColumnModel().getColumn(2).setPreferredWidth(100);
        }
        else {
            this.getColumnModel().getColumn(0).setPreferredWidth(20);
            this.getColumnModel().getColumn(1).setPreferredWidth(90);
            this.getColumnModel().getColumn(2).setPreferredWidth(70);
            this.getColumnModel().getColumn(3).setPreferredWidth(60);
            this.getColumnModel().getColumn(4).setPreferredWidth(330);
        }

        setVisibleRowCount(rows);

        getTableHeader().setDefaultRenderer(new VizieRTableHeaderRenderer(getTableHeader().getDefaultRenderer()));

        this.setFont( COURIER );
        this.getSelectionModel().addListSelectionListener(new VizieRTableSelectionListener());
    }

    /** Constructor table creation
     *
     * @param v vector of lines to put into the list
     */
    public VizieRTable(Vector v) {
        this(null, null, null, v, 20, SURVEY_MODE);
    }

    private void setVisibleRowCount(int nbRows) {
        int height = this.getRowHeight()*nbRows;
        setPreferredScrollableViewportSize(new Dimension(560, height));
    }

    /** Creation d'objets VizieRCatalog à partir d'une ligne de donnée
     * telle que produite par VizieRQuery
     * @param v
     * @return
     */
    private Vector createObjectsFromRows(Vector v) {
        Vector result = new Vector();
        Enumeration e = v.elements();

        String s;
        String[] parts;
        String name, desc, category;
        int nbKRow, density;
        int nbPartsExpected = mode==SURVEY_MODE?3:4;
        while( e.hasMoreElements() ) {
            s = (String)e.nextElement();

            name = null;
            desc = null;
            nbKRow = -1;
            category = null;
            density = -1;

            // split the string in 3 parts : name, description and nb of rows
            parts = s.split("\t", -1);
            if( parts.length<nbPartsExpected ) {
                continue;
            }

            if( mode==SURVEY_MODE) {
                name = parts[0];
                desc = parts[1];
                try {
                    nbKRow = Integer.parseInt(parts[2]);
                }
                catch(NumberFormatException nfe) {
                    nbKRow = 0;
                }
            }
            else {
                name = parts[0];
                category = parts[1];
                try {
                    density = Integer.parseInt(parts[2]);
                }
                catch(NumberFormatException nfe) {
                    density = 0;
                }
                desc = parts[3];
            }

            result.add(new VizieRCatalog(name, desc, category, density, nbKRow));
        }

        return result;
    }

    /** Programatically select a catalogue row, given its name
     *  Do nothing if the catalogue is not found
     *
     * @param catName name of the catalogue
     */
    public void selectCatalog(String catName) {
        Enumeration e = vCats.elements();
        int k = 0;
        while( e.hasMoreElements() ) {
            if( ((VizieRCatalog)e.nextElement()).getName().equals(catName)) {
                setRowSelectionInterval(k, k);
            }
            k++;
        }
    }

    /** Get catalog field contain
     *
     * @return catalog
     */
    public JTextField getCatalogField() {
        return catalog;
    }

    /** Get Catalog Text Field
     *
     * @return the TextField component corresponding to the catalog
     */
    public JTextField getCatalogTextField() {
        return catalog;
    }

    public Vector getList() {
        return v;
    }

    public void resetList() {
        if( mode==SEARCH_MODE ) {
            colIdx = -1;
            this.getTableHeader().repaint();
        }
        this.clearSelection();
        selectedCats = null;
        ((AbstractTableModel)this.getModel()).fireTableRowsUpdated(0, vCats.size()-1);
    }

    protected void preSelection(Vector v) {
        this.v = v;
        this.vCats = createObjectsFromRows(v);
        selectedCats = null;
        ((AbstractTableModel)this.getModel()).fireTableDataChanged();
    }

    /** Set ReadMe button
     *
     * @param getReadMe
     */
    public void setReadMeButton(JButton getReadMe) {
        this.getReadMe = getReadMe;
    }

    /** Set Catalog Text Field
     *
     * @param catalog text field component
     */
    public void setCatalogTextField(JTextField catalog) {
        this.catalog = catalog;
    }

    /** Get ReadMe button component
     *
     * @return the TextField component corresponding to the catalog
     */
    public JButton getReadMeButton() {
        return getReadMe;
    }

    /** to react to selection changes */
    class VizieRTableSelectionListener implements ListSelectionListener {

        public void valueChanged(ListSelectionEvent e) {
            String s="";
            int i;
            // viziercat setting is depending on selected catalog
            int[] idx = getSelectedRows();
            VizieRCatalog[] cats = new VizieRCatalog[idx.length];
            for( i=0; i<idx.length; i++ ) cats[i] = (VizieRCatalog)vCats.get(idx[i]);

            for( i=0; i<cats.length; i++ ) {

                // Get the first word to retrieve the catalog number
                String id = cats[i].getName();

                // Catalog id is merge in the catalog field
                // if shift is set, it is added to the previous
                if( ! s.equals("") ) s = s + "," + id;
                else s = id;
            }
            if (catalog != null) {
                catalog.setText(s);
            }

            // Readme button avtivation or not
            if (getReadMe != null)  getReadMe.setEnabled(i == 1);
            if (getMoc != null)  getMoc.setEnabled(i == 1);
        }
    }

    private void sort(Vector data, boolean ascending, Comparator comp) {
        List l = Arrays.asList(data.toArray());
        Collections.sort(l, comp);
        if( !ascending ) Collections.reverse(l);

        vCats = new Vector(l);

        // update the table view
        ((AbstractTableModel)this.getModel()).fireTableRowsUpdated(0, vCats.size()-1);
        // updathe the header
        this.getTableHeader().repaint();
    }

    /**
     * Inner class implementing a table model for the JTable displaying VizieR Tables
     *
     */
    class VizieRTableModel extends AbstractTableModel {

        @Override
        public String getColumnName(int col) {
            if( mode==SURVEY_MODE ) {
                switch(col) {
                    case 0 : return "Name";

                    case 1 : return "Description";

                    case 2 : return "Nb of KRows";

                    default : return "";
                }
            }
            else {
                switch(col) {
                case 0 : return "";

                case 1 : return "Name";

                case 2 : return "Category";

                case 3 : return "Density";

                case 4 : return "Description";

                default : return "";
            }

            }
        }
        public int getRowCount() {
            return vCats==null?0:vCats.size();
        }
        public int getColumnCount() { return mode==SURVEY_MODE?3:5; }

        @Override
        public Class getColumnClass(int column) {
            if( mode==SEARCH_MODE && column==0 ) return Boolean.class;

            return String.class;
        }

        public Object getValueAt(int row, int col) {
            if( mode==SURVEY_MODE ) {
                switch(col) {
                    case 0 : return ((VizieRCatalog)vCats.get(row)).getName();

                    case 1 : return ((VizieRCatalog)vCats.get(row)).getDesc();

                    case 2 : return new Integer(((VizieRCatalog)vCats.get(row)).getNbKRow());

                    default : return "";
                }
            }
            else {
                switch(col) {
                    case 0 : return isSelected(row)?Boolean.TRUE:Boolean.FALSE;

                    case 1 : return ((VizieRCatalog)vCats.get(row)).getName();

                    case 2 : return ((VizieRCatalog)vCats.get(row)).getCategory();

                    case 3 : return new Integer(((VizieRCatalog)vCats.get(row)).getDensity());

                    case 4 : return ((VizieRCatalog)vCats.get(row)).getDesc();

                    default : return "";
                }
            }
        }
        @Override
        public boolean isCellEditable(int row, int col) {
            if( mode==SEARCH_MODE && col==0 ) return true;

            return false;
        }




    } // end of inner class VizieRTableModel

    private boolean isSelected(int row) {
        int[] selected = getSelectedRows();
        for( int i=0; i<selected.length; i++ ) {
            if( row==selected[i] ) return true;
        }
        return false;
    }

    private int colIdx = -1; // index of the clicked column
    boolean ascending = true; // sort order (ascending if true)
    /** inner class allowing to listen to mouse events on the JTable header
     */
    class VizieRTableHeaderListener extends MouseAdapter {


        @Override
        public void mouseClicked(MouseEvent e) {
            TableColumnModel columnModel = getColumnModel();
            int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            final int column = convertColumnIndexToModel(viewColumn);
            colIdx = column;

            // in this mode, column==0 means we clicked on the checkbox column
            if( mode==SEARCH_MODE && column==0 ) return;

            if( e.getClickCount() == 1 && column != -1 ) {

                Comparator surveyComp = new Comparator() {
                    public final int compare (Object a, Object b) {
                        Object val1, val2;
                        val1 = val2 = null;
                        if( mode==SURVEY_MODE) {
                        switch(column) {
                                case 0 : val1 = ((VizieRCatalog)a).getName();
                                val2 = ((VizieRCatalog)b).getName();
                                break;

                                case 1 : val1 = ((VizieRCatalog)a).getDesc();
                                val2 = ((VizieRCatalog)b).getDesc();
                                break;

                                case 2 : val1 = new Integer(((VizieRCatalog)a).getNbKRow());
                                val2 = new Integer(((VizieRCatalog)b).getNbKRow());
                                break;
                            }
                        }
                        else {


                        switch(column) {
                            case 1 : val1 = ((VizieRCatalog)a).getName();
                            val2 = ((VizieRCatalog)b).getName();
                            break;

                            case 2 : val1 = ((VizieRCatalog)a).getCategory();
                            val2 = ((VizieRCatalog)b).getCategory();
                            break;

                            case 3 : val1 = new Integer(((VizieRCatalog)a).getDensity());
                            val2 = new Integer(((VizieRCatalog)b).getDensity());
                            break;

                            case 4 : val1 = ((VizieRCatalog)a).getDesc();
                            val2 = ((VizieRCatalog)b).getDesc();
                            break;
                            }
                        }
                        return ((Comparable)val1).compareTo(val2);
                    }
                };

                final Vector selected = getSelectedCats();

                sort(vCats, ascending, surveyComp);

                ascending = !ascending;

                // update selection so to keep initial selection after sorting is done
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        clearSelection();
                        for( int i=0; i<vCats.size(); i++ ) {
                            VizieRCatalog cat = (VizieRCatalog)vCats.get(i);
                            if( selected.contains(cat) ) {
                                addRowSelectionInterval(i, i);
                            }
                        }
                    }
                });
            }
        }



    } // end of inner class VizieRTableHeaderListener

    /** Returns the vector of selected catalogues */
    private Vector getSelectedCats() {
        int[] idx = getSelectedRows();
        Vector v = new Vector();
        for( int i=0; i<idx.length; i++ ) {
            v.add(vCats.get(idx[i]));
        }
        return v;
    }

    class VizieRTableHeaderRenderer extends DefaultTableCellRenderer {

        TableCellRenderer renderer;

        /** Constructor
         */
        public VizieRTableHeaderRenderer(TableCellRenderer defaultRenderer) {
                    renderer = defaultRenderer;
        }

        /**
         * Overwrites DefaultTableCellRenderer.
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object
                value, boolean isSelected,
                boolean hasFocus, int row,
                int column) {

            Component comp = renderer.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // if column col has been clicked, add a small arrow to the column header
            if( comp instanceof JLabel ) {
                if( column==colIdx ) {
                    ImageIcon icon = ascending?Util.getAscSortIcon():Util.getDescSortIcon();
                    ((JLabel)comp).setIcon(icon);
                    ((JLabel)comp).setHorizontalTextPosition(SwingConstants.LEADING);
                }
                else ((JLabel)comp).setIcon(null);
            }

            return comp;
        }


    }// end of inner class VizieRTableHeaderRenderer


    private Vector selectedCats;
    // listener allowing to select multiple rows with the checkboxes
    class VizieRTableMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent me) {
            super.mousePressed(me);

            int row = rowAtPoint(me.getPoint());
            int col = columnAtPoint(me.getPoint());

            if( col==0 ) {
                VizieRCatalog clickedCat = ((VizieRCatalog)vCats.get(row));
                VizieRCatalog vCat;
                if( selectedCats!=null ) {
                    for( int i=0; i<vCats.size(); i++ ) {
                        vCat = (VizieRCatalog)vCats.get(i);
                        if( selectedCats.contains(vCat) ) {
                            if( vCat==clickedCat ) removeRowSelectionInterval(i, i);
                            else addRowSelectionInterval(i, i);
                        }
                    }
                }
            }
            selectedCats = getSelectedCats();
        }

    }


}
