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

package cds.aladin;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.Comparator;

import cds.tools.Util;

/**
 * Gestion des applications utilisable par Aladin et connues via le GLU
 * @author Pierre Fernique
 * @version 1.0 sept 2007 Création
 */
public class GluApp implements Comparator {
   
   static public String JAVAPATH = "java";
   static public String JAVAWS = "javaws";
   
   private Aladin aladin;
   String ordre = "Y";          // numéro d'ordre du formulaire
   public String tagGlu;        // Identificateur GLU unique de l'application
   public String aladinLabel;   // Désignation de l'application utilisée dans les menus (3-4 mots)
   public String description;   // Courte description (une ligne max)
   public String verboseDescr;  // Description de l'application (1 paragraphe ou plus)
   public String institute;     // Institut origine de l'appli
   public String releaseNumber; // Numéro de la version (détermine la nécessité d'une mise-à-jour)
   public String nextNumber;    // Numéro de la prochaine version non encore installée
   public String copyright;     // Mention légale du copyright
   public String docUrl;        // Adresse d'une page de doc sur l'application
   public String jarUrl;        // Adresse pour le téléchargement du jar (un seul jar par appli en téléchargement)
   public String javaParam;     // paramètre java 
   public String downloadUrl;   // Adresse pour l'installation
   public String wsUrl;         // Adresse du lancement par webstart
   public String appletUrl;     // Adresse du lancement par l'applet
   public String pluginUrl;     // Adresse de download du plugin
   public String activated;     // Yes si actif
   public String system;        // Ligne de commande perso
   public String dir;           // Le répertoire d'installation du jar
   protected long downloading;  // Nombre d'octets en cours de déjà déchargé, -1 sinon
   protected boolean interrupt; // true si on interrompt un chargement
   private int type;            // Type d'application (masque de bits)
   
   static final int JAVA =    1;
   static final int PLASTIC = 1<<1;
   
   /**
    * 
    * @param tagGlu Identificateur GLU unique de l'application
    * @param aladinLabel Désignation de l'application utilisée dans les menus (3-4 mots)
    * @param description Courte description (une ligne max)
    * @param verboseDescription Description de l'application (1 paragraphe ou plus)
    * @param institute Institut
    * @param releaseNumber Numéro de la version (détermine la nécessité d'une mise-à-jour)
    * @param copyright Mention légale du copyright
    * @param docUser Adresse d'une page de doc sur l'application
    * @param jar Adresse pour le téléchargement du jar (un seul jar par appli en téléchargement)
    * @param javaParam Ligne de commande à exécuter pour lancer l'application sans path (ex java -jar Topcat.jar)
    * @param type type d'application (pour le moment uniquement JAVA/Plastic)
    */
   public GluApp(Aladin aladin, String tagGlu,String aladinLabel, String aladinMenuNumber, String description,
         String verboseDescription, String institute, String releaseNumber, String copyright,
         String docUser,String jar, String javaParam, String dir, String download, String webstart,
         String applet,String plugin,String aladinActivated, String system, int type) {
      this.aladin       = aladin;
      this.tagGlu       = tagGlu;
      this.aladinLabel  = aladinLabel;
      this.ordre        = aladinMenuNumber;
      this.description  = description;
      this.verboseDescr = verboseDescription;
      this.institute    = institute;
      this.releaseNumber= releaseNumber;
      this.copyright    = copyright;
      this.docUrl       = docUser;
      this.jarUrl       = jar;
      this.javaParam    = javaParam;
      this.downloadUrl  = download;
      this.wsUrl        = webstart;
      this.appletUrl    = applet;
      this.pluginUrl    = plugin;
      this.dir          = dir;
      this.type         = type;
      this.system       = system;
      this.activated    = aladinActivated;
      downloading=-1;
      
//      Aladin.trace(3,"VOTools ["+tagGlu+"] => "+menu);
   }
   
   /** Cration manuelle d'une nouvelle description d'application VO */
   public GluApp(Aladin aladin, String tagGlu) {
      this.aladin=aladin;
      this.tagGlu=tagGlu;
      downloading=-1;
   }
   
   /** Si l'application est surchargé par un enregsitrement GLU, mise à jour
    * des informations locales */
   protected void merge(String aladinLabel,String aladinMenuNumber,String description,String verboseDescr,
         String institute, String releaseNumber, String copyright, String docUser,
         String jar, String javaParam, String download, String webstart, String applet, String plugin,
         String aladinActivated, int type) {
      this.tagGlu      = tagGlu;
      this.aladinLabel = aladinLabel;
      this.ordre       = aladinMenuNumber;
      this.description = description;
      this.verboseDescr= verboseDescr;
      this.institute   = institute;
      if( !this.releaseNumber.equals(releaseNumber) ) nextNumber=releaseNumber;
      this.copyright   = copyright;
      this.docUrl      = docUser;
      this.jarUrl      = jar;
      this.javaParam   = javaParam;
      this.downloadUrl = download;
      this.wsUrl       = webstart;
      this.appletUrl   = applet;
      this.pluginUrl   = plugin;
      this.activated   = this.activated==null ? aladinActivated : this.activated;
      this.type        = type;
      
//      Aladin.trace(3,"VOTools ["+tagGlu+"] => *** UPDATED");
   }
   
   static protected final int JAR      = 1;
   static protected final int DOWNLOAD = 2;
   static protected final int WEBSTART = 3;
   static protected final int APPLET   = 4;
   static protected final int PLUGIN   = 5;
   
   /** Retourne le type d'installation supporté par l'application */
   protected int getInstallMode() {
      if( wsUrl!=null ) return WEBSTART;
      if( appletUrl!=null ) return APPLET;
      if( pluginUrl!=null ) return PLUGIN;
      if( jarUrl!=null ) return JAR;
      if( downloadUrl!=null ) return DOWNLOAD;
      return 0;
   }
   
   /** Retourne true si le bouton "Install" peut être activé */
   protected boolean canBeInstall() {
      int mode=getInstallMode();
      return mode!=0 && mode!=WEBSTART && mode!=APPLET; 
//      if( mode==WS || mode==0 ) return false;
//      if( dir==null ) return true;
//      if( nextNumber!=null ) return true;
//      return false;
   }
   
   /** En cours de déchargement */
   protected boolean isDownloading() { return downloading!=-1; }
   
   /** Retourne true si le bouton "Run" peut être activé */
   protected boolean canBeRun() {
      int mode=getInstallMode();
      if( mode==WEBSTART || mode==APPLET ) return true;
      if( isDownloading() ) return false;
      if( dir!=null || system!=null) return true;
      return false;
   }
   
   /** Retourne true s'il doit être intégré dans les menus Aladin */
   protected boolean canBeMenu() {
//      if( !canBeRun() ) return false;
      return isActivated();
   }
   
   /** Retourne true si l'application est activée dans les menus */
   protected boolean isActivated() {
      if( activated==null  || activated.equals("No")) return false;
      return true;
   }
   
   /** Retourne true s'il y a une nouvelle version disponible */
   protected boolean hasNewRelease() { return nextNumber!=null; }
   
   /** Génère une ligne GLU "%key value", ou "" si la valeur est null ou commence par "--" */
  static protected String glu(String key,String value) {
      if( value==null || value.trim().startsWith("--") ) return "";
      return "%"+Util.align(key,20)+value+Util.CR;
   }
   
   /** Retourne l'enregistrement GLU qui correspond à l'application */
   protected String getGluDic() {
      StringBuffer s = new StringBuffer();
      s.append(glu("ActionName",tagGlu));
      s.append(glu("DistribDomain","ALADIN"));
      s.append(glu("Owner","CDS'aladin"));
      s.append(glu("Description",description));
      s.append(glu("VerboseDescr",verboseDescr));
      s.append(glu("Institute",institute));
      s.append(glu("ReleaseNumber",releaseNumber));
      s.append(glu("NextNumber",nextNumber));
      s.append(glu("Copyright",copyright));
      s.append(glu("Doc.User",docUrl));
      s.append(glu("Jar",jarUrl));
      s.append(glu("Webstart",wsUrl));
      s.append(glu("Applet",appletUrl));
      s.append(glu("Download",downloadUrl));
      s.append(glu("JavaParam",javaParam));
      s.append(glu("Aladin.VOLabel",aladinLabel));
      s.append(glu("Aladin.Activated",activated));
      s.append(glu("Dir",dir));
      s.append(glu("System",system));
      s.append(Util.CR);
      return s.toString();
   }
   
   /** Retourne la commande a exécuter */
   protected String getCommand() {
      if( getInstallMode()==WEBSTART ) return JAVAWS+" "+wsUrl;
      if( dir==null ) return null;
      if( system!=null ) return system;
      return getJavaCommand();
   }
   
   /** Retourne la commande java à exécuter en l'absence d'une commande particulère */
   protected String getJavaCommand() {
      if( javaParam!=null ) return JAVAPATH+" "+javaParam;
      return null;
   }
   
   /** Retourne true si l'utilisateur a surcharger la commande */
   protected boolean hasDedicatedCommand() {
      return system!=null;
   }
   
   /** Retourne true si l'utilisateur a surcharger le répertoire */
   protected boolean hasDedicatedDir() {
      return dir!=null && !dir.equals(aladin.getVOPath());
   }
   
   /** Retourne le répertoire d'installation, ou null si aucun */
   protected String getDir() {
      int mode = getInstallMode();
      if( mode==APPLET || mode==WEBSTART ) return null;
      return dir;
   }

   /** Lance l'éxécution de l'application VO */
   protected boolean exec() {
      if( !isInstalled() ) {
         FrameVOTool.display(aladin,this);
         return false;
      }
      
      aladin.log("VOToolExec",tagGlu);
      if( getInstallMode()==APPLET ) return execApplet();
      if( getInstallMode()==PLUGIN ) return execPlugin();
      final String dir = getDir();
      Aladin.trace(1,"Exec: "+(dir!=null?"cd "+dir+";":"") + getCommand());
      try {
         new Thread(tagGlu){
            public void run() {
               try { Runtime.getRuntime().exec(getCommand(),null,dir==null?null : new File(dir));
               }catch( Exception e1) { e1.printStackTrace(); }
            }
         }.start();
      } catch( Exception e ) { e.printStackTrace(); return false; }
      return true;
   }
   
   /** Démarrage en applet */
   protected boolean execApplet() {
      aladin.glu.showDocument("Http", appletUrl, true);
      return true;
   }
      
   /** Démarrage en plugin */
   protected boolean execPlugin() {
      // Le javaParam sert pour connaitre la commande script, et à défaut le nom de l'enregistrement GLU
      String pluginID = javaParam!=null ? javaParam : tagGlu;
      AladinPlugin ap = aladin.plugins.find(pluginID);
      if( ap!=null ) {
         try { ap.start(); } catch( AladinException e1 ) {
            e1.printStackTrace();
            aladin.error(aladin.chaine.getString("PLUGERROR")+"\n\n"+e1.getMessage());
            return false;
         }
      } else {
         aladin.error(aladin.chaine.getString("PLUGERROR")+"\n\nPlugin "+tagGlu+" unknown");
         return false;
      }
      return true;
   }
      
   protected String getInstallationName( String jarUrl) {
      try {
         return jarUrl.substring(jarUrl.lastIndexOf('/'));
      } catch( Exception e) {}
      return null;
   }
   
   /** Installation de l'application
    * @return 1 Ok, 0, non Ok, -1 rien à dire, -2 pas encore implanté
    */
   protected int install() {
      aladin.log("VOToolInstall",tagGlu);
      switch( getInstallMode() ) {
         case PLUGIN: installPlugin(); return -3;
         case JAR: installJar(); return -3;
         case DOWNLOAD : aladin.glu.showDocument("Http", downloadUrl, true); return -1;
         default: return -2;
      }
   }
   
   /** Interruption d'un chargement */
   protected void interrupt() { interrupt=true; }
   
   synchronized void setDownloading(long n) { downloading=n; }
   
   /** Déchargement du fichier jar d'un plugin dans le répertoire Plugin
    *d'Aladin. Le répertoire adéquat est créé si besoin
    * @return true si ça a marché.
    */
   protected boolean installPlugin() {
      try {
         aladin.plugins.close();
      } catch( Exception e ) {
         e.printStackTrace();
      }
      return installJar1(pluginUrl,aladin.plugins.getPlugPath());
   }
   
   /** Retourne vrai si l'outil VO est installé (pour ceux qui s'installe) */
   protected boolean isInstalled() {
      int mode = getInstallMode();
      String file=null;
      if( mode==JAR ) file = aladin.getVOPath()+Util.FS+getInstallationName(jarUrl);
      else if( mode==PLUGIN ) file = aladin.plugins.getPlugPath()+Util.FS+getInstallationName(pluginUrl);
      else return true;   // Les autres modes n'ont pas besoin d'installation spécifique
      return (new File(file)).exists();
   }
   
   
   /** Déchargement du fichier jar de l'application dans le répertoire VOTools
    * du cache d'Aladin. Le répertoire adéquat est créé si besoin
    * @return true si ça a marché.
    */
   protected boolean installJar() { return installJar1(jarUrl,aladin.getVOPath()); }
   protected boolean installJar1(final String jarUrl,final String targetPath) {
       setDownloading(0);
       interrupt=false;
       (new Thread("Install") {
         public void run() {
            try {
               URL u = new URL(jarUrl);
               InputStream inp = u.openStream();
               if( inp==null ) throw new Exception();
               DataInputStream in = new DataInputStream(inp);
               if( in==null ) throw new Exception();
               String installationName = getInstallationName(jarUrl);
               if( installationName==null ) throw new Exception("Installation name error");
               String file = targetPath+Util.FS+installationName;
               String tmp = file+".tmp";
               File g = new File(tmp); g.delete();
               RandomAccessFile out = new RandomAccessFile(tmp,"rw");
               byte buf[] = new byte[512];
               int n;
               while( !interrupt && in.available()==0 ) Util.pause(100);
               while( !interrupt && (n=in.read(buf))>0 ) {
                  out.write(buf,0,n); 
                  setDownloading(downloading+n);
               }
               in.close(); out.close();
               if( interrupt ) {
                  g.delete();
                  setDownloading(-1);
               } else {
                  File f = new File(file); 
                  myDelete(file);
                  g.renameTo(f);
                  dir=targetPath;
                  if( nextNumber!=null ) releaseNumber=nextNumber;
                  nextNumber=null;
               }
            } catch( Exception e ) {
               if( aladin.levelTrace>=3 ) e.printStackTrace();
               aladin.error("Installation error");
            }
            setDownloading(-1);
            aladin.frameVOTool.downloadEnd();
         }
      }).start();
      return true;
   }
   
   // On va renommer le fichier que l'on aurait voulu écraser et qui possède
   // un lock système. Ca permettra de mettre le nouveau à la place en contournant le problème
   private void myDelete(String file) throws Exception {
      File f= new File(file);
      for( int i=0; i<10; i++ ) {
         File t = i==0 ? new File(file) : new File(file+"-toberemoved."+i);
         t.delete();  
         Util.pause(500);
         if( t.exists() ) continue;   // problème pour effacer
         if( i>0 ) {
            if( !f.renameTo(t) ) {
               throw new Exception();
            }
         }
         return;
      }
      
      // Bon, après 10 tentatives c'est qu'il y a un sérieux problème
      throw new Exception("myDelete error");
   }
   
   public GluApp() {}
   
   /** Fournit un Comparator de mouvement pour les tris */
   static protected Comparator getComparator() { return new GluApp(); }

   public int compare(Object o1, Object o2) {
      GluApp a1 = (GluApp)o1;
      GluApp a2 = (GluApp)o2;
      if( a1.ordre==a2.ordre ) return 0;
      if( a1.ordre==null ) return -1;
      if( a2.ordre==null ) return 1;
      return a1.ordre.compareTo(a2.ordre);
   }

}
