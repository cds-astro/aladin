package cds.aladin;

public interface IMListener {
	short PROCESSING = 1;
	short ERROR = 0;
	short LOWMEMORY = -1;
	
	public void progressStatusChange(short status);
	
	public void checkProceedAction(long nbpoints) throws Exception;
}
