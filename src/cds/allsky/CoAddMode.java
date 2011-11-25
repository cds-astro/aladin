package cds.allsky;

public enum CoAddMode {
	KEEP, OVERWRITE, AVERAGE, REPLACE, KEEPCELL;
	
	public static CoAddMode getDefault() {
		return OVERWRITE;
	}
}
