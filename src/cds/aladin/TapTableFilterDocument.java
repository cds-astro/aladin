package cds.aladin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;

import cds.tools.Util;

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
public class TapTableFilterDocument extends PlainDocument{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9106769181814191291L;
	
	ServerTap serverTap;
	JComboBox<String> tablesGui;
	Map<String, TapTable> tablesMetaData;
	
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

	public TapTableFilterDocument() {
		// TODO Auto-generated constructor stub
	}

	public TapTableFilterDocument(ServerTap server) throws BadLocationException {
		this.serverTap = server;
		this.tablesGui = server.tablesGui;
		this.tablesMetaData = server.tablesMetaData;

		JTextComponent editor = (JTextComponent) tablesGui.getEditor().getEditorComponent();
		
		editor.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
//				System.err.println("keyPressed");
				if (tablesGui.isDisplayable())
					tablesGui.setPopupVisible(true);
				
				if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE || e.getKeyCode() == KeyEvent.VK_DELETE) {
					String mask = getMask();
					if (mask != null && !mask.isEmpty()) {
						processLevelFlag = 2;
					}
				} else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					processLevelFlag = 0;
					serverTap.changeTableSelection((String) serverTap.tablesGui.getSelectedItem());
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
		
		tablesGui.addActionListener(new ActionListener() {
			
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
		
		
		processLevelFlag = 0;
		setDefaultTable();
	}
	
	@Override
	public void remove(int offs, int len) throws BadLocationException {
		if (processLevelFlag < 0) {
			return;
		}
		super.remove(offs, len);
		if (processLevelFlag == 2) {
			searchTable();
			tablesGui.setPopupVisible(true);
		}
	}

	@Override
	public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
//		System.out.println("insertString called processLevelFlag: "+processLevelFlag);
		if (processLevelFlag < 0) {
			return;
		}
		super.insertString(offs, str, a);

		if (processLevelFlag > 0) {
			searchTable();
			tablesGui.setPopupVisible(true);
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
				setDefaultTable();
			} else if (!isValidSelection(mask)) {//when what is written on JComboBox is invalid/incomplete/not selected
				processLevelFlag = 0;
				setDefaultTable(getFirstItemOnJComboBox());
			}
//			System.err.println(tablesGui.getSelectedItem()+" ==? "+serverTap.selectedTableName);
//			System.out.println(tablesGui.getSelectedItem() != null
//					&& !serverTap.selectedTableName.equalsIgnoreCase(tablesGui.getSelectedItem().toString()));
			if (tablesGui.getSelectedItem() != null
					&& !serverTap.selectedTableName.equalsIgnoreCase(tablesGui.getSelectedItem().toString())) {
				Aladin.trace(3, "Change table selection from within the document");
				serverTap.changeTableSelection((String) serverTap.tablesGui.getSelectedItem());
			}
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	private void searchTable(){
		String mask = getMask();
		processLevelFlag = -1;
		if (mask == null || mask.trim().isEmpty()) {
			resetPopUpMenu();
		} else {
			Vector<String> matches = getMatches(mask);
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
		Document tablesGuiDocument = ((JTextComponent) tablesGui.getEditor().getEditorComponent()).getDocument();
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
		if (mask != null && tablesMetaData.containsKey(mask)) {
			result = true;
		}
		return result;
	}
	
	/**
	 * Sets the options of the tablesGui to default tables
	 */
	public void resetPopUpMenu() {
		Vector<String> tables = new Vector<String>(this.tablesMetaData.keySet().size());
		tables.addAll(this.tablesMetaData.keySet());
		resetPopUpMenu(tables);
	}
	
	/**
	 * Sets the options of the tablesGui to tables param
	 */
	public void resetPopUpMenu(Vector<String> tables) {
		DefaultComboBoxModel<String> items = new DefaultComboBoxModel<String>(tables);
		tablesGui.removeAllItems();
//		System.out.println("resetting popup menu options");
		/*if (tables!=null) {
			System.out.println("resetting popup menu options to something");
			
		}
		for (String string : tables) {
			System.out.print(string+", ");
		}
		System.out.println();*/
		tablesGui.setModel(items);
	}

	public String getFirstItemOnJComboBox() {
//		System.err.println("tablesGui.getSelectedItem() ::"+tablesGui.getSelectedItem());
//		System.err.println("tablesGui.getModel().getElementAt(0) ::"+tablesGui.getModel().getElementAt(0));
		return (String) tablesGui.getModel().getElementAt(0);
	}
	
	/**
	 * Performs a reset-to-empty and insert tableName on a JCombobox
	 * @param tableName
	 * @throws BadLocationException
	 */
	public void setDefaultTable(String tableName) throws BadLocationException {
		if (tableName != null) {
			super.remove(0, getLength());
			super.insertString(0, tableName, null);
			tablesGui.setSelectedItem(tableName);
		}
	}
	
	/**
	 * Performs a reset-to-empty and insert (one of the available) tablesName  on a JCombobox
	 * @param tableName
	 * @throws BadLocationException
	 */
	public void setDefaultTable() throws BadLocationException {
		String defaultTableSelected = tablesMetaData.keySet().iterator().next();
		setDefaultTable(defaultTableSelected);
	}

	/**
	 * Checks the likeliness of the user input with table name and the description(if available)
	 * @param mask
	 * @return matches
	 */
	private Vector<String> getMatches(String mask) {
		Vector<String> matches = new Vector<String>();
		if (mask != null && !mask.isEmpty()) {
			for (String tableName : this.tablesMetaData.keySet()) {
				boolean checkDescription = false;
				TapTable table = tablesMetaData.get(tableName);
				if (table != null && table.getDescription() != null && !table.getDescription().isEmpty()) {
					checkDescription = true;
				}
				if (!(Util.indexOfIgnoreCase(tableName, mask) >= 0
						|| (checkDescription && Util.indexOfIgnoreCase(table.getDescription(), mask) >= 0))) {
					continue;
				}
				matches.add(tableName);
			}
		}
		return matches;
	}
}
