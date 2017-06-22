// Copyright 1999-2017 - Universit� de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import cds.allsky.Constante;
import cds.allsky.Context;
import cds.moc.Healpix;
import cds.moc.HealpixMoc;
import cds.mocmulti.MocItem2;
import cds.tools.Util;

/**
 * G�re les noeuds de l'arbre des HiPS
 * @author Pierre Fernique [CDS]
 * @version 2.0 Janvier 2017 - d�sormais utilis� pour le HipsStore
 */
public class TreeObjDir extends TreeObj {

   public String internalId;    // Alternative � l'ID de l'identificateur GLU
   private String url;          // L'url ou le path du survey
   public String description;   // Courte description (une ligne max)
   public String verboseDescr;  // Description de l'application (1 paragraphe ou plus)
   public String ack;           // L'acknowledgement
   public String copyright;     // Mention l�gale du copyright
   public String copyrightUrl;  // Url pour renvoyer une page HTML d�crivant les droits
   public String hpxParam;      // Les param�tres propres � HEALPIX
   public String version="";    // Le num�ro de version du survey
   public String aladinProfile; // profile de l'enregistrement GLU (notamment "localdef")*
   private String skyFraction;  // Fraction du ciel (0..1)
   public String aladinLabel;
   public int minOrder=-1;      // Min order Healpix
   public int maxOrder=-1;      // Max order Healpix
   private boolean useCache=true;// Non utilisation du cache local
   private boolean cube=false;   // true si le survey est un cube
   private boolean color=false;  // true si le survey est en couleur
   private boolean inFits=false; // true si le survey est fourni en FITS
   private boolean inJPEG=false; // true si le survey est fourni en JPEG
   private boolean inPNG=false;  // true si le survey est fourni en PNG
   private boolean truePixels=false; // true si par d�faut le survey est fourni en truePixels (FITS)
   private boolean truePixelsSet=false; // true si le mode par d�faut du survey a �t� positionn� manuellement
   private boolean cat=false;    // true s'il s'agit d'un catalogue hi�rarchique
   private boolean progen=false; // true s'il s'agit d'un catalogue progen
   private boolean map=false;    // true s'il s'agit d'une map HEALPix FITS
   private boolean moc=false;    // true s'il faut tout de suite charger le MOC
   public int cubeDepth=-1;      // Profondeur du cube HiPs (-1 si inconnue)
   public int cubeFirstFrame=0;  // Premi�re frame � afficher (0 par d�faut)
   public int frame=Localisation.GAL;  // Frame d'indexation
   public Coord target=null;     // Target for starting display
   public double radius=-1;   // Field size for starting display
   public int nside=-1;          // Max NSIDE
   public boolean local=false;   // Il s'agit d'un survey sur disque local
   public Image imPreview=null;  // Image preview
   public boolean previewError=false; // true s'il y a eu un probl�me lors du chargement du preview
   
   protected MyProperties prop=null; // Ensemble des propri�t�s associ�es au HiPS (via son fichier de properties ou MocServer)

   /** Construction d'un TreeObjHips � partir des infos qu'il est possible de glaner
    * � l'endroit indiqu�, soit par exploration du r�pertoire, soit par le fichier Properties */
   public TreeObjDir(Aladin aladin,String pathOrUrl) throws Exception {
      String s;
      this.aladin = aladin;
      local=!(pathOrUrl.startsWith("http:") || pathOrUrl.startsWith("https:") ||pathOrUrl.startsWith("ftp:"));
      MyProperties prop = new MyProperties();

      // Par http ou ftp ?
      try {
         InputStreamReader in=null;
         if( !local ) in = new InputStreamReader( (new URL(pathOrUrl+"/"+Constante.FILE_PROPERTIES)).openStream(), "UTF-8" );
         else in = new InputStreamReader( new FileInputStream(new File(pathOrUrl+Util.FS+Constante.FILE_PROPERTIES)), "UTF-8" );
         try { prop.load(in); } finally { in.close(); }
      } catch( Exception e ) { aladin.trace(3,"No properties file found => auto discovery..."); }


      // recherche du frame Healpix (ancienne & nouvelle syntaxe)
      String strFrame = prop.getProperty(Constante.KEY_HIPS_FRAME);
      if( strFrame==null  ) strFrame = prop.getProperty(Constante.OLD_HIPS_FRAME);
      if( strFrame==null  ) strFrame = "galactic";
      if( strFrame.equals("equatorial") || strFrame.equals("C") || strFrame.equals("Q") ) frame=Localisation.ICRS;
      else if( strFrame.equals("ecliptic") || strFrame.equals("E") ) frame=Localisation.ECLIPTIC;
      else if( strFrame.equals("galactic") || strFrame.equals("G") ) frame=Localisation.GAL;

      url=pathOrUrl;

      s = prop.getProperty(Constante.KEY_OBS_COLLECTION);
      if( s==null ) s = prop.getProperty(Constante.OLD_OBS_COLLECTION);
      if( s!=null ) label=s;
      else {
         char c = local?Util.FS.charAt(0):'/';
         int end = pathOrUrl.length();
         int offset = pathOrUrl.lastIndexOf(c);
         if( offset==end-1 && offset>0 ) { end=offset; offset = pathOrUrl.lastIndexOf(c,end-1); }
         label = pathOrUrl.substring(offset+1,end);
      }
      id="__"+label;

      s = prop.getProperty(Constante.OLD_VERSION);
      if( s!=null ) version=s;

      description = prop.getProperty(Constante.KEY_OBS_TITLE);
      if( description==null ) description = prop.getProperty(Constante.OLD_OBS_TITLE);
      verboseDescr = prop.getProperty(Constante.KEY_OBS_DESCRIPTION);
      if( verboseDescr==null ) verboseDescr = prop.getProperty(Constante.OLD_OBS_DESCRIPTION);
      copyright = prop.getProperty(Constante.KEY_OBS_COPYRIGHT);
      if( copyright==null ) copyright = prop.getProperty(Constante.OLD_OBS_COPYRIGHT);
      copyrightUrl = prop.getProperty(Constante.KEY_OBS_COPYRIGHT_URL);
      if( copyrightUrl==null ) copyrightUrl = prop.getProperty(Constante.OLD_OBS_COPYRIGHT_URL);
      useCache = !local && Boolean.parseBoolean( prop.getProperty(Constante.OLD_USECACHE,"True") );
      skyFraction = prop.getProperty(Constante.KEY_MOC_SKY_FRACTION);

      s = prop.getProperty(Constante.KEY_HIPS_INITIAL_RA);
      if( s!=null) {
         String s1 = prop.getProperty(Constante.KEY_HIPS_INITIAL_DEC);
         if( s1!=null ) s = s+" "+s1;
         else s=null;
      }

      // Pour supporter l'ancien vocabulaire
      if( s==null )  s = prop.getProperty(Constante.OLD_TARGET);

      if( s==null ) target=null;
      else {
         try { target = new Coord(s); }
         catch( Exception e) { aladin.trace(3,"target error!"); target=null; }
      }
      double div2=2;
      s = prop.getProperty(Constante.KEY_HIPS_INITIAL_FOV);
      if( s==null ) { s = prop.getProperty(Constante.OLD_HIPS_INITIAL_FOV); div2=1; }
      if( s==null ) radius=-1;
      else {
         try { radius=(Server.getAngleInArcmin(s, Server.RADIUSd)/60.)/div2; }
         catch( Exception e) { aladin.trace(3,"radius error!"); radius=-1; }
      }

      s = prop.getProperty(Constante.KEY_HIPS_TILE_WIDTH);
      if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_TILE_WIDTH);
      if( s!=null ) try { nside = Integer.parseInt(s); } catch( Exception e) {
         aladin.trace(3,"NSIDE number not parsable !");
         nside=-1;
      }

      s = prop.getProperty(Constante.KEY_HIPS_ORDER);
      if( s==null ) s = prop.getProperty(Constante.OLD_HIPS_ORDER);
      try { maxOrder = new Integer(s); }
      catch( Exception e ) {
         maxOrder = getMaxOrderByPath(pathOrUrl,local);
         
         if( maxOrder==-1 ) throw new Exception("Not an HiPS");
         if( maxOrder==-1 ) {
            aladin.trace(3,"No maxOrder found (even with scanning dir.) => assuming 11");
            maxOrder=11;
         }
      }

      // Les param�tres li�s aux cubes
      String s1 = prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE);
      if( s1!=null ) cube = s1.indexOf("cube")>=0;

      // Pour compatibilit� avec l'ancien vocabulaire
      else {
         try { cube = new Boolean(prop.getProperty(Constante.OLD_ISCUBE)); }
         catch( Exception e ) { cube=false; }
      }
      if( cube ) {
         s = prop.getProperty(Constante.KEY_CUBE_DEPTH);
         if( s==null ) s = prop.getProperty(Constante.OLD_CUBE_DEPTH);
         if( s!=null ) {
            try { cubeDepth = Integer.parseInt(s); }
            catch( Exception e ) {
               aladin.trace(3,"CubeDepth syntax error ["+s+"] => trying autodetection");
               cubeDepth=-1;
            }
         }
         s = prop.getProperty(Constante.KEY_CUBE_FIRSTFRAME);
         if( s==null ) s = prop.getProperty(Constante.OLD_CUBE_FIRSTFRAME);
         if( s!=null ) {
            try { cubeFirstFrame = Integer.parseInt(s); }
            catch( Exception e ) {
               aladin.trace(3,"cubeFirstFrame syntax error ["+s+"] => assuming frame 0");
               cubeFirstFrame=-1;
            }
         }
      }

      progen = pathOrUrl.endsWith("HpxFinder") || pathOrUrl.endsWith("HpxFinder/");

      s = prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE);
      if( s!=null) {
         cat = s.indexOf("catalog")>=0;

         // Pour compatibilit� avec l'ancien vocabulaire
      } else {
         s = prop.getProperty(Constante.OLD_ISCAT);
         if( s!=null ) cat = new Boolean(s);
         else cat = getFormatByPath(pathOrUrl,local,2);
      }

      // D�termination du format des cellules dans le cas d'un survey pixels
      String keyColor = prop.getProperty(Constante.KEY_DATAPRODUCT_SUBTYPE);
      if( keyColor!=null ) color = keyColor.indexOf("color")>=0;

      // Pour compatibilit� avec l'ancien vocabulaire
      else {
         keyColor = prop.getProperty(Constante.OLD_ISCOLOR);
         if( keyColor==null ) keyColor = prop.getProperty("isColor");
         if( keyColor!=null ) color = new Boolean(keyColor);
      }

      if( !cat && !progen /* && (keyColor==null || !color)*/ ) {
         String format = prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
         if( format==null ) format = prop.getProperty(Constante.OLD_HIPS_TILE_FORMAT);
         if( format!=null ) {
            int a,b;
            inFits = (a=Util.indexOfIgnoreCase(format, "fit"))>=0;
            inJPEG = (b=Util.indexOfIgnoreCase(format, "jpeg"))>=0
                  || (b=Util.indexOfIgnoreCase(format, "jpg"))>=0;
                  inPNG  = (b=Util.indexOfIgnoreCase(format, "png"))>=0;
                  truePixels = inFits && a<b;                         // On d�marre dans le premier format indiqu�
         } else {
            inFits = getFormatByPath(pathOrUrl,local,0);
            inJPEG = getFormatByPath(pathOrUrl,local,1);
            inPNG  = getFormatByPath(pathOrUrl,local,3);
            truePixels = local && inFits || !(!local && (inJPEG || inPNG));   // par d�faut on d�marre en FITS en local, en Jpeg en distant
         }
         if( keyColor==null ) {
            color = getIsColorByPath(pathOrUrl,local);
         }
         if( color ) truePixels=false;
      }
      
      if( color && !inJPEG && !inPNG) inJPEG=true;

      aladin.trace(4,toString1());

   }

   /** G�n�ration d'un label � partir de l'ID ex: CDS/P/DSS2/color => DSS2 color */
   private String createLabel(String id,boolean cat) {
      String label=id;
      int p1 = id.indexOf('/');
      int p2 = id.indexOf('/',p1+1);
      if( cat ) { if( p1>0 ) label = id.substring(p1+1); }
      else if( p2>0 ) label = id.substring(p2+1);
      return label.replace('/',' ');
   }

   /** Cr�ation � partir d'un fichier de properties (ne supporte que HiPS 1.3 car d�di�
    * principalement � des enregistrements issues du MocServer */
   public TreeObjDir(Aladin aladin, String id, MyProperties prop) {
      String s;

      this.aladin = aladin;
      this.id = internalId = id;
//      this.local = isLocal;
      this.prop = prop;
      
      // Type de Collection
      s=prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE);
      if( s!=null ) {
         if( s.indexOf("catalog")>=0 ) cat=true;
         else if( s.indexOf("cube")>=0 ) cube=true;
         else if( s.indexOf("progen")>=0 ) progen=true;
      } else {
         cat = prop.getProperty("cs_service_url")!=null;
      }

      // R�f�rence spatiale
      s=prop.getProperty(Constante.KEY_HIPS_FRAME);
      if( s!=null ) frame = Context.getFrameVal(s);
      else frame = cat ? Localisation.ICRS : Localisation.GAL;

      // le label est construit � partir de l'ID
//      aladinLabel = label = createLabel(id,cat);
      
      // ou le label construit � partir du obs_title et/ou obs_collection
//      s=prop.getProperty( cat ? Constante.KEY_OBS_TITLE : Constante.KEY_OBS_COLLECTION );
//      if( s==null ) s=prop.getProperty( !cat ? Constante.KEY_OBS_TITLE : Constante.KEY_OBS_COLLECTION );
      s=prop.getProperty( Constante.KEY_OBS_TITLE );
      if( s==null ) s=prop.getProperty( Constante.KEY_OBS_COLLECTION );
      aladinLabel = label = s!=null ? s : createLabel(id,cat);
      
      // Le path de l'arbre
      s=prop.getProperty(Constante.KEY_CLIENT_CATEGORY);
      if( s==null ) s="Others";
      path = s+"/"+label.replace("/","\\/");
      
      // l'ordre de tri
      ordre=prop.getProperty(Constante.KEY_CLIENT_SORT_KEY);
      if( ordre==null ) ordre="Z";

      // Divers champs de descriptions
      description = prop.getProperty(Constante.KEY_OBS_TITLE);
      verboseDescr = prop.getProperty(Constante.KEY_OBS_DESCRIPTION);
      copyright = prop.getProperty(Constante.KEY_OBS_COPYRIGHT);
      copyrightUrl = prop.getProperty(Constante.KEY_OBS_COPYRIGHT_URL);
      skyFraction = prop.getProperty(Constante.KEY_MOC_SKY_FRACTION);

      // Le champ initial
      s = prop.getProperty(Constante.KEY_HIPS_INITIAL_RA);
      if( s!=null) {
         String s1 = prop.getProperty(Constante.KEY_HIPS_INITIAL_DEC);
         if( s1!=null ) s = s+" "+s1;
         else s=null;
      }
      if( s==null ) target=null;
      else {
         try { target = new Coord(s); }
         catch( Exception e) { aladin.trace(3,"target error!"); target=null; }
      }
      double div2=2;
      s = prop.getProperty(Constante.KEY_HIPS_INITIAL_FOV);
      if( s==null ) radius=-1;
      else {
         try { radius=(Server.getAngleInArcmin(s, Server.RADIUSd)/60.)/div2; }
         catch( Exception e) { aladin.trace(3,"radius error!"); radius=-1; }
      }

      // La taille de la tuile Hips
      s = prop.getProperty(Constante.KEY_HIPS_TILE_WIDTH);
      if( s!=null ) try { nside = Integer.parseInt(s); } catch( Exception e) {
         aladin.trace(3,"NSIDE number not parsable !");
         nside=-1;
      }

      // Le maxOrder
      s = prop.getProperty(Constante.KEY_HIPS_ORDER);
      try { maxOrder = new Integer(s); }
      catch( Exception e ) {
         maxOrder=11;
      }

      // Les param�tres li�s aux cubes
      if( cube ) {
         s = prop.getProperty(Constante.KEY_CUBE_DEPTH);
         if( s!=null ) {
            try { cubeDepth = Integer.parseInt(s); }
            catch( Exception e ) { cubeDepth=-1; }
         }
         s = prop.getProperty(Constante.KEY_CUBE_FIRSTFRAME);
         if( s!=null ) {
            try { cubeFirstFrame = Integer.parseInt(s); }
            catch( Exception e ) { cubeFirstFrame=-1; }
         }
      }

      // D�termination du format des cellules dans le cas d'un survey pixels
      s = prop.getProperty(Constante.KEY_DATAPRODUCT_SUBTYPE);
      if( s==null ) {
         s = prop.getProperty(Constante.OLD_ISCOLOR);
         if( s==null ) s = prop.getProperty("isColor");
         if( s!=null ) s="color";
      }
      if( s!=null ) color = s.indexOf("color")>=0;

      if( !cat && !progen ) {
         String format = prop.getProperty(Constante.KEY_HIPS_TILE_FORMAT);
         if( format!=null ) {
            int a,b;
            inFits = (a=Util.indexOfIgnoreCase(format, "fit"))>=0;
            inJPEG = (b=Util.indexOfIgnoreCase(format, "jpeg"))>=0
                  || (b=Util.indexOfIgnoreCase(format, "jpg"))>=0;
                  inPNG  = (b=Util.indexOfIgnoreCase(format, "png"))>=0;
                  truePixels = inFits && a<b;                         // On d�marre dans le premier format indiqu�
         }
         if( color ) truePixels=false;
      }

      if( color && !inJPEG && !inPNG) inJPEG=true;
//      aladin.trace(4,toString1());
   }

   /** Cr�ation � partir d'un enregistrement GLU */
   public TreeObjDir(Aladin aladin,String actionName,String id,String aladinMenuNumber, String url,String aladinLabel,
         String description,String verboseDescr,String ack,String aladinProfile,String copyright,String copyrightUrl,String path,
         String aladinHpxParam,String skyFraction) {
      super(aladin,actionName,aladinMenuNumber,aladinLabel,path);
      this.aladinLabel  = aladinLabel;
      this.url          = url;
      this.description  = description;
      this.verboseDescr = verboseDescr;
      this.ack          = ack;
      this.copyright    = copyright;
      this.copyrightUrl = copyrightUrl;
      this.hpxParam     = aladinHpxParam;
      this.aladinProfile= aladinProfile;
      this.internalId   = id;
      this.skyFraction  = skyFraction;

      if( this.url!=null ) {
         char c = this.url.charAt(this.url.length()-1);
         if( c=='/' || c=='\\' ) this.url = this.url.substring(0,this.url.length()-1);
      }

      // Parsing des param�tres Healpix
      // ex: 3 8 nocache
      boolean first=true;
      if( hpxParam!=null ) {
         StringTokenizer st = new StringTokenizer(hpxParam);
         try {
            String s;
            while( st.hasMoreTokens() ) {
               s = st.nextToken();

               // test minOrder maxOrder (si un seul nombre => maxOrder);
               try {
                  int n = Integer.parseInt(s);
                  if( maxOrder!=-1 ) { minOrder=maxOrder; maxOrder=n; }
                  else maxOrder=n;
               } catch( Exception e ) {}

               if( Util.indexOfIgnoreCase(s, "nocache")>=0 ) useCache=false;
               if( Util.indexOfIgnoreCase(s, "color")>=0 ) color=true;
               if( Util.indexOfIgnoreCase(s, "cube")>=0 ) cube=true;
               if( Util.indexOfIgnoreCase(s, "fits")>=0 ) { inFits=true; if( first ) { first=false ; truePixels=true; } }
               if( Util.indexOfIgnoreCase(s, "jpeg")>=0
                     || Util.indexOfIgnoreCase(s, "jpg")>=0) { inJPEG=true; if( first ) { first=false ; truePixels=false;} }
               if( Util.indexOfIgnoreCase(s, "png")>=0 )  { inPNG=true; if( first ) { first=false ; truePixels=false;} }
               if( Util.indexOfIgnoreCase(s, "gal")>=0 ) frame = Localisation.GAL;
               if( Util.indexOfIgnoreCase(s, "ecl")>=0 ) frame = Localisation.ECLIPTIC;
               if( Util.indexOfIgnoreCase(s, "equ")>=0 ) frame = Localisation.ICRS;
               if( Util.indexOfIgnoreCase(s, "cat")>=0 ) cat=true;
               if( Util.indexOfIgnoreCase(s, "progen")>=0 ) progen=true;
               if( Util.indexOfIgnoreCase(s, "map")>=0 ) map=true;
               if( Util.indexOfIgnoreCase(s, "moc")>=0 ) moc=true;


               // Un num�ro de version du genre "v1.23" ?
               if( s.charAt(0)=='v' ) {
                  try {
                     double n = Double.parseDouble(s.substring(1));
                     version = "-"+s;
                  } catch( Exception e ) {}
               }
            }
            if( minOrder==-1 ) minOrder=2;
            if( maxOrder==-1 ) maxOrder=8;
         } catch( Exception e ) {}
      }

      // dans le cas d'un r�pertoire local => pas d'utilisateur du cache
      if( url!=null && !url.startsWith("http") && !url.startsWith("ftp") ) useCache=false;

      if( color && !inJPEG && !inPNG ) inJPEG=true;
      
      if( !Aladin.BETA ) {
         if( copyright!=null || copyrightUrl!=null ) setCopyright(copyright);
         setMoc();
      }
   }
   
   protected boolean isHiPS() {
      if( prop==null ) return true;   // S'il n'y a pas de prop, �a ne peut �tre qu'un HiPS
      return prop.getProperty(Constante.KEY_HIPS_SERVICE_URL)!=null;
   }
   
   /** Retourne l'URL d'acc�s aux prog�nitors, null sinon */
   protected String getProgenitorsUrl() {
      if( prop==null ) return null;
      return prop.getProperty(Constante.KEY_HIPS_PROGENITOR_URL);
   }
   
   /** Retourne l'URL d'acc�s au MOC, null sinon */
   protected String getMocUrl() {
      String u=null;
      
      // On dispose du MOC directement dans le MOC server qui a fourni les propri�t�s ?
      if( hasMocByMocServer() ) {
         String params = internalId+"&get=moc";
         u = aladin.glu.getURL("MocServer",params,true).toString();
         
      // Serait-il explicitement mentionn� ?
      } else if( prop!=null ) u = prop.getProperty("moc_access_url");
      
      // Pour un HiPS on peut y acc�der directement
      else if( isHiPS() ) return getUrl()+"/Moc.fits";
      
      return u;
   }
   
   protected String getProperty(String key) {
      if( prop==null ) return null;
      return prop.getProperty(key);
   }
   
   private boolean getIsColorByPath(String path,boolean local) {
      String ext = inPNG ? ".png" : ".jpg";
      MyInputStream in = null;
      try {
         if( local ) return Util.isJPEGColored(path+Util.FS+"Norder3"+Util.FS+"Allsky"+ext);
         in = new MyInputStream( Util.openStream(path+"/Norder3/Allsky"+ext) );
         byte [] buf = in.readFully();
         return Util.isColoredImage(buf);
      } catch( Exception e) {
         aladin.trace(3,"Allsky"+ext+" not found => assume B&W survey");
         return false;
      }
      finally { try { if( in!=null ) in.close(); } catch( Exception e1 ) {} }
   }

   private boolean getFormatByPath(String path,boolean local,int fmt) {
      String ext = fmt==0 ? ".fits" : fmt==1 ? ".jpg" : fmt==3 ? ".png" : ".xml";
      return local && (new File(path+Util.FS+"Norder3"+Util.FS+"Allsky"+ext)).exists() ||
            !local && Util.isUrlResponding(path+"/Norder3/Allsky"+ext);
   }

   private int getMaxOrderByPath(String urlOrPath,boolean local) {
      for( int n=25; n>=1; n--) {
         if( local && new File(urlOrPath+Util.FS+"Norder"+n).isDirectory()
               || !local && Util.isUrlResponding(urlOrPath+"/Norder"+n)) return n;
      }
      return -1;

      //      int maxOrder=-1;
      //      for( int n=3; n<100; n++ ) {
      //         if( local && !(new File(urlOrPath+Util.FS+"Norder"+n).isDirectory()) ||
      //            !local && !Util.isUrlResponding(urlOrPath+"/Norder"+n)) break;
      //         maxOrder=n;
      //      }
      //      return maxOrder;
   }
   
   protected JPanel createPanel() {
      
      if( !Aladin.BETA ) return super.createPanel(); 
      
      JLabel lab = new JLabel(label);
      lab.setBackground( background );
      
      gc = new GridBagConstraints();
      gc.fill = GridBagConstraints.VERTICAL;
      gc.anchor = GridBagConstraints.CENTER;
      gc.gridx = GridBagConstraints.RELATIVE;
      //      gc.insets = new Insets(2,0,4,5);
      gc.insets = new Insets(0,0,0,5);
      gb = new GridBagLayout();
      JPanel panel = new JPanel(gb);
//      panel.setOpaque(true);
      panel.setBackground( background );
      gb.setConstraints(lab,gc);
      panel.add(lab);
      return panel;
   }

   public String toString1() {
      double r;
      Coord c;
      return "GluSky ["+id+"]"
      +(isCatalog() ?" catalog" : isProgen() ?" progen" :isMap()?" fitsMap":" survey")
      +" maxOrder:"+getMaxOrder()
      +(getLosangeOrder()>=0?" cellOrder:"+getLosangeOrder():"")
      +(!isCatalog() && isColored() ?" colored" : " B&W")
      +(!isCube() ? "" : " cube"+(cubeDepth==-1 ? "" : "/"+cubeDepth+(cubeFirstFrame==0?"":"/"+cubeFirstFrame)))
      +(!isFits() ? "" : isTruePixels() ?" *inFits*" : " inFits")
      +(!isJPEG() ? "" : isTruePixels() ?" inJPEG" : " *inJPEG*")
      +(!isPNG()  ? "" : isTruePixels() ?" inPNG"  : " *inPNG*")
      +(loadMocNow() ? " withMoc" : "")
      +(useCache() ? " cache" : " nocache")
      +" "+Localisation.getFrameName(getFrame())
      +(isLocalDef() ? " localDef":"")
      +(isLocal() ? " local" : "")
      +((c=getTarget())!=null?" target:"+c:"")
      +((r=getRadius())!=-1?"/"+Coord.getUnit(r):"")
      +" \""+label+"\" => "+getUrl();
   }

   /** retourne true si cette d�finition doit �tre sauvegard�e dans le dico GLU local */
   protected boolean isLocalDef() { return aladinProfile!=null && aladinProfile.indexOf("localdef")>=0; }

   /** Retourne true si la description GLU correspond � un fichier Map healpix*/
   protected boolean isMap() { return map; }

   /** Retourne true s'il s'agit d'un catalogue hi�rarchique */
   protected boolean isCatalog() { return cat; }

   /** Retourne true s'il s'agit d'un catalogue hi�rarchique */
   protected boolean isCDSCatalog() { return cat && internalId.startsWith("CDS/"); }
   
   /** Retourne true s'il existe un acc�s SIA  ou SIA2 (par URL directe ou via GLU tag */
   protected boolean hasSIA() { return hasSIAv1() || hasSIAv2(); }
   
   /** Retourne true s'il existe un acc�s SIA v1 (par URL directe ou via GLU tag */
   protected boolean hasSIAv1() {
      return prop!=null && (prop.get("sia_service_url")!=null || prop.get("sia_glutag")!=null );
   }
   
   /** Retourne true s'il existe un acc�s SIA v2 (par URL directe ou via GLU tag */
   protected boolean hasSIAv2() {
      return prop!=null && (prop.get("sia2_service_url")!=null || prop.get("sia2_glutag")!=null);
   }
   
   /** Retourne true s'il existe un acc�s SSA (par URL direct ou via GLU tag */
   protected boolean hasSSA() {
      return prop!=null && (prop.get("ssa_service_url")!=null || prop.get("ssa_glutag")!=null);
   }
   
   /** Retourne true si la collection dispose d'un HiPS */
   protected boolean hasHips() { return prop!=null && 
         (prop.get("hips_service_url")!=null || prop.get("hips_service_path")!=null); }
   
   /** Retourne true si la collection dispose d'un acc�s TAP */
   protected boolean hasTAP() {
      if( prop==null ) return false;
      return prop.get("tap_glutag")!=null || prop.get("tap_service_url")!=null;
   }
   
   /** Retourne true si la collection dispose d'un acc�s cone search */
   protected boolean hasCS() {
      return isCDSCatalog() || prop!=null && prop.get("cs_service_url")!=null;
   }
   
   /** Retourne true si la collection dispose d'une URL donnant acc�s � un preview */
   protected boolean hasPreview() {
      return (isCDSCatalog() || isHiPS());
   }
   
   /** Retourne l'URL du preview
    * ex: http://alasky.u-strasbg.fr/footprints/tables/vizier/B_denis_denis/densityMap?format=png&size=small
    */
   protected String getPreviewUrl() {
      if( isCDSCatalog() ) {
         String s;
         if( internalId.startsWith("CDS/Simbad") ) s="simbad";
         else {
            s = Util.getSubpath(internalId, 1, -1);
            s = "vizier/"+ s.replace("/","_");
         }
//         return "http://alasky.u-strasbg.fr/footprints/tables/"+s+"/densityMap?format=png&size=small";
         return aladin.glu.getURL("Vignettes.CDS", s)+"";
      }
      
      if( isHiPS() ) return getUrl()+"/preview.jpg";
      
      return null;
   }
   
   /** Retourne true si la collection dispose d'une URL donnant des infos suppl�mentaires */
   protected boolean hasInfo() {
      return prop!=null && (prop.get("web_access_url")!=null || prop.get("obs_description_url")!=null);
   }
   
   /** Retourne true si la collection a �t� cr��e/maj r�cemment */
   protected boolean isNew() {
      return isNewHips() || isNewObsRelease();
   }
   
   /** Retourne true si la collection (hors HiPS) a �t� cr��e/maj r�cemment */
   protected boolean isNewObsRelease() {
      if( prop==null ) return false;
      return lastDays( prop.getProperty("obs_release_date"),90 );
   }
   
   /** Retourne true si le HiPS associ�e � la collection a �t� cr��/maj r�cemment */
   protected boolean isNewHips() {
      if( prop==null ) return false;
      String date = prop.getProperty("hips_creation_date");
      if( date==null ) date = prop.getProperty("hips_release_date");
      return lastDays(date,90);
   }
   
   private boolean lastDays(String date,int days ) {
      if( date==null ) return false;
      try {
         long t = Util.getTimeFromISO(date);
         long now = System.currentTimeMillis();
         return now-t < 86400L*days*1000L;    
      } catch( Exception e ) {}
      return false;
   }

   
   /** Retourne true si la collection dispose d'un MOC */
   protected  boolean hasMoc() {
      return hasMocByMocServer() || prop!=null && prop.getProperty("moc_access_url")!=null || isHiPS();
   }
   
   /** Retourne true si la collection dispose d'un MOC via le MocServer
    * (=> celui-ci ayant ajout� le mot cl� moc_sky_fraction) */
   private boolean hasMocByMocServer() { return prop!=null && prop.get("moc_sky_fraction")!=null; }
   
   /** Retourne l'URL d'un Cone search ou null si aucun */
   protected String getCSUrl() {
      if( prop==null ) return null;
      String u = prop.get("cs_service_url");
      if( u!=null && !(u.endsWith("?") || u.endsWith("&")) ) u+='?';
      return u;
   }
   
   /** Retourne l'URL donnant des informations suppl�mentaires sur la collection */
   protected String getInfoUrl() {
      if( prop==null ) return null;
      String url = prop.get("obs_description_url");
      if( url==null ) url = prop.get("web_access_url");
      return url;
   }

   /** Retourne true s'il s'agit d'un catalogue hi�rarchique pour des prog�niteurs */
   protected boolean isProgen() { return progen; }

   /** Retourne true s'il s'agit d'un survey ou d'une map couleur (par d�faut JPG) */
   protected boolean isColored() { return color; }

   /** Retourne true s'il s'agit d'un HiPS cube */
   protected boolean isCube() { return cube; }

   protected int getFrame() { return frame; }

   /** Retourne true s'il s'agit d'un survey fournissante les losanges en FITS => true pixel */
   protected boolean isFits() { return inFits; }

   protected int getMaxOrder() { return maxOrder; }

   /** Retourne le target par d�faut (premier affichage)  sous la forme J2000 d�cimal, null sinon */
   protected Coord getTarget() { return target; }

   /** Retourne le rayon du champ par d�faut (premier affichage) en degr�s, -1 sinon */
   protected double getRadius() { return radius; }

   /** Retourne le num�ro de version du survey, "" si non d�fini */
   protected String getVersion() { return version==null ? "" : version; }

   protected int getLosangeOrder() {
      if( progen || cat || nside==-1 /*|| maxOrder==-1 */) return -1;
      return (int)Healpix.log2(nside) /*- maxOrder*/;
   }
   
   /** true si d�j� charg� dans la pile */
   protected Color isInStack() { return aladin.calque.isLoaded(internalId); }

   protected boolean isLocal() { return local; }

   protected boolean loadMocNow() { return moc; }

   /** Retourne true s'il s'agit d'un survey fournissante les losanges en JPEG
    * => 8 bits pixel + compression avec perte */
   protected boolean isJPEG() { return inJPEG; }

   /** Retourne true s'il s'agit d'un survey fournissante les losanges en PNG
    * => 8 bits pixel + compression sans perte + transparence */
   protected boolean isPNG() { return inPNG; }

   /** Retourne true si par d�faut le survey est fourni en true pixels (FITS)  */
   protected boolean isTruePixels() {
      if( truePixelsSet ) return truePixels;
      return !isColored() && (inFits && local || !(inJPEG || inPNG) && !local);
   }

   /** Retourne true si le survey utilise le cache local */
   protected boolean useCache() { return useCache; }

   /** retourne l'URL de base pour acc�der au serveur HTTP */
   protected String getUrl() {
      
      // HiPS local ?
      if( prop!=null ) {
         String path = prop.getProperty("hips_service_path");
         if( path!=null ) return path;
      }
      
      try {
         if( id!=null && aladin.glu.aladinDic.get(id)!=null) {
            return aladin.glu.getURL(id,"",false,false,1)+"";
         }
      } catch( Exception e ) {
         e.printStackTrace();
      }
      //      if( url==null && id!=null ) url = aladin.glu.getURL(id)+"";
      return url;
   }

   /** Retourne le champ %AladinTree qui correspond au path du noeud sans le noeud terminal */
   protected String getAladinTree() {
      int i = path.lastIndexOf('/');
      if( i==-1 ) return "";
      return path.substring(0,i);
   }

   /** Retourne l'enregistrement GLU qui correspond
    * @url peut indiquer une url alternative par rapport au d�faut
    * @return l'enregistrement GLU qui va bien
    */
   public String getGluDic() {


      StringBuffer s = new StringBuffer();
      s.append(GluApp.glu("ActionName",id));
      s.append(GluApp.glu("Description",description));
      //      s.append(GluApp.glu("DistribDomain","ALADIN"));
      //      s.append(GluApp.glu("Owner","CDS'aladin"));
      s.append(GluApp.glu("Url",getUrl()));
      s.append(GluApp.glu("Aladin.Label",label));
      s.append(GluApp.glu("Aladin.Tree",getAladinTree()));
      s.append(GluApp.glu("Aladin.HpxParam",hpxParam));
      s.append(GluApp.glu("Aladin.Profile",aladinProfile));
      s.append(GluApp.glu("Copyright",copyright));
      s.append(GluApp.glu("Copyright.Url",copyrightUrl));
      s.append(GluApp.glu("VerboseDescr",verboseDescr));
      s.append(Util.CR);
      return s.toString();
   }
   
   /** Chargement par d�faut � effectuer (suite � un double-clic sur le noeud de l'arbre) */
   protected void load() {
      if( hasSIA() ) loadSIA();
      else if( hasSSA() ) loadSSA();
      else if( getUrl()==null && isCatalog() ) loadCS();
      else loadHips();
   }
   
   void queryByXmatch() {
      ServerXmatch serverXmatch = new ServerXmatch(aladin);

      // D�termination du PlanCatalogue
      Plan plan=null;
      for( Object p : aladin.calque.getSelectedPlanes() ) {
         if( ((Plan)p).isSimpleCatalog() && ((Plan)p).flagOk) { plan=(Plan)p; break; }
      }
      if( plan==null ) {
         for( Object p : aladin.calque.getPlans() ) {
            if( ((Plan)p).isSimpleCatalog() && ((Plan) p).flagOk) { plan=(Plan)p; break; }
         }
      }
      if( plan==null ) {
         aladin.warning("You need to select or have a catalog plane in the stack");
         return;
      }
      serverXmatch.setPlan(plan);
      serverXmatch.setCatName(internalId);
      serverXmatch.setPlanName(internalId);

      // Pas de limitation
      serverXmatch.setLimit("5000000");

      // Et c'est parti
      serverXmatch.submit();
   }

   /** Open the TAP form associated to this collection => Chaitra */
   void queryByTap() {
      // GLU TAP tag
      String gluTag = prop.get("tap_glutag");
      
      // Generation of simplified TAP form by GLU definition 
     /* if( gluTag==null ) {
         aladin.info("Simplified TAP form based on GLU definition "+gluTag+"\n(Not yet implemented)");
         // Chaitra ...
         return;
      } */
      
      // If there is no TAP glu definition, we will use base TAP url
      String url = prop.get("tap_service_url");
      String id = prop.get("ID");
      if( id!=null && url!=null ) {
    	  try {
			TapManager.getInstance(aladin).loadTapServerForSimpleFrame(id, url, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			aladin.warning("Error unable to load "+url+"\n"+e.getMessage());
		}
//         aladin.info("Generic TAP form for "+url+"\n(Not yet implemented)");
//         // Chaitra ...
//         return;
      } else {
    	  aladin.warning("Error! No TAP form configured for "+gluTag);
      }
      
      //TODO:: tintin remove the below just for demo
     /* String url1 = "http://tapvizier.u-strasbg.fr/TAPVizieR/tap";
      String id1 = "GAIA_VIZIER";
      url1 = "http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap2";
      id1 = "TAPCADC98";
      if( id1!=null && url1!=null ) {
    	  TapManager tapManager = TapManager.getInstance(aladin);
          tapManager.loadTapServerForSimpleFrame(id1, url1);
//         aladin.info("Generic TAP form for "+url+"\n(Not yet implemented)");
//         // Chaitra ...
//         return;
      }*/
   }
 
   /** G�n�ration et ex�cution de la requ�te script correspondant au protocole SSA */
   protected void loadSSA() { aladin.execAsyncCommand( getSSACmd()+" "+getDefaultTarget()+" "+getDefaultRadius(1) ); }
   protected String getSSABkm() { return getSSACmd()+" $TARGET $RADIUS"; }
   protected String getSSACmd() {
      // Glu tags sp�cifiques ?
      String gluTag = prop.get("ssa_glutag");
      
      // URL de base ?
      // On passe tout de m�me par le ID afin d'avoir une commande script clean
      // => voir GluServer pour la r�solution internalId => Url de base
      if( gluTag==null && prop.get("ssa_service_url")!=null )  gluTag = "SSA("+internalId+")";

      return "get "+gluTag;
   }
   
   
   /** G�n�ration et ex�cution de la requ�te script correspondant au protocole SIA ou SIA2 */
   protected void loadSIA() { aladin.execAsyncCommand( getSIACmd()+" "+getDefaultTarget()+" "+getDefaultRadius(1) ); }
   protected String getSIABkm() { return getSIACmd()+" $TARGET $RADIUS"; }
   private String getSIACmd() {
      // Glu tags sp�cifiques ?
      String gluTag = prop.get("sia_glutag");
      if( gluTag==null ) gluTag = prop.get("sia2_glutag");
      
      // URL de base ?
      // On passe tout de m�me par le ID afin d'avoir une commande script clean
      // => voir GluServer pour la r�solution internalId => Url de base
      if( gluTag==null && prop.get("sia2_service_url")!=null ) gluTag = "SIA2("+internalId+")";
      if( gluTag==null && prop.get("sia_service_url")!=null )  gluTag = "SIA("+internalId+")";
      
      return "get "+gluTag;
   }
   
   protected void loadCS(Coord c,double radius) {
      aladin.execAsyncCommand( getCSCmd()+" "+aladin.localisation.ICRSToFrame( c ).getDeg()+" "+Coord.getUnit( radius ));
   }
   /** G�n�ration et ex�cution de la requ�te script correspondant au protocole CS ou assimil� ASU */
   protected void loadCS() { aladin.execAsyncCommand( getCSCmd()+" "+getDefaultTarget()+" "+getDefaultRadius(15)); }
   protected String getCSBkm() { return getCSCmd()+" $TARGET $RADIUS"; }
   private String getCSCmd() {
      String cmd = null;

      // On passe par VizieR/Simbad via la commande script adapt�e
      if( isCDSCatalog() ) {
         int i = internalId.indexOf('/');
         String cat = internalId.substring(i+1);
         if( internalId.startsWith("CDS/Simbad") ) cmd = "get Simbad";
         else cmd = "get VizieR("+cat+",allcolumns)";

      // Acc�s direct CS
      } else if( prop!=null && (prop.get("cs_service_url")!=null) ) cmd = "get CS("+internalId+")";

      return cmd;
   }
   
   /** G�n�ration et ex�cution de la requ�te script correspondant au protocole MOC */
   protected void loadMoc( ) { aladin.execAsyncCommand( getMocCmd() ); }
   protected String getMocBkm() { return getMocCmd(); }
   private String getMocCmd() { return "get MOC("+Tok.quote(internalId)+")"; }

   /** G�n�ration et ex�cution de la requ�te script correspondant au protocole HiPS */
   protected void loadHips() {
      String trg = getDefaultTarget();
      String s = trg==null ? "" : " "+trg+" "+getDefaultRadius();
      aladin.execAsyncCommand( getHipsCmd()+s );
   }
   protected String getHipsBkm() { return getHipsCmd()+" $TARGET $RADIUS"; }
   protected String getHipsCmd() {
      String id = Tok.quote(internalId!=null?internalId:label);
      String mode = !isCatalog() && isTruePixels() ? ",fits":"";
      String cmd = "get HiPS("+id+mode+")";
      return cmd;
   }
   
   /** G�n�ration et ex�cution de la requ�te script correspondant � l'acc�s aux prog�niteurs */
   protected void loadProgenitors() { aladin.execAsyncCommand( getProgenitorsCmd() ); }
   protected String getProgenitorsBkm() { return getProgenitorsCmd(); }
   private String getProgenitorsCmd() {
      String progen = getProgenitorsUrl();
      if( progen==null ) progen = url+"/"+Constante.FILE_HPXFINDER;
      return Tok.quote(internalId+" PNG")+"=load "+progen;
   }

   /** G�n�ration et ex�cution de la requ�te script permettant le chargement de la totalit� d'un catalogue VizieR */
   protected void loadAll() { aladin.execAsyncCommand( getLoadAllCmd() ); }
   protected String getAllBkm() { return getLoadAllCmd(); }
   private String getLoadAllCmd() {
      int i = internalId.indexOf('/');
      String cat = internalId.substring(i+1);
      return "get VizieR("+cat+",allsky,allcolumns)";
   }
   
   private String getDefaultTarget() {
      Coord coo;
      if( aladin.view.isFree() || !Projection.isOk( aladin.view.getCurrentView().getProj()) ) {
         String s = aladin.localisation.getTextSaisie();
         if( s.length()!=0 ) return s;
         coo = getTarget();
         s = coo==null ? null : coo.toString();
         if( s!=null ) return s;
         return null;
//         aladin.info("Pas encore implant�, il faudrait demander Target+Radius");
      }
      coo = aladin.view.getCurrentView().getCooCentre();
      coo = aladin.localisation.ICRSToFrame( coo );
      return coo.getDeg();
   }
   
//   private double getDefaultRadiusInDeg() {
//      if( aladin.view.isFree() || !Projection.isOk( aladin.view.getCurrentView().getProj()) ) {
//         return 14./60;
//      }
//      return  aladin.view.getCurrentView().getTaille();
//   }
//   
   private String getDefaultRadius() { return getDefaultRadius(-1); }
   private String getDefaultRadius(double maxRad) {
      if( aladin.view.isFree() || !Projection.isOk( aladin.view.getCurrentView().getProj()) ) {
         return "14'";
      }
      double radius = aladin.view.getCurrentView().getTaille();
      if( maxRad>=0 && radius>maxRad ) radius=maxRad;
      return Coord.getUnit( radius );
   }
   
  
   private boolean scanning=false;
   synchronized void setScanning(boolean flag) { scanning = flag; }
   synchronized boolean isScanning() { return scanning; }
   
   /** G�n�ration du MOC qui correspond aux sources CS ou SIA ou SSA du champ courant */
   protected void scan( MocItem2 mo ) {
      setScanning(true);
      try { scan1(mo); }
      finally { setScanning(false); }
   }
   
   protected void scan1( MocItem2 mo ) {
      String url=null;
      Coord c = aladin.view.getCurrentView().getCooCentre();
      double rad = aladin.view.getCurrentView().getTaille();
      if( rad>1 ) rad=1;
      String radius = Util.myRound( rad );

      
      if( hasCS() ) {
         url = getCSUrl();
         if( !url.endsWith("?") && !url.endsWith("&") ) url+="?";
         url += "RA="+c.al+"&DEC="+c.del+"&SR="+radius+"&VERB=2";
         
      } else if( hasSIA() ) {
         url = prop.get("sia_service_url");
         if( url!=null ) {
            if( !url.endsWith("?") && !url.endsWith("&") ) url+="?";
            url+="POS="+c.al+","+c.del+"&SIZE="+radius+"&FORMAT=image/fits";
         } else {
            url = prop.get("sia2_service_url");
            if( url!=null ) {
               if( !url.endsWith("?") && !url.endsWith("&") ) url+="?";
               url+="REQUEST=query&POS=CIRCLE="+c.al+" "+c.del+" "+radius+"&FORMAT=image/fits";
            }
         }

      } else if( hasSSA() ) {
         url = prop.get("ssa_service_url");
         if( !url.endsWith("?") && !url.endsWith("&") ) url+="?";
         url+="REQUEST=queryData&POS="+c.al+","+c.del+"&SIZE="+radius;
      }
      
      if( url==null ) return;
      
      // M�morisation du r�sultat
      HealpixMoc moc;
      try { 
         moc = scan(url); 
         if( mo.moc==null ) mo.moc=moc;
         else mo.moc.add(moc);
      } catch( Exception e ) { if( aladin.levelTrace>=3 )  e.printStackTrace();  }
      
      // M�morisation de la surface couverte
      try {
         int order=11;
         Healpix hpx = new Healpix();
         moc = new HealpixMoc(order);
         int i=0;
         moc.setCheckConsistencyFlag(false);
         for( long n : hpx.queryDisc(order, c.al, c.del,  rad) ) {
            moc.add(order, n);
            if( (++i)%1000==0 )  moc.checkAndFix();
         }
         moc.setCheckConsistencyFlag(true);
         
         if( mo.mocRef==null ) mo.mocRef=moc;
         else mo.mocRef.add(moc);
        
         
      } catch( Exception e ) { if( aladin.levelTrace>=3 )  e.printStackTrace(); }
      
   }
   
   
   private MyInputStream inScan=null;
   
   protected void abortScan() { 
      try { if( inScan!=null ) { inScan.close(); inScan=null; }  } catch( Exception e ) {}
   }
   
   /**  G�n�ration d'un Moc � partir du catalogue retourn�e par l'URL
    * pass�e en param�tre */
   private HealpixMoc scan( String url) {
      Pcat pcat = new Pcat(aladin);
      pcat.plan = new PlanCatalog(aladin);
      pcat.plan.label="test";
      HealpixMoc moc=null;
      int order=11;
      
      try {
         moc = new HealpixMoc(order);
         inScan=new MyInputStream( Util.openStream(url,false,30000) );
         pcat.tableParsing(inScan,null);
         
         Iterator<Obj> it = pcat.iterator();
         Healpix hpx = new Healpix();
         int n=0;
         while( it.hasNext() ) {
//            if( n%100==0 && aladin.directory.isAbortingScan() ) throw new Exception("Aborted");
            Obj o = it.next();
            if( !(o instanceof Position) ) continue;
            if( Double.isNaN( ((Position)o).raj ) ) continue;
            try {
               long pix = hpx.ang2pix(order, ((Position)o).raj, ((Position)o).dej);
               moc.add(order,pix);
               n++;
               if( n>10000 ) { moc.checkAndFix(); n=0; }
            } catch( Exception e ) {
               if( aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
         moc.setCheckConsistencyFlag(true);
          
      } catch( Exception e ) { if( aladin.levelTrace>=3 )  e.printStackTrace();
      } finally { if( inScan!=null ) try { inScan.close(); inScan=null; } catch( Exception e) {} }
      
      return moc;
   }
   
   void loadCopyright() { aladin.glu.showDocument(copyrightUrl); }

   void setDefaultMode(int mode) {
      truePixelsSet=true;
      if( mode==PlanBG.FITS && inFits ) truePixels=true;
      else if( mode==PlanBG.JPEG && (inJPEG || inPNG) ) truePixels=false;
   }

   protected void reset() {
      truePixelsSet=false;
   }

   void setMoc() {
      if( skyFraction==null || skyFraction.equals("1") ) return;

      JButton b = new JButton(" (get Moc)");
      b.setFont(b.getFont().deriveFont(Font.ITALIC));
      b.setForeground(Color.blue);
      b.setBackground(background);
      b.setContentAreaFilled(false);
      b.setBorder( BorderFactory.createMatteBorder(0, 0, 1, 0, Color.blue) );
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { loadMoc(); }
      });
      gc.insets.bottom=7;
      gb.setConstraints(b,gc);
      getPanel().add(b);
   }

   void loadDensityMap() {
      int off1 = internalId.indexOf('/');
      int off2 = internalId.lastIndexOf('/');
      String catId = internalId.substring(off1+1, off2);
      
      try {  aladin.calque.newPlanDMap(internalId,catId);
      } catch( Exception e ) { }
//      String url = aladin.glu.gluResolver("getDMap",catId,false);
//      aladin.execAsyncCommand("'DM "+internalId+"'=load "+url);
   }
   
   void queryByMoc(PlanMoc planMoc) {
      ServerMocQuery serverMoc = new ServerMocQuery(aladin);
      serverMoc.setPlan(planMoc);
      
      // Positionnement de l'identificateur du catalog
      int i = internalId.indexOf('/');
      String catName = internalId.substring(i+1);
      serverMoc.setCatName(catName);
      
      // Postionnement du label du plan � cr�er
      serverMoc.setPlanName(internalId);
      
      // Pas de limitation
//      serverMoc.setLimit("unlimited");
      serverMoc.setLimit("5000000");
      
      // Et c'est parti
      serverMoc.submit();
   }

   void setUrl(String url) { this.url=url; }
   void setCopyright(String copyright) {
      Component c=null;
      if( copyrightUrl==null ) {
         JLabel l = new JLabel("("+copyright+")");
         c=l;
         gc.insets.bottom=0;
      } else {
         JButton b = new JButton(copyright!=null?copyright : "Copyright");
         b.setFont(b.getFont().deriveFont(Font.ITALIC));
         b.setForeground(Color.blue);
         b.setBackground(background);
         b.setContentAreaFilled(false);
         b.setBorder( BorderFactory.createMatteBorder(0, 0, 1, 0, Color.blue) );
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { loadCopyright(); }
         });
         c=b;
         gc.insets.bottom=5;
      }
      gb.setConstraints(c,gc);
      getPanel().add(c);
   }

}
