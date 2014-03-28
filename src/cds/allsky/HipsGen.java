// Copyright 2012 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//

package cds.allsky;

import java.io.File;
import java.io.FileInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import cds.aladin.Aladin;
import cds.aladin.MyProperties;
import cds.moc.HealpixMoc;
import cds.tools.Util;

public class HipsGen {

   private File file;
   private boolean force=false;
   private boolean flagMode=false;
   private boolean flagAbort=false,flagPause=false,flagResume=false;
   public Context context;
   
   public String launcher = "Aladin.jar -hipsgen";

   private Vector<Action> actions;

   public HipsGen() {
      this.context = new Context();
      actions = new Vector<Action>();
   }

   /**
    * Analyse le fichier contenant les paramètres de config de la construction
    * du allsky sous le format : option = valeur
    * 
    * @throws Exception
    *             si l'erreur dans le parsing des options nécessite une
    *             interrption du programme
    */
   private void parseConfig() throws Exception {

      // Extrait toutes les options du fichier
      // pour construire le contexte

      // Ouverture et lecture du fichier
      MyProperties properties = new MyProperties();
//      Reader reader = new FileReader(file);
      FileInputStream reader = new FileInputStream(file);
      properties.load(reader);

      Set<Object> keys = properties.keySet();
      for (Object opt : keys) {
         String val = properties.getProperty((String)opt);

         try {
            setContextFromOptions((String)opt, val);
         } catch (Exception e) {
            e.printStackTrace();
            break;
         }
      }

      reader.close();
   }

   /**
    * Affecte à un objet Context l'option de configuration donnée
    *
    * @param opt
    *            nom de l'option
    * @param val
    *            valeur de l'option
    * @throws Exception
    *             si l'interprétation de la valeur nécessite une interrption du
    *             programme
    */
   private void setContextFromOptions(String opt, String val) throws Exception {
      // enlève des éventuels apostrophes ou guillemets
      val = val.replace("\'", "");
      val = val.replace("\"", "");
      System.out.println("OPTION: "+opt + "=" + val);

      // System.out.println(opt +" === " +val);
      if( opt.equalsIgnoreCase("h")) {
         usage(launcher);
      } else if (opt.equalsIgnoreCase("verbose"))    { Context.setVerbose(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("blank"))      { context.setBlankOrig(Double.parseDouble(val));
      } else if (opt.equalsIgnoreCase("order"))      { context.setOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("diffOrder"))  { context.setDiffOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("bitpix"))     { context.setBitpix(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("frame"))      { context.setFrameName(val);
      } else if (opt.equalsIgnoreCase("maxThread"))  { context.setMaxNbThread(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("skyval"))     { context.setSkyval(val);
      } else if (opt.equalsIgnoreCase("exptime"))    { context.setExpTime(val);
      } else if (opt.equalsIgnoreCase("fading"))     { context.setFading(val);
      } else if (opt.equalsIgnoreCase("mixing"))     { context.setMixing(val);
      } else if (opt.equalsIgnoreCase("color"))      { context.setColor(val);
      } else if (opt.equalsIgnoreCase("red"))        { context.setRgbInput(val, 0);
      } else if (opt.equalsIgnoreCase("green"))      { context.setRgbInput(val, 1);
      } else if (opt.equalsIgnoreCase("blue"))       { context.setRgbInput(val, 2);
      } else if (opt.equalsIgnoreCase("redparam"))   { context.setRgbCmParam(val, 0);
      } else if (opt.equalsIgnoreCase("greenparam")) { context.setRgbCmParam(val, 1);
      } else if (opt.equalsIgnoreCase("blueparam"))  { context.setRgbCmParam(val, 2);
      } else if (opt.equalsIgnoreCase("img"))        { context.setImgEtalon(val);
      } else if (opt.equalsIgnoreCase("fitskeys"))   { context.setIndexFitskey(val);
      } else if (opt.equalsIgnoreCase("publisher"))  { context.setPublisher(val);
      } else if (opt.equalsIgnoreCase("hdu"))        { context.setHDU(val);
      
      } else if (opt.equalsIgnoreCase("debug")) {
         if (Boolean.parseBoolean(val)) Context.setVerbose(4);
         
      } else if (opt.equalsIgnoreCase("in") || opt.equalsIgnoreCase("input")) {
         context.setInputPath(val);
         
      } else if (opt.equalsIgnoreCase("out") || opt.equalsIgnoreCase("output")) {
         context.setOutputPath(val);
         
      } else if (opt.equalsIgnoreCase("mode") || opt.equalsIgnoreCase("pixel")) {
         if (opt.equalsIgnoreCase("pixel") ) context.warning("Prefer \"mode\" instead of \"pixel\"");
         context.setCoAddMode(CoAddMode.valueOf(val.toUpperCase()));
         flagMode=true;
         
      } else if (opt.equalsIgnoreCase("region") || opt.equalsIgnoreCase("moc")) {
         if (val.endsWith("fits")) {
            HealpixMoc moc = new HealpixMoc();
            moc.read(val);
            context.setMocArea(moc);
         } else context.setMocArea(val);
         
      } else if (opt.equalsIgnoreCase("blocking") || opt.equalsIgnoreCase("cutting") || opt.equalsIgnoreCase("partitioning")) {
         context.setPartitioning(val);
         
      } else if (opt.equalsIgnoreCase("circle") || opt.equalsIgnoreCase("radius")) {
         try {
            context.setCircle(val);
         } catch (ParseException e) {
            throw new Exception(e.getMessage());
         }
         
      } else if (opt.equalsIgnoreCase("border")) {
         try {
            context.setBorderSize(val);
         } catch (ParseException e) {
            throw new Exception(e.getMessage());
         }
         
      } else if ( opt.equalsIgnoreCase("jpegMethod") || opt.equalsIgnoreCase("method")) {
         if( opt.equalsIgnoreCase("jpegMethod") ) context.warning("Prefer \"method\" instead of \""+opt+"\"");
         context.setMethod(val);
         
      } else if (opt.equalsIgnoreCase("pixelGood")) { context.setPixelGood(val);
      } else if (opt.equalsIgnoreCase("pixelCut")) { context.setPixelCut(val);
      } else if (opt.equalsIgnoreCase("pixelRange") || opt.equalsIgnoreCase("dataCut")) {
         if (opt.equalsIgnoreCase("dataCut") ) context.warning("Prefer \"pixelRange\" instead of \"dataCut\"");
         context.setDataCut(val);
         context.setPixelGood(val);  // A VOIR S'IL FAUT LE LAISSER
      } else throw new Exception("Option unknown [" + opt + "]");
      
   }
   
   static private SimpleDateFormat SDF;
   static {
      SDF = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
      SDF.setTimeZone(TimeZone.getDefault());
   }
   
   public void execute(String [] args) {
      int length = args.length;
      boolean first=true;
      
      if (length == 0) {
         usage(launcher);
         return;
      }
      
      // extrait les options en ligne de commande, et les analyse
      for (String arg : args) {
         // si c'est dans un fichier
         String param = "-param=";
         if (arg.startsWith(param)) {
            try {
               setConfigFile(arg.substring(param.length()));
            } catch (Exception e) {
               e.printStackTrace();
               return;
            }
            continue;
         }
         
         // Juste pour pouvoir appeler directement par le main() de cette classe
         // et non celle d'Aladin
         else if( arg.equalsIgnoreCase("-skygen") || arg.equalsIgnoreCase("-hipsgen")) continue;
         
         // help
         else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
            HipsGen.usage(launcher);
            return;
         }
         
         if( first ) {
            first=false;
            context.info("Starting HipsGen "+SDF.format(new Date())+" (based on Aladin "+Aladin.VERSION+")...");
         }

         // debug
         if (arg.equalsIgnoreCase("-debug") || arg.equalsIgnoreCase("-d")) Context.setVerbose(4);
         else if (arg.equalsIgnoreCase("-fast") ) context.mixing=true;
         else if (arg.equalsIgnoreCase("-force") || arg.equalsIgnoreCase("-f") )  force=true;
         else if (arg.equalsIgnoreCase("-n") )  context.fake=true;

         // toutes les autres options écrasent les précédentes
         else if (arg.contains("=")) {
            String[] opts = arg.split("=");
            try {
               // si il y a un - on l'enlève
               opts[0] = opts[0].substring(opts[0].indexOf('-') + 1);

               setContextFromOptions(opts[0], opts[1]);
            } catch (Exception e) {
               e.printStackTrace();
               context.error(e.getMessage());
               return;
            }
         }
         // les autres mots sont supposées des actions (si +ieurs, seule la
         // dernière est gardée)
         else {
            try {
               Action a = Action.valueOf(arg.toUpperCase());
               if( a==Action.FINDER ) a=Action.INDEX;     // Pour compatibilité
               if( a==Action.PROGEN ) a=Action.DETAILS;   // Pour compatibilité
               if( a==Action.CONCAT && !flagMode ) context.setCoAddMode(CoAddMode.AVERAGE);
               if( a==Action.ABORT ) flagAbort=true;    // Bidouillage pour pouvoir tuer un skygen en cours d'exécution
               if( a==Action.PAUSE ) flagPause=true;    // Bidouillage pour pouvoir mettre en pause un skygen en cours d'exécution
               if( a==Action.RESUME ) flagResume=true;  // Bidouillage pour pouvoir remettre en route un skygen en pause
               actions.add(a);
            } catch (Exception e) {
               context.error("Unknown skygen command ["+arg+"] !");
               return;
            }
         }
      }
      
      // Permet de tuer proprement une tache déjà en cours d'exécution
      if( flagAbort ) {
         try { context.taskAbort(); }
         catch( Exception e ) { context.error(e.getMessage()); }
         return;
      }
      
      // Permet de mettre en pause temporaire une tache en cours d'exécution
      if( flagPause ) {
         try { context.setTaskPause(true); }
         catch( Exception e ) { context.error(e.getMessage()); }
         return;
      }
      
      // Permet de mettre reprendre une tache en pause
      if( flagResume ) {
         try { context.setTaskPause(false); }
         catch( Exception e ) { context.error(e.getMessage()); }
         return;
      }


      // Les tâches à faire si aucune n'est indiquées
      boolean all=false;
      if( actions.size()==0 ) {
         all=true;
         actions.add(Action.INDEX);
         actions.add(Action.TILES);

         if( !context.isColor() ) {
            actions.add(Action.GZIP);
            actions.add(Action.PNG);
            actions.add(Action.DETAILS);
         }
      }
     
      // Nettoyage avant ?
      if( force ) {
         context.setIgnoreStamp(true);
         if( all ) actions.add(0, Action.CLEAN);
         else {
            for( int i=0; i<actions.size() ;i++ ) {
               Action a = actions.get(i);
                    if( a==Action.INDEX )   { actions.add(i, Action.CLEANINDEX);   i++; }
               else if( a==Action.DETAILS ) { actions.add(i, Action.CLEANDETAILS); i++; }
               else if( a==Action.TILES )   { actions.add(i, Action.CLEANTILES);   i++; }
               else if( a==Action.JPEG )    { actions.add(i, Action.CLEANJPEG);    i++; }
               else if( a==Action.PNG )     { actions.add(i, Action.CLEANPNG);     i++; }
            }
         }
      }
      
      if( context.fake ) context.warning("NO RUN MODE (option -n), JUST PRINT INFORMATION !!!");
      for( Action a : actions ) {
         context.info("Action => "+a+": "+a.doc());
      }

      // C'est parti
      try {
         long t = System.currentTimeMillis();
         new Task(context,actions,true);
         context.done("The end (done in "+Util.getTemps(System.currentTimeMillis()-t)+")");
      } catch (Exception e) {
         e.printStackTrace();
         context.error(e.getMessage());
         return;
      }
   }
   
   /** Juste pour pouvoir exécuter skygen comme une commande script Aladin */
   public void executeAsync(String [] args) { new ExecuteAsyncThread(args); }
   class ExecuteAsyncThread extends Thread {
      String [] args;
      public ExecuteAsyncThread(String [] args) { this.args=args; start(); }
      public void run() { execute(args); }
   }
   
   // Aladin.jar -hipsgen
   private static void usage(String launcher) {
      System.out.println("Usage: java -jar "+launcher+" [-f] options... [ACTION ...]");
      System.out.println("       java -jar "+launcher+" [-f] -param=configfile\n\n");
      System.out.println("This config file must contains these following options, or use them\n" +
      		"             directly in the comand line :");
      System.out.println(
            "-f                 Do not take into account possible previous computation\n"+
            "-n                 Just print process information, but do not execute it.\n"+
            "in=dir             Source image directory (fits or jpg|png +hhh or HiPS)" + "\n" +
            "out=dir            HiPS target directory (default $PWD+\""+Constante.ALLSKY+"\")" + "\n" +
            "mode=xx            Coadd mode when restart: pixel level(OVERWRITE|KEEP|AVERAGE) \n" +
            "                   or tile level (REPLACETILE|KEEPTILE) - (default OVERWRITE)" + "\n" +
            "img=file           Specifical reference image for default initializations \n" +
            "                   (BITPIX,BSCALE,BZERO,BLANK,order,pixelCut,dataRange)" + "\n" +
            "bitpix=nn          Specifical target bitpix (-64|-32|8|16|32|64)" + "\n" +
            "order=nn           Specifical HEALPix order" + "\n" +
//            "diffOrder          Diff between MOC order and optimal order" + "\n" +
            "hdu=n1,n2-n3,...|all  List of HDU numbers (0 is the primary HDU - default is 0)\n" +
            "border=...         Margins (in pixels) to ignore in the original images (N W S E or constant)" + "\n" +
            "circle=nn          Circle mask (in pixels) centered on each original images" + "\n" +
            "blank=nn           Specifical BLANK value" + "\n" +
            "maxThread=nn       Max number of computing threads" + "\n" +
            "region=moc         Specifical HEALPix region to compute (ex: 3/34-38 50 53)\n" +
            "                   or Moc.fits file (all sky by default)" + "\n" +
            "pixelCut=min max   Specifical pixel cut and/or transfert function for PNG/JPEG 8 bits\n" +
            "                   conversion - ex: \"120 140 log\")" + "\n" +
            "pixelRange=min max Specifical pixel value range (required for bitpix\n" +
            "                   conversion, or for removing bad pixels - ex: \"-5 110\")" + "\n" +
//            "pixelGood=min [max] Range of pixel values kept" + "\n" +
            "skyval=true|key    Fits key to use for removing a sky background, true for automatic detection" + "\n" +
//            "exptime=key        Fits key to use for adjusting variation of exposition" + "\n" +
            "fitskeys=list      Fits key list (blank separator) designing metadata FITS keyword value to memorized in the HiPS index" + "\n" + 
            "fading=true|false  False to avoid fading effect on overlapping original images (default is true)" + "\n" +
            "mixing=true|false  False to avoid mixing (and fading) effect on overlapping original images (default is true)" + "\n" +
            "partitioning=true|false True for cutting large original images in blocks of 1024x1024 (default is true)" + "\n" +
            "method=m           Method (MEDIAN|MEAN) (default MEDIAN) for aggregating compressed tiles (jpeg|png)" + "\n" +
            "color=jpeg|png     The source images are colored images (jpg or png) and the tiles will be produced in jpeg (resp. png)" + "\n" +
            "publisher=name     Name of the person|institute who builds the HiPS" + "\n"+
            "verbose=n          Debug information from -1 (nothing) to 4 (a lot)" + "\n"
//            "debug=true|false   to set output display as te most verbose or just statistics" + "\n" +
//            "red        all-sky used for RED component (see rgb action)\n" +
//            "green      all-sky used for BLUE component (see rgb action)\n" +
//            "blue       all-sky used for GREEN component (see rgb action)\n" +
//            "redcm      Transfert function for RED component (hsin, log, sqrt, linear or sqr)\n" +
//            "greencm    Transfert function for BLUE component (hsin, log, sqrt, linear or sqr)\n" +
//            "bluecm    Transfert function for GREEN component (hsin, log, sqrt, linear or sqr)\n" +
//            "frame           Healpix frame (C or G - default C for ICRS)" + "\n" +
      );
      
      System.out.println("\nSpecifical actions (by default: \"INDEX TILES PNG GZIP PROGEN\"):" + "\n" +
            "INDEX      "+Action.INDEX.doc() + "\n" +
            "TILES      "+Action.TILES.doc() + "\n" +
            "JPEG       "+Action.JPEG.doc() + "\n" +
            "PNG        "+Action.PNG.doc() + "\n" +
//            "RGB        "+Action.RGB.doc() + "\n" +
            "MOC        "+Action.MOC.doc() + "\n" +
//            "MOCHIGHT   "+Action.MOCHIGHT.doc() + "\n" +
            "ALLSKY     "+Action.ALLSKY.doc() + "\n"+
            "TREE       "+Action.TREE.doc() + "\n"+
            "CONCAT     "+Action.CONCAT.doc() + "\n"+
            "GZIP       "+Action.GZIP.doc() + "\n"+
            "DETAILS    "+Action.DETAILS.doc() + "\n"
            );
      System.out.println("\nEx: java -jar "+launcher+" in=/MyImages    => Do all the job." +
      		             "\n    java -jar "+launcher+" in=/MyImages bitpix=16 pixelCut=\"-1 100 log\" => Do all the job" +
      		             "\n           The FITS tiles will be coded in short integers, the preview tiles" +
      		             "\n           will map the physical values [-1..100] with a log function contrast in [0..255]." +
                         "\n    java -jar "+launcher+" in=/MyImages blank=0 border=\"100 50 100 50\" mode=REPLACETILE    => recompute tiles" +
                         "\n           The original pixels in the border or equal to 0 will be ignored."+
                         "\n    java -jar "+launcher+" in=HiPS out=HiPStarget CONCAT   => Concatenate HiPS to HiPStarget"
//                         "\n    java -jar Aladin.jar -mocgenred=/MySkyRed redparam=sqrt blue=/MySkyBlue output=/RGB rgb  => compute a RGB all-sky"
                         );
   }

   private void setConfigFile(String configfile) throws Exception {
      this.file = new File(configfile);
      parseConfig();
   }

   public static void main(String[] args) {
      HipsGen generator = new HipsGen();
      generator.launcher="HipsGen";
      generator.execute(args);
   }
}
