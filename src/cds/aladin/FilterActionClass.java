/**
 * 
 */
package cds.aladin;

import java.util.Vector;

import javax.swing.JComboBox;

/**
 * @author chaitra
 *
 */
public interface FilterActionClass {
	
	public void checkSelectionChanged(JComboBox<String> comboBox);
	
	public Vector<String> getMatches(String mask, JComboBox<String> comboBox);

}
