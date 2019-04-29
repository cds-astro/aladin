// Copyright 1999-2018 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.allsky;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Iterator;

import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.fits.Fits;
import cds.moc.SpaceMoc;
import cds.mocmulti.MultiMoc;
import cds.tools.pixtools.Util;

/**
 * Classe abstraite d'�crivant les actions possibles (voir la m�thode Task)
 * @author Pierre Fernique [CDS]
 *
 */
public abstract class Builder {

   protected Builder b=null;                 // Subtilit� pour faire afficher des statistiques

   protected Context context;
   protected Builder(Context context) { this.context=context; }

   /** Constructeur g�n�ral pour toutes les actions possibles */
   static public Builder createBuilder(Context context,Action action) throws Exception {
      switch(action) {
         case INDEX:     return new BuilderIndex(context);
//         case TINDEX:    return new BuilderTIndex(context);
         case TILES:     return new BuilderTiles(context);
         case ALLSKY:    return new BuilderAllsky(context);
         case JPEG:      return new BuilderJpg(context);
         case PNG:       return new BuilderPng(context);
         case MOC:       return new BuilderMoc(context);
         case MOCERROR:  return new BuilderMocError(context);
         case MOCINDEX:  return new BuilderMocIndex(context);
         case CLEAN:     return new BuilderClean(context);
         case CLEANALL:  return new BuilderCleanAll(context);
         case CLEANINDEX:return new BuilderCleanIndex(context);
         case CLEANTINDEX:return new BuilderCleanTIndex(context);
         case CLEANDETAILS:return new BuilderCleanDetails(context);
         case CLEANTILES:return new BuilderCleanTiles(context);
         case CLEANFITS: return new BuilderCleanFits(context);
         case CLEANJPEG: return new BuilderCleanJpg(context);
         case CLEANPNG:  return new BuilderCleanPng(context);
         case CLEANDATE: return new BuilderCleanDate(context);
         case CLEANWEIGHT:return new BuilderCleanWeight(context);
         case LINT:      return new BuilderLint(context);
//         case GZIP:      return new BuilderGzip(context);
         case GUNZIP:    return new BuilderGunzip(context);
         case RGB:       return new BuilderRgb(context);
         case TREE:      return new BuilderTree(context);
         case CONCAT:    return new BuilderConcat(context);
         case APPEND:    return new BuilderAppend(context);
         case CUBE:      return new BuilderCube(context);
         case DETAILS:   return new BuilderDetails(context);
         case MAPTILES:  return new BuilderMapTiles(context);
         case UPDATE:    return new BuilderUpdate(context);
         case PROP:      return new BuilderProp(context);
         case MIRROR:    return new BuilderMirror(context);
         case MAP:       return new BuilderMap(context);
         case TMOC:      return new BuilderTMoc(context);
         case STMOC:     return new BuilderSTMoc(context);
//         case ZIP:       return new BuilderZip(context);
         default: break;
      }
      throw new Exception("No builder associated to this action");
   }

   /** Valide les pr�conditions � l'ex�cution de la t�che */
   public abstract void validateContext() throws Exception;

   /** Retourne true si l'ex�cution de la t�che est inutile (ex: d�j� faite) */
   public boolean isAlreadyDone() { return false; }

   /** Ex�cute la t�che */
   public abstract void run() throws Exception;

   /** Ex�cute la t�che sans info technique */
   protected void build() throws Exception {}
      
   /** Retourne l'identificateur de l'action */
   public abstract Action getAction();

   /** Affiche des statistiques de progression */
   public void showStatistics() { }

   /** Indique le mode Just-print - not run -> retourne true si c'est le cas avec un message d'info */
   public boolean isFake() {
      if( !context.fake ) return false;
      context.info("Action "+getAction()+" not run due to the -n option");
      return true;
   }

   // Quelques validateurs g�n�riques utilis�s par les diff�rents Builders.

   // V�rifie que le r�pertoire Input a �t� pass� en param�tre et est utilisable
   protected void validateInput() throws Exception {
      if( context.isValidateInput() ) return;
      String input = context.getInputPath();
      if( input==null ) throw new Exception("Argument \"input\" is required");
      File f = new File(input);
      if( !f.canRead()) throw new Exception("Input not available ["+input+"]");
      if( f.isFile() ) {
         context.info("Unique input image detected");
         context.setFlagInputFile(true);
      }
      if( context.isExistingAllskyDir(input) && context.hasPropertyFile(input) ) {
         throw new Exception("The input directory must be a image collection, not a HiPS => aborted");
      }
      context.setValidateInput(true);
   }
   
   
   static private String FS = cds.tools.Util.FS;
   
   
   // V�rifie que le r�pertoire Output a �t� pass� en param�tre, sinon essaye de le d�duire
   // du r�pertoire Input en ajoutant le suffixe HiPS
   // S'il existe d�j�, v�rifie qu'il s'agit bien d'un r�pertoire utilisable
   protected void validateOutput() throws Exception { 
      String output = context.getOutputPath();
      

      String name = null;
      String path = null;
      int i = output==null ? -1 : output.lastIndexOf(FS);

      // Path indiqu� sp�cifiquement ?
      if( i>=0 ) path = output.substring(0,i);

      // non ! donc par d�faut le r�pertoire contenant l'image ou le r�pertoire des images originales
      else {
         String input = context.getInputPath();
         if( input!=null && !input.startsWith("http://") && !input.startsWith("https://") ) {
            int j = input.lastIndexOf(FS);
            if( j>=0 ) path = input.substring(0, j);
            else path = ".";

            // Sauf en cas de mirroir => le r�pertoire courant   
         } else path = ".";
      }

      // Nom indiqu� sp�cifiquement ?
      if( output!=null && i<output.length()-1 ) name = output.substring(i+1);

      // non ! alors on le fabrique depuis l'ID
      else {
         String id = context.getHipsId();

         // Pas d'id => On ajoute simplement le suffixe HiPS au r�pertoire d'origine
         if( id==null ) {
            String input = context.getInputPath();
            if( input!=null && !input.startsWith("http://") && !input.startsWith("https://") ) {
               int j = input.lastIndexOf(FS);
               name = input.substring(j+1)+Constante.HIPS;

               // sauf s'il s'agit d'un MIRROR, ou alors on le prend tel que
            } else {
               int j = input.lastIndexOf('/');
               name = input.substring(j+1);
            }

            // Un Id => on l'utilise comme nom de r�pertoire cible (avec des _ � la place des / et ?)
         } else {
            id = id.substring(6);
            id = id.replace('/','_');
            id = id.replace('?','_');
            name = id;
         }
      }

      output = path+FS+name;
      context.setOutputPath( output );

      File f = new File(output);
      if( f.exists() && (!f.isDirectory()  || !f.canRead())) throw new Exception("Ouput directory not available ["+output+"]");
      context.info("the output directory will be "+output);
      
      
      context.setValidateOutput(true);
      
//      if( true ) System.exit(0);
   }
   
   /** V�rifie que le r�pertoire HpxIndex existe et peut �tre utilis� */
   protected void validateIndex() throws Exception {
      String path = context.getHpxFinderPath();
      if( path==null ) throw new Exception("HEALPix index directory [HpxFinder] not defined => specify the output (or input) directory");
      File f = new File(path);
      if( !f.exists() || !f.isDirectory() || !f.canRead() ) throw new Exception("HEALPix index directory not available ["+path+"]");
   }

   // R�cup�re l'ordre en fonction d'un r�pertoire. Si un order particulier a �t� pass� en param�tre,
   // v�rifie sa coh�rence avec celui trouv�
   protected void validateOrder(String path) throws Exception {
      int order = context.getOrder();
      int orderIndex = Util.getMaxOrderByPath( path );
      if( order==-1 || context instanceof ContextGui ) {
         context.info("Order retrieved from ["+path+"] => "+orderIndex);
         context.setOrder(orderIndex);
      } else if( orderIndex!=-1 && order!=orderIndex ) throw new Exception("Detected order ["+orderIndex+"] does not correspond to the param order ["+order+"]");
   }

   /** R�cup�ration de la profondeur (cube) */
   protected void validateDepth() throws Exception {

      if( context.depthInit ) return;

      // tentative de r�cup�ration de la profondeur par une image �talon
      if( !context.isColor() ) {
         String img = context.getImgEtalon();
         if( img==null && context.getInputPath()!=null) {
            img = context.justFindImgEtalon( context.getInputPath() );
            if( img!=null ) context.info("Use this reference image => "+img);
         }
         if( img!=null ) {
            try { context.setImgEtalon(img); }
            catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
         }
      }

      // Tentative de r�cup�ration de la profondeur par le fichier des properties
      if( !context.depthInit ) {
         try {
            context.loadProperties();
            String s = context.prop.getProperty(Constante.KEY_CUBE_DEPTH);
            if( s==null ) s = context.prop.getProperty(Constante.OLD_CUBE_DEPTH);
            if( s!=null ) {
               int depth = Integer.parseInt(s);
               if( depth>1 ) context.setDepth( depth );
            }
         } catch( Exception e ) { context.warning("Propertie file problem => "+e.getMessage()); }
      }

      if( context.depthInit && context.depth>1 ) {
         String s="";
         if( context.isCubeCanal() ) {
            s=" (crpix3="+context.crpix3+" crval3="+context.crval3+" cdelt3="+context.cdelt3+")";
         }
         context.info("Working on HiPS cube => depth="+context.depth+s);
      }
   }

   // Valide les cuts pass�s en param�tre, ou � d�faut cherche � en obtenir depuis une image �talon
   protected void validateCut() throws Exception {
      if( context.isValidateCut() ) return;
      double [] cut = null;
      //      double [] cut = context.getCut();

      double [] pixelGood = context.pixelGood;
      boolean missingGood = pixelGood!=null && context.good!=null;

      boolean missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      boolean missingRange = cut==null || cut[2]==0 && cut[3]==0;

      // S'il y a des pixelRange et/ou pixelCut indiqu�s sur la ligne de commande, il faut les convertir
      // en cut[] � r�cup�rant le bzero et le bscale depuis le fichier Allsky.fits ou depuis une image �talon
      double [] pixelRangeCut = context.getPixelRangeCut();
      if( (missingCut  || missingRange ) && pixelRangeCut!=null || missingGood ) {
         try {
            setBzeroBscaleFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
         } catch( Exception e ) {

            String img = context.getImgEtalon();
            if( img==null && context.getInputPath()!=null) {
               img = context.justFindImgEtalon( context.getInputPath() );
               if( img!=null ) context.info("Use this reference image => "+img);
            }
            if( img!=null ) {
               try { context.setImgEtalon(img); }
               catch( Exception e1) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
            }
         }

         try {
            if( cut==null ) cut = new double[5];
            for( int i=0; i<4; i++ ) {
               if( Double.isNaN(pixelRangeCut[i]) ) continue;
               cut[i] = (pixelRangeCut[i] - context.bzero)/context.bscale;
               //               System.out.println("Retreiving from user pixelRangeCut["+i+"]="+pixelRangeCut[i]+" => cut["+i+"]="+cut[i]);
            }

            if( missingGood ) {
               context.good = new double[2];
               for( int i=0; i<2; i++ ) {
                  context.good[i] = (context.pixelGood[i] - context.bzero)/context.bscale;
               }
            }

         } catch( Exception e ) {
            throw new Exception("Cannot retrieve BZERO & BSCALE from previous Allsky.fits file or reference image");
         }
      }

      // S'il me manque le cut du pixelCut ou du pixelRange, il faut que je r�cup�re une image �talon
      // que j'en d�duise les cutOrig, bzeroOrig, bscaleOrig, puis que j'en calcule le bzero, bscale et donc les cut
      missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      if( missingCut ) {
         String img = context.getImgEtalon();
         if( img==null && context.getInputPath()!=null) {
            img = context.justFindImgEtalon( context.getInputPath() );
            if( img!=null ) context.info("Use this reference image => "+img);
         }
         if( img!=null ) {
            try { context.setImgEtalon(img); }
            catch( Exception e) { context.warning("Reference image problem ["+img+"] => "+e.getMessage()); }
         }

         context.initParameters();

         double [] imgCut = context.getCut();
         if( cut==null ) cut = new double[5];
         if( missingCut )   {
            cut[0]= imgCut[0];
            cut[1]= imgCut[1];
            if( cut[0]!=cut[1] ) context.info("Estimating pixel cut from the reference image => ["+cut[0]+" .. "+cut[1]+"]");
         }
      }

      // S'il me manque toujours le pixelCut, je vais tenter de les r�cup�rer par le fichier des properties
      missingCut   = cut==null || cut[0]==0 && cut[1]==0;
      if( missingCut ) updateCutByProperties(cut);

      context.setCut(cut);

      double bz=context.bzero;
      double bs=context.bscale;
      if( cut==null || cut[0]==0 && cut[1]==0 ) throw new Exception("Argument \"pixelCut\" required");
      if( !( cut[0] < cut[1] ) ) throw new Exception("pixelCut error ["+ip(cut[0],bz,bs)+" .. "+ip(cut[1],bz,bs)+"]");
      context.info("pixel cut ["+ip(cut[0],bz,bs)+" .. "+ip(cut[1],bz,bs)+"]");
      context.setValidateCut(true);
   }


   /** Met � jour les champs manquants du cut[] via les infos du fichier des properties */
   protected void updateCutByProperties(double [] cut) {
      try {
         String propFile = context.getOutputPath()+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            InputStreamReader in = new InputStreamReader( new BufferedInputStream( new FileInputStream(propFile) ), "UTF-8");
            prop.load(in);
            in.close();
            String s = prop.getProperty(Constante.KEY_HIPS_PIXEL_CUT);
            if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_PIXEL_CUT);
            context.setPixelCut(s);
            double [] pixelRangeCut = context.getPixelRangeCut();

            // Il me faut alors BZERO  et BSCALE
            try {
               setBzeroBscaleFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
               if( cut==null ) cut = new double[5];
               for( int i=0; i<4; i++ ) {
                  if( Double.isNaN(pixelRangeCut[i]) ) continue;
                  cut[i] = (pixelRangeCut[i] - context.bzero)/context.bscale;
               }
               context.info("Pixel cut from the propertie file => ["+ip(cut[0],context.bzero,context.bscale)+" .. "+ip(cut[1],context.bzero,context.bscale)+"]");
            } catch( Exception e ) { }
         }
      } catch( Exception e ) {}
   }


   protected void validateBitpix() {
      if( context.bitpix!=-1 ) return;
      try {
         setBzeroBscaleFromPreviousAllsky(context.getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits");
      } catch( Exception e ) { }
   }

   protected void validateLabel() {
      if( context.label!=null ) return;
      String label = getALabel(context.getOutputPath(),context.getInputPath());
      if( label!=null && label.length()>0 ) context.label=label;
   }

   protected String getALabel(String outputPath,String inputPath) {
      String label=null;

      // Je vais essay� de le r�cup�rer depuis le fichier des propri�t�s
      try {
         String propFile = outputPath+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            InputStreamReader in = new InputStreamReader( new BufferedInputStream( new FileInputStream(propFile) ), "UTF-8");
            prop.load(in);
            in.close();
            
            String s =  MultiMoc.getID(prop);
            s=context.getLabelFromHipsId(s);
            if( s==null ) {
               s = prop.getProperty(Constante.KEY_OBS_TITLE);
            }
//            String s = prop.getProperty(Constante.KEY_OBS_COLLECTION);
            if( s==null ) {
               s = prop.getProperty(Constante.OLD_OBS_COLLECTION);
            }
            if( s!=null && s.length()>0 ) label=s;
         }
      } catch( Exception e ) { }
      if( label!=null ) return label;

      // Je vais le construire � partir du nom du r�pertoire
      if( inputPath!=null ) {
         int offset = inputPath.lastIndexOf(Util.FS);
         if( offset<0 ) offset = inputPath.lastIndexOf('/');
         if( offset>=0 ) label=inputPath.substring(offset+1);
         else label=inputPath;
      } else {
         int offset = outputPath.lastIndexOf(Util.FS);
         if( offset<0 ) offset = outputPath.lastIndexOf('/');
         if( offset>=0 ) label=outputPath.substring(offset+1);
         else label=outputPath;
      }

      return label;
   }

   // Retourne le code HEALPix correspondant au syst�me de r�f�rence des coordonn�es
   // du survey HEALPix
   protected String getFrame() {
      if( context.hasFrame() ) return context.getFrameCode();
      try {
         if( context.prop==null ) context.loadProperties();
         String s = context.prop.getProperty(Constante.KEY_HIPS_FRAME);
         if( s==null ) s = context.prop.getProperty(Constante.OLD_HIPS_FRAME);
         if( s==null ) s="G";
         if( s.equals("equatorial") ) return "C";
         if( s.equals("ecliptic")) return "E";
         if( s.equals("galactic")) return "G";
         return s;
      } catch( Exception e ) { e.printStackTrace(); }
      return context.getFrameCode();
   }

   protected String ip(double raw,double bzero,double bscale) {
      return cds.tools.Util.myRound(raw) + (bzero!=0 || bscale!=1 ? "/"+cds.tools.Util.myRound(raw*bscale+bzero) : "");
   }

   /**
    * Initialisation des param�tres FITS � partir d'un Allsky.fits pr�c�dent
    */
   protected void setFitsParamFromPreviousAllsky(String allskyFile) throws Exception {
      Fits f = new Fits();

      f.loadFITS(allskyFile);
      double [] cut = f.findAutocutRange(0,0,true);

      context.setBitpix(f.bitpix);
      context.setCut(cut);
      try {
         double blank = f.headerFits.getDoubleFromHeader("BLANK");
         context.blank=blank;
      } catch( Exception e ) { }
      try {
         double bzero = f.headerFits.getDoubleFromHeader("BZERO");
         context.bzero=bzero;
      } catch( Exception e ) { }
      try {
         double bscale = f.headerFits.getDoubleFromHeader("BSCALE");
         context.bscale=bscale;
      } catch( Exception e ) { }

   }

   protected void setBzeroBscaleFromPreviousAllsky(String allskyFile) throws Exception {
      Fits f = new Fits();
      f.loadHeaderFITS(allskyFile);
      try {
         double bzero = f.headerFits.getDoubleFromHeader("BZERO");
         context.bzero=bzero;
      } catch( Exception e ) { }
      try {
         double bscale = f.headerFits.getDoubleFromHeader("BSCALE");
         context.bscale=bscale;
      } catch( Exception e ) { }

      // J'en profite
      if( context.bitpix==-1 ) {
         try {
            double bitpix = f.headerFits.getDoubleFromHeader("BITPIX");
            context.bitpix=(int)bitpix;
         } catch( Exception e ) { }

      }
   }
   
   
   // Gestion des param�tres pour le stockage dans une partition alternative
   private class Part {
      double size;   // taille courante en Ko
      double max;    // taille max de la partition en Ko ou <=0 si non utilis�
      String dir;    // Nom du r�pertoire root de la partition
   }
   
   static final int MAXPART = 10;   // Nombre max de partitions d'un HiPS
   protected Part part [];            // Gestion des partitions alternatives (mais pas la partition originale)
   
   // Retourne le nombre de Ko correspondant � une expression du genre 4.2g
   private double getMem( String s ) throws Exception {
      char u = Character.toLowerCase( s.charAt( s.length()-1 ) );
      double fct = u=='k' ? 1 : u=='m' ? 1024 : u=='g' ? 1024*1024 : u=='t' ? 1024*1024*1024 : -1;
      if( fct==-1 ) throw new Exception("Invalid mem unit");
      return Double.parseDouble( s.substring( 0,s.length()-1 )) * fct;
   }
   
   // Retourne la taille moyenne d'une tuile (tous les formats additionn�s) - en bytes
   private long getTileSize( int bitpix, int width, int depth, String fmt ) {
      long nbpix = width*width;
      
      // Cas couleur
      if( bitpix==0 ) {
         long nbBytesJpg = fmt.toLowerCase().indexOf("jpeg")>=0 ? (nbpix*4)/10L : 0L;
         long nbBytesPng = fmt.toLowerCase().indexOf("png")>=0  ? (nbpix*4)/8L : 0L;
         long nbBytes = (nbBytesJpg + nbBytesPng ) * depth;
         return nbBytes;
      } 
      
      // Cas classique
      long nbBytesFits = fmt.toLowerCase().indexOf("fits")>=0 ? 2880 + Math.abs(bitpix)/8 * nbpix : 0L;
      long nbBytesJpg = fmt.toLowerCase().indexOf("jpeg")>=0  ? nbBytesFits/10L : 0L;
      long nbBytesPng = fmt.toLowerCase().indexOf("png")>=0   ? nbBytesFits/8L : 0L;
      long nbBytes = (nbBytesFits + nbBytesJpg + nbBytesPng ) * depth;
      return nbBytes;
   }
   
   /** G�n�ration des liens symboliques et des r�pertoires Dirnn � l'order le plus profond afin de
     * pouvoir par la suite r�partir le HiPS sur plusieurs partitions
     */
   protected void validateSplit(String outputPath, String split, SpaceMoc moc, int order, int bitpix, int tileWidth, int depth, String fmt) throws Exception {
      
      // D�termination de la taille totale requise (en Ko)
      moc.setMocOrder(order);
      long numberOfTiles = moc.getUsedArea();
      long tileSize = getTileSize( bitpix, tileWidth, depth, fmt);
      if( tileSize==0 ) throw new Exception("No remote tile found");
      long fullSize = (long)( ( 1.3 * tileSize * numberOfTiles + 8 ) / 1024. );
      context.stat("Max order="+order+", Number of tiles="+numberOfTiles+", tile size("+fmt+")="+cds.tools.Util.getUnitDisk( tileSize )+", HiPS size estimation="+ cds.tools.Util.getUnitDisk( fullSize*1024L ) );
      
      // Scan du param�tre de split
      part = new Part[ MAXPART ];
      String s = split;
      Tok tok = new Tok(s,";");
      double firstPartSize = getMem( tok.nextToken() );
//      System.out.println("First part => "+Util.getUnitDisk( (long)( firstPartSize*1024L) ));
      if( fullSize<=firstPartSize ) {
         context.info("Enough space in master partition quota => no split required");
         return;   // tout tient dans la premi�re partition
      }
      for( int n=0; tok.hasMoreTokens(); n++ ) {
         if( n==MAXPART ) throw new Exception("Too many slitting partitions");
         part[n] = new Part();
         String s1 = tok.nextToken();
         int i = s1.lastIndexOf(' ');
         part[n].max = i<0 ? -1 : getMem( s1.substring(i+1) );
         part[n].dir = i<0 ? s1 : s1.substring(0,i);
         
         if( !( new File(part[n].dir).isAbsolute() ) ) throw new Exception("alternative target partitions must use an absolute path");
//         System.out.println(part[n].dir+" => "+Util.getUnitDisk( (long)( part[n].max)*1024L ));
      }
      
      // D�termination de la taille de chaque Dirnn de niveau le plus profond (en Ko)
      HashMap<Long, Double> hashDir = new HashMap<>();
      Iterator<Long> it = moc.pixelIterator();
      while( it.hasNext() ) {
         long ndirLink = it.next()/10000;
         Double mem = hashDir.get(ndirLink);
         if( mem==null ) hashDir.put(ndirLink, tileSize/1024. );
         else hashDir.put( ndirLink, tileSize/1024. + mem);
      }
      
      // G�n�ration des r�pertoires et liens symboliques vers les partitions alternatives
      String mainPart = outputPath;   // Path de la partition principale
      int nAltPart = 0;   // indice de la partition alternative courante
      int nlinks=0;       // Nombre de links pour la partition altenative courante
      int totNlinks=0;    // Total du nombre de links vers les partitions alternatives
      boolean first = true;
      
      for( Long norderLink : hashDir.keySet() ) {
         
         // Que va-t-on gagner comme place ?
         double sizeLink = hashDir.get(norderLink);
         fullSize -= sizeLink;
         
         // Plus de place sur la partition alternative courante ? on passe � la partition suivante
         if( part[nAltPart].size > part[nAltPart].max && part[nAltPart].max>0 ) { 
            context.stat(part[nAltPart].dir+" will content "+cds.tools.Util.getUnitDisk( (long)( part[nAltPart].size*1024L))+" thanks to "+nlinks+" links");
            nAltPart++;
            nlinks=0;
         }
         if( nAltPart>=part.length ) throw new Exception("Not enough space on alternative target partitions");
         part[nAltPart].size += sizeLink;
         nlinks++;
         totNlinks++;
         
         // Cr�ation du r�pertoire Ndir sur la partition alternative, ainsi que du lien symbolique associ�
         if( !context.fake ) {
            String sOrig = cds.tools.pixtools.Util.getFileDir( mainPart, order, norderLink*10000);
            String sTarg = cds.tools.pixtools.Util.getFileDir( part[nAltPart].dir, order, norderLink*10000);

            File fTarg = new File( sTarg );
            File fOrig = new File( sOrig );

            if( first ) cds.tools.Util.createPath(sOrig);
            fTarg.mkdirs();
            Files.createSymbolicLink(fOrig.toPath(), fTarg.toPath() );
         }
         first=false;
         
         // si on a fait assez de liens symboliques, pas la peine d'aller plus loin
         if( fullSize<firstPartSize ) break;

      }
      
      if( part[nAltPart].size!=0 ) context.stat(part[nAltPart].dir+" will content "+cds.tools.Util.getUnitDisk( (long)( part[nAltPart].size*1024L))+" thanks to "+nlinks+" links");
      int nbdir = (hashDir.size()-totNlinks);
      context.stat(mainPart+" will content "+cds.tools.Util.getUnitDisk( fullSize*1024L )+" (HiPS hierarchy"+(nbdir>0 ?" and "+nbdir+" directories)":")"));

   }
   

   
}
