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
	int maxOrder;
	String dirpath;
	int progress=0;
	float progressFactor;
	ColorModel cm;
	int bitpix;
	int width;
	double blank,bscale,bzero;

	public JPGBuild(double[] cut, final ColorModel cm, AllskyPanel allsky) {
	   dirpath=allsky.getOutputPath();
	   maxOrder = allsky.getOrder();
	   bitpix = allsky.getBitpix();
	   blank = allsky.getBlank();
	   width=DBBuilder.SIDE;
	   double bb[] = allsky.getBScaleBZero();
	   bscale=bb[0];
	   bzero=bb[1];
	   cutminmax=cut;
	   this.cm = cm;
	}
	
	/**
	 * Lance l'écriture JPG de tous les fichiers FITS trouvés dans la hiérarchie
	 * @param cut
	 * @param dir
	 */
//	private void toJPG(double[] cut, final ColorModel cm, /*final int tFct,*/ File dir) {
//		File[] children = dir.listFiles(new FilenameFilter() {
//			
//			public boolean accept(File dir, String name) {
//				return name.startsWith("Dir") || name.startsWith("Npix");
//			}
//		});
//		for (int i=0; i<children.length; i++) {
//			if (children[i].isDirectory()) {
//				toJPG(cut,cm,/*tFct,*/children[i]);
//				progress+=(int)(100*((i+1)/children.length)*progressFactor);
//			}
//			else {
//				String filename = children[i].getPath();
//				filename = filename.substring(0, filename.lastIndexOf("."));
//				toJPG(cut, cm, /*tFct,*/ filename);
//				if (progressFactor==1)
//					progress+=(int)(100.*((i+1.)/children.length));
//			}
//		}
//	}

	/**
	 * Ouvre une image fits et la convertit en JPG au meme endroit
	 * @param cut
	 * @param filename
	 */
//	private void toJPG(final double[] cut, final ColorModel cm, final String filename) {
//		Fits file = new Fits();
//		try {
//			file.loadFITS(filename+".fits");
//			
//			// Cut et Ecriture du JPEG 8 bits
//			if( cm==null ) file.toPix8(cut[0],cut[1]);
//			else file.toPix8(cut[0],cut[1],cm);
//			
//			file.writeJPEG(filename+".jpg");
//			
//		} catch (Exception e) {
//			Aladin.trace(3,e.getMessage());
//		}
//	}
	
	public int getProgress() {
		return progress;
	}

	public synchronized void start(){
		(new Thread(this)).start();
	}
	
//	public void run() {
//		File dir = new File(dirpath);
//		if (dir.isDirectory()) {
//			File[] children = dir.listFiles(new FilenameFilter() {
//				
//				public boolean accept(File dir, String name) {
//					return name.startsWith("Norder");
//				}
//			});
//			progressFactor = 1.F/(float)children.length; 
//			// pour tous répertoires Norder du répertoire principal
//			double fct = 100./children.length;
//			for (int i=0; i<children.length; i++) {
//				toJPG(cutminmax,cm/*transfertFct*/,children[i]);
//				progress = (int) ((i+1)*fct);
//			}
//			// convertit le Allsky
//			toJPG(cutminmax,cm/*transfertFct*/,dirpath+Util.FS+"Norder3"+Util.FS+"Allsky");
//		}
//	}
	
	
	private boolean stopped = false;
	
	public void run() {
	   try {
	      progressFactor = 100f/768f;
	      progress=0;
	      for( int i=0; i<768; i++ ) {
	         createJpg(dirpath,3,i);
	         progress = (int)(i*progressFactor);
	      }
	      (new SkyGenerator()).createAllSkyJpgColor(dirpath,3,64);
	      progress=100;
	   } catch( Exception e ) {
	      e.printStackTrace();
	   }
	}
	
    Fits createJpg(String path,int order, long npix ) throws Exception {
        String file = Util.getFilePath(path,order,npix);
        
        // si le process a été arrêté on essaie de ressortir au plus vite
        if( stopped ) return null;
        
        // S'il n'existe pas le fits, c'est une branche morte
        if( !new File(file+".fits").exists() ) return null;
        
        Fits out = null;
        if( order==maxOrder ) {
           out = new Fits();
           out.loadFITS(file+".fits");    
        } else {
           Fits fils[] = new Fits[4];
           boolean found = false;
           for( int i =0; !stopped && i<4; i++ ) {
              fils[i] = createJpg(path,order+1,npix*4+i);
              if (fils[i] != null && !found) found = true;
           }
           if( found ) out = createNodeJpg(fils);
        }
        if( out!=null ) {
           if( cm==null ) out.toPix8(cutminmax[0],cutminmax[1]);
           else out.toPix8(cutminmax[0],cutminmax[1],cm);
           out.writeJPEG(file+".jpg");
        }
        return out;
    }

	
    Fits createNodeJpg(Fits fils[]) throws Exception {
       Fits out = new Fits(width,width,bitpix);
       out.setBlank(blank);
       out.setBscale(bscale);
       out.setBzero(bzero);
       
       Fits in;
       for( int dg=0; dg<2; dg++ ) {
          for( int hb=0; hb<2; hb++ ) {
             int quad = dg<<1 | hb;
             in = fils[quad];
             int offX = (dg*width)/2;
             int offY = ((1-hb)*width)/2;
             
             for( int y=0; y<width; y+=2 ) {
                for( int x=0; x<width; x+=2 ) {
                   
                   double pix=blank;
                   if( in!=null ) {
                      double p1 = in.getPixelDouble(x,y);
                      double p2 = in.getPixelDouble(x+1,y);
                      double p3 = in.getPixelDouble(x,y+1);
                      double p4 = in.getPixelDouble(x+1,y+1);

                      // On garde la valeur médiane
                      if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
                      else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
                      else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
                      else pix=p4;
                   }
                   out.setPixelDouble(offX+(x/2), offY+(y/2), pix);
                }
             }
          }
       }

       for( int i=0; i<4; i++ ) {
          if( fils[i]!=null ) fils[i].free();
       }
       
       return out;
    }
}