//
//Copyright 1999-2005 - Universite Louis Pasteur / Centre National de la
//Recherche Scientifique
//
//------
//
//Address: Centre de Donnees astronomiques de Strasbourg
//       11 rue de l'Universite
//       67000 STRASBOURG
//       FRANCE
//Email:   question@simbad.u-strasbg.fr
//
//-------
//
//In accordance with the international conventions about intellectual
//property rights this software and associated documentation files
//(the "Software") is protected. The rightholder authorizes :
//the reproduction and representation as a private copy or for educational
//and research purposes outside any lucrative use,
//subject to the following conditions:
//
//The above copyright notice shall be included.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
//OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON INFRINGEMENT,
//LOSS OF DATA, LOSS OF PROFIT, LOSS OF BARGAIN OR IMPOSSIBILITY
//TO USE SUCH SOFWARE. IN NO EVENT SHALL THE RIGHTHOLDER BE LIABLE
//FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
//THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
//
//For any other exploitation contact the rightholder.
//
//                     -----------
//
//Conformement aux conventions internationales relatives aux droits de
//propriete intellectuelle ce logiciel et sa documentation sont proteges.
//Le titulaire des droits autorise :
//la reproduction et la representation a titre de copie privee ou des fins
//d'enseignement et de recherche et en dehors de toute utilisation lucrative.
//Cette autorisation est faite sous les conditions suivantes :
//
//La mention du copyright portee ci-dessus devra etre clairement indiquee.
//
//LE LOGICIEL EST LIVRE "EN L'ETAT", SANS GARANTIE D'AUCUNE SORTE.
//LE TITULAIRE DES DROITS NE SAURAIT, EN AUCUN CAS ETRE TENU CONTRACTUELLEMENT
//OU DELICTUELLEMENT POUR RESPONSABLE DES DOMMAGES DIRECTS OU INDIRECTS
//(Y COMPRIS ET A TITRE PUREMENT ILLUSTRATIF ET NON LIMITATIF,
//LA PRIVATION DE JOUISSANCE DU LOGICIEL, LA PERTE DE DONNEES,
//LE MANQUE A GAGNER OU AUGMENTATION DE COUTS ET DEPENSES, LES PERTES
//D'EXPLOITATION,LES PERTES DE MARCHES OU TOUTES ACTIONS EN CONTREFACON)
//POUVANT RESULTER DE L'UTILISATION, DE LA MAUVAISE UTILISATION
//OU DE L'IMPOSSIBILITE D'UTILISER LE LOGICIEL, ALORS MEME
//QU'IL AURAIT ETE AVISE DE LA POSSIBILITE DE SURVENANCE DE TELS DOMMAGES.
//
//Pour toute autre utilisation contactez le titulaire des droits.
//
package cds.tools;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import javax.net.ssl.*;
import javax.swing.*;

import cds.aladin.Aladin;
import cds.aladin.Forme;
import cds.aladin.MyInputStream;
import cds.aladin.Tok;
import cds.image.EPSGraphics;

/**
 * Diverses m�thodes utilitaires
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

	/** Ouverture d'un MyInputStream avec le User-Agent correspondant � Aladin */
    static public MyInputStream openStream(String u) throws Exception { return openStream(new URL(u),true); }
    static public MyInputStream openStream(String u,boolean useCache) throws Exception {
       return openStream(new URL(u),useCache);
    }
    static public MyInputStream openStream(URL u) throws Exception { return openStream(u,true); }
    static public MyInputStream openStream(URL u, boolean useCache) throws Exception {
	   URLConnection conn = u.openConnection();
	   if( !useCache ) conn.setUseCaches(false);
	   conn.setConnectTimeout(10000);
// DEJA FAIT DANS Aladin.myInit() => mais sinon ne marche pas en applet
	   if( conn instanceof HttpURLConnection ) {
	      HttpURLConnection http = (HttpURLConnection)conn;
	      http.setRequestProperty("http.agent", "Aladin/"+Aladin.VERSION);
	   }

       MyInputStream mis = new MyInputStream(openConnectionCheckRedirects(conn));
//       MyInputStream mis = new MyInputStream(conn.getInputStream());
	   return mis.startRead();
	}

    /**
     * Java does not follow HTTP --> HTTPS redirections by default
     * This code allows to retrieve the "final" stream from a URLConnection, after following the redirections
     *
     * Code copied from http://download.oracle.com/javase/1.4.2/docs/guide/deployment/deployment-guide/upgrade-guide/article-17.html
     */
    static private InputStream openConnectionCheckRedirects(URLConnection conn) throws IOException {
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
            if( posV+4>posE ) return s;   // d�j� pas bcp de d�cimales
            return s.substring(0,posV+4)+s.substring(posE);
         }
      }

      // cas g�n�ral
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

      // Probl�me en cas de notation scientifique

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

   /**
    * Tokenizer sp�cialis� : renvoie le tableau des chaines s�par�s par sep ssi freq(c1) dans s == freq(c2) dans s
    * exemple : tokenize...("xmatch 2MASS( RA , DE ) GSC( RA2000 , DE2000 )", ' ', '(', ')' ) renvoie :
    * {"xmatch" , "2MASS( RA , DE )", "GSC( RA2000 , DE2000 )"}
    * Le d�limiteur n'est pas consid�r� comme un token
    * @param s
    * @param sep ensemble des d�limiteurs
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

		// ajout du dernier �l�ment
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

	/** Utilitaire pour ajouter des blancs apr�s un mot afin de lui donner une taille particuli�re
	 * @param key le mot � aligner
	 * @param n le nombre de caract�res souhait�s
	 * @return le mot align�, ou si trop grand, avec juste un espace derri�re
	 */
	static public String align(String key,int n) { return align(key,n,""); }
    static public String align(String key,int n,String suffixe) {
		int i=key.length();
		if( i>=n ) return key+ suffixe +" ";
		StringBuffer s = new StringBuffer();
		for( int j=0; j<n-i; j++ ) s.append(' ');
		return key+suffixe+s;
	}

    /** Utilitaire pour ajouter des z�ros avant un nombre pour l'aligner sur 3 digits
     * @param x la valeur � aligner
     * @return le nombre align�
     */
    static public String align3(int x) {
        if( x<10 ) return "00"+x;
        else if( x<100 ) return "0"+x;
        else return ""+x;
    }

    /** Utilitaire pour ajouter des z�ros avant un nombre pour l'aligner sur 2 digits
     * @param x la valeur � aligner
     * @return le nombre align�
     */
    static public String align2(int x) {
        if( x<10 ) return "0"+x;
        else return ""+x;
    }

	/** Arrondit et limite le nombre de d�cimales
	 * @param d nombre � arrondir
	 * @param nbDec nb de d�cimales � conserver
	 * @return le nombre arrondi en conservant nbDec d�cimales
	 */
	public static double round(double d, int nbDec) {
		double fact = Math.pow(10,nbDec);
		return Math.round(d*fact)/fact;
	}

    /**
     * Utilitaire pour ins�rer des \n dans un texte afin de replier les lignes
     * @param s Le texte � "folder"
     * @param taille le nombre maximum de caract�res par ligne (80 par d�faut)
     * @param html true si on met en forme en HTML en vu de l'usage dans
     *        un widget SWING (<html>... <br>... </html>)
     * @return le texte avec les retours � la ligne
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
	 * @param cm Le mod�le de couleur
	 * @param component 0-Rouge, 1-Vert, 2-Bleu
	 * @return les 256 valeurs de la table pour la composante indiqu�e
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
     * @param g Le contexte graphique concern�
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

    /** Tracade d'un joli petit cercle de 7 pixels de diam�tre */
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

    /** Remplissage d'un joli cercle de 7 pixels de diam�tre */
    static public void fillCircle8(Graphics g,int x, int y) {
       if( !(g instanceof Graphics2D) ) {
          g.drawOval(x-4, y-4, 8, 8);
          g.fillOval(x-4, y-4, 8, 8);
          return;
       }
       g.fillRect(x-2,y-2,6,6);
       drawCircle8(g,x,y);
    }

    /** Tracade d'un joli petit cercle de 7 pixels de diam�tre */
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

    /** Remplissage d'un joli cercle de 7 pixels de diam�tre */
    static public void fillCircle7(Graphics g,int x, int y) {
       if( !(g instanceof Graphics2D) ) {
          g.fillOval(x-3, y-3, 6, 6);
          g.drawOval(x-3, y-3, 6, 6);
          return;
       }
       g.fillRect(x-2,y-2,5,5);
       drawCircle7(g,x,y);
    }

    /** Tracade d'un joli petit cercle de 5 pixels de diam�tre */
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

    /** Remplissage d'un joli cercle de 5 pixels de diam�tre */
    static public void fillCircle5(Graphics g,int x, int y) {
       if( !(g instanceof Graphics2D) ) {
          g.fillOval(x-2, y-2, 4, 4);
          g.drawOval(x-2, y-2, 4, 4);
          return;
       }
       g.fillRect(x-1,y-1,3,3);
       drawCircle5(g,x,y);
    }

    /** Remplissage d'un joli cercle de 2 pixels de diam�tre */
    static public void fillCircle2(Graphics g,int x, int y) {
       if( !(g instanceof Graphics2D) ) {
          g.fillOval(x-1, y-1, 2, 2);
          g.drawOval(x-1, y-1, 2, 2);
          return;
       }
       g.drawLine(x,y-1,x,y+1);
       g.drawLine(x-1,y,x+1,y);
    }

    /** Trac� d'une fl�che entre (x,y) et (x1,y1), avec un label �ventuel et une taille d'empennage de L pixels */
    static public void drawFleche(Graphics g,double x,double y,double x1,double y1,int L, String s) {
       g.drawLine((int)x,(int)y,(int)x1,(int)y1);

       double theta,delta;
       if( x!=x1) {
          theta = Math.atan( (y1-y)/(x1-x) );
          if( x>x1 ) theta += Math.PI;
       } else {
          if( y<y1 ) theta = Math.PI/2;
          else theta = -Math.PI/2;
       }
       delta = 3.0*Math.PI/4;
       double dx1 = L*Math.cos( theta+delta);
       double dy1 = L*Math.sin( theta+delta);
       double dx2 = L*Math.cos( theta-delta);
       double dy2 = L*Math.sin( theta-delta);

       g.drawLine((int)(x1+dx1),(int)(y1+dy1),(int)x1,(int)y1);
       g.drawLine((int)x1,(int)y1,(int)(x1+dx2),(int)(y1+dy2));

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
    
    /** Trac� d'un cartouche, �ventuellement semi-transparent
     * Retourne les coordonn�es pour �crire dedans */
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

    /**Tracage d'une petite �toile */
    static public void drawStar(Graphics g,int x, int y) {
       g.drawLine(x,y-3,x,y-2);
       g.drawLine(x-1,y-1,x+1,y-1);
       g.drawLine(x-3,y,x+3,y);
       g.drawLine(x-2,y+1,x+2,y+1);
       g.drawLine(x-1,y+2,x+1,y+2);
       g.drawLine(x-2,y+3,x-2,y+3);
       g.drawLine(x+2,y+3,x+2,y+3);
   }
    
    /**
     * Dessin d'un bouton radio
     * @param g le contexte graphique
     * @param x,y la position (coin en haut � gauche)
     * @param colorBord Couleur du bord
     * @param colorFond couleur du fond ou si null, couleur par d�faut
     * @param colorCoche couleur de la coche ou si null, couleur par d�faut
     * @param selected coche active si true
     */
    static public void drawRadio(Graphics g,int x,int y,
          Color colorBord,Color colorFond,Color colorCoche,boolean selected) {
       int w = CINT.length+1;

       // Une couleur de fond particuli�re ?
       if( colorFond!=null ) {
          g.setColor(colorFond);
          g.fillRect(x+1,y+1,w,w);

          // Couleur de fond par d�faut
       } else {
          for( int i=0; i<CINT.length; i++ ) {
             g.setColor(CINT[i]);
             g.drawLine(x+1,y+1+i,x+CINT.length,y+1+i);
          }
       }
       
       g.setColor(colorBord);
       g.drawArc(x,y,w,w,0,360);

       // La petite coche de s�lection
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

    /**
     * Dessin d'une checkbox
     * @param g le contexte graphique
     * @param x,y la position (coin en haut � gauche)
     * @param colorBord Couleur du bord
     * @param colorFond couleur du fond ou si null, couleur par d�faut
     * @param colorCoche couleur de la coche ou si null, couleur par d�faut
     * @param selected coche active si true
     */
    static public void drawCheckbox(Graphics g,int x,int y,
          Color colorBord,Color colorFond,Color colorCoche,boolean selected) {
       int w = CINT.length+1;
       g.setColor(colorBord);
       g.drawRect(x,y,w,w);

       // Une couleur de fond particuli�re ?
       if( colorFond!=null ) {
          g.setColor(colorFond);
          g.fillRect(x+1,y+1,w,w);

          // Couleur de fond par d�faut
       } else {
          for( int i=0; i<CINT.length; i++ ) {
             g.setColor(CINT[i]);
             g.drawLine(x+1,y+1+i,x+CINT.length,y+1+i);
          }
       }

       // La petite coche de s�lection
       if( selected ) {
          g.setColor(colorCoche==null?Color.black:colorCoche);
          g.fillRect(x+3, y+4, 2, 5);
          for( int i=0; i<4; i++ ){
             g.drawLine(x+5+i,y+6-i,x+5+i,y+7-i);
          }
       }
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

    /** Trac�e d'une ellipse avec angle, m�thode manuelle
     * Utilis� si le contexte graphique ne supporte par Graphics2D */
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
            tmpX = semiMA*Math.cos(curAngle);
            tmpY = semiMI*Math.sin(curAngle);
            // rotation
            x = tmpX*Math.cos(angle)-tmpY*Math.sin(angle)+xCenter;
            y = tmpX*Math.sin(angle)+tmpY*Math.cos(angle)+yCenter;

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

    /** Positionne un tooltip sur un JComponent en v�rifiant au pr�alable
     * qu'il n'aurait pas �t� d�j� positionn� */
    static public void toolTip(JComponent c,String s) {
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
     * Attention : enableEvents(AWTEvent.WINDOW_EVENT_MASK); doit �tre appel� auparavant dans le constructeur de la JFrame
      );
     * @param f la JFrame � traiter
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
	 * @param o objet � trouver
	 * @param array tableau dans lequel on recherche
	 * @return premier indice de o dans array, -1 si non trouv�
	 */
	static public int indexInArrayOf(Object o, Object[] array) {
		if( o==null || array==null ) return -1;

		for( int i=0; i<array.length; i++ ) {
			if( o.equals(array[i]) ) return i;
		}
		return -1;
	}

    /** Recherche la position d'une chaine dans un tableau de chaine
     * @param s la chaine � chercher
     * @param array le tableau de chaines
     * @param caseInsensitive true si on ignore la distinction maj/min
     * @return position ou -1 si non trouv�
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
     * � partir de la position indiqu�e */
    static public int indexOfIgnoreCase(String s,String w,int offset) {
       s = toUpper(s);
       w = toUpper(w);
       return s.indexOf(w,offset);
    }

	/**
	 * Remplit une chaine avec des blancs jusqu'� obtenir la longueur d�sir�e
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

	/** Met � baskslash avant tous les slashs */
	static public String slash(String s) {
	   StringBuffer res = new StringBuffer();
	   char a[] = s.toCharArray();
	   for( int i=0; i<a.length; i++ ) {
	      if( a[i]=='/' ) res.append('\\');
	      res.append(a[i]);
	   }
	   return res.toString();
	}

    /** Nettoie un StringBuffer pour �viter des allocations inutiles */
    static public void resetString(StringBuffer s) {
       int n = s.length();
       if( n==0 ) return;
       s.delete(0,n);
    }

    /** Concat�nation de paths.
     * et ins�re le s�parateur / uniquement si c'est n�cessaire.
     * Remplace les \ �ventuelles par / (et r�ciproquement)
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

    /** Conversion en minuscules d'une chaine, chaque premi�re lettre en majuscule*/
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
       JButton h = new JButton(new ImageIcon(Aladin.aladin.getImagette("Help.gif")));
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

    /** Teste simplement si l'url existe et r�pond */
    public static boolean isUrlResponding(String url) {
       try { return isUrlResponding(new URL(url)); }
      catch( MalformedURLException e ) {}
      return false;
    }

    /** Teste simplement si l'url existe et r�pond */
    public static boolean isUrlResponding(URL url) {
       try {
          HttpURLConnection conn = (HttpURLConnection) url.openConnection();
          conn.setRequestMethod("HEAD");
          int code = conn.getResponseCode();
//          System.out.println(url+" => ["+code+"]");
//          if( code/100 == 4 ) return false;
          return code/100 == 2;
       } catch( Exception e ) { }
          return false;
       }

    /** Retourne true s'il s'agit dun fichier JPEG couleur */
    static public boolean isJPEGColored(String file) throws Exception {
       RandomAccessFile f = new RandomAccessFile(file,"r");
       byte [] buf = new byte[(int)f.length()];
       f.readFully(buf);
       f.close();
       return isJPEGColored(buf);
    }

    /** Retourne true s'il s'agit d'un buffer contenant un JPEG couleur */
    static public boolean isJPEGColored(byte [] buf) throws Exception {
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
	 * Suppression r�cursive d'un r�pertoire
	 * @param dir
	 * @return false sur un �chec
	 */
	public static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			// pour tous les enfants du r�pertoire
			for (int i=0; i<children.length; i++) {
				// essaye de le supprimer comme un r�pertoire r�cursivement
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) { return false; }
			}
		}
		// et supprime le repertoire maintenant qu'il est vide
		return dir.delete();
	}


	/**
	 * Suppression r�cursive des fichiers selon une expression r�guli�re
	 * @param dir
	 * @return false sur un �chec
	 */
	public static boolean deleteDir(File dir, String regex) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			// pour tous les enfants du r�pertoire
			for (int i=0; i<children.length; i++) {
				// essaye de le supprimer comme un r�pertoire r�cursivement
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
	 * @param createIfNeeded doit-on cr�er le fichier s'il n'existe pas
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

	/** Cr�ation si n�cessaire des r�pertoires et sous-r�pertoire du fichier
	 * pass� en param�tre
	 */
	public static void createPath(String filename) throws Exception {
	   File f = new File(new File(filename).getParent());
	   f.mkdirs();
	   if( !f.exists() ) throw new Exception("Cannot create directory for "+filename);
	   
//	   File f;
//	   String FS = filename.indexOf('/')>=0 ? "/" : "\\";
//
//	   // Pour acc�lerer, on teste d'abord l'existence �ventuelle du dernier r�pertoire
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

	/** Permet le choix d'un r�pertoire */
	static public String dirBrowser(Component c,String currentDirectoryPath) {
	   JFileChooser fd = new JFileChooser(currentDirectoryPath);
	   fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	   fd.setAcceptAllFileFilterUsed(false);
	   if (fd.showOpenDialog(c) == JFileChooser.APPROVE_OPTION) {
	      try {
	         return Util.concatDir(fd.getCurrentDirectory().getAbsolutePath(),fd.getSelectedFile().getName());
	      } catch( Exception e ) {
	         e.printStackTrace();
	      }
	   }
	   return null;
	}


	static private String HEX = "0123456789ABCDEF";

    /** Affichage en hexad�cimal d'un caract�re */
    static public String hex(char c) { return hex((int)c); }

    /** Affichage en hexad�cimal d'un octet */
    static public String hex(int b) {
	   return ""+HEX.charAt(b/16)+HEX.charAt(b%16);
	}

    /**
     * retourne un objet 'Composite' (pour un footprint) � partir d'un niveau d'opacit�
     * Le passage par une m�thode permettrait d'appliquer une fonction de transfert
     *
     * @param opacityLevel nvieau d'opacit� d�sir�
     * @return objet Composite correspondant
     */
    static public Composite getFootprintComposite(float opacityLevel) {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityLevel);
    }

    /**
     * retourne un objet 'Composite' (pour un footprint) � partir d'un niveau d'opacit�
     * Le passage par une m�thode permettrait d'appliquer une fonction de transfert
     *
     * @param opacityLevel nvieau d'opacit� d�sir�
     * @return objet Composite correspondant
     */
    static public Composite getImageComposite(float opacityLevel) {
        return AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacityLevel);
    }

    /** Trac� d'un rectangle avec aplat semi-transparent.
     * Si la transparence n'est pas activ�e, trac� d'un simple rectangle */
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

    /** Diminue la priorit� du Thread runme par rapport au Thread ref */
    static public void decreasePriority(Thread ref,Thread runme) {
       try {
          runme.setPriority(ref.getPriority()-1);
       } catch( Exception e ) {}
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

    static private boolean tryNano=false;
    static private Method nanoMethod=null;

    /** R�cup�ration du temps en ms via la m�thode System.nanoTime() si possible
     * sinon via la m�thode classique System.currentTimeMillis().
     * @param unit 0-ns 1:ms 2:s
     */
    static public long getTime() { return getTime(1); }
    static public long getTime(int unit) {
       if( !tryNano ) {
          tryNano=true;
          try { nanoMethod = System.class.getMethod("nanoTime",new Class[] {}); }
          catch( Exception e) { }
       }
       if( nanoMethod!=null ) {
          try { return ((Long)(nanoMethod.invoke((Object)null, (Object[])null))).longValue()/(unit==1? 1000000L : unit==2 ? 1000000000L : 1); }
          catch( Exception e) { nanoMethod=null; }
       }
       long t=System.currentTimeMillis();
       if( unit==0 ) return t*1000000L;
       if( unit==2 ) return t/1000L;
       return t;

       // DES QU'ON NE SUPPORTERA PLUS JAV 1.4
//       return System.nanoTime()/1000000;
    }
    
    /** Retourne la lettre code d'un champ TFORM FITS nD */
    static final public char getFitsType(String form) {
       int l=form.indexOf('(');
       if( l==-1 ) l=form.length();
       return form.charAt(l-1);
    }
    
    /** retourne la taille du champs FITS exprim� sous la forme nD(xxx) ou nPD(xxx) */
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
     * @param type le code du type de donn�es
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
    
    /**
     * Affiche le chiffre donn� avec une unit� de volume disque (K M T)
     * @param val taille en octets
     * @return le volume disque dans une unite coherente + l'unite utilisee
     */
    static final public String unites[] = {"","KB","MB","GB","TB","PB","EB","ZB"};
    static final public String getUnitDisk(double val) {
    	return getUnitDisk(val, 2);
    }
    static final public String getUnitDisk(double val, int format) {
    	int unit = 0;
    	while (val >= 1024 && unit<unites.length-1) {
    		unit++;
    		val /= 1024L;
    	}
    	NumberFormat nf = NumberFormat.getInstance();
    	nf.setMaximumFractionDigits(format);
    	return nf.format(val)+unites[unit];
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
                        // pas de v�rification
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
           sb.append(String.format((Locale)null, "<TR><TD>%.5f</TD><TD>%.5f</TD></TR>\n", forme.o[0].raj, forme.o[0].dej));
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

// PAS ENCORE TESTE
//    /** Extrait le premier nombre entier qui se trouve dans la chaine � partir
//     * d'une certaine position
//     * Ne prend pas en compte un signe �ventuel
//     * @param s la chaine � traiter
//     * @param pos la position de d�part
//     * @return le nombre trouv�, ou 0 si aucun
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


}
