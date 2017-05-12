package cds.aladin;

import java.awt.Component;
import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

public class TapClientModesRenderer extends BasicComboBoxRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4372535217338044431L;

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		String label = (String) value;
		if (index < 0) {
			setText("");
			setToolTipText(TapClient.modesToolTip);
			Image image = Aladin.aladin.getImagette(TapClient.modeIconImage);
			if (image != null) {
				setIcon(new ImageIcon(image));
			}
		} else {
			setText(label);
			setToolTipText(TapClient.modeIconToolTips[index]);
			setIcon(null);
		}
		setBackground(Aladin.BLUE);
		
		return this;
	}

}