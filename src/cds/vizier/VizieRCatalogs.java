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

import java.awt.*;
import java.util.Vector;
import cds.tools.*;
import java.awt.event.*;

import javax.swing.*;

import cds.aladin.*;

/**
 * VizieRCatalogs
 *
 * @author Andre Schaaff [CDS]
 * @version 1.1 : (september 2002) VizieRCatalogs
 * @version 1.0 beta : (june 2002) renamed as VizieRCatalogs
 * @version 0.9 : (february 2002) creation
 */
public class VizieRCatalogs extends JFrame implements CDSConstants, WindowListener {

  // a reference to a VizieRList object
  protected VizieRTable vizierlist = null;

  // the frame title
  protected CDSLabel title = null;

  // a control button
  protected JButton controlButton = null;

  /** Constructor VizieRCatalogs
    *
    * @param cat a field in which the catalog selections are set (like the catalog field from
Aladin)
    * @param getReadMe the Get info. button from Aladin
    * @param catalogs a vector of catalogs
    * @param controlButton a control button (like SUBMIT)
    */
  public VizieRCatalogs(JTextField cat, final JButton getReadMe, final JButton getMoc, final Vector catalogs, JButton
controlButton) {

     Aladin.setIcon(this);

    // main panel
    JPanel panelMain = new JPanel();
    panelMain.setFont(BOLD);
    BorderLayout mainLayout = new BorderLayout(0, 0);

    panelMain.setLayout( mainLayout );
    title = new CDSLabel("Catalogs", Label.LEFT, PLAIN);
    panelMain.add(title, "North");

    // creates a list of catalogs and add it to the main panel
    vizierlist = new VizieRTable(cat, getReadMe, getMoc, catalogs, 20, VizieRTable.SEARCH_MODE);
    JScrollPane scroll = new JScrollPane(vizierlist);
    scroll.setSize(580,600);
    panelMain.add(scroll, "Center");

    // left button(s) panel
    JPanel panelButtonLeft = new JPanel();
    panelButtonLeft.setFont(BOLD);
    if (getReadMe != null) panelButtonLeft.add(getReadMe);
    if (getMoc != null ) panelButtonLeft.add(getMoc);

    // right buttons panel
    JPanel panelButtonRight = new JPanel();
//    panelButtonRight.setBackground(BKGD);
    panelButtonRight.setFont(BOLD);

    // if control button exists then it is added to the right panel
    if (controlButton != null) {
      this.controlButton = controlButton;
      panelButtonRight.add(controlButton);
    }
    JButton reset = new JButton("Reset");
    JButton close = new JButton("Close");
    panelButtonRight.add(reset);
    panelButtonRight.add(close);

    // bottom panel
    JPanel panelBottom = new JPanel();
//    panelBottom.setBackground(BKGD);
    panelBottom.setFont(BOLD);
    BorderLayout buttonlayout = new BorderLayout(5, 5);
    panelBottom.setLayout( buttonlayout );
    panelBottom.add(panelButtonLeft, "West");
    panelBottom.add(panelButtonRight, "East");

    panelMain.add(panelBottom, "South");

    // Reset button listener
    reset.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (vizierlist != null) {
           vizierlist.resetList();
//          for (int i = 0; i < vizierlist.getItemCount(); i++)
//            vizierlist.deselect(i);
          if (vizierlist.getCatalogField() != null)
            vizierlist.getCatalogField().setText("");
        }
        if (getReadMe != null)
          getReadMe.setEnabled(false);
      }
      }
    );

    // Close button listener
    close.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (getReadMe != null)
          getReadMe.setEnabled(false);
        hide();
      }
      }
    );

    // listener
    addWindowListener(this);

    // add main panel to frame content panel, pack and show
    getContentPane().add(panelMain);
    pack();
    setLocation(300,300);

    //    this.setSize((vizierlist.getHCaracterCount() * 6), this.getSize().height);
    show();
    }

  /** Constructor VizieRCatalogs
   *
   * @param catalogs a vector of catalogs
   * @param controlButton a control button (like SUBMIT)
   */
   public VizieRCatalogs(final Vector catalogs, JButton controlButton) {
    this(null, null, null, catalogs, controlButton);
   }

  /** Constructor VizieRCatalogs
    *
    * @param catalogs a vector of catalogs
    */
   public VizieRCatalogs(final Vector catalogs) {
    this(null, null, null, catalogs, null);
   }

  /** Show a preselection in vizier list
   *
   * @param v the contain of the vector is used to preselect elements in the VizieR catalog list
   */
  public void show(Vector v) {
    if (vizierlist != null) {
      vizierlist.resetList();
      vizierlist.preSelection(v);
    }
    show();
  }

  /** Reset catalog list
   *
   */
  public void resetCatList() {
    if (vizierlist != null)
      vizierlist.resetList();
  }

  /** Set a title
   *
   * @param title used to set the title of the frame
   */
  public void setLabel(String title) {
      this.title.setText(title);
  }

  /** Get Title
   *
   * @return CDSLabel the title of the frame
   */
  public CDSLabel getLabel() {
    return title;
  }

  /** Get Vizer List
   *
   * @return VizieRList return a reference to the VizieR catalog list
   */
  public VizieRTable getVizerList() {
    return vizierlist;
  }

  /** Set Vizier List
   *
   * @param vizierlist used to set the VizieR catalog list
   */
  public void setVizerList(VizieRTable vizierlist) {
    this.vizierlist = vizierlist;
  }

  /** Get Control Button
   *
   * @return controlButton used to get a reference to the control button
   */
  public JButton getControlButton() {
    return controlButton;
  }

  /** Set Control Button
   *
   * @param controlButton used to set a control button
   */
  public void setControlButton(JButton controlButton) {
    this.controlButton = controlButton;
  }

  /** Windows closing
   *
   * @param e window event
   */
  public void windowClosing(WindowEvent e){
    hide();
  }

  /** Window Closed
   *
   * @param e WindowEvent
   */
  public void windowClosed(WindowEvent e){
    hide();
  }

  // other methods not implemented
  public void componentShown(ComponentEvent e) {}
  public void actionPerformed(ActionEvent e) {}
  public void windowOpened(WindowEvent e){}
  public void windowDeactivated(WindowEvent e){}
  public void windowActivated(WindowEvent e){}
  public void windowDeiconified(WindowEvent e){}
  public void windowIconified(WindowEvent e){}
}
