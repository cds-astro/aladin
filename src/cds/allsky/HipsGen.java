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
import java.util.Date;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.allsky.Context.JpegMethod;
import cds.moc.HealpixMoc;
import cds.tools.Util;

public class HipsGen {

   private File file;
   private boolean force=false;
   private boolean flagMode=false;
   private boolean flagConcat=false;
   private boolean flagMethod=false;
   private boolean flagRGB=false;
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
      } else if (opt.equalsIgnoreCase("minOrder"))   { context.setMinOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("mocOrder"))   { context.setMocOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("tileOrder"))  { context.setTileOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("bitpix"))     { context.setBitpix(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("frame"))      { context.setFrameName(val);
      } else if (opt.equalsIgnoreCase("maxThread"))  { context.setMaxNbThread(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("skyval"))     { context.setSkyval(val);
      } else if (opt.equalsIgnoreCase("exptime"))    { context.setExpTime(val);
      } else if (opt.equalsIgnoreCase("fading"))     { context.setFading(val); 
      } else if (opt.equalsIgnoreCase("mixing"))     { context.setMixing(val);
      } else if (opt.equalsIgnoreCase("color"))      { context.setColor(val);
      } else if (opt.equalsIgnoreCase("inRed"))      { context.setRgbInput(val, 0); flagRGB=true;
      } else if (opt.equalsIgnoreCase("inGreen"))    { context.setRgbInput(val, 1); flagRGB=true;
      } else if (opt.equalsIgnoreCase("inBlue"))     { context.setRgbInput(val, 2); flagRGB=true;
      } else if (opt.equalsIgnoreCase("cmRed"))      { context.setRgbCmParam(val, 0);
      } else if (opt.equalsIgnoreCase("cmGreen"))    { context.setRgbCmParam(val, 1);
      } else if (opt.equalsIgnoreCase("cmBlue"))     { context.setRgbCmParam(val, 2);
      } else if (opt.equalsIgnoreCase("img"))        { context.setImgEtalon(val);
      } else if (opt.equalsIgnoreCase("fitskeys"))   { context.setIndexFitskey(val);
      } else if (opt.equalsIgnoreCase("publisher"))  { context.setPublisher(val);
      } else if (opt.equalsIgnoreCase("ivorn"))      { context.setIvorn(val);
      } else if (opt.equalsIgnoreCase("target"))     { context.setTarget(val);
      } else if (opt.equalsIgnoreCase("targetRadius")){ context.setTargetRadius(val);
      } else if (opt.equalsIgnoreCase("label"))      { context.setLabel(val);
      } else if (opt.equalsIgnoreCase("hdu"))        { context.setHDU(val);

      } else if (opt.equalsIgnoreCase("debug")) {
         if (Boolean.parseBoolean(val)) Context.setVerbose(4);

      } else if (opt.equalsIgnoreCase("in") || opt.equalsIgnoreCase("input")) {
         context.setInputPath(val);

      } else if (opt.equalsIgnoreCase("out") || opt.equalsIgnoreCase("output")) {
         context.setOutputPath(val);

      } else if (opt.equalsIgnoreCase("mode") || opt.equalsIgnoreCase("pixel")) {
         if (opt.equalsIgnoreCase("pixel") ) context.warning("Prefer \"mode\" instead of \"pixel\"");
         context.setMode(Mode.valueOf(val.toUpperCase()));
         flagMode=true;

      } else if (opt.equalsIgnoreCase("region") || opt.equalsIgnoreCase("moc")) {
         if (val.endsWith("fits")) {
            HealpixMoc moc = new HealpixMoc();
            moc.read(val);
            context.setMocArea(moc);
         } else context.setMocArea(val);

      } else if (opt.equalsIgnoreCase("blocking") || opt.equalsIgnoreCase("cutting") || opt.equalsIgnoreCase("partitioning")) {
         context.setPartitioning(val);

      } else if( opt.equalsIgnoreCase("tileTypes") ) {
         context.setTileTypes(val);

      } else if( opt.equalsIgnoreCase("shape") ) {
         context.setShape(val);

      } else if (opt.equalsIgnoreCase("maxRatio")) {
         try {  context.setMaxRatio(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }

      } else if (opt.equalsIgnoreCase("circle") || opt.equalsIgnoreCase("radius")) {
         try {  context.setCircle(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }

      } else if (opt.equalsIgnoreCase("polygon") || opt.equalsIgnoreCase("fov") ) {
         try {  context.setPolygon(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }

      } else if (opt.equalsIgnoreCase("border")) {
         try {
            context.setBorderSize(val);
         } catch (ParseException e) {
            throw new Exception(e.getMessage());
         }

      } else if ( opt.equalsIgnoreCase("jpegMethod") || opt.equalsIgnoreCase("method")) {
         if( opt.equalsIgnoreCase("jpegMethod") ) context.warning("Prefer \"method\" instead of \""+opt+"\"");
         flagMethod=true;
         context.setMethod(val);

      } else if (opt.equalsIgnoreCase("pixelGood")) { context.setPixelGood(val);
      } else if (opt.equalsIgnoreCase("pixelCut")) { context.setPixelCut(val);
      } else if (opt.equalsIgnoreCase("pixelRange") || opt.equalsIgnoreCase("dataCut")) {
         if (opt.equalsIgnoreCase("dataCut") ) context.warning("Prefer \"pixelRange\" instead of \"dataCut\"");
         context.setDataCut(val);
         //         context.setPixelGood(val);  // A VOIR S'IL FAUT LE LAISSER
      } else throw new Exception("Option unknown [" + opt + "]");

   }

   static public SimpleDateFormat SDF;
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
         else if (arg.equalsIgnoreCase("-nice") ) context.mirrorDelay=500;
         else if (arg.equalsIgnoreCase("-clone") ) context.testClonable=false;
         else if (arg.equalsIgnoreCase("-live") ) context.setLive(true);
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
               if( a==Action.CONCAT ) {
                  flagConcat=true;
                  if( !flagMode ) context.setMode(Mode.AVERAGE);
               }
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

         // S'agirait-il de la génération d'un HiPS RGB
         if( flagRGB ) actions.add(Action.RGB);

         else {

            // S'agirait-il d'une map HEALPix
            boolean flagMapFits=false;
            File f = new File(context.getInputPath());
            if( !f.isDirectory() && f.exists() ) {
               try {
                  MyInputStream in = new MyInputStream( new FileInputStream(f));
                  in = in.startRead();
                  flagMapFits = (in.getType() & MyInputStream.HEALPIX)!=0;
                  in.close();
                  context.setMap(flagMapFits);
               } catch( Exception e ) { }
            }


            // d'une map FITS peut être ?
            if( flagMapFits ) actions.add(Action.MAPTILES);

            // d'une collection d'images ?
            else {
               actions.add(Action.INDEX);
               actions.add(Action.TILES);
            }

            if( !context.isColor() ) {
               actions.add(Action.GZIP);
               //            actions.add(Action.JPEG);
               actions.add(Action.PNG);
               if( !flagMapFits ) actions.add(Action.DETAILS);
            }
         }

      }

      // Ajustement du mode par défaut dans le cas d'une génération d'une HiPS RGB
      if( flagRGB && !flagMode ) context.setMode(Mode.REPLACETILE);

      // Ajustement de la méthode par défaut (moyenne pour les FITS, médiane pour les couleurs)
      // à moins qu'elle n'ait été spécifiquement indiquée
      if( context.isColor() && !flagMethod ) {
         context.setJpegMethod( JpegMethod.MEDIAN );
      }

      if( context.getMode()==Mode.ADD  ) {
         context.setFading(false);
         context.setPartitioning("false");
         context.setMixing("true");
         context.info("Pixel mode=ADD => fading, partitioning and no mixing parameter ignored");
      }


      // Nettoyage avant ?
      if( force ) {
         context.setIgnoreStamp(true);
         if( all ) actions.add(0, Action.CLEAN);
         else {
            for( int i=0; i<actions.size() ;i++ ) {
               Action a = actions.get(i);
               if( a==Action.INDEX )   { actions.add(i, Action.CLEANINDEX);   i++; }
               else if( a==Action.MIRROR )  { actions.add(i, Action.CLEAN); i++; }
               else if( a==Action.DETAILS ) { actions.add(i, Action.CLEANDETAILS); i++; }
               else if( a==Action.TILES )   { actions.add(i, Action.CLEANTILES);   i++; }
               else if( a==Action.MAPTILES ){ actions.add(i, Action.CLEANTILES);   i++; }
               else if( a==Action.JPEG )    { actions.add(i, Action.CLEANJPEG);    i++; }
               else if( a==Action.PNG )     { actions.add(i, Action.CLEANPNG);     i++; }
               else if( a==Action.CUBE )    { actions.add(i, Action.CLEAN);        i++; }
            }
         }
      }

      if( context.fake ) context.warning("NO RUN MODE (option -n), JUST PRINT INFORMATION !!!");
      for( Action a : actions ) {
         context.info("Action => "+a+": "+a.doc());
      }

      // Positionnement du frame par défaut
      if( !flagRGB ) setDefaultFrame();

      // Positionnement du pubDid
      if( context.ivorn==null && !flagConcat ) {
         String s = context.checkIvorn(null, false);
         context.setIvorn(s);
         context.warning("IVORN identifier is strongly recommended (parameter ivorn=xxx) => assuming "+s);
         context.info("The IVORN can be modified after the process by editing the properties file)");
      }

      // C'est parti
      try {
         long t = System.currentTimeMillis();
         new Task(context,actions,true);
         context.done("=================== THE END (done in "+Util.getTemps(System.currentTimeMillis()-t)+") =======================");
      } catch (Exception e) {
         e.printStackTrace();
         context.error(e.getMessage());
         return;
      }
   }

   // Positionnement du frame par défaut (equatorial, sauf s'il y a déjà
   // un HiPS existant, auquel cas il faut regarder dans ses propriétés,
   // et s'il n'y en a a pas, c'est du galactic
   private void setDefaultFrame() {
      // Le frame est explicite => rien à faire
      if( context.hasFrame() ) return;

      String path = context.getOutputPath();
      String frame=null;

      // Je vais essayer de récupérer le frame précédent depuis le fichier des propriétés
      try {
         String propFile = path+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            FileInputStream in = new FileInputStream(propFile);
            prop.load(in);
            in.close();
            String s =prop.getProperty(Constante.KEY_HIPS_FRAME);
            if( s==null ) s =prop.getProperty(Constante.OLD_HIPS_FRAME);

            // Good trouvé !
            if( s!=null && s.length()>0 ) frame=s;

            // pas de propriété hips_frame positionnée => galactic
            else frame="galactic";

            // Pas trouvé ! si le HiPS existe déjà, alors c'est pas défaut du galactic
            // sinon de l'equatorial
         } else {
            if( context.isExistingAllskyDir() ) frame="galactic";
            else frame="equatorial";
         }
      } catch( Exception e ) { }
      context.setFrameName(frame);
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
      System.out.println("Usage: java -jar "+launcher+" in=file|dir [otherParams ... ACTIONs ...]");
      System.out.println("       java -jar "+launcher+" -param=configfile\n");
      System.out.println("The config file must contain these following options, or use them\n" +
            "directly on the comand line :\n");
      System.out.println(
            "Required parameter:\n"+
                  "   in=dir             Source image directory (FITS or JPEG|PNG +hhh or HiPS),\n"+
                  "                      unique image or HEALPix map file" + "\n" +
                  "\n"+
                  "Basic optional parameters:\n"+
                  "   out=dir            HiPS target directory (default $PWD+\""+Constante.HIPS+"\")" + "\n" +
                  "   label=name         Label of the survey (by default, input directory name)" + "\n"+
                  "   publisher=name     Name of the person|institute who builds the HiPS" + "\n"+
                  "   hdu=n1,n2-n3,...|all  List of HDU numbers (0 is the primary HDU - default is 0)\n" +
                  "   blank=nn           Specifical BLANK value" + "\n" +
                  "   skyval=true|key    Fits key to use for removing a sky background, true for automatic detection" + "\n" +
                  "   color=jpeg|png     The source images are colored images (jpg or png) and the tiles will be " + "\n" +
                  "                      produced in jpeg (resp. png)" + "\n" +
                  "   shape=...          Shape of the observations (ellipse|rectangle)" + "\n" +
                  "   border=...         Margins (in pixels) to ignore in the original observations (N W S E or " + "\n" +
                  "                      constant)" + "\n" +
                  "   fov=true|x1,y1..   Observed regions by files.fov or global polygon (in FITS convention)." + "\n" +
                  "   verbose=n          Debug information from -1 (nothing) to 4 (a lot)" + "\n"+
                  "   -f                 clear previous computations\n"+
                  "   -n                 Just print process information, but do not execute it.\n"+
                  "\n"+
                  "Advanced optional parameters:\n"+
                  "   order=nn           Specifical HEALPix order - by default, adapted to the original resolution" + "\n" +
                  "   bitpix=nn          Specifical target bitpix (-64|-32|8|16|32|64)" + "\n" +
                  "   pixelCut=min max   Specifical pixel cut and/or transfert function for PNG/JPEG 8 bits\n" +
                  "                      conversion - ex: \"120 140 log\")" + "\n" +
                  "   pixelRange=min max Specifical pixel value range (required for bitpix\n" +
                  "                      conversion, or for removing bad pixels - ex: \"-5 110\")" + "\n" +
                  "   pixelGood=min [max] Range of pixel values kept" + "\n" +
                  "   img=file           Specifical reference image for default initializations \n" +
                  "                      (BITPIX,BSCALE,BZERO,BLANK,order,pixelCut,pixelRange)" + "\n" +
                  "   mode=xx            Coadd mode when restart: pixel level(OVERWRITE|KEEP|ADD|AVERAGE) \n" +
                  "                      or tile level (REPLACETILE|KEEPTILE) - (default OVERWRITE)" + "\n" +
                  "                      Or LINK|COPY for CUBE action (default COPY)" + "\n" +
                  "   fading=true|false  False to avoid fading effect on overlapping original images " + "\n" +
                  "                      (default is true)" + "\n" +
                  "   mixing=true|false  False to avoid mixing (and fading) effect on overlapping original images " + "\n" +
                  "                      (default is true)" + "\n" +
                  "   partitioning=true|false True for cutting large original images in blocks of 1024x1024 " + "\n" +
                  "                      (default is true)" + "\n" +
                  "   region=moc         Specifical HEALPix region to compute (ex: 3/34-38 50 53)\n" +
                  "                      or Moc.fits file (all sky by default)" + "\n" +
                  "   maxRatio=nn        Max height/width pixel ratio tolerated for original obs " + "\n" +
                  "                      (default 2, 0 for removing the test)" + "\n" +
                  //          "   exptime=key        Fits key to use for adjusting variation of exposition" + "\n" +
                  "   fitskeys=list      Fits key list (blank separator) designing metadata FITS keyword value " + "\n" +
                  "                      to memorized in the HiPS index" + "\n" +
                  "   minOrder=nn        Specifical HEALPix min order (only for DETAILS action)" + "\n" +
                  "   method=m           Method (MEDIAN|MEAN|FIRST) (default MEDIAN) for aggregating colored " + "\n" +
                  "                      compressed tiles (JPEG|PNG)" + "\n" +
                  "   tileOrder=nn       Specifical tile order - default "+Constante.ORDER + "\n" +
                  "   mocOrder=nn        Specifical HEALPix MOC order (only for MOC action) - by default " + "\n" +
                  "                      auto-adapted to the HiPS" + "\n" +
                  "   inRed              HiPS red path component (RGB action)\n" +
                  "   inGreen            HiPS green path component (RGB action)\n" +
                  "   inBlue             HiPS blue path component (RGB action)\n" +
                  "   cmRed              Colormap parameters for HiPS red component (min [mid] max [fct])\n" +
                  "   cmGreen            Colormap parameters for HiPS green component (min [mid] max [fct])\n" +
                  "   cmBlue             Colormap parameters for HiPS blue component (min [mid] max [fct])\n" +
                  "   tileTypes          List of tile format to copy (MIRROR action)" + "\n" +
                  "   maxThread=nn       Max number of computing threads" + "\n" +
                  "   target=ra +dec     Default HiPS target (ICRS deg)" + "\n"+
                  "   targetRadius=rad   Default HiPS radius view (deg)" + "\n"+
                  "   -nice              Slow download for avoiding to overload remote http server (dedicated " + "\n" +
                  "                      to MIRROR action)" + "\n"
                  //          "   debug=true|false   to set output display as te most verbose or just statistics" + "\n" +
                  //                            "   frame           Healpix frame (C or G - default C for ICRS)" + "\n" +
            );

      System.out.println("\nSpecifical actions (by default: \"INDEX TILES PNG GZIP DETAILS\"):" + "\n" +
            "   INDEX      "+Action.INDEX.doc() + "\n" +
            "   TILES      "+Action.TILES.doc() + "\n" +
            "   JPEG       "+Action.JPEG.doc() + "\n" +
            "   PNG        "+Action.PNG.doc() + "\n" +
            "   RGB        "+Action.RGB.doc() + "\n" +
            "   MOC        "+Action.MOC.doc() + "\n" +
            //            "   MOCHIGHT   "+Action.MOCHIGHT.doc() + "\n" +
            "   ALLSKY     "+Action.ALLSKY.doc() + "\n"+
            "   TREE       "+Action.TREE.doc() + "\n"+
            "   MAPTILES   "+Action.MAPTILES.doc() + "\n"+
            "   CONCAT     "+Action.CONCAT.doc() + "\n"+
            "   CUBE       "+Action.CUBE.doc() + "\n"+
            "   GZIP       "+Action.GZIP.doc() + "\n"+
            "   CLEANFITS  "+Action.CLEANFITS.doc() + "\n"+
            "   DETAILS    "+Action.DETAILS.doc() + "\n"+
            "   MIRROR    "+Action.MIRROR.doc() + "\n"
            );
      System.out.println("\nEx: java -jar "+launcher+" in=/MyImages    => Do all the job." +
            "\n    java -jar "+launcher+" in=/MyImages bitpix=16 pixelCut=\"-1 100 log\" => Do all the job" +
            "\n           The FITS tiles will be coded in short integers, the preview tiles" +
            "\n           will map the physical values [-1..100] with a log function contrast in [0..255]." +
            "\n    java -jar "+launcher+" in=/MyImages blank=0 border=\"100 50 100 50\" mode=REPLACETILE   " + "\n" +
            "           => recompute tiles. The original pixels in the border or null will be ignored."+
            "\n    java -jar "+launcher+" in=HiPS out=HiPStarget CONCAT => Concatenate HiPS to HiPStarget"
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
