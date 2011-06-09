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

import java.util.Vector;

/**
* <p>Generic class for other set classes </p>
* @author Andre Schaaff
* @version 2.6 Copyright CDS 2002-2005
*  (kickoff 31 May 02)
* @see SavotSet
*/
public class SavotSet {

  // storage of the set elements
  protected Vector set = null;

  /**
   * Constructor
  */
  public SavotSet() {
  }

  /**
   * Add an item to the set
   * @param item
   */
  public void addItem(Object item) {
    if (set == null)
      set = new Vector();
    set.addElement(item);
  }

  /**
   * Get an item at a given position (index)
   * @param index
   * @return Object
   */
  public Object getItemAt(int index) {
    if (set == null)
      return null;
    if (index >= 0 && index < set.size())
      return (Object)set.elementAt(index);
    else return null;
  }

  /**
   * Remove an item at a given position (index)
   * @param index
   */
  public void removeItemAt(int index) {
    if (index >= 0 && index < set.size())
      set.removeElementAt(index);
  }

  /**
   * Remove all items
   */
  public void removeAllItems() {
    if (set.size() > 0)
      set.removeAllElements();
  }

  /**
   * Set the whole set to a given set
   * @param set
   */
  public void setItems(Vector set) {
    this.set = set;
  }

  /**
   * Get the whole set
   * @return a Vector
   */
  public Vector getItems() {
    return set;
  }

  /**
   * Get the number of items
   * @return int
   */
  public int getItemCount() {
    if (set != null)
      return set.size();
    else return 0;
  }
}
