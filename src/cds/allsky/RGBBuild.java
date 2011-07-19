package cds.allsky;

import java.io.File;
import java.io.FileNotFoundException;

import cds.aladin.Aladin;
import cds.aladin.Calib;
import cds.aladin.Coord;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.aladin.PlanImageRGB;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

public class RGBBuild implements Runnable {
	int progress;
	PlanBG[] plans;
	private final Aladin aladin;
	private SkyGenerator sg;
	private String output;

	
	public RGBBuild(Aladin aladin, Object[] p, String outpath) {
		this.aladin = aladin;
		sg = new SkyGenerator();
		plans = new PlanBG[p.length];
		output = outpath;
		for (int i=0; i<p.length; i++) {
			if (p[i] != null) {
				plans[i] = (PlanBG)p[i];
			}
		}
	}

	public int getProgress() {
		return progress;
	}

	public synchronized void start(){
		(new Thread(this)).start();
	}
	
//    private String ext= ".fits";
    private String ext= ".jpg";
	
	
	public void run() {

		PlanBG planBG = null;
		// Les chemins vers les plans
		String pathRouge = null;
		String pathVert = null;
		String pathBleu = null;
		// Utilise le minmax des plans
		double[] mmRouge = new double[2];
		double[] mmVert = new double[2];
		double[] mmBleu = new double[2];
		for (int i=0; i<plans.length; i++) {
			if (plans[i] != null) {
				planBG = plans[i];
				if (i==0) {
					pathRouge = planBG.getUrl();
					mmRouge[0] = planBG.getPixelMin();
					mmRouge[1] = planBG.getPixelMax();
				}
				else if (i==1) {
					pathVert = planBG.getUrl();
					mmVert[0] = planBG.getPixelMin();
					mmVert[1] = planBG.getPixelMax();
				}
				else if (i==2) {
					pathBleu = planBG.getUrl();
					mmBleu[0] = planBG.getPixelMin();
					mmBleu[1] = planBG.getPixelMax();
				}
			}
		}
		
		// recherche la meilleure résolution commune
		// -> sinon, on risque d'avoir seulement une couleur en zoomant
		int orderLimit = 100;
		for (int i=0; i<plans.length; i++) {
			planBG = plans[i];
			if (planBG != null && orderLimit > planBG.getMaxHealpixOrder()-DBBuilder.ORDER) {
				orderLimit = planBG.getMaxHealpixOrder()-DBBuilder.ORDER;
			}
		}
		
		// pour chaque fichier fits de l'arborescence
		for (int order = 3 ; order <= orderLimit ; order++) {
		   long npixmax = cds.tools.pixtools.Util.getMax(order)+1;
		   
		   for (int npix = 0 ; npix < npixmax ; npix++) {
		      // va chercher le meme fichier dans chacune des 2/3 arboresences
		      String fRouge = (pathRouge!=null)?cds.tools.pixtools.Util.getFilePath(pathRouge, order, npix)+ext:null;
		      String fVert = (pathVert!=null)?cds.tools.pixtools.Util.getFilePath(pathVert, order, npix)+ext:null;
		      String fBleu = (pathBleu!=null)?cds.tools.pixtools.Util.getFilePath(pathBleu, order, npix)+ext:null;

		      if (fRouge != null && !(new File(fRouge)).exists()) fRouge=null;
		      if (fVert != null && !(new File(fVert)).exists()) fVert=null;
		      if (fBleu != null && !(new File(fBleu)).exists()) fBleu=null;

		      if (fRouge == null && fVert == null && fBleu == null) continue;

		      // combinaison couleur classique Aladin
		      PlanImageRGB rgb;
		      try {
		         rgb = new PlanImageRGB(aladin, fRouge,mmRouge,fVert,mmVert,fBleu,mmBleu);
		         String pathRGB = cds.tools.pixtools.Util.getFilePath(output, order, npix)+".jpg";
		         (new File(pathRGB.substring(0, pathRGB.lastIndexOf(Util.FS)))).mkdirs();
		         aladin.save.saveImageColor(pathRGB, rgb, 2);
		      } catch (FileNotFoundException e) {
		         Aladin.trace(3, e.getMessage());
		      } catch (Exception e) {
		         Aladin.trace(3, e.getMessage());
		      }
		      progress = (int) (100*((float)npix/npixmax)/(1+orderLimit-3));
		      if (order == 3 && npix%100==0)
		         preview(output,npix);
		   }
		}
		preview(output,0);
		progress = 100;
	}

	/** Création/rafraichissemnt d'un allsky (en l'état) et affichage */
	void preview(String path, int last) {
	   try {
          try {
        	  sg.createAllSkyJpgColor(path,3,64);
          } catch (Exception e) {
        	  Aladin.trace(3,e.getMessage());
          }
          
          Plan planPreview = aladin.calque.getPlan("MySkyColor");
          if( planPreview==null || planPreview.isFree() ) {
             double[] res = CDSHealpix.pix2ang_nest(cds.tools.pixtools.Util.nside(3), last);
             double[] radec = CDSHealpix.polarToRadec(new double[] {res[0],res[1]});
             radec = Calib.GalacticToRaDec(radec[0],radec[1]);
             int n = aladin.calque.newPlanBG(path, "=MySkyColor", Coord.getSexa(radec[0],radec[1]), "30" );
             aladin.trace(4,"RGBGuild: Create MySky");
             planPreview = aladin.calque.getPlan(n);
          } else {
             ((PlanBG)planPreview).forceReload();
             aladin.calque.repaintAll();
             aladin.trace(4,"RGBGuild: Create MySky");
             
          }
      } catch( Exception e ) {e.printStackTrace(); }
	}
		
}
