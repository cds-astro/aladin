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

package cds.tools;

import java.awt.Color;
import java.awt.Component;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

/**
 * A JTable with alternate colors between even and odd rows
 * @author Thomas Boch [CDS]
 *
 *
 *
 */
public class TwoColorJTable extends JTable {
    //////// Following code deals with having a JTable with alternate colors ///////////////////////////////
    //////// Copied from http://elliotth.blogspot.com/2004/09/alternating-row-colors-in-jtable.html ////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////

    static public Color DEFAULT_ALTERNATE_COLOR = new Color(241, 241, 254);

    private Color alternateColor = DEFAULT_ALTERNATE_COLOR;

    public TwoColorJTable(TableModel tableModel, Color alternateColor) {
        super(tableModel);

        this.alternateColor = alternateColor;
    }
    
    public TwoColorJTable(Vector<Vector<String>> tabledata, Vector<String> columnNames) {
        super(tabledata, columnNames);
    }

    public TwoColorJTable() {
        super();
    }

    public TwoColorJTable(TableModel tableModel) {
        super(tableModel);
    }

 // I think we don't need to paint empty rows
 // I prefer to keep the empty parts untouched


//    /**
//     * Paints empty rows too, after letting the UI delegate do
//     * its painting.
//     */
//    public void paint(Graphics g) {
//        super.paint(g);
//        paintEmptyRows(g);
//    }


//    /**
//     * Paints the backgrounds of the implied empty rows when the
//     * table model is insufficient to fill all the visible area
//     * available to us. We don't involve cell renderers, because
//     * we have no data.
//     */
//    protected void paintEmptyRows(Graphics g) {
//        final int rowCount = getRowCount();
//        final Rectangle clip = g.getClipBounds();
//        if (rowCount * rowHeight < clip.height) {
//            for (int i = rowCount; i <= clip.height/rowHeight; ++i) {
//                g.setColor(getBackground());
//                g.fillRect(clip.x, i * rowHeight, clip.width, rowHeight);
//            }
//        }
//    }

    /**
     * Changes the behavior of a table in a JScrollPane to be more like
     * the behavior of JList, which expands to fill the available space.
     * JTable normally restricts its size to just what's needed by its
     * model.
     */
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            JViewport parent = (JViewport) getParent();
            return (parent.getHeight() > getPreferredSize().height);
        }
        return false;
    }

    /**
     * Returns the appropriate background color for the given row.
     */
    protected Color colorForRow(int row) {
        return (row % 2 == 0) ? getBackground() : alternateColor;
    }

    /**
     * Shades alternate rows in different colors.
     */
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (isCellSelected(row, column) == false) {
            c.setBackground(colorForRow(row));
            c.setForeground(UIManager.getColor("Table.foreground"));
        } else {
            c.setBackground(UIManager.getColor("Table.selectionBackground"));
            c.setForeground(UIManager.getColor("Table.selectionForeground"));
        }
        return c;
    }
}
