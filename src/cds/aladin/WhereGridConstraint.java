package cds.aladin;

import static cds.aladin.Constants.AND;
import static cds.aladin.Constants.REMOVEWHERECONSTRAINT;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Class is a representation of the gui of the where constraint additition.
 * This is not a where constraint model class
 * @author chaitra
 *
 */
public abstract class WhereGridConstraint extends JPanel{

	private static final long serialVersionUID = 1L;
	protected static final String[] operators = { "=", "!=", "<", ">", "<=", ">=", "BETWEEN", "NOT BETWEEN", "IS",
			"IS NOT", "LIKE", "NOT LIKE" };
	protected static final String defaultValue = "IS NOT NULL";
	protected static final String[] andOrOptions = { AND, "OR" };
	protected static final String DELETEBUTTON_TOOLTIP = "Click to delete this constraint";
	
	protected ServerTap serverTap;
	JComboBox<String> andOrOperator;
	JComponent firstGridComponent;
	JComponent secondGridComponent;
	JComponent thirdGridComponent;
	JButton removeButton;
	
	
	public WhereGridConstraint(ServerTap serverTap) {
		// TODO Auto-generated constructor stub
		this.serverTap = serverTap;
		this.andOrOperator = new JComboBox(andOrOptions);
		this.andOrOperator.addItemListener(new ItemListener() {
	         public void itemStateChanged(ItemEvent e) {
	        	 write();
	          }
	       });
		this.removeButton = new JButton(new ImageIcon(Aladin.aladin.getImagette("delete_button.png")));
		this.removeButton.setToolTipText(DELETEBUTTON_TOOLTIP);
		this.removeButton.setActionCommand(REMOVEWHERECONSTRAINT);
	}
	
	public WhereGridConstraint(ServerTap serverTap, JComponent firstGridComponent, JComponent secondGridComponent, JComponent thirdGridComponent) {
		// TODO Auto-generated constructor stub
		this(serverTap);
		this.firstGridComponent = firstGridComponent;
		this.secondGridComponent = secondGridComponent;
		this.thirdGridComponent = thirdGridComponent;
	}
	
	public WhereGridConstraint(ServerTap serverTap, Vector columnNames) {
		// TODO Auto-generated constructor stub
		this(serverTap);
		this.firstGridComponent = new JComboBox(columnNames);
		this.secondGridComponent = new JTextField(6);
		this.thirdGridComponent = new JComboBox(operators);
	}
	
	public void addWhereConstraints() {
		add(this.andOrOperator);
		add(this.firstGridComponent);
		add(this.secondGridComponent);
		add(this.thirdGridComponent);
		add(this.removeButton);
	}
	
	public void removeAndOr() {
		this.remove(this.andOrOperator);
	}
	
	public void write() {
		serverTap.writeQuery();
	}
	/**
	 * Method to remove the leading AND/OR operator fromthe first where constraint
	 * @param whereClausesPanel
	 */
	public static void removeFirstAndOrOperator(JPanel whereClausesPanel) {
		if (whereClausesPanel.getComponentCount() > 0) {
			WhereGridConstraint whereGridConstraint = (WhereGridConstraint) whereClausesPanel.getComponent(0);
			if (whereGridConstraint.andOrOperator != null) {
				whereGridConstraint.remove(whereGridConstraint.andOrOperator);
				whereGridConstraint.andOrOperator = null;
			}
		}
	}
	
	public abstract String getAdqlString() throws Exception;

	public JComboBox<String> getAndOrOperator() {
		return andOrOperator;
	}

	public void setAndOrOperator(JComboBox<String> andOrOperator) {
		this.andOrOperator = andOrOperator;
	}

	public JComponent getFirstGridComponent() {
		return firstGridComponent;
	}

	public void setFirstGridComponent(JComponent firstGridComponent) {
		this.firstGridComponent = firstGridComponent;
	}

	public JComponent getSecondGridComponent() {
		return secondGridComponent;
	}

	public void setSecondGridComponent(JComponent secondGridComponent) {
		this.secondGridComponent = secondGridComponent;
	}

	public JComponent getThirdGridComponent() {
		return thirdGridComponent;
	}

	public void setThirdGridComponent(JComponent thirdGridComponent) {
		this.thirdGridComponent = thirdGridComponent;
	}

	public JButton getRemoveButton() {
		return removeButton;
	}

	public void setRemoveButton(JButton removeButton) {
		this.removeButton = removeButton;
	}

}
