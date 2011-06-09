package cds.allsky;

import java.awt.image.ColorModel;
import java.io.File;
import java.io.FilenameFilter;

import cds.aladin.Aladin;
import cds.aladin.PlanImage;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

public class JPGBuild implements Runnable {

	double[] cutminmax;
	String dirpath;
	int progress=0;
	float progressFactor;
//	int transfertFct = PlanImage.LINEAR;
	ColorModel cm;
	
	public JPGBuild(double[] cut, final ColorModel cm, /*final int tFct,*/ String path) {
		dirpath=path;
		cutminmax=cut;
//		transfertFct = tFct;
		this.cm = cm;
	}
	
	/**
	 * Lance l'écriture JPG de tous les fichiers FITS trouvés dans la hiérarchie
	 * @param cut
	 * @param dir
	 */
	private void toJPG(double[] cut, final ColorModel cm, /*final int tFct,*/ File dir) {
		File[] children = dir.listFiles(new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				return name.startsWith("Dir") || name.startsWith("Npix");
			}
		});
		for (int i=0; i<children.length; i++) {
			if (children[i].isDirectory()) {
				toJPG(cut,cm,/*tFct,*/children[i]);
				progress+=(int)(100*((i+1)/children.length)*progressFactor);
			}
			else {
				String filename = children[i].getPath();
				filename = filename.substring(0, filename.lastIndexOf("."));
				toJPG(cut, cm, /*tFct,*/ filename);
				if (progressFactor==1)
					progress+=(int)(100.*((i+1.)/children.length));
			}
		}
	}

	/**
	 * Ouvre une image fits et la convertit en JPG au meme endroit
	 * @param cut
	 * @param filename
	 */
	private void toJPG(final double[] cut, final ColorModel cm, /*final int tFct,*/ final String filename) {
		Fits file = new Fits();
		try {
			file.loadFITS(filename+".fits");
			
			// Cut et Ecriture du JPEG 8 bits
			if( cm==null ) file.toPix8(cut[0],cut[1]);
			else file.toPix8(cut[0],cut[1],cm);
			
//			switch (tFct) {
//			case PlanImage.ASINH :
//				file.toPix8ASinH(cut[0],cut[1]); break;
//			case PlanImage.SQRT :
//				file.toPix8Sqrt(cut[0],cut[1]); break;
//			case PlanImage.SQR :
//				file.toPix8Pow(cut[0],cut[1]); break;
//			case PlanImage.LOG :
//				file.toPix8Log(cut[0],cut[1]); break;
//			case PlanImage.LINEAR :
//				file.toPix8(cut[0],cut[1]); break;
//
//			}
			file.writeJPEG(filename+".jpg");
			
		} catch (Exception e) {
			Aladin.trace(3,e.getMessage());
		}
	}
	
	public int getProgress() {
		return progress;
	}

	public synchronized void start(){
		(new Thread(this)).start();
	}
	
	public void run() {
		File dir = new File(dirpath);
		if (dir.isDirectory()) {
			File[] children = dir.listFiles(new FilenameFilter() {
				
				public boolean accept(File dir, String name) {
					return name.startsWith("Norder");
				}
			});
			progressFactor = 1.F/(float)children.length; 
			// pour tous répertoires Norder du répertoire principal
			double fct = 100./children.length;
			for (int i=0; i<children.length; i++) {
				toJPG(cutminmax,cm/*transfertFct*/,children[i]);
				progress = (int) ((i+1)*fct);
			}
			// convertit le Allsky
			toJPG(cutminmax,cm/*transfertFct*/,dirpath+Util.FS+"Norder3"+Util.FS+"Allsky");
		}
		
	}
}