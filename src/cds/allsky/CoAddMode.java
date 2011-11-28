package cds.allsky;

public enum CoAddMode {
	KEEP, OVERWRITE, AVERAGE, REPLACEALL, KEEPALL;
	
	public static CoAddMode getDefault() {
		return OVERWRITE;
	}
}
