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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import cds.aladin.Aladin;
import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.fits.Fits;
import cds.moc.SMoc;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

public class HipsGen {

    private File file;
    private boolean force=false;
    private boolean trim=false;
    private boolean gzip=false;
    
    private boolean flagMode=false;
    private boolean flagConcat=false;
    private boolean flagMirror=false;
    private boolean flagZip=false;
    private boolean flagUpdate=false;
    private boolean flagLint=false;
    private boolean flagTMoc=false;
    private boolean flagTIndex=false;
    private boolean flagMocError=false;
    private boolean flagProp=false;
    private boolean flagModeTree=false;
    private boolean flagRGB=false;
    private boolean flagGunzip=false;
    private boolean flagMapFits=false;
    private boolean flagHealpixMap=false;
    private boolean flagCrc=false;
    private boolean flagAbort=false,flagPause=false,flagResume=false;
    private boolean flagValidator=false;
    private boolean flagHtml = false;
    public Context context;
    
    private boolean flagHHHcar=false;

    public boolean endOfWork=true;

    private String cache = null; // Path alternatif pour un cache disque (dans le cas d'images compressées)
    private long cacheSize = -1;  // Taille alternative du cache disque (en Mo)
    private boolean cacheRemoveOnExit = true; // Suppression ou non du cache en fin de calcul

    public String launcher = "Aladin.jar -hipsgen";

    private Vector<Action> actions;
    private ArrayList<Param> listParam; 

    public HipsGen() {
        this.context = new Context();
        actions = new Vector<>();
        listParam=new ArrayList<>();
    }

    /**
     * Analyse le fichier contenant les paramètres de config de la construction
     * du allsky sous le format : option = valeur
     *
     * @throws Exception
     *             si l'erreur dans le parsing des options nécessite une
     *             interrption du programme
     */
    private MyProperties parseConfig() throws Exception {

        // Extrait toutes les options du fichier
        // pour construire le contexte

        // Ouverture et lecture du fichier
        MyProperties properties = new MyProperties();
        InputStreamReader reader = new InputStreamReader( new BufferedInputStream( new FileInputStream(file) ));
        properties.load(reader);

        //      Set<Object> keys = properties.keySet();
        //      for (Object opt : keys) {

        for( String opt : properties.getKeys() ) {
            if( opt.startsWith("#") ) continue;
            if( opt.trim().length()==0 ) continue;
            String val = properties.getProperty(opt);

            try {
                setContextFromOptions(opt, val);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        reader.close();
        return properties;
    }


    
    /** Retourne le paramètre qui remplace un paramètre devenu obsolète, null sinon */
    private String[] obsolete(String s,String v) {
       if( s.equalsIgnoreCase("mixing") ) {
          if( v.equalsIgnoreCase("false"))     return new String[] {"mode","overlay_none"};
          if( v.equalsIgnoreCase("true"))      return new String[] {"mode","overlay_mean"};
       }
       return null;
    }

    private MyProperties paramHist=new MyProperties();
    
    /**
     * Affecte à un objet Context l'option de configuration donnée
     *
     * @param opt
     *            nom de l'option
     * @param val
     *            valeur de l'option
     * @throws Exception
     *             si l'interprétation de la valeur nécessite une interruption du
     *             programme
     */
    private void setContextFromOptions(String p,String v) throws Exception {
       setContextFromOptions(p+(v==null?"":"="+v));
    }
    private void setContextFromOptions(String param) throws Exception {
        
        param = ParamObsolete.getLastSyntax(context, param);
        if( param==null ) return;
        String opt, val=null;
        int i = param.indexOf('=');
        if( i<0 ) opt=param;
        else {
           opt = param.substring(0,i).trim();
           val = Tok.unQuote( param.substring(i+1).trim() );
        }
        
        String info=null;
        try {
           Param pa = Param.get(opt);
           listParam.add(pa);
           opt=pa.toString();
           info=pa.info();
        } catch( Exception e) {}
        context.param(opt + "=" + (val==null?"null":val) + (info==null?"":" => "+info) );
        
        paramHist.setProperty(opt, val);    // Mémorisation

               if( Param.cache.equals(opt) )        { cache=val;
        } else if( Param.cacheSize.equals(opt) )    { cacheSize = Long.parseLong(val);
        } else if( Param.cacheRemoveOnExit.equals(opt) ){ cacheRemoveOnExit = Boolean.parseBoolean(val);
//        } else if( Param.hhh.equals(opt))               { generateHHH(val);
        } else if( Param.mirrorSplit.equals(opt) )        { context.setSplit(val);
        } else if( Param.verbose.equals(opt) )      { Context.setVerbose(Integer.parseInt(val));
        } else if( Param.pilot.equals(opt) )        { context.setPilot(Integer.parseInt(val));
        } else if( Param.blank.equals(opt) )        { 
           try {
              context.setBlankOrig(Double.parseDouble(val));
           } catch( Exception e ) {
              context.setBlankOrig(val);   // peut être un mot clé spécifique alternatif pour le BLANK
           }
        } else if( Param.order.equals(opt) )   { context.setOrder(Integer.parseInt(val));
        } else if( Param.mocOrder.equals(opt) )     { context.setMocOrder(val);
        } else if( Param.mapNside.equals(opt) )        { context.setMapNside(Integer.parseInt(val));
//        } else if( Param.tileOrder.equals(opt) )    { context.setTileOrder(Integer.parseInt(val));
        } else if( Param.tileWidth.equals(opt) )  { context.setTileOrder((int)CDSHealpix.log2( Integer.parseInt(val)));
        } else if( Param.bitpix.equals(opt) ) { context.setBitpix(Integer.parseInt(val));
        } else if( Param.frame.equals(opt) )   { context.setFrameName(val);
        } else if( Param.maxThread.equals(opt) )    { context.setMaxNbThread(Integer.parseInt(val));
        } else if( Param.skyVal.equals(opt) )       { context.setSkyval(val);
        } else if( Param.skyvalues.equals(opt) )    { context.setSkyValues(val);
        } else if( Param.expTime.equals(opt) )      { context.setExpTime(val);
        } else if( Param.color.equals(opt) )        { context.setColor(val);
        } else if( Param.inRed.equals(opt) )        { context.setRgbInput(val, 0); flagRGB=true;
        } else if( Param.inGreen.equals(opt) )      { context.setRgbInput(val, 1); flagRGB=true;
        } else if( Param.inBlue.equals(opt) )       { context.setRgbInput(val, 2); flagRGB=true;
        } else if( Param.cmRed.equals(opt) )        { context.setRgbCmParam(val, 0);
        } else if( Param.cmGreen.equals(opt) )      { context.setRgbCmParam(val, 1);
        } else if( Param.cmBlue.equals(opt) )       { context.setRgbCmParam(val, 2);
        } else if( Param.luptonQ.equals(opt) )      { context.setRgbLuptonQ(val);
        } else if( Param.luptonM.equals(opt) )      { context.setRgbLuptonM(val);
        } else if( Param.luptonS.equals(opt) )      { context.setRgbLuptonS(val);
        } else if( Param.img.equals(opt) )          { context.setImgEtalon(val);
        } else if( Param.fitsKeys.equals(opt) )     { context.setIndexFitskey(val);
        } else if( Param.status.equals(opt) )  { context.setStatus(val);
        } else if( Param.target.equals(opt) )       { context.setTarget(val);
        } else if( Param.title.equals(opt) )    { context.setTitle(val);
//        } else if( Param.filter.equals(opt) )       { context.setFilter(val);
        } else if( Param.hdu.equals(opt) )          { context.setHDU(val);
        } else if( Param.creator.equals(opt) ) { context.setCreator(val);
        } else if( Param.id.equals(opt) )  { context.setHipsId(val);
        } else if( Param.in.equals(opt) )           { context.setInputPath(val);
        } else if( Param.out.equals(opt) )          { context.setOutputPath(val);
        } else if( Param.incremental.equals(opt) )  { context.setLive(Boolean.parseBoolean(val));
        } else if( Param.mode.equals(opt) )         { context.setMode(val);  flagMode=true;
        } else if( Param.partitioning.equals(opt) ) { context.setPartitioning(val);
        } else if( Param.mirrorFormat.equals(opt) )   { context.setTileFormat(val);
        } else if( Param.shape.equals(opt) )        { context.setShape(val);
        } else if( Param.validRange.equals(opt) )    { context.setPixelGood(val);
        } else if( Param.pixelCut.equals(opt) )  { context.setPixelCut(val);
        } else if( Param.dataRange.equals(opt) ) { context.setDataRange(val);
        } else if( Param.minOrder.equals(opt) )  { context.setMinOrder(Integer.parseInt(val));
        } else if( Param.fastCheck.equals(opt) ) { context.setfastCheck(Boolean.parseBoolean(val));
        } else if( Param.region.equals(opt) ) {
            if (val.endsWith("fits")) {
                SMoc moc = new SMoc();
                moc.read(val);
                context.setMocArea(moc);
            } else context.setMocArea(val);
        } else if( Param.maxRatio.equals(opt) ) {
            try {  context.setMaxRatio(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
            //      } else if( opt.equalsIgnoreCase("radius")) {
            //         try {  context.setCircle(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
        } else if( Param.fov.equals(opt) ) {
            try {  context.setFov(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
        } else if( Param.border.equals(opt) ) {
            try { context.setBorderSize(val); } catch (ParseException e) { throw new Exception(e.getMessage()); }
        } else throw new Exception("Unknown parameter [" + opt + "]");


    }
    
    private void generateHHHcar( String file, int frame) throws Exception {
       if( (new File(file)).isDirectory() ) throw new Exception("hhh generation failed");
       Fits f = new Fits();
       f.loadPreview(file);
       int w = f.width;
       int h = f.height;
       context.info("Creating hhh file (assuming full sky CAR projection) for "+file);
       
       String prefLon = frame==Localisation.ICRS ? "RA---" : frame==Localisation.GAL ? "GLON--" : "ELON--";
       String prefLat = frame==Localisation.ICRS ? "DE---" : frame==Localisation.GAL ? "GLAT--" : "ELAT--";

       String filehhh = Fits.getHHHName(file);
       context.setInputPath(filehhh);
       double cd = 360.0 / w;
       int crpix1 = w/2;
       int crpix2 = h/2;
       double crval1 = cd/2.; 
       double crval2 = -cd/2; 
       if( crval1<=-180 ) crval1+=360.;
       if( crval1>180 ) crval1-=360;

       BufferedWriter t = null;
       try {
          t = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(filehhh)));
          t.write("NAXIS1  = "+w);       t.newLine();
          t.write("NAXIS2  = "+h);       t.newLine();
          t.write("CRPIX1  = "+crpix1);  t.newLine();
          t.write("CRPIX2  = "+crpix2);  t.newLine();
          t.write("CRVAL1  = "+crval1);  t.newLine();
          t.write("CRVAL2  = "+crval2);  t.newLine();
          t.write("CTYPE1  = "+prefLon+"CAR"); t.newLine();
          t.write("CTYPE2  = "+prefLat+"CAR"); t.newLine();
          t.write("CD1_1   = "+(-cd));      t.newLine();
          t.write("CD1_2   = 0");        t.newLine();
          t.write("CD2_1   = 0");        t.newLine();
          t.write("CD2_2   = "+cd);      t.newLine();
       }
       finally {
          if( t!=null ) t.close();
       }

       context.setInputPath(filehhh);
    }


    // Génération des fichiers .hhh qui vont bien
    // ex CAR: [path/]Titan[.ext] 46080x23040 [23040x23040] [0]
    // le dernier chiffre indique la colonne origine des longitudes, par défaut le milieu de l'image
    //
    // ex STEREO: [path/]Titan[.ext] 20000 60
    // ext => "-N" ou "-S"
    // le premier chiffre indique la largeur de l'image en pixel, le deuxième la taille angulaire
    // correspondante en degrés
    private void generateHHH( String s1 ) throws Exception {
        int width,height;
        int wCell,hCell;
        int nlig,ncol;
        String path,name,ext;
        int origLon= -1;   // => -1 = origine des longitudes au centre de l'image (comme d'hab)
        double cd;

        boolean flagLonInverse = true;  // false si longitude céleste, true pour les planètes

        // Parsing des arguments
        Tok tok = new Tok(s1);

        // Parsing du nom de fichier  => path/image.ext
        String s = tok.nextToken();
        int i = s.lastIndexOf(File.separator);
        path = i==-1 ? "" : s.substring(0,i+1);
        int j = s.lastIndexOf('.');
        if( j==-1 ) j=s.length();
        name = s.substring(i+1,j);
        ext = s.substring(j);

        // Parsing de la taille globale de l'image
        s = tok.nextToken();
        i = s.indexOf('x');

        // Le cas CARTESIAN
        if( i>0 ) {
            width = Integer.parseInt(s.substring(0,i) );
            height = Integer.parseInt(s.substring(i+1) );

            // Parsing de la taille des imagettes (si requis)
            if( tok.hasMoreTokens() ) {
                s = tok.nextToken();
                i = s.indexOf('x');
                wCell = Integer.parseInt(s.substring(0,i) );
                hCell = Integer.parseInt(s.substring(i+1) );

                // Parsing d'une éventuelle origine des longitudes différentes de width/2
                if( tok.hasMoreTokens() ) {
                    s = tok.nextToken();
                    origLon = Integer.parseInt( s );
                }

            } else {
                wCell = width;
                hCell = height;
            }

            // On génère les fichiers .hhh
            boolean flagUniq = false;
            if( width==wCell && height==hCell ) {
                flagUniq = true;
                ncol=nlig=1;
            } else {
                ncol = (int)( Math.ceil( (double)width/wCell) );
                nlig = (int)( Math.ceil( (double)height/hCell) );
            }
            cd = 360.0 / width;

            context.info("Generation of .hhh files for CAR "+ncol+"x"+nlig+" image(s) orig="+origLon);

            int index=0;
            for( int lig=0; lig<nlig; lig++) {
                for( int col=0; col<ncol; col++, index++) {
                    String suffix = flagUniq?"":"-"+index;
                    String filename = path+name+suffix+ext;
                    File f = new File( filename );
                    if( !f.exists() ) context.warning("Missing file => "+filename);
                    String filehhh = path+name+suffix+".hhh";

                    int w = col==ncol-1 ? width-col*wCell  : wCell;
                    int h = lig==nlig-1 ? height-lig*hCell : hCell;

                    int crpix1 = w/2;
                    int crpix2 = h/2;
                    int xc = col*wCell + crpix1;
                    int yc = lig*hCell + crpix2;

                    int deltaX = (origLon==-1 ? width/2 : origLon) -xc;
                    int deltaY = height/2 -yc;
                    double crval1 = -deltaX*cd +(flagLonInverse?-cd/2.:cd/2.);   // Ne pas oublier le demi pixel de l'origine
                    double crval2 = deltaY*cd -cd/2;     // Ne pas oublier le demi pixel de l'origine
                    if( crval1<=-180 ) crval1+=360.;
                    if( crval1>180 ) crval1-=360;

                    BufferedWriter t = null;
                    try {
                        t = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(filehhh)));
                        t.write("NAXIS1  = "+w);       t.newLine();
                        t.write("NAXIS2  = "+h);       t.newLine();
                        t.write("CRPIX1  = "+crpix1);  t.newLine();
                        t.write("CRPIX2  = "+crpix2);  t.newLine();
                        t.write("CRVAL1  = "+crval1);  t.newLine();
                        t.write("CRVAL2  = "+crval2);  t.newLine();
                        t.write("CTYPE1  = RA---CAR"); t.newLine();
                        t.write("CTYPE2  = DEC--CAR"); t.newLine();
                        t.write("CD1_1   = "+(flagLonInverse?cd:-cd));      t.newLine();
                        t.write("CD1_2   = 0");        t.newLine();
                        t.write("CD2_1   = 0");        t.newLine();
                        t.write("CD2_2   = "+cd);      t.newLine();
                    }
                    catch( Exception e ) {
                       e.printStackTrace();
                    }
                    finally {
                        if( t!=null ) t.close();
                    }

                }
            }

            // Le cas STEREOGRAPHIC
        } else {
            width = Integer.parseInt(s);
            double radius = Double.parseDouble( tok.nextToken() );
            cd = (2.*Math.tan( Math.toRadians( (90-radius)/2 )) * (360/Math.PI)) /width;

            double crval1 =(flagLonInverse?-cd/2.:cd/2.);   // Ne pas oublier le demi pixel de l'origine

            // Le pole Nord et Sud
            for( int k=0; k<2; k++ ) {

                double crval2 = (k==0 ? 90 : -90) -cd/2;   // Ne pas oublier le demi pixel de l'origine

                String suffix = k==0 ? "-N":"-S";
                String filename = path+name+suffix+ext;
                File f = new File( filename );
                if( !f.exists() ) context.warning("Missing file => "+filename);
                String filehhh = path+name+suffix+".hhh";

                BufferedWriter t = null;
                try {
                    t = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(filehhh)));
                    t.write("NAXIS1  = "+width);   t.newLine();
                    t.write("NAXIS2  = "+width);   t.newLine();
                    t.write("CRPIX1  = "+width/2); t.newLine();
                    t.write("CRPIX2  = "+width/2); t.newLine();
                    t.write("CRVAL1  = "+crval1);  t.newLine();
                    t.write("CRVAL2  = "+crval2);  t.newLine();
                    t.write("CTYPE1  = RA---STG"); t.newLine();
                    t.write("CTYPE2  = DEC--STG"); t.newLine();
                    t.write("CD1_1   = "+(flagLonInverse?cd:-cd));      t.newLine();
                    t.write("CD1_2   = 0");        t.newLine();
                    t.write("CD2_1   = 0");        t.newLine();
                    t.write("CD2_2   = "+cd);      t.newLine();
                }
                finally {
                    if( t!=null ) t.close();
                }

                String filefov = path+name+suffix+".fov";
                try {
                    t = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(filefov)));
                    double xc = width/2.;
                    double yc = width/2.;
                    double r = width/2. ;
                    t.write(xc+" "+yc+" "+r);   t.newLine();
                }
                finally {
                    if( t!=null ) t.close();
                }

            }

            context.info("Generation of .hhh & .fov files for STG North and South image");
        }
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
        for( int i=0; i<args.length; i++ ) {
           String arg=args[i];
           arg = ParamObsolete.getLastSyntax(context,arg);
           if( arg==null ) continue;

            // si c'est dans un fichier
            String param = "-param=";
            if (arg.startsWith(param)) {
                try {
                    MyProperties hist=setConfigFile(arg.substring(param.length()));
                    
                    // Pour conserver l'historique des paramètres dans properties
                    for( String key : hist.getKeys() ) {
                       if( key.trim().length()==0 || key.charAt(0)=='#' ) continue;
                       String p1 = ParamObsolete.getLastSyntax(context,key+"="+hist.get(key));
                       if( p1==null ) continue;
                       String q = Tok.quote( p1 );
                       if( context.scriptCommand==null ) context.scriptCommand=q;
                       else context.scriptCommand+=" "+q;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
                continue;
            }
            

            // Juste pour pouvoir appeler directement par le main() de cette classe
            // et non celle d'Aladin
            else if( arg.equalsIgnoreCase("-skygen") || arg.equalsIgnoreCase("-hipsgen")) continue;
            
            else if( ParamOption.html.equals(arg) ) { flagHtml=true; continue; }

            // Manuel
            else if( ParamOption.man.equals(arg) ) {
               if( i<args.length-1 ) help(launcher,args[i+1]);
               else usage(launcher,FULL|(flagHtml?HTML:0));
               return;
            }
            // Help
            else if( ParamOption.h.equals(arg) ) {
               if( i<args.length-1 ) help(launcher,args[i+1]);
               else usage(launcher,(flagHtml?HTML:0));
               return;
            }

            if( first ) {
                first=false;
                context.info("Starting HipsGen "+SDF.format(new Date())+" (based on Aladin "+Aladin.VERSION+")...");
            }
            
            // Mémorisation de l'option
            String q = Tok.quote(arg);
            if( context.scriptCommand==null ) context.scriptCommand=q;
            else context.scriptCommand+=" "+q;
            
            ParamOption po=null;
                 if( ParamOption.d.equals(arg) )       { context.setVerbose(4); po=ParamOption.d; }
            else if( ParamOption.clean.equals(arg) )   { force=true; po=ParamOption.clean; }
            else if( ParamOption.trim.equals(arg) )    { trim=true; po=ParamOption.trim; }
            else if( ParamOption.gzip.equals(arg) )    { gzip=true; po=ParamOption.gzip; }
            else if( ParamOption.hhhcar.equals(arg) )  { flagHHHcar=true; po=ParamOption.hhhcar; }
            else if( ParamOption.nice.equals(arg) )    { context.mirrorDelay=500; po=ParamOption.nice; }
            else if( ParamOption.notouch.equals(arg) ) { context.notouch=true; po=ParamOption.notouch; }
            else if( ParamOption.nocolor.equals(arg) ) { context.setTerm(false); po=ParamOption.nocolor; }
            else if( ParamOption.color.equals(arg) )   { context.setTerm(true); po=ParamOption.color; }
            else if( ParamOption.clone.equals(arg) )   { context.testClonable=false; po=ParamOption.clone; }
//            else if( ParamOption.live.equals(arg) )    { context.setLive(true); po=ParamOption.live; }
            else if( ParamOption.n.equals(arg) )       { context.fake=true; po=ParamOption.n; }
            else if( ParamOption.cds.equals(arg) )     { context.cdsLint=true; po=ParamOption.cds; }
//            else if( ParamOption.check.equals(arg) )   { context.setMirrorCheck(true); po=ParamOption.check; }
//            else if( ParamOption.nocheck.equals(arg) ) { context.setMirrorCheck(false); po=ParamOption.nocheck; }
                 
            if( po!=null ) context.param(po.info());

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
                    if( a==Action.ZIP )    flagZip=true;
                    if( a==Action.UPDATE ) flagUpdate=true;
                    if( a==Action.GUNZIP ) flagGunzip=true;
                    if( a==Action.MAP )    flagHealpixMap=true;
                    if( a==Action.CHECKCODE )    flagCrc=true;
                    if( a==Action.CHECK )        flagCrc=true;
                    if( a==Action.CHECKFAST )    flagCrc=true;
                    if( a==Action.CHECKDATASUM ) flagCrc=true;
                    if( a==Action.LINT )   flagLint=true;
                    if( a==Action.TMOC )   flagTMoc=true;
                    if( a==Action.TINDEX ) flagTIndex=true;
                    if( a==Action.PROP )   flagProp=true;
                    if( a==Action.MOCERROR ) flagMocError=true;
                    if( a==Action.VALIDATOR ) flagValidator=true;
                    if( a==Action.CONCAT )  flagConcat=true;
                    if( a==Action.ABORT ) flagAbort=true;    // Bidouillage pour pouvoir tuer un skygen en cours d'exécution
                    if( a==Action.PAUSE ) flagPause=true;    // Bidouillage pour pouvoir mettre en pause un skygen en cours d'exécution
                    if( a==Action.RESUME ) flagResume=true;  // Bidouillage pour pouvoir remettre en route un skygen en pause
                    actions.add(a);
                } catch (Exception e) {
                    context.error("Unknown action/parameter ["+arg+"] !");
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

        if( flagHHHcar ) {
           try {
              generateHHHcar(context.getInputPath(),context.getFrame());
           } catch( Exception e ) {
              if( Aladin.levelTrace>=3 ) e.printStackTrace();
              context.error("hhh file generation failed for "+context.getInputPath());
              return;
           }
        }

        // Les tâches à faire si aucune n'est indiquées
        boolean all=false;
        if( actions.size()==0 && context.getInputPath()!=null ) {
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
//                    actions.add(Action.GZIP);    // JE NE GZIPPE PLUS PAR DEFAUT
                    actions.add(Action.PNG);
                }
                actions.add(Action.CHECKCODE);
                
                if( !context.isColor() && !flagMapFits ) actions.add(Action.DETAILS);
//                actions.add(Action.STMOC);
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

            if( !flagConcat && !flagMirror   && !flagZip  && !flagUpdate && !flagLint && !flagHealpixMap && !flagGunzip && !flagCrc
                            && !flagMocError && !flagProp && !flagTMoc   && !flagTIndex && !flagValidator && !flagConcat) {
                String s = context.checkHipsId(context.hipsId, true); 
                context.setHipsId(s);

                // dans le cas d'un mirroir l'ID est nécessairement fourni par les properties distantes
            } else if( flagMirror ) {
                InputStreamReader in1=null;
                try {
                    MyProperties prop = new MyProperties();
                    in1 = new InputStreamReader( Util.openAnyStream( context.getInputPath()+"/properties"), "UTF-8" );
                    prop.load(in1);
                    context.setHipsId( context.getIdFromProp(prop) );
                } catch( Exception e ) {
                    context.warning("remote properties file missing");
                } finally{  if( in1!=null ) in1.close(); }
            }

        } catch (Exception e) {
            context.error(e.getMessage());
            return;
        }
        
        if( !flagMirror && !flagLint && !flagZip && !flagCrc && !flagValidator && !flagHealpixMap ) {
           String id = context.getHipsId();
           if( id==null || (!flagUpdate && id.startsWith("ivo://UNK.AUT")) ) {
               context.warning("Missing HiPS IVOID identifier (see "+Param.id+" parameter)"
                       +(id==null?"":" => in the meantime, assuming "+id));

           }
        }
        
        // Ajustement du monde de compression des tuiles par défaut
        if( trim ) context.trim=true;
        if( gzip ) context.gzip=true;

        // Ajustement du mode par défaut dans le cas d'une génération d'une HiPS RGB
        if( flagRGB && !flagMode ) context.setModeMerge(ModeMerge.mergeOverwriteTile);

        // Ajustement de la méthode par défaut (moyenne pour les FITS, médiane first pour les couleurs)
        // à moins qu'elle n'ait été spécifiquement indiquée
        if( context.isColor() && !context.isSetModeTree() ) {
            context.setModeTree( ModeTree.treeMedian );
        }

        if( context.getModeOverlay()==ModeOverlay.overlayAdd ) {
            context.setFading(false);
            context.setLive(false);
            context.setPartitioning("false");
            context.setMixing("true");
            context.info("Pixel coadd mode="+ModeOverlay.overlayAdd+" => fading, partitioning, no-mixing and live parameter ignored");
       } 

        // Nettoyage avant ?
        if( force ) {
            context.setIgnoreStamp(true);
            if( all ) actions.add(0, Action.CLEAN);
            else {
                for( int i=0; i<actions.size() ;i++ ) {
                    Action a = actions.get(i);
                         if( a==Action.INDEX )   { actions.add(i, Action.CLEANINDEX);   i++; }
                    else if( a==Action.TINDEX )  { actions.add(i, Action.CLEANTINDEX);  i++; }
                    else if( a==Action.MIRROR )  { actions.add(i, Action.CLEANALL);     i++; }
                    else if( a==Action.DETAILS ) { actions.add(i, Action.CLEANDETAILS); i++; }
                    else if( a==Action.TILES )   { actions.add(i, Action.CLEANTILES);   i++; }
                    else if( a==Action.MAPTILES ){ actions.add(i, Action.CLEANTILES);   i++; }
                    else if( a==Action.JPEG )    { actions.add(i, Action.CLEANJPEG);    i++; }
                    else if( a==Action.PNG )     { actions.add(i, Action.CLEANPNG);     i++; }
                    else if( a==Action.RGB )     { actions.add(i, Action.CLEAN);        i++; }
                    else if( a==Action.CUBE )    { actions.add(i, Action.CLEAN);        i++; }
                    else if( a==Action.CHECKCODE ) context.setCheckForce(true);
                }
            }
        }

        if( context.fake ) context.warning("NO RUN MODE (option -n), JUST INFORMATION PRINT !!!");
        for( Action a : actions ) {
            context.param("Action => "+a+": "+a.info());
            if( !flagMapFits && a==Action.MAPTILES ) flagMapFits=true;
        }

        // Alertes sur les paramètres inutiles
        for( Param p : listParam ) {
           if( !p.checkActions(actions) ) context.warning(p+" not use for these actions");
        }

        // Positionnement du frame par défaut
        if( !flagRGB && !flagMapFits ) setDefaultFrame();

        // C'est parti
        try {
            endOfWork=false;

            // Création d'un cache disque si nécessaire
            MyInputStreamCached.context = context;
            if( !context.fake && (cache!=null || cacheSize!=-1) ) {
                MyInputStreamCached.setCache( cache==null ? null : new File(cache), cacheSize );
            }

            long t = System.currentTimeMillis();
            new Task(context,actions,true);
            if( context.isTaskAborting() ) context.abort(context.getTitle("(aborted after "+Util.getTemps((System.currentTimeMillis()-t)*1000L),'='));
            else {
                // Suppression du cache disque si nécessaire
                if( cacheRemoveOnExit ) MyInputStreamCached.removeCache();
                if( context.nbPilot>0 ) context.warning("Pilot test limited to "+context.nbPilot+" images => partial HiPS");
//                    else context.info("Tip: Edit the \"properties\" file for describing your HiPS (full description, copyright, ...)");
                context.removeListReport();
                context.done(context.getTitle("THE END (done in "+Util.getTemps((System.currentTimeMillis()-t)*1000L)+")",'='));
            }

        } catch (Exception e) {
            if( context.getVerbose()>0 ) e.printStackTrace();

            // Suppression du cache disque si spécifique pour éviter d'avoir à recommencer
            if( cacheRemoveOnExit && cache!=null ) MyInputStreamCached.removeCache();

            context.error(e.getMessage());

            context.removeListReport();

        } finally {
            endOfWork=true;
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
                if( context.isExistingAllskyDir()  ) frame=force?"equatorial":"galactic";
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

    public static final int FULL = 1;
    public static final int HTML = 2;

    // Aladin.jar -hipsgen
    private static void usage(String launcher) { usage(launcher,0); }
    private static void usage(String launcher,int mode) {
       
       boolean flagHtml = (mode&HTML)!=0;
       
       if( flagHtml ) System.out.println("<HTML><H1>Hipsgen reference manual"
          + "<BR><FONT SIZE=-1>related to Hipsgen/Aladin "+Aladin.VERSION+"</FONT></H1>\n<PRE>\n");
       System.out.println("Usage: java -jar "+launcher+" in=dir [otherParams ...ACTIONs...]");
       System.out.println("       java -jar "+launcher+" -param=configfile [...ACTIONs...]\n");
       System.out.println("       java -jar "+launcher+" -h");
       System.out.println("       java -jar "+launcher+" -man [param|ACTION]\n");
       System.out.println("HiPS generator from a set of source images. Provides additional\n"
             + "HiPS manipulation utilities (duplication, concatenation, checking, etc).");
       System.out.println("The parameters are provided in the configfile, or directly on the command line.");
       System.out.println("Default actions: "+Action.defaultList()+"\n");
       System.out.println("Available options:");
       System.out.println( ParamOption.help());
       System.out.println("Ex: java -jar "+launcher+" in=/MyImg "+Param.id+"=AUT/P/myhips    => Do all the job" +
             "\n    java -jar "+launcher+" in=/MyImg "+Param.id+"=AUT/P/myhips INDEX TILES" +
             "\n           => Generate the spatial index and the FITS tiles only" +
             "\n    java -jar "+launcher+" in=HiPS1 out=HiPS2 CONCAT    => Concatenate HiPS1 to HiPS2" +
             "\n    java -jar "+launcher+" in=http://remote/hips MIRROR => copy the remote HiPS locally"
             //                         "\n    java -jar Aladin.jar -mocgenred=/MySkyRed redparam=sqrt blue=/MySkyBlue output=/RGB rgb  => compute a RGB all-sky"
             );

       System.out.println("\n(c) Université de Strasbourg/CNRS 2018-2023 - "+launcher+" based on Aladin "+Aladin.VERSION+" from CDS\n");
       if( flagHtml ) {
          System.out.println("</PRE>\n<BR><BR<BR><H2>Available actions</H2>");
          System.out.println( Action.help(launcher,mode));
          System.out.println("<BR><BR<BR><H2>Available parameters</H2>");
          System.out.println( Param.help(launcher,mode));
          System.out.println("</HTML>\n");
       } else {
          System.out.println("Available actions:");
          System.out.println( Action.help(launcher,mode));
          System.out.println("Available parameters:");
          System.out.println( Param.help(launcher,mode));
       }
    }

    private void help(String launcher,String opt) {
       opt = ParamObsolete.getLastSyntax(context, opt);
       if( opt==null ) return;
       try {
          Param p = Param.get(opt);
          System.out.println( p.fullHelp(launcher,0));
          return;
       } catch( Exception e) {}
       try {
          Action p = Action.get(opt);
          System.out.println( p.fullHelp(launcher,0));
          return;
       } catch( Exception e) {}
    }

    private MyProperties setConfigFile(String configfile) throws Exception {
        this.file = new File(configfile);
        return parseConfig();
    }

    public static void main(String[] args) {
        HipsGen generator = new HipsGen();
        generator.launcher="Hipsgen";
        generator.execute(args);
    }
}
