// Copyright 2010 - UDS/CNRS
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

package cds.aladin;

import java.awt.Graphics;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.Properties;

import cds.fits.Fits;
import cds.fits.HeaderFits;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;

/**
 * Plan gérant une image au format Healpix
 *
 * @author Thomas Boch [CDS]
 *
 */
public class PlanHealpix extends PlanBG {
   
   static final public String PROPERTIES = "properties";

    // noms des clés utilisés dans le fichier properties
    static public final String KEY_ORIGINAL_PATH = "dataPath";
    static public final String KEY_LOCAL_DATA = "localData";
    static public final String KEY_GZ = "gzipped";
    static public final String KEY_OFFSET = "offset";
    static public final String KEY_SIZERECORD = "sizeRecord";
    static public final String KEY_PROCESSING_DATE = "processingDate";
    static public final String KEY_LAST_MODIFICATON_DATE = "lastModified";
    static public final String KEY_NSIDE_PIXEL = "nsidePixel";
    static public final String KEY_NSIDE_FILE = "nsideFile";
    static public final String KEY_ORDER_GENERATED_IMGS = "orderGeneratedImgs";
    static public final String KEY_TFIELDS = "tfields";
    static public final String KEY_TTYPES = "ttypes";
    static public final String KEY_LENHPX = "lenhpx";
    static public final String KEY_TYPEHPX = "typehpx";
    static public final String KEY_ISPARTIAL = "isPartial";
    static public final String KEY_ARGB = "ARGB";
    static public final String KEY_ORDERING = "ordering";
    static public final String KEY_NBPIXGENERATEDIMAGE = "nbPixGeneratedImage";
    static public final String KEY_CURTFORMBITPIX = "curTFormBitpix";
    static public final String KEY_COORDSYS = "coordsys";
    // Aladin version used to generate the properties file
    static public final String KEY_ALADINVERSION = "aladinVersion";

    static final int POLA_SEGMENT_MAGIC_CODE = -42;
    static final int POLA_AMPLITUDE_MAGIC_CODE = -41;
    static final int POLA_ANGLE_MAGIC_CODE = -40;

    static Object lock = new Object();

    // systeme de coordonnees (keyword FITS COORDSYS) : G (galactique), E (ecliptique) ou C (Celestial==ICRS)
    private char coordsys;

    private int nSideFile;
    private int sizeRecord;
    private int nRecord;
    private int nField; // nombre de TFORM différents
    private char[] typeHpx;
    private int[] lenHpx;
    private String dirName;
    private String ordering; // RING or NESTED
    private double badData;  // Valeur pour le BAD_DATA
    private boolean hasBadData=false; // true si une valeur BAD_DATA a été positionnée

    protected int idxTFormToRead = 0; // indice du TFORM auquel on s'intéresse
    protected String[] tfieldNames; // noms des différents TFORM
//    private double minData; // valeur min de l'autocut global
//    private double maxData; // valeur max de l'autocut global
    private int newNSideImage;
    private int newNSideFile;
    private int hpxOrderGeneratedImgs;

    private boolean isLocal; // true if the original file is a local file
    private boolean isGZ = false;
    private boolean isPartial = false; // true if partial Healpix mode
    private boolean isARGB=false;      // True if ARGB Healpix map (32 int bits, COLORMOD ARGB)
    private double[] partialHpxPixIdx; // indexes of healpix pixels
    private int nbRecordsPartial;

    private String originalPath;
    private String pixelPath; // differs from originalPath if gzipped, or distant file
    private String tempFilePath;

    private int nbPixGeneratedImage; // taille des losanges images générées

    private int curTFormBitpix;

    private int myAllskyMode = FIRST; // mode de creation des images de niveau superieur et de l'image all sky : FIRST, MOYENNE, ...

    private boolean fromProperties; // true si la création du plan a été demandée depuis la fenetre des properties. Dans ce cas, on ne touche pas à idxTFormToRead, même pour les fichiers partiels

	/** @param mode : DRAWPIXEL : les pixels, DRAWPOLARISATION : les segments de polarisation, DRAWANGLE : les angles sous forme d'une image */
    public PlanHealpix(Aladin aladin, String file, MyInputStream in, String label, int mode, int idxTFormToRead, boolean fromProperties) {
        super(aladin);

        this.fromProperties = fromProperties;
        init(file, in, label, idxTFormToRead);
        setDrawMode(mode);

        threading();
    }


    // juste pour les classes derivees
    public PlanHealpix(Aladin aladin) {
        super(aladin);
    }

    protected void log() {}

    private void init(String file, MyInputStream in,String label,int idxTFormToRead) {
        if (file != null && (file.startsWith("http") || file.startsWith("ftp"))) {
            isLocal = false;
        } else {
            isLocal = true;
        }

        this.idxTFormToRead = idxTFormToRead;

        video = VIDEO_NORMAL;
        flagOk = false;
        isOldPlan = false;
        type = ALLSKYIMG;
        frameOrigin = Localisation.GAL;
        this.dis = in;
        this.filename = file;
        cacheID = survey = file;
        this.originalPath = file;

        int i = file.lastIndexOf(Util.FS);
        if (i > 0) {
            survey = survey.substring(i + 1);
        }
        setLabel(label==null ? survey : label);

        this.dirName = getDirname() + Util.FS + dirNameForIdx(idxTFormToRead);

        this.survey = this.dirName;
//        System.out.println("survey=["+survey+"]");
        aladin.log("AllskyMap",label);
    }

    private String getDirname() {
        String s = null;
        if (isLocal) {
            File f = new File(this.originalPath);
            try {
               s = f.getCanonicalPath().replace('/', '_')
                               .replace('\\', '_').replace(':', '_').replace('.', '_')
                               .replace('?', '_');
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            s = this.originalPath.replace('/', '_')
                           .replace('\\', '_').replace(':', '_').replace('.', '_')
                           .replace('?', '_');
        }
        return s;
    }

    private String dirNameForIdx(int idx) {
        switch (idx) {

        case POLA_SEGMENT_MAGIC_CODE:
            // le repertore de polarisation contient des fichier MEF avec Q et U
            return "polarisation";

        case POLA_AMPLITUDE_MAGIC_CODE:
            // le repertoire contenant les fichiers d'amplitude de polarisation
            return "polarisation_amplitude";

        case POLA_ANGLE_MAGIC_CODE:
            // le repertoire contenant les fichiers d'angle de polarisation
            return "polarisation_angle";

        default:
            return "TFIELD"+(idx+1);
        }
    }


    @Override
    protected boolean Free() {
        ringValues = null;
        return super.Free();
    }

    // clean temporary file
    private void cleanup() {
        if (tempFilePath!=null ) {
            try {
                new File(tempFilePath).delete();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            tempFilePath = null;
        }

        // mise à null des tableaux de travail
        partialValues = null;
        partialHpxPixIdx = null;
    }

    /** Retourne le Nordre des losanges */
    @Override
    protected int getLosangeOrder() { return (int)log2(nbPixGeneratedImage); }

    private boolean dumpStreamToFile(InputStream srcStream, File destFile) {
        Aladin.trace(2, "Dumping input stream to temp file: "+destFile.getName());
        BufferedInputStream bis = new BufferedInputStream(srcStream);
        BufferedOutputStream bos;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(destFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        byte[] buf = new byte[1024];
        int len;
        try {
            while ((len = bis.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }

        bis.close();
        bos.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }

        return true;
    }

    private void setPixelPath() throws IOException {
        if (isLocal && !isGZ) {
            this.pixelPath = this.originalPath;
        } else {
            this.tempFilePath = aladin.createTempFile("PlanHealpix",
                    ".hpx").getCanonicalPath();
            dumpStreamToFile(dis, new File(this.tempFilePath));
            this.pixelPath = tempFilePath;
        }

        Aladin.trace(2, "isLocal: " + isLocal + ", isGZ: " + isGZ
                + ", originalPath: " + originalPath + ", pixelPath: "
                + pixelPath);
    }

    @Override
    protected boolean waitForPlan() {
        try {

            boolean needProcessing = needProcessing(this.dirName, true);
            if (needProcessing) { // pour eviter de charger un flux distant alors qu'on a deja les donnees

                try {
                    this.isGZ = dis.isGZ();
                    this.isARGB = (dis.getType() & MyInputStream.ARGB) != 0;
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            setPixelPath();

            startHealpixCreation();
            if (needProcessing) writePropertiesFile(this.dirName);

            suite();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // the properties file will be used to check the file modification date
    // and to store other parameters
    private boolean writePropertiesFile(String dir) {
        Properties prop = new Properties();
        prop.setProperty(KEY_ORIGINAL_PATH, originalPath);
        if (isLocal) {
            try {
                String modifDate = new File(originalPath).lastModified() + "";
                prop.setProperty(KEY_LAST_MODIFICATON_DATE, modifDate);
            } catch (Exception e) {}
        }
        prop.setProperty(KEY_PROCESSING_DATE, DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
        prop.setProperty(KEY_NSIDE_FILE, newNSideImage+""); // TODO : oui, je sais, j'ai merdé sur les noms !! newNSideImage devrait s'appeler newNSideFile
        prop.setProperty(KEY_NSIDE_PIXEL, newNSideFile+""); // et newNSideFile devrait s'appeler newNSidePixel

        prop.setProperty(KEY_ORDER_GENERATED_IMGS, hpxOrderGeneratedImgs+"");

        prop.setProperty(KEY_ORDERING, ordering);

        prop.setProperty(KEY_TFIELDS, nField+"");

        prop.setProperty(KEY_TTYPES, Util.join(tfieldNames, ','));

        prop.setProperty(KEY_LOCAL_DATA, isLocal+"");

        prop.setProperty(KEY_GZ, isGZ+"");

        prop.setProperty(KEY_OFFSET, initialOffsetHpx+"");

        prop.setProperty(KEY_SIZERECORD, sizeRecord+"");

        prop.setProperty(KEY_ISPARTIAL, isPartial+"");

        prop.setProperty(KEY_ARGB, isARGB+"");

        prop.setProperty(KEY_NBPIXGENERATEDIMAGE, nbPixGeneratedImage+"");

        prop.setProperty(KEY_CURTFORMBITPIX, curTFormBitpix+"");

        prop.setProperty(KEY_COORDSYS, coordsys+"");

        prop.setProperty(KEY_LENHPX, Util.join(lenHpx, ','));

        prop.setProperty(KEY_TYPEHPX, Util.join(typeHpx, ','));

        prop.setProperty(KEY_ALADINVERSION, aladin.VERSION);


        try {
            prop.store(new FileOutputStream(propertiesFile(dir)), null);
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    private void suite() {

       url = getCacheDir()+Util.FS+survey;
       minOrder = 3;
       maxOrder = 3;
//       useCache = true;
       useCache=false;
       truePixels = inFits = true;
       inJPEG = false;
       color = isARGB;
       Aladin.trace(3, this+"");

       from="local";
       co = new Coord(0,0);
       Localisation.frameToFrame(co, Localisation.GAL, Localisation.ICRS);
       objet = co+"";
       Projection p =new Projection("test",Projection.WCS,co.al,co.del,60*4,60*4,250,250,500,500,0,false,Calib.SIN,Calib.FK5);
       p.frame = getFrame();

       setNewProjD(p);
       initZoom=1./ (Aladin.OUTREACH?64:32);

//       timerLastDrawBG=-1;
       active=selected=true;

       pixList = new Hashtable<String, HealpixKey>(1000);
       allsky=null;
       loader = new HealpixLoader();

       creatDefaultCM();
    }


    /** Commence la creation de la boule Healpix à partir du flux passé en param
     *
     * @param in le flux du fichier Healpix
     */
    private void startHealpixCreation() {
        Aladin.trace(2,"Loading HEALPIX FITS image");

        try {
            File tmp = new File(getCacheDir()+Util.FS+this.dirName);

            if( ! needProcessing(this.dirName, true) ) {
               return;
            }
            tmp.mkdir();
        }
        catch(Exception e) {
            e.printStackTrace();
        }


        double start = System.currentTimeMillis();
        MyInputStream isTmp = null;
        try {
            isTmp = new MyInputStream(new FileInputStream(pixelPath));
        } catch (FileNotFoundException e3) {
            e3.printStackTrace();
        } catch (IOException e3) {
            e3.printStackTrace();
        }

        try {
            headerFits = new FrameHeaderFits(isTmp);
        } catch (Exception e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
            return;
        }

        int naxis = headerFits.getIntFromHeader("NAXIS");
        // S'agit-il juste d'une entête FITS indiquant des EXTENSIONs
        if( naxis<=1 && headerFits.getStringFromHeader("EXTEND")!=null ) {
           // Je saute l'éventuel baratin de la première HDU
           try {
              naxis1 = headerFits.getIntFromHeader("NAXIS1");
              isTmp.skip(naxis1);
           } catch( Exception e) {}
           try {
              // On se cale sur le prochain segment de 2880
              long pos = isTmp.getPos();
              if( pos%2880!=0 ) {
                 long offset = ((pos/2880)+1) *2880  -pos;
                 isTmp.skip(offset);
              }
              headerFits = new FrameHeaderFits(isTmp);
           } catch (Exception e) {
               // TODO Auto-generated catch block
               e.printStackTrace();
               return;
           }
        }

        int nside=0;
        int nsideImage=0;
        int minLevel = 3; // Norder minimum désiré
        initialOffsetHpx = isTmp.getPos();
        try {

            nside = headerFits.getIntFromHeader("NSIDE");
            int maxSizeGeneratedImage = 512;
            if( nside<maxSizeGeneratedImage ) maxSizeGeneratedImage=nside;      // PF : Pour pouvoir charger des "petits cieux"
            Aladin.trace(3, "maxSizeGeneratedImage: "+maxSizeGeneratedImage);
            nbPixGeneratedImage = 2*maxSizeGeneratedImage;
            nSideFile = nside;
            double levelImage;
            do {
                nbPixGeneratedImage /= 2;
                levelImage = getLevelImage(nside, nbPixGeneratedImage);
            }
            // TODO : à voir avec Pierre
            while( levelImage<minLevel ); // niveau minimum : minLevel (=3)


            nsideImage = (int)CDSHealpix.pow2((long)levelImage);
            Aladin.trace(3, "NSIDE image: "+nsideImage);
            Aladin.trace(3, "Level image : "+levelImage);
            Aladin.trace(3, "nb pixels generated image : "+nbPixGeneratedImage);
        }
        catch(Exception e) {e.printStackTrace();}


        naxis1 = sizeRecord = headerFits.getIntFromHeader("NAXIS1");
        naxis2 = nRecord = headerFits.getIntFromHeader("NAXIS2");
        nField = headerFits.getIntFromHeader("TFIELDS");
        ordering = headerFits.getStringFromHeader("ORDERING");

        Aladin.trace(3, "sizeRecord: "+sizeRecord);
        Aladin.trace(3, "nRecord: "+nRecord);
        Aladin.trace(3, "ordering: "+ordering);

        // Recherche d'une valeur BAD_DATA - PF mars 2010
        try {
           badData = Double.parseDouble(headerFits.getStringFromHeader("BAD_DATA"));
           hasBadData=true;
           System.out.println("BAD_DATA: "+badData);
        } catch( Exception e ) {
//           // Pour HEALPIX - A VIRER LORSQUE LE MOT CLE BAD_DATA SERA POSITIONNE
//           badData=-1.637499996306027E30/*-1.6375E+30*/; hasBadData=true;
//           System.out.println("PLANCK default BAD_DATA: "+badData);
        }

        // Recherche du COORDSYS
        try {
            String s = headerFits.getStringFromHeader("COORDSYS");
            coordsys = s.charAt(0);
        } catch (Exception e) {
            // coordsys galactic par defaut
            coordsys = 'G';
        } finally {
            frameOrigin = coordsysToFrame(coordsys);
        }

        Aladin.trace(3, "COORDSYS vaut "+coordsys);


        typeHpx = new char[nField];  // type de données pour le champ
        lenHpx = new int[nField];     // nombre d'items du champ

        tfieldNames = new String[nField];

        // Boucle sur chaque champ
        for( int i=0; i<nField; i++ ) {

           // Récupération du nom du champ
           String fName = headerFits.getStringFromHeader("TTYPE"+(i+1));
           if (fName==null) fName = "TTYPE"+(i+1);
           tfieldNames[i] = fName;

           // Récupération du format d'entrée
           String s=null;
           try { s = headerFits.getStringFromHeader("TFORM"+(i+1)); } catch( Exception e1 ) {}
           int k;

           // Nombre d'items par champ
           lenHpx[i]=0;
           for( k=0; k<s.length() && Character.isDigit(s.charAt(k)); k++ ) {
              lenHpx[i] = lenHpx[i]*10 + (s.charAt(k)-'0');
           }
           if( k==0 ) lenHpx[i]=1;
           typeHpx[i] = s.charAt(k);
        }

        try {
            rafHpx = new RandomAccessFile(pixelPath, "r");
        }
        catch(FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return;
        }


        hpxOrderGeneratedImgs = (int)log2(nbPixGeneratedImage);
        Aladin.trace(3, "hpxOrderGeneratedImgs: "+hpxOrderGeneratedImgs);
        createHealpixOrder(hpxOrderGeneratedImgs);


        newNSideImage = nsideImage;
        maxOrder = (int)log2(newNSideImage);
        Aladin.trace(3, "MAXORDER: "+maxOrder);

        newNSideFile = nside;


        generateHierarchy(idxTFormToRead);
        double end = System.currentTimeMillis();

        Aladin.trace(3, "TOTAL TIME: "+(end-start)/1000.0+" s");

    }

    private void generateHierarchy(int idxTForm) {
        generateHierarchy(idxTForm, myAllskyMode);
    }

    /**
     * Generate hierarchy data for index idxTform
     * @param idxTForm TFORM index to use
     * @param modeAllsky mode to use for all sky data generation (
     */
    private void generateHierarchy(int idxTForm, int modeAllsky) {
        // plans de polarisation
        if (idxTForm<0) {
            generatePolarisationData();
            long size = Util.dirSize(new File(getCacheDir() + Util.FS + getDirname() + Util.FS + dirNameForIdx(idxTForm)));
            addInCache(size/1024);
            return;
        }


        int nbGeneratedImg = 12*newNSideImage*newNSideImage;
        Aladin.trace(3, "Nb images to generate: "+nbGeneratedImg);


        Aladin.trace(3, "nbPixGeneratedImage : "+nbPixGeneratedImage);

        // on récupère l'ensemble des pixels RING
        if (ordering.equals("RING")) {
            fillRingValues(0, 12*newNSideFile*newNSideFile, idxTForm);
        }


        if (tfieldNames.length>0 && tfieldNames[0].equals("PIXEL") ) {
            isPartial = true;

            // on skippe le premier champ qui donne les numéros Healpix des pixels
            if (idxTForm==0 && ! fromProperties) {
                idxTForm = 1;
                idxTFormToRead = 1;
                this.dirName = getDirname() + Util.FS + dirNameForIdx(idxTFormToRead);
                this.survey = this.dirName;
            }

            int sizeOneRec = 0; // taille d'un record
            for (int i=0; i<typeHpx.length; i++) {
                sizeOneRec += Util.binSizeOf(typeHpx[i], lenHpx[i]);
            }
            nbRecordsPartial = naxis1*naxis2/sizeOneRec;

            Aladin.trace(3, "nbRecordsPartial: "+nbRecordsPartial);


            if (ordering.equals("NESTED") ) {
                // on passe 0 comme indice, car le champ PIXEL est en position 0
                partialHpxPixIdx = getValuesNested(0, nbRecordsPartial, rafHpx, initialOffsetHpx, 0);
            }
            else {
                partialHpxPixIdx = getValuesRing(0, nbRecordsPartial, newNSideFile);
            }
            fillPartialValues(idxTForm);
        }


        this.curTFormBitpix = getBitpixFromFormat(typeHpx[idxTForm]);

        String dir = getDirname() + Util.FS + dirNameForIdx(idxTForm);

        //// Creation des images ////
        double fct = 85./nbGeneratedImg;
        for( int i=0; i<nbGeneratedImg; i++ ) {
            pourcent += idxTFormToRead>=0 ? fct : fct/3.;
            Fits out = new Fits(nbPixGeneratedImage, nbPixGeneratedImage, curTFormBitpix);
            buildDoubleHealpix(newNSideImage, i, newNSideFile, out, idxTForm, dir);
        }

        // on rend la mémoire
        ringValues = null;


        int curNorder = (int)log2(newNSideImage);
        Fits[] fils = new Fits[4];
        for (int i=0; i<4; i++) {
            fils[i] = new Fits();
        }

        Aladin.trace(3, "curNorder: "+curNorder);
        // création des Norder de plus basse résolution jusqu'à 3
        for (int norder=curNorder-1; norder>=3 ; norder--) {
            // génération des fichiers de niveau norder
            int nbPix = (int)(12*Math.pow(CDSHealpix.pow2(norder), 2));
            for (long npix=0; npix<nbPix; npix++) {
                for (int i=0; i<4; i++) {
                    try {
                        fils[i].loadFITS(getFilePath(getCacheDir() + Util.FS + dir,
                                norder+1, npix*4+i)+".fits");
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                try {
                    createNodeHpx(getFilePath(getCacheDir() + Util.FS + dir,
                            norder, npix)+".fits", norder, npix, fils, myAllskyMode);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        // création du fichier allsky pour le niveau le plus haut (3)
        try {
            createAllSky(getCacheDir(), dir, 3, 64, myAllskyMode);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // ajout taille fichiers créés dans cache
        String dir2 = getCacheDir() + Util.FS + getDirname() + Util.FS + dirNameForIdx(idxTForm);
//        System.out.println(dir2);
        long size = Util.dirSize(new File(dir2));
        addInCache(size/1024);

        cleanup();
    }

    /**
     * Checks whether the files to generate have already been produced and are stored in the cache
     * @return false if the files are in the cache. true otherwise
     */
    private boolean needProcessing(String dir, boolean readProperties) {

        // check if the Allsky file exists and has a sensible size
        File tmp = new File(getCacheDir() + Util.FS + dir
                + Util.FS + "Norder3" + Util.FS + "Allsky.fits");
        if ( ! (tmp.exists() && tmp.length() > 0) ) {
            return true;
        }


        File propFile = propertiesFile(dir);
        java.util.Properties prop = new java.util.Properties();
        try {
            prop.load(new FileInputStream(propFile));
        } catch (Exception e) {
            File dirToRemove = new File(getCacheDir() + Util.FS + getDirname());
            Util.deleteDir(dirToRemove);
            return true;
        }


        // if local file, check last modification date
        if (isLocal) {
            String modifDate = prop.getProperty(KEY_LAST_MODIFICATON_DATE);
            double modifDateValue;
            try {
                modifDateValue = Double.valueOf(modifDate).doubleValue();
            }
            catch(Exception e) {
                return true;
            }
            if (modifDate==null || modifDateValue==0L || modifDateValue!=new File(originalPath).lastModified() ) {
                return true;
            }
        }

        if ( !readProperties) {
            return false;
        }

        /**** Arrivé à ce niveau : pas besoin de processing, ***/
        /**** on recupere juste les valeurs dont on a besoin dans le fichier properties ****/
        // si exception : on relance le processing

        // on "touch" le répertoire parent pour qu'il soit considere comme recent
        Util.touch(new File(getCacheDir() + Util.FS + getDirname()), false);

        try {
            // check Aladin version, and force reprocessing if it differs from current version
            String version = prop.getProperty(KEY_ALADINVERSION);
            if (version==null || ! version.equals(aladin.VERSION)) {
                Aladin.trace(3, "Detected a different version of Aladin, recreate the Healpix files");
                File dirToRemove = new File(getCacheDir() + Util.FS + getDirname());
                Util.deleteDir(dirToRemove);
                return true;
            }

            // coordsys
            String s = prop.getProperty(KEY_COORDSYS, "G");
            coordsys = s.charAt(0);
            frameOrigin = coordsysToFrame(coordsys);

            isLocal = new Boolean(prop.getProperty(KEY_LOCAL_DATA))
                    .booleanValue();

            isGZ = new Boolean(prop.getProperty(KEY_GZ)).booleanValue();

            isARGB = new Boolean(prop.getProperty(KEY_ARGB)).booleanValue();

            originalPath = prop.getProperty(KEY_ORIGINAL_PATH);

            initialOffsetHpx = new Long(prop.getProperty(KEY_OFFSET)).longValue();

            sizeRecord = new Integer(prop.getProperty(KEY_SIZERECORD)).intValue();

            // on va remplir les valeurs de newNSideImage et newSideFile dont a
            // besoin getOnePixelFromCache
            newNSideImage = Integer.parseInt(prop.getProperty(KEY_NSIDE_FILE));
            newNSideFile = Integer.parseInt(prop.getProperty(KEY_NSIDE_PIXEL));
            hpxOrderGeneratedImgs = Integer.parseInt(prop
                    .getProperty(KEY_ORDER_GENERATED_IMGS));
            ordering = prop.getProperty(KEY_ORDERING);

            // on recupere le nombre de champs et leurs noms
            nField = Integer.parseInt(prop.getProperty(KEY_TFIELDS));
            tfieldNames = Util.split(prop.getProperty(KEY_TTYPES), ",");

            // healpix partiel
            isPartial = new Boolean(prop.getProperty(KEY_ISPARTIAL))
                    .booleanValue();

            // nbPixGeneratedImage
            nbPixGeneratedImage = Integer.parseInt(prop
                    .getProperty(KEY_NBPIXGENERATEDIMAGE));

            // curTFormBitpix
            curTFormBitpix = Integer.parseInt(prop
                    .getProperty(KEY_CURTFORMBITPIX));

            // lenHpx
            lenHpx = Util.splitAsInt(prop.getProperty(KEY_LENHPX), ",");

            // typeHpx
            typeHpx = Util.splitAschar(prop.getProperty(KEY_TYPEHPX), ",");


        } catch (Exception e) {
            e.printStackTrace();
            // on relance la generation en cas d'exception et on supprime un eventuel repertoire existant
            File dirToRemove = new File(getCacheDir() + Util.FS + getDirname());
            Aladin.trace(3, "Exception while reading properties file, remove directory "+dirToRemove
                            + " and recreate Healpix files");
            Util.deleteDir(dirToRemove);
            return true;
        }

        return false;
    }

    private File propertiesFile(String dir) {
        return new File(getCacheDir()+Util.FS+dir+Util.FS+PROPERTIES);
    }

    private int getBitpixFromFormat(char t) {
        switch (t) {
        case 'I':
            return 16;
        case 'J':
           return 32;
        case 'K':
           return 64;
        case 'E':
            return -32;
        case 'D':
            return -64;

        default:
            return -1;
        }
    }

    // Pour éviter les tests à chaque fois
    private String dirCache=null;
    private boolean flagCache=false;
    static final String CACHEHPX = "HPX";
    /** Retourne le répertoire du Cache ou null si impossible à créer */
    @Override
	public String getCacheDir() {
       if( flagCache ) return dirCache;
       flagCache=true;

       String dir = System.getProperty("user.home")+Util.FS+Aladin.CACHE+Util.FS+Cache.CACHE;

       // Création de $HOME/.aladin/Cache/HPX si ce n'est déjà fait
       dir = dir+Util.FS+CACHEHPX;
       File f = new File(dir);
       if( !f.isDirectory() && !f.mkdirs() ) return null;

       dirCache=dir;
       return dir;
    }

    static protected String getCacheDirPath() {
        return System.getProperty("user.home")+Util.FS+Aladin.CACHE+Util.FS+Cache.CACHE+Util.FS+CACHEHPX;
    }

    private double[] getValues(long lowHealpixIdx, long highHealpixIdx,
                                RandomAccessFile raf, long initialOffset, long idxTForm) {
        if (isPartial && ordering.equals("NESTED")) {
            return getValuesPartialNested(lowHealpixIdx, highHealpixIdx);
        }
        else if (isPartial && ordering.equals("RING")) {
            return getValuesPartialRing(lowHealpixIdx, highHealpixIdx);
        }
        else if (ordering.equals("RING")) {
            return getValuesRing(lowHealpixIdx, highHealpixIdx, nSideFile);
        }
        else {
            return getValuesNested(lowHealpixIdx, highHealpixIdx, raf, initialOffset, idxTForm);
        }
    }

    private double[] getValuesRing(long lowHealpixIdx, long highHealpixIdx, int nside) {
        return fillRingBuffer((int)lowHealpixIdx, (int)highHealpixIdx, nside);
    }

    private double[] getValuesNested(long lowHealpixIdx, long highHealpixIdx,
            RandomAccessFile raf, long initialOffset, long idxTForm) {




        // TODO : ATTENTION : si lowHealpixIdx n'est pas un multiple du format (1024E par exemple), ça ne marchera pas
        double val = 0; // Valeur du champ courant

        int nbValues = (int) (highHealpixIdx - lowHealpixIdx + 1);
        double[] result = new double[nbValues];


        // On prend un buffer assez grand pour tout recuperer en une fois
        // (raisonnable quand on genere des images 512*512)
        int nbRowsToRead = nbValues / lenHpx[(int)idxTForm];
//        System.out.println("nbValues: "+nbValues);
//        System.out.println("NB ROWS TO READ: "+nbRowsToRead);
//        System.out.println(sizeRecord);
        int sizeBuf = nbRowsToRead * sizeRecord;
        byte[] buf = new byte[sizeBuf];

        int nbRowsToSkip = (int) lowHealpixIdx / lenHpx[(int)idxTForm];
        // on se place à l'endroit qui nous intéresse et on lit
        try {

            // System.out.println("NB ROWS TO SKIP: "+ nbRowsToSkip);
            raf.seek(initialOffset + nbRowsToSkip * sizeRecord);
            try {
                raf.readFully(buf);
            } catch (EOFException e) {
            } catch (IOException e) {
                e.printStackTrace();
            }
            // System.out.println(buf.length);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        int resultIdx = 0;
        int offset = 0;
        int offsetField;
        for( int idxRow=0; idxRow<nbRowsToRead; idxRow++, offset+=sizeRecord ) {
            offsetField = 0;
            for( int idxField=0; idxField<nField; idxField++ ) {
                if( idxField!=idxTForm ) {
                    offsetField += Util.binSizeOf(typeHpx[idxField], lenHpx[idxField]);
                    continue;
                }
                int totalOffset = offset+offsetField;
                for( int k=0; k<lenHpx[idxField]; k++, resultIdx++ ) {
                    int offsetc = Util.binSizeOf(typeHpx[idxField],k);
                    switch(typeHpx[idxField]) {
                    case 'I': val = getShort(buf,totalOffset+offsetc); break;
                    case 'J': val = getInt(buf,totalOffset+offsetc); break;
                    case 'K': val = (((long)getInt(buf,totalOffset+offsetc))<<32) 
                                             | (((long)getInt(buf,totalOffset+offsetc+4))& 0xFFFFFFFFL); break;
                    case 'E': val = Float.intBitsToFloat( getInt(buf,totalOffset+offsetc) );  break;
                    case 'D': long a = (((long)getInt(buf,totalOffset+offsetc))<<32) 
                                             | (((long)getInt(buf,totalOffset+offsetc+4))& 0xFFFFFFFFL);
                              val = Double.longBitsToDouble(a); break;
                    default: val=-1;
                }
                result[resultIdx] = val;
                }
            }
        }

        return result;
    }

    public static long getHealpixMin(int n1, long n, int n2, boolean nside) {
        if (nside)
            return n*(long)(Math.pow(4,(log2(n2) - log2(n1))/log2(2)));
        else
            return n*(long)(Math.pow(4,(n2 - n1)/log2(2)));
    }

    public static long log2 (long x) { return CDSHealpix.log2(x); }

    /** Retourne le numéro du pixel père (pour l'order précédent) */
    static public long getFather(long npix) { return npix/4; }


    // PF janv 2011 - remplacement par une Hashmap 
    //pour ne plus faire exploser la mémoire en cas de grand NSIDE
//    private double[] partialValues;
    HashMap partialValues;
    private void fillPartialValues(long idxTForm) {
//        partialValues = new double[12*newNSideFile*newNSideFile];
        partialValues = new HashMap();
        double[] values;
        if( ordering.equals("NESTED")) {
            values = getValuesNested(0, nbRecordsPartial, rafHpx, initialOffsetHpx, idxTForm);
        }
        else {
            values = getValuesRing(0, nbRecordsPartial, newNSideFile);
        }
        // on initialize les valeurs à NaN
//        for (int i = 0; i < partialValues.length; i++) {
//            partialValues[i] = Double.NaN;
//        }
//        // on boucle sur les indices partiels
//        for (int i = 0; i < partialHpxPixIdx.length; i++) {
//            partialValues[(int)partialHpxPixIdx[i]] = values[i];
//        }
        // on boucle sur les indices partiels
        for (int i = 0; i < partialHpxPixIdx.length; i++) {
            partialValues.put((long)partialHpxPixIdx[i],values[i]);
        }

    }

    private double[] getValuesPartialRing(long low, long high) {
       int nbVal = (int)(high-low);
       double[] ret = new double[nbVal];
       try {
          for (int i=0; i<nbVal; i++) {
//             ret[i] = partialValues[(int)CDSHealpix.nest2ring(nSideFile, low + i)];
             Double a = (Double)partialValues.get( CDSHealpix.nest2ring(nSideFile, low + i));
             ret[i] = a==null ? Double.NaN : a.doubleValue();
          }
       } catch( Exception e ) { e.printStackTrace(); }
       return ret;
    }

    private double[] getValuesPartialNested(long low, long high) {
        int nbVal = (int)(high-low);
        double[] ret = new double[nbVal];
        for (int i=0; i<nbVal; i++) {
//            ret[i] = partialValues[(int)low+i];
           Double a = (Double)partialValues.get( low+i );
           ret[i] = a==null ? Double.NaN : a.doubleValue();
        }

        return ret;
    }


    private double[] ringValues; // tableau des valeurs des pixels RING, indexées en RING
    private void fillRingValues(int minIdx, int maxIdx, long idxTForm) {
        // lecture par morceaux de <step>, pour ne pas exploser la mémoire !
        int nbValues = maxIdx - minIdx + 1;
        ringValues = new double[nbValues];
        // TODO : s'assurer qu'il s'agisse bien d'un multiple du format !! (eg 1024)
//        System.out.println(lenHpx[(int)idxTForm]);
        int step = Math.max(nbPixGeneratedImage*nbPixGeneratedImage, lenHpx[(int)idxTForm]);
        int min = minIdx;
        int max = Math.min(min+step, maxIdx);
        while (min<maxIdx) {
            System.arraycopy(getValuesNested(min, max, rafHpx, initialOffsetHpx, idxTForm),
                             0, ringValues, min-minIdx, max-min);
            min += step;
            max = Math.min(min+step, maxIdx);
        }
    }

    private double[] fillRingBuffer(int minNested, int maxNested, int nside) {
        int length = maxNested - minNested;
        double[] val = new double[length];
        try {
           for (int i=0; i<length; i++) {
              val[i] = ringValues[(int)CDSHealpix.nest2ring(nside, minNested + i)];
           }
        } catch( Exception e ) {
           e.printStackTrace();
        }

        return val;
    }

    boolean buildDoubleHealpix(int nside_file, long npix_file, int nside,
            Fits out, long idxTForm, String dir) {
        return buildDoubleHealpix(nside_file, npix_file, nside, out, true, idxTForm, dir);
    }

    boolean buildDoubleHealpix(int nside_file, long npix_file, int nside,
                               Fits out, boolean write, long idxTForm, String dir) {
        boolean empty = true;
        long min, max;
        long index;

        // cherche les numéros de pixels Healpix dans ce losange
        min = getHealpixMin(nside_file, npix_file, nside, true);
        max = min+out.width*out.height;


        double[] values = getValues(min, max, rafHpx, initialOffsetHpx, idxTForm);
        double value;
        // cherche la valeur à affecter dans chacun des pixels healpix
        for( int y=0; y<out.height; y++ ) {
            for( int x=0; x<out.width; x++ ) {


                index = min + xy2hpx(y*out.width+x);
//                for( int i=0; i<overSamplingFactor; i++ ) {
//                    indexFather = getFather(indexFather);
//                }

                value = values[(int) (index-min)];
                out.setPixelDouble(x, y, value);

            }
        }

        // Passage en mode couleur si nécessaire
        if( isARGB ) out.setARGB();

        if (write) {
            try {
                String filePath = getFilePath(getCacheDir() + Util.FS + dir,
                        (int) log2(nside_file), npix_file)
                        + ".fits";

                out.writeFITS(filePath);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return !empty;
    }

    /** Retourne un tableau de pixels d'origine couvrant la vue courante */
//    protected byte [] getCurrentBufPixels(Plan pi,double rzoomX, double rzoomY, int viewWidth, int viewHeight, double zoom) {
//       lockGetPixelInfo=true;
//       int w = viewWidth;
//       int h = viewHeight;
//       byte [] pixelsOrigin = new byte[w*h*npix];
//       byte [] onePixelOrigin = new byte[npix];
//
//       double blank = curTFormBitpix>0 ? (double) Integer.MIN_VALUE : Double.NaN;
//
//       int offset=0;
//       double fct = 100./h;
//       for( int y=h-1; y>=0; y-- ) {
//          pi.pourcent+=fct;
//          for( int x=0; x<w; x++ ) {
//             if( !getOnePixelFromCache(pi.projd,onePixelOrigin,rzoomX + x/zoom,rzoomY + y/zoom) ) {
//                setPixVal(onePixelOrigin, curTFormBitpix, 0, blank);
//                if( !((PlanImage)pi).isBlank ) {
//                   ((PlanImage)pi).isBlank=true;
//                   ((PlanImage)pi).blank=blank;
//                   if( curTFormBitpix>0 ) ((PlanImage)pi).headerFits.setKeyValue("BLANK", blank+"");
//                }
//             }
//             System.arraycopy(onePixelOrigin, 0, pixelsOrigin, offset, npix);
//             offset+=npix;
//         }
//       }
//
//       lockGetPixelInfo=false;
//       return pixelsOrigin;
//    }

    /** Retourne le bitpix */
    protected int getBitpix() { return this.curTFormBitpix; }


    /** Retourne le répertoire du survey */
    public String getSurveyDir() {
       return getCacheDir() + Util.FS + dirName;
    }

    private RandomAccessFile rafHpx; // pour acces aleatoire aux pixels healpix
    private long initialOffsetHpx = Long.MIN_VALUE;

    public String getFilePath(int idxTForm, int order, long npix) {
        return
             getCacheDir() + Util.FS + getDirname() + Util.FS + dirNameForIdx(idxTForm) + Util.FS +
             "Norder" + order + Util.FS +
             "Dir" + ((npix / DIRSIZE)*DIRSIZE) + Util.FS +
             "Npix" + npix;
    }

    protected String getAllskyFilePath(int idxTForm,int order) {
        return
            getCacheDir() + Util.FS + getDirname() + Util.FS + dirNameForIdx(idxTForm)
            + Util.FS + "Norder" + order + Util.FS + "Allsky.fits";
    }

    private double getLevelImage(int nside, int nbPixImage) {
//        System.out.println("\n");
//        System.out.println(nside);
//        System.out.println(nbPixImage);
//        System.out.println("\n");
        int nbPix = 12*nside*nside;
        int nbNeededImages = nbPix/(nbPixImage*nbPixImage);
        if( nbNeededImages<1 ) return Double.NEGATIVE_INFINITY;
        double nsideImage = Math.sqrt(nbNeededImages/12);
        return Math.log(nsideImage)/Math.log(2);
    }

    protected boolean hasPolarisationData() {
        List<String> l = Arrays.asList(tfieldNames);
        return (l.contains("U-POLARISATION") || l.contains("U_POLARISATION"))
            && (l.contains("Q-POLARISATION") || l.contains("Q_POLARISATION"));
    }
    
    protected boolean isPartial() { return isPartial; }

    /**
     * Creation d'un nouveau plan avec infos de polarisation
     *
     * @param mode POLA_AMPLITUDE_MAGIC_CODE, POLA_ANGLE_MAGIC_CODE ou POLA_SEGMENT_MAGIC_CODE
     */
    protected void displayPolarisation(int mode) {

        // deplacement dans un folder si necessaire
        Plan folderPlane = null;
        synchronized (this) {
            // si pas encore dans un folder, on crée le folder qui va bien
            if (folder == 0) {
                String saveLabel = label;
                label = labelForField(idxTFormToRead);
                int idx = aladin.calque.newFolder(saveLabel, 0, false);
                if (idx >= 0) {
                    folderPlane = aladin.calque.plan[idx];
                    aladin.calque.permute(this, folderPlane);
                    aladin.view.newView(1);
                    aladin.calque.repaintAll();
                }
            } else {
                boolean found = false;
                for (int i = aladin.calque.plan.length - 1; i >= 0; i--) {
                    if (aladin.calque.plan[i] == this) {
                        found = true;
                        continue;
                    }
                    if (!found) {
                        continue;
                    }
                    if (aladin.calque.plan[i].type == Plan.FOLDER) {
                        folderPlane = aladin.calque.plan[i];
                        break;
                    }
                }
            }
        }

        MyInputStream in;
        try {
            if (isLocal)
                in = new MyInputStream(new FileInputStream(originalPath));
            else
                in = new MyInputStream(Util.openStream(originalPath));

            in = in.startRead();
            in.getType(); // il faut absolument faire cet appel, sinon
                          // MyInputStream.isGZ() renvoie toujours false !!
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String label = labelForField(mode);
        synchronized (this) {
            int idx = aladin.calque.newPlanHealpix(originalPath, in, label,
                    mode == POLA_SEGMENT_MAGIC_CODE ? PlanBG.DRAWPOLARISATION
                            : PlanBG.DRAWPIXEL, mode, false);
            Plan polaPlane = aladin.calque.plan[idx];

            // on met le nouveau plan dans le folder approprié
            if (folderPlane != null) {
                int newIdx = aladin.calque.getIndex(folderPlane);
                // on reste en dessous des plans polarisation
                if (aladin.calque.plan[newIdx+1].type==Plan.ALLSKYPOL) {
                    newIdx += 1;
                }
                aladin.calque.permute(polaPlane, aladin.calque.plan[newIdx]);
                aladin.view.newView(1);
                aladin.calque.repaintAll();
            }
            if (mode==POLA_ANGLE_MAGIC_CODE) {
                idx = aladin.calque.getIndex(polaPlane);
                aladin.execAsyncCommand("cm @" + idx + " polarisation");
            }
            else if (mode==POLA_AMPLITUDE_MAGIC_CODE) {
                idx = aladin.calque.getIndex(polaPlane);
                aladin.execAsyncCommand("cm @" + idx + " log");
            }
        }
    }


    static protected double[] getPolaAmpMinMax(Fits fitsQ, Fits fitsU, boolean dataExtrema) {
//        String minKw = dataExtrema ? "DATAMIN" : "PIXELMIN";
//        String maxKw = dataExtrema ? "DATAMAX" : "PIXELMAX";

        // prendre le PIXELMIN/PIXELMAX n'a pas beaucoup de sens pour l'amplitude, on prend tout le range systématiquement
        String minKw = "DATAMIN";
        String maxKw = "DATAMAX";
        double dataMinUAllsky = Double.parseDouble(fitsU.headerFits
                .getStringFromHeader(minKw));
        double dataMaxUAllsky = Double.parseDouble(fitsU.headerFits
                .getStringFromHeader(maxKw));

        double dataMinQAllsky = Double.parseDouble(fitsQ.headerFits
                .getStringFromHeader(minKw));
        double dataMaxQAllsky = Double.parseDouble(fitsQ.headerFits
                .getStringFromHeader(maxKw));

        double maxU = Math.max(Math.abs(dataMinUAllsky), Math
                .abs(dataMaxUAllsky));
        double maxQ = Math.max(Math.abs(dataMinQAllsky), Math
                .abs(dataMaxQAllsky));
//        double minU = Math.min(Math.abs(dataMinUAllsky), Math
//                .abs(dataMaxUAllsky));
//        double minQ = Math.min(Math.abs(dataMinQAllsky), Math
//                .abs(dataMaxQAllsky));

//        double minAmp = dataExtrema ? 0.0 : Math.sqrt(minU * minU + minQ * minQ);
        double minAmp = 0.;
        double maxAmp = Math.sqrt(maxU * maxU + maxQ * maxQ);

        return new double[] {minAmp, maxAmp};
    }



        private void generatePolarisationData() {
        synchronized (lock) {
            try {
                // are the polarisation data already generated ?
                File tmp = new File(getCacheDir() + Util.FS + getDirname()
                        + Util.FS + dirNameForIdx(idxTFormToRead) + Util.FS
                        + "Norder3" + Util.FS + "Allsky.fits");
                if (!(tmp.exists() && tmp.length() > 0)) {
                    // creation des donnees de polarisation
                    File polaDir = new File(getCacheDir() + Util.FS
                            + getDirname() + Util.FS
                            + dirNameForIdx(idxTFormToRead));
                    if (!polaDir.exists() && !polaDir.mkdir()) {
                        System.err.println("Can't create directory " + polaDir);
                        return;
                    }
                    // on cree les boules pour Q et U
                    List<String> l = Arrays.asList(tfieldNames);
                    int idxPolaU = l.indexOf("U-POLARISATION");
                    int idxPolaQ = l.indexOf("Q-POLARISATION");
                    // les donnees WMAP ont des noms de champ legerement
                    // differents
                    // :(
                    if (idxPolaU < 0) {
                        idxPolaU = l.indexOf("U_POLARISATION");
                    }
                    if (idxPolaQ < 0) {
                        idxPolaQ = l.indexOf("Q_POLARISATION");
                    }

                    if (idxPolaU < 0 || idxPolaQ < 0) {
                        System.err.println("Can't find polarisation indexes");
                        return;
                    }

                    String dirnamePolaU = getDirname() + Util.FS
                            + dirNameForIdx(idxPolaU);
                    String dirnamePolaQ = getDirname() + Util.FS
                            + dirNameForIdx(idxPolaQ);

                    if (needProcessing(dirnamePolaU, false)) {
                        // mode plus proche voisin pour les all sky de
                        // polarisation
                        generateHierarchy(idxPolaU, FIRST);
                        long size = Util.dirSize(new File(dirnamePolaU));
                        addInCache(size/1024);
                        writePropertiesFile(dirnamePolaU);
                    }
                    if (needProcessing(dirnamePolaQ, false)) {
                        generateHierarchy(idxPolaQ, FIRST);
                        long size = Util.dirSize(new File(dirnamePolaU));
                        addInCache(size/1024);
                        writePropertiesFile(dirnamePolaQ);
                    }
                    Fits fitsOut = null;

                    Fits fitsQ = new Fits();
                    Fits fitsU = new Fits();
                    double dataMinPola, dataMaxPola, pixelMinPola, pixelMaxPola;
                    dataMinPola = pixelMinPola = 0.0;
                    dataMaxPola = pixelMaxPola = 1.0;
                    // ***** Generation du fichier allsky pour la polarisation !
                    // ***** //
                    try {
                        fitsU.loadFITS(getAllskyFilePath(idxPolaU, 3));
                        fitsQ.loadFITS(getAllskyFilePath(idxPolaQ, 3));

                        if (idxTFormToRead == POLA_AMPLITUDE_MAGIC_CODE) {
                            double[] dataExtrema = getPolaAmpMinMax(fitsQ,
                                    fitsU, true);
                            dataMinPola = dataExtrema[0];
                            dataMaxPola = dataExtrema[1];
                            double[] pixelExtrema = getPolaAmpMinMax(fitsQ,
                                    fitsU, false);
                            pixelMinPola = pixelExtrema[0];
                            pixelMaxPola = pixelExtrema[1];
                        } else if (idxTFormToRead == POLA_ANGLE_MAGIC_CODE) {
                            dataMinPola = pixelMinPola = -90.0;
                            dataMaxPola = pixelMaxPola = 90.0;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        if (idxTFormToRead != POLA_SEGMENT_MAGIC_CODE) {
                            fitsOut = new Fits(fitsU.width, fitsU.height, -32);
                        }
                        computePolarisation(fitsOut, fitsQ, fitsU, 3, npix,
                                true, dataMinPola, dataMaxPola, pixelMinPola,
                                pixelMaxPola, idxTFormToRead);
                        fitsOut = null;
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }

                    // ///// boucle sur les norder et generation des fichiers
                    // individuels pour les losanges healpix /////
                    int deepestNorder = (int) log2(newNSideImage);

                    for (int norder = 3; norder <= deepestNorder; norder++) {
                        int nbPix = (int) (12 * Math
                                .pow(CDSHealpix.pow2(norder), 2));
                        for (int npix = 0; npix < nbPix; npix++) {
                            try {
                                fitsU.loadFITS(getFilePath(idxPolaU, norder,
                                        npix)
                                        + ".fits");
                                fitsQ.loadFITS(getFilePath(idxPolaQ, norder,
                                        npix)
                                        + ".fits");
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            }

                            try {
                                if (fitsOut == null
                                        && idxTFormToRead != POLA_SEGMENT_MAGIC_CODE) {
                                    fitsOut = new Fits(fitsU.width,
                                            fitsU.heightCell, -32);
                                }
                                computePolarisation(fitsOut, fitsQ, fitsU,
                                        norder, npix, false, dataMinPola,
                                        dataMaxPola, pixelMinPola,
                                        pixelMaxPola, idxTFormToRead);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                                continue;
                            }
                        }
                    }

                    // on fixe le bitpix à -32
                    if (idxTFormToRead!=POLA_SEGMENT_MAGIC_CODE) {
                        this.curTFormBitpix = -32;
                    }

                    // écriture fichier de properties dans repertoire de
                    // polarisation
                    writePropertiesFile(getDirname() + Util.FS
                            + dirNameForIdx(idxTFormToRead));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A partir de fichiers FITS de polarisation Q et U, génération des fichiers d'angle et de norme
     * Le fichier d'angle contient également la norme en extension
     *
     * @param fitsQ
     * @param fitsU
     * @param fitsAngle
     * @param fitsNorm
     * @param norder niveau healpix
     * @param npix indice losange healpix
     * @param allsky s'il s'agit d'un fichier allsky
     * @param dataMin
     * @param dataMax
     * @param pixelMin
     * @param pixelMax
     *
     * @param mode:
     */
    private void computePolarisation(Fits fitsOut, Fits fitsQ, Fits fitsU,
                                     int norder, int npix, boolean allsky,
                                     double dataMin, double dataMax, double pixelMin, double pixelMax, int mode)
                                    throws Exception {
        HeaderFits extHeader;
        int mybitpix = -32;
        int w, h;
        w = fitsQ.width;
        h = fitsQ.height;

        try {
            String header = "XTENSION=IMAGE\nBITPIX="+mybitpix
                +"\nNAXIS=2\nNAXIS1="+w
                +"\nNAXIS2="+h;
            if (mode!=POLA_SEGMENT_MAGIC_CODE) {
                header += "\nPIXELMIN="+pixelMin+"\nPIXELMAX="+pixelMax;
                header += "\nDATAMIN="+dataMin+"\nDATAMAX="+dataMax;
            }
            extHeader = new HeaderFits(header);
            if (allsky) {
                Aladin.trace(3, "Allsky header:\n"+header);
            }

        }
        catch(Exception e) {
            e.printStackTrace();
            return;
        }

        //////// mode DRAW_POLA_SEGMENT ////////
        if (mode==POLA_SEGMENT_MAGIC_CODE) {
            fitsQ.clearExtensions();
            fitsQ.addFitsExtension(extHeader, fitsU.pixels);
            String path = allsky ? getAllskyFilePath(POLA_SEGMENT_MAGIC_CODE, norder) : getFilePath(POLA_SEGMENT_MAGIC_CODE, norder, npix) + ".fits";
            fitsQ.writeFITS(path);
            return ;
        }

        /////// modes DRAW_POLA_AMP et DRAW_POLA_ANG ///////
        fitsOut.clearExtensions();
        fitsOut.headerFits = extHeader;
        double valU, valQ;
        double value;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                valU = fitsU.getPixelFull(x, y);
                valQ = fitsQ.getPixelFull(x, y);
                // on s'intéresse à la norme
                if (mode==POLA_AMPLITUDE_MAGIC_CODE) {
                    value = Math.sqrt(valU * valU + valQ * valQ);
                }
                // on s'intéresse à l'angle
                else {
                    value = Math.toDegrees(0.5 * Math.atan2(valU, valQ));
                }
//              angle = 0.5 * Math.atan(valU/valQ);
                fitsOut.setPixelDouble(x, y, value);
            }
        }
        String path = allsky ? getAllskyFilePath(mode, norder) : getFilePath(mode, norder, npix) + ".fits";
        fitsOut.writeFITS(path);
    }

    /**
     * Création d'un nouveau PlanHealpix pour l'indice tform passé en parametre
     * On met le tout dans un folder si necessaire
     * @param idxField
     */
    protected boolean loadNewField(int idxField) {
        if (idxField>=nField) {
            return false;
        }

        Plan folder = null;
        synchronized (this) {
            // si pas encore dans un folder, on crée le folder qui va bien
            if (this.folder == 0) {
                String saveLabel = this.label;
                this.label = labelForField(idxTFormToRead);
                int idx = aladin.calque.newFolder(saveLabel, 0, false);
                if (idx>=0) {
                    folder = aladin.calque.plan[idx];
                    aladin.calque.permute(this, folder);
                    aladin.view.newView(1);
                    aladin.calque.repaintAll();
                }
            }
            else {
                boolean found = false;
                for (int i = aladin.calque.plan.length-1; i>=0; i--) {
                    if (aladin.calque.plan[i]==this) {
                        found = true;
                        continue;
                    }
                    if (!found) {
                        continue;
                    }
                    if (aladin.calque.plan[i].type==Plan.FOLDER) {
                        folder = aladin.calque.plan[i];
                        break;
                    }
                }
            }
        }

        MyInputStream in;
        try {
            if (isLocal) in = new MyInputStream(new FileInputStream(originalPath));
            else in = new MyInputStream(Util.openStream(originalPath));

            in = in.startRead();
            in.getType(); // il faut absolument faire cet appel, sinon MyInputStream.isGZ() renvoie toujours false !!
        }
        catch(Exception e) {
            e.printStackTrace();
            return false;
        }

        int idx = aladin.calque.newPlanHealpix(originalPath, in,
                                     tfieldNames[idxField],
                                     PlanBG.DRAWPIXEL, idxField, true);
        if (idx<0) {
            return false;
        }
        // on met le nouveau plan dans le folder approprié
        if (folder!=null) {
            int newIdx = aladin.calque.getIndex(folder);
            // on reste en dessous des plans polarisation
            if (aladin.calque.plan[newIdx+1].type==Plan.ALLSKYPOL) {
                newIdx += 1;
            }
            aladin.calque.permute(aladin.calque.plan[idx], aladin.calque.plan[newIdx]);
            aladin.view.newView(1);
            aladin.calque.repaintAll();
        }

        return true;
    }

    private String labelForField(int idx) {
        if ( idx==POLA_SEGMENT_MAGIC_CODE ){
            return "Polarisation";
        }
        else if ( idx==POLA_AMPLITUDE_MAGIC_CODE ) {
            return "Amplitude";
        }
        else if ( idx==POLA_ANGLE_MAGIC_CODE ) {
            return "Angle";
        }

        return tfieldNames[idxTFormToRead];
    }

    // TODO : rien pour le moment
//    protected boolean Free() { return true;}

    /** Création des fichiers Allsky FITS 8 bits et JPEG pour tout un niveau Healpix
     * @param path Emplacement de la base
     * @param survey nom du survey
     * @param order order Healpix
     * @param outLosangeWidth largeur des losanges pour le Allsky (typiquement 64 ou 128 pixels)
     * @param mode FIRST, MAX, MEDIANE, MOYENNE, SIGMA
     */
    public void createAllSky(String path,String survey,int order,int outLosangeWidth, int mode) throws Exception {
       long t=System.currentTimeMillis();
       int nside = (int)CDSHealpix.pow2(order);
       int n = 12*nside*nside;
       int nbOutLosangeWidth = (int)Math.sqrt(n);
       int nbOutLosangeHeight = (int)((double)n/nbOutLosangeWidth);
       if( (double)n/nbOutLosangeWidth!=nbOutLosangeHeight ) nbOutLosangeHeight++;
       int outFileWidth = outLosangeWidth * nbOutLosangeWidth;
       Aladin.trace(3, "Création Allsky "+(isARGB?"ARB ":"")+"order="+order+": "+n+" losanges ("+nbOutLosangeWidth+"x"+nbOutLosangeHeight
             +" de "+outLosangeWidth+"x"+outLosangeWidth+" soit "+outFileWidth+"x"+nbOutLosangeHeight*outLosangeWidth+" pixels)...");

       Fits out=null;
//     Fits out = new Fits(outFileWidth,nbOutLosangeHeight*outLosangeWidth,8);

       double fct=15./n;
       for( int npix=0; npix<n; npix++ ) {
          String name = getFilePath(survey, order, npix);
          Fits in = new Fits();
          String filename = path+Util.FS+name;
// if( npix%100==0 ) System.out.print(npix+"...");
 pourcent+=fct;
          try {
             in.loadFITS(filename+".fits");
             if( out==null ) {
                out = new Fits(outFileWidth,nbOutLosangeHeight*outLosangeWidth,in.bitpix);
                if( hasBadData ) out.setBlank(badData);
             }

             int yLosange=npix/nbOutLosangeWidth;
             int xLosange=npix%nbOutLosangeWidth;
             int gap = in.width/outLosangeWidth;
             // PF, Rapide,efficace (et très moche)
             if( gap==0 )  { createAllSky(path,survey,order,in.width,mode); return; }
             int nombre=gap*gap;

             double liste [] = new double[nombre];
             for( int y=0; y<in.width/gap; y++ ) {
                for( int x=0; x<in.width/gap; x++ ) {
                    double total=0;
                    double carre=0;

                    int i=0;
                    double pix=0.0;
                    double max=Integer.MIN_VALUE;

                    for( int y1=0; y1<gap; y1++ ) {
                       for( int x1=0; x1<gap; x1++) {
                          int offsetY = isARGB ? y*gap+y1 : in.heightCell-1-(y*gap+y1);
                          pix = in.getPixelDouble(x*gap+x1,offsetY) ;

                          if( mode==FIRST ) break;
                          if( mode==MEDIANE ) { liste[i++]=pix; continue; }
                          if( mode==MAX ) { if( pix>max ) max=pix; continue; }
                          total+=pix;
                          carre+=pix*pix;
                       }
                    }

                    switch (mode) {
                        case FIRST:
                            // rien à faire, on a déja la valeur qui nous intéresse
                            break;
                        case MOYENNE:
                            pix = (total / nombre);
                            break;
                        case MEDIANE:
                            Arrays.sort(liste);
                            pix = liste[nombre / 2];
                            break;
                        case SIGMA:
                            pix = (Math.sqrt(carre / nombre
                                    - (total / nombre) * (total / nombre)));
                            break;
                        case MAX:
                            pix = max;
                            break;
                        default:
                            throw new Exception("mode " + mode
                                    + " non supporté !");
                    }



                   if( pix<dataMin ) dataMin=pix;
                   else if( pix>dataMax ) dataMax=pix;

                   int xOut= xLosange*outLosangeWidth + x;
                   int yOut = yLosange*outLosangeWidth +y;

                   out.setPixelDouble(xOut, out.heightCell-1-yOut, pix);
                }
             }

          } catch( Exception e ) {
             e.printStackTrace();
             return;
//             System.err.println("Erreur sur "+name +" ("+e.getMessage()+")");
          }
       }
       String filename = path+Util.FS+survey+Util.FS+"Norder"+order+Util.FS+"Allsky";

       // Passage en mode ARGB si nécessaire
       if( isARGB ) out.setARGB();

       // Sinon, FITS classique => détermination des valeurs des pixels
       else {
          // Détermination des pixCutmin..pixCutmax et min..max directement dans le fichier AllSky
          double range [] = out.findAutocutRange();

          out.headerFits.setKeyValue("PIXELMIN", range[0]+"");
          out.headerFits.setKeyValue("PIXELMAX", range[1]+"");
          out.headerFits.setKeyValue("DATAMIN",  range[2]+"");
          out.headerFits.setKeyValue("DATAMAX",  range[3]+"");

          Aladin.trace(3, "PIXELMINMAX = ["+range[0]+" "+range[1]+"] DATAMINMAX=["+range[2]+" "+range[3]+"]");
       }
       out.writeFITS(filename+".fits");

       Aladin.trace(3, "\nConstruction "+survey+Util.FS+"Norder"+order+Util.FS+"Allsky en "+
             (int)((System.currentTimeMillis()-t)/1000)+"s");
    }

    private int coordsysToFrame(char sys) {
        switch (sys) {
        case 'C':
            return Localisation.ICRS;

        case 'G':
            return Localisation.GAL;

        case 'E':
            return Localisation.ECLIPTIC;

        default:
            return Localisation.GAL;
        }
    }

    /** Dessin du ciel complet en rapide */
    protected boolean drawAllSky(Graphics g,ViewSimple v) {
       localAllSky=true;
       return super.drawAllSky(g,v);
    }

    static final int FIRST   = 0;
    static final int MOYENNE = 1;
    static final int MEDIANE = 2;
    static final int SIGMA   = 3;
    static final int MAX     = 4;

    /** Création d'un losange par concaténation de ses 4 fils
     * et suppression des fichiers 8bits FITS des fils en question
     * puisque désormais inutiles.
     * @param file Nom du fichier complet
     * @param aladinTree Path de la base
     * @param sky  Nom de la base
     * @param order Ordre Healpix du losange
     * @param npix Numéro Healpix du losange
     * @param fils les 4 fils du losange
     * @param mode méthode de cumul des pixels (FIRST, MOYENNE, MEDIANE...)
     */
    Fits createNodeHpx(String file,int order,long npix,Fits fils[],
          int mode) throws Exception {
       int w = nbPixGeneratedImage;
       Fits out = new Fits(w,w,16);
       Fits in;
       for( int dg=0; dg<2; dg++ ) {
          for( int hb=0; hb<2; hb++ ) {
             int quad = dg<<1 | hb;
             in = fils[quad];
             int offX = (dg*w)>>>1;
             int offY = ((1-hb)*w)>>>1;
             int pix8,p1,p2=0,p3=0,p4=0;
             for( int y=0; y<w; y+=2 ) {
                for( int x=0; x<w; x+=2 ) {
                   p1= in.getPixelInt(x,y);
                   if( mode!=FIRST ) {
                      p2 = in.getPixelInt(x+1,y);
                      p3 = in.getPixelInt(x,y+1);
                      p4 = in.getPixelInt(x+1,y+1);
                   }
                   switch( mode ) {
                      case FIRST:   pix8 = p1; break;
                      case MOYENNE: pix8 = (p1+p2+p3+p4)/4; break;
                      case MAX :    pix8 = Math.max(Math.max(p1,p2),Math.max(p2,p3)); break;
                      case SIGMA:   pix8 = (int)( Math.sqrt( (p1*p1+p2*p2+p3*p3+p4*p4)/4 - Math.pow((p1+p2+p3+p4)/4.,2.) ))*2; break;
                      case MEDIANE:
                         if( p1>p2 && (p1<p3 || p1<p4) || p1<p2 && (p1>p3 || p1>p4) ) pix8=p1;
                         else if( p2>p1 && (p2<p3 || p2<p4) || p2<p1 && (p2>p3 || p2>p4) ) pix8=p2;
                         else if( p3>p1 && (p3<p2 || p3<p4) || p3<p1 && (p3>p2 || p3>p4) ) pix8=p3;
                         else pix8=p4;
                         break;
                      default: throw new Exception("mode non supporté ("+mode+")");
                   }
                   out.setPixelInt(offX+(x>>>1), offY+(y>>>1), pix8);
                }
             }
          }
       }
       out.writeFITS(file);

       return out;
    }

    protected char getCoordsys() {
        return coordsys;
    }

    protected int getNSideFile() {
        return newNSideFile;
    }

}
