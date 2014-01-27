
import java.util.Enumeration;
import java.util.Hashtable;

import cds.aladin.*;

public class ImageCreationPlug extends AladinPlugin {
   
   public String menu() { return "CreationPlug"; }
   public String description()   {
      return "PLUGIN TUTORIAL:\n" +
             "This plugin is an example of an image plane creation.\n" +
             "It will create an image and will provide an astrometrical " +
             "solution for it.";
   }   
   public String author()        { return "Pierre Fernique [CDS]"; }
   public String version()       { return "1.0 - nov 2013"; }
   public String url() { return "http://aladin.u-strasbg.fr/java/Plugins/ImageCreationPlug.java"; }
   
   /*
    * 1) Create a new image in the Aladin stack and gets its corresponding
    *    AladinData object.
    * 2) Generate a pixel array and set them in the new plane
    * 3) Generate a Fits header and set it in the new plan
    */
   public void exec() {
      try { 
         createImage("x1",100,150,100,0.33,         10,  10, 3);
         createImage("x2",120,170,100,0.33,         10,  20, 1);
//         createImage(150,200,true);
      } catch( Exception e ) { e.printStackTrace(); }
   }
   
   public void createImage(String name,int crpix1,int crpix2,
         double bzero,double bscale,double blank,double skyval,double exptime) throws Exception {
      AladinData ad = aladin.createAladinData(name);

      int width=200,height=400;
      double [][] pix = new double[width][400];
      for( int x=0; x<width; x++ ) {
         for( int y=0; y<height; y++ ) {
            double v;
            if( x<30 || x>width-30 ) v=30;
            else if( y<50 || y>height-50 ) v=50;
            else if( (x-width/2)*(x-width/2) + (y-height/2)*(y-height/2) <30 ) v=blank;
            else v=80;
            
            if( v!=blank && (skyval!=0 || exptime!=1) ) {
               double vv = v*bscale + bzero;
               vv = vv*exptime + skyval;
               v = (vv - bzero) / bscale;
            }
            pix[x][y]= v;
         }
      }
      ad.setPixels(pix,-32);

      // Calibration 
      System.out.println("----------Création via setFitsHeader(String) -----------");
      String header = 
         "SIMPLE  = T\n"+
         "BITPIX  = -32\n"+
         "NAXIS   = 2\n"+
         "NAXIS1  = "+width+"\n"+
         "NAXIS2  = "+height+"\n"+
         "CRPIX1  = "+crpix1+"\n"+
         "CRPIX2  = "+crpix2+"\n"+
         "CRVAL1  = 83.63310542835717\n"+
         "CRVAL2  = 22.014486753213667\n"+
         "CTYPE1  = RA---TAN\n"+
         "CTYPE2  = DEC--TAN\n"+
         "CD1_1   = -2.8004558788238224E-4\n"+
         "CD1_2   = -3.078969511615841E-6\n"+
         "CD2_1   = -3.078969511615841E-6\n"+
         "CD2_2   = 2.8004558788238224E-4\n"+
         "RADECSYS= FK5\n";

      if( bzero!=0 )             header+="BZERO   = "+bzero+"\n";
      if( bscale!=1 )            header+="BSCALE  = "+bscale+"\n";
      if( !Double.isNaN(blank) ) header+="BLANK   = "+blank+"\n";
      if( skyval!=0 )            header+="SKYVAL  = "+skyval+"\n";
      if( exptime!=1 )           header+="EXPTIME = "+exptime+"\n";

      ad.setFitsHeader(header);
      Hashtable h = ad.seeFitsKeys();
      Enumeration e = h.keys();
      while( e.hasMoreElements() ) {
         String k = (String)e.nextElement();
         System.out.println(k+" = ["+(String)h.get(k)+"]");
      }

      //         System.out.println("--------- Modif directe de CRPIX1, NAXIS3 et ajout de TOTO Hashtable ad.seeFitsKeys() --------");
      //         h.put("CRPIX1","200");
      //         h.put("TOTO","bidule");
      //         h.put("NAXIS","3");
      //         ad.fitsKeysModified();
      //         h = ad.seeFitsKeys();
      //         e = h.keys();
      //         while( e.hasMoreElements() ) {
      //            String k = (String)e.nextElement();
      //            System.out.println(k+" = ["+(String)h.get(k)+"]");
      //         }         
      //         System.out.println("-----------Visu header FITS final via ad.getFitsHeader() ---------------------");
      //         System.out.println(ad.getFitsHeader());
   } 
}