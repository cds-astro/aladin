// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
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

package cds.tools;

import static cds.aladin.Constants.DATE_FORMATS;
import static cds.aladin.Constants.LISTE_CARACTERE_STRING;
import static cds.aladin.Constants.RESULTS_RESOURCE_NAME;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import cds.aladin.Aladin;
import cds.aladin.Coord;
import cds.aladin.Forme;
import cds.aladin.MyInputStream;
import cds.aladin.Plan;
import cds.aladin.Tok;
import cds.image.EPSGraphics;
import cds.savot.model.ResourceSet;
import cds.savot.model.SavotResource;
import cds.savot.pull.SavotPullParser;
import cds.xml.TapQueryResponseStatusReader;
import healpix.essentials.FastMath;

/**
 * Diverses méthodes utilitaires
 */
public final class Util {


   public static String CR;
   public static String FS;

   static {
      CR = System.getProperty("line.separator");
      FS = System.getProperty("file.separator");
   }

   /** Ouverture d'un MyInputStream que ce soit un fichier ou une url */
   static public MyInputStream openAnyStream(String urlOrFile) throws Exception {
      if( urlOrFile.startsWith("http:") || urlOrFile.startsWith("https:")
            || urlOrFile.startsWith("ftp:") ) return openStream(urlOrFile);
      FileInputStream f = new FileInputStream(urlOrFile);
      MyInputStream is = new MyInputStream(f);
      return is.startRead();
   }

   /** Ouverture d'un MyInputStream avec le User-Agent correspondant à Aladin */
   static public MyInputStream openStream(String u) throws Exception { return openStream(new URL(u),true,10000); }
   static public MyInputStream openStream(String u,boolean useCache, int timeOut) throws Exception {
      return openStream(new URL(u),useCache,timeOut);
   }
//   static public MyInputStream openStream(URL u) throws Exception { return openStream(u,true,10000); }
   static public MyInputStream openStream(URL u) throws Exception { return openStream(u,true,-1); }
   static public MyInputStream openStream(URL u, boolean useCache,int timeOut) throws Exception {
      URLConnection conn = u.openConnection();
      if( !useCache ) conn.setUseCaches(false);
      if( timeOut>0 ) conn.setConnectTimeout(timeOut);
      // DEJA FAIT DANS Aladin.myInit() => mais sinon ne marche pas en applet
      if( conn instanceof HttpURLConnection ) {
         HttpURLConnection http = (HttpURLConnection)conn;
         http.setRequestProperty("http.agent", "Aladin/"+Aladin.VERSION);
         http.setRequestProperty("Accept-Encoding", "gzip");
      }

      MyInputStream mis = new MyInputStream(openConnectionCheckRedirects(conn,timeOut));
      //       MyInputStream mis = new MyInputStream(conn.getInputStream());
      return mis.startRead();
   }
   
   static public MyInputStream openStreamForTap(URL u, boolean useCache,int timeOut) throws Exception {
	      URLConnection conn = u.openConnection();
	      if( !useCache ) conn.setUseCaches(false);
	      if( timeOut>0 ) conn.setConnectTimeout(timeOut);
	      // DEJA FAIT DANS Aladin.myInit() => mais sinon ne marche pas en applet
	      if( conn instanceof HttpURLConnection ) {
	         HttpURLConnection http = (HttpURLConnection)conn;
	         http.setRequestProperty("http.agent", "Aladin/"+Aladin.VERSION);
	         http.setRequestProperty("Accept-Encoding", "gzip");
	         InputStream is = null;
	         if (http.getResponseCode() >= 400) {
	        	is = http.getErrorStream();
				TapQueryResponseStatusReader queryStatusReader = new TapQueryResponseStatusReader();
				queryStatusReader.load(is);
				is.close();
				String errorMessage = queryStatusReader.getQuery_status_message();
				if (errorMessage == null || errorMessage.isEmpty()) {
					errorMessage = "Error: "+http.getResponseCode()+" from server. Unable to get response! Server : "+u;
				} else {
					errorMessage = queryStatusReader.getQuery_status_value() + " " + errorMessage;
				}
				http.disconnect();
				throw new IOException(errorMessage);
			}
	      }

	      MyInputStream mis = new MyInputStream(openConnectionCheckRedirects(conn,timeOut));
	      //       MyInputStream mis = new MyInputStream(conn.getInputStream());
	      return mis.startRead();
	   }
   
   /** Je suis obligé de passer par un Thread indépendant pour qu'un timeout soit effectivement pris en compte
    * -1 si timeout indéfini
    */
   static public InputStream openConnectionCheckRedirects(URLConnection conn, long timeOut) throws Exception {
      
      // Pas de timeout => le thread courant fera l'affaire
      // POUR LE MOMENT ON COURT-CIRCUITE CELA CAR POUR LES IMAGES 2MASS DE FRANCOIS B.
      // ON DEPASSE LARGEMENT LE CHIEN DE GARDE DE 10s JUSTE POUR CREER LE SOCKET
      if( timeOut==-1 ) return openConnectionCheckRedirects1(conn);
      
      // C'est parti...
      OpenConnection c = (new Util()).new OpenConnection(conn,timeOut);
      if( c.error!=null ) throw c.error;
      return c.in;
   }
   
   class OpenConnection extends Thread {
      URLConnection conn;
      InputStream in=null;
      boolean ok=false;
      Exception error=null;
      long t0;
      
      public OpenConnection(URLConnection conn, long timeout) {
         this.conn=conn;
         t0 = System.currentTimeMillis();
         start();
         
         // Tant  qu'il n'y a ni réponse, ni erreur, ni timeout, on attend
         while( in==null && error==null && (timeout==-1 || (System.currentTimeMillis()-t0)<timeout) ) {
            try { Util.pause(10); }
            catch( Exception e ) { }
         }
      }
      
      public void run() {
         try {
            in = openConnectionCheckRedirects1(conn);
            ok = true;
         } catch( Exception e ) { error=e; }
      }
   }


   /**
    * Java does not follow HTTP --> HTTPS redirections by default
    * This code allows to retrieve the "final" stream from a URLConnection, after following the redirections
    *
    * Code copied from http://download.oracle.com/javase/1.4.2/docs/guide/deployment/deployment-guide/upgrade-guide/article-17.html
    */
   static private InputStream openConnectionCheckRedirects1(URLConnection conn) throws IOException {
      boolean redir;
      int redirects = 0;
      InputStream in = null;
      do {
         if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).setInstanceFollowRedirects(false);
         }
         // We want to open the input stream before getting headers
         // because getHeaderField() et al swallow IOExceptions.
         in = conn.getInputStream();
         redir = false;
         if (conn instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) conn;
            int stat = http.getResponseCode();
            if (stat >= 300 && stat <= 307 && stat != 306 && stat != HttpURLConnection.HTTP_NOT_MODIFIED) {
               URL base = http.getURL();
               String loc = http.getHeaderField("Location");
               URL target = null;
               if (loc != null) {
                  target = new URL(base, loc);
               }
               http.disconnect();
               // Redirection should be allowed only for HTTP and HTTPS
               // and should be limited to 5 redirections at most.
               if (target == null || !(target.getProtocol().equals("http") ||
                     target.getProtocol().equals("https")) || redirects >= 5) {
                  throw new SecurityException("illegal URL redirect");
               }
               redir = true;
               conn = target.openConnection();
               try { conn.setUseCaches(http.getUseCaches()); } catch( Exception e ) { }
               redirects++;
            }
         }
      } while (redir);
      return in;
   }

   /** Voir matchMask(). */
   static public boolean matchMaskIgnoreCase(String mask, String word) {
      if( word==null || mask==null ) return false;
      return matchMask(toUpper(mask),toUpper(word));
   }

   /** Adapted from a C-algorithm from P. Fernique
    * checks whether word matches mask
    * @param mask a string which may contain '?' and '*' wildcards
    * @param word the string to check
    * @return boolean true if word matches mask, false otherwise
    */
   static public boolean matchMask(String mask, String word) {
      if( word==null || mask==null ) return false;
      //       if( mask.indexOf('*')<0 && mask.indexOf('?')<0 ) return word.indexOf(mask)>=0;

      mask = mask+'\0';
      word = word+'\0';
      int indiceM,indiceA;
      indiceM=indiceA=0;
      String stringB=null;
      String stringC=null;

      while( mask.charAt(indiceM)!='\0' || word.charAt(indiceA)!='\0' ) {
         if( mask.charAt(indiceM)=='\\' ) {
            indiceM++;
            continue;
         }

         if( mask.charAt(indiceM)=='*' && (indiceM==0 || mask.charAt(indiceM-1)!='\\') ) {
            indiceM++;
            stringB = mask.substring(indiceM);
            continue;
         }
         if( stringB!=null && !stringB.equals(mask) && word.charAt(indiceA)==word.charAt(0) ) stringC = word.substring(indiceA);

         if( mask.charAt(indiceM)==word.charAt(indiceA) || mask.charAt(indiceM)=='?' ) {
            if( mask.charAt(indiceM)=='\0' ) {
               if( stringB==null ) return false;
            }
            else indiceM++;
            if( word.charAt(indiceA)=='\0' ) return false;
            else indiceA++;
         }
         else {
            if( stringB!=null ) {
               mask = stringB;
               indiceM = 0;

               if( stringC!=null ) {
                  word = stringC;
                  indiceA = 0;
                  stringC = null;
               }
               else {
                  if( stringB.charAt(0)!=word.charAt(indiceA) || word.charAt(indiceA)=='\\' ) {
                     if( word.charAt(indiceA)=='\0' ) return false;
                     else indiceA++;
                  }
               }
            } else return false;
         }
      }
      return true;
   }

   static DecimalFormat DF;
   static {
      DF = new DecimalFormat();
      DF.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.US));
      DF.setGroupingSize(0);
   }

   static public String myRound(double x) {

      // cas particulier de la notation scientifique
      String s = x+"";
      int posV; // position de la virgule
      int posE; // position de l'exposant
      if( (posE=s.indexOf('E'))>0 ) {
         if( (posV=s.indexOf('.'))>0) {
            if( posV+4>posE ) return s;   // déjà pas bcp de décimales
            return s.substring(0,posV+4)+s.substring(posE);
         }
      }

      // cas général
      double y = Math.abs(x);
      if( y>1000 ) DF.setMaximumFractionDigits(0);
      else if( y>100 ) DF.setMaximumFractionDigits(1);
      else if( y>10 ) DF.setMaximumFractionDigits(2);
      else if( y>1 ) DF.setMaximumFractionDigits(3);
      else if( y>0.1 ) DF.setMaximumFractionDigits(4);
      else if( y>0.01 ) DF.setMaximumFractionDigits(5);
      else DF.setMaximumFractionDigits(6);

      return DF.format(x);
   }

   /**
    * Arrondit en travaillant sur la representation String
    * @param x Le nombre a arrondir
    * @param p Le nombre de decimales souhaitees
    * @return
    */
   static public String myRound(String x) { return myRound(x,0); }
   static public String myRound(String x,int p) {

      // Problème en cas de notation scientifique

      char a[] = x.toCharArray();
      char b[] = new char[a.length];
      int j=0;
      int mode=0;

      int len=x.indexOf('E');
      if( len<0 ) len=x.indexOf('e');

      int n = len<0 ? a.length : len;

      for( int i=0; i<n; i++ ) {
         switch(mode) {
            case 0: if( a[i]=='.' ) {
               if (p == 0)  return new String(b,0,j);
               mode = 1;
            }
            b[j++]=a[i];
            break;
            case 1: p--;
            if( p==0 ) mode=2;
            if( i+1<a.length && Character.isDigit(a[i+1]) && a[i+1]>='5' ) {
               b[j++]=a[i]++;
            } else b[j++]=a[i];
            break;
            case 2:
               if( Character.isDigit(a[i])) break;
               mode=3;
            case 3:
               b[j++]=a[i];
               break;
         }
      }

      String s = new String(b,0,j);
      if( len>=0 ) return s+x.substring(len);
      return s;

   }
   
   /** Retourne la sous-chaine d'un path délimité par des /
    * Rq: le / initial présent ou absent n'a pas d'incidence
    * @param path le path
    * @param deb l'indice de l'élément (commence à 0)
    * @param num le nombre d'éléments (par défaut 1)
    * @return ex: CDS/P/DSS2/color,2,2  => DSS2/color
    */
   static public String getSubpath(String path,int deb ) { return getSubpath(path,deb,1); }
   static public String getSubpath(String path,int deb, int num ) {
      if( path==null ) return null;
      int j=-1;
      int posDeb=-1;
      for( int i=0, pos=0; pos!=-1; pos=path.indexOf('/',pos+1), i++ ) {
         if( i==deb ) { posDeb=pos+ (path.charAt(pos)=='/'? 1:0); j=i; }
         if( j!=-1 && i-j==num ) return path.substring(posDeb,pos);
      }
      return (j>=0 || num==-1 ) && posDeb>=0 ? path.substring(posDeb) : null;
   }

   /**
    * Tokenizer spécialisé : renvoie le tableau des chaines séparés par sep ssi freq(c1) dans s == freq(c2) dans s
    * exemple : tokenize...("xmatch 2MASS( RA , DE ) GSC( RA2000 , DE2000 )", ' ', '(', ')' ) renvoie :
    * {"xmatch" , "2MASS( RA , DE )", "GSC( RA2000 , DE2000 )"}
    * Le délimiteur n'est pas considéré comme un token
    * @param s
    * @param sep ensemble des délimiteurs
    * @param c1
    * @param c2
    * @return
    */
   static public String[] split(String s, String sep, char c1, char c2, boolean trim) {
      if( s==null ) return null;
      char[] c = s.toCharArray();

      Vector v = new Vector();
      StringBuffer sb = new StringBuffer();
      int nbC1 = 0;
      int nbC2 = 0;

      for( int i=0; i<c.length; i++ ) {
         if( c[i]==c1 ) nbC1++;
         if( c[i]==c2 ) nbC2++;

         if( sep.indexOf(c[i])>=0 && nbC1==nbC2 ) {
            if( sb.length()>0 ) v.addElement(trim?sb.toString().trim():sb.toString());
            sb = new StringBuffer();
            continue;
         }

         sb.append(c[i]);

      }

      // ajout du dernier élément
      if( sb.length()>0 ) v.addElement(trim?sb.toString().trim():sb.toString());

      String[] tokens = new String[v.size()];
      v.copyInto(tokens);
      v = null;
      return tokens;
   }

   static public int[] splitAsInt(String s, String sep) {
      String[] items =  split(s, sep, '@', '@');
      int[] ret = new int[items.length];
      for (int i=0; i<items.length; i++) {
         ret[i] = Integer.parseInt(items[i]);
      }

      return ret;
   }

   static public char[] splitAschar(String s, String sep) {
      String[] items = split(s, sep, '@', '@');
      char[] ret = new char[items.length];
      for (int i = 0; i < items.length; i++) {
         ret[i] = items[i].charAt(0);
      }

      return ret;
   }

   static public String[] split(String s, String sep) {
      return split(s,sep,'@','@');
   }

   static public String[] split(String s, String sep, char c1, char c2) {
      return split(s,sep,c1,c2,false);
   }

   static public String join(String[] items, char c) {
      StringBuffer sb = new StringBuffer();
      for (int i=0; i<items.length; i++) {
         if (i!=0) {
            sb.append(",");
         }
         sb.append(items[i]);
      }
      return sb.toString();
   }

   static public String join(int[] items, char c) {
      StringBuffer sb = new StringBuffer();
      for (int i=0; i<items.length; i++) {
         if (i!=0) {
            sb.append(c);
         }
         sb.append(items[i]);
      }
      return sb.toString();
   }

   static public String join(char[] items, char c) {
      StringBuffer sb = new StringBuffer();
      for (int i=0; i<items.length; i++) {
         if (i!=0) {
            sb.append(c);
         }
         sb.append(items[i]);
      }
      return sb.toString();
   }

   /** Utilitaire pour ajouter des blancs après un mot afin de lui donner une taille particulière
    * @param key le mot à aligner
    * @param n le nombre de caractères souhaités
    * @return le mot aligné, ou si trop grand, avec juste un espace derrière
    */
   static public String align(String key,int n) { return align(key,n,""); }
   static public String align(String key,int n,String suffixe) {
      int i=key.length();
      if( i>=n ) return key+ suffixe +" ";
      StringBuffer s = new StringBuffer();
      for( int j=0; j<n-i; j++ ) s.append(' ');
      return key+suffixe+s;
   }

   /** Utilitaire pour ajouter des zéros avant un nombre pour l'aligner sur 3 digits
    * @param x la valeur à aligner
    * @return le nombre aligné
    */
   static public String align3(int x) {
      if( x<10 ) return "00"+x;
      else if( x<100 ) return "0"+x;
      else return ""+x;
   }

   /** Utilitaire pour ajouter des zéros avant un nombre pour l'aligner sur 2 digits
    * @param x la valeur à aligner
    * @return le nombre aligné
    */
   static public String align2(int x) {
      if( x<10 ) return "0"+x;
      else return ""+x;
   }

   /** Arrondit et limite le nombre de décimales
    * @param d nombre à arrondir
    * @param nbDec nb de décimales à conserver
    * @return le nombre arrondi en conservant nbDec décimales
    */
   public static double round(double d, int nbDec) {
      double fact = Math.pow(10,nbDec);
      return Math.round(d*fact)/fact;
   }

   /**
    * Utilitaire pour insérer des \n dans un texte afin de replier les lignes
    * @param s Le texte à "folder"
    * @param taille le nombre maximum de caractères par ligne (80 par défaut)
    * @param html true si on met en forme en HTML en vu de l'usage dans
    *        un widget SWING (<html>... <br>... </html>)
    * @return le texte avec les retours à la ligne
    */
   static public String fold(String s) { return fold(s,80,false); }
   static public String fold(String s,int limit) { return fold(s,limit,false); }
   static public String fold(String s,int limit,boolean html) {
      if( s==null ||s.trim().length()==0 ) return s;
      char a[] = s.toCharArray();
      String NL = !html ? "\n" : "<BR>";
      StringBuffer res = new StringBuffer(a.length+30);
      if( html ) res.append("<html>");
      boolean debut=true;
      for( int i=0,k=0,marge=0; i<a.length; i++,k++ ) {
         boolean space = Character.isSpaceChar(a[i]);
         if( debut ) {
            if( space ) marge++;
            else debut=false;
         }
         if( a[i]=='\n' ) {
            k=0;
            res.append(NL);
            continue;
         }
         if( k>limit+10
               || k>limit && space ) {
            res.append(NL);
            for( int j=0; j<marge; j++ ) res.append(' ');
            k=0;
         }
         if( !(k==0 && space) ) res.append(a[i]);
      }
      if( html ) res.append("</html>");
      return res.toString();
   }

   /** Extrait la table des couleurs pour une composante sous la forme d'un tableau de 256 bytes
    * @param cm Le modèle de couleur
    * @param component 0-Rouge, 1-Vert, 2-Bleu
    * @return les 256 valeurs de la table pour la composante indiquée
    */
   static public byte [] getTableCM(ColorModel cm,int component) {
      byte [] tcm = new byte[256];
      for( int i=0; i<tcm.length; i++ ) {
         tcm[i] = (byte) (0xFF & ( component==0 ? cm.getRed(i) : component==1 ? cm.getGreen(i) : cm.getBlue(i) ));
      }
      return tcm;
   }

   /**
    *
    * @param c couleur dont on veut la couleur inverse
    * @return
    */
   static public Color getReverseColor(Color c) {
      if( c==null ) return null;
      return new Color(255-c.getRed(), 255-c.getGreen(), 255-c.getBlue());
   }

   static final Color CEBOX = new Color(172,168,153);
   static final Color CIBOX = new Color(113,111,100);

   /**
    * Dessine les bords d'un rectangle avec un effet de volume
    * @param g Le contexte graphique concerné
    * @param w la largeur
    * @param h la hauteur
    */
   static public void drawEdge(Graphics g,int w,int h) { drawEdge(g,0,0,w,h); }
   static public void drawEdge(Graphics g,int x,int y,int w,int h) {
      g.setColor(CIBOX);
      g.drawLine(x,y,x+w-1,y); g.drawLine(x,y,x,y+h-1);
      g.setColor(Color.lightGray);
      g.drawLine(x+w-1,y+h-1,x,y+h-1); g.drawLine(x+w-1,y+h-1,x+w-1,y);
   }
   
   /** Tracage d'un logo cercle coupé en 2 verticalement de 12 pixels de diamètre */
   static public void drawCirclePix(Graphics g,int x,int y) {
      
      // Demi cercle supérieur
      g.drawLine(x-1,y-5,x+2,y-5);
      g.drawLine(x-3,y-4,x-2,y-4);  g.drawLine(x+1,y-4,x+4,y-4);
      g.drawLine(x-4,y-3,x-4,y-3);  g.drawLine(x+1,y-3,x+5,y-3);
      g.drawLine(x-4,y-2,x-4,y-2);  g.drawLine(x+1,y-2,x+5,y-2);
      g.drawLine(x-5,y-1,x-5,y-1);  g.drawLine(x+1,y-1,x+6,y-1);
      g.drawLine(x-5,y,x-5,y);      g.drawLine(x+1,y,x+6,y);
      
      // Demi cercle inférieur
      g.drawLine(x-5,y+1,x-5,y+1);  g.drawLine(x+1,y+1,x+6,y+1);
      g.drawLine(x-5,y+2,x-5,y+2);  g.drawLine(x+1,y+2,x+6,y+2);
      g.drawLine(x-4,y+3,x-4,y+3);  g.drawLine(x+1,y+3,x+5,y+3);
      g.drawLine(x-4,y+4,x-4,y+4);  g.drawLine(x+1,y+4,x+5,y+4);
      g.drawLine(x-3,y+5,x-2,y+5);  g.drawLine(x+1,y+5,x+4,y+5);
      g.drawLine(x-1,y+6,x+2,y+6);
   }

   /** Tracage d'un joli petit cercle de 7 pixels de diamètre */
   static public void drawCircle8(Graphics g,int x,int y) {
      if( !(g instanceof Graphics2D) ) {
         g.drawOval(x-4, y-4, 8, 8);
         return;
      }
      g.drawLine(x-3,y-1,x-3,y+2);
      g.drawLine(x+4,y-1,x+4,y+2);
      g.drawLine(x-1,y-3,x+2,y-3);
      g.drawLine(x-1,y+4,x+2,y+4);
      g.drawLine(x-2,y-2,x-2,y-2);
      g.drawLine(x-2,y+3,x-2,y+3);
      g.drawLine(x+3,y+3,x+3,y+3);
      g.drawLine(x+3,y-2,x+3,y-2);
   }

   /** Remplissage d'un joli cercle de 7 pixels de diamètre */
   static public void fillCircle8(Graphics g,int x, int y) {
      if( !(g instanceof Graphics2D) ) {
         g.drawOval(x-4, y-4, 8, 8);
         g.fillOval(x-4, y-4, 8, 8);
         return;
      }
      g.fillRect(x-2,y-2,6,6);
      drawCircle8(g,x,y);
   }

   /** Tracade d'un joli petit cercle de 7 pixels de diamètre */
   static public void drawCircle7(Graphics g,int x,int y) {
      if( !(g instanceof Graphics2D) ) {
         g.drawOval(x-3, y-3, 6, 6);
         return;
      }
      g.drawLine(x-3,y-1,x-3,y+1);
      g.drawLine(x+3,y-1,x+3,y+1);
      g.drawLine(x-1,y-3,x+1,y-3);
      g.drawLine(x-1,y+3,x+1,y+3);
      g.drawLine(x-2,y-2,x-2,y-2);
      g.drawLine(x-2,y+2,x-2,y+2);
      g.drawLine(x+2,y+2,x+2,y+2);
      g.drawLine(x+2,y-2,x+2,y-2);
   }

   /** Remplissage d'un joli cercle de 7 pixels de diamètre */
   static public void fillCircle7(Graphics g,int x, int y) {
      if( !(g instanceof Graphics2D) ) {
         g.fillOval(x-3, y-3, 6, 6);
         g.drawOval(x-3, y-3, 6, 6);
         return;
      }
      g.fillRect(x-2,y-2,5,5);
      drawCircle7(g,x,y);
   }

   /** Tracade d'un joli petit cercle de 5 pixels de diamètre */
   static public void drawCircle5(Graphics g,int x,int y) {
      if( !(g instanceof Graphics2D) ) {
         g.drawOval(x-2, y-2, 4, 4);
         return;
      }
      g.drawLine(x-2,y-1,x-2,y+1);
      g.drawLine(x+2,y-1,x+2,y+1);
      g.drawLine(x-1,y-2,x+1,y-2);
      g.drawLine(x-1,y+2,x+1,y+2);
   }

   /** Remplissage d'un joli cercle de 5 pixels de diamètre */
   static public void fillCircle5(Graphics g,int x, int y) {
      if( !(g instanceof Graphics2D) ) {
         g.fillOval(x-2, y-2, 4, 4);
         g.drawOval(x-2, y-2, 4, 4);
         return;
      }
      g.fillRect(x-1,y-1,3,3);
      drawCircle5(g,x,y);
   }

   /** Remplissage d'un joli cercle de 2 pixels de diamètre */
   static public void fillCircle2(Graphics g,int x, int y) {
      if( !(g instanceof Graphics2D) ) {
         g.fillOval(x-1, y-1, 2, 2);
         g.drawOval(x-1, y-1, 2, 2);
         return;
      }
      g.drawLine(x,y-1,x,y+1);
      g.drawLine(x-1,y,x+1,y);
   }
   
   /** Tracé d'un petit triangle plein */
   static public void fillTriangle7(Graphics g, int x, int y) {
      g.drawLine(x, y, x+8, y); x++; y++;
      g.drawLine(x, y, x+6, y); x++; y++;
      g.drawLine(x, y, x+4, y); x++; y++;
      g.drawLine(x, y, x+2, y); x++; y++;
      g.drawLine(x, y, x,   y);
   }

   /** Tracé d'une flèche entre (x,y) et (x1,y1), avec un label éventuel et une taille d'empennage de L pixels */
   static private void drawFleche1(Graphics g,double x,double y,double x1,double y1,int L) {
      g.drawLine((int)x,(int)y,(int)x1,(int)y1);

      double theta,delta;
      if( x!=x1) {
         theta = FastMath.atan( (y1-y)/(x1-x) );
         if( x>x1 ) theta += Math.PI;
      } else {
         if( y<y1 ) theta = Math.PI/2;
         else theta = -Math.PI/2;
      }
      delta = 3.0*Math.PI/4;
      double dx1 = L*FastMath.cos( theta+delta);
      double dy1 = L*FastMath.sin( theta+delta);
      double dx2 = L*FastMath.cos( theta-delta);
      double dy2 = L*FastMath.sin( theta-delta);

      g.drawLine((int)(x1+dx1),(int)(y1+dy1),(int)x1,(int)y1);
      g.drawLine((int)x1,(int)y1,(int)(x1+dx2),(int)(y1+dy2));

   }

   static public void drawFlecheOutLine(Graphics g,double x,double y,double x1,double y1,int L, String s) {
      boolean fd2 =  g instanceof Graphics2D;
      Graphics2D g2 = fd2 ? (Graphics2D)g:null;
      Object aliasing = null;
      Stroke st = null;
      Color c = g.getColor();
      if( fd2 ) {
         g.setColor(Color.black);
         aliasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
         st = g2.getStroke();
         g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g2.setStroke(new BasicStroke(2.4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_MITER));
         drawFleche1(g,x,y,x1,y1,L);
         g2.setStroke(st);
         g.setColor(c);
      }
      drawFleche1(g,x,y,x1,y1,L);

      if( s!=null ) {
         if( x1<x ) x1-=10;
         else x1+=2;
         if( y1>y ) y1+=10;
         else y1-=2;
         if( fd2 ) Util.drawStringOutline(g, s,(int)x1,(int)y1, null,null);
         else g.drawString(s,(int)x1,(int)y1);
      }
      if( fd2 ) g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aliasing);

   }

   static public void drawFleche(Graphics g,double x,double y,double x1,double y1,int L, String s) {
      drawFleche1(g,x,y,x1,y1,L);
      if( s!=null ) {
         if( x1<x ) x1-=10;
         else x1+=2;
         if( y1>y ) y1+=10;
         else y1-=2;
         g.drawString(s,(int)x1,(int)y1);
      }
   }


   static public void drawFillOval(Graphics gr,int x, int y, int w, int h, float transparency,Color bg) {
      if( bg!=null ) gr.setColor(bg);
      try {
         Graphics2D g = (Graphics2D) gr;
         Composite saveComposite = g.getComposite();
         g.setComposite(Util.getImageComposite(transparency));
         g.fillOval(x,y,w,h);
         g.setComposite(saveComposite);
      } catch( Exception e ) { }
      gr.drawOval(x,y,w,h);
   }

   static public void drawFillPolygon(Graphics gr,Polygon pol, float transparency,Color bg) {
      if( bg!=null ) gr.setColor(bg);
      try {
         Graphics2D g = (Graphics2D) gr;
         Composite saveComposite = g.getComposite();
         g.setComposite(Util.getImageComposite(transparency));
         g.fillPolygon(pol);
         g.setComposite(saveComposite);
      } catch( Exception e ) { }
      gr.drawPolygon(pol);
   }

   static public void drawStringOutline(Graphics g, String s, int x, int y, Color c, Color cOutLine) {

      if( c==null ) c=g.getColor();
      if( cOutLine==null ) cOutLine=Color.black;
      if( !(g instanceof Graphics2D ) ) { g.drawString(s, x, y); return; }

      Graphics2D g2 = (Graphics2D) g;
      Color cb = g2.getColor();
      Object aliasing = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      AffineTransform t = g2.getTransform();
      try {
         Font f = g2.getFont();
         FontMetrics fm = g2.getFontMetrics(f);
         GlyphVector v = f.createGlyphVector(fm.getFontRenderContext(), s);
         Shape s1 = v.getOutline();
         g2.translate(x,y);
         Stroke st = g2.getStroke();
         g2.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_BEVEL));
         g.setColor(cOutLine);
         g2.draw(s1);
         g2.setStroke(st);
         g2.setColor(c);
         //          g2.translate(-0.3,-0.3);
         g2.fill(s1);
      } finally {
         g2.setTransform(t);
      }
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aliasing);
      g2.setColor(cb);
   }

   /** Tracé d'un cartouche, éventuellement semi-transparent
    * Retourne les coordonnées pour écrire dedans */
   static public void drawCartouche(Graphics gr,int x, int y, int w, int h,
         float transparency,Color fg, Color bg) {
      if( h%2==1 ) h--;
      Color c = gr.getColor();
      try {
         Graphics2D g = (Graphics2D) gr;
         Composite saveComposite = g.getComposite();
         Composite myComposite = Util.getImageComposite(transparency);
         g.setComposite(myComposite);

         if( bg!=null ) {
            g.setColor(bg);
            g.fillRect(x,y,w,h);
            g.fillArc(x-h/2, y, h, h, 90, 180);
            g.fillArc(x+w-h/2, y, h, h, 90, -180);
         }

         // Bord
         if( fg!=null ) {
            g.setColor(fg);
            g.drawLine(x,y,x+w,y);
            g.drawLine(x,y+h,x+w,y+h);
            g.drawArc(x-h/2, y, h, h, 90, 180);
            g.drawArc(x+w-h/2, y, h, h, 90, -180);
         }

         g.setComposite(saveComposite);

      } catch( Exception e ) {
         // Fond
         if( bg!=null ) {
            gr.setColor(bg);
            gr.fillRect(x,y,w,h);
            gr.fillArc(x-h/2, y, h, h, 90, 180);
            gr.fillArc(x+w-h/2, y, h, h, 90, -180);
         }

         // Bord
         if( fg!=null ) {
            gr.setColor(fg);
            gr.drawLine(x,y,x+w,y);
            gr.drawLine(x,y+h,x+w,y+h);
            gr.drawArc(x-h/2, y, h, h, 90, 180);
            gr.drawArc(x+w-h/2, y, h, h, 90, -180);
         }
      }
      gr.setColor(c);
   }
   
   static public void drawRect(Graphics g,int x,int y,int w, int h, Color ch, Color cb) {
      int r=0;
      g.setColor(ch);
      g.drawLine(x+r,y, x+w-r,y);
      g.drawLine(x,y+r, x,y+h-r);

      g.setColor(cb);
      g.drawLine(x+r,y+h,x+w-r,y+h);
      g.drawLine(x+w,y+r,x+w,y+h-r);
   }

   static public void drawRoundRect(Graphics g,int x,int y,int w, int h,int r, Color ch, Color cb) {
      g.setColor(ch);
      g.drawLine(x+r,y, x+w-r,y);
      g.drawLine(x,y+r, x,y+h-r);
      g.drawArc(x,y,r*2,r*2,90,90);
      g.drawArc(x+w-r*2,y,r*2,r*2,90,-45);
      g.drawArc(x,y+h-r*2,r*2,r*2,180,45);

      g.setColor(cb);
      g.drawLine(x+r,y+h,x+w-r,y+h);
      g.drawLine(x+w,y+r,x+w,y+h-r);
      g.drawArc(x+w-2*r,y+h-2*r,r*2,r*2,270,90);
      g.drawArc(x+w-r*2,y,r*2,r*2,45,-45);
      g.drawArc(x,y+h-r*2,r*2,r*2,180+45,45);
   }

   /**Tracage d'une petite étoile */
   static public void drawStar(Graphics g,int x, int y,Color c) {
      g.setColor(c);
      g.drawLine(x,y-3,x,y-2);
      g.drawLine(x-1,y-1,x+1,y-1);
      g.drawLine(x-3,y,x+3,y);
      g.drawLine(x-2,y+1,x+2,y+1);
      g.drawLine(x-1,y+2,x+1,y+2);
      g.drawLine(x-2,y+3,x-2,y+3);
      g.drawLine(x+2,y+3,x+2,y+3);
   }
   
   /** Dessin d'un triangle "warning" */
   static public void drawWarning(Graphics g,int x,int y, Color bg, Color fg) {
      int h=6;
      int w=5;
      int w2 = 1+ w/2;
      
      // Le triangle
      g.setColor( bg );
      Polygon p = new Polygon( new int[]{ x+w2, x+w+1, x }, new int[] {y, y+h, y+h}, 3);
      g.fillPolygon(p);
      g.drawPolygon(p);
      
      // Le !
      g.setColor( fg );
      g.drawLine( x+w2, y+2, x+w2, y+h-2);
      g.drawLine( x+w2, y+h, x+w2, y+h);
   }
   


   /**
    * Dessin d'un bouton radio
    * @param g le contexte graphique
    * @param x,y la position (coin en haut à gauche)
    * @param colorBord Couleur du bord
    * @param colorFond couleur du fond ou si null, couleur par défaut
    * @param colorCoche couleur de la coche ou si null, couleur par défaut
    * @param selected coche active si true
    */
   static public void drawRadio(Graphics g,int x,int y,
         Color colorBord,Color colorFond,Color colorCoche,boolean selected) {
      int w = CINT.length+1;

      // Une couleur de fond particulière ?
      if( colorFond!=null ) {
         g.setColor(colorFond);
         g.fillRect(x+1,y+1,w,w);

         // Couleur de fond par défaut
      } else {
         for( int i=0; i<CINT.length; i++ ) {
            g.setColor(CINT[i]);
            g.drawLine(x+1,y+1+i,x+CINT.length,y+1+i);
         }
      }

      g.setColor(colorBord);
      g.drawArc(x,y,w,w,0,360);

      // La petite coche de sélection
      if( selected ) {
         g.setColor(colorCoche==null?Color.black:colorCoche);
         g.fillArc(x+2,y+2,w-4,w-4,0,360);
      }
   }


   // Couleurs de la coche
   static final private Color CINT[] = {
      new Color(232,239,246),new Color(243,247,250),
      new Color(255,255,255),new Color(243,247,251),new Color(232,239,247),
      new Color(221,232,243),new Color(215,228,241),
      new Color(210,224,239),new Color(205,221,237)
   };
   
   static private boolean first = true;

   /**
    * Dessin d'une checkbox
    * @param g le contexte graphique
    * @param x,y la position (coin en haut à gauche)
    * @param colorBord Couleur du bord
    * @param colorFond couleur du fond ou si null, couleur par défaut
    * @param colorCoche couleur de la coche ou si null, couleur par défaut
    * @param selected coche active si true
    */
   static public void drawCheckbox(Graphics g,int x,int y,
         Color colorBord,Color colorFond,Color colorCoche,boolean selected) {
      int w = CINT.length+1;
      g.setColor(colorBord);
      g.drawRect(x,y,w,w);

      // Une couleur de fond particulière ?
      if( colorFond!=null ) {
         g.setColor(colorFond);
         g.fillRect(x+1,y+1,w,w);

         // Couleur de fond par défaut
      } else {
         if( first && Aladin.DARK_THEME ) {
            for( int i=0; i<CINT.length; i++ ) CINT[i] = CINT[i].darker();
            first=false;
         }
         for( int i=0; i<CINT.length; i++ ) {
            g.setColor(CINT[i]);
            g.drawLine(x+1,y+1+i,x+CINT.length,y+1+i);
         }
      }

      // La petite coche de sélection
      if( selected ) {
         drawCheck(g,x,y,colorCoche==null?Color.black:colorCoche);
//         g.setColor(colorCoche==null?Color.black:colorCoche);
//         g.fillRect(x+3, y+4, 2, 5);
//         for( int i=0; i<4; i++ ){
//            g.drawLine(x+5+i,y+6-i,x+5+i,y+7-i);
//         }
      }
   }
   
   static public void drawCheck(Graphics g, int x, int y, Color c) {
      g.setColor(c);
      g.fillRect(x+3, y+4, 2, 5);
      for( int i=0; i<4; i++ ) g.drawLine(x+5+i,y+6-i,x+5+i,y+7-i);
     
   }

   //    static public void drawVerticalSplitPaneTriangle(Graphics g,int x, int y) {
   //       g.drawLine(x,y,x+1,y);
   //       g.drawLine(x+2,y+1,x+2,y+1);
   //       g.drawLine(x+3,y+2,x+3,y+2);
   //       g.drawLine(x+4,y+3,x+4,y+15);
   //       g.drawLine(x,y+16,x+3,y+16);
   //
   //       g.drawLine(x+2,y+4,x+2,y+5);
   //       g.drawLine(x+1,y+5,x+1,y+5);
   //
   //       g.drawLine(x+2,y+8,x+2,y+9);
   //       g.drawLine(x+1,y+9,x+1,y+9);
   //
   //       g.drawLine(x+2,y+12,x+2,y+13);
   //       g.drawLine(x+1,y+13,x+1,y+13);
   //    }
   //
   //    static public void drawHorizontalSplitPaneTriangle(Graphics g,int x, int y) {
   //       g.drawLine(x,y,x,y+1);
   //       g.drawLine(x+1,y+2,x+1,y+2);
   //       g.drawLine(x+2,y+3,x+2,y+3);
   //       g.drawLine(x+3,y+4,x+15,y+4);
   //       g.drawLine(x+16,y,x+16,y+3);
   //
   //       g.drawLine(x+4,y+2,x+5,y+2);
   //       g.drawLine(x+5,y+1,x+5,y+1);
   //
   //       g.drawLine(x+8,y+2,x+9,y+2);
   //       g.drawLine(x+9,y+1,x+9,y+1);
   //
   //       g.drawLine(x+12,y+2,x+13,y+2);
   //       g.drawLine(x+13,y+1,x+13,y+1);
   //    }

   //    static public void drawVerticalSplitPaneTriangle(Graphics g,int x, int y) {
   //       g.drawLine(x,y,x,y+4);
   //       g.drawLine(x+1,y+1,x+1,y+3);
   //       g.drawLine(x+2,y+2,x+2,y+2);
   //
   //       y+=7;
   //       g.drawLine(x+2,y,x+2,y+4);
   //       g.drawLine(x+1,y+1,x+1,y+3);
   //       g.drawLine(x,y+2,x,y+2);
   //    }
   //
   //    static public void drawHorizontalSplitPaneTriangle(Graphics g,int x, int y) {
   //       g.drawLine(x,y,x+4,y);
   //       g.drawLine(x+1,y+1,x+3,y+1);
   //       g.drawLine(x+2,y+2,x+2,y+2);
   //
   //       x+=7;
   //       g.drawLine(x,y+2,x+4,y+2);
   //       g.drawLine(x+1,y+1,x+3,y+1);
   //       g.drawLine(x+2,y,x+2,y);
   //    }

   /** Draws an ellipse which can be rotated
    *  @param g - the graphic context we draw on
    *  @param c - color of the ellipse
    *  @param xCenter,yCenter - the "center" of the ellipse
    *  @param semiMA - value of the semi-major axis
    *  @param semiMI - value of the semi-minor axis
    *  @param angle - rotation angle around center
    */
   static public void drawEllipse(Graphics g,double xCenter, double yCenter, double semiMA, double semiMI, double angle) {
      if( g instanceof EPSGraphics ) ((EPSGraphics)g).drawEllipse(xCenter,yCenter,semiMA,semiMI,angle);
      else if( !(g instanceof Graphics2D ) ) drawEllipseOld(g,xCenter,yCenter,semiMA,semiMI,angle);
      else {
         Graphics2D g2d = (Graphics2D)g;
         AffineTransform saveTransform = g2d.getTransform();
         angle = angle*Math.PI/180.0;
         g2d.rotate(angle, xCenter, yCenter);
         g2d.draw(new Ellipse2D.Double(xCenter-semiMA,yCenter-semiMI,semiMA*2,semiMI*2));
         g2d.setTransform(saveTransform);
      }
   }

   /** Draws an ellipse which can be rotated
    *  @param g - the graphic context we draw on
    *  @param c - color of the ellipse
    *  @param xCenter,yCenter - the "center" of the ellipse
    *  @param semiMA - value of the semi-major axis
    *  @param semiMI - value of the semi-minor axis
    *  @param angle - rotation angle around center
    */
   static public void fillEllipse(Graphics g,double xCenter, double yCenter, double semiMA, double semiMI, double angle) {
      //       if( g instanceof EPSGraphics ) ((EPSGraphics)g).fillEllipse(xCenter,yCenter,semiMA,semiMI,angle);
      //       else if( !(g instanceof Graphics2D ) ) drawEllipseOld(g,xCenter,yCenter,semiMA,semiMI,angle);
      //       else {
      Graphics2D g2d = (Graphics2D)g;
      AffineTransform saveTransform = g2d.getTransform();
      angle = angle*Math.PI/180.0;
      g2d.rotate(angle, xCenter, yCenter);
      g2d.fill(new Ellipse2D.Double(xCenter-semiMA,yCenter-semiMI,semiMA*2,semiMI*2));
      g2d.setTransform(saveTransform);
      //       }
   }

   /** Tracée d'une ellipse avec angle, méthode manuelle
    * Utilisé si le contexte graphique ne supporte par Graphics2D */
   static private void drawEllipseOld(Graphics g, double xCenter, double yCenter, double semiMA, double semiMI, double angle) {
      // convert the angle into radians
      angle = angle*Math.PI/180.0;

      // number of iterations
      int nbIt = 30;
      Point[] p = new Point[nbIt];
      double x,y,tmpX,tmpY;
      double curAngle;

      // first, we fill the array
      for(int i=0; i<nbIt; i++) {
         curAngle = 2.0*i/nbIt*Math.PI;
         tmpX = semiMA*FastMath.cos(curAngle);
         tmpY = semiMI*FastMath.sin(curAngle);
         // rotation
         x = tmpX*FastMath.cos(angle)-tmpY*FastMath.sin(angle)+xCenter;
         y = tmpX*FastMath.sin(angle)+tmpY*FastMath.cos(angle)+yCenter;

         //System.out.println(x+" "+y);
         p[i] = new Point((int)x,(int)y);
      }

      // then we draw
      for(int i=0; i<nbIt-1; i++) {
         g.drawLine(p[i].x,p[i].y,p[i+1].x,p[i+1].y);
      }
      // complete the ellipse
      g.drawLine(p[nbIt-1].x,p[nbIt-1].y,p[0].x,p[0].y);
   }

   /** Positionne un tooltip sur un JComponent en vérifiant au préalable
    * qu'il n'aurait pas été déjà positionné */
   static public void toolTip(JComponent c,String s) { toolTip(c,s,false); }
   static public void toolTip(JComponent c,String s,boolean fold) {
      if( fold ) s = fold(s,40,true);
      String o = c.getToolTipText();
      if( s!=null && s.length()==0 ) s=null;
      if( o==s ) return;
      if( o==null || s==null || !o.equals(s) ) c.setToolTipText(s);
   }

   // LE TEMPS QUE THOMAS AIT FINI SES MISES A JOUR HISTOIRE DE NE PAS FAIRE
   // LES CHOSES EN MEME TEMPS
   static public void setCloseShortcut(final JFrame f, final boolean dispose) {
      Util.setCloseShortcut(f,dispose,Aladin.aladin);
   }

   /**
    * met en place les raccourcis clavier ESC et Ctrl-W pour fermer une JFrame
    * Attention : enableEvents(AWTEvent.WINDOW_EVENT_MASK); doit être appelé auparavant dans le constructeur de la JFrame
      );
    * @param f la JFrame à traiter
    * @param dispose si true, on 'dipose' la JFrame pour la fermer, sinon on se contente de faire un hide
    */
   static public void setCloseShortcut(final JFrame f, final boolean dispose, final Aladin aladinInst) {
      if( aladinInst!=null ) {
         f.getRootPane().registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               if( aladinInst.getCommand().robotMode ) {
                  aladinInst.stopRobot(f);
                  return;
               }
               if( dispose ) f.dispose(); else f.setVisible(false);
            }
         },
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
         JComponent.WHEN_IN_FOCUSED_WINDOW
               );
      }

      f.getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { if( dispose ) f.dispose(); else f.setVisible(false); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit ().getMenuShortcutKeyMask()),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
   }

   /**
    * Pause du thread courant
    * @param ms temps de pause en millisecondes
    */
   static public void pause(int ms) {
      try { Thread.currentThread().sleep(ms); }
      catch( Exception e) {}
   }

   /**
    * Decodeur HTTP
    * Temporairement necessaire car URLDecoder n'apparait que dans la JVM 1.2
    */
   //    static public String myDecode(String s) {
   //       char a[] = s.toCharArray();
   //       char d[] = new char[2];
   //       StringBuffer b = new StringBuffer(a.length);
   //       char c;
   //       int mode=0;
   //
   //       for( int i=0; i<a.length; i++) {
   //          c=a[i];
   //          switch(mode ) {
   //             case 0: // Copie simple
   //                if( c!='%' ) { b.append(c=='+'?' ':c); break; }
   //                else mode=1;
   //                break;
   //             case 1:
   //                d[0]=c;
   //                mode=2;
   //                break;
   //             case 2:
   //                d[1]=c;
   //                c = (char)(Integer.parseInt(new String(d),16));
   //                b.append(c);
   //                mode=0;
   //          }
   //       }
   //
   //       return b.toString();
   //    }
   /**
    * Cherche un objet dans un tableau et retourne l'indice correspondant
    * @param o objet à trouver
    * @param array tableau dans lequel on recherche
    * @return premier indice de o dans array, -1 si non trouvé
    */
   static public int indexInArrayOf(Object o, Object[] array) {
      if( o==null || array==null ) return -1;

      for( int i=0; i<array.length; i++ ) {
         if( o.equals(array[i]) ) return i;
      }
      return -1;
   }

   /** Recherche la position d'une chaine dans un tableau de chaine
    * @param s la chaine à chercher
    * @param array le tableau de chaines
    * @param caseInsensitive true si on ignore la distinction maj/min
    * @return position ou -1 si non trouvé
    */
   static public int indexInArrayOf(String s,String[] array) { return indexInArrayOf(s,array,false); }
   static public int indexInArrayOf(String s,String[] array,boolean caseInsensitive) {
      if( s==null || array==null ) return -1;

      for( int i=0; i<array.length; i++ ) {
         if( !caseInsensitive && s.equals(array[i])
               || caseInsensitive && s.equalsIgnoreCase(array[i]) ) return i;
      }
      return -1;
   }

   /** Recherche la position d'un mot dans une chaine en ignorant la case */
   static public int indexOfIgnoreCase(String s,String w) {
      return indexOfIgnoreCase(s,w,0);
   }

   /** Recherche la position d'un mot dans une chaine en ignorant la case
    * à partir de la position indiquée */
   static public int indexOfIgnoreCase(String s,String w,int offset) {
      s = toUpper(s);
      w = toUpper(w);
      return s.indexOf(w,offset);
   }

   /**
    * Remplit une chaine avec des blancs jusqu'à obtenir la longueur désirée
    * @param s
    * @param totLength
    * @return String
    */
   static public String fillWithBlank(String s, int totLength) {
      StringBuffer sb = new StringBuffer(s);
      for( int i=s.length(); i<totLength; i++ ) {
         sb.append(" ");
      }
      return sb.toString();
   }

   /** Met à baskslash avant tous les slashs */
   static public String slash(String s) {
      StringBuffer res = new StringBuffer();
      char a[] = s.toCharArray();
      for( int i=0; i<a.length; i++ ) {
         if( a[i]=='/' ) res.append('\\');
         res.append(a[i]);
      }
      return res.toString();
   }

   /** Nettoie un StringBuffer pour éviter des allocations inutiles */
   static public void resetString(StringBuffer s) {
      int n = s.length();
      if( n==0 ) return;
      s.delete(0,n);
   }

   /** Nettoie un StringBuilder pour éviter des allocations inutiles */
   static public void resetString(StringBuilder s) {
      int n = s.length();
      if( n==0 ) return;
      s.delete(0,n);
   }

   /** Retourne un path sous une forme plus compact.
    * Insére si nécessaire des /.../ au milieu du path si sa taille dépasse width
    */
   static public String getShortPath(String path,int width) {
      try {
         if( path.length()>width ) {
            File f = new File(path);
            StringTokenizer st = new StringTokenizer(f.getCanonicalPath(),Util.FS,true);
            StringBuffer s = new StringBuffer();
            width -= f.getName().length();
            String w=null;
            while( s.length()<width && st.hasMoreTokens() ) {
               w = st.nextToken();
               s.append(w);
            }
            if( st.hasMoreTokens() ) {
               if( w!=null && !w.equals(Util.FS) ) s.append(st.nextToken());
               w = st.nextToken();  // Pour vérifier si par hasard il ne reste que le nom
               if( st.hasMoreElements() ) s.append("..."+Util.FS);
               s.append(f.getName());
            }
            return s.toString();
         }
      } catch( IOException e ) { }

      return path;
   }


   /** Concaténation de paths.
    * et insère le séparateur / uniquement si c'est nécessaire.
    * Remplace les \ éventuelles par / (et réciproquement)
    */
   static public String concatDir(String path1,String path2) { return concatDir(path1,path2,FS.charAt(0)); }
   static public String concatDir(String path1,String path2,char FS) {
      StringBuffer s = new StringBuffer(100);
      char c=0;
      char FSS = FS=='/' ? '\\' : '/';

      for( int i=0; i<2; i++ ) {
         if( c!=FS && s.length()>0 ) s.append(FS);
         String path = i==0 ? path1 : path2;
         if( path==null ) path="";
         else path = path.trim();
         int n = path.length();
         for( int  j=0; j<n; j++ ) {
            c = path.charAt(j);
            if( c==FSS ) c=FS;
            s.append(c);
         }
      }
      return s.toString();
   }

   /** Conversion en majuscules d'une chaine */
   static public String toUpper(String s) {
      char a[] = s.toCharArray();
      for( int i=0; i<a.length; i++ ) a[i] = Character.toUpperCase(a[i]);
      return new String(a);
   }

   /** Conversion en minuscules d'une chaine */
   static public String toLower(String s) {
      char a[] = s.toCharArray();
      for( int i=0; i<a.length; i++ ) a[i] = Character.toLowerCase(a[i]);
      return new String(a);
   }

   /** Conversion en minuscules d'une chaine, chaque première lettre en majuscule*/
   static public String toUpLower(String s) {
      char a[] = s.toCharArray();
      boolean space=true;
      for( int i=0; i<a.length; i++ ) {
         a[i] = space ? Character.toUpperCase(a[i]) : Character.toLowerCase(a[i]);
         space = Character.isSpace(a[i]);
      }
      return new String(a);
   }

   /** Retourne un bouton avec une icone en forme de point d'interrogation */
   static public JButton getHelpButton(final Component f, final String help) {
      JButton h = new JButton(new ImageIcon(Aladin.aladin.getImagette("Help.png")));
      h.setMargin(new Insets(0,0,0,0));
      h.setBorderPainted(false);
      h.setContentAreaFilled(false);
      if( help!=null ) {
         h.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { Aladin.info(f,help); }
         });
      }
      return h;
   }

   /** Teste simplement si l'url existe et répond */
   public static boolean isUrlResponding(String url) {
      try { return isUrlResponding(new URL(url)); }
      catch( MalformedURLException e ) {}
      return false;
   }

   /** Teste simplement si l'url existe et répond */
   public static boolean isUrlResponding(URL url) {
      try {
         HttpURLConnection conn = (HttpURLConnection) url.openConnection();
         conn.setRequestMethod("HEAD");
         int code = conn.getResponseCode();
//                   System.out.println(url+" => ["+code+"]");
//                   if( code/100 == 4 ) return false;
         return code/100 == 2 || code==403;
      } catch( Exception e ) { }
      return false;
   }

   /** Retourne true s'il s'agit dun fichier JPEG couleur */
   static public boolean isJPEGColored(String file) throws Exception {
      RandomAccessFile f = null;
      byte [] buf = null;
      try {
         f = new RandomAccessFile(file,"r");
         buf = new byte[(int)f.length()];
         f.readFully(buf);
      } finally { if( f!=null )  f.close(); }
      return isColoredImage(buf);
   }

   /** Retourne true s'il s'agit d'un buffer contenant une image couleur */
   static public boolean isColoredImage(byte [] buf) throws Exception {
      JButton obs = new JButton();
      Image img = Toolkit.getDefaultToolkit().createImage(buf);
      boolean encore=true;
      while( encore ) {
         try {
            MediaTracker mt = new MediaTracker(Aladin.aladin);
            mt.addImage(img,0);
            mt.waitForID(0);
            encore=false;
         } catch( InterruptedException e ) { }
      }
      int width =img.getWidth(obs);
      int height=img.getHeight(obs);
      if( width==-1 ) { throw new Exception("width = -1"); }

      BufferedImage imgBuf = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
      Graphics g = imgBuf.getGraphics();
      g.drawImage(img,0,0,obs);
      g.finalize(); g=null;

      int taille=width*height;
      int [] rgb = ((DataBufferInt)imgBuf.getRaster().getDataBuffer()).getData();

      //       int rgb[] = new int[taille];
      //       imgBuf.getRGB(0, 0, width, height, rgb, 0, width);

      imgBuf.flush(); imgBuf=null;
      for( int i=0;i<taille; i++ ) {
         int p = rgb[i];
         int red = ((p >>> 16) & 0xFF);
         int green = ((p >>> 8) & 0xFF);
         int blue = (p & 0xFF);
         if( red!=green || green!=blue ) return true;
      }
      return false;
   }



   /**
    *
    * @param dir
    * @return taille du repertoire
    */
   public static long dirSize(File dir) {
      if (dir==null) return 0L;
      if (dir.isFile()) {
         return dir.length();
      }
      long length = 0;
      File[] files = dir.listFiles();
      for (int i = 0; files!=null && i < files.length; i++) {
         File file = files[i];
         length += dirSize(file);
      }

      return length;
   }


   /**
    * Suppression récursive d'un répertoire
    * @param dir
    * @return false sur un échec
    */
   public static boolean deleteDir(File dir) {
      if (dir.isDirectory()) {
         String[] children = dir.list();
         // pour tous les enfants du répertoire
         for (int i=0; i<children.length; i++) {
            // essaye de le supprimer comme un répertoire récursivement
            boolean success = deleteDir(new File(dir, children[i]));
            if (!success) { return false; }
         }
      }
      // et supprime le repertoire maintenant qu'il est vide
      return dir.delete();
   }


   /**
    * Suppression récursive des fichiers selon une expression régulière
    * @param dir
    * @return false sur un échec
    */
   public static boolean deleteDir(File dir, String regex) {
      if (dir.isDirectory()) {
         String[] children = dir.list();
         // pour tous les enfants du répertoire
         for (int i=0; i<children.length; i++) {
            // essaye de le supprimer comme un répertoire récursivement
            boolean success = deleteDir(new File(dir, children[i]), regex);
            if (!success) { return false; }
         }
      }
      else if (dir.getAbsolutePath().matches(regex))
         return dir.delete();
      return true;
   }

   /**
    * touch d'un fichier
    * @param file
    * @param createIfNeeded doit-on créer le fichier s'il n'existe pas
    * @return true on success, false on failure
    */
   public static boolean touch(File file, boolean createIfNeeded) {
      if (file.exists()) {
         if ( ! file.setLastModified(System.currentTimeMillis()) ) {
            return false;
         }
      }
      else if (createIfNeeded) {
         try {
            return file.createNewFile();
         } catch (IOException e) {
            return false;
         }
      }

      return true;
   }

   /** Création si nécessaire des répertoires et sous-répertoire du fichier
    * passé en paramètre
    */
   public static void createPath(String filename) throws Exception {
      File f = new File(new File(filename).getParent());
      f.mkdirs();
      if( !f.exists() ) throw new Exception("Cannot create directory for "+filename);

      //	   File f;
      //	   String FS = filename.indexOf('/')>=0 ? "/" : "\\";
      //
      //	   // Pour accélerer, on teste d'abord l'existence éventuelle du dernier répertoire
      //	   int i = filename.lastIndexOf(FS);
      //	   if( i<0 ) return;
      //	   f = new File( filename.substring(0,i) ) ;
      //	   if( f.exists() ) return;
      //
      //	   for( int pos=filename.indexOf(FS,3); pos>=0; pos=filename.indexOf(FS,pos+1)) {
      //	      f = new File( filename.substring(0,pos) );
      //	      if( !f.exists() ) f.mkdir();
      //	   }
   }


   /** Ouverture de la fenêtre de sélection d'un ou plusieurs fichiers ou répertoires
    * @param title le titre de la fenêtre
    * @param initDir le répertoire d'initialisation
    * @param field le champ à remplir avec le résultat
    * @param mode 0 sans filtre, 1-filtre fits/Healpix, 2-tous les filtres, 3-uniquement la sélection des répertoires
    * @return la liste des fichiers sélectionnés, null si annulation
    */
   static public String dirBrowser(String title,String initDir,JTextField field,int mode) {
      if( Aladin.aladin.configuration.isLookAndFeelJava() ) return dirBrowserJava(title,initDir,field,mode);
      else return dirBrowserNative(null,title,initDir,field);
   }
      
   static public String dirBrowserJava(String title,String initDir,JTextField field,int mode) {
      StringBuilder s = null;
      
      JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory(new java.io.File(initDir==null?".":initDir));
      chooser.setDialogTitle(title);
      
      if( mode==3 ) {
         chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
         
      } else {

         chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

         if( mode>0 ) {
            final FileFilter fitsFilter=  new FileNameExtensionFilter("Fits files (.fits ...)","fits","fit","ftz","fits.gz","ftz.gz","fz","fits.bz2");
            final FileFilter hpxFilter=  new FileNameExtensionFilter("Hpx filter","hpx");

            chooser.addChoosableFileFilter(  fitsFilter);

            chooser.addChoosableFileFilter( new FileFilter() {
               public String getDescription() { return "HEALPix Fits maps"; }
               public boolean accept(File f) {
                  if( f.isDirectory() ) return true;
                  if( !(fitsFilter.accept(f) ||  hpxFilter.accept(f)) ) return false;
                  MyInputStream in=null;
                  try {
                     in = new MyInputStream(new FileInputStream(f));
                     if( (in.getType()&MyInputStream.HEALPIX)!=0) return true;
                  } catch( Exception e ) { }
                  finally { if( in!=null ) try { in.close(); } catch(Exception e) {} }
                  return false;
               }
            });

            if( mode>1  ) {
               chooser.addChoosableFileFilter( new FileFilter() {
                  public String getDescription() { return "Hierarchical Progressive Surveys (HiPS)"; }
                  public boolean accept(File f) {
                     if( !f.isDirectory() ) return false;
                     File f1 = new File(f.getAbsolutePath()+FS+"Norder3");
                     if( f1.isDirectory() ) return true;
                     return false;
                  }
               });
               chooser.addChoosableFileFilter( new FileFilter() {
                  public String getDescription() { return "Fits cubes"; }
                  public boolean accept(File f) {
                     if( f.isDirectory() ) return true;
                     if( !fitsFilter.accept(f) ) return false;
                     MyInputStream in=null;
                     try {
                        in = new MyInputStream(new FileInputStream(f));
                        if( (in.getType()&MyInputStream.CUBE)!=0 
                              && (in.getType()&MyInputStream.RGB)==0) return true;
                     } catch( Exception e ) { }
                     finally { if( in!=null ) try { in.close(); } catch(Exception e) {} }
                     return false;
                  }
               });
               chooser.addChoosableFileFilter( new FileFilter() {
                  public String getDescription() { return "Fits RGB images"; }
                  public boolean accept(File f) {
                     if( f.isDirectory() ) return true;
                     if( !fitsFilter.accept(f) ) return false;
                     MyInputStream in=null;
                     try {
                        in = new MyInputStream(new FileInputStream(f));
                        if( (in.getType()&MyInputStream.RGB)!=0) return true;
                     } catch( Exception e ) { }
                     finally { if( in!=null ) try { in.close(); } catch(Exception e) {} }
                     return false;
                  }
               });
               chooser.addChoosableFileFilter( new FileFilter() {
                  public String getDescription() { return "Multi-Order Coverage map (MOC)"; }
                  public boolean accept(File f) {
                     if( f.isDirectory() ) return true;
                     MyInputStream in=null;
                     try {
                        in = new MyInputStream(new FileInputStream(f));
                        if( (in.getType()&MyInputStream.HPXMOC)!=0 ) return true;
                     } catch( Exception e ) { }
                     finally { if( in!=null ) try { in.close(); } catch(Exception e) {} }
                     return false;
                  }
               });

               chooser.addChoosableFileFilter( new FileNameExtensionFilter("Jpeg or png images (.png, .jpg)","jpg","jpeg","png") );
               chooser.addChoosableFileFilter( new FileNameExtensionFilter("XML tables (.xml, .vot, ...)","xml","vot") );
               chooser.addChoosableFileFilter( new FileNameExtensionFilter("ASCII tables (.txt, .csv, .tbl)","txt","csv","tbl") );
               chooser.addChoosableFileFilter( new FileNameExtensionFilter("Aladin scripts (.ajs)","ajs") );
               chooser.addChoosableFileFilter( new FileNameExtensionFilter("Aladin stack backups (.aj)","aj") );

               chooser.setMultiSelectionEnabled(true);
            }
         }
      }

      if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
         
         // on va mettre à la queue leu leu tous les fichiers et répertoires
         // sélectionnés, avec des guillemets et un espace sépareur si plus d'un
         boolean first=true;
         for( File f : chooser.getSelectedFiles() ) {
            String file = f.getAbsolutePath();
            if( s==null ) s = new StringBuilder(file); 
            else {
               if( first ) s = new StringBuilder(Q(s.toString()));
               first=false;
               s.append(" "+Q(file));
            }
         }
         
         // Si on ne sélectionne qu'un répertoire, il arrive par cette méthode uniquement !?
         if( s==null && chooser.getSelectedFile()!=null ) s = new StringBuilder(chooser.getSelectedFile()+"");
         
         if( s!=null ) field.setText(s+"");
      } 

      return s==null ? null : s+"";
   }

   static private String Q(String s) { return "\""+s+"\""; }

   private static final String DEFAULT_FILENAME = "-";

   // Sélection d'un fichier (FileDialog Native)
   static public String dirBrowserNative(Frame parent, String title,String initDir,JTextField field) {
      FileDialog fd = new FileDialog(parent,title);
      if( initDir!=null ) fd.setDirectory(initDir);

      // (thomas) astuce pour permettre la selection d'un repertoire
      // (c'est pas l'ideal, mais je n'ai pas trouve de moyen plus propre en AWT)
      fd.setFile(DEFAULT_FILENAME);
      fd.setVisible(true);
      String dir = fd.getDirectory();
      String name =  fd.getFile();
      // si on n'a pas changé le nom, on a selectionne un repertoire
      boolean isDir = false;
      if( name!=null && name.equals(DEFAULT_FILENAME) ) {
         name = "";
         isDir = true;
      }
      String t = (dir==null?"":dir)+(name==null?"":name);
      if( field!=null ) field.setText(t);
      if( (name!=null && name.length()>0) || isDir ) return t;

      return null;
   }

   static private String HEX = "0123456789ABCDEF";

   /** Affichage en hexadécimal d'un caractère */
   static public String hex(char c) { return hex((int)c); }

   /** Affichage en hexadécimal d'un octet */
   static public String hex(int b) {
      return ""+HEX.charAt(b/16)+HEX.charAt(b%16);
   }

   /**
    * retourne un objet 'Composite' (pour un footprint) à partir d'un niveau d'opacité
    * Le passage par une méthode permettrait d'appliquer une fonction de transfert
    *
    * @param opacityLevel nvieau d'opacité désiré
    * @return objet Composite correspondant
    */
   static public Composite getFootprintComposite(float opacityLevel) {
      return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityLevel);
   }

   /**
    * retourne un objet 'Composite' (pour un footprint) à partir d'un niveau d'opacité
    * Le passage par une méthode permettrait d'appliquer une fonction de transfert
    *
    * @param opacityLevel nvieau d'opacité désiré
    * @return objet Composite correspondant
    */
   static public Composite getImageComposite(float opacityLevel) {
      return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityLevel);
   }

   /** Tracé d'un rectangle avec aplat semi-transparent.
    * Si la transparence n'est pas activée, tracé d'un simple rectangle */
   static public void drawArea(Aladin aladin,Graphics g,Rectangle rect,Color color) {
      drawArea(aladin,g,rect.x,rect.y,rect.width,rect.height,color,0.15f,true);
   }
   static public void drawArea(Aladin aladin,Graphics g,int x,int y,int width,int height,
         Color color,float transparency,boolean withBord) {
      Color c = g.getColor();
      g.setColor(color);
      if( g instanceof Graphics2D && aladin.configuration.isTransparent() ) {
         Graphics2D g2d = (Graphics2D)g;
         Composite saveComposite = g2d.getComposite();
         Composite myComposite = Util.getImageComposite(transparency);
         g2d.setComposite(myComposite);
         g2d.fillRect(x,y, width, height);
         g2d.setComposite(saveComposite);
         int x1=x+width;
         int y1=y+height;
         if( withBord ) {
            g2d.drawLine(x,y, x1, y);
            g2d.drawLine(x,y, x, y1);
            g2d.setColor(color);
            g2d.drawLine(x1,y1, x1,y);
            g2d.drawLine(x1,y1, x,y1);
         }

      } else if( withBord ) g.drawRect(x,y, width,height);
      g.setColor(c);
   }


   static private ImageIcon DESC_ICON;
   /**
    * returns the triangle icon used in JTable to indicate a descending sort
    * @return the corresponding icon
    */
   static public ImageIcon getDescSortIcon() {
      if( DESC_ICON==null ) DESC_ICON = new ImageIcon(Aladin.aladin.getImagette("arrow_up.gif"));
      return DESC_ICON;
   }

   static private ImageIcon ASC_ICON;
   /**
    * returns the triangle icon used in JTable to indicate an ascending sort
    * @return the corresponding icon
    */
   static public ImageIcon getAscSortIcon() {
      if( ASC_ICON==null ) ASC_ICON = new ImageIcon(Aladin.aladin.getImagette("arrow_down.gif"));
      return ASC_ICON;
   }

   /** Diminue la priorité du Thread runme par rapport au Thread ref */
   static public void decreasePriority(Thread ref,Thread runme) {
      try {
         runme.setPriority(ref.getPriority()-2);
      } catch( Exception e ) {}
   }
   
   static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm";
   static final SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
   static {
      TimeZone utc = TimeZone.getTimeZone("UTC");
      sdf.setTimeZone(utc);
   }
   
   /** Retourne le temps Unix à partir d'une DATE ISO UTC */
   public static long getTimeFromISO(String date_iso) throws Exception {
      if( date_iso.indexOf('T')<0) date_iso += "T00:00";
      if( date_iso.charAt(date_iso.length()-1)=='Z' ) date_iso=date_iso.substring(0,date_iso.length()-1);
      Date date = sdf.parse(date_iso);
      return date.getTime();
   }

   /** retourne une date ISO 8601 (YYYY-MM-DD) à partir d'une valeur MJD */
   static public String getDateFromMJD(String mjd) {
      try {
         String s = Astrodate.JDToDate( Astrodate.MJDToJD( Double.parseDouble(mjd)));
         int i = s.indexOf("T");
         return i>0 ? s.substring(0,i) : s;
      }catch( Exception e) {}
      return "";
   }

   /** retourne un temps en milliseconde sous une forme lisible 3j 5h 10mn 3.101s */
   static public String getTemps(long ms) { return getTemps(ms,false);  }
   static public String getTemps(long ms,boolean round) {
      StringBuffer s = new StringBuffer();
      if( ms>86400000 ) { long j = ms/86400000; ms -= j*86400000; s.append(j+"j"); }
      if( ms>3600000 ) { long h = ms/3600000; ms -= h*3600000; if( s.length()>0 ) s.append(' '); s.append(h+"h"); }
      if( ms>60000 ) { long m = ms/60000; ms -= m*60000; if( s.length()>0 ) s.append(' '); s.append(m+"m"); }
      if( s.length()>0 ) s.append(' '); s.append( (round ? ""+ms/1000 : ""+ms/1000.)+"s");
      return s.toString();
   }

   //    static private boolean tryNano=false;
   //    static private Method nanoMethod=null;

   /** Récupération du temps en ms via la méthode System.nanoTime() si possible
    * sinon via la méthode classique System.currentTimeMillis().
    * @param unit 0-ns 1:ms 2:s
    */
   static final public long getTime() { return getTime(1); }
   static final public long getTime(int unit) {
      return unit==1 ? System.currentTimeMillis()
            : unit==0 ? System.nanoTime() : System.currentTimeMillis()/1000L;

            //       if( !tryNano ) {
            //          tryNano=true;
            //          try { nanoMethod = System.class.getMethod("nanoTime",new Class[] {}); }
            //          catch( Exception e) { }
            //       }
            //       if( nanoMethod!=null ) {
            //          try { return ((Long)(nanoMethod.invoke((Object)null, (Object[])null))).longValue()/(unit==1? 1000000L : unit==2 ? 1000000000L : 1); }
            //          catch( Exception e) { nanoMethod=null; }
            //       }
            //       long t=System.currentTimeMillis();
            //       if( unit==0 ) return t*1000000L;
            //       if( unit==2 ) return t/1000L;
            //       return t;

   }

   /** Retourne la lettre code d'un champ TFORM FITS nD */
   static final public char getFitsType(String form) {
      int l=form.indexOf('(');
      if( l==-1 ) l=form.length();
      return form.charAt(l-1);
   }

   /** retourne la taille du champs FITS exprimé sous la forme nD(xxx) ou nPD(xxx) */
   static final public int binSizeOf(String form) throws Exception {
      try {
         int l=form.indexOf('(');
         if( l==-1 ) l=form.length();
         if( l==1 ) return binSizeOf(form.charAt(0),1);
         if( l>1 && form.charAt(l-2)=='P' ) return 8;
         int n = Integer.parseInt( form.substring(0,l-1) );
         return binSizeOf(form.charAt(l-1),n);
      } catch( Exception e ) {
         System.err.println("Pb pour "+form);
         throw e;
      }
   }

   /** Retourne le nombre d'octets d'un champ BINTABLE
    * @param n le nombre d'items
    * @param type le code du type de données
    * @return le nombre d'octets
    */
   static final public int binSizeOf( char type, int n) {
      if( type=='X' ) return n/8 + (n%8>0 ? 1:0);  // Champ de bits
      int sizeOf = type=='L'? 1:
         type=='B'? 1:
            type=='I'? 2:
               type=='J'? 4:
                  type=='A'? 1:
                     type=='E'? 4:
                        type=='D'? 8:
                           type=='K'? 8:
                              type=='C'? 8:
                                 type=='M'? 16:
                                    type=='P'? 8:
                                       0;
      return sizeOf * n;
   }

   //    HashMap<String, String> getNextJsonObj(MyInputStream in) {
   //       while( encore ) {
   //          char ch = in.g
   //          switch
   //       }
   //    }

   
   /** Affiche dans une unité cohérente le chiffre peut être suivi d'une autre unité
    * par défaut il s'agit de BYTES. ex; 1024m => 1g */
   static final public String getUnitDisk(String val) throws Exception {
      int i=0;
      int unit;
      long size;
      for( i=0; i<val.length() && Character.isDigit(val.charAt(i)); i++);
      if( i==val.length() ) {
         size = (long)Double.parseDouble(val.trim());
         unit=0;
      } else {
         String s = val.substring(i).trim();
         unit = Util.indexInArrayOf(s, unites, false);
//         if( unit==-1 ) throw new Exception("Unit unknown !");
         if( unit==-1 ) return val;
         size = (long)Double.parseDouble( val.substring(0,i));
      }
      return getUnitDisk(size,unit,2);
   }

   /**
    * Affiche le chiffre donné avec une unité de volume disque (K M T)
    * @param val taille en octets
    * @param unit l'unité de départ (par défaut le byte)
    * @param format le nombre de décimals après la virgule (par défaut 2)
    * @return le volume disque dans une unite coherente + l'unite utilisee
    */
   static final public String unites[] = {"B","KB","MB","GB","TB","PB","EB","ZB"};
   static final public String getUnitDisk(long val) { return getUnitDisk(val, 0, 2); }
   static final public String getUnitDisk(long val, int unit, int format) {
      long div,rest=0;
      boolean neg=false;
      if( val<0 ) { neg=true; val=-val; }
      while (val >= 1024L && unit<unites.length-1) {
         unit++;
         div = val / 1024L;
         rest = val % 1024L;
         val=div;
      }
      NumberFormat nf = NumberFormat.getInstance();
      nf.setMaximumFractionDigits(format);
      double x = val+rest/1024.;
      return (neg?"-":"")+nf.format(x)+unites[unit];
   }

   public static ArrayList<File> getFiles(String path, final String suffix) {
      ArrayList<File> flist = new ArrayList<File>();
      File[] files = (new File(path)).listFiles();
      for (File file : files) {
         if (file.isDirectory())
            flist.addAll(getFiles(file.getAbsolutePath(), suffix));
         else if (file.getName().endsWith(suffix))
            flist.add(file);

      }
      return flist;
   }

   public static boolean find(String path, String suffix) {
      File[] files = (new File(path)).listFiles();
      for (File file : files) {
         if (file.isDirectory())
            return find(file.getAbsolutePath(), suffix);
         else if (file.getName().endsWith(suffix))
            return true;

      }
      return false;
   }

   public static void httpsInit() {
      HostnameVerifier hv = new HostnameVerifier() {
         public boolean verify(String urlHostName, SSLSession session) {
            System.out.println("Warning: URL Host: " + urlHostName
                  + " vs. " + session.getPeerHost());
            return true;
         }
      };

      HttpsURLConnection.setDefaultHostnameVerifier(hv);

      TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {

               public void checkClientTrusted(X509Certificate[] chain,
                     String authType) throws CertificateException {
                  // pas de vérification
               }

               public void checkServerTrusted(X509Certificate[] chain,
                     String authType) throws CertificateException {
                  // on autorise tous les serveurs

               }

               public X509Certificate[] getAcceptedIssuers() {
                  return null;
               }
            }
      };

      try {
         SSLContext sc = SSLContext.getInstance("SSL");
         sc.init(null, trustAllCerts, null);
         HttpsURLConnection
         .setDefaultSSLSocketFactory(sc.getSocketFactory());
      } catch (NoSuchAlgorithmException nsae) {
         nsae.printStackTrace();
      } catch (KeyManagementException kme) {
         kme.printStackTrace();
      }
   }


   /**
    * @param x angle in degrees
    * @return Tan()
    */
   public static final double tand(double x) { return Math.tan( x*(Math.PI/180.0) ); }

   /** Cos() in degres */
   public static final double cosd(double x) { return Math.cos( x*(Math.PI/180.0) ); }


   /**
    * build a VOTable document from a list of Forme
    * @param formes list of Forme
    * @return VOTable document with 2 columns: ra, dec corresponding to the positions of the objects
    */
   public static String createVOTable(List<Forme> formes) {
      StringBuffer sb = new StringBuffer()
      .append("<?xml version=\"1.0\"?>\n")
      .append("<VOTABLE version=\"1.2\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n")
      .append("xmlns=\"http://www.ivoa.net/xml/VOTable/v1.2\"\n")
      .append("xmlns:stc=\"http://www.ivoa.net/xml/STC/v1.30\" >\n")
      .append("<RESOURCE>\n")
      .append("<TABLE>\n")
      .append("<GROUP ID=\"J2000\" utype=\"stc:AstroCoords\">\n")
      .append("  <PARAM datatype=\"char\" arraysize=\"*\" ucd=\"pos.frame\" name=\"cooframe\"\n")
      .append("    utype=\"stc:AstroCoords.coord_system_id\" value=\"ICRS\" />\n")
      .append("  <FIELDref ref=\"ra\"/>\n")
      .append("  <FIELDref ref=\"dec\"/>\n")
      .append("</GROUP>\n")
      .append("<FIELD name=\"RA\" ID=\"ra\" ucd=\"pos.eq.ra;meta.main\" ref=\"J2000\"\n")
      .append("  utype=\"stc:AstroCoords.Position2D.Value2.C1\"\n")
      .append("  datatype=\"double\" unit=\"deg\" />\n")
      .append("<FIELD name=\"Dec\" ID=\"dec\" ucd=\"pos.eq.dec;meta.main\" ref=\"J2000\"\n")
      .append("  utype=\"stc:AstroCoords.Position2D.Value2.C2\"\n")
      .append("  datatype=\"double\" unit=\"deg\" />\n")
      .append("<DATA><TABLEDATA>\n");

      for (Forme forme: formes) {
         sb.append(String.format((Locale)null, "<TR><TD>%.5f</TD><TD>%.5f</TD></TR>\n", forme.o[0].getRa(), forme.o[0].getDec()));
      }

      sb.append("</TABLEDATA></DATA>\n")
      .append("</TABLE>\n")
      .append("</RESOURCE>\n")
      .append("</VOTABLE>");

      return sb.toString();

   }

   static public String extractJSON(String key,String s) {
      String k="\""+key+"\"";
      int o1 = s.indexOf(key);
      if( o1<0 ) return null;
      int o2 = s.indexOf('"',o1+k.length()+1);
      if( o2<0 ) return null;
      return Tok.unQuote( (new Tok(s.substring(o2),"},")).nextToken() );
   }

   static private String B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

   /** Conversion en base 64 - code Fox */
   static public String toB64(byte [] p) {
      StringBuffer res= new StringBuffer((int)(p.length*1.25));

      char [] tab = B64.toCharArray();
      char [] b4 = new char[4];
      int c, c3, nb=0;
      int i=0;

      while( i<p.length ) {
         c = p[i++]&0xff;
         c3 = c<<16;
         b4[2] = b4[3] = '=';

         if( i<p.length ) {
            c = p[i++]&0xff;
            c3 |= (c<<8);
            b4[2]=0;
            if( i<p.length ) {
               c = p[i++]&0xff;
               c3 |=  c;
               b4[3]=0;
            }
         }
         if( b4[3]==0 ) b4[3] = tab[c3&63];
         c3 >>= 6;
            if( b4[2]==0 ) b4[2] = tab[c3&63];
            c3 >>= 6;
            b4[1] = tab[c3&63];
            c3 >>= 6;
         b4[0] = tab[c3&63];
         res.append(b4);
         nb += 4;
         if( (nb%64)==0 ) res.append(CR+" ");
      }
      return res.toString();
   }

   /** Backslash ce qu'il faut en JSON */
   static public String escapeJSON( String s ) {
      if( s.indexOf('"')<0 && s.indexOf('\\')<0 ) return s;
      char [] a = s.toCharArray();
      StringBuilder s1 = new StringBuilder(a.length);
      for( int i=0; i<a.length; i++ ) {
         char ch = a[i];
         if( ch=='"' ) s1.append('\\');
         else if( ch=='\\' && i<a.length-1 ) {
            char ch1=a[i+1];
            if( ch1!='n' && ch1!='t' ) s1.append('\\');
         }
         s1.append(ch);
      }
      return s1.toString();
   }

   /**
	 * Method to parse date given in natural language.
	 * Parses date in the below formats only: the delimiters could include "-" or "/" or " "
	 * <ol><li>dd-MM-yyyy</li>
	 * <li>dd-MMM-yyyy</li>
	 * <li>yyyy-MM-dd</li>
	 * <li>yyyy-MMM-dd</li>
	 * </ol>
	 * 
	 * Including the combination resulting with time provided in <b>HH:mm</b> or <b>HH:mm:ss</b> formats.
	 * 
	 * @param input
	 * @return 
	 * @throws ParseException 
	 */
	public static Date parseDate(String input) throws ParseException {
		String dateFormat = null;
		Date date = null;
		input = input.trim();
	    
		if(input.contains(" ") || input.contains("/") || input.contains("-")){
			input = input.replaceAll("[\\s/-]+", "-");
			SimpleEntry<String, String> timeFormat = null;

			int hourMinDelimiter = input.indexOf(":");
			if (hourMinDelimiter != -1) {
				if (input.indexOf(":", hourMinDelimiter + 1)==-1) {
					timeFormat = new SimpleEntry<String, String>("-\\d{1,2}:\\d{1,2}$", "-HH:mm");
				} else {
					timeFormat = new SimpleEntry<String, String>("-\\d{1,2}:\\d{1,2}:\\d{1,2}$", "-HH:mm:ss");
				}
			}
			
			StringBuffer completeRegEx = null;
			for (String regExp : DATE_FORMATS.keySet()) {
				if (timeFormat != null) {
					completeRegEx = new StringBuffer(regExp).append(timeFormat.getKey());
					 if (input.matches(completeRegEx.toString())) {
						 dateFormat = DATE_FORMATS.get(regExp) + timeFormat.getValue();
						 break;
					}
				} else if(input.matches(regExp)){
					dateFormat = DATE_FORMATS.get(regExp);
					break;
				}
			}
		}
		
		if (dateFormat!=null && !dateFormat.isEmpty()) {
			DateFormat dateformat = new SimpleDateFormat(dateFormat);
			dateformat.setTimeZone(TimeZone.getTimeZone("UTC"));
		    try {
				date =  dateformat.parse(input);
//				System.out.println(date);
			} catch (ParseException pe) {
				throw pe;
			} 
		} 
		
		return date;
	}
	
	/**
	 * Method to convert date to MJD.
	 * @param date
	 * @return dateinMJD
	 */
	public static double ISOToMJD(Date date) {
		double result = 0.0d;
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		result = Astrodate.dateToJD(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
				cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
		System.out.println(cal.get(Calendar.YEAR)+" " +cal.get(Calendar.MONTH)+1+" " +cal.get(Calendar.DAY_OF_MONTH)+" " +
				cal.get(Calendar.HOUR_OF_DAY)+" " +cal.get(Calendar.MINUTE)+" " +cal.get(Calendar.SECOND));
		result = Astrodate.JDToMJD(result);
		return result;
	}
	
	public static List<Coord> getRectangleVertices(double ra, double dec, double width, double height) {
		List<Coord> rectVertices = new ArrayList<Coord>();
		width = width/2;
		height = height/2;
		rectVertices.add(new Coord(ra-width, dec-height));
		rectVertices.add(new Coord(ra+width, dec-height));
		rectVertices.add(new Coord(ra+width, dec+height));
		rectVertices.add(new Coord(ra-width, dec+height));
		
		return rectVertices;
		
	}
	
	/**
	 * Method to extract resource of type="results"
	 * @param resourceSet
	 * @return results typed SavotResource
	 */
	public static SavotResource populateResultsResource(SavotPullParser savotParser) {
		ResourceSet resourceSet = savotParser.getAllResources().getResources();
		SavotResource resultsResource = null;
		if (resourceSet!=null && resourceSet.getItemCount()>0) {
			for (int i = 0; i < resourceSet.getItemCount(); i++) {
				SavotResource resource= resourceSet.getItemAt(i);
				if (resource.getType().equalsIgnoreCase(RESULTS_RESOURCE_NAME)) {
					resultsResource = resource;
				}
			}
		}
		return resultsResource;
	}
	
	/**
	 * Methode statique qui dit si une expression est une chaine de caractÃ¨re ou non
	 * @param str La chaine a parser
	 * @return vrai ou faux
	 * @author Mallory Marcot
	 */
	public static boolean isString(String str) {
		
		String str_upper = new String(str.toUpperCase());
		for(int i=0; i<str_upper.length(); i++) {
			if( LISTE_CARACTERE_STRING.contains(str_upper.charAt(i) + ""))
				return true;
		}
		
		return false;
	}
	
	/**
	 * Methode qui juge si il faut entourer l'expression de simple quote ou non
	 * @param str Expression Ã  parser
	 * @return La chaine correctement formattÃ©e
	 * @author Mallory Marcot
	 */
	public static String formatterPourRequete(boolean considerAsString, String str) {
		if((considerAsString || isString(str)) && !str.toLowerCase().equals("null") && !dejaQuote(str))
			str = "'" + str + "'";
		
		return " "+ str;
	}
	
	
	/**
	 * Permet de savoir si une chaine de caractÃ¨re est entourÃ©e de simple quote
	 * ou non
	 * @param str La chaine Ã  tester
	 * @return oui ou non
	 * @author Mallory Marcot
	 */
	public static boolean dejaQuote(String str) {
		boolean ret = false;
		
		if(str.charAt(0) == '\'' && str.charAt(str.length()-1) == '\'')
			ret = true;
		
		return ret;
	}
	
	public static Plan getPlanByLabel(Plan[] plans, String aladinFileName) {
		Plan plan = null;
		for (int i = 0; i < plans.length; i++) {
			if (plans[i].label!=null && plans[i].label.equalsIgnoreCase(aladinFileName)) {
				plan = plans[i];
			}
		}
		return plan;
	}
	
	public static String getDomainNameFromUrl(String url) throws URISyntaxException {
		String result = url;
	    URI uri = new URI(url);
	    String domain = uri.getHost();
	    result =  domain.startsWith("www.") ? domain.substring(4) : domain;
	    return result;
	}

   // PAS ENCORE TESTE
   //    /** Extrait le premier nombre entier qui se trouve dans la chaine à partir
   //     * d'une certaine position
   //     * Ne prend pas en compte un signe éventuel
   //     * @param s la chaine à traiter
   //     * @param pos la position de départ
   //     * @return le nombre trouvé, ou 0 si aucun
   //     */
   //    public int getInteger(String s) { return getInteger(s,0); }
   //    public int getInteger(String s,int pos) {
   //       int i;
   //       int n=s.length();
   //       for( i=pos; i<n && !Character.isDigit(s.charAt(i)); i++);
   //
   //       int val;
   //       for( val=0; i<n && Character.isDigit(s.charAt(i)); i++) {
   //          val = val*10 + (int)(s.charAt(i)-'0');
   //       }
   //
   //       return val;
   //    }
	
//	static public void main(String [] a) {
//       try {
//         System.out.println("==>"+ getUnitDisk(1024*1024*1024));
//         System.out.println("==>"+ getUnitDisk((1024*1024)+"KB"));
//         System.out.println("==>"+ getUnitDisk((1024)+"MB"));
//      } catch( Exception e ) {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//      }
//	}


}
