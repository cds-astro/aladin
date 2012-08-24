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
import java.io.FileReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import cds.moc.HealpixMoc;

public class SkyGen {

   private static final String fileIn = "fileIn.log";
   private static final String fileErr = "fileErr.log";
   private File file;
   private boolean force=false;
   private boolean flagAbort=false,flagPause=false,flagResume=false;
   public Context context;

   private Vector<Action> actions;

   public SkyGen() {
      this.context = new Context();
      actions = new Vector<Action>();
   }

   /**
    * Analyse le fichier contenant les param�tres de config de la construction
    * du allsky sous le format : option = valeur
    * 
    * @throws Exception
    *             si l'erreur dans le parsing des options n�cessite une
    *             interrption du programme
    */
   private void parseConfig() throws Exception {

      // Extrait toutes les options du fichier
      // pour construire le contexte

      // Ouverture et lecture du fichier
      Properties properties = new Properties();
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
    * Affecte � un objet Context l'option de configuration donn�e
    *
    * @param opt
    *            nom de l'option
    * @param val
    *            valeur de l'option
    * @throws Exception
    *             si l'interpr�tation de la valeur n�cessite une interrption du
    *             programme
    */
   private void setContextFromOptions(String opt, String val) throws Exception {
      // enl�ve des �ventuels apostrophes ou guillemets
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
      } else if (opt.equalsIgnoreCase("mode") || opt.equalsIgnoreCase("pixel")) {
         if (opt.equalsIgnoreCase("pixel") ) context.warning("Prefer \"mode\" instead of \"pixel\"");
         context.setCoAddMode(CoAddMode.valueOf(val.toUpperCase()));
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
      } else if (opt.equalsIgnoreCase("skyval")) {
         context.setSkyval(val);
      } else if (opt.equalsIgnoreCase("border")) {
         try {
            context.setBorderSize(val);
         } catch (ParseException e) {
            throw new Exception(e.getMessage());
         }
      } else if (opt.equalsIgnoreCase("jpegMethod")|| opt.equalsIgnoreCase("method")) {
         if (opt.equalsIgnoreCase("method") ) context.warning("Prefer \"jpegMethod\" instead of \"method\"");
         context.setMethod(val);
      } else if (opt.equalsIgnoreCase("pixelCut")) {
         context.setPixelCut(val);
      } else if (opt.equalsIgnoreCase("pixelRange") || opt.equalsIgnoreCase("dataCut")) {
         if (opt.equalsIgnoreCase("dataCut") ) context.warning("Prefer \"pixelRange\" instead of \"dataCut\"");
         context.setDataCut(val);
      } else if (opt.equalsIgnoreCase("color")) {
         context.setColor(Boolean.parseBoolean(val));
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
         // help
         else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
            SkyGen.usage();
            return;
         }
         // debug
         else if (arg.equalsIgnoreCase("-debug") || arg.equalsIgnoreCase("-d")) {
            Context.setVerbose(4);
         }
         else if (arg.equalsIgnoreCase("-force") ) {
            force=true;
         }

         // toutes les autres options �crasent les pr�c�dentes
         else if (arg.contains("=")) {
            String[] opts = arg.split("=");
            try {
               // si il y a un - on l'enl�ve
               opts[0] = opts[0].substring(opts[0].indexOf('-') + 1);

               setContextFromOptions(opts[0], opts[1]);
            } catch (Exception e) {
               context.error(e.getMessage());
               return;
            }
         }
         // les autres mots sont suppos�es des actions (si +ieurs, seule la
         // derni�re est gard�e)
         else {
            try {
               Action a = Action.valueOf(arg.toUpperCase());
               if( a==Action.FINDER ) a=Action.INDEX;   // Pour compatibilit�
               if( a==Action.ABORT ) flagAbort=true;    // Bidouillage pour pouvoir tuer un skygen en cours d'ex�cution
               if( a==Action.PAUSE ) flagPause=true;    // Bidouillage pour pouvoir mettre en pause un skygen en cours d'ex�cution
               if( a==Action.RESUME ) flagResume=true;    // Bidouillage pour pouvoir remettre en route un skygen en pause
               actions.add(a);
            } catch (Exception e) {
               e.printStackTrace();
               return;
            }
         }
      }
      
      // Permet de tuer proprement une tache d�j� en cours d'ex�cution
      if( flagAbort ) {
         try { context.taskAbort(); }
         catch( Exception e ) { context.error(e.getMessage()); }
         return;
      }
      
      // Permet de mettre en pause temporaire une tache en cours d'ex�cution
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


      // Les t�ches � faire si aucune n'est indiqu�es
      boolean all=false;
      if( actions.size()==0 ) {
         all=true;
         actions.add(Action.INDEX);
         actions.add(Action.TILES);
         actions.add(Action.GZIP);
         actions.add(Action.JPEG);
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
   
   /** Juste pour pouvoir ex�cuter skygen comme une commande script Aladin */
   public void executeAsync(String [] args) { new ExecuteAsyncThread(args); }
   class ExecuteAsyncThread extends Thread {
      String [] args;
      public ExecuteAsyncThread(String [] args) { this.args=args; start(); }
      public void run() { execute(args); }
   }
   
   private static void usage() {
      System.out.println("SkyGen [-force] options... [action [action...]]");
      System.out.println("SkyGen [-force] -param=configfile\n");
      System.out.println("This config file must contains these following options, or use them directly in the comand line :");
      System.out.println(
            "-force     Do not take into account possible previous computation\n"+
            "input      Source image directory (fits or jpg+hhh)" + "\n" +
            "output     HEALPix target directory (default $PWD+\"ALLSKY\")" + "\n" +
            "mode       Coadd mode when restart: pixel level(OVERWRITE|KEEP|AVERAGE) or tile level (REPLACETILE|KEEPTILE) - (default OVERWRITE)" + "\n" +
            "img        Specifical reference image for default initializations (BITPIX,BSCALE,BZERO,BLANK,order,pixelCut,dataCut)" + "\n" +
            "bitpix     Specifical target bitpix" + "\n" +
            "order      Specifical HEALPix order" + "\n" +
            "border     Margins (in pixels) to ignore in the original images (N W S E or constant)" + "\n" +
            "blank      Specifical BLANK value" + "\n" +
            "skyval     Fits key to use for removing a sky background" + "\n" +
            "region     Specifical HEALPix region to compute (ex: 3/34-38 50 53) or Moc.fits file (all sky by default)" + "\n" +
            "jpegMethod Jpeg HEALPix method (MEDIAN|MEAN) (default MEDIAN)" + "\n" +
            "pixelCut   Specifical pixel cut and/or transfert function for JPEG 8 bits conversion - ex: \"120 140 log\")" + "\n" +
            "pixelRange Specifical pixel value range (required for bitpix conversion - ex: \"-32000 +32000\")" + "\n" +
            "color      True if the source images are colored jpeg (default is false)" + "\n" +
//            "frame           Healpix frame (C or G - default C for ICRS)" + "\n" +
            "verbose    Show live statistics : tracelevel from -1 (nothing) to 4 (a lot)" + "\n" +
            "debug      true|false - to set output display as te most verbose or just statistics" + "\n");
      System.out.println("\nSpecifical actions (by default all required actions):" + "\n" +
            "index      Build finder index" + "\n" +
            "tiles      Build HEALPix FITS tiles" + "\n" +
            "jpeg       Build JPEG tiles (from FITS tiles)" + "\n" +
            "moc        Build final MOC (based on generated tiles)" + "\n" +
            "mocIndex   Build index MOC (based on HEALPix index)" + "\n" +
            "allsky     Build low resolution Allsky view (Fits and/or Jpeg)" + "\n"+
            "tree       (Re)Build tree FITS tiles from FITS low level tiles" + "\n"+
            "clean      Remove all HEALPix survey" + "\n"+
            "cleanIndex Remove HEALPix index" + "\n"+
            "cleanTiles Remove all HEALPix survey except the index" + "\n"+
            "cleanfits  Remove FITS tiles" + "\n"+
            "cleanjpeg  Remove Jpeg tiles " + "\n"+
            "gzip       gzip some fits tiles and Allsky.fits (by keeping the same names)" + "\n"+
            "gunzip     gunzip all fits tiles and Allsky.fits (by keeping the same names)" + "\n"+
            "progen     Adapt HEALPix tree index to a progenitor usage" + "\n"
            );
      System.out.println("\nEx: SkyGen -input=/MyImages    => Do all the job." +
      		             "\n    SkyGen -input=/MyImages -bitpix=16 -pixelCut=\"-1 100 log\" => Do all the job" +
      		             "\n           The HEALPix fits tiles will be coded in short integers, the Jpeg tiles" +
      		             "\n           will map the originals values [-1..100] with a log function contrast." +
      		             "\n    SkyGen -input=/MyImages -blank=0 -border=\"100 50 100 50\" -mode=REPLACETILE    => recompute tiles" +
      		             "\n           The original pixels in the border or equal to 0 will be ignored.");
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
