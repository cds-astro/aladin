package cds.aladin;

import static cds.aladin.Constants.REGISTRYPANEL;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

public class DataLabel {
	private String label;
	private String value;
	private String description;
	protected JRadioButton gui;
	
	public DataLabel() {
		// TODO Auto-generated constructor stub
	}
	public DataLabel(String label, String value, String description){
		this.label = label;
		this.value = value;
		if (description!=null && !description.isEmpty()) {
//			this.description = "<html>"+description+"</html>";
			this.description = description;
		}
		
	}
	
	public DataLabel(String label, String value, String description, boolean setui){
		this(label, value, description);
		if (setui) {
			setUi();
		}
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void setAll(String label, String value, String description) {
		this.label = label;
		this.value = value;
		this.description = description;
	}
	
	public void setUi() {
		gui = new JRadioButton(this.label+"  ::"+this.value);
		if (this.description!=null && !this.description.isEmpty()) {
			gui.setToolTipText("<html><p width=\"500\">"+this.description+"</p></html>");
		}
	}
	
	public void setUiActionForTapRegistry() {
		this.gui.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
//				Integer.parseInt(e.getActionCommand());
				Container registryPanel = SwingUtilities.getAncestorNamed(REGISTRYPANEL, gui);
				if (registryPanel instanceof TapFrameServer) {
					TapFrameServer frameServer = (TapFrameServer) registryPanel;
					frameServer.setReload(label);
				}
			}
		});
	}
	
	@Override
	public String toString() {
		StringBuffer toPrint = new StringBuffer();
//		toPrint.append("Selected ID: ").append(selectedId).append(", ")
		toPrint.append("Label: ").append(this.label).append(", ")
		.append("Value: ").append(this.value).append(", ")
		.append("Description: ").append(this.description);
		return toPrint.toString();
	}

}
