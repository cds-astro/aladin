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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import cds.aladin.Aladin;
import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.allsky.Context.JpegMethod;
import cds.moc.HealpixMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

public class HipsGen {

   private File file;
   private boolean force=false;
   private boolean flagMode=false;
   private boolean flagConcat=false;
   private boolean flagMirror=false;
   private boolean flagUpdate=false;
   private boolean flagLint=false;
   private boolean flagMethod=false;
   private boolean flagRGB=false;
   private boolean flagMapFits=false;
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
      InputStreamReader reader = new InputStreamReader( new BufferedInputStream( new FileInputStream(file) ));
      properties.load(reader);

//      Set<Object> keys = properties.keySet();
//      for (Object opt : keys) {
         
      for( String opt : properties.getKeys() ) {
         String val = properties.getProperty(opt);

         try {
            setContextFromOptions(opt, val);
         } catch (Exception e) {
            e.printStackTrace();
            break;
         }
      }

      reader.close();
   }
   
   
   /** Retourne le paramètre qui remplace un paramètre devenu obsolète, null sinon */
   private String obsolete(String s) {
      
      if( s.equalsIgnoreCase("ivorn") )      return "creator_did";
      if( s.equalsIgnoreCase("id") )         return "creator_did";
      if( s.equalsIgnoreCase("input") )      return "in";
      if( s.equalsIgnoreCase("output") )     return "out";
      if( s.equalsIgnoreCase("pixel") )      return "mode";
      if( s.equalsIgnoreCase("moc") )        return "region";
      if( s.equalsIgnoreCase("blocking") )   return "partitioning";
      if( s.equalsIgnoreCase("cutting") )    return "partitioning";
      if( s.equalsIgnoreCase("polygon") )    return "fov";
      if( s.equalsIgnoreCase("jpegMethod") ) return "method";
      if( s.equalsIgnoreCase("dataCut") )    return "hips_data_range";
      if( s.equalsIgnoreCase("pixelRange") ) return "hips_data_range";
      if( s.equalsIgnoreCase("pixelCut") )   return "hips_pixel_cut";
      if( s.equalsIgnoreCase("histoPercent"))return "skyval";
      if( s.equalsIgnoreCase("publisher") )  return "creator";
      if( s.equalsIgnoreCase("label") )      return "obs_title";
      if( s.equalsIgnoreCase("publisher") )  return "hips_creator";
      if( s.equalsIgnoreCase("creator") )    return "hips_creator";
      if( s.equalsIgnoreCase("pixel") )      return "mode";
      if( s.equalsIgnoreCase("blocking") )   return "partitioning";
      if( s.equalsIgnoreCase("cutting") )    return "partitioning";
      if( s.equalsIgnoreCase("circle") )     return "radius";
      if( s.equalsIgnoreCase("status") )     return "hips_status";
      if( s.equalsIgnoreCase("order") )      return "hips_order";
      if( s.equalsIgnoreCase("minOrder") )   return "hips_min_order";
      if( s.equalsIgnoreCase("frame") )      return "hips_frame";
      
      return null;
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
      
      String alt=obsolete(opt);
      if( alt!=null ) {
         context.warning("Deprecated parameter, use \""+alt+"\"");
         opt=alt;
      }

      // System.out.println(opt +" === " +val);
      if( opt.equalsIgnoreCase("h")) {
         usage(launcher);
      } else if (opt.equalsIgnoreCase("verbose"))      { Context.setVerbose(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("blank"))        { context.setBlankOrig(Double.parseDouble(val));
      } else if (opt.equalsIgnoreCase("hips_order"))   { context.setOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("mocOrder"))     { context.setMocOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("nside"))        { context.setMapNside(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("tileOrder"))    { context.setTileOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("hips_tile_width"))  { context.setTileOrder((int)CDSHealpix.log2( Integer.parseInt(val)));
      } else if (opt.equalsIgnoreCase("bitpix"))       { context.setBitpix(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("hips_frame"))   { context.setFrameName(val);
      } else if (opt.equalsIgnoreCase("maxThread"))    { context.setMaxNbThread(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("skyval"))       { context.setSkyval(val);
      } else if (opt.equalsIgnoreCase("skyvalues"))    { context.setSkyValues(val);
      } else if (opt.equalsIgnoreCase("exptime"))      { context.setExpTime(val);
      } else if (opt.equalsIgnoreCase("fading"))       { context.setFading(val); 
      } else if (opt.equalsIgnoreCase("mixing"))       { context.setMixing(val);
      } else if (opt.equalsIgnoreCase("color"))        { context.setColor(val);
      } else if (opt.equalsIgnoreCase("inRed"))        { context.setRgbInput(val, 0); flagRGB=true;
      } else if (opt.equalsIgnoreCase("inGreen"))      { context.setRgbInput(val, 1); flagRGB=true;
      } else if (opt.equalsIgnoreCase("inBlue"))       { context.setRgbInput(val, 2); flagRGB=true;
      } else if (opt.equalsIgnoreCase("cmRed"))        { context.setRgbCmParam(val, 0);
      } else if (opt.equalsIgnoreCase("cmGreen"))      { context.setRgbCmParam(val, 1);
      } else if (opt.equalsIgnoreCase("cmBlue"))       { context.setRgbCmParam(val, 2);
      } else if (opt.equalsIgnoreCase("img"))          { context.setImgEtalon(val);
      } else if (opt.equalsIgnoreCase("fitskeys"))     { context.setIndexFitskey(val);
      } else if (opt.equalsIgnoreCase("hips_status"))  { context.setStatus(val);
      } else if (opt.equalsIgnoreCase("target"))       { context.setTarget(val);
      } else if (opt.equalsIgnoreCase("targetRadius")) { context.setTargetRadius(val);
      } else if (opt.equalsIgnoreCase("label"))        { context.setLabel(val);
      } else if (opt.equalsIgnoreCase("filter"))       { context.setFilter(val);
      } else if (opt.equalsIgnoreCase("hdu"))          { context.setHDU(val);
      } else if (opt.equalsIgnoreCase("hips_creator")) { context.setCreator(val);
      } else if (opt.equalsIgnoreCase("creator_did"))  { context.setHipsId(val);
      } else if (opt.equalsIgnoreCase("debug"))        { if (Boolean.parseBoolean(val)) Context.setVerbose(4);
      } else if (opt.equalsIgnoreCase("in"))           { context.setInputPath(val);
      } else if (opt.equalsIgnoreCase("out") )         { context.setOutputPath(val);
      } else if (opt.equalsIgnoreCase("mode"))         { context.setMode(Mode.valueOf(val.toUpperCase())); flagMode=true;
      } else if( opt.equalsIgnoreCase("partitioning")) { context.setPartitioning(val);
      } else if( opt.equalsIgnoreCase("tileTypes") )   { context.setTileTypes(val);
      } else if( opt.equalsIgnoreCase("shape") )       { context.setShape(val);
      } else if ( opt.equalsIgnoreCase("method"))      { context.setMethod(val); flagMethod=true;
      } else if (opt.equalsIgnoreCase("histoPercent")) { context.setHistoPercent(val);
      } else if (opt.equalsIgnoreCase("pixelGood"))    { context.setPixelGood(val);
      } else if (opt.equalsIgnoreCase("hips_pixel_cut"))  { context.setPixelCut(val);
      } else if (opt.equalsIgnoreCase("hips_data_range")) { context.setDataCut(val);
      } else if (opt.equalsIgnoreCase("hips_min_order"))  { context.setMinOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("region")) {
         if (val.endsWith("fits")) {
            HealpixMoc moc = new HealpixMoc();
            moc.read(val);
            context.setMocArea(moc);
         } else context.setMocArea(val);
      } else if( opt.equalsIgnoreCase("maxRatio")) {
         try {  context.setMaxRatio(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
      } else if( opt.equalsIgnoreCase("radius")) {
         try {  context.setCircle(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
      } else if( opt.equalsIgnoreCase("fov") ) {
         try {  context.setPolygon(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
      } else if (opt.equalsIgnoreCase("border")) {
         try { context.setBorderSize(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
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
         
         // Mémorisation de la commande
         String q = Tok.quote(arg);
         if( context.scriptCommand==null ) context.scriptCommand=q;
         else context.scriptCommand+=" "+q;

         // debug
         if (arg.equalsIgnoreCase("-debug") || arg.equalsIgnoreCase("-d")) Context.setVerbose(4);
         else if (arg.equalsIgnoreCase("-fast") ) context.mixing=true;
         else if (arg.equalsIgnoreCase("-force") || arg.equalsIgnoreCase("-f") )  force=true;
         else if (arg.equalsIgnoreCase("-nice") ) context.mirrorDelay=500;
         else if (arg.equalsIgnoreCase("-notouch") ) context.notouch=true;
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
               if( a==Action.MIRROR ) flagMirror=true;
               if( a==Action.UPDATE ) flagUpdate=true;
               if( a==Action.LINT )   flagLint=true;
               if( a==Action.CONCAT ) {
                  flagConcat=true;
                  if( !flagMode ) context.setMode(Mode.AVERAGE);
               }
               if( a==Action.ABORT ) flagAbort=true;    // Bidouillage pour pouvoir tuer un skygen en cours d'exécution
               if( a==Action.PAUSE ) flagPause=true;    // Bidouillage pour pouvoir mettre en pause un skygen en cours d'exécution
               if( a==Action.RESUME ) flagResume=true;  // Bidouillage pour pouvoir remettre en route un skygen en pause
               actions.add(a);
            } catch (Exception e) {
               context.error("Unknown parameter ["+arg+"] !");
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
            flagMapFits=false;
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
      
      // Vérification de l'ID
      try {
         // Si inconnu, je vais essayé de le récupérer depuis le fichier des propriétés
         if( context.hipsId==null && context.getOutputPath()!=null ) {
            
            try {
               String propFile = context.getOutputPath()+Util.FS+Constante.FILE_PROPERTIES;
               MyProperties prop = new MyProperties();
               File f = new File( propFile );
               if( f.exists() ) {
                  InputStreamReader in = new InputStreamReader( new BufferedInputStream( new FileInputStream(propFile) ));
                  prop.load(in);
                  in.close();
                  String s = prop.getProperty(Constante.KEY_CREATOR_DID);
                  if( s!=null ) context.setHipsId(s);
               }
            } catch( Exception e ) { }
         }
         
         if( !flagConcat && !flagMirror && !flagUpdate && !flagLint) {
            String s = context.checkHipsId(context.hipsId);
            context.setHipsId(s);
         }
      } catch (Exception e) {
         context.error(e.getMessage());
         return;
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
               else if( a==Action.RGB )     { actions.add(i, Action.CLEAN);        i++; }
               else if( a==Action.CUBE )    { actions.add(i, Action.CLEAN);        i++; }
            }
         }
      }

      if( context.fake ) context.warning("NO RUN MODE (option -n), JUST PRINT INFORMATION !!!");
      for( Action a : actions ) {
         context.info("Action => "+a+": "+a.doc());
         if( !flagMapFits && a==Action.MAPTILES ) flagMapFits=true;
      }

      // Positionnement du frame par défaut
      if( !flagRGB && !flagMapFits ) setDefaultFrame();

      // C'est parti
      try {
         
          long t = System.currentTimeMillis();
         new Task(context,actions,true);
         if( context.isTaskAborting() ) context.abort(context.getTitle("(aborted after "+Util.getTemps(System.currentTimeMillis()-t),'='));
         else {
            if( !flagMirror ) {
               String id = context.getHipsId();
               if( id==null || id.startsWith("ivo://UNK.AUT") ) {
                  context.warning("a valid HiPS IVOID identifier is strongly recommended => in the meantime, assuming "+context.getHipsId());

               }
               context.info("Tip: Edit the \"properties\" file for describing your HiPS (full description, copyright, ...)");
            }
            context.done(context.getTitle("THE END (done in "+Util.getTemps(System.currentTimeMillis()-t),'='));
         }
         
      } catch (Exception e) {
         if( context.getVerbose()>0 ) e.printStackTrace();
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
            InputStreamReader in = new InputStreamReader( new BufferedInputStream( new FileInputStream(propFile) ));
            prop.load(in);
            in.close();
            String s =prop.getProperty(Constante.KEY_HIPS_FRAME);
            if( s==null ) s =prop.getProperty(Constante.OLD_HIPS_FRAME);

            // Good trouvé !
            if( s!=null && s.length()>0 ) frame=s;

            // pas de propriété hips_frame positionnée => galactic
            else frame=force?"equatorial":"galactic";

            // Pas trouvé ! si le HiPS existe déjà, alors c'est pas défaut du galactic
            // sinon de l'equatorial
         } else {
            if( context.isExistingAllskyDir() ) frame=force?"equatorial":"galactic";
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
                  "   in=dir                  Source image directory (FITS or JPEG|PNG +hhh or HiPS),\n"+
                  "                           unique image or HEALPix map file" + "\n" +
                  "\n"+
                  "Basic optional parameters:\n"+
                  "   out=dir                 HiPS target directory (default $PWD+\""+Constante.HIPS+"\")" + "\n" +
                  "   obs_title=name          Name of the survey (by default, input directory name)" + "\n"+
                  "   creator_did=id          HiPS identifier (syntax: [ivo://]AUTHORITY/internalID)" + "\n"+
                  "   hips_creator=name       Name of the person|institute who builds the HiPS" + "\n"+
                  "   hips_status=xx          HiPS status (private|public clonable|clonableOnce|unclonable)\n" +
                  "                          (default: public clonableOnce)\n" +
                  "   hdu=n1,n2-n3,...|all    List of HDU numbers (0 is the primary HDU - default is 0)\n" +
                  "   blank=nn                Specifical BLANK value" + "\n" +
                  "   skyval=key|true|%info|%min %max   Fits key to use for removing a sky background, true auto detection " + "\n" +
                  "                           or percents of pixel histogram kept (central ex 99, or min max ex 0.3 99.7)" + "\n" +
                  "   color=jpeg|png          The source images are colored images (jpg or png) and the tiles will be " + "\n" +
                  "                           produced in jpeg (resp. png)" + "\n" +
                  "   shape=...               Shape of the observations (ellipse|rectangle)" + "\n" +
                  "   border=...              Margins (in pixels) to ignore in the original observations (N W S E or " + "\n" +
                  "                           constant)" + "\n" +
                  "   fov=true|x1,y1..        Observed regions by files.fov or global polygon (in FITS convention)." + "\n" +
                  "   verbose=n               Debug information from -1 (nothing) to 4 (a lot)" + "\n"+
                  "   -live                   incremental HiPS (keep weight associated to each HiPS pixel)" + "\n"+
                  "   -f                      clear previous computations\n"+
                  "   -n                      Just print process information, but do not execute it.\n"+
                  "\n"+
                  "Advanced optional parameters:\n"+
                  "   hips_order=nn           Specifical HEALPix order - by default, adapted to the original resolution" + "\n" +
                  "   hips_pixel_bitpix=nn    Specifical target bitpix (-64|-32|8|16|32|64)" + "\n" +
                  "   hips_pixel_cut=min max  Specifical pixel cut and/or transfert function for PNG/JPEG 8 bits\n" +
                  "                           conversion - ex: \"120 140 log\")" + "\n" +
                  "   hips_data_range=min max Specifical pixel value range (required for bitpix\n" +
                  "                           conversion, or for removing bad pixels - ex: \"-5 110\")" + "\n" +
                  "   pixelGood=min [max]     Range of pixel values kept" + "\n" +
                  "   img=file                Specifical reference image for default initializations \n" +
                  "                           (BITPIX,BSCALE,BZERO,BLANK,order,pixelCut,pixelRange)" + "\n" +
                  "   mode=xx                 Coadd mode when restart: pixel level(OVERWRITE|KEEP|ADD|AVERAGE) \n" +
                  "                           or tile level (REPLACETILE|KEEPTILE) - (default OVERWRITE)" + "\n" +
                  "                           Or LINK|COPY for CUBE action (default COPY)" + "\n" +
                  "   fading=true|false       False to avoid fading effect on overlapping original images " + "\n" +
                  "                           (default is false)" + "\n" +
                  "   mixing=true|false       False to avoid mixing (and fading) effect on overlapping original images " + "\n" +
                  "                           (default is true)" + "\n" +
                  "   partitioning=true|false True for cutting large original images in blocks of 1024x1024 " + "\n" +
                  "                           (default is true)" + "\n" +
                  "   region=moc              Specifical HEALPix region to compute (ex: 3/34-38 50 53)\n" +
                  "                           or Moc.fits file (all sky by default)" + "\n" +
                  "   maxRatio=nn             Max height/width pixel ratio tolerated for original obs " + "\n" +
                  "                           (default 2, 0 for removing the test)" + "\n" +
                  "   fitskeys=list           Fits key list (blank separator) designing metadata FITS keyword value " + "\n" +
                  "                           to memorized in the HiPS index" + "\n" +
                  "   hips_min_order=nn       Specifical HEALPix min order (only for DETAILS action)" + "\n" +
                  "   method=m                Method (MEDIAN|MEAN|FIRST) (default MEDIAN) for aggregating colored " + "\n" +
                  "                           compressed tiles (JPEG|PNG)" + "\n" +
                  "   hip_frame               Target coordinate frame (equatorial|galactic)" + "\n" +
                  "   hips_tile_width=nn      Specifical tile width (pow of 2) - default 512" + "\n" +
                  "   mocOrder=nn             Specifical HEALPix MOC order (only for MOC action) - by default " + "\n" +
                  "                           auto-adapted to the HiPS" + "\n" +
                  "   nside=nn                HEALPix map NSIDE (only for MAP action) - by default 2048" + "\n" +
                  "   exptime=key             Fits key to use for adjusting variation of exposition" + "\n" +
                  "   inRed                   HiPS red path component (RGB action)\n" +
                  "   inGreen                 HiPS green path component (RGB action)\n" +
                  "   inBlue                  HiPS blue path component (RGB action)\n" +
                  "   cmRed                   Colormap parameters for HiPS red component (min [mid] max [fct])\n" +
                  "   cmGreen                 Colormap parameters for HiPS green component (min [mid] max [fct])\n" +
                  "   cmBlue                  Colormap parameters for HiPS blue component (min [mid] max [fct])\n" +
                  "   filter=gauss            Gaussian filter applied on the 3 input HiPS (RGB action)" + "\n" +
                  "   tileTypes               List of tile format to copy (MIRROR action)" + "\n" +
                  "   maxThread=nn            Max number of computing threads" + "\n" +
                  "   target=ra +dec          Default HiPS target (ICRS deg)" + "\n"+
                  "   targetRadius=rad        Default HiPS radius view (deg)" + "\n"+
                  "   -notouch                Do not touch the hips_release_date" + "\n"+
                  "   -nice                   Slow download for avoiding to overload remote http server (dedicated " + "\n" +
                  "                           to MIRROR action)" + "\n"
                  //          "   debug=true|false   to set output display as te most verbose or just statistics" + "\n" +
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
            "   APPEND     "+Action.APPEND.doc() + "\n"+
            "   CONCAT     "+Action.CONCAT.doc() + "\n"+
            "   CUBE       "+Action.CUBE.doc() + "\n"+
            "   GZIP       "+Action.GZIP.doc() + "\n"+
            "   CLEANFITS  "+Action.CLEANFITS.doc() + "\n"+
            "   DETAILS    "+Action.DETAILS.doc() + "\n"+
            "   MAP        "+Action.MAP.doc() + "\n" +
            "   MIRROR     "+Action.MIRROR.doc() + "\n"+
            "   LINT       "+Action.LINT.doc() + "\n"
            );
      System.out.println("\nEx: java -jar "+launcher+" in=/MyImages    => Do all the job." +
            "\n    java -jar "+launcher+" in=/MyImages hips_pixel_bitpix=16 hips_pixel_cut=\"-1 100 log\"" +
            "\n           The FITS tiles will be coded in short integers, the preview tiles" +
            "\n           will map the physical values [-1..100] with a log function contrast in [0..255]." +
            "\n    java -jar "+launcher+" in=/MyImages blank=0 border=\"100 50 100 50\" mode=REPLACETILE   " + "\n" +
            "           => recompute tiles. The original pixels in the border or null will be ignored."+
            "\n    java -jar "+launcher+" in=HiPS out=HiPStarget CONCAT => Concatenate HiPS to HiPStarget"
            //                         "\n    java -jar Aladin.jar -mocgenred=/MySkyRed redparam=sqrt blue=/MySkyBlue output=/RGB rgb  => compute a RGB all-sky"
            );
      
      System.out.println("\n(c) Unistra/CNRS 2016 - "+launcher+" based on Aladin "+Aladin.VERSION+" from CDS");
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
