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

package cds.allsky;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import cds.aladin.Coord;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

public class BuildTable extends JTable {

   static int  DEFAULT_BITPIX = -32;
   private int 	bitpix = DEFAULT_BITPIX; // bitpix par d�faut pour les calculs d'espace disque
   
   static final private int MAXHEALPIXORDER = 15;

   private static final int CHECK_IDX = 0;
   private static final int RES_IDX   = 1;
   private static final int NSIDE_IDX = 2;
   private static final int TILES_IDX = 3;
   private static final int VOL_IDX   = 4;

   static private int [] columnSize = { 60,100,120,120,80 };
   static private String[] columnNames = {
      "",
      "<html>Pixel angular<br>resolution</html>",
      "<html>HEALPix<br>order / NSIDE</html>",
      "<html>Depth / Nb tiles<br><i>(for a full sky)</i></html>",
      "<html>Space<br>required</html>"
   };
   static private String[] columnToolTips = {
      "Your choice",
      "Target pixel angular resolution",
      "HEALPix parameters: order = log2(NSIDE)",
      "<html>Number of levels & Number of tiles (image 512x512)<br>at the end of the process<html>",
      "Disk space used at the end of the process"
   };

   BuildTable() {
      super(createData(DEFAULT_BITPIX),columnNames);
      setAutoscrolls(true);
      for( int i=0; i<columnSize.length; i++ )  getColumnModel().getColumn(i).setPreferredWidth( columnSize[i]);
   }

   /**
    * Cr�e un header avec un tooltip
    */
   protected JTableHeader createDefaultTableHeader() {
      return new JTableHeader(columnModel) {
         public String getToolTipText(MouseEvent e) {
            java.awt.Point p = e.getPoint();
            int index = columnModel.getColumnIndexAtX(p.x);
            int realIndex = columnModel.getColumn(index).getModelIndex();
            return columnToolTips[realIndex];
         }
      };
   }

   public void setBitpix(int newbitpix) {
      bitpix = newbitpix;
      setDiskHeader(newbitpix);
      this.updateData();
   }
   
   private static String getDiskHeader(int bitpix) {
      return  "Max disk space (bitpix="+bitpix+")";
   }
   
   public static void setDiskHeader(int bitpix) {
      columnToolTips[VOL_IDX] = BuildTable.getDiskHeader(bitpix);
   }

   int defaultRow = -1;
   private int defaultOrder = -1;

   public void setDefaultRow(int row) {
      defaultRow = row;
   }

   public int getDefaultRow() {
      return defaultRow;
   }

   public int setSelectedOrder(int order) {
      defaultOrder = order;
      setValueAt(Boolean.TRUE,order-3,CHECK_IDX);
      return order-3;
   }

   public void reset() {
      defaultOrder = -1;
      for (int i = 0; i < this.getColumnCount(); i++) {
         setValueAt(new Boolean(false),i,CHECK_IDX);
      }
   }

   
   static final Border bord = BorderFactory.createEmptyBorder(0,10,0,0);
   /**
    * Fait un rendu diff�rent (color� bleu) pour la ligne conseill�e par d�faut
    * + ajoute un tooltip sur la colonne des nsides
    */
   public Component prepareRenderer(TableCellRenderer renderer,
         int rowIndex, int colIndex) {
      Component c = super.prepareRenderer(renderer, rowIndex, colIndex);
      Color color = getBackground();
      if (rowIndex==defaultRow ) color = new Color(204, 234, 234); // bleut�
      c.setBackground(color);
      ((JComponent)c).setBorder(bord);

      // ajoute un tooltip sur la colonne des nsides
      if (colIndex==NSIDE_IDX && c instanceof JComponent)
         ((JComponent)c).setToolTipText(getOrderToolTip(rowIndex));
      
      return c;
   }

   private static Object[][] createData(int bitpix) {
      Object[][] data;
      // Tableau des r�solutions
      data = new Object[MAXHEALPIXORDER-3][5];

      double surface = 4. * Math.PI * (180. / Math.PI) * (180. / Math.PI);
      long pixelPerFile = (long)Math.pow(4,BuilderController.ORDER);
      long nbBytePerPixel = (long)( Math.abs(bitpix)/8 );

      // colonne des checkbox
      for (int i = 0; i < data.length; i++) {
         int order = i+3+BuilderController.ORDER;
         long nside = CDSHealpix.pow2(order);
         long nbPixel = 12*nside*nside;

         data[i][CHECK_IDX] = new Boolean(false);
         data[i][RES_IDX]   = Coord.getUnit( Math.sqrt(surface/nbPixel) );
         data[i][NSIDE_IDX] = order+" / "+nside;
         data[i][TILES_IDX] = (i+3)+" / "+(nbPixel/pixelPerFile);
         data[i][VOL_IDX]   = Util.getUnitDisk(nbPixel*nbBytePerPixel);
      }
      return data;
   }

   /**
    * R�cup�re le chiffre dans la colonne "order" de la ligne coch�e
    * @return order choisi ou celui par d�faut si rien n'est coch� ou -1
    */
   public int getOrder() {
      for (int i = 0; i < getRowCount(); i++) {
         if (getValueAt(i, CHECK_IDX) == Boolean.TRUE)  return i+3;
      }
      return defaultOrder;
   }

   /**
    * Pr�pare un texte pour afficher le niveau dans un tooltip
    * @param row
    * @return
    */
   public String getOrderToolTip(int row) {
      return "<html>512 x 2<sup>"+(row+3)+"</sup></html>";
   }

   public void updateData() {
      long nbBytePerPixel = (long)( Math.abs(bitpix)/8 );
      for (int i = 0; i < this.getRowCount(); i++) {
         int order = i+3+BuilderController.ORDER;
         long nside = CDSHealpix.pow2(order);
         long nbPixel = 12*nside*nside;
         this.setValueAt( Util.getUnitDisk(nbPixel*nbBytePerPixel) ,i,VOL_IDX);
      }
      repaint();
   }


   @Override
   public Object getValueAt(int row, int column) {
      return super.getModel().getValueAt(row, column);
   }

   public Class<? extends Object> getColumnClass(int c) {
      return getValueAt(0, c).getClass();
   }

   public void setValueAt(Object value, int row, int col) {
      if (col != CHECK_IDX) {
         super.setValueAt(value, row, col);
         return;
      }
      // interdit de d�cocher une ligne d�j� coch�e
      if (value == Boolean.TRUE && getValueAt(row, CHECK_IDX) == Boolean.TRUE) return;

      // v�rifie qu'une autre ligne n'est pas d�j� coch�e, si oui, on la
      // d�coche
      for (int i = 0; i < getRowCount(); i++) {
         super.setValueAt(new Boolean(false), i, CHECK_IDX);
      }

      super.setValueAt(value, row, col);
   }

   public boolean isCellEditable(int row, int col) {
      return col==0;
   }

}