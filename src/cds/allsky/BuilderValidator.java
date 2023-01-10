 // Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.allsky;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import cds.fits.Fits;
import cds.fits.HeaderFits;
import cds.tools.pixtools.Util;

/** Validation du code de Hipsgen
 * @author Pierre Fernique [CDS]
 * @version 1.0 - Decembre 2022
 */
public class BuilderValidator extends Builder {
   static private int SUFFIXE = 0;
   
   static private boolean ALLTEST=true;
   
   static private int OUTPUTBITPIX= 16;
   static private int INPUTBITPIX = -32;
   static private double BLANK=0;
   static private double RA=90;
   static private double DEC=41;
   static private long GLOBALDATASUMARITHM = 3199876792L;
   static private long GLOBALDATASUM = 3149592267L;
   static private long TIMETILEREF = 154158L;
   
   String path;
   String input,output;
   long globalDatasum=-1L;
   long timetimeFitsTiles=-1L;
   long timetimePngTiles=-1L;

   public BuilderValidator(Context context) {
      super(context);
   }

   public Action getAction() { return Action.VALIDATOR; }
   
   private Builder execute(Action a) throws Exception {
      Builder b=Task.validator(context,a);
      if( context.isTaskAborting() ) throw new Exception("Aborting");
      return b;
   }
   
   public void run() throws Exception {
      path = context.getOutputPath();
      
//      validate("fits");
      validate("png");
//      validateArith();
      
      
//      validateTrim();
      
      if( ALLTEST ) {
         context.valid("FINAL REPORT");
         if( globalDatasum!=GLOBALDATASUM ) context.error("Global DATASUM not matching => Hips modifications");
         else context.info("The HiPS is conformed");
         context.info("TimeFitsTile:" + timetimeFitsTiles);
         if( (timetimeFitsTiles-TIMETILEREF)>0 && timetimeFitsTiles-TIMETILEREF>0.5*TIMETILEREF )
            context.warning("Fits tile generation seems to be slower than before ("+getFct(timetimeFitsTiles,TIMETILEREF)+")");
         else if(  (TIMETILEREF-timetimeFitsTiles)>0 && TIMETILEREF-timetimeFitsTiles>0.5*TIMETILEREF )
            context.info("Fits tile generation seems to be faster than before ("+getFct(TIMETILEREF,timetimeFitsTiles)+")");
      }
   }
   
   public void validateTrim() throws Exception {
      input = path+Util.FS+"DataTrim";
      cds.tools.Util.deleteDir(new File(input));
      cds.tools.Util.createPath(input);
      
      Fits f = new Fits(512,512,-32);
      f.initBlank();
      for( int y=100; y<200; y++ ) {
         for( int x=150; x<250; x++ ) {
            f.setPixelDouble(x, y, x+y);
         }
      }
      String filename = input+Util.FS+"file1.fits";
      String datasum = f.addDataSum();
      f.writeFITS(filename);
      
      Fits trim = f.trimFactory();
      String filetrim = input+Util.FS+"trim1.fits";
      trim.writeFITS(filetrim);
      
      Fits f1 = new Fits(filetrim);
      String filename2 = input+Util.FS+"file2.fits";
      f1.writeFITS(filename2);
      String datasum1 = f1.addDataSum();
      if( !datasum.equals(datasum1) ) System.out.println("Les pixels ne sont pas identiques");
      
      
   }
   
   public void validateArith() throws Exception {
      context.valid("Arithmetic HiPS generation...");
      String fmt="fits";
      context.reset();
      
      output = path+Util.FS+"HipsA";
      context.valid("Generation HipsADD in "+output+"....");
      input = path+Util.FS+"DataAdd";
      context.setInputPath(input);
      cds.tools.Util.deleteDir(new File(input));
      cds.tools.Util.createPath(input);
      createImg(input,fmt,1000,500,RA,DEC,100,false);
      createImg(input,fmt,1000,500,RA+0.2,DEC+0.2,200,false);
      
      context.setOutputPath(output);
      cds.tools.Util.deleteDir(new File(output));
      context.setPixelCut("0 300");
      context.setMode("add");
      execute(Action.INDEX);
      context.setBitpix(OUTPUTBITPIX);
      execute(Action.TILES);
      
      context.reset();
      output = path+Util.FS+"HipsTime";
      context.valid("Generation HipsTime in "+output+"....");
      input = path+Util.FS+"DataTime";
      context.setInputPath(input);
      cds.tools.Util.deleteDir(new File(input));
      cds.tools.Util.createPath(input);
      createImg(input,fmt,1000,1000,RA-0.5,DEC,3,false);
      createImg(input,fmt,1000,1000,RA-0.5+0.1,DEC+0.1,2,false);
     
      context.setOutputPath(output);
      cds.tools.Util.deleteDir(new File(output));
      context.setMode("add");
      context.setPixelCut("0 5");
      execute(Action.INDEX);
      context.setBitpix(OUTPUTBITPIX);
      execute(Action.TILES);
      
      context.reset();
      input = path+Util.FS+"HipsTime";
      context.setInputPath(input);
      output = path+Util.FS+"HipsA";
      context.setOutputPath(output);
      context.valid("Concat "+input+" in "+output+" by division....");
      context.setMode("div");
      execute(Action.CONCAT);
      
//      checkFits(Util.getFilePath(output,7,27304)+".fits", 
//            new double[][] {{254,248,300},{254,249,150},{254,247,200},{255,248,100}
//            ,{381,116,Double.NaN},} );
      
      context.flagGlobalDataSum=true;
      BuilderCheckDataSum b = (BuilderCheckDataSum)execute(Action.CHECKDATASUM);
      globalDatasum = b.getDataGlobalDataSum();
      if( GLOBALDATASUMARITHM!=globalDatasum ) {
         context.error("Global DATASUM not matching !");
         throw new Exception("Global DATASUM not matching !");
      }

      context.valid("Arithmetic HiPS OK");
 }
   
   public void validate(String fmt) throws Exception {
      boolean flagColor = !fmt.equals("fits");
      
      // Creation et verification d'un premier HiPS
      context.reset();
      createProgenitors(fmt);
      output = path+Util.FS+"Hips"+fmt;
      context.setOutputPath(output);
      cds.tools.Util.deleteDir(new File(output));
      if( !flagColor ) {
         context.setFov("true");
         context.setDataRange("0 1000");
         context.setPixelCut("5 256");
         context.setLive(true);
      }
      
      context.valid("INDEX (HpxFinder) in "+output+"....");
      execute(Action.INDEX);

      context.valid("TILES in "+output+"....");
      context.setBitpix(flagColor?0:OUTPUTBITPIX);
      Builder b1 = execute(Action.TILES);
      timetimeFitsTiles = b1.getExecTime();
      
      context.valid("TILES redo (KEEPTILE) after deleting Norder0 in "+output+"....");
      cds.tools.Util.deleteDir(new File(output+Util.FS+"Norder0"));
      context.setModeMerge(ModeMerge.mergeKeepTile);
      Builder b2 = execute(Action.TILES);
      context.info("time1="+timetimeFitsTiles+" time2="+b2.getExecTime());

      // Ajout d'images a posteriori (en ayant nettoyer le HpxFinder à chaque fois)
      context.valid("TILES again (OVERWRITE, KEEP, ADD, MEAN) in "+output+"....");
      double dd=-0.7;
      double a=0.22;
      validateUpdate(fmt,"overwrite",        dd);  dd-=a;
      validateUpdate(fmt,"keep",             dd);  dd-=a;
      validateUpdate(fmt,"add",              dd);  dd-=a;
      validateUpdate(fmt,"mergeAdd,overlayMean", dd);  dd-=a;
      validateUpdate(fmt,"mean",               dd);  dd-=a;
      validateUpdate(fmt,"mean,fading",        dd);  dd-=a;

      // APPEND suivant différents modes
      context.valid("APPEND (OVERWRITE,KEEP,ADD,MEAN,MUL) in "+output+"....");
      dd=0.7;
      validateAppend(fmt,"overwrite",     dd);   dd+=a; 
      validateAppend(fmt,"keep",dd);      dd+=a;
      validateAppend(fmt,"add",  dd);     dd+=a;
      validateAppend(fmt,"mergeAdd,overlayMean", dd);   dd+=a;
      validateAppend(fmt,"mean",          dd);   dd+=a;
      if( !flagColor ) validateAppend(fmt,"mul,first",     dd);    dd+=a;
      

      context.valid("DETAILS in "+output+"....");
      execute(Action.DETAILS);

      if( !flagColor ) {
         context.valid("PNG in "+output+"....");
         execute(Action.PNG);
      }

      context.valid("LINT "+output+"....");
      execute(Action.LINT);

      context.valid("CHECKCODE and DATASUM in "+output+"....");
      execute(Action.CHECKCODE);
      execute(Action.CHECK);
      
      if( !flagColor ) {
         context.flagGlobalDataSum=true;
         BuilderCheckDataSum b = (BuilderCheckDataSum)execute(Action.CHECKDATASUM);
         globalDatasum = b.getDataGlobalDataSum();
      }
   }
   
   private String getFct(double v1, double v2) {
      return "x"+( (int)((v1/v2)*100.) /100.);
   }
   
   private void validateUpdate(String fmt,String mode,double shiftDec) throws Exception {
      context.reset();
      context.setOutputPath(output);
      context.setPartitioning("300");
      context.setBitpix(fmt.equals("fits")?OUTPUTBITPIX:0);
      
      execute(Action.CLEANINDEX);
//      cds.tools.Util.deleteDir(new File(input));   //  Alternative qui donne le même résultat sans supprimer l'index
      createProgenitorBis(fmt,shiftDec);
      context.setMode(mode);
      execute(Action.INDEX);
      execute(Action.TILES);
   }
   
   private void validateAppend(String fmt,String mode,double shiftDec) throws Exception {
      createProgenitorBis(fmt,shiftDec);
      context.setMode(mode);
      execute(Action.APPEND);
   }
   
   //  Creation de fichiers images sources
   private void createProgenitors(String fmt) throws Exception {
      input = path+Util.FS+"Data"+fmt;
      context.setInputPath(input);
      context.info("(Re)creating "+fmt+" data (progenitors) in "+input+"....");
      cds.tools.Util.deleteDir(new File(input));
      cds.tools.Util.createPath(input);
      
      // 2 mires legerement decalees
      String s = createImg(input,fmt,1500,500,RA,DEC,255,true);
      createFov(s,1500,500);
      for( int i=0; i<12; i++ ) createImg(input,fmt,800,2800,RA+.02,DEC+.02,145,true);   // Pour augmenter le poids (test mode live)
      
      // Deux zones avec beaucoup d'overlays
      if( ALLTEST ) {
         for( int i=0; i<50; i++ ) createImg(input,fmt,100,100,RA-0.85,DEC+0.42,80,false);
         for( int i=0; i<200; i++ ) createImg(input,fmt,100,100,RA+0.85,DEC-0.42,50,false);

         // Des images espacees...
         for( int i=0;i<4*360; i+=5 ) {
            createImg(input,fmt,300,300,RA+(i/50.+3)*Math.cos(Math.PI*i/180.),DEC+(i/100.+3)*Math.sin(Math.PI*i/180.),i/10,false);
         }
      }
   }
   
   //  Creation d'un deuxieme lots de fichiers images progenitors
   private void createProgenitorBis(String fmt,double deltaDec) throws Exception {
      input = path+Util.FS+"Data"+fmt+"bis";
      context.setInputPath(input);
      context.info("(Re)creating "+fmt+" (progenitors) in "+input+"....");
      cds.tools.Util.deleteDir(new File(input));
      cds.tools.Util.createPath(input);
      
      createImg(input,fmt,3000,100,RA,DEC-deltaDec,45,true);
      createImg(input,fmt,75,75,RA-3.2,DEC-deltaDec,10,false);
   }
   
   // Creation d'un fichier .fov associé au fichier filename
   private void createFov(String filename,int width, int height) throws Exception{
      filename = filename.substring(0, filename.lastIndexOf('.'))+".fov";
      OutputStream os = null;
      os = new FileOutputStream(filename);
      os.write( "10 10\n".getBytes() );
      os.write( ((width-30)+" 30\n").getBytes() );
      os.write( ((width-10)+" "+(height-8)+"\n").getBytes() );
      os.write( ("50 "+(height-40)+"\n").getBytes() );
      os.write( ("100 "+(height/2)+"\n").getBytes() );
      os.close();
   }
   
   // Creation d'une image source de test
   private String createImg(String input,String fmt,int width, int height, double ra,double dec,double value,boolean flagAdvanced) throws Exception {
      boolean flagColor = !fmt.equals("fits");
      int bitpix = flagColor?0:INPUTBITPIX;
      Fits f= new Fits(width,height,bitpix);
      f.setBlank(flagColor?0:BLANK);
      StringBuilder s = new StringBuilder();
      s.append("SIMPLE=T\n");
      s.append("BITPIX="+bitpix+"\n");
      s.append("NAXIS=2\n");
      s.append("NAXIS1="+f.width+"\n");
      s.append("NAXIS2="+f.height+"\n");
      s.append("CRPIX1="+(f.width/2)+"\n");
      s.append("CRPIX2="+(f.height/2)+"\n");
      s.append("CRVAL1="+ra+"\n");
      s.append("CRVAL2="+dec+"\n");
      s.append("CTYPE1=RA---SIN\n");
      s.append("CTYPE2=DEC--SIN\n");
      s.append("RADECSYS=FK5\n");
      s.append("CD1_1=-0.0016667\n");
      s.append("CD1_2=0\n");
      s.append("CD2_1=0\n");
      s.append("CD2_2=0.0016667\n");
      f.headerFits = new HeaderFits(s.toString());
      f.headerFits.setOriginalHeaderFits(s.toString());
      
      for( int y=0; y<f.height; y++ ) {
         for( int x=0; x<f.width; x++ ) {
            if( flagColor ) {
               int v=(int)value;
               int p = !flagAdvanced ? v : x<f.width/3 ? v : x<2*f.width/3 ? (v<<8) : (v<<16);
               int rgb = (0xFF<<24) | p ;
               f.setPixelRGB(x, y, rgb);
            } else {
               double p = !flagAdvanced ? value : x<f.width/3 ? value/3 : x<2*f.width/3 ? 2*value/3 : value;
               f.setPixelDouble(x, y, p);
            }
         }
      }
      if( flagAdvanced ) {
         int c=30;
         int ymid = f.height/2-1;
         int xmid = f.width/2-1;
         for( int y=ymid-c; y<=ymid+c; y++ ) {
            for( int x=xmid-c*3; x<xmid+c*3; x++ ) {
               if( y!=ymid || x!=xmid ) {
                  if( flagColor ) f.setPixelRGB(x, y, 0);   // transparent
                  else f.setPixelDouble(x,y,BLANK);
               }
            }
         }

         for( int y=0; y<c; y++ ) {
            for( int x=0; x<c; x++ ) {
               if( flagColor ) {
                  int p = (x*y)/3;
                  if( p>255 ) p=255;  
                  int p1 = (0xFF<<24)| ((0xFF & (255-p))<<8) | (0xFF & p);
                  f.setPixelRGB(10+c+x,10+c+y,p1);
                  f.setPixelRGB(f.width-c-10-x,f.height-c-10-y,p1);
               } else {
                  double p = (x*y)/3;
                  if( p>255 ) p=255;  
                  f.setPixelDouble(10+c+x,10+c+y,p);
                  f.setPixelDouble(f.width-c-10-x,f.height-c-10-y,p);
               }
            }
         }
      }

      String filename = input+Util.FS+"Image"+(++SUFFIXE)+"."+fmt;
      if( fmt.equals("fits") ) f.writeFITS(filename);
      else 
         if( fmt.equals("png"))  f.writePng(filename);
      else f.writeJpg(filename);
      
      return filename;
   }
   
   /** Retourne true si le fichier Fits matche bien les valeurs x,y,pix [] */
   private void checkFits(String filename, double [][] xyv) throws Exception {
      boolean rep=true;
      Fits f = new Fits(filename);
      for( double []xyv1 : xyv ) {
         int x = (int)(xyv1[0]-0.5);
         int y = (int)(xyv1[1]-0.5);
         double p = f.getPixelFull(x, y);
         boolean ok = f.isBlankPixel(p) && Double.isNaN(xyv1[2]) || p==xyv1[2];
         rep &= ok;
         if( !rep ) context.error("checkFits on "+filename+" ("+x+","+y+")="+p+(ok?" ok":(" error (should be "+xyv1[2]+")")));
      }
      if( !rep ) throw new Exception("The generated HiPS does not match!");
   }


   @Override
   public void validateContext() throws Exception { }
}
