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

import java.net.*;
import java.awt.Dimension;
import java.awt.Point;
import java.io.*;
import java.util.*;

import java.lang.reflect.Method;

import cds.tools.Util;
import cds.vizier.VizieRQuery;

/**
 * Objet gerant les interactions avec le Generateur de Liens Uniformes (GLU). Un
 * objet GLU permet de resoudre des marques GLU, soit en utilisant le
 * "glufilter" local dans le cas d'une session standalone soit en accedant a
 * distance a un proxyGLU qui se chargera de faire la resolution GLU et de
 * retourner le resultat.
 * <P>
 * Chaque resolution GLU (standalone ou non) generera un log distant via une
 * requete HTTP
 * <P>
 * <B>Rq </B> Dans le cas ``standalone'' il est indispensable que le dico GLU
 * additionnel pour Aladin JAVA se trouve dans le repertoire d'installation et
 * se nomme ALAGLU
 * @author Pierre Fernique [CDS]
 * @version 2.4 : oct 2012 - correction $n substitution avant le '?' dans une URL
 * @version 2.3 : sept 2007 - Peut charger des fichiers glu locaux
 * @version 2.2 : sept 2005 - Gère en STANDALONE la recherche du site NPHGLU le
 *          plus proche
 * @version 2.1 : sept 2005 - Supprime les Servers redondants (issus du GLU)
 * @version 2.0 : juin 2005 - Support de Konqueror sous Linux via dcop
 * @version 1.9 : avril 2005 - Prise en compte des Mac pour ouvrir un Browser
 *          (Mac OS X uniquement)
 * @version 1.8 : déc 2004 - ajout de FireFox
 * @version 1.7 : 18 jan 04 - utilisation de nph-glu.pl pour charger des
 *          additifs au dico local
 * @version 1.6 : 16 dec 02 - Prise en compte de Mozilla
 * @version 1.5 : 13 mai 02 - Adaptation pour utilisation hors package Aladin
 * @version 1.4 : 4 mars 02 - Meilleur gestion de l'acces au dico GLU distant
 * @version 1.3 : 20 mars 00 - gestion d'un log asynchrone
 * @version 1.2 : 2 juin 99 - Demarrage du browser sous Windows + bug cutParam
 * @version 1.1 : 26 mai 99 - Alternative du Glu en java
 * @version 1.0 : (5 mai 99) - Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Glu implements Runnable {

   // Profondeur max de la récursivité des définitions GLU (%I..)
   private static int MAXPROF = 6;

   /** URL d'envoi des logs */
   protected static String LOGSCRIPT = "nph-alalog.pl";

   protected static String ALADINLOG = "http://aladin.u-strasbg.fr/java/"
         + LOGSCRIPT;

   protected static String NPHGLU = "nph-glu.pl";

   protected static String NPHGLUALADIN = "http://aladin.u-strasbg.fr/java/"
         + NPHGLU;
//   protected static String NPHGLUALADIN = "vizier.iucaa.ernet.in/"
//      + NPHGLU;

   // Browsers testés (pour le cas Unix)
   static private String KONQUEROR = "konqueror";

   private String[] BROWSERS = { "firefox", "mozilla", "netscape", "Netscape",
         KONQUEROR, "google-chrome" };

   static String BROWSER = null;

   public Aladin aladin; // Reference

   Thread thread; // Thread pour mettre a jour le log

   String param; // Les elements du log

   static int nWindow = 0; // Numero de la fenetre netscape

   static boolean newWindow = false; // true - showDocument dans une nouvelle
                                     // fenetre

   /**
    * Tableau associatif contenant le dico GLU additionnel Aladin sous la forme
    * id -> URL avec des $nn
    */
   protected Hashtable aladinDic = null;
   protected Hashtable aladinDicFilters = null;

   /** Memorisation des serveurs definis par le dictionnaire GLU */
   protected static Vector vGluServer;
   
   /** Memorisation des applications PLASTICs définies par le dictionnaire GLU */
   protected static Vector vGluApp;
   
   /** Mémorisation des ciels de background définis par le dictionnaire GLU */
   protected static Vector vGluSky;
   
   /** Mémorisation des items des Tree outreach définis par le dictionnaire GLU */
   protected static Vector vGluCategory;
   
   /** Tri des listes */
   protected void tri() {
      Comparator c = TreeNode.getComparator();
      Collections.sort(vGluSky,c);
      Collections.sort(vGluCategory,c);
      
      c = Server.getComparator();
//      Collections.sort(vGluApp,c);
      Collections.sort(vGluServer,c);
   }

   /**
    * Creation de l'objet GLU. <BR>
    * En mode Standalone, met a jour la variable ``glufilter'' (cherche dans le
    * repertoire d'installation Aladin.HOME, puis dans /usr/local/glu)
    * @param aladin reference
    */
   protected Glu(Aladin aladin) {
      this.aladin  = aladin;
      aladinDic    = new Hashtable();
      vGluServer   = new Vector(50);
      vGluApp      = new Vector(10);
      vGluSky      = new Vector(10);
      vGluCategory = new Vector(10);

      // Peut être un site GLU défini dans la configuration utilisateur
      String nphGlu = aladin.configuration.get(Configuration.GLU);
      if( nphGlu != null ) NPHGLUALADIN = nphGlu;

      // Methode propre a la version Standalone
      if( Aladin.STANDALONE ) {

         // Lecture du dico GLU propre a Aladin
         try {
            if( Aladin.NETWORK ) {
               DataInputStream dis = new DataInputStream(getClass()
                     .getResourceAsStream("/" + Aladin.ALAGLU));
               loadGluDic(dis,false,false);
               dis.close();
               testNetwork();
//               if( !testCurrentAlaSite() && !testAlaSites(true, false) ) {
//                  Aladin.info(aladin.chaine.getString("NONET"));
//                  // Aladin.NETWORK=false; JE PREFERE NE PAS CACHER LES
//                  // RESSOURCES DISTANTESS (POUR DEMO)
//               } else flagNoGlu=true;
            }
         } catch( Exception e ) {
            Aladin.warning("AlaGlu.dic not found !", 1);
         }
         
         // Un Dico GLu pour décrire les applications VOtools ?
         addVOGluFile();
         
         // Des dico GLU locaux ?
         addOtherGluFiles();
      }

      // Recuperation des enregistrements GLU des serveurs additionnels
      if( Aladin.NETWORK ) getRemoteGluDic();
      
      // Et on tri le tout selon %Aladin.MenuNumber
      tri();
      
   }
   
   /** Test asynchrone du réseau */
   protected void testNetwork() {
      (new Thread("Network test") {
            @Override
            public void run() {
               try {
                  Aladin.trace(3,"Testing network...");
                  if( !testCurrentAlaSite() && !testAlaSites(true, false) ) {
                     Aladin.info(aladin.chaine.getString("NONET"));
                     // Aladin.NETWORK=false; JE PREFERE NE PAS CACHER LES
                     // RESSOURCES DISTANTESS (POUR DEMO)
                  } else flagNoGlu=true;
               } catch( Exception e) {
                  Aladin.info(aladin.chaine.getString("No full network access !"));
                  flagNoGlu=true;
               }
            }
      }).start();
   }
   
   /** Rechargement complet du GLU avec reconstructions des menus et
       * des frames associés (nettoyage préalable du cache)
       */
   void reload() { reload(true); }
   void reload(boolean clearBefore) {
      if( clearBefore ) {
         aladin.cache.clear();
         aladin.glu  = new Glu(aladin);
      }
      ServerDialog oldDialog = aladin.dialog;
      
      Point p = null;
      Dimension d=null;
      int c = aladin.dialog.current;
      try {
         aladin.dialog.getLocationOnScreen(); 
         d = aladin.dialog.getSize();
      } catch( Exception e ) { p=null; }
      VizieRQuery.resetKeywords();
      
      aladin.dialog = new ServerDialog(aladin);
      aladin.dialog.setCurrent(c);
      if( p!=null ) {
         aladin.dialog.flagSetPos=true;
         aladin.dialog.setLocation(p);
         aladin.dialog.setSize(d);
      }
      if( oldDialog.isVisible() ) aladin.dialog.showNow();
      oldDialog.dispose();
   }
   
   /** Ajout d'éventuels dico glu locaux. Parcours la variable Aladin.GLUFILE qui contient
    * la liste des filenames des dico additionnals. Le filename peut être un nom de fichier
    * complet ou un nom de fichier qui doit se trouver dans le CLASSPATH */
   private void addOtherGluFiles() {
      if( Aladin.GLUFILE==null ) return;
      StringTokenizer st = new StringTokenizer(Aladin.GLUFILE,";");
      while( st.hasMoreTokens()) {
         String filename = st.nextToken();
         try {
            File f = new File(filename);
            DataInputStream dis;
            boolean localFile=false;
            try {
               InputStream in=null;
               if( filename.startsWith("http://") ) in=Util.openStream(filename);
               else { in=new FileInputStream(f); localFile=true; }
               dis=new DataInputStream(in);
            } catch( Exception e ) {
               dis = new DataInputStream(getClass().getResourceAsStream("/"+filename));
            }
            if( dis==null ) throw new Exception(); 
            if( loadGluDic( dis, false,localFile) ) Aladin.trace(1,"Additionnal "+(localFile?"local":"remote")+" Glu dic loaded ["+filename+"]");
            else throw new Exception();
            dis.close();
         } catch( Throwable e ) {
            System.err.println("Cannot load the Glu dictionary: ["+filename+"]");
         }
      }
   }

   // Paramètre pour récupérer le dico GLU en format parfile court, avec
   // les mirroirs et distribué dans le domaine ALADIN
   // voir getRemoteGluDic();
   static private final String GLUDICPARAM = "?param=-p+-a+-w+Z:ALADIN";

   /**
    * Lancement de la recuparation des enregistrements GLU qui se trouvent sur
    * le site GLU le plus proche
    */
   private void getRemoteGluDic() {
      DataInputStream dis;
      URL url = null;

      try {
         if( Aladin.STANDALONE ) url = new URL(NPHGLUALADIN + GLUDICPARAM);
         else url = new URL(Aladin.CGIPATH + "/" + NPHGLU + GLUDICPARAM);

         Aladin.trace(1, "Loading the remote glu dictionary");
         Aladin.trace(3, "  => " + url);
         InputStream in = aladin.cache.get(url);
         dis = new DataInputStream(in);
         loadGluDic(dis,true,false);
         try { dis.close(); } catch( Exception e) {}
         
      } catch( Exception e ) {
         System.err.println("Remote Glu dictionary not reached");
      }

   }

   // Ajout des quotes simples et prefixe par \ des quotes internes
   // --> Entree : la chaine a traiter
   // --> Retour : la chaine traitee
   //
   static String quote(String s) {
      char a[] = s.toCharArray();
      StringBuffer res = new StringBuffer("'");
      for( int i=0; i<a.length; i++ ) {
         if( a[i]=='\'' ) res.append('\\');
         res.append(a[i]);
      }
      res.append('\'');
      return res.toString();
   }
   
   
   // Vieux code que je garde pour mémoire
//   static String quote(String s) {
//         static String quote(String s) {
//      boolean first = true;
//      StringBuffer res = new StringBuffer("'");
//      StringTokenizer st = new StringTokenizer(s, "'");
//      while( st.hasMoreTokens() ) {
//         if( first ) first = false;
//         else res.append("\\'");
//         res.append(st.nextToken());
//      }
//      res.append("'");
//      return res.toString();
//   }

//  JE GARDE LE CODE POUR MEMOIRE
//
//  /** Appel au GLU local (mode standalone).
//    *  L'appel se fera au moyen d'une commande exec
//    *
//    * @param tag le tag GLU a resoudre
//    * @return l'URL resolue (mais sous forme de String), ou null si probleme
//    */
//   final String gluFilter(String tag) {
//
//      Process p; // Le process
//      StringBuffer url=new StringBuffer(""); // Le resultat
//      int b; // pour lire chaque caractere de la reponse
//      InputStream in; // flux de la reponse
//      String[] cmd=new String[4]; // Tableau des arguments du exec
//
//      try {
//         // preparation de la commande
//         cmd[0]=GLUFILTER;
//         cmd[1]="-e";
//         cmd[2]="-r";
//         cmd[3]=tag;
//         Aladin.trace(3,"Querying the local GLU: " + cmd[0] + " -e -r '"
//               + cmd[3] + "' ...");
//
//         // Creation du process associe au glufilter
//         p=Runtime.getRuntime().exec(cmd);
//         in=p.getInputStream();
//
//         // Lecture du resultat, caractere par caractere
//         while( (b=in.read()) != -1 )
//            url.append((char) b);
//
//      } catch( Exception e ) {
//         System.err.println("Glufilter error : " + e);
//         return null;
//      }
//
//      // Detection des erreurs
//      String u=url.toString();
//      if( u.startsWith("<*") ) {
//         Aladin.trace(3,"WARNING: " + u);
//         return null;
//      }
//
//      return u;
//   }
//

   /*
    * Recherche d'un browser préféré par l'utilisateur ou Test de l'existence
    * d'un browser compatible par essais successifs de lancement avec l'option
    * "-v" Positionne la variable de classe BROWSER en fonction du resultat Si
    * non trouve, BROWSER est initialise a ""
    */
   private void setBrowser() {
      String[] cmd = new String[2];
      Process p;

      // Y aurait-il une préférence de l'utilisateur ?
      String defaultBrowser = aladin.configuration.get(Configuration.BROWSER);
      if( defaultBrowser != null ) {
         BROWSER = defaultBrowser;
         return;
      }

      // On teste les browsers possibles
      for( int i = 0; i < BROWSERS.length; i++ ) {

         cmd[0] = BROWSERS[i];
         cmd[1] = "-v";
         //System.out.println("Trying: "+cmd[0]+" "+cmd[1]);

         try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            int res = p.exitValue();
            // System.out.println("Erreur code => "+res);
            if( res == 0 ) {
               BROWSER = BROWSERS[i];
               return;
            }
         } catch( Exception e ) {
         }
      }
      Aladin.warning(aladin.chaine.getString("NOBROWSER"));
      BROWSER = "";
   }

   /**
    * Gestion de la prochaine fenetre Browser. Positionne le flag pour
    * determiner si la prochaine fenetre browser a creer dot etre une nouvelle
    * fenetre ou s'il faut reutiliser la precedente
    * @param newWindow <I>true </I>: il faudra une nouvelle fenetre, <I>false
    *           </I>: il n'en faut pas
    */
   protected void newWindow(boolean newWindow) {
      this.newWindow = newWindow;
   }

   /**
    * Affichage d'un document HTML via une marque GLU.
    * @see aladin.Glu#showDocument(java.lang.String, java.lang.String, boolean)
    */
   protected void showDocument(String id, String params) {
      showDocument(id, params, false);
   }

   // Pour le lancement du Browser sous Mac OS X
   private static Method openURL = null;
   
   /** Affichage d'une URL - NE MARCHE QU'EN STANDALONE */
   protected void showDocument(String url) {
      if( !Aladin.STANDALONE ) return;
      try { showDocument(new URL(url)); }catch( Exception e ) { }
      return;
   }

   /**
    * Affichage d'un document HTML via une marque GLU. Dans le cas
    * ``standalone'' on va se baser uniquement sur Netscape et demander soit
    * dans demarrer un, soit de faire afficher la page souhaitee par le Netscape
    * courant. Pour cela, on utilise la commande -remote de Netscape qui permet
    * d'acceder au Display X pour interragir avec le Client X Netscape courant.
    * <P>
    * Pour la version Applet, une simple utilisation de showDocument() resoud
    * une partie des problemes. En revanche il va etre necessaire d'utiliser le
    * GLU en ``rebond'' afin de contourner la politique de securite JAVA. Pour
    * cela le dico GLU additionnel pour Aladin contient une marque ``http'' qui
    * prend en parametre une URL. Il suffit donc de demander cette marque-la
    * avec l'option de resolution immediate pour s'en sortir (un peu sioux mais
    * efficace)
    */
   protected void showDocument(String id, String params, boolean encode) {
      URL url = getURL(id, params, encode); // Resolution GLU
      if( url == null ) return;
      showDocument(url);
   }
   private  void showDocument(URL url) {
      String window = "";
 
      if( !Aladin.isApplet() ) {
         Process p;
         String[] cmd = new String[3];

         // Dans une nouvelle fenetre netscape ?
         if( newWindow ) {
            window = ",aladin-" + nWindow;
            nWindow++;
         }

         // On determine le type de plate-forme
         String syst = System.getProperty("os.name");
         Aladin.trace(1, "Launching the browser for [" + syst + "]");

         // Dans le cas Windows
         if( syst != null && syst.startsWith("Windows") ) {

            try {
               // preparation de la commande "rundll32
               // url.dll,FileProtocolHandler url"
               cmd[0] = "rundll32";
               cmd[1] = "url.dll,FileProtocolHandler";
               cmd[2] = url.toString();
               Aladin.trace(2, "trying: " + cmd[0] + " " + cmd[1] + " "
                     + cmd[2]);

               // Creation du process associe au browser
               p = Runtime.getRuntime().exec(cmd);
            } catch( Exception e1 ) {
               Aladin.warning(aladin.chaine.getString("ERRORBROWSER")+" " + e1);
            }

            // Pour les macs
         } else if( syst != null && syst.startsWith("Mac") ) {
            try {
               if( openURL == null ) {
                  Class mrjClass = Class.forName("com.apple.mrj.MRJFileUtils");
                  openURL = mrjClass.getDeclaredMethod("openURL",
                        new Class[] { String.class });
               }
               openURL.invoke(null, new Object[] { url.toString() });
            } catch( Exception emac ) {
               Aladin.warning(aladin.chaine.getString("ERRORBROWSER")+" " + emac);
            }

            // Dans le cas Unix (on prend le premier browser compatible qui est
            // trouvé)
         } else {
            if( BROWSER == null ) setBrowser();
            if( BROWSER.length() == 0 ) return;

            try {

               // Konqueror. dcop permet de savoir s'il y a un konqueror déjà
               // actif,
               // on va ainsi récupérer son identificateur pour relancer une
               // deuxième
               // commande dcop konqueror-nnn konqueror-mainwindow#1 openURL url
               if( BROWSER.indexOf(KONQUEROR) >= 0 ) {
                  String c = "dcop " + KONQUEROR + "*";
                  p = Runtime.getRuntime().exec(c);

                  // Récupération de l'identificateur d'un éventuel konqueror
                  // déjà actif
                  InputStream in = p.getInputStream();
                  StringBuffer konqID = new StringBuffer();
                  byte b;
                  while( (b = (byte) in.read()) != -1 && b != '\r' && b != '\n' )
                     konqID.append((char) b);

                  // J'attend la fin du process
                  p.waitFor();

                  // Il y a effectivement un konqueror déjà actif
                  if( p.exitValue() == 0
                        && konqID.toString().indexOf(KONQUEROR) >= 0 ) {
                     c = "dcop " + konqID + " konqueror-mainwindow#1 openURL "
                           + url;
                     Aladin.trace(2, "Trying: " + c);
                     p = Runtime.getRuntime().exec(c);

                     // J'attend à nouveau la fin du process
                     p.waitFor();

                     if( p.exitValue() == 0 ) return;
                  }

                  // On essaye de demarrer un nouveau KONQUEROR
                  Aladin.trace(2, "Trying: " + KONQUEROR + " " + url);

                  // Creation du process KONQUEROR
                  p = Runtime.getRuntime().exec(
                        KONQUEROR + " " + url.toString());
               // Chrome
               } else if (BROWSER.indexOf("chrome")>=0) {
                   String myCmd = BROWSER+" "+url.toString();
                   Aladin.trace(2, myCmd);
                   p = Runtime.getRuntime().exec(myCmd);
                   p.waitFor();

                   if( p.exitValue() == 0 ) return;

                   throw new Exception("Can not launch Chrome browser !");

                  // Netscape et dérivés. On essaye d'abord l'option -remote
                  // openURL(url),
                  // si ça ne marche pas, on lance un nouveau browser
               } else {

                  // preparation de la commande "netscape -remote
                  // openURL(url,nn)"
                  cmd[0] = BROWSER;
                  cmd[1] = "-remote";
                  cmd[2] = "openURL(" + url.toString() + window + ")";
                  Aladin.trace(2, "Trying: " + cmd[0] + " " + cmd[1] + " "
                        + cmd[2]);

                  // Creation du process associe au netscape remote
                  p = Runtime.getRuntime().exec(cmd);

                  // J'attend sa fin
                  p.waitFor();

                  if( p.exitValue() == 0 ) return;

                  // On essaye de demarrer un nouveau netscape
                  Aladin.trace(2, "Trying: " + cmd[0] + " " + url);

                  // Creation du process netscape
                  p = Runtime.getRuntime().exec(BROWSER + " " + url.toString());
               }
            } catch( Exception e ) {
               System.out.println("Browser launching problem : " + e);
            }
         }
         return;
      }

      // Pour le mode Applet
      Aladin.trace(1, "Opening a new browser page");
      try {
         if( newWindow ) aladin.getAppletContext().showDocument(url, window);
         else aladin.getAppletContext().showDocument(url, "aladin");
      } catch( Exception e ) {
         System.err.println("showDocument() error : " + e);
      }
   }

   /**
    * Cherche la premiere lettre qui soit un espace, un \t, ou un \n.
    * @param a Le tableau de caracteres
    * @param i l'offset courant dans le tableau
    * @return Le nouvel offset ou -1 si fin de chaine
    */
   static int afterWord(char[] a, int i) {
      while( i < a.length && a[i] != ' ' && a[i] != '\t' && a[i] != '\n' )
         i++;
      return (i == a.length) ? -1 : i;
   }

   /**
    * Cherche la premiere lettre qui ne soit pas un espace, un \t, ou un \n.
    * @param a Le tableau de caracteres
    * @param i l'offset courant dans le tableau
    * @return Le nouvel offset ou -1 si fin de chaine
    */
   static int afterSpace(char[] a, int i) {
      while( i < a.length && (a[i] == ' ' || a[i] == '\t' || a[i] == '\n') )
         i++;
      return (i == a.length) ? -1 : i;
   }

   /**
    * Recherche le nom d'un champ GLU.
    * @param s La ligne a analyser
    * @return Le nom du champ GLU ou null si non trouve
    */
   static String getName(String s) {
      char[] a = s.toCharArray();
      int i;

      if( a[0] != '%' ) return null;
      if( (i = afterWord(a, 1)) == -1 ) return null;
      return new String(a, 1, i - 1);
   }

   /**
    * Recherche la valeur d'un champ GLU. <BR>
    * <B>ATTENTION : </B> Ne prend pas en compte les valeurs de champs qui
    * seraient repliees sur plusieurs lignes sans le signe \ en continuation
    * @param s La ligne a analyser
    * @param dis Flux de donnees (en cas de continuation sur plusieurs lignes
    * @return La valeur champ GLU ou null si non trouve
    */
   static String getValue(String s1, DataInputStream dis) {
      int i;
      StringBuffer res = new StringBuffer(); // Pour construire sur plusieurs
                                             // lignes
      String s = new String(s1); // Pour ne pas faire d'effet de bord
      char[] a = s.toCharArray();

      // On passe le nom du champ et les blancs qui suivent
      if( (i = afterWord(a, 0)) == -1 ) return null;
      if( (i = afterSpace(a, i)) == -1 ) return null;

      // S'agit-il d'une ligne avec continuation ?
      while( a[a.length - 1] == '\\' ) {

         // Memorisation de la ligne courante
         res.append(a, i, a.length - i - 1);

         // Lecture de la ligne suivante
         try {
            do {
               s = dis.readLine();
            } while( s != null
                  && (s.charAt(0) == '#' || s.trim().length() == 0) );
            if( s == null ) break;
         } catch( Exception e ) {
            s = null;
            break;
         }
         a = s.toCharArray();

         // On laisse les blancs en debut de ligne
         i = afterSpace(a, 0);
      }

      // On ajoute la derniere ligne qui n'est pas en continuation
      // si ce n'est pas deja fait
      if( s != null ) res.append(a, i, a.length - i);

      return res.toString();
   }

   /**
    * Recuperation du numero du parametre dans une chaine GLU %Param.XXX
    * @param s la chaine au format "$nn=xxx" ou "nn:xxx"
    * @return le numero du parametre (nn) ou "" s'il y a un probleme
    */
   static String getNumParam(String s) {
      char[] a = s.toCharArray();
      int i, j;

      for( i = 0; i < a.length && (a[i] < '0' || a[i] > '9'); i++ )
         ;
      for( j = i + 1; j < a.length && a[j] >= '0' && a[j] <= '9'; j++ )
         ;
      return (j < a.length) ? new String(a, i, j - i) : "";
   }

   /**
    * Recuperation de la valeur du parametre dans une chaine GLU %Param.XXX
    * @param s la chaine au format "$nn=xxx" ou "nn:xxx"
    * @return la valeur du parametre (xxx) ou la totalite de la chaine s'il y a
    *         un probleme.
    */
   static String getValParam(String s) {
      char[] a = s.toCharArray();
      int i;

      for( i = 0; i < a.length && a[i] != ':' && a[i] != '='; i++ )
         ;
      return (i < a.length) ? new String(a, i + 1, a.length - i - 1) : s;
   }
   
   /**
    * Retourne l'indice du ciel GLU dans la liste des ciels GLU connus
    * @param A le tagGlu du ciel à chercher
    * @param flagSubstring true si on prend en compte le cas d'une sous-chaine
    * @param mode 0 - match exact
    *             1 - substring
    *             2 - match exact puis substring dernier élément du menu  (ex DssColored ok pour Optical/DSS/DssColored)
    * @return l'indice du ciel dans Glu.vGluSky, sinon -1
    */
   protected int findGluSky(String A) { return findGluSky(A,0); }
   protected int findGluSky(String A,int mode) {
      for( int i = vGluSky.size()-1; i >=0; i-- ) {
         TreeNodeAllsky gs = (TreeNodeAllsky) vGluSky.elementAt(i);
         if( A.equals(gs.id) || A.equals(gs.label) ) return i;
         if( mode==1 && Util.indexOfIgnoreCase(gs.label, A)>=0 ) return i;
         if( mode==2 ) {
            int offset = gs.label.lastIndexOf('/');
            if( A.equals(gs.label.substring(offset+1)) ) return i;
         }
      }
      
      if( mode==2 ) {
         for( int i = vGluSky.size()-1; i >=0; i-- ) {
            TreeNodeAllsky gs = (TreeNodeAllsky) vGluSky.elementAt(i);
            int offset = gs.label.lastIndexOf('/');
            if( Util.indexOfIgnoreCase(gs.label.substring(offset+1),A)>=0 ) return i;
         }
      }
      return -1;
   }
   
   /** Ajout ou remplacement d'un glusky */
   public void addGluSky(TreeNodeAllsky gsky) {
      String A = gsky.id;
      int i = findGluSky(A);
      if( i!= -1 ) vGluSky.setElementAt(gsky, i);
      else vGluSky.addElement(gsky);
   }

   /**
    * Supprime un ciel GLU de la liste des ciels GLU connus
    * @param actionName le tag Glu du ciel à supprimer
    * @return true si la suppression a été effectuée, false sinon
    */
   protected boolean removeGluSky(String actionName) {
      int i = findGluSky(actionName);
      if( i == -1 ) return false;
      vGluSky.removeElementAt(i);
      return true;
   }
   
   /** Retourne la description du ciel d'indice i */
   protected TreeNodeAllsky getGluSky(int i) {
      return (TreeNodeAllsky)vGluSky.elementAt(i);
   }

   /**
    * Retourne l'indice du serveur GLU dans la liste des serveurs GLU connus
    * @param actionName le tag Glu du serveur à chercher
    * @return l'indice du serveur dans Glu.vGluServer, sinon -1
    */
   private int findGluServer(String actionName) {
      for( int i = vGluServer.size()-1; i >=0; i-- ) {
         ServerGlu gs = (ServerGlu) vGluServer.elementAt(i);
         if( actionName.equals(gs.actionName) ) return i;
      }
      return -1;
   }

   /**
    * Supprime un serveur GLU de la liste des serveurs GLU connus
    * @param actionName le tag Glu du serveur à supprimer
    * @return true si la suppression a été effectuée, false sinon
    */
   protected boolean removeGluServer(String actionName) {
      int i = findGluServer(actionName);
      if( i == -1 ) return false;
      vGluServer.removeElementAt(i);
      return true;
   }
   
   /**
    * Retourne l'indice de l'application GLU dans la liste des applications GLU connues
    * @param actionName le tag Glu ou le menu de l'application à chercher
    * @return l'indice du serveur dans Glu.vGluApp, sinon -1
    */
   protected int findGluApp(String actionName) {
      for( int i = vGluApp.size()-1; i>=0 ; i-- ) {
         GluApp gs = (GluApp) vGluApp.elementAt(i);
         if( actionName.equals(gs.tagGlu) ) return i;
         if( gs.aladinLabel!=null && actionName.equals(gs.aladinLabel) ) return i; 
      }
      return -1;
   }
   
   /**
    * Retourne l'application GLU dans la liste des applications GLU connues
    * @param actionName le tag Glu de l'application à chercher
    * @return l'application sinon null
    */
   protected GluApp getGluApp(String actionName) {
      for( int i = vGluApp.size()-1; i>=0 ; i-- ) {
         GluApp gs = (GluApp) vGluApp.elementAt(i);
         if( actionName.equals(gs.tagGlu) ) return gs;
      }
      return null;
   }
   
   /** Retourne la description de l'application d'indice i */
   protected GluApp getGluApp(int i) {
      return (GluApp)vGluApp.elementAt(i);
   }

   /**
    * Supprime l'application de la liste des applications GLU connues
    * @param actionName le tag Glu de l'application à supprimer
    * @return true si la suppression a été effectuée, false sinon
    */
   protected boolean removeGluApp(String actionName) {
      int i = findGluApp(actionName);
      if( i == -1 ) return false;
      vGluApp.removeElementAt(i);
      return true;
   }
   
   /** Mémorise les informations des applications VO dans un petit fichier GLU
    * écrit dans le répertoire cache d'Aladin (.aladin/VOTools) */
   public boolean writeGluAppDic() {
      try {
         String file = aladin.getVOPath()+Util.FS+"VOTools.dic";
         File f = new File(file);
         f.delete();
         RandomAccessFile out = new RandomAccessFile(file,"rw");
         
         // Les VOtools (vGluApp)
         Enumeration e = vGluApp.elements();
         while( e.hasMoreElements() ) {
            GluApp vo = (GluApp)e.nextElement();
            out.writeBytes(vo.getGluDic());
         }
         
         // On en profite pour ajouter les GSky (vGluSky)
         e = vGluSky.elements();
         while( e.hasMoreElements() ) {
            TreeNodeAllsky gs = (TreeNodeAllsky)e.nextElement();
            if( !gs.isLocalDef()  ) continue;
            out.writeBytes(gs.getGluDic());
         }

         out.close();
         aladin.trace(3,file+" successfully saved");
         return true;
      } catch( Exception e ) { 
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return false;
   }
   
   /** Lecture des informations des applications VO mémorisées dans un fichier
    * GLU dans le cache d'Aladin */
   private void addVOGluFile() {
      try {
         String file = aladin.getVOPath()+Util.FS+"VOTools.dic";
         File f = new File(file);
         DataInputStream dis=new DataInputStream( new FileInputStream(f));
         if( dis==null ) throw new Exception(); 
         if( loadGluDic( dis, false,true) ) Aladin.trace(1,"VOTools Glu dic loaded ["+file+"]");
         else throw new Exception();
         dis.close();
      } catch( Throwable e ) { }
   }


   
   /**
    * Mémorisation dans le vecteur vGluApp des applications connues par Aladin
    * via le dictionnaire GLU
    */
   private void memoApplication(String actionName,String aladinLabel,String aladinMenuNumber,String description,
         String verboseDescr,String institute,String releaseNumber,String copyright,
         String docUser,String jar,String javaParam,String download,String webstart,String applet,
         String dir,String aladinActivated,String system) {

      GluApp vo = getGluApp(actionName);
      if( vo!=null ) {
         vo.merge(aladinLabel,aladinMenuNumber,description,verboseDescr,institute,releaseNumber,
               copyright,docUser,jar,javaParam,download,webstart,applet,aladinActivated,
               GluApp.JAVA|GluApp.PLASTIC);
      } else {
         vGluApp.addElement( new GluApp(aladin,actionName,aladinLabel,aladinMenuNumber,description,verboseDescr,
               institute,releaseNumber,copyright,docUser,jar,javaParam,dir,download,webstart,
               applet,aladinActivated,system,GluApp.JAVA|GluApp.PLASTIC) );
      }
   }

   /** Ajout d'une application vide */
   protected GluApp addApplication(String actionName) {
      removeGluApp(actionName);
      GluApp ap = new GluApp(aladin,actionName);
      vGluApp.addElement(ap);
      return ap;
   }

   /**
    * Génère la liste des menus des applications compatibles Aladin (Plastics)
    */   
   protected String[] getAppMenu() {
      String menu[] = new String[ vGluApp.size() ];
      Enumeration e = vGluApp.elements();
      for( int i=0; e.hasMoreElements(); i++ ) {
         GluApp ga = (GluApp)e.nextElement();
         if( !ga.canBeMenu() ) continue;
         menu[i] = Util.slash( ga.aladinLabel!=null ? ga.aladinLabel : ga.tagGlu );
      }
      return menu;
   }
   
   /**
    * Génère la liste des menus des ciels décrits par le GLU
    */   
   protected String[] getGluSkyMenu() {
      String menu[] = new String[ vGluSky.size() ];
      Enumeration e = vGluSky.elements();
      for( int i=0; e.hasMoreElements(); i++ ) {
         TreeNodeAllsky ga = (TreeNodeAllsky)e.nextElement();
         menu[i] = ga.path!=null ? ga.path : ga.label;
      }
      return menu;
   }
   
   
   /** 
    * Memorisation dans le Vecteur vGluSky des Ciels definis au moyen du
    * dictionnaire GLU propre a Aladin
    */
   private void memoGluSky(String actionName,String aladinLabel,String aladinMenuNumber,String url,String description,
         String verboseDescr,String aladinProfile,String copyright,String copyrightUrl,String aladinTree,String aladinSurvey,String aladinHpxParam) {
      
      // Pour éviter les doublons
      int find = findGluSky(actionName);
      if( find>=0 ) {
         System.err.println("Allsky [" + actionName + ":" + description
               + "] redefined => Aladin will use the last one (remote)");
      }
      
      // Pour incompatibilité avec version <=6.100
      boolean incompatible=false;
      if( aladinSurvey!=null || aladinLabel==null || aladinLabel.indexOf('/')>=0 ) {
         int i;
         if( aladinLabel!=null && (i=aladinLabel.lastIndexOf('/'))>0 ) {
            aladinTree=aladinLabel.substring(0,i);
            aladinLabel=aladinLabel.substring(i+1);
         }
         if( aladinSurvey!=null ) {
            char c = url.charAt(url.length()-1);
            if( c!='/' && c!='\\' ) url = url+ '/';
            url = url + aladinSurvey;
         }
         if( aladinHpxParam==null ) aladinHpxParam="map";
         incompatible=true;
      }
      
      // Construction du path pour l'arbre (noeud terminal inclus)
      String path = aladinTree==null ? aladinLabel : aladinTree+"/"+aladinLabel;
      
      TreeNodeAllsky tn =  new TreeNodeAllsky(aladin,actionName,aladinMenuNumber,url,aladinLabel,
            description,verboseDescr,aladinProfile,copyright,copyrightUrl,path,aladinHpxParam);
      
      if( find<0 ) vGluSky.addElement(tn);
      else vGluSky.setElementAt(tn,find);
      
      if( incompatible ) {
         System.err.println("Uncompatible GLU allsky record ["+actionName+"] => translated\nBut please update it:\n" +
               getGluSky( findGluSky(actionName) ).getGluDic());
      }
   }
   
   /** Memorisation des noeuds pour l'arbre d'outreach (cf TreeServer)
    * Si ID déjà existant, on remplace le précédent
    */
   private void memoTree(String actionName,String description,String aladinTree,String url, 
         String docUser, String aladinUrlDemo) {
      // POUR LE MOMENT ON NE TRAITE QUE LES TREE AVEC WP5:/ EN PREFIXE
      if( aladinTree==null || !aladinTree.startsWith("WP5:/") ) return;      
      aladinTree=aladinTree.substring(5);
      
      Enumeration e=vGluCategory.elements();
      while( e.hasMoreElements() ) {
         TreeNode n = (TreeNode)e.nextElement();
         if( n.id.equals(actionName) ) { vGluCategory.remove(n); break; }
      }
      vGluCategory.addElement(new TreeNodeCategory(aladin,actionName,description,
            aladinTree,url,docUser,aladinUrlDemo));
   }
    
   /**
    * Memorisation dans le Vecteur vGluServer des Serveurs definis au moyen du
    * dictionnaire GLU propre a Aladin
    * @param actionName L'identificateur GLU -> nom du serveur
    * @param description Description du serveur
    * @param verboseDescr Description verbose du serveur
    * @param aladinMenu Label du sous-menu Popup ou null si aucun
    * @param aladinMenuNumber indice de l'ordre du menu ou sous-menu
    * @param aladinLabel Label du bouton ou null si aucun
    * @param aladinLabelPlane Label du plan ou null si aucun
    * @param docUser URL vers la doc utilisateur associee
    * @param paramDescription1 Descriptions de chaque parametre (cle 1,2,3...)
    * @param paramDataType1 Type de chaque parametre (cle 1,2,3...)
    * @param paramValue1 Valeurs de chaque parametre (cle 1,2,3..., separateur \t) s'il
    *           n'y a qu'une valeur, il s'agit d'une valeur par defaut, s'il y
    *           en a plusieurs, c'est la liste des valeurs possibles, sinon ce
    *           sera un champ texte libre.
    * @param resultDataType Format des données retournés par le serveur
    * @param institute Mention de l'origine
    * @param aladinFilter1 liste des filtres prédéfinis
    * @param aladinLogo Le logo associé (adresse HTTP)
    * @param record Simple copie de l'enregistrement GLU orginal
    */
   private void memoServer(String actionName, String description, String verboseDescr,
         String aladinMenu, String aladinMenuNumber,String aladinLabel,
         String aladinLabelPlane, String docUser, Hashtable paramDescription1, 
         Hashtable paramDataType1, Hashtable paramValue1,
         String resultDataType, String institute, Vector aladinFilter1,
         String aladinLogo,String dir,String system,StringBuffer record) {
      int i;

      // Pour éviter les doublons
      if( removeGluServer(actionName) ) {
         System.err.println("Server [" + actionName + ":" + description
               + "] redefined => Aladin will use the last one (remote)");
      }

      if( paramDescription1 == null ) return;
      int n = paramDescription1.size();
      String[] paramDescription = new String[n];
      for( i = 1; i <= n; i++ )
         paramDescription[i - 1] = (String) paramDescription1.get(i + "");

      String[] paramDataType = new String[n];
      for( i = 1; i <= n; i++ )
         paramDataType[i - 1] = (String) paramDataType1.get(i + "");

      String[] paramValue = new String[n];
      for( i = 1; i <= n; i++ )
         paramValue[i - 1] = (String) paramValue1.get(i + "");

      String aladinFilter[] = null;
      if( aladinFilter1 != null && (n = aladinFilter1.size()) > 0 ) {
         aladinFilter = new String[n];
         Enumeration e = aladinFilter1.elements();
         for( i = 0; i < n; i++ )
            aladinFilter[i] = (String) e.nextElement();
      }
      
      if( system!=null && system.trim().length()==0 ) system=null;
      if( institute == null ) institute = description;

      vGluServer.addElement(actionName.equals("SkyBoT.IMCCE") ? 
            new ServerSkybot(aladin, actionName, description, verboseDescr, aladinMenu, 
                  aladinMenuNumber, aladinLabel, aladinLabelPlane, docUser, paramDescription, paramDataType, paramValue, 
                  resultDataType, institute, aladinFilter, aladinLogo, record)
          : new ServerGlu(aladin, actionName, description, verboseDescr, aladinMenu, 
                  aladinMenuNumber, aladinLabel, aladinLabelPlane, docUser, paramDescription, paramDataType, paramValue, 
                  resultDataType, institute, aladinFilter, aladinLogo, dir, system, record));
   }

   static private String subCR(String s) {
      char[] a = s.toCharArray();
      int i, j;
      for( i = j = 0; i < a.length; i++, j++ ) {
         if( i < a.length - 1 && a[i] == '\\' && a[i + 1] == 'n' ) {
            a[j] = '\n';
            i++;
         } else a[j] = a[i];
      }
      return (new String(a, 0, j)).trim();
   }

   // en JVM 1.1.8, le readLine() de DataInputStream est buggué lorsque je
   // l'appelle
   // récursivement (voir %I si dessous) : il oublie les premiers caractères du
   // flux !!
   // J'ai donc contourné le problème en mémorisant le nom de l'enregistrement
   // GLU
   // qu'il faut recharger. si l'identificateur %A n'est pas connu à cause de
   // cette
   // troncature, je la reconstruits à partir de lastA
   private String lastA = null;

   /**
    * Chargement d'un dico GLU additionnel. - Mise a jour de tableau associatif
    * aladinDic - Mise a jour du Vector vGluServer pour les Servers additionels
    * definis au moyen d'enregistrements GLU
    * @param dis Flux de donnees
    * @param profondeur niveau de la récursivité pour éviter les cycles
    * @param testDomain true si on doit vérifier que le domaine de distribution
    *                   est bien ALADIN avant d'insérer le nouveau serveur
    * @return <I>true </I> Ok, <I>false </I> sinon.
    */
   protected boolean loadGluDic(DataInputStream dis,boolean testDomain,boolean localFile) {
      return loadGluDic(dis, 0,testDomain,true,localFile);
   }
   
   // Retourne true si cela correspond à un mot clé, éventuellement avec le suffixe de la langue 
   private boolean isKey(String s,String key) { return isKey(s,key,false); }
   private boolean isKey(String s,String key,boolean testLang) {
      if( testLang ) {
         String suf = aladin.configuration.getLang();
         if( suf.length()>0 && s.equalsIgnoreCase(key+suf) ) return true;
      }
      return s.equalsIgnoreCase(key);
   }

   protected boolean loadGluDic(DataInputStream dis, int profondeur,boolean testDomain,boolean overwrite,boolean localFile) {
      String name; // Le nom du champ courant
      String value; // La valeur du champ courant
      //      String A=null; // L'identificateur de l'enr courant
      String actionName = lastA; // L'identificateur de l'enr courant (voir rq ci-dessous
                        // bug JVM 1.1.8)
      String releaseNumber = null; // Numéro de release de l'application
      String copyright = null;  // Copyright
      String copyrightUrl = null;  // Copyright Url
      String jar = null;// Url du package jar
      String download = null;// Url de la page de download
      String webstart = null; // Url du lancement par webstart
      String applet = null; // Url du lancement par applet
      String javaParam = null;// ligne de commande pour une application java (sans l'appel à java)
      String dir=null;  // Répertoire d'installation dans le cas d'un application VO
      String aladinActivated=null;  // NO si l'application est désactivé des menus Aladin
      String system=null;  // Commande en ligne pour une applcatin plastic
      String aladinMenu = null; // Label du sous-menu Popup ou null sinon
      String aladinMenuNumber = null; // Ordre du menu ou sous menu sous la forme d'une suite de nombres séparés par des . ex: 1.20.300
      String aladinLabel = null; // Label du bouton ou null sinon
      String aladinLabelPlane = null; // Masque du Label du plan ou null sinon
      String docUser = null; // URL d'une doc associee ou null sinon
      String url = null; // Le masque de l'URL de l'enr courant
      String s = null; // Variable de travail
      String description = null; // La description courante
      String verboseDescr = null;// La description verbose courante
      String resultDataType = null; // Le type de donnees retournees
      String seeAction = null; // Dans le cas d'indirection
      String institute = null;// Mention de l'institut d'origine
      String aladinLogo = null;// Le nom du logo associé
      String aladinTree = null;// L'arborescence dans le cas d'un Tree Outreach (ex: WP5:/Nebulae/HII)
      String aladinUrlDemo = null;//Url renvoyant un exemple (catalog ou image)
      Vector recI = new Vector(); // Dans le cas d'indirections
      Hashtable paramDescription = null;// Les descriptions des parametres (cle=numero du
                          // param)
      Hashtable paramDataType = null;// Les types des parametres (cle=numero du param)
      Hashtable paramValue = null;// Les valeurs possibles des parametres (cle=numero du
                          // param)
      Vector aladinFilter = null; // Les filtres prédéfinis
      String aladinSurvey=null; // Le préfixe du survey dans le cas d'enregistrement de Ciel
      String aladinHpxParam=null;      // Paramètres particuliers dans le cas d'un ciel Healpix
      String aladinBookmarks=null;    // Liste des bookmarks
      StringBuffer record = null;  // Copie de l'enregistrement en cours.
      boolean ignore=false; // true si on doit ignorer l'enregistrement courant (cf overwrite)
      boolean distribAladin = !testDomain; // true si distribue dans le domaine ALADIN
      boolean flagLabel = false; // true si on a un champ %Aladin.Label
      String aladinProfile=null;  // indications d'usage (undergraduate, beta, 5.9+...)
      boolean flagPlastic = false; // true si on a un champ %Aladin.Plastic
      boolean flagGluSky = false;  // true si on a "hpx" dans le profile => GluSky background

      int maxIndir = Integer.MAX_VALUE; // Pour repérer la meilleure indirection

      paramDescription = new Hashtable();
      paramDataType = new Hashtable();
      paramValue = new Hashtable(10);
      aladinFilter = new Vector(10);

      try {

         // Test d'une profondeur excessive de la récursivité
         if( profondeur > MAXPROF ) throw new Exception(
               "Cyclic GLU definitions");
         
         while( (s = dis.readLine()) != null ) {
            if( s.equals("") || s.charAt(0) == '#' ) continue;
            if( (name = getName(s)) == null ) continue;
            if( (value = getValue(s, dis)) == null ) continue;
            
            // Dans le cas ou l'on ne doit pas écraser une éventuelle définition pré-existante
            if( !overwrite ) {
               if( isKey(name,"A") || isKey(name,"ActionName")) {
                  if( ignore ) ignore=false;
                  StringTokenizer st = new StringTokenizer(value);
                  while( st.hasMoreTokens() ) {
                     String id = st.nextToken();
                     if( aladinDic.get(id)!=null ) {
                        ignore=true;
                        aladin.trace(3,"GLU record overwrite ignored: "+value);
                        break;
                     }
                  }
               }
               if( ignore ) continue;
            }
            
            if( record!=null && !isKey(name,"A") && !isKey(name,"ActionName") ) {
               record.append("%"+Util.fillWithBlank(name, 4)+" "+value+"\n");
            }
                        
                 if( isKey(name,"Aladin.Menu",true) )  aladinMenu = subCR(value);
            else if( isKey(name,"Aladin.Tree",true) )  aladinTree=subCR(value);
            else if( isKey(name,"Aladin.UrlDemo") )    aladinUrlDemo=subCR(value);
            else if( isKey(name,"ReleaseNumber") )     releaseNumber=subCR(value);
            else if( isKey(name,"Download") )          download=subCR(value);
            else if( isKey(name,"Jar") )               jar=subCR(value);
            else if( isKey(name,"Webstart") )          webstart=subCR(value);
            else if( isKey(name,"Applet") )            applet=subCR(value);
            else if( isKey(name,"JavaParam") )         javaParam=subCR(value);
            else if( isKey(name,"Dir") )               dir=value;
            else if( isKey(name,"Aladin.Activated") )  aladinActivated=subCR(value);
            else if( isKey(name,"Aladin.Survey") )     aladinSurvey=subCR(value); 
            else if( isKey(name,"Aladin.HpxParam") )   { aladinHpxParam=subCR(value); flagGluSky=true; } 
            else if( isKey(name,"Aladin.Bookmarks") )  aladinBookmarks=subCR(value); 
            else if( isKey(name,"System") )            system=subCR(value);
            else if( isKey(name,"M.C",true) || isKey(name,"Copyright",true) )  copyright=subCR(value);
            else if( isKey(name,"Copyright.Url",false)) copyrightUrl=subCR(value);
            else if( isKey(name,"Aladin.MenuNumber") )  aladinMenuNumber = subCR(value);
            else if( isKey(name,"Aladin.LabelPlane",true) ) aladinLabelPlane = subCR(value);
            else if( isKey(name,"Aladin.Logo") )       aladinLogo = subCR(value);
            else if( isKey(name,"Aladin.Filter") )     aladinFilter.addElement(subCR(value));
            else if( isKey(name,"F.U",true) || isKey(name,"Doc.User",true) )   docUser = value;
            else if( isKey(name,"D",true) || isKey(name,"Description",true) )  description = value;
            else if( isKey(name,"M.D",true) || isKey(name,"M.V",true) || isKey(name,"VD",true) || isKey(name,"VerboseDescr",true) ) verboseDescr = value;
            else if( isKey(name,"R") || isKey(name,"ResultDataType")) resultDataType = value;
            else if( isKey(name,"A") || isKey(name,"ActionName") ) {
               // Dans le cas d'un enregistrement d'indirection on mémorise la meilleure
               // indirection directement dans aladinDic sous la syntaxe "%I gluID"
               // Et on garde de coté la meilleure indirection pour après
               if( seeAction != null ) {
                  recI.addElement(seeAction);
                  StringTokenizer aST = new StringTokenizer(actionName);
                  while( aST.hasMoreTokens() ) {
                     String key = aST.nextToken();
                     aladinDic.put(key, "%I " + seeAction);
                  }
               }
               
               if( hasValidProfile(aladinProfile,aladinTree,flagPlastic) && distribAladin ) {
                  if( aladinBookmarks!=null ) aladin.bookmarks.memoGluBookmarks(actionName,aladinBookmarks);
                  else if( flagGluSky ) memoGluSky(actionName,aladinLabel,aladinMenuNumber,url,description,verboseDescr,aladinProfile,copyright,copyrightUrl,aladinTree,
                        aladinSurvey,aladinHpxParam);
                  else if( aladinTree!=null ) memoTree(actionName,description,aladinTree,url,docUser,aladinUrlDemo);
                  else if( flagPlastic ) memoApplication(actionName,aladinLabel,aladinMenuNumber,description,verboseDescr,institute,releaseNumber,
                                                  copyright,docUser,jar,javaParam,download,webstart,applet,dir,aladinActivated,system);
                  else if( flagLabel ) memoServer(actionName,description,verboseDescr,aladinMenu,aladinMenuNumber,
                                                  aladinLabel,aladinLabelPlane,docUser,paramDescription,paramDataType,paramValue,
                                                  resultDataType,institute,aladinFilter,aladinLogo,dir,localFile?system:null,record);
               }
               distribAladin = !testDomain;
               flagGluSky=flagPlastic=flagLabel = false;
               aladinUrlDemo=aladinTree=aladinProfile=null;

               // On mémorise le filtre pour le serveurs non GLU
               if( !flagLabel ) putAladinFilter(actionName,aladinFilter);
               
               maxIndir = Integer.MAX_VALUE;
               copyright=copyrightUrl=releaseNumber=jar=javaParam=download=webstart=applet=dir=system=aladinActivated=actionName=description=verboseDescr=resultDataType=aladinMenu=aladinMenuNumber=aladinLabel=aladinLabelPlane=docUser=seeAction=url=institute=aladinLogo=aladinSurvey=aladinHpxParam=aladinBookmarks=null;
               paramDescription = new Hashtable();
               paramDataType = new Hashtable();
               paramValue = new Hashtable(10);
               aladinFilter = new Vector(10);
               actionName = subCR(getValue(s, dis));
               record=new StringBuffer(1000);
               record.append("%"+Util.fillWithBlank(name, 4)+" "+actionName+"\n");
            } else if( name.equals("U") || name.equals("Url") ) {
               url = value;
               StringTokenizer aST = new StringTokenizer(actionName);
               while( aST.hasMoreTokens() )
                  aladinDic.put(aST.nextToken(), url);
               // Le %L overide le %U (BEURK), EN FAIT IL FAUDRAIT ETRE PLUS
               // MALIN DANS
               // LE CAS OU %L CONTIENT $url MAIS CE SERA POUR UNE PROCHAINE
               // FOIS.
            } else if( name.equals("L") || name.equals("FullTextResult") ) {
               url = value;
               StringTokenizer aST = new StringTokenizer(actionName);
               while( aST.hasMoreTokens() )
                  aladinDic.put(aST.nextToken(), url);
               
            // On retient la meilleure indirection (voir après le while)
            } else if( name.equals("I") || name.equals("SeeAction") ) { 
               int m = 0;
               int i = 0;
               if( name.equals("I") ) {
                  i = value.indexOf(':'); // Syntaxe courte: %I id:nn
                  if( i > 0 ) try {
                     m = Integer.parseInt(value.substring(i + 1));
                  } catch( Exception e ) {
                     m = 10000;
                  }
                  else i = value.length();
               } else {
                  i = value.indexOf(' '); // Syntaxe longue: %SeeAction id
                                          // availability=nn
                  if( i > 0 ) try {
                     m = Integer.parseInt(value.substring(i + 14));
                  } catch( Exception e ) {
                     m = 10000;
                  }
                  else i = value.length();
               }
               if( m < maxIndir ) {
                  maxIndir = m;
                  seeAction = value.substring(0, i);
               }
               memoAlaSites(actionName, value.substring(0, i));
            // Le XLabel ne sert qu'à éviter aux versions Aladin <5 de charger des enregistrements GLU qui ne leur sont pas destinées
            } else if( name.equals("Aladin.Label") || name.equals("Aladin.XLabel")) {   
               aladinLabel = subCR(value);
               flagLabel = true;
            } else if( name.equals("Aladin.Name") ) {   // destiné à remplacer Aladin.Label à partir de la versino 5
               aladinLabel = subCR(value);
            } else if( name.equals("Aladin.Profile") ) {
               aladinProfile=value;
               flagLabel = true;
               flagGluSky |= value.indexOf("hpx")>=0;
            } else if( name.equals("Aladin.VOLabel") ) {
               aladinLabel = subCR(value);
               flagPlastic = true;
            } else if( name.equals("Z") || name.equals("DistribDomain") ) {
               if( getValue(s, dis).equals("ALADIN") ) distribAladin = true;
            } else if( name.equals("P.D") || name.equals("Param.Description") ) {
               paramDescription.put(getNumParam(value), getValParam(value));
            } else if( name.equals("P.K") || name.equals("Param.DataType") ) {
               paramDataType.put(getNumParam(value), getValParam(value));
            } else if( name.equals("M.I") || name.equals("Institute") ) {
               institute = value;
            } else if( name.equals("P.V") || name.equals("Param.Value") ) {
               String num = getNumParam(value);
               String v = getValParam(value);
               String v1 = (String) paramValue.get(num);
               if( v1 != null ) v1 = v1 + "\t" + v;
               else v1 = v;
               paramValue.put(num, v1);
            }
         }

         dis.close();
         
         // Le dernier si besoin est
         if( seeAction != null ) {
            recI.addElement(seeAction);
            StringTokenizer aST = new StringTokenizer(actionName);
            while( aST.hasMoreTokens() ) {
               aladinDic.put(aST.nextToken(), "%I " + seeAction);
            }
         }
         if( hasValidProfile(aladinProfile,aladinTree,flagPlastic) && distribAladin ) {
            if( aladinBookmarks!=null ) aladin.bookmarks.memoGluBookmarks(actionName,aladinBookmarks);
            else if( flagGluSky ) memoGluSky(actionName,aladinLabel,aladinMenuNumber,url,description,verboseDescr,aladinProfile,copyright,copyrightUrl,aladinTree,
                  aladinSurvey,aladinHpxParam);
            else if( aladinTree!=null ) memoTree(actionName,description,aladinTree,url,docUser,aladinUrlDemo);
            else if( flagPlastic ) memoApplication(actionName,aladinLabel,aladinMenuNumber,description,verboseDescr,institute,releaseNumber,
                                            copyright,docUser,jar,javaParam,download,webstart,applet,dir,aladinActivated,system);
            else if( flagLabel ) memoServer(actionName,description,verboseDescr,aladinMenu,aladinMenuNumber,
                                            aladinLabel,aladinLabelPlane,docUser,paramDescription,paramDataType,paramValue,
                                            resultDataType,institute,aladinFilter,aladinLogo,dir,localFile?system:null,record);
         }

         // On mémorise le filtre pour le serveurs non GLU
         if( !flagLabel ) putAladinFilter(actionName,aladinFilter);

         // On charge tous les enregistrements d'indirections qui nous manque si ils
         // ne sont pas encore dans le dictionnaire locale
         Enumeration eI = recI.elements();
         while( eI.hasMoreElements() ) {
            seeAction = (String) eI.nextElement();
            if( aladinDic.get(seeAction) == null ) {
               lastA = seeAction;
               loadRemoteGluRecord(seeAction, profondeur + 1);
            }
         }

      } catch( Exception e ) {
         System.err.println("loadGluDic error: " + e);
         e.printStackTrace();
         return false;
      }

      return true;
   }
   
   /** Méthode bas niveau pour modifier/ajouter une URL du dico GLU interne */
   public void put(String actionName,String url) {
      aladinDic.put(actionName,url);
   }
   
   /** Méthode bas niveau pour récupérer une URL du dico GLU interne */
   public String get(String actionName) {
      return (String) aladinDic.get(actionName);
   }
   
   private boolean isComparator(char ch) {
      return ch=='<' || ch=='>';
   }
   
   /** Retourne vrai si le test de version décrit par s correspond avec la version
    * d'Aladin. (ne prend en compte que la première décimale)
    * Ex: >5.0, 4.3, <=4.6
    */
   private boolean hasValidNumVersion(String s) {
      double num = aladin.realNumVersion(Aladin.VERSION);
      int test=0;  // -2:<, -1:<=, 0:=, 1:>=, 2:>
      int i=0;
      try {
         if( s.charAt(i)=='<' ) { test=-2; i++; }
         else if( s.charAt(i)=='>' ) { test=2; i++; }
         else if( s.charAt(i)=='=' ) { test=0; i++; }
         
         if( s.charAt(i)=='=' ) { test/=2; i++; }
         
         double n = Double.parseDouble( s.substring(i) );
         
         switch(test ) {
            case -2: return num<n;
            case -1: return num<=n;
            case  0: return num==n;
            case  1: return num>=n;
            case  2: return num>n;
         }
         
      } catch( Exception e ) {
         System.err.println("GLU Aladin.profile version number error ["+s+"]");
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
      return false;
   }
   
//   private boolean hasValidProfile(String profile) {
//      boolean rep = hasValidProfile1(profile);
//      if( profile!=null ) System.out.println("Aladin.profile "+profile+" "+(rep? "Ok":"Nok"));
//      return rep;
//   }
   
   /** Détermine si l'enregistrement GLU doit être pris en compte ou non.
    * 1) En mode OUTREACH, le mot "undergraduate" doit être obligatoirement présent
    * 2) D'autre part, il faut que toutes les contraintes mentionnées soit respectées 
    */
   private boolean hasValidProfile(String profile,String tree,boolean flagPlastic) {
      if( Aladin.OUTREACH && !flagPlastic &&
            (profile==null || profile.indexOf(Configuration.UNDERGRADUATE)<0) ) return false;
      if( profile==null ) return true;
      
      boolean rep=true;
      StringTokenizer st = new StringTokenizer(profile);
      while( rep && st.hasMoreTokens() ) {
         String s = st.nextToken();
         if( s.equals("undergraduate") ) { if( tree==null ) rep &= Aladin.OUTREACH; }
         else if( s.equals("beta") ) rep &= Aladin.BETA;
         else if( s.equals("proto") ) rep &= Aladin.PROTO;
         else if( s.equals("applet") ) rep &= aladin.isApplet();
         else if( s.equals("standalone") ) rep &= aladin.STANDALONE;
         else if( Character.isDigit(s.charAt(0)) || isComparator(s.charAt(0)) ) rep &= hasValidNumVersion(s);
         else if( s.equals("hpx") || s.equals("localdef") ) continue;   // simplement ignoré
         else rep=false;
      }
      return rep;
   }
   
   /** mémorise les filtres associés à la marque GLU A */
   private void putAladinFilter(String A,Vector AF) {
      int n;
      if( AF==null || (n=AF.size())==0 ) return;
      
      String filters[] = null;
      filters = new String[n];
      Enumeration e = AF.elements();
      for( int i=0; i<n; i++ ) filters[i] = (String) e.nextElement();

      if( aladinDicFilters==null ) aladinDicFilters = new Hashtable();
      StringTokenizer aST = new StringTokenizer(A);
      while( aST.hasMoreTokens() ) aladinDicFilters.put(aST.nextToken(), filters);

   }

   // Liste les noms des ID GLU des serveurs GLU connus, voir memoAlaSites()
   protected Vector AlaSites = new Vector();

   // Mémorise les sites GLU connus via les indirections de l'enregistrement
   // AlaU qui
   // se trouve dans AlaGlu.dic => voir testAlaSites()
   private void memoAlaSites(String A, String I) {
      if( !A.equals("AlaU") ) return;
      AlaSites.addElement(I);
   }

   /** Teste si le site GLU courant répond */
   private boolean testCurrentAlaSite() {
      try {
         URL testGlu = new URL(NPHGLUALADIN + "?J2000");
         DataInputStream dis = new DataInputStream(testGlu.openStream());

         if( !dis.readLine().startsWith("%DataTypeName") ) throw new Exception();
         return true;
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
      int i = NPHGLUALADIN.indexOf('/',8);
      if( i<0 ) i=NPHGLUALADIN.length();
      String s = NPHGLUALADIN.substring(7,i);
      Aladin.trace(2,"GLU registry site not responding [" + s + "] => Aladin will select another one automatically...");
      return false;
   }

   /**
    * Teste un à un les différents GLU sites répertoriés via l'enregistrement
    * GLU AlaU qui se trouve dans le fichier AlaGlu.dic REMARQUE : ne sert que
    * pour la version STANDALONE ou APPLET SIGNEE BUG: J'AI L'IMPRESSION QUE
    * L'INTERRUPTION FORCEE NE MARCHE PAS
    * @param firstOne true si on s'arrête au premier qui marche, sinon on garde
    *           le plus rapide
    * @param flagConfig permet de faire évoluer une liste de sites potentiels
    *           pour faire patienter, null sinon
    * @return true si on a au-moins un site qui répond
    */
   protected boolean testAlaSites(boolean firstOne, boolean flagConfig) {
      long time;
      long minTime = Long.MAX_VALUE;
      String bestI = null;
      boolean flagInterrupt = false;

      Enumeration e = AlaSites.elements();
      while( e.hasMoreElements() && !flagInterrupt ) {
         time = Long.MAX_VALUE;
         String ala = null;
         try {
            long d1 = System.currentTimeMillis();
            ala = (String) e.nextElement();
            //System.out.println("Testing Glu site "+aladinDic.get(ala)+
            // "/"+NPHGLU+"...");
            String s = aladinDic.get(ala) + "/" + NPHGLU;
            if( flagConfig ) aladin.configuration.setSelectGluChoice(s);
            URL testGlu = new URL(s + "?J2000");
            DataInputStream dis = new DataInputStream(testGlu.openStream());
            if( !dis.readLine().startsWith("%DataTypeName") ) throw new Exception();
            time = System.currentTimeMillis() - d1;
         } catch( Exception e1 ) {
            if( Thread.currentThread().isInterrupted() ) flagInterrupt = true;
            time = Long.MAX_VALUE;
         }

         if( time < minTime ) {
            minTime = time;
            bestI = ala;
         }

         // On retient le premier qui répond
         if( firstOne && bestI != null ) break;
      }

      if( bestI == null ) return false;

      // Je mets à jour le NPHGLU le plus proche
      NPHGLUALADIN = aladinDic.get(bestI) + "/" + NPHGLU;

      Aladin.trace(3, "New Glu site selected: [" + NPHGLUALADIN + "]");

      // Je mémorise le site qui a répondu le plus rapidement
      aladinDic.put("AlaU", "%I " + bestI);

      return true;
   }

   // Pour permettre de mémoriser le site GLU désigné lors d'une sélection
   // automatique suite à un défaut du site par défaut
   private boolean flagNoGlu;
   
   /**
    * Positionnement du nouveau site glu par défaut
    * @return null si ok, le site alternatif par défaut
    */
   protected String setDefaultGluSite(String s) {
      if( !s.endsWith("/" + NPHGLU) ) s += "/" + NPHGLU;
      if( !flagNoGlu && s.equals(NPHGLUALADIN) ) return null;
      NPHGLUALADIN = s;
      Aladin.trace(3, "New default Glu site: [" + NPHGLUALADIN + "]");

      // Mémorisation ds la config utilisateur
      try {
         aladin.configuration.set(Configuration.GLU, NPHGLUALADIN);
      } catch( Exception e ) {
      }

      // Utilisation pour la session courante
      if( !(flagNoGlu=testCurrentAlaSite()) ) {
         aladin.warning(aladin.chaine.getString("NOGLU1")+" \n" + NPHGLUALADIN
               + " "+aladin.chaine.getString("NOGLU2"));
         testAlaSites(true, false);
         return NPHGLUALADIN;
      }
      return null;
   }

   /**
    * Enleve les \ qui precede un caractere particulier
    * @param s La chaine a traiter
    * @param x Le caractere qui doit suivre \
    * @return La chaine traitee
    */
   static String removeBackslash(String s, char x) {
      if( s.length() == 0 ) return s;
      char[] a = s.toCharArray();
      StringBuffer res = new StringBuffer();
      int i;

      for( i = 0; i < a.length - 1; i++ ) {
         if( a[i] == '\\' && a[i + 1] == x ) continue;
         res.append(a[i]);
      }
      res.append(a[i]); // On ajoute le dernier caractere

      return res.toString();
   }

   /** Prefixage des " par \ */
   static String prefixQuote(String s) {
      char[] a = s.toCharArray();
      StringBuffer b = new StringBuffer();

      for( int i = 0; i < a.length; i++ ) {
         if( a[i] == '"' ) b.append('\\');
         b.append(a[i]);
      }

      return b.toString();
   }

   /**
    * Decoupage des parametres d'une marque GLU. Les parametres peuvent etre
    * ``enquotes''.
    * @param s La liste des parametres a traiter
    * @return Le tableau des parametres donnees individuellement sans les quotes
    */
   static String[] cutParam(String s) {
      char[] a = s.toCharArray();
      int i = 0;
      int deb; // indice de debut du parametre courant
      Vector p = new Vector(3); // Vecteur de travail
      String[] res = null; // Ce qui sera retourne
      char quote; // La quote courante (' ou ")

      while( true ) {
         // On passe les blancs eventuels
         if( (i = afterSpace(a, i)) == -1 ) break;

         // Le parametre est ``enquote''
         if( a[i] == '\'' || a[i] == '"' ) {
            quote = a[i];
            for( deb = ++i; i < a.length; i++ ) {
               if( a[i] == quote && a[i - 1] != '\\' ) break;
            }
            if( i == a.length ) break; // Fin impromptue
            p.addElement(removeBackslash(new String(a, deb, i - deb), quote));
            i++; // On passe la quote courante

            // Le parametre n'est pas enquote
         } else {
            deb = i;
            if( (i = afterWord(a, i)) == -1 ) {
               p.addElement(new String(a, deb, a.length - deb));
               break;
            } else p.addElement(new String(a, deb, i - deb));
         }
      }

      // Mappage du vecteur sous la forme d'un tableau de Strings
      res = new String[p.size()];
      for( i = 0; i < res.length; i++ )
         res[i] = new String((String) p.elementAt(i));
      return res;

   }

   static public final int URL    = 0;
   static public final int ENCODE = 1;
   static public final int NOURL  = 2;
   
//   public static void main(String argv[]) {
//      String url = "http://cdsarc.u-strasbg.fr/viz-bin/nph-Cat/$2?-plus=-+&$1&bidule=$3";
//      String [] param = { "/un/deux/trois" };
//      String s = dollarSet(url,param,URL);
//      System.out.println("url  =["+url+"]");
//      System.out.println("param=["+param[0]+"]");
//      System.out.println(  "==> ["+s+"]");
//   }
   
   /**
    * Substitution dans un String des $nn par des parametres.
    * Rq: par défaut (mode URL): HTTP encode là où il faut et supprime tous les &value=$nn non renseigné
    * @param s La ligne a traiter
    * @param param Les parametres a inserer
    * @param mode ULR=0:substitution pour une URL, ENCODE=1:les paramètres ont déjà été HTTPencodés, NOURL=2:substitution simple
    * @return La ligne traitee
    */
   static public String dollarSet(String s, String[] param,int mode) {
      char[] a; // Mappage de la chaine s
      int i = 0;
      int deb, fin; // pour memoriser les positions
      int offsetNum; // Offset du premier digit d'un $nnn
      int num; // nnn du $nnn courant
      boolean afterQuestion = false;   // true lorsqu'on a dépassé le ? en mode encodage
      StringBuffer res = new StringBuffer();
      boolean encode = (mode & ENCODE) == ENCODE;
      boolean isurl = (mode & NOURL) == 0;
      
      // Mappage de la chaine s dans un tableau manipulable
      if( s.length() == 0 ) return "";
      a = s.toCharArray();

      one: while( true ) {
         deb = i;

         // Recherche du prochain $nnn
         num = -1;
         do {
            while( i < a.length && a[i] != '$' ) {
               
               // Si nécessaire, on encode les paramètres lorsqu'on a dépassé le "?" de l'URL
               if( isurl && !encode && !afterQuestion && a[i]=='?' ) {
                  afterQuestion=true;
                  for( int j=0;j<param.length; j++) param[j] = URLEncoder.encode(param[j]);
               }
               i++;
            }
            if( i == a.length ) break one;

            // Determination du numero
            for( offsetNum = ++i; i < a.length && a[i] > '0' && a[i] <= '9'; i++ );
            try {
               num = Integer.parseInt(new String(a, offsetNum, i - offsetNum));
            } catch( NumberFormatException e ) {
               num = 0;
            }
            num--; // Les indices $nnn commence en 1
         } while( num < 0 );
         

         // Recherche de la fin du prefixe
         fin = offsetNum - 1; // Par defaut
         if( isurl && (num >= param.length || param[num].length() == 0) ) {
            while( fin > 0 && a[fin] != '&' && a[fin] != '?' ) fin--; // on supprime le "&name="
            if( fin==0 ) fin=offsetNum-1; // De fait avant le '?'
            else if( a[fin] == '?' ) fin++; // On laisse le '?'
         }

         // Memorisation du prefixe
         res.append(new String(a, deb, fin - deb));

         // Memorisation de la valeur si necessaire
         if( num < param.length ) {
            res.append(param[num]);
            
            // Encodage des paramètres si dans le paramètre que l'on vient de substituer
            // il y a un '?'
            if( isurl && !encode && !afterQuestion ) {
               for( int k=0; k<param[num].length(); k++ ) {
                  if( param[num].charAt(k)=='?' ) {
                     afterQuestion=true;
                     for( int j=0;j<param.length; j++) param[j] = URLEncoder.encode(param[j]);
                     break;
                  }
               }
            }
         }
      }

      // Ajout de la fin de la ligne
      res.append(new String(a, deb, a.length - deb));

      // Le resultat
      String url = res.toString();

      // Suppression d'un eventuel '?' tout seul en bout
      int n = url.length();
      if( isurl && n>0 && url.charAt(n - 1) == '?' ) url = url.substring(0, n- 1);

      return url;

   }

   /**
    * Glu resolver en JAVA. Dedie a la version Standalone. N'implante que le
    * strict minimum pour generer une URL a partir d'une marque GLU
    * @param id l'identificateur de la marque GLU
    * @param params les parametres de la marque
    * @param encode true si les parametres sont deja http-encodees sinon false,
    *           ou meme absent
    * @return l'URL calculee, ou null en cas de probleme
    */
   String gluResolver(String id, String params, boolean encode) {
      String[] param;
      int i;
      
      // J'enlève les éventuelles options GLU en suffixe de l'identificateur
      i = id.lastIndexOf(',');
      if( i>0 ) id = id.substring(0,i);

      Aladin.trace(3, "Querying the inside GLU <&" + id + (encode ? ",n" : "")
            + " " + params + ">...");

      // Decoupage des parametres
      param = cutParam(params);

      // Encodage des parametres si necessaire => C'EST DESORMAIS FAIT DANS dollarSet()
//      if( !encode ) {
//         for( i = 0; i < param.length; i++ )
//            param[i] = URLEncoder.encode(param[i]);
//      }

      // Recherche du masque de l'URL
      String url = (String) aladinDic.get(id);

      // Introuvable dans le dico local on va tenter de recherche
      // l'enregistrement
      // a distance
      if( url == null && aladin.NETWORK ) {
         //         Aladin.trace(3,"\"" + id + "\" not found in the inside GLU...");
         loadRemoteGluRecord(id);
         url = (String) aladinDic.get(id);
      }

      // Dans le cas d'une indirection dans le dico GLU inside
      // J'avais remplacé l'URL par "%I tagGLU", il faut donc que je
      // recherche récursivement l'URL réelle.
      while( url != null && url.startsWith("%I ") ) {
         url = (String) aladinDic.get(url.substring(3));
      }

      // Toujours introuvable
      if( url == null ) return null;

      // Substitution des $nn par les parametres adequats
      url = dollarSet(url, param,encode?ENCODE:0);

      // Résolution récursive s'il y a une marque GLU dans l'URL elle-même
      url = gluRecFilter(url);

      return url;
   }
   
   /** Insertion des paramètres dans une chaine système */
   String gluSystem(String system, String params) {
      String[] param = cutParam(params);
      system = dollarSet(system,param,NOURL);
      return system;
   }

   /**
    * GluFilter récursif dans le cas d'UN UNIQUE TAG GLU PRESENT dans l'URL
    * retourné (dans le cas d'une résolution GLU inside Java)
    * @param u l'url pouvant contenir des tags GLUs
    * @return l'URL résolu complètement
    */
   private String gluRecFilter(String u) {
      int i, j, k;

      // Détermination de l'emplacement du premier TAG GLU
      if( (i = u.indexOf("<&")) < 0 || (j = u.indexOf('>', i)) < 0 ) return u;

      // Peut être y a-t-il des paramètres à la marque GLU => (on les ignore)
      k = u.indexOf(",", i);
      if( k > 0 && k < j ) j = k;
      else {
         k = u.indexOf(" ", i);
         if( k > 0 && k < j ) j = k;
      }

      return u.substring(0, i) + getURL(u.substring(i + 2, j))
            + u.substring(j + 1);

   }

   /**
    * Chargement a distance d'un additif au dictionnaire GLU local via un
    * nph-glu. Ceci n'a d'interet que pour le Standalone
    * @param id l'enregistrement a charger
    * @param profondeur niveau de la récursivité pour éviter les cycles
    */
   private void loadRemoteGluRecord(String id) {
      loadRemoteGluRecord(id, 0);
   }

   private void loadRemoteGluRecord(String id, int profondeur) {
      try {
         URL url = new URL(NPHGLUALADIN + "?" + URLEncoder.encode(id)
               + "&param=-p");
         Aladin.trace(3, "Loading GLU record for \"" + id + "\" from " + url
               + "...");
         loadGluDic(new DataInputStream(url.openStream()), profondeur,true,true,false);
      } catch( Exception e ) {
         System.out.println(e);
      }
   }
   
   /** Retourne un MyInputStream en fonction d'une URI */
   public MyInputStream getMyInputStream(String uri,boolean withLog) throws Exception {
      MyInputStream is;
      
      if( !uri.startsWith("http:") && !uri.startsWith("ftp:") ) {
         is = new MyInputStream(new FileInputStream( aladin.getFullFileName(uri)));
         is=is.startRead();
      }
      else {
         URL u;
         if( Aladin.STANDALONE ) u = new URL(uri);
         else u = getURL("Http",uri,true,withLog);
         
//         is = new MyInputStream( u.openStream());
         is = Util.openStream(u);
      }
      return is;
   }
   
   /**
    * Procedure d'obtention de l'URL en fonction d'un tag GLU. Cette fonction
    * resoud un tag GLU quelque soit le mode de fonctionnement en applet ou en
    * standalone. Elle se charge de generer un log de la demande
    * @param id l'identificateur de la marque GLU
    * @param params les parametres de la marque
    * @param encode true si les parametres sont deja http-encodees sinon false,
    *           ou meme absent
    * @param withLog true si il faut generer un log
    * @return l'URL calculee, ou null en cas de probleme
    */
   public URL getURL(String id) {
      return getURL(id, "", false);
   }

   public URL getURL(String id, String params) {
      return getURL(id, params, false);
   }

   public URL getURL(String id, String params, boolean encode) {
      return getURL(id, params, encode, true);
   }

   public URL getURL(String id, String params, boolean encode, boolean withLog) {
      URL url = null; // L'URL resultante
      String tag; // Le tag GLU a utilise
      String option; // Les options de ce tag GLU
      String u; // L'URL a construire sous forme de chaine
      
      // Reseau ?
      if( !Aladin.NETWORK ) {
//         try {
//            if( id.equalsIgnoreCase("http") ) return new URL(params);
//         } catch( MalformedURLException e ) {}
//         System.err.println("getURL failed (no network)");
         return null;
      }

      Aladin.trace(4, "Glu.getURL(" + id + "," + params + ", encode=" + encode
            + ", withLog=" + withLog + ")");
      // log
      if( withLog ) log(id, params);

      // Si les parametres sont deja httpencodes, j'ajoute l'option
      // n au tag GLU histoire de ne pas le faire deux fois
      if( encode ) option = ",n";
      else option = ",";

      // Resolution, soit inside, soit locale, soit distante
      try {

         // Standalone => Appel inside
         if( Aladin.STANDALONE ) {

            // Appel au GLU inside
            u = gluResolver(id, params, encode);

            if( u == null ) {
               Aladin.trace(3, "getURL error: glu record \"" + id
                     + "\" not found !\n");
               return null;
            }
            url = new URL(u);

            // Applet non signée => Appel au GLU distant, on va retourner l'URL
            // d'appel
            // a ce GLU distant avec l'option R dans le tag GLU
         } else {
            option = option + "R";
            tag = "<&" + id + option + " " + params + ">";
            url = new URL(aladin.CGIPATH + "/" + NPHGLU + "?"
                  + URLEncoder.encode(tag));
            //            url = new URL(aladin.getCodeBase()+"/"+NPHGLU+"?" +
            // URLEncoder.encode(tag));
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) {
            System.err.println("getURL error: " + e);
            e.printStackTrace();
         }
         return null;
      }

      Aladin.trace(3, "Get: " + ((url == null) ? "null" : url.toString()));
      return url;
   }

   private boolean flagVers; // Vrai s'il faut lire le resultat du log

   // pour connaitre le numero de la dern version

   private boolean lock = false;
   
   /** Attend le lock */
   private void waitLock() { 
      while( !getLock() ) {
         Util.pause(100);
         aladin.trace(4,"Glu.waitLock...");
      }
   }

   /** Demande le lock */
   synchronized boolean getLock() {
      if( lock ) return false;
      lock=true;
      return true;
   }

   /** Libere le lock */
   synchronized void unlock() { lock=false; }

   /**
    * Envoi d'un log. Generation d'un log sur le serveur de l'applet ou sur
    * aladin.u-strasbg.fr par defaut. Utilise un simple appel a l'URL alalog.pl
    * avec les parametres de la derniere marque GLU. Rq: Utilise un verrou afin
    * de pouvoir tranquillement passer les variables param et flagVers au thread
    * qui va etre charge du log sans se les faire ecraser par le log suivant
    * @param id l'identificateur GLU <BR>
    * @param params les parametres de la marque GLU
    * @return null ou eventuellement le retour du log si Id=="Start"
    */
   protected void log(String id, String params) {

      if( !Aladin.NETWORK || !Aladin.LOG || !aladin.configuration.isLog() ) return;
      
      // les trucs inutiles ou un peu trop indiscrets
      if( id.equals("VizX") ) return;
      if( id.equals("Load") || id.equals("Http") ) params="";
      
      try {
         waitLock(); // verrouillage
         param = ALADINLOG + "?id=" + (id==null ? "":URLEncoder.encode(id)) + "&params="
               + (params==null ? "" : URLEncoder.encode(params));
         flagVers = (Aladin.STANDALONE && id.equals("Start"));

         thread = new Thread(this,"AladinLog");
         Util.decreasePriority(Thread.currentThread(), thread);
         thread.start();
      } catch( Exception e ) {
         e.printStackTrace();
         unlock();
      }
   }

   /** Envoi d'un log. */
   public void run() {

      URL url;
      String tmp = param; // Copie des variables critiques
      boolean flagTmp = flagVers; // Copie des variables critiques
      unlock(); // liberation du verrou
      
      try {

         // Construction de l'URL par defaut
         if( Aladin.APPLETSERVER == null && Aladin.RHOST == null ) {
            url = new URL(tmp);

            // Construction de l'URL par rebond + ajout du param &host= pour
            // que le nph-glu.pl ajoute la provenance
         } else {
            tmp += "&host=";
            if( Aladin.RHOST != null ) tmp += Aladin.RHOST;
            url = getURL("Http", tmp, true, false);
         }

         InputStream is = url.openStream();
         
         // Lecture du numero de la derniere version disponible
         if( flagTmp ) {
            DataInputStream dis = new DataInputStream(is);
            aladin.setCurrentVersion(dis.readLine());
            dis.close();
         }

         is.close();
      } catch( Exception elog ) {
      }
   }
}
