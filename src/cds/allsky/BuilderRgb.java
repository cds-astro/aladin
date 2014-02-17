package cds.allsky;

import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.FileNotFoundException;

import cds.aladin.Aladin;
import cds.aladin.ColorMap;
import cds.aladin.Coord;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.aladin.PlanImage;
import cds.aladin.PlanImageMosaic;
import cds.aladin.PlanImageRGB;
import cds.allsky.Context.JpegMethod;
import cds.fits.Fits;
import cds.tools.pixtools.CDSHealpix;
import cds.tools.pixtools.Util;

public class BuilderRgb extends Builder {
	private PlanBG[] p;
//	private final Aladin aladin;
//	private BuilderAllsky builderAllsky;
	private String path;
    private int width=-1;
    private double [] blank;
    private double [] bscale;
    private double [] bzero;
    private byte [][] tcm;
    private int [] bitpix;
    private int maxOrder = 100;
    private int missing=-1;
    
    private JpegMethod method;
    private int format;
    
    private int statNbFile;
    private long statSize;
    private long startTime,totalTime;
    
    public BuilderRgb(Context context) {
       super(context);
//       aladin=((ContextGui)context).mainPanel.aladin;
    }
    
    public Action getAction() { return Action.RGB; }
    
    public void run() throws Exception {
       build();       
       if( !context.isTaskAborting() ) {
          (new BuilderAllsky(context)).createAllSkyColor(path,3,Context.MODE[format],64);
       }
       if( !context.isTaskAborting() ) (new BuilderMoc(context)).createMoc(path);
    }
    
    // Demande d'affichage des stats (dans le TabRgb)
    public void showStatistics() {
       context.showRgbStat(statNbFile, statSize, totalTime);
    }
    
    public void validateContext() throws Exception { 
       Object [] plans = ((ContextGui)context).getRgbPlans();
       String path = ((ContextGui)context).getRgbOutput();
       JpegMethod method = ((ContextGui)context).getRgbMethod();
       this.format=((ContextGui)context).getRgbFormat();
       String label=null;
       
       p = new PlanBG[3];
       for( int c=0; c<3; c++ ) {
          if( !(plans[c] instanceof PlanBG) ) continue;
          p[c]=(PlanBG)plans[c];
       }
       this.path = path;
       
       bitpix = new int[3]; 
       for( int c=0; c<3; c++ ) bitpix[c]=0;
       blank = new double[3];
       bzero = new double[3];
       bscale = new double[3];
       tcm = new byte[3][];
       this.method=method;

       // recherche la meilleure r�solution commune
       int frame=-1;
       for( int c=0; c<3; c++) {
          if( p[c]==null ) { missing=c; continue; }
          if( label==null ) label = p[c].label;
          if( frame==-1 ) frame = p[c].getFrameOrigin();
          else if( frame!=p[c].getFrameOrigin() ) {
             context.warning("All components must be used the same HEALPix coordinate system !");
             return;
          }
          
          IndexColorModel cm = ColorMap.getCM(p[c].cmControl[0],p[c].cmControl[1],p[c].cmControl[2],
                false, p[c].typeCM,p[c].transfertFct,p[c].isTransparent());
          tcm[c] = cds.tools.Util.getTableCM(cm, 2);
//          tcm[c] = cds.tools.Util.getTableCM(p[c].getCM(), 2);
 
          context.setProperty(c==0?"red":c==1?"green":"blue",p[c].label+" ["
               +p[c].getPixelMin()+" "+p[c].getPixelMiddle()+" "+p[c].getPixelMax()+" "
               +PlanImage.TRANSFERTFCT[ p[c].transfertFct ]+"]");
          int order = p[c].getMaxFileOrder();
          if( maxOrder > order)  maxOrder = order;
       }
       ((ContextGui)context).mainPanel.clearForms();
       context.setFrame(frame);
       context.setOrder(maxOrder);
       context.setOutputPath(path);
       context.setBitpixOrig(0);
       if( context.label==null || context.label.trim().length()==0 ) context.label="RGB-"+label;
       context.writePropertiesFile();
    }

    private void initStat() { statNbFile=0; statSize=0; startTime = System.currentTimeMillis(); }

    // Mise � jour des stats
    private void updateStat(File f) {
       statNbFile++;
       statSize += f.length();
       totalTime = System.currentTimeMillis()-startTime;
    }
    
    public void build() throws Exception  {
       initStat();
       for( int i=0; i<768; i++ ) {
          if( context.isTaskAborting() ) new Exception("Task abort !");
          // Si le fichier existe d�j� on ne l'�crase pas
          String rgbfile = Util.getFilePath(path,3, i)+Context.EXT[format];
          if( !context.isInMocTree(3, i) ) continue;
          if ((new File(rgbfile)).exists()) continue;
          createRGB(3,i);
          context.setProgressLastNorder3(i);
       }
    }

	// G�n�ration des RGB r�cursivement en repartant du niveau de meilleure r�solution
	// afin de pouvoir utiliser un calcul de m�diane sur chacune des composantes R,G et B
	private Fits [] createRGB(int order, long npix) throws Exception {

	   if( context.isTaskAborting() ) new Exception("Task abort !");

	   // S'il n'existe pas au-moins 2 composantes en fichiers fits, c'est une branche morte
	   int trouve=0;
	   for( int c=0; c<3; c++ ) {
	      if( c==missing ) continue;
	      String file = Util.getFilePath(p[c].getUrl(),order,npix)+".fits";
	      if( new File(file).exists() ) trouve++;
	   }
	   if( trouve==0 ) return null;

	   Fits [] out = null;

	   // Branche terminale
	   if( order==maxOrder ) out = createLeaveRGB(order, npix);

	   // Noeud interm�diaire
	   else {
	      Fits [][] fils = new Fits[4][3];
	      boolean found = false;
	      for( int i=0; i<4; i++ ) {
	         fils[i] = createRGB(order+1,npix*4+i);
	         if( fils[i] != null && !found ) found = true;
	      }
	      if( found ) out = createNodeRGB(fils);
	   }
	   if( out!=null) generateRGB(out, order, npix);
	   return out;
	}


	// G�n�ration d'un noeud par la m�diane pour les 3 composantes
	// (on ne g�n�re pas encore le RGB (voir generateRGB(...))
	private Fits [] createNodeRGB(Fits [][] fils) throws Exception {
       
       Fits [] out = new Fits[3];
       for( int c=0; c<3; c++ ) {
          out[c] = new Fits(width,width,bitpix[c]);
          out[c].setBlank(blank[c]);
          out[c].setBzero(bzero[c]);
          out[c].setBscale(bscale[c]);
       }
       
       for( int dg=0; dg<2; dg++ ) {
          for( int hb=0; hb<2; hb++ ) {
             int quad = dg<<1 | hb;
             int offX = (dg*width)/2;
             int offY = ((1-hb)*width)/2;
             
             for( int c=0; c<3; c++ ) {
                if( c==missing ) continue;
                Fits in = fils[quad]==null ? null : fils[quad][c];
                double p[] = new double[4];
                double coef[] = new double[4];
                
                for( int y=0; y<width; y+=2 ) {
                   for( int x=0; x<width; x+=2 ) {
                      
                      double pix=blank[c];
                      if( in!=null ) {

                          // On prend la moyenne (sans prendre en compte les BLANK)
                         if( method==Context.JpegMethod.MEAN ) {
                            double totalCoef=0;
                            for( int i=0; i<4; i++ ) {
                               int dx = i==1 || i==3 ? 1 : 0;
                               int dy = i>=2 ? 1 : 0;
                               p[i] = in.getPixelDouble(x+dx,y+dy);
                               if( in.isBlankPixel(p[i]) ) coef[i]=0;
                               else coef[i]=1;
                               totalCoef+=coef[i];
                            }
                            if( totalCoef!=0 ) {
                            	pix = 0;
                               for( int i=0; i<4; i++ ) {
                                  if( coef[i]!=0 ) pix += p[i]*(coef[i]/totalCoef);
                               }
                            }
                            
                         // On garde la valeur m�diane (les BLANK seront automatiquement non retenus)
                         } else {

                            double p1 = in.getPixelDouble(x,y);
                            if( in.isBlankPixel(p1) ) p1=Double.NaN;
                            double p2 = in.getPixelDouble(x+1,y);
                            if( in.isBlankPixel(p2) ) p1=Double.NaN;
                            double p3 = in.getPixelDouble(x,y+1);
                            if( in.isBlankPixel(p3) ) p1=Double.NaN;
                            double p4 = in.getPixelDouble(x+1,y+1);
                            if( in.isBlankPixel(p4) ) p1=Double.NaN;

                            if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix=p1;
                            else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix=p2;
                            else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix=p3;
                            else pix=p4;
                         }
                      }

                      out[c].setPixelDouble(offX+(x/2), offY+(y/2), pix);
                   }
                }
             }
          }
       }

       for( int j=0; j<4; j++ ) {
          for( int c=0; c<3; c++ ) {
             if( c==missing ) continue;
             if( fils[j]!=null && fils[j][c]!=null ) fils[j][c].free();
          }
       }
       
       return out;
    }

    // G�n�ration d'une feuille terminale (=> simple chargement des composantes)
	private Fits [] createLeaveRGB(int order, long npix) throws Exception {
	   if( context.isTaskAborting() ) new Exception("Task abort !");
	   Fits[] out =null;
	   out = new Fits[3];

	   // Chargement des 3 (ou �ventuellement 2) composantes
	   for( int c=0; c<3; c++ ) {
	      if( c==missing ) continue;
	      out[c] = new Fits();
	      try {
            out[c].loadFITS( Util.getFilePath( p[c].getUrl(),order, npix)+".fits");
            
         // Un losange d'une des composantes est manquant
         } catch( Exception e ) {
            out[c]=null;
         }

	      // Initialisation des constantes pour cette composante
	      if( out[c]!=null && bitpix[c]==0 ) {
	         bitpix[c]=out[c].bitpix;
	         blank[c]=out[c].blank;
	         bscale[c]=out[c].bscale;
	         bzero[c]=out[c].bzero;
	         if( width==-1 ) width = out[c].width;  // La largeur d'un losange est la m�me qq soit la couleur
	      }
	   }
	   return out;
	}
	
    // g�n�ration du RGB � partir des composantes
    private void generateRGB(Fits [] out, int order, long npix) throws Exception {
       byte [][] pixx8 = new byte [3][];
       
       // Passage en 8 bits pour chaque composante
       for( int c=0; c<3; c++ ) {
          if( c==missing || out[c]==null ) continue;
          pixx8[c] = out[c].toPix8(p[c].getCutMin(),p[c].getCutMax(), tcm[c], 
                format==Context.PNG ? Fits.PIX_255 : Fits.PIX_256);
      }
       
       Fits rgb = new Fits(width,width,0);
       int [] pix8 = new int[3];
       for( int i=width*width-1; i>=0; i-- ) {
          int tot = 0;  // Pour faire la moyenne en cas d'une composante manquante
          for( int c=0; c<3; c++ ) {
             if( c==missing ) continue;
             if( out[c]==null ) pix8[c]=0;
             else {
                pix8[c] = 0xFF & (int)pixx8[c][i];
                tot += pix8[c];
             }
          }
          if( missing!=-1 ) pix8[missing] = tot/2;
          int pix;
          if( tot==0 ) pix=0x00;
          else {
             pix = 0xFF;
             for( int c=0; c<3; c++ ) pix = (pix<<8) | pix8[c];
          }
          rgb.rgb[i]=pix;
       }
       String file="";

       file = Util.getFilePath(path,order, npix)+Context.EXT[format];
       rgb.writeRGBcompressed(file,Context.MODE[format]);
       rgb.free();

       File f = new File(file);
       updateStat(f);
    }
}
