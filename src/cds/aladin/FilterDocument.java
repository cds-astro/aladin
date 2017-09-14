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

package cds.aladin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

/**
 * <p>This class is inspired by Thomas Bierhance's auto-completion code.
 * <br>Click:	<a href="http://www.orbital-computer.de/JComboBox/#usage">here</a> for more information.
 * The code "http://www.orbital-computer.de/JComboBox/source/AutoCompletion.java" is in Public domain
 * http://creativecommons.org/licenses/publicdomain/ <br></p>
 *  
 * <p>Though instead of auto-completion, this class aims only to shorten the options to choose as per the search criteria.
 * User still gets to type in their filter without the back-end interfering, unless they change focus(e.g. tab-out) from the <code>JComboBox</code>.</br>
 * 		- At this point a valid input is selected, whether the user has typed it in or not.</br>
 * During search the user can clear the search input with backspace/delete to see the full list of options again.</p>
 * TODO:: tintin the sysouts 
 */
public class FilterDocument extends PlainDocument{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9106769181814191291L;
	
	FilterActionClass actionClass;
	JComboBox<String> comboBox;
	List<String> keys;
	
	/**
	 ** <p>
	 * processLevelFlag for insertString and remove operations.
	   <ul>Set the flag as per the below list
	  	<li>-1 :: Processing i.e. already filtering.</li>
	  	<li> 0 :: Admin - straight-away inserts or removes</li>
	  	<li> 1 :: start processing- i.e. do filtering </li>
	  	<li> 2 :: do a remove if there is no input, for backspace/delete key operations</li>
	 * </ul>
	 * </p>
	 */
	int processLevelFlag = 0;

	public FilterDocument() {
		// TODO Auto-generated constructor stub
	}

	public FilterDocument(FilterActionClass actClass, JComboBox<String> gui, List<String> keys, String defaultSelection) throws BadLocationException {
		this.actionClass = actClass;
		this.comboBox = gui;
		this.keys = keys; //is the model that reflects your combobox. if you discard an entry then programmatically as you are already discarding table in tapmeta, 
		//the filter will automatically pick up
		//for now filter is only used for "tap tables"
		//if it is needed for more.. i.e. keys cannot point to tap metadata but to something more generalized 
		//and implement the below to ensure to update keys if you remove/add an item from gui
		//TODO:: if we extend filter's usuage beyond tap
		/*gui.getModel().addListDataListener(new ListDataListener() {
			
			@Override
			public void intervalRemoved(ListDataEvent e) {
				// TODO Auto-generated method stub
				System.err.println("removed"+((DefaultComboBoxModel)e.getSource()).getElementAt(e.getIndex0()-1));
			}
			
			@Override
			public void intervalAdded(ListDataEvent e) {
				// TODO Auto-generated method stub
				System.err.println("added"+ ((DefaultComboBoxModel)e.getSource()).getElementAt(e.getIndex0()-1));
			}
			
			@Override
			public void contentsChanged(ListDataEvent e) {
				// TODO Auto-generated method stub
				System.err.println("changed");
			}
		});*/
		
		JTextComponent editor = (JTextComponent) comboBox.getEditor().getEditorComponent();
		
		editor.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
//				System.err.println("keyPressed");
				if (comboBox.isDisplayable())
					comboBox.setPopupVisible(true);
				
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
					String mask = getMask();
					if (mask != null && !mask.isEmpty()) {
						processLevelFlag = 2;
					}
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					processLevelFlag = 0;
//					actionClass.tableSelectionChanged(comboBox);
					actionClass.checkSelectionChanged(comboBox);
				} else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
					processLevelFlag = -1;// can put this to 0 if you want the table to change on key-up and down selections
				} else {
					processLevelFlag = 1;
				}
			}
		});
		
		editor.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
//				System.err.println("focuslost triggered");
				setGuiAsPerUserInput();
				
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				// TODO Auto-generated method stub
			}
		});
		
		comboBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
//				System.err.println("ActionListener:: pro: "+processLevelFlag+", "+e.paramString());
				if (processLevelFlag < 0 ) {
					return;
				} else {
					setGuiAsPerUserInput();
				}
			}
		});
		
		
		/*tablesGui.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				System.err.println("itemStateChanged");
		       if (event.getStateChange() == ItemEvent.SELECTED) {
		    	   System.err.println("itemStateChanged selected");
		    	   setGuiAsPerUserInput();
		       }
			}
		});*/
		
		processLevelFlag = -1;
		if (comboBox.getItemCount() > 0) {
			if (defaultSelection == null) {
				setDefault();
			} else {
				setDefault(defaultSelection);
			}
		}
		processLevelFlag = 0;
	}
	
	@Override
	public void remove(int offs, int len) throws BadLocationException {
		if (processLevelFlag < 0) {
			return;
		}
		super.remove(offs, len);
		if (processLevelFlag == 2) {
			search();
			comboBox.setPopupVisible(true);
		}
	}

	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
//		System.out.println("insertString called processLevelFlag: "+processLevelFlag);
		if (processLevelFlag < 0) {
			return;
		}
		super.insertString(offs, str, a);
//		System.out.println("insertString called processLevelFlag: "+processLevelFlag);
		if (processLevelFlag > 0) {
			search();
			comboBox.setPopupVisible(true);
		}
//		Aladin.trace(3, "is it windows platform: "+Aladin.winPlateform);
		if (Aladin.winPlateform) {//for windows issue
			JTextComponent editor = (JTextComponent) comboBox.getEditor().getEditorComponent();
			editor.setCaretPosition(getLength());
		}
		
	}
	
	/**
	 * <p>Method for setting a valid input text in the JComboBox 
	 *    and triggers the changing the serverTap gui accordingly
	 * <ol>
	 * 	<li> in case null if the input: will set the first of the JComboBox options</li>
	 * 	<li> in case there is a input: will set the first of the matches </li>
	 * </ol>
	 * No action in case a valid input is already chosen.<p>
	 */
	public void setGuiAsPerUserInput() {
		try {
			String mask = getMask();
			if (mask == null || mask.isEmpty()) {//when nothing is written on JComboBox
				processLevelFlag = 0;
				setDefault();
			} else if (!isValidSelection(mask)) {//when what is written on JComboBox is invalid/incomplete/not selected
				processLevelFlag = 0;
				setDefault(getFirstItemOnJComboBox());
			}
//			System.err.println(tablesGui.getSelectedItem()+" ==? "+serverTap.selectedTableName);
//			System.out.println(tablesGui.getSelectedItem() != null
//					&& !serverTap.selectedTableName.equalsIgnoreCase(tablesGui.getSelectedItem().toString()));
			actionClass.checkSelectionChanged(comboBox);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private void search(){
		String mask = getMask();
		processLevelFlag = -1;
		if (mask == null || mask.trim().isEmpty()) {
			resetPopUpMenu();
		} else {
			Vector<String> matches = actionClass.getMatches(mask, this.comboBox);
			if (!matches.isEmpty()) {
				resetPopUpMenu(matches);
			}
		}
		processLevelFlag = 0;
	}
	
	/**
	 * Method to get the user input on the editable JComboBox
	 * @return
	 */
	public String getMask() {
		Document tablesGuiDocument = ((JTextComponent) comboBox.getEditor().getEditorComponent()).getDocument();
		String mask = null;
		try {
			mask = tablesGuiDocument.getText(0, tablesGuiDocument.getLength()).trim();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return mask;
	}
	
	public boolean isValidSelection(String mask) {
		boolean result = false;
		if (mask != null && keys.contains(mask)) {
			result = true;
		}
		return result;
	}
	
	/**
	 * Sets the options of the tablesGui to default tables
	 */
	public void resetPopUpMenu() {
		Vector<String> resetKeys = new Vector<String>(this.keys.size());
		resetKeys.addAll(this.keys);
		resetPopUpMenu(resetKeys);
	}
	
	/**
	 * Sets the options of the tablesGui to tables param
	 */
	public void resetPopUpMenu(Vector<String> keys) {
		DefaultComboBoxModel items = new DefaultComboBoxModel(keys);
		comboBox.removeAllItems();
//		System.out.println("resetting popup menu options");
		/*if (tables!=null) {
			System.out.println("resetting popup menu options to something");
			
		}
		for (String string : tables) {
			System.out.print(string+", ");
		}
		System.out.println();*/
		comboBox.setModel(items);
	}

	public String getFirstItemOnJComboBox() {
//		System.err.println("tablesGui.getSelectedItem() ::"+tablesGui.getSelectedItem());
//		System.err.println("tablesGui.getModel().getElementAt(0) ::"+tablesGui.getModel().getElementAt(0));
		return (String) comboBox.getModel().getElementAt(0);
	}
	
	/**
	 * Performs a reset-to-empty and insert tableName on a JCombobox
	 * @param tableName
	 * @throws BadLocationException
	 */
	public void setDefault(String tableName) throws BadLocationException {
		if (tableName != null && this.keys.contains(tableName)) {
			super.remove(0, getLength());
			super.insertString(0, tableName, null);
			comboBox.setSelectedItem(tableName);
		}
	}
	
	/**
	 * Performs a reset-to-empty and insert (one of the available) tablesName  on a JCombobox
	 * @param tableName
	 * @throws BadLocationException
	 */
	public void setDefault() throws BadLocationException {
		String defaultTableSelected = this.keys.get(0);
		setDefault(defaultTableSelected);
	}
	
}
