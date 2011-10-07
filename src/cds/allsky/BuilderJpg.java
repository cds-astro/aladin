package cds.allsky;

import java.awt.image.ColorModel;
import java.io.File;
import java.io.FilenameFilter;

import cds.aladin.Aladin;
import cds.aladin.PlanImage;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

/** Construction de la hiérarchie des tuiles JPEG à partir des tuiles FITS de plus bas
 * niveau. La méthode employée est la médiane pour passer des 4 pixels de niveau
 * inférieur au pixel de niveau supérieur (fait disparaitre peu à peu les étoiles
 * faibles). Le passage en 8 bits se fait, soit par une table de couleur (cm) fournie,
 * soit par une intervalle (cut[]).
 * @author Anaïs Oberto & Pierre Fernique
 */
public class BuilderJpg implements Runnable {

	private double[] cutminmax;
	private int maxOrder;
	private String dirpath;
	private int progress=0;
	private float progressFactor;
	private byte [] tcm;
	private int bitpix;
	private int width;
	private double blank,bscale,bzero;
	private Context context;
	
	private int statNbFile;
	private long statSize;
	private long startTime,totalTime;
	private long statLastShowTime;
	
	static public final int MEDIANE = 0;
	static public final int MOYENNE = 1;
	private int method;

	/**
	 * Création du générateur JPEG.
	 * @param cut borne de l'intervalle pour le passage en 8 bits (uniquement si cm==null)
	 * @param cm table des couleurs pour le passage en 8 bits (prioritaire sur cut), 
	 * @param context
	 */
	public BuilderJpg(double[] cut, final ColorModel cm, int method, Context context) {
	   this.context = context;
	   dirpath=context.getOutputPath();
	   maxOrder = getMaxOrder();
	   bitpix = context.getBitpix();
	   blank = context.getBlankOrig();
	   width=Constante.SIDE;
	   bscale=context.getBScaleOrig();
	   bzero=context.getBZeroOrig();
	   cutminmax=cut;
	   this.tcm = cm==null ? null : cds.tools.Util.getTableCM(cm,2);
	   this.method=method;
	}
	
	private void initStat() { statNbFile=0; statSize=0; statLastShowTime=-1; startTime = System.currentTimeMillis(); }
	
    // Mise à jour des stats
    private void updateStat(File f) {
       statNbFile++;
       statSize += f.length();
       long t = System.currentTimeMillis();
       if( t-statLastShowTime < 1000 ) return;
       totalTime = System.currentTimeMillis()-startTime;
       statLastShowTime=t;
       showStat();
    }
    
    // Demande d'affichage des stats
    private void showStat() {
       context.showJpgStat(statNbFile, statSize, totalTime);
    }

    // Détermine le niveau terminal en recherchant les répertoires Norder
    private int getMaxOrder() {
       for( int order=3; true; order++ ) {
          File f = new File(dirpath+Util.FS+"Norder"+order);
          if( !f.isDirectory() ) return order-1;
       }
    }
    
	public int getProgress() {
		return progress;
	}

	public synchronized void start(){
		(new Thread(this)).start();
	}
	
	private boolean stopped = false;
	
	public void run() {
	   try {
	      initStat();
	      progressFactor = 100f/768f;
	      progress=0;
	      for( int i=0; i<768; i++ ) {
	         createJpg(dirpath,3,i);
	         progress = (int)(i*progressFactor);
	      }
	      (new BuilderAllsky(context,-1)).createAllSkyJpgColor(dirpath,3,64);
	      progress=100;
	   } catch( Exception e ) {
	      e.printStackTrace();
	   }
	}
	
	/** Construction récursive de la hiérarchie des tuiles JPEG à partir des tuiles FITS
	 * de plus bas niveau. La méthode employée est la médiane sur les 4 pixels de niveau inférieurs */
    private Fits createJpg(String path,int order, long npix ) throws Exception {
        String file = Util.getFilePath(path,order,npix);
        
        
        // si le process a été arrêté on essaie de ressortir au plus vite
        if( stopped ) return null;
        
        // S'il n'existe pas le fits, c'est une branche morte
        if( !new File(file+".fits").exists() ) return null;
        
        Fits out = null;
        if( order==maxOrder ) out = createLeaveJpg(file);
        else {
           Fits fils[] = new Fits[4];
           boolean found = false;
           for( int i =0; !stopped && i<4; i++ ) {
              fils[i] = createJpg(path,order+1,npix*4+i);
              if (fils[i] != null && !found) found = true;
           }
           if( found ) out = createNodeJpg(fils);
        }
        if( out!=null ) {
           if( tcm==null ) out.toPix8(cutminmax[0],cutminmax[1]);
           else out.toPix8(cutminmax[0],cutminmax[1],tcm);
           out.writeJPEG(file+".jpg");
           
           File f = new File(file+".jpg");
           updateStat(f);
        }
        return out;
    }
    
    /** Construction d'une tuile terminale. De fait, simple chargement
     * du fichier FITS correspondant. */
    private Fits createLeaveJpg(String file)  {
       Fits out = null;
       try {
          out = new Fits();
          out.loadFITS(file+".fits");
      } catch( Exception e ) { out=null; }
      return out;
    }

	/** Construction d'une tuile intermédiaire à partir des 4 tuiles filles */
    private Fits createNodeJpg(Fits fils[]) throws Exception {
       Fits out = new Fits(width,width,bitpix);
       out.setBlank(blank);
       out.setBscale(bscale);
       out.setBzero(bzero);
       
       Fits in;
       double p[] = new double[4];
       double coef[] = new double[4];
       
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

                       // On prend la moyenne (sans prendre en compte les BLANK)
                      if( method==MOYENNE ) {
                         double totalCoef=0;
                         for( int i=0; i<4; i++ ) {
                            int dx = i==1 || i==3 ? 1 : 0;
                            int dy = i>=2 ? 1 : 0;
                            p[i] = in.getPixelDouble(x+dx,y+dy);
                            if( out.isBlankPixel(p[i]) ) coef[i]=0;
                            else coef[i]=1;
                            totalCoef+=coef[i];
                         }
                         if( totalCoef!=0 ) {
                            for( int i=0; i<4; i++ ) {
                               if( coef[i]!=0 ) pix += p[i]*(coef[i]/totalCoef);
                            }
                         }
                         
                      // On garde la valeur médiane (les BLANK seront automatiquement non retenus)
                      } else {

                         double p1 = in.getPixelDouble(x,y);
                         double p2 = in.getPixelDouble(x+1,y);
                         double p3 = in.getPixelDouble(x,y+1);
                         double p4 = in.getPixelDouble(x+1,y+1);

                         if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
                         else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
                         else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
                         else pix=p4;
                      }
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