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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;

import cds.allsky.Constante;
import cds.moc.Healpix;
import cds.tools.Util;

/** G�re les noeuds de l'arbre du formulaire ServerAllsky */
public class TreeNodeAllsky extends TreeNode {

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

   /** Construction d'un TreeNodeAllSky � partir des infos qu'il est possible de glaner
    * � l'endroit indiqu�, soit par exploration du r�pertoire, soit par le fichier Properties */
   public TreeNodeAllsky(Aladin aladin,String pathOrUrl) {
      String s;
      this.aladin = aladin;
      local=!(pathOrUrl.startsWith("http:") || pathOrUrl.startsWith("https:") ||pathOrUrl.startsWith("ftp:"));
      MyProperties prop = new MyProperties();

      // Par http ou ftp ?
      try {
         InputStream in=null;
         if( !local ) in = (new URL(pathOrUrl+"/"+Constante.FILE_PROPERTIES)).openStream();
         else in = new FileInputStream(new File(pathOrUrl+Util.FS+Constante.FILE_PROPERTIES));
         if( in!=null ) { prop.load(in); in.close(); }
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

   public TreeNodeAllsky(Aladin aladin,String actionName,String id,String aladinMenuNumber, String url,String aladinLabel,
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

      if( copyright!=null || copyrightUrl!=null ) setCopyright(copyright);
      
      if( color && !inJPEG && !inPNG ) inJPEG=true;
      
      setMoc();

      //      Aladin.trace(3,this.toString1());
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

   protected void submit() {
      String mode = isTruePixels() ? ",fits":"";
      aladin.console.printCommand("get allsky("+Tok.quote(label)+mode+")");

      aladin.allsky(this);
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

   void loadMoc() {
      MyInputStream mis = null;
      try {
         mis = Util.openAnyStream( getUrl()+"/Moc.fits" );
         aladin.calque.newPlanMOC(mis,label+" MOC");
      }
      catch( Exception e) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
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
