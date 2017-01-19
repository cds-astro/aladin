package cds.aladin;

import static cds.aladin.Constants.SPACESTRING;

import javax.swing.JLabel;

public class PositionConstraint extends WhereGridConstraint{
	private static final long serialVersionUID = 1L;
	
	private static String POSQuery = "CONTAINS(POINT('ICRS', %1$s, %2$s), CIRCLE('ICRS', %3$s, %4$s, %5$s)) = 1";
	private String raConstraint;
	private String decConstraint;
	private String radiusConstraint;
	private String selectedDecColumnName;
	private String selectedRaColumnName;
	
	public PositionConstraint(ServerTap serverTap) {
		// TODO Auto-generated constructor stub
		super(serverTap);
	}
	
	public PositionConstraint(ServerTap serverTap, String raConstraint, String decConstraint, String radiusConstraint, String selectedRaColumnName, String selectedDecColumnName) {
		// TODO Auto-generated constructor stub
		super(serverTap, new JLabel("Ra= "+raConstraint), new JLabel("Dec= "+decConstraint), new JLabel("Radius= "+radiusConstraint));
		this.raConstraint = raConstraint;
		this.decConstraint = decConstraint;
		this.radiusConstraint = radiusConstraint;
		this.selectedRaColumnName = selectedRaColumnName;
		this.selectedDecColumnName = selectedDecColumnName;
	}
	
	@Override
	public String getAdqlString() throws Exception{
		StringBuffer whereClause = new StringBuffer();
		if (this.andOrOperator != null) {
			whereClause.append(this.andOrOperator.getSelectedItem()).append(SPACESTRING);
		}
		whereClause.append(String.format(POSQuery, this.selectedRaColumnName, this.selectedDecColumnName,
				this.raConstraint, this.decConstraint, this.radiusConstraint)).append(SPACESTRING);

		return whereClause.toString();
	}

	public String getSelectedRaColumnName() {
		return selectedRaColumnName;
	}

	public void setSelectedRaColumnName(String selectedRaColumnName) {
		this.selectedRaColumnName = selectedRaColumnName;
	}

	public String getSelectedDecColumnName() {
		return selectedDecColumnName;
	}

	public void setSelectedDecColumnName(String selectedDecColumnName) {
		this.selectedDecColumnName = selectedDecColumnName;
	}

	
}
