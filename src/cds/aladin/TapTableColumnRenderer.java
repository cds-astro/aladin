package cds.aladin;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class TapTableColumnRenderer extends JLabel implements ListCellRenderer {
	
	protected DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();

	@Override
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {
		JLabel column = (JLabel) defaultRenderer.getListCellRendererComponent(list, value, index, isSelected,
				cellHasFocus);
		String columnName = ((TapTableColumn) value).getColumn_name();
		StringBuffer texter = new StringBuffer("<html><p>").append(columnName).append("</html></p>");
    	column.setPreferredSize(new Dimension(150, 28));//25(Server.HAUT) renders a little bit small
		column.setToolTipText(columnName);
		column.setText(texter.toString());
		return column;
	}

}