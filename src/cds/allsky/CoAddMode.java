package cds.allsky;

public enum CoAddMode {
	KEEP, OVERWRITE, AVERAGE, REPLACETILE;
	
	public static CoAddMode getDefault() {
		return OVERWRITE;
	}
}
