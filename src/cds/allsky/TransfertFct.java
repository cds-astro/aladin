package cds.allsky;

import cds.aladin.PlanImage;

public enum TransfertFct {
	LOG (PlanImage.LOG), SQRT (PlanImage.SQRT), LINEAR (PlanImage.LINEAR), 
	ASINH (PlanImage.ASINH), POW2 (PlanImage.SQR);
	
	private final int code;
	TransfertFct(int i) {
		code = i;
	}
	
	int code() { return code;}
}
