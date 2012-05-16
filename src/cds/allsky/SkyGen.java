package cds.allsky;

import java.awt.image.ColorModel;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Properties;
import java.util.Set;

import cds.aladin.ColorMap;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

public class SkyGen {

   private File file;
   private Context context;

   int order = -1;
   private Action action;

   public SkyGen() {
      this.context = new Context();
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
      Properties properties = new Properties();
      Reader reader = new FileReader(file);
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
    * Lance quelques vérifications de cohérence entre les options données
    * 
    * @throws Exception
    *             si une incohérence des options nécessite une interrption du
    *             programme
    */
   private void validateContext() throws Exception {
      // ---- Qq vérifications
      
      // arguments des répertoires de départ
      if ( (action==Action.TILES || action==Action.INDEX || action==null) && context.getInputPath() == null) {
         throw new Exception("Argument \"input\" is missing");
         // Par défaut le répertoire courant
         //context.setInputPath(System.getProperty("user.dir"));
      }
      // Par défaut le répertoire courant
      if (context.getOutputPath() == null && context.getInputPath()!=null ) context.setOutputPath(context.getInputPath() + Constante.ALLSKY);

      // Deuxième vérif
      if (context.getOutputPath() == null) {
         throw new Exception("Argument \"output\" is missing");
      }
      context.info("HEALPix survey directory: "+context.getOutputPath());
      
      if( action==Action.JPEG || action==action.ALLSKY || action==action.MOC 
            || action==action.GZIP || action==action.GUNZIP ) {
         if( !context.isExistingAllskyDir() ) throw new Exception("HEALPix survey not found!");
      }
      
      // données déjà présentes ?
      if ( (action==Action.TILES || action==Action.INDEX || action==null) && !context.isExistingDir()) {
         throw new Exception("Input dir does NOT exist : " + context.getInputPath());
      }
      if (context.isExistingAllskyDir()) {
         if (context.getCoAddMode() == null) {
            if( action==Action.TILES || action==null ) context.warning("Default behaviour for computing pixels already computed : " + CoAddMode.getDefault());
            context.setCoAddMode(CoAddMode.getDefault());
         }
      }
      // à l'inverse, si il y a l'option "pixel"
      // ca laisse sous entendre que l'utilisateur pensait avoir dejà des
      // données
      else if (context.getCoAddMode() != null) {
         context.warning("There is NO already computed tiles, option " + context.getCoAddMode() + " will be ignored");
      }
      

      // si on n'a pas d'image etalon, on la cherche + initialise avec
      if ( (action==null || action==Action.TILES || action==Action.INDEX || action==Action.ALLSKY || action==Action.JPEG) 
            && context.getImgEtalon()==null ) {
         
         double memoCut[] = context.getCut();
         double memoCutOrig[] = context.getCutOrig();
         boolean found=false;
         if( context.getInputPath()!=null ) {
           found = context.findImgEtalon(context.getInputPath());
            if (!found) {
               String msg = "There is no available images in source directory: " + context.getInputPath();
               context.warning(msg);
               throw new Exception(msg);
            }
         // On va déterminer les cuts par les tuiles déjà construites
         } else if( context.getOutputPath()!=null ) {
            String s = context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Dir0";
            found = context.findImgEtalon(s);
            if (!found) {
               String msg = "There is no tiles images in output directory ("+s+")";
               throw new Exception(msg);
            } else {
               context.warning("Using tiles images as reference image");
            }
         }
         
         // On remet le cut qui avait été explicitement indiqué en paramètre
         if( found ) {
            if( memoCut!=null ) context.setCut(memoCut);
            if( memoCutOrig!=null ) context.setCutOrig(memoCutOrig);
         }

         if( action==null || action==Action.TILES || action==Action.INDEX || action==Action.JPEG ) {
            Fits file = new Fits();
            try {
               file.loadHeaderFITS(context.getImgEtalon());
               // calcule le meilleur nside/norder
               long nside = healpix.core.HealpixIndex.calculateNSide(file.getCalib().GetResol()[0] * 3600.);
               order = ((int) Util.order((int) nside) - Constante.ORDER);
            } catch (Exception e) {
              throw new Exception("Reference image calibration error ("+context.getImgEtalon()+")");
            }
         }
      }
      
      // si le numéro d'order donné est différent de celui calculé
      // attention n'utilise pas la méthode context.getOrder car elle a un default à 3
      if (order != context.order && -1 != context.order) {
         context.warning("The number of levels (" + context.getOrder() + ") is not optimal (" + order + ") => sub (or over) sample will be applied");
      } else {
         context.setOrder(order);
      }
      // si le bitpix donné est différent de celui calculé
      if (context.getBitpix() != context.getBitpixOrig() && (action==null || action==Action.TILES)) {
         context.warning("The provided BITPIX (" + context.getBitpix() + ") is different than the original one (" + context.getBitpixOrig() + ") => bitpix conversion will be applied");
      }

      // il faut au moins un cut (ou img) pour construire des JPEG ou ALLSKY
      if (context.getCut()==null && (action==Action.JPEG || action==Action.ALLSKY || context.getBitpix() != context.getBitpixOrig()))
         throw new Exception("Range cuts unknown: option \"img\" or \"pixelCut\" are required");
      
      if( action==null || action==Action.TILES || action==Action.INDEX || action==Action.JPEG || action==Action.ALLSKY ) {
         context.initParameters();
         if( !context.verifCoherence() ) throw new Exception("Uncompatible pre-existing HEALPix survey => choose another output directory");
         if( context.getBScale()==0 ) throw new Exception("Big bug => BSCALE=0 !! please contact CDS");
      }
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
      if(opt.equalsIgnoreCase("h"))
         usage();
      else if (opt.equalsIgnoreCase("verbose"))
         Context.setVerbose(Integer.parseInt(val));
      else if (opt.equalsIgnoreCase("debug")) {
         if (Boolean.parseBoolean(val)) Context.setVerbose(4);
      }
      else if (opt.equalsIgnoreCase("input"))
         context.setInputPath(val);
      else if (opt.equalsIgnoreCase("output"))
         context.setOutputPath(val);
      else if (opt.equalsIgnoreCase("blank"))
         context.setBlankOrig(Double.parseDouble(val));
      else if (opt.equalsIgnoreCase("order"))
         context.setOrder(Integer.parseInt(val));
      else if (opt.equalsIgnoreCase("pixelGeneration") || opt.equalsIgnoreCase("pixel"))
         context.setCoAddMode(CoAddMode.valueOf(val.toUpperCase()));
      else if (opt.equalsIgnoreCase("tileGeneration")) {
         if (CoAddMode.valueOf(val.toUpperCase()) == CoAddMode.OVERWRITE)
            context.setCoAddMode(CoAddMode.REPLACEALL);
         else if (CoAddMode.valueOf(val.toUpperCase()) == CoAddMode.KEEP)
            context.setCoAddMode(CoAddMode.KEEPALL);
      }
      else if (opt.equalsIgnoreCase("bitpix"))
         context.setBitpix(Integer.parseInt(val));
      else if (opt.equalsIgnoreCase("region")) {
         if (val.endsWith("fits")) {
            HealpixMoc moc = new HealpixMoc();
            moc.read(val);
            context.setMoc(moc);
         }
         else context.setMoc(val);
      }
      else if (opt.equalsIgnoreCase("frame"))
         context.setFrameName(val);
      else if (opt.equalsIgnoreCase("skyval"))
         context.setSkyval(val);
      else if (opt.equalsIgnoreCase("border"))
         try {
            context.setBorderSize(val);
         } catch (ParseException e) {
            throw new Exception(e.getMessage());
         }
         else if (opt.equalsIgnoreCase("pixelCut"))
            context.setPixelCut(val);
         else if (opt.equalsIgnoreCase("method"))
            context.setMethod(val);
         else if (opt.equalsIgnoreCase("dataCut")) {
            context.setDataCut(val);
         }
         else if (opt.equalsIgnoreCase("color"))
            context.setColor(Boolean.parseBoolean(val));
         else if (opt.equalsIgnoreCase("img")) {
            context.setImgEtalon(val);
         }
         else throw new Exception("Option unknown [" + opt + "]");
      
   }

   enum Action {
      INDEX, TILES, JPEG, MOC, MOCINDEX, ALLSKY, GZIP, GUNZIP,
      FINDER  // Pour compatibilité
   }

   public static void main(String[] args) {
      SkyGen generator = new SkyGen();
      int length = args.length;
      if (length == 0) {
         usage();
         return;
      }
      // extrait les options en ligne de commande, et les analyse
      for (String arg : args) {
         // si c'est dans un fichier
         String param = "-param=";
         if (arg.startsWith(param)) {
            try {
               generator.setConfigFile(arg.substring(param.length()));
            } catch (Exception e) {
               e.printStackTrace();
               return;
            }
            continue;
         }
         // help
         else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
            SkyGen.usage();
            return;
         }
         // debug
         else if (arg.equalsIgnoreCase("-debug") || arg.equalsIgnoreCase("-d")) {
            Context.setVerbose(4);
         }
         // toutes les autres options écrasent les précédentes
         else if (arg.contains("=")) {
            String[] opts = arg.split("=");
            try {
               // si il y a un - on l'enlève
               opts[0] = opts[0].substring(opts[0].indexOf('-') + 1);

               generator.setContextFromOptions(opts[0], opts[1]);
            } catch (Exception e) {
               generator.context.error(e.getMessage());
               return;
            }
         }
         // les autres mots sont supposées des actions (si +ieurs, seule la
         // dernière est gardée)
         else {
            try {
               generator.action = Action.valueOf(arg.toUpperCase());
               if( generator.action==Action.FINDER ) generator.action=Action.INDEX;   // Pour compatibilité
            } catch (Exception e) {
               e.printStackTrace();
               return;
            }
         }

      }

      generator.context.action(""+(generator.action==null?"All steps (index+tiles+jpeg+allsky+moc)":generator.action));

      try {
         generator.validateContext();
      } catch (Exception e) {
         generator.context.error(e.getMessage());
         return;
      }
      // lance les calculs
      generator.start();
   }
   
   private void start() {
      context.setIsRunning(true);
      context.loadProp();
      Task task = new Task(context);
      
      if (action==null) {
         // aucune action définie -> on fait la totale
         action = Action.INDEX;
         start();
         action = Action.TILES;
         start();
         action = Action.JPEG;
         start();
         action = Action.MOC;
         start();
         context.done("The end");
         return;
      }

      switch (action) {
         case INDEX : {
            context.running("Creating HEALPix index (depth="+context.getOrder()+")...");
            BuilderIndex builder = new BuilderIndex(context);
            File f = new File(context.getHpxFinderPath()+Util.FS+"Norder"+order);
            if (context.isIndexDone(f)) {
               context.info("Found an existing index => use it \"as is\"");
               builder.loadMoc();
               if( context.getNbLowCells()!=-1 ) {
                  context.info("Found an index MOC covering "+context.getNbLowCells()+" low level HEALPix cells");
               }

            } else {
               ThreadProgressBar progressBar = new ThreadProgressBar(builder);
               (new Thread(progressBar)).start();
               // laisse le temps au thread de se lancer
               try {
                  Thread.sleep(200);
               } catch (InterruptedException e) {
               }
               builder.build();
               progressBar.stop();
               context.doneIndex();
            }
            break;
         }
         case JPEG : {
            context.running("Creating Jpeg tiles...");
            // Calcule d'une fonction de transfert si besoin
            ColorModel cm = null;
            if (context.fct!=null) {
               cm = ColorMap.getCM(0, 128, 255,false, 0/*PlanImage.CMGRAY*/, context.fct.code());
            }
            BuilderJpg builder = new BuilderJpg(cm, context);
            double cut [] = context.getCut();
            String fct = context.getTransfertFct();
            context.info("Will map pixel range ["+cut[0]+" .. "+cut[1]+"] to [0..255] ("+fct+")");
            ThreadProgressBar progressBar = new ThreadProgressBar(builder);
            (new Thread(progressBar)).start();
            // laisse le temps au thread de se lancer
            try {
               Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            builder.run();
            progressBar.stop();
            context.doneJpeg();
            // la construction du allsky est automatiquement faite par le builder
            break;
         }
         case ALLSKY : {
            context.running("Creating Allsky views (FITS and JPEG if possible)...");
            double cut [] = context.getCut();
            context.info("Pixel range=["+cut[2]+" .. "+cut[3]+"] cut=["+cut[0]+" .. "+cut[1]+"]");
            BuilderAllsky builder = new BuilderAllsky(context, -1);
            try {
               builder.createAllSky(3, 64);
               if (context.getCut()!=null) builder.createAllSkyJpgColor(3,64,false);
               context.doneAllsky();
            } catch (Exception e) {
               context.error(e.getMessage());
//               e.printStackTrace();
               System.exit(0);
            }
            break;
         }
         case MOC : {
            context.running("Creating MOC covering generated tiles)...");
            BuilderMoc builder = new BuilderMoc();
            builder.createMoc(context.outputPath);
            context.doneMoc();
            break;
         }
         case MOCINDEX : {
            context.running("Creating MOC covering HEALPix index)...");
            BuilderMoc builder = new BuilderMoc();
            builder.createMoc(context.getHpxFinderPath());
            context.doneMocIndex();
            break;
         }
         case GZIP : {
            context.running("Gzipping all FITS tiles...");
            BuilderGzip gz = new BuilderGzip(context);
            gz.gzip();
            context.doneGzip();
            break;
         }
         case GUNZIP : {
            context.running("Gunzipping all FITS tiles...");
            BuilderGzip gz = new BuilderGzip(context);
            gz.gunzip();
            context.doneGunzip();
            break;
         }
         case TILES : {
            context.running("Creating FITS tiles and allsky (max depth="+context.getOrder()+")...");
            if( context.getNbLowCells()==-1 ) {
               (new BuilderIndex(context)).loadMoc();  // pour connaitre le nombre de cellules à calculer
               if( context.getNbLowCells()!=-1 ) {
                  context.info("Found an index MOC covering "+context.getNbLowCells()+" low level HEALPix cells");
               }
            }
            
            // Un peu de baratin
            int b0=context.getBitpixOrig(), b1=context.getBitpix();
            if( b0!=b1 ) {
               context.info("BITPIX conversion from "+context.getBitpixOrig()+" to "+context.getBitpix());
               double cutOrig[] = context.getCutOrig();
               double cut[] = context.getCut();
               context.info("Will map original pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] to ["+cut[2]+" .. "+cut[3]+"]");
            }
            else context.info("BITPIX = "+b1+" (no conversion)");
            if( context.getDiskMem()!=-1 ) {
               context.info("Disk requirement (high approximation) : "+cds.tools.Util.getUnitDisk(context.getDiskMem()*1.25));
            }
            double bs=context.getBScale(), bz=context.getBZero();
            if( bs!=1 || bz!=0 ) { context.info("BSCALE="+bs+" BZERO="+bz); }
            double bl0 = context.getBlankOrig();
            double bl1 = context.getBlank();
            if( context.hasAlternateBlank() ) context.info("BLANK conversion from "+(Double.isNaN(bl0)?"NaN":bl0)+" to "+(Double.isNaN(bl1)?"NaN":bl1));
            else context.info("BLANK="+ (Double.isNaN(bl1)?"NaN":bl1));
            
            BuilderController builder = new BuilderController(task,context);
            ThreadProgressBar progressBar = new ThreadProgressBar(builder);
            (new Thread(progressBar)).start();
            // laisse le temps au thread de se lancer
            try {
               Thread.sleep(200);
            } catch (InterruptedException e) {
            }
            try {
               builder.build();
            } catch (Exception e) {
//               e.printStackTrace();
               context.error(e.getMessage());
               System.exit(0);
            } finally {
               progressBar.stop();
            }
            context.doneTiles();
            
            // force à recréer le allsky
//            action = Action.ALLSKY;
//            start();
            break;
         }

       }
      context.setIsRunning(false);
   }

   private static void usage() {
      System.out.println("SkyGen -param=configfile\n");
      System.out.println("This configfile must contains these following options, or use them in comand line :");
      System.out.println(
            "input     Directory of original images (fits or jpg+hhh)" + "\n" +
            "output    Target directory (default $PWD+\"ALLSKY\")" + "\n" +
            "pixel     keep|keepall|overwrite|average|replaceall - in case of already computed values (default overwrite)" + "\n" +
            "method    Level up pixel computation for Jpeg (and jpeg color) : median|mean (default is median)" + "\n" +
            "region    Healpix region to compute (ex: 3/34-38 50 53) or Moc.fits file (nothing means all the sky)" + "\n" +
            "blank     BLANK value alternative (use of FITS header by default)" + "\n" +
            "border    Margins to ignore in the original images (N W S E or constant)" + "\n" +
            "frame     Healpix frame (C or G - default C for ICRS)" + "\n" +
            "skyval    Fits key to use for removing sky background" + "\n" +
            "bitpix    Target bitpix (default is original one)" + "\n" +
            "order     Number of Healpix Order (default computed from the original resolution)" + "\n" +
            "pixelCut  Display range cut and optionnaly a transfert function name (BSCALE,BZERO applied)(required JPEG 8 bits conversion - ex: \"120 140 log\")" + "\n" +
            "dataCut   Range for pixel vals (BSCALE,BZERO applied)(required for bitpix conversion - ex: \"-32000 +32000\")" + "\n" +
            "color     True if your input images are colored jpeg (default is false)" + "\n" +
            "img       Image path to use for initialization (default is first found)" + "\n" +
            "verbose   Show live statistics : tracelevel from -1 (nothing) to 4 (a lot)" + "\n" +
            "debug     true|false - to set output display as te most verbose or just statistics" + "\n");
      System.out.println("\nUse one of these actions at end of command line :" + "\n" +
            "index     Build finder index" + "\n" +
            "tiles     Build Healpix tiles" + "\n" +
            "jpeg      Build JPEG tiles from original tiles" + "\n" +
            "moc       Build MOC (based on generated tiles)" + "\n" +
            "mocindex  Build MOC (based on HEALPix index)" + "\n" +
            "allsky    Build Allsky.fits and Allsky.jpg fits pixelCut exists (even if not used)" + "\n"
//            + "gzip      gzip all fits tiles and Allsky.fits (by keeping the same names)" + "\n"
//            + "gunzip    gunzip all fits tiles and Allsky.fits (by keeping the same names)"
            );
   }

   private void setConfigFile(String configfile) throws Exception {
      this.file = new File(configfile);
      parseConfig();
   }

   class ThreadProgressBar implements Runnable {
      Progressive builder;
      boolean isRunning = false;
      public ThreadProgressBar(Progressive builder) {
         this.builder = builder;
      }

      public void run() {
         isRunning=true;
         while (isRunning) {
            context.setProgress(this.builder.getProgress());
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
         }

      }
      public void stop() {
//         context.setProgress(this.builder.getProgress());
         isRunning=false;
      }
   }
}
