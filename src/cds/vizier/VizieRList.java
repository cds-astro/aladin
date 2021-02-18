// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.vizier;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cds.aladin.Aladin;

/**
 * VizieR catalog list
 * <P>
 * Two important things : a text field and a button can be added from outside
 * <P>
 * Example of button use : show additional informations for a selected catalog
 * <P>
 * Text field use is fixed : selected catalog names are added to this field with a comma as separator
 * <P>
 * Multiple selection are allowed.
 * @author Pierre Fernique [CDS]
 * @version 1.1 : (august 2002) votable ready
 * @version 1.0 : (february 2002) transfer to VizieR package [Andre Schaaff] and add of methods
 * @version Aladin 1.0 (in Aladin package): (11 mai 99) Toilettage du code
 * @version Aladin 0.9 (in Aladin package): (??) creation
 */
public final class VizieRList extends JList {
   protected JTextField catalog;    // Catalog name field
   protected JButton getReadMe;     // Button used to get (for example) informations about a selected catalog
   protected int hCaracterCount = 0;
   protected MyList list;

//   static final boolean LSCREEN= Toolkit.getDefaultToolkit().getScreenSize().width>1000;
//
//    /** Taille moyenne des fonts */
//    static protected final int  SIZE   = LSCREEN?12:10;
//
//    protected static Font COURIER= new Font("Monospaced",Font.PLAIN, SIZE);

   /** Constructor List creation
    * Set the selected catalogs
    * @param catalog text field (can be optional)
    * @param getReadMe button (can be optional)
    * @param v vector of lines to put into the list
    */
   public VizieRList(JTextField catalog, JButton getReadMe, Vector v) {
      this(catalog, getReadMe, v, 20);
   }

   /** Constructor List creation
    *
    * @param catalog text field (can be optional)
    * @param getReadMe button (can be optional)
    * @param v vector of lines to put into the list
    * @param rows number of rows to show at one time
    */
   public VizieRList(JTextField catalog, JButton getReadMe, Vector v,int rows) {
      setVisibleRowCount(rows);
      this.catalog = catalog;
      this.getReadMe = getReadMe;
      this.setFont( Aladin.COURIER );
      setModel(list = new MyList());
      preSelection(v);
      addListSelectionListener(new ListenList());
   }

   /** Constructor List creation
    *
    * @param v vector of lines to put into the list
    */
   public VizieRList(Vector v) {
      this(null, null, v, 20);
   }

  /** Set the default list
   * Set the selected catalogs
   * @param v vector of lines show in the preselection
   */
   public void preSelection(Vector v) {
      list.initList(v);

//      hCaracterCount = 0;
//      if( countItems()>0 ) clear();
//
//      Enumeration e = v.elements();
//      while( e.hasMoreElements() ) {
//        String s = (String)e.nextElement();
//        addItem(s);
//        int length = s.length();
//        if (hCaracterCount < length)  hCaracterCount = length;
//      }
   }
   
   /** Retourne la liste sous forme d'un vector */
   public Vector getList() { return list.v; }
   
  /** Reset the list
   *
   */
   public void resetList() {
      getSelectionModel().clearSelection();
//      for( int i=0; i<countItems(); i++ )
//        if( isSelected(i) ) deselect(i);
//          makeVisible(0);
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

   public int getHCaracterCount() {
    return hCaracterCount;
   }
   
   class MyList implements ListModel {
      private Vector v;
      
      protected int initList(Vector v) {
         this.v = (Vector)v.clone();
         int hCaracterCount=0;
         Enumeration e = v.elements();
         while( e.hasMoreElements() ) {
            int l = ((String)e.nextElement()).length();
            if( l>hCaracterCount ) hCaracterCount = l;
         }
         return hCaracterCount;
      }
      
      protected void add(String s) {
         v.add(s);
      }

      public Object getElementAt(int index) {
         return v.elementAt(index);
      }

      public int getSize() {
         return v.size();
      }

      public void addListDataListener(ListDataListener l) { }
      public void removeListDataListener(ListDataListener l) { }
      
   }

   
   class ListenList  implements ListSelectionListener {
      
      public void valueChanged(ListSelectionEvent e) {
         String s="";
         int i;
         // viziercat setting is depending on selected catalog
         Object [] name = getSelectedValues();
         for( i=0; i<name.length; i++ ) {

            // Get the first word to retrieve the catalog number
            StringTokenizer st = new StringTokenizer((String)name[i]," :");
            String id = st.nextToken();

            // Catalog id is merge in the catalog field
            // if shift is set, it is added to the previous
            if( s.equals("") == false ) s = s + "," + id;
            else s = id;
         }
         if (catalog != null)
            catalog.setText(s);

         // Readme button avtivation or not
         if (getReadMe != null){
            getReadMe.setEnabled(i == 1);

            String lastSelectedCatalog = (String)getSelectedValue();
            if (lastSelectedCatalog != null) {
               StringTokenizer st = new StringTokenizer(lastSelectedCatalog," :");
               String id = st.nextToken();
            }
         }
      }
   }


}
