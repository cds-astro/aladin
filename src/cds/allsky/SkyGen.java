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
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import cds.aladin.MyProperties;
import cds.moc.HealpixMoc;

public class SkyGen {

   private File file;
   private boolean force=false;
   private boolean flagMode=false;
   private boolean flagAbort=false,flagPause=false,flagResume=false;
   public Context context;

   private Vector<Action> actions;

   public SkyGen() {
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
         usage();
      } else if (opt.equalsIgnoreCase("verbose")) {
         Context.setVerbose(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("debug")) {
         if (Boolean.parseBoolean(val)) Context.setVerbose(4);
      } else if (opt.equalsIgnoreCase("input")) {
         context.setInputPath(val);
      } else if (opt.equalsIgnoreCase("output")) {
         context.setOutputPath(val);
      } else if (opt.equalsIgnoreCase("blank")) {
         context.setBlankOrig(Double.parseDouble(val));
      } else if (opt.equalsIgnoreCase("order")) {
         context.setOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("diffOrder")) {
         context.setDiffOrder(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("mode") || opt.equalsIgnoreCase("pixel")) {
         if (opt.equalsIgnoreCase("pixel") ) context.warning("Prefer \"mode\" instead of \"pixel\"");
         context.setCoAddMode(CoAddMode.valueOf(val.toUpperCase()));
         flagMode=true;
      } else if (opt.equalsIgnoreCase("bitpix")) {
         context.setBitpix(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("region") || opt.equalsIgnoreCase("moc")) {
         if (val.endsWith("fits")) {
            HealpixMoc moc = new HealpixMoc();
            moc.read(val);
            context.setMocArea(moc);
         } else context.setMocArea(val);
      } else if (opt.equalsIgnoreCase("frame")) {
         context.setFrameName(val);
      } else if (opt.equalsIgnoreCase("maxThread")) {
         context.setMaxNbThread(Integer.parseInt(val));
      } else if (opt.equalsIgnoreCase("skyval")) {
         context.setSkyval(val);
      } else if (opt.equalsIgnoreCase("fading")) {
         context.setFading(val);
      } else if (opt.equalsIgnoreCase("mixing")) {
         context.setMixing(val);
      } else if (opt.equalsIgnoreCase("border")) {
         try {
            context.setBorderSize(val);
         } catch (ParseException e) {
            throw new Exception(e.getMessage());
         }
      } else if ( opt.equalsIgnoreCase("jpegMethod") || opt.equalsIgnoreCase("method")) {
         if( opt.equalsIgnoreCase("jpegMethod") ) context.warning("Prefer \"method\" instead of \""+opt+"\"");
         context.setMethod(val);
      } else if (opt.equalsIgnoreCase("pixelCut")) {
         context.setPixelCut(val);
      } else if (opt.equalsIgnoreCase("pixelRange") || opt.equalsIgnoreCase("dataCut")) {
         if (opt.equalsIgnoreCase("dataCut") ) context.warning("Prefer \"pixelRange\" instead of \"dataCut\"");
         context.setDataCut(val);
      } else if (opt.equalsIgnoreCase("color")) {
         context.setColor(val);
      } else if (opt.equalsIgnoreCase("red"))   { context.setRgbInput(val, 0);
      } else if (opt.equalsIgnoreCase("green")) { context.setRgbInput(val, 1);
      } else if (opt.equalsIgnoreCase("blue"))  { context.setRgbInput(val, 2);
      } else if (opt.equalsIgnoreCase("redparam"))   { context.setRgbCmParam(val, 0);
      } else if (opt.equalsIgnoreCase("greenparam")) { context.setRgbCmParam(val, 1);
      } else if (opt.equalsIgnoreCase("blueparam"))  { context.setRgbCmParam(val, 2);
      } else if (opt.equalsIgnoreCase("img")) {
         context.setImgEtalon(val);
      } else throw new Exception("Option unknown [" + opt + "]");
      
   }
   
   public void execute(String [] args) {
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
               setConfigFile(arg.substring(param.length()));
            } catch (Exception e) {
               e.printStackTrace();
               return;
            }
            continue;
         }
         
         // Juste pour pouvoir appeler directement par le main() de cette classe
         // et non celle d'Aladin
         else if( arg.equalsIgnoreCase("-skygen") ) continue;
         
         // help
         else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
            SkyGen.usage();
            return;
         }
         // debug
         else if (arg.equalsIgnoreCase("-debug") || arg.equalsIgnoreCase("-d")) {
            Context.setVerbose(4);
         }
         else if (arg.equalsIgnoreCase("-fast") ) {
            context.mixing=true;
         }

         else if (arg.equalsIgnoreCase("-force") || arg.equalsIgnoreCase("-f") ) {
            force=true;
         }

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
               if( a==Action.FINDER ) a=Action.INDEX;   // Pour compatibilité
               if( a==Action.ABORT ) flagAbort=true;    // Bidouillage pour pouvoir tuer un skygen en cours d'exécution
               if( a==Action.PAUSE ) flagPause=true;    // Bidouillage pour pouvoir mettre en pause un skygen en cours d'exécution
               if( a==Action.RESUME ) flagResume=true;    // Bidouillage pour pouvoir remettre en route un skygen en pause
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
            actions.add(Action.JPEG);
            actions.add(Action.PROGEN);
         }
      }
     
      // Nettoyage avant ?
      if( force ) {
         context.setIgnoreStamp(true);
         if( all ) actions.add(0, Action.CLEAN);
         else {
            for( int i=0; i<actions.size() ;i++ ) {
               Action a = actions.get(i);
               if( a==Action.INDEX ) { actions.add(i, Action.CLEANINDEX); i++; }
               else if( a==Action.TILES ) { actions.add(i, Action.CLEANTILES); i++; }
               else if( a==Action.JPEG )  { actions.add(i, Action.CLEANJPEG);  i++; }
            }
         }
      }

      // C'est parti
      try {
         new Task(context,actions,true);
         context.done("The end");
      } catch (Exception e) {
         e.printStackTrace();
         context.error(e.getMessage());
         return;
      }
   }
   
   private boolean findAction(Vector<Action> actions,Action s) {
      for( Action a : actions ) { if( s==a ) return true; }
      return false;
   }
   
   /** Juste pour pouvoir exécuter skygen comme une commande script Aladin */
   public void executeAsync(String [] args) { new ExecuteAsyncThread(args); }
   class ExecuteAsyncThread extends Thread {
      String [] args;
      public ExecuteAsyncThread(String [] args) { this.args=args; start(); }
      public void run() { execute(args); }
   }
   
   private static void usage() {
      System.out.println("Usage: java -jar Aladin.jar -skygen [-f] options... [action [action...]]");
      System.out.println("       java -jar Aladin.jar -skygen [-f] -param=configfile\n\n");
      System.out.println("This config file must contains these following options, or use them\n" +
      		"             directly in the comand line :");
      System.out.println(
            "-f                 Do not take into account possible previous computation\n"+
            "input=dir          Source image directory (fits or jpg|png+hhh or HiPS)" + "\n" +
            "output=dir         HiPS target directory (default $PWD+\"ALLSKY\")" + "\n" +
            "mode=xx            Coadd mode when restart: pixel level(OVERWRITE|KEEP|AVERAGE) \n" +
            "                   or tile level (REPLACETILE|KEEPTILE) - (default OVERWRITE)" + "\n" +
            "img=file           Specifical reference image for default initializations \n" +
            "                   (BITPIX,BSCALE,BZERO,BLANK,order,pixelCut,dataCut)" + "\n" +
            "bitpix=nn          Specifical target bitpix (-64|-32|8|16|32|64)" + "\n" +
            "order=nn           Specifical HEALPix order" + "\n" +
//            "diffOrder          Diff between MOC order and optimal order" + "\n" +
            "border=...         Margins (in pixels) to ignore in the original images (N W S E or constant)" + "\n" +
            "blank=nn           Specifical BLANK value" + "\n" +
            "skyval=key         Fits key to use for removing a sky background" + "\n" +
            "maxThread=nn       Max number of computing threads (8 per default, -1 for max)" + "\n" +
            "region=moc         Specifical HEALPix region to compute (ex: 3/34-38 50 53)\n" +
            "                   or Moc.fits file (all sky by default)" + "\n" +
//            "tobemerged=dir     all-sky directory to be merged to an already existing all-sky\n" +
            "pixelCut=min max   Specifical pixel cut and/or transfert function for JPEG 8 bits\n" +
            "                   conversion - ex: \"120 140 log\")" + "\n" +
            "pixelRange=min max Specifical pixel value range (required for bitpix\n" +
            "                   conversion - ex: \"-32000 +32000\")" + "\n" +
            "fading=true|false  False to avoid fading effect on overlapping original images (default is true)" + "\n" +
            "mixing=true|false  False to avoid mixing (and fading) effect on overlapping original images (default is true)" + "\n" +
            "method=m           Method (MEDIAN|MEAN) (default MEDIAN) for aggregating compressed tiles (jpeg|png)" + "\n" +
            "color=jpeg|png     The source images are colored images (jpg or png) and the tiles will be produced in jpeg (resp. png)" + "\n" +
            "verbose            Show live statistics : tracelevel from -1 (nothing) to 4 (a lot)" + "\n" +
            "debug=true|false   to set output display as te most verbose or just statistics" + "\n"
//            "red        all-sky used for RED component (see rgb action)\n" +
//            "green      all-sky used for BLUE component (see rgb action)\n" +
//            "blue       all-sky used for GREEN component (see rgb action)\n" +
//            "redcm      Transfert function for RED component (hsin, log, sqrt, linear or sqr)\n" +
//            "greencm    Transfert function for BLUE component (hsin, log, sqrt, linear or sqr)\n" +
//            "bluecm    Transfert function for GREEN component (hsin, log, sqrt, linear or sqr)\n" +
//            "frame           Healpix frame (C or G - default C for ICRS)" + "\n" +
      );
      System.out.println("\nSpecifical actions (by default: \"index tiles jpeg moc allsky gzip progen\"):" + "\n" +
            "index      Build finder index" + "\n" +
            "tiles      Build FITS tiles" + "\n" +
            "jpeg       Build JPEG tiles (from FITS tiles)" + "\n" +
            "png        Build PNG tiles (from FITS tiles)" + "\n" +
//            "rgb        Build RGB tiles (from 2 or 3 pre-computed all-skies)" + "\n" +
            "moc        Build final MOC (based on generated tiles)" + "\n" +
//            "mochight   Build final MOC (based on pixels of generated tiles)" + "\n" +
            "mocIndex   Build index MOC (based on HEALPix index)" + "\n" +
            "allsky     Build low resolution Allsky view (Fits and/or Jpeg|png)" + "\n"+
            "tree       (Re)Build tree FITS tiles from FITS low level tiles" + "\n"+
            "concat     Concatenate an HiPS to another already built HiPS" + "\n"+
            "clean      Remove all HiPS files (index, tiles, directories, allsky, MOC, ...)" + "\n"+
            "cleanIndex Remove HEALPix index" + "\n"+
            "cleanTiles Remove all HEALPix survey except the index" + "\n"+
            "cleanfits  Remove FITS tiles" + "\n"+
            "cleanjpeg  Remove JPEG tiles " + "\n"+
            "cleanpng   Remove PNG tiles " + "\n"+
            "gzip       gzip some fits tiles and Allsky.fits (keeping the same names)" + "\n"+
            "gunzip     gunzip all fits tiles and Allsky.fits (keeping the same names)" + "\n"+
            "progen     Adapt the index to a progenitor usage" + "\n"
            );
      System.out.println("\nEx: java -jar Aladin.jar -skygen input=/MyImages    => Do all the job." +
      		             "\n    java -jar Aladin.jar -skygen input=/MyImages -bitpix=16 -pixelCut=\"-1 100 log\" => Do all the job" +
      		             "\n           The HEALPix fits tiles will be coded in short integers, the Jpeg tiles" +
      		             "\n           will map the originals values [-1..100] with a log function contrast." +
                         "\n    java -jar Aladin.jar -skygen input=/MyImages blank=0 border=\"100 50 100 50\" mode=REPLACETILE    => recompute tiles" +
                         "\n           The original pixels in the border or equal to 0 will be ignored."+
                         "\n    java -jar Aladin.jar -skygen input=HiPS ouput=HiPStarget concat   => Concatenate HiPS to HiPStarget"
//                         "\n    java -jar Aladin.jar -mocgenred=/MySkyRed redparam=sqrt blue=/MySkyBlue output=/RGB rgb  => compute a RGB all-sky"
                         );
   }

   private void setConfigFile(String configfile) throws Exception {
      this.file = new File(configfile);
      parseConfig();
   }

   public static void main(String[] args) {
      SkyGen generator = new SkyGen();
      generator.execute(args);
   }
}
