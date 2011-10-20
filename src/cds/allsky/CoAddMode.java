package cds.allsky;

public enum CoAddMode {
	KEEP, OVERWRITE, AVERAGE, REPLACE;
	
	public static CoAddMode getDefault() {
		return OVERWRITE;
	}
}
