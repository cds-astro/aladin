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

import cds.moc.Healpix;
import cds.tools.Util;

/** Gère les noeuds de l'arbre du formulaire ServerAllsky */
public class TreeNodeAllsky extends TreeNode {

   private String url;           // L'url ou le path du survey
   public String description;   // Courte description (une ligne max)
   public String verboseDescr;  // Description de l'application (1 paragraphe ou plus)
   public String copyright;     // Mention légale du copyright
   public String copyrightUrl;  // Url pour renvoyer une page HTML décrivant les droits
   public String hpxParam;      // Les paramètres propres à HEALPIX
   public String version="";    // Le numéro de version du survey
   public String imageSourcePath; // Le template d'accès aux images sources (progéniteurs) (ex: id~=/(.*)/http://aladin.u-strasbg.fr/get?img=$1/)
   public String aladinProfile; // profile de l'enregistrement GLU (notamment "localdef")*
   public String aladinLabel;
   public int minOrder=-1;      // Min order Healpix
   public int maxOrder=-1;      // Max order Healpix
   private boolean useCache=true;// Non utilisation du cache local
   private boolean color=false;  // true si le survey est en couleur
   private boolean inFits=false; // true si le survey est fourni en FITS
   private boolean inJPEG=false; // true si le survey est fourni en JPEG
   private boolean truePixels=false; // true si par défaut le survey est fourni en truePixels (FITS)
   private boolean cat=false;    // true s'il s'agit d'un catalogue hiérarchique
   private boolean map=false;    // true s'il s'agit d'une map HEALPix FITS
   private boolean moc=false;    // true s'il faut tout de suite charger le MOC
   public int frame=Localisation.GAL;  // Frame d'indexation
   public Coord target=null;     // Target for starting display
   public double radius=-1;   // Field size for starting display
   public int nside=-1;          // Max NSIDE
   public boolean local=false;   // Il s'agit d'un survey sur disque local
   
   /** Construction d'un TreeNodeAllSky à partir des infos qu'il est possible de glaner
    * à l'endroit indiqué, soit par exploration du répertoire, soit par le fichier Properties */
   public TreeNodeAllsky(Aladin aladin,String pathOrUrl) {
      String s;
      this.aladin = aladin;
      local=!(pathOrUrl.startsWith("http:") || pathOrUrl.startsWith("https:") ||pathOrUrl.startsWith("ftp:"));
      MyProperties prop = new MyProperties();
      
      // Par http ou ftp ?
      try {
         InputStream in=null;
         if( !local ) in = (new URL(pathOrUrl+"/"+PlanHealpix.PROPERTIES)).openStream();
         else in = new FileInputStream(new File(pathOrUrl+Util.FS+PlanHealpix.PROPERTIES));
         if( in!=null ) { prop.load(in); in.close(); }
      } catch( Exception e ) { aladin.trace(3,"No properties file found => auto discovery..."); }
      

      // recherche du frame Healpix
      String strFrame = prop.getProperty(PlanHealpix.KEY_COORDSYS,"G");
      char c1 = strFrame.charAt(0);
      if( c1=='C' ) frame=Localisation.ICRS;
      else if( c1=='E' ) frame=Localisation.ECLIPTIC;
      else if( c1=='G' ) frame=Localisation.GAL;

      url=pathOrUrl;
      
      s = prop.getProperty(PlanHealpix.KEY_LABEL);
      if( s!=null ) label=s;
      else {
         char c = local?Util.FS.charAt(0):'/'; 
         int end = pathOrUrl.length();
         int offset = pathOrUrl.lastIndexOf(c);
         if( offset==end-1 && offset>0 ) { end=offset; offset = pathOrUrl.lastIndexOf(c,end-1); }
         label = pathOrUrl.substring(offset+1,end);
      }
      id=label;
      
      s = prop.getProperty(PlanHealpix.KEY_VERSION);
      if( s!=null ) version=s;
      
      s = prop.getProperty(PlanHealpix.KEY_IMAGESOURCEPATH);
      if( s!=null ) {
         imageSourcePath=s;
         Aladin.trace(4,"TreeNodeAllsky() => imageSourcePath="+s);
      }
      
      description = prop.getProperty(PlanHealpix.KEY_DESCRIPTION);
      verboseDescr = prop.getProperty(PlanHealpix.KEY_DESCRIPTION_VERBOSE);
      copyright = prop.getProperty(PlanHealpix.KEY_COPYRIGHT);
      copyrightUrl = prop.getProperty(PlanHealpix.KEY_COPYRIGHT_URL);
      useCache = !local && Boolean.parseBoolean( prop.getProperty(PlanHealpix.KEY_USECACHE,"True") );
      
      s = prop.getProperty(PlanHealpix.KEY_TARGET);
      if( s==null ) target=null;
      else {
         try { target = new Coord(s); }
         catch( Exception e) { aladin.trace(3,"target error!"); target=null; }
      }
      s = prop.getProperty(PlanHealpix.KEY_TARGETRADIUS);
      if( s==null ) radius=-1;
      else {
         try { radius=Server.getAngle(s, Server.RADIUSd); }
         catch( Exception e) { aladin.trace(3,"radius error!"); radius=-1; }
      }
      
      s = prop.getProperty(PlanHealpix.KEY_NSIDE);
      if( s!=null ) try { nside = Integer.parseInt(s); } catch( Exception e) {
         aladin.trace(3,"NSIDE number not parsable !");
         nside=-1;
      }
      
      try { maxOrder = new Integer(prop.getProperty(PlanHealpix.KEY_MAXORDER)); }
      catch( Exception e ) {
         maxOrder = getMaxOrderByPath(pathOrUrl,local);
         if( maxOrder==-1 ) {
            aladin.trace(3,"No maxOrder found (even with scanning dir.) => assuming 11");
            maxOrder=11;
         }
      }
      
      s = prop.getProperty(PlanHealpix.KEY_ISCAT);
      if( s!=null ) cat = new Boolean(s);
      else cat = getFormatByPath(pathOrUrl,local,2);
      
      // Détermination du format des cellules dans le cas d'un survey pixels
      String keyColor = prop.getProperty(PlanHealpix.KEY_ISCOLOR);
      if( keyColor!=null ) color = new Boolean(keyColor);
      if( color ) inJPEG=true;
      if( !cat && (keyColor==null || !color) ) {
         String format = prop.getProperty(PlanHealpix.KEY_FORMAT);
         if( format!=null ) {
            int a,b;
            inFits = (a=Util.indexOfIgnoreCase(format, "fit"))>=0;
            inJPEG = (b=Util.indexOfIgnoreCase(format, "jpeg"))>=0 || (b=Util.indexOfIgnoreCase(format, "jpg"))>=0;
            truePixels = inFits && a<b;                         // On démarre dans le premier format indiqué
         } else {
            inFits = getFormatByPath(pathOrUrl,local,0);
            inJPEG = getFormatByPath(pathOrUrl,local,1);
            truePixels = local && inFits || !local && inJPEG;   // par défaut on démarre en FITS en local, en Jpeg en distant
         }
         if( keyColor==null ) color = getIsColorByPath(pathOrUrl,local);
      }
      
      aladin.trace(4,toString1());
   }
   
   
   private boolean getIsColorByPath(String path,boolean local) {
      try { 
         if( local ) return Util.isJPEGColored(path+Util.FS+"Norder3"+Util.FS+"Allsky.jpg");
         MyInputStream in = new MyInputStream( Util.openStream(path+"/Norder3/Allsky.jpg") );
         byte [] buf = in.readFully();
         return Util.isJPEGColored(buf);
      } catch( Exception e) {
         aladin.trace(3,"Allsky.jpg not found => assume B&W survey");
         return false;
      }
   }
   
   private boolean getFormatByPath(String path,boolean local,int fmt) {
      String ext = fmt==0 ? ".fits" : fmt==1 ? ".jpg" : ".xml";
      return local && (new File(path+Util.FS+"Norder3"+Util.FS+"Allsky"+ext)).exists() ||
            !local && Util.isUrlResponding(path+"/Norder3/Allsky"+ext);
   }
   
   private int getMaxOrderByPath(String urlOrPath,boolean local) {
      int maxOrder=-1;
      for( int n=3; n<100; n++ ) {
         if( local && !(new File(urlOrPath+Util.FS+"Norder"+n).isDirectory()) ||
            !local && !Util.isUrlResponding(urlOrPath+"/Norder"+n)) break;
         maxOrder=n;
      }
      return maxOrder;
   }

   public TreeNodeAllsky(Aladin aladin,String actionName,String aladinMenuNumber, String url,String aladinLabel,
         String description,String verboseDescr,String aladinProfile,String copyright,String copyrightUrl,String path,
         String aladinHpxParam) {
      super(aladin,actionName,aladinMenuNumber,aladinLabel,path);
      this.aladinLabel  = aladinLabel;
      this.url          = url;
      this.description  = description;
      this.verboseDescr = verboseDescr;
      this.copyright    = copyright;
      this.copyrightUrl = copyrightUrl;
      this.hpxParam     = aladinHpxParam;
      this.aladinProfile= aladinProfile;
      
      if( this.url!=null ) {
         char c = this.url.charAt(this.url.length()-1);
         if( c=='/' || c=='\\' ) this.url = this.url.substring(0,this.url.length()-1);
      }

      // Parsing des paramètres Healpix
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
               if( Util.indexOfIgnoreCase(s, "fits")>=0 ) { inFits=true; if( first ) { first=false ; truePixels=true; } }
               if( Util.indexOfIgnoreCase(s, "jpeg")>=0 ) { inJPEG=true; if( first ) { first=false ; truePixels=false;} } 
               if( Util.indexOfIgnoreCase(s, "gal")>=0 ) frame = Localisation.GAL;
               if( Util.indexOfIgnoreCase(s, "ecl")>=0 ) frame = Localisation.ECLIPTIC;
               if( Util.indexOfIgnoreCase(s, "equ")>=0 ) frame = Localisation.ICRS;
               if( Util.indexOfIgnoreCase(s, "cat")>=0 ) cat=true;
               if( Util.indexOfIgnoreCase(s, "map")>=0 ) map=true;
               if( Util.indexOfIgnoreCase(s, "moc")>=0 ) moc=true;
               
              
               // Un numéro de version du genre "v1.23" ?
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
      
      // dans le cas d'un répertoire local => pas d'utilisateur du cache
      if( url!=null && !url.startsWith("http") && !url.startsWith("ftp") ) useCache=false;
      
      if( copyright!=null || copyrightUrl!=null ) setCopyright(copyright);

//      Aladin.trace(3,this.toString1());
   }
   
   public String toString1() {
      double r;
      Coord c;
      return "GluSky ["+id+"]"
                  +(isCatalog() ?" catalog" : isMap()?" fitsMap":" survey")
                  +" maxOrder:"+getMaxOrder()
                  +(getLosangeOrder()>=0?" cellOrder:"+getLosangeOrder():"")
                  +(!isCatalog() && isColored() ?" colored" : " B&W")
                  +(!isFits() ? "" : isTruePixels() ?" *inFits*" : " inFits")
                  +(!isJPEG() ? "" : isTruePixels() ?" inJPEG" : " *inJPEG*")
                  +(loadMocNow() ? " withMoc" : "")
                  +(useCache() ? " cache" : " nocache")
                  +" "+Localisation.getFrameName(getFrame())
                  +(isLocalDef() ? " localDef":"")
                  +(isLocal() ? " local" : "")
                  +((c=getTarget())!=null?" target:"+c:"")
                  +((r=getRadius())!=-1?"/"+Coord.getUnit(r):"")
                  +" \""+label+"\" => "+getUrl();
   }
   
   /** retourne true si cette définition doit être sauvegardée dans le dico GLU local */
   protected boolean isLocalDef() { return aladinProfile!=null && aladinProfile.indexOf("localdef")>=0; }
   
   /** Retourne true si la description GLU correspond à un fichier Map healpix*/
   protected boolean isMap() { return map; }
   
   /** Retourne true s'il s'agit d'un catalogue hiérarchique */
   protected boolean isCatalog() { return cat; }
   
   /** Retourne true s'il s'agit d'un survey ou d'une map couleur */
   protected boolean isColored() { return color; }
   
   protected int getFrame() { return frame; }
   
   /** Retourne true s'il s'agit d'un survey fournissante les losanges en FITS => true pixel */
   protected boolean isFits() { return inFits; }
   
   protected int getMaxOrder() { return maxOrder; }
   
   /** Retourne le target par défaut (premier affichage)  sous la forme J2000 décimal, null sinon */
   protected Coord getTarget() { return target; }
   
   /** Retourne le rayon du champ par défaut (premier affichage) en degrés, -1 sinon */
   protected double getRadius() { return radius; }
   
   /** retourne le template d'accès aux progéniteurs */
   protected String getImageSourcePath() { return imageSourcePath; }
   
   /** Retourne le numéro de version du survey, "" si non défini */
   protected String getVersion() { return version==null ? "" : version; }
   
   protected int getLosangeOrder() { 
      if( cat || nside==-1 || maxOrder==-1) return -1;
      return (int)Healpix.log2(nside) - maxOrder;
   }
   
   protected boolean isLocal() { return local; }
   
   protected boolean loadMocNow() { return moc; }
   
   /** Retourne true s'il s'agit d'un survey fournissante les losanges en JPEG => 8 bits pixel + compression */
   protected boolean isJPEG() { return inJPEG; }
   
   /** Retourne true si par défaut le survey est fourni en true pixels (FITS)  */
   protected boolean isTruePixels() { return truePixels; }
   
   /** Retourne true si le survey utilise le cache local */
   protected boolean useCache() { return useCache; }
   
   /** retourne l'URL de base pour accéder au serveur HTTP */
   protected String getUrl() {
      if( url==null && id!=null ) url = aladin.glu.getURL(id)+"";
      return url;
   }

   /** Retourne le champ %AladinTree qui correspond au path du noeud sans le noeud terminal */
   protected String getAladinTree() {
      int i = path.lastIndexOf('/');
      if( i==-1 ) return "";
      return path.substring(0,i);
   }
   
   /** Retourne l'enregistrement GLU qui correspond
    * @url peut indiquer une url alternative par rapport au défaut
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
   
   protected void submit() { aladin.allsky(this); }
   
   void loadCopyright() { aladin.glu.showDocument(copyrightUrl); }
   
   void setDefaultMode(int mode) {
      if( mode==PlanBG.FITS && inFits ) truePixels=true;
      else if( mode==PlanBG.JPEG && inJPEG ) truePixels=false;
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
