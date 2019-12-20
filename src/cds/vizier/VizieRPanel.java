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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Label;
import java.io.DataInputStream;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.aladin.MyInputStream;
import cds.aladin.MyLabel;
import cds.aladin.ServerVizieR;
import cds.tools.CDSConstants;
import cds.tools.CDSMethods;

/**
 * VizieR panel
 * <P>
 * This panel has multiple usage : as an integrated panel in Aladin application but also in
 * other application as an external api
 * <P>
 *
 * @authors Pierre Fernique - Andre Schaaff [CDS]
 * @version 0.9 : (fevrier 2002) creation
 */
public class VizieRPanel extends JPanel implements CDSConstants {

   // Object components
   protected MyLabel titre = null;              // Title
   protected JTextField tauthor = new JTextField(15); // Free text
   protected JTextField ttarget = new JTextField(15); // Target field
   protected JTextField tradius = new JTextField(15); // Radius field
   protected JComboBox unit = new JComboBox();        // Unit choice
   protected JComboBox coordinate = null;             // Coordinate choice

   // Astrores complient stream, result of the query
   protected DataInputStream vizierStream = null;

   // Output as stream or in a frame
   protected int outputMode = FRAME;

   // List of lists of constraints
   protected JList lk[]; 		      // n keywords lists
   private MyList resList = new MyList();
   protected JList resultat = new JList(resList);     // List filled with the query result

   // target and radius strings
   protected String target = null;
   protected String radius = null;

   // VizieR meta data

   protected int nSection;

   // GLU
   protected Aladin aladin;
//   protected Jglu glu = null; // GLU interactions, must be replaced with JGlu as soon as possible

   // Text for buttons, titles, ...
   static final String SUBMIT = "SUBMIT";
   String DEFAULT_TITRE,KEYWORD;
   static final String TITRE = "Copyright CDS, a changer";
   static final int DEFAULTROWS = 6;

   VizieRQuery vq = null;
   private ServerVizieR vizier;

   protected BorderLayout borderLayout1 = new BorderLayout();

   /** Form title modification depending on the presence of a target
   *
   * @param target the string containing the target, ou <I>null</I> if nothing
   */
   protected void setTitre(String target){
      String s = null;
      s=(target==null)?DEFAULT_TITRE:
                       "Click directly on the SUBMIT button to retrieve all VizieR catalogs around " + target +
                         "\n" +  " or include constraints below to reduce the number of matching catalogs.";
      if (s!= null)
        titre.setText(s);
      else
        titre.setText("probleme");
   }

   protected void createChaine() {
      Chaine c = aladin.getChaine();		// Qu'est-ce qui faut pas faire !!
      DEFAULT_TITRE=c.getString("VZINFO2");
      KEYWORD=c.getString("VZKEYWORD");

   }

  /** Constructor
  *
  * @param glu
  * @param outputMode
  * @param withparam
  * @param target
  * @param radius
  * @param rows
  */
   public VizieRPanel(ServerVizieR vizier, int outputMode, boolean withparam, String target, String radius, int rows) {
    try {
      this.vizier = vizier;
      aladin= vizier.aladin;
      
      createChaine();
      this.outputMode = outputMode;

      //
      vq = new VizieRQuery();
      vq.setGLU(aladin.glu);

      // VizieR call for meta information
      if( vq.metaDataQuery() == false || vq.getNameKey().size() == 0 ) {
        System.err.println("VizieR meta query error "+(vq.getMetaError()==null?"":vq.getMetaError()));
      }

      nSection = vq.getNameKey().size();
      this.setLayout(borderLayout1);
      makeForm(withparam, target, radius, rows);
    }
    catch(Exception ex) {
      ex.printStackTrace();
    }
  }

   /** Return the VizieRQuery reference - PF 21 nov 05 */
   public VizieRQuery getVizieRQuery() { return vq; }

   /** Cursor corresponding to the java machine
   *  used to resolve security checking
   *
   * @param c component
   * @param type
   */
   protected static void makeCursor(Component c, int type) {

    while( c!=null && !(c instanceof Frame) )
      c=c.getParent();
    if( c==null )
      return;
    ((Frame)c).setCursor(type==WAIT?Frame.WAIT_CURSOR:
                             type==HAND?Frame.HAND_CURSOR:
                             type==CROSSHAIR?Frame.CROSSHAIR_CURSOR:
                             type==MOVE?Frame.MOVE_CURSOR:
                             type==RESIZE?Frame.N_RESIZE_CURSOR:
                             type==TEXT?Frame.TEXT_CURSOR:
                             Cursor.DEFAULT_CURSOR
                             );
   }

   /** Cursor management
   *
   */
   private int oc = DEFAULT;
   private void waitCursor() { makeCursor(WAIT); }
   private void defaultCursor() { makeCursor(DEFAULT); }
   private void makeCursor(int c) {
      if( oc==c )
        return;
      makeCursor(this,c);
      if( resultat!=null )
        makeCursor(resultat,c);
      makeCursor(tauthor,c);
      for( int i=0; i<lk.length; i++ )
        makeCursor(lk[i],c);
      oc=c;
   }

  /** Return a keyword section name
  * ex: -kw.Astronomy returns Astronomy
  *
  * @param s
  * @return String
  */
  private String getSectionName(String s) {
    int i;
    char [] a = s.toCharArray();

    for( i=0; i<a.length && a[i]!='.'; i++);

    return i==a.length ? s : new String(a,i+1,a.length-i-1);
   }

   /** Add in a Layout depending on Java Virtual Machine
    *
   * @param ct Container
   * @param c Component
   * @param s String
   *
   */
   protected static void makeAdd(Container ct,Component c,String s) {
    try {
      ct.add(c,s);
    } catch( Error e ) {
        ct.add(s,c);
      }
   }

  /** Manage the associated panels design
    *
    * @param withparam
    * @param target
    * @param radius
    * @param rows
    */
   protected void makeForm(boolean withparam,String target, String radius, int rows) {
    JPanel byword = null;       // keyword panel
    JPanel fields = null;       // fields panel
    JPanel parameters = null;   // parameters panel
    JPanel lll = null;          // 3 lists panel
    JPanel actions = null;      // action panel
    int i;

    GridBagConstraints  c = new GridBagConstraints();

    // Form title setting
    titre = new MyLabel(DEFAULT_TITRE, Label.CENTER, PLAIN);
    titre.setBackground(Aladin.BLUE);
    fields = new JPanel();
    fields.setLayout( new BorderLayout(3,3) );

    byword = new JPanel();
    byword.setBackground(Aladin.BLUE);
    byword.setLayout( new BorderLayout(3,3) );
    byword.setFont(BOLD);

    // Construction
    // title panel and keyword query panel
    JPanel haut = new JPanel();
    haut.setBackground(Aladin.BLUE);
    haut.setLayout( new BorderLayout(3,3) );

    // Free keyword interrogation panel (author...)
    makeAdd(byword,new JLabel(KEYWORD),"West");
    makeAdd(byword,tauthor,"Center");

    // Window components are depending on
    if ((target == null && radius == null)){

      // Target panel construction
      parameters = new JPanel();
      GridBagLayout gridbag = new GridBagLayout();

      parameters.setLayout( gridbag );
      parameters.setFont(BOLD);

      JLabel label = new JLabel("Target ");
      c = new GridBagConstraints();
      c.gridx = 0;
      c.gridy = 0;
      c.gridwidth = 2;
      c.weightx = 1;
      c.weighty = 0.1;
      c.anchor = GridBagConstraints.SOUTH;
      c.insets = new Insets(5,5,5,5);
      gridbag.setConstraints(label, c);
      parameters.add(label);

      // Target field
      c = new GridBagConstraints();
      c.gridx = 2;
      c.gridy = 0;
      c.gridwidth = 4;
      c.weightx = 1;
      c.weighty = 0.1;
      c.ipadx = 50;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.insets = new Insets(5,5,5,5);
      gridbag.setConstraints(ttarget, c);
      parameters.add(ttarget);

      // Create and fill coordinate popup
      coordinate = new JComboBox();
      coordinate.addItem("today");
      coordinate.addItem("J2000");
      coordinate.addItem("B1975");
      coordinate.addItem("B1950");
      coordinate.addItem("B1900");
      coordinate.addItem("B1875");
      coordinate.addItem("B1855");
      coordinate.addItem("Galactic");
      coordinate.addItem("Supergal.");
      coordinate.setSelectedItem("J2000");

      // Coordinate field
      c = new GridBagConstraints();
      c.gridx = 6;
      c.gridy = 0;
      c.gridwidth = 2;
      c.weightx = 1;
      c.weighty = 0.1;
      c.insets = new Insets(5,5,5,5);
      gridbag.setConstraints(coordinate, c);
      parameters.add(coordinate);

      // Radius label
      label = new JLabel("Radius ");
      c = new GridBagConstraints();
      c.gridx = 8;
      c.gridy = 0;
      c.gridwidth = 2;
      c.weightx = 1;
      c.weighty = 0.1;
      c.anchor = GridBagConstraints.SOUTH;
      c.insets = new Insets(5,5,5,5);
      gridbag.setConstraints(label, c);
      parameters.add(label);

      // Radius field
      c = new GridBagConstraints();
      c.gridx = 10;
      c.gridy = 0;
      c.gridwidth = 1;
      c.weightx = 0.1;
      c.weighty = 0.1;
      c.insets = new Insets(5,5,5,5);
      gridbag.setConstraints(tradius, c);
      parameters.add(tradius);
      tradius.setText("10.0"); // default value

      // Radius panel construction
      unit = new JComboBox();
      unit.addItem("deg");
      unit.addItem(ARCMIN);
      unit.addItem(ARCSEC);

      // Set default unit to arcmin
      unit.setSelectedItem(ARCMIN);
      c = new GridBagConstraints();
      c.gridx = 11;
      c.gridy = 0;
      c.gridwidth = 2;
      c.weightx = 1;
      c.weighty = 0.1;
      c.insets = new Insets(5,5,5,5);
      gridbag.setConstraints(unit, c);
      parameters.add(unit);
    }
    else {
      if (radius != null)
        tradius.setText(radius);
      if (target != null)
        ttarget.setText(target);
    }

    // Keywords section panel construction
    lll = new JPanel();

    GridBagLayout gridbag = new GridBagLayout();
    lll.setLayout( gridbag );
    lll.setFont(BOLD);
    lll.setBackground(Aladin.BLUE);
    c = new GridBagConstraints();

    lk = new JList[nSection];
    JLabel label = null;
    for( int j=0; j<nSection; j++ ) {
      label = new JLabel();
      c = new GridBagConstraints();
      c.gridx = j;
      c.gridy = 0;
      c.gridwidth = 1;
      c.weightx = 0.1;
      c.weighty = 0.1;
      c.fill = GridBagConstraints.HORIZONTAL;
      c.anchor = GridBagConstraints.SOUTH;
      c.insets = new Insets(0, 0, 0, 0);
      gridbag.setConstraints(label, c);
      label.setText(getSectionName((String)vq.getNameKey().elementAt(j)));

      lll.add(label);

      Vector v = (Vector)vq.gethKey().get(vq.getNameKey().elementAt(j));
      if (rows < 0) lk[j] = new JList(v);
      else {
        lk[j] = new JList(v);
        lk[j].setVisibleRowCount(rows);
        lk[j].setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        lk[j].addListSelectionListener( new ListSelectionListener() {
         public void valueChanged(ListSelectionEvent e) {
           vizier.resetCatalog(); 
         }
      });
      }
//      Enumeration e = v.elements();
//      while( e.hasMoreElements() )
//        lk[j].addItem((String)e.nextElement());
      c = new GridBagConstraints();
      c.gridx = j;
      c.gridy = 1;
      c.gridwidth = 1;
      c.weightx = 1;
      c.weighty = 1;
      c.fill = GridBagConstraints.BOTH;
      c.insets = new Insets(0, 2, 0, 2);

      JScrollPane jp = new JScrollPane(lk[j]);
      gridbag.setConstraints(jp, c);
      lll.add(jp);
    }

    // Change to a GridBagLayout to have non equal lists

    // Action button panel construction
    actions = new JPanel();
    actions.setBackground(Aladin.BLUE);
    actions.setLayout( new FlowLayout(FlowLayout.RIGHT));
    actions.setFont( BOLD );

    makeAdd(haut, titre, "North");
    makeAdd(haut, byword, "Center");
    if ((target == null && radius == null) && withparam == true)
      makeAdd(haut, parameters, "South");

    // List and action panel
    JPanel bas = new JPanel();
    bas.setBackground(Aladin.BLUE);
    bas.setLayout( new BorderLayout(3, 3) );
    makeAdd(bas, lll, "Center");
    makeAdd(bas, actions, "South");

    // Previous panels are added to the main frame
    makeAdd(this, haut, "North");
    makeAdd(this, bas, "Center");

    setTitre(this.target);
  }

  /** Reset a keyword list
   * @param l list to reset
   */
   public static void resetList(JList l) {
      l.clearSelection();
//      for( int i=0; i<l.countItems(); i++ )
//        if( l.isSelected(i) ) l.deselect(i);
//          l.makeVisible(0);
   }


   /**
    * Form reset
    */
   public void resetAll() {
      tauthor.setText("");
      if (ttarget != null) {
        ttarget.setText("");
        tradius.setText("10.0");
      }
      for( int i=0; i<nSection; i++ )
        resetList(lk[i]);
   }

    /**
    *
    * @return boolean
    */
    public boolean submit() {
      int selected[];
      int i;
      StringBuffer extra = new StringBuffer();

      waitCursor();

      for( int j = 0; j < nSection; j++ ) {
        selected = lk[j].getSelectedIndices();
        for( i = 0; i < selected.length; i++ )
          CDSMethods.append(extra, (String)vq.getNameKey().elementAt(j), (String)lk[j].getModel().getElementAt(selected[i]));
      }

      boolean res = vq.submit(ttarget.getText(), tradius.getText(), (String)unit.getSelectedItem(), tauthor.getText(), extra.toString(), outputMode, resList.getList());
      defaultCursor();
      return res;
    }

    /**
     * submit
     * @param a
     * @param b
     * @return boolean
     */
    public boolean submit(String a, String b) {
      int selected[];
      int i;
      StringBuffer extra = new StringBuffer();

      waitCursor();

      for( int j = 0; j < nSection; j++ ) {
        selected = lk[j].getSelectedIndices();
        for( i = 0; i < selected.length; i++ )
          CDSMethods.append(extra, (String)vq.getNameKey().elementAt(j), (String)lk[j].getModel().getElementAt(selected[i]));
      }

      boolean res = vq.submit(a, b, (String)unit.getSelectedItem(), (String)coordinate.getSelectedItem(), tauthor.getText(), extra.toString(), outputMode, resList.getList());
      defaultCursor();
      return res;
    }

  /**
   * Returns Missions or Surveys
   *
   * @param type MISSION or SURVEY
   * @return a strings table
   */
    public String[] getSelection(String type) {

      try {
        // type MISSION
        if (type.compareTo("MISSION") == 0) {
          String[] temp = new String[vq.getvArchives().size()];

          for (int i = 0; i < vq.getvArchives().size(); i++) {
            temp[i] = new String((String)vq.getvArchives().elementAt(i));
          }
          return temp;
        }
        else // type SURVEY
        if (type.compareTo("SURVEY") == 0) {
          String[] temp = new String[vq.getvSurveys().size()];

          for (int i = 0; i < vq.getvSurveys().size(); i++) {
            temp[i] = new String((String)vq.getvSurveys().elementAt(i));
          }
          return temp;
        }
      } catch(ArrayIndexOutOfBoundsException e) {
        System.out.println(e);
        }
      return null;
    }

   /**
    * Return the Catalogs data input stream
    *
    * @return xml stream
    */
   public MyInputStream getResultStream() {
      return vq.getResultStream();
   }

   /** Return the Catalogs data list
    *
    * @return a List
    */
   public JList getResultList() {
      return this.resultat;
   }

   /**
    * Return the Catalogs data list
    *
    * @return a Vector
    */
   public Vector getResultVector() {
      return resList.getList();
//      Vector result = new Vector();
//      for (int i = 0; i < this.resultat.getItemCount(); i++)
//        result.addElement(this.resultat.getItem(i));
//      return result;
   }

    /**
     * getAuthor
     *
     * @return author TextField
     */
    public JTextField getAuthor() {
      return tauthor;
    }

    /**
     * getList
     *
     * @param i
     * @return a list
     */
    public JList getList(int i) {
      return lk[i];
    }

    /**
     * getListCount
     *
     * @return length of the list
     */
    public int getListCount() {
      return lk.length;
    }

    /**
     * Set label
     *
     * @param label
     */
    public void setLabel(String label) {
      titre.setText(label);
    }

    class MyList implements ListModel {
       private Vector v = new Vector();

       protected void initList(Vector v) { this.v = (Vector)v.clone(); }

       protected Vector getList() { return v; }

       protected void add(String s) { v.add(s); }

       public Object getElementAt(int index) { return v.elementAt(index); }

       public int getSize() { return v.size(); }

       public void addListDataListener(ListDataListener l) { }
       public void removeListDataListener(ListDataListener l) { }

    }

}
