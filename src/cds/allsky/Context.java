// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
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

import java.awt.Polygon;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JProgressBar;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.Localisation;
import cds.aladin.MyInputStream;
import cds.aladin.MyProperties;
import cds.aladin.Tok;
import cds.astro.Astrocoo;
import cds.astro.Astroframe;
import cds.astro.Galactic;
import cds.astro.ICRS;
import cds.fits.CacheFits;
import cds.fits.Fits;
import cds.fits.HeaderFits;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.tools.Util;
import cds.tools.hpxwcs.Tile2HPX;
import cds.tools.hpxwcs.Tile2HPX.WCSFrame;
import cds.tools.pixtools.CDSHealpix;

/**
 * Classe pour unifier les accès aux paramètres nécessaires pour les calculs
 * + accès aux méthodes d'affichages qui sont redirigées selon l'interface
 * existante
 * @author Oberto + Fernique
 *
 */
public class Context {
   
   static final public String FORCOMPATIBILITY = "#____FOR_COMPATIBILITY_WITH_OLD_HIPS_CLIENTS____";
   
   private static boolean verbose=false;
   protected String hipsId=null;             // Identificateur du HiPS (publisher_did, sinon publisher_id/obs_id sans le préfixe ivo://)
   protected String label;                   // Nom du survey

   protected String inputPath;               // Répertoire des images origales ou path de l'image originale (unique)
   protected String outputPath;              // Répertoire de la boule HEALPix à générer
   protected String hpxFinderPath;           // Répertoire de l'index Healpix (null si défaut => dans outputPath/HpxFinder)
   protected String imgEtalon;               // Nom (complet) de l'image qui va servir d'étalon
   protected HeaderFits header=null;         // Entête FITS associée
   protected boolean isInputFile=false;      // true si le paramètre input concerne un fichier unique
   public int depth=1;                    // profondeur du cube (1 pour une image)
   protected boolean depthInit=false;        // true si la profondeur du cube a été positionné
   public double crpix3=0,crval3=0,cdelt3=0;    // paramètre pour un cube
   public String bunit3=null;
   protected int [] hdu = null;              // Liste des HDU à prendre en compte
   public int bitpixOrig = -1;               // BITPIX des images originales
   protected double blankOrig= Double.NaN;   // Valeur du BLANK en entrée
   protected boolean hasAlternateBlank=false;// true si on a indiqué une valeur BLANK alternative
   public double bZeroOrig=0;                // Valeur BZERO d'origine
   public double bScaleOrig=1;               // Valeur BSCALE d'origine
   protected boolean bscaleBzeroOrigSet=false; // true si on a positionné
   protected boolean flagNoInitEtalon=false;      // true => bloque la maj du cutOrig par estimation automatique
   protected double[] cutOrig;               // Valeurs cutmin,cutmax, datamin,datamax des images originales (valeurs raw)
   protected double[] pixelRangeCut;         // range et cut passé sur la ligne de commande (valeurs physiques)
   public double[] pixelGood=null;           // Plage des valeurs des pixels conservés (valeurs physiques)
   public double[] good=null;                // Plage des valeurs de pixels conservés (raw)
   public int[] borderSize = {0,0,0,0};      // Bords à couper sur les images originales
   public Polygon polygon = null;            // Polygone global des pixels observés
   public boolean scanFov=false;             // true s'il faut rechercher des fichiers xxxx.fov
   protected int circle = 0;                 // Rayon du cercle à garder, <=0 pour tout
   public int dataArea = Constante.SHAPE_UNKNOWN; // Type d'observable (totalité, en ellipse ou en rectangle)
   public double maxRatio = Constante.PIXELMAXRATIO; // Rapport max tolérable entre hauteur et largeur d'une image source
   protected boolean fading=false;           // Activation du fading entre les images originales
   protected boolean mixing=true;            // Activation du mélange des pixels des images originales
   protected boolean fake=false;             // Activation du mode "just-print norun"
   protected boolean flagRecomputePartitioning=true; // true si hipsgen doit déterminer automatiquement la taille du partitionnement
   protected boolean partitioning=true;      // Activation de la lecture par blocs des fimages originales
   public String skyvalName;                 // Nom du champ à utiliser dans le header pour soustraire un valeur de fond (via le cacheFits)
   public double pourcentMin=-1;             // Pourcentage de l'info à garder en début d'histog. si autocut (ex: 0.003), -1 = défaut
   public double pourcentMax=-1;             // Pourcentage de l'info à garder en fin d'histog. si autocut (ex: 0.9995), -1 = défaut
   public String expTimeName;                // Nom du champ à utiliser dans le header pour diviser par une valeur (via le cacheFits)
   protected double coef;                    // Coefficient permettant le calcul dans le BITPIX final => voir initParameters()
   protected ArrayList<String> fitsKeys=null;// Liste des mots clés dont la valeur devra être mémorisée dans les fichiers d'index JSON
   protected int typicalImgWidth=-1;         // Taille typique d'une image d'origine
   protected int mirrorDelay=0;              // délais entre deux récupérartion de fichier lors d'un MIRROR (0 = sans délai)
   protected boolean notouch=false;          // true si on ne doit pas modifier la date du hips_release_date
   
   protected int bitpix = -1;                // BITPIX de sortie
   protected double blank = Double.NaN;      // Valeur du BLANK en sortie
   protected double bzero=0;                 // Valeur BZERO de la boule Healpix à générer
   protected double bscale=1;                // Valeur BSCALE de la boule HEALPix à générer
   //   protected boolean bscaleBzeroSet=false;   // true si le bScale/bZero de sortie a été positionnés
   protected double[] cut;   // Valeurs cutmin,cutmax, datamin,datamax pour la boule Healpix à générer
   protected TransfertFct fct = TransfertFct.LINEAR; // Fonction de transfert des pixels fits -> jpg
   private JpegMethod jpegMethod = JpegMethod.MEDIAN;
   protected Mode mode=Mode.getDefault();   // Methode de traitement par défaut
   protected int maxNbThread=-1;             // Nombre de threads de calcul max imposé par l'utilisateur
   protected String creator=null;          // Le nom de la personne qui a fait le HiPS
   protected String status=null;             // status du HiPs (private|public clonable|unclonable|clonableOnce)
   protected String redInfo;                 // Information de colormap lors de la génération d'un HIPS RGB (composante red)
   protected String greenInfo;               // Information de colormap lors de la génération d'un HIPS RGB (composante green)
   protected String blueInfo;                // Information de colormap lors de la génération d'un HIPS RGB (composante blue)
   protected boolean gaussFilter=false;      // Filtrage gaussien lors de la génération d'un HiPS RGB (pour améliorer le rendu du fond)
   protected int nbPilot=-1;                 // Indique le nombre d'images à prendre en compte (pour faire un test pilot)

   protected int order = -1;                 // Ordre maximal de la boule HEALPix à générer
   public int minOrder= -1;                  // Ordre minimal de la boule HEALPix à générer (valide uniquement pour les HiPS HpxFinder)
   private int frame =-1;                    // Système de coordonnée de la boule HEALPIX à générée
   protected HealpixMoc mocArea = null;      // Zone du ciel à traiter (décrite par un MOC)
   protected HealpixMoc mocIndex = null;     // Zone du ciel correspondant à l'index Healpix
   protected HealpixMoc moc = null;          // Intersection du mocArea et du mocIndex => regénérée par setParameters()
   protected int mocOrder=-1;                // order du MOC des tuiles
   protected int nside=1024;                 // NSIDE pour la génération d'une MAP healpix
   protected int tileOrder=-1;               // Valeur particulière d'un ordre pour les tuiles
   protected CacheFits cacheFits;            // Cache FITS pour optimiser les accès disques à la lecture
   protected Vector<String> keyAddProp=null; // Clés des propriétés additionnelles à mémoriser dans le fichier properties
   protected Vector<String> valueAddProp=null;// Valeurs des propriétés additionnelles à mémoriser dans le fichier properties
   protected String target=null;             // ra,de en deg du "centre" du HiPS s'il est indiqué
   protected String targetRadius=null;       // radius en deg de la taille du premier champ HiPS à afficher
   protected String resolution=null;         // resolution en arcsec du pixel des images originales
   protected String scriptCommand;           // Mémorisation de la commande  script
   protected int targetColorMode = Constante.TILE_PNG;       // Mode de compression des tuiles couleurs

   protected ArrayList<String> tileTypes=null;          // Liste des formats de tuiles à copier (mirror) séparés par un espace
   protected boolean testClonable=true;
   protected boolean live=false;             // true si on doit garder les tuiles de poids

   public Context() {}

   public void reset() {
      mocArea=mocIndex=moc=null;
      mode=Mode.getDefault();
      hasAlternateBlank=false;
      bscaleBzeroOrigSet=false;
      imgEtalon=hpxFinderPath=inputPath=outputPath=null;
      lastNorder3=-2;
      live=validateOutputDone=validateInputDone=validateCutDone=validateRegion=false;
      isMap=false;
      prop=null;
      pixelGood=null;
      good=null;
      pixelRangeCut=null;
      depth=1;
      depthInit=false;
      crpix3=crval3=cdelt3=0;
      bunit3=null;
      tileTypes = null;
      outputRGB = null;
      redInfo=blueInfo=greenInfo=null;
      gaussFilter = false;
      plansRGB = new String [3];
      cmsRGB = new String [3];
      flagNoInitEtalon=false;
      tile2Hpx=null;
   }

   // manipulation des chaines désignant le système de coordonnées (syntaxe longue et courte)
   static public String getFrameName(int frame) { return frame==Localisation.GAL ? "galactic"
         : frame==Localisation.ECLIPTIC ? "ecliptic" : frame==-1 ? "?" : "equatorial"; }
   static public String getFrameCode(int frame ) { return frame==Localisation.GAL ? "G"
         : frame==Localisation.ECLIPTIC ? "E" : frame==-1 ? "?" : "C"; }
   static public int getFrameVal( String frame) {
      return (frame.equals("G")||frame.startsWith("gal"))? Localisation.GAL:
         frame.equals("E") || frame.startsWith("ecl") ? Localisation.ECLIPTIC : Localisation.ICRS;
   }
   static public String getCanonicalFrameName( String s) { return getFrameName( getFrameVal(s)); }

   // Getters
   public String getLabel() {
      if( label==null ) return getLabelFromHipsId();
      return label;

   }
   public String getHipsId() { return hipsId; }
   public boolean getFading() { return fading; }
   public int[] getBorderSize() { return dataArea==Constante.SHAPE_UNKNOWN ?  borderSize : new int[]{0,0,0,0}; }
   public double getMaxRatio() { return maxRatio; }
   public int getOrder() { return order; }
   public boolean hasFrame() { return frame>=0; }
   public int getFrame() { return hasFrame() ? frame : Localisation.ICRS; }
   public String getFrameName() { return getFrameName( getFrame() ); }
   public String getFrameCode() { return getFrameCode( getFrame() ); }
   public CacheFits getCache() { return cacheFits; }
   public String getInputPath() { return inputPath; }
   public String getOutputPath() { return outputPath; }
   public String getHpxFinderPath() { return hpxFinderPath!=null ? hpxFinderPath : Util.concatDir( getOutputPath(),Constante.FILE_HPXFINDER); }
   public String getImgEtalon() { return imgEtalon; }
   public int getBitpixOrig() { return bitpixOrig; }
   public int getBitpix() { return isColor() ? bitpixOrig : bitpix; }
   public int getNpix() { return isColor() || bitpix==-1 ? 4 : Math.abs(bitpix)/8; }  // Nombre d'octets par pixel
   public int getNpixOrig() { return isColor() || bitpixOrig==-1 ? 4 : Math.abs(bitpixOrig)/8; }  // Nombre d'octets par pixel
   public double getBScaleOrig() { return bScaleOrig; }
   public double getBZeroOrig() { return bZeroOrig; }
   public double getBZero() { return bzero; }
   public double getBScale() { return bscale; }
   public double getBlank() { return blank; }
   public double getBlankOrig() { return blankOrig; }
   public boolean hasAlternateBlank() { return hasAlternateBlank; }
   public HealpixMoc getArea() { return mocArea; }
   public Mode getMode() { return mode; } //isColor() ? CoAddMode.REPLACETILE : coAdd; }
   public double[] getCut() throws Exception { return cut; }
   public double[] getCutOrig() throws Exception { return cutOrig; }
   public String getSkyval() { return skyvalName; }
   public boolean isColor() { return bitpixOrig==0; }
   public boolean isCube() { return depth>1; }
   public boolean isCubeCanal() { return crpix3!=0 || crval3!=0 || cdelt3!=0; }
   //   public boolean isBScaleBZeroSet() { return bscaleBzeroSet; }
   public boolean isInMocTree(int order,long npix)  { return moc==null || moc.isIntersecting(order,npix); }
   public boolean isInMoc(int order,long npix) { return moc==null || moc.isIntersecting(order,npix); }
   public boolean isMocDescendant(int order,long npix) { return moc==null || moc.isDescendant(order,npix); }
   public int getMaxNbThread() { return maxNbThread; }
   public int getMocOrder() { return mocOrder; }
   public long getMapNside() { return nside; }
   public int getMinOrder() { return minOrder; }
   public int getTileOrder() { return tileOrder==-1 ? Constante.ORDER : tileOrder; }
   public int getTileSide() { return (int) CDSHealpix.pow2( getTileOrder() ); }
   public int getDepth() { return depth; }

   // Setters
   public void setLive(boolean flag) { live=flag; }
   public void setFlagInputFile(boolean flag) { isInputFile=flag; }
   public void setHeader(HeaderFits h) { header=h; }
   public void setCreator(String s) { creator=s; }
   public void setStatus(String s ) { status=s; }
   public void setHipsId(String s) { hipsId= canonHipsId(s); }
   public void setLabel(String s)     { label=s; }
   public void setMaxNbThread(int max) { maxNbThread = max; }
   public void setFading(boolean fading) { this.fading = fading; }
   public void setFading(String s) { fading = s.equalsIgnoreCase("false") ? false : true; }
   public void setMixing(String s) { mixing = s.equalsIgnoreCase("false") ? false : true; }
   public void setPartitioning(String s) {
      try {
         int val = Integer.parseInt(s);
         Constante.ORIGCELLWIDTH = val;
         partitioning = true;
         flagRecomputePartitioning = false;
      } catch( Exception e ) {
         partitioning = s.equalsIgnoreCase("false") ? false : true;
      }
   }
   public void setCircle(String r) throws Exception { this.circle = Integer.parseInt(r); }
   public void setMaxRatio(String r) throws Exception { maxRatio = Double.parseDouble(r); }
   public void setBorderSize(String borderSize) throws ParseException { this.borderSize = parseBorderSize(borderSize); }
   public void setBorderSize(int[] borderSize) { this.borderSize = borderSize; }
   public void setOrder(int order) { this.order = order; }
   public void setMinOrder(int minOrder) { this.minOrder = minOrder; }
   public void setMocOrder(int mocOrder) { this.mocOrder = mocOrder; }
   public void setMapNside(int nside) { this.nside = nside; }
   public void setTileOrder(int tileOrder) { this.tileOrder = tileOrder; }
   public void setFrame(int frame) { this.frame=frame; }
   public void setFrameName(String frame) { this.frame=getFrameVal(frame); }
   public void setFilter(String s) throws Exception {
      if( s.equalsIgnoreCase("gauss") )  gaussFilter=true;
      else throw new Exception("Unknown filter ["+s+"] (=> only \"gauss\" are presently supported)");
   }
   public void setSkyValName(String s ) {
      skyvalName=s;
      if( s==null ) return;
      if(s.equalsIgnoreCase("true") || s.equalsIgnoreCase("auto") ) {
         skyvalName="auto";
         info("Skyval automatical adjustement activated...");
      }
      else info("Skyval adjustement based on the FITS keyword ["+s+"]");
   }
   public int [] getHDU() { return hdu; }
   public void setHDU(String s) throws Exception { hdu = parseHDU(s); }

   public void setShape(String s) throws Exception {
      if( Util.indexOfIgnoreCase(s,"circle")>=0      || Util.indexOfIgnoreCase(s,"ellipse")>=0 ) {
         dataArea=Constante.SHAPE_ELLIPSE;
         info("Ellipse shape data area autodetection");
      }
      else if( Util.indexOfIgnoreCase(s,"square")>=0 || Util.indexOfIgnoreCase(s,"rectangular")>=0 ) {
         dataArea=Constante.SHAPE_RECTANGULAR;
         info("Rectangular shape data area autodetection");
      }
      else {
         dataArea=Constante.SHAPE_UNKNOWN;
         throw new Exception("Unknown observation shape ["+s+"] (=> circle, ellipse, square, rectangular)");
      }
   }
   
   private String addendum_id=null;
   public void setAddendum(String addId) { addendum_id=addId; }
   public void addAddendum(String addId) throws Exception {
      if( addId.equals(hipsId) ) throw new Exception("Addendum_id identical to the original HiPS ID ["+hipsId+"]");
      if( addendum_id==null ) addendum_id=addId;
      else {
         Tok tok = new Tok(addendum_id,"\t");
         while( tok.hasMoreTokens() ) { if( tok.nextToken().equals(addId) ) throw new Exception("Addendum_id already used  ["+addendum_id+"]"); }
         addendum_id += "\t"+addId;
      }
   }

   public String getRgbOutput() { return getOutputPath(); }
   public JpegMethod getRgbMethod() { return getJpegMethod(); }
   public int getRgbFormat() { return targetColorMode; }

   public void setPolygon(String r) throws Exception {
      scanFov = r.equalsIgnoreCase("true") || (polygon = createPolygon(r))!=null;
      if( scanFov ) info("FoV files associated to the original images");
   }
   
   // retourne l'identificateur du HiPS à partir des propriétés passées en paramètre
   public String getIdFromProp(MyProperties prop) {
      String s = prop.getProperty(Constante.KEY_CREATOR_DID);
      if( s!=null ) return s;
      s = prop.getProperty(Constante.KEY_PUBLISHER_DID);
      if( s!=null ) return s;
      s = prop.getProperty(Constante.KEY_OBS_ID);
      if( s==null ) return null;
      String creator = prop.getProperty(Constante.KEY_CREATOR_ID);
      if( creator==null ) creator = prop.getProperty(Constante.KEY_PUBLISHER_ID);
      if( creator==null ) creator="ivo://UNK.AUT";
      return creator+"?"+s;
   }


   /** Vérifie l'ID passé en paramètre, et s'il n'est pas bon le met en forme
    * @param s ID proposée, null si génération automatique
    * @param withException true si on veut avoir une exception en cas d'erreur
    * @return l'ID canonique
    */
   public String canonHipsId(String s) {
      try {
         s=checkHipsId(s,false);
      } catch( Exception e ) { }
      return s;
   }
   public String checkHipsId(String s ) throws Exception { return checkHipsId(s,true); }
   private String checkHipsId(String s,boolean withException) throws Exception {

      String auth,id;
      boolean flagQuestion=false;  // true si l'identificateur utilise un ? après l'authority ID, et non un /
      
      if( s==null && prop!=null )  s = getIdFromProp(prop);
      
      if( s==null || s.trim().length()==0 ) {
         verbose=false;
         s=getLabel()!=null?getLabel():"";
         if( withException ) throw new Exception("Missing ID (ex: id=CDS/P/DSS2/color)");
      }

      if( s.startsWith("ivo://")) s=s.substring(6);

      // Check de l'authority
      int offset = s.indexOf('/');
      int offset1 = s.indexOf('?');
      if( offset>=0 || offset1>=0) {
         if( offset==-1 ) offset=s.length();
         if( offset1==-1 ) offset1=s.length();
         if( offset1<offset ) { offset=offset1; flagQuestion=true; }
         else flagQuestion=false;
      }
      
      if( offset==-1) {
         auth="UNK.AUTH";
//         if( verbose ) warning("Id error => missing authority => assuming "+auth);
         if( withException ) throw new Exception("ID error => missing authority (ex: id=CDS/P/DSS2/color)");

      } else {
         auth = s.substring(0,offset);
         s=s.substring(offset+1);
         if( auth.length()<3) {
            while( auth.length()<3) auth=auth+"_";
//            if( verbose ) warning("Creator ID error => at least 3 characters are required => assuming "+auth);
            if( withException ) throw new Exception("ID error => at least 3 authority characters are required (ex: id=CDS/P/DSS2/color)");
         }
         StringBuilder a = new StringBuilder();
         boolean bug=false;
         for( char c : auth.toCharArray()) {
            if( !Character.isLetterOrDigit(c) && c!='.' && c!='-' ) { c='.'; bug=true; }
            a.append(c);
         }
         if( bug ) {
            auth=a.toString();
//            if( verbose ) warning("Creator ID error => some characters are not allowed => assuming "+auth);
            if( withException ) throw new Exception("ID error => some characters are not allowed (ex: id=CDS/P/DSS2/color)");
         }
      }

      // Check de l'identifier
      id=s.trim();
      if( id.startsWith("P/") || id.startsWith("C/")) id=id.substring(2);

      if( id.length()==0) {
         id="ID"+(System.currentTimeMillis()/1000);
//         if( verbose ) warning("Id error => missing ID => assuming "+id);
         if( withException ) throw new Exception("ID error: suffix Id missing (ex: id=CDS/P/DSS2/color)");
      } else {
         StringBuilder a = new StringBuilder();
         boolean bug=false;
         for( char c : id.toCharArray()) {
            if( Character.isSpaceChar(c) ) { c='-'; bug=true; }
            a.append(c);
         }
         if( bug ) {
            id=a.toString();
//            if( verbose ) warning("Id identifier error => some characters are not allowed => assuming "+id);
            if( withException ) throw new Exception("ID suffix error: some characters are not allowed (ex: id=CDS/P/DSS2/color)");
         }
      }

      String mode = isCube() ? "C": "P";

      return "ivo://"+auth+(flagQuestion?"?":"/")+mode+"/"+id;
   }

   /** retourne un label issu de l'ID du HiPS */
   public String getLabelFromHipsId() {
      if( hipsId==null ) return null;
      String s = hipsId;
      if( s!=null && s.startsWith("ivo://") ) s=s.substring(6);
      int offset = s.indexOf('/');
      int offset1 = s.indexOf('?');
      if( offset==-1 && offset1==-1 ) return null;
      if( offset==-1 ) offset=s.length();
      if( offset1==-1 ) offset1=s.length();
      offset = Math.min(offset1,offset);
      String s1 = s.substring(offset+1);
      if( s1.startsWith("P/") ) s1=s1.substring(2);
      s1=s1.replace('/',' ');
      return s1;
   }

   static public Polygon createPolygon(String r) throws Exception {
      Polygon p = new Polygon();
      Tok tok = new Tok(r," ,;\t");
      while( tok.hasMoreTokens()) {
         int x = (int)( Double.parseDouble(tok.nextToken()) +0.5);
         int y = (int)( Double.parseDouble(tok.nextToken()) +0.5);
         p.addPoint(x, y);
      }
      return p;
   }

   /** Indication des types de tuiles à copier lors d'une action MIRROR */
   protected void setTileTypes(String s) {
      Tok tok = new Tok(s);
      while( tok.hasMoreTokens() ) addTileType(tok.nextToken());
   }

   /** Mémorisation d'une extension pour le mirroring HiPS (MIRROR).
    * Ajoute le '.' en préfixe, sauf si l'extension est vide */
   protected void addTileType(String s) {
      if( s.equalsIgnoreCase("jpeg") ) s="jpg";
      if( tileTypes==null ) tileTypes = new ArrayList<String>();
      tileTypes.add( s.length()==0 ? s : "."+s.toLowerCase() );
   }

   /** Retourne la liste des formats de tuiles mirrorées */
   protected String getTileTypes() {
      if( tileTypes==null ) return null;
      StringBuilder format = new StringBuilder();
      for( String s :tileTypes) {
         if( s.length()==0 ) continue;
         if( format.length()>0 ) format.append(' ');
         if( s.equals(".jpg")) format.append("jpeg");
         else format.append(s.substring(1));
      }
      return format.toString();
   }


   // Construit le tableau des HDU à partir d'une syntaxe "1,3,4-7" ou "all"
   // dans le cas de all, retourne un tableau ne contenant que -1
   static public int [] parseHDU(String s) throws Exception {
      int [] hdu = null;
      if( s.length()==0 || s.equals("0") ) return hdu;
      if( s.equalsIgnoreCase("all") ) return new int[]{-1}; // Toutes les extensions images
      StringTokenizer st = new StringTokenizer(s," ,;-",true);
      ArrayList<Integer> a = new ArrayList<Integer>();
      boolean flagRange=false;
      int previousN=-1;
      while( st.hasMoreTokens() ) {
         String s1=st.nextToken();
         if( s1.equals("-") ) { flagRange=true; continue; }
         else if( !Character.isDigit( s1.charAt(0) ) ) continue;
         int n = Integer.parseInt(s1);
         if( flagRange ) {
            for( int i=previousN+1; i<=n && i<1000; i++ ) a.add(i);
            flagRange=false;
         }  else a.add(n);
         previousN = n;
      }
      hdu = new int[a.size()];
      for( int i=0; i<hdu.length; i++ ) hdu[i]=a.get(i);
      return hdu;
   }

   public void setInputPath(String path)  { this.inputPath = path;  }
   public void setOutputPath(String path) { this.outputPath = path; }
   public void setImgEtalon(String filename) throws Exception { imgEtalon = filename; initFromImgEtalon(); }
   public void setIndexFitskey(String list) {
      StringTokenizer st = new StringTokenizer(list);
      fitsKeys = new ArrayList<String>(st.countTokens());
      while( st.hasMoreTokens() ) fitsKeys.add(st.nextToken());
   }
   public void setMode(Mode coAdd) { this.mode = coAdd; }
   public void setTarget(String target) { this.target = target; }
   public void setTargetRadius(String targetRadius) { this.targetRadius = targetRadius; }
   public void setBScaleOrig(double x) { bScaleOrig = x; bscaleBzeroOrigSet=true; }
   public void setBZeroOrig(double x) { bZeroOrig = x; bscaleBzeroOrigSet=true; }
   //   public void setBScale(double x) { bscale = x; bscaleBzeroSet=true; }
   //   public void setBZero(double x) { bzero = x; bscaleBzeroSet=true; }
   public void setBitpixOrig(int bitpixO) {
      this.bitpixOrig = bitpixO;
      if (this.bitpix==-1) this.bitpix = bitpixO;
   }
   public void setBitpix(int bitpix) { this.bitpix = bitpix; }
   public void setBlankOrig(double x) {  
      blankOrig = x; hasAlternateBlank=true; }
   public void setColor(String colorMode) {
      if( colorMode.equalsIgnoreCase("false")) return;
      bitpixOrig=0;
      if( colorMode.equalsIgnoreCase("png")) targetColorMode=Constante.TILE_PNG;
      else targetColorMode=Constante.TILE_JPEG;
   }
   public void setCut(double [] cut) { this.cut=cut; }
   public void setPixelCut(String scut) throws Exception {
      StringTokenizer st = new StringTokenizer(scut," ");
      int i=0;
      if( pixelRangeCut==null ) pixelRangeCut = new double[]{Double.NaN,Double.NaN,Double.NaN,Double.NaN};
      while( st.hasMoreTokens() ) {
         String s = st.nextToken();
         try {
            pixelRangeCut[i]=Double.parseDouble(s);
            i++;
         } catch( Exception e) {
            setTransfertFct(s);
         }

      }
      if( i==1 || i>2 ) throw new Exception("pixelCut parameter error");
   }
   public void setPilot(int nbPilot) { this.nbPilot=nbPilot; }
   
   public void setPixelGood(String sGood) throws Exception {
      StringTokenizer st = new StringTokenizer(sGood," ");
      if( pixelGood==null ) pixelGood = new double[]{Double.NaN,Double.NaN};
      try {
         pixelGood[0] = Double.parseDouble(st.nextToken());
         if( st.hasMoreTokens() ) pixelGood[1] = Double.parseDouble(st.nextToken());
         else pixelGood[1] = pixelGood[0];
      } catch( Exception e ) { throw new Exception("pixelGood parameter error"); }
   }
   
   /** positionnement des pourcentages pour le cut de l'histogramme, soit
    * sous la forme d'une seule valeur (pourcentage centrale retenue => ex:99)
    * soit sous la forme de deux valeurs (pourcentage min et pourcentage max
    * ex => 0.3 et 99.7
    * @param sHist
    * @throws Exception
    */
   public void setHistoPercent(String sHist) throws Exception {
      StringTokenizer st = new StringTokenizer(sHist," ");
      int n = st.countTokens();
      
      try {
         if( n>2 ) throw new Exception();
         pourcentMin = Double.parseDouble(st.nextToken())/100.;
         
         // Une seule valeur => représente le pourcentage central retenue
         // ex: 99 => pourcentMin=0.005 et pourcentMax=0.995
         if( n==1 ) {
            pourcentMin = (1-pourcentMin)/2;
            pourcentMax = 1-pourcentMin;
            
         // Deux valeurs => représente le pourcentMin et pourcentMax directement
         } else {
            pourcentMax = Double.parseDouble(st.nextToken())/100;
         }
      } catch( Exception e ) { throw new Exception("histoPercent parameter error"); }
   }


   public double [] getPixelRangeCut() throws Exception { return pixelRangeCut; }


   public TransfertFct getFct() throws Exception { return fct; }
   public String getTransfertFct()  throws Exception { return getFct().toString().toLowerCase(); }

   public void setTransfertFct(String txt) {
      this.fct=TransfertFct.valueOf(txt.toUpperCase());
   }

   /** Donne l'extension des fichiers losanges */
   public String getTileExt() {
      return isColor() ? Constante.TILE_EXTENSION[ targetColorMode ] : ".fits";
   }

   protected enum JpegMethod { MEDIAN, MEAN, FIRST; }

   /**
    * @param jpegMethod the method to set
    * @see Context#MEDIAN
    * @see Context#MEAN
    */
   public void setJpegMethod(JpegMethod jpegMethod) {
      this.jpegMethod = jpegMethod;
   }
   public void setMethod(String jpegMethod) {
      this.jpegMethod = JpegMethod.valueOf(jpegMethod.toUpperCase());
   }
   public JpegMethod getJpegMethod() { return jpegMethod; }

   public void setDataCut(String scut) throws Exception {
      StringTokenizer st = new StringTokenizer(scut," ");
      int i=2;
      //      if( cut==null ) cut = new double[4];
      if( pixelRangeCut==null ) pixelRangeCut = new double[]{Double.NaN,Double.NaN,Double.NaN,Double.NaN};
      while( st.hasMoreTokens() && i<4 ) {
         String s = st.nextToken();
         pixelRangeCut[i]=Double.parseDouble(s);
         i++;
      }
      if( i<4 ) throw new Exception("Missing dataCut parameter");
      
      // BIZARRE !
//      setCutOrig(cutOrig);
   }


   public void setCutOrig(double [] cutOrig) {
      this.cutOrig=cutOrig;
   }

   /** Initialisation de la profondeur d'un cube */
   public void setDepth(int depth) {
      this.depth=depth;
      depthInit=true;
   }

   private String lastImgEtalon = null;

   /**
    * Lit l'image etalon, et affecte les données d'origines (bitpix, bscale, bzero, blank, cut)
    * @throws Exception s'il y a une erreur à la lecture du fichier
    */
   protected void initFromImgEtalon() throws Exception {
      
      // Déja fait ,
      if( lastImgEtalon!=null && lastImgEtalon.equals(imgEtalon)) return;

      String path = imgEtalon;
      Fits fitsfile = new Fits();

      fitsfile.loadHeaderFITS(path);

      setBitpixOrig(fitsfile.bitpix);
      if( !isColor() ) {
         setBZeroOrig(fitsfile.bzero);
         setBScaleOrig(fitsfile.bscale);
         if( !Double.isNaN(fitsfile.blank) ) setBlankOrig(fitsfile.blank);
      }

      // Mémorise la taille typique de l'image étalon
      typicalImgWidth = Math.max(fitsfile.width,fitsfile.height);
      
      // Peut être s'agit-il d'un cube ?
      try {
         setDepth( fitsfile.headerFits.getIntFromHeader("NAXIS3") );

         try {
            crpix3 = fitsfile.headerFits.getDoubleFromHeader("CRPIX3");
            crval3 = fitsfile.headerFits.getDoubleFromHeader("CRVAL3");
            cdelt3 = fitsfile.headerFits.getDoubleFromHeader("CDELT3");
            bunit3 = fitsfile.headerFits.getStringFromHeader("BUNIT3");
         }catch( Exception e ) { crpix3=crval3=cdelt3=0; bunit3=null; }

      } catch( Exception e ) { setDepth(1); }

      // Il peut s'agir d'un fichier .hhh (sans pixel)
      try { initCut(fitsfile); } catch( Exception e ) {
         Aladin.trace(4,"initFromImgEtalon :"+ e.getMessage());
      }


      // Positionnement initiale du HiPS par défaut
      if( target==null ) {
         Coord c = fitsfile.calib.getImgCenter();
         String s = Util.round(c.al,5)+" "+(c.del>=0?"+":"")+Util.round(c.del,5);
         setTarget(s);
         info("setTarget => "+s);
         if( targetRadius==null ) {
            double r = Math.max( fitsfile.calib.getImgHeight(),fitsfile.calib.getImgWidth());
            setTargetRadius(Util.round(r,5)+"");
         }
      }

      // Mémorisation de la résolution initiale
      double [] res = fitsfile.calib.GetResol();
      resolution = Util.myRound(Math.min(res[0],res[1]));

      lastImgEtalon = imgEtalon;
   }

   /**
    * Lit l'image et calcul son autocut : affecte les datacut et pixelcut *Origines*
    * @param file
    */
   protected void initCut(Fits file) throws Exception {
      int w = file.width;
      int h = file.height;
      int d = file.depth;
      int x=0, y=0, z=0;
      if (w > 1024) { w = 1024; x=file.width/2 - 512; }
      if (h > 1024) { h = 1024; y=file.height/2 -512; }
      if (d > 1 ) { d = 1;  z=file.depth/2 - 1/2; }
      if( file.getFilename()!=null ) file.loadFITS(file.getFilename(), 0, x, y, z, w, h, d);
      
      if( !flagNoInitEtalon ) {

         double[] cutOrig = file.findAutocutRange();

         //       cutOrig[2]=cutOrig[3]=0;  // ON NE MET PAS LE PIXELRANGE, TROP DANGEREUX... // J'HESITE DE FAIT !!!

         // PLUTOT QUE DE NE PAS INITIALISER, ON VA DOUBLER LA TAILLE DE L'INTERVALLE (sans dépasser les limites)
         double rangeData   = cutOrig[3] - cutOrig[2];
         double centerRange = cutOrig[2]/2 + cutOrig[3]/2;
         if( !Double.isInfinite( centerRange-rangeData ) ) cutOrig[2] = centerRange-rangeData;
         if( !Double.isInfinite( centerRange+rangeData ) ) cutOrig[3] = centerRange+rangeData;
         
         double max = Fits.getMax(file.bitpix);
         double min = Fits.getMin(file.bitpix);
         if( cutOrig[2]<min ) cutOrig[2]=min;
         if( cutOrig[3]>max ) cutOrig[3]=max;

         setCutOrig(cutOrig);
      }
   }

   static private int nbFiles;  // nombre de fichiers scannés
   /**
    * Sélectionne un fichier de type FITS (ou équivalent) dans le répertoire donné => va servir d'étalon
    * @return true si trouvé
    */
   boolean findImgEtalon(String rootPath) {
      if( isInputFile ) {
         try {
            setImgEtalon(rootPath);
         }  catch( Exception e) { return false; }
         return true;
      }
      nbFiles=0;
      return findImgEtalon1(rootPath);
   }
   boolean findImgEtalon1(String rootPath) {
      File main = new File(rootPath);
      String[] list = main.list();
      if( list==null ) return false;
      String path = rootPath;

      ArrayList<String> dir = new ArrayList<String>();

      for( int f = 0 ; f < list.length ; f++ ) {
         if( !rootPath.endsWith(Util.FS) ) rootPath = rootPath+Util.FS;
         path = rootPath+list[f];

         if( (new File(path)).isDirectory() ) {
            if( !list[f].equals(Constante.SURVEY) ) dir.add(path);
            continue;
         }

         nbFiles++;
         if( nbFiles>100 ) {
            Aladin.trace(4, "Context.findImgEtalon: too many files - ignored this step...");
            return false;
         }

         // essaye de lire l'entete fits du fichier
         // s'il n'y a pas eu d'erreur ça peut servir d'étalon
         MyInputStream in = null;
         try {
            in = (new MyInputStream( new FileInputStream(path))).startRead();
            if( (in.getType()&MyInputStream.FITS) != MyInputStream.FITS && !in.hasCommentCalib() ) continue;
            Aladin.trace(4, "Context.findImgEtalon: "+path+"...");
            setImgEtalon(path);
            return true;

         }  catch( Exception e) { Aladin.trace(4, "findImgEtalon : " +e.getMessage()); continue; }
         finally { if( in!=null ) try { in.close(); } catch( Exception e1 ) {} }
      }

      for( String s : dir ) {
         if( findImgEtalon1(s) ) return true;
      }

      return false;
   }

   String justFindImgEtalon(String rootPath) throws MyInputStreamCachedException {
      MyInputStream in = null;
      
      if( isInputFile ) {
         
//         // Cas particulier d'une image couleur avec un fichier .hhh qui l'accompagne
//         if( isColor() ) {
//            String file = rootPath;
//            int offset = file.lastIndexOf('.');
//            if( offset!=-1 )  file = file.substring(0,offset);
//            file += ".hhh";
//            if( (new File(file).exists()) ) rootPath=file;
//         }
         
         return rootPath;
      }

      File main = new File(rootPath);
      String[] list = main.list();
      if( list==null ) return null;
      String path = rootPath;

      ArrayList<String> dir = new ArrayList<String>();

      for( int f = 0 ; f < list.length ; f++ ) {
         if( !rootPath.endsWith(Util.FS) ) rootPath = rootPath+Util.FS;
         path = rootPath+list[f];

         if( (new File(path)).isDirectory() ) {
            if( !list[f].equals(Constante.SURVEY) ) dir.add(path);
            continue;
         }

         // essaye de lire l'entete fits du fichier
         // s'il n'y a pas eu d'erreur ça peut servir d'étalon
         try {
            // cas particulier d'un survey couleur en JPEG ou PNG avec calibration externe
            if( path.endsWith(".hhh") ) return path;

//            in = (new MyInputStream( new FileInputStream(path)) ).startRead();
            in = (new MyInputStreamCached(path) ).startRead();
            
            long type = in.getType();
            if( (type&MyInputStream.FITS) != MyInputStream.FITS && !in.hasCommentCalib() ) continue;
            return path + (hdu==null || hdu.length>0 && hdu[0]==-1 ? "":"["+hdu[0]+"]");

         }  
         catch( MyInputStreamCachedException e) { taskAbort(); throw e; }
         catch( Exception e) {
            Aladin.trace(4, "justFindImgEtalon : " +e.getMessage());
            continue;
         }
         finally { if( in!=null ) try { in.close(); } catch( Exception e1 ) {} }
      }

      for( String s : dir ) {
         String rep = justFindImgEtalon(s);
         if( rep!=null ) return rep;
      }
      return null;
   }

   protected String outputRGB;
   protected JpegMethod methodRgb;
   protected String [] plansRGB = new String [3];
   protected String [] cmsRGB = new String [3];

   public void setRgbInput(String path,int c) {
      plansRGB[c] = path;
   }

   public void setRgbCmParam(String cmParam,int c) { cmsRGB[c] = cmParam; }

   public void setSkyval(String fieldName) throws Exception {
      boolean flagNum = false;
      
      // S'agit-il de valeurs numériques pour indiquer un
      // pourcentage de l'histogramme à conserver ?
      try {
         StringTokenizer st = new StringTokenizer(fieldName);
         Double.parseDouble( st.nextToken() );
         flagNum = true;
      } catch( Exception e ) { }
      
      // Va pour les valeurs numériques
      if( flagNum ) {
         this.skyvalName = "auto";
         setHistoPercent(fieldName);
         
      // Simple mot clé
      } else {
         this.skyvalName = fieldName.toUpperCase();
      }
   }
   
   /** Postionnement direct des valeurs du skyval, notamment pour un CONCAT
    * @param s contient les 4 valeurs du cutOrig[]
    * @throws Exception
    */
   public void setSkyValues(String s) throws Exception {
      Tok tok = new Tok(s);
      cutOrig = new double[4];
      for( int i=0; tok.hasMoreTokens(); i++ ) {
         try { cutOrig[i] = Double.parseDouble( tok.nextToken() );
         } catch( Exception e) { throw new Exception("hips_skyval_values parsing error ["+s+"]"); }
      }
      flagNoInitEtalon=true;
   }

   public void setExpTime(String expTime) {
      this.expTimeName = expTime.toUpperCase();
   }

   public void setCache(CacheFits cache) {
      this.cacheFits = cache;
      cache.setContext(this);
   }

   protected void setMocArea(String s) throws Exception {
      if( s.length()==0 ) return;
      mocArea = new HealpixMoc(s);
      if( mocArea.getSize()==0 ) throw new Exception("MOC sky area syntax error");
   }

   public void setMocArea(HealpixMoc area) throws Exception {
      mocArea = area;
   }

   public double getSkyArea() {
      if( moc==null ) return 1;
      return moc.getCoverage();
   }

   public double getIndexSkyArea() {
      if( mocIndex==null ) return 1;
      return mocIndex.getCoverage();
   }

   /** Initialisation des paramètres */
   public void initParameters() throws Exception {

      if( !isColor() ) {
         bitpix = getBitpix();

         bitpixOrig = getBitpixOrig();
         cutOrig = getCutOrig();
         blankOrig = getBlankOrig();
         //         bZeroOrig = getBZeroOrig();
         //         bScaleOrig = getBScaleOrig();

         // Le blank de sortie est imposée
         blank = getDefaultBlankFromBitpix(bitpix);

         // le cut de sortie est par défaut le même que celui d'entrée
         cut = new double[5];
         if( cutOrig==null ) cutOrig = new double[5];
         System.arraycopy(cutOrig, 0, cut, 0, cutOrig.length);

         // si les dataCut d'origine sont nuls ou incorrects, on les mets au max
         if( cutOrig[2]>=cutOrig[3] ) {
            cutOrig[2] = bitpixOrig==-64?-Double.MAX_VALUE : bitpixOrig==-32? -Float.MAX_VALUE
                  : bitpixOrig==64?Long.MIN_VALUE+1 : bitpixOrig==32?Integer.MIN_VALUE+1 : bitpixOrig==16?Short.MIN_VALUE+1:1;
            cutOrig[3] = bitpixOrig==-64?Double.MAX_VALUE : bitpixOrig==-32? Float.MAX_VALUE
                  : bitpixOrig==64?Long.MAX_VALUE : bitpixOrig==32?Integer.MAX_VALUE : bitpixOrig==16?Short.MAX_VALUE:255;
         }

         // Y a-t-il un changement de bitpix ?
         // Les cuts changent
         if( bitpixOrig!=-1 && bitpix != bitpixOrig ) {
            cut[2] = bitpix==-64?-Double.MAX_VALUE : bitpix==-32? -Float.MAX_VALUE
                  : bitpix==64?Long.MIN_VALUE+1 : bitpix==32?Integer.MIN_VALUE+1 : bitpix==16?Short.MIN_VALUE+1:1;
            cut[3] = bitpix==-64?Double.MAX_VALUE : bitpix==-32? Float.MAX_VALUE
                  : bitpix==64?Long.MAX_VALUE : bitpix==32?Integer.MAX_VALUE : bitpix==16?Short.MAX_VALUE:255;
            coef = (cut[3]-cut[2]) / (cutOrig[3]-cutOrig[2]);

            cut[0] = (cutOrig[0]-cutOrig[2])*coef + cut[2];
            cut[1] = (cutOrig[1]-cutOrig[2])*coef + cut[2];

            bzero = bZeroOrig + bScaleOrig*(cutOrig[2] - cut[2]/coef);
            bscale = bScaleOrig/coef;


            info("Change BITPIX from "+bitpixOrig+" to "+bitpix);
            info("Map original pixel range ["+cutOrig[2]+" .. "+cutOrig[3]+"] " +
                  "to ["+cut[2]+" .. "+cut[3]+"]");
            info("Change BZERO,BSCALE,BLANK="+bZeroOrig+","+bScaleOrig+","+blankOrig
                  +" to "+bzero+","+bscale+","+blank);

            if( Double.isInfinite(bzero) || Double.isInfinite(bscale) ) throw new Exception("pixelRange parameter required !");

            // Pas de changement de bitpix
         } else {
            bzero=bZeroOrig;
            bscale=bScaleOrig;
            Aladin.trace(3,"BITPIX kept "+bitpix+" BZERO,BSCALE,BLANK="+bzero+","+bscale+","+blank);
         }

         // Calcul des valeurs raw des good pixels
         if( pixelGood!=null ) {
            good = new double[2];
            good[0] = (pixelGood[0]-bZeroOrig)/bScaleOrig;
            good[1] = (pixelGood[1]-bZeroOrig)/bScaleOrig;
         }

      }

      // Détermination de la zone du ciel à calculer
      initRegion();
   }

   /** Détermination de la zone du ciel à calculer (appeler par initParameters()) ne pas utiliser tout
    * seul sauf si besoin explicite */
   protected void initRegion() throws Exception {
      if( isValidateRegion() ) return;
      try {
         if( mocIndex==null ) {
            if( isMap() ) mocIndex=new HealpixMoc("0/0-11");
            else loadMocIndex();
         }
      } catch( Exception e ) {
         //         warning("No MOC index found => assume all sky");
         mocIndex=new HealpixMoc("0/0-11");  // par défaut tout le ciel
      }
      if( mocArea==null ) moc = mocIndex;
      else moc = mocIndex.intersection(mocArea);
      setValidateRegion(true);
   }

   /** Retourne la zone du ciel à calculer */
   protected HealpixMoc getRegion() { return moc; }

   /** Chargement du MOC de l'index */
   protected void loadMocIndex() throws Exception {
      HealpixMoc mocIndex = new HealpixMoc();
      mocIndex.read( getHpxFinderPath()+Util.FS+Constante.FILE_MOC);
      this.mocIndex=mocIndex;
   }

   /** Chargement du MOC réel */
   protected void loadMoc() throws Exception {
      HealpixMoc mocIndex = new HealpixMoc();
      mocIndex.read( getOutputPath()+Util.FS+Constante.FILE_MOC);
      this.mocIndex=mocIndex;
   }

   protected HealpixMoc getMocIndex() { return mocIndex; }

   //   /** Positionne les cuts de sortie en fonction du fichier Allsky.fits
   //    * @return retourn le cut ainsi calculé
   //    * @throws Exception
   //    */
   //   protected double [] setCutFromAllsky() throws Exception {
   //      double [] cut = new double[4];
   //      String fileName=getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits";
   //      try {
   //         if( !(new File(fileName)).exists() ) throw new Exception("No available Allsky.fits file for computing cuts");
   //         Fits fits = new Fits();
   //         fits.loadFITS(fileName);
   //         cut = fits.findAutocutRange(0, 0, true);
   //         info("setCut from Allsky.fits => cut=["+cut[0]+".."+cut[1]+"] range=["+cut[2]+".."+cut[3]+"]");
   //         setCut(cut);
   //      } catch( Exception e ) { throw new Exception("No available Allsky.fits file for computing cuts"); }
   //      return cut;
   //   }

   public boolean verifTileOrder() {

      // Récupération d'un éventuel changement de TileOrder dans les propriétés du HpxFinder
      InputStreamReader in = null;
      boolean flagTileOrderFound=false;
      try {
         String propFile = getHpxFinderPath()+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         prop.load( in=new InputStreamReader( new FileInputStream( propFile )) );
         int o;
         String s = prop.getProperty(Constante.KEY_HIPS_TILE_WIDTH);
         if( s!=null ) o = (int)CDSHealpix.log2( Integer.parseInt(s));
         else o = Integer.parseInt( prop.getProperty(Constante.OLD_TILEORDER) );

         if( o!=getTileOrder() ) {
            if( tileOrder!=-1 && o!=tileOrder ) {
               warning("Uncompatible tileOrder="+tileOrder+" compared to pre-existing survey tileOrder="+o);
               return false;

            }
            setTileOrder(o);
            int w = getTileSide();
            info("Specifical tileOrder="+o+" tileSize="+w+"x"+w);
         }
         flagTileOrderFound=true;
      }
      catch( Exception e ) { }
      finally { if( in!=null ) { try { in.close(); } catch( Exception e ) {} } }

      // Si rien d'indiqué dans Properties du HpxFinder, c'est que ce doit être l'ordre par défaut
      if( !flagTileOrderFound ) {
         if( getTileOrder()!=Constante.ORDER ) {
            warning("Uncompatible tileOrder="+getTileOrder()+" compared to default pre-existing survey tileOrder="+Constante.ORDER);
            return false;

         }
      }

      return true;
   }

   public boolean verifFrame() {

      // Récupération d'un éventuel changement de hips_frame dans les propriétés du HpxFinder
      InputStreamReader in = null;
      boolean flagFrameFound=false;
      try {
         String propFile = getHpxFinderPath()+Util.FS+Constante.FILE_PROPERTIES;
         MyProperties prop = new MyProperties();
         prop.load( in=new InputStreamReader( new FileInputStream( propFile )) );
         int o=0;
         String s = prop.getProperty(Constante.KEY_HIPS_FRAME);
         if( s!=null ) {
            flagFrameFound=true;
            o = getFrameVal(s);
         }

         if( flagFrameFound ) {
            if( hasFrame() ) {
               if( o!=getFrame() ) {
                  warning("Uncompatible coordinate frame="+getFrameName()+" compared to pre-existing survey frame="+getFrameName(o));
                  return false;
               }
            } else {
               setFrame(o);
            }
         }
      } catch( Exception e ) { }
      finally { if( in!=null ) { try { in.close(); } catch( Exception e ) {} } }

      return true;
   }

   public boolean verifCoherence() {

      if( !verifFrame() ) return false;
      if( !verifTileOrder() ) return false;

      if( mode==Mode.REPLACETILE ) return true;

      if( !isColor() ) {
         String fileName=getOutputPath()+Util.FS+"Norder3"+Util.FS+"Allsky.fits";
         if( !(new File(fileName)).exists() ) return true;
         Fits fits = new Fits();
         try { fits.loadHeaderFITS(fileName); }
         catch( Exception e ) { return true; }
         if( fits.bitpix!=bitpix ) {
            warning("Uncompatible BITPIX="+bitpix+" compared to pre-existing survey BITPIX="+fits.bitpix);
            return false;
         }
         boolean nanO = Double.isNaN(fits.blank);
         boolean nan = Double.isNaN(blank);

         // Cas particulier des Survey préexistants sans BLANK en mode entier. Dans ce cas, on accepte
         // tout de même de traiter en sachant que le blank défini par l'utilisateur sera
         // considéré comme celui du survey existant. Mais il faut nécessairement que l'utilisateur
         // renseigne ce champ blank explicitement
         if( bitpix>0 && nanO ) {
            nan = !Double.isNaN(getBlankOrig());
         }

         if( nanO!=nan || !nan && fits.blank!=blank ) {
            warning("Uncompatible BLANK="+blank+" compared to pre-existing survey BLANK="+fits.blank);
            return false;
         }
      }

      int or = cds.tools.pixtools.Util.getMaxOrderByPath(getOutputPath());
      if( or!=-1 && or!=getOrder() ) {
         warning("Uncompatible order="+getOrder()+" compared to pre-existing survey order="+or);
         return false;
      }

      return true;
   }

   private double getDefaultBlankFromBitpix(int bitpix) {
      return bitpix<0 ? Double.NaN : bitpix==32 ? Integer.MIN_VALUE : bitpix==16 ? Short.MIN_VALUE : 0;
   }

   /** Interprétation de la chaine décrivant les bords à ignorer dans les images sources,
    * soit une seule valeur appliquée à tous les bords,
    * soit 4 valeurs affectées à la java de la manière suivante : Nord, Ouest, Sud, Est
    * @throws ParseException */
   private int [] parseBorderSize(String s) throws ParseException {
      int [] border = { 0,0,0,0 };
      try {
         StringTokenizer st = new StringTokenizer(s," ,;-");
         for( int i=0; i<4 && st.hasMoreTokens(); i++ ) {
            String s1 = st.nextToken();
            border[i] = Integer.parseInt(s1);
            if( i==0 ) border[3]=border[2]=border[1]=border[0];
         }
         int x = border[0]; border[0] = border[2]; border[2] = x;  // Permutations pour respecter l'ordre North West South East
      } catch( Exception e ) {
         throw new ParseException("Border error => assume 0", 0);
      }
      return border;
   }

   protected boolean isExistingDir() {
      String path = getInputPath();
      if( path==null ) return false;
      return  (new File(path)).isDirectory();
   }
   
   protected boolean isExistingAllskyDir() { return isExistingAllskyDir( getOutputPath() ); }
   protected boolean isExistingAllskyDir(String path) {
      if( path==null ) return false;
      File f = new File(path);
      if( !f.exists() ) {
         return false;
      }
      int order = cds.tools.pixtools.Util.getMaxOrderByPath(path);
      return order!=-1;
   }
   
   protected boolean hasPropertyFile(String path) {
      File f = new File(path+Util.FS+Constante.FILE_PROPERTIES);
      return f.exists();
   }

   protected boolean isExistingIndexDir() {
      String path = getHpxFinderPath();
      if( path==null ) return false;
      File f = new File(path);
      if( !f.exists() ) return false;
      for( File fc : f.listFiles() ) { if( fc.isDirectory() && fc.getName().startsWith("Norder") ) return true; }
      return false;
   }

   /** Positionne le MOC correspondant à l'index */
   protected void setMocIndex(HealpixMoc m) throws Exception {
      mocIndex=m;
   }

   /** Retourne le nombre de cellules à calculer (baser sur le MOC de l'index et le MOC de la zone) */
   protected long getNbLowCells() {
      int o = getOrder();
      if( moc==null && mocIndex==null || o==-1 ) return -1;
      HealpixMoc m = moc!=null ? moc : mocIndex;
      if( o!=m.getMocOrder() ) {
         m =  (HealpixMoc) m.clone();
         try { m.setMocOrder( o ); } catch( Exception e ) {}
      }
      long res = m.getUsedArea() * depth;
      //      Aladin.trace(4,"getNbLowsCells => mocOrder="+m.getMocOrder()+" => UsedArea="+m.getUsedArea()+"+ depth="+depth+" => "+res);
      return res;
   }

   /** Retourne le volume du HiPS en fits en fonction du nombre de cellules prévues et du bitpix */
   protected long getDiskMem() {
      long nbLowCells = getNbLowCells();
      if( nbLowCells==-1 || bitpix==0 ) return -1;
      long mem = nbLowCells *getTileSide()*getTileSide() * (Math.abs(bitpix)/8);

      return mem;
   }

   protected int lastNorder3=-2;
   protected void setProgressLastNorder3 (int lastNorder3) { this.lastNorder3=lastNorder3; }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showIndexStat(int statNbFile, int statBlocFile, int statNbZipFile, long statMemFile, long statPixSize, long statMaxSize,
         int statMaxWidth, int statMaxHeight, int statMaxDepth, int statMaxNbyte,long statDuree) {
      String s;
      if( statNbFile==-1 ) s = "";
      else {
         String nbPerSec =  statDuree>1000 ? ""+Util.round(statNbFile/(statDuree/1000.),1) : "";
         s= statNbFile+" file"+(statNbFile>1?"s":"")
               +" in "+Util.getTemps(statDuree)
               +(nbPerSec.length()==0 ? "":" => "+nbPerSec+"/s")
               + (statNbFile>0 && statNbZipFile==statNbFile ? " - all gzipped" : statNbZipFile>0 ? " ("+statNbZipFile+" gzipped)":"")
               + " => "+Util.getUnitDisk(statPixSize).replace("B","pix")
               + " using "+Util.getUnitDisk(statMemFile)
               + (statNbFile>1 && statMaxSize<0 ? "" : " => biggest: ["+statMaxWidth+"x"+statMaxHeight
                     +(statMaxDepth>1?"x"+statMaxDepth:"")+" x"+statMaxNbyte+"]");
      }
      stat(s);
   }

   // Demande d'affichage des stats (dans le TabBuild)
   protected void showTilesStat(int statNbThreadRunning, int statNbThread, long totalTime,
         int statNbTile, int statNbEmptyTile, int statNodeTile, long statMinTime, long statMaxTime, long statAvgTime,
         long statNodeAvgTime,long usedMem,long deltaTime,long deltaNbTile) {

      if( statNbTile==0 ) return;
      long nbCells = getNbLowCells();
      long nbLowTile = statNbTile+statNbEmptyTile;
      String sNbCells = nbCells==-1 ? "" : "/"+nbCells;
      String pourcentNbCells = nbCells==-1 ? "" :
         nbCells==0 ? "-":(Math.round( ( (double)nbLowTile/nbCells )*1000)/10.)+"%";
      
      long tempsTotalEstime = nbLowTile==0 ? 0 : nbCells==0 ? 0 :(nbCells*totalTime)/nbLowTile - totalTime;

      long nbTilesPerMin = (deltaNbTile*60000L)/deltaTime;

      String s=statNbTile+(statNbEmptyTile==0?"":"+"+statNbEmptyTile)+sNbCells+" tiles + "+statNodeTile+" nodes in "+Util.getTemps(totalTime,true)+" ("
            +pourcentNbCells+(nbTilesPerMin<=0 ? "": " "+nbTilesPerMin+" tiles/mn EndsIn:"+Util.getTemps(tempsTotalEstime,true))+") "
//            +Util.getTemps(statAvgTime)+"/tile ["+Util.getTemps(statMinTime)+" .. "+Util.getTemps(statMaxTime)+"] "
//            +Util.getTemps(statNodeAvgTime)+"/node"
            +(statNbThread==0 ? "":"by "+statNbThreadRunning+"/"+statNbThread+" threads")
            //         +" using "+Util.getUnitDisk(usedMem)
            ;

      stat(s);
      if( cacheFits!=null && cacheFits.getStatNbOpen()>0 ) stat(cacheFits+"");

      setProgress(statNbTile+statNbEmptyTile, nbCells);
   }

   protected void showMapStat(long  cRecord,long nbRecord, long cTime,CacheFits cache, String info ) {
      double pourcent = (double)cRecord/nbRecord;
      long totalTime = (long)( cTime/pourcent);
      long endsIn = totalTime-cTime;
      stat(Util.round(pourcent*100,1)+"% in " +Util.getTemps(cTime, true)+" endsIn:"+Util.getTemps(endsIn, true)
            + " (record="+(cRecord+1)+"/"+nbRecord+")");
      if( cache!=null && cache.getStatNbOpen()>0 ) stat(cache+"");
      setProgress(cRecord,nbRecord);
   }

   // Demande d'affichage des stats (dans le TabJpeg)
   protected void showJpgStat(int statNbFile, long cTime,int statNbThread,int statNbThreadRunning) {
      long nbLowCells = getNbLowCells();

      double pourcent = nbLowCells<=0 ? 0 : (double)statNbFile/nbLowCells;
      long totalTime = (long)( cTime/pourcent );
      long endsIn = totalTime-cTime;
      String pourcentNbCells = nbLowCells==-1 ? "" :
         (Math.round( ( (double)statNbFile/nbLowCells )*1000)/10.)+"%) ";

      String s;
      if( nbLowCells<=0 ) s = s=statNbFile+" tiles in "+Util.getTemps(cTime,true);
      else s=statNbFile+"/"+nbLowCells+" tiles in "+Util.getTemps(cTime,true)+" ("
            +pourcentNbCells+" endsIn:"+Util.getTemps(endsIn,true)
            +(statNbThread==0 ? "":" by "+statNbThreadRunning+"/"+statNbThread+" threads");

      stat(s);
   }

   // Demande d'affichage des stats (dans le TabJpeg)
   protected void showMirrorStat(int statNbFile, long cumul, long lastCumulPerSec,
         long cTime,int statNbThread,int statNbThreadRunning) {
      long nbLowCells = getNbLowCells();

      double pourcent = nbLowCells<=0 ? 0 : (double)statNbFile/nbLowCells;
      long totalTime = (long)( cTime/pourcent );
      long endsIn = totalTime-cTime;
      String pourcentNbCells = nbLowCells==-1 ? "" :
         (Math.round( ( (double)statNbFile/nbLowCells )*1000)/10.)+"%) ";

      String debit = cTime>1000L ? Util.getUnitDisk( cumul/(cTime/1000L) )+"/s" : "OB/s";
      String debitI = Util.getUnitDisk( lastCumulPerSec )+"/s";

      String s;
      if( nbLowCells<=0 ) s = s=statNbFile+" tiles in "+Util.getTemps(cTime,true);
      else s=statNbFile+"/"+nbLowCells+" tiles in "+Util.getTemps(cTime,true)+" ("
            +pourcentNbCells+"endsIn:"+Util.getTemps(endsIn,true)
            +" speed:"+ debitI + " avg:"+debit +" for "+Util.getUnitDisk(cumul)
            +(statNbThread==0 ? "":" by "+statNbThreadRunning+"/"+statNbThread+" threads");

      stat(s);
   }
   
   // Demande d'affichage des stats (dans le TabJpeg)
   protected void showRGBStat(int statNbFile, long cTime,int statNbThread,int statNbThreadRunning) {
      long nbLowCells = getNbLowCells();

      double pourcent = nbLowCells<=0 ? 0 : (double)statNbFile/nbLowCells;
      long totalTime = (long)( cTime/pourcent );
      long endsIn = totalTime-cTime;
      String pourcentNbCells = nbLowCells==-1 ? "" :
         (Math.round( ( (double)statNbFile/nbLowCells )*1000)/10.)+"%) ";

      String s;
      if( nbLowCells<=0 ) s = s=statNbFile+" tiles in "+Util.getTemps(cTime,true);
      else s=statNbFile+"/"+nbLowCells+" tiles in "+Util.getTemps(cTime,true)+" ("
            +pourcentNbCells+" endsIn:"+Util.getTemps(endsIn,true)
            +(statNbThread==0 ? "":" by "+statNbThreadRunning+"/"+statNbThread+" threads");

      stat(s);
   }


//   // Demande d'affichage des stats (dans le TabRgb)
//   protected void showRgbStat(int statNbFile, long statSize, long totalTime) {
//      if( statNbFile>0 ) showJpgStat(statNbFile, totalTime, 1, 1);
//   }

   protected Action action=null;      // Action en cours (voir Action)
   protected double progress=-1;       // Niveau de progression de l'action en cours, -1 si non encore active, =progressMax si terminée
   protected double progressMax=Double.MAX_VALUE;   // Progression max de l'action en cours (MAX_VALUE si inconnue)
   protected JProgressBar progressBar=null;  // la progressBar attaché à l'action
   protected MyProperties prop=null;

   private boolean isMap=false;       // true s'il s'agit d'une map HEALPix FITS
   protected boolean isMap() { return isMap; }
   protected void setMap(boolean flag ) { isMap=flag; }

   protected boolean ignoreStamp;
   public void setIgnoreStamp(boolean flag) { ignoreStamp=true; }

   private boolean taskRunning=false;        // true s'il y a un processus de calcul en cours
   public boolean isTaskRunning() { return taskRunning; }
   public void setTaskRunning(boolean flag) {
      if( flag ) taskAborting=false;            // Si la dernière tache a été interrompue, il faut reswitcher le drapeau
      else progressBar=null;
      taskRunning=flag;
      resumeWidgets();
   }
   private boolean taskPause=false;          // true si le processus de calcul est en pause
   public boolean isTaskPause() { return taskPause; }
   public void setTaskPause(boolean flag) {
      taskPause=flag;
      resumeWidgets();
   }

   protected boolean taskAborting=false;       // True s'il y a une demande d'interruption du calcul en cours
   public void taskAbort() { taskAborting=true; taskPause=false; }
   public boolean isTaskAborting() {
      if( taskAborting ) return true;
      while( taskPause ) Util.pause(500);
      return false;
   }

   static private SimpleDateFormat DATEFORMAT = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
   //   static protected String getNow() { return DATEFORMAT.format( new Date() ); }
   static protected String getNow() { return Constante.getDate(); }
   static long getTime(String date) throws Exception { return DATEFORMAT.parse(date).getTime(); }

   static private String getKeyActionStart(Action a) { return "Processing."+a+".start"; }
   static private String getKeyActionEnd(Action a)   { return "Processing."+a+".end"; }

   public void startAction(Action a) throws Exception {
      action=a;
      action.startTime();
      //      running("========= "+action+" ==========");
      //      updateProperties( getKeyActionStart(action), getNow(),true);
      setProgress(0,-1);
   }
   public void endAction() throws Exception {
      if( action==null ) return;
      if( isTaskAborting() ) abort(action+" abort (after "+Util.getTemps(action.getDuree())+")");
      else {
         done(action+" done (in "+Util.getTemps(action.getDuree())+")");
         //         updateProperties( getKeyActionEnd(action), getNow(),true);
      }
      action=null;
   }
   public Action getAction() { return action; }

   /** true si l'action a été correctement estampillée comme terminée dans le fichier des propriétés */
   public boolean actionAlreadyDone(Action a) {
      if( ignoreStamp ) return false;
      try {
         if( prop==null ) loadProperties();
         if( prop==null ) return false;
         String end = prop.getProperty( getKeyActionEnd(a) );
         if( end==null ) return false;    // Jamais encore terminée
         String start = prop.getProperty( getKeyActionStart(a) );
         if( start==null ) return false;  // Jamais encore commencée
         //         System.out.println("ActionAlready done: "+a+" start="+start+" end="+end);
         if( getTime(end)<getTime(start) ) return false; // Certainement relancée, mais non-achevée
      } catch( Exception e ) {
         e.printStackTrace();
         return false;
      }
      return true;
   }

   /** true si les deux actions sont terminées et que la première précède la seconde */
   public boolean actionPrecedeAction(Action avant,Action apres) {
      if( ignoreStamp ) return false;
      try {
         if( prop==null ) loadProperties();
         if( prop==null ) return false;
         if( !actionAlreadyDone(avant) || !actionAlreadyDone(apres)) return false; // L'une des 2 actions n'a pas été terminée
         String endAvant = prop.getProperty( getKeyActionEnd(avant) );
         String endApres = prop.getProperty( getKeyActionEnd(apres) );
         //         System.out.println("actionPrecedeAction done: "+avant+"="+endAvant+" "+apres+"="+endApres);
         if( getTime(endApres)<getTime(endAvant) ) return false;  // L'action avant est postérieure
      } catch( Exception e ) {
         e.printStackTrace();
         return false;
      }
      return true;
   }

   public void setProgress(double progress,double progressMax) { setProgress(progress); setProgressMax(progressMax); }
   public void setProgress(double progress) { this.progress=progress; }
   public void setProgressMax(double progressMax) { this.progressMax=progressMax; }
   public void progressStatus() { System.out.print('.'); flagNL=true; }

   //   static private char []  STATUS = { '|','/','-','\\' };
   //   private int indexStatus=0;
   //
   //   public void progressStatus() {
   //      System.out.print( "\b" );
   //      System.out.print( STATUS[indexStatus] );
   //      indexStatus++;
   //      if( indexStatus>=STATUS.length ) indexStatus=0;
   //      flagNL=true;
   //   }

   public void enableProgress(boolean flag) { System.out.println("progress ["+action+"] enable="+flag); }
   public void setProgressBar(JProgressBar bar) { }
   public void resumeWidgets() { }

   public void trace(int i, String string) {
      if (Aladin.levelTrace>=i)
         System.out.println(string);
   }
   public void setTrace(int trace) {
      Aladin.levelTrace = trace;
   }

   /**
    * @param verbose the verbose level to set
    */
   public static void setVerbose(boolean verbose) {
      Context.verbose = verbose;
      BuilderTiles.DEBUG=true;
   }

   /** Verbose or not ? */
   public static int getVerbose() { return Aladin.levelTrace; }

   /**
    * Niveau de verbosité :
    * -1    rien
    * 0     stats
    * 1-4   traces habituelles d'Aladin
    * @param verbose the verbose to set
    */
   public static void setVerbose(int level) {
      if (level>=0) {
         Context.verbose = true;
         Aladin.levelTrace = level;
      }
      else {
         Context.verbose = false;
         Aladin.levelTrace = 0;
      }
   }


   private boolean flagNL=false;   // indique qu'il faut ou non insérer un NL dans les stats, infos...

   // Insère un NL si nécessaire
   private void nl() { if( flagNL ) System.out.println(); flagNL=false; }
   //   private void nl() { if( flagNL ) System.out.print("\b"); flagNL=false; }
   
   // Retourne la chaine indiquée encadrée par deux traits
   // ex: ------------------- toto -------------------
   static public String getTitle(String s ) { return getTitle(s,'-'); }
   static public String getTitle(String s, char c ) { return getTitle(s,c,102); }
   static public String getTitle(String s, char c, int len) {
      int m = (len - 2 - s.length() )/2;
      StringBuilder s1 = new StringBuilder();
      for( int i=0; i<m; i++ ) s1.append(c);
      return s1+" "+s+" "+(s.length()%2==0?"":" ")+s1;
   }
   
   protected boolean ANSI = false;
   
   private String rouge() { return ANSI ? "\033[32m" : ""; }
   private String brun()  { return ANSI ? "\033[33m" : ""; }
   private String blue()  { return ANSI ? "\033[34m" : ""; }
   private String violet(){ return ANSI ? "\033[35m" : ""; }
   private String bluec() { return ANSI ? "\033[36m" : ""; }
   private String cyan()  { return ANSI ? "\033[37m" : ""; }
   private String end()   { return ANSI ? "\033[0m"  : ""; }

   public void running(String s)  { nl(); System.out.println(blue()  +"RUN   : "+getTitle(s,'=')+end()); }
   public void done(String r)     { nl(); System.out.println(blue()  +"DONE  : "+r+end()); }
   public void abort(String r)    { nl(); System.out.println(rouge() +"ABORT : "+r+end()); }
   public void info(String s)     { nl(); System.out.println(         "INFO  : "+s); }
   public void warning(String s)  { nl(); System.out.println(violet()+"*WARN*: "+s+end()); }
   public void error(String s)    { nl(); System.out.println(rouge() +"*ERROR: "+s+end()); }
   public void action(String s)   { nl(); System.out.println(blue()  +"ACTION: "+s+end()); }
   public void stat(String s)     { nl(); System.out.println(bluec() +"STAT  : "+s+end()); }

   private boolean validateOutputDone=false;
   public boolean isValidateOutput() { return validateOutputDone; }
   public void setValidateOutput(boolean flag) { validateOutputDone=flag; }

   private boolean validateInputDone=false;
   public boolean isValidateInput() { return validateInputDone; }
   public void setValidateInput(boolean flag) { validateInputDone=flag; }

   private boolean validateCutDone=false;
   public boolean isValidateCut() { return validateCutDone; }
   public void setValidateCut(boolean flag) { validateCutDone=flag; }

   private boolean validateRegion=false;
   public boolean isValidateRegion() { return validateRegion; }
   public void setValidateRegion(boolean flag) { validateRegion=flag; }

   static private final Astrocoo COO_GAL = new Astrocoo(new Galactic());
   static private final Astrocoo COO_EQU = new Astrocoo(new ICRS());
   static private Astroframe AF_GAL1 = new Galactic();
   static private Astroframe AF_ICRS1 = new ICRS();

   /** Mémorisation d'une propriété à ajouter dans le fichier properties */
   protected void setComment(String comment) { setPropriete1("#","#"+comment,false); }
   protected void insertPropriete(String key,String value) { setPropriete1(key,value,true); }
   protected void setPropriete(String key, String value) { setPropriete1(key,value,false); }
   
   private void setPropriete1(String key, String value,boolean flagInsert) {
      if( keyAddProp==null ) {
         keyAddProp = new Vector<String>();
         valueAddProp = new Vector<String>();
      }
      if( flagInsert ) {
         keyAddProp.insertElementAt(key,0);
         valueAddProp.insertElementAt(value,0);
      } else {
         keyAddProp.addElement(key);
         valueAddProp.addElement(value);
      }
   }

   private String INDEX =

         "<HTML>\n" +
               "<HEAD>\n" +
               "   <script type=\"text/javascript\" src=\"http://code.jquery.com/jquery-1.10.1.min.js\"></script>\n" +
               "   <link rel=\"stylesheet\" href=\"http://aladin.u-strasbg.fr/AladinLite/api/v2/latest/aladin.min.css\" >\n" +
               "   <script type=\"text/javascript\">var jqMenu = jQuery.noConflict();</script>\n" +
               "   <script type=\"text/javascript\">\n" +
               "var hipsDir=null;</script>\n" +
               "</HEAD>\n" +

         "<H1>\"$LABEL\" progressive survey</H1>\n" +
         "This Web resource contains HiPS(*) components for <B>$LABEL</B> progressive survey.\n" +
         "<script type=\"text/javascript\">\n" +
         "hipsDir = location.href;\n" +
         "hipsDir = hipsDir.substring(0,hipsDir.lastIndexOf(\"/\",hipsDir.length));\n" +
         "document.getElementById(\"hipsBase\").innerHTML=hipsDir;\n" +
         "</script>\n" +
         "<TABLE>\n" +
         "<TR>\n" +
         "<TD>\n" +
         "   <script type=\"text/javascript\" src=\"http://aladin.u-strasbg.fr/AladinLite/api/v2/latest/aladin.min.js\" charset=\"utf-8\"></script>\n" +
         "<div id=\"aladin-lite-div\" style=\"width:350px;height:350px;\"></div>\n" +
         "<script type=\"text/javascript\">\n" +
         "//var hipsDir = location.href;\n" +
         "//hipsDir = hipsDir.substring(0,hipsDir.lastIndexOf(\"/\",hipsDir.length));\n" +
         "var aladin = $.aladin(\"#aladin-lite-div\");\n" +
         "aladin.setImageSurvey(aladin.createImageSurvey('$LABEL', '$LABEL',\n" +
         "hipsDir, '$SYS', $ORDER, {imgFormat: '$FMT'}));\n" +

         "</script>    \n" +

         "</TD>\n" +
         "<TD>\n" +
         "<UL>\n" +
         "$INFO" +
         "   <LI> <B>Raw property file:</B> <A HREF=\"properties\">properties</A>\n" +
         "   <LI> <B>Base URL:<p id=\"hipsBase\"></p></B> \n" +
         "</UL>\n" +
         "</TD>\n" +
         "</TR>\n" +
         "</TABLE>\n" +

         "This survey can be displayed by <A HREF=\"http://aladin.u-strasbg.fr/AladinLite\">Aladin Lite</A> (see above), \n" +
         "by <A HREF=\"http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=downloading\">Aladin Desktop</A> client\n" +
         "(just open the base URL)<BR>or any other HiPS aware clients .\n" +
         "<HR>\n" +
         "<I>(*) The HiPS technology allows a dedicated client to access an astronomical survey at any location and at any scale. \n" +
         "HiPS is based on HEALPix sky tessellation and it is designed for astronomical scientifical usages (low distorsion, true pixel values...)." +
         "HiPS technical documentation is available <A HREF=\"http://aladin.u-strasbg.fr/HiPS/HiPS%20technical%20doc.pdf\">here<A></I>\n" +
         "<script type=\"text/javascript\">\n" +
         "document.getElementById(\"hipsBase\").innerHTML=hipsDir;\n" +
         "</script>\n" +

         "</HTML>\n" +
         "";

   /** Génère un fichier index.html afin de pouvoir afficher qq chose si l'utilisateur charge
    * directement dans son navigateur le répertoire de base du HiPS généré
    */
   protected void writeIndexHtml() throws Exception {
      String label = getLabel();
      if( label==null || label.length()==0 ) label= "XXX_"+(System.currentTimeMillis()/1000);

      int order = getOrder();
      if( order==-1 ) order = cds.tools.pixtools.Util.getMaxOrderByPath( getOutputPath() );

      if( moc==null ) try { loadMoc(); } catch( Exception e ) { }
      if( prop==null ) loadProperties();
      String sys = prop.getProperty(Constante.KEY_HIPS_FRAME);
      if( sys==null ) sys="galactic";

      long nside = CDSHealpix.pow2(order);
      long nsideP = CDSHealpix.pow2(order+getTileOrder());
      double resol = CDSHealpix.pixRes(nsideP)/3600;

      int width = getTileSide();
      String tiles = getAvailableTileFormats();
      String fmt = tiles.indexOf("png")>=0 ? "png" : "jpg";

      String res = INDEX.replace("$LABEL",label);
      StringBuilder info = new StringBuilder();
      info.append("   <LI> <B>Label:</B> "+label+"\n");
      info.append("   <LI> <B>Type:</B> "+(depth>1?"HiPS cube ("+depth+" frames)" : isColor() ? "colored HiPS image" : "HiPS image")+"\n");
      info.append("   <LI> <B>Best pixel angular resolution:</B> "+Coord.getUnit( resol )+"\n");
      info.append("   <LI> <B>Max tile order:</B> "+order+" (NSIDE="+nside+")\n");
      info.append("   <LI> <B>Available encoding tiles:</B> "+tiles+"\n");
      info.append("   <LI> <B>Tile size:</B> "+width+"x"+width+"\n");
      if( bitpix!=0 && bitpix!=-1 ) info.append("   <LI> <B>FITS tile BITPIX:</B> "+bitpix+"\n");
      info.append("   <LI> <B>Processing date:</B> "+getNow()+"\n");
      info.append("   <LI> <B>HiPS builder:</B> "+"Aladin/HipsGen "+Aladin.VERSION+"\n");
      info.append("   <LI> <B>Coordinate frame:</B> " +sys+"\n");
      if( moc!=null ) {
         double cov = moc.getCoverage();
         double degrad = Math.toDegrees(1.0);
         double skyArea = 4.*Math.PI*degrad*degrad;
         info.append("   <LI> <B>Sky area:</B> "+Util.round(cov*100, 3)+"% of sky => "+Coord.getUnit(skyArea*cov, false, true)+"^2\n");
         info.append("   <LI> <B>Associated coverage map:</B> <A HREF=\""+Constante.FILE_MOC+"\">MOC</A>\n");
      }

      String metadata = cds.tools.Util.concatDir( getHpxFinderPath(),Constante.FILE_METADATAXML);
      if( (new File(metadata)).exists() ) {
         info.append("   <LI> <B>Original data access template:</B> <A HREF=\"HpxFinder/"+Constante.FILE_METADATAXML+"\">"+Constante.FILE_METADATAXML+"</A>\n");
      }

      res = res.replace("$INFO",info);
      res = res.replace("$ORDER",order+"");
      res = res.replace("$SYS",sys);
      res = res.replace("$FMT",fmt);

      String tmp = getOutputPath()+Util.FS+"index.html";
      File ftmp = new File(tmp);
      if( ftmp.exists() ) ftmp.delete();
      FileOutputStream out = null;
      try {
         out = new FileOutputStream(ftmp);
         out.write(res.getBytes());
      } finally {  if( out!=null ) out.close(); }
   }

   /** Création d'un fichier metadata.txt associé au HiPS */
   protected void writeMetadataFits() throws Exception {
      writeMetadataFits(getOutputPath(),header);
   }
   
   static public void writeMetadataFits(String path, HeaderFits header) throws Exception {

      // POUR LE MOMENT JE PREFERE NE PAS LE METTRE
      //      // Si je n'ai pas de Header spécifique, je récupère
      //      // celui de l'image étalon
      //      if( header==null && imgEtalon!=null ) {
      //         try {
      //            MyInputStream in = new MyInputStream( new FileInputStream( imgEtalon ) );
      //            header = new HeaderFits( in );
      //            in.close();
      //         } catch( Exception e ) { e.printStackTrace(); }
      //      }

      if( header==null )  return;

      String tmp = path+Util.FS+Constante.FILE_METADATATXT;
      File ftmp = new File(tmp);
      if( ftmp.exists() ) ftmp.delete();
      FileOutputStream out = null;
      try {
         out = new FileOutputStream(ftmp);
         out.write(( header.getOriginalHeaderFits()).getBytes() );
      } finally {  if( out!=null ) out.close(); }

   }

   /** Création, ou mise à jour des fichiers meta associées au survey
    */
   protected void writeMetaFile() throws Exception {
      writePropertiesFile(null);
      
      // On en profite pour écrire le fichier index.html
      writeIndexHtml();

      // Et metadata.fits
      writeMetadataFits();

    }
   
   /** Création, ou mise à jour du fichier des Properties associées au survey
    * @param stream null pour l'écrire à l'emplacement prévu par défaut
    */
   protected void writePropertiesFile(OutputStreamWriter stream) throws Exception {


      // Ajout de l'IVORN si besoin
      if( hipsId==null ) setHipsId(null);

      // Ajout de l'order si besoin
      int order = getOrder();
      if( order==-1 ) order = cds.tools.pixtools.Util.getMaxOrderByPath( getOutputPath() );

      //      loadProperties();

      insertPropriete(Constante.KEY_CREATOR_DID,hipsId);
      
      // Y a-t-il un creator indiqué ?
      if( creator!=null ) setPropriete(Constante.KEY_CREATOR,creator);
      else setPropriete("#"+Constante.KEY_CREATOR,"HiPS creator (institute or person)");
      setPropriete("#"+Constante.KEY_HIPS_COPYRIGHT,"Copyright mention of the HiPS");
      
      if( addendum_id!=null ) setPropriete(Constante.KEY_ADDENDUM_ID,addendum_id);
      setPropriete(Constante.KEY_OBS_TITLE,getLabel());
      setPropriete("#"+Constante.KEY_OBS_COLLECTION,"Dataset collection name");
      setPropriete("#"+Constante.KEY_OBS_DESCRIPTION,"Dataset text description");
      setPropriete("#"+Constante.KEY_OBS_ACK,"Acknowledgement mention");
      setPropriete("#"+Constante.KEY_PROV_PROGENITOR,"Provenance of the original data (free text)");
      setPropriete("#"+Constante.KEY_BIB_REFERENCE,"Bibcode for bibliographic reference");
      setPropriete("#"+Constante.KEY_BIB_REFERENCE_URL,"URL to bibliographic reference");
      setPropriete("#"+Constante.KEY_OBS_COPYRIGHT,"Copyright mention of the original data");
      setPropriete("#"+Constante.KEY_OBS_COPYRIGHT_URL,"URL to copyright page of the original data");
      setPropriete("#"+Constante.KEY_T_MIN,"Start time in MJD ( =(Unixtime/86400)+40587  or https://heasarc.gsfc.nasa.gov/cgi-bin/Tools/xTime/xTime.pl)");
      setPropriete("#"+Constante.KEY_T_MAX,"Stop time in MJD");
      setPropriete("#"+Constante.KEY_OBS_REGIME,"Waveband keyword (Radio Infrared Optical UV X-ray Gamma-ray)");
      setPropriete("#"+Constante.KEY_EM_MIN,"Start in spectral coordinates in meters ( =2.998E8/freq in Hz, or =1.2398841929E-12*energy in MeV )");
      setPropriete("#"+Constante.KEY_EM_MAX,"Stop in spectral coordinates in meters");
      //      setPropriete("#"+Constante.KEY_CLIENT_CATEGORY,"ex: Image/Gas-lines/Halpha/VTSS");
      //      setPropriete("#"+Constante.KEY_CLIENT_SORT_KEY,"ex: 06-03-01");


      setPropriete(Constante.KEY_HIPS_BUILDER,"Aladin/HipsGen "+Aladin.VERSION);
      setPropriete(Constante.KEY_HIPS_VERSION, Constante.HIPS_VERSION);
      if( !notouch ) setPropriete(Constante.KEY_HIPS_RELEASE_DATE,getNow());

      setPropriete(Constante.KEY_HIPS_FRAME, getFrameName());
      setPropriete(Constante.KEY_HIPS_ORDER,order+"");
      setPropriete(Constante.KEY_HIPS_TILE_WIDTH,CDSHealpix.pow2( getTileOrder())+"");

      // L'url
      setPropriete("#"+Constante.KEY_HIPS_SERVICE_URL,"ex: http://yourHipsServer/"+label+"");
      setPropriete(Constante.KEY_HIPS_STATUS,"public master clonableOnce");
      
      // le status du HiPS : par defaut "public master clonableOnce"
      String pub = Constante.PUBLIC;
      String clone = Constante.CLONABLEONCE;
      if( status!=null ) {
         Tok tok = new Tok(status);
         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken().toLowerCase();
            if( s.equals(Constante.PRIVATE) ) pub = Constante.PRIVATE;
            else if( s.equals(Constante.UNCLONABLE) ) pub=Constante.UNCLONABLE;
            else if( s.equals(Constante.CLONABLE) ) pub=Constante.CLONABLE;
         }
      }
      setPropriete(Constante.KEY_HIPS_STATUS,pub+" "+Constante.MASTER+" "+clone);

      // Ajout des formats de tuiles supportés
      String fmt = getAvailableTileFormats();
      if( fmt.length()>0 ) setPropriete(Constante.KEY_HIPS_TILE_FORMAT,fmt);

      if( fmt.indexOf("fits")>=0) {
         if( bitpix!=-1 ) setPropriete(Constante.KEY_HIPS_PIXEL_BITPIX,bitpix+"");
      }
      //      setPropriete(Constante.KEY_HIPS_PROCESS_OVERLAY,
      //            isMap() ? "none" : mode==Mode.ADD ? "add" :
      //               fading ? "border_fading" : mixing ? "mean" : "first");
//      setPropriete(Constante.KEY_HIPS_PROCESS_HIERARCHY, jpegMethod.toString().toLowerCase());


      if( cut!=null ) {
         if( cut[0]!=0 || cut[1]!=0 ) {
            setPropriete(Constante.KEY_HIPS_PIXEL_CUT,  Util.myRound(bscale*cut[0]+bzero)+" "+Util.myRound(bscale*cut[1]+bzero));
         }
         if( cut[2]!=0 || cut[3]!=0 ) setPropriete(Constante.KEY_HIPS_DATA_RANGE,Util.myRound(bscale*cut[2]+bzero)+" "+Util.myRound(bscale*cut[3]+bzero));
      }

      // Ajout du target et du radius par défaut
      if( target!=null ) {
         int offset = target.indexOf(' ');
         setPropriete(Constante.KEY_HIPS_INITIAL_RA,  target.substring(0,offset));
         setPropriete(Constante.KEY_HIPS_INITIAL_DEC, target.substring(offset+1));
      }
      if( targetRadius!=null ) setPropriete(Constante.KEY_HIPS_INITIAL_FOV, targetRadius);

      // Resolution du hiPS
      double res = CDSHealpix.pixRes( CDSHealpix.pow2( order + getTileOrder()) );
      setPropriete(Constante.KEY_HIPS_PIXEL_SCALE, Util.myRound(res/3600.) );

      if( resolution!=null ) setPropriete(Constante.KEY_S_PIXEL_SCALE, resolution);

      // Pour le cas d'un Cube
      if( depth>1 ) {
         setPropriete(Constante.KEY_DATAPRODUCT_TYPE,"cube");
         setPropriete(Constante.KEY_CUBE_DEPTH,depth+"");
         setPropriete(Constante.KEY_CUBE_FIRSTFRAME,depth/2+"");

         if( isCubeCanal() ) {
            setPropriete(Constante.KEY_CUBE_CRPIX3,crpix3+"");
            setPropriete(Constante.KEY_CUBE_CRVAL3,crval3+"");
            setPropriete(Constante.KEY_CUBE_CDELT3,cdelt3+"");
            setPropriete(Constante.KEY_CUBE_BUNIT3,bunit3+"");
         }

         // Sinon c'est un HiPS image
      } else {
         setPropriete(Constante.KEY_DATAPRODUCT_TYPE,"image");
      }
      
      // En cas de HiPS pouvant être étendu
      setPropriete(Constante.KEY_DATAPRODUCT_SUBTYPE,live ? "live" : null);

      // Dans le cas d'un HiPS couleur
      if( isColor() ) {
         setPropriete(Constante.KEY_DATAPRODUCT_SUBTYPE, live ? "color live" : "color");
         if( redInfo!=null )   setPropriete(Constante.KEY_HIPS_RGB_RED,redInfo);
         if( greenInfo!=null ) setPropriete(Constante.KEY_HIPS_RGB_GREEN,greenInfo);
         if( blueInfo!=null )  setPropriete(Constante.KEY_HIPS_RGB_BLUE,blueInfo);
      }

      HealpixMoc m = moc!=null ? moc : mocIndex;
      double skyFraction = m==null ? 0 : m.getCoverage();
      if( skyFraction>0 ) {

         setPropriete(Constante.KEY_MOC_SKY_FRACTION, Util.myRound( skyFraction ) );

         long tileSizeFits = Math.abs(bitpix/8) * CDSHealpix.pow2( getTileOrder()) * CDSHealpix.pow2( getTileOrder()) + 2048L;
         long tileSizeJpeg = 70000;
         long tileSizePng = 100000;
         double coverage = m.getCoverage();
         long numberOfTiles =  CDSHealpix.pow2(order) *  CDSHealpix.pow2(order) * 12L;
         long fitsSize = (long)( ( tileSizeFits*numberOfTiles * 1.3 * coverage) )/1024L;
         long jpegSize = (long)( ( tileSizeJpeg*numberOfTiles * 1.3 * coverage) )/1024L;
         long pngSize = (long)( ( tileSizePng*numberOfTiles * 1.3 * coverage) )/1024L;
         long size = (fmt.indexOf("fits")>=0 ? fitsSize : 0)
               + (fmt.indexOf("jpeg")>=0 ? jpegSize : 0)
               + (fmt.indexOf("png")>=0 ? pngSize : 0)
               + 8;

         setPropriete(Constante.KEY_HIPS_ESTSIZE, size+"" );
      }

      // Mise en place effective des proprétés
      String k[] = new String[ keyAddProp==null ? 0 : keyAddProp.size() ];
      String v[] = new String[ k.length ];
      for( int i=0; i<k.length; i++ ) {
         k[i] = keyAddProp.get(i);
         v[i] = valueAddProp.get(i);
      }
      updateProperties(k,v,true,stream);

   }

   /** Ecriture du fichier des propriétés pour le HpxFinder */
   protected void writeHpxFinderProperties() throws Exception {

      // Ajout de l'IVORN si besoin
      if( hipsId==null ) setHipsId(null);

      MyProperties prop = new MyProperties();
      String label = getLabel()+"-meta";
      String finderHipxId = getHipsId()+"/meta";

//      int offset = finderHipxId.indexOf('/');
//      if( offset==-1 ) prop.setProperty(Constante.KEY_OBS_ID,finderHipxId);
//      else {
//         prop.setProperty(Constante.KEY_OBS_ID,finderHipxId.substring(offset+1));
//         prop.setProperty(Constante.KEY_PUBLISHER_ID,"ivo://"+finderHipxId.substring(0,offset));
//      }
      
      prop.setProperty(Constante.KEY_CREATOR_DID,finderHipxId);
      
//      obs_id               = /CDS/P/toto/meta
//            publisher_id         = ivo://ivo:
//            obs_collection       = toto-meta

      prop.setProperty(Constante.KEY_OBS_TITLE, label);
      prop.setProperty(Constante.KEY_DATAPRODUCT_TYPE, "meta");
      prop.setProperty(Constante.KEY_HIPS_FRAME, getFrameName());
      prop.setProperty(Constante.KEY_HIPS_ORDER, getOrder()+"");
      if( minOrder>3 ) prop.setProperty(Constante.KEY_HIPS_ORDER_MIN, minOrder+"");
      if( !notouch ) prop.setProperty(Constante.KEY_HIPS_RELEASE_DATE, getNow());
      prop.setProperty(Constante.KEY_HIPS_VERSION, Constante.HIPS_VERSION);
      prop.setProperty(Constante.KEY_HIPS_BUILDER, "Aladin/HipsGen "+Aladin.VERSION);

      // Gestion de la compatibilité
      // Pour compatibilité (A VIRER D'ICI UN OU DEUX ANS (2017?))
      while( prop.removeComment(FORCOMPATIBILITY) );
     prop.add("#",FORCOMPATIBILITY);
      prop.add(Constante.OLD_OBS_COLLECTION,label);
      prop.add(Constante.OLD_HIPS_FRAME, getFrameCode() );
      prop.add(Constante.OLD_HIPS_ORDER,prop.getProperty(Constante.KEY_HIPS_ORDER) );
//    prop.add(Constante.OLD_HIPS_ORDER,getOrder()+"" );
      if( minOrder>3 ) prop.add(Constante.OLD_HIPS_ORDER_MIN, minOrder+"");
      prop.add(Constante.KEY_HIPS_TILE_WIDTH,CDSHealpix.pow2( getTileOrder())+"");


      String propFile = getHpxFinderPath()+Util.FS+Constante.FILE_PROPERTIES;
      File f = new File(propFile);
      if( f.exists() ) f.delete();
      OutputStreamWriter out = null;
      try {
         out = new OutputStreamWriter( new FileOutputStream(f), "UTF-8");
         prop.store( out, null);
      } finally {  if( out!=null ) out.close(); }
   }

   // Retourne les types de tuiles déjà construites (en regardant l'existence de allsky.xxx associé)
   protected String getAvailableTileFormats() {
      String path = BuilderAllsky.getFileName(getOutputPath(),3,0);
      StringBuffer res = new StringBuffer();
      for( int i=0; i<Constante.TILE_EXTENSION.length; i++ ) {
         File f = new File(path+Constante.TILE_EXTENSION[i]);
         if( !f.exists() ) continue;
         if( res.length()>0 ) res.append(' ');
         res.append(Constante.TILE_MODE[i]);
      }
      return res.toString();
   }

   private void replaceKey(MyProperties prop, String oldKey, String key) {
      if( prop.getProperty(key)==null ) prop.replaceKey(oldKey,key);
   }

   private void replaceKeys(MyProperties prop) {
      replaceKey(prop,Constante.OLD_HIPS_PUBLISHER,Constante.KEY_CREATOR);
      replaceKey(prop,Constante.OLD_HIPS_BUILDER,Constante.KEY_HIPS_BUILDER);
      replaceKey(prop,Constante.OLD_OBS_COLLECTION,Constante.KEY_OBS_TITLE);
      replaceKey(prop,Constante.OLD_OBS_TITLE,Constante.KEY_OBS_TITLE);
      replaceKey(prop,Constante.OLD_OBS_DESCRIPTION,Constante.KEY_OBS_DESCRIPTION);
      replaceKey(prop,Constante.OLD1_OBS_DESCRIPTION,Constante.KEY_OBS_DESCRIPTION);
      replaceKey(prop,Constante.OLD_OBS_ACK,Constante.KEY_OBS_ACK);
      replaceKey(prop,Constante.OLD_OBS_COPYRIGHT,Constante.KEY_OBS_COPYRIGHT);
      replaceKey(prop,Constante.OLD_OBS_COPYRIGHT_URL,Constante.KEY_OBS_COPYRIGHT_URL);
      replaceKey(prop,Constante.OLD_CUBE_DEPTH,Constante.KEY_CUBE_DEPTH);
      replaceKey(prop,Constante.OLD_CUBE_FIRSTFRAME,Constante.KEY_CUBE_FIRSTFRAME);
      replaceKey(prop,Constante.OLD_HIPS_RELEASE_DATE,Constante.KEY_HIPS_RELEASE_DATE);
      replaceKey(prop,Constante.OLD_HIPS_DATA_RANGE,Constante.KEY_HIPS_DATA_RANGE);
      replaceKey(prop,Constante.OLD_HIPS_PIXEL_CUT,Constante.KEY_HIPS_PIXEL_CUT);
      replaceKey(prop,Constante.OLD_HIPS_ORDER,Constante.KEY_HIPS_ORDER);
      replaceKey(prop,Constante.OLD_HIPS_ORDER_MIN,Constante.KEY_HIPS_ORDER_MIN);
      replaceKey(prop,Constante.OLD_HIPS_TILE_FORMAT,Constante.KEY_HIPS_TILE_FORMAT);
      replaceKey(prop,Constante.OLD_HIPS_TILE_WIDTH,Constante.KEY_HIPS_TILE_WIDTH);
      replaceKey(prop,Constante.OLD_CLIENT_CATEGORY,Constante.KEY_CLIENT_CATEGORY);
      replaceKey(prop,Constante.OLD_HIPS_RGB_RED,Constante.KEY_HIPS_RGB_RED);
      replaceKey(prop,Constante.OLD_HIPS_RGB_GREEN,Constante.KEY_HIPS_RGB_GREEN);
      replaceKey(prop,Constante.OLD_HIPS_RGB_BLUE,Constante.KEY_HIPS_RGB_BLUE);

      String s;
      
      // Certains champs seront en plus convertis
      
      // On supprime toutes références au PUBLISHER, et on utilise le CREATOR
      if( prop.getProperty(Constante.KEY_CREATOR_DID)==null ) {
         s= prop.getProperty(Constante.KEY_PUBLISHER_DID);
         if( s!=null ) {
            prop.insert( Constante.KEY_CREATOR_DID, s);
         } else {
            s= prop.getProperty(Constante.KEY_CREATOR_ID);
            if( s==null ) s= prop.getProperty(Constante.KEY_PUBLISHER_ID);
            if( s==null ) s="ivo://UNK.AUT";
            String obs_id = prop.getProperty(Constante.KEY_OBS_ID);
            if( obs_id!=null ) {
               String creator_did = s+"?"+obs_id;
               prop.insert( Constante.KEY_CREATOR_DID, creator_did);
            }
         }
      }
      prop.remove(Constante.KEY_PUBLISHER_DID);
      prop.remove(Constante.KEY_PUBLISHER_ID);
      
//      s = prop.getProperty(Constante.KEY_OBS_ID);
//      if( s==null ) {
//         s = prop.getProperty(Constante.KEY_CREATOR_DID);
//         if( s!=null ) {
//            int index = s.indexOf("/",6);
//            if( index>0 ) {
//               prop.insert(Constante.KEY_CREATOR_DID, s.substring(0,index));
//               prop.insert(Constante.KEY_OBS_ID, s.substring(index+1));
//               prop.remove(Constante.KEY_PUBLISHER_ID);
//            }
//         }
//      }
      
      s = prop.getProperty(Constante.OLD_HIPS_CREATION_DATE);
      if( s!=null && prop.getProperty(Constante.KEY_HIPS_CREATION_DATE)==null) {
         try {
            String v = Constante.sdf.format( HipsGen.SDF.parse(s) )+"Z";
            prop.replaceKey(Constante.OLD_HIPS_CREATION_DATE, Constante.KEY_HIPS_CREATION_DATE);
            prop.replaceValue(Constante.KEY_HIPS_CREATION_DATE, v);
         } catch( ParseException e ) { }
      }
      s = prop.getProperty(Constante.OLD_HIPS_RELEASE_DATE);
      if( s!=null && prop.getProperty(Constante.KEY_HIPS_RELEASE_DATE)==null) {
         try {
            String v = Constante.sdf.format( HipsGen.SDF.parse(s) )+"Z";
            prop.replaceKey(Constante.OLD_HIPS_RELEASE_DATE, Constante.KEY_HIPS_RELEASE_DATE);
            if( !notouch ) prop.replaceValue(Constante.KEY_HIPS_RELEASE_DATE, v);
         } catch( ParseException e ) { }
      }

      s = prop.getProperty(Constante.OLD_HIPS_FRAME);
      if( s!=null && prop.getProperty(Constante.KEY_HIPS_FRAME)==null) {
         String v = getCanonicalFrameName(s);
         prop.setProperty(Constante.KEY_HIPS_FRAME,v);
      }
      s = prop.getProperty(Constante.OLD_TARGET);
      if( s!=null ) {
         int i = s.indexOf(' ');
         prop.setProperty(Constante.KEY_HIPS_INITIAL_RA,s.substring(0,i));
         prop.setProperty(Constante.KEY_HIPS_INITIAL_DEC,s.substring(i+1));
         prop.remove(Constante.OLD_TARGET);
      }
      s = prop.getProperty(Constante.OLD_HIPS_INITIAL_FOV);
      if( s!=null ) prop.replaceKey(Constante.OLD_HIPS_INITIAL_FOV,Constante.KEY_HIPS_INITIAL_FOV);

      // Certains champs sont remplacés sous une autre forme, à moins qu'ils n'aient été
      // déjà mis à jour
      s = prop.getProperty(Constante.OLD_ISCOLOR);
      if( s==null ) s = prop.getProperty("isColor");
      if( s!=null ) {
         if( s.equals("true")
               && prop.getProperty(Constante.KEY_DATAPRODUCT_SUBTYPE)==null) prop.setProperty(Constante.KEY_DATAPRODUCT_SUBTYPE, "color");
         //         prop.remove(Constante.OLD_ISCOLOR);
      }
      s = prop.getProperty(Constante.OLD_ISCAT);
      if( s!=null ) {
         if( s.equals("true")
               && prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE)==null) prop.setProperty(Constante.KEY_DATAPRODUCT_TYPE, "catalog");
         //         prop.remove(Constante.OLD_ISCAT);
      }
      s = prop.getProperty(Constante.OLD_ISCUBE);
      if( s!=null ) {
         if( s.equals("true")
               && prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE)==null) prop.setProperty(Constante.KEY_DATAPRODUCT_TYPE, "cube");
         //         prop.remove(Constante.OLD_ISCUBE);
      }

      prop.remove(Constante.OLD_ALADINVERSION);
      prop.remove("hips_glu_tag");
   }


   /** Mise à jour du fichier des propriétés associées au survey HEALPix (propertie file dans la racine)
    * Conserve les clés/valeurs existantes.
    * @param key liste des clés à mettre à jour
    * @param value liste des valuers associées
    * @param overwrite si false, ne peut modifier une clé/valeur déjà existante
    * @param stream null pour écriture à l'endroit par défaut
    * @throws Exception
    */
   protected void updateProperties(String[] key, String[] value,boolean overwrite) throws Exception {
      updateProperties(key,value,overwrite,null);
   }
   protected void updateProperties(String[] key, String[] value,boolean overwrite,OutputStreamWriter stream) throws Exception {

      waitingPropertieFile();
      try {
         String propFile = getOutputPath()+Util.FS+Constante.FILE_PROPERTIES;

         // Chargement des propriétés existantes
         prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            if( !f.canRead() ) throw new Exception("Propertie file not available ! ["+propFile+"]");
            InputStreamReader in = new InputStreamReader( new FileInputStream(propFile), "UTF-8" );
            prop.load(in);
            in.close();
         }

         // Changement éventuel de vocabulaire
         replaceKeys(prop);
         
         // S'il n'y a pas d'indication hips_initial... on les indique manu-militari
         String ra  = prop.get( Constante.KEY_HIPS_INITIAL_RA );
         String dec = prop.get( Constante.KEY_HIPS_INITIAL_DEC );
         String fov = prop.get( Constante.KEY_HIPS_INITIAL_FOV );
         if( ra==null || dec==null || fov==null ) {
            if( fov==null ) {

               // On va préférer prendre le moc_order indiqué dans les properties
               // pour éviter de récupérer le bug sur le MocOrder
               try {
                  int n = Integer.parseInt( prop.get("moc_order"));
                  HealpixMoc mm = new HealpixMoc();
                  mm.setMocOrder(n);
                  fov =  mm.getAngularRes()+"";
               } catch( Exception e) {
                  fov = moc.getAngularRes()+"";
               }
               prop.replaceValue( Constante.KEY_HIPS_INITIAL_FOV,fov);
            }
            if( ra==null || dec==null ) {
               Healpix hpx = new Healpix();
               if( moc.isAllSky() ) { ra="0"; dec="+0"; }
               else {
                  try {
                     int o = moc.getMocOrder();
                     long pix = moc.pixelIterator().next();
                     double coo[] = hpx.pix2ang(o,pix);
                     ra = coo[0]+"";
                     dec = coo[1]+"";
                  } catch( Exception e ) { }
               }
               prop.replaceValue( Constante.KEY_HIPS_INITIAL_RA,ra);
               prop.replaceValue( Constante.KEY_HIPS_INITIAL_DEC,dec);
            }
         }


         String v;
         // Mise à jour des propriétés
         for( int i=0; i<key.length; i++ ) {
 
            if( !notouch && key[i].equals(Constante.KEY_HIPS_RELEASE_DATE) ) {
               // Conservation de la première date de processing si nécessaire
               if( prop.getProperty(Constante.KEY_HIPS_CREATION_DATE)==null
                     && (v=prop.getProperty(Constante.KEY_HIPS_RELEASE_DATE))!=null) {
                  prop.setProperty(Constante.KEY_HIPS_CREATION_DATE, v);
               }
            }

            // Je n'ajoute une proposition de clé que si elle n'y est pas déjà
            if( key[i].charAt(0)=='#') {
               if( prop.getProperty(key[i].substring(1))!=null ) continue;
            }

            // insertion ou remplacement
            if( overwrite ) {
               if( value[i]==null ) prop.remove(key[i]);
               else if( value[i]!=null ) prop.setProperty(key[i], value[i]);

               // insertion que si nouveau
            } else {
               v = prop.getProperty(key[i]);
               if( v==null && value[i]!=null ) prop.setProperty(key[i], value[i]);
            }

            // Suppression d'une ancienne proposition de clé éventuelle
            if( value[i]!=null && key[i].charAt(0)!='#') {
               if( prop.getProperty("#"+key[i])!=null ) prop.remove("#"+key[i]);
            }
            
         }
         
         // Mémorisation des paramètres de générations
         if( scriptCommand!=null ) {
            int n=0;
            while( prop.getProperty("hipsgen_params"+(n==0?"":"_"+n))!=null) n++;
            prop.add("hipsgen_date"+(n==0?"":"_"+n),getNow());
            prop.add("hipsgen_params"+(n==0?"":"_"+n),scriptCommand);
         }
         
         // Gestion de la compatibilité
         // Pour compatibilité (A VIRER D'ICI UN OU DEUX ANS (2017?))
         while( prop.removeComment(FORCOMPATIBILITY) );
         prop.add("#",FORCOMPATIBILITY);
         prop.add(Constante.OLD_OBS_COLLECTION,getLabel());
         prop.add(Constante.OLD_HIPS_FRAME, getFrameCode() );
         prop.add(Constante.OLD_HIPS_ORDER,prop.getProperty(Constante.KEY_HIPS_ORDER) );
//         prop.add(Constante.OLD_HIPS_ORDER,getOrder()+"" );
         String fmt = getAvailableTileFormats();
         if( fmt.length()>0 ) prop.add(Constante.OLD_HIPS_TILE_FORMAT,fmt);
         if( fmt.indexOf("fits")>=0 && cut!=null ) {
            if( cut[0]!=0 || cut[1]!=0 ) prop.add(Constante.OLD_HIPS_PIXEL_CUT, Util.myRound(bscale*cut[0]+bzero)+" "+Util.myRound(bscale*cut[1]+bzero));
            if( cut[2]!=0 || cut[3]!=0 ) prop.add(Constante.OLD_HIPS_DATA_RANGE,Util.myRound(bscale*cut[2]+bzero)+" "+Util.myRound(bscale*cut[3]+bzero));
         }
         if( isColor() ) prop.add(Constante.OLD_ISCOLOR,"true");
         if( isCube() ) {
            prop.add(Constante.OLD_ISCUBE,"true");
            prop.add(Constante.OLD_CUBE_DEPTH,depth+"");
         }
         
         // Remplacement du précédent fichier
         if( stream!=null ) prop.store( stream, null);
         else {
            String tmp = getOutputPath()+Util.FS+Constante.FILE_PROPERTIES+".tmp";
            File ftmp = new File(tmp);
            if( ftmp.exists() ) ftmp.delete();
            File dir = new File( getOutputPath() );
            if( !dir.exists() && !dir.mkdir() ) throw new Exception("Cannot create output directory");
            OutputStreamWriter out = null;
            try {
               out = new OutputStreamWriter( new FileOutputStream(ftmp), "UTF-8");
               prop.store( out, null);


            } finally {  if( out!=null ) out.close(); }

            if( f.exists() && !f.delete() ) throw new Exception("Propertie file locked ! (cannot delete)");
            if( !ftmp.renameTo(new File(propFile)) ) throw new Exception("Propertie file locked ! (cannot rename)");
         }

      }
      finally { releasePropertieFile(); }
   }
   
   /** Lecture des propriétés */
   protected void loadProperties() throws Exception {
      waitingPropertieFile();
      try {
         String propFile = getOutputPath()+Util.FS+Constante.FILE_PROPERTIES;
         prop = new MyProperties();
         File f = new File( propFile );
         if( f.exists() ) {
            if( !f.canRead() ) throw new Exception("Propertie file not available ! ["+propFile+"]");
            InputStreamReader in = new InputStreamReader( new BufferedInputStream( new FileInputStream(propFile) ), "UTF-8");
            prop.load(in);
            in.close();

            // Changement éventuel de vocabulaire
            replaceKeys(prop);
         }
      }
      finally { releasePropertieFile(); }
   }

   // Gestion d'un lock pour accéder de manière exclusive aux fichiers des propriétés
   transient private boolean lock;
   private final Object lockObj= new Object();
   private void waitingPropertieFile() {
      while( !getLock() ) {
         try { Thread.currentThread().sleep(100); } catch( InterruptedException e ) {  }
      }
   }
   private void releasePropertieFile() { lock=false; }
   private boolean getLock() {
      synchronized( lockObj ) {
         if( lock ) return false;
         lock=true;
         return true;
      }
   }

   protected double[] gal2ICRSIfRequired(double al, double del) { return gal2ICRSIfRequired(new double[]{al,del}); }
   protected double[] gal2ICRSIfRequired(double [] aldel) {
      if( getFrame()==Localisation.ICRS ) return aldel;
      Astrocoo coo = (Astrocoo) COO_GAL.clone();
      coo.set(aldel[0],aldel[1]);
      coo.convertTo(AF_ICRS1);
      aldel[0] = coo.getLon();
      aldel[1] = coo.getLat();
      return aldel;
   }
   protected double[] ICRS2galIfRequired(double al, double del) { return ICRS2galIfRequired(new double[]{al,del}); }
   protected double[] ICRS2galIfRequired(double [] aldel) {
      if( getFrame()==Localisation.ICRS ) return aldel;
      Astrocoo coo = (Astrocoo) COO_EQU.clone();
      coo.set(aldel[0], aldel[1]);
      coo.convertTo(AF_GAL1);
      aldel[0] = coo.getLon();
      aldel[1] = coo.getLat();
      return aldel;
   }

   public int[] xy2hpx = null;
   public int[] hpx2xy = null;

   /** Méthode récursive utilisée par createHealpixOrder */
   private void fillUp(int[] npix, int nsize, int[] pos) {
      int size = nsize * nsize;
      int[][] fils = new int[4][size / 4];
      int[] nb = new int[4];
      for (int i = 0; i < size; i++) {
         int dg = (i % nsize) < (nsize / 2) ? 0 : 1;
         int bh = i < (size / 2) ? 1 : 0;
         int quad = (dg << 1) | bh;
         int j = pos == null ? i : pos[i];
         npix[j] = npix[j] << 2 | quad;
         fils[quad][nb[quad]++] = j;
      }
      if (size > 4)
         for (int i = 0; i < 4; i++)
            fillUp(npix, nsize / 2, fils[i]);
   }

   /** Creation des tableaux de correspondance indice Healpix <=> indice XY */
   public void createHealpixOrder(int order) {
      int nside = (int) CDSHealpix.pow2(order);
      if( xy2hpx!=null && xy2hpx.length == nside*nside ) return;  // déja fait
      xy2hpx = new int[nside * nside];
      hpx2xy = new int[nside * nside];
      fillUp(xy2hpx, nside, null);
      for (int i = 0; i < xy2hpx.length; i++) hpx2xy[xy2hpx[i]] = i;
   }

   /**
    * Retourne l'indice XY en fonction d'un indice Healpix => nécessité
    * d'initialiser au préalable avec createHealpixOrdre(int)
    */
   final public int xy2hpx(int hpxOffset) {
      return xy2hpx[hpxOffset];
   }

   /**
    * Retourne l'indice XY en fonction d'un indice Healpix => nécessité
    * d'initialiser au préalable avec createHealpixOrdre(int)
    */
   final public int hpx2xy(int xyOffset) {
      return hpx2xy[xyOffset];
   }

   /** Retourne le nombre d'octets disponibles en RAM */
   public long getMem() {
      return Runtime.getRuntime().maxMemory()-
            (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
   }
   
   private Tile2HPX tile2Hpx = null;
   
   protected void updateHeader(Fits fits,int order,long npix) {
      if( fits.headerFits==null ) return;
      if( creator!=null) fits.headerFits.setKeyValue("ORIGIN", creator);
      fits.headerFits.setKeyValue("CPYRIGHT", "See HiPS properties file");
      fits.headerFits.setKeyValue("COMMENT", "HiPS FITS tile generated by Aladin/Hipsgen "+Aladin.VERSION);
      fits.headerFits.setKeyValue("ORDER", ""+order);
      fits.headerFits.setKeyValue("NPIX", ""+npix);
      
      // Génération des mots clés WCS dans l'entête des tuiles (appel code FX)
      try {
         if( tile2Hpx==null ) {
            tile2Hpx = new Tile2HPX(order, fits.width, frame==Localisation.ICRS ? WCSFrame.EQU: 
                  frame==Localisation.ECLIPTIC ? WCSFrame.ECL : WCSFrame.GAL );
         }
         Map<String, String> map = tile2Hpx.toFitsHeader(npix);
         for(Map.Entry<String, String> e : map.entrySet()) {
            
            // Je vire les commentaires qui foutent le bouzin
            String key = e.getKey().trim();
            String val=e.getValue();
            int i=val.indexOf('/');
            if( i>0 ) val = val.substring(0, i).trim();
            
            fits.headerFits.setKeyValue(key, val);
         }
      } catch( Exception e ) {
         e.printStackTrace();
      }

   }

}
