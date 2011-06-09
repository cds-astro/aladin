package cds.allsky;

import java.io.File;

public class ExportThread implements Runnable {
	String outfile;
	AllskyPanel allsky;
	int progress=0;
	
	public ExportThread(AllskyPanel allsky, String filename) {
		this.allsky=allsky;
		outfile = filename;
	}

	public int getProgress() {
		File f = new File(outfile);
		if (!f.exists())
			return 0;
		long size = f.length()/1024/1024;
		// la taille d'un fichier avec nside=4096 et bitpix=-32 est 768M
		long sizeFin = 4096*4096*12*(Math.abs(allsky.getBitpix()/8))/1024/1024;
		return (int) (100*size/sizeFin);
	}

	public synchronized void start(){
		(new Thread(this)).start();
	}
	
	public void run() {
		File f = new File(outfile);
		f.delete();
		allsky.export(outfile);
	}

}
