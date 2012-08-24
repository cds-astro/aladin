package cds.allsky;

import java.io.File;

import cds.fits.Fits;
import cds.tools.Util;

public class BuilderCheck extends Builder {

   protected BuilderCheck(Context context) {
      super(context);
   }

   private static final String fileIn = "fileIn.log";
   private static final String fileErr = "fileErr.log";
   
   @Override
   public void validateContext() throws Exception {
      // TODO Auto-generated method stub
      validateInput();
   }

   @Override
   public void run() throws Exception {
      context.running("Check your input files...");
      String pathSource = context.getInputPath();
      String readFiles = "", errFiles = "";
      testReadFiles(pathSource, readFiles, errFiles);
      context.done("Read files are listed in : "+fileIn);
      context.done("Erroneous files are listed in : "+fileErr);
   }

   private void testReadFiles(String pathSource, String readFiles, String errFiles) {
      File main = new File(pathSource);

      String currentpath = pathSource;
      String[] list = main.list();
      for (int f = 0 ; f < list.length ; f++) {
         String currentfile = pathSource+Util.FS+list[f];
         File file = new File(currentfile);
         if (file.isDirectory() && !list[f].equals(Constante.SURVEY)) {
//                System.out.println("Look into dir " + currentfile);
          testReadFiles(currentfile, readFiles, errFiles);
          currentpath = pathSource;
         }
         else {
            Fits fitsfile = new Fits();
            try {
               fitsfile.loadFITS(currentfile);
               readFiles += currentfile;
            } catch (Exception e) {
               errFiles += currentfile;
            }
            
         }
      }
   }
   
   @Override
   public Action getAction() {
      // TODO Auto-generated method stub
      return null;
   }

}
